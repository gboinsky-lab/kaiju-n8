#!/usr/bin/env python3
"""Arte provisoria das armas numeradas do jogador (PM-11A). Substituivel pelos modelos do Miguel.

- Numbers 1: a baioneta do Narumi (mesmo OBJ e textura; o Narumi No. 1 usa a mesma arma).
- Numbers 6: o OBJ do rifle com a textura puxada para o azul-gelo do traje do Reno No. 6 (#8FD8FF).
- Numbers 2: icone 16x16 das manoplas (preto com placas laranja, a cor do Isao) segurado como item.
- Numbers 4 (PM-11B): o OBJ do machado com a textura puxada para o ciano das asas da Kikoru No. 4 (#3CC5EC).
- Numbers 10 (PM-11B): o traje 3D do Mk1 Reforcado (mesma malha) com textura, camada de armadura e icone escurecidos
  e puxados para o vermelho-arroxeado do traje do Hoshina No. 10 (#9A2A5A).

Uso: python3 tools/art/gen_numbered_gear.py
"""
import json
from pathlib import Path

from PIL import Image, ImageDraw

ASSETS = Path(__file__).resolve().parents[2] / "src/main/resources/assets/kn8"
ICE = (143, 216, 255)


def ice_rifle():
    src = Image.open(ASSETS / "textures/item/rifle.png").convert("RGBA")
    out = Image.new("RGBA", src.size)
    px, po = src.load(), out.load()
    for y in range(src.height):
        for x in range(src.width):
            r, g, b, a = px[x, y]
            lum = (r * 0.3 + g * 0.59 + b * 0.11) / 255.0
            # Mantem o sombreado (luminancia) e troca o tom pelo azul-gelo; partes claras ficam quase brancas.
            mix = 0.55
            tinted = (int(min(255, c * lum * 1.25 * mix + v * (1 - mix))) for c, v in zip(ICE, (r, g, b)))
            po[x, y] = tuple(tinted) + (a,)
    out.save(ASSETS / "textures/item/numbers6_rifle.png")
    model = json.loads((ASSETS / "models/item/rifle.json").read_text())
    model["textures"] = {"texture": "kn8:item/numbers6_rifle", "particle": "kn8:item/numbers6_rifle"}
    (ASSETS / "models/item/numbers6_rifle.json").write_text(json.dumps(model, indent=2) + "\n")


def tint(src_path, color, mix, dark=1.0):
    src = Image.open(src_path).convert("RGBA")
    out = Image.new("RGBA", src.size)
    px, po = src.load(), out.load()
    for y in range(src.height):
        for x in range(src.width):
            r, g, b, a = px[x, y]
            lum = (r * 0.3 + g * 0.59 + b * 0.11) / 255.0
            po[x, y] = tuple(int(min(255, (c * lum * 1.25 * mix + v * (1 - mix)) * dark))
                             for c, v in zip(color, (r, g, b))) + (a,)
    return out


def wing_axe():
    tint(ASSETS / "textures/item/axe.png", (60, 197, 236), 0.5).save(ASSETS / "textures/item/numbers4_axe.png")
    model = json.loads((ASSETS / "models/item/axe.json").read_text())
    model["textures"] = {"texture": "kn8:item/numbers4_axe", "particle": "kn8:item/numbers4_axe"}
    (ASSETS / "models/item/numbers4_axe.json").write_text(json.dumps(model, indent=2) + "\n")


def no10_suit():
    red = (154, 42, 90)
    tint(ASSETS / "textures/models/suit/mk1_reinforced.png", red, 0.45, 0.8).save(
        ASSETS / "textures/models/suit/numbers10_suit.png")
    tint(ASSETS / "textures/models/armor/mk1_reinforced_layer_1.png", red, 0.45, 0.8).save(
        ASSETS / "textures/models/armor/numbers10_suit_layer_1.png")
    tint(ASSETS / "textures/item/mk1_reinforced.png", red, 0.5, 0.85).save(
        ASSETS / "textures/item/numbers10_suit.png")
    (ASSETS / "models/item/numbers10_suit.json").write_text(json.dumps(
        {"parent": "minecraft:item/generated", "textures": {"layer0": "kn8:item/numbers10_suit"}}, indent=2) + "\n")
    mesh = json.loads((ASSETS / "meshes/suit/mk1_reinforced.json").read_text())
    (ASSETS / "meshes/suit/numbers10_suit.json").write_text(json.dumps(mesh, indent=2) + "\n")


def gauntlets():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([3, 6, 12, 14], fill=(30, 30, 34, 255), outline=(12, 12, 14, 255))  # punho
    d.rectangle([4, 2, 11, 6], fill=(36, 36, 40, 255), outline=(12, 12, 14, 255))  # dedos
    for x in (5, 7, 9):
        d.line([(x, 2), (x, 5)], fill=(18, 18, 20, 255))
    d.rectangle([5, 8, 10, 11], fill=(240, 140, 40, 255))  # placa laranja
    d.line([(5, 8), (10, 8)], fill=(255, 196, 96, 255))
    d.line([(4, 13), (11, 13)], fill=(240, 140, 40, 255))
    img.save(ASSETS / "textures/item/numbers2_gauntlets.png")
    (ASSETS / "models/item/numbers2_gauntlets.json").write_text(json.dumps(
        {"parent": "minecraft:item/handheld", "textures": {"layer0": "kn8:item/numbers2_gauntlets"}}, indent=2) + "\n")


if __name__ == "__main__":
    ice_rifle()
    gauntlets()
    wing_axe()
    no10_suit()
    print("armas numeradas do jogador geradas")
