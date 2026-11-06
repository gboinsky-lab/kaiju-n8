"""Etapa 7-B (Miguel, 2026-11-06, respostas da docs/ETAPA7_AJUSTES.md): minigun da Rin com superaquecimento e
supressao, marcacao do Narumi, analise da Konomi, marca do Ryo, veneno do Haruichi e escudo da Tae.

Grava a arma kn8:rin_minigun (modelo do Miguel, meshy_src/arma_rin_shinonome.glb, convertido pelo
meshy_convert.py), o cinto kn8:minigun_belt, o perfil kn8:minigun, receitas, patente, e os blocos on_hit/rally dos
perfis. Numeros [SUPOSICAO] pela tabela da docs/ETAPA7_AJUSTES.md.

Uso: python3 tools/data/gen_etapa7b.py (rodar depois do gen_especiais1/2: muda os perfis que eles gravam).
"""
import copy
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / "src/main/resources/data/kn8/kn8"
TAGS = ROOT / "src/main/resources/data/kn8/tags"
ASSETS = ROOT / "src/main/resources/assets/kn8"
sys.path.insert(0, str(Path(__file__).resolve().parent))
from gen_especiais1 import L, muzzle, write  # noqa: E402


def edit(folder, name, change):
    """Le, muda e grava mantendo o fim de linha do arquivo."""
    path = DATA / folder / f"{name}.json"
    raw = path.read_bytes().decode("utf-8")
    newline = "\r\n" if "\r\n" in raw else "\n"
    data = json.loads(raw)
    change(data)
    text = json.dumps(data, indent=2, ensure_ascii=False) + "\n"
    path.write_bytes(text.replace("\n", newline).encode("utf-8"))


# Supressao: Lentidao I (-15% de velocidade) por 1,5 s, renovada a cada tiro.
SUPPRESS = {"slow_ticks": 30, "slow_level": 1}
PINK = 0xD98AB8


def minigun():
    write("weapon", "rin_minigun", {
        "item": "kn8:rin_minigun", "base_damage": 6, "reach": 40.0, "style": "firearm", "profile": "kn8:minigun",
        "actions": {"light": {"multiplier": 0.4, "duration_ticks": 2, "impact_tick": 1}},
        "sounds": {"shot": "kn8:weapon.rifle_shot"},
        "on_hit": SUPPRESS,
        # 5 s atirando sem parar superaquece; 3 s esfriando.
        "automatic": {"overheat_ticks": 100, "cooldown_ticks": 60, "cool_per_tick": 1.0}})
    profile = json.loads((DATA / "weapon_profile/rifle.json").read_text(encoding="utf-8"))
    profile["reload"]["magazine"] = 120
    profile["reload"]["magazine_item"] = "kn8:minigun_belt"
    profile["reload"]["stages"][1]["ticks"] = 30
    profile["recoil"] = {"pitch": 0.35, "yaw": 0.25, "recover_ticks": 2, "body_kick": 0.3}
    profile["npc"]["spare_magazines"] = 3
    write("weapon_profile", "minigun", profile)
    write("workbench", "rin_minigun", {"result": "kn8:rin_minigun", "category": "weapon", "order": 21,
                                       "ingredients": [{"item": "minecraft:iron_ingot", "count": 14},
                                                       {"item": "kn8:muscle_fiber", "count": 6},
                                                       {"item": "kn8:core_fragment", "count": 3},
                                                       {"item": "minecraft:redstone", "count": 6}]})
    write("workbench", "minigun_belt", {"result": "kn8:minigun_belt", "category": "weapon", "order": 22,
                                        "ingredients": [{"item": "minecraft:iron_ingot", "count": 6},
                                                        {"item": "minecraft:redstone", "count": 2}]})
    edit("rank", "vice_capitao", lambda d: [d["unlocks"].append(i) for i in ("kn8:rin_minigun", "kn8:minigun_belt")
                                             if i not in d["unlocks"]])
    tag = TAGS / "item/real_size_weapons.json"
    data = json.loads(tag.read_text(encoding="utf-8"))
    if "kn8:rin_minigun" not in data["values"]:
        data["values"].append("kn8:rin_minigun")
    tag.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
    model = ASSETS / "models/item/minigun_belt.json"
    model.write_text(json.dumps({"parent": "minecraft:item/generated",
                                 "textures": {"layer0": "kn8:item/minigun_belt"}}, indent=2) + "\n",
                     encoding="utf-8")
    # Cinto: o pente do rifle tingido de laranja-latao (municao em fita).
    from PIL import Image
    src = Image.open(ASSETS / "textures/item/rifle_magazine.png").convert("RGBA")
    out = Image.new("RGBA", src.size)
    for x in range(src.width):
        for y in range(src.height):
            r, g, b, a = src.getpixel((x, y))
            lum = (r + g + b) / 3 / 255
            out.putpixel((x, y), (int(60 + 190 * lum), int(40 + 140 * lum), int(20 + 50 * lum), a))
    out.save(ASSETS / "textures/item/minigun_belt.png")
    # Animacao do jogador: o tiro da espingarda (perfil do rifle).
    path = ASSETS / "player_animations/combat.json"
    raw = path.read_bytes().decode("utf-8")
    newline = "\r\n" if "\r\n" in raw else "\n"
    anims = json.loads(raw)
    anims_root = anims["animations"]
    if "player.rin_minigun.shoot" not in anims_root:
        anims_root["player.rin_minigun.shoot"] = copy.deepcopy(anims_root["player.shotgun.shoot"])
        text = json.dumps(anims, indent=2, ensure_ascii=False) + "\n"
        path.write_bytes(text.replace("\n", newline).encode("utf-8"))


