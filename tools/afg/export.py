"""Write game files: terrain tiles + LODs, building models, collision, TXDs, IDE, IPL, radar."""
import math
import os
import sys

import numpy as np

from . import rw, textures
from .buildings import PREFABS
from .dem import hillshade
from .world import CELL, CELLS, HALF, N, SURFACE, STEEP_SURFACE, TERRAIN_TEX, to_game, MAT_ROAD, MAT_FIELD, MAT_ROCK

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "vendor", "DragonFF"))
from gtaLib import col as dcol  # noqa: E402

TILE = 32                    # cells per HD tile (250 units)
LOD_TILE = 64                # cells per LOD tile (500 units)
LOD_STEP = 2
NT = CELLS // TILE           # 24
NL = CELLS // LOD_TILE       # 12
TXD_TERRAIN, TXD_BUILD = "afg_terrain", "afg_build"
UV_SCALE = 8.0               # terrain texture repeats every 8 units
COL_REGION = 8               # HD tiles per collision archive side (8x8 tiles = 2000 units)
SUN = np.array([-0.45, 0.55, 0.70]) / np.linalg.norm([-0.45, 0.55, 0.70])


def quat_heading(deg):
    """IPL rotation for a heading (degrees, counter-clockwise around Z).

    SA stores the conjugate (inverse) rotation in text IPLs, so the Z component
    is negated relative to the textbook quaternion.
    """
    a = math.radians(deg) / 2
    return 0.0, 0.0, -math.sin(a), math.cos(a)


