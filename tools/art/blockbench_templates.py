#!/usr/bin/env python3
"""Modelos-base do Blockbench para animar (0.5.0-D5, Miguel: animar no Blockbench) e volta para o jogo.

Gera, para o Blockbench (formato Generic Model, .bbmodel), com as animacoes atuais ja dentro:
  - jogador_espadas_duplas: boneco do jogador com uma espada do Hoshina em cada mao (animacoes player.dual_reverse.*
    e o especial da espada);
  - jogador_machado: boneco do jogador com o machado da Kikoru (player.two_handed_axe.* e o especial);
  - hoshina: o Hoshina NPC (malha do Meshy, ossos do jogo, as duas espadas; parado/andando/correndo e as tecnicas).
E le de volta o .bbmodel salvo pelo Miguel (`importar`), gravando as animacoes no jogo.

Convencoes (medidas no Blockbench 5.2 e no jogo, 2026-10-09):
  - Blockbench: Y para cima, o boneco olha para +Z, braco direito em -X; rotacao do grupo = Rz(z) Ry(y) Rx(x).
  - Jogo (PAL do jogador e GeckoLib do Hoshina usam os mesmos numeros): o Blockbench mostra igual com
    (x, -y, -z) na rotacao e (x, y, -z) na posicao.
  - Arma na mao: o jogo gira a arma em volta do punho DEPOIS de deita-la (Rx(-90) Ry(180) do vanilla e, na PAL,
    Z por -Y, Y por -Z, X por -X). No Blockbench o osso right_item/left_item gira a arma do jeito comum; a conversao
    entre os dois e uma conta de matriz (item_to_bb / item_from_bb).
Os .json gerados vao para o tools/blockbench/build.js, que monta o projeto dentro do proprio Blockbench (web) e
grava o .bbmodel (assim o arquivo sai no formato exato da versao atual).
Uso:
  python3 tools/art/blockbench_templates.py gerar <pasta> [modelo...]
  python3 tools/art/blockbench_templates.py importar <arquivo.bbmodel>   (so lista; para valer, o .bbmodel vai em
      tools/blockbench/animacoes/ e os geradores gen_player_animations.py / gen_hoshina_animations.py o aplicam)
"""
import base64
import io
import json
import math
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).parent))
import preview_held_items as ph  # noqa: E402
from rig_trichonephila_mesh import read_obj  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/kn8"
COMBAT = ASSETS / "player_animations/combat.json"
HOSHINA_ANIMS = ASSETS / "animations/entity/hoshina.animation.json"
PX = 16.0
TICK = 1 / 20
SAMPLE = TICK / 4  # 4 amostras por tick: giros rapidos (180 graus em 2 ticks) ficam iguais ao Blockbench


# ------------------------------------------------------------------------------------------------ matrizes
def r3(m):
    return np.asarray(m)[:3, :3]


def euler_zyx(m):
    """Angulos (graus) com m = Rz(z) Ry(y) Rx(x)."""
    y = math.asin(max(-1.0, min(1.0, -m[2, 0])))
    x = math.atan2(m[2, 1], m[2, 2])
    z = math.atan2(m[1, 0], m[0, 0])
    return [round(math.degrees(a), 3) for a in (x, y, z)]


def rzyx(x, y, z):
    return r3(ph.Rz(math.radians(z)) @ ph.Ry(math.radians(y)) @ ph.Rx(math.radians(x)))


def limb_to_bb(v):
    return [v[0], -v[1], -v[2]]


def pos_to_bb(v):
    return [v[0], v[1], -v[2]]


# Do espaco da ModelPart do vanilla (Y para baixo, frente -Z) para o do Blockbench.
W = r3(ph.Rx(math.pi))
# Cadeia do vanilla do braco ate a arma (ItemInHandLayer), antes do giro da PAL.
B_PLAYER = r3(ph.Rx(-math.pi / 2) @ ph.Ry(math.pi))


def pal_item(k):
    """Giro da arma da PAL para os numeros k da animacao: ZP(-y), YP(-z), XP(-x)."""
    return r3(ph.Rz(math.radians(-k[1])) @ ph.Ry(math.radians(-k[2])) @ ph.Rx(math.radians(-k[0])))


def item_to_bb(k, frame):
    """Numeros da animacao do jogo -> rotacao do osso da arma no Blockbench. frame = matriz do espaco "deitado" da
    arma para o espaco do braco no Blockbench."""
    return euler_zyx(frame @ pal_item(k) @ frame.T)


def item_from_bb(b, frame):
    p = frame.T @ rzyx(*b) @ frame
    a = euler_zyx(p)  # p = Rz(a2) Ry(a1) Rx(a0) = Rz(-ky) Ry(-kz) Rx(-kx)
    return [round(-a[0], 3), round(-a[2], 3), round(-a[1], 3)]


