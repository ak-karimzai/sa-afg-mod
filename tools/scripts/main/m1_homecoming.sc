// Mission 1 - Homecoming: meet Kaka Rahim at the bazaar, drive flour to Deh-e-Bagh.
MISSION_START
{
LVAR_INT uncle truck blip stage
GOSUB m1_main
IF HAS_DEATHARREST_BEEN_EXECUTED
    PRINT_BIG M_FAIL 5000 1
ENDIF
GOSUB m1_cleanup
MISSION_END

m1_main:
SCRIPT_NAME AFM1
uncle = -1
truck = -1
blip = -1
flag_player_on_mission = 1
REGISTER_MISSION_GIVEN
WAIT 0
PRINT_BIG AF1_T 3000 2
REQUEST_MODEL 158
REQUEST_MODEL BOBCAT
LOAD_ALL_MODELS_NOW
CREATE_CHAR PEDTYPE_CIVMALE 158 @bazaar_uncle@ uncle
SET_CHAR_HEADING uncle @bazaar_uncle.h@
SET_CHAR_PROOFS uncle TRUE TRUE TRUE TRUE TRUE
CREATE_CAR BOBCAT @bazaar_car@ truck
SET_CAR_HEADING truck @bazaar_car.h@
SET_PLAYER_CONTROL player OFF
SWITCH_WIDESCREEN ON
PRINT_NOW AF1_01 4500 1
WAIT 4500
PRINT_NOW AF1_02 5000 1
WAIT 5000
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
SET_CAMERA_BEHIND_PLAYER
ADD_BLIP_FOR_CAR truck blip
PRINT_NOW AF1_03 5000 1
stage = 0

m1_loop:
WAIT 0
IF IS_CAR_DEAD truck
    PRINT_NOW AF1_06 4000 1
    PRINT_BIG M_FAIL 5000 1
    RETURN
ENDIF
IF stage = 0
    IF IS_CHAR_IN_CAR scplayer truck
        IF NOT blip = -1
            REMOVE_BLIP blip
            blip = -1
        ENDIF
        ADD_BLIP_FOR_COORD @deh_e_bagh@ blip
        PRINT_NOW AF1_04 6000 1
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
        IF LOCATE_CHAR_IN_CAR_3D scplayer @deh_e_bagh@ 6.0 6.0 4.0 TRUE
            SET_PLAYER_CONTROL player OFF
            PRINT_NOW AF1_07 6000 1
            WAIT 6000
            SET_PLAYER_CONTROL player ON
            mission_progress = 1
            ADD_SCORE player 300
            PRINT_WITH_NUMBER_BIG M_PASS 300 5000 1
            REGISTER_MISSION_PASSED AF1_T
            PLAYER_MADE_PROGRESS 1
            PLAY_MISSION_PASSED_TUNE 1
            RETURN
        ENDIF
    ENDIF
ENDIF
GOTO m1_loop

m1_cleanup:
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
MARK_MODEL_AS_NO_LONGER_NEEDED BOBCAT
flag_player_on_mission = 0
MISSION_HAS_FINISHED
RETURN
}
