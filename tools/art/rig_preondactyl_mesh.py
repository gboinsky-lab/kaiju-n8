#!/usr/bin/env python3
"""Rigging do Preondactyl (0.6-E, kaiju voador do Miguel; malha do Meshy presa aos ossos da GeckoLib).

Entrada: tools/art/converted/preondactyl/preondactyl.obj + .png (meshy_convert.py; 5 m de altura, asas abertas).
Saida: meshes/preondactyl.json + um OBJ por osso, textures/entity/preondactyl.png (borda nas ilhas de UV),
geo/entity/preondactyl.geo.json (so ossos) e animations/entity/preondactyl.animation.json.

Divisao pela superficie a partir de um esqueleto medido nas vistas com grade (como o modo esqueleto do
rig_primigenius_mesh.py): sementes ao longo de cada segmento e cada vertice vai para a semente mais proxima andando
pela pele (Dijkstra na malha soldada). Asa em duas partes (ombro -> pulso -> ponta) para dobrar no bater de asas.
O modelo vem centrado pela caixa (que inclui a cauda e as asas): aqui o centro da hitbox vai para o quadril.
Uso: python3 tools/art/rig_preondactyl_mesh.py   (requer numpy, scipy e pillow)
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
NAME = "preondactyl"
TEXTURE_SIZE = 1024

# Juntas em metros, nas coordenadas do OBJ convertido (frente = -Z, X negativo = "left").
SKELETON = {
    "pelvis": [-0.1, 1.6, -0.15], "chest": [-0.1, 2.6, -0.7], "neck": [0.1, 3.1, -1.1],
    "head": [0.35, 3.9, -2.4],
    "shoulder_left": [-0.85, 3.0, -0.5], "wrist_left": [-2.8, 4.2, -0.5], "tip_left": [-4.3, 3.0, -0.5],
    "shoulder_right": [0.85, 3.0, -0.5], "wrist_right": [2.8, 4.2, -0.5], "tip_right": [4.3, 3.0, -0.5],
    "hip_left": [-0.65, 1.5, -0.2], "knee_left": [-0.85, 0.8, -0.3], "foot_left": [-0.95, 0.1, -0.4],
    "hip_right": [0.5, 1.5, -0.2], "knee_right": [0.8, 0.8, -0.3], "foot_right": [0.9, 0.1, -0.4],
    "tail_base": [-0.1, 1.6, 0.6], "tail_mid": [-1.5, 0.9, 1.9], "tail_tip": [-3.1, 0.4, 2.85],
}
SEEDS = [
    ("body", "pelvis", "chest", 0.0, 1.0), ("body", "chest", "neck", 0.0, 0.5),
    ("body", "chest", "shoulder_left", 0.0, 0.6), ("body", "chest", "shoulder_right", 0.0, 0.6),
    ("neck", "chest", "neck", 0.7, 1.0), ("neck", "neck", "head", 0.0, 0.45),
    ("head", "neck", "head", 0.7, 1.0),
    ("wing_left", "shoulder_left", "wrist_left", 0.2, 1.0), ("wing_left_tip", "wrist_left", "tip_left", 0.15, 1.0),
    ("wing_right", "shoulder_right", "wrist_right", 0.2, 1.0),
    ("wing_right_tip", "wrist_right", "tip_right", 0.15, 1.0),
    ("leg_left", "hip_left", "knee_left", 0.3, 1.0), ("leg_left", "knee_left", "foot_left", 0.0, 1.0),
    ("leg_right", "hip_right", "knee_right", 0.3, 1.0), ("leg_right", "knee_right", "foot_right", 0.0, 1.0),
    ("tail", "tail_base", "tail_mid", 0.2, 1.0), ("tail", "tail_mid", "tail_tip", 0.0, 1.0),
]
# Hierarquia (as animacoes usam estes nomes): (osso, pai, junta do pivo).
BONES = [
    ("root", None, None), ("body", "root", "pelvis"), ("neck", "body", "neck"), ("head", "neck", "head_base"),
    ("wing_left", "body", "shoulder_left"), ("wing_left_tip", "wing_left", "wrist_left"),
    ("wing_right", "body", "shoulder_right"), ("wing_right_tip", "wing_right", "wrist_right"),
    ("leg_left", "body", "hip_left"), ("leg_right", "body", "hip_right"),
    ("tail_1", "body", "tail_base"), ("tail_2", "tail_1", "tail_q1"), ("tail_3", "tail_2", "tail_q2"),
]


def label_faces(vertices, faces, skeleton):
    from scipy.sparse import coo_matrix
    from scipy.sparse.csgraph import dijkstra
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
    # Cauda em 3 pedacos pela distancia ao longo de base -> meio -> ponta.
    centers = vertices[faces].mean(axis=1)
    tail = labels == "tail"
    labels[tail] = [f"tail_{min(3, int(tail_param(p, skeleton) * 3) + 1)}" for p in centers[tail]]
    return labels


def tail_line(skeleton):
    return [skeleton["tail_base"], skeleton["tail_mid"], skeleton["tail_tip"]]


def tail_param(point, skeleton):
    line = tail_line(skeleton)
    lengths = [np.linalg.norm(line[i + 1] - line[i]) for i in range(2)]
    total, walked, best = sum(lengths), 0.0, (np.inf, 0.0)
    for i, segment in enumerate(lengths):
        direction = line[i + 1] - line[i]
        t = np.clip(np.dot(point - line[i], direction) / segment ** 2, 0, 1)
        distance = np.linalg.norm(point - (line[i] + direction * t))
        if distance < best[0]:
            best = (distance, (walked + t * segment) / total)
        walked += segment
    return best[1]


def tail_point(skeleton, fraction):
    line = tail_line(skeleton)
    lengths = [np.linalg.norm(line[i + 1] - line[i]) for i in range(2)]
    target = fraction * sum(lengths)
    for i, segment in enumerate(lengths):
        if target <= segment:
            return line[i] + (line[i + 1] - line[i]) * (target / segment)
        target -= segment
    return line[-1]


def animations():
    """Voo (bater de asas), planar, parado no chao, andar, mordida, garra, cauda, mergulho, raio, dano e
    autodestruicao. Tempos dos golpes = windup_ticks dos JSON de habilidade (ability/preondactyl_*.json)."""
    p = f"{NAME}."
    # Asa esquerda: Z positivo abaixa (conferido no preview); a direita e o espelho.
    up, down = -35, 30
    flap = {
        "wing_left": {"rotation": kf((0, [0, 0, up]), (0.35, [0, 0, down]), (0.7, [0, 0, up]))},
        "wing_right": {"rotation": kf((0, [0, 0, -up]), (0.35, [0, 0, -down]), (0.7, [0, 0, -up]))},
        "wing_left_tip": {"rotation": kf((0, [0, 0, -15]), (0.35, [0, 0, 20]), (0.7, [0, 0, -15]))},
        "wing_right_tip": {"rotation": kf((0, [0, 0, 15]), (0.35, [0, 0, -20]), (0.7, [0, 0, 15]))},
        "body": {"rotation": kf((0, [35, 0, 0]), (0.7, [35, 0, 0])),
                 "position": kf((0, [0, -2, 0]), (0.35, [0, 2, 0]), (0.7, [0, -2, 0]))},
        "neck": {"rotation": kf((0, [-25, 0, 0]), (0.7, [-25, 0, 0]))},
        "leg_left": {"rotation": kf((0, [55, 0, 0]), (0.7, [55, 0, 0]))},
        "leg_right": {"rotation": kf((0, [55, 0, 0]), (0.7, [55, 0, 0]))},
        "tail_1": {"rotation": kf((0, [-15, 0, 0]), (0.35, [-10, 0, 0]), (0.7, [-15, 0, 0]))},
    }
    glide = {
        "wing_left": {"rotation": kf((0, [0, 0, 5]), (1.0, [0, 0, 8]), (2.0, [0, 0, 5]))},
        "wing_right": {"rotation": kf((0, [0, 0, -5]), (1.0, [0, 0, -8]), (2.0, [0, 0, -5]))},
        "body": {"rotation": kf((0, [35, 0, 0]), (2.0, [35, 0, 0]))},
        "neck": {"rotation": kf((0, [-25, 0, 0]), (2.0, [-25, 0, 0]))},
        "leg_left": {"rotation": kf((0, [55, 0, 0]), (2.0, [55, 0, 0]))},
        "leg_right": {"rotation": kf((0, [55, 0, 0]), (2.0, [55, 0, 0]))},
    }
    fold = {
        "wing_left": {"rotation": kf((0, [0, -45, 25]), (1.5, [0, -45, 27]), (3.0, [0, -45, 25]))},
        "wing_right": {"rotation": kf((0, [0, 45, -25]), (1.5, [0, 45, -27]), (3.0, [0, 45, -25]))},
        "wing_left_tip": {"rotation": kf((0, [0, -100, 30]), (3.0, [0, -100, 30]))},
        "wing_right_tip": {"rotation": kf((0, [0, 100, -30]), (3.0, [0, 100, -30]))},
        "neck": {"rotation": kf((0, [0, 0, 0]), (1.5, [-3, 0, 0]), (3.0, [0, 0, 0]))},
    }
    walk = dict(fold)
    walk = {**fold,
            "leg_left": {"rotation": kf((0, [25, 0, 0]), (0.5, [-25, 0, 0]), (1.0, [25, 0, 0]))},
            "leg_right": {"rotation": kf((0, [-25, 0, 0]), (0.5, [25, 0, 0]), (1.0, [-25, 0, 0]))},
            "tail_1": {"rotation": kf((0, [0, 8, 0]), (0.5, [0, -8, 0]), (1.0, [0, 8, 0]))}}

    def strike(hit, end, bones):
        return {"animation_length": end, "bones": bones(hit, end)}

    bite = strike(0.4, 0.8, lambda h, e: {
        "neck": {"rotation": kf((0, [0, 0, 0]), (h * 0.6, [-30, 0, 0]), (h, [30, 0, 0]), (e, [0, 0, 0]))},
        "head": {"rotation": kf((0, [0, 0, 0]), (h * 0.6, [-20, 0, 0]), (h, [15, 0, 0]), (e, [0, 0, 0]))}})
    claw = strike(0.45, 0.9, lambda h, e: {
        "body": {"rotation": kf((0, [0, 0, 0]), (h * 0.6, [-25, 0, 0]), (h, [20, 0, 0]), (e, [0, 0, 0]))},
        "leg_left": {"rotation": kf((0, [0, 0, 0]), (h * 0.6, [-70, 0, 0]), (h, [-20, 0, 0]), (e, [0, 0, 0]))},
        "leg_right": {"rotation": kf((0, [0, 0, 0]), (h * 0.6, [-70, 0, 0]), (h, [-20, 0, 0]), (e, [0, 0, 0]))}})
    tail = strike(0.5, 1.0, lambda h, e: {
        "body": {"rotation": kf((0, [0, 0, 0]), (h * 0.6, [0, -40, 0]), (h, [0, 60, 0]), (e, [0, 0, 0]))},
        "tail_1": {"rotation": kf((0, [0, 0, 0]), (h * 0.6, [0, -30, 0]), (h, [0, 40, 0]), (e, [0, 0, 0]))},
        "tail_2": {"rotation": kf((0, [0, 0, 0]), (h, [0, 30, 0]), (e, [0, 0, 0]))}})
    dive = {"animation_length": 1.5, "bones": {
        "wing_left": {"rotation": kf((0, [0, 0, 30]), (0.3, [0, 30, -10]), (1.2, [0, 30, -10]), (1.5, [0, 0, 0]))},
        "wing_right": {"rotation": kf((0, [0, 0, -30]), (0.3, [0, -30, 10]), (1.2, [0, -30, 10]), (1.5, [0, 0, 0]))},
        "body": {"rotation": kf((0, [35, 0, 0]), (0.3, [70, 0, 0]), (1.2, [70, 0, 0]), (1.5, [0, 0, 0]))},
        "leg_left": {"rotation": kf((0, [55, 0, 0]), (1.0, [55, 0, 0]), (1.2, [-40, 0, 0]), (1.5, [0, 0, 0]))},
        "leg_right": {"rotation": kf((0, [55, 0, 0]), (1.0, [55, 0, 0]), (1.2, [-40, 0, 0]), (1.5, [0, 0, 0]))}}}
    beam = {"animation_length": 2.4, "bones": {
        "neck": {"rotation": kf((0, [0, 0, 0]), (0.8, [-35, 0, 0]), (1.6, [20, 0, 0]), (2.2, [20, 0, 0]),
                                (2.4, [0, 0, 0]))},
        "head": {"rotation": kf((0, [0, 0, 0]), (0.8, [-20, 0, 0]), (1.6, [10, 0, 0]), (2.4, [0, 0, 0]))},
        "wing_left": {"rotation": kf((0, [0, 0, 0]), (0.8, [0, 0, 40]), (1.6, [0, 0, 10]), (2.4, [0, 0, 0]))},
        "wing_right": {"rotation": kf((0, [0, 0, 0]), (0.8, [0, 0, -40]), (1.6, [0, 0, -10]), (2.4, [0, 0, 0]))}}}
    hurt = {"animation_length": 0.35, "bones": {
        "body": {"rotation": kf((0, [0, 0, 0]), (0.1, [-10, 0, 0]), (0.35, [0, 0, 0]))},
        "neck": {"rotation": kf((0, [0, 0, 0]), (0.1, [-15, 0, 0]), (0.35, [0, 0, 0]))}}}
    shake = {"loop": True, "animation_length": 0.2, "bones": {
        "body": {"rotation": kf((0, [0, 0, -4]), (0.1, [0, 0, 4]), (0.2, [0, 0, -4]))},
        "wing_left": {"rotation": kf((0, [0, 0, 50]), (0.1, [0, 0, 60]), (0.2, [0, 0, 50]))},
        "wing_right": {"rotation": kf((0, [0, 0, -50]), (0.1, [0, 0, -60]), (0.2, [0, 0, -50]))}}}
    return {"format_version": "1.8.0", "animations": {
        p + "movement.idle": {"loop": True, "animation_length": 3.0, "bones": fold},
        p + "movement.walk": {"loop": True, "animation_length": 1.0, "bones": walk},
        p + "movement.fly": {"loop": True, "animation_length": 0.7, "bones": flap},
        p + "movement.glide": {"loop": True, "animation_length": 2.0, "bones": glide},
        p + "action.bite": bite, p + "action.claw": claw, p + "action.tail_strike": tail,
        p + "action.dive": dive, p + "action.energy_beam": beam,
        p + "action.self_destruct": shake,
        p + "reaction.hurt": hurt,
    }}


def main():
    source = ROOT / "tools/art/converted" / NAME
    vertices, uvs, normals, faces = read_obj(source / f"{NAME}.obj")
    skeleton = {k: np.array(v, dtype=float) for k, v in SKELETON.items()}
    # Centro da hitbox no quadril (o meshy_convert centra pela caixa, que inclui asas e cauda).
    offset = np.array([skeleton["pelvis"][0], 0.0, skeleton["pelvis"][2]])
    vertices = vertices - offset
    skeleton = {k: v - offset for k, v in skeleton.items()}
    labels = label_faces(vertices, faces, skeleton)
    vertices, uvs, normals, faces, labels = cap_holes(vertices, uvs, normals, faces, labels)
    skeleton["head_base"] = skeleton["neck"] + (skeleton["head"] - skeleton["neck"]) * 0.55
    skeleton["tail_q1"] = tail_point(skeleton, 1 / 3)
    skeleton["tail_q2"] = tail_point(skeleton, 2 / 3)
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
    missing = set(labels) - {b for b, _, _ in BONES}
    assert not missing, f"faces em ossos que nao existem: {missing}"
    (ASSETS / "meshes" / f"{NAME}.json").write_text(json.dumps(index, indent=2) + "\n", encoding="utf-8")
    image = Image.open(source / f"{NAME}.png").convert("RGBA")
    mask = uv_mask([source / f"{NAME}.obj"], image.width, image.height)
    padded = dilate(np.asarray(image)[..., :3], mask, DILATE_STEPS)
    Image.fromarray(np.clip(padded, 0, 255).astype(np.uint8)).convert("RGBA") \
        .resize((TEXTURE_SIZE, TEXTURE_SIZE), Image.Resampling.BOX).save(ASSETS / f"textures/entity/{NAME}.png")
    bones = []
    for bone, parent, joint in BONES:
        entry = {"name": bone, "pivot": geo_pivot(skeleton[joint] if joint else np.zeros(3))}
        if parent:
            entry["parent"] = parent
        bones.append(entry)
    low, high = vertices.min(axis=0), vertices.max(axis=0)
    span = float(np.max(high - low))
    geo = {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": f"geometry.{NAME}", "texture_width": 16, "texture_height": 16,
                        "visible_bounds_width": round(span + 1, 1), "visible_bounds_height": round(span + 1, 1),
                        "visible_bounds_offset": [0, round(float(high[1]) / 2, 1), 0]},
        "bones": bones}]}
    (ASSETS / f"geo/entity/{NAME}.geo.json").write_text(json.dumps(geo, indent=2) + "\n", encoding="utf-8")
    (ASSETS / f"animations/entity/{NAME}.animation.json").write_text(json.dumps(animations(), indent=2) + "\n",
                                                                     encoding="utf-8")
    print("caixa:", np.round(low, 2).tolist(), np.round(high, 2).tolist())


if __name__ == "__main__":
    main()
