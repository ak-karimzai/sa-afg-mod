// =============================================================================
// GTA San Andreas - Afghanistan: "The Valley"
// Main script. Compiled with gta3sc (see tools/build.py). Point tokens (at-sign name at-sign) are
// replaced with map coordinates from the generator before compiling.
// =============================================================================

//@GENERATED_VARS@

VAR_INT mission_progress mission_shown contact_blip safe_blip wardrobe_blip
VAR_INT save_armed wardrobe_armed style style_model style_count style_ok
VAR_INT car_gen pickup hunter_given
VAR_INT mission_timer mission_counter mission_bar  // on-screen timer / counter / bar used by the missions
VAR_FLOAT contact_x contact_y contact_z player_x player_y player_z
// story characters, always in the world when you are near: 0 Kaka Rahim (bazaar), 1 Captain Nasir (police
// station), 2 Major Hamid (Camp Sangar HQ). Missions use these handles for their cutscenes.
VAR_INT contact_ped[3] contact_model[3] npc_i
VAR_FLOAT npc_x[3] npc_y[3] npc_z[3] npc_h[3] npc_d
// ambient population (filled by //@GENERATED_INIT@)
VAR_FLOAT amb_site_x[8] amb_site_y[8] amb_spot_x[96] amb_spot_y[96] amb_spot_z[96]
VAR_INT amb_site_kind[8] amb_site_count amb_ped[10] amb_active amb_i amb_j amb_k amb_model amb_timer
VAR_INT weather_timer weather_pick
VAR_FLOAT amb_d amb_tx amb_ty amb_tz

SCRIPT_NAME MAIN
SET_DEATHARREST_STATE OFF
DO_FADE 0 FADE_OUT
SET_TOTAL_NUMBER_OF_MISSIONS 0  // gta3sc fills in the real count
SET_PROGRESS_TOTAL 0
SET_MAX_WANTED_LEVEL 0
SET_TIME_OF_DAY 8 30
SET_INT_STAT 181 4  // all "islands" unlocked: no wanted level for crossing old city borders

REQUEST_COLLISION @safehouse.x@ @safehouse.y@
LOAD_SCENE @safehouse@
CREATE_PLAYER 0 @safehouse@ player
GET_PLAYER_GROUP player player_group
GET_PLAYER_CHAR player scplayer
SET_CHAR_HEADING scplayer 180.0
SET_CAMERA_BEHIND_PLAYER
GIVE_PLAYER_CLOTHES_OUTSIDE_SHOP player VEST VEST 0
GIVE_PLAYER_CLOTHES_OUTSIDE_SHOP player JEANSDENIM JEANS 2
GIVE_PLAYER_CLOTHES_OUTSIDE_SHOP player SNEAKERBINCBLK SNEAKER 3
BUILD_PLAYER_MODEL player
FORCE_WEATHER_NOW WEATHER_EXTRASUNNY_DESERT
SET_AREA_VISIBLE 0
SET_PLAYER_CONTROL player ON
ADD_HOSPITAL_RESTART @hospital@ @hospital.h@ 0
ADD_POLICE_RESTART @police@ @police.h@ 0

// no vanilla traffic, trains, planes, police or gang wars: this map has no SA road network
SET_PED_DENSITY_MULTIPLIER 0.0
SET_CAR_DENSITY_MULTIPLIER 0.0
SWITCH_RANDOM_TRAINS OFF
SWITCH_AMBIENT_PLANES OFF
SWITCH_EMERGENCY_SERVICES OFF
SET_GANG_WARS_ACTIVE OFF

// relationships: MISSION1 = Black Stone bandits, MISSION2 = police / army / villagers who fight back
SET_RELATIONSHIP ACQUAINTANCE_TYPE_PED_HATE PEDTYPE_MISSION1 PEDTYPE_PLAYER1
SET_RELATIONSHIP ACQUAINTANCE_TYPE_PED_HATE PEDTYPE_MISSION1 PEDTYPE_MISSION2
SET_RELATIONSHIP ACQUAINTANCE_TYPE_PED_HATE PEDTYPE_MISSION2 PEDTYPE_MISSION1
SET_RELATIONSHIP ACQUAINTANCE_TYPE_PED_RESPECT PEDTYPE_MISSION2 PEDTYPE_PLAYER1

ADD_SCORE player 250
mission_progress = 0
mission_shown = -1
save_armed = 1
wardrobe_armed = 1
style = 1
style_model = 128
hunter_given = 0
amb_active = -1

// story characters
contact_model[0] = 158          // Kaka Rahim: old farmer
npc_x[0] = @bazaar_uncle.x@
npc_y[0] = @bazaar_uncle.y@
npc_z[0] = @bazaar_uncle.z@
npc_h[0] = @bazaar_uncle.h@
contact_model[1] = 288          // Captain Nasir: desert sheriff
npc_x[1] = @police_nasir.x@
npc_y[1] = @police_nasir.y@
npc_z[1] = @police_nasir.z@
npc_h[1] = @police_nasir.h@
contact_model[2] = 287          // Major Hamid: army
npc_x[2] = @base_major.x@
npc_y[2] = @base_major.y@
npc_z[2] = @base_major.z@
npc_h[2] = @base_major.h@
npc_i = 0
WHILE npc_i < 3
    contact_ped[npc_i] = -1
    npc_i += 1
