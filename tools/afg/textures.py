"""Procedural, seamlessly tiling textures (numpy only)."""
import numpy as np

S = 256


def _noise(freq, seed, size=S):
    """Tileable value noise in [0,1] with `freq` cells per side."""
    rng = np.random.default_rng(seed)
    g = rng.random((freq, freq))
    t = np.arange(size) * freq / size
    i0 = np.floor(t).astype(int)
    f = t - i0
    f = f * f * (3 - 2 * f)
    i1 = (i0 + 1) % freq
    a = g[i0][:, i0]; b = g[i0][:, i1]
    c = g[i1][:, i0]; d = g[i1][:, i1]
    fx, fy = f[None, :], f[:, None]
    return (a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy


def fbm(seed, base=4, octaves=5, gain=0.5, size=S):
    out = np.zeros((size, size))
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        out += amp * _noise(base * 2 ** o, seed + o, size)
        tot += amp
        amp *= gain
    return out / tot


def _rgb(base, var, n):
    """base colour (0-255), per-channel variation scale, noise field in [0,1] -> HxWx3."""
    base = np.asarray(base, np.float64)
    var = np.asarray(var, np.float64)
    return base[None, None, :] + (n[..., None] - 0.5) * var[None, None, :]


def _finish(rgb, alpha=None):
    rgb = np.clip(rgb, 0, 255).astype(np.uint8)
    a = np.full(rgb.shape[:2] + (1,), 255, np.uint8) if alpha is None else \
        np.clip(alpha, 0, 255).astype(np.uint8)[..., None]
    return np.concatenate([rgb, a], -1)


def speckle(seed, density, size=S):
    rng = np.random.default_rng(seed)
    return (rng.random((size, size)) < density).astype(np.float64)


def sand():
    n = fbm(1, 4) * 0.7 + fbm(2, 32, 3) * 0.3
    rgb = _rgb((196, 170, 128), (60, 55, 45), n)
    rgb -= speckle(3, 0.03)[..., None] * 35
    return _finish(rgb)


def dirt():
    n = fbm(4, 4) * 0.6 + fbm(5, 32, 3) * 0.4
    rgb = _rgb((150, 120, 88), (70, 60, 50), n)
    rgb -= speckle(6, 0.05)[..., None] * 30
    return _finish(rgb)


def grass():
    n = fbm(7, 4)
    m = fbm(8, 16, 4)
    tufts = speckle(9, 0.12) * fbm(10, 64, 2)
    rgb = _rgb((150, 140, 90), (60, 50, 40), n)
    green = np.array([118, 128, 70])
    w = np.clip((m - 0.45) * 3, 0, 1)[..., None]
    rgb = rgb * (1 - w) + green * w
    rgb += tufts[..., None] * 25
    return _finish(rgb)


def field():
    """Irrigated wheat field rows (rows along V)."""
    x = np.arange(S)
    rows = 0.5 + 0.5 * np.sin(x / S * 2 * np.pi * 16)
    n = fbm(11, 8, 4)
    base = np.array([96, 128, 52]) * (0.8 + 0.3 * n[..., None])
    soil = np.array([120, 96, 64]) * (0.85 + 0.2 * n[..., None])
    w = (rows[None, :] > 0.25)[..., None] * (0.7 + 0.3 * fbm(12, 32, 2))[..., None]
    return _finish(soil * (1 - w) + base * w)


def rock():
    n = fbm(13, 4, 6, 0.55)
    cracks = np.abs(fbm(14, 8, 4) - 0.5) < 0.012
    strata = 0.5 + 0.5 * np.sin(np.arange(S)[:, None] / S * 2 * np.pi * 6 + fbm(15, 4) * 6)
    rgb = _rgb((128, 116, 102), (90, 80, 70), n * 0.75 + strata * 0.25)
    rgb[cracks] *= 0.55
    return _finish(rgb)


def snow():
    n = fbm(16, 4)
    rgb = _rgb((232, 236, 242), (30, 28, 20), n)
    rgb -= speckle(17, 0.02)[..., None] * 40
    return _finish(rgb)


def road():
    """Packed dirt track."""
    n = fbm(18, 4) * 0.6 + fbm(19, 32, 3) * 0.4
    rgb = _rgb((168, 140, 104), (50, 45, 40), n)
    rgb -= speckle(20, 0.04)[..., None] * 25
    return _finish(rgb)


def mudbrick():
    """Adobe plaster with faint brick courses."""
    n = fbm(21, 4) * 0.7 + fbm(22, 32, 3) * 0.3
    rgb = _rgb((176, 140, 100), (50, 45, 40), n)
    y = np.arange(S)
    course = (y % 32 < 2)[:, None]
    stag = ((y // 32) % 2)[:, None] * 32
    xs = (np.arange(S)[None, :] + stag) % 64 < 2
    brick = (course | xs) & (fbm(23, 4) > 0.55)
    rgb[brick] *= 0.82
    rgb *= (0.92 + 0.1 * (fbm(24, 2) > 0.6))[..., None]
    return _finish(rgb)


def mudroof():
    n = fbm(25, 8)
    straw = speckle(26, 0.15) * fbm(27, 64, 2)
    rgb = _rgb((160, 132, 96), (40, 35, 30), n) + straw[..., None] * 30
    return _finish(rgb)


def plaster():
    n = fbm(28, 4)
    rgb = _rgb((222, 216, 200), (30, 30, 30), n)
    stains = np.clip((fbm(29, 4) - 0.6) * 3, 0, 1)
    rgb -= stains[..., None] * np.array([40, 45, 55])
    return _finish(rgb)


def dome_tile():
    """Turquoise/blue glazed tile."""
    y, x = np.mgrid[0:S, 0:S]
    grid = ((x % 32) < 2) | ((y % 32) < 2)
    n = fbm(30, 8)
    rgb = _rgb((40, 150, 170), (30, 50, 50), n)
    pattern = (np.sin(x / 32 * 2 * np.pi) * np.sin(y / 32 * 2 * np.pi)) > 0.6
    rgb[pattern] = rgb[pattern] * 0.6 + np.array([230, 220, 180]) * 0.4
    rgb[grid] = (200, 196, 180)
    return _finish(rgb)


def wood():
    y, x = np.mgrid[0:S, 0:S]
    grain = fbm(31, 4) * 6 + x / S * 40
    rgb = _rgb((120, 84, 52), (40, 30, 20), 0.5 + 0.5 * np.sin(grain))
    planks = (x % 64) < 3
    rgb[planks] *= 0.5
    return _finish(rgb)


def stone():
    """Dry-stone masonry."""
    rng = np.random.default_rng(32)
    y, x = np.mgrid[0:S, 0:S]
    row = y // 32
    off = (row % 2) * 24
    col = (x + off) // 48 % 6
    shade = rng.random((8, 6))[row % 8, col] * 0.35 + 0.65
    joint = ((y % 32) < 3) | (((x + off) % 48) < 3)
    n = fbm(33, 8)
    rgb = _rgb((150, 138, 120), (50, 45, 40), n) * shade[..., None]
    rgb[joint] = rgb[joint] * 0.45
    return _finish(rgb)


def hesco():
    y, x = np.mgrid[0:S, 0:S]
    mesh = ((x % 32) < 2) | ((y % 32) < 2)
    n = fbm(34, 8)
    rgb = _rgb((176, 156, 120), (40, 35, 30), n)
    rgb[mesh] = (110, 110, 100)
    return _finish(rgb)


def concrete():
    n = fbm(35, 8) * 0.6 + fbm(36, 64, 2) * 0.4
    rgb = _rgb((165, 160, 150), (40, 40, 40), n)
    return _finish(rgb)


def metal():
    x = np.arange(S)
    ridges = 0.5 + 0.5 * np.sin(x / S * 2 * np.pi * 16)
    rust = np.clip((fbm(37, 4) - 0.5) * 3, 0, 1)
    rgb = np.array([150, 152, 150]) * (0.75 + 0.25 * ridges)[None, :, None] * np.ones((S, 1, 1))
    rgb = rgb * (1 - rust[..., None]) + np.array([130, 80, 50]) * rust[..., None]
    return _finish(rgb)


def canvas(base=(110, 112, 76)):
    n = fbm(38, 8) * 0.5 + fbm(39, 64, 2) * 0.5
    return _finish(_rgb(base, (30, 30, 25), n))


def leaves():
    n = fbm(40, 16, 4)
    holes = fbm(41, 32, 3)
    rgb = _rgb((90, 120, 50), (60, 70, 40), n)
    rgb[holes > 0.62] *= 0.55
    return _finish(rgb)


def bark():
    x = np.arange(S)
    n = fbm(42, 4)
    stripes = 0.5 + 0.5 * np.sin(x / S * 2 * np.pi * 10 + fbm(43, 4) * 4)
    rgb = _rgb((110, 100, 84), (50, 45, 40), n * 0.5 + stripes * 0.5)
    return _finish(rgb)


def window():
    y, x = np.mgrid[0:S, 0:S]
    rgb = np.zeros((S, S, 3)) + 30
    frame = (x < 16) | (x > S - 16) | (y < 16) | (y > S - 16) | (np.abs(x - S // 2) < 6)
    rgb[frame] = (100, 70, 45)
    return _finish(rgb)


def door():
    rgb = wood()[..., :3].astype(np.float64)
    rgb = rgb * np.array([0.7, 0.85, 1.1])  # painted blue-green weathered door
    rgb = rgb * 0.6 + np.array([40, 90, 100]) * 0.4
    return _finish(rgb)


def awning(c1, c2):
    x = np.arange(S)
    stripes = ((x // 32) % 2)[None, :, None]
    n = fbm(44, 8)[..., None]
    rgb = (np.array(c1) * stripes + np.array(c2) * (1 - stripes)) * (0.85 + 0.25 * n)
    return _finish(rgb)


def sandbag():
    y, x = np.mgrid[0:S, 0:S]
    row = y // 32
    off = (row % 2) * 32
    bx = ((x + off) % 64) / 64
    by = (y % 32) / 32
    bulge = np.sin(bx * np.pi) * np.sin(by * np.pi)
    n = fbm(45, 16)
    rgb = np.array([170, 150, 110])[None, None, :] * (0.55 + 0.45 * bulge[..., None]) * (0.9 + 0.2 * n[..., None])
    return _finish(rgb)


def flag():
    """Fictional provincial police flag: green field, white band, simple star."""
    y, x = np.mgrid[0:S, 0:S]
    rgb = np.zeros((S, S, 3)) + (30, 110, 60)
    band = (y > S * 0.4) & (y < S * 0.6)
    rgb[band] = (235, 235, 230)
    star = np.hypot(x - S * 0.25, y - S * 0.5) < S * 0.08
    rgb[star] = (220, 180, 40)
    return _finish(rgb)


ALL = {
    "afg_sand": sand, "afg_dirt": dirt, "afg_grass": grass, "afg_field": field,
    "afg_rock": rock, "afg_snow": snow, "afg_road": road,
    "afg_mud": mudbrick, "afg_mudroof": mudroof, "afg_plaster": plaster, "afg_dome": dome_tile,
    "afg_wood": wood, "afg_stone": stone, "afg_hesco": hesco, "afg_concrete": concrete,
    "afg_metal": metal, "afg_canvas": canvas, "afg_leaves": leaves, "afg_bark": bark,
    "afg_window": window, "afg_door": door, "afg_sandbag": sandbag,
    "afg_awn_red": lambda: awning((170, 50, 40), (220, 200, 160)),
    "afg_awn_blue": lambda: awning((40, 70, 150), (220, 210, 180)),
    "afg_awn_green": lambda: awning((50, 120, 60), (210, 200, 160)),
    "afg_tent_w": lambda: canvas((200, 190, 165)),
    "afg_tent_dark": lambda: canvas((70, 62, 54)),
    "afg_flag": flag,
}
TERRAIN = ["afg_sand", "afg_dirt", "afg_grass", "afg_field", "afg_rock", "afg_snow", "afg_road"]


def generate(names):
    return [(n, ALL[n](), False) for n in names]
