# GTA San Andreas – Afghanistan: "The Valley"

A total-conversion mod for **classic GTA San Andreas (PC)**. It replaces the San Andreas map with a
36 × 36 km slice of real Afghan terrain (the Shomali plain and the Ghorband valley north of Kabul,
scaled 1:6). It adds a new 8-mission story, original Afghan-style music, new zone names, a
dry-highland weather cycle, character outfits and a new radar map.

![Map](docs/map.png)

> **Status: version 1.0, generated and checked on Linux, but not yet play-tested inside the game.**
> Every file was generated from scratch and checked: models and textures were read back with an
> independent parser and rendered (see `docs/preview_*.png`), and scripts compile with gta3sc.
> But nobody has started the game with it yet. Please read *Known issues* before you play, and
> report what happens.

## What's in it

| Area | Content |
|---|---|
| **Map** | Real elevation data (SRTM), 576 terrain tiles + 144 LOD tiles, dirt roads routed through the mountains with switchbacks, irrigated fields, poplar-lined roads, orchards |
| **Places** | Qala-e-Naw (town, bazaar, mosque, police station, your safehouse), Camp Sangar (army base with runway, hangars, helipads), villages Deh-e-Bagh, Dara-e-Sabz, Sar-e-Kotal, Shahrak, Darwaza Checkpoint, Sang-e-Siah bandit camp, Qala-e-Kohna mountain fortress, Burj-e-Khak ruins |
| **Buildings** | 28 new models: mud-brick *qala* compounds with towers, houses, mosques with turquoise domes and minarets, bazaar stalls, watchtowers, HESCO walls, guard towers, tents, hangars, fortress, ruins, trees |
| **Story** | 8 missions: *Homecoming, Market Day, Medicine for Shahrak, Darwaza, Aid Convoy, Black Stone, Eagle's Nest, Qala-e-Kohna* – see [docs/STORY.md](docs/STORY.md) |
| **People** | Villagers (men, women, elders), farmers, police at the checkpoint, soldiers at the base, Black Stone bandits – all reuse SA's rural/desert peds |
| **Character styles** | Wardrobe in your courtyard: villager, farmer, shepherd, city clothes (CJ), police volunteer (after mission 4), army fatigues (after mission 5) |
| **Music** | 6 original instrumental tracks (rubab, dutar, nay flute, tabla, dhol, harmonium drone in Afghan modes and rhythms such as *mughulchi* 7/8 and *attan*), played by an in-car radio |
| **Atmosphere** | New timecyc: deep-blue high-altitude sky, dusty horizon, golden dawn/dusk, longer view distance; weather cycle with clear days, haze, rare dust storms and rain |
| **Vehicles** | Parked pickups, sedans, motorbikes and tractors in towns; police pickups at the checkpoint; army jeeps, trucks, Cargobob and Dodo at the base; Hunter + Rhino unlocked at the end |
| **UI** | New zone names, radar map, renamed vehicles (Police Pickup, Army Jeep, Army Truck, Desert Pickup) |

## Requirements

1. **GTA San Andreas PC, classic version 1.0 US.** The *Definitive Edition* will **not** work.
   Steam or Rockstar Launcher copies must be downgraded to 1.0 first, with a downgrader tool.
2. **ModLoader** – <https://github.com/thelink2012/modloader/releases>
3. **CLEO 5** (or CLEO 4.4) – <https://github.com/cleolibrary/CLEO5/releases>. Mission text and the radio need it.
4. **SilentPatch** (strongly recommended) – <https://github.com/CookiePLMonster/SilentPatch/releases>
5. An ASI loader. CLEO 5 and SilentPatch ship one; otherwise use Ultimate ASI Loader.

## Install

1. Install the requirements above. Your game folder should now contain a `modloader` folder and `cleo.asi`.
2. Unzip this mod anywhere and run **`INSTALL.bat`**, then select your GTA San Andreas folder.
   - On Linux/Wine or macOS, run `python3 install.py "/path/to/GTA San Andreas"` instead.
3. Start the game and choose **New Game**. Old saves will not work, and saving in this mod can overwrite save slots, so back up
   `Documents\GTA San Andreas User Files` if you care about them.

The installer **never modifies or overwrites an original game file**. It only adds:
- `modloader\afghanistan\` – models, textures, radar, `gta.dat`, `timecyc.dat`, `water.dat`, `main.scm`
- `data\maps\afghan\` – map definitions, placements, zones, collision
- `CLEO\afghan_radio.cs`, `CLEO\afghan_radio\*.mp3`, `CLEO\CLEO_TEXT\afghan.fxt`

It also reads your `gta3.img` and writes empty stand-ins for the vanilla map's streamed IPL files
(into `modloader\afghanistan\emptied\`). Without them the game would try to place San Andreas
buildings that no longer exist.

**Uninstall:** run `UNINSTALL.bat` (or `python3 install.py --uninstall <folder>`).

## How to play

- You start in your **safehouse courtyard** in Qala-e-Naw.
- **Missions:** walk into the marker of the contact shown on the radar:
  - **R** – Kaka Rahim at the bazaar
  - **police badge** – Captain Nasir
  - **flag** – Major Hamid at Camp Sangar
- **Save:** the save-disk marker in your courtyard.
- **Wardrobe:** the T-shirt marker in your courtyard. Each visit switches to the next outfit.
- **Radio:** in any vehicle the Afghan radio plays automatically. Press **N** for the next track and **M** to turn it on/off.
- Wanted levels, random traffic and random pedestrians are switched off, because the map has no road-node network for AI drivers.
  Villagers, soldiers and police are spawned by script around towns instead.

## Customising

- **Your own music:** replace the mp3 files in `CLEO\afghan_radio\` and keep the same file names.
- **Better character or vehicle models:** use any SA-compatible `.dff`/`.txd`. For example, drop a Toyota Hilux mod named
  `sandking.dff/.txd` or a new ped skin named `dnmylc.dff/.txd` into a new folder under `modloader\`. ModLoader then
  replaces those models everywhere: villagers, the wardrobe and missions.
- **Change the world:** everything is generated by `tools/build.py`: terrain region, towns, roads, buildings, textures,
  missions and music. See [docs/TECHNICAL.md](docs/TECHNICAL.md).

## Known issues / limitations

- **Not play-tested yet** (see the status note above). If the game crashes on load, see the troubleshooting list in
  [docs/TECHNICAL.md](docs/TECHNICAL.md#troubleshooting).
- No interiors. Houses are solid.
- No AI traffic or random pedestrians; only script-spawned people near settlements.
- Story characters reuse existing SA ped models (no new 3D people), and outfits switch the whole player model.
- Terrain is low-poly by modern standards (about 7.8 m per vertex), and textures are procedural.
- Speech is subtitles only. There is no voice acting.

## Credits & licences

- Terrain: AWS Terrain Tiles (Mapzen / Tilezen joerd), built from SRTM and other public datasets – see
  <https://github.com/tilezen/joerd/blob/master/docs/attribution.md>.
- Tools used at build time (not shipped in the mod): **gta3sc** script compiler by thelink2012 (MIT), and
  **DragonFF** collision writer and file parsers by Parik27 and contributors (GPL-3.0).
- File format references: gtamods.com wiki and the gta-reversed project.
- All models, textures, scripts, text and music in this mod were generated by the build scripts in this repository.
- GTA San Andreas is © Rockstar Games. This mod contains no Rockstar files, so you need your own copy of the game. It is a
  non-commercial fan project. All characters, groups and events are fictional.
