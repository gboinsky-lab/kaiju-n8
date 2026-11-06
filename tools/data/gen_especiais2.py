#!/usr/bin/env python3
"""ESPECIAIS-2 (modelos do Miguel de 2026-11-05): Aoi Kaguragi, Hakua Igarashi, Kota Tachibana, Tae Nakanoshima e
Ryo Ikaruga como soldados especiais, Konomi Okonogi como NPC de pesquisa (Miguel: "a unica que nao entra como
combatente e a Konomi, que seria um npc para melhorias, pesquisas e etc") e as armas novas (espingarda do Iharu e do
Kota, arco AW-0027 do Haruichi e Pile Driver PB-3115 do Aoi).

Tecnicas pelo dossie do Miguel (THE GAME, marcado [THE GAME]; o canon so da o Pile Driver do Aoi): numeros da tabela
v1.2 (vida, armadura, velocidade) e o resto [SUPOSICAO]. Efeitos so desenhados, crescendo com o corpo (VFX-2).

Uso: python3 tools/data/gen_especiais2.py   (depois: gen_especiais1.py nao precisa rodar de novo)
"""
import copy
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / "src/main/resources/data/kn8/kn8"
TAGS = ROOT / "src/main/resources/data/kn8/tags"

import sys
sys.path.insert(0, str(Path(__file__).parent))
from gen_especiais1 import L, muzzle, write  # noqa: E402


def rgb(hexcolor):
    return int(hexcolor.lstrip("#"), 16)


BASE = {
    "follow_range": 48,
    "knockback_resistance": 0.5,
    "dash": {"cooldown_ticks": 100, "speed": 1.0, "invulnerable_ticks": 6, "react_ticks": 5,
             "gap_close_distance": 40.0},
    "counter": {"multiplier": 2.2, "cooldown_ticks": 200, "dash_speed": 0.9, "invulnerable_ticks": 10,
                "strike_delay_ticks": 8, "react_ticks": 6},
    "parry": {"chance": 0.15, "damage_factor": 0.4, "cooldown_ticks": 25, "counter_window_ticks": 10},
    "escalation": {"points_per_second": 2.0, "max_points": 40, "decay_per_second": 2.0,
                   "cooldown_reduction_at_max": 0.3},
}
RANGED = {"keep_distance": 16.0, "retreat_distance": 8.0, "orbit": True, "orbit_switch_ticks": 80,
          "retreat_speed": 1.3, "orbit_speed": 1.15, "chase_speed": 1.3, "cover_health": 0.5}


def shot(mult, windup, duration, cooldown, priority, color, max_range=34.0, min_range=4.0, count=1, spread=0.0,
         width=0.35, **extra):
    slash = {"count": count, "speed": 3.5, "range": max_range + 4.0, "width": width, "color": rgb(color),
             "bullet": True}
    if spread:
        slash["spread_degrees"] = spread
    slash.update(extra)
    return {"type": "slash", "hits": [mult], "windup_ticks": windup, "duration_ticks": duration,
            "cooldown_ticks": cooldown, "min_range": min_range, "max_range": max_range, "priority": priority,
            "slash": slash}


def profile(health, armor, speed, release, max_release, damage, aura, weapon, height, techniques, tactics,
            ranged=True, **extra):
    data = {"health": health, "armor": armor, "speed": speed}
    data.update(copy.deepcopy(BASE))
    if ranged:
        data["ranged"] = dict(RANGED)
    else:
        data["dash"]["gap_close_distance"] = 8.0
    data.update({"release": release, "max_release": max_release, "kaiju_damage": damage, "aura": aura,
                 "weapon": weapon, "height": height, "techniques": techniques, "tactics": tactics})
    data.update(extra)
    return data


