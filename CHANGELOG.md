# Changelog

## 1.0.1 – 2026-10-20

First version that starts in the game (tested under Wine on Linux: the game loads into Qala-e-Naw and the first
mission hint shows).

Fixes:
- **Crash while loading the map.** The 144 terrain LOD tiles and the bush model had no collision entry, and SA
  crashes in `CEntity::GetBoundRect` when it places a model without one. They now get bounds-only collision
  (new `afg_lod.col`, bush in `afg_props.col`).
- **Crash when the player is created.** With the city IDEs removed, about 11,400 DFFs in `gta3.img` have no model
  info and overflow the game's 550-entry "extra objects" directory, so `player.dff` was never registered. New
  `afghan_limits.asi` raises that directory to 16,000 entries (it checks the exact bytes first and does nothing on
  other exe versions).

Installer:
- Bundles and installs ModLoader 0.3.10, CLEO 5.4.0 (with Silent's ASI Loader) and SilentPatch when they are
  missing. An existing `vorbisFile.dll` is backed up to `_afghanmod_backup\` first.
- Checks that `gta_sa.exe` is the 1.0 US executable (by entry point, so the Compact exe is accepted) and stops with
  instructions if it is not.
- Turns off ModLoader's AutoRefresh, which fails under Wine and on network drives and floods `modloader.log`.
- Linux: folder names are matched case-insensitively, so CLEO's `cleo\` and the mod's `CLEO\` become one folder.

## 1.0 – 2026-10-19

First release (not run in the game).