class Exporter:
    def __init__(self, world, layout, out, id_base, log=print):
        self.w, self.L, self.out, self.log = world, layout, out, log
        self.next_id = id_base
        self.ide = []        # (id, name, txd, drawdist, flags)
        self.ids = {}
        self.inst = []       # (id, name, x, y, z, qx, qy, qz, qw, lod)
        self.col_files = {}  # filename -> [ColModel]
        os.makedirs(out, exist_ok=True)
        z = world.z
        self.gx = np.gradient(z, axis=1) / CELL           # dz/dx
        self.gy = -np.gradient(z, axis=0) / CELL          # dz/dy (rows run south)
        n = np.stack([-self.gx, -self.gy, np.ones_like(z)], -1)
        self.nrm = n / np.linalg.norm(n, axis=-1, keepdims=True)
        # baked light: lambert + soft ambient occlusion from local concavity
        lam = np.clip(self.nrm @ SUN, 0, 1)
        lap = (np.roll(z, 1, 0) + np.roll(z, -1, 0) + np.roll(z, 1, 1) + np.roll(z, -1, 1) - 4 * z) / CELL
        ao = np.clip(1 - np.clip(-lap, 0, None) * 0.25, 0.75, 1.0) * np.clip(1 + np.clip(lap, 0, None) * 0.1, 1, 1.1)
        tint = textures._noise(12, 99, 1024)[:N, :N] * 0.16 + 0.92
        self.light = np.clip((0.50 + 0.55 * lam) * ao * tint, 0, 1.15)

    def new_id(self, name, txd, dist, flags=0):
        mid = self.next_id
        self.next_id += 1
        self.ide.append((mid, name, txd, dist, flags))
        self.ids[name] = mid
        return mid

    # ------------------------------------------------------------------ terrain
    def _tile_mesh(self, r0, c0, cells, step, centre, lowered=0.0):
        """Return (pos, nrm, uv, col, ncol, tris, materials_used) for a tile."""
        w = self.w
        cx, cy, cz = centre
        pos, nrm, uv, col, tris = [], [], [], [], []
        mat_ids = []
        n = cells // step
        for i in range(n):
            for j in range(n):
                r, c = r0 + i * step, c0 + j * step
                corners = [(r + step, c), (r + step, c + step), (r, c + step), (r, c)]  # SW, SE, NE, NW
                m = int(w.mat[min(r + step // 2, CELLS - 1), min(c + step // 2, CELLS - 1)])
                cn = self.nrm[min(r + step // 2, N - 1), min(c + step // 2, N - 1)]
                steep = cn[2] < 0.72
                base = len(pos)
                for rr, cc in corners:
                    x, y = to_game(rr, cc)
                    z = w.z[rr, cc] - lowered
                    pos.append((x - cx, y - cy, z - cz))
                    nrm.append(self.nrm[rr, cc])
                    if steep:  # project on the vertical plane facing the slope
                        if abs(cn[0]) > abs(cn[1]):
                            uv.append((y / UV_SCALE, -z / UV_SCALE))
                        else:
                            uv.append((x / UV_SCALE, -z / UV_SCALE))
                    else:
                        uv.append((x / UV_SCALE, -y / UV_SCALE))
                    li = self.light[rr, cc]
                    if m == MAT_ROAD:
                        li *= 0.95
                    col.append(li)
                tris.append((base, base + 1, base + 2, m))
                tris.append((base, base + 2, base + 3, m))
                mat_ids.append(m)
        used = sorted(set(mat_ids))
        remap = {m: k for k, m in enumerate(used)}
        tris = [(a, b, c, remap[m]) for a, b, c, m in tris]
        col = np.asarray(col)
        day = np.clip(np.stack([col * 0.86, col * 0.82, col * 0.76, np.ones_like(col) / 1.0], -1) * 255, 0, 255)
        day[:, 3] = 255
        night = day.copy()
        night[:, :3] = np.clip(day[:, :3] * np.array([0.30, 0.33, 0.45]), 0, 255)
        return pos, nrm, uv, day.astype(np.uint8), night.astype(np.uint8), tris, used

    def _write_tile_dff(self, name, data):
        pos, nrm, uv, day, night, tris, used = data

        class M:
            pass
        m = M()
        m.pos, m.nrm, m.uv, m.col, m.ncol, m.tris = pos, None, uv, day, night, tris  # prelit only
        m.materials = [{"tex": TERRAIN_TEX[u]} for u in used]
        rw.write_dff(os.path.join(self.out, name + ".dff"), m, name)

    def _tile_col(self, name, r0, c0, centre):
        cx, cy, cz = centre
        w = self.w
        verts = []
        for i in range(TILE + 1):
            for j in range(TILE + 1):
                x, y = to_game(r0 + i, c0 + j)
                verts.append((x - cx, y - cy, w.z[r0 + i, c0 + j] - cz))
        dcol.Sections.init_sections(3)
        faces, groups = [], []
        G = 8
        for gi in range(0, TILE, G):
            for gj in range(0, TILE, G):
                start = len(faces)
                for i in range(gi, gi + G):
                    for j in range(gj, gj + G):
                        r, c = r0 + i, c0 + j
                        m = int(w.mat[r, c])
                        surf = SURFACE[m]
                        if w.cell_slope[r, c] > 42 and m != MAT_ROAD:
                            surf = STEEP_SURFACE
                        nw, ne = i * (TILE + 1) + j, i * (TILE + 1) + j + 1
                        sw, se = nw + TILE + 1, ne + TILE + 1
                        faces.append(dcol.TFace(sw, se, ne, surf, 0xCC))
                        faces.append(dcol.TFace(sw, ne, nw, surf, 0xCC))
                idx = [k for f in faces[start:] for k in f[:3]]
                p = np.asarray([verts[k] for k in idx])
                groups.append(dcol.TFaceGroup(tuple(p.min(0)), tuple(p.max(0)), start, len(faces) - 1))
        vz = np.asarray(verts)
        if np.abs(vz).max() > 255:
            raise ValueError(f"{name}: collision vertex out of COL range ({np.abs(vz).max():.1f})")
        return self._col_model(name, verts, faces, groups, [])

    def _col_model(self, name, verts, faces, groups, boxes):
        cm = dcol.ColModel()
        cm.version = 3
        cm.model_name = name
        cm.model_id = self.ids[name]
        pts = [v for v in verts] + [b.min for b in boxes] + [b.max for b in boxes]
        p = np.asarray(pts, np.float64)
        mn, mx = p.min(0), p.max(0)
        ctr = (mn + mx) / 2
        cm.bounds = dcol.TBounds(tuple(mn), tuple(mx), tuple(ctr), float(np.linalg.norm(mx - ctr)))
        cm.mesh_verts = verts
        cm.mesh_faces = faces
        cm.face_groups = groups
        cm.boxes = boxes
        return cm

    def terrain(self):
        w = self.w
        # LOD tiles first so their IPL indices are 0..NL*NL-1
        lod_index = {}
        for li in range(NL):
            for lj in range(NL):
                name = f"lodafg_t{li:02d}{lj:02d}"
                r0, c0 = li * LOD_TILE, lj * LOD_TILE
                x0, y0 = to_game(r0 + LOD_TILE / 2, c0 + LOD_TILE / 2)
                block = w.z[r0:r0 + LOD_TILE + 1, c0:c0 + LOD_TILE + 1]
                centre = (x0, y0, float((block.min() + block.max()) / 2))
                self.new_id(name, TXD_TERRAIN, 3000)
                self._write_tile_dff(name, self._tile_mesh(r0, c0, LOD_TILE, LOD_STEP, centre, lowered=1.2))
                lod_index[(li, lj)] = len(self.inst)
                self.inst.append((self.ids[name], name, *centre, 0, 0, 0, 1, -1))
        for ti in range(NT):
            for tj in range(NT):
                name = f"afg_t{ti:02d}{tj:02d}"
                r0, c0 = ti * TILE, tj * TILE
                x0, y0 = to_game(r0 + TILE / 2, c0 + TILE / 2)
                block = w.z[r0:r0 + TILE + 1, c0:c0 + TILE + 1]
                centre = (x0, y0, float((block.min() + block.max()) / 2))
                self.new_id(name, TXD_TERRAIN, 299)
                self._write_tile_dff(name, self._tile_mesh(r0, c0, TILE, 1, centre))
                cm = self._tile_col(name, r0, c0, centre)
                region = f"afg_land{ti // COL_REGION}{tj // COL_REGION}.col"
                self.col_files.setdefault(region, []).append(cm)
                lod = lod_index[(ti * TILE // LOD_TILE, tj * TILE // LOD_TILE)]
                self.inst.append((self.ids[name], name, *centre, 0, 0, 0, 1, lod))
        self.log(f"  terrain: {NT * NT} tiles + {NL * NL} LOD tiles")

    # ------------------------------------------------------------------ buildings
    def buildings(self):
        dcol.Sections.init_sections(3)
        props = []
        for name, (builder, dist, _) in PREFABS.items():
            m = builder()
            # bake simple lighting into the prelight colours
            m.bake_lighting()
            self.new_id(name, TXD_BUILD, dist)
            rw.write_dff(os.path.join(self.out, name + ".dff"), m, name)
            boxes = [dcol.TBox(tuple(a), tuple(b), dcol.TSurface(s, 0, 0, 0xCC)) for a, b, s in m.boxes]
            if not boxes:
                continue  # no collision (bushes)
            props.append(self._col_model(name, [], [], [], boxes))
        self.col_files["afg_props.col"] = props
        for model, x, y, z, hd in self.L.items:
            self.inst.append((self.ids[model], model, x, y, z, *quat_heading(hd), -1))
        self.log(f"  buildings: {len(PREFABS)} models, {len(self.L.items)} placements")

    # ------------------------------------------------------------------ textures
    def txds(self):
        rw.write_txd(os.path.join(self.out, TXD_TERRAIN + ".txd"), textures.generate(TERRAIN_TEX))
        build = sorted({m["tex"] for b, _, _ in PREFABS.values() for m in b().materials})
        rw.write_txd(os.path.join(self.out, TXD_BUILD + ".txd"), textures.generate(build))
        self.log(f"  txd: {len(TERRAIN_TEX)} terrain + {len(build)} building textures")

    def collisions(self):
        for fname, models in self.col_files.items():
            c = dcol.coll()
            c.models = models
            with open(os.path.join(self.out, fname), "wb") as f:
                f.write(c.write_memory())
        self.log(f"  col: {len(self.col_files)} archives")

    # ------------------------------------------------------------------ data files
    def ide_ipl(self, data_dir):
        os.makedirs(data_dir, exist_ok=True)
        with open(os.path.join(data_dir, "afghan.ide"), "w", newline="\r\n") as f:
            f.write("# Afghanistan mod - generated by tools/build.py\nobjs\n")
            for mid, name, txd, dist, flags in self.ide:
                f.write(f"{mid}, {name}, {txd}, {dist}, {flags}\n")
            f.write("end\n")
        with open(os.path.join(data_dir, "afghan.ipl"), "w", newline="\r\n") as f:
            f.write("# Afghanistan mod - generated by tools/build.py\ninst\n")
            for mid, name, x, y, z, qx, qy, qz, qw, lod in self.inst:
                f.write(f"{mid}, {name}, 0, {x:.3f}, {y:.3f}, {z:.3f}, {qx:.6f}, {qy:.6f}, {qz:.6f}, {qw:.6f}, {lod}\n")
            f.write("end\n")
        self.log(f"  ide: {len(self.ide)} models (ids {self.ide[0][0]}..{self.ide[-1][0]}), ipl: {len(self.inst)} instances")

    # ------------------------------------------------------------------ radar
    def radar(self, preview_path=None):
        w = self.w
        px = 1536
        pal = np.array([(205, 182, 140), (170, 140, 105), (165, 160, 110), (110, 145, 70),
                        (135, 125, 115), (240, 242, 248), (120, 92, 62)], float)
        hs = hillshade(w.z, CELL)[:-1, :-1]
        img = pal[w.mat] * (0.55 + 0.45 * hs[..., None])
        from PIL import Image, ImageDraw
        im = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8)).resize((px, px), Image.BILINEAR)
        d = ImageDraw.Draw(im)
        k = px / (2 * HALF)
        for p in w.road_paths:
            pts = [((x + HALF) * k, (HALF - y) * k) for x, y in p["pts"][::2]]
            d.line(pts, fill=(95, 70, 45), width=3)
        for model, x, y, z, hd in self.L.items:
            r = PREFABS[model][2]
            if model in ("afg_bush", "afg_poplar", "afg_mulberry", "afg_haystack", "afg_well"):
                continue
            X, Y, R = (x + HALF) * k, (HALF - y) * k, max(r * k * 0.8, 1)
            d.rectangle([X - R, Y - R, X + R, Y + R], fill=(150, 110, 75))
        if preview_path:
            im.save(preview_path)
        arr = np.asarray(im.convert("RGBA"))
        for row in range(12):
            for colm in range(12):
                tile = arr[row * 128:(row + 1) * 128, colm * 128:(colm + 1) * 128]
                name = f"radar{row * 12 + colm:02d}"
                rw.write_txd(os.path.join(self.out, name + ".txd"), [(name, np.ascontiguousarray(tile), False)],
                             mipmaps=False)
        self.log("  radar: 144 tiles")
