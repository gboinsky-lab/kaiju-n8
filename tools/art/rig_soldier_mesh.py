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
STEM = "soldier_1"
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
# Itens nas costas (0.6-D: bainhas do Hoshina) ficam no tronco: pecas soltas (componente conexo separado do corpo)
# com pelo menos min_faces faces e inteiramente atras de z > z_min.
BACK_ITEMS = None
# 0.6-D: outras especies humanoides com o mesmo rig (soldado especial). use_species troca as medidas acima.
SPECIES = {
    # 0.5.0-B: soldado refeito pelo Miguel (capacete, 1,9 m, 11 mil tri): maos ate y 0,78, quadril em 0,90.
    # O capacete e mais largo que o pescoco: acima de arm_max_y nada e braco.
    # 0.5.0-B: soldado estilo Minecraft do Miguel (capacete desce ate 1,44 com |x| ate 0,27; ombro sobe ate 1,46 por
    # fora dele; bolsas do cinto ate |x| 0,20 ficam no tronco).
    "soldier": {"stem": "soldier_1", "hip_y": 0.90, "neck_y": 1.44, "hand_min_y": 0.80,
                "arm_inner_x": [(1.44, 0.28), (1.12, 0.168), (0.0, 0.214)], "arm_max_y": 1.50, "back_items": None},
    # Hoshina (modelo do Miguel, 1,85 m): bracos a partir de |x| 0,25; as duas bainhas nas costas saem pela lateral
    # esquerda do modelo (x < -0,28) e ficam atras do tronco (z > 0,09).
    # 0.5.0-D4: Hoshina refeito de novo pelo Miguel (o anterior tinha um rosto copiado na nuca). Sem bainhas nas
    # costas (back_items None); o cinto fica entre 0,80 e 0,95, entao o quadril desce para 0,80 (cinto no tronco).
    "hoshina": {"stem": "hoshina", "hip_y": 0.80, "neck_y": 1.47, "hand_min_y": 0.80,
                "arm_inner_x": [(1.2, 0.21), (0.0, 0.24)], "arm_max_y": 1.47,
                "back_items": None},
    # 0.5.0-D8: Kikoru (modelo do Miguel, 1,57 m: Miguel 2026-10-09): cabelo com marias-chiquinhas largas (|x| ate
    # 0,37) acima do ombro (1,20): acima de arm_max_y nada e braco; cinto e bolsas (ate |x| 0,17) entre 0,74 e 0,83
    # ficam no tronco; maos ate 0,69; o coldre da coxa direita fica na perna. Cortes medidos a 1,6 m x 1,57/1,6.
    "kikoru": {"stem": "kikoru", "hip_y": 0.746, "neck_y": 1.187, "hand_min_y": 0.687,
               "arm_inner_x": [(0.932, 0.152), (0.0, 0.201)], "arm_max_y": 1.192,
               "back_items": None,
               "leg_pivot_center": True},
    # 0.7-C: Reno (1,70 m), Mina (1,65 m) e Narumi (1,78 m), modelos estilo Minecraft do Miguel. Cortes medidos pelo
    # vao entre braco e tronco faixa a faixa (meshy_convert, frente -Z): acima do ombro nada e braco; o rabo de cavalo
    # da Mina cai atras da cabeca ate ~1,0 m e fica no tronco.
    "reno": {"stem": "reno", "hip_y": 0.74, "neck_y": 1.27, "hand_min_y": 0.63,
             "arm_inner_x": [(1.0, 0.175), (0.0, 0.205)], "arm_max_y": 1.27, "back_items": None,
             "leg_pivot_center": True},
    "mina": {"stem": "mina", "hip_y": 0.71, "neck_y": 1.22, "hand_min_y": 0.63,
             "arm_inner_x": [(0.88, 0.16), (0.0, 0.195)], "arm_max_y": 1.22, "back_items": None,
             "leg_pivot_center": True},
    "narumi": {"stem": "narumi", "hip_y": 0.78, "neck_y": 1.32, "hand_min_y": 0.68,
               "arm_inner_x": [(0.93, 0.165), (0.0, 0.205)], "arm_max_y": 1.32, "back_items": None,
               "leg_pivot_center": True},
    # 0.7-D: Kikoru com a arma numerada 4 (1,57 m, casaco longo ate o chao: cortado pelas pernas no meio, fica um pouco
    # mais duro). As 4 asas em X vieram num GLB separado: viram os ossos wing_left/wing_right presos nas costas
    # (attachments), achatadas em Z (as asas da frente nao atravessam os bracos) e com a textura num atlas lado a lado.
    "kikoru_no4": {"stem": "kikoru_no4", "hip_y": 0.746, "neck_y": 1.187, "hand_min_y": 0.66,
                   "arm_inner_x": [(0.932, 0.152), (0.0, 0.201)], "arm_max_y": 1.192, "back_items": None,
                   "leg_pivot_center": True,
                   "attachments": [{"stem": "kikoru_no4_wings", "anchor": [0.0, 1.08, 0.2], "z_scale": 0.45,
                                    "bones": ["wing_left", "wing_right"], "parent": "body"}]},
    # 0.7-D: Reno com a arma numerada 6 (traje azul, 1,70 m): mesmas medidas do Reno normal.
    "reno_no6": {"stem": "reno_no6", "hip_y": 0.74, "neck_y": 1.27, "hand_min_y": 0.63,
                 "arm_inner_x": [(1.0, 0.175), (0.0, 0.205)], "arm_max_y": 1.27, "back_items": None,
                 "leg_pivot_center": True},
    # 0.7-F: Kafka Hibino (forma humana, 1,81 m, traje da Forca de Defesa) e Kaiju No. 8 (2 m, musculoso, cranio
    # branco). Bracos abertos em A: vao entre braco e tronco medido faixa a faixa (meshy_convert, frente -Z).
    "kafka": {"stem": "kafka", "hip_y": 0.80, "neck_y": 1.43, "hand_min_y": 0.74,
              "arm_inner_x": [(1.0, 0.17), (0.0, 0.205)], "arm_max_y": 1.43, "back_items": None,
              "leg_pivot_center": True},
    "kaiju_no8": {"stem": "kaiju_no8", "hip_y": 0.82, "neck_y": 1.47, "hand_min_y": 0.68,
                  "arm_inner_x": [(1.12, 0.22), (0.95, 0.24), (0.0, 0.275)], "arm_max_y": 1.47, "back_items": None,
                  "leg_pivot_center": True},
    # 0.6-F: Hoshina com o traje numerado 10: a cauda do No. 10 sai do quadril esquerdo, passa por baixo da mao,
    # sobe pelas costas e faz um arco por cima da cabeca ate a ponta na frente-direita. Separada pela superficie
    # (sementes da cauda contra sementes do resto do corpo) e dividida em 4 ossos pelo comprimento da linha.
    "hoshina_no10": {"stem": "hoshina_no10", "hip_y": 0.70, "neck_y": 1.31, "hand_min_y": 0.60,
                     "arm_inner_x": [(1.0, 0.205), (0.0, 0.26)], "back_items": None,
                     "tail": [[-0.3, 0.72, -0.12], [-0.45, 0.63, 0.2], [-0.5, 1.1, 0.38], [-0.35, 1.6, 0.25],
                              [-0.1, 1.8, 0.0], [0.32, 1.6, -0.42]],
                     "head_box": [-0.19, 0.23, -0.37, 0.08, 1.64]},
}
# Esqueleto do resto do corpo (sementes que disputam a pele com as da cauda): (inicio, fim).
BODY_SEEDS = [([0, 0.9, 0], [0, 1.25, 0]), ([0, 1.25, 0], [0, 1.7, 0]), ([0, 1.3, 0], [0.33, 1.3, 0]),
              ([0, 1.3, 0], [-0.33, 1.3, 0]), ([0.36, 1.3, 0], [0.38, 0.66, 0]), ([-0.36, 1.3, 0], [-0.4, 0.66, -0.05]),
              ([0.12, 0.9, 0], [0.12, 0.05, 0]), ([-0.12, 0.9, 0], [-0.12, 0.05, 0])]
