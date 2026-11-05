#!/usr/bin/env python3
"""Sons da etapa SONS (Miguel 2026-10-30: "todos os sons feitos pelo Claude", sem ElevenLabs): alarmes de invasao,
tecnicas dos especiais, ataques e tiros dos kaiju, socos e o canhao. Mesma sintese do gen_sounds.py (ruido filtrado,
senoides, envelopes; nada gravado de terceiros, sem problema de licenca), com as pecas de la.

Cada personagem tem uma assinatura (decisoes de identidade em docs/audio/SONS_FEITOS.md): Hoshina = ar fino e metal
agudo e rapido; Kikoru = machado grave com estalo eletrico; Narumi = estocada perfurante e tiro seco da baioneta;
Isao = soco de pressao de ar com baque; No. 8 = soco de carne e osso muito grave; Mina = capacitor subindo e canhao
enorme; Reno = tiro limpo com cauda de gelo trincando.

Saida: src/main/resources/assets/kn8/sounds/<grupo>/<nome>_<n>.ogg; as entradas do sounds.json sao escritas pelo
proprio script (chave = evento, legenda subtitles.kn8.<evento>).
Uso: python3 tools/audio/gen_sounds_sons.py [nome ...]   (requer numpy e ffmpeg com libvorbis)
"""
import json
import sys
import zlib
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
from gen_sounds import (OUT, RATE, axe_hit, bandpass, clang, drive, env, flesh, gunshot, lowpass, lowpass_steep,  # noqa
                        mix, noise, normalize, ring, roar_voice, room, saw, sweep, t_axis, tame, thump, whoosh,
                        write_ogg)

SOUNDS_JSON = OUT.parent / "sounds.json"


# ------------------------------------------------------------------------------------------------- pecas novas
def tone(seconds, freq, harmonics=(1.0,), vib=0.0, vib_hz=5.0):
    t = t_axis(seconds)
    f = np.broadcast_to(np.asarray(freq, dtype=float), t.shape) * (1 + vib * np.sin(2 * np.pi * vib_hz * t))
    phase = 2 * np.pi * np.cumsum(f) / RATE
    return sum(a * np.sin(phase * (i + 1)) for i, a in enumerate(harmonics))


def at(signal, seconds_offset, length):
    """Coloca o sinal a partir de seconds_offset num buffer de length segundos."""
    out = np.zeros(int(length * RATE))
    start = int(seconds_offset * RATE)
    end = min(len(out), start + len(signal))
    out[start:end] += signal[:end - start]
    return out


def electric(seconds, rng, density=0.004, low=1500, high=7000):
    """Estalos de descarga eletrica: pulsos curtos e ruido chiado em rajadas."""
    t = t_axis(seconds)
    sparks = (rng.uniform(0, 1, len(t)) < density) * rng.uniform(-1, 1, len(t))
    crackle = bandpass(lowpass(sparks, 6000) * 8 + noise(seconds, rng) * 0.15, low, high)
    buzz = np.sign(np.sin(2 * np.pi * 120 * t)) * 0.08
    return crackle + lowpass(buzz, 900)


def charge_up(seconds, rng, f0, f1, grit=0.3):
    """Carga subindo (capacitor/energia): senoide que acelera + chiado crescente."""
    t = t_axis(seconds)
    rise = (t / seconds) ** 1.5
    whine = np.sin(sweep(seconds, f0, f1, 1.6)) * (0.3 + 0.7 * rise)
    hiss = bandpass(noise(seconds, rng), 2000, 8000) * rise * grit
    return whine + hiss


def ice_crack(seconds, rng, count=10):
    """Gelo trincando: estalos agudos e secos espalhados, com brilho de cristal."""
    t = t_axis(seconds)
    out = np.zeros(len(t))
    for _ in range(count):
        start = rng.uniform(0, seconds * 0.8)
        g = t >= start
        out[g] += bandpass(noise(seconds, rng), 2500, 9000)[g] * np.exp(-(t[g] - start) * rng.uniform(60, 140))
    glint = ring(seconds, [5200, 7900, 10400], 6, rng) * 0.15
    return out + glint


def air_burst(seconds, rng, low, high, rate):
    """Onda de pressao de ar: sopro que abre e some rapido."""
    t = t_axis(seconds)
    return bandpass(noise(seconds, rng), low, high) * np.exp(-t * rate) * np.clip(t / 0.01, 0, 1)


def stereo_free(signal):
    return normalize(signal)


# ------------------------------------------------------------------------------------------------- alarmes
def alarm_tones(rng, notes, step, length, harm=(1.0, 0.45, 0.2)):
    """Sequencia de bips (notas em Hz, None = pausa), cada uma com 'step' segundos."""
    out = np.zeros(int(length * RATE))
    for i, f in enumerate(notes):
        if f is None:
            continue
        beep = tone(step * 0.85, f, harm) * env(step * 0.85, 0.008, 1.2, step * 0.5)
        out += at(beep, i * step, length)
    return out


def invasion_start(rng):
    """Abertura: buzina grave subindo, dois bips altos e o comeco da sirene."""
    s = 2.6
    t = t_axis(s)
    horn = saw(sweep(s, 110, 220, 0.6)) * env(s, 0.15, 1.0, 1.2) * 0.5
    horn = lowpass_steep(horn, 1600, 2)
    beeps = alarm_tones(rng, [None, None, 1320, None, 1320], 0.22, s) * 0.6
    return normalize(drive(horn + beeps + lowpass(noise(s, rng), 200) * 0.2 * env(s, 0.1, 1.0), 1.4)) * 0.9


