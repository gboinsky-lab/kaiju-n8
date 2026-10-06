#!/usr/bin/env python3
"""Previa (fora do jogo) das armas na mao: jogador em 3a pessoa (parado e com pose de mira CROSSBOW_HOLD), 1a pessoa
e o Soldado 1 em cada pose de arma. Reproduz a cadeia de transformacoes do jogo:

- jogador (vanilla 1.21.1, ItemInHandLayer): braco (ModelPart) -> Rx(-90) Ry(180) -> T(+-1/16, 2/16, -10/16) ->
  display "thirdperson_*" do JSON do item -> T(-0,5) -> modelo OBJ;
- 1a pessoa (ItemInHandRenderer): T(+-0,56, -0,52, -0,72) -> display "firstperson_*" -> T(-0,5);
- soldado (GeckoLib + SoldierRenderer): ossos do .geo.json com a animacao -> pivo do item_right -> alinha ao braco
  (ombro -> mao) -> T(0, -1/16, -2/16) -> Rx(-90) -> display "thirdperson_righthand" -> T(-0,5).

Uso: python3 tools/art/preview_held_items.py [saida.png]   (padrao: build/preview_held_items.png)
Requer numpy e pillow. Serve para conferir orientacao e posicao; luz e cores sao aproximadas.
"""
import json
import math
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/kn8"


# ------------------------------------------------------------------------------------------------ matrizes
def T(x, y, z):
    m = np.eye(4)
    m[:3, 3] = (x, y, z)
    return m


def S(x, y, z):
    return np.diag([x, y, z, 1.0])


def Rx(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0, 0], [0, c, -s, 0], [0, s, c, 0], [0, 0, 0, 1.0]])


def Ry(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s, 0], [0, 1, 0, 0], [-s, 0, c, 0], [0, 0, 0, 1.0]])


def Rz(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0, 0], [s, c, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1.0]])


def deg(d):
    return math.radians(d)


def rotation_to(a, b):
    """Rotacao minima que leva a direcao a em b (como Quaternionf.rotationTo)."""
    a = np.asarray(a, float) / np.linalg.norm(a)
    b = np.asarray(b, float) / np.linalg.norm(b)
    v = np.cross(a, b)
    c = float(np.dot(a, b))
    k = np.array([[0, -v[2], v[1]], [v[2], 0, -v[0]], [-v[1], v[0], 0]])
    r = np.eye(3) + k + k @ k / (1 + c)
    m = np.eye(4)
    m[:3, :3] = r
    return m


def apply(m, pts):
    p = np.c_[pts, np.ones(len(pts))] @ m.T
    return p[:, :3]


# ------------------------------------------------------------------------------------------------ modelos
def read_obj(path, flip_v=True):
    v, t, f = [], [], []
    for line in Path(path).read_text(encoding="utf-8").splitlines():
        p = line.split()
        if not p:
            continue
        if p[0] == "v":
            v.append([float(x) for x in p[1:4]])
        elif p[0] == "vt":
            t.append([float(x) for x in p[1:3]])
        elif p[0] == "f":
            idx = [[int(i) - 1 for i in c.split("/")] for c in p[1:]]
            for k in range(1, len(idx) - 1):
                f.append([idx[0], idx[k], idx[k + 1]])
    f = np.array(f)
    uv = np.array(t)[f[:, :, 1]]
    if flip_v:
        uv[:, :, 1] = 1 - uv[:, :, 1]
    return np.array(v)[f[:, :, 0]], uv


def item(name):
    model = json.loads((ASSETS / f"models/item/{name}.json").read_text(encoding="utf-8"))
    tris, uv = read_obj(ASSETS / model["model"].split(":")[1])
    tex = Image.open(ASSETS / f"textures/{model['textures']['texture'].split(':')[1]}.png").convert("RGBA")
    return tris, uv, tex, model.get("display", {})


def display(disp, context, left=False):
    """ItemTransform.apply do vanilla (inclui o espelho da mao esquerda)."""
    d = disp.get(context, {})
    rx, ry, rz = d.get("rotation", [0, 0, 0])
    tx, ty, tz = d.get("translation", [0, 0, 0])
    sx, sy, sz = d.get("scale", [1, 1, 1])
    if left:
        ry, rz, tx = -ry, -rz, -tx
    return T(tx / 16, ty / 16, tz / 16) @ Rx(deg(rx)) @ Ry(deg(ry)) @ Rz(deg(rz)) @ S(sx, sy, sz) @ T(-0.5, -0.5, -0.5)


