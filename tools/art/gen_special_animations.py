#!/usr/bin/env python3
"""Animacoes dos soldados especiais da 0.7-C: Reno (rifle), Mina (canhao no quadril) e Narumi (baioneta longa).

Rodar DEPOIS de rig_soldier_mesh.py <especie> (ele grava movement.idle/walk, action.attack, shoot/reload_rifle e as
posturas de braco do soldado comum); este script acrescenta por cima:
  - movement.run (passada mais longa e rapida);
  - arms.<familia>_ready/walk/run/aim (familia = armsFamily() da entidade: rifle, cannon, spear);
  - action.<tecnica> para cada tecnica do special_soldier/<especie>.json, com o golpe/disparo no tick do dano;
  - action.dash, action.parry e action.counter (reacoes do HoshinaEntity).
Canhao: os bracos ficam baixos (quadril) e o osso item_right gira para o cano apontar para a frente (a arma de fogo
fica alinhada ao braco, como no soldado; Rx(braco) + Rx(item) = mira).
Uso: python3 tools/art/gen_special_animations.py [especie...]   (padrao: reno mina narumi)
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from kaiju_art import kf  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/kn8/animations/entity"
PROFILES = ROOT / "src/main/resources/data/kn8/kn8/special_soldier"
TICK = 1 / 20
RUN_LENGTH = 0.6
WALK_LENGTH = 0.9
# Mira do braco direito com a arma de fogo (soldado comum, AIM["rifle"]): cano para a frente.
GUN_AIM_X = -88

# Posturas por familia: (braco direito, braco esquerdo, item_right) em graus. None = sem canal no item.
STANCES = {
    "rifle": {
        "ready": ([-48, -28, 0], [-62, 40, 0], None),
        "walk": ([-48, -28, 0], [-62, 40, 0], None),
        "run": ([-30, -20, 0], [-45, 35, 0], None),
        "aim": ([-88, -26, 0], [-92, 42, 0], None),
    },
    # Canhao pesado da Mina na altura do quadril (referencia do Miguel): bracos baixos, uma mao no punho e a outra na
    # alca de cima; o item gira para o cano ficar na horizontal.
    "cannon": {
        "ready": ([-18, 0, 0], [-40, 30, 0], [GUN_AIM_X + 18, 0, 0]),
        "walk": ([-18, 0, 0], [-40, 30, 0], [GUN_AIM_X + 18, 0, 0]),
        "run": ([-10, 0, 0], [-30, 25, 0], [GUN_AIM_X + 10, 0, 0]),
        "aim": ([-25, 0, 0], [-48, 32, 0], [GUN_AIM_X + 25, 0, 0]),
    },
    # Baioneta do Narumi (lamina como a espada): duas maos na diagonal, lamina para cima e para a frente.
    "spear": {
        "ready": ([-35, -15, 0], [-55, 40, 0], None),
        "walk": ([-35, -15, 0], [-55, 40, 0], None),
        "run": ([-20, -10, 0], [-35, 30, 0], None),
        "aim": ([-50, -20, 0], [-70, 45, 0], None),
    },
}
FAMILY = {"reno": "rifle", "mina": "cannon", "narumi": "spear"}
DASH_TICKS = 8
PARRY_TICKS = 8


def add(a, b):
    return [round(x + y, 2) for x, y in zip(a, b)]


def arms_pose(stance, length, sway, loop=True):
    right, left, item = stance
    half = length / 2
    bones = {
        "arm_right": {"rotation": kf((0, right), (half, add(right, [-sway, 0, 0])), (length, right))},
        "arm_left": {"rotation": kf((0, left), (half, add(left, [-sway, 0, 0])), (length, left))},
    }
    if item is not None:
        bones["item_right"] = {"rotation": kf((0, item), (length, item))}
    return {"loop": loop, "animation_length": length, "bones": bones}


def run(species):
    leg = 45
    return {"loop": True, "animation_length": RUN_LENGTH, "bones": {
        "leg_left": {"rotation": kf((0, [-leg, 0, 0]), (RUN_LENGTH / 2, [leg, 0, 0]), (RUN_LENGTH, [-leg, 0, 0]))},
        "leg_right": {"rotation": kf((0, [leg, 0, 0]), (RUN_LENGTH / 2, [-leg, 0, 0]), (RUN_LENGTH, [leg, 0, 0]))},
        "body": {"position": kf((0, [0, 0, 0]), (RUN_LENGTH / 4, [0, 1.0, 0]), (RUN_LENGTH / 2, [0, 0, 0]),
                                (RUN_LENGTH * 3 / 4, [0, 1.0, 0]), (RUN_LENGTH, [0, 0, 0])),
                 "rotation": kf((0, [10, 5, 0]), (RUN_LENGTH / 2, [10, -5, 0]), (RUN_LENGTH, [10, 5, 0]))},
    }}


def shot(stances, hit_ticks, duration_ticks, kick, item_kick=0.0):
    """Disparo: sobe para a mira, recua no tick de cada tiro e volta a postura de combate no fim."""
    aim_right, aim_left, aim_item = stances["aim"]
    end = duration_ticks * TICK
    right, left, body, item = [(0, aim_right)], [(0, aim_left)], [(0, [0, 0, 0])], [(0, aim_item)]
    for tick in hit_ticks:
        t = tick * TICK
        right += [(max(0.01, t - 0.05), aim_right), (t, add(aim_right, [-kick, 0, 0])),
                  (t + 0.15, aim_right)]
        left += [(max(0.01, t - 0.05), aim_left), (t, add(aim_left, [-kick * 0.7, 0, 0])), (t + 0.15, aim_left)]
        body += [(t, [-kick / 3, 0, 0]), (t + 0.2, [0, 0, 0])]
        if aim_item is not None:
            item += [(t, add(aim_item, [-item_kick, 0, 0])), (t + 0.15, aim_item)]
    bones = {"arm_right": {"rotation": kf(*sorted(right + [(end, aim_right)], key=lambda f: f[0]))},
             "arm_left": {"rotation": kf(*sorted(left + [(end, aim_left)], key=lambda f: f[0]))},
             "body": {"rotation": kf(*sorted(body + [(end, [0, 0, 0])], key=lambda f: f[0]))}}
    if aim_item is not None:
        bones["item_right"] = {"rotation": kf(*sorted(item + [(end, aim_item)], key=lambda f: f[0]))}
    return {"animation_length": round(end, 3), "bones": bones}


def charge_shot(stances, hit_tick, duration_ticks, kick):
    """Tiro carregado/Anti-Giant: abaixa o corpo e firma as pernas durante o preparo, dispara com coice forte."""
    anim = shot(stances, [hit_tick], duration_ticks, kick, kick * 0.5)
    t, end = hit_tick * TICK, duration_ticks * TICK
    anim["bones"]["body"]["position"] = kf((0, [0, 0, 0]), (t * 0.6, [0, -2.5, 0]), (t, [0, -2.5, 1.5]),
                                           (end - 0.1, [0, 0, 0]), (end, [0, 0, 0]))
    anim["bones"]["leg_left"] = {"rotation": kf((0, [0, 0, 0]), (t * 0.6, [25, 0, -8]), (end - 0.1, [25, 0, -8]),
                                                (end, [0, 0, 0]))}
    anim["bones"]["leg_right"] = {"rotation": kf((0, [0, 0, 0]), (t * 0.6, [-30, 0, 8]),
                                                 (end - 0.1, [-30, 0, 8]), (end, [0, 0, 0]))}
    return anim


def thrusts(stances, hit_ticks, duration_ticks, lunge=False):
    """Estocadas da baioneta: recolhe as duas maos e estica para a frente no tick de cada golpe."""
    ready_right, ready_left, _ = stances["aim"]
    end = duration_ticks * TICK
    back_right, back_left = add(ready_right, [25, 10, 0]), add(ready_left, [25, -10, 0])
    out_right, out_left = [-95, -5, 0], [-92, 25, 0]
    right, left, body = [(0, ready_right)], [(0, ready_left)], [(0, [0, 0, 0])]
    previous = 0.0
    for tick in hit_ticks:
        t = tick * TICK
        wind = previous + (t - previous) * 0.55
        right += [(wind, back_right), (t, out_right)]
        left += [(wind, back_left), (t, out_left)]
        body += [(wind, [-6, 10, 0]), (t, [14, -8, 0])]
        previous = t
    tail = previous + (end - previous) * 0.4
    right += [(tail, out_right), (end, ready_right)]
    left += [(tail, out_left), (end, ready_left)]
    body += [(tail, [10, 0, 0]), (end, [0, 0, 0])]
    bones = {"arm_right": {"rotation": kf(*right)}, "arm_left": {"rotation": kf(*left)},
             "body": {"rotation": kf(*body)}}
    if lunge:
        t = hit_ticks[0] * TICK
        bones["leg_right"] = {"rotation": kf((0, [0, 0, 0]), (t, [-50, 0, 5]), (end - 0.1, [-30, 0, 5]),
                                             (end, [0, 0, 0]))}
        bones["leg_left"] = {"rotation": kf((0, [0, 0, 0]), (t, [35, 0, -5]), (end - 0.1, [20, 0, -5]),
                                            (end, [0, 0, 0]))}
    return {"animation_length": round(end, 3), "bones": bones}


def sweep(stances, hit_tick, duration_ticks):
    """Varrida larga com a baioneta: carrega para a direita e corta da direita para a esquerda girando o corpo."""
    ready_right, ready_left, _ = stances["aim"]
    t, end = hit_tick * TICK, duration_ticks * TICK
    return {"animation_length": round(end, 3), "bones": {
        "arm_right": {"rotation": kf((0, ready_right), (t * 0.6, [-70, 50, 20]), (t, [-75, -60, -10]),
                                     (end - 0.1, [-60, -40, 0]), (end, ready_right))},
        "arm_left": {"rotation": kf((0, ready_left), (t * 0.6, [-60, 70, 0]), (t, [-80, -20, 0]),
                                    (end - 0.1, [-70, 10, 0]), (end, ready_left))},
        "body": {"rotation": kf((0, [0, 0, 0]), (t * 0.6, [0, 35, 0]), (t, [5, -40, 0]), (end - 0.1, [0, -20, 0]),
                                (end, [0, 0, 0]))},
    }}


def butt_strike(stances, hit_tick, duration_ticks):
    """Coronhada do Reno: puxa o rifle para tras e bate com a coronha para a frente."""
    aim_right, aim_left, _ = stances["aim"]
    t, end = hit_tick * TICK, duration_ticks * TICK
    return {"animation_length": round(end, 3), "bones": {
        "arm_right": {"rotation": kf((0, aim_right), (t * 0.6, [-40, -40, 0]), (t, [-70, 20, 0]), (end, aim_right))},
        "arm_left": {"rotation": kf((0, aim_left), (t * 0.6, [-50, 20, 0]), (t, [-80, 50, 0]), (end, aim_left))},
        "body": {"rotation": kf((0, [0, 0, 0]), (t * 0.6, [-4, 20, 0]), (t, [8, -15, 0]), (end, [0, 0, 0]))},
    }}


def dash(stances):
    ready_right, ready_left, item = stances["aim"]
    end = DASH_TICKS * TICK
    bones = {"body": {"rotation": kf((0, [0, 0, 0]), (end * 0.4, [-18, 0, 0]), (end, [0, 0, 0])),
                      "position": kf((0, [0, 0, 0]), (end * 0.4, [0, -1.5, 0]), (end, [0, 0, 0]))},
             "leg_right": {"rotation": kf((0, [0, 0, 0]), (end * 0.4, [-35, 0, 10]), (end, [0, 0, 0]))},
             "leg_left": {"rotation": kf((0, [0, 0, 0]), (end * 0.4, [30, 0, -10]), (end, [0, 0, 0]))},
             "arm_right": {"rotation": kf((0, ready_right), (end, ready_right))},
             "arm_left": {"rotation": kf((0, ready_left), (end, ready_left))}}
    if item is not None:
        bones["item_right"] = {"rotation": kf((0, item), (end, item))}
    return {"animation_length": round(end, 3), "bones": bones}


def parry(stances):
    """Aparar: a arma atravessada a frente do peito e volta."""
    ready_right, ready_left, item = stances["aim"]
    guard_right, guard_left = [-75, 45, 0], [-75, -10, 0]
    end = PARRY_TICKS * TICK
    bones = {"arm_right": {"rotation": kf((0, ready_right), (0.1, guard_right), (0.2, guard_right),
                                          (end, ready_right))},
             "arm_left": {"rotation": kf((0, ready_left), (0.1, guard_left), (0.2, guard_left), (end, ready_left))}}
    if item is not None:
        bones["item_right"] = {"rotation": kf((0, item), (end, item))}
    return {"animation_length": round(end, 3), "bones": bones}


def hit_ticks(technique):
    count = len(technique["hits"]) if technique["type"] == "combo" else 1
    step = technique.get("hit_interval", 2)
    return [technique["windup_ticks"] + i * step for i in range(count)]


def technique_animation(name, technique, family, stances):
    ticks = hit_ticks(technique)
    duration = technique["duration_ticks"]
    if technique["type"] == "slash":
        slash = technique.get("slash", {})
        big = slash.get("explosion_radius", 0) >= 3.0
        if big:
            return charge_shot(stances, ticks[0], duration, 24 if family == "cannon" else 14)
        kick = 18 if family == "cannon" else 9
        count = slash.get("count", 1)
        # Rajada (Burst Fire): os tiros saem juntos no servidor; a animacao mostra o coice repetido.
        shots = [ticks[0] + i * 2 for i in range(count)] if count > 1 and family != "spear" else ticks
        shots = [t for t in shots if t < duration - 3] or ticks
        return shot(stances, shots, duration, kick, kick * 0.4 if family == "cannon" else 0.0)
    if name == "butt_strike":
        return butt_strike(stances, ticks[0], duration)
    if name == "sweeping_slash":
        return sweep(stances, ticks[0], duration)
    return thrusts(stances, ticks, duration, lunge=technique.get("dash_in", 0) > 0)


def build(species):
    family = FAMILY[species]
    stances = STANCES[family]
    profile = json.loads((PROFILES / f"{species}.json").read_text(encoding="utf-8"))
    p = f"{species}."
    out = {p + "movement.run": run(species)}
    for stance, (length, sway) in {"ready": (3.0, 2), "walk": (WALK_LENGTH / 2, 3), "run": (RUN_LENGTH / 2, 3),
                                   "aim": (2.0, 1)}.items():
        out[f"{p}arms.{family}_{stance}"] = arms_pose(stances[stance], length, sway)
    for name, technique in profile["techniques"].items():
        out[f"{p}action.{name}"] = technique_animation(name, technique, family, stances)
    counter_tick = profile["counter"]["strike_delay_ticks"]
    out[p + "action.counter"] = (thrusts(stances, [counter_tick], counter_tick + 8) if family == "spear"
                                 else butt_strike(stances, counter_tick, counter_tick + 8))
    out[p + "action.dash"] = dash(stances)
    out[p + "action.parry"] = parry(stances)
    if family == "cannon":
        # O tiro comum (startAttack do atirador) e a recarga com o canhao no quadril, nao no ombro do rifle.
        out[p + "action.shoot_rifle"] = shot(stances, [1], 12, 18, 7)
        out[p + "action.reload_rifle"] = arms_pose(stances["ready"], 32 / 20, 6, loop=False)
    return out


def main():
    for species in sys.argv[1:] or ["reno", "mina", "narumi"]:
        path = ASSETS / f"{species}.animation.json"
        raw = path.read_bytes().decode("utf-8")
        newline = "\r\n" if "\r\n" in raw else "\n"
        data = json.loads(raw)
        generated = build(species)
        data["animations"].update(generated)
        path.write_bytes((json.dumps(data, indent=2) + "\n").replace("\n", newline).encode("utf-8"))
        print(species, ", ".join(sorted(name.split(".", 1)[1] for name in generated)))


if __name__ == "__main__":
    main()