def siren_loop(rng):
    """Sirene da cidade em laco perfeito (4 s = duas subidas): o comeco e o fim tem a mesma fase."""
    s = 4.0
    t = t_axis(s)
    freq = 560 + 380 * (0.5 - 0.5 * np.cos(2 * np.pi * t / 2.0))
    phase = 2 * np.pi * np.cumsum(freq) / RATE
    # Fecha a fase num numero inteiro de voltas para o laco nao estalar.
    phase *= np.round(phase[-1] / (2 * np.pi)) * 2 * np.pi / phase[-1]
    body = np.sin(phase) + 0.45 * np.sin(phase * 2) + 0.25 * np.sin(phase * 3) + 0.12 * np.sin(phase * 5)
    wobble = 1 + 0.05 * np.sin(2 * np.pi * 4.0 * t)
    sig = body * wobble
    sig = sig / np.max(np.abs(sig)) * 0.8
    return sig  # sem normalize: o normalize apaga o fim (fade) e o laco estalaria


def wave_yoju(rng):
    return normalize(alarm_tones(rng, [880, 1175], 0.24, 0.7)) * 0.75


def wave_honju(rng):
    return normalize(alarm_tones(rng, [660, 880, 660, 880], 0.22, 1.1, (1.0, 0.6, 0.35))) * 0.8


def wave_numbered(rng):
    """Numerado: tres bips graves e um tom longo dissonante."""
    s = 1.8
    beeps = alarm_tones(rng, [523, 523, 523], 0.2, s, (1.0, 0.7, 0.4))
    long_tone = at((tone(1.0, 466, (1.0, 0.5)) + tone(1.0, 494, (0.6,))) * env(1.0, 0.02, 1.0, 0.5), 0.7, s)
    return normalize(drive(beeps + long_tone * 0.7, 1.3)) * 0.85


def critical(rng):
    """Ameaca critica: klaxon rapido em duas notas, aspero, 2 s."""
    s = 2.0
    t = t_axis(s)
    gate = ((t * 5) % 1.0) < 0.6
    f = np.where(((t * 2.5) % 1.0) < 0.5, 740, 554)
    sq = np.sign(np.sin(2 * np.pi * np.cumsum(f) / RATE))
    return normalize(lowpass_steep(sq * gate, 3500, 2) * env(s, 0.01, 0.8, 1.6)) * 0.8


def wave_cleared(rng):
    return normalize(alarm_tones(rng, [784, 1047], 0.18, 0.6, (1.0, 0.3))) * 0.7


def victory(rng):
    """Vitoria: arpejo maior subindo e acorde longo (metal suave)."""
    s = 2.6
    out = alarm_tones(rng, [523, 659, 784], 0.16, s, (1.0, 0.4, 0.15))
    chord = sum(tone(1.9, f, (1.0, 0.3)) for f in (523, 659, 784, 1047)) * env(1.9, 0.02, 1.5, 0.6)
    return normalize(out + at(chord, 0.5, s) * 0.6) * 0.8


def defeat(rng):
    """Derrota: tres notas descendo e um tom grave que some."""
    s = 2.6
    out = alarm_tones(rng, [392, 330, 262], 0.3, s, (1.0, 0.5, 0.3))
    low = at(tone(1.5, 131, (1.0, 0.6, 0.3)) * env(1.5, 0.05, 1.0, 0.4), 0.95, s)
    return normalize(out + low * 0.8) * 0.8


# ------------------------------------------------------------------------------------------------- kaiju


def acid_spray(rng):
    """Jato de acido: esguicho molhado e chiado corroendo."""
    s = 1.0
    t = t_axis(s)
    gush = bandpass(noise(s, rng), 300, 2500) * env(s, 0.03, 1.4, 0.25) * 1.5
    sizzle = bandpass(noise(s, rng), 4000, 10000) * (rng.uniform(0, 1, len(t)) > 0.6) * np.exp(-t * 2) * 0.7
    return tame(gush + sizzle, 9000)


def web_shot(rng):
    """Teia: 'tchuim' elastico curto."""
    s = 0.45
    t = t_axis(s)
    twang = np.sin(sweep(s, 900, 260, 0.6)) * np.exp(-t * 9)
    hiss = bandpass(noise(s, rng), 1500, 5000) * np.exp(-t * 18) * 0.8
    return normalize(twang + hiss) * 0.8


def spine_shot(rng):
    """Espinhos: rajada de assobios curtos."""
    s = 0.7
    out = np.zeros(int(s * RATE))
    for k in range(4):
        part = whoosh(0.18, rng.uniform(2500, 3200), rng.uniform(5000, 6500), rng)
        out += at(part, k * 0.09, s)
    return tame(out, 8000) * 0.85


def spore(rng):
    """Esporos: baforada abafada e fofa."""
    s = 0.9
    t = t_axis(s)
    puff = lowpass_steep(noise(s, rng), 700, 2) * 10 * env(s, 0.04, 1.6, 0.1)
    return normalize(puff + bandpass(noise(s, rng), 1000, 3000) * env(s, 0.1, 1.2) * 0.3) * 0.75


def claw(rng):
    """Garra/estocada de pata: rasgo grave no ar."""
    return normalize(whoosh(0.45, rng.uniform(380, 460), rng.uniform(1300, 1600), rng, low=0.7)) * 0.9


def tail_swipe(rng):
    """Rabada: varrida longa e pesada."""
    s = 0.9
    t = t_axis(s)
    return normalize(whoosh(s, 180, 700, rng, low=1.4) * (1 + 0.3 * np.sin(2 * np.pi * 6 * t)))


def charge_rush(rng):
    """Investida: galope pesado acelerando (4 passos) e o sopro do corpo."""
    s = 1.2
    out = np.zeros(int(s * RATE))
    for k, off in enumerate((0.0, 0.26, 0.48, 0.66)):
        out += at(thump(0.35, 80 + k * 6, 35, rng, 0.2), off, s)
    return normalize(out + whoosh(s, 300, 900, rng, low=0.5) * 0.5)


