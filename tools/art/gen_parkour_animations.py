#!/usr/bin/env python3
"""Animacoes de parkour do jogador (0.5.0-E) -> src/main/resources/assets/kn8/player_animations/parkour.json.

Nomes kn8:player.parkour.<nome>, tocados pelo servidor (ParkourService) na camada de combate da PAL. Ossos da PAL
direto (sem o to_pal das armas: no rolamento e nos saltos as pernas giram junto com o corpo, nao descontam a
inclinacao). Convencoes da PAL conferidas nas animacoes de combate: X negativo = braco/perna para a frente (-180 =
braco para cima), Z positivo = braco direito para fora; "body" gira o jogador inteiro em volta do quadril e
"body_pos" Y negativo abaixa o corpo (px).

Uso: python3 tools/art/gen_parkour_animations.py
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "src/main/resources/assets/kn8/player_animations/parkour.json"
ZERO = [0, 0, 0]


def animate(frames, length, loop=None):
    """frames [(segundos, {chave: vetor})]; chave = osso (rotacao) ou osso_pos (posicao). Osso que falta num frame
    volta a 0 (repouso)."""
    channels = sorted({key for _, pose in frames for key in pose})
    bones = {}
    for key in channels:
        bone, channel = (key[:-4], "position") if key.endswith("_pos") else (key, "rotation")
        series = {f"{round(t, 3)}": {"vector": list(pose.get(key, ZERO))} for t, pose in frames}
        bones.setdefault(bone, {})[channel] = series
    anim = {"animation_length": round(length, 3), "bones": bones}
    if loop is not None:
        anim = {"loop": loop, **anim}
    return anim


def run_cycle(period, lean, swing=45, arm_swing=40, extra=None, steps=4):
    """Laco de corrida: pernas e bracos alternados. extra = pose somada em todos os frames (ex.: braco na parede)."""
    frames = []
    for i in range(steps + 1):
        phase = [1, 0, -1, 0, 1][i * 4 // steps]
        pose = {
            "body": lean,
            "right_leg": [-swing * phase, 0, 0],
            "left_leg": [swing * phase, 0, 0],
            "right_arm": [arm_swing * phase, 0, 0],
            "left_arm": [-arm_swing * phase, 0, 0],
        }
        pose.update(extra or {})
        frames.append((period * i / steps, pose))
    return animate(frames, period, loop=True)


def wall_run(side):
    """side +1 = parede a direita: corpo inclina para a parede e o braco desse lado encosta nela."""
    arm = "right_arm" if side > 0 else "left_arm"
    return run_cycle(0.5, [10, 0, 18 * side], extra={arm: [-40, 0, 70 * side]})


ANIMATIONS = {
    # Correndo: agacha, lanca com os bracos para cima, encolhe as pernas no ar e estica para pousar.
    "long_jump": animate([
        (0.0, {}),
        (0.08, {"body": [25, 0, 0], "body_pos": [0, -2, 0], "right_arm": [40, 0, 10], "left_arm": [40, 0, -10],
                "right_leg": [-20, 0, 0], "left_leg": [20, 0, 0]}),
        (0.2, {"body": [10, 0, 0], "right_arm": [-160, 0, 15], "left_arm": [-160, 0, -15],
               "right_leg": [-35, 0, 0], "left_leg": [30, 0, 0]}),
        (0.45, {"body": [15, 0, 0], "right_arm": [-100, 0, 20], "left_arm": [-100, 0, -20],
                "right_leg": [-70, 0, 0], "left_leg": [-50, 0, 0]}),
        (0.7, {"body": [5, 0, 0], "right_arm": [-40, 0, 15], "left_arm": [-40, 0, -15],
               "right_leg": [-30, 0, 0], "left_leg": [-20, 0, 0]}),
        (0.85, {}),
    ], 0.85),
    # Vault: maos no obstaculo, pernas juntas passando de lado, pousa.
    "vault": animate([
        (0.0, {}),
        (0.08, {"body": [30, 0, 0], "right_arm": [-80, 0, 5], "left_arm": [-80, 0, -5]}),
        (0.2, {"body": [25, -25, 0], "body_pos": [0, 2, 0], "right_arm": [-60, 0, 10], "left_arm": [-60, 0, -10],
               "right_leg": [-80, 0, 35], "left_leg": [-80, 0, 35]}),
        (0.35, {"body": [10, -10, 0], "right_arm": [-30, 0, 30], "left_arm": [-30, 0, -30],
                "right_leg": [-40, 0, 10], "left_leg": [-25, 0, 10]}),
        (0.5, {}),
    ], 0.5),
    # Escalada curta: bracos alternados para cima puxando, pernas empurrando.
    "climb": animate([
        (0.0, {}),
        (0.1, {"body": [20, 0, 0], "right_arm": [-170, 0, 10], "left_arm": [-140, 0, -10],
               "right_leg": [-60, 0, 0], "left_leg": [10, 0, 0]}),
        (0.25, {"body": [25, 0, 0], "right_arm": [-120, 0, 10], "left_arm": [-170, 0, -10],
                "right_leg": [0, 0, 0], "left_leg": [-70, 0, 0]}),
        (0.4, {"body": [35, 0, 0], "right_arm": [-60, 0, 10], "left_arm": [-60, 0, -10],
               "right_leg": [-50, 0, 0], "left_leg": [-20, 0, 0]}),
        (0.6, {}),
    ], 0.6),
    "wall_run_right": wall_run(1),
    "wall_run_left": wall_run(-1),
    # Escorregando de frente para a parede: maos na parede, uma perna dobrada encostada.
    "wall_slide": animate([
        (0.0, {"body": [-5, 0, 0], "right_arm": [-150, 0, 20], "left_arm": [-145, 0, -20],
               "right_leg": [-35, 0, 0], "left_leg": [-5, 0, 0], "head": [20, 0, 0]}),
        (0.4, {"body": [-5, 0, 0], "right_arm": [-145, 0, 20], "left_arm": [-150, 0, -20],
               "right_leg": [-30, 0, 0], "left_leg": [-8, 0, 0], "head": [20, 0, 0]}),
        (0.8, {"body": [-5, 0, 0], "right_arm": [-150, 0, 20], "left_arm": [-145, 0, -20],
               "right_leg": [-35, 0, 0], "left_leg": [-5, 0, 0], "head": [20, 0, 0]}),
    ], 0.8, loop=True),
    # Pulo na parede: corpo vira para longe, bracos abrem, pernas encolhem e esticam.
    "wall_jump": animate([
        (0.0, {"body": [-10, 0, 0], "right_arm": [-140, 0, 20], "left_arm": [-140, 0, -20],
               "right_leg": [-60, 0, 0], "left_leg": [-60, 0, 0]}),
        (0.15, {"body": [-15, 0, 0], "right_arm": [-60, 0, 60], "left_arm": [-60, 0, -60],
                "right_leg": [-20, 0, 0], "left_leg": [10, 0, 0]}),
        (0.4, {"body": [5, 0, 0], "right_arm": [-30, 0, 30], "left_arm": [-30, 0, -30],
               "right_leg": [-25, 0, 0], "left_leg": [-10, 0, 0]}),
        (0.55, {}),
    ], 0.55),
    # Pendurado: bracos para cima segurando a borda, pernas balancando de leve.
    "ledge_hang": animate([
        (0.0, {"right_arm": [-172, 0, 12], "left_arm": [-172, 0, -12], "right_leg": [-8, 0, 0],
               "left_leg": [6, 0, 0], "head": [-15, 0, 0]}),
        (0.75, {"right_arm": [-170, 0, 12], "left_arm": [-170, 0, -12], "right_leg": [6, 0, 0],
                "left_leg": [-8, 0, 0], "head": [-15, 0, 0]}),
        (1.5, {"right_arm": [-172, 0, 12], "left_arm": [-172, 0, -12], "right_leg": [-8, 0, 0],
               "left_leg": [6, 0, 0], "head": [-15, 0, 0]}),
    ], 1.5, loop=True),
    # Subindo a borda: puxa com os bracos, joelho em cima e fica de pe.
    "ledge_climb": animate([
        (0.0, {"right_arm": [-172, 0, 12], "left_arm": [-172, 0, -12]}),
        (0.15, {"body": [30, 0, 0], "right_arm": [-90, 0, 15], "left_arm": [-90, 0, -15],
                "right_leg": [-90, 0, 0], "left_leg": [-20, 0, 0]}),
        (0.35, {"body": [20, 0, 0], "right_arm": [-30, 0, 15], "left_arm": [-30, 0, -15],
                "right_leg": [-40, 0, 0], "left_leg": [-60, 0, 0]}),
        (0.5, {}),
    ], 0.5),
    # Slide: o jogador fica deitado (pose de rastejar do vanilla, passa por vao de 1 bloco); mergulho de cabeca com
    # os bracos a frente e as pernas esticadas, segura a pose ate o servidor encerrar.
    "slide": animate([
        (0.0, {}),
        (0.12, {"right_arm": [-170, 0, 15], "left_arm": [-170, 0, -15], "right_leg": [5, 0, 5],
                "left_leg": [5, 0, -5], "head": [-35, 0, 0]}),
    ], 0.12, loop="hold_on_last_frame"),
    # Rolamento: cambalhota para a frente rente ao chao. Termina em 360 e salta para 0 (mesma orientacao) para a
    # volta a postura nao desenrolar o giro.
    "roll": animate([
        (0.0, {"body_pos": [0, -4, 0], "right_leg": [-60, 0, 0], "left_leg": [-60, 0, 0]}),
        (0.12, {"body": [110, 0, 0], "body_pos": [0, -10, -4], "right_arm": [-60, 0, 20], "left_arm": [-60, 0, -20],
                "right_leg": [-110, 0, 0], "left_leg": [-110, 0, 0]}),
        (0.25, {"body": [220, 0, 0], "body_pos": [0, -12, -8], "right_arm": [-40, 0, 20], "left_arm": [-40, 0, -20],
                "right_leg": [-120, 0, 0], "left_leg": [-120, 0, 0]}),
        (0.38, {"body": [330, 0, 0], "body_pos": [0, -8, -10], "right_leg": [-90, 0, 0], "left_leg": [-90, 0, 0],
                "right_arm": [-30, 0, 15], "left_arm": [-30, 0, -15]}),
        (0.45, {"body": [360, 0, 0], "body_pos": [0, -4, -10], "right_leg": [-50, 0, 0], "left_leg": [-40, 0, 0]}),
        (0.451, {"body": [0, 0, 0], "body_pos": [0, -4, -10], "right_leg": [-50, 0, 0], "left_leg": [-40, 0, 0]}),
        (0.6, {}),
    ], 0.6),
}


def main():
    data = {"format_version": "1.8.0", "animations": {
        f"player.parkour.{name}": anim for name, anim in ANIMATIONS.items()}}
    OUT.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
    print(f"{OUT.relative_to(ROOT)}: {len(ANIMATIONS)} animacoes")


if __name__ == "__main__":
    main()
