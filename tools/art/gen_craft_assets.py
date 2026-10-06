#!/usr/bin/env python3
"""Arte placeholder da Etapa 3 da 0.2 (fabricacao): bancada (bloco), trajes (icone + camada de armadura) e
suprimentos (icones), com os modelos de item/bloco e o blockstate. Substituivel por arte final.
Uso: python3 tools/art/gen_craft_assets.py
"""
import json
from pathlib import Path

from PIL import Image, ImageDraw

ASSETS = Path(__file__).resolve().parents[2] / "src/main/resources/assets/kn8"

# Trajes: (base, faixa, detalhe). Mk1 = preto com ciano (Forca de Defesa); reforcado = preto com laranja.
SUITS = {
    "training_suit": ((70, 82, 70), (150, 160, 140), (200, 205, 190)),
    "mk1": ((28, 31, 38), (53, 200, 255), (230, 236, 245)),
    "mk1_reinforced": ((24, 26, 30), (255, 138, 42), (200, 205, 215)),
}


def save(image, rel):
    path = ASSETS / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)


def write_json(rel, data):
    path = ASSETS / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def item_model(name):
    write_json(f"models/item/{name}.json",
               {"parent": "minecraft:item/generated", "textures": {"layer0": f"kn8:item/{name}"}})


# --- icones -------------------------------------------------------------------------------------------------------

def suit_icon(base, stripe, detail):
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(image)
    dark = tuple(max(0, c - 25) for c in base)
    # Peitoral com ombreiras (formato de colete).
    d.polygon([(2, 3), (5, 2), (7, 4), (9, 4), (11, 2), (14, 3), (14, 7), (12, 7), (12, 14), (4, 14), (4, 7),
               (2, 7)], fill=base, outline=dark)
    d.line([(8, 5), (8, 13)], fill=dark)
    d.line([(5, 9), (11, 9)], fill=stripe)
    d.line([(3, 4), (4, 4)], fill=detail)
    d.line([(12, 4), (13, 4)], fill=detail)
    d.point((8, 7), fill=stripe)
    return image


def coolant_icon():
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(image)
    d.rectangle([5, 1, 10, 3], fill=(150, 160, 175), outline=(80, 90, 105))
    d.rectangle([4, 4, 11, 14], fill=(40, 120, 200), outline=(20, 60, 110))
    d.rectangle([6, 6, 7, 12], fill=(140, 220, 255))
    d.line([(4, 9), (11, 9)], fill=(230, 240, 250))
    return image


def stim_icon():
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(image)
    d.line([(2, 13), (4, 11)], fill=(200, 205, 215))
    d.polygon([(4, 10), (10, 4), (12, 6), (6, 12)], fill=(80, 220, 110), outline=(30, 110, 50))
    d.line([(6, 9), (9, 6)], fill=(190, 255, 200))
    d.rectangle([11, 2, 14, 5], fill=(180, 185, 195), outline=(90, 95, 105))
    return image


def catalyst_icon():
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(image)
    d.polygon([(8, 1), (13, 7), (8, 15), (3, 7)], fill=(255, 120, 30), outline=(130, 50, 10))
    d.polygon([(8, 4), (10, 7), (8, 11), (6, 7)], fill=(255, 210, 120))
    d.point((7, 5), fill=(255, 255, 230))
    return image


# --- camada de armadura (64x32, mapa de humanoide) ---------------------------------------------------------------

def armor_layer(base, stripe, detail):
    image = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(image)
    dark = tuple(max(0, c - 20) for c in base)
    # Corpo (16..40, 16..32) e bracos (40..56, 16..32): o peitoral usa os dois.
    d.rectangle([16, 16, 39, 31], fill=base)
    d.rectangle([40, 16, 55, 31], fill=base)
    # Frente do corpo: faixa no peito, linha central e cinto.
    d.rectangle([20, 22, 27, 23], fill=stripe)
    d.line([(24, 20), (24, 31)], fill=dark)
    d.rectangle([20, 29, 27, 29], fill=detail)
    d.rectangle([20, 20, 21, 21], fill=detail)
    d.rectangle([26, 20, 27, 21], fill=detail)
    # Costas: placa e faixa.
    d.rectangle([33, 21, 38, 26], fill=dark)
    d.rectangle([33, 23, 38, 23], fill=stripe)
    # Bracos: ombreira clara e faixa no antebraco.
    d.rectangle([40, 20, 55, 21], fill=detail)
    d.rectangle([44, 16, 51, 19], fill=detail)
    d.rectangle([40, 27, 55, 27], fill=stripe)
    return image


