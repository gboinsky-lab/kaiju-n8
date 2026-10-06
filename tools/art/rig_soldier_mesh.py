#!/usr/bin/env python3
"""Rigging do Soldado 1 (malha do Meshy presa aos ossos da GeckoLib), 0.1-B / Etapa F.

Entrada: tools/art/converted/soldier_1/soldier_1.obj + .png (meshy_convert.py, modelo inteiro). Em metros, pes em
Y = 0, frente -Z. A divisao em partes e feita aqui pela forma (split), sem a segmentacao do Meshy.
Saida (especie "soldier"):
  - assets/kn8/meshes/soldier.json + meshes/soldier/<osso>.obj (head, body, arm_left/right, leg_left/right);
  - assets/kn8/textures/entity/soldier.png (512, com borda nas ilhas de UV: tools/art/pad_texture.py);
  - assets/kn8/geo/entity/soldier.geo.json (so ossos; pivos no pescoco, ombros, quadris; item_right na mao direita);
  - assets/kn8/animations/entity/soldier.animation.json (pernas, poses de arma por classe, ataque, tiro, dano).
A GeckoLib inverte o X do pivo ao carregar: pivos gravados com X negado. "left" = lado de X negativo (mesma
convencao em que o meshy_convert nomeou os bracos).
Uso: python3 tools/art/rig_soldier_mesh.py
"""
import json
import shutil
import sys
from pathlib import Path

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).parent))
from rig_trichonephila_mesh import read_obj, write_obj  # noqa: E402
from kaiju_art import kf  # noqa: E402
from pad_texture import DILATE_STEPS, dilate, uv_mask  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/kn8"
SOURCE = ROOT / "tools/art/converted/soldier_1"
NAME = "soldier"
PX = 16.0
# 1024 (0.2, modelo novo de 7 mil triangulos): em 512 as placas brancas pegavam o preto das ilhas vizinhas.
TEXTURE_SIZE = 1024
TOP_SLICE = 0.08
# Faixa do punho (m acima da ponta dos dedos) usada para o osso item_right.
HAND_BAND = (0.06, 0.11)
# Duracao do passo (s); a GeckoLib toca a animacao no tempo real, sem acompanhar a velocidade.
WALK_LENGTH = 0.9
# Cortes da malha (m), medidos no perfil do modelo: quadril, pescoco, ponta dos dedos e borda de dentro dos bracos.
HIP_Y = 0.93
NECK_Y = 1.52
HAND_MIN_Y = 0.68
# Borda de dentro do braco: (altura acima da qual vale, |x|). Na altura das maos as placas das coxas chegam a
# |x| 0,255, entao abaixo da cintura o braco so comeca depois delas.
ARM_INNER_X = [(1.02, 0.212), (0.0, 0.258)]
# Pedaco isolado com ate tantas faces vai para o osso vizinho (absorb_fragments).
FRAGMENT_FACES = 40
# Brilho maximo do "preto do macacao" usado para tampar buracos (cap_holes).
DARK_LEVEL = 30


def load_mesh():
    return read_obj(SOURCE / "soldier_1.obj")


def split(mesh):
    """Divide a malha inteira pela forma (modelo "estilo Minecraft" de 2026-10-06, segunda versao com 7 mil
    triangulos: bracos retos colados no tronco, borda em |x| ~0,21 m). Bracos: fora do tronco e acima da ponta dos dedos; cabeca: acima do pescoco;
    corpo: acima do quadril; abaixo, perna do lado (X negativo = "left", convencao do meshy_convert)."""
    vertices, uvs, normals, faces = mesh
    c = vertices[faces].mean(axis=1)
    inner = np.select([c[:, 1] >= y for y, _ in ARM_INNER_X], [x for _, x in ARM_INNER_X])
    arm = (c[:, 1] >= HAND_MIN_Y) & (np.abs(c[:, 0]) > inner)
    head = ~arm & (c[:, 1] > NECK_Y)
    body = ~arm & ~head & (c[:, 1] > HIP_Y)
    leg = ~arm & ~head & ~body
    groups = {"head": head, "body": body,
              "arm_left": arm & (c[:, 0] < 0), "arm_right": arm & (c[:, 0] > 0),
              "leg_left": leg & (c[:, 0] <= 0), "leg_right": leg & (c[:, 0] > 0)}
    labels = np.empty(len(faces), dtype=object)
    for name, sel in groups.items():
        labels[sel] = name
    labels = absorb_fragments(vertices, faces, labels)
    vertices, uvs, normals, faces, labels = cap_holes(vertices, uvs, normals, faces, labels,
                                                      dark_uv(vertices, uvs, faces, labels))
    return {name: (vertices, uvs, normals, faces[labels == name]) for name in groups}


