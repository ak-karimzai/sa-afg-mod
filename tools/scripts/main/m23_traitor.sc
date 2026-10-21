// Mission 23 - Traitor's Road: Nasir runs for the fortress up the mountain road in his police pickup with two
// escort cars and the station safe. Chase him in the army jeep and stop him before the fortress gate; the escort
// shoots back and blocks the road.
MISSION_START
{
LVAR_INT blip cp stage jeep car[4] drv[4] gun[4] wp[3] nblip k k2 retask taunted blocked
LVAR_FLOAT px py pz d cx cy cz bx by bz bh
GOSUB tr_main
IF HAS_DEATHARREST_BEEN_EXECUTED
    PRINT_BIG M_FAIL 5000 1
ENDIF
GOSUB tr_cleanup
MISSION_END

tr_main:
SCRIPT_NAME AFM23
blip = -1
cp = -1
jeep = -1
nblip = -1
retask = 0
taunted = 0
blocked = 0
k = 0
WHILE k < 4
    car[k] = -1
    drv[k] = -1
    gun[k] = -1
    k += 1
ENDWHILE
wp[0] = 0
wp[1] = 0
wp[2] = 0
flag_player_on_mission = 1
REGISTER_MISSION_GIVEN
WAIT 0
REQUEST_MODEL 287
REQUEST_MODEL 288
REQUEST_MODEL 283
REQUEST_MODEL 247
REQUEST_MODEL 248
REQUEST_MODEL 355
REQUEST_MODEL 359
REQUEST_MODEL COPCARRU
REQUEST_MODEL BOBCAT
REQUEST_MODEL PATRIOT
LOAD_ALL_MODELS_NOW
GOSUB tr_contact

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
PRINT_BIG AFT_T 3000 2
TASK_PLAY_ANIM contact_ped[2] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AFT_01 7000 1
WAIT 7000
SET_FIXED_CAMERA_POSITION @cam_base_hq_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_ct@ JUMP_CUT
PRINT_NOW AFT_02 6500 1
WAIT 6500
SET_FIXED_CAMERA_POSITION @cam_base_hq_p@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_pt@ JUMP_CUT
CLEAR_CHAR_TASKS contact_ped[2]
PRINT_NOW AFT_06 2000 1
WAIT 2000
SET_FIXED_CAMERA_POSITION @cam_base_hq_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_ct@ JUMP_CUT
TASK_PLAY_ANIM contact_ped[2] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AFT_07 5000 1
WAIT 5000
CLEAR_CHAR_TASKS contact_ped[2]
DO_FADE 800 FADE_OUT
WAIT 900
// Deh-e-Bagh, the foot of the mountain road
SET_TIME_OF_DAY 17 0
REQUEST_COLLISION @tr_jeep.x@ @tr_jeep.y@
LOAD_SCENE @tr_jeep@
CREATE_CAR PATRIOT @tr_jeep@ jeep
SET_CAR_HEADING jeep @tr_jeep.h@
SET_CAR_HEALTH jeep 1800
WARP_CHAR_INTO_CAR scplayer jeep
GOSUB tr_spawn_convoy
RESTORE_CAMERA_JUMPCUT
SET_CAMERA_BEHIND_PLAYER
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
DO_FADE 800 FADE_IN
PRINT_NOW AFT_03 5000 1
PRINT AFT_08 6000 1
stage = 1

// ---------------------------------------------------------------- the chase
tr_loop:
WAIT 250
GET_CHAR_COORDINATES scplayer px py pz
retask += 1
k = 0
WHILE k < 3
    IF NOT IS_CAR_DEAD car[k]
    AND NOT IS_CHAR_DEAD drv[k]
        k2 = wp[k]
        IF k2 < fr_count
            GET_CAR_COORDINATES car[k] cx cy cz
            GET_DISTANCE_BETWEEN_COORDS_2D cx cy fr_x[k2] fr_y[k2] d
            IF d < 24.0
                wp[k] += 1
                k2 = wp[k]
                IF k2 < fr_count
                    GOSUB tr_drive
                ENDIF
            ELSE
                IF retask > 15
                    GOSUB tr_drive
                ENDIF
            ENDIF
        ENDIF
    ENDIF
    k += 1
ENDWHILE
IF retask > 15
    retask = 0
ENDIF
// Nasir shouts when you get close
IF taunted = 0
AND NOT IS_CAR_DEAD car[0]
    GET_CAR_COORDINATES car[0] cx cy cz
    GET_DISTANCE_BETWEEN_COORDS_2D px py cx cy d
    IF d < 45.0
        taunted = 1
        PRINT_NOW AFT_09 4000 1
    ENDIF
ENDIF
// the rear escort turns across the road half way up
IF blocked = 0
AND wp[0] > 14
    GOSUB tr_block
ENDIF
// Nasir's car is finished when it is wrecked or he is dead
IF IS_CAR_DEAD car[0]
OR IS_CHAR_DEAD drv[0]
    GOSUB tr_pass
    RETURN
ENDIF
k2 = fr_count
k2 -= 3
IF wp[0] >= k2
    PRINT_NOW AFT_11 5000 1
    PRINT_BIG M_FAIL 5000 1
    RETURN
ENDIF
GOTO tr_loop

// ---------------------------------------------------------------- helpers
tr_drive:
TASK_CAR_DRIVE_TO_COORD drv[k] car[k] fr_x[k2] fr_y[k2] fr_z[k2] 17.0 MODE_STRAIGHTLINE -1 3
RETURN

tr_spawn_convoy:
// Nasir at waypoint 3, a lead escort at 4, a rear escort at 2
CREATE_CAR COPCARRU fr_x[3] fr_y[3] fr_z[3] car[0]
SET_CAR_HEADING car[0] fr_h[3]
SET_CAR_HEALTH car[0] 2000
CREATE_CAR BOBCAT fr_x[4] fr_y[4] fr_z[4] car[1]
SET_CAR_HEADING car[1] fr_h[4]
CREATE_CAR BOBCAT fr_x[2] fr_y[2] fr_z[2] car[2]
SET_CAR_HEADING car[2] fr_h[2]
wp[0] = 4
wp[1] = 5
wp[2] = 3
CREATE_CHAR_INSIDE_CAR car[0] PEDTYPE_MISSION1 288 drv[0]
CREATE_CHAR_AS_PASSENGER car[0] PEDTYPE_MISSION1 283 0 gun[0]
CREATE_CHAR_INSIDE_CAR car[1] PEDTYPE_MISSION1 247 drv[1]
CREATE_CHAR_AS_PASSENGER car[1] PEDTYPE_MISSION1 248 0 gun[1]
CREATE_CHAR_INSIDE_CAR car[2] PEDTYPE_MISSION1 247 drv[2]
CREATE_CHAR_AS_PASSENGER car[2] PEDTYPE_MISSION1 248 0 gun[2]
SET_CHAR_MAX_HEALTH drv[0] 400
SET_CHAR_HEALTH drv[0] 400
SET_CHAR_CANT_BE_DRAGGED_OUT drv[0] TRUE
k = 0
WHILE k < 3
    GIVE_WEAPON_TO_CHAR drv[k] WEAPONTYPE_PISTOL 200
    GIVE_WEAPON_TO_CHAR gun[k] WEAPONTYPE_AK47 900
    SET_CHAR_ACCURACY gun[k] 35
    SET_CHAR_DROPS_WEAPONS_WHEN_DEAD drv[k] FALSE
    SET_CHAR_DROPS_WEAPONS_WHEN_DEAD gun[k] FALSE
    TASK_DRIVE_BY gun[k] scplayer -1 0.0 0.0 0.0 70.0 DRIVEBY_AI_ALL_DIRN TRUE 100
    k2 = wp[k]
    GOSUB tr_drive
    k += 1
ENDWHILE
ADD_BLIP_FOR_CAR car[0] nblip
CHANGE_BLIP_SCALE nblip 3
RETURN

tr_block:
blocked = 1
PRINT_NOW AFT_10 5000 1
k2 = wp[0]
k2 += 6
IF k2 >= fr_count
    k2 = fr_count
    k2 -= 1
ENDIF
bx = fr_x[k2]
by = fr_y[k2]
bz = fr_z[k2]
bh = fr_h[k2]
bh += 90.0
CREATE_CAR BOBCAT bx by bz car[3]
SET_CAR_HEADING car[3] bh
bx += 2.5
CREATE_CHAR PEDTYPE_MISSION1 247 bx by bz drv[3]
bx -= 5.0
CREATE_CHAR PEDTYPE_MISSION1 248 bx by bz gun[3]
GIVE_WEAPON_TO_CHAR drv[3] WEAPONTYPE_AK47 500
GIVE_WEAPON_TO_CHAR gun[3] WEAPONTYPE_ROCKETLAUNCHER 6
SET_CHAR_ACCURACY drv[3] 35
SET_CHAR_ACCURACY gun[3] 12
SET_CHAR_DROPS_WEAPONS_WHEN_DEAD drv[3] FALSE
SET_CHAR_DROPS_WEAPONS_WHEN_DEAD gun[3] FALSE
TASK_KILL_CHAR_ON_FOOT drv[3] scplayer
TASK_KILL_CHAR_ON_FOOT gun[3] scplayer
RETURN

tr_pass:
IF NOT nblip = -1
    REMOVE_BLIP nblip
    nblip = -1
ENDIF
WAIT 3000
PRINT_NOW AFT_12 7000 1
WAIT 4000
mission_progress = 23
ADD_SCORE player 7000
PRINT_WITH_NUMBER_BIG M_PASS 7000 5000 1
REGISTER_MISSION_PASSED AFT_T
PLAYER_MADE_PROGRESS 1
PLAY_MISSION_PASSED_TUNE 1
RETURN

tr_contact:
IF NOT DOES_CHAR_EXIST contact_ped[2]
    CREATE_CHAR PEDTYPE_CIVMALE 287 @base_major@ contact_ped[2]
    SET_CHAR_HEADING contact_ped[2] @base_major.h@
    SET_CHAR_PROOFS contact_ped[2] TRUE TRUE TRUE TRUE TRUE
    SET_CHAR_NEVER_TARGETTED contact_ped[2] TRUE
    SET_CHAR_STAY_IN_SAME_PLACE contact_ped[2] TRUE
ENDIF
RETURN

tr_cleanup:
IF NOT blip = -1
    REMOVE_BLIP blip
    blip = -1
ENDIF
IF NOT nblip = -1
    REMOVE_BLIP nblip
    nblip = -1
ENDIF
IF NOT cp = -1
    DELETE_CHECKPOINT cp
    cp = -1
ENDIF
k = 0
WHILE k < 4
    IF NOT drv[k] = -1
        MARK_CHAR_AS_NO_LONGER_NEEDED drv[k]
        drv[k] = -1
    ENDIF
    IF NOT gun[k] = -1
        MARK_CHAR_AS_NO_LONGER_NEEDED gun[k]
        gun[k] = -1
    ENDIF
    IF NOT car[k] = -1
        MARK_CAR_AS_NO_LONGER_NEEDED car[k]
        car[k] = -1
    ENDIF
    k += 1
ENDWHILE
IF NOT jeep = -1
    MARK_CAR_AS_NO_LONGER_NEEDED jeep
    jeep = -1
ENDIF
IF DOES_CHAR_EXIST contact_ped[2]
    CLEAR_CHAR_TASKS contact_ped[2]
ENDIF
MARK_MODEL_AS_NO_LONGER_NEEDED 287
MARK_MODEL_AS_NO_LONGER_NEEDED 288
MARK_MODEL_AS_NO_LONGER_NEEDED 283
MARK_MODEL_AS_NO_LONGER_NEEDED 247
MARK_MODEL_AS_NO_LONGER_NEEDED 248
MARK_MODEL_AS_NO_LONGER_NEEDED COPCARRU
MARK_MODEL_AS_NO_LONGER_NEEDED BOBCAT
MARK_MODEL_AS_NO_LONGER_NEEDED PATRIOT
RESTORE_CAMERA_JUMPCUT
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
flag_player_on_mission = 0
MISSION_HAS_FINISHED
RETURN
}
