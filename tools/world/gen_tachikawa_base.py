#!/usr/bin/env python3
"""Base de Tachikawa grande, no meio da cidade de Tachikawa (BASES-1; BASES-4, Miguel 2026-10-30: "refaca as 2 bases
elas estao muito pequenas ... fazer a base separada por inteiro (a base tem que ser grande) e depois coloque a base
dentro de uma area que ficaria bom"), pelas referencias de bases e cidades do Miguel (docs/referencias_bases/).

A base e uma estrutura propria (kn8:tachikawa_base, ~210 x 180): QG no meio, campo de treino a oeste, aerodromo a
leste, alas duplas e cantos com torre ao sul e a avenida de entrada ao norte; os bairros da cidade nascem em volta
dela (gen_sites.py).

  tachikawa_base/hq        peca inicial, 81 x 72:
                           estacionamento com carros na frente (painel do manga), QG largo de 7 andares com o volume
                           vertical da entrada e caixas com antenas nas pontas do telhado, torre de comunicacao com
                           radomes e mastro a oeste; dentro: recepcao com os oficiais, refeitorio, enfermaria,
                           briefing, arsenal, sala de monitoramento (parede de telas e consoles em dois niveis), dojo,
                           vestiario com as baias dos trajes, laboratorio, alojamentos, sala de descanso, salas da
                           capita e do vice-capitao, sala de reuniao, heliporto; atras, patio de formatura, area de
                           veiculos e o hangar de teste do traje (estrutura de aco, plataforma, tela de diagnostico)
  tachikawa_base/front     avenida de entrada ao norte (81 x 24): portal com o "3", guaritas, bandeiras, mais vagas
  tachikawa_base/training  campo de treino urbano a oeste (romance oficial da 3a Divisao: simula o bairro de uma
                           estacao, muros altos e grossos, sala de controle que ve a area e abre/fecha as passagens):
                           muro de 3 de espessura, sala de controle no alto com o painel da arena (solta kaiju de
                           treino), alavancas que abrem as duas comportas pelo redstone dentro do muro, estacao,
                           predios brancos de faixas vermelhas (ep. 3), ruas, carros e entulho

O que a obra nao mostra (planta, salas, posicao de cada coisa) e adaptacao [SUPOSICAO]. Gerado pelo gen_sites.py.
"""
from gen_base import Piece, ROAD, stairs
from gen_city import ASPHALT, tree, street_lamp
from gen_ruins import drop_officers, emergency_lights, field_post, ruin
from gen_structures import Float, officer, soldier

LANTERN = {"hanging": "true", "waterlogged": "false"}
LADDER_N = {"facing": "north", "waterlogged": "false"}
SLAB_TOP = {"type": "top", "waterlogged": "false"}
CAR_COLORS = ["white", "light_gray", "black", "gray", "blue", "red", "white", "light_gray"]
ORANGE = "minecraft:orange_concrete"
PRIORITY = 10


def car(b, x, z, color, along_z=True):
    """Carro 2 x 4 (como os do lote de estacionamento da cidade comum)."""
    w, d = (2, 4) if along_z else (4, 2)
    b.fill(x, 1, z, x + w - 1, 1, z + d - 1, "minecraft:black_concrete")
    b.fill(x, 2, z, x + w - 1, 2, z + d - 1, f"minecraft:{color}_concrete")
    if along_z:
        b.fill(x, 3, z + 1, x + 1, 3, z + 2, f"minecraft:{color}_concrete")
        b.fill(x, 2, z, x + 1, 2, z, "minecraft:glass")
    else:
        b.fill(x + 1, 3, z, x + 2, 3, z + 1, f"minecraft:{color}_concrete")
        b.fill(x, 2, z, x, 2, z + 1, "minecraft:glass")


def screens(b, x0, y0, z0, x1, y1, z1):
    """Parede de telas: moldura preta, telas azuis opacas (concreto: vidro mostraria a janela de tras), muitas
    acesas (sea lantern) porque a sala de monitoramento e escura."""
    for x in range(min(x0, x1), max(x0, x1) + 1):
        for y in range(y0, y1 + 1):
            for z in range(min(z0, z1), max(z0, z1) + 1):
                k = x + z
                if y in (y0, y1) or k % 4 == 0:
                    block = "minecraft:black_concrete"
                elif (k + y) % 3 == 0:
                    block = "minecraft:sea_lantern"
                elif (k + y) % 2 == 0:
                    block = "minecraft:cyan_concrete"
                else:
                    block = "minecraft:light_blue_concrete"
                b.set(x, y, z, block)


def sphere(b, cx, cy, cz, r, block):
    for x in range(cx - r, cx + r + 1):
        for y in range(cy - r, cy + r + 1):
            for z in range(cz - r, cz + r + 1):
                if (x - cx) ** 2 + (y - cy) ** 2 + (z - cz) ** 2 <= r * r + 0.5:
                    b.set(x, y, z, block)


def armor_stand(item, yaw=0.0):
    """Baia de traje: suporte com o traje de treino vestido (o Mk1 e o Mk1 Reforcado tem a armadura vanilla
    transparente: o modelo 3D deles so aparece no jogador)."""
    return {"id": "minecraft:armor_stand", "ShowArms": True, "Invulnerable": True, "PersistenceRequired": True,
            "ArmorItems": [{}, {}, {"id": item, "count": 1}, {}], "Rotation": [Float(yaw), Float(0.0)]}


def dummy(yaw=0.0):
    return {"id": "kn8:training_dummy", "Rotation": [Float(yaw), Float(0.0)]}


def table_row(b, x0, x1, z, y=1):
    b.fill(x0, y, z, x1, y, z, "minecraft:smooth_quartz_slab", SLAB_TOP)
    for x in range(x0, x1 + 1, 2):
        b.set(x, y, z - 1, "minecraft:spruce_stairs", stairs("south"))
        b.set(x, y, z + 1, "minecraft:spruce_stairs", stairs("north"))


# --- QG, estacionamento, torre e hangar ---------------------------------------------------------------------------