def dark_uv(vertices, uvs, faces, labels):
    """Coordenada de textura de um ponto preto do macacao (centro de uma face escura do tronco)."""
    image = np.asarray(Image.open(SOURCE / "soldier_1.png").convert("RGB")).astype(float)
    height, width = image.shape[:2]
    centers = uvs[faces].mean(axis=1)
    rows = np.clip(((1 - centers[:, 1]) * height).astype(int), 0, height - 1)
    cols = np.clip((centers[:, 0] * width).astype(int), 0, width - 1)
    brightness = image[rows, cols].mean(axis=1)
    candidates = np.where((labels == "body") & (brightness < DARK_LEVEL))[0]
    return centers[candidates[np.argmin(brightness[candidates])]]


def boundary_loops(vertices, faces):
    """Contornos abertos (arestas usadas por uma face so) de um pedaco de malha, em ordem, com os indices originais."""
    _, welded = np.unique(np.round(vertices, 5), axis=0, return_inverse=True)
    welded = welded.reshape(-1)
    count = {}
    for a, b, c in welded[faces]:
        for edge in ((a, b), (b, c), (c, a)):
            key = (min(edge), max(edge))
            count[key] = count.get(key, 0) + 1
    neighbors = {}
    for (a, b), uses in count.items():
        if uses == 1:
            neighbors.setdefault(a, []).append(b)
            neighbors.setdefault(b, []).append(a)
    representative = {}
    for index, key in enumerate(welded):
        representative.setdefault(key, index)
    loops, seen = [], set()
    for start in neighbors:
        if start in seen:
            continue
        loop, current, previous = [], start, None
        while current not in seen:
            seen.add(current)
            loop.append(representative[current])
            options = [n for n in neighbors[current] if n != previous and n not in seen]
            if not options:
                break
            previous, current = current, options[0]
        if len(loop) >= 3:
            loops.append(loop)
    return loops


def cap_holes(vertices, uvs, normals, faces, labels, fill_uv=None):
    """O Meshy deixa aberta a lateral do tronco/quadril onde o braco encosta (e o lado de dentro do braco): com o
    braco levantado aparecia um buraco atravessando o quadril. Fecha cada contorno aberto de cada osso com um leque de
    triangulos (nos dois sentidos) para nunca ver o "oco" da malha. Cor: fill_uv (o preto do macacao, no soldado) ou,
    sem ele, a da propria borda (kaiju: a pele em volta do corte da junta)."""
    new_vertices, new_uvs, new_faces, new_labels = [], [], [], []
    base = len(vertices)
    for name in np.unique(labels):
        part = faces[labels == name]
        for loop in boundary_loops(vertices, part):
            ring = vertices[loop]
            center_index = base + len(new_vertices)
            new_vertices.append(ring.mean(axis=0))
            first = center_index + 1
            new_vertices.extend(ring)
            new_uvs.extend([fill_uv if fill_uv is not None else uvs[loop[0]]] * (len(loop) + 1))
            for i in range(len(loop)):
                a, b = first + i, first + (i + 1) % len(loop)
                new_faces.extend([(center_index, a, b), (center_index, b, a)])
                new_labels.extend([name, name])
    if not new_vertices:
        return vertices, uvs, normals, faces, labels
    added = np.array(new_vertices)
    vertices = np.vstack([vertices, added])
    uvs = np.vstack([uvs, np.array(new_uvs)])
    normals = np.vstack([normals, np.tile([0.0, 1.0, 0.0], (len(added), 1))])
    return vertices, uvs, normals, np.vstack([faces, np.array(new_faces)]), \
        np.concatenate([labels, np.array(new_labels, dtype=object)])


