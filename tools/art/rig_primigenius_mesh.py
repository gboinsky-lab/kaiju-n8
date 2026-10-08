#!/usr/bin/env python3
"""Rigging do Primigenius do Meshy (malha presa aos ossos da GeckoLib), modelo "estilo Minecraft" de 2026-10-06.

Entrada: tools/art/converted/<especie>/<especie>.obj + .png (meshy_convert.py), em metros, pes em Y = 0, frente -Z.
Saida (por especie):
  - assets/kn8/meshes/<especie>.json + meshes/<especie>/<osso>.obj;
  - assets/kn8/textures/entity/<especie>.png (com borda nas ilhas de UV: pad_texture.py);
  - assets/kn8/geo/entity/<especie>.geo.json: os MESMOS ossos do modelo de cubos (body, head, jaw, fin_left/right,
    arm_*/forearm_*, leg_*, tail_1..4), so que sem cubos e com os pivos nas juntas da malha nova.
As animacoes (animations/entity/<especie>.animation.json, de build_primigenius.py) continuam as mesmas: so giram
ossos, entao valem para a malha nova.

Divisao pela forma (cortes em metros na tabela SPECIES, medidos nas vistas de tools/art/preview.py / no perfil):
cauda (atras e baixo), cabeca (frente e alto; mandibula = parte de baixo da boca), bracos (fora do tronco: o vao
entre braco e tronco fecha conforme sobe, por isso a borda de dentro depende da altura), antebraco (braco abaixo do
cotovelo), pernas (abaixo do quadril, pelo lado: X negativo = "left"), o resto e o corpo. As "fin" (barbatanas do
modelo de cubos) ficam sem malha; nas especies com "horn_min_y" os chifres vao para horn_left/right.
A GeckoLib inverte o X do pivo ao carregar: os pivos sao gravados com X negado.
Uso: python3 tools/art/rig_primigenius_mesh.py [especie]   (padrao: primigenius; requer numpy e pillow)
"""
import json
import shutil
import sys
from pathlib import Path

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).parent))
from pad_texture import DILATE_STEPS, dilate, uv_mask  # noqa: E402
from rig_soldier_mesh import cap_holes  # noqa: E402
from rig_trichonephila_mesh import read_obj, write_obj  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/kn8"
PX = 16.0
TEXTURE_SIZE = 1024

