"""Peles provisorias dos civis (PM-10A): 4 variantes 64x64 no formato da pele do jogador (largo), so a camada de
baixo. Roupa comum (camisa, calca, sapato), cabelo e rosto simples com um pouco de sombreado por linha. O Miguel
manda o modelo proprio depois; ate la o civil usa o modelo do jogador.

Uso: python3 tools/art/gen_civilian_skins.py
"""
import os
import random

from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), '..', '..', 'src', 'main', 'resources', 'assets', 'kn8', 'textures',
                   'entity', 'civilian')

# (pele, cabelo, camisa, calca, sapato, manga_curta)
VARIANTS = [
    ((224, 182, 140), (52, 36, 26), (86, 128, 178), (58, 62, 74), (40, 34, 30), True),
    ((198, 150, 112), (24, 22, 22), (196, 82, 74), (46, 52, 86), (30, 30, 34), False),
    ((236, 200, 168), (150, 104, 60), (232, 228, 214), (112, 92, 70), (70, 52, 40), True),
    ((156, 112, 82), (30, 24, 20), (70, 140, 96), (60, 60, 62), (28, 26, 26), False),
]


def shade(color, factor):
    return tuple(max(0, min(255, int(c * factor))) for c in color)


def fill(img, box, color, rng, noise=0.05):
    x0, y0, x1, y1 = box
    for y in range(y0, y1):
        # Mais escuro embaixo de cada face: volume sem textura detalhada.
        row = 1.0 - 0.12 * (y - y0) / max(1, y1 - y0 - 1)
        for x in range(x0, x1):
            img.putpixel((x, y), shade(color, row * (1.0 + rng.uniform(-noise, noise))) + (255,))


def box_faces(img, u, v, w, h, d, color, rng):
    """Faces de uma caixa no atlas do Minecraft: topo/baixo em (u+d, v), lados em (u, v+d)."""
    fill(img, (u + d, v, u + d + w, v + d), shade(color, 1.08), rng)
    fill(img, (u + d + w, v, u + d + 2 * w, v + d), shade(color, 0.8), rng)
    fill(img, (u, v + d, u + 2 * (d + w), v + d + h), color, rng)


def limb(img, u, v, top_color, bottom_color, split, rng):
    """Braco/perna 4x12x4: faixa de cima (manga/calca) e de baixo (pele/sapato) a partir da linha split."""
    box_faces(img, u, v, 4, 12, 4, top_color, rng)
    fill(img, (u + 8, v, u + 12, v + 4), shade(bottom_color, 0.8), rng)
    fill(img, (u, v + 4 + split, u + 16, v + 16), bottom_color, rng)


def make(index, variant):
    skin, hair, shirt, pants, shoes, short = variant
    rng = random.Random(1000 + index)
    img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    # Cabeca 8x8x8 em (0, 0).
    box_faces(img, 0, 0, 8, 8, 8, skin, rng)
    fill(img, (8, 0, 16, 8), hair, rng)                  # topo
    fill(img, (0, 8, 32, 10), hair, rng)                 # franja e lados de cima
    fill(img, (24, 8, 32, 14), hair, rng)                # nuca
    fill(img, (0, 10, 2, 13), hair, rng)                 # costeleta direita
    fill(img, (22, 10, 24, 13), hair, rng)               # costeleta esquerda
    face = (8, 8)
    for x in (face[0] + 1, face[0] + 5):
        img.putpixel((x, face[1] + 4), (250, 250, 250, 255))
        img.putpixel((x + 1, face[1] + 4), (40, 40, 52, 255))
    for x in range(face[0] + 3, face[0] + 5):
        img.putpixel((x, face[1] + 6), shade(skin, 0.7) + (255,))
    # Tronco 8x12x4 em (16, 16).
    box_faces(img, 16, 16, 8, 12, 4, shirt, rng)
    fill(img, (20, 30, 28, 32), shade(pants, 0.9), rng)  # cintura (calca)
    # Bracos: direito (40, 16), esquerdo (32, 48); manga curta = 4 px de camisa.
    sleeve = 4 if short else 11
    limb(img, 40, 16, shirt, skin, sleeve, rng)
    limb(img, 32, 48, shirt, skin, sleeve, rng)
    # Pernas: direita (0, 16), esquerda (16, 48); sapato nas 2 linhas de baixo.
    limb(img, 0, 16, pants, shoes, 10, rng)
    limb(img, 16, 48, pants, shoes, 10, rng)
    return img


def main():
    os.makedirs(OUT, exist_ok=True)
    for i, variant in enumerate(VARIANTS):
        path = os.path.join(OUT, f'civilian_{i}.png')
        make(i, variant).save(path)
        print('gravado', os.path.normpath(path))


if __name__ == '__main__':
    main()