# ------------------------------------------------------------------------------------------------ texturas
def data_url(image):
    buf = io.BytesIO()
    image.save(buf, "PNG")
    return "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()


def file_url(path):
    return data_url(Image.open(path).convert("RGBA"))


def player_skin():
    """Pele 64x64 simples (traje preto e branco, rosto) no layout do jogador, so para ver o boneco."""
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    def box(u, v, w, h, dd, front, side=None, top=None):
        side = side or front
        top = top or front
        d.rectangle([u + dd, v, u + dd + 2 * w - 1, v + dd - 1], fill=top)
        d.rectangle([u, v + dd, u + dd - 1, v + dd + h - 1], fill=side)
        d.rectangle([u + dd, v + dd, u + dd + w - 1, v + dd + h - 1], fill=front)
        d.rectangle([u + dd + w, v + dd, u + 2 * dd + w - 1, v + dd + h - 1], fill=side)
        d.rectangle([u + 2 * dd + w, v + dd, u + 2 * dd + 2 * w - 1, v + dd + h - 1], fill=front)

    skin, hair, black, white, grey = (225, 180, 140), (60, 30, 80), (25, 25, 30), (235, 235, 235), (90, 90, 100)
    box(0, 0, 8, 8, 8, skin, hair, hair)
    d.rectangle([24, 8, 31, 15], fill=hair)  # nuca
    d.rectangle([8, 8, 15, 10], fill=hair)  # franja
    d.rectangle([9, 12, 10, 12], fill=(250, 210, 40))
    d.rectangle([13, 12, 14, 12], fill=(250, 210, 40))
    box(16, 16, 8, 12, 4, black, grey, white)
    d.rectangle([23, 20, 24, 31], fill=white)  # ziper
    box(40, 16, 4, 12, 4, white, white, white)
    d.rectangle([40, 28, 55, 31], fill=black)  # luva
    box(32, 48, 4, 12, 4, white, white, white)
    d.rectangle([32, 60, 47, 63], fill=black)
    box(0, 16, 4, 12, 4, black, grey)
    d.rectangle([0, 28, 15, 31], fill=white)  # bota
    box(16, 48, 4, 12, 4, black, grey)
    d.rectangle([16, 60, 31, 63], fill=white)
    return img


# ------------------------------------------------------------------------------------------------ malhas
def mesh_entry(name, parent, texture, points, uv, tex_size):
    """points: (N, 3, 3) em px do Blockbench; uv: (N, 3, 2) em [0, 1] com v para baixo."""
    flat = np.asarray(points).reshape(-1, 3)
    faces = []
    for i in range(len(points)):
        a, b, c = 3 * i, 3 * i + 1, 3 * i + 2
        faces.append([a, b, c] + [round(float(x) * tex_size, 3) for x in np.asarray(uv[i]).reshape(-1)])
    return {"name": name, "parent": parent, "texture": texture, "vertices": np.round(flat, 3).tolist(),
            "faces": faces}


# ------------------------------------------------------------------------------------------------ jogador
PLAYER_GROUPS = [
    # nome, pivo (px, Blockbench), pai. "body" e o osso da PAL que gira o jogador inteiro em volta do quadril.
    ("body", [0, 12, 0], None),
    ("torso", [0, 24, 0], "body"),
    ("head", [0, 24, 0], "body"),
    ("right_arm", [-5, 22, 0], "body"),
    ("left_arm", [5, 22, 0], "body"),
    ("right_leg", [-1.9, 12, 0], "body"),
    ("left_leg", [1.9, 12, 0], "body"),
]
PLAYER_CUBES = [
    ("torso", [-4, 12, -2], [4, 24, 2], [16, 16]),
    ("head", [-4, 24, -4], [4, 32, 4], [0, 0]),
    ("right_arm", [-8, 12, -2], [-4, 24, 2], [40, 16]),
    ("left_arm", [4, 12, -2], [8, 24, 2], [32, 48]),
    ("right_leg", [-3.9, 0, -2], [0.1, 12, 2], [0, 16]),
    ("left_leg", [-0.1, 0, -2], [3.9, 12, 2], [16, 48]),
]


def skin_faces(lo, hi, uv):
    """UV de cada face pelo layout da pele, com o rosto para +Z (o boneco olha para o sul, como o jogo nesta
    conversao) e o lado direito do personagem em -X."""
    w, h, d = (hi[0] - lo[0], hi[1] - lo[1], hi[2] - lo[2])
    u, v = uv
    regions = {"up": (u + d, v, w, d), "down": (u + d + w, v, w, d), "west": (u, v + d, d, h),
               "south": (u + d, v + d, w, h), "east": (u + d + w, v + d, d, h), "north": (u + 2 * d + w, v + d, w, h)}
    return {face: [x, y, x + ww, y + hh] for face, (x, y, ww, hh) in regions.items()}


