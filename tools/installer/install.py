#!/usr/bin/env python3
"""Cross-platform installer (Linux/Wine/macOS, or Windows with Python). Same steps as install.ps1.

    python3 install.py "/path/to/GTA San Andreas"
    python3 install.py --uninstall "/path/to/GTA San Andreas"

Options:
    --no-requirements   do not install the bundled ModLoader / CLEO 5 / SilentPatch
    --force             install even if gta_sa.exe is not the 1.0 US executable
"""
import os
import re
import shutil
import struct
import sys
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
REQ = os.path.join(HERE, "requirements")
REMOVE = ["modloader/afghanistan", "data/maps/afghan", "CLEO/afghan_radio", "CLEO/afghan_radio.cs",
          "CLEO/CLEO_TEXT/afghan.fxt"]
EMPTY_IPL = b"bnry" + struct.pack("<6I", 0, 0, 0, 0, 0, 0) + struct.pack("<I", 0x4C) + bytes(76 - 32)
EP_10US = 0x424570  # entry point RVA of the 1.0 US exe (HOODLUM / Compact)
BACKUP = "_afghanmod_backup"

MODLOADER = ("modloader-0.3.10.zip", "modloader.asi", {"readme.txt", "leia-me.txt"})
CLEO = ("CLEO-5.4.0-SilentASILoader.zip", "cleo.asi", {"cleo_readme"})
SILENTPATCH = ("SilentPatchSA-1.1-build34.1.zip", "silentpatchsa.asi", {"readme.txt"})
ASI_LOADER = {"vorbisfile.dll", "vorbishooked.dll"}


def find(game, rel):
    """Case-insensitive lookup (game folders copied from Windows have mixed case). None if missing."""
    cur = game
    for part in rel.split("/"):
        match = [e for e in os.listdir(cur) if e.lower() == part.lower()] if os.path.isdir(cur) else []
        if not match:
            return None
        cur = os.path.join(cur, match[0])
    return cur


def resolve(game, rel):
    """Like find(), but missing parts keep the given spelling. Existing folders are reused whatever their case,
    so CLEO's 'cleo/cleo_text' and the mod's 'CLEO/CLEO_TEXT' end up in one folder on case-sensitive systems."""
    cur = game
    for part in rel.split("/"):
        match = [e for e in os.listdir(cur) if e.lower() == part.lower()] if os.path.isdir(cur) else []
        cur = os.path.join(cur, match[0] if match else part)
    return cur


def copy_file(src, game, rel):
    dst = resolve(game, rel)
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    shutil.copy2(src, dst)


def exe_entry_point(path):
    with open(path, "rb") as f:
        f.seek(0x3C)
        (pe,) = struct.unpack("<I", f.read(4))
        f.seek(pe + 0x28)
        data = f.read(4)
    return struct.unpack("<I", data)[0] if len(data) == 4 else None


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


def extract(game, archive, skip, skip_files=()):
    with zipfile.ZipFile(os.path.join(REQ, archive)) as z:
        for info in z.infolist():
            name = info.filename.rstrip("/")
            top = name.split("/")[0].lower()
            if info.is_dir() or top in skip or name.lower() in skip_files:
                continue
            dst = resolve(game, name)
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            with z.open(info) as src, open(dst, "wb") as out:
                shutil.copyfileobj(src, out)


def install_requirements(game):
    if not os.path.isdir(REQ):
        print("(no bundled requirements folder - skipping ModLoader/CLEO/SilentPatch)")
        return
    for (archive, marker, skip), label in ((MODLOADER, "ModLoader"), (CLEO, "CLEO 5"), (SILENTPATCH, "SilentPatch")):
        if find(game, marker):
            print(f"{label}: already installed")
            continue
        skip_files = set()
        if archive == CLEO[0]:
            if find(game, "vorbisHooked.dll") or find(game, "dinput8.dll"):
                skip_files = ASI_LOADER  # an ASI loader is already there; keep it
            elif find(game, "vorbisFile.dll"):
                os.makedirs(os.path.join(game, BACKUP), exist_ok=True)
                shutil.copy2(find(game, "vorbisFile.dll"), os.path.join(game, BACKUP, "vorbisFile.dll"))
                print(f"  original vorbisFile.dll backed up to {BACKUP}/ (CLEO ships it as vorbisHooked.dll)")
        extract(game, archive, skip, skip_files)
        print(f"{label}: installed")


