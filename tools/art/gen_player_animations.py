#!/usr/bin/env python3
"""Animacoes PAL do jogador (M9): leve, pesado, bloqueio e esquiva, como poses simples de 3 a 5 keyframes (Fase 4).

Os tempos do leve e do pesado saem do JSON da arma base (combat_knife): duracao = duration_ticks, pico do golpe =
impact_tick. Assim o golpe visual cai no tick em que o servidor aplica o dano (M10), e mudar o JSON muda a animacao
na proxima geracao. Esquiva e bloqueio ainda nao tem JSON (chegam com o CombatProfile do M10): tempos fixos aqui.
Formato igual ao do PT6 (aprovado). Ossos da PAL: head, torso, right_arm, left_arm, right_leg, left_leg.
Uso: python3 tools/art/gen_player_animations.py
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
WEAPON = ROOT / "src/main/resources/data/kn8/kn8/weapon/combat_knife.json"
RIFLE = ROOT / "src/main/resources/data/kn8/kn8/weapon/rifle.json"
OUT = ROOT / "src/main/resources/assets/kn8/player_animations/combat.json"
TICK = 1 / 20
DODGE_TICKS = 10
BLOCK_RAISE_TICKS = 4


def keys(*frames):
    return {f"{round(t, 3)}": {"vector": v} for t, v in frames}


def light(action):
    end, hit = action["duration_ticks"] * TICK, action["impact_tick"] * TICK
    wind = hit / 2
    return {"animation_length": round(end, 3), "bones": {
        "right_arm": {"rotation": keys((0, [0, 0, 0]), (wind, [-110, 0, 25]), (hit, [20, 0, -15]),
                                       (end, [0, 0, 0]))},
        "torso": {"rotation": keys((0, [0, 0, 0]), (wind, [0, 20, 0]), (hit, [0, -15, 0]), (end, [0, 0, 0]))},
    }}


def heavy(action):
    end, hit = action["duration_ticks"] * TICK, action["impact_tick"] * TICK
    raise_t = hit * 0.7
    return {"animation_length": round(end, 3), "bones": {
        "right_arm": {"rotation": keys((0, [0, 0, 0]), (raise_t, [-165, 0, 10]), (hit, [-20, 0, 0]),
                                       (end, [0, 0, 0]))},
        "left_arm": {"rotation": keys((0, [0, 0, 0]), (raise_t, [-165, 0, -10]), (hit, [-20, 0, 0]),
                                      (end, [0, 0, 0]))},
        "torso": {"rotation": keys((0, [0, 0, 0]), (raise_t, [-12, 0, 0]), (hit, [18, 0, 0]), (end, [0, 0, 0]))},
    }}


def special_slam(special):
    """0.5: ataque especial ground_slam (machado): ergue a arma com as duas maos, agacha e crava no chao no tick de
    impacto do JSON (special.impact_tick), segura um instante e volta."""
    end, hit = special["duration_ticks"] * TICK, special["impact_tick"] * TICK
    raise_t = hit * 0.65
    hold = hit + (end - hit) * 0.4
    up, down = [-175, 0, 12], [-35, 0, 5]
    return {"animation_length": round(end, 3), "bones": {
        "right_arm": {"rotation": keys((0, [0, 0, 0]), (raise_t, up), (hit, down), (hold, down), (end, [0, 0, 0]))},
        "left_arm": {"rotation": keys((0, [0, 0, 0]), (raise_t, [-175, 0, -12]), (hit, [-35, 0, -5]),
                                      (hold, [-35, 0, -5]), (end, [0, 0, 0]))},
        "torso": {"rotation": keys((0, [0, 0, 0]), (raise_t, [-15, 0, 0]), (hit, [30, 0, 0]), (hold, [30, 0, 0]),
                                   (end, [0, 0, 0]))},
        "right_leg": {"rotation": keys((0, [0, 0, 0]), (hit, [-30, 0, 0]), (hold, [-30, 0, 0]), (end, [0, 0, 0]))},
        "left_leg": {"rotation": keys((0, [0, 0, 0]), (hit, [20, 0, 0]), (hold, [20, 0, 0]), (end, [0, 0, 0]))},
    }}


def special_slash(special):
    """0.6-D: corte a distancia (slash_wave, espada do Hoshina): leva a lamina ao ombro esquerdo e corta na
    diagonal ate a direita no tick de impacto do JSON, com o tronco girando junto."""
    end, hit = special["duration_ticks"] * TICK, special["impact_tick"] * TICK
    wind = hit * 0.6
    follow = hit + (end - hit) * 0.35
    return {"animation_length": round(end, 3), "bones": {
        "right_arm": {"rotation": keys((0, [0, 0, 0]), (wind, [-130, 40, 40]), (hit, [-70, -30, -50]),
                                       (follow, [-50, -40, -40]), (end, [0, 0, 0]))},
        "left_arm": {"rotation": keys((0, [0, 0, 0]), (wind, [-30, 0, -10]), (hit, [-20, 0, -25]),
                                      (end, [0, 0, 0]))},
        "torso": {"rotation": keys((0, [0, 0, 0]), (wind, [0, 35, 0]), (hit, [5, -30, 0]), (end, [0, 0, 0]))},
        "right_leg": {"rotation": keys((0, [0, 0, 0]), (hit, [-20, 0, 0]), (end, [0, 0, 0]))},
    }}


def shoot(action, body_kick=1.0):
    """M10b: mira com os dois bracos e coice no tick de impacto do JSON do rifle. 0.5.0-D: o coice do corpo escala
    com o recoil.body_kick do perfil (pistola na mao so: o braco sobe mais)."""
    end, hit = action["duration_ticks"] * TICK, action["impact_tick"] * TICK
    aim = [-90, -8, 0]
    recoil = [-90 - 25 * body_kick, -8, 0]
    return {"animation_length": round(end, 3), "bones": {
        "right_arm": {"rotation": keys((0, aim), (hit, recoil), (end, aim))},
        "left_arm": {"rotation": keys((0, [-85, 25, 0]), (hit, [-105, 25, 0]), (end, [-85, 25, 0]))},
        "head": {"rotation": keys((0, [0, 0, 0]), (hit, [-4 * body_kick, 0, 0]), (end, [0, 0, 0]))},
        "torso": {"rotation": keys((0, [0, 0, 0]), (hit, [-3 * body_kick, 0, 0]), (end, [0, 0, 0]))},
    }}


def dodge():
    end = DODGE_TICKS * TICK
    low = end * 0.3
    return {"animation_length": round(end, 3), "bones": {
        "torso": {"rotation": keys((0, [0, 0, 0]), (low, [20, 0, 15]), (end, [0, 0, 0]))},
        "right_leg": {"rotation": keys((0, [0, 0, 0]), (low, [-30, 0, 0]), (end, [0, 0, 0]))},
        "left_leg": {"rotation": keys((0, [0, 0, 0]), (low, [15, 0, 0]), (end, [0, 0, 0]))},
    }}


def dash():
    """0.1-B: dash (8 ticks, como combat.dashDurationTicks): corpo inclinado para a frente, bracos para tras."""
    end = 8 * TICK
    mid = end * 0.4
    return {"animation_length": round(end, 3), "bones": {
        "torso": {"rotation": keys((0, [0, 0, 0]), (mid, [-25, 0, 0]), (end, [0, 0, 0]))},
        "right_arm": {"rotation": keys((0, [0, 0, 0]), (mid, [40, 0, 10]), (end, [0, 0, 0]))},
        "left_arm": {"rotation": keys((0, [0, 0, 0]), (mid, [40, 0, -10]), (end, [0, 0, 0]))},
        "right_leg": {"rotation": keys((0, [0, 0, 0]), (mid, [-35, 0, 0]), (end, [0, 0, 0]))},
        "left_leg": {"rotation": keys((0, [0, 0, 0]), (mid, [30, 0, 0]), (end, [0, 0, 0]))},
    }}


def charge():
    """0.1-B: postura de carga do ataque carregado; segura ate o golpe (o golpe pesado substitui a animacao)."""
    up = 6 * TICK
    return {"loop": "hold_on_last_frame", "animation_length": round(up, 3), "bones": {
        "right_arm": {"rotation": keys((0, [0, 0, 0]), (up, [-150, 20, 15]))},
        "left_arm": {"rotation": keys((0, [0, 0, 0]), (up, [-40, -20, 0]))},
        "torso": {"rotation": keys((0, [0, 0, 0]), (up, [0, 25, 0]))},
    }}


def block():
    up = BLOCK_RAISE_TICKS * TICK
    # Sobe a guarda e SEGURA a ultima pose ate o servidor mandar parar (AnimationBridge.stopPlayer).
    return {"loop": "hold_on_last_frame", "animation_length": round(up, 3), "bones": {
        "right_arm": {"rotation": keys((0, [0, 0, 0]), (up, [-75, -30, 0]))},
        "left_arm": {"rotation": keys((0, [0, 0, 0]), (up, [-75, 30, 0]))},
        "torso": {"rotation": keys((0, [0, 0, 0]), (up, [8, 0, 0]))},
    }}


WEAPONS = ROOT / "src/main/resources/data/kn8/kn8/weapon"
PROFILES = ROOT / "src/main/resources/data/kn8/kn8/weapon_profile"

# Poses de cada familia (0.5.0-D5, referencias do Miguel conferidas em jogo de frente, de lado e de costas). Uma
# tabela so para o jogador (PAL) e para os soldados especiais (GeckoLib): to_pal/to_gecko convertem. Chaves neutras:
#   torso       inclinacao do corpo inteiro acima do quadril [x para a frente, y giro, 0];
#   body_pos    desce/sobe o corpo inteiro (px);
#   head        cabeca em relacao ao tronco (com o tronco inclinado, x negativo olha para a frente);
#   right_leg / left_leg   pernas em relacao ao chao (o conversor desconta a inclinacao na PAL);
#   right_arm / left_arm   bracos em relacao ao tronco; X negativo = para a frente, Z positivo = direito para fora;
#   right_item / left_item rotacao da arma em volta do punho (ordem e sinais da PAL; o SoldierRenderer faz igual);
#   right_item_pos / left_item_pos  desliza a arma na mao (px, espaco do braco: +X esquerda do corpo, +Z para tras).
# Laminas do Hoshina: pegada invertida com a lamina colada no antebraco, por fora (item [-120, 0, 0] e 4 px atras):
# de frente some atras do braco, de costas e um traco (fotos do Miguel). Machado: atravessado na frente com a cabeca
# baixa do lado direito, a mao direita perto da cabeca e o cabo passando pela esquerda (fotos "Battle Axe"); correndo
# so a mao direita, o machado arrastando atras.
# 0.5.0-D6 (videos do Miguel, docs/referencias_video): espadas com as laminas para FORA, saindo dos punhos para os
# lados e um pouco para baixo (BLADE_OUT), bracos abertos parado e maos baixas a frente andando; machado parado com a
# cabeca no chao a frente e a direita e o cabo subindo para a esquerda, andando com a mao direita so e a cabeca
# arrastando a frente; o saque ergue o machado atravessado no peito ("draw_via") e baixa para a postura.
ALONG_FOREARM = [-120, 0, 0]
BEHIND_FOREARM = [0, 0, 4]
BLADE_OUT = [-90, 0, -145]
BLADE_OUT_LEFT = [-90, 0, 145]
AXE_LOW = [25, 90, 0]
AXE_ACROSS = [-25, 90, 0]
AXE_GRIP = [14, 0, 0]
# Hoshina NPC: as posturas aprovadas antes dos videos (laminas coladas no antebraco). O NPC continua com elas ate o
# Miguel decidir se ele tambem muda ([DECIDIR]); gen_hoshina_animations.py le esta tabela.
HOSHINA_NPC_DUAL = {
    "idle": {"body_pos": [0, -4, 0], "right_leg": [-35, 0, 20], "left_leg": [30, 0, -18], "torso": [25, 0, 0],
             "head": [-22, 0, 0], "right_arm": [60, 0, 20], "left_arm": [30, 0, -15],
             "right_item": ALONG_FOREARM, "left_item": ALONG_FOREARM,
             "right_item_pos": BEHIND_FOREARM, "left_item_pos": BEHIND_FOREARM},
    "move": {"swing": 30, "period": 0.7, "body_pos": [0, -1, 0], "torso": [8, 0, 0], "head": [-6, 0, 0],
             "right_arm": [-5, 0, 35], "left_arm": [-5, 0, -35], "right_item": ALONG_FOREARM,
             "left_item": ALONG_FOREARM, "right_item_pos": BEHIND_FOREARM, "left_item_pos": BEHIND_FOREARM},
    "run": {"swing": 50, "period": 0.45, "body_pos": [0, -5, 0], "torso": [40, 0, 0], "head": [-34, 0, 0],
            "right_arm": [50, 0, 45], "left_arm": [40, 0, -40], "right_item": ALONG_FOREARM,
            "left_item": ALONG_FOREARM, "right_item_pos": BEHIND_FOREARM, "left_item_pos": BEHIND_FOREARM},
}
STANCES = {
    # Faca: base atletica, pernas um pouco abertas, guarda curta.
    "knife": {
        "idle": {"body_pos": [0, -1.5, 0], "right_leg": [-12, 0, 8], "left_leg": [10, 0, -8], "torso": [8, 8, 0],
                 "right_arm": [-35, -12, 8], "left_arm": [-25, 18, -6]},
        "move": {"swing": 28, "period": 0.7, "body_pos": [0, -1, 0], "torso": [10, 8, 0],
                 "right_arm": [-35, -12, 8], "left_arm": [-25, 18, -6]},
        "run": {"swing": 45, "period": 0.5, "body_pos": [0, -1, 0], "torso": [20, 0, 0],
                "right_arm": [30, 0, 20], "left_arm": [-30, 0, -20]},
    },
    # Espada de uma mao (referencia: braco caido a frente, lamina apontando para o chao na diagonal).
    "sword": {
        "idle": {"body_pos": [0, -1.5, 0], "right_leg": [-14, 0, 6], "left_leg": [12, 0, -6], "torso": [6, 10, 0],
                 "right_arm": [-18, 0, 22], "right_item": [55, 0, 0], "left_arm": [-10, 6, -6]},
        "move": {"swing": 26, "period": 0.75, "body_pos": [0, -1, 0], "torso": [6, 8, 0],
                 "right_arm": [-18, 0, 22], "right_item": [55, 0, 0], "left_arm": [-10, 6, -6]},
        "run": {"swing": 45, "period": 0.5, "body_pos": [0, -1, 0], "torso": [22, 0, 0],
                "right_arm": [40, 0, 18], "right_item": [70, 0, 0], "left_arm": [-35, 0, -15]},
    },
    # Hoshina. Parado: agachado, perna direita a frente, tronco inclinado, bracos para tras (direito mais alto).
    # Andando: quase reto, bracos abertos para os lados e para baixo. Correndo: bem inclinado e baixo, bracos
    # abertos para tras como "asas" (direito um pouco mais alto).
    "dual_reverse": {
        "idle": {"body_pos": [0, -2.5, 0], "right_leg": [-15, 0, 12], "left_leg": [12, 0, -12], "torso": [12, 0, 0],
                 "head": [-10, 0, 0], "right_arm": [-20, 0, 45], "left_arm": [-20, 0, -45],
                 "right_item": BLADE_OUT, "left_item": BLADE_OUT_LEFT},
        "move": {"swing": 30, "period": 0.7, "body_pos": [0, -1, 0], "torso": [8, 0, 0], "head": [-6, 0, 0],
                 "right_arm": [-35, 10, 22], "left_arm": [-35, -10, -22], "right_item": BLADE_OUT,
                 "left_item": BLADE_OUT_LEFT},
        # Correndo (comeco do video 1): bem inclinado e baixo, bracos para tras, laminas coladas no antebraco.
        "run": {"swing": 50, "period": 0.45, "body_pos": [0, -5, 0], "torso": [40, 0, 0], "head": [-34, 0, 0],
                "right_arm": [50, 0, 45], "left_arm": [40, 0, -40], "right_item": ALONG_FOREARM,
                "left_item": ALONG_FOREARM, "right_item_pos": BEHIND_FOREARM, "left_item_pos": BEHIND_FOREARM},
    },
    # Kikoru (fotos "Battle Axe"): machado atravessado na frente, as duas maos no cabo; andando igual; correndo com a
    # mao direita so, machado arrastando atras e o braco esquerdo livre.
    "two_handed_axe": {
        # 0.5.0-D7: bracos e pegada do machado do Miguel (jogador_machado2.bbmodel, tempo 0,5 de stance,
        # stance_move e stance_run), convertidos do Blockbench; pernas e tronco continuam os da 0.5.0-D6.
        "idle": {"body_pos": [0, -0.8, 0], "right_leg": [-6, 0, 10], "left_leg": [6, 0, -10], "torso": [4, 0, 0],
                 "right_arm": [-67.42, -15.0, 20.17], "left_arm": [-54.49, 15.58, -15.05],
                 "right_item": [-15.95, 105.31, 1.84], "right_item_pos": [11.0, -4.0, -2.0]},
        "move": {"swing": 22, "period": 0.85, "body_pos": [0, -0.5, 0], "torso": [6, 0, 0],
                 "right_arm": [16.5, 12.0, 37.97], "left_arm": [-18.23, 5.18, -26.18],
                 "right_item": [-6.32, 14.39, 6.71], "right_item_pos": [0, 0, 0]},
        "draw_via": {"torso": [2, 15, 0], "right_arm": [-35, -25, 0], "left_arm": [-40, 30, 0],
                     "right_item": AXE_ACROSS, "right_item_pos": AXE_GRIP},
        "run": {"swing": 40, "period": 0.55, "torso": [22, 0, 0], "head": [-18, 0, 0],
                "right_arm": [-64.62, -5.37, 6.49], "right_arm_pos": [0, -1.0, 0], "left_arm": [-30.77, -0.49, 0.99],
                "right_item": [0.23, 75.14, -11.22], "right_item_pos": [15.0, -1.0, 4.0]},
    },
}

LEGS = ("right_leg", "left_leg")
ZERO = [0, 0, 0]
GECKO_NAMES = {"torso": "body", "head": "head", "right_arm": "arm_right", "left_arm": "arm_left",
               "right_leg": "leg_right", "left_leg": "leg_left", "right_item": "item_right", "left_item": "item_left"}


def to_pal(pose):
    """Pose neutra -> {(osso, canal): vetor} da PAL. A inclinacao vai no osso "body" (gira o jogador inteiro em volta
    do quadril; o "torso" da PAL so gira o cubo do tronco) e as pernas descontam a inclinacao para ficar no chao."""
    lean = pose.get("torso", ZERO)
    out = {}
    for key, v in pose.items():
        if key == "torso":
            out[("body", "rotation")] = v
        elif key == "body_pos":
            out[("body", "position")] = v
        elif key.endswith("_pos"):
            out[(key[:-4], "position")] = v
        elif key in LEGS:
            out[(key, "rotation")] = [v[0] - lean[0], v[1] - lean[1], v[2]]
        else:
            out[(key, "rotation")] = v
    if "torso" in pose:
        for leg in LEGS:
            out.setdefault((leg, "rotation"), [-lean[0], -lean[1], 0])
    return out


def to_gecko(pose):
    """Pose neutra -> {(osso, canal): vetor} do soldado especial (GeckoLib: bracos e cabeca filhos do "body", pernas
    filhas da raiz; mesmos angulos, conferido em jogo lado a lado)."""
    out = {}
    for key, v in pose.items():
        if key == "body_pos":
            out[("root", "position")] = v
        elif key.endswith("_pos"):
            out[(GECKO_NAMES[key[:-4]], "position")] = v
        else:
            out[(GECKO_NAMES[key], "rotation")] = v
    return out


def merged(*poses):
    out = {}
    for pose in poses:
        out.update(pose)
    return out


def animate(frames, length, convert=to_pal, loop=None, vector=True):
    """Frames [(segundos, pose neutra)] -> animacao. Osso que falta num frame fica na posicao de repouso (0)."""
    converted = [(t, convert(pose)) for t, pose in frames]
    channels = sorted({key for _, c in converted for key in c})
    bones = {}
    for bone, channel in channels:
        series = [(t, c.get((bone, channel), ZERO)) for t, c in converted]
        out = {}
        for t, v in series:
            out[f"{round(t, 3)}"] = {"vector": list(v)} if vector else list(v)
        bones.setdefault(bone, {})[channel] = out
    anim = {"animation_length": round(length, 3), "bones": bones}
    if loop is not None:
        anim = {"loop": loop, **anim}
    return anim


# Combos (golpe leve, um por passo do combo; tempos do light do JSON da arma) e golpe pesado de cada familia.
# Cada golpe: pose de preparo (wind) e pose do impacto (hit); comeca e termina na postura parada.
COMBOS = {
    "knife": [
        # 1: estocada curta para a frente com passo.
        {"wind": {"right_arm": [-60, -10, 10], "torso": [5, 25, 0]},
         "hit": {"right_arm": [-95, -5, 0], "torso": [15, -10, 0], "right_leg": [-30, 0, 6]}},
        # 2: corte horizontal da direita para a esquerda.
        {"wind": {"right_arm": [-80, 30, 40], "torso": [5, 35, 0]},
         "hit": {"right_arm": [-80, -60, -10], "torso": [8, -30, 0]}},
        # 3: corte de revés subindo, com giro do tronco.
        {"wind": {"right_arm": [-40, -50, -20], "torso": [5, -35, 0]},
         "hit": {"right_arm": [-130, 40, 30], "torso": [-5, 30, 0], "left_leg": [20, 0, -10]}},
    ],
    "sword": [
        # 1: diagonal de cima/direita para baixo/esquerda.
        {"wind": {"right_arm": [-160, 20, 30], "torso": [-5, 25, 0]},
         "hit": {"right_arm": [-40, -40, -20], "right_item": [20, 0, 0], "torso": [15, -20, 0],
                 "right_leg": [-25, 0, 6]}},
        # 2: horizontal de volta (esquerda para direita).
        {"wind": {"right_arm": [-80, -60, -10], "torso": [5, -30, 0]},
         "hit": {"right_arm": [-85, 50, 30], "torso": [8, 35, 0]}},
        # 3: estocada com avanco.
        {"wind": {"right_arm": [-50, 0, 10], "torso": [0, 20, 0], "right_leg": [10, 0, 6]},
         "hit": {"right_arm": [-95, 0, 0], "right_item": [-10, 0, 0], "torso": [20, -5, 0],
                 "right_leg": [-45, 0, 6], "left_leg": [30, 0, -6], "body_pos": [0, -2, 0]}},
    ],
    # Hoshina (video 1): cortes alternando as laminas, giro inteiro com os bracos abertos (o corpo volta de frente
    # pelo outro lado: "spin") e o X agachado.
    "dual_reverse": [
        {"wind": {"right_arm": [-50, 30, 70], "torso": [20, 30, 0]},
         "hit": {"right_arm": [-80, -50, 10], "torso": [20, -25, 0], "right_leg": [-25, 0, 12]}},
        {"wind": {"left_arm": [-50, -30, -70], "torso": [20, -30, 0]},
         "hit": {"left_arm": [-80, 50, -10], "torso": [20, 25, 0], "left_leg": [-25, 0, -12]}},
        {"wind": {"right_arm": [-20, 0, 80], "left_arm": [-20, 0, -80], "torso": [20, 40, 0],
                  "body_pos": [0, -3, 0]},
         "hit": {"right_arm": [-25, 0, 85], "left_arm": [-25, 0, -85], "torso": [20, -200, 0],
                 "body_pos": [0, -4, 0]},
         "spin": -360},
        {"wind": {"right_arm": [-150, -30, 20], "left_arm": [-150, 30, -20], "torso": [10, 0, 0]},
         "hit": {"right_arm": [-40, 40, -20], "left_arm": [-40, -40, 20], "torso": [35, 0, 0],
                 "body_pos": [0, -6, 0]}},
    ],
    # Kikoru (video 2): varrida baixa da direita para a esquerda com a cabeca do machado rente ao chao e varrida
    # atravessada na altura do peito (o machado termina do lado esquerdo).
    "two_handed_axe": [
        # A varrida baixa tem a rotacao propria (cabeca rente ao chao), nao a da pegada parada (0.5.0-D7).
        {"wind": {"right_arm": [-25, 35, 25], "left_arm": [-30, 60, 0], "torso": [12, 50, 0],
                  "body_pos": [0, -1.5, 0], "right_item": AXE_LOW, "right_item_pos": AXE_GRIP},
         "hit": {"right_arm": [-45, -55, 0], "left_arm": [-40, -25, 0], "torso": [18, -50, 0],
                 "right_leg": [-25, 0, 12], "body_pos": [0, -2.5, 0], "right_item": AXE_LOW,
                 "right_item_pos": AXE_GRIP}},
        {"wind": {"right_arm": [-80, 50, 0], "left_arm": [-80, 70, 0], "torso": [0, 55, 0],
                  "right_item": AXE_ACROSS, "right_item_pos": AXE_GRIP},
         "hit": {"right_arm": [-85, -50, 0], "left_arm": [-80, -30, 0], "torso": [8, -55, 0],
                 "right_item": AXE_ACROSS, "right_item_pos": AXE_GRIP, "left_leg": [20, 0, -10]}},
    ],
}

# Pesado de cada familia (0.5.0-D6, videos do Miguel); sem entrada aqui, o ultimo golpe do combo mais amplo.
# - Hoshina: avanco de lado com estocada da lamina direita (corpo de perfil, braco esticado para o alvo);
# - Kikoru: machado erguido sobre o ombro direito e golpe de cima para baixo ate o chao.
HEAVIES = {
    "dual_reverse": {
        "wind": {"right_arm": [10, 0, 45], "left_arm": [-20, 0, -60], "torso": [10, -25, 0],
                 "body_pos": [0, -3, 0]},
        "hit": {"right_arm": [-15, 0, 85], "left_arm": [-10, 0, -60], "torso": [15, -70, 0],
                "right_item": [90, 0, 0], "right_leg": [-55, 0, 10], "left_leg": [35, 0, -10],
                "body_pos": [0, -5, 0]}},
    "two_handed_axe": {
        "wind": {"right_arm": [-170, -15, 0], "left_arm": [-165, 20, 0], "torso": [-12, 10, 0],
                 "right_item": [0, 0, 0], "right_item_pos": AXE_GRIP},
        "hit": {"right_arm": [-45, -10, 0], "left_arm": [-45, 15, 0], "torso": [30, 0, 0],
                "right_item": [45, 0, 0], "right_item_pos": AXE_GRIP, "body_pos": [0, -3, 0],
                "right_leg": [-30, 0, 10], "left_leg": [20, 0, -10]}},
}


# 0.5.0-D3: especial (tecla R) de cada familia, saindo da postura dela (pesquisa do anime/manga):
# - Hoshina, forma 1 Kuuchi: lamina direita armada a frente do peito (lado esquerdo) e varredura invertida para
#   fora num instante, com avanco da perna direita (o corte "invisivel" que solta pressao de ar);
# - Kikoru, Axe Slam: salta com o machado erguido e crava no chao no tick do impacto.
SPECIALS = {
    "dual_reverse": {
        "wind": {"right_arm": [-95, -55, 0], "left_arm": [-30, 0, -70], "torso": [28, -35, 0]},
        "hit": {"right_arm": [-75, 10, 80], "left_arm": [-25, 0, -85], "torso": [30, 30, 0],
                "right_leg": [-60, 0, 12], "left_leg": [45, 0, -10], "body_pos": [0, -6, 0]}},
    "two_handed_axe": {
        "wind": {"right_arm": [-175, 0, 12], "left_arm": [-175, 0, -12], "right_item": [0, 0, 0],
                 "torso": [-15, 0, 0], "right_leg": [-35, 0, 6], "left_leg": [-20, 0, -6], "body_pos": [0, 7, 0]},
        "hit": {"right_arm": [-35, 0, 5], "left_arm": [-35, 0, -5], "right_item": [0, 0, 0], "torso": [32, 0, 0],
                "right_leg": [-35, 0, 10], "left_leg": [25, 0, -10], "body_pos": [0, -3, 0]}},
}


def strike_frames(action, start, wind, hit, spin=None):
    """Preparo, impacto no tick do JSON, continuacao e recuperacao ate a postura. Com spin (graus), o giro do
    tronco segue no mesmo sentido ate a volta inteira em vez de desenrolar (giro de 360 graus)."""
    end, impact = action["duration_ticks"] * TICK, action["impact_tick"] * TICK
    t_wind, t_follow = impact * 0.6, impact + (end - impact) * 0.35
    wound = merged(start, wind)
    struck = merged(wound, hit)
    follow, back = struck, start
    if spin is not None:
        lean = start.get("torso", ZERO)
        turned = struck["torso"][1]
        follow = merged(struck, {"torso": [struck["torso"][0], turned + (spin - turned) * 0.6, 0]})
        back = merged(start, {"torso": [lean[0], lean[1] + spin, lean[2]]})
    return [(0, start), (t_wind, wound), (impact, struck), (t_follow, follow), (end, back)], end


def strike(action, start, wind, hit, convert=to_pal, vector=True, spin=None):
    """Golpe que sai da postura (start), vai ao preparo, acerta no tick de impacto do JSON e volta a postura."""
    frames, end = strike_frames(action, start, wind, hit, spin)
    return animate(frames, end, convert, vector=vector)


def heavy_strike(action, start, family):
    """Pesado de cada familia (HEAVIES); sem entrada, o ultimo golpe do combo, mais lento e mais amplo."""
    if family in HEAVIES:
        return strike(action, start, HEAVIES[family]["wind"], HEAVIES[family]["hit"])
    last = COMBOS[family][-1]
    wind = {bone: [v[0] * 1.15, v[1], v[2]] if bone.endswith("arm") else v for bone, v in last["wind"].items()}
    return strike(action, start, wind, last["hit"])


def stance_frames(pose):
    """Postura parada em laco com uma respiracao de 2 graus nos bracos (os golpes passam por cima)."""
    breath = {bone: [v[0] - 2, v[1], v[2]] if bone.endswith("arm") else v for bone, v in pose.items()}
    return [(0, pose), (1.0, breath), (2.0, pose)], 2.0


def stance(pose, convert=to_pal, vector=True):
    frames, length = stance_frames(pose)
    return animate(frames, length, convert, loop=True, vector=vector)


def stance_move_frames(pose):
    """Andando/correndo: pernas balancam (opostas) com a amplitude do perfil, corpo sobe e desce a cada passo, o
    resto fica na postura da familia; "free_arm" balanca um braco livre (machado correndo com uma mao so)."""
    period, swing = pose["period"], pose["swing"]
    base = pose.get("body_pos", ZERO)
    fixed = {k: v for k, v in pose.items() if k not in ("period", "swing", "body_pos", "free_arm")}
    free = pose.get("free_arm")
    frames = []
    for step in range(5):
        t = period * step / 4
        phase = (1, 0, -1, 0, 1)[step]
        frame = dict(fixed)
        frame["right_leg"] = [-swing * phase, 0, 3]
        frame["left_leg"] = [swing * phase, 0, -3]
        frame["body_pos"] = base if step % 2 == 0 else [base[0], base[1] + 0.6, base[2]]
        if free:
            arm, rest, amplitude = free
            frame[arm] = [rest[0] - amplitude * phase, rest[1], rest[2]]
        frames.append((t, frame))
    # Pernas no meio do passo (fases 0): ficam retas.
    return frames, period


def stance_move(pose, convert=to_pal, vector=True):
    frames, length = stance_move_frames(pose)
    return animate(frames, length, convert, loop=True, vector=vector)


def draw(pose, ticks, hands, via=None):
    """Saque: a mao vai a bainha/coldre (quadril ou costas) e traz a arma ate a postura no tempo do perfil. Com via
    (machado), a arma passa por essa pose antes de baixar para a postura."""
    end = max(ticks, 2) * TICK
    reach = end * 0.4
    target = pose or {"right_arm": [-60, -8, 0], "left_arm": [-55, 25, 0]}
    if via:
        return animate([(0, {}), (reach, via), (end * 0.7, via), (end, target)], end)
    middle = {"right_arm": [25, 20, 25]}
    if hands != "one" or "left_arm" in target:
        middle["left_arm"] = [25, -20, -25] if hands == "dual" else ZERO
    for item in ("right_item", "left_item"):
        if item in target:
            middle[item] = [90, 0, 0]
    if "torso" in target:
        middle["torso"] = [0, -10, 0]
    return animate([(0, {}), (reach, middle), (end, target)], end)


def guard(hands):
    """Guarda por familia (segura ate o servidor soltar): uma mao na frente; duas maos com a arma atravessada;
    duas laminas cruzadas em X."""
    up = BLOCK_RAISE_TICKS * TICK
    if hands == "dual":
        pose = {"right_arm": [-95, -40, 0], "left_arm": [-95, 40, 0], "right_item": [0, 0, -35],
                "left_item": [0, 0, 35]}
    elif hands == "two":
        pose = {"right_arm": [-80, -45, 0], "left_arm": [-80, 45, 0]}
    else:
        pose = {"right_arm": [-85, -25, 15], "left_arm": [-40, 20, 0]}
    pose["torso"] = [8, 0, 0]
    return animate([(0, {}), (up, pose)], up, loop="hold_on_last_frame")


def reload(stages, hands):
    """Recarga por etapas (tempos do perfil): arma baixa, mao esquerda solta o pente, busca outro na cintura, coloca e
    engatilha. Cada etapa e um trecho da animacao, entao o som de cada uma (servidor) cai junto."""
    times, t = [], 0.0
    for stage in stages:
        t += stage["ticks"] * TICK
        times.append(t)
    end = times[-1]
    aim = [-75, -8, 0] if hands == "two" else [-70, -15, 0]
    # So um pouco abaixo da mira: em primeira pessoa a arma continua na tela durante a recarga.
    low = [-62, -14, 0]
    eject, insert = times[0], times[1] if len(times) > 1 else end
    return {"animation_length": round(end, 3), "bones": {
        "right_arm": {"rotation": keys((0, aim), (eject * 0.5, low), (insert, low), (end, aim))},
        "left_arm": {"rotation": keys((0, [-85, 25, 0]), (eject * 0.6, [-55, 10, 0]), (eject, [-10, 15, -15]),
                                      (insert * 0.8, [-60, 5, 0]), (insert, [-70, 0, 0]),
                                      (end - (end - insert) * 0.3, [-80, 30, 0]), (end, [-85, 25, 0]))},
        "head": {"rotation": keys((0, [0, 0, 0]), (eject, [20, 0, 0]), (insert, [20, 0, 0]), (end, [0, 0, 0]))},
    }}


def profile_animations():
    """0.5.0-D: player.<perfil>.stance/draw/guard/reload de cada weapon_profile/<id>.json."""
    result = {}
    for path in sorted(PROFILES.glob("*.json")):
        profile = json.loads(path.read_text(encoding="utf-8"))
        name = path.stem
        hands = profile.get("hands", "one")
        poses = STANCES.get(name) if profile.get("stance", True) else None
        if poses:
            result[f"player.{name}.stance"] = stance(poses["idle"])
            result[f"player.{name}.stance_move"] = stance_move(poses["move"])
            result[f"player.{name}.stance_run"] = stance_move(poses["run"])
        weapon = weapon_of(name)
        if poses and weapon and name in COMBOS:
            start = poses["idle"]
            light, heavy = weapon["actions"].get("light"), weapon["actions"].get("heavy")
            for step, move in enumerate(COMBOS[name]):
                result[f"player.{name}.light_{step + 1}"] = strike(light, start, move["wind"], move["hit"],
                                                                   spin=move.get("spin"))
            if heavy:
                result[f"player.{name}.heavy"] = heavy_strike(heavy, start, name)
        pose = poses["idle"] if poses else None
        result[f"player.{name}.draw"] = draw(pose, profile.get("draw", {}).get("ticks", 0), hands,
                                             poses.get("draw_via") if poses else None)
        result[f"player.{name}.guard"] = guard(hands)
        if "reload" in profile:
            result[f"player.{name}.reload"] = reload(profile["reload"]["stages"], hands)
    return result


def weapon_of(profile_name):
    """JSON da arma que usa este perfil (os tempos dos golpes vem dela)."""
    for path in sorted(WEAPONS.glob("*.json")):
        weapon = json.loads(path.read_text(encoding="utf-8"))
        if weapon.get("profile") == f"kn8:{profile_name}":
            return weapon
    return None


def body_kick(weapon):
    profile = weapon.get("profile")
    if not profile:
        return 1.0
    path = PROFILES / (profile.split(":", 1)[1] + ".json")
    if not path.exists():
        return 1.0
    return json.loads(path.read_text(encoding="utf-8")).get("recoil", {}).get("body_kick", 1.0)


def weapon_animations():
    """Uma animacao por acao de cada arma (player.<item>.<acao>), com os tempos do JSON da propria arma."""
    result = {}
    for path in sorted(WEAPONS.glob("*.json")):
        weapon = json.loads(path.read_text(encoding="utf-8"))
        item = weapon["item"].split(":", 1)[1]
        actions = weapon["actions"]
        if weapon["style"] == "firearm":
            result[f"player.{item}.shoot"] = shoot(actions["light"], body_kick(weapon))
            continue
        if "light" in actions:
            result[f"player.{item}.light"] = light(actions["light"])
        if "heavy" in actions:
            result[f"player.{item}.heavy"] = heavy(actions["heavy"])
        special = weapon.get("special")
        family = weapon.get("profile", "").split(":", 1)[-1]
        if special and family in SPECIALS and family in STANCES:
            move = SPECIALS[family]
            result[f"player.{item}.special"] = strike(special, STANCES[family]["idle"], move["wind"], move["hit"])
        elif special and special["type"] == "ground_slam":
            result[f"player.{item}.special"] = special_slam(special)
        elif special and special["type"] == "slash_wave":
            result[f"player.{item}.special"] = special_slash(special)
    return result


def main():
    weapon = json.loads(WEAPON.read_text(encoding="utf-8"))
    actions = weapon["actions"]
    data = {"format_version": "1.8.0", "animations": {
        "player.action.light": light(actions["light"]),
        "player.action.heavy": heavy(actions["heavy"]),
        "player.action.block": block(),
        "player.action.dodge": dodge(),
        "player.action.shoot": shoot(json.loads(RIFLE.read_text(encoding="utf-8"))["actions"]["light"]),
        "player.action.dash": dash(),
        "player.action.charge": charge(),
    }}
    # Animacoes genericas (comandos de teste /kn8 anim) + as de cada arma (usadas no combate).
    data["animations"].update(weapon_animations())
    data["animations"].update(profile_animations())
    # 0.5.0-D5: o que o Miguel animou no Blockbench (tools/blockbench/animacoes/*.bbmodel) vale por cima.
    from blockbench_templates import imported_player
    data["animations"].update(imported_player())
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
    for name, anim in data["animations"].items():
        print(f"{name}: {anim['animation_length']} s")


if __name__ == "__main__":
    main()
