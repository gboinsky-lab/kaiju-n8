#!/usr/bin/env python3
"""Etapa 6: fusoes do Kaiju No. 9 pelo "Dossie de Fusoes" do Miguel (2026-11-06): Abissal (No. 9 + No. 11), Neural
(No. 9 + No. 15) e Couracado (No. 9 + No. 14). Chefes extremos (T9/T9+), vida/dano/armadura/velocidade e
multiplicadores do dossie; altura visual 2,8-3,5 blocos (modelo de 2 m com escala).

Ataques so com os tipos que ja existem e as fases do dossie pelo `health_below` de cada ataque:
- Abissal: Lamina Abissal 1,35x (arco 6), Canhao de Pressao 1,8x (18 blocos, carga 0,8 s; abaixo de 65%), Cauda
  da Mare 1,55x (giro de 240, raio 5), Prisao Abissal (prende 2,5 s; abaixo de 65%), Diluvio Abissal 3 x 1,4x (raio
  12, abaixo de 30%). Fica a 7-13 blocos (apoio).
- Neural: Punhos Convergentes 4 x 0,5x, Tentaculos Perfurantes 1,65x (14 blocos), Distorcao Neural 0,55x (raio 9,
  abaixo de 70%), Carcere Neural 3 alvos presos 2,2 s (abaixo de 70%), Dominio Cerebral 6 x 0,67x (raio 11, abaixo
  de 35%). Flanqueia.
- Couracado: Punho de Ruptura 1,7x, Explosao de Nucleo 1,9x (raio 7), Lanca de Energia 2,0x (20 blocos, carga 1 s),
  Colapso Absoluto 4,5x (raio 10, abaixo de 25%); Couraca Adaptativa (aparo frontal: 18% a menos de frente) e
  Muralha Negra (escudo frontal de 65% por 3,5 s a cada 18 s, sem virar brecha).

Nascem quando o No. 9 (ou a forma preta) absorve o No. 11, o No. 15 ou o No. 14 derrotado (`absorb` do
numbered/kaiju_no9*.json) e, raras, nas invasoes grandes (1%, como as outras formas).

Uso: python3 tools/data/gen_fusoes.py   (depois do rig: rig_from_skin.py importar, rig_kaiju_no9_mesh.py e
humanoid_anims.py para as tres)
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from gen_numerados_a import (DATA, L, SMART, ai, area, flurry, humanoid, melee, palette, projectile,  # noqa: E402
                             sweep, write)

HIT = [L("sparks", "accent", count=5, size=0.08, speed=0.5, spread=1.0, lifetime=6, forward=1.0, up=1.1),
       L("pulse", "accent", size=0.6, spread=0.6, lifetime=5, forward=1.0, up=1.1)]

# Especie que o No. 9 absorve -> fusao, chave do aviso.
ABSORB = [("kn8:kaiju_no11", "kn8:kaiju_no9_no11", "kn8.no9.abyss_form"),
          ("kn8:kaiju_no15", "kn8:kaiju_no9_no15", "kn8.no9.neural_form"),
          ("kn8:kaiju_no14", "kn8:kaiju_no9_no14", "kn8.no9.armored_form")]
# Invasoes grandes onde as formas do No. 9 ja aparecem de surpresa (1%).
SURPRISE = ["kaiju_horde", "mass_resurrection", "no10_assault", "ariake_assault"]


def phase(ability, below):
    ability["behavior"]["health_below"] = below
    return ability


def extra(ability, **behavior):
    ability["behavior"].update(behavior)
    return ability


def fusion(health, damage, armor, speed, height, moves, tags):
    data = humanoid(health, damage, armor, speed, 9.6, height / 2.0, 2.0, moves, tags, 5, "medium")
    data["rage"] = {"health_below": 0.3, "damage_multiplier": 1.2, "speed_multiplier": 1.1,
                    "cooldown_multiplier": 0.7}
    return data


def abyss():
    water = "#46D3E2"
    moves = {
        "no9_abyss_blade": sweep(1.35, 6.0, 10, 100, "action.slash", 2, arc=150),
        "no9_abyss_cannon": phase(projectile(1.8, 16, 220, "action.beam", 4, 5, 18, water, speed=3.0, heavy=True,
                                             particles="kn8:energy_charge"), 0.65),
        "no9_abyss_tail": sweep(1.55, 5.0, 12, 180, "action.wave", 3, arc=240, heavy=True),
        "no9_abyss_prison": phase(projectile(0.9, 12, 320, "action.point", 3, 3, 14, "#BFF6FF", speed=2.0,
                                             slow_ticks=50, slow_level=5), 0.65),
        "no9_abyss_deluge": phase(flurry(1.4, 12.0, 3, 10, 1100, "action.deluge", 5, 12.0), 0.3),
    }
    data = fusion(7200.0, 62.0, 22.0, 0.38, 3.0, moves, ["intelligent", "fusion", "water"])
    vfx = palette("#1E4FD8", "#F2F6FF", water, "#FFFFFF", "energy", {
        "no9_abyss_blade": [L("band", "accent", "arc", 1, 0.7, 0.0, 3.0, 9, 1.2, 1.2, tilt=-15, edge="core"),
                            L("smoke", "secondary", size=0.4, speed=0.04, spread=1.2, lifetime=14, forward=1.4,
                              up=1.0)],
        "no9_abyss_cannon_muzzle": [L("pulse", "accent", size=0.8, spread=0.8, lifetime=5, forward=0.6, up=1.7),
                                    L("smoke", "secondary", size=0.4, speed=0.05, spread=0.8, lifetime=14,
                                      forward=0.8, up=1.7)],
        "no9_abyss_tail": [L("band", "primary", "arc", 1, 0.8, 0.0, 3.2, 10, -1.0, 0.6, edge="accent"),
                           L("wave", "accent", "ring", 1, 0.3, 0.0, 4.0, 12, 0, 0.05)],
        "no9_abyss_prison_muzzle": [L("pulse", "core", size=0.7, spread=0.7, lifetime=6, forward=0.6, up=1.4)],
        "no9_abyss_deluge": [L("wave", "accent", "ring", 1, 0.6, 0.0, 11.0, 20, 0, 0.05),
                             L("wave", "secondary", "ring", 1, 0.4, 0.0, 7.0, 16, 0, 0.05, delay=6),
                             L("smoke", "secondary", size=0.8, speed=0.06, spread=4.0, lifetime=26, forward=0,
                               up=0.4)],
    })
    # Pressao Abissal [SUPOSICAO]: cada golpe que acerta poe uma carga no alvo (dura 30 s); com 4, golpe de pressao e Lentidao.
    ai_data = ai("support", group={"role": "support", "support_distance": 10.0}, terrain={"water_malus": 0.0},
                 charges={"max": 4, "on_hit": 1, "decay_ticks": 600, "burst_damage": 0.8, "slow_ticks": 60,
                          "slow_level": 2}, **SMART)
    return "kaiju_no9_no11", moves, data, ai_data, vfx


def neural():
    pink = "#E040B0"
    moves = {
        "no9_neural_fists": flurry(0.5, 4.5, 4, 5, 160, "action.flurry", 2, 4.5),
        # Tentaculos puxam quem acertam; Distorcao e Dominio tomam a mente (controle mental do No. 15).
        "no9_neural_tentacles": extra(projectile(1.65, 10, 200, "action.point", 3, 4, 14, pink, speed=2.6),
                                      pull=0.9),
        "no9_neural_distortion": extra(phase(area(0.55, 9.0, 16, 340, "action.wave", 3, 8.0, sound="kn8:kaiju.roar",
                                                  slow_ticks=60, slow_level=1), 0.7), mental_ticks=60),
        "no9_neural_cage": phase(projectile(0.6, 14, 380, "action.beam", 3, 3, 14, "#F7B8E8", speed=2.2, hits=3,
                                            arc_degrees=50, slow_ticks=44, slow_level=5), 0.7),
        "no9_neural_domain": extra(phase(flurry(0.67, 11.0, 6, 6, 1200, "action.domain", 5, 11.0), 0.35),
                                   mental_ticks=80),
    }
    data = fusion(6800.0, 58.0, 21.0, 0.42, 3.0, moves, ["intelligent", "fusion", "psychic"])
    vfx = palette("#7A2AA8", "#F2E6EE", pink, "#FFE6F8", "energy", {
        "no9_neural_fists": [L("pulse", "accent", size=0.7, spread=0.7, lifetime=5, forward=1.2, up=1.1),
                             L("sparks", "core", count=4, size=0.07, speed=0.5, spread=1.0, lifetime=6, forward=1.2,
                               up=1.1)],
        "no9_neural_tentacles_muzzle": [L("bolt", "accent", "burst", 3, 0.07, 0.0, 1.2, 6, 0.6, 1.4)],
        "no9_neural_distortion": [L("pulse", "accent", size=2.4, spread=2.4, lifetime=10, forward=0, up=1.6),
                                  L("wave", "primary", "ring", 1, 0.4, 0.0, 9.0, 16, 0, 0.05)],
        "no9_neural_cage_muzzle": [L("pulse", "core", size=0.8, spread=0.8, lifetime=6, forward=0.6, up=1.6)],
        "no9_neural_domain": [L("wave", "accent", "ring", 1, 0.5, 0.0, 10.0, 18, 0, 0.05),
                              L("bolt", "accent", "burst", 8, 0.08, 0.0, 4.0, 10, 0, 1.0),
                              L("pulse", "primary", size=3.0, spread=3.0, lifetime=12, forward=0, up=1.6)],
    })
    # Consciencia Expandida [SUPOSICAO]: preve o golpe repetido (como o No. 1, mais lenta) e cada golpe desviado guarda
    # uma carga; com 3, o proximo golpe dela sai x1,5.
    ai_data = ai("flanker", circle_chance=0.4, chase_speed=1.25,
                 foresight={"enabled": True, "cooldown_ticks": 100, "feint_bonus": 0.15, "feint_ticks": 30},
                 charges={"max": 3, "on_dodge": 1, "decay_ticks": 400, "empower": 1.5}, **SMART)
    return "kaiju_no9_no15", moves, data, ai_data, vfx


def armored():
    red = "#E13B43"
    moves = {
        "no9_armored_rupture": melee(1.7, 12, 140, "action.punch", 3, 5.0, heavy=True),
        "no9_armored_core_burst": area(1.9, 7.0, 18, 320, "action.burst", 3, 6.0, sound="kn8:kaiju.energy_beam"),
        "no9_armored_lance": projectile(2.0, 20, 260, "action.beam", 3, 6, 20, red, speed=3.2, heavy=True,
                                        particles="kn8:energy_charge"),
        "no9_armored_collapse": phase(area(4.5, 10.0, 40, 1300, "action.collapse", 5, 9.0), 0.25),
    }
    data = fusion(8400.0, 68.0, 25.0, 0.30, 3.3, moves, ["intelligent", "fusion", "armored"])
    vfx = palette("#F0EEE6", "#141418", red, "#FFE0E2", "energy", {
        "no9_armored_rupture": [L("pulse", "accent", size=1.0, spread=1.0, lifetime=6, forward=1.4, up=1.0),
                                L("crack", "secondary", "line", 3, 0.2, 0.0, 2.0, 24, 1.4, 0.02, edge="accent")],
        "no9_armored_core_burst": [L("flash", "accent", size=1.2, spread=1.2, lifetime=4, forward=0, up=1.4),
                                   L("wave", "accent", "ring", 1, 0.4, 0.0, 6.5, 14, 0, 0.05),
                                   L("bolt", "accent", "burst", 6, 0.08, 0.0, 3.0, 8, 0, 1.2)],
        "no9_armored_lance_muzzle": [L("flash", "core", size=0.7, spread=0.7, lifetime=3, forward=0.8, up=1.6),
                                     L("bolt", "accent", "burst", 4, 0.07, 0.0, 1.4, 6, 0.8, 1.6)],
        "no9_armored_collapse": [L("wave", "accent", "ring", 1, 0.6, 0.0, 9.5, 18, 0, 0.05),
                                 L("crack", "secondary", "line", 8, 0.3, 0.0, 8.0, 40, 0, 0.02, edge="accent"),
                                 L("flash", "accent", size=2.0, spread=2.0, lifetime=5, forward=0, up=1.0)],
        # Muralha Negra: anel na frente enquanto as placas fecham, estalo no golpe barrado e estouro ao abrir.
        "shield": [L("pulse", "secondary", size=1.6, spread=1.6, lifetime=12, forward=0.9, up=1.2)],
        "shield_hit": [L("sparks", "accent", count=5, size=0.08, speed=0.5, spread=1.2, lifetime=6, forward=0.9,
                         up=1.2)],
        "shield_break": [L("flash", "accent", size=1.0, spread=1.0, lifetime=4, forward=0.9, up=1.2)],
    })
    ai_data = ai("aggressor", recovery_after_heavy_ticks=24, look_speed=10.0,
                 guard={"factor": 0.82, "front_arc_degrees": 140.0, "open_after_attack_ticks": 20,
                        "blocks_heavy": True, "low_health_below": 0.65, "low_factor": 0.92},
                 shield={"reduction": 0.65, "up_ticks": 70, "down_ticks": 290, "front_arc_degrees": 120.0,
                         "exposed": False}, **SMART)
    return "kaiju_no9_no14", moves, data, ai_data, vfx


def add_absorb():
    for form in ("kaiju_no9", "kaiju_no9_black"):
        path = DATA / "numbered" / f"{form}.json"
        data = json.loads(path.read_text(encoding="utf-8"))
        absorb = data.setdefault("absorb", [])
        for prey, into, message in ABSORB:
            if not any(entry["into"] == into for entry in absorb):
                absorb.append({"species": [prey], "into": into, "seek_radius": 20, "cast_ticks": 60,
                               "target_health_below": 0.3, "self_health_below": 1.0, "carcass": True,
                               "message": message})
        write("numbered", form, data)


def add_surprises(kaiju_id):
    for invasion in SURPRISE:
        path = DATA / "invasion" / f"{invasion}.json"
        data = json.loads(path.read_text(encoding="utf-8"))
        wave = data["waves"][-1]["kaiju"]
        species = f"kn8:{kaiju_id}"
        if not any(entry["species"] == species for entry in wave):
            wave.append({"species": species, "count": 1, "chance": 0.01})
            write("invasion", invasion, data)


def main():
    sounds = DATA / "sound_profile/kaiju.json"
    raw = sounds.read_bytes().decode("utf-8")
    newline = "\r\n" if "\r\n" in raw else "\n"
    sound = json.loads(raw)
    # Mesmo comportamento das formas do No. 9 (regenera, adapta, se retira): copia do No. 9 com o Isao.
    base = json.loads((DATA / "numbered/kaiju_no9_isao.json").read_text(encoding="utf-8"))
    for build in (abyss, neural, armored):
        kaiju_id, moves, data, ai_data, vfx = build()
        for name, ability in moves.items():
            write("ability", name, ability)
            if ability["type"] == "kn8:projectile":
                sound["abilities"][name] = {"windup": "kn8:kaiju.energy_charge", "release": "kn8:kaiju.energy_beam",
                                            "travel": "kn8:travel.energy_beam"}
            vfx["effects"].setdefault(name, HIT)
        vfx["effects"].setdefault("claw", HIT)
        write("kaiju", kaiju_id, data)
        # Mantem o que os geradores seguintes poem no kaiju_ai (resistencia dos chefes da 7-C).
        old = DATA / "kaiju_ai" / f"{kaiju_id}.json"
        if old.exists():
            previous = json.loads(old.read_text(encoding="utf-8"))
            if "control_resist" in previous:
                ai_data["control_resist"] = previous["control_resist"]
        write("kaiju_ai", kaiju_id, ai_data)
        write("numbered", kaiju_id, base)
        write("locomotion", kaiju_id, {"stride": round(1.4 * data["dimensions"]["scale"], 1)})
        write("vfx_profile", kaiju_id, vfx)
        add_surprises(kaiju_id)
    add_absorb()
    sounds.write_bytes((json.dumps(sound, indent=2, ensure_ascii=False) + "\n").replace("\n", newline)
                       .encode("utf-8"))
    print("Etapa 6: dados das fusoes gravados")


if __name__ == "__main__":
    main()
