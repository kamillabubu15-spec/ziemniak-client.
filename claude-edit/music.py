#!/usr/bin/env python3
"""Phonk soundtrack for the Claude edit: 130 BPM, F minor, synthesized offline with numpy.

Writes soundtrack.wav (48 kHz stereo, 24-bit) and events.json, the list of hits the
picture is cut to. Usage: python3 claude-edit/music.py [out_dir]
"""
import json
import os
import sys

import numpy as np
import soundfile as sf
from scipy import signal
from scipy.ndimage import minimum_filter1d

SR = 48000
BPM = 130
BEAT = 60 / BPM
S16 = BEAT / 4
BEATS = 74
LENGTH = BEATS * BEAT + 0.6
N = int(LENGTH * SR)
rng = np.random.default_rng(20261027)

SECTIONS = {'intro': [0, 8], 'build': [8, 12], 'drop1': [12, 36], 'break': [36, 44],
            'drop2': [44, 68], 'outro': [68, 74]}
QUESTION = {'t0': 0.62, 'cps': 11.5, 'text': 'Claude, kim właściwie jesteś?'}
SEND_BEAT = 7.5
GAPS = [(11.5, 12), (43.5, 44), (67.5, 68)]
TAPESTOP = (67, 67.5)


def T(beat):
    return beat * BEAT


def hz(note):
    names = {'C': 0, 'C#': 1, 'Db': 1, 'D': 2, 'Eb': 3, 'E': 4, 'F': 5, 'F#': 6, 'Gb': 6,
             'G': 7, 'Ab': 8, 'A': 9, 'Bb': 10, 'B': 11}
    midi = 12 * (int(note[-1]) + 1) + names[note[:-1]]
    return 440 * 2 ** ((midi - 69) / 12)


# ---------- dsp helpers ----------
def filt(x, kind, f, order=2):
    return signal.sosfilt(signal.butter(order, f, btype=kind, fs=SR, output='sos'), x, axis=-1)


def svf(x, fc, q=0.7, mode='lp'):
    """Chamberlin state-variable filter with a per-sample cutoff (used for sweeps)."""
    fc = np.broadcast_to(np.asarray(fc, float), x.shape)
    f = 2 * np.sin(np.pi * np.minimum(fc, SR / 6.5) / SR)
    damp = 1 / q
    low = band = 0.0
    out = np.empty_like(x)
    for i in range(len(x)):
        low += f[i] * band
        high = x[i] - low - damp * band
        band += f[i] * high
        out[i] = low if mode == 'lp' else band if mode == 'bp' else high
    return out


def saw(f, t, ph=0.0):
    """PolyBLEP sawtooth (fixed frequency)."""
    dt = f / SR
    p = (f * t + ph) % 1.0
    y = 2 * p - 1
    m = p < dt
    x = p[m] / dt
    y[m] -= x + x - x * x - 1
    m = p > 1 - dt
    x = (p[m] - 1) / dt
    y[m] -= x * x + x + x + 1
    return y


def square(f, t):
    """Band-limited square from odd harmonics."""
    y = np.zeros_like(t)
    k = 1
    while f * k < 15000:
        y += np.sin(2 * np.pi * f * k * t) / k
        k += 2
    return y * 4 / np.pi


def seconds(d):
    n = int(d * SR)
    return n, np.arange(n) / SR


def widen(x, ms=11):
    d = int(ms * SR / 1000)
    r = np.concatenate([np.zeros(d), x[:-d]])
    return np.stack([x, 0.82 * x + 0.35 * r])


# ---------- instruments ----------
def kick(drive=3.2):
    n, t = seconds(0.42)
    f = 47 + 165 * np.exp(-t * 34) + 70 * np.exp(-t * 260)
    body = np.sin(2 * np.pi * np.cumsum(f) / SR) * (0.85 * np.exp(-t * 8.5) + 0.15 * np.exp(-t * 2))
    body *= np.clip((0.42 - t) / 0.04, 0, 1)
    click = filt(rng.standard_normal(n), 'highpass', 2600) * np.exp(-t * 330) * 0.55
    return np.tanh(drive * (body + click)) / np.tanh(drive)


