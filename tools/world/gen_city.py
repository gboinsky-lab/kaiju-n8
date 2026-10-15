#!/usr/bin/env python3
"""Cidade (construcoes C2): estrutura de varias pecas por jigsaw, colocada com /place structure kn8:city.

  city/plaza        praca central 33 x 33 (fonte, arvores, monumento da Forca de Defesa); 4 ruas e 8 lotes
  city/street       rua 9 x 16 (calcadas, faixa amarela, postes) com 4 lotes dos lados
  city/crossing     cruzamento 9 x 9 com faixa de pedestre
  city/street_end   fim de rua com barreira (quando a cidade para de crescer)
  city/lot_*        lotes de frente para a rua: loja de conveniencia, restaurante de ramen, casa, predio, predio
                    alto, escritorio (torre de vidro), parque, estacionamento e predio destruido por kaiju

Lotes de 7 de largura ocupam um encaixe; os de 15 ocupam dois (o segundo encaixe nao cabe e fica so a calcada).
A cidade ainda nao nasce sozinha no mundo: isso e a etapa C3. Uso: python3 tools/world/gen_city.py
"""
from gen_base import Piece, ROAD, element
from gen_structures import DATA, dump, item

POOL = "kn8:city"
ASPHALT = "minecraft:gray_concrete"
SIDEWALK = "minecraft:smooth_stone"
CURB = "minecraft:light_gray_concrete"


def leaves():
    return {"distance": "1", "persistent": "true", "waterlogged": "false"}


def tree(b, x, z, height=5):
    b.fill(x, 1, z, x, height, z, "minecraft:oak_log", {"axis": "y"})
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            for dy in (height - 1, height):
                if abs(dx) + abs(dz) < 4 and b.get(x + dx, dy, z + dz) is None:
                    b.set(x + dx, dy, z + dz, "minecraft:oak_leaves", leaves())
    b.set(x, height + 1, z, "minecraft:oak_leaves", leaves())


def street_lamp(b, x, z):
    b.fill(x, 1, z, x, 4, z, "minecraft:polished_andesite_wall",
           {"east": "none", "north": "none", "south": "none", "up": "true", "waterlogged": "false",
            "west": "none"})
    b.set(x, 5, z, "minecraft:sea_lantern")


def street_piece():
    """Rua 9 x 16: entra em (4, 0, 0), segue em (4, 0, 15), lotes em z 3 e 11 dos dois lados."""
    b = Piece((9, 8, 16), 201, floor=ASPHALT)
    for z in range(16):
        for x in (0, 1, 7, 8):
            b.set(x, 0, z, SIDEWALK)
        b.set(2, 0, z, CURB)
        b.set(6, 0, z, CURB)
        if z % 4 < 2:
            b.set(4, 0, z, "minecraft:yellow_concrete")
    street_lamp(b, 0, 7)
    street_lamp(b, 8, 7)
    b.jigsaw(4, 0, "north", "kn8:city_street", "kn8:city_link", "minecraft:empty", ASPHALT)
    b.jigsaw(4, 15, "south", "kn8:city_link", "kn8:city_street", f"{POOL}/streets", ASPHALT)
    for z in (3, 11):
        b.jigsaw(0, z, "west", "kn8:city_slot", "kn8:city_lot", f"{POOL}/lots", SIDEWALK)
        b.jigsaw(8, z, "east", "kn8:city_slot", "kn8:city_lot", f"{POOL}/lots", SIDEWALK)
    b.finish("city/street")