# --- Aoi Kaguragi: Pile Driver PB-3115 (canon), golpes pesados de perto, Postura inabalavel protege o esquadrao.
def aoi():
    tech = {
        "steadfast_swing": {"type": "combo", "hits": [1.4], "windup_ticks": 8, "duration_ticks": 18,
                            "cooldown_ticks": 60, "max_range": 4.0, "priority": 1, "dash_in": 0.6},
        "skillful_bash": {"type": "combo", "hits": [0.9, 1.1], "windup_ticks": 4, "duration_ticks": 14,
                          "cooldown_ticks": 90, "max_range": 3.8, "priority": 2, "final_knockback": 0.6},
        "stalwart_stance": {"type": "combo", "hits": [1.0], "windup_ticks": 10, "duration_ticks": 22,
                            "cooldown_ticks": 360, "max_range": 4.0, "priority": 2},
        "fortified_crushing_blow": {"type": "combo", "hits": [2.6], "windup_ticks": 16, "duration_ticks": 30,
                                    "cooldown_ticks": 220, "max_range": 4.2, "priority": 3, "honju_priority": 1,
                                    "final_knockback": 1.0},
        # Canon: estaca de perfuracao contra partes resistentes (preparo longo, um golpe enorme).
        "pile_driver": {"type": "combo", "hits": [3.4], "windup_ticks": 22, "duration_ticks": 36,
                        "cooldown_ticks": 300, "max_range": 4.2, "priority": 4, "honju_priority": 2,
                        "dash_in": 1.0, "final_knockback": 1.6},
    }
    tactics = {"aggression": 0.85, "preferred_distance": 3.2, "lateral_step": False, "damage_tolerance": 0.15,
               "safety_distance": 8.0, "heavy_vs_big": 1, "dodge_chance": 0.5, "protect_ally": 10.0,
               "exploit_openings": 2}
    data = profile(330, 16, 0.28, 15, 35, 0.75, "kn8:aoi_steel", "kn8:aoi_pile_bunker", 1.83, tech, tactics,
                   ranged=False,
                   heavy={"techniques": ["pile_driver", "fortified_crushing_blow"], "power": 3, "radius": 2.0},
                   heavy_guard={"factor": 0.5, "front_arc_degrees": 120.0, "cooldown_ticks": 40},
                   rally={"stalwart_stance": {"radius": 10.0, "duration_ticks": 160, "resistance": 2}})
    write("special_soldier", "aoi", data)
    hit = lambda size=1.0, delay=0: [L("pulse", "accent", size=0.6 * size, spread=0.6 * size, lifetime=5,
                                       forward=1.2, up=1.1, delay=delay),
                                     L("sparks", "core", count=5, size=0.08, speed=0.6, lifetime=6, forward=1.2,
                                       up=1.1, delay=delay)]
    effects = {
        "steadfast_swing": hit(),
        "skillful_bash": hit(0.8),
        "skillful_bash/2": hit(1.1),
        "stalwart_stance": [L("wave", "accent", "ring", 1, 0.3, 0.0, 4.0, 18, 0, 0.05),
                            L("pulse", "accent", "rise", 3, 1.0, 0.2, 1.6, 12, 0, 0.3)],
        "fortified_crushing_blow_charge": [L("bolt", "accent", "converge", 4, 0.07, 0.0, 1.0, 12, 0.6, 1.1)],
        "fortified_crushing_blow": hit(1.6) + [L("crack", "secondary", "line", 3, 0.2, 0.0, 2.2, 30, 1.2, 0.02,
                                                 edge="accent")],
        "pile_driver_charge": [L("pulse", "accent", "converge", 3, 0.7, 0.0, 1.0, 10, 0.8, 1.1),
                               L("bolt", "accent", "converge", 4, 0.07, 0.0, 1.0, 10, 0.8, 1.1, delay=10)],
        "pile_driver": [L("beam", "core", "line", 1, 0.12, 0.0, 2.4, 4, 1.0, 1.1),
                        L("flash", "core", size=0.9, spread=0.9, lifetime=3, forward=1.6, up=1.1),
                        L("pulse", "accent", size=1.4, spread=1.4, lifetime=6, forward=1.6, up=1.1),
                        L("pulse", "accent", size=1.0, spread=1.0, lifetime=6, forward=1.9, up=1.1, delay=2),
                        L("sparks", "core", count=8, size=0.09, speed=0.8, lifetime=7, forward=1.6, up=1.1)],
        "counter": hit(0.9),
    }
    write("vfx_profile", "aoi", {"palette": {"primary": "#22262C", "secondary": "#121418", "accent": "#7FA8D8",
                                             "core": "#EEF4FF"}, "material": "metal", "effects": effects})
    write("aura", "aoi_steel", {"color": "#7FA8D8", "secondary": "#EEF4FF", "style": "lightning", "min_release": 1,
                                "size": 0.95,
                                "bolts": {"color": "#9CC0EA", "core": "#FFFFFF", "rate": 4, "length": 0.26,
                                          "branches": 0.3, "crawl": 0.6, "width": 0.05}})
    write("sound_profile", "aoi", {"techniques": {
        "steadfast_swing": {"release": "kn8:weapon.axe_heavy"},
        "skillful_bash": {"release": "kn8:weapon.fist_swing"},
        "stalwart_stance": {"release": "kn8:technique.charge"},
        "fortified_crushing_blow": {"windup": "kn8:technique.charge", "release": "kn8:weapon.axe_special"},
        "pile_driver": {"windup": "kn8:technique.charge", "release": "kn8:technique.isao_thunder"}}})


