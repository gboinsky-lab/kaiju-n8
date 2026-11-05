#!/usr/bin/env python3
"""AJUSTES-2 (Miguel 2026-11-05: "pode criar uma forma do n8 vermelha sem controle npc, uma pequena e uma gigante"):
dados do Kaiju No. 8 sem controle como kaiju inimigo, nas duas escalas. A arte e a do No. 8 (malha, ossos e
animacoes; KN8Entities.MODEL_ALIASES) com a textura vermelha do descontrole (kaiju_no8_rampage.png, a mesma do
jogador sem controle). Golpes = os do No. 8 aliado (socos, combo, investida, golpe no chao, rugido, soco de ruptura),
sem guarda nem contra-ataque: so ataca o mais perto.

Pequeno: 3 m (escala 1,5, como o No. 8 NPC). Gigante: 22 m (escala 11; "No. 8 colossal" da tabela mestra).
AJUSTES-3 (Miguel 2026-11-05: "numero 8 sem controle nao deve mudar muita coisa"): o pequeno tem os numeros do
No. 8 aliado (vida 1.800, armadura 19, velocidade 0,40 + 5%); o colossal, a vida do No. 10 gigante (4.500).
Balanceamento em docs/BALANCEAMENTO.md.
Uso: python3 tools/data/gen_no8_rampage.py
"""
import copy
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / "src/main/resources/data/kn8/kn8"

FORMS = {
    # id: (escala, vida, dano, armadura, velocidade, tamanho, prefixo das habilidades, fator de area)
    "kaiju_no8_rampage": (1.5, 1800.0, 44.0, 19.0, 0.42, "medium", "no8r", 1.0),
    "kaiju_no8_rampage_giant": (11.0, 4500.0, 66.0, 20.0, 0.30, "supergiant", "no8rg", 4.0),
}

RED = {"#18E3E3": "#E3241A", "#E8FFFF": "#FFE6E0", "#405C5B": "#5C3A38", "#3A4040": "#403030",
       "#18e3e3": "#E3241A", "#e8ffff": "#FFE6E0"}


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def abilities(prefix, area):
    """Golpes do No. 8 em escala de kaiju (os do gigante com alcance e area maiores)."""
    def scaled(value):
        return round(value * area, 1)

    return {
        f"{prefix}_rush_combo": {
            "type": "kn8:multi_hit", "damage_multiplier": 0.55, "radius": scaled(1.0), "windup_ticks": 5,
            "active_ticks": 12, "cooldown_ticks": 60, "animation": "action.rush_combo",
            "vfx": [{"effect": "kn8:impact", "intensity": 0.7}], "camera_shake": 0.15,
            "sound": "kn8:technique.no8_punch",
            "behavior": {"priority": 3, "max_range": scaled(3.0), "hits": 4, "hit_interval": 3}},
        f"{prefix}_heavy_punch": {
            "type": "kn8:melee", "damage_multiplier": 1.8, "windup_ticks": 10, "active_ticks": 2,
            "cooldown_ticks": 80, "animation": "action.heavy_punch", "heavy": True,
            "destruction": {"radius": scaled(1.6), "power": 3, "crater": False, "depth": 0.0},
            "vfx": [{"effect": "kn8:impact", "intensity": 1.0}, {"effect": "kn8:shockwave", "intensity": 0.6}],
            "camera_shake": 0.3, "sound": "kn8:technique.no8_punch",
            "behavior": {"priority": 2, "max_range": scaled(3.5)}},
        f"{prefix}_dash_strike": {
            "type": "kn8:charge", "damage_multiplier": 1.3, "radius": scaled(1.3), "windup_ticks": 8,
            "active_ticks": 16, "cooldown_ticks": 90, "animation": "action.dash_strike", "telegraph": "line",
            "heavy": True, "destruction": {"radius": scaled(1.2), "power": 3, "crater": False},
            "vfx": [{"effect": "kn8:dust", "intensity": 0.6}, {"effect": "kn8:impact", "intensity": 0.8}],
            "camera_shake": 0.25, "sound": "kn8:kaiju.punch",
            "behavior": {"priority": 4, "min_range": scaled(5.0), "max_range": scaled(22.0)}},
        f"{prefix}_ground_smash": {
            "type": "kn8:area_melee", "damage_multiplier": 1.6, "radius": scaled(3.5), "windup_ticks": 18,
            "active_ticks": 4, "cooldown_ticks": 140, "animation": "action.ground_smash",
            "telegraph": "ground_circle", "heavy": True,
            "destruction": {"radius": scaled(3.5), "power": 3, "crater": True, "depth": 1.5},
            "vfx": [{"effect": "kn8:shockwave", "intensity": 1.2}, {"effect": "kn8:dust", "intensity": 1.0}],
            "camera_shake": 0.45, "sound": "kn8:kaiju.slam",
            "behavior": {"priority": 3, "max_range": scaled(3.0)}},
        f"{prefix}_rupture_punch": {
            "type": "kn8:melee", "damage_multiplier": 3.0, "windup_ticks": 14, "active_ticks": 2,
            "cooldown_ticks": 200, "animation": "action.rupture_punch", "heavy": True,
            "destruction": {"radius": scaled(2.5), "power": 4, "crater": True, "depth": 1.0},
            "vfx": [{"effect": "kn8:impact", "intensity": 1.4}, {"effect": "kn8:shockwave", "intensity": 1.2}],
            "camera_shake": 0.6, "sound": "kn8:technique.no8_rupture",
            "behavior": {"priority": 5, "max_range": scaled(3.5), "health_below": 0.6}},
        f"{prefix}_roar": {
            "type": "kn8:area_melee", "damage_multiplier": 0.3, "radius": scaled(6.0), "windup_ticks": 12,
            "active_ticks": 4, "cooldown_ticks": 240, "animation": "action.roar",
            "vfx": [{"effect": "kn8:roar", "intensity": 1.2}], "camera_shake": 0.35, "sound": "kn8:kaiju.roar",
            "behavior": {"priority": 1, "max_range": scaled(6.0), "slow_ticks": 40, "slow_level": 1}},
    }


