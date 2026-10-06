#!/usr/bin/env python3
"""Arte final (v1) do Primigenius Yoju e do Primigenius Yoju ressurgido: um modelo, um esqueleto, duas texturas.

Folha aprovada: brute curvado com cabeca de crocodilo e barbatanas, apoiado nos bracos enormes, corcunda com
placas na coluna, nucleo em brasa no peito e cauda grossa. Linha clara = Yoju; linha verde = ressurgido.
Medidas: hitbox do JSON 2,4 x 4,6 blocos (bracos e cauda passam um pouco, como na folha). Guia de arte: kaiju
grande, ate 35 ossos, textura 128x128. Animacoes: as genericas + slam e charge com os tempos do JSON.
Uso: python3 tools/art/build_primigenius.py
"""
import json
import kaiju_art as art
from kaiju_art import Cube, Model, kf

PALETTES = {
    # Linha de baixo da folha: osso claro com musgo nas costas.
    "primigenius": {
        "hide": "#BDB6A3", "hide_dark": "#A39C8A", "hide_light": "#D3CDBC",
        "back": "#5A6148", "back_dark": "#474D38", "back_light": "#6E7660",
        "belly": "#9C6B57", "belly_dark": "#7E5243", "belly_light": "#B07D66",
        "plate": "#5A6148", "plate_edge": "#747B62", "plate_dark": "#474D38",
        "fin": "#8A4B3A", "fin_dark": "#6A3529",
        "teeth": "#E8E4D6", "mouth": "#4A1C18", "claw": "#3B2B24", "eye": "#FF5A1E",
        "core": "#E2481E", "core_hot": "#FFD27A", "core_edge": "#9E2A16",
    },
    # Linha de cima da folha: verde musgo com placas claras na coluna.
    "primigenius_resurrected": {
        "hide": "#5E7149", "hide_dark": "#4A5A3A", "hide_light": "#728A5A",
        "back": "#3F4C33", "back_dark": "#323D29", "back_light": "#4F5E40",
        "belly": "#8C5A44", "belly_dark": "#704534", "belly_light": "#A06A50",
        "plate": "#B7AD95", "plate_edge": "#CFC6AE", "plate_dark": "#9C937C",
        "fin": "#8A4B3A", "fin_dark": "#6A3529",
        "teeth": "#E8E4D6", "mouth": "#4A1C18", "claw": "#3B2B24", "eye": "#FF5A1E",
        "core": "#E2481E", "core_hot": "#FFD27A", "core_edge": "#9E2A16",
    },
}
# Etapa C (escala aprovada): Yoju com 6 blocos de altura / Honju com 9.
MODEL_SCALE = 6.0 / 4.6
BASE = "primigenius"


