#!/usr/bin/env python3
"""RIG-1 (Miguel, 2026-10-29: "nao quero modelos travados que parecem que nem estao atacando direito"): ossos
articulados a partir do rig que o Miguel fez no Meshy (esqueleto humanoide de 24 juntas, pesos por vertice).

A GeckoLib move cada pedaco da malha como peca rigida (sem pele deformando): o rig do Meshy serve para (1) as juntas
no lugar certo (ombro, cotovelo, punho, quadril, joelho, tornozelo, cintura, pescoco) e (2) cortar a malha pelo osso
de maior peso de cada vertice, em vez de cortar pela forma. As animacoes que vieram no GLB NAO sao usadas (Miguel).

Ossos (os nomes antigos continuam; os novos ficam dentro deles, entao as animacoes atuais seguem valendo):
  root
    body (quadril) > chest (cintura) > head (pescoco)
                                     > arm_<lado> (ombro) > forearm_<lado> (cotovelo) > hand_<lado> (punho) > item_<lado>
    leg_<lado> (quadril) > shin_<lado> (joelho) > foot_<lado> (tornozelo)
"left" = lado de X negativo na malha convertida (mesma convencao do rig_soldier_mesh/rig_kaiju_no9_mesh).

Dois passos:
  python3 tools/art/rig_from_skin.py importar <especie> <arquivo.glb>
      le os pesos do GLB e grava tools/art/skins/<especie>.npz (pequeno, vai para o Git: o GLB fica fora)
  python3 tools/art/rig_from_skin.py <especie>
      corta a malha convertida (tools/art/converted/<especie>/<especie>.obj) e grava meshes/<especie>.json,
      meshes/<especie>/<osso>.obj e geo/entity/<especie>.geo.json. Textura e animacoes nao mudam.
"""
import json
import shutil
import struct
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).parent))
from rig_primigenius_mesh import geo_pivot, weld  # noqa: E402
from rig_soldier_mesh import cap_holes  # noqa: E402
from rig_trichonephila_mesh import read_obj, write_obj  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/kn8"
SKINS = ROOT / "tools/art/skins"
CONVERTED = ROOT / "tools/art/converted"

# Junta do Mixamo -> parte do corpo (o lado sai da posicao da junta, nao do nome).
JOINT_PART = {"Hips": "body", "Spine": "body", "Spine1": "chest", "Spine2": "chest", "Neck": "head",
              "Head": "head", "HeadTop_End": "head", "headfront": "head",
              # Clavicula fica no peito: no Meshy ela leva o alto do peito junto e o peito rasgaria ao erguer o braco.
              "Shoulder": "chest", "Arm": "arm", "ForeArm": "forearm", "Hand": "hand",
              "UpLeg": "leg", "Leg": "shin", "Foot": "foot", "ToeBase": "foot"}
SIDED = {"arm", "forearm", "hand", "leg", "shin", "foot"}
# Pivo de cada osso = junta do Mixamo.
PIVOT_JOINT = {"body": "Hips", "chest": "Spine1", "head": "Neck", "arm": "Arm", "forearm": "ForeArm",
               "hand": "Hand", "leg": "UpLeg", "shin": "Leg", "foot": "Foot"}
