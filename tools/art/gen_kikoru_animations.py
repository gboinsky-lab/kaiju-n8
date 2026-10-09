#!/usr/bin/env python3
"""Animacoes da Kikoru Shinomiya (0.5.0-D8), soldado especial com o machado de duas maos (kn8:axe).

Posturas e golpes saem das mesmas poses do jogador com o machado (gen_player_animations.STANCES/COMBOS/HEAVIES/
SPECIALS da familia two_handed_axe, ja com a pegada que o Miguel ajustou no Blockbench), convertidas para os ossos
da GeckoLib (to_gecko). As tecnicas seguem os tempos de data/kn8/kn8/special_soldier/kikoru.json (o golpe visual cai
no tick do dano). A posicao do machado vem da rotacao: o mesmo ponto do cabo que o jogador segura fica no punho da
Kikoru em todas as animacoes. Membros pelo caminho curto entre poses (slerp_track).
Escreve em assets/kn8/animations/entity/kikoru.animation.json (gerado antes por rig_soldier_mesh.py kikoru; rodar
este depois dele). O que o Miguel animar em tools/blockbench/animacoes/kikoru.bbmodel vale por cima.
Uso: python3 tools/art/gen_kikoru_animations.py
"""
import json
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).parent))
from gen_player_animations import (COMBOS, HEAVIES, SPECIALS, STANCES, animate, axe_hand_point, merged,  # noqa: E402
                                   stance, stance_move, to_gecko)

ROOT = Path(__file__).resolve().parents[2]
SPECIES = "kikoru"
PROFILE = ROOT / f"src/main/resources/data/kn8/kn8/special_soldier/{SPECIES}.json"
OUT = ROOT / f"src/main/resources/assets/kn8/animations/entity/{SPECIES}.animation.json"
TICK = 1 / 20
AXE = STANCES["two_handed_axe"]
IDLE = AXE["idle"]
# Reacoes (sem tecnica no JSON): esquiva e aparar (ticks), como as do Hoshina.
DASH_TICKS = 8
PARRY_TICKS = 8
SMOOTHED_LIMBS = ("arm_right", "arm_left", "leg_right", "leg_left", "head")


def split(anim):
    """Corpo (movement) e bracos/machado (arms): controllers diferentes na entidade."""
    def part(keep):
        return dict(anim, bones={b: ch for b, ch in anim["bones"].items() if keep(b)})
    return part(lambda b: not b.startswith(("arm", "item"))), part(lambda b: b.startswith(("arm", "item")))


def strikes(moves, hits, duration, start=IDLE, extra=None):
    """Golpes em sequencia (ticks): cada um vai ao preparo (wind) e acerta no tick do JSON (hit); depois da ultima
    pancada continua, volta a postura 2 ticks antes do fim e fica (a transicao para a postura nao gira nada)."""
    frames = [(0, start)]
    previous = 0
    struck = start
    for move, hit in zip(moves, hits):
        wound = merged(start, move["wind"], (extra or {}).get("wind", {}))
        struck = merged(wound, move["hit"], (extra or {}).get("hit", {}))
        frames.append((previous + (hit - previous) * 0.6, wound))
        frames.append((hit, struck))
        previous = hit
    frames.append((previous + (duration - previous) * 0.35, struck))
    frames.append((duration - 2, start))
    frames.append((duration, start))
    return animate([(t * TICK, pose) for t, pose in frames], duration * TICK, to_gecko, vector=False)


def technique(name, moves, profile, extra=None):
    t = profile["techniques"][name]
    step = t.get("hit_interval", 2)
    count = len(t["hits"]) if t["type"] == "combo" else 1
    hits = [t["windup_ticks"] + i * step for i in range(count)]
    return strikes([moves[i % len(moves)] for i in range(count)], hits, t["duration_ticks"], extra=extra)