def build():
    m = Model(BASE, 128, MODEL_SCALE)
    m.bone("root", None, [0, 0, 0], [])
    m.bone("body", "root", [0, 24, 0], [
        Cube([-11, 14, -2], [22, 14, 16], "hide", up="back"),                 # quadril
        Cube([-10, 28, 0], [20, 18, 14], "hide", up="back", south="back"),     # lombar
        Cube([-14, 16, -16], [28, 26, 18], "hide", north="belly", up="back"),  # peito/barriga
        Cube([-17, 40, -18], [34, 16, 20], "hide", up="back"),                # ombros
        Cube([-12, 56, -14], [24, 8, 16], "back"),                            # corcunda
        Cube([-10, 16, -17], [20, 16, 1], "belly"),                           # placa da barriga
        Cube([-5, 18, -17.5], [10, 9, 0.5], "core"),                          # NUCLEO (barriga, abaixo da cabeca)
        Cube([-2, 64, -12], [4, 7, 4], "plate"),                              # placas da coluna
        Cube([-2, 64, -6], [4, 8, 4], "plate"),
        Cube([-2, 62, -1], [4, 6, 4], "plate"),
        Cube([-2, 46, 4], [4, 6, 4], "plate"),
        Cube([-2, 46, 9], [4, 5, 4], "plate"),
    ])
    m.bone("head", "body", [0, 40, -18], [
        Cube([-7, 34, -30], [14, 10, 13], "hide", up="back"),                 # cranio
        Cube([-7.5, 42, -30], [15, 2, 8], "plate"),                           # crista sobre os olhos
        Cube([-5, 32, -42], [10, 7, 12], "hide", up="back"),                  # focinho
        Cube([-4.5, 30.5, -42], [9, 1.5, 11], "teeth"),                       # dentes de cima
        Cube([-6.5, 40, -30.5], [2, 1.5, 0.5], "eye"),
        Cube([4.5, 40, -30.5], [2, 1.5, 0.5], "eye"),
    ])
    m.bone("jaw", "head", [0, 33, -30], [
        Cube([-6, 26, -41], [12, 5, 12], "hide", up="mouth"),
        Cube([-5, 31, -40.5], [10, 1.5, 10], "teeth"),                        # dentes de baixo
    ])
    m.pair("fin_{side}", "head", [-7, 38, -26], [Cube([-13, 34, -29], [6, 9, 1], "fin")])
    m.pair("arm_{side}", "body", [-17, 48, -8], [
        Cube([-27, 30, -16], [11, 20, 15], "hide", up="back"),
        Cube([-25, 46, -15], [9, 8, 13], "back"),                             # deltoide
    ])
    m.pair("forearm_{side}", "arm_{side}", [-21, 32, -9], [
        Cube([-27, 8, -21], [12, 24, 13], "hide", up="back"),
        Cube([-27.5, 14, -21.5], [11, 8, 1], "plate"),                        # placa do antebraco
        Cube([-28, 2, -25], [13, 7, 14], "hide"),                             # mao
        Cube([-28, 0, -28], [13, 4, 4], "claw"),                              # garras
    ])
    m.pair("leg_{side}", "body", [-9, 22, 4], [
        Cube([-17, 10, -1], [11, 14, 13], "hide"),
        Cube([-16, 3, -2], [9, 9, 11], "hide"),
        Cube([-16.5, 0, -6], [10, 4, 14], "hide"),
        Cube([-16.5, 0, -8], [10, 3, 2], "claw"),
    ])
    m.bone("tail_1", "body", [0, 22, 12], [Cube([-8, 14, 12], [16, 13, 12], "hide", up="back"),
                                           Cube([-2, 27, 14], [4, 4, 6], "plate")])
    m.bone("tail_2", "tail_1", [0, 18, 24], [Cube([-6, 9, 24], [12, 10, 12], "hide", up="back"),
                                             Cube([-1.5, 19, 26], [3, 3, 6], "plate")])
    m.bone("tail_3", "tail_2", [0, 13, 36], [Cube([-4, 5, 36], [8, 8, 12], "hide", up="back"),
                                             Cube([-1, 13, 38], [2, 2, 6], "plate")])
    m.bone("tail_4", "tail_3", [0, 8, 48], [Cube([-2.5, 2, 48], [5, 6, 10], "hide", up="back")])
    return m


def fix_parents(model):
    """pair() usa o mesmo nome de pai com {side}: troca pelo lado certo."""
    fixed = []
    for name, parent, pivot, cubes in model.bones:
        if parent and "{side}" in parent:
            parent = parent.format(side="left" if name.endswith("left") else "right")
        fixed.append((name, parent, pivot, cubes))
    model.bones = fixed


