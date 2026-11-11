#!/usr/bin/env python3
"""Trajes 3D da Forca de Defesa (0.6-C): malha do Meshy presa as partes do modelo do JOGADOR do Minecraft.

Entrada: tools/art/converted/<traje>/<traje>.obj + .png (meshy_convert.py, height_m 1,5 = da gola aos pes, a altura
do corpo + pernas do modelo do jogador). Em metros, pes em Y = 0, frente -Z, lado direito do personagem em +X.
Saida (traje = id do item, ex. mk1):
  - assets/kn8/meshes/suit/<traje>.json + meshes/suit/<traje>/<parte>.obj (body, right_arm, left_arm, right_leg,
    left_leg), cada parte no espaco local da ModelPart do jogador: blocos, Y para baixo, X espelhado (o Minecraft
    desenha o modelo com escala (-1, -1, 1)), origem no pivo da parte. O SuitLayer aplica translateAndRotate da
    parte e desenha, entao o traje segue andar, correr e as animacoes de combate (PAL).
  - assets/kn8/textures/models/suit/<traje>.png (com borda nas ilhas de UV).
Os bracos do Meshy vem abertos (~20 graus, pedido no prompt para separar do tronco): cada braco e endireitado pelo
eixo medido (PCA) em volta do ombro, para ficar como o braco do jogador parado.
Uso: python3 tools/art/rig_suit_mesh.py <traje>   (requer numpy, pillow e scipy)
"""
import json
import shutil
import sys
from pathlib import Path

import numpy as np
from PIL import Image
from scipy.spatial import cKDTree

sys.path.insert(0, str(Path(__file__).parent))
from pad_texture import DILATE_STEPS, dilate, uv_mask  # noqa: E402
from rig_soldier_mesh import absorb_fragments, cap_holes  # noqa: E402
from rig_trichonephila_mesh import read_obj, write_obj  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/kn8"
# Altura da gola no espaco do modelo do jogador (blocos): pescoco em y = 0, pes em y = 1,5.
COLLAR = 1.5
# Pivos das partes do jogador (blocos, espaco do modelo: Y para baixo; direita em X negativo), de HumanoidModel.
PIVOTS = {"body": (0.0, 0.0, 0.0), "right_arm": (-5 / 16, 2 / 16, 0.0), "left_arm": (5 / 16, 2 / 16, 0.0),
          "right_leg": (-1.9 / 16, 12 / 16, 0.0), "left_leg": (1.9 / 16, 12 / 16, 0.0)}
# Caixa minima de cada parte (largura em X, profundidade em Z, blocos) e centro da caixa do jogador no espaco local
# da parte: o Meshy fez o traje mais fino que o boneco do Minecraft (braco de 0,2 contra 0,25 + manga da skin) e a
# skin cobria o traje. Como a armadura vanilla (1 px maior que a pele em cada lado), cada parte e centrada na caixa
# do jogador e alargada ate pelo menos este tamanho (so aumenta; a altura nao muda).
MIN_BOX = {"body": (10 / 16, 6 / 16, 0.0), "right_arm": (6 / 16, 6 / 16, -1 / 16), "left_arm": (6 / 16, 6 / 16, 1 / 16),
           "right_leg": (5 / 16, 5 / 16, 0.0), "left_leg": (5 / 16, 5 / 16, 0.0)}
# Altura de cada fatia do ajuste de largura (blocos = 1 px).
SLICE = 1 / 16
MAX_STRETCH = 1.8
HEAD_STRETCH = 3.2
# 0.5.0-B (traje de corpo inteiro, Biblioteca v21): "segunda pele" escura em volta de cada parte do jogador, um pouco
# maior que a camada externa da skin (0,25 px), para a skin nunca aparecer pelos vaos da malha do Meshy. Caixas do
# HumanoidModel em px (x0, y0, z0, largura, altura, profundidade), no espaco local da ModelPart.
LINER_BOXES = {"body": (-4, 0, -2, 8, 12, 4), "right_arm": (-3, -2, -2, 4, 12, 4), "left_arm": (-1, -2, -2, 4, 12, 4),
               "right_leg": (-2, 0, -2, 4, 12, 4), "left_leg": (-2, 0, -2, 4, 12, 4)}