def player_hand(side):
    """Pivo do osso da arma: o ponto em volta do qual o jogo (PAL) gira a arma, no Blockbench (px)."""
    arm = np.array([-5.0, 22.0, 0.0]) if side == "right" else np.array([5.0, 22.0, 0.0])
    sign = 1 if side == "right" else -1
    offset = B_PLAYER @ np.array([sign / 16, 2 / 16, -10 / 16])
    return arm + PX * (W @ offset)


def player_weapon(item_name, side, tex_size):
    tris, uv, _, disp = ph.item(item_name)
    context = "thirdperson_righthand" if side == "right" else "thirdperson_lefthand"
    pivot = player_hand(side)
    m = ph.Rx(-math.pi / 2) @ ph.Ry(math.pi) @ ph.display(disp, context, left=side == "left")
    q = ph.apply(m, tris.reshape(-1, 3)) @ W.T
    points = pivot + PX * q
    return mesh_entry(f"{item_name}_{side}", f"{side}_item", item_name, points.reshape(-1, 3, 3), uv, tex_size)


def frames_of(channel):
    out = []
    for t, v in channel.items():
        vec = v["vector"] if isinstance(v, dict) else v
        out.append((float(t), [float(x) for x in vec]))
    return sorted(out)


def loop_of(anim):
    loop = anim.get("loop")
    return "loop" if loop is True else "hold" if loop == "hold_on_last_frame" else "once"


def bb_matrix(v):
    return rzyx(*v)


def bb_item_track(keys, frame, tolerance=1.0):
    """Rotacao da arma do jogo para o Blockbench (0.5.0-D7). Cada keyframe converte exato, mas o Blockbench interpola os
    numeros dele e, entre dois keyframes, mostrava outra orientacao (ate 33 graus no golpe pesado do machado). Onde a
    diferenca passa de `tolerance` graus, o trecho ganha keyframes a cada quarto de tick (so ali)."""
    def bb(k):
        return item_to_bb(k, frame)
    out = []
    for (t0, a), (t1, b) in zip(keys, keys[1:]):
        steps = max(1, round((t1 - t0) / SAMPLE))
        game = [(t0 + (t1 - t0) * i / steps, [x + (y - x) * i / steps for x, y in zip(a, b)])
                for i in range(steps + 1)]
        ends = [bb(a), bb(b)]
        if out:
            ends[0] = out[-1][1]
        ends[1] = nearest_euler(ends[1], ends[0], bb_matrix)
        worst = 0.0
        for i, (_, g) in enumerate(game):
            f = i / steps
            mixed = rzyx(*[x + (y - x) * f for x, y in zip(*ends)])
            worst = max(worst, math.degrees(math.acos(max(-1.0, min(1.0, (np.trace(mixed.T @ rzyx(*bb(g))) - 1) / 2)))))
        samples = game if worst > tolerance else [game[0], game[-1]]
        for t, g in samples:
            v = bb(g)
            v = nearest_euler(v, out[-1][1], bb_matrix) if out else v
            if not out or round(t, 4) != out[-1][0]:
                out.append((round(t, 4), v))
    return out or [(round(t, 4), bb(v)) for t, v in keys]


def convert_channels(bones, rotation, position, item_frame=None):
    out = {}
    for bone, channels in bones.items():
        for channel, frames in channels.items():
            frame = item_frame(bone) if item_frame else None
            if channel == "rotation" and frame is not None:
                out.setdefault(bone, {})[channel] = [[t] + v for t, v in bb_item_track(frames_of(frames), frame)]
                continue
            conv = rotation(bone) if channel == "rotation" else position
            out.setdefault(bone, {})[channel] = [[t] + conv(v) for t, v in frames_of(frames)]
    return out


PLAYER_FRAME = W @ B_PLAYER


WEAPONS = ROOT / "src/main/resources/data/kn8/kn8/weapon"
PROFILES_DIR = ROOT / "src/main/resources/data/kn8/kn8/special_soldier"


def weapon_json(item):
    return json.loads((WEAPONS / f"{item}.json").read_text(encoding="utf-8"))


def player_markers(anim_name, item):
    """Instante do dano (s) de cada golpe do jogador: o servidor aplica o golpe nesse tick, a animacao tem que
    acertar ali (regra 6)."""
    weapon = weapon_json(item)
    action = anim_name.rsplit(".", 1)[1]
    if action.startswith("light"):
        return [weapon["actions"]["light"]["impact_tick"] / 20]
    if action == "heavy":
        return [weapon["actions"]["heavy"]["impact_tick"] / 20]
    if action == "special" and "special" in weapon:
        return [weapon["special"]["impact_tick"] / 20]
    return []


def hoshina_markers(anim_name):
    profile = json.loads((PROFILES_DIR / "hoshina.json").read_text(encoding="utf-8"))
    technique = profile["techniques"].get(anim_name.rsplit(".", 1)[1])
    if technique:
        step = technique.get("hit_interval", 2)
        return [(technique["windup_ticks"] + i * step) / 20 for i in range(len(technique["hits"]))]
    if anim_name.endswith("kaeshi_uchi"):
        return [profile["counter"]["strike_delay_ticks"] / 20]
    return []