SPECIES = {
    # Segunda leva do Meshy (2026-10-06, a tarde): corpo curvado e cauda longa; todas no modo esqueleto (o corte por
    # plano soltava garras no slam). "recenter_feet" poe os pes no centro da hitbox; juntas ja nessas coordenadas
    # (frente = -Z). As especies sem "skeleton" usariam os cortes por plano de split().
    "primigenius": {
        "recenter_feet": True, "jaw_y": 4.5, "jaw_z": -2.3,
        "skeleton": {
            "pelvis": [0.0, 2.4, 0.3],
            "chest": [0.0, 3.6, -0.6],
            "back": [0.0, 4.6, 0.2],
            "neck": [0.0, 4.4, -1.5],
            "head": [0.05, 4.9, -3.0],
            "shoulder_left": [-1.35, 3.85, -0.9],
            "elbow_left": [-1.75, 2.9, -1.7],
            "hand_left": [-1.7, 2.6, -3.2],
            "shoulder_right": [1.35, 3.85, -0.9],
            "elbow_right": [1.7, 2.8, -1.8],
            "hand_right": [1.6, 2.5, -3.4],
            "hip_left": [-0.8, 2.1, 0.3],
            "knee_left": [-1.2, 1.0, 0.0],
            "foot_left": [-1.47, 0.15, 0.25],
            "hip_right": [0.8, 2.1, 0.0],
            "knee_right": [1.25, 1.0, -0.3],
            "foot_right": [1.51, 0.15, -0.28],
            "tail_base": [0.0, 2.8, 1.4],
            "tail_mid": [0.15, 2.2, 3.6],
            "tail_tip": [0.32, 1.82, 6.19],
        },
    },
    "primigenius_resurrected": {
        "recenter_feet": True, "jaw_y": 4.75, "jaw_z": -2.6,
        "skeleton": {
            "pelvis": [0.0, 2.4, 0.2],
            "chest": [0.0, 3.7, -0.7],
            "back": [0.0, 4.7, 0.1],
            "neck": [0.0, 4.6, -1.5],
            "head": [0.05, 5.1, -2.8],
            "shoulder_left": [-1.2, 4.1, -1.1],
            "elbow_left": [-1.6, 3.1, -1.6],
            "hand_left": [-1.75, 2.6, -2.8],
            "shoulder_right": [1.2, 4.1, -1.1],
            "elbow_right": [1.6, 3.1, -1.6],
            "hand_right": [1.85, 2.55, -2.8],
            "hip_left": [-0.6, 2.2, 0.4],
            "knee_left": [-0.9, 1.1, 0.9],
            "foot_left": [-1.02, 0.15, 0.84],
            "hip_right": [0.6, 2.2, -0.2],
            "knee_right": [1.0, 1.1, -0.6],
            "foot_right": [1.13, 0.15, -0.97],
            "tail_base": [0.0, 2.8, 1.4],
            "tail_mid": [0.0, 2.2, 3.3],
            "tail_tip": [0.02, 1.62, 5.3],
        },
    },
    # Honju e revivido: pose agachada com as maos quase no chao. Os dois GLB vieram girados ~35 graus em Y
    # (yaw_deg). Juntas em metros, medidas nas vistas com o esqueleto desenhado por cima do modelo.
    "primigenius_honju": {
        "yaw_deg": 35, "recenter_feet": True, "jaw_y": 6.4, "jaw_z": -2.6,
        "skeleton": {
            "pelvis": [0.5, 3.0, 0.4],
            "chest": [0.7, 5.0, -1.0],
            "neck": [0.9, 6.2, -2.0],
            "head": [0.9, 7.0, -3.3],
            "shoulder_left": [-2.0, 5.4, -1.5],
            "elbow_left": [-2.7, 3.3, -2.3],
            "hand_left": [-2.92, 1.2, -3.25],
            "shoulder_right": [3.3, 5.4, -1.5],
            "elbow_right": [3.0, 3.3, -3.0],
            "hand_right": [1.99, 1.2, -4.52],
            "hip_left": [-1.4, 2.6, 0.5],
            "knee_left": [-1.8, 1.3, 1.0],
            "foot_left": [-2.2, 0.15, 1.0],
            "hip_right": [2.0, 2.6, 0.2],
            "knee_right": [2.6, 1.2, -0.5],
            "foot_right": [2.8, 0.15, -0.6],
            "tail_base": [0.5, 4.2, 2.6],
            "tail_mid": [0.4, 3.0, 5.0],
            "tail_tip": [0.2, 6.2, 7.7],
            "horn_tip_left": [-0.64, 9.0, -2.16],
            "horn_tip_right": [2.3, 8.43, -4.16],
            "back": [0.6, 6.6, 0.4],
        },
    },
    # 0.5.0-C: No. 10 forma pequena refeita pelo Miguel (4 m; cauda enrola para tras e para a esquerda, desce ate
    # ~0,8 m, abre ate 2 m do eixo e sobe de novo ate 2,3 m): base -> fundo -> curva de fora -> ponta.
    "kaiju_no10_small": {
        "recenter_feet": True, "jaw_y": 3.5, "jaw_z": -0.12, "head_cylinder": [3.38, 0.3],
        # Rosto ~0,1 m fora do eixo do tronco: volta ao meio (Miguel pediu a cabeca centrada na 0.6-E).
        "head_shift": [-0.1, 0.0], "head_shift_blend": 0.35, "head_shift_radius": 0.55,
        "skeleton": {
            "pelvis": [0.0, 2.0, 0.05], "chest": [0.0, 2.75, 0.05], "back": [0.0, 3.0, 0.4],
            "neck": [0.05, 3.25, -0.05], "head": [0.1, 3.6, -0.1],
            "shoulder_left": [-0.7, 2.9, 0.0], "elbow_left": [-0.89, 2.35, -0.05],
            "hand_left": [-0.76, 1.65, -0.2],
            "shoulder_right": [0.72, 2.9, 0.0], "elbow_right": [0.92, 2.35, -0.05],
            "hand_right": [0.86, 1.6, -0.2],
            "hip_left": [-0.35, 1.9, 0.0], "knee_left": [-0.56, 1.0, -0.1], "foot_left": [-0.66, 0.15, -0.15],
            "hip_right": [0.35, 1.9, 0.0], "knee_right": [0.62, 1.0, -0.1], "foot_right": [0.72, 0.15, -0.15],
            "tail_base": [-0.1, 1.5, 0.45], "tail_mid": [-1.1, 0.8, 1.35], "tail_bend": [-1.95, 1.6, 2.15],
            "tail_tip": [-1.4, 2.3, 1.75],
        },
    },
    # 0.5.0-C: forma gigante refeita pelo Miguel (24 m; chifre longo para a frente, 8 m, fica na cabeca; cauda
    # enrolada para tras e para a esquerda: desce ate ~2 m, abre ate 13 m do eixo e sobe ate 16 m).
    "kaiju_no10_giant": {
        "recenter_feet": True, "jaw_y": 20.3, "jaw_z": -1.3, "head_cylinder": [19.6, 2.2],
        "head_shift": [-0.4, 0.0], "head_shift_blend": 2.0, "head_shift_radius": 3.2,
        "skeleton": {
            "pelvis": [0.0, 9.5, 0.5], "chest": [0.0, 15.5, 0.3], "back": [0.0, 18.0, 2.5],
            "neck": [0.2, 19.0, -0.5], "head": [0.4, 21.0, -1.5],
            "shoulder_left": [-4.3, 18.0, 0.0], "elbow_left": [-7.1, 13.0, -0.5], "hand_left": [-6.9, 9.0, -1.5],
            "shoulder_right": [4.3, 18.0, 0.0], "elbow_right": [6.9, 13.0, -0.5], "hand_right": [6.9, 9.0, -1.5],
            "hip_left": [-2.5, 9.5, 0.5], "knee_left": [-3.9, 5.0, 1.5], "foot_left": [-5.5, 0.6, -0.5],
            "hip_right": [2.5, 9.5, 0.5], "knee_right": [3.7, 5.0, 1.5], "foot_right": [5.5, 0.6, -0.5],
            "tail_base": [-0.5, 7.5, 3.5], "tail_mid": [-5.0, 2.0, 12.5], "tail_bend": [-13.0, 7.0, 15.0],
            "tail_tip": [-10.0, 16.0, 9.5],
        },
    },
    "primigenius_revived": {
        "yaw_deg": 35, "recenter_feet": True, "jaw_y": 6.4, "jaw_z": -2.9,
        "skeleton": {
            "pelvis": [0.5, 3.0, 0.3],
            "chest": [0.7, 5.0, -1.0],
            "back": [0.5, 6.8, 0.2],
            "neck": [0.8, 6.0, -1.8],
            "head": [0.8, 6.9, -3.2],
            "horn_tip_left": [-0.37, 9.0, -1.64],
            "horn_tip_right": [2.29, 8.51, -3.29],
            "shoulder_left": [-2.2, 5.4, -1.3],
            "elbow_left": [-2.8, 3.4, -2.3],
            "hand_left": [-2.9, 1.2, -3.4],
            "shoulder_right": [3.0, 5.4, -1.5],
            "elbow_right": [2.7, 3.4, -2.8],
            "hand_right": [2.01, 1.2, -4.34],
            "hip_left": [-1.0, 2.6, 0.6],
            "knee_left": [-1.3, 1.3, 1.27],
            "foot_left": [-1.9, 0.15, 1.0],
            "hip_right": [1.8, 2.6, 0.0],
            "knee_right": [2.3, 1.3, -0.7],
            "foot_right": [2.3, 0.15, -1.1],
            "tail_base": [0.4, 4.0, 2.3],
            "tail_mid": [0.0, 3.0, 5.0],
            "tail_tip": [-0.4, 6.2, 6.4],
        },
    },
}