def crossing_piece():
    b = Piece((9, 8, 9), 202, floor=ASPHALT)
    for x, z in ((0, 0), (8, 0), (0, 8), (8, 8)):
        b.set(x, 0, z, SIDEWALK)
    for i in range(2, 7, 2):
        for k in (1, 7):
            b.set(i, 0, k, "minecraft:white_concrete")
            b.set(k, 0, i, "minecraft:white_concrete")
    b.set(0, 1, 0, "minecraft:polished_andesite_wall",
          {"east": "none", "north": "none", "south": "none", "up": "true", "waterlogged": "false", "west": "none"})
    b.set(0, 2, 0, "minecraft:redstone_lamp", {"lit": "false"})
    b.jigsaw(4, 0, "north", "kn8:city_street", "kn8:city_link", "minecraft:empty", ASPHALT)
    for x, z, facing in ((4, 8, "south"), (0, 4, "west"), (8, 4, "east")):
        b.jigsaw(x, z, facing, "kn8:city_link", "kn8:city_street", f"{POOL}/streets", ASPHALT)
    b.finish("city/crossing")


def street_end_piece():
    b = Piece((9, 4, 4), 203, floor=ASPHALT)
    for z in range(4):
        for x in (0, 1, 7, 8):
            b.set(x, 0, z, SIDEWALK)
    for x in range(1, 8):
        b.set(x, 1, 3, "minecraft:yellow_concrete" if x % 2 else "minecraft:black_concrete")
    b.jigsaw(4, 0, "north", "kn8:city_street", "kn8:city_link", "minecraft:empty", ASPHALT)
    b.finish("city/street_end")


