#!/usr/bin/env python3
"""Base comum da arte em cubos dos kaiju (GeckoLib): cubos, ossos, pintores de textura por paleta, empacotamento
do atlas, geracao do .geo.json e validacao do kit de arte.

Cada especie descreve so a forma (ossos e cubos com "materiais") e as paletas. O empacotamento depende so da
forma, entao variantes de cor (ex.: versao "ressurgida") compartilham o mesmo .geo.json e trocam so a textura.
Convencoes do kit: frente = norte (-Z), pes em Y = 0, 16 px por bloco, osso raiz "root".
"""
import json
import math
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/kn8"
FACES = ("north", "south", "east", "west", "up", "down")


def rgba(hex_color):
    hex_color = hex_color.lstrip("#")
    return tuple(int(hex_color[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def noise(x, y, seed=0):
    return ((x * 73856093) ^ (y * 19349663) ^ (seed * 83492791)) % 9


# ------------------------------------------------------------------------------------------------ pintores
def paint_hide(p, w, h, key="hide", seed=0):
    img = Image.new("RGBA", (w, h))
    base, dark, light = rgba(p[key]), rgba(p[key + "_dark"]), rgba(p[key + "_light"])
    for x in range(w):
        for y in range(h):
            n = noise(x, y, seed)
            img.putpixel((x, y), dark if n < 2 else (light if n == 8 else base))
    if p.get("crack"):
        crack = rgba(p["crack"])
        hot = rgba(p.get("crack_hot", p["crack"]))
        # Fissuras em brasa (versoes ressurgidas): linhas diagonais quebradas.
        for start in range(-h, w, 13):
            for y in range(h):
                x = start + (y * 2) // 3 + (1 if noise(start, y, 9) < 3 else 0)
                if 0 <= x < w and noise(x, y, 11) < 4:
                    img.putpixel((x, y), hot if noise(x, y, 13) == 0 else crack)
    return img


def paint_plate(p, w, h):
    img = Image.new("RGBA", (w, h))
    base, edge, dark = rgba(p["plate"]), rgba(p["plate_edge"]), rgba(p["plate_dark"])
    for x in range(w):
        for y in range(h):
            border = x == 0 or y == 0 or x == w - 1 or y == h - 1
            img.putpixel((x, y), edge if border else (dark if noise(x, y, 4) < 2 else base))
    return img


def paint_core(p, w, h):
    img = Image.new("RGBA", (w, h))
    hot, mid, edge = rgba(p["core_hot"]), rgba(p["core"]), rgba(p["core_edge"])
    cx, cy = (w - 1) / 2, (h - 1) / 2
    radius = max(1.0, min(w, h) / 2)
    for x in range(w):
        for y in range(h):
            d = math.hypot((x - cx) / max(1, w / 2), (y - cy) / max(1, h / 2))
            img.putpixel((x, y), hot if d < 0.35 else (mid if d < 0.75 else edge))
    return img


def paint_teeth(p, w, h):
    img = Image.new("RGBA", (w, h), rgba(p["mouth"]))
    tooth = rgba(p["teeth"])
    for x in range(w):
        if x % 3 != 2:
            for y in range(h):
                img.putpixel((x, y), tooth)
    return img


def paint_fin(p, w, h):
    img = Image.new("RGBA", (w, h), rgba(p["fin"]))
    rib = rgba(p["fin_dark"])
    for x in range(0, w, 2):
        for y in range(h):
            img.putpixel((x, y), rib)
    return img


def paint_gradient_v(top, bottom):
    def painter(p, w, h):
        img = Image.new("RGBA", (w, h))
        a, b = rgba(p[top]), rgba(p[bottom])
        for y in range(h):
            t = y / max(1, h - 1)
            color = tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)
            for x in range(w):
                img.putpixel((x, y), color)
        return img
    return painter


def solid(key):
    return lambda p, w, h: Image.new("RGBA", (w, h), rgba(p[key]))


MATERIALS = {
    "hide": lambda p, w, h: paint_hide(p, w, h),
    "back": lambda p, w, h: paint_hide(p, w, h, key="back", seed=2),
    "belly": lambda p, w, h: paint_hide(p, w, h, key="belly", seed=5),
    "plate": paint_plate,
    "core": paint_core,
    "teeth": paint_teeth,
    "fin": paint_fin,
    "spike": paint_gradient_v("spike_tip", "spike"),
    "horn": paint_gradient_v("horn_tip", "horn"),
    "claw": solid("claw"),
    "eye": solid("eye"),
    "mouth": solid("mouth"),
}


# ------------------------------------------------------------------------------------------------ modelo
class Cube:
    def __init__(self, origin, size, material, **faces):
        self.origin = list(origin)
        self.size = list(size)
        self.faces = {f: material for f in FACES}
        self.faces.update(faces)


def quantize(value, step=4):
    """Tamanho da regiao no atlas: arredonda para cima em multiplos de 4 px (faces menores reaproveitam a regiao,
    usando o canto superior esquerdo dela, sem esticar a textura)."""
    whole = max(1, math.ceil(value))
    if whole <= 2:
        return whole
    # Faces grandes: degraus de 8 px (mais reaproveitamento; a textura de pele e ruido, nao se nota o corte).
    step = step if whole <= 16 else 8
    return step * math.ceil(whole / step)


def region_key(material, size, face):
    w, h = face_dims(size, face)
    return (material, quantize(w), quantize(h))


def face_dims(size, face):
    sx, sy, sz = size
    return {"north": (sx, sy), "south": (sx, sy), "east": (sz, sy), "west": (sz, sy),
            "up": (sx, sz), "down": (sx, sz)}[face]


def mirror(cube):
    """Copia espelhada em X (lado esquerdo <-> direito)."""
    x, y, z = cube.origin
    copy = Cube([-(x + cube.size[0]), y, z], cube.size, "hide")
    copy.faces = dict(cube.faces)
    copy.faces["east"], copy.faces["west"] = cube.faces["west"], cube.faces["east"]
    return copy


class Model:
    def __init__(self, name, texture_size, scale=1.0):
        self.name = name
        self.texture_size = texture_size
        # Escala do modelo inteiro (Etapa C): multiplica posicoes, tamanhos e pivos no .geo.json; o atlas de textura
        # nao muda (uv_size fica igual, a textura so estica junto com o cubo).
        self.scale = scale
        self.bones = []

    def bone(self, name, parent, pivot, cubes):
        self.bones.append((name, parent, list(pivot), cubes))

    def pair(self, base_name, parent, pivot, cubes):
        """Osso do lado esquerdo (x negativo) + espelho do lado direito."""
        self.bone(base_name.format(side="left"), parent, pivot, cubes)
        self.bone(base_name.format(side="right"), parent, [-pivot[0], pivot[1], pivot[2]],
                  [mirror(c) for c in cubes])

    def keys(self):
        wanted = []
        for _, _, _, cubes in self.bones:
            for cube in cubes:
                for face, material in cube.faces.items():
                    key = region_key(material, cube.size, face)
                    if key not in wanted:
                        wanted.append(key)
        return wanted

    def layout(self):
        """Posicao de cada regiao no atlas (prateleiras). Depende so da forma: igual para todas as paletas."""
        keys = sorted(self.keys(), key=lambda k: (-k[2], -k[1], k[0]))
        size = self.texture_size
        x = y = shelf = 0
        regions = {}
        for key in keys:
            _, w, h = key
            if x + w > size:
                x, y, shelf = 0, y + shelf, 0
            if y + h > size:
                raise SystemExit(f"{self.name}: textura {size}x{size} nao comporta as regioes (parou em {key}).")
            regions[key] = (x, y)
            x += w
            shelf = max(shelf, h)
        return regions

    def texture(self, palette, regions):
        image = Image.new("RGBA", (self.texture_size, self.texture_size), (0, 0, 0, 0))
        for (material, w, h), (x, y) in regions.items():
            image.paste(MATERIALS[material](palette, w, h), (x, y))
        return image

    def geo(self, identifier, regions):
        bones = []
        for name, parent, pivot, cubes in self.bones:
            entry = {"name": name, "pivot": [round(v * self.scale, 4) for v in pivot]}
            if parent:
                entry["parent"] = parent
            if cubes:
                out = []
                for cube in cubes:
                    uv = {}
                    for face, material in cube.faces.items():
                        w, h = face_dims(cube.size, face)
                        u, v = regions[region_key(material, cube.size, face)]
                        uv[face] = {"uv": [u, v], "uv_size": [w, h]}
                    out.append({"origin": [round(v * self.scale, 4) for v in cube.origin],
                                "size": [round(v * self.scale, 4) for v in cube.size], "uv": uv})
                entry["cubes"] = out
            bones.append(entry)
        span = max(max(abs(c.origin[0]), abs(c.origin[0] + c.size[0]), abs(c.origin[2]),
                       abs(c.origin[2] + c.size[2]), c.origin[1] + c.size[1])
                   for _, _, _, cubes in self.bones for c in cubes) / 16 * self.scale
        return {"format_version": "1.12.0", "minecraft:geometry": [{
            "description": {"identifier": f"geometry.{identifier}", "texture_width": self.texture_size,
                            "texture_height": self.texture_size,
                            "visible_bounds_width": round(span * 2 + 1, 2),
                            "visible_bounds_height": round(span * 2 + 1, 2),
                            "visible_bounds_offset": [0, round(span / 2, 2), 0]},
            "bones": bones}]}


# ------------------------------------------------------------------------------------------------ animacao
def kf(*frames):
    return {(f"{t:.2f}".rstrip("0").rstrip(".") if t else "0.0"): v for t, v in frames}


def validate(model, anims, species, required, max_bones):
    names = [b[0] for b in model.bones]
    assert names[0] == "root", "o primeiro osso deve ser root"
    assert len(names) <= max_bones, f"{len(names)} ossos (limite {max_bones})"
    for anim in required:
        assert f"{species}.{anim}" in anims["animations"], f"falta {species}.{anim}"
    for anim, data in anims["animations"].items():
        for bone_name in data["bones"]:
            assert bone_name in names, f"{anim} usa osso inexistente {bone_name}"


def write(species, geo, anims, texture):
    for sub in ("geo/entity", "animations/entity", "textures/entity"):
        (ASSETS / sub).mkdir(parents=True, exist_ok=True)
    (ASSETS / f"geo/entity/{species}.geo.json").write_text(json.dumps(geo, indent=2) + "\n", encoding="utf-8")
    (ASSETS / f"animations/entity/{species}.animation.json").write_text(json.dumps(anims, indent=2) + "\n",
                                                                        encoding="utf-8")
    texture.save(ASSETS / f"textures/entity/{species}.png")


def scale_animations(anims, factor):
    """Escala os deslocamentos ("position") das animacoes junto com o modelo; rotacoes e escalas nao mudam."""
    for data in anims["animations"].values():
        for channels in data["bones"].values():
            for key, value in channels.get("position", {}).items():
                channels["position"][key] = [round(component * factor, 4) for component in value]
    return anims


def rename_animations(anims, old_prefix, new_prefix):
    """Mesmas animacoes para outra especie (variante de cor): so troca o prefixo dos nomes."""
    return {"format_version": anims["format_version"], "animations": {
        name.replace(old_prefix + ".", new_prefix + ".", 1): data for name, data in anims["animations"].items()}}
