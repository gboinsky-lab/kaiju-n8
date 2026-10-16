#!/usr/bin/env python3
"""Locais da obra (construcoes C3): duas bases e duas cidades, pelas referencias do mangá/anime
(pesquisa e fontes em docs/LOCAIS_KAIJU_N8.md).

  ariake_base/core       Base Maritima de Ariake (Primeira Divisao, sede da Forca de Defesa): QG de vidro de 7
                         andares com heliporto, laboratorio de armas numeradas, bloco de detencao (onde o Kafka ficou),
                         catapulta eletromagnetica (lanca a Kikoru no ep. 22) e salao das divisoes; alas grandes da
                         base ao norte, oeste e leste
  ariake_base/harbor     porto ao sul (111 x 50, 3 blocos abaixo da base): cais, bacia com agua, barcos de patrulha,
                         guindastes de porto, conteineres, armazens, pier e farol
  tachikawa_base/core    Base de Tachikawa (Terceira Divisao: Mina, Hoshina): QG comprido com portico e torre com o
                         "3", sala de operacoes e patio de formatura; alas ao norte e oeste, campo de treino (a arena
                         com o painel) ao sul
  tachikawa_base/airfield aerodromo a leste (o Acampamento de Tachikawa real fica junto ao aerodromo): pista,
                         hangares com helicopteros, torre de controle, tanques de combustivel e heliportos
  shinagawa/station      estacao de Shinagawa: trilhos elevados com o Shinkansen e plataformas, saguao de vidro,
                         torres de escritorio do Shinagawa Intercity e praca da estacao
  shinagawa/lot_*        torre de escritorio alta, calcadao coberto (Musashi-Koyama Palm) e restos do formigueiro
                         (a horda de kaiju-formiga em Shinagawa)
  tachikawa/station      estacao de Tachikawa: monotrilho Tama elevado com estacao e trem, os dois lados da estacao
                         da JR ligados pela passarela, praca com fonte
  tachikawa/lot_*        loja de departamentos (GRANDUO), Parque Showa Kinen (flores, lago) e sala de shows (Stage
                         Garden)

As ruas das duas cidades sao as da cidade comum com pools proprios (gen_city.street_piece(prefix)); os lotes de cada
cidade misturam os proprios com os da cidade comum. Onde nascem: worldgen no fim (kn8:checked_jigsaw: longe de vilas,
sem agua, plano; conjunto kn8:defense_sites; /locate structure #kn8:defense_sites).
Uso: python3 tools/world/gen_sites.py
Ver no jogo: /place structure kn8:ariake_base_layout (sem conferir o terreno) ou kn8:ariake_base (conferindo).
"""
from gen_base import Piece, ROAD, element, stairs
from gen_city import ASPHALT, SIDEWALK, crossing_piece, floors_building, lot, street_end_piece, street_piece, \
    street_pools, tree, street_lamp
from gen_structures import DATA, dump, item, soldier

BASE_POOL = "kn8:defense_base"
GLASS = "minecraft:light_blue_stained_glass"
LANTERN = {"hanging": "true", "waterlogged": "false"}
# Cores das seis divisoes nas bandeiras do salao de Ariake [SUPOSICAO: a obra nao fixa cores por divisao].
DIVISION_COLORS = ["blue", "green", "orange", "red", "purple", "yellow"]
# "3" da Terceira Divisao (5 x 7, de cima para baixo).
DIGIT_3 = ["#####", "    #", "    #", " ####", "    #", "    #", "#####"]


def cross_roads(b, size, lo, hi):
    """Ruas das quatro saidas cruzando o centro (como no centro grande da base)."""
    for i in range(size):
        for w in range(lo, hi + 1):
            b.set(i, 0, w, ROAD)
            b.set(w, 0, i, ROAD)


def glyph(b, rows, x0, ytop, z, block, scale=1, mirror=False):
    """Desenha um padrao de '#' numa parede: lido certo de quem olha para o norte (x cresce para a direita);
    mirror = parede virada para o norte (quem olha para o sul ve x crescer para a esquerda)."""
    for r, line in enumerate(rows):
        for c, ch in enumerate(line[::-1] if mirror else line):
            if ch != "#":
                continue
            for dx in range(scale):
                for dy in range(scale):
                    b.set(x0 + c * scale + dx, ytop - r * scale - dy, z, block)


def flagpole(b, x, z, color, height=8):
    b.fill(x, 1, z, x, height, z, "minecraft:iron_bars")
    for y in range(height - 2, height + 1):
        b.set(x + 1, y, z, f"minecraft:{color}_wool")
        b.set(x + 2, y, z, f"minecraft:{color}_wool" if y != height - 1 else "minecraft:white_wool")


def doors(b, x0, z0, x1, z1, height=2):
    """Abre portas de 3 no meio dos quatro lados de um predio."""
    mx, mz = (x0 + x1) // 2, (z0 + z1) // 2
    for v in (-1, 0, 1):
        for x, z in ((mx + v, z0), (mx + v, z1), (x0, mz + v), (x1, mz + v)):
            b.fill(x, 1, z, x, height, z, "minecraft:air")


def columns(b, x0, z0, x1, z1, top, block, every=4):
    """Pilares na fachada (cortina de vidro com montantes)."""
    for x in range(x0, x1 + 1, every):
        for z in (z0, z1):
            b.fill(x, 1, z, x, top, z, block)
    for z in range(z0, z1 + 1, every):
        for x in (x0, x1):
            b.fill(x, 1, z, x, top, z, block)
    for x, z in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
        b.fill(x, 1, z, x, top, z, block)


