# GTA San Andreas (PC v1.0) file formats - verified reference

Researched 2026-10-19. Tags used below:
- **[CODE]**: checked against gta-reversed source (github.com/gta-reversed/gta-reversed, a function-by-function re-implementation of gta_sa.exe 1.0 US; each function names its original address). This is the strongest evidence available without the game.
- **[WIKI]**: gtamods.com wiki, raw wikitext fetched.
- **[FILE]**: seen in a real data file fetched from the web.
- **[UNVERIFIED]**: plausible but not confirmed from a primary source.

Reference files saved next to this document (scratchpad):

| File | What it is | Provenance |
|---|---|---|
| `timecyc_sa_reference.dat` | 52-column SA timecyc in the PS2 format, 23 weathers × 8 hours. This is the `timecycp.dat` that ships with SA. | huggingface.co/datasets/Opendsds/samp-data `data/timecycp.dat`. Its header and structure match the wiki description of the console file. |
| `timecyc_pc51col_unverified.dat` | 51-column PC-style timecyc (no DirMult column). | github.com/MrNiceGuy420/GTA-Stuff. Not certain it is unmodified, because line 320 is not the known "255" bug line. |
| `gta_dat_reference.dat` | SA gta.dat **reconstructed** from 2 independent derived copies (see the file header). | SA-MP copy (s0beit repo) plus the SAxVCxLC "base" copy. All IMG/IDE/IPL lines are in both. |
| `ref/info_zon_reference.zon` | Real SA info.zon, 378 zones. | HF samp-data. Every line is identical to the SAxVCxLC copy (an independent source). |
| `ref/map_zon_reference.zon` | Real SA map.zon, 6 zones. | HF samp-data. Identical to the base lines of the SAxVCxLC copy. |
| `ref/water_dat_reference.dat` | A real water.dat from a SA total conversion (230 quads). | HF samp-data. **Not** the vanilla file. |
| `ref/water_dat_single_quad_mapcleaner.dat` | **One-quad water.dat** that covers the whole map, from Steve-M's SA Map Cleaner 0.4b. | files.steve-m.com, CRC-checked zip entry. |
| `ref/main_scm_mapcleaner_compiled.scm` | 597-byte compiled minimal SA main.scm from Map Cleaner. | Same zip. |
| `blank_scm_sa.txt` | (A) verbatim Sanny "stripped main", (B) opcode→GTA3script name table, (C) untested gta3sc translation, (D) verbatim Map Cleaner stripped-main source. | zazmahall.de, gta3sc commands.xml, steve-m.com |
| `ref/countn2_text_ipl_example.ipl`, `ref/countn2_stream0_decompiled_head.txt` | Real text IPL and a decompiled binary stream IPL. | github.com/Jeefrect/GTA-SA-IPLs-Decompiled |

---

## 1. timecyc.dat

**Parser [CODE]** (`CTimeCycle::Initialise`, 0x5BBAC0, TimeCycle.cpp):
- Loop: `for w in 0..22 (NUM_WEATHERS=23): for h in 0..7 (NUM_HOURS=8)`. Weather is the outer loop, so you write all 8 lines of one weather, then the next weather.
- Before parsing, each line is cleaned by `CFileLoader::LoadLine`: every char < 0x20 and every **comma** becomes a space, and leading whitespace is skipped. A line is skipped if its first non-blank char is **`/`** or if it is empty. Only `/` marks a comment; `#` does **not**. The whole `//` comment convention works because only the first character is checked.
- Exactly 23×8 = **184 data lines** are consumed. Anything after that is ignored.
- sscanf format, in order. The integer columns use `%d`, so **write integers there**. A value like `200.0` in a `%d` field shifts the rest of the line.

| # | Field | scanf | Notes |
|---|---|---|---|
|1-3| Amb RGB (static/map ambient) | %d×3 | |
|4-6| Amb_Obj RGB (dynamic ambient) | %d×3 | |
|7-9| Dir RGB | %d×3 | read but **unused** |
|10-12| Sky top RGB | %d×3 | |
|13-15| Sky bottom RGB | %d×3 | |
|16-18| SunCore RGB | %d×3 | |
|19-21| SunCorona RGB | %d×3 | |
|22| SunSz | %f | stored as int8(v*10+0.5) |
|23| SprSz | %f | ×10 |
|24| SprBght | %f | ×10 |
|25| Shdw | %d | |
|26| LightShd | %d | |
|27| PoleShd | %d | |
|28| FarClp | %f | stored as int16 |
|29| FogSt | %f | int16 |
|30| LightOnGround | %f | ×10 |
|31-33| LowClouds RGB | %d×3 | |
|34-36| BottomClouds RGB | %d×3 | |
|37-40| Water R G B A | %f×4 | |
|41-44| PostFx1 **A** R G B | %f×4 | alpha comes FIRST. It is stored as `uint8(A*2)`, so use the PS2 range 0-127 (128 = opaque) |
|45-48| PostFx2 **A** R G B | %f×4 | same |
|49| CloudAlpha | %f | |
|50| IntensityLimit (HighLightMinIntensity) | %d | |
|51| WaterFogAlpha | %d | |
|52| DirMult | %f | ×100 |

