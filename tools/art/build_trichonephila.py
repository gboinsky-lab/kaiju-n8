#!/usr/bin/env python3
"""[SUBSTITUIDO] A Trichonephila agora usa a malha do Meshy (tools/art/rig_trichonephila_mesh.py). Este script
so fornece as animacoes (funcao animations()); NAO rode o main, ele sobrescreveria o modelo e a textura.

Arte final (v1) da Trichonephila: modelo GeckoLib, textura 64x64 e animacoes, a partir da folha de conceito.

Kit de arte (M8): frente = norte (-Z), pes em Y = 0, 16 px por bloco, osso raiz "root", ate 20 ossos, hitbox do
JSON 1,6 x 1,2 blocos (corpo ~26 x 20 px; patas abertas passam um pouco). Animacoes obrigatorias:
movement.idle, movement.walk, action.bite (0,40 s, bote em 0,30 s), reaction.hurt, overlay.breathe.

Pose de descanso sem rotacao de osso (so cubos alinhados): evita ambiguidade de sinal de eixo entre Blockbench e
GeckoLib. As patas sao "em degrau" (femur sobe e abre, tibia desce), estilo voxel da folha.
Uso: python3 tools/art/build_trichonephila.py  (gera os 3 arquivos em assets/kn8/...).
"""
import json
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/kn8"
NAME = "trichonephila"
TEX = 64

# Paleta da folha de conceito
CHARCOAL = (43, 43, 43, 255)
BROWN = (59, 46, 40, 255)
YELLOW = (224, 172, 54, 255)
YELLOW_DARK = (186, 138, 38, 255)
BLACK = (27, 27, 27, 255)
BONE = (201, 191, 169, 255)
RED = (224, 31, 38, 255)
RED_LIGHT = (255, 120, 110, 255)
JOINT = (90, 82, 76, 255)
HAIR = (122, 110, 92, 255)
TAN = (185, 163, 134, 255)
TIP = (42, 29, 25, 255)


# --------------------------------------------------------------------------------------------- pintores
def noise(x, y, seed=0):
    return ((x * 73856093) ^ (y * 19349663) ^ (seed * 83492791)) % 7


def paint_chitin(w, h):
    img = Image.new("RGBA", (w, h))
    for x in range(w):
        for y in range(h):
            edge = x == 0 or y == 0 or x == w - 1 or y == h - 1
            img.putpixel((x, y), BONE if edge and noise(x, y, 1) == 0 else (CHARCOAL if noise(x, y) < 2 else BROWN))
    return img


def paint_plate(w, h):
    img = paint_chitin(w, h)
    cx = w // 2
    for y in range(h):
        img.putpixel((cx, y), BONE)
        if w > 4 and y % 4 == 1:
            img.putpixel((max(0, cx - 2), y), BONE)
            img.putpixel((min(w - 1, cx + 2), y), BONE)
    return img


def bands(length, period=4, yellow=2, offset=0):
    return [((i + offset) % period) < yellow for i in range(length)]


def band_color(is_yellow, x, y):
    if is_yellow:
        return YELLOW_DARK if noise(x, y, 3) == 0 else YELLOW
    return CHARCOAL if noise(x, y, 4) == 0 else BLACK


