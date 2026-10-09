#!/usr/bin/env python3
"""Rigging do Kaiju No. 9 (0.2, Etapa 8): malha do Meshy (7,3 mil triangulos, 2 m, pose A) presa aos ossos da
GeckoLib + animacoes.

Entrada: tools/art/converted/kaiju_no9/kaiju_no9.obj + .png (meshy_convert.py). Metros, pes em Y = 0, frente -Z.
Divisao pelo esqueleto (mesmo metodo dos Primigenius: sementes nos segmentos e Dijkstra pela superficie soldada),
ossos de humanoide: body, head, arm_*/forearm_*, leg_*. Juntas medidas nas vistas (skelview) do modelo.
Saida: meshes/kaiju_no9.json + meshes/kaiju_no9/<osso>.obj, textures/entity/kaiju_no9.png,
geo/entity/kaiju_no9.geo.json (so ossos) e animations/entity/kaiju_no9.animation.json.
Uso: python3 tools/art/rig_kaiju_no9_mesh.py [especie]

0.7-E: tambem a forma preta do No. 9 (kaiju_no9_black, modelo do Miguel de 1,9 m): mesmos ossos e animacoes, juntas
proprias em SPECIES. Os ataques novos das formas saem do gen_ability_animations.py (rodar depois).
"""
import json
import shutil
import sys
from pathlib import Path

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).parent))
from kaiju_art import kf  # noqa: E402
from pad_texture import DILATE_STEPS, dilate, uv_mask  # noqa: E402
from rig_primigenius_mesh import geo_pivot, weld  # noqa: E402
from rig_soldier_mesh import cap_holes  # noqa: E402
from rig_trichonephila_mesh import read_obj, write_obj  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/kn8"
NAME = sys.argv[1] if len(sys.argv) > 1 else "kaiju_no9"
SOURCE = ROOT / "tools/art/converted" / NAME
TEXTURE_SIZE = 1024

SPECIES = {
    "kaiju_no9": {
        "pelvis": [0, 1.0, 0], "chest": [0, 1.4, 0], "neck": [0, 1.7, 0], "head": [0, 1.88, 0],
        "shoulder_left": [-0.3, 1.58, 0], "elbow_left": [-0.5, 1.15, 0], "hand_left": [-0.7, 0.8, 0],
        "shoulder_right": [0.3, 1.58, 0], "elbow_right": [0.5, 1.15, 0], "hand_right": [0.7, 0.8, 0],
        "hip_left": [-0.12, 0.95, 0], "knee_left": [-0.15, 0.5, 0], "foot_left": [-0.17, 0.05, 0],
        "hip_right": [0.12, 0.95, 0], "knee_right": [0.15, 0.5, 0], "foot_right": [0.17, 0.05, 0],
    },
    # 0.7-E: forma preta (bracos mais junto do corpo e mais baixos; cabeca chata com o sorriso a 1,75 m). Medida
    # por fatias de altura da malha convertida.
    "kaiju_no9_black": {
        "pelvis": [0, 0.95, 0], "chest": [0, 1.35, 0], "neck": [0, 1.66, 0], "head": [0, 1.82, 0],
        "shoulder_left": [-0.33, 1.5, 0], "elbow_left": [-0.43, 1.1, 0], "hand_left": [-0.48, 0.72, 0],
        "shoulder_right": [0.33, 1.5, 0], "elbow_right": [0.43, 1.1, 0], "hand_right": [0.48, 0.72, 0],
        "hip_left": [-0.13, 0.9, 0], "knee_left": [-0.18, 0.5, 0], "foot_left": [-0.25, 0.06, -0.08],
        "hip_right": [0.13, 0.9, 0], "knee_right": [0.18, 0.5, 0], "foot_right": [0.25, 0.06, -0.08],
    },
}
SKELETON = SPECIES[NAME]
# Abaixo desta altura nada e cabeca (na forma preta o caminho pela pele levava os espinhos do peito para a cabeca).
HEAD_MIN_Y = {"kaiju_no9_black": 1.62}.get(NAME)
SEEDS = [
    ("body", "pelvis", "chest", 0.0, 1.0), ("body", "chest", "neck", 0.0, 0.7),
    ("body", "chest", "shoulder_left", 0.0, 0.6), ("body", "chest", "shoulder_right", 0.0, 0.6),
    ("body", "pelvis", "hip_left", 0.0, 0.3), ("body", "pelvis", "hip_right", 0.0, 0.3),
    ("head", "neck", "head", 0.5, 1.0),
    ("arm_left", "shoulder_left", "elbow_left", 0.3, 0.85), ("forearm_left", "elbow_left", "hand_left", 0.2, 1.0),
    ("arm_right", "shoulder_right", "elbow_right", 0.3, 0.85),
    ("forearm_right", "elbow_right", "hand_right", 0.2, 1.0),
    ("leg_left", "hip_left", "knee_left", 0.1, 1.0), ("leg_left", "knee_left", "foot_left", 0.0, 1.0),
    ("leg_right", "hip_right", "knee_right", 0.1, 1.0), ("leg_right", "knee_right", "foot_right", 0.0, 1.0),
]
# (osso, pai, junta do pivo)
BONES = [("root", None, None), ("body", "root", "pelvis"), ("head", "body", "neck"),
         ("arm_left", "body", "shoulder_left"), ("forearm_left", "arm_left", "elbow_left"),
         ("arm_right", "body", "shoulder_right"), ("forearm_right", "arm_right", "elbow_right"),
         ("leg_left", "root", "hip_left"), ("leg_right", "root", "hip_right")]


