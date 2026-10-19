#!/usr/bin/env python3
"""Software preview renderer for the generated game files.

Reads DFF/TXD back with DragonFF's parser (independent of our writer), places
instances exactly like the game does from the IPL (conjugated quaternions),
backface-culls with RenderWare's winding and rasterises with a z-buffer.

    python3 tools/dev/render.py town   out.png
    python3 tools/dev/render.py models out.png
"""
import math
import os
import sys

import numpy as np
from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
sys.path.insert(0, os.path.join(ROOT, "tools", "vendor", "DragonFF"))
from gtaLib import dff as ddff, txd as dtxd  # noqa: E402

MODELS = os.path.join(ROOT, "dist", "game", "modloader", "afghanistan", "models")
DATA = os.path.join(ROOT, "dist", "game", "data", "maps", "afghan")

_tex_cache, _mesh_cache = {}, {}


def textures(txd_name):
    if txd_name not in _tex_cache:
        t = dtxd.txd()
        t.load_file(os.path.join(MODELS, txd_name + ".txd"))
        out = {}
        for n in t.native_textures:
            rgba = np.frombuffer(n.to_rgba(0), np.uint8).reshape(n.height, n.width, 4)
            out[n.name] = rgba[..., :3].astype(np.float32) / 255
        _tex_cache[txd_name] = out
    return _tex_cache[txd_name]


def mesh(name):
    if name not in _mesh_cache:
        d = ddff.dff()
        d.load_file(os.path.join(MODELS, name + ".dff"))
        g = d.clumps[0].geometry_list[0]
        pos = np.array([(v.x, v.y, v.z) for v in g.vertices], np.float64)
        uv = np.array([(u.u, u.v) for u in g.uv_layers[0]], np.float64)
        col = np.array([(c.r, c.g, c.b) for c in g.prelit_colors], np.float64) / 255
        tris = np.array([(t.a, t.b, t.c, t.material) for t in g.triangles], np.int64)
        mats = [m.textures[0].name for m in g.materials]
        _mesh_cache[name] = (pos, uv, col, tris, mats)
    return _mesh_cache[name]


def ipl_matrix(qx, qy, qz, qw):
    """What CFileLoader::LoadObjectInstance does: conjugate, then quaternion -> matrix."""
    x, y, z, w = -qx, -qy, -qz, qw
    return np.array([[1 - 2 * (y * y + z * z), 2 * (x * y - z * w), 2 * (x * z + y * w)],
                     [2 * (x * y + z * w), 1 - 2 * (x * x + z * z), 2 * (y * z - x * w)],
                     [2 * (x * z - y * w), 2 * (y * z + x * w), 1 - 2 * (x * x + y * y)]])


class Camera:
    def __init__(self, eye, target, w=960, h=540, fov=60):
        self.eye = np.asarray(eye, float)
        f = np.asarray(target, float) - self.eye
        f /= np.linalg.norm(f)
        r = np.cross(f, [0, 0, 1]); r /= np.linalg.norm(r)
        u = np.cross(r, f)
        self.R = np.stack([r, u, f])
        self.w, self.h = w, h
        self.fl = (w / 2) / math.tan(math.radians(fov) / 2)

    def project(self, p):
        c = (p - self.eye) @ self.R.T
        z = c[:, 2]
        sx = self.w / 2 + self.fl * c[:, 0] / np.maximum(z, 1e-3)
        sy = self.h / 2 - self.fl * c[:, 1] / np.maximum(z, 1e-3)
        return np.stack([sx, sy, z], 1)