# Ajustes por especie: ossos que a especie ja tinha alem destes (mantidos no .geo.json) e correcoes de corte.
TAIL = ["tail_1", "tail_2", "tail_3", "tail_4"]
SPECIES = {
    # Isao: as manoplas do Numbers 2 vao com a mao; item invisivel (kn8:numbers2_gauntlet) no punho.
    "isao": {"items": True},
    # Formas de kaiju: sem item. O No. 9 com o Isao tem espinhos nos ombros que o Meshy dividiu entre ombro e braco.
    "kaiju_no9_isao": {"items": False},
    "kaiju_no13": {"items": False},
    # RIG-2 (Miguel, 2026-10-29): os outros humanoides. "keep" = ossos que o rig humanoide nao tem (cauda, mandibula,
    # barbatanas, asas): continuam com as faces e o pivo de antes; "reparent" = pai novo deles (asas no peito).
    # "new_mesh" = o Miguel mandou um modelo novo junto com o rig.
    "soldier": {"items": True},
    "hoshina": {"items": True, "new_mesh": True},
    "hoshina_no10": {"items": True, "keep": TAIL},
    "kikoru": {"items": True},
    "kikoru_no4": {"items": True, "keep": ["wing_left", "wing_right"],
                   "reparent": {"wing_left": "chest", "wing_right": "chest"}},
    "mina": {"items": True},
    "reno": {"items": True},
    "reno_no6": {"items": True},
    "narumi": {"items": True},
    "kafka": {"items": True},
    "kaiju_no8": {"items": True, "new_mesh": True},
    "kaiju_no9": {"items": False},
    "kaiju_no9_black": {"items": False},
    "kaiju_no9_fusion": {"items": False, "keep": TAIL + ["jaw", "fin_left", "fin_right"], "legs_on_body": True},
    "kaiju_no10_small": {"items": False, "keep": TAIL + ["jaw", "fin_left", "fin_right"], "legs_on_body": True},
    "kaiju_no10_giant": {"items": False, "keep": TAIL + ["jaw", "fin_left", "fin_right"], "legs_on_body": True},
}
# FIX-2: "legs_on_body" = pernas filhas do tronco, como no rig de antes (No. 10 e fusao): as animacoes deles inclinam o
# tronco contando que as pernas vao junto; com as pernas na raiz a bacia abria ("o modelo parte no meio").
# FIX-2 (Miguel, 2026-10-29: "modelos que se partem no meio", "armas fora da mao"): especies que mantiveram a malha e
# as animacoes de antes do RIG-2 voltam ao pivo antigo nos ossos de cima (tronco, cabeca, ombros, quadril), gravado de
# antes do RIG-2 em skins/legacy_pivots.json. As animacoes aprovadas (Blockbench, tecnicas, pegadas das armas) giram
# de novo no mesmo ponto; as juntas novas (peito, cotovelo, punho, joelho, tornozelo) ficam no rig do Meshy. Fora:
# Hoshina e Kaiju No. 8 (modelo novo) e as especies com animacoes refeitas no humanoid_anims.py (No. 9, forma preta,
# Isao, No. 13, No. 9 com o Isao), feitas sobre os pivos do rig.
LEGACY_PIVOTS = SKINS / "legacy_pivots.json"
# Rodadas do filtro de maioria (cada face vai para o osso da maioria das vizinhas quando ela e minoria).
SMOOTH_ROUNDS = 3
# Pedaco solto de um osso com menos faces que isto vai para o vizinho.
FRAGMENT_FACES = 40


# ------------------------------------------------------------------------------------------------ importar (GLB)
def _glb(path):
    raw = Path(path).read_bytes()
    size = struct.unpack("<I", raw[12:16])[0]
    doc = json.loads(raw[20:20 + size])
    start = 20 + size
    blob = raw[start + 8:start + 8 + struct.unpack("<I", raw[start:start + 4])[0]]
    kinds = {5121: np.uint8, 5123: np.uint16, 5125: np.uint32, 5126: np.float32}
    widths = {"SCALAR": 1, "VEC2": 2, "VEC3": 3, "VEC4": 4, "MAT4": 16}

    def accessor(index):
        a = doc["accessors"][index]
        view = doc["bufferViews"][a["bufferView"]]
        offset = view.get("byteOffset", 0) + a.get("byteOffset", 0)
        width = widths[a["type"]]
        return np.frombuffer(blob, kinds[a["componentType"]], a["count"] * width, offset).reshape(a["count"], width)
    return doc, accessor


