#!/usr/bin/env python3
"""Cross-platform installer (Linux/Wine/macOS or Windows with Python). Same steps as install.ps1.

    python3 install.py "/path/to/GTA San Andreas"
    python3 install.py --uninstall "/path/to/GTA San Andreas"
"""
import os
import shutil
import struct
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
REMOVE = ["modloader/afghanistan", "data/maps/afghan", "CLEO/afghan_radio", "CLEO/afghan_radio.cs",
          "CLEO/CLEO_TEXT/afghan.fxt"]
EMPTY_IPL = b"bnry" + struct.pack("<6I", 0, 0, 0, 0, 0, 0) + struct.pack("<I", 0x4C) + bytes(76 - 32)


def img_ipls(path):
    with open(path, "rb") as f:
        if f.read(4) != b"VER2":
            raise SystemExit(f"{path} is not an IMG v2 archive")
        (count,) = struct.unpack("<I", f.read(4))
        names = []
        for _ in range(count):
            entry = f.read(32)
            name = entry[8:].split(b"\0")[0].decode("ascii", "replace")
            if name.lower().endswith(".ipl"):
                names.append(name)
        return names


def find(game, rel):
    """Case-insensitive path lookup (game folders copied from Windows have mixed case)."""
    cur = game
    for part in rel.split("/"):
        match = [e for e in os.listdir(cur) if e.lower() == part.lower()] if os.path.isdir(cur) else []
        if not match:
            return None
        cur = os.path.join(cur, match[0])
    return cur


def install(game):
    if not find(game, "gta_sa.exe"):
        raise SystemExit("gta_sa.exe not found - pick the classic GTA San Andreas folder")
    if not find(game, "modloader"):
        raise SystemExit("ModLoader is not installed (no 'modloader' folder)")
    if not find(game, "cleo.asi"):
        print("WARNING: cleo.asi not found - CLEO 4.4+/5 is needed for text and radio")
    src = os.path.join(HERE, "game")
    for root, _, files in os.walk(src):
        rel = os.path.relpath(root, src)
        dst = os.path.join(game, rel)
        os.makedirs(dst, exist_ok=True)
        for fn in files:
            shutil.copy2(os.path.join(root, fn), os.path.join(dst, fn))
    total = 0
    for archive in ("gta3.img", "gta_int.img"):
        p = find(game, "models/" + archive)
        if not p:
            continue
        out = os.path.join(game, "modloader", "afghanistan", "emptied", archive)
        os.makedirs(out, exist_ok=True)
        for name in img_ipls(p):
            with open(os.path.join(out, name), "wb") as f:
                f.write(EMPTY_IPL)
            total += 1
    print(f"installed; emptied {total} vanilla stream IPLs. Start a NEW GAME.")


def uninstall(game):
    for rel in REMOVE:
        p = os.path.join(game, *rel.split("/"))
        if os.path.isdir(p):
            shutil.rmtree(p)
        elif os.path.exists(p):
            os.remove(p)
    print("removed")


if __name__ == "__main__":
    args = sys.argv[1:]
    if not args:
        raise SystemExit(__doc__)
    if args[0] == "--uninstall":
        uninstall(args[1])
    else:
        install(args[0])