def split(vertices, faces):
    from scipy.sparse import coo_matrix
    from scipy.sparse.csgraph import dijkstra
    skeleton = {k: np.array(v, dtype=float) for k, v in SKELETON.items()}
    welded = weld(vertices)
    count = welded.max() + 1
    position = np.zeros((count, 3))
    position[welded] = vertices
    wf = welded[faces]
    edges = np.vstack([wf[:, [0, 1]], wf[:, [1, 2]], wf[:, [2, 0]]])
    length = np.linalg.norm(position[edges[:, 0]] - position[edges[:, 1]], axis=1) + 1e-6
    graph = coo_matrix((length, (edges[:, 0], edges[:, 1])), shape=(count, count)).tocsr()
    seeds, owners = [], []
    for bone, start, end, t0, t1 in SEEDS:
        for t in np.linspace(t0, t1, 6):
            point = skeleton[start] + (skeleton[end] - skeleton[start]) * t
            seeds.append(int(np.argmin(np.linalg.norm(position - point, axis=1))))
            owners.append(bone)
    _, _, sources = dijkstra(graph, directed=False, indices=sorted(set(seeds)), min_only=True,
                             return_predecessors=True)
    owner_of = {seed: owner for seed, owner in zip(seeds, owners)}
    vertex_label = np.array([owner_of.get(int(src), "body") for src in sources], dtype=object)
    a, b, c = (vertex_label[wf[:, i]] for i in range(3))
    labels = np.where(b == c, b, a)
    if HEAD_MIN_Y is not None:
        low = vertices[faces].mean(axis=1)[:, 1] < HEAD_MIN_Y
        labels[(labels == "head") & low] = "body"
    return labels


