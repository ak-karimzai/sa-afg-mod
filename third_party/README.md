# Bundled third-party tools

These are the unmodified release archives of the tools the mod needs. The installer extracts each one only if
it is not already installed. All three are MIT-licensed (licence texts next to this file).

| Archive | Version | Source | SHA-256 |
|---|---|---|---|
| `modloader-0.3.10.zip` | ModLoader 0.3.10 by LINK/2012 | <https://github.com/thelink2012/modloader/releases/tag/v0.3.10> | `3bb0e3d656a55d8c0a7ce98876033e2b9b672e8b7b5596f8044ccaa8f38dc403` |
| `CLEO-5.4.0-SilentASILoader.zip` | CLEO 5.4.0 + Silent's ASI Loader (`SA.CLEO-v5.4.0+Silent_ASI_Loader.zip`) | <https://github.com/cleolibrary/CLEO5/releases/tag/v5.4.0> | `610d1a9324f318103756f17a073354887a5537810f041ba015445d7fb91d7832` |
| `SilentPatchSA-1.1-build34.1.zip` | SilentPatch for SA, 1.1 build 34.1 | <https://github.com/CookiePLMonster/SilentPatch/releases/tag/1.1-BUILD34.1-SA> | `0837bbdc0548a4a952f924adefd2bb6eacc866407f153844fe1f56995668c42c` |

The CLEO archive also contains `bass.dll` (BASS audio library by Un4seen Developments, free for non-commercial use)
and Silent's ASI Loader (`vorbisFile.dll`, with the game's original DLL renamed to `vorbisHooked.dll`).

To update a tool, replace its archive, update this table and the file name in `tools/installer/install.py` and
`tools/installer/install.ps1`.