TAIL = None
TAIL_BONES = 4
# Caixa da cabeca (x minimo, x maximo, z minimo, z maximo, topo; m) nos modelos com cauda.
HEAD_BOX = None
# Acima desta altura nada e braco (capacete/cabelo mais largos que o pescoco); None = sem limite.
ARM_MAX_Y = None
# Pivo da perna no meio da largura dela (Kikoru: o topo das pernas e torto e deixava os pivos assimetricos).
LEG_PIVOT_CENTER = False


def use_species(name):
    """Ativa as medidas da especie (variaveis do modulo usadas pelas funcoes abaixo)."""
    global NAME, STEM, SOURCE, HIP_Y, NECK_Y, HAND_MIN_Y, ARM_INNER_X, BACK_ITEMS, TAIL, HEAD_BOX, ARM_MAX_Y
    global LEG_PIVOT_CENTER, ATTACHMENTS
    spec = SPECIES[name]
    ATTACHMENTS = spec.get("attachments", [])
    NAME, STEM = name, spec["stem"]
    SOURCE = ROOT / "tools/art/converted" / STEM
    HIP_Y, NECK_Y, HAND_MIN_Y = spec["hip_y"], spec["neck_y"], spec["hand_min_y"]
    ARM_INNER_X, BACK_ITEMS = spec["arm_inner_x"], spec["back_items"]
    TAIL = [np.array(point, dtype=float) for point in spec["tail"]] if spec.get("tail") else None
    HEAD_BOX = spec.get("head_box")
    ARM_MAX_Y = spec.get("arm_max_y")
    LEG_PIVOT_CENTER = spec.get("leg_pivot_center", False)


