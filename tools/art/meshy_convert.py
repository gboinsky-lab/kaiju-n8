#!/usr/bin/env python3
"""Converte modelos GLB do Meshy AI para o mod (Etapa A).

Armas -> modelo de item OBJ do NeoForge (loader "neoforge:obj"): textura reduzida, escala real, orientacao de
"espada vanilla" (ponta para cima e para a direita no quadrado do item, empunhadura no canto de baixo), para os
display transforms padrao de item segurado servirem de ponto de partida. Ajuste fino: abrir o JSON do item no
Blockbench (aba Display) e copiar os valores de volta.

Entidades (soldado, aranha, bainhas) -> OBJ + textura em tools/art/converted/<nome>/, prontos para o renderizador de
malha presa aos ossos da GeckoLib (proxima etapa). O soldado e dividido nas partes da segmentacao do Meshy.

Uso: python3 tools/art/meshy_convert.py --src <pasta com os GLB>
Requer: pip install trimesh fast_simplification scipy pillow
"""
import argparse
import json
import math
from pathlib import Path

import numpy as np
import trimesh
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/kn8"
CONVERTED = ROOT / "tools/art/converted"
CONFIG = Path(__file__).with_name("meshy_assets.json")
AXES = {"x": 0, "y": 1, "z": 2}
PIXEL = 1.0 / 16.0
# Ponto do quadrado do item (em pixels) onde fica a empunhadura, como numa espada vanilla.
GRIP_PIXEL = np.array([5.0, 5.0, 8.0]) * PIXEL
# Tamanho no item: 1 m de arma = 1,15 unidade (uma espada vanilla tem ~1,2 na diagonal).
ITEM_UNITS_PER_METER = 1.15


def load_single(path):
    scene = trimesh.load(path, force="scene")
    meshes = []
    for node in scene.graph.nodes_geometry:
        transform, name = scene.graph.get(node)
        mesh = scene.geometry[name].copy()
        mesh.apply_transform(transform)
        meshes.append(mesh)
    return meshes


def texture_of(mesh, size):
    image = mesh.visual.material.baseColorTexture
    if image is None:
        raise SystemExit("Modelo sem textura base: confira o export do Meshy.")
    return image.convert("RGBA").resize((size, size), Image.Resampling.LANCZOS)


def decimate(mesh, max_triangles):
    """Reduz triangulos sem misturar ilhas de UV.

    A reducao funde vertices de costura (mesma posicao, UVs de ilhas diferentes). Antes, cada vertice restante
    pegava a UV do vertice original mais proximo, e um triangulo podia ficar com cantos em ilhas diferentes (bug da
    textura da Trichonephila na 0.1-B). Agora a UV e decidida POR TRIANGULO: acha-se o triangulo original mais perto
    do centro do triangulo reduzido e os tres cantos usam as coordenadas baricentricas dele (todos na mesma ilha).
    O resultado tem um vertice por canto (normais suaves da malha reduzida)."""
    if max_triangles is None or len(mesh.faces) <= max_triangles:
        return mesh
    import fast_simplification
    from scipy.spatial import cKDTree
    from trimesh.triangles import closest_point, points_to_barycentric
    ratio = 1.0 - max_triangles / len(mesh.faces)
    # Solda os vertices pela posicao antes de reduzir: o GLB do Meshy separa os vertices nas costuras da UV e a
    # reducao tratava cada costura como borda, abrindo buracos na malha (visto nos Primigenius de 2026-10-06).
    shape = trimesh.Trimesh(mesh.vertices.copy(), mesh.faces.copy(), process=False)
    shape.merge_vertices(merge_tex=True, merge_norm=True)
    points, faces = fast_simplification.simplify(shape.vertices, shape.faces, target_reduction=ratio)
    welded = trimesh.Trimesh(points, faces, process=False)
    # Triangulo original mais perto de cada centro: 8 candidatos pelo centro, decide a distancia real ao triangulo.
    centers = welded.triangles_center
    candidates = 8
    _, near = cKDTree(mesh.triangles_center).query(centers, k=candidates)
    flat = near.ravel()
    projected = closest_point(mesh.triangles[flat], np.repeat(centers, candidates, axis=0))
    distance = np.linalg.norm(projected - np.repeat(centers, candidates, axis=0), axis=1).reshape(-1, candidates)
    source = near[np.arange(len(near)), np.argmin(distance, axis=1)]
    corners = points[faces].reshape(-1, 3)
    source_triangles = np.repeat(mesh.triangles[source], 3, axis=0)
    source_uv = np.repeat(mesh.visual.uv[mesh.faces[source]], 3, axis=0)
    bary = points_to_barycentric(source_triangles, corners)
    uv = np.einsum("ij,ijk->ik", bary, source_uv)
    normals = welded.vertex_normals[faces].reshape(-1, 3)
    reduced = trimesh.Trimesh(corners, np.arange(len(corners)).reshape(-1, 3), vertex_normals=normals,
                              process=False)
    reduced.visual = trimesh.visual.TextureVisuals(uv=uv, material=mesh.visual.material)
    return reduced


