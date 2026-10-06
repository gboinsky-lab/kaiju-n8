#!/usr/bin/env python3
"""Rigging automatico da Trichonephila do Meshy (malha presa aos ossos da GeckoLib).

Entrada: tools/art/converted/trichonephila/trichonephila.obj + .png (gerados por meshy_convert.py), em metros,
pes em Y = 0, frente para -Z. Saida:
  - assets/kn8/meshes/trichonephila/<osso>.obj : pedaco da malha de cada osso (coordenadas do modelo, em blocos);
  - assets/kn8/meshes/trichonephila.json       : indice (osso -> malha), lido pelo MeshRenderLayer;
  - assets/kn8/textures/entity/trichonephila.png : textura do Meshy (a malha usa a textura e o render type do
    proprio modelo GeckoLib, para desenhar no mesmo buffer);
  - assets/kn8/geo/entity/trichonephila.geo.json : esqueleto SO de ossos (sem cubos), mesmos nomes de antes;
  - assets/kn8/animations/entity/trichonephila.animation.json : as animacoes de build_trichonephila.py.

Divisao (modelo de 8 patas, 2026-10-06): cefalotorax = body (frente = head, presas = fang_left/right), abdomen
atras, patas = o resto; cada pata e achada pela parte distante do centro e as faces perto do quadril vao para a
pata cuja linha passa mais perto (da frente para tras = leg_<lado>_0..3). Pivo de cada pata = ponto da pata
mais perto do centro (quadril). A GeckoLib inverte o X do pivo ao carregar: os pivos sao gravados com X negado.
Substitui a arte em cubos de build_trichonephila.py (que nao deve mais ser rodado para esta especie).
Uso: python3 tools/art/rig_trichonephila_mesh.py   (requer numpy, pillow e scipy)
"""
import json
import math
import shutil
import sys
from pathlib import Path

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).parent))
from pad_texture import DILATE_STEPS, dilate, uv_mask  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/kn8"
SOURCE = ROOT / "tools/art/converted/trichonephila"
NAME = "trichonephila"
PX = 16.0

# Medidas da malha convertida (metros, frente -Z), modelo "estilo Minecraft" de 2026-10-06 (8 patas, corpo baixo):
# cefalotorax |x| < 0,42 na frente de ABDOMEN_START_Z; abdomen (caixa listrada) atras, |x| < 0,7, acima do chao.
CENTER_Z = -0.5
CORE_HALF_WIDTH = 0.42
ABDOMEN_START_Z = 0.3
ABDOMEN_HALF_WIDTH = 0.7
ABDOMEN_MIN_Y = 0.45
# Cabeca = frente do cefalotorax; queliceras = parte baixa da frente (as presas brancas).
HEAD_Z = -1.2
FANG_Z = -1.45
FANG_MAX_Y = 0.6
# Patas: a parte distante do centro (> LEG_FAR_RADIUS) separa bem cada pata; pedacos vizinhos (distancia
# LEG_LINK) se juntam, e pedacos na mesma direcao (< LEG_MERGE_DEGREES) sao a mesma pata. O resto de cada pata
# (perto do quadril) vai para a pata cuja linha (vista de cima) passa mais perto.
LEG_FAR_RADIUS = 1.3
LEG_LINK = 0.12
LEG_MERGE_DEGREES = 12.0
LEG_MIN_FACES = 80
# Pivo do quadril: so vertices abaixo disto (a pata sobe ate o "joelho" acima do corpo).
LEG_MAX_Y = 1.7
HIP_QUANTILE = 0.1


def read_obj(path):
    vertices, uvs, normals, faces = [], [], [], []
    for line in path.read_text(encoding="utf-8").splitlines():
        parts = line.split()
        if not parts:
            continue
        if parts[0] == "v":
            vertices.append([float(x) for x in parts[1:4]])
        elif parts[0] == "vt":
            uvs.append([float(x) for x in parts[1:3]])
        elif parts[0] == "vn":
            normals.append([float(x) for x in parts[1:4]])
        elif parts[0] == "f":
            faces.append([int(token.split("/")[0]) - 1 for token in parts[1:4]])
    return np.array(vertices), np.array(uvs), np.array(normals), np.array(faces)