def paint_bands_v(w, h, center_line=False, offset=0):
    """Faixas horizontais (mudam ao longo de v): preto e amarelo na mesma proporcao, com borda de osso."""
    img = Image.new("RGBA", (w, h))
    pattern = bands(h, offset=offset)
    for y in range(h):
        for x in range(w):
            img.putpixel((x, y), band_color(pattern[y], x, y))
        # chevrons de osso na borda preto -> amarelo (como na folha)
        if not pattern[y] and y + 1 < h and pattern[y + 1]:
            for x in range(w):
                if abs(x - w // 2) % 3 == 1:
                    img.putpixel((x, y), BONE)
    if center_line and w >= 3:
        for y in range(h):
            img.putpixel((w // 2, y), BONE)
    return img


def paint_bands_u(w, h, offset=0):
    """Faixas verticais (mudam ao longo de u), com uma linha diagonal de osso como as rachaduras da folha."""
    img = Image.new("RGBA", (w, h))
    pattern = bands(w, offset=offset)
    for x in range(w):
        for y in range(h):
            img.putpixel((x, y), band_color(pattern[x], x, y))
    if w >= 6 and h >= 4:
        for y in range(h):
            x = (y * w) // (h * 2) + w // 4
            if 0 <= x < w:
                img.putpixel((x, y), BONE)
    return img


def paint_leg_v(w, h):
    img = Image.new("RGBA", (w, h))
    pattern = bands(h, period=4, yellow=2)
    for y in range(h):
        for x in range(w):
            color = YELLOW if pattern[y] else BLACK
            if pattern[y] and x == 0:
                color = YELLOW_DARK
            img.putpixel((x, y), color)
    return img


def paint_joint(w, h):
    img = Image.new("RGBA", (w, h))
    for x in range(w):
        for y in range(h):
            img.putpixel((x, y), HAIR if noise(x, y, 5) < 2 else JOINT)
    return img


def paint_eyes(w, h):
    """Face frontal da cabeca: oito olhos vermelhos em duas fileiras (folha: 4 + 4)."""
    img = paint_chitin(w, h)
    rows = [h // 2 - 1, h // 2 + 1]
    cols = [1, 3, w - 4, w - 2] if w >= 8 else list(range(0, w, 2))
    for ry in rows:
        for cx in cols:
            if 0 <= ry < h and 0 <= cx < w:
                img.putpixel((cx, ry), RED)
    if w >= 8 and h >= 4:
        img.putpixel((1, rows[0]), RED_LIGHT)
        img.putpixel((w - 2, rows[0]), RED_LIGHT)
    return img


def paint_fang(w, h):
    img = Image.new("RGBA", (w, h), BROWN)
    for x in range(w):
        img.putpixel((x, h - 1), TAN)
    return img


def solid(color):
    return lambda w, h: Image.new("RGBA", (w, h), color)


def paint_red_eye(w, h):
    img = Image.new("RGBA", (w, h), RED)
    img.putpixel((0, 0), RED_LIGHT)
    return img


MATERIALS = {
    "chitin": paint_chitin, "plate": paint_plate, "eyes": paint_eyes, "fang": paint_fang,
    "abd_top": lambda w, h: paint_bands_v(w, h, center_line=True),
    "abd_side": lambda w, h: paint_bands_u(w, h),
    "abd_back": lambda w, h: paint_bands_v(w, h, center_line=True, offset=2),
    "abd_bottom": solid(BROWN), "leg_v": paint_leg_v, "leg_u": lambda w, h: paint_bands_u(w, h),
    "joint": paint_joint, "tip": solid(TIP), "tan": solid(TAN), "red_eye": paint_red_eye,
}


# --------------------------------------------------------------------------------------------- geometria
class Cube:
    def __init__(self, origin, size, faces, inflate=0.0):
        self.origin, self.size, self.faces, self.inflate = origin, size, faces, inflate


def face_dims(size, face):
    sx, sy, sz = size
    return {"north": (sx, sy), "south": (sx, sy), "east": (sz, sy), "west": (sz, sy),
            "up": (sx, sz), "down": (sx, sz)}[face]


def mats(all_faces=None, **overrides):
    faces = {f: all_faces for f in ("north", "south", "east", "west", "up", "down")}
    faces.update(overrides)
    return faces


BONES = []  # (name, parent, pivot, cubes)


def bone(name, parent, pivot, cubes):
    BONES.append((name, parent, pivot, cubes))


def leg(side, index, zc, shift):
    """Pata em degrau: coxa, femur (2 degraus abrindo), joelho alto com pelos, tibia listrada, ponta."""
    sign = 1 if side == "right" else -1   # direita do kaiju = +X no Blockbench (frente = -Z)

    def box(x0, y0, z0, sx, sy, sz, faces):
        x = x0 if sign > 0 else -(x0 + sx)
        return Cube([x, y0, z0], [sx, sy, sz], faces)

    cubes = [
        box(5, 9.5, zc - 1.5, 2, 3, 3, mats("chitin")),
        box(7, 11.5, zc - 1, 3, 2, 2, mats("leg_u")),
        box(10, 13, zc + shift * 0.33 - 1, 3, 2, 2, mats("leg_u")),
        box(13, 14, zc + shift * 0.55 - 1.5, 3, 3, 3, mats("joint")),
        box(14.5, 7, zc + shift * 0.78 - 1, 2, 7, 2, mats("leg_v", up="joint")),
        box(15.5, 1, zc + shift - 1, 2, 6, 2, mats("leg_v", down="tip")),
        box(15.75, 0, zc + shift - 0.75, 1.5, 1, 1.5, mats("tip")),
    ]
    bone(f"leg_{side}_{index}", "body", [sign * 5, 10.5, zc], cubes)


def build():
    bone("root", None, [0, 0, 0], [])
    bone("body", "root", [0, 10, -6], [
        Cube([-5, 7, -12], [10, 6, 11], mats("chitin", up="plate")),
        Cube([-4, 13, -11], [8, 1, 9], mats("chitin", up="plate")),
        Cube([-2, 9, -1], [4, 3, 2], mats("chitin")),
    ])
    bone("head", "body", [0, 10, -12], [
        Cube([-4, 7.5, -16], [8, 5, 4], mats("chitin", north="eyes")),
        Cube([-3, 10, -16.5], [2, 1.5, 0.5], mats("red_eye")),
        Cube([1, 10, -16.5], [2, 1.5, 0.5], mats("red_eye")),
    ])
    for side, sign in (("left", -1), ("right", 1)):
        x = 1 if sign > 0 else -3
        bone(f"fang_{side}", "head", [sign * 2, 8.5, -15.5], [
            Cube([x, 4.5, -17], [2, 4, 2], mats("fang", up="chitin")),
            Cube([x + 0.25, 3.5, -16.75], [1.5, 1, 1.5], mats("tan")),
        ])
    bone("abdomen", "body", [0, 11, 0], [
        Cube([-8, 8, 0], [16, 10, 18], mats(north="abd_back", south="abd_back", east="abd_side",
                                            west="abd_side", up="abd_top", down="abd_bottom")),
        Cube([-6, 18, 2], [12, 2, 14], mats("abd_top", east="abd_side", west="abd_side", north="abd_back",
                                             south="abd_back")),
        Cube([-6, 10, 18], [12, 7, 2], mats("abd_back", east="abd_side", west="abd_side", up="abd_top")),
        Cube([-9, 10, 3], [1, 6, 12], mats("abd_side", up="abd_top", down="abd_bottom")),
        Cube([8, 10, 3], [1, 6, 12], mats("abd_side", up="abd_top", down="abd_bottom")),
        Cube([-6, 7, 2], [12, 1, 14], mats("abd_bottom")),
    ])
    for index, (zc, shift) in enumerate(zip([-10.5, -7.5, -4.5, -1.5], [-8, -3, 3, 8])):
        leg("left", index, zc, shift)
        leg("right", index, zc, shift)


# --------------------------------------------------------------------------------------------- atlas
def pack():
    """Empacota uma regiao por (material, largura, altura) numa textura 64x64 (prateleiras)."""
    import math
    wanted = {}
    for _, _, _, cubes in BONES:
        for cube in cubes:
            for face, material in cube.faces.items():
                w, h = face_dims(cube.size, face)
                key = (material, max(1, math.ceil(w)), max(1, math.ceil(h)))
                wanted[key] = None
    keys = sorted(wanted, key=lambda k: (-k[2], -k[1]))
    image = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
    x = y = shelf = 0
    regions = {}
    for key in keys:
        material, w, h = key
        if x + w > TEX:
            x, y, shelf = 0, y + shelf, 0
        if y + h > TEX:
            raise SystemExit(f"Textura {TEX}x{TEX} nao comporta as regioes (parou em {key}).")
        image.paste(MATERIALS[material](w, h), (x, y))
        regions[key] = (x, y)
        x += w
        shelf = max(shelf, h)
    return image, regions


def geo_json(regions):
    import math
    bones = []
    for name, parent, pivot, cubes in BONES:
        entry = {"name": name, "pivot": pivot}
        if parent:
            entry["parent"] = parent
        if cubes:
            out = []
            for cube in cubes:
                uv = {}
                for face, material in cube.faces.items():
                    w, h = face_dims(cube.size, face)
                    key = (material, max(1, math.ceil(w)), max(1, math.ceil(h)))
                    u, v = regions[key]
                    uv[face] = {"uv": [u, v], "uv_size": [w, h]}
                item = {"origin": cube.origin, "size": cube.size, "uv": uv}
                if cube.inflate:
                    item["inflate"] = cube.inflate
                out.append(item)
            entry["cubes"] = out
        bones.append(entry)
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": f"geometry.{NAME}", "texture_width": TEX, "texture_height": TEX,
                        "visible_bounds_width": 3, "visible_bounds_height": 2.5,
                        "visible_bounds_offset": [0, 0.75, 0]},
        "bones": bones}]}