def write_obj(path, vertices, uvs, normals, faces, material_ref):
    path.parent.mkdir(parents=True, exist_ok=True)
    mtl = path.with_suffix(".mtl")
    mtl.write_text(f"newmtl main\nKd 1.0 1.0 1.0\nmap_Kd {material_ref}\n", encoding="utf-8")
    lines = [f"mtllib {mtl.name}", f"o {path.stem}", "usemtl main"]
    lines += [f"v {x:.5f} {y:.5f} {z:.5f}" for x, y, z in vertices]
    # 0.5.0-C2: UV presa em [0, 1]; o Meshy deixa ate 0,2% de sobra e a textura repete (puxa pixels do outro lado).
    lines += [f"vt {min(max(u, 0.0), 1.0):.5f} {min(max(v, 0.0), 1.0):.5f}" for u, v in uvs]
    lines += [f"vn {x:.4f} {y:.4f} {z:.4f}" for x, y, z in normals]
    for a, b, c in faces + 1:
        lines.append(f"f {a}/{a}/{a} {b}/{b}/{b} {c}/{c}/{c}")
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def weapon_frame(spec, vertices):
    """Matriz que leva o arquivo original para o quadrado do item (ponta -> diagonal +X+Y, lado -> +Z)."""
    length_axis = AXES[spec["length_axis"]]
    up_axis = AXES[spec["up_axis"]]
    side_axis = 3 - length_axis - up_axis
    tip_sign = -1.0 if spec["tip"] == "-" else 1.0
    low, high = vertices.min(axis=0), vertices.max(axis=0)
    length = high[length_axis] - low[length_axis]
    # Tamanho no estilo Minecraft (a mao do jogador e enorme): item_length na tabela; sem ele, escala real.
    item_length = spec.get("item_length", spec["length_m"] * ITEM_UNITS_PER_METER)
    scale = item_length / length
    tip_value = low[length_axis] if tip_sign < 0 else high[length_axis]
    grip_along = tip_value - tip_sign * spec["grip_from_tip"] * length
    grip_up = low[up_axis] + spec["grip_from_bottom"] * (high[up_axis] - low[up_axis])
    grip_side = (low[side_axis] + high[side_axis]) / 2.0
    grip = np.zeros(3)
    grip[length_axis], grip[up_axis], grip[side_axis] = grip_along, grip_up, grip_side
    if spec.get("orientation", "diagonal") == "horizontal":
        # Arma de fogo: cano para a direita no quadrado do item (vira "para a frente" na mao).
        diagonal = np.array([1.0, 0.0, 0.0])
        across = np.array([0.0, 1.0, 0.0])
    else:
        # Lamina: diagonal de espada vanilla (ponta para cima e para a direita).
        diagonal = np.array([1.0, 1.0, 0.0]) / math.sqrt(2.0)
        across = np.array([-1.0, 1.0, 0.0]) / math.sqrt(2.0)
    depth = np.array([0.0, 0.0, 1.0])
    rotation = np.zeros((3, 3))
    rotation[:, length_axis] = diagonal * tip_sign
    rotation[:, up_axis] = across
    rotation[:, side_axis] = depth
    # Garante base direita (sem espelhar o modelo).
    if np.linalg.det(rotation) < 0:
        rotation[:, side_axis] = -rotation[:, side_axis]
    return rotation, grip, scale


GUI_FILL = 0.9
GUI_MAX_SCALE = 2.5


def gui_fit(vertices):
    """Icone do inventario: enquadra a arma no quadrado (armas pequenas ficam legiveis; o tamanho na mao nao muda)."""
    low, high = vertices.min(axis=0), vertices.max(axis=0)
    extent = max(high[0] - low[0], high[1] - low[1])
    scale = min(GUI_MAX_SCALE, GUI_FILL / extent)
    center = (low + high) / 2
    translation = [round((0.5 - center[0]) * 16 * scale, 3), round((0.5 - center[1]) * 16 * scale, 3), 0]
    return {"rotation": [0, 0, 0], "translation": translation, "scale": [round(scale, 3)] * 3}