def leap(rng):
    """Salto do kaiju: impulso grave e ar subindo."""
    s = 0.8
    return normalize(thump(s, 90, 40, rng, 0.3) * 0.7 + whoosh(s, 400, 1600, rng) * 0.6)


def grab(rng):
    """Agarrao do No. 13: estalo de garras fechando."""
    s = 0.4
    t = t_axis(s)
    snap = np.sin(2 * np.pi * 1200 * t) * np.exp(-t * 70) + bandpass(noise(s, rng), 1500, 6000) * np.exp(-t * 50)
    return normalize(drive(snap + flesh(s, rng, 400, 2000, 20), 1.6)) * 0.85




# ------------------------------------------------------------------------------------------------- tecnicas


def hoshina_slash(rng):
    """Corte do Hoshina: ar fino muito rapido e o 'shiing' agudo das laminas duplas."""
    s = rng.uniform(0.22, 0.26)
    blades = ring(s, [4200, 6100, 8300], 14, rng) * 0.35
    return tame(whoosh(s, rng.uniform(2200, 2600), rng.uniform(5200, 6200), rng) + blades, 9000) * 0.85


def tail_slash(rng):
    """Cauda do Numbers 10: chicote pesado com fio de metal."""
    s = 0.4
    t = t_axis(s)
    crack = bandpass(noise(s, rng), 2000, 8000) * np.exp(-np.maximum(t - 0.25, 0) * 80) * (t > 0.25) * 2
    return tame(whoosh(s, 700, 2600, rng, low=0.3) + crack + ring(s, [2900, 4100], 12, rng) * 0.2, 8000)


def narumi_thrust(rng):
    """Estocada da baioneta: perfuracao reta (ar subindo rapido) e o 'tsk' da ponta."""
    s = 0.25
    t = t_axis(s)
    tip = bandpass(noise(s, rng), 5000, 11000) * np.exp(-np.maximum(t - 0.16, 0) * 70) * (t > 0.16) * 2
    return tame(whoosh(s, 1800, 5200, rng) + tip + ring(s, [3600, 5200], 18, rng) * 0.2, 9500) * 0.85


def ice_freeze(rng):
    """Gelo se espalhando: trincas em sequencia e chiado frio."""
    s = 1.2
    t = t_axis(s)
    frost = bandpass(noise(s, rng), 5000, 11000) * env(s, 0.2, 1.0, 0.5) * 0.3
    return tame(ice_crack(s, rng, 22) + frost, 11000) * 0.85




# ------------------------------------------------------------------------------------------------- jogador
def perfect_dodge(rng):
    """Esquiva perfeita: o tempo 'dobra' (sopro que some) e um brilho alto."""
    s = 0.7
    t = t_axis(s)
    rev = whoosh(s, 3000, 900, rng) * 0.6
    chime = (tone(0.6, 1760, (1.0, 0.3)) + tone(0.6, 2637, (0.6,))) * env(0.6, 0.005, 2.0)
    return normalize(rev + at(chime, 0.08, s) * 0.7) * 0.8


def knife_flurry(rng):
    """Sequencia de cortes da faca: quatro cortes rapidos."""
    s = 0.6
    out = np.zeros(int(s * RATE))
    for k in range(4):
        out += at(whoosh(0.13, rng.uniform(1900, 2300), rng.uniform(4200, 5000), rng), k * 0.13, s)
    return tame(out, 7000) * 0.8


def dash_cut(rng):
    """Corte em avanco da espada: arrancada e o arco longo da lamina."""
    s = 0.6
    t = t_axis(s)
    dash = whoosh(0.3, 600, 2400, rng, low=0.5)
    cut = at(whoosh(0.32, 1100, 3400, rng) + ring(0.32, [3150, 4870], 9, rng) * 0.3, 0.2, s)
    return tame(at(dash, 0, s) + cut, 6000)

# ------------------------------------------------------------------------------------------------- AJUSTES-2
def seamless(make, seconds, fade=0.25, peak=0.75):
    """Laco sem estalo: gera um pouco a mais, joga fora o comeco (partida dos filtros) e funde o fim no comeco."""
    f = int(fade * RATE)
    n = int(seconds * RATE)
    sig = make(seconds + 2 * fade)[f:]
    sig = sig - np.mean(sig)
    out = sig[:n].copy()
    ramp = np.linspace(0.0, 1.0, f)
    out[:f] = out[:f] * ramp + sig[n:n + f] * (1.0 - ramp)
    return out / np.max(np.abs(out)) * peak  # sem normalize: o fade do fim estalaria o laco


def major_threat(rng):
    """Invasao grande com numerado: buzina grave que cresce, tres batidas pesadas e um tritono agudo no fim."""
    s = 4.2
    t = t_axis(s)
    horn = (saw(sweep(s, 55, 49, 1.0)) + saw(sweep(s, 55.8, 49.6, 1.0)) * 0.8) * env(s, 1.2, 1.0, 2.2)
    horn = lowpass_steep(horn, 700, 3) * 0.6
    drums = np.zeros(len(t))
    for off in (1.3, 1.9, 2.5):
        drums += at(thump(1.2, 70, 30, rng, 0.4), off, s) * 1.2
    sting = (tone(1.4, 740, (1.0, 0.4)) + tone(1.4, 1047, (0.7, 0.3))) * env(1.4, 0.02, 1.4, 0.3)
    return normalize(drive(horn + drums + at(sting, 2.7, s) * 0.35, 1.5)) * 0.9


