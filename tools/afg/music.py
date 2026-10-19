"""Original Afghan-style instrumental music, synthesised from scratch.

Instruments: rubab / dutar (Karplus-Strong plucked strings with sympathetic
resonance), tabla & zerbaghali (pitched membrane hits), dhol, harmonium
drone, nay flute. Melodies are generated from Afghan/Hindustani modes with
motif repetition and rhythmic cycles (keherwa 8, dadra 6, mughulchi 7).
Everything here is original and free to redistribute with the mod.
"""
import os
import shutil
import subprocess
import wave

import numpy as np
from scipy.signal import lfilter

SR = 32000

MODES = {  # semitone steps from Sa
    "kafi": [0, 2, 3, 5, 7, 9, 10],
    "bhairavi": [0, 1, 3, 5, 7, 8, 10],
    "bhairav": [0, 1, 4, 5, 7, 8, 11],
    "yaman": [0, 2, 4, 6, 7, 9, 11],
    "pilu": [0, 2, 3, 5, 7, 8, 11],
}

# rhythm cycles: list of (stroke, beat position) per cycle, stroke in "D" bass, "T" treble, "K" muted, "N" ring
TAALS = {
    "keherwa": (8, [("D", 0), ("T", 1), ("N", 2), ("T", 3), ("K", 4), ("T", 5), ("D", 6), ("T", 7)]),
    "dadra": (6, [("D", 0), ("T", 1), ("N", 2), ("K", 3), ("T", 4), ("N", 5)]),
    "mughulchi": (7, [("D", 0), ("T", 1), ("T", 2), ("D", 3), ("N", 4), ("D", 5), ("T", 6)]),
    "attan": (8, [("D", 0), ("K", 1), ("T", 2), ("D", 3), ("D", 4), ("K", 5), ("T", 6), ("N", 7)]),
}


def hz(semi, base=220.0):
    return base * 2 ** (semi / 12)


# ----------------------------------------------------------------- instruments

def pluck(f, dur, bright=0.6, decay=0.996, rng=None):
    """Karplus-Strong string via an IIR comb filter."""
    rng = rng or np.random.default_rng()
    n = int(dur * SR)
    p = max(int(round(SR / f - 0.5)), 2)  # averaging filter adds half a sample of delay
    exc = rng.uniform(-1, 1, p)
    # brighter pluck = less smoothing of the excitation
    exc = lfilter([bright, 1 - bright], [1], exc)
    x = np.zeros(n)
    x[:p] = exc
    # y[n] = x[n] + decay * (y[n-p] + y[n-p-1]) / 2
    den = np.zeros(p + 2)
    den[0] = 1
    den[p] = den[p + 1] = -decay * 0.5
    y = lfilter([1], den, x)
    env = np.minimum(1, np.linspace(0, 40, n)) * np.exp(-np.linspace(0, dur * 1.2, n))
    return y * env * 0.5


def rubab_note(f, dur, rng):
    y = pluck(f, dur, bright=0.75, decay=0.997, rng=rng)
    y += 0.35 * pluck(f * 2, dur, bright=0.9, decay=0.994, rng=rng)  # paired course an octave up
    return y


def dutar_note(f, dur, rng):
    return pluck(f, dur, bright=0.5, decay=0.995, rng=rng) * 0.9


def membrane(f0, f1, dur, noise=0.2, decay=18.0, rng=None):
    rng = rng or np.random.default_rng()
    n = int(dur * SR)
    t = np.arange(n) / SR
    f = f1 + (f0 - f1) * np.exp(-t * 30)
    ph = 2 * np.pi * np.cumsum(f) / SR
    y = np.sin(ph) * np.exp(-t * decay)
    y += noise * rng.uniform(-1, 1, n) * np.exp(-t * 60)
    return y


def _mix(*sigs):
    out = np.zeros(max(len(s) for s in sigs))
    for s in sigs:
        out[:len(s)] += s
    return out


def tabla(stroke, rng):
    if stroke == "D":  # dayan + bayan together (dha)
        return _mix(0.9 * membrane(150, 90, 0.6, 0.1, 6, rng), 0.5 * membrane(700, 680, 0.4, 0.3, 14, rng))
    if stroke == "T":  # na / ta - ringing treble
        return _mix(0.6 * membrane(720, 700, 0.35, 0.25, 12, rng), 0.2 * membrane(1440, 1400, 0.2, 0, 20, rng))
    if stroke == "N":  # tin - open ring
        return 0.5 * membrane(700, 690, 0.5, 0.1, 8, rng)
    return 0.4 * membrane(400, 300, 0.12, 0.6, 40, rng)  # muted ke / ka


