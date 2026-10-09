#!/usr/bin/env python3
"""Rigging automatico da Trichonephila do Meshy (malha presa aos ossos da GeckoLib).

Entrada: tools/art/converted/trichonephila/trichonephila.obj + .png (gerados por meshy_convert.py), em metros,
pes em Y = 0, frente para -Z. Saida:
  - assets/kn8/meshes/trichonephila/<osso>.obj : pedaco da malha de cada osso (coordenadas do modelo, em blocos);
  - assets/kn8/meshes/trichonephila.json       : indice (osso -> malha), lido pelo MeshRenderLayer;
  - assets/kn8/textures/entity/trichonephila.png : textura do Meshy (a malha usa a textura e o render type do
    proprio modelo GeckoLib, para desenhar no mesmo buffer);
  - assets/kn8/geo/entity/trichonephila.geo.json : esqueleto SO de ossos (sem cubos), mesmos nomes de antes;
  - assets/kn8/animations/entity/trichonephila.animation.json : as animacoes de build_trichonephila.py.

Divisao (modelo de 8 patas, 2026-10-06): cefalotorax = body (frente = head, presas = fang_left/right), abdomen
atras, patas = o resto; cada pata e achada pela parte distante do centro e as faces perto do quadril vao para a
pata cuja linha passa mais perto (da frente para tras = leg_<lado>_0..3). Pivo de cada pata = ponto da pata
mais perto do centro (quadril). A GeckoLib inverte o X do pivo ao carregar: os pivos sao gravados com X negado.
Substitui a arte em cubos de build_trichonephila.py (que nao deve mais ser rodado para esta especie).
0.6: uma tabela de medidas por especie (SPECIES): a Trichonephila Honju (modelo do Miguel, 8 m) usa o mesmo rig.
Uso: python3 tools/art/rig_trichonephila_mesh.py [especie]   (padrao trichonephila; requer numpy, pillow e scipy)
"""
import json
import math
import shutil
import sys
from pathlib import Path

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).parent))
from pad_texture import DILATE_STEPS, dilate, uv_mask  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/kn8"
PX = 16.0
# Medidas por especie (metros, frente -Z). Os valores ativos ficam nas variaveis do modulo (use_species).
SPECIES = {
    # Modelo "estilo Minecraft" de 2026-10-06 (8 patas, corpo baixo): cefalotorax |x| < 0,5 na frente de
    # ABDOMEN_START_Z; abdomen (caixa listrada) atras. 0.2: CORE_HALF_WIDTH 0,5 (era 0,42): a raiz das patas colada
    # na lateral do cefalotorax ficava na pata e, no passo, girava para fora do corpo como lascas.
    "trichonephila": {"center_z": -0.5, "core_half_width": 0.5, "abdomen_start_z": 0.3, "abdomen_half_width": 0.7,
                      "abdomen_min_y": 0.45, "head_z": -1.2, "fang_z": -1.45, "fang_max_y": 0.6,
                      "leg_far_radius": 1.3, "leg_max_y": 1.7, "bounds": [7, 4]},
    # 0.6: Trichonephila Honju (Tecedeira Abissal, 8 m): cabeca humanoide alta na frente, cefalotorax de -2 a 0,
    # abdomen de 0 a 3,5 bem acima do chao; patas da frente longas. Sem queliceras separadas (o rosto e a cabeca).
    "trichonephila_honju": {"center_z": -1.0, "core_half_width": 1.2, "abdomen_start_z": 0.1,
                            "abdomen_half_width": 1.5, "abdomen_min_y": 1.15, "head_z": -1.75, "fang_z": -99.0,
                            "fang_max_y": 0.0, "leg_far_radius": 1.9, "leg_max_y": 3.2, "bounds": [10, 6],
                            # Rig por caminhos (split_paths): o corte por planos misturava patas que passam por cima
                            # e por baixo do corpo. Eixo do corpo (pontos por osso) e pes = pontos baixos afastados.
                            "paths": {"regions": {"head": [[-0.7, 2.9, -2.8], [0.7, 4.2, -1.8]],
                                                  "body": [[-0.75, 1.7, -1.8], [0.75, 3.2, 0.0]],
                                                  "abdomen": [[-1.0, 1.3, 0.3], [1.0, 3.4, 3.8]]},
                                      "axis": {"head": [[0.0, 3.4, -2.3], [0.0, 3.0, -2.0]],
                                               "body": [[0.0, 2.6, -1.4], [0.0, 2.5, -0.6], [0.0, 2.3, 0.0]],
                                               "abdomen": [[0.0, 2.4, 0.8], [0.0, 2.4, 1.8], [0.0, 2.0, 2.8]]},
                                      # Pontas das 8 patas medidas nas vistas (a deteccao automatica juntava patas que
                                      # se encostam). leg_right_3 e um pedaco solto da malha (vira osso inteiro).
                                      "tips": {"leg_left_0": [-1.4, 0.0, -3.8], "leg_left_1": [-3.1, 0.0, -0.4],
                                               # Joelhos altos (a pata sobe e desce): ponto extra da mesma pata.
                                               "leg_left_1_knee": [-2.46, 3.19, -1.06],
                                               "leg_left_2": [-2.3, 0.0, 1.5], "leg_left_3": [-3.4, 0.0, 3.5],
                                               "leg_right_0": [2.1, 0.0, -4.0], "leg_right_1": [2.5, 1.9, -1.8],
                                               "leg_right_2": [3.4, 0.4, 1.4], "leg_right_3": [1.6, 0.4, 3.0]}}},
    # 0.7-A: Camponotus (formiga Yoju do Miguel, 6 m de comprimento; a revivida e o mesmo corpo azul). 6 patas:
    # leg_<lado>_0..2 (frente -> tras); leg_<lado>_3 fica sem malha (as animacoes da aranha continuam valendo). A
    # pata de tras esquerda veio dobrada por baixo do abdomen (ponta em x ~0, z 2,4). Sem queliceras separadas: as
    # mandibulas ficam na cabeca.
    "camponotus": {"center_z": -0.5, "core_half_width": 0.45, "abdomen_start_z": 0.4, "abdomen_half_width": 0.6,
                   "abdomen_min_y": 1.0, "head_z": -1.1, "fang_z": -99.0, "fang_max_y": 0.0,
                   "leg_far_radius": 1.0, "leg_max_y": 1.5, "bounds": [7, 4],
                   "paths": {"all_axis_centers": True,
                             "regions": {"head": [[-0.6, 1.0, -2.7], [0.6, 2.9, -1.15]],
                                         "body": [[-0.4, 1.1, -1.0], [0.4, 2.3, 0.3]],
                                         "abdomen": [[-0.55, 0.9, 0.6], [0.55, 2.6, 3.0]]},
                             "axis": {"head": [[0.0, 2.0, -1.9], [0.0, 1.9, -1.3]],
                                      "body": [[0.0, 1.7, -0.8], [0.0, 1.7, -0.2], [0.0, 1.7, 0.3]],
                                      "abdomen": [[0.0, 1.7, 1.0], [0.0, 1.7, 1.9], [0.0, 1.5, 2.7]]},
                             "tips": {"leg_left_0": [-1.03, 0.01, -2.2], "leg_left_1": [-1.5, 0.04, -0.49],
                                      "leg_left_2": [0.14, 0.0, 2.39],
                                      "leg_right_0": [0.54, 0.2, -2.35], "leg_right_1": [1.54, 0.28, -1.17],
                                      "leg_right_2": [1.43, 0.02, 0.92]}}},
}
SPECIES["camponotus_reborn"] = SPECIES["camponotus"]
# 0.7-E: No. 9 fundido a formiga (modelo do Miguel, 6 m): torso do No. 9 em pe sobre o torax da formiga, no osso
# do corpo (sobe ate 3,9 m). Oito pontas de pata no chao (as duas de tras ficam sob o abdomen), medidas na malha.
SPECIES["kaiju_no9_camponotus"] = {
    "center_z": -0.5, "core_half_width": 0.6, "abdomen_start_z": 0.2, "abdomen_half_width": 0.9,
    "abdomen_min_y": 0.8, "head_z": -1.4, "fang_z": -99.0, "fang_max_y": 0.0, "leg_far_radius": 1.0,
    "leg_max_y": 1.6, "bounds": [7, 5],
    "paths": {"all_axis_centers": True,
              "regions": {"head": [[-0.75, 0.8, -2.8], [0.75, 2.3, -1.4]],
                          "body": [[-0.7, 1.0, -1.4], [0.7, 4.0, 0.1]],
                          "abdomen": [[-1.0, 1.1, 0.2], [1.0, 2.7, 3.0]]},
              "axis": {"head": [[0.0, 1.7, -2.2], [0.0, 1.8, -1.6]],
                       "body": [[0.0, 1.6, -1.2], [0.0, 1.7, -0.6], [0.0, 2.6, -0.9], [0.0, 3.4, -0.9]],
                       "abdomen": [[0.0, 1.7, 0.6], [0.0, 1.7, 1.6], [0.0, 1.5, 2.6]]},
              "tips": {"leg_left_0": [-1.57, 0.05, -2.05], "leg_left_1": [-1.66, 0.03, -0.69],
                       "leg_left_2": [-1.77, 0.03, 1.79], "leg_left_3": [-0.08, 0.12, 2.24],
                       "leg_right_0": [0.45, 0.08, -2.36], "leg_right_1": [1.74, 0.0, -0.96],
                       "leg_right_2": [1.79, 0.06, 1.19], "leg_right_3": [0.84, 0.17, 1.98]}}}
