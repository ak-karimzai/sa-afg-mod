"""Minimal RenderWare 3.6 writers for GTA San Andreas (PC): DFF models and TXD dictionaries."""
import struct

import numpy as np

RW_VERSION = 0x1803FFFF  # RW 3.6.0.3, what SA PC uses

CH_STRUCT, CH_STRING, CH_EXTENSION = 0x01, 0x02, 0x03
CH_TEXTURE, CH_MATERIAL, CH_MATLIST = 0x06, 0x07, 0x08
CH_FRAMELIST, CH_GEOMETRY, CH_CLUMP = 0x0E, 0x0F, 0x10
CH_ATOMIC, CH_TEXNATIVE, CH_TEXDICT, CH_GEOMLIST = 0x14, 0x15, 0x16, 0x1A
CH_BINMESH, CH_FRAME_NAME, CH_NIGHT_COLORS = 0x50E, 0x253F2FE, 0x253F2F9


def chunk(ctype, payload):
    return struct.pack("<III", ctype, len(payload), RW_VERSION) + payload


def rw_string(s):
    b = s.encode("ascii") + b"\0"
    b += b"\0" * (-len(b) % 4)
    return chunk(CH_STRING, b)


# --------------------------------------------------------------------------- DFF

def write_dff(path, mesh, frame_name):
    """Write a single-atomic clump. `mesh` is an afg.meshkit.Mesh."""
    pos = np.asarray(mesh.pos, np.float32)
    nrm = np.asarray(mesh.nrm, np.float32) if mesh.nrm is not None else None
    uv = np.asarray(mesh.uv, np.float32)
    col = np.asarray(mesh.col, np.uint8)
    ncol = np.asarray(mesh.ncol, np.uint8) if mesh.ncol is not None else None
    tris = np.asarray(mesh.tris, np.uint32)  # (n, 4): a, b, c, material
    nv, nt = len(pos), len(tris)
    if nv > 65535:
        raise ValueError(f"{frame_name}: too many vertices ({nv})")

    # --- frame list: one identity frame
    frame = struct.pack("<9f3fiI", 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, -1, 0)
    frame_ext = chunk(CH_EXTENSION, chunk(CH_FRAME_NAME, frame_name.encode("ascii")))
    framelist = chunk(CH_FRAMELIST, chunk(CH_STRUCT, struct.pack("<I", 1) + frame) + frame_ext)

    # --- geometry struct
    flags = 0x02 | 0x04 | 0x08 | 0x20  # positions, textured, prelit, light
    if nrm is not None:
        flags |= 0x10
    flags |= 1 << 16  # one UV set
    g = bytearray(struct.pack("<IIII", flags, nt, nv, 1))
    g += col.tobytes()
    g += uv.tobytes()
    tri_file = np.stack([tris[:, 1], tris[:, 0], tris[:, 3], tris[:, 2]], 1).astype(np.uint16)
    g += tri_file.tobytes()
    centre = (pos.min(0) + pos.max(0)) / 2
    radius = float(np.sqrt(((pos - centre) ** 2).sum(1)).max())
    g += struct.pack("<4fII", *centre, radius, 1, 0 if nrm is None else 1)
    g += pos.tobytes()
    if nrm is not None:
        g += nrm.tobytes()

    # --- materials
    mats = b""
    for m in mesh.materials:
        tex_struct = chunk(CH_STRUCT, struct.pack("<I", 0x1106))  # trilinear, wrap U/V
        tex = chunk(CH_TEXTURE, tex_struct + rw_string(m["tex"]) + rw_string("") + chunk(CH_EXTENSION, b""))
        r, gg, b, a = m.get("color", (255, 255, 255, 255))
        mstruct = struct.pack("<I4BIi3f", 0, r, gg, b, a, 0, 1, 1.0, 1.0, 1.0)
        mats += chunk(CH_MATERIAL, chunk(CH_STRUCT, mstruct) + tex + chunk(CH_EXTENSION, b""))
    nm = len(mesh.materials)
    matlist = chunk(CH_MATLIST, chunk(CH_STRUCT, struct.pack("<I", nm) + struct.pack(f"<{nm}i", *([-1] * nm))) + mats)

    # --- extensions: bin mesh (+ night colours)
    bm = bytearray()
    split_count = 0
    for mi in range(nm):
        idx = tris[tris[:, 3] == mi][:, :3].reshape(-1)
        if len(idx) == 0:
            continue
        split_count += 1
        bm += struct.pack("<II", len(idx), mi) + idx.astype(np.uint32).tobytes()
    binmesh = chunk(CH_BINMESH, struct.pack("<III", 0, split_count, nt * 3) + bm)
    ext = binmesh
    if ncol is not None:
        ext += chunk(CH_NIGHT_COLORS, struct.pack("<I", 1) + ncol.tobytes())
    geometry = chunk(CH_GEOMETRY, chunk(CH_STRUCT, bytes(g)) + matlist + chunk(CH_EXTENSION, ext))
    geomlist = chunk(CH_GEOMLIST, chunk(CH_STRUCT, struct.pack("<I", 1)) + geometry)

    atomic = chunk(CH_ATOMIC, chunk(CH_STRUCT, struct.pack("<IIII", 0, 0, 5, 0)) + chunk(CH_EXTENSION, b""))
    clump = chunk(CH_CLUMP, chunk(CH_STRUCT, struct.pack("<III", 1, 0, 0)) + framelist + geomlist + atomic
                  + chunk(CH_EXTENSION, b""))
    with open(path, "wb") as f:
        f.write(clump)


# --------------------------------------------------------------------------- TXD

def _mips(img):
    """img: (h, w, 4) uint8 RGBA -> list of levels down to 4x4."""
    levels = [img]
    cur = img.astype(np.float32)
    while min(cur.shape[:2]) > 4:
        h, w = cur.shape[0] // 2, cur.shape[1] // 2
        cur = cur[:h * 2, :w * 2].reshape(h, 2, w, 2, 4).mean((1, 3))
        levels.append(np.clip(cur + 0.5, 0, 255).astype(np.uint8))
    return levels


def write_txd(path, textures, mipmaps=True):
    """textures: list of (name, rgba ndarray (h, w, 4) uint8, has_alpha)."""
    natives = b""
    for name, img, alpha in textures:
        if len(name) > 31:
            raise ValueError(name)
        levels = _mips(img) if mipmaps else [img]
        h, w = img.shape[:2]
        raster = 0x0500 | (0x8000 if mipmaps else 0)  # 8888 (+ mipmaps)
        d3dfmt = 21  # D3DFMT_A8R8G8B8
        s = struct.pack("<IBBH", 9, 6 if mipmaps else 2, 0x11, 0)
        s += name.encode().ljust(32, b"\0") + b"".ljust(32, b"\0")
        s += struct.pack("<IIHHBBBB", raster, d3dfmt, w, h, 32, len(levels), 4, 1 if alpha else 0)
        for lv in levels:
            bgra = lv[..., [2, 1, 0, 3]].tobytes()
            s += struct.pack("<I", len(bgra)) + bgra
        natives += chunk(CH_TEXNATIVE, chunk(CH_STRUCT, s) + chunk(CH_EXTENSION, b""))
    body = chunk(CH_STRUCT, struct.pack("<HH", len(textures), 2)) + natives + chunk(CH_EXTENSION, b"")
    with open(path, "wb") as f:
        f.write(chunk(CH_TEXDICT, body))