def box(x, y, z, w, h, d):
    """Cubo em pixels -> triangulos em blocos."""
    lo = np.array([x, y, z]) / 16
    hi = lo + np.array([w, h, d]) / 16
    c = np.array([[lo[0] if i & 1 == 0 else hi[0], lo[1] if i & 2 == 0 else hi[1], lo[2] if i & 4 == 0 else hi[2]]
                  for i in range(8)])
    quads = [(0, 1, 3, 2), (4, 6, 7, 5), (0, 4, 5, 1), (2, 3, 7, 6), (0, 2, 6, 4), (1, 5, 7, 3)]
    return np.array([[c[a], c[b], c[cc]] for a, b, cc, dd in quads] + [[c[a], c[cc], c[dd]] for a, b, cc, dd in quads])


# ------------------------------------------------------------------------------------------------ rasterizador
class Canvas:
    def __init__(self, w, h, bg=(150, 190, 230)):
        self.w, self.h = w, h
        self.img = np.zeros((h, w, 3), np.float32)
        self.img[:] = bg
        self.z = np.full((h, w), np.inf, np.float32)

    def draw(self, tris, project, uv=None, tex=None, color=(200, 200, 200)):
        tex = np.asarray(tex, np.float32) if tex is not None else None
        scr = project(tris.reshape(-1, 3)).reshape(-1, 3, 3)
        n = np.cross(tris[:, 1] - tris[:, 0], tris[:, 2] - tris[:, 0])
        n /= np.linalg.norm(n, axis=1, keepdims=True) + 1e-12
        light = np.array([0.2, 0.9, 0.4]) / np.linalg.norm([0.2, 0.9, 0.4])
        shade = 0.55 + 0.45 * np.abs(n @ light)
        for i, ((x0, y0, z0), (x1, y1, z1), (x2, y2, z2)) in enumerate(scr):
            if not np.isfinite([x0, y0, x1, y1, x2, y2]).all():
                continue
            minx, maxx = max(int(min(x0, x1, x2)), 0), min(int(max(x0, x1, x2)) + 1, self.w - 1)
            miny, maxy = max(int(min(y0, y1, y2)), 0), min(int(max(y0, y1, y2)) + 1, self.h - 1)
            if minx > maxx or miny > maxy:
                continue
            den = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2)
            if abs(den) < 1e-12:
                continue
            xs, ys = np.meshgrid(np.arange(minx, maxx + 1) + 0.5, np.arange(miny, maxy + 1) + 0.5)
            a = ((y1 - y2) * (xs - x2) + (x2 - x1) * (ys - y2)) / den
            b = ((y2 - y0) * (xs - x2) + (x0 - x2) * (ys - y2)) / den
            c = 1 - a - b
            m = (a >= 0) & (b >= 0) & (c >= 0)
            z = a * z0 + b * z1 + c * z2
            zb = self.z[miny:maxy + 1, minx:maxx + 1]
            m &= z < zb
            if not m.any():
                continue
            if tex is not None:
                u = a * uv[i, 0, 0] + b * uv[i, 1, 0] + c * uv[i, 2, 0]
                v = a * uv[i, 0, 1] + b * uv[i, 1, 1] + c * uv[i, 2, 1]
                th, tw = tex.shape[:2]
                col = tex[np.clip((v * th).astype(int), 0, th - 1), np.clip((u * tw).astype(int), 0, tw - 1)]
                m &= col[..., 3] > 25
                rgb = col[..., :3]
            else:
                rgb = np.broadcast_to(np.array(color, np.float32), m.shape + (3,))
            zb[m] = z[m]
            self.img[miny:maxy + 1, minx:maxx + 1][m] = rgb[m] * shade[i]

    def image(self, label=""):
        im = Image.fromarray(np.clip(self.img, 0, 255).astype(np.uint8))
        if label:
            ImageDraw.Draw(im).text((6, 6), label, fill=(0, 0, 0))
        return im


def ortho(eye, center, scale, w, h, up=(0, 1, 0)):
    f = -np.asarray(eye, float)
    f /= np.linalg.norm(f)
    r = np.cross(f, up)
    r /= np.linalg.norm(r)
    u = np.cross(r, f)
    c = np.asarray(center, float)

    def project(p):
        d = p - c
        return np.stack([w / 2 + (d @ r) * scale, h / 2 - (d @ u) * scale, d @ f], axis=1)
    return project