LINER_INFLATE_PX = 0.3
# AJUSTE-2A: casca do traje shell_px alem da segunda pele, medida pela camada de dentro de cada fatia (percentil) e
# sem mexer no miolo (raio normalizado abaixo de SHELL_CORE).
SHELL_PX = 0.4
SHELL_PERCENTILE = 12
SHELL_CORE = 0.25
# AJUSTE-2A: peca menor que isto (blocos, em qualquer eixo) anda inteira; a escala dela fica neste limite.
RIGID_MAX_SPAN = 8 / 16
RIGID_MAX_SCALE = 1.6
# Cortes medidos na vista de frente de cada traje (metros do OBJ convertido): borda de dentro dos bracos por altura
# (altura acima da qual vale, |x|), ponta dos dedos e quadril.
SUITS = {
    # 0.5.0-B: Mk1 estilo Minecraft do Miguel (bracos em bloco); vao entre braco e tronco medido por altura (bolsas
    # do cinto ate |x| 0,22 ficam no tronco).
    "mk1": {"arm_inner_x": [(1.20, 0.19), (1.05, 0.205), (0.0, 0.232)], "hand_min_y": 0.76,
            "hip_y": 0.82, "liner": True},
    # Luvas grandes descem ate ~0,64, por fora das coxas (|x| > 0,38).
    "mk1_reinforced": {"arm_inner_x": [(1.22, 0.24), (0.90, 0.30), (0.0, 0.38)], "hand_min_y": 0.62,
                       "hip_y": 0.80, "liner": True},
    # EQUIP-1 (modelos do Miguel de 2026-11-05): armaduras numeradas do jogador (bracos e tronco do Meshy finos:
    # com o limite de 1,8 a segunda pele escura cobria as placas, visto em jogo). Escala do meshy_assets.json = gola
    # aos pes em 1,5 m. "drop" tira o pino marrom do manequim do Meshy no pescoco; "head" vai no osso da cabeca
    # (capacete); "leg_core" = so as botas vao nas pernas (o casaco longo fica no tronco: pernas andam por
    # dentro dele); "tail" = acima da gola ou sem par espelhado no lado esquerdo (osso proprio preso ao tronco, balanca).
    "numbers4_suit": {"max_stretch": 1.5, "arm_inner_x": [(1.30, 0.235), (0.0, 0.26)], "hand_min_y": 0.90, "hip_y": 1.02,
                      "head": {"min_y": 1.57, "max_abs_x": 0.20}, "head_box": (9.5 / 16, 9.5 / 16),
                      "leg_core": {"boot_y": 0.26}, "liner": True},
    "numbers6_suit": {"max_stretch": 1.5, "arm_inner_x": [(1.30, 0.205), (0.0, 0.22)], "hand_min_y": 0.86, "hip_y": 0.86,
                      "drop": [{"min_y": 1.50, "max_abs_x": 0.10}], "liner": True},
    "numbers10_suit": {"max_stretch": 1.5, "arm_inner_x": [(1.30, 0.235), (0.0, 0.26)], "hand_min_y": 0.86, "hip_y": 0.86,
                       "drop": [{"min_y": 1.48, "max_abs_x": 0.08, "max_z": 0.10}],
                       "tail": {"above_y": 1.62, "asym": 0.03, "from_y": 0.70, "max_x": -0.15, "tip_x": -0.28},
                       "liner": True},
}


