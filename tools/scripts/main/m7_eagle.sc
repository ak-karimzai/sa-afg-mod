// Mission 7 - Eagle's Nest: photograph Qala-e-Kohna from a helicopter under RPG fire and land back at the base.
MISSION_START
{
LVAR_INT heli blip stage enemy[3] k photos
LVAR_FLOAT px py pz d
GOSUB m7_main
IF HAS_DEATHARREST_BEEN_EXECUTED
    PRINT_BIG M_FAIL 5000 1
ENDIF
GOSUB m7_cleanup
MISSION_END

m7_main:
SCRIPT_NAME AFM7
heli = -1
blip = -1
photos = 0
k = 0
WHILE k < 3
    enemy[k] = -1
    k += 1
ENDWHILE
flag_player_on_mission = 1
REGISTER_MISSION_GIVEN
WAIT 0
REQUEST_MODEL 287
REQUEST_MODEL 248
REQUEST_MODEL 359
REQUEST_MODEL MAVERICK
LOAD_ALL_MODELS_NOW
GOSUB m7_contact
CREATE_CAR MAVERICK @base_heli@ heli
SET_CAR_HEADING heli 0.0

// ---------------------------------------------------------------- briefing
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
PRINT_BIG AF7_T 3000 2
TASK_PLAY_ANIM contact_ped[2] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AF7_01 4500 1
WAIT 4500
SET_FIXED_CAMERA_POSITION @cam_base_hq_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_ct@ JUMP_CUT
PRINT_NOW AF7_02 5000 1
WAIT 5000
SET_FIXED_CAMERA_POSITION @cam_base_hq_p@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_pt@ JUMP_CUT
CLEAR_CHAR_TASKS contact_ped[2]
PRINT_NOW AF7_08 4000 1
WAIT 4000
SET_FIXED_CAMERA_POSITION @cam_base_hq_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_ct@ JUMP_CUT
TASK_PLAY_ANIM contact_ped[2] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AF7_09 5000 1
WAIT 5000
CLEAR_CHAR_TASKS contact_ped[2]
GOSUB m7_cut_end

ADD_BLIP_FOR_CAR heli blip
PRINT_NOW AF7_10 5000 1
stage = 0

// ---------------------------------------------------------------- the flight
m7_loop:
WAIT 100
IF IS_CAR_DEAD heli
    PRINT_NOW AF7_06 4000 1
    PRINT_BIG M_FAIL 5000 1
    RETURN
ENDIF
GET_CHAR_COORDINATES scplayer px py pz
IF stage = 0
    IF IS_CHAR_IN_CAR scplayer heli
        REMOVE_BLIP blip
        ADD_BLIP_FOR_COORD @fortress_keep@ blip
        PRINT_NOW AF7_03 6000 1
        stage = 1
    ENDIF
    GOTO m7_loop
ENDIF
IF NOT IS_CHAR_IN_CAR scplayer heli
    // on foot somewhere: back to the helicopter
    GET_DISTANCE_BETWEEN_COORDS_2D px py @base_heli.x@ @base_heli.y@ d
    IF d > 60.0
        IF NOT stage = 9
            REMOVE_BLIP blip
            ADD_BLIP_FOR_CAR heli blip
            PRINT_NOW AF7_13 4000 1
            stage = 9
        ENDIF
    ENDIF
    GOTO m7_loop
ENDIF
IF stage = 9
    // back aboard: resume where we were
    REMOVE_BLIP blip
    IF photos < 40
        ADD_BLIP_FOR_COORD @fortress_keep@ blip
        stage = 1
    ELSE
        ADD_BLIP_FOR_COORD @base_heli@ blip
        stage = 3
    ENDIF
    GOTO m7_loop
ENDIF
IF stage = 1
    GET_DISTANCE_BETWEEN_COORDS_2D px py @fortress_keep.x@ @fortress_keep.y@ d
    IF d < 230.0
        stage = 2
        CREATE_CHAR PEDTYPE_MISSION1 248 @fort_rpg0@ enemy[0]
        CREATE_CHAR PEDTYPE_MISSION1 248 @fort_rpg1@ enemy[1]
        CREATE_CHAR PEDTYPE_MISSION1 248 @fort_rpg2@ enemy[2]
        k = 0
        WHILE k < 3
            GIVE_WEAPON_TO_CHAR enemy[k] WEAPONTYPE_ROCKETLAUNCHER 12
            SET_CHAR_ACCURACY enemy[k] 10
            SET_CHAR_DROPS_WEAPONS_WHEN_DEAD enemy[k] FALSE
            TASK_KILL_CHAR_ON_FOOT enemy[k] scplayer
            k += 1
        ENDWHILE
        PRINT_NOW AF7_11 6000 1
        mission_bar = 0
        DISPLAY_ONSCREEN_COUNTER_WITH_STRING mission_bar COUNTER_DISPLAY_BAR AF7_PH
    ENDIF
    GOTO m7_loop
ENDIF
IF stage = 2
    GET_DISTANCE_BETWEEN_COORDS_2D px py @fortress_keep.x@ @fortress_keep.y@ d
    IF d < 180.0
        photos += 1
        mission_bar = photos
        mission_bar *= 5
        mission_bar /= 2
    ENDIF
    IF photos >= 40
        CLEAR_ONSCREEN_COUNTER mission_bar
        PRINT_NOW AF7_04 6000 1
        REMOVE_BLIP blip
        ADD_BLIP_FOR_COORD @base_heli@ blip
        stage = 3
    ENDIF
    GOTO m7_loop
ENDIF
IF stage = 3
    GET_DISTANCE_BETWEEN_COORDS_2D px py @base_heli.x@ @base_heli.y@ d
    IF d < 300.0
        PRINT_NOW AF7_05 5000 1
        stage = 4
    ENDIF
    GOTO m7_loop
ENDIF
IF LOCATE_CAR_3D heli @base_heli@ 12.0 12.0 5.0 TRUE
AND IS_CAR_STOPPED heli
    GOSUB m7_debrief
    RETURN
ENDIF
GOTO m7_loop

// ---------------------------------------------------------------- debrief
m7_debrief:
SET_PLAYER_CONTROL player OFF
SWITCH_WIDESCREEN ON
REMOVE_BLIP blip
blip = -1
WAIT 1000
PRINT_NOW AF7_07 5500 1
WAIT 5500
PRINT_NOW AF7_12 5000 1
WAIT 5000
GOSUB m7_cut_end
mission_progress = 7
ADD_SCORE player 2500
PRINT_WITH_NUMBER_BIG M_PASS 2500 5000 1
REGISTER_MISSION_PASSED AF7_T
PLAYER_MADE_PROGRESS 1
PLAY_MISSION_PASSED_TUNE 1
RETURN

m7_cut_end:
RESTORE_CAMERA_JUMPCUT
SET_CAMERA_BEHIND_PLAYER
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
RETURN

m7_contact:
IF NOT DOES_CHAR_EXIST contact_ped[2]
    CREATE_CHAR PEDTYPE_CIVMALE 287 @base_major@ contact_ped[2]
    SET_CHAR_HEADING contact_ped[2] @base_major.h@
    SET_CHAR_PROOFS contact_ped[2] TRUE TRUE TRUE TRUE TRUE
    SET_CHAR_NEVER_TARGETTED contact_ped[2] TRUE
    SET_CHAR_STAY_IN_SAME_PLACE contact_ped[2] TRUE
ENDIF
RETURN

m7_cleanup:
CLEAR_ONSCREEN_COUNTER mission_bar
IF NOT blip = -1
    REMOVE_BLIP blip
    blip = -1
ENDIF
k = 0
WHILE k < 3
    IF NOT enemy[k] = -1
        MARK_CHAR_AS_NO_LONGER_NEEDED enemy[k]
        enemy[k] = -1
    ENDIF
    k += 1
ENDWHILE
IF NOT heli = -1
    MARK_CAR_AS_NO_LONGER_NEEDED heli
    heli = -1
ENDIF
IF DOES_CHAR_EXIST contact_ped[2]
    CLEAR_CHAR_TASKS contact_ped[2]
ENDIF
MARK_MODEL_AS_NO_LONGER_NEEDED 287
MARK_MODEL_AS_NO_LONGER_NEEDED 248
MARK_MODEL_AS_NO_LONGER_NEEDED MAVERICK
RESTORE_CAMERA_JUMPCUT
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
flag_player_on_mission = 0
MISSION_HAS_FINISHED
RETURN
}