# 0.7-B: kaiju cogumelo do Miguel. Sem abdomen (o osso fica sem malha, pivo no corpo): chapeu com a boca = head,
# caule = body, patas/raizes = leg_<lado>_<n> (frente -> tras). abdomen_start_z alto: o corpo inteiro usa
# core_half_width. Phaneroplasmodium (Yoju, 5 m): 6 pontas de pata no chao (as outras duas ficam no ar e vao para a
# pata mais perto). Myxogasterocarp (Honju, 9 m): chapeus empilhados, raizes como patas (6 pontas no chao).
SPECIES["phaneroplasmodium"] = {
    "center_z": 0.0, "core_half_width": 1.1, "abdomen_start_z": 99.0, "abdomen_half_width": 1.1,
    "abdomen_min_y": 2.2, "head_z": -0.8, "fang_z": -99.0, "fang_max_y": 0.0, "leg_far_radius": 1.8,
    "leg_max_y": 2.5, "bounds": [7, 6],
    "paths": {"regions": {"head": [[-2.3, 3.7, -2.2], [2.3, 5.1, 2.2]],
                          "body": [[-0.9, 1.7, -0.9], [0.9, 3.4, 0.9]]},
              "axis": {"head": [[0.0, 4.3, 0.0], [0.0, 3.8, -0.9]],
                       "body": [[0.0, 2.2, 0.0], [0.0, 2.8, 0.0], [0.0, 3.3, 0.0]]},
              "tips": {"leg_left_0": [-1.43, 0.06, -1.61], "leg_left_1": [-2.16, 0.05, -0.48],
                       "leg_left_2": [-1.66, 0.01, 1.29], "leg_right_0": [1.65, 0.04, -1.76],
                       "leg_right_1": [2.14, 0.01, 0.48], "leg_right_2": [1.5, 0.01, 1.87]}}}