def perspective(fov_y, w, h):
    """Camera na origem olhando para -Z (espaco da camera do Minecraft)."""
    k = (h / 2) / math.tan(math.radians(fov_y) / 2)

    def project(p):
        z = -p[:, 2]
        z = np.where(z < 0.01, np.nan, z)
        return np.stack([w / 2 + p[:, 0] / z * k, h / 2 - p[:, 1] / z * k, z], axis=1)
    return project


# ------------------------------------------------------------------------------------------------ jogador vanilla
PLAYER_PARTS = {
    # nome: (pivo px, cubo px, cor) no espaco do modelo vanilla (Y para baixo, frente -Z)
    "head": ((0, 0, 0), (-4, -8, -4, 8, 8, 8), (200, 160, 120)),
    "body": ((0, 0, 0), (-4, 0, -2, 8, 12, 4), (70, 170, 170)),
    "right_arm": ((-5, 2, 0), (-3, -2, -2, 4, 12, 4), (200, 160, 120)),
    "left_arm": ((5, 2, 0), (-1, -2, -2, 4, 12, 4), (200, 160, 120)),
    "right_leg": ((-1.9, 12, 0), (-2, 0, -2, 4, 12, 4), (60, 60, 160)),
    "left_leg": ((1.9, 12, 0), (-2, 0, -2, 4, 12, 4), (60, 60, 160)),
}


def part_matrix(name, rot):
    px, py, pz = PLAYER_PARTS[name][0]
    xr, yr, zr = rot.get(name, (0, 0, 0))
    return T(px / 16, py / 16, pz / 16) @ Rz(zr) @ Ry(yr) @ Rx(xr)


def player_view(weapon, pose, eye):
    tris, uv, tex, disp = item(weapon)
    rot = {}
    if pose == "crossbow_hold":
        rot = {"right_arm": (-math.pi / 2 + 0.1, -0.3, 0), "left_arm": (-1.5, 0.6, 0)}
    # LivingEntityRenderer: Ry(180 - yaw), escala do jogador 0,9375, S(-1,-1,1), T(0,-1,501,0).
    entity = Ry(math.pi) @ S(0.9375, 0.9375, 0.9375) @ S(-1, -1, 1) @ T(0, -1.501, 0)
    canvas = Canvas(420, 420)
    project = ortho(eye, (0, 1.0, -0.4), 150, 420, 420)
    for name, (_, cube, color) in PLAYER_PARTS.items():
        canvas.draw(apply(entity @ part_matrix(name, rot), box(*cube).reshape(-1, 3)).reshape(-1, 3, 3), project,
                    color=color)
    hand = entity @ part_matrix("right_arm", rot) @ Rx(-math.pi / 2) @ Ry(math.pi) @ T(1 / 16, 0.125, -0.625)
    m = hand @ display(disp, "thirdperson_righthand")
    canvas.draw(apply(m, tris.reshape(-1, 3)).reshape(-1, 3, 3), project, uv, tex)
    return canvas.image(f"jogador {weapon} {pose}")


def first_person(weapon):
    tris, uv, tex, disp = item(weapon)
    canvas = Canvas(480, 270)
    m = T(0.56, -0.52, -0.72) @ display(disp, "firstperson_righthand")
    canvas.draw(apply(m, tris.reshape(-1, 3)).reshape(-1, 3, 3), perspective(70, 480, 270), uv, tex)
    return canvas.image(f"1a pessoa {weapon}")


# ------------------------------------------------------------------------------------------------ soldado
def keyframe(channel, time):
    if channel is None:
        return None
    if isinstance(channel, list):
        return channel
    frames = sorted((float(t), v) for t, v in channel.items())
    if time <= frames[0][0]:
        return frames[0][1]
    for (t0, v0), (t1, v1) in zip(frames, frames[1:]):
        if t0 <= time <= t1:
            k = (time - t0) / (t1 - t0) if t1 > t0 else 0
            return [a + (b - a) * k for a, b in zip(v0, v1)]
    return frames[-1][1]


