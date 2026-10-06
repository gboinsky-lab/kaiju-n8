#!/usr/bin/env python3
"""Icones da HUD (Etapa B, referencia "HUD de combate avancado - estilo anime"): atlas 64x32 em
textures/gui/hud_icons.png. Pode ser substituido por arte propria mantendo as posicoes:
  emblema 32x32 em (0, 0); corredor (stamina) 12x12 em (32, 0); termometro (heat) 12x12 em (44, 0).
Uso: python3 tools/art/gen_hud_icons.py
"""
from pathlib import Path

from PIL import Image, ImageDraw

OUT = Path(__file__).resolve().parents[2] / "src/main/resources/assets/kn8/textures/gui/hud_icons.png"
LIGHT = (220, 228, 236, 255)
MID = (150, 162, 176, 255)
DARK = (60, 70, 84, 255)
GREEN = (120, 220, 130, 255)
RED = (235, 70, 60, 255)


def emblem(draw):
    # Hexagono externo.
    hexagon = [(16, 1), (29, 8), (29, 23), (16, 30), (3, 23), (3, 8)]
    draw.polygon(hexagon, outline=MID)
    # Cabeca de kaiju estilizada (original): cranio angular, chifres para tras, olhos em fenda, mandibula.
    head = [(16, 7), (22, 10), (24, 16), (21, 23), (16, 25), (11, 23), (8, 16), (10, 10)]
    draw.polygon(head, fill=DARK, outline=LIGHT)
    draw.line([(10, 10), (6, 6)], fill=LIGHT)
    draw.line([(22, 10), (26, 6)], fill=LIGHT)
    draw.line([(11, 15), (14, 16)], fill=LIGHT)
    draw.line([(21, 15), (18, 16)], fill=LIGHT)
    draw.line([(13, 21), (16, 22), (19, 21)], fill=MID)
    draw.line([(16, 8), (16, 13)], fill=MID)


def runner(draw, ox):
    pixels = [".......LL...", ".......LL...", ".....LLL....", "...LL.LLL...", "......LL.LL.",
              ".....LLL....", "....L...L...", "...L.....L..", "..L.....L...", ".L..........",
              "............", "............"]
    for y, row in enumerate(pixels):
        for x, char in enumerate(row):
            if char == "L":
                draw.point((ox + x, y), fill=GREEN)


def thermometer(draw, ox):
    draw.rectangle([ox + 4, 0, ox + 6, 7], outline=LIGHT)
    draw.rectangle([ox + 5, 3, ox + 5, 8], fill=RED)
    draw.ellipse([ox + 2, 7, ox + 8, 11], fill=RED, outline=LIGHT)


def main():
    image = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    emblem(draw)
    runner(draw, 32)
    thermometer(draw, 44)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    image.save(OUT)
    print(f"Gerado: {OUT}")


if __name__ == "__main__":
    main()
