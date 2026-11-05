#!/usr/bin/env python3
"""Acessorios 3D do jogador (EQUIP-1, modelos do Miguel de 2026-11-05): asas do Numbers 4 e manoplas do Numbers 2.

Entrada: tools/art/converted/<id>/<id>.obj + .png (meshy_convert.py; metros, Y para cima, frente -Z, direita +X).
Saida: assets/kn8/meshes/suit/<id>.json + meshes/suit/<id>/<parte>.obj e textures/models/suit/<id>.png, no mesmo
formato dos trajes (SuitLayer): cada parte no espaco local da ModelPart do jogador (blocos, Y para baixo, X
espelhado). "pivots" no JSON = onde cada parte gira, no espaco da peca-mae.

- numbers4_wings: duas metades (wing_right, wing_left) presas ao tronco pelo centro da mochila, que fica nas costas.
  O plano das asas vem inclinado ~47 graus no modelo: endireitado (plano das costas). O SuitAccessoryLayer abre no ar
  e fecha (gira para tras) no chao.
- numbers2_gauntlets: uma manopla por braco (right_arm, left_arm), endireitada e presa ao antebraco: o punho fechado
  (pe do modelo, Y = 0) na mao do jogador.

Uso: python3 tools/art/rig_accessory_mesh.py <id>   (requer numpy, pillow e scipy)
"""
import json
import shutil
import sys
from pathlib import Path

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).parent))
from pad_texture import DILATE_STEPS, dilate, uv_mask  # noqa: E402
from rig_suit_mesh import add_liner, dark_uv  # noqa: E402
from rig_trichonephila_mesh import read_obj, write_obj  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/kn8"
# Mochila das asas no tronco do jogador (blocos, espaco local do tronco: Y para baixo da gola, Z para tras): meio das
# costas, encostada (o tronco vai ate 2 px para tras).
WING_ROOT = np.array([0.0, 5.5 / 16, 2.6 / 16])
# Envergadura no jogador (blocos): o modelo tem 1,58 m; um pouco menor que o jogador alto [SUPOSICAO].
WING_SPAN = 1.4
# Manopla: do punho (mao do jogador, 11 px abaixo do ombro) ate pouco abaixo do cotovelo; caixa um pouco maior que a
# manga do traje (6 px).
GAUNTLET_TOP = 3.5 / 16
GAUNTLET_BOTTOM = 11.5 / 16
GAUNTLET_BOX = 6.6 / 16


def rotation_to(a, b):
    """Matriz que gira o vetor unitario a ate b (Rodrigues)."""
    v, c = np.cross(a, b), float(a @ b)
    vx = np.array([[0, -v[2], v[1]], [v[2], 0, -v[0]], [-v[1], v[0], 0]])
    return np.eye(3) + vx + vx @ vx / (1.0 + c)


def to_model(points):
    """Metros (Y para cima, direita +X) -> espaco do jogador (Y para baixo, direita em X negativo)."""
    return np.stack([-points[:, 0], -points[:, 1], points[:, 2]], axis=1)


def wings(vertices, normals):
    # Normal do plano das asas (menor variancia) virada para tras (+Z); gira para ficar exatamente em +Z.
    centered = vertices - vertices.mean(axis=0)
    normal = np.linalg.svd(centered, full_matrices=False)[2][2]
    if normal[2] < 0:
        normal = -normal
    rot = rotation_to(normal, np.array([0.0, 0.0, 1.0]))
    pack = (np.abs(vertices[:, 0]) < 0.12) & (np.abs(vertices[:, 1] - vertices[:, 1].mean()) < 0.15)
    center = vertices[pack].mean(axis=0)
    points = (vertices - center) @ rot.T
    out_normals = normals @ rot.T
    scale = WING_SPAN / (points[:, 0].max() - points[:, 0].min())
    points = to_model(points * scale)
    out_normals = to_model(out_normals)
    print(f"  asas endireitadas {np.degrees(np.arccos(np.clip(normal[2], -1, 1))):.1f} graus, escala {scale:.2f}")
    return points, out_normals


