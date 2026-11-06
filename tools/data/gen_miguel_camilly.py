#!/usr/bin/env python3
"""MIGUEL-CAMILLY (etapa 3 do plano de 2026-11-05): os personagens originais do Miguel como soldados especiais.

Miguel: foice-armamento (corta em arco e dispara pelo eixo), vermelho. Camilly: rifle com lamina lateral, atiradora
com baioneta; arma e tecnicas no tema rosa (Miguel: "arma da Camilly em tons de rosa e tecnicas renomeadas no tema
rosa"). Kits do dossie do Miguel ([PROPOSTA ORIGINAL]); numeros [SUPOSICAO].

Uso: python3 tools/data/gen_miguel_camilly.py
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from gen_especiais1 import L, muzzle, write  # noqa: E402
from gen_especiais2 import DATA, TAGS, profile, shot  # noqa: E402


def miguel():
    red = "#D0202A"
    tech = {
        # Corte Orbital Rubro: varrida de quase 180 graus abaixando o corpo.
        "crimson_orbital_cut": {"type": "combo", "hits": [1.6], "windup_ticks": 8, "duration_ticks": 18,
                                "cooldown_ticks": 70, "max_range": 4.6, "priority": 2, "final_knockback": 0.8},
        # Disparo pelo Eixo da Foice.
        "scythe_axis_shot": shot(1.8, 8, 16, 60, 1, red, max_range=30.0, min_range=4.5),
        # Gancho do Ceifador: prende e puxa para fora do equilibrio (o golpe segura: Lentidao forte curta).
        "reapers_hook": {"type": "combo", "hits": [1.2], "windup_ticks": 6, "duration_ticks": 16,
                         "cooldown_ticks": 140, "max_range": 4.8, "priority": 2, "dash_in": 0.8},
        # Arco Ascendente: de baixo para cima, lanca.
        "rising_arc": {"type": "combo", "hits": [1.8], "windup_ticks": 8, "duration_ticks": 18,
                       "cooldown_ticks": 110, "max_range": 4.4, "priority": 2, "final_knockback": 1.2},
        # Tiro Perfurante de Nucleo: carga longa, deixa o alvo vulneravel.
        "core_piercing_shot": shot(3.0, 18, 28, 220, 3, red, max_range=36.0, min_range=5.0, width=0.45,
                                   vulnerable=1.15),
        # Danca de Dupla Ceifa: corte, meia-volta e tiro.
        "double_reap_dance": {"type": "combo", "hits": [1.0, 1.1, 1.3], "windup_ticks": 6, "duration_ticks": 26,
                              "cooldown_ticks": 150, "max_range": 4.6, "priority": 3},
        # Chuva de Estilhacos Rubros: municao de fragmentacao que cai numa area.
        "crimson_shard_rain": shot(0.8, 12, 22, 240, 2, red, max_range=26.0, min_range=6.0, count=6, spread=30.0,
                                   explosion_radius=1.5),
        # Ultimate: Eclipse do Ceifador (giro e disparo perfurante em linha).
        "reaper_eclipse": shot(4.2, 26, 40, 420, 4, red, max_range=34.0, min_range=3.0, width=0.9,
                               vulnerable=1.2),
    }
    tactics = {"aggression": 0.8, "preferred_distance": 4.0, "lateral_step": True, "damage_tolerance": 0.2,
               "safety_distance": 9.0, "heavy_vs_big": 1, "dodge_chance": 0.7, "exploit_openings": 2}
    data = profile(560, 20, 0.34, 40, 85, 1.05, "kn8:miguel_crimson", "kn8:miguel_scythe", 1.8, tech, tactics,
                   ranged=False,
                   heavy={"techniques": ["reaper_eclipse", "core_piercing_shot"], "power": 3, "radius": 2.0},
                   heavy_guard={"factor": 0.5, "front_arc_degrees": 120.0, "cooldown_ticks": 40})
    write("special_soldier", "miguel", data)
    cut = lambda tilt, size=1.9: L("band", "secondary", "arc", 1, 0.6, 0.0, size, 9, 1.0, 1.0, tilt=tilt,
                                   edge="accent")
    effects = {
        "crimson_orbital_cut": [cut(-8, 2.6), L("sparks", "accent", count=5, size=0.08, speed=0.6, lifetime=6,
                                                 forward=1.4, up=0.8)],
        "scythe_axis_shot": muzzle("accent"),
        "scythe_axis_shot_impact": [L("sparks", "accent", count=5, size=0.07, speed=0.5, lifetime=5, forward=0,
                                      up=0)],
        "reapers_hook": [cut(60, 1.6), L("pulse", "accent", size=0.6, spread=0.6, lifetime=5, forward=1.6, up=1.1)],
        "rising_arc": [cut(90, 2.2), L("wave", "accent", "ring", 1, 0.2, 0.0, 1.8, 8, 1.2, 0.05)],
        "core_piercing_shot_charge": [L("bolt", "accent", "converge", 4, 0.07, 0.0, 1.0, 12, 0.9, 1.3),
                                      L("beam", "accent", "line", 1, 0.03, 0.0, 6.0, 18, 0.9, 1.3)],
        "core_piercing_shot": muzzle("accent") + [L("pulse", "accent", size=0.8, spread=0.8, lifetime=5,
                                                    forward=1.0, up=1.3)],
        "core_piercing_shot_trail": [L("beam", "accent", "line", 1, 0.05, 0.0, 1.4, 3, 0, 0)],
        "core_piercing_shot_impact": [L("flash", "core", size=0.5, spread=0.5, lifetime=2, forward=0, up=0),
                                      L("sparks", "accent", count=7, size=0.08, speed=0.6, lifetime=6, forward=0,
                                        up=0)],
        "double_reap_dance": [cut(-20)],
        "double_reap_dance/1": [cut(-20)],
        "double_reap_dance/2": [cut(160)],
        "double_reap_dance/3": muzzle("accent"),
        "crimson_shard_rain": muzzle("accent") + [L("sparks", "accent", "arc", 6, 0.06, 0.6, 1.6, 5, 0.9, 1.6)],
        "crimson_shard_rain_trail": [L("glow", "accent", "burst", 1, 0.12, 0.0, 0.2, 4, 0, 0)],
        "crimson_shard_rain_explosion": [L("wave", "accent", "ring", 1, 0.25, 0.0, 1.8, 10, 0, 0.05),
                                         L("shard", "accent", "burst", 6, 0.2, 0.3, 1.0, 10, 0, 0.2)],
        "crimson_shard_rain_impact": [L("sparks", "accent", count=3, size=0.06, speed=0.4, lifetime=4, forward=0,
                                        up=0)],
        "reaper_eclipse_charge": [cut(0, 3.0), L("band", "secondary", "arc", 1, 0.6, 0.0, 3.0, 9, 1.0, 1.0,
                                                 tilt=180, edge="accent", delay=6),
                                  L("crack", "secondary", "line", 3, 0.2, 0.0, 2.0, 30, 1.2, 0.02, edge="accent",
                                    delay=10),
                                  L("bolt", "accent", "converge", 5, 0.08, 0.0, 1.2, 12, 0.9, 1.2, delay=12)],
        "reaper_eclipse": muzzle("accent") + [L("flash", "core", size=1.0, spread=1.0, lifetime=3, forward=1.2,
                                                up=1.2),
                                              L("pulse", "accent", size=1.4, spread=1.4, lifetime=6, forward=1.4,
                                                up=1.2)],
        "reaper_eclipse_trail": [L("beam", "accent", "line", 1, 0.12, 0.0, 2.0, 4, 0, 0),
                                 L("band", "secondary", "arc", 1, 0.4, 0.0, 1.0, 4, 0, 0, edge="accent")],
        "reaper_eclipse_impact": [L("flash", "core", size=0.9, spread=0.9, lifetime=3, forward=0, up=0),
                                  L("wave", "accent", "ring", 1, 0.3, 0.0, 2.6, 10, 0, 0.05),
                                  L("sparks", "accent", count=8, size=0.09, speed=0.7, lifetime=7, forward=0, up=0)],
        "counter": [cut(30, 1.8)],
    }
    write("vfx_profile", "miguel", {"palette": {"primary": "#1A1A1C", "secondary": "#0E0D0F", "accent": red,
                                                "core": "#FFE6E6"}, "material": "suit", "effects": effects})
    write("aura", "miguel_crimson", {"color": red, "secondary": "#FFE6E6", "style": "lightning", "min_release": 1,
                                     "size": 1.0,
                                     "bolts": {"color": "#E83A40", "core": "#FFFFFF", "rate": 5, "length": 0.3,
                                               "branches": 0.5, "crawl": 0.5, "width": 0.05}})
    write("sound_profile", "miguel", {"techniques": {
        "crimson_orbital_cut": {"release": "kn8:weapon.axe_swing"},
        "scythe_axis_shot": {"release": "kn8:weapon.rifle_shot"},
        "reapers_hook": {"release": "kn8:weapon.sword_heavy"},
        "rising_arc": {"release": "kn8:weapon.axe_heavy"},
        "core_piercing_shot": {"windup": "kn8:technique.charge", "release": "kn8:technique.reno_shot"},
        "double_reap_dance": {"release": "kn8:technique.hoshina_slash"},
        "crimson_shard_rain": {"release": "kn8:technique.narumi_blast"},
        "reaper_eclipse": {"windup": "kn8:technique.charge", "release": "kn8:technique.isao_thunder"}}})


def camilly():
    pink, light = "#FF6FB0", "#FFB8DA"
    tech = {
        # Tiro de Precisao Rosa (era Carmesim).
        "rose_precision_shot": shot(1.6, 8, 16, 50, 1, pink, max_range=40.0),
        # Rajada Petala (rajada de supressao): cone estreito que freia o avanco.
        "petal_burst": shot(0.6, 6, 18, 110, 2, light, max_range=26.0, count=5, spread=10.0, slow_ticks=30,
                            slow_level=1),
        # Corte Lateral de Baioneta (lamina rosa).
        "rose_side_cut": {"type": "combo", "hits": [1.3], "windup_ticks": 4, "duration_ticks": 12,
                          "cooldown_ticks": 60, "max_range": 3.6, "priority": 2},
        # Investida de Baioneta Rosa.
        "rose_bayonet_charge": {"type": "combo", "hits": [1.7], "windup_ticks": 8, "duration_ticks": 18,
                                "cooldown_ticks": 130, "max_range": 5.0, "priority": 2, "dash_in": 1.4,
                                "final_knockback": 1.0},
        # Disparo Perfurante Rosa: carga no cano, quebra placa.
        "pink_piercing_shot": shot(2.8, 16, 26, 200, 3, pink, max_range=40.0, width=0.45, vulnerable=1.15),
        # Sequencia Cruzada Rosa: corte e tiro na abertura.
        "pink_cross_sequence": {"type": "combo", "hits": [1.0, 1.5], "windup_ticks": 5, "duration_ticks": 20,
                                "cooldown_ticks": 140, "max_range": 3.8, "priority": 3},
        # Fogo de Cobertura Rosa: protege os aliados (Resistencia).
        "rose_cover_fire": shot(0.6, 8, 22, 320, 2, light, max_range=28.0, count=6, spread=16.0),
        # Ultimate: Tempestade Rosa (era Tempestade Carmesim): salva em pontos diferentes da carapaca.
        "rose_storm": shot(1.1, 20, 34, 420, 4, pink, max_range=34.0, count=7, spread=14.0, width=0.4,
                           vulnerable=1.2),
    }
    tactics = {"damage_tolerance": 0.3, "safety_distance": 14.0, "dodge_chance": 0.75, "protect_ally": 12.0,
               "exploit_openings": 2}
    data = profile(500, 19, 0.36, 30, 70, 0.75, "kn8:camilly_rose", "kn8:camilly_rifle", 1.65, tech, tactics,
                   rally={"rose_cover_fire": {"radius": 10.0, "duration_ticks": 100, "resistance": 1}})
    # Guarda Reversa: aparar com a lamina lateral mais vezes.
    data["parry"]["chance"] = 0.3
    data["ranged"]["keep_distance"] = 14.0
    write("special_soldier", "camilly", data)
    cut = lambda tilt, size=1.5: L("band", "secondary", "arc", 1, 0.45, 0.0, size, 8, 1.0, 1.1, tilt=tilt,
                                   edge="accent")
    effects = {
        "rose_precision_shot": muzzle("accent"),
        "rose_precision_shot_impact": [L("sparks", "accent", count=5, size=0.07, speed=0.5, lifetime=5, forward=0,
                                         up=0)],
        "petal_burst": muzzle("primary") + [L("sparks", "primary", "arc", 5, 0.06, 0.6, 1.2, 4, 0.9, 1.35)],
        "petal_burst_impact": [L("sparks", "primary", count=3, size=0.06, speed=0.4, lifetime=4, forward=0, up=0)],
        "rose_side_cut": [cut(-40)],
        "rose_bayonet_charge": [L("beam", "accent", "line", 1, 0.08, 0.0, 2.0, 5, 1.0, 1.1),
                                L("pulse", "accent", size=0.7, spread=0.7, lifetime=5, forward=1.6, up=1.1)],
        "pink_piercing_shot_charge": [L("pulse", "accent", "converge", 3, 0.5, 0.0, 0.8, 10, 0.9, 1.35),
                                      L("bolt", "accent", "converge", 3, 0.06, 0.0, 0.8, 10, 0.9, 1.35, delay=6)],
        "pink_piercing_shot": muzzle("accent") + [L("pulse", "accent", size=0.8, spread=0.8, lifetime=5,
                                                    forward=1.0, up=1.35)],
        "pink_piercing_shot_trail": [L("beam", "accent", "line", 1, 0.05, 0.0, 1.4, 3, 0, 0)],
        "pink_piercing_shot_impact": [L("flash", "core", size=0.45, spread=0.45, lifetime=2, forward=0, up=0),
                                      L("sparks", "accent", count=6, size=0.07, speed=0.6, lifetime=6, forward=0,
                                        up=0)],
        "pink_cross_sequence": [cut(-40)],
        "pink_cross_sequence/1": [cut(-40)],
        "pink_cross_sequence/2": muzzle("accent") + [cut(40, 1.2)],
        "rose_cover_fire": muzzle("primary") + [L("wave", "accent", "ring", 1, 0.2, 0.0, 3.5, 14, 0, 0.05)],
        "rose_cover_fire_impact": [L("sparks", "primary", count=3, size=0.06, speed=0.4, lifetime=4, forward=0,
                                     up=0)],
        "rose_storm_charge": [L("beam", "accent", "line", 1, 0.03, 0.0, 6.0, 20, 0.9, 1.35),
                              L("pulse", "accent", "converge", 3, 0.6, 0.0, 1.0, 12, 0.9, 1.35, delay=8)],
        "rose_storm": muzzle("accent") + [L("sparks", "accent", "arc", 7, 0.07, 0.7, 1.4, 5, 0.9, 1.35)],
        "rose_storm_trail": [L("beam", "accent", "line", 1, 0.04, 0.0, 1.0, 3, 0, 0)],
        "rose_storm_impact": [L("flash", "core", size=0.4, spread=0.4, lifetime=2, forward=0, up=0),
                              L("sparks", "accent", count=4, size=0.07, speed=0.5, lifetime=5, forward=0, up=0)],
        "counter": [cut(30, 1.4)],
    }
    write("vfx_profile", "camilly", {"palette": {"primary": light, "secondary": "#1C1418", "accent": pink,
                                                 "core": "#FFF0F7"}, "material": "suit", "effects": effects})
    write("aura", "camilly_rose", {"color": pink, "secondary": "#FFF0F7", "style": "lightning", "min_release": 1,
                                   "size": 0.9,
                                   "bolts": {"color": light, "core": "#FFFFFF", "rate": 5, "length": 0.27,
                                             "branches": 0.4, "crawl": 0.5, "width": 0.045}})
    write("sound_profile", "camilly", {"techniques": {
        "rose_precision_shot": {"release": "kn8:weapon.rifle_shot"},
        "petal_burst": {"release": "kn8:weapon.rifle_shot"},
        "rose_side_cut": {"release": "kn8:weapon.sword_swing"},
        "rose_bayonet_charge": {"release": "kn8:weapon.dash_cut"},
        "pink_piercing_shot": {"windup": "kn8:technique.charge", "release": "kn8:technique.reno_shot"},
        "pink_cross_sequence": {"release": "kn8:technique.hoshina_slash"},
        "rose_cover_fire": {"release": "kn8:weapon.rifle_shot"},
        "rose_storm": {"windup": "kn8:technique.charge", "release": "kn8:technique.narumi_shot"}}})


def weapons():
    scythe = {"item": "kn8:miguel_scythe", "base_damage": 12, "reach": 4.2, "style": "heavy", "profile": "kn8:sword",
              "actions": {"light": {"multiplier": 1.0, "duration_ticks": 16, "impact_tick": 7},
                          "heavy": {"multiplier": 2.4, "duration_ticks": 30, "impact_tick": 14}},
              "combo": [1.0, 1.1, 1.25],
              "sounds": {"swing": "kn8:weapon.axe_swing", "heavy": "kn8:weapon.axe_heavy",
                         "hit": "kn8:weapon.sword_hit"},
              "special": {"id": "reaper_eclipse", "type": "ground_slam", "multiplier": 2.6, "duration_ticks": 28,
                          "impact_tick": 14, "radius": 4.0, "forward": 1.5, "stamina_cost": 35, "heat_cost": 6,
                          "cooldown_ticks": 240, "knockback": 1.2, "stagger_ticks": 25, "camera_shake": 0.4,
                          "vfx": [{"effect": "kn8:technique", "intensity": 1.0}],
                          "sound": "kn8:technique.isao_thunder"}}
    rifle = {"item": "kn8:camilly_rifle", "base_damage": 6, "reach": 48.0, "style": "firearm",
             "profile": "kn8:rifle",
             "actions": {"light": {"multiplier": 1.0, "duration_ticks": 8, "impact_tick": 1},
                         "heavy": {"multiplier": 2.3, "duration_ticks": 24, "impact_tick": 6}},
             "sounds": {"shot": "kn8:weapon.rifle_shot"}}
    write("weapon", "miguel_scythe", scythe)
    write("weapon", "camilly_rifle", rifle)
    bench = {"miguel_scythe": (21, [("minecraft:iron_ingot", 10), ("kn8:muscle_fiber", 6),
                                    ("kn8:core_fragment", 4), ("kn8:intact_core", 1)]),
             "camilly_rifle": (22, [("minecraft:iron_ingot", 8), ("kn8:muscle_fiber", 5), ("kn8:core_fragment", 3),
                                    ("minecraft:pink_dye", 2)])}
    for name, (order, items) in bench.items():
        write("workbench", name, {"result": f"kn8:{name}", "category": "weapon", "order": order,
                                  "ingredients": [{"item": i, "count": n} for i, n in items]})
    path = DATA / "rank/capitao.json"
    raw = path.read_bytes().decode("utf-8")
    newline = "\r\n" if "\r\n" in raw else "\n"
    data = json.loads(raw)
    for item in ("kn8:miguel_scythe", "kn8:camilly_rifle"):
        if item not in data["unlocks"]:
            data["unlocks"].append(item)
    path.write_bytes((json.dumps(data, indent=2, ensure_ascii=False) + "\n").replace("\n", newline).encode("utf-8"))
    tag = TAGS / "item/real_size_weapons.json"
    raw = tag.read_bytes().decode("utf-8")
    newline = "\r\n" if "\r\n" in raw else "\n"
    data = json.loads(raw)
    for item in ("kn8:miguel_scythe", "kn8:camilly_rifle"):
        if item not in data["values"]:
            data["values"].append(item)
    tag.write_bytes((json.dumps(data, indent=2) + "\n").replace("\n", newline).encode("utf-8"))


if __name__ == "__main__":
    miguel()
    camilly()
    weapons()
    print("MIGUEL-CAMILLY: perfis, auras, efeitos, sons e armas gravados")
