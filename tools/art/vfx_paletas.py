"""VFX-0 (diretriz KaijuVFX): paleta dominante e cores de destaque de cada textura de entidade, para as fichas de
identidade visual (docs/VFX_FICHAS_IDENTIDADE.md).
Uso: python3 tools/art/vfx_paletas.py [saida.png]
Paleta = k-medias de 6 cores nos pixels opacos; destaque = k-medias de 3 nos pixels saturados (s > 0,45, v > 0,35).
A folha mostra a paleta a esquerda e os destaques (borda branca) a direita."""
import sys, glob, os, random, colorsys
from PIL import Image, ImageDraw

TEX = os.path.join(os.path.dirname(__file__), '..', '..', 'src', 'main', 'resources', 'assets', 'kn8', 'textures',
                   'entity')


def kmeans(px, k=6, it=12):
    random.seed(1)
    cent = random.sample(px, k)
    groups = []
    for _ in range(it):
        groups = [[] for _ in cent]
        for p in px:
            j = min(range(len(cent)), key=lambda i: sum((p[c] - cent[i][c]) ** 2 for c in range(3)))
            groups[j].append(p)
        cent = [tuple(sum(c[i] for c in g) // len(g) for i in range(3)) if g else cent[n]
                for n, g in enumerate(groups)]
    return sorted(zip([len(g) for g in groups], cent), reverse=True)


def pixels(path, size):
    im = Image.open(path).convert('RGBA')
    im.thumbnail((size, size))
    return [p[:3] for p in im.getdata() if p[3] > 200]


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else 'build/vfx_paletas.png'
    names = sorted(os.path.basename(f)[:-4] for f in glob.glob(TEX + '/*.png'))
    sheet = Image.new('RGB', (900, 26 * len(names) + 10), (30, 30, 30))
    draw = ImageDraw.Draw(sheet)
    for row, name in enumerate(names):
        px = pixels(f'{TEX}/{name}.png', 96)
        if len(px) > 4000:
            px = random.Random(2).sample(px, 4000)
        palette = kmeans(px)
        total = sum(s for s, _ in palette)
        sat = [p for p in pixels(f'{TEX}/{name}.png', 128)
               if (lambda h, s, v: s > 0.45 and v > 0.35)(*colorsys.rgb_to_hsv(*(c / 255 for c in p)))]
        accents = kmeans(random.Random(3).sample(sat, min(len(sat), 2000)), k=3) if len(sat) >= 30 else []
        y = 6 + row * 26
        draw.text((6, y + 6), name, fill=(230, 230, 230))
        x = 200
        for _, c in palette:
            draw.rectangle([x, y, x + 50, y + 20], fill=c)
            x += 54
        x += 20
        for _, c in accents:
            draw.rectangle([x, y, x + 50, y + 20], fill=c, outline=(255, 255, 255))
            x += 54
        print(name.ljust(26), ' '.join('#%02X%02X%02X:%d%%' % (*c, round(100 * s / total)) for s, c in palette),
              '| destaques', ' '.join('#%02X%02X%02X' % c for _, c in accents))
    os.makedirs(os.path.dirname(out) or '.', exist_ok=True)
    sheet.save(out)


if __name__ == '__main__':
    main()