Your list of 52 numbers in that order is **confirmed**. The one detail it was missing: PostFx is ARGB with alpha first, and alpha is doubled when the file is read.

**Weather order [CODE]** (eWeatherType.h; the same names appear as section comments in both fetched files):
0 EXTRASUNNY_LA, 1 SUNNY_LA, 2 EXTRASUNNY_SMOG_LA, 3 SUNNY_SMOG_LA, 4 CLOUDY_LA, 5 SUNNY_SF, 6 EXTRASUNNY_SF, 7 CLOUDY_SF, 8 RAINY_SF, 9 FOGGY_SF, 10 SUNNY_VEGAS, 11 EXTRASUNNY_VEGAS, 12 CLOUDY_VEGAS, 13 EXTRASUNNY_COUNTRYSIDE, 14 SUNNY_COUNTRYSIDE, 15 CLOUDY_COUNTRYSIDE, 16 RAINY_COUNTRYSIDE, 17 EXTRASUNNY_DESERT, 18 SUNNY_DESERT, 19 SANDSTORM_DESERT, 20 UNDERWATER, 21 EXTRACOLOURS_1, 22 EXTRACOLOURS_2. EXTRACOLOURS are used for interiors and script "extra colours". Index = color/8 + 21.

**Hours [CODE]**: Midnight, 5AM, 6AM, 7AM, Midday, 7PM, 8PM, 10PM (sample times 0,5,6,7,12,19,20,22).

**PC vs PS2 [WIKI]**: The PC `timecyc.dat` is broken in 3 ways:
- RAINY_COUNTRYSIDE 8PM (line 320) starts with `255` instead of `22 22 22`.
- The DirMult column is missing everywhere, so it reads as 0 (51 columns).
- PostFx alpha is already in the 0-255 range, but the game doubles it anyway.

The wiki concludes that all ports expect the PS2-format file. Caveat: with DirMult = 1.0, vehicles look too bright unless SilentPatch is installed. Recommendation: generate 52 columns, PostFx alpha ≤ 127, and DirMult 1.0 (or 0.0 to mimic vanilla PC).

## 2. info.zon / map.zon

**Loading [CODE]**:
- These are plain IPL files. gta.dat loads them with `IPL DATA\MAP.ZON` and `IPL DATA\INFO.ZON`, placed before the map IPLs.
- `CFileLoader::LoadScene` handles the `zone`…`end` section. `#` comments and empty lines are skipped, and commas become spaces.
- Line parse: `sscanf("%s %d %f %f %f %f %f %f %d %s")` must return 10 values. The fields are:
  `InfoLabel, Type, X1, Y1, Z1, X2, Y2, Z2, Level, TextLabel`
- `CTheZones::CreateZone`: both labels go into `char[8]` and are uppercased, so the **maximum is 7 characters**. Coordinates are stored as **int16** and min/max are sorted automatically.

Values:
- **Type** [CODE eZoneType]:
  - 0 = NAVI. Shown zone name. This is info.zon.
  - 1 = LOCAL_NAVI. Higher priority when zones overlap.
  - 2 = INFO. `CreateZone` treats it as unreachable, so **do not use it**.
  - 3 = MAP. Island zones; this is map.zon.
- **Level** [CODE eLevelName]: 0 = countryside, 1 = Los Santos, 2 = San Fierro, 3 = Las Venturas.
  - [WIKI] In map.zon, level picks the city (cop-car type, etc.); outside any map zone you get countryside.
  - All 378 vanilla info.zon lines use type 0 and level 1. All map.zon lines use type 3 and text `UNUSED`.
- **TextLabel** is the GXT key shown when you enter the zone. A CLEO FXT entry can supply it (see §13).
- Hardcoded zones created at Init [CODE]: `SAN_AND` (NAVI) and `THEMAP` (MAP), both covering -3000..3000, z -2000..2000, level 0.

Real lines [FILE]:
```
zone
SUNMA, 0, -2353.17, 2275.79, 0.0, -2153.17, 2475.79, 200.0, 1, SUNMA
GAN1, 0, 2222.56, -1722.33, -89.0839, 2632.83, -1628.53, 110.916, 1, GAN
end
```
```
zone
Vegas, 3, 685.0, 476.093, -500.0, 3000.0, 3000.0, 500.0, 3, UNUSED
LA01, 3, 480.0, -3000.0, -500.0, 3000.0, -850.0, 500.0, 1, UNUSED
end
```
Note [WIKI]: zones are written into the save file, so you need a new game after changing them.

## 3. water.dat

**Format [FILE + CODE]**:
- First line is `processed`. Every known file has it.
- Then one polygon per line: 4 vertices (quad) or 3 vertices (triangle). Each vertex is 7 floats: `X Y Z FlowX FlowY BigWaves SmallWaves`. After the vertices comes a single integer **flag**.
- So a quad line has 29 tokens and a triangle line has 22.