def polyline_distance(points, line):
    """Menor distancia de cada ponto ate a linha quebrada (segmentos entre os pontos de line)."""
    line = np.asarray(line, dtype=float)
    best = np.full(len(points), np.inf)
    for a, b in zip(line[:-1], line[1:]):
        ab = b - a
        t = np.clip(((points - a) @ ab) / max(ab @ ab, 1e-12), 0.0, 1.0)
        best = np.minimum(best, np.linalg.norm(points - (a + t[:, None] * ab), axis=1))
    return best


def drop_faces(vertices, faces, cfg):
    """Tira as faces das caixas do "drop" (pino do manequim no pescoco)."""
    if not cfg.get("drop"):
        return faces
    c = vertices[faces].mean(axis=1)
    keep = np.ones(len(faces), dtype=bool)
    for box in cfg["drop"]:
        sel = (c[:, 1] >= box["min_y"]) & (np.abs(c[:, 0]) <= box["max_abs_x"])
        if "max_z" in box:
            sel &= c[:, 2] <= box["max_z"]
        keep &= ~sel
    print(f"  {np.count_nonzero(~keep)} faces do pino tiradas")
    return faces[keep]


def split(vertices, faces, cfg):
    """Bracos: fora do tronco e acima da ponta dos dedos; corpo: acima do quadril; abaixo, perna do lado. Lado do
    personagem: direita em +X (meshy_convert gira o modelo de frente para -Z). Opcionais (EQUIP-1): capacete,
    cauda e casaco longo no tronco."""
    c = vertices[faces].mean(axis=1)
    inner = np.select([c[:, 1] >= y for y, _ in cfg["arm_inner_x"]], [x for _, x in cfg["arm_inner_x"]])
    tail = np.zeros(len(faces), dtype=bool)
    if "tail" in cfg:
        # Cauda = o que esta acima da gola (a volta por cima da cabeca); medido na vista de lado do traje.
        # A cauda desce pelo lado esquerdo, colada ao braco: la vale o que nao tem par espelhado no lado direito
        # (o traje e simetrico, a cauda nao).
        t = cfg["tail"]
        mirror, _ = cKDTree(c).query(c * [-1, 1, 1])
        tail = (c[:, 1] > t["above_y"]) | ((mirror > t["asym"]) & (c[:, 1] > t["from_y"]) & (c[:, 0] < t["max_x"]))
        # Ponta da cauda: abaixo dos dedos, por fora da perna (espelhada na mao direita, nao sai pela simetria).
        tail |= (c[:, 1] > t["from_y"]) & (c[:, 1] < cfg["hand_min_y"]) & (c[:, 0] < t["tip_x"])
    head = np.zeros(len(faces), dtype=bool)
    if "head" in cfg:
        head = ~tail & (c[:, 1] >= cfg["head"]["min_y"]) & (np.abs(c[:, 0]) <= cfg["head"]["max_abs_x"])
    arm = ~tail & ~head & (c[:, 1] >= cfg["hand_min_y"]) & (np.abs(c[:, 0]) > inner)
    body = ~tail & ~head & ~arm & (c[:, 1] > cfg["hip_y"])
    leg = ~tail & ~head & ~arm & ~body
    if "leg_core" in cfg:
        core = cfg["leg_core"]
        coat = leg & (c[:, 1] > core["boot_y"])
        body |= coat
        leg &= ~coat
    groups = {"body": body, "right_arm": arm & (c[:, 0] > 0), "left_arm": arm & (c[:, 0] <= 0),
              "right_leg": leg & (c[:, 0] > 0), "left_leg": leg & (c[:, 0] <= 0), "head": head, "tail": tail}
    labels = np.empty(len(faces), dtype=object)
    for name, sel in groups.items():
        labels[sel] = name
    return absorb_fragments(vertices, faces, labels)


def to_model(points):
    """Metros do OBJ (Y para cima, pes em 0, direita em +X) -> espaco do modelo do jogador (Y para baixo, gola em 0,
    direita em X negativo). Duas inversoes = rotacao: a ordem dos vertices (frente das faces) nao muda."""
    return np.stack([-points[:, 0], COLLAR - points[:, 1], points[:, 2]], axis=1)


