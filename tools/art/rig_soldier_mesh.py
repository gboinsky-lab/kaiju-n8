#!/usr/bin/env python3
"""Rigging do Soldado 1 (malha do Meshy presa aos ossos da GeckoLib), 0.1-B / Etapa F.

Entrada: tools/art/converted/soldier_1/soldier_1_<parte>.obj (meshy_convert.py; partes da segmentacao do Meshy:
head, torso, waist, arm_left, arm_right, legs, boots) + soldier_1.png. Em metros, pes em Y = 0, frente -Z.
Saida (especie "soldier"):
  - assets/kn8/meshes/soldier.json + meshes/soldier/<osso>.obj  (body = torso + cintura; pernas+botas separadas
    pelo lado em leg_left / leg_right);
  - assets/kn8/textures/entity/soldier.png (512, com borda nas ilhas de UV: tools/art/pad_texture.py);
  - assets/kn8/geo/entity/soldier.geo.json (so ossos; pivos no pescoco, ombros, quadris; item_right na mao direita);
  - assets/kn8/animations/entity/soldier.animation.json (pernas, poses de arma por classe, ataque, tiro, dano).
A GeckoLib inverte o X do pivo ao carregar: pivos gravados com X negado. "left" = lado de X negativo (mesma
convencao em que o meshy_convert nomeou os bracos).
Uso: python3 tools/art/rig_soldier_mesh.py
"""
import json
import shutil
import sys
from pathlib import Path

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).parent))
from rig_trichonephila_mesh import read_obj, write_obj  # noqa: E402
from kaiju_art import kf  # noqa: E402
from pad_texture import DILATE_STEPS, dilate, uv_mask  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/kn8"
SOURCE = ROOT / "tools/art/converted/soldier_1"
NAME = "soldier"
PX = 16.0
TEXTURE_SIZE = 512
TOP_SLICE = 0.08
# Faixa do punho (m acima da ponta dos dedos) usada para o osso item_right.
HAND_BAND = (0.06, 0.11)
# Duracao do passo (s); a GeckoLib toca a animacao no tempo real, sem acompanhar a velocidade.
WALK_LENGTH = 0.9
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


def geo_pivot(point):
    return [round(-point[0] * PX, 3), round(point[1] * PX, 3), round(point[2] * PX, 3)]


def hand_point(mesh):
    """Centro do punho (onde a arma e segurada): faixa logo acima da ponta dos dedos."""
    vertices, _, _, faces = mesh
    used = vertices[np.unique(faces)]
    low = used[:, 1].min()
    band = used[(used[:, 1] > low + HAND_BAND[0]) & (used[:, 1] < low + HAND_BAND[1])]
    return band.mean(axis=0)


# Poses dos bracos (graus, convencao do Blockbench/GeckoLib): X negativo = levanta para a frente. Y: braco direito
# negativo / esquerdo positivo = para dentro (na frente do corpo). Z: direito negativo / esquerdo positivo = para
# dentro. Os bracos da malha ja abrem ~20 graus (pose "A"), por isso o Y das poses de arma e maior que o do vanilla.
# [SUPOSICAO] sinais de Y/Z conferidos so no simulador (tools/art/preview_soldier.py), nao em jogo.
POSES = {
    #             braco direito        braco esquerdo
    "unarmed": ([0, 0, -4], [0, 0, 4]),
    "blade": ([-28, -8, -4], [0, 0, 4]),
    "rifle": ([-48, -38, 0], [-62, 50, 0]),
    "pistol": ([-38, -26, 0], [-34, 46, 0]),
}
AIM = {
    "unarmed": ([-55, -30, 0], [-55, 30, 0]),
    "blade": ([-45, -15, 0], [-30, 20, 0]),
    "rifle": ([-88, -36, 0], [-92, 52, 0]),
    "pistol": ([-88, -36, 0], [-88, 36, 0]),
}
RECOIL = {"rifle": 8, "pistol": 16}


def add(a, b):
    return [round(x + y, 2) for x, y in zip(a, b)]


def arms_loop(right, left, length, sway):
    """Pose parada dos bracos com respiracao (sway em graus no X)."""
    half = length / 2
    return {"loop": True, "animation_length": length, "bones": {
        "arm_right": {"rotation": kf((0, right), (half, add(right, [-sway, 0, 0])), (length, right))},
        "arm_left": {"rotation": kf((0, left), (half, add(left, [-sway, 0, 0])), (length, left))},
    }}


def arms_walk(right, left, swing):
    """Bracos balancando ao andar (opostos as pernas: perna esquerda para a frente = braco direito para a frente)."""
    return {"loop": True, "animation_length": WALK_LENGTH, "bones": {
        "arm_right": {"rotation": kf((0, add(right, [-swing, 0, 0])), (WALK_LENGTH / 2, add(right, [swing, 0, 0])),
                                     (WALK_LENGTH, add(right, [-swing, 0, 0])))},
        "arm_left": {"rotation": kf((0, add(left, [swing, 0, 0])), (WALK_LENGTH / 2, add(left, [-swing, 0, 0])),
                                    (WALK_LENGTH, add(left, [swing, 0, 0])))},
    }}


