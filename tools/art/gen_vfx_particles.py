#!/usr/bin/env python3
"""VFX-1 (diretriz KaijuVFX): texturas das particulas proprias do kn8.

Cada silhueta e branca/cinza com transparencia; a cor vem da paleta do perfil visual no jogo (a particula multiplica
a textura pela cor). 16 x 16 px como as particulas vanilla. Variacoes (lasca, esporo) sao sorteadas pela particula.

    python3 tools/art/gen_vfx_particles.py

Grava:
    src/main/resources/assets/kn8/textures/particle/<nome>.png
    src/main/resources/assets/kn8/particles/<tipo>.json
"""
import json
import math
import os
import random

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
TEX = os.path.join(ROOT, 'src/main/resources/assets/kn8/textures/particle')
DEF = os.path.join(ROOT, 'src/main/resources/assets/kn8/particles')
SIZE = 16
C = (SIZE - 1) / 2.0


def image(fn):
    img = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()
    for y in range(SIZE):
        for x in range(SIZE):
            value = fn(x, y)
            if value is None:
                continue
            gray, alpha = value
            if alpha <= 0:
                continue
            g = max(0, min(255, int(round(gray * 255))))
            px[x, y] = (g, g, g, max(0, min(255, int(round(alpha * 255)))))
    return img


def arc(x, y):
    """Meia-lua de corte: borda externa brilhante, fina nas pontas (lamina passando)."""
    dx, dy = x - C, y - C - 3.0
    r = math.hypot(dx, dy)
    angle = math.atan2(dy, dx)
    if dy > 0.5 or not 5.0 <= r <= 8.2:
        return None
    # Mais grossa no meio do arco, afina nas pontas.
    middle = 1.0 - abs(angle + math.pi / 2) / (math.pi / 2)
    thick = 1.0 + 2.2 * middle
    edge = 8.2 - r
    if edge > thick:
        return None
    return (1.0, min(1.0, 0.35 + 0.65 * middle) * (1.0 - 0.4 * edge / max(thick, 0.01)))


def ring(x, y):
    """Anel fino de onda de choque (fica deitado no chao)."""
    r = math.hypot(x - C, y - C)
    d = abs(r - 6.4)
    if d > 1.2:
        return None
    return (1.0, 1.0 - d / 1.2 * 0.7)


def streak(x, y):
    """Risco de velocidade: fino e comprido, some nas pontas."""
    dx, dy = abs(x - C), abs(y - C)
    if dx > 1.0 or dy > 7.5:
        return None
    return (1.0, (1.0 - dy / 7.5) * (1.0 if dx < 0.6 else 0.45))


def glow(x, y):
    """Ponto de energia: miolo forte e halo suave."""
    r = math.hypot(x - C, y - C) / 7.5
    if r > 1.0:
        return None
    return (1.0, (1.0 - r) ** 1.6)


def shard_points(seed):
    rnd = random.Random(seed)
    n = rnd.choice([3, 4, 5])
    pts = []
    for i in range(n):
        a = 2 * math.pi * i / n + rnd.uniform(-0.4, 0.4)
        rad = rnd.uniform(3.5, 7.0)
        pts.append((C + math.cos(a) * rad, C + math.sin(a) * rad))
    return pts


def inside(poly, x, y):
    hit = False
    for i in range(len(poly)):
        x1, y1 = poly[i]
        x2, y2 = poly[(i + 1) % len(poly)]
        if (y1 > y) != (y2 > y) and x < (x2 - x1) * (y - y1) / (y2 - y1) + x1:
            hit = not hit
    return hit


def shard(seed):
    """Lasca/cristal irregular com um lado claro e outro escuro (le como volume quando tingido)."""
    poly = shard_points(seed)

    def fn(x, y):
        if not inside(poly, x + 0.5, y + 0.5):
            return None
        light = 0.55 + 0.45 * (1.0 if (x - C) - (y - C) < 0 else 0.4)
        return (light, 1.0)
    return fn


def spore(seed):
    """Nuvem pequena e macia (esporo, vapor, nevoa)."""
    rnd = random.Random(seed)
    blobs = [(C + rnd.uniform(-2.5, 2.5), C + rnd.uniform(-2.5, 2.5), rnd.uniform(3.0, 5.0)) for _ in range(3)]

    def fn(x, y):
        a = 0.0
        for bx, by, br in blobs:
            r = math.hypot(x - bx, y - by) / br
            if r < 1.0:
                a = max(a, (1.0 - r) ** 1.2)
        if a <= 0.02:
            return None
        return (0.85 + 0.15 * a, a * 0.9)
    return fn


TEXTURES = {
    'arc': [('arc', arc)],
    'ring': [('ring', ring)],
    # VFX-2: anel virado para a camera (ar comprimido); mesma textura do anel.
    'pulse': [('ring', ring)],
    'streak': [('streak', streak)],
    'glow': [('glow', glow)],
    'shard': [('shard_%d' % i, shard(100 + i)) for i in range(3)],
    'spore': [('spore_%d' % i, spore(200 + i)) for i in range(3)],
}


def main():
    os.makedirs(TEX, exist_ok=True)
    os.makedirs(DEF, exist_ok=True)
    for kind, variants in TEXTURES.items():
        names = []
        for name, fn in variants:
            image(fn).save(os.path.join(TEX, name + '.png'))
            names.append('kn8:' + name)
        with open(os.path.join(DEF, kind + '.json'), 'w', newline='\n') as out:
            json.dump({'textures': names}, out, indent=2)
            out.write('\n')
        print(kind, names)


if __name__ == '__main__':
    main()
