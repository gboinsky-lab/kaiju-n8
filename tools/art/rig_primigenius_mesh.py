#!/usr/bin/env python3
"""Rigging do Primigenius do Meshy (malha presa aos ossos da GeckoLib), modelo "estilo Minecraft" de 2026-10-06.

Entrada: tools/art/converted/<especie>/<especie>.obj + .png (meshy_convert.py), em metros, pes em Y = 0, frente -Z.
Saida (por especie):
  - assets/kn8/meshes/<especie>.json + meshes/<especie>/<osso>.obj;
  - assets/kn8/textures/entity/<especie>.png (com borda nas ilhas de UV: pad_texture.py);
  - assets/kn8/geo/entity/<especie>.geo.json: os MESMOS ossos do modelo de cubos (body, head, jaw, fin_left/right,
    arm_*/forearm_*, leg_*, tail_1..4), so que sem cubos e com os pivos nas juntas da malha nova.
As animacoes (animations/entity/<especie>.animation.json, de build_primigenius.py) continuam as mesmas: so giram
ossos, entao valem para a malha nova.

Divisao pela forma (cortes em metros na tabela SPECIES, medidos nas vistas de tools/art/preview.py / no perfil):
cauda (atras e baixo), cabeca (frente e alto; mandibula = parte de baixo da boca), bracos (fora do tronco: o vao
entre braco e tronco fecha conforme sobe, por isso a borda de dentro depende da altura), antebraco (braco abaixo do
cotovelo), pernas (abaixo do quadril, pelo lado: X negativo = "left"), o resto e o corpo. As "fin" (barbatanas do
modelo de cubos) ficam sem malha.
A GeckoLib inverte o X do pivo ao carregar: os pivos sao gravados com X negado.
Uso: python3 tools/art/rig_primigenius_mesh.py [especie]   (padrao: primigenius; requer numpy e pillow)
"""
import json
import shutil
import sys
from pathlib import Path

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).parent))
from pad_texture import DILATE_STEPS, dilate, uv_mask  # noqa: E402
from rig_trichonephila_mesh import read_obj, write_obj  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/kn8"
PX = 16.0
TEXTURE_SIZE = 1024

SPECIES = {
    "primigenius": {
        # Cauda: atras do quadril e baixa; 4 segmentos de mesmo comprimento ate a ponta.
        "tail_z": 0.9, "tail_max_y": 2.6, "tail_half_width": 0.9,
        # Cabeca: na frente do pescoco e alta; mandibula = abaixo de jaw_y na frente de jaw_z.
        "head_z": -2.0, "head_min_y": 3.9, "jaw_y": 4.6, "jaw_z": -2.3,
        # Borda de dentro do braco (|x|) por altura (y), interpolada; acima de arm_max_y nao ha braco.
        "arm_inner": [(0.7, 1.55), (2.2, 1.55), (2.6, 1.3), (3.0, 1.1), (4.2, 1.15)],
        "arm_min_y": 0.7, "arm_max_y": 4.2, "elbow_y": 3.0,
        # Quadril: abaixo = perna do lado.
        "hip_y": 2.3,
    },
}


def split(mesh, cfg):
    vertices, _, _, faces = mesh
    c = vertices[faces].mean(axis=1)
    x, y, z = c[:, 0], c[:, 1], c[:, 2]
    labels = np.array(["body"] * len(faces), dtype=object)
    tail = (z > cfg["tail_z"]) & (y < cfg["tail_max_y"]) & (np.abs(x) < cfg["tail_half_width"])
    head = ~tail & (z < cfg["head_z"]) & (y > cfg["head_min_y"])
    ys, xs = zip(*cfg["arm_inner"])
    inner = np.interp(y, ys, xs)
    arm = ~tail & ~head & (y >= cfg["arm_min_y"]) & (y <= cfg["arm_max_y"]) & (np.abs(x) > inner)
    leg = ~tail & ~head & ~arm & (y < cfg["hip_y"])
    labels[head] = "head"
    labels[head & (y < cfg["jaw_y"]) & (z < cfg["jaw_z"])] = "jaw"
    for side, mask in (("left", x <= 0), ("right", x > 0)):
        labels[arm & mask & (y >= cfg["elbow_y"])] = f"arm_{side}"
        labels[arm & mask & (y < cfg["elbow_y"])] = f"forearm_{side}"
        labels[leg & mask] = f"leg_{side}"
    if tail.any():
        start, end = z[tail].min(), z[tail].max()
        segment = np.clip(((z - start) / (end - start) * 4).astype(int), 0, 3)
        for number in range(4):
            labels[tail & (segment == number)] = f"tail_{number + 1}"
    return labels