def travel_energy_beam(rng):
    """Raio de energia voando: zumbido grave pulsando com chiado eletrico (laco de 2 s)."""
    def make(seconds):
        t = t_axis(seconds)
        pulse = 1 + 0.25 * np.sin(2 * np.pi * 9 * t)
        hum = (saw(2 * np.pi * 72 * t) * 0.5 + np.sin(2 * np.pi * 144 * t) * 0.4) * pulse
        fizz = bandpass(noise(seconds, rng), 1500, 5000) * 0.6
        return lowpass_steep(hum, 1400, 2) + fizz + electric(seconds, rng, 0.002) * 0.7
    return seamless(make, 2.0)


def travel_slash(rng):
    """Corte que voa: assobio de ar fino tremendo (laco de 1,5 s)."""
    def make(seconds):
        t = t_axis(seconds)
        air = bandpass(noise(seconds, rng), 2400, 6500) * (1 + 0.45 * np.sin(2 * np.pi * 17 * t))
        whistle = np.sin(2 * np.pi * 3100 * t + 0.6 * np.sin(2 * np.pi * 5 * t)) * 0.12
        return lowpass_steep(air * 3 + whistle, 9000, 2)
    return seamless(make, 1.5, peak=0.6)


def travel_spiral(rng):
    """Espiral do Isao: ar girando (duas faixas que se alternam) e um ronco de pressao (laco de 1,5 s)."""
    def make(seconds):
        t = t_axis(seconds)
        spin = 0.5 + 0.5 * np.sin(2 * np.pi * 6 * t)
        low = bandpass(noise(seconds, rng), 400, 1200) * spin
        high = bandpass(noise(seconds, rng), 1500, 4000) * (1 - spin)
        roar = np.sin(2 * np.pi * 110 * t) * 0.25
        return (low + high) * 3 + roar
    return seamless(make, 1.5)


def travel_shell(rng):
    """Granada do canhao da Mina: assobio que treme e ronco grave do projetil grande (laco de 2 s)."""
    def make(seconds):
        t = t_axis(seconds)
        whistle = np.sin(2 * np.pi * 1350 * t + 2.0 * np.sin(2 * np.pi * 7 * t)) * 0.25
        air = bandpass(noise(seconds, rng), 800, 2600) * 1.6
        rumble = lowpass_steep(noise(seconds, rng), 180, 3) * 22
        return whistle + air + rumble
    return seamless(make, 2.0)


def travel_ice(rng):
    """Lanca/bala de gelo: chiado frio com cristais tilintando (laco de 1,5 s)."""
    def make(seconds):
        hiss = bandpass(noise(seconds, rng), 4500, 10000) * 1.4
        return hiss + ice_crack(seconds, rng, int(seconds * 14)) * 0.8 + ring(seconds, [6200, 8800], 0.2, rng) * 0.1
    return seamless(make, 1.5, peak=0.6)


def travel_organic(rng):
    """Acido, teia, espinhos e esporos voando: sopro umido borbulhando (laco de 1,2 s)."""
    def make(seconds):
        t = t_axis(seconds)
        bubbles = (rng.uniform(0, 1, len(t)) < 0.0015) * rng.uniform(-1, 1, len(t))
        flutter = 1 + 0.5 * np.sin(2 * np.pi * 9 * t)
        return lowpass(bubbles, 900) * 10 + bandpass(noise(seconds, rng), 300, 1300) * flutter
    return seamless(make, 1.2, peak=0.55)


def tail_guard(rng):
    """Guarda da cauda do Numbers 10: estalo metalico duro com batida (no lugar da bigorna vanilla)."""
    s = 0.6
    t = t_axis(s)
    hit = bandpass(noise(s, rng), 1500, 7000) * np.exp(-t * 70) * 2
    metal = ring(s, [1750, 2650, 4100, 5600], 7, rng) * 0.6
    return normalize(drive(hit + metal + thump(s, 220, 90, rng, 0.2) * 0.5, 1.5)) * 0.85


# ------------------------------------------------------------------------------------------------- SONS-2
# Miguel 2026-11-05: "alguns sons estao muito fofinhos ... ataques de carregamento tem que ser potentes e fortes; o
# Falling Thunder deveria ter som de raio caindo; vale para soco, energia, tiro e tudo mais". Pecas de impacto com
# sub-grave (30-60 Hz), estalo de banda larga no ataque, corpo distorcido e cauda longa; cargas que crescem em
# grave, aspereza e descargas ate travar com um baque.
def transient(seconds, rng, low=1500, high=12000, rate=220, gain=3.0):
    """Estalo do ataque (o 'tapa' que da forca ao golpe): ruido largo que some em poucos ms."""
    t = t_axis(seconds)
    return bandpass(noise(seconds, rng), low, high) * np.exp(-t * rate) * gain


def sub_hit(seconds, f0, f1, decay=4.0, gain=1.6):
    """Sub-grave do impacto: senoide descendo devagar, sentida mais do que ouvida."""
    t = t_axis(seconds)
    return np.sin(sweep(seconds, f0, f1, 0.4)) * np.exp(-t * decay) * np.clip(t / 0.004, 0, 1) * gain


def rolling(seconds, rng, cutoff=180, decay=1.2, start=0.0, wobble=2.5, gain=30.0):
    """Ronco que rola (trovao, desabamento): grave filtrado com ondas lentas de volume."""
    t = t_axis(seconds)
    swell = 0.55 + 0.45 * lowpass_steep(noise(seconds, rng), wobble, 2) * 60
    body = lowpass_steep(noise(seconds, rng), cutoff, 3) * gain * np.clip(swell, 0.15, 1.6)
    return body * np.exp(-np.maximum(t - start, 0) * decay) * np.clip((t - start) / 0.03, 0, 1)


def power(signal, amount=2.4, peak=0.95):
    """Compressao forte (tanh) e volume no talo: o som 'empurra'."""
    return normalize(drive(signal, amount), peak)