def plaza_piece():
    b = Piece((33, 14, 33), 204, floor="minecraft:polished_andesite")
    for x in range(33):
        for z in range(33):
            if (x + z) % 4 == 0:
                b.set(x, 0, z, "minecraft:smooth_stone")
    # Fonte no meio.
    b.fill(13, 0, 13, 19, 1, 19, "minecraft:stone_bricks")
    b.fill(14, 1, 14, 18, 1, 18, "minecraft:water", {"level": "0"})
    b.fill(16, 1, 16, 16, 3, 16, "minecraft:stone_bricks")
    b.set(16, 4, 16, "minecraft:water", {"level": "0"})
    # Monumento da Forca de Defesa: pedestal, pilar e emblema.
    b.fill(15, 1, 4, 17, 1, 6, "minecraft:polished_deepslate")
    b.fill(16, 2, 5, 16, 6, 5, "minecraft:iron_block")
    b.fill(15, 7, 5, 17, 7, 5, "minecraft:blue_concrete")
    b.set(16, 8, 5, "minecraft:lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
    for x, z in ((6, 6), (26, 6), (6, 26), (26, 26)):
        tree(b, x, z)
        for dx, dz in ((-1, 0), (1, 0), (0, -1), (0, 1)):
            b.set(x + dx, 0, z + dz, "minecraft:grass_block", {"snowy": "false"})
    for x, z, facing in ((10, 16, "east"), (22, 16, "west"), (16, 10, "south"), (16, 22, "north")):
        b.set(x, 1, z, "minecraft:spruce_stairs", {"facing": facing, "half": "bottom", "shape": "straight",
                                                   "waterlogged": "false"})
    for x, z in ((2, 2), (30, 2), (2, 30), (30, 30), (12, 12), (20, 20)):
        street_lamp(b, x, z)
    for x, z, facing in ((16, 0, "north"), (16, 32, "south"), (0, 16, "west"), (32, 16, "east")):
        b.jigsaw(x, z, facing, "kn8:city_link", "kn8:city_street", f"{POOL}/streets", "minecraft:polished_andesite")
    # Lotes da praca longe das saidas de rua (um lote de 15 em x 4 vai de -3 a 11; a rua comeca em 12).
    for x, z, facing in ((4, 0, "north"), (28, 0, "north"), (4, 32, "south"), (28, 32, "south"),
                         (0, 4, "west"), (0, 28, "west"), (32, 4, "east"), (32, 28, "east")):
        b.jigsaw(x, z, facing, "kn8:city_slot", "kn8:city_lot", f"{POOL}/lots", "minecraft:polished_andesite")
    b.finish("city/plaza")


# --- lotes (frente para a rua em z = 0; jigsaw no meio da frente) -------------------------------------------------

def lot(width, depth, height, seed, floor=SIDEWALK):
    b = Piece((width, height, depth), seed, floor=floor)
    b.jigsaw(width // 2, 0, "north", "kn8:city_lot", "kn8:city_slot", "minecraft:empty", floor)
    return b


def floors_building(b, x0, z0, x1, z1, floors, wall, window, floor_h=4, accent=None, glass_front=False):
    """Predio de varios andares: lajes a cada floor_h, janelas em faixa, escada de mao no canto de tras."""
    top = floors * floor_h
    b.fill(x0, 1, z0, x1, top, z1, wall)
    b.fill(x0 + 1, 1, z0 + 1, x1 - 1, top - 1, z1 - 1, "minecraft:air")
    for f in range(floors):
        y = f * floor_h
        if f:
            b.fill(x0 + 1, y, z0 + 1, x1 - 1, y, z1 - 1, "minecraft:smooth_stone")
        for x in range(x0 + 1, x1):
            for z in (z0, z1):
                if (x - x0) % 3 != 0 or glass_front:
                    b.fill(x, y + 2, z, x, y + floor_h - 1, z, window)
        for z in range(z0 + 1, z1):
            for x in (x0, x1):
                if (z - z0) % 3 != 0 or glass_front:
                    b.fill(x, y + 2, z, x, y + floor_h - 1, z, window)
        if accent:
            for x in range(x0, x1 + 1):
                b.set(x, y + 1, z0, accent)
        b.set(x0 + 2, y + floor_h - 1, z0 + 2, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})
    b.fill(x1 - 1, 1, z1 - 1, x1 - 1, top, z1 - 1, "minecraft:ladder", {"facing": "north", "waterlogged": "false"})
    for f in range(1, floors + 1):
        b.set(x1 - 1, f * floor_h, z1 - 1, "minecraft:ladder", {"facing": "north", "waterlogged": "false"})
    b.fill(x0, top, z0, x1, top, z1, "minecraft:gray_concrete")
    b.set(x1 - 1, top, z1 - 1, "minecraft:air")
    mid = (x0 + x1) // 2
    b.fill(mid, 1, z0, mid, 2, z0, "minecraft:air")
    return top


def lot_konbini():
    b = lot(7, 9, 7, 211)
    b.fill(0, 1, 2, 6, 4, 8, "minecraft:white_concrete")
    b.fill(1, 1, 3, 5, 3, 7, "minecraft:air")
    b.fill(1, 1, 2, 5, 3, 2, "minecraft:glass_pane")
    b.fill(3, 1, 2, 3, 2, 2, "minecraft:air")
    for x in range(7):
        b.set(x, 4, 2, "minecraft:blue_concrete" if x % 2 else "minecraft:white_concrete")
        b.set(x, 3, 1, "minecraft:green_carpet" if x % 2 else "minecraft:white_carpet")
    b.fill(0, 3, 1, 6, 3, 1, "minecraft:spruce_slab", {"type": "top", "waterlogged": "false"})
    for x in range(7):
        b.set(x, 4, 1, "minecraft:green_carpet" if x % 2 else "minecraft:white_carpet")
    for z in (4, 6):
        b.fill(1, 1, z, 2, 2, z, "minecraft:bookshelf")
    b.fill(5, 1, 4, 5, 1, 6, "minecraft:smooth_stone_slab", {"type": "top", "waterlogged": "false"})
    b.chest(5, 1, 7, "kn8:chests/city_shop", "west")
    b.fill(1, 1, 7, 2, 2, 7, "minecraft:white_concrete")
    b.set(4, 3, 5, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})
    b.set(5, 5, 6, "minecraft:iron_block")
    b.finish("city/lot_konbini")


