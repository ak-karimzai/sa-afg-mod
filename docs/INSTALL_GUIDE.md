# Step-by-step: turning an original GTA San Andreas into "The Valley"

This guide takes you from a normal, unmodified copy of **GTA San Andreas (PC, classic edition)** to the Afghanistan
mod, on Windows or on Linux with Wine. Nothing here needs modding experience. Budget about 15 minutes.

The short version: **copy the game folder → make sure `gta_sa.exe` is the 1.0 US exe → run the installer → New Game.**

---

## 0. What you need

| | |
|---|---|
| **The game** | Your own copy of GTA San Andreas for PC, the *classic* 2005 game. The *Definitive Edition* (2021) is a different engine and cannot be modded this way. |
| **The 1.0 US `gta_sa.exe`** | The only version ModLoader and CLEO work with. Step 2 explains how to get it from whatever copy you have. **The mod does not include this file: it is Rockstar's copyrighted executable and must come from your own game.** |
| **Disk space** | About 400 MB on top of the game (the game itself is ~4.5 GB). |
| **Windows** | Windows 7 or newer, PowerShell 5 (built in). |
| **Linux / macOS** | Wine 8 or newer (tested with Wine 10 in a 64-bit prefix) and Python 3. |
| **The mod** | `AfghanistanMod-v1.1.0.zip` from the releases page. It already contains ModLoader, CLEO 5 and SilentPatch; you do not download those separately. |

---

## 1. Make a copy of the game folder

Never mod the only copy you have. Copy the whole game folder somewhere you own and that is not protected:

- Windows: for example `C:\Games\GTA San Andreas` (**not** `C:\Program Files`, which Windows protects, and **not** a
  network drive or a shared folder).
- Linux: for example `~/Games/GTA San Andreas` (a normal folder, not an NTFS mount if you can avoid it).

Where the original lives:

| Copy | Usual location |
|---|---|
| Steam | `C:\Program Files (x86)\Steam\steamapps\common\Grand Theft Auto San Andreas` |
| Rockstar Games Launcher | `C:\Program Files\Rockstar Games\Grand Theft Auto San Andreas` |
| Disc (2005) | `C:\Program Files (x86)\Rockstar Games\GTA San Andreas` |

Also back up your saves if you care about them: `Documents\GTA San Andreas User Files`. The mod uses the same save
slots.

---

## 2. Get the 1.0 US `gta_sa.exe`

Open the copied folder and look at `gta_sa.exe`. Which case are you?

| Your `gta_sa.exe` | Where it comes from | What to do |
|---|---|---|
| **~5.19 MB** (5,190,656 bytes) | Already a 1.0 US "Compact"/"HOODLUM" exe | Nothing. Go to step 3. |
| **~14.4 MB**, the game asks for the disc | Original 1.0 disc exe (copy-protected) | Rename it to `gta_sa_original.exe` and put a 1.0 US *Compact* exe next to it as `gta_sa.exe` (see below). |
| **~14.4 MB**, version 1.01 | Patched disc copy | Same as above: the 1.01 exe is not supported by CLEO. |
| **~4–5 MB but the game has no "Version 1.0" line**, or the folder came from Steam / Rockstar Launcher | Version 3.0 (Steam) / 1.0 (Rockstar, 2.0 engine) | Downgrade to 1.0 US (see below). |

**How to check the version in game:** start the game, in the main menu press **F1** (or look at the bottom right of
the main menu). A 1.0 exe shows nothing or "Version 1.0"; Steam shows "3.00".

### Downgrading a Steam or Rockstar Launcher copy

1. Search for *"GTA San Andreas downgrader"* or *"GTA SA Steam to 1.0 downgrade patch"* on a GTA modding community
   site you trust (gtaforums.com, gtagarage.com, gtainside.com, libertycity.net all host one).
2. Point the downgrader at your **copied** folder (never at the Steam folder itself). It replaces `gta_sa.exe` with the
   1.0 US exe and restores the audio files that Rockstar removed in later versions.