def power_charge(seconds, rng, f0, f1, whine0, whine1, grit=0.6, lock=True):
    """Carga potente: dente-de-serra grave desafinado subindo e distorcendo, ronco de turbina que abre o filtro,
    apito aspero subindo, descargas cada vez mais densas e, no fim, o baque da trava."""
    t = t_axis(seconds)
    rise = (t / seconds) ** 1.3
    low = (saw(sweep(seconds, f0, f1, 1.3)) + saw(sweep(seconds, f0 * 1.012, f1 * 1.012, 1.3)) * 0.8
           + np.sin(sweep(seconds, f0 / 2, f1 / 2, 1.3)) * 0.9)
    low = lowpass(low, 200 + 2600 * rise) * (0.35 + 0.9 * rise)
    turbine = lowpass(bandpass(noise(seconds, rng), 80, 2000), 300 + 4000 * rise) * 4 * rise
    whine = saw(sweep(seconds, whine0, whine1, 1.6)) * 0.22 * rise ** 1.5
    sparks = electric(seconds, rng, 0.001 + 0.006 * rise[-1] * 0.5) * rise * grit
    out = drive(low + turbine + lowpass_steep(whine, 6000, 2) + sparks, 2.2)
    if lock:
        out = out + at(sub_hit(0.35, 90, 40, 9) + transient(0.35, rng, 800, 6000, 120, 1.2), seconds - 0.08,
                       seconds + 0.3)[:len(out)]
    return normalize(out, 0.95)


def lightning_strike(rng, seconds=3.2):
    """Raio caindo: rasgo agudo em rajada (o canal abrindo), estalo enorme, chiado eletrico e o trovao rolando."""
    t = t_axis(seconds)
    rip = np.zeros(len(t))
    for k in range(6):
        start = 0.012 * k + rng.uniform(0, 0.006)
        g = t >= start
        rip[g] += bandpass(noise(seconds, rng), 1800, 14000)[g] * np.exp(-(t[g] - start) * 160) * (1.6 - k * 0.15)
    crack = transient(seconds, rng, 500, 14000, 45, 4.0)
    sizzle = bandpass(noise(seconds, rng), 2500, 8000) * (rng.uniform(0, 1, len(t)) > 0.6) * np.exp(-t * 6) * 0.9
    boom = sub_hit(seconds, 70, 28, 1.6, 2.2)
    thunder = rolling(seconds, rng, 220, 0.9, 0.08, 2.0, 45.0)
    return power(room(rip + crack + sizzle + boom, 2.2) + thunder, 2.6)


def impact(rng, seconds, sub0, sub1, body_low, body_high, crack_gain=3.0, tail=1.5, size=1.4):
    """Impacto pesado generico (soco, onda, explosao pequena)."""
    t = t_axis(seconds)
    body = bandpass(noise(seconds, rng), body_low, body_high) * np.exp(-t * 14) * 3
    return room(transient(seconds, rng, 1200, 11000, 200, crack_gain) + sub_hit(seconds, sub0, sub1, tail * 2.5)
                + body + rolling(seconds, rng, 160, tail * 2, 0.0, 3.0, 18.0), size)


# --- cargas
def technique_charge(rng):
    """Carga de tecnica (Release subindo no traje): turbina grave subindo e travando em 0,8 s."""
    return power_charge(0.8, rng, 45, 110, 500, 2200, 0.7) * 0.9


def mina_charge(rng):
    """Carga do canhao da Mina: canhao enorme energizando (1,6 s) ate travar com um baque metalico."""
    s = 1.6
    out = power_charge(s, rng, 32, 95, 300, 2600, 0.9)
    clunk = at(clang(rng, 0.5, [420, 640, 980], 9) * 0.5, s - 0.08, s + 0.4)[:len(out)]
    return power(out + clunk, 1.6)


def energy_charge(rng):
    """Carga do raio de energia na boca do kaiju: ronco de plasma subindo com descargas fortes."""
    s = 1.4
    out = power_charge(s, rng, 28, 80, 200, 1400, 1.2, lock=False)
    t = t_axis(s)
    roar = rolling(s, rng, 300, 0.0, 0.0, 6.0, 20.0) * (t / s)
    return power(out + roar, 1.8)


def kaiju_windup(rng):
    """Preparo de golpe pesado: rosnado crescendo por cima de um ronco do chao que sobe ate tremer."""
    s = 1.3
    t = t_axis(s)
    growl = roar_voice(s, rng, [(0, 36), (1, 50)], [(0, 0.1), (1, 0.8)], rasp=0.9, rough_hz=24, sub=1.0)
    ground = rolling(s, rng, 110, 0.0, 0.0, 4.0, 40.0) * (t / s) ** 1.2
    return power(growl * (0.4 + t / s) + ground + sub_hit(s, 40, 55, 0.2, 0.6) * (t / s), 2.0)


# --- golpes
def fist_swing(rng):
    """Soco comum: ar deslocado pesado (punho grande, nao um sopro)."""
    s = 0.28
    t = t_axis(s)
    air = whoosh(s, rng.uniform(380, 480), rng.uniform(1300, 1600), rng, low=1.2)
    return power(air + sub_hit(s, 90, 60, 8, 0.5) * np.sin(np.pi * t / s), 1.6, 0.85)


def fist_hit(rng):
    """Soco que acerta: estalo, carne e o baque grave do corpo."""
    s = 0.5
    return power(impact(rng, s, 110, 45, 300, 2200, 2.5, 3.0, 0.8) + flesh(s, rng, 400, 2500, 22) * 1.2, 2.2)


def no8_punch(rng):
    """Soco do No. 8: estalo enorme, sub-grave que treme e o ar estourando, com eco de bicho grande."""
    s = 1.2
    t = t_axis(s)
    burst = air_burst(s, rng, 200, 3000, 9) * 2.0
    return power(impact(rng, s, 75, 30, 150, 1500, 4.0, 1.2, 1.6) + burst, 2.6)