def pal_to_bb(anim_name, anim):
    def rotation(bone):
        return (lambda v: item_to_bb(v, PLAYER_FRAME)) if bone.endswith("_item") else limb_to_bb
    return {"name": anim_name, "length": anim["animation_length"], "loop": loop_of(anim),
            "bones": convert_channels(anim["bones"], rotation, pos_to_bb,
                                      lambda bone: PLAYER_FRAME if bone.endswith("_item") else None)}


def player_template(name, weapons, prefixes, extra):
    """weapons: {"right": item, "left": item ou None}; prefixes/extra: animacoes do combat.json a incluir."""
    tex_size = 64
    groups = [{"name": n, "origin": o, "parent": p} for n, o, p in PLAYER_GROUPS]
    cubes = [{"name": n + "_cubo", "parent": n, "from": f, "to": t, "faces": skin_faces(f, t, uv), "texture": "pele"}
             for n, f, t, uv in PLAYER_CUBES]
    textures = [{"name": "pele", "png": data_url(player_skin())}]
    meshes = []
    for side, item in weapons.items():
        pivot = player_hand(side)
        groups.append({"name": f"{side}_item", "origin": [round(float(c), 4) for c in pivot],
                       "parent": f"{side}_arm"})
        if item:
            meshes.append(player_weapon(item, side, tex_size))
            if not any(t["name"] == item for t in textures):
                textures.append({"name": item, "png": file_url(ASSETS / f"textures/item/{item}.png")})
    data = json.loads(COMBAT.read_text(encoding="utf-8"))["animations"]
    animations = [pal_to_bb(n, a) for n, a in data.items() if n.startswith(prefixes) or n in extra]
    for anim in animations:
        anim["markers"] = player_markers(anim["name"], weapons["right"])
    return {"format": "free", "name": name, "texture_width": tex_size, "texture_height": tex_size,
            "textures": textures, "groups": groups, "cubes": cubes, "meshes": meshes, "animations": animations}


# ------------------------------------------------------------------------------------------------ Hoshina
Y180 = r3(ph.Ry(math.pi))


def hoshina_bones(species="hoshina"):
    geo = json.loads((ASSETS / f"geo/entity/{species}.geo.json").read_text(encoding="utf-8"))
    return {b["name"]: b for b in geo["minecraft:geometry"][0]["bones"]}


def gecko_internal(pivot):
    """Pivo do .geo.json -> espaco em que a GeckoLib desenha (o X e invertido ao carregar), em blocos."""
    return np.array([-pivot[0], pivot[1], pivot[2]]) / PX


def to_bb_from_internal(points):
    """Espaco interno da GeckoLib (blocos) -> Blockbench (px): giro de 180 graus em Y."""
    return PX * (np.asarray(points) @ Y180.T)


def hoshina_hand(bones, item_bone):
    """Mesma cadeia do SoldierRenderer: alinha ao braco (ombro -> mao) e desce 1 px / avanca 2 px."""
    arm = gecko_internal(bones[bones[item_bone]["parent"]]["pivot"])
    hand = gecko_internal(bones[item_bone]["pivot"])
    rto = r3(ph.rotation_to((0, -1, 0), hand - arm))
    center = hand + rto @ np.array([0, -1 / 16, -2 / 16])
    return rto, center


def hoshina_weapon(bones, item_bone, item_name, tex_size):
    tris, uv, _, disp = ph.item(item_name)
    rto, center = hoshina_hand(bones, item_bone)
    scale = disp["thirdperson_righthand"]["scale"][0]
    s = min(0.85, scale) / scale
    m = ph.Rx(-math.pi / 2) @ ph.S(s, s, s) @ ph.display(disp, "thirdperson_righthand")
    q = center + ph.apply(m, tris.reshape(-1, 3)) @ rto.T
    return mesh_entry(f"{item_name}_{item_bone}", item_bone, item_name,
                      to_bb_from_internal(q).reshape(-1, 3, 3), uv, tex_size)


def hoshina_frames(bones):
    return {item_bone: Y180 @ hoshina_hand(bones, item_bone)[0] @ r3(ph.Rx(-math.pi / 2))
            for item_bone in ("item_right", "item_left")}


# Animacoes de corpo inteiro do Hoshina: no jogo sao dois controllers (corpo + bracos); no Blockbench uma so.
HOSHINA_COMBINED = {"hoshina.parado": ("hoshina.movement.idle", "hoshina.arms.blade_ready"),
                    "hoshina.andando": ("hoshina.movement.walk", "hoshina.arms.blade_walk"),
                    "hoshina.correndo": ("hoshina.movement.run", "hoshina.arms.blade_run")}