# Pedaco isolado com ate tantas faces vai para o osso vizinho (absorb_fragments).
FRAGMENT_FACES = 40
# Brilho maximo do "preto do macacao" usado para tampar buracos (cap_holes).
DARK_LEVEL = 30


ATTACHMENTS = []


def load_attachments():
    """0.7-D: pecas de outro GLB presas ao corpo (asas da Numbers 4). Cada uma e centrada no "anchor" (costas), achatada
    em Z por "z_scale" e dividida pelo lado (X negativo = primeiro osso, "left"). Devolve {osso: malha} e os pivos."""
    meshes, pivots, parents = {}, {}, {}
    for spec in ATTACHMENTS:
        source = ROOT / "tools/art/converted" / spec["stem"]
        vertices, uvs, normals, faces = read_obj(source / f"{spec['stem']}.obj")
        center = (vertices.min(axis=0) + vertices.max(axis=0)) / 2
        vertices = (vertices - center) * np.array([1.0, 1.0, spec.get("z_scale", 1.0)]) + np.array(spec["anchor"])
        c = vertices[faces].mean(axis=1)
        left, right = spec["bones"]
        for bone, sel in ((left, c[:, 0] <= spec["anchor"][0]), (right, c[:, 0] > spec["anchor"][0])):
            meshes[bone] = (vertices, uvs, normals, faces[sel])
            pivots[bone] = np.array(spec["anchor"], dtype=float)
            parents[bone] = spec["parent"]
    return meshes, pivots, parents


def padded_texture(source, stem):
    image = Image.open(source / f"{stem}.png").convert("RGBA")
    mask = uv_mask([source / f"{stem}.obj"], image.width, image.height)
    padded = dilate(np.asarray(image)[..., :3], mask, DILATE_STEPS)
    return Image.fromarray(np.clip(padded, 0, 255).astype(np.uint8)).convert("RGBA") \
        .resize((TEXTURE_SIZE, TEXTURE_SIZE), Image.Resampling.BOX)


def load_mesh():
    return read_obj(SOURCE / f"{STEM}.obj")


def split(mesh):
    """Divide a malha inteira pela forma (modelo "estilo Minecraft" de 2026-10-06, segunda versao com 7 mil
    triangulos: bracos retos colados no tronco, borda em |x| ~0,21 m). Bracos: fora do tronco e acima da ponta dos dedos; cabeca: acima do pescoco;
    corpo: acima do quadril; abaixo, perna do lado (X negativo = "left", convencao do meshy_convert)."""
    vertices, uvs, normals, faces = mesh
    c = vertices[faces].mean(axis=1)
    inner = np.select([c[:, 1] >= y for y, _ in ARM_INNER_X], [x for _, x in ARM_INNER_X])
    arm = (c[:, 1] >= HAND_MIN_Y) & (np.abs(c[:, 0]) > inner)
    if ARM_MAX_Y is not None:
        arm &= c[:, 1] < ARM_MAX_Y
    back = back_items(vertices, faces) if BACK_ITEMS else np.zeros(len(faces), dtype=bool)
    # Bainhas nas costas: nao sao braco nem cabeca (iriam girar junto com eles).
    arm &= ~back
    tail = tail_faces(vertices, faces) if TAIL else np.zeros(len(faces), dtype=bool)
    if TAIL:
        # Acima do pescoco, fora da caixa da cabeca (cubo de poucos triangulos, medido na vista em grade), so pode
        # ser a cauda: o arco passa por cima da cabeca.
        x0, x1, z0, z1, top = HEAD_BOX
        in_head = (c[:, 0] > x0) & (c[:, 0] < x1) & (c[:, 2] > z0) & (c[:, 2] < z1) & (c[:, 1] < top)
        tail = np.where(c[:, 1] > NECK_Y, ~in_head, tail)
    arm &= ~tail
    head = ~arm & ~back & ~tail & (c[:, 1] > NECK_Y)
    # Bainhas ficam no tronco inteiras, mesmo a ponta abaixo do quadril (senao iriam com a perna).
    body = (~arm & ~head & ~tail & (c[:, 1] > HIP_Y)) | back
    leg = ~arm & ~head & ~body & ~tail
    groups = {"head": head, "body": body,
              "arm_left": arm & (c[:, 0] < 0), "arm_right": arm & (c[:, 0] > 0),
              "leg_left": leg & (c[:, 0] <= 0), "leg_right": leg & (c[:, 0] > 0)}
    if TAIL:
        part = np.array([min(TAIL_BONES, int(polyline_param(point, TAIL) * TAIL_BONES) + 1) for point in c])
        for number in range(1, TAIL_BONES + 1):
            groups[f"tail_{number}"] = tail & (part == number)
    labels = np.empty(len(faces), dtype=object)
    for name, sel in groups.items():
        labels[sel] = name
    labels = absorb_fragments(vertices, faces, labels)
    vertices, uvs, normals, faces, labels = cap_holes(vertices, uvs, normals, faces, labels,
                                                      dark_uv(vertices, uvs, faces, labels))
    return {name: (vertices, uvs, normals, faces[labels == name]) for name in groups}


