# Changelog

## Unreleased

- **Flying Carpet** redesigned: the Leviathan's magnet could not pick the sedan out of the fortress courtyard in
  game (the courtyard is too narrow to get low enough, and the script never attached the winch in the first
  place), so the mission is now a raid. Fly the Leviathan to Qala-e-Kohna, land outside the walls, take the
  sedan from the courtyard on foot and drive it down the mountain road to the motor pool. Same garrison, same
  drop point, new dialogue.

## 1.3.0 - 2026-10-21 - act two: "The Snow Road" (12 new missions)

Twenty-four missions instead of twelve. After the Black Stone falls, a second act with a new enemy (Haji Gul
Zarghun, the opium baron who paid for Siahdast), a traitor inside the valley, and a different kind of gameplay in
every mission. See [docs/STORY.md](docs/STORY.md) for the mission table.

- **Ashes**: night raid on the bazaar (pickups, motorbikes, foot soldiers), then a chase down the Deh-e-Bagh road.
- **Buzkashi**: motorised buzkashi against five riders. Carry the sack on your bike into the circle; drop it when
  you are rammed off; the riders carry it to their circle. Two score counters, first to three, then a shoot-out.
- **Courier**: steal the money car without wrecking it (damage bar), the driver flees, pickups chase you home.
- **The Wedding**: sniper overwatch over a wedding party in Dara-e-Sabz; three groups on foot, then a technical.
- **Dust Storm**: sandstorm weather, a plane wreck on the plain, a wounded pilot on a five-minute timer.
- **Judas**: the trap at Sar-e-Kotal. Captain Nasir is revealed as Haji Gul's man and leaves the station for good
  (the main script stops spawning him after this mission; the police contact marker is gone).
- **Jingle Truck**: steal an overloaded truck and bring it down the mountain road with failed brakes (momentum bar,
  rollover = fail), heavy-truck sideswipes against chasers, and a stunt ramp over a roadblock at the bottom.
- **Echoes in the Gorge**: silent raid with knife and silenced pistol. A noise meter fills with loud shots and
  engines; half fills wakes the camp, full brings the gorge down on you. Plant charges, get clear, rockfall.
- **Flying Carpet**: the Leviathan's magnet. Lift the ledger car out of the fortress courtyard under RPG fire and
  set it down at the motor pool; the script tracks the car hanging under the helicopter.
- **Iron Harvest**: a Rhino mission up the valley through three roadblocks to the camp and its ammunition dump.
- **Traitor's Road**: chase Nasir's convoy up the mountain road; an RPG roadblock turns across the road half way.
- **Haji Gul** (finale): three-wave siege at the base entry with six soldiers, then the Hunter against Haji Gul's
  escaping helicopter (flies waypoints north with HELI_GOTO_COORDS). Epilogue with Rahim.
- New global route array `qb` (town road Qala-e-Naw -> Deh-e-Bagh); contact logic rewritten around a
  `contact_kind` variable (the compiler allows at most 8 conditions per IF).
- Timecyc: "golden dust and lapis blue" palette: deeper lapis sky at midday, indigo shadows and an amber afterglow
  in the evening.
- docs/DESIGN_BRIEF.md records the longer mission brief these were adapted from and what the engine cannot do.

Saves made with 1.2.0 do not work with this main.scm (every script rebuild moves the saved instruction pointer):
start a New Game.

## 1.2.0 - 2026-10-20 - action missions, police uniform

Twelve missions instead of eight. Four new action missions are woven into the story, the raids got harder, and the
finale is a chase followed by a helicopter hunt.

- **Burj-e-Khak** (new, after Darwaza): the Shahrak doctor has been kidnapped. Night raid on the ruins east of the
  base: eight guards on patrol, two marksmen with sniper rifles on the perimeter. Free the doctor (he follows you),
  get him into a vehicle and drive him home while Black Stone pickups waiting along the road ram you and shoot from
  the passenger seat. Fails if the doctor dies.
