#!/usr/bin/env python3
"""Sons do kn8 gerados por sintese (0.2, Etapa 1): nada de gravacao de terceiros, sem problema de licenca.

Cada som e montado com ruido filtrado, senoides, envelopes e distorcao leve, em numpy, salvo como WAV e convertido
para OGG Vorbis mono pelo ffmpeg (formato que o Minecraft le). Varios sons tem 2 ou 3 variacoes (semente diferente)
para nao repetir igual.

Saida: src/main/resources/assets/kn8/sounds/<grupo>/<nome>_<n>.ogg
Os nomes batem com assets/kn8/sounds.json e com KN8Sounds. Para trocar um som por um arquivo melhor, basta salvar
um .ogg com o mesmo nome por cima (nada muda no codigo).
Uso: python3 tools/audio/gen_sounds.py [nome ...]   (requer numpy e ffmpeg com libvorbis)
"""
import subprocess
import zlib
import tempfile
import wave
from pathlib import Path

import numpy as np

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "src/main/resources/assets/kn8/sounds"
RATE = 44100


# ---------------------------------------------------------------------------------------------- blocos de sintese
def t_axis(seconds):
    return np.arange(int(seconds * RATE)) / RATE


def noise(seconds, rng):
    return rng.uniform(-1.0, 1.0, int(seconds * RATE))


def lowpass(signal, cutoff):
    """Passa-baixa de 1 polo (cutoff em Hz, pode variar no tempo)."""
    cutoff = np.broadcast_to(np.asarray(cutoff, dtype=float), signal.shape)
    alpha = 1.0 - np.exp(-2.0 * np.pi * cutoff / RATE)
    out = np.empty_like(signal)
    acc = 0.0
    for i, (x, a) in enumerate(zip(signal, alpha)):
        acc += a * (x - acc)
        out[i] = acc
    return out


def lowpass_steep(signal, cutoff, order=4):
    """Passa-baixa mais forte (varios polos em serie): para grave sem chiado."""
    for _ in range(order):
        signal = lowpass(signal, cutoff)
    return signal


def bandpass(signal, low, high):
    return lowpass(signal, high) - lowpass(signal, low)


def env(seconds, attack, decay_power=1.0, hold=0.0):
    """Envelope: sobe em 'attack' s, segura 'hold' s e cai ate 0 no fim (curva pela potencia)."""
    t = t_axis(seconds)
    rise = np.clip(t / max(attack, 1e-4), 0, 1)
    fall_start = attack + hold
    fall = np.clip(1 - (t - fall_start) / max(seconds - fall_start, 1e-4), 0, 1) ** decay_power
    return rise * np.where(t < fall_start, 1.0, fall)


def sweep(seconds, f0, f1, shape=1.0):
    """Fase de uma senoide que vai de f0 a f1 Hz."""
    t = t_axis(seconds)
    freq = f0 + (f1 - f0) * (t / seconds) ** shape
    return 2 * np.pi * np.cumsum(freq) / RATE


def saw(phase):
    return 2.0 * ((phase / (2 * np.pi)) % 1.0) - 1.0


def drive(signal, amount):
    return np.tanh(signal * amount) / np.tanh(amount)


def mix(*parts):
    length = max(len(p) for p in parts)
    out = np.zeros(length)
    for p in parts:
        out[:len(p)] += p
    return out


def normalize(signal, peak=0.89):
    signal = signal - np.mean(signal)
    top = np.max(np.abs(signal))
    signal = signal / top * peak if top > 0 else signal
    fade = min(len(signal), int(0.01 * RATE))
    signal[-fade:] *= np.linspace(1, 0, fade)
    return signal


