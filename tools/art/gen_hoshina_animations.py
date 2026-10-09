#!/usr/bin/env python3
"""Animacoes das tecnicas do Hoshina (0.6-D), com os tempos de data/kn8/kn8/special_soldier/hoshina.json.

Cada golpe visual cai no tick em que o servidor aplica o dano (windup_ticks + i * hit_interval), como nas
animacoes do jogador (gen_player_animations.py). Escreve em assets/kn8/animations/entity/hoshina.animation.json
(gerado antes por rig_soldier_mesh.py hoshina; rodar este depois dele). Mantem as quebras de linha do arquivo.
Ossos: body, head, arm_left, arm_right, leg_left, leg_right. Braco direito: X negativo = para frente/cima, Y
negativo = para dentro; o esquerdo e o espelho (Y e Z com sinal trocado).
Uso: python3 tools/art/gen_hoshina_animations.py [especie]   (hoshina ou hoshina_no10; rodar depois do
     rig_soldier_mesh.py da mesma especie)
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PROFILES = ROOT / "src/main/resources/data/kn8/kn8/special_soldier"
ANIMATIONS = ROOT / "src/main/resources/assets/kn8/animations/entity"
TICK = 1 / 20
DASH_TICKS = 8
PARRY_TICKS = 8
COUNTER_EXTRA_TICKS = 8

# Poses dos bracos (direito; o esquerdo e o espelho).
REST = [-30, -10, 0]
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


# 0.5.0-D3: tecnicas pelas formas do estilo Hoshina (anime/manga e KAIJU NO. 8 THE GAME): forma 1 Kuuchi (corte
# unico tao rapido que parece invisivel, solta pressao de ar), 2 Kosa-uchi (corte cruzado em X), 3 Kaeshi-uchi (correndo,
# corte cruzado girando), 4 Ran-uchi (diagonal alta, horizontal baixa, subida vertical e varredura girando, em cadeia),
# 5 Kasumi-uchi (parece um X duplo, mas o terceiro corte vem baixo pelo lado), 6 Yae-uchi (oito golpes em estrela) e 7
# Juni-hitoe (doze camadas, com a cauda). Laminas sempre invertidas: o corte e o antebraco varrendo (lamina ao longo
# dele). Todas partem da postura baixa (movement.idle) e voltam a ela, sem "pulo" entre controllers.
OPEN = [-75, 10, 80]
ACROSS = [-95, -70, -5]
COCKED = [-115, -60, -15]
HIGH_OUT = [-160, 0, 45]
LOW_IN = [-35, -45, -30]
LOW_OUT = [-20, 0, 65]
LOW_ACROSS = [-50, -75, -10]
DOWN = [-5, 0, 15]
UP = [-170, -10, 10]
CROSS_CHEST = [-90, -55, -10]
READY = [65, 0, 20]
READY_LEFT = [30, 0, 15]
STANCE = {"root": [0, -4, 0], "body": [28, 0, 0], "head": [-25, 0, 0], "leg_right": [-40, 0, 25],
          "leg_left": [35, 0, -22]}
LUNGE = {"root": [0, -5, 0], "leg_right": [-60, 0, 12], "leg_left": [45, 0, -10]}


class Track:
    """Keyframes por osso em ticks; comeca e termina na postura baixa."""

    def __init__(self, end):
        self.end = end
        self.bones = {}
        for bone, value in STANCE.items():
            self.set(bone, 0, value)
        self.set("arm_right", 0, READY)
        self.set("arm_left", 0, mirror(READY_LEFT))

    def set(self, bone, tick, value, channel=None):
        channel = channel or ("position" if bone == "root" else "rotation")
        self.bones.setdefault(bone, {}).setdefault(channel, []).append((tick, value))

    def arms(self, tick, right=None, left=None):
        if right is not None:
            self.set("arm_right", tick, right)
        if left is not None:
            self.set("arm_left", tick, mirror(left))

    def spin(self, start, hit, degrees=360):
        """Giro do corpo inteiro no osso root (yaw), sem desenrolar: depois do giro volta a 0 no mesmo instante."""
        self.set("root", start, [0, 0, 0], "rotation")
        self.set("root", hit, [0, degrees, 0], "rotation")
        self.set("root", hit + 0.02, [0, 0, 0], "rotation")

    def build(self):
        for bone, value in STANCE.items():
            self.set(bone, self.end, value)
        self.set("arm_right", self.end, READY)
        self.set("arm_left", self.end, mirror(READY_LEFT))
        bones = {}
        for bone, channels in self.bones.items():
            bones[bone] = {channel: keys(*((tick * TICK, v) for tick, v in frames))
                           for channel, frames in channels.items()}
        return anim(self.end, bones)


def lunge(track, tick):
    for bone, value in LUNGE.items():
        track.set(bone, tick, value)


def settle(track, tick):
    for bone, value in STANCE.items():
        track.set(bone, tick, value)


def slash_single(t):
    """Forma 1, Kuuchi: lamina direita armada no ombro esquerdo e varredura para fora num instante, com avanco."""
    end, hit = t["duration_ticks"], t["windup_ticks"]
    track = Track(end)
    track.arms(hit * 0.6, right=COCKED, left=LOW_OUT)
    track.set("body", hit * 0.6, [22, 35, 0])
    track.arms(hit, right=OPEN, left=[-25, 10, 75])
    track.set("body", hit, [30, -30, 0])
    lunge(track, hit)
    settle(track, hit + (end - hit) * 0.7)
    return track.build()


def slash_cross(t):
    """Forma 2, Kosa-uchi: as duas laminas abertas para cima e fechando em X a frente do peito no tick do corte."""
    end, hit = t["duration_ticks"], t["windup_ticks"]
    track = Track(end)
    wide = [-130, 0, 75]
    track.arms(hit * 0.6, right=wide, left=wide)
    track.set("body", hit * 0.6, [10, 0, 0])
    track.arms(hit, right=CROSS_CHEST, left=CROSS_CHEST)
    track.set("body", hit, [34, 0, 0])
    lunge(track, hit)
    track.arms(hit + 3, right=[-80, -65, -15], left=[-80, -65, -15])
    settle(track, hit + (end - hit) * 0.7)
    return track.build()


# Forma 4 (Ran-uchi): cadeia de quatro cortes; cada um = (braco, preparo, corte, inclinacao do corpo).
WILD = [("right", HIGH_OUT, LOW_IN, [34, -20, 0]), ("left", LOW_OUT, LOW_ACROSS, [36, 20, 0]),
        ("right", DOWN, UP, [12, -10, 0]), ("both", ACROSS, OPEN, [30, 0, 0])]
# Forma 6 (Yae-uchi): oito cortes em estrela, alternando as laminas e a direcao.
STAR = [("right", COCKED, OPEN), ("left", COCKED, OPEN), ("right", HIGH_OUT, LOW_IN), ("left", HIGH_OUT, LOW_IN),
        ("right", LOW_OUT, LOW_ACROSS), ("left", LOW_OUT, LOW_ACROSS), ("right", DOWN, UP), ("left", DOWN, UP)]


def strike(track, side, pre_tick, hit, pre, cut):
    if side in ("right", "both"):
        track.arms(pre_tick, right=pre)
        track.arms(hit, right=cut)
    if side in ("left", "both"):
        track.arms(pre_tick, left=pre)
        track.arms(hit, left=cut)


def finisher(track, pre_tick, hit):
    """Ultimo golpe forte: as duas laminas sobem e descem cruzando, corpo mergulha."""
    track.arms(pre_tick, right=[-170, 0, 30], left=[-170, 0, 30])
    track.set("body", pre_tick, [0, 0, 0])
    track.arms(hit, right=LOW_IN, left=LOW_IN)
    track.set("body", hit, [42, 0, 0])
    lunge(track, hit)


def combo(t, pattern="wild", heavy_last=False):
    """Golpes da tecnica, um por tick de dano; o padrao do anime decide a pose de cada um."""
    end, first, step, count = t["duration_ticks"], t["windup_ticks"], t.get("hit_interval", 2), len(t["hits"])
    track = Track(end)
    lunge(track, first)
    for i in range(count):
        hit = first + i * step
        pre = max(hit - max(step, 1) * 0.5, 0.5)
        if heavy_last and i == count - 1:
            finisher(track, pre, hit)
            continue
        if pattern == "wild":
            side, pre_pose, cut, body = WILD[i % len(WILD)]
            strike(track, side, pre, hit, pre_pose, cut)
            track.set("body", hit, body)
            if side == "both":
                track.spin(pre, hit)
        else:
            side, pre_pose, cut = STAR[i % len(STAR)]
            strike(track, side, pre, hit, pre_pose, cut)
            track.set("body", hit, [30 + (i % 2) * 6, 25 if side == "left" else -25, 0])
    settle(track, first + (count - 1) * step + (end - first - (count - 1) * step) * 0.7)
    return track.build()


def basic_attack(end=12, hit=5):
    """Golpe comum (sem tecnica): varredura invertida da direita e a esquerda acompanhando, impacto no tick 5 (o
    soldado aplica o dano pelo light da espada do Hoshina, impact_tick 4, com folga de saque)."""
    track = Track(end)
    track.arms(hit * 0.5, right=COCKED, left=LOW_OUT)
    track.set("body", hit * 0.5, [26, 25, 0])
    track.arms(hit, right=OPEN, left=COCKED)
    track.set("body", hit, [32, -20, 0])
    lunge(track, hit)
    track.arms(hit + 3, left=OPEN)
    settle(track, hit + (end - hit) * 0.7)
    return track.build()


def kasumi(t):
    """Forma 5, Kasumi-uchi: dois cortes em X (direita e esquerda), passo para o lado e o terceiro baixo pelo lado."""
    end, first, step = t["duration_ticks"], t["windup_ticks"], t.get("hit_interval", 2)
    hits = [first + i * step for i in range(len(t["hits"]))]
    track = Track(end)
    strike(track, "right", hits[0] - 2, hits[0], HIGH_OUT, LOW_IN)
    track.set("body", hits[0], [32, -20, 0])
    strike(track, "left", hits[1] - 2, hits[1], HIGH_OUT, LOW_IN)
    track.set("body", hits[1], [32, 20, 0])
    side = (hits[1] + hits[2]) / 2
    track.set("root", side, [5, -6, 0])
    track.set("body", side, [20, 0, 22])
    track.arms(side, right=LOW_OUT, left=LOW_OUT)
    track.set("root", hits[2], [5, -9, 0])
    track.set("body", hits[2], [45, -40, 10])
    track.set("leg_right", hits[2], [-75, 0, 35])
    track.set("leg_left", hits[2], [50, 0, -30])
    track.arms(hits[2], right=LOW_ACROSS, left=[-30, 10, 80])
    settle(track, hits[2] + (end - hits[2]) * 0.7)
    return track.build()


def counter(c):
    """Forma 3, Kaeshi-uchi: abaixa no dash lateral, cruza as laminas e solta o corte cruzado girando."""
    strike_tick = c["strike_delay_ticks"]
    end = strike_tick + COUNTER_EXTRA_TICKS
    low = strike_tick * 0.4
    track = Track(end)
    track.set("root", low, [0, -8, 0])
    track.set("body", low, [40, 0, 0])
    track.set("leg_right", low, [-70, 0, 30])
    track.set("leg_left", low, [55, 0, -25])
    track.arms(low, right=CROSS_CHEST, left=CROSS_CHEST)
    track.arms(strike_tick, right=OPEN, left=OPEN)
    track.set("body", strike_tick, [30, 0, 0])
    track.spin(low, strike_tick, -360)
    lunge(track, strike_tick)
    settle(track, strike_tick + COUNTER_EXTRA_TICKS * 0.7)
    return track.build()


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


REVERSED = [180, 0, 0]
# Com o braco para tras a lamina invertida apontaria para cima: girada para sair para tras do corpo.
BEHIND = [100, 0, 0]


def arms():
    """As duas laminas sempre invertidas (pegada reversa, lamina ao longo do antebraco). 0.5.0-D4 (referencias do
    Miguel): parado, em guarda e correndo os dois bracos ficam para tras do corpo, o direito mais alto e o esquerdo
    mais baixo, laminas saindo para tras; andando: corpo quase reto e bracos abertos para os lados e para baixo."""
    poses = {"ready": ([65, 0, 20], [30, 0, 15]), "walk": ([10, 0, 40], [10, 0, 40]),
             "run": ([75, 0, 25], [35, 0, 20]), "aim": ([60, 0, 25], [25, 0, 20])}
    lengths = {"ready": (2.0, 2), "walk": (0.7, 6), "run": (0.45, 8), "aim": (2.0, 2)}
    def loop(length, right, left, breath, item):
        def bone(pose, flip):
            moved = [pose[0] - breath, pose[1], pose[2]]
            convert = mirror if flip else (lambda v: v)
            return {"rotation": keys((0, convert(pose)), (length / 2, convert(moved)), (length, convert(pose)))}
        items = {"item_right": {"rotation": keys((0, item))}, "item_left": {"rotation": keys((0, item))}}
        return {"loop": True, "animation_length": length, "bones": dict(items, **{
            "arm_right": bone(right, False), "arm_left": bone(left, True)})}

    return {f"hoshina.arms.blade_{name}": loop(lengths[name][0], right, left, lengths[name][1],
                                               REVERSED if name == "walk" else BEHIND)
            for name, (right, left) in poses.items()}


def movement():
    """0.5.0-D2: corpo inteiro. Parado: bem baixo (root desce para os pes ficarem no chao com as pernas abertas),
    perna direita a frente e aberta, esquerda atras, tronco inclinado e cabeca olhando para a frente. Andando:
    quase reto (referencia 1); correndo: bem baixo e inclinado, passadas longas (referencias 3 e 5)."""
    idle = {"loop": True, "animation_length": 2.0, "bones": {
        "root": {"position": keys((0, [0, -4, 0]), (1.0, [0, -4.3, 0]), (2.0, [0, -4, 0]))},
        "leg_right": {"rotation": keys((0, [-40, 0, 25]))},
        "leg_left": {"rotation": keys((0, [35, 0, -22]))},
        "body": {"rotation": keys((0, [28, 0, 0]), (1.0, [30, 0, 0]), (2.0, [28, 0, 0]))},
        "head": {"rotation": keys((0, [-25, 0, 0]))}}}

    def gait(length, swing, lean, low):
        half, quarter = length / 2, length / 4
        return {"loop": True, "animation_length": length, "bones": {
            "root": {"position": keys((0, [0, -low, 0]), (quarter, [0, -low + 0.7, 0]), (half, [0, -low, 0]),
                                      (half + quarter, [0, -low + 0.7, 0]), (length, [0, -low, 0]))},
            "leg_right": {"rotation": keys((0, [-swing, 0, 8]), (half, [swing, 0, 8]), (length, [-swing, 0, 8]))},
            "leg_left": {"rotation": keys((0, [swing, 0, -8]), (half, [-swing, 0, -8]), (length, [swing, 0, -8]))},
            "body": {"rotation": keys((0, [lean, 0, 0]), (length, [lean, 0, 0]))},
            "head": {"rotation": keys((0, [-lean + 5, 0, 0]), (length, [-lean + 5, 0, 0]))}}}

    return {"hoshina.movement.idle": idle, "hoshina.movement.walk": gait(0.7, 30, 10, 1),
            "hoshina.movement.run": gait(0.45, 50, 42, 6)}


def tail_animations(slash_ticks=10, guard_ticks=9):
    """0.6-F (traje numerado 10): cauda balancando parada, corte da cauda (pico no meio) e guarda (a cauda passa
    pela frente do corpo e segura). Ossos tail_1..tail_4 do rig_soldier_mesh.py hoshina_no10."""
    sway = {"tail_1": [0, 5, 0], "tail_2": [3, 0, 0], "tail_3": [0, -4, 0], "tail_4": [4, 0, 0]}
    idle = {"loop": True, "animation_length": 3.0, "bones": {
        bone: {"rotation": keys((0, [0, 0, 0]), (1.5, v), (3.0, [0, 0, 0]))} for bone, v in sway.items()}}
    end, peak = slash_ticks * TICK, slash_ticks * TICK * 0.4
    slash = {"animation_length": round(end, 3), "bones": {
        "tail_1": {"rotation": keys((0, [0, 0, 0]), (peak * 0.6, [0, 50, 0]), (peak, [10, -70, 0]),
                                    (end, [0, 0, 0]))},
        "tail_2": {"rotation": keys((0, [0, 0, 0]), (peak * 0.6, [-20, 20, 0]), (peak, [15, -30, 0]),
                                    (end, [0, 0, 0]))},
        "tail_3": {"rotation": keys((0, [0, 0, 0]), (peak, [20, -20, 0]), (end, [0, 0, 0]))},
        "tail_4": {"rotation": keys((0, [0, 0, 0]), (peak, [30, 0, 0]), (end, [0, 0, 0]))}}}
    g_end = guard_ticks * TICK
    guard = {"animation_length": round(g_end, 3), "bones": {
        "tail_1": {"rotation": keys((0, [0, 0, 0]), (0.08, [-35, 30, 0]), (g_end * 0.7, [-35, 30, 0]),
                                    (g_end, [0, 0, 0]))},
        "tail_2": {"rotation": keys((0, [0, 0, 0]), (0.08, [-20, 15, 0]), (g_end * 0.7, [-20, 15, 0]),
                                    (g_end, [0, 0, 0]))}}}
    return {"tail.idle": idle, "tail.slash": slash, "tail.guard": guard}


def main():
    species = sys.argv[1] if len(sys.argv) > 1 else "hoshina"
    profile = json.loads((PROFILES / f"{species}.json").read_text(encoding="utf-8"))
    techniques = profile["techniques"]
    out = ANIMATIONS / f"{species}.animation.json"
    raw = out.read_bytes().decode("utf-8")
    newline = "\r\n" if "\r\n" in raw else "\n"
    data = json.loads(raw)
    animations = data["animations"]
    generated = arms()
    generated.update(movement())
    generated["hoshina.action.attack"] = basic_attack()
    generated["hoshina.action.kuuchi"] = slash_single(techniques["kuuchi"])
    generated["hoshina.action.kosa_uchi"] = slash_cross(techniques["kosa_uchi"])
    generated["hoshina.action.ran_uchi"] = combo(techniques["ran_uchi"], "wild")
    generated["hoshina.action.kasumi_uchi"] = kasumi(techniques["kasumi_uchi"])
    generated["hoshina.action.yae_uchi"] = combo(techniques["yae_uchi"], "star", heavy_last=True)
    generated["hoshina.action.kaeshi_uchi"] = counter(profile["counter"])
    generated["hoshina.action.dash"] = dash()
    generated["hoshina.action.parry"] = parry()
    if "juni_hitoe" in techniques:
        # 0.6-F: 12 golpes alternando as espadas, o ultimo com as duas (a cauda acompanha pelo controller dela).
        generated["hoshina.action.juni_hitoe"] = combo(techniques["juni_hitoe"], "star", heavy_last=True)
    # Os nomes sao gerados com o prefixo do Hoshina e trocados pelo da especie.
    generated = {species + name[len("hoshina"):]: anim for name, anim in generated.items()}
    if "numbers10" in profile:
        for name, anim in tail_animations().items():
            generated[f"{species}.{name}"] = anim
    for animation in generated.values():
        # Keyframes em ordem de tempo (o passo lateral do Kasumi-uchi entra depois).
        for bone in animation["bones"].values():
            for channel in ("rotation", "position"):
                if channel in bone:
                    bone[channel] = dict(sorted(bone[channel].items(), key=lambda item: float(item[0])))
    animations.update(generated)
    text = json.dumps(data, indent=2) + "\n"
    out.write_bytes(text.replace("\n", newline).encode("utf-8"))
    for name in generated:
        print(f"{name}: {generated[name]['animation_length']} s")


if __name__ == "__main__":
    main()
