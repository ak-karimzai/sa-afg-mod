// Mission 15 - Courier: Haji Gul's money car sits at the Deh-e-Bagh roadside with four guards. When they see you
// the driver runs for the mountain road. Stop the sedan without wrecking it, take it, and bring it to the police
// station while Haji Gul's pickups try to get it back.
MISSION_START
{
LVAR_INT blip cp stage enemy[5] eblip[5] k alive car cblip wp k2 retask chaser[2] cdrv[2] cgun[2] chase_on health
LVAR_FLOAT px py pz d cx cy cz
GOSUB co_main
IF HAS_DEATHARREST_BEEN_EXECUTED
    PRINT_BIG M_FAIL 5000 1
ENDIF
GOSUB co_cleanup
MISSION_END

co_main:
SCRIPT_NAME AFM15
blip = -1
cp = -1
car = -1
cblip = -1
wp = 0
retask = 0
chase_on = 0
k = 0
WHILE k < 5
    enemy[k] = -1
    eblip[k] = -1
    k += 1
ENDWHILE
k = 0
WHILE k < 2
    chaser[k] = -1
    cdrv[k] = -1
    cgun[k] = -1
    k += 1
ENDWHILE
flag_player_on_mission = 1
REGISTER_MISSION_GIVEN
WAIT 0
REQUEST_MODEL 288
REQUEST_MODEL 247
REQUEST_MODEL 248
REQUEST_MODEL 162
REQUEST_MODEL 200
REQUEST_MODEL 355
REQUEST_MODEL SENTINEL
REQUEST_MODEL BOBCAT
LOAD_ALL_MODELS_NOW
GOSUB co_contact

// ---------------------------------------------------------------- Nasir at the station
DO_FADE 400 FADE_OUT
WAIT 450
SET_PLAYER_CONTROL player OFF
SWITCH_WIDESCREEN ON
SET_CHAR_COORDINATES scplayer @talk_police@
SET_CHAR_HEADING scplayer @talk_police.h@
SET_CHAR_COORDINATES contact_ped[1] @police_nasir@
SET_CHAR_HEADING contact_ped[1] @police_nasir.h@
WAIT 600
SET_FIXED_CAMERA_POSITION @cam_police_w@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_police_wt@ JUMP_CUT
DO_FADE 400 FADE_IN
PRINT_BIG AFC_T 3000 2
TASK_PLAY_ANIM contact_ped[1] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AFC_01 6500 1
WAIT 6500
SET_FIXED_CAMERA_POSITION @cam_police_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_police_ct@ JUMP_CUT
PRINT_NOW AFC_02 6000 1
WAIT 6000
SET_FIXED_CAMERA_POSITION @cam_police_p@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_police_pt@ JUMP_CUT
CLEAR_CHAR_TASKS contact_ped[1]
PRINT_NOW AFC_06 2500 1
WAIT 2500
SET_FIXED_CAMERA_POSITION @cam_police_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_police_ct@ JUMP_CUT
TASK_PLAY_ANIM contact_ped[1] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AFC_07 3500 1
WAIT 3500
CLEAR_CHAR_TASKS contact_ped[1]
SET_TIME_OF_DAY 11 0
RESTORE_CAMERA_JUMPCUT
SET_CAMERA_BEHIND_PLAYER
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
ADD_BLIP_FOR_COORD @co_car@ blip
CREATE_CHECKPOINT CHECKPOINT_TUBE @co_car@ @co_car@ 8.0 cp
PRINT_NOW AFC_03 6000 1
stage = 0

// ---------------------------------------------------------------- Deh-e-Bagh
co_loop:
WAIT 250
GET_CHAR_COORDINATES scplayer px py pz
IF stage = 0
    GET_DISTANCE_BETWEEN_COORDS_2D px py @co_car.x@ @co_car.y@ d
    IF d < 180.0
        stage = 1
        REMOVE_BLIP blip
        blip = -1
        DELETE_CHECKPOINT cp
        cp = -1
        CREATE_CAR SENTINEL @co_car@ car
        SET_CAR_HEADING car @co_car.h@
        SET_CAR_HEALTH car 1200
        CREATE_CHAR_INSIDE_CAR car PEDTYPE_MISSION1 200 enemy[4]
        CREATE_CHAR PEDTYPE_MISSION1 247 @co_g0@ enemy[0]
        CREATE_CHAR PEDTYPE_MISSION1 248 @co_g1@ enemy[1]
        CREATE_CHAR PEDTYPE_MISSION1 162 @co_g2@ enemy[2]
        CREATE_CHAR PEDTYPE_MISSION1 247 @co_g3@ enemy[3]
        SET_CHAR_HEADING enemy[0] @co_g0.h@
        SET_CHAR_HEADING enemy[1] @co_g1.h@
        SET_CHAR_HEADING enemy[2] @co_g2.h@
        SET_CHAR_HEADING enemy[3] @co_g3.h@
        k = 0
        WHILE k < 5
            GIVE_WEAPON_TO_CHAR enemy[k] WEAPONTYPE_AK47 500
            SET_CHAR_ACCURACY enemy[k] 35
            SET_CHAR_DROPS_WEAPONS_WHEN_DEAD enemy[k] FALSE
            SET_CHAR_STAY_IN_SAME_PLACE enemy[k] TRUE
            ADD_BLIP_FOR_CHAR enemy[k] eblip[k]
            k += 1
        ENDWHILE
        GIVE_WEAPON_TO_CHAR enemy[4] WEAPONTYPE_PISTOL 100
        SET_CHAR_CANT_BE_DRAGGED_OUT enemy[4] TRUE
        ADD_BLIP_FOR_CAR car cblip
        CHANGE_BLIP_SCALE cblip 3
        mission_bar = 100
        DISPLAY_ONSCREEN_COUNTER_WITH_STRING mission_bar COUNTER_DISPLAY_BAR AFC_HB
    ENDIF
    GOTO co_loop
ENDIF
// the sedan must survive, whatever stage we are in
IF IS_CAR_DEAD car
    CLEAR_ONSCREEN_COUNTER mission_bar
    PRINT_NOW AFC_12 5000 1
    PRINT_BIG M_FAIL 5000 1
    RETURN
ENDIF
GET_CAR_HEALTH car health
health -= 200
IF health < 0
    health = 0
ENDIF
mission_bar = health
mission_bar /= 10
IF stage = 1
    GET_DISTANCE_BETWEEN_COORDS_2D px py @co_car.x@ @co_car.y@ d
    IF d < 38.0
        stage = 2
    ENDIF
    IF d < 200.0
    AND IS_CHAR_SHOOTING scplayer
        stage = 2
    ENDIF
    k = 0
    WHILE k < 5
        IF HAS_CHAR_BEEN_DAMAGED_BY_CHAR enemy[k] scplayer
            stage = 2
        ENDIF
        k += 1
    ENDWHILE
    IF stage = 2
        GOSUB co_alarm
    ENDIF
    GOTO co_loop
ENDIF
IF stage = 2
    // the courier runs for the mountain road
    IF IS_CHAR_IN_CAR scplayer car
        stage = 4
        GOSUB co_taken
        GOTO co_loop
    ENDIF
    IF IS_CHAR_DEAD enemy[4]
        stage = 3
        PRINT_NOW AFC_09 5000 1
        GOTO co_loop
    ENDIF
    IF NOT IS_CHAR_IN_CAR enemy[4] car
        // thrown out or bailed: he gives up on the car
        stage = 3
        TASK_KILL_CHAR_ON_FOOT enemy[4] scplayer
        PRINT_NOW AFC_09 5000 1
        GOTO co_loop
    ENDIF
    retask += 1
    GET_CAR_COORDINATES car cx cy cz
    k2 = wp
    GET_DISTANCE_BETWEEN_COORDS_2D cx cy fr_x[k2] fr_y[k2] d
    IF d < 24.0
        wp += 1
        k2 = wp
        IF k2 < fr_count
            GOSUB co_drive
        ENDIF
    ELSE
        IF retask > 15
            retask = 0
            GOSUB co_drive
        ENDIF
    ENDIF
    IF wp >= 20
        CLEAR_ONSCREEN_COUNTER mission_bar
        PRINT_NOW AFC_13 5000 1
        PRINT_BIG M_FAIL 5000 1
        RETURN
    ENDIF
    GOTO co_loop
ENDIF
IF stage = 3
    IF IS_CHAR_IN_CAR scplayer car
        stage = 4
        GOSUB co_taken
    ENDIF
    GOTO co_loop
ENDIF
// stage 4: bring it home
GOSUB co_chasers
IF IS_CHAR_IN_CAR scplayer car
AND LOCATE_CHAR_IN_CAR_3D scplayer @co_drop@ 6.0 6.0 5.0 FALSE
    GOSUB co_pass
    RETURN
ENDIF
GOTO co_loop

// ---------------------------------------------------------------- helpers
co_drive:
TASK_CAR_DRIVE_TO_COORD enemy[4] car fr_x[k2] fr_y[k2] fr_z[k2] 15.0 MODE_STRAIGHTLINE -1 3
RETURN

co_alarm:
PRINT_NOW AFC_08 6000 1
k = 0
WHILE k < 4
    IF NOT IS_CHAR_DEAD enemy[k]
        SET_CHAR_STAY_IN_SAME_PLACE enemy[k] FALSE
        TASK_KILL_CHAR_ON_FOOT enemy[k] scplayer
    ENDIF
    k += 1
ENDWHILE
IF NOT IS_CHAR_DEAD enemy[4]
    SET_CHAR_STAY_IN_SAME_PLACE enemy[4] FALSE
    wp = 0
    k2 = 0
    retask = 0
    GOSUB co_drive
ENDIF
RETURN

co_taken:
IF NOT cblip = -1
    REMOVE_BLIP cblip
    cblip = -1
ENDIF
PRINT_NOW AFC_10 6000 1
ADD_BLIP_FOR_COORD @co_drop@ blip
CREATE_CHECKPOINT CHECKPOINT_TUBE @co_drop@ @co_drop@ 6.0 cp
// the remaining guards keep coming
k = 0
WHILE k < 5
    IF NOT IS_CHAR_DEAD enemy[k]
        SET_CHAR_STAY_IN_SAME_PLACE enemy[k] FALSE
        TASK_KILL_CHAR_ON_FOOT enemy[k] scplayer
    ENDIF
    k += 1
ENDWHILE
retask = 0
RETURN

co_chasers:
IF chase_on = 0
    GET_DISTANCE_BETWEEN_COORDS_2D px py @co_chase0.x@ @co_chase0.y@ d
    IF d < 260.0
        chase_on = 1
        PRINT_NOW AFC_11 5000 1
        CREATE_CAR BOBCAT @co_chase0@ chaser[0]
        SET_CAR_HEADING chaser[0] @co_chase0.h@
        CREATE_CAR BOBCAT @co_chase1@ chaser[1]
        SET_CAR_HEADING chaser[1] @co_chase1.h@
        k = 0
        WHILE k < 2
            CREATE_CHAR_INSIDE_CAR chaser[k] PEDTYPE_MISSION1 247 cdrv[k]
            CREATE_CHAR_AS_PASSENGER chaser[k] PEDTYPE_MISSION1 248 0 cgun[k]
            GIVE_WEAPON_TO_CHAR cdrv[k] WEAPONTYPE_PISTOL 200
            GIVE_WEAPON_TO_CHAR cgun[k] WEAPONTYPE_MICRO_UZI 900
            SET_CHAR_ACCURACY cgun[k] 30
            SET_CHAR_DROPS_WEAPONS_WHEN_DEAD cdrv[k] FALSE
            SET_CHAR_DROPS_WEAPONS_WHEN_DEAD cgun[k] FALSE
            TASK_CAR_MISSION cdrv[k] chaser[k] -1 3 30.0 3
            TASK_DRIVE_BY cgun[k] scplayer -1 0.0 0.0 0.0 60.0 DRIVEBY_AI_ALL_DIRN TRUE 100
            k += 1
        ENDWHILE
    ENDIF
    RETURN
ENDIF
IF chase_on = 1
    retask += 1
    IF retask > 40
        retask = 0
        k = 0
        WHILE k < 2
            IF NOT IS_CAR_DEAD chaser[k]
            AND NOT IS_CHAR_DEAD cdrv[k]
                TASK_CAR_MISSION cdrv[k] chaser[k] -1 3 30.0 3
            ENDIF
            k += 1
        ENDWHILE
    ENDIF
ENDIF
RETURN

// ---------------------------------------------------------------- the station
co_pass:
CLEAR_ONSCREEN_COUNTER mission_bar
SET_PLAYER_CONTROL player OFF
SWITCH_WIDESCREEN ON
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
WAIT 800
SET_FIXED_CAMERA_POSITION @cam_police_w@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_police_wt@ JUMP_CUT
IF DOES_CHAR_EXIST contact_ped[1]
    TASK_PLAY_ANIM contact_ped[1] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
ENDIF
PRINT_NOW AFC_14 6000 1
WAIT 6000
PRINT_NOW AFC_15 4500 1
WAIT 4500
IF DOES_CHAR_EXIST contact_ped[1]
    CLEAR_CHAR_TASKS contact_ped[1]
ENDIF
RESTORE_CAMERA_JUMPCUT
SET_CAMERA_BEHIND_PLAYER
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
mission_progress = 15
ADD_SCORE player 3000
PRINT_WITH_NUMBER_BIG M_PASS 3000 5000 1
REGISTER_MISSION_PASSED AFC_T
PLAYER_MADE_PROGRESS 1
PLAY_MISSION_PASSED_TUNE 1
RETURN

co_contact:
IF NOT DOES_CHAR_EXIST contact_ped[1]
    CREATE_CHAR PEDTYPE_CIVMALE 288 @police_nasir@ contact_ped[1]
    SET_CHAR_HEADING contact_ped[1] @police_nasir.h@
    SET_CHAR_PROOFS contact_ped[1] TRUE TRUE TRUE TRUE TRUE
    SET_CHAR_NEVER_TARGETTED contact_ped[1] TRUE
    SET_CHAR_STAY_IN_SAME_PLACE contact_ped[1] TRUE
ENDIF
RETURN

co_cleanup:
CLEAR_ONSCREEN_COUNTER mission_bar
IF NOT blip = -1
    REMOVE_BLIP blip
    blip = -1
ENDIF
IF NOT cblip = -1
    REMOVE_BLIP cblip
    cblip = -1
ENDIF
IF NOT cp = -1
    DELETE_CHECKPOINT cp
    cp = -1
ENDIF
k = 0
WHILE k < 5
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
    k += 1
ENDWHILE
IF NOT car = -1
    MARK_CAR_AS_NO_LONGER_NEEDED car
    car = -1
ENDIF
IF DOES_CHAR_EXIST contact_ped[1]
    CLEAR_CHAR_TASKS contact_ped[1]
ENDIF
MARK_MODEL_AS_NO_LONGER_NEEDED 288
MARK_MODEL_AS_NO_LONGER_NEEDED 247
MARK_MODEL_AS_NO_LONGER_NEEDED 248
MARK_MODEL_AS_NO_LONGER_NEEDED 162
MARK_MODEL_AS_NO_LONGER_NEEDED 200
MARK_MODEL_AS_NO_LONGER_NEEDED SENTINEL
MARK_MODEL_AS_NO_LONGER_NEEDED BOBCAT
RESTORE_CAMERA_JUMPCUT
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
flag_player_on_mission = 0
MISSION_HAS_FINISHED
RETURN
}
