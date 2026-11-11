#!/usr/bin/env python3
"""FASE 2-C (documento mestre, Miguel 2026-11-11): pedacos de malha no osso errado depois do rig automatico.

O rig do Meshy (rig_from_skin.py, osso de maior peso por face) as vezes poe no osso da perna ou do braco pedacos que
sao do tronco: o rabo de cavalo e as tiras da frente do casaco do Soichiro iam na coxa e saiam girando com ela ao
correr ("lascas"). Este script acha, nos ossos de membro (leg/shin/arm/forearm), os pedacos ligados da malha:
  - longe do segmento do osso (do pivo dele ao pivo do filho): mais que STRAY_FACTOR x a distancia mediana da malha
    do osso a esse segmento e mais que STRAY_FLOOR metros;
  - na coxa, inteiros acima do pivo do quadril (acima da junta e tronco, nao perna);
e os passa para o osso do tronco (body, chest ou head) com a malha mais perto deles. Nao cria nem apaga triangulo: so muda
o osso (o OBJ de destino ganha os vertices, UV e normais do pedaco). O original fica no historico do Git.

Rodar DEPOIS do rig da especie (rig_from_skin.py / rig_soldier_mesh.py / rig_kaiju_no9_mesh.py), que regravam os
OBJ. Rodar de novo nao move mais nada.
Uso: python3 tools/art/rig_strays.py [--dry] <especie> ...
"""
import json
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).parent))
from rig_trichonephila_mesh import read_obj, write_obj  # noqa: E402
from mesh_audit import components  # noqa: E402
from preview_rig import Rig  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/kn8"
CHILD = {"leg": "shin", "shin": "foot", "arm": "forearm", "forearm": "hand"}
STRAY_FACTOR = 2.5
STRAY_FLOOR = 0.12
HIP_MARGIN = 0.02
PASSES = 4


def segment_distance(points, a, b):
    ab = b - a
    t = np.clip(((points - a) @ ab) / (ab @ ab), 0, 1)
    return np.linalg.norm(points - (a + t[:, None] * ab), axis=1)


def strays(rig, bone, mesh):
    kind, _, side = bone.partition("_")
    child = f"{CHILD.get(kind)}_{side}"
    if kind not in CHILD or child not in rig.bones:
        return []
    v, _, _, f = mesh
    a, b = rig.pivot(bone), rig.pivot(child)
    dist = segment_distance(v, a, b)
    median = float(np.median(dist[np.unique(f)]))
    label = components(v, f)
    out = []
    for comp in np.unique(label):
        faces = np.nonzero(label == comp)[0]
        idx = np.unique(f[faces])
        far = dist[idx].min() > max(STRAY_FLOOR, STRAY_FACTOR * median)
        above_hip = kind == "leg" and v[idx][:, 1].min() > a[1] - HIP_MARGIN
        if far or above_hip:
            out.append(faces)
    return out


TORSO = ("body", "chest", "head")


def target(rig, meshes, bone, faces):
    """Osso do tronco cuja malha esta mais perto do pedaco (o fim do rabo de cavalo vai com o resto dele, no peito)."""
    from scipy.spatial import cKDTree
    v, _, _, f = meshes[bone]
    points = v[np.unique(f[faces])]
    best, dest = np.inf, "body"
    for name in TORSO:
        if name in meshes and len(meshes[name][3]):
            d = cKDTree(meshes[name][0][np.unique(meshes[name][3])]).query(points)[0].min()
            if d < best:
                best, dest = d, name
    return dest


def run(species, dry=False):
    rig = Rig(species)
    index = json.loads((ASSETS / f"meshes/{species}.json").read_text(encoding="utf-8"))["bones"]
    meshes = dict(rig.meshes)
    moves = []
    for bone, mesh in rig.meshes.items():
        for faces in strays(rig, bone, mesh):
            moves.append((bone, faces, target(rig, rig.meshes, bone, faces)))
    if not moves:
        print(f"{species}: nada a mover")
        return False
    count = {}
    for bone, faces, dest in moves:
        v, uv, n, f = rig.meshes[bone]
        dv, duv, dn, df = meshes[dest]
        meshes[dest] = (np.vstack([dv, v]), np.vstack([duv, uv]), np.vstack([dn, n]),
                        np.vstack([df, f[faces] + len(dv)]))
        count[(bone, dest)] = count.get((bone, dest), 0) + len(faces)
    for bone in {b for b, _, _ in moves}:
        v, uv, n, f = meshes[bone]
        keep = np.ones(len(f), bool)
        keep[np.concatenate([faces for b, faces, _ in moves if b == bone])] = False
        meshes[bone] = (v, uv, n, f[keep])
    for (bone, dest), tris in sorted(count.items()):
        print(f"{species}: {bone} -> {dest}: {tris} triangulos")
    if dry:
        return True
    for bone in {b for b, _, _ in moves} | {d for _, _, d in moves}:
        v, uv, n, f = meshes[bone]
        write_obj(ASSETS / index[bone].split(":")[1], v, uv, n, f)
    return True


def main():
    args = sys.argv[1:]
    dry = "--dry" in args
    for species in (a for a in args if a != "--dry"):
        # Tirar um pedaco muda a mediana do osso: repete ate nada mudar (no maximo PASSES vezes).
        for _ in range(1 if dry else PASSES):
            if not run(species, dry):
                break


if __name__ == "__main__":
    main()
