// Mission 22 - Iron Harvest: the army trucked the Rhino up to Darwaza. Drive it up the Ghorband valley, smash
// the three roadblocks Haji Gul put across the road (pickups, riflemen, RPG teams), then take the camp at
// Sang-e-Siah: kill the garrison and blow the ammunition dump. Lose the tank and the mission is lost.
MISSION_START
{
LVAR_INT blip cp stage tank enemy[16] eblip[16] k alive bcar[3] bblip[3] bstate[3] blocks tech tdrv tgun barrel[8] dblip dump_on away camp_on n
LVAR_FLOAT px py pz d bx by bz
GOSUB ih_main
IF HAS_DEATHARREST_BEEN_EXECUTED
    PRINT_BIG M_FAIL 5000 1
ENDIF
GOSUB ih_cleanup
MISSION_END

ih_main:
SCRIPT_NAME AFM22
blip = -1
cp = -1
tank = -1
tech = -1
tdrv = -1
tgun = -1
dblip = -1
dump_on = 0
away = 0
camp_on = 0
blocks = 3
k = 0
WHILE k < 16
    enemy[k] = -1
    eblip[k] = -1
    k += 1
ENDWHILE
k = 0
WHILE k < 3
    bcar[k] = -1
    bblip[k] = -1
    bstate[k] = 0
    k += 1
ENDWHILE
k = 0
WHILE k < 8
    barrel[k] = -1
    k += 1
ENDWHILE
flag_player_on_mission = 1
REGISTER_MISSION_GIVEN
WAIT 0
REQUEST_MODEL 287
REQUEST_MODEL 247
REQUEST_MODEL 248
REQUEST_MODEL 162
REQUEST_MODEL 200
REQUEST_MODEL 355
REQUEST_MODEL 359
REQUEST_MODEL 1225
REQUEST_MODEL RHINO
REQUEST_MODEL BOBCAT
LOAD_ALL_MODELS_NOW
GOSUB ih_contact

// ---------------------------------------------------------------- Hamid at HQ
DO_FADE 400 FADE_OUT
WAIT 450
SET_PLAYER_CONTROL player OFF
SWITCH_WIDESCREEN ON
SET_CHAR_COORDINATES scplayer @talk_base_hq@
SET_CHAR_HEADING scplayer @talk_base_hq.h@
SET_CHAR_COORDINATES contact_ped[2] @base_major@
SET_CHAR_HEADING contact_ped[2] @base_major.h@
WAIT 600
SET_FIXED_CAMERA_POSITION @cam_base_hq_w@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_wt@ JUMP_CUT
DO_FADE 400 FADE_IN
PRINT_BIG AFI_T 3000 2
TASK_PLAY_ANIM contact_ped[2] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AFI_01 7000 1
WAIT 7000
SET_FIXED_CAMERA_POSITION @cam_base_hq_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_ct@ JUMP_CUT
PRINT_NOW AFI_02 7000 1
WAIT 7000
SET_FIXED_CAMERA_POSITION @cam_base_hq_p@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_pt@ JUMP_CUT
CLEAR_CHAR_TASKS contact_ped[2]
PRINT_NOW AFI_06 3500 1
WAIT 3500
SET_FIXED_CAMERA_POSITION @cam_base_hq_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_ct@ JUMP_CUT
TASK_PLAY_ANIM contact_ped[2] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AFI_07 5500 1
WAIT 5500
CLEAR_CHAR_TASKS contact_ped[2]
DO_FADE 800 FADE_OUT
WAIT 900
// the trucks drop you at Darwaza next to the tank
SET_TIME_OF_DAY 9 0
REQUEST_COLLISION @ih_tank.x@ @ih_tank.y@
LOAD_SCENE @ih_tank@
CREATE_CAR RHINO @ih_tank@ tank
SET_CAR_HEADING tank @ih_tank.h@
SET_CAR_HEALTH tank 4000
SET_CHAR_COORDINATES scplayer @checkpoint@
RESTORE_CAMERA_JUMPCUT
SET_CAMERA_BEHIND_PLAYER
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
DO_FADE 800 FADE_IN
ADD_BLIP_FOR_CAR tank blip
PRINT_NOW AFI_03 5000 1
stage = 0

// ---------------------------------------------------------------- up the valley
ih_loop:
WAIT 250
GET_CHAR_COORDINATES scplayer px py pz
IF IS_CAR_DEAD tank
    CLEAR_ONSCREEN_COUNTER mission_counter
    PRINT_NOW AFI_11 5000 1
    PRINT_BIG M_FAIL 5000 1
    RETURN
ENDIF
IF stage = 0
    IF IS_CHAR_IN_CAR scplayer tank
        stage = 1
        REMOVE_BLIP blip
        blip = -1
        PRINT_NOW AFI_08 6000 1
        mission_counter = 3
        DISPLAY_ONSCREEN_COUNTER_WITH_STRING mission_counter COUNTER_DISPLAY_NUMBER AFI_CT
        ADD_BLIP_FOR_COORD @ih_b0_car@ blip
    ENDIF
    GOTO ih_loop
ENDIF
// out of the tank: a reminder, no fail
IF NOT IS_CHAR_IN_CAR scplayer tank
    IF away = 0
        away = 1
        ADD_BLIP_FOR_CAR tank cp
        PRINT_NOW AFI_12 4000 1
    ENDIF
ELSE
    IF away = 1
        away = 0
        REMOVE_BLIP cp
        cp = -1
    ENDIF
ENDIF
IF stage = 1
    GOSUB ih_roadblocks
    IF blocks = 0
        stage = 2
        CLEAR_ONSCREEN_COUNTER mission_counter
        IF NOT blip = -1
            REMOVE_BLIP blip
            blip = -1
        ENDIF
        PRINT_NOW AFI_10 7000 1
        ADD_BLIP_FOR_COORD @bandit_camp@ blip
    ENDIF
    GOTO ih_loop
ENDIF
// stage 2: the camp
IF camp_on = 0
    GET_DISTANCE_BETWEEN_COORDS_2D px py @bandit_camp.x@ @bandit_camp.y@ d
    IF d < 280.0
        GOSUB ih_camp
    ENDIF
    GOTO ih_loop
ENDIF
alive = 0
k = 0
WHILE k < 16
    IF NOT enemy[k] = -1
        IF IS_CHAR_DEAD enemy[k]
            IF NOT eblip[k] = -1
                REMOVE_BLIP eblip[k]
                eblip[k] = -1
            ENDIF
        ELSE
            alive += 1
        ENDIF
    ENDIF
    k += 1
ENDWHILE
IF NOT IS_CHAR_DEAD tgun
    alive += 1
ENDIF
IF NOT IS_CHAR_DEAD tdrv
    alive += 1
ENDIF
mission_counter = alive
IF dump_on = 1
    GOSUB ih_dump_check
ENDIF
IF alive = 0
AND dump_on = 0
    GOSUB ih_pass
    RETURN
ENDIF
GOTO ih_loop

// ---------------------------------------------------------------- the three roadblocks
ih_roadblocks:
k = 0
WHILE k < 3
    IF bstate[k] = 0
        IF k = 0
            GET_DISTANCE_BETWEEN_COORDS_2D px py @ih_b0_car.x@ @ih_b0_car.y@ d
        ENDIF
        IF k = 1
            GET_DISTANCE_BETWEEN_COORDS_2D px py @ih_b1_car.x@ @ih_b1_car.y@ d
        ENDIF
        IF k = 2
            GET_DISTANCE_BETWEEN_COORDS_2D px py @ih_b2_car.x@ @ih_b2_car.y@ d
        ENDIF
        IF d < 240.0
            bstate[k] = 1
            GOSUB ih_spawn_block
        ENDIF
    ENDIF
    IF bstate[k] = 1
        // a roadblock is finished when the pickup is wrecked and its four men are dead
        n = 0
        IF NOT IS_CAR_DEAD bcar[k]
            n += 1
        ENDIF
        alive = k
        alive *= 4
        dump_on = alive
        dump_on += 4
        WHILE alive < dump_on
            IF IS_CHAR_DEAD enemy[alive]
                IF NOT eblip[alive] = -1
                    REMOVE_BLIP eblip[alive]
                    eblip[alive] = -1
                ENDIF
            ELSE
                n += 1
            ENDIF
            alive += 1
        ENDWHILE
        dump_on = 0
        IF n = 0
            bstate[k] = 2
            blocks -= 1
            mission_counter = blocks
            IF NOT bblip[k] = -1
                REMOVE_BLIP bblip[k]
                bblip[k] = -1
            ENDIF
            IF NOT blip = -1
                REMOVE_BLIP blip
                blip = -1
            ENDIF
            IF k = 0
                ADD_BLIP_FOR_COORD @ih_b1_car@ blip
            ENDIF
            IF k = 1
                ADD_BLIP_FOR_COORD @ih_b2_car@ blip
            ENDIF
        ENDIF
    ENDIF
    k += 1
ENDWHILE
RETURN

ih_spawn_block:
PRINT_NOW AFI_09 4000 1
alive = k
alive *= 4
IF k = 0
    CREATE_CAR BOBCAT @ih_b0_car@ bcar[0]
    SET_CAR_HEADING bcar[0] @ih_b0_car.h@
    CREATE_CHAR PEDTYPE_MISSION1 247 @ih_b0_e0@ enemy[0]
    CREATE_CHAR PEDTYPE_MISSION1 248 @ih_b0_e1@ enemy[1]
    CREATE_CHAR PEDTYPE_MISSION1 162 @ih_b0_e2@ enemy[2]
    CREATE_CHAR PEDTYPE_MISSION1 200 @ih_b0_e3@ enemy[3]
ENDIF
IF k = 1
    CREATE_CAR BOBCAT @ih_b1_car@ bcar[1]
    SET_CAR_HEADING bcar[1] @ih_b1_car.h@
    CREATE_CHAR PEDTYPE_MISSION1 247 @ih_b1_e0@ enemy[4]
    CREATE_CHAR PEDTYPE_MISSION1 248 @ih_b1_e1@ enemy[5]
    CREATE_CHAR PEDTYPE_MISSION1 162 @ih_b1_e2@ enemy[6]
    CREATE_CHAR PEDTYPE_MISSION1 200 @ih_b1_e3@ enemy[7]
ENDIF
IF k = 2
    CREATE_CAR BOBCAT @ih_b2_car@ bcar[2]
    SET_CAR_HEADING bcar[2] @ih_b2_car.h@
    CREATE_CHAR PEDTYPE_MISSION1 247 @ih_b2_e0@ enemy[8]
    CREATE_CHAR PEDTYPE_MISSION1 248 @ih_b2_e1@ enemy[9]
    CREATE_CHAR PEDTYPE_MISSION1 162 @ih_b2_e2@ enemy[10]
    CREATE_CHAR PEDTYPE_MISSION1 200 @ih_b2_e3@ enemy[11]
ENDIF
SET_CAR_HEALTH bcar[k] 1200
ADD_BLIP_FOR_CAR bcar[k] bblip[k]
n = alive
n += 4
WHILE alive < n
    GIVE_WEAPON_TO_CHAR enemy[alive] WEAPONTYPE_AK47 500
    SET_CHAR_ACCURACY enemy[alive] 40
    SET_CHAR_DROPS_WEAPONS_WHEN_DEAD enemy[alive] FALSE
    ADD_BLIP_FOR_CHAR enemy[alive] eblip[alive]
    TASK_KILL_CHAR_ON_FOOT enemy[alive] scplayer
    alive += 1
ENDWHILE
// two RPG gunners per roadblock: the tank is the target
n -= 1
GIVE_WEAPON_TO_CHAR enemy[n] WEAPONTYPE_ROCKETLAUNCHER 10
SET_CHAR_ACCURACY enemy[n] 30
SET_CURRENT_CHAR_WEAPON enemy[n] WEAPONTYPE_ROCKETLAUNCHER
n -= 2
GIVE_WEAPON_TO_CHAR enemy[n] WEAPONTYPE_ROCKETLAUNCHER 10
SET_CHAR_ACCURACY enemy[n] 30
SET_CURRENT_CHAR_WEAPON enemy[n] WEAPONTYPE_ROCKETLAUNCHER
RETURN

// ---------------------------------------------------------------- Sang-e-Siah
ih_camp:
camp_on = 1
REMOVE_BLIP blip
blip = -1
// release the roadblock crews and wrecks before reusing the slots
k = 0
WHILE k < 12
    IF NOT eblip[k] = -1
        REMOVE_BLIP eblip[k]
        eblip[k] = -1
    ENDIF
    IF NOT enemy[k] = -1
        MARK_CHAR_AS_NO_LONGER_NEEDED enemy[k]
        enemy[k] = -1
    ENDIF
    k += 1
ENDWHILE
k = 0
WHILE k < 3
    IF NOT bcar[k] = -1
        MARK_CAR_AS_NO_LONGER_NEEDED bcar[k]
        bcar[k] = -1
    ENDIF
    k += 1
ENDWHILE
// the garrison stands at the old Black Stone posts (enemy 12..15 are the four nearest the road)
CREATE_CHAR PEDTYPE_MISSION1 247 @camp0@ enemy[0]
CREATE_CHAR PEDTYPE_MISSION1 248 @camp1@ enemy[1]
CREATE_CHAR PEDTYPE_MISSION1 162 @camp2@ enemy[2]
CREATE_CHAR PEDTYPE_MISSION1 200 @camp3@ enemy[3]
CREATE_CHAR PEDTYPE_MISSION1 247 @camp4@ enemy[4]
CREATE_CHAR PEDTYPE_MISSION1 248 @camp5@ enemy[5]
CREATE_CHAR PEDTYPE_MISSION1 162 @camp6@ enemy[6]
CREATE_CHAR PEDTYPE_MISSION1 200 @camp7@ enemy[7]
CREATE_CHAR PEDTYPE_MISSION1 247 @camp8@ enemy[8]
CREATE_CHAR PEDTYPE_MISSION1 248 @camp9@ enemy[9]
k = 0
WHILE k < 10
    GIVE_WEAPON_TO_CHAR enemy[k] WEAPONTYPE_AK47 500
    SET_CHAR_ACCURACY enemy[k] 40
    SET_CHAR_DROPS_WEAPONS_WHEN_DEAD enemy[k] FALSE
    ADD_BLIP_FOR_CHAR enemy[k] eblip[k]
    TASK_KILL_CHAR_ON_FOOT enemy[k] scplayer
    k += 1
ENDWHILE
GIVE_WEAPON_TO_CHAR enemy[1] WEAPONTYPE_ROCKETLAUNCHER 10
GIVE_WEAPON_TO_CHAR enemy[5] WEAPONTYPE_ROCKETLAUNCHER 10
GIVE_WEAPON_TO_CHAR enemy[8] WEAPONTYPE_ROCKETLAUNCHER 10
SET_CURRENT_CHAR_WEAPON enemy[1] WEAPONTYPE_ROCKETLAUNCHER
SET_CURRENT_CHAR_WEAPON enemy[5] WEAPONTYPE_ROCKETLAUNCHER
SET_CURRENT_CHAR_WEAPON enemy[8] WEAPONTYPE_ROCKETLAUNCHER
SET_CHAR_ACCURACY enemy[1] 30
SET_CHAR_ACCURACY enemy[5] 30
SET_CHAR_ACCURACY enemy[8] 30
k = 10
WHILE k < 16
    enemy[k] = -1
    k += 1
ENDWHILE
// the technical
CREATE_CAR BOBCAT @ih_tech@ tech
SET_CAR_HEADING tech @ih_tech.h@
CREATE_CHAR_INSIDE_CAR tech PEDTYPE_MISSION1 247 tdrv
CREATE_CHAR_AS_PASSENGER tech PEDTYPE_MISSION1 248 0 tgun
GIVE_WEAPON_TO_CHAR tgun WEAPONTYPE_AK47 900
GIVE_WEAPON_TO_CHAR tdrv WEAPONTYPE_AK47 500
SET_CHAR_DROPS_WEAPONS_WHEN_DEAD tgun FALSE
SET_CHAR_DROPS_WEAPONS_WHEN_DEAD tdrv FALSE
TASK_CAR_MISSION tdrv tech -1 3 30.0 3
TASK_DRIVE_BY tgun scplayer -1 0.0 0.0 0.0 60.0 DRIVEBY_AI_ALL_DIRN TRUE 100
// the ammunition dump: eight barrels around the old loot spot
k = 0
WHILE k < 8
    bx = @m6_loot.x@
    by = @m6_loot.y@
    bz = @m6_loot.z@
    IF k = 1
        bx += 1.6
    ENDIF
    IF k = 2
        bx -= 1.6
    ENDIF
    IF k = 3
        by += 1.6
    ENDIF
    IF k = 4
        by -= 1.6
    ENDIF
    IF k = 5
        bx += 1.6
        by += 1.6
    ENDIF
    IF k = 6
        bx -= 1.6
        by -= 1.6
    ENDIF
    IF k = 7
        bx += 1.6
        by -= 1.6
    ENDIF
    CREATE_OBJECT 1225 bx by bz barrel[k]
    k += 1
ENDWHILE
ADD_BLIP_FOR_COORD @m6_loot@ dblip
dump_on = 1
mission_counter = 12
DISPLAY_ONSCREEN_COUNTER_WITH_STRING mission_counter COUNTER_DISPLAY_NUMBER AFI_C2
RETURN

ih_dump_check:
// one barrel hit = the whole dump goes up
n = 0
k = 0
WHILE k < 8
    IF NOT barrel[k] = -1
        IF DOES_OBJECT_EXIST barrel[k]
            IF HAS_OBJECT_BEEN_DAMAGED barrel[k]
                n = 1
            ENDIF
        ELSE
            n = 1
        ENDIF
    ENDIF
    k += 1
ENDWHILE
IF n = 0
    RETURN
ENDIF
dump_on = 0
IF NOT dblip = -1
    REMOVE_BLIP dblip
    dblip = -1
ENDIF
k = 0
WHILE k < 8
    IF NOT barrel[k] = -1
        IF DOES_OBJECT_EXIST barrel[k]
            DELETE_OBJECT barrel[k]
        ENDIF
        barrel[k] = -1
    ENDIF
    k += 1
ENDWHILE
ADD_EXPLOSION @m6_loot@ EXPLOSION_ROCKET
WAIT 300
ADD_EXPLOSION @m6_loot@ EXPLOSION_CAR
WAIT 300
ADD_EXPLOSION @m6_loot@ EXPLOSION_BARREL
WAIT 400
ADD_EXPLOSION @m6_loot@ EXPLOSION_CAR
RETURN

// ---------------------------------------------------------------- done
ih_pass:
CLEAR_ONSCREEN_COUNTER mission_counter
WAIT 2500
PRINT_NOW AFI_13 7000 1
WAIT 4000
mission_progress = 22
ADD_SCORE player 5000
PRINT_WITH_NUMBER_BIG M_PASS 5000 5000 1
REGISTER_MISSION_PASSED AFI_T
PLAYER_MADE_PROGRESS 1
PLAY_MISSION_PASSED_TUNE 1
RETURN

ih_contact:
IF NOT DOES_CHAR_EXIST contact_ped[2]
    CREATE_CHAR PEDTYPE_CIVMALE 287 @base_major@ contact_ped[2]
    SET_CHAR_HEADING contact_ped[2] @base_major.h@
    SET_CHAR_PROOFS contact_ped[2] TRUE TRUE TRUE TRUE TRUE
    SET_CHAR_NEVER_TARGETTED contact_ped[2] TRUE
    SET_CHAR_STAY_IN_SAME_PLACE contact_ped[2] TRUE
ENDIF
RETURN

ih_cleanup:
CLEAR_ONSCREEN_COUNTER mission_counter
IF NOT blip = -1
    REMOVE_BLIP blip
    blip = -1
ENDIF
IF NOT cp = -1
    REMOVE_BLIP cp
    cp = -1
ENDIF
IF NOT dblip = -1
    REMOVE_BLIP dblip
    dblip = -1
ENDIF
k = 0
WHILE k < 16
    IF NOT eblip[k] = -1
        REMOVE_BLIP eblip[k]
        eblip[k] = -1
    ENDIF
    IF NOT enemy[k] = -1
        MARK_CHAR_AS_NO_LONGER_NEEDED enemy[k]
        enemy[k] = -1
    ENDIF
    k += 1
ENDWHILE
k = 0
WHILE k < 3
    IF NOT bblip[k] = -1
        REMOVE_BLIP bblip[k]
        bblip[k] = -1
    ENDIF
    IF NOT bcar[k] = -1
        MARK_CAR_AS_NO_LONGER_NEEDED bcar[k]
        bcar[k] = -1
    ENDIF
    k += 1
ENDWHILE
k = 0
WHILE k < 8
    IF NOT barrel[k] = -1
        IF DOES_OBJECT_EXIST barrel[k]
            DELETE_OBJECT barrel[k]
        ENDIF
        barrel[k] = -1
    ENDIF
    k += 1
ENDWHILE
IF NOT tdrv = -1
    MARK_CHAR_AS_NO_LONGER_NEEDED tdrv
    tdrv = -1
ENDIF
IF NOT tgun = -1
    MARK_CHAR_AS_NO_LONGER_NEEDED tgun
    tgun = -1
ENDIF
IF NOT tech = -1
    MARK_CAR_AS_NO_LONGER_NEEDED tech
    tech = -1
ENDIF
IF NOT tank = -1
    MARK_CAR_AS_NO_LONGER_NEEDED tank
    tank = -1
ENDIF
IF DOES_CHAR_EXIST contact_ped[2]
    CLEAR_CHAR_TASKS contact_ped[2]
ENDIF
MARK_MODEL_AS_NO_LONGER_NEEDED 287
MARK_MODEL_AS_NO_LONGER_NEEDED 247
MARK_MODEL_AS_NO_LONGER_NEEDED 248
MARK_MODEL_AS_NO_LONGER_NEEDED 162
MARK_MODEL_AS_NO_LONGER_NEEDED 200
MARK_MODEL_AS_NO_LONGER_NEEDED 1225
MARK_MODEL_AS_NO_LONGER_NEEDED RHINO
MARK_MODEL_AS_NO_LONGER_NEEDED BOBCAT
RESTORE_CAMERA_JUMPCUT
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
flag_player_on_mission = 0
MISSION_HAS_FINISHED
RETURN
}