def configure_modloader(game):
    """AutoRefresh watches the modloader folder for changes. That fails under Wine and on network drives and
    floods modloader.log ("Failed to watch directory changes"), so turn it off. The value must not carry an
    inline comment: ModLoader reads 'false ; comment' as true."""
    data = find(game, "modloader/.data")
    if not data:
        return
    cfg = os.path.join(data, "config.ini")
    if not os.path.exists(cfg):
        default = os.path.join(data, "config.ini.0")
        if not os.path.exists(default):
            return
        shutil.copy2(default, cfg)
    with open(cfg, "rb") as f:
        text = f.read().decode("utf-8", "replace")
    new = re.sub(r"(?im)^[ \t]*AutoRefresh[ \t]*=.*?(\r?)$", r"AutoRefresh = false\1", text)
    if new != text:
        with open(cfg, "wb") as f:
            f.write(new.encode("utf-8"))
        print("ModLoader: AutoRefresh disabled")


def install(game, force=False, requirements=True):
    exe = find(game, "gta_sa.exe")
    if not exe:
        raise SystemExit("gta_sa.exe not found - pick the classic GTA San Andreas folder")
    img = find(game, "models/gta3.img")
    if not img:
        raise SystemExit("models/gta3.img not found")
    ep = exe_entry_point(exe)
    if ep != EP_10US:
        msg = ("gta_sa.exe is not the 1.0 US executable that ModLoader and CLEO need "
               f"(entry point {0x400000 + ep:#x}, expected {0x400000 + EP_10US:#x}).\n"
               "Steam / Rockstar Launcher copies: downgrade to 1.0 first.\n"
               "Old disc copies (copy protected 1.0): replace gta_sa.exe with the 1.0 US 'Compact' exe.\n"
               "See README, section 'Getting the 1.0 US exe'. Use --force to install anyway.")
        if not force:
            raise SystemExit("ERROR: " + msg)
        print("WARNING: " + msg)
    if requirements:
        install_requirements(game)
    if not find(game, "modloader"):
        raise SystemExit("ModLoader is not installed (no 'modloader' folder)")
    if not find(game, "cleo.asi"):
        print("WARNING: cleo.asi not found - CLEO 4.4+/5 is needed for text and radio")
    configure_modloader(game)
    src = os.path.join(HERE, "game")
    for root, _, files in os.walk(src):
        for fn in files:
            path = os.path.join(root, fn)
            copy_file(path, game, os.path.relpath(path, src).replace(os.sep, "/"))
    total = 0
    for archive in ("gta3.img", "gta_int.img"):
        p = find(game, "models/" + archive)
        if not p:
            continue
        out = os.path.join(resolve(game, "modloader/afghanistan"), "emptied", archive)
        os.makedirs(out, exist_ok=True)
        for name in img_ipls(p):
            with open(os.path.join(out, name), "wb") as f:
                f.write(EMPTY_IPL)
            total += 1
    print(f"installed; emptied {total} vanilla stream IPLs. Start a NEW GAME.")


def uninstall(game):
    for rel in REMOVE:
        p = find(game, rel)
        if p and os.path.isdir(p):
            shutil.rmtree(p)
        elif p:
            os.remove(p)
    print("removed the Afghanistan mod (ModLoader, CLEO and SilentPatch were left installed)")


if __name__ == "__main__":
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    flags = {a for a in sys.argv[1:] if a.startswith("--")}
    if not args:
        raise SystemExit(__doc__)
    if "--uninstall" in flags:
        uninstall(args[0])
    else:
        install(args[0], force="--force" in flags, requirements="--no-requirements" not in flags)
