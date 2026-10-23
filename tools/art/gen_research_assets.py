#!/usr/bin/env python3
"""Arte do terminal de pesquisa do laboratorio (PM-8C): bloco orientavel com frente (tela com o grafico da analise e
uma amostra de tecido de kaiju), topo (tampa de vidro com amostras) e lados de chapa azulada, mais blockstate,
modelos, loot e o lugar na tag da picareta. Mesma base da bancada/estacao. Substituivel por arte final.
Uso: python3 tools/art/gen_research_assets.py
"""
import sys
from pathlib import Path

from PIL import ImageDraw

sys.path.insert(0, str(Path(__file__).parent))
from gen_craft_assets import _frame, _hazard, _metal, _rivets, save  # noqa: E402
from gen_station_assets import DATA, write_block_files  # noqa: E402

NAME = "research_terminal"
CYAN = (53, 200, 255, 255)
TISSUE = (196, 64, 96, 255)


def textures():
    side = _metal(21, (56, 66, 84))
    d = ImageDraw.Draw(side)
    for y in (4, 7, 10):  # aletas de ventilacao
        d.line([(3, y), (12, y)], fill=(36, 44, 58))
    _rivets(side, [(1, 2), (13, 2), (1, 11), (13, 11)])
    _hazard(side, 12)
    _frame(side)

    front = _metal(22, (56, 66, 84))
    d = ImageDraw.Draw(front)
    d.rectangle([2, 2, 13, 10], fill=(10, 20, 30), outline=(36, 52, 72))  # tela
    points = [(3, 8), (5, 6), (7, 7), (9, 4), (11, 5), (12, 3)]  # grafico da analise
    d.line(points, fill=CYAN)
    d.rectangle([3, 3, 5, 4], fill=TISSUE)  # amostra
    d.line([(3, 9), (12, 9)], fill=(24, 60, 80))
    _hazard(front, 12)
    _frame(front)

    top = _metal(23, (70, 80, 98))
    d = ImageDraw.Draw(top)
    d.rectangle([2, 2, 13, 13], fill=(30, 56, 72), outline=(90, 140, 170))  # tampa de vidro
    for x0, y0 in ((4, 4), (9, 5), (5, 9), (10, 10)):  # amostras
        d.rectangle([x0, y0, x0 + 1, y0 + 1], fill=TISSUE)
    top.putpixel((7, 7), CYAN)
    _frame(top)

    bottom = _metal(24, (40, 44, 52))
    _frame(bottom, (60, 64, 72), (24, 26, 32))
    return {f"{NAME}_side": side, f"{NAME}_front": front, f"{NAME}_top": top, f"{NAME}_bottom": bottom}


def main():
    for name, image in textures().items():
        save(image, f"textures/block/{name}.png")
    write_block_files(NAME)
    print("terminal de pesquisa: texturas, modelos, blockstate, loot e tag")


if __name__ == "__main__":
    main()