SPECIES["myxogasterocarp"] = {
    "center_z": 0.0, "core_half_width": 1.7, "abdomen_start_z": 99.0, "abdomen_half_width": 1.7,
    "abdomen_min_y": 4.0, "head_z": -1.5, "fang_z": -99.0, "fang_max_y": 0.0, "leg_far_radius": 2.8,
    "leg_max_y": 3.0, "bounds": [11, 10],
    "paths": {"regions": {"head": [[-3.5, 6.7, -3.5], [3.5, 9.1, 3.5]],
                          "body": [[-3.6, 3.3, -3.6], [3.6, 6.2, 3.6]]},
              "axis": {"head": [[0.0, 7.6, 0.0], [0.0, 7.0, -1.5]],
                       "body": [[0.0, 3.5, 0.0], [0.0, 4.6, 0.0], [0.0, 5.6, 0.0]]},
              "tips": {"leg_left_0": [-0.79, 0.04, -4.45], "leg_left_1": [-3.38, 0.03, -0.74],
                       "leg_right_0": [1.79, 0.05, -3.51], "leg_right_1": [3.29, 0.04, -0.11],
                       "leg_right_2": [3.22, 0.06, 3.01], "leg_right_3": [1.9, 0.06, 2.8]}}}
NAME = "trichonephila"
SOURCE = ROOT / "tools/art/converted/trichonephila"
CENTER_Z = CORE_HALF_WIDTH = ABDOMEN_START_Z = ABDOMEN_HALF_WIDTH = ABDOMEN_MIN_Y = 0.0
HEAD_Z = FANG_Z = FANG_MAX_Y = LEG_FAR_RADIUS = LEG_MAX_Y = 0.0
BOUNDS = [7, 4]
# Patas: a parte distante do centro (> LEG_FAR_RADIUS) separa bem cada pata; pedacos vizinhos (distancia
# LEG_LINK) se juntam, e pedacos na mesma direcao (< LEG_MERGE_DEGREES) sao a mesma pata. O resto de cada pata
# (perto do quadril) vai para a pata cuja linha (vista de cima) passa mais perto.
LEG_LINK = 0.12
LEG_MERGE_DEGREES = 12.0
LEG_MIN_FACES = 80
HIP_QUANTILE = 0.1


