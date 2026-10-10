#!/usr/bin/env python3
"""Gera as fichas de habilidades e atributos (balanceamento v1.2 aplicado) direto dos JSON do datapack, para o
documento nunca ficar diferente do jogo. Saida: markdown no stdout.
Uso: python3 tools/docs/gen_fichas_habilidades.py > docs/FICHAS_HABILIDADES.md
"""
import json
from pathlib import Path

DATA = Path(__file__).resolve().parents[2] / "src/main/resources/data/kn8/kn8"
TICK = 20.0

# Origem de cada habilidade (Biblioteca de habilidades dos kaiju, secao 1.3): CANON, ADAPTACAO, ORIGINAL, PENDENTE.
ORIGIN = {
    "finger_gun": "ADAPTAÇÃO (No. 9 dispara pelos dedos)",
    "no10_finger_cannon": "ADAPTAÇÃO", "no10g_finger_cannon": "ADAPTAÇÃO",
}
SPECIES_ORIGIN = {
    "kaiju_no9_black": "MOD ORIGINAL (forma preta inicial: v1.2 §5)",
    "kaiju_no9_fusion": "MOD ORIGINAL (fusão: v1.2 §6)",
    "kaiju_no9_camponotus": "MOD ORIGINAL (fusão com formiga: v1.2 §7)",
    "philinosoma": "MOD ORIGINAL (espécie do Miguel)", "diclonius": "MOD ORIGINAL (espécie do Miguel)",
    "camponotus": "MOD ORIGINAL (espécie do Miguel)", "camponotus_reborn": "MOD ORIGINAL (espécie do Miguel)",
    "phaneroplasmodium": "MOD ORIGINAL (espécie do Miguel)", "myxogasterocarp": "MOD ORIGINAL (espécie do Miguel)",
    "kaiju_larva": "ADAPTAÇÃO (larva que origina o No. 8)",
}
TYPES = {"melee": "corpo a corpo", "area_melee": "área", "charge": "investida", "sweep": "varredura",
         "projectile": "projétil", "leap": "salto", "multi_hit": "rajada de golpes"}


def load(folder, name):
    return json.loads((DATA / folder / f"{name}.json").read_text(encoding="utf-8"))


def stats(d):
    f = d.get("fortitude", 0)
    o = d.get("overrides", {})
    return (o.get("health", 20 * 2 ** (f - 2)), o.get("damage", 2 * 1.6 ** (f - 2)), o.get("armor", min(20, 2 * f)),
            d.get("speed"))


def ability_row(aid, base_damage):
    a = load("ability", aid)
    b = a.get("behavior", {})
    kind = TYPES.get(a["type"].split(":")[1], a["type"])
    hits = b.get("hits", 1)
    mult = a["damage_multiplier"]
    dmg = base_damage * mult
    dmg_txt = f"{dmg:.0f}" + (f" × {hits}" if hits > 1 else "")
    rng = []
    if b.get("max_range", -1) > 0:
        rng.append(f"{b.get('min_range', 0):g}–{b.get('max_range', 0):g} blocos")
    elif b.get("min_range"):
        rng.append(f"a partir de {b['min_range']:g} blocos")
    if a.get("radius"):
        rng.append(f"raio {a['radius']:g}")
    if b.get("explosion_radius"):
        rng.append(f"explode {b['explosion_radius']:g}")
    extra = []
    if b.get("slow_ticks"):
        extra.append(f"lentidão {b.get('slow_level', 1)} por {b['slow_ticks'] / TICK:g} s")
    if b.get("health_below", 1) < 1:
        extra.append(f"só abaixo de {b['health_below'] * 100:.0f}%")
    if a.get("heavy"):
        extra.append("pesado (fura bloqueio)")
    return (f"| `{aid}` | {kind} | ×{mult:g} = {dmg_txt} | {a['windup_ticks'] / TICK:.2f} s | "
            f"{a['cooldown_ticks'] / TICK:g} s | {', '.join(rng) or 'alcance do corpo'} | {', '.join(extra) or '—'} | "
            f"{ORIGIN.get(aid, 'ADAPTAÇÃO DO MOD')} |")


