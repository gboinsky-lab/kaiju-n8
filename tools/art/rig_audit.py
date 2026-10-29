#!/usr/bin/env python3
"""FIX-2: auditoria dos modelos com ossos articulados (RIG-1/RIG-2) sem abrir o jogo.

Mede, para cada pose pedida:
  - juntas abrindo: para cada par de ossos com malhas encostadas no repouso (ombro, cotovelo, cintura, quadril,
    joelho...), os vertices da costura de um osso sao levados pelos dois ossos; a maior diferenca (px) e quanto a
    junta se abre ("o modelo parte no meio");
  - arma na mao: a arma e montada com a mesma cadeia do SoldierRenderer (pivo do osso do item, eixo do braco em
    repouso, 1 px para baixo e 2 para a frente, Rx(-90), giro animado do item na ordem da PAL, display
    thirdperson_righthand, escala do soldado) e a distancia (px) do punho (fim da malha da mao) ate a malha da arma.

Tambem desenha a previa com a arma (sufixo ":arma" no quadro, como no preview_rig.py).

Uso: python3 tools/art/rig_audit.py <especie> [--poses anim@t+anim@t ...] [--png saida.png]
     sem --poses mede o parado/andando/correndo da familia de arma do perfil (BASE abaixo).
"""
import json
import math
import os
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).parent))
import preview_held_items as ph  # noqa: E402
from preview_rig import Rig, rx, ry, rz, PX  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/kn8"

# Mesma conta do SoldierRenderer (HAND_DOWN, HAND_FORWARD, SOLDIER_ITEM_SCALE).
HAND_DOWN, HAND_FORWARD, SOLDIER_ITEM_SCALE = 1 / 16, 2 / 16, 0.85

# Arma de cada especie (special_soldier/<id>.json; a do Hoshina vai nas duas maos) e familia das animacoes de braco.
WEAPONS = {"hoshina": ("blade", {"item_right": "hoshina_sword", "item_left": "hoshina_sword"}),
           "hoshina_no10": ("blade", {"item_right": "hoshina_sword", "item_left": "hoshina_sword"}),
           "kikoru": ("axe", {"item_right": "axe"}), "kikoru_no4": ("axe", {"item_right": "axe"}),
           "mina": ("cannon", {"item_right": "mina_cannon"}), "reno": ("rifle", {"item_right": "rifle"}),
           "reno_no6": ("rifle", {"item_right": "rifle"}), "narumi": ("spear", {"item_right": "narumi_bayonet"}),
           "kafka": ("rifle", {"item_right": "rifle"}), "soldier": ("rifle", {"item_right": "rifle"}),
           "kaiju_no8": ("fists", {}), "isao": ("fists", {})}


def arm_root(bones, item_bone):
    """Osso do braco inteiro (ombro) acima do osso do item: o SoldierRenderer alinha a arma ao eixo ombro -> item.
    KN8_ITEM_AXIS=parent mede como era antes da FIX-2 (eixo pai do item -> item)."""
    if os.environ.get("KN8_ITEM_AXIS") == "parent":
        return bones[item_bone].get("parent")
    bone = bones[item_bone].get("parent")
    while bone and not bone.startswith("arm_") and bones[bone].get("parent"):
        bone = bones[bone].get("parent")
    return bone if bone and bone.startswith("arm_") else bones[item_bone].get("parent")