# --------------------------------------------------------------------------------------------- animacoes
def kf(*frames):
    """Keyframes lineares: kf((0.0, [x, y, z]), (0.5, [...]))."""
    return {f"{t:.2f}".rstrip("0").rstrip(".") if t else "0.0": v for t, v in frames}


LEGS_A = ["leg_left_0", "leg_right_1", "leg_left_2", "leg_right_3"]
LEGS_B = ["leg_right_0", "leg_left_1", "leg_right_2", "leg_left_3"]
SWING = 14  # graus de balanco das patas na caminhada
LIFT = 1.5  # pixels que a pata sobe na fase de avanco


def walk_bones():
    bones = {"body": {"position": kf((0.0, [0, 0, 0]), (0.25, [0, 0.5, 0]), (0.5, [0, 0, 0]),
                                     (0.75, [0, 0.5, 0]), (1.0, [0, 0, 0]))}}
    for group, phase in ((LEGS_A, 1), (LEGS_B, -1)):
        for name in group:
            # Lados opostos giram com sinal oposto (espelho); os dois grupos ficam em fase oposta (marcha alternada).
            side = 1 if "right" in name else -1
            a = SWING * phase * side
            bones[name] = {
                "rotation": kf((0.0, [0, a, 0]), (0.5, [0, -a, 0]), (1.0, [0, a, 0])),
                "position": kf((0.0, [0, 0, 0]), (0.25, [0, LIFT if phase > 0 else 0, 0]), (0.5, [0, 0, 0]),
                               (0.75, [0, 0 if phase > 0 else LIFT, 0]), (1.0, [0, 0, 0])),
            }
    return bones