def animations(species):
    p = species + "."
    tail = ["tail_1", "tail_2", "tail_3", "tail_4"]
    walk = {
        "body": {"position": kf((0, [0, 0, 0]), (0.4, [0, 1, 0]), (0.8, [0, 0, 0]), (1.2, [0, 1, 0]),
                                (1.6, [0, 0, 0])),
                 "rotation": kf((0, [0, 0, 2]), (0.8, [0, 0, -2]), (1.6, [0, 0, 2]))},
        "arm_left": {"rotation": kf((0, [18, 0, 0]), (0.8, [-18, 0, 0]), (1.6, [18, 0, 0]))},
        "arm_right": {"rotation": kf((0, [-18, 0, 0]), (0.8, [18, 0, 0]), (1.6, [-18, 0, 0]))},
        "leg_left": {"rotation": kf((0, [-15, 0, 0]), (0.8, [15, 0, 0]), (1.6, [-15, 0, 0]))},
        "leg_right": {"rotation": kf((0, [15, 0, 0]), (0.8, [-15, 0, 0]), (1.6, [15, 0, 0]))},
    }
    for i, name in enumerate(tail):
        sway = 6 + i * 2
        walk[name] = {"rotation": kf((0, [0, sway, 0]), (0.8, [0, -sway, 0]), (1.6, [0, sway, 0]))}
    # SLAM: preparo de 20 ticks (1,0 s) -> bracos acima da cabeca; impacto em 1,0 s; volta ate 1,5 s.
    slam = {
        "body": {"rotation": kf((0, [0, 0, 0]), (0.8, [14, 0, 0]), (1.0, [-18, 0, 0]), (1.5, [0, 0, 0])),
                 "position": kf((0, [0, 0, 0]), (0.8, [0, 2, 2]), (1.0, [0, -2, -2]), (1.5, [0, 0, 0]))},
        "arm_left": {"rotation": kf((0, [0, 0, 0]), (0.8, [-150, 0, -10]), (1.0, [15, 0, 0]),
                                    (1.5, [0, 0, 0]))},
        "arm_right": {"rotation": kf((0, [0, 0, 0]), (0.8, [-150, 0, 10]), (1.0, [15, 0, 0]),
                                     (1.5, [0, 0, 0]))},
        "jaw": {"rotation": kf((0, [0, 0, 0]), (0.8, [25, 0, 0]), (1.1, [0, 0, 0]))},
    }
    # CHARGE: preparo de 15 ticks (0,75 s) abaixando a cabeca; avanco ativo ate 1,75 s; volta em 1,9 s.
    charge = {
        "body": {"rotation": kf((0, [0, 0, 0]), (0.75, [-15, 0, 0]), (1.75, [-15, 0, 0]), (1.9, [0, 0, 0])),
                 "position": kf((0, [0, 0, 0]), (0.75, [0, -3, 2]), (1.75, [0, -3, 0]), (1.9, [0, 0, 0]))},
        "head": {"rotation": kf((0, [0, 0, 0]), (0.75, [-12, 0, 0]), (1.75, [-12, 0, 0]), (1.9, [0, 0, 0]))},
        "arm_left": {"rotation": kf((0, [0, 0, 0]), (0.75, [25, 0, 0]), (1.0, [-25, 0, 0]), (1.25, [25, 0, 0]),
                                    (1.5, [-25, 0, 0]), (1.75, [25, 0, 0]), (1.9, [0, 0, 0]))},
        "arm_right": {"rotation": kf((0, [0, 0, 0]), (0.75, [25, 0, 0]), (1.0, [25, 0, 0]), (1.25, [-25, 0, 0]),
                                     (1.5, [25, 0, 0]), (1.75, [-25, 0, 0]), (1.9, [0, 0, 0]))},
    }
    return {"format_version": "1.8.0", "animations": {
        p + "movement.idle": {"loop": True, "animation_length": 2.5, "bones": {
            "body": {"position": kf((0, [0, 0, 0]), (1.25, [0, -0.5, 0]), (2.5, [0, 0, 0]))},
            "head": {"rotation": kf((0, [0, 0, 0]), (1.25, [3, 0, 0]), (2.5, [0, 0, 0]))},
            "jaw": {"rotation": kf((0, [0, 0, 0]), (1.25, [6, 0, 0]), (2.5, [0, 0, 0]))},
            "fin_left": {"rotation": kf((0, [0, 0, 0]), (1.25, [0, -10, 0]), (2.5, [0, 0, 0]))},
            "fin_right": {"rotation": kf((0, [0, 0, 0]), (1.25, [0, 10, 0]), (2.5, [0, 0, 0]))},
            "tail_3": {"rotation": kf((0, [0, 0, 0]), (1.25, [0, 5, 0]), (2.5, [0, 0, 0]))},
        }},
        p + "movement.walk": {"loop": True, "animation_length": 1.6, "bones": walk},
        p + "action.slam": {"animation_length": 1.5, "bones": slam},
        p + "action.charge": {"animation_length": 1.9, "bones": charge},
        p + "reaction.hurt": {"animation_length": 0.35, "bones": {
            "body": {"rotation": kf((0, [0, 0, 0]), (0.1, [8, 0, 0]), (0.35, [0, 0, 0])),
                     "position": kf((0, [0, 0, 0]), (0.1, [0, 0, 2]), (0.35, [0, 0, 0]))},
            "jaw": {"rotation": kf((0, [0, 0, 0]), (0.1, [20, 0, 0]), (0.35, [0, 0, 0]))}}},
        p + "overlay.breathe": {"loop": True, "animation_length": 3.0, "bones": {
            "fin_left": {"rotation": kf((0, [0, 0, 0]), (1.5, [0, -6, 0]), (3.0, [0, 0, 0]))},
            "fin_right": {"rotation": kf((0, [0, 0, 0]), (1.5, [0, 6, 0]), (3.0, [0, 0, 0]))}}},
    }}


REQUIRED = ["movement.idle", "movement.walk", "action.slam", "action.charge", "reaction.hurt",
            "overlay.breathe"]

if __name__ == "__main__":
    model = build()
    fix_parents(model)
    regions = model.layout()
    base_anims = art.scale_animations(animations(BASE), MODEL_SCALE)
    for species, palette in PALETTES.items():
        anims = art.rename_animations(base_anims, BASE, species)
        art.validate(model, anims, species, REQUIRED, max_bones=35)
        if (art.ASSETS / f"meshes/{species}.json").exists():
            # Especie com malha do Meshy (rig_primigenius_mesh.py): so as animacoes; geo e textura sao da malha.
            (art.ASSETS / f"animations/entity/{species}.animation.json").write_text(
                json.dumps(anims, indent=2) + "\n", encoding="utf-8")
            print(f"{species}: malha do Meshy, so animacoes regeradas")
            continue
        art.write(species, model.geo(species, regions), anims, model.texture(palette, regions))
        print(f"{species}: {len(model.bones)} ossos, {sum(len(b[3]) for b in model.bones)} cubos, "
              f"{len(regions)} regioes")
