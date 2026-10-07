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


# --- Kaiju No. 9 -----------------------------------------------------------------------------------------------
def finger_gun(name):
    hit, end = ability(name)
    aim = [-90, 0, 0]
    return {"animation_length": end, "bones": {
        "arm_right": rot((0, ZERO), (hit * 0.7, aim), (hit, [-100, 0, 0]), (end - 0.1, aim), (end, ZERO)),
        "forearm_right": rot((0, ZERO), (hit * 0.7, [0, 0, 0]), (end, ZERO)),
        "body": rot((0, ZERO), (hit * 0.7, [0, -15, 0]), (end, ZERO)),
    }}


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
    "kaiju_no9": {"action.finger_gun": (finger_gun, "finger_gun")},
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