# --- Hakua Igarashi: tiros de fogo e o Incentivo Direto que cura o esquadrao aos poucos.
def hakua():
    c, f = "#8ECBE8", "#C9D3DD"
    tech = {
        "sincere_shot": shot(1.4, 6, 14, 45, 1, c),
        "direct_encouragement": shot(1.0, 10, 20, 360, 2, c),
        "determination_barrage": shot(0.7, 10, 22, 200, 3, f, count=5, spread=18.0),
        "advanced_pursuit": shot(1.8, 8, 16, 120, 2, c, max_range=40.0),
    }
    tactics = {"damage_tolerance": 0.3, "safety_distance": 16.0, "dodge_chance": 0.75, "melee_counter": False,
               "protect_ally": 12.0, "exploit_openings": 1}
    data = profile(300, 14, 0.31, 18, 36, 0.6, "kn8:hakua_glow", "kn8:rifle", 1.85, tech, tactics,
                   burn={"determination_barrage": 4},
                   rally={"direct_encouragement": {"radius": 12.0, "duration_ticks": 100, "regeneration": 2}})
    write("special_soldier", "hakua", data)
    effects = {
        "sincere_shot": muzzle("accent"),
        "sincere_shot_impact": [L("sparks", "accent", count=4, size=0.07, speed=0.4, lifetime=5, forward=0, up=0)],
        # Incentivo: aneis quentes subindo em volta e o tiro.
        "direct_encouragement": muzzle("accent") + [L("pulse", "accent", "rise", 3, 1.0, 0.15, 1.6, 14, 0, 0.3),
                                                     L("wave", "accent", "ring", 1, 0.2, 0.0, 3.5, 14, 0, 0.05)],
        "direct_encouragement_impact": [L("sparks", "accent", count=3, size=0.06, speed=0.4, lifetime=4, forward=0,
                                          up=0)],
        "determination_barrage": muzzle("primary", "#5A4A40"),
        "determination_barrage_trail": [L("wisp", "primary", "rise", 1, 0.2, 0.05, 0.2, 6, 0, 0)],
        "determination_barrage_impact": [L("flash", "primary", size=0.35, spread=0.35, lifetime=2, forward=0, up=0),
                                         L("wisp", "primary", "rise", 3, 0.3, 0.2, 0.6, 12, 0, 0)],
        "advanced_pursuit": muzzle("accent") + [L("sparks", "core", "line", 3, 0.06, 0.6, 1.4, 4, 0.9, 1.35)],
        "advanced_pursuit_impact": [L("sparks", "accent", count=5, size=0.07, speed=0.5, lifetime=5, forward=0,
                                      up=0)],
        "counter": [L("sparks", "core", count=4, size=0.07, speed=0.5, lifetime=5, forward=1.0, up=1.1)],
    }
    write("vfx_profile", "hakua", {"palette": {"primary": "#C9D3DD", "secondary": "#141212", "accent": "#8ECBE8",
                                               "core": "#F4FAFF"}, "material": "suit", "effects": effects})
    write("aura", "hakua_glow", {"color": "#8ECBE8", "secondary": "#F4FAFF", "style": "lightning", "min_release": 1,
                                 "size": 0.85,
                                 "bolts": {"color": "#B8E2F6", "core": "#FFFFFF", "rate": 4, "length": 0.25,
                                           "branches": 0.4, "crawl": 0.5, "width": 0.045}})
    write("sound_profile", "hakua", {"techniques": {
        "sincere_shot": {"release": "kn8:weapon.rifle_shot"},
        "direct_encouragement": {"windup": "kn8:technique.charge", "release": "kn8:weapon.rifle_shot"},
        "determination_barrage": {"release": "kn8:technique.narumi_blast"},
        "advanced_pursuit": {"release": "kn8:technique.reno_shot"}}})


