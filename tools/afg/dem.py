"""Download and resample real elevation data (AWS Terrain Tiles, Terrarium encoding)."""
import io
import math
import os
import urllib.request

import numpy as np
from PIL import Image

TILE_URL = "https://s3.amazonaws.com/elevation-tiles-prod/terrarium/{z}/{x}/{y}.png"
CACHE = os.path.join(os.path.dirname(__file__), "..", "..", "cache", "dem")


def _tile(z, x, y):
    os.makedirs(CACHE, exist_ok=True)
    path = os.path.join(CACHE, f"{z}_{x}_{y}.png")
    if not os.path.exists(path):
        with urllib.request.urlopen(TILE_URL.format(z=z, x=x, y=y), timeout=60) as r:
            data = r.read()
        with open(path, "wb") as f:
            f.write(data)
    rgb = np.asarray(Image.open(path).convert("RGB"), dtype=np.float64)
    return rgb[..., 0] * 256 + rgb[..., 1] + rgb[..., 2] / 256 - 32768


def _merc(lat, lon, z):
    n = 2 ** z * 256
    x = (lon + 180) / 360 * n
    s = math.sin(math.radians(lat))
    y = (0.5 - math.log((1 + s) / (1 - s)) / (4 * math.pi)) * n
    return x, y


def sample(lat, lon, size_m, n, z=12):
    """Return an n x n height grid (metres) centred on lat/lon covering size_m x size_m.

    Row 0 is the NORTH edge, column 0 the WEST edge.
    """
    cx, cy = _merc(lat, lon, z)
    m_per_px = 156543.03392 * math.cos(math.radians(lat)) / 2 ** z
    half_px = size_m / 2 / m_per_px
    x0, x1 = int(cx - half_px) - 2, int(cx + half_px) + 2
    y0, y1 = int(cy - half_px) - 2, int(cy + half_px) + 2
    tx0, tx1, ty0, ty1 = x0 // 256, x1 // 256, y0 // 256, y1 // 256
    mosaic = np.zeros(((ty1 - ty0 + 1) * 256, (tx1 - tx0 + 1) * 256))
    for ty in range(ty0, ty1 + 1):
        for tx in range(tx0, tx1 + 1):
            mosaic[(ty - ty0) * 256:(ty - ty0 + 1) * 256,
                   (tx - tx0) * 256:(tx - tx0 + 1) * 256] = _tile(z, tx, ty)
    # bilinear resample
    t = np.linspace(-half_px, half_px, n)
    px = cx + t - tx0 * 256
    py = cy + t - ty0 * 256
    gx, gy = np.meshgrid(px, py)
    ix, iy = np.floor(gx).astype(int), np.floor(gy).astype(int)
    fx, fy = gx - ix, gy - iy
    a = mosaic[iy, ix]; b = mosaic[iy, ix + 1]
    c = mosaic[iy + 1, ix]; d = mosaic[iy + 1, ix + 1]
    return (a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy


def hillshade(h, cell, az=315, alt=45):
    gy, gx = np.gradient(h, cell)
    slope = np.arctan(np.hypot(gx, gy))
    aspect = np.arctan2(-gx, gy)
    az, alt = math.radians(az), math.radians(alt)
    return np.clip(np.sin(alt) * np.cos(slope) +
                   np.cos(alt) * np.sin(slope) * np.cos(az - aspect), 0, 1)