def current_mesh(species):
    """Malha que o jogo usa hoje (todas as pecas de meshes/<especie>, ja com as correcoes de cada rig: cabeca do
    No. 10 no eixo, cauda, asas em atlas), sem as tampas dos cortes antigos, e o osso de cada face."""
    index = json.loads((ASSETS / "meshes" / f"{species}.json").read_text(encoding="utf-8"))["bones"]
    parts, labels, base = [], [], 0
    for bone, path in index.items():
        v, t, n, f = read_obj(ASSETS / path.split(":")[1])
        parts.append((v, t, n, f + base))
        labels += [bone] * len(f)
        base += len(v)
    vertices, uvs, normals, faces = (np.vstack([part[i] for part in parts]) for i in range(4))
    # Tampa do cap_holes = leque desenhado nos dois sentidos: faces repetidas com os mesmos vertices saem.
    welded = weld(vertices)
    keys = [tuple(sorted(face)) for face in welded[faces]]
    count = {}
    for key in keys:
        count[key] = count.get(key, 0) + 1
    keep = np.array([count[key] == 1 for key in keys])
    return vertices, uvs, normals, faces[keep], np.array(labels, dtype=object)[keep]


def similarity(source, target):
    """Escala, giro e translacao (Umeyama) que levam os pontos source aos target (pares ja casados)."""
    mu_s, mu_t = source.mean(0), target.mean(0)
    a, b = source - mu_s, target - mu_t
    u, d, vt = np.linalg.svd(b.T @ a / len(a))
    fix = np.eye(3)
    fix[2, 2] = np.sign(np.linalg.det(u @ vt))
    rotation = u @ fix @ vt
    scale = (d * np.diag(fix)).sum() / (a ** 2).sum(1).mean()
    return scale, rotation, mu_t - scale * rotation @ mu_s


def fit(positions, target, rounds=25):
    """Encaixa o GLB (ja virado para -Z e na altura) na malha do jogo: cada vertice do jogo puxa o vertice do GLB
    mais perto; os 10% de pares mais longe (cauda corrigida, cabeca recentralizada) nao contam."""
    from scipy.spatial import cKDTree
    scale, rotation, shift = 1.0, np.eye(3), np.zeros(3)
    current = positions.copy()
    for _ in range(rounds):
        distance, nearest = cKDTree(current).query(target)
        keep = distance <= np.percentile(distance, 90)
        s, r, t = similarity(current[nearest[keep]], target[keep])
        current = s * current @ r.T + t
        scale, rotation, shift = s * scale, r @ rotation, s * r @ shift + t
    return lambda points: scale * points @ rotation.T + shift


def glb_mesh(doc, accessor):
    primitive = doc["meshes"][0]["primitives"][0]
    attributes = primitive["attributes"]
    uvs = accessor(attributes["TEXCOORD_0"]).astype(float) * np.array([1.0, -1.0]) + np.array([0.0, 1.0])
    faces = accessor(primitive["indices"]).astype(int).reshape(-1, 3)
    return (accessor(attributes["POSITION"]).astype(float), uvs, accessor(attributes["NORMAL"]).astype(float),
            faces)


def glb_texture(doc, glb):
    import io
    from PIL import Image
    raw = Path(glb).read_bytes()
    size = struct.unpack("<I", raw[12:16])[0]
    start = 20 + size
    blob = raw[start + 8:start + 8 + struct.unpack("<I", raw[start:start + 4])[0]]
    material = doc["materials"][doc["meshes"][0]["primitives"][0].get("material", 0)]
    image = doc["images"][doc["textures"][material["pbrMetallicRoughness"]["baseColorTexture"]["index"]]["source"]]
    view = doc["bufferViews"][image["bufferView"]]
    offset = view.get("byteOffset", 0)
    return Image.open(io.BytesIO(blob[offset:offset + view["byteLength"]])).convert("RGBA")