def band_mean(points, axis, low, high):
    sel = points[(points[:, axis] >= low) & (points[:, axis] <= high)]
    return sel.mean(axis=0) if len(sel) else points.mean(axis=0)


def pivots(vertices, faces, labels, cfg):
    def pts(name):
        return vertices[np.unique(faces[labels == name])]

    result = {"root": np.zeros(3), "body": np.array([0.0, cfg["hip_y"], pts("body")[:, 2].mean()])}
    head = pts("head")
    # Pescoco: parte de tras da cabeca; mandibula: dobradica atras da boca.
    result["head"] = band_mean(head, 2, head[:, 2].max() - 0.3, head[:, 2].max()) * [0, 1, 1]
    result["jaw"] = np.array([0.0, cfg["jaw_y"], cfg["jaw_z"]])
    top = head[:, 1].max()
    for side, sign in (("left", -1), ("right", 1)):
        arm = pts(f"arm_{side}")
        result[f"arm_{side}"] = band_mean(arm, 1, cfg["arm_max_y"] - 0.4, cfg["arm_max_y"])
        both = np.vstack([arm, pts(f"forearm_{side}")])
        result[f"forearm_{side}"] = band_mean(both, 1, cfg["elbow_y"] - 0.15, cfg["elbow_y"] + 0.15)
        leg = pts(f"leg_{side}")
        result[f"leg_{side}"] = band_mean(leg, 1, cfg["hip_y"] - 0.3, cfg["hip_y"])
        result[f"fin_{side}"] = np.array([sign * 0.5, top, result["head"][2] - 0.3])
    for number in range(1, 5):
        segment = pts(f"tail_{number}")
        result[f"tail_{number}"] = band_mean(segment, 2, segment[:, 2].min(), segment[:, 2].min() + 0.25)
    return result


def geo_pivot(point):
    return [round(-point[0] * PX, 3), round(point[1] * PX, 3), round(point[2] * PX, 3)]


def main():
    name = sys.argv[1] if len(sys.argv) > 1 else "primigenius"
    cfg = SPECIES[name]
    source = ROOT / "tools/art/converted" / name
    vertices, uvs, normals, faces = read_obj(source / f"{name}.obj")
    labels = split((vertices, uvs, normals, faces), cfg)
    # Hierarquia e ordem dos ossos do modelo de cubos (as animacoes usam estes nomes).
    old_geo = json.loads((ASSETS / f"geo/entity/{name}.geo.json").read_text(encoding="utf-8"))
    old_bones = old_geo["minecraft:geometry"][0]["bones"]
    points = pivots(vertices, faces, labels, cfg)
    mesh_dir = ASSETS / "meshes" / name
    if mesh_dir.exists():
        shutil.rmtree(mesh_dir)
    index = {"bones": {}}
    for bone in old_bones:
        part = faces[labels == bone["name"]]
        if len(part):
            write_obj(mesh_dir / f"{bone['name']}.obj", vertices, uvs, normals, part)
            index["bones"][bone["name"]] = f"kn8:meshes/{name}/{bone['name']}.obj"
            print(f"{bone['name']}: {len(part)} triangulos")
    missing = set(labels) - {b["name"] for b in old_bones}
    assert not missing, f"faces em ossos que nao existem: {missing}"
    (ASSETS / "meshes" / f"{name}.json").write_text(json.dumps(index, indent=2) + "\n", encoding="utf-8")
    image = Image.open(source / f"{name}.png").convert("RGBA")
    mask = uv_mask([source / f"{name}.obj"], image.width, image.height)
    padded = dilate(np.asarray(image)[..., :3], mask, DILATE_STEPS)
    Image.fromarray(np.clip(padded, 0, 255).astype(np.uint8)).convert("RGBA") \
        .resize((TEXTURE_SIZE, TEXTURE_SIZE), Image.Resampling.BOX).save(ASSETS / f"textures/entity/{name}.png")
    bones = []
    for bone in old_bones:
        entry = {"name": bone["name"], "pivot": geo_pivot(points[bone["name"]])}
        if "parent" in bone:
            entry["parent"] = bone["parent"]
        bones.append(entry)
    low, high = vertices.min(axis=0), vertices.max(axis=0)
    span = float(np.max(high - low))
    geo = {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": f"geometry.{name}", "texture_width": 16, "texture_height": 16,
                        "visible_bounds_width": round(span + 1, 1), "visible_bounds_height": round(span + 1, 1),
                        "visible_bounds_offset": [0, round(float(high[1]) / 2, 1), 0]},
        "bones": bones}]}
    (ASSETS / f"geo/entity/{name}.geo.json").write_text(json.dumps(geo, indent=2) + "\n", encoding="utf-8")
    print("pivos:", {k: np.round(v, 2).tolist() for k, v in points.items()})


if __name__ == "__main__":
    main()
