"""Game data files (gta.dat, zones, water, timecyc, FXT) and the compiled mission script."""
import math
import os
import re
import shutil
import subprocess

import numpy as np

from . import asi
from .text import TEXT
from .world import HALF

TOOLS = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
REF = os.path.join(TOOLS, "reference")
GTA3SC = os.path.join(TOOLS, "vendor", "gta3sc", "build", "gta3sc")
SCRIPTS = os.path.join(TOOLS, "scripts")
COL_FILES = ["afg_land00", "afg_land01", "afg_land02", "afg_land10", "afg_land11", "afg_land12",
             "afg_land20", "afg_land21", "afg_land22", "afg_props", "afg_lod"]


# =========================================================================== points

class Points:
    """Named script coordinates: name -> (x, y, z, heading)."""

    def __init__(self, world, layout):
        self.w, self.L = world, layout
        self.p = {}
        for k, (x, y, z, h) in layout.points.items():
            self.add(_slug(k), x, y, h)
        for s in world.sites.values():
            self.add("site_" + s["key"][4:].lower(), s["x"], s["y"])
        self._derived()
        self._mission_details()

    def add(self, name, x, y, h=0.0, dz=1.0, z=None):
        z = self.w.height(x, y) + dz if z is None else z
        self.p[name] = (float(x), float(y), float(z), float(h) % 360)

    def road(self, a, b):
        for r in self.w.road_paths:
            if (r["a"], r["b"]) == (a, b):
                return r["pts"]
            if (r["a"], r["b"]) == (b, a):
                return r["pts"][::-1]
        raise KeyError((a, b))

    def along(self, pts, dist):
        """Point and heading `dist` units along a polyline."""
        seg = np.hypot(*np.diff(pts, axis=0).T)
        acc = np.concatenate([[0], np.cumsum(seg)])
        dist = float(np.clip(dist, 0, acc[-1] - 1))
        i = int(np.searchsorted(acc, dist) - 1)
        i = max(i, 0)
        t = (dist - acc[i]) / max(seg[i], 1e-6)
        x, y = pts[i] + (pts[i + 1] - pts[i]) * t
        dx, dy = pts[i + 1] - pts[i]
        heading = math.degrees(math.atan2(-dx, dy))  # GTA heading: 0 = north, counter-clockwise
        return float(x), float(y), heading

    def roadside(self, name, pts, dist, side=7.0, dh=0.0, dz=1.2):
        """Point `dist` along a road, `side` units right of the travel direction, heading = travel + dh."""
        x, y, h = self.along(pts, dist)
        a = math.radians(h)
        # right-hand side of travel direction
        rx, ry = math.cos(a), math.sin(a)
        self.add(name, x + rx * side, y + ry * side, h + dh, dz=dz)

    def near_road(self, x, y):
        best = None
        for r in self.w.road_paths:
            d = np.hypot(r["pts"][:, 0] - x, r["pts"][:, 1] - y)
            i = int(d.argmin())
            if best is None or d[i] < best[0]:
                best = (d[i], r["pts"], i)
        _, pts, i = best
        seg = np.hypot(*np.diff(pts, axis=0).T)
        return pts, float(np.sum(seg[:i]))

    def open_spots(self, cx, cy, radius, n, seed):
        """Spots inside a site that are not inside a building footprint."""
        rng = np.random.default_rng(seed)
        out = []
        for _ in range(4000):
            a, d = rng.uniform(0, 2 * math.pi), rng.uniform(4, radius)
            x, y = cx + d * math.cos(a), cy + d * math.sin(a)
            if all(math.hypot(x - tx, y - ty) > tr + 1.5 for tx, ty, tr in self.L.taken if tr > 1):
                out.append((x, y))
                if len(out) == n:
                    break
        return out

    def _derived(self):
        w, s = self.w, self.w.sites
        sx, sy, _, _ = self.p["safehouse"]
        # mission contacts stand 2.5 units in front of the contact marker, facing it
        for marker, npc in (("bazaar", "bazaar_uncle"), ("police", "police_nasir"), ("base_hq", "base_major")):
            x, y, _, _ = self.p[marker]
            self.add(npc, x + 2.5, y, 90)
        self.add("save", sx + 6, sy, 0)
        self.add("wardrobe", sx - 6, sy, 0)
        # cars by the bazaar / safehouse
        pts, d = self.near_road(s["Qala-e-Naw"]["x"], s["Qala-e-Naw"]["y"])
        self.roadside("bazaar_car", pts, d + 40)
        self.roadside("bazaar_car2", pts, d + 60, side=-7)
        # bazaar extortionists: around the bazaar lane
        bx, by = s["Qala-e-Naw"]["x"], s["Qala-e-Naw"]["y"]
        for i, (dx, dy) in enumerate([(-45, 6), (-20, -6), (20, 5), (45, -5)]):
            self.add(f"thug{i}", bx + dx, by + dy)
        # checkpoint defence: attackers come down the valley road
        cp = s["Darwaza Checkpoint"]
        valley = self.road("Darwaza Checkpoint", "Dara-e-Sabz")
        for i, dist in enumerate((120, 135, 150, 165)):
            self.roadside(f"cp_att{i}", valley, dist, side=(-8, 8, -14, 14)[i])
        for i, (dx, dy) in enumerate([(10, 6), (-10, 6), (0, -12)]):
            self.add(f"cp_ally{i}", cp["x"] + dx, cp["y"] + dy)
        # aid convoy ambush 60% of the way up the valley
        length = float(np.hypot(*np.diff(valley, axis=0).T).sum())
        amb = 0.6 * length
        self.roadside("ambush", valley, amb, side=0)
        for i, (dd, side) in enumerate([(-10, 20), (5, -22), (15, 25), (25, -18), (35, 20), (-20, -25)]):
            self.roadside(f"amb{i}", valley, amb + dd, side=side)
        # base: aid truck + heli + soldiers
        bx, by, _, _ = self.p["base_motorpool"]
        self.add("aid_truck", bx, by, 0, dz=1.5)
        # bandit camp: ring of guards
        c = s["Sang-e-Siah"]
        spots = self.open_spots(c["x"], c["y"], 50, 10, 6)
        for i, (x, y) in enumerate(spots):
            self.add(f"camp{i}", x, y)
        # fortress courtyard + walls
        f = s["Qala-e-Kohna"]
        inner = [(-12, -10), (12, -10), (-15, 0), (15, 0), (-8, -16), (8, -16), (-15, 12), (15, 12), (0, -4),
                 (-5, 10), (5, 10)]
        for i, (dx, dy) in enumerate(inner):
            self.add(f"fort{i}", f["x"] + dx, f["y"] + dy, z=f["z"] + 1.0)
        self.add("fort_boss", f["x"], f["y"] + 3, 180, z=f["z"] + 1.0)
        self.add("fort_start", f["x"], f["y"] - 60, 0)
        for i, (dx, dy) in enumerate([(-30, 10), (30, 10), (0, 34)]):
            self.add(f"fort_rpg{i}", f["x"] + dx, f["y"] + dy)
        for i, (dx, dy) in enumerate([(-4, -4), (4, -4), (-8, -2), (8, -2)]):
            self.add(f"fort_ally{i}", f["x"] + dx, f["y"] - 60 + dy)
        # hospital / police restarts
        mx, my, _, _ = self.p["mosque"]
        self.add("hospital", mx - 10, my - 8, 180)
        # ambient sites: (key, cx, cy, radius, kind) kind 0 civilians, 1 army, 2 police
        self.ambient = []
        for name, kind in [("Qala-e-Naw", 0), ("Deh-e-Bagh", 0), ("Dara-e-Sabz", 0), ("Sar-e-Kotal", 0),
                           ("Shahrak", 0), ("Camp Sangar", 1), ("Darwaza Checkpoint", 2)]:
            st = s[name]
            rad = min(st["r"] * 0.85, 120)
            spots = self.open_spots(st["x"], st["y"], rad, 12, hash(name) % 1000)
            spots += [spots[-1]] * (12 - len(spots))
            self.ambient.append((name, st["x"], st["y"], kind, spots))
        # parked cars: (model, point)
        self.cars = []
        for name, models in [("Qala-e-Naw", [495, 422, 436, 529, 468, 543]),
                             ("Deh-e-Bagh", [478, 543, 531]), ("Dara-e-Sabz", [422, 468, 478]),
                             ("Sar-e-Kotal", [468, 478]), ("Shahrak", [543, 422, 529]),
                             ("Darwaza Checkpoint", [599, 599]), ("Sang-e-Siah", [495, 468])]:
            st = s[name]
            pts, d = self.near_road(st["x"], st["y"])
            for i, m in enumerate(models):
                key = f"car_{_slug(name)}_{i}"
                self.roadside(key, pts, d + 18 + 22 * i, side=7 if i % 2 == 0 else -7)
                self.cars.append((m, key))
        b = s["Camp Sangar"]
        for i, (m, dx, dy, h) in enumerate([(470, -100, -50, 90), (470, -100, -40, 90), (470, -100, -30, 90),
                                            (433, -100, -20, 90), (433, -100, -10, 90)]):
            self.add(f"car_base{i}", b["x"] + dx, b["y"] + dy, h, dz=1.5)
            self.cars.append((m, f"car_base{i}"))
        hx, hy, _, _ = self.p["base_heli"]
        self.add("car_cargobob", hx, hy - 40, 0, dz=1.5)
        self.cars.append((548, "car_cargobob"))
        rx, ry, _, _ = self.p["base_runway"]
        self.add("car_dodo", rx, ry, 0, dz=1.5)
        self.cars.append((593, "car_dodo"))
        self.add("hunter", hx + 30, hy + 20, 0, dz=1.5)
        self.add("rhino", hx - 25, hy + 60, 0, dz=1.5)
        # pickups (model, ammo, point)
        self.pickups = []
        self.add("pk_health_safe", sx + 3, sy + 3)
        self.pickups.append((1240, 0, "pk_health_safe"))
        px, py, _, _ = self.p["police"]
        self.add("pk_shotgun", px + 4, py, dz=0.8)
        self.pickups.append((349, 30, "pk_shotgun"))
        self.add("pk_armour_base", b["x"] - 30, b["y"] - 60, dz=0.8)
        self.pickups.append((1242, 0, "pk_armour_base"))
        self.add("pk_m4", b["x"] - 34, b["y"] - 60, dz=0.8)
        self.pickups.append((356, 150, "pk_m4"))
        self.add("pk_sniper", b["x"] - 38, b["y"] - 60, dz=0.8)
        self.pickups.append((358, 20, "pk_sniper"))
        for name in ("Deh-e-Bagh", "Dara-e-Sabz", "Shahrak", "Sar-e-Kotal"):
            x, y = self.open_spots(s[name]["x"], s[name]["y"], 30, 1, 3)[0]
            key = "pk_health_" + _slug(name)
            self.add(key, x, y, dz=0.8)
            self.pickups.append((1240, 0, key))

    # approximate half extents (x, y) of the solid part of each prefab, in model space (see buildings.py)
    SOLID = {"afg_shops": (11, 5), "afg_police": (9.2, 8.5), "afg_mosque": (12, 12), "afg_house_s": (5, 5),
             "afg_house_l": (6, 6), "afg_well": (1, 1), "afg_wtower": (2.5, 2.5), "afg_barracks": (10, 6),
             "afg_tent": (5, 5), "afg_tent_dk": (5, 5), "afg_contain": (3.5, 1.5), "afg_hesco": (5, 1),
             "afg_gtower": (2.5, 2.5), "afg_hangar": (20, 15), "afg_haystack": (2, 2), "afg_cp_hut": (2, 2),
             "afg_sandbags": (3, 0.5), "afg_barrier": (2, 0.4), "afg_cave": (8, 6), "afg_fortress": (23.6, 23.6),
             "afg_ruinwall": (10, 1), "afg_ruintower": (3, 3), "afg_poplar": (0.6, 0.6), "afg_mulberry": (1, 1)}

    def _clear(self, px, py, margin=0.5):
        """True if (px, py) is not inside the solid footprint of any placed building or prop."""
        for model, x, y, _, h in self.L.items:
            if abs(x - px) > 30 or abs(y - py) > 30 or model not in self.SOLID:
                continue
            ex, ey = self.SOLID[model]
            a = math.radians(-h)
            dx, dy = px - x, py - y
            lx = dx * math.cos(a) - dy * math.sin(a)
            ly = dx * math.sin(a) + dy * math.cos(a)
            if abs(lx) <= ex + margin and abs(ly) <= ey + margin:
                return False
        return True

    def _mission_details(self):
        """Cutscene cameras, roadblocks, NPC marks and other per-mission points (v1.1 'real missions')."""
        s = self.w.sites
        # --- story contacts: marker m, contact 2.5 units east of it facing west, player on the marker facing east.
        # The wide shot is taken from the south (open side: bazaar lane / in front of the station facade).
        for marker in ("bazaar", "police", "base_hq"):
            x, y, z, _ = self.p[marker]
            g = z - 1.0
            self.add(f"talk_{marker}", x, y, 270)
            # two-shot: the first candidate position that is not inside a building
            for dx, dy in ((1.25, -4.6), (-2.5, -4.2), (5.0, -4.2), (1.25, 4.6), (-2.5, 4.2), (5.0, 4.2)):
                if self._clear(x + dx, y + dy):
                    break
            self.add(f"cam_{marker}_w", x + dx, y + dy, z=g + 1.9)
            self.add(f"cam_{marker}_wt", x + 1.25, y + 0.3, z=g + 1.1)
            self.add(f"cam_{marker}_c", x - 0.9, y - 0.85, z=g + 1.75)    # over the player's shoulder -> contact
            self.add(f"cam_{marker}_ct", x + 2.5, y, z=g + 1.5)
            self.add(f"cam_{marker}_p", x + 3.4, y + 0.85, z=g + 1.75)    # over the contact's shoulder -> player
            self.add(f"cam_{marker}_pt", x, y, z=g + 1.5)

        def arrival(prefix, marker):
            """An NPC waiting 4 units north of a village marker, the spot he walks to, and a camera on both."""
            x, y, _, _ = self.p[marker]
            self.add(f"{prefix}", x + 1.0, y + 4.0, 180)
            self.add(f"{prefix}_to", x - 2.5, y + 1.5, 90)
            for dx, dy in ((-7.0, -3.0), (7.0, -3.0), (-7.0, 3.0), (7.0, 3.0), (0.0, -8.0)):
                if self._clear(x + dx, y + dy):
                    break
            self.add(f"cam_{prefix}", x + dx, y + dy, dz=2.0)
            self.add(f"cam_{prefix}_t", x - 1.0, y + 2.0, dz=1.2)

        # --- mission 1: Black Stone roadblock half way to Deh-e-Bagh, the baker at the village
        rd = self.road("Qala-e-Naw", "Deh-e-Bagh")
        rl = float(np.hypot(*np.diff(rd, axis=0).T).sum())
        self.roadside("m1_block", rd, 0.52 * rl, side=3.5, dh=35, dz=1.5)   # pickup angled across half the road
        self.roadside("m1_thug0", rd, 0.52 * rl - 7, side=-2.5, dh=180)
        self.roadside("m1_thug1", rd, 0.52 * rl - 5, side=5.0, dh=180)
        arrival("m1_baker", "deh_e_bagh")
        # --- mission 2: the stall keeper being shaken down, bystanders
        tx, ty, _, _ = self.p["thug0"]
        self.add("m2_victim", tx + 2.2, ty, 90)
        for dx, dy in ((1.0, -6.5), (1.0, 6.5), (-6.0, -3.0), (8.0, -3.0)):
            if self._clear(tx + dx, ty + dy):
                break
        self.add("cam_m2", tx + dx, ty + dy, dz=1.8)
        self.add("cam_m2_t", tx + 1.0, ty + 0.2, dz=1.0)
        bx, by = s["Qala-e-Naw"]["x"], s["Qala-e-Naw"]["y"]
        for i, (dx, dy) in enumerate([(-12, -5), (6, 6), (32, -4)]):
            self.add(f"m2_civ{i}", bx + dx, by + dy)
        # --- mission 3: the doctor at Shahrak
        arrival("m3_doctor", "shahrak")
        # --- mission 4: attackers arriving by pickup, flankers
        valley = self.road("Darwaza Checkpoint", "Dara-e-Sabz")
        self.roadside("cp_car", valley, 190, side=0, dh=180, dz=1.5)
        self.roadside("cp_car2", valley, 215, side=0, dh=180, dz=1.5)
        for i, (dist, side) in enumerate([(130, -22), (145, 24)]):
            self.roadside(f"cp_flank{i}", valley, dist, side=side)
        # --- mission 5: escort jeep, roadblock and RPG strike at the ambush, the elder
        ax, ay, _, _ = self.p["aid_truck"]
        self.add("m5_jeep", ax, ay - 14, 0, dz=1.5)
        length = float(np.hypot(*np.diff(valley, axis=0).T).sum())
        amb = 0.6 * length
        self.roadside("m5_block", valley, amb + 55, side=3.5, dh=35, dz=1.5)
        self.roadside("m5_boom", valley, amb + 30, side=0)
        arrival("m5_elder", "dara_e_sabz")
        # --- mission 6: the stolen aid in front of the cave
        c = s["Sang-e-Siah"]
        self.add("m6_loot", c["x"], c["y"] + 14, dz=0.8)
        # --- epilogue in the safehouse courtyard (after the last mission)
        sx, sy, _, _ = self.p["safehouse"]
        self.add("m8_rahim", sx, sy + 3.0, 180)
        self.add("m8_farid", sx, sy, 0)
        self.add("cam_end", sx - 4.5, sy - 3.5, dz=1.9)
        self.add("cam_end_t", sx, sy + 1.5, dz=1.2)

        # ================= v1.2 action missions =================
        def first_clear(cands):
            for x, y in cands:
                if self._clear(x, y):
                    return x, y
            return cands[0]

        # --- Darwaza: two more attackers in the last wave
        for i, (dist, side) in enumerate([(180, -10), (195, 10)]):
            self.roadside(f"cp_att{4 + i}", valley, dist, side=side)
        # --- Black Stone camp: reinforcement pickup comes up from the valley
        camp_rd = self.road("Dara-e-Sabz", "Sang-e-Siah")
        camp_len = float(np.hypot(*np.diff(camp_rd, axis=0).T).sum())
        self.roadside("camp_car", camp_rd, camp_len - 170, side=0, dh=0, dz=1.5)
        # --- Burj-e-Khak: hostage rescue at the ruins, marksmen on the perimeter, pickups waiting on the way back
        r = s["Burj-e-Khak"]
        rx, ry = r["x"], r["y"]
        hx, hy = first_clear([(rx + 8, ry + 2), (rx - 8, ry + 2), (rx + 2, ry - 9)])
        self.add("bk_hostage", hx, hy, 90)
        for i, (x, y) in enumerate(self.open_spots(rx, ry, 45, 8, 11)):
            self.add(f"bk{i}", x, y)
        for i, (dx, dy) in enumerate([(-58, 34), (52, -44)]):
            x, y = first_clear([(rx + dx, ry + dy), (rx + dx + 8, ry + dy), (rx + dx, ry + dy - 8)])
            self.add(f"bk_sn{i}", x, y)
        rd = self.road("Camp Sangar", "Qala-e-Naw")
        self.roadside("bk_car0", rd, 0.35 * float(np.hypot(*np.diff(rd, axis=0).T).sum()), side=6.5, dz=1.5)
        rd = self.road("Qala-e-Naw", "Shahrak")
        self.roadside("bk_car1", rd, 0.45 * float(np.hypot(*np.diff(rd, axis=0).T).sum()), side=6.5, dz=1.5)
        # --- Fuel and Fire: three fuel stacks in Sar-e-Kotal, guards, reinforcements from the valley road
        k = s["Sar-e-Kotal"]
        for i, (x, y) in enumerate(self.open_spots(k["x"], k["y"], 32, 3, 21)):
            self.add(f"fd{i}", x, y)
        for i, (x, y) in enumerate(self.open_spots(k["x"], k["y"], 55, 8, 22)):
            self.add(f"fdg{i}", x, y)
        rd = self.road("Dara-e-Sabz", "Sar-e-Kotal")
        rl = float(np.hypot(*np.diff(rd, axis=0).T).sum())
        self.roadside("fd_car", rd, rl - 170, side=0, dh=0, dz=1.5)
        self.roadside("fd_r0", rd, rl - 150, side=-6)
        self.roadside("fd_r1", rd, rl - 145, side=6)
        # --- The Pass + the fortress chase: waypoints along the mountain road Deh-e-Bagh -> Qala-e-Kohna
        fort_rd = self.road("Deh-e-Bagh", "Qala-e-Kohna")
        self.route_fr = self._waypoints(fort_rd, 80.0)
        fl = float(np.hypot(*np.diff(fort_rd, axis=0).T).sum())
        ax, ay, ah = self.along(fort_rd, 0.4 * fl)
        a = math.radians(ah)
        cands = [(ax + math.cos(a) * sd, ay + math.sin(a) * sd) for sd in (12, -12, 18, -18, 8)]
        px, py = first_clear(cands)
        self.add("ps_amb", px, py, ah)
        assert len(self.route_fr) >= 30, len(self.route_fr)
        # the boss's getaway pickup waits inside the gate, pointing at the road
        gx, gy, _, _ = self.p["fortress_gate"]
        wx, wy, _, _ = self.route_fr[-3]
        self.add("fort_gate_car", gx, gy - 4, math.degrees(math.atan2(-(wx - gx), wy - gy)), dz=1.5)
        fx, fy, _, _ = self.p["fort_start"]
        self.add("fort_jeep", fx + 7, fy - 6, 0, dz=1.5)
        # --- Vulture: the Black Stone remnants flee Sang-e-Siah -> Dara-e-Sabz -> Darwaza
        rd = np.concatenate([self.road("Sang-e-Siah", "Dara-e-Sabz"), self.road("Dara-e-Sabz", "Darwaza Checkpoint")])
        self.route_cv = self._waypoints(rd, 80.0)
        assert len(self.route_cv) >= 40, len(self.route_cv)
        assert len(self.route_fr) <= 56 and len(self.route_cv) <= 64, (len(self.route_fr), len(self.route_cv))
        self._act2(first_clear)

    def _act2(self, first_clear):
        """v1.3 'The Snow Road': points for missions 13-20."""
        s, w = self.w.sites, self.w

        def slope(x, y, step=10.0):
            return max(abs(w.height(x + dx, y + dy) - w.height(x, y)) for dx, dy in
                       ((step, 0), (-step, 0), (0, step), (0, -step)))

        # --- 13 Ashes: night raid on the bazaar. Pickups come in from the Darwaza road, bikes from the
        # Deh-e-Bagh road, the leader flees back towards Deh-e-Bagh.
        qn = s["Qala-e-Naw"]
        rd = self.road("Qala-e-Naw", "Darwaza Checkpoint")
        self.roadside("as_car0", rd, 230, side=0, dh=180, dz=1.5)
        self.roadside("as_car1", rd, 262, side=0, dh=180, dz=1.5)
        qb = self.road("Qala-e-Naw", "Deh-e-Bagh")
        self.roadside("as_bike0", qb, 220, side=-3, dh=180, dz=1.2)
        self.roadside("as_bike1", qb, 240, side=3, dh=180, dz=1.2)
        for i, (dx, dy) in enumerate([(-60, 12), (-38, -12), (40, 10), (62, -8)]):
            x, y = first_clear([(qn["x"] + dx, qn["y"] + dy), (qn["x"] + dx, qn["y"] - dy), (qn["x"] + dx + 8, qn["y"])])
            self.add(f"as_foot{i}", x, y)
        pts, d = self.near_road(qn["x"], qn["y"])
        self.roadside("as_pcar", pts, d + 84, side=7, dz=1.5)        # Rahim's pickup for the chase
        self.roadside("as_flee", pts, d - 30, side=-6, dz=1.5)       # the leader's getaway pickup
        self.route_qb = self._waypoints(qb, 80.0)
        assert 10 <= len(self.route_qb) <= 24, len(self.route_qb)
        # --- 14 Courier: the money car parked at the Deh-e-Bagh roadside with its guards; drop-off at the station
        db = s["Deh-e-Bagh"]
        pts, d = self.near_road(db["x"], db["y"])
        self.roadside("co_car", pts, d + 95, side=-6, dz=1.5)
        for i, (dd, side) in enumerate([(-8, -10), (6, -11), (14, -4), (-2, 4)]):
            self.roadside(f"co_g{i}", pts, d + 95 + dd, side=side, dh=180)
        px, py, _, _ = self.p["police"]
        pts, d = self.near_road(px, py)
        self.roadside("co_drop", pts, d + 12, side=6, dz=1.2)
        for i, (dd, side) in enumerate([(30, 7), (46, -7)]):
            self.roadside(f"co_chase{i}", qb, 60 + dd, side=side, dh=180, dz=1.5)   # chasers wait on the town road
        # --- 15 The Wedding: the party in Dara-e-Sabz, a sniper post on the highest ground around it, three groups
        # of attackers converging on foot, a technical up the valley road at the end
        ds = s["Dara-e-Sabz"]
        for i, (x, y) in enumerate(self.open_spots(ds["x"], ds["y"], 22, 8, 31)):
            self.add(f"wd_guest{i}", x, y)
        ex, ey = first_clear([(ds["x"] + 4, ds["y"] + 6), (ds["x"] - 6, ds["y"] + 4), (ds["x"], ds["y"] - 8)])
        self.add("wd_elder", ex, ey, 180)
        best = None
        for ang in range(0, 360, 10):
            a = math.radians(ang)
            x, y = ds["x"] + 115 * math.cos(a), ds["y"] + 115 * math.sin(a)
            if abs(x) > 2900 or abs(y) > 2900 or not self._clear(x, y):
                continue
            h = w.height(x, y)
            if best is None or h > best[0]:
                best = (h, x, y, ang)
        _, x, y, post_ang = best
        self.add("wd_post", x, y, (math.degrees(math.atan2(-(ds["x"] - x), ds["y"] - y))) % 360)
        self.add("cam_wd", ds["x"] - 7, ds["y"] - 5, dz=2.0)
        self.add("cam_wd_t", ex, ey, dz=1.2)
        angs = [post_ang + 150, post_ang + 210, post_ang + 90]
        for g, ang in enumerate(angs):
            for i, (dr, da) in enumerate([(0, 0), (8, 6), (-6, -7), (14, -4)]):
                a = math.radians(ang + da)
                self.add(f"wd_a{g}_{i}", ds["x"] + (175 + dr) * math.cos(a), ds["y"] + (175 + dr) * math.sin(a))
        valley = self.road("Darwaza Checkpoint", "Dara-e-Sabz")
        vl = float(np.hypot(*np.diff(valley, axis=0).T).sum())
        self.roadside("wd_car", valley, vl - 260, side=0, dh=0, dz=1.5)
        # --- 16 Dust Storm: the Dodo down on the plain east of the base, the pilot beside it, Haji Gul's men
        # already there, two pickups waiting between the wreck and the base
        cands = [(2650 + dx, -1050 + dy) for dx in (0, -120, 120, -240, 240) for dy in (0, -150, 150, 300)]
        cands = [c for c in cands if abs(c[0]) < 2900 and abs(c[1]) < 2900]
        cands.sort(key=lambda c: slope(*c))
        cx, cy = cands[0]
        self.add("ds_crash", cx, cy, 300, dz=1.6)
        self.add("ds_pilot", cx + 6, cy - 3, 90)
        for i, (dx, dy) in enumerate([(-18, 14), (16, 18), (-22, -12), (20, -16), (0, 26), (26, 2)]):
            self.add(f"ds_e{i}", cx + dx, cy + dy)
        self.add("ds_ecar", cx - 30, cy + 30, 135, dz=1.5)
        bx, by = s["Camp Sangar"]["x"], s["Camp Sangar"]["y"]
        for i, t in enumerate((0.35, 0.55)):
            x, y = cx + (bx - cx) * t + (60 if i else -60), cy + (by - cy) * t
            self.add(f"ds_chase{i}", x, y, (math.degrees(math.atan2(-(cx - x), cy - y))) % 360, dz=1.5)
        self.add("cam_ds", cx - 9, cy - 9, dz=2.5)
        self.add("cam_ds_t", cx, cy, dz=1.5)
        # --- 17 Judas: the meeting in burnt-out Sar-e-Kotal, the trap closes with police pickups from the valley road
        sk = s["Sar-e-Kotal"]
        mx, my = first_clear([(sk["x"] + 6, sk["y"] - 10), (sk["x"] - 10, sk["y"] + 6), (sk["x"], sk["y"] + 14)])
        self.add("jd_meet", mx, my, 0)
        self.add("jd_inf", mx, my + 3, 180)
        self.add("cam_jd", mx - 6, my - 4, dz=1.9)
        self.add("cam_jd_t", mx, my + 1.5, dz=1.3)
        for i, (x, y) in enumerate(self.open_spots(sk["x"], sk["y"], 60, 6, 41)):
            self.add(f"jd_e{i}", x, y)
        rd = self.road("Dara-e-Sabz", "Sar-e-Kotal")
        rl = float(np.hypot(*np.diff(rd, axis=0).T).sum())
        self.roadside("jd_car0", rd, rl - 140, side=0, dh=0, dz=1.5)
        self.roadside("jd_car1", rd, rl - 175, side=0, dh=0, dz=1.5)
        # --- 18 Iron Harvest: the Rhino waits past the checkpoint; three roadblocks up the valley; the camp
        self.roadside("ih_tank", valley, 70, side=0, dh=0, dz=1.8)
        camp_rd = self.road("Dara-e-Sabz", "Sang-e-Siah")
        cl = float(np.hypot(*np.diff(camp_rd, axis=0).T).sum())
        for j, (road, dist) in enumerate([(valley, 0.42 * vl), (valley, 0.78 * vl), (camp_rd, 0.5 * cl)]):
            self.roadside(f"ih_b{j}_car", road, dist, side=0, dh=90, dz=1.5)
            for i, (dd, side) in enumerate([(12, -6), (12, 7), (20, -13), (22, 12)]):
                self.roadside(f"ih_b{j}_e{i}", road, dist + dd, side=side, dh=180)
        self.roadside("ih_tech", camp_rd, cl - 120, side=0, dh=180, dz=1.5)
        # --- 19 Traitor's Road: the jeep at the foot of the mountain road
        fort_rd = self.road("Deh-e-Bagh", "Qala-e-Kohna")
        self.roadside("tr_jeep", fort_rd, 14, side=6, dz=1.5)
        # --- 20 Haji Gul: the siege of Camp Sangar, then the helicopter from Burj-e-Khak northwards
        # the attack comes up the Qala-e-Naw road, so the fight is at the road's entry into the base perimeter
        rd = self.road("Qala-e-Naw", "Camp Sangar")
        rl = float(np.hypot(*np.diff(rd, axis=0).T).sum())
        self.roadside("hg_gate", rd, rl - 235, side=0, dh=180)
        for i, (dd, side) in enumerate([(10, -9), (10, 9), (16, -4), (16, 4), (6, -14), (6, 14)]):
            self.roadside(f"hg_ally{i}", rd, rl - 235 + dd, side=side, dh=180)
        for i, (dd, side) in enumerate([(0, -8), (0, 8), (-12, -14), (-12, 14), (-24, -5), (-24, 5), (-36, -11), (-36, 11)]):
            self.roadside(f"hg_att{i}", rd, rl - 340 + dd, side=side)
        for i, dd in enumerate((390, 430, 470)):
            self.roadside(f"hg_car{i}", rd, rl - dd, side=0, dh=0, dz=1.5)
        self.roadside("cam_hg", rd, rl - 215, side=12, dz=3.0)
        self.roadside("cam_hg_t", rd, rl - 250, side=0, dz=1.5)
        r = s["Burj-e-Khak"]
        self.add("hg_heli", r["x"] + 10, r["y"] + 34, 0, dz=1.8)
        for i, (x, y) in enumerate([(2900, -2000), (2900, -600), (2800, 1300), (2650, 2950)]):
            self.add(f"hg_wp{i}", x, y, z=w.height(x, y) + 110)
        # --- 14 Buzkashi: the flattest open ground 250-450 units from Qala-e-Naw, well away from the roads
        best = None
        for ang in range(0, 360, 12):
            for rad in (260, 330, 400, 460):
                a = math.radians(ang)
                x, y = qn["x"] + rad * math.cos(a), qn["y"] + rad * math.sin(a)
                if abs(x) > 2800 or abs(y) > 2800:
                    continue
                _, dist = self.near_road(x, y)
                roadd = min(float(np.hypot(r["pts"][:, 0] - x, r["pts"][:, 1] - y).min()) for r in w.road_paths)
                if roadd < 90:
                    continue
                sl = max(slope(x + dx, y + dy, 20.0) for dx, dy in ((0, 0), (50, 0), (-50, 0), (0, 50), (0, -50)))
                if best is None or sl < best[0]:
                    best = (sl, x, y)
        _, bx, by = best
        self.add("bz_center", bx, by, 0)
        self.add("bz_pit", bx + 65, by, 0)
        self.add("bz_pbike", bx - 70, by - 10, 90, dz=1.2)
        for i in range(5):
            a = math.radians(90 + i * 60)
            self.add(f"bz_r{i}", bx + 55 * math.cos(a), by + 55 * math.sin(a), (math.degrees(-a) + 90) % 360, dz=1.2)
        for i, ang in enumerate((200, 250)):
            a = math.radians(ang)
            x, y = bx + 170 * math.cos(a), by + 170 * math.sin(a)
            self.add(f"bz_car{i}", x, y, (math.degrees(math.atan2(-(bx - x), by - y))) % 360, dz=1.5)
        self.add("cam_bz", bx - 12, by - 12, dz=3.0)
        # --- 19 Jingle Truck: the truck at the fortress gate (fort_gate_car), the ramp and the block at the bottom
        fl = float(np.hypot(*np.diff(fort_rd, axis=0).T).sum())
        x, y, h = self.along(fort_rd, 150)
        self.add("jt_ramp", x, y, (h + 180) % 360, dz=0.0)
        self.roadside("jt_block0", fort_rd, 70, side=3.0, dh=60, dz=1.5)
        self.roadside("jt_block1", fort_rd, 84, side=-3.0, dh=120, dz=1.5)
        self.roadside("jt_chase0", fort_rd, fl - 120, side=4, dh=180, dz=1.5)
        self.roadside("jt_chase1", fort_rd, fl - 100, side=-4, dh=180, dz=1.5)
        x, y, _, _ = self.p["fort_start"]
        self.add("jt_drop", x - 12, y - 10, 0)
        self.add("cam_jt", x + 6, y - 18, dz=2.2)
        gx, gy, _, _ = self.p["fort_gate_car"]
        self.add("cam_jt_t", gx, gy, dz=1.4)
        # --- 20 Echoes in the Gorge: the column camped at 55% of the mountain road, sentries around it, charge spots
        # on the uphill side above the road, the drop-off and the safe point further down
        ec = 0.55 * fl
        for i, dd in enumerate((-30, -6, 18)):
            self.roadside(f"ec_t{i}", fort_rd, ec + dd, side=0, dh=0, dz=1.5)
        for i, (dd, side) in enumerate([(-50, 8), (-38, -9), (-14, 10), (6, -10), (32, 9), (44, -7)]):
            self.roadside(f"ec_s{i}", fort_rd, ec + dd, side=side, dh=180)
        for i, dd in enumerate((-26, 0, 26)):
            x, y, h = self.along(fort_rd, ec + dd)
            a = math.radians(h)
            # uphill side of the road: whichever side is higher
            lx, ly = x - math.cos(a) * 16, y - math.sin(a) * 16
            rx, ry = x + math.cos(a) * 16, y + math.sin(a) * 16
            cx, cy = (lx, ly) if w.height(lx, ly) > w.height(rx, ry) else (rx, ry)
            self.add(f"ec_c{i}", cx, cy)
        self.roadside("ec_start", fort_rd, ec - 160, side=6, dh=0)
        self.roadside("ec_safe", fort_rd, ec - 230, side=5, dh=0)
        for i, dd in enumerate((-40, -20, 0, 20, 40)):
            self.roadside(f"ec_x{i}", fort_rd, ec + dd, side=0)
        # --- 21 Flying Carpet: the sedan in the fortress courtyard, the drop point at the motor pool
        f = s["Qala-e-Kohna"]
        self.add("fc_car", f["x"], f["y"] - 6, 90, z=f["z"] + 1.5)
        mx, my, _, _ = self.p["base_motorpool"]
        self.add("fc_drop", mx + 20, my + 10, 0, dz=1.0)

    def _waypoints(self, pts, spacing):
        """(x, y, z, heading) every `spacing` units along a polyline, ending exactly at its end."""
        total = float(np.hypot(*np.diff(pts, axis=0).T).sum())
        out = []
        d = 0.0
        while d < total - spacing * 0.5:
            x, y, h = self.along(pts, d)
            out.append((x, y, self.w.height(x, y) + 1.0, h % 360))
            d += spacing
        x, y, h = self.along(pts, total - 1)
        out.append((x, y, self.w.height(x, y) + 1.0, h % 360))
        return out

    def substitute(self, src):
        def rep(m):
            name, field = m.group(1), m.group(2)
            if name not in self.p:
                raise KeyError(f"unknown script point @{name}@")
            x, y, z, h = self.p[name]
            if field is None:
                return f"{x:.2f} {y:.2f} {z:.2f}"
            return f"{dict(x=x, y=y, z=z, h=h)[field]:.2f}"
        return re.sub(r"@([a-z0-9_]+)(?:\.([xyzh]))?@", rep, src)


