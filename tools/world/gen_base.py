#!/usr/bin/env python3
"""Base da Forca de Defesa (construcoes, etapa C1): estrutura de varias pecas por jigsaw.

  defense_base/core      centro 25x25: quartel-general de 3 andares (bancada, sala de comando, sala do capitao,
                         heliporto no telhado), praca com bandeira e quatro saidas
  defense_base/wing_*    alas 25x20 sorteadas em cada saida: alojamento, arsenal, treino (com pista de parkour),
                         hangar e enfermaria; cada uma fecha o seu lado com o muro e um portao
  defense_base/corner_*  cantos 20x20 entre duas alas: muro nos dois lados de fora e torre de vigia

A base inteira tem ~65 x 65 blocos. Pecas encaixam por blocos jigsaw no chao (y = 0, viram o piso ao gerar); o
canto e tentado pelas duas alas vizinhas e so a primeira cabe. Uso: python3 tools/world/gen_base.py
Ver no jogo: /place structure kn8:defense_base   (ou /place jigsaw kn8:defense_base/core kn8:base_core 3 ~ ~ ~)
"""
from gen_structures import DATA, Build, Float, dump, item, soldier

FLOOR = "minecraft:gray_concrete"
ROAD = "minecraft:light_gray_concrete"
POOL = "kn8:defense_base"


class Piece(Build):
    """Peca de base: chao de concreto e ar ate o teto (limpa arvores e terreno onde a peca fica)."""

    def __init__(self, size, seed, floor=FLOOR):
        super().__init__(size, seed)
        self.floor_block = floor

    def jigsaw(self, x, z, facing, name, target, pool, final=FLOOR):
        self.set(x, 0, z, "minecraft:jigsaw", {"orientation": f"{facing}_up"},
                 {"id": "minecraft:jigsaw", "name": name, "target": target, "pool": pool, "final_state": final,
                  "joint": "aligned", "placement_priority": 0, "selection_priority": 0})

    def finish(self, name):
        w, h, d = self.size
        for x in range(w):
            for z in range(d):
                if (x, 0, z) not in self.blocks:
                    self.set(x, 0, z, self.floor_block)
                for y in range(1, h):
                    if (x, y, z) not in self.blocks:
                        self.set(x, y, z, "minecraft:air")
        self.save(name)

    def wall_line(self, x0, z0, x1, z1, gate=None):
        """Muro de 4 de altura (concreto, tijolo de barro, grade) de (x0, z0) a (x1, z1); gate = (de, ate) no eixo."""
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for z in range(min(z0, z1), max(z0, z1) + 1):
                along = x if z0 == z1 else z
                if gate and gate[0] <= along <= gate[1]:
                    self.set(x, 0, z, "minecraft:yellow_concrete" if along % 2 else "minecraft:black_concrete")
                    continue
                self.set(x, 1, z, "minecraft:light_gray_concrete")
                self.set(x, 2, z, "minecraft:light_gray_concrete")
                self.set(x, 3, z, "minecraft:mud_bricks" if along % 2 else "minecraft:packed_mud")
                self.set(x, 4, z, "minecraft:iron_bars")

    def lamp_post(self, x, z):
        self.fill(x, 1, z, x, 3, z, "minecraft:polished_andesite")
        self.set(x, 4, z, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})

    def bed(self, x, z, facing="south", color="red", y=1):
        dx, dz = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}[facing]
        props = {"facing": facing, "occupied": "false"}
        self.set(x, y, z, f"minecraft:{color}_bed", dict(props, part="foot"))
        self.set(x + dx, y, z + dz, f"minecraft:{color}_bed", dict(props, part="head"))

    def building(self, x0, z0, x1, z1, height, wall="minecraft:white_concrete", stripe="minecraft:cyan_concrete",
                 roof="minecraft:gray_concrete", door=None, inner_floor="minecraft:polished_deepslate"):
        """Predio retangular: paredes, faixa, janelas a cada 3 blocos, laje; door = (lado, de, ate)."""
        self.fill(x0, 1, z0, x1, height, z1, wall)
        self.fill(x0 + 1, 1, z0 + 1, x1 - 1, height - 1, z1 - 1, "minecraft:air")
        self.fill(x0 + 1, 0, z0 + 1, x1 - 1, 0, z1 - 1, inner_floor)
        for x in range(x0, x1 + 1):
            for z in (z0, z1):
                self.set(x, height - 1, z, stripe)
                if 0 < x - x0 < x1 - x0 and (x - x0) % 3 == 1:
                    self.set(x, 2, z, "minecraft:light_gray_stained_glass_pane")
        for z in range(z0, z1 + 1):
            for x in (x0, x1):
                self.set(x, height - 1, z, stripe)
                if 0 < z - z0 < z1 - z0 and (z - z0) % 3 == 1:
                    self.set(x, 2, z, "minecraft:light_gray_stained_glass_pane")
        self.fill(x0, height, z0, x1, height, z1, roof)
        if door:
            side, a, b = door
            for v in range(a, b + 1):
                pos = {"north": (v, z0), "south": (v, z1), "west": (x0, v), "east": (x1, v)}[side]
                self.set(pos[0], 1, pos[1], "minecraft:air")
                self.set(pos[0], 2, pos[1], "minecraft:air")
        for x in range(x0 + 2, x1 - 1, 4):
            for z in range(z0 + 2, z1 - 1, 4):
                self.set(x, height - 1, z, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})


