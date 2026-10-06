#!/usr/bin/env python3
"""Arte final (v1) do Primigenius Honju (Tita Bruto, marrom) e do Honju ressurgido (roxo): um modelo, um
esqueleto, duas texturas.

Folha aprovada: Modelo 01 - Tita Bruto. Bipede curvado e massivo, ombros com espinhos, dois chifres grandes, nucleo
em brasa no peito e brilho no pescoco (orgao de chamar o bando), bracos ate o chao, cauda grossa curvada na ponta.
Ressurgido: mesma criatura em roxo, chifres e espinhos carmim, fissuras em brasa, nucleo mais forte.
Medidas: 3,5 x 5,5 blocos (hitbox do JSON do revivido atualizada junto). Guia de arte: grande, ate 35 ossos,
textura 256x256 (mini-chefe). Animacoes: genericas + slam, charge, bite (tempos do JSON) e roar (invocar o bando, M16).
Uso: python3 tools/art/build_primigenius_honju.py
"""
import kaiju_art as art
from kaiju_art import Cube, Model, kf

PALETTES = {
    "primigenius_honju": {
        "hide": "#6B4632", "hide_dark": "#4E3224", "hide_light": "#80583F",
        "back": "#4A3346", "back_dark": "#3A2837", "back_light": "#5C4157",
        "belly": "#5E3E2C", "belly_dark": "#4A2F21", "belly_light": "#714B36",
        "plate": "#9C8670", "plate_edge": "#C9B79D", "plate_dark": "#7C6855",
        "horn": "#B8792E", "horn_tip": "#F0C070", "spike": "#B8792E", "spike_tip": "#EBD3A8",
        "fin": "#8A4B3A", "fin_dark": "#6A3529",
        "teeth": "#EDE3CC", "mouth": "#3A1414", "claw": "#1F1B1A", "eye": "#FF7A1E",
        "core": "#F05A1A", "core_hot": "#FFE08A", "core_edge": "#B8241A",
    },
    "primigenius_revived": {
        "hide": "#6D4D6B", "hide_dark": "#4C324C", "hide_light": "#80607E",
        "back": "#3D2738", "back_dark": "#2C1C29", "back_light": "#4E3449",
        "belly": "#5A3E58", "belly_dark": "#463046", "belly_light": "#6C4E6A",
        "plate": "#7A6352", "plate_edge": "#E8D2BC", "plate_dark": "#5E4A3D",
        "horn": "#7E1216", "horn_tip": "#E3262B", "spike": "#7E1216", "spike_tip": "#E3262B",
        "fin": "#8E1418", "fin_dark": "#5E0C10",
        "teeth": "#EDE3CC", "mouth": "#3A1414", "claw": "#1A1A1A", "eye": "#FF8A2A",
        "core": "#FF7A1E", "core_hot": "#FFF6C8", "core_edge": "#E3262B",
        "crack": "#E8401A", "crack_hot": "#FFB25A",
    },
}
# Etapa C (escala aprovada): Yoju com 6 blocos de altura / Honju com 9.
MODEL_SCALE = 9.0 / 5.5
BASE = "primigenius_honju"


