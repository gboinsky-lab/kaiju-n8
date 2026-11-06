"""Etapa 7-C (Miguel, 2026-11-06, respostas da docs/ETAPA7_AJUSTES.md): resistencia crescente dos chefes a gelo,
veneno e atordoamento; ataque mental e ilusao do No. 15; puxao do No. 11; previsao do No. 1 que falha contra fintas;
divisao do Myxogasterocarp interrompida pelo nucleo. Numeros [SUPOSICAO].

Uso: python3 tools/data/gen_etapa7c.py (rodar depois do gen_numerados_a.py, que regrava os kaiju_ai e habilidades
dos numerados).
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / "src/main/resources/data/kn8/kn8"
sys.path.insert(0, str(Path(__file__).resolve().parent))
from gen_especiais1 import L  # noqa: E402

BOSSES = ["kaiju_no1", "kaiju_no10_giant", "kaiju_no10_small", "kaiju_no11", "kaiju_no12", "kaiju_no13",
          "kaiju_no14", "kaiju_no15", "kaiju_no15_kikoru", "kaiju_no2", "kaiju_no6", "kaiju_no8_rampage",
          "kaiju_no8_rampage_giant", "kaiju_no9", "kaiju_no9_black", "kaiju_no9_camponotus", "kaiju_no9_fusion",
          "kaiju_no9_fusion_giant", "kaiju_no9_isao", "kaiju_no9_no11", "kaiju_no9_no14", "kaiju_no9_no15",
          "primigenius_honju", "primigenius_revived", "trichonephila_honju"]
# Cada aplicacao seguida dura 25% menos (100, 75, 50, 25%...), no minimo 25%; 30 s sem receber zera.
RESIST = {"step": 0.25, "min": 0.25, "reset_ticks": 600}


def edit(folder, name, change):
    path = DATA / folder / f"{name}.json"
    raw = path.read_bytes().decode("utf-8")
    newline = "\r\n" if "\r\n" in raw else "\n"
    data = json.loads(raw)
    change(data)
    text = json.dumps(data, indent=2, ensure_ascii=False) + "\n"
    path.write_bytes(text.replace("\n", newline).encode("utf-8"))


def write(folder, name, data):
    (DATA / folder / f"{name}.json").write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n",
                                                encoding="utf-8")


def bosses():
    for name in BOSSES:
        edit("kaiju_ai", name, lambda d: d.__setitem__("control_resist", dict(RESIST)))


def no15():
    for kaiju, prefix in (("kaiju_no15", "no15"), ("kaiju_no15_kikoru", "no15k")):
        # Ataque mental: o elo psiquico confunde (jogador: nausea e escuridao; soldado larga o alvo).
        edit("ability", f"{prefix}_psychic_link", lambda d: d["behavior"].__setitem__("mental_ticks", 80))
        write("ability", f"{prefix}_illusion", {
            "type": "kn8:illusion", "damage_multiplier": 0.0, "windup_ticks": 14, "active_ticks": 2,
            "cooldown_ticks": 360, "animation": "action.point", "vfx": [], "camera_shake": 0.0,
            "sound": "kn8:kaiju.energy_beam",
            "behavior": {"priority": 3, "min_range": 0, "max_range": 14}})
        edit("kaiju", kaiju, lambda d: d["abilities"].append(f"kn8:{prefix}_illusion")
             if f"kn8:{prefix}_illusion" not in d["abilities"] else None)
        # Ilusao: por 6 s, metade dos golpes de quem esta a 14 blocos acerta uma imagem falsa.
        edit("kaiju_ai", kaiju, lambda d: d.__setitem__("illusion", {"miss_chance": 0.5, "duration_ticks": 120,
                                                                      "radius": 14.0}))
        edit("vfx_profile", kaiju, lambda d: d["effects"].__setitem__(f"{prefix}_illusion", [
            L("pulse", "accent", "burst", 3, 1.2, 0.0, 2.5, 14),
            L("smoke", "accent", "ring", 6, 0.8, 0.02, 2.5, 30, 0.0, 1.0),
            L("bolt", "accent", "converge", 4, 0.06, 0.0, 2.0, 8, 0.0, 1.4)]))


def no11():
    # Puxao: o jato d'agua arrasta o alvo para perto do No. 11.
    edit("ability", "no11_water_jet", lambda d: d["behavior"].__setitem__("pull", 1.1))


def no1():
    # Previsao: o golpe repetido e esquivado (um a cada 3 s); trocar de tipo e a finta (+25% por 2 s).
    edit("kaiju_ai", "kaiju_no1", lambda d: d.__setitem__("foresight", {"enabled": True, "cooldown_ticks": 60,
                                                                         "feint_bonus": 0.25, "feint_ticks": 40}))


def myxo():
    edit("kaiju_ai", "myxogasterocarp", lambda d: d.__setitem__("split", {
        "health_below": [0.66, 0.33], "channel_ticks": 60, "into": "kn8:phaneroplasmodium", "count": 2,
        "interrupt_core": 0.15, "stagger_ticks": 40}))


if __name__ == "__main__":
    bosses()
    no15()
    no11()
    no1()
    myxo()
    print("Etapa 7-C: resistencia dos chefes, No. 15, No. 11, No. 1 e Myxogasterocarp gravados")
