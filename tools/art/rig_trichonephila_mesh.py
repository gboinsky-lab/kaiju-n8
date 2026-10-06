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

Divisao: abdomen (regiao de tras e alta), patas (fora do miolo, agrupadas por angulo em volta do centro, separando
onde ha vao entre elas; da frente para tras = leg_<lado>_0..), o resto e o corpo. Pivo de cada pata = ponto da pata
mais perto do centro (quadril). A GeckoLib inverte o X do pivo ao carregar: os pivos sao gravados com X negado.
Substitui a arte em cubos de build_trichonephila.py (que nao deve mais ser rodado para esta especie).
Uso: python3 tools/art/rig_trichonephila_mesh.py
"""
import json
import math
import shutil
import sys
from pathlib import Path

import numpy as np

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/kn8"
SOURCE = ROOT / "tools/art/converted/trichonephila"
NAME = "trichonephila"
PX = 16.0

# Medidas da malha convertida (metros), conferidas na vista de cima: centro de onde saem as patas e o miolo do corpo.
CENTER_Z = -0.25
CORE_HALF_WIDTH = 0.7
CORE_FRONT_Z = -2.2
ABDOMEN_START_Z = 0.25
# Abdomen como elipsoide (centro e raios em metros): pega a bola de tras sem roubar o alto das patas traseiras.
ABDOMEN_CENTER = (0.0, 2.55, 1.2)
ABDOMEN_RADII = (1.15, 1.25, 1.0)
ABDOMEN_TOLERANCE = 1.15
LEG_GAP_DEGREES = 12.0
# Nenhuma pata sobe acima disto: faces de "pata" mais altas sao a lateral do abdomen.
LEG_MAX_Y = 2.7


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


def classify(vertices, faces):
    centroids = vertices[faces].mean(axis=1)
    x, y, z = centroids[:, 0], centroids[:, 1], centroids[:, 2]
    ellipsoid = (((x - ABDOMEN_CENTER[0]) / ABDOMEN_RADII[0]) ** 2 + ((y - ABDOMEN_CENTER[1]) / ABDOMEN_RADII[1]) ** 2
                 + ((z - ABDOMEN_CENTER[2]) / ABDOMEN_RADII[2]) ** 2)
    abdomen = (z > ABDOMEN_START_Z) & (ellipsoid < ABDOMEN_TOLERANCE)
    core = (np.abs(x) < CORE_HALF_WIDTH) & (z > CORE_FRONT_Z) & (z <= ABDOMEN_START_Z)
    leg = ~(abdomen | core)
    abdomen = abdomen | (leg & (y > LEG_MAX_Y) & (z > ABDOMEN_START_Z))
    leg = leg & ~abdomen
    labels = np.array(["body"] * len(faces), dtype=object)
    labels[abdomen] = "abdomen"
    # Angulo de cada face de pata em volta do centro: -90 = frente, +90 = tras (lado decidido pelo sinal de x).
    angle = np.degrees(np.arctan2(z - CENTER_Z, np.abs(x)))
    for side, mask in (("right", leg & (x > 0)), ("left", leg & (x <= 0))):
        indices = np.where(mask)[0]
        order = indices[np.argsort(angle[indices])]
        clusters, current = [], [order[0]] if len(order) else []
        for previous, index in zip(order, order[1:]):
            if angle[index] - angle[previous] > LEG_GAP_DEGREES:
                clusters.append(current)
                current = []
            current.append(index)
        if current:
            clusters.append(current)
        # Grupos pequenos demais sao ruido (pedacos do corpo na borda): ficam no corpo.
        clusters = [c for c in clusters if len(c) > 40]
        for number, cluster in enumerate(clusters[:4]):
            labels[cluster] = f"leg_{side}_{number}"
    return labels


def pivot_of(vertices, faces, labels, name):
    """Quadril: vertice da pata mais perto do centro, no plano horizontal."""
    used = np.unique(faces[labels == name])
    points = vertices[used]
    points = points[points[:, 1] < LEG_MAX_Y]
    distance = np.hypot(points[:, 0], points[:, 2] - CENTER_Z)
    hip = points[np.argmin(distance)]
    return hip


def geo_pivot(point):
    """Metros (espaco de render) -> pixels do .geo.json, com o X negado (a GeckoLib inverte ao carregar)."""
    return [round(-point[0] * PX, 3), round(point[1] * PX, 3), round(point[2] * PX, 3)]


def main():
    vertices, uvs, normals, faces = read_obj(SOURCE / f"{NAME}.obj")
    labels = classify(vertices, faces)
    bones = {"root": None, "body": "root", "head": "body", "fang_left": "head", "fang_right": "head",
             "abdomen": "body"}
    pivots = {"root": np.zeros(3), "body": np.array([0, 2.0, CENTER_Z]), "head": np.array([0, 2.0, -1.5]),
              "fang_left": np.array([0.15, 1.8, -1.9]), "fang_right": np.array([-0.15, 1.8, -1.9]),
              "abdomen": np.array([0, 2.0, ABDOMEN_START_Z])}
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
    shutil.copy(SOURCE / f"{NAME}.png", ASSETS / f"textures/entity/{NAME}.png")
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
                        "visible_bounds_width": 6, "visible_bounds_height": 5, "visible_bounds_offset": [0, 2, 0]},
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
