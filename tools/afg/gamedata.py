"""Game data files (gta.dat, zones, water, timecyc, FXT) and the compiled mission script."""
import math
import os
import re
import shutil
import subprocess

import numpy as np

from .text import TEXT
from .world import HALF

TOOLS = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
REF = os.path.join(TOOLS, "reference")
GTA3SC = os.path.join(TOOLS, "vendor", "gta3sc", "build", "gta3sc")
SCRIPTS = os.path.join(TOOLS, "scripts")
COL_FILES = ["afg_land00", "afg_land01", "afg_land02", "afg_land10", "afg_land11", "afg_land12",
             "afg_land20", "afg_land21", "afg_land22", "afg_props"]


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

    def roadside(self, name, pts, dist, side=7.0):
        x, y, h = self.along(pts, dist)
        a = math.radians(h)
        # right-hand side of travel direction
        rx, ry = math.cos(a), math.sin(a)
        self.add(name, x + rx * side, y + ry * side, h, dz=1.2)

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
            blend(v, (9, 10, 11), (62, 112, 196), 0.45)      # sky top: deep blue
            blend(v, (12, 13, 14), (214, 196, 162), 0.50)    # horizon: dust
            blend(v, (0, 1, 2), (60, 50, 40), 0.25)          # warmer ambient
        else:
            blend(v, (9, 10, 11), (8, 12, 30), 0.30)         # clear mountain night
        if hours[h] in ("6AM", "7PM"):
            blend(v, (12, 13, 14), (235, 150, 90), 0.35)     # golden dawn / dusk
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