def clap():
    n, t = seconds(0.42)
    env = np.zeros(n)
    for off in (0, 0.0105, 0.021):
        env += (t >= off) * np.exp(-np.maximum(t - off, 0) / 0.0055)
    env += (t >= 0.03) * np.exp(-np.maximum(t - 0.03, 0) / 0.13) * 0.85
    x = filt(rng.standard_normal(n) * env, 'bandpass', [850, 4200]) * 1.9
    x += np.sin(2 * np.pi * 195 * t) * np.exp(-t / 0.045) * 0.4
    return np.tanh(1.6 * x) / np.tanh(1.6)


def snare(v=1.0):
    n, t = seconds(0.2)
    x = filt(rng.standard_normal(n), 'bandpass', [1100, 7500]) * np.exp(-t / 0.075) * 1.2
    x += np.sin(2 * np.pi * (215 - 60 * t) * t) * np.exp(-t / 0.035) * 0.6
    return np.tanh(1.4 * x * v)


HAT_F = [205.3, 304.4, 369.6, 522.7, 540.0, 800.0]


def hat(decay=0.035):
    n, t = seconds(decay * 7 + 0.01)
    x = sum(np.sign(np.sin(2 * np.pi * f * 3.1 * t + i)) for i, f in enumerate(HAT_F))
    x = filt(x, 'highpass', 7200, 4) + 0.35 * filt(rng.standard_normal(n), 'highpass', 9000)
    return x * np.exp(-t / decay) * 0.5


def perc():
    n, t = seconds(0.08)
    x = np.sin(2 * np.pi * 1650 * t) * np.exp(-t / 0.012) + 0.4 * filt(rng.standard_normal(n), 'highpass', 4000) * np.exp(-t / 0.008)
    return x


def crash(dur=2.4):
    n, t = seconds(dur)
    x = np.stack([rng.standard_normal(n), rng.standard_normal(n)])
    metal = sum(np.sign(np.sin(2 * np.pi * f * 7.3 * t + i)) for i, f in enumerate(HAT_F))
    x = filt(x + 0.25 * metal, 'highpass', 3800, 2)
    x = filt(x, 'lowpass', 13000, 2)
    env = 0.7 * np.exp(-t / 0.55) + 0.3 * np.exp(-t / 0.08)
    return x * env * 0.35


def impact():
    n, t = seconds(2.8)
    f = 27 + 70 * np.exp(-t * 3.2)
    sub = np.tanh(2.6 * np.sin(2 * np.pi * np.cumsum(f) / SR)) * np.exp(-t * 1.35)
    thump = filt(rng.standard_normal(n), 'lowpass', 170) * np.exp(-t * 8) * 3.0
    x = sub * 0.95 + thump * 0.55
    return x * np.minimum(1, t / 0.002)


def whoosh(dur=0.5, up=False):
    n, t = seconds(dur)
    p = t / dur
    fc = 600 * (12 ** p) if up else 7000 * (1 / 12) ** p
    x = svf(rng.standard_normal(n), fc, q=1.6, mode='bp')
    env = np.sin(np.pi * p) ** 1.5
    return widen(x * env * 0.5)


def riser(dur):
    n, t = seconds(dur)
    p = t / dur
    nz = svf(rng.standard_normal(n), 320 * (7200 / 320) ** (p ** 1.4), q=2.2, mode='bp') * p ** 2.2
    ft = 170 * 5.0 ** (p ** 1.25)
    ph = 2 * np.pi * np.cumsum(ft * (1 + 0.012 * np.sin(2 * np.pi * (4 + 14 * p) * t))) / SR
    tone = (np.sin(ph) + 0.4 * np.sin(2 * ph) + 0.2 * np.sin(3 * ph)) * p ** 1.8 * 0.22
    return widen(nz * 0.9 + tone)


