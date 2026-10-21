// Mission 19 - Jingle Truck (the overweight payload): steal Haji Gul's overloaded truck from the fortress gate
// and take it down the mountain road at night. Half way down the brakes go: keep the momentum up or the axles
// lock, but roll it on a hairpin and it is over. Pickups chase you; use the truck's weight to swipe them off
// the road. At the bottom the road is blocked: hit the dirt ramp at full speed and jump the block.
MISSION_START
{
LVAR_INT blip cp stage truck enemy[8] eblip[8] k alive guards_left wp k2 brakes chaser[2] cdrv[2] cgun[2] block[2] ramp retask health
LVAR_FLOAT px py pz d spd cx cy cz
GOSUB jt_main
IF HAS_DEATHARREST_BEEN_EXECUTED
    PRINT_BIG M_FAIL 5000 1
ENDIF
GOSUB jt_cleanup
MISSION_END

jt_main:
SCRIPT_NAME AFM19
blip = -1
cp = -1
truck = -1
ramp = -1
brakes = 0
retask = 0
k = 0
WHILE k < 8
    enemy[k] = -1
    eblip[k] = -1
    k += 1
ENDWHILE
k = 0
WHILE k < 2
    chaser[k] = -1
    cdrv[k] = -1
    cgun[k] = -1
    block[k] = -1
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
REQUEST_MODEL 1660
REQUEST_MODEL YANKEE
REQUEST_MODEL BOBCAT
LOAD_ALL_MODELS_NOW
GOSUB jt_contact

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
PRINT_BIG AFL_T 3000 2
TASK_PLAY_ANIM contact_ped[2] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AFL_01 7500 1
WAIT 7500
SET_FIXED_CAMERA_POSITION @cam_base_hq_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_ct@ JUMP_CUT
PRINT_NOW AFL_02 6000 1
WAIT 6000
SET_FIXED_CAMERA_POSITION @cam_base_hq_p@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_pt@ JUMP_CUT
CLEAR_CHAR_TASKS contact_ped[2]
PRINT_NOW AFL_06 3000 1
WAIT 3000
SET_FIXED_CAMERA_POSITION @cam_base_hq_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_ct@ JUMP_CUT
TASK_PLAY_ANIM contact_ped[2] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AFL_07 5000 1
WAIT 5000
CLEAR_CHAR_TASKS contact_ped[2]
DO_FADE 800 FADE_OUT
WAIT 900
// Karim drops you above the gate
SET_TIME_OF_DAY 23 0
REQUEST_COLLISION @jt_drop.x@ @jt_drop.y@
LOAD_SCENE @jt_drop@
SET_CHAR_COORDINATES scplayer @jt_drop@
SET_CHAR_HEADING scplayer @jt_drop.h@
GIVE_WEAPON_TO_CHAR scplayer WEAPONTYPE_AK47 300
SET_CURRENT_CHAR_WEAPON scplayer WEAPONTYPE_AK47
CREATE_CAR YANKEE @fort_gate_car@ truck
SET_CAR_HEADING truck @fort_gate_car.h@
SET_CAR_HEALTH truck 2600
SET_CAR_HEAVY truck TRUE
CREATE_CHAR PEDTYPE_MISSION1 247 @fort_rpg0@ enemy[0]
CREATE_CHAR PEDTYPE_MISSION1 248 @fort_rpg1@ enemy[1]
CREATE_CHAR PEDTYPE_MISSION1 162 @fort_ally0@ enemy[2]
CREATE_CHAR PEDTYPE_MISSION1 200 @fort_ally1@ enemy[3]
k = 0
WHILE k < 4
    GIVE_WEAPON_TO_CHAR enemy[k] WEAPONTYPE_AK47 500
    SET_CHAR_ACCURACY enemy[k] 35
    SET_CHAR_DROPS_WEAPONS_WHEN_DEAD enemy[k] FALSE
    ADD_BLIP_FOR_CHAR enemy[k] eblip[k]
    TASK_KILL_CHAR_ON_FOOT enemy[k] scplayer
    k += 1
ENDWHILE
SET_FIXED_CAMERA_POSITION @cam_jt@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_jt_t@ JUMP_CUT
DO_FADE 800 FADE_IN
WAIT 2500
RESTORE_CAMERA_JUMPCUT
SET_CAMERA_BEHIND_PLAYER
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
PRINT_NOW AFL_03 6000 1
mission_counter = 4
DISPLAY_ONSCREEN_COUNTER_WITH_STRING mission_counter COUNTER_DISPLAY_NUMBER AFL_CT
stage = 0

// ---------------------------------------------------------------- the gate
jt_loop:
WAIT 250
GET_CHAR_COORDINATES scplayer px py pz
IF IS_CAR_DEAD truck
    CLEAR_ONSCREEN_COUNTER mission_counter
    CLEAR_ONSCREEN_COUNTER mission_bar
    PRINT_NOW AFL_14 5000 1
    PRINT_BIG M_FAIL 5000 1
    RETURN
ENDIF
IF stage = 0
    alive = 0
    k = 0
    WHILE k < 4
        IF IS_CHAR_DEAD enemy[k]
            IF NOT eblip[k] = -1
                REMOVE_BLIP eblip[k]
                eblip[k] = -1
            ENDIF
        ELSE
            alive += 1
        ENDIF
        k += 1
    ENDWHILE
    mission_counter = alive
    IF alive = 0
        CLEAR_ONSCREEN_COUNTER mission_counter
        stage = 1
        ADD_BLIP_FOR_CAR truck blip
        PRINT_NOW AFL_08 6000 1
    ENDIF
    GOTO jt_loop
ENDIF
IF stage = 1
    IF IS_CHAR_IN_CAR scplayer truck
        stage = 2
        REMOVE_BLIP blip
        blip = -1
        ADD_BLIP_FOR_COORD @deh_e_bagh@ blip
        CREATE_CHECKPOINT CHECKPOINT_TUBE @deh_e_bagh@ @deh_e_bagh@ 10.0 cp
        wp = fr_count
        wp -= 1
        PRINT_NOW AFL_08 5000 1
    ENDIF
    GOTO jt_loop
ENDIF
// stage 2: the descent. Where is the truck along the road?
GET_CAR_COORDINATES truck cx cy cz
IF wp > 0
    k2 = wp
    k2 -= 1
    GET_DISTANCE_BETWEEN_COORDS_2D cx cy fr_x[k2] fr_y[k2] d
    IF d < 45.0
        wp -= 1
    ENDIF
ENDIF
// the brakes go after the first switchbacks
k2 = fr_count
k2 -= 9
IF brakes = 0
AND wp <= k2
    GOSUB jt_brakes_fail
ENDIF
IF brakes = 1
    IF IS_CHAR_IN_CAR scplayer truck
        GET_CAR_SPEED truck spd
    ELSE
        spd = 0.0
    ENDIF
    IF spd < 9.0
        mission_bar -= 4
    ELSE
        mission_bar += 4
        IF mission_bar > 100
            mission_bar = 100
        ENDIF
    ENDIF
    IF mission_bar < 1
        CLEAR_ONSCREEN_COUNTER mission_bar
        PRINT_NOW AFL_12 5000 1
        PRINT_BIG M_FAIL 5000 1
        RETURN
    ENDIF
    IF IS_CAR_UPSIDEDOWN truck
        CLEAR_ONSCREEN_COUNTER mission_bar
        PRINT_NOW AFL_13 5000 1
        PRINT_BIG M_FAIL 5000 1
        RETURN
    ENDIF
    retask += 1
    IF retask > 40
        retask = 0
        k = 0
        WHILE k < 2
            IF NOT IS_CAR_DEAD chaser[k]
            AND NOT IS_CHAR_DEAD cdrv[k]
                TASK_CAR_MISSION cdrv[k] chaser[k] -1 3 34.0 3
            ENDIF
            k += 1
        ENDWHILE
    ENDIF
ENDIF
// the block at the bottom
IF brakes = 1
AND ramp = -1
AND wp <= 8
    GOSUB jt_block
ENDIF
IF LOCATE_CAR_3D truck @deh_e_bagh@ 30.0 30.0 12.0 FALSE
    GOSUB jt_pass
    RETURN
ENDIF
GOTO jt_loop

// ---------------------------------------------------------------- helpers
jt_brakes_fail:
brakes = 1
PRINT_NOW AFL_09 7000 1
mission_bar = 100
DISPLAY_ONSCREEN_COUNTER_WITH_STRING mission_bar COUNTER_DISPLAY_BAR AFL_MO
CREATE_CAR BOBCAT @jt_chase0@ chaser[0]
SET_CAR_HEADING chaser[0] @jt_chase0.h@
CREATE_CAR BOBCAT @jt_chase1@ chaser[1]
SET_CAR_HEADING chaser[1] @jt_chase1.h@
k = 0
WHILE k < 2
    CREATE_CHAR_INSIDE_CAR chaser[k] PEDTYPE_MISSION1 247 cdrv[k]
    CREATE_CHAR_AS_PASSENGER chaser[k] PEDTYPE_MISSION1 248 0 cgun[k]
    GIVE_WEAPON_TO_CHAR cdrv[k] WEAPONTYPE_PISTOL 200
    GIVE_WEAPON_TO_CHAR cgun[k] WEAPONTYPE_AK47 900
    SET_CHAR_ACCURACY cgun[k] 30
    SET_CHAR_DROPS_WEAPONS_WHEN_DEAD cdrv[k] FALSE
    SET_CHAR_DROPS_WEAPONS_WHEN_DEAD cgun[k] FALSE
    TASK_CAR_MISSION cdrv[k] chaser[k] -1 3 34.0 3
    TASK_DRIVE_BY cgun[k] scplayer -1 0.0 0.0 0.0 60.0 DRIVEBY_AI_ALL_DIRN TRUE 100
    k += 1
ENDWHILE
PRINT AFL_10 6000 1
RETURN

jt_block:
PRINT_NOW AFL_11 7000 1
CREATE_OBJECT 1660 @jt_ramp@ ramp
SET_OBJECT_HEADING ramp @jt_ramp.h@
CREATE_CAR BOBCAT @jt_block0@ block[0]
SET_CAR_HEADING block[0] @jt_block0.h@
CREATE_CAR BOBCAT @jt_block1@ block[1]
SET_CAR_HEADING block[1] @jt_block1.h@
CREATE_CHAR PEDTYPE_MISSION1 162 @jt_block0@ enemy[4]
CREATE_CHAR PEDTYPE_MISSION1 200 @jt_block1@ enemy[5]
k = 4
WHILE k < 6
    GIVE_WEAPON_TO_CHAR enemy[k] WEAPONTYPE_AK47 500
    SET_CHAR_ACCURACY enemy[k] 30
    SET_CHAR_DROPS_WEAPONS_WHEN_DEAD enemy[k] FALSE
    TASK_KILL_CHAR_ON_FOOT enemy[k] scplayer
    k += 1
ENDWHILE
RETURN

jt_pass:
CLEAR_ONSCREEN_COUNTER mission_bar
REMOVE_BLIP blip
blip = -1
DELETE_CHECKPOINT cp
cp = -1
k = 0
WHILE k < 2
    IF NOT chaser[k] = -1
        IF NOT IS_CAR_DEAD chaser[k]
            EXPLODE_CAR chaser[k]
        ENDIF
    ENDIF
    k += 1
ENDWHILE
WAIT 2500
PRINT_NOW AFL_15 7000 1
WAIT 4000
mission_progress = 19
ADD_SCORE player 5000
PRINT_WITH_NUMBER_BIG M_PASS 5000 5000 1
REGISTER_MISSION_PASSED AFL_T
PLAYER_MADE_PROGRESS 1
PLAY_MISSION_PASSED_TUNE 1
RETURN

jt_contact:
IF NOT DOES_CHAR_EXIST contact_ped[2]
    CREATE_CHAR PEDTYPE_CIVMALE 287 @base_major@ contact_ped[2]
    SET_CHAR_HEADING contact_ped[2] @base_major.h@
    SET_CHAR_PROOFS contact_ped[2] TRUE TRUE TRUE TRUE TRUE
    SET_CHAR_NEVER_TARGETTED contact_ped[2] TRUE
    SET_CHAR_STAY_IN_SAME_PLACE contact_ped[2] TRUE
ENDIF
RETURN

jt_cleanup:
CLEAR_ONSCREEN_COUNTER mission_counter
CLEAR_ONSCREEN_COUNTER mission_bar
IF NOT blip = -1
    REMOVE_BLIP blip
    blip = -1
ENDIF
IF NOT cp = -1
    DELETE_CHECKPOINT cp
    cp = -1
ENDIF
k = 0
WHILE k < 8
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
WHILE k < 2
    IF NOT cdrv[k] = -1
        MARK_CHAR_AS_NO_LONGER_NEEDED cdrv[k]
        cdrv[k] = -1
    ENDIF
    IF NOT cgun[k] = -1
        MARK_CHAR_AS_NO_LONGER_NEEDED cgun[k]
        cgun[k] = -1
    ENDIF
    IF NOT chaser[k] = -1
        MARK_CAR_AS_NO_LONGER_NEEDED chaser[k]
        chaser[k] = -1
    ENDIF
    IF NOT block[k] = -1
        MARK_CAR_AS_NO_LONGER_NEEDED block[k]
        block[k] = -1
    ENDIF
    k += 1
ENDWHILE
IF NOT ramp = -1
    IF DOES_OBJECT_EXIST ramp
        DELETE_OBJECT ramp
    ENDIF
    ramp = -1
ENDIF
IF NOT truck = -1
    MARK_CAR_AS_NO_LONGER_NEEDED truck
    truck = -1
ENDIF
IF DOES_CHAR_EXIST contact_ped[2]
    CLEAR_CHAR_TASKS contact_ped[2]
ENDIF
MARK_MODEL_AS_NO_LONGER_NEEDED 287
MARK_MODEL_AS_NO_LONGER_NEEDED 247
MARK_MODEL_AS_NO_LONGER_NEEDED 248
MARK_MODEL_AS_NO_LONGER_NEEDED 162
MARK_MODEL_AS_NO_LONGER_NEEDED 200
MARK_MODEL_AS_NO_LONGER_NEEDED 1660
MARK_MODEL_AS_NO_LONGER_NEEDED YANKEE
MARK_MODEL_AS_NO_LONGER_NEEDED BOBCAT
RESTORE_CAMERA_JUMPCUT
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
flag_player_on_mission = 0
MISSION_HAS_FINISHED
RETURN
}