# --- bancada ------------------------------------------------------------------------------------------------------

def bench_textures():
    metal = (62, 68, 78)
    dark = (34, 38, 46)
    light = (110, 118, 130)
    side = Image.new("RGBA", (16, 16), metal)
    d = ImageDraw.Draw(side)
    d.rectangle([0, 0, 15, 15], outline=dark)
    d.rectangle([1, 1, 14, 2], fill=light)
    for x in range(0, 16, 4):  # faixa de aviso amarela e preta
        d.polygon([(x, 13), (x + 2, 13), (x + 4, 15), (x + 2, 15)], fill=(240, 190, 40))
    d.rectangle([0, 12, 15, 12], fill=dark)
    d.point((3, 5), fill=light)
    d.point((12, 5), fill=light)
    d.point((3, 10), fill=light)
    d.point((12, 10), fill=light)

    front = side.copy()
    d = ImageDraw.Draw(front)
    d.rectangle([3, 4, 12, 10], fill=(10, 30, 45), outline=dark)  # tela
    d.line([(4, 8), (6, 6), (8, 7), (11, 5)], fill=(53, 200, 255))
    d.point((11, 9), fill=(76, 217, 100))

    top = Image.new("RGBA", (16, 16), (80, 86, 96))
    d = ImageDraw.Draw(top)
    d.rectangle([0, 0, 15, 15], outline=dark)
    for i in range(3, 15, 4):
        d.line([(i, 1), (i, 14)], fill=(70, 76, 86))
        d.line([(1, i), (14, i)], fill=(70, 76, 86))
    d.rectangle([2, 2, 6, 4], fill=(150, 155, 165))  # chave
    d.rectangle([9, 9, 13, 13], fill=(255, 138, 42), outline=(140, 60, 15))  # fragmento de nucleo
    d.line([(9, 2), (13, 6)], fill=(200, 205, 215))

    bottom = Image.new("RGBA", (16, 16), dark)
    ImageDraw.Draw(bottom).rectangle([0, 0, 15, 15], outline=(24, 26, 32))
    return {"defense_workbench_side": side, "defense_workbench_front": front, "defense_workbench_top": top,
            "defense_workbench_bottom": bottom}


def main():
    for name, colors in SUITS.items():
        save(suit_icon(*colors), f"textures/item/{name}.png")
        save(armor_layer(*colors), f"textures/models/armor/{name}_layer_1.png")
        item_model(name)
    for name, painter in (("suit_coolant", coolant_icon), ("stamina_stim", stim_icon),
                          ("release_catalyst", catalyst_icon)):
        save(painter(), f"textures/item/{name}.png")
        item_model(name)
    for name, image in bench_textures().items():
        save(image, f"textures/block/{name}.png")
    write_json("models/block/defense_workbench.json", {
        "parent": "minecraft:block/orientable_with_bottom",
        "textures": {"top": "kn8:block/defense_workbench_top", "front": "kn8:block/defense_workbench_front",
                     "side": "kn8:block/defense_workbench_side", "bottom": "kn8:block/defense_workbench_bottom"}})
    write_json("models/item/defense_workbench.json", {"parent": "kn8:block/defense_workbench"})
    write_json("blockstates/defense_workbench.json", {"variants": {
        "facing=north": {"model": "kn8:block/defense_workbench"},
        "facing=east": {"model": "kn8:block/defense_workbench", "y": 90},
        "facing=south": {"model": "kn8:block/defense_workbench", "y": 180},
        "facing=west": {"model": "kn8:block/defense_workbench", "y": 270}}})
    print("Gerado: bancada, 3 trajes e 3 suprimentos")


if __name__ == "__main__":
    main()