def tick():
    n, t = seconds(0.035)
    x = filt(rng.standard_normal(n), 'bandpass', [1800, 6500]) * np.exp(-t * 420) * 0.8
    x += np.sin(2 * np.pi * (650 + 250 * rng.random()) * t) * np.exp(-t * 240) * 0.35
    return x


def pop():
    n, t = seconds(0.12)
    f = 780 + 900 * (1 - np.exp(-t * 40))
    return np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t / 0.035) * 0.5


def bell(f, dur=1.8, bright=1.0):
    n, t = seconds(dur)
    idx = 3.2 * bright * np.exp(-t * 6) + 0.5
    x = np.sin(2 * np.pi * f * t + idx * np.sin(2 * np.pi * f * 3.5 * t))
    x += 0.3 * np.sin(2 * np.pi * f * 2 * t) * np.exp(-t * 3)
    return x * np.minimum(1, t / 0.002) * np.exp(-t * 2.4) * 0.5


def cowbell(f, dur, drive=1.7, cutoff=None):
    n, t = seconds(dur + 0.06)
    x = 0.62 * square(f, t) + 0.45 * square(f * 1.4823, t)
    x = filt(x, 'bandpass', [f * 1.05, min(f * 4.2, 16000)])
    env = np.minimum(1, t / 0.0012) * (0.32 * np.exp(-t / 0.016) + 0.68 * np.exp(-t / 0.17))
    env *= np.clip((dur + 0.05 - t) / 0.05, 0, 1)
    x = np.tanh(drive * x * env * 1.6) / np.tanh(drive)
    if cutoff:
        x = filt(x, 'lowpass', cutoff, 2)
    return x


def pad(freqs, dur, cutoff, voices=5, detune=0.16):
    n, t = seconds(dur)
    L = np.zeros(n)
    R = np.zeros(n)
    for f in freqs:
        for v in range(voices):
            k = (v - (voices - 1) / 2) / ((voices - 1) / 2)
            s = saw(f * 2 ** (k * detune / 12), t, rng.random())
            a = (k + 1) * np.pi / 4
            L += s * np.cos(a)
            R += s * np.sin(a)
    x = np.stack([L, R]) / (len(freqs) * voices ** 0.5)
    if np.ndim(cutoff) == 0:
        return filt(x, 'lowpass', cutoff, 2)
    return np.stack([svf(x[0], cutoff, 0.8), svf(x[1], cutoff, 0.8)])


def bass_808(notes, drive):
    """notes: (start_s, dur_s, freq, glide_from_freq or None, glide_s[, gain, decay_s, swell])."""
    out = np.zeros(N)
    for t0, dur, f1, f0, g, *opt in notes:
        gain, decay, swell = (opt + [1.0, 0.0, False][len(opt):])[:3]
        n, t = seconds(dur + 0.03)
        if f0:
            f = f0 * (f1 / f0) ** np.clip(t / g, 0, 1)
        else:
            f = np.full(n, f1)
        f = f * (1 + 0.5 * np.exp(-t * 70))
        x = np.sin(2 * np.pi * np.cumsum(f) / SR)
        amp = np.minimum(1, t / 0.003) * (0.78 + 0.22 * np.exp(-t * 3.5)) * np.clip((dur + 0.03 - t) / 0.03, 0, 1)
        if decay:
            amp *= np.exp(-t / decay)
        if swell:
            amp *= np.clip(t / dur, 0, 1) ** 1.5
        x *= amp
        y = (np.tanh(x * drive) + 0.12 * np.tanh(x * drive * 4)) * gain
        y += filt(np.tanh(x * drive * 6), 'bandpass', [180, 1400]) * 0.32 * gain
        i0 = int(round(t0 * SR))
        m = min(n, N - i0)
        out[i0:i0 + m] += y[:m]
    return filt(out, 'lowpass', 3600, 2) * 0.8


# ---------- mixing ----------
buses = {k: np.zeros((2, N)) for k in ('drums', 'bass', 'lead', 'pads', 'fx', 'ui', 'send', 'gapfx')}
ev = {k: [] for k in ('kick', 'clap', 'snare', 'hat', 'crash', 'impact', 'cow', 'bass', 'tick', 'whoosh', 'heart')}


