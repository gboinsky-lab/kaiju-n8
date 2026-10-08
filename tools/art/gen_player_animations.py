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

# 0.5.0-D: postura de cada familia (bracos, tronco e item; PAL tem right_item/left_item). Pernas ficam com o vanilla
# (andar continua normal). Graus no padrao do PT6: X negativo = braco para a frente.
STANCES = {
    # Faca: guarda baixa e curta, mao esquerda protegendo.
    "knife": {"right_arm": [-35, -12, 8], "left_arm": [-25, 18, -6], "torso": [6, 8, 0]},
    # Espada de uma mao: lamina na frente do corpo, um pouco erguida.
    "sword": {"right_arm": [-42, -18, 12], "left_arm": [-12, 10, -4], "torso": [4, 10, 0]},
    # Hoshina (Miguel): postura baixa e agachada, as duas laminas invertidas (pegada reversa, lamina para tras).
    "dual_reverse": {"right_arm": [-38, -22, 18], "left_arm": [-38, 22, -18], "torso": [16, 0, 0],
                     "right_item": [180, 0, 0], "left_item": [180, 0, 0]},
    # Machado pesado: duas maos no cabo, de lado, pronto para o giro.
    "two_handed_axe": {"right_arm": [-48, -30, 0], "left_arm": [-55, 38, 0], "torso": [6, 22, 0]},
}


def stance(pose):
    """Postura em laco com uma respiracao de 2 graus (segura a pose; os golpes passam por cima)."""
    breath = {bone: [v[0] - 2, v[1], v[2]] if bone.endswith("arm") else v for bone, v in pose.items()}
    return {"loop": True, "animation_length": 2.0, "bones": {
        bone: {"rotation": keys((0, v), (1.0, breath[bone]), (2.0, v))} for bone, v in pose.items()}}


def draw(pose, ticks, hands):
    """Saque: a mao vai a bainha/coldre (quadril ou costas) e traz a arma ate a postura no tempo do perfil."""
    end = max(ticks, 2) * TICK
    reach = end * 0.4
    target = pose or {"right_arm": [-60, -8, 0], "left_arm": [-55, 25, 0]}
    bones = {"right_arm": {"rotation": keys((0, [0, 0, 0]), (reach, [25, 20, 25]), (end, target["right_arm"]))}}
    if hands != "one" or "left_arm" in target:
        left_from = [25, -20, -25] if hands == "dual" else [0, 0, 0]
        bones["left_arm"] = {"rotation": keys((0, [0, 0, 0]), (reach, left_from), (end, target["left_arm"]))}
    if "torso" in target:
        bones["torso"] = {"rotation": keys((0, [0, 0, 0]), (reach, [0, -10, 0]), (end, target["torso"]))}
    for item in ("right_item", "left_item"):
        if item in target:
            bones[item] = {"rotation": keys((0, [0, 0, 0]), (reach, [90, 0, 0]), (end, target[item]))}
    return {"animation_length": round(end, 3), "bones": bones}


def guard(hands):
    """Guarda por familia (segura ate o servidor soltar): uma mao na frente; duas maos com a arma atravessada;
    duas laminas cruzadas em X."""
    up = BLOCK_RAISE_TICKS * TICK
    if hands == "dual":
        right, left, items = [-95, -40, 0], [-95, 40, 0], True
    elif hands == "two":
        right, left, items = [-80, -45, 0], [-80, 45, 0], False
    else:
        right, left, items = [-85, -25, 15], [-40, 20, 0], False
    bones = {"right_arm": {"rotation": keys((0, [0, 0, 0]), (up, right))},
             "left_arm": {"rotation": keys((0, [0, 0, 0]), (up, left))},
             "torso": {"rotation": keys((0, [0, 0, 0]), (up, [8, 0, 0]))}}
    if items:
        bones["right_item"] = {"rotation": keys((0, [0, 0, 0]), (up, [0, 0, -35]))}
        bones["left_item"] = {"rotation": keys((0, [0, 0, 0]), (up, [0, 0, 35]))}
    return {"loop": "hold_on_last_frame", "animation_length": round(up, 3), "bones": bones}


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
        pose = STANCES.get(name) if profile.get("stance", True) else None
        if pose:
            result[f"player.{name}.stance"] = stance(pose)
        result[f"player.{name}.draw"] = draw(pose, profile.get("draw", {}).get("ticks", 0), hands)
        result[f"player.{name}.guard"] = guard(hands)
        if "reload" in profile:
            result[f"player.{name}.reload"] = reload(profile["reload"]["stages"], hands)
    return result


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
        if special and special["type"] == "ground_slam":
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
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
    for name, anim in data["animations"].items():
        print(f"{name}: {anim['animation_length']} s")


if __name__ == "__main__":
    main()