def main():
    no8_vfx = json.loads((DATA / "vfx_profile/kaiju_no8.json").read_text(encoding="utf-8"))
    text = json.dumps(no8_vfx)
    for cyan, red in RED.items():
        text = text.replace(cyan, red)
    red_vfx = json.loads(text)
    for kaiju_id, (scale, health, damage, armor, speed, size, prefix, area) in FORMS.items():
        moves = abilities(prefix, area)
        for name, data in moves.items():
            write(DATA / "ability" / f"{name}.json", data)
        write(DATA / "kaiju" / f"{kaiju_id}.json", {
            "class": "numbered", "fortitude": 9.0 if scale < 2 else 9.6, "size": size,
            "dimensions": {"width": 0.9, "height": 2.0, "scale": scale},
            "overrides": {"health": health, "damage": damage, "armor": armor},
            "speed": speed, "intelligence": 2,
            "parts": [
                {"name": "head", "offset": [0.0, 1.62, 0.0], "width": 0.5, "height": 0.4, "multiplier": 1.3},
                {"name": "torso", "offset": [0.0, 0.95, 0.0], "width": 0.8, "height": 0.7, "multiplier": 1.0},
                {"name": "core", "offset": [0.0, 1.25, 0.18], "width": 0.25, "height": 0.25, "multiplier": 3.0,
                 "core": True},
                {"name": "legs", "offset": [0.0, 0.0, 0.0], "width": 0.7, "height": 0.95, "multiplier": 0.8}],
            "core": {"health_fraction": 0.25, "exposed_on": ["heavy_hit"]},
            "abilities": [f"kn8:{name}" for name in moves],
            "weaknesses": [], "spawn": {"natural": False}, "rarity": "event",
            "tags": ["humanoid", "numbered", "rampage"] + (["giant"] if scale > 2 else []),
            "rage": {"health_below": 0.35, "damage_multiplier": 1.2, "speed_multiplier": 1.15,
                     "cooldown_multiplier": 0.7}})
        # Sem controle: so avanca e bate (nao circula, nao recua), mais rapido atras do alvo.
        write(DATA / "kaiju_ai" / f"{kaiju_id}.json", {"chase_speed": 1.3, "group": {"role": "aggressor"}})
        write(DATA / "locomotion" / f"{kaiju_id}.json", {"stride": round(2.0 * scale / 1.5, 1)})
        profile = copy.deepcopy(red_vfx)
        # As receitas das tecnicas do No. 8 aliado viram as das habilidades (mesmo golpe, em vermelho).
        for technique in ("rush_combo", "heavy_punch", "dash_strike", "ground_smash", "rupture_punch", "roar"):
            if technique in profile["effects"]:
                profile["effects"][f"{prefix}_{technique}"] = profile["effects"][technique]
        write(DATA / "vfx_profile" / f"{kaiju_id}.json", profile)
    print("ok:", ", ".join(FORMS))


if __name__ == "__main__":
    main()
