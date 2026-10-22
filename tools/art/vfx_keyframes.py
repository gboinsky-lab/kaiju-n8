#!/usr/bin/env python3
"""VFX-1 (diretriz KaijuVFX §4): marcas de efeito nas animacoes GeckoLib.

Grava o bloco "particle_effects" nas animacoes da tabela MARCAS. No jogo, o controller "action" de kaiju e soldados
especiais entrega cada marca ao cliente (VfxKeyframes), que desenha a receita "effect" do perfil visual da entidade
(data/kn8/kn8/vfx_profile/<id>.json) saindo do "locator" (head/mouth, ground, item_right, item_left, body).

Rodar DEPOIS dos geradores que regravam as animacoes (build_*.py, gen_*_animations.py), porque eles nao conhecem as
marcas:

    python3 tools/art/vfx_keyframes.py
"""
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ANIM = os.path.join(ROOT, 'src/main/resources/assets/kn8/animations/entity')

# arquivo (especie) -> animacao -> [(segundos, receita, origem)]
MARCAS = {
    'trichonephila': {
        # Demonstracao da VFX-1: as presas brilham em amarelo no inicio da mordida (a receita fica no perfil).
        'trichonephila.action.bite': [(0.0, 'bite_windup', 'mouth')],
    },
    # VFX-2: rastro curto e grosso do punho do No. 8 em cada soco (tempos = tick do golpe no JSON / 20).
    'kaiju_no8': {
        'kaiju_no8.action.rush_combo': [(0.2, 'fist_trail', 'item_right'), (0.4, 'fist_trail', 'item_left'),
                                        (0.6, 'fist_trail', 'item_right'), (0.8, 'fist_trail', 'item_left')],
        'kaiju_no8.action.heavy_punch': [(0.4, 'fist_trail', 'item_right')],
        'kaiju_no8.action.dash_strike': [(0.25, 'fist_trail', 'item_right')],
        'kaiju_no8.action.counter': [(0.4, 'fist_trail', 'item_right')],
    },
}


def main():
    for species, animations in MARCAS.items():
        path = os.path.join(ANIM, species + '.animation.json')
        with open(path, newline='') as f:
            text = f.read()
        crlf = '\r\n' in text
        data = json.loads(text)
        for name, marks in animations.items():
            animation = data['animations'].get(name)
            if animation is None:
                print('sem a animacao', name, 'em', species)
                continue
            animation['particle_effects'] = {
                '%s' % float(time): {'effect': effect, 'locator': locator} for time, effect, locator in marks}
        out = json.dumps(data, indent=2) + '\n'
        if crlf:
            out = out.replace('\n', '\r\n')
        with open(path, 'w', newline='') as f:
            f.write(out)
        print(species, sum(len(m) for m in animations.values()), 'marca(s)')


if __name__ == '__main__':
    main()
