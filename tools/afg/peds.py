"""Police uniform: repaint the game's two sheriff peds (CSHER 283 / DSHER 288) as Afghan police in Taliban-style
dress: kameez and shalwar instead of the shirt and trousers, a waistcoat, the hat and the hair repainted as a wrapped turban
(the hat patch also textures the top of the skull, so it cannot simply be made transparent), a beard. The textures are derived from the player's OWN gta3.img
at build time, so no Rockstar file is stored in the repository; the result goes into modloader/afghanistan/models/
as csher.txd / dsher.txd, which ModLoader serves instead of the originals.

The UV layout of both models is identical (same mesh, different texture). Region coordinates are in texture
pixels of the 128x256 atlas and were read off the model with tools/dev/pedview.py.
"""
import math
import os
import struct
import sys

import numpy as np

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "vendor", "DragonFF"))
from gtaLib import dff as ddff, txd as dtxd  # noqa: E402

from . import rw  # noqa: E402

# name in TXD, colours (kameez, shalwar, turban, waistcoat or None)
STYLES = {
    "dsher": dict(tex="DSHER", kameez=(228, 222, 206), shalwar=(214, 206, 188), turban=(32, 32, 36), vest=(36, 34, 40)),
    "csher": dict(tex="csher", kameez=(40, 38, 44), shalwar=(48, 46, 52), turban=(236, 234, 226), vest=None),
}
# texture regions (x0, x1, y0, y1)
R_SHIRT = (0, 72, 0, 142)
R_SLEEVES = (72, 128, 30, 82)      # upper arm, both halves
R_FOREARM = (92, 128, 82, 126)     # skin on the model -> becomes the long sleeve
R_PANTS = (0, 56, 144, 236)
R_HAT = (56, 84, 180, 194)
R_BADGE = (64, 86, 8, 38)
R_STAR = (24, 56, 232, 256)
R_FACE = (56, 128, 214, 256)
R_HAIR = (56, 128, 194, 214)


def img_read(img_path, filename):
    with open(img_path, "rb") as f:
        if f.read(4) != b"VER2":
            raise ValueError(f"{img_path} is not an IMG v2 archive")
        (count,) = struct.unpack("<I", f.read(4))
        for _ in range(count):
            e = f.read(32)
            off, sz1, sz2 = struct.unpack("<IHH", e[:8])
            name = e[8:].split(b"\0")[0].decode("ascii", "replace")
            if name.lower() == filename.lower():
                f.seek(off * 2048)
                return f.read((sz1 or sz2) * 2048)
    raise KeyError(filename)


def _tint(rgb, colour, lo=0.45, hi=0.80):
    """Recolour a region, keeping its folds: luminance -> colour * (lo + hi * L)."""
    L = (0.299 * rgb[..., 0] + 0.587 * rgb[..., 1] + 0.114 * rgb[..., 2]) / 255.0
    out = np.array(colour, np.float32)[None, None, :] * (lo + hi * L[..., None])
    return np.clip(out, 0, 255)


def _box(img, r):
    x0, x1, y0, y1 = r
    return img[y0:y1, x0:x1]


def _raster(tris_uv, w, h, attrs=None):
    """Rasterise UV triangles (pixel space) into a coverage mask and, optionally, interpolated attributes."""
    mask = np.zeros((h, w), bool)
    out = np.zeros((h, w, attrs.shape[2]), np.float32) if attrs is not None else None
    for ti, tri in enumerate(tris_uv):
        x0, x1 = int(max(tri[:, 0].min() - 1, 0)), int(min(tri[:, 0].max() + 2, w))
        y0, y1 = int(max(tri[:, 1].min() - 1, 0)), int(min(tri[:, 1].max() + 2, h))
        if x0 >= x1 or y0 >= y1:
            continue
        gx, gy = np.meshgrid(np.arange(x0, x1) + 0.5, np.arange(y0, y1) + 0.5)
        (ax, ay), (bx, by), (cx, cy) = tri
        area = (bx - ax) * (cy - ay) - (cx - ax) * (by - ay)
        if abs(area) < 1e-9:
            continue
        w0 = ((bx - gx) * (cy - gy) - (cx - gx) * (by - gy)) / area
        w1 = ((cx - gx) * (ay - gy) - (ax - gx) * (cy - gy)) / area
        w2 = 1 - w0 - w1
        inside = (w0 >= -0.02) & (w1 >= -0.02) & (w2 >= -0.02)
        mask[y0:y1, x0:x1] |= inside
        if attrs is not None:
            a = attrs[ti]
            val = w0[..., None] * a[0] + w1[..., None] * a[1] + w2[..., None] * a[2]
            out[y0:y1, x0:x1][inside] = val[inside]
    return mask, out