def import_skin(species, glb, new_mesh=False):
    """Pesos e juntas do GLB no espaco da malha do jogo (metros, frente -Z, pes em Y 0).

    Padrao: a malha continua a que o jogo ja usa (pecas atuais, com as correcoes); o GLB e encaixado nela.
    new_mesh: o Miguel mandou um modelo novo junto com o rig (No. 8 "mob quad", Hoshina): a malha e a textura passam a
    ser as do GLB, na altura do meshy_assets.json."""
    from scipy.spatial import cKDTree
    doc, accessor = _glb(glb)
    primitive = doc["meshes"][0]["primitives"][0]
    positions = accessor(primitive["attributes"]["POSITION"]).astype(float)
    joints = accessor(primitive["attributes"]["JOINTS_0"]).astype(int)
    weights = accessor(primitive["attributes"]["WEIGHTS_0"]).astype(float)
    skin = doc["skins"][0]
    names = [doc["nodes"][i]["name"].split(":")[-1] for i in skin["joints"]]
    inverse = accessor(skin["inverseBindMatrices"]).astype(float).reshape(-1, 4, 4).transpose(0, 2, 1)
    bind = np.array([np.linalg.inv(m)[:3, 3] for m in inverse])
    # O Meshy olha para +Z; o jogo para -Z: giro de 180 graus.
    flip = np.array([-1.0, 1.0, -1.0])
    if new_mesh:
        assets = json.loads((ROOT / "tools/art/meshy_assets.json").read_text(encoding="utf-8"))["entities"]
        height = assets[species]["height_m"]
        scale = height / np.ptp(positions[:, 1])
        low, high = positions.min(0) * scale * flip, positions.max(0) * scale * flip
        center = np.array([(low[0] + high[0]) / 2, positions[:, 1].min() * scale, (low[2] + high[2]) / 2])

        def place(points):
            return points * scale * flip - center
        _, uvs, normals, faces = glb_mesh(doc, accessor)
        vertices, normals = place(positions), normals * flip
        labels = np.array(["body"] * len(faces), dtype=object)
        write_new_mesh(species, glb, doc, vertices, uvs, normals, faces)
    else:
        vertices, uvs, normals, faces, labels = current_mesh(species)
        scale = np.ptp(vertices[:, 1]) / np.ptp(positions[:, 1])
        rough = positions * scale * flip
        rough += np.array([vertices[:, 0].mean() - rough[:, 0].mean(), vertices[:, 1].min() - rough[:, 1].min(),
                           vertices[:, 2].mean() - rough[:, 2].mean()])
        snap = fit(rough, vertices)

        def place(points):
            out = points * scale * flip
            out += np.array([vertices[:, 0].mean(), vertices[:, 1].min(), vertices[:, 2].mean()]) - np.array(
                [(positions * scale * flip)[:, 0].mean(), (positions * scale * flip)[:, 1].min(),
                 (positions * scale * flip)[:, 2].mean()])
            return snap(out)
    distance, nearest = cKDTree(place(positions)).query(vertices)
    print(f"{species}: {len(vertices)} vertices, {len(faces)} faces, distancia ao GLB media "
          f"{distance.mean() * 100:.1f} cm, maxima {distance.max() * 100:.1f} cm")
    SKINS.mkdir(parents=True, exist_ok=True)
    np.savez_compressed(SKINS / f"{species}.npz", joints=joints[nearest].astype(np.uint8),
                        weights=np.round(weights[nearest], 3).astype(np.float16), names=np.array(names),
                        bind=place(bind), vertices=vertices.astype(np.float32), uvs=uvs.astype(np.float32),
                        normals=normals.astype(np.float32), faces=faces.astype(np.int32),
                        old_labels=labels.astype(str))