ENDWHILE

//@GENERATED_INIT@

ADD_SPRITE_BLIP_FOR_CONTACT_POINT @save@ RADAR_SPRITE_SAVEHOUSE safe_blip
ADD_SPRITE_BLIP_FOR_CONTACT_POINT @wardrobe@ RADAR_SPRITE_TSHIRT wardrobe_blip

WAIT 0
GOSUB apply_style
DECLARE_MISSION_FLAG flag_player_on_mission
DO_FADE 1500 FADE_IN
PRINT_BIG AFG_TTL 4000 2
WAIT 2000
PRINT_NOW AFG_INT 6000 1
WAIT 6000
PRINT_NOW AFG_I2 5000 1
PRINT_HELP AFG_CON

LAUNCH_MISSION ambient.sc

// =============================================================================
main_loop:
WAIT 250
IF IS_PLAYER_PLAYING player
    // the exe can re-enable these after a load / death
    SET_PED_DENSITY_MULTIPLIER 0.0
    SET_CAR_DENSITY_MULTIPLIER 0.0

    IF flag_player_on_mission = 0
        GOSUB update_npcs
        GOSUB update_contact
        GOSUB check_contact
        GOSUB check_save
        GOSUB check_wardrobe
    ENDIF
    // keep the chosen outfit (some ped models get reset after loading a save)
    IF NOT IS_CHAR_MODEL scplayer style_model
    AND NOT style_model = 0
        GOSUB apply_style
    ENDIF
    IF mission_progress = 8
    AND hunter_given = 0
        REQUEST_MODEL HUNTER
        LOAD_ALL_MODELS_NOW
        CREATE_CAR_GENERATOR @hunter@ @hunter.h@ HUNTER -1 -1 1 0 0 0 10000 car_gen
        SWITCH_CAR_GENERATOR car_gen 101
        CREATE_CAR_GENERATOR @rhino@ @rhino.h@ RHINO -1 -1 1 0 0 0 10000 car_gen
        SWITCH_CAR_GENERATOR car_gen 101
        hunter_given = 1
        PRINT_HELP AFG_END
    ENDIF
ENDIF
GOTO main_loop

// ----------------------------------------------------------------- story characters
// Rahim, Nasir and Hamid stand at their places whenever you are within 140 units; they are removed
// again when you are far away. They cannot be hurt and never run from gunfire.
update_npcs:
GET_CHAR_COORDINATES scplayer player_x player_y player_z
npc_i = 0
WHILE npc_i < 3
    GET_DISTANCE_BETWEEN_COORDS_2D player_x player_y npc_x[npc_i] npc_y[npc_i] npc_d
    IF contact_ped[npc_i] = -1
        IF npc_d < 140.0
            GOSUB npc_spawn
        ENDIF
    ELSE
        IF NOT DOES_CHAR_EXIST contact_ped[npc_i]
            contact_ped[npc_i] = -1
        ELSE
            IF npc_d > 220.0
                DELETE_CHAR contact_ped[npc_i]
                contact_ped[npc_i] = -1
            ENDIF
        ENDIF
    ENDIF
    npc_i += 1
ENDWHILE
RETURN

npc_spawn:
REQUEST_MODEL contact_model[npc_i]
LOAD_ALL_MODELS_NOW
CREATE_CHAR PEDTYPE_CIVMALE contact_model[npc_i] npc_x[npc_i] npc_y[npc_i] npc_z[npc_i] contact_ped[npc_i]
SET_CHAR_HEADING contact_ped[npc_i] npc_h[npc_i]
SET_CHAR_PROOFS contact_ped[npc_i] TRUE TRUE TRUE TRUE TRUE
SET_CHAR_NEVER_TARGETTED contact_ped[npc_i] TRUE
SET_CHAR_STAY_IN_SAME_PLACE contact_ped[npc_i] TRUE
MARK_MODEL_AS_NO_LONGER_NEEDED contact_model[npc_i]
RETURN

// ----------------------------------------------------------------- mission contact marker
update_contact:
IF mission_shown = mission_progress
    RETURN
ENDIF
IF NOT mission_shown = -1
    REMOVE_BLIP contact_blip
ENDIF
mission_shown = mission_progress
IF mission_progress = 0
OR mission_progress = 1
OR mission_progress = 2
    contact_x = @bazaar.x@
    contact_y = @bazaar.y@
    contact_z = @bazaar.z@
    ADD_SPRITE_BLIP_FOR_CONTACT_POINT contact_x contact_y contact_z RADAR_SPRITE_RYDER contact_blip
ENDIF
IF mission_progress = 3
OR mission_progress = 5
    contact_x = @police.x@
    contact_y = @police.y@
    contact_z = @police.z@
    ADD_SPRITE_BLIP_FOR_CONTACT_POINT contact_x contact_y contact_z RADAR_SPRITE_POLICE contact_blip
