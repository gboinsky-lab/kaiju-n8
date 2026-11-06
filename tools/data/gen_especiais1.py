#!/usr/bin/env python3
"""ESPECIAIS-1 (modelos do Miguel de 2026-11-05): auras, perfis visuais e de som de Soichiro Hoshina, Jura Igarashi,
Rin Shinonome, Iharu Furuhashi e Haruichi Izumo.

Paletas tiradas das texturas dos modelos e do dossie do Miguel (docs/especiais_1 no CLAUDE.md): Soichiro lilas do
cabelo com a lamina preta; Jura a bola de ferro em chamas (laranja/vermelho); Rin tiros e a rajada incendiaria
(laranja sobre o verde-oliva do traje); Iharu a municao eletrificada (amarelo); Haruichi teal do cabelo e as flechas
envenenadas (verde). Efeitos so desenhados (raios, faixas, ondas, labaredas), sem particulas, crescendo com o corpo
(regra da VFX-2). Tamanhos e tempos [SUPOSICAO].

Uso: python3 tools/data/gen_especiais1.py
"""
import json
from pathlib import Path

DATA = Path(__file__).resolve().parents[2] / "src/main/resources/data/kn8/kn8"


def L(shape, color="accent", pattern="burst", count=1, size=0.3, speed=0.0, spread=1.0, lifetime=10, forward=0.8,
      up=1.2, delay=0, tilt=0.0, edge=""):
    layer = {"shape": shape, "color": color, "pattern": pattern, "count": count, "size": size, "speed": speed,
             "spread": spread, "lifetime": lifetime, "forward": forward, "up": up}
    if delay:
        layer["delay"] = delay
    if tilt:
        layer["tilt"] = tilt
    if edge:
        layer["edge"] = edge
    return layer


def muzzle(color, smoke="#9A9A9A"):
    return [L("flash", "core", size=0.25, spread=0.25, lifetime=2, forward=0.9, up=1.35),
            L("sparks", color, "line", 3, 0.06, 0.5, 1.0, 4, 0.9, 1.35),
            L("smoke", smoke, size=0.2, speed=0.02, spread=0.2, lifetime=12, forward=0.9, up=1.35)]


def write(folder, name, data):
    path = DATA / folder / f"{name}.json"
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def soichiro():
    cut = lambda tilt, size=1.6: L("band", "secondary", "arc", 1, 0.5, 0.0, size, 9, 1.0, 1.2, tilt=tilt,
                                   edge="accent")
    effects = {
        "poised_marksmanship": muzzle("accent"),
        "poised_marksmanship_impact": [L("sparks", "accent", count=4, size=0.07, speed=0.4, lifetime=5, forward=0,
                                         up=0)],
        # Guarda que muda: corte e tiro juntos, abrindo a brecha.
        "adaptive_stance": [cut(-25), L("flash", "core", size=0.3, spread=0.3, lifetime=2, forward=1.2, up=1.3,
                                        delay=4), L("sparks", "accent", "line", 3, 0.06, 0.6, 1.4, 5, 1.0, 1.3,
                                                    delay=4)],
        "adaptive_stance/1": [cut(-25)],
        "adaptive_stance/2": [cut(30), L("flash", "core", size=0.3, spread=0.3, lifetime=2, forward=1.2, up=1.3)],
        # Esgrima prodigiosa: quatro cortes em angulos diferentes, o ultimo largo.
        "prodigious_swordsmanship": [cut(0)],
        "prodigious_swordsmanship/1": [cut(-35)],
        "prodigious_swordsmanship/2": [cut(35)],
        "prodigious_swordsmanship/3": [cut(-70)],
        "prodigious_swordsmanship/4": [cut(0, 2.6), L("pulse", "accent", size=0.9, spread=1.0, lifetime=6,
                                                      forward=1.2, up=1.2)],
        "astute_strike": [cut(10, 1.3)],
        "astute_strike_trail": [L("band", "secondary", "arc", 1, 0.35, 0.0, 1.1, 4, 0, 0, edge="accent")],
        "astute_strike_impact": [L("sparks", "accent", count=5, size=0.07, speed=0.5, lifetime=6, forward=0, up=0)],
        # Golpe final magistral: corte vertical enorme com clarao e onda no chao.
        "finishing_masterstroke_charge": [L("bolt", "accent", "converge", 4, 0.07, 0.0, 1.2, 10, 0.4, 1.3),
                                          L("bolt", "accent", "converge", 4, 0.07, 0.0, 1.2, 10, 0.4, 1.3,
                                            delay=8)],
        "finishing_masterstroke": [L("band", "secondary", "arc", 1, 0.8, 0.0, 3.2, 12, 1.2, 1.4, tilt=90,
                                     edge="accent"),
                                   L("flash", "core", size=0.8, spread=0.8, lifetime=3, forward=1.4, up=1.2),
                                   L("wave", "accent", "ring", 1, 0.3, 0.0, 3.0, 10, 1.4, 0.05),
                                   L("crack", "secondary", "line", 3, 0.2, 0.0, 2.5, 30, 1.2, 0.02, edge="accent")],
        "counter": [cut(45, 1.8)],
    }
    write("vfx_profile", "soichiro", {"palette": {"primary": "#1A1A1E", "secondary": "#0E0D10", "accent": "#777DE0",
                                                  "core": "#D9DCEB"}, "material": "suit", "effects": effects})
    write("aura", "soichiro_lilac", {"color": "#6A74D0", "secondary": "#D9DCEB", "style": "lightning",
                                     "min_release": 1, "size": 0.95,
                                     "bolts": {"color": "#777DE0", "core": "#FFFFFF", "rate": 5, "length": 0.28,
                                               "branches": 0.3, "crawl": 0.5, "width": 0.045}})
    write("sound_profile", "soichiro", {"techniques": {
        "poised_marksmanship": {"release": "kn8:weapon.pistol_shot"},
        "adaptive_stance": {"release": "kn8:technique.hoshina_slash"},
        "prodigious_swordsmanship": {"release": "kn8:technique.hoshina_slash"},
        "astute_strike": {"release": "kn8:technique.hoshina_release"},
        "finishing_masterstroke": {"windup": "kn8:technique.charge", "release": "kn8:weapon.sword_heavy"}}})


