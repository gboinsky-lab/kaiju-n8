#!/usr/bin/env python3
"""Arte do Nucleo da Base (PM-8D): bloco orientavel com frente (planta da base 3 x 3 acesa em ciano), topo (estrela
da Forca de Defesa sobre um nucleo de kaiju) e lados de chapa escura, mais blockstate, modelos, loot e a tag da
picareta. Mesma base da bancada/estacao. Substituivel por arte final.
Uso: python3 tools/art/gen_base_core_assets.py
"""
import sys
from pathlib import Path

from PIL import ImageDraw

sys.path.insert(0, str(Path(__file__).parent))
from gen_craft_assets import _frame, _hazard, _metal, _rivets, save  # noqa: E402
from gen_station_assets import write_block_files  # noqa: E402

NAME = "base_core"
CYAN = (53, 200, 255, 255)
CORE = (255, 120, 60, 255)


def textures():
    side = _metal(31, (44, 50, 62))
    _rivets(side, [(1, 2), (13, 2), (1, 11), (13, 11)])
    d = ImageDraw.Draw(side)
    d.line([(7, 3), (7, 10)], fill=(30, 90, 120))
    _hazard(side, 12)
    _frame(side)

    front = _metal(32, (44, 50, 62))
    d = ImageDraw.Draw(front)
    d.rectangle([2, 2, 13, 11], fill=(8, 16, 26), outline=(36, 52, 72))
    for gx in range(3):  # planta 3 x 3 da base
        for gy in range(3):
            x0, y0 = 3 + gx * 3, 3 + gy * 3
            color = CORE if (gx, gy) == (1, 1) else CYAN
            d.rectangle([x0, y0, x0 + 1, y0 + 1], fill=color)
    _hazard(front, 13)
    _frame(front)

    top = _metal(33, (60, 66, 80))
    d = ImageDraw.Draw(top)
    d.ellipse([4, 4, 11, 11], fill=(120, 40, 20), outline=CORE)
    for x, y in ((7, 5), (7, 6), (6, 7), (7, 7), (8, 7), (5, 7), (9, 7), (7, 8), (6, 9), (8, 9)):
        top.putpixel((x, y), CYAN)
    _frame(top)

    bottom = _metal(34, (40, 44, 52))
    _frame(bottom, (60, 64, 72), (24, 26, 32))
    return {f"{NAME}_side": side, f"{NAME}_front": front, f"{NAME}_top": top, f"{NAME}_bottom": bottom}


def main():
    for name, image in textures().items():
        save(image, f"textures/block/{name}.png")
    write_block_files(NAME)
    print("nucleo da base: texturas, modelos, blockstate, loot e tag")


if __name__ == "__main__":
    main()
