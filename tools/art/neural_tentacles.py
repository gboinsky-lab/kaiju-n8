#!/usr/bin/env python3
"""TENTACULOS (Miguel, 2026-11-06: tentaculos do Neural independentes): separa os quatro tentaculos do No. 9 Neural
(kaiju_no9_no15) em ossos proprios, dois pedacos cada (base e ponta), para que se mexam sem os bracos e as pernas.

O modelo do Miguel e de blocos soltos: o corpo e a cabeca sao os dois maiores pedacos ligados da malha e os
tentaculos sao as centenas de pedacos pequenos (raiz nas costas). Pedaco pequeno acima de UPPER_Y = tentaculo de
cima (sai das costas para os lados e curva para a frente ate a mao em |x| 0,75); abaixo, nas costas ou fora do
quadril = tentaculo de baixo (desce das costas ate a altura das maos). Lado pelo X (esquerda = X negativo).
Base e ponta: |x| acima de UPPER_TIP_X (cima) ou altura abaixo de LOWER_TIP_Y (baixo) e ponta.

Rodar DEPOIS do rig_from_skin.py / rig_kaiju_no9_mesh.py desta especie (eles regravam as pecas) e ANTES do
humanoid_anims.py kaiju_no9_no15 (que anima os tentaculos). Uso: python3 tools/art/neural_tentacles.py
"""
import json
import sys
from pathlib import Path

import numpy as np
from scipy.sparse import coo_matrix
from scipy.sparse.csgraph import connected_components

sys.path.insert(0, str(Path(__file__).parent))
from rig_primigenius_mesh import geo_pivot, weld  # noqa: E402
from rig_trichonephila_mesh import read_obj, write_obj  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/kn8"
SPECIES = "kaiju_no9_no15"
UPPER_Y = 1.02
UPPER_TIP_X = 0.45
LOWER_TIP_Y = 0.8
LOWER_MIN_Y = 0.45
LOWER_OUT_X = 0.28
# (osso, pai) na ordem do geo; a ponta e filha da base.
TENTACLES = []
for _level, _parent in (("upper", "chest"), ("lower", "body")):
    for _side in ("left", "right"):
        TENTACLES.append((f"tentacle_{_level}_{_side}", _parent))
        TENTACLES.append((f"tentacle_{_level}_{_side}_tip", f"tentacle_{_level}_{_side}"))


def classify(centroids, small):
    x, y, z = centroids[:, 0], centroids[:, 1], centroids[:, 2]
    side = np.where(x < 0, "left", "right")
    labels = np.full(len(centroids), None, dtype=object)
    upper = small & (y > UPPER_Y)
    lower = small & (y <= UPPER_Y) & (y > LOWER_MIN_Y) & ((z > 0.02) | (np.abs(x) > LOWER_OUT_X))
    for i in np.nonzero(upper)[0]:
        labels[i] = f"tentacle_upper_{side[i]}" + ("_tip" if abs(x[i]) > UPPER_TIP_X else "")
    for i in np.nonzero(lower)[0]:
        labels[i] = f"tentacle_lower_{side[i]}" + ("_tip" if y[i] < LOWER_TIP_Y else "")
    return labels