def jura():
    flame = lambda count=3, up=1.0, spread=1.0, delay=0: L("wisp", "primary", "rise", count, 0.35, 0.2, spread, 14,
                                                           0.6, up, delay=delay)
    effects = {
        "herculean_toss": [L("pulse", "accent", size=0.7, spread=0.8, lifetime=6, forward=0.6, up=1.0), flame(2)],
        "herculean_toss_trail": [L("wisp", "primary", "rise", 2, 0.3, 0.1, 0.4, 8, 0, 0),
                                 L("smoke", "secondary", size=0.25, speed=0.02, spread=0.2, lifetime=14, forward=0,
                                   up=0)],
        "herculean_toss_impact": [L("flash", "accent", size=0.5, spread=0.5, lifetime=3, forward=0, up=0),
                                  L("wave", "primary", "ring", 1, 0.25, 0.0, 1.8, 10, 0, 0.05),
                                  L("wisp", "primary", "rise", 4, 0.35, 0.25, 1.0, 14, 0, 0)],
        "herculean_toss_explosion": [L("wave", "primary", "ring", 1, 0.3, 0.0, 2.0, 12, 0, 0.05),
                                     L("smoke", "secondary", size=0.4, speed=0.05, spread=0.8, lifetime=20, forward=0,
                                       up=0.2)],
        # Sangue quente: incendeia o espirito da equipe (aneis de calor subindo) e bate com a bola.
        "hot_blooded_impact_charge": [L("pulse", "accent", "rise", 3, 0.9, 0.2, 1.4, 10, 0, 0.3),
                                      flame(4, 0.2, 1.4), L("pulse", "accent", "rise", 3, 0.9, 0.2, 1.4, 10, 0, 0.3,
                                                            delay=6)],
        "hot_blooded_impact": [L("flash", "accent", size=0.6, spread=0.6, lifetime=3, forward=1.4, up=0.8),
                               L("wave", "primary", "ring", 1, 0.3, 0.0, 3.0, 12, 1.4, 0.05),
                               flame(5, 0.2, 2.0)],
        # Mergulho do zenite: a bola sobe e cai, racha o chao e o calor sobe em volta.
        "zenith_plunge_charge": [L("wisp", "primary", "rise", 3, 0.35, 0.3, 0.6, 14, 0.4, 2.2)],
        "zenith_plunge": [L("flash", "accent", size=0.9, spread=0.9, lifetime=3, forward=1.4, up=0.3),
                          L("crack", "secondary", "line", 5, 0.25, 0.0, 3.0, 40, 1.4, 0.02, edge="primary"),
                          L("wave", "primary", "ring", 1, 0.35, 0.0, 3.6, 12, 1.4, 0.05),
                          L("wave", "accent", "ring", 1, 0.25, 0.0, 2.4, 10, 1.4, 0.05, delay=4),
                          flame(6, 0.1, 2.4, 2),
                          L("smoke", "secondary", size=0.5, speed=0.06, spread=1.4, lifetime=26, forward=1.4, up=0.2,
                            delay=4)],
        "blazing_slugger": [L("sparks", "accent", count=5, size=0.08, speed=0.6, lifetime=6, forward=1.2, up=1.0),
                            flame(2, 1.0, 0.6)],
        "counter": [L("sparks", "accent", count=5, size=0.08, speed=0.6, lifetime=6, forward=1.2, up=1.0)],
    }
    write("vfx_profile", "jura", {"palette": {"primary": "#FF5A1F", "secondary": "#2A2624", "accent": "#FFB347",
                                              "core": "#FFF2D8"}, "material": "metal", "effects": effects})
    write("aura", "jura_flame", {"color": "#FF5A1F", "secondary": "#FFB347", "style": "flame", "min_release": 1,
                                 "size": 1.05,
                                 "bolts": {"color": "#FF8A3C", "core": "#FFF2D8", "rate": 5, "length": 0.3,
                                           "branches": 0.6, "crawl": 0.4, "width": 0.06}})
    write("sound_profile", "jura", {"techniques": {
        "herculean_toss": {"release": "kn8:weapon.axe_heavy"},
        "hot_blooded_impact": {"windup": "kn8:technique.charge", "release": "kn8:weapon.axe_special"},
        "zenith_plunge": {"windup": "kn8:technique.charge", "release": "kn8:technique.kikoru_thunder"},
        "blazing_slugger": {"release": "kn8:weapon.axe_swing"}}})