def tail_faces(vertices, faces):
    """Faces da cauda: cada vertice vai para a semente mais proxima andando pela pele (Dijkstra na malha soldada);
    sementes ao longo da linha da cauda (TAIL) contra sementes do esqueleto do corpo (BODY_SEEDS)."""
    from scipy.sparse import coo_matrix
    from scipy.sparse.csgraph import dijkstra
    _, welded = np.unique(np.round(vertices, 5), axis=0, return_inverse=True)
    welded = welded.reshape(-1)
    count = welded.max() + 1
    position = np.zeros((count, 3))
    position[welded] = vertices
    wf = welded[faces]
    edges = np.vstack([wf[:, [0, 1]], wf[:, [1, 2]], wf[:, [2, 0]]])
    length = np.linalg.norm(position[edges[:, 0]] - position[edges[:, 1]], axis=1) + 1e-6
    graph = coo_matrix((length, (edges[:, 0], edges[:, 1])), shape=(count, count)).tocsr()
    seeds, owner = [], []
    segments = [(TAIL[i], TAIL[i + 1], True) for i in range(len(TAIL) - 1)]
    segments += [(np.array(a, dtype=float), np.array(b, dtype=float), False) for a, b in BODY_SEEDS]
    for start, end, is_tail in segments:
        for t in np.linspace(0, 1, 8):
            seeds.append(int(np.argmin(np.linalg.norm(position - (start + (end - start) * t), axis=1))))
            owner.append(is_tail)
    _, _, sources = dijkstra(graph, directed=False, indices=sorted(set(seeds)), min_only=True,
                             return_predecessors=True)
    owner_of = dict(zip(seeds, owner))
    vertex_tail = np.array([owner_of.get(int(source), False) for source in sources])
    votes = vertex_tail[wf].sum(axis=1)
    return votes >= 2


def polyline_param(point, line):
    """Posicao (0 = inicio, 1 = fim) do ponto mais proximo na linha, pelo comprimento."""
    lengths = [np.linalg.norm(line[i + 1] - line[i]) for i in range(len(line) - 1)]
    total, walked, best = sum(lengths), 0.0, (np.inf, 0.0)
    for i, segment in enumerate(lengths):
        direction = line[i + 1] - line[i]
        t = np.clip(np.dot(point - line[i], direction) / segment ** 2, 0, 1)
        distance = np.linalg.norm(point - (line[i] + direction * t))
        if distance < best[0]:
            best = (distance, (walked + t * segment) / total)
        walked += segment
    return best[1]


def polyline_point(line, fraction):
    lengths = [np.linalg.norm(line[i + 1] - line[i]) for i in range(len(line) - 1)]
    target = fraction * sum(lengths)
    for i, segment in enumerate(lengths):
        if target <= segment:
            return line[i] + (line[i + 1] - line[i]) * (target / segment)
        target -= segment
    return line[-1]


