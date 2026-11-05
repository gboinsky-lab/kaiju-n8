#!/usr/bin/env python3
"""AJUSTES-2: os estouros do Narumi liam brancos (decisao de 2026-10-30, pendente desde a COMBATE-6/8).

Causa: o clarao (`flash`) e os pulsos extras do Numbers 1 usavam a cor de nucleo (#FFE6F2/#FFFFFF), e o brilho do
desenho soma as cores: de longe o rosa some no branco. Aqui o clarao e os pulsos brancos das tecnicas da baioneta
viram rosa (o miolo do raio `beam` continua claro, e o que da a linha fina) e o clarao fica 25% menor. Idempotente:
so mexe em camada que ainda esta branca. Perfis: narumi, narumi_no1 e as receitas do Narumi no perfil do jogador.
Uso: python3 tools/data/narumi_bursts.py
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
VFX = ROOT / "src/main/resources/data/kn8/kn8/vfx_profile"
TECHNIQUES = ("eruption_slash", "slash_salvo", "seven_branched_sword", "thunder_fire", "sky_turner", "bayonet_shot")
WHITE = {"core", "#FFFFFF", "#FFE6F2", "#ffffff", "#ffe6f2"}
FLASH_PINK = "#FF7AB8"
PULSE_PINK = "#FFB0D4"
FLASH_SCALE = 0.75


def fix(layers):
    changed = False
    for layer in layers:
        if layer.get("color") not in WHITE:
            continue
        if layer.get("shape") == "flash":
            layer["color"] = FLASH_PINK
            layer["size"] = round(layer.get("size", 1.0) * FLASH_SCALE, 3)
            changed = True
        elif layer.get("shape") == "pulse":
            layer["color"] = PULSE_PINK
            changed = True
    return changed


def main():
    for name in ("narumi", "narumi_no1", "player"):
        path = VFX / f"{name}.json"
        raw = path.read_bytes()
        data = json.loads(raw)
        changed = 0
        for key, layers in data["effects"].items():
            if key.split("/")[0].split("_explosion")[0].split("_trail")[0].split("_impact")[0] in TECHNIQUES:
                changed += fix(layers)
        text = json.dumps(data, indent=2, ensure_ascii=False) + "\n"
        if b"\r\n" in raw:
            text = text.replace("\n", "\r\n")
        path.write_bytes(text.encode("utf-8"))
        print(name, changed)


if __name__ == "__main__":
    main()