3. If the downgrader asks which exe to install, pick **1.0 US (HOODLUM / Compact)**, not EU and not 1.01.

### Replacing a disc exe

1. From the same kind of site, download the **"GTA SA 1.0 US Compact exe"** (sometimes called "no-CD 1.0 US",
   about 5 MB).
2. In your copied folder, rename the old `gta_sa.exe` to `gta_sa_original.exe`.
3. Put the downloaded file in the folder as `gta_sa.exe`.

### How to be sure it is right

The installer in step 3 checks the exe for you and refuses to continue if it is wrong. Technically it reads the PE
entry point, which for the 1.0 US exe is `0x824570`. If you want to check yourself before installing:

```powershell
# Windows PowerShell
$f=[IO.File]::OpenRead("C:\Games\GTA San Andreas\gta_sa.exe"); $b=New-Object IO.BinaryReader($f)
$f.Seek(0x3C,0)|Out-Null; $pe=$b.ReadInt32(); $f.Seek($pe+0x28,0)|Out-Null; '{0:X}' -f ($b.ReadUInt32()+0x400000); $f.Close()
```

```bash
# Linux
python3 -c "import struct,sys;d=open(sys.argv[1],'rb').read();pe=struct.unpack_from('<I',d,0x3c)[0];print(hex(struct.unpack_from('<I',d,pe+0x28)[0]+0x400000))" "GTA San Andreas/gta_sa.exe"
```

Both should print `824570`.

---

## 3. Install the mod

1. Unzip `AfghanistanMod-v1.1.0.zip` anywhere (for example your Downloads folder). You get `INSTALL.bat`,
   `UNINSTALL.bat`, `install.py`, a `game` folder, a `requirements` folder and `docs`.
2. Run the installer:
   - **Windows:** double-click **`INSTALL.bat`**. A folder dialog opens: pick the copied game folder (the one with
     `gta_sa.exe`). If Windows SmartScreen complains, choose *More info → Run anyway*; the installer is a plain
     PowerShell script you can open in Notepad.
   - **Linux / macOS:**
     ```bash
     cd ~/Downloads/AfghanistanMod-v1.1.0
     python3 install.py "$HOME/Games/GTA San Andreas"
     ```
3. Read the output. It should end with `installed; emptied N vanilla stream IPLs. Start a NEW GAME.`

What the installer did, so nothing is a surprise:

| Step | What happens |
|---|---|
| Version check | Stops with instructions if `gta_sa.exe` is not 1.0 US. |
| ModLoader 0.3.10 | Copied in if `modloader.asi` is missing. Its *AutoRefresh* is switched off (it breaks under Wine and on network drives). |
| CLEO 5.4.0 + ASI Loader | Copied in if `cleo.asi` is missing. The ASI loader replaces `vorbisFile.dll`; your original is saved to `_afghanmod_backup\` (CLEO also keeps it as `vorbisHooked.dll`). This is the **only original file that changes**. |
| SilentPatch | Copied in if `SilentPatchSA.asi` is missing. |
| The mod | New folders only: `modloader\afghanistan\`, `data\maps\afghan\`, `CLEO\afghan_radio\`, plus `CLEO\afghan_radio.cs` and `CLEO\CLEO_TEXT\afghan.fxt`. |
| Empty IPLs | Reads your `models\gta3.img` and writes empty stand-ins for the vanilla map's streamed placement files into `modloader\afghanistan\emptied\`, so San Andreas buildings are not placed on the Afghan terrain. |

Your `gta3.img`, `gta_int.img`, `data\*.dat` and other original files are **not** modified. Everything the mod needs is
served through ModLoader.

---

## 4. First start

1. Start `gta_sa.exe` from the copied folder (Linux: `cd "~/Games/GTA San Andreas" && wine gta_sa.exe`).
2. In the menu choose **New Game**. Saves from the unmodded game will not work with the mod (and the other way round).
3. You start in your courtyard in Qala-e-Naw. The radar shows:
   - **R** – Kaka Rahim at the bazaar: walk into the red marker next to him to start the first mission;
   - a **floppy disk** – the save point in your courtyard;
   - a **T-shirt** – the wardrobe.
4. Controls are the normal game controls. In any vehicle the Afghan radio plays: **N** next track, **M** on/off.
5. Save at the courtyard marker whenever you like.

If instead you see the San Andreas map, you loaded an old save or the installer did not run on this folder.

---

## 5. Settings worth changing (optional)

- **Options → Display:** set your real resolution and move *Draw Distance* to maximum; the terrain is built for long
  views.
- **Frame limiter:** leave it **on**. The physics of this game misbehave above 30 FPS, and the escort jeep in mission 5
  follows better with it on.
- **Widescreen:** fine either way; cutscenes switch to letterbox on their own.
- **Linux:** if there are only GStreamer warnings about MPEG-1 in the terminal, ignore them; they only concern the
  intro videos.

---

## 6. Check that everything is in place

If something looks wrong, these are the files to look at before asking for help:

| File (in the game folder) | What it tells you |
|---|---|
| `modloader\modloader.log` | Whether ModLoader started and loaded `afghanistan` (look for `afghan_limits.asi` and `AFG_LAND*.COL`). |
| `cleo.log` | Whether CLEO started, found `afghan.fxt` (87+ entries) and `afghan_radio.cs`. "Unsupported game version" means the exe is wrong. |
| `modloader\afghanistan\main.scm` | Must exist (about 38 KB). |
| `CLEO\CLEO_TEXT\afghan.fxt` | Must exist; if text shows up as `AF1_01` labels in game, this file or CLEO is missing. |

The troubleshooting table in [TECHNICAL.md](TECHNICAL.md#troubleshooting) lists the known symptoms and their fixes.

---

## 7. Going back to the original game

- Run **`UNINSTALL.bat`** (or `python3 install.py --uninstall "<folder>"`). This removes the mod's folders and leaves
  ModLoader, CLEO and SilentPatch installed, because other mods may use them.
- To undo everything: delete the copied folder. Your original installation was never touched.
- If you only want the vanilla map back for a while, rename `modloader\afghanistan` to `modloader\_ignore_afghanistan`
  (ModLoader ignores folders starting with `_ignore`) and start a new game.

---

## 8. Making your own changes

You can change the mod without rebuilding anything:

| Want to… | Do this |
|---|---|
| Use your own music | Replace the mp3 files in `CLEO\afghan_radio\`, keeping the same file names. |
| Better car or character models | Drop any SA-compatible `.dff`/`.txd` pair into a new folder under `modloader\`, named like the model it replaces (for example `sandking.dff`/`.txd` for the Desert Pickup, `dnmylc.dff`/`.txd` for the villager outfit). ModLoader swaps it everywhere. |
| Change the text or dialogue | Edit `CLEO\CLEO_TEXT\afghan.fxt` (one `KEY text` per line, keys of at most 7 characters). |
| Change the weather look | Edit `modloader\afghanistan\data\timecyc.dat`. |

For missions, buildings, roads or the terrain itself you rebuild from source:

```bash
git clone https://github.com/ak-karimzai/sa-afg-mod.git
cd sa-afg-mod
tools/setup.sh                                   # once: fetches gta3sc and DragonFF into tools/vendor/
python3 tools/build.py --no-music                # full build into dist/ (~25 s; first run downloads elevation tiles)
python3 tools/dev/quick_scripts.py --install "/path/to/GTA San Andreas"   # after editing scripts or text (~8 s)
```

Mission scripts are in `tools/scripts/main/` (one file per mission, GTA3script), all text in `tools/afg/text.py`, the
named map coordinates the scripts use in `tools/afg/gamedata.py`. See [TECHNICAL.md](TECHNICAL.md) for how it fits
together.
