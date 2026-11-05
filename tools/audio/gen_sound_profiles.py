#!/usr/bin/env python3
"""SONS: liga os sons do gen_sounds_sons.py aos dados, sem Java.

- sound_profile/<perfil>.json: som de preparo (windup) e de golpe/disparo (release) de cada tecnica dos soldados
  especiais, pela assinatura de cada personagem (docs/audio/SONS_FEITOS.md);
- sound_profile/kaiju.json: preparo e saida do tiro das habilidades dos kaiju, pelo nome da habilidade;
- "sound" das habilidades (impacto), "sounds" das armas dos punhos e do canhao e o "sound" de cada tecnica das armas
  do jogador.
Regras por palavra no nome (a primeira que bate vale); rodar de novo depois de criar tecnica ou habilidade.
Uso: python3 tools/audio/gen_sound_profiles.py
"""
import glob
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / "src/main/resources/data/kn8/kn8"

CHARGE = "kn8:technique.charge"


def cue(windup=None, release=None, volume=None):
    out = {}
    if windup:
        out["windup"] = windup
    if release:
        out["release"] = release
    if volume:
        out["volume"] = volume
    return out


# perfil -> lista de (palavras no nome, tipo da tecnica ou None, cue); a primeira que bate vale.
def hoshina():
    return [(("kuuchi", "kosa_uchi"), None, cue("kn8:technique.hoshina_slash", "kn8:technique.hoshina_release")),
            (("juni_hitoe", "yae_uchi"), None, cue(CHARGE, "kn8:technique.hoshina_slash")),
            ((), None, cue(None, "kn8:technique.hoshina_slash"))]


def kikoru():
    return [(("falling_thunder", "shockwave", "ground_smash", "guard_break", "dive_strike", "vertical"), None,
             cue(CHARGE, "kn8:technique.kikoru_thunder")),
            (("half_moon", "heavy_swing"), None, cue(None, "kn8:technique.kikoru_thunder")),
            ((), None, cue(None, "kn8:technique.kikoru_swing"))]


def narumi():
    return [(("bayonet_shot",), None, cue(None, "kn8:technique.narumi_shot")),
            (("thunder_fire", "flame_shower"), None, cue(CHARGE, "kn8:technique.narumi_blast")),
            (("seven_branched",), None, cue(CHARGE, "kn8:technique.narumi_thrust")),
            ((), None, cue(None, "kn8:technique.narumi_thrust"))]


PROFILES = {
    "hoshina": hoshina(),
    "hoshina_no10": hoshina(),
    "kikoru": kikoru(),
    "kikoru_no4": kikoru(),
    "narumi": narumi(),
    "narumi_no1": narumi(),
    "isao": [(("thunder_emperor",), None, cue(CHARGE, "kn8:technique.isao_punch")),
             ((), "slash", cue(None, "kn8:technique.isao_blast")),
             ((), None, cue(None, "kn8:technique.isao_punch"))],
    "isao_base": [((), None, cue(None, "kn8:weapon.fist_swing"))],
    "kafka": [((), None, cue(None, "kn8:weapon.fist_swing"))],
    "kaiju_no8": [(("rupture_punch",), None, cue(CHARGE, "kn8:technique.no8_rupture", 1.5)),
                  (("ground_smash",), None, cue(None, "kn8:kaiju.slam", 1.5)),
                  (("roar",), None, cue(None, "kn8:kaiju.roar", 1.5)),
                  ((), None, cue(None, "kn8:technique.no8_punch"))],
    "mina": [(("precision_shot",), None, cue(None, "kn8:weapon.rifle_shot")),
             ((), None, cue("kn8:technique.mina_charge", "kn8:weapon.cannon_shot", 2.0))],
    "reno": [(("butt_strike",), None, cue(None, "kn8:weapon.fist_swing")),
             ((), None, cue(None, "kn8:technique.reno_shot"))],
    "reno_no6": [(("freeze_burst",), None, cue(CHARGE, "kn8:technique.ice_shatter")),
                 (("ice_field",), None, cue(CHARGE, "kn8:technique.ice_freeze")),
                 (("ice_spear",), None, cue(CHARGE, "kn8:technique.reno_shot")),
                 ((), None, cue(None, "kn8:technique.reno_shot"))],
}

# Habilidades de kaiju: (palavras, windup, release na saida do tiro, impacto). release so vale para projetil.
ABILITIES = [
    (("finger_gun", "finger_cannon"), None, "kn8:kaiju.finger_gun", "kn8:weapon.fist_hit"),
    (("energy_blast", "energy_beam", "atomic_breath", "hybrid_beam"), "kn8:kaiju.energy_charge",
     "kn8:kaiju.energy_beam", "kn8:kaiju.slam"),
    (("spore_bomb",), "kn8:kaiju.energy_charge", "kn8:kaiju.spore", "kn8:kaiju.slam"),
    (("acid",), None, "kn8:kaiju.acid_spray", "kn8:kaiju.acid_spray"),
    (("web",), None, "kn8:kaiju.web_shot", "kn8:kaiju.web_shot"),
    (("spine_shot",), None, "kn8:kaiju.spine_shot", "kn8:weapon.knife_hit"),
    (("spore",), None, "kn8:kaiju.spore", "kn8:kaiju.spore"),
    (("throat_grab", "tendril"), None, "kn8:kaiju.grab", "kn8:kaiju.grab"),
    (("sonic_blast", "force_blast"), None, "kn8:technique.isao_blast", "kn8:technique.isao_blast"),
    (("claw", "leg_stab", "leg_swipe", "multi_leg", "tail_stab", "multi_appendage", "blade_arm", "mandible",
      "tail_strike"), None, None, "kn8:kaiju.claw"),
    (("thunder_emperor", "barrage_blast", "heavy_punch", "flurry", "sprint", "rush", "charge"), None, None,
     "kn8:kaiju.punch"),
    (("tail_swipe", "tail_sweep", "tail_smash", "tail_barrage"), None, None, "kn8:kaiju.tail_swipe"),
    (("hooved",), None, None, "kn8:kaiju.punch"),
]

