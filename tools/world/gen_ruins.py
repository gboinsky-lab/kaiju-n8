#!/usr/bin/env python3
"""Variante pos-ataque das bases (BASES-3, referencias de bases do Miguel: "variante danificada do exterior para
missoes pos-ataque: falhas estruturais, janelas quebradas, torres avariadas e zonas interditadas").

ruin() trabalha direto nos blocos de uma peca ja desenhada (coordenadas cruas do template): abre crateras (tudo
dentro do raio some; no chao vira entulho), queima a casca em volta, quebra parte dos vidros, espalha entulho, cerca
as crateras que tocam o chao com a barreira amarela e preta e tira as entidades que estavam onde a cratera abriu.
field_post() monta o posto de campanha (tenda com os oficiais e os servicos) para a base continuar funcionando.
"""
import math
import random

from gen_structures import Build, Float, officer

RUBBLE = ["minecraft:cobblestone", "minecraft:andesite", "minecraft:cobbled_deepslate", "minecraft:gravel",
          "minecraft:cracked_stone_bricks", "minecraft:coarse_dirt"]
FLOOR_RUBBLE = ["minecraft:cobbled_deepslate", "minecraft:coarse_dirt", "minecraft:cobblestone",
                "minecraft:andesite"]
SCORCH = {
    "minecraft:white_concrete": "minecraft:gray_concrete",
    "minecraft:light_gray_concrete": "minecraft:gray_concrete",
    "minecraft:gray_concrete": "minecraft:black_concrete",
    "minecraft:white_terracotta": "minecraft:gray_terracotta",
    "minecraft:smooth_sandstone": "minecraft:brown_terracotta",
    "minecraft:polished_andesite": "minecraft:cobbled_deepslate",
    "minecraft:smooth_stone": "minecraft:cobblestone",
    "minecraft:blue_concrete": "minecraft:black_concrete",
    "minecraft:orange_concrete": "minecraft:brown_concrete",
}
KEEP = ("minecraft:jigsaw", "minecraft:lever", "minecraft:redstone_wire", "minecraft:iron_door",
        "kn8:training_console")


def _dist(a, b):
    return math.sqrt(sum((p - q) ** 2 for p, q in zip(a, b)))


def ruin(b, craters, seed, glass=0.6, ground=0, keep_box=None):
    """craters = [(x, y, z, raio)] em coordenadas cruas; ground = y cru do chao; keep_box = (x0, y0, z0, x1, y1, z1)
    que nao e tocada (circuito das comportas)."""
    rnd = random.Random(seed)

    def kept(x, y, z):
        if keep_box is None:
            return False
        x0, y0, z0, x1, y1, z1 = keep_box
        return x0 <= x <= x1 and y0 <= y <= y1 and z0 <= z <= z1

    w, h, d = b.size
    for cx, cy, cz, r in craters:
        reach = int(r + 2)
        for x in range(max(0, cx - reach), min(w, cx + reach + 1)):
            for z in range(max(0, cz - reach), min(d, cz + reach + 1)):
                for y in range(max(0, cy - reach), min(h, cy + reach + 1)):
                    if kept(x, y, z):
                        continue
                    dist = _dist((x, y, z), (cx, cy, cz))
                    name = Build.get(b, x, y, z)
                    if name in KEEP:
                        continue
                    if dist < r:
                        if y > ground:
                            Build.set(b, x, y, z, "minecraft:air")
                        elif y == ground:
                            Build.set(b, x, y, z, rnd.choice(FLOOR_RUBBLE))
                    elif dist < r + 1.6 and name in SCORCH and rnd.random() < 0.6:
                        Build.set(b, x, y, z, SCORCH[name])
        # Entulho no fundo da cratera e em volta dela.
        for x in range(max(0, cx - reach), min(w, cx + reach + 1)):
            for z in range(max(0, cz - reach), min(d, cz + reach + 1)):
                horiz = math.hypot(x - cx, z - cz)
                if horiz < r + 1 and rnd.random() < 0.3 and not kept(x, ground + 1, z):
                    name = Build.get(b, x, ground + 1, z)
                    if name in (None, "minecraft:air"):
                        block = rnd.choice(RUBBLE)
                        Build.set(b, x, ground + 1, z, "minecraft:cobblestone" if block == "minecraft:gravel"
                                  else block)
        # Zona interditada: barreira amarela e preta em volta das crateras que tocam o chao.
        if cy - r <= ground + 2:
            ring = r + 2
            steps = int(2 * math.pi * ring)
            for k in range(steps):
                a = 2 * math.pi * k / steps
                x, z = int(round(cx + ring * math.cos(a))), int(round(cz + ring * math.sin(a)))
                if not (0 <= x < w and 0 <= z < d) or kept(x, ground + 1, z):
                    continue
                if Build.get(b, x, ground + 1, z) in (None, "minecraft:air"):
                    Build.set(b, x, ground + 1, z, "minecraft:yellow_concrete" if k % 2 else "minecraft:black_concrete")
                    if Build.get(b, x, ground + 2, z) in (None, "minecraft:air"):
                        Build.set(b, x, ground + 2, z, "minecraft:iron_bars")
    # Vidros quebrados.
    for (x, y, z), (name, _props, _nbt) in list(b.blocks.items()):
        if "glass" in name and y > ground and not kept(x, y, z) and rnd.random() < glass:
            Build.set(b, x, y, z, "minecraft:air")
    # Quem estava onde a cratera abriu sai.
    def inside(entity):
        pos = [float(v) for v in entity["pos"]]
        return any(_dist(pos, (cx + 0.5, cy, cz + 0.5)) < r + 1 for cx, cy, cz, r in craters)
    b.entities = [e for e in b.entities if not inside(e)]


