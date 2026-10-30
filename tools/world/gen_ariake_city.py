#!/usr/bin/env python3
"""Cidade costeira de Ariake (Koto, Toquio) com a Base Maritima de Ariake dentro (BASES-2, Miguel 2026-10-30:
"de preferencia quero elas dentro das respectivas cidades"; escolha 2: cidade costeira nova para Ariake), pelas
referencias de bases e cidades do Miguel (docs/referencias_bases/).

  ariake/station         peca inicial 41 x 41: estacao de monotrilho sem condutor elevada (como a Yurikamome) e a via
                         expressa elevada da baia por cima das ruas (vias elevadas da vista do manga), praca
  ariake/base_core       saida norte da estacao, 81 x 72, chao no y 12 (subsolo embaixo): praca cerimonial com o
                         circulo e as bandeiras das seis divisoes, QG de vidro de 7 andares (centro de operacoes
                         nacional, sala do conselho de madeira escura com mesa oval longa, gabinete do Diretor-Geral),
                         laboratorio de armas numeradas, ala de seguranca (antecamara, corredor com guarda, sala de
                         contencao de paineis de metal com a cadeira de contencao, sala de observacao), catapulta
                         eletromagnetica, salao das divisoes, patio de logistica e passarela elevada; no subsolo o
                         abrigo fortificado e o treino de trajes e armas numeradas [SUPOSICAO: a obra so cita]
  ariake/harbor          porto a oeste (81 x 44): cais, bacia com agua, guindastes, conteineres, armazens, pier,
                         barco de patrulha e farol
  ariake/lot_*           Tokyo Big Sight (piramides invertidas), armazem de logistica e parque com canal

As ruas sao as da cidade comum (gen_city.street_piece("ariake")). O que a obra nao mostra e adaptacao [SUPOSICAO].
Gerado pelo gen_sites.py.
"""
from gen_base import Piece, ROAD, stairs
from gen_city import ASPHALT, SIDEWALK, floors_building, lot, street_lamp, tree
from gen_structures import Build, Float, officer, soldier
from gen_ruins import drop_officers, emergency_lights, field_post, ruin
from gen_tachikawa_base import armor_stand, car, dummy, flagpole, helicopter, helipad, screens

GLASS = "minecraft:light_blue_stained_glass"
LANTERN = {"hanging": "true", "waterlogged": "false"}
LADDER = {"waterlogged": "false"}
SLAB_TOP = {"type": "top", "waterlogged": "false"}
DIVISION_COLORS = ["blue", "green", "orange", "red", "purple", "yellow"]
PRIORITY = 10
LINK = "kn8:ariake_base_link"


class Lifted(Piece):
    """Peca com subsolo: o chao fica no y {lift} do template e o desenho usa y relativo a ele (negativo = subsolo)."""

    def __init__(self, size, seed, lift, floor="minecraft:stone"):
        super().__init__(size, seed, floor=floor)
        self.lift = lift

    def set(self, x, y, z, name, props=None, nbt=None):
        super().set(x, y + self.lift, z, name, props, nbt)

    def get(self, x, y, z):
        return super().get(x, y + self.lift, z)

    def entity(self, x, y, z, nbt):
        super().entity(x, y + self.lift, z, nbt)

    def finish(self, name):
        self.lift = 0
        super().finish(name)


def ladder(b, x, y0, y1, z, facing):
    b.fill(x, y0, z, x, y1, z, "minecraft:ladder", dict(LADDER, facing=facing))


# --- estacao --------------------------------------------------------------------------------------------------------