def stairs(facing):
    return {"facing": facing, "half": "bottom", "shape": "straight", "waterlogged": "false"}


# --- centro: quartel-general ------------------------------------------------------------------------------------

def core():
    b = Piece((25, 22, 25), 101)
    # Ruas das quatro saidas ate o QG e praca em volta.
    for i in range(25):
        for w in range(10, 15):
            b.set(i, 0, w, ROAD)
            b.set(w, 0, i, ROAD)
    for x, z in ((3, 3), (21, 3), (3, 21), (21, 21)):
        b.lamp_post(x, z)
    # QG 13 x 13, tres andares (pisos em y 0, 5 e 10, laje em 15), portas nos quatro lados.
    x0, z0, x1, z1 = 6, 6, 18, 18
    b.building(x0, z0, x1, z1, 15, door=None)
    for floor_y in (5, 10):
        b.fill(x0 + 1, floor_y, z0 + 1, x1 - 1, floor_y, z1 - 1, "minecraft:polished_deepslate")
        for x in range(x0, x1 + 1):
            b.set(x, floor_y + 3, z0, "minecraft:cyan_concrete")
            b.set(x, floor_y + 3, z1, "minecraft:cyan_concrete")
        for z in range(z0, z1 + 1):
            b.set(x0, floor_y + 3, z, "minecraft:cyan_concrete")
            b.set(x1, floor_y + 3, z, "minecraft:cyan_concrete")
        for x in range(x0 + 1, x1, 3):
            b.set(x, floor_y + 2, z0, "minecraft:light_gray_stained_glass_pane")
            b.set(x, floor_y + 2, z1, "minecraft:light_gray_stained_glass_pane")
        for z in range(z0 + 1, z1, 3):
            b.set(x0, floor_y + 2, z, "minecraft:light_gray_stained_glass_pane")
            b.set(x1, floor_y + 2, z, "minecraft:light_gray_stained_glass_pane")
        for x in (x0 + 3, x1 - 3):
            for z in (z0 + 3, z1 - 3):
                b.set(x, floor_y + 4, z, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})
    for v in (11, 12, 13):
        for pos in ((v, z0), (v, z1), (x0, v), (x1, v)):
            b.set(pos[0], 1, pos[1], "minecraft:air")
            b.set(pos[0], 2, pos[1], "minecraft:air")
    # Escadas: lances alternados (leste em z = 7, oeste em z = 8, leste em z = 7) com o buraco do piso de cima so
    # em cima do proprio lance.
    flights = ((0, 7, 1), (5, 8, -1), (10, 7, 1))
    for base_y, z, step in flights[:2]:
        for k in range(4):
            x = 7 + k if step > 0 else 11 - k
            b.set(x, base_y + 1 + k, z, "minecraft:stone_brick_stairs", stairs("east" if step > 0 else "west"))
            b.set(x, base_y + 5, z, "minecraft:air")
    # Terreo: lanternas presas na laje do primeiro andar (o building so ilumina o ultimo andar).
    for x in (x0 + 3, x1 - 3):
        for z in (z0 + 3, z1 - 3):
            b.set(x, 4, z, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})
    # Terreo: recepcao com bancada, baus e mapas.
    b.set(16, 1, 16, "kn8:defense_workbench", {"facing": "north"})
    b.set(14, 1, 17, "kn8:defense_workbench", {"facing": "north"})
    b.chest(16, 1, 8, "kn8:chests/base_command", "west")
    b.set(8, 1, 16, "minecraft:cartography_table")
    b.set(9, 1, 16, "minecraft:lectern", {"facing": "north", "has_book": "false", "powered": "false"})
    for x in range(9, 16):
        b.set(x, 1, 12, "minecraft:cyan_carpet")
    # Primeiro andar: sala de comando (mesas de cartografia, monitores de observador, baus).
    for x in (10, 12, 14):
        b.set(x, 6, 14, "minecraft:cartography_table")
        b.set(x, 7, 17, "minecraft:observer", {"facing": "north", "powered": "false"})
    b.chest(16, 6, 9, "kn8:chests/base_command", "west")
    for x in range(9, 17):
        b.set(x, 6, 11, "minecraft:blue_carpet")
    # Segundo andar: sala do capitao (cama, estante, bau).
    b.bed(15, 14, "south", "blue", y=11)
    b.fill(9, 11, 17, 12, 12, 17, "minecraft:bookshelf")
    b.chest(16, 11, 9, "kn8:chests/base_command", "west")
    b.set(12, 11, 12, "minecraft:lectern", {"facing": "south", "has_book": "false", "powered": "false"})
    # Telhado: heliporto (H amarelo), guarda-corpo, antena e bandeira.
    b.fill(x0 + 1, 15, z0 + 1, x1 - 1, 15, z1 - 1, "minecraft:black_concrete")
    for z in range(9, 16):
        b.set(10, 15, z, "minecraft:yellow_concrete")
        b.set(14, 15, z, "minecraft:yellow_concrete")
    for x in range(11, 14):
        b.set(x, 15, 12, "minecraft:yellow_concrete")
    for i in range(x0, x1 + 1):
        for pos in ((i, z0), (i, z1), (x0, i), (x1, i)):
            b.set(pos[0], 16, pos[1], "minecraft:iron_bars")
    base_y, z, step = flights[2]
    for k in range(4):
        b.set(7 + k, base_y + 1 + k, z, "minecraft:stone_brick_stairs", stairs("east"))
        b.set(7 + k, base_y + 5, z, "minecraft:air")
    b.fill(17, 16, 17, 17, 20, 17, "minecraft:iron_bars")
    b.set(17, 21, 17, "minecraft:lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
    b.fill(20, 1, 7, 20, 9, 7, "minecraft:iron_bars")
    for y in (7, 8, 9):
        for dz, color in ((-1, "blue"), (-2, "white" if y == 8 else "blue"), (-3, "blue")):
            b.set(20, y, 7 + dz, f"minecraft:{color}_wool")
    # Saidas para as alas (jigsaw no meio de cada lado, virado para fora).
    for x, z, facing in ((12, 0, "north"), (12, 24, "south"), (0, 12, "west"), (24, 12, "east")):
        b.jigsaw(x, z, facing, "kn8:base_core", "kn8:base_wing", f"{POOL}/wings", ROAD)
    # Guardas nas portas do QG.
    b.entity(12, 1, 5, soldier("rifle", "normal", 180.0))
    b.entity(12, 1, 19, soldier("rifle", "normal", 0.0))
    b.entity(5, 1, 12, soldier("pistol", "normal", 90.0))
    b.finish("defense_base/core")


# --- alas -------------------------------------------------------------------------------------------------------

def wing(name, seed):
    """Ala 25 x 20: encaixa no centro pelo jigsaw em (12, 0, 0); muro com portao no fundo (z = 19); saidas para os
    cantos nos dois lados."""
    b = Piece((25, 16, 20), seed)
    for z in range(20):
        for x in range(10, 15):
            b.set(x, 0, z, ROAD)
    b.wall_line(0, 19, 24, 19, gate=(10, 14))
    b.jigsaw(12, 0, "north", "kn8:base_wing", "kn8:base_core", "minecraft:empty", ROAD)
    b.jigsaw(24, 10, "east", "kn8:base_side", "kn8:base_corner", f"{POOL}/corners_px")
    b.jigsaw(0, 10, "west", "kn8:base_side", "kn8:base_corner", f"{POOL}/corners_nx")
    return b


def wing_barracks():
    b = wing("barracks", 111)
    for bx0, bx1 in ((1, 8), (16, 23)):
        b.building(bx0, 2, bx1, 16, 5, wall="minecraft:light_gray_concrete", stripe="minecraft:green_concrete",
                   door=("east" if bx0 == 1 else "west", 8, 10))
        inner_x = (bx0 + 1, bx1 - 1)
        for z in range(4, 15, 3):
            b.bed(inner_x[0], z, "east", "green")
            b.bed(inner_x[1], z, "west", "green")
            b.set(inner_x[1] - 1, 1, z + 1, "minecraft:barrel", {"facing": "up", "open": "false"},
                  {"id": "minecraft:barrel", "LootTable": "kn8:chests/base_barracks"})
    # Refeitorio ao ar livre: mesas e bancos entre os alojamentos.
    for z in (5, 9, 13):
        b.fill(11, 1, z, 13, 1, z, "minecraft:spruce_slab", {"type": "top", "waterlogged": "false"})
    b.lamp_post(10, 2)
    b.lamp_post(14, 2)
    b.entity(12, 1, 17, soldier("rifle", "normal", 0.0))
    b.entity(9, 1, 9, soldier("sword", "low", 90.0))
    b.finish("defense_base/wing_barracks")


def wing_armory():
    b = wing("armory", 112)
    b.building(1, 2, 9, 15, 6, wall="minecraft:gray_concrete", stripe="minecraft:orange_concrete",
               door=("east", 7, 10))
    b.set(2, 1, 4, "kn8:defense_workbench", {"facing": "east"})
    b.set(2, 1, 6, "kn8:defense_workbench", {"facing": "east"})
    b.set(2, 1, 8, "minecraft:anvil", {"facing": "north"})
    b.set(2, 1, 10, "minecraft:smithing_table")
    b.set(2, 1, 12, "minecraft:grindstone", {"face": "floor", "facing": "north"})
    for z in (4, 7, 10, 13):
        b.chest(8, 1, z, "kn8:chests/base_armory", "west")
    # Deposito de veiculos: dois caminhoes de blocos.
    for tx in (17, 21):
        b.fill(tx, 1, 4, tx + 2, 1, 10, "minecraft:black_concrete")
        b.fill(tx, 2, 4, tx + 2, 3, 6, "minecraft:green_terracotta")
        b.fill(tx, 2, 7, tx + 2, 3, 10, "minecraft:green_concrete")
        b.fill(tx, 3, 4, tx + 2, 3, 4, "minecraft:glass")
        for z in (5, 9):
            b.set(tx - 1, 1, z, "minecraft:coal_block")
            b.set(tx + 3, 1, z, "minecraft:coal_block")
    b.fill(17, 1, 13, 23, 1, 15, "minecraft:barrel", {"facing": "up", "open": "false"})
    b.entity(12, 1, 17, soldier("rifle", "normal", 0.0))
    b.entity(11, 1, 6, soldier("pistol", "normal", 270.0))
    b.finish("defense_base/wing_armory")


def wing_training():
    b = wing("training", 113)
    b.fill(1, 0, 1, 9, 0, 17, "minecraft:coarse_dirt")
    for z in (3, 6, 9):
        b.entity(3, 1, z, {"id": "kn8:training_dummy", "Rotation": [Float(90.0), Float(0.0)]})
    for z in (12, 14, 16):
        b.set(1, 1, z, "minecraft:target", {"power": "0"})
        b.set(1, 2, z, "minecraft:hay_block", {"axis": "y"})
    # Pista de parkour (0.5.0-E): muro de 1 (vault), muro de 2 (escalada), corredor de parede (wall-run e
    # wall-jump), borda de 3 (segurar e subir) e barra baixa (slide).
    b.fill(15, 0, 1, 23, 0, 17, "minecraft:smooth_stone")
    b.fill(15, 1, 3, 23, 1, 3, "minecraft:stone_bricks")
    b.fill(15, 1, 6, 23, 2, 6, "minecraft:stone_bricks")
    b.fill(16, 1, 9, 16, 5, 15, "minecraft:stone_bricks")
    b.fill(20, 1, 9, 20, 5, 15, "minecraft:stone_bricks")
    b.fill(21, 1, 17, 23, 3, 17, "minecraft:stone_bricks")
    b.fill(17, 2, 17, 19, 2, 17, "minecraft:stone_brick_slab", {"type": "bottom", "waterlogged": "false"})
    b.set(17, 1, 17, "minecraft:air")
    b.set(18, 1, 17, "minecraft:air")
    b.set(19, 1, 17, "minecraft:air")
    b.lamp_post(14, 1)
    b.entity(12, 1, 17, soldier("rifle", "normal", 0.0))
    b.entity(6, 1, 12, soldier("knife", "high", 270.0))
    b.finish("defense_base/wing_training")


def wing_hangar():
    b = wing("hangar", 114)
    # Heliporto.
    b.fill(1, 0, 2, 9, 0, 10, "minecraft:black_concrete")
    for z in range(4, 9):
        b.set(3, 0, z, "minecraft:yellow_concrete")
        b.set(7, 0, z, "minecraft:yellow_concrete")
    for x in (4, 5, 6):
        b.set(x, 0, 6, "minecraft:yellow_concrete")
    for x, z in ((1, 2), (9, 2), (1, 10), (9, 10)):
        b.set(x, 1, z, "minecraft:redstone_lamp", {"lit": "false"})
    # Hangar aberto para a rua com um helicoptero de blocos.
    b.fill(15, 1, 2, 23, 7, 16, "minecraft:light_gray_concrete")
    b.fill(15, 1, 3, 22, 6, 15, "minecraft:air")
    b.fill(15, 0, 3, 22, 0, 15, "minecraft:polished_andesite")
    b.fill(15, 7, 2, 23, 7, 16, "minecraft:gray_concrete")
    b.fill(18, 1, 7, 20, 3, 11, "minecraft:blue_concrete")
    b.fill(18, 2, 7, 20, 3, 7, "minecraft:light_blue_stained_glass")
    b.fill(19, 2, 12, 19, 2, 15, "minecraft:blue_concrete")
    b.fill(19, 4, 5, 19, 4, 13, "minecraft:iron_bars")
    b.fill(17, 4, 9, 21, 4, 9, "minecraft:iron_bars")
    for z in (4, 6):
        b.set(22, 1, z, "minecraft:barrel", {"facing": "up", "open": "false"})
    b.set(22, 1, 14, "minecraft:lava_cauldron")
    b.entity(12, 1, 17, soldier("rifle", "normal", 0.0))
    b.finish("defense_base/wing_hangar")


def wing_medical():
    b = wing("medical", 115)
    b.building(2, 2, 22, 13, 5, wall="minecraft:white_concrete", stripe="minecraft:red_concrete",
               door=("south", 11, 13))
    for x in range(4, 21, 3):
        b.bed(x, 4, "south", "white")
    b.set(4, 1, 11, "minecraft:brewing_stand",
          {"has_bottle_0": "false", "has_bottle_1": "false", "has_bottle_2": "false"})
    b.set(6, 1, 11, "minecraft:water_cauldron", {"level": "3"})
    b.chest(20, 1, 11, "kn8:chests/base_medical", "west")
    b.chest(18, 1, 11, "kn8:chests/base_medical", "west")
    # Cruz vermelha no telhado.
    for x, z in ((12, 6), (12, 7), (12, 8), (11, 7), (13, 7)):
        b.set(x, 6, z, "minecraft:red_concrete")
    b.entity(12, 1, 17, soldier("rifle", "normal", 0.0))
    b.finish("defense_base/wing_medical")


# --- cantos -----------------------------------------------------------------------------------------------------

def corner(name, side):
    """Canto 20 x 20 entre duas alas. side = +1: encaixa no lado +x da ala (jigsaw em x = 0), muro em x = 19 e
    z = 19; side = -1: encaixa no lado -x (jigsaw em x = 19), muro em x = 0 e z = 19."""
    b = Piece((20, 18, 20), 120 + side)
    outer_x = 19 if side > 0 else 0
    b.wall_line(outer_x, 0, outer_x, 19)
    b.wall_line(0, 19, 19, 19)
    if side > 0:
        b.jigsaw(0, 10, "west", "kn8:base_corner", "kn8:base_side", "minecraft:empty")
    else:
        b.jigsaw(19, 10, "east", "kn8:base_corner", "kn8:base_side", "minecraft:empty")
    # Torre de vigia no canto de fora.
    tx = 14 if side > 0 else 1
    tz = 14
    for x, z in ((tx, tz), (tx + 4, tz), (tx, tz + 4), (tx + 4, tz + 4)):
        b.fill(x, 1, z, x, 12, z, "minecraft:polished_deepslate")
    b.fill(tx, 13, tz, tx + 4, 13, tz + 4, "minecraft:smooth_stone")
    for i in range(5):
        for x, z in ((tx + i, tz), (tx + i, tz + 4), (tx, tz + i), (tx + 4, tz + i)):
            b.set(x, 14, z, "minecraft:iron_bars")
    b.fill(tx + 2, 1, tz + 2, tx + 2, 12, tz + 2, "minecraft:polished_deepslate")
    b.fill(tx + 2, 1, tz + 1, tx + 2, 13, tz + 1, "minecraft:ladder", {"facing": "north", "waterlogged": "false"})
    b.set(tx + 3, 14, tz + 3, "minecraft:sea_lantern")
    b.set(tx + 1, 14, tz + 3, "minecraft:bell", {"attachment": "floor", "facing": "north", "powered": "false"})
    b.fill(tx, 16, tz, tx + 4, 16, tz + 4, "minecraft:gray_concrete")
    for x, z in ((tx, tz), (tx + 4, tz), (tx, tz + 4), (tx + 4, tz + 4)):
        b.set(x, 15, z, "minecraft:polished_deepslate")
    b.entity(tx + 2, 14, tz + 3, soldier("rifle", "normal", 0.0))
    # Caixas de suprimento e um poste no patio do canto.
    b.lamp_post(9, 9)
    for x, z in ((5, 5), (6, 5), (5, 6)):
        b.set(x, 1, z, "minecraft:spruce_planks")
    b.finish(f"defense_base/{name}")


# --- worldgen e loot ------------------------------------------------------------------------------------------

WINGS = [("wing_barracks", 3), ("wing_armory", 3), ("wing_training", 3), ("wing_hangar", 2), ("wing_medical", 2)]
BIOMES = ["minecraft:plains", "minecraft:sunflower_plains", "minecraft:savanna", "minecraft:meadow",
          "minecraft:desert", "minecraft:snowy_plains"]


def element(location):
    return {"element_type": "minecraft:single_pool_element", "location": location, "projection": "rigid",
            "processors": "minecraft:empty"}


def worldgen():
    wg = DATA / "kn8/worldgen"
    dump(DATA / "kn8/tags/worldgen/biome/has_structure/defense_base.json", {"values": BIOMES})
    dump(wg / "template_pool/defense_base/core.json", {"fallback": "minecraft:empty", "elements": [
        {"weight": 1, "element": element("kn8:defense_base/core")}]})
    dump(wg / "template_pool/defense_base/wings.json", {"fallback": "minecraft:empty", "elements": [
        {"weight": weight, "element": element(f"kn8:defense_base/{name}")} for name, weight in WINGS]})
    for side in ("px", "nx"):
        dump(wg / f"template_pool/defense_base/corners_{side}.json", {"fallback": "minecraft:empty", "elements": [
            {"weight": 1, "element": element(f"kn8:defense_base/corner_{side}")}]})
    # Planicies largas: a base (65 x 65) e rigida e o beard_thin cobre so pequenos desniveis.
    dump(wg / "structure/defense_base.json", {
        "type": "minecraft:jigsaw", "biomes": "#kn8:has_structure/defense_base", "step": "surface_structures",
        "spawn_overrides": {}, "terrain_adaptation": "beard_thin", "start_pool": "kn8:defense_base/core",
        "size": 3, "start_height": {"absolute": -1}, "project_start_to_heightmap": "WORLD_SURFACE_WG",
        "max_distance_from_center": 80, "use_expansion_hack": False})
    dump(wg / "structure_set/defense_base.json", {
        "structures": [{"structure": "kn8:defense_base", "weight": 1}],
        "placement": {"type": "minecraft:random_spread", "spacing": 72, "separation": 28, "salt": 280_430_021}})
    loot = {
        "base_command": [(3, 6), [item("minecraft:iron_ingot", 8, 2, 5), item("minecraft:paper", 8, 2, 6),
                                  item("minecraft:map", 4), item("kn8:release_catalyst", 1),
                                  item("kn8:suit_coolant", 6, 1, 3), item("kn8:stamina_stim", 6, 1, 3),
                                  item("minecraft:gold_ingot", 4, 1, 3)]],
        "base_armory": [(3, 6), [item("kn8:rifle_magazine", 8, 1, 2), item("kn8:pistol_magazine", 8, 1, 2),
                                 item("kn8:combat_knife", 4), item("kn8:pistol", 2), item("kn8:rifle", 1),
                                 item("kn8:sword", 1), item("minecraft:iron_ingot", 10, 2, 6),
                                 item("minecraft:redstone", 6, 2, 8)]],
        "base_medical": [(3, 5), [item("kn8:stamina_stim", 10, 1, 3), item("kn8:suit_coolant", 10, 1, 3),
                                  item("minecraft:golden_apple", 2), item("minecraft:glass_bottle", 6, 1, 4),
                                  item("minecraft:honey_bottle", 6, 1, 2), item("minecraft:bread", 6, 2, 4)]],
        "base_barracks": [(1, 3), [item("minecraft:bread", 10, 1, 4), item("minecraft:cooked_beef", 6, 1, 3),
                                   item("minecraft:leather", 4, 1, 3), item("minecraft:torch", 6, 2, 8),
                                   item("kn8:stamina_stim", 3)]],
    }
    for name, (rolls, entries) in loot.items():
        dump(DATA / f"kn8/loot_table/chests/{name}.json", {"type": "minecraft:chest", "pools": [{
            "rolls": {"type": "minecraft:uniform", "min": rolls[0], "max": rolls[1]}, "entries": entries}]})


if __name__ == "__main__":
    (DATA / "kn8/structure/defense_base").mkdir(parents=True, exist_ok=True)
    core()
    wing_barracks()
    wing_armory()
    wing_training()
    wing_hangar()
    wing_medical()
    corner("corner_px", 1)
    corner("corner_nx", -1)
    worldgen()