# Modo esqueleto (Honju da segunda leva, pose agachada com as maos quase no chao, onde cortes por plano misturam mao,
# perna e tronco): cada osso recebe sementes ao longo do seu segmento do esqueleto e cada vertice vai para a semente
# mais proxima ANDANDO PELA SUPERFICIE (Dijkstra na malha soldada). Uma mao encostada na coxa continua sendo mao,
# porque o caminho pela pele entre as duas passa pelo tronco. (osso, junta de inicio, junta de fim, trecho t0..t1)
SKELETON_SEEDS = [
    ("body", "pelvis", "chest", 0.0, 1.0), ("body", "chest", "neck", 0.0, 0.9),
    ("body", "chest", "shoulder_left", 0.0, 0.45), ("body", "chest", "shoulder_right", 0.0, 0.45),
    ("body", "pelvis", "hip_left", 0.0, 0.45), ("body", "pelvis", "hip_right", 0.0, 0.45),
    ("body", "pelvis", "tail_base", 0.0, 0.5), ("body", "tail_base", "chest", 0.3, 1.0),
    ("head", "neck", "head", 0.7, 1.0),
    ("arm_left", "shoulder_left", "elbow_left", 0.3, 0.8), ("forearm_left", "elbow_left", "hand_left", 0.25, 1.0),
    ("arm_right", "shoulder_right", "elbow_right", 0.3, 0.8), ("forearm_right", "elbow_right", "hand_right", 0.25, 1.0),
    ("leg_left", "hip_left", "knee_left", 0.35, 1.0), ("leg_left", "knee_left", "foot_left", 0.0, 1.0),
    ("leg_right", "hip_right", "knee_right", 0.35, 1.0), ("leg_right", "knee_right", "foot_right", 0.0, 1.0),
    ("tail", "tail_base", "tail_mid", 0.25, 1.0), ("tail", "tail_mid", "tail_tip", 0.0, 1.0),
]


