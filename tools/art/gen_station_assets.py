#!/usr/bin/env python3
"""Arte da estacao de preparacao do esquadrao (PM-8): bloco orientavel com frente (cruz verde da enfermaria e as
barras de municao), topo (estrela da Forca de Defesa e pentes) e lados de chapa, mais blockstate, modelos, loot e o
lugar na tag da picareta. Mesma base de chapa/moldura da bancada (gen_craft_assets.py). Substituivel por arte final.
Uso: python3 tools/art/gen_station_assets.py
"""
import json
import sys
from pathlib import Path

from PIL import ImageDraw

sys.path.insert(0, str(Path(__file__).parent))
from gen_craft_assets import ASSETS, _frame, _hazard, _metal, _rivets, save, write_json  # noqa: E402

NAME = "squad_station"
DATA = ASSETS.parents[1] / "data"
GREEN = (76, 217, 100, 255)
AMMO = (232, 197, 71, 255)


def textures():
    side = _metal(11, (58, 72, 64))
    d = ImageDraw.Draw(side)
    d.rectangle([2, 3, 13, 10], outline=(36, 46, 40))
    for y in (5, 7, 9):
        d.line([(4, y), (11, y)], fill=(44, 56, 48))
    _rivets(side, [(1, 2), (13, 2), (1, 11), (13, 11)])
    _hazard(side, 12)
    _frame(side)

    front = _metal(12, (58, 72, 64))
    d = ImageDraw.Draw(front)
    d.rectangle([2, 2, 13, 10], fill=(16, 26, 20), outline=(40, 56, 46))  # tela
    d.rectangle([4, 4, 7, 5], fill=GREEN)  # cruz da enfermaria
    d.rectangle([5, 3, 6, 6], fill=GREEN)
    for i, x in enumerate((9, 11)):  # barras de municao (cheia e pela metade)
        d.rectangle([x, 3 + 2 * i, x, 8], fill=AMMO)
    d.line([(3, 8), (7, 8)], fill=(24, 70, 40))
    _hazard(front, 12)
    _frame(front)

    top = _metal(13, (78, 92, 84))
    d = ImageDraw.Draw(top)
    for x0 in (2, 6, 10):  # pentes deitados
        d.rectangle([x0, 9, x0 + 2, 13], fill=(40, 44, 50), outline=(24, 26, 30))
        top.putpixel((x0 + 1, 9), AMMO)
    for x, y in ((7, 2), (6, 4), (7, 4), (8, 4), (5, 4), (9, 4), (6, 5), (8, 5), (7, 5), (6, 6), (8, 6)):
        top.putpixel((x, y), (53, 200, 255, 255))  # estrela da Forca de Defesa
    _frame(top)

    bottom = _metal(14, (40, 44, 52))
    _frame(bottom, (60, 64, 72), (24, 26, 32))
    return {f"{NAME}_side": side, f"{NAME}_front": front, f"{NAME}_top": top, f"{NAME}_bottom": bottom}


def write_block_files(name):
    """Modelo orientavel, item, blockstate por direcao, loot e tag da picareta de um bloco de chapa."""
    write_json(f"models/block/{name}.json", {
        "parent": "minecraft:block/orientable_with_bottom",
        "textures": {"top": f"kn8:block/{name}_top", "front": f"kn8:block/{name}_front",
                     "side": f"kn8:block/{name}_side", "bottom": f"kn8:block/{name}_bottom"}})
    write_json(f"models/item/{name}.json", {"parent": f"kn8:block/{name}"})
    rotations = {"north": 0, "east": 90, "south": 180, "west": 270}
    write_json(f"blockstates/{name}.json", {"variants": {
        f"facing={facing}": ({"model": f"kn8:block/{name}", "y": y} if y else {"model": f"kn8:block/{name}"})
        for facing, y in rotations.items()}})
    loot = DATA / "kn8/loot_table/blocks" / f"{name}.json"
    loot.write_text(json.dumps({"type": "minecraft:block", "pools": [{
        "rolls": 1, "entries": [{"type": "minecraft:item", "name": f"kn8:{name}"}],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}]}, indent=2) + "\n", encoding="utf-8")
    tag = DATA / "minecraft/tags/block/mineable/pickaxe.json"
    values = json.loads(tag.read_text(encoding="utf-8"))
    if f"kn8:{name}" not in values["values"]:
        values["values"].append(f"kn8:{name}")
        tag.write_text(json.dumps(values, indent=2) + "\n", encoding="utf-8")


def main():
    for name, image in textures().items():
        save(image, f"textures/block/{name}.png")
    write_block_files(NAME)
    print("estacao de preparacao: texturas, modelos, blockstate, loot e tag")


if __name__ == "__main__":
    main()
