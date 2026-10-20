// Mission 12 - Vulture: the Black Stone remnants flee down the Ghorband valley in five pickups. Take the Hunter
// and destroy them before they cross Darwaza. Epilogue: tea with Rahim at sunrise.
MISSION_START
{
LVAR_INT heli blip stage car[5] drv[5] gun[5] wp[5] cblip[5] k k2 dead retask rahim away
LVAR_FLOAT px py pz d cx cy cz
GOSUB vt_main
IF HAS_DEATHARREST_BEEN_EXECUTED
    PRINT_BIG M_FAIL 5000 1
ENDIF
GOSUB vt_cleanup
MISSION_END

vt_main:
SCRIPT_NAME AFM12
heli = -1
blip = -1
rahim = -1
retask = 0
away = 0
k = 0
WHILE k < 5
    car[k] = -1
    drv[k] = -1
    gun[k] = -1
    cblip[k] = -1
    wp[k] = 0
    k += 1
ENDWHILE
flag_player_on_mission = 1
REGISTER_MISSION_GIVEN
WAIT 0
REQUEST_MODEL 287
REQUEST_MODEL 158
REQUEST_MODEL 247
REQUEST_MODEL 248
REQUEST_MODEL 355
REQUEST_MODEL HUNTER
REQUEST_MODEL BOBCAT
LOAD_ALL_MODELS_NOW
GOSUB vt_contact
CREATE_CAR HUNTER @hunter@ heli
SET_CAR_HEADING heli @hunter.h@

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
PRINT_BIG AFV_T 3000 2
TASK_PLAY_ANIM contact_ped[2] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AFV_01 7000 1
WAIT 7000
SET_FIXED_CAMERA_POSITION @cam_base_hq_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_ct@ JUMP_CUT
PRINT_NOW AFV_02 6000 1
WAIT 6000
SET_FIXED_CAMERA_POSITION @cam_base_hq_p@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_pt@ JUMP_CUT
CLEAR_CHAR_TASKS contact_ped[2]
PRINT_NOW AFV_06 3000 1
WAIT 3000
SET_FIXED_CAMERA_POSITION @cam_base_hq_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_ct@ JUMP_CUT
TASK_PLAY_ANIM contact_ped[2] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AFV_07 5500 1
WAIT 5500
CLEAR_CHAR_TASKS contact_ped[2]
RESTORE_CAMERA_JUMPCUT
SET_CAMERA_BEHIND_PLAYER
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
ADD_BLIP_FOR_CAR heli blip
PRINT_NOW AFV_03 5000 1
stage = 0

// ---------------------------------------------------------------- the hunt
vt_loop:
WAIT 250
IF IS_CAR_DEAD heli
    PRINT_NOW AFV_10 4000 1
    PRINT_BIG M_FAIL 5000 1
    RETURN
ENDIF
GET_CHAR_COORDINATES scplayer px py pz
IF stage = 0
    IF IS_CHAR_IN_CAR scplayer heli
        stage = 1
        REMOVE_BLIP blip
        blip = -1
        GOSUB vt_spawn
        PRINT_NOW AFV_08 7000 1
    ENDIF
    GOTO vt_loop
ENDIF
// out of the helicopter?
IF NOT IS_CHAR_IN_CAR scplayer heli
    IF away = 0
        away = 1
        ADD_BLIP_FOR_CAR heli blip
        PRINT_NOW AFV_03 4000 1
    ENDIF
ELSE
    IF away = 1
        away = 0
        REMOVE_BLIP blip
        blip = -1
    ENDIF
ENDIF
// drive the pickups down the valley
retask += 1
k = 0
WHILE k < 5
    IF NOT IS_CAR_DEAD car[k]
    AND NOT IS_CHAR_DEAD drv[k]
        k2 = wp[k]
        IF k2 < cv_count
            GET_CAR_COORDINATES car[k] cx cy cz
            GET_DISTANCE_BETWEEN_COORDS_2D cx cy cv_x[k2] cv_y[k2] d
            IF d < 24.0
                wp[k] += 1
                k2 = wp[k]
                IF k2 < cv_count
                    GOSUB vt_drive
                ENDIF
            ELSE
                IF retask > 15
                    GOSUB vt_drive
                ENDIF
            ENDIF
        ENDIF
    ENDIF
    k += 1
ENDWHILE
IF retask > 15
    retask = 0
ENDIF
dead = 0
k = 0
WHILE k < 5
    IF IS_CAR_DEAD car[k]
        dead += 1
        IF NOT cblip[k] = -1
            REMOVE_BLIP cblip[k]
            cblip[k] = -1
        ENDIF
    ELSE
        k2 = cv_count
        k2 -= 1
        IF wp[k] >= k2
            PRINT_NOW AFV_09 5000 1
            PRINT_BIG M_FAIL 5000 1
            RETURN
        ENDIF
    ENDIF
    k += 1
ENDWHILE
mission_counter = 5
mission_counter -= dead
IF dead = 5
    CLEAR_ONSCREEN_COUNTER mission_counter
    WAIT 2500
    PRINT_NOW AFV_11 6000 1
    WAIT 6000
    GOSUB vt_epilogue
    RETURN
ENDIF
GOTO vt_loop

// ---------------------------------------------------------------- helpers
vt_drive:
TASK_CAR_DRIVE_TO_COORD drv[k] car[k] cv_x[k2] cv_y[k2] cv_z[k2] 14.0 MODE_STRAIGHTLINE -1 3
RETURN

vt_spawn:
CREATE_CAR BOBCAT cv_x[0] cv_y[0] cv_z[0] car[0]
SET_CAR_HEADING car[0] cv_h[0]
CREATE_CAR BOBCAT cv_x[3] cv_y[3] cv_z[3] car[1]
SET_CAR_HEADING car[1] cv_h[3]
CREATE_CAR BOBCAT cv_x[6] cv_y[6] cv_z[6] car[2]
SET_CAR_HEADING car[2] cv_h[6]
CREATE_CAR BOBCAT cv_x[9] cv_y[9] cv_z[9] car[3]
SET_CAR_HEADING car[3] cv_h[9]
CREATE_CAR BOBCAT cv_x[12] cv_y[12] cv_z[12] car[4]
SET_CAR_HEADING car[4] cv_h[12]
wp[0] = 1
wp[1] = 4
wp[2] = 7
wp[3] = 10
wp[4] = 13
k = 0
WHILE k < 5
    CREATE_CHAR_INSIDE_CAR car[k] PEDTYPE_MISSION1 247 drv[k]
    CREATE_CHAR_AS_PASSENGER car[k] PEDTYPE_MISSION1 248 0 gun[k]
    GIVE_WEAPON_TO_CHAR drv[k] WEAPONTYPE_AK47 500
    GIVE_WEAPON_TO_CHAR gun[k] WEAPONTYPE_AK47 900
    SET_CHAR_DROPS_WEAPONS_WHEN_DEAD drv[k] FALSE
    SET_CHAR_DROPS_WEAPONS_WHEN_DEAD gun[k] FALSE
    SET_CHAR_ACCURACY gun[k] 30
    TASK_DRIVE_BY gun[k] scplayer -1 0.0 0.0 0.0 90.0 DRIVEBY_AI_ALL_DIRN TRUE 100
    ADD_BLIP_FOR_CAR car[k] cblip[k]
    k2 = wp[k]
    GOSUB vt_drive
    k += 1
ENDWHILE
mission_counter = 5
DISPLAY_ONSCREEN_COUNTER_WITH_STRING mission_counter COUNTER_DISPLAY_NUMBER AFV_CT
RETURN

// ---------------------------------------------------------------- epilogue: tea with Rahim
vt_epilogue:
DO_FADE 1000 FADE_OUT
WAIT 1100
SET_PLAYER_CONTROL player OFF
SWITCH_WIDESCREEN ON
SET_TIME_OF_DAY 7 30
REQUEST_COLLISION @safehouse.x@ @safehouse.y@
LOAD_SCENE @m8_farid@
SET_CHAR_COORDINATES scplayer @m8_farid@
SET_CHAR_HEADING scplayer @m8_farid.h@
CREATE_CHAR PEDTYPE_CIVMALE 158 @m8_rahim@ rahim
SET_CHAR_HEADING rahim @m8_rahim.h@
SET_CHAR_PROOFS rahim TRUE TRUE TRUE TRUE TRUE
WAIT 600
SET_FIXED_CAMERA_POSITION @cam_end@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_end_t@ JUMP_CUT
DO_FADE 1000 FADE_IN
TASK_PLAY_ANIM rahim IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AF8_10 4500 1
WAIT 4500
CLEAR_CHAR_TASKS rahim
PRINT_NOW AF8_11 2500 1
WAIT 2500
TASK_PLAY_ANIM rahim IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AF8_12 6000 1
WAIT 6000
CLEAR_CHAR_TASKS rahim
RESTORE_CAMERA_JUMPCUT
SET_CAMERA_BEHIND_PLAYER
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
mission_progress = 12
ADD_SCORE player 15000
PRINT_WITH_NUMBER_BIG M_PASS 15000 5000 1
REGISTER_MISSION_PASSED AFV_T
PLAYER_MADE_PROGRESS 1
PLAY_MISSION_PASSED_TUNE 1
WAIT 5500
PRINT_BIG AFG_TTL 5000 2
RETURN

vt_contact:
IF NOT DOES_CHAR_EXIST contact_ped[2]
    CREATE_CHAR PEDTYPE_CIVMALE 287 @base_major@ contact_ped[2]
    SET_CHAR_HEADING contact_ped[2] @base_major.h@
    SET_CHAR_PROOFS contact_ped[2] TRUE TRUE TRUE TRUE TRUE
    SET_CHAR_NEVER_TARGETTED contact_ped[2] TRUE
    SET_CHAR_STAY_IN_SAME_PLACE contact_ped[2] TRUE
ENDIF
RETURN

vt_cleanup:
CLEAR_ONSCREEN_COUNTER mission_counter
IF NOT blip = -1
    REMOVE_BLIP blip
    blip = -1
ENDIF
k = 0
WHILE k < 5
    IF NOT cblip[k] = -1
        REMOVE_BLIP cblip[k]
        cblip[k] = -1
    ENDIF
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
IF NOT heli = -1
    MARK_CAR_AS_NO_LONGER_NEEDED heli
    heli = -1
ENDIF
IF NOT rahim = -1
    MARK_CHAR_AS_NO_LONGER_NEEDED rahim
    rahim = -1
ENDIF
IF DOES_CHAR_EXIST contact_ped[2]
    CLEAR_CHAR_TASKS contact_ped[2]
ENDIF
MARK_MODEL_AS_NO_LONGER_NEEDED 287
MARK_MODEL_AS_NO_LONGER_NEEDED 158
MARK_MODEL_AS_NO_LONGER_NEEDED 247
MARK_MODEL_AS_NO_LONGER_NEEDED 248
MARK_MODEL_AS_NO_LONGER_NEEDED HUNTER
MARK_MODEL_AS_NO_LONGER_NEEDED BOBCAT
RESTORE_CAMERA_JUMPCUT
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
flag_player_on_mission = 0
MISSION_HAS_FINISHED
RETURN
}
