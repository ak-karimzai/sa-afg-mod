"""Heightmap, sites, roads and terrain materials for the whole map."""
import heapq
import math

import numpy as np

from . import dem
from .textures import fbm

HALF = 3000.0                # map spans -3000..3000 like vanilla SA
CELLS = 768                  # grid cells per side
CELL = 2 * HALF / CELLS      # 7.8125 units
N = CELLS + 1                # vertices per side
SCALE = 6.0                  # real metres per game unit (horizontal and vertical)
CENTER = (35.00, 69.10)      # Shomali plain / Ghorband valley mouth, north of Kabul

# terrain material ids (index into TERRAIN_TEX); surface = SA collision surface id
MAT_SAND, MAT_DIRT, MAT_GRASS, MAT_FIELD, MAT_ROCK, MAT_SNOW, MAT_ROAD = range(7)
TERRAIN_TEX = ["afg_sand", "afg_dirt", "afg_grass", "afg_field", "afg_rock", "afg_snow", "afg_road"]
SURFACE = {MAT_SAND: 31, MAT_DIRT: 26, MAT_GRASS: 13, MAT_FIELD: 20, MAT_ROCK: 35, MAT_SNOW: 30,
           MAT_ROAD: 27}
STEEP_SURFACE = 18  # STEEP_CLIFF: player slides, stops people climbing cliffs

# (kind, display name, gxt key, x, y, radius)
SITES = [
    ("town", "Qala-e-Naw", "AFZ_QN", 1500, 500, 170),
    ("base", "Camp Sangar", "AFZ_CS", 1900, -1750, 230),
    ("village", "Deh-e-Bagh", "AFZ_DB", 900, -500, 100),
    ("village", "Dara-e-Sabz", "AFZ_DS", -1300, 650, 90),
    ("village", "Sar-e-Kotal", "AFZ_SK", -300, 1350, 80),
    ("village", "Shahrak", "AFZ_SH", 2200, 2100, 110),
    ("camp", "Sang-e-Siah", "AFZ_SS", -2500, 550, 70),
    ("fortress", "Qala-e-Kohna", "AFZ_QK", -1500, -1300, 75),
    # placed automatically where the town -> valley road enters the mountains
    ("checkpoint", "Darwaza Checkpoint", "AFZ_DC", None, None, 45),
    ("ruins", "Burj-e-Khak", "AFZ_BK", 2550, -2550, 60),
]

ROADS = [
    ("Qala-e-Naw", "Darwaza Checkpoint"), ("Darwaza Checkpoint", "Dara-e-Sabz"),
    ("Qala-e-Naw", "Camp Sangar"), ("Qala-e-Naw", "Deh-e-Bagh"), ("Dara-e-Sabz", "Sang-e-Siah"),
    ("Dara-e-Sabz", "Sar-e-Kotal"), ("Qala-e-Naw", "Shahrak"), ("Camp Sangar", "Burj-e-Khak"),
    ("Deh-e-Bagh", "Qala-e-Kohna"), ("Deh-e-Bagh", "Camp Sangar"),
]


def to_grid(x, y):
    """game coords -> (row, col) floats. Row 0 = north edge (y=+HALF)."""
    return (HALF - y) / CELL, (x + HALF) / CELL


def to_game(r, c):
    return -HALF + c * CELL, HALF - r * CELL


def _blur(a, sigma):
    rad = int(3 * sigma) + 1
    k = np.exp(-0.5 * (np.arange(-rad, rad + 1) / sigma) ** 2)
    k /= k.sum()
    p = np.pad(a, rad, mode="edge")
    p = np.apply_along_axis(lambda v: np.convolve(v, k, "valid"), 0, p)
    return np.apply_along_axis(lambda v: np.convolve(v, k, "valid"), 1, p)