def kaiju_section(name):
    d = load("kaiju", name)
    hp, dmg, armor, speed = stats(d)
    out = [f"### `{name}` — {d['class']}", "",
           f"Vida **{hp:,.0f}** · dano base **{dmg:.1f}** · armadura **{armor:g}** · velocidade **{speed}**"
           .replace(",", ".")]
    if name in SPECIES_ORIGIN:
        out.append(f" · origem: {SPECIES_ORIGIN[name]}")
    rage = d.get("rage")
    if rage:
        out.append(f"\n\nFúria abaixo de {rage['health_below'] * 100:.0f}%: dano ×{rage['damage_multiplier']:g}, "
                   f"velocidade ×{rage['speed_multiplier']:g}, recargas ×{rage['cooldown_multiplier']:g}.")
    num = DATA / "numbered" / f"{name}.json"
    if num.exists():
        n = json.loads(num.read_text(encoding="utf-8"))
        rg = n.get("regeneration", {})
        bits = [f"regenera {rg.get('per_second', 0) * 100:g}%/s abaixo de {rg.get('below', 0) * 100:.0f}%"
                + (f" ({rg['fast_per_second'] * 100:g}%/s abaixo de {rg['fast_below'] * 100:.0f}%)"
                   if rg.get('fast_per_second') else "")]
        if n.get("flee_health"):
            bits.append(f"foge com {n['flee_health'] * 100:.0f}%")
        if n.get("command_radius"):
            bits.append(f"comanda kaiju a {n['command_radius']:g} blocos")
        if n.get("revive"):
            bits.append(f"revive até {n.get('max_revived_alive', 3)}")
        if n.get("hardening"):
            h = n["hardening"]
            bits.append(f"pele endurecida ({h['chance'] * 100:.0f}% por golpe, −{h['reduction'] * 100:.0f}% por "
                        f"{h['duration_ticks'] / TICK:g} s)")
        if n.get("adaptation"):
            a = n["adaptation"]
            bits.append(f"análise (−{a['per_hit'] * 100:g}% por golpe repetido do mesmo tipo, até "
                        f"−{a['max_reduction'] * 100:.0f}%)")
        if n.get("transform"):
            t = n["transform"]
            bits.append(f"vira `{t['into'].split(':')[1]}` abaixo de {t.get('health_below', 0.5) * 100:.0f}%")
        for rule in n.get("absorb", []):
            bits.append(f"absorve {', '.join(s.split(':')[1] for s in rule['species'])} → "
                        f"`{rule['into'].split(':')[1]}`")
        out.append("\n\nNumerado: " + "; ".join(bits) + ".")
    out += ["", "| Habilidade | Tipo | Dano | Preparo | Recarga | Alcance | Efeito | Origem |",
            "|---|---|---|---|---|---|---|---|"]
    out += [ability_row(a.split(":")[1], dmg) for a in d.get("abilities", [])]
    return "\n".join(out) + "\n"


def special_section(name):
    d = load("special_soldier", name)
    out = [f"### `{name}`", "",
           f"Vida **{d['health']}** · armadura **{d['armor']}** · velocidade **{d['speed']}** · Release "
           f"{d.get('release', 0)}% (máx. {d.get('max_release', 0)}%) · dano contra kaiju ×{d['kaiju_damage']} · "
           f"arma `{d['weapon'].split(':')[1]}`", "",
           "Dano de cada golpe = dano da arma × multiplicador × Release × `kaiju_damage` (contra kaiju).", ""]
    defense = []
    if d.get("dash"):
        defense.append(f"esquiva a cada {d['dash']['cooldown_ticks'] / TICK:g} s")
    if d.get("counter"):
        c = d["counter"]
        defense.append(f"contra-ataque ×{c['multiplier']:g} a cada {c['cooldown_ticks'] / TICK:g} s")
    if d.get("parry"):
        defense.append(f"apara {d['parry']['chance'] * 100:g}% dos golpes (dano ×{d['parry']['damage_factor']:g})")
    if d.get("regeneration"):
        r = d["regeneration"]
        defense.append(f"regenera {r['per_second'] * 100:g}%/s abaixo de {r['health_below'] * 100:g}%")
    if d.get("transform"):
        defense.append(f"vira `{d['transform']['into'].split(':')[1]}`"
                       + (f" abaixo de {d['transform']['health_below'] * 100:g}%" if d['transform'].get('health_below')
                          else " sem alvo"))
    if defense:
        out += ["Defesa: " + "; ".join(defense) + ".", ""]
    out += ["| Técnica | Tipo | Multiplicadores por golpe | Recarga | Alcance | Observação |",
            "|---|---|---|---|---|---|"]
    for tid, t in d.get("techniques", {}).items():
        hits = t.get("hits", [])
        mult = " + ".join(f"{h:g}" for h in hits) if hits else "—"
        rng = f"{t.get('min_range', 0):g}–{t.get('max_range', 0):g}" if t.get("max_range") else "—"
        notes = []
        if t.get("slash"):
            sl = t["slash"]
            notes.append(("tiro" if sl.get("bullet") else "corte a distância") + f" ×{sl.get('count', 1)}"
                         + (f", explode {sl['explosion_radius']:g}" if sl.get("explosion_radius") else "")
                         + (f", lentidão {sl.get('slow_level', 1)}" if sl.get("slow_ticks") else ""))
        if t.get("expose_core_ticks"):
            notes.append(f"expõe o núcleo {t['expose_core_ticks'] / TICK:g} s")
        if t.get("requires_full_release"):
            notes.append("só com Full Release")
        if t.get("honju_priority"):
            notes.append("prefere Honju")
        if t.get("dash_in"):
            notes.append("avança até o alvo")
        out.append(f"| `{tid}` | {t.get('type')} | ×({mult}) | {t.get('cooldown_ticks', 0) / TICK:g} s | {rng} | "
                   f"{', '.join(notes) or '—'} |")
    return "\n".join(out) + "\n"


def main():
    print("<!-- gerado por tools/docs/gen_fichas_habilidades.py a partir dos JSON; não editar à mão -->\n")
    print("## Kaiju\n")
    for p in sorted((DATA / "kaiju").glob("*.json")):
        print(kaiju_section(p.stem))
    print("## Personagens (soldados especiais)\n")
    for p in sorted((DATA / "special_soldier").glob("*.json")):
        print(special_section(p.stem))


if __name__ == "__main__":
    main()
