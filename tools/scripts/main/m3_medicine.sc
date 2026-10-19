// Mission 3 - Medicine for Shahrak: timed delivery across the plain.
MISSION_START
{
LVAR_INT uncle truck blip stage
GOSUB m3_main
IF HAS_DEATHARREST_BEEN_EXECUTED
    PRINT_BIG M_FAIL 5000 1
ENDIF
GOSUB m3_cleanup
MISSION_END

m3_main:
SCRIPT_NAME AFM3
uncle = -1
truck = -1
blip = -1
flag_player_on_mission = 1
REGISTER_MISSION_GIVEN
WAIT 0
PRINT_BIG AF3_T 3000 2
REQUEST_MODEL 158
REQUEST_MODEL SANDKING
LOAD_ALL_MODELS_NOW
CREATE_CHAR PEDTYPE_CIVMALE 158 @bazaar_uncle@ uncle
SET_CHAR_HEADING uncle @bazaar_uncle.h@
SET_CHAR_PROOFS uncle TRUE TRUE TRUE TRUE TRUE
CREATE_CAR SANDKING @bazaar_car2@ truck
SET_CAR_HEADING truck @bazaar_car2.h@
SET_PLAYER_CONTROL player OFF
SWITCH_WIDESCREEN ON
PRINT_NOW AF3_01 5000 1
WAIT 5000
PRINT_NOW AF3_02 5000 1
WAIT 5000
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
SET_CAMERA_BEHIND_PLAYER
ADD_BLIP_FOR_CAR truck blip
PRINT_NOW AF1_03 5000 1
stage = 0
mission_timer = 150000
DISPLAY_ONSCREEN_TIMER mission_timer TIMER_DOWN

m3_loop:
WAIT 0
IF IS_CAR_DEAD truck
    PRINT_NOW AF1_06 4000 1
    PRINT_BIG M_FAIL 5000 1
    RETURN
ENDIF
IF mission_timer < 1
    PRINT_NOW AF3_04 4000 1
    PRINT_BIG M_FAIL 5000 1
    RETURN
ENDIF
IF stage = 0
    IF IS_CHAR_IN_CAR scplayer truck
        IF NOT blip = -1
            REMOVE_BLIP blip
            blip = -1
        ENDIF
        ADD_BLIP_FOR_COORD @shahrak@ blip
        PRINT_NOW AF3_03 6000 1
        stage = 1
    ENDIF
ELSE
    IF NOT IS_CHAR_IN_CAR scplayer truck
        IF NOT blip = -1
            REMOVE_BLIP blip
            blip = -1
        ENDIF
        ADD_BLIP_FOR_CAR truck blip
        PRINT_NOW AF1_03 5000 1
        stage = 0
    ELSE
        IF LOCATE_CHAR_IN_CAR_3D scplayer @shahrak@ 6.0 6.0 4.0 TRUE
            CLEAR_ONSCREEN_TIMER mission_timer
            SET_PLAYER_CONTROL player OFF
            PRINT_NOW AF3_05 6000 1
            WAIT 6000
            SET_PLAYER_CONTROL player ON
            mission_progress = 3
            ADD_SCORE player 800
            PRINT_WITH_NUMBER_BIG M_PASS 800 5000 1
            REGISTER_MISSION_PASSED AF3_T
            PLAYER_MADE_PROGRESS 1
            PLAY_MISSION_PASSED_TUNE 1
            RETURN
        ENDIF
    ENDIF
ENDIF
GOTO m3_loop

m3_cleanup:
CLEAR_ONSCREEN_TIMER mission_timer
IF NOT blip = -1
    REMOVE_BLIP blip
    blip = -1
ENDIF
IF NOT uncle = -1
    MARK_CHAR_AS_NO_LONGER_NEEDED uncle
    uncle = -1
ENDIF
IF NOT truck = -1
    MARK_CAR_AS_NO_LONGER_NEEDED truck
    truck = -1
ENDIF
MARK_MODEL_AS_NO_LONGER_NEEDED 158
MARK_MODEL_AS_NO_LONGER_NEEDED SANDKING
flag_player_on_mission = 0
MISSION_HAS_FINISHED
RETURN
}