def write_obj(path, vertices, uvs, normals, faces):
    used = np.unique(faces)
    remap = -np.ones(len(vertices), dtype=int)
    remap[used] = np.arange(len(used))
    lines = [f"o {path.stem}"]
    lines += [f"v {x:.5f} {y:.5f} {z:.5f}" for x, y, z in vertices[used]]
    lines += [f"vt {u:.5f} {v:.5f}" for u, v in uvs[used]]
    lines += [f"vn {x:.4f} {y:.4f} {z:.4f}" for x, y, z in normals[used]]
    for a, b, c in remap[faces] + 1:
        lines.append(f"f {a}/{a}/{a} {b}/{b}/{b} {c}/{c}/{c}")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def leg_lines(centroids, leg, side_mask):
    """Uma linha (vista de cima) por pata deste lado, da frente para tras."""
    from scipy.cluster.hierarchy import fcluster, linkage
    x, z = centroids[:, 0], centroids[:, 2]
    radius = np.hypot(x, z - CENTER_Z)
    angle = np.degrees(np.arctan2(z - CENTER_Z, np.abs(x)))
    far = np.where(leg & side_mask & (radius > LEG_FAR_RADIUS))[0]
    labels = fcluster(linkage(centroids[far], "single"), LEG_LINK, "distance")
    pieces = [far[labels == k] for k in np.unique(labels)]
    pieces = sorted((g for g in pieces if len(g) > LEG_MIN_FACES // 4), key=lambda g: angle[g].mean())
    legs = []
    for piece in pieces:
        if legs and angle[piece].mean() - angle[legs[-1]].mean() < LEG_MERGE_DEGREES:
            legs[-1] = np.concatenate([legs[-1], piece])
        else:
            legs.append(piece)
    lines = []
    for group in (g for g in legs if len(g) > LEG_MIN_FACES):
        points = centroids[group][:, [0, 2]]
        middle = points.mean(axis=0)
        direction = np.linalg.svd(points - middle)[2][0]
        lines.append((middle, direction))
    return lines


def classify(vertices, faces):
    centroids = vertices[faces].mean(axis=1)
    x, y, z = centroids[:, 0], centroids[:, 1], centroids[:, 2]
    core = (np.abs(x) < CORE_HALF_WIDTH) & (z < ABDOMEN_START_Z)
    abdomen = (np.abs(x) < ABDOMEN_HALF_WIDTH) & (z >= ABDOMEN_START_Z) & (y > ABDOMEN_MIN_Y)
    leg = ~(core | abdomen)
    labels = np.array(["body"] * len(faces), dtype=object)
    labels[abdomen] = "abdomen"
    labels[core & (z < HEAD_Z)] = "head"
    fang = core & (z < FANG_Z) & (y < FANG_MAX_Y)
    labels[fang & (x <= 0)] = "fang_left"
    labels[fang & (x > 0)] = "fang_right"
    for side, side_mask in (("right", x > 0), ("left", x <= 0)):
        lines = leg_lines(centroids, leg, side_mask)
        if len(lines) != 4:
            print(f"AVISO: {len(lines)} patas do lado {side} (esperado 4); confira a vista de cima")
        members = np.where(leg & side_mask)[0]
        distance = np.stack([np.abs((centroids[members][:, 0] - m[0]) * d[1] - (centroids[members][:, 2] - m[1]) * d[0])
                             for m, d in lines], axis=1)
        nearest = np.argmin(distance, axis=1)
        for number in range(len(lines)):
            labels[members[nearest == number]] = f"leg_{side}_{number}"
    return labels


def pivot_of(vertices, faces, labels, name):
    """Quadril: media dos 10% de vertices da pata mais perto do centro (vista de cima)."""
    points = vertices[np.unique(faces[labels == name])]
    points = points[points[:, 1] < LEG_MAX_Y]
    distance = np.hypot(points[:, 0], points[:, 2] - CENTER_Z)
    nearest = points[distance <= np.quantile(distance, HIP_QUANTILE)]
    return nearest.mean(axis=0)


def geo_pivot(point):
    """Metros (espaco de render) -> pixels do .geo.json, com o X negado (a GeckoLib inverte ao carregar)."""
    return [round(-point[0] * PX, 3), round(point[1] * PX, 3), round(point[2] * PX, 3)]


def main():
    vertices, uvs, normals, faces = read_obj(SOURCE / f"{NAME}.obj")
    labels = classify(vertices, faces)
    bones = {"root": None, "body": "root", "head": "body", "fang_left": "head", "fang_right": "head",
             "abdomen": "body"}
    def center(name):
        return vertices[np.unique(faces[labels == name])].mean(axis=0)

    def top_back(name):
        """Pivo na ligacao com o pai: alto e atras (cabeca/queliceras giram em volta da junta)."""
        points = vertices[np.unique(faces[labels == name])]
        return np.array([points[:, 0].mean(), points[:, 1].max(), points[:, 2].max()])

    pivots = {"root": np.zeros(3), "body": np.array([0.0, center("body")[1], CENTER_Z]),
              "head": np.array([0.0, center("head")[1], HEAD_Z]),
              "fang_left": top_back("fang_left"), "fang_right": top_back("fang_right"),
              "abdomen": np.array([0.0, center("abdomen")[1], ABDOMEN_START_Z])}
    for side in ("left", "right"):
        for number in range(4):
            name = f"leg_{side}_{number}"
            bones[name] = "body"
            pivots[name] = pivot_of(vertices, faces, labels, name) if np.any(labels == name) else pivots["body"]
    mesh_dir = ASSETS / "meshes" / NAME
    if mesh_dir.exists():
        shutil.rmtree(mesh_dir)
    index = {"bones": {}}
    for name in bones:
        part = faces[labels == name]
        if len(part):
            write_obj(mesh_dir / f"{name}.obj", vertices, uvs, normals, part)
            index["bones"][name] = f"kn8:meshes/{NAME}/{name}.obj"
            print(f"{name}: {len(part)} triangulos")
    (ASSETS / "meshes" / f"{NAME}.json").write_text(json.dumps(index, indent=2) + "\n", encoding="utf-8")
    # Textura com borda nas ilhas de UV (sem mipmap no Minecraft a costura pega o fundo).
    image = Image.open(SOURCE / f"{NAME}.png").convert("RGBA")
    mask = uv_mask([SOURCE / f"{NAME}.obj"], image.width, image.height)
    padded = dilate(np.asarray(image)[..., :3], mask, DILATE_STEPS)
    Image.fromarray(np.clip(padded, 0, 255).astype(np.uint8)).convert("RGBA").save(
        ASSETS / f"textures/entity/{NAME}.png")
    stale = ASSETS / f"textures/entity/{NAME}_mesh.png"
    if stale.exists():
        stale.unlink()
    geo_bones = []
    for name, parent in bones.items():
        entry = {"name": name, "pivot": geo_pivot(pivots[name])}
        if parent:
            entry["parent"] = parent
        geo_bones.append(entry)
    geo = {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": f"geometry.{NAME}", "texture_width": 16, "texture_height": 16,
                        "visible_bounds_width": 7, "visible_bounds_height": 4, "visible_bounds_offset": [0, 1, 0]},
        "bones": geo_bones}]}
    (ASSETS / f"geo/entity/{NAME}.geo.json").write_text(json.dumps(geo, indent=2) + "\n", encoding="utf-8")
    sys.path.insert(0, str(Path(__file__).parent))
    import build_trichonephila
    anims = build_trichonephila.animations()
    (ASSETS / f"animations/entity/{NAME}.animation.json").write_text(json.dumps(anims, indent=2) + "\n",
                                                                     encoding="utf-8")
    print("pivos das patas:", {k: np.round(v, 2).tolist() for k, v in pivots.items() if k.startswith("leg")})


if __name__ == "__main__":
    main()