def _slug(s):
    return re.sub(r"[^a-z0-9]+", "_", s.lower()).strip("_")


# =========================================================================== data files

def write_gta_dat(path):
    keep_ide = re.compile(r"^IDE DATA\\MAPS\\(generic|veh_mods)\\|^IDE DATA\\MAPS\\interior\\(props|props2|propext)\.IDE|"
                          r"^IDE DATA\\TXDCUT\.IDE", re.I)
    lines = ["# GTA San Andreas - Afghanistan total conversion",
             "# Generated from the vanilla layout: city IDE/IPL lines removed, Afghanistan map added.",
             "# Remove the modloader\\afghanistan folder to get the original game back.", ""]
    with open(os.path.join(REF, "gta_dat_reference.dat")) as f:
        ref = [ln.strip() for ln in f]
    for ln in ref:
        if ln.startswith("IMG "):
            lines.append(ln)
    lines += ["", "SPLASH loadsc2", ""]
    lines += [ln for ln in ref if keep_ide.match(ln)]
    lines += ["IDE DATA\\MAPS\\AFGHAN\\AFGHAN.IDE", ""]
    lines += [f"COLFILE 0 DATA\\MAPS\\AFGHAN\\{c.upper()}.COL" for c in COL_FILES]
    lines += ["", "IPL DATA\\MAPS\\AFGHAN\\MAP.ZON", "IPL DATA\\MAPS\\AFGHAN\\INFO.ZON",
              "IPL DATA\\MAPS\\AFGHAN\\AFGHAN.IPL", ""]
    with open(path, "w", newline="\r\n") as f:
        f.write("\n".join(lines) + "\n")