def render(instances, cam, sky=(0.62, 0.75, 0.92), fog=(0.85, 0.80, 0.72), fog_end=2500.0):
    W, H = cam.w, cam.h
    img = np.zeros((H, W, 3), np.float32)
    yy = np.linspace(0, 1, H)[:, None, None]
    img[:] = np.array(sky) * (1 - yy) + np.array(fog) * yy
    zbuf = np.full((H, W), np.inf)
    for name, txd, M, t in instances:
        pos, uv, col, tris, mats = mesh(name)
        tex = textures(txd)
        world = pos @ M.T + t
        scr = cam.project(world)
        for a, b, c, m in tris:
            p = scr[[a, b, c]]
            if (p[:, 2] < 1).any():
                continue
            # RenderWare front faces are counter-clockwise in world space; in screen space (y down) that
            # appears clockwise, so cull triangles whose screen-space signed area is positive.
            area = (p[1, 0] - p[0, 0]) * (p[2, 1] - p[0, 1]) - (p[2, 0] - p[0, 0]) * (p[1, 1] - p[0, 1])
            if area >= 0:
                continue
            x0, x1 = int(max(p[:, 0].min(), 0)), int(min(p[:, 0].max() + 1, W))
            y0, y1 = int(max(p[:, 1].min(), 0)), int(min(p[:, 1].max() + 1, H))
            if x0 >= x1 or y0 >= y1:
                continue
            gx, gy = np.meshgrid(np.arange(x0, x1) + 0.5, np.arange(y0, y1) + 0.5)
            w0 = ((p[1, 0] - gx) * (p[2, 1] - gy) - (p[2, 0] - gx) * (p[1, 1] - gy)) / area
            w1 = ((p[2, 0] - gx) * (p[0, 1] - gy) - (p[0, 0] - gx) * (p[2, 1] - gy)) / area
            w2 = 1 - w0 - w1
            inside = (w0 >= 0) & (w1 >= 0) & (w2 >= 0)
            if not inside.any():
                continue
            iz = w0 / p[0, 2] + w1 / p[1, 2] + w2 / p[2, 2]
            z = 1 / iz
            zb = zbuf[y0:y1, x0:x1]
            vis = inside & (z < zb)
            if not vis.any():
                continue
            # perspective-correct UV and colour
            uvp = (w0[..., None] * uv[a] / p[0, 2] + w1[..., None] * uv[b] / p[1, 2] + w2[..., None] * uv[c] / p[2, 2]) * z[..., None]
            cp = (w0[..., None] * col[a] / p[0, 2] + w1[..., None] * col[b] / p[1, 2] + w2[..., None] * col[c] / p[2, 2]) * z[..., None]
            tx = tex[mats[m]]
            th, tw = tx.shape[:2]
            u = (np.mod(uvp[..., 0], 1) * tw).astype(int) % tw
            v = (np.mod(uvp[..., 1], 1) * th).astype(int) % th
            rgb = tx[v, u] * np.clip(cp * 1.6, 0, 1.4)
            f = np.clip(z / fog_end, 0, 1)[..., None] ** 1.5
            rgb = rgb * (1 - f) + np.array(fog) * f
            region = img[y0:y1, x0:x1]
            region[vis] = rgb[vis]
            zb[vis] = z[vis]
    return Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8))


def load_ipl():
    ide = {}
    for ln in open(os.path.join(DATA, "afghan.ide")):
        parts = [s.strip() for s in ln.split(",")]
        if len(parts) == 5:
            ide[int(parts[0])] = (parts[1], parts[2], float(parts[3]))
    inst = []
    for ln in open(os.path.join(DATA, "afghan.ipl")):
        parts = [s.strip() for s in ln.split(",")]
        if len(parts) == 11:
            mid = int(parts[0])
            x, y, z, qx, qy, qz, qw = map(float, parts[3:10])
            inst.append((ide[mid][0], ide[mid][1], ide[mid][2], np.array([x, y, z]), (qx, qy, qz, qw)))
    return inst


def scene(center, radius, cam, lod=False):
    out = []
    for name, txd, dist, t, q in load_ipl():
        d = np.linalg.norm(t[:2] - np.asarray(center[:2]))
        is_lod = name.startswith("lod")
        if is_lod != lod:
            continue
        reach = radius + (180 if name.startswith("afg_t") or is_lod else 0)
        if d > reach:
            continue
        out.append((name, txd, ipl_matrix(*q), t))
    return out


if __name__ == "__main__":
    what, path = sys.argv[1], sys.argv[2]
    if what == "models":
        names = ["afg_compound", "afg_mosque", "afg_shops", "afg_house_l", "afg_wtower", "afg_poplar",
                 "afg_mulberry", "afg_gtower", "afg_fortress"]
        inst = []
        for i, n in enumerate(names[:-1]):
            inst.append((n, "afg_build", np.eye(3), np.array([(i % 4) * 34.0, (i // 4) * 36.0, 0])))
        inst.append(("afg_fortress", "afg_build", np.eye(3), np.array([150.0, 30.0, 0])))
        cam = Camera((50, -75, 45), (70, 30, 0), 1280, 720, 60)
        render(inst, cam, fog_end=1e9).save(path)
    else:
        sys.path.insert(0, os.path.join(ROOT, "tools"))
        import json
        pts = json.load(open(os.path.join(ROOT, "build", "points.json")))["points"]
        key = {"town": "safehouse", "base": "base_hq", "fort": "fortress_gate", "valley": "dara_e_sabz",
               "camp": "bandit_camp", "checkpoint": "checkpoint"}[what]
        x, y, z, _ = pts[key]
        offs = {"town": (-60, -140, 60), "base": (-150, -200, 90), "fort": (40, -140, 70),
                "valley": (120, -160, 80), "camp": (60, -120, 60), "checkpoint": (-60, -80, 40)}[what]
        eye = (x + offs[0], y + offs[1], z + offs[2])
        cam = Camera(eye, (x + 40 if what == "town" else x, y, z), 1280, 720, 65)
        inst = scene((x, y), 350, cam) + scene((x, y), 3000, cam, lod=True)
        render(inst, cam).save(path)
