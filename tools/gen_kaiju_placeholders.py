#!/usr/bin/env python3
"""Gera modelo, animacoes e textura placeholder de cada kaiju a partir do JSON de dados da especie.

Fonte unica: data/kn8/kn8/kaiju/<especie>.json (dimensions e parts). Assim a hitbox, as partes e o modelo nunca
divergem (licao do PT4). Arte final substitui estes arquivos mantendo os nomes de ossos e de animacoes.

Animacoes: as 5 genericas + uma por habilidade da especie (nome = campo "animation" do JSON da habilidade, pico
no tick de impacto windup_ticks).
Ossos: "root" (pai de tudo, usado pelas animacoes genericas); com partes no JSON, um osso por parte (cubo do
tamanho da parte); sem partes, um corpo do tamanho da hitbox e, se a especie tiver a tag "arachnid", 8 patas.
Frente = norte (-Z), padrao da GeckoLib. Offset das partes no JSON: [direita, cima, frente].
Uso: pip install pillow && python3 tools/gen_kaiju_placeholders.py
"""
import json
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / "src/main/resources/data/kn8/kn8/kaiju"
ABILITIES = ROOT / "src/main/resources/data/kn8/kn8/ability"
ASSETS = ROOT / "src/main/resources/assets/kn8"
PX = 16
TILE = 8
# Cores por especie: (corpo, destaque/nucleo)
COLORS = {
    "trichonephila": ((90, 70, 40, 255), (200, 60, 60, 255)),
    "primigenius": ((120, 60, 200, 255), (230, 40, 40, 255)),
    "primigenius_revived": ((60, 120, 90, 255), (255, 140, 0, 255)),
}
DEFAULT_COLORS = ((128, 128, 128, 255), (255, 0, 0, 255))
# Especies que ja tem arte final: o gerador de placeholders NAO toca nos arquivos delas.
FINAL_ART = {"trichonephila", "primigenius", "primigenius_revived", "primigenius_resurrected", "primigenius_honju"}


def face_uv(tile):
    u, v = {"body": (0, 0), "accent": (8, 0)}[tile]
    return {side: {"uv": [u, v], "uv_size": [TILE, TILE]}
            for side in ("north", "south", "east", "west", "up", "down")}


def cube(name, parent, center_x, base_y, center_z, width, height, depth, tile):
    w, h, d = width * PX, height * PX, depth * PX
    cx, cz = center_x * PX, center_z * PX
    return {"name": name, "parent": parent, "pivot": [round(cx, 3), round(base_y * PX, 3), round(cz, 3)],
            "cubes": [{"origin": [round(cx - w / 2, 3), round(base_y * PX, 3), round(cz - d / 2, 3)],
                       "size": [round(w, 3), round(h, 3), round(d, 3)], "uv": face_uv(tile)}]}


def bones_for(definition):
    bones = [{"name": "root", "pivot": [0, 0, 0]}]
    parts = definition.get("parts", [])
    if parts:
        for part in parts:
            right, up, forward = part["offset"]
            # Frente do modelo = -Z (norte). O renderer gira o modelo 180 graus, entao a direita do kaiju e +X no
            # modelo (correcao do M7b: antes estava espelhado; nao aparecia porque todas as partes tinham right = 0).
            bones.append(cube(part["name"], "root", right, up, -forward, part["width"], part["height"],
                              part["width"], "accent" if part.get("core") else "body"))
        return bones
    width = definition["dimensions"]["width"]
    height = definition["dimensions"]["height"]
    if "arachnid" in definition.get("tags", []):
        body_h = height * 0.5
        bones.append(cube("body", "root", 0, height * 0.4, 0, width * 0.6, body_h, width * 0.8, "body"))
        bones.append(cube("head", "body", 0, height * 0.45, -width * 0.5, width * 0.35, body_h * 0.7,
                          width * 0.3, "accent"))
        for side, sign in (("left", 1), ("right", -1)):
            for index in range(4):
                z = (index - 1.5) * width * 0.22
                bones.append(cube(f"leg_{side}_{index}", "root", sign * width * 0.4, 0, z, width * 0.12,
                                  height * 0.45, width * 0.08, "body"))
        return bones
    bones.append(cube("body", "root", 0, 0, 0, width, height, width, "body"))
    return bones