# --- Kota Tachibana: espingarda na linha de frente, granada congelante [THE GAME] e protege quem esta perto.
def kota():
    c, ice = "#E8C060", "#C83A40"
    tech = {
        "steady_shot": shot(0.45, 6, 16, 45, 1, c, max_range=18.0, min_range=2.0, count=5, spread=12.0),
        "menacing_blow": shot(0.7, 10, 20, 140, 2, c, max_range=16.0, min_range=2.0, count=6, spread=14.0,
                              slow_ticks=40, slow_level=1),
        "freeze_grenade": shot(1.8, 12, 24, 260, 3, ice, max_range=22.0, min_range=4.0, width=0.6,
                               explosion_radius=2.5, slow_ticks=50, slow_level=4),
        "solid_shot": shot(0.55, 6, 16, 110, 2, c, max_range=16.0, min_range=2.0, count=5, spread=8.0),
    }
    tactics = {"aggression": 0.7, "damage_tolerance": 0.2, "safety_distance": 10.0, "dodge_chance": 0.6,
               "melee_counter": False, "protect_ally": 14.0, "exploit_openings": 1}
    data = profile(310, 15, 0.30, 16, 34, 0.65, "kn8:kota_frost", "kn8:shotgun", 1.76, tech, tactics)
    data["ranged"]["keep_distance"] = 9.0
    data["ranged"]["retreat_distance"] = 4.0
    write("special_soldier", "kota", data)
    effects = {
        "steady_shot": muzzle("accent") + [L("sparks", "accent", "arc", 5, 0.06, 0.6, 1.2, 4, 0.9, 1.35)],
        "steady_shot_impact": [L("sparks", "accent", count=3, size=0.06, speed=0.4, lifetime=4, forward=0, up=0)],
        "menacing_blow": muzzle("core") + [L("pulse", "accent", size=0.7, spread=0.7, lifetime=5, forward=1.0,
                                             up=1.35)],
        "menacing_blow_impact": [L("sparks", "core", count=4, size=0.07, speed=0.5, lifetime=5, forward=0, up=0)],
        "freeze_grenade": [L("pulse", "primary", size=0.5, spread=0.5, lifetime=5, forward=0.9, up=1.35)],
        "freeze_grenade_trail": [L("glow", "primary", "burst", 1, 0.15, 0.0, 0.2, 4, 0, 0)],
        "freeze_grenade_explosion": [L("wave", "primary", "ring", 1, 0.3, 0.0, 2.6, 14, 0, 0.05),
                                     L("shard", "primary", "burst", 8, 0.3, 0.3, 1.4, 14, 0, 0.3),
                                     L("smoke", "core", size=0.4, speed=0.03, spread=0.8, lifetime=22, forward=0,
                                       up=0.1)],
        "freeze_grenade_impact": [L("shard", "primary", "burst", 4, 0.25, 0.3, 0.8, 10, 0, 0)],
        "solid_shot": muzzle("accent"),
        "solid_shot_impact": [L("sparks", "accent", count=3, size=0.06, speed=0.4, lifetime=4, forward=0, up=0)],
        "counter": [L("sparks", "core", count=4, size=0.07, speed=0.5, lifetime=5, forward=1.0, up=1.1)],
    }
    write("vfx_profile", "kota", {"palette": {"primary": "#C83A40", "secondary": "#141618", "accent": "#E8C060",
                                              "core": "#E07832"}, "material": "suit", "effects": effects})
    write("aura", "kota_frost", {"color": "#C83A40", "secondary": "#E07832", "style": "lightning", "min_release": 1,
                                 "size": 0.9,
                                 "bolts": {"color": "#F09050", "core": "#FFFFFF", "rate": 4, "length": 0.26,
                                           "branches": 0.4, "crawl": 0.5, "width": 0.05}})
    write("sound_profile", "kota", {"techniques": {
        "steady_shot": {"release": "kn8:weapon.rifle_shot", "pitch": 0.8},
        "menacing_blow": {"release": "kn8:weapon.rifle_shot", "pitch": 0.7},
        "freeze_grenade": {"windup": "kn8:technique.charge", "release": "kn8:technique.reno_shot"},
        "solid_shot": {"release": "kn8:weapon.rifle_shot", "pitch": 0.8}}})