def helipad(b, x0, z0, y=0, size=9):
    b.fill(x0, y, z0, x0 + size - 1, y, z0 + size - 1, "minecraft:black_concrete")
    for z in range(z0 + 2, z0 + size - 2):
        b.set(x0 + 2, y, z, "minecraft:yellow_concrete")
        b.set(x0 + size - 3, y, z, "minecraft:yellow_concrete")
    for x in range(x0 + 3, x0 + size - 3):
        b.set(x, y, z0 + size // 2, "minecraft:yellow_concrete")


def helicopter(b, x0, z0, y=1, color="blue"):
    """Helicoptero de blocos 3 x 9 (como o do hangar da base pequena)."""
    b.fill(x0, y, z0, x0 + 2, y + 2, z0 + 4, f"minecraft:{color}_concrete")
    b.fill(x0, y + 1, z0, x0 + 2, y + 2, z0, "minecraft:light_blue_stained_glass")
    b.fill(x0 + 1, y + 1, z0 + 5, x0 + 1, y + 1, z0 + 8, f"minecraft:{color}_concrete")
    b.fill(x0 + 1, y + 3, z0 - 2, x0 + 1, y + 3, z0 + 6, "minecraft:iron_bars")
    b.fill(x0 - 2, y + 3, z0 + 2, x0 + 4, y + 3, z0 + 2, "minecraft:iron_bars")


# --- Base Maritima de Ariake --------------------------------------------------------------------------------------

def ariake_core():
    b = Piece((51, 46, 51), 301)
    cross_roads(b, 51, 23, 27)
    for x in range(51):
        for z in range(51):
            if b.get(x, 0, z) is None and (x + z) % 5 == 0:
                b.set(x, 0, z, "minecraft:light_gray_concrete")
    # QG: torre de vidro 17 x 17, 7 andares de 5 (laje no 35), montantes brancos, portas nos quatro lados.
    x0, z0, x1, z1 = 17, 17, 33, 33
    top = floors_building(b, x0, z0, x1, z1, 7, "minecraft:white_concrete", GLASS, floor_h=5, glass_front=True)
    columns(b, x0, z0, x1, z1, top, "minecraft:white_concrete")
    for f in range(1, 7):
        for x in range(x0, x1 + 1):
            b.set(x, f * 5, z0, "minecraft:blue_concrete")
            b.set(x, f * 5, z1, "minecraft:blue_concrete")
        for z in range(z0, z1 + 1):
            b.set(x0, f * 5, z, "minecraft:blue_concrete")
            b.set(x1, f * 5, z, "minecraft:blue_concrete")
    doors(b, x0, z0, x1, z1, 3)
    # Saguao: bancadas, recepcao e o mapa de Toquio (mesas de cartografia).
    b.fill(x0 + 1, 0, z0 + 1, x1 - 1, 0, z1 - 1, "minecraft:polished_diorite")
    b.set(20, 1, 30, "kn8:defense_workbench", {"facing": "east"})
    b.set(20, 1, 28, "kn8:defense_workbench", {"facing": "east"})
    b.fill(23, 1, 29, 27, 1, 29, "minecraft:smooth_quartz_slab", {"type": "top", "waterlogged": "false"})
    for x in (24, 26):
        b.set(x, 1, 21, "minecraft:cartography_table")
    b.chest(30, 1, 20, "kn8:chests/base_command", "west")
    # Andares: sala de comando (2o), escritorio do Diretor-Geral (6o) com estantes e mesa.
    for x in (21, 24, 27, 30):
        b.set(x, 11, 22, "minecraft:cartography_table")
        b.set(x, 12, 19, "minecraft:observer", {"facing": "south", "powered": "false"})
    b.chest(30, 11, 30, "kn8:chests/base_command", "west")
    b.fill(19, 26, 31, 31, 27, 31, "minecraft:bookshelf")
    b.fill(23, 26, 25, 27, 26, 25, "minecraft:dark_oak_slab", {"type": "top", "waterlogged": "false"})
    b.set(25, 26, 27, "minecraft:lectern", {"facing": "north", "has_book": "false", "powered": "false"})
    b.chest(20, 26, 20, "kn8:chests/ariake_lab", "east")
    # Telhado: heliporto, guarda-corpo, antenas e o emblema (azul e branco) no alto.
    helipad(b, 21, 21, y=top)
    for i in range(x0, x1 + 1):
        for pos in ((i, z0), (i, z1), (x0, i), (x1, i)):
            b.set(pos[0], top + 1, pos[1], "minecraft:iron_bars")
    for x, z in ((18, 18), (32, 32)):
        b.fill(x, top + 1, z, x, top + 8, z, "minecraft:iron_bars")
        b.set(x, top + 9, z, "minecraft:lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
    b.fill(22, top + 1, 33, 28, top + 5, 33, "minecraft:blue_concrete")
    b.fill(23, top + 2, 33, 27, top + 4, 33, "minecraft:white_concrete")
    b.set(25, top + 3, 33, "minecraft:blue_concrete")
    # Noroeste: laboratorio de armas numeradas (vitrines com nucleos, bancada, suportes de pesquisa).
    b.building(2, 2, 20, 14, 6, wall="minecraft:white_concrete", stripe="minecraft:purple_concrete",
               door=("south", 10, 12))
    for cx in (4, 9, 14):
        b.fill(cx, 1, 4, cx + 2, 3, 6, "minecraft:glass")
        b.fill(cx + 1, 1, 5, cx + 1, 2, 5, "minecraft:air")
        b.set(cx + 1, 1, 5, "minecraft:magma_block")
        b.set(cx + 1, 2, 5, "minecraft:end_rod", {"facing": "up"})
    b.set(4, 1, 12, "kn8:defense_workbench", {"facing": "north"})
    b.set(6, 1, 12, "minecraft:brewing_stand",
          {"has_bottle_0": "false", "has_bottle_1": "false", "has_bottle_2": "false"})
    for x in (12, 15, 18):
        b.chest(x, 1, 12, "kn8:chests/ariake_lab", "north")
    # Nordeste: bloco de detencao (celas de grade; a do fundo tem a cama do Kafka).
    b.building(30, 2, 48, 16, 5, wall="minecraft:gray_concrete", stripe="minecraft:red_concrete",
               door=("south", 38, 40))
    b.fill(31, 1, 9, 47, 4, 9, "minecraft:iron_bars")
    for x in (36, 41):
        b.fill(x, 1, 3, x, 4, 8, "minecraft:iron_bars")
    for cx in (31, 37, 42):
        b.bed(cx + 1, 4, "south", "white")
    b.set(45, 1, 4, "minecraft:barrel", {"facing": "up", "open": "false"},
          {"id": "minecraft:barrel", "LootTable": "kn8:chests/base_barracks"})
    b.fill(33, 1, 13, 45, 1, 13, "minecraft:gray_carpet")
    b.entity(39, 1, 12, soldier("rifle", "elite", 0.0))
    b.entity(39, 1, 18, soldier("rifle", "high", 0.0))
    # Sudoeste: catapulta eletromagnetica. Rampa subindo para leste (trilhos de cobre, capacitores com para-raios).
    for x in range(2, 21):
        y = 1 + (x - 4) * 10 // 16 if x >= 4 else 1
        b.fill(x, y, 38, x, y, 40, "minecraft:polished_blackstone_slab", {"type": "top", "waterlogged": "false"})
        b.set(x, y, 37, "minecraft:waxed_cut_copper")
        b.set(x, y, 41, "minecraft:waxed_cut_copper")
        b.set(x, y + 1, 37, "minecraft:waxed_copper_grate")
        b.set(x, y + 1, 41, "minecraft:waxed_copper_grate")
        if x % 3 == 0 and y > 1:
            b.fill(x, 1, 39, x, y - 1, 39, "minecraft:iron_block")
    b.fill(2, 1, 38, 4, 1, 40, "minecraft:redstone_block")
    b.fill(2, 2, 39, 2, 3, 39, "minecraft:polished_blackstone_wall",
           {"east": "none", "north": "none", "south": "none", "up": "true", "waterlogged": "false", "west": "none"})
    for cx in (5, 11, 17):
        b.fill(cx, 1, 31, cx + 2, 4, 33, "minecraft:waxed_copper_block")
        b.set(cx + 1, 5, 32, "minecraft:lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
    b.building(4, 44, 12, 48, 4, wall="minecraft:light_gray_concrete", stripe="minecraft:yellow_concrete",
               door=("north", 7, 9))
    b.set(8, 1, 46, "minecraft:lever", {"face": "floor", "facing": "north", "powered": "false"})
    b.set(6, 1, 46, "minecraft:observer", {"facing": "north", "powered": "false"})
    # Sudeste: salao das divisoes (mesa longa) e as seis bandeiras no patio.
    b.building(30, 30, 48, 41, 7, wall="minecraft:white_concrete", stripe="minecraft:blue_concrete",
               door=("north", 38, 40))
    b.fill(33, 1, 35, 45, 1, 36, "minecraft:dark_oak_slab", {"type": "top", "waterlogged": "false"})
    for x in range(33, 46, 2):
        b.set(x, 1, 34, "minecraft:spruce_stairs", stairs("south"))
        b.set(x, 1, 37, "minecraft:spruce_stairs", stairs("north"))
    for k, color in enumerate(DIVISION_COLORS):
        flagpole(b, 30 + k * 3, 45, color)
    for x, z in ((3, 22), (3, 28), (47, 22), (47, 28), (22, 3), (28, 3), (22, 47), (28, 47)):
        b.lamp_post(x, z)
    for x, z, yaw in ((25, 16, 180.0), (25, 34, 0.0), (16, 25, 90.0), (34, 25, 270.0)):
        b.entity(x, 1, z, soldier("rifle", "high", yaw))
    for x, z, facing, pool in ((25, 0, "north", "large_wings"), (0, 25, "west", "large_wings"),
                               (50, 25, "east", "large_wings")):
        b.jigsaw(x, z, facing, "kn8:base_core_l", "kn8:base_wing_l", f"{BASE_POOL}/{pool}", ROAD)
    b.jigsaw(25, 50, "south", "kn8:base_core_l", "kn8:base_wing_l", "kn8:ariake_base/harbor", ROAD)
    b.finish("ariake_base/core")


def ariake_harbor():
    """Porto 111 x 50: encaixa no centro pelo jigsaw em (55, 3, 0); o cais (y 3) fica no nivel da base e a bacia
    (agua y 1-3, fundo y 0) abaixo dela."""
    deck = 3
    b = Piece((111, 30, 50), 302, floor="minecraft:stone")
    rnd = b.random
    basin = (8, 22, 102, 45)
    for x in range(111):
        for z in range(50):
            inside = basin[0] <= x <= basin[2] and basin[1] <= z <= basin[3]
            if inside:
                b.set(x, 0, z, "minecraft:gravel" if rnd.random() < 0.6 else "minecraft:mud")
                b.fill(x, 1, z, x, deck, z, "minecraft:water", {"level": "0"})
            else:
                b.fill(x, 0, z, x, deck - 1, z, "minecraft:stone")
                tile = "gray_concrete" if (x // 4 + z // 4) % 2 else "light_gray_concrete"
                b.set(x, deck, z, f"minecraft:{tile}")
    # Borda do cais (amarela e preta) e defensas de pneu (blocos pretos) ao longo da agua.
    for x in range(basin[0] - 1, basin[2] + 2):
        b.set(x, deck, basin[1] - 1, "minecraft:yellow_concrete" if x % 2 else "minecraft:black_concrete")
        if x % 6 == 0:
            b.set(x, deck - 1, basin[1] - 1, "minecraft:black_concrete")
    for z in range(0, 3):
        for x in range(111):
            b.set(x, deck, z, ROAD)
    for z in range(3, basin[1] - 1):
        for x in range(53, 58):
            b.set(x, deck, z, ROAD)
    # Quebra-mar ao sul e muro dos lados.
    for x in range(111):
        for z in (46, 47, 48, 49):
            b.fill(x, 0, z, x, deck + 1, z, "minecraft:stone_bricks" if z < 49 else "minecraft:polished_andesite")
    for z in range(50):
        for x in (0, 110):
            b.fill(x, deck + 1, z, x, deck + 2, z, "minecraft:light_gray_concrete")
            b.set(x, deck + 3, z, "minecraft:iron_bars")
    # Guindastes de porto: pernas no cais, viga ate em cima da bacia, cabine e gancho.
    for c in (24, 86):
        for x in (c - 3, c + 3):
            for z in (8, 19):
                b.fill(x, deck + 1, z, x, deck + 16, z, "minecraft:orange_concrete")
            b.fill(x, deck + 17, 4, x, deck + 17, 34, "minecraft:orange_concrete")
        for z in (4, 8, 19, 34):
            b.fill(c - 3, deck + 17, z, c + 3, deck + 17, z, "minecraft:orange_concrete")
        b.fill(c - 1, deck + 15, 28, c + 1, deck + 16, 30, "minecraft:white_concrete")
        b.fill(c - 1, deck + 16, 28, c + 1, deck + 16, 28, "minecraft:glass")
        b.fill(c, deck + 9, 32, c, deck + 16, 32, "minecraft:chain", {"axis": "y", "waterlogged": "false"})
        b.fill(c - 1, deck + 6, 31, c + 1, deck + 8, 33, "minecraft:blue_concrete")
    # Conteineres empilhados (3 x 3 x 7) e armazens.
    colors = ["red", "blue", "green", "orange", "gray", "cyan"]
    for row, z in enumerate((5, 11)):
        for k, x in enumerate((4, 12, 76, 92, 100)):
            for level in range(1 + (k + row) % 2 + 1):
                color = colors[(k * 3 + row + level) % len(colors)]
                y = deck + 1 + level * 3
                b.fill(x, y, z, x + 6, y + 2, z + 2, f"minecraft:{color}_concrete")
                b.fill(x, y, z + 1, x, y + 2, z + 1, "minecraft:iron_bars")
    for wx0 in (34, 59):
        b.fill(wx0, deck + 1, 5, wx0 + 13, deck + 8, 16, "minecraft:light_gray_concrete")
        b.fill(wx0 + 1, deck + 1, 6, wx0 + 12, deck + 7, 15, "minecraft:air")
        b.fill(wx0 + 4, deck + 1, 16, wx0 + 9, deck + 4, 16, "minecraft:air")
        b.fill(wx0, deck + 8, 5, wx0 + 13, deck + 8, 16, "minecraft:gray_concrete")
        for x in range(wx0, wx0 + 14):
            b.set(x, deck + 7, 16, "minecraft:blue_concrete")
        for x in range(wx0 + 2, wx0 + 12, 3):
            b.fill(x, deck + 1, 7, x + 1, deck + 2, 8, "minecraft:spruce_planks")
            b.set(x, deck + 7, 11, "minecraft:lantern", LANTERN)
        b.chest(wx0 + 2, deck + 1, 14, "kn8:chests/ariake_harbor", "east")
    # Pier de madeira entrando na bacia, com postes.
    b.fill(51, deck, 22, 54, deck, 38, "minecraft:spruce_planks")
    for z in (24, 30, 36):
        for x in (51, 54):
            b.fill(x, 0, z, x, deck - 1, z, "minecraft:spruce_log", {"axis": "y"})
            b.set(x, deck + 1, z, "minecraft:spruce_fence")
            b.set(x, deck + 2, z, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
    # Barco de patrulha (casco na agua, convés no nivel do cais, ponte, mastro e canhao na proa).
    def boat(bx0, bx1, cz, half, cabin):
        for x in range(bx0, bx1 + 1):
            w = half if x < bx1 - 5 else max(0, half - (x - (bx1 - 5)))
            for z in range(cz - w, cz + w + 1):
                b.fill(x, 1, z, x, deck - 1, z, "minecraft:gray_concrete")
                b.set(x, deck, z, "minecraft:smooth_stone")
            for z in (cz - w - 1, cz + w + 1):
                b.set(x, deck, z, "minecraft:gray_concrete")
                b.set(x, deck + 1, z, "minecraft:iron_bars")
            b.set(x, deck - 1, cz - w - 1, "minecraft:white_concrete")
            b.set(x, deck - 1, cz + w + 1, "minecraft:white_concrete")
        cx0, cx1 = cabin
        b.fill(cx0, deck + 1, cz - half + 1, cx1, deck + 4, cz + half - 1, "minecraft:white_concrete")
        b.fill(cx0 + 1, deck + 3, cz - half + 1, cx1 - 1, deck + 3, cz + half - 1, "minecraft:glass")
        b.fill(cx0 + 1, deck + 1, cz - half + 2, cx1 - 1, deck + 2, cz + half - 2, "minecraft:air")
        b.fill((cx0 + cx1) // 2, deck + 5, cz, (cx0 + cx1) // 2, deck + 9, cz, "minecraft:iron_bars")
        b.set((cx0 + cx1) // 2, deck + 10, cz, "minecraft:sea_lantern")
        gx = bx1 - 7
        b.set(gx, deck + 1, cz, "minecraft:black_concrete")
        b.fill(gx + 1, deck + 2, cz, gx + 3, deck + 2, cz, "minecraft:iron_bars")
    boat(16, 44, 31, 3, (24, 31))
    boat(64, 82, 40, 2, (68, 73))
    b.chest(27, deck + 1, 31, "kn8:chests/ariake_harbor", "north")
    # Farol no quebra-mar (faixas vermelhas e brancas, lanterna de vidro).
    fx, fz = 98, 45
    b.fill(fx, 0, fz, fx + 4, deck + 1, fz, "minecraft:stone_bricks")
    for y in range(deck + 2, deck + 20):
        color = "red" if (y // 3) % 2 else "white"
        b.fill(fx, y, fz, fx + 4, y, fz + 4, f"minecraft:{color}_concrete")
        b.fill(fx + 1, y, fz + 1, fx + 3, y, fz + 3, "minecraft:air")
    b.fill(fx - 1, deck + 20, fz - 1, fx + 5, deck + 20, fz + 5, "minecraft:smooth_stone")
    b.fill(fx + 2, deck + 2, fz + 1, fx + 2, deck + 20, fz + 1, "minecraft:ladder",
           {"facing": "south", "waterlogged": "false"})
    b.fill(fx, deck + 2, fz + 2, fx, deck + 3, fz + 2, "minecraft:air")
    b.fill(fx, deck + 21, fz, fx + 4, deck + 23, fz + 4, "minecraft:glass")
    b.fill(fx + 1, deck + 21, fz + 1, fx + 3, deck + 23, fz + 3, "minecraft:air")
    b.set(fx + 2, deck + 22, fz + 2, "minecraft:sea_lantern")
    b.set(fx + 2, deck + 21, fz + 1, "minecraft:air")
    b.fill(fx, deck + 24, fz, fx + 4, deck + 24, fz + 4, "minecraft:red_concrete")
    for x in range(10, 101, 10):
        if not 50 <= x <= 55:
            b.fill(x, deck + 1, 20, x, deck + 3, 20, "minecraft:polished_andesite")
            b.set(x, deck + 4, 20, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
    b.entity(53, deck + 1, 37, soldier("rifle", "high", 180.0))
    b.entity(30, deck + 1, 18, soldier("rifle", "normal", 180.0))
    b.entity(80, deck + 1, 18, soldier("pistol", "normal", 180.0))
    b.entity(28, deck + 1, 31, soldier("rifle", "normal", 90.0))
    b.jigsaw(55, 0, "north", "kn8:base_wing_l", "kn8:base_core_l", "minecraft:empty", ROAD, y=deck)
    b.finish("ariake_base/harbor")


# --- Base de Tachikawa --------------------------------------------------------------------------------------------

def tachikawa_core():
    b = Piece((51, 34, 51), 311)
    cross_roads(b, 51, 23, 27)
    # QG da Terceira Divisao: predio comprido de 4 andares (z 4-18) com portico na rua norte.
    x0, z0, x1, z1 = 4, 4, 46, 18
    b.fill(x0, 1, z0, x1, 20, z1, "minecraft:white_concrete")
    b.fill(x0 + 1, 1, z0 + 1, x1 - 1, 19, z1 - 1, "minecraft:air")
    for f in range(4):
        y = f * 5
        if f:
            b.fill(x0 + 1, y, z0 + 1, x1 - 1, y, z1 - 1, "minecraft:polished_deepslate")
        for x in range(x0 + 1, x1):
            if (x - x0) % 3:
                for z in (z0, z1):
                    b.fill(x, y + 2, z, x, y + 3, z, "minecraft:light_gray_stained_glass_pane")
        for x in range(x0, x1 + 1):
            b.set(x, y + 5, z0, "minecraft:orange_terracotta")
            b.set(x, y + 5, z1, "minecraft:orange_terracotta")
        for x in range(x0 + 4, x1, 7):
            b.set(x, y + 4, (z0 + z1) // 2, "minecraft:lantern", LANTERN)
    b.fill(x0, 20, z0, x1, 20, z1, "minecraft:gray_concrete")
    # Portico: a rua norte atravessa o terreo do QG.
    b.fill(22, 1, z0, 28, 4, z1, "minecraft:air")
    for x in (22, 28):
        b.fill(x, 1, z0 + 1, x, 4, z1 - 1, "minecraft:white_concrete")
    for z in range(z0, z1 + 1):
        for x in range(23, 28):
            b.set(x, 0, z, ROAD)
    b.fill(23, 5, z0, 27, 5, z1, "minecraft:polished_deepslate")
    for z in (z0 + 3, z1 - 3):
        b.set(25, 4, z, "minecraft:lantern", LANTERN)
    # Portas dos dois lados do portico e escada de mao.
    for x in (22, 28):
        b.fill(x, 1, 11, x, 2, 11, "minecraft:air")
    b.fill(x1 - 1, 1, z1 - 1, x1 - 1, 20, z1 - 1, "minecraft:ladder", {"facing": "north", "waterlogged": "false"})
    for f in range(1, 4):
        b.set(x1 - 1, f * 5, z1 - 1, "minecraft:ladder", {"facing": "north", "waterlogged": "false"})
    b.set(x1 - 1, 20, z1 - 1, "minecraft:air")
    # Torre do relogio com o "3" da divisao virado para o patio (sul).
    b.fill(21, 21, 9, 29, 30, 15, "minecraft:white_concrete")
    b.fill(22, 21, 10, 28, 29, 14, "minecraft:air")
    b.fill(21, 31, 9, 29, 31, 15, "minecraft:orange_concrete")
    glyph(b, DIGIT_3, 23, 29, 15, "minecraft:orange_concrete")
    glyph(b, DIGIT_3, 23, 29, 9, "minecraft:orange_concrete", mirror=True)
    b.set(25, 32, 12, "minecraft:lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
    # Dentro: escritorio da capita (oeste) e do vice-capitao (leste) no 3o andar; recepcao no terreo.
    b.set(8, 1, 15, "kn8:defense_workbench", {"facing": "north"})
    b.set(10, 1, 15, "kn8:defense_workbench", {"facing": "north"})
    b.chest(6, 1, 6, "kn8:chests/base_command", "south")
    b.chest(40, 1, 6, "kn8:chests/base_armory", "south")
    b.fill(7, 11, 15, 12, 12, 15, "minecraft:bookshelf")
    b.bed(15, 12, "south", "orange", y=11)
    b.set(34, 11, 10, "minecraft:lectern", {"facing": "south", "has_book": "false", "powered": "false"})
    b.chest(36, 11, 15, "kn8:chests/base_command", "north")
    # Sudoeste: sala de operacoes (telas de observador e mapas).
    b.building(3, 31, 20, 46, 6, wall="minecraft:light_gray_concrete", stripe="minecraft:orange_concrete",
               door=("east", 37, 39))
    for z in (34, 38, 42):
        for x in (6, 10, 14):
            b.set(x, 1, z, "minecraft:cartography_table")
        b.set(4, 2, z, "minecraft:observer", {"facing": "east", "powered": "false"})
    b.chest(18, 1, 44, "kn8:chests/base_command", "west")
    # Sudeste: patio de formatura com a bandeira da divisao e a tropa em fila.
    b.fill(30, 0, 30, 47, 0, 47, "minecraft:polished_andesite")
    flagpole(b, 38, 46, "orange", 10)
    flagpole(b, 33, 46, "blue")
    flagpole(b, 43, 46, "white")
    for x in (32, 35, 38, 41, 44):
        b.entity(x, 1, 36, soldier("rifle", "normal", 180.0))
    for x, z in ((2, 22), (2, 28), (48, 22), (48, 28), (22, 48), (28, 48)):
        b.lamp_post(x, z)
    b.entity(25, 1, 20, soldier("rifle", "high", 0.0))
    b.entity(20, 1, 25, soldier("sword", "high", 90.0))
    wings = f"{BASE_POOL}/large_wings"
    for x, z, facing, pool in ((25, 0, "north", wings), (0, 25, "west", wings),
                               (25, 50, "south", f"{BASE_POOL}/large_arena"),
                               (50, 25, "east", "kn8:tachikawa_base/airfield")):
        b.jigsaw(x, z, facing, "kn8:base_core_l", "kn8:base_wing_l", pool, ROAD)
    b.finish("tachikawa_base/core")


def tachikawa_airfield():
    """Aerodromo 51 x 72: encaixa no centro pelo jigsaw em (25, 0, 0); pista ao longo de z."""
    b = Piece((51, 26, 72), 312, floor="minecraft:grass_block")
    for x in range(51):
        for z in range(72):
            b.set(x, 0, z, "minecraft:grass_block", {"snowy": "false"})
    for z in range(0, 4):
        for x in range(51):
            b.set(x, 0, z, ROAD)
    # Pista 13 x 64 com faixa central, cabeceiras e luzes; taxiway ao lado.
    for x in range(19, 32):
        for z in range(5, 70):
            b.set(x, 0, z, "minecraft:black_concrete")
    for z in range(8, 67):
        if z % 6 < 3:
            b.set(25, 0, z, "minecraft:white_concrete")
    for z in list(range(6, 10)) + list(range(65, 69)):
        for x in range(20, 31, 2):
            b.set(x, 0, z, "minecraft:white_concrete")
    for z in range(6, 70, 8):
        b.set(19, 0, z, "minecraft:sea_lantern")
        b.set(31, 0, z, "minecraft:sea_lantern")
    for x in range(32, 37):
        for z in range(4, 70):
            b.set(x, 0, z, "minecraft:gray_concrete")
    for z in range(4, 70):
        b.set(34, 0, z, "minecraft:yellow_concrete" if z % 4 < 2 else "minecraft:gray_concrete")
    # Hangares a oeste (abertos para a pista) com helicopteros.
    for hz0 in (6, 27):
        hz1 = hz0 + 17
        b.fill(2, 0, hz0, 16, 0, hz1, "minecraft:polished_andesite")
        # Paredes de fundo e dos lados ate o arco do telhado (casca em degraus; a frente, x 16, fica aberta).
        b.fill(2, 1, hz0, 2, 9, hz1, "minecraft:light_gray_concrete")
        for z in (hz0, hz1):
            b.fill(2, 1, z, 16, 13, z, "minecraft:light_gray_concrete")
            for k in range(4):
                b.fill(2, 10 + k, z, 1 + k, 10 + k, z, "minecraft:air")
                b.fill(17 - k, 10 + k, z, 16, 10 + k, z, "minecraft:air")
        for k in range(4):
            for z in range(hz0, hz1 + 1):
                b.set(2 + k, 10 + k, z, "minecraft:gray_concrete")
                b.set(16 - k, 10 + k, z, "minecraft:gray_concrete")
        b.fill(6, 13, hz0, 12, 13, hz1, "minecraft:gray_concrete")
        helicopter(b, 7, hz0 + 4)
        helicopter(b, 11, hz0 + 7, color="gray")
        b.set(3, 1, hz0 + 2, "minecraft:barrel", {"facing": "up", "open": "false"})
        b.set(3, 1, hz1 - 2, "minecraft:lava_cauldron")
        b.chest(3, 1, hz0 + 8, "kn8:chests/base_armory", "east")
        b.set(9, 8, hz0 + 8, "minecraft:lantern", LANTERN)
    helipad(b, 4, 49)
    helipad(b, 4, 60)
    helicopter(b, 7, 52, color="green")
    # Torre de controle a leste: coluna, cabine de vidro, antena.
    b.fill(40, 1, 8, 45, 15, 13, "minecraft:white_concrete")
    b.fill(41, 1, 9, 44, 14, 12, "minecraft:air")
    b.fill(42, 1, 12, 42, 16, 12, "minecraft:ladder", {"facing": "north", "waterlogged": "false"})
    b.fill(39, 16, 7, 46, 16, 14, "minecraft:smooth_stone")
    b.fill(39, 17, 7, 46, 19, 14, "minecraft:light_blue_stained_glass")
    b.fill(40, 17, 8, 45, 19, 13, "minecraft:air")
    b.set(42, 16, 12, "minecraft:ladder", {"facing": "north", "waterlogged": "false"})
    b.fill(39, 20, 7, 46, 20, 14, "minecraft:gray_concrete")
    b.fill(45, 21, 13, 45, 24, 13, "minecraft:iron_bars")
    b.set(45, 25, 13, "minecraft:redstone_lamp", {"lit": "false"})
    for x in (41, 43):
        b.set(x, 17, 8, "minecraft:observer", {"facing": "south", "powered": "false"})
    b.fill(40, 1, 10, 40, 2, 11, "minecraft:air")
    # Tanques de combustivel e caminhao-tanque.
    for tz in (32, 40, 48):
        b.fill(40, 1, tz, 44, 6, tz + 4, "minecraft:white_concrete")
        b.fill(40, 1, tz, 40, 6, tz, "minecraft:air")
        b.fill(44, 1, tz, 44, 6, tz, "minecraft:air")
        b.fill(40, 1, tz + 4, 40, 6, tz + 4, "minecraft:air")
        b.fill(44, 1, tz + 4, 44, 6, tz + 4, "minecraft:air")
        b.fill(41, 6, tz + 1, 43, 6, tz + 3, "minecraft:red_concrete")
    b.fill(46, 1, 58, 48, 1, 66, "minecraft:black_concrete")
    b.fill(46, 2, 58, 48, 3, 60, "minecraft:green_concrete")
    b.fill(46, 2, 61, 48, 3, 66, "minecraft:white_concrete")
    # Cerca de fora (o aerodromo e a borda da base).
    for z in range(4, 72):
        for x in (0, 50):
            b.fill(x, 1, z, x, 3, z, "minecraft:iron_bars")
    for x in range(51):
        b.fill(x, 1, 71, x, 3, 71, "minecraft:iron_bars")
    for x, z in ((38, 22), (38, 56), (17, 46)):
        b.lamp_post(x, z)
    b.entity(36, 1, 20, soldier("rifle", "normal", 180.0))
    b.entity(14, 1, 30, soldier("pistol", "normal", 90.0))
    b.entity(43, 17, 10, soldier("rifle", "normal", 0.0))
    b.jigsaw(25, 0, "north", "kn8:base_wing_l", "kn8:base_core_l", "minecraft:empty", ROAD)
    b.finish("tachikawa_base/airfield")


# --- cidades ------------------------------------------------------------------------------------------------------

def station_jigsaws(b, prefix, size=41, final="minecraft:polished_andesite"):
    """Saidas de rua no meio dos lados e oito lotes perto dos cantos (como a praca da cidade comum)."""
    mid, last = size // 2, size - 1
    for x, z, facing in ((mid, 0, "north"), (mid, last, "south"), (0, mid, "west"), (last, mid, "east")):
        b.jigsaw(x, z, facing, "kn8:city_link", "kn8:city_street", f"kn8:{prefix}/streets", final)
    for x, z, facing in ((4, 0, "north"), (last - 4, 0, "north"), (4, last, "south"), (last - 4, last, "south"),
                         (0, 4, "west"), (0, last - 4, "west"), (last, 4, "east"), (last, last - 4, "east")):
        b.jigsaw(x, z, facing, "kn8:city_slot", "kn8:city_lot", f"kn8:{prefix}/lots", final)


def shinagawa_station():
    b = Piece((41, 62, 41), 321, floor="minecraft:polished_andesite")
    for x in range(41):
        for z in range(41):
            if (x + z) % 4 == 0:
                b.set(x, 0, z, "minecraft:smooth_stone")
    for i in range(41):
        for w in range(16, 25):
            b.set(i, 0, w, ASPHALT)
            b.set(w, 0, i, ASPHALT)
    # Trilhos elevados (leste-oeste): laje no y 8, quatro linhas, plataforma no meio com cobertura.
    deck = 8
    b.fill(0, deck, 13, 40, deck, 27, "minecraft:smooth_stone")
    b.fill(0, deck - 1, 13, 40, deck - 1, 13, "minecraft:light_gray_concrete")
    b.fill(0, deck - 1, 27, 40, deck - 1, 27, "minecraft:light_gray_concrete")
    for x in (2, 10, 30, 38):
        for z in (13, 20, 27):
            b.fill(x, 1, z, x, deck - 1, z, "minecraft:light_gray_concrete")
    for z in (15, 18, 22, 25):
        b.fill(0, deck + 1, z, 40, deck + 1, z, "minecraft:rail", {"shape": "east_west", "waterlogged": "false"})
    b.fill(0, deck + 1, 19, 40, deck + 1, 21, "minecraft:smooth_stone_slab", {"type": "top", "waterlogged": "false"})
    for x in range(1, 40, 2):
        b.set(x, deck + 1, 19, "minecraft:yellow_concrete")
        b.set(x, deck + 1, 21, "minecraft:yellow_concrete")
    for x in range(2, 40, 6):
        b.fill(x, deck + 2, 20, x, deck + 4, 20, "minecraft:iron_bars")
        b.set(x, deck + 4, 20, "minecraft:sea_lantern")
    b.fill(0, deck + 5, 18, 40, deck + 5, 22, "minecraft:white_concrete")
    for x in range(0, 41):
        b.set(x, deck + 1, 13, "minecraft:iron_bars")
        b.set(x, deck + 1, 27, "minecraft:iron_bars")
    # Shinkansen (branco com faixa azul) parado na linha de cima.
    for car in range(3):
        cx0 = 2 + car * 12
        b.fill(cx0, deck + 2, 24, cx0 + 10, deck + 4, 26, "minecraft:white_concrete")
        b.fill(cx0, deck + 3, 24, cx0 + 10, deck + 3, 24, "minecraft:light_blue_stained_glass")
        b.fill(cx0, deck + 2, 24, cx0 + 10, deck + 2, 26, "minecraft:blue_concrete")
        b.fill(cx0, deck + 3, 26, cx0 + 10, deck + 3, 26, "minecraft:light_blue_stained_glass")
    b.fill(38, deck + 2, 24, 40, deck + 3, 26, "minecraft:white_concrete")
    b.set(40, deck + 4, 25, "minecraft:white_concrete")
    # Saguao de vidro ao sul dos trilhos (bilheteria e catracas), com escada ate a plataforma.
    sx0, sz0, sx1, sz1 = 3, 29, 15, 37
    for xa, xb in ((sx0, sx1), (25, 37)):
        b.fill(xa, 1, sz0, xb, 6, sz1, GLASS)
        b.fill(xa + 1, 1, sz0 + 1, xb - 1, 5, sz1 - 1, "minecraft:air")
        b.fill(xa, 6, sz0, xb, 6, sz1, "minecraft:white_concrete")
        columns(b, xa, sz0, xb, sz1, 6, "minecraft:white_concrete")
        b.fill(xa + 1, 0, sz0 + 1, xb - 1, 0, sz1 - 1, "minecraft:polished_diorite")
        mid = (xa + xb) // 2
        b.fill(mid - 1, 1, sz1, mid + 1, 3, sz1, "minecraft:air")
        b.fill(mid - 1, 1, sz0, mid + 1, 3, sz0, "minecraft:air")
        for x in range(xa + 2, xb - 1, 2):
            b.set(x, 1, sz0 + 3, "minecraft:iron_bars")
        b.set(xa + 2, 4, sz0 + 4, "minecraft:lantern", LANTERN)
    # Escada de mao num pilar ate a plataforma (sem o guarda-corpo em cima dela).
    b.fill(14, 1, 27, 14, deck - 1, 27, "minecraft:light_gray_concrete")
    b.fill(14, 1, 28, 14, deck, 28, "minecraft:ladder", {"facing": "south", "waterlogged": "false"})
    b.set(14, deck + 1, 27, "minecraft:air")
    # Torres do Shinagawa Intercity ao norte (vidro escuro, coroa no alto).
    for tx0 in (2, 28):
        top = floors_building(b, tx0, 1, tx0 + 10, 11, 13, "minecraft:gray_concrete", "minecraft:gray_stained_glass",
                              glass_front=True)
        columns(b, tx0, 1, tx0 + 10, 11, top, "minecraft:light_gray_concrete", every=5)
        b.fill(tx0 + 2, top + 1, 3, tx0 + 8, top + 3, 9, "minecraft:light_gray_concrete")
        b.fill(tx0 + 3, top + 4, 4, tx0 + 7, top + 4, 8, "minecraft:gray_concrete")
        b.set(tx0 + 5, top + 5, 6, "minecraft:redstone_lamp", {"lit": "false"})
        b.fill(tx0 + 4, 1, 11, tx0 + 6, 2, 11, "minecraft:air")
        b.chest(tx0 + 2, 1, 9, "kn8:chests/city_shop", "east")
    # Praca da estacao (sul): ponto de taxi, bancos, arvores.
    for x in (3, 37):
        tree(b, x, 39, 4)
    for x in range(26, 35, 2):
        b.set(x, 1, 39, "minecraft:spruce_stairs", stairs("north"))
    b.fill(5, 1, 39, 14, 1, 39, "minecraft:yellow_concrete")
    for x in (6, 10):
        b.fill(x, 1, 40, x + 1, 1, 40, "minecraft:black_concrete")
    for x, z in ((1, 28), (39, 28), (1, 12), (39, 12)):
        street_lamp(b, x, z)
    station_jigsaws(b, "shinagawa")
    b.finish("shinagawa/station")


def shinagawa_tower():
    b = lot(15, 14, 78, 322)
    top = floors_building(b, 0, 1, 14, 13, 17, "minecraft:gray_concrete", "minecraft:light_blue_stained_glass",
                          glass_front=True)
    columns(b, 0, 1, 14, 13, top, "minecraft:white_concrete", every=7)
    b.fill(5, 1, 1, 9, 3, 1, "minecraft:glass")
    b.fill(6, 1, 1, 8, 2, 1, "minecraft:air")
    b.fill(2, top + 1, 3, 12, top + 1, 11, "minecraft:light_gray_concrete")
    b.fill(4, top + 2, 5, 10, top + 2, 9, "minecraft:gray_concrete")
    b.fill(7, top + 3, 7, 7, top + 6, 7, "minecraft:iron_bars")
    b.set(7, top + 7, 7, "minecraft:redstone_lamp", {"lit": "false"})
    b.chest(2, 1, 11, "kn8:chests/city_shop", "east")
    b.finish("shinagawa/lot_tower")


def shinagawa_arcade():
    """Calcadao coberto (shotengai do Musashi-Koyama Palm): corredor de 5 com teto de vidro e lojas dos lados."""
    b = lot(15, 15, 12, 323)
    for z in range(15):
        for x in range(5, 10):
            b.set(x, 0, z, "minecraft:polished_andesite" if (x + z) % 2 else "minecraft:smooth_stone")
    # Arco de entrada com a placa colorida.
    for x in (4, 10):
        b.fill(x, 1, 1, x, 8, 1, "minecraft:white_concrete")
    b.fill(4, 8, 1, 10, 9, 1, "minecraft:white_concrete")
    for x, color in zip(range(5, 10), ("red", "orange", "yellow", "lime", "light_blue")):
        b.set(x, 8, 1, f"minecraft:{color}_concrete")
    # Teto em arco de vidro sobre o corredor.
    for z in range(1, 15):
        b.set(4, 7, z, "minecraft:white_concrete")
        b.set(10, 7, z, "minecraft:white_concrete")
        b.fill(5, 8, z, 9, 8, z, "minecraft:glass")
        b.set(5, 8, z, "minecraft:white_concrete")
        b.set(9, 8, z, "minecraft:white_concrete")
        b.fill(6, 9, z, 8, 9, z, "minecraft:glass")
        if z % 4 == 2:
            b.set(7, 7, z, "minecraft:lantern", LANTERN)
    # Lojas dos dois lados (4 de cada), toldos de la colorida, balcoes e baus.
    awnings = ["red", "blue", "green", "orange", "purple", "cyan", "yellow", "pink"]
    for side, (xa, xb, face, counter) in enumerate(((0, 4, "east", 4), (10, 14, "west", 10))):
        for k, (za, zb) in enumerate(((2, 4), (5, 8), (9, 11), (12, 14))):
            color = awnings[side * 4 + k]
            b.fill(xa, 1, za, xb, 6, zb, "minecraft:white_concrete" if k % 2 else "minecraft:smooth_sandstone")
            inner0, inner1 = xa + 1, xb - 1
            b.fill(inner0, 1, za, inner1, 4, zb, "minecraft:air")
            b.fill(counter, 1, za, counter, 3, zb, "minecraft:air")
            b.fill(counter, 4, za, counter, 4, zb, f"minecraft:{color}_wool")
            b.set(counter, 1, za, "minecraft:smooth_stone_slab", {"type": "top", "waterlogged": "false"})
            if k % 2 == 0:
                b.chest(inner0 + (0 if side == 0 else 2), 1, zb, "kn8:chests/city_shop", face)
            else:
                b.set(inner0 + 1, 1, zb, "minecraft:barrel", {"facing": "up", "open": "false"})
            b.set(inner0 + 1, 3, za, "minecraft:lantern", LANTERN)
    b.finish("shinagawa/lot_arcade")


def shinagawa_ant_ruins():
    """Restos da horda de kaiju-formiga: montes do formigueiro com tuneis, carro esmagado e entulho."""
    b = lot(15, 12, 10, 324)
    rnd = b.random
    for x in range(15):
        for z in range(1, 12):
            b.set(x, 0, z, "minecraft:coarse_dirt" if rnd.random() < 0.6 else "minecraft:rooted_dirt")
    for cx, cz, r, h in ((4, 6, 3, 5), (11, 8, 2, 4)):
        for x in range(cx - r, cx + r + 1):
            for z in range(cz - r, cz + r + 1):
                d = abs(x - cx) + abs(z - cz)
                height = max(0, h - d)
                for y in range(1, height + 1):
                    b.set(x, y, z, "minecraft:packed_mud" if (x + y + z) % 3 else "minecraft:tuff")
        b.fill(cx, 1, cz, cx, h, cz, "minecraft:air")
        b.set(cx, 1, cz - r, "minecraft:air")
        b.set(cx, 2, cz - r + 1, "minecraft:air")
    b.fill(8, 1, 2, 12, 1, 4, "minecraft:black_concrete")
    b.fill(8, 2, 2, 12, 2, 4, "minecraft:red_concrete")
    b.set(9, 2, 3, "minecraft:air")
    debris = ["minecraft:cobblestone", "minecraft:gravel", "minecraft:cracked_stone_bricks", "minecraft:bone_block"]
    for x in range(15):
        for z in range(1, 12):
            if b.get(x, 1, z) is None and rnd.random() < 0.18:
                block = rnd.choice(debris)
                b.set(x, 1, z, block, {"axis": "y"} if block == "minecraft:bone_block" else None)
    b.set(2, 1, 10, "minecraft:cobweb")
    b.chest(13, 1, 10, "kn8:chests/kaiju_remains", "west")
    b.finish("shinagawa/lot_ant_ruins")


def tachikawa_station():
    b = Piece((41, 30, 41), 331, floor="minecraft:polished_andesite")
    for x in range(41):
        for z in range(41):
            if (x * 3 + z) % 7 == 0:
                b.set(x, 0, z, "minecraft:smooth_stone")
    for i in range(41):
        for w in range(16, 25):
            b.set(i, 0, w, ASPHALT)
            b.set(w, 0, i, ASPHALT)
    # Monotrilho Tama (norte-sul): viga no y 12 sobre pilares em T no canteiro do meio da rua.
    beam = 12
    b.fill(19, beam, 0, 21, beam, 40, "minecraft:light_gray_concrete")
    for z in (2, 10, 30, 38):
        b.fill(20, 1, z, 20, beam - 1, z, "minecraft:light_gray_concrete")
        b.fill(18, beam - 1, z, 22, beam - 1, z, "minecraft:light_gray_concrete")
        b.set(20, 0, z, "minecraft:light_gray_concrete")
    # Estacao do monotrilho: plataformas dos dois lados da viga e cobertura.
    b.fill(14, beam, 13, 18, beam, 27, "minecraft:smooth_stone")
    b.fill(22, beam, 13, 26, beam, 27, "minecraft:smooth_stone")
    for z in range(13, 28):
        b.set(14, beam + 1, z, "minecraft:iron_bars")
        b.set(26, beam + 1, z, "minecraft:iron_bars")
    for x, z in ((14, 13), (26, 13), (14, 27), (26, 27), (14, 20), (26, 20)):
        b.fill(x, beam + 1, z, x, beam + 4, z, "minecraft:white_concrete")
        if z != 20:
            b.fill(x, 1, z, x, beam - 1, z, "minecraft:white_concrete")
    b.fill(14, 1, 12, 14, beam, 12, "minecraft:ladder", {"facing": "north", "waterlogged": "false"})
    b.set(14, beam + 1, 13, "minecraft:air")
    b.fill(14, beam + 5, 13, 26, beam + 5, 27, "minecraft:white_concrete")
    for z in range(15, 26, 5):
        b.set(17, beam + 4, z, "minecraft:lantern", LANTERN)
        b.set(23, beam + 4, z, "minecraft:lantern", LANTERN)
    # Trem do monotrilho (branco com faixa laranja) montado na viga.
    b.fill(19, beam + 1, 14, 21, beam + 3, 26, "minecraft:white_concrete")
    b.fill(19, beam + 2, 14, 21, beam + 2, 26, "minecraft:light_blue_stained_glass")
    b.fill(19, beam + 1, 14, 21, beam + 1, 26, "minecraft:orange_concrete")
    # Estacao da JR: os dois lados (oeste e leste da rua) ligados pela passarela no y 6.
    for xa, xb in ((2, 14), (26, 38)):
        b.building(xa, 29, xb, 38, 7, wall="minecraft:smooth_sandstone", stripe="minecraft:green_concrete",
                   door=("south", (xa + xb) // 2 - 1, (xa + xb) // 2 + 1), inner_floor="minecraft:polished_diorite")
        for x in range(xa + 2, xb - 1, 2):
            b.set(x, 1, 33, "minecraft:iron_bars")
        b.chest(xa + 1, 1, 37, "kn8:chests/city_shop", "east")
    # Passarela no y 4 (anda no 5) passando pelas paredes dos dois predios, com escada de mao dentro de cada um.
    b.fill(15, 4, 32, 25, 4, 34, "minecraft:smooth_stone")
    for x in range(15, 26):
        b.set(x, 5, 32, "minecraft:glass_pane")
        b.set(x, 5, 34, "minecraft:glass_pane")
    for x, ladder_x, facing in ((14, 13, "west"), (26, 27, "east")):
        b.fill(x, 5, 33, x, 6, 33, "minecraft:air")
        b.fill(ladder_x, 1, 33, ladder_x, 4, 33, "minecraft:ladder", {"facing": facing, "waterlogged": "false"})
    # Praca com fonte (noroeste) e jardim com a arte publica (nordeste).
    b.fill(4, 0, 4, 12, 1, 12, "minecraft:stone_bricks")
    b.fill(5, 1, 5, 11, 1, 11, "minecraft:water", {"level": "0"})
    b.fill(8, 1, 8, 8, 3, 8, "minecraft:quartz_pillar", {"axis": "y"})
    b.set(8, 4, 8, "minecraft:water", {"level": "0"})
    for x, z in ((29, 4), (36, 4), (29, 11), (36, 11)):
        tree(b, x, z, 4)
    b.fill(32, 1, 7, 33, 4, 8, "minecraft:orange_concrete")
    b.fill(32, 5, 7, 33, 5, 8, "minecraft:yellow_concrete")
    for x, z in ((1, 1), (39, 1), (1, 39), (39, 39)):
        street_lamp(b, x, z)
    station_jigsaws(b, "tachikawa")
    b.finish("tachikawa/station")


def tachikawa_department():
    """Loja de departamentos junto a estacao (GRANDUO/Isetan): 8 andares, placas verticais, vitrines."""
    b = lot(15, 14, 40, 332)
    top = floors_building(b, 0, 1, 14, 13, 8, "minecraft:white_terracotta", "minecraft:light_gray_stained_glass",
                          accent="minecraft:red_terracotta")
    b.fill(1, 1, 1, 13, 3, 1, "minecraft:glass")
    b.fill(6, 1, 1, 8, 2, 1, "minecraft:air")
    for x in (0, 14):
        b.fill(x, 6, 0, x, top - 2, 0, "minecraft:red_concrete")
        for y in range(7, top - 2, 3):
            b.set(x, y, 0, "minecraft:white_concrete")
    b.fill(3, top + 1, 1, 11, top + 3, 1, "minecraft:white_concrete")
    b.fill(4, top + 2, 1, 10, top + 2, 1, "minecraft:red_concrete")
    for z in (4, 8):
        for x in (3, 7, 11):
            b.fill(x, 1, z, x + 1, 1, z, "minecraft:smooth_quartz_slab", {"type": "top", "waterlogged": "false"})
    b.chest(2, 1, 11, "kn8:chests/city_shop", "east")
    b.chest(12, 5, 11, "kn8:chests/city_shop", "west")
    b.finish("tachikawa/lot_department")


def tachikawa_showa_park():
    """Parque Showa Kinen: campo de girassois e flores amarelas, lago com ponte, alameda de arvores."""
    b = lot(15, 15, 10, 333, floor="minecraft:grass_block")
    for z in range(15):
        b.set(7, 0, z, "minecraft:dirt_path")
    for x in range(1, 7):
        for z in range(1, 8):
            if (x + z) % 2:
                b.set(x, 1, z, "minecraft:sunflower", {"half": "lower"})
                b.set(x, 2, z, "minecraft:sunflower", {"half": "upper"})
            else:
                b.set(x, 1, z, "minecraft:dandelion")
    b.fill(9, 0, 2, 13, 0, 7, "minecraft:water", {"level": "0"})
    b.fill(9, 1, 4, 13, 1, 4, "minecraft:spruce_slab", {"type": "bottom", "waterlogged": "false"})
    b.set(11, 1, 5, "minecraft:lily_pad")
    for z in (9, 12):
        tree(b, 3, z, 5)
        tree(b, 11, z, 5)
    for z in (10, 13):
        b.set(6, 1, z, "minecraft:spruce_stairs", stairs("east"))
        b.set(8, 1, z, "minecraft:spruce_stairs", stairs("west"))
    street_lamp(b, 8, 1)
    b.finish("tachikawa/lot_showa_park")


def tachikawa_stage_garden():
    """Sala de shows (Tachikawa Stage Garden): fachada de vidro, palco e plateia em degraus."""
    b = lot(15, 14, 14, 334)
    b.fill(0, 1, 2, 14, 10, 13, "minecraft:white_concrete")
    b.fill(1, 1, 3, 13, 9, 12, "minecraft:air")
    b.fill(1, 1, 2, 13, 6, 2, GLASS)
    b.fill(6, 1, 2, 8, 3, 2, "minecraft:air")
    for x in range(0, 15):
        b.set(x, 10, 2, "minecraft:cyan_concrete")
    b.fill(0, 11, 2, 14, 11, 13, "minecraft:light_gray_concrete")
    b.fill(2, 12, 4, 12, 12, 11, "minecraft:light_gray_concrete")
    b.fill(1, 1, 10, 13, 2, 12, "minecraft:dark_oak_planks")
    for step in range(3):
        z = 5 + step
        b.fill(1, 1, z, 13, 1, z, "minecraft:red_carpet")
    for x in (3, 7, 11):
        b.set(x, 9, 9, "minecraft:lantern", LANTERN)
        b.set(x, 3, 12, "minecraft:note_block", {"instrument": "harp", "note": "0", "powered": "false"})
    b.finish("tachikawa/lot_stage_garden")


# --- worldgen e loot ----------------------------------------------------------------------------------------------

# Biomas planos (as checagens de agua e desnivel ficam no kn8:checked_jigsaw).
BASE_BIOMES = ["minecraft:plains", "minecraft:sunflower_plains", "minecraft:savanna", "minecraft:meadow",
               "minecraft:snowy_plains"]
CITY_BIOMES = BASE_BIOMES + ["minecraft:forest", "minecraft:birch_forest"]
SHINAGAWA_LOTS = [("shinagawa/lot_tower", 4), ("shinagawa/lot_arcade", 3), ("shinagawa/lot_ant_ruins", 2),
                  ("city/lot_office", 3), ("city/lot_apartment_tall", 2), ("city/lot_apartment", 2),
                  ("city/lot_konbini", 2), ("city/lot_ramen", 2), ("city/lot_parking", 1)]
TACHIKAWA_LOTS = [("tachikawa/lot_department", 3), ("tachikawa/lot_showa_park", 2),
                  ("tachikawa/lot_stage_garden", 1), ("city/lot_apartment", 4), ("city/lot_apartment_tall", 2),
                  ("city/lot_house", 3), ("city/lot_konbini", 2), ("city/lot_ramen", 2), ("city/lot_park", 1),
                  ("city/lot_parking", 1)]
# (estrutura, pool inicial, size, distancia max., biomas, raio da checagem, desnivel max.)
SITES = [
    ("ariake_base", "kn8:ariake_base/core", 3, 100, BASE_BIOMES, 104, 14),
    ("tachikawa_base", "kn8:tachikawa_base/core", 3, 110, BASE_BIOMES, 112, 14),
    ("shinagawa_city", "kn8:shinagawa/station", 6, 116, CITY_BIOMES, 96, 16),
    ("tachikawa_city", "kn8:tachikawa/station", 6, 116, CITY_BIOMES, 96, 16),
]
# Fracao das amostras que pode ter agua (riacho ou lagoa pequena, que a construcao cobre; lago e rio largo nao) e
# raio do miolo em que o desnivel conta (a borda irregular fica com o beard_thin).
MAX_WATER = 0.05
SLOPE_RADIUS = 64
# Conjuntos que os locais evitam (chunks de raio): vilas e as construcoes de superficie do vanilla.
AVOID = [("minecraft:villages", 14), ("minecraft:pillager_outposts", 8), ("minecraft:desert_pyramids", 6),
         ("minecraft:igloos", 6), ("minecraft:swamp_huts", 6), ("minecraft:woodland_mansions", 10),
         ("minecraft:jungle_temples", 6)]


def worldgen():
    wg = DATA / "kn8/worldgen"
    dump(wg / "template_pool/ariake_base/harbor.json", {"fallback": "minecraft:empty", "elements": [
        {"weight": 1, "element": element("kn8:ariake_base/harbor")}]})
    dump(wg / "template_pool/tachikawa_base/airfield.json", {"fallback": "minecraft:empty", "elements": [
        {"weight": 1, "element": element("kn8:tachikawa_base/airfield")}]})
    for name, start in (("ariake_base", "core"), ("tachikawa_base", "core"), ("shinagawa", "station"),
                        ("tachikawa", "station")):
        dump(wg / f"template_pool/{name}/{start}.json", {"fallback": "minecraft:empty", "elements": [
            {"weight": 1, "element": element(f"kn8:{name}/{start}")}]})
    street_pools("shinagawa", SHINAGAWA_LOTS, street_weight=5, crossing_weight=3)
    street_pools("tachikawa", TACHIKAWA_LOTS)
    for name, start, size, distance, biomes, radius, slope in SITES:
        dump(DATA / f"kn8/tags/worldgen/biome/has_structure/{name}.json", {"values": biomes})
        settings = {"biomes": f"#kn8:has_structure/{name}", "step": "surface_structures", "spawn_overrides": {},
                    "terrain_adaptation": "beard_thin"}
        # Desenho (sem conferir o terreno): usado pelo local abaixo e pelo /place para testar.
        dump(wg / f"structure/{name}_layout.json", dict(settings, **{
            "type": "minecraft:jigsaw", "start_pool": start, "size": size, "start_height": {"absolute": -1},
            "project_start_to_heightmap": "WORLD_SURFACE_WG", "max_distance_from_center": distance,
            "use_expansion_hack": False}))
        dump(wg / f"structure/{name}.json", dict(settings, **{
            "type": "kn8:checked_jigsaw", "layout": f"kn8:{name}_layout", "check_radius": radius,
            "check_step": 12, "max_slope": slope, "slope_radius": SLOPE_RADIUS,
            "max_water": MAX_WATER,
            "avoid": [{"structure_set": s, "chunks": c} for s, c in AVOID]}))
    # Tag para o /locate achar qualquer um dos quatro (/locate structure #kn8:defense_sites).
    dump(DATA / "kn8/tags/worldgen/structure/defense_sites.json", {"values": [f"kn8:{name}" for name, *_ in SITES]})
    # Um conjunto para os quatro: os locais nunca nascem colados um no outro. ~1 candidato a cada 770 blocos
    # [SUPOSICAO].
    # Bases com peso 2: os biomas delas sao menos e o terreno delas e maior (sem isso nasciam a ~20.000 blocos).
    dump(wg / "structure_set/defense_sites.json", {
        "structures": [{"structure": f"kn8:{name}", "weight": 2 if name.endswith("_base") else 1}
                       for name, *_ in SITES],
        "placement": {"type": "minecraft:random_spread", "spacing": 48, "separation": 16, "salt": 280_430_031}})
    loot = {
        "ariake_lab": [(3, 6), [item("kn8:core_fragment", 6, 1, 2), item("kn8:kaiju_tissue", 10, 2, 5),
                                item("kn8:muscle_fiber", 8, 1, 4), item("kn8:release_catalyst", 2),
                                item("kn8:suit_coolant", 6, 1, 3), item("minecraft:redstone", 6, 2, 8),
                                item("kn8:intact_core", 1)]],
        "ariake_harbor": [(2, 5), [item("minecraft:iron_ingot", 8, 2, 5), item("minecraft:cod", 6, 2, 6),
                                   item("minecraft:string", 6, 2, 6),
                                   item("kn8:rifle_magazine", 4, 1, 2), item("minecraft:bread", 8, 2, 4),
                                   item("kn8:stamina_stim", 4, 1, 2), item("minecraft:compass", 2)]],
    }
    for name, (rolls, entries) in loot.items():
        dump(DATA / f"kn8/loot_table/chests/{name}.json", {"type": "minecraft:chest", "pools": [{
            "rolls": {"type": "minecraft:uniform", "min": rolls[0], "max": rolls[1]}, "entries": entries}]})


if __name__ == "__main__":
    for folder in ("ariake_base", "tachikawa_base", "shinagawa", "tachikawa"):
        (DATA / f"kn8/structure/{folder}").mkdir(parents=True, exist_ok=True)
    ariake_core()
    ariake_harbor()
    tachikawa_core()
    tachikawa_airfield()
    shinagawa_station()
    shinagawa_tower()
    shinagawa_arcade()
    shinagawa_ant_ruins()
    tachikawa_station()
    tachikawa_department()
    tachikawa_showa_park()
    tachikawa_stage_garden()
    for prefix in ("shinagawa", "tachikawa"):
        street_piece(prefix)
        crossing_piece(prefix)
        street_end_piece(prefix)
    worldgen()
