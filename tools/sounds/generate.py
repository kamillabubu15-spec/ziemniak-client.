"""Generator dzwiekow Ziemniak Client (wszystko syntezowane, bez cudzych probek).

Uruchom:  python3 tools/sounds/generate.py
Wynik:    src/main/resources/assets/ziemniak/sounds/**.ogg  (potrzebny ffmpeg z libvorbis)
"""
import math
import os
import subprocess
import tempfile
import wave

import numpy as np

SR = 44100
ROOT = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.normpath(os.path.join(ROOT, "..", "..", "src", "main", "resources", "assets", "ziemniak", "sounds"))
rng = np.random.default_rng(1234)


# ------------------------------------------------------------------ podstawy

def t_axis(dur):
    return np.arange(int(SR * dur)) / SR


def env(dur, attack=0.004, decay=8.0):
    t = t_axis(dur)
    a = np.clip(t / max(attack, 1e-4), 0, 1)
    return a * np.exp(-t * decay)


def note(name):
    names = {"C": 0, "C#": 1, "D": 2, "D#": 3, "E": 4, "F": 5, "F#": 6, "G": 7, "G#": 8, "A": 9, "A#": 10, "B": 11}
    n, o = name[:-1], int(name[-1])
    return 440.0 * 2 ** ((names[n] + (o - 4) * 12 - 9) / 12)


def sine_sweep(f0, f1, dur, curve=1.0):
    t = t_axis(dur)
    k = (t / dur) ** curve
    f = f0 + (f1 - f0) * k
    return np.sin(2 * np.pi * np.cumsum(f) / SR)


def bell(f, dur, decay=6.0, bright=1.0):
    """Dzwoneczek / pozytywka (FM)."""
    t = t_axis(dur)
    idx = 2.2 * bright * np.exp(-t * 9)
    mod = np.sin(2 * np.pi * f * 3.5 * t) * idx
    s = np.sin(2 * np.pi * f * t + mod) + 0.25 * np.sin(2 * np.pi * f * 2.01 * t) * np.exp(-t * 12)
    return s * env(dur, 0.002, decay)


def blip(f, dur=0.05, up=1.15):
    return sine_sweep(f, f * up, dur) * env(dur, 0.002, 40)


def pop(f0=900, f1=260, dur=0.09):
    s = sine_sweep(f0, f1, dur, 0.5) * env(dur, 0.001, 28)
    click = rng.normal(0, 1, int(SR * 0.004)) * 0.25
    s[: len(click)] += click
    return s


def noise(dur):
    return rng.normal(0, 1, int(SR * dur))


def lowpass(x, cutoff):
    """Prosty filtr 1-biegunowy; cutoff moze byc tablica (zmienny w czasie)."""
    cutoff = np.broadcast_to(cutoff, x.shape)
    a = np.exp(-2 * np.pi * cutoff / SR)
    y = np.zeros_like(x)
    prev = 0.0
    for i in range(len(x)):
        prev = (1 - a[i]) * x[i] + a[i] * prev
        y[i] = prev
    return y


def bandnoise(dur, f0, f1):
    n = noise(dur)
    t = t_axis(dur)
    fc = f0 + (f1 - f0) * (t / dur)
    lo = lowpass(n, fc * 1.4)
    return lo - lowpass(lo, fc * 0.6)


def mix(*parts):
    """parts: (sygnal, start_s, glosnosc)."""
    end = max(int(st * SR) + len(sig) for sig, st, _ in parts)
    out = np.zeros(end)
    for sig, st, g in parts:
        i = int(st * SR)
        out[i:i + len(sig)] += sig * g
    return out


def reverb(x, amount=0.25, delays=(0.031, 0.047, 0.067, 0.089), fb=0.45, tail=0.4):
    y = np.concatenate([x, np.zeros(int(SR * tail))])
    wet = np.zeros_like(y)
    for d in delays:
        n = int(d * SR)
        buf = np.copy(y)
        for _ in range(5):
            buf = np.concatenate([np.zeros(n), buf[:-n]]) * fb
            wet += buf
    return y + wet * amount / len(delays)


def fade_out(x, dur=0.02):
    n = min(len(x), int(SR * dur))
    x = x.copy()
    x[-n:] *= np.linspace(1, 0, n)
    return x