def main():
    index_path = ASSETS / "meshes" / f"{SPECIES}.json"
    index = json.loads(index_path.read_text(encoding="utf-8"))
    folder = ASSETS / "meshes" / SPECIES
    parts = {}
    for bone in index["bones"]:
        if bone.startswith("tentacle_"):
            continue
        parts[bone] = read_obj(folder / f"{bone}.obj")
    # Juntar tudo para achar os pedacos ligados (rodar de novo parte das pecas sem tentaculo e da o mesmo).
    for bone in list(index["bones"]):
        if bone.startswith("tentacle_"):
            v, uv, n, f = read_obj(folder / f"{bone}.obj")
            owner = "chest" if "upper" in bone else "body"
            ov, ouv, on, of = parts[owner]
            parts[owner] = (np.vstack([ov, v]), np.vstack([ouv, uv]), np.vstack([on, n]), np.vstack([of, f + len(ov)]))
    names = list(parts)
    vertices = np.vstack([parts[b][0] for b in names])
    offsets = np.cumsum([0] + [len(parts[b][0]) for b in names])
    faces = np.vstack([parts[b][3] + offsets[i] for i, b in enumerate(names)])
    owner = np.concatenate([[b] * len(parts[b][3]) for b in names])
    welded = weld(vertices)
    wf = welded[faces]
    edges = np.vstack([wf[:, [0, 1]], wf[:, [1, 2]]])
    graph = coo_matrix((np.ones(len(edges)), (edges[:, 0], edges[:, 1])), shape=(welded.max() + 1,) * 2)
    _, component = connected_components(graph, directed=False)
    face_component = component[wf[:, 0]]
    ids, counts = np.unique(face_component, return_counts=True)
    big = set(ids[np.argsort(-counts)][:2])
    small = np.array([c not in big for c in face_component]) & (owner != "head")
    # O pedaco inteiro vai para um osso so (pelo centro dele), para nao rasgar os blocos.
    centroids = vertices[faces].mean(axis=1)
    piece_center = {c: centroids[face_component == c].mean(axis=0) for c in np.unique(face_component[small])}
    labels = classify(np.array([piece_center.get(c, centroids[i]) for i, c in enumerate(face_component)]), small)
    final = np.where(labels == None, owner, labels)  # noqa: E711
    uvs = np.vstack([parts[b][1] for b in names])
    normals = np.vstack([parts[b][2] for b in names])
    bones = {}
    for bone in list(dict.fromkeys(list(index["bones"]) + [t for t, _ in TENTACLES])):
        mask = final == bone
        if not mask.any():
            index["bones"].pop(bone, None)
            continue
        write_obj(folder / f"{bone}.obj", vertices, uvs, normals, faces[mask])
        index["bones"][bone] = f"kn8:meshes/{SPECIES}/{bone}.obj"
        bones[bone] = centroids[mask]
        print(f"{bone}: {mask.sum()} faces")
    index_path.write_text(json.dumps(index, indent=2) + "\n", encoding="utf-8")
    # Pivos: a base na raiz (ponto mais perto do meio das costas), a ponta onde encosta na base.
    geo_path = ASSETS / "geo/entity" / f"{SPECIES}.geo.json"
    geo = json.loads(geo_path.read_text(encoding="utf-8"))
    geometry = geo["minecraft:geometry"][0]
    geometry["bones"] = [b for b in geometry["bones"] if not b["name"].startswith("tentacle_")]
    pivots = {}
    for bone, parent in TENTACLES:
        if bone not in bones:
            continue
        points = bones[bone]
        if bone.endswith("_tip"):
            base = bones[parent]
            dist = np.linalg.norm(points[:, None, :] - base[None, ::7, :], axis=2).min(axis=1)
            pivot = points[np.argsort(dist)[:20]].mean(axis=0)
        else:
            pivot = points[np.argsort(np.abs(points[:, 0]))[:30]].mean(axis=0)
        pivots[bone] = pivot
    # Os dois lados com o mesmo pivo espelhado (o modelo e quase simetrico; a medida de cada lado variava 10 cm).
    for bone, parent in TENTACLES:
        if bone not in pivots:
            continue
        other = pivots.get(bone.replace("left", "right") if "left" in bone else bone.replace("right", "left"))
        pivot = pivots[bone] if other is None else np.array([
            np.sign(pivots[bone][0]) * (abs(pivots[bone][0]) + abs(other[0])) / 2,
            (pivots[bone][1] + other[1]) / 2, (pivots[bone][2] + other[2]) / 2])
        geometry["bones"].append({"name": bone, "pivot": geo_pivot(pivot), "parent": parent})
        print(f"  pivo {bone}: {np.round(pivot, 3)}")
    geo_path.write_text(json.dumps(geo, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
