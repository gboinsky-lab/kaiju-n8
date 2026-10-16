"""MOV-2: animacao do salto tatico dos soldados (`<especie>.action.vault`).

Uma animacao para todas as especies de soldado (mesmos ossos: root, body, pernas), gravada por cima de cada
`animations/entity/<especie>.animation.json`. Rodar DEPOIS dos scripts que regravam esses arquivos
(`rig_soldier_mesh.py`, `gen_special_animations.py`, `gen_hoshina_animations.py`, `gen_kikoru_animations.py`).

Os bracos ficam com a postura da arma (controller "arms"): o salto mexe so no corpo e nas pernas, para a arma nao
sair da mao. O impulso vem do servidor (`SoldierLeap`); o tempo bate com `LEAP_TICKS` (agachar 3 ticks, no ar ate
~11, pouso amortecido ate 14).

Uso: python3 tools/art/gen_vault_animations.py
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/kn8/animations/entity"
SPECIES = ["soldier", "hoshina", "hoshina_no10", "kikoru", "kikoru_no4", "reno", "reno_no6", "mina", "narumi",
           "kafka", "kaiju_no8", "isao"]
LENGTH = 0.7


def kf(*frames):
    return {str(round(t, 3)): list(v) for t, v in frames}


def vault():
    # Corpo: positivo em X = inclina para a frente; pernas: negativo em X = joelho para a frente.
    return {"animation_length": LENGTH, "bones": {
        "body": {
            "rotation": kf((0.0, [0, 0, 0]), (0.15, [18, 0, 0]), (0.3, [12, 0, 0]), (0.55, [20, 0, 0]),
                           (LENGTH, [0, 0, 0])),
            "position": kf((0.0, [0, 0, 0]), (0.15, [0, -2.5, 0]), (0.25, [0, 0.5, 0]), (0.55, [0, -2, 0]),
                           (LENGTH, [0, 0, 0])),
        },
        "leg_right": {"rotation": kf((0.0, [0, 0, 0]), (0.15, [-25, 0, 4]), (0.3, [-70, 0, 6]), (0.45, [-50, 0, 4]),
                                     (0.55, [-20, 0, 4]), (LENGTH, [0, 0, 0]))},
        "leg_left": {"rotation": kf((0.0, [0, 0, 0]), (0.15, [-25, 0, -4]), (0.3, [-55, 0, -6]), (0.45, [-35, 0, -4]),
                                    (0.55, [-15, 0, -4]), (LENGTH, [0, 0, 0]))},
    }}


def main():
    for species in SPECIES:
        path = ASSETS / f"{species}.animation.json"
        raw = path.read_bytes().decode("utf-8")
        newline = "\r\n" if "\r\n" in raw else "\n"
        data = json.loads(raw)
        data["animations"][f"{species}.action.vault"] = vault()
        text = json.dumps(data, indent=2) + "\n"
        path.write_bytes(text.replace("\n", newline).encode("utf-8"))
        print(f"{species}: {species}.action.vault")


if __name__ == "__main__":
    main()