def ariake_station():
    from gen_sites import station_jigsaws
    b = Piece((41, 26, 41), 361, floor="minecraft:polished_andesite")
    for x in range(41):
        for z in range(41):
            if (x * 2 + z) % 5 == 0:
                b.set(x, 0, z, "minecraft:smooth_stone")
    for i in range(41):
        for w in range(16, 25):
            b.set(i, 0, w, ASPHALT)
            b.set(w, 0, i, ASPHALT)
    # Monotrilho sem condutor (leste-oeste) no y 8: viga branca, plataforma com portas de vidro e cobertura curva.
    deck = 8
    b.fill(0, deck, 9, 40, deck, 11, "minecraft:white_concrete")
    for x in (3, 13, 27, 37):
        b.fill(x, 1, 10, x, deck - 1, 10, "minecraft:white_concrete")
        b.fill(x, deck - 1, 9, x, deck - 1, 11, "minecraft:white_concrete")
    b.fill(10, deck, 5, 30, deck, 8, "minecraft:smooth_stone")
    b.fill(10, deck, 12, 30, deck, 14, "minecraft:smooth_stone")
    for x in range(10, 31):
        b.set(x, deck + 1, 8, "minecraft:glass_pane")
        b.set(x, deck + 1, 12, "minecraft:glass_pane")
        b.set(x, deck + 2, 8, "minecraft:glass_pane")
        b.set(x, deck + 2, 12, "minecraft:glass_pane")
    for x in range(10, 31, 4):
        b.set(x, deck + 1, 8, "minecraft:air")
        b.set(x, deck + 2, 8, "minecraft:air")
    b.fill(10, deck + 5, 6, 30, deck + 5, 14, "minecraft:white_concrete")
    b.fill(10, deck + 4, 5, 30, deck + 4, 5, "minecraft:white_concrete")
    b.fill(10, deck + 4, 15, 30, deck + 4, 15, "minecraft:white_concrete")
    for x in range(12, 30, 6):
        b.set(x, deck + 4, 10, "minecraft:sea_lantern")
    # Trem do monotrilho (branco com faixa azul) parado na viga.
    b.fill(12, deck + 1, 9, 28, deck + 3, 11, "minecraft:white_concrete")
    b.fill(12, deck + 2, 9, 28, deck + 2, 11, "minecraft:light_blue_stained_glass")
    b.fill(12, deck + 1, 9, 28, deck + 1, 11, "minecraft:blue_concrete")
    b.fill(10, 1, 3, 10, deck, 3, "minecraft:white_concrete")
    b.fill(10, 1, 4, 10, deck, 4, "minecraft:ladder", dict(LADDER, facing="south"))
    # Via expressa elevada da baia (leste-oeste) mais alta, no y 14, com guarda-corpo, pilares e um caminhao.
    hy = 14
    b.fill(0, hy, 30, 40, hy, 37, "minecraft:gray_concrete")
    for x in range(41):
        b.set(x, hy + 1, 30, "minecraft:light_gray_concrete")
        b.set(x, hy + 1, 37, "minecraft:light_gray_concrete")
        if x % 4 < 2:
            b.set(x, hy, 33, "minecraft:white_concrete")
            b.set(x, hy, 34, "minecraft:white_concrete")
    for x in (4, 12, 28, 36):  # pilares fora da rua norte-sul (x 16-24)
        b.fill(x - 1, 1, 33, x + 1, hy - 1, 34, "minecraft:light_gray_concrete")
        b.fill(x - 2, hy - 1, 30, x + 2, hy - 1, 37, "minecraft:light_gray_concrete")
    for x in range(0, 41, 8):
        b.fill(x, hy + 2, 37, x, hy + 4, 37, "minecraft:iron_bars")
        b.set(x, hy + 5, 37, "minecraft:sea_lantern")
    b.fill(24, hy + 1, 31, 30, hy + 2, 32, "minecraft:green_terracotta")
    b.fill(29, hy + 3, 31, 30, hy + 3, 32, "minecraft:light_blue_stained_glass")
    car(Shifted(b, 0, hy - 1, 0), 10, 35, "white", along_z=False)
    # Saguao da estacao no chao (oeste) e praca com bancos e arvores (leste).
    b.building(1, 2, 8, 14, 5, wall="minecraft:white_concrete", stripe="minecraft:blue_concrete",
               door=("east", 7, 9))
    b.chest(2, 1, 13, "kn8:chests/city_shop", "east")
    for x, z in ((29, 2), (37, 2), (29, 26), (37, 26)):
        tree(b, x, z, 4)
    for x in range(30, 37, 2):
        b.set(x, 1, 13, "minecraft:spruce_stairs", stairs("north"))
    for x, z in ((1, 39), (39, 39), (1, 26), (39, 26)):
        street_lamp(b, x, z)
    station_jigsaws(b, "ariake", district="kn8:ariake/base_core")
    b.finish("ariake/station")


class Shifted:
    """Desenha um helper (carro) mais alto: so desloca o y."""

    def __init__(self, b, dx, dy, dz):
        self.b, self.d = b, (dx, dy, dz)

    def fill(self, x0, y0, z0, x1, y1, z1, name, props=None):
        dx, dy, dz = self.d
        self.b.fill(x0 + dx, y0 + dy, z0 + dz, x1 + dx, y1 + dy, z1 + dz, name, props)


# --- base -----------------------------------------------------------------------------------------------------------

W, D, LIFT, UP = 81, 72, 12, 50


def glass_tower(b, x0, z0, x1, z1):
    """QG de vidro (o mesmo desenho da Base Maritima avulsa): 7 andares de 5, montantes, faixas azuis."""
    from gen_sites import columns
    top = floors_building(b, x0, z0, x1, z1, 7, "minecraft:white_concrete", GLASS, floor_h=5, glass_front=True)
    columns(b, x0, z0, x1, z1, top, "minecraft:white_concrete")
    for f in range(1, 7):
        for x in range(x0, x1 + 1):
            b.set(x, f * 5, z0, "minecraft:blue_concrete")
            b.set(x, f * 5, z1, "minecraft:blue_concrete")
        for z in range(z0, z1 + 1):
            b.set(x0, f * 5, z, "minecraft:blue_concrete")
            b.set(x1, f * 5, z, "minecraft:blue_concrete")
    mx = (x0 + x1) // 2
    b.fill(mx - 1, 1, z0, mx + 1, 3, z0, "minecraft:air")
    b.fill(mx - 1, 1, z1, mx + 1, 3, z1, "minecraft:air")
    b.fill(x0 + 1, 0, z0 + 1, x1 - 1, 0, z1 - 1, "minecraft:polished_diorite")
    return top


