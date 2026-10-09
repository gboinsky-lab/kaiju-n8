#!/usr/bin/env python3
"""Corrige um .bbmodel do Hoshina/jogador sem apagar keyframes (0.5.0-D7, arquivo do Miguel de 2026-10-09).

1. Giro "fantasma": quando um keyframe de rotacao difere do anterior por mais de 180 graus num eixo e o mesmo angulo
   +-360 fica mais perto, troca pelo equivalente (mesma pose no keyframe, sem a volta inteira no meio).
2. Pose parcial em laco: --pose <animacao>@<tempo> faz os keyframes de "ancora" (inicio, meio e fim do laco) dessa
   animacao que ainda guardam a pose antiga virarem a pose do tempo indicado (a respiracao de -2 graus nos bracos,
   do modelo-base, fica no meio do laco). So nos ossos/canais que o Miguel mudou em algum keyframe.
3. --stance-items: nas tecnicas (action.*), a espada fica na rotacao da postura parada, como o jogo faz (o controller
   "arms" continua rodando); acrescenta um keyframe no tempo 0 com essa rotacao, e um keyframe unico de posicao
   passa a ter a posicao da postura. O importador ignora canais de espada das tecnicas iguais aos da postura.
Uso: python3 tools/blockbench/corrigir_poses.py <entrada.bbmodel> <saida.bbmodel> [--pose anim@t ...]
     [--hold anim:osso:canal@t ...] [--stance-items]
"""
import copy
import json
import sys
import uuid


def value(key):
    p = key["data_points"][0]
    return [float(p.get(c, 0) or 0) for c in "xyz"]


def set_value(key, v):
    key["data_points"][0] = {c: f"{round(x, 4):g}" for c, x in zip("xyz", v)}


def channel_keys(anim, bone, channel):
    for animator in anim.get("animators", {}).values():
        if animator.get("name") == bone:
            return sorted((k for k in animator.get("keyframes", []) if k["channel"] == channel),
                          key=lambda k: k["time"])
    return []


def sample(keys, t):
    """Valor linear no tempo t (como o Blockbench com interpolacao linear)."""
    if not keys:
        return None
    if t <= keys[0]["time"]:
        return value(keys[0])
    for a, b in zip(keys, keys[1:]):
        if a["time"] <= t <= b["time"]:
            f = (t - a["time"]) / (b["time"] - a["time"]) if b["time"] > a["time"] else 0
            va, vb = value(a), value(b)
            return [x + (y - x) * f for x, y in zip(va, vb)]
    return value(keys[-1])


def unwrap(model, base, log):
    """So nos keyframes editados pelo Miguel (diferentes do modelo-base): os giros de 360 graus das tecnicas
    originais (Ran-uchi, Kaeshi-uchi) sao de proposito e ficam."""
    base_anims = {a["name"]: a for a in base["animations"]}
    for anim in model["animations"]:
        base_anim = base_anims.get(anim["name"], {"animators": {}})
        for animator in anim.get("animators", {}).values():
            keys = sorted((k for k in animator.get("keyframes", []) if k["channel"] == "rotation"),
                          key=lambda k: k["time"])
            old = {round(k["time"], 4): value(k) for k in channel_keys(base_anim, animator["name"], "rotation")}
            # O primeiro keyframe nao tem anterior: confere contra o seguinte (367 no t=0 e 47 no seguinte = volta
            # de 320 graus no comeco do golpe; arquivo do Miguel de 2026-10-09).
            if len(keys) > 1 and old.get(round(keys[0]["time"], 4)) != value(keys[0]):
                first, nxt = value(keys[0]), value(keys[1])
                fixed = list(first)
                for i in range(3):
                    while fixed[i] - nxt[i] > 180:
                        fixed[i] -= 360
                    while fixed[i] - nxt[i] < -180:
                        fixed[i] += 360
                if fixed != first:
                    set_value(keys[0], fixed)
                    log.append(f"{anim['name']} {animator['name']} rotacao t={keys[0]['time']}: {first} -> {fixed}"
                               " (mesma pose, sem a volta inteira)")
            for prev, key in zip(keys, keys[1:]):
                if old.get(round(key["time"], 4)) == value(key):
                    continue
                pv, v = value(prev), value(key)
                fixed = list(v)
                for i in range(3):
                    while fixed[i] - pv[i] > 180:
                        fixed[i] -= 360
                    while fixed[i] - pv[i] < -180:
                        fixed[i] += 360
                if fixed != v:
                    # So aceita se a orientacao e a mesma: +-360 por eixo sempre e (Rz Ry Rx).
                    set_value(key, fixed)
                    log.append(f"{anim['name']} {animator['name']} rotacao t={key['time']}: {v} -> {fixed}"
                               " (mesma pose, sem a volta inteira)")