def animations():
    p = NAME + "."
    bite = {
        # Prepara (recua e abre as presas) ate 0,18 s; BOTE no 0,30 s (tick 6 do JSON = dano no servidor);
        # volta ao neutro em 0,40 s.
        "body": {"rotation": kf((0.0, [0, 0, 0]), (0.18, [12, 0, 0]), (0.30, [-14, 0, 0]), (0.40, [0, 0, 0])),
                 "position": kf((0.0, [0, 0, 0]), (0.18, [0, 0, 1]), (0.30, [0, 0, -3]), (0.40, [0, 0, 0]))},
        "head": {"rotation": kf((0.0, [0, 0, 0]), (0.18, [8, 0, 0]), (0.30, [-10, 0, 0]), (0.40, [0, 0, 0]))},
        "fang_left": {"rotation": kf((0.0, [0, 0, 0]), (0.18, [35, 0, 0]), (0.30, [-10, 0, 0]),
                                     (0.40, [0, 0, 0]))},
        "fang_right": {"rotation": kf((0.0, [0, 0, 0]), (0.18, [35, 0, 0]), (0.30, [-10, 0, 0]),
                                      (0.40, [0, 0, 0]))},
    }
    return {"format_version": "1.8.0", "animations": {
        p + "movement.idle": {"loop": True, "animation_length": 2.0, "bones": {
            "body": {"position": kf((0.0, [0, 0, 0]), (1.0, [0, -0.3, 0]), (2.0, [0, 0, 0]))},
            "abdomen": {"rotation": kf((0.0, [0, 0, 0]), (1.0, [2, 0, 0]), (2.0, [0, 0, 0]))},
            "fang_left": {"rotation": kf((0.0, [0, 0, 0]), (1.2, [6, 0, 0]), (1.5, [0, 0, 0]), (2.0, [0, 0, 0]))},
            "fang_right": {"rotation": kf((0.0, [0, 0, 0]), (1.2, [6, 0, 0]), (1.5, [0, 0, 0]),
                                          (2.0, [0, 0, 0]))},
        }},
        p + "movement.walk": {"loop": True, "animation_length": 1.0, "bones": walk_bones()},
        p + "action.bite": {"animation_length": 0.4, "bones": bite},
        # Opcional no kit (so especies sem habilidades usam): mesma mordida, mais lenta.
        p + "action.attack": {"animation_length": 0.6, "bones": {
            name: {channel: {f"{float(t) * 1.5:.2f}": v for t, v in keys.items()}
                   for channel, keys in channels.items()} for name, channels in bite.items()}},
        p + "reaction.hurt": {"animation_length": 0.3, "bones": {
            "body": {"rotation": kf((0.0, [0, 0, 0]), (0.08, [10, 0, 0]), (0.30, [0, 0, 0])),
                     "position": kf((0.0, [0, 0, 0]), (0.08, [0, 0, 1.5]), (0.30, [0, 0, 0]))}}},
        # Camada que toca por cima de tudo: so escala o abdomen, sem girar corpo nem patas.
        p + "overlay.breathe": {"loop": True, "animation_length": 2.5, "bones": {
            "abdomen": {"scale": kf((0.0, [1, 1, 1]), (1.25, [1.04, 1.03, 1.04]), (2.5, [1, 1, 1]))}}},
    }}