Details:
- Flags [CODE]: the polygon is created as `CWaterPolygon(isInvisible = (Flags&1)==0, isLimitedDepth = (Flags&2)!=0)`. That means **1 = visible**, and 2 = shallow/limited depth. Vanilla lines end with `1`.
- X and Y are cast to int. Points outside ±3000 are clamped and lose their wave parameters [CODE `AddWaterLevelVertex`].
- Limits [CODE]: 301 quads, 6 triangles, 1021 vertices. Wiki: "307 distinct water planes". Polygons must be axis-aligned rectangles (or right triangles).
- The parser shown in gta-reversed is a NOTSA re-implementation, so the original line parser is not 100% confirmed. Keep the exact vanilla layout.

**Nearly empty works [FILE]**: SA Map Cleaner ships this as its whole water.dat. It is a single quad that gives a full-map water surface at z = 0:
```
processed
-3000.0 -3000.0 0.0 0.0 0.0 0.0 0.0    3000.0 -3000.0 0.0 0.0 0.0 0.0 0.0    -3000.0 3000.0 0.0 0.0 0.0 0.0 0.0    3000.0 3000.0 0.0 0.0 0.0 0.0 0.0  1
```
[UNVERIFIED] A single small quad placed somewhere hidden should also load, because the code just loops over lines. Its behaviour has not been tested in-game.

Example of a real-file line (TC):
`-1500.0 -3000.0 0.0 0.00000 0.00000 0.00000 0.00000    -1500.0 -2700.0 0.0 ...  1`

## 4. gta.dat

- No verbatim Rockstar copy was found online. `gta_dat_reference.dat` is a reconstruction; its IMG/IDE/IPL lines are confirmed by 2 independent copies.
- Original content:
  - `IMG DATA\PATHS\CARREC.IMG`, `IMG DATA\SCRIPT\SCRIPT.IMG`, `IMG MODELS\CUTSCENE.IMG`
  - `SPLASH loadsc2`
  - 54 `IDE` lines (53 under `DATA\MAPS\...`: generic, LA/SF/vegas/country, interior, leveldes, props, veh_mods; plus `IDE DATA\TXDCUT.IDE`) and 52 `IPL` lines in total
  - `IPL DATA\MAP.ZON`, `IPL DATA\INFO.ZON`, then the map IPLs: LA×10, SF×5, vegas×4, country×5, interior×12, `leveldes\levelmap`, `seabed`, `paths..paths5`, `cull`, `tunnels`, `occluSF/veg/LA/int`, `audiozon`
- **No `COLFILE` or `TEXDICTION` lines in gta.dat.** Those live in `DATA\DEFAULT.DAT` [FILE]:
  - `IDE DATA\DEFAULT.IDE`, `IDE DATA\VEHICLES.IDE`, `IDE DATA\PEDS.IDE`
  - `COLFILE 0 MODELS\COLL\WEAPONS.COL`
  - TEXDICTION/MODELFILE lines are commented out
- Hardcoded archives [CODE]: `MODELS\GTA3.IMG`, `MODELS\GTA_INT.IMG` (and player.img).

Loader facts [CODE `CFileLoader::LoadLevel`; `CGame::Initialise` loads DEFAULT.DAT, then gta.dat]:
- Keywords are matched by prefix: `#` comment, `EXIT`, `TEXDICTION`, `IPL`, `IMG`, `COLFILE` (path after skipping the level digit), `MODELFILE`, `HIERFILE`, `IDE`. `SPLASH` is ignored.
- `IMG MODELS\GTA_INT.IMG` is ignored, because it is already hardcoded.
- **The first `IPL` line triggers** `CObjectData::Initialise`, `CVehicleModelInfo::SetupCommonData`, `CStreaming::Init2` (which reads all IMG directories), and `CColStore::LoadAllBoundingBoxes`.
  - So **all `IMG` lines must come before the first `IPL` line.**
  - **gta.dat must contain at least one `IPL` line**, otherwise none of that, nor `LoadAllRemainingIpls`, ever runs.
  - [WIKI] SA supports at most 5 extra IMG entries.

## 5. Binary IPL ("bnry") and removing the map

**Header [CODE tBinaryIplFile, size 0x4C = 76 bytes; WIKI agrees]**, little-endian:
```
0x00 char[4]  "bnry"
0x04 u32 numInst   0x08 u32 numUnk1  0x0C u32 numUnk2  0x10 u32 numUnk3
0x14 u32 numCars   0x18 u32 numUnk4
0x1C u32 offInst   0x20 u32 sizeInst(0)   0x24 offUnk1 0x28 sizeUnk1
0x2C offUnk2 0x30 sizeUnk2   0x34 offUnk3 0x38 sizeUnk3
0x3C u32 offCars   0x40 u32 sizeCars(0)   0x44 offUnk4 0x48 sizeUnk4
```
- Offsets are from the start of the file.
- INST record = 40 bytes: `float px,py,pz, qx,qy,qz,qw; int32 modelId; uint32 flags (low byte = interior, 0x100 unimportant, 0x200 force matrix, 0x400 underwater, 0x800 tunnel, 0x1000 tunnel transition); int32 lodIndex`.
- CARS record = 48 bytes: `float x,y,z,angle; int32 model, col1, col2, forceSpawn, alarm, doorLock, unk1, unk2`.
- **Empty valid file**: `b"bnry" + 6×u32 0 + offInst=0x4C + 11×u32 0` (76 bytes), padded to 2048 inside the IMG. Setting every offset to 0x4C is what R* defaults look like [WIKI "0x4C by default"]. With count 0, the offset is never dereferenced.

