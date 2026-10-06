#!/usr/bin/env python3
"""Prepara a textura de uma malha do Meshy para o Minecraft: borda nas ilhas de UV + reducao de tamanho.

Por que: entidades no Minecraft nao usam mipmap e amostram "nearest". Uma textura 1024 num soldado de 1,9 m fica
~400 texels por metro: de longe ela "cintila" (serrilhado) e a borda de cada triangulo pega o fundo preto entre as
ilhas (riscos pretos/brancos nas costuras). Correcao: (1) espalha a cor de cada ilha para fora alguns texels
(dilatacao), para a costura pegar a cor certa; (2) reduz a textura com filtro de area para um tamanho proporcional.
As UVs sao normalizadas (0..1), entao a malha nao muda.

Uso: python3 tools/art/pad_texture.py <textura.png> <malha1.obj> [malha2.obj ...] [--size 512] [--out saida.png]
Requer so numpy e pillow.
"""
import argparse

import numpy as np
from PIL import Image, ImageDraw

DILATE_STEPS = 16


def uv_mask(objs, width, height):
    mask = Image.new("L", (width, height), 0)
    draw = ImageDraw.Draw(mask)
    for path in objs:
        uvs, faces = [], []
        for line in open(path, encoding="utf-8"):
            p = line.split()
            if not p:
                continue
            if p[0] == "vt":
                uvs.append((float(p[1]), float(p[2])))
            elif p[0] == "f":
                faces.append([int(c.split("/")[1]) - 1 for c in p[1:]])
        for face in faces:
            # OBJ tem V para cima; a imagem tem Y para baixo.
            draw.polygon([(uvs[i][0] * width, (1.0 - uvs[i][1]) * height) for i in face], fill=255, outline=255)
    return np.asarray(mask) > 0


def dilate(rgb, mask, steps):
    rgb = rgb.astype(np.float32)
    filled = mask.copy()
    for _ in range(steps):
        total = np.zeros_like(rgb)
        count = np.zeros(mask.shape, np.float32)
        for dy in (-1, 0, 1):
            for dx in (-1, 0, 1):
                if dx == 0 and dy == 0:
                    continue
                shifted = np.roll(np.roll(filled, dy, 0), dx, 1)
                total += np.roll(np.roll(rgb, dy, 0), dx, 1) * shifted[..., None]
                count += shifted
        grow = (~filled) & (count > 0)
        rgb[grow] = total[grow] / count[grow][:, None]
        filled |= grow
    return rgb


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("texture")
    parser.add_argument("objs", nargs="+")
    parser.add_argument("--size", type=int, default=512)
    parser.add_argument("--out")
    args = parser.parse_args()
    image = Image.open(args.texture).convert("RGBA")
    rgb = np.asarray(image)[..., :3]
    mask = uv_mask(args.objs, image.width, image.height)
    padded = dilate(rgb, mask, DILATE_STEPS)
    out = Image.fromarray(np.clip(padded, 0, 255).astype(np.uint8)).convert("RGBA")
    if args.size != image.width:
        out = out.resize((args.size, args.size), Image.Resampling.BOX)
    out.save(args.out or args.texture)
    print(f"{args.texture}: {mask.mean() * 100:.0f}% coberto pelas UVs, borda de {DILATE_STEPS} texels, "
          f"{image.width} -> {args.size}")


if __name__ == "__main__":
    main()