# --- Tae Nakanoshima: municao incendiaria e granada termica [THE GAME].
def tae():
    c, f = "#B7A0D8", "#8A77B7"
    tech = {
        "aggressive_shot": shot(1.4, 6, 14, 45, 1, f),
        "blazing_fire_shot": shot(1.3, 8, 18, 140, 2, c),
        "heat_grenade": shot(2.0, 12, 24, 240, 3, c, max_range=24.0, width=0.6, explosion_radius=2.5),
        "passionate_shot": shot(1.6, 6, 14, 110, 2, f),
    }
    tactics = {"damage_tolerance": 0.3, "safety_distance": 16.0, "dodge_chance": 0.7, "melee_counter": False,
               "exploit_openings": 1}
    data = profile(260, 12, 0.29, 15, 32, 0.6, "kn8:tae_blaze", "kn8:rifle", 1.66, tech, tactics,
                   burn={"blazing_fire_shot": 6, "heat_grenade": 5},
                   rally={"heat_grenade": {"duration_ticks": 120, "strength": 1}})
    write("special_soldier", "tae", data)
    effects = {
        "aggressive_shot": muzzle("accent"),
        "aggressive_shot_impact": [L("sparks", "accent", count=4, size=0.07, speed=0.4, lifetime=5, forward=0,
                                     up=0)],
        "blazing_fire_shot": muzzle("primary", "#5A4A40"),
        "blazing_fire_shot_trail": [L("wisp", "primary", "rise", 1, 0.22, 0.05, 0.2, 6, 0, 0)],
        "blazing_fire_shot_impact": [L("flash", "primary", size=0.4, spread=0.4, lifetime=2, forward=0, up=0),
                                     L("wisp", "primary", "rise", 4, 0.35, 0.25, 0.8, 14, 0, 0)],
        "heat_grenade": [L("pulse", "primary", size=0.5, spread=0.5, lifetime=5, forward=0.9, up=1.35)],
        "heat_grenade_trail": [L("wisp", "primary", "rise", 1, 0.2, 0.05, 0.2, 6, 0, 0)],
        "heat_grenade_explosion": [L("flash", "accent", size=0.8, spread=0.8, lifetime=3, forward=0, up=0),
                                   L("wave", "primary", "ring", 1, 0.3, 0.0, 2.6, 12, 0, 0.05),
                                   L("wisp", "primary", "rise", 6, 0.4, 0.3, 1.6, 16, 0, 0),
                                   L("smoke", "secondary", size=0.45, speed=0.05, spread=0.9, lifetime=22,
                                     forward=0, up=0.2)],
        "heat_grenade_impact": [L("wisp", "primary", "rise", 3, 0.3, 0.2, 0.6, 12, 0, 0)],
        "passionate_shot": muzzle("accent") + [L("sparks", "primary", "line", 3, 0.06, 0.6, 1.4, 4, 0.9, 1.35)],
        "passionate_shot_impact": [L("sparks", "primary", count=4, size=0.07, speed=0.5, lifetime=5, forward=0,
                                     up=0)],
        "counter": [L("sparks", "core", count=4, size=0.07, speed=0.5, lifetime=5, forward=1.0, up=1.1)],
    }
    write("vfx_profile", "tae", {"palette": {"primary": "#B7A0D8", "secondary": "#161212", "accent": "#8A77B7",
                                             "core": "#F8F4FF"}, "material": "suit", "effects": effects})
    write("aura", "tae_blaze", {"color": "#B7A0D8", "secondary": "#F8F4FF", "style": "lightning", "min_release": 1,
                                "size": 0.85,
                                "bolts": {"color": "#D6C8F0", "core": "#FFFFFF", "rate": 4, "length": 0.25,
                                          "branches": 0.5, "crawl": 0.5, "width": 0.045}})
    write("sound_profile", "tae", {"techniques": {
        "aggressive_shot": {"release": "kn8:weapon.rifle_shot"},
        "blazing_fire_shot": {"release": "kn8:technique.narumi_blast"},
        "heat_grenade": {"windup": "kn8:technique.charge", "release": "kn8:technique.narumi_blast"},
        "passionate_shot": {"release": "kn8:weapon.rifle_shot"}}})