def back_items(vertices, faces):
    """Faces das pecas nas costas (bainhas): componentes conexos grandes, separados do corpo, atras de z_min; ou,
    com "region", tudo que esta do lado de fora do tronco (x < x_max) e atras de z_min (malha soldada)."""
    if "region" in BACK_ITEMS:
        center = vertices[faces].mean(axis=1)
        region = BACK_ITEMS["region"]
        return (center[:, 0] < region["x_max"]) & (center[:, 2] > region["z_min"])
    from scipy.sparse import coo_matrix
    from scipy.sparse.csgraph import connected_components
    _, welded = np.unique(np.round(vertices, 5), axis=0, return_inverse=True)
    wf = welded.reshape(-1)[faces]
    rows = np.repeat(np.arange(len(faces)), 3)
    graph = coo_matrix((np.ones(len(rows)), (rows, wf.reshape(-1) + len(faces))),
                       shape=(len(faces) + welded.max() + 1,) * 2)
    _, component = connected_components(graph, directed=False)
    component = component[:len(faces)]
    sizes = np.bincount(component)
    z_min = vertices[faces][:, :, 2].min(axis=1)
    back = np.zeros(len(faces), dtype=bool)
    for comp in np.where(sizes >= BACK_ITEMS["min_faces"])[0]:
        members = component == comp
        if sizes[comp] < sizes.max() and z_min[members].min() > BACK_ITEMS["z_min"]:
            back |= members
    return back


def dark_uv(vertices, uvs, faces, labels):
    """Coordenada de textura de um ponto preto do macacao (centro de uma face escura do tronco)."""
    image = np.asarray(Image.open(SOURCE / f"{STEM}.png").convert("RGB")).astype(float)
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


def reload(pose, length):
    """0.5.0-D: recarga por etapas (tempos do weapon_profile): abaixa a arma, a mao esquerda vai ao pente na cintura
    (soltar), volta com o pente (colocar) e puxa o ferrolho (engatilhar)."""
    right, left = POSES[pose]
    low = add(right, [18, 0, 0])
    pouch = [-10, 20, -20]
    t1, t2, t3 = length * 0.3, length * 0.6, length * 0.82
    return {"animation_length": round(length, 2), "bones": {
        "arm_right": {"rotation": kf((0, right), (t1, low), (t3, low), (length, right))},
        "arm_left": {"rotation": kf((0, left), (t1 * 0.6, pouch), (t1, pouch), (t2, add(left, [10, -10, 0])),
                                    (t3, add(left, [-15, 15, 0])), (length, left))},
        "head": {"rotation": kf((0, [0, 0, 0]), (t1, [18, 0, 0]), (t3, [12, 0, 0]), (length, [0, 0, 0]))},
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
        # Tempos do weapon_profile/rifle.json (32 ticks) e pistol.json (22 ticks).
        p + "action.reload_rifle": reload("rifle", 32 / 20),
        p + "action.reload_pistol": reload("pistol", 22 / 20),
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
    use_species(sys.argv[1] if len(sys.argv) > 1 else "soldier")
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
        # 0.6-D: mao esquerda (segunda arma do soldado especial; o soldado comum usa a faca de apoio nela).
        "item_left": hand_point(meshes["arm_left"]),
    }
    if LEG_PIVOT_CENTER:
        for leg in ("leg_left", "leg_right"):
            v, _, _, f = meshes[leg]
            used = v[np.unique(f)]
            pivots[leg][0] = (used[:, 0].min() + used[:, 0].max()) / 2
    parents = {"root": None, "body": "root", "head": "body", "arm_left": "body", "arm_right": "body",
               "leg_left": "root", "leg_right": "root", "item_right": "arm_right", "item_left": "arm_left"}
    if TAIL:
        # 0.6-F: cauda em 4 ossos encadeados (pivo no comeco de cada pedaco). Sem espada na ponta (Miguel).
        for number in range(1, TAIL_BONES + 1):
            pivots[f"tail_{number}"] = polyline_point(TAIL, (number - 1) / TAIL_BONES)
            parents[f"tail_{number}"] = "body" if number == 1 else f"tail_{number - 1}"
    if ATTACHMENTS:
        # Atlas: textura do personagem na metade esquerda, das pecas presas na direita (o render usa uma textura so).
        extra, extra_pivots, extra_parents = load_attachments()
        meshes = {bone: (v, t * np.array([0.5, 1.0]), n, f) for bone, (v, t, n, f) in meshes.items()}
        for bone, (v, t, n, f) in extra.items():
            meshes[bone] = (v, t * np.array([0.5, 1.0]) + np.array([0.5, 0.0]), n, f)
        pivots.update(extra_pivots)
        parents.update(extra_parents)
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
    texture = padded_texture(SOURCE, STEM)
    if ATTACHMENTS:
        atlas = Image.new("RGBA", (TEXTURE_SIZE * 2, TEXTURE_SIZE))
        atlas.paste(texture, (0, 0))
        stem = ATTACHMENTS[0]["stem"]
        atlas.paste(padded_texture(ROOT / "tools/art/converted" / stem, stem), (TEXTURE_SIZE, 0))
        texture = atlas
    texture.save(ASSETS / f"textures/entity/{NAME}.png")
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
