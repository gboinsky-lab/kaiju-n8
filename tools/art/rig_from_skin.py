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
SPECIES = {
    # Isao: as manoplas do Numbers 2 vao com a mao; item invisivel (kn8:numbers2_gauntlet) no punho.
    "isao": {"items": True},
    # Formas de kaiju: sem item. O No. 9 com o Isao tem espinhos nos ombros que o Meshy dividiu entre ombro e braco.
    "kaiju_no9_isao": {"items": False},
    "kaiju_no13": {"items": False},
}
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


def import_skin(species, glb):
    """Pesos e juntas do GLB levados para o espaco da malha convertida (metros, frente -Z, pes em Y 0)."""
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
    vertices = read_obj(CONVERTED / species / f"{species}.obj")[0]
    # O Meshy olha para +Z; a malha convertida para -Z: giro de 180 graus, mesma escala e mesmos pes.
    flip = np.array([-1.0, 1.0, -1.0])
    scale = np.ptp(vertices[:, 1]) / np.ptp(positions[:, 1])

    def place(points):
        out = points * scale * flip
        return out
    moved = place(positions)
    offset = np.array([vertices[:, 0].mean() - moved[:, 0].mean(), vertices[:, 1].min() - moved[:, 1].min(),
                       vertices[:, 2].mean() - moved[:, 2].mean()])
    moved += offset
    distance, nearest = cKDTree(moved).query(vertices)
    print(f"{species}: {len(vertices)} vertices, distancia ao GLB media {distance.mean() * 100:.1f} cm, "
          f"maxima {distance.max() * 100:.1f} cm")
    SKINS.mkdir(parents=True, exist_ok=True)
    np.savez_compressed(SKINS / f"{species}.npz", joints=joints[nearest].astype(np.uint8),
                        weights=np.round(weights[nearest], 3).astype(np.float16), names=np.array(names),
                        bind=place(bind) + offset)


# ------------------------------------------------------------------------------------------------ cortar
def bone_of(name, position):
    part = JOINT_PART.get(name.replace("Left", "").replace("Right", ""))
    if part is None:
        return "body"
    if part in SIDED:
        return f"{part}_{'left' if position[0] < 0 else 'right'}"
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


def pivots(skin, meshes):
    names, bind = list(skin["names"]), skin["bind"]
    out = {"root": np.zeros(3)}
    for bone, joint in PIVOT_JOINT.items():
        for side in ("left", "right") if bone in SIDED else (None,):
            key = f"{bone}_{side}" if side else bone
            candidates = [bind[i] for i, n in enumerate(names)
                          if n.replace("Left", "").replace("Right", "") == joint
                          and (side is None or (bind[i][0] < 0) == (side == "left"))]
            out[key] = candidates[0]
    for side in ("left", "right"):
        # Pegada: entre o punho e o centro da mao (onde ficaria o cabo de uma arma).
        hand = meshes.get(f"hand_{side}")
        wrist = out[f"hand_{side}"]
        out[f"item_{side}"] = wrist + 0.5 * (hand.mean(axis=0) - wrist) if hand is not None else wrist
    return out


def build(species):
    cfg = SPECIES[species]
    skin = dict(np.load(SKINS / f"{species}.npz"))
    vertices, uvs, normals, faces = read_obj(CONVERTED / species / f"{species}.obj")
    labels = label_faces(vertices, faces, skin)
    vertices, uvs, normals, faces, labels = cap_holes(vertices, uvs, normals, faces, labels)
    parents = hierarchy(cfg["items"])
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
    points = pivots(skin, used)
    geo_path = ASSETS / f"geo/entity/{species}.geo.json"
    geo = json.loads(geo_path.read_text(encoding="utf-8"))
    bones = []
    for bone, parent in parents.items():
        entry = {"name": bone, "pivot": geo_pivot(points[bone])}
        if parent:
            entry["parent"] = parent
        bones.append(entry)
    # Ossos da especie que nao sao do corpo humanoide (cauda, asas) continuam como estavam.
    known = set(parents)
    bones += [b for b in geo["minecraft:geometry"][0]["bones"] if b["name"] not in known]
    geo["minecraft:geometry"][0]["bones"] = bones
    geo_path.write_text(json.dumps(geo, indent=2) + "\n", encoding="utf-8")


def main():
    if sys.argv[1] == "importar":
        import_skin(sys.argv[2], sys.argv[3])
        return
    for species in sys.argv[1:]:
        print(species)
        build(species)


if __name__ == "__main__":
    main()