# ---------------------------------------------------------------------------------------------- sons
def resonator(signal, freq, q):
    """Passa-faixa ressonante (biquad RBJ, ganho 0 dB no pico) com frequencia que pode variar no tempo: e o
    formante da garganta/boca. So numpy + laco simples (o projeto nao depende de scipy)."""
    freq = np.broadcast_to(np.asarray(freq, dtype=float), signal.shape)
    w0 = 2 * np.pi * np.clip(freq, 20, RATE * 0.45) / RATE
    alpha = np.sin(w0) / (2 * q)
    a0 = 1 + alpha
    b0 = (alpha / a0).tolist()
    a1 = (-2 * np.cos(w0) / a0).tolist()
    a2 = ((1 - alpha) / a0).tolist()
    out = [0.0] * len(signal)
    x1 = x2 = y1 = y2 = 0.0
    for i, x in enumerate(signal.tolist()):
        y = b0[i] * (x - x2) - a1[i] * y1 - a2[i] * y2
        x2, x1 = x1, x
        y2, y1 = y1, y
        out[i] = y
    return np.array(out)


def curve(seconds, points):
    """Curva por pontos (tempo relativo 0..1, valor) ao longo do som."""
    t = t_axis(seconds) / seconds
    xs, ys = zip(*points)
    return np.interp(t, xs, ys)


def room(signal, size=1.0):
    """Eco curto de criatura grande ao ar livre: copias atrasadas, mais graves e mais fracas."""
    out = signal.copy()
    for delay, gain in ((0.045, 0.35), (0.085, 0.25), (0.14, 0.16), (0.22, 0.1)):
        shift = int(delay * size * RATE)
        out[shift:] += lowpass(signal, 1800)[:len(signal) - shift] * gain
    return out


def roar_voice(seconds, rng, pitch, opening, rasp=0.7, rough_hz=28.0, sub=0.6):
    """Voz de kaiju (0.2, refeita a pedido do Miguel: "mais grave e parecido com um rugido").

    Fonte de "pregas vocais" grave e irregular (dente-de-serra + sub-harmonico f/2, que da o ronco rasgado), modulada
    em amplitude a ~25-35 Hz (aspereza do rugido), mais sopro de ar. Tudo passa por 3 formantes que abrem com a boca
    ({@code opening} 0 = fechada, 1 = escancarada), um peito grave e um eco de bicho grande.
    pitch e opening sao listas de pontos (tempo relativo, valor)."""
    t = t_axis(seconds)
    f0 = curve(seconds, pitch)
    jitter = 1 + lowpass_steep(noise(seconds, rng), 12, 2) * 6 * 0.05 + 0.025 * np.sin(2 * np.pi * 6.5 * t)
    phase = 2 * np.pi * np.cumsum(f0 * jitter) / RATE
    source = saw(phase) + sub * np.sin(phase * 0.5) + 0.35 * saw(phase * 0.5)
    flutter_hz = rough_hz * (1 + 0.25 * lowpass(noise(seconds, rng), 3) * 10)
    flutter = 1 + 0.75 * np.sin(2 * np.pi * np.cumsum(flutter_hz) / RATE)
    breath = noise(seconds, rng) * rasp * 1.6
    excite = source * flutter + breath * (0.6 + 0.4 * flutter)
    mouth = curve(seconds, opening)
    voice = (resonator(excite, 180 + 420 * mouth, 3.0) * 1.0
             + resonator(excite, 520 + 700 * mouth, 4.0) * 0.55
             + resonator(excite, 1500 + 900 * mouth, 6.0) * 0.18 * (0.3 + mouth))
    chest = lowpass_steep(source * flutter, 160, 3) * 1.4
    body = drive((voice * 3 + chest * sub) * 1.5, 2.2)
    return lowpass_steep(room(body), 4200, 2)


def kaiju_roar(rng):
    """Rugido de investida: abre a boca toda, sobe e cai o tom, longo."""
    s = rng.uniform(2.4, 2.8)
    base = rng.uniform(42, 50)
    voice = roar_voice(s, rng, [(0, base * 0.8), (0.2, base * 1.15), (0.6, base * 1.05), (1, base * 0.7)],
                       [(0, 0.1), (0.15, 0.9), (0.65, 1.0), (1, 0.3)], rasp=0.8)
    return normalize(voice * env(s, 0.18, 1.3, s * 0.45))