def straighten(points, normals, pivot):
    """Gira o braco no plano da frente (em volta do ombro) ate o eixo dele ficar vertical."""
    centered = points - pivot
    axis = np.linalg.svd(centered[:, :2] - centered[:, :2].mean(axis=0), full_matrices=False)[2][0]
    if axis[1] < 0:
        axis = -axis
    angle = np.arctan2(axis[0], axis[1])
    c, s = np.cos(angle), np.sin(angle)
    rot = np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])
    print(f"  braco endireitado em {np.degrees(angle):.1f} graus")
    return centered @ rot.T + pivot, normals @ rot.T


def fit_box(points, min_width, min_depth, center_x, max_stretch=MAX_STRETCH):
    """Fatia por fatia na altura: centra a fatia na caixa do jogador (X/Z) e alarga ate cobrir a caixa minima (nunca
    encolhe). O traje do Meshy e mais fino que o boneco e cada fatia tem o proprio centro (a bota avanca, a canela
    fica atras): centrar a peca inteira deixava a canela atras da calca da skin e escala unica nao cobria o miolo.
    Centro e largura de cada fatia pelos percentis 5-95 (pecas soltas nao contam); escala e centro suavizados entre
    fatias vizinhas para nao fazer degrau."""
    low, high = points.min(axis=0), points.max(axis=0)
    slices = max(1, int(np.ceil((high[1] - low[1]) / SLICE)))
    index = np.clip(((points[:, 1] - low[1]) / SLICE).astype(int), 0, slices - 1)
    factors = np.ones((slices, 2))
    centers = np.tile([[(low[0] + high[0]) / 2, (low[2] + high[2]) / 2]], (slices, 1))
    for k in range(slices):
        member = points[index == k]
        if len(member) < 3:
            continue
        x5, x95 = np.percentile(member[:, 0], [5, 95])
        z5, z95 = np.percentile(member[:, 2], [5, 95])
        centers[k] = [(x5 + x95) / 2, (z5 + z95) / 2]
        factors[k] = [max(1.0, min_width / max(x95 - x5, 1e-6)), max(1.0, min_depth / max(z95 - z5, 1e-6))]
    # Fatias quase vazias (ponta do ombro, do pe) pediam 3-8x: limite.
    factors = np.minimum(factors, max_stretch)

    def smooth(values):
        kernel = np.ones(3) / 3
        return np.stack([np.convolve(np.pad(values[:, i], 1, mode="edge"), kernel, "valid") for i in (0, 1)],
                        axis=1)

    factors, centers = smooth(factors), smooth(centers)
    # AJUSTE-2A: escala interpolada pela altura de cada vertice (antes era a da fatia inteira: um triangulo com
    # cantos em fatias vizinhas era esticado de um jeito em cada canto e virava uma lasca).
    mids = low[1] + (np.arange(slices) + 0.5) * SLICE
    y = points[:, 1]
    fx, fz = (np.interp(y, mids, factors[:, i]) for i in (0, 1))
    cx, cz = (np.interp(y, mids, centers[:, i]) for i in (0, 1))
    out = points.copy()
    out[:, 0] = (points[:, 0] - cx) * fx + center_x
    out[:, 2] = (points[:, 2] - cz) * fz
    print(f"  escala x ate {factors[:, 0].max():.2f}, z ate {factors[:, 1].max():.2f}")
    return out