def soldier_view(weapon, anims_used, time, eye):
    geo = json.loads((ASSETS / "geo/entity/soldier.geo.json").read_text(encoding="utf-8"))
    bones = {b["name"]: b for b in geo["minecraft:geometry"][0]["bones"]}
    anims = json.loads((ASSETS / "animations/entity/soldier.animation.json").read_text(encoding="utf-8"))["animations"]
    rot, pos = {}, {}
    for name in anims_used:  # ultimo vence (como os controllers da GeckoLib)
        for bone, ch in anims[name]["bones"].items():
            r = keyframe(ch.get("rotation"), time)
            p = keyframe(ch.get("position"), time)
            if r is not None:
                rot[bone] = r
            if p is not None:
                pos[bone] = p

    def pivot(name):
        x, y, z = bones[name]["pivot"]
        return np.array([-x, y, z]) / 16  # a GeckoLib inverte o X ao carregar

    def matrix(name):
        b = bones[name]
        parent = matrix(b["parent"]) if "parent" in b else np.eye(4)
        kx, ky, kz = rot.get(name, (0, 0, 0))
        px, py, pz = pos.get(name, (0, 0, 0))
        p = pivot(name)
        # GeckoLib: X e Y da rotacao invertidos (como o Blockbench), ordem Z, Y, X.
        r = Rz(deg(kz)) @ Ry(deg(-ky)) @ Rx(deg(-kx))
        return parent @ T(-px / 16, py / 16, pz / 16) @ T(*p) @ r @ T(*(-p))

    entity = Ry(math.pi)  # GeoEntityRenderer: Ry(180 - yaw), yaw 0
    canvas = Canvas(420, 420)
    project = ortho(eye, (0, 1.0, -0.4), 150, 420, 420)
    tex = Image.open(ASSETS / "textures/entity/soldier.png").convert("RGBA")
    index = json.loads((ASSETS / "meshes/soldier.json").read_text(encoding="utf-8"))["bones"]
    for bone, path in index.items():
        t, u = read_obj(ASSETS / path.split(":")[1])
        canvas.draw(apply(entity @ matrix(bone), t.reshape(-1, 3)).reshape(-1, 3, 3), project, u, tex)
    if weapon:
        tris, uv, itex, disp = item(weapon)
        arm = pivot("arm_right")
        hand = pivot("item_right")
        m = entity @ matrix("item_right") @ T(*hand) @ rotation_to((0, -1, 0), hand - arm) \
            @ T(0, -1 / 16, -2 / 16) @ Rx(-math.pi / 2) @ display(disp, "thirdperson_righthand")
        canvas.draw(apply(m, tris.reshape(-1, 3)).reshape(-1, 3, 3), project, uv, itex)
    return canvas.image(f"soldado {weapon or 'sem arma'} {anims_used[-1].split('.', 1)[1]} t={time}")


def grid(images, columns):
    w, h = images[0].size
    rows = (len(images) + columns - 1) // columns
    out = Image.new("RGB", (w * columns, h * rows), (255, 255, 255))
    for i, im in enumerate(images):
        out.paste(im.resize((w, h)), ((i % columns) * w, (i // columns) * h))
    return out


def main():
    """Gera <saida>_jogador.png e <saida>_soldado.png (padrao: build/preview_held_items_*.png)."""
    base = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "build/preview_held_items.png"
    side, front = (1, 0.25, 0.15), (0.35, 0.25, 1)
    player = []
    for weapon in ("rifle", "pistol", "sword", "combat_knife"):
        player += [player_view(weapon, "parado", side), player_view(weapon, "crossbow_hold", side),
                   player_view(weapon, "crossbow_hold", front), first_person(weapon)]
    soldier = []
    for weapon, pose in (("rifle", "rifle"), ("pistol", "pistol"), ("sword", "blade"), (None, "unarmed")):
        idle = ["soldier.movement.idle"]
        soldier += [soldier_view(weapon, idle + [f"soldier.arms.{pose}_ready"], 0, side),
                    soldier_view(weapon, idle + [f"soldier.arms.{pose}_ready"], 0, front),
                    soldier_view(weapon, idle + [f"soldier.arms.{pose}_aim"], 0, side),
                    soldier_view(weapon, idle + [f"soldier.arms.{pose}_aim"], 0, front)]
    soldier += [soldier_view("rifle", ["soldier.movement.walk", "soldier.arms.rifle_walk"], 0.0, side),
                soldier_view("sword", ["soldier.movement.walk", "soldier.arms.blade_walk"], 0.45, side),
                soldier_view("sword", ["soldier.movement.idle", "soldier.action.attack"], 0.15, side),
                soldier_view("sword", ["soldier.movement.idle", "soldier.action.attack"], 0.28, side)]
    base.parent.mkdir(parents=True, exist_ok=True)
    for suffix, images in (("jogador", player), ("soldado", soldier)):
        out = base.with_name(f"{base.stem}_{suffix}.png")
        grid(images, 4).save(out)
        print(f"previa salva em {out}")


if __name__ == "__main__":
    main()
