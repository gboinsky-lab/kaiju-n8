#!/usr/bin/env python3
"""NUMERADOS-C (Miguel 2026-11-06: "Continue"): o que ficou de fora dos numerados do anime na NUMERADOS-A.

- No. 12: regeneracao e adaptacao ao tipo de golpe (o mesmo mecanismo do No. 9, numbered/kaiju_no12.json; a
  adaptacao e mais forte que a do No. 9: e a especialidade dele) [SUPOSICAO numeros].
- No. 15: a forma Kikoru (disfarce) revela a forma kaiju abaixo de 50% da vida (transform do
  numbered/kaiju_no15_kikoru.json, como o No. 10 pequeno -> gigante) [SUPOSICAO sentido e limiar].
- No. 11: hidrocinese que depende de agua: com agua perto (dentro, chuva ou bloco de agua a 6 blocos) golpes x1,3 e
  recargas x0,75; seco, golpes x0,85 (bloco water do kaiju_ai/kaiju_no11.json) [SUPOSICAO numeros].
A precognicao do No. 1 ja entrou na 7-C (foresight).

Uso: python3 tools/data/gen_numerados_c.py   (depois do gen_numerados_a.py e do gen_etapa7c.py)
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from gen_numerados_a import DATA, write  # noqa: E402

LANG = Path(__file__).resolve().parents[2] / "src/main/resources/assets/kn8/lang"
MESSAGES = {
    "kn8.no15.true_form": ("O disfarce caiu: o Kaiju No. 15 mostrou a forma verdadeira!",
                           "The disguise falls: Kaiju No. 15 reveals its true form!"),
}


def numbered():
    write("numbered", "kaiju_no12", {
        "command_radius": 0.0, "flee_health": 0.0,
        "regeneration": {"below": 0.5, "per_second": 0.012, "fast_below": 0.2, "fast_per_second": 0.025,
                         "delay_after_hit_ticks": 20},
        "adaptation": {"per_hit": 0.03, "max_reduction": 0.3, "decay_ticks": 120, "ranged_factor": 1.0}})
    write("numbered", "kaiju_no15_kikoru", {
        "command_radius": 0.0, "flee_health": 0.0,
        "transform": {"into": "kn8:kaiju_no15", "after_combat_ticks": 72000, "health_below": 0.5,
                      "health_fraction": 0.5, "destruction_radius": 0.0, "message": "kn8.no15.true_form"}})


def water():
    path = DATA / "kaiju_ai/kaiju_no11.json"
    data = json.loads(path.read_text(encoding="utf-8"))
    data["water"] = {"radius": 6.0, "wet_damage": 1.3, "wet_cooldown": 0.75, "dry_damage": 0.85}
    write("kaiju_ai", "kaiju_no11", data)


def lang():
    for name, index in (("pt_br", 0), ("en_us", 1)):
        path = LANG / f"{name}.json"
        text = path.read_text(encoding="utf-8")
        data = json.loads(text)
        added = [key for key in MESSAGES if key not in data]
        if not added:
            continue
        body = text.rstrip().rstrip("}").rstrip()
        lines = "".join(f',\n  "{key}": {json.dumps(MESSAGES[key][index], ensure_ascii=False)}' for key in added)
        path.write_text(body + lines + "\n}\n", encoding="utf-8")


def main():
    numbered()
    water()
    lang()
    print("NUMERADOS-C: dados gravados")


if __name__ == "__main__":
    main()
