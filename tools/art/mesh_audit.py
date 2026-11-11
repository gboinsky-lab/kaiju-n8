#!/usr/bin/env python3
"""FASE 2-C (documento mestre, Miguel 2026-11-11): auditoria de geometria de todas as malhas do jogo, sem abrir o jogo.

Para cada modelo com malha (assets/kn8/meshes/<id>.json, inclusive trajes e acessorios) mede, por osso:
  - triangulos degenerados (area ~0: nao aparecem, mas indicam reducao quebrada);
  - "postes": triangulos com uma aresta muito maior que a do resto da malha (o lixo da reducao do Meshy que virou o
    poste preto em cima do No. 2): aresta > LONG_FACTOR x a mediana E > LONG_MIN_M metros;
  - UV fora de [0, 1] (a textura repete e puxa pixels do outro lado);
  - pedacos soltos: componentes ligados pequenos (< FRAGMENT_TRIS triangulos) longe do resto do osso (> FLOAT_M m):
    lascas que ficam paradas no ar quando o osso gira;
  - textura que nao existe.
E, para as especies com rig de corpo (rig_audit.WEAPONS), as juntas abrindo no parado/andando/correndo.

Saida: tabela no terminal e, com --md, em markdown (docs/AUDITORIA_GEOMETRIA.md). So le; nada e gravado nos assets.
Uso: python3 tools/art/mesh_audit.py [--md saida.md] [--joints] [id...]   (relatorio: docs/AUDITORIA_GEOMETRIA.md)
     python3 tools/art/mesh_audit.py --fix-poles <id>:<osso> ...  (tira os postes que a auditoria achou)
"""
import json
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).parent))
from rig_trichonephila_mesh import read_obj  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/kn8"
LONG_FACTOR = 12.0
LONG_MIN_M = 0.5
FRAGMENT_TRIS = 24
FLOAT_M = 0.08
JOINT_GAP_PX = 3.0


def components(vertices, faces):
    from scipy.sparse import coo_matrix
    from scipy.sparse.csgraph import connected_components
    _, welded = np.unique(np.round(vertices, 4), axis=0, return_inverse=True)
    welded = welded.reshape(-1)
    wf = welded[faces]
    size = welded.max() + 1
    rows = np.concatenate([wf[:, 0], wf[:, 1], wf[:, 2]])
    cols = np.concatenate([wf[:, 1], wf[:, 2], wf[:, 0]])
    _, label = connected_components(coo_matrix((np.ones(len(rows)), (rows, cols)), shape=(size, size)),
                                    directed=False)
    return label[wf[:, 0]]


def audit_bone(vertices, uvs, faces, scale):
    """scale: metros por unidade da malha (1 nos kaiju/personagens, 1/16 nos trajes em px... ver chamador)."""
    out = {"tris": len(faces), "degenerate": 0, "long": 0, "uv": 0, "fragments": 0}
    if not len(faces):
        return out
    tri = vertices[faces] * scale
    edges = np.stack([np.linalg.norm(tri[:, i] - tri[:, (i + 1) % 3], axis=1) for i in range(3)], axis=1)
    area = 0.5 * np.linalg.norm(np.cross(tri[:, 1] - tri[:, 0], tri[:, 2] - tri[:, 0]), axis=1)
    longest = edges.max(axis=1)
    median = float(np.median(longest)) or 1e-6
    out["degenerate"] = int((area < 1e-10).sum())
    long_mask = (longest > LONG_FACTOR * median) & (longest > LONG_MIN_M)
    out["long"] = int(long_mask.sum())
    out["long_max"] = round(float(longest[long_mask].max()), 2) if long_mask.any() else 0.0
    out["median"] = round(median, 3)
    face_uv = uvs[faces] if len(uvs) else np.zeros((len(faces), 3, 2))
    out["uv"] = int(((face_uv < -1e-3) | (face_uv > 1 + 1e-3)).any(axis=(1, 2)).sum())
    if len(faces) > FRAGMENT_TRIS:
        label = components(vertices, faces)
        counts = np.bincount(label)
        big = counts.argmax()
        from scipy.spatial import cKDTree
        # Solto = longe de QUALQUER outra parte do osso (modelos de blocos soltos encostam nos vizinhos: nao contam).
        frag = 0
        for comp in np.nonzero((counts > 0) & (counts < FRAGMENT_TRIS))[0]:
            mine = label == comp
            pts = tri[mine].reshape(-1, 3)
            tree = cKDTree(tri[~mine].reshape(-1, 3))
            if tree.query(pts)[0].min() > FLOAT_M:
                frag += 1
        out["fragments"] = frag
    return out


def mesh_index(path):
    data = json.loads(path.read_text(encoding="utf-8"))
    return data.get("bones", data)