def write_new_mesh(species, glb, doc, vertices, uvs, normals, faces):
    """Modelo novo: grava a fonte convertida (tools/art/converted) e a textura do jogo (com borda nas ilhas)."""
    from PIL import Image
    from pad_texture import DILATE_STEPS, dilate, uv_mask
    folder = CONVERTED / species
    folder.mkdir(parents=True, exist_ok=True)
    write_obj(folder / f"{species}.obj", vertices, uvs, normals, faces)
    image = glb_texture(doc, glb)
    image.save(folder / f"{species}.png")
    target = ASSETS / f"textures/entity/{species}.png"
    size = Image.open(target).size[0] if target.exists() else 1024
    mask = uv_mask([folder / f"{species}.obj"], image.width, image.height)
    padded = dilate(np.asarray(image)[..., :3].astype(float), mask, DILATE_STEPS)
    Image.fromarray(np.clip(padded, 0, 255).astype(np.uint8)).convert("RGBA") \
        .resize((size, size), Image.Resampling.BOX).save(target)


# ------------------------------------------------------------------------------------------------ cortar
def bone_of(name, position):
    part = JOINT_PART.get(name.replace("Left", "").replace("Right", ""))
    if part is None:
        return "body"
    if part in SIDED:
        # Lado pelo nome da junta (Left = esquerda do personagem = X negativo no jogo, conferido no Isao): o rig do
        # No. 10 gigante veio com juntas da direita do lado esquerdo do corpo.
        return f"{part}_{'left' if name.startswith('Left') else 'right'}"
    return part


def face_neighbors(faces, welded):
    wf = welded[faces]
    edges = {}
    for index, (a, b, c) in enumerate(wf):
        for edge in ((a, b), (b, c), (c, a)):
            edges.setdefault((min(edge), max(edge)), []).append(index)
    neighbors = [[] for _ in range(len(faces))]
    for shared in edges.values():
        for i in shared:
            neighbors[i].extend(j for j in shared if j != i)
    return neighbors


def label_faces(vertices, faces, skin):
    names, bind = list(skin["names"]), skin["bind"]
    joints, weights = skin["joints"].astype(int), skin["weights"].astype(float)
    bones = np.array([bone_of(n, p) for n, p in zip(names, bind)], dtype=object)
    # Peso somado por osso do jogo em cada face (juntas da mesma parte somam: Spine1 + Spine2 = peito).
    labels = []
    for face in faces:
        score = {}
        for v in face:
            for j, w in zip(joints[v], weights[v]):
                score[bones[j]] = score.get(bones[j], 0.0) + w
        labels.append(max(score, key=score.get))
    labels = np.array(labels, dtype=object)
    welded = weld(vertices)
    neighbors = face_neighbors(faces, welded)
    for _ in range(SMOOTH_ROUNDS):
        changed = labels.copy()
        for i, near in enumerate(neighbors):
            if not near:
                continue
            values, counts = np.unique(labels[near], return_counts=True)
            best = values[np.argmax(counts)]
            if best != labels[i] and counts.max() > len(near) / 2:
                changed[i] = best
        labels = changed
    return absorb(labels, neighbors)


def absorb(labels, neighbors):
    """Pedaco solto e pequeno de um osso (ponta de espinho com peso do braco no meio do peito) vai para o vizinho."""
    seen = np.zeros(len(labels), dtype=bool)
    for start in range(len(labels)):
        if seen[start]:
            continue
        group, stack = [], [start]
        seen[start] = True
        while stack:
            i = stack.pop()
            group.append(i)
            for j in neighbors[i]:
                if not seen[j] and labels[j] == labels[start]:
                    seen[j] = True
                    stack.append(j)
        if len(group) >= FRAGMENT_FACES:
            continue
        border = [labels[j] for i in group for j in neighbors[i] if labels[j] != labels[start]]
        if border:
            values, counts = np.unique(np.array(border, dtype=object), return_counts=True)
            labels[group] = values[np.argmax(counts)]
    return labels