def write(name, parts, pivots):
    out_dir = ASSETS / "meshes/suit" / name
    if out_dir.exists():
        shutil.rmtree(out_dir)
    index = {"bones": {}, "pivots": {}}
    for part, (points, uvs, normals, faces) in parts.items():
        write_obj(out_dir / f"{part}.obj", points, uvs, normals, faces)
        index["bones"][part] = f"kn8:meshes/suit/{name}/{part}.obj"
        print(f"{part}: {len(faces)} triangulos")
    index["pivots"] = {k: [round(float(v), 5) for v in p] for k, p in pivots.items()}
    if not index["pivots"]:
        del index["pivots"]
    (ASSETS / "meshes/suit" / f"{name}.json").write_text(json.dumps(index, indent=2) + "\n", encoding="utf-8")


def main():
    name = sys.argv[1]
    source = ROOT / "tools/art/converted" / name
    vertices, uvs, normals, faces = read_obj(source / f"{name}.obj")
    centers = vertices[faces].mean(axis=1)
    if name == "numbers4_wings":
        points, model_normals = wings(vertices, normals)
        # Direita do personagem = X negativo no espaco do jogador.
        side = points[faces].mean(axis=1)[:, 0]
        parts = {"wing_right": (points, uvs, model_normals, faces[side <= 0]),
                 "wing_left": (points, uvs, model_normals, faces[side > 0])}
        write(name, parts, {"wing_right": WING_ROOT, "wing_left": WING_ROOT})
    elif name == "numbers2_gauntlets":
        parts = {}
        for part, sign in (("right_arm", 1), ("left_arm", -1)):
            sel = centers[:, 0] * sign > 0
            used = np.unique(faces[sel])
            g = vertices[used]
            axis = np.linalg.svd(g - g.mean(axis=0), full_matrices=False)[2][0]
            if axis[1] < 0:
                axis = -axis
            rot = rotation_to(axis, np.array([0.0, 1.0, 0.0]))
            local = (vertices - g.mean(axis=0)) @ rot.T
            local_normals = normals @ rot.T
            # Punho (Y menor) na mao: Y do jogador para baixo, do topo da manopla ao punho.
            low, high = local[used, 1].min(), local[used, 1].max()
            height = GAUNTLET_BOTTOM - GAUNTLET_TOP
            scale = height / (high - low)
            points = to_model(local * scale)
            points[:, 1] += GAUNTLET_TOP - points[used, 1].min()
            # Centro do braco do jogador no espaco da parte (o pivo fica 1 px para dentro do meio do braco).
            center_x = -1 / 16 if part == "right_arm" else 1 / 16
            # Escala unica por eixo (o ajuste fatia a fatia do traje separava as placas e a pele aparecia).
            g = points[used]
            low, high = np.percentile(g, 2, axis=0), np.percentile(g, 98, axis=0)
            mid = (low + high) / 2
            factor = GAUNTLET_BOX / np.maximum(high - low, 1e-6)
            points[used, 0] = (g[:, 0] - mid[0]) * factor[0] + center_x
            points[used, 2] = (g[:, 2] - mid[2]) * factor[2]
            print(f"  {part}: escala x {factor[0]:.2f}, z {factor[2]:.2f}")
            # Manga escura por dentro (a pele do braco aparecia entre as placas, visto em jogo).
            image = np.asarray(Image.open(source / f"{name}.png").convert("RGB")).astype(float)
            x0 = -3 if part == "right_arm" else -1
            liner = (x0, GAUNTLET_TOP * 16, -2, 4, (GAUNTLET_BOTTOM - GAUNTLET_TOP) * 16, 4)
            parts[part] = add_liner(part, points, uvs, to_model(local_normals), faces[sel],
                                    dark_uv(image, uvs, faces[sel]), liner)
        write(name, parts, {})
    else:
        raise SystemExit(f"acessorio desconhecido: {name}")
    image = Image.open(source / f"{name}.png").convert("RGBA")
    mask = uv_mask([source / f"{name}.obj"], image.width, image.height)
    padded = dilate(np.asarray(image)[..., :3], mask, DILATE_STEPS)
    texture = ASSETS / "textures/models/suit" / f"{name}.png"
    Image.fromarray(np.clip(padded, 0, 255).astype(np.uint8)).convert("RGBA").save(texture)


if __name__ == "__main__":
    main()