def place(bus, x, t, gain=1.0, pan=0.0, rev=0.0):
    if x.ndim == 1:
        a = (pan + 1) * np.pi / 4
        x = np.stack([x * np.cos(a), x * np.sin(a)]) * np.sqrt(2)
    i0 = int(round(t * SR))
    if i0 < 0:
        x, i0 = x[:, -i0:], 0
    m = min(x.shape[1], N - i0)
    if m <= 0:
        return
    buses[bus][:, i0:i0 + m] += x[:, :m] * gain
    if rev:
        buses['send'][:, i0:i0 + m] += x[:, :m] * gain * rev


KICK, CLAP = kick(), clap()
RIFF = {
    'A': [(0, 'F5'), (3, 'F5'), (6, 'Ab5'), (8, 'G5'), (10, 'F5'), (12, 'Eb5'), (14, 'C5')],
    'B': [(0, 'Db5'), (3, 'F5'), (6, 'Ab5'), (8, 'G5'), (10, 'E5'), (12, 'C5'), (14, 'E5')],
}
BASS = {
    'A': [(0, 5, 'F1', None), (6, 3, 'F1', None), (10, 6, 'Ab1', 'F1')],
    'B': [(0, 5, 'Db2', 'Ab1'), (6, 2, 'Db2', None), (8, 4, 'C2', None), (12, 2, 'C2', None), (14, 2, 'E2', None)],
}
bass_notes = []


def riff(bar_beat, kind, gain=0.42, cutoff=None, octave=False, drive=1.7, rev=0.35):
    notes = RIFF[kind]
    for i, (st, name) in enumerate(notes):
        nxt = notes[i + 1][0] if i + 1 < len(notes) else 16
        dur = min(nxt - st, 3) * S16
        t0 = T(bar_beat) + st * S16
        f = hz(name)
        c = cutoff(t0) if callable(cutoff) else cutoff
        place('lead', cowbell(f, dur, drive, c), t0, gain, pan=-0.12, rev=rev)
        if octave:
            place('lead', cowbell(f / 2, dur, drive * 1.8, c), t0, gain * 0.55, pan=0.18, rev=rev * 0.6)
        ev['cow'].append([round(t0, 4), round(f, 2)])


def bassline(bar_beat, kind, until=16):
    for st, ln, name, frm in BASS[kind]:
        if st >= until:
            continue
        t0 = T(bar_beat) + st * S16
        bass_notes.append((t0, ln * S16 - 0.01, hz(name), hz(frm) if frm else None, 0.07))
        ev['bass'].append([round(t0, 4), round(ln * S16, 4), round(hz(name), 2)])


def drums_bar(bb, kind, hard, until=16):
    kicks = [0, 3, 6, 10] + ([14] if kind == 'B' else [])
    for s in kicks:
        if s < until:
            place('drums', KICK, T(bb) + s * S16, 0.95)
            ev['kick'].append(round(T(bb) + s * S16, 4))
    for s in (4, 12):
        if s < until:
            place('drums', CLAP, T(bb) + s * S16, 0.72, rev=0.22)
            ev['clap'].append(round(T(bb) + s * S16, 4))
    for s in range(0, min(until, 16), 2):
        if kind == 'B' and s >= 12:
            continue
        v = 0.5 if s % 4 == 2 else 0.3
        place('drums', hat(), T(bb) + s * S16, v, pan=0.25)
        ev['hat'].append(round(T(bb) + s * S16, 4))
    if kind == 'B' and until >= 16:
        for k in range(8):
            place('drums', hat(0.022), T(bb) + (12 + k / 2) * S16, 0.22 + 0.06 * k, pan=0.25)
    if kind == 'A' and until > 14:
        place('drums', hat(0.16), T(bb) + 14 * S16, 0.32, pan=-0.2)
    if hard:
        for s in (7, 15):
            if s < until:
                place('drums', perc(), T(bb) + s * S16, 0.28, pan=0.45, rev=0.2)


