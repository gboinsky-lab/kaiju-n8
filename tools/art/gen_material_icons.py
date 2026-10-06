#!/usr/bin/env python3
"""Icones 16x16 placeholder dos materiais de desmonte (M11b/0.1-B) + modelos de item. Substituiveis por arte final.
Uso: python3 tools/art/gen_material_icons.py
"""
import json
from pathlib import Path

from PIL import Image, ImageDraw

ASSETS = Path(__file__).resolve().parents[2] / "src/main/resources/assets/kn8"


def tissue(d):
    d.polygon([(3, 5), (8, 2), (13, 5), (12, 12), (6, 14), (2, 10)], fill=(130, 60, 70), outline=(70, 25, 35))
    d.line([(5, 7), (10, 6)], fill=(170, 90, 100))
    d.line([(4, 10), (9, 11)], fill=(170, 90, 100))


def fiber(d):
    for i, color in enumerate([(180, 70, 70), (150, 50, 55), (200, 95, 90)]):
        d.line([(3 + i * 3, 2), (6 + i * 3, 13)], fill=color, width=2)


def fragment(d):
    d.polygon([(4, 9), (7, 3), (12, 6), (11, 12), (6, 13)], fill=(200, 70, 30), outline=(110, 30, 10))
    d.point((8, 7), fill=(255, 210, 120))


def core(d):
    d.ellipse([2, 2, 13, 13], fill=(230, 80, 25), outline=(120, 30, 10))
    d.ellipse([5, 5, 10, 10], fill=(255, 200, 110))
    d.point((7, 6), fill=(255, 255, 230))


def main():
    for name, painter in (("kaiju_tissue", tissue), ("muscle_fiber", fiber), ("core_fragment", fragment),
                          ("intact_core", core)):
        image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        painter(ImageDraw.Draw(image))
        image.save(ASSETS / f"textures/item/{name}.png")
        model = {"parent": "minecraft:item/generated", "textures": {"layer0": f"kn8:item/{name}"}}
        (ASSETS / f"models/item/{name}.json").write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")
        print(f"Gerado: {name}")


if __name__ == "__main__":
    main()