def lot_ramen():
    b = lot(7, 9, 7, 212)
    b.fill(0, 1, 2, 6, 4, 8, "minecraft:dark_oak_planks")
    b.fill(1, 1, 3, 5, 3, 7, "minecraft:air")
    b.fill(1, 1, 2, 5, 1, 2, "minecraft:air")
    b.fill(1, 3, 2, 5, 3, 2, "minecraft:red_wool")
    b.fill(0, 4, 1, 6, 4, 2, "minecraft:red_concrete")
    for x in (0, 6):
        b.set(x, 3, 1, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})
    b.fill(1, 1, 5, 5, 1, 5, "minecraft:spruce_slab", {"type": "top", "waterlogged": "false"})
    for x in (1, 3, 5):
        b.set(x, 1, 4, "minecraft:oak_fence")
    b.set(2, 1, 7, "minecraft:smoker", {"facing": "north", "lit": "false"})
    b.set(4, 1, 7, "minecraft:water_cauldron", {"level": "3"})
    b.chest(5, 1, 7, "kn8:chests/city_shop", "north")
    b.fill(0, 5, 2, 6, 5, 8, "minecraft:dark_oak_slab", {"type": "bottom", "waterlogged": "false"})
    b.finish("city/lot_ramen")


def lot_house():
    b = lot(7, 9, 11, 213, floor="minecraft:grass_block")
    for z in range(1, 3):
        b.set(3, 0, z, "minecraft:dirt_path")
    b.set(1, 1, 1, "minecraft:poppy")
    b.set(5, 1, 1, "minecraft:dandelion")
    b.fill(0, 1, 3, 6, 7, 8, "minecraft:bricks")
    b.fill(1, 1, 4, 5, 6, 7, "minecraft:air")
    b.fill(1, 0, 4, 5, 0, 7, "minecraft:oak_planks")
    b.fill(1, 4, 4, 5, 4, 7, "minecraft:oak_planks")
    b.fill(0, 4, 3, 6, 7, 3, "minecraft:white_concrete")
    for x in (1, 5):
        b.set(x, 2, 3, "minecraft:glass_pane")
        b.set(x, 6, 3, "minecraft:glass_pane")
    b.fill(3, 1, 3, 3, 2, 3, "minecraft:air")
    b.fill(5, 1, 7, 5, 4, 7, "minecraft:ladder", {"facing": "north", "waterlogged": "false"})
    b.bed(1, 6, "south", "light_blue", y=5)
    b.chest(4, 5, 7, "kn8:chests/city_home", "north")
    b.set(1, 1, 7, "minecraft:crafting_table")
    for k, y in enumerate((8, 9, 10)):
        for x in range(7):
            for z in (3 + k, 8 - k):
                b.set(x, y, z, "minecraft:dark_oak_stairs",
                      {"facing": "south" if z == 3 + k else "north", "half": "bottom", "shape": "straight",
                       "waterlogged": "false"})
        if k == 2:
            b.fill(0, y, 6, 6, y, 6, "minecraft:dark_oak_planks")
    b.finish("city/lot_house")


def lot_apartment():
    b = lot(7, 10, 18, 214)
    top = floors_building(b, 0, 1, 6, 9, 4, "minecraft:light_gray_concrete", "minecraft:glass_pane",
                          accent="minecraft:cyan_terracotta")
    for f in range(1, 4):
        b.fill(1, f * 4 + 1, 0, 5, f * 4 + 1, 0, "minecraft:iron_bars")
    b.set(3, top + 1, 5, "minecraft:iron_block")
    b.finish("city/lot_apartment")


def lot_apartment_tall():
    b = lot(15, 12, 34, 215)
    top = floors_building(b, 0, 1, 14, 11, 8, "minecraft:white_terracotta", "minecraft:glass_pane",
                          accent="minecraft:orange_terracotta")
    for f in range(1, 8):
        for x0 in (1, 9):
            b.fill(x0, f * 4 + 1, 0, x0 + 4, f * 4 + 1, 0, "minecraft:iron_bars")
    b.fill(2, top + 1, 3, 4, top + 2, 5, "minecraft:gray_concrete")
    b.chest(3, 5, 4, "kn8:chests/city_home", "south")
    b.finish("city/lot_apartment_tall")