def absorb_fragments(vertices, faces, labels, max_faces=FRAGMENT_FACES):
    """Pedaco pequeno e isolado de um osso (ponta de dedo da luva do lado da coxa, lasca de placa) vai para o osso
    vizinho com quem divide mais arestas: sem isso ele fica parado quando o braco levanta (0.2, soldado novo)."""
    from scipy.sparse import coo_matrix
    from scipy.sparse.csgraph import connected_components
    _, welded = np.unique(np.round(vertices, 5), axis=0, return_inverse=True)
    wf = welded.reshape(-1)[faces]
    # Faces vizinhas = dividem uma aresta (par de vertices soldados).
    edges = {}
    for index, (a, b, c) in enumerate(wf):
        for edge in ((a, b), (b, c), (c, a)):
            edges.setdefault((min(edge), max(edge)), []).append(index)
    pairs = [(f[i], f[j]) for f in edges.values() for i in range(len(f)) for j in range(i + 1, len(f))]
    pairs = np.array(pairs) if pairs else np.zeros((0, 2), dtype=int)
    for _ in range(3):
        same = labels[pairs[:, 0]] == labels[pairs[:, 1]]
        graph = coo_matrix((np.ones(same.sum()), (pairs[same, 0], pairs[same, 1])),
                           shape=(len(faces), len(faces)))
        _, component = connected_components(graph, directed=False)
        sizes = np.bincount(component)
        changed = False
        for comp in np.where(sizes <= max_faces)[0]:
            members = set(np.where(component == comp)[0])
            votes = {}
            for a, b in pairs:
                if (a in members) != (b in members):
                    other = b if a in members else a
                    votes[labels[other]] = votes.get(labels[other], 0) + 1
            if votes:
                labels[list(members)] = max(votes, key=votes.get)
                changed = True
        if not changed:
            break
    return labels


def top_point(mesh, fraction=TOP_SLICE):
    vertices, _, _, faces = mesh
    used = vertices[np.unique(faces)]
    top = used[:, 1].max()
    span = top - used[:, 1].min()
    band = used[used[:, 1] > top - span * fraction]
    return np.array([band[:, 0].mean(), top - span * fraction * 0.5, band[:, 2].mean()])


def bottom_center(mesh):
    vertices, _, _, faces = mesh
    used = vertices[np.unique(faces)]
    return used.mean(axis=0)


def geo_pivot(point):
    return [round(-point[0] * PX, 3), round(point[1] * PX, 3), round(point[2] * PX, 3)]


def hand_point(mesh):
    """Centro do punho (onde a arma e segurada): faixa logo acima da ponta dos dedos."""
    vertices, _, _, faces = mesh
    used = vertices[np.unique(faces)]
    low = used[:, 1].min()
    band = used[(used[:, 1] > low + HAND_BAND[0]) & (used[:, 1] < low + HAND_BAND[1])]
    return band.mean(axis=0)


# Poses dos bracos (graus, convencao do Blockbench/GeckoLib): X negativo = levanta para a frente. Y: braco direito
# negativo / esquerdo positivo = para dentro (na frente do corpo). Z: direito negativo / esquerdo positivo = para
# dentro. Os bracos da malha abrem ~9 graus (mao para fora do ombro), por isso o Y das poses de arma e um pouco
# maior que o do vanilla.
# [SUPOSICAO] sinais de Y/Z conferidos so no simulador (tools/art/preview_soldier.py), nao em jogo.
POSES = {
    #             braco direito        braco esquerdo
    "unarmed": ([0, 0, -2], [0, 0, 2]),
    "blade": ([-28, -4, -2], [0, 0, 2]),
    "rifle": ([-48, -28, 0], [-62, 40, 0]),
    "pistol": ([-38, -18, 0], [-34, 36, 0]),
}
AIM = {
    "unarmed": ([-55, -22, 0], [-55, 22, 0]),
    "blade": ([-45, -8, 0], [-30, 14, 0]),
    "rifle": ([-88, -26, 0], [-92, 42, 0]),
    "pistol": ([-88, -27, 0], [-88, 27, 0]),
}
RECOIL = {"rifle": 8, "pistol": 16}


def add(a, b):
    return [round(x + y, 2) for x, y in zip(a, b)]