def animations():
    p = NAME + "."
    anims = {
        p + "movement.idle": {"loop": True, "animation_length": 3.0, "bones": {
            "body": {"rotation": kf((0, [0, 0, 0]), (1.5, [2, 0, 0]), (3.0, [0, 0, 0]))},
            "head": {"rotation": kf((0, [0, 0, 0]), (1.0, [0, 12, 0]), (2.0, [0, -12, 0]), (3.0, [0, 0, 0]))},
            "arm_left": {"rotation": kf((0, [0, 0, 0]), (1.5, [0, 0, 3]), (3.0, [0, 0, 0]))},
            "arm_right": {"rotation": kf((0, [0, 0, 0]), (1.5, [0, 0, -3]), (3.0, [0, 0, 0]))}}},
        p + "movement.walk": {"loop": True, "animation_length": 0.8, "bones": {
            "body": {"position": kf((0, [0, 0, 0]), (0.2, [0, 0.6, 0]), (0.4, [0, 0, 0]), (0.6, [0, 0.6, 0]),
                                    (0.8, [0, 0, 0])),
                     "rotation": kf((0, [4, 0, 0]), (0.8, [4, 0, 0]))},
            "arm_left": {"rotation": kf((0, [25, 0, 0]), (0.4, [-25, 0, 0]), (0.8, [25, 0, 0]))},
            "arm_right": {"rotation": kf((0, [-25, 0, 0]), (0.4, [25, 0, 0]), (0.8, [-25, 0, 0]))},
            "leg_left": {"rotation": kf((0, [-30, 0, 0]), (0.4, [30, 0, 0]), (0.8, [-30, 0, 0]))},
            "leg_right": {"rotation": kf((0, [30, 0, 0]), (0.4, [-30, 0, 0]), (0.8, [30, 0, 0]))}}},
        # Golpe de garra (ability claw: windup 8 + active 2 ticks = impacto em 0,4 s).
        p + "action.claw": {"animation_length": 0.7, "bones": {
            "body": {"rotation": kf((0, [0, 0, 0]), (0.3, [-5, 25, 0]), (0.45, [8, -25, 0]), (0.7, [0, 0, 0]))},
            "arm_right": {"rotation": kf((0, [0, 0, 0]), (0.3, [-150, 0, 20]), (0.45, [-40, -30, -10]),
                                         (0.7, [0, 0, 0]))},
            "forearm_right": {"rotation": kf((0, [0, 0, 0]), (0.3, [-30, 0, 0]), (0.45, [0, 0, 0]),
                                             (0.7, [0, 0, 0]))},
            "arm_left": {"rotation": kf((0, [0, 0, 0]), (0.3, [-30, 0, 0]), (0.7, [0, 0, 0]))}}},
        # Investida (ability charge: 15 + 20 ticks).
        p + "action.charge": {"animation_length": 1.9, "bones": {
            "body": {"rotation": kf((0, [0, 0, 0]), (0.75, [-35, 0, 0]), (1.75, [-35, 0, 0]), (1.9, [0, 0, 0]))},
            "arm_left": {"rotation": kf((0, [0, 0, 0]), (0.75, [50, 0, 0]), (1.75, [50, 0, 0]), (1.9, [0, 0, 0]))},
            "arm_right": {"rotation": kf((0, [0, 0, 0]), (0.75, [50, 0, 0]), (1.75, [50, 0, 0]), (1.9, [0, 0, 0]))},
            "leg_left": {"rotation": kf((0, [0, 0, 0]), (0.75, [-30, 0, 0]), (1.0, [30, 0, 0]), (1.25, [-30, 0, 0]),
                                        (1.5, [30, 0, 0]), (1.75, [-30, 0, 0]), (1.9, [0, 0, 0]))},
            "leg_right": {"rotation": kf((0, [0, 0, 0]), (0.75, [30, 0, 0]), (1.0, [-30, 0, 0]), (1.25, [30, 0, 0]),
                                         (1.5, [-30, 0, 0]), (1.75, [30, 0, 0]), (1.9, [0, 0, 0]))}}},
        # Revivendo uma carcaca: bracos erguidos para a frente, maos abertas, 3 s (revive_cast_ticks 60).
        p + "action.revive": {"animation_length": 3.0, "bones": {
            "body": {"rotation": kf((0, [0, 0, 0]), (0.5, [-10, 0, 0]), (2.6, [-10, 0, 0]), (3.0, [0, 0, 0]))},
            "head": {"rotation": kf((0, [0, 0, 0]), (0.5, [20, 0, 0]), (2.6, [20, 0, 0]), (3.0, [0, 0, 0]))},
            "arm_left": {"rotation": kf((0, [0, 0, 0]), (0.5, [-70, 20, 0]), (1.5, [-80, 20, 0]),
                                        (2.6, [-70, 20, 0]), (3.0, [0, 0, 0]))},
            "arm_right": {"rotation": kf((0, [0, 0, 0]), (0.5, [-70, -20, 0]), (1.5, [-80, -20, 0]),
                                         (2.6, [-70, -20, 0]), (3.0, [0, 0, 0]))}}},
        p + "reaction.hurt": {"animation_length": 0.3, "bones": {
            "body": {"rotation": kf((0, [0, 0, 0]), (0.1, [10, 0, 0]), (0.3, [0, 0, 0]))},
            "head": {"rotation": kf((0, [0, 0, 0]), (0.1, [15, 0, 0]), (0.3, [0, 0, 0]))}}},
        p + "overlay.breathe": {"loop": True, "animation_length": 2.5, "bones": {
            "body": {"scale": kf((0, [1, 1, 1]), (1.25, [1.02, 1.0, 1.02]), (2.5, [1, 1, 1]))}}},
    }
    # "attack" e o golpe generico do KaijuEntity (sem habilidade): o mesmo da garra.
    anims[p + "action.attack"] = anims[p + "action.claw"]
    return {"format_version": "1.8.0", "animations": anims}