def gecko_to_bb(anim_name, bones_json, frames, length, loop):
    def rotation(bone):
        return (lambda v: item_to_bb(v, frames[bone])) if bone.startswith("item_") else limb_to_bb
    return {"name": anim_name, "length": length, "loop": loop,
            "bones": convert_channels(bones_json, rotation, pos_to_bb,
                                      lambda bone: frames[bone] if bone.startswith("item_") else None)}


def hilt_center(vertices, pivot):
    """Centro do cabo da espada (parte com aneis, antes da guarda: a fatia mais larga perto do pivo) na malha."""
    center = vertices.mean(0)
    axes = np.linalg.svd(vertices - center)[2]
    along = (vertices - pivot) @ axes[0]
    if along.max() < -along.min():
        along = -along
    edges = np.linspace(along.min(), along.max(), 40)
    widths = [np.ptp((vertices[(along >= a) & (along < b)] - pivot) @ axes[1])
              if ((along >= a) & (along < b)).sum() > 2 else 0 for a, b in zip(edges, edges[1:])]
    guard = edges[int(np.argmax(widths[:25]))]
    return vertices[along < guard].mean(0)


# Miguel (2026-10-09): espada "um pouco mais pra cima e pra frente dos bracos". Deslocamento da pegada no espaco do
# braco em repouso (px, Blockbench: +Y para cima, +Z para a frente do Hoshina).
HOSHINA_GRIP_SHIFT = np.array([0.0, 1.5, 1.5])
# E "mais uma aumentada para cima para que ele pegue mais no cabo" (e "mais ainda"): a espada sobe ao longo dela (px, para o lado
# da ponta), entao a mao fica mais perto do pomo e longe da guarda.
HOSHINA_GRIP_ALONG = 3.0


def hoshina_grip_points(species="hoshina"):
    """0.5.0-D7 (Miguel: "pega na lamina"): para cada espada do Hoshina, o punho (fim da malha do braco, ultimos 3 px)
    e o centro do cabo, no espaco do Blockbench em relacao ao pivo do osso da espada."""
    bones = hoshina_bones(species)
    index = json.loads((ASSETS / f"meshes/{species}.json").read_text(encoding="utf-8"))["bones"]
    out = {}
    for side in ("right", "left"):
        item_bone, arm_bone = f"item_{side}", f"arm_{side}"
        pivot = np.array([bones[item_bone]["pivot"][0], bones[item_bone]["pivot"][1], -bones[item_bone]["pivot"][2]])
        shoulder = np.array([bones[arm_bone]["pivot"][0], bones[arm_bone]["pivot"][1], -bones[arm_bone]["pivot"][2]])
        arm = to_bb_from_internal(read_obj(ASSETS / index[arm_bone].split(":")[1])[0])
        direction = (pivot - shoulder) / np.linalg.norm(pivot - shoulder)
        along = (arm - shoulder) @ direction
        fist = arm[along > along.max() - 3].mean(0)
        sword = np.array(hoshina_weapon(bones, item_bone, "hoshina_sword", 1024)["vertices"], float)
        hilt = hilt_center(sword, pivot)
        tip = sword[np.argmax(np.linalg.norm(sword - hilt, axis=1))]
        toward_tip = (tip - hilt) / np.linalg.norm(tip - hilt)
        out[item_bone] = (fist - pivot + HOSHINA_GRIP_SHIFT, hilt - HOSHINA_GRIP_ALONG * toward_tip - pivot)
    return out


def grip_gecko(anim, frames, points):
    """Posicao da espada do Hoshina refeita pela rotacao (cada keyframe de rotacao, ja amostrado a cada quarto de
    tick): o centro do cabo fica no punho. Posicoes postas a mao compensavam a geometria e deixavam a mao na lamina."""
    for bone, (fist, hilt) in points.items():
        channels = anim["bones"].get(bone)
        if not channels or "rotation" not in channels:
            continue
        positions = {}
        for t, k in channels["rotation"].items():
            rotation = rzyx(*item_to_bb(k, frames[bone]))
            positions[t] = [round(float(x), 3) for x in pos_to_bb(list(fist - rotation @ hilt))]
        channels["position"] = positions
    return anim


