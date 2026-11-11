#!/usr/bin/env python3
"""FASE 2-B (documento mestre, Miguel 2026-11-11): pegada da arma de cada personagem, por personagem.

Os personagens dividem as posturas de braco por familia (gen_special_animations.py: rifle, spear, katana, flail...) e a
arma e montada pelo display do item: armas de formatos diferentes (arco, minigun, Pile Driver, bola) ficavam com a
pegada de outra (o arco como rifle, a minigun apontando para o chao, o Pile Driver atravessado no peito). Este script
regrava SO as posturas arms.<familia>_ready/walk/run/aim da especie (bracos e giro do osso item_right), mantendo o
balanco que ja havia nos bracos, e:
  - Kikoru (e Kikoru No. 4): a posicao do machado e refeita pela rotacao para a mao ficar no mesmo ponto do cabo
    (grip_gecko + axe_grip_points, a conta do gen_kikoru_animations.py);
  - Mina: ajoelha nos tiros das tecnicas (Miguel: "no joelho, segurando na frente e atras do canhao"): pernas e
    altura do corpo por cima das action.<tecnica>, descendo nos primeiros ticks e levantando nos ultimos.
Numeros escolhidos na previa (tools/art/rig_audit.py com a arma) e conferidos em jogo [SUPOSICAO angulos].

Rodar DEPOIS de todos os geradores da especie (rig_soldier_mesh, gen_special_animations, gen_kikoru_animations,
gen_vault_animations, humanoid_anims) e antes do vfx_keyframes.py (ele so acrescenta marcas). Rodar de novo da o
mesmo resultado.
Uso: python3 tools/art/weapon_grips.py [especie...]   (padrao: todas da tabela)
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))

ROOT = Path(__file__).resolve().parents[2]
ANIMS = ROOT / "src/main/resources/assets/kn8/animations/entity"
STANCES = ("ready", "walk", "run", "aim")

# especie -> (familia, braco direito, braco esquerdo, item_right, extras). Os mesmos numeros nas quatro posturas
# (arma pesada/longa: a pose de correr nao muda a pegada). "grip": "axe" = posicao do machado pela rotacao.
GRIPS = {
    # Minigun no quadril, cano na horizontal para a frente, a esquerda por baixo do corpo da arma.
    "rin": ("rifle", [-28, -10, 0], [-55, 35, 0], [-60, 0, 0], {}),
    # Arco em pe, segurado pelo meio com o braco estendido a frente (o modelo tem uma arma so: a mao direita).
    "haruichi": ("rifle", [-80, -5, 0], [-40, 30, 0], [-45, 0, 0], {}),
    # Pile Driver na horizontal a frente do peito, a estaca para a frente (segurado pelas alcas).
    "aoi": ("spear", [-60, -10, 0], [-60, 30, 0], [45, 0, 0], {}),
    # Bola de ferro pendurada pela corda ao lado do corpo.
    "jura": ("flail", [-30, 0, 15], [-10, 10, -10], [60, 0, 0], {}),
    # Katana em guarda: lamina para cima e para a frente, sem cruzar o rosto.
    "soichiro": ("katana", [-40, -25, 0], [-45, 30, 0], [0, 30, 0], {}),
    # Baioneta com as duas maos baixas, lamina apontada para a frente.
    "narumi": ("spear", [-30, -15, 0], [-50, 40, 0], [30, 30, 0], {}),
    # Machado em guarda baixa ao lado direito, cabeca perto do chao a frente (como a pegada do jogador, AXE_LOW).
    "kikoru": ("axe", [-20, -10, 10], [-35, 30, 0], [60, 0, 0], {"grip": "axe"}),
    "kikoru_no4": ("axe", [-20, -10, 10], [-35, 30, 0], [60, 0, 0], {"grip": "axe"}),
    # Canhao da Mina na horizontal no quadril, segurado na frente (esquerda) e atras (direita).
    "mina": ("cannon", [-20, -5, 0], [-55, 40, 0], [55, 0, 0], {}),
}
# Mina ajoelhada nas tecnicas de tiro: direcao de cada segmento da perna (espaco do modelo: +X direita, +Y cima,
# -Z frente), como o Hoshina No. 10 parado (humanoid_anims.NO10_KNEEL_LEGS): joelho direito no chao, perna esquerda
# a frente com a canela em pe.
KNEEL = {"species": "mina", "actions": ("precision_shot", "cannon_shot", "charged_cannon", "anti_giant_shot"),
         "legs": {"right": {"leg": (0.15, -1.0, -0.35), "shin": (0.0, -0.15, 1.0), "foot": (0.0, -0.7, 1.0)},
                  "left": {"leg": (-0.15, -0.25, -1.0), "shin": (0.0, -1.0, 0.05), "foot": (0.0, 0.0, -1.0)}},
         "body": [8, 0, 0], "ease_ticks": 4}


def load(species):
    path = ANIMS / f"{species}.animation.json"
    raw = path.read_bytes().decode("utf-8")
    return path, json.loads(raw), "\r\n" if "\r\n" in raw else "\n"


def save(path, data, newline):
    path.write_bytes((json.dumps(data, indent=2) + "\n").replace("\n", newline).encode("utf-8"))


def first(channel):
    key = sorted(channel, key=float)[0]
    value = channel[key]
    return value if isinstance(value, list) else value.get("post", value.get("vector"))


def regrip(species, data):
    family, right, left, item, extra = GRIPS[species]
    changed = []
    for stance in STANCES:
        name = f"{species}.arms.{family}_{stance}"
        anim = data["animations"].get(name)
        if anim is None:
            continue
        for bone, target in (("arm_right", right), ("arm_left", left)):
            channel = anim["bones"].setdefault(bone, {}).get("rotation") or {"0.0": target}
            base = first(channel)
            # Mantem o balanco (diferenca de cada keyframe para o primeiro) em volta da pose nova.
            anim["bones"][bone]["rotation"] = {
                t: [round(target[i] + (v if isinstance(v, list) else first({t: v}))[i] - base[i], 2) for i in range(3)]
                for t, v in channel.items()}
        length = anim.get("animation_length", 1.0)
        anim["bones"].setdefault("item_right", {})["rotation"] = {"0.0": list(item), str(length): list(item)}
        anim["bones"]["item_right"].pop("position", None)
        if extra.get("grip") == "axe":
            axe_grip(species, anim)
        changed.append(name)
    return changed


def axe_grip(species, anim):
    import gen_kikoru_animations as gk
    from blockbench_templates import grip_gecko, hoshina_bones, hoshina_frames
    gk.SPECIES = species
    points, _ = gk.axe_grip_points()
    grip_gecko(anim, hoshina_frames(hoshina_bones(species)), points)


def kneel(data):
    import numpy as np
    import preview_rig
    from humanoid_anims import _aim, _keyframe
    from preview_rig import sample
    species = KNEEL["species"]
    rig = preview_rig.Rig(species)
    pose = {}
    for side, dirs in KNEEL["legs"].items():
        hip, knee, ankle = (rig.pivot(f"{b}_{side}") for b in ("leg", "shin", "foot"))
        foot = rig.meshes.get(f"foot_{side}")
        toe = foot[0].reshape(-1, 3)[np.argmin(foot[0].reshape(-1, 3)[:, 2])] if foot else ankle + [0, 0, -0.1]
        parent = np.eye(3)
        for bone, rest in (("leg", knee - hip), ("shin", ankle - knee), ("foot", toe - ankle)):
            local = _aim(rest, parent.T @ np.asarray(dirs[bone], float))
            pose[f"{bone}_{side}"] = _keyframe(local)
            parent = parent @ local
    # Altura: o ponto mais baixo das pernas ajoelhadas encosta no chao (como o Hoshina No. 10).
    rig.anims[f"{species}.try.kneel"] = {"animation_length": 1.0, "bones": {
        b: {"rotation": {"0.0": r}} for b, r in pose.items()}}
    rot, pos = rig.pose("try.kneel@0")
    world = rig.world(rot, pos)
    low = min(float((m[0].reshape(-1, 3) @ world[b][:3, :3].T + world[b][:3, 3])[:, 1].min())
              for b, m in rig.meshes.items() if b.startswith(("leg", "shin", "foot")))
    drop = round(-low * 16, 2)
    ease = KNEEL["ease_ticks"] / 20
    changed = []
    for action in KNEEL["actions"]:
        name = f"{species}.action.{action}"
        anim = data["animations"].get(name)
        if anim is None:
            continue
        length = anim["animation_length"]
        times = ["0.0", str(round(ease, 3)), str(round(max(ease, length - ease), 3)), str(length)]
        # Bracos e canhao na pegada da postura (frente e tras), com o coice da animacao por cima.
        _, right, left, item, _ = GRIPS[species]
        for bone, target in (("arm_right", right), ("arm_left", left), ("item_right", item)):
            channel = anim["bones"].get(bone, {}).get("rotation")
            if not channel:
                continue
            base = first(channel)
            out = {}
            for t, v in channel.items():
                value = [target[i] + (v if isinstance(v, list) else first({t: v}))[i] - base[i] for i in range(3)]
                out[t] = [round(x, 2) for x in value]
            anim["bones"][bone]["rotation"] = out
        for bone, rot in pose.items():
            anim["bones"].setdefault(bone, {})["rotation"] = {
                times[0]: [0, 0, 0], times[1]: rot, times[2]: rot, times[3]: [0, 0, 0]}
            anim["bones"][bone].pop("position", None)
        root = anim["bones"].setdefault("root", {})
        root["position"] = {times[0]: [0, 0, 0], times[1]: [0, drop, 0], times[2]: [0, drop, 0], times[3]: [0, 0, 0]}
        level_cannon(species, data, name)
        changed.append(name)
    print(f"  {species}: ajoelha {drop} px nas tecnicas de tiro")
    return changed


def cannon_pitch(audit, spec):
    """Inclinacao (graus, + = para cima) do eixo mais longo da arma no mundo, na pose spec (so a animacao dada: no
    jogo o controller de acao substitui os ossos que anima, nao soma)."""
    import math
    import numpy as np
    rot, pos = audit.pose(spec)
    world = audit.world(rot, pos)
    pts, _ = audit.item_world("item_right", world, rot, pos)
    axis = np.linalg.svd(pts - pts.mean(0), full_matrices=False)[2][0]
    if axis[2] > 0:
        axis = -axis
    return math.degrees(math.atan2(axis[1], math.hypot(axis[0], axis[2])))


def level_cannon(species, data, name):
    """O canhao no tiro ajoelhado fica com a mesma inclinacao da postura de pe (horizontal): mede cada keyframe do
    giro do item na previa (rig_audit, a conta do SoldierRenderer) e corrige o X ate bater. O coice continua: a
    diferenca de cada keyframe para a postura e medida na animacao e mantida."""
    import rig_audit
    audit = rig_audit.Audit(species)
    family = GRIPS[species][0]
    audit.anims.update(data["animations"])
    target = cannon_pitch(audit, f"arms.{family}_ready@0")
    channel = data["animations"][name]["bones"]["item_right"]["rotation"]
    kick = {t: v[0] - GRIPS[species][3][0] for t, v in channel.items()}
    short = name.split(".", 1)[1]
    for t in channel:
        for _ in range(4):
            audit.anims.update(data["animations"])
            error = cannon_pitch(audit, f"{short}@{t}") - (target - kick[t])
            if abs(error) < 0.5:
                break
            channel[t][0] = round(channel[t][0] + error, 2)


def main():
    names = sys.argv[1:] or list(GRIPS) + [KNEEL["species"]]
    for species in names:
        path, data, newline = load(species)
        changed = []
        if species in GRIPS:
            changed += regrip(species, data)
        if species == KNEEL["species"]:
            changed += kneel(data)
        save(path, data, newline)
        print(f"{species}: {len(changed)} animacoes")


if __name__ == "__main__":
    main()