def build():
    m = Model(BASE, 256, MODEL_SCALE)
    m.bone("root", None, [0, 0, 0], [])
    body = [
        Cube([-15, 20, -6], [30, 14, 18], "hide", up="back"),                 # quadril
        Cube([-19, 32, -12], [38, 22, 22], "hide", north="belly", up="back"),  # tronco
        Cube([-24, 50, -14], [48, 14, 24], "hide", up="back"),                # ombros
        Cube([-12, 34, -13], [24, 16, 1], "belly"),                           # placa do peito
        Cube([-6, 36, -13.5], [12, 10, 0.5], "core"),                         # NUCLEO (peito)
        Cube([-8, 48, -24], [16, 12, 12], "hide", up="back"),                 # pescoco
        Cube([-8.5, 48, -24.5], [17, 5, 9], "core"),                          # brilho do pescoco (frente e lados)
    ]
    for z, height in ((-10, 9), (-4, 10), (2, 9), (8, 7)):                    # espinhos da coluna
        body.append(Cube([-2, 64, z], [4, height, 4], "spike"))
    for x in (-22, 18):                                                       # espinhos dos ombros
        body.append(Cube([x, 64, -8], [4, 7, 4], "spike"))
        body.append(Cube([x + (-2 if x < 0 else 2), 62, 0], [4, 6, 4], "spike"))
    m.bone("body", "root", [0, 34, 0], body)
    m.bone("head", "body", [0, 58, -16], [
        Cube([-9, 54, -34], [18, 14, 16], "hide", up="back"),                 # cranio (projetado a frente)
        Cube([-7, 52, -44], [14, 9, 10], "hide", up="back"),                  # focinho
        Cube([-9.5, 66, -35], [19, 3, 10], "plate"),                          # placa da testa
        Cube([-7, 62, -34.5], [3, 1.5, 0.5], "eye"),
        Cube([4, 62, -34.5], [3, 1.5, 0.5], "eye"),
        Cube([-6.5, 50.5, -44], [13, 1.5, 10], "teeth"),                      # dentes de cima
    ])
    m.bone("jaw", "head", [0, 54, -34], [
        Cube([-8, 46, -43], [16, 6, 13], "hide", up="mouth"),
        Cube([-7, 52, -42.5], [14, 1.5, 10], "teeth"),                        # dentes de baixo
    ])
    m.pair("horn_{side}", "head", [-9, 64, -26], [
        Cube([-16, 62, -29], [7, 5, 6], "horn"),
        Cube([-20, 66, -28.5], [5, 10, 5], "horn"),
        Cube([-19, 76, -28], [4, 6, 4], "horn"),
        Cube([-17, 81, -27.5], [3, 3, 3], "horn"),
    ])
    m.pair("arm_{side}", "body", [-22, 56, -4], [
        Cube([-34, 36, -12], [13, 22, 16], "hide", up="back"),
        Cube([-33, 54, -11], [11, 8, 14], "back"),                            # deltoide
    ])
    m.pair("forearm_{side}", "arm_{side}", [-27, 38, -6], [
        Cube([-35, 12, -16], [15, 26, 16], "hide", up="back"),
        Cube([-34.5, 18, -16.5], [13, 12, 1], "plate"),
        Cube([-36, 4, -20], [16, 9, 17], "hide"),                             # mao
        Cube([-36, 0, -24], [16, 5, 4], "claw"),                              # garras
    ])
    m.pair("leg_{side}", "body", [-10, 28, 0], [
        Cube([-20, 14, -6], [14, 16, 16], "hide"),
        Cube([-19, 4, -5], [12, 11, 13], "hide"),
        Cube([-19.5, 0, -10], [13, 5, 17], "hide"),
        Cube([-19.5, 0, -12], [13, 4, 3], "claw"),
    ])
    m.bone("tail_1", "body", [0, 26, 10], [Cube([-9, 18, 12], [18, 14, 13], "hide", up="back"),
                                           Cube([-2, 32, 14], [4, 6, 4], "spike")])
    m.bone("tail_2", "tail_1", [0, 22, 25], [Cube([-7, 14, 25], [14, 11, 13], "hide", up="back"),
                                             Cube([-1.5, 25, 27], [3, 5, 3], "spike")])
    m.bone("tail_3", "tail_2", [0, 18, 38], [Cube([-5, 12, 38], [10, 8, 12], "hide", up="back"),
                                             Cube([-1, 20, 40], [2, 4, 3], "spike")])
    m.bone("tail_4", "tail_3", [0, 16, 50], [Cube([-3, 14, 50], [6, 6, 8], "hide", up="back"),
                                             Cube([-2.5, 20, 54], [5, 8, 4], "hide", up="back")])
    return m


def fix_parents(model):
    fixed = []
    for name, parent, pivot, cubes in model.bones:
        if parent and "{side}" in parent:
            parent = parent.format(side="left" if name.endswith("left") else "right")
        fixed.append((name, parent, pivot, cubes))
    model.bones = fixed


