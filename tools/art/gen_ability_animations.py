#!/usr/bin/env python3
"""Animacoes dos ataques novos dos kaiju (0.6): acrescenta (ou refaz) so estas animacoes no
assets/kn8/animations/entity/<especie>.animation.json, sem mexer nas outras.

O pico de cada golpe cai no tick de impacto do JSON da habilidade (windup_ticks), como nas armas do jogador: mudar
o JSON e rodar de novo ajusta a animacao. Rodar DEPOIS dos scripts de rig/build de cada especie (eles reescrevem o
arquivo inteiro e apagariam estas).
Uso: python3 tools/art/gen_ability_animations.py
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/kn8/animations/entity"
ABILITIES = ROOT / "src/main/resources/data/kn8/kn8/ability"
TICK = 1 / 20


def ability(name):
    data = json.loads((ABILITIES / f"{name}.json").read_text(encoding="utf-8"))
    hit = data["windup_ticks"] * TICK
    end = (data["windup_ticks"] + max(1, data.get("active_ticks", 1))) * TICK + 0.25
    return round(hit, 3), round(end, 3)


def keys(*frames):
    return {f"{round(t, 3)}": v for t, v in frames}


def rot(*frames):
    return {"rotation": keys(*frames)}


ZERO = [0, 0, 0]


# --- Primigenius (todas as formas: mesmos ossos de braco, cauda e mandibula) ------------------------------------
def strike(name):
    """Golpe de casco: braco direito sobe e desce na frente."""
    hit, end = ability(name)
    up = hit * 0.6
    return {"animation_length": end, "bones": {
        "arm_right": rot((0, ZERO), (up, [-120, 0, 15]), (hit, [-20, 0, 0]), (end, ZERO)),
        "forearm_right": rot((0, ZERO), (up, [-40, 0, 0]), (hit, [0, 0, 0]), (end, ZERO)),
        "body": rot((0, ZERO), (up, [0, 15, 0]), (hit, [8, -12, 0]), (end, ZERO)),
    }}


def punch(name):
    """Soco pesado: carrega para tras com o corpo girado e solta com o peso."""
    hit, end = ability(name)
    load = hit * 0.7
    return {"animation_length": end, "bones": {
        "arm_right": rot((0, ZERO), (load, [-30, 35, 30]), (hit, [-95, -10, 0]), (end, ZERO)),
        "forearm_right": rot((0, ZERO), (load, [-70, 0, 0]), (hit, [-5, 0, 0]), (end, ZERO)),
        "body": rot((0, ZERO), (load, [-6, 30, 0]), (hit, [14, -20, 0]), (end, ZERO)),
        "jaw": rot((0, ZERO), (load, [20, 0, 0]), (hit, [5, 0, 0]), (end, ZERO)),
    }}


def tail_swipe(name):
    """Rabada: o corpo gira e a cauda varre de um lado ao outro."""
    hit, end = ability(name)
    load = hit * 0.6
    return {"animation_length": end, "bones": {
        "body": rot((0, ZERO), (load, [0, -25, 0]), (hit, [0, 35, 0]), (end, ZERO)),
        "tail_1": rot((0, ZERO), (load, [0, 30, 0]), (hit, [0, -35, 0]), (end, ZERO)),
        "tail_2": rot((0, ZERO), (load, [0, 25, 0]), (hit, [0, -30, 0]), (end, ZERO)),
        "tail_3": rot((0, ZERO), (load, [0, 20, 0]), (hit, [0, -25, 0]), (end, ZERO)),
        "tail_4": rot((0, ZERO), (load, [0, 15, 0]), (hit, [0, -20, 0]), (end, ZERO)),
    }}


def tail_stab(name):
    """0.6-E (No. 10): a cauda passa por cima do ombro e perfura para a frente."""
    hit, end = ability(name)
    load = hit * 0.6
    return {"animation_length": end, "bones": {
        "body": rot((0, ZERO), (load, [-10, 0, 0]), (hit, [12, 0, 0]), (end, ZERO)),
        "tail_1": rot((0, ZERO), (load, [-45, 0, 0]), (hit, [-60, 0, 0]), (end, ZERO)),
        "tail_2": rot((0, ZERO), (load, [-35, 0, 0]), (hit, [-50, 0, 0]), (end, ZERO)),
        "tail_3": rot((0, ZERO), (load, [-30, 0, 0]), (hit, [-20, 0, 0]), (end, ZERO)),
        "tail_4": rot((0, ZERO), (load, [-20, 0, 0]), (hit, [10, 0, 0]), (end, ZERO)),
    }}


def tail_smash(name):
    """0.6-E (No. 10 gigante): ergue a cauda bem alto e bate no chao atras de si."""
    hit, end = ability(name)
    load = hit * 0.65
    return {"animation_length": end, "bones": {
        "body": rot((0, ZERO), (load, [12, 0, 0]), (hit, [-10, 0, 0]), (end, ZERO)),
        "tail_1": rot((0, ZERO), (load, [-50, 0, 0]), (hit, [20, 0, 0]), (end, ZERO)),
        "tail_2": rot((0, ZERO), (load, [-40, 0, 0]), (hit, [25, 0, 0]), (end, ZERO)),
        "tail_3": rot((0, ZERO), (load, [-30, 0, 0]), (hit, [20, 0, 0]), (end, ZERO)),
        "tail_4": rot((0, ZERO), (load, [-20, 0, 0]), (hit, [10, 0, 0]), (end, ZERO)),
    }}


def multi_strike(name):
    """0.6-E (No. 10): rajada de golpes alternando os bracos, um por golpe do JSON (behavior.hits)."""
    data = json.loads((ABILITIES / f"{name}.json").read_text(encoding="utf-8"))
    behavior = data.get("behavior", {})
    hits, step = behavior.get("hits", 1), behavior.get("hit_interval", 4) * TICK
    first, end = ability(name)
    end = round(first + hits * step + 0.25, 3)
    right, left = [(0, ZERO)], [(0, ZERO)]
    for i in range(hits):
        t = first + i * step
        arm = right if i % 2 == 0 else left
        arm += [(t - step * 0.5, [-40, 0, 0]), (t, [-110, 0, 0])]
    right.append((end, ZERO))
    left.append((end, ZERO))
    return {"animation_length": end, "bones": {
        "arm_right": rot(*right), "arm_left": rot(*left),
        "body": rot((0, ZERO), (first, [10, 0, 0]), (end - 0.1, [10, 0, 0]), (end, ZERO)),
    }}


def energy_blast(name):
    """Raio de energia: abre a boca, recua carregando (o aviso brilha na boca) e dispara para a frente."""
    hit, end = ability(name)
    return {"animation_length": end, "bones": {
        "body": rot((0, ZERO), (hit * 0.8, [-12, 0, 0]), (hit, [10, 0, 0]), (end, ZERO)),
        "head": rot((0, ZERO), (hit * 0.8, [-20, 0, 0]), (hit, [5, 0, 0]), (end, ZERO)),
        "jaw": rot((0, ZERO), (hit * 0.5, [30, 0, 0]), (hit, [40, 0, 0]), (end - 0.1, [10, 0, 0]), (end, ZERO)),
        "arm_left": rot((0, ZERO), (hit * 0.8, [20, 0, -25]), (hit, [0, 0, -10]), (end, ZERO)),
        "arm_right": rot((0, ZERO), (hit * 0.8, [20, 0, 25]), (hit, [0, 0, 10]), (end, ZERO)),
    }}


# --- Trichonephila (pernas 0 = da frente) -------------------------------------------------------------------------
def leg_swipe(name):
    hit, end = ability(name)
    load = hit * 0.6
    return {"animation_length": end, "bones": {
        "leg_left_0": rot((0, ZERO), (load, [0, 30, 25]), (hit, [0, -40, 5]), (end, ZERO)),
        "leg_right_0": rot((0, ZERO), (load, [0, -30, -25]), (hit, [0, 40, -5]), (end, ZERO)),
        "body": rot((0, ZERO), (load, [-8, 0, 0]), (hit, [6, 0, 0]), (end, ZERO)),
    }}


def leg_stab(name):
    hit, end = ability(name)
    load = hit * 0.6
    return {"animation_length": end, "bones": {
        "leg_left_0": rot((0, ZERO), (load, [0, 0, 45]), (hit, [0, 15, -10]), (end, ZERO)),
        "body": rot((0, ZERO), (load, [-12, 0, 0]), (hit, [10, 0, 0]), (end, ZERO)),
        "head": rot((0, ZERO), (load, [-8, 0, 0]), (hit, [6, 0, 0]), (end, ZERO)),
    }}


def multi_leg(name):
    """Varias pernas da frente golpeando em sequencia (um golpe a cada hit_interval)."""
    hit, end = ability(name)
    data = json.loads((ABILITIES / f"{name}.json").read_text(encoding="utf-8"))
    hits = data.get("behavior", {}).get("hits", 1)
    step = data.get("behavior", {}).get("hit_interval", 4) * TICK
    bones = {}
    for leg, side in (("leg_left_0", 1), ("leg_right_0", -1), ("leg_left_1", 1), ("leg_right_1", -1)):
        frames = [(0, ZERO)]
        for n in range(hits):
            t = hit + n * step
            if (n % 2 == 0) == (side == 1):
                frames += [(max(0.01, t - step * 0.5), [0, 0, 40 * side]), (t, [0, 10 * side, -10 * side])]
        frames.append((end, ZERO))
        bones[leg] = rot(*sorted(frames, key=lambda f: f[0]))
    bones["body"] = rot((0, ZERO), (hit, [8, 0, 0]), (end - 0.1, [8, 0, 0]), (end, ZERO))
    return {"animation_length": end, "bones": bones}


def web_shot(name):
    """Teia: o abdomen sobe e aponta para a frente (por baixo do corpo) no disparo."""
    hit, end = ability(name)
    return {"animation_length": end, "bones": {
        "abdomen": rot((0, ZERO), (hit * 0.7, [-35, 0, 0]), (hit, [-50, 0, 0]), (end, ZERO)),
        "body": rot((0, ZERO), (hit * 0.7, [10, 0, 0]), (hit, [14, 0, 0]), (end, ZERO)),
    }}


def leap(name):
    """Salto: agacha (pernas dobradas), estica no impulso, recolhe no ar e abre ao cair."""
    hit, end = ability(name)
    air = hit + (end - hit) * 0.5
    bones = {"body": {"rotation": keys((0, ZERO), (hit, [-15, 0, 0]), (air, [10, 0, 0]), (end, ZERO)),
                      "position": keys((0, ZERO), (hit * 0.8, [0, -4, 0]), (hit, [0, 0, 0]), (end, ZERO))}}
    for side in ("left", "right"):
        sign = 1 if side == "left" else -1
        for i in range(4):
            bones[f"leg_{side}_{i}"] = rot((0, ZERO), (hit * 0.8, [0, 0, 25 * sign]), (hit, [0, 0, -20 * sign]),
                                           (air, [0, 0, 30 * sign]), (end, ZERO))
    return {"animation_length": end, "bones": bones}


def spore_puff(name):
    """0.7-B (cogumelos): o chapeu (head) recua e se fecha, o caule se encolhe e o chapeu solta os esporos para a
    frente no impacto."""
    hit, end = ability(name)
    load = hit * 0.7
    return {"animation_length": end, "bones": {
        "head": {"rotation": keys((0, ZERO), (load, [-18, 0, 0]), (hit, [14, 0, 0]), (end, ZERO)),
                 "scale": keys((0, [1, 1, 1]), (load, [0.92, 1.08, 0.92]), (hit, [1.08, 0.94, 1.08]),
                               (end, [1, 1, 1]))},
        "body": rot((0, ZERO), (load, [-6, 0, 0]), (hit, [8, 0, 0]), (end, ZERO)),
    }}


# --- Kaiju No. 9 -----------------------------------------------------------------------------------------------
def finger_gun(name):
    hit, end = ability(name)
    aim = [-90, 0, 0]
    return {"animation_length": end, "bones": {
        "arm_right": rot((0, ZERO), (hit * 0.7, aim), (hit, [-100, 0, 0]), (end - 0.1, aim), (end, ZERO)),
        "forearm_right": rot((0, ZERO), (hit * 0.7, [0, 0, 0]), (end, ZERO)),
        "body": rot((0, ZERO), (hit * 0.7, [0, -15, 0]), (end, ZERO)),
    }}


# --- formas do No. 9 (0.7-E) ------------------------------------------------------------------------------------
def multi_hits(name):
    """Tempos dos golpes de uma habilidade multi_hit (behavior.hits/hit_interval) e o fim da animacao."""
    data = json.loads((ABILITIES / f"{name}.json").read_text(encoding="utf-8"))
    behavior = data.get("behavior", {})
    hits, step = behavior.get("hits", 1), behavior.get("hit_interval", 4) * TICK
    first, _ = ability(name)
    return [round(first + i * step, 3) for i in range(hits)], step, round(first + hits * step + 0.25, 3)


def absorb(length):
    """Absorcao (humanoide): bracos abertos para a frente, corpo curvado, como se puxasse a presa com tentaculos."""
    hold = length - 0.4
    return {"animation_length": length, "bones": {
        "body": rot((0, ZERO), (0.4, [-14, 0, 0]), (hold, [-18, 0, 0]), (length, ZERO)),
        "head": rot((0, ZERO), (0.4, [18, 0, 0]), (hold, [10, 0, 0]), (length, ZERO)),
        "arm_left": rot((0, ZERO), (0.4, [-80, 30, -25]), (length / 2, [-95, 20, -20]), (hold, [-80, 30, -25]),
                        (length, ZERO)),
        "arm_right": rot((0, ZERO), (0.4, [-80, -30, 25]), (length / 2, [-95, -20, 20]), (hold, [-80, -30, 25]),
                         (length, ZERO)),
        "forearm_left": rot((0, ZERO), (0.4, [-20, 0, 0]), (hold, [-30, 0, 0]), (length, ZERO)),
        "forearm_right": rot((0, ZERO), (0.4, [-20, 0, 0]), (hold, [-30, 0, 0]), (length, ZERO)),
    }}


def ant_absorb(length):
    """Absorcao da forma formiga: o torso do No. 9 se ergue, o corpo empina e as presas abrem."""
    hold = length - 0.4
    return {"animation_length": length, "bones": {
        "body": rot((0, ZERO), (0.4, [-14, 0, 0]), (hold, [-16, 0, 0]), (length, ZERO)),
        "head": rot((0, ZERO), (0.4, [-20, 0, 0]), (hold, [-10, 0, 0]), (length, ZERO)),
        "fang_left": rot((0, ZERO), (0.4, [0, -30, 0]), (hold, [0, -25, 0]), (length, ZERO)),
        "fang_right": rot((0, ZERO), (0.4, [0, 30, 0]), (hold, [0, 25, 0]), (length, ZERO)),
    }}


def blade_arm(name):
    """Braco que vira lamina: ergue o braco direito atras do ombro e corta na horizontal a frente."""
    hit, end = ability(name)
    load = hit * 0.65
    return {"animation_length": end, "bones": {
        "arm_right": rot((0, ZERO), (load, [-100, 40, 60]), (hit, [-80, -60, 0]), (end, ZERO)),
        "forearm_right": rot((0, ZERO), (load, [-30, 0, 0]), (hit, [0, 0, 0]), (end, ZERO)),
        "body": rot((0, ZERO), (load, [0, 30, 0]), (hit, [6, -35, 0]), (end, ZERO)),
        "arm_left": rot((0, ZERO), (load, [-20, 0, -20]), (end, ZERO)),
    }}


def tendril(name):
    """Tentaculo: os dois bracos apontam e o corpo se lanca para a frente no disparo."""
    hit, end = ability(name)
    aim = hit * 0.7
    return {"animation_length": end, "bones": {
        "arm_right": rot((0, ZERO), (aim, [-75, 0, 10]), (hit, [-95, 0, 0]), (end, ZERO)),
        "arm_left": rot((0, ZERO), (aim, [-75, 0, -10]), (hit, [-95, 0, 0]), (end, ZERO)),
        "body": rot((0, ZERO), (aim, [-10, 0, 0]), (hit, [12, 0, 0]), (end, ZERO)),
    }}


def spike_burst(name):
    """Espinhos do corpo: encolhe-se (escala menor, bracos fechados) e explode abrindo tudo no impacto."""
    hit, end = ability(name)
    load = hit * 0.8
    return {"animation_length": end, "bones": {
        "body": {"rotation": keys((0, ZERO), (load, [25, 0, 0]), (hit, [-12, 0, 0]), (end, ZERO)),
                 "scale": keys((0, [1, 1, 1]), (load, [0.9, 0.92, 0.9]), (hit, [1.12, 1.06, 1.12]),
                               (end, [1, 1, 1]))},
        "arm_left": rot((0, ZERO), (load, [-30, 0, 30]), (hit, [0, 0, -80]), (end, ZERO)),
        "arm_right": rot((0, ZERO), (load, [-30, 0, -30]), (hit, [0, 0, 80]), (end, ZERO)),
        "head": rot((0, ZERO), (load, [25, 0, 0]), (hit, [-25, 0, 0]), (end, ZERO)),
    }}


def tail_barrage(name):
    """Rajada de cauda (fusao): a cauda passa pelo ombro e perfura varias vezes, uma por golpe do JSON."""
    times, step, end = multi_hits(name)
    tail = {f"tail_{i}": [(0, ZERO)] for i in range(1, 5)}
    for t in times:
        for i, (up, down) in enumerate([(-45, -60), (-35, -50), (-30, -15), (-20, 15)], start=1):
            tail[f"tail_{i}"] += [(t - step * 0.5, [up, 0, 0]), (t, [down, 0, 0])]
    bones = {name_: rot(*(frames + [(end, ZERO)])) for name_, frames in tail.items()}
    bones["body"] = rot((0, ZERO), (times[0], [10, 0, 0]), (end - 0.1, [10, 0, 0]), (end, ZERO))
    return {"animation_length": end, "bones": bones}


def mandible_rush(name):
    """Rajada de mordidas (formiga): a cabeca investe e as presas fecham a cada golpe."""
    times, step, end = multi_hits(name)
    head, left, right = [(0, ZERO)], [(0, ZERO)], [(0, ZERO)]
    for t in times:
        head += [(t - step * 0.5, [-12, 0, 0]), (t, [10, 0, 0])]
        left += [(t - step * 0.5, [0, -30, 0]), (t, [0, 10, 0])]
        right += [(t - step * 0.5, [0, 30, 0]), (t, [0, -10, 0])]
    return {"animation_length": end, "bones": {
        "head": rot(*(head + [(end, ZERO)])), "fang_left": rot(*(left + [(end, ZERO)])),
        "fang_right": rot(*(right + [(end, ZERO)])),
        "body": rot((0, ZERO), (times[0], [6, 0, 0]), (end - 0.1, [6, 0, 0]), (end, ZERO)),
    }}


def ant_finger_gun(name):
    """Finger Gun da forma formiga: o corpo empina e aponta (o torso do No. 9 vai junto com o corpo)."""
    hit, end = ability(name)
    return {"animation_length": end, "bones": {
        "body": rot((0, ZERO), (hit * 0.7, [-12, 0, 0]), (hit, [-6, 0, 0]), (end, ZERO)),
        "head": rot((0, ZERO), (hit * 0.7, [-10, 0, 0]), (end, ZERO)),
    }}


def humanoid_leap(name):
    """Voo curto (No. 9 preto): agacha, estica no impulso com os bracos para tras e cai abrindo os bracos."""
    hit, end = ability(name)
    air = hit + (end - hit) * 0.5
    return {"animation_length": end, "bones": {
        "body": {"rotation": keys((0, ZERO), (hit * 0.8, [20, 0, 0]), (hit, [-15, 0, 0]), (air, [10, 0, 0]),
                                  (end, ZERO)),
                 "position": keys((0, ZERO), (hit * 0.8, [0, -3, 0]), (hit, [0, 0, 0]), (end, ZERO))},
        "arm_left": rot((0, ZERO), (hit * 0.8, [30, 0, 0]), (hit, [50, 0, -20]), (air, [-60, 0, -40]), (end, ZERO)),
        "arm_right": rot((0, ZERO), (hit * 0.8, [30, 0, 0]), (hit, [50, 0, 20]), (air, [-60, 0, 40]), (end, ZERO)),
        "leg_left": rot((0, ZERO), (hit * 0.8, [-40, 0, 0]), (hit, [20, 0, 0]), (air, [-30, 0, 0]), (end, ZERO)),
        "leg_right": rot((0, ZERO), (hit * 0.8, [-40, 0, 0]), (hit, [10, 0, 0]), (air, [-20, 0, 0]), (end, ZERO)),
    }}


def ant_rush(name):
    """Ant Rush (forma formiga): o corpo abaixa e avanca com a cabeca baixa e as presas abertas durante a investida."""
    data = json.loads((ABILITIES / f"{name}.json").read_text(encoding="utf-8"))
    start = data["windup_ticks"] * TICK
    end = round(start + data.get("active_ticks", 1) * TICK + 0.2, 3)
    return {"animation_length": end, "bones": {
        "body": rot((0, ZERO), (start, [12, 0, 0]), (end - 0.2, [12, 0, 0]), (end, ZERO)),
        "head": rot((0, ZERO), (start, [18, 0, 0]), (end - 0.2, [18, 0, 0]), (end, ZERO)),
        "fang_left": rot((0, ZERO), (start, [0, -30, 0]), (end - 0.2, [0, -30, 0]), (end, ZERO)),
        "fang_right": rot((0, ZERO), (start, [0, 30, 0]), (end - 0.2, [0, 30, 0]), (end, ZERO)),
    }}


def fixed(make, length):
    """Animacao sem habilidade (gesto de absorver/reviver), com a duracao do numbered/<id>.json."""
    return lambda _name: make(length)


PRIMIGENIUS = {"action.strike": (strike, "hooved_strike"), "action.tail_swipe": (tail_swipe, "tail_swipe")}
HONJU = {"action.punch": (punch, "heavy_punch"), "action.tail_swipe": (tail_swipe, "tail_swipe")}
SPECIES = {
    "primigenius": PRIMIGENIUS,
    "primigenius_resurrected": PRIMIGENIUS,
    "primigenius_honju": {**HONJU, "action.energy_blast": (energy_blast, "energy_blast")},
    "primigenius_revived": {**HONJU, "action.energy_blast": (energy_blast, "energy_blast_revived")},
    "trichonephila": {"action.leg_swipe": (leg_swipe, "leg_swipe"), "action.leg_stab": (leg_stab, "leg_stab"),
                      "action.multi_leg": (multi_leg, "multi_leg"), "action.web_shot": (web_shot, "web_shot"),
                      "action.leap": (leap, "leap")},
    # 0.6-B: a Honju usa os ataques da aranha (mesmos ossos); a explosao de teia usa a animacao da teia.
    "trichonephila_honju": {"action.leg_swipe": (leg_swipe, "leg_swipe"), "action.leg_stab": (leg_stab, "leg_stab"),
                            "action.multi_leg": (multi_leg, "multi_leg"), "action.web_shot": (web_shot, "web_burst"),
                            "action.leap": (leap, "leap")},
    # 0.7-E: formas do No. 9. O gesto de absorver dura o cast_ticks do numbered/<id>.json (60 = 3 s; formiga 50).
    "kaiju_no9": {"action.finger_gun": (finger_gun, "finger_gun"), "action.absorb": (fixed(absorb, 3.0), None)},
    "kaiju_no9_black": {"action.finger_gun": (finger_gun, "finger_gun"),
                        "action.blade_arm": (blade_arm, "no9_black_blade_arm"),
                        "action.tendril": (tendril, "no9_black_tendril"),
                        "action.spike_burst": (spike_burst, "no9_black_spike_burst"),
                        "action.multi_finger_gun": (tendril, "no9_black_multi_finger_gun"),
                        "action.leap": (humanoid_leap, "no9_black_short_flight"),
                        "action.absorb": (fixed(absorb, 3.0), None)},
    "kaiju_no9_fusion": {"action.punch": (punch, "no10_heavy_punch"),
                         "action.tail_swipe": (tail_swipe, "no10_tail_sweep"),
                         "action.tail_stab": (tail_stab, "no10_tail_stab"),
                         "action.finger_gun": (finger_gun, "finger_gun"),
                         "action.finger_cannon": (finger_gun, "no10_finger_cannon"),
                         "action.multi_finger_cannon": (tendril, "no9_fusion_multi_finger_cannon"),
                         "action.multi_strike": (multi_strike, "no10_multi_appendage"),
                         "action.tail_barrage": (tail_barrage, "no9_fusion_tail_barrage"),
                         "action.hybrid_beam": (energy_blast, "no9_fusion_hybrid_beam"),
                         "action.revive": (fixed(absorb, 2.5), None), "action.absorb": (fixed(absorb, 3.0), None)},
    "kaiju_no9_camponotus": {"action.leg_swipe": (leg_swipe, "leg_swipe"), "action.leg_stab": (leg_stab, "leg_stab"),
                             "action.acid_spray": (web_shot, "no9_ant_acid_spray"),
                             "action.leap": (leap, "no9_ant_pounce"),
                             "action.mandible_rush": (mandible_rush, "no9_ant_mandible_rush"),
                             "action.charge": (ant_rush, "no9_ant_rush"),
                             "action.finger_gun": (ant_finger_gun, "finger_gun"),
                             "action.revive": (fixed(ant_absorb, 2.5), None),
                             "action.absorb": (fixed(ant_absorb, 2.5), None)},
    # 0.6-E: Kaiju No. 10 (ossos do Primigenius); "action.slam" e "action.charge" vem do build_primigenius.py.
    "kaiju_no10_small": {"action.punch": (punch, "no10_heavy_punch"),
                         "action.tail_swipe": (tail_swipe, "no10_tail_sweep"),
                         "action.tail_stab": (tail_stab, "no10_tail_stab"),
                         "action.finger_cannon": (finger_gun, "no10_finger_cannon"),
                         "action.multi_strike": (multi_strike, "no10_multi_appendage")},
    "kaiju_no10_giant": {"action.punch": (punch, "no10g_heavy_punch"),
                         "action.tail_swipe": (tail_swipe, "no10g_tail_sweep"),
                         "action.tail_smash": (tail_smash, "no10g_tail_smash"),
                         "action.finger_cannon": (finger_gun, "no10g_finger_cannon"),
                         "action.multi_strike": (multi_strike, "no10g_multi_appendage")},
    # 0.7-A: Honju novos (ossos do Primigenius; slam/charge/bite/roar vem do build_primigenius_honju.py) e a formiga
    # (ossos da aranha). Espinhos do Philinosoma saem da boca como o raio; o acido da formiga sai do abdomen
    # dobrado por baixo do corpo, como a teia.
    "philinosoma": {**HONJU, "action.spine_shot": (energy_blast, "philinosoma_spine_shot")},
    "diclonius": {**HONJU, "action.energy_blast": (energy_blast, "diclonius_atomic_breath")},
    "camponotus": {"action.leg_swipe": (leg_swipe, "leg_swipe"), "action.leg_stab": (leg_stab, "leg_stab"),
                   "action.acid_spray": (web_shot, "camponotus_acid_spray"), "action.leap": (leap, "leap")},
    "camponotus_reborn": {"action.leg_swipe": (leg_swipe, "leg_swipe"), "action.leg_stab": (leg_stab, "leg_stab"),
                          "action.acid_spray": (web_shot, "camponotus_reborn_acid_spray"),
                          "action.leap": (leap, "leap")},
    # 0.7-B: cogumelos (ossos da aranha, sem abdomen).
    "phaneroplasmodium": {"action.leg_swipe": (leg_swipe, "leg_swipe"), "action.leg_stab": (leg_stab, "leg_stab"),
                          "action.multi_leg": (multi_leg, "multi_leg"),
                          "action.spore_shot": (spore_puff, "phaneroplasmodium_spore_shot")},
    "myxogasterocarp": {"action.leg_swipe": (leg_swipe, "leg_swipe"), "action.leg_stab": (leg_stab, "leg_stab"),
                        "action.multi_leg": (multi_leg, "multi_leg"),
                        "action.spore_bomb": (spore_puff, "myxogasterocarp_spore_bomb")},
}


def main():
    for species, anims in SPECIES.items():
        path = ASSETS / f"{species}.animation.json"
        raw = path.read_bytes().decode("utf-8")
        data = json.loads(raw)
        for anim, (make, ability_name) in anims.items():
            data["animations"][f"{species}.{anim}"] = make(ability_name)
        text = json.dumps(data, indent=2) + "\n"
        # Mantem a quebra de linha do arquivo (alguns sao CRLF) para o Git so mostrar o que mudou.
        if "\r\n" in raw:
            text = text.replace("\n", "\r\n")
        path.write_bytes(text.encode("utf-8"))
        print(species, ", ".join(anims))


if __name__ == "__main__":
    main()