def no8_rupture(rng):
    """Soco de ruptura: ar sugado em carga, estouro brutal, sub-grave longo e destrocos caindo."""
    s = 2.6
    t = t_axis(s)
    suck = power_charge(0.45, rng, 60, 160, 2400, 700, 0.5, lock=False) * 0.7
    blast = impact(rng, s - 0.4, 60, 22, 120, 1800, 5.0, 0.8, 2.2) * 1.4
    debris = bandpass(noise(s, rng), 300, 3000) * (rng.uniform(0, 1, len(t)) > 0.97) * np.exp(
        -np.maximum(t - 0.5, 0) * 2.0) * (t > 0.5) * 8
    return power(at(suck, 0.0, s) + at(blast, 0.4, s) + lowpass_steep(debris, 2200, 2), 2.4)


def kaiju_punch(rng):
    """Soco de kaiju (No. 9, No. 10, No. 13): estalo de couraca e baque enorme."""
    s = 1.0
    return power(impact(rng, s, 70, 28, 200, 1600, 3.5, 1.4, 1.6) + flesh(s, rng, 300, 1800, 10) * 0.8, 2.4)


def isao_punch(rng):
    """Soco das manoplas: disparo pneumatico (estampido) com baque metalico pesado."""
    s = 0.7
    pop = air_burst(s, rng, 300, 5000, 22) * 3
    clank = clang(rng, s, [700, 1150, 1750], 14) * 0.5
    return power(impact(rng, s, 95, 38, 250, 2500, 3.5, 2.0, 1.0) + pop + clank, 2.4)


def isao_blast(rng):
    """Onda de choque das manoplas: estouro de pressao, ar rasgando e o chao tremendo."""
    s = 1.5
    burst = air_burst(s, rng, 150, 4000, 5) * 3
    return power(impact(rng, s, 60, 25, 120, 2000, 4.0, 1.0, 1.8) + burst + whoosh(s, 400, 2400, rng) * 0.7, 2.5)


def isao_thunder(rng):
    """Thunder Emperor: barragem de 8 socos pneumaticos e o ultimo como uma explosao."""
    s = 2.6
    out = np.zeros(int(s * RATE))
    for k in range(8):
        out += at(isao_punch(rng) * 0.7, k * 0.11, s)
    out += at(isao_blast(rng) * 1.3, 0.95, s)
    return power(out, 1.6)


def kikoru_swing(rng):
    """Machado da Kikoru: lamina enorme cortando o ar e descargas eletricas estalando no fim."""
    s = 0.75
    t = t_axis(s)
    spin = 1 + 0.5 * np.sin(2 * np.pi * np.interp(t, [0, s], [7, 12]) * t)
    zap = electric(s, rng, 0.006) * np.clip((t - 0.3) / 0.06, 0, 1) * np.exp(-np.maximum(t - 0.45, 0) * 6) * 1.6
    return power(whoosh(s, 220, 1000, rng, low=1.6) * spin + zap + transient(s, rng, 2000, 9000, 50, 0.6) * (t > 0.35),
                 2.0, 0.9)


def kikoru_thunder(rng):
    """Falling Thunder/Half-Moon/golpe no chao: o raio caindo (rasgo, estalo, trovao rolando) e o machado cravando."""
    s = 3.2
    strike = lightning_strike(rng, s)
    hit = at(axe_hit(rng) * 0.9, 0.03, s) + at(sub_hit(0.8, 60, 25, 4, 1.4), 0.03, s)
    return power(strike + hit, 1.8)


# --- tiros e energia
def cannon_shot(rng):
    """Canhao da Mina: estalo supersonico, estrondo que bate no peito, coice metalico e eco longo rolando."""
    s = 2.6
    t = t_axis(s)
    blast = transient(s, rng, 600, 12000, 80, 5.0) + sub_hit(s, 55, 22, 1.8, 2.6)
    body = lowpass_steep(noise(s, rng), 900, 2) * 14 * np.exp(-t * 7)
    clank = clang(rng, s, [380, 610, 900], 10) * 0.3
    echo = rolling(s, rng, 400, 1.1, 0.1, 1.5, 35.0)
    return power(room(blast + body + clank, 2.4) + echo, 2.8)


def mina_explosion(rng):
    """Explosao do tiro da Mina: detonacao enorme, sub-grave e destrocos rolando."""
    s = 2.4
    t = t_axis(s)
    debris = bandpass(noise(s, rng), 400, 3000) * (rng.uniform(0, 1, len(t)) > 0.985) * np.exp(-t * 1.8) * 6
    return power(impact(rng, s, 50, 20, 100, 2500, 5.0, 0.7, 2.2) + lowpass_steep(debris, 2500, 2), 2.8)


def narumi_shot(rng):
    """Tiro da baioneta: disparo pesado e seco com coice grave."""
    s = 0.7
    return power(gunshot(rng, s, 1600, 1100, 9) + sub_hit(s, 110, 45, 9, 0.9) + transient(s, rng, 1500, 12000, 260,
                                                                                                  2.0), 2.0)


def narumi_blast(rng):
    """Seven-Branched Sword/Thunder Fire: explosoes fortes em sequencia, cada uma com estalo e sub-grave."""
    s = 2.0
    out = np.zeros(int(s * RATE))
    for k in range(5):
        out += at(impact(rng, 0.7, 90, 35, 150, 2500, 3.0, 2.0, 1.0) * (1.0 - k * 0.08), k * 0.17
                  + rng.uniform(0, 0.03), s)
    return power(out, 2.0)


