#!/usr/bin/env python3
"""Rigging do Soldado 1 (malha do Meshy presa aos ossos da GeckoLib), 0.1-B / Etapa F.

Entrada: tools/art/converted/soldier_1/soldier_1_<parte>.obj (meshy_convert.py; partes da segmentacao do Meshy:
head, torso, waist, arm_left, arm_right, legs, boots) + soldier_1.png. Em metros, pes em Y = 0, frente -Z.
Saida (especie "soldier"):
  - assets/kn8/meshes/soldier.json + meshes/soldier/<osso>.obj  (body = torso + cintura; pernas+botas separadas
    pelo lado em leg_left / leg_right);
  - assets/kn8/textures/entity/soldier.png;
  - assets/kn8/geo/entity/soldier.geo.json (so ossos; pivos no pescoco, ombros, quadris; item_right na mao direita);
  - assets/kn8/animations/entity/soldier.animation.json (idle, walk, attack, shoot, hurt).
A GeckoLib inverte o X do pivo ao carregar: pivos gravados com X negado. "left" = lado de X negativo (mesma
convencao em que o meshy_convert nomeou os bracos).
Uso: python3 tools/art/rig_soldier_mesh.py
"""
import json
import shutil
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).parent))
from rig_trichonephila_mesh import read_obj, write_obj  # noqa: E402
from kaiju_art import kf  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/kn8"
SOURCE = ROOT / "tools/art/converted/soldier_1"
NAME = "soldier"
PX = 16.0
TOP_SLICE = 0.08
# Altura do quadril (m): acima = corpo, abaixo = perna do lado. A segmentacao do Meshy nao separa bem cintura/botas.
HIP_Y = 1.1


def load_part(part):
    return read_obj(SOURCE / f"soldier_1_{part}.obj")


def merge(parts):
    vertices, uvs, normals, faces = [], [], [], []
    offset = 0
    for v, t, n, f in parts:
        vertices.append(v)
        uvs.append(t)
        normals.append(n)
        faces.append(f + offset)
        offset += len(v)
    return np.vstack(vertices), np.vstack(uvs), np.vstack(normals), np.vstack(faces)


def split_trunk(mesh):
    """Tronco + cintura + pernas + botas: acima do quadril = corpo; abaixo, perna do lado (X negativo = "left",
    mesma convencao em que o meshy_convert nomeou os bracos)."""
    vertices, uvs, normals, faces = mesh
    centroid = vertices[faces].mean(axis=1)
    body = centroid[:, 1] > HIP_Y
    return {"body": (vertices, uvs, normals, faces[body]),
            "leg_left": (vertices, uvs, normals, faces[~body & (centroid[:, 0] <= 0)]),
            "leg_right": (vertices, uvs, normals, faces[~body & (centroid[:, 0] > 0)])}


def top_point(mesh, fraction=TOP_SLICE):
    vertices, _, _, faces = mesh
    used = vertices[np.unique(faces)]
    top = used[:, 1].max()
    span = top - used[:, 1].min()
    band = used[used[:, 1] > top - span * fraction]
    return np.array([band[:, 0].mean(), top - span * fraction * 0.5, band[:, 2].mean()])


def bottom_point(mesh, fraction=0.06):
    vertices, _, _, faces = mesh
    used = vertices[np.unique(faces)]
    low = used[:, 1].min()
    span = used[:, 1].max() - low
    band = used[used[:, 1] < low + span * fraction]
    return np.array([band[:, 0].mean(), low + span * fraction, band[:, 2].mean()])


def geo_pivot(point):
    return [round(-point[0] * PX, 3), round(point[1] * PX, 3), round(point[2] * PX, 3)]