def hoshina_template():
    tex_size = 1024
    bones = hoshina_bones()
    groups = [{"name": n, "origin": [b["pivot"][0], b["pivot"][1], -b["pivot"][2]], "parent": b.get("parent")}
              for n, b in bones.items()]
    index = json.loads((ASSETS / "meshes/hoshina.json").read_text(encoding="utf-8"))["bones"]
    meshes = []
    for bone, location in index.items():
        v, uv, _, f = read_obj(ASSETS / location.split(":")[1])
        tuv = uv[f].copy()
        tuv[..., 1] = 1 - tuv[..., 1]
        meshes.append(mesh_entry(bone, bone, "hoshina", to_bb_from_internal(v)[f], tuv, tex_size))
    for item_bone in ("item_right", "item_left"):
        meshes.append(hoshina_weapon(bones, item_bone, "hoshina_sword", tex_size))
    textures = [{"name": "hoshina", "png": file_url(ASSETS / "textures/entity/hoshina.png")},
                {"name": "hoshina_sword", "png": file_url(ASSETS / "textures/item/hoshina_sword.png")}]
    frames = hoshina_frames(bones)
    data = json.loads(HOSHINA_ANIMS.read_text(encoding="utf-8"))["animations"]
    animations = []
    for name, parts in HOSHINA_COMBINED.items():
        merged = {}
        for part in parts:
            merged.update(data[part]["bones"])
        length = max(data[part]["animation_length"] for part in parts)
        animations.append(gecko_to_bb(name, merged, frames, length, "loop"))
    for name, anim in data.items():
        if name.startswith("hoshina.action.") and "rifle" not in name and "pistol" not in name:
            # 0.5.0-D7: no jogo o controller "arms" continua rodando nas tecnicas e as espadas ficam na rotacao e na
            # posicao da postura; sem isso o Blockbench mostrava a espada na rotacao zero (fora da mao). O importador
            # nao leva para o jogo canais de espada iguais aos da postura.
            stance = {bone: {ch: {"0": next(iter(keys.values()))} for ch, keys in channels.items()}
                      for bone, channels in data["hoshina.arms.blade_ready"]["bones"].items()
                      if bone.startswith("item_")}
            bones_json = dict(stance, **anim["bones"])
            animations.append(gecko_to_bb(name, bones_json, frames, anim["animation_length"], loop_of(anim)))
    for anim in animations:
        anim["markers"] = hoshina_markers(anim["name"])
    groups = [{"name": g["name"], "origin": [round(float(c), 4) for c in g["origin"]], "parent": g["parent"]}
              for g in groups]
    return {"format": "free", "name": "hoshina", "texture_width": tex_size, "texture_height": tex_size,
            "textures": textures, "groups": groups, "meshes": meshes, "animations": animations}


TEMPLATES = {
    "jogador_espadas_duplas": lambda: player_template(
        "jogador_espadas_duplas", {"right": "hoshina_sword", "left": "hoshina_sword"},
        ("player.dual_reverse.",), ("player.hoshina_sword.special",)),
    "jogador_machado": lambda: player_template(
        "jogador_machado", {"right": "axe", "left": None},
        ("player.two_handed_axe.",), ("player.axe.special",)),
    "hoshina": hoshina_template,
}


# ------------------------------------------------------------------------------------------------ importar
IMPORTED = ROOT / "tools/blockbench/animacoes"


def read_bbmodel(path):
    """Animacoes de um .bbmodel: {nome: {"length", "loop", "bones": {osso: {canal: [(t, [x, y, z])]}}}}."""
    data = json.loads(Path(path).read_text(encoding="utf-8"))
    groups = {g["name"] for g in data.get("groups", [])}
    out = {}
    for anim in data.get("animations", []):
        bones = {}
        for animator in anim.get("animators", {}).values():
            if animator.get("type", "bone") != "bone":
                continue
            for key in animator.get("keyframes", []):
                if key["channel"] not in ("rotation", "position", "scale"):
                    continue
                point = key["data_points"][0]
                rest = 1.0 if key["channel"] == "scale" else 0.0
                vec = [float(point.get(axis, rest) if point.get(axis, "") != "" else rest) for axis in "xyz"]
                bones.setdefault(animator["name"], {}).setdefault(key["channel"], []).append(
                    (round(float(key["time"]), 4), vec))
        for channels in bones.values():
            for channel in channels:
                channels[channel].sort()
        out[anim["name"]] = {"length": float(anim.get("length") or 0), "loop": anim.get("loop", "once"),
                             "bones": bones}
    return groups, out


def game_loop(loop):
    return True if loop == "loop" else "hold_on_last_frame" if loop == "hold" else None




def game_item_matrix(k):
    """Orientacao da arma para os numeros k do jogo (a mesma ordem da PAL e do SoldierRenderer)."""
    return pal_item(k)


def nearest_euler(v, prev, matrix=None):
    """Entre os numeros equivalentes a v (a outra solucao de Euler, o caso de gimbal e +-360 por eixo), os mais perto
    de prev: a interpolacao linear do jogo vai pelo caminho curto em vez de dar a volta. Cada candidato so vale se der
    a mesma orientacao de v (conferido pela matriz)."""
    matrix = matrix or game_item_matrix
    target = matrix(v)
    x, y, z = v
    # A outra solucao de Euler (eixo do meio z nos numeros do jogo, y no Blockbench) e, no gimbal, x fixo no anterior
    # com o outro eixo compensando. So vale o candidato que der a mesma orientacao.
    candidates = [[x, y, z], [x + 180, y + 180, 180 - z], [x + 180, 180 - y, z + 180]]
    for sign in (1, -1):
        candidates.append([prev[0], y + sign * (prev[0] - x), z])
        candidates.append([prev[0], y, z + sign * (prev[0] - x)])
    best = None
    for option in candidates:
        if np.abs(matrix(option) - target).max() > 1e-4:
            continue
        cand = []
        for a, p in zip(option, prev):
            while a - p > 180:
                a -= 360
            while a - p < -180:
                a += 360
            cand.append(round(a, 3))
        dist = sum(abs(a - p) for a, p in zip(cand, prev))
        if best is None or dist < best[0]:
            best = (dist, cand)
    return best[1]


