#!/usr/bin/env python3
"""Espada da Forca de Defesa (kn8:sword): modelo e textura proprios, gerados por codigo (design original).

O modelo do Meshy da espada saiu como uma agulha fina com uma guarda redonda enorme (teste da 0.1-B). Este script
substitui o OBJ e a textura do item:
  - lamina de um gume (costas retas, fio que sobe ate a ponta), corte em "cunha" (costas grossas, fio fino), com
    faixa ciano no meio (mesma cor das luzes do traje);
  - guarda retangular, cabo trancado em losangos e pomo;
  - orientacao de espada vanilla (empunhadura no canto de baixo, ponta para cima e para a direita no quadrado do
    item), entao os display transforms de "item segurado" do vanilla servem sem ajuste.
Saida: assets/kn8/models/item/sword.obj + .mtl, assets/kn8/textures/item/sword.png (64x64, estilo pixel) e o "gui"
do sword.json enquadrado.
Uso: python3 tools/art/build_sword.py   (requer numpy e pillow)
"""
import json
import math
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/kn8"
NAME = "sword"
TEX = 64
# Empunhadura no quadrado do item, como numa espada vanilla (pixels / 16) — igual ao meshy_convert.py.
GRIP = np.array([5.0, 5.0, 8.0]) / 16.0
ALONG = np.array([1.0, 1.0, 0.0]) / math.sqrt(2.0)
ACROSS = np.array([-1.0, 1.0, 0.0]) / math.sqrt(2.0)
DEPTH = np.array([0.0, 0.0, 1.0])

# Medidas (unidades do item; 1 = um bloco). s = distancia ao longo da espada a partir do centro da mao.
HANDLE = (-0.21, 0.07)
HANDLE_RADIUS = 0.032
GUARD = (0.07, 0.105)
GUARD_HALF = (0.095, 0.05)  # meia largura (atravessado) e meia espessura
BLADE_START = 0.105
BLADE_SPINE = 0.04  # posicao das costas no eixo atravessado (fio fica do outro lado)
# Estacoes da lamina: (s, largura, espessura das costas)
BLADE = [(0.105, 0.088, 0.03), (0.55, 0.082, 0.026), (1.05, 0.074, 0.022), (1.17, 0.062, 0.02),
         (1.25, 0.036, 0.014), (1.31, 0.004, 0.006)]
POMMEL = (-0.245, -0.21)
POMMEL_HALF = 0.042

# Faixas de V da textura (linhas em pixels) por material.
BANDS = {"spine": (0, 6), "side": (6, 22), "bevel": (22, 34), "guard": (34, 44), "grip": (44, 58),
         "pommel": (58, 64)}


class Mesh:
    def __init__(self):
        self.tris = []  # (p0, p1, p2, uv0, uv1, uv2)

    def quad(self, p, uv):
        self.tris.append((p[0], p[1], p[2], uv[0], uv[1], uv[2]))
        self.tris.append((p[0], p[2], p[3], uv[0], uv[2], uv[3]))


def to_item(s, a, d):
    return GRIP + ALONG * s + ACROSS * a + DEPTH * d


def band_uv(band, u, v):
    """u e v em 0..1 dentro da faixa (com meio texel de folga nas bordas para a amostragem nearest)."""
    top, bottom = BANDS[band]
    margin = 0.5 / TEX
    return (margin + u * (1 - 2 * margin), (top + 0.5 + v * (bottom - top - 1)) / TEX)


def ring_strips(mesh, stations, polygon, bands, u_range):
    """Liga anéis consecutivos (mesmo numero de vertices) em faixas; cada lado do poligono tem sua faixa."""
    s0, s1 = stations[0][0], stations[-1][0]
    for (sa, ring_a), (sb, ring_b) in zip(stations, stations[1:]):
        ua = u_range[0] + (u_range[1] - u_range[0]) * (sa - s0) / (s1 - s0)
        ub = u_range[0] + (u_range[1] - u_range[0]) * (sb - s0) / (s1 - s0)
        n = len(ring_a)
        for k in range(n):
            band, v0, v1 = bands[k]
            j = (k + 1) % n
            p = [to_item(sa, *ring_a[k]), to_item(sb, *ring_b[k]), to_item(sb, *ring_b[j]), to_item(sa, *ring_a[j])]
            uv = [band_uv(band, ua, v0), band_uv(band, ub, v0), band_uv(band, ub, v1), band_uv(band, ua, v1)]
            mesh.quad(p, uv)