def weld(vertices):
    """Indice de vertice soldado: o OBJ separa vertices iguais nas costuras de UV."""
    _, index = np.unique(np.round(vertices, 5), axis=0, return_inverse=True)
    return index.reshape(-1)


def split_skeleton(mesh, cfg):
    from scipy.sparse import coo_matrix
    from scipy.sparse.csgraph import dijkstra
    vertices, _, _, faces = mesh
    skeleton = {name: np.array(point, dtype=float) for name, point in cfg["skeleton"].items()}
    welded = weld(vertices)
    count = welded.max() + 1
    position = np.zeros((count, 3))
    position[welded] = vertices
    wf = welded[faces]
    edges = np.vstack([wf[:, [0, 1]], wf[:, [1, 2]], wf[:, [2, 0]]])
    length = np.linalg.norm(position[edges[:, 0]] - position[edges[:, 1]], axis=1) + 1e-6
    graph = coo_matrix((length, (edges[:, 0], edges[:, 1])), shape=(count, count)).tocsr()
    seeds, owners = [], []
    # Chifres com ponta no esqueleto: osso proprio pela superficie (a cabeca do Honju e torta, plano nao serve).
    extra = [(f"horn_{side}", "head", f"horn_tip_{side}", 0.8, 1.0) for side in ("left", "right")
             if f"horn_tip_{side}" in skeleton]
    if "back" in skeleton:
        # Ponto no alto das costas: sem ele a pele das costas ficava mais perto das sementes da cauda.
        extra += [("body", "tail_base", "back", 0.3, 1.0), ("body", "back", "neck", 0.0, 1.0)]
    seeds_table = SKELETON_SEEDS
    if "tail_bend" in skeleton:
        # Cauda enrolada (No. 10 refeito): base -> meio -> curva -> ponta; uma reta meio -> ponta cortaria o vazio.
        seeds_table = [row for row in SKELETON_SEEDS if row[1:3] != ("tail_mid", "tail_tip")]
        seeds_table += [("tail", "tail_mid", "tail_bend", 0.0, 1.0), ("tail", "tail_bend", "tail_tip", 0.0, 1.0)]
    for bone, start, end, t0, t1 in seeds_table + extra:
        for t in np.linspace(t0, t1, 6):
            point = skeleton[start] + (skeleton[end] - skeleton[start]) * t
            seeds.append(int(np.argmin(np.linalg.norm(position - point, axis=1))))
            owners.append(bone)
    _, _, sources = dijkstra(graph, directed=False, indices=sorted(set(seeds)), min_only=True,
                             return_predecessors=True)
    owner_of = {seed: owner for seed, owner in zip(seeds, owners)}
    vertex_label = np.array([owner_of.get(int(src), "body") for src in sources], dtype=object)
    # Rotulo da face: o da maioria dos 3 vertices (empate: o do primeiro).
    a, b, c = (vertex_label[wf[:, i]] for i in range(3))
    labels = np.where(b == c, b, a)
    centers = vertices[faces].mean(axis=1)
    x, y, z = centers[:, 0], centers[:, 1], centers[:, 2]
    if "head_cylinder" in cfg:
        # Cabeca larga e baixa (No. 10 refeito): pela superficie os espinhos do tronco ficam mais perto da metade de
        # baixo do rosto; acima do queixo e perto do eixo da cabeca, e cabeca.
        min_y, radius = cfg["head_cylinder"]
        near = np.hypot(x - skeleton["head"][0], z - skeleton["head"][2]) < radius
        labels[(labels == "body") & (y > min_y) & near] = "head"
    head = labels == "head"
    head_dx = x - skeleton["head"][0]
    labels[head & (y < cfg["jaw_y"]) & (z < cfg["jaw_z"])] = "jaw"
    if "horn_min_y" in cfg and "horn_tip_left" not in skeleton:
        horn = head & (y > cfg["horn_min_y"]) & (np.abs(head_dx) > cfg["horn_x"])
        labels[horn & (head_dx <= 0)] = "horn_left"
        labels[horn & (head_dx > 0)] = "horn_right"
    # Cauda em 4 pedacos pelo comprimento ao longo da linha base -> meio -> ponta (a cauda faz curva).
    tail = labels == "tail"
    labels[tail] = [f"tail_{min(4, int(tail_param(point, skeleton) * 4) + 1)}" for point in centers[tail]]
    return labels