def main():
    name = sys.argv[1]
    cfg = SUITS[name]
    source = ROOT / "tools/art/converted" / name
    vertices, uvs, normals, faces = read_obj(source / f"{name}.obj")
    faces = drop_faces(vertices, faces, cfg)
    labels = split(vertices, faces, cfg)
    if not cfg.get("liner"):
        # AJUSTE-2A: so sem a segunda pele. Com ela o oco ja nao aparece, e nos modelos de placas soltas o contorno
        # aberto de cada placa (gola, barra do casaco) virava um leque de uma cor so: os triangulos escuros enormes do
        # peito do Numbers 10.
        vertices, uvs, normals, faces, labels = cap_holes(vertices, uvs, normals, faces, labels)
    model = to_model(vertices)
    model_normals = np.stack([-normals[:, 0], -normals[:, 1], normals[:, 2]], axis=1)
    out_dir = ASSETS / "meshes/suit" / name
    if out_dir.exists():
        shutil.rmtree(out_dir)
    index = {"bones": {}}
    image_rgb = np.asarray(Image.open(source / f"{name}.png").convert("RGB")).astype(float)
    parts = dict(PIVOTS)
    if "head" in cfg:
        parts["head"] = (0.0, 0.0, 0.0)
    if "tail" in cfg:
        parts["tail"] = tail_root(model, faces[labels == "tail"])
        index["pivots"] = {"tail": [round(float(v), 5) for v in parts["tail"]]}
    for part, pivot in parts.items():
        part_faces = faces[labels == part]
        if not len(part_faces):
            print(f"AVISO: {part} vazio")
            continue
        used = np.unique(part_faces)
        points, part_normals = model.copy(), model_normals.copy()
        if part == "tail":
            # Cauda: sem ajuste de largura, no espaco do tronco com origem na raiz (o SuitLayer balanca em volta).
            write_obj(out_dir / f"{part}.obj", points - np.array(pivot), uvs, part_normals, part_faces)
            index["bones"][part] = f"kn8:meshes/suit/{name}/{part}.obj"
            print(f"{part}: {len(part_faces)} triangulos, raiz {np.round(pivot, 3)}")
            continue
        if part.endswith("_arm"):
            points[used], part_normals[used] = straighten(model[used], model_normals[used], np.array(pivot))
        local = points - np.array(pivot)
        box = (*cfg["head_box"], 0.0) if part == "head" else MIN_BOX[part]
        before = local[used].copy()
        # AJUSTE-2A: a gola/mascara precisa passar da cabeca do jogador (8 px + a camada do chapeu): escala propria.
        stretch = cfg.get("head_stretch", HEAD_STRETCH) if part == "head" else cfg.get("max_stretch", MAX_STRETCH)
        local[used] = fit_box(local[used], *box, stretch)
        if cfg.get("liner") and part in LINER_BOXES:
            local[used] = push_out(local[used], part, cfg.get("shell_px", SHELL_PX))
        local[used] = rigid_pieces(before, local[used], used, part_faces)
        reach = cfg.get("arm_reach")
        if reach and part.endswith("_arm"):
            # 0.5.0-B: a manga do Mk1 novo terminava antes da mao do jogador (a mao da skin aparecia): estica o braco
            # em Y a partir do ombro ate cobrir a mao (o braco do jogador vai de -2 a 10 px do pivo).
            bottom = local[used][:, 1].max()
            if bottom < reach:
                local[used, 1] *= reach / bottom
                print(f"  braco esticado {reach / bottom:.2f}x ate a mao")
        if cfg.get("liner") and part in LINER_BOXES:
            local, liner_uvs, part_normals, part_faces = add_liner(part, local, uvs, part_normals, part_faces,
                                                                  dark_uv(image_rgb, uvs, part_faces))
        else:
            liner_uvs = uvs
        write_obj(out_dir / f"{part}.obj", local, liner_uvs, part_normals, part_faces)
        index["bones"][part] = f"kn8:meshes/suit/{name}/{part}.obj"
        print(f"{part}: {len(part_faces)} triangulos")
    (ASSETS / "meshes/suit" / f"{name}.json").write_text(json.dumps(index, indent=2) + "\n", encoding="utf-8")
    image = Image.open(source / f"{name}.png").convert("RGBA")
    mask = uv_mask([source / f"{name}.obj"], image.width, image.height)
    padded = dilate(np.asarray(image)[..., :3], mask, DILATE_STEPS)
    texture = ASSETS / "textures/models/suit" / f"{name}.png"
    texture.parent.mkdir(parents=True, exist_ok=True)
    Image.fromarray(np.clip(padded, 0, 255).astype(np.uint8)).convert("RGBA").save(texture)