# --- Ryo Ikaruga: comandante; a Ordem de disparo e o Zelo contagiante fortalecem quem esta perto.
def ryo():
    c = "#3F6FC0"
    tech = {
        "hot_blooded_shot": shot(1.4, 6, 14, 45, 1, c),
        "firing_command": shot(2.4, 12, 22, 160, 3, c, max_range=40.0, width=0.45),
        "contagious_zeal": shot(1.0, 10, 20, 400, 2, c),
        "passionate_shot": shot(1.6, 6, 14, 110, 2, c),
    }
    tactics = {"damage_tolerance": 0.3, "safety_distance": 18.0, "dodge_chance": 0.75, "melee_counter": False,
               "protect_ally": 12.0, "exploit_openings": 1}
    data = profile(310, 15, 0.31, 18, 36, 0.6, "kn8:ryo_command", "kn8:rifle", 1.73, tech, tactics,
                   rally={"firing_command": {"radius": 10.0, "duration_ticks": 100, "strength": 1},
                          "contagious_zeal": {"radius": 14.0, "duration_ticks": 200, "strength": 2, "speed": 1}})
    write("special_soldier", "ryo", data)
    effects = {
        "hot_blooded_shot": muzzle("accent"),
        "hot_blooded_shot_impact": [L("sparks", "accent", count=4, size=0.07, speed=0.4, lifetime=5, forward=0,
                                      up=0)],
        "firing_command_charge": [L("wave", "accent", "ring", 1, 0.2, 0.0, 3.0, 12, 0, 0.05)],
        "firing_command": muzzle("core") + [L("pulse", "accent", size=0.8, spread=0.8, lifetime=5, forward=1.0,
                                              up=1.35)],
        "firing_command_trail": [L("beam", "accent", "line", 1, 0.04, 0.0, 1.2, 3, 0, 0)],
        "firing_command_impact": [L("flash", "core", size=0.5, spread=0.5, lifetime=2, forward=0, up=0),
                                  L("sparks", "accent", count=6, size=0.07, speed=0.6, lifetime=6, forward=0, up=0)],
        # Zelo contagiante: ondas vermelhas saindo do Ryo para o esquadrao.
        "contagious_zeal": muzzle("accent") + [L("wave", "accent", "ring", 1, 0.3, 0.0, 5.0, 16, 0, 0.05),
                                               L("wave", "accent", "ring", 1, 0.2, 0.0, 3.5, 14, 0, 0.05, delay=5),
                                               L("pulse", "accent", "rise", 3, 1.0, 0.2, 1.6, 12, 0, 0.3)],
        "contagious_zeal_impact": [L("sparks", "accent", count=3, size=0.06, speed=0.4, lifetime=4, forward=0,
                                     up=0)],
        "passionate_shot": muzzle("accent"),
        "passionate_shot_impact": [L("sparks", "accent", count=4, size=0.07, speed=0.5, lifetime=5, forward=0,
                                     up=0)],
        "counter": [L("sparks", "core", count=4, size=0.07, speed=0.5, lifetime=5, forward=1.0, up=1.1)],
    }
    write("vfx_profile", "ryo", {"palette": {"primary": "#1C1E22", "secondary": "#101114", "accent": "#3F6FC0",
                                             "core": "#EAFBFF"}, "material": "suit", "effects": effects})
    write("aura", "ryo_command", {"color": "#3F6FC0", "secondary": "#EAFBFF", "style": "lightning", "min_release": 1,
                                  "size": 0.9,
                                  "bolts": {"color": "#50C9D8", "core": "#FFFFFF", "rate": 4, "length": 0.26,
                                            "branches": 0.3, "crawl": 0.5, "width": 0.045}})
    write("sound_profile", "ryo", {"techniques": {
        "hot_blooded_shot": {"release": "kn8:weapon.rifle_shot"},
        "firing_command": {"windup": "kn8:technique.charge", "release": "kn8:technique.reno_shot"},
        "contagious_zeal": {"windup": "kn8:technique.charge", "release": "kn8:weapon.rifle_shot"},
        "passionate_shot": {"release": "kn8:weapon.rifle_shot"}}})


# --- Konomi Okonogi: operadora; nao luta (Miguel). Perfil so para o modelo, a altura e a vida.
def konomi():
    data = profile(220, 10, 0.28, 1, 1, 0.0, "kn8:none", "kn8:isao_fist", 1.49, {}, {"melee_counter": False},
                   ranged=False)
    write("special_soldier", "konomi", data)
    write("vfx_profile", "konomi", {"palette": {"primary": "#EDEDED", "secondary": "#3A2A24", "accent": "#7FC8E8",
                                                "core": "#FFFFFF"}, "material": "suit", "effects": {}})
    write("sound_profile", "konomi", {"techniques": {}})