def arms_loop(right, left, length, sway):
    """Pose parada dos bracos com respiracao (sway em graus no X)."""
    half = length / 2
    return {"loop": True, "animation_length": length, "bones": {
        "arm_right": {"rotation": kf((0, right), (half, add(right, [-sway, 0, 0])), (length, right))},
        "arm_left": {"rotation": kf((0, left), (half, add(left, [-sway, 0, 0])), (length, left))},
    }}


def arms_walk(right, left, swing):
    """Bracos balancando ao andar (opostos as pernas: perna esquerda para a frente = braco direito para a frente)."""
    return {"loop": True, "animation_length": WALK_LENGTH, "bones": {
        "arm_right": {"rotation": kf((0, add(right, [-swing, 0, 0])), (WALK_LENGTH / 2, add(right, [swing, 0, 0])),
                                     (WALK_LENGTH, add(right, [-swing, 0, 0])))},
        "arm_left": {"rotation": kf((0, add(left, [swing, 0, 0])), (WALK_LENGTH / 2, add(left, [-swing, 0, 0])),
                                    (WALK_LENGTH, add(left, [swing, 0, 0])))},
    }}


def shoot(pose):
    right, left = AIM[pose]
    kick = RECOIL[pose]
    return {"animation_length": 0.4, "bones": {
        "arm_right": {"rotation": kf((0, right), (0.05, add(right, [-kick, 0, 0])), (0.4, right))},
        "arm_left": {"rotation": kf((0, left), (0.05, add(left, [-kick * 0.7, 0, 0])), (0.4, left))},
        "body": {"rotation": kf((0, [0, 0, 0]), (0.05, [-2, 0, 0]), (0.4, [0, 0, 0]))},
    }}


def animations():
    """Controllers do SoldierEntity: "movement" (pernas e tronco), "arms" (pose da arma), "action" e "reaction".
    Cada controller mexe em ossos/canais diferentes, entao as camadas se somam (andar + mirar)."""
    p = NAME + "."
    leg = 30
    anims = {
        # Pernas e tronco ------------------------------------------------------------------------------------
        p + "movement.idle": {"loop": True, "animation_length": 3.0, "bones": {
            "body": {"position": kf((0, [0, 0, 0]), (1.5, [0, -0.25, 0]), (3.0, [0, 0, 0])),
                     "rotation": kf((0, [0, 0, 0]), (1.5, [1.5, 0, 0]), (3.0, [0, 0, 0]))},
            "leg_left": {"rotation": kf((0, [0, 0, -1]))},
            "leg_right": {"rotation": kf((0, [0, 0, 1]))},
        }},
        p + "movement.walk": {"loop": True, "animation_length": WALK_LENGTH, "bones": {
            "leg_left": {"rotation": kf((0, [-leg, 0, 0]), (WALK_LENGTH / 2, [leg, 0, 0]),
                                        (WALK_LENGTH, [-leg, 0, 0]))},
            "leg_right": {"rotation": kf((0, [leg, 0, 0]), (WALK_LENGTH / 2, [-leg, 0, 0]),
                                         (WALK_LENGTH, [leg, 0, 0]))},
            "body": {"position": kf((0, [0, 0, 0]), (WALK_LENGTH / 4, [0, 0.6, 0]), (WALK_LENGTH / 2, [0, 0, 0]),
                                    (WALK_LENGTH * 3 / 4, [0, 0.6, 0]), (WALK_LENGTH, [0, 0, 0])),
                     "rotation": kf((0, [3, 4, 0]), (WALK_LENGTH / 2, [3, -4, 0]), (WALK_LENGTH, [3, 4, 0]))},
        }},
        # Golpe corpo a corpo: pico em 0,25 s (o dano sai no tick do JSON da arma, no servidor).
        p + "action.attack": {"animation_length": 0.6, "bones": {
            "arm_right": {"rotation": kf((0, [-30, -10, 0]), (0.15, [-150, -10, 10]), (0.28, [-15, -35, -10]),
                                         (0.6, [-30, -10, 0]))},
            "arm_left": {"rotation": kf((0, [-20, 15, 0]), (0.15, [-40, 20, 0]), (0.6, [-20, 15, 0]))},
            "body": {"rotation": kf((0, [0, 0, 0]), (0.15, [-4, 18, 0]), (0.28, [6, -18, 0]), (0.6, [0, 0, 0]))},
        }},
        p + "action.shoot_rifle": shoot("rifle"),
        p + "action.shoot_pistol": shoot("pistol"),
        p + "reaction.hurt": {"animation_length": 0.3, "bones": {
            "body": {"rotation": kf((0, [0, 0, 0]), (0.1, [-8, 0, 0]), (0.3, [0, 0, 0]))},
        }},
    }
    # Bracos: <pose>_ready (parado), <pose>_walk (andando) e <pose>_aim (com alvo) para cada classe de arma.
    for pose, (right, left) in POSES.items():
        firearm = pose in RECOIL
        anims[f"{p}arms.{pose}_ready"] = arms_loop(right, left, 3.0, 2)
        anims[f"{p}arms.{pose}_walk"] = arms_loop(right, left, WALK_LENGTH / 2, 3) if firearm \
            else arms_walk(right, left, 20 if pose == "unarmed" else 12)
        aim_right, aim_left = AIM[pose]
        anims[f"{p}arms.{pose}_aim"] = arms_loop(aim_right, aim_left, 2.0, 1)
    return {"format_version": "1.8.0", "animations": anims}


