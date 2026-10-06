#!/usr/bin/env python3
"""Refaz a textura de uma malha do Meshy cujas UVs foram quebradas pela reducao de triangulos.

Problema (Trichonephila, 0.1-B): a reducao de 10 mil para 6 mil triangulos do meshy_convert.py antigo dava a cada
vertice a UV do vertice original mais proximo; nas costuras da textura isso junta, no mesmo triangulo, cantos de
ilhas diferentes. Quase metade dos triangulos passava a cobrir a textura inteira (linhas douradas e manchas brancas
no jogo).

Correcao (sem precisar do GLB original):
  1. classifica cada triangulo: UV boa (escala de texels por metro parecida com a mediana) ou quebrada;
  2. cria um atlas novo com uma celula por triangulo (mesma forma do triangulo em 3D, densidade uniforme, 1 texel de
     borda repetida em volta para a amostragem "nearest" do Minecraft nao pegar a celula vizinha);
  3. triangulo bom: copia a cor da textura original pela UV antiga;
     triangulo quebrado: cor do ponto bom mais proximo na superficie (posicao + normal, para nao pegar o outro lado
     de uma pata fina).
Saida: o mesmo OBJ (um vertice por canto, v/vt/vn com o mesmo indice, como o rig espera) e a textura nova.

Uso: python3 tools/art/rebake_mesh_texture.py <entrada.obj> <entrada.png> [--size 1024] [--out-obj X --out-png Y]
Depois rode o rig da especie (ex.: tools/art/rig_trichonephila_mesh.py) para regerar meshes/ e textures/.
Requer so numpy e pillow.
"""
import argparse
import math
from pathlib import Path

import numpy as np
from PIL import Image

MARGIN = 1
# Triangulo "quebrado": alguma aresta com texels/metro fora de [mediana / LIMIT, mediana * LIMIT].
LIMIT = 3.0
# Peso da normal na busca do vizinho bom (metros equivalentes por unidade de normal).
NORMAL_WEIGHT = 0.1
# Lado das celulas da grade de busca (metros).
GRID = 0.2


def read_obj(path):
    v, t, n, f = [], [], [], []
    for line in Path(path).read_text(encoding="utf-8").splitlines():
        p = line.split()
        if not p:
            continue
        if p[0] == "v":
            v.append([float(x) for x in p[1:4]])
        elif p[0] == "vt":
            t.append([float(x) for x in p[1:3]])
        elif p[0] == "vn":
            n.append([float(x) for x in p[1:4]])
        elif p[0] == "f":
            idx = [[int(i) - 1 for i in c.split("/")] for c in p[1:]]
            for k in range(1, len(idx) - 1):
                f.append([idx[0], idx[k], idx[k + 1]])
    f = np.array(f)
    return np.array(v)[f[:, :, 0]], np.array(t)[f[:, :, 1]], np.array(n)[f[:, :, 2]]


def classify(P, UV, size):
    """True = UV boa."""
    edges3 = np.stack([P[:, 1] - P[:, 0], P[:, 2] - P[:, 1], P[:, 0] - P[:, 2]], axis=1)
    edgesuv = np.stack([UV[:, 1] - UV[:, 0], UV[:, 2] - UV[:, 1], UV[:, 0] - UV[:, 2]], axis=1) * size
    l3 = np.linalg.norm(edges3, axis=2)
    luv = np.linalg.norm(edgesuv, axis=2)
    ratio = luv / np.maximum(l3, 1e-9)
    median = np.median(ratio[l3 > 1e-6])
    ok = (ratio < median * LIMIT) & (ratio > median / LIMIT)
    return ok.all(axis=1) | (l3.max(axis=1) < 1e-6), median


def layout(P):
    """Forma 2D de cada triangulo (metros): canto 0 na origem, maior aresta no eixo X. Devolve ordem e cantos 2D."""
    n = len(P)
    order = np.zeros((n, 3), dtype=int)
    flat = np.zeros((n, 3, 2))
    for i in range(n):
        lengths = [np.linalg.norm(P[i, (k + 1) % 3] - P[i, k]) for k in range(3)]
        k = int(np.argmax(lengths))
        o = [k, (k + 1) % 3, (k + 2) % 3]  # rotacao ciclica: mantem o sentido do triangulo
        a, b, c = P[i, o[0]], P[i, o[1]], P[i, o[2]]
        base = max(lengths[k], 1e-9)
        e = (b - a) / base
        cx = float(np.dot(c - a, e))
        h = float(np.linalg.norm((c - a) - cx * e))
        order[i] = o
        flat[i] = [[0, 0], [base, 0], [min(max(cx, 0), base), h]]
    return order, flat