def hit(t, big=1.0):
    place('fx', impact(), t, 0.9 * big)
    place('fx', crash(), t, 0.85, rev=0.3)
    ev['impact'].append(round(t, 4))
    ev['crash'].append(round(t, 4))


def drop(b0, bars, hard, last_until=16):
    for bar in range(bars):
        bb = b0 + bar * 4
        kind = 'A' if bar % 2 == 0 else 'B'
        until = last_until if bar == bars - 1 else 16
        drums_bar(bb, kind, hard, until)
        bassline(bb, kind, until)
        riff(bb, kind, gain=0.56 if hard else 0.52, octave=hard, drive=2.4 if hard else 1.7)
        if bar % 4 == 0 and bar > 0:
            place('fx', crash(), T(bb), 0.6, rev=0.3)
            ev['crash'].append(round(T(bb), 4))
        if bar < bars - 1:
            place('fx', whoosh(0.42, up=True), T(bb + 3.55), 0.28)
            ev['whoosh'].append(round(T(bb + 4), 4))


# --- intro + build (beats 0-12) ---
place('fx', impact(), 0.0, 0.35)
fm_add9 = [hz('F3'), hz('Ab3'), hz('C4'), hz('G4'), hz('F2')]
n_pad = int(T(11.5) * SR)
sweep = 320 * (2600 / 320) ** (np.arange(n_pad) / n_pad) ** 1.6
p = pad(fm_add9, T(11.5), sweep)
p *= np.minimum(1, np.arange(n_pad) / (2.2 * SR))
p[:, -480:] *= np.linspace(1, 0, 480)
place('pads', p, 0.0, 0.5, rev=0.5)
for bar, kind in ((0, 'A'), (4, 'B')):
    for st, name in RIFF[kind]:
        place('lead', filt(bell(hz(name)), 'lowpass', 3200), T(bar) + st * S16, 0.17, pan=0.2, rev=0.75)
for k in range(1, len(QUESTION['text']) + 1):
    if QUESTION['text'][k - 1] != ' ':
        tt = QUESTION['t0'] + k / QUESTION['cps']
        place('ui', tick(), tt, 0.33, pan=0.1 * np.sin(k))
        ev['tick'].append(round(tt, 4))
place('ui', pop(), T(SEND_BEAT), 0.5, rev=0.2)
for b in (4, 4.75, 6, 6.75):
    place('drums', filt(KICK, 'lowpass', 160), T(b), 0.45)
    ev['heart'].append(round(T(b), 4))
place('fx', riser(T(11.5) - T(8)), T(8), 0.5)
for b, v in [(8, .25), (9, .3), (10, .38), (10.5, .44)] + [(11 + k / 4, .55 + .1 * k) for k in range(2)]:
    place('drums', snare(), T(b), v, rev=0.25)
    ev['snare'].append(round(T(b), 4))
for k in range(14):
    b = 8 + k / 4
    name = ['F5', 'Ab5', 'C6', 'Eb6'][k % 4]
    place('lead', bell(hz(name) * (2 if k >= 12 else 1), 0.7, 0.6), T(b), 0.07 + 0.006 * k, pan=0.4 * np.sin(k), rev=0.6)
bass_notes.append((T(8), T(11.45) - T(8), hz('F1'), hz('C1'), T(3), 0.45, 0.0, True))
place('gapfx', crash()[:, ::-1][:, -int(1.0 * SR):], T(12) - 1.0, 0.75)

# --- drop 1 (12-36) ---
hit(T(12))
drop(12, 6, hard=False)

# --- break (36-44) ---
for b in (36, 37.5, 39.25):
    place('drums', KICK, T(b), 0.85)
    ev['kick'].append(round(T(b), 4))
place('drums', CLAP, T(38), 0.6, rev=0.45)
ev['clap'].append(round(T(38), 4))
for b in range(36, 40):
    place('drums', hat(), T(b + 0.5), 0.25, pan=0.25)