def hold_pose(model, base, anim_name, t, log, only=None):
    """Keyframes que ainda guardam o valor do modelo-base (pose antiga) passam a ter a pose do tempo t. Keyframe que o
    Miguel editou nunca muda. Sem only: so as ancoras do laco (inicio, meio, fim) dos canais que ele mexeu; com only
    {(osso, canal)}: todos os keyframes antigos daquele canal."""
    anim = next(a for a in model["animations"] if a["name"] == anim_name)
    base_anim = next(a for a in base["animations"] if a["name"] == anim_name)
    length = anim["length"]
    anchors = {round(x, 4) for x in (0, length / 2, length)}
    for animator in anim.get("animators", {}).values():
        bone = animator["name"]
        for channel in ("rotation", "position", "scale"):
            if only is not None and (bone, channel) not in only:
                continue
            keys = channel_keys(anim, bone, channel)
            old = {round(k["time"], 4): value(k) for k in channel_keys(base_anim, bone, channel)}
            # Canal que o modelo-base nao tinha: o "valor antigo" e o repouso (0; escala 1).
            rest = [1.0, 1.0, 1.0] if channel == "scale" else [0.0, 0.0, 0.0]
            if not old:
                old = {round(k["time"], 4): rest for k in keys if value(k) == rest}
            if all(old.get(round(k["time"], 4)) == value(k) for k in keys):
                continue  # canal sem edicao do Miguel
            target = sample(keys, t)
            for key in keys:
                tk = round(key["time"], 4)
                if old.get(tk) != value(key):
                    continue  # editado pelo Miguel: fica
                if only is None and tk not in anchors:
                    continue
                v = list(target)
                # Respiracao do modelo-base (2 graus nos bracos no meio do laco).
                if only is None and channel == "rotation" and bone.startswith("arm") and tk == round(length / 2, 4):
                    v[0] -= 2
                if value(key) != v:
                    log.append(f"{anim_name} {bone} {channel} t={key['time']}: {[round(x, 2) for x in value(key)]}"
                               f" -> {[round(x, 2) for x in v]}")
                    set_value(key, v)


def stance_items(model, log):
    idle = next(a for a in model["animations"] if a["name"].endswith(".parado"))
    for anim in model["animations"]:
        if ".action." not in anim["name"]:
            continue
        for animator_id, animator in idle["animators"].items():
            bone = animator["name"]
            if not bone.startswith("item"):
                continue
            rot = sample(channel_keys(idle, bone, "rotation"), 0)
            target = anim.setdefault("animators", {}).setdefault(
                animator_id, {"name": bone, "type": "bone", "keyframes": []})
            target.setdefault("keyframes", [])
            pos = sample(channel_keys(idle, bone, "position"), 0)
            for key in target["keyframes"]:
                # Posicao fixa posta para compensar o modelo-base antigo (que mostrava a espada sem a rotacao da
                # postura): passa a ser a da postura parada, senao a espada desliza na mao quando a tecnica comeca.
                if key["channel"] == "position" and pos is not None and value(key) != pos and len(
                        [k for k in target["keyframes"] if k["channel"] == "position"]) == 1:
                    log.append(f"{anim['name']} {bone} posicao t={key['time']}: {value(key)} -> "
                               f"{[round(x, 2) for x in pos]} (posicao da postura parada)")
                    set_value(key, pos)
            if any(k["channel"] == "rotation" for k in target["keyframes"]):
                continue
            key = {"channel": "rotation", "data_points": [{}], "uuid": str(uuid.uuid4()), "time": 0, "color": -1,
                   "interpolation": "linear", "bezier_linked": True, "bezier_left_time": [-0.1, -0.1, -0.1],
                   "bezier_left_value": [0, 0, 0], "bezier_right_time": [0.1, 0.1, 0.1],
                   "bezier_right_value": [0, 0, 0]}
            set_value(key, rot)
            target["keyframes"].append(key)
            log.append(f"{anim['name']} {bone}: rotacao da postura parada no t=0 ({[round(x, 2) for x in rot]})")