def main():
    meshes = split(load_mesh())
    pivots = {
        "root": np.zeros(3),
        "body": np.array([0.0, HIP_Y, 0.0]),
        "head": np.array([0.0, NECK_Y, bottom_center(meshes["head"])[2]]),
        "arm_left": top_point(meshes["arm_left"]),
        "arm_right": top_point(meshes["arm_right"]),
        "leg_left": np.array([top_point(meshes["leg_left"])[0], HIP_Y, top_point(meshes["leg_left"])[2]]),
        "leg_right": np.array([top_point(meshes["leg_right"])[0], HIP_Y, top_point(meshes["leg_right"])[2]]),
        "item_right": hand_point(meshes["arm_right"]),
    }
    parents = {"root": None, "body": "root", "head": "body", "arm_left": "body", "arm_right": "body",
               "leg_left": "root", "leg_right": "root", "item_right": "arm_right"}
    mesh_dir = ASSETS / "meshes" / NAME
    if mesh_dir.exists():
        shutil.rmtree(mesh_dir)
    index = {"bones": {}}
    for bone, (v, t, n, f) in meshes.items():
        write_obj(mesh_dir / f"{bone}.obj", v, t, n, f)
        index["bones"][bone] = f"kn8:meshes/{NAME}/{bone}.obj"
        print(f"{bone}: {len(f)} triangulos")
    (ASSETS / "meshes" / f"{NAME}.json").write_text(json.dumps(index, indent=2) + "\n", encoding="utf-8")
    # Textura com borda nas ilhas e reduzida (sem mipmap no Minecraft: 1024 cintila e mostra as costuras).
    image = Image.open(SOURCE / "soldier_1.png").convert("RGBA")
    mask = uv_mask([SOURCE / "soldier_1.obj"], image.width, image.height)
    padded = dilate(np.asarray(image)[..., :3], mask, DILATE_STEPS)
    Image.fromarray(np.clip(padded, 0, 255).astype(np.uint8)).convert("RGBA") \
        .resize((TEXTURE_SIZE, TEXTURE_SIZE), Image.Resampling.BOX).save(ASSETS / f"textures/entity/{NAME}.png")
    bones = []
    for bone, parent in parents.items():
        entry = {"name": bone, "pivot": geo_pivot(pivots[bone])}
        if parent:
            entry["parent"] = parent
        bones.append(entry)
    geo = {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": f"geometry.{NAME}", "texture_width": 16, "texture_height": 16,
                        "visible_bounds_width": 3, "visible_bounds_height": 3, "visible_bounds_offset": [0, 1, 0]},
        "bones": bones}]}
    (ASSETS / f"geo/entity/{NAME}.geo.json").write_text(json.dumps(geo, indent=2) + "\n", encoding="utf-8")
    (ASSETS / f"animations/entity/{NAME}.animation.json").write_text(json.dumps(animations(), indent=2) + "\n",
                                                                     encoding="utf-8")
    print("pivos:", {k: np.round(v, 2).tolist() for k, v in pivots.items()})


if __name__ == "__main__":
    main()
