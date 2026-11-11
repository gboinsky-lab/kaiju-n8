#!/usr/bin/env python3
"""FASE 2-C (Miguel, 2026-11-11: casaco do Haruichi, opcao "b"): abas de tras de casaco longo em ossos proprios que
acompanham a coxa pela metade, como uma saia.

O rig automatico do Meshy pos a parte de tras do casaco nos ossos da coxa e da canela: ao correr a aba girava inteira
com a perna e saia em lascas. Aqui as faces de leg_<lado> e shin_<lado> que ficam atras do eixo do osso (mais que
BEHIND na direcao +Z, as costas) vao para um osso novo coat_<lado> (filho da raiz, pivo no quadril, como a coxa).
Nas animacoes, coat_<lado> recebe FOLLOW x a rotacao da coxa (mesmos keyframes e easings: a reta entre keyframes
escalada continua a mesma reta) e a posicao da coxa inteira.

Rodar DEPOIS do rig da especie (rig_from_skin.py, rig_soldier_mesh.py, rig_strays.py), que regravam os OBJ, e DEPOIS
de todo gerador de animacao da especie (humanoid_anims.py, weapon_grips.py, gen_vault_animations.py); o
vfx_keyframes.py pode vir antes ou depois (so mexe nas marcas). Rodar de novo nao move faces (os ossos ja existem) e
so regrava os canais das abas.
Uso: python3 tools/art/coat_bones.py [especie...]   (padrao: as da tabela)
"""
import json
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).parent))
from preview_rig import Rig, PX  # noqa: E402
from rig_trichonephila_mesh import write_obj  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/kn8"
# especie -> (distancia atras do eixo em metros, fracao da rotacao da coxa) [SUPOSICAO numeros, escolhidos na previa]
SPECIES = {"haruichi": (0.08, 0.5)}
CHILD = {"leg": "shin", "shin": "foot"}
SIDES = ("left", "right")


def behind(rig, bone, limit):
    kind, _, side = bone.partition("_")
    v, _, _, f = rig.meshes[bone]
    a, b = rig.pivot(bone), rig.pivot(f"{CHILD[kind]}_{side}")
    c = v[f].mean(1)
    ab = b - a
    t = np.clip(((c - a) @ ab) / (ab @ ab), 0, 1)
    axis = a + t[:, None] * ab
    return (c[:, 2] - axis[:, 2]) > limit


def split(species, limit):
    rig = Rig(species)
    index_path = ASSETS / f"meshes/{species}.json"
    index = json.loads(index_path.read_text(encoding="utf-8"))
    if all(f"coat_{s}" in index["bones"] for s in SIDES):
        print(f"{species}: abas ja separadas")
        return
    geo_path = ASSETS / f"geo/entity/{species}.geo.json"
    geo = json.loads(geo_path.read_text(encoding="utf-8"))
    bones = geo["minecraft:geometry"][0]["bones"]
    for side in SIDES:
        parts = []
        for bone in (f"leg_{side}", f"shin_{side}"):
            v, uv, n, f = rig.meshes[bone]
            mask = behind(rig, bone, limit)
            parts.append((v, uv, n, f[mask]))
            write_obj(ASSETS / index["bones"][bone].split(":")[1], v, uv, n, f[~mask])
            print(f"{species}: {bone} -> coat_{side}: {int(mask.sum())} triangulos")
        offset = 0
        vs, uvs, ns, fs = [], [], [], []
        for v, uv, n, f in parts:
            vs.append(v)
            uvs.append(uv)
            ns.append(n)
            fs.append(f + offset)
            offset += len(v)
        name = f"coat_{side}"
        path = f"meshes/{species}/{name}.obj"
        write_obj(ASSETS / path, np.vstack(vs), np.vstack(uvs), np.vstack(ns), np.vstack(fs))
        index["bones"][name] = f"kn8:{path}"
        leg = next(b for b in bones if b["name"] == f"leg_{side}")
        bones.insert(bones.index(leg) + 1, {"name": name, "pivot": list(leg["pivot"]), "parent": leg.get("parent", "root")})
    index_path.write_text(json.dumps(index, indent=2) + "\n", encoding="utf-8")
    geo_path.write_text(json.dumps(geo, indent=2) + "\n", encoding="utf-8")


def scaled(value, factor):
    if isinstance(value, list):
        return [round(x * factor, 3) for x in value]
    out = dict(value)
    for key in ("pre", "post", "vector"):
        if key in out:
            out[key] = scaled(out[key], factor)
    return out


def animate(species, follow):
    path = ASSETS / f"animations/entity/{species}.animation.json"
    raw = path.read_bytes().decode("utf-8")
    newline = "\r\n" if "\r\n" in raw else "\n"
    data = json.loads(raw)
    count = 0
    for anim in data["animations"].values():
        bones = anim.get("bones", {})
        for side in SIDES:
            bones.pop(f"coat_{side}", None)
            leg = bones.get(f"leg_{side}")
            if not leg:
                continue
            coat = {}
            if "rotation" in leg:
                rotation = leg["rotation"]
                coat["rotation"] = ({t: scaled(v, follow) for t, v in rotation.items()}
                                    if isinstance(rotation, dict) else scaled(rotation, follow))
            if "position" in leg:
                coat["position"] = leg["position"]
            if coat:
                bones[f"coat_{side}"] = coat
                count += 1
    path.write_bytes((json.dumps(data, indent=2) + "\n").replace("\n", newline).encode("utf-8"))
    print(f"{species}: {count} canais de aba")


def main():
    for species in sys.argv[1:] or list(SPECIES):
        limit, follow = SPECIES[species]
        split(species, limit)
        animate(species, follow)


if __name__ == "__main__":
    main()