def cap(mesh, s, ring, band, flip):
    """Tampa plana (leque a partir do centro)."""
    center = np.mean([to_item(s, *c) for c in ring], axis=0)
    n = len(ring)
    for k in range(n):
        j = (k + 1) % n
        a, b = to_item(s, *ring[k]), to_item(s, *ring[j])
        uvc = band_uv(band, 0.5, 0.5)
        if flip:
            mesh.tris.append((center, b, a, uvc, band_uv(band, 0.9, 0.9), band_uv(band, 0.1, 0.9)))
        else:
            mesh.tris.append((center, a, b, uvc, band_uv(band, 0.1, 0.9), band_uv(band, 0.9, 0.9)))


def blade(mesh):
    stations = []
    for s, width, thick in BLADE:
        spine = BLADE_SPINE
        edge = spine - width
        mid = spine - width * 0.45  # inicio do chanfro do fio
        t = thick / 2
        # Anel (atravessado, profundidade): costas (2), lado, chanfro ate o fio, outro lado.
        stations.append((s, [(spine, t), (spine, -t), (mid, -t * 0.85), (edge, 0.0), (mid, t * 0.85)]))
    bands = [("spine", 0, 1), ("side", 0, 1), ("bevel", 0, 1), ("bevel", 1, 0), ("side", 1, 0)]
    ring_strips(mesh, stations, None, bands, (0.0, 1.0))
    cap(mesh, BLADE[0][0], stations[0][1], "guard", True)


def prism(mesh, s0, s1, ring, band, u_wrap=True):
    """Prisma ao longo da espada com o mesmo anel nas duas pontas; textura enrolada em volta (u) e ao longo (v)."""
    n = len(ring)
    for k in range(n):
        j = (k + 1) % n
        p = [to_item(s0, *ring[k]), to_item(s1, *ring[k]), to_item(s1, *ring[j]), to_item(s0, *ring[j])]
        u0, u1 = k / n, (k + 1) / n
        mesh.quad(p, [band_uv(band, u0, 0), band_uv(band, u0, 1), band_uv(band, u1, 1), band_uv(band, u1, 0)])
    cap(mesh, s0, ring, band, True)
    cap(mesh, s1, ring, band, False)


def octagon(radius_a, radius_d, offset=0.0):
    return [(offset + radius_a * math.cos(math.pi / 8 + k * math.pi / 4),
             radius_d * math.sin(math.pi / 8 + k * math.pi / 4)) for k in range(8)]


def build_mesh():
    mesh = Mesh()
    blade(mesh)
    wa, wd = GUARD_HALF
    guard_ring = [(wa, wd), (-wa, wd), (-wa, -wd), (wa, -wd)]
    prism(mesh, GUARD[0], GUARD[1], guard_ring, "guard")
    prism(mesh, HANDLE[0], HANDLE[1], octagon(HANDLE_RADIUS, HANDLE_RADIUS * 0.85), "grip")
    prism(mesh, POMMEL[0], POMMEL[1], octagon(POMMEL_HALF, POMMEL_HALF * 0.8), "pommel")
    return mesh


