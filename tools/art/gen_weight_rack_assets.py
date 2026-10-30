#!/usr/bin/env python3
"""Arte do suporte de pesos (TREINO): bloco orientavel com frente (barra com anilhas pretas), topo (barra vista de
cima) e lados de chapa escura com faixa de aviso, mais blockstate, modelos, loot e o lugar na tag da picareta. Mesma
base da bancada/estacao/terminal. Substituivel por arte final.
Uso: python3 tools/art/gen_weight_rack_assets.py
"""
import sys
from pathlib import Path

from PIL import ImageDraw

sys.path.insert(0, str(Path(__file__).parent))
from gen_craft_assets import _frame, _hazard, _metal, _rivets, save  # noqa: E402
from gen_station_assets import write_block_files  # noqa: E402

NAME = "weight_rack"
BAR = (190, 196, 204, 255)
PLATE = (24, 26, 30, 255)
PLATE_EDGE = (70, 74, 82, 255)


def textures():
    side = _metal(31, (52, 56, 64))
    d = ImageDraw.Draw(side)
    d.rectangle([6, 1, 9, 13], fill=(40, 44, 50), outline=(80, 86, 96))  # coluna do suporte
    _rivets(side, [(1, 2), (13, 2)])
    _hazard(side, 13)
    _frame(side)

    front = _metal(32, (52, 56, 64))
    d = ImageDraw.Draw(front)
    d.rectangle([1, 2, 2, 13], fill=(40, 44, 50))  # colunas
    d.rectangle([13, 2, 14, 13], fill=(40, 44, 50))
    d.line([(0, 5), (15, 5)], fill=BAR)  # barra
    for x0 in (1, 12):  # anilhas
        d.rectangle([x0, 2, x0 + 2, 8], fill=PLATE, outline=PLATE_EDGE)
    d.rectangle([4, 10, 11, 12], fill=PLATE, outline=PLATE_EDGE)  # anilhas guardadas embaixo
    _hazard(front, 13)
    _frame(front)

    top = _metal(33, (64, 68, 78))
    d = ImageDraw.Draw(top)
    d.line([(0, 7), (15, 7)], fill=BAR)
    d.line([(0, 8), (15, 8)], fill=(120, 126, 136))
    for x0 in (1, 12):
        d.rectangle([x0, 4, x0 + 2, 11], fill=PLATE, outline=PLATE_EDGE)
    _frame(top)

    bottom = _metal(34, (40, 44, 52))
    _frame(bottom, (60, 64, 72), (24, 26, 32))
    return {f"{NAME}_side": side, f"{NAME}_front": front, f"{NAME}_top": top, f"{NAME}_bottom": bottom}


def main():
    for name, image in textures().items():
        save(image, f"textures/block/{name}.png")
    write_block_files(NAME)
    print("suporte de pesos: texturas, modelos, blockstate, loot e tag")


if __name__ == "__main__":
    main()