def rigid_pieces(before, after, used, part_faces):
    """AJUSTE-2A: peca solta pequena (placa, bloco da perna do Numbers 10) anda inteira: o centro vai para onde o ajuste
    mandou e a forma so muda de escala em X/Z, igual em toda a peca. O ajuste por altura (fit_box/push_out) entortava
    cada bloco (cada altura com outra escala e outro centro) e os blocos viravam lascas. Malha continua grande (Mk1)
    continua com o ajuste por altura."""
    from scipy.sparse import coo_matrix
    from scipy.sparse.csgraph import connected_components
    local_index = {v: i for i, v in enumerate(used)}
    faces = np.vectorize(local_index.get)(part_faces)
    _, welded = np.unique(np.round(before, 5), axis=0, return_inverse=True)
    welded = welded.reshape(-1)
    wf = welded[faces]
    size = welded.max() + 1
    rows = np.concatenate([wf[:, 0], wf[:, 1], wf[:, 2]])
    cols = np.concatenate([wf[:, 1], wf[:, 2], wf[:, 0]])
    _, piece = connected_components(coo_matrix((np.ones(len(rows)), (rows, cols)), shape=(size, size)),
                                    directed=False)
    piece = piece[welded]
    out = after.copy()
    moved = 0
    for label in np.unique(piece):
        member = piece == label
        span = before[member].max(axis=0) - before[member].min(axis=0)
        if span.max() > RIGID_MAX_SPAN or member.sum() < 3:
            continue
        c0, c1 = before[member].mean(axis=0), after[member].mean(axis=0)
        scale = np.ones(3)
        for axis in (0, 2):
            spread = before[member, axis].std()
            if spread > 1e-6:
                scale[axis] = np.clip(after[member, axis].std() / spread, 1.0 / RIGID_MAX_SCALE, RIGID_MAX_SCALE)
        out[member] = c1 + (before[member] - c0) * scale
        moved += 1
    print(f"  {moved} pecas rigidas")
    return out


def push_out(points, part, shell_px):
    """AJUSTE-2A: casca do traje por fora da segunda pele. O encaixe acima so garante a largura do traje; onde a
    superficie fica por dentro da caixa da segunda pele ela some e so as pontas das placas atravessam (o traje parecia
    estilhacado: 30-69% da superficie escondida nos Numbers e no Mk1 Reforcado). Cada vertice anda para fora a partir
    do eixo da parte, na medida que falta para a camada de dentro (percentil SHELL_PERCENTILE da fatia) passar a
    segunda pele em shell_px; a ordem das camadas fica (as placas continuam por cima), as placas nao incham, so andam."""
    x0, y0, z0, w, h, d = LINER_BOXES[part]
    margin = LINER_INFLATE_PX + shell_px
    cx, cz = (x0 + w / 2) / 16, (z0 + d / 2) / 16
    hw, hd = (w / 2 + margin) / 16, (d / 2 + margin) / 16
    dx, dz = points[:, 0] - cx, points[:, 2] - cz
    radius = np.maximum(np.abs(dx) / hw, np.abs(dz) / hd)
    low, high = points[:, 1].min(), points[:, 1].max()
    slices = max(1, int(np.ceil((high - low) / SLICE)))
    index = np.clip(((points[:, 1] - low) / SLICE).astype(int), 0, slices - 1)
    gaps = np.zeros(slices)
    for k in range(slices):
        member = radius[(index == k) & (radius > SHELL_CORE)]
        if len(member) >= 3:
            gaps[k] = max(0.0, 1.0 - np.percentile(member, SHELL_PERCENTILE))
    gaps = np.convolve(np.pad(gaps, 2, mode="edge"), np.ones(5) / 5, "valid")
    mids = low + (np.arange(slices) + 0.5) * SLICE
    gap = np.interp(points[:, 1], mids, gaps)
    # Pontos perto do eixo sao o miolo (por dentro de tudo): ficam onde estao, com passagem suave ate a casca (um corte
    # seco no SHELL_CORE esticava os triangulos entre o miolo e a casca em pontas).
    weight = np.clip((radius - SHELL_CORE) / SHELL_CORE, 0.0, 1.0)
    scale = (radius + gap * weight) / np.maximum(radius, 1e-6)
    out = points.copy()
    out[:, 0] = cx + dx * scale
    out[:, 2] = cz + dz * scale
    print(f"  casca empurrada ate {gap.max() * hw * 16:.2f} px")
    return out


