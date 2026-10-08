#!/usr/bin/env python3
"""Auditoria de texturas e materiais (0.5.0-C2, Biblioteca v22 Prioridade 2).

Confere, sem abrir o jogo, tudo que tem textura no mod:
- malhas das entidades (meshes/<especie>.json + um OBJ por osso, textura textures/entity/<especie>.png) e dos
  trajes (meshes/suit/<item>.json, textures/models/suit/<item>.png);
- modelos OBJ das armas (models/item/<arma>.json com loader neoforge:obj) e modelos comuns de item/bloco
  (texturas citadas existem);
- ossos das malhas que nao existem no .geo.json (a malha nao apareceria).

Por triangulo de cada malha: UV fora de [0, 1] e textura amostrada em 4 pontos (centro e perto de cada canto). Face
com a maioria das amostras transparentes (alpha < 16) = "sem textura" (aparece como buraco); amostra rosa/magenta
pura = textura de erro. As texturas do Meshy ja saem preenchidas (pad_texture), entao tambem conta:
- UV esticada: triangulo cuja area na UV e mais de 25x a mediana (por area 3D) atravessa ilhas da textura e vira
  uma faixa de cores erradas (o defeito das "linhas douradas" da aranha na 0.1-B);
- bordas abertas: arestas de um osso que so tem um triangulo. So informativo: a GeckoLib desenha as malhas com
  entityCutoutNoCull (confirmado no GeoModel#getRenderType), entao a borda aberta mostra o lado de dentro com
  textura, nao um buraco; e o Meshy faz espinhos e asas como laminas abertas. O cap_holes dos scripts de rig tampa
  os contornos fechados do corte entre ossos.
Uso: python3 tools/art/audit_textures.py [saida.md] (padrao: build/audit_textures.md; o resumo com as conclusoes fica
em docs/AUDITORIA_TEXTURAS.md)
"""
import json
import sys
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/kn8"
ALPHA_HOLE = 16
STRETCH = 25.0
COLOR_SPREAD = 60
# Acima disso de faces com problema o asset precisa de atencao (abaixo: costuras de poucos triangulos).
ATTENTION = 0.005
SAMPLES = np.array([[1 / 3, 1 / 3, 1 / 3], [0.6, 0.2, 0.2], [0.2, 0.6, 0.2], [0.2, 0.2, 0.6]])


def read_obj(path, positions=False):
    verts, vfaces = [], []
    uvs, faces = [], []
    for line in path.read_text(encoding="utf-8").splitlines():
        parts = line.split()
        if not parts:
            continue
        if parts[0] == "v":
            verts.append([float(parts[1]), float(parts[2]), float(parts[3])])
        elif parts[0] == "vt":
            uvs.append([float(parts[1]), float(parts[2])])
        elif parts[0] == "f":
            corners = [p.split("/") for p in parts[1:]]
            uv_index = [int(c[1]) - 1 if len(c) > 1 and c[1] else -1 for c in corners]
            v_index = [int(c[0]) - 1 for c in corners]
            # Poligonos com mais de 3 cantos viram leque de triangulos.
            for k in range(1, len(uv_index) - 1):
                faces.append([uv_index[0], uv_index[k], uv_index[k + 1]])
                vfaces.append([v_index[0], v_index[k], v_index[k + 1]])
    result = np.array(uvs, dtype=float).reshape(-1, 2), np.array(faces, dtype=int).reshape(-1, 3)
    if positions:
        return result + (np.array(verts, dtype=float).reshape(-1, 3), np.array(vfaces, dtype=int).reshape(-1, 3))
    return result


def stretched_and_open(obj_path, texture):
    """Triangulos com UV esticada que atravessa ilhas (a cor muda mais que COLOR_SPREAD dentro dele) e arestas
    abertas (soldando vertices pela posicao)."""
    uvs, faces, verts, vfaces = read_obj(obj_path, positions=True)
    if len(faces) == 0 or (faces < 0).any():
        return 0, 0
    tri = uvs[faces]
    a, b = tri[:, 1] - tri[:, 0], tri[:, 2] - tri[:, 0]
    uv_area = 0.5 * np.abs(a[:, 0] * b[:, 1] - a[:, 1] * b[:, 0])
    p = verts[vfaces]
    area = 0.5 * np.linalg.norm(np.cross(p[:, 1] - p[:, 0], p[:, 2] - p[:, 0]), axis=1)
    ratio = uv_area / np.maximum(area, 1e-12)
    valid = area > 1e-9
    median = np.median(ratio[valid]) if valid.any() else 0
    candidates = np.where((ratio > STRETCH * median) & valid & (uv_area > 1e-5))[0] if median > 0 else []
    height, width = texture.shape[:2]
    stretched = 0
    for face in candidates:
        points = SAMPLES @ tri[face]
        x = np.clip((points[:, 0] % 1.0) * width, 0, width - 1).astype(int)
        y = np.clip((1.0 - points[:, 1] % 1.0) * height, 0, height - 1).astype(int)
        if np.ptp(texture[y, x, :3].astype(int), axis=0).max() > COLOR_SPREAD:
            stretched += 1
    _, weld = np.unique(np.round(verts, 4), axis=0, return_inverse=True)
    w = weld.reshape(-1)[vfaces]
    edges = np.sort(np.concatenate([w[:, [0, 1]], w[:, [1, 2]], w[:, [2, 0]]]), axis=1)
    _, counts = np.unique(edges, axis=0, return_counts=True)
    return stretched, int((counts == 1).sum())


