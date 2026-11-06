#!/usr/bin/env python3
"""Etapa 5 do plano de 2026-11-05: kaiju de nome cientifico da onda de ataques da 2a temporada (modelos do Miguel de
2026-11-05, sem rig): Polyonax, Lacerta Fratris, Imitatio Mantidis (preto e verde: o Miguel quer as duas), Maxilla
Cornuta e Anguilla Ambulans.

Vida, dano, armadura, velocidade, multiplicadores e paleta pelo "Balanceamento Completo dos Kaijus" v1.0 do Miguel
(docs/referencias_balanceamento/, secao dos kaiju da 2a temporada; T4/T5 = Honju). Ataques so com os tipos que ja
existem; animacoes do Philinosoma (bite, charge, punch, roar, slam, spine_shot, tail_swipe).

- Polyonax (T5, quadrupede pesado): golpe 1,4x, investida 1,8x, pisao 2,1x, varredura 1,5x em area.
- Lacerta Fratris (T4, predador movel): mordida 1,2x, cauda 1,3x, salto lateral, combo 3 x 0,55x.
- Imitatio Mantidis (T4+, agil): laminas 2 x 0,9x, salto 1,4x, corte cruzado 1,8x; aparo frontal parcial (bloco
  `guard` do kaiju_ai: de frente leva 50%; costas, depois do ataque e golpe pesado passam).
- Maxilla Cornuta (T5, bruiser): chifrada 1,8x, carga 2,3x (aviso na linha), pisao 2,0x, rugido 1,0x em area.
- Anguilla Ambulans (T4+, mobilidade): mordida 1,4x, chicote 1,6x, descarga/onda 1,8x em area, mergulho (salto).

Onde aparecem (antes da etapa 7, que revisa tudo): na ultima onda de uma invasao cada, com chance [SUPOSICAO].

Uso: python3 tools/data/gen_especies.py   (depois: rig_primigenius_mesh.py <especie> para os seis)
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from gen_numerados_a import DATA, L, ai, area, charge, flurry, leap, melee, palette, sweep, write  # noqa: E402

# Invasao, chance na ultima onda.
APPEARS = {
    "polyonax": ("kaiju_horde", 0.4),
    "lacerta_fratris": ("tachikawa_city_raid", 0.4),
    "imitatio_mantidis": ("honju_rampage", 0.35),
    "imitatio_mantidis_green": ("diclonius_attack", 0.35),
    "maxilla_cornuta": ("honju_assault", 0.4),
    "anguilla_ambulans": ("ariake_assault", 0.35),
}


def honju(health, damage, armor, speed, fortitude, width, height, head_z, abilities, tags):
    return {"class": "honju", "fortitude": fortitude, "size": "large",
            "dimensions": {"width": width, "height": height},
            "overrides": {"health": health, "damage": damage, "armor": armor}, "speed": speed,
            "intelligence": 3,
            "parts": [
                {"name": "head", "offset": [0.0, height * 0.75, head_z], "width": width * 0.4,
                 "height": height * 0.25, "multiplier": 1.2},
                {"name": "torso", "offset": [0.0, height * 0.3, 0.0], "width": width * 0.75,
                 "height": height * 0.5, "multiplier": 1.0},
                {"name": "core", "offset": [0.0, height * 0.5, 0.8], "width": 1.2, "height": 1.2, "multiplier": 3.0,
                 "core": True},
                {"name": "legs", "offset": [0.0, 0.0, 0.0], "width": width * 0.9, "height": height * 0.33,
                 "multiplier": 0.8}],
            "core": {"health_fraction": 0.25, "exposed_on": ["heavy_hit"]},
            "abilities": [f"kn8:{name}" for name in abilities], "weaknesses": [],
            "dismantle": "kn8:primigenius_honju", "spawn": {"natural": False}, "rarity": "rare", "tags": tags}


def hit(color="accent"):
    return [L("sparks", color, count=5, size=0.08, speed=0.5, spread=1.0, lifetime=6, forward=1.0, up=1.1),
            L("pulse", color, size=0.6, spread=0.6, lifetime=5, forward=1.0, up=1.1)]


def stomp(color="accent"):
    return [L("wave", color, "ring", 1, 0.4, 0.0, 3.5, 14, 1.0, 0.05),
            L("crack", "secondary", "line", 5, 0.3, 0.0, 3.0, 36, 1.0, 0.02, edge=color),
            L("smoke", "primary", size=0.5, speed=0.05, spread=1.4, lifetime=24, forward=1.0, up=0.2)]


def tail(color="accent"):
    return [L("band", "secondary", "arc", 1, 0.8, 0.0, 3.0, 10, -1.0, 0.6, edge=color)]


def trail():
    return [L("smoke", "primary", size=0.5, speed=0.03, spread=0.8, lifetime=18, forward=0, up=0.1)]


def polyonax():
    moves = {
        "polyonax_strike": melee(1.4, 10, 50, "action.punch", 2, 4.0),
        "polyonax_rush": charge(1.8, 140, "action.charge", 3, 6, 22),
        "polyonax_stomp": area(2.1, 4.5, 20, 160, "action.slam", 3, 4.0),
        "polyonax_sweep": sweep(1.5, 5.0, 14, 100, "action.tail_swipe", 2, arc=200, heavy=True),
    }
    data = honju(520.0, 18.0, 14.0, 0.30, 6.4, 5.0, 5.5, 3.5, moves, ["reptile", "quadruped"])
    vfx = palette("#526B43", "#2C3A24", "#D1C9A7", "#F4F0E2", "scales", {
        "polyonax_strike": hit(), "polyonax_rush": hit(), "polyonax_rush_trail": trail(),
        "polyonax_stomp": stomp(), "polyonax_sweep": tail()})
    ai_data = ai("aggressor", recovery_after_heavy_ticks=24, look_speed=10.0)
    return moves, data, ai_data, vfx


def lacerta():
    moves = {
        "lacerta_bite": melee(1.2, 8, 40, "action.bite", 2, 3.5),
        "lacerta_tail": sweep(1.3, 4.5, 10, 70, "action.tail_swipe", 2, arc=160),
        "lacerta_side_leap": leap(0.8, 120, "action.charge", 2, 5, 14),
        "lacerta_combo": flurry(0.55, 3.5, 3, 5, 90, "action.punch", 3, 3.5),
    }
    data = honju(310.0, 12.0, 12.0, 0.39, 5.8, 4.5, 8.0, 3.0, moves, ["reptile"])
    vfx = palette("#4F8B58", "#2A4A30", "#C4C45D", "#F4F2C8", "scales", {
        "lacerta_bite": hit(), "lacerta_tail": tail(), "lacerta_side_leap": stomp(),
        "lacerta_combo": [L("band", "accent", "arc", 1, 0.5, 0.0, 2.0, 8, 1.0, 1.2, tilt=30, edge="core")]})
    ai_data = ai("flanker", circle_chance=0.5, chase_speed=1.3)
    return moves, data, ai_data, vfx


def mantidis(green):
    prefix = "mantidis_green" if green else "mantidis"
    moves = {
        f"{prefix}_blades": flurry(0.9, 4.0, 2, 6, 60, "action.punch", 2, 4.0),
        f"{prefix}_leap": leap(1.4, 140, "action.charge", 3, 5, 16),
        f"{prefix}_cross_cut": sweep(1.8, 4.5, 14, 110, "action.slam", 3, arc=120, heavy=True),
    }
    data = honju(390.0, 15.0, 13.0, 0.44, 6.0, 4.0, 8.0, 1.0, moves, ["insect", "blade"])
    # Paleta do documento (verde-escuro e marfim); o preto usa o corpo escuro do modelo com o marfim de destaque.
    colors = ("#3E7551", "#1F3A28") if green else ("#26282B", "#121314")
    cut = [L("band", "accent", "arc", 1, 0.6, 0.0, 2.6, 9, 1.2, 1.4, tilt=-35, edge="core"),
           L("band", "accent", "arc", 1, 0.6, 0.0, 2.6, 9, 1.2, 1.4, tilt=35, edge="core", delay=2)]
    vfx = palette(*colors, "#D8D1A6", "#FFFBEA", "chitin", {
        f"{prefix}_blades": [L("band", "accent", "arc", 1, 0.5, 0.0, 2.2, 8, 1.0, 1.4, tilt=25, edge="core")],
        f"{prefix}_leap": stomp(), f"{prefix}_cross_cut": cut})
    # Aparo frontal parcial (documento): de frente leva metade; costas, atacando, guarda aberta e golpe pesado passam.
    ai_data = ai("flanker", circle_chance=0.6, chase_speed=1.35,
                 guard={"factor": 0.5, "front_arc_degrees": 120.0, "open_after_attack_ticks": 30})
    return moves, data, ai_data, vfx


def maxilla():
    moves = {
        "maxilla_horn": melee(1.8, 12, 60, "action.bite", 2, 4.5, heavy=True),
        "maxilla_charge": charge(2.3, 180, "action.charge", 3, 8, 28),
        "maxilla_stomp": area(2.0, 4.5, 20, 160, "action.slam", 3, 4.0),
        "maxilla_roar": area(1.0, 8.0, 18, 220, "action.roar", 2, 7.0, sound="kn8:kaiju.roar"),
    }
    data = honju(650.0, 21.0, 15.0, 0.27, 6.6, 3.5, 9.0, 4.0, moves, ["reptile", "horned"])
    vfx = palette("#383B3D", "#1C1E1F", "#C77A3B", "#FFE2C4", "scales", {
        "maxilla_horn": hit(), "maxilla_charge": hit(), "maxilla_charge_trail": trail(),
        "maxilla_stomp": stomp(),
        "maxilla_roar": [L("wave", "accent", "ring", 1, 0.5, 0.0, 6.0, 18, 0, 0.05),
                         L("pulse", "accent", size=1.6, spread=1.6, lifetime=8, forward=0.8, up=2.2)]})
    ai_data = ai("aggressor", recovery_after_heavy_ticks=28, look_speed=9.0)
    return moves, data, ai_data, vfx


def anguilla():
    moves = {
        "anguilla_bite": melee(1.4, 9, 45, "action.bite", 2, 3.5),
        "anguilla_whip": sweep(1.6, 5.5, 12, 80, "action.tail_swipe", 2, arc=200),
        "anguilla_discharge": area(1.8, 6.0, 18, 180, "action.roar", 3, 5.5, sound="kn8:kaiju.energy_beam",
                                   slow_ticks=30, slow_level=2),
        "anguilla_dive": leap(1.0, 160, "action.charge", 2, 6, 18),
    }
    data = honju(480.0, 16.0, 13.0, 0.40, 6.2, 5.0, 9.0, 1.5, moves, ["serpent", "aquatic"])
    vfx = palette("#214D70", "#10263A", "#57C9D8", "#E0FAFF", "scales", {
        "anguilla_bite": hit(), "anguilla_whip": tail(),
        "anguilla_discharge": [L("wave", "accent", "ring", 1, 0.4, 0.0, 5.5, 16, 0, 0.05),
                               L("bolt", "accent", "burst", 8, 0.08, 0.0, 3.0, 8, 0, 1.0),
                               L("pulse", "core", size=1.4, spread=1.4, lifetime=6, forward=0, up=1.6)],
        "anguilla_dive": stomp()})
    # Agua: o caminho pela agua custa pouco (serpente aquatica).
    ai_data = ai("flanker", circle_chance=0.4, chase_speed=1.3, terrain={"water_malus": 0.0})
    return moves, data, ai_data, vfx


def add_to_invasion(kaiju_id, invasion, chance):
    path = DATA / "invasion" / f"{invasion}.json"
    data = json.loads(path.read_text(encoding="utf-8"))
    wave = data["waves"][-1]["kaiju"]
    species = f"kn8:{kaiju_id}"
    if not any(entry["species"] == species for entry in wave):
        wave.append({"species": species, "count": 1, "chance": chance})
        write("invasion", invasion, data)


def main():
    builds = {"polyonax": polyonax, "lacerta_fratris": lacerta, "imitatio_mantidis": lambda: mantidis(False),
              "imitatio_mantidis_green": lambda: mantidis(True), "maxilla_cornuta": maxilla,
              "anguilla_ambulans": anguilla}
    for kaiju_id, build in builds.items():
        moves, data, ai_data, vfx = build()
        for name, ability in moves.items():
            write("ability", name, ability)
            vfx["effects"].setdefault(name, hit())
        write("kaiju", kaiju_id, data)
        write("kaiju_ai", kaiju_id, ai_data)
        write("locomotion", kaiju_id, {"stride": 3.5, "step_dust": 3})
        write("vfx_profile", kaiju_id, vfx)
        add_to_invasion(kaiju_id, *APPEARS[kaiju_id])
    print("Etapa 5: dados gravados")


if __name__ == "__main__":
    main()