def dash():
    """Esquiva/avanco: corpo para tras, pernas no passo, machado junto ao corpo."""
    mid = DASH_TICKS * 0.4
    pose = merged(IDLE, {"torso": [-18, 0, 0], "right_leg": [-35, 0, 10], "left_leg": [30, 0, -10],
                         "body_pos": [0, -1.5, 0]})
    frames = [(0, IDLE), (mid, pose), (DASH_TICKS - 2, IDLE), (DASH_TICKS, IDLE)]
    return animate([(t * TICK, p) for t, p in frames], DASH_TICKS * TICK, to_gecko, vector=False)


def parry():
    """Aparar: o machado atravessado a frente do peito (a pose do saque) e volta."""
    guard = merged(IDLE, AXE["draw_via"])
    frames = [(0, IDLE), (2, guard), (4, guard), (PARRY_TICKS - 2, IDLE), (PARRY_TICKS, IDLE)]
    return animate([(t * TICK, p) for t, p in frames], PARRY_TICKS * TICK, to_gecko, vector=False)


def axe_grip_points():
    """Punho da Kikoru e ponto do cabo (Blockbench, px, em relacao ao pivo do osso item_right) que o jogador segura
    na pegada parada: mesma fracao do comprimento do cabo, medida no machado como cada um o desenha."""
    from blockbench_templates import (ASSETS, hoshina_bones, hoshina_weapon, player_hand, player_weapon, read_obj,
                                      to_bb_from_internal)

    def axis_param(vertices):
        center = vertices.mean(0)
        axis = np.linalg.svd(vertices - center)[2][0]
        along = (vertices - center) @ axis
        across = np.linalg.norm((vertices - center) - np.outer(along, axis), axis=1)
        low, high = np.quantile(along, 0.15), np.quantile(along, 0.85)
        # A cabeca (lamina larga) fica do lado de along alto: o cabo comeca do outro lado.
        if across[along > high].mean() < across[along < low].mean():
            axis, along = -axis, -along
        return center, axis, along.min(), along.max()

    player = np.array(player_weapon("axe", "right", 64)["vertices"], float)
    center, axis, lo, hi = axis_param(player)
    grip = player_hand("right") + axe_hand_point()
    fraction = ((grip - center) @ axis - lo) / (hi - lo)
    bones = hoshina_bones(SPECIES)
    pivot = np.array([bones["item_right"]["pivot"][0], bones["item_right"]["pivot"][1],
                      -bones["item_right"]["pivot"][2]])
    shoulder = np.array([bones["arm_right"]["pivot"][0], bones["arm_right"]["pivot"][1],
                         -bones["arm_right"]["pivot"][2]])
    index = json.loads((ASSETS / f"meshes/{SPECIES}.json").read_text(encoding="utf-8"))["bones"]
    arm = to_bb_from_internal(read_obj(ASSETS / index["arm_right"].split(":")[1])[0])
    direction = (pivot - shoulder) / np.linalg.norm(pivot - shoulder)
    reach = (arm - shoulder) @ direction
    fist = arm[reach > reach.max() - 3].mean(0)
    axe = np.array(hoshina_weapon(bones, "item_right", "axe", 1024)["vertices"], float)
    center, axis, lo, hi = axis_param(axe)
    target = lo + fraction * (hi - lo)
    along = (axe - center) @ axis
    handle = axe[np.abs(along - target) < 0.75].mean(0)
    return {"item_right": (fist - pivot, handle - pivot)}, round(float(fraction), 3)


def densify_items(anim):
    """Rotacao do machado a cada quarto de tick (o jogo interpola os numeros em linha reta: as amostras sao a mesma
    curva) para a posicao calculada pela rotacao acompanhar o giro inteiro, nao so os keyframes."""
    from blockbench_templates import SAMPLE
    channel = anim["bones"].get("item_right", {}).get("rotation")
    if not channel:
        return
    keys = sorted((float(t), v) for t, v in channel.items())
    out = {}
    for (t0, a), (t1, b) in zip(keys, keys[1:]):
        steps = max(1, round((t1 - t0) / SAMPLE)) if t1 > t0 and a != b else 1
        for i in range(steps):
            f = i / steps
            out[f"{round(t0 + (t1 - t0) * f, 4)}"] = [round(x + (y - x) * f, 3) for x, y in zip(a, b)]
    out[f"{round(keys[-1][0], 4)}"] = keys[-1][1]
    anim["bones"]["item_right"]["rotation"] = out