def limb_matrix(v):
    """Orientacao de um membro (jogador na PAL ou osso da GeckoLib) a partir dos numeros do jogo."""
    return rzyx(*limb_to_bb(v))


def _angle(a, b):
    return math.degrees(math.acos(max(-1.0, min(1.0, (np.trace(a.T @ b) - 1) / 2))))


def slerp_track(keys, matrix=limb_matrix, tolerance=4.0):
    """0.5.0-D7 (Miguel: "animacoes muito bugadas"): o jogo interpola os angulos de Euler em linha reta e, entre duas
    poses muito diferentes (ou com numeros equivalentes longe um do outro, como 169 e -115), o membro passava por
    orientacoes que nao estao em nenhuma das duas: a perna ou o braco davam um rodopio. Cada keyframe vira os numeros
    equivalentes mais perto do anterior e, onde a reta ainda se afasta do caminho curto (slerp) mais que a
    tolerancia, entram amostras do caminho curto a cada quarto de tick."""
    from scipy.spatial.transform import Rotation, Slerp
    out = [(keys[0][0], list(keys[0][1]))]
    for t1, b in keys[1:]:
        t0, a = out[-1]
        b = nearest_euler(b, a, matrix)
        ma, mb = matrix(a), matrix(b)
        steps = max(1, round((t1 - t0) / SAMPLE)) if t1 > t0 else 1
        curve = Slerp([0, 1], Rotation.from_matrix([ma, mb]))
        straight = all(_angle(matrix([x + (y - x) * f for x, y in zip(a, b)]), curve(f).as_matrix()) <= tolerance
                       for f in (0.25, 0.5, 0.75))
        if not straight:
            for i in range(1, steps):
                f = i / steps
                v = limb_to_bb(euler_zyx(curve(f).as_matrix()))
                out.append((round(t0 + (t1 - t0) * f, 4), nearest_euler(v, out[-1][1], matrix)))
        out.append((t1, nearest_euler(b, out[-1][1], matrix)))
    # Passando perto do gimbal, a mesma pose final pode sair com outros numeros. A transicao para a postura mistura os
    # numeros (PAL e GeckoLib): o fim volta aos numeros originais no comeco da pose parada final, num instante.
    final = list(keys[-1][1])
    if np.abs(np.array(out[-1][1]) - final).max() > 1e-3:
        target = matrix(final)
        j = len(out) - 1
        while j > 0 and np.abs(matrix(out[j - 1][1]) - target).max() < 1e-4:
            j -= 1
        hold = out[j][0]
        out = out[:j + 1] + [(round(hold + 0.0002, 4), final)] + [(t, final) for t, _ in out[j + 1:]
                                                                   if t > hold + 0.0002]
    return out


def item_track(keys, frame):
    """Rotacao da espada do Blockbench para o jogo (0.5.0-D7). A conversao entre os dois espacos nao e linear: um
    keyframe so converte certo no proprio instante e, entre dois keyframes, o jogo (que interpola os numeros dele)
    passava por outras orientacoes (ate 180 graus de diferenca: a espada atravessava o corpo). Por isso a curva do
    Blockbench e amostrada 4 vezes por tick, cada amostra e convertida e escolhida a solucao mais perto da anterior."""
    out = []
    for (t0, a), (t1, b) in zip(keys, keys[1:] + [keys[-1]]):
        steps = max(1, round((t1 - t0) / SAMPLE)) if t1 > t0 and a != b else 1
        for i in range(steps if t1 > t0 else 1):
            f = i / steps
            t = round(t0 + (t1 - t0) * f, 4)
            v = item_from_bb([x + (y - x) * f for x, y in zip(a, b)], frame)
            out.append((t, nearest_euler(v, out[-1][1]) if out else v))
    last_t, last = keys[-1]
    if out[-1][0] != round(last_t, 4):
        v = item_from_bb(last, frame)
        out.append((round(last_t, 4), nearest_euler(v, out[-1][1])))
    return out