# Arma de fogo na mao (0.1-B): cano ao longo do braco (Ry 90), cabo para o lado de tras do braco; o ponto
# "hand_grip" do item (coordenadas do OBJ ja convertido, medido em tools/art/preview_held_items.py) vai para o centro
# do punho. Alvo em pixels no quadro do display: 3a pessoa (0, -2, 1); 1a pessoa (-1, 2, 2) com 3 graus para o centro.
GUN_THIRD = {"rotation_y": 90, "target": (0.0, -2.0, 1.0), "scale": 0.85}
GUN_FIRST = {"rotation_y": 93, "target": (-1.0, 2.0, 2.0)}


def gun_transform(grip, rotation_y, target, scale):
    g = [(c - 0.5) * 16 * scale for c in grip]
    angle = math.radians(rotation_y)
    rotated = (g[0] * math.cos(angle) + g[2] * math.sin(angle), g[1], -g[0] * math.sin(angle) + g[2] * math.cos(angle))
    translation = [round(target[i] - rotated[i], 3) for i in range(3)]
    right = {"rotation": [0, rotation_y, 0], "translation": translation, "scale": [scale] * 3}
    left = {"rotation": [0, -rotation_y, 0], "translation": translation, "scale": [scale] * 3}
    return right, left


# 0.2 (pedido do Miguel): armas maiores na mao do JOGADOR. O punho fica no mesmo ponto da mao (a translacao e
# recalculada). Padrao abaixo; cada arma pode ter "held_scale": [3a pessoa, 1a pessoa] na tabela (o machado, que ja
# era grande, cresce menos). O soldado le a escala do display e volta ao tamanho de referencia (0,85) no
# SoldierRenderer, entao nao muda.
HELD_SCALE_THIRD = 1.3
HELD_SCALE_FIRST = 1.2


def euler_xyz(degrees):
    """Rotacao do display do item no Minecraft: Quaternionf().rotationXYZ(x, y, z) = Rx * Ry * Rz."""
    x, y, z = (math.radians(d) for d in degrees)
    rx = np.array([[1, 0, 0], [0, math.cos(x), -math.sin(x)], [0, math.sin(x), math.cos(x)]])
    ry = np.array([[math.cos(y), 0, math.sin(y)], [0, 1, 0], [-math.sin(y), 0, math.cos(y)]])
    rz = np.array([[math.cos(z), -math.sin(z), 0], [math.sin(z), math.cos(z), 0], [0, 0, 1]])
    return rx @ ry @ rz


def blade_transform(rotation, translation, scale, factor):
    """Display de lamina aumentado por 'factor' com a empunhadura (GRIP_PIXEL) parada no mesmo lugar da mao:
    ponto final = T + R * (s * g), entao T' = T + 16 * R * (s - s') * g (T em pixels, g em blocos)."""
    grip = GRIP_PIXEL - 0.5
    new_scale = round(scale * factor, 3)
    shift = euler_xyz(rotation) @ (grip * (scale - new_scale)) * 16
    moved = [round(translation[i] + shift[i], 3) for i in range(3)]
    return {"rotation": rotation, "translation": moved, "scale": [new_scale] * 3}


def mirrored(transform):
    rotation = transform["rotation"]
    return {**transform, "rotation": [rotation[0], -rotation[1], -rotation[2]]}


def held_display(spec):
    """Display de mao: lamina = espada vanilla; arma de fogo = gun_transform com o hand_grip da tabela."""
    held_third, held_first = spec.get("held_scale", [HELD_SCALE_THIRD, HELD_SCALE_FIRST])
    if spec.get("orientation") != "horizontal" or "hand_grip" not in spec:
        third = blade_transform([0, -90, 55], [0, 4.0, 0.5], 0.85, held_third)
        first = blade_transform([0, -90, 25], [1.13, 3.2, 1.13], 0.68, held_first)
        # Mao esquerda: mesma translacao e rotacao espelhada (o jogo espelha o X ao aplicar), como no vanilla.
        return {"thirdperson_righthand": third, "thirdperson_lefthand": mirrored(third),
                "firstperson_righthand": first, "firstperson_lefthand": mirrored(first)}
    third_right, third_left = gun_transform(spec["hand_grip"], GUN_THIRD["rotation_y"], GUN_THIRD["target"],
                                            round(GUN_THIRD["scale"] * held_third, 3))
    first_right, first_left = gun_transform(spec["hand_grip"], GUN_FIRST["rotation_y"], GUN_FIRST["target"],
                                            round(spec.get("first_person_scale", 0.7) * held_first, 3))
    return {"thirdperson_righthand": third_right, "thirdperson_lefthand": third_left,
            "firstperson_righthand": first_right, "firstperson_lefthand": first_left}


