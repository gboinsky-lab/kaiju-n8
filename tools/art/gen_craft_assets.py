#!/usr/bin/env python3
"""Arte da Etapa 3 da 0.2 (fabricacao; desde a 0.3 sombreada por pixel_shading.py): bancada (bloco), trajes (icone + camada de armadura) e
suprimentos (icones), com os modelos de item/bloco e o blockstate. Substituivel por arte final.
Uso: python3 tools/art/gen_craft_assets.py
"""
import json
import random
import sys
from pathlib import Path

from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).parent))
from pixel_shading import dot, mask, new, paint, shade  # noqa: E402

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


# --- icones (0.3: sombreados por pixel_shading, estilo dos itens do Minecraft) -----------------------------------

def suit_icon(base, stripe, detail):
    canvas = new()
    body, d = mask()
    # Peitoral com ombreiras largas e gola (silhueta do peitoral vanilla, mais "tatico").
    d.polygon([(1, 3), (5, 1), (6, 3), (9, 3), (10, 1), (14, 3), (14, 7), (12, 8), (12, 14), (3, 14), (3, 8),
               (1, 7)], fill=255)
    d.rectangle([7, 2, 8, 3], fill=0)
    paint(canvas, body, base)
    plates, d = mask()
    d.rectangle([2, 3, 4, 6], fill=255)
    d.rectangle([11, 3, 13, 6], fill=255)
    paint(canvas, plates, detail, outline=False)
    chest, d = mask()
    d.rectangle([5, 5, 10, 8], fill=255)
    paint(canvas, chest, shade(base, 0.12), outline=False)
    for x in range(4, 12):
        dot(canvas, x, 10, stripe)
    dot(canvas, 7, 6, stripe)
    dot(canvas, 8, 6, stripe)
    for y in range(9, 14):
        dot(canvas, 7, y, shade(base, -0.35))
    return canvas


def coolant_icon():
    canvas = new()
    cap, d = mask()
    d.rectangle([5, 1, 10, 3], fill=255)
    paint(canvas, cap, (150, 160, 175))
    tank, d = mask()
    d.rounded_rectangle([3, 4, 12, 14], radius=2, fill=255)
    paint(canvas, tank, (40, 120, 200))
    glass, d = mask()
    d.rectangle([5, 6, 7, 12], fill=255)
    paint(canvas, glass, (150, 225, 255), outline=False)
    for x in range(4, 12):
        dot(canvas, x, 9, (225, 240, 250))
    for x, y in ((10, 6), (9, 12), (11, 11)):
        dot(canvas, x, y, (200, 240, 255))
    return canvas


def stim_icon():
    canvas = new()
    needle, d = mask()
    d.line([(1, 14), (4, 11)], fill=255)
    paint(canvas, needle, (200, 205, 215), outline=False)
    body, d = mask()
    d.polygon([(4, 10), (10, 4), (12, 6), (6, 12)], fill=255)
    paint(canvas, body, (70, 210, 100))
    plunger, d = mask()
    d.rectangle([11, 1, 14, 4], fill=255)
    paint(canvas, plunger, (175, 180, 190))
    for i in range(3):
        dot(canvas, 6 + i, 8 - i, (200, 255, 210))
    return canvas


def catalyst_icon():
    canvas = new()
    glow, d = mask()
    d.polygon([(8, 0), (14, 7), (8, 15), (2, 7)], fill=255)
    paint(canvas, glow, (255, 120, 30))
    inner, d = mask()
    d.polygon([(8, 3), (11, 7), (8, 12), (5, 7)], fill=255)
    paint(canvas, inner, (255, 205, 110), outline=False)
    dot(canvas, 7, 5, (255, 255, 235))
    dot(canvas, 6, 6, (255, 245, 210))
    return canvas


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

def _metal(seed, base=(64, 70, 80)):
    """Chapa de metal 16x16 com leve ruido (o Minecraft fica "liso demais" com cor chapada)."""
    rnd = random.Random(seed)
    image = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            image.putpixel((x, y), shade(base, rnd.uniform(-0.07, 0.07)) + (255,))
    return image


def _frame(image, light=(118, 126, 138), dark=(30, 34, 42)):
    d = ImageDraw.Draw(image)
    d.line([(0, 0), (15, 0)], fill=light)
    d.line([(0, 0), (0, 15)], fill=light)
    d.line([(15, 1), (15, 15)], fill=dark)
    d.line([(1, 15), (15, 15)], fill=dark)


def _rivets(image, points):
    for x, y in points:
        image.putpixel((x, y), (150, 158, 170, 255))
        image.putpixel((x + 1, y + 1), (28, 30, 36, 255))


def _hazard(image, y0):
    for x in range(16):
        for y in range(y0, y0 + 2):
            yellow = ((x + y) // 2) % 2 == 0
            image.putpixel((x, y), (240, 190, 40, 255) if yellow else (30, 30, 34, 255))


def bench_textures():
    side = _metal(1)
    d = ImageDraw.Draw(side)
    d.rectangle([2, 3, 13, 10], outline=(44, 48, 56))  # porta de painel
    d.line([(3, 4), (12, 4)], fill=(96, 104, 116))
    d.rectangle([11, 6, 12, 7], fill=(160, 166, 176))  # puxador
    _rivets(side, [(1, 2), (13, 2), (1, 11), (13, 11)])
    _hazard(side, 12)
    _frame(side)
    d.line([(1, 14), (14, 14)], fill=(40, 44, 52))

    front = _metal(2)
    d = ImageDraw.Draw(front)
    d.rectangle([2, 2, 13, 9], fill=(16, 22, 30), outline=(40, 46, 56))  # tela
    for x, y in ((3, 7), (4, 6), (5, 6), (6, 5), (7, 6), (8, 4), (9, 4), (10, 5), (11, 3), (12, 3)):
        front.putpixel((x, y), (53, 200, 255, 255))
    for x in range(3, 13):
        front.putpixel((x, 8), (24, 70, 90, 255))
    front.putpixel((12, 8), (76, 217, 100, 255))
    d.rectangle([3, 10, 12, 11], fill=(44, 48, 56))  # teclado
    for x in range(4, 12, 2):
        front.putpixel((x, 10), (120, 128, 140, 255))
    _hazard(front, 12)
    _frame(front)
    d.line([(1, 14), (14, 14)], fill=(40, 44, 52))

    top = _metal(3, (82, 88, 98))
    d = ImageDraw.Draw(top)
    for i in (5, 10):
        d.line([(i, 1), (i, 14)], fill=(70, 76, 86))
        d.line([(1, i), (14, i)], fill=(70, 76, 86))
    # chave inglesa
    for x, y in ((2, 2), (3, 3), (4, 4), (5, 5), (6, 6)):
        top.putpixel((x, y), (176, 182, 192, 255))
    top.putpixel((1, 2), (176, 182, 192, 255))
    top.putpixel((2, 1), (176, 182, 192, 255))
    # faca de combate
    for x in range(9, 14):
        top.putpixel((x, 3), (210, 216, 224, 255))
    top.putpixel((8, 3), (40, 40, 44, 255))
    top.putpixel((7, 3), (40, 40, 44, 255))
    # fragmento de nucleo brilhando
    d.rectangle([10, 10, 13, 13], fill=(255, 138, 42), outline=(140, 60, 15))
    top.putpixel((11, 11), (255, 220, 150, 255))
    # tecido de kaiju
    d.rectangle([2, 10, 5, 12], fill=(130, 60, 70), outline=(70, 25, 35))
    _frame(top)

    bottom = _metal(4, (40, 44, 52))
    _frame(bottom, (60, 64, 72), (24, 26, 32))
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
