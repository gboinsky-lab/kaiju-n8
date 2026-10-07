"""Sombreamento de pixel art no estilo dos itens do Minecraft (0.3): cada forma ganha contorno escuro por fora,
luz nas bordas de cima/esquerda e sombra nas de baixo/direita. Usado pelos icones de itens e blocos.

Uso: desenhe cada parte num Image "L" (mascara 0/255) e chame paint(canvas, mascara, cor). A ordem importa: o que vem
depois cobre o que veio antes, e o contorno so e desenhado onde ainda esta transparente.
"""
from PIL import Image, ImageDraw


def shade(color, amount):
    """Clareia (amount > 0) ou escurece (< 0) uma cor RGB."""
    return tuple(max(0, min(255, int(c + (255 - c) * amount if amount > 0 else c * (1 + amount)))) for c in color)


def mask(size=16):
    image = Image.new("L", (size, size), 0)
    return image, ImageDraw.Draw(image)


def paint(canvas, shape, color, outline=True, light=0.28, dark=-0.32, outline_amount=-0.62):
    w, h = shape.size
    m = shape.load()
    px = canvas.load()
    inside = lambda x, y: 0 <= x < w and 0 <= y < h and m[x, y] > 0
    for y in range(h):
        for x in range(w):
            if not inside(x, y):
                continue
            c = color
            if not inside(x - 1, y) or not inside(x, y - 1):
                c = shade(color, light)
            elif not inside(x + 1, y) or not inside(x, y + 1):
                c = shade(color, dark)
            px[x, y] = c + (255,)
    if outline:
        line = shade(color, outline_amount) + (255,)
        for y in range(h):
            for x in range(w):
                if inside(x, y) or px[x, y][3] > 0:
                    continue
                if any(inside(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                    px[x, y] = line


def dot(canvas, x, y, color):
    canvas.putpixel((x, y), tuple(color) + (255,))


def new(size=16):
    return Image.new("RGBA", (size, size), (0, 0, 0, 0))
