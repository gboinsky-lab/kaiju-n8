"""Estimativa do jogador por patente contra cada kaiju (B-2). So contas pelos JSON e pelos padroes do config: nao roda
o jogo. Tempo para derrubar = vida / dano por segundo do golpe leve em sequencia (combo, Release, forca, armadura do
kaiju, teto de 12% por golpe); golpes para cair = vida do jogador / golpe medio das habilidades do kaiju (armadura do
traje, Release e resistencia). Nao conta esquiva, bloqueio, pesado, especial, nucleo, partes, pesquisa nem esquadrao.

Perfis por patente [SUPOSICAO]: o Release e o corpo sobem por treino, nao pela patente; aqui vai um jogador tipico.

Uso: python3 tools/balance/jogador_por_patente.py > docs/B2_JOGADOR_POR_PATENTE.md"""
import json
import os

ROOT = os.path.join(os.path.dirname(__file__), '..', '..', 'src', 'main', 'resources', 'data', 'kn8', 'kn8')
# Padroes do kn8-server.toml (ServerConfig).
RELEASE_DIVISOR = 25.0
STRENGTH_PER_LEVEL = 0.003
RESISTANCE_PER_LEVEL = 0.002
REDUCTION_PER_RELEASE = 0.004
MAX_REDUCTION = 0.4
ATTACK_SPEED_FULL = 0.5
LIGHT_CAP = 0.12

# patente, Release usado (limite pessoal tipico), nivel do corpo, traje, melhor arma corpo a corpo, arma de fogo
PROFILES = [
    ('Candidato', 8, 0, 'training_suit', 'combat_knife', None),
    ('Oficial', 15, 10, 'mk1', 'combat_knife', 'rifle'),
    ('Oficial Senior', 25, 20, 'mk1_reinforced', 'sword', 'rifle'),
    ('Lider de Pelotao', 40, 35, 'mk1_reinforced', 'axe', 'rifle'),
    ('Vice-Capitao', 60, 50, 'mk1_reinforced', 'hoshina_sword', 'rifle'),
    ('Capitao', 80, 70, 'mk1_reinforced', 'hoshina_sword', 'rifle'),
]
KAIJU = ['trichonephila', 'camponotus', 'phaneroplasmodium', 'primigenius', 'primigenius_resurrected',
         'primigenius_honju', 'primigenius_revived', 'philinosoma', 'diclonius', 'myxogasterocarp',
         'trichonephila_honju', 'preondactyl', 'kaiju_no13', 'kaiju_no9', 'kaiju_no10_small', 'kaiju_no10_giant']
RANK_HEALTH = {'Candidato': 20, 'Oficial': 22, 'Oficial Senior': 24, 'Lider de Pelotao': 26, 'Vice-Capitao': 28,
               'Capitao': 30}


def load(kind, name):
    with open(os.path.join(ROOT, kind, name + '.json'), encoding='utf-8') as f:
        return json.load(f)


def armored(damage, armor, toughness=0.0):
    # Formula vanilla (CombatRules.getDamageAfterAbsorb).
    reduced = armor - damage / (2.0 + toughness / 4.0)
    return damage * (1.0 - min(20.0, max(armor * 0.2, reduced)) / 25.0)


def player_dps(weapon, release, body, kaiju_health, kaiju_armor):
    w = load('weapon', weapon)
    light = w['actions']['light']
    combo = w.get('combo') or [1.0]
    speed = 1.0 + ATTACK_SPEED_FULL * release / 100.0
    firearm = w.get('style') == 'firearm'
    strength = 1.0 if firearm else 1.0 + STRENGTH_PER_LEVEL * body
    mult = 1.0 + release / RELEASE_DIVISOR
    total = 0.0
    for c in combo:
        hit = w['base_damage'] * light['multiplier'] * c * mult * strength
        hit = min(armored(hit, kaiju_armor), kaiju_health * LIGHT_CAP)
        total += hit
    seconds = len(combo) * light['duration_ticks'] / speed / 20.0
    return total / seconds


def kaiju_hit(kaiju):
    damage = kaiju['overrides']['damage']
    mults = []
    for ability in kaiju.get('abilities', []):
        try:
            mults.append(load('ability', ability.split(':')[1]).get('damage_multiplier', 1.0))
        except FileNotFoundError:
            pass
    return damage * (sum(mults) / len(mults) if mults else 1.0)


def main():
    print('# B-2 — jogador por patente contra cada kaiju (estimativa)\n')
    print('Gerado por `tools/balance/jogador_por_patente.py`. Cada celula: **segundos para derrubar com o golpe leve'
          ' em sequencia** (corpo a corpo / arma de fogo) e **golpes medios do kaiju para o jogador cair**. So o'
          ' combate solo, sem esquiva, bloqueio, pesado, especial, nucleo, pesquisa nem esquadrao. Perfis de cada'
          ' patente [SUPOSICAO]:\n')
    print('| Patente | Release | Corpo | Traje | Corpo a corpo | Arma de fogo | Vida |')
    print('|---|---|---|---|---|---|---|')
    for name, release, body, suit, melee, gun in PROFILES:
        print(f'| {name} | {release}% | {body} | {suit} | {melee} | {gun or "-"} | {RANK_HEALTH[name]} |')
    print()
    header = '| Kaiju | Vida | ' + ' | '.join(p[0] for p in PROFILES) + ' |'
    print(header)
    print('|' + '---|' * (len(PROFILES) + 2))
    for kid in KAIJU:
        k = load('kaiju', kid)
        health, armor = k['overrides']['health'], k['overrides'].get('armor', 0.0)
        hit = kaiju_hit(k)
        cells = []
        for name, release, body, suit, melee, gun in PROFILES:
            s = load('suit', suit)
            melee_t = health / player_dps(melee, release, body, health, armor)
            gun_t = health / player_dps(gun, release, body, health, armor) if gun else None
            reduction = min(MAX_REDUCTION, REDUCTION_PER_RELEASE * release) + RESISTANCE_PER_LEVEL * body
            taken = armored(hit, s['armor'], s.get('toughness', 0.0)) * max(0.0, 1.0 - reduction)
            hits = RANK_HEALTH[name] / taken
            gun_txt = f' / {gun_t:.0f}' if gun_t else ''
            cells.append(f'{melee_t:.0f}{gun_txt} s · {hits:.1f}')
        print(f'| {kid} | {health:.0f} | ' + ' | '.join(cells) + ' |')


if __name__ == '__main__':
    main()
