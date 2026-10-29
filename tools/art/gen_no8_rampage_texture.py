#!/usr/bin/env python3
"""Textura do Kaiju No. 8 sem controle (Miguel, 2026-10-29: "na forma dele descontrolada as linhas ciano e o olho ciano
em vermelho"): copia textures/entity/kaiju_no8.png trocando so o ciano (tom 140-220 graus com alguma saturacao) por
vermelho, com o mesmo brilho; o resto do corpo (cinza-escuro, cranio branco) fica igual.
Uso: python3 tools/art/gen_no8_rampage_texture.py   (rodar de novo se a textura do No. 8 mudar)
"""
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
TEXTURES = ROOT / "src/main/resources/assets/kn8/textures/entity"
HUE_RANGE = (140.0, 220.0)
MIN_SATURATION = 0.12
# Vermelho do descontrole (tom 0) um pouco mais saturado que o ciano original, para ler de longe.
SATURATION_BOOST = 1.15


def main():
    image = Image.open(TEXTURES / "kaiju_no8.png").convert("RGBA")
    rgba = np.asarray(image).astype(float) / 255.0
    rgb = rgba[..., :3]
    high, low = rgb.max(axis=2), rgb.min(axis=2)
    spread = np.maximum(high - low, 1e-6)
    saturation = np.where(high > 0, (high - low) / np.maximum(high, 1e-6), 0.0)
    r, g, b = rgb[..., 0], rgb[..., 1], rgb[..., 2]
    hue = np.where(high == r, ((g - b) / spread) % 6, np.where(high == g, (b - r) / spread + 2, (r - g) / spread + 4))
    hue *= 60.0
    cyan = (hue >= HUE_RANGE[0]) & (hue <= HUE_RANGE[1]) & (saturation >= MIN_SATURATION)
    value = high
    sat = np.clip(saturation * SATURATION_BOOST, 0.0, 1.0)
    red = np.stack([value, value * (1 - sat), value * (1 - sat)], axis=2)
    out = rgb.copy()
    out[cyan] = red[cyan]
    result = np.concatenate([out, rgba[..., 3:]], axis=2)
    Image.fromarray(np.clip(result * 255.0 + 0.5, 0, 255).astype(np.uint8), "RGBA") \
        .save(TEXTURES / "kaiju_no8_rampage.png")
    print(f"{int(cyan.sum())} pixels ciano -> vermelho")


if __name__ == "__main__":
    main()