def tail_root(model, tail_faces):
    """Raiz da cauda (espaco do modelo): o ponto da cauda mais perto do meio das costas, na altura da cintura."""
    points = model[np.unique(tail_faces)]
    waist = np.array([0.0, COLLAR - 0.95, 0.10])
    return points[np.argmin(np.linalg.norm(points - waist, axis=1))]


def dark_uv(image, uvs, faces):
    """UV do ponto mais escuro (preto do macacao) entre os centros das faces da parte, para pintar a segunda pele."""
    height, width = image.shape[:2]
    centers = uvs[faces].mean(axis=1)
    px = np.clip((centers[:, 0] * width).astype(int), 0, width - 1)
    py = np.clip(((1.0 - centers[:, 1]) * height).astype(int), 0, height - 1)
    brightness = image[py, px].sum(axis=1)
    darkest = np.argsort(brightness)[:max(1, len(brightness) // 20)]
    return centers[darkest[len(darkest) // 2]]


def add_liner(part, local, uvs, normals, faces, uv, box=None):
    """Soma a caixa da segunda pele (dos dois lados, 24 triangulos) a malha da parte (box: outra caixa, em px)."""
    x0, y0, z0, w, h, d = box or LINER_BOXES[part]
    pad = LINER_INFLATE_PX
    lo = np.array([x0 - pad, y0 - pad, z0 - pad]) / 16
    hi = np.array([x0 + w + pad, y0 + h + pad, z0 + d + pad]) / 16
    corners = np.array([[lo[0] if i & 1 == 0 else hi[0], lo[1] if i & 2 == 0 else hi[1],
                         lo[2] if i & 4 == 0 else hi[2]] for i in range(8)])
    quads = [((0, 2, 6, 4), (-1, 0, 0)), ((1, 5, 7, 3), (1, 0, 0)), ((0, 4, 5, 1), (0, -1, 0)),
             ((2, 3, 7, 6), (0, 1, 0)), ((0, 1, 3, 2), (0, 0, -1)), ((4, 6, 7, 5), (0, 0, 1))]
    base = len(local)
    new_vertices, new_normals, new_faces = [], [], []
    for quad, normal in quads:
        start = base + len(new_vertices)
        new_vertices.extend(corners[list(quad)])
        new_normals.extend([normal] * 4)
        a, b, c, e = range(start, start + 4)
        # Os dois sentidos: a caixa aparece de qualquer lado, com ou sem descarte de face de tras.
        new_faces.extend([(a, b, c), (a, c, e), (a, c, b), (a, e, c)])
    local = np.vstack([local, new_vertices])
    normals = np.vstack([normals, np.array(new_normals, dtype=float)])
    uvs = np.vstack([uvs, np.tile(uv, (len(new_vertices), 1))])
    faces = np.vstack([faces, np.array(new_faces)])
    return local, uvs, normals, faces


if __name__ == "__main__":
    main()