def dhol(stroke, rng):
    if stroke in ("D", "N"):
        return 1.0 * membrane(110, 55, 0.5, 0.25, 9, rng)
    return 0.5 * membrane(500, 380, 0.15, 0.7, 30, rng)


def drone(freqs, dur, rng):
    """Harmonium-ish reed drone: detuned band-limited saws, slow swell."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    out = np.zeros(n)
    for f in freqs:
        for det in (-0.6, 0.6):
            ff = f + det
            for k in range(1, 12):
                out += np.sin(2 * np.pi * ff * k * t + rng.uniform(0, 6)) / k * (0.85 ** k)
    swell = 0.75 + 0.25 * np.sin(2 * np.pi * t / 7.0)
    fade = np.minimum(1, t / 3) * np.minimum(1, (dur - t) / 3)
    return out * swell * fade * 0.05


def nay_note(f, dur, rng):
    n = int(dur * SR)
    t = np.arange(n) / SR
    vib = 1 + 0.006 * np.sin(2 * np.pi * 5.2 * t) * np.minimum(1, t / 0.4)
    ph = 2 * np.pi * np.cumsum(f * vib) / SR
    tone = np.sin(ph) + 0.25 * np.sin(2 * ph) + 0.08 * np.sin(3 * ph)
    breath = lfilter([1], [1, -0.95], rng.uniform(-1, 1, n)) * 0.02
    env = np.minimum(1, t / 0.08) * np.minimum(1, (dur - t) / 0.12)
    return (tone + breath) * env * 0.35


# ----------------------------------------------------------------- composition

def motif(rng, length, mode_len):
    """Stepwise melodic cell (scale degrees), Afghan tunes move mostly by step."""
    deg = [int(rng.integers(0, 5))]
    for _ in range(length - 1):
        step = rng.choice([-2, -1, -1, 0, 1, 1, 2, 3], p=[.08, .25, .1, .07, .25, .12, .08, .05])
        deg.append(int(np.clip(deg[-1] + step, -3, mode_len + 4)))
    return deg


def degree_to_semi(d, mode):
    octave, idx = divmod(d, len(mode))
    return mode[idx] + 12 * octave


def place(buf, sig, start):
    i = int(start * SR)
    if i >= len(buf):
        return
    j = min(len(buf), i + len(sig))
    buf[i:j] += sig[:j - i]


def reverb(x, mix=0.25):
    out = x.copy()
    for d, g in ((0.031, 0.5), (0.047, 0.45), (0.071, 0.4), (0.113, 0.3)):
        n = int(d * SR)
        den = np.zeros(n + 1)
        den[0], den[n] = 1, -g
        out += mix * lfilter([1], den, x) * 0.25
    return out


def compose(spec, seed):
    rng = np.random.default_rng(seed)
    mode = MODES[spec["mode"]]
    bpm, taal = spec["bpm"], spec["taal"]
    beats, strokes = TAALS[taal]
    beat = 60 / bpm
    dur = spec["seconds"]
    total_beats = int(dur / beat)
    mix = np.zeros(int((dur + 4) * SR))
    base = spec.get("base", 220.0)

    # drone on Sa + Pa
    if spec.get("drone", True):
        place(mix, drone([base / 2, base / 2 * 1.5], dur + 2, rng), 0)

    # percussion
    perc = tabla if spec.get("perc", "tabla") == "tabla" else dhol
    intro_beats = beats * 2
    for b in range(intro_beats, total_beats):
        pos = b % beats
        for st, sb in strokes:
            if sb == pos:
                hit = perc(st, rng) * (0.9 + 0.2 * rng.random())
                place(mix, hit * spec.get("perc_gain", 0.5), b * beat + rng.normal(0, 0.004))
                # fill: extra eighth notes near the end of a cycle
                if pos == beats - 1 and rng.random() < 0.4:
                    place(mix, perc("T", rng) * 0.3, (b + 0.5) * beat)

    # melody: phrases built from motifs: A A B A' structure, repeated with variation
    inst = {"rubab": rubab_note, "dutar": dutar_note, "nay": nay_note}[spec["lead"]]
    sub = spec.get("subdiv", 2)
    phrase_len = beats * sub
    A = motif(rng, phrase_len, len(mode))
    B = motif(rng, phrase_len, len(mode))
    B = [d + 2 for d in B]
    form = ["A", "A", "B", "A2"]
    t_beat = beats  # start after one cycle of drone
    cycle = 0
    while t_beat < total_beats - beats:
        part = form[cycle % len(form)]
        cell = A if part in ("A", "A2") else B
        if part == "A2":
            cell = cell[:-2] + [0, 0]  # cadence on Sa
        if cycle >= 8 and rng.random() < 0.3:  # later variation
            cell = [d + int(rng.choice([-1, 0, 0, 1])) for d in cell]
        for k, d in enumerate(cell):
            if rng.random() < spec.get("rest", 0.12) and k % sub:
                continue
            start = (t_beat + k / sub) * beat
            note_len = beat / sub * (3 if spec["lead"] == "nay" else 4)
            f = hz(degree_to_semi(d, mode), base)
            sig = inst(f, note_len, rng)
            place(mix, sig * spec.get("lead_gain", 0.6), start)
            # rubab ornament: quick grace note from above
            if spec["lead"] == "rubab" and rng.random() < 0.12:
                g = hz(degree_to_semi(d + 1, mode), base)
                place(mix, inst(g, 0.08, rng) * 0.3, start - 0.05)
        t_beat += beats
        cycle += 1

    # sympathetic strings shimmer for rubab tunes
    if spec["lead"] == "rubab":
        mix = reverb(mix, 0.35)
    else:
        mix = reverb(mix, 0.2)
    n = int(dur * SR)
    mix = mix[:n]
    fade = np.minimum(1, np.arange(n) / (2 * SR)) * np.minimum(1, (n - np.arange(n)) / (4 * SR))
    mix *= fade
    mix /= np.abs(mix).max() + 1e-9
    return (mix * 0.85 * 32767).astype(np.int16)


TRACKS = [
    dict(name="01_shomali_morning", title="Shomali Morning", mode="kafi", taal="keherwa", bpm=104,
         lead="rubab", seconds=150),
    dict(name="02_mughulchi", title="Mughulchi of the Pass", mode="bhairav", taal="mughulchi", bpm=150,
         lead="rubab", seconds=140, base=233.08),
    dict(name="03_valley_night", title="Night in Dara-e-Sabz", mode="bhairavi", taal="dadra", bpm=72,
         lead="nay", seconds=170, subdiv=1, perc_gain=0.3, rest=0.2),
    dict(name="04_bazaar_run", title="Bazaar Run", mode="pilu", taal="dadra", bpm=138, lead="dutar",
         seconds=130, perc="dhol", perc_gain=0.55),
    dict(name="05_mountain_road", title="Mountain Road", mode="yaman", taal="keherwa", bpm=92, lead="rubab",
         seconds=160, base=196.0),
    dict(name="06_attan", title="Attan", mode="kafi", taal="attan", bpm=128, lead="dutar", seconds=140,
         perc="dhol", perc_gain=0.7, base=246.94),
]


def build(out_dir, log=print):
    os.makedirs(out_dir, exist_ok=True)
    ffmpeg = shutil.which("ffmpeg")
    names = []
    for i, spec in enumerate(TRACKS):
        pcm = compose(spec, seed=1000 + i)
        wav = os.path.join(out_dir, spec["name"] + ".wav")
        with wave.open(wav, "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(SR)
            w.writeframes(pcm.tobytes())
        final = wav
        if ffmpeg:
            mp3 = wav[:-4] + ".mp3"
            subprocess.run([ffmpeg, "-y", "-loglevel", "error", "-i", wav, "-codec:a", "libmp3lame", "-b:a", "128k",
                            "-metadata", f"title={spec['title']}", "-metadata", "artist=Afghanistan mod (generated)",
                            mp3], check=True)
            os.remove(wav)
            final = mp3
        names.append(os.path.basename(final))
        log(f"  track {spec['title']:28s} {spec['seconds']}s -> {os.path.basename(final)}")
    with open(os.path.join(out_dir, "playlist.txt"), "w", newline="\r\n") as f:
        f.write("\n".join(names) + "\n")
    return names