def new_key(channel, time, v):
    key = {"channel": channel, "data_points": [{}], "uuid": str(uuid.uuid4()), "time": round(time, 4), "color": -1,
           "interpolation": "linear", "bezier_linked": True, "bezier_left_time": [-0.1, -0.1, -0.1],
           "bezier_left_value": [0, 0, 0], "bezier_right_time": [0.1, 0.1, 0.1], "bezier_right_value": [0, 0, 0]}
    set_value(key, v)
    return key


def animators_by_bone(anim):
    return {a.get("name"): a for a in anim.get("animators", {}).values()}


def hold_loop(model, anim_name, t, log, breath=2.0):
    """0.5.0-D7 (Miguel: "paradas muito bruscas"): postura parada = a pose do tempo t no laco inteiro, em todos os
    canais (keyframes antigos ganham o valor da pose; nada e apagado). Respiracao: bracos e tronco descem `breath`
    graus no meio do laco e o laco fecha no mesmo valor."""
    anim = next(a for a in model["animations"] if a["name"] == anim_name)
    length = anim["length"]
    for bone, animator in animators_by_bone(anim).items():
        for channel in ("rotation", "position", "scale"):
            keys = channel_keys(anim, bone, channel)
            if not keys:
                continue
            pose = sample(keys, t)
            times = {round(k["time"], 4) for k in keys}
            for extra in (0.0, length / 2, length):
                if round(extra, 4) not in times:
                    animator["keyframes"].append(new_key(channel, extra, pose))
            for key in channel_keys(anim, bone, channel):
                v = list(pose)
                mid = abs(key["time"] - length / 2) < 1e-3
                if mid and channel == "rotation" and bone.startswith("arm"):
                    v[0] -= breath
                if mid and channel == "rotation" and bone == "body":
                    v[0] += breath / 2
                if value(key) != v:
                    set_value(key, v)
    log.append(f"{anim_name}: pose de {t} no laco inteiro (respiracao {breath} graus)")


def smooth_loop(model, anim_name, harmonics, log, samples_per_loop=24):
    """Cada canal vira um ciclo continuo (serie de Fourier com ate `harmonics` voltas por laco): somem os picos de
    0,05 s e a emenda do laco fecha. Os keyframes existentes recebem o valor da curva e entram keyframes a cada
    1/samples_per_loop do laco para a interpolacao linear seguir a curva."""
    import numpy as np
    anim = next(a for a in model["animations"] if a["name"] == anim_name)
    length = anim["length"]
    grid = np.linspace(0, length, 240, endpoint=False)
    for bone, animator in animators_by_bone(anim).items():
        for channel in ("rotation", "position", "scale"):
            keys = channel_keys(anim, bone, channel)
            if len(keys) < 2:
                continue
            data = np.array([sample(keys, t) for t in grid])
            spectrum = np.fft.rfft(data, axis=0)
            spectrum[harmonics + 1:] = 0
            def curve(t):
                phase = 2 * np.pi * t / length
                out = spectrum[0].real / len(grid)
                for h in range(1, harmonics + 1):
                    c = spectrum[h] * 2 / len(grid)
                    out = out + c.real * np.cos(h * phase) - c.imag * np.sin(h * phase)
                return [round(float(x), 4) for x in out]
            times = {round(k["time"], 4) for k in keys}
            for i in range(samples_per_loop + 1):
                t = round(length * i / samples_per_loop, 4)
                if t not in times:
                    animator["keyframes"].append(new_key(channel, t, curve(t)))
            for key in channel_keys(anim, bone, channel):
                set_value(key, curve(key["time"]))
    log.append(f"{anim_name}: canais suavizados como ciclo (ate {harmonics} harmonicos)")


