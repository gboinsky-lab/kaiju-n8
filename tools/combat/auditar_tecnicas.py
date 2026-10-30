"""COMBATE-8: auditoria das tecnicas (jogador e NPC).

Para cada tecnica confere o que o jogo precisa para ela aparecer direito:
- jogador (weapon/<arma>.json, special + techniques): animacao player.<arma>.special(_<id>) no combat.json, nome
  kn8.weapon.special.<id> nos dois idiomas, receita visual no perfil "player" (ou "kaiju_no8" nos punhos da forma)
  e o som do JSON;
- NPC (special_soldier/<perfil>.json): a tecnica na lista de animadas da entidade (ANIMATED* no Java), a animacao
  <prefixo>.action.<id> no arquivo da especie e a receita visual no perfil (a tecnica ou o primeiro golpe "<id>/1").

Uso: python3 tools/combat/auditar_tecnicas.py [saida.md]   (sem saida: so imprime as lacunas)
"""
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
DATA = ROOT / "src/main/resources/data/kn8/kn8"
ASSETS = ROOT / "src/main/resources/assets/kn8"
JAVA = ROOT / "src/main/java/com/kn8/common/soldier/special"

# Perfil -> (classe da entidade, constante com a lista de animadas, prefixo das animacoes).
NPC = {
    "hoshina": ("HoshinaEntity", "ANIMATED", "hoshina"),
    "hoshina_no10": ("HoshinaEntity", "ANIMATED", "hoshina_no10"),
    "kikoru": ("KikoruEntity", "ANIMATED_KIKORU", "kikoru"),
    "kikoru_no4": ("KikoruNo4Entity", "ANIMATED_NO4", "kikoru_no4"),
    "mina": ("MinaEntity", "ANIMATED_MINA", "mina"),
    "reno": ("RenoEntity", "ANIMATED_RENO", "reno"),
    "reno_no6": ("RenoNo6Entity", "ANIMATED_NO6", "reno_no6"),
    "narumi": ("NarumiEntity", "ANIMATED_NARUMI", "narumi"),
    "narumi_no1": ("NarumiNo1Entity", "ANIMATED_NO1", "narumi"),
    "kafka": ("KafkaEntity", "ANIMATED_KAFKA", "kafka"),
    "kaiju_no8": ("KaijuNo8Entity", "ANIMATED_NO8", "kaiju_no8"),
    "isao": ("IsaoEntity", "ANIMATED_ISAO", "isao"),
    "isao_base": ("IsaoBaseEntity", "ANIMATED_BASE", "isao"),
}
# Punhos da forma do jogador: animados pelo fantoche do No. 8 e com os efeitos do perfil dele.
FORM_WEAPONS = {"kaiju_no8_fist": "kaiju_no8"}


def load(path):
    return json.loads(path.read_text(encoding="utf-8"))


def animated(cls, const):
    text = (JAVA / f"{cls}.java").read_text(encoding="utf-8")
    m = re.search(const + r"\s*=\s*List\.of\((.*?)\);", text, re.S)
    return set(re.findall(r'"([a-z0-9_]+)"', m.group(1))) if m else set()


def recipe(effects, tid):
    return tid in effects or f"{tid}/1" in effects


def main():
    combat = load(ASSETS / "player_animations/combat.json")["animations"]
    pt = load(ASSETS / "lang/pt_br.json")
    en = load(ASSETS / "lang/en_us.json")
    vfx = {p.stem: load(p).get("effects", {}) for p in (DATA / "vfx_profile").glob("*.json")}
    rows, gaps = [], []

    for path in sorted((DATA / "weapon").glob("*.json")):
        weapon = load(path)
        item = weapon["item"].split(":", 1)[1]
        techs = ([weapon["special"]] if weapon.get("special") else []) + weapon.get("techniques", [])
        for i, t in enumerate(techs):
            tid = t["id"]
            anim = f"player.{item}.special" if i == 0 else f"player.{item}.special_{tid}"
            form = FORM_WEAPONS.get(item)
            # Arma de fogo: o especial sai com a animacao do tiro (player.<arma>.shoot).
            fire = weapon["style"] == "firearm" and f"player.{item}.shoot" in combat
            has_anim = form is not None or fire or anim in combat or (i > 0 and f"player.{item}.special" in combat)
            own_anim = form is not None or fire or anim in combat
            lang = f"kn8.weapon.special.{tid}" in pt and f"kn8.weapon.special.{tid}" in en
            profile = form or "player"
            # Especial antigo sem receita: os efeitos comuns da lista "vfx" do proprio JSON.
            vis = recipe(vfx.get(profile, {}), tid) or bool(t.get("vfx"))
            sound = t.get("sound", "-")
            row = ("jogador", item, tid, "propria" if own_anim else ("do especial" if has_anim else "FALTA"),
                   "ok" if lang else "FALTA", "ok" if vis else "FALTA", sound)
            rows.append(row)
            if not has_anim or not lang or not vis:
                gaps.append(row)

    for profile, (cls, const, prefix) in NPC.items():
        listed = animated(cls, const)
        anims = load(ASSETS / f"animations/entity/{prefix}.animation.json")["animations"]
        effects = vfx.get(profile, {})
        for tid in load(DATA / f"special_soldier/{profile}.json")["techniques"]:
            in_list = tid in listed
            has_anim = f"{prefix}.action.{tid}" in anims
            vis = recipe(effects, tid)
            row = ("npc", profile, tid, "ok" if in_list and has_anim else
                   ("FALTA na lista" if has_anim else "FALTA"), "-", "ok" if vis else "FALTA",
                   "kn8:weapon.sword_swing (inicio)")
            rows.append(row)
            if not (in_list and has_anim) or not vis:
                gaps.append(row)

    for g in gaps:
        print("LACUNA", *g)
    print(f"{len(rows)} tecnicas, {len(gaps)} lacunas")
    if len(sys.argv) > 1:
        out = ["| Quem | Arma/perfil | Tecnica | Animacao | Nome | Efeito | Som |", "|---|---|---|---|---|---|---|"]
        out += ["| " + " | ".join(r) + " |" for r in rows]
        pathlib.Path(sys.argv[1]).write_text("\n".join(out) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
