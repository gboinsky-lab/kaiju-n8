"""Ajustes da matriz de duelos (MATRIZ, Miguel 2026-11-06: "Decidir a matriz").

Rodar por ultimo, depois dos geradores que regravam estes arquivos (gen_especiais1/2.py, gen_etapa7b.py,
gen_miguel_camilly.py, gen_numerados_b.py). Muda so os campos listados e mantem o resto do arquivo.

Decisoes medidas em docs/MATRIZ_DECISOES.md:
- Soichiro e Jura (T5+ no dossie) continuam vencendo o No. 13, mas nao mais rapido que o Hoshina (o duelista).
- Miguel e Camilly (comandantes T6) mais perto um do outro contra o No. 13; a Camilly com mais defesa (Miguel,
  2026-11-06: "Mais defesa"): aparar, esquiva e distancia.
- No. 2 e No. 6 (T8+): golpes de area no tamanho do corpo e janela parada menor depois do golpe pesado.
"""
import json
import os
import sys

BASE = os.path.join(os.path.dirname(__file__), '..', '..', 'src', 'main', 'resources', 'data', 'kn8', 'kn8')

CHANGES = {
    'special_soldier/soichiro.json': {'kaiju_damage': 0.75},
    'special_soldier/jura.json': {'kaiju_damage': 0.7},
    'special_soldier/camilly.json': {'kaiju_damage': 1.0, 'parry.chance': 0.45, 'parry.damage_factor': 0.3,
                                     'dash.cooldown_ticks': 70, 'tactics.dodge_chance': 0.9,
                                     'ranged.keep_distance': 16.0, 'ranged.retreat_distance': 10.0},
    'ability/no2_shockwave.json': {'radius': 14.0, 'windup_ticks': 12, 'behavior.max_range': 12.0},
    'ability/no2_crush.json': {'radius': 7.0, 'windup_ticks': 16},
    'ability/no6_avalanche.json': {'radius': 7.5, 'windup_ticks': 16},
    'ability/no6_freezing_field.json': {'radius': 14.0, 'damage_multiplier': 0.6, 'behavior.max_range': 12.0},
    'kaiju_ai/kaiju_no2.json': {'recovery_after_heavy_ticks': 12},
    'kaiju_ai/kaiju_no6.json': {'recovery_after_heavy_ticks': 12},
}


def apply(base):
    for rel, fields in CHANGES.items():
        path = os.path.join(base, rel)
        with open(path, encoding='utf-8', newline='') as f:
            text = f.read()
        data = json.loads(text)
        for key, value in fields.items():
            node = data
            parts = key.split('.')
            for p in parts[:-1]:
                node = node[p]
            node[parts[-1]] = value
        out = json.dumps(data, indent=2, ensure_ascii=False) + '\n'
        if '\r\n' in text:
            out = out.replace('\n', '\r\n')
        with open(path, 'w', encoding='utf-8', newline='') as f:
            f.write(out)
        print('ok', rel)


if __name__ == '__main__':
    apply(sys.argv[1] if len(sys.argv) > 1 else BASE)