How stream IPLs relate to text IPLs [CODE]:
- Every `*.ipl` inside any IMG gets its own IplStore slot (`LoadCdDirectory`).
- When the text IPL `XXX.ipl` from gta.dat is loaded, `SetupRelatedIpls` looks for slots named `XXX_stream*`. It gives them the text IPL's instance array as their LOD table and enables dynamic streaming.
- Stream IPLs with no matching text IPL are processed later by `CIplStore::LoadAllRemainingIpls`. Each is loaded once through `LoadIplBoundingBox`, which **creates every instance**, calls `GetModelInfo()` on it, and resolves `lodIndex` against a **null** LOD array.
- So **removing the text IPL/IDE lines while leaving vanilla `*_streamN.ipl` in gta3.img is expected to crash**:
  - LOD index ≠ -1 → null pointer.
  - Model ID not defined → `LoadObjectInstance` returns null → null dereference.
  - (Strongly implied by the code, not run.)
- The first archive to register a name wins (`if (!info.HasCdPosnAndSize())`), and gta3.img is registered first. So you **cannot shadow** gta3.img entries by putting same-named files in a later IMG. Edit gta3.img itself, or use ModLoader.

How map cleaners do it [FILE: SA Map Cleaner 0.4b2 ReadMe, steve-m.com]:
- "removes all IPL, IMG, SPLASH and the selected IDE entries from your gta.dat … all .ipl files are deleted from the archive". It also deletes the dff/txd/col/ifp used only by the removed IDEs, keeps radar*.txd and nodes*.dat, and deletes gta_int.img.
- It ships a one-quad water.dat and a stripped main.scm.
- Known issue from the ReadMe: with default settings some users got an endless loading loop until they added custom objects. That fits the "need ≥1 IPL line" finding above.
- Forum advice [search summary, UNVERIFIED]: keep the IDE/IPL entries in gta.dat but empty the files, to avoid a flickering loading screen and a sound bug.

## 6. IMG v2 [CODE + WIKI]
- Header: `char[4] "VER2"; u32 numEntries`. Then numEntries × 32-byte entries:
  - `u32 offset (sectors); u16 sizeA (sectors); u16 sizeB (sectors); char[24] name (NUL-terminated, includes extension)`
- Sector = **2048 bytes**. Offsets are absolute, from the start of the archive. Each file is padded to a sector boundary.
- Game behaviour: if `sizeB != 0` it uses sizeB, otherwise sizeA [CODE]. Write the size in sizeA and 0 in sizeB, as stock files do [WIKI].
- Order files the same as the directory, with no gaps (the game may read several files with one read) [WIKI].
- The name must contain a `.` and an extension, and fit in 24 bytes including the NUL. So the base name is ≤ 19 chars for `.dff`/`.txd`.
- Recognised extensions: DFF, TXD, COL, IPL, DAT (nodesN), IFP, RRR, SCM [CODE].
- A DFF whose name has no IDE entry goes to an "extra objects" directory and is ignored for normal models.

## 7. Text IPL inst + quaternion
- Line format [CODE `sscanf("%d %s %d %f %f %f %f %f %f %f %d")`, exactly 11 fields]:
  `ID, ModelName, Interior/Flags, PosX, PosY, PosZ, RotX, RotY, RotZ, RotW, LOD`
  - ModelName is ignored by the game [WIKI].
  - Interior: low byte = area, plus the same flag bits as the binary format.
  - LOD = -1, or an index into the same IPL's inst list.
- Text IPL keywords are case-sensitive, lowercase (`inst`, `end`), and `#` starts a comment.
- **Quaternion is stored conjugated [CODE]**: `LoadObjectInstance` does `q.imag = -q.imag` and then `CMatrix::SetRotate(q)`, which is the standard quaternion→matrix with m_right = first column.
  - When |qx| and |qy| ≤ 0.05, it skips the matrix and sets heading = acos(w)·(qz<0 ? +2 : −2).
  - So for a rotation by angle **a about +Z (counter-clockwise, right-handed)**, write `0, 0, -sin(a/2), cos(a/2)`.
  - In general, write `(-x, -y, -z, w)` of the true rotation quaternion.
  - Real example [FILE countn2_stream0]: `3361, cxref_woodstair, 1024, …, 0.00000, 0.00000, -0.70711, 0.70711, -1` is heading +90°.