def weapons():
    shotgun = {"item": "kn8:shotgun", "base_damage": 9, "reach": 20.0, "style": "firearm", "profile": "kn8:rifle",
               "actions": {"light": {"multiplier": 1.0, "duration_ticks": 14, "impact_tick": 1},
                           "heavy": {"multiplier": 2.0, "duration_ticks": 26, "impact_tick": 6}},
               "sounds": {"shot": "kn8:weapon.rifle_shot"}}
    bow = {"item": "kn8:haruichi_bow", "base_damage": 7, "reach": 50.0, "style": "firearm", "profile": "kn8:rifle",
           "actions": {"light": {"multiplier": 1.0, "duration_ticks": 12, "impact_tick": 2},
                       "heavy": {"multiplier": 2.4, "duration_ticks": 28, "impact_tick": 8}},
           "sounds": {"shot": "kn8:technique.narumi_shot"}}
    pile = {"item": "kn8:aoi_pile_bunker", "base_damage": 12, "reach": 3.4, "style": "heavy",
            "profile": "kn8:sword",
            "actions": {"light": {"multiplier": 1.0, "duration_ticks": 18, "impact_tick": 8},
                        "heavy": {"multiplier": 2.4, "duration_ticks": 32, "impact_tick": 15}},
            "combo": [1.0, 1.15],
            "sounds": {"swing": "kn8:weapon.axe_swing", "heavy": "kn8:weapon.axe_heavy",
                       "hit": "kn8:weapon.axe_hit"},
            "special": {"id": "pile_driver", "type": "lunge", "multiplier": 3.0, "duration_ticks": 22,
                        "impact_tick": 10, "radius": 1.2, "forward": 3.0, "stamina_cost": 35, "heat_cost": 6,
                        "cooldown_ticks": 220, "knockback": 1.4, "stagger_ticks": 30, "camera_shake": 0.4,
                        "vfx": [{"effect": "kn8:technique", "intensity": 1.0}],
                        "sound": "kn8:technique.isao_thunder"}}
    for name, data in (("shotgun", shotgun), ("haruichi_bow", bow), ("aoi_pile_bunker", pile)):
        write("weapon", name, data)
    bench = {
        "shotgun": (18, [("minecraft:iron_ingot", 8), ("kn8:muscle_fiber", 3), ("minecraft:gunpowder", 4)]),
        "haruichi_bow": (19, [("minecraft:iron_ingot", 6), ("kn8:muscle_fiber", 6), ("kn8:core_fragment", 3),
                              ("minecraft:string", 4)]),
        "aoi_pile_bunker": (20, [("minecraft:iron_ingot", 12), ("kn8:muscle_fiber", 6), ("kn8:core_fragment", 4),
                                 ("kn8:intact_core", 1)]),
    }
    for name, (order, items) in bench.items():
        write("workbench", name, {"result": f"kn8:{name}", "category": "weapon", "order": order,
                                  "ingredients": [{"item": i, "count": n} for i, n in items]})
    # Patentes: espingarda de Oficial Senior; arco e Pile Driver de Vice-Capitao [SUPOSICAO].
    for rank, items in (("oficial_senior", ["kn8:shotgun"]),
                        ("vice_capitao", ["kn8:haruichi_bow", "kn8:aoi_pile_bunker"])):
        path = DATA / "rank" / f"{rank}.json"
        raw = path.read_bytes().decode("utf-8")
        newline = "\r\n" if "\r\n" in raw else "\n"
        data = json.loads(raw)
        for item in items:
            if item not in data["unlocks"]:
                data["unlocks"].append(item)
        text = json.dumps(data, indent=2, ensure_ascii=False) + "\n"
        path.write_bytes(text.replace("\n", newline).encode("utf-8"))
    tag = TAGS / "item/real_size_weapons.json"
    data = json.loads(tag.read_text(encoding="utf-8"))
    for item in ("kn8:haruichi_bow", "kn8:aoi_pile_bunker"):
        if item not in data["values"]:
            data["values"].append(item)
    tag.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    aoi()
    hakua()
    kota()
    tae()
    ryo()
    konomi()
    weapons()
    print("ESPECIAIS-2: perfis, auras, efeitos, sons e armas gravados")