def audit_model(model_id, index_path):
    bones = mesh_index(index_path)
    rows = []
    texture = None
    suit = "suit/" in model_id or model_id.startswith("suit")
    for bone, location in bones.items():
        path = ASSETS / location.split(":")[1]
        if not path.exists():
            rows.append((bone, {"missing": True}))
            continue
        v, uv, _, f = read_obj(path)
        # Trajes e acessorios: blocos (1 bloco = 1 m); a segunda pele (24 ultimos triangulos) e caixa de proposito.
        if suit and len(f) > 24:
            f = f[:-24]
        rows.append((bone, audit_bone(v, uv, f, 1.0)))
    if not suit:
        texture = ASSETS / f"textures/entity/{model_id}.png"
    else:
        texture = ASSETS / f"textures/models/{model_id}.png"
    return rows, texture.exists()


def joints(species):
    import rig_audit
    audit = rig_audit.Audit(species)
    worst = []
    for spec in rig_audit.base_poses(species):
        gaps, _ = audit.measure(spec)
        for joint, gap in gaps.items():
            if gap > JOINT_GAP_PX:
                worst.append((spec, joint, round(gap, 1)))
    return worst


def main():
    args = sys.argv[1:]
    md = None
    if "--md" in args:
        i = args.index("--md")
        md = Path(args[i + 1])
        del args[i:i + 2]
    with_joints = "--joints" in args
    if with_joints:
        args.remove("--joints")
    targets = []
    for path in sorted((ASSETS / "meshes").glob("*.json")):
        targets.append((path.stem, path))
    for path in sorted((ASSETS / "meshes/suit").glob("*.json")):
        targets.append(("suit/" + path.stem, path))
    if args:
        targets = [t for t in targets if t[0] in args]
    lines = ["| Modelo | Triangulos | Degenerados | Postes | UV fora | Pedacos soltos | Textura | Osso pior |",
             "|---|---|---|---|---|---|---|---|"]
    problems = []
    for model_id, path in targets:
        rows, has_texture = audit_model(model_id, path)
        total = {k: sum(r.get(k, 0) for _, r in rows) for k in ("tris", "degenerate", "long", "uv", "fragments")}
        missing = [b for b, r in rows if r.get("missing")]
        worst = max(rows, key=lambda r: r[1].get("long", 0) * 10 + r[1].get("fragments", 0))[0] if rows else "-"
        flag = total["long"] or total["uv"] or total["fragments"] or missing or not has_texture
        lines.append(f"| {model_id} | {total['tris']} | {total['degenerate']} | {total['long']} | {total['uv']} | "
                     f"{total['fragments']} | {'sim' if has_texture else 'NAO'} | {worst if flag else '-'} |")
        if flag:
            problems.append((model_id, total, missing, [(b, r) for b, r in rows
                                                         if r.get("long") or r.get("fragments") or r.get("uv")]))
        print(lines[-1])
    if with_joints:
        import rig_audit
        lines += ["", "| Especie | Pose | Junta | Abertura (px) |", "|---|---|---|---|"]
        for species in rig_audit.WEAPONS:
            for spec, joint, gap in joints(species):
                lines.append(f"| {species} | {spec} | {joint} | {gap} |")
                print(f"junta {species} {spec} {joint} {gap}")
    if md:
        md.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"{len(problems)} modelo(s) com algo a olhar")
    for model_id, total, missing, bones in problems:
        detail = ", ".join(f"{b}: {r.get('long', 0)} postes (ate {r.get('long_max', 0)} m, mediana "
                           f"{r.get('median', 0)})/{r.get('fragments', 0)} soltos/{r.get('uv', 0)} uv"
                           for b, r in bones)
        print(f"  {model_id}: {detail}{' faltando ' + str(missing) if missing else ''}")


def fix_poles(model_id, bone):
    """Tira os "postes" de um osso (triangulos de tampa esticados que atravessam o corpo; a lista sai da auditoria).
    So apaga linhas f do OBJ: vertices, UV e o resto ficam (o original continua no historico do Git)."""
    location = mesh_index(ASSETS / f"meshes/{model_id}.json")[bone].split(":")[1]
    path = ASSETS / location
    v, _, _, f = read_obj(path)
    tri = v[f]
    longest = np.stack([np.linalg.norm(tri[:, i] - tri[:, (i + 1) % 3], axis=1) for i in range(3)], 1).max(1)
    bad = set(np.nonzero((longest > LONG_FACTOR * np.median(longest)) & (longest > LONG_MIN_M))[0].tolist())
    lines = path.read_bytes().decode("utf-8").splitlines(keepends=True)
    out, index = [], 0
    for line in lines:
        if line.startswith("f "):
            if len(line.split()) != 4:
                raise SystemExit(f"{path}: face que nao e triangulo")
            index += 1
            if index - 1 in bad:
                continue
        out.append(line)
    if index != len(f):
        raise SystemExit(f"{path}: {index} faces no arquivo, {len(f)} lidas")
    path.write_bytes("".join(out).encode("utf-8"))
    print(f"{model_id}/{bone}: {len(bad)} poste(s) tirado(s)")


if __name__ == "__main__":
    if sys.argv[1:2] == ["--fix-poles"]:
        for target in sys.argv[2:]:
            fix_poles(*target.split(":"))
    else:
        main()
