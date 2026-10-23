#!/usr/bin/env python3
"""Icone do Radio de Campo (PM-10B): radio militar verde-oliva com antena longa, alto-falante e luz vermelha.
Substituivel por arte final. Uso: python3 tools/art/gen_field_radio.py
"""
import json
from pathlib import Path

from PIL import Image, ImageDraw

ASSETS = Path(__file__).resolve().parents[2] / "src/main/resources/assets/kn8"


def main():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.line([(10, 0), (10, 5)], fill=(40, 42, 40, 255))  # antena
    img.putpixel((10, 0), (90, 92, 88, 255))
    d.rectangle([4, 5, 12, 15], fill=(84, 96, 58, 255), outline=(36, 42, 26, 255))  # corpo
    for x in range(5, 12):  # brilho do topo
        img.putpixel((x, 6), (110, 124, 78, 255))
    for y in (8, 10, 12):  # grade do alto-falante
        d.line([(6, y), (10, y)], fill=(44, 50, 32, 255))
    img.putpixel((11, 8), (255, 70, 50, 255))  # luz
    d.line([(6, 14), (8, 14)], fill=(30, 34, 22, 255))  # botao
    out = ASSETS / "textures/item/field_radio.png"
    img.save(out)
    (ASSETS / "models/item/field_radio.json").write_text(json.dumps(
        {"parent": "minecraft:item/handheld", "textures": {"layer0": "kn8:item/field_radio"}}, indent=2) + "\n")
    print("radio de campo:", out)


if __name__ == "__main__":
    main()
