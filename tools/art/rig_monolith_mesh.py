#!/usr/bin/env python3
"""NUMERADOS-B: Kaiju No. 14 (modelo do Miguel de 2026-11-05: monolito preto com quatro mascaras nos cantos).

Na obra o No. 14 e um bloco que flutua (levitacao, nao asas), atira raios pelas faces e se protege com um campo de
forca. Sem membros: a malha inteira vai num osso so ("body", filho de "root") e as animacoes mexem o bloco inteiro:
flutuar subindo e descendo com um giro lento, inclinar para a frente no raio, girar no pulso, subir e cair no golpe
de area. O monolito fica 1 m acima do chao (HOVER) dentro da caixa de colisao.

Entrada: tools/art/converted/kaiju_no14/kaiju_no14.obj + .png (meshy_convert.py). Saida: meshes/kaiju_no14.json +
meshes/kaiju_no14/body.obj, textures/entity/kaiju_no14.png, geo/entity/kaiju_no14.geo.json e
animations/entity/kaiju_no14.animation.json.
Uso: python3 tools/art/rig_monolith_mesh.py
"""
import json
import shutil
import sys
from pathlib import Path

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).parent))
from pad_texture import DILATE_STEPS, dilate, uv_mask  # noqa: E402
from rig_trichonephila_mesh import read_obj, write_obj  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/kn8"
NAME = "kaiju_no14"
PX = 16
HOVER = 1.0
TEXTURE_SIZE = 1024


def rot(*frames):
    return {f"{t:.2f}": {"vector": list(v), "easing": "easeinoutsine"} for t, v in frames}


def anim(length, body_rot=None, body_pos=None, loop=False, scale=None):
    bones = {}
    if body_rot:
        bones.setdefault("body", {})["rotation"] = rot(*body_rot)
    if body_pos:
        bones.setdefault("body", {})["position"] = rot(*body_pos)
    if scale:
        bones.setdefault("body", {})["scale"] = rot(*scale)
    data = {"animation_length": length, "bones": bones}
    if loop:
        data["loop"] = True
    return data


def ability_ticks(name):
    data = json.loads((ROOT / f"src/main/resources/data/kn8/kn8/ability/{name}.json").read_text(encoding="utf-8"))
    hit = data["windup_ticks"] / 20.0
    return hit, hit + data.get("active_ticks", 2) / 20.0 + 0.5


def animations():
    p = f"{NAME}."
    out = {
        # Flutua: sobe e desce meio bloco e gira devagar (a obra: bloco suspenso, sem tocar o chao).
        p + "movement.idle": anim(4.0, body_rot=[(0, (0, 0, 0)), (2, (0, 8, 0)), (4, (0, 0, 0))],
                                  body_pos=[(0, (0, 0, 0)), (2, (0, 8, 0)), (4, (0, 0, 0))], loop=True),
        p + "movement.walk": anim(2.0, body_rot=[(0, (6, 0, 2)), (1, (6, 4, -2)), (2, (6, 0, 2))],
                                  body_pos=[(0, (0, 4, 0)), (1, (0, 10, 0)), (2, (0, 4, 0))], loop=True),
        p + "overlay.breathe": anim(3.0, scale=[(0, (1, 1, 1)), (1.5, (1.02, 1.0, 1.02)), (3, (1, 1, 1))], loop=True),
        p + "reaction.hurt": anim(0.3, body_rot=[(0, (0, 0, 0)), (0.08, (-6, 0, 4)), (0.3, (0, 0, 0))]),
    }
    hit, end = ability_ticks("no14_face_beam")
    out[p + "action.beam"] = anim(end, body_rot=[(0, (0, 0, 0)), (hit * 0.8, (-10, 0, 0)), (hit, (14, 0, 0)),
                                                 (hit + 0.2, (14, 0, 0)), (end, (0, 0, 0))])
    hit, end = ability_ticks("no14_beam_volley")
    out[p + "action.volley"] = anim(end, body_rot=[(0, (0, 0, 0)), (hit, (0, 90, 0)), (end, (0, 180, 0))],
                                    body_pos=[(0, (0, 0, 0)), (hit, (0, 12, 0)), (end, (0, 0, 0))])
    hit, end = ability_ticks("no14_pulse")
    out[p + "action.pulse"] = anim(end, scale=[(0, (1, 1, 1)), (hit * 0.8, (0.92, 0.92, 0.92)),
                                               (hit, (1.12, 1.12, 1.12)), (end, (1, 1, 1))])
    hit, end = ability_ticks("no14_crushing_field")
    out[p + "action.slam"] = anim(end, body_pos=[(0, (0, 0, 0)), (hit * 0.8, (0, 40, 0)), (hit, (0, -14, 0)),
                                                 (hit + 0.25, (0, -14, 0)), (end, (0, 0, 0))],
                                  body_rot=[(0, (0, 0, 0)), (hit * 0.8, (0, 45, 0)), (hit, (0, 45, 0)),
                                            (end, (0, 0, 0))])
    # Teleporte curto (salto do kaiju): some para baixo e volta.
    hit, end = ability_ticks("no14_blink")
    out[p + "action.leap"] = anim(end, scale=[(0, (1, 1, 1)), (hit * 0.5, (0.2, 1.4, 0.2)), (end - 0.2, (0.2, 1.4, 0.2)),
                                              (end, (1, 1, 1))])
    out[p + "action.attack"] = out[p + "action.pulse"]
    out[p + "action.claw"] = out[p + "action.pulse"]
    return out