def shift_head(vertices, faces, labels, cfg):
    """Leva a cabeca (cabeca, mandibula, chifres) para o eixo do tronco: desloca por "head_shift" (dx, dz) inteira e,
    no pescoco (faixa de "head_shift_blend" abaixo da cabeca, ate "head_shift_radius" do eixo), aos poucos, para nao
    abrir a juncao. As juntas da cabeca (pescoco, cabeca) andam junto. Devolve vertices e esqueleto novos."""
    dx, dz = cfg["head_shift"]
    head_labels = [name for name in set(labels) if name in ("head", "jaw") or name.startswith("horn_")]
    head_vertices = np.unique(faces[np.isin(labels, head_labels)])
    bottom = vertices[head_vertices, 1].min()
    blend = cfg.get("head_shift_blend", 0.3)
    neck = np.array(cfg["skeleton"]["neck"], dtype=float)
    weight = np.clip((vertices[:, 1] - (bottom - blend)) / blend, 0.0, 1.0)
    near = np.hypot(vertices[:, 0] - neck[0], vertices[:, 2] - neck[2]) < cfg.get("head_shift_radius", 0.5)
    body = np.zeros(len(vertices), dtype=bool)
    body[np.unique(faces[labels == "body"])] = True
    # So o pescoco (tronco) faz a transicao; ombros e bracos ficam onde estao.
    arms = np.unique(faces[np.char.startswith(labels.astype(str), "arm") | np.char.startswith(labels.astype(str),
                                                                                            "forearm")])
    body[arms] = False
    weight = np.where(near & body, weight, 0.0)
    weight[head_vertices] = 1.0
    vertices = vertices.copy()
    vertices[:, 0] += weight * dx
    vertices[:, 2] += weight * dz
    skeleton = dict(cfg["skeleton"])
    for joint in ("neck", "head"):
        skeleton[joint] = [skeleton[joint][0] + dx, skeleton[joint][1], skeleton[joint][2] + dz]
    return vertices, {**cfg, "skeleton": skeleton}


def tail_polyline(skeleton):
    bend = [skeleton["tail_bend"]] if "tail_bend" in skeleton else []
    return [skeleton["tail_base"], skeleton["tail_mid"], *bend, skeleton["tail_tip"]]


def tail_param(point, skeleton):
    """Posicao (0 = base, 1 = ponta) do ponto mais proximo na linha da cauda, pelo comprimento."""
    line = tail_polyline(skeleton)
    lengths = [np.linalg.norm(line[i + 1] - line[i]) for i in range(len(line) - 1)]
    total, walked, best = sum(lengths), 0.0, (np.inf, 0.0)
    for i, segment in enumerate(lengths):
        direction = line[i + 1] - line[i]
        t = np.clip(np.dot(point - line[i], direction) / max(segment ** 2, 1e-9), 0, 1)
        distance = np.linalg.norm(point - (line[i] + direction * t))
        if distance < best[0]:
            best = (distance, (walked + t * segment) / total)
        walked += segment
    return best[1]


