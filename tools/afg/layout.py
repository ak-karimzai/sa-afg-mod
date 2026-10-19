"""Place buildings, props and vegetation on the generated world."""
import math

import numpy as np

from .buildings import PREFABS
from .world import CELL, CELLS, to_game


class Layout:
    def __init__(self, world, seed=1234, log=print):
        self.w = world
        self.rng = np.random.default_rng(seed)
        self.items = []      # (model, x, y, z, heading_deg)
        self.taken = []      # (x, y, radius)
        self.points = {}     # named gameplay points for scripts: name -> (x, y, z, heading)
        self.log = log
        for s in world.sites.values():
            getattr(self, "_" + s["kind"])(s)
            self.log(f"  {s['name']:20s} {sum(1 for _ in self._in_site(s)):4d} objects")
        n0 = len(self.items)
        self._roadside_trees()
        self._orchards()
        self._bushes()
        self.log(f"  vegetation: {len(self.items) - n0} objects; total {len(self.items)}")

    # ---------------------------------------------------------------- helpers
    def _in_site(self, s):
        for it in self.items:
            if math.hypot(it[1] - s["x"], it[2] - s["y"]) < s["r"] + 30:
                yield it

    def free(self, x, y, rad, road_clear=True):
        for tx, ty, tr in self.taken:
            if math.hypot(tx - x, ty - y) < tr + rad:
                return False
        if road_clear:
            r, c = (3000 - y) / CELL, (x + 3000) / CELL
            r0, r1 = int(max(r - rad / CELL - 2, 0)), int(min(r + rad / CELL + 2, CELLS))
            c0, c1 = int(max(c - rad / CELL - 2, 0)), int(min(c + rad / CELL + 2, CELLS))
            if self.w.road_mask[r0:r1, c0:c1].min() < 7:
                return False
        return True

    def put(self, model, x, y, heading=0.0, z=None, check=True, road_clear=True, rad=None):
        rad = PREFABS[model][2] if rad is None else rad
        if check and not self.free(x, y, rad, road_clear):
            return False
        if z is None:
            z = self.w.height(x, y)
        self.items.append((model, float(x), float(y), float(z), float(heading) % 360))
        self.taken.append((x, y, rad))
        return True

    def put_near(self, model, x, y, heading=0.0, rmax=90.0):
        """Place at (x, y) or the nearest free spot on a widening spiral. Returns the position used."""
        for r in np.arange(0, rmax, 4.0):
            steps = max(1, int(2 * math.pi * r / 6))
            for k in range(steps):
                a = 2 * math.pi * k / steps
                px, py = x + r * math.cos(a), y + r * math.sin(a)
                if self.put(model, px, py, heading):
                    return px, py
        raise RuntimeError(f"no room for {model} near ({x:.0f}, {y:.0f})")

    @staticmethod
    def rot(dx, dy, heading):
        a = math.radians(heading)
        return dx * math.cos(a) - dy * math.sin(a), dx * math.sin(a) + dy * math.cos(a)

    def point(self, name, x, y, heading=0.0, z=None):
        self.points[name] = (float(x), float(y), float(self.w.height(x, y) if z is None else z), float(heading))

    def scatter(self, s, models, count, rmin, rmax, headings=(0, 90, 180, 270), tries=60):
        placed = 0
        for _ in range(count * tries):
            if placed >= count:
                break
            a = self.rng.uniform(0, 2 * math.pi)
            d = self.rng.uniform(rmin, rmax)
            x, y = s["x"] + d * math.cos(a), s["y"] + d * math.sin(a)
            m = models[self.rng.integers(len(models))]
            if self.put(m, x, y, float(self.rng.choice(headings))):
                placed += 1
        return placed

    def _road_heading_near(self, x, y):
        """Heading (deg) of the nearest road segment."""
        best = None
        for p in self.w.road_paths:
            pts = p["pts"]
            d = np.hypot(pts[:, 0] - x, pts[:, 1] - y)
            i = int(d.argmin())
            if best is None or d[i] < best[0]:
                j = min(i + 1, len(pts) - 1)
                i0 = max(j - 2, 0)
                dx, dy = pts[j] - pts[i0]
                best = (d[i], math.degrees(math.atan2(dy, dx)), pts[i])
        return best

    # ---------------------------------------------------------------- site kinds
    def _town(self, s):
        x, y = s["x"], s["y"]
        self.taken.append((x, y, 8))  # central square
        mx, my = self.put_near("afg_mosque", x + 2, y + 34, 0)
        self.put("afg_well", x - 6, y + 4, 0, check=False)
        # bazaar: two shop rows facing each other across an east-west lane
        for i, dx in enumerate((-55, -30, 30, 55)):
            self.put("afg_shops", x + dx, y + 14, 0, road_clear=False)
            self.put("afg_shops", x + dx, y - 14, 180, road_clear=False)
        px, py = self.put_near("afg_police", x + 75, y - 55, 0)
        self.point("police", px, py - 12, 180)
        # player's safehouse: compound west of the bazaar
        sx, sy = self.put_near("afg_compound", x - 95, y - 45, 0)
        self.point("safehouse", sx, sy - 6, 180)            # inside the courtyard
        self.point("safehouse_car", sx + 8, sy - 22, 90)    # outside the gate
        self.point("bazaar", x - 30, y, 90)
        self.point("mosque", mx, my - 16, 0)
        self.scatter(s, ["afg_compound", "afg_compound", "afg_house_l", "afg_house_s", "afg_house_s"], 45, 40, s["r"])
        self.scatter(s, ["afg_house_s", "afg_well", "afg_haystack"], 15, 30, s["r"] + 40)
        self.put("afg_wtower", x + s["r"] - 10, y + s["r"] * 0.3, 0)

    def _village(self, s):
        x, y = s["x"], s["y"]
        self.put_near("afg_mosque", x, y + 18, 0, rmax=s["r"])
        self.put("afg_well", x + 4, y - 2, 0)
        n = int(s["r"] / 9)
        self.scatter(s, ["afg_compound", "afg_compound", "afg_house_l", "afg_house_s"], n, 25, s["r"])
        self.scatter(s, ["afg_haystack", "afg_house_s", "afg_well"], 6, 20, s["r"] + 30)
        self.put("afg_wtower", x - s["r"] * 0.7, y + s["r"] * 0.5, 0)
        self.point(s["name"], x, y - 6, 0)

    def _base(self, s):
        x, y = s["x"], s["y"]
        H = 180  # half size of perimeter
        segs = 0
        for side in range(4):
            for i in range(36):
                t = -H + 5 + i * 10
                px, py, hd = [(x + t, y - H, 0), (x + H, y + t, 90), (x - t, y + H, 180), (x - H, y - t, 270)][side]
                if self.w.road_mask[int((3000 - py) / CELL), int((px + 3000) / CELL)] < 16:
                    continue  # gate
                self.put("afg_hesco", px, py, hd, check=False)
                segs += 1
        for cx, cy in [(-1, -1), (1, -1), (1, 1), (-1, 1), (0, -1), (0, 1), (-1, 0), (1, 0)]:
            self.put("afg_gtower", x + cx * (H - 6), y + cy * (H - 6), 0, check=False)
        self.taken.append((x, y, 0))
        # runway along north-south on the east side
        self.put("afg_runway", x + 120, y, 0, check=False, rad=30)
        self.taken.append((x + 120, y, 25))
        for dy in (-90, -30, 30, 90):
            self.taken.append((x + 120, y + dy, 25))
        self.put("afg_hangar", x + 70, y + 110, 90, check=False)
        self.put("afg_hangar", x + 70, y + 40, 90, check=False)
        self.put("afg_helipad", x + 70, y - 40, 0, check=False)
        self.put("afg_helipad", x + 70, y - 80, 0, check=False)
        self.point("base_heli", x + 70, y - 40, 0)
        self.point("base_runway", x + 120, y - 130, 0)
        for i in range(4):
            self.put("afg_barracks", x - 100, y + 100 - i * 25, 0, check=False)
        for i in range(6):
            self.put("afg_tent", x - 40 + (i % 3) * 14, y + 100 - (i // 3) * 20, 0, check=False)
        for i in range(10):
            self.put("afg_contain", x - 130 + (i % 5) * 7, y - 100 - (i // 5) * 4, 0, check=False)
        self.put("afg_police", x - 40, y - 60, 0, check=False)  # HQ building reuses the station model
        self.point("base_hq", x - 40, y - 72, 180)
        self.point("base_gate", x, y - H - 20, 0)
        self.point("base_motorpool", x - 100, y - 60, 0)

    def _checkpoint(self, s):
        x, y = s["x"], s["y"]
        dist, hd, (rx, ry) = self._road_heading_near(x, y)
        nx, ny = -math.sin(math.radians(hd)), math.cos(math.radians(hd))
        self.put("afg_cp_hut", rx + nx * 14, ry + ny * 14, hd, check=False)
        self.put("afg_gtower", rx - nx * 16, ry - ny * 16, hd, check=False)
        for k, off in enumerate((-8, 8)):
            bx, by = rx + nx * off, ry + ny * off
            self.put("afg_barrier", bx, by, hd + 90, check=False)
        fx, fy = math.cos(math.radians(hd)), math.sin(math.radians(hd))
        for side in (-1, 1):
            self.put("afg_sandbags", rx + nx * 11 * side + fx * 6, ry + ny * 11 * side + fy * 6, hd + 90, check=False)
            self.put("afg_hesco", rx + nx * 22 * side, ry + ny * 22 * side, hd + 90, check=False)
        self.point("checkpoint", rx, ry, hd)

    def _camp(self, s):
        x, y = s["x"], s["y"]
        self.put("afg_cave", x, y + 25, 0)
        self.scatter(s, ["afg_tent_dk"], 6, 12, 45, headings=(0, 30, 60, 90, 120, 150))
        self.scatter(s, ["afg_sandbags"], 6, 25, 55, headings=(0, 45, 90, 135))
        self.put("afg_wtower", x - 35, y - 10, 0)
        self.point("bandit_camp", x, y, 0)

    def _fortress(self, s):
        x, y = s["x"], s["y"]
        self.put("afg_fortress", x, y, 0, check=False)
        self.taken.append((x, y, 28))
        self.scatter(s, ["afg_tent_dk", "afg_sandbags"], 6, 32, 60, headings=(0, 90, 180, 270))
        self.point("fortress_gate", x, y - 30, 0)
        self.point("fortress_keep", x, y + 4, 180)

    def _ruins(self, s):
        x, y = s["x"], s["y"]
        self.put("afg_ruintower", x, y, 0, check=False)
        self.taken.append((x, y, 5))
        for i, (dx, dy, hd) in enumerate([(0, 20, 0), (20, 0, 90), (0, -20, 180), (-20, 0, 270)]):
            self.put("afg_ruinwall", x + dx, y + dy, hd, check=False)
        self.point("ruins", x, y - 30, 0)

    # ---------------------------------------------------------------- vegetation
    def _roadside_trees(self):
        sites = [s for s in self.w.sites.values() if s["kind"] in ("village", "town")]
        for p in self.w.road_paths:
            pts = p["pts"]
            seglen = np.hypot(*np.diff(pts, axis=0).T)
            acc = 0.0
            for i in range(1, len(pts)):
                acc += seglen[i - 1]
                if acc < 13:
                    continue
                acc = 0.0
                x, y = pts[i]
                near = min(math.hypot(x - s["x"], y - s["y"]) - s["r"] for s in sites)
                if near > 350 or self.rng.random() < 0.25:
                    continue
                dx, dy = pts[i] - pts[i - 1]
                ln = math.hypot(dx, dy) or 1
                nx, ny = -dy / ln, dx / ln
                for side in (-1, 1):
                    tx, ty = x + nx * 11 * side, y + ny * 11 * side
                    self.put("afg_poplar", tx, ty, self.rng.uniform(0, 360), z=self.w.height(tx, ty) - 0.3,
                             rad=2)

    def _orchards(self):
        fr, fc = np.nonzero(self.w.field_mask)
        idx = self.rng.choice(len(fr), size=min(900, len(fr)), replace=False)
        for i in idx:
            x, y = to_game(fr[i] + 0.5, fc[i] + 0.5)
            x += self.rng.uniform(-3, 3); y += self.rng.uniform(-3, 3)
            model = "afg_mulberry" if self.rng.random() < 0.7 else "afg_poplar"
            self.put(model, x, y, self.rng.uniform(0, 360), z=self.w.height(x, y) - 0.3, rad=3)

    def _bushes(self):
        cand = np.argwhere((self.w.mat == 2) & (self.w.cell_slope < 15))
        idx = self.rng.choice(len(cand), size=min(700, len(cand)), replace=False)
        for i in idx:
            x, y = to_game(cand[i][0] + 0.5, cand[i][1] + 0.5)
            self.put("afg_bush", x, y, self.rng.uniform(0, 360), z=self.w.height(x, y), rad=1.5, road_clear=False)