def main():
    source = ROOT / "tools/art/converted" / NAME
    vertices, uvs, normals, faces = read_obj(source / f"{NAME}.obj")
    low, high = vertices.min(axis=0), vertices.max(axis=0)
    vertices = vertices - np.array([(low[0] + high[0]) / 2, low[1] - HOVER, (low[2] + high[2]) / 2])
    mesh_dir = ASSETS / "meshes" / NAME
    if mesh_dir.exists():
        shutil.rmtree(mesh_dir)
    write_obj(mesh_dir / "body.obj", vertices, uvs, normals, faces)
    (ASSETS / "meshes" / f"{NAME}.json").write_text(json.dumps(
        {"bones": {"body": f"kn8:meshes/{NAME}/body.obj"}}, indent=2) + "\n", encoding="utf-8")
    image = Image.open(source / f"{NAME}.png").convert("RGBA")
    mask = uv_mask([source / f"{NAME}.obj"], image.width, image.height)
    padded = dilate(np.asarray(image)[..., :3], mask, DILATE_STEPS)
    Image.fromarray(np.clip(padded, 0, 255).astype(np.uint8)).convert("RGBA") \
        .resize((TEXTURE_SIZE, TEXTURE_SIZE), Image.Resampling.BOX).save(ASSETS / f"textures/entity/{NAME}.png")
    center = float((vertices[:, 1].min() + vertices[:, 1].max()) / 2)
    span = float(np.max(vertices.max(axis=0) - vertices.min(axis=0)))
    geo = {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": f"geometry.{NAME}", "texture_width": 16, "texture_height": 16,
                        "visible_bounds_width": round(span + 2, 1), "visible_bounds_height": round(span + 2, 1),
                        "visible_bounds_offset": [0, round(center, 1), 0]},
        "bones": [{"name": "root", "pivot": [0, 0, 0]},
                  {"name": "body", "parent": "root", "pivot": [0, round(center * PX, 3), 0]}]}]}
    (ASSETS / f"geo/entity/{NAME}.geo.json").write_text(json.dumps(geo, indent=2) + "\n", encoding="utf-8")
    (ASSETS / f"animations/entity/{NAME}.animation.json").write_text(json.dumps(
        {"format_version": "1.8.0", "animations": animations()}, indent=2) + "\n", encoding="utf-8")
    print(f"{NAME}: {len(faces)} triangulos, altura {vertices[:, 1].max():.2f} m (flutua {HOVER} m)")


if __name__ == "__main__":
    main()