def resource(location, folder, suffix):
    namespace, path = location.split(":", 1) if ":" in location else ("minecraft", location)
    return ROOT / "src/main/resources/assets" / namespace / folder / f"{path}{suffix}", namespace


def audit_mesh(obj_path, texture):
    """Devolve (triangulos, fora de [0,1], sem textura, magenta)."""
    uvs, faces = read_obj(obj_path)
    if len(faces) == 0:
        return 0, 0, 0, 0
    if (faces < 0).any():
        return len(faces), len(faces), 0, 0
    tri = uvs[faces]
    outside = int(((tri < -1e-4) | (tri > 1 + 1e-4)).any(axis=(1, 2)).sum())
    height, width = texture.shape[:2]
    points = np.einsum("sk,fkc->fsc", SAMPLES, tri)
    x = np.clip((points[..., 0] % 1.0) * width, 0, width - 1).astype(int)
    y = np.clip((1.0 - (points[..., 1] % 1.0)) * height, 0, height - 1).astype(int)
    pixels = texture[y, x]
    holes = int(((pixels[..., 3] < ALPHA_HOLE).sum(axis=1) > len(SAMPLES) // 2).sum())
    magenta = int(((pixels[..., 0] > 240) & (pixels[..., 1] < 16) & (pixels[..., 2] > 240)).any(axis=1).sum())
    return len(faces), outside, holes, magenta


def load_texture(path):
    return np.array(Image.open(path).convert("RGBA"))


def geo_bones(species):
    path = ASSETS / "geo/entity" / f"{species}.geo.json"
    if not path.exists():
        return None
    data = json.loads(path.read_text(encoding="utf-8"))
    return {bone["name"] for geometry in data["minecraft:geometry"] for bone in geometry.get("bones", [])}


def audit_index(name, index_path, texture_path, geo, rows, problems):
    if not texture_path.exists():
        problems.append(f"{name}: textura nao existe ({texture_path.relative_to(ROOT)})")
        return
    texture = load_texture(texture_path)
    bones = json.loads(index_path.read_text(encoding="utf-8"))["bones"]
    totals = np.zeros(6, dtype=int)
    for bone, location in bones.items():
        obj, _ = resource(location, "", "")
        if not obj.exists():
            problems.append(f"{name}/{bone}: OBJ nao existe ({location})")
            continue
        if geo is not None and bone not in geo:
            problems.append(f"{name}/{bone}: osso da malha nao existe no .geo.json (nao aparece)")
        result = np.array(audit_mesh(obj, texture) + stretched_and_open(obj, texture))
        totals += result
        if result[0] and (result[1] + result[2] + result[3] + result[4]) / result[0] > ATTENTION:
            problems.append(f"{name}/{bone}: {result[2]} sem textura, {result[3]} magenta, {result[1]} UV fora, "
                            f"{result[4]} UV esticada, de {result[0]} triangulos")
    rows.append((name, *(int(v) for v in totals), f"{texture.shape[1]}x{texture.shape[0]}"))


def audit_item_models(rows, problems):
    for model in sorted((ASSETS / "models/item").glob("*.json")) + sorted((ASSETS / "models/block").glob("*.json")):
        data = json.loads(model.read_text(encoding="utf-8"))
        for key, location in data.get("textures", {}).items():
            if location.startswith("#"):
                continue
            png, namespace = resource(location, "textures", ".png")
            if namespace == "kn8" and not png.exists():
                problems.append(f"{model.name}: textura '{key}' nao existe ({location})")
        if data.get("loader") == "neoforge:obj":
            obj, _ = resource(data["model"], "", "")
            location = data.get("textures", {}).get("texture")
            png = resource(location, "textures", ".png")[0] if location else None
            if not obj.exists() or png is None or not png.exists():
                problems.append(f"{model.name}: OBJ ou textura da arma nao existe")
                continue
            image = load_texture(png)
            result = audit_mesh(obj, image) + stretched_and_open(obj, image)
            rows.append((f"item/{model.stem}", *result, "obj"))
            if result[0] and (result[1] + result[2] + result[3]) / result[0] > ATTENTION:
                problems.append(f"item/{model.stem}: {result[2]} sem textura, {result[3]} magenta, {result[1]} UV "
                                f"fora, de {result[0]} triangulos")


def main():
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "build/audit_textures.md"
    rows, problems = [], []
    for index in sorted((ASSETS / "meshes").glob("*.json")):
        species = index.stem
        audit_index(species, index, ASSETS / "textures/entity" / f"{species}.png", geo_bones(species), rows,
                    problems)
    for index in sorted((ASSETS / "meshes/suit").glob("*.json")):
        audit_index(f"suit/{index.stem}", index, ASSETS / "textures/models/suit" / f"{index.stem}.png", None, rows,
                    problems)
    audit_item_models(rows, problems)
    lines = ["| Asset | Triangulos | UV fora de [0,1] | Sem textura | Magenta | UV esticada | Arestas abertas | Textura |",
             "|---|---|---|---|---|---|---|---|"]
    lines += ["| " + " | ".join(str(v) for v in row) + " |" for row in rows]
    lines += ["", "Problemas:" if problems else "Nenhum problema encontrado."] + [f"- {p}" for p in problems]
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print("\n".join(lines))


if __name__ == "__main__":
    main()
