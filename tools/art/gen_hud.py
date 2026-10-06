#!/usr/bin/env python3
"""Arte da HUD de combate (0.2), fiel a referencia "HUD de combate avancado - estilo anime" do Miguel.

Gera texturas em alta resolucao (SCALE pixels de textura por pixel da GUI), desenhadas pelo KN8Hud com blit
escalado, para ficarem nitidas em qualquer escala de GUI:
  textures/gui/hud/frame.png        painel fixo: moldura do RELEASE, emblema hexagonal, rotulos, caixas dos icones,
                                    molduras de STAMINA/HEAT e os segmentos vazios;
  textures/gui/hud/fill_release.png segmentos acesos do RELEASE (degrade azul com brilho);
  textures/gui/hud/fill_surge.png   mesmos segmentos em laranja (Surto acima do treinado);
  textures/gui/hud/fill_stamina.png segmentos verdes;
  textures/gui/hud/fill_heat.png    segmentos vermelho -> laranja;
  textures/gui/hud/digits.png       numeros do valor ("0123456789%/ ") em pixel art com brilho, linha azul e
                                    linha laranja.
As posicoes (unidades da GUI) estao em LAYOUT e sao as mesmas constantes do KN8Hud: mudou aqui, mude la.
Emblema e icones sao design original (cabeca de fera estilizada, corredor, termometro).
Uso: python3 tools/art/gen_hud.py   (requer numpy e pillow)
"""
import json
from pathlib import Path

import numpy as np
from PIL import Image, ImageChops, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "src/main/resources/assets/kn8/textures/gui/hud"
SCALE = 4

# Unidades da GUI (painel inteiro = 214 x 97). Bate com as constantes de KN8Hud.
LAYOUT = {
    "panel": (214, 97),
    "release_bar": (50, 39, 146, 8),     # x, y, largura, altura dos segmentos do RELEASE
    "value": (69, 13, 16),               # x, y, altura da celula dos numeros (glifo = 9/13 dela)
    "stamina_bar": (71, 64, 122, 6),
    "heat_bar": (71, 85, 122, 6),
    "segments": 10,
}

PANEL = (10, 16, 30, 200)
PANEL_EDGE = (16, 24, 42, 120)
LINE = (205, 214, 224, 255)
LINE_DIM = (150, 160, 175, 170)
EMPTY = (58, 70, 88, 235)
EMPTY_EDGE = (70, 82, 98, 255)
TROUGH = (20, 26, 36, 230)
LABEL = (236, 241, 246, 255)
ICON_BOX = (14, 20, 32, 215)