def ability_animation(ability):
    """Bote generico com o pico no tick de impacto do JSON (windup_ticks) e fim em windup + active."""
    windup = ability["windup_ticks"] / 20.0
    end = max(windup + ability.get("active_ticks", 1) / 20.0, windup + 0.05)
    crouch = round(max(windup * 0.6, 0.01), 3)
    return {"animation_length": round(end, 3), "bones": {
        "root": {"rotation": {"0.0": [0, 0, 0], str(crouch): [-15, 0, 0], str(round(windup, 3)): [25, 0, 0],
                              str(round(end, 3)): [0, 0, 0]},
                 "position": {"0.0": [0, 0, 0], str(round(windup, 3)): [0, 0, -4], str(round(end, 3)): [0, 0, 0]}}}}


def animations_for(species, bones, definition):
    names = {bone["name"] for bone in bones}
    legs = sorted(name for name in names if name.startswith("leg_"))
    walk_bones = {"root": {"position": {"0.0": [0, 0, 0], "0.25": [0, 0.6, 0], "0.5": [0, 0, 0],
                                        "0.75": [0, 0.6, 0], "1.0": [0, 0, 0]}}}
    for index, leg in enumerate(legs):
        swing = 25 if index % 2 == 0 else -25
        walk_bones[leg] = {"rotation": {"0.0": [swing, 0, 0], "0.5": [-swing, 0, 0], "1.0": [swing, 0, 0]}}
    if "legs" in names:
        walk_bones["legs"] = {"rotation": {"0.0": [8, 0, 0], "0.5": [-8, 0, 0], "1.0": [8, 0, 0]}}
    prefix = species + "."
    animations = {"format_version": "1.8.0", "animations": {
        prefix + "movement.idle": {"loop": True, "animation_length": 2.0, "bones": {
            "root": {"rotation": {"0.0": [0, 0, 0], "1.0": [1.5, 0, 0], "2.0": [0, 0, 0]}}}},
        prefix + "movement.walk": {"loop": True, "animation_length": 1.0, "bones": walk_bones},
        prefix + "action.attack": {"animation_length": 0.6, "bones": {
            "root": {"rotation": {"0.0": [0, 0, 0], "0.2": [-12, 0, 0], "0.35": [20, 0, 0], "0.6": [0, 0, 0]}}}},
        prefix + "reaction.hurt": {"animation_length": 0.3, "bones": {
            "root": {"rotation": {"0.0": [0, 0, 0], "0.1": [-10, 0, 6], "0.3": [0, 0, 0]}}}},
        prefix + "overlay.breathe": {"loop": True, "animation_length": 2.5, "bones": {
            "root": {"scale": {"0.0": [1, 1, 1], "1.25": [1.015, 1.02, 1.015], "2.5": [1, 1, 1]}}}},
    }}
    # Uma animacao por habilidade da especie, com o nome do campo "animation" do JSON da habilidade (M8).
    for ability_id in definition.get("abilities", []):
        path = ABILITIES / (ability_id.split(":", 1)[1] + ".json")
        if path.exists():
            ability = json.loads(path.read_text(encoding="utf-8"))
            animations["animations"][prefix + ability["animation"]] = ability_animation(ability)
    return animations


def texture(species):
    body, accent = COLORS.get(species, DEFAULT_COLORS)
    image = Image.new("RGBA", (16, 8), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw.rectangle([0, 0, 7, 7], fill=body)
    draw.rectangle([8, 0, 15, 7], fill=accent)
    return image


def main():
    for sub in ("geo/entity", "animations/entity", "textures/entity"):
        (ASSETS / sub).mkdir(parents=True, exist_ok=True)
    for path in sorted(DATA.glob("*.json")):
        species = path.stem
        if species in FINAL_ART:
            print(f"Pulado: {species} (arte final em tools/art/)")
            continue
        definition = json.loads(path.read_text(encoding="utf-8"))
        bones = bones_for(definition)
        size = max(definition["dimensions"]["width"], definition["dimensions"]["height"])
        geo = {"format_version": "1.12.0", "minecraft:geometry": [{
            "description": {"identifier": f"geometry.{species}", "texture_width": 16, "texture_height": 8,
                            "visible_bounds_width": round(size * 1.5 + 1, 2),
                            "visible_bounds_height": round(size * 1.5 + 1, 2),
                            "visible_bounds_offset": [0, round(definition["dimensions"]["height"] / 2, 2), 0]},
            "bones": bones}]}
        (ASSETS / f"geo/entity/{species}.geo.json").write_text(json.dumps(geo, indent=2) + "\n", encoding="utf-8")
        (ASSETS / f"animations/entity/{species}.animation.json").write_text(
            json.dumps(animations_for(species, bones, definition), indent=2) + "\n", encoding="utf-8")
        texture(species).save(ASSETS / f"textures/entity/{species}.png")
        print(f"Gerado: {species} ({len(bones)} ossos)")


if __name__ == "__main__":
    main()
