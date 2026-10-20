// Mission 3 - Medicine for Shahrak: timed delivery across the plain, then Captain Nasir calls.
MISSION_START
{
LVAR_INT truck blip cp stage doctor hurry
GOSUB m3_main
IF HAS_DEATHARREST_BEEN_EXECUTED
    PRINT_BIG M_FAIL 5000 1
ENDIF
GOSUB m3_cleanup
MISSION_END

m3_main:
SCRIPT_NAME AFM3
truck = -1
blip = -1
cp = -1
doctor = -1
hurry = 0
flag_player_on_mission = 1
REGISTER_MISSION_GIVEN
WAIT 0
REQUEST_MODEL 158
REQUEST_MODEL 160
REQUEST_MODEL SANDKING
LOAD_ALL_MODELS_NOW
GOSUB m3_contact
CREATE_CAR SANDKING @bazaar_car2@ truck
SET_CAR_HEADING truck @bazaar_car2.h@

// ---------------------------------------------------------------- intro cutscene
DO_FADE 400 FADE_OUT
WAIT 450
SET_PLAYER_CONTROL player OFF
SWITCH_WIDESCREEN ON
SET_CHAR_COORDINATES scplayer @talk_bazaar@
SET_CHAR_HEADING scplayer @talk_bazaar.h@
SET_CHAR_COORDINATES contact_ped[0] @bazaar_uncle@
SET_CHAR_HEADING contact_ped[0] @bazaar_uncle.h@
WAIT 600
SET_FIXED_CAMERA_POSITION @cam_bazaar_w@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_bazaar_wt@ JUMP_CUT
DO_FADE 400 FADE_IN
PRINT_BIG AF3_T 3000 2
TASK_PLAY_ANIM contact_ped[0] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AF3_01 4500 1
WAIT 4500
SET_FIXED_CAMERA_POSITION @cam_bazaar_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_bazaar_ct@ JUMP_CUT
PRINT_NOW AF3_02 5000 1
WAIT 5000
SET_FIXED_CAMERA_POSITION @cam_bazaar_p@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_bazaar_pt@ JUMP_CUT
CLEAR_CHAR_TASKS contact_ped[0]
PRINT_NOW AF3_06 2500 1
WAIT 2500
SET_FIXED_CAMERA_POSITION @cam_bazaar_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_bazaar_ct@ JUMP_CUT
TASK_PLAY_ANIM contact_ped[0] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AF3_07 5000 1
WAIT 5000
CLEAR_CHAR_TASKS contact_ped[0]
GOSUB m3_cut_end

ADD_BLIP_FOR_CAR truck blip
PRINT_NOW AF3_08 5000 1
stage = 0

// ---------------------------------------------------------------- the race
m3_loop:
WAIT 100
IF IS_CAR_DEAD truck
    PRINT_NOW AF1_06 4000 1
    PRINT_BIG M_FAIL 5000 1
    RETURN
ENDIF
IF stage = 0
    IF IS_CHAR_IN_CAR scplayer truck
        REMOVE_BLIP blip
        ADD_BLIP_FOR_COORD @shahrak@ blip
        CREATE_CHECKPOINT CHECKPOINT_TUBE @shahrak@ @shahrak@ 6.0 cp
        PRINT_NOW AF3_03 6000 1
        mission_timer = 180000
        DISPLAY_ONSCREEN_TIMER_WITH_STRING mission_timer TIMER_DOWN AF3_TM
        stage = 1
    ENDIF
    GOTO m3_loop
ENDIF
IF mission_timer < 1
    PRINT_NOW AF3_04 4000 1
    PRINT_BIG M_FAIL 5000 1
    RETURN
ENDIF
IF hurry = 0
AND mission_timer < 45000
    hurry = 1
    PRINT_NOW AF3_09 4000 1
ENDIF
IF NOT IS_CHAR_IN_CAR scplayer truck
    IF stage = 1
        REMOVE_BLIP blip
        ADD_BLIP_FOR_CAR truck blip
        PRINT_NOW AF3_08 5000 1
        stage = 2
    ENDIF
ELSE
    IF stage = 2
        REMOVE_BLIP blip
        ADD_BLIP_FOR_COORD @shahrak@ blip
        PRINT_NOW AF3_03 5000 1
        stage = 1
    ENDIF
    IF LOCATE_CHAR_IN_CAR_3D scplayer @shahrak@ 6.0 6.0 4.0 FALSE
        GOSUB m3_arrive
        RETURN
    ENDIF
ENDIF
GOTO m3_loop

// ---------------------------------------------------------------- the clinic
m3_arrive:
CLEAR_ONSCREEN_TIMER mission_timer
SET_PLAYER_CONTROL player OFF
SWITCH_WIDESCREEN ON
REMOVE_BLIP blip
blip = -1
DELETE_CHECKPOINT cp
cp = -1
SET_CAR_FORWARD_SPEED truck 0.0
CREATE_CHAR PEDTYPE_CIVMALE 160 @m3_doctor@ doctor
SET_CHAR_HEADING doctor @m3_doctor.h@
SET_CHAR_PROOFS doctor TRUE TRUE TRUE TRUE TRUE
SET_FIXED_CAMERA_POSITION @cam_m3_doctor@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_m3_doctor_t@ JUMP_CUT
TASK_GO_STRAIGHT_TO_COORD doctor @m3_doctor_to@ PEDMOVE_RUN 6000
PRINT_NOW AF3_05 4500 1
WAIT 4500
TASK_PLAY_ANIM doctor IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AF3_11 5500 1
WAIT 5500
CLEAR_CHAR_TASKS doctor
GOSUB m3_cut_end
mission_progress = 3
ADD_SCORE player 800
PRINT_WITH_NUMBER_BIG M_PASS 800 5000 1
REGISTER_MISSION_PASSED AF3_T
PLAYER_MADE_PROGRESS 1
PLAY_MISSION_PASSED_TUNE 1
// a moment later the phone rings
WAIT 6000
PRINT_NOW AF3_10 7000 1
RETURN

m3_cut_end:
RESTORE_CAMERA_JUMPCUT
SET_CAMERA_BEHIND_PLAYER
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
RETURN

m3_contact:
IF NOT DOES_CHAR_EXIST contact_ped[0]
    CREATE_CHAR PEDTYPE_CIVMALE 158 @bazaar_uncle@ contact_ped[0]
    SET_CHAR_HEADING contact_ped[0] @bazaar_uncle.h@
    SET_CHAR_PROOFS contact_ped[0] TRUE TRUE TRUE TRUE TRUE
    SET_CHAR_NEVER_TARGETTED contact_ped[0] TRUE
    SET_CHAR_STAY_IN_SAME_PLACE contact_ped[0] TRUE
ENDIF
RETURN

m3_cleanup:
CLEAR_ONSCREEN_TIMER mission_timer
IF NOT blip = -1
    REMOVE_BLIP blip
    blip = -1
ENDIF
IF NOT cp = -1
    DELETE_CHECKPOINT cp
    cp = -1
ENDIF
IF NOT doctor = -1
    MARK_CHAR_AS_NO_LONGER_NEEDED doctor
    doctor = -1
ENDIF
IF NOT truck = -1
    MARK_CAR_AS_NO_LONGER_NEEDED truck
    truck = -1
ENDIF
IF DOES_CHAR_EXIST contact_ped[0]
    CLEAR_CHAR_TASKS contact_ped[0]
ENDIF
MARK_MODEL_AS_NO_LONGER_NEEDED 158
MARK_MODEL_AS_NO_LONGER_NEEDED 160
MARK_MODEL_AS_NO_LONGER_NEEDED SANDKING
RESTORE_CAMERA_JUMPCUT
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
flag_player_on_mission = 0
MISSION_HAS_FINISHED
RETURN
}