def rin():
    effects = {
        "focused_shot": muzzle("accent"),
        "focused_shot_impact": [L("sparks", "core", count=4, size=0.07, speed=0.4, lifetime=5, forward=0, up=0)],
        "thorough_sweep_fire": muzzle("core") + [L("sparks", "accent", "arc", 6, 0.06, 0.6, 1.6, 4, 0.9, 1.35)],
        "thorough_sweep_fire_impact": [L("sparks", "core", count=3, size=0.06, speed=0.4, lifetime=4, forward=0,
                                         up=0)],
        "incinerating_barrage": muzzle("accent", "#5A4A40"),
        "incinerating_barrage_trail": [L("wisp", "accent", "rise", 1, 0.2, 0.05, 0.2, 6, 0, 0)],
        "incinerating_barrage_impact": [L("flash", "accent", size=0.35, spread=0.35, lifetime=2, forward=0, up=0),
                                        L("wisp", "accent", "rise", 3, 0.3, 0.2, 0.6, 12, 0, 0)],
        "circular_barrage": muzzle("core") + [L("wave", "accent", "ring", 1, 0.2, 0.0, 3.0, 10, 0, 0.05)],
        "circular_barrage_impact": [L("sparks", "accent", count=3, size=0.06, speed=0.4, lifetime=4, forward=0,
                                      up=0)],
        "baton_strike": [L("sparks", "core", count=4, size=0.07, speed=0.5, lifetime=5, forward=1.0, up=1.1),
                         L("pulse", "core", size=0.5, spread=0.5, lifetime=5, forward=1.0, up=1.1)],
        "counter": [L("sparks", "core", count=4, size=0.07, speed=0.5, lifetime=5, forward=1.0, up=1.1)],
    }
    write("vfx_profile", "rin", {"palette": {"primary": "#2E3A2A", "secondary": "#121612", "accent": "#D98AB8",
                                             "core": "#F9F2F8"}, "material": "suit", "effects": effects})
    write("aura", "rin_ember", {"color": "#D98AB8", "secondary": "#F9F2F8", "style": "lightning", "min_release": 1,
                                "size": 0.85,
                                "bolts": {"color": "#F0B8DC", "core": "#FFFFFF", "rate": 4, "length": 0.25,
                                          "branches": 0.3, "crawl": 0.5, "width": 0.045}})
    write("sound_profile", "rin", {"techniques": {
        "focused_shot": {"release": "kn8:weapon.rifle_shot"},
        "thorough_sweep_fire": {"release": "kn8:weapon.rifle_shot"},
        "incinerating_barrage": {"release": "kn8:technique.narumi_blast"},
        "circular_barrage": {"windup": "kn8:technique.charge", "release": "kn8:weapon.rifle_shot"},
        "baton_strike": {"release": "kn8:weapon.fist_swing"}}})