def kaiju_ambient(rng):
    """Rosnado de espera: boca quase fechada, ronco grave e longo (o rugido de verdade fica para a investida)."""
    s = rng.uniform(1.8, 2.3)
    base = rng.uniform(36, 44)
    voice = roar_voice(s, rng, [(0, base), (0.5, base * 1.08), (1, base * 0.9)],
                       [(0, 0.0), (0.5, 0.35), (1, 0.1)], rasp=0.6, rough_hz=22, sub=0.9)
    return normalize(voice * env(s, 0.3, 1.2, s * 0.3)) * 0.85


def kaiju_hurt(rng):
    """Dano: urro curto de dor, ataque seco, tom pula para cima e despenca."""
    s = rng.uniform(0.6, 0.75)
    base = rng.uniform(55, 68)
    voice = roar_voice(s, rng, [(0, base * 1.5), (0.15, base * 1.25), (1, base * 0.7)],
                       [(0, 0.9), (0.3, 0.7), (1, 0.2)], rasp=1.0, rough_hz=34, sub=0.5)
    grunt = thump(s, 90, 45, rng, 0.0) * 0.5
    return normalize(drive(voice * env(s, 0.01, 1.8, 0.08) + grunt, 1.3))


def kaiju_death(rng):
    """Morte: rugido que perde forca, tom caindo ate um estertor grave."""
    s = 3.0
    base = rng.uniform(46, 52)
    voice = roar_voice(s, rng, [(0, base * 1.1), (0.3, base), (0.75, base * 0.6), (1, base * 0.4)],
                       [(0, 0.8), (0.3, 1.0), (0.8, 0.3), (1, 0.0)], rasp=0.9, rough_hz=20)
    return normalize(voice * env(s, 0.12, 1.0, 0.9))


def thump(seconds, f0, f1, rng, crack=0.0):
    t = t_axis(seconds)
    sub = np.sin(sweep(seconds, f0, f1, 0.5)) * np.exp(-t * 6 / seconds)
    rumble = lowpass_steep(noise(seconds, rng), 140) * 40 * np.exp(-t * 4 / seconds)
    snap = bandpass(noise(seconds, rng), 1500, 6000) * np.exp(-t * 60) * crack
    return sub + rumble + snap


def kaiju_step(rng):
    return normalize(thump(0.45, rng.uniform(70, 85), 35, rng, 0.15)) * 0.9


def kaiju_slam(rng):
    s = 1.6
    t = t_axis(s)
    debris = bandpass(noise(s, rng), 300, 3000) * np.exp(-t * 3) * 0.6 * (rng.uniform(0, 1, len(t)) > 0.97)
    return normalize(drive(thump(s, 70, 28, rng, 0.7) + lowpass_steep(debris, 1500, 2) * 6, 1.5))


def kaiju_bite(rng):
    s = 0.4
    t = t_axis(s)
    crunch = bandpass(noise(s, rng), 400, 3500) * (np.exp(-t * 25) + 0.6 * np.exp(-np.maximum(t - 0.08, 0) * 30)
                                                    * (t > 0.08))
    clack = np.sin(2 * np.pi * 900 * t) * np.exp(-t * 80)
    snarl = roar_voice(s, rng, [(0, 80), (1, 60)], [(0, 0.8), (1, 0.2)], rasp=1.0) * env(s, 0.01, 2.0)
    return normalize(drive(crunch * 2 + clack, 1.8) + snarl * 0.3)


def gunshot(rng, seconds, crack_hz, body_hz, tail):
    t = t_axis(seconds)
    crack = bandpass(noise(seconds, rng), crack_hz, 9000) * np.exp(-t * 90)
    body = lowpass_steep(noise(seconds, rng), body_hz, 2) * 12 * np.exp(-t * 18)
    boom = np.sin(sweep(seconds, 120, 50)) * np.exp(-t * 14)
    echo = lowpass_steep(noise(seconds, rng), 700, 2) * 4 * np.exp(-t * tail) * (t > 0.06)
    return normalize(drive(crack * 3 + body + boom + echo, 2.5))


def rifle_shot(rng):
    return gunshot(rng, 0.7, 1200, 700, 6)