# Pixel font 5x7 (design proprio, cantos chanfrados como na referencia).
FONT = {
    "0": [".###.", "#...#", "#..##", "#.#.#", "##..#", "#...#", ".###."],
    "1": ["..#..", ".##..", "..#..", "..#..", "..#..", "..#..", ".###."],
    "2": [".###.", "#...#", "....#", "...#.", "..#..", ".#...", "#####"],
    "3": [".###.", "#...#", "....#", "..##.", "....#", "#...#", ".###."],
    "4": ["...#.", "..##.", ".#.#.", "#..#.", "#####", "...#.", "...#."],
    "5": ["#####", "#....", "####.", "....#", "....#", "#...#", ".###."],
    "6": [".###.", "#....", "#....", "####.", "#...#", "#...#", ".###."],
    "7": ["#####", "....#", "...#.", "..#..", ".#...", ".#...", ".#..."],
    "8": [".###.", "#...#", "#...#", ".###.", "#...#", "#...#", ".###."],
    "9": [".###.", "#...#", "#...#", ".####", "....#", "....#", ".###."],
    "%": ["##..#", "##..#", "...#.", "..#..", ".#...", "#..##", "#..##"],
    "/": ["....#", "...#.", "...#.", "..#..", ".#...", ".#...", "#...."],
    " ": [".....", ".....", ".....", ".....", ".....", ".....", "....."],
    "A": [".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"],
    "E": ["#####", "#....", "#....", "####.", "#....", "#....", "#####"],
    "H": ["#...#", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"],
    "I": [".###.", "..#..", "..#..", "..#..", "..#..", "..#..", ".###."],
    "L": ["#....", "#....", "#....", "#....", "#....", "#....", "#####"],
    "M": ["#...#", "##.##", "#.#.#", "#.#.#", "#...#", "#...#", "#...#"],
    "N": ["#...#", "##..#", "#.#.#", "#.#.#", "#..##", "#...#", "#...#"],
    "R": ["####.", "#...#", "#...#", "####.", "#.#..", "#..#.", "#...#"],
    "S": [".####", "#....", "#....", ".###.", "....#", "....#", "####."],
    "T": ["#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#.."],
    "X": ["#...#", "#...#", ".#.#.", "..#..", ".#.#.", "#...#", "#...#"],
}
DIGITS = "0123456789%/ "
# Numeros do valor: grade 6x9 (traco mais fino que a 5x7 na mesma altura, como na referencia).
DIGIT_FONT = {
    "0": [".####.", "#....#", "#...##", "#..#.#", "#.#..#", "##...#", "#....#", "#....#", ".####."],
    "1": ["..##..", ".#.#..", "...#..", "...#..", "...#..", "...#..", "...#..", "...#..", ".#####"],
    "2": [".####.", "#....#", ".....#", ".....#", "....#.", "...#..", "..#...", ".#....", "######"],
    "3": [".####.", "#....#", ".....#", ".....#", "..###.", ".....#", ".....#", "#....#", ".####."],
    "4": ["....#.", "...##.", "..#.#.", ".#..#.", "#...#.", "######", "....#.", "....#.", "....#."],
    "5": ["######", "#.....", "#.....", "#####.", ".....#", ".....#", ".....#", "#....#", ".####."],
    "6": [".####.", "#.....", "#.....", "#####.", "#....#", "#....#", "#....#", "#....#", ".####."],
    "7": ["######", ".....#", "....#.", "...#..", "..#...", "..#...", "..#...", "..#...", "..#..."],
    "8": [".####.", "#....#", "#....#", "#....#", ".####.", "#....#", "#....#", "#....#", ".####."],
    "9": [".####.", "#....#", "#....#", "#....#", ".#####", ".....#", ".....#", ".....#", ".####."],
    "%": [".#...#", "#.#..#", ".#..#.", "...#..", "..#...", ".#..#.", "#..#.#", "#...#.", "......"],
    "/": [".....#", "....#.", "....#.", "...#..", "...#..", "..#...", "..#...", ".#....", "#....."],
    " ": ["......"] * 9,
}


def px(value):
    return int(round(value * SCALE))


def canvas(width, height):
    return Image.new("RGBA", (px(width), px(height)), (0, 0, 0, 0))


def poly(draw, points, **kwargs):
    draw.polygon([(px(x), px(y)) for x, y in points], **kwargs)


def line(draw, points, fill=LINE, width=0.5):
    draw.line([(px(x), px(y)) for x, y in points], fill=fill, width=max(1, px(width)), joint="curve")


def text(image, x, y, string, size, color, shadow=True, font=None):
    """Pixel font: cada pixel da fonte vira um quadrado de 'size' unidades da GUI."""
    font = font or FONT
    draw = ImageDraw.Draw(image)
    cursor = x
    for char in string:
        glyph = font[char]
        for row, bits in enumerate(glyph):
            for col, bit in enumerate(bits):
                if bit == "#":
                    x0, y0 = cursor + col * size, y + row * size
                    if shadow:
                        draw.rectangle([px(x0 + size * 0.4), px(y0 + size * 0.4), px(x0 + size * 1.4) - 1,
                                        px(y0 + size * 1.4) - 1], fill=(0, 0, 0, 150))
                    draw.rectangle([px(x0), px(y0), px(x0 + size) - 1, px(y0 + size) - 1], fill=color)
        cursor += (len(glyph[0]) + 1) * size


def blur_rgba(image, radius):
    """Desfoque com alfa pre-multiplicado: sem isso o preto dos pixels transparentes vaza e o brilho fica com
    caixas escuras em volta (visto em jogo)."""
    arr = np.asarray(image).astype(np.float32)
    alpha = arr[..., 3:4] / 255.0
    premultiplied = np.concatenate([arr[..., :3] * alpha, arr[..., 3:4]], axis=2)
    channels = [Image.fromarray(np.clip(premultiplied[..., i], 0, 255).astype(np.uint8)).filter(
        ImageFilter.GaussianBlur(radius)) for i in range(4)]
    out = np.stack([np.asarray(c).astype(np.float32) for c in channels], axis=2)
    a = np.maximum(out[..., 3:4], 1e-3) / 255.0
    out[..., :3] = np.where(out[..., 3:4] > 0, out[..., :3] / a, 0)
    return Image.fromarray(np.clip(out, 0, 255).astype(np.uint8))


def glow(image, radius, strength):
    """Brilho em volta das partes opacas (o "neon" dos numeros e segmentos da referencia)."""
    arr = np.asarray(blur_rgba(image, px(radius))).astype(np.float32)
    arr[..., 3] *= strength
    halo = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8))
    return Image.alpha_composite(halo, image)