def iharu():
    effects = {
        "single_minded_shot": muzzle("accent"),
        "single_minded_shot_impact": [L("sparks", "accent", count=4, size=0.07, speed=0.4, lifetime=5, forward=0,
                                        up=0)],
        "thunderbolt_blast": muzzle("primary") + [L("bolt", "primary", "burst", 2, 0.06, 0.0, 0.8, 5, 0.9, 1.35)],
        "thunderbolt_blast_trail": [L("bolt", "primary", "burst", 1, 0.05, 0.0, 0.5, 3, 0, 0)],
        "thunderbolt_blast_impact": [L("bolt", "primary", "burst", 4, 0.06, 0.0, 1.0, 6, 0, 0),
                                     L("flash", "core", size=0.35, spread=0.35, lifetime=2, forward=0, up=0)],
        "conduct_round_sweep": muzzle("primary"),
        "conduct_round_sweep_impact": [L("bolt", "primary", "burst", 3, 0.05, 0.0, 1.4, 6, 0, 0)],
        "rapid_thunderbolt_charge": [L("bolt", "primary", "converge", 4, 0.07, 0.0, 1.0, 8, 0.6, 1.3),
                                     L("bolt", "primary", "converge", 4, 0.07, 0.0, 1.0, 8, 0.6, 1.3, delay=7)],
        "rapid_thunderbolt": muzzle("primary") + [L("pulse", "primary", size=0.8, spread=0.8, lifetime=5,
                                                    forward=1.0, up=1.35)],
        "rapid_thunderbolt_trail": [L("bolt", "primary", "burst", 2, 0.06, 0.0, 0.7, 4, 0, 0)],
        "rapid_thunderbolt_impact": [L("flash", "core", size=0.6, spread=0.6, lifetime=3, forward=0, up=0),
                                     L("bolt", "primary", "burst", 6, 0.07, 0.0, 1.8, 8, 0, 0),
                                     L("wave", "primary", "ring", 1, 0.2, 0.0, 2.0, 8, 0, 0.05)],
        # Flash Adapter (canon): pico de potencia; o corpo dispara para a frente com descarga.
        "flash_adapter": [L("pulse", "core", size=0.9, spread=0.9, lifetime=5, forward=0.2, up=1.0),
                          L("bolt", "primary", "burst", 6, 0.07, 0.0, 1.4, 6, 0.4, 1.0),
                          L("sparks", "accent", count=5, size=0.08, speed=0.6, lifetime=6, forward=1.2, up=1.1,
                            delay=3)],
        "counter": [L("bolt", "primary", "burst", 3, 0.06, 0.0, 1.0, 5, 1.0, 1.1)],
    }
    write("vfx_profile", "iharu", {"palette": {"primary": "#F07824", "secondary": "#141414", "accent": "#FFD45C",
                                               "core": "#FFFFFF"}, "material": "energy", "effects": effects})
    write("aura", "iharu_spark", {"color": "#F07824", "secondary": "#FFD45C", "style": "lightning", "min_release": 1,
                                  "size": 0.9,
                                  "bolts": {"color": "#FFB04A", "core": "#FFFFFF", "rate": 7, "length": 0.32,
                                            "branches": 0.6, "crawl": 0.5, "width": 0.05}})
    write("sound_profile", "iharu", {"techniques": {
        "single_minded_shot": {"release": "kn8:weapon.rifle_shot"},
        "thunderbolt_blast": {"release": "kn8:technique.reno_shot"},
        "conduct_round_sweep": {"release": "kn8:weapon.rifle_shot"},
        "rapid_thunderbolt": {"windup": "kn8:technique.charge", "release": "kn8:technique.isao_thunder"},
        "flash_adapter": {"release": "kn8:technique.isao_punch"}}})


