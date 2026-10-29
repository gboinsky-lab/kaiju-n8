#!/usr/bin/env python3
"""RIG-1: previa de uma especie com malha presa aos ossos (meshes/<especie>.json + geo) posada por animacoes do
arquivo da especie. Mesma conta da GeckoLib: pivo com X invertido ao carregar, giro Rz * Ry * Rx com X e Y negados
(o Bedrock gira ao contrario), posicao em px com X negado. Serve para conferir cortes e articulacoes sem abrir o jogo.

Uso: python3 tools/art/preview_rig.py <especie> <saida.png> <animacao>@<tempo>[+<animacao>@<tempo>...] ...
     cada argumento depois da saida e um quadro; animacoes somadas com "+" (ex.: movement.walk@0.2+arms.fists_walk@0.2);
     "parado" = sem animacao; sufixo ":lado" ou ":costas" muda a camera; ":ossos" pinta cada osso de uma cor.
"""
import colorsys
import json
import math
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).parent))
from rig_trichonephila_mesh import read_obj  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/kn8"
PX = 16.0
SIZE = int(__import__("os").environ.get("KN8_PREVIEW_SIZE", "320"))


def rx(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])


def ry(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])


def rz(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


def sample(channel, t):
    """Valor do canal no tempo t (linear entre keyframes; o jogo usa a mesma reta por padrao)."""
    keys = sorted((float(k), v if isinstance(v, list) else v.get("post", v.get("vector"))) for k, v in channel.items())
    if t <= keys[0][0]:
        return np.array(keys[0][1], float)
    for (t0, v0), (t1, v1) in zip(keys, keys[1:]):
        if t <= t1:
            u = (t - t0) / max(t1 - t0, 1e-9)
            return np.array(v0, float) * (1 - u) + np.array(v1, float) * u
    return np.array(keys[-1][1], float)


class Rig:
    def __init__(self, species):
        geo = json.loads((ASSETS / f"geo/entity/{species}.geo.json").read_text(encoding="utf-8"))
        self.bones = {b["name"]: b for b in geo["minecraft:geometry"][0]["bones"]}
        index = json.loads((ASSETS / f"meshes/{species}.json").read_text(encoding="utf-8"))["bones"]
        self.meshes = {bone: read_obj(ASSETS / path.split(":")[1]) for bone, path in index.items()}
        self.texture = np.asarray(Image.open(ASSETS / f"textures/entity/{species}.png").convert("RGB"), float)
        self.anims = json.loads((ASSETS / f"animations/entity/{species}.animation.json")
                                .read_text(encoding="utf-8"))["animations"]
        self.species = species

    def pivot(self, bone):
        p = self.bones[bone]["pivot"]
        return np.array([-p[0], p[1], p[2]]) / PX

    def pose(self, spec):
        """Rotacao/posicao somadas de cada osso para "anim@t+anim@t"."""
        rot, pos = {}, {}
        if spec in ("", "parado"):
            return rot, pos
        for part in spec.split("+"):
            name, _, t = part.partition("@")
            anim = self.anims[f"{self.species}.{name}"]
            for bone, channels in anim["bones"].items():
                if "rotation" in channels:
                    rot[bone] = rot.get(bone, 0) + sample(channels["rotation"], float(t or 0))
                if "position" in channels:
                    pos[bone] = pos.get(bone, 0) + sample(channels["position"], float(t or 0))
        return rot, pos

    def world(self, rot, pos):
        out = {}

        def matrix(bone):
            if bone in out:
                return out[bone]
            r = rot.get(bone, np.zeros(3))
            p = pos.get(bone, np.zeros(3))
            m = np.eye(4)
            m[:3, :3] = rz(math.radians(r[2])) @ ry(math.radians(-r[1])) @ rx(math.radians(-r[0]))
            pivot = self.pivot(bone)
            local = np.eye(4)
            local[:3, 3] = pivot + np.array([-p[0], p[1], p[2]]) / PX
            back = np.eye(4)
            back[:3, 3] = -pivot
            m = local @ m @ back
            parent = self.bones[bone].get("parent")
            out[bone] = (matrix(parent) @ m) if parent else m
            return out[bone]
        for bone in self.bones:
            matrix(bone)
        return out

    def draw(self, spec, view="frente", by_bone=False):
        rot, pos = self.pose(spec)
        world = self.world(rot, pos)
        polys = []
        names = list(self.bones)
        for bone, (v, uv, _, f) in self.meshes.items():
            m = world[bone]
            p = v @ m[:3, :3].T + m[:3, 3]
            # (x na imagem, altura, profundidade para quem olha): frente = camera em -Z olhando para o modelo.
            if view == "lado":
                p = np.c_[-p[:, 2], p[:, 1], p[:, 0]]
            elif view == "costas":
                p = np.c_[p[:, 0], p[:, 1], p[:, 2]]
            else:
                p = np.c_[-p[:, 0], p[:, 1], -p[:, 2]]
            color = None
            if by_bone:
                h = (names.index(bone) * 0.381) % 1
                color = np.array(colorsys.hsv_to_rgb(h, 0.7, 0.95)) * 255
            for face in f:
                tri = p[face]
                if color is None:
                    u = uv[face].mean(0)
                    tx = self.texture[int((1 - u[1]) * (self.texture.shape[0] - 1)) % self.texture.shape[0],
                                      int(u[0] * (self.texture.shape[1] - 1)) % self.texture.shape[1]]
                else:
                    tx = color
                n = np.cross(tri[1] - tri[0], tri[2] - tri[0])
                length = np.linalg.norm(n)
                shade = 0.45 + 0.55 * abs(n[2] / length) if length else 1
                polys.append((tri[:, 2].mean(), tri, tuple(int(c * shade) for c in tx)))
        return polys

    def render(self, frames, out):
        panels = []
        drawn = [(spec, self.draw(spec.split(":")[0], *self._opts(spec))) for spec in frames]
        allpts = np.vstack([tri for _, polys in drawn for _, tri, _ in polys])
        low, high = allpts.min(0), allpts.max(0)
        scale = (SIZE - 30) / max(high[1] - low[1], high[0] - low[0])
        for spec, polys in drawn:
            img = Image.new("RGB", (SIZE, SIZE), (236, 238, 242))
            d = ImageDraw.Draw(img)
            cx = SIZE / 2 - (low[0] + high[0]) / 2 * scale
            cy = SIZE - 12 + low[1] * scale
            d.line([(0, cy), (SIZE, cy)], fill=(170, 170, 170))
            for _, tri, color in sorted(polys, key=lambda p: p[0]):
                d.polygon([(cx + x * scale, cy - y * scale) for x, y, _ in tri], fill=color)
            d.text((4, 3), spec, fill=(0, 0, 0))
            panels.append(img)
        sheet = Image.new("RGB", (SIZE * len(panels), SIZE), (255, 255, 255))
        for i, panel in enumerate(panels):
            sheet.paste(panel, (SIZE * i, 0))
        sheet.save(out)

    @staticmethod
    def _opts(spec):
        tags = spec.split(":")[1:]
        view = "lado" if "lado" in tags else "costas" if "costas" in tags else "frente"
        return view, "ossos" in tags


if __name__ == "__main__":
    Rig(sys.argv[1]).render(sys.argv[3:], sys.argv[2])