def convert_weapon(name, spec, src):
    mesh = load_single(src / spec["source"])[0]
    rotation, grip, scale = weapon_frame(spec, mesh.vertices)
    # Espessura: escala os eixos "cima" e lateral (nao o comprimento) em volta da empunhadura.
    length_axis = AXES[spec["length_axis"]]
    thickness = np.ones(3) * spec.get("thickness", 1.0)
    thickness[length_axis] = 1.0
    local = (mesh.vertices - grip) * thickness
    if "blade_thickness" in spec:
        # So a lamina: da ponta ate blade_fraction do comprimento (o cabo fica como esta).
        low, high = mesh.vertices.min(axis=0), mesh.vertices.max(axis=0)
        length = high[length_axis] - low[length_axis]
        tip = low[length_axis] if spec["tip"] == "-" else high[length_axis]
        from_tip = np.abs(mesh.vertices[:, length_axis] - tip) / length
        blade = from_tip < spec["blade_fraction"]
        factor = np.ones(3) * spec["blade_thickness"]
        factor[length_axis] = 1.0
        # Engrossa em volta do eixo central da lamina (centro do corte transversal), nao da empunhadura.
        center = mesh.vertices[blade].mean(axis=0) - grip
        center[length_axis] = 0.0
        local[blade] = (local[blade] - center) * factor + center
    vertices = local @ rotation.T * scale + GRIP_PIXEL
    normals = mesh.vertex_normals @ rotation.T
    texture_of(mesh, spec["texture"]).save(ASSETS / f"textures/item/{name}.png")
    write_obj(ASSETS / f"models/item/{name}.obj", vertices, mesh.visual.uv, normals, mesh.faces, "#texture")
    model = {
        "loader": "neoforge:obj",
        "model": f"kn8:models/item/{name}.obj",
        "flip_v": True,
        "automatic_culling": False,
        "textures": {"texture": f"kn8:item/{name}", "particle": f"kn8:item/{name}"},
        "display": {
            **held_display(spec),
            "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
            "gui": gui_fit(vertices),
            "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
        },
    }
    (ASSETS / f"models/item/{name}.json").write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")
    low, high = vertices.min(axis=0), vertices.max(axis=0)
    print(f"arma {name}: {len(mesh.faces)} tri, caixa {np.round(low, 2)} -> {np.round(high, 2)}")


def normalize_entity(meshes, spec):
    """Escala real, pes em Y = 0, centro em X/Z = 0, frente para -Z (o Meshy exporta de frente para +Z)."""
    combined = trimesh.util.concatenate(meshes) if len(meshes) > 1 else meshes[0]
    low, high = combined.bounds
    if "height_m" in spec:
        scale = spec["height_m"] / (high[1] - low[1])
    else:
        scale = spec["length_m"] / max(high[0] - low[0], high[2] - low[2])
    center = np.array([(low[0] + high[0]) / 2, low[1], (low[2] + high[2]) / 2])
    turn = trimesh.transformations.rotation_matrix(math.pi, [0, 1, 0])[:3, :3]
    return center, scale, turn


def convert_entity(name, spec, src):
    mesh = load_single(src / spec["source"])[0]
    mesh = decimate(mesh, spec.get("max_triangles"))
    center, scale, turn = normalize_entity([mesh], spec)
    vertices = ((mesh.vertices - center) * scale) @ turn.T
    normals = mesh.vertex_normals @ turn.T
    out = CONVERTED / name
    out.mkdir(parents=True, exist_ok=True)
    texture_of(mesh, spec["texture"]).save(out / f"{name}.png")
    if "segmentation" in spec:
        parts = split_by_segmentation(mesh, vertices, src / spec["segmentation"])
        for part_name, faces in parts.items():
            write_part(out / f"{name}_{part_name}.obj", vertices, mesh.visual.uv, normals, mesh.faces[faces],
                       f"kn8:entity/{name}")
            print(f"  parte {part_name}: {len(faces)} tri")
    write_obj(out / f"{name}.obj", vertices, mesh.visual.uv, normals, mesh.faces, f"kn8:entity/{name}")
    low, high = vertices.min(axis=0), vertices.max(axis=0)
    print(f"entidade {name}: {len(mesh.faces)} tri, tamanho {np.round(high - low, 2)} m")


