#!/usr/bin/env python3
"""Trajes 3D da Forca de Defesa (0.6-C): malha do Meshy presa as partes do modelo do JOGADOR do Minecraft.

Entrada: tools/art/converted/<traje>/<traje>.obj + .png (meshy_convert.py, height_m 1,5 = da gola aos pes, a altura
do corpo + pernas do modelo do jogador). Em metros, pes em Y = 0, frente -Z, lado direito do personagem em +X.
Saida (traje = id do item, ex. mk1):
  - assets/kn8/meshes/suit/<traje>.json + meshes/suit/<traje>/<parte>.obj (body, right_arm, left_arm, right_leg,
    left_leg), cada parte no espaco local da ModelPart do jogador: blocos, Y para baixo, X espelhado (o Minecraft
    desenha o modelo com escala (-1, -1, 1)), origem no pivo da parte. O SuitLayer aplica translateAndRotate da
    parte e desenha, entao o traje segue andar, correr e as animacoes de combate (PAL).
  - assets/kn8/textures/models/suit/<traje>.png (com borda nas ilhas de UV).
Os bracos do Meshy vem abertos (~20 graus, pedido no prompt para separar do tronco): cada braco e endireitado pelo
eixo medido (PCA) em volta do ombro, para ficar como o braco do jogador parado.
Uso: python3 tools/art/rig_suit_mesh.py <traje>   (requer numpy, pillow e scipy)
"""
import json
import shutil
import sys
from pathlib import Path

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).parent))
from pad_texture import DILATE_STEPS, dilate, uv_mask  # noqa: E402
from rig_soldier_mesh import absorb_fragments, cap_holes  # noqa: E402
from rig_trichonephila_mesh import read_obj, write_obj  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/kn8"
# Altura da gola no espaco do modelo do jogador (blocos): pescoco em y = 0, pes em y = 1,5.
COLLAR = 1.5
# Pivos das partes do jogador (blocos, espaco do modelo: Y para baixo; direita em X negativo), de HumanoidModel.
PIVOTS = {"body": (0.0, 0.0, 0.0), "right_arm": (-5 / 16, 2 / 16, 0.0), "left_arm": (5 / 16, 2 / 16, 0.0),
          "right_leg": (-1.9 / 16, 12 / 16, 0.0), "left_leg": (1.9 / 16, 12 / 16, 0.0)}
# Cortes medidos na vista de frente de cada traje (metros do OBJ convertido): borda de dentro dos bracos por altura
# (altura acima da qual vale, |x|), ponta dos dedos e quadril.
SUITS = {
    "mk1": {"arm_inner_x": [(1.20, 0.20), (0.95, 0.26), (0.0, 0.28)], "hand_min_y": 0.74, "hip_y": 0.82},
    # Luvas grandes descem ate ~0,64, por fora das coxas (|x| > 0,38).
    "mk1_reinforced": {"arm_inner_x": [(1.22, 0.24), (0.90, 0.30), (0.0, 0.38)], "hand_min_y": 0.62,
                       "hip_y": 0.80},
}


def split(vertices, faces, cfg):
    """Bracos: fora do tronco e acima da ponta dos dedos; corpo: acima do quadril; abaixo, perna do lado. Lado do
    personagem: direita em +X (meshy_convert gira o modelo de frente para -Z)."""
    c = vertices[faces].mean(axis=1)
    inner = np.select([c[:, 1] >= y for y, _ in cfg["arm_inner_x"]], [x for _, x in cfg["arm_inner_x"]])
    arm = (c[:, 1] >= cfg["hand_min_y"]) & (np.abs(c[:, 0]) > inner)
    body = ~arm & (c[:, 1] > cfg["hip_y"])
    leg = ~arm & ~body
    groups = {"body": body, "right_arm": arm & (c[:, 0] > 0), "left_arm": arm & (c[:, 0] <= 0),
              "right_leg": leg & (c[:, 0] > 0), "left_leg": leg & (c[:, 0] <= 0)}
    labels = np.empty(len(faces), dtype=object)
    for name, sel in groups.items():
        labels[sel] = name
    return absorb_fragments(vertices, faces, labels)


def to_model(points):
    """Metros do OBJ (Y para cima, pes em 0, direita em +X) -> espaco do modelo do jogador (Y para baixo, gola em 0,
    direita em X negativo). Duas inversoes = rotacao: a ordem dos vertices (frente das faces) nao muda."""
    return np.stack([-points[:, 0], COLLAR - points[:, 1], points[:, 2]], axis=1)


def straighten(points, normals, pivot):
    """Gira o braco no plano da frente (em volta do ombro) ate o eixo dele ficar vertical."""
    centered = points - pivot
    axis = np.linalg.svd(centered[:, :2] - centered[:, :2].mean(axis=0), full_matrices=False)[2][0]
    if axis[1] < 0:
        axis = -axis
    angle = np.arctan2(axis[0], axis[1])
    c, s = np.cos(angle), np.sin(angle)
    rot = np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])
    print(f"  braco endireitado em {np.degrees(angle):.1f} graus")
    return centered @ rot.T + pivot, normals @ rot.T


def main():
    name = sys.argv[1]
    cfg = SUITS[name]
    source = ROOT / "tools/art/converted" / name
    vertices, uvs, normals, faces = read_obj(source / f"{name}.obj")
    labels = split(vertices, faces, cfg)
    vertices, uvs, normals, faces, labels = cap_holes(vertices, uvs, normals, faces, labels)
    model = to_model(vertices)
    model_normals = np.stack([-normals[:, 0], -normals[:, 1], normals[:, 2]], axis=1)
    out_dir = ASSETS / "meshes/suit" / name
    if out_dir.exists():
        shutil.rmtree(out_dir)
    index = {"bones": {}}
    for part, pivot in PIVOTS.items():
        part_faces = faces[labels == part]
        if not len(part_faces):
            print(f"AVISO: {part} vazio")
            continue
        used = np.unique(part_faces)
        points, part_normals = model.copy(), model_normals.copy()
        if part.endswith("_arm"):
            points[used], part_normals[used] = straighten(model[used], model_normals[used], np.array(pivot))
        local = points - np.array(pivot)
        write_obj(out_dir / f"{part}.obj", local, uvs, part_normals, part_faces)
        index["bones"][part] = f"kn8:meshes/suit/{name}/{part}.obj"
        print(f"{part}: {len(part_faces)} triangulos")
    (ASSETS / "meshes/suit" / f"{name}.json").write_text(json.dumps(index, indent=2) + "\n", encoding="utf-8")
    image = Image.open(source / f"{name}.png").convert("RGBA")
    mask = uv_mask([source / f"{name}.obj"], image.width, image.height)
    padded = dilate(np.asarray(image)[..., :3], mask, DILATE_STEPS)
    texture = ASSETS / "textures/models/suit" / f"{name}.png"
    texture.parent.mkdir(parents=True, exist_ok=True)
    Image.fromarray(np.clip(padded, 0, 255).astype(np.uint8)).convert("RGBA").save(texture)


if __name__ == "__main__":
    main()