- Real text-IPL sample: `ref/countn2_text_ipl_example.ipl`. Vanilla main IPLs often have an empty `inst` section; the instances live in binary `_streamN` files.

## 8. IDE objs + free IDs
- [CODE `LoadObject`] First try `ID, Model, TXD, DrawDist, Flags` (5 fields). That form is accepted only if **DrawDist ≥ 4.0**.
- Otherwise the line is read as the old format `ID, Model, TXD, MeshCount(1|2|3), DrawDist[×MeshCount], Flags`.
- Section is `objs` … `end`. Model and TXD buffers are 24 chars.
- Flags: see gtamods "Item Definition" IDE flags. For example 0x200000 disables backface culling, and 0x8000 means it does not collide with flyers.
- Hard limits [CODE ModelInfo.h / Streaming.h]:
  - Model IDs 0-19999.
  - **Atomic model infos ≤ 14000** in total. Vanilla uses most of them, so a large map on top of vanilla IDEs can hit this.
  - TXD slots 5000, COL slots 255, IPL slots 256.
- Free IDs in unmodded SA [WIKI-like source: libertycity.net article "Unused IDs", same list as gtaforums topic 488464; UNVERIFIED by code]:
  - Large blocks: **18631-19999**, 11682-12799, 15065-15999, 16791-16999, 13891-14382, 14904-15024, 6526-6862, 17475-17499, 18113-18199.
  - The full list is in the source.
  - If you drop the vanilla map IDEs from gta.dat, almost everything outside default/vehicles/peds IDs (0-299 peds, 321-372 weapons, 400-611 vehicles and the default.ide items) becomes free.
  - Hardcoded and script models are matched by name (`MatchAllModelStrings`), so missing ones become -1 [UNVERIFIED impact].

## 9. COL3 and surfaces
- Header [CODE ColHelpers.h + WIKI], little-endian:
  - `char[4] 'COL3'; u32 size (bytes after this field); char[22] name; u16 modelId`
  - `TBounds{ min[3], max[3], center[3], radius }` (40 bytes)
  - `u16 nSpheres, nBoxes, nFaces; u8 nLines; u8 pad; u32 flags; u32 offSpheres, offBoxes, offLines, offVerts, offFaces, offPlanes; u32 nShadowFaces, offShadowVerts, offShadowFaces`
  - That is 88 bytes after the 32-byte file header.
- **Offsets are relative to file position + 4** (that is, counted from the size field).
- Flags: 2 = not empty, 8 = has face groups, 16 = has shadow mesh.
- Sphere = center[3], radius, surface[4]. Box = min[3], max[3], surface[4]. Surface = `material, flag, brightness, light`.
- Vertex = int16×3 /128, so coordinates must stay within ±255.99 of the model origin.
- Face = u16 a, b, c, u8 material, u8 light. Face groups are optional; if used, write `[groups][u32 count]` directly before the faces.
- **DragonFF `gtaLib/col.py`** (github.com/Parik27/DragonFF, fetched) **does write COL2/3**:
  - It writes header_len 104 (+12 for v3) and offsets relative to after the fourcc. That matches the spec.
  - Caveats: no 2-byte alignment padding after the vertex arrays (the wiki calls it optional); it writes 0 for the lines/planes offsets; and for v4 it bumps header_len without writing the extra field (irrelevant for COL3).
  - You must supply bounds, and set `model.version = 3`.
