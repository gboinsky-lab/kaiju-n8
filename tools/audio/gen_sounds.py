#!/usr/bin/env python3
"""Sons do kn8 gerados por sintese (0.2, Etapa 1): nada de gravacao de terceiros, sem problema de licenca.

Cada som e montado com ruido filtrado, senoides, envelopes e distorcao leve, em numpy, salvo como WAV e convertido
para OGG Vorbis mono pelo ffmpeg (formato que o Minecraft le). Varios sons tem 2 ou 3 variacoes (semente diferente)
para nao repetir igual.

Saida: src/main/resources/assets/kn8/sounds/<grupo>/<nome>_<n>.ogg
Os nomes batem com assets/kn8/sounds.json e com KN8Sounds. Para trocar um som por um arquivo melhor, basta salvar
um .ogg com o mesmo nome por cima (nada muda no codigo).
Uso: python3 tools/audio/gen_sounds.py   (requer numpy e ffmpeg com libvorbis)
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
def growl(seconds, base, rng, rough=0.6, glide=0.8):
    """Rugido: dente-de-serra grave com vibrato irregular, ruido no meio (garganta) e formante que abre e fecha."""
    t = t_axis(seconds)
    wobble = 1 + 0.06 * np.sin(2 * np.pi * rng.uniform(5, 9) * t) + 0.03 * lowpass(noise(seconds, rng), 8) * 20
    phase = sweep(seconds, base, base * glide) * 1.0
    phase = np.cumsum(np.gradient(phase) * wobble)
    body = saw(phase) + 0.5 * saw(phase * 2.01)
    throat = bandpass(noise(seconds, rng), 200, 1400) * rough * 3
    formant = 300 + 900 * np.sin(np.pi * np.clip(t / seconds, 0, 1)) ** 2
    voice = lowpass(body + throat, formant)
    return drive(voice * 2.5, 2.0)


def kaiju_roar(rng):
    s = 1.9
    return normalize(growl(s, rng.uniform(70, 90), rng) * env(s, 0.25, 1.4, 0.6))


def kaiju_ambient(rng):
    s = 1.3
    return normalize(growl(s, rng.uniform(55, 70), rng, rough=1.0, glide=0.9) * env(s, 0.35, 1.2, 0.2)) * 0.8


def kaiju_hurt(rng):
    s = 0.55
    return normalize(growl(s, rng.uniform(110, 140), rng, rough=0.8, glide=0.6) * env(s, 0.02, 1.6))


def kaiju_death(rng):
    s = 2.6
    return normalize(growl(s, rng.uniform(90, 105), rng, rough=0.9, glide=0.35) * env(s, 0.15, 1.1, 0.8))


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
    return normalize(drive(crunch * 2 + clack, 1.8) + growl(s, 160, rng) * env(s, 0.01, 2.0) * 0.3)


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


def dash(rng):
    return whoosh(0.28, 900, 3000, rng) * 0.7


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


def main():
    for name, (group, make, variants) in SOUNDS.items():
        for n in range(1, variants + 1):
            rng = np.random.default_rng(zlib.crc32(f"{name}_{n}".encode()))
            write_ogg(OUT / group / f"{name}_{n}.ogg", make(rng))
        print(f"{group}/{name}: {variants} variacao(oes)")


if __name__ == "__main__":
    main()
