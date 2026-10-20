#!/usr/bin/env python3
"""Rebuild only the text (FXT) and the scripts (main.scm, afghan_radio.cs), without touching models or music.

    python3 tools/dev/quick_scripts.py                      # -> dist/game (created if missing)
    python3 tools/dev/quick_scripts.py --install "/path/to/GTA San Andreas"

The world and layout are regenerated in memory (about 8 s) because the scripts need the map coordinates.
"""
import os
import shutil
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
TOOLS = os.path.dirname(HERE)
ROOT = os.path.dirname(TOOLS)
sys.path.insert(0, TOOLS)

from afg import gamedata, layout, world  # noqa: E402


def main():
    args = sys.argv[1:]
    game_dir = None
    if "--install" in args:
        game_dir = args[args.index("--install") + 1]
    dist_game = os.path.join(ROOT, "dist", "game")
    w = world.World()
    L = layout.Layout(w, log=lambda *a, **k: None)
    pts = gamedata.Points(w, L)
    cleo = os.path.join(dist_game, "CLEO")
    ml = os.path.join(dist_game, "modloader", "afghanistan")
    os.makedirs(os.path.join(cleo, "CLEO_TEXT"), exist_ok=True)
    os.makedirs(ml, exist_ok=True)
    fxt = os.path.join(cleo, "CLEO_TEXT", "afghan.fxt")
    gamedata.write_fxt(fxt)
    scm = os.path.join(ml, "main.scm")
    cs = os.path.join(cleo, "afghan_radio.cs")
    gamedata.compile_scripts(pts, os.path.join(ROOT, "build", "scripts"), scm, cs)
    print(f"fxt: {len(gamedata.TEXT)} strings; main.scm {os.path.getsize(scm)} bytes; afghan_radio.cs {os.path.getsize(cs)} bytes")
    if game_dir:
        for src, rel in ((fxt, "CLEO/CLEO_TEXT/afghan.fxt"), (scm, "modloader/afghanistan/main.scm"),
                         (cs, "CLEO/afghan_radio.cs")):
            dst = os.path.join(game_dir, rel)
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            shutil.copy(src, dst)
            print("installed", rel)


if __name__ == "__main__":
    main()