- Surface IDs [CODE eSurfaceType.h]:
  - 0 DEFAULT, 1 TARMAC, 2 TARMAC_FUCKED, 3 TARMAC_REALLYFUCKED, 4 PAVEMENT, 5 PAVEMENT_FUCKED, 6 GRAVEL, 7 FUCKED_CONCRETE, 8 PAINTED_GROUND
  - 9 GRASS_SHORT_LUSH, 10 GRASS_MEDIUM_LUSH, 11 GRASS_LONG_LUSH, 12 GRASS_SHORT_DRY, 13 GRASS_MEDIUM_DRY, 14 GRASS_LONG_DRY, 15 GOLFGRASS_ROUGH, 16 GOLFGRASS_SMOOTH, 17 STEEP_SLIDYGRASS, 18 **STEEP_CLIFF**
  - 19 FLOWERBED, 20 MEADOW, 21 WASTEGROUND, 22 WOODLANDGROUND, 23 VEGETATION, 24 MUD_WET, 25 MUD_DRY, 26 DIRT, 27 DIRTTRACK
  - 28 SAND_DEEP, 29 SAND_MEDIUM, 30 SAND_COMPACT, 31 **SAND_ARID**, 32 SAND_MORE, 33 SAND_BEACH, 34 CONCRETE_BEACH
  - 35 **ROCK_DRY**, 36 ROCK_WET, 37 ROCK_CLIFF, 38 WATER_RIVERBED, 39 WATER_SHALLOW, 40 CORNFIELD, 41 HEDGE
  - 42 WOOD_CRATES, 43 WOOD_SOLID, 44 WOOD_THIN, 45 GLASS, 46 GLASS_WINDOWS_LARGE, 47 GLASS_WINDOWS_SMALL, 48 EMPTY1, 49 EMPTY2
  - 50 GARAGE_DOOR, 51 THICK_METAL_PLATE, 52 SCAFFOLD_POLE, 53 LAMP_POST, 54 METAL_GATE, 55 METAL_CHAIN_FENCE, 56 GIRDER, 57 FIRE_HYDRANT, 58 CONTAINER, 59 NEWS_VENDOR
  - 60 WHEELBASE, 61 CARDBOARDBOX, 62 PED, 63 CAR, 64 CAR_PANEL, 65 CAR_MOVINGCOMPONENT, 66 TRANSPARENT_CLOTH, 67 RUBBER, 68 PLASTIC, 69 TRANSPARENT_STONE
  - 70 WOOD_BENCH, 71 CARPET, 72 FLOORBOARD, 73 STAIRSWOOD
  - 74-157 are P_* procedural-object surfaces (for example 74 P_SAND, 76 P_SAND_ARID, 78 P_SAND_ROCKY, 82 P_GRASS_DRY, 86 P_ROADSIDEDES, 109 P_MOUNTAIN, 113 P_BUSHYDRY, 123 P_DIRTROCKY, 131 P_DESERTROCKS, 143 P_CACTUSDENSE)
  - 158 DOOR, 159 PLASTICBARRIER, 160 PARKGRASS, 161 STAIRSSTONE, 162 STAIRSMETAL, 163 STAIRSCARPET, 164 FLOORMETAL, 165 FLOORCONCRETE
  - 166 BIN_BAG, 167 THIN_METAL_SHEET, 168 METAL_BARREL, 169 PLASTIC_CONE, 170 PLASTIC_DUMPSTER, 171 METAL_DUMPSTER, 172 WOOD_PICKET_FENCE, 173 WOOD_SLATTED_FENCE, 174 WOOD_RANCH_FENCE, 175 UNBREAKABLE_GLASS
  - 176 HAY_BALE, 177 GORE, 178 RAILTRACK

## 10. IDs (open.mp docs, cross-checked against peds.ide / vehicles.ide)
- **Skin ID = ped model ID in peds.ide.**
  - Vanilla peds.ide covers 0-299. **IDs 300-311 are SA-MP-only** and are not in single-player.
  - Several low IDs (1-6, 8, 42, 65, 74, 86, 119, 149, 208, 265-273, 289) are special-character or empty slots in SP. They need `LOAD_SPECIAL_CHARACTER` (023C) or are unused.
- Rural/desert civilians:
  - Men: 32 DWMOLC1, 33 DWMOLC2 (old), 34 DWMYLC1, 128 DNMYLC, 132 DNMOLC1, 133 DNMOLC2 (old), 158 CWMOFR (old farmer), 159 CWMOHB1, 160 CWMOHB2 (old hillbilly), 161 CWMYFR (farmer), 162 CWMYHB1, 200 CWMYHB2, 202 DWMYLC2 (farmer)
  - Women: 31 DWFOLC (old), 129 DNFOLC1, 130 DNFOLC2 (old), 131 DNFYLC, 157 CWFYHB, 196 CWFOFR (old), 197 CWFOHB (old), 198 CWFYFR1, 199 CWFYFR2, 201 DWFYLC2
  - Elderly: 10 BFOST (old woman)
- Farmers: 158, 161, 201, 202.
- Military: **287 army** (COP / PED_TYPE_EMG).
- Police: 280 lapd1, 281 sfpd1, 282 lvpd1, 283 csher (county sheriff), 288 dsher (desert sheriff), 284 lapdm1, 285 swat, 286 fbi.
- Rural thugs:
  - 247 BIKERA, 248 BIKERB and 100 WMYCR are GANG10 bikers.
  - Criminals: 21, 28-30, 47, 143, 144, 146, 181, 183, 184, 223, 250.
  - Hillbillies 159, 160, 162, 200 and 254 (biker drug dealer) also suit.
  - Gangs: 102-104 Ballas, 105-107 Grove, 108-110 Vagos, 114-116 Aztecas, 173-175 Rifa, 111-113 Russian mafia.
- Vehicles: Sandking 495, Patriot 470, Barracks 433, Bobcat 422, Sadler 543 (damaged 605), Walton 478, Yosemite 554, Mesa 500, Sanchez 468, Quad 471, Benson 499, Flatbed 455, Cargobob 548, Maverick 487, Hunter 425, Rhino 432, Police Ranger 599 (model `copcarru`), Burrito 482, Picador 600.
  - Also: Rancher 489, Tractor 531, Combine 532, Enforcer 427, Police Maverick 497, Journey 508, BF Injection 424, Bandito 568, Dune 573, DFT-30 578, Mule 414, Pony 413, Yankee 456, Tampa 549.
  - The vehicles.ide GXT name keys (column 6) are SANDKIN, BARRCKS, RANGER, MESAA, YOSEMIT and so on, which matters for §13.