def hierarchy(items):
    parents = {"root": None, "body": "root", "chest": "body", "head": "chest"}
    for side in ("left", "right"):
        parents.update({f"arm_{side}": "chest", f"forearm_{side}": f"arm_{side}", f"hand_{side}": f"forearm_{side}",
                        f"leg_{side}": "root", f"shin_{side}": f"leg_{side}", f"foot_{side}": f"shin_{side}"})
        if items:
            parents[f"item_{side}"] = f"hand_{side}"
    return parents


def seam(vertices, faces, labels, bone, parent):
    """Centro do corte entre um osso e o pai (vertices soldados que as duas pecas dividem)."""
    welded = weld(vertices)
    mine = set(welded[faces[labels == bone]].ravel())
    shared = mine & set(welded[faces[labels == parent]].ravel())
    if len(shared) < 6:
        return None
    points = np.zeros((welded.max() + 1, 3))
    points[welded] = vertices
    return points[sorted(shared)].mean(axis=0)


def pivots(skin, meshes, cut=None):
    """Pivo de cada osso = junta do rig. Junta fora do corpo (rig do No. 10 gigante veio com o quadril 45 m atras do
    modelo) ou ausente: centro do corte entre o osso e o pai."""
    names, bind = list(skin["names"]), skin["bind"]
    out = {"root": np.zeros(3)}
    everything = np.vstack(list(meshes.values()))
    height = np.ptp(everything[:, 1])
    for bone, joint in PIVOT_JOINT.items():
        for side in ("left", "right") if bone in SIDED else (None,):
            key = f"{bone}_{side}" if side else bone
            candidates = [bind[i] for i, n in enumerate(names)
                          if n.replace("Left", "").replace("Right", "") == joint
                          and (side is None or n.startswith("Left") == (side == "left"))]
            point = candidates[0] if candidates else None
            near = meshes.get(key, everything)
            if point is None or np.min(np.linalg.norm(near - point, axis=1)) > 0.08 * height:
                fallback = cut(key) if cut else None
                if fallback is None and key in meshes and bone in SIDED:
                    # Membro em repouso: a junta fica no alto da propria peca.
                    own = meshes[key]
                    top = own[own[:, 1] > own[:, 1].max() - 0.06 * np.ptp(own[:, 1]) - 1e-6]
                    fallback = top.mean(axis=0)
                if fallback is not None:
                    print(f"  pivo de {key} pelo corte (junta do rig fora do corpo)")
                    point = fallback
                elif point is not None:
                    # So a profundidade quebrada: altura e lado da junta valem, o Z vem do corpo naquela altura.
                    band = everything[np.abs(everything[:, 1] - point[1]) < 0.03 * height]
                    band = band[np.abs(band[:, 0] - point[0]) < 0.08 * height] if len(band) else band
                    if len(band):
                        point = np.array([point[0], point[1], band[:, 2].mean()])
                        print(f"  pivo de {key}: profundidade pelo corpo (junta do rig fora do corpo)")
                    else:
                        point = near.mean(axis=0)
                        print(f"  pivo de {key} no centro da peca")
                else:
                    point = near.mean(axis=0) if key in meshes else np.zeros(3)
                    print(f"  pivo de {key} no centro da peca")
            out[key] = point
    for side in ("left", "right"):
        # Pegada: entre o punho e o centro da mao (onde ficaria o cabo de uma arma).
        hand = meshes.get(f"hand_{side}")
        wrist = out[f"hand_{side}"]
        out[f"item_{side}"] = wrist + 0.5 * (hand.mean(axis=0) - wrist) if hand is not None else wrist
    return out