def smoothstep(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


class World:
    def __init__(self, log=print):
        self.log = log
        h = dem.sample(CENTER[0], CENTER[1], 2 * HALF * SCALE, N)
        h = _blur(h, 0.8)
        z = (h - np.percentile(h, 0.5)) / SCALE + 6.0
        z = np.maximum(z, 4.0)
        # raise a hill ring along flat map edges so nobody drives off the world
        r, c = np.mgrid[0:N, 0:N]
        edge = np.minimum.reduce([r, c, N - 1 - r, N - 1 - c]) * CELL
        ring = 1 - smoothstep(0, 220, edge)
        hills = 40 + 90 * _grid_noise(77)
        z = np.maximum(z, z + ring * hills * (z < 150))
        self.z = z
        self.road_mask = np.full((N, N), np.inf)   # distance to nearest road centre (units)
        self.site_mask = np.zeros((N, N), bool)
        self.sites = {}
        self.road_paths = []
        self._place_sites()
        self._build_roads()
        self._flatten_sites()  # again: roads must not cut through compounds
        self._materials()

    # ------------------------------------------------------------------ helpers
    def height(self, x, y):
        r, c = to_grid(x, y)
        r = np.clip(r, 0, N - 1.001); c = np.clip(c, 0, N - 1.001)
        r0, c0 = int(r), int(c)
        fr, fc = r - r0, c - c0
        z = self.z
        return float((z[r0, c0] * (1 - fc) + z[r0, c0 + 1] * fc) * (1 - fr) +
                     (z[r0 + 1, c0] * (1 - fc) + z[r0 + 1, c0 + 1] * fc) * fr)

    def slope_deg(self):
        gy, gx = np.gradient(self.z, CELL)
        return np.degrees(np.arctan(np.hypot(gx, gy)))

    def _disk(self, x, y, radius):
        r, c = to_grid(x, y)
        rr = int(radius / CELL) + 2
        r0, r1 = max(int(r) - rr, 0), min(int(r) + rr + 1, N)
        c0, c1 = max(int(c) - rr, 0), min(int(c) + rr + 1, N)
        gr, gc = np.mgrid[r0:r1, c0:c1]
        gx, gy = to_game(gr, gc)
        d = np.hypot(gx - x, gy - y)
        return (slice(r0, r1), slice(c0, c1)), d

    # ------------------------------------------------------------------ sites
    def _place_sites(self):
        slope = self.slope_deg()
        rough = _blur(slope, 4)
        for kind, name, key, x, y, rad in SITES:
            if x is None:
                continue
            best = None
            for dx in range(-160, 161, 16):
                for dy in range(-160, 161, 16):
                    sl, d = self._disk(x + dx, y + dy, rad)
                    score = rough[sl][d < rad].mean() + math.hypot(dx, dy) * 0.02
                    if best is None or score < best[0]:
                        best = (score, x + dx, y + dy)
            _, sx, sy = best
            sl, d = self._disk(sx, sy, rad)
            sz = float(np.median(self.z[sl][d < rad]))
            self.sites[name] = dict(kind=kind, name=name, key=key, x=sx, y=sy, z=sz, r=rad)
            self.log(f"  site {name:20s} {kind:10s} ({sx:7.1f},{sy:7.1f},{sz:6.1f})"
                     f" mean slope {best[0]:.1f}")
        self._place_checkpoint()
        self._flatten_sites()

    def _place_checkpoint(self):
        """Put the checkpoint where the town -> Dara-e-Sabz track leaves the plain."""
        town, dest = self.sites["Qala-e-Naw"], self.sites["Dara-e-Sabz"]
        cells = self._astar(town, dest)
        pts = [to_game(r, c) for r, c in cells]
        for x, y in pts:
            if self.height(x, y) > town["z"] + 10 and math.hypot(x - town["x"], y - town["y"]) > 900:
                break
        kind, name, key, _, _, rad = next(s for s in SITES if s[3] is None)
        z = self.height(x, y)
        self.sites[name] = dict(kind=kind, name=name, key=key, x=x, y=y, z=z, r=rad)
        self.log(f"  site {name:20s} {kind:10s} ({x:7.1f},{y:7.1f},{z:6.1f}) on valley road")

    def _flatten_sites(self):
        for s in self.sites.values():
            sl, d = self._disk(s["x"], s["y"], s["r"] + 70)
            w = 1 - smoothstep(s["r"], s["r"] + 60, d)
            self.z[sl] = self.z[sl] * (1 - w) + s["z"] * w
            self.site_mask[sl] |= d < s["r"]

    # ------------------------------------------------------------------ roads
    def _astar(self, a, b):
        z = self.z
        start = tuple(int(round(v)) for v in to_grid(a["x"], a["y"]))
        goal = tuple(int(round(v)) for v in to_grid(b["x"], b["y"]))
        # coarse search on a 2-cell lattice for speed
        step = 2
        start = (start[0] // step * step, start[1] // step * step)
        goal = (goal[0] // step * step, goal[1] // step * step)
        nbrs = [(dr * step, dc * step) for dr in (-1, 0, 1) for dc in (-1, 0, 1) if dr or dc]
        nbrs += [(2 * step, step), (step, 2 * step), (-2 * step, step), (-step, 2 * step),
                 (2 * step, -step), (step, -2 * step), (-2 * step, -step), (-step, -2 * step)]
        g = {start: 0.0}
        prev = {}
        heap = [(0.0, start)]
        while heap:
            _, cur = heapq.heappop(heap)
            if cur == goal:
                break
            cg = g[cur]
            for dr, dc in nbrs:
                nr, nc = cur[0] + dr, cur[1] + dc
                if not (0 <= nr < N and 0 <= nc < N):
                    continue
                dist = math.hypot(dr, dc) * CELL
                grade = abs(z[nr, nc] - z[cur]) / dist
                if grade > 0.30:
                    continue
                cost = dist * (1 + (grade / 0.07) ** 2)
                if self.road_mask[nr, nc] < 4:
                    cost *= 0.35  # reuse existing roads
                ng = cg + cost
                if ng < g.get((nr, nc), 1e18):
                    g[(nr, nc)] = ng
                    prev[(nr, nc)] = cur
                    hh = math.hypot(goal[0] - nr, goal[1] - nc) * CELL
                    heapq.heappush(heap, (ng + hh, (nr, nc)))
        if goal not in prev:
            raise RuntimeError(f"no road from {a['name']} to {b['name']}")
        path = [goal]
        while path[-1] != start:
            path.append(prev[path[-1]])
        return path[::-1]

    def _build_roads(self):
        for an, bn in ROADS:
            a, b = self.sites[an], self.sites[bn]
            cells = self._astar(a, b)
            pts = np.array([to_game(r, c) for r, c in cells], np.float64)
            # densify + smooth (Chaikin x3)
            for _ in range(3):
                q = 0.75 * pts[:-1] + 0.25 * pts[1:]
                rr = 0.25 * pts[:-1] + 0.75 * pts[1:]
                pts = np.vstack([pts[:1], np.column_stack([q, rr]).reshape(-1, 2), pts[-1:]])
            zs = np.array([self.height(x, y) for x, y in pts])
            k = 9
            zs = np.convolve(np.pad(zs, k, mode="edge"), np.ones(2 * k + 1) / (2 * k + 1), "valid")
            self._carve(pts, zs)
            self.road_paths.append(dict(a=an, b=bn, pts=pts, z=zs))
            length = np.hypot(*np.diff(pts, axis=0).T).sum()
            self.log(f"  road {an} -> {bn}: {length:.0f} units")

    def _carve(self, pts, zs, half_w=6.0, fall=14.0):
        target = np.full((N, N), np.nan)
        best = np.full((N, N), np.inf)
        for (x, y), zz in zip(pts, zs):
            sl, d = self._disk(x, y, half_w + fall)
            upd = d < best[sl]
            best[sl][upd] = d[upd]
            target[sl][upd] = zz
        w = 1 - smoothstep(half_w, half_w + fall, best)
        m = ~np.isnan(target)
        self.z[m] = self.z[m] * (1 - w[m]) + target[m] * w[m]
        self.road_mask = np.minimum(self.road_mask, best)

    # ------------------------------------------------------------------ materials
    def _materials(self):
        """Material per cell (CELLS x CELLS), evaluated at cell centres."""
        zc = (self.z[:-1, :-1] + self.z[1:, :-1] + self.z[:-1, 1:] + self.z[1:, 1:]) / 4
        gy, gx = np.gradient(self.z, CELL)
        sl = np.degrees(np.arctan(np.hypot(gx, gy)))
        slc = (sl[:-1, :-1] + sl[1:, :-1] + sl[:-1, 1:] + sl[1:, 1:]) / 4
        roadc = np.minimum.reduce([self.road_mask[:-1, :-1], self.road_mask[1:, :-1],
                                   self.road_mask[:-1, 1:], self.road_mask[1:, 1:]])
        n1 = _grid_noise(91)[:CELLS, :CELLS]
        n2 = _grid_noise(92, 24)[:CELLS, :CELLS]
        mat = np.full((CELLS, CELLS), MAT_SAND, np.uint8)
        mat[n1 > 0.55] = MAT_DIRT
        mat[(n2 > 0.5) & (slc < 14)] = MAT_GRASS
        mat[(slc > 18) & (n1 > 0.4)] = MAT_DIRT
        mat[slc > 26] = MAT_ROCK
        mat[(zc > 345 + 40 * n2) & (slc < 40)] = MAT_SNOW
        # irrigated fields: rectangular plots around villages and the town
        r, c = np.mgrid[0:CELLS, 0:CELLS]
        gx_, gy_ = to_game(r + 0.5, c + 0.5)
        rng = np.random.default_rng(5)
        pr = rng.random((200, 200))
        # plots ~45 x 30 units, slightly skewed so they don't look like a chessboard
        px = np.floor((gx_ + 0.3 * gy_) / 45).astype(int) % 200
        py = np.floor((gy_ - 0.2 * gx_) / 30).astype(int) % 200
        plot = pr[py, px]
        self.field_mask = np.zeros((CELLS, CELLS), bool)
        for s in self.sites.values():
            if s["kind"] not in ("village", "town"):
                continue
            d = np.hypot(gx_ - s["x"], gy_ - s["y"])
            reach = s["r"] * (1.8 + 1.4 * n2)
            fm = (d > s["r"] * 0.9) & (d < reach) & (slc < 6) & (plot < 0.55)
            self.field_mask |= fm
        mat[self.field_mask] = MAT_FIELD
        site_c = self.site_mask[:-1, :-1]
        mat[site_c] = MAT_DIRT
        mat[roadc < 6.5] = MAT_ROAD
        self.mat = mat
        self.cell_slope = slc


def _grid_noise(seed, base=6):
    """Large-scale noise on the vertex grid (N x N)."""
    n = fbm(seed, base, 5, size=1024)
    return n[:N, :N]