def lot_office():
    b = lot(15, 14, 56, 216)
    top = floors_building(b, 0, 1, 14, 13, 13, "minecraft:gray_concrete", "minecraft:light_blue_stained_glass",
                          glass_front=True)
    for x in (0, 4, 10, 14):
        for z in (1, 13):
            b.fill(x, 1, z, x, top, z, "minecraft:gray_concrete")
    b.fill(5, 1, 1, 9, 3, 1, "minecraft:glass")
    b.fill(6, 1, 1, 8, 2, 1, "minecraft:air")
    b.fill(3, top + 1, 4, 11, top + 1, 10, "minecraft:black_concrete")
    for z in range(5, 10):
        b.set(5, top + 1, z, "minecraft:yellow_concrete")
        b.set(9, top + 1, z, "minecraft:yellow_concrete")
    for x in (6, 7, 8):
        b.set(x, top + 1, 7, "minecraft:yellow_concrete")
    b.fill(13, top + 1, 12, 13, top + 6, 12, "minecraft:iron_bars")
    b.set(13, top + 7, 12, "minecraft:redstone_lamp", {"lit": "false"})
    b.chest(2, 1, 11, "kn8:chests/city_shop", "east")
    b.finish("city/lot_office")


def lot_park():
    b = lot(15, 11, 9, 217, floor="minecraft:grass_block")
    for z in range(11):
        b.set(7, 0, z, "minecraft:dirt_path")
    for x in range(15):
        b.set(x, 0, 5, "minecraft:dirt_path")
    tree(b, 3, 2)
    tree(b, 11, 8, 6)
    tree(b, 12, 2, 4)
    b.fill(2, 0, 7, 4, 0, 9, "minecraft:water", {"level": "0"})
    for x, z in ((6, 3), (8, 7)):
        b.set(x, 1, z, "minecraft:spruce_stairs", {"facing": "east" if x < 7 else "west", "half": "bottom",
                                                   "shape": "straight", "waterlogged": "false"})
    for x, z, flower in ((1, 4, "minecraft:poppy"), (13, 6, "minecraft:cornflower"), (9, 9, "minecraft:allium"),
                         (5, 1, "minecraft:dandelion")):
        b.set(x, 1, z, flower)
    street_lamp(b, 8, 4)
    b.finish("city/lot_park")


def lot_parking():
    b = lot(15, 11, 6, 218, floor=ASPHALT)
    for x in range(1, 15, 3):
        b.fill(x, 0, 4, x, 0, 9, "minecraft:white_concrete")
    for x, color in ((2, "red"), (8, "blue"), (11, "white")):
        b.fill(x, 1, 5, x + 1, 1, 8, "minecraft:black_concrete")
        b.fill(x, 2, 5, x + 1, 2, 8, f"minecraft:{color}_concrete")
        b.fill(x, 3, 6, x + 1, 3, 7, f"minecraft:{color}_concrete")
        b.fill(x, 2, 5, x + 1, 2, 5, "minecraft:glass")
    for x in range(15):
        b.set(x, 1, 10, "minecraft:oak_fence")
    b.finish("city/lot_parking")