def build(species):
    cfg = SPECIES[species]
    skin = dict(np.load(SKINS / f"{species}.npz"))
    if "faces" in skin:
        vertices, uvs, normals = (skin[k].astype(float) for k in ("vertices", "uvs", "normals"))
        faces, old = skin["faces"].astype(int), skin["old_labels"].astype(object)
    else:
        # RIG-1: Isao, No. 9 com o Isao e No. 13 foram importados da malha convertida.
        vertices, uvs, normals, faces = read_obj(CONVERTED / species / f"{species}.obj")
        old = np.array([""] * len(faces), dtype=object)
    labels = label_faces(vertices, faces, skin)
    if species in json.loads(LEGACY_PIVOTS.read_text(encoding="utf-8")) if LEGACY_PIVOTS.exists() else False:
        # FIX-2: com o pivo antigo do quadril, o corte do quadril tambem volta ao antigo (o Meshy punha parte da bacia
        # na coxa e a bacia abria ao levantar a perna). Joelho e tornozelo continuam pelo rig.
        for side in ("left", "right"):
            leg = f"leg_{side}"
            labels[(old == "body") & (labels == leg)] = "body"
            labels[(old == leg) & (labels == "body")] = leg
    keep = cfg.get("keep", [])
    kept = np.isin(old, keep)
    labels[kept] = old[kept]
    vertices, uvs, normals, faces, labels = cap_holes(vertices, uvs, normals, faces, labels)
    geo_path = ASSETS / f"geo/entity/{species}.geo.json"
    geo = json.loads(geo_path.read_text(encoding="utf-8"))
    previous = {b["name"]: b for b in geo["minecraft:geometry"][0]["bones"]}
    parents = hierarchy(cfg["items"])
    if cfg.get("legs_on_body"):
        parents["leg_left"] = parents["leg_right"] = "body"
    for bone in keep:
        parents[bone] = cfg.get("reparent", {}).get(bone, previous[bone].get("parent"))
    mesh_dir = ASSETS / "meshes" / species
    if mesh_dir.exists():
        shutil.rmtree(mesh_dir)
    index, used = {"bones": {}}, {}
    for bone in parents:
        part = faces[labels == bone]
        if len(part):
            write_obj(mesh_dir / f"{bone}.obj", vertices, uvs, normals, part)
            index["bones"][bone] = f"kn8:meshes/{species}/{bone}.obj"
            used[bone] = vertices[np.unique(part)]
            print(f"  {bone}: {len(part)} triangulos")
    (ASSETS / "meshes" / f"{species}.json").write_text(json.dumps(index, indent=2) + "\n", encoding="utf-8")
    def cut(bone):
        parent = parents.get(bone)
        if parent not in (None, "root"):
            return seam(vertices, faces, labels, bone, parent)
        return seam(vertices, faces, labels, bone, "body")
    points = pivots(skin, used, cut)
    legacy = json.loads(LEGACY_PIVOTS.read_text(encoding="utf-8")).get(species, {}) if LEGACY_PIVOTS.exists() else {}
    bones = []
    for bone, parent in parents.items():
        pivot = geo_pivot(points[bone]) if bone in points else previous[bone]["pivot"]
        if bone in legacy:
            pivot = legacy[bone]
        # A pegada das armas foi medida no lugar antigo do item (espadas do Hoshina, machado da Kikoru): com a mesma
        # malha, o item fica onde estava (so passa a ser filho da mao).
        if bone.startswith("item_") and bone in previous and not cfg.get("new_mesh"):
            pivot = previous[bone]["pivot"]
        entry = {"name": bone, "pivot": pivot}
        if parent:
            entry["parent"] = parent
        bones.append(entry)
    known = set(parents)
    bones += [b for b in previous.values() if b["name"] not in known]
    geo["minecraft:geometry"][0]["bones"] = bones
    geo_path.write_text(json.dumps(geo, indent=2) + "\n", encoding="utf-8")


def main():
    if sys.argv[1] == "importar":
        import_skin(sys.argv[2], sys.argv[3], SPECIES[sys.argv[2]].get("new_mesh", False))
        return
    for species in sys.argv[1:]:
        print(species)
        build(species)


if __name__ == "__main__":
    main()