def validate(geo, anims):
    names = [b["name"] for b in geo["minecraft:geometry"][0]["bones"]]
    assert "root" in names and len(names) <= 20, "osso root ausente ou mais de 20 ossos"
    required = ["movement.idle", "movement.walk", "action.bite", "reaction.hurt", "overlay.breathe"]
    for anim in required:
        assert f"{NAME}.{anim}" in anims["animations"], f"falta {NAME}.{anim}"
    for anim, data in anims["animations"].items():
        for bone_name in data["bones"]:
            assert bone_name in names, f"{anim} usa osso inexistente {bone_name}"
    bite = anims["animations"][f"{NAME}.action.bite"]
    assert bite["animation_length"] == 0.4, "a mordida deve durar 0,40 s"
    assert "0.3" in bite["bones"]["body"]["rotation"], "o bote deve ter keyframe em 0,30 s"
    print("validacao do kit: OK")


if __name__ == "__main__" and False:  # desativado: ver docstring
    build()
    texture, regions = pack()
    print(f"ossos: {len(BONES)}  cubos: {sum(len(c) for *_, c in BONES)}  regioes: {len(regions)}")
    (ASSETS / "geo/entity").mkdir(parents=True, exist_ok=True)
    (ASSETS / "textures/entity").mkdir(parents=True, exist_ok=True)
    texture.save(ASSETS / f"textures/entity/{NAME}.png")
    geo = geo_json(regions)
    anims = animations()
    validate(geo, anims)
    (ASSETS / "animations/entity").mkdir(parents=True, exist_ok=True)
    (ASSETS / f"geo/entity/{NAME}.geo.json").write_text(json.dumps(geo, indent=2) + "\n", encoding="utf-8")
    (ASSETS / f"animations/entity/{NAME}.animation.json").write_text(json.dumps(anims, indent=2) + "\n",
                                                                     encoding="utf-8")