def fix_channel(model, anim_name, bone, channel, t, log):
    """Todos os keyframes do canal recebem o valor do tempo t (ex.: espada que girava 80 graus a cada passo)."""
    anim = next(a for a in model["animations"] if a["name"] == anim_name)
    keys = channel_keys(anim, bone, channel)
    pose = sample(keys, t)
    for key in keys:
        set_value(key, pose)
    log.append(f"{anim_name} {bone} {channel}: fixo no valor de t={t}")


def actions_from_stance(model, stance_name, log):
    """Tecnicas comecam e terminam na postura parada: o primeiro e o ultimo keyframe de cada canal que a postura tem
    recebem o valor dela (o golpe nao pula da postura para a pose antiga)."""
    stance = next(a for a in model["animations"] if a["name"] == stance_name)
    for anim in model["animations"]:
        if ".action." not in anim["name"]:
            continue
        for bone, animator in animators_by_bone(anim).items():
            if bone.startswith("item"):
                continue
            for channel in ("rotation", "position"):
                keys = channel_keys(anim, bone, channel)
                pose_keys = channel_keys(stance, bone, channel)
                if not keys or not pose_keys:
                    continue
                pose = sample(pose_keys, 0)
                for key in (keys[0], keys[-1]):
                    if abs(key["time"]) < 1e-3 or abs(key["time"] - anim["length"]) < 1e-3:
                        if value(key) != pose:
                            set_value(key, pose)
        log.append(f"{anim['name']}: comeca e termina na postura de {stance_name}")


def set_length(model, anim_name, length, log):
    anim = next(a for a in model["animations"] if a["name"] == anim_name)
    log.append(f"{anim_name}: duracao {anim['length']} -> {length}")
    anim["length"] = length


def main():
    args = sys.argv[1:]
    src, dst = args[0], args[1]
    base_path = None
    poses, holds, lengths, items = [], [], [], False
    holds_loop, smooths, stance_actions, fixes = [], [], None, []
    i = 2
    while i < len(args):
        if args[i] == "--pose":
            poses.append(args[i + 1]); i += 2
        elif args[i] == "--hold":
            holds.append(args[i + 1]); i += 2
        elif args[i] == "--base":
            base_path = args[i + 1]; i += 2
        elif args[i] == "--segurar":
            holds_loop.append(args[i + 1]); i += 2
        elif args[i] == "--fixar":
            fixes.append(args[i + 1]); i += 2
        elif args[i] == "--suavizar":
            smooths.append(args[i + 1]); i += 2
        elif args[i] == "--acoes-da-postura":
            stance_actions = args[i + 1]; i += 2
        elif args[i] == "--length":
            lengths.append(args[i + 1]); i += 2
        elif args[i] == "--stance-items":
            items = True; i += 1
        else:
            raise SystemExit(f"argumento desconhecido: {args[i]}")
    model = json.load(open(src, encoding="utf-8"))
    base = json.load(open(base_path, encoding="utf-8")) if base_path else copy.deepcopy(model)
    log = []
    unwrap(model, base, log)
    for spec in poses:
        anim, t = spec.split("@")
        hold_pose(model, base, anim, float(t), log)
    for spec in holds:
        what, t = spec.split("@")
        anim, bone, channel = what.split(":")
        hold_pose(model, base, anim, float(t), log, only={(bone, channel)})
    for spec in lengths:
        anim, length = spec.split("=")
        set_length(model, anim, float(length), log)
    for spec in holds_loop:
        anim, t = spec.split("@")
        hold_loop(model, anim, float(t), log)
    for spec in smooths:
        anim, harmonics = spec.split("@")
        smooth_loop(model, anim, int(harmonics), log)
    for spec in fixes:
        what, t = spec.split("@")
        anim, bone, channel = what.split(":")
        fix_channel(model, anim, bone, channel, float(t), log)
    if stance_actions:
        actions_from_stance(model, stance_actions, log)
    if items:
        stance_items(model, log)
    json.dump(model, open(dst, "w", encoding="utf-8"), separators=(",", ":"))
    print("\n".join(log))


if __name__ == "__main__":
    main()