def write_zones(world, map_path, info_path):
    with open(map_path, "w", newline="\r\n") as f:
        f.write("# Afghanistan mod: one island zone, level 0 = countryside (rural police, country peds)\n")
        f.write("zone\nAFGHAN, 3, -3000.0, -3000.0, -500.0, 3000.0, 3000.0, 1500.0, 0, UNUSED\nend\n")
    rows = [
        ("PLAIN", 0, 450, -3000, 3000, 3000, "AFZ_PL"),
        ("GHORB", 0, -3000, 300, 450, 1700, "AFZ_GV"),
        ("NMTNS", 0, -3000, 1700, 450, 3000, "AFZ_NM"),
        ("SMTNS", 0, -3000, -3000, 450, 300, "AFZ_SM"),
    ]
    for s in world.sites.values():
        r = s["r"] + 70
        rows.append((s["key"][4:] + "Z", 1, s["x"] - r, s["y"] - r, s["x"] + r, s["y"] + r, s["key"]))
    with open(info_path, "w", newline="\r\n") as f:
        f.write("# Afghanistan mod zones. Type 0 = region name, 1 = town (higher priority). Text from CLEO FXT.\nzone\n")
        for name, t, x0, y0, x1, y1, key in rows:
            assert len(name) <= 7 and len(key) <= 7
            f.write(f"{name}, {t}, {x0:.1f}, {y0:.1f}, -500.0, {x1:.1f}, {y1:.1f}, 1500.0, 0, {key}\n")
        f.write("end\n")