riff(36, 'A', gain=0.4, cutoff=720, rev=0.6)
riff(40, 'B', gain=0.42, cutoff=lambda t: 720 * (7000 / 720) ** ((t - T(40)) / (T(43.5) - T(40))), rev=0.5)
bass_notes.append((T(36), T(4) - 0.01, hz('F1'), None, 0.0, 0.8, 1.1))
bass_notes.append((T(40), T(3.45), hz('F2'), hz('F1'), T(3.4), 0.5, 0.0, True))
bp_n = int(T(7.5) * SR)
pb = pad(fm_add9, T(7.5), 380 * (3000 / 380) ** (np.arange(bp_n) / bp_n) ** 2)
pb *= np.minimum(1, np.arange(bp_n) / (0.4 * SR))
pb[:, -480:] *= np.linspace(1, 0, 480)
place('pads', pb, T(36), 0.45, rev=0.5)
roll = [(40 + k * 0.5, 0.32 + 0.03 * k) for k in range(4)] + [(42 + k * 0.25, 0.48 + 0.03 * k) for k in range(4)] + \
       [(43 + k * 0.125, 0.62 + 0.04 * k) for k in range(4)]
for b, v in roll:
    place('drums', snare(), T(b), v, rev=0.25)
    ev['snare'].append(round(T(b), 4))
place('fx', riser(T(43.5) - T(40)), T(40), 0.55)
place('gapfx', crash()[:, ::-1][:, -int(1.0 * SR):], T(44) - 1.0, 0.8)

# --- drop 2 (44-68) ---
hit(T(44), 1.15)
drop(44, 5, hard=True)
drums_bar(64, 'A', True, until=8)
bassline(64, 'A', until=8)
riff(64, 'A', gain=0.56, octave=True, drive=2.4)
for k in range(8):
    b = 66 + k * 0.125
    place('drums', snare(), T(b), 0.5 + 0.06 * k, rev=0.15)
    ev['snare'].append(round(T(b), 4))
    if k % 2 == 0:
        place('drums', KICK, T(b), 0.8)
        ev['kick'].append(round(T(b), 4))
place('fx', riser(T(1)), T(66), 0.35)

# --- outro (68-74) ---
hit(T(68), 1.25)
bass_notes.append((T(68), T(3.2), hz('F1'), None, 0.0, 1.0, 0.75))
place('lead', cowbell(hz('F5'), 0.6, 2.2), T(68), 0.5, rev=0.9)
place('lead', cowbell(hz('F4'), 0.6, 3.0), T(68), 0.3, rev=0.7)
place('lead', filt(bell(hz('C6'), 3.0, 0.5), 'lowpass', 4000), T(70), 0.12, pan=-0.3, rev=0.8)
po_n = int(T(6) * SR)
po = pad(fm_add9, T(6), 1400)
po *= np.minimum(1, np.arange(po_n) / (0.05 * SR)) * np.linspace(1, 0, po_n) ** 1.5
place('pads', po, T(68), 0.42, rev=0.6)

# ---------- render buses ----------
bass = bass_808(bass_notes, drive=2.6)
buses['bass'] += np.stack([bass, bass])

t_all = np.arange(N) / SR
sc = np.ones(N)
for tk in ev['kick']:
    i0 = int(tk * SR)
    m = min(N - i0, int(0.35 * SR))
    tt = t_all[:m]
    sc[i0:i0 + m] = np.minimum(sc[i0:i0 + m], 1 - 0.55 * (1 - np.exp(-tt / 0.004)) * np.exp(-tt / 0.12))
buses['pads'] *= sc
buses['lead'] *= 1 - 0.25 * (1 - sc)
buses['bass'] *= 1 - 0.3 * (1 - sc)