def shoot(pose):
    right, left = AIM[pose]
    kick = RECOIL[pose]
    return {"animation_length": 0.4, "bones": {
        "arm_right": {"rotation": kf((0, right), (0.05, add(right, [-kick, 0, 0])), (0.4, right))},
        "arm_left": {"rotation": kf((0, left), (0.05, add(left, [-kick * 0.7, 0, 0])), (0.4, left))},
        "body": {"rotation": kf((0, [0, 0, 0]), (0.05, [-2, 0, 0]), (0.4, [0, 0, 0]))},
    }}


def animations():
    """Controllers do SoldierEntity: "movement" (pernas e tronco), "arms" (pose da arma), "action" e "reaction".
    Cada controller mexe em ossos/canais diferentes, entao as camadas se somam (andar + mirar)."""
    p = NAME + "."
    leg = 30
    anims = {
        # Pernas e tronco ------------------------------------------------------------------------------------
        p + "movement.idle": {"loop": True, "animation_length": 3.0, "bones": {
            "body": {"position": kf((0, [0, 0, 0]), (1.5, [0, -0.25, 0]), (3.0, [0, 0, 0])),
                     "rotation": kf((0, [0, 0, 0]), (1.5, [1.5, 0, 0]), (3.0, [0, 0, 0]))},
            "leg_left": {"rotation": kf((0, [0, 0, -1]))},
            "leg_right": {"rotation": kf((0, [0, 0, 1]))},
        }},
        p + "movement.walk": {"loop": True, "animation_length": WALK_LENGTH, "bones": {
            "leg_left": {"rotation": kf((0, [-leg, 0, 0]), (WALK_LENGTH / 2, [leg, 0, 0]),
                                        (WALK_LENGTH, [-leg, 0, 0]))},
            "leg_right": {"rotation": kf((0, [leg, 0, 0]), (WALK_LENGTH / 2, [-leg, 0, 0]),
                                         (WALK_LENGTH, [leg, 0, 0]))},
            "body": {"position": kf((0, [0, 0, 0]), (WALK_LENGTH / 4, [0, 0.6, 0]), (WALK_LENGTH / 2, [0, 0, 0]),
                                    (WALK_LENGTH * 3 / 4, [0, 0.6, 0]), (WALK_LENGTH, [0, 0, 0])),
                     "rotation": kf((0, [3, 4, 0]), (WALK_LENGTH / 2, [3, -4, 0]), (WALK_LENGTH, [3, 4, 0]))},
        }},
        # Golpe corpo a corpo: pico em 0,25 s (o dano sai no tick do JSON da arma, no servidor).
        p + "action.attack": {"animation_length": 0.6, "bones": {
            "arm_right": {"rotation": kf((0, [-30, -10, 0]), (0.15, [-150, -10, 10]), (0.28, [-15, -35, -10]),
                                         (0.6, [-30, -10, 0]))},
            "arm_left": {"rotation": kf((0, [-20, 15, 0]), (0.15, [-40, 20, 0]), (0.6, [-20, 15, 0]))},
            "body": {"rotation": kf((0, [0, 0, 0]), (0.15, [-4, 18, 0]), (0.28, [6, -18, 0]), (0.6, [0, 0, 0]))},
        }},
        p + "action.shoot_rifle": shoot("rifle"),
        p + "action.shoot_pistol": shoot("pistol"),
        p + "reaction.hurt": {"animation_length": 0.3, "bones": {
            "body": {"rotation": kf((0, [0, 0, 0]), (0.1, [-8, 0, 0]), (0.3, [0, 0, 0]))},
        }},
    }
    # Bracos: <pose>_ready (parado), <pose>_walk (andando) e <pose>_aim (com alvo) para cada classe de arma.
    for pose, (right, left) in POSES.items():
        firearm = pose in RECOIL
        anims[f"{p}arms.{pose}_ready"] = arms_loop(right, left, 3.0, 2)
        anims[f"{p}arms.{pose}_walk"] = arms_loop(right, left, WALK_LENGTH / 2, 3) if firearm \
            else arms_walk(right, left, 20 if pose == "unarmed" else 12)
        aim_right, aim_left = AIM[pose]
        anims[f"{p}arms.{pose}_aim"] = arms_loop(aim_right, aim_left, 2.0, 1)
    return {"format_version": "1.8.0", "animations": anims}


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
        "item_right": hand_point(meshes["arm_right"]),
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
    # Textura com borda nas ilhas e reduzida (sem mipmap no Minecraft: 1024 cintila e mostra as costuras).
    image = Image.open(SOURCE / "soldier_1.png").convert("RGBA")
    mask = uv_mask([SOURCE / "soldier_1.obj"], image.width, image.height)
    padded = dilate(np.asarray(image)[..., :3], mask, DILATE_STEPS)
    Image.fromarray(np.clip(padded, 0, 255).astype(np.uint8)).convert("RGBA") \
        .resize((TEXTURE_SIZE, TEXTURE_SIZE), Image.Resampling.BOX).save(ASSETS / f"textures/entity/{NAME}.png")
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