def pistol_shot(rng):
    return gunshot(rng, 0.45, 1800, 1100, 10)


def whoosh(seconds, f0, f1, rng, low=0.0):
    t = t_axis(seconds)
    center = np.interp(t, [0, seconds * 0.5, seconds], [f0, f1, f0 * 0.8])
    air = bandpass(noise(seconds, rng), center * 0.5, center * 1.5)
    bell = np.sin(np.pi * np.clip(t / seconds, 0, 1)) ** 2
    out = air * bell * 4
    if low:
        out += np.sin(sweep(seconds, 90, 60)) * bell * low
    return normalize(out)


def blade_swing(rng):
    return whoosh(0.32, rng.uniform(1400, 1800), rng.uniform(3500, 4500), rng) * 0.8


def blade_heavy(rng):
    return whoosh(0.55, rng.uniform(700, 900), rng.uniform(2200, 2800), rng, low=0.5)


def ring(seconds, partials, decay, rng):
    """Ressonancia de metal (lamina vibrando): parciais inarmonicos que somem rapido."""
    t = t_axis(seconds)
    out = np.zeros(len(t))
    for i, f in enumerate(partials):
        f *= rng.uniform(0.985, 1.015)
        out += np.sin(2 * np.pi * f * t + rng.uniform(0, 6.28)) * np.exp(-t * decay * (1 + i * 0.35)) / (1 + i * 0.6)
    return out


def flesh(seconds, rng, low, high, rate):
    """Corte em carne de kaiju: ruido filtrado com estalos curtos."""
    t = t_axis(seconds)
    grains = (rng.uniform(0, 1, len(t)) > 0.996) * rng.uniform(0.5, 1.0, len(t))
    grains = lowpass(grains, 600) * 30
    return bandpass(noise(seconds, rng), low, high) * (np.exp(-t * rate) + grains * np.exp(-t * rate * 0.5))


# Faca de combate: pequena e rapida -> sons curtos, agudos e secos.
def tame(signal, cutoff):
    """Corta o chiado de cima (o passa-faixa de 1 polo deixa passar muito agudo do ruido branco)."""
    return normalize(lowpass_steep(signal, cutoff, 3))


def knife_swing(rng):
    return tame(whoosh(rng.uniform(0.16, 0.2), rng.uniform(1700, 2100), rng.uniform(3600, 4400), rng), 5500) * 0.75


def knife_heavy(rng):
    """Estocada: sopro curto que sobe e um 'tsk' do fio no fim."""
    s = 0.3
    t = t_axis(s)
    tip = bandpass(noise(s, rng), 5000, 10000) * np.exp(-np.maximum(t - 0.2, 0) * 60) * (t > 0.2) * 2
    return tame(whoosh(s, 1300, 3600, rng) + tip * 0.6, 6000) * 0.85


def knife_hit(rng):
    s = 0.22
    t = t_axis(s)
    click = np.sin(2 * np.pi * rng.uniform(2400, 2900) * t) * np.exp(-t * 140)
    return tame(drive(flesh(s, rng, 1200, 4500, 28) * 2.5 + click * 0.6, 1.5), 6000) * 0.85


# Espada: lamina longa de metal -> arco medio com "shing" metalico.
SWORD_RING = [3150, 4870, 6620, 8230]


def sword_swing(rng):
    s = rng.uniform(0.38, 0.44)
    blade = ring(s, SWORD_RING, 9, rng) * np.sin(np.pi * np.clip(t_axis(s) / s, 0, 1)) * 0.25
    return tame(whoosh(s, rng.uniform(800, 1000), rng.uniform(2200, 2600), rng) + blade, 4500) * 0.85


def sword_heavy(rng):
    """Arco largo: o metal 'canta' no inicio (giro da lamina) e o ar passa mais grave."""
    s = 0.62
    t = t_axis(s)
    draw = ring(s, [f * 0.9 for f in SWORD_RING], 5, rng) * np.clip(t / 0.05, 0, 1) * 0.4
    return tame(whoosh(s, 600, 1900, rng, low=0.3) + draw, 4500)