- Weapons (weapon ID / model ID):
  - knife 4/335, grenade 16/342, Desert Eagle 24/348, Shotgun 25/349, Micro SMG (UZI) 28/352, AK-47 30/355, M4 31/356, Country Rifle 33/357, Sniper 34/358, RPG 35/359
  - Also: MP5 29/353, TEC9 32/372, Heat Seeker 36/360, Molotov 18/344
  - The weapon model must be loaded (REQUEST_MODEL) before GIVE_WEAPON_TO_CHAR.

## 11. Minimal main.scm
- The minimal MAIN thread, from both the zazmahall "total stripped main" and Steve-M's Map Cleaner stripped main, both verbatim in `blank_scm_sa.txt`:
  1. SCRIPT_NAME 'MAIN'
  2. DO_FADE 0 OUT
  3. SET_TOTAL_NUMBER_OF_MISSIONS 0
  4. SET_PROGRESS_TOTAL 0
  5. SET_MAX_WANTED_LEVEL
  6. SET_DEATHARREST_STATE 0
  7. SET_TIME_OF_DAY
  8. **REQUEST_COLLISION x y** (04E4)
  9. **LOAD_SCENE x y z** (03CB)
  10. **CREATE_PLAYER 0 x y z → player** (0053)
  11. GET_PLAYER_GROUP (07AF)
  12. **GET_PLAYER_CHAR** (01F5)
  13. SET_CHAR_HEADING
  14. SET_CAMERA_BEHIND_PLAYER (0373)
  15. clothes 087B (optional)
  16. **BUILD_PLAYER_MODEL** (070D)
  17. SET_INT_STAT 181 4 (cities unlocked)
  18. FORCE_WEATHER_NOW
  19. **SET_AREA_VISIBLE 0** (04BB)
  20. **SET_PLAYER_CONTROL player ON** (01B4)
  21. RELEASE_WEATHER
  22. ADD_HOSPITAL_RESTART / ADD_POLICE_RESTART
  23. **DO_FADE 1000 IN**
  24. WAIT, CLEAR_HELP, DECLARE_MISSION_FLAG
  25. an endless `WAIT`/`GOTO` loop
- Map Cleaner's 597-byte compiled version is saved as a binary reference.
- The 0x0A60/0x0A52 "widget" opcodes in the SA-MP mobile mainv1 are mobile-only. Do not use that file for PC.

## 12. gta3sc
- github.com/thelink2012/gta3sc: C++17, builds with CMake on Linux (GCC/Clang). Configs exist for gta3, gtavc and **gtasa**.
- Command line:
  - SA main: `gta3sc main.sc --config=gtasa [-o main.scm] [--datadir=<dir with gta.dat/default.dat/IDEs>]`
  - **CLEO**: `--cs` outputs a CLEO script and sets `-fcleo`; `--cm` outputs a CLEO custom mission.
- Config files needed: the whole `config/` folder. It is found next to the binary (`/proc/self/exe` dir + `config`), else `~/.local/share/gta3sc/config`, else `/usr/share/gta3sc/config`.
  - It contains `gta3sc.xml` and `gtasa/{commandline.txt, commands.xml, constants.xml, default.xml, alternators.xml, cleo.xml, sounds.xml, eventchoices.xml, taskchoices.xml, taskopcodes.xml, extensions.xml}`.
  - `commandline.txt` sets `-mheader=gtasa -mtyped-text-label -fstreamed-scripts` (it generates script.img), `-fswitch -farrays -fconst`, and so on.
  - Without `--datadir` it uses `default.xml` for model names.
- The language is **GTA3script (Rockstar names, e.g. `CREATE_PLAYER`)**, not Sanny opcode syntax.
- In CLEO scripts, non-default models are an error (test `cleo_disallow_model.sc`).
- The repo's SA tests pass `--guesser`. [UNVERIFIED] Whether a full SA main compiles cleanly without it was not tried.

## 13. CLEO FXT (CLEO 5 source, cleolibrary/CLEO5)
- Folder: `<game>\cleo\cleo_text\*.fxt` (created if missing). Files are loaded on the `GameBegin` callback.
- Format: one entry per line, `KEY<whitespace>text`. Comments start with `#` or `//`. Trailing spaces are trimmed. Keys are uppercased.
- The line buffer is 256 bytes, so keep lines under 255 chars (stream fail stops parsing).
- Key length: CLEO's opcodes read GXT labels as max **7 chars**.
- **CLEO hooks the game's `CText::Get`** and returns an FXT entry first, then falls back to the GXT.
  - So FXT keys work from **main.scm** text commands (PRINT_NOW, PRINT_HELP, etc.).
  - FXT keys also **override existing GXT keys**: vehicle names from the vehicles.ide GXT key, and zone names from the zone text label, since all go through `CText::Get`.
- A duplicate static key from another FXT file is rejected with a warning.