def write_water(path):
    # one invisible-from-above safety plane at z=0 under the whole map (terrain is always >= 4)
    shutil.copy(os.path.join(REF, "water_dat_single_quad_mapcleaner.dat"), path)


INT_COLS = set(range(0, 21)) | {24, 25, 26} | set(range(30, 36)) | {49, 50}


def write_timecyc(path):
    """Retint the 52-column (PS2-layout) reference timecyc for a dry, dusty, high-altitude look."""
    out = ["// Afghanistan mod timecyc - derived from the SA PS2-format timecyc (52 columns).",
           "// Dusty horizons, deep blue high-altitude sky, warm post-fx, longer view distance."]
    hours = ["Midnight", "5AM", "6AM", "7AM", "Midday", "7PM", "8PM", "10PM"]
    weathers = ["EXTRASUNNY_LA", "SUNNY_LA", "EXTRASUNNY_SMOG_LA", "SUNNY_SMOG_LA", "CLOUDY_LA", "SUNNY_SF",
                "EXTRASUNNY_SF", "CLOUDY_SF", "RAINY_SF", "FOGGY_SF", "SUNNY_VEGAS", "EXTRASUNNY_VEGAS",
                "CLOUDY_VEGAS", "EXTRASUNNY_COUNTRYSIDE", "SUNNY_COUNTRYSIDE", "CLOUDY_COUNTRYSIDE",
                "RAINY_COUNTRYSIDE", "EXTRASUNNY_DESERT", "SUNNY_DESERT", "SANDSTORM_DESERT", "UNDERWATER",
                "EXTRACOLOURS_1", "EXTRACOLOURS_2"]
    data = []
    with open(os.path.join(REF, "timecyc_sa_reference.dat")) as f:
        for ln in f:
            s = ln.strip()
            if not s or s.startswith("/"):
                continue
            vals = [float(v) for v in s.replace(",", " ").split()]
            if len(vals) == 51:  # one known reference line lacks DirMult (PC-style)
                vals.append(1.0)
            if len(vals) != 52:
                raise ValueError("reference timecyc line without 52 columns")
            vals[40] = min(vals[40], 127.0)  # post-fx alphas use the 0-127 range
            vals[44] = min(vals[44], 127.0)
            data.append(vals)
    assert len(data) == 184

    def blend(v, idx, target, t):
        for k, c in zip(idx, target):
            v[k] = v[k] * (1 - t) + c * t

    for i, v in enumerate(data):
        w, h = divmod(i, 8)
        day = hours[h] in ("6AM", "7AM", "Midday", "7PM")
        if weathers[w] in ("UNDERWATER", "EXTRACOLOURS_1", "EXTRACOLOURS_2"):
            continue
        storm = weathers[w] in ("SANDSTORM_DESERT", "FOGGY_SF", "RAINY_SF", "RAINY_COUNTRYSIDE")
        if day:
            blend(v, (9, 10, 11), (34, 72, 190), 0.50)       # sky top: lapis blue
            blend(v, (12, 13, 14), (218, 196, 150), 0.50)    # horizon: golden dust
            blend(v, (0, 1, 2), (62, 52, 38), 0.25)          # warmer ambient
        else:
            blend(v, (9, 10, 11), (10, 12, 40), 0.35)        # clear mountain night, indigo
            blend(v, (0, 1, 2), (22, 20, 46), 0.20)          # indigo shadows
        if hours[h] in ("6AM", "7PM"):
            blend(v, (12, 13, 14), (238, 152, 84), 0.40)     # golden dawn / dusk
        if hours[h] in ("8PM", "10PM"):
            blend(v, (12, 13, 14), (120, 70, 50), 0.25)      # amber afterglow low on the horizon
        blend(v, (41, 42, 43), (196, 152, 104), 0.30)        # warm post-fx 1
        blend(v, (30, 31, 32), (200, 184, 160), 0.30)        # low clouds dusty
        blend(v, (33, 34, 35), (215, 205, 190), 0.30)
        if not storm:
            v[27] = max(v[27], 1600.0 if day else 1100.0)     # far clip: see the mountains
            v[28] = max(v[28], 150.0)                         # fog start
        if weathers[w] == "SANDSTORM_DESERT":
            blend(v, (12, 13, 14), (190, 140, 90), 0.6)
            blend(v, (41, 42, 43), (180, 120, 70), 0.5)
    for i, v in enumerate(data):
        w, h = divmod(i, 8)
        if h == 0:
            out.append(f"//////////// {weathers[w]}")
        out.append(f"// {hours[h]}")
        cells = []
        for k, x in enumerate(v):
            cells.append(str(int(round(min(max(x, 0), 255 if k < 21 else 1e9)))) if k in INT_COLS else f"{x:.2f}")
        out.append(" ".join(cells))
    with open(path, "w", newline="\r\n") as f:
        f.write("\n".join(out) + "\n")