def lot_ruined():
    b = lot(15, 12, 16, 219)
    rnd = b.random
    x0, z0, x1, z1 = 1, 2, 13, 11
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            top = int(14 - max(0, (x - 5) + (z - 5)) * 1.2 - rnd.random() * 2)
            edge = x in (x0, x1) or z in (z0, z1)
            for y in range(1, max(1, top)):
                if y % 4 == 0:
                    b.set(x, y, z, "minecraft:smooth_stone" if not edge else "minecraft:gray_concrete")
                elif edge:
                    b.set(x, y, z, "minecraft:cracked_stone_bricks" if rnd.random() < 0.3
                          else "minecraft:light_gray_concrete" if (x + z) % 3 else "minecraft:air")
    for k in range(3):
        for t in range(7):
            b.set(3 + k * 2 + t // 3, 10 - t, z0, "minecraft:air")
            b.set(4 + k * 2 + t // 3, 10 - t, z0, "minecraft:black_concrete")
    debris = ["minecraft:cobblestone", "minecraft:gravel", "minecraft:andesite", "minecraft:cracked_stone_bricks"]
    for x in range(15):
        for z in range(12):
            if (x > 8 or z > 8) and rnd.random() < 0.4:
                b.set(x, 1, z, rnd.choice(debris))
    b.set(4, 1, 5, "minecraft:cobweb")
    b.chest(3, 1, 9, "kn8:chests/ruins", "east")
    b.finish("city/lot_ruined")


LOTS = [("lot_konbini", 3), ("lot_ramen", 2), ("lot_house", 4), ("lot_apartment", 4), ("lot_apartment_tall", 3),
        ("lot_office", 2), ("lot_park", 2), ("lot_parking", 2), ("lot_ruined", 2)]
BIOMES = ["minecraft:plains", "minecraft:sunflower_plains", "minecraft:savanna", "minecraft:meadow",
          "minecraft:desert", "minecraft:snowy_plains", "minecraft:forest", "minecraft:birch_forest"]


def worldgen():
    wg = DATA / "kn8/worldgen"
    dump(DATA / "kn8/tags/worldgen/biome/has_structure/city.json", {"values": BIOMES})
    dump(wg / "template_pool/city/plaza.json", {"fallback": "minecraft:empty", "elements": [
        {"weight": 1, "element": element("kn8:city/plaza")}]})
    dump(wg / "template_pool/city/streets.json", {"fallback": "kn8:city/street_ends", "elements": [
        {"weight": 6, "element": element("kn8:city/street")},
        {"weight": 2, "element": element("kn8:city/crossing")}]})
    dump(wg / "template_pool/city/street_ends.json", {"fallback": "minecraft:empty", "elements": [
        {"weight": 1, "element": element("kn8:city/street_end")}]})
    dump(wg / "template_pool/city/lots.json", {"fallback": "minecraft:empty", "elements": [
        {"weight": w, "element": element(f"kn8:city/{name}")} for name, w in LOTS]})
    # So a estrutura (para o /place); onde e quanto ela nasce no mundo fica para a etapa C3 (structure_set).
    dump(wg / "structure/city.json", {
        "type": "minecraft:jigsaw", "biomes": "#kn8:has_structure/city", "step": "surface_structures",
        "spawn_overrides": {}, "terrain_adaptation": "beard_thin", "start_pool": "kn8:city/plaza", "size": 6,
        "start_height": {"absolute": -1}, "project_start_to_heightmap": "WORLD_SURFACE_WG",
        "max_distance_from_center": 116, "use_expansion_hack": False})
    loot = {
        "city_shop": [(2, 5), [item("minecraft:bread", 10, 1, 4), item("minecraft:cooked_chicken", 6, 1, 3),
                               item("minecraft:honey_bottle", 4, 1, 2), item("minecraft:paper", 6, 1, 4),
                               item("minecraft:emerald", 3, 1, 2), item("kn8:stamina_stim", 2)]],
        "city_home": [(1, 4), [item("minecraft:bread", 8, 1, 3), item("minecraft:book", 4, 1, 2),
                               item("minecraft:string", 5, 1, 4), item("minecraft:iron_nugget", 6, 2, 8),
                               item("minecraft:clock", 1), item("minecraft:white_wool", 4, 1, 3)]],
    }
    for name, (rolls, entries) in loot.items():
        dump(DATA / f"kn8/loot_table/chests/{name}.json", {"type": "minecraft:chest", "pools": [{
            "rolls": {"type": "minecraft:uniform", "min": rolls[0], "max": rolls[1]}, "entries": entries}]})


if __name__ == "__main__":
    (DATA / "kn8/structure/city").mkdir(parents=True, exist_ok=True)
    plaza_piece()
    street_piece()
    crossing_piece()
    street_end_piece()
    lot_konbini()
    lot_ramen()
    lot_house()
    lot_apartment()
    lot_apartment_tall()
    lot_office()
    lot_park()
    lot_parking()
    lot_ruined()
    worldgen()