def reno_shot(rng):
    """Tiro do Reno: rifle pesado com coice e a cauda de gelo trincando."""
    s = 0.9
    shot = gunshot(rng, s, 1300, 800, 7) + sub_hit(s, 100, 45, 8, 0.8) + transient(s, rng, 1500, 12000, 240, 1.6)
    return power(shot + at(ice_crack(0.5, rng, 8) * 0.6, 0.12, s), 2.0)


def finger_gun(rng):
    """Finger Gun do No. 9: estalo supersonico violento, como uma bala grossa, e o baque do ar."""
    s = 0.6
    t = t_axis(s)
    snap = transient(s, rng, 1200, 14000, 160, 4.0) + sub_hit(s, 120, 50, 10, 0.9)
    whip = bandpass(noise(s, rng), 3000, 10000) * np.exp(-np.maximum(t - 0.01, 0) * 40) * 0.8
    return power(room(snap + whip, 1.2), 2.4)


def energy_beam(rng):
    """Disparo do raio de energia: estouro, plasma rugindo e descargas, sumindo devagar."""
    s = 1.8
    t = t_axis(s)
    blast = transient(s, rng, 400, 9000, 50, 3.5) + sub_hit(s, 70, 30, 1.6, 2.0)
    plasma = (saw(sweep(s, 110, 70)) * 0.6 + saw(sweep(s, 111.5, 71)) * 0.5) * np.exp(-t * 1.4)
    roar = bandpass(noise(s, rng), 300, 4000) * 2.4 * np.exp(-t * 1.8)
    return power(lowpass_steep(plasma, 2500, 2) + roar + blast + electric(s, rng, 0.006) * np.exp(-t * 2) * 1.5, 2.6)


def self_destruct(rng):
    """Autodestruicao do Preondactyl: explosao gigante com o chao tremendo por segundos."""
    s = 3.2
    return power(impact(rng, s, 45, 18, 80, 2500, 5.0, 0.5, 2.8) + rolling(s, rng, 160, 0.8, 0.1, 1.5, 40.0), 2.8)


def ice_shatter(rng):
    """Explosao congelante do No. 6: estouro de cristal enorme, gelo voando e baque grave."""
    s = 1.4
    t = t_axis(s)
    burst = bandpass(noise(s, rng), 2000, 12000) * np.exp(-t * 8) * 2.0
    return power(burst + ice_crack(s, rng, 40) * 1.3 + impact(rng, s, 90, 35, 200, 2000, 2.5, 2.0, 1.2), 2.2)


def hoshina_release(rng):
    """Kuuchi e cortes que voam: estalo de ar cortado violento e o corte assobiando para longe."""
    s = 0.7
    t = t_axis(s)
    crack = transient(s, rng, 2500, 13000, 110, 3.5)
    fly = np.sin(sweep(s, 3400, 1200)) * np.exp(-t * 5) * 0.3
    return power(crack + whoosh(s, 1600, 5000, rng) * 1.3 + fly + ring(s, [4400, 6600], 9, rng) * 0.3
                 + sub_hit(s, 140, 70, 12, 0.5), 2.0, 0.9)



