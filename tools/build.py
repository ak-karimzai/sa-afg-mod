#!/usr/bin/env python3
"""Build the whole Afghanistan mod into ../dist.

    python3 tools/build.py            # everything
    python3 tools/build.py --no-music # skip audio synthesis (slow)
"""
import argparse
import json
import os
import shutil
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from afg import export, layout, world  # noqa: E402

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
DIST = os.path.join(ROOT, "dist")
ID_BASE = 11682  # first free model id in vanilla SA (see docs/TECHNICAL.md)
VERSION = "1.1.0"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--no-music", action="store_true")
    ap.add_argument("--no-scripts", action="store_true")
    ap.add_argument("--zip", action="store_true", help="also create release/AfghanistanMod-v<VERSION>.zip")
    args = ap.parse_args()
    t0 = time.time()
    if os.path.exists(DIST):
        shutil.rmtree(DIST)
    game = os.path.join(DIST, "game")              # mirrors the GTA SA folder
    models = os.path.join(game, "modloader", "afghanistan", "models")
    data = os.path.join(game, "data", "maps", "afghan")

    print("[1/6] world")
    w = world.World()
    print("[2/6] layout")
    L = layout.Layout(w)
    print("[3/6] models / collision / textures")
    ex = export.Exporter(w, L, models, ID_BASE)
    ex.terrain()
    ex.buildings()
    ex.txds()
    ex.collisions()
    ex.ide_ipl(data)
    print("[4/6] radar")
    os.makedirs(os.path.join(ROOT, "docs"), exist_ok=True)
    ex.radar(os.path.join(ROOT, "docs", "map.png"))
    if ex.next_id > 12799:
        raise SystemExit(f"model ids overflow the free range: {ex.next_id}")

    print("[5/6] scripts / data")
    from afg import gamedata
    pts = gamedata.build(w, L, game, ROOT, skip_scripts=args.no_scripts)
    # every named script coordinate, for modders and the preview renderer
    os.makedirs(os.path.join(ROOT, "build"), exist_ok=True)
    with open(os.path.join(ROOT, "build", "points.json"), "w") as f:
        json.dump({"points": pts.p, "sites": list(w.sites.values()),
                   "roads": [{"a": r["a"], "b": r["b"], "pts": r["pts"][::8].round(1).tolist(),
                              "z": r["z"][::8].round(2).tolist()} for r in w.road_paths]}, f, indent=1)
    print("[6/6] music")
    if not args.no_music:
        from afg import music
        cached = os.path.join(ROOT, "cache", "music")
        names = [t["name"] + ".mp3" for t in music.TRACKS]
        if not all(os.path.exists(os.path.join(cached, n)) for n in names):
            music.build(cached)
        else:
            print("  using cached tracks (delete cache/music to re-synthesise)")
        shutil.copytree(cached, os.path.join(game, "CLEO", "afghan_radio"))
    # installer + docs next to the game folder
    inst = os.path.join(ROOT, "tools", "installer")
    for fn in ("INSTALL.bat", "UNINSTALL.bat", "install.ps1", "uninstall.ps1", "install.py"):
        shutil.copy(os.path.join(inst, fn), os.path.join(DIST, fn))
    for fn in ("README.md", "CHANGELOG.md"):
        shutil.copy(os.path.join(ROOT, fn), os.path.join(DIST, fn))
    # ModLoader, CLEO 5 and SilentPatch (MIT), installed by the installer when missing
    shutil.copytree(os.path.join(ROOT, "third_party"), os.path.join(DIST, "requirements"))
    shutil.copytree(os.path.join(ROOT, "docs"), os.path.join(DIST, "docs"))
    if args.zip:
        z = shutil.make_archive(os.path.join(ROOT, "release", f"AfghanistanMod-v{VERSION}"), "zip", DIST)
        print(f"zip: {z} ({os.path.getsize(z) / 1e6:.0f} MB)")
    print(f"done in {time.time() - t0:.0f}s -> {DIST}")


if __name__ == "__main__":
    main()