def bb_to_pal(anim):
    bones = {}
    for bone, channels in anim["bones"].items():
        for channel, frames in channels.items():
            if channel == "position":
                conv = pos_to_bb  # a conversao e a propria inversa
            elif channel == "scale":
                conv = list  # escala igual nos dois (0.5.0-D7: pernas encolhidas do Hoshina parado)
            elif bone.endswith("_item"):
                bones.setdefault(bone, {})[channel] = {f"{t}": {"vector": v}
                                                       for t, v in item_track(frames, PLAYER_FRAME)}
                continue
            else:
                conv = limb_to_bb
            bones.setdefault(bone, {})[channel] = {f"{t}": {"vector": conv(v)} for t, v in frames}
    out = {"animation_length": anim["length"], "bones": bones}
    loop = game_loop(anim["loop"])
    return {"loop": loop, **out} if loop is not None else out


def bb_to_gecko(anim, frames):
    bones = {}
    for bone, channels in anim["bones"].items():
        for channel, keys in channels.items():
            if channel == "position":
                conv = pos_to_bb
            elif channel == "scale":
                conv = list
            elif bone.startswith("item_"):
                bones.setdefault(bone, {})[channel] = {f"{t}": v for t, v in item_track(keys, frames[bone])}
                continue
            else:
                conv = limb_to_bb
            bones.setdefault(bone, {})[channel] = {f"{t}": conv(v) for t, v in keys}
    out = {"animation_length": anim["length"], "bones": bones}
    loop = game_loop(anim["loop"])
    return {"loop": loop, **out} if loop is not None else out


def split_hoshina(anim):
    """Corpo inteiro do Blockbench -> movement (corpo/pernas/cabeca/raiz) + arms (bracos e espadas)."""
    def part(keep):
        return dict(anim, bones={b: c for b, c in anim["bones"].items() if keep(b)})
    return part(lambda b: not b.startswith(("arm", "item"))), part(lambda b: b.startswith(("arm", "item")))


def imported_player():
    """Animacoes do jogador vindas dos .bbmodel em tools/blockbench/animacoes (usado pelo gen_player_animations)."""
    out = {}
    for path in sorted(IMPORTED.glob("*.bbmodel")):
        groups, anims = read_bbmodel(path)
        if "right_item" not in groups:
            continue
        for name, anim in anims.items():
            if name.startswith("player."):
                out[name] = bb_to_pal(anim)
    return out


def imported_hoshina(species="hoshina"):
    """Animacoes do Hoshina vindas dos .bbmodel (usado pelo gen_hoshina_animations; nomes hoshina.* trocados pelo
    prefixo da especie)."""
    out = {}
    frames = None
    for path in sorted(IMPORTED.glob("*.bbmodel")):
        groups, anims = read_bbmodel(path)
        if "item_right" not in groups:
            continue
        # 0.5.0-D7: o traje numerado 10 tem outro rig (ombros e maos em outro lugar): a rotacao da espada e o ponto
        # da pegada usam o rig da propria especie.
        frames = frames or hoshina_frames(hoshina_bones(species))
        stance = {(bone, channel): keys[0][1] for bone, channels in anims.get("hoshina.parado", {}).get(
            "bones", {}).items() if bone.startswith("item") for channel, keys in channels.items()}
        for name, anim in anims.items():
            if not name.startswith("hoshina."):
                continue
            if name not in HOSHINA_COMBINED:
                # Tecnica: canal de espada parado no valor da postura so mostra no Blockbench o que o jogo ja faz (o
                # controller "arms" continua rodando); nao vai para o jogo, senao andando/correndo a espada pularia
                # para a pose de parado no comeco da tecnica.
                anim = dict(anim, bones={bone: {ch: keys for ch, keys in channels.items()
                                                if not (bone.startswith("item") and keys
                                                        and all(v == stance.get((bone, ch)) for _, v in keys))}
                                         for bone, channels in anim["bones"].items()})
            if name in HOSHINA_COMBINED:
                movement, arms = split_hoshina(anim)
                out[HOSHINA_COMBINED[name][0]] = bb_to_gecko(movement, frames)
                arms = bb_to_gecko(arms, frames)
                grip_gecko(arms, frames, hoshina_grip_points(species))
                out[HOSHINA_COMBINED[name][1]] = arms
            else:
                out[name] = bb_to_gecko(anim, frames)
    return {species + name[len("hoshina"):]: anim for name, anim in out.items()}


def main():
    if len(sys.argv) < 3:
        print(__doc__)
        return
    if sys.argv[1] == "gerar":
        out = Path(sys.argv[2])
        out.mkdir(parents=True, exist_ok=True)
        for name in sys.argv[3:] or list(TEMPLATES):
            spec = TEMPLATES[name]()
            spec["out"] = f"{name}.bbmodel"
            (out / f"{name}.json").write_text(json.dumps(spec), encoding="utf-8")
            print(name, len(spec["animations"]), "animacoes")
    elif sys.argv[1] == "importar":
        groups, anims = read_bbmodel(sys.argv[2])
        print("ossos:", sorted(groups))
        for name, anim in anims.items():
            print(f"{name}: {anim['length']} s, {anim['loop']}, ossos {sorted(anim['bones'])}")


if __name__ == "__main__":
    main()