def pack(flat, density, size):
    """Prateleiras por altura. Devolve a origem (px) de cada celula ou None se nao couber."""
    w = np.ceil(flat[:, 1, 0] * density).astype(int) + 2 * MARGIN + 1
    h = np.ceil(flat[:, 2, 1] * density).astype(int) + 2 * MARGIN + 1
    origins = np.zeros((len(flat), 2), dtype=int)
    x = y = shelf = 0
    for i in np.argsort(-h, kind="stable"):
        if w[i] > size:
            return None
        if x + w[i] > size:
            x, y, shelf = 0, y + shelf, 0
        if y + h[i] > size:
            return None
        origins[i] = (x, y)
        x += w[i]
        shelf = max(shelf, h[i])
    return origins


def nearest(points, cloud):
    """Indice do ponto da nuvem mais proximo (grade espacial, so numpy)."""
    keys = np.floor(cloud[:, :3] / GRID).astype(np.int64)
    qkeys = np.floor(points[:, :3] / GRID).astype(np.int64)
    buckets = {}
    for idx, key in enumerate(map(tuple, keys)):
        buckets.setdefault(key, []).append(idx)
    buckets = {k: np.array(v) for k, v in buckets.items()}
    result = np.full(len(points), -1)
    by_cell = {}
    for idx, key in enumerate(map(tuple, qkeys)):
        by_cell.setdefault(key, []).append(idx)
    for key, queries in by_cell.items():
        cand = [buckets[(key[0] + dx, key[1] + dy, key[2] + dz)]
                for dx in (-1, 0, 1) for dy in (-1, 0, 1) for dz in (-1, 0, 1)
                if (key[0] + dx, key[1] + dy, key[2] + dz) in buckets]
        q = points[queries]
        if cand:
            cand = np.concatenate(cand)
            d = ((q[:, None, :] - cloud[cand][None, :, :]) ** 2).sum(axis=2)
            result[queries] = cand[np.argmin(d, axis=1)]
        else:
            for j, qi in enumerate(queries):
                result[qi] = int(np.argmin(((cloud - q[j]) ** 2).sum(axis=1)))
    return result