def gradient_fill(mask, colors):
    """Pinta a mascara com um degrade horizontal por pontos (posicao 0..1, cor RGB) e brilho no alto."""
    width, height = mask.size
    xs = np.linspace(0, 1, width)
    stops = np.array([p for p, _ in colors])
    rgb = np.stack([np.interp(xs, stops, [c[i] for _, c in colors]) for i in range(3)], axis=1)
    out = np.zeros((height, width, 4), np.float32)
    out[..., :3] = rgb[None, :, :]
    # Faixa clara no terco de cima de cada segmento (reflexo).
    shade = np.ones(height)
    shade[: height // 3] = 1.25
    shade[-max(1, height // 5):] = 0.8
    out[..., :3] = np.clip(out[..., :3] * shade[:, None, None], 0, 255)
    out[..., 3] = np.asarray(mask).astype(np.float32)
    return Image.fromarray(out.astype(np.uint8))


def segment_shapes(x, y, width, height, slant, gap):
    count = LAYOUT["segments"]
    seg = (width - slant - gap * (count - 1)) / count
    shapes = []
    for i in range(count):
        left = x + i * (seg + gap)
        shapes.append([(left + slant, y), (left + slant + seg, y), (left + seg, y + height), (left, y + height)])
    return shapes


def segment_mask(size, origin, shapes):
    mask = Image.new("L", size, 0)
    draw = ImageDraw.Draw(mask)
    ox, oy = origin
    for shape in shapes:
        draw.polygon([(px(x - ox), px(y - oy)) for x, y in shape], fill=255)
    return mask


# ---------------------------------------------------------------------------------------------- emblema e icones
def emblem(draw, cx, cy, r):
    """Cabeca de fera estilizada (original): cranio em escudo, chifres, olhos em fenda e garras dos lados."""
    silver = (225, 230, 236, 255)
    shade = (150, 158, 170, 255)
    dark = (24, 30, 42, 255)
    # Garras/asas laterais.
    for side in (-1, 1):
        claw = [(cx + side * 0.30 * r, cy - 0.55 * r), (cx + side * 0.80 * r, cy - 0.75 * r),
                (cx + side * 0.95 * r, cy - 0.35 * r), (cx + side * 0.70 * r, cy - 0.40 * r),
                (cx + side * 0.85 * r, cy + 0.10 * r), (cx + side * 0.65 * r, cy + 0.55 * r),
                (cx + side * 0.55 * r, cy + 0.15 * r), (cx + side * 0.40 * r, cy - 0.20 * r)]
        poly(draw, claw, fill=shade)
    # Cranio.
    head = [(cx, cy - 0.70 * r), (cx + 0.40 * r, cy - 0.45 * r), (cx + 0.50 * r, cy + 0.05 * r),
            (cx + 0.25 * r, cy + 0.65 * r), (cx, cy + 0.80 * r), (cx - 0.25 * r, cy + 0.65 * r),
            (cx - 0.50 * r, cy + 0.05 * r), (cx - 0.40 * r, cy - 0.45 * r)]
    poly(draw, head, fill=silver)
    # Chifres.
    for side in (-1, 1):
        poly(draw, [(cx + side * 0.18 * r, cy - 0.60 * r), (cx + side * 0.45 * r, cy - 1.0 * r),
                    (cx + side * 0.38 * r, cy - 0.48 * r)], fill=silver)
    # Olhos em fenda e sulcos.
    for side in (-1, 1):
        poly(draw, [(cx + side * 0.10 * r, cy - 0.05 * r), (cx + side * 0.40 * r, cy - 0.18 * r),
                    (cx + side * 0.34 * r, cy + 0.04 * r)], fill=dark)
        line(draw, [(cx + side * 0.05 * r, cy + 0.25 * r), (cx + side * 0.18 * r, cy + 0.55 * r)], fill=shade,
             width=0.5)
    line(draw, [(cx, cy - 0.55 * r), (cx, cy - 0.15 * r)], fill=shade, width=0.6)


def runner(draw, x, y, s):
    """Corredor (STAMINA), silhueta branca."""
    white = (240, 244, 248, 255)
    draw.ellipse([px(x + 0.62 * s), px(y + 0.02 * s), px(x + 0.86 * s), px(y + 0.26 * s)], fill=white)
    line(draw, [(x + 0.66 * s, y + 0.30 * s), (x + 0.48 * s, y + 0.62 * s)], fill=white, width=0.16 * s)
    line(draw, [(x + 0.62 * s, y + 0.34 * s), (x + 0.90 * s, y + 0.50 * s)], fill=white, width=0.11 * s)
    line(draw, [(x + 0.62 * s, y + 0.34 * s), (x + 0.30 * s, y + 0.40 * s), (x + 0.16 * s, y + 0.58 * s)],
         fill=white, width=0.11 * s)
    line(draw, [(x + 0.48 * s, y + 0.62 * s), (x + 0.70 * s, y + 0.78 * s), (x + 0.64 * s, y + 0.98 * s)],
         fill=white, width=0.12 * s)
    line(draw, [(x + 0.48 * s, y + 0.62 * s), (x + 0.30 * s, y + 0.82 * s), (x + 0.06 * s, y + 0.84 * s)],
         fill=white, width=0.12 * s)


def thermometer(draw, x, y, s):
    """Termometro (HEAT): tubo branco, bulbo vermelho, marcas ao lado."""
    white = (236, 240, 244, 255)
    red = (230, 52, 40, 255)
    draw.rounded_rectangle([px(x + 0.36 * s), px(y + 0.02 * s), px(x + 0.58 * s), px(y + 0.72 * s)],
                           radius=px(0.11 * s), fill=white)
    draw.rectangle([px(x + 0.42 * s), px(y + 0.30 * s), px(x + 0.52 * s), px(y + 0.76 * s)], fill=red)
    draw.ellipse([px(x + 0.26 * s), px(y + 0.62 * s), px(x + 0.68 * s), px(y + 1.0 * s)], fill=red)
    for i in range(4):
        yy = y + (0.12 + 0.13 * i) * s
        draw.rectangle([px(x + 0.70 * s), px(yy), px(x + 0.82 * s), px(yy + 0.05 * s)], fill=white)


# ---------------------------------------------------------------------------------------------- frame
def frame():
    width, height = LAYOUT["panel"]
    image = canvas(width, height)
    draw = ImageDraw.Draw(image)

    # Painel do RELEASE: fundo escuro com borda que desbota para a direita.
    poly(draw, [(4, 0.5), (209, 0.5), (213.5, 5), (213.5, 34), (196, 53), (4, 53), (0.5, 49), (0.5, 4)], fill=PANEL)
    fade = canvas(width, height)
    fade_draw = ImageDraw.Draw(fade)
    poly(fade_draw, [(60, 1), (209, 1), (213, 5), (213, 34), (196, 52.5), (60, 52.5)], fill=PANEL_EDGE)
    image = Image.alpha_composite(image, fade)
    draw = ImageDraw.Draw(image)
    # Linhas de cima e o recorte do canto direito.
    line(draw, [(4, 0.5), (150, 0.5)], fill=LINE_DIM)
    line(draw, [(156, 0.5), (205, 0.5), (213.5, 8)], fill=LINE)
    line(draw, [(160, 12), (178, 32)], fill=LINE, width=0.6)  # traco diagonal decorativo
    # Moldura da barra: borda de cima longa, ponta direita inclinada e base.
    line(draw, [(47, 36), (213.5, 36), (199, 52.5), (47, 52.5)], fill=LINE, width=0.6)
    line(draw, [(205, 33), (213.5, 33)], fill=LINE, width=0.6)

    # Moldura externa do emblema (cantos chanfrados, como colchetes) e hexagono duplo dentro.
    line(draw, [(6, 0.5), (0.5, 6), (0.5, 47), (6, 52.5)], fill=LINE_DIM, width=0.5)
    line(draw, [(20, 52.5), (40, 52.5)], fill=LINE_DIM, width=0.5)
    # Emblema: hexagono duplo com pontas a esquerda e a direita.
    hexagon = [(1, 27), (12, 3), (47, 3), (58, 27), (47, 51), (12, 51)]
    poly(draw, hexagon, fill=(8, 12, 22, 235))
    line(draw, hexagon + [hexagon[0]], fill=LINE, width=0.7)
    inner = [(5, 27), (14.5, 7), (44.5, 7), (54, 27), (44.5, 47), (14.5, 47)]
    line(draw, inner + [inner[0]], fill=LINE_DIM, width=0.45)
    for (ax, ay), (bx, by) in ((inner[1], inner[2]), (inner[4], inner[5])):
        midx = (ax + bx) / 2
        line(draw, [(midx - 4, ay + (1 if ay < 27 else -1)), (midx + 4, ay + (1 if ay < 27 else -1))], fill=LINE,
             width=0.6)
    emblem(draw, 29.5, 27, 13)

    text(image, 56, 4, "RELEASE", 1.0, LABEL)

    # Segmentos vazios do RELEASE.
    bx, by, bw, bh = LAYOUT["release_bar"]
    for shape in segment_shapes(bx, by, bw, bh, 3, 1.6):
        poly(draw, shape, fill=EMPTY)

    # Linhas de STAMINA e HEAT.
    for row_y, label, icon in ((57, "STAMINA", runner), (78, "HEAT", thermometer)):
        draw.rounded_rectangle([px(0.5), px(row_y), px(18.5), px(row_y + 18)], radius=px(2.5), fill=ICON_BOX,
                               outline=LINE_DIM, width=max(1, px(0.35)))
        icon(draw, 3, row_y + 2.5, 13)
        poly(draw, [(20, row_y), (212, row_y), (203, row_y + 18), (20, row_y + 18)], fill=PANEL)
        line(draw, [(66, row_y + 1), (212, row_y + 1), (203, row_y + 17.5), (66, row_y + 17.5)], fill=LINE,
             width=0.55)
        text(image, 24, row_y + 6, label, 0.86, LABEL)
        key = "stamina_bar" if label == "STAMINA" else "heat_bar"
        x, y, w, h = LAYOUT[key]
        # Trilho continuo (na referencia so os segmentos acesos aparecem divididos).
        draw.rounded_rectangle([px(x - 1), px(y - 1), px(x + w + 1), px(y + h + 1)], radius=px(1.5), fill=TROUGH)
        draw.rounded_rectangle([px(x), px(y), px(x + w), px(y + h)], radius=px(1), fill=(48, 56, 70, 235))
    image.save(OUT / "frame.png")


def fill(name, key, slant, gap, colors, glow_radius):
    """Faixa dos segmentos acesos (mesma caixa da barra + margem do brilho)."""
    x, y, w, h = LAYOUT[key]
    margin = 2
    size = (px(w + 2 * margin), px(h + 2 * margin))
    shapes = segment_shapes(x, y, w, h, slant, gap)
    mask = segment_mask(size, (x - margin, y - margin), shapes)
    image = gradient_fill(mask, colors)
    image = glow(image, glow_radius, 0.85)
    image.save(OUT / f"fill_{name}.png")
    return image.size


def digits():
    """Atlas de numeros: uma celula por caractere de DIGITS; linha 0 azul, linha 1 laranja (Surto)."""
    cell_w, cell_h = 10, 13  # pixels da fonte: glifo 6x9 com 2 de margem para o brilho sumir antes da borda
    size = 1.0
    rows = [((150, 215, 255, 255), (20, 110, 255)), ((255, 205, 120, 255), (255, 100, 10))]
    sheet = Image.new("RGBA", (px(cell_w * len(DIGITS)), px(cell_h * len(rows))), (0, 0, 0, 0))
    for r, (core, halo) in enumerate(rows):
        for i, char in enumerate(DIGITS):
            cell = Image.new("RGBA", (px(cell_w), px(cell_h)), (0, 0, 0, 0))
            text(cell, 2, 2, char, size, (*halo, 255), shadow=False, font=DIGIT_FONT)
            cell = glow(cell, 0.5, 0.6)
            core_layer = Image.new("RGBA", cell.size, (0, 0, 0, 0))
            text(core_layer, 2, 2, char, size, core, shadow=False, font=DIGIT_FONT)
            # Halo fino so na borda de cada pixel: traco nitido como na referencia.
            core_layer = Image.alpha_composite(cell, core_layer)
            # Miolo mais claro (nucleo do neon) por cima do halo.
            cell = core_layer
            sheet.paste(cell, (px(i * cell_w), px(r * cell_h)))
    sheet.save(OUT / "digits.png")
    return sheet.size


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    frame()
    sizes = {
        "release": fill("release", "release_bar", 3, 1.6, [(0, (120, 210, 255)), (1, (30, 110, 230))], 0.9),
        "surge": fill("surge", "release_bar", 3, 1.6, [(0, (255, 200, 90)), (1, (255, 120, 20))], 0.9),
        "stamina": fill("stamina", "stamina_bar", 0, 1.4, [(0, (70, 200, 90)), (0.6, (120, 240, 120)),
                                                         (1, (80, 210, 100))], 0.7),
        "heat": fill("heat", "heat_bar", 0, 1.4, [(0, (240, 40, 30)), (0.5, (255, 90, 30)), (1, (255, 150, 40))], 0.7),
    }
    sizes["digits"] = digits()
    # Filtro linear: a arte e desenhada reduzida (SCALE pixels por unidade da GUI) e sem ele as linhas finas somem.
    for name in ("frame", "fill_release", "fill_surge", "fill_stamina", "fill_heat", "digits"):
        (OUT / f"{name}.png.mcmeta").write_text('{"texture": {"blur": true}}\n', encoding="utf-8")
    print(json.dumps({"scale": SCALE, "layout": LAYOUT, "textures": sizes}))


if __name__ == "__main__":
    main()
