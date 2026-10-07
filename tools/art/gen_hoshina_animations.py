#!/usr/bin/env python3
"""Animacoes das tecnicas do Hoshina (0.6-D), com os tempos de data/kn8/kn8/special_soldier/hoshina.json.

Cada golpe visual cai no tick em que o servidor aplica o dano (windup_ticks + i * hit_interval), como nas
animacoes do jogador (gen_player_animations.py). Escreve em assets/kn8/animations/entity/hoshina.animation.json
(gerado antes por rig_soldier_mesh.py hoshina; rodar este depois dele). Mantem as quebras de linha do arquivo.
Ossos: body, head, arm_left, arm_right, leg_left, leg_right. Braco direito: X negativo = para frente/cima, Y
negativo = para dentro; o esquerdo e o espelho (Y e Z com sinal trocado).
Uso: python3 tools/art/gen_hoshina_animations.py
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PROFILE = ROOT / "src/main/resources/data/kn8/kn8/special_soldier/hoshina.json"
OUT = ROOT / "src/main/resources/assets/kn8/animations/entity/hoshina.animation.json"
TICK = 1 / 20
DASH_TICKS = 8
PARRY_TICKS = 8
COUNTER_EXTRA_TICKS = 8

# Poses dos bracos (direito; o esquerdo e o espelho).
REST = [-30, -10, 0]
HIGH = [-150, 30, 35]
LOW = [-40, -40, -45]
GUARD = [-60, -25, 0]


def mirror(v):
    return [v[0], -v[1], -v[2]]


def keys(*frames):
    """Keyframes {tempo: vetor}; tempos repetidos ficam com o ultimo."""
    out = {}
    for t, v in frames:
        out[f"{round(t, 3)}"] = v
    return out


def anim(length_ticks, bones):
    return {"animation_length": round(length_ticks * TICK, 3), "bones": bones}


def slash_single(t):
    """Kuuchi: lamina direita ao ombro esquerdo e corte diagonal no tick do corte."""
    end, hit = t["duration_ticks"], t["windup_ticks"]
    wind = hit * 0.6
    return anim(end, {
        "arm_right": {"rotation": keys((0, REST), (wind * TICK, HIGH), (hit * TICK, LOW), (end * TICK, REST))},
        "arm_left": {"rotation": keys((0, mirror(REST)), (hit * TICK, mirror([-50, 10, 10])),
                                      (end * TICK, mirror(REST)))},
        "body": {"rotation": keys((0, [0, 0, 0]), (wind * TICK, [0, 30, 0]), (hit * TICK, [6, -25, 0]),
                                  (end * TICK, [0, 0, 0]))},
    })


def slash_cross(t):
    """Kosa-uchi: cruza as duas espadas acima da cabeca e abre em X no tick do corte."""
    end, hit = t["duration_ticks"], t["windup_ticks"]
    wind = hit * 0.6
    cross = [-150, -35, -20]
    return anim(end, {
        "arm_right": {"rotation": keys((0, REST), (wind * TICK, cross), (hit * TICK, LOW), (end * TICK, REST))},
        "arm_left": {"rotation": keys((0, mirror(REST)), (wind * TICK, mirror(cross)), (hit * TICK, mirror(LOW)),
                                      (end * TICK, mirror(REST)))},
        "body": {"rotation": keys((0, [0, 0, 0]), (wind * TICK, [-8, 0, 0]), (hit * TICK, [10, 0, 0]),
                                  (end * TICK, [0, 0, 0]))},
    })


def combo(t, heavy_last=False):
    """Golpes alternando os bracos, um por tick de dano; o ultimo pode ser com os dois (finalizacao forte)."""
    end, first, step, count = t["duration_ticks"], t["windup_ticks"], t.get("hit_interval", 2), len(t["hits"])
    right, left = [(0, REST)], [(0, mirror(REST))]
    body = [(0, [0, 0, 0])]
    for i in range(count):
        hit = first + i * step
        pre = max(hit - max(step, 2) * 0.5, 0)
        last = i == count - 1
        if last and heavy_last:
            right += [(pre, [-170, 0, 15]), (hit, [-20, -20, -10])]
            left += [(pre, mirror([-170, 0, 15])), (hit, mirror([-20, -20, -10]))]
            body += [(pre, [-12, 0, 0]), (hit, [22, 0, 0])]
        elif i % 2 == 0:
            right += [(pre, HIGH), (hit, LOW)]
            body += [(hit, [8, -20, 0])]
        else:
            left += [(pre, mirror(HIGH)), (hit, mirror(LOW))]
            body += [(hit, [8, 20, 0])]
    right.append((end, REST))
    left.append((end, mirror(REST)))
    body.append((end, [0, 0, 0]))
    as_time = lambda frames: keys(*((tick * TICK, v) for tick, v in frames))
    return anim(end, {"arm_right": {"rotation": as_time(right)}, "arm_left": {"rotation": as_time(left)},
                      "body": {"rotation": as_time(body)},
                      "leg_right": {"rotation": keys((0, [0, 0, 0]), (first * TICK, [-25, 0, 0]),
                                                     (end * TICK, [0, 0, 0]))},
                      "leg_left": {"rotation": keys((0, [0, 0, 0]), (first * TICK, [15, 0, 0]),
                                                    (end * TICK, [0, 0, 0]))}})


def kasumi(t):
    """Kasumi-uchi: dois cortes leves, passo lateral (corpo inclina) e o terceiro forte com as duas espadas."""
    data = combo(t, heavy_last=True)
    hits = [t["windup_ticks"] + i * t.get("hit_interval", 2) for i in range(len(t["hits"]))]
    side = (hits[-2] + hits[-1]) / 2
    data["bones"]["body"]["rotation"].update(keys((side * TICK, [0, 0, 18])))
    return data


def counter(c):
    """Kaeshi-uchi: abaixa e gira no dash lateral; o contra-ataque sai no strike_delay_ticks."""
    strike = c["strike_delay_ticks"]
    end = strike + COUNTER_EXTRA_TICKS
    low = strike * 0.4
    return anim(end, {
        "body": {"rotation": keys((0, [0, 0, 0]), (low * TICK, [20, -120, 0]), ((strike - 1) * TICK, [10, -330, 0]),
                                  (strike * TICK, [12, -360, 0]), (end * TICK, [0, -360, 0]))},
        "arm_right": {"rotation": keys((0, REST), (low * TICK, [20, 0, 30]), ((strike - 2) * TICK, HIGH),
                                       (strike * TICK, LOW), (end * TICK, REST))},
        "arm_left": {"rotation": keys((0, mirror(REST)), (low * TICK, mirror([20, 0, 30])),
                                      (strike * TICK, mirror(LOW)), (end * TICK, mirror(REST)))},
        "leg_right": {"rotation": keys((0, [0, 0, 0]), (low * TICK, [-45, 0, 0]), (end * TICK, [0, 0, 0]))},
        "leg_left": {"rotation": keys((0, [0, 0, 0]), (low * TICK, [30, 0, 0]), (end * TICK, [0, 0, 0]))},
    })


def dash():
    mid = DASH_TICKS * 0.4
    return anim(DASH_TICKS, {
        "body": {"rotation": keys((0, [0, 0, 0]), (mid * TICK, [-25, 0, 0]), (DASH_TICKS * TICK, [0, 0, 0]))},
        "arm_right": {"rotation": keys((0, REST), (mid * TICK, [40, 0, 15]), (DASH_TICKS * TICK, REST))},
        "arm_left": {"rotation": keys((0, mirror(REST)), (mid * TICK, mirror([40, 0, 15])),
                                      (DASH_TICKS * TICK, mirror(REST)))},
        "leg_right": {"rotation": keys((0, [0, 0, 0]), (mid * TICK, [-40, 0, 0]), (DASH_TICKS * TICK, [0, 0, 0]))},
        "leg_left": {"rotation": keys((0, [0, 0, 0]), (mid * TICK, [35, 0, 0]), (DASH_TICKS * TICK, [0, 0, 0]))},
    })


def parry():
    """Espadas cruzadas a frente no instante do golpe e volta."""
    guard = [-85, -40, 0]
    return anim(PARRY_TICKS, {
        "arm_right": {"rotation": keys((0, GUARD), (2 * TICK, guard), (5 * TICK, guard), (PARRY_TICKS * TICK, REST))},
        "arm_left": {"rotation": keys((0, mirror(GUARD)), (2 * TICK, mirror(guard)), (5 * TICK, mirror(guard)),
                                      (PARRY_TICKS * TICK, mirror(REST)))},
        "body": {"rotation": keys((0, [0, 0, 0]), (2 * TICK, [-6, 0, 0]), (PARRY_TICKS * TICK, [0, 0, 0]))},
    })


def arms():
    """Duas espadas: pronto (laminas baixas a frente), andando (balanco) e em guarda com alvo."""
    ready_r, aim_r = [-25, -12, 8], [-55, -30, 0]
    out = {}
    out["hoshina.arms.blade_ready"] = {"loop": True, "animation_length": 2.0, "bones": {
        "arm_right": {"rotation": keys((0, ready_r), (1, [-27, -12, 8]), (2, ready_r))},
        "arm_left": {"rotation": keys((0, mirror(ready_r)), (1, mirror([-27, -12, 8])), (2, mirror(ready_r)))}}}
    out["hoshina.arms.blade_walk"] = {"loop": True, "animation_length": 1.0, "bones": {
        "arm_right": {"rotation": keys((0, [-15, -12, 8]), (0.5, [-40, -12, 8]), (1, [-15, -12, 8]))},
        "arm_left": {"rotation": keys((0, mirror([-40, -12, 8])), (0.5, mirror([-15, -12, 8])),
                                      (1, mirror([-40, -12, 8])))}}}
    out["hoshina.arms.blade_aim"] = {"loop": True, "animation_length": 2.0, "bones": {
        "arm_right": {"rotation": keys((0, aim_r), (1, [-57, -30, 0]), (2, aim_r))},
        "arm_left": {"rotation": keys((0, mirror([-45, -35, 0])), (1, mirror([-47, -35, 0])),
                                      (2, mirror([-45, -35, 0])))},
        "body": {"rotation": keys((0, [4, 10, 0]), (2, [4, 10, 0]))}}}
    return out


def main():
    profile = json.loads(PROFILE.read_text(encoding="utf-8"))
    techniques = profile["techniques"]
    raw = OUT.read_bytes().decode("utf-8")
    newline = "\r\n" if "\r\n" in raw else "\n"
    data = json.loads(raw)
    animations = data["animations"]
    generated = arms()
    generated["hoshina.action.kuuchi"] = slash_single(techniques["kuuchi"])
    generated["hoshina.action.kosa_uchi"] = slash_cross(techniques["kosa_uchi"])
    generated["hoshina.action.ran_uchi"] = combo(techniques["ran_uchi"])
    generated["hoshina.action.kasumi_uchi"] = kasumi(techniques["kasumi_uchi"])
    generated["hoshina.action.yae_uchi"] = combo(techniques["yae_uchi"], heavy_last=True)
    generated["hoshina.action.kaeshi_uchi"] = counter(profile["counter"])
    generated["hoshina.action.dash"] = dash()
    generated["hoshina.action.parry"] = parry()
    for animation in generated.values():
        # Keyframes em ordem de tempo (o passo lateral do Kasumi-uchi entra depois).
        for bone in animation["bones"].values():
            bone["rotation"] = dict(sorted(bone["rotation"].items(), key=lambda item: float(item[0])))
    animations.update(generated)
    text = json.dumps(data, indent=2) + "\n"
    OUT.write_bytes(text.replace("\n", newline).encode("utf-8"))
    for name in generated:
        print(f"{name}: {generated[name]['animation_length']} s")


if __name__ == "__main__":
    main()
