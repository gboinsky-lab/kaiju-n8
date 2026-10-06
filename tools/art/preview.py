#!/usr/bin/env python3
"""Previa rapida de um modelo GeckoLib (.geo.json + .png): vistas frente, lado esquerdo, tras, cima e isometrica.
Renderizacao ortografica simples (pintor), sem rotacao de osso e sem animacao. So para conferir forma e textura.
Uso: python3 tools/art/preview.py <especie> [saida.png]
"""
import json
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/kn8"
SCALE = 9


def corners(o, s, face):
    x0, y0, z0 = o
    x1, y1, z1 = o[0] + s[0], o[1] + s[1], o[2] + s[2]
    return {
        "north": [(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)],
        "south": [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)],
        "east": [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)],
        "west": [(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)],
        "up": [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)],
        "down": [(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)],
    }[face]


NORMALS = {"north": (0, 0, -1), "south": (0, 0, 1), "east": (1, 0, 0), "west": (-1, 0, 0),
           "up": (0, 1, 0), "down": (0, -1, 0)}


def view_matrix(name):
    """Retorna (projecao(x,y,z)->(sx,sy,depth), direcao de visao)."""
    if name == "front":
        return (lambda p: (-p[0], -p[1], p[2])), (0, 0, 1)
    if name == "left":
        return (lambda p: (p[2], -p[1], p[0])), (1, 0, 0)
    if name == "back":
        return (lambda p: (p[0], -p[1], -p[2])), (0, 0, -1)
    if name == "top":
        return (lambda p: (-p[0], -p[2], -p[1])), (0, -1, 0)
    yaw, pitch = math.radians(35), math.radians(30)

    def iso(p):
        # Gira em torno de Y (yaw) e inclina a camera para baixo (pitch): olha de cima, pela frente-esquerda.
        x = p[0] * math.cos(yaw) - p[2] * math.sin(yaw)
        z = p[0] * math.sin(yaw) + p[2] * math.cos(yaw)
        y = p[1] * math.cos(pitch) + z * math.sin(pitch)
        depth = z * math.cos(pitch) - p[1] * math.sin(pitch)
        return (-x, -y, depth)
    return iso, None


def render(geo, texture, view, size=(360, 300)):
    project, direction = view_matrix(view)
    faces = []
    origin_depth = project((0, 0, 0))[2]
    for bone in geo["minecraft:geometry"][0]["bones"]:
        for cube in bone.get("cubes", []):
            for face, uv in cube["uv"].items():
                # Face virada para longe da camera (normal com profundidade positiva) nao e desenhada.
                if project(NORMALS[face])[2] - origin_depth >= 0:
                    continue
                pts = [project(c) for c in corners(cube["origin"], cube["size"], face)]
                depth = sum(p[2] for p in pts) / 4
                faces.append((depth, pts, uv))
    faces.sort(key=lambda f: -f[0])
    image = Image.new("RGBA", size, (205, 207, 210, 255))
    draw = ImageDraw.Draw(image)
    cx, cy = size[0] / 2, size[1] * 0.78
    for _, pts, uv in faces:
        u0, v0 = uv["uv"]
        uw, vh = uv["uv_size"]
        nu, nv = max(1, math.ceil(abs(uw))), max(1, math.ceil(abs(vh)))
        for i in range(nu):
            for j in range(nv):
                def lerp(a, b):
                    tl, tr, br, bl = pts
                    top = [tl[k] + (tr[k] - tl[k]) * a for k in range(2)]
                    bot = [bl[k] + (br[k] - bl[k]) * a for k in range(2)]
                    return [top[k] + (bot[k] - top[k]) * b for k in range(2)]
                quad = [lerp(i / nu, j / nv), lerp((i + 1) / nu, j / nv), lerp((i + 1) / nu, (j + 1) / nv),
                        lerp(i / nu, (j + 1) / nv)]
                px = texture.getpixel((min(texture.width - 1, int(u0 + i * abs(uw) / nu)),
                                       min(texture.height - 1, int(v0 + j * abs(vh) / nv))))
                if px[3] == 0:
                    continue
                draw.polygon([(cx + q[0] * SCALE, cy + q[1] * SCALE) for q in quad], fill=px)
    return image


def main():
    global SCALE
    species = sys.argv[1]
    out = sys.argv[2] if len(sys.argv) > 2 else f"/tmp/{species}_preview.png"
    if len(sys.argv) > 3:
        SCALE = float(sys.argv[3])  # kaiju grandes: escala menor para caber no quadro
    geo = json.loads((ASSETS / f"geo/entity/{species}.geo.json").read_text())
    texture = Image.open(ASSETS / f"textures/entity/{species}.png").convert("RGBA")
    views = ["front", "left", "back", "top", "iso"]
    tiles = [render(geo, texture, v) for v in views]
    sheet = Image.new("RGBA", (360 * len(views), 300), (205, 207, 210, 255))
    for i, tile in enumerate(tiles):
        sheet.paste(tile, (360 * i, 0))
        ImageDraw.Draw(sheet).text((360 * i + 10, 10), views[i].upper(), fill=(30, 30, 30, 255))
    sheet.save(out)
    print(out)


if __name__ == "__main__":
    main()