ENDIF
IF mission_progress = 4
OR mission_progress = 6
OR mission_progress = 7
    contact_x = @base_hq.x@
    contact_y = @base_hq.y@
    contact_z = @base_hq.z@
    ADD_SPRITE_BLIP_FOR_CONTACT_POINT contact_x contact_y contact_z RADAR_SPRITE_FLAG contact_blip
ENDIF
IF mission_progress = 8
    mission_shown = 8
    RETURN
ENDIF
RETURN

check_contact:
IF mission_progress > 7
    RETURN
ENDIF
IF NOT LOCATE_CHAR_ON_FOOT_3D scplayer contact_x contact_y contact_z 1.5 1.5 2.0 FALSE
    RETURN
ENDIF
REMOVE_BLIP contact_blip
mission_shown = -1
IF mission_progress = 0
    LOAD_AND_LAUNCH_MISSION m1_homecoming.sc
ENDIF
IF mission_progress = 1
    LOAD_AND_LAUNCH_MISSION m2_market.sc
ENDIF
IF mission_progress = 2
    LOAD_AND_LAUNCH_MISSION m3_medicine.sc
ENDIF
IF mission_progress = 3
    LOAD_AND_LAUNCH_MISSION m4_darwaza.sc
ENDIF
IF mission_progress = 4
    LOAD_AND_LAUNCH_MISSION m5_convoy.sc
ENDIF
IF mission_progress = 5
    LOAD_AND_LAUNCH_MISSION m6_blackstone.sc
ENDIF
IF mission_progress = 6
    LOAD_AND_LAUNCH_MISSION m7_eagle.sc
ENDIF
IF mission_progress = 7
    LOAD_AND_LAUNCH_MISSION m8_fortress.sc
ENDIF
// wait until the mission thread has set the flag so we don't relaunch it
WAIT 500
RETURN

check_save:
IF LOCATE_CHAR_ON_FOOT_3D scplayer @save@ 1.2 1.2 2.0 TRUE
    IF save_armed = 1
        save_armed = 0
        PRINT_HELP AFG_SAV
        ACTIVATE_SAVE_MENU
        WHILE NOT HAS_SAVE_GAME_FINISHED
            WAIT 0
        ENDWHILE
        SET_PLAYER_CONTROL player ON
    ENDIF
ELSE
    IF NOT LOCATE_CHAR_ANY_MEANS_3D scplayer @save@ 4.0 4.0 4.0 FALSE
        save_armed = 1
    ENDIF
ENDIF
RETURN

// ----------------------------------------------------------------- wardrobe / character styles
check_wardrobe:
IF LOCATE_CHAR_ON_FOOT_3D scplayer @wardrobe@ 1.2 1.2 2.0 TRUE
    IF wardrobe_armed = 1
        wardrobe_armed = 0
        style_ok = 0
        WHILE style_ok = 0
            style += 1
            IF style > 6
                style = 1
            ENDIF
            style_ok = 1
            IF style = 5
            AND mission_progress < 4
                style_ok = 0
            ENDIF
            IF style = 6
            AND mission_progress < 5
                style_ok = 0
            ENDIF
        ENDWHILE
        GOSUB style_to_model
        GOSUB apply_style
        IF style = 1
            PRINT_NOW AFG_WR1 3000 1
        ENDIF
        IF style = 2
            PRINT_NOW AFG_WR2 3000 1
        ENDIF
        IF style = 3
            PRINT_NOW AFG_WR3 3000 1
        ENDIF
        IF style = 4
            PRINT_NOW AFG_WR4 3000 1
        ENDIF
        IF style = 5
            PRINT_NOW AFG_WR5 3000 1
        ENDIF
        IF style = 6
            PRINT_NOW AFG_WR6 3000 1
        ENDIF
        IF mission_progress < 5
            PRINT_HELP AFG_WRD
        ENDIF
    ENDIF
ELSE
    IF NOT LOCATE_CHAR_ANY_MEANS_3D scplayer @wardrobe@ 3.0 3.0 3.0 FALSE
        wardrobe_armed = 1
    ENDIF
ENDIF
RETURN

style_to_model:
// 1 villager, 2 farmer, 3 shepherd, 4 city clothes (CJ), 5 police volunteer, 6 army
IF style = 1
    style_model = 128   // DNMYLC
ENDIF
IF style = 2
    style_model = 161   // CWMYFR
ENDIF
IF style = 3
    style_model = 202   // DWMYLC2
ENDIF
IF style = 4
    style_model = 0     // CJ with his own clothes
ENDIF
IF style = 5
    style_model = 288   // DSHER
ENDIF
IF style = 6
    style_model = 287   // ARMY
ENDIF
RETURN

apply_style:
IF style_model = 0
    SET_PLAYER_MODEL player 0
    BUILD_PLAYER_MODEL player
    RETURN
ENDIF
REQUEST_MODEL style_model
LOAD_ALL_MODELS_NOW
SET_PLAYER_MODEL player style_model
MARK_MODEL_AS_NO_LONGER_NEEDED style_model
RETURN
