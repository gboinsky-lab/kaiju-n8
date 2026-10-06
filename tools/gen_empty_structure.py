#!/usr/bin/env python3
"""Gera templates de estrutura vazios (so ar) usados pelos GameTests, em data/kn8/structure/.

Por que um script: arquivos .nbt sao binarios; gerar por script deixa os templates reproduziveis e revisaveis.
O GameTest cerca o template com barreiras: testes com raycast precisam que o raio comece DENTRO do template.

Templates:
  empty_3x3    3x3x3  (testes sem raycast)
  empty_9x7x9  9x7x9  (PT7+: kaiju de 4,6 blocos de altura e raio comecando dentro da estrutura)

Uso: pip install nbtlib && python3 tools/gen_empty_structure.py
"""
from pathlib import Path

from nbtlib import Compound, File, Int, List, String

DATA_VERSION_1_21_1 = 3955  # DataVersion do Minecraft 1.21.1
TEMPLATES = {
    "empty_3x3": (3, 3, 3),
    "empty_9x7x9": (9, 7, 9),
    # M11a: pista comprida para o rifle (>20 blocos) e a investida (charge).
    "empty_9x7x33": (9, 7, 33),
    # 0.1-B: Honju com 9 blocos de altura.
    "empty_15x12x15": (15, 12, 15),
}


def build(size_x, size_y, size_z):
    blocks = List[Compound]([
        Compound({"pos": List[Int]([Int(x), Int(y), Int(z)]), "state": Int(0)})
        for x in range(size_x) for y in range(size_y) for z in range(size_z)
    ])
    return Compound({
        "DataVersion": Int(DATA_VERSION_1_21_1),
        "size": List[Int]([Int(size_x), Int(size_y), Int(size_z)]),
        "palette": List[Compound]([Compound({"Name": String("minecraft:air")})]),
        "blocks": blocks,
        "entities": List[Compound]([]),
    })


out_dir = Path(__file__).resolve().parent.parent / "src/main/resources/data/kn8/structure"
out_dir.mkdir(parents=True, exist_ok=True)
for name, size in TEMPLATES.items():
    path = out_dir / f"{name}.nbt"
    File(build(*size)).save(path, gzipped=True)
    print(f"Gerado: {path} {size}")
