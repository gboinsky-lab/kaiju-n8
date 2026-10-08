#!/usr/bin/env python3
"""Icones 16x16 dos materiais de desmonte (M11b/0.1-B) + modelos de item; desde a 0.3 sombreados por
pixel_shading.py (estilo dos itens do Minecraft).
Uso: python3 tools/art/gen_material_icons.py
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from pixel_shading import dot, mask, new, paint  # noqa: E402

ASSETS = Path(__file__).resolve().parents[2] / "src/main/resources/assets/kn8"


def tissue():
    canvas = new()
    lump, d = mask()
    d.polygon([(3, 5), (8, 2), (13, 5), (13, 11), (7, 14), (2, 10)], fill=255)
    paint(canvas, lump, (140, 62, 74))
    for x, y in ((5, 7), (6, 7), (7, 6), (9, 10), (10, 10), (11, 9), (5, 11), (6, 11)):
        dot(canvas, x, y, (190, 100, 112))
    for x, y in ((8, 8), (9, 7), (4, 9)):
        dot(canvas, x, y, (95, 35, 45))
    return canvas


def fiber():
    canvas = new()
    for i, color in enumerate([(190, 70, 70), (160, 50, 56), (210, 96, 90)]):
        strand, d = mask()
        d.line([(3 + i * 3, 2), (6 + i * 3, 13)], fill=255, width=2)
        paint(canvas, strand, color)
    for y in (5, 9):
        dot(canvas, 6, y, (240, 150, 140))
    return canvas


def fragment():
    canvas = new()
    shard, d = mask()
    d.polygon([(4, 10), (7, 2), (13, 6), (11, 13), (5, 13)], fill=255)
    paint(canvas, shard, (215, 80, 30))
    glow, d = mask()
    d.polygon([(7, 6), (10, 7), (9, 10), (6, 10)], fill=255)
    paint(canvas, glow, (255, 190, 90), outline=False)
    dot(canvas, 8, 7, (255, 245, 210))
    return canvas


def core():
    canvas = new()
    orb, d = mask()
    d.ellipse([1, 1, 14, 14], fill=255)
    paint(canvas, orb, (235, 85, 25))
    ring, d = mask()
    d.ellipse([4, 4, 11, 11], fill=255)
    paint(canvas, ring, (255, 170, 70), outline=False)
    center, d = mask()
    d.ellipse([6, 6, 9, 9], fill=255)
    paint(canvas, center, (255, 230, 160), outline=False)
    dot(canvas, 6, 5, (255, 255, 235))
    dot(canvas, 4, 4, (255, 210, 150))
    return canvas


def magazine(curved, width):
    """0.5.0-D2: pente (cinza escuro, cartucho de latao aparecendo em cima). Provisorio ate o modelo do Miguel."""
    canvas = new()
    body, d = mask()
    left = 8 - width // 2
    if curved:
        d.polygon([(left, 3), (left + width, 3), (left + width + 2, 13), (left + 2, 13)], fill=255)
    else:
        d.rectangle([left, 3, left + width, 13], fill=255)
    paint(canvas, body, (58, 62, 70))
    base, d = mask()
    shift = 2 if curved else 0
    d.rectangle([left + shift - 1, 13, left + width + shift + 1, 14], fill=255)
    paint(canvas, base, (40, 42, 48), outline=False)
    top, d = mask()
    d.rectangle([left + 1, 1, left + width - 1, 3], fill=255)
    paint(canvas, top, (214, 170, 70), outline=False)
    for y in (6, 9):
        dot(canvas, left + 1 + (1 if curved and y > 7 else 0), y, (110, 116, 126))
    return canvas


def ammo(count, length):
    """0.5.0-D2: cartuchos de latao com ponta de cobre lado a lado. Provisorio ate o modelo do Miguel."""
    canvas = new()
    gap = 12 // count
    for i in range(count):
        x = 2 + i * gap
        top = 14 - length
        shell, d = mask()
        d.rectangle([x, top + 3, x + 1, 14], fill=255)
        paint(canvas, shell, (214, 170, 70))
        tip, d = mask()
        d.rectangle([x, top, x + 1, top + 2], fill=255)
        paint(canvas, tip, (184, 104, 60), outline=False)
        dot(canvas, x, top + 4, (250, 220, 140))
    return canvas


def main():
    for name, painter in (("kaiju_tissue", tissue), ("muscle_fiber", fiber), ("core_fragment", fragment),
                          ("intact_core", core), ("rifle_magazine", lambda: magazine(True, 4)),
                          ("pistol_magazine", lambda: magazine(False, 3)), ("rifle_ammo", lambda: ammo(4, 11)),
                          ("pistol_ammo", lambda: ammo(4, 7))):
        image = painter()
        image.save(ASSETS / f"textures/item/{name}.png")
        model = {"parent": "minecraft:item/generated", "textures": {"layer0": f"kn8:item/{name}"}}
        (ASSETS / f"models/item/{name}.json").write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")
        print(f"Gerado: {name}")


if __name__ == "__main__":
    main()
