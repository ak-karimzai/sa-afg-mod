// Mission 9 - The Pass: a Black Stone convoy (three trucks, two escort pickups) climbs the mountain road to the
// fortress at night. Take position above the switchbacks with the RPG and destroy every truck before it gets past.
MISSION_START
{
LVAR_INT blip cp stage car[5] drv[5] gun[2] wp[5] tblip[3] k k2 dead lead retask escort_out cartimer health
LVAR_FLOAT px py pz d cx cy cz
GOSUB ps_main
IF HAS_DEATHARREST_BEEN_EXECUTED
    PRINT_BIG M_FAIL 5000 1
ENDIF
GOSUB ps_cleanup
MISSION_END

ps_main:
SCRIPT_NAME AFM9
blip = -1
cp = -1
escort_out = 0
cartimer = 0
retask = 0
k = 0
WHILE k < 5
    car[k] = -1
    drv[k] = -1
    wp[k] = 0
    k += 1
ENDWHILE
gun[0] = -1
gun[1] = -1
k = 0
WHILE k < 3
    tblip[k] = -1
    k += 1
ENDWHILE
flag_player_on_mission = 1
REGISTER_MISSION_GIVEN
WAIT 0
REQUEST_MODEL 287
REQUEST_MODEL 247
REQUEST_MODEL 248
REQUEST_MODEL 162
REQUEST_MODEL 355
REQUEST_MODEL 359
REQUEST_MODEL 358
REQUEST_MODEL YANKEE
REQUEST_MODEL BOBCAT
LOAD_ALL_MODELS_NOW
GOSUB ps_contact

// ---------------------------------------------------------------- briefing at HQ
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
PRINT_BIG AFP_T 3000 2
TASK_PLAY_ANIM contact_ped[2] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AFP_01 6500 1
WAIT 6500
SET_FIXED_CAMERA_POSITION @cam_base_hq_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_ct@ JUMP_CUT
PRINT_NOW AFP_02 6500 1
WAIT 6500
SET_FIXED_CAMERA_POSITION @cam_base_hq_p@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_pt@ JUMP_CUT
CLEAR_CHAR_TASKS contact_ped[2]
PRINT_NOW AFP_06 3500 1
WAIT 3500
SET_FIXED_CAMERA_POSITION @cam_base_hq_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_ct@ JUMP_CUT
TASK_PLAY_ANIM contact_ped[2] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AFP_07 6000 1
WAIT 6000
CLEAR_CHAR_TASKS contact_ped[2]
GIVE_WEAPON_TO_CHAR scplayer WEAPONTYPE_ROCKETLAUNCHER 8
GIVE_WEAPON_TO_CHAR scplayer WEAPONTYPE_SNIPERRIFLE 20
DO_FADE 800 FADE_OUT
WAIT 900
SET_TIME_OF_DAY 20 0
RESTORE_CAMERA_JUMPCUT
SET_CAMERA_BEHIND_PLAYER
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
DO_FADE 800 FADE_IN
ADD_BLIP_FOR_COORD @ps_amb@ blip
CREATE_CHECKPOINT CHECKPOINT_TUBE @ps_amb@ @ps_amb@ 6.0 cp
PRINT_NOW AFP_03 6000 1
stage = 0

// ---------------------------------------------------------------- the ambush
ps_loop:
WAIT 250
GET_CHAR_COORDINATES scplayer px py pz
IF stage = 0
    IF LOCATE_CHAR_ANY_MEANS_3D scplayer @ps_amb@ 14.0 14.0 10.0 FALSE
        stage = 1
        REMOVE_BLIP blip
        blip = -1
        DELETE_CHECKPOINT cp
        cp = -1
        PRINT_NOW AFP_08 6000 1
        WAIT 3000
        GOSUB ps_spawn_convoy
        PRINT_NOW AFP_09 5000 1
    ENDIF
    GOTO ps_loop
ENDIF
// drive the convoy along the road
retask += 1
k = 0
WHILE k < 5
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
                    GOSUB ps_drive
                ENDIF
            ELSE
                IF retask > 15
                    GOSUB ps_drive
                ENDIF
            ENDIF
        ENDIF
    ENDIF
    k += 1
ENDWHILE
IF retask > 15
    retask = 0
ENDIF
// the escort dismounts when the first truck is hit
IF escort_out = 0
    k = 1
    WHILE k < 4
        IF IS_CAR_DEAD car[k]
            escort_out = 1
        ELSE
            GET_CAR_HEALTH car[k] health
            IF health < 1350
                escort_out = 1
            ENDIF
        ENDIF
        k += 1
    ENDWHILE
    IF escort_out = 1
        PRINT_NOW AFP_10 5000 1
        IF NOT IS_CHAR_DEAD drv[0]
            TASK_LEAVE_ANY_CAR drv[0]
        ENDIF
        IF NOT IS_CHAR_DEAD drv[4]
            TASK_LEAVE_ANY_CAR drv[4]
        ENDIF
        IF NOT IS_CHAR_DEAD gun[0]
            TASK_LEAVE_ANY_CAR gun[0]
        ENDIF
        IF NOT IS_CHAR_DEAD gun[1]
            TASK_LEAVE_ANY_CAR gun[1]
        ENDIF
        cartimer = 10
    ENDIF
ENDIF
IF cartimer > 0
    cartimer -= 1
    IF cartimer = 0
        IF NOT IS_CHAR_DEAD drv[0]
            TASK_KILL_CHAR_ON_FOOT drv[0] scplayer
        ENDIF
        IF NOT IS_CHAR_DEAD drv[4]
            TASK_KILL_CHAR_ON_FOOT drv[4] scplayer
        ENDIF
        IF NOT IS_CHAR_DEAD gun[0]
            TASK_KILL_CHAR_ON_FOOT gun[0] scplayer
        ENDIF
        IF NOT IS_CHAR_DEAD gun[1]
            TASK_KILL_CHAR_ON_FOOT gun[1] scplayer
        ENDIF
    ENDIF
ENDIF
// trucks: dead count, blips, and the one that got away
dead = 0
lead = 0
k = 1
WHILE k < 4
    IF IS_CAR_DEAD car[k]
        dead += 1
        IF NOT tblip[k] = -1
            REMOVE_BLIP tblip[k]
            tblip[k] = -1
        ENDIF
    ELSE
        IF wp[k] > lead
            lead = wp[k]
        ENDIF
    ENDIF
    k += 1
ENDWHILE
// careful: tblip is indexed 0..2 for trucks 1..3
k2 = dead
mission_counter = 3
mission_counter -= k2
IF lead >= 29
    PRINT_NOW AFP_11 5000 1
    PRINT_BIG M_FAIL 5000 1
    RETURN
ENDIF
IF dead = 3
    CLEAR_ONSCREEN_COUNTER mission_counter
    WAIT 2500
    PRINT_NOW AFP_12 6000 1
    WAIT 3000
    mission_progress = 9
    ADD_SCORE player 4000
    PRINT_WITH_NUMBER_BIG M_PASS 4000 5000 1
    REGISTER_MISSION_PASSED AFP_T
    PLAYER_MADE_PROGRESS 1
    PLAY_MISSION_PASSED_TUNE 1
    RETURN
ENDIF
GOTO ps_loop

// ---------------------------------------------------------------- helpers
ps_drive:
// drive car k straight at waypoint k2 (MODE_STRAIGHTLINE needs no road nodes); 3 = plough through
TASK_CAR_DRIVE_TO_COORD drv[k] car[k] fr_x[k2] fr_y[k2] fr_z[k2] 14.0 MODE_STRAIGHTLINE -1 3
RETURN

ps_spawn_convoy:
// lead escort at waypoint 7, three trucks behind it, rear escort at waypoint 3
CREATE_CAR BOBCAT fr_x[7] fr_y[7] fr_z[7] car[0]
SET_CAR_HEADING car[0] fr_h[7]
CREATE_CAR YANKEE fr_x[6] fr_y[6] fr_z[6] car[1]
SET_CAR_HEADING car[1] fr_h[6]
CREATE_CAR YANKEE fr_x[5] fr_y[5] fr_z[5] car[2]
SET_CAR_HEADING car[2] fr_h[5]
CREATE_CAR YANKEE fr_x[4] fr_y[4] fr_z[4] car[3]
SET_CAR_HEADING car[3] fr_h[4]
CREATE_CAR BOBCAT fr_x[3] fr_y[3] fr_z[3] car[4]
SET_CAR_HEADING car[4] fr_h[3]
wp[0] = 8
wp[1] = 7
wp[2] = 6
wp[3] = 5
wp[4] = 4
k = 0
WHILE k < 5
    CREATE_CHAR_INSIDE_CAR car[k] PEDTYPE_MISSION1 247 drv[k]
    GIVE_WEAPON_TO_CHAR drv[k] WEAPONTYPE_AK47 500
    SET_CHAR_ACCURACY drv[k] 40
    SET_CHAR_DROPS_WEAPONS_WHEN_DEAD drv[k] FALSE
    SET_CAR_HEALTH car[k] 1400
    k2 = wp[k]
    GOSUB ps_drive
    k += 1
ENDWHILE
CREATE_CHAR_AS_PASSENGER car[0] PEDTYPE_MISSION1 248 0 gun[0]
CREATE_CHAR_AS_PASSENGER car[4] PEDTYPE_MISSION1 162 0 gun[1]
GIVE_WEAPON_TO_CHAR gun[0] WEAPONTYPE_AK47 500
GIVE_WEAPON_TO_CHAR gun[1] WEAPONTYPE_ROCKETLAUNCHER 5
SET_CHAR_ACCURACY gun[0] 40
SET_CHAR_ACCURACY gun[1] 15
SET_CHAR_DROPS_WEAPONS_WHEN_DEAD gun[0] FALSE
SET_CHAR_DROPS_WEAPONS_WHEN_DEAD gun[1] FALSE
k = 1
WHILE k < 4
    k2 = k
    k2 -= 1
    ADD_BLIP_FOR_CAR car[k] tblip[k2]
    k += 1
ENDWHILE
mission_counter = 3
DISPLAY_ONSCREEN_COUNTER_WITH_STRING mission_counter COUNTER_DISPLAY_NUMBER AFP_CT
RETURN

ps_contact:
IF NOT DOES_CHAR_EXIST contact_ped[2]
    CREATE_CHAR PEDTYPE_CIVMALE 287 @base_major@ contact_ped[2]
    SET_CHAR_HEADING contact_ped[2] @base_major.h@
    SET_CHAR_PROOFS contact_ped[2] TRUE TRUE TRUE TRUE TRUE
    SET_CHAR_NEVER_TARGETTED contact_ped[2] TRUE
    SET_CHAR_STAY_IN_SAME_PLACE contact_ped[2] TRUE
ENDIF
RETURN

ps_cleanup:
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
WHILE k < 3
    IF NOT tblip[k] = -1
        REMOVE_BLIP tblip[k]
        tblip[k] = -1
    ENDIF
    k += 1
ENDWHILE
k = 0
WHILE k < 5
    IF NOT drv[k] = -1
        MARK_CHAR_AS_NO_LONGER_NEEDED drv[k]
        drv[k] = -1
    ENDIF
    IF NOT car[k] = -1
        MARK_CAR_AS_NO_LONGER_NEEDED car[k]
        car[k] = -1
    ENDIF
    k += 1
ENDWHILE
k = 0
WHILE k < 2
    IF NOT gun[k] = -1
        MARK_CHAR_AS_NO_LONGER_NEEDED gun[k]
        gun[k] = -1
    ENDIF
    k += 1
ENDWHILE
IF DOES_CHAR_EXIST contact_ped[2]
    CLEAR_CHAR_TASKS contact_ped[2]
ENDIF
MARK_MODEL_AS_NO_LONGER_NEEDED 287
MARK_MODEL_AS_NO_LONGER_NEEDED 247
MARK_MODEL_AS_NO_LONGER_NEEDED 248
MARK_MODEL_AS_NO_LONGER_NEEDED 162
MARK_MODEL_AS_NO_LONGER_NEEDED YANKEE
MARK_MODEL_AS_NO_LONGER_NEEDED BOBCAT
RESTORE_CAMERA_JUMPCUT
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
flag_player_on_mission = 0
MISSION_HAS_FINISHED
RETURN
}