def use_species(name):
    """Ativa as medidas da especie (variaveis do modulo usadas pelas funcoes abaixo)."""
    global NAME, SOURCE, CENTER_Z, CORE_HALF_WIDTH, ABDOMEN_START_Z, ABDOMEN_HALF_WIDTH, ABDOMEN_MIN_Y, HEAD_Z
    global FANG_Z, FANG_MAX_Y, LEG_FAR_RADIUS, LEG_MAX_Y, BOUNDS
    spec = SPECIES[name]
    NAME, SOURCE = name, ROOT / "tools/art/converted" / name
    CENTER_Z, CORE_HALF_WIDTH = spec["center_z"], spec["core_half_width"]
    ABDOMEN_START_Z, ABDOMEN_HALF_WIDTH, ABDOMEN_MIN_Y = (spec["abdomen_start_z"], spec["abdomen_half_width"],
                                                          spec["abdomen_min_y"])
    HEAD_Z, FANG_Z, FANG_MAX_Y = spec["head_z"], spec["fang_z"], spec["fang_max_y"]
    LEG_FAR_RADIUS, LEG_MAX_Y, BOUNDS = spec["leg_far_radius"], spec["leg_max_y"], spec["bounds"]


use_species("trichonephila")


def read_obj(path):
    vertices, uvs, normals, faces = [], [], [], []
    for line in path.read_text(encoding="utf-8").splitlines():
        parts = line.split()
        if not parts:
            continue
        if parts[0] == "v":
            vertices.append([float(x) for x in parts[1:4]])
        elif parts[0] == "vt":
            uvs.append([float(x) for x in parts[1:3]])
        elif parts[0] == "vn":
            normals.append([float(x) for x in parts[1:4]])
        elif parts[0] == "f":
            faces.append([int(token.split("/")[0]) - 1 for token in parts[1:4]])
    return np.array(vertices), np.array(uvs), np.array(normals), np.array(faces)