def _masks(dff_data, w, h):
    """Brim pixels (hat brim not shared with the crown) and beard pixels, from the model's geometry."""
    d = ddff.dff()
    d.load_memory(dff_data)
    g = d.clumps[0].geometry_list[0]
    P = np.array([(v.x, v.y, v.z) for v in g.vertices], np.float32)   # bind pose: +x up, +z forward
    U = np.array([(u.u * w, u.v * h) for u in g.uv_layers[0]], np.float32)
    T = np.array([(t.a, t.b, t.c) for t in g.triangles], np.int64)
    tu, tp = U[T], P[T]
    up = tp[:, :, 0].mean(1)
    r = np.hypot(tp[:, :, 1], tp[:, :, 2]).mean(1)
    in_hat = (tu[:, :, 0].min(1) >= R_HAT[0] - 1) & (tu[:, :, 0].max(1) <= R_HAT[1] + 1) & \
             (tu[:, :, 1].min(1) >= R_HAT[2] - 1) & (tu[:, :, 1].max(1) <= R_HAT[3] + 1)
    brim = in_hat & (up > 0.74) & (r > 0.105)
    crown = in_hat & (up > 0.74) & (r <= 0.105)
    brim_mask, _ = _raster(tu[brim], w, h)
    crown_mask, _ = _raster(tu[crown], w, h)
    brim_only = brim_mask & ~crown_mask
    # head: every triangle textured from the face / hair patches (below the hands patch)
    in_head = (tu[:, :, 0].min(1) >= R_FACE[0] - 1) & (tu[:, :, 1].min(1) >= R_HAIR[2] - 1)
    head_mask, pos = _raster(tu[in_head], w, h, tp[in_head])
    x, y, z = pos[..., 0], pos[..., 1], pos[..., 2]
    beard = head_mask & (x > 0.55) & (x < 0.705) & (z > -0.03)
    beard &= ~((np.abs(y) < 0.02) & (x > 0.655))           # leave the mouth
    # soften the upper edge: weight 1 at the jaw, fading out below the cheekbones
    weight = np.clip((0.705 - x) / 0.035, 0, 1) * beard
    # scalp: above the brow line (eyes are at x ~ 0.76), plus the back of the head down to the nape
    scalp = head_mask & ((x > 0.785) | ((x > 0.70) & (z < -0.045)))
    return brim_only, weight, scalp


def repaint(rgba, dff_data, style, seed=7):
    rng = np.random.default_rng(seed)
    h, w = rgba.shape[:2]
    img = rgba.astype(np.float32).copy()
    img[..., 3] = 255
    # --- cloth: kameez (shirt, upper and lower sleeves), shalwar (trousers)
    for r in (R_SHIRT, R_SLEEVES):
        _box(img, r)[..., :3] = _tint(_box(rgba, r)[..., :3], style["kameez"])
    fa = _box(img, R_FOREARM)
    noise = rng.normal(0, 6, fa.shape[:2])[..., None]
    fa[..., :3] = np.clip(np.array(style["kameez"], np.float32) * 0.92 + noise, 0, 255)
    _box(img, R_PANTS)[..., :3] = _tint(_box(rgba, R_PANTS)[..., :3], style["shalwar"])
    # --- waistcoat over the torso (front and back), open in a V at the collar
    if style["vest"]:
        x0, x1, y0, y1 = R_SHIRT
        sub = img[y0:y1, x0:x1]
        yy, xx = np.mgrid[0:y1 - y0, 0:x1 - x0]
        vest = (xx >= 13) & (xx <= 59)
        v_open = (yy >= 52) & (yy <= 100) & (np.abs(xx - 36) < (yy - 52) * 0.42 + 2)
        hem = yy >= 136
        m = vest & ~v_open & ~hem
        base = _tint(_box(rgba, R_SHIRT)[..., :3], style["vest"], 0.6, 0.6)
        sub[..., :3][m] = base[m]
        # thin lighter edge along the opening
        edge = vest & ~m & (yy >= 50) & (yy <= 102) & (np.abs(np.abs(xx - 36) - ((yy - 52) * 0.42 + 2)) < 1.5)
        sub[..., :3][edge] = np.array(style["vest"], np.float32) * 1.8
    # --- no sheriff star, no shoulder badge
    fill = style["vest"] if style["vest"] else style["kameez"]
    _box(img, R_STAR)[..., :3] = np.array(fill, np.float32) * 0.75
    _box(img, R_BADGE)[..., :3] = np.array(style["kameez"], np.float32) * 0.95
    # --- turban: the hat patch also textures the top of the skull, so the hat stays and becomes wrapped cloth,
    # together with the hair at the sides and back of the head and the scalp above the brow line
    _, beard_w, scalp = _masks(dff_data, w, h)
    yy, xx = np.mgrid[0:h, 0:w]
    bands = 0.78 + 0.22 * np.sin((xx * 0.55 + yy * 1.6) * 0.9) ** 2
    turb = np.clip(np.array(style["turban"], np.float32)[None, None, :] * bands[..., None] + rng.normal(0, 5, (h, w, 1)), 0, 255)
    cloth = scalp.copy()
    x0, x1, y0, y1 = R_HAT
    cloth[y0:y1, x0:x1] = True
    x0, x1, y0, y1 = R_HAIR
    cloth[y0:y1, x0:x1] = True
    img[..., :3][cloth] = turb[cloth]
    # --- beard
    beard_col = np.array((24, 17, 12), np.float32)
    grain = rng.normal(0, 9, (h, w, 1))
    bw = (beard_w * 0.95)[..., None]
    img[..., :3] = img[..., :3] * (1 - bw) + (beard_col + grain) * bw
    return np.clip(img, 0, 255).astype(np.uint8)


def build(game_dir, out_dir, log=print):
    """Write csher.txd / dsher.txd into out_dir from the game's gta3.img. Returns the files written."""
    img = os.path.join(game_dir, "models", "gta3.img") if game_dir else None
    if not img or not os.path.exists(img):
        log("  police uniform: skipped (no game folder with models/gta3.img; pass --game or set GTA_SA_DIR)")
        return []
    written = []
    for base, style in STYLES.items():
        t = dtxd.txd()
        t.load_memory(img_read(img, base + ".txd"))
        n = t.native_textures[0]
        rgba = np.frombuffer(n.to_rgba(0), np.uint8).reshape(n.get_height(), n.get_width(), 4)
        out = repaint(rgba, img_read(img, base + ".dff"), style)
        path = os.path.join(out_dir, base + ".txd")
        rw.write_txd(path, [(n.name, np.ascontiguousarray(out), False)])
        written.append(path)
    log(f"  police uniform: {', '.join(os.path.basename(p) for p in written)} (from the game's own textures)")
    return written
