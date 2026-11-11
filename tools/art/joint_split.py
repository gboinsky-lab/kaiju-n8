#!/usr/bin/env python3
"""FASE 2-C (Miguel, 2026-11-11): triangulos que atravessam a junta cortados no plano dela.

A reducao do Meshy deixa triangulos longos que comecam na coxa e terminam na canela (ou no braco e no antebraco). O
corte pelo rig poe cada triangulo inteiro num osso so: parado ele fica no lugar, mas ao dobrar a junta o canto que
esta do outro lado sai do corpo como uma lamina ("espeto"). Aqui cada triangulo de um par de ossos ligados por uma
junta (joelho, tornozelo, cotovelo, punho) que passa do plano da junta mais que MARGIN dos dois lados e cortado no
plano: o pedaco de cada lado fica com o osso daquele lado (vertices novos com posicao, UV e normal interpoladas). A
superficie parada continua a mesma; dobrando, abre uma fresta na junta (a mesma das pecas rigidas da GeckoLib) em vez
da lamina.

Plano da junta: passa pelo pivo do filho, normal = media das direcoes dos dois ossos (pai: do pivo dele ao pivo do
filho; filho: do pivo dele ao pivo do neto). Lado positivo = filho.

Rodar DEPOIS do rig da especie e do rig_strays.py (eles regravam os OBJ); rodar de novo nao corta mais nada.
Uso: python3 tools/art/joint_split.py [--dry] <especie> ...
"""
import json
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).parent))
from preview_rig import Rig  # noqa: E402
from rig_trichonephila_mesh import write_obj  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/kn8"
MARGIN = 0.03
JOINTS = (("leg", "shin", "foot"), ("shin", "foot", None), ("arm", "forearm", "hand"), ("forearm", "hand", None))
SIDES = ("left", "right")


class Mesh:
    def __init__(self, v, uv, n, f):
        self.v, self.uv, self.n = list(v), list(uv), list(n)
        self.f = [list(x) for x in f]

    def add(self, v, uv, n):
        self.v.append(v)
        self.uv.append(uv)
        self.n.append(n)
        return len(self.v) - 1

    def arrays(self):
        return np.array(self.v), np.array(self.uv), np.array(self.n), np.array(self.f, dtype=int)


def plane(rig, parent, child, grandchild):
    joint = rig.pivot(child)
    a = joint - rig.pivot(parent)
    a /= np.linalg.norm(a)
    if grandchild and grandchild in rig.bones:
        b = rig.pivot(grandchild) - joint
        b /= np.linalg.norm(b)
        normal = a + b
    else:
        normal = a
    return joint, normal / np.linalg.norm(normal)


def clip(src, face, dist, keep_positive):
    """Poligono do lado pedido do plano (indices na malha de origem e pontos novos interpolados)."""
    out = []
    for i in range(3):
        j = (i + 1) % 3
        a, b = face[i], face[j]
        da, db = dist[i], dist[j]
        inside_a = da >= 0 if keep_positive else da < 0
        inside_b = db >= 0 if keep_positive else db < 0
        if inside_a:
            out.append(("old", a))
        if inside_a != inside_b:
            t = da / (da - db)
            out.append(("new", (a, b, t)))
    return out


def split_pair(rig, meshes, parent, child, grandchild):
    joint, normal = plane(rig, parent, child, grandchild)
    moved = 0
    for owner, other, sign in ((parent, child, 1.0), (child, parent, -1.0)):
        src = meshes[owner]
        v = np.array(src.v)
        keep = []
        new_faces_owner, new_faces_other = [], []
        for face in src.f:
            d = (v[face] - joint) @ normal * sign  # positivo = lado do outro osso
            if d.max() > MARGIN and d.min() < -MARGIN:
                new_faces_owner.append(clip(src, face, d, keep_positive=False))
                new_faces_other.append(clip(src, face, d, keep_positive=True))
                moved += 1
            else:
                keep.append(face)
        src.f = keep
        for polys, target in ((new_faces_owner, src), (new_faces_other, meshes[other])):
            for poly in polys:
                idx = []
                for kind, data in poly:
                    if kind == "old":
                        k = data
                        idx.append(target.add(src.v[k], src.uv[k], src.n[k]) if target is not src else k)
                    else:
                        a, b, t = data
                        pos = np.array(src.v[a]) * (1 - t) + np.array(src.v[b]) * t
                        uv = np.array(src.uv[a]) * (1 - t) + np.array(src.uv[b]) * t
                        nn = np.array(src.n[a]) * (1 - t) + np.array(src.n[b]) * t
                        nn = nn / (np.linalg.norm(nn) or 1)
                        idx.append(target.add(pos, uv, nn))
                for k in range(1, len(idx) - 1):
                    target.f.append([idx[0], idx[k], idx[k + 1]])
    return moved


def run(species, dry=False):
    rig = Rig(species)
    index = json.loads((ASSETS / f"meshes/{species}.json").read_text(encoding="utf-8"))["bones"]
    meshes = {b: Mesh(*m) for b, m in rig.meshes.items()}
    touched = set()
    for parent_kind, child_kind, grand_kind in JOINTS:
        for side in SIDES:
            parent, child = f"{parent_kind}_{side}", f"{child_kind}_{side}"
            grand = f"{grand_kind}_{side}" if grand_kind else None
            if parent not in meshes or child not in meshes:
                continue
            moved = split_pair(rig, meshes, parent, child, grand)
            if moved:
                touched |= {parent, child}
                print(f"{species}: {parent}/{child}: {moved} triangulo(s) cortado(s)")
    if not touched:
        print(f"{species}: nada a cortar")
    if dry:
        return
    for bone in touched:
        v, uv, n, f = meshes[bone].arrays()
        write_obj(ASSETS / index[bone].split(":")[1], v, uv, n, f)


def main():
    args = sys.argv[1:]
    dry = "--dry" in args
    for species in (a for a in args if a != "--dry"):
        run(species, dry)


if __name__ == "__main__":
    main()
