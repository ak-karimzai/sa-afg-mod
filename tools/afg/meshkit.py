"""Tiny mesh builder for procedural GTA models.

Coordinates are GTA's: X east, Y north, Z up. Triangles are counter-clockwise
seen from the front. UVs are world-scaled (1 unit = 1/`tex_scale` repeats) so
textures tile at a constant density regardless of object size.
"""
import math

import numpy as np


class Mesh:
    def __init__(self):
        self.pos, self.nrm, self.uv, self.col = [], [], [], []
        self.ncol = None
        self.tris = []
        self.materials = []  # dicts: {"tex": name, "surface": col surface id}
        self._mat_index = {}
        self.boxes = []  # collision boxes: (min xyz, max xyz, surface)

    # -- materials
    def mat(self, tex, surface=0):
        if tex not in self._mat_index:
            self._mat_index[tex] = len(self.materials)
            self.materials.append({"tex": tex, "surface": surface})
        return self._mat_index[tex]

    # -- primitives
    def quad(self, p0, p1, p2, p3, tex, uv_scale=4.0, shade=1.0, surface=0, uvs=None):
        """Quad p0..p3 counter-clockwise from the front."""
        m = self.mat(tex, surface)
        p = np.array([p0, p1, p2, p3], np.float64)
        n = np.cross(p[1] - p[0], p[3] - p[0])
        ln = np.linalg.norm(n)
        if ln < 1e-9:
            return
        n /= ln
        if uvs is None:
            # project onto the plane's two tangent axes
            u_axis = p[1] - p[0]
            u_axis /= np.linalg.norm(u_axis)
            v_axis = np.cross(n, u_axis)
            uvs = [((q - p[0]) @ u_axis / uv_scale, -((q - p[0]) @ v_axis) / uv_scale) for q in p]
        base = len(self.pos)
        c = int(np.clip(255 * shade, 0, 255))
        for q, t in zip(p, uvs):
            self.pos.append(q)
            self.nrm.append(n)
            self.uv.append(t)
            self.col.append((c, c, c, 255))
        self.tris.append((base, base + 1, base + 2, m))
        self.tris.append((base, base + 2, base + 3, m))

    def tri(self, p0, p1, p2, tex, uv_scale=4.0, shade=1.0, surface=0):
        m = self.mat(tex, surface)
        p = np.array([p0, p1, p2], np.float64)
        n = np.cross(p[1] - p[0], p[2] - p[0])
        n /= max(np.linalg.norm(n), 1e-9)
        u_axis = (p[1] - p[0]) / max(np.linalg.norm(p[1] - p[0]), 1e-9)
        v_axis = np.cross(n, u_axis)
        base = len(self.pos)
        c = int(np.clip(255 * shade, 0, 255))
        for q in p:
            self.pos.append(q)
            self.nrm.append(n)
            self.uv.append(((q - p[0]) @ u_axis / uv_scale, -((q - p[0]) @ v_axis) / uv_scale))
            self.col.append((c, c, c, 255))
        self.tris.append((base, base + 1, base + 2, m))

    def box(self, x0, y0, z0, x1, y1, z1, tex, top=None, uv_scale=4.0, collide=True, surface=0,
            bottom=False, shade=1.0):
        """Axis-aligned box. `top` overrides the roof texture."""
        top = top or tex
        s = shade
        # walls (counter-clockwise seen from outside)
        self.quad((x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1), tex, uv_scale, 0.80 * s, surface)  # south
        self.quad((x1, y1, z0), (x0, y1, z0), (x0, y1, z1), (x1, y1, z1), tex, uv_scale, 0.70 * s, surface)  # north
        self.quad((x0, y1, z0), (x0, y0, z0), (x0, y0, z1), (x0, y1, z1), tex, uv_scale, 0.75 * s, surface)  # west
        self.quad((x1, y0, z0), (x1, y1, z0), (x1, y1, z1), (x1, y0, z1), tex, uv_scale, 0.90 * s, surface)  # east
        self.quad((x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1), top, uv_scale, 1.0 * s, surface)  # top
        if bottom:
            self.quad((x0, y1, z0), (x1, y1, z0), (x1, y0, z0), (x0, y0, z0), tex, uv_scale, 0.5 * s, surface)
        if collide:
            self.boxes.append(((min(x0, x1), min(y0, y1), min(z0, z1)),
                               (max(x0, x1), max(y0, y1), max(z0, z1)), surface))

    def cylinder(self, cx, cy, z0, z1, r, tex, sides=10, top_tex=None, uv_scale=4.0, collide=True, surface=0,
                 r_top=None):
        r_top = r if r_top is None else r_top
        for i in range(sides):
            a0, a1 = 2 * math.pi * i / sides, 2 * math.pi * (i + 1) / sides
            p0 = (cx + r * math.cos(a0), cy + r * math.sin(a0), z0)
            p1 = (cx + r * math.cos(a1), cy + r * math.sin(a1), z0)
            p2 = (cx + r_top * math.cos(a1), cy + r_top * math.sin(a1), z1)
            p3 = (cx + r_top * math.cos(a0), cy + r_top * math.sin(a0), z1)
            shade = 0.75 + 0.25 * math.cos((a0 + a1) / 2 - math.radians(45))
            u0, u1 = i * 2 * math.pi * r / sides / uv_scale, (i + 1) * 2 * math.pi * r / sides / uv_scale
            self.quad(p0, p1, p2, p3, tex, shade=shade, surface=surface,
                      uvs=[(u0, 0), (u1, 0), (u1, -(z1 - z0) / uv_scale), (u0, -(z1 - z0) / uv_scale)])
            if r_top > 0:
                self.tri((cx, cy, z1), p3, p2, top_tex or tex, uv_scale, 1.0, surface)
        if collide:
            rr = max(r, r_top)
            self.boxes.append(((cx - rr * 0.8, cy - rr * 0.8, z0), (cx + rr * 0.8, cy + rr * 0.8, z1), surface))

    def dome(self, cx, cy, z0, r, tex, sides=12, rings=5, surface=0, uv_scale=4.0, height=None):
        height = r if height is None else height
        for j in range(rings):
            b0, b1 = math.pi / 2 * j / rings, math.pi / 2 * (j + 1) / rings
            for i in range(sides):
                a0, a1 = 2 * math.pi * i / sides, 2 * math.pi * (i + 1) / sides

                def pt(a, b):
                    return (cx + r * math.cos(b) * math.cos(a), cy + r * math.cos(b) * math.sin(a),
                            z0 + height * math.sin(b))
                shade = 0.8 + 0.2 * math.cos((a0 + a1) / 2 - math.radians(45))
                p0, p1, p2, p3 = pt(a0, b0), pt(a1, b0), pt(a1, b1), pt(a0, b1)
                if j == rings - 1:
                    self.tri(p0, p1, p2, tex, uv_scale, shade, surface)
                else:
                    self.quad(p0, p1, p2, p3, tex, uv_scale, shade, surface)

    def wall(self, x0, y0, x1, y1, z0, z1, thick, tex, top=None, uv_scale=4.0, surface=0):
        """Straight wall from (x0,y0) to (x1,y1), axis aligned only."""
        if abs(x1 - x0) >= abs(y1 - y0):
            self.box(min(x0, x1), y0 - thick / 2, z0, max(x0, x1), y0 + thick / 2, z1, tex, top, uv_scale,
                     surface=surface)
        else:
            self.box(x0 - thick / 2, min(y0, y1), z0, x0 + thick / 2, max(y0, y1), z1, tex, top, uv_scale,
                     surface=surface)

    # -- post processing
    def bake_lighting(self, sun=(-0.45, 0.55, 0.70), ambient=0.42, night=(0.30, 0.32, 0.45)):
        """Fake day prelight from normals + per-face shade already in col; night = tinted copy."""
        sun = np.asarray(sun) / np.linalg.norm(sun)
        nrm = np.asarray(self.nrm)
        lam = np.clip(nrm @ sun, 0, 1)
        base = np.asarray(self.col, np.float64)[:, :3] / 255
        day = np.clip(base * (ambient + (1 - ambient) * lam)[:, None], 0, 1)
        self.col = [tuple(int(v * 255) for v in c) + (255,) for c in day]
        nt = np.clip(day * np.asarray(night) * 1.6, 0, 1)
        self.ncol = [tuple(int(v * 255) for v in c) + (255,) for c in nt]

    def translate(self, dx, dy, dz):
        self.pos = [(p[0] + dx, p[1] + dy, p[2] + dz) for p in self.pos]
        self.boxes = [((a[0] + dx, a[1] + dy, a[2] + dz), (b[0] + dx, b[1] + dy, b[2] + dz), s)
                      for a, b, s in self.boxes]

    def extend(self, other):
        remap = [self.mat(m["tex"], m["surface"]) for m in other.materials]
        base = len(self.pos)
        self.pos += list(other.pos)
        self.nrm += list(other.nrm)
        self.uv += list(other.uv)
        self.col += list(other.col)
        self.tris += [(a + base, b + base, c + base, remap[m]) for a, b, c, m in other.tris]
        self.boxes += list(other.boxes)

    def bounds(self):
        p = np.asarray(self.pos)
        return p.min(0), p.max(0)
