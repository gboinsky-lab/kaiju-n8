#!/usr/bin/env python3
"""RIG-1: animacoes dos humanoides com ossos articulados (rig_from_skin.py: peito, cotovelo, punho, joelho, pe).

Miguel (2026-10-29): "nao quero modelos travados que parecem que nem estao atacando direito"; as animacoes que vieram
do Meshy nao sao usadas. Aqui as animacoes que ja existiam (mesmos nomes, mesmos tempos de impacto do JSON) sao
refeitas com o corpo todo: guarda de punhos com os cotovelos dobrados, golpe que sai do quadril e do peito girando
(antecipacao, impacto rapido, recuperacao), joelhos dobrando ao andar/correr, pes acompanhando o chao.

Convencao (graus, como no Blockbench/GeckoLib, conferida no preview_rig.py):
  X negativo levanta o membro para a frente; forearm X negativo dobra o cotovelo; shin X positivo dobra o joelho;
  foot X negativo levanta a ponta do pe; body/chest X positivo inclina para a frente; body/chest Y positivo gira para
  a direita (ombro direito para tras); braco direito Z positivo abre para fora (esquerdo: negativo).
Curvas: cada keyframe leva a "easing" da GeckoLib para chegar nele (golpe: easeincubic, acelera ate o impacto;
preparo: easeoutcubic; volta: easeinoutsine).

Rodar DEPOIS dos outros geradores da especie (rig_*, gen_special_animations, gen_ability_animations,
gen_vault_animations): estas animacoes substituem as de mesmo nome.
Uso: python3 tools/art/humanoid_anims.py [especie...]   (padrao: todas as de SPECIES)
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from gen_ability_animations import ability, multi_hits  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/kn8/animations/entity"
PROFILES = ROOT / "src/main/resources/data/kn8/kn8/special_soldier"
TICK = 1 / 20
ZERO = [0, 0, 0]
CENTER = ("body", "chest", "head", "root")
STRIKE, WIND, BACK, HOLD = "easeincubic", "easeoutcubic", "easeinoutsine", "linear"


# ------------------------------------------------------------------------------------------------ poses
def mirror(pose):
    """Mesmo gesto do outro lado: troca esquerda/direita e inverte Y e Z."""
    out = {}
    for bone, value in pose.items():
        if bone == "body_pos":
            out[bone] = [-value[0], value[1], value[2]]
            continue
        other = bone.replace("_left", "_TMP").replace("_right", "_left").replace("_TMP", "_right")
        out[other] = [value[0], -value[1], -value[2]]
    return out


def merge(*poses):
    """Poses somadas por cima umas das outras (a ultima vence em cada osso)."""
    out = {}
    for pose in poses:
        out.update(pose)
    return out


def offset(pose, delta):
    """Soma angulos osso a osso (para variar uma pose base sem reescreve-la)."""
    out = dict(pose)
    for bone, value in delta.items():
        out[bone] = [round(a + b, 2) for a, b in zip(out.get(bone, ZERO), value)]
    return out


def timeline(length, frames, loop=False):
    """frames: lista de (tempo, pose, easing). Osso ausente num quadro mantem a pose do quadro anterior."""
    bones = sorted({b for _, pose, _ in frames for b in pose})
    channels = {}
    for bone in bones:
        last = None
        keys = {}
        for t, pose, easing in frames:
            value = pose.get(bone, last if last is not None else ZERO)
            last = value
            stamp = "0.0" if t == 0 else f"{round(t, 3)}"
            keys[stamp] = [round(v, 2) for v in value] if t == 0 else {"vector": [round(v, 2) for v in value],
                                                                          "easing": easing}
        target = "body" if bone == "body_pos" else bone
        channels.setdefault(target, {})["position" if bone == "body_pos" else "rotation"] = keys
    out = {"animation_length": round(length, 3), "bones": channels}
    if loop:
        out["loop"] = True
    return out


# ------------------------------------------------------------------------------------------------ locomocao
def gait(length, base, stride, knee, swing, elbow, bob, lean=0.0, twist=4.0, arms=True, foot=True):
    """Ciclo de passos: contato (perna da frente esticada, calcanhar), apoio, passagem (joelho dobrado subindo) e de
    novo, espelhado. Bracos balancam ao contrario das pernas, cotovelo mais dobrado quando o braco vai a frente."""
    def contact(front):
        back = "right" if front == "left" else "left"
        pose = {f"leg_{front}": [-stride, 0, 0], f"shin_{front}": [4, 0, 0],
                f"leg_{back}": [stride * 0.75, 0, 0], f"shin_{back}": [knee * 0.35, 0, 0],
                "body_pos": [0, -bob, 0], "body": [lean, twist if front == "left" else -twist, 0],
                "chest": [lean * 0.4, -twist * 2 if front == "left" else twist * 2, 0]}
        if foot:
            pose[f"foot_{front}"] = [-12, 0, 0]
            pose[f"foot_{back}"] = [14, 0, 0]
        if arms:
            pose[f"arm_{front}"] = [swing, 0, 0]
            pose[f"arm_{back}"] = [-swing, 0, 0]
            pose[f"forearm_{front}"] = [-elbow * 0.6, 0, 0]
            pose[f"forearm_{back}"] = [-elbow * 1.4, 0, 0]
        return pose

    def passing(lifted):
        down = "right" if lifted == "left" else "left"
        pose = {f"leg_{lifted}": [-stride * 0.25, 0, 0], f"shin_{lifted}": [knee, 0, 0],
                f"leg_{down}": [stride * 0.1, 0, 0], f"shin_{down}": [6, 0, 0],
                "body_pos": [0, bob * 0.6, 0], "body": [lean, 0, 0], "chest": [lean * 0.4, 0, 0]}
        if foot:
            pose[f"foot_{lifted}"] = [-knee * 0.25, 0, 0]
            pose[f"foot_{down}"] = [-4, 0, 0]
        if arms:
            pose[f"arm_{lifted}"] = [0, 0, 0]
            pose[f"arm_{down}"] = [0, 0, 0]
            pose[f"forearm_{lifted}"] = [-elbow, 0, 0]
            pose[f"forearm_{down}"] = [-elbow, 0, 0]
        return pose
    q = length / 4
    steps = [contact("left"), passing("right"), contact("right"), passing("left"), contact("left")]
    frames = [(i * q, offset(base, step), BACK) for i, step in enumerate(steps)]
    return timeline(length, frames, loop=True)


def breathing(length, base, depth=2.0, arms=True):
    """Parado em guarda: o peito respira, o corpo balanca de leve; nunca congelado."""
    inhale = offset(base, {"chest": [-depth, 0, 0], "head": [depth * 0.5, 0, 0], "body_pos": [0, 0.2, 0]})
    if arms:
        inhale = offset(inhale, {"arm_left": [-depth, 0, 0], "arm_right": [-depth, 0, 0]})
    return timeline(length, [(0, base, BACK), (length / 2, inhale, BACK), (length, base, BACK)], loop=True)


# ------------------------------------------------------------------------------------------------ golpes
def punch_pose(side, base, power=1.0, height=0.0, cross=10.0):
    """Soco reto com o braco do lado: braco esticado na altura do ombro, peito e quadril girando para o golpe."""
    sign = 1 if side == "right" else -1
    other = "left" if side == "right" else "right"
    return offset(merge(base, {
        f"arm_{side}": [-88 - height, -cross * sign, 4 * sign], f"forearm_{side}": [-4, 0, 0],
        f"hand_{side}": [0, 0, 0],
        f"arm_{other}": base.get(f"arm_{other}", ZERO), f"forearm_{other}": base.get(f"forearm_{other}", ZERO),
    }), {"chest": [4 * power, -24 * power * sign, 0], "body": [3 * power, -10 * power * sign, 0],
         "head": [0, 14 * power * sign, 0]})


def cock_pose(side, base, power=1.0):
    """Preparo do soco: cotovelo para tras, punho junto ao rosto, peito girando para o lado do braco."""
    sign = 1 if side == "right" else -1
    return offset(merge(base, {f"arm_{side}": [-15, 25 * sign, 20 * sign], f"forearm_{side}": [-120, 0, 0]}),
                  {"chest": [-3 * power, 26 * power * sign, 0], "body": [0, 12 * power * sign, 0],
                   "head": [0, -16 * power * sign, 0], "body_pos": [0, -0.8 * power, 0]})


def lunge(base, depth=1.0, lead="left"):
    """Passo de golpe pesado: perna da frente dobrada, de tras esticada, quadril baixo."""
    back = "right" if lead == "left" else "left"
    return offset(base, {f"leg_{lead}": [-32 * depth, 0, 0], f"shin_{lead}": [38 * depth, 0, 0],
                         f"foot_{lead}": [-6 * depth, 0, 0],
                         f"leg_{back}": [22 * depth, 0, 0], f"shin_{back}": [8 * depth, 0, 0],
                         f"foot_{back}": [12 * depth, 0, 0], "body_pos": [0, -2.0 * depth, -1.0 * depth]})


def combo(base, hits, end, sides=("left", "right"), power=1.0, height=0.0):
    """Socos alternados: cada golpe estica no tick do JSON e volta meio caminho para a guarda antes do proximo."""
    frames = [(0, base, BACK)]
    previous = 0.0
    for i, t in enumerate(hits):
        side = sides[i % len(sides)]
        gap = t - previous
        frames.append((t - min(gap * 0.6, 0.12), cock_pose(side, base, power * 0.6), WIND))
        frames.append((t, punch_pose(side, base, power, height), STRIKE))
        previous = t
    frames.append((min(end, previous + 0.12), punch_pose(sides[(len(hits) - 1) % len(sides)], base, power * 0.8,
                                                       height), HOLD))
    frames.append((end, base, BACK))
    return timeline(end, frames)


# ------------------------------------------------------------------------------------------------ COMBATE-4
# Squadron Style do Isao (documento de referencias: Barrage Blast = cotovelada curta, Twin Blast com os dois punhos,
# Rising Blast = uppercut) e o Thunder Emperor do Miguel (barragem de socos fortes, o ultimo maior). Servem ao Isao e a
# forma do No. 9 com o Isao. Poses inferidas [INFERENCIA].
def elbow_anim(base, hit, end, power=1.0):
    """Cotovelada: cotovelo direito recolhido atras e o tronco girando para meter o cotovelo no alvo com passo."""
    wind = offset(base, {"arm_right": [-60, 50 * power, 40], "forearm_right": [-140, 0, 0], "chest": [0, 38 * power, 0],
                         "body": [0, 16 * power, 0], "head": [0, -15, 0]})
    impact = offset(lunge(base, 0.7 * power), {"arm_right": [-55, -78 * power, 40], "forearm_right": [-148, 0, 0],
                                               "chest": [8, -48 * power, 0], "body": [5, -20 * power, 0],
                                               "head": [0, 20, 0]})
    return timeline(end, [(0, base, BACK), (hit * 0.65, wind, WIND), (hit, impact, STRIKE),
                          (hit + 0.15, impact, HOLD), (end, base, BACK)])


def twin_anim(base, hit, end, power=1.0):
    """Twin Blast: os dois punhos recolhidos juntos e disparados ao mesmo tempo para a frente."""
    load = offset(base, {"arm_left": [-10, -20, -25], "arm_right": [-10, 20, 25], "forearm_left": [-120, 0, 0],
                         "forearm_right": [-120, 0, 0], "chest": [-8, 0, 0], "body_pos": [0, -1.0, 0.6]})
    blast = offset(lunge(base, 0.9 * power), {"arm_left": [-58, 8, -6], "arm_right": [-58, -8, 6],
                                              "forearm_left": [-4, 0, 0], "forearm_right": [-4, 0, 0],
                                              "chest": [12, 0, 0], "body": [6, 0, 0]})
    blast = merge(blast, {"arm_left": [-92, 8, -6], "arm_right": [-92, -8, 6]})
    return timeline(end, [(0, base, BACK), (hit * 0.7, load, WIND), (hit, blast, STRIKE), (hit + 0.2, blast, HOLD),
                          (end, base, BACK)])


def rising_anim(base, hit, end, power=1.0):
    """Rising Blast: agacha com o punho direito baixo e sobe num uppercut, o corpo esticando para cima."""
    crouch = offset(base, {"arm_right": [30, 20, 20], "forearm_right": [-70, 0, 0], "chest": [14, 25, 0],
                           "body": [10, 10, 0], "body_pos": [0, -2.5 * power, 0], "leg_left": [-22, 0, 0],
                           "shin_left": [40, 0, 0], "leg_right": [-10, 0, 0], "shin_right": [35, 0, 0]})
    upper = offset(base, {"arm_right": [-165, -10, 0], "forearm_right": [-35, 0, 0], "chest": [-16, -20, 0],
                          "body": [-6, -8, 0], "head": [-18, 0, 0], "body_pos": [0, 1.2 * power, 0],
                          "leg_right": [12, 0, 0], "foot_right": [25, 0, 0]})
    return timeline(end, [(0, base, BACK), (hit * 0.65, crouch, WIND), (hit, upper, STRIKE),
                          (hit + 0.2, upper, HOLD), (end, base, BACK)])


def thunder_barrage(base, hits, end, power=1.0):
    """Thunder Emperor (Miguel, 2026-10-29): barragem de socos fortes alternados e o ultimo, maior, de corpo
    inteiro."""
    frames = [(0, base, BACK)]
    previous = 0.0
    for i, t in enumerate(hits[:-1]):
        side = ("left", "right")[i % 2]
        gap = t - previous
        frames.append((t - min(gap * 0.5, 0.05), cock_pose(side, base, power * 0.7), WIND))
        frames.append((t, punch_pose(side, lunge(base, 0.5), power * 1.3, height=(i % 3 - 1) * 12), STRIKE))
        previous = t
    last = hits[-1]
    coil = offset(lunge(base, 0.6), {"arm_right": [25, 35, 35], "forearm_right": [-125, 0, 0],
                                     "chest": [-6, 55, 0], "body": [0, 25, 0], "head": [0, -35, 0]})
    strike = offset(punch_pose("right", lunge(base, 1.2), 2.0 * power), {"arm_left": [60, 0, -20],
                                                                        "forearm_left": [-90, 0, 0]})
    frames += [(last - min(last - previous, 0.15) * 0.8, coil, WIND), (last, strike, STRIKE),
               (last + 0.25, strike, HOLD), (end, base, BACK)]
    return timeline(end, frames)


# ------------------------------------------------------------------------------------------------ Isao
def technique(profile, name):
    t = profile["techniques"][name]
    count = len(t["hits"]) if t["type"] == "combo" else 1
    step = t.get("hit_interval", 2)
    return [(t["windup_ticks"] + i * step) * TICK for i in range(count)], t["duration_ticks"] * TICK


# Guarda do Isao (boxeador com as manoplas: punhos na altura do queixo, cotovelos fechados, joelhos soltos).
ISAO_GUARD = {"arm_left": [-40, 14, 10], "forearm_left": [-98, 0, 0], "hand_left": [0, 0, 0],
              "arm_right": [-32, -16, -12], "forearm_right": [-108, 0, 0], "hand_right": [0, 0, 0]}
ISAO_STANCE = {"body": [4, 8, 0], "chest": [4, -6, 0], "head": [-4, -4, 0],
               "leg_left": [-10, 0, -3], "shin_left": [12, 0, 0], "foot_left": [-2, 0, 0],
               "leg_right": [8, 0, 3], "shin_right": [10, 0, 0], "foot_right": [-6, 0, 0], "body_pos": [0, -0.5, 0]}
ISAO_READY = {"arm_left": [-18, 6, 6], "forearm_left": [-55, 0, 0], "arm_right": [-14, -8, -6],
              "forearm_right": [-60, 0, 0]}


def isao(profile):
    p = "isao."
    guard = merge(ISAO_STANCE, ISAO_GUARD)
    out = {}
    # Camada de movimento (pernas, quadril e peito) e de bracos (guarda) sao controllers separados.
    legs_only = {k: v for k, v in ISAO_STANCE.items()}
    out[p + "movement.idle"] = breathing(3.0, legs_only, arms=False)
    out[p + "movement.walk"] = gait(0.9, {"body": [3, 0, 0], "chest": [2, 0, 0]}, stride=26, knee=48, swing=0,
                                    elbow=0, bob=0.6, lean=3, arms=False)
    out[p + "movement.run"] = gait(0.6, {"body": [12, 0, 0], "chest": [6, 0, 0], "head": [-10, 0, 0]}, stride=46,
                                   knee=95, swing=0, elbow=0, bob=1.4, lean=12, twist=6, arms=False)
    arms = {"ready": (ISAO_READY, 3.0, 2.0), "walk": (ISAO_GUARD, 0.45, 2.5), "run": (ISAO_GUARD, 0.3, 3.0),
            "aim": (ISAO_GUARD, 2.0, 1.5)}
    for name, (pose, length, sway) in arms.items():
        up = offset(pose, {"arm_left": [-sway, 0, 0], "arm_right": [-sway, 0, 0],
                           "forearm_left": [-sway, 0, 0], "forearm_right": [sway, 0, 0]})
        out[f"{p}arms.fists_{name}"] = timeline(length, [(0, pose, BACK), (length / 2, up, BACK),
                                                         (length, pose, BACK)], loop=True)
    techniques = profile["techniques"]
    if "barrage_blast" in techniques:
        # COMBATE-4: Barrage Blast da obra = cotovelada curta.
        (hit,), end = technique(profile, "barrage_blast")
        out[p + "action.barrage_blast"] = elbow_anim(guard, hit, end, 1.2)
    if "twin_blast" in techniques:
        (hit,), end = technique(profile, "twin_blast")
        out[p + "action.twin_blast"] = twin_anim(guard, hit, end)
    if "rising_blast" in techniques:
        (hit,), end = technique(profile, "rising_blast")
        out[p + "action.rising_blast"] = rising_anim(guard, hit, end)
    if "jab_combo" in techniques:
        hits, end = technique(profile, "jab_combo")
        out[p + "action.jab_combo"] = combo(guard, hits, end, power=0.8)
    if "spiral_blast" in techniques:
        (hit,), end = technique(profile, "spiral_blast")
        impact = offset(punch_pose("right", lunge(guard, 0.6), 1.3), {"hand_right": [0, 0, -90],
                                                                       "forearm_right": [0, -40, 0]})
        out[p + "action.spiral_blast"] = timeline(end, [
            (0, guard, BACK), (hit * 0.75, offset(cock_pose("right", guard, 1.3), {"hand_right": [0, 0, 60]}), WIND),
            (hit, impact, STRIKE), (hit + 0.2, impact, HOLD), (end, guard, BACK)])
    if "heavy_straight" in techniques:
        (hit,), end = technique(profile, "heavy_straight")
        cocked = cock_pose("right", lunge(guard, 0.4), 1.5)
        impact = punch_pose("right", lunge(guard, 1.0), 1.5)
        out[p + "action.heavy_straight"] = timeline(end, [
            (0, guard, BACK), (hit * 0.7, cocked, WIND), (hit, impact, STRIKE),
            (hit + 0.2, offset(impact, {"chest": [3, -6, 0]}), HOLD), (end, guard, BACK)])
    if "elbow_strike" in techniques:
        (hit,), end = technique(profile, "elbow_strike")
        wind = offset(guard, {"arm_right": [-60, 50, 40], "forearm_right": [-140, 0, 0], "chest": [0, 35, 0],
                              "body": [0, 15, 0], "head": [0, -15, 0]})
        impact = offset(lunge(guard, 0.6), {"arm_right": [-55, -75, 40], "forearm_right": [-145, 0, 0],
                                            "chest": [6, -45, 0], "body": [4, -18, 0], "head": [0, 20, 0]})
        out[p + "action.elbow_strike"] = timeline(end, [
            (0, guard, BACK), (hit * 0.7, wind, WIND), (hit, impact, STRIKE), (hit + 0.15, impact, HOLD),
            (end, guard, BACK)])
    if "force_blast" in techniques:
        (hit,), end = technique(profile, "force_blast")
        load = offset(guard, {"arm_left": [70, -20, -30], "arm_right": [70, 20, 30], "forearm_left": [-30, 0, 0],
                              "forearm_right": [-30, 0, 0], "chest": [-12, 0, 0], "head": [-8, 0, 0],
                              "body_pos": [0, -2.0, 0], "shin_left": [20, 0, 0], "shin_right": [20, 0, 0],
                              "leg_left": [-14, 0, 0], "leg_right": [-10, 0, 0]})
        blast = offset(lunge(guard, 0.8), {"arm_left": [-30, 30, -40], "arm_right": [-30, -30, 40],
                                           "forearm_left": [80, 0, 0], "forearm_right": [88, 0, 0],
                                           "chest": [12, 0, 0], "body": [6, 0, 0], "head": [-6, 0, 0]})
        out[p + "action.force_blast"] = timeline(end, [
            (0, guard, BACK), (hit * 0.8, load, WIND), (hit, blast, STRIKE), (hit + 0.25, blast, HOLD),
            (end, guard, BACK)])
    if "thunder_emperor" in techniques:
        # COMBATE-4: barragem de socos com o ultimo maior (Miguel).
        hits, end = technique(profile, "thunder_emperor")
        out[p + "action.thunder_emperor"] = thunder_barrage(guard, hits, end)
    counter_hit = profile["counter"]["strike_delay_ticks"] * TICK
    counter_end = counter_hit + 8 * TICK
    hook_wind = offset(guard, {"arm_left": [-20, -20, -60], "forearm_left": [-90, 0, 0], "chest": [0, -25, 0],
                               "body_pos": [0, -1.2, 0]})
    hook = offset(guard, {"arm_left": [-80, 60, -70], "forearm_left": [-95, 0, 0], "chest": [4, 30, 0],
                          "body": [2, 12, 0], "head": [0, -12, 0]})
    out[p + "action.counter"] = timeline(counter_end, [
        (0, guard, BACK), (counter_hit * 0.6, hook_wind, WIND), (counter_hit, hook, STRIKE),
        (counter_hit + 0.1, hook, HOLD), (counter_end, guard, BACK)])
    dash_end = 8 * TICK
    dash = offset(guard, {"body": [18, 0, 0], "chest": [6, 0, 0], "head": [-14, 0, 0], "body_pos": [0, -1.5, 0],
                          "leg_left": [-40, 0, 0], "shin_left": [55, 0, 0], "leg_right": [35, 0, 0],
                          "shin_right": [20, 0, 0], "foot_right": [20, 0, 0]})
    out[p + "action.dash"] = timeline(dash_end, [(0, guard, BACK), (dash_end * 0.4, dash, WIND),
                                                 (dash_end, guard, BACK)])
    parry_end = 8 * TICK
    block = offset(guard, {"arm_left": [-55, -30, -5], "forearm_left": [-30, 0, 0], "arm_right": [-55, 30, 5],
                           "forearm_right": [-35, 0, 0], "hand_left": [0, 0, -40], "hand_right": [0, 0, 40],
                           "chest": [-4, 0, 0], "head": [10, 0, 0], "body_pos": [0, -0.8, 0.6]})
    block = merge(block, {"forearm_left": [-128, 0, 0], "forearm_right": [-128, 0, 0],
                          "arm_left": [-60, 52, 10], "arm_right": [-60, -52, -10]})
    out[p + "action.parry"] = timeline(parry_end, [(0, guard, BACK), (0.1, block, WIND), (0.2, block, HOLD),
                                                   (parry_end, guard, BACK)])
    # Golpe comum (sem tecnica): jab da guarda.
    jab = 0.25
    out[p + "action.attack"] = timeline(0.6, [(0, guard, BACK), (jab * 0.5, cock_pose("left", guard, 0.5), WIND),
                                             (jab, punch_pose("left", guard, 0.8), STRIKE),
                                             (jab + 0.1, punch_pose("left", guard, 0.7), HOLD), (0.6, guard, BACK)])
    hurt = offset(guard, {"chest": [-14, 6, 0], "head": [-20, 8, 0], "body_pos": [0, 0, 1.2],
                          "arm_left": [10, 0, 0], "arm_right": [10, 0, 0]})
    out[p + "reaction.hurt"] = timeline(0.3, [(0, guard, BACK), (0.08, hurt, WIND), (0.3, guard, BACK)])
    return out


# ------------------------------------------------------------------------------------------------ kaiju humanoides
def rake(side, base, power=1.0, high=True):
    """Garra de cima para baixo cruzando o corpo: (preparo, impacto)."""
    sign = 1 if side == "right" else -1
    up = offset(base, {f"arm_{side}": [-150 if high else -100, 20 * sign, 30 * sign],
                       f"forearm_{side}": [-70, 0, 0], f"hand_{side}": [-20, 0, 0],
                       "chest": [-10 * power, 30 * power * sign, 0], "body": [-4, 10 * power * sign, 0],
                       "head": [0, -12 * sign, 0]})
    down = offset(base, {f"arm_{side}": [-45, -55 * sign, -15 * sign], f"forearm_{side}": [-15, 0, 0],
                         f"hand_{side}": [25, 0, 0], "chest": [16 * power, -32 * power * sign, 0],
                         "body": [8 * power, -12 * power * sign, 0], "head": [-6, 14 * sign, 0],
                         "body_pos": [0, -1.0 * power, 0]})
    return up, down


def sprint_pose(base, phase, lean=32):
    """Investida: corpo baixo e inclinado, bracos para tras, pernas correndo (phase 0 ou 1)."""
    front, back = ("left", "right") if phase == 0 else ("right", "left")
    return offset(base, {"body": [lean, 0, 0], "chest": [8, 0, 0], "head": [-lean * 0.8, 0, 0],
                         "arm_left": [55, 0, -15], "arm_right": [55, 0, 15], "forearm_left": [-40, 0, 0],
                         "forearm_right": [-40, 0, 0], f"leg_{front}": [-60, 0, 0], f"shin_{front}": [60, 0, 0],
                         f"leg_{back}": [30, 0, 0], f"shin_{back}": [40, 0, 0], f"foot_{back}": [20, 0, 0],
                         "body_pos": [0, -2.0, 0]})


def charge(base, name):
    data = json.loads((ROOT / f"src/main/resources/data/kn8/kn8/ability/{name}.json").read_text(encoding="utf-8"))
    start = data["windup_ticks"] * TICK
    stop = start + data.get("active_ticks", 1) * TICK
    end = stop + 0.3
    crouch = offset(base, {"body": [20, 0, 0], "head": [-16, 0, 0], "leg_left": [-30, 0, 0], "shin_left": [55, 0, 0],
                           "leg_right": [-20, 0, 0], "shin_right": [50, 0, 0], "arm_left": [35, 0, -10],
                           "arm_right": [35, 0, 10], "body_pos": [0, -3.0, 0]})
    frames = [(0, base, BACK), (start, crouch, WIND)]
    step, t, phase = 0.2, start + 0.1, 0
    while t < stop:
        frames.append((t, sprint_pose(base, phase), BACK))
        t += step
        phase ^= 1
    frames += [(stop, sprint_pose(base, phase), BACK), (end, base, BACK)]
    return timeline(end, frames)


def pounce(base, name, claws=True):
    hit, end = ability(name)
    crouch = offset(base, {"body": [24, 0, 0], "head": [-18, 0, 0], "leg_left": [-50, 0, 0], "shin_left": [85, 0, 0],
                           "foot_left": [-30, 0, 0], "leg_right": [-45, 0, 0], "shin_right": [80, 0, 0],
                           "foot_right": [-30, 0, 0], "arm_left": [45, 0, -20], "arm_right": [45, 0, 20],
                           "forearm_left": [-30, 0, 0], "forearm_right": [-30, 0, 0], "body_pos": [0, -4.0, 0]})
    launch = offset(base, {"body": [-12, 0, 0], "leg_left": [20, 0, 0], "leg_right": [25, 0, 0],
                           "foot_left": [30, 0, 0], "foot_right": [30, 0, 0], "arm_left": [-150, 0, -15],
                           "arm_right": [-150, 0, 15]})
    air = offset(base, {"body": [14, 0, 0], "leg_left": [-60, 0, 0], "shin_left": [90, 0, 0],
                        "leg_right": [-50, 0, 0], "shin_right": [95, 0, 0],
                        "arm_left": [-120 if claws else -60, 20, -30], "arm_right": [-120 if claws else -60, -20, 30],
                        "forearm_left": [-50, 0, 0], "forearm_right": [-50, 0, 0]})
    land = offset(base, {"body": [28, 0, 0], "head": [-20, 0, 0], "leg_left": [-40, 0, 0], "shin_left": [70, 0, 0],
                         "leg_right": [-30, 0, 0], "shin_right": [65, 0, 0], "arm_left": [-70, 10, -20],
                         "arm_right": [-70, -10, 20], "forearm_left": [-20, 0, 0], "forearm_right": [-20, 0, 0],
                         "body_pos": [0, -3.0, 0]})
    air_t = hit + (end - hit) * 0.45
    return timeline(end, [(0, base, BACK), (hit * 0.8, crouch, WIND), (hit, launch, STRIKE), (air_t, air, BACK),
                          (end - 0.2, land, STRIKE), (end, base, BACK)])


def kaiju_common(p, base, walk, lean_hurt=12):
    out = {p + "movement.idle": breathing(3.0, base, depth=2.5)}
    out[p + "movement.walk"] = walk
    hurt = offset(base, {"chest": [-lean_hurt, 8, 0], "head": [-18, 10, 0], "body_pos": [0, 0, 1.5],
                         "arm_left": [15, 0, -10], "arm_right": [15, 0, 10]})
    out[p + "reaction.hurt"] = timeline(0.3, [(0, base, BACK), (0.08, hurt, WIND), (0.3, base, BACK)])
    return out


# No. 13: predador curvado de bracos longos (garras vermelhas), joelhos sempre dobrados.
NO13_READY = {"body": [10, 0, 0], "chest": [8, 0, 0], "head": [-16, 0, 0],
              "arm_left": [-18, 8, 6], "forearm_left": [-38, 0, 0], "hand_left": [-10, 0, 0],
              "arm_right": [-18, -8, -6], "forearm_right": [-38, 0, 0], "hand_right": [-10, 0, 0],
              "leg_left": [-14, 0, -3], "shin_left": [26, 0, 0], "foot_left": [-10, 0, 0],
              "leg_right": [-12, 0, 3], "shin_right": [24, 0, 0], "foot_right": [-10, 0, 0], "body_pos": [0, -0.8, 0]}


def kaiju_no13(_profile):
    p, base = "kaiju_no13.", NO13_READY
    out = kaiju_common(p, base, gait(0.8, base, stride=30, knee=60, swing=24, elbow=36, bob=1.0, lean=4, twist=5))
    hit, end = ability("claw")
    up, down = rake("right", base)
    claw = timeline(end, [(0, base, BACK), (hit * 0.7, up, WIND), (hit, down, STRIKE), (hit + 0.1, down, HOLD),
                          (end, base, BACK)])
    out[p + "action.claw"] = claw
    out[p + "action.attack"] = claw
    times, step, end = multi_hits("no13_flurry")
    frames = [(0, base, BACK)]
    for i, t in enumerate(times):
        up, down = rake("right" if i % 2 == 0 else "left", base, 0.8, high=i % 2 == 0)
        frames += [(t - step * 0.55, up, WIND), (t, down, STRIKE)]
    out[p + "action.flurry"] = timeline(end, frames + [(end, base, BACK)])
    hit, end = ability("no13_heavy_punch")
    out[p + "action.punch"] = timeline(end, [
        (0, base, BACK), (hit * 0.7, cock_pose("right", lunge(base, 0.4), 1.6), WIND),
        (hit, punch_pose("right", lunge(base, 1.1), 1.6), STRIKE),
        (hit + 0.15, punch_pose("right", lunge(base, 1.1), 1.4), HOLD), (end, base, BACK)])
    hit, end = ability("no13_throat_grab")
    reach = offset(lunge(base, 0.9), {"arm_right": [-100, -10, -4], "forearm_right": [-4, 0, 0],
                                      "hand_right": [30, 0, 0], "chest": [12, -20, 0], "body": [8, -8, 0],
                                      "head": [-10, 10, 0], "arm_left": [20, 0, -15]})
    grab = offset(reach, {"hand_right": [-70, 0, 0], "forearm_right": [-20, 0, 0], "arm_right": [-8, 0, 0],
                          "chest": [-6, 0, 0]})
    out[p + "action.throat_grab"] = timeline(end, [
        (0, base, BACK), (hit * 0.6, offset(base, {"arm_right": [-40, 20, 20], "forearm_right": [-80, 0, 0],
                                                    "chest": [-4, 22, 0], "body_pos": [0, -1.5, 0]}), WIND),
        (hit, reach, STRIKE), (hit + 0.1, grab, WIND), (end - 0.15, grab, HOLD), (end, base, BACK)])
    out[p + "action.charge"] = charge(base, "no13_sprint")
    out[p + "action.leap"] = pounce(base, "no13_pounce")
    return out


# No. 9 com o Isao: ereto e calmo (o No. 9 e frio), manoplas pesadas meio erguidas.
NO9_ISAO_READY = {"body": [3, 0, 0], "chest": [2, 0, 0], "head": [-2, 0, 0],
                  "arm_left": [-14, 6, 4], "forearm_left": [-45, 0, 0], "arm_right": [-14, -6, -4],
                  "forearm_right": [-45, 0, 0], "leg_left": [-6, 0, -2], "shin_left": [10, 0, 0],
                  "leg_right": [-4, 0, 2], "shin_right": [8, 0, 0], "foot_left": [-4, 0, 0], "foot_right": [-4, 0, 0]}


def point(base, sides, recoil=0.0):
    """Finger Gun: braco(s) esticado(s) na mira, ombro do tiro para a frente, punho com o dedo apontando."""
    pose = offset(base, {"chest": [0, -18 if sides == ("right",) else 0, 0], "head": [0, 10, 0]})
    for side in sides:
        pose = merge(pose, {f"arm_{side}": [-90 + recoil, 0, 0], f"forearm_{side}": [-recoil * 0.8, 0, 0],
                            f"hand_{side}": [-recoil * 0.6, 0, 0]})
    return pose


def kaiju_no9_isao(_profile):
    p, base = "kaiju_no9_isao.", NO9_ISAO_READY
    out = kaiju_common(p, base, gait(0.8, base, stride=26, knee=48, swing=18, elbow=40, bob=0.7, lean=2, twist=4))
    hit, end = ability("claw")
    up, down = rake("right", base, 0.9)
    claw = timeline(end, [(0, base, BACK), (hit * 0.7, up, WIND), (hit, down, STRIKE), (hit + 0.1, down, HOLD),
                          (end, base, BACK)])
    out[p + "action.claw"] = claw
    out[p + "action.attack"] = claw
    for anim, name, sides in (("finger_gun", "finger_gun", ("right",)),
                              ("multi_finger_gun", "no9_black_multi_finger_gun", ("right", "left"))):
        hit, end = ability(name)
        aim = point(base, sides)
        out[p + "action." + anim] = timeline(end, [(0, base, BACK), (hit * 0.6, aim, WIND), (hit - 0.05, aim, HOLD),
                                                   (hit, point(base, sides, 22), STRIKE), (hit + 0.12, aim, BACK),
                                                   (end - 0.1, aim, HOLD), (end, base, BACK)])
    hit, end = ability("no9_black_tendril")
    open_ = offset(base, {"arm_left": [-70, 25, -25], "arm_right": [-70, -25, 25], "forearm_left": [-50, 0, 0],
                          "forearm_right": [-50, 0, 0], "chest": [-10, 0, 0], "body_pos": [0, 0, 1.0]})
    thrust = offset(lunge(base, 0.6), {"arm_left": [-95, 5, -5], "arm_right": [-95, -5, 5],
                                       "forearm_left": [-2, 0, 0], "forearm_right": [-2, 0, 0],
                                       "hand_left": [30, 0, 0], "hand_right": [30, 0, 0], "chest": [12, 0, 0]})
    out[p + "action.tendril"] = timeline(end, [(0, base, BACK), (hit * 0.7, open_, WIND), (hit, thrust, STRIKE),
                                               (hit + 0.15, thrust, HOLD), (end, base, BACK)])
    # COMBATE-4: Barrage Blast = cotovelada; Thunder Emperor = barragem de socos (as do Isao, em escala de kaiju).
    hit, end = ability("no9_isao_barrage_blast")
    out[p + "action.barrage_blast"] = elbow_anim(base, hit, end, 1.3)
    hit, end = ability("no9_isao_sonic_blast")
    out[p + "action.sonic_blast"] = timeline(end, [
        (0, base, BACK), (hit * 0.7, cock_pose("right", lunge(base, 0.4), 1.4), WIND),
        (hit, punch_pose("right", lunge(base, 1.0), 1.4), STRIKE),
        (hit + 0.2, punch_pose("right", lunge(base, 1.0), 1.2), HOLD), (end, base, BACK)])
    times, step, end = multi_hits("no9_isao_thunder_emperor")
    out[p + "action.thunder_emperor"] = thunder_barrage(base, times, end)
    hit, end = ability("no9_isao_force_blast")
    load = offset(base, {"arm_left": [70, -20, -30], "arm_right": [70, 20, 30], "forearm_left": [-30, 0, 0],
                         "forearm_right": [-30, 0, 0], "chest": [-12, 0, 0], "body_pos": [0, -2.0, 0],
                         "shin_left": [20, 0, 0], "shin_right": [20, 0, 0], "leg_left": [-14, 0, 0],
                         "leg_right": [-10, 0, 0]})
    blast = offset(lunge(base, 0.9), {"arm_left": [-30, 30, -40], "arm_right": [-30, -30, 40],
                                      "forearm_left": [40, 0, 0], "forearm_right": [40, 0, 0],
                                      "chest": [16, 0, 0], "body": [8, 0, 0]})
    out[p + "action.force_blast"] = timeline(end, [(0, base, BACK), (hit * 0.8, load, WIND), (hit, blast, STRIKE),
                                                   (hit + 0.25, blast, HOLD), (end, base, BACK)])
    out[p + "action.leap"] = pounce(base, "no9_black_short_flight", claws=False)
    length = 3.0
    hold = length - 0.4
    pull = offset(base, {"body": [12, 0, 0], "chest": [10, 0, 0], "head": [-10, 0, 0],
                         "arm_left": [-85, 25, -20], "arm_right": [-85, -25, 20], "forearm_left": [-15, 0, 0],
                         "forearm_right": [-15, 0, 0], "hand_left": [20, 0, 0], "hand_right": [20, 0, 0]})
    tug = offset(pull, {"arm_left": [20, 0, 0], "arm_right": [20, 0, 0], "forearm_left": [-50, 0, 0],
                        "forearm_right": [-50, 0, 0], "body": [-10, 0, 0], "body_pos": [0, 0, 1.5]})
    frames = [(0, base, BACK), (0.4, pull, WIND)]
    t = 0.4
    while t + 0.5 < hold:
        frames += [(t + 0.25, tug, BACK), (t + 0.5, pull, BACK)]
        t += 0.5
    out[p + "action.absorb"] = timeline(length, frames + [(hold, pull, HOLD), (length, base, BACK)])
    return out


# ------------------------------------------------------------------------------------------------ RIG-2
# Kaiju No. 8 (soldado especial e fantoche da forma do jogador): lutador pesado, guarda baixa e larga de brigao,
# socos com o corpo inteiro, golpe no chao com os dois punhos e rugido abrindo o peito.
NO8_GUARD = {"arm_left": [-30, 14, 8], "forearm_left": [-92, 0, 0], "hand_left": [0, 0, 0],
             "arm_right": [-24, -16, -10], "forearm_right": [-100, 0, 0], "hand_right": [0, 0, 0]}
NO8_STANCE = {"body": [8, 10, 0], "chest": [6, -8, 0], "head": [-8, -4, 0],
              "leg_left": [-14, 0, -6], "shin_left": [20, 0, 0], "foot_left": [-6, 0, 0],
              "leg_right": [10, 0, 6], "shin_right": [16, 0, 0], "foot_right": [-8, 0, 0], "body_pos": [0, -0.9, 0]}
NO8_READY = {"arm_left": [-12, 6, 4], "forearm_left": [-40, 0, 0], "arm_right": [-10, -6, -4],
             "forearm_right": [-45, 0, 0]}


def fighter_movement(p, stance, guard, ready, out, walk=0.9, run=0.6, heavy=1.0):
    out[p + "movement.idle"] = breathing(3.0, stance, arms=False, depth=2.0 * heavy)
    out[p + "movement.walk"] = gait(walk, {"body": [4, 0, 0], "chest": [3, 0, 0]}, stride=26, knee=48, swing=0,
                                    elbow=0, bob=0.7 * heavy, lean=4, arms=False)
    out[p + "movement.run"] = gait(run, {"body": [14, 0, 0], "chest": [6, 0, 0], "head": [-12, 0, 0]}, stride=46,
                                   knee=95, swing=0, elbow=0, bob=1.5 * heavy, lean=14, twist=6, arms=False)
    arms = {"ready": (ready, 3.0, 2.0), "walk": (guard, 0.45, 2.5), "run": (guard, 0.3, 3.0),
            "aim": (guard, 2.0, 1.5)}
    for name, (pose, length, sway) in arms.items():
        up = offset(pose, {"arm_left": [-sway, 0, 0], "arm_right": [-sway, 0, 0],
                           "forearm_left": [-sway, 0, 0], "forearm_right": [sway, 0, 0]})
        out[f"{p}arms.fists_{name}"] = timeline(length, [(0, pose, BACK), (length / 2, up, BACK),
                                                         (length, pose, BACK)], loop=True)


def kaiju_no8(profile):
    p = "kaiju_no8."
    guard = merge(NO8_STANCE, NO8_GUARD)
    out = {}
    fighter_movement(p, NO8_STANCE, NO8_GUARD, NO8_READY, out, heavy=1.3)
    hits, end = technique(profile, "rush_combo")
    out[p + "action.rush_combo"] = combo(guard, hits, end, sides=("right", "left"), power=1.4)
    (hit,), end = technique(profile, "heavy_punch")
    impact = punch_pose("right", lunge(guard, 1.1), 1.8)
    out[p + "action.heavy_punch"] = timeline(end, [
        (0, guard, BACK), (hit * 0.7, cock_pose("right", lunge(guard, 0.4), 1.8), WIND), (hit, impact, STRIKE),
        (hit + 0.2, offset(impact, {"chest": [4, -6, 0]}), HOLD), (end, guard, BACK)])
    # COMBATE-7 (obra, cap. 2 e 127: soco direto de ruptura): postura baixa com ombro e quadril girados para tras,
    # o braco sai de uma vez com o corpo todo e segura no impacto (o alvo e que voa).
    if "rupture_punch" in profile["techniques"]:
        (hit,), end = technique(profile, "rupture_punch")
        coil = offset(guard, {"body": [10, 28, 0], "chest": [10, 32, 0], "head": [-8, -30, 0],
                              "body_pos": [0, -3.0, 1.0], "leg_left": [-45, 0, -6], "shin_left": [70, 0, 0],
                              "leg_right": [25, 0, 8], "shin_right": [40, 0, 0], "foot_right": [15, 0, 0]})
        strike = offset(punch_pose("right", lunge(guard, 1.5), 1.9), {"chest": [6, -30, 0], "body": [4, -18, 0]})
        out[p + "action.rupture_punch"] = timeline(end, [
            (0, guard, BACK), (hit * 0.75, cock_pose("right", coil, 2.0), WIND), (hit, strike, STRIKE),
            (hit + 0.3, offset(strike, {"chest": [2, -4, 0]}), HOLD), (end, guard, BACK)])
    (hit,), end = technique(profile, "dash_strike")
    charge_pose = offset(guard, {"body": [26, 0, 0], "chest": [8, 0, 0], "head": [-20, 0, 0],
                                 "body_pos": [0, -2.5, 0], "leg_left": [-50, 0, 0], "shin_left": [60, 0, 0],
                                 "leg_right": [35, 0, 0], "shin_right": [30, 0, 0], "foot_right": [20, 0, 0]})
    out[p + "action.dash_strike"] = timeline(end, [
        (0, guard, BACK), (hit * 0.6, cock_pose("right", charge_pose, 1.4), WIND),
        (hit, punch_pose("right", lunge(guard, 1.3), 1.8), STRIKE),
        (hit + 0.2, punch_pose("right", lunge(guard, 1.3), 1.5), HOLD), (end, guard, BACK)])
    (hit,), end = technique(profile, "ground_smash")
    lift = offset(guard, {"arm_left": [-170, -10, -10], "arm_right": [-170, 10, 10], "forearm_left": [40, 0, 0],
                          "forearm_right": [45, 0, 0], "chest": [-18, 0, 0], "body": [-6, 0, 0], "head": [-12, 0, 0],
                          "body_pos": [0, 1.5, 0]})
    smash = offset(guard, {"arm_left": [-20, 0, 0], "arm_right": [-20, 0, 0], "forearm_left": [60, 0, 0],
                           "forearm_right": [70, 0, 0], "chest": [20, 0, 0], "body": [8, 0, 0], "head": [-6, 0, 0],
                           "leg_left": [-40, 0, -10], "shin_left": [70, 0, 0], "leg_right": [-20, 0, 10],
                           "shin_right": [55, 0, 0], "body_pos": [0, -5.0, -1.5]})
    out[p + "action.ground_smash"] = timeline(end, [
        (0, guard, BACK), (hit * 0.65, lift, WIND), (hit, smash, STRIKE), (hit + 0.3, smash, HOLD),
        (end, guard, BACK)])
    (hit,), end = technique(profile, "roar")
    breathe_in = offset(guard, {"chest": [14, 0, 0], "head": [18, 0, 0], "arm_left": [10, 0, 10],
                                "arm_right": [10, 0, -10], "body_pos": [0, -1.0, 0]})
    roar = offset(guard, {"chest": [-24, 0, 0], "head": [-34, 0, 0], "arm_left": [40, -10, -75],
                          "arm_right": [40, 10, 75], "forearm_left": [60, 0, 0], "forearm_right": [70, 0, 0],
                          "body": [-6, 0, 0]})
    out[p + "action.roar"] = timeline(end, [(0, guard, BACK), (hit * 0.7, breathe_in, WIND), (hit, roar, STRIKE),
                                            (end - 0.25, offset(roar, {"head": [4, 0, 0]}), HOLD),
                                            (end, guard, BACK)])
    counter_hit = profile["counter"]["strike_delay_ticks"] * TICK
    counter_end = counter_hit + 8 * TICK
    upper_wind = offset(guard, {"arm_right": [20, 20, 20], "forearm_right": [-80, 0, 0], "chest": [4, 25, 0],
                                "body_pos": [0, -2.0, 0], "shin_left": [20, 0, 0], "shin_right": [20, 0, 0]})
    uppercut = offset(guard, {"arm_right": [-140, 0, -10], "forearm_right": [10, 0, 0], "chest": [-14, -25, 0],
                              "head": [-10, 10, 0], "body_pos": [0, 0.8, 0]})
    out[p + "action.counter"] = timeline(counter_end, [
        (0, guard, BACK), (counter_hit * 0.6, upper_wind, WIND), (counter_hit, uppercut, STRIKE),
        (counter_hit + 0.1, uppercut, HOLD), (counter_end, guard, BACK)])
    dash_end = 8 * TICK
    out[p + "action.dash"] = timeline(dash_end, [(0, guard, BACK), (dash_end * 0.4, charge_pose, WIND),
                                                 (dash_end, guard, BACK)])
    parry_end = 8 * TICK
    block = merge(offset(guard, {"chest": [-4, 0, 0], "head": [10, 0, 0], "body_pos": [0, -0.8, 0.6]}),
                  {"forearm_left": [-128, 0, 0], "forearm_right": [-128, 0, 0],
                   "arm_left": [-60, 52, 10], "arm_right": [-60, -52, -10]})
    out[p + "action.parry"] = timeline(parry_end, [(0, guard, BACK), (0.1, block, WIND), (0.2, block, HOLD),
                                                   (parry_end, guard, BACK)])
    # Golpe comum (e o soco leve da forma do jogador: KaijuFormClient toca "attack").
    jab = 0.25
    out[p + "action.attack"] = timeline(0.6, [(0, guard, BACK), (jab * 0.5, cock_pose("right", guard, 0.8), WIND),
                                             (jab, punch_pose("right", guard, 1.2), STRIKE),
                                             (jab + 0.1, punch_pose("right", guard, 1.0), HOLD), (0.6, guard, BACK)])
    hurt = offset(guard, {"chest": [-14, 6, 0], "head": [-20, 8, 0], "body_pos": [0, 0, 1.2],
                          "arm_left": [10, 0, 0], "arm_right": [10, 0, 0]})
    out[p + "reaction.hurt"] = timeline(0.3, [(0, guard, BACK), (0.08, hurt, WIND), (0.3, guard, BACK)])
    return out


# No. 9 (2 m, frio e calculista) e a forma preta (5 m, agressiva, braco-lamina e espinhos).
NO9_READY = {"body": [2, 0, 0], "chest": [2, 0, 0], "head": [-2, 0, 0],
             "arm_left": [-8, 4, 6], "forearm_left": [-28, 0, 0], "arm_right": [-8, -4, -6],
             "forearm_right": [-28, 0, 0], "leg_left": [-4, 0, -2], "shin_left": [8, 0, 0],
             "leg_right": [-3, 0, 2], "shin_right": [6, 0, 0], "foot_left": [-4, 0, 0], "foot_right": [-3, 0, 0]}
NO9_BLACK_READY = {"body": [12, 0, 0], "chest": [10, 0, 0], "head": [-18, 0, 0],
                   "arm_left": [-20, 10, 8], "forearm_left": [-45, 0, 0], "hand_left": [-10, 0, 0],
                   "arm_right": [-20, -10, -8], "forearm_right": [-45, 0, 0], "hand_right": [-10, 0, 0],
                   "leg_left": [-16, 0, -4], "shin_left": [28, 0, 0], "foot_left": [-10, 0, 0],
                   "leg_right": [-12, 0, 4], "shin_right": [24, 0, 0], "foot_right": [-10, 0, 0],
                   "body_pos": [0, -1.0, 0]}


def no9_common(p, base, out, walk_args):
    out.update(kaiju_common(p, base, gait(0.8, base, **walk_args)))
    hit, end = ability("claw")
    up, down = rake("right", base, 0.9)
    claw = timeline(end, [(0, base, BACK), (hit * 0.7, up, WIND), (hit, down, STRIKE), (hit + 0.1, down, HOLD),
                          (end, base, BACK)])
    out[p + "action.claw"] = claw
    out[p + "action.attack"] = claw
    out[p + "action.charge"] = charge(base, "charge")
    hit, end = ability("finger_gun")
    aim = point(base, ("right",))
    out[p + "action.finger_gun"] = timeline(end, [(0, base, BACK), (hit * 0.6, aim, WIND), (hit - 0.05, aim, HOLD),
                                                  (hit, point(base, ("right",), 22), STRIKE),
                                                  (hit + 0.12, aim, BACK), (end - 0.1, aim, HOLD), (end, base, BACK)])
    for name, length in (("absorb", 3.0), ("revive", 3.0)):
        hold = length - 0.4
        pull = offset(base, {"body": [12, 0, 0], "chest": [10, 0, 0], "head": [-10, 0, 0],
                             "arm_left": [-85, 25, -20], "arm_right": [-85, -25, 20], "forearm_left": [-15, 0, 0],
                             "forearm_right": [-15, 0, 0], "hand_left": [20, 0, 0], "hand_right": [20, 0, 0]})
        if name == "revive":
            # Reviver: maos abertas para a carcaca, dedos tremendo, corpo inclinado sobre ela.
            pull = offset(pull, {"arm_left": [10, 0, 0], "arm_right": [10, 0, 0], "body": [6, 0, 0],
                                 "hand_left": [-40, 0, 0], "hand_right": [-40, 0, 0]})
        tug = offset(pull, {"arm_left": [20, 0, 0], "arm_right": [20, 0, 0], "forearm_left": [-50, 0, 0],
                            "forearm_right": [-50, 0, 0], "body": [-10, 0, 0], "body_pos": [0, 0, 1.5]})
        frames = [(0, base, BACK), (0.4, pull, WIND)]
        t = 0.4
        while t + 0.5 < hold:
            frames += [(t + 0.25, tug, BACK), (t + 0.5, pull, BACK)]
            t += 0.5
        out[p + "action." + name] = timeline(length, frames + [(hold, pull, HOLD), (length, base, BACK)])


def kaiju_no9(_profile):
    p, out = "kaiju_no9.", {}
    no9_common(p, NO9_READY, out, {"stride": 24, "knee": 44, "swing": 16, "elbow": 30, "bob": 0.6, "lean": 2,
                                   "twist": 3})
    return out


def kaiju_no9_black(_profile):
    p, base, out = "kaiju_no9_black.", NO9_BLACK_READY, {}
    no9_common(p, base, out, {"stride": 30, "knee": 60, "swing": 22, "elbow": 36, "bob": 1.0, "lean": 4,
                              "twist": 5})
    hit, end = ability("no9_black_multi_finger_gun")
    aim = point(base, ("right", "left"))
    out[p + "action.multi_finger_gun"] = timeline(end, [
        (0, base, BACK), (hit * 0.6, aim, WIND), (hit, point(base, ("right", "left"), 22), STRIKE),
        (hit + 0.12, aim, BACK), (end - 0.1, aim, HOLD), (end, base, BACK)])
    hit, end = ability("no9_black_blade_arm")
    wind = offset(base, {"arm_right": [-90, 60, 50], "forearm_right": [-20, 0, 0], "chest": [-4, 45, 0],
                         "body": [0, 18, 0], "head": [0, -20, 0], "body_pos": [0, -1.5, 0]})
    slash = offset(lunge(base, 0.8), {"arm_right": [-85, -80, -10], "forearm_right": [-5, 0, 0],
                                      "chest": [10, -50, 0], "body": [6, -20, 0], "head": [0, 24, 0]})
    out[p + "action.blade_arm"] = timeline(end, [(0, base, BACK), (hit * 0.7, wind, WIND), (hit, slash, STRIKE),
                                                 (hit + 0.15, slash, HOLD), (end, base, BACK)])
    hit, end = ability("no9_black_tendril")
    open_ = offset(base, {"arm_left": [-70, 25, -25], "arm_right": [-70, -25, 25], "forearm_left": [-50, 0, 0],
                          "forearm_right": [-50, 0, 0], "chest": [-10, 0, 0], "body_pos": [0, 0, 1.0]})
    thrust = offset(lunge(base, 0.6), {"arm_left": [-95, 5, -5], "arm_right": [-95, -5, 5],
                                       "forearm_left": [40, 0, 0], "forearm_right": [40, 0, 0],
                                       "hand_left": [30, 0, 0], "hand_right": [30, 0, 0], "chest": [12, 0, 0]})
    out[p + "action.tendril"] = timeline(end, [(0, base, BACK), (hit * 0.7, open_, WIND), (hit, thrust, STRIKE),
                                               (hit + 0.15, thrust, HOLD), (end, base, BACK)])
    hit, end = ability("no9_black_spike_burst")
    curl = offset(base, {"body": [20, 0, 0], "chest": [20, 0, 0], "head": [20, 0, 0], "arm_left": [-40, 30, 20],
                         "arm_right": [-40, -30, -20], "forearm_left": [-90, 0, 0], "forearm_right": [-90, 0, 0],
                         "shin_left": [30, 0, 0], "shin_right": [30, 0, 0], "body_pos": [0, -3.0, 0]})
    burst = offset(base, {"body": [-10, 0, 0], "chest": [-18, 0, 0], "head": [-25, 0, 0],
                          "arm_left": [10, -20, -85], "arm_right": [10, 20, 85], "forearm_left": [40, 0, 0],
                          "forearm_right": [40, 0, 0], "hand_left": [30, 0, 0], "hand_right": [30, 0, 0],
                          "body_pos": [0, 1.0, 0]})
    out[p + "action.spike_burst"] = timeline(end, [(0, base, BACK), (hit * 0.8, curl, WIND), (hit, burst, STRIKE),
                                                   (hit + 0.25, burst, HOLD), (end, base, BACK)])
    out[p + "action.leap"] = pounce(base, "no9_black_short_flight", claws=True)
    return out


# ------------------------------------------------------------------------------------------------ passada automatica
def sample(channel, t):
    keys = []
    for k, v in channel.items():
        if isinstance(v, dict):
            v = v.get("vector", v.get("post", v.get("pre")))
        keys.append((float(k), [float(x) for x in v]))
    keys.sort()
    if t <= keys[0][0]:
        return keys[0][1]
    for (t0, v0), (t1, v1) in zip(keys, keys[1:]):
        if t <= t1:
            u = (t - t0) / max(t1 - t0, 1e-9)
            return [a * (1 - u) + b * u for a, b in zip(v0, v1)]
    return keys[-1][1]


def compact(times, values, tolerance=0.6):
    """Keyframes lineares a cada 0,05 s sem os pontos que a reta entre os vizinhos ja da."""
    keep = [0]
    for i in range(1, len(times) - 1):
        a, b = keep[-1], i + 1
        u = (times[i] - times[a]) / (times[b] - times[a])
        guess = [x * (1 - u) + y * u for x, y in zip(values[a], values[b])]
        if max(abs(g - v) for g, v in zip(guess, values[i])) > tolerance:
            keep.append(i)
    keep.append(len(times) - 1)
    return {("0.0" if times[i] == 0 else f"{round(times[i], 3)}"): [round(x, 2) for x in values[i]] for i in keep}


def articulate(name, anim, unarmed):
    """Joelho, pe e peito (e cotovelo de quem luta sem arma) tirados do movimento que a animacao ja tem: so entra em
    osso que a animacao nao anima. Braco de quem segura arma fica como estava (pegada medida das duas maos)."""
    length = float(anim.get("animation_length", 0) or 0)
    bones = anim.setdefault("bones", {})
    if length <= 0:
        return
    steps = max(2, int(round(length / 0.05)) + 1)
    times = [length * i / (steps - 1) for i in range(steps)]
    loop = bool(anim.get("loop"))
    moving = loop and (".movement." in name or ".arms." in name)
    for side in ("left", "right"):
        leg = bones.get(f"leg_{side}", {}).get("rotation")
        if leg and f"shin_{side}" not in bones:
            xs = [sample(leg, t)[0] for t in times]
            bends, feet = [], []
            for i, x in enumerate(xs):
                j, k = max(i - 1, 0), min(i + 1, steps - 1)
                speed = (xs[k] - xs[j]) / max(times[k] - times[j], 1e-6)
                if moving:
                    bend = 0.22 * max(0.0, -speed) + 0.3 * max(0.0, x)
                else:
                    bend = 0.7 * max(0.0, -x - 8) + 0.25 * max(0.0, x) + 0.1 * max(0.0, -speed)
                bend = min(bend, 100.0)
                bends.append([bend, 0.0, 0.0])
                feet.append([max(-40.0, min(40.0, -(x + bend) * 0.8)), 0.0, 0.0])
            bones[f"shin_{side}"] = {"rotation": compact(times, bends)}
            bones[f"foot_{side}"] = {"rotation": compact(times, feet)}
        arm = bones.get(f"arm_{side}", {}).get("rotation")
        if unarmed and arm and f"forearm_{side}" not in bones:
            values = [[-(15 + 0.25 * max(0.0, -sample(arm, t)[0])), 0.0, 0.0] for t in times]
            bones[f"forearm_{side}"] = {"rotation": compact(times, values)}
    body = bones.get("body", {}).get("rotation")
    legs = [bones.get(f"leg_{s}", {}).get("rotation") for s in ("left", "right")]
    if "chest" not in bones and (body or all(legs)):
        values = []
        for t in times:
            b = sample(body, t) if body else [0.0, 0.0, 0.0]
            twist = 0.12 * (sample(legs[0], t)[0] - sample(legs[1], t)[0]) if all(legs) and moving else 0.0
            values.append([0.25 * b[0], 0.4 * b[1] + twist, 0.2 * b[2]])
        if any(abs(v) > 0.5 for value in values for v in value):
            bones["chest"] = {"rotation": compact(times, values)}


# ------------------------------------------------------------------------------------- COMBATE-2: pernas nas posturas
# Miguel (2026-10-29): "em posturas quero apenas a perna dobrando". Joelho e pe das posturas (parado, andando,
# correndo) refeitos pela fase do passo; a coxa, o tempo do passo e os bracos ficam como estao (armas na mao). Graus.
POSTURE_KNEE = {
    # (joelho no apoio, joelho no meio da perna indo a frente)
    "walk": (8.0, 58.0),
    "run": (18.0, 100.0),
}
IDLE_KNEE = 10.0
IDLE_KNEE_PER_DEGREE = 0.5


def posture_legs(name, anim):
    """Joelho e pe de uma postura em laco, tirados da coxa: a perna que vai a frente (coxa com X diminuindo) dobra o
    joelho ate o pico logo no comeco do balanco e estica ao pisar; no apoio o joelho fica quase reto e o pe plano
    (pe = -(coxa + joelho)). Parado: joelho levemente dobrado (mais com a perna aberta), pe plano. Sobrescreve o que a
    passada automatica (articulate) tinha posto, sempre a partir da coxa: rodar de novo da o mesmo resultado."""
    kind = name.rsplit(".", 1)[-1]
    length = float(anim.get("animation_length", 0) or 0)
    bones = anim.get("bones", {})
    if length <= 0 or kind not in ("idle", "walk", "run"):
        return False
    steps = max(2, int(round(length / 0.05)) + 1)
    times = [length * i / (steps - 1) for i in range(steps)]
    changed = False
    for side in ("left", "right"):
        leg = bones.get(f"leg_{side}", {}).get("rotation")
        if not leg:
            continue
        xs = [sample(leg, t)[0] for t in times]
        if kind == "idle":
            knees = [IDLE_KNEE + IDLE_KNEE_PER_DEGREE * abs(x) for x in xs]
        else:
            stance, peak = POSTURE_KNEE[kind]
            speeds = []
            for i in range(steps):
                # Laco: vizinhos dao a volta (o primeiro e o ultimo quadro sao o mesmo instante).
                j, k = (i - 1) % (steps - 1), (i + 1) % (steps - 1)
                speeds.append(-(xs[k] - xs[j]) / (2 * length / (steps - 1)))
            top = max(max(speeds), 1e-6)
            knees = [stance + (peak - stance) * min(1.0, max(0.0, v) / (0.6 * top)) for v in speeds]
            # Suaviza o pico (media com os vizinhos, em laco) para nao estalar.
            knees = [(knees[(i - 1) % (steps - 1)] + 2 * knees[i] + knees[(i + 1) % (steps - 1)]) / 4
                     for i in range(steps)]
        feet = []
        for i, (x, k) in enumerate(zip(xs, knees)):
            flat = -(x + k)
            swinging = kind != "idle" and k > POSTURE_KNEE.get(kind, (0, 0))[0] + 10
            feet.append([max(-60.0, min(60.0, flat * (0.6 if swinging else 1.0))), 0.0, 0.0])
        bones[f"shin_{side}"] = {"rotation": compact(times, [[k, 0.0, 0.0] for k in knees])}
        bones[f"foot_{side}"] = {"rotation": compact(times, feet)}
        changed = True
    return changed


# Hoshina No. 10 parado (Miguel, 2026-10-29, figura): agachado com o joelho direito no chao, a perna esquerda a frente e
# para fora com a canela em pe e o pe plano, tronco inclinado, cabeca para a frente; bracos e laminas para tras
# (controller "arms", sem mudar). O modelo do No. 10 ja tem as coxas inclinadas no repouso: as pernas sao dadas pela
# DIRECAO de cada segmento (coxa, canela, pe; espaco do modelo da GeckoLib: +X direita do personagem, +Y cima, -Z frente)
# e as rotacoes saem dela; a altura do corpo sai da malha (preview_rig) para o joelho encostar no chao.
NO10_KNEEL_TRUNK = {"body": [28, 0, 0], "chest": [12, 6, 0], "head": [-38, -4, 0]}
NO10_KNEEL_LEGS = {
    "right": {"leg": (0.25, -1.0, -0.45), "shin": (0.05, -0.15, 1.0), "foot": (0.0, -0.7, 1.0)},
    "left": {"leg": (-0.45, -0.2, -1.0), "shin": (-0.05, -1.0, 0.1), "foot": (-0.15, 0.0, -1.0)},
}


def _keyframe(local):
    """Matriz local -> keyframe [x, y, z] da GeckoLib (o preview_rig monta Rz(z) Ry(-y) Rx(-x))."""
    from scipy.spatial.transform import Rotation
    c, b, a = Rotation.from_matrix(local).as_euler("ZYX", degrees=True)
    return [round(float(-a), 2), round(float(-b), 2), round(float(c), 2)]


def _aim(rest, target):
    import numpy as np
    a = np.asarray(rest, float) / np.linalg.norm(rest)
    b = np.asarray(target, float) / np.linalg.norm(target)
    v, c = np.cross(a, b), float(np.dot(a, b))
    k = np.array([[0, -v[2], v[1]], [v[2], 0, -v[0]], [-v[1], v[0], 0]])
    return np.eye(3) + k + k @ k / (1 + c)


def hoshina_no10_kneel(data):
    import numpy as np
    import preview_rig
    name = "hoshina_no10.movement.idle"
    rig = preview_rig.Rig("hoshina_no10")
    pose = dict(NO10_KNEEL_TRUNK)
    for side, dirs in NO10_KNEEL_LEGS.items():
        hip, knee, ankle = (rig.pivot(f"{b}_{side}") for b in ("leg", "shin", "foot"))
        # Sem malha do pe (o corte do No. 10 deixou o pe esquerdo na canela): a ponta do pe vai reta a frente.
        foot = rig.meshes.get(f"foot_{side}")
        toe = foot[0].reshape(-1, 3)[np.argmin(foot[0].reshape(-1, 3)[:, 2])] if foot else ankle + [0, 0, -0.1]
        parent = np.eye(3)
        for bone, rest in (("leg", knee - hip), ("shin", ankle - knee), ("foot", toe - ankle)):
            local = _aim(rest, parent.T @ np.asarray(dirs[bone], float))
            pose[f"{bone}_{side}"] = _keyframe(local)
            parent = parent @ local
    bones = {}
    for bone, rot in pose.items():
        if bone in ("body", "chest"):
            # Respiracao: o tronco sobe e desce 1,5 grau em 2 s.
            breath = 1.5 if bone == "body" else 0.8
            bones[bone] = {"rotation": {"0.0": rot, "1.0": [rot[0] + breath, rot[1], rot[2]], "2.0": rot}}
        else:
            bones[bone] = {"rotation": {"0.0": rot, "2.0": rot}}
    for side in ("left", "right"):
        # O Blockbench do Miguel encolhia as pernas por escala; com o joelho de verdade, escala 1.
        bones[f"leg_{side}"]["scale"] = {"0.0": [1, 1, 1], "2.0": [1, 1, 1]}
        bones[f"leg_{side}"]["position"] = {"0.0": [0, 0, 0], "2.0": [0, 0, 0]}
    bones["body"]["position"] = {"0.0": [0, 0, 0], "2.0": [0, 0, 0]}
    anim = {"loop": True, "animation_length": 2.0, "bones": bones}
    rig.anims[name] = anim
    rot, pos = rig.pose("movement.idle@0")
    world = rig.world(rot, pos)
    low = min(float((m[0].reshape(-1, 3) @ world[b][:3, :3].T + world[b][:3, 3])[:, 1].min())
              for b, m in rig.meshes.items() if not b.startswith("tail"))
    bones["root"] = {"position": {"0.0": [0, round(-low * 16, 2), 0], "2.0": [0, round(-low * 16, 2), 0]}}
    data["animations"][name] = anim


# Especies so com a passada automatica (animacoes de arma medidas, Blockbench do Miguel, caudas): o que ja existe fica.
ARTICULATE = {"soldier": False, "hoshina": False, "hoshina_no10": False, "kikoru": False, "kikoru_no4": False,
              "mina": False, "reno": False, "reno_no6": False, "narumi": False, "kafka": False,
              "kaiju_no9_fusion": True, "kaiju_no10_small": True, "kaiju_no10_giant": True}


SPECIES = {"isao": isao, "kaiju_no13": kaiju_no13, "kaiju_no9_isao": kaiju_no9_isao, "kaiju_no8": kaiju_no8,
           "kaiju_no9": kaiju_no9, "kaiju_no9_black": kaiju_no9_black}


def main():
    for species in sys.argv[1:] or list(SPECIES) + list(ARTICULATE):
        path = ASSETS / f"{species}.animation.json"
        raw = path.read_bytes().decode("utf-8")
        newline = "\r\n" if "\r\n" in raw else "\n"
        data = json.loads(raw)
        if species in ARTICULATE:
            for name, anim in data["animations"].items():
                articulate(name, anim, ARTICULATE[species])
                if anim.get("loop") and ".movement." in name:
                    posture_legs(name, anim)
            if species == "hoshina_no10":
                hoshina_no10_kneel(data)
            path.write_bytes((json.dumps(data, indent=2) + "\n").replace("\n", newline).encode("utf-8"))
            print(species, len(data["animations"]), "animacoes com joelho/pe/peito")
            continue
        profile = None
        if (PROFILES / f"{species}.json").exists():
            profile = json.loads((PROFILES / f"{species}.json").read_text(encoding="utf-8"))
            # Perfis que usam o modelo desta especie (Isao sem o Numbers 2) entram com as tecnicas deles.
            for other in {"isao": ["isao_base"]}.get(species, []):
                extra = json.loads((PROFILES / f"{other}.json").read_text(encoding="utf-8"))
                for name, t in extra["techniques"].items():
                    profile["techniques"].setdefault(name, t)
        generated = SPECIES[species](profile)
        data["animations"].update(generated)
        # O resto (tiro, recarga, salto tatico) ganha joelho/pe/peito sem mexer nos bracos.
        for name, anim in data["animations"].items():
            if name not in generated:
                articulate(name, anim, False)
        path.write_bytes((json.dumps(data, indent=2) + "\n").replace("\n", newline).encode("utf-8"))
        print(species, len(generated), "animacoes:", ", ".join(sorted(n.split(".", 1)[1] for n in generated)))


if __name__ == "__main__":
    main()