def rebake(P, UV, N, tex, size):
    good, median = classify(P, UV, tex.shape[0])
    order, flat = layout(P)
    # Maior densidade (texels/m) que cabe no atlas.
    low, high = 1.0, 4096.0
    origins = None
    for _ in range(30):
        mid = (low + high) / 2
        packed = pack(flat, mid, size)
        if packed is None:
            high = mid
        else:
            low, origins = mid, packed
    density = low
    out = np.zeros((size, size, 4), dtype=np.float32)
    newuv = np.zeros((len(P), 3, 2))
    th, tw = tex.shape[:2]
    pending = []  # (texel ys, texel xs, pontos 3D com normal) dos triangulos quebrados
    cloud_pts, cloud_rgba = [], []
    for i in range(len(P)):
        o = order[i]
        a2 = flat[i] * density + MARGIN
        ox, oy = origins[i]
        for k in range(3):
            px, py = ox + a2[k, 0], oy + a2[k, 1]
            newuv[i, o[k]] = (px / size, py / size)
        w = int(np.ceil(flat[i, 1, 0] * density)) + 2 * MARGIN + 1
        h = int(np.ceil(flat[i, 2, 1] * density)) + 2 * MARGIN + 1
        xs, ys = np.meshgrid(np.arange(w) + 0.5, np.arange(h) + 0.5)
        (x0, y0), (x1, y1), (x2, y2) = a2
        den = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2)
        if abs(den) < 1e-9:
            bary = np.stack([np.ones_like(xs), np.zeros_like(xs), np.zeros_like(xs)], axis=-1)
            inside = np.zeros_like(xs, dtype=bool)
        else:
            l0 = ((y1 - y2) * (xs - x2) + (x2 - x1) * (ys - y2)) / den
            l1 = ((y2 - y0) * (xs - x2) + (x0 - x2) * (ys - y2)) / den
            bary = np.stack([l0, l1, 1 - l0 - l1], axis=-1)
            inside = (bary >= 0).all(axis=-1)
            bary = np.clip(bary, 0, None)
            bary /= bary.sum(axis=-1, keepdims=True)
        corners3 = P[i, o]
        pts = bary @ corners3
        ty, tx = oy + np.arange(h)[:, None] + np.zeros((1, w), int), ox + np.arange(w)[None, :] + np.zeros((h, 1), int)
        if good[i]:
            uv = bary @ UV[i, o]
            sx = np.clip((uv[..., 0] * tw).astype(int), 0, tw - 1)
            sy = np.clip(((1 - uv[..., 1]) * th).astype(int), 0, th - 1)
            rgba = tex[sy, sx]
            out[ty, tx] = rgba
            normal = N[i].mean(axis=0)
            normal /= max(np.linalg.norm(normal), 1e-9)
            feats = np.concatenate([pts[inside], np.repeat(normal[None] * NORMAL_WEIGHT, inside.sum(), 0)], 1)
            cloud_pts.append(feats)
            cloud_rgba.append(rgba[inside])
        else:
            normal = N[i].mean(axis=0)
            normal /= max(np.linalg.norm(normal), 1e-9)
            feats = np.concatenate([pts.reshape(-1, 3), np.repeat(normal[None] * NORMAL_WEIGHT, pts.size // 3, 0)], 1)
            pending.append((ty.ravel(), tx.ravel(), feats))
    cloud = np.concatenate(cloud_pts)
    colors = np.concatenate(cloud_rgba)
    if pending:
        ty = np.concatenate([p[0] for p in pending])
        tx = np.concatenate([p[1] for p in pending])
        feats = np.concatenate([p[2] for p in pending])
        out[ty, tx] = colors[nearest(feats, cloud)]
    out[..., 3] = 255
    return out.astype(np.uint8), newuv, good, density, median


def write_obj(path, P, UV, N):
    """Um vertice por canto: v/vt/vn com o mesmo indice (o read_obj dos scripts de rig espera isso)."""
    lines = []
    mtl = Path(path).with_suffix(".mtl")
    if mtl.exists():
        lines.append(f"mtllib {mtl.name}")
    lines.append(f"o {Path(path).stem}")
    if mtl.exists():
        lines.append("usemtl main")
    flatp, flatuv, flatn = P.reshape(-1, 3), UV.reshape(-1, 2), N.reshape(-1, 3)
    lines += [f"v {x:.5f} {y:.5f} {z:.5f}" for x, y, z in flatp]
    # OBJ tem V para cima.
    lines += [f"vt {u:.6f} {1.0 - v:.6f}" for u, v in flatuv]
    lines += [f"vn {x:.4f} {y:.4f} {z:.4f}" for x, y, z in flatn]
    for t in range(len(P)):
        a, b, c = 3 * t + 1, 3 * t + 2, 3 * t + 3
        lines.append(f"f {a}/{a}/{a} {b}/{b}/{b} {c}/{c}/{c}")
    Path(path).write_text("\n".join(lines) + "\n", encoding="utf-8")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("obj")
    parser.add_argument("png")
    parser.add_argument("--size", type=int, default=1024)
    parser.add_argument("--out-obj")
    parser.add_argument("--out-png")
    args = parser.parse_args()
    P, UV, N = read_obj(args.obj)
    tex = np.asarray(Image.open(args.png).convert("RGBA"))
    image, newuv, good, density, median = rebake(P, UV, N, tex, args.size)
    write_obj(args.out_obj or args.obj, P, newuv, N)
    Image.fromarray(image).save(args.out_png or args.png)
    print(f"{len(P)} triangulos, {int((~good).sum())} com UV quebrada (refeitos pelo vizinho bom); "
          f"densidade antiga {median:.0f} texels/m, nova {density:.0f} texels/m em {args.size}x{args.size}")


if __name__ == "__main__":
    main()