def tail_point(skeleton, fraction):
    line = tail_polyline(skeleton)
    lengths = [np.linalg.norm(line[i + 1] - line[i]) for i in range(len(line) - 1)]
    target = fraction * sum(lengths)
    for i, segment in enumerate(lengths):
        if target <= segment:
            return line[i] + (line[i + 1] - line[i]) * (target / segment)
        target -= segment
    return line[-1]


def pivots_skeleton(vertices, faces, labels, cfg):
    """Pivos direto nas juntas do esqueleto (mesmos nomes de osso do modelo de cubos)."""
    sk = {name: np.array(point, dtype=float) for name, point in cfg["skeleton"].items()}
    result = {"root": np.zeros(3), "body": sk["pelvis"], "head": sk["neck"],
              "jaw": np.array([sk["head"][0], cfg["jaw_y"], cfg["jaw_z"]])}
    for side, sign in (("left", -1), ("right", 1)):
        result[f"arm_{side}"] = sk[f"shoulder_{side}"]
        result[f"forearm_{side}"] = sk[f"elbow_{side}"]
        result[f"leg_{side}"] = sk[f"hip_{side}"]
        result[f"fin_{side}"] = sk["head"] + [sign * 0.5, 0.5, 0.3]
        if np.any(labels == f"horn_{side}"):
            horn = vertices[np.unique(faces[labels == f"horn_{side}"])]
            # Base do chifre: a parte do chifre mais perto do centro da cabeca.
            nearest = np.argsort(np.linalg.norm(horn - sk["head"], axis=1))[:max(3, len(horn) // 10)]
            result[f"horn_{side}"] = horn[nearest].mean(axis=0)
    for number in range(1, 5):
        result[f"tail_{number}"] = tail_point(sk, (number - 1) / 4)
    return result


def split(mesh, cfg):
    vertices, _, _, faces = mesh
    c = vertices[faces].mean(axis=1)
    x, y, z = c[:, 0], c[:, 1], c[:, 2]
    labels = np.array(["body"] * len(faces), dtype=object)
    tail = (z > cfg["tail_z"]) & (y < cfg["tail_max_y"]) & (np.abs(x) < cfg["tail_half_width"])
    if "tail_low_z" in cfg:
        # Perna de tras (passada aberta): abaixo do quadril so e cauda o que esta atras do pe.
        tail &= (y >= cfg["hip_y"]) | (z > cfg["tail_low_z"])
    head = ~tail & (z < cfg["head_z"]) & (y > cfg["head_min_y"])
    # Centro da cabeca e do tronco em X (o Honju da segunda leva tem o tronco ~0,7 m para o lado dos pes).
    head_dx = x - cfg.get("head_x", 0.0)
    body_dx = x - cfg.get("body_x", 0.0)
    if "head_half_width" in cfg:
        # Cabeca baixa e projetada (segunda leva): sem limite de largura os ombros entravam na cabeca.
        head &= np.abs(head_dx) < cfg["head_half_width"]
    ys, xs = zip(*cfg["arm_inner"])
    inner = np.interp(y, ys, xs)
    arm = ~tail & ~head & (y >= cfg["arm_min_y"]) & (y <= cfg["arm_max_y"]) & (np.abs(body_dx) > inner)
    if "arm_low_z" in cfg:
        # Abaixo do quadril so e braco o que esta a frente das pernas (maos quase no chao, ao lado das coxas).
        arm &= (y >= cfg["hip_y"]) | (z < cfg["arm_low_z"])
    if "arm_back_z" in cfg:
        # Lateral/costas do tronco atras do ombro nao e braco (ao girar o braco elas iam para a frente).
        arm &= z < cfg["arm_back_z"]
    if "arm_front_z" in cfg:
        # Maos e dedos a frente dos pes (Honju curvado): sempre braco, mesmo abaixo de arm_min_y.
        arm |= ~tail & ~head & (z < cfg["arm_front_z"]) & (y <= cfg["arm_max_y"]) & (np.abs(body_dx) > cfg["arm_front_x"])
    leg = ~tail & ~head & ~arm & (y < cfg["hip_y"])
    labels[head] = "head"
    labels[head & (y < cfg["jaw_y"]) & (z < cfg["jaw_z"])] = "jaw"
    if "horn_min_y" in cfg:
        horn = head & (y > cfg["horn_min_y"]) & (np.abs(head_dx) > cfg["horn_x"])
        labels[horn & (head_dx <= 0)] = "horn_left"
        labels[horn & (head_dx > 0)] = "horn_right"
    for side, body_side, leg_side in (("left", body_dx <= 0, x <= 0), ("right", body_dx > 0, x > 0)):
        labels[arm & body_side & (y >= cfg["elbow_y"])] = f"arm_{side}"
        labels[arm & body_side & (y < cfg["elbow_y"])] = f"forearm_{side}"
        labels[leg & leg_side] = f"leg_{side}"
    if tail.any():
        start, end = z[tail].min(), z[tail].max()
        segment = np.clip(((z - start) / (end - start) * 4).astype(int), 0, 3)
        for number in range(4):
            labels[tail & (segment == number)] = f"tail_{number + 1}"
    return labels


def prepare(vertices, normals, cfg):
    """Ajustes do modelo antes dos cortes: "yaw_deg" desfaz um giro do modelo em volta de Y (os Honju da segunda
    leva vieram ~35 graus tortos; angulo achado pela simetria esquerda/direita) e "recenter_feet" poe os pes no
    centro da hitbox."""
    if cfg.get("yaw_deg"):
        angle = np.radians(cfg["yaw_deg"])
        turn = np.array([[np.cos(angle), 0, np.sin(angle)], [0, 1, 0], [-np.sin(angle), 0, np.cos(angle)]])
        vertices = vertices @ turn.T
        normals = normals @ turn.T
    if cfg.get("recenter_feet"):
        vertices = vertices - feet_center(vertices)
    return vertices, normals


def feet_center(vertices, feet_height=0.35):
    """Centro (X/Z) dos pes: o meshy_convert centra pela caixa inteira, e nos modelos curvados de cauda longa
    (2026-10-06, segunda leva) isso deixava o corpo para a frente da hitbox. Com "recenter_feet" o kaiju fica em pe
    sobre a hitbox e a cauda sai para tras."""
    feet = vertices[vertices[:, 1] < feet_height]
    return np.array([(feet[:, 0].min() + feet[:, 0].max()) / 2, 0.0, (feet[:, 2].min() + feet[:, 2].max()) / 2])


def band_mean(points, axis, low, high):
    sel = points[(points[:, axis] >= low) & (points[:, axis] <= high)]
    return sel.mean(axis=0) if len(sel) else points.mean(axis=0)


def pivots(vertices, faces, labels, cfg):
    def pts(name):
        return vertices[np.unique(faces[labels == name])]

    result = {"root": np.zeros(3), "body": np.array([0.0, cfg["hip_y"], pts("body")[:, 2].mean()])}
    head = pts("head")
    # Pescoco: parte de tras da cabeca; mandibula: dobradica atras da boca.
    result["head"] = band_mean(head, 2, head[:, 2].max() - 0.3, head[:, 2].max()) * [0, 1, 1]
    result["jaw"] = np.array([0.0, cfg["jaw_y"], cfg["jaw_z"]])
    top = head[:, 1].max()
    for side, sign in (("left", -1), ("right", 1)):
        arm = pts(f"arm_{side}")
        result[f"arm_{side}"] = band_mean(arm, 1, cfg["arm_max_y"] - 0.4, cfg["arm_max_y"])
        both = np.vstack([arm, pts(f"forearm_{side}")])
        result[f"forearm_{side}"] = band_mean(both, 1, cfg["elbow_y"] - 0.15, cfg["elbow_y"] + 0.15)
        leg = pts(f"leg_{side}")
        result[f"leg_{side}"] = band_mean(leg, 1, cfg["hip_y"] - 0.3, cfg["hip_y"])
        result[f"fin_{side}"] = np.array([sign * 0.5, top, result["head"][2] - 0.3])
        if np.any(labels == f"horn_{side}"):
            horn = pts(f"horn_{side}")
            # Base do chifre: a parte mais perto do meio da cabeca.
            result[f"horn_{side}"] = band_mean(horn, 0, *sorted((sign * cfg["horn_x"],
                                                                  sign * (cfg["horn_x"] + 0.3))))
    for number in range(1, 5):
        segment = pts(f"tail_{number}")
        result[f"tail_{number}"] = band_mean(segment, 2, segment[:, 2].min(), segment[:, 2].min() + 0.25)
    return result


def geo_pivot(point):
    return [round(-point[0] * PX, 3), round(point[1] * PX, 3), round(point[2] * PX, 3)]


def main():
    name = sys.argv[1] if len(sys.argv) > 1 else "primigenius"
    cfg = SPECIES[name]
    source = ROOT / "tools/art/converted" / name
    vertices, uvs, normals, faces = read_obj(source / f"{name}.obj")
    vertices, normals = prepare(vertices, normals, cfg)
    skeleton_mode = "skeleton" in cfg
    labels = (split_skeleton if skeleton_mode else split)((vertices, uvs, normals, faces), cfg)
    # Hierarquia e ordem dos ossos do modelo de cubos (as animacoes usam estes nomes).
    old_geo = json.loads((ASSETS / f"geo/entity/{name}.geo.json").read_text(encoding="utf-8"))
    old_bones = old_geo["minecraft:geometry"][0]["bones"]
    if "head_shift" in cfg:
        vertices, cfg = shift_head(vertices, faces, labels, cfg)
    points = (pivots_skeleton if skeleton_mode else pivots)(vertices, faces, labels, cfg)
    # Tampa os cortes das juntas com a cor da pele em volta (sem isso o ombro "abre" no slam e mostra o oco).
    vertices, uvs, normals, faces, labels = cap_holes(vertices, uvs, normals, faces, labels)
    for bone in old_bones:
        # Osso sem malha (ex.: horn_* quando o modelo nao tem chifre): pivo no da cabeca.
        points.setdefault(bone["name"], points["head"])
    mesh_dir = ASSETS / "meshes" / name
    if mesh_dir.exists():
        shutil.rmtree(mesh_dir)
    index = {"bones": {}}
    for bone in old_bones:
        part = faces[labels == bone["name"]]
        if len(part):
            write_obj(mesh_dir / f"{bone['name']}.obj", vertices, uvs, normals, part)
            index["bones"][bone["name"]] = f"kn8:meshes/{name}/{bone['name']}.obj"
            print(f"{bone['name']}: {len(part)} triangulos")
    missing = set(labels) - {b["name"] for b in old_bones}
    assert not missing, f"faces em ossos que nao existem: {missing}"
    (ASSETS / "meshes" / f"{name}.json").write_text(json.dumps(index, indent=2) + "\n", encoding="utf-8")
    image = Image.open(source / f"{name}.png").convert("RGBA")
    mask = uv_mask([source / f"{name}.obj"], image.width, image.height)
    padded = dilate(np.asarray(image)[..., :3], mask, DILATE_STEPS)
    Image.fromarray(np.clip(padded, 0, 255).astype(np.uint8)).convert("RGBA") \
        .resize((TEXTURE_SIZE, TEXTURE_SIZE), Image.Resampling.BOX).save(ASSETS / f"textures/entity/{name}.png")
    bones = []
    for bone in old_bones:
        entry = {"name": bone["name"], "pivot": geo_pivot(points[bone["name"]])}
        if "parent" in bone:
            entry["parent"] = bone["parent"]
        bones.append(entry)
    low, high = vertices.min(axis=0), vertices.max(axis=0)
    span = float(np.max(high - low))
    geo = {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": f"geometry.{name}", "texture_width": 16, "texture_height": 16,
                        "visible_bounds_width": round(span + 1, 1), "visible_bounds_height": round(span + 1, 1),
                        "visible_bounds_offset": [0, round(float(high[1]) / 2, 1), 0]},
        "bones": bones}]}
    (ASSETS / f"geo/entity/{name}.geo.json").write_text(json.dumps(geo, indent=2) + "\n", encoding="utf-8")
    print("pivos:", {k: np.round(v, 2).tolist() for k, v in points.items()})


if __name__ == "__main__":
    main()
