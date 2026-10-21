// Mission 18 - Judas: Nasir sends you alone to meet his informer in the ruins of Sar-e-Kotal at night. It is a
// trap: the informer is Haji Gul's man and Nasir's own policemen close the road behind you. Fight your way
// out and get down to Darwaza, where the honest half of the police is waiting.
MISSION_START
{
LVAR_INT blip cp stage enemy[11] eblip[11] k alive car[2] cartimer chaser cdrv cgun chase_on retask
LVAR_FLOAT px py pz d
GOSUB jd_main
IF HAS_DEATHARREST_BEEN_EXECUTED
    PRINT_BIG M_FAIL 5000 1
ENDIF
GOSUB jd_cleanup
MISSION_END

jd_main:
SCRIPT_NAME AFM18
blip = -1
cp = -1
chaser = -1
cdrv = -1
cgun = -1
chase_on = 0
cartimer = 0
retask = 0
car[0] = -1
car[1] = -1
k = 0
WHILE k < 11
    enemy[k] = -1
    eblip[k] = -1
    k += 1
ENDWHILE
flag_player_on_mission = 1
REGISTER_MISSION_GIVEN
WAIT 0
REQUEST_MODEL 288
REQUEST_MODEL 283
REQUEST_MODEL 201
REQUEST_MODEL 247
REQUEST_MODEL 248
REQUEST_MODEL 162
REQUEST_MODEL 200
REQUEST_MODEL 355
REQUEST_MODEL 346
REQUEST_MODEL COPCARRU
LOAD_ALL_MODELS_NOW
GOSUB jd_contact

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
PRINT_BIG AFJ_T 3000 2
TASK_PLAY_ANIM contact_ped[1] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AFJ_01 7000 1
WAIT 7000
SET_FIXED_CAMERA_POSITION @cam_police_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_police_ct@ JUMP_CUT
PRINT_NOW AFJ_02 6000 1
WAIT 6000
SET_FIXED_CAMERA_POSITION @cam_police_p@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_police_pt@ JUMP_CUT
CLEAR_CHAR_TASKS contact_ped[1]
PRINT_NOW AFJ_06 3000 1
WAIT 3000
SET_FIXED_CAMERA_POSITION @cam_police_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_police_ct@ JUMP_CUT
TASK_PLAY_ANIM contact_ped[1] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AFJ_07 3500 1
WAIT 3500
CLEAR_CHAR_TASKS contact_ped[1]
DO_FADE 800 FADE_OUT
WAIT 900
SET_TIME_OF_DAY 21 0
RESTORE_CAMERA_JUMPCUT
SET_CAMERA_BEHIND_PLAYER
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
DO_FADE 800 FADE_IN
ADD_BLIP_FOR_COORD @jd_meet@ blip
CREATE_CHECKPOINT CHECKPOINT_TUBE @jd_meet@ @jd_meet@ 3.0 cp
PRINT_NOW AFJ_03 6000 1
stage = 0

// ---------------------------------------------------------------- Sar-e-Kotal
jd_loop:
WAIT 250
GET_CHAR_COORDINATES scplayer px py pz
IF stage = 0
    GET_DISTANCE_BETWEEN_COORDS_2D px py @jd_meet.x@ @jd_meet.y@ d
    IF d < 160.0
        stage = 1
        CREATE_CHAR PEDTYPE_CIVMALE 201 @jd_inf@ enemy[10]
        SET_CHAR_HEADING enemy[10] @jd_inf.h@
        SET_CHAR_PROOFS enemy[10] TRUE TRUE TRUE TRUE TRUE
        SET_CHAR_STAY_IN_SAME_PLACE enemy[10] TRUE
    ENDIF
    GOTO jd_loop
ENDIF
IF stage = 1
    IF LOCATE_CHAR_ON_FOOT_3D scplayer @jd_meet@ 2.0 2.0 2.0 TRUE
        GOSUB jd_trap
        stage = 2
    ENDIF
    GOTO jd_loop
ENDIF
IF stage = 2
    // the police pickups arrive, the crews bail out
    IF cartimer > 0
        cartimer -= 1
        IF cartimer = 0
            k = 6
            WHILE k < 10
                IF NOT IS_CHAR_DEAD enemy[k]
                    TASK_LEAVE_ANY_CAR enemy[k]
                ENDIF
                k += 1
            ENDWHILE
        ENDIF
    ENDIF
    retask += 1
    IF retask > 24
        retask = 0
        k = 6
        WHILE k < 10
            IF NOT IS_CHAR_DEAD enemy[k]
                IF NOT IS_CHAR_IN_ANY_CAR enemy[k]
                    TASK_KILL_CHAR_ON_FOOT enemy[k] scplayer
                ENDIF
            ENDIF
            k += 1
        ENDWHILE
    ENDIF
    alive = 0
    k = 0
    WHILE k < 11
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
        stage = 3
        PRINT_NOW AFJ_11 7000 1
        WAIT 7000
        PRINT_NOW AFJ_12 5000 1
        ADD_BLIP_FOR_COORD @checkpoint@ blip
        CREATE_CHECKPOINT CHECKPOINT_TUBE @checkpoint@ @checkpoint@ 8.0 cp
    ENDIF
    GOTO jd_loop
ENDIF
// stage 3: down the valley to Darwaza
GET_DISTANCE_BETWEEN_COORDS_2D px py @jd_meet.x@ @jd_meet.y@ d
IF chase_on = 0
AND d > 220.0
    chase_on = 1
    CREATE_CAR COPCARRU @jd_car1@ chaser
    SET_CAR_HEADING chaser @jd_car1.h@
    SET_CAR_HEALTH chaser 1500
    CREATE_CHAR_INSIDE_CAR chaser PEDTYPE_MISSION1 283 cdrv
    CREATE_CHAR_AS_PASSENGER chaser PEDTYPE_MISSION1 288 0 cgun
    GIVE_WEAPON_TO_CHAR cdrv WEAPONTYPE_PISTOL 200
    GIVE_WEAPON_TO_CHAR cgun WEAPONTYPE_AK47 900
    SET_CHAR_ACCURACY cgun 35
    SET_CHAR_DROPS_WEAPONS_WHEN_DEAD cdrv FALSE
    SET_CHAR_DROPS_WEAPONS_WHEN_DEAD cgun FALSE
    TASK_CAR_MISSION cdrv chaser -1 3 34.0 3
    TASK_DRIVE_BY cgun scplayer -1 0.0 0.0 0.0 60.0 DRIVEBY_AI_ALL_DIRN TRUE 100
    PRINT_NOW AFJ_13 4000 1
    retask = 0
ENDIF
IF chase_on = 1
    retask += 1
    IF retask > 40
        retask = 0
        IF NOT IS_CAR_DEAD chaser
        AND NOT IS_CHAR_DEAD cdrv
            TASK_CAR_MISSION cdrv chaser -1 3 34.0 3
        ENDIF
    ENDIF
ENDIF
IF LOCATE_CHAR_ANY_MEANS_3D scplayer @checkpoint@ 8.0 8.0 6.0 FALSE
    GOSUB jd_pass
    RETURN
ENDIF
GOTO jd_loop

// ---------------------------------------------------------------- the trap closes
jd_trap:
SET_PLAYER_CONTROL player OFF
SWITCH_WIDESCREEN ON
REMOVE_BLIP blip
blip = -1
DELETE_CHECKPOINT cp
cp = -1
SET_CHAR_COORDINATES scplayer @jd_meet@
SET_CHAR_HEADING scplayer @jd_meet.h@
SET_FIXED_CAMERA_POSITION @cam_jd@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_jd_t@ JUMP_CUT
TASK_PLAY_ANIM enemy[10] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AFJ_08 6000 1
WAIT 4500
// headlights on the valley road
CREATE_CAR COPCARRU @jd_car0@ car[0]
SET_CAR_HEADING car[0] @jd_car0.h@
CREATE_CAR COPCARRU @jd_car1@ car[1]
SET_CAR_HEADING car[1] @jd_car1.h@
CREATE_CHAR_INSIDE_CAR car[0] PEDTYPE_MISSION1 283 enemy[6]
CREATE_CHAR_AS_PASSENGER car[0] PEDTYPE_MISSION1 288 0 enemy[7]
CREATE_CHAR_INSIDE_CAR car[1] PEDTYPE_MISSION1 288 enemy[8]
CREATE_CHAR_AS_PASSENGER car[1] PEDTYPE_MISSION1 283 0 enemy[9]
CREATE_CHAR PEDTYPE_MISSION1 247 @jd_e0@ enemy[0]
CREATE_CHAR PEDTYPE_MISSION1 248 @jd_e1@ enemy[1]
CREATE_CHAR PEDTYPE_MISSION1 162 @jd_e2@ enemy[2]
CREATE_CHAR PEDTYPE_MISSION1 200 @jd_e3@ enemy[3]
CREATE_CHAR PEDTYPE_MISSION1 247 @jd_e4@ enemy[4]
CREATE_CHAR PEDTYPE_MISSION1 248 @jd_e5@ enemy[5]
WAIT 1500
PRINT_NOW AFJ_09 6000 1
WAIT 6000
CLEAR_CHAR_TASKS enemy[10]
SET_CHAR_PROOFS enemy[10] FALSE FALSE FALSE FALSE FALSE
SET_CHAR_STAY_IN_SAME_PLACE enemy[10] FALSE
// the informer turns PEDTYPE_MISSION1 by behaviour: he simply attacks
GIVE_WEAPON_TO_CHAR enemy[10] WEAPONTYPE_PISTOL 100
RESTORE_CAMERA_JUMPCUT
SET_CAMERA_BEHIND_PLAYER
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
PRINT_NOW AFJ_10 6000 1
k = 0
WHILE k < 11
    IF k < 10
        GIVE_WEAPON_TO_CHAR enemy[k] WEAPONTYPE_AK47 500
    ENDIF
    SET_CHAR_ACCURACY enemy[k] 35
    SET_CHAR_DROPS_WEAPONS_WHEN_DEAD enemy[k] FALSE
    ADD_BLIP_FOR_CHAR enemy[k] eblip[k]
    k += 1
ENDWHILE
k = 0
WHILE k < 6
    TASK_KILL_CHAR_ON_FOOT enemy[k] scplayer
    k += 1
ENDWHILE
TASK_KILL_CHAR_ON_FOOT enemy[10] scplayer
TASK_CAR_DRIVE_TO_COORD enemy[6] car[0] @jd_meet@ 18.0 MODE_STRAIGHTLINE -1 3
TASK_CAR_DRIVE_TO_COORD enemy[8] car[1] @jd_meet@ 18.0 MODE_STRAIGHTLINE -1 3
cartimer = 36
mission_counter = 11
DISPLAY_ONSCREEN_COUNTER_WITH_STRING mission_counter COUNTER_DISPLAY_NUMBER AFJ_CT
RETURN

// ---------------------------------------------------------------- Darwaza
jd_pass:
SET_PLAYER_CONTROL player OFF
SWITCH_WIDESCREEN ON
REMOVE_BLIP blip
blip = -1
DELETE_CHECKPOINT cp
cp = -1
IF NOT chaser = -1
    IF NOT IS_CAR_DEAD chaser
        EXPLODE_CAR chaser
    ENDIF
ENDIF
WAIT 1500
PRINT_NOW AFJ_14 6500 1
WAIT 6500
PRINT_NOW AFJ_15 6000 1
WAIT 6000
RESTORE_CAMERA_JUMPCUT
SET_CAMERA_BEHIND_PLAYER
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
// Nasir is gone from the station for good (main script stops spawning him)
IF DOES_CHAR_EXIST contact_ped[1]
    DELETE_CHAR contact_ped[1]
    contact_ped[1] = -1
ENDIF
mission_progress = 18
ADD_SCORE player 4000
PRINT_WITH_NUMBER_BIG M_PASS 4000 5000 1
REGISTER_MISSION_PASSED AFJ_T
PLAYER_MADE_PROGRESS 1
PLAY_MISSION_PASSED_TUNE 1
RETURN

jd_contact:
IF NOT DOES_CHAR_EXIST contact_ped[1]
    CREATE_CHAR PEDTYPE_CIVMALE 288 @police_nasir@ contact_ped[1]
    SET_CHAR_HEADING contact_ped[1] @police_nasir.h@
    SET_CHAR_PROOFS contact_ped[1] TRUE TRUE TRUE TRUE TRUE
    SET_CHAR_NEVER_TARGETTED contact_ped[1] TRUE
    SET_CHAR_STAY_IN_SAME_PLACE contact_ped[1] TRUE
ENDIF
RETURN

jd_cleanup:
CLEAR_ONSCREEN_COUNTER mission_counter
IF NOT blip = -1
    REMOVE_BLIP blip
    blip = -1
ENDIF
IF NOT cp = -1
    DELETE_CHECKPOINT cp
    cp = -1
ENDIF
k = 0
WHILE k < 11
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
    IF NOT car[k] = -1
        MARK_CAR_AS_NO_LONGER_NEEDED car[k]
        car[k] = -1
    ENDIF
    k += 1
ENDWHILE
IF NOT cdrv = -1
    MARK_CHAR_AS_NO_LONGER_NEEDED cdrv
    cdrv = -1
ENDIF
IF NOT cgun = -1
    MARK_CHAR_AS_NO_LONGER_NEEDED cgun
    cgun = -1
ENDIF
IF NOT chaser = -1
    MARK_CAR_AS_NO_LONGER_NEEDED chaser
    chaser = -1
ENDIF
IF DOES_CHAR_EXIST contact_ped[1]
    CLEAR_CHAR_TASKS contact_ped[1]
ENDIF
MARK_MODEL_AS_NO_LONGER_NEEDED 288
MARK_MODEL_AS_NO_LONGER_NEEDED 283
MARK_MODEL_AS_NO_LONGER_NEEDED 201
MARK_MODEL_AS_NO_LONGER_NEEDED 247
MARK_MODEL_AS_NO_LONGER_NEEDED 248
MARK_MODEL_AS_NO_LONGER_NEEDED 162
MARK_MODEL_AS_NO_LONGER_NEEDED 200
MARK_MODEL_AS_NO_LONGER_NEEDED COPCARRU
RESTORE_CAMERA_JUMPCUT
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
flag_player_on_mission = 0
MISSION_HAS_FINISHED
RETURN
}
