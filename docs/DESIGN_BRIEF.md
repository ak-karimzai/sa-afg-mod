# Design brief: act two and what the engine can do

Act two ("The Snow Road", missions 13-24) was built from a longer ten-mission brief. This page records which of its
ideas went into the game, how they were adapted to what classic GTA San Andreas can actually do by script on this
rural map, and which ones cannot be done without new engine features or assets.

## Adapted into missions

| Brief | In the mod | How it was adapted |
|---|---|---|
| The Overweight Payload (jingle truck, brakes fail, min-speed, tipping, ramp jump over a closed gate) | 19 **Jingle Truck** | A heavy truck (`SET_CAR_HEAVY`) down the fortress road at night. The engine cannot disable brakes, so after the "brakes fail" point a momentum bar drains whenever the truck is slow and the mission fails when it empties; a rollover (`IS_CAR_UPSIDEDOWN`) fails too. Pursuit pickups ram you; the heavy truck swipes them off. At the bottom a stunt ramp object (`1660`) is placed on the road in front of a two-pickup block. There are no dunes on this map. |
| The Buzkashi Blitz (motorised buzkashi, sandbag, rival riders, gates lock) | 14 **Buzkashi** | Five AI riders on Sanchez bikes ram the player; the sack is a pickup that is carried on the bike, drops where you are knocked off, and is taken by any rider that reaches it; both sides score into one circle. Gates/flooding is not possible; instead Haji Gul's men attack when you win. |
| Echoes in the Gorge (tremor meter, silent weapons, avalanche, flare) | 20 **Echoes in the Gorge** | A noise meter driven by loud shots and engines (knife and silenced pistol are free). Half full wakes the camp, full triggers a "rockfall" on the player. Then charges above the road, a safe point, and a scripted rockfall (explosion chain) that destroys the column. No snow, snowmobile, crossbow or wingsuit exist in SA. |
| The Flying Carpet (sky-crane, magnet, container, swinging) | 21 **Flying Carpet** | The Leviathan is SA's magnet helicopter. It lifts the courier's car (not a container) out of the fortress courtyard; the script detects the car hanging under the helicopter and counts the drop at the motor pool. Enemy helicopters cannot be swatted; the danger is RPG fire from the walls. |
| Mirage Express (sandstorm, zero visibility) | 17 **Dust Storm** | Sandstorm weather and a crash site found by radar. There are no railways on this map, so no train. |
| Blue Mosque Mirage / social stealth | 18 **Judas** (partly) | A night meeting that turns into a trap; the disguise system of the brief is not possible (SA has no crowd-blending or suspicion model). |
| Crown of the Hindu Kush (three-tier assault, escape vehicle) | 24 **Haji Gul** | Siege of the base, then the Hunter against an escaping helicopter. |

## Not possible in GTA San Andreas by script

- **Kite flying / kite duels** (Kite Runner Recon): there is no kite object, physics or camera.
- **Trains and minecarts** (Mirage Express, Lapis Lazuli Heist): the map has no rails and SA's train system needs them.
- **Avalanche, snow walls, wingsuit, parachute-from-cable-car**: no avalanche effect, no wingsuit; parachutes exist but
  there is no cable car.
- **City traffic hacking, bollards, cattle trucks, a ten-floor glass tower, parade floats, fireworks**: this map is a
  rural valley with no city, no traffic system (no road nodes) and no such props.
- **Dam spillway, winch-climbing a wall, amphibious cars**: no such vehicles or physics.

## Visual style notes (from the second brief)

- **Palette**: the generated `timecyc.dat` now aims at "golden dust and lapis blue": a deeper lapis sky at midday,
  dusty gold horizons, indigo shadows and an amber afterglow after sunset (`tools/afg/gamedata.py`, `write_timecyc`).
- **Jingle art on vehicles, tandoor smoke, pigeons, kites in the sky**: these need new textures, models or particle
  effects. They are not in this version.
- **Architecture**: the map has one region style (mud-brick compounds, a fortress, ruins, an army base). River-stone
  cliff villages and a capital with glass towers would be a new map.
- The image-generation prompts in the brief are for external tools (Midjourney, DALL-E); nothing in the mod uses them.