def drop_officers(b):
    b.entities = [e for e in b.entities if e["nbt"].get("id") != "kn8:defense_officer"]


def field_post(b, x0, z0, ground=0):
    """Posto de campanha 11 x 7 (tenda branca com a faixa vermelha): oficiais, estacao do esquadrao, terminal de
    pesquisa, bancada, macas e caixa medica. Coordenadas cruas; a frente (aberta) e o lado z0."""
    y = ground
    x1, z1 = x0 + 10, z0 + 6
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            Build.set(b, x, y, z, "minecraft:smooth_stone")
            for yy in range(y + 1, y + 5):
                Build.set(b, x, yy, z, "minecraft:air")
    for x in range(x0, x1 + 1):
        Build.set(b, x, y + 1, z1, "minecraft:white_wool")
        Build.set(b, x, y + 2, z1, "minecraft:white_wool")
        Build.set(b, x, y + 3, z1 - 1, "minecraft:white_wool")
        Build.set(b, x, y + 3, z0 + 1, "minecraft:white_wool")
        Build.set(b, x, y + 4, z0 + 2, "minecraft:red_wool" if x % 4 == 0 else "minecraft:white_wool")
        Build.set(b, x, y + 4, (z0 + z1) // 2, "minecraft:white_wool")
        Build.set(b, x, y + 4, z1 - 2, "minecraft:white_wool")
    for z in range(z0 + 1, z1 + 1):
        for x in (x0, x1):
            Build.set(b, x, y + 1, z, "minecraft:white_wool")
            Build.set(b, x, y + 2, z, "minecraft:white_wool")
    for x in (x0, x1):
        for yy in range(y + 1, y + 4):
            Build.set(b, x, yy, z0, "minecraft:oak_fence", {"east": "false", "west": "false", "north": "false",
                                                            "south": "false", "waterlogged": "false"})
    Build.set(b, x0 + 2, y + 1, z1 - 1, "kn8:squad_station", {"facing": "north"})
    Build.set(b, x0 + 4, y + 1, z1 - 1, "kn8:research_terminal", {"facing": "north"})
    Build.set(b, x0 + 6, y + 1, z1 - 1, "kn8:defense_workbench", {"facing": "north"})
    Build.chest(b, x1 - 1, y + 1, z1 - 1, "kn8:chests/base_medical", "north")
    for x in (x0 + 7, x0 + 9):
        Build.set(b, x, y + 1, z0 + 2, "minecraft:white_bed", {"facing": "south", "occupied": "false",
                                                               "part": "foot"})
        Build.set(b, x, y + 1, z0 + 3, "minecraft:white_bed", {"facing": "south", "occupied": "false",
                                                               "part": "head"})
    Build.set(b, x0 + 3, y + 3, (z0 + z1) // 2, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})
    Build.set(b, x0 + 7, y + 3, (z0 + z1) // 2, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})
    Build.entity(b, x0 + 3, y + 1, z0 + 2, officer("officer", 180.0))
    Build.entity(b, x0 + 5, y + 1, z0 + 2, officer("exam_officer", 180.0))


def emergency_lights(b, spots, ground=0):
    """Postes de luz de emergencia (vermelha) nas zonas interditadas: barra de ferro e lampada acesa por baixo."""
    for x, z in spots:
        Build.set(b, x, ground + 1, z, "minecraft:redstone_block")
        Build.set(b, x, ground + 2, z, "minecraft:redstone_lamp", {"lit": "true"})
        Build.set(b, x, ground + 3, z, "minecraft:red_stained_glass")


__all__ = ["ruin", "drop_officers", "field_post", "emergency_lights", "Float"]
