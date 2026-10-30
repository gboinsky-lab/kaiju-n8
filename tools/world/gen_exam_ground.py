#!/usr/bin/env python3
"""Campo do Exame de Admissao (EXAME, Miguel 2026-10-30: "exame de admissao numa area propria, com armas, armadura,
municao e itens entregues no inicio"). Pela obra (cap. 2-4: a prova de selecao da Forca de Defesa acontece numa area
de testes cercada, com mata e ruinas, e os candidatos recebem os trajes e as armas no comeco): campo cercado de
61 x 61 com portao ao sul, pavilhao de entrada com o Oficial de Provas e os armarios do equipamento, torre de
observacao, refletores e a zona de prova ao norte (mata, ruinas, muro de escalada, entulho). Sem soldados: os kaiju do
exame sao do candidato. O meio da zona de prova fica aberto (os kaiju surgem a 6-14 blocos do centro).

Grava o template, o pool, a estrutura com checagem de terreno (como os locais da obra), a tag de biomas e entra no
conjunto kn8:defense_sites e na tag #kn8:defense_sites. Uso: python3 tools/world/gen_exam_ground.py
"""
import json
from pathlib import Path

from gen_structures import DATA, Build, dump, officer

SIZE = (61, 18, 61)
GATE = range(27, 34)
NAME = "exam_ground"
BIOMES = ["minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow", "minecraft:forest",
          "minecraft:birch_forest", "minecraft:savanna"]


def perimeter(b):
    last = SIZE[0] - 1
    for i in range(SIZE[0]):
        for x, z in ((i, 0), (i, last), (0, i), (last, i)):
            if z == last and x in GATE:
                continue
            b.set(x, 1, z, "minecraft:stone_bricks")
            b.set(x, 2, z, "minecraft:iron_bars")
            b.set(x, 3, z, "minecraft:iron_bars")
    # Portao: dois pilares e o arco com a faixa azul da Forca de Defesa.
    for x in (GATE.start - 1, GATE.stop):
        b.fill(x, 1, last, x, 6, last, "minecraft:polished_deepslate")
    b.fill(GATE.start - 1, 7, last, GATE.stop, 7, last, "minecraft:white_concrete")
    b.fill(GATE.start, 6, last, GATE.stop - 1, 6, last, "minecraft:blue_concrete")
    for x in (GATE.start - 1, GATE.stop):
        b.set(x, 8, last, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})


def pavilion(b):
    x0, x1, z0, z1 = 20, 40, 47, 57
    b.fill(x0, 0, z0, x1, 0, z1, "minecraft:smooth_stone")
    b.walls(x0, 1, z0, x1, 4, z1, "minecraft:white_concrete")
    b.fill(x0, 5, z0, x1, 5, z1, "minecraft:light_gray_concrete")
    b.fill(x0, 5, z0, x1, 5, z0, "minecraft:blue_concrete")
    b.fill(x0, 5, z1, x1, 5, z1, "minecraft:blue_concrete")
    for z in (z0, z1):  # portas para o portao (sul) e para a zona de prova (norte)
        b.fill(29, 1, z, 31, 3, z, "minecraft:air")
    for x in range(x0 + 2, x1 - 1, 4):  # janelas
        for z in (z0, z1):
            if not 28 <= x <= 32:
                b.fill(x, 2, z, x + 1, 3, z, "minecraft:glass_pane")
    for z in range(z0 + 2, z1 - 1, 3):
        for x in (x0, x1):
            b.set(x, 2, z, "minecraft:glass_pane")
            b.set(x, 3, z, "minecraft:glass_pane")
    # Balcao do Oficial de Provas e os armarios do equipamento (traje, armas, municao).
    b.fill(26, 1, 51, 34, 1, 51, "minecraft:smooth_stone_slab", {"type": "top", "waterlogged": "false"})
    b.set(30, 1, 51, "minecraft:air")
    b.entity(30, 1, 50, officer("exam_officer", 0.0))
    for x in range(x0 + 1, x0 + 5):
        b.set(x, 1, z0 + 1, "minecraft:barrel", {"facing": "south", "open": "false"})
        b.set(x, 2, z0 + 1, "minecraft:barrel", {"facing": "south", "open": "false"})
    for x in range(x1 - 4, x1):
        b.set(x, 1, z0 + 1, "minecraft:barrel", {"facing": "south", "open": "false"})
        b.set(x, 2, z0 + 1, "minecraft:barrel", {"facing": "south", "open": "false"})
    b.fill(x0 + 1, 1, z1 - 1, x0 + 3, 1, z1 - 1, "minecraft:spruce_slab", {"type": "bottom", "waterlogged": "false"})
    b.fill(x1 - 3, 1, z1 - 1, x1 - 1, 1, z1 - 1, "minecraft:spruce_slab", {"type": "bottom", "waterlogged": "false"})
    b.set(x0 + 1, 1, 52, "kn8:weight_rack", {"facing": "east"})
    for x in (24, 30, 36):
        b.set(x, 4, 52, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})