def animations():
    p = NAME + "."
    swing = 25
    return {"format_version": "1.8.0", "animations": {
        p + "movement.idle": {"loop": True, "animation_length": 3.0, "bones": {
            "body": {"position": kf((0, [0, 0, 0]), (1.5, [0, -0.2, 0]), (3.0, [0, 0, 0]))},
            "arm_left": {"rotation": kf((0, [0, 0, 0]), (1.5, [0, 0, 2]), (3.0, [0, 0, 0]))},
            "arm_right": {"rotation": kf((0, [0, 0, 0]), (1.5, [0, 0, -2]), (3.0, [0, 0, 0]))},
        }},
        p + "movement.walk": {"loop": True, "animation_length": 1.0, "bones": {
            "leg_left": {"rotation": kf((0, [swing, 0, 0]), (0.5, [-swing, 0, 0]), (1.0, [swing, 0, 0]))},
            "leg_right": {"rotation": kf((0, [-swing, 0, 0]), (0.5, [swing, 0, 0]), (1.0, [-swing, 0, 0]))},
            "arm_left": {"rotation": kf((0, [-swing, 0, 0]), (0.5, [swing, 0, 0]), (1.0, [-swing, 0, 0]))},
            "arm_right": {"rotation": kf((0, [swing, 0, 0]), (0.5, [-swing, 0, 0]), (1.0, [swing, 0, 0]))},
            "body": {"position": kf((0, [0, 0, 0]), (0.25, [0, 0.5, 0]), (0.5, [0, 0, 0]), (0.75, [0, 0.5, 0]),
                                    (1.0, [0, 0, 0]))},
        }},
        # Golpe corpo a corpo: pico em 0,25 s (o dano sai no tick do JSON da arma, no servidor).
        p + "action.attack": {"animation_length": 0.6, "bones": {
            "arm_right": {"rotation": kf((0, [0, 0, 0]), (0.15, [-130, 0, 20]), (0.25, [20, 0, -10]),
                                         (0.6, [0, 0, 0]))},
            "body": {"rotation": kf((0, [0, 0, 0]), (0.15, [0, 15, 0]), (0.25, [0, -15, 0]), (0.6, [0, 0, 0]))},
        }},
        # Tiro: mira com os dois bracos e coice.
        p + "action.shoot": {"animation_length": 0.4, "bones": {
            "arm_right": {"rotation": kf((0, [-85, 0, 0]), (0.05, [-100, 0, 0]), (0.4, [-85, 0, 0]))},
            "arm_left": {"rotation": kf((0, [-80, -25, 0]), (0.05, [-95, -25, 0]), (0.4, [-80, -25, 0]))},
            "head": {"rotation": kf((0, [0, 0, 0]), (0.05, [-4, 0, 0]), (0.4, [0, 0, 0]))},
        }},
        p + "reaction.hurt": {"animation_length": 0.3, "bones": {
            "body": {"rotation": kf((0, [0, 0, 0]), (0.1, [8, 0, 0]), (0.3, [0, 0, 0]))},
            "head": {"rotation": kf((0, [0, 0, 0]), (0.1, [12, 0, 0]), (0.3, [0, 0, 0]))},
        }},
    }}


def main():
    meshes = {
        "head": load_part("head"),
        "arm_left": load_part("arm_left"),
        "arm_right": load_part("arm_right"),
    }
    meshes.update(split_trunk(merge([load_part("torso"), load_part("waist"), load_part("legs"),
                                     load_part("boots")])))
    torso = load_part("torso")
    pivots = {
        "root": np.zeros(3),
        "body": np.array([0.0, HIP_Y, 0.0]),
        "head": np.array([0.0, top_point(torso)[1], top_point(torso)[2]]),
        "arm_left": top_point(meshes["arm_left"]),
        "arm_right": top_point(meshes["arm_right"]),
        "leg_left": np.array([top_point(meshes["leg_left"])[0], HIP_Y, top_point(meshes["leg_left"])[2]]),
        "leg_right": np.array([top_point(meshes["leg_right"])[0], HIP_Y, top_point(meshes["leg_right"])[2]]),
        "item_right": bottom_point(meshes["arm_right"]),
    }
    parents = {"root": None, "body": "root", "head": "body", "arm_left": "body", "arm_right": "body",
               "leg_left": "root", "leg_right": "root", "item_right": "arm_right"}
    mesh_dir = ASSETS / "meshes" / NAME
    if mesh_dir.exists():
        shutil.rmtree(mesh_dir)
    index = {"bones": {}}
    for bone, (v, t, n, f) in meshes.items():
        write_obj(mesh_dir / f"{bone}.obj", v, t, n, f)
        index["bones"][bone] = f"kn8:meshes/{NAME}/{bone}.obj"
        print(f"{bone}: {len(f)} triangulos")
    (ASSETS / "meshes" / f"{NAME}.json").write_text(json.dumps(index, indent=2) + "\n", encoding="utf-8")
    shutil.copy(SOURCE / "soldier_1.png", ASSETS / f"textures/entity/{NAME}.png")
    bones = []
    for bone, parent in parents.items():
        entry = {"name": bone, "pivot": geo_pivot(pivots[bone])}
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
    print("pivos:", {k: np.round(v, 2).tolist() for k, v in pivots.items()})


if __name__ == "__main__":
    main()