## 14. Radar tiles [CODE Radar.cpp/Radar.h]
- `gRadarTextures[y][x] = FindTxdSlot(format("radar{:02d}", y*12 + x))`, 12×12 tiles, **radar00..radar143**, row-major.
- Tile (x, y) covers world X ∈ [-3000+500x, -3000+500(x+1)] and Y ∈ [3000-500(y+1), 3000-500y]. So **radar00 = top-left at (-3000, +3000)** and radar143 = bottom-right. Each tile is 500 world units.
- The game uses **the first texture in the TXD** (`GetFirstTexture`), so the texture name does not matter. The convention is `radarNN`.
- The TXDs must be in an IMG (slots are found by name), normally gta3.img.
- Tile size: any power of two works. Radar mods ship 256-1024 tiles, DXT1 or 32-bit. Vanilla 128×128 is [UNVERIFIED].
- A Python RW TXD writer for radar tiles exists in github.com/Ghady983/GTA-SA-Radar-Map-Mod-Maker (`build_radar_txd`, D3D9 platform, RW version 0x1803FFFF).

## 15. ModLoader (thelink2012/modloader docs + source)
- **main.scm**: handled by std.scm. Drop it in a mod folder; streamed scripts go in `script.img/`.
- **gta.dat, water.dat**: Override + Merge.
  - **If exactly one mod supplies the file (and no readme matches), it is a plain override**: the whole file is replaced, so removed lines stay removed (`GetMergedData`: `count == 1 && readme_data.empty()` → override).
  - With ≥2 copies, or readme lines (gta.dat supports readme parsing), they are merged. The merged gta.dat is **re-ordered by section**: IMG, CDIMAGE, TEXDICTION, MODELFILE, IDE, COLFILE, MAPZONE, IPL, HIERFILE, SPLASH, EXIT.
- **timecyc.dat**: override only.
- **info.zon / map.zon / *.ipl**: override (the highest-priority copy).
- **Caveat**: "IDE and IPL that doesn't have a matching gta.dat are NOT loaded". New IDE/IPL files still need gta.dat lines.
- **IMG contents** (dff, txd, col, binary ipl, nodes*.dat, ifp, rrr, scm): std.stream. Drop the loose files, or put them in a folder named `gta3.img/`.
  - Nodes files must be inside a `*.img` folder; new clothes go in `player.img/`.
  - std.stream also detours IMG/COLFILE/TEXDICTION/MODELFILE/HIERFILE from gta.dat and "fixes the COLFILE command in GTA SA not working properly".
  - Replacing radarNN.txd and `*_streamN.ipl` this way is the supported path. This is also the way around the "first IMG wins" rule from §5.
- [UNVERIFIED] Whether ModLoader can make a stream IPL in gta3.img disappear (as opposed to replacing it). The safe approach is to drop empty 76-byte `bnry` files under the same names.

---
### Sources
- gta-reversed: https://github.com/gta-reversed/gta-reversed (TimeCycle.cpp, FileLoader.cpp, IplStore.cpp, tBinaryIplFile.h, FileObjectInstance.h, Streaming.cpp, Directory.h, WaterLevel.cpp/.h, TheZones.cpp, Zone.h, eSurfaceType.h, eWeatherType.h, Radar.cpp/.h, ColHelpers.h, Matrix.cpp, ModelInfo.h)
- gtamods wiki: Time_cycle, Gta.dat, ZONE, Item_Placement, INST, OBJS, Item_Definition, IMG_archive, Collision_File, Water.dat
- https://huggingface.co/datasets/Opendsds/samp-data (data/timecycp.dat, info.zon, map.zon, water.dat, gta.dat, default.dat)
- https://github.com/goodidea82/SAxVCxLC (base/data/*), https://github.com/ananay/mod-s0beit-sa (bin/data/gta.two)
- https://github.com/MrNiceGuy420/GTA-Stuff (timecyc.dat)
- https://github.com/Jeefrect/GTA-SA-IPLs-Decompiled, https://gist.github.com/blewert/839de8228f3dd7a21603
- https://github.com/Parik27/DragonFF (gtaLib/col.py)
- https://github.com/openmultiplayer/web (docs/scripting/resources/skins.md, vehicleid.md, weaponids.md)
- https://zazmahall.de/ZAZGTASANATORIUM/mainscm-coding-Tut2_eng.htm ; http://steve-m.com/downloads/tools/mapcleaner/ (mapcleaner-0.4b.zip / 0.4b2 ReadMe)
- https://github.com/thelink2012/gta3sc (README, src/main.cpp, src/system.cpp, config/gtasa/*)
- https://github.com/cleolibrary/CLEO5 (cleo_plugins/Text/CTextManager.cpp, Text.cpp)
- https://github.com/thelink2012/modloader (doc/plugins/gta3/std.data.md, std.stream.md, std.scm.md; std.data/data.hpp, data_traits/gta.dat.cpp)
- https://libertycity.net/articles/gta-san-andreas/9976-unused-ids-in-gta-san-andreas.html
- https://github.com/Ghady983/GTA-SA-Radar-Map-Mod-Maker