class Audit(Rig):
    def __init__(self, species):
        super().__init__(species)
        self.items = {}
        for bone, name in WEAPONS.get(species, (None, {}))[1].items():
            if bone in self.bones:
                tris, _, _, disp = ph.item(name)
                self.items[bone] = (name, tris.reshape(-1, 3), disp)
        self.seams = self._seams()

    # ---------------------------------------------------------------------------------------------- juntas
    def _seams(self):
        """Pares de ossos encostados no repouso e os vertices da costura de cada um (ate 0,6 px do outro)."""
        verts = {b: np.unique(m[0].reshape(-1, 3), axis=0) for b, m in self.meshes.items()}
        out = []
        for a, b in self._joints(verts):
            if True:
                va, vb = verts[a], verts[b]
                lo = np.maximum(va.min(0), vb.min(0)) - 0.05
                hi = np.minimum(va.max(0), vb.max(0)) + 0.05
                if np.any(lo > hi):
                    continue
                sa = va[np.all((va >= lo) & (va <= hi), axis=1)]
                sb = vb[np.all((vb >= lo) & (vb <= hi), axis=1)]
                if len(sa) == 0 or len(sb) == 0:
                    continue
                d = np.sqrt(((sa[:, None, :] - sb[None, :, :]) ** 2).sum(-1)).min(1) if len(sa) * len(sb) < 4e6 \
                    else np.array([np.sqrt(((sb - p) ** 2).sum(1)).min() for p in sa])
                seam = sa[d < 0.6 / PX]
                if len(seam) >= 3:
                    out.append((a, b, seam))
        return out

    def _joints(self, verts):
        """Juntas de verdade: osso e pai (ombro, cotovelo, joelho, pescoco...) e tronco com as pernas (quadril; as
        pernas sao filhas da raiz). Coxas encostadas uma na outra nao contam."""
        pairs = []
        for b in verts:
            parent = self.bones[b].get("parent")
            while parent and parent not in verts:
                parent = self.bones[parent].get("parent")
            if parent:
                pairs.append((parent, b))
        for side in ("left", "right"):
            if "body" in verts and f"leg_{side}" in verts and ("body", f"leg_{side}") not in pairs:
                pairs.append(("body", f"leg_{side}"))
        return pairs

    def gaps(self, world):
        """Junta -> quanto abre (px) na pose."""
        res = {}
        for a, b, seam in self.seams:
            pa = seam @ world[a][:3, :3].T + world[a][:3, 3]
            pb = seam @ world[b][:3, :3].T + world[b][:3, 3]
            res[f"{a}/{b}"] = float(np.percentile(np.linalg.norm(pa - pb, axis=1), 90) * PX)
        return res

    # ---------------------------------------------------------------------------------------------- armas
    def item_world(self, bone, world, rot, pos):
        name, tris, disp = self.items[bone]
        parent = self.bones[bone]["parent"]
        p = self.pivot(bone)
        offset = np.array(pos.get(bone, np.zeros(3)), float)
        base = world[parent] @ ph.T(*(p + np.array([-offset[0], offset[1], offset[2]]) / PX))
        arm = self.pivot(arm_root(self.bones, bone))
        axis = p - arm
        rto = ph.rotation_to((0, -1, 0), axis) if np.linalg.norm(axis) > 1e-4 / PX else np.eye(4)
        k = np.array(rot.get(bone, np.zeros(3)), float)
        anim = ph.Rz(math.radians(-k[1])) @ ph.Ry(math.radians(-k[2])) @ ph.Rx(math.radians(-k[0]))
        scale = disp["thirdperson_righthand"]["scale"][0]
        s = min(SOLDIER_ITEM_SCALE, scale) / scale
        m = base @ rto @ ph.T(0, -HAND_DOWN, -HAND_FORWARD) @ ph.Rx(-math.pi / 2) @ anim @ ph.S(s, s, s) \
            @ ph.display(disp, "thirdperson_righthand")
        return ph.apply(m, tris), m

    def fist(self, bone, world):
        """Punho: o terco final da malha da mao (ou do fim do braco) no mundo."""
        side = bone.split("_")[1]
        hand = f"hand_{side}" if f"hand_{side}" in self.meshes else f"arm_{side}"
        v = self.meshes[hand][0].reshape(-1, 3)
        w = v @ world[hand][:3, :3].T + world[hand][:3, 3]
        elbow = world[hand][:3, :3] @ self.pivot(hand) + world[hand][:3, 3]
        along = np.linalg.norm(w - elbow, axis=1)
        return w[along > np.percentile(along, 50)].mean(0)

    def grip(self, bone, world, rot, pos):
        """Distancia (px) do punho ao eixo da arma (linha do cabo ao longo do comprimento): mao fora do cabo ou arma
        girada para fora da mao aparecem aqui; a malha encostando em qualquer lugar nao conta."""
        pts, _ = self.item_world(bone, world, rot, pos)
        f = self.fist(bone, world)
        center = pts.mean(0)
        axis = np.linalg.svd(pts - center, full_matrices=False)[2][0]
        along = (pts - center) @ axis
        t = float(np.clip((f - center) @ axis, along.min(), along.max()))
        return float(np.linalg.norm(f - (center + t * axis)) * PX)

    def measure(self, spec):
        rot, pos = self.pose(spec)
        world = self.world(rot, pos)
        gaps = self.gaps(world)
        grips = {b: self.grip(b, world, rot, pos) for b in self.items}
        return gaps, grips

    # ---------------------------------------------------------------------------------------------- previa
    def draw(self, spec, view="frente", by_bone=False):
        polys = super().draw(spec, view, by_bone)
        if not self._with_items:
            return polys
        rot, pos = self.pose(spec)
        world = self.world(rot, pos)
        for bone in self.items:
            pts, _ = self.item_world(bone, world, rot, pos)
            if view == "lado":
                pts = np.c_[-pts[:, 2], pts[:, 1], pts[:, 0]]
            elif view == "costas":
                pts = np.c_[pts[:, 0], pts[:, 1], pts[:, 2]]
            else:
                pts = np.c_[-pts[:, 0], pts[:, 1], -pts[:, 2]]
            for tri in pts.reshape(-1, 3, 3):
                polys.append((tri[:, 2].mean() - 0.02, tri, (200, 40, 40)))
        return polys

    _with_items = True