def sword_hit(rng):
    s = 0.45
    t = t_axis(s)
    edge = ring(s, [2350, 3720, 5480], 14, rng) * 0.6
    thud = np.sin(sweep(s, 160, 80)) * np.exp(-t * 30) * 0.5
    return tame(drive(flesh(s, rng, 700, 4000, 16) * 2.2 + edge + thud, 1.6), 5000)


# Machado: pesado -> ar grave girando ("vuum-vuum") e golpe de corte seco que racha.
def axe_swing(rng):
    s = rng.uniform(0.5, 0.58)
    t = t_axis(s)
    spin = 1 + 0.5 * np.sin(2 * np.pi * rng.uniform(10, 13) * t)
    return normalize(whoosh(s, rng.uniform(320, 400), rng.uniform(1100, 1300), rng, low=0.9) * spin) * 0.9


def axe_heavy(rng):
    s = 0.85
    t = t_axis(s)
    spin = 1 + 0.6 * np.sin(2 * np.pi * np.interp(t, [0, s], [7, 14]) * t)
    return normalize(whoosh(s, 220, 900, rng, low=1.4) * spin)


def axe_hit(rng):
    """Golpe de machado: baque grave + estalo de osso/couraca rachando."""
    s = 0.6
    t = t_axis(s)
    crack = np.zeros(len(t))
    for _ in range(5):
        start = rng.uniform(0.0, 0.12)
        g = t >= start
        crack[g] += bandpass(noise(s, rng), 600, 3500)[g] * np.exp(-(t[g] - start) * 45)
    return normalize(drive(thump(s, 110, 40, rng, 0.4) * 1.3 + crack * 1.6 + flesh(s, rng, 300, 1800, 10), 1.8))


def axe_special(rng):
    """0.5, ataque especial do machado (Golpe Sismico): lamina cortando o chao + estrondo grave + pedras caindo."""
    s = 1.8
    t = t_axis(s)
    crack = np.zeros(len(t))
    for _ in range(9):
        start = rng.uniform(0.0, 0.35)
        g = t >= start
        crack[g] += bandpass(noise(s, rng), 500, 4000)[g] * np.exp(-(t[g] - start) * 30)
    debris = bandpass(noise(s, rng), 300, 2500) * np.exp(-t * 2.5) * (rng.uniform(0, 1, len(t)) > 0.96)
    boom = thump(s, 62, 24, rng, 0.9)
    return normalize(drive(boom * 1.4 + crack * 1.2 + lowpass_steep(debris, 1800, 2) * 7, 1.7))


def dash(rng):
    # Mais longo e com "tum" grave: o primeiro ficou baixo demais no teste em jogo (0.2, Etapa 1).
    return whoosh(0.36, 600, 2400, rng, low=0.6)


def clang(rng, seconds, partials, decay):
    t = t_axis(seconds)
    out = np.zeros(len(t))
    for i, f in enumerate(partials):
        f *= rng.uniform(0.98, 1.02)
        out += np.sin(2 * np.pi * f * t) * np.exp(-t * decay * (1 + i * 0.4)) / (1 + i * 0.5)
    hit = bandpass(noise(seconds, rng), 2000, 9000) * np.exp(-t * 120)
    return out + hit


def parry(rng):
    return normalize(clang(rng, 0.9, [1180, 1870, 2650, 3910, 5230], 5))


def guard_break(rng):
    s = 0.8
    t = t_axis(s)
    crunch = bandpass(noise(s, rng), 300, 2500) * np.exp(-t * 12) * 2
    return normalize(drive(clang(rng, s, [620, 990, 1610, 2380], 7) + crunch, 2.0))


def overheat_alarm(rng):
    s = 0.7
    t = t_axis(s)
    tone = np.where(t % 0.35 < 0.15, np.sign(np.sin(2 * np.pi * 1450 * t)), 0.0)
    tone += np.where((t % 0.35 >= 0.17) & (t % 0.35 < 0.32), np.sign(np.sin(2 * np.pi * 1100 * t)), 0.0)
    return normalize(lowpass(tone, 5000)) * 0.6