def write_fxt(path, extra=None):
    text = dict(TEXT)
    text.update(extra or {})
    with open(path, "w", encoding="cp1252", newline="\r\n") as f:
        f.write("# Afghanistan mod text (CLEO FXT)\n")
        for k, v in text.items():
            if len(k) > 7:
                raise ValueError(f"FXT key too long: {k}")
            if len(k) + 1 + len(v) > 250:
                raise ValueError(f"FXT line too long: {k}")
            f.write(f"{k} {v}\n")
    return text


# =========================================================================== scripts

def compile_scripts(points, build_dir, out_main, out_cleo):
    """Render templates with coordinates and compile main.scm + the CLEO radio script."""
    if os.path.exists(build_dir):
        shutil.rmtree(build_dir)
    os.makedirs(os.path.join(build_dir, "main"))
    gen = generated_header(points)
    for root, _, files in os.walk(SCRIPTS):
        for fn in files:
            if not fn.endswith(".sc"):
                continue
            with open(os.path.join(root, fn)) as f:
                src = f.read()
            src = src.replace("//@GENERATED_INIT@", gen["init"]).replace("//@GENERATED_VARS@", gen["vars"])
            src = points.substitute(src)
            rel = os.path.relpath(os.path.join(root, fn), SCRIPTS)
            dst = os.path.join(build_dir, rel)
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            with open(dst, "w") as f:
                f.write(src)
    os.makedirs(os.path.dirname(out_main), exist_ok=True)
    _gta3sc([os.path.join(build_dir, "main.sc"), "--config=gtasa", "--guesser", "-fno-entity-tracking", "-o", out_main], build_dir)
    img = os.path.join(os.path.dirname(out_main), "script.img")
    if os.path.exists(img):
        os.remove(img)  # we have no streamed scripts; keep the game's own script.img untouched
    os.makedirs(os.path.dirname(out_cleo), exist_ok=True)
    _gta3sc([os.path.join(build_dir, "cleo", "afghan_radio.sc"), "--config=gtasa", "--guesser", "-fno-entity-tracking", "--cs", "-o",
             out_cleo], build_dir)


