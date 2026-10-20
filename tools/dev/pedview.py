#!/usr/bin/env python3
"""Preview a ped model (bind pose) with a texture, without the game.

    python3 tools/dev/pedview.py dsher out.png                      # model + texture from the game's gta3.img
    python3 tools/dev/pedview.py dsher out.png --txd path/to/dsher.txd   # model from gta3.img, texture from a file

Needs GTA_SA_DIR (or --game) pointing at a game folder with models/gta3.img. Renders front, side and back views.
"""
import math
import os
import struct
import sys

import numpy as np
from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
sys.path.insert(0, os.path.join(ROOT, "tools", "vendor", "DragonFF"))
from gtaLib import dff as ddff, txd as dtxd  # noqa: E402


def img_read(img_path, filename):
    with open(img_path, "rb") as f:
        if f.read(4) != b"VER2":
            raise SystemExit("not an IMG v2 archive")
        (count,) = struct.unpack("<I", f.read(4))
        for _ in range(count):
            e = f.read(32)
            off, sz1, sz2 = struct.unpack("<IHH", e[:8])
            name = e[8:].split(b"\0")[0].decode("ascii", "replace")
            if name.lower() == filename.lower():
                f.seek(off * 2048)
                return f.read((sz1 or sz2) * 2048)
    raise SystemExit(f"{filename} not in {img_path}")


def load_textures(data):
    t = dtxd.txd()
    t.load_memory(data)
    out = {}
    for n in t.native_textures:
        rgba = np.frombuffer(n.to_rgba(0), np.uint8).reshape(n.get_height(), n.get_width(), 4)
        out[n.name.lower()] = rgba.astype(np.float32) / 255
    return out


def render_ped(dff_data, tex, w=360, h=720, views=("front", "side", "back")):
    d = ddff.dff()
    d.load_memory(dff_data)
    g = d.clumps[0].geometry_list[0]
    P = np.array([(v.x, v.y, v.z) for v in g.vertices], np.float64)
    UV = np.array([(u.u, u.v) for u in g.uv_layers[0]], np.float64)
    T = np.array([(t.a, t.b, t.c, t.material) for t in g.triangles], np.int64)
    mats = [m.textures[0].name.lower() if m.textures else "" for m in g.materials]
    # SA ped bind pose: X is up (feet at -x), Z is forward. Make a (right, up, depth) frame per view.
    up = P[:, 0]
    sheet = Image.new("RGB", (w * len(views), h), (70, 80, 95))
    for vi, view in enumerate(views):
        if view == "front":
            right, depth = P[:, 1], P[:, 2]
        elif view == "back":
            right, depth = -P[:, 1], -P[:, 2]
        else:
            right, depth = -P[:, 2], P[:, 1]
        scale = h * 0.85 / (up.max() - up.min())
        sx = w / 2 + right * scale
        sy = h / 2 - (up - (up.max() + up.min()) / 2) * scale
        img = np.zeros((h, w, 3), np.float32)
        img[:] = (0.27, 0.31, 0.37)
        zbuf = np.full((h, w), -np.inf)
        # face normals for a little shading
        for a, b, c, m in T:
            p = np.array([(sx[i], sy[i], depth[i]) for i in (a, b, c)])
            area = (p[1, 0] - p[0, 0]) * (p[2, 1] - p[0, 1]) - (p[2, 0] - p[0, 0]) * (p[1, 1] - p[0, 1])
            if abs(area) < 1e-6:
                continue
            x0, x1 = int(max(p[:, 0].min(), 0)), int(min(p[:, 0].max() + 1, w))
            y0, y1 = int(max(p[:, 1].min(), 0)), int(min(p[:, 1].max() + 1, h))
            if x0 >= x1 or y0 >= y1:
                continue
            gx, gy = np.meshgrid(np.arange(x0, x1) + 0.5, np.arange(y0, y1) + 0.5)
            w0 = ((p[1, 0] - gx) * (p[2, 1] - gy) - (p[2, 0] - gx) * (p[1, 1] - gy)) / area
            w1 = ((p[2, 0] - gx) * (p[0, 1] - gy) - (p[0, 0] - gx) * (p[2, 1] - gy)) / area
            w2 = 1 - w0 - w1
            inside = (w0 >= 0) & (w1 >= 0) & (w2 >= 0)
            z = w0 * p[0, 2] + w1 * p[1, 2] + w2 * p[2, 2]
            zb = zbuf[y0:y1, x0:x1]
            vis = inside & (z > zb)
            if not vis.any():
                continue
            uvp = w0[..., None] * UV[a] + w1[..., None] * UV[b] + w2[..., None] * UV[c]
            tx = tex.get(mats[m])
            if tx is None:
                rgb = np.full(uvp.shape[:2] + (3,), 0.8, np.float32)
            else:
                th, tw = tx.shape[:2]
                u = (np.mod(uvp[..., 0], 1) * tw).astype(int) % tw
                v = (np.mod(uvp[..., 1], 1) * th).astype(int) % th
                texel = tx[v, u]
                alpha = texel[..., 3]
                vis = vis & (alpha > 0.5)   # alpha-tested, like the game's ped rendering
                rgb = texel[..., :3]
            # lambert from the face normal (light from the viewer, slightly above)
            n = np.cross(P[b] - P[a], P[c] - P[a])
            n /= (np.linalg.norm(n) + 1e-9)
            if view == "front":
                nd = np.array([n[0], n[1], n[2]]) @ np.array([0.3, 0.0, 1.0])
            elif view == "back":
                nd = np.array([n[0], n[1], n[2]]) @ np.array([0.3, 0.0, -1.0])
            else:
                nd = np.array([n[0], n[1], n[2]]) @ np.array([0.3, 1.0, 0.0])
            shade = 0.55 + 0.45 * max(0.0, nd)
            region = img[y0:y1, x0:x1]
            region[vis] = (rgb * shade)[vis] if rgb.ndim == 3 else rgb
            zb[vis] = z[vis]
        sheet.paste(Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8)), (vi * w, 0))
    return sheet


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    opts = {sys.argv[i]: sys.argv[i + 1] for i, a in enumerate(sys.argv) if a.startswith("--") and i + 1 < len(sys.argv)}
    if len(args) < 2:
        raise SystemExit(__doc__)
    model, out = args[0], args[1]
    game = opts.get("--game") or os.environ.get("GTA_SA_DIR")
    if not game:
        raise SystemExit("set GTA_SA_DIR or pass --game")
    img = os.path.join(game, "models", "gta3.img")
    dff_data = img_read(img, model + ".dff")
    if "--txd" in opts:
        tex = load_textures(open(opts["--txd"], "rb").read())
    else:
        tex = load_textures(img_read(img, model + ".txd"))
    render_ped(dff_data, tex).save(out)
    print("wrote", out)


if __name__ == "__main__":
    main()
