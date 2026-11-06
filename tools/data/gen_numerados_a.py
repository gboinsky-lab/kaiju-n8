#!/usr/bin/env python3
"""NUMERADOS-A (etapa 4 do plano de 2026-11-05): Kaiju No. 1, No. 11, No. 12 e No. 15 (forma kaiju e forma Kikoru),
modelos do Miguel com o rig do Meshy. Habilidades pelo dossie do Miguel (secao II, CANON), so com os tipos que ja
existem (golpe, varrida, area, rajada, projetil, investida, salto); numeros [SUPOSICAO] pela tabela de tiers v1.2
(fortitude 9,0 = perto do No. 13). Alturas: No. 1 20 m (tabela mestra), os outros [SUPOSICAO] (tabela: pendente).

Uso: python3 tools/data/gen_numerados_a.py   (depois: humanoid_anims.py <especie>)
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / "src/main/resources/data/kn8/kn8"


def write(folder, name, data):
    path = DATA / folder / f"{name}.json"
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def melee(mult, windup, cooldown, anim, prio, max_range, heavy=False, sound="kn8:kaiju.punch", **behavior):
    data = {"type": "kn8:melee", "damage_multiplier": mult, "windup_ticks": windup, "active_ticks": 2,
            "cooldown_ticks": cooldown, "animation": anim,
            "vfx": [{"effect": "kn8:impact", "intensity": 1.0 if heavy else 0.7}],
            "camera_shake": 0.3 if heavy else 0.15, "sound": sound,
            "behavior": {"priority": prio, "max_range": max_range, **behavior}}
    if heavy:
        data["heavy"] = True
    return data


def sweep(mult, radius, windup, cooldown, anim, prio, arc=150, heavy=False, sound="kn8:kaiju.claw", **behavior):
    data = {"type": "kn8:sweep", "damage_multiplier": mult, "radius": radius, "windup_ticks": windup,
            "active_ticks": 2, "cooldown_ticks": cooldown, "animation": anim,
            "vfx": [{"effect": "kn8:impact", "intensity": 0.9}], "camera_shake": 0.2, "sound": sound,
            "behavior": {"priority": prio, "arc_degrees": arc, "arc_center": 0, **behavior}}
    if heavy:
        data["heavy"] = True
        data["telegraph"] = "arc"
    return data


def flurry(mult, radius, hits, interval, cooldown, anim, prio, max_range):
    return {"type": "kn8:multi_hit", "damage_multiplier": mult, "radius": radius, "windup_ticks": 5,
            "active_ticks": hits * interval, "cooldown_ticks": cooldown, "animation": anim,
            "vfx": [{"effect": "kn8:impact", "intensity": 0.6}], "camera_shake": 0.15, "sound": "kn8:kaiju.claw",
            "behavior": {"priority": prio, "max_range": max_range, "hits": hits, "hit_interval": interval}}


def projectile(mult, windup, cooldown, anim, prio, min_range, max_range, color, speed=2.4, heavy=False,
               sound="kn8:kaiju.energy_beam", particles=None, **behavior):
    data = {"type": "kn8:projectile", "damage_multiplier": mult, "windup_ticks": windup, "active_ticks": 2,
            "cooldown_ticks": cooldown, "animation": anim, "vfx": [{"effect": "kn8:impact", "intensity": 0.9}],
            "camera_shake": 0.2, "sound": sound,
            "behavior": {"priority": prio, "min_range": min_range, "max_range": max_range,
                         "projectile_speed": speed, "projectile_range": max_range + 4, "color": color, **behavior}}
    if heavy:
        data["heavy"] = True
        data["telegraph"] = "line"
    if particles:
        data["particles"] = particles
    return data


def area(mult, radius, windup, cooldown, anim, prio, max_range, sound="kn8:kaiju.slam", **behavior):
    return {"type": "kn8:area_melee", "damage_multiplier": mult, "radius": radius, "windup_ticks": windup,
            "active_ticks": 4, "cooldown_ticks": cooldown, "animation": anim, "telegraph": "ground_circle",
            "heavy": True, "vfx": [{"effect": "kn8:shockwave", "intensity": 1.0}, {"effect": "kn8:dust",
                                                                                     "intensity": 0.8}],
            "camera_shake": 0.35, "sound": sound, "behavior": {"priority": prio, "max_range": max_range, **behavior}}


def charge(mult, cooldown, anim, prio, min_range, max_range):
    return {"type": "kn8:charge", "damage_multiplier": mult, "radius": 1.3, "windup_ticks": 8, "active_ticks": 16,
            "cooldown_ticks": cooldown, "animation": anim, "telegraph": "line", "heavy": True,
            "destruction": {"radius": 1.2, "power": 2, "crater": False},
            "vfx": [{"effect": "kn8:dust", "intensity": 0.6}, {"effect": "kn8:impact", "intensity": 0.8}],
            "camera_shake": 0.25, "sound": "kn8:kaiju.charge_rush",
            "behavior": {"priority": prio, "min_range": min_range, "max_range": max_range}}


def leap(mult, cooldown, anim, prio, min_range, max_range):
    return {"type": "kn8:leap", "damage_multiplier": mult, "radius": 2.0, "windup_ticks": 8, "active_ticks": 20,
            "cooldown_ticks": cooldown, "animation": anim, "telegraph": "ground_circle", "heavy": True,
            "vfx": [{"effect": "kn8:impact", "intensity": 0.9}, {"effect": "kn8:dust", "intensity": 0.8}],
            "camera_shake": 0.3, "sound": "kn8:kaiju.leap",
            "behavior": {"priority": prio, "min_range": min_range, "max_range": max_range}}


def humanoid(health, damage, armor, speed, fortitude, scale, height, abilities, tags, intelligence, size):
    width = 0.8 if height >= 1.8 else 0.6
    return {"class": "numbered", "fortitude": fortitude, "size": size,
            "dimensions": {"width": width, "height": height, "scale": scale},
            "overrides": {"health": health, "damage": damage, "armor": armor}, "speed": speed,
            "intelligence": intelligence,
            "parts": [
                {"name": "head", "offset": [0.0, height * 0.82, 0.0], "width": 0.5, "height": height * 0.2,
                 "multiplier": 1.3},
                {"name": "torso", "offset": [0.0, height * 0.46, 0.0], "width": width - 0.1,
                 "height": height * 0.34, "multiplier": 1.0},
                {"name": "core", "offset": [0.0, height * 0.62, 0.15], "width": 0.25, "height": 0.25,
                 "multiplier": 3.0, "core": True},
                {"name": "legs", "offset": [0.0, 0.0, 0.0], "width": width - 0.2, "height": height * 0.45,
                 "multiplier": 0.8}],
            "core": {"health_fraction": 0.25},
            "abilities": ["kn8:claw"] + [f"kn8:{name}" for name in abilities],
            "weaknesses": [], "spawn": {"natural": False}, "rarity": "event",
            "tags": ["humanoid", "numbered"] + tags,
            "rage": {"health_below": 0.35, "damage_multiplier": 1.15, "speed_multiplier": 1.1,
                     "cooldown_multiplier": 0.75}}


def ai(role="aggressor", **extra):
    data = {"circle_chance": 0.3, "circle_distance": 5.0, "circle_ticks": 24, "chase_speed": 1.2,
            "ranged_beyond": 6.0, "ranged_priority": 1,
            "wounds": {"max_active": 2, "parts": [
                {"part": "legs", "threshold": 0.12, "duration_ticks": 200, "speed_multiplier": 0.75,
                 "disables": ["kn8:charge", "kn8:leap"], "damage_taken": 1.2},
                {"part": "head", "threshold": 0.1, "duration_ticks": 200, "stagger_ticks": 20,
                 "stagger_multiplier": 1.3}]},
            "group": {"role": role}}
    data.update(extra)
    return data


# Leitura de habitos (PM-3) com os numeros do No. 9: le o alvo e responde com o que tem.
SMART = {"habits": json.loads((DATA / "kaiju_ai/kaiju_no9.json").read_text(encoding="utf-8"))["habits"]}


def palette(primary, secondary, accent, core, material, effects):
    return {"palette": {"primary": primary, "secondary": secondary, "accent": accent, "core": core},
            "material": material, "effects": effects}


def L(shape, color="accent", pattern="burst", count=1, size=0.3, speed=0.0, spread=1.0, lifetime=10, forward=0.8,
      up=1.2, delay=0, tilt=0.0, edge=""):
    layer = {"shape": shape, "color": color, "pattern": pattern, "count": count, "size": size, "speed": speed,
             "spread": spread, "lifetime": lifetime, "forward": forward, "up": up}
    for key, value in (("delay", delay), ("tilt", tilt), ("edge", edge)):
        if value:
            layer[key] = value
    return layer


def no1():
    """No. 1: olhos que preveem o movimento (pseudo-precognicao, visao anatomica, ataque inevitavel)."""
    eye = "#E8E030"
    moves = {
        # Ataque inevitavel: le o impulso e golpeia antes da esquiva (preparo curtissimo).
        "no1_inevitable_strike": melee(1.6, 3, 70, "action.punch", 4, 3.5),
        # Visao anatomica: golpe no ponto fraco (articulacoes), deixa o alvo vulneravel.
        "no1_weak_point_strike": flurry(0.6, 1.2, 4, 3, 110, "action.flurry", 3, 3.2),
        # Feixe dos olhos: tiro preciso e rapido.
        "no1_retina_beam": projectile(1.4, 6, 50, "action.beam", 3, 4, 40, eye, speed=3.2),
        # Leitura: rajada de feixes em leque para onde o alvo vai esquivar.
        "no1_predictive_volley": projectile(0.7, 10, 140, "action.beam", 2, 6, 34, eye, speed=3.0, hits=5,
                                            arc_degrees=40),
        "no1_sprint": charge(1.3, 90, "action.charge", 3, 6, 26),
    }
    return moves, humanoid(3500.0, 60.0, 19.0, 0.36, 9.3, 10.0, 2.0, moves, ["intelligent", "giant"], 5,
                           "supergiant"), ai("aggressor", **SMART), \
        palette("#2A2A30", "#141418", eye, "#FFFFF0", "flesh", {
            "no1_retina_beam_muzzle": [L("flash", "accent", size=0.4, spread=0.4, lifetime=3, forward=0.5, up=1.7),
                                       L("beam", "accent", "line", 1, 0.05, 0.0, 3.0, 4, 0.6, 1.7)],
            "no1_predictive_volley_muzzle": [L("flash", "accent", size=0.5, spread=0.5, lifetime=3, forward=0.5,
                                               up=1.7),
                                             L("sparks", "accent", "arc", 5, 0.06, 0.6, 1.4, 4, 0.6, 1.7)],
            "no1_inevitable_strike": [L("beam", "accent", "line", 1, 0.06, 0.0, 2.0, 4, 1.0, 1.2),
                                      L("pulse", "core", size=0.7, spread=0.7, lifetime=5, forward=1.2, up=1.1)],
            "no1_weak_point_strike": [L("sparks", "accent", count=4, size=0.07, speed=0.5, lifetime=5, forward=1.0,
                                        up=1.1)],
            "no1_sprint_trail": [L("streak", "accent", "line", 2, 0.06, 0.0, 1.2, 4, 0, 1.0)],
        })


def no11():
    """No. 11: hidrocinese (onda, jato, esferas de agua) e braco inchado no soco."""
    water = "#3FA8E8"
    moves = {
        "no11_sweeping_wave": sweep(1.3, 6.0, 12, 110, "action.wave", 3, arc=160, heavy=True,
                                    sound="kn8:kaiju.slam", slow_ticks=30, slow_level=2),
        "no11_water_jet": projectile(1.5, 8, 60, "action.beam", 3, 4, 30, water, speed=2.8),
        "no11_water_sphere": projectile(0.8, 10, 110, "action.point", 2, 5, 28, water, speed=1.8, hits=3,
                                        arc_degrees=24),
        "no11_swollen_punch": melee(2.4, 14, 140, "action.punch", 4, 3.8, heavy=True),
        "no11_flurry": flurry(0.5, 1.0, 4, 3, 70, "action.flurry", 2, 3.0),
    }
    return moves, humanoid(2500.0, 46.0, 18.0, 0.4, 9.0, 2.25, 2.0, moves, ["intelligent"], 4, "large"), \
        ai("support", group={"role": "support", "support_distance": 10.0}), \
        palette("#2E5A6A", "#16262E", water, "#E0F6FF", "scales", {
            "no11_sweeping_wave": [L("wave", "accent", "ring", 1, 0.5, 0.0, 4.0, 14, 1.5, 0.1),
                                   L("band", "accent", "arc", 1, 0.8, 0.0, 3.5, 12, 1.5, 0.6, edge="core"),
                                   L("smoke", "core", size=0.4, speed=0.04, spread=1.2, lifetime=18, forward=1.5,
                                     up=0.3)],
            "no11_water_jet_muzzle": [L("pulse", "accent", size=0.5, spread=0.5, lifetime=4, forward=0.6, up=1.6),
                                      L("beam", "core", "line", 1, 0.08, 0.0, 2.0, 4, 0.6, 1.6)],
            "no11_water_sphere_muzzle": [L("pulse", "accent", size=0.6, spread=0.6, lifetime=5, forward=0.8,
                                           up=1.3)],
            "no11_swollen_punch": [L("pulse", "accent", size=1.0, spread=1.0, lifetime=6, forward=1.3, up=1.1),
                                   L("sparks", "core", count=6, size=0.08, speed=0.6, lifetime=6, forward=1.3,
                                     up=1.1)],
        })


def no12():
    """No. 12: antebracos em laminas, espada de carne, esgrima que se adapta (le os habitos)."""
    blade = "#C8C0B4"
    moves = {
        "no12_blade_arms": sweep(1.4, 3.6, 8, 50, "action.slash", 3, arc=150),
        "no12_blade_flurry": flurry(0.55, 1.4, 5, 3, 80, "action.flurry", 3, 3.4),
        "no12_flesh_sword": sweep(2.4, 5.0, 14, 150, "action.slash", 4, arc=120, heavy=True,
                                  sound="kn8:kaiju.claw"),
        "no12_thrust": melee(1.8, 8, 90, "action.punch", 2, 4.2),
        "no12_lunge": charge(1.4, 100, "action.charge", 3, 6, 22),
    }
    return moves, humanoid(2600.0, 48.0, 19.0, 0.42, 9.0, 2.5, 2.0, moves, ["intelligent"], 5, "large"), \
        ai("aggressor", **SMART), \
        palette("#6A6A6E", "#2A2A2E", blade, "#FFF8F0", "flesh", {
            "no12_blade_arms": [L("band", "secondary", "arc", 1, 0.6, 0.0, 2.2, 9, 1.0, 1.1, tilt=-20,
                                  edge="accent")],
            "no12_blade_flurry": [L("band", "secondary", "arc", 1, 0.5, 0.0, 1.8, 8, 1.0, 1.1, tilt=30,
                                    edge="accent")],
            "no12_flesh_sword": [L("band", "secondary", "arc", 1, 0.9, 0.0, 3.2, 12, 1.2, 1.1, tilt=-10,
                                   edge="accent"),
                                 L("crack", "secondary", "line", 3, 0.2, 0.0, 2.0, 30, 1.2, 0.02, edge="accent")],
            "no12_thrust": [L("beam", "accent", "line", 1, 0.08, 0.0, 2.0, 4, 1.0, 1.1)],
        })


def no15(kikoru):
    """No. 15: voo, metamorfose dos membros, regeneracao, feixe bucal (leque ou unico) e a conexao psicogenica que
    paralisa (Lentidao forte); a forma Kikoru imita a Kikoru (machado de carne)."""
    beam = "#B05AE0"
    prefix = "no15k" if kikoru else "no15"
    moves = {
        f"{prefix}_mouth_beam": projectile(2.2, 14, 160, "action.beam", 3, 6, 34, beam, speed=2.6, heavy=True),
        f"{prefix}_spread_beam": projectile(0.7, 10, 120, "action.beam", 2, 5, 28, beam, speed=2.6, hits=5,
                                            arc_degrees=50),
        f"{prefix}_psychic_link": projectile(0.4, 16, 300, "action.point", 4, 4, 24, "#E0C8FF", speed=1.6,
                                             slow_ticks=60, slow_level=5),
        f"{prefix}_limb_axe": sweep(2.0, 4.2, 12, 110, "action.slash", 3, arc=140, heavy=True),
        f"{prefix}_flurry": flurry(0.55, 1.2, 4, 3, 70, "action.flurry", 2, 3.0),
        f"{prefix}_flight_dive": leap(1.6, 140, "action.leap", 3, 6, 18),
    }
    if kikoru:
        kaiju = humanoid(1800.0, 40.0, 17.0, 0.44, 9.0, 1.0, 1.6, moves, ["intelligent", "mimic"], 5, "medium")
    else:
        kaiju = humanoid(2400.0, 46.0, 18.0, 0.42, 9.0, 1.75, 2.0, moves, ["intelligent"], 5, "large")
    return moves, kaiju, ai("flanker", **SMART), palette(
        "#5A3A6A" if not kikoru else "#E8C060", "#1E1424", beam, "#F4E8FF", "flesh", {
            f"{prefix}_mouth_beam_muzzle": [L("flash", "accent", size=0.6, spread=0.6, lifetime=3, forward=0.4,
                                              up=1.6),
                                            L("bolt", "accent", "converge", 4, 0.07, 0.0, 1.0, 8, 0.4, 1.6)],
            f"{prefix}_spread_beam_muzzle": [L("flash", "accent", size=0.5, spread=0.5, lifetime=3, forward=0.4,
                                               up=1.6)],
            f"{prefix}_psychic_link_muzzle": [L("pulse", "core", "rise", 3, 0.6, 0.1, 0.8, 12, 0.5, 1.6),
                                              L("beam", "core", "line", 1, 0.03, 0.0, 4.0, 14, 0.6, 1.6)],
            f"{prefix}_limb_axe": [L("band", "secondary", "arc", 1, 0.8, 0.0, 2.6, 10, 1.1, 1.0, tilt=-15,
                                     edge="accent")],
            f"{prefix}_flight_dive": [L("wave", "accent", "ring", 1, 0.3, 0.0, 2.4, 10, 0, 0.05)],
        })


def main():
    sounds = DATA / "sound_profile/kaiju.json"
    raw = sounds.read_bytes().decode("utf-8")
    newline = "\r\n" if "\r\n" in raw else "\n"
    sound = json.loads(raw)
    for kaiju_id, build in (("kaiju_no1", no1), ("kaiju_no11", no11), ("kaiju_no12", no12),
                            ("kaiju_no15", lambda: no15(False)), ("kaiju_no15_kikoru", lambda: no15(True))):
        moves, kaiju, kai, vfx = build()
        for name, data in moves.items():
            write("ability", name, data)
            if data["type"] == "kn8:projectile":
                sound["abilities"][name] = {"windup": "kn8:kaiju.energy_charge", "release": "kn8:kaiju.energy_beam",
                                            "travel": "kn8:travel.energy_beam"}
        write("kaiju", kaiju_id, kaiju)
        write("kaiju_ai", kaiju_id, kai)
        scale = kaiju["dimensions"]["scale"]
        write("locomotion", kaiju_id, {"stride": round(1.4 * scale, 1)})
        # Toda habilidade com receita propria (a do tiro na saida fica no _muzzle): impacto na cor do kaiju.
        hit = [{"shape": "sparks", "color": "accent", "pattern": "burst", "count": 5, "size": 0.08, "speed": 0.5,
                "spread": 1.0, "lifetime": 6, "forward": 1.0, "up": 1.1},
               {"shape": "pulse", "color": "accent", "pattern": "burst", "count": 1, "size": 0.6, "speed": 0.0,
                "spread": 0.6, "lifetime": 5, "forward": 1.0, "up": 1.1}]
        for name in ["claw", *moves]:
            vfx["effects"].setdefault(name, hit)
        write("vfx_profile", kaiju_id, vfx)
    sounds.write_bytes((json.dumps(sound, indent=2, ensure_ascii=False) + "\n").replace("\n", newline)
                       .encode("utf-8"))
    print("NUMERADOS-A: dados gravados")


if __name__ == "__main__":
    main()