def _gta3sc(args, cwd):
    r = subprocess.run([GTA3SC] + args, cwd=cwd, capture_output=True, text=True)
    msg = (r.stdout + r.stderr).strip()
    if r.returncode != 0:
        raise RuntimeError("gta3sc failed:\n" + msg)
    for line in msg.splitlines():
        if "warning" in line:
            print("   ", line)


def generated_header(points):
    """Global variable padding (the exe expects some globals at fixed indices) + data arrays."""
    # indices: player=2 scplayer=3 player_group=11 flag_player_on_mission=409 (gta3sc --expect-var)
    pad1 = " ".join(f"spare_var_{i}" for i in range(4, 11))
    pad2 = [f"spare_var_{i}" for i in range(12, 409)]
    vars_ = ["VAR_INT player scplayer", f"VAR_INT {pad1}", "VAR_INT player_group"]
    for i in range(0, len(pad2), 20):
        vars_.append("VAR_INT " + " ".join(pad2[i:i + 20]))
    vars_.append("VAR_INT flag_player_on_mission")
    init = []
    amb = points.ambient
    for si, (name, cx, cy, kind, spots) in enumerate(amb):
        init.append(f"amb_site_x[{si}] = {cx:.2f}")
        init.append(f"amb_site_y[{si}] = {cy:.2f}")
        init.append(f"amb_site_kind[{si}] = {kind}")
        for k, (x, y) in enumerate(spots):
            z = points.w.height(x, y) + 1.0
            init.append(f"amb_spot_x[{si * 12 + k}] = {x:.2f}")
            init.append(f"amb_spot_y[{si * 12 + k}] = {y:.2f}")
            init.append(f"amb_spot_z[{si * 12 + k}] = {z:.2f}")
    init.append(f"amb_site_count = {len(amb)}")
    for prefix, route in (("fr", points.route_fr), ("cv", points.route_cv), ("qb", points.route_qb)):
        for i, (x, y, z, h) in enumerate(route):
            init.append(f"{prefix}_x[{i}] = {x:.2f}")
            init.append(f"{prefix}_y[{i}] = {y:.2f}")
            init.append(f"{prefix}_z[{i}] = {z:.2f}")
            init.append(f"{prefix}_h[{i}] = {h:.2f}")
        init.append(f"{prefix}_count = {len(route)}")
    for i, (model, key) in enumerate(points.cars):
        x, y, z, h = points.p[key]
        init.append(f"CREATE_CAR_GENERATOR {x:.2f} {y:.2f} {z:.2f} {h:.2f} {model} -1 -1 0 0 0 0 10000 car_gen")
        init.append("SWITCH_CAR_GENERATOR car_gen 101")
    for model, ammo, key in points.pickups:
        x, y, z, _ = points.p[key]
        if ammo:
            init.append(f"CREATE_PICKUP_WITH_AMMO {model} PICKUP_ON_STREET_SLOW {ammo} {x:.2f} {y:.2f} {z:.2f} pickup")
        else:
            init.append(f"CREATE_PICKUP {model} PICKUP_ON_STREET_SLOW {x:.2f} {y:.2f} {z:.2f} pickup")
    return {"vars": "\n".join(vars_), "init": "\n".join(init)}