# ------------------------------------------------------------------------------------------------ textura
def paint():
    img = np.zeros((TEX, TEX, 4), np.uint8)
    rng = np.random.default_rng(8)

    def fill(band, fn):
        top, bottom = BANDS[band]
        for y in range(top, bottom):
            for x in range(TEX):
                img[y, x] = (*fn(x, (y - top) / max(bottom - top - 1, 1)), 255)

    def jitter(rgb, amount=6):
        n = int(rng.integers(-amount, amount + 1))
        return tuple(int(np.clip(c + n, 0, 255)) for c in rgb)

    # Costas: metal escuro com brilho no meio.
    fill("spine", lambda x, v: jitter((58, 62, 70) if 0.3 < v < 0.7 else (38, 41, 48)))
    # Lado da lamina: aco escurecido, faixa ciano no meio (linha de energia), mais claro perto do chanfro.

    def side(x, v):
        if 0.42 < v < 0.58:
            return (70, 225, 235) if (x // 4) % 2 == 0 or 0.46 < v < 0.54 else (40, 160, 175)
        base = 70 + int(60 * v)
        return jitter((base, base + 4, base + 12))
    fill("side", side)
    # Chanfro do fio: prata clara, quase branca no fio.

    def bevel(x, v):
        base = 170 + int(75 * v)
        return jitter((base, base + 3, min(base + 8, 255)), 4)
    fill("bevel", bevel)
    # Guarda: metal escuro com borda clara e um ponto ciano.
    fill("guard", lambda x, v: (60, 210, 225) if (x % 16 in (7, 8) and 0.3 < v < 0.7)
         else jitter((90, 95, 105) if v < 0.15 or v > 0.85 else (45, 48, 56)))
    # Cabo: tranca em losangos (preto e cinza escuro).

    def grip(x, v):
        y = int(v * 13)
        diamond = (x + y) % 6 < 3 if (x // 3 + y // 3) % 2 == 0 else (x - y) % 6 < 3
        return jitter((52, 54, 60) if diamond else (22, 23, 27), 3)
    fill("grip", grip)
    fill("pommel", lambda x, v: jitter((120, 126, 136) if v < 0.5 else (70, 74, 82)))
    return Image.fromarray(img, "RGBA")


# ------------------------------------------------------------------------------------------------ saida
def write(mesh):
    lines = ["mtllib sword.mtl", "o sword", "usemtl main"]
    vts, vns = [], []
    for p0, p1, p2, *_ in mesh.tris:
        lines += [f"v {p[0]:.5f} {p[1]:.5f} {p[2]:.5f}" for p in (p0, p1, p2)]
    for *_, uv0, uv1, uv2 in mesh.tris:
        # OBJ tem V para cima (o sword.json usa flip_v).
        vts += [f"vt {u:.5f} {1.0 - v:.5f}" for u, v in (uv0, uv1, uv2)]
    for p0, p1, p2, *_ in mesh.tris:
        n = np.cross(np.asarray(p1) - p0, np.asarray(p2) - p0)
        n = n / (np.linalg.norm(n) or 1.0)
        vns += [f"vn {n[0]:.4f} {n[1]:.4f} {n[2]:.4f}"] * 3
    lines += vts + vns
    for t in range(len(mesh.tris)):
        a, b, c = 3 * t + 1, 3 * t + 2, 3 * t + 3
        lines.append(f"f {a}/{a}/{a} {b}/{b}/{b} {c}/{c}/{c}")
    (ASSETS / f"models/item/{NAME}.obj").write_text("\n".join(lines) + "\n", encoding="utf-8")
    (ASSETS / f"models/item/{NAME}.mtl").write_text("newmtl main\nKd 1.0 1.0 1.0\nmap_Kd #texture\n",
                                                    encoding="utf-8")


def gui_fit(points):
    """Mesmo enquadramento do meshy_convert.py (icone ocupa 90% do quadrado)."""
    low, high = points.min(axis=0), points.max(axis=0)
    scale = min(2.5, 0.9 / max(high[0] - low[0], high[1] - low[1]))
    center = (low + high) / 2
    return {"rotation": [0, 0, 0],
            "translation": [round((0.5 - center[0]) * 16 * scale, 3), round((0.5 - center[1]) * 16 * scale, 3), 0],
            "scale": [round(scale, 3)] * 3}


def main():
    mesh = build_mesh()
    write(mesh)
    paint().save(ASSETS / f"textures/item/{NAME}.png")
    points = np.array([p for t in mesh.tris for p in t[:3]])
    model_path = ASSETS / f"models/item/{NAME}.json"
    model = json.loads(model_path.read_text(encoding="utf-8"))
    model["display"]["gui"] = gui_fit(points)
    model_path.write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")
    low, high = points.min(axis=0), points.max(axis=0)
    print(f"espada: {len(mesh.tris)} triangulos, caixa {np.round(low, 3)} .. {np.round(high, 3)}")


if __name__ == "__main__":
    main()