def haruichi():
    effects = {
        "personal_style_shot": muzzle("primary"),
        "personal_style_shot_impact": [L("sparks", "primary", count=4, size=0.06, speed=0.4, lifetime=5, forward=0,
                                         up=0)],
        "precision_snipe_charge": [L("beam", "primary", "line", 1, 0.03, 0.0, 6.0, 16, 0.9, 1.35)],
        "precision_snipe": muzzle("primary") + [L("pulse", "primary", size=0.5, spread=0.5, lifetime=4, forward=1.0,
                                                  up=1.35)],
        "precision_snipe_trail": [L("beam", "primary", "line", 1, 0.04, 0.0, 1.2, 3, 0, 0)],
        "precision_snipe_impact": [L("flash", "core", size=0.4, spread=0.4, lifetime=2, forward=0, up=0),
                                   L("sparks", "primary", count=6, size=0.07, speed=0.6, lifetime=6, forward=0,
                                     up=0)],
        "clearance": muzzle("primary") + [L("sparks", "primary", "arc", 6, 0.06, 0.6, 1.8, 4, 0.9, 1.35)],
        "clearance_impact": [L("sparks", "primary", count=3, size=0.06, speed=0.4, lifetime=4, forward=0, up=0)],
        # Postura de supressao: firma e coordena os aliados (anel no chao) antes do tiro.
        "suppressive_stance": muzzle("primary") + [L("wave", "primary", "ring", 1, 0.2, 0.0, 3.5, 14, 0, 0.05)],
        "suppressive_stance_impact": [L("sparks", "primary", count=3, size=0.06, speed=0.4, lifetime=4, forward=0,
                                        up=0)],
        # Salva de flechas envenenadas (arco AW-0027): riscos verdes e nuvem venenosa no impacto.
        "manifold_arrow_salvo_charge": [L("pulse", "accent", size=0.6, spread=0.6, lifetime=8, forward=0.8, up=1.4)],
        "manifold_arrow_salvo": [L("sparks", "accent", "arc", 9, 0.06, 0.8, 1.8, 5, 0.9, 1.4)],
        "manifold_arrow_salvo_trail": [L("beam", "accent", "line", 1, 0.035, 0.0, 0.9, 3, 0, 0)],
        "manifold_arrow_salvo_impact": [L("smoke", "accent", size=0.3, speed=0.02, spread=0.4, lifetime=24, forward=0,
                                          up=0.1),
                                        L("sparks", "accent", count=3, size=0.06, speed=0.3, lifetime=5, forward=0,
                                          up=0)],
        "counter": [L("sparks", "core", count=4, size=0.07, speed=0.5, lifetime=5, forward=1.0, up=1.1)],
    }
    write("vfx_profile", "haruichi", {"palette": {"primary": "#3FBFA8", "secondary": "#121614", "accent": "#7FD34E",
                                                  "core": "#E8FFF8"}, "material": "suit", "effects": effects})
    write("aura", "haruichi_teal", {"color": "#3FBFA8", "secondary": "#E8FFF8", "style": "lightning",
                                    "min_release": 1, "size": 0.85,
                                    "bolts": {"color": "#6FDCC6", "core": "#FFFFFF", "rate": 4, "length": 0.25,
                                              "branches": 0.0, "crawl": 0.6, "width": 0.04}})
    write("sound_profile", "haruichi", {"techniques": {
        "personal_style_shot": {"release": "kn8:weapon.rifle_shot"},
        "precision_snipe": {"windup": "kn8:technique.charge", "release": "kn8:technique.reno_shot"},
        "clearance": {"release": "kn8:weapon.rifle_shot"},
        "suppressive_stance": {"release": "kn8:technique.reno_shot"},
        "manifold_arrow_salvo": {"windup": "kn8:technique.charge", "release": "kn8:technique.narumi_shot"}}})


if __name__ == "__main__":
    soichiro()
    jura()
    rin()
    iharu()
    haruichi()
    print("ESPECIAIS-1: auras, perfis visuais e de som gravados")