def base_core(ruined=False):
    """ruined (BASES-3): variante depois de um ataque [SUPOSICAO: a obra nao mostra ataque direto a Ariake]."""
    b = Lifted((W, LIFT + UP, D), 362, LIFT)
    # Subsolo macico (o desenho abre as salas) e o chao da base.
    b.fill(0, -LIFT, 0, W - 1, -1, D - 1, "minecraft:stone")
    for x in range(W):
        for z in range(D):
            b.set(x, 0, z, "minecraft:light_gray_concrete" if (x + z) % 5 == 0 else "minecraft:polished_andesite")
    for z in range(0, 20):
        for x in range(37, 44):
            b.set(x, 0, z, ROAD)
    b.jigsaw(40, 0, "north", "kn8:city_street", "kn8:city_link", "minecraft:empty", ROAD, y=LIFT, priority=PRIORITY)
    # Muro: portao da frente e portao oeste (para o porto).
    b.wall_line(0, 3, W - 1, 3, gate=(34, 46))
    b.wall_line(0, 3, 0, D - 1, gate=(31, 41))
    b.wall_line(W - 1, 3, W - 1, D - 1)
    b.wall_line(0, D - 1, W - 1, D - 1)
    for x in (32, 48):
        b.fill(x - 1, 1, 4, x + 1, 3, 6, "minecraft:white_concrete")
        b.fill(x - 1, 4, 4, x + 1, 4, 6, "minecraft:blue_concrete")
        b.fill(x, 1, 6, x, 2, 6, "minecraft:air")
    b.entity(35, 1, 5, soldier("rifle", "high", 180.0))
    b.entity(45, 1, 5, soldier("rifle", "high", 180.0))
    for z in range(31, 42):
        for x in range(1, 6):
            b.set(x, 0, z, ROAD)
    b.jigsaw(0, 36, "west", LINK, LINK, "kn8:ariake/harbor", ROAD, y=LIFT, priority=PRIORITY)

    # Praca cerimonial com o circulo (vista do manga) e as bandeiras das seis divisoes.
    cx, cz = 40, 12
    for x in range(cx - 9, cx + 10):
        for z in range(cz - 7, cz + 8):
            r2 = (x - cx) ** 2 + ((z - cz) * 9 / 7) ** 2
            if r2 <= 81:
                ring = 49 <= r2 <= 81 or 9 <= r2 <= 16
                b.set(x, 0, z, "minecraft:white_concrete" if ring else "minecraft:smooth_stone")
    for k, color in enumerate(DIVISION_COLORS):
        flagpole(b, (18, 23, 28, 50, 55, 60)[k], 17, color, 9)
    for x, z in ((12, 11), (68, 11)):
        tree(b, x, z, 5)

    # QG de vidro no meio.
    hx0, hz0, hx1, hz1 = 32, 20, 48, 36
    top = glass_tower(b, hx0, hz0, hx1, hz1)
    hq_rooms(b, hx0, hz0, hx1, hz1)
    helipad(b, 36, 24, top)
    for i in range(hx0, hx1 + 1):
        for pos in ((i, hz0), (i, hz1), (hx0, i - hx0 + hz0), (hx1, i - hx0 + hz0)):
            b.set(pos[0], top + 1, pos[1], "minecraft:iron_bars")
    for x, z in ((hx0 + 1, hz0 + 1), (hx1 - 1, hz1 - 1)):
        b.fill(x, top + 1, z, x, top + 8, z, "minecraft:iron_bars")
        b.set(x, top + 9, z, "minecraft:lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
    b.fill(37, top + 1, hz1, 43, top + 5, hz1, "minecraft:blue_concrete")
    b.fill(38, top + 2, hz1, 42, top + 4, hz1, "minecraft:white_concrete")
    b.set(40, top + 3, hz1, "minecraft:blue_concrete")

    numbered_lab(b)
    security_wing(b)
    catapult(b)
    divisions_hall(b)
    logistics(b)
    underground(b)
    for x, z in ((6, 18), (74, 18), (6, 66), (74, 66), (28, 46), (52, 46)):
        b.lamp_post(x, z)
    if ruined:
        g = LIFT  # coordenadas cruas: o chao fica no y 12
        ruin(b, [(48, 28 + g, 22, 7), (78, 3 + g, 38, 5), (28, g, 10, 5), (66, 7 + g, 58, 5)], 373, glass=0.7,
             ground=g)
        drop_officers(b)
        field_post(b, 56, 5, ground=g)
        emergency_lights(b, [(20, 10), (36, 10), (74, 30)], ground=g)
    b.finish("ariake/base_core" + ("_ruined" if ruined else ""))


def hq_rooms(b, x0, z0, x1, z1):
    """Saguao (oficiais, servicos), centro de operacoes (1o), conselho (3o), gabinete do Diretor-Geral (6o)."""
    b.entity(38, 1, 32, officer("officer", 180.0))
    b.entity(42, 1, 32, officer("exam_officer", 180.0))
    b.fill(37, 1, 31, 43, 1, 31, "minecraft:smooth_quartz_slab", SLAB_TOP)
    b.set(34, 1, 34, "kn8:squad_station", {"facing": "east"})
    b.set(34, 1, 30, "kn8:research_terminal", {"facing": "east"})
    b.set(34, 1, 22, "kn8:defense_workbench", {"facing": "east"})
    b.set(46, 1, 22, "kn8:defense_workbench", {"facing": "west"})
    for x in (38, 40, 42):
        b.set(x, 1, 25, "minecraft:cartography_table")
    b.chest(46, 1, 30, "kn8:chests/base_command", "west")
    # 1o: centro de operacoes nacional (telas largas, mapas, consoles dos operadores, escuro).
    y = 5
    screens(b, x0 + 1, y + 1, z1 - 1, x1 - 2, y + 4, z1 - 1)  # sem cobrir a escada de mao (x1 - 1)
    for z in (27, 30):
        for x in range(x0 + 2, x1 - 1):
            if x != 40:
                b.set(x, y + 1, z, "minecraft:polished_blackstone_slab", SLAB_TOP)
                b.set(x, y + 1, z - 1, "minecraft:dark_oak_stairs", stairs("south"))
                if x % 3 == 0:
                    b.set(x, y + 2, z, "minecraft:black_stained_glass_pane")
    for x in (37, 40, 43):
        b.set(x, y + 1, 23, "minecraft:cartography_table")
    # 3o: sala do conselho (madeira escura, mesa oval longa, luzes pendentes, terminais).
    y = 15
    for x in range(x0 + 1, x1):
        for z in range(z0 + 1, z1):
            if (x, z) != (x1 - 1, z1 - 1):  # furo da escada de mao
                b.set(x, y, z, "minecraft:dark_oak_planks")
    for z in range(z0 + 1, z1 - 1):
        b.fill(x0 + 1, y + 1, z, x0 + 1, y + 4, z, "minecraft:dark_oak_planks")
        b.fill(x1 - 1, y + 1, z, x1 - 1, y + 4, z, "minecraft:dark_oak_planks")
    for x in range(36, 45):
        for z in range(25, 32):
            dx, dz = (x - 40) / 4.5, (z - 28) / 3.5
            if dx * dx + dz * dz <= 1.0:
                b.set(x, y + 1, z, "minecraft:dark_oak_slab", SLAB_TOP)
    for x in range(37, 44, 2):
        b.set(x, y + 1, 24, "minecraft:dark_oak_stairs", stairs("south"))
        b.set(x, y + 1, 32, "minecraft:dark_oak_stairs", stairs("north"))
        b.set(x, y + 2, 25, "minecraft:black_stained_glass_pane")
        b.set(x, y + 2, 31, "minecraft:black_stained_glass_pane")
    for x in (37, 40, 43):
        b.set(x, y + 4, 28, "minecraft:chain", {"axis": "y", "waterlogged": "false"})
        b.set(x, y + 3, 28, "minecraft:lantern", LANTERN)
    # 6o: gabinete do Diretor-Geral.
    y = 30
    b.fill(x0 + 2, y + 1, z1 - 1, x1 - 2, y + 2, z1 - 1, "minecraft:bookshelf")
    b.fill(38, y + 1, 27, 42, y + 1, 27, "minecraft:dark_oak_slab", SLAB_TOP)
    b.set(40, y + 1, 29, "minecraft:dark_oak_stairs", stairs("north"))
    b.set(40, y + 2, 27, "minecraft:lectern", {"facing": "south", "has_book": "false", "powered": "false"})
    b.chest(x0 + 2, y + 1, z0 + 2, "kn8:chests/ariake_lab", "east")
    for f in (2, 4, 5):
        y = f * 5
        for x in range(x0 + 3, x1 - 2, 4):
            b.set(x, y + 1, 24, "minecraft:spruce_slab", SLAB_TOP)
            b.set(x, y + 1, 32, "minecraft:spruce_slab", SLAB_TOP)
            b.set(x, y + 1, 25, "minecraft:spruce_stairs", stairs("south"))


def numbered_lab(b):
    """Laboratorio de armas numeradas (vitrines com nucleos, bancada, terminais)."""
    b.building(4, 20, 26, 32, 6, wall="minecraft:white_concrete", stripe="minecraft:purple_concrete",
               door=("east", 25, 27))
    for cx in (6, 12, 18):
        b.fill(cx, 1, 22, cx + 2, 3, 24, "minecraft:glass")
        b.fill(cx + 1, 1, 23, cx + 1, 2, 23, "minecraft:air")
        b.set(cx + 1, 1, 23, "minecraft:magma_block")
        b.set(cx + 1, 2, 23, "minecraft:end_rod", {"facing": "up"})
    b.set(6, 1, 30, "kn8:defense_workbench", {"facing": "north"})
    for x in (9, 11):
        b.set(x, 1, 30, "kn8:research_terminal", {"facing": "north"})
    b.set(14, 1, 30, "minecraft:brewing_stand", {"has_bottle_0": "false", "has_bottle_1": "false",
                                                 "has_bottle_2": "false"})
    for x in (17, 20, 23):
        b.chest(x, 1, 30, "kn8:chests/ariake_lab", "north")


def security_wing(b):
    """Ala de seguranca: antecamara com guarda e porta de ferro, corredor, sala de observacao com vidro escuro e a
    sala de contencao (paineis de metal, cadeira de contencao no meio), celas."""
    x0, z0, x1, z1 = 56, 20, 78, 46
    b.building(x0, z0, x1, z1, 7, wall="minecraft:gray_concrete", stripe="minecraft:red_concrete",
               door=("west", 22, 23), inner_floor="minecraft:polished_andesite")
    # Antecamara (z 21-25) com o posto da guarda e a porta de ferro para o corredor.
    b.fill(x0 + 1, 1, 26, x1 - 1, 6, 26, "minecraft:gray_concrete")
    b.set(60, 1, 26, "minecraft:iron_door", {"facing": "south", "half": "lower", "hinge": "left", "open": "false",
                                             "powered": "false"})
    b.set(60, 2, 26, "minecraft:iron_door", {"facing": "south", "half": "upper", "hinge": "left", "open": "false",
                                             "powered": "false"})
    b.set(61, 2, 25, "minecraft:stone_button", {"face": "wall", "facing": "north", "powered": "false"})
    b.set(61, 2, 27, "minecraft:stone_button", {"face": "wall", "facing": "south", "powered": "false"})
    b.fill(64, 1, 22, 70, 1, 22, "minecraft:polished_blackstone_slab", SLAB_TOP)
    b.entity(67, 1, 24, soldier("rifle", "elite", 0.0))
    # Corredor de acesso controlado (z 27-29) e, ao sul, observacao (oeste) e contencao (leste).
    for z in (30,):
        b.fill(x0 + 1, 1, z, x1 - 1, 6, z, "minecraft:gray_concrete")
    b.fill(58, 1, 30, 59, 2, 30, "minecraft:air")
    b.fill(64, 1, 30, 64, 6, z1 - 1, "minecraft:tinted_glass")  # vidro escuro da observacao
    b.fill(64, 1, 30, 64, 1, z1 - 1, "minecraft:gray_concrete")
    for z in range(32, 45, 4):
        b.set(58, 1, z, "minecraft:polished_blackstone_slab", SLAB_TOP)
        b.set(59, 1, z, "minecraft:dark_oak_stairs", stairs("west"))
    screens(b, 57, 2, 31, 57, 4, 44)
    # Contencao: paineis de metal, porta de ferro, cadeira de contencao.
    cx0, cz0, cx1, cz1 = 65, 31, 77, 45
    for x in range(cx0, cx1 + 1):
        for z in range(cz0, cz1 + 1):
            b.set(x, 0, z, "minecraft:iron_block" if (x + z) % 2 else "minecraft:polished_blackstone")
    for y in range(1, 6):
        for z in range(cz0, cz1 + 1):
            b.set(cx1, y, z, "minecraft:iron_block" if (z + y) % 3 else "minecraft:polished_blackstone_bricks")
        for x in range(cx0 + 1, cx1 + 1):
            b.set(x, y, cz1, "minecraft:iron_block" if (x + y) % 3 else "minecraft:polished_blackstone_bricks")
            b.set(x, y, 30, "minecraft:iron_block" if (x + y) % 3 else "minecraft:polished_blackstone_bricks")
    b.set(70, 1, 30, "minecraft:iron_door", {"facing": "north", "half": "lower", "hinge": "left", "open": "false",
                                             "powered": "false"})
    b.set(70, 2, 30, "minecraft:iron_door", {"facing": "north", "half": "upper", "hinge": "left", "open": "false",
                                             "powered": "false"})
    b.set(71, 2, 29, "minecraft:stone_button", {"face": "wall", "facing": "north", "powered": "false"})
    b.set(71, 1, 38, "minecraft:polished_blackstone_stairs", stairs("west"))
    for z in (37, 39):
        b.set(71, 1, z, "minecraft:polished_blackstone_wall",
              {"east": "none", "north": "none", "south": "none", "up": "true", "waterlogged": "false",
               "west": "none"})
        b.set(71, 2, z, "minecraft:chain", {"axis": "z", "waterlogged": "false"})
    b.set(72, 2, 38, "minecraft:polished_blackstone_wall",
          {"east": "none", "north": "none", "south": "none", "up": "true", "waterlogged": "false", "west": "none"})
    b.set(71, 5, 38, "minecraft:sea_lantern")


def catapult(b):
    """Catapulta eletromagnetica (lanca a Kikoru no ep. 22): rampa de trilho de cobre subindo para leste."""
    for x in range(4, 23):
        y = 1 + (x - 6) * 10 // 16 if x >= 6 else 1
        b.fill(x, y, 54, x, y, 56, "minecraft:polished_blackstone_slab", SLAB_TOP)
        b.set(x, y, 53, "minecraft:waxed_cut_copper")
        b.set(x, y, 57, "minecraft:waxed_cut_copper")
        b.set(x, y + 1, 53, "minecraft:waxed_copper_grate")
        b.set(x, y + 1, 57, "minecraft:waxed_copper_grate")
        if x % 3 == 0 and y > 1:
            b.fill(x, 1, 55, x, y - 1, 55, "minecraft:iron_block")
    b.fill(4, 1, 54, 6, 1, 56, "minecraft:redstone_block")
    for cx in (7, 13, 19):
        b.fill(cx, 1, 47, cx + 2, 4, 49, "minecraft:waxed_copper_block")
        b.set(cx + 1, 5, 48, "minecraft:lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
    b.building(6, 60, 14, 64, 4, wall="minecraft:light_gray_concrete", stripe="minecraft:yellow_concrete",
               door=("north", 9, 11))
    b.set(10, 1, 62, "minecraft:lever", {"face": "floor", "facing": "north", "powered": "false"})
    b.set(8, 1, 62, "minecraft:observer", {"facing": "north", "powered": "false"})


def divisions_hall(b):
    """Salao das divisoes (reuniao das divisoes, temporada 2): mesa longa."""
    b.building(56, 52, 76, 64, 7, wall="minecraft:white_concrete", stripe="minecraft:blue_concrete",
               door=("west", 57, 59))
    b.fill(59, 1, 57, 73, 1, 58, "minecraft:dark_oak_slab", SLAB_TOP)
    for x in range(59, 74, 2):
        b.set(x, 1, 56, "minecraft:spruce_stairs", stairs("south"))
        b.set(x, 1, 59, "minecraft:spruce_stairs", stairs("north"))
    for k, color in enumerate(DIVISION_COLORS):
        b.fill(59 + k * 2, 3, 63, 59 + k * 2, 5, 63, f"minecraft:{color}_wool")


def logistics(b):
    """Patio de logistica e transporte (vista do manga: areas abertas para transporte e tropas)."""
    helipad(b, 28, 56)
    helicopter(b, 31, 57, color="blue")
    for x in (40, 46):
        b.fill(x, 1, 56, x + 3, 2, 62, "minecraft:green_terracotta")
        b.fill(x, 1, 56, x + 3, 2, 57, "minecraft:gray_concrete")
        b.fill(x, 3, 56, x + 3, 3, 57, "minecraft:light_blue_stained_glass")
    for k, x in enumerate((26, 34)):
        b.fill(x, 1, 66, x + 6, 3, 68, f"minecraft:{['red', 'blue'][k]}_concrete")
    for x in (30, 36, 42, 48):
        b.entity(x, 1, 50, soldier("rifle", "normal", 180.0))
    # Entrada do subsolo (abrigo e treino): guarita com escada de mao.
    b.fill(51, 1, 39, 53, 3, 41, "minecraft:gray_concrete")
    b.fill(52, 1, 40, 52, 2, 40, "minecraft:air")
    b.fill(52, 1, 39, 52, 2, 39, "minecraft:air")
    b.fill(51, 4, 39, 53, 4, 41, "minecraft:red_concrete")
    ladder(b, 52, -9, 2, 40, "north")


def underground(b):
    """Subsolo: corredor, abrigo fortificado (camadas de concreto e ferro) e treino de trajes e armas numeradas.
    A obra so cita os dois (abrigo que aguenta um kaiju de fortitude 10; pratica de trajes no subsolo) [SUPOSICAO]."""
    fy = -10  # piso
    b.fill(8, fy + 1, 38, 74, fy + 4, 42, "minecraft:air")
    b.fill(8, fy, 38, 74, fy, 42, "minecraft:polished_andesite")
    for x in range(10, 74, 6):
        b.set(x, fy + 4, 40, "minecraft:sea_lantern")
    b.fill(52, fy + 1, 41, 52, 2, 41, "minecraft:gray_concrete")  # parede da escada de mao
    # Abrigo: paredes em camadas (concreto, ferro, deepslate) e porta de ferro.
    sx0, sz0, sx1, sz1 = 6, 44, 36, 68
    b.fill(sx0 - 1, fy, sz0 - 1, sx1 + 1, fy + 8, sz1 + 1, "minecraft:reinforced_deepslate")
    b.fill(sx0, fy + 1, sz0, sx1, fy + 7, sz1, "minecraft:iron_block")
    b.fill(sx0 + 1, fy + 1, sz0 + 1, sx1 - 1, fy + 6, sz1 - 1, "minecraft:air")
    b.fill(sx0 + 1, fy, sz0 + 1, sx1 - 1, fy, sz1 - 1, "minecraft:deepslate_tiles")
    for z in (sz0 - 1, sz0):
        b.fill(20, fy + 1, z, 21, fy + 2, z, "minecraft:air")
    door = {"facing": "north", "hinge": "left", "open": "false", "powered": "false"}
    b.set(20, fy + 1, sz0, "minecraft:iron_door", dict(door, half="lower"))
    b.set(20, fy + 2, sz0, "minecraft:iron_door", dict(door, half="upper"))
    b.set(21, fy + 1, sz0, "minecraft:iron_door", dict(door, half="lower", hinge="right"))
    b.set(21, fy + 2, sz0, "minecraft:iron_door", dict(door, half="upper", hinge="right"))
    b.set(19, fy + 2, sz0 - 2, "minecraft:stone_button", {"face": "wall", "facing": "north", "powered": "false"})
    b.set(22, fy + 2, sz0 + 1, "minecraft:stone_button", {"face": "wall", "facing": "south", "powered": "false"})
    for x in range(sx0 + 2, sx1 - 1, 3):
        b.bed(x, sz1 - 2, "north", "gray", y=fy + 1)
    for x in range(sx0 + 2, sx1 - 1, 4):
        b.set(x, fy + 1, sz0 + 3, "minecraft:barrel", {"facing": "up", "open": "false"},
              {"id": "minecraft:barrel", "LootTable": "kn8:chests/base_barracks"})
    b.chest(sx0 + 1, fy + 1, 56, "kn8:chests/base_medical", "east")
    b.fill(sx1 - 3, fy + 1, 52, sx1 - 1, fy + 2, 60, "minecraft:blast_furnace", {"facing": "west", "lit": "false"})
    for x in range(sx0 + 4, sx1, 8):
        for z in range(sz0 + 4, sz1, 8):
            b.set(x, fy + 6, z, "minecraft:sea_lantern")
    # Treino de trajes e armas numeradas: salao alto com paredes blindadas, alvos, pesos, bonecos e observacao.
    tx0, tz0, tx1, tz1 = 44, 44, 76, 68
    b.fill(tx0, fy, tz0, tx1, fy + 9, tz1, "minecraft:polished_blackstone_bricks")
    b.fill(tx0 + 1, fy + 1, tz0 + 1, tx1 - 1, fy + 8, tz1 - 1, "minecraft:air")
    b.fill(tx0 + 1, fy, tz0 + 1, tx1 - 1, fy, tz1 - 1, "minecraft:polished_andesite")
    for x in range(tx0 + 2, tx1 - 1):
        for z in (tz0 + 4, tz1 - 4):
            if x % 2:
                b.set(x, fy, z, "minecraft:yellow_concrete")
    b.fill(56, fy + 1, tz0, 58, fy + 3, tz0, "minecraft:air")
    b.fill(56, fy + 1, 43, 58, fy + 3, 43, "minecraft:air")
    b.fill(48, fy + 4, tz0, 54, fy + 6, tz0, "minecraft:glass")
    for x, z in ((48, 50), (48, 56), (48, 62)):
        b.entity(x, fy + 1, z, dummy(270.0))
    for z in range(48, 66, 4):
        b.set(tx1 - 1, fy + 2, z, "minecraft:target", {"power": "0"})
    for x in (60, 64, 68):
        b.set(x, fy + 1, 66, "kn8:weight_rack", {"facing": "north"})
    b.entity(66, fy + 1, 52, armor_stand("kn8:training_suit", 90.0))
    for x in range(tx0 + 4, tx1, 8):
        for z in range(tz0 + 4, tz1, 8):
            b.set(x, fy + 8, z, "minecraft:lantern", LANTERN)
    b.entity(60, fy + 1, 56, soldier("sword", "elite", 90.0))
    ladder(b, 52, -9, 2, 40, "north")  # o corredor foi aberto por cima da escada de mao


# --- porto ----------------------------------------------------------------------------------------------------------

def ariake_harbor():
    """Porto 81 x 44 a oeste da base: encaixa pelo jigsaw em (40, 3, 0); cais no nivel da base, bacia abaixo."""
    deck = 3
    b = Piece((81, 30, 44), 363, floor="minecraft:stone")
    rnd = b.random
    basin = (6, 18, 74, 38)
    for x in range(81):
        for z in range(44):
            if basin[0] <= x <= basin[2] and basin[1] <= z <= basin[3]:
                b.set(x, 0, z, "minecraft:gravel" if rnd.random() < 0.6 else "minecraft:mud")
                b.fill(x, 1, z, x, deck, z, "minecraft:water", {"level": "0"})
            else:
                b.fill(x, 0, z, x, deck - 1, z, "minecraft:stone")
                tile = "gray_concrete" if (x // 4 + z // 4) % 2 else "light_gray_concrete"
                b.set(x, deck, z, f"minecraft:{tile}")
    for x in range(basin[0] - 1, basin[2] + 2):
        b.set(x, deck, basin[1] - 1, "minecraft:yellow_concrete" if x % 2 else "minecraft:black_concrete")
    for z in range(0, 3):
        for x in range(81):
            b.set(x, deck, z, ROAD)
    for z in range(3, basin[1] - 1):
        for x in range(38, 43):
            b.set(x, deck, z, ROAD)
    for x in range(81):
        for z in (39, 40, 41, 42, 43):
            b.fill(x, 0, z, x, deck + 1, z, "minecraft:stone_bricks" if z < 43 else "minecraft:polished_andesite")
    for z in range(44):
        for x in (0, 80):
            b.fill(x, deck + 1, z, x, deck + 2, z, "minecraft:light_gray_concrete")
            b.set(x, deck + 3, z, "minecraft:iron_bars")
    for c in (20, 60):  # guindastes
        for x in (c - 3, c + 3):
            for z in (6, 15):
                b.fill(x, deck + 1, z, x, deck + 16, z, "minecraft:orange_concrete")
            b.fill(x, deck + 17, 3, x, deck + 17, 30, "minecraft:orange_concrete")
        for z in (3, 6, 15, 30):
            b.fill(c - 3, deck + 17, z, c + 3, deck + 17, z, "minecraft:orange_concrete")
        b.fill(c - 1, deck + 15, 24, c + 1, deck + 16, 26, "minecraft:white_concrete")
        b.fill(c, deck + 9, 28, c, deck + 16, 28, "minecraft:chain", {"axis": "y", "waterlogged": "false"})
        b.fill(c - 1, deck + 6, 27, c + 1, deck + 8, 29, "minecraft:blue_concrete")
    colors = ["red", "blue", "green", "orange", "gray", "cyan"]
    for row, z in enumerate((4, 10)):
        for k, x in enumerate((2, 10, 64, 72)):
            for level in range(1 + (k + row) % 2 + 1):
                y = deck + 1 + level * 3
                b.fill(x, y, z, x + 6, y + 2, z + 2, f"minecraft:{colors[(k * 3 + row + level) % 6]}_concrete")
    for wx0 in (24, 45):  # armazens
        b.fill(wx0, deck + 1, 4, wx0 + 11, deck + 8, 14, "minecraft:light_gray_concrete")
        b.fill(wx0 + 1, deck + 1, 5, wx0 + 10, deck + 7, 13, "minecraft:air")
        b.fill(wx0 + 3, deck + 1, 14, wx0 + 8, deck + 4, 14, "minecraft:air")
        b.fill(wx0, deck + 8, 4, wx0 + 11, deck + 8, 14, "minecraft:gray_concrete")
        for x in range(wx0, wx0 + 12):
            b.set(x, deck + 7, 14, "minecraft:blue_concrete")
        b.chest(wx0 + 2, deck + 1, 12, "kn8:chests/ariake_harbor", "east")
        b.set(wx0 + 5, deck + 7, 9, "minecraft:lantern", LANTERN)
    b.fill(38, deck, 18, 41, deck, 32, "minecraft:spruce_planks")  # pier
    for z in (20, 26, 32):
        for x in (38, 41):
            b.fill(x, 0, z, x, deck - 1, z, "minecraft:spruce_log", {"axis": "y"})
            b.set(x, deck + 1, z, "minecraft:spruce_fence")
    # Barco de patrulha (casco, conves, ponte, mastro e canhao na proa).
    bz, half = 28, 3
    for x in range(10, 33):
        w = half if x < 28 else max(0, half - (x - 28))
        for z in range(bz - w, bz + w + 1):
            b.fill(x, 1, z, x, deck - 1, z, "minecraft:gray_concrete")
            b.set(x, deck, z, "minecraft:smooth_stone")
        for z in (bz - w - 1, bz + w + 1):
            b.set(x, deck, z, "minecraft:gray_concrete")
            b.set(x, deck + 1, z, "minecraft:iron_bars")
    b.fill(15, deck + 1, bz - 2, 21, deck + 4, bz + 2, "minecraft:white_concrete")
    b.fill(16, deck + 3, bz - 2, 20, deck + 3, bz + 2, "minecraft:glass")
    b.fill(16, deck + 1, bz - 1, 20, deck + 2, bz + 1, "minecraft:air")
    b.fill(18, deck + 5, bz, 18, deck + 9, bz, "minecraft:iron_bars")
    b.set(25, deck + 1, bz, "minecraft:black_concrete")
    b.fill(26, deck + 2, bz, 28, deck + 2, bz, "minecraft:iron_bars")
    b.chest(17, deck + 1, bz, "kn8:chests/ariake_harbor", "north")
    # Farol no quebra-mar.
    fx, fz = 72, 39
    for y in range(deck + 2, deck + 18):
        color = "red" if (y // 3) % 2 else "white"
        b.fill(fx, y, fz, fx + 4, y, fz + 4, f"minecraft:{color}_concrete")
        b.fill(fx + 1, y, fz + 1, fx + 3, y, fz + 3, "minecraft:air")
    b.fill(fx - 1, deck + 18, fz - 1, fx + 5, deck + 18, fz + 5, "minecraft:smooth_stone")
    b.fill(fx + 2, deck + 2, fz + 1, fx + 2, deck + 18, fz + 1, "minecraft:ladder", dict(LADDER, facing="south"))
    b.fill(fx, deck + 2, fz + 2, fx, deck + 3, fz + 2, "minecraft:air")
    b.fill(fx, deck + 19, fz, fx + 4, deck + 21, fz + 4, "minecraft:glass")
    b.fill(fx + 1, deck + 19, fz + 1, fx + 3, deck + 21, fz + 3, "minecraft:air")
    b.set(fx + 2, deck + 20, fz + 2, "minecraft:sea_lantern")
    b.fill(fx, deck + 22, fz, fx + 4, deck + 22, fz + 4, "minecraft:red_concrete")
    b.entity(40, deck + 1, 31, soldier("rifle", "high", 180.0))
    b.entity(22, deck + 1, 16, soldier("rifle", "normal", 180.0))
    b.entity(58, deck + 1, 16, soldier("pistol", "normal", 180.0))
    b.jigsaw(40, 0, "north", LINK, LINK, "minecraft:empty", ROAD, y=deck, priority=PRIORITY)
    b.finish("ariake/harbor")


# --- lotes ----------------------------------------------------------------------------------------------------------

def lot_bigsight():
    """Tokyo Big Sight (Ariake): quatro piramides invertidas sobre pernas, por cima do saguao de vidro."""
    b = lot(15, 14, 22, 364)
    b.fill(1, 1, 2, 13, 5, 13, GLASS)
    b.fill(2, 1, 3, 12, 4, 12, "minecraft:air")
    b.fill(6, 1, 2, 8, 3, 2, "minecraft:air")
    b.fill(1, 6, 2, 13, 6, 13, "minecraft:light_gray_concrete")
    for cx, cz in ((4, 5), (10, 5), (4, 11), (10, 11)):
        b.fill(cx, 7, cz, cx, 9, cz, "minecraft:light_gray_concrete")
        for k in range(5):
            y = 10 + k
            r = k // 2 + 1
            for x in range(cx - r, cx + r + 1):
                for z in range(cz - r, cz + r + 1):
                    edge = abs(x - cx) == r or abs(z - cz) == r
                    b.set(x, y, z, "minecraft:gray_stained_glass" if not edge else "minecraft:light_gray_concrete")
    b.chest(2, 1, 12, "kn8:chests/city_shop", "east")
    b.finish("ariake/lot_bigsight")


def lot_warehouse():
    """Armazem de logistica da zona portuaria com docas de carga e um caminhao."""
    b = lot(15, 12, 10, 365, floor=ASPHALT)
    b.fill(1, 1, 4, 13, 7, 11, "minecraft:light_gray_concrete")
    b.fill(2, 1, 5, 12, 6, 10, "minecraft:air")
    for x in (3, 7, 11):
        b.fill(x, 1, 4, x + 1, 3, 4, "minecraft:air")
        b.fill(x, 4, 4, x + 1, 4, 4, "minecraft:yellow_concrete")
    b.fill(1, 7, 4, 13, 7, 11, "minecraft:gray_concrete")
    for x in range(1, 14):
        b.set(x, 6, 4, "minecraft:blue_concrete")
    b.fill(3, 1, 0, 6, 2, 2, "minecraft:white_concrete")
    b.fill(3, 1, 0, 6, 2, 0, "minecraft:gray_concrete")
    b.set(8, 1, 7, "minecraft:barrel", {"facing": "up", "open": "false"})
    b.chest(12, 1, 9, "kn8:chests/ariake_harbor", "west")
    b.finish("ariake/lot_warehouse")


def lot_canal():
    """Parque com canal (como o parque linear junto aos canais da baia): agua, passeio, ponte e arvores."""
    b = lot(15, 15, 8, 366, floor="minecraft:grass_block")
    for x in range(15):
        for z in range(15):
            b.set(x, 0, z, "minecraft:grass_block", {"snowy": "false"})
    for z in range(15):
        b.set(1, 0, z, SIDEWALK)
        b.set(13, 0, z, SIDEWALK)
        for x in range(5, 10):
            b.set(x, 0, z, "minecraft:water", {"level": "0"})
        b.set(4, 0, z, "minecraft:stone_bricks")
        b.set(10, 0, z, "minecraft:stone_bricks")
    b.fill(4, 1, 7, 10, 1, 8, "minecraft:spruce_slab", {"type": "bottom", "waterlogged": "false"})
    for z in (3, 11):
        tree(b, 2, z, 4)
        tree(b, 12, z, 4)
    street_lamp(b, 3, 7)
    b.finish("ariake/lot_canal")


ARIAKE_LOTS = [("ariake/lot_bigsight", 2), ("ariake/lot_warehouse", 4), ("ariake/lot_canal", 2),
               ("city/lot_office", 3), ("city/lot_apartment_tall", 2), ("city/lot_konbini", 2),
               ("city/lot_parking", 2)]


__all__ = ["ariake_station", "base_core", "ariake_harbor", "lot_bigsight", "lot_warehouse", "lot_canal",
           "ARIAKE_LOTS"]