def base_poses(species):
    family = WEAPONS.get(species, ("unarmed", {}))[0]
    names = set(json.loads(
        (ASSETS / f"animations/entity/{species}.animation.json").read_text(encoding="utf-8"))["animations"])

    def arms(state):
        for fam in (family, "unarmed"):
            for st in (state, "walk" if state == "run" else state):
                if f"{species}.arms.{fam}_{st}" in names:
                    return f"arms.{fam}_{st}"
        return None
    out = []
    for move, state, times in (("idle", "ready", (0.0, 0.5)), ("walk", "walk", (0.0, 0.25, 0.5)),
                               ("run", "run", (0.0, 0.15, 0.3))):
        if f"{species}.movement.{move}" not in names:
            continue
        for t in times:
            parts = [f"movement.{move}@{t}"]
            a = arms(state)
            if a:
                parts.append(f"{a}@{t}")
            out.append("+".join(parts))
    return out


def main():
    args = sys.argv[1:]
    species = args.pop(0)
    png = None
    if "--png" in args:
        i = args.index("--png")
        png = args[i + 1]
        del args[i:i + 2]
    if "--poses" in args:
        args.remove("--poses")
    poses = args or base_poses(species)
    audit = Audit(species)
    worst_gap, worst_grip = 0.0, 0.0
    for spec in poses:
        gaps, grips = audit.measure(spec.split(":")[0])
        top = sorted(gaps.items(), key=lambda kv: -kv[1])[:3]
        worst_gap = max(worst_gap, top[0][1] if top else 0)
        worst_grip = max([worst_grip] + list(grips.values()))
        print(f"{spec:55s} juntas: " + ", ".join(f"{k} {v:.1f}" for k, v in top)
              + ("  | arma: " + ", ".join(f"{k} {v:.1f}" for k, v in grips.items()) if grips else ""))
    print(f"{species}: pior junta {worst_gap:.1f} px, pior arma {worst_grip:.1f} px")
    if png:
        audit.render(poses, png)


if __name__ == "__main__":
    main()
