#!/usr/bin/env python3
"""Construcoes do mundo (0.2): templates .nbt em data/kn8/structure/ + worldgen (structure, template_pool,
structure_set, tag de biomas) e baus com loot table. Geradas por codigo para ficarem reproduziveis e revisaveis.

  defense_outpost   posto avancado da Forca de Defesa (muro, sede com bancada, heliporto, patio de treino com
                    bonecos, holofotes, bandeira, soldados de guarda)
  ruined_building   predio destruido por kaiju (andares quebrados, marcas de garra, entulho)
  kaiju_remains     esqueleto de um kaiju antigo numa cratera (espinha, costelas, cranio, nucleo apagado)
  watchtower        torre de vigia com sirene, holofote e plataforma

Uso: python3 tools/world/gen_structures.py   (sem dependencias). Ver no jogo: /place structure kn8:<nome>
"""
import json
import random
from pathlib import Path

from nbt_io import CompoundList, DoubleList, Float, IntList, write

ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / "src/main/resources/data"
DATA_VERSION_1_21_1 = 3955

# Blocos que ligam nos vizinhos (painel de vidro, grade, cerca): propriedades calculadas no fim.
CONNECTING = ("minecraft:iron_bars", "minecraft:glass_pane", "minecraft:oak_fence", "minecraft:spruce_fence",
              "minecraft:light_gray_stained_glass_pane")


class Build:
    def __init__(self, size, seed):
        self.size = size
        self.blocks = {}
        self.entities = []
        self.random = random.Random(seed)

    def set(self, x, y, z, name, props=None, nbt=None):
        if 0 <= x < self.size[0] and 0 <= y < self.size[1] and 0 <= z < self.size[2]:
            self.blocks[(x, y, z)] = (name, dict(props or {}), nbt)

    def get(self, x, y, z):
        entry = self.blocks.get((x, y, z))
        return entry[0] if entry else None

    def fill(self, x0, y0, z0, x1, y1, z1, name, props=None):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, name, props)

    def walls(self, x0, y0, z0, x1, y1, z1, name):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.set(x, y, z0, name)
                self.set(x, y, z1, name)
            for z in range(z0, z1 + 1):
                self.set(x0, y, z, name)
                self.set(x1, y, z, name)

    def chest(self, x, y, z, loot, facing="north", block="minecraft:chest"):
        props = {"facing": facing}
        if block == "minecraft:chest":
            props.update({"type": "single", "waterlogged": "false"})
        else:
            props.update({"open": "false"})
        self.set(x, y, z, block, props, {"id": block, "LootTable": loot})

    def entity(self, x, y, z, nbt):
        self.entities.append({"pos": DoubleList([x + 0.5, float(y), z + 0.5]),
                              "blockPos": IntList([int(x), int(y), int(z)]), "nbt": nbt})

    def connect(self):
        sides = {"north": (0, -1), "south": (0, 1), "west": (-1, 0), "east": (1, 0)}
        for (x, y, z), (name, props, nbt) in list(self.blocks.items()):
            if name not in CONNECTING:
                continue
            for side, (dx, dz) in sides.items():
                other = self.get(x + dx, y, z + dz)
                solid = other is not None and other != "minecraft:air" and "lantern" not in other \
                    and "torch" not in other and "carpet" not in other
                props[side] = "true" if solid else "false"
            props["waterlogged"] = "false"

    def save(self, name):
        self.connect()
        palette, index, blocks = [], {}, []
        for (x, y, z), (block, props, nbt) in sorted(self.blocks.items()):
            key = (block, tuple(sorted(props.items())))
            if key not in index:
                index[key] = len(palette)
                state = {"Name": block}
                if props:
                    state["Properties"] = dict(sorted(props.items()))
                palette.append(state)
            entry = {"pos": IntList([x, y, z]), "state": index[key]}
            if nbt:
                entry["nbt"] = nbt
            blocks.append(entry)
        root = {"DataVersion": DATA_VERSION_1_21_1, "size": IntList(list(self.size)),
                "palette": CompoundList(palette), "blocks": CompoundList(blocks),
                "entities": CompoundList(self.entities)}
        path = DATA / "kn8/structure" / f"{name}.nbt"
        write(path, root)
        print(f"{name}: {len(blocks)} blocos, {len(self.entities)} entidades, {self.size}")