def tower(b):
    x0, z0 = 52, 4
    for x, z in ((x0, z0), (x0 + 4, z0), (x0, z0 + 4), (x0 + 4, z0 + 4)):
        b.fill(x, 1, z, x, 11, z, "minecraft:polished_deepslate")
    b.fill(x0 + 2, 1, z0 + 2, x0 + 2, 11, z0 + 2, "minecraft:polished_deepslate")
    b.fill(x0 + 2, 1, z0 + 3, x0 + 2, 12, z0 + 3, "minecraft:ladder", {"facing": "south", "waterlogged": "false"})
    b.fill(x0, 12, z0, x0 + 4, 12, z0 + 4, "minecraft:smooth_stone")
    b.set(x0 + 2, 12, z0 + 3, "minecraft:air")
    for i in range(5):
        for x, z in ((x0 + i, z0), (x0 + i, z0 + 4), (x0, z0 + i), (x0 + 4, z0 + i)):
            b.set(x, 13, z, "minecraft:iron_bars")
    b.set(x0 + 4, 14, z0 + 4, "minecraft:sea_lantern")


def tree(b, x, z, height):
    b.fill(x, 1, z, x, height, z, "minecraft:oak_log", {"axis": "y"})
    top = height
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            for dy in (-1, 0, 1):
                if abs(dx) + abs(dz) + abs(dy) <= 3 and (dx, dz) != (0, 0) or dy == 1 and (dx, dz) == (0, 0):
                    if b.get(x + dx, top + dy, z + dz) is None:
                        b.set(x + dx, top + dy, z + dz, "minecraft:oak_leaves",
                              {"distance": "1", "persistent": "true", "waterlogged": "false"})


def ruin(b, x0, z0, length, along_x, height):
    for i in range(length):
        x, z = (x0 + i, z0) if along_x else (x0, z0 + i)
        top = height - (b.random.random() < 0.4) - (i % 3 == 2)
        for y in range(1, top + 1):
            block = "minecraft:cracked_stone_bricks" if b.random.random() < 0.3 else "minecraft:stone_bricks"
            b.set(x, y, z, block)
    for _ in range(length):
        dx, dz = b.random.randint(-2, 2), b.random.randint(-2, 2)
        x, z = (x0 + b.random.randint(0, length - 1) + dx, z0 + dz) if along_x \
            else (x0 + dx, z0 + b.random.randint(0, length - 1) + dz)
        if b.get(x, 1, z) is None:
            b.set(x, 1, z, b.random.choice(["minecraft:cobblestone", "minecraft:gravel", "minecraft:andesite"]))