- **Fuel and Fire** (new, after Aid Convoy): the Black Stone fuel dump in occupied Sar-e-Kotal. Approach at dusk, plant
  charges on three fuel stacks (stand in the marker for three seconds; the stacks are real explosive barrels, you can
  also shoot them), the alarm brings a pickup and more men up the road, a 30-second detonation timer, then get clear
  or finish them.
- **The Pass** (new, after Black Stone): a convoy of three trucks and two escort pickups actually drives up the
  mountain road at night. From the ambush position above the switchbacks you must destroy all three trucks with the
  RPG and sniper rifle before they get past; the escort dismounts and hunts you once the first truck burns.
- **Vulture** (new finale): the Black Stone remnants flee down the Ghorband valley in five pickups. Take the Hunter and
  destroy them before any of them crosses Darwaza. Then the sunrise epilogue with Rahim.
- **Qala-e-Kohna**: 14 defenders (two RPG teams), you get an RPG at the gate, and when his guard is nearly gone
  Siahdast runs for a pickup and flees down the mountain road with a gunner: take the squad's jeep and stop him before
  he reaches the valley.
- **Black Stone**: a reinforcement pickup comes up from the valley when the camp is alerted (12 bandits).
- **Darwaza**: the last wave has eight attackers.
- **Police uniform**: the policemen (Captain Nasir, the Darwaza checkpoint, the "police volunteer" outfit) now wear
  Afghan dress in the Taliban style: kameez and shalwar, a waistcoat, a snug turban, a beard. The textures are derived
  from your own game's `gta3.img` at build time (`tools/build.py --game <folder>`), so no Rockstar texture is stored
  in the repository or shipped unchanged.
- Vehicles now drive without road nodes: convoys follow waypoint routes along the generated roads in straight
  segments (`TASK_CAR_DRIVE_TO_COORD` in straight-line mode); chasers use the node-free ram-player behaviour.
- New developer tool `tools/dev/pedview.py` renders a ped model with any texture (used to design the uniform).


## 1.1.0 - 2026-10-20 - "real missions"

The eight missions were rebuilt to play like the game's own: every one now opens with a cutscene (fixed cameras,
speaking characters, letterbox), has staged objectives with checkpoints and on-screen counters, mid-mission events,
proper fail states and a closing scene.

- Story characters are now **always in the world**: Kaka Rahim at his bazaar stall, Captain Nasir in front of the
  police station, Major Hamid at Camp Sangar HQ. They cannot be hurt and are the people you talk to in the cutscenes.
- **Homecoming**: a Black Stone "road tax" post half way to Deh-e-Bagh (they shout, then shoot if you do not stop),
  a damage bar for Rahim's pickup, the baker meets you at the village.
- **Market Day**: the extortionists are shaking down a stall keeper (hands up, gun drawn) while you talk; bystanders
  flee when the shooting starts; the bandits drop cash; closing scene with Rahim.
- **Medicine for Shahrak**: 3-minute timer with a "hurry" warning, the doctor runs out to the pickup, Captain Nasir
  phones you afterwards to set up the next mission.
- **Darwaza**: briefing at the station, three policemen you must keep alive (mission fails if they all die), waves
  2 and 3 arrive in pickups with men on the slopes, an RPG gunner in the last wave, the sergeant talks to you.
- **Aid Convoy**: an army jeep with two soldiers (Karim and Jawed) follows the truck; at the ambush an RPG hits
  the road, a bandit pickup blocks it, the soldiers dismount and fight; aid-truck damage bar; the elder of
  Dara-e-Sabz receives the aid.
- **Black Stone**: raid at first light; guards walk their rounds until they notice you (distance, gunfire or being
  hit); afterwards find the stolen aid (briefcase pickup) to finish.
- **Eagle's Nest**: a "photos" progress bar fills while you circle the fortress under RPG fire; getting out of the
  helicopter sends you back to it; debrief with Hamid.
- **Qala-e-Kohna**: lead the squad to the gate, the gate is blown, storm the fortress; epilogue at sunrise in your
  courtyard with Rahim.
- Villagers now stand and chat in groups instead of only wandering.
- New developer tool `tools/dev/quick_scripts.py`: rebuilds only text + scripts in about 8 s and can install them
  into a game folder.


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