def rin():
    def change(d):
        d["weapon"] = "kn8:rin_minigun"
        # Rajada da minigun: 8 x 0,25 (tabela do dossie), um tiro a cada 3 ticks; a recarga e o superaquecimento.
        d["techniques"]["minigun_barrage"] = {
            "type": "slash", "hits": [0.25] * 8, "hit_interval": 3, "windup_ticks": 6, "duration_ticks": 32,
            "cooldown_ticks": 100, "min_range": 3.0, "max_range": 30.0, "priority": 2,
            "slash": {"count": 1, "speed": 4.0, "range": 40.0, "width": 0.25, "color": PINK, "bullet": True}}
        on_hit = d.setdefault("on_hit", {})
        for tech in ("minigun_barrage", "thorough_sweep_fire", "circular_barrage"):
            on_hit[tech] = dict(SUPPRESS)
    edit("special_soldier", "rin", change)

    def vfx(d):
        d["effects"]["minigun_barrage"] = muzzle("accent")
        d["effects"]["minigun_barrage_impact"] = [L("sparks", "accent", "burst", 3, 0.05, 0.4, 0.5, 4)]
    edit("vfx_profile", "rin", vfx)
    edit("sound_profile", "rin", lambda d: d["techniques"].__setitem__(
        "minigun_barrage", {"release": "kn8:weapon.rifle_shot"}))


def marks():
    # Narumi: marcacao +15% para todos por 5 s (tiro da baioneta e, no Numbers 1, a estocada no ponto fraco).
    mark = {"mark_ticks": 100, "mark_bonus": 0.15}
    edit("special_soldier", "narumi", lambda d: d.setdefault("on_hit", {}).__setitem__("bayonet_shot", dict(mark)))

    def no1(d):
        on_hit = d.setdefault("on_hit", {})
        for tech in ("bayonet_shot", "weak_point_thrust"):
            on_hit[tech] = dict(mark)
    edit("special_soldier", "narumi_no1", no1)
    # Konomi: analise pelo radar, +8% por 8 s (renovada pela KonomiEntity).
    edit("special_soldier", "konomi", lambda d: d.setdefault("on_hit", {}).__setitem__(
        "analysis", {"mark_ticks": 160, "mark_bonus": 0.08}))
    # Ryo: o Tiro Apaixonado marca o kaiju: +20% no proximo golpe de qualquer um (vale 10 s).
    edit("special_soldier", "ryo", lambda d: d.setdefault("on_hit", {}).__setitem__(
        "passionate_shot", {"next_hit_bonus": 0.2, "next_hit_ticks": 200}))


def poison():
    # Haruichi: flechas envenenadas, veneno de 4 s (6/s nos kaiju).
    venom = {"poison_ticks": 80, "poison_per_second": 6.0}

    def change(d):
        on_hit = d.setdefault("on_hit", {})
        for tech in ("precision_snipe", "manifold_arrow_salvo"):
            on_hit[tech] = dict(venom)
    edit("special_soldier", "haruichi", change)
    edit("weapon", "haruichi_bow", lambda d: d.__setitem__("on_hit", {"poison_ticks": 80, "poison_per_second": 3.0}))


def shield():
    # Tae: a Granada de Calor vira apoio da equipe: cura aos poucos e escudo de 8% da vida (Absorcao) a 10 blocos.
    edit("special_soldier", "tae", lambda d: d["rally"].__setitem__(
        "heat_grenade", {"radius": 10.0, "duration_ticks": 160, "strength": 1, "regeneration": 1, "shield": 0.08}))


if __name__ == "__main__":
    minigun()
    rin()
    marks()
    poison()
    shield()
    print("Etapa 7-B: minigun, marcas, veneno e escudo gravados")