def main():
    vertices, uvs, normals, faces = read_obj(SOURCE / f"{NAME}.obj")
    labels = split(vertices, faces)
    vertices, uvs, normals, faces, labels = cap_holes(vertices, uvs, normals, faces, labels)
    mesh_dir = ASSETS / "meshes" / NAME
    if mesh_dir.exists():
        shutil.rmtree(mesh_dir)
    index = {"bones": {}}
    for bone, _, _ in BONES:
        part = faces[labels == bone]
        if len(part):
            write_obj(mesh_dir / f"{bone}.obj", vertices, uvs, normals, part)
            index["bones"][bone] = f"kn8:meshes/{NAME}/{bone}.obj"
            print(f"{bone}: {len(part)} triangulos")
    (ASSETS / "meshes" / f"{NAME}.json").write_text(json.dumps(index, indent=2) + "\n", encoding="utf-8")
    image = Image.open(SOURCE / f"{NAME}.png").convert("RGBA")
    mask = uv_mask([SOURCE / f"{NAME}.obj"], image.width, image.height)
    padded = dilate(np.asarray(image)[..., :3], mask, DILATE_STEPS)
    Image.fromarray(np.clip(padded, 0, 255).astype(np.uint8)).convert("RGBA") \
        .resize((TEXTURE_SIZE, TEXTURE_SIZE), Image.Resampling.BOX).save(ASSETS / f"textures/entity/{NAME}.png")
    bones = []
    for bone, parent, joint in BONES:
        entry = {"name": bone, "pivot": geo_pivot(np.array(SKELETON[joint], dtype=float) if joint else np.zeros(3))}
        if parent:
            entry["parent"] = parent
        bones.append(entry)
    geo = {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": f"geometry.{NAME}", "texture_width": 16, "texture_height": 16,
                        "visible_bounds_width": 3, "visible_bounds_height": 3, "visible_bounds_offset": [0, 1, 0]},
        "bones": bones}]}
    (ASSETS / f"geo/entity/{NAME}.geo.json").write_text(json.dumps(geo, indent=2) + "\n", encoding="utf-8")
    (ASSETS / f"animations/entity/{NAME}.animation.json").write_text(json.dumps(animations(), indent=2) + "\n",
                                                                    encoding="utf-8")


if __name__ == "__main__":
    main()