def write_part(path, vertices, uvs, normals, faces, material_ref):
    used = np.unique(faces)
    remap = -np.ones(len(vertices), dtype=int)
    remap[used] = np.arange(len(used))
    write_obj(path, vertices[used], uvs[used], normals[used], remap[faces], material_ref)


def split_by_segmentation(mesh, vertices, segmentation_path):
    """Cada face do soldado vai para a parte da segmentacao mais proxima (as duas malhas sao o mesmo modelo)."""
    from scipy.spatial import cKDTree
    parts = load_single(segmentation_path)
    combined = trimesh.util.concatenate(parts)
    low, high = combined.bounds
    target_low, target_high = vertices.min(axis=0), vertices.max(axis=0)
    turn = trimesh.transformations.rotation_matrix(math.pi, [0, 1, 0])[:3, :3]
    points, labels = [], []
    names = label_parts(parts)
    for index, part in enumerate(parts):
        p = (part.vertices - [(low[0] + high[0]) / 2, low[1], (low[2] + high[2]) / 2]) @ turn.T
        p = p / (high[1] - low[1]) * (target_high[1] - target_low[1])
        points.append(p)
        labels += [index] * len(p)
    tree = cKDTree(np.vstack(points))
    centroids = vertices[mesh.faces].mean(axis=1)
    _, nearest = tree.query(centroids)
    face_label = np.array(labels)[nearest]
    return {names[i]: np.where(face_label == i)[0] for i in range(len(parts)) if np.any(face_label == i)}


def label_parts(parts):
    """Nomeia as partes da segmentacao pela posicao (cabeca no topo, botas embaixo, bracos nas laterais)."""
    info = []
    for index, part in enumerate(parts):
        c = part.vertices.mean(axis=0)
        info.append((index, c))
    names = {}
    by_height = sorted(info, key=lambda item: -item[1][1])
    names[by_height[0][0]] = "head"
    names[by_height[-1][0]] = "boots"
    rest = [item for item in info if item[0] not in names]
    xs = sorted(rest, key=lambda item: item[1][0])
    # Depois do giro de 180 graus, x negativo do arquivo vira o lado direito do soldado.
    names[xs[0][0]] = "arm_right"
    names[xs[-1][0]] = "arm_left"
    middle = sorted([item for item in rest if item[0] not in names], key=lambda item: -item[1][1])
    for order, (index, _) in enumerate(middle):
        names[index] = ["torso", "legs", "waist", "extra"][min(order, 3)] if order < 3 else f"extra_{order}"
    return names


def refresh_displays(config, only=None):
    for name, spec in config["weapons"].items():
        path = ASSETS / f"models/item/{name}.json"
        if (only and name != only) or not path.exists():
            continue
        model = json.loads(path.read_text(encoding="utf-8"))
        model["display"].update(held_display(spec))
        path.write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")
        print(f"display de {name}: 3a/1a pessoa x{spec.get('held_scale', [HELD_SCALE_THIRD, HELD_SCALE_FIRST])}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--src", help="pasta com os GLB do Meshy")
    parser.add_argument("--only", help="converter so este nome")
    parser.add_argument("--display-only", action="store_true",
                        help="so refaz o display de mao dos JSON de arma ja convertidos (nao precisa dos GLB)")
    args = parser.parse_args()
    if args.display_only:
        refresh_displays(json.loads(CONFIG.read_text(encoding="utf-8")), args.only)
        return
    if not args.src:
        parser.error("--src e obrigatorio (so --display-only dispensa)")
    src = Path(args.src)
    config = json.loads(CONFIG.read_text(encoding="utf-8"))
    for name, spec in config["weapons"].items():
        if spec.get("replaced_by"):
            # Arte refeita por outro script (ex.: espada -> build_sword.py): nao sobrescrever.
            print(f"pulando {name}: gerado por {spec['replaced_by']}")
        elif not args.only or args.only == name:
            convert_weapon(name, spec, src)
    for name, spec in config["entities"].items():
        if args.only and args.only != name:
            continue
        if not (src / spec["source"]).exists():
            # Modelos chegam aos poucos: o que ainda nao foi feito no Meshy nao derruba a conversao do resto.
            print(f"pulando {name}: {spec['source']} nao esta em {src}")
            continue
        convert_entity(name, spec, src)


if __name__ == "__main__":
    main()