W, D, H = 81, 72, 58
HQ = (8, 21, 72, 37)  # x0, z0, x1, z1
FLOOR_H = 5
FLOORS = 7


def base_core(ruined=False):
    """ruined (BASES-3): variante depois do raide do No. 10 (obra: ~75% da base destruida)."""
    b = Piece((W, H, D), 351, floor="minecraft:polished_andesite")
    x0, z0, x1, z1 = HQ
    roof = FLOORS * FLOOR_H
    # Entrada pela avenida (peca da frente): rua ate o portao e o acesso ao QG.
    for z in range(0, 21):
        for x in range(37, 44):
            b.set(x, 0, z, ASPHALT)
    for z in range(0, 21, 3):
        b.set(40, 0, z, "minecraft:white_concrete")
    b.jigsaw(40, 0, "north", "kn8:base_core_l", "kn8:base_wing_l", "kn8:tachikawa_base/front", ASPHALT)
    # Muro com os portoes: frente, oeste (campo de treino), leste (aerodromo) e fundos (alas).
    b.wall_line(0, 3, W - 1, 3, gate=(34, 46))
    b.wall_line(0, 3, 0, D - 1, gate=(44, 52))
    b.wall_line(W - 1, 3, W - 1, D - 1, gate=(32, 38))
    b.wall_line(0, D - 1, W - 1, D - 1, gate=(36, 44))
    for z in range(32, 39):
        for x in range(73, W):
            b.set(x, 0, z, ROAD)
    for x in (32, 48):  # guaritas
        b.fill(x - 1, 1, 4, x + 1, 3, 6, "minecraft:white_concrete")
        b.fill(x, 2, 4, x, 2, 6, "minecraft:glass_pane")
        b.fill(x, 1, 6, x, 2, 6, "minecraft:air")
        b.fill(x - 1, 4, 4, x + 1, 4, 6, ORANGE)
    b.entity(35, 1, 5, soldier("rifle", "normal", 180.0))
    b.entity(45, 1, 5, soldier("rifle", "normal", 180.0))
    # Estacionamento diante da fachada (painel do manga: fileiras de carros e o acesso pavimentado).
    for x in range(1, W - 1):
        for z in range(4, 20):
            if not 37 <= x <= 43:
                b.set(x, 0, z, ASPHALT)
    for z in (7, 13):
        for x in list(range(2, 34, 3)) + list(range(47, 79, 3)):
            b.fill(x, 0, z, x, 0, z + 5, "minecraft:white_concrete")
    k = 0
    for z in (8, 14):
        for x in list(range(3, 33, 3)) + list(range(48, 78, 3)):
            if (x * 7 + z) % 5:  # algumas vagas vazias
                car(b, x, z, CAR_COLORS[k % len(CAR_COLORS)])
                k += 1
    # Arvores e cerca-viva baixa dos dois lados do QG (como no painel).
    for x in list(range(9, 34)) + list(range(47, 72)):
        b.set(x, 1, 19, "minecraft:azalea_leaves", {"distance": "1", "persistent": "true", "waterlogged": "false"})
    for x in (10, 16, 22, 28, 52, 58, 64, 70):
        tree(b, x, 18, 5)

    # QG: bloco largo de 7 andares, janelas repetidas com montantes, lajes marcadas na fachada.
    b.fill(x0, 1, z0, x1, roof, z1, "minecraft:white_concrete")
    b.fill(x0 + 1, 1, z0 + 1, x1 - 1, roof - 1, z1 - 1, "minecraft:air")
    for f in range(FLOORS):
        y = f * FLOOR_H
        b.fill(x0 + 1, y, z0 + 1, x1 - 1, y, z1 - 1,
               "minecraft:smooth_stone" if f == 0 else "minecraft:polished_deepslate")
        for x in range(x0 + 1, x1):
            if (x - x0) % 3:
                for z in (z0, z1):
                    b.fill(x, y + 2, z, x, y + 3, z, "minecraft:light_gray_stained_glass_pane")
        for z in range(z0 + 1, z1):
            if (z - z0) % 3:
                for x in (x0, x1):
                    b.fill(x, y + 2, z, x, y + 3, z, "minecraft:light_gray_stained_glass_pane")
        if f:
            for x in range(x0, x1 + 1):
                b.set(x, y, z0, "minecraft:light_gray_concrete")
                b.set(x, y, z1, "minecraft:light_gray_concrete")
        # Paredes das alas (x 34 e 46) com a porta para o hall do meio.
        for x in (34, 46):
            b.fill(x, y + 1, z0 + 1, x, y + 4, z1 - 1, "minecraft:light_gray_concrete")
            b.fill(x, y + 1, 28, x, y + 2, 30, "minecraft:air")
    b.fill(x0, roof, z0, x1, roof, z1, "minecraft:gray_concrete")
    for x in range(x0, x1 + 1):
        b.set(x, roof + 1, z0, "minecraft:light_gray_concrete")
        b.set(x, roof + 1, z1, "minecraft:light_gray_concrete")
    for z in range(z0, z1 + 1):
        b.set(x0, roof + 1, z, "minecraft:light_gray_concrete")
        b.set(x1, roof + 1, z, "minecraft:light_gray_concrete")
    # Volume vertical da entrada (avanca 2 na fachada e sobe acima do telhado), faixa de janelas estreitas.
    b.fill(34, 1, 19, 46, roof + 3, 20, "minecraft:white_concrete")
    b.fill(34, roof, 21, 46, roof + 3, 37, "minecraft:white_concrete")
    b.fill(35, roof + 1, 22, 45, roof + 2, 36, "minecraft:air")
    for x in range(37, 44):
        if x % 2:
            b.fill(x, 6, 19, x, roof + 1, 19, "minecraft:light_blue_stained_glass")
    b.fill(34, roof + 3, 19, 46, roof + 3, 37, "minecraft:light_gray_concrete")
    b.fill(37, 1, 19, 43, 3, 21, "minecraft:air")  # porta de vidro
    for x in (37, 43):
        b.fill(x, 1, 19, x, 3, 19, "minecraft:glass")
    b.fill(35, 4, 15, 45, 4, 18, "minecraft:light_gray_concrete")  # marquise
    for x in (35, 45):
        b.fill(x, 1, 15, x, 3, 15, "minecraft:white_concrete")
    b.fill(38, roof + 1, 19, 42, roof + 1, 19, ORANGE)
    # Caixas nas pontas do telhado com antenas (painel).
    for ax0 in (x0 + 1, x1 - 6):
        b.fill(ax0, roof + 1, z0 + 2, ax0 + 5, roof + 3, z0 + 8, "minecraft:white_concrete")
        b.fill(ax0 + 2, roof + 4, z0 + 5, ax0 + 2, roof + 10, z0 + 5, "minecraft:iron_bars")
        b.fill(ax0 + 1, roof + 8, z0 + 5, ax0 + 3, roof + 8, z0 + 5, "minecraft:iron_bars")
        b.set(ax0 + 2, roof + 11, z0 + 5, "minecraft:lightning_rod",
              {"facing": "up", "powered": "false", "waterlogged": "false"})
    helipad(b, 50, 25, roof)
    b.fill(46, roof + 1, 28, 46, roof + 2, 30, "minecraft:air")  # porta da casa de maquinas para o telhado
    # Escada de mao em todos os andares e no telhado (fundo do hall).
    b.fill(44, 1, z1 - 1, 44, roof, z1 - 1, "minecraft:ladder", LADDER_N)
    hq_interior(b)

    # Torre de comunicacao a oeste (painel): fuste com faixas, mirante de vidro, dois radomes e o mastro.
    tx0, tz0 = 2, 23
    top = 40
    b.fill(tx0, 1, tz0, tx0 + 4, top, tz0 + 4, "minecraft:white_concrete")
    b.fill(tx0 + 1, 1, tz0 + 1, tx0 + 3, top, tz0 + 3, "minecraft:air")
    for x, z in ((tx0, tz0), (tx0 + 4, tz0), (tx0, tz0 + 4), (tx0 + 4, tz0 + 4)):
        b.fill(x, 1, z, x, top, z, "minecraft:light_gray_concrete")
    for y in range(4, top, 4):
        b.set(tx0 + 2, y, tz0, "minecraft:glass_pane")
        b.set(tx0 + 2, y, tz0 + 4, "minecraft:glass_pane")
    b.fill(tx0 + 2, 1, tz0, tx0 + 2, 2, tz0, "minecraft:air")
    b.fill(tx0 + 2, 1, tz0 + 3, tx0 + 2, top + 1, tz0 + 3, "minecraft:ladder", LADDER_N)
    b.fill(tx0 - 1, top + 1, tz0 - 1, tx0 + 5, top + 1, tz0 + 5, "minecraft:smooth_stone")
    b.fill(tx0 - 1, top + 2, tz0 - 1, tx0 + 5, top + 4, tz0 + 5, "minecraft:light_blue_stained_glass")
    b.fill(tx0, top + 2, tz0, tx0 + 4, top + 4, tz0 + 4, "minecraft:air")
    b.set(tx0 + 2, top + 1, tz0 + 3, "minecraft:ladder", LADDER_N)
    b.fill(tx0 - 1, top + 5, tz0 - 1, tx0 + 5, top + 5, tz0 + 5, "minecraft:gray_concrete")
    b.set(tx0 + 1, top + 2, tz0 + 1, "minecraft:observer", {"facing": "south", "powered": "false"})
    b.set(tx0 + 3, top + 2, tz0 + 1, "minecraft:lectern", {"facing": "south", "has_book": "false",
                                                           "powered": "false"})
    sphere(b, tx0, top + 7, tz0 + 2, 1, "minecraft:white_concrete")
    sphere(b, tx0 + 4, top + 7, tz0 + 2, 1, "minecraft:white_concrete")
    b.fill(tx0 + 2, top + 6, tz0 + 2, tx0 + 2, top + 14, tz0 + 2, "minecraft:iron_bars")
    for y in (top + 9, top + 12):
        b.fill(tx0 + 1, y, tz0 + 2, tx0 + 3, y, tz0 + 2, "minecraft:iron_bars")
    b.set(tx0 + 2, top + 15, tz0 + 2, "minecraft:lightning_rod",
          {"facing": "up", "powered": "false", "waterlogged": "false"})
    b.set(tx0 + 2, top + 4, tz0 + 2, "minecraft:lantern", LANTERN)

    # Fundos: patio de formatura, veiculos e o hangar de teste do traje.
    for z in range(z1 + 1, D - 1):
        for x in range(38, 43):
            b.set(x, 0, z, ROAD)
    b.fill(37, 1, z1, 43, 3, z1, "minecraft:air")
    b.fill(8, 0, 40, 34, 0, 56, "minecraft:polished_andesite")
    flagpole(b, 20, 41, "orange", 10)
    flagpole(b, 14, 41, "blue")
    flagpole(b, 26, 41, "white")
    for x in (12, 16, 20, 24, 28):
        b.entity(x, 1, 50, soldier("rifle", "normal", 180.0))
    helipad(b, 10, 59)
    helicopter(b, 13, 60, color="orange")
    for x in (24, 30):  # caminhoes
        b.fill(x, 1, 60, x + 3, 2, 66, "minecraft:green_terracotta")
        b.fill(x, 1, 60, x + 3, 2, 61, "minecraft:gray_concrete")
        b.fill(x, 3, 60, x + 3, 3, 61, "minecraft:light_blue_stained_glass")
    b.fill(2, 0, 44, 8, 0, 52, ROAD)  # saida para o campo de treino
    test_hangar(b)
    for x, z in ((6, 38), (36, 38), (6, 69), (36, 69), (78, 38), (78, 69)):
        b.lamp_post(x, z)
    suffix = "_ruined" if ruined else ""
    b.jigsaw(0, 48, "west", "kn8:tachikawa_base_link", "kn8:tachikawa_base_link",
             f"kn8:tachikawa_base/training{suffix}", ROAD)
    b.jigsaw(W - 1, 35, "east", "kn8:base_core_l", "kn8:base_wing_l", "kn8:tachikawa_base/airfield", ROAD)
    b.jigsaw(40, D - 1, "south", "kn8:base_core_l", "kn8:base_wing_l", "kn8:defense_base/large_wings", ROAD)
    if ruined:
        # Raide do No. 10 com os kaiju voadores: ala oeste do QG desabada, fachada leste aberta, alto da torre
        # arrancado, teto do hangar caido, cratera no estacionamento e o muro leste rompido [SUPOSICAO posicoes].
        ruin(b, [(20, 30, 29, 12), (14, 14, 26, 8), (34, 26, 24, 9), (54, 34, 30, 10), (66, 18, 22, 7),
                 (4, 46, 25, 5), (60, 16, 54, 9), (18, 0, 10, 5), (80, 2, 30, 4)], 371, glass=0.75)
        drop_officers(b)
        field_post(b, 22, 42)
        emergency_lights(b, [(10, 10), (26, 10), (70, 30)])
    b.finish(f"tachikawa_base/hq{suffix}")