def animations(species):
    p = species + "."
    walk = {
        "body": {"position": kf((0, [0, 0, 0]), (0.45, [0, 1.2, 0]), (0.9, [0, 0, 0]), (1.35, [0, 1.2, 0]),
                                (1.8, [0, 0, 0])),
                 "rotation": kf((0, [0, 0, 2.5]), (0.9, [0, 0, -2.5]), (1.8, [0, 0, 2.5]))},
        "leg_left": {"rotation": kf((0, [-18, 0, 0]), (0.9, [18, 0, 0]), (1.8, [-18, 0, 0]))},
        "leg_right": {"rotation": kf((0, [18, 0, 0]), (0.9, [-18, 0, 0]), (1.8, [18, 0, 0]))},
        "arm_left": {"rotation": kf((0, [12, 0, 0]), (0.9, [-12, 0, 0]), (1.8, [12, 0, 0]))},
        "arm_right": {"rotation": kf((0, [-12, 0, 0]), (0.9, [12, 0, 0]), (1.8, [-12, 0, 0]))},
    }
    for i, name in enumerate(["tail_1", "tail_2", "tail_3", "tail_4"]):
        sway = 5 + i * 3
        walk[name] = {"rotation": kf((0, [0, sway, 0]), (0.9, [0, -sway, 0]), (1.8, [0, sway, 0]))}
    slam = {  # preparo 20 ticks: bracos sobem; impacto em 1,0 s; volta ate 1,5 s
        "body": {"rotation": kf((0, [0, 0, 0]), (0.8, [12, 0, 0]), (1.0, [-20, 0, 0]), (1.5, [0, 0, 0])),
                 "position": kf((0, [0, 0, 0]), (0.8, [0, 3, 2]), (1.0, [0, -3, -2]), (1.5, [0, 0, 0]))},
        "arm_left": {"rotation": kf((0, [0, 0, 0]), (0.8, [-160, 0, -12]), (1.0, [20, 0, 0]), (1.5, [0, 0, 0]))},
        "arm_right": {"rotation": kf((0, [0, 0, 0]), (0.8, [-160, 0, 12]), (1.0, [20, 0, 0]),
                                     (1.5, [0, 0, 0]))},
        "jaw": {"rotation": kf((0, [0, 0, 0]), (0.8, [30, 0, 0]), (1.1, [0, 0, 0]))},
    }
    charge = {  # preparo 15 ticks: chifres para a frente; investida ativa ate 1,75 s
        "body": {"rotation": kf((0, [0, 0, 0]), (0.75, [-20, 0, 0]), (1.75, [-20, 0, 0]), (1.9, [0, 0, 0])),
                 "position": kf((0, [0, 0, 0]), (0.75, [0, -4, 3]), (1.75, [0, -4, 0]), (1.9, [0, 0, 0]))},
        "head": {"rotation": kf((0, [0, 0, 0]), (0.75, [-18, 0, 0]), (1.75, [-18, 0, 0]), (1.9, [0, 0, 0]))},
        "arm_left": {"rotation": kf((0, [0, 0, 0]), (0.75, [30, 0, 0]), (1.0, [-20, 0, 0]), (1.25, [30, 0, 0]),
                                    (1.5, [-20, 0, 0]), (1.75, [30, 0, 0]), (1.9, [0, 0, 0]))},
        "arm_right": {"rotation": kf((0, [0, 0, 0]), (0.75, [30, 0, 0]), (1.0, [30, 0, 0]), (1.25, [-20, 0, 0]),
                                     (1.5, [30, 0, 0]), (1.75, [-20, 0, 0]), (1.9, [0, 0, 0]))},
        "leg_left": {"rotation": kf((0.75, [0, 0, 0]), (1.0, [-25, 0, 0]), (1.25, [25, 0, 0]),
                                    (1.5, [-25, 0, 0]), (1.75, [0, 0, 0]))},
        "leg_right": {"rotation": kf((0.75, [0, 0, 0]), (1.0, [25, 0, 0]), (1.25, [-25, 0, 0]),
                                     (1.5, [25, 0, 0]), (1.75, [0, 0, 0]))},
    }
    bite = {  # preparo 6 ticks: bote em 0,30 s; fim em 0,40 s
        "head": {"rotation": kf((0, [0, 0, 0]), (0.18, [12, 0, 0]), (0.3, [-15, 0, 0]), (0.4, [0, 0, 0])),
                 "position": kf((0, [0, 0, 0]), (0.18, [0, 0, 1]), (0.3, [0, 0, -4]), (0.4, [0, 0, 0]))},
        "jaw": {"rotation": kf((0, [0, 0, 0]), (0.18, [40, 0, 0]), (0.3, [0, 0, 0]), (0.4, [0, 0, 0]))},
        "body": {"rotation": kf((0, [0, 0, 0]), (0.18, [5, 0, 0]), (0.3, [-8, 0, 0]), (0.4, [0, 0, 0]))},
    }
    roar = {  # invocar o bando (M16): cabeca para cima, boca aberta, bracos abertos, 2,0 s
        "head": {"rotation": kf((0, [0, 0, 0]), (0.4, [30, 0, 0]), (1.6, [30, 0, 0]), (2.0, [0, 0, 0]))},
        "jaw": {"rotation": kf((0, [0, 0, 0]), (0.4, [45, 0, 0]), (1.6, [45, 0, 0]), (2.0, [0, 0, 0]))},
        "body": {"rotation": kf((0, [0, 0, 0]), (0.4, [10, 0, 0]), (1.6, [10, 0, 0]), (2.0, [0, 0, 0]))},
        "arm_left": {"rotation": kf((0, [0, 0, 0]), (0.4, [-20, 0, -35]), (1.6, [-20, 0, -35]), (2.0, [0, 0, 0]))},
        "arm_right": {"rotation": kf((0, [0, 0, 0]), (0.4, [-20, 0, 35]), (1.6, [-20, 0, 35]),
                                     (2.0, [0, 0, 0]))},
    }
    return {"format_version": "1.8.0", "animations": {
        p + "movement.idle": {"loop": True, "animation_length": 2.5, "bones": {
            "body": {"position": kf((0, [0, 0, 0]), (1.25, [0, -0.6, 0]), (2.5, [0, 0, 0]))},
            "head": {"rotation": kf((0, [0, 0, 0]), (1.25, [-3, 0, 0]), (2.5, [0, 0, 0]))},
            "jaw": {"rotation": kf((0, [0, 0, 0]), (1.25, [8, 0, 0]), (2.5, [0, 0, 0]))},
            "tail_4": {"rotation": kf((0, [0, 0, 0]), (1.25, [0, 8, 0]), (2.5, [0, 0, 0]))},
        }},
        p + "movement.walk": {"loop": True, "animation_length": 1.8, "bones": walk},
        p + "action.slam": {"animation_length": 1.5, "bones": slam},
        p + "action.charge": {"animation_length": 1.9, "bones": charge},
        p + "action.bite": {"animation_length": 0.4, "bones": bite},
        p + "action.roar": {"animation_length": 2.0, "bones": roar},
        p + "reaction.hurt": {"animation_length": 0.35, "bones": {
            "body": {"rotation": kf((0, [0, 0, 0]), (0.1, [7, 0, 0]), (0.35, [0, 0, 0])),
                     "position": kf((0, [0, 0, 0]), (0.1, [0, 0, 2]), (0.35, [0, 0, 0]))},
            "jaw": {"rotation": kf((0, [0, 0, 0]), (0.1, [20, 0, 0]), (0.35, [0, 0, 0]))}}},
        p + "overlay.breathe": {"loop": True, "animation_length": 3.0, "bones": {
            "jaw": {"rotation": kf((0, [0, 0, 0]), (1.5, [3, 0, 0]), (3.0, [0, 0, 0]))}}},
    }}


REQUIRED = ["movement.idle", "movement.walk", "action.slam", "action.charge", "action.bite", "action.roar",
            "reaction.hurt", "overlay.breathe"]

if __name__ == "__main__":
    model = build()
    fix_parents(model)
    regions = model.layout()
    base_anims = art.scale_animations(animations(BASE), MODEL_SCALE)
    for species, palette in PALETTES.items():
        anims = art.rename_animations(base_anims, BASE, species)
        art.validate(model, anims, species, REQUIRED, max_bones=35)
        art.write(species, model.geo(species, regions), anims, model.texture(palette, regions))
        print(f"{species}: {len(model.bones)} ossos, {sum(len(b[3]) for b in model.bones)} cubos, "
              f"{len(regions)} regioes")