def save(path, x, peak=0.7):
    x = fade_out(np.asarray(x, dtype=np.float64))
    m = np.max(np.abs(x)) or 1.0
    x = x / m * peak
    full = os.path.join(OUT, path + ".ogg")
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        wav = tmp.name
    with wave.open(wav, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes((x * 32767).astype("<i2").tobytes())
    subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", wav, "-c:a", "libvorbis", "-q:a", "5", full], check=True)
    os.remove(wav)
    print("ok", path, f"{len(x) / SR:.2f}s")


# ------------------------------------------------------------------ dzwieki

def sparkle(count=6, dur=0.45, lo=2200, hi=5200):
    parts = []
    for i in range(count):
        f = rng.uniform(lo, hi)
        parts.append((bell(f, 0.18, 22, 0.6), rng.uniform(0, dur - 0.18), rng.uniform(0.3, 0.7)))
    return mix(*parts)


def arpeggio(notes, step=0.055, length=0.35, decay=9, vol=1.0):
    return mix(*[(bell(note(n), length, decay), i * step, vol) for i, n in enumerate(notes)])


def build():
    # --- UI
    save("ui/hover", blip(1900, 0.035, 1.08) * 0.6, 0.35)
    save("ui/click", mix((pop(1100, 380, 0.07), 0, 1.0), (bell(note("A6"), 0.12, 30, 0.4), 0.004, 0.35)), 0.6)
    save("ui/toggle_on", mix((blip(note("C6"), 0.06), 0, 1), (blip(note("G6"), 0.08), 0.06, 1), (bell(note("C7"), 0.2, 18, 0.5), 0.1, 0.4)))
    save("ui/toggle_off", mix((blip(note("G5"), 0.06, 0.95), 0, 1), (blip(note("C5"), 0.09, 0.9), 0.06, 1)))
    save("ui/tab", mix((bandnoise(0.12, 800, 3500) * env(0.12, 0.02, 12), 0, 0.5), (bell(note("E6"), 0.25, 14), 0.05, 0.8)))
    save("ui/open", reverb(mix((arpeggio(["E5", "G5", "B5", "E6", "G6"], 0.05, 0.4), 0, 1), (sparkle(5, 0.5), 0.15, 0.4)), 0.3))
    save("ui/close", reverb(arpeggio(["G6", "E6", "B5", "G5"], 0.045, 0.3), 0.25))
    roul = [(blip(note(n), 0.04), i * 0.045, 0.8) for i, n in enumerate(["C6", "E6", "G6", "C7", "D6", "F6", "A6", "D7", "E6"])]
    save("ui/random", reverb(mix(*roul, (arpeggio(["C6", "E6", "G6", "C7"], 0.03, 0.5), 0.45, 1), (sparkle(8, 0.6), 0.45, 0.5)), 0.3))
    save("ui/reset", mix((arpeggio(["G6", "E6", "C6"], 0.07, 0.3), 0, 1), (sine_sweep(300, 900, 0.12) * env(0.12, 0.005, 20), 0.22, 0.8)))
    save("ui/error", mix((sine_sweep(330, 300, 0.12) * env(0.12, 0.003, 18), 0, 1), (sine_sweep(250, 220, 0.16) * env(0.16, 0.003, 14), 0.1, 1)), 0.55)

    # --- ekwipunek
    save("inv/open", reverb(mix((pop(700, 220, 0.1), 0, 1), (bell(note("C6"), 0.3, 12), 0.03, 0.6), (bell(note("G6"), 0.3, 12), 0.08, 0.5)), 0.2))
    save("inv/close", mix((pop(500, 160, 0.1), 0, 1), (bell(note("G5"), 0.2, 16), 0.02, 0.4)))
    save("inv/slot", bell(3100, 0.05, 60, 0.3), 0.25)

    # --- efekty klikniecia
    save("fx/hearts", mix((pop(1400, 600, 0.06), 0, 0.8), (sparkle(4, 0.3, 2500, 4500), 0.02, 0.7)))
    save("fx/stars", reverb(sparkle(7, 0.4, 3000, 6500), 0.3))
    drop = sine_sweep(380, 1300, 0.09, 2.0) * env(0.09, 0.002, 25)
    save("fx/wave", reverb(mix((drop, 0, 1), (drop * 0.5, 0.11, 0.6)), 0.35))
    party = noise(0.08) * env(0.08, 0.001, 45)
    save("fx/confetti", mix((party, 0, 0.8), (pop(600, 150, 0.12), 0, 1), (sparkle(10, 0.7, 2000, 5500), 0.05, 0.6)))
    save("fx/flowers", reverb(arpeggio(["D6", "F#6", "A6", "D7"], 0.04, 0.35, 10), 0.3))
    t = t_axis(0.45)
    boing = np.sin(2 * np.pi * np.cumsum(420 + 160 * np.sin(2 * np.pi * 9 * t) * np.exp(-t * 5)) / SR) * env(0.45, 0.003, 6)
    save("fx/big_heart", mix((boing, 0, 1), (bell(note("C7"), 0.3, 10), 0.02, 0.3)))
    crack = np.zeros(int(SR * 0.4))
    for _ in range(26):
        i = rng.integers(0, len(crack) - 300)
        crack[i:i + 300] += rng.normal(0, 1, 300) * np.exp(-np.arange(300) / 40) * rng.uniform(0.3, 1)
    save("fx/sparks", mix((crack * env(0.4, 0.001, 6), 0, 0.8), (sparkle(5, 0.35, 3500, 6000), 0, 0.5)))

    # --- gra / HUD
    save("game/combo", mix((bell(note("E6"), 0.3, 14, 0.8), 0, 1), (blip(note("E7"), 0.04), 0, 0.3)))
    lub = sine_sweep(90, 45, 0.12) * env(0.12, 0.004, 22)
    dub = sine_sweep(80, 40, 0.1) * env(0.1, 0.004, 26)
    save("game/heartbeat", mix((lub, 0, 1), (dub, 0.17, 0.8)), 0.8)
    save("game/zoom_in", bandnoise(0.22, 500, 4000) * env(0.22, 0.05, 6) + sine_sweep(500, 1400, 0.22) * env(0.22, 0.03, 9) * 0.2, 0.45)
    save("game/zoom_out", bandnoise(0.2, 3500, 500) * env(0.2, 0.03, 7) + sine_sweep(1300, 500, 0.2) * env(0.2, 0.02, 10) * 0.2, 0.4)

    # --- shadery / pobieranie / edytor HUD
    gl = mix(*[(bell(note(n), 0.5, 7, 0.7), i * 0.035, 0.6) for i, n in enumerate(["C5", "E5", "G5", "B5", "D6", "F#6", "A6", "C7", "E7"])])
    save("shader/apply", reverb(mix((gl, 0, 1), (bandnoise(0.6, 3000, 9000) * env(0.6, 0.1, 4), 0, 0.15)), 0.35))
    save("shader/off", reverb(mix(*[(bell(note(n), 0.35, 9, 0.6), i * 0.04, 0.6) for i, n in enumerate(["C7", "G6", "E6", "C6", "G5"])]), 0.25))
    save("dl/start", mix((blip(note("G5"), 0.05), 0, 1), (blip(note("D6"), 0.06), 0.06, 1)))
    save("dl/done", reverb(mix((arpeggio(["C6", "E6", "G6"], 0.08, 0.3), 0, 1), (bell(note("C7"), 0.7, 5), 0.24, 1), (sparkle(6, 0.6), 0.25, 0.4)), 0.35))
    save("hud/pick", mix((blip(900, 0.05, 1.5), 0, 1)), 0.45)
    save("hud/drop", mix((pop(600, 200, 0.07), 0, 1), (bell(note("C6"), 0.15, 20, 0.5), 0.01, 0.4)), 0.5)

    # --- pozytywka do menu (petla ~19 s)
    bpm = 100
    beat = 60 / bpm / 2
    melody = ["E6", "G6", "A6", "G6", "E6", "D6", "C6", "D6",
              "E6", "G6", "C7", "B6", "A6", "G6", "E6", "-",
              "D6", "E6", "G6", "E6", "D6", "C6", "A5", "C6",
              "D6", "E6", "D6", "C6", "A5", "G5", "C6", "-"]
    bass = ["C5", "G5", "A4", "E5", "F4", "C5", "G4", "D5"]
    length = len(melody) * beat * 2
    parts = []
    for rep in range(2):
        off = rep * len(melody) * beat
        for i, n in enumerate(melody):
            if n != "-":
                parts.append((bell(note(n), 1.2, 3.5, 0.8), off + i * beat, 0.55))
        for i, n in enumerate(bass * 2):
            parts.append((bell(note(n), 1.6, 2.5, 0.4), off + i * beat * 4, 0.3))
    song = mix(*parts)
    n = int(length * SR)
    song = reverb(song, 0.35, tail=1.5)
    loop = song[:n].copy()
    tail = song[n:]
    loop[: len(tail)] += tail  # ogon poglosu na poczatek = bezszwowa petla
    save("music/musicbox", loop, 0.5)


if __name__ == "__main__":
    build()
