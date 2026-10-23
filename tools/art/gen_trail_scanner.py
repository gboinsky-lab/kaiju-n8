#!/usr/bin/env python3
"""Icone do Rastreador de Kaiju (PM-9): aparelho de mao com tela verde-agua, pontos do rastro em laranja e antena.
Substituivel por arte final. Uso: python3 tools/art/gen_trail_scanner.py
"""
import json
from pathlib import Path

from PIL import Image, ImageDraw

ASSETS = Path(__file__).resolve().parents[2] / "src/main/resources/assets/kn8"


def main():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.line([(11, 1), (11, 4)], fill=(150, 156, 168, 255))  # antena
    img.putpixel((11, 1), (255, 90, 60, 255))
    d.rectangle([3, 4, 13, 14], fill=(46, 52, 64, 255), outline=(20, 22, 28, 255))  # corpo
    d.rectangle([5, 6, 11, 10], fill=(12, 40, 44, 255))  # tela
    for x, y in ((6, 9), (7, 8), (8, 8), (9, 7), (10, 7)):  # rastro
        img.putpixel((x, y), (255, 176, 48, 255))
    img.putpixel((10, 6), (255, 80, 60, 255))  # kaiju
    d.line([(5, 12), (7, 12)], fill=(76, 217, 100, 255))  # botoes
    d.point([(10, 12), (11, 12)], fill=(53, 200, 255, 255))
    for x in range(4, 13):  # brilho da borda
        img.putpixel((x, 5), (78, 86, 102, 255))
    out = ASSETS / "textures/item/trail_scanner.png"
    img.save(out)
    (ASSETS / "models/item/trail_scanner.json").write_text(json.dumps(
        {"parent": "minecraft:item/handheld", "textures": {"layer0": "kn8:item/trail_scanner"}}, indent=2) + "\n")
    print("rastreador de kaiju:", out)


if __name__ == "__main__":
    main()