def dismantle(rng):
    s = 0.5
    t = t_axis(s)
    grains = np.zeros(len(t))
    for _ in range(6):
        start = rng.uniform(0, s - 0.08)
        g = (t >= start) & (t < start + 0.07)
        grains[g] += np.exp(-(t[g] - start) * 40)
    wet = bandpass(noise(s, rng), 250, 2200) * grains
    return normalize(drive(wet * 3 + lowpass(noise(s, rng), 300) * grains * 2, 1.6)) * 0.8


def siren(rng):
    s = 3.0
    t = t_axis(s)
    freq = 650 + 350 * (0.5 - 0.5 * np.cos(2 * np.pi * t / 1.5))
    phase = 2 * np.pi * np.cumsum(freq) / RATE
    tone = np.sin(phase) + 0.4 * np.sin(phase * 2) + 0.2 * np.sin(phase * 3)
    return normalize(tone * env(s, 0.2, 0.6, 2.4)) * 0.7


# nome -> (grupo, funcao, variacoes)
SOUNDS = {
    "kaiju_roar": ("kaiju", kaiju_roar, 3),
    "kaiju_ambient": ("kaiju", kaiju_ambient, 3),
    "kaiju_hurt": ("kaiju", kaiju_hurt, 3),
    "kaiju_death": ("kaiju", kaiju_death, 2),
    "kaiju_step": ("kaiju", kaiju_step, 3),
    "kaiju_slam": ("kaiju", kaiju_slam, 2),
    "kaiju_bite": ("kaiju", kaiju_bite, 3),
    "rifle_shot": ("weapon", rifle_shot, 3),
    "pistol_shot": ("weapon", pistol_shot, 3),
    "blade_swing": ("weapon", blade_swing, 3),
    "blade_heavy": ("weapon", blade_heavy, 2),
    "knife_swing": ("weapon", knife_swing, 3),
    "knife_heavy": ("weapon", knife_heavy, 2),
    "knife_hit": ("weapon", knife_hit, 3),
    "sword_swing": ("weapon", sword_swing, 3),
    "sword_heavy": ("weapon", sword_heavy, 2),
    "sword_hit": ("weapon", sword_hit, 3),
    "axe_swing": ("weapon", axe_swing, 3),
    "axe_heavy": ("weapon", axe_heavy, 2),
    "axe_hit": ("weapon", axe_hit, 3),
    "axe_special": ("weapon", axe_special, 2),
    "parry": ("weapon", parry, 2),
    "guard_break": ("weapon", guard_break, 2),
    "dash": ("suit", dash, 2),
    "overheat_alarm": ("suit", overheat_alarm, 1),
    "dismantle": ("carcass", dismantle, 3),
    "siren": ("alert", siren, 1),
}


def write_ogg(path, signal):
    path.parent.mkdir(parents=True, exist_ok=True)
    pcm = (np.clip(signal, -1, 1) * 32767).astype(np.int16)
    with tempfile.NamedTemporaryFile(suffix=".wav") as tmp:
        with wave.open(tmp.name, "wb") as wav:
            wav.setnchannels(1)
            wav.setsampwidth(2)
            wav.setframerate(RATE)
            wav.writeframes(pcm.tobytes())
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", tmp.name, "-c:a", "libvorbis", "-q:a", "4",
                        str(path)], check=True)


def main(only=None):
    for name, (group, make, variants) in SOUNDS.items():
        if only and name not in only:
            continue
        for n in range(1, variants + 1):
            rng = np.random.default_rng(zlib.crc32(f"{name}_{n}".encode()))
            write_ogg(OUT / group / f"{name}_{n}.ogg", make(rng))
        print(f"{group}/{name}: {variants} variacao(oes)")


if __name__ == "__main__":
    import sys

    # Sem argumentos gera tudo; com nomes, so esses (o Vorbis muda os bytes a cada execucao, entao regerar tudo
    # deixaria todos os .ogg "alterados" no Git mesmo sem mudar o som).
    main(set(sys.argv[1:]) or None)