def flagpole(b, x, z, color, height=8):
    b.fill(x, 1, z, x, height, z, "minecraft:iron_bars")
    for y in range(height - 2, height + 1):
        b.set(x + 1, y, z, f"minecraft:{color}_wool")
        b.set(x + 2, y, z, f"minecraft:{color}_wool" if y != height - 1 else "minecraft:white_wool")


def helipad(b, x0, z0, y=0, size=9):
    b.fill(x0, y, z0, x0 + size - 1, y, z0 + size - 1, "minecraft:black_concrete")
    for z in range(z0 + 2, z0 + size - 2):
        b.set(x0 + 2, y, z, "minecraft:yellow_concrete")
        b.set(x0 + size - 3, y, z, "minecraft:yellow_concrete")
    for x in range(x0 + 3, x0 + size - 3):
        b.set(x, y, z0 + size // 2, "minecraft:yellow_concrete")


def helicopter(b, x0, z0, y=1, color="blue"):
    b.fill(x0, y, z0, x0 + 2, y + 2, z0 + 4, f"minecraft:{color}_concrete")
    b.fill(x0, y + 1, z0, x0 + 2, y + 2, z0, "minecraft:light_blue_stained_glass")
    b.fill(x0 + 1, y + 1, z0 + 5, x0 + 1, y + 1, z0 + 8, f"minecraft:{color}_concrete")
    b.fill(x0 + 1, y + 3, z0 - 2, x0 + 1, y + 3, z0 + 6, "minecraft:iron_bars")
    b.fill(x0 - 2, y + 3, z0 + 2, x0 + 4, y + 3, z0 + 2, "minecraft:iron_bars")


def hq_interior(b):
    """Salas por andar (alas oeste x 9-33 e leste x 47-71, hall x 35-45)."""
    # Terreo: recepcao (hall), refeitorio (oeste), enfermaria (leste).
    b.fill(36, 1, 26, 44, 1, 26, "minecraft:smooth_stone_slab", SLAB_TOP)
    b.set(40, 1, 26, "minecraft:air")
    b.entity(38, 1, 28, officer("officer", 180.0))
    b.entity(42, 1, 28, officer("exam_officer", 180.0))
    b.set(36, 1, 34, "kn8:squad_station", {"facing": "north"})
    b.set(41, 1, 35, "kn8:research_terminal", {"facing": "north"})
    for z in (24, 29, 33):
        table_row(b, 14, 30, z)
    for z in range(22, 37, 2):
        b.set(9, 1, z, "minecraft:smoker", {"facing": "east", "lit": "false"})
        b.set(9, 1, z + 1, "minecraft:barrel", {"facing": "east", "open": "false"})
    b.set(11, 1, 22, "minecraft:water_cauldron", {"level": "3"})
    b.chest(11, 1, 36, "kn8:chests/base_barracks", "north")
    for x in range(49, 71, 3):
        b.bed(x, 22, "south", "white")
        b.bed(x, 36, "north", "white")
    b.set(70, 1, 29, "minecraft:brewing_stand", {"has_bottle_0": "false", "has_bottle_1": "false",
                                                 "has_bottle_2": "false"})
    b.chest(70, 1, 31, "kn8:chests/base_medical", "west")
    b.fill(48, 1, 29, 52, 1, 29, "minecraft:white_stained_glass")
    # 1o: briefing (oeste: bancos virados para o quadro) e arsenal (leste).
    y = 5
    b.fill(9, y + 1, 23, 9, y + 3, 35, "minecraft:white_concrete")
    b.fill(9, y + 2, 25, 9, y + 2, 33, "minecraft:black_concrete")
    for x in range(13, 32, 3):
        for z in range(23, 36):
            if z != 29:
                b.set(x, y + 1, z, "minecraft:spruce_stairs", stairs("west"))
    b.set(48, y + 1, 23, "kn8:defense_workbench", {"facing": "south"})
    b.set(52, y + 1, 23, "kn8:defense_workbench", {"facing": "south"})
    for x in (56, 60, 64):
        b.chest(x, y + 1, 22, "kn8:chests/base_armory", "south")
    for z in range(25, 36, 2):
        b.set(70, y + 1, z, "minecraft:barrel", {"facing": "west", "open": "false"})
    for x in (50, 54, 58):
        b.entity(x, y + 1, 35, armor_stand("kn8:training_suit", 180.0))
    # 2o: sala de monitoramento (oeste: parede de telas, consoles em dois niveis, escura) e dojo (leste).
    y = 10
    screens(b, 10, y + 1, 23, 10, y + 4, 35)
    b.fill(21, y + 1, 22, 33, y + 1, 36, "minecraft:polished_blackstone")  # nivel de tras
    for x, level in ((14, 1), (17, 1), (24, 2), (28, 2)):
        for z in range(23, 36):
            if z in (29,):
                continue
            b.set(x, y + level, z, "minecraft:polished_blackstone_slab", SLAB_TOP)
            if z % 3 == 0:
                b.set(x, y + level + 1, z, "minecraft:black_stained_glass_pane")
            b.set(x + 1, y + level, z, "minecraft:dark_oak_stairs", stairs("west"))
    b.fill(48, y, 23, 70, y, 35, "minecraft:birch_planks")
    for x in range(48, 71):
        b.set(x, y, 22, "minecraft:dark_oak_planks")
        b.set(x, y, 36, "minecraft:dark_oak_planks")
    b.entity(55, y + 1, 29, dummy(90.0))
    b.entity(63, y + 1, 29, dummy(270.0))
    for z in range(23, 36, 4):  # suportes de armas de treino na parede
        b.set(70, y + 2, z, "minecraft:spruce_trapdoor",
              {"facing": "west", "half": "top", "open": "true", "powered": "false", "waterlogged": "false"})
    # 3o: vestiario com as baias dos trajes (oeste) e laboratorio (leste).
    y = 15
    for z, yaw in ((23, 0.0), (35, 180.0)):
        for x in range(11, 33, 4):
            b.entity(x + 1, y + 1, z, armor_stand("kn8:training_suit", yaw))
            b.fill(x, y + 1, z, x, y + 3, z, "minecraft:iron_bars")
            b.set(x + 1, y + 4, 22 if z == 23 else 36, "minecraft:light_blue_stained_glass")
            b.set(x + 2, y + 1, z, "minecraft:barrel", {"facing": "up", "open": "false"})
    for x in range(12, 32, 4):
        b.set(x, y + 1, 29, "minecraft:spruce_slab", {"type": "bottom", "waterlogged": "false"})
    b.set(50, y + 1, 23, "kn8:research_terminal", {"facing": "south"})
    b.set(56, y + 1, 23, "kn8:research_terminal", {"facing": "south"})
    b.set(62, y + 1, 23, "minecraft:brewing_stand", {"has_bottle_0": "false", "has_bottle_1": "false",
                                                     "has_bottle_2": "false"})
    b.chest(68, y + 1, 23, "kn8:chests/base_medical", "south")
    b.fill(48, y + 1, 30, 70, y + 3, 30, "minecraft:white_stained_glass")
    b.fill(58, y + 1, 30, 60, y + 2, 30, "minecraft:air")
    for x in (52, 60, 66):
        b.set(x, y + 1, 34, "minecraft:smooth_quartz_slab", SLAB_TOP)
    # 4o e 5o: alojamentos e sala de descanso.
    for y, east_rest in ((20, False), (25, True)):
        for z in (23, 35):
            for x in range(10, 33, 3):
                b.bed(x, z, "south" if z == 23 else "north", "orange", y=y + 1)
        if east_rest:
            for x in range(50, 70, 4):
                b.set(x, y + 1, 26, "minecraft:spruce_stairs", stairs("south"))
                b.set(x + 1, y + 1, 26, "minecraft:spruce_stairs", stairs("south"))
            b.fill(70, y + 1, 23, 70, y + 3, 35, "minecraft:bookshelf")
            b.set(60, y + 1, 32, "minecraft:jukebox", {"has_record": "false"})
        else:
            for z in (23, 35):
                for x in range(49, 71, 3):
                    b.bed(x, z, "south" if z == 23 else "north", "orange", y=y + 1)
    # 6o: sala da capita (oeste), reuniao (hall), vice-capitao (leste).
    y = 30
    for x0 in (12, 56):
        b.fill(x0, y + 1, 28, x0 + 4, y + 1, 30, "minecraft:dark_oak_planks")
        b.set(x0 + 2, y + 1, 31, "minecraft:dark_oak_stairs", stairs("north"))
        b.set(x0 + 2, y + 2, 29, "minecraft:lectern", {"facing": "south", "has_book": "false",
                                                       "powered": "false"})
    b.fill(9, y + 1, 23, 9, y + 3, 35, "minecraft:bookshelf")
    b.fill(71, y + 1, 23, 71, y + 3, 35, "minecraft:bookshelf")
    b.chest(10, y + 1, 36, "kn8:chests/base_command", "north")
    b.chest(70, y + 1, 36, "kn8:chests/base_command", "north")
    b.fill(30, y + 2, 23, 31, y + 3, 23, "minecraft:orange_wool")  # bandeira da divisao na parede
    b.fill(36, y + 1, 27, 44, y + 1, 31, "minecraft:polished_blackstone_slab", SLAB_TOP)
    for x in range(36, 45, 2):
        b.set(x, y + 1, 26, "minecraft:dark_oak_stairs", stairs("south"))
        b.set(x, y + 1, 32, "minecraft:dark_oak_stairs", stairs("north"))
    for floor in range(FLOORS):
        for x in (20, 40, 60):
            if not (floor == 2 and x == 20):  # sala de monitoramento fica escura (telas acesas)
                b.set(x, floor * FLOOR_H + 4, 29, "minecraft:lantern", LANTERN)


def test_hangar(b):
    """Hangar de teste do traje (referencia do anime: pe-direito alto, aco aparente, plataforma, cabos, tela)."""
    x0, z0, x1, z1, top = 42, 40, 77, 68, 16
    b.fill(x0, 1, z0, x1, top - 1, z1, "minecraft:light_gray_concrete")
    b.fill(x0 + 1, 1, z0 + 1, x1 - 1, top - 1, z1 - 1, "minecraft:air")
    b.fill(x0, top, z0, x1, top, z1, "minecraft:light_gray_concrete")
    for x in range(x0 + 2, x1, 4):
        b.fill(x, top, z0 + 1, x, top, z1 - 1, "minecraft:glass")
    for x in range(x0, x1 + 1, 5):
        for z in (z0, z1):
            b.fill(x, 1, z, x, top, z, "minecraft:gray_concrete")
        b.fill(x, top - 2, z0 + 1, x, top - 1, z1 - 1, "minecraft:polished_blackstone")  # trelica
    for z in range(z0, z1 + 1, 7):
        for x in (x0, x1):
            b.fill(x, 1, z, x, top, z, "minecraft:gray_concrete")
    for x in range(x0 + 1, x1):
        if (x - x0) % 5:
            for z in (z0, z1):
                b.fill(x, 9, z, x, 11, z, "minecraft:glass")
    b.fill(x0, 1, 50, x0, 8, 58, "minecraft:air")  # portao para o patio
    b.fill(x0 + 1, 0, z0 + 1, x1 - 1, 0, z1 - 1, "minecraft:polished_andesite")
    for x in range(x0 + 2, x1 - 1):
        for z in (z0 + 3, z1 - 3):
            if x % 2:
                b.set(x, 0, z, "minecraft:yellow_concrete")
    # Plataforma de avaliacao com o traje no meio, pesos e cabos de monitoramento.
    b.fill(55, 1, 49, 65, 1, 59, "minecraft:polished_deepslate")
    b.entity(60, 2, 54, armor_stand("kn8:training_suit", 270.0))
    for x, z in ((56, 50), (64, 50), (56, 58), (64, 58)):
        b.set(x, 2, z, "kn8:weight_rack", {"facing": "east"})
    for x, z in ((58, 52), (62, 52), (58, 56), (62, 56), (60, 50), (60, 58)):
        b.fill(x, 9, z, x, top - 3, z, "minecraft:chain", {"axis": "y", "waterlogged": "false"})
    # Tela de diagnostico na parede leste e consoles virados para ela.
    screens(b, x1 - 1, 3, 47, x1 - 1, 11, 61)
    for z in range(48, 61, 3):
        b.set(70, 1, z, "minecraft:polished_blackstone_slab", SLAB_TOP)
        b.set(69, 1, z, "minecraft:dark_oak_stairs", stairs("east"))
        b.set(70, 2, z, "minecraft:black_stained_glass_pane")
    # Passarela no alto (lados norte e sul) com escada de mao.
    for z in (z0 + 1, z1 - 1):
        b.fill(x0 + 1, 7, z, x1 - 1, 7, z, "minecraft:smooth_stone_slab", SLAB_TOP)
        for x in range(x0 + 1, x1):
            b.set(x, 8, z + (1 if z == z0 + 1 else -1), "minecraft:iron_bars")
    b.fill(x0 + 2, 1, z0 + 1, x0 + 2, 7, z0 + 1, "minecraft:ladder", {"facing": "south", "waterlogged": "false"})
    b.set(x0 + 2, 8, z0 + 2, "minecraft:air")
    b.entity(48, 1, 44, dummy(90.0))
    b.entity(48, 1, 64, dummy(90.0))
    b.entity(68, 1, 54, soldier("rifle", "normal", 90.0))
    b.chest(x0 + 1, 1, z1 - 1, "kn8:chests/base_armory", "east")
    for x in range(x0 + 4, x1, 8):
        for z in (z0 + 7, z1 - 7):
            b.set(x, top - 3, z, "minecraft:lantern", LANTERN)


# --- campo de treino urbano ---------------------------------------------------------------------------------------

TW, TD, TH = 65, 56, 30
WALL = (3, 4, 61, 52)  # x0, z0, x1, z1 por fora do muro de 3
WALL_TOP = 9
ROOM = (18, 2, 46, 9)  # sala de controle: x0, z0, x1, z1 (piso no y 10)
WEST_LEVER, EAST_LEVER = 22, 42


def dust(sides):
    props = {"north": "none", "south": "none", "east": "none", "west": "none", "power": "0"}
    props.update(sides)
    return props


def gate_circuit(b, lever_x, step):
    """Comporta de ferro ligada a alavanca da sala de controle: redstone em degraus dentro do muro (z 5), do piso
    da sala (y 10) ate o chao, e a porta de ferro no fim. step = -1 desce para oeste, +1 para leste."""
    z = 5
    floor = WALL_TOP + 1
    x = lever_x + step  # primeiro po, ao lado da alavanca
    b.set(lever_x, floor + 1, z, "minecraft:lever", {"face": "floor", "facing": "north", "powered": "false"})
    up, down = ("east", "west") if step < 0 else ("west", "east")
    for k in range(floor):
        dx = x + step * k
        y = floor - k
        b.set(dx, y - 1, z, "minecraft:stone")
        b.set(dx, y, z, "minecraft:redstone_wire", dust({up: "up" if k else "side", down: "side"}))
        if k > 1:
            b.set(dx, y + 1, z, "minecraft:air")
        elif k == 0:
            b.set(dx + step, y, z, "minecraft:glass")  # o po desce pelo vidro (nao conduz)
    last = x + step * (floor - 1)
    door_x = last + step
    for zz in (z - 1, z + 1):
        b.fill(door_x, 1, zz, door_x, 2, zz, "minecraft:air")
    door = {"facing": "north", "hinge": "left", "open": "false", "powered": "false"}
    b.set(door_x, 1, z, "minecraft:iron_door", dict(door, half="lower"))
    b.set(door_x, 2, z, "minecraft:iron_door", dict(door, half="upper"))
    b.set(door_x, 0, z, "minecraft:stone")
    for zz in (z - 2, z + 2):  # faixa amarela e preta diante da comporta
        b.set(door_x, 0, zz, "minecraft:yellow_concrete")
    return door_x


def apartment(b, x0, z0, x1, z1, floors, damaged=False, seed=0):
    """Predio branco de faixas vermelhas verticais (ep. 3), janelas quebradas; danificado perde um canto."""
    top = floors * 4
    b.fill(x0, 1, z0, x1, top, z1, "minecraft:white_concrete")
    b.fill(x0 + 1, 1, z0 + 1, x1 - 1, top - 1, z1 - 1, "minecraft:air")
    for f in range(floors):
        y = f * 4
        if f:
            b.fill(x0 + 1, y, z0 + 1, x1 - 1, y, z1 - 1, "minecraft:smooth_stone")
        for x in range(x0 + 1, x1):
            for z in (z0, z1):
                if (x - x0) % 3 == 1:
                    b.fill(x, y + 2, z, x, y + 3, z, "minecraft:air" if (x + f + seed) % 4 == 0
                           else "minecraft:light_gray_stained_glass_pane")
        for z in range(z0 + 1, z1):
            for x in (x0, x1):
                if (z - z0) % 3 == 1:
                    b.fill(x, y + 2, z, x, y + 3, z, "minecraft:light_gray_stained_glass_pane")
    for x in (x0 + 2, x1 - 2):
        for z in (z0, z1):
            b.fill(x, 1, z, x, top, z, "minecraft:red_terracotta")
    b.fill(x0, top, z0, x1, top, z1, "minecraft:light_gray_concrete")
    mid = (x0 + x1) // 2
    b.fill(mid, 1, z0, mid, 2, z0, "minecraft:air")
    b.fill(mid, 1, z1, mid, 2, z1, "minecraft:air")
    if damaged:
        for x in range(x0, x0 + 5):
            for z in range(z0, z0 + 4):
                for y in range(top - 5 + (x - x0) + (z - z0), top + 1):
                    b.set(x, y, z, "minecraft:air")
        for x, z in ((x0 - 1, z0 + 1), (x0 + 1, z0 - 1), (x0 + 3, z0 - 1)):
            b.set(x, 1, z, "minecraft:cobblestone")
        b.set(x0 + 1, 1, z0 - 2, "minecraft:white_terracotta")


def base_training(ruined=False):
    b = Piece((TW, TH, TD), 352, floor="minecraft:polished_andesite")
    wx0, wz0, wx1, wz1 = WALL
    b.jigsaw(32, 0, "north", "kn8:tachikawa_base_link", "kn8:tachikawa_base_link", "minecraft:empty", ROAD)
    for z in range(0, wz0):
        for x in range(26, 39):
            b.set(x, 0, z, ROAD)
    # Muro alto e grosso (3 blocos) com a faixa de aviso no alto.
    for t in range(3):
        b.walls(wx0 + t, 1, wz0 + t, wx1 - t, WALL_TOP, wz1 - t, "minecraft:gray_concrete")
    for x in range(wx0, wx1 + 1):
        for z in (wz0, wz1):
            b.set(x, WALL_TOP, z, "minecraft:yellow_concrete" if x % 2 else "minecraft:black_concrete")
    for z in range(wz0, wz1 + 1):
        for x in (wx0, wx1):
            b.set(x, WALL_TOP, z, "minecraft:yellow_concrete" if z % 2 else "minecraft:black_concrete")
    for x in range(wx0, wx1 + 1, 6):
        b.set(x, WALL_TOP + 1, wz1, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
    # Sala de controle em cima do muro da frente, vidro virado para o campo (ve a area toda).
    rx0, rz0, rx1, rz1 = ROOM
    fy = WALL_TOP + 1
    b.fill(rx0, fy, rz0, rx1, fy + 5, rz1, "minecraft:white_concrete")
    b.fill(rx0 + 1, fy + 1, rz0 + 1, rx1 - 1, fy + 4, rz1 - 1, "minecraft:air")
    b.fill(rx0, fy + 1, rz1, rx1, fy + 4, rz1, "minecraft:light_blue_stained_glass")
    b.fill(rx0 + 1, fy + 5, rz0 + 1, rx1 - 1, fy + 5, rz1 - 1, "minecraft:gray_concrete")
    for x in range(rx0 + 2, rx1 - 1, 3):
        b.set(x, fy + 2, rz0, "minecraft:glass_pane")
    for x, z in ((rx0, rz0), (rx1, rz0), (rx0, rz1), (rx1, rz1)):
        b.fill(x, 1, z, x, fy - 1, z, "minecraft:light_gray_concrete")
    screens(b, rx0 + 2, fy + 1, rz0 + 1, rx1 - 2, fy + 3, rz0 + 1)
    for x in range(rx0 + 3, rx1 - 2):
        if x in (WEST_LEVER, WEST_LEVER - 1, EAST_LEVER, EAST_LEVER + 1, 32):
            continue
        b.set(x, fy + 1, rz1 - 2, "minecraft:polished_blackstone_slab", SLAB_TOP)
        if x % 3 == 0:
            b.set(x, fy + 2, rz1 - 2, "minecraft:black_stained_glass_pane")
    b.set(32, fy + 1, rz1 - 2, "kn8:training_console", {"facing": "south", "selection": "1"})
    for x in range(rx0 + 4, rx1 - 2, 4):
        b.set(x, fy + 4, (rz0 + rz1) // 2, "minecraft:lantern", LANTERN)
    # Acesso: poco com escada de mao dentro do muro, entrada pelo lado de fora.
    sx = 34
    b.fill(sx, 1, wz0, sx, 2, wz0, "minecraft:air")
    b.fill(sx, 1, wz0 + 1, sx, fy, wz0 + 1, "minecraft:ladder", LADDER_N)
    # Comportas (oeste e leste) abertas pelas alavancas da sala.
    gate_circuit(b, WEST_LEVER, -1)
    gate_circuit(b, EAST_LEVER, 1)
    b.entity(32, fy + 1, 5, soldier("rifle", "normal", 0.0))
    # Dentro: bairro de estacao simulado (ruas, estacao ao fundo, predios do ep. 3, carros, entulho, pontos cegos).
    ix0, iz0, ix1, iz1 = wx0 + 3, wz0 + 3, wx1 - 3, wz1 - 3
    for x in range(ix0, ix1 + 1):
        for z in range(iz0, iz1 + 1):
            b.set(x, 0, z, "minecraft:gray_concrete")
    for x in range(ix0, ix1 + 1):
        for z in range(22, 27):
            b.set(x, 0, z, ASPHALT)
        if x % 4 == 0:
            b.set(x, 0, 24, "minecraft:white_concrete")
    for z in range(iz0, iz1 + 1):
        for x in range(30, 35):
            b.set(x, 0, z, ASPHALT)
    apartment(b, ix0 + 1, iz0 + 1, ix0 + 11, iz0 + 11, 3, seed=1)
    apartment(b, ix1 - 11, iz0 + 1, ix1 - 1, iz0 + 11, 4, seed=2)
    apartment(b, ix0 + 1, 31, ix0 + 11, 41, 3, damaged=True, seed=3)
    apartment(b, ix1 - 11, 31, ix1 - 1, 41, 2, damaged=True, seed=4)
    # Estacao ao fundo: predio baixo com a faixa verde e a cobertura da plataforma.
    b.fill(22, 1, 43, 42, 6, 48, "minecraft:smooth_sandstone")
    b.fill(23, 1, 44, 41, 5, 47, "minecraft:air")
    for x in range(22, 43):
        b.set(x, 5, 43, "minecraft:green_concrete")
    b.fill(30, 1, 43, 34, 3, 43, "minecraft:air")
    b.fill(22, 7, 42, 42, 7, 49, "minecraft:light_gray_concrete")
    for x in range(24, 41, 4):
        b.set(x, 2, 43, "minecraft:glass_pane")
    # Carros abandonados, barreiras e entulho (pontos cegos).
    for x, z, color, along in ((10, 23, "white", False), (50, 24, "red", False), (37, 14, "gray", True),
                               (26, 34, "blue", True)):
        car(b, x, z, color, along_z=along)
    for x, z in ((20, 18), (44, 18), (20, 38), (44, 38)):
        b.fill(x, 1, z, x + 2, 2, z, "minecraft:light_gray_concrete")
    for x, z in ((24, 28), (40, 21), (17, 27), (48, 29), (36, 40)):
        b.set(x, 1, z, "minecraft:cobblestone")
        b.set(x + 1, 1, z, "minecraft:gravel")
    for x, z in ((ix0 + 13, iz0 + 1), (ix1 - 13, iz0 + 1), (ix0 + 13, iz1 - 8), (ix1 - 13, iz1 - 8)):
        street_lamp(b, x, z)
    if ruined:
        # Muro rompido dos dois lados, predio do treino e estacao atingidos; o muro da frente, a sala de controle,
        # as comportas e o redstone ficam (keep_box).
        ruin(b, [(4, 4, 30, 5), (32, 4, 51, 5), (12, 9, 13, 5), (34, 6, 46, 4), (55, 10, 36, 4)], 372, glass=0.5,
             keep_box=(10, 0, 3, 54, 11, 7))
        emergency_lights(b, [(9, 30), (32, 42)])
    b.finish("tachikawa_base/training" + ("_ruined" if ruined else ""))


def base_front():
    """Avenida de entrada (BASES-4): a borda da base virada para a cidade. Portal com o "3" da divisao, guaritas,
    fileiras de bandeiras, vagas dos dois lados, ponto de onibus e arvores [SUPOSICAO desenho]."""
    b = Piece((81, 20, 24), 353, floor="minecraft:polished_andesite")
    for z in range(24):
        for x in range(34, 47):
            b.set(x, 0, z, ASPHALT)
        if z % 4 < 2:
            b.set(40, 0, z, "minecraft:white_concrete")
    for z in range(24):
        for x in (33, 47):
            b.set(x, 0, z, "minecraft:light_gray_concrete")
    # Vagas dos dois lados da avenida.
    k = 0
    for x0 in (3, 50):
        for z in (4, 13):
            for x in range(x0, x0 + 28, 3):
                b.fill(x, 0, z - 1, x, 0, z + 4, "minecraft:white_concrete")
                if (x + z) % 4:
                    car(b, x + 1, z, CAR_COLORS[k % len(CAR_COLORS)])
                    k += 1
    # Portal com o "3" sobre a avenida e guaritas.
    b.fill(31, 1, 18, 33, 11, 20, "minecraft:white_concrete")
    b.fill(47, 1, 18, 49, 11, 20, "minecraft:white_concrete")
    b.fill(31, 12, 18, 49, 13, 20, ORANGE)
    b.fill(31, 11, 19, 49, 11, 19, "minecraft:white_concrete")
    digit = ["#####", "    #", "    #", " ####", "    #", "    #", "#####"]
    # Placa virada para fora (quem chega da cidade olha para o norte: x cresce para a direita).
    for r, line in enumerate(digit):
        for c, ch in enumerate(line):
            b.set(38 + c, 18 - r, 21, "minecraft:white_concrete" if ch == "#" else "minecraft:orange_concrete")
    for x in (28, 52):
        b.fill(x - 1, 1, 19, x + 1, 3, 21, "minecraft:white_concrete")
        b.fill(x, 2, 19, x, 2, 21, "minecraft:glass_pane")
        b.fill(x - 1, 4, 19, x + 1, 4, 21, ORANGE)
        b.entity(x, 1, 18, soldier("rifle", "normal", 0.0))
    b.fill(34, 1, 22, 46, 1, 22, "minecraft:yellow_concrete")
    b.fill(35, 1, 22, 45, 1, 22, "minecraft:air")
    for x in range(36, 46, 2):
        b.set(x, 1, 21, "minecraft:black_concrete")
    # Bandeiras ao longo da avenida, arvores e ponto de onibus.
    for z in (3, 9, 15):
        flagpole(b, 31, z, "orange", 7)
        flagpole(b, 48, z, "blue", 7)
    for z in (2, 10):
        tree(b, 1, z, 5)
        tree(b, 79, z, 5)
    b.fill(56, 1, 21, 62, 1, 21, "minecraft:smooth_stone_slab", {"type": "bottom", "waterlogged": "false"})
    b.fill(56, 3, 20, 62, 3, 22, "minecraft:light_gray_concrete")
    b.fill(56, 1, 22, 56, 2, 22, "minecraft:iron_bars")
    b.fill(62, 1, 22, 62, 2, 22, "minecraft:iron_bars")
    for x in (2, 78):
        street_lamp(b, x, 21)
    b.jigsaw(40, 0, "north", "kn8:base_wing_l", "kn8:base_core_l", "minecraft:empty", ASPHALT)
    b.finish("tachikawa_base/front")


__all__ = ["base_core", "base_training", "base_front"]