ir_n, ir_t = seconds(2.6)
ir = rng.standard_normal((2, ir_n))
ir = filt(ir, 'highpass', 160) * np.exp(-ir_t / 0.3) * 0.5 + filt(ir, 'lowpass', 3200) * np.exp(-ir_t / 0.62)
ramp = int(0.01 * SR)
ir[:, :ramp] *= np.linspace(0, 1, ramp)
ir /= np.sqrt(np.sum(ir ** 2) / 2)
send = filt(buses['send'], 'highpass', 220)
verb = np.stack([signal.fftconvolve(send[c], ir[c])[:N] for c in range(2)])
verb = filt(verb, 'lowpass', 9000) * 0.32

drums = np.tanh(buses['drums'] * 1.25) / 1.25
mix = drums + buses['bass'] * 0.95 + buses['lead'] + buses['pads'] + buses['fx'] + buses['ui'] + verb
mix = filt(mix, 'highpass', 24, 2)
mix -= 0.3 * filt(mix, 'highpass', 9500, 1)

# tape stop (slows the whole mix to a halt), then the gaps of silence before each drop
t0, t1 = T(TAPESTOP[0]), T(TAPESTOP[1])
i0, i1 = int(t0 * SR), int(t1 * SR)
u = np.arange(i1 - i0) / (i1 - i0)
src = i0 + (i1 - i0) * (u - u ** 2.5 / 2.5)
for c in range(2):
    mix[c, i0:i1] = np.interp(src, np.arange(N), mix[c]) * (1 - u ** 3)
gate = np.ones(N)
fade = int(0.006 * SR)
for a, b in GAPS:
    ia, ib = int(T(a) * SR), int(T(b) * SR)
    gate[ia:ib] = 0
    gate[ia - fade:ia] = np.minimum(gate[ia - fade:ia], np.linspace(1, 0, fade))
mix *= gate
mix += buses['gapfx']

# master: loudness, soft clip, look-ahead limiter
drop_rms = np.sqrt(np.mean(mix[:, int(T(12) * SR):int(T(36) * SR)] ** 2))
mix *= 10 ** (-8.5 / 20) / drop_rms
mix = np.tanh(mix * 1.15) / 1.15
ceiling = 10 ** (-1.0 / 20)
need = np.minimum(1.0, ceiling / np.maximum(np.max(np.abs(mix), axis=0), 1e-9))
look = int(0.004 * SR)
g = minimum_filter1d(need, size=2 * look + 1, mode='nearest')
rel = np.exp(-1 / (0.08 * SR))
gs = np.empty_like(g)
cur = 1.0
for i in range(N):
    cur = g[i] if g[i] < cur else g[i] + (cur - g[i]) * rel
    gs[i] = cur
gs = np.convolve(gs, np.ones(look) / look, mode='same')
mix *= np.minimum(gs, need)
fo = int(1.2 * SR)
mix[:, -fo:] *= np.linspace(1, 0, fo) ** 2
mix = np.clip(mix, -ceiling, ceiling)

out_dir = sys.argv[1] if len(sys.argv) > 1 else os.path.dirname(os.path.abspath(__file__))
sf.write(os.path.join(out_dir, 'soundtrack.wav'), mix.T, SR, subtype='PCM_24')
events = {
    'bpm': BPM, 'beat': BEAT, 'beats': BEATS, 'duration': round(T(BEATS), 4),
    'sections': SECTIONS, 'question': QUESTION, 'send': round(T(SEND_BEAT), 4),
    'gaps': [[round(T(a), 4), round(T(b), 4)] for a, b in GAPS],
    'tapestop': [round(t0, 4), round(t1, 4)],
    'risers': [[round(T(8), 4), round(T(11.5), 4)], [round(T(40), 4), round(T(43.5), 4)]],
    **{k: sorted(v) for k, v in ev.items()},
}
with open(os.path.join(out_dir, 'events.json'), 'w') as fh:
    json.dump(events, fh)
peak = 20 * np.log10(np.max(np.abs(mix)))
print(f'soundtrack.wav: {LENGTH:.2f} s, peak {peak:.2f} dBFS, drop RMS '
      f'{20 * np.log10(np.sqrt(np.mean(mix[:, int(T(12) * SR):int(T(36) * SR)] ** 2))):.1f} dBFS')
