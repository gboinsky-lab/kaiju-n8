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
        for channel in ("rotation", "position"):
            if only is not None and (bone, channel) not in only:
                continue
            keys = channel_keys(anim, bone, channel)
            old = {round(k["time"], 4): value(k) for k in channel_keys(base_anim, bone, channel)}
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


def main():
    args = sys.argv[1:]
    src, dst = args[0], args[1]
    base_path = None
    poses, holds, items = [], [], False
    i = 2
    while i < len(args):
        if args[i] == "--pose":
            poses.append(args[i + 1]); i += 2
        elif args[i] == "--hold":
            holds.append(args[i + 1]); i += 2
        elif args[i] == "--base":
            base_path = args[i + 1]; i += 2
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
    if items:
        stance_items(model, log)
    json.dump(model, open(dst, "w", encoding="utf-8"), separators=(",", ":"))
    print("\n".join(log))


if __name__ == "__main__":
    main()