def smooth(anim):
    from blockbench_templates import slerp_track
    for bone in SMOOTHED_LIMBS:
        channel = anim["bones"].get(bone, {}).get("rotation")
        if channel and len(channel) > 1:
            frames = sorted((float(t), v) for t, v in channel.items())
            anim["bones"][bone]["rotation"] = {f"{round(t, 4)}": v for t, v in slerp_track(frames)}


def main():
    profile = json.loads(PROFILE.read_text(encoding="utf-8"))
    combo, heavy, slam = COMBOS["two_handed_axe"], HEAVIES["two_handed_axe"], SPECIALS["two_handed_axe"]
    lunge = {"hit": {"torso": [28, 0, 0], "body_pos": [0, -3, 0], "right_leg": [-40, 0, 10], "left_leg": [30, 0, -10]}}
    generated = {}
    for name, built in (("idle", stance(IDLE, to_gecko, vector=False)),
                        ("walk", stance_move(AXE["move"], to_gecko, vector=False)),
                        ("run", stance_move(AXE["run"], to_gecko, vector=False))):
        movement, arms = split(built)
        generated[f"{SPECIES}.movement.{name}"] = movement
        generated[f"{SPECIES}.arms.axe_{dict(idle='ready', walk='walk', run='run')[name]}"] = arms
    # Em combate (aim) fica na guarda parada, como o Hoshina.
    generated[f"{SPECIES}.arms.axe_aim"] = json.loads(json.dumps(generated[f"{SPECIES}.arms.axe_ready"]))
    actions = {
        # Axe Slash: as duas varridas do combo do jogador (baixa e no peito).
        "axe_slash": technique("axe_slash", combo, profile),
        # Heavy Swing: o pesado de cima.
        "heavy_swing": technique("heavy_swing", [heavy], profile),
        # Shockwave: varrida baixa rente ao chao que solta a onda.
        "shockwave": technique("shockwave", [combo[0]], profile, extra={"hit": {"body_pos": [0, -3.5, 0]}}),
        # Dash Strike: avanco com a varrida no peito.
        "dash_strike": technique("dash_strike", [combo[1]], profile, extra=lunge),
        # Ground Smash: o Axe Slam do jogador (sobe e crava o machado no chao).
        "ground_smash": technique("ground_smash", [slam], profile),
        # Guard Break: o pesado de cima com avanco.
        "guard_break": technique("guard_break", [heavy], profile, extra=lunge),
        # Contra-ataque: varrida baixa.
        "counter": strikes([combo[0]], [profile["counter"]["strike_delay_ticks"]],
                           profile["counter"]["strike_delay_ticks"] + 8),
        "dash": dash(),
        "parry": parry(),
    }
    generated[f"{SPECIES}.action.attack"] = technique("axe_slash", combo[:1], dict(
        techniques={"axe_slash": {"type": "combo", "hits": [1.0], "windup_ticks": 5, "duration_ticks": 12}}))
    for name, anim in actions.items():
        generated[f"{SPECIES}.action.{name}"] = anim
    from blockbench_templates import grip_gecko, hoshina_bones, hoshina_frames, imported_special
    generated.update(imported_special(SPECIES))
    points, fraction = axe_grip_points()
    frames = hoshina_frames(hoshina_bones(SPECIES))
    for name, anim in generated.items():
        if ".action." in name:
            smooth(anim)
        if "item_right" in anim["bones"]:
            densify_items(anim)
            grip_gecko(anim, frames, points)
    raw = OUT.read_bytes().decode("utf-8")
    newline = "\r\n" if "\r\n" in raw else "\n"
    data = json.loads(raw)
    data["animations"].update(generated)
    OUT.write_bytes((json.dumps(data, indent=2) + "\n").replace("\n", newline).encode("utf-8"))
    print(f"machado seguro a {fraction:.0%} do cabo (como o jogador)")
    for name in sorted(generated):
        print(f"{name}: {generated[name]['animation_length']} s")


if __name__ == "__main__":
    main()