def test_zone(b):
    # Mata nas bordas da zona de prova (o meio fica aberto para os kaiju).
    for x, z, h in ((5, 6, 5), (9, 12, 6), (4, 20, 5), (8, 30, 6), (5, 38, 5), (12, 4, 5), (22, 5, 6),
                    (40, 5, 5), (47, 9, 6), (55, 18, 5), (51, 26, 6), (56, 34, 5), (50, 40, 5), (16, 42, 5),
                    (44, 42, 6)):
        tree(b, x, z, h)
    ruin(b, 16, 14, 7, True, 4)
    ruin(b, 40, 16, 6, False, 3)
    ruin(b, 20, 36, 5, False, 3)
    ruin(b, 36, 37, 6, True, 4)
    # Muro de escalada (parkour da FIX-3) com plataforma no alto.
    b.fill(26, 1, 3, 34, 8, 3, "minecraft:stone_bricks")
    b.fill(26, 9, 3, 34, 9, 4, "minecraft:smooth_stone_slab", {"type": "bottom", "waterlogged": "false"})
    # Caixas de suprimento espalhadas (cobertura dos atiradores).
    for x, z in ((14, 26), (46, 30), (24, 24), (37, 27)):
        b.set(x, 1, z, "minecraft:barrel", {"facing": "up", "open": "false"})
        b.set(x + 1, 1, z, "minecraft:barrel", {"facing": "up", "open": "false"})


def lights(b):
    for x, z in ((3, 3), (57, 3), (3, 45), (57, 45), (30, 45)):
        b.fill(x, 1, z, x, 6, z, "minecraft:polished_andesite_wall",
               {"east": "none", "north": "none", "south": "none", "up": "true", "waterlogged": "false",
                "west": "none"})
        b.set(x, 7, z, "minecraft:sea_lantern")
    for z in range(46, 60, 4):  # caminho do portao ao pavilhao
        for x in (27, 33):
            b.set(x, 1, z, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})


def exam_ground():
    b = Build(SIZE, 1104)
    b.fill(0, 0, 0, SIZE[0] - 1, 0, SIZE[2] - 1, "minecraft:grass_block", {"snowy": "false"})
    b.fill(29, 0, 44, 31, 0, SIZE[2] - 1, "minecraft:dirt_path")
    perimeter(b)
    pavilion(b)
    tower(b)
    test_zone(b)
    lights(b)
    b.save(NAME)


def worldgen():
    wg = DATA / "kn8/worldgen"
    dump(DATA / f"kn8/tags/worldgen/biome/has_structure/{NAME}.json", {"values": BIOMES})
    dump(wg / f"template_pool/{NAME}/start.json", {"fallback": "minecraft:empty", "elements": [{
        "weight": 1, "element": {"element_type": "minecraft:single_pool_element", "location": f"kn8:{NAME}",
                                 "projection": "rigid", "processors": "minecraft:empty"}}]})
    dump(wg / f"structure/{NAME}_layout.json", {
        "biomes": f"#kn8:has_structure/{NAME}", "step": "surface_structures", "spawn_overrides": {},
        "terrain_adaptation": "beard_thin", "type": "minecraft:jigsaw", "start_pool": f"kn8:{NAME}/start",
        "size": 1, "start_height": {"absolute": -1}, "project_start_to_heightmap": "WORLD_SURFACE_WG",
        "max_distance_from_center": 80, "use_expansion_hack": False})
    avoid = json.loads((wg / "structure/tachikawa_base.json").read_text(encoding="utf-8"))["avoid"]
    dump(wg / f"structure/{NAME}.json", {
        "biomes": f"#kn8:has_structure/{NAME}", "step": "surface_structures", "spawn_overrides": {},
        "terrain_adaptation": "beard_thin", "type": "kn8:checked_jigsaw", "layout": f"kn8:{NAME}_layout",
        "check_radius": 40, "check_step": 8, "max_slope": 10, "slope_radius": 32, "max_water": 0.05,
        "avoid": avoid})
    sets = wg / "structure_set/defense_sites.json"
    data = json.loads(sets.read_text(encoding="utf-8"))
    if not any(entry["structure"] == f"kn8:{NAME}" for entry in data["structures"]):
        data["structures"].append({"structure": f"kn8:{NAME}", "weight": 2})
        dump(sets, data)
    tag = DATA / "kn8/tags/worldgen/structure/defense_sites.json"
    data = json.loads(tag.read_text(encoding="utf-8"))
    if f"kn8:{NAME}" not in data["values"]:
        data["values"].append(f"kn8:{NAME}")
        dump(tag, data)
    dump(DATA / f"kn8/kn8/site/{NAME}.json", {"structure": f"kn8:{NAME}", "division": "selection"})


if __name__ == "__main__":
    exam_ground()
    worldgen()
