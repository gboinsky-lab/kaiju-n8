#!/usr/bin/env python3
"""NUMERADOS-B (etapa 4 do plano de 2026-11-05): Kaiju No. 2, No. 6 e No. 14 (modelos do Miguel, sem rig).

Vida, dano, armadura, velocidade e multiplicadores pelo "Balanceamento Completo dos Kaijus" v1.0 do Miguel
(docs/referencias_balanceamento/, 2026-11-05); alturas: No. 2 25 m e No. 6 22 m pela tabela mestra, No. 14 10 m
[SUPOSICAO]. Habilidades do dossie (CANON) so com os tipos que ja existem.

- No. 2 (rocha, cauda): soco 1,8x, esmagamento 2,5x, onda de choque 1,6x em area, descarga pela boca 3,2x,
  investida 1,7x; tanque com janela depois dos golpes fortes.
- No. 6 (rei dos kaiju, frio): garra 1,2x, sopro gelado em rajada, estaca de gelo 1,7x, avalanche 2,4x, campo
  congelante (Lentidao); territorial, congelamento curto (nunca permanente).
- No. 14 (monolito): raios pelas faces 1,5x, salva para os lados, pulso 1,8x, area 2,4x, teleporte curto (salto).
  Campo de forca de 35% fica [DECIDIR] (precisa de codigo novo); por ora armadura alta.

Uso: python3 tools/data/gen_numerados_b.py   (depois: rig_primigenius_mesh.py kaiju_no2|kaiju_no6 e
rig_monolith_mesh.py)
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from gen_numerados_a import (DATA, L, SMART, ai, area, charge, leap, melee, palette, projectile,  # noqa: E402
                             sweep, write)


def kaiju(health, damage, armor, speed, fortitude, scale, width, height, abilities, tags, parts, size):
    return {"class": "numbered", "fortitude": fortitude, "size": size,
            "dimensions": {"width": width, "height": height, "scale": scale},
            "overrides": {"health": health, "damage": damage, "armor": armor}, "speed": speed,
            "intelligence": 4, "parts": parts, "core": {"health_fraction": 0.25, "exposed_on": ["heavy_hit"]},
            "abilities": [f"kn8:{name}" for name in abilities], "weaknesses": [],
            "spawn": {"natural": False}, "rarity": "event", "tags": ["numbered"] + tags,
            "rage": {"health_below": 0.35, "damage_multiplier": 1.15, "speed_multiplier": 1.1,
                     "cooldown_multiplier": 0.75}}


def reptile_parts(height, width, head_z):
    return [
        {"name": "head", "offset": [0.0, height * 0.78, head_z], "width": width * 0.35, "height": height * 0.2,
         "multiplier": 1.2},
        {"name": "torso", "offset": [0.0, height * 0.3, 0.0], "width": width * 0.7, "height": height * 0.5,
         "multiplier": 1.0},
        {"name": "core", "offset": [0.0, height * 0.55, 0.6], "width": 1.0, "height": 1.0, "multiplier": 3.0,
         "core": True},
        {"name": "legs", "offset": [0.0, 0.0, 0.0], "width": width * 0.8, "height": height * 0.33,
         "multiplier": 0.8},
        {"name": "tail", "offset": [0.0, 0.4, -width * 0.8], "width": width * 0.5, "height": height * 0.3,
         "multiplier": 0.7}]


def no2():
    beam = "#E1C34D"
    moves = {
        "no2_punch": melee(1.8, 12, 70, "action.punch", 2, 4.0, heavy=True),
        "no2_crush": area(2.5, 4.5, 20, 160, "action.slam", 3, 4.0),
        "no2_shockwave": area(1.6, 9.0, 16, 200, "action.roar", 2, 8.0, sound="kn8:kaiju.roar"),
        "no2_mouth_discharge": projectile(3.2, 24, 280, "action.spine_shot", 4, 6, 34, beam, speed=2.2,
                                          heavy=True, particles="kn8:energy_charge"),
        "no2_rush": charge(1.7, 140, "action.charge", 3, 6, 26),
        "no2_tail": sweep(1.4, 5.0, 12, 90, "action.tail_swipe", 2, arc=140),
    }
    data = kaiju(2600.0, 58.0, 20.0, 0.28, 9.4, 25.0 / 9.0, 4.0, 9.0, moves, ["reptile", "giant", "tank"],
                 reptile_parts(9.0, 4.0, 1.1), "supergiant")
    vfx = palette("#77852B", "#2A2A20", beam, "#FFF6D0", "scales", {
        "no2_punch": [L("pulse", "accent", size=1.0, spread=1.0, lifetime=6, forward=1.4, up=1.0),
                      L("crack", "secondary", "line", 3, 0.2, 0.0, 1.8, 24, 1.4, 0.02, edge="accent")],
        "no2_crush": [L("wave", "accent", "ring", 1, 0.4, 0.0, 4.0, 14, 1.2, 0.05),
                      L("crack", "secondary", "line", 6, 0.3, 0.0, 3.5, 40, 1.2, 0.02, edge="accent"),
                      L("smoke", "primary", size=0.6, speed=0.05, spread=1.6, lifetime=28, forward=1.2, up=0.2)],
        "no2_shockwave": [L("wave", "accent", "ring", 1, 0.5, 0.0, 7.0, 18, 0, 0.05),
                          L("wave", "core", "ring", 1, 0.3, 0.0, 4.5, 14, 0, 0.05, delay=4),
                          L("pulse", "accent", size=2.0, spread=2.0, lifetime=8, forward=0.5, up=1.6)],
        "no2_mouth_discharge_muzzle": [L("flash", "core", size=0.9, spread=0.9, lifetime=3, forward=0.6, up=1.7),
                                       L("bolt", "accent", "burst", 6, 0.09, 0.0, 1.6, 6, 0.6, 1.7)],
        "no2_rush": [L("pulse", "accent", size=1.0, spread=1.0, lifetime=6, forward=1.2, up=1.0)],
        "no2_rush_trail": [L("smoke", "primary", size=0.5, speed=0.03, spread=0.8, lifetime=18, forward=0, up=0.1)],
        "no2_tail": [L("band", "secondary", "arc", 1, 0.8, 0.0, 3.0, 10, -1.0, 0.6, edge="accent")],
    })
    ai_data = ai("aggressor", recovery_after_heavy_ticks=24, look_speed=10.0, **SMART)
    return "kaiju_no2", moves, data, ai_data, vfx


def no6():
    ice = "#8BDCF2"
    moves = {
        "no6_claw": sweep(1.2, 4.0, 8, 50, "action.punch", 2, arc=140),
        "no6_frost_breath": projectile(0.5, 14, 160, "action.spine_shot", 3, 4, 20, ice, speed=1.4, hits=6,
                                       arc_degrees=30, slow_ticks=40, slow_level=2,
                                       particles="kn8:energy_charge"),
        "no6_ice_stake": projectile(1.7, 12, 110, "action.roar", 3, 6, 30, "#EAFBFF", speed=2.4, slow_ticks=30,
                                    slow_level=2),
        "no6_avalanche": area(2.4, 5.0, 22, 200, "action.slam", 4, 4.5, slow_ticks=40, slow_level=3),
        "no6_freezing_field": area(0.35, 10.0, 18, 300, "action.roar", 2, 9.0, sound="kn8:kaiju.roar",
                                   slow_ticks=100, slow_level=3),
        "no6_tail": sweep(1.5, 5.5, 12, 90, "action.tail_swipe", 2, arc=160),
    }
    data = kaiju(3800.0, 62.0, 19.0, 0.34, 9.6, 22.0 / 9.0, 5.0, 9.0, moves, ["reptile", "giant", "ice"],
                 reptile_parts(9.0, 5.0, 1.0), "supergiant")
    frost = [L("shard", "accent", "burst", 6, 0.3, 0.3, 1.2, 14, 0, 0.3),
             L("smoke", "core", size=0.4, speed=0.03, spread=0.8, lifetime=22, forward=0, up=0.1)]
    vfx = palette("#2A3446", "#151A24", ice, "#EAFBFF", "ice", {
        "no6_claw": [L("band", "secondary", "arc", 1, 0.7, 0.0, 2.6, 9, 1.2, 1.0, tilt=-25, edge="accent"),
                     L("shard", "accent", "burst", 4, 0.25, 0.3, 0.8, 12, 1.2, 1.0)],
        "no6_frost_breath_muzzle": [L("smoke", "accent", size=0.6, speed=0.06, spread=1.0, lifetime=20, forward=1.0,
                                      up=1.6), L("pulse", "accent", size=0.8, spread=0.8, lifetime=6, forward=0.8,
                                                 up=1.6)],
        "no6_ice_stake_muzzle": [L("shard", "core", "burst", 5, 0.3, 0.4, 1.0, 10, 0.8, 1.6)],
        "no6_avalanche": [L("wave", "accent", "ring", 1, 0.5, 0.0, 5.0, 16, 1.2, 0.05),
                          L("crack", "secondary", "line", 6, 0.3, 0.0, 4.0, 40, 1.2, 0.02, edge="core")] + frost,
        "no6_freezing_field": [L("wave", "accent", "ring", 1, 0.4, 0.0, 9.0, 30, 0, 0.05),
                               L("wave", "core", "ring", 1, 0.3, 0.0, 6.0, 30, 0, 0.05, delay=6)] + frost,
        "no6_tail": [L("band", "secondary", "arc", 1, 0.8, 0.0, 3.2, 10, -1.0, 0.6, edge="accent")],
    })
    ai_data = ai("aggressor", recovery_after_heavy_ticks=20, look_speed=10.0, **SMART)
    return "kaiju_no6", moves, data, ai_data, vfx


def no14():
    red = "#E13B43"
    moves = {
        "no14_face_beam": projectile(1.5, 10, 50, "action.beam", 3, 4, 34, red, speed=2.8,
                                     particles="kn8:energy_charge"),
        "no14_beam_volley": projectile(0.7, 14, 160, "action.volley", 2, 4, 28, red, speed=2.6, hits=4,
                                       arc_degrees=60),
        "no14_pulse": area(1.8, 4.0, 12, 120, "action.pulse", 3, 3.5, sound="kn8:kaiju.energy_beam"),
        "no14_crushing_field": area(2.4, 6.0, 24, 240, "action.slam", 4, 5.0),
        "no14_blink": leap(1.2, 200, "action.leap", 2, 8, 18),
    }
    data = kaiju(3000.0, 50.0, 24.0, 0.30, 9.0, 10.0 / 7.0, 2.6, 7.0, moves, ["construct", "levitating"], [
        {"name": "faces", "offset": [0.0, 5.2, 0.0], "width": 2.8, "height": 1.8, "multiplier": 1.3},
        {"name": "torso", "offset": [0.0, 1.0, 0.0], "width": 2.6, "height": 4.2, "multiplier": 1.0},
        {"name": "core", "offset": [0.0, 3.6, 0.9], "width": 0.9, "height": 0.9, "multiplier": 3.0, "core": True}],
        "large")
    data["rage"] = {"health_below": 0.35, "damage_multiplier": 1.15, "speed_multiplier": 1.0,
                    "cooldown_multiplier": 0.7}
    vfx = palette("#17171D", "#0C0C10", red, "#FFE0E2", "energy", {
        "no14_face_beam_muzzle": [L("flash", "accent", size=0.6, spread=0.6, lifetime=3, forward=0.6, up=1.7),
                                  L("bolt", "accent", "burst", 3, 0.07, 0.0, 1.0, 5, 0.6, 1.7)],
        "no14_beam_volley_muzzle": [L("pulse", "accent", size=1.2, spread=1.2, lifetime=6, forward=0, up=1.5),
                                    L("bolt", "accent", "burst", 6, 0.07, 0.0, 1.6, 6, 0, 1.5)],
        "no14_pulse": [L("pulse", "accent", size=1.8, spread=1.8, lifetime=8, forward=0, up=1.0),
                       L("wave", "accent", "ring", 1, 0.3, 0.0, 4.0, 12, 0, 0.05)],
        "no14_crushing_field": [L("wave", "accent", "ring", 1, 0.5, 0.0, 6.0, 16, 0, 0.05),
                                L("crack", "secondary", "line", 6, 0.3, 0.0, 5.0, 40, 0, 0.02, edge="accent"),
                                L("bolt", "accent", "burst", 6, 0.08, 0.0, 2.0, 8, 0, 0.5)],
        "no14_blink": [L("flash", "accent", size=1.0, spread=1.0, lifetime=3, forward=0, up=1.0),
                       L("bolt", "accent", "converge", 4, 0.07, 0.0, 1.4, 6, 0, 1.0)],
    })
    ai_data = ai("support", group={"role": "support", "support_distance": 12.0}, **SMART)
    ai_data["wounds"] = {"max_active": 1, "parts": [
        {"part": "faces", "threshold": 0.12, "duration_ticks": 200, "stagger_ticks": 20, "stagger_multiplier": 1.3,
         "disables": ["kn8:projectile"]}]}
    return "kaiju_no14", moves, data, ai_data, vfx


def main():
    sounds = DATA / "sound_profile/kaiju.json"
    raw = sounds.read_bytes().decode("utf-8")
    newline = "\r\n" if "\r\n" in raw else "\n"
    sound = json.loads(raw)
    hit = [L("sparks", "accent", count=5, size=0.08, speed=0.5, spread=1.0, lifetime=6, forward=1.0, up=1.1),
           L("pulse", "accent", size=0.6, spread=0.6, lifetime=5, forward=1.0, up=1.1)]
    for build in (no2, no6, no14):
        kaiju_id, moves, data, ai_data, vfx = build()
        for name, ability in moves.items():
            write("ability", name, ability)
            if ability["type"] == "kn8:projectile":
                sound["abilities"][name] = {"windup": "kn8:kaiju.energy_charge", "release": "kn8:kaiju.energy_beam",
                                            "travel": "kn8:travel.energy_beam"}
            vfx["effects"].setdefault(name, hit)
        write("kaiju", kaiju_id, data)
        write("kaiju_ai", kaiju_id, ai_data)
        write("locomotion", kaiju_id, {"stride": round(1.4 * data["dimensions"]["scale"] * 2, 1)})
        write("vfx_profile", kaiju_id, vfx)
    sounds.write_bytes((json.dumps(sound, indent=2, ensure_ascii=False) + "\n").replace("\n", newline)
                       .encode("utf-8"))
    print("NUMERADOS-B: dados gravados")


if __name__ == "__main__":
    main()