def write_obj(path, vertices, uvs, normals, faces):
    used = np.unique(faces)
    remap = -np.ones(len(vertices), dtype=int)
    remap[used] = np.arange(len(used))
    lines = [f"o {path.stem}"]
    lines += [f"v {x:.5f} {y:.5f} {z:.5f}" for x, y, z in vertices[used]]
    lines += [f"vt {u:.5f} {v:.5f}" for u, v in uvs[used]]
    lines += [f"vn {x:.4f} {y:.4f} {z:.4f}" for x, y, z in normals[used]]
    for a, b, c in remap[faces] + 1:
        lines.append(f"f {a}/{a}/{a} {b}/{b}/{b} {c}/{c}/{c}")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def leg_lines(centroids, leg, side_mask):
    """Uma linha (vista de cima) por pata deste lado, da frente para tras."""
    from scipy.cluster.hierarchy import fcluster, linkage
    x, z = centroids[:, 0], centroids[:, 2]
    radius = np.hypot(x, z - CENTER_Z)
    angle = np.degrees(np.arctan2(z - CENTER_Z, np.abs(x)))
    far = np.where(leg & side_mask & (radius > LEG_FAR_RADIUS))[0]
    labels = fcluster(linkage(centroids[far], "single"), LEG_LINK, "distance")
    pieces = [far[labels == k] for k in np.unique(labels)]
    pieces = sorted((g for g in pieces if len(g) > LEG_MIN_FACES // 4), key=lambda g: angle[g].mean())
    legs = []
    for piece in pieces:
        if legs and angle[piece].mean() - angle[legs[-1]].mean() < LEG_MERGE_DEGREES:
            legs[-1] = np.concatenate([legs[-1], piece])
        else:
            legs.append(piece)
    lines = []
    for group in (g for g in legs if len(g) > LEG_MIN_FACES):
        points = centroids[group][:, [0, 2]]
        middle = points.mean(axis=0)
        direction = np.linalg.svd(points - middle)[2][0]
        lines.append((middle, direction))
    return lines


def classify(vertices, faces):
    centroids = vertices[faces].mean(axis=1)
    x, y, z = centroids[:, 0], centroids[:, 1], centroids[:, 2]
    core = (np.abs(x) < CORE_HALF_WIDTH) & (z < ABDOMEN_START_Z)
    abdomen = (np.abs(x) < ABDOMEN_HALF_WIDTH) & (z >= ABDOMEN_START_Z) & (y > ABDOMEN_MIN_Y)
    leg = ~(core | abdomen)
    labels = np.array(["body"] * len(faces), dtype=object)
    labels[abdomen] = "abdomen"
    labels[core & (z < HEAD_Z)] = "head"
    fang = core & (z < FANG_Z) & (y < FANG_MAX_Y)
    labels[fang & (x <= 0)] = "fang_left"
    labels[fang & (x > 0)] = "fang_right"
    for side, side_mask in (("right", x > 0), ("left", x <= 0)):
        lines = leg_lines(centroids, leg, side_mask)
        if len(lines) != 4:
            print(f"AVISO: {len(lines)} patas do lado {side} (esperado 4); confira a vista de cima")
        members = np.where(leg & side_mask)[0]
        distance = np.stack([np.abs((centroids[members][:, 0] - m[0]) * d[1] - (centroids[members][:, 2] - m[1]) * d[0])
                             for m, d in lines], axis=1)
        nearest = np.argmin(distance, axis=1)
        for number in range(len(lines)):
            labels[members[nearest == number]] = f"leg_{side}_{number}"
    return labels


def split_paths(vertices, faces, cfg):
    """0.6: rig por caminhos na superficie (aranhas de patas que cruzam o corpo). De cada pe, o caminho mais curto
    pela malha ate o centro do corpo sobe pela pata; os vertices do caminho ate ele entrar no corpo (largura do
    cefalotorax/abdomen) sao sementes da pata. O eixo do corpo da as sementes de cabeca, corpo e abdomen. Cada vertice
    vai para a semente mais perto pela superficie (Dijkstra); a face, para a maioria dos 3 vertices. Devolve os
    rotulos e o quadril (ultima semente) de cada pata."""
    from scipy.sparse import coo_matrix
    from scipy.sparse.csgraph import dijkstra
    from rig_primigenius_mesh import weld
    welded = weld(vertices)
    count = welded.max() + 1
    position = np.zeros((count, 3))
    position[welded] = vertices
    wf = welded[faces]
    edges = np.vstack([wf[:, [0, 1]], wf[:, [1, 2]], wf[:, [2, 0]]])
    length = np.linalg.norm(position[edges[:, 0]] - position[edges[:, 1]], axis=1) + 1e-6
    graph = coo_matrix((length, (edges[:, 0], edges[:, 1])), shape=(count, count)).tocsr()

    def nearest(point):
        return int(np.argmin(np.linalg.norm(position - np.array(point), axis=1)))

    seeds, owners = [], []
    for bone, points in cfg["axis"].items():
        for point in points:
            seeds.append(nearest(point))
            owners.append(bone)
    # Miolo de cada parte do corpo inteiro como semente (so pontos no eixo deixavam as patas comerem o torax).
    for bone, (low, high) in cfg.get("regions", {}).items():
        inside = np.where(np.all((position >= np.array(low)) & (position <= np.array(high)), axis=1))[0]
        seeds.extend(int(i) for i in inside)
        owners.extend([bone] * len(inside))
    center = nearest(cfg["axis"]["body"][len(cfg["axis"]["body"]) // 2])
    if cfg.get("all_axis_centers"):
        # 0.7-A (formiga): o abdomen e um pedaco solto da malha; o caminho de cada pe vai ate o ponto do eixo mais
        # perto (de qualquer parte do corpo), senao a pata de tras levava o abdomen inteiro.
        centers = sorted({nearest(point) for points in cfg["axis"].values() for point in points})
        distance, predecessors, _ = dijkstra(graph, directed=False, indices=centers, min_only=True,
                                             return_predecessors=True)
    else:
        distance, predecessors = dijkstra(graph, directed=False, indices=center, return_predecessors=True)

    def inside_body(p):
        half = CORE_HALF_WIDTH if p[2] < ABDOMEN_START_Z else ABDOMEN_HALF_WIDTH
        return abs(p[0]) < half and p[1] > ABDOMEN_MIN_Y * 0.6

    # Pontas das patas dadas no SPECIES; de cada uma, o caminho mais curto ate o centro sobe pela pata ate entrar no
    # corpo. Pata que e um pedaco solto da malha (sem caminho) vira o osso inteiro.
    from scipy.sparse.csgraph import connected_components
    _, piece = connected_components(graph, directed=False)
    hips = {}
    for key, point in cfg["tips"].items():
        # "<pata>_knee": joelho alto da pata (sobe ate ele e desce ate o pe); mesmo osso, caminho proprio.
        name = key[:-len("_knee")] if key.endswith("_knee") else key
        foot = nearest(point)
        if not np.isfinite(distance[foot]):
            members = np.where(piece == piece[foot])[0]
            hips[name] = position[members[np.argmin(np.linalg.norm(position[members] - position[center], axis=1))]]
            seeds.extend(int(m) for m in members)
            owners.extend([name] * len(members))
            continue
        path, node = [], foot
        while node != center and node >= 0 and not inside_body(position[node]):
            path.append(node)
            node = predecessors[node]
        if not path:
            print(f"AVISO: {name} sem caminho ate o corpo")
            continue
        if not key.endswith("_knee"):
            hips[name] = position[path[-1]].copy()
        step = max(1, len(path) // 12)
        for node in path[::step]:
            seeds.append(int(node))
            owners.append(name)
    _, _, sources = dijkstra(graph, directed=False, indices=sorted(set(seeds)), min_only=True,
                             return_predecessors=True)
    owner_of = {seed: owner for seed, owner in zip(seeds, owners)}
    vertex_label = np.array([owner_of.get(int(src), "body") for src in sources], dtype=object)
    a, b, c = (vertex_label[wf[:, i]] for i in range(3))
    return np.where(b == c, b, a), hips


def pivot_of(vertices, faces, labels, name):
    """Quadril: media dos 10% de vertices da pata mais perto do centro (vista de cima)."""
    points = vertices[np.unique(faces[labels == name])]
    points = points[points[:, 1] < LEG_MAX_Y]
    distance = np.hypot(points[:, 0], points[:, 2] - CENTER_Z)
    nearest = points[distance <= np.quantile(distance, HIP_QUANTILE)]
    return nearest.mean(axis=0)


def geo_pivot(point):
    """Metros (espaco de render) -> pixels do .geo.json, com o X negado (a GeckoLib inverte ao carregar)."""
    return [round(-point[0] * PX, 3), round(point[1] * PX, 3), round(point[2] * PX, 3)]


def main():
    use_species(sys.argv[1] if len(sys.argv) > 1 else "trichonephila")
    vertices, uvs, normals, faces = read_obj(SOURCE / f"{NAME}.obj")
    paths = SPECIES[NAME].get("paths")
    hips = {}
    if paths:
        labels, hips = split_paths(vertices, faces, paths)
    else:
        labels = classify(vertices, faces)
    bones = {"root": None, "body": "root", "head": "body", "fang_left": "head", "fang_right": "head",
             "abdomen": "body"}
    def center(name):
        return vertices[np.unique(faces[labels == name])].mean(axis=0)

    def top_back(name):
        """Pivo na ligacao com o pai: alto e atras (cabeca/queliceras giram em volta da junta)."""
        points = vertices[np.unique(faces[labels == name])]
        if len(points) == 0:
            # Especie sem queliceras separadas (Honju): o osso existe (animacoes) com o pivo na cabeca.
            return np.array([0.0, center("head")[1], HEAD_Z])
        return np.array([points[:, 0].mean(), points[:, 1].max(), points[:, 2].max()])

    pivots = {"root": np.zeros(3), "body": np.array([0.0, center("body")[1], CENTER_Z]),
              "head": np.array([0.0, center("head")[1], HEAD_Z]),
              "fang_left": top_back("fang_left"), "fang_right": top_back("fang_right"),
              # Especie sem abdomen (cogumelos da 0.7-B): o osso existe para as animacoes, pivo no corpo.
              "abdomen": (np.array([0.0, center("abdomen")[1], ABDOMEN_START_Z]) if np.any(labels == "abdomen")
                          else np.array([0.0, center("body")[1], CENTER_Z]))}
    for side in ("left", "right"):
        for number in range(4):
            name = f"leg_{side}_{number}"
            bones[name] = "body"
            if name in hips:
                pivots[name] = hips[name]
            else:
                pivots[name] = pivot_of(vertices, faces, labels, name) if np.any(labels == name) else pivots["body"]
    # Tampa os cortes das juntas com a cor em volta (a pata girando nao mostra o oco do corpo).
    from rig_soldier_mesh import cap_holes
    vertices, uvs, normals, faces, labels = cap_holes(vertices, uvs, normals, faces, labels)
    mesh_dir = ASSETS / "meshes" / NAME
    if mesh_dir.exists():
        shutil.rmtree(mesh_dir)
    index = {"bones": {}}
    for name in bones:
        part = faces[labels == name]
        if len(part):
            write_obj(mesh_dir / f"{name}.obj", vertices, uvs, normals, part)
            index["bones"][name] = f"kn8:meshes/{NAME}/{name}.obj"
            print(f"{name}: {len(part)} triangulos")
    (ASSETS / "meshes" / f"{NAME}.json").write_text(json.dumps(index, indent=2) + "\n", encoding="utf-8")
    # Textura com borda nas ilhas de UV (sem mipmap no Minecraft a costura pega o fundo).
    image = Image.open(SOURCE / f"{NAME}.png").convert("RGBA")
    mask = uv_mask([SOURCE / f"{NAME}.obj"], image.width, image.height)
    padded = dilate(np.asarray(image)[..., :3], mask, DILATE_STEPS)
    Image.fromarray(np.clip(padded, 0, 255).astype(np.uint8)).convert("RGBA").save(
        ASSETS / f"textures/entity/{NAME}.png")
    stale = ASSETS / f"textures/entity/{NAME}_mesh.png"
    if stale.exists():
        stale.unlink()
    geo_bones = []
    for name, parent in bones.items():
        entry = {"name": name, "pivot": geo_pivot(pivots[name])}
        if parent:
            entry["parent"] = parent
        geo_bones.append(entry)
    geo = {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": f"geometry.{NAME}", "texture_width": 16, "texture_height": 16,
                        "visible_bounds_width": BOUNDS[0], "visible_bounds_height": BOUNDS[1], "visible_bounds_offset": [0, 1, 0]},
        "bones": geo_bones}]}
    (ASSETS / f"geo/entity/{NAME}.geo.json").write_text(json.dumps(geo, indent=2) + "\n", encoding="utf-8")
    sys.path.insert(0, str(Path(__file__).parent))
    import build_trichonephila
    anims = build_trichonephila.animations()
    if NAME != "trichonephila":
        # Mesmos ossos e mesmo passo; so o nome da especie muda nas chaves (<especie>.<camada>.<nome>).
        anims["animations"] = {key.replace("trichonephila.", NAME + ".", 1): value
                               for key, value in anims["animations"].items()}
    (ASSETS / f"animations/entity/{NAME}.animation.json").write_text(json.dumps(anims, indent=2) + "\n",
                                                                     encoding="utf-8")
    print("pivos das patas:", {k: np.round(v, 2).tolist() for k, v in pivots.items() if k.startswith("leg")})


if __name__ == "__main__":
    main()