def build(world, layout, game_dir, root, skip_scripts=False):
    pts = Points(world, layout)
    afg = os.path.join(game_dir, "data", "maps", "afghan")
    ml = os.path.join(game_dir, "modloader", "afghanistan")
    os.makedirs(afg, exist_ok=True)
    os.makedirs(os.path.join(ml, "data"), exist_ok=True)
    # collision archives are loaded statically through COLFILE lines
    models = os.path.join(ml, "models")
    for c in COL_FILES:
        shutil.move(os.path.join(models, c + ".col"), os.path.join(afg, c + ".col"))
    write_gta_dat(os.path.join(ml, "data", "gta.dat"))
    asi.build(os.path.join(ml, "afghan_limits.asi"))  # extra-objects directory 550 -> 16000
    write_zones(world, os.path.join(afg, "map.zon"), os.path.join(afg, "info.zon"))
    write_water(os.path.join(ml, "data", "water.dat"))
    write_timecyc(os.path.join(ml, "data", "timecyc.dat"))
    cleo = os.path.join(game_dir, "CLEO")
    os.makedirs(os.path.join(cleo, "CLEO_TEXT"), exist_ok=True)
    write_fxt(os.path.join(cleo, "CLEO_TEXT", "afghan.fxt"))
    print(f"  data: gta.dat, zones, water, timecyc, fxt ({len(TEXT)} strings)")
    if not skip_scripts:
        compile_scripts(pts, os.path.join(root, "build", "scripts"), os.path.join(ml, "main.scm"),
                        os.path.join(cleo, "afghan_radio.cs"))
        print(f"  scripts: main.scm ({os.path.getsize(os.path.join(ml, 'main.scm'))} bytes), afghan_radio.cs")
    return pts
