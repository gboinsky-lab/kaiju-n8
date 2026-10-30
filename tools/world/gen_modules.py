#!/usr/bin/env python3
"""Modulos da base do jogador (PM-8D): um template .nbt por modulo em data/kn8/structure/base_module/, todos do
mesmo tamanho de lote (11 x 9 x 11; y 0 = chao, assentado um bloco abaixo do Nucleo da Base). Cada modulo tem uma
porta no meio de cada lado (o lote pode ficar em qualquer posicao em volta do nucleo, sem girar o template).
O que faz cada modulo funcionar sao os blocos e NPCs que ja existem (bancada, terminal de pesquisa, estacao de
preparacao, oficiais, bonecos de treino).

Uso: python3 tools/world/gen_modules.py
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from gen_base import Piece  # noqa: E402
from gen_structures import Float, officer  # noqa: E402

SIZE = (11, 9, 11)
MID = 5


def doors(b, height=2):
    """Porta de 1 bloco no meio de cada parede do predio (x/z 1 e 9)."""
    for x, z in ((MID, 1), (MID, 9), (1, MID), (9, MID)):
        for y in range(1, height + 1):
            b.set(x, y, z, "minecraft:air")


def module_building(b, stripe):
    b.building(1, 1, 9, 9, 5, stripe=stripe)
    doors(b)
    for x in (3, 7):  # luz no meio
        b.set(x, 4, MID, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})


def workshop():
    b = Piece(SIZE, 401)
    module_building(b, "minecraft:orange_concrete")
    b.set(3, 1, 3, "kn8:defense_workbench", {"facing": "south"})
    b.set(7, 1, 3, "kn8:defense_workbench", {"facing": "south"})
    b.set(3, 1, 7, "minecraft:smithing_table")
    b.set(7, 1, 7, "minecraft:anvil", {"facing": "north"})
    b.set(2, 1, 5, "minecraft:chest", {"facing": "east", "type": "single", "waterlogged": "false"})
    b.set(8, 1, 5, "minecraft:chest", {"facing": "west", "type": "single", "waterlogged": "false"})
    b.finish("base_module/workshop")


def laboratory():
    b = Piece(SIZE, 402)
    module_building(b, "minecraft:purple_concrete")
    b.set(3, 1, 3, "kn8:research_terminal", {"facing": "south"})
    b.set(7, 1, 3, "kn8:research_terminal", {"facing": "south"})
    b.set(3, 1, 7, "minecraft:brewing_stand",
          {"has_bottle_0": "false", "has_bottle_1": "false", "has_bottle_2": "false"})
    b.fill(6, 1, 7, 8, 2, 7, "minecraft:glass")  # vitrine com uma amostra
    b.set(7, 1, 7, "minecraft:magma_block")
    b.set(2, 1, 5, "minecraft:chest", {"facing": "east", "type": "single", "waterlogged": "false"})
    b.finish("base_module/laboratory")


def barracks():
    b = Piece(SIZE, 403)
    module_building(b, "minecraft:green_concrete")
    b.set(MID, 1, 3, "kn8:squad_station", {"facing": "south"})
    b.bed(2, 6, "south", "white")
    b.bed(8, 6, "south", "white")
    b.bed(3, 7, "south", "white")
    b.bed(7, 7, "south", "white")
    b.set(2, 1, 3, "minecraft:chest", {"facing": "east", "type": "single", "waterlogged": "false"})
    b.finish("base_module/barracks")


def command():
    b = Piece(SIZE, 404)
    module_building(b, "minecraft:cyan_concrete")
    b.set(3, 1, 3, "minecraft:cartography_table")
    b.set(4, 1, 3, "minecraft:lectern", {"facing": "south", "has_book": "false", "powered": "false"})
    for x in range(3, 8):
        b.set(x, 1, 6, "minecraft:cyan_carpet")
    b.entity(4, 1, 7, officer("officer", 180.0))
    b.entity(6, 1, 7, officer("exam_officer", 180.0))
    b.finish("base_module/command")


def training():
    b = Piece(SIZE, 405, floor="minecraft:coarse_dirt")
    for x in range(SIZE[0]):  # cerca com abertura no meio de cada lado
        for z in (0, SIZE[2] - 1):
            if x != MID:
                b.set(x, 1, z, "minecraft:spruce_fence")
    for z in range(SIZE[2]):
        for x in (0, SIZE[0] - 1):
            if z != MID:
                b.set(x, 1, z, "minecraft:spruce_fence")
    for x in (3, MID, 7):
        b.entity(x, 1, 3, {"id": "kn8:training_dummy", "Rotation": [Float(0.0), Float(0.0)]})
    b.fill(2, 1, 8, 3, 1, 8, "minecraft:hay_block", {"axis": "y"})
    b.set(7, 1, 8, "minecraft:target")
    # TREINO: suportes de pesos.
    b.set(8, 1, 4, "kn8:weight_rack", {"facing": "west"})
    b.set(8, 1, 5, "kn8:weight_rack", {"facing": "west"})
    b.set(8, 1, 8, "minecraft:target")
    b.fill(4, 0, 6, 6, 0, 8, "minecraft:gravel")
    b.finish("base_module/training")


def main():
    for build in (workshop, laboratory, barracks, command, training):
        build()


if __name__ == "__main__":
    main()