# nome do evento -> (pasta, funcao, variacoes, legenda pt, legenda en)
SOUNDS = {
    "alert.invasion_start": ("alert", invasion_start, 1, "Alerta de invasão", "Invasion alert"),
    "alert.siren_loop": ("alert", siren_loop, 1, "Sirene", "Siren"),
    "alert.wave_yoju": ("alert", wave_yoju, 1, "Alerta: Yoju", "Alert: Yoju"),
    "alert.wave_honju": ("alert", wave_honju, 1, "Alerta: Honju", "Alert: Honju"),
    "alert.wave_numbered": ("alert", wave_numbered, 1, "Alerta: kaiju numerado", "Alert: numbered kaiju"),
    "alert.critical": ("alert", critical, 1, "Ameaça crítica", "Critical threat"),
    "alert.wave_cleared": ("alert", wave_cleared, 1, "Onda eliminada", "Wave cleared"),
    "alert.victory": ("alert", victory, 1, "Invasão contida", "Invasion repelled"),
    "alert.defeat": ("alert", defeat, 1, "Invasão perdida", "Invasion lost"),
    "alert.major_threat": ("alert", major_threat, 1, "Ameaça grande se aproxima", "Major threat approaching"),
    "travel.energy_beam": ("travel", travel_energy_beam, 1, "Raio de energia zumbe", "Energy beam hums"),
    "travel.slash": ("travel", travel_slash, 1, "Corte voando", "Slash flies"),
    "travel.spiral": ("travel", travel_spiral, 1, "Espiral girando", "Spiral whirls"),
    "travel.shell": ("travel", travel_shell, 1, "Projétil do canhão assobia", "Cannon shell whistles"),
    "travel.ice": ("travel", travel_ice, 1, "Gelo voando", "Ice flies"),
    "travel.organic": ("travel", travel_organic, 1, "Projétil de kaiju voando", "Kaiju projectile flies"),
    "technique.tail_guard": ("technique", tail_guard, 2, "Cauda defende", "Tail guards"),
    "kaiju.windup": ("kaiju", kaiju_windup, 2, "Kaiju prepara um golpe", "Kaiju winds up"),
    "kaiju.energy_charge": ("kaiju", energy_charge, 2, "Kaiju carrega energia", "Kaiju charges energy"),
    "kaiju.energy_beam": ("kaiju", energy_beam, 2, "Raio de energia", "Energy beam"),
    "kaiju.finger_gun": ("kaiju", finger_gun, 3, "Finger Gun", "Finger Gun"),
    "kaiju.acid_spray": ("kaiju", acid_spray, 2, "Jato de ácido", "Acid spray"),
    "kaiju.web_shot": ("kaiju", web_shot, 2, "Teia", "Web shot"),
    "kaiju.spine_shot": ("kaiju", spine_shot, 2, "Espinhos", "Spines"),
    "kaiju.spore": ("kaiju", spore, 2, "Esporos", "Spores"),
    "kaiju.claw": ("kaiju", claw, 3, "Golpe de garra", "Claw strike"),
    "kaiju.tail_swipe": ("kaiju", tail_swipe, 2, "Rabada", "Tail swipe"),
    "kaiju.punch": ("kaiju", kaiju_punch, 3, "Soco de kaiju", "Kaiju punch"),
    "kaiju.charge_rush": ("kaiju", charge_rush, 2, "Investida", "Charge"),
    "kaiju.leap": ("kaiju", leap, 2, "Salto de kaiju", "Kaiju leap"),
    "kaiju.grab": ("kaiju", grab, 2, "Agarrão", "Grab"),
    "kaiju.self_destruct": ("kaiju", self_destruct, 1, "Autodestruição", "Self-destruct"),
    "technique.charge": ("technique", technique_charge, 2, "Técnica carregando", "Technique charges"),
    "technique.hoshina_slash": ("technique", hoshina_slash, 3, "Corte duplo", "Twin-blade slash"),
    "technique.hoshina_release": ("technique", hoshina_release, 2, "Corte que voa", "Flying slash"),
    "technique.tail_slash": ("technique", tail_slash, 2, "Cauda corta", "Tail slash"),
    "technique.kikoru_swing": ("technique", kikoru_swing, 3, "Machado eletrizado", "Electrified axe"),
    "technique.kikoru_thunder": ("technique", kikoru_thunder, 2, "Trovão do machado", "Axe thunder"),
    "technique.narumi_thrust": ("technique", narumi_thrust, 3, "Estocada", "Thrust"),
    "technique.narumi_shot": ("technique", narumi_shot, 3, "Tiro da baioneta", "Bayonet shot"),
    "technique.narumi_blast": ("technique", narumi_blast, 2, "Rajada explosiva", "Explosive burst"),
    "technique.isao_punch": ("technique", isao_punch, 3, "Soco das manoplas", "Gauntlet punch"),
    "technique.isao_blast": ("technique", isao_blast, 2, "Onda de choque", "Shockwave"),
    "technique.isao_thunder": ("technique", isao_thunder, 1, "Thunder Emperor", "Thunder Emperor"),
    "technique.no8_punch": ("technique", no8_punch, 3, "Soco do Kaiju No. 8", "Kaiju No. 8 punch"),
    "technique.no8_rupture": ("technique", no8_rupture, 1, "Soco de ruptura", "Rupture punch"),
    "technique.mina_charge": ("technique", mina_charge, 1, "Canhão carregando", "Cannon charges"),
    "technique.mina_explosion": ("technique", mina_explosion, 2, "Explosão do canhão", "Cannon blast"),
    "technique.reno_shot": ("technique", reno_shot, 3, "Tiro congelante", "Freezing shot"),
    "technique.ice_freeze": ("technique", ice_freeze, 2, "Gelo se espalhando", "Ice spreads"),
    "technique.ice_shatter": ("technique", ice_shatter, 2, "Gelo estilhaça", "Ice shatters"),
    "weapon.fist_swing": ("weapon", fist_swing, 3, "Soco", "Punch"),
    "weapon.fist_hit": ("weapon", fist_hit, 3, "Soco acerta", "Punch lands"),
    "weapon.cannon_shot": ("weapon", cannon_shot, 2, "Tiro de canhão", "Cannon shot"),
    "weapon.knife_flurry": ("weapon", knife_flurry, 2, "Sequência de cortes", "Knife flurry"),
    "weapon.dash_cut": ("weapon", dash_cut, 2, "Corte em avanço", "Dash cut"),
    "combat.perfect_dodge": ("combat", perfect_dodge, 1, "Esquiva perfeita", "Perfect dodge"),
}
# Alcance em blocos (attenuation_distance) dos que precisam ser ouvidos de longe.
RANGE = {"weapon.cannon_shot": 80, "technique.mina_explosion": 64, "kaiju.energy_beam": 64,
         "kaiju.self_destruct": 96, "technique.kikoru_thunder": 48, "technique.no8_rupture": 48,
         "technique.isao_thunder": 40, "kaiju.windup": 40, "travel.energy_beam": 40, "travel.shell": 40}
STREAM = {"alert.siren_loop"}


def file_name(event, n):
    return event.split(".", 1)[1] + f"_{n}"


def update_json():
    data = json.loads(SOUNDS_JSON.read_text(encoding="utf-8"))
    for event, (group, _, variants, _, _) in SOUNDS.items():
        entries = []
        for n in range(1, variants + 1):
            entry = {"name": f"kn8:{group}/{file_name(event, n)}"}
            if event in RANGE:
                entry["attenuation_distance"] = RANGE[event]
            if event in STREAM:
                entry["stream"] = True
            entries.append(entry if len(entry) > 1 else entry["name"])
        data[event] = {"subtitle": f"subtitles.kn8.{event}", "sounds": entries}
    SOUNDS_JSON.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def update_lang():
    root = OUT.parent / "lang"
    for lang, index in (("pt_br", 3), ("en_us", 4)):
        path = root / f"{lang}.json"
        data = json.loads(path.read_text(encoding="utf-8"))
        for event, spec in SOUNDS.items():
            data[f"subtitles.kn8.{event}"] = spec[index]
        path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def main(only=None):
    for event, (group, make, variants, _, _) in SOUNDS.items():
        if only and event not in only:
            continue
        for n in range(1, variants + 1):
            rng = np.random.default_rng(zlib.crc32(f"{event}_{n}".encode()))
            write_ogg(OUT / group / f"{file_name(event, n)}.ogg", make(rng))
        print(f"{event}: {variants}")
    update_json()
    update_lang()


if __name__ == "__main__":
    main(set(sys.argv[1:]) or None)
