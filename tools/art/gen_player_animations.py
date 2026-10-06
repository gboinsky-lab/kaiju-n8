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


def shoot(action):
    """M10b: mira com os dois bracos e coice no tick de impacto do JSON do rifle."""
    end, hit = action["duration_ticks"] * TICK, action["impact_tick"] * TICK
    aim = [-90, -8, 0]
    recoil = [-115, -8, 0]
    return {"animation_length": round(end, 3), "bones": {
        "right_arm": {"rotation": keys((0, aim), (hit, recoil), (end, aim))},
        "left_arm": {"rotation": keys((0, [-85, 25, 0]), (hit, [-105, 25, 0]), (end, [-85, 25, 0]))},
        "head": {"rotation": keys((0, [0, 0, 0]), (hit, [-4, 0, 0]), (end, [0, 0, 0]))},
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


def weapon_animations():
    """Uma animacao por acao de cada arma (player.<item>.<acao>), com os tempos do JSON da propria arma."""
    result = {}
    for path in sorted(WEAPONS.glob("*.json")):
        weapon = json.loads(path.read_text(encoding="utf-8"))
        item = weapon["item"].split(":", 1)[1]
        actions = weapon["actions"]
        if weapon["style"] == "firearm":
            result[f"player.{item}.shoot"] = shoot(actions["light"])
            continue
        if "light" in actions:
            result[f"player.{item}.light"] = light(actions["light"])
        if "heavy" in actions:
            result[f"player.{item}.heavy"] = heavy(actions["heavy"])
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
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
    for name, anim in data["animations"].items():
        print(f"{name}: {anim['animation_length']} s")


if __name__ == "__main__":
    main()