def soldier(variant, level="normal", yaw=0.0):
    return {"id": "kn8:soldier", "kn8_variant": variant, "kn8_power_level": level, "PersistenceRequired": True,
            "Rotation": [Float(yaw), Float(0.0)]}


# --- posto avancado ---------------------------------------------------------------------------------------------

def defense_outpost():
    b = Build((25, 12, 25), 81)
    # y = 0 e a camada do chao (assentada no terreno: start_height -1 no worldgen).
    for x in range(25):
        for z in range(25):
            b.set(x, 0, z, "minecraft:gray_concrete" if (x + z) % 7 else "minecraft:light_gray_concrete")
            for y in range(1, 8):
                b.set(x, y, z, "minecraft:air")
    # Muro de barreiras (concreto + sacos de terra) com portao ao sul (z = 0).
    for i in range(25):
        for x, z in ((i, 0), (i, 24), (0, i), (24, i)):
            if z == 0 and 10 <= x <= 14:
                continue
            b.set(x, 1, z, "minecraft:light_gray_concrete")
            b.set(x, 2, z, "minecraft:mud_bricks" if i % 2 else "minecraft:packed_mud")
            b.set(x, 3, z, "minecraft:iron_bars")
    # Faixa de aviso no portao.
    for x in range(10, 15):
        b.set(x, 0, 0, "minecraft:yellow_concrete" if x % 2 else "minecraft:black_concrete")
        b.set(x, 0, 1, "minecraft:yellow_concrete" if x % 2 == 0 else "minecraft:black_concrete")
    # Holofotes nos cantos.
    for x, z in ((1, 1), (23, 1), (1, 23), (23, 23)):
        b.fill(x, 1, z, x, 5, z, "minecraft:polished_andesite")
        b.set(x, 6, z, "minecraft:sea_lantern")
        b.set(x, 7, z, "minecraft:light_gray_concrete")
    # Sede (comando) no fundo a esquerda: paredes brancas com faixa ciano, janelas, laje e antena.
    x0, z0, x1, z1 = 2, 13, 13, 22
    b.fill(x0, 1, z0, x1, 5, z1, "minecraft:white_concrete")
    b.fill(x0 + 1, 1, z0 + 1, x1 - 1, 4, z1 - 1, "minecraft:air")
    b.fill(x0 + 1, 0, z0 + 1, x1 - 1, 0, z1 - 1, "minecraft:polished_deepslate")
    for x in range(x0, x1 + 1):
        b.set(x, 3, z0, "minecraft:cyan_concrete")
        b.set(x, 3, z1, "minecraft:cyan_concrete")
    for z in range(z0, z1 + 1):
        b.set(x0, 3, z, "minecraft:cyan_concrete")
        b.set(x1, 3, z, "minecraft:cyan_concrete")
    for x in (x0 + 2, x0 + 3, x1 - 3, x1 - 2):
        b.set(x, 2, z0, "minecraft:glass_pane")
        b.set(x, 2, z1, "minecraft:glass_pane")
    for z in (z0 + 3, z0 + 4, z0 + 6):
        b.set(x0, 2, z, "minecraft:glass_pane")
        b.set(x1, 2, z, "minecraft:glass_pane")
    b.fill(7, 1, z0, 8, 2, z0, "minecraft:air")  # porta
    b.fill(x0, 5, z0, x1, 5, z1, "minecraft:gray_concrete")
    b.fill(x0, 6, z0, x1, 6, z0, "minecraft:light_gray_concrete")
    b.fill(x0, 6, z1, x1, 6, z1, "minecraft:light_gray_concrete")
    b.fill(11, 6, 20, 11, 9, 20, "minecraft:iron_bars")
    b.set(11, 10, 20, "minecraft:lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
    b.set(10, 6, 20, "minecraft:iron_block")
    # Interior: bancada, bau de suprimentos, mapa (mesa de cartografia), luzes.
    b.set(4, 1, 21, "kn8:defense_workbench", {"facing": "south"})
    b.chest(6, 1, 21, "kn8:chests/outpost", "south")
    b.set(8, 1, 21, "minecraft:cartography_table")
    b.set(11, 1, 21, "minecraft:crafting_table")
    b.set(11, 1, 15, "minecraft:smithing_table")
    for x, z in ((4, 15), (11, 18), (4, 18)):
        b.set(x, 4, z, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})
    for x in range(3, 13):
        b.set(x, 1, 17, "minecraft:cyan_carpet")
    # Heliporto a frente a direita.
    hx, hz = 15, 3
    b.fill(hx, 0, hz, hx + 7, 0, hz + 7, "minecraft:black_concrete")
    for z in range(hz + 2, hz + 6):
        b.set(hx + 2, 0, z, "minecraft:yellow_concrete")
        b.set(hx + 5, 0, z, "minecraft:yellow_concrete")
    b.fill(hx + 3, 0, hz + 3, hx + 4, 0, hz + 4, "minecraft:yellow_concrete")
    for x, z in ((hx, hz), (hx + 7, hz), (hx, hz + 7), (hx + 7, hz + 7)):
        b.set(x, 1, z, "minecraft:redstone_lamp", {"lit": "false"})
    # Patio de treino a direita no fundo: chao de terra, bonecos e alvos.
    b.fill(15, 0, 13, 22, 0, 22, "minecraft:coarse_dirt")
    for x in (16, 18, 20):
        b.entity(x, 1, 20, {"id": "kn8:training_dummy", "Rotation": [Float(180.0), Float(0.0)]})
    b.set(22, 1, 15, "minecraft:target", {"power": "0"})
    b.set(22, 1, 17, "minecraft:target", {"power": "0"})
    b.set(22, 2, 15, "minecraft:hay_block", {"axis": "y"})
    # Caixas de suprimento perto do portao e bandeira da Forca de Defesa.
    for x, z in ((3, 3), (4, 3), (3, 4)):
        b.set(x, 1, z, "minecraft:spruce_planks")
    b.chest(4, 1, 4, "kn8:chests/outpost_supplies", "south", "minecraft:barrel")
    b.fill(8, 1, 3, 8, 7, 3, "minecraft:iron_bars")
    for y in (5, 6, 7):
        b.set(9, y, 3, "minecraft:blue_wool")
        b.set(10, y, 3, "minecraft:white_wool" if y == 6 else "minecraft:blue_wool")
        b.set(11, y, 3, "minecraft:blue_wool")
    # Postes de luz no caminho.
    for z in (5, 9):
        b.set(9, 1, z, "minecraft:oak_fence")
        b.set(9, 2, z, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
        b.set(15 - 1, 1, z, "minecraft:oak_fence")
        b.set(14, 2, z, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
    # Guardas.
    b.entity(9, 1, 1, soldier("rifle", "normal", 180.0))
    b.entity(15, 1, 1, soldier("rifle", "normal", 180.0))
    b.entity(18, 1, 15, soldier("sword", "normal", 0.0))
    b.save("defense_outpost")


# --- predio destruido -------------------------------------------------------------------------------------------

def ruined_building():
    w, h = 17, 20
    b = Build((w, h, w), 7)
    rnd = b.random
    x0, z0, x1, z1 = 3, 3, 13, 13
    floors = [0, 4, 8, 12, 16]
    # Altura que sobrou em cada coluna: o kaiju arrancou o canto nordeste (x e z altos) e mordeu o topo.
    def remaining(x, z):
        bite = max(0, (x - 6) + (z - 6)) * 1.3
        return int(h - 2 - bite - rnd.random() * 2)
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            top = remaining(x, z)
            edge = x in (x0, x1) or z in (z0, z1)
            for y in range(0, top):
                if y in floors:
                    b.set(x, y, z, "minecraft:smooth_stone" if not edge else "minecraft:gray_concrete")
                elif edge:
                    # Pilar a cada 5 blocos ao longo da fachada (antes valia o eixo errado e a fachada
                    # inteira virava pilar); janelas em faixas no meio de cada andar.
                    along = x - x0 if z in (z0, z1) else z - z0
                    pillar = along % 5 == 0
                    window = y % 4 in (2, 3) and along % 5 in (2, 3)
                    if pillar:
                        b.set(x, y, z, "minecraft:stone_bricks")
                    elif window:
                        b.set(x, y, z, "minecraft:air" if rnd.random() < 0.6 else "minecraft:glass_pane")
                    else:
                        b.set(x, y, z, "minecraft:cracked_stone_bricks" if rnd.random() < 0.25
                              else "minecraft:light_gray_concrete")
                else:
                    b.set(x, y, z, "minecraft:air")
            if top < h - 2 and rnd.random() < 0.5:
                b.set(x, top, z, "minecraft:andesite_slab", {"type": "bottom", "waterlogged": "false"})
    # Buracos de garra: tres rasgos diagonais na fachada sul (z = z0), com marcas pretas em volta.
    for k in range(3):
        for t in range(9):
            x, y = 5 + k * 2 + t // 3, 13 - t
            b.set(x, y, z0, "minecraft:air")
            b.set(x, y, z0 + 1, "minecraft:air")
            b.set(x + 1, y, z0, "minecraft:black_concrete")
    # Porta e entulho em volta (concreto quebrado, cascalho, pedra).
    b.fill(7, 1, z0, 8, 2, z0, "minecraft:air")
    debris = ["minecraft:cobblestone", "minecraft:gravel", "minecraft:andesite", "minecraft:light_gray_concrete",
              "minecraft:cracked_stone_bricks", "minecraft:mossy_cobblestone"]
    for x in range(w):
        for z in range(w):
            if x0 <= x <= x1 and z0 <= z <= z1:
                continue
            near = max(0, 4 - min(abs(x - x1), abs(z - z1)) // 2) if (x > 9 or z > 9) else 1
            for y in range(1, 1 + rnd.randint(0, near)):
                if rnd.random() < 0.55:
                    b.set(x, y, z, rnd.choice(debris))
    # Dentro: escadaria quebrada, bau e uma placa de alerta (lanterna apagada).
    for i in range(4):
        b.set(5 + i, 1 + i, 11, "minecraft:stone_brick_stairs",
              {"facing": "east", "half": "bottom", "shape": "straight", "waterlogged": "false"})
    b.chest(11, 1, 5, "kn8:chests/ruins", "west")
    b.set(4, 1, 4, "minecraft:cobweb")
    b.set(12, 5, 12, "minecraft:cobweb")
    b.save("ruined_building")


# --- restos de kaiju --------------------------------------------------------------------------------------------

def kaiju_remains():
    w, h, d = 29, 12, 17
    b = Build((w, h, d), 13)
    rnd = b.random
    cx, cz, depth = 14, 8, 3
    # Cratera: tigela de terra grossa/cascalho (y = 0..depth abaixo do chao; start_height -4).
    for x in range(w):
        for z in range(d):
            r = ((x - cx) / 14.0) ** 2 + ((z - cz) / 8.5) ** 2
            if r > 1.0:
                continue
            bottom = int(depth * (1 - r))
            ground = depth - bottom
            b.set(x, ground, z, "minecraft:coarse_dirt" if rnd.random() < 0.6 else "minecraft:gravel")
            for y in range(ground + 1, depth + 1 + 4):
                b.set(x, y, z, "minecraft:air")
            if r > 0.75 and rnd.random() < 0.3:
                b.set(x, depth + 1, z, "minecraft:coarse_dirt")
    base = depth - depth + 1  # fundo da cratera + 1
    # Espinha: arco de osso ao longo de X, subindo no meio.
    spine = {}
    for x in range(3, 26):
        t = (x - 14) / 11.0
        y = base + int(4 * (1 - t * t))
        spine[x] = y
        b.set(x, y, cz, "minecraft:bone_block", {"axis": "x"})
    # Costelas: arcos dos dois lados, de dois em dois blocos.
    for x in range(8, 20, 2):
        top = spine[x]
        for side in (-1, 1):
            for k in range(1, 6):
                z = cz + side * k
                y = top - (k * k) // 5
                if y >= base:
                    b.set(x, y, z, "minecraft:bone_block", {"axis": "z" if k < 3 else "y"})
    # Cranio na ponta oeste (x baixo): caixa de osso com orbitas, mandibula aberta e dentes.
    b.fill(0, base, cz - 2, 3, base + 3, cz + 2, "minecraft:bone_block", {"axis": "y"})
    b.fill(1, base + 1, cz - 1, 2, base + 2, cz + 1, "minecraft:air")
    b.set(0, base + 2, cz - 1, "minecraft:black_concrete")
    b.set(0, base + 2, cz + 1, "minecraft:black_concrete")
    for z in range(cz - 2, cz + 3, 2):
        b.set(0, base, z, "minecraft:pointed_dripstone",
              {"thickness": "tip", "vertical_direction": "up", "waterlogged": "false"})
    # Garras/patas meio enterradas.
    for x, z in ((9, 2), (19, 2), (9, 14), (19, 14)):
        b.fill(x, base, z, x, base + 2, z, "minecraft:bone_block", {"axis": "y"})
        b.set(x + 1, base + 2, z, "minecraft:bone_block", {"axis": "x"})
    # Nucleo apagado no peito (magma, ainda quente) e bau com restos.
    b.set(14, base, cz, "minecraft:magma_block")
    b.set(14, base + 1, cz, "minecraft:air")
    b.chest(16, base, cz + 1, "kn8:chests/kaiju_remains", "north")
    b.save("kaiju_remains")


# --- torre de vigia ---------------------------------------------------------------------------------------------

def watchtower():
    b = Build((9, 20, 9), 5)
    b.fill(0, 0, 0, 8, 0, 8, "minecraft:gray_concrete")
    for x, z in ((1, 1), (7, 1), (1, 7), (7, 7)):
        b.fill(x, 1, z, x, 14, z, "minecraft:polished_deepslate")
    for y in (5, 10):
        for i in range(1, 8):
            for x, z in ((i, 1), (i, 7), (1, i), (7, i)):
                if b.get(x, y, z) is None:
                    b.set(x, y, z, "minecraft:iron_bars")
    # Escada no meio (escada de mao presa num pilar).
    b.fill(4, 1, 4, 4, 14, 4, "minecraft:polished_deepslate")
    b.fill(4, 1, 3, 4, 15, 3, "minecraft:ladder", {"facing": "north", "waterlogged": "false"})
    # Plataforma com guarda-corpo, sirene (sino), holofote e bau.
    b.fill(0, 15, 0, 8, 15, 8, "minecraft:smooth_stone")
    b.set(4, 15, 3, "minecraft:air")
    for i in range(9):
        for x, z in ((i, 0), (i, 8), (0, i), (8, i)):
            b.set(x, 16, z, "minecraft:iron_bars")
    b.set(2, 16, 6, "minecraft:bell", {"attachment": "floor", "facing": "north", "powered": "false"})
    b.set(6, 16, 6, "minecraft:sea_lantern")
    b.set(6, 17, 6, "minecraft:daylight_detector", {"inverted": "false", "power": "0"})
    b.chest(6, 16, 2, "kn8:chests/watchtower", "west")
    b.fill(1, 18, 1, 7, 18, 7, "minecraft:gray_concrete")
    for x, z in ((1, 1), (7, 1), (1, 7), (7, 7)):
        b.set(x, 17, z, "minecraft:polished_deepslate")
    b.set(4, 19, 4, "minecraft:lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
    b.entity(3, 16, 4, soldier("rifle", "normal", 0.0))
    b.save("watchtower")


# --- worldgen e loot ------------------------------------------------------------------------------------------

# (nome, biomas, espacamento, separacao, sal, adaptacao do terreno, altura do inicio)
WORLDGEN = [
    ("defense_outpost", ["minecraft:plains", "minecraft:sunflower_plains", "minecraft:savanna", "minecraft:meadow",
                         "minecraft:forest", "minecraft:birch_forest", "minecraft:taiga", "minecraft:desert"],
     40, 16, 280_430_011, "beard_thin", -1),
    ("ruined_building", ["minecraft:plains", "minecraft:sunflower_plains", "minecraft:forest",
                         "minecraft:dark_forest", "minecraft:savanna", "minecraft:desert", "minecraft:taiga",
                         "minecraft:snowy_plains", "minecraft:swamp"], 26, 10, 280_430_012, "beard_thin", -1),
    # beard_thin: sem adaptacao a cratera ficava enterrada em morro (visto no mundo novo, seed 8008).
    ("kaiju_remains", ["minecraft:plains", "minecraft:desert", "minecraft:savanna", "minecraft:snowy_plains",
                       "minecraft:sunflower_plains"], 34, 14, 280_430_013, "beard_thin", -4),
    ("watchtower", ["minecraft:plains", "minecraft:meadow", "minecraft:forest", "minecraft:taiga",
                    "minecraft:savanna", "minecraft:snowy_plains", "minecraft:birch_forest"],
     30, 12, 280_430_014, "beard_thin", -1),
]


def item(name, weight, low=1, high=1):
    entry = {"type": "minecraft:item", "name": name, "weight": weight}
    if high > 1 or low > 1:
        entry["functions"] = [{"function": "minecraft:set_count",
                               "count": {"type": "minecraft:uniform", "min": low, "max": high}}]
    return entry


LOOT = {
    "outpost": [(3, 5), [item("minecraft:iron_ingot", 10, 1, 4), item("minecraft:redstone", 8, 2, 6),
                         item("kn8:kaiju_tissue", 8, 1, 3), item("kn8:suit_coolant", 5, 1, 2),
                         item("kn8:stamina_stim", 5, 1, 2), item("minecraft:bread", 10, 2, 5),
                         item("kn8:combat_knife", 2), item("kn8:training_dummy", 3)]],
    "outpost_supplies": [(2, 4), [item("minecraft:bread", 10, 3, 6), item("minecraft:cooked_beef", 6, 2, 4),
                                  item("kn8:stamina_stim", 4, 1, 3), item("kn8:suit_coolant", 4, 1, 3),
                                  item("minecraft:torch", 6, 4, 12)]],
    "ruins": [(2, 5), [item("kn8:kaiju_tissue", 10, 1, 4), item("kn8:muscle_fiber", 6, 1, 3),
                       item("minecraft:iron_ingot", 6, 1, 3), item("minecraft:coal", 8, 2, 6),
                       item("minecraft:glass_bottle", 4, 1, 2), item("minecraft:paper", 5, 1, 4)]],
    "kaiju_remains": [(2, 4), [item("kn8:core_fragment", 6, 1, 2), item("kn8:muscle_fiber", 10, 2, 5),
                               item("kn8:kaiju_tissue", 10, 2, 6), item("minecraft:bone", 8, 3, 8),
                               item("kn8:release_catalyst", 1)]],
    "watchtower": [(2, 4), [item("minecraft:arrow", 6, 4, 12), item("kn8:stamina_stim", 6, 1, 2),
                            item("kn8:suit_coolant", 5, 1, 2), item("minecraft:bread", 8, 2, 4),
                            item("minecraft:spyglass", 2), item("minecraft:iron_ingot", 6, 1, 3)]],
}


def dump(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def worldgen():
    wg = DATA / "kn8/worldgen"
    for name, biomes, spacing, separation, salt, adaptation, start in WORLDGEN:
        dump(DATA / f"kn8/tags/worldgen/biome/has_structure/{name}.json", {"values": biomes})
        dump(wg / f"template_pool/{name}/start.json", {"fallback": "minecraft:empty", "elements": [{
            "weight": 1, "element": {"element_type": "minecraft:single_pool_element", "location": f"kn8:{name}",
                                     "projection": "rigid", "processors": "minecraft:empty"}}]})
        dump(wg / f"structure/{name}.json", {
            "type": "minecraft:jigsaw", "biomes": f"#kn8:has_structure/{name}", "step": "surface_structures",
            "spawn_overrides": {}, "terrain_adaptation": adaptation, "start_pool": f"kn8:{name}/start", "size": 1,
            "start_height": {"absolute": start}, "project_start_to_heightmap": "WORLD_SURFACE_WG",
            "max_distance_from_center": 80, "use_expansion_hack": False})
        dump(wg / f"structure_set/{name}.json", {
            "structures": [{"structure": f"kn8:{name}", "weight": 1}],
            "placement": {"type": "minecraft:random_spread", "spacing": spacing, "separation": separation,
                          "salt": salt}})
    for name, (rolls, entries) in LOOT.items():
        dump(DATA / f"kn8/loot_table/chests/{name}.json", {"type": "minecraft:chest", "pools": [{
            "rolls": {"type": "minecraft:uniform", "min": rolls[0], "max": rolls[1]}, "entries": entries}]})


if __name__ == "__main__":
    defense_outpost()
    ruined_building()
    kaiju_remains()
    watchtower()
    worldgen()