# Armas: chave "sounds" e "sound" por tecnica (mesmas regras dos perfis, pelo nome da tecnica).
WEAPON_SOUNDS = {
    "kaiju_no8_fist": {"swing": "kn8:weapon.fist_swing", "heavy": "kn8:technique.no8_punch",
                       "hit": "kn8:weapon.fist_hit"},
    "isao_fist": {"swing": "kn8:weapon.fist_swing", "heavy": "kn8:weapon.fist_swing", "hit": "kn8:weapon.fist_hit"},
    "numbers2_gauntlet": {"swing": "kn8:weapon.fist_swing", "heavy": "kn8:technique.isao_punch",
                          "hit": "kn8:weapon.fist_hit"},
    "numbers2_gauntlets": {"swing": "kn8:weapon.fist_swing", "heavy": "kn8:technique.isao_punch",
                           "hit": "kn8:weapon.fist_hit"},
    "mina_cannon": {"shot": "kn8:weapon.cannon_shot"},
}
WEAPON_TECHNIQUE_RULES = {
    "hoshina_sword": "hoshina", "axe": "kikoru", "numbers4_axe": "kikoru", "narumi_bayonet": "narumi",
    "numbers1_bayonet": "narumi", "numbers2_gauntlets": "isao", "kaiju_no8_fist": "kaiju_no8",
    "numbers6_rifle": "reno_no6",
}
# Especiais do jogador que ja tem som proprio de antes (Golpe Sismico, Mergulho do Ceu).
KEEP = {"seismic_cleave", "numbers4_sky_dive"}
WEAPON_SPECIAL = {"combat_knife": "kn8:weapon.knife_flurry", "sword": "kn8:weapon.dash_cut"}


def pick(rules, name, kind):
    for words, need, value in rules:
        if need and need != kind:
            continue
        if not words or any(word in name for word in words):
            return value
    return {}


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    # Mantem o fim de linha do arquivo (alguns JSON do projeto sao CRLF).
    crlf = path.exists() and b"\r\n" in path.read_bytes()
    text = json.dumps(data, indent=2, ensure_ascii=False) + "\n"
    path.write_bytes((text.replace("\n", "\r\n") if crlf else text).encode("utf-8"))


def main():
    for profile, rules in PROFILES.items():
        spec = json.loads((DATA / "special_soldier" / f"{profile}.json").read_text(encoding="utf-8"))
        techniques = {}
        for name, technique in spec.get("techniques", {}).items():
            value = pick(rules, name, technique.get("type"))
            if value:
                techniques[name] = value
        write(DATA / "sound_profile" / f"{profile}.json", {"techniques": techniques})
    abilities = {}
    for path in sorted(glob.glob(str(DATA / "ability" / "*.json"))):
        name = Path(path).stem
        data = json.loads(Path(path).read_text(encoding="utf-8"))
        for words, windup, release, impact in ABILITIES:
            if any(word in name for word in words):
                entry = cue(windup, release if data["type"] == "kn8:projectile" else None)
                if entry:
                    abilities[name] = entry
                if impact and data.get("sound") != impact:
                    data["sound"] = impact
                    write(Path(path), data)
                break
    write(DATA / "sound_profile" / "kaiju.json", {"abilities": abilities})
    for path in sorted(glob.glob(str(DATA / "weapon" / "*.json"))):
        name = Path(path).stem
        data = json.loads(Path(path).read_text(encoding="utf-8"))
        before = json.dumps(data)
        if name in WEAPON_SOUNDS:
            data.setdefault("sounds", {}).update(WEAPON_SOUNDS[name])
        if name in WEAPON_SPECIAL and "special" in data:
            data["special"]["sound"] = WEAPON_SPECIAL[name]
        rules = PROFILES.get(WEAPON_TECHNIQUE_RULES.get(name, ""))
        if rules:
            for technique in ([data["special"]] if "special" in data else []) + data.get("techniques", []):
                if technique["id"] in KEEP:
                    continue
                value = pick(rules, technique["id"], "slash" if technique.get("type") == "slash_wave" else None)
                sound = value.get("release")
                if sound:
                    technique["sound"] = sound
        if json.dumps(data) != before:
            write(Path(path), data)
    print("perfis:", len(PROFILES) + 1, "habilidades com preparo/saida:", len(abilities))


if __name__ == "__main__":
    main()
