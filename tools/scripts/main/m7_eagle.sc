// Mission 7 - Eagle's Nest: scout Qala-e-Kohna by helicopter and bring it back.
MISSION_START
{
LVAR_INT major heli blip stage enemy[3] k
LVAR_FLOAT px py pz d
GOSUB m7_main
IF HAS_DEATHARREST_BEEN_EXECUTED
    PRINT_BIG M_FAIL 5000 1
ENDIF
GOSUB m7_cleanup
MISSION_END

m7_main:
SCRIPT_NAME AFM7
major = -1
heli = -1
blip = -1
k = 0
WHILE k < 3
    enemy[k] = -1
    k += 1
ENDWHILE
flag_player_on_mission = 1
REGISTER_MISSION_GIVEN
WAIT 0
PRINT_BIG AF7_T 3000 2
REQUEST_MODEL 287
REQUEST_MODEL MAVERICK
REQUEST_MODEL 248
REQUEST_MODEL 359
LOAD_ALL_MODELS_NOW
CREATE_CHAR PEDTYPE_MISSION2 287 @base_major@ major
SET_CHAR_HEADING major @base_major.h@
SET_CHAR_PROOFS major TRUE TRUE TRUE TRUE TRUE
CREATE_CAR MAVERICK @base_heli@ heli
SET_CAR_HEADING heli 0.0
SET_PLAYER_CONTROL player OFF
SWITCH_WIDESCREEN ON
PRINT_NOW AF7_01 5000 1
WAIT 5000
PRINT_NOW AF7_02 5000 1
WAIT 5000
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
SET_CAMERA_BEHIND_PLAYER
ADD_BLIP_FOR_CAR heli blip
stage = 0

m7_loop:
WAIT 100
IF IS_CAR_DEAD heli
    PRINT_NOW AF7_06 4000 1
    PRINT_BIG M_FAIL 5000 1
    RETURN
ENDIF
IF stage = 0
    IF IS_CHAR_IN_CAR scplayer heli
        REMOVE_BLIP blip
        ADD_BLIP_FOR_COORD @fortress_keep@ blip
        PRINT_NOW AF7_03 6000 1
        stage = 1
    ENDIF
    GOTO m7_loop
ENDIF
IF stage = 1
    GET_CHAR_COORDINATES scplayer px py pz
    GET_DISTANCE_BETWEEN_COORDS_2D px py @fortress_keep.x@ @fortress_keep.y@ d
    IF d < 170.0
        stage = 2
        CREATE_CHAR PEDTYPE_MISSION1 248 @fort_rpg0@ enemy[0]
        CREATE_CHAR PEDTYPE_MISSION1 248 @fort_rpg1@ enemy[1]
        CREATE_CHAR PEDTYPE_MISSION1 248 @fort_rpg2@ enemy[2]
        k = 0
        WHILE k < 3
            GIVE_WEAPON_TO_CHAR enemy[k] WEAPONTYPE_ROCKETLAUNCHER 10
            SET_CHAR_ACCURACY enemy[k] 10
            TASK_KILL_CHAR_ON_FOOT enemy[k] scplayer
            k += 1
        ENDWHILE
        PRINT_NOW AF7_04 6000 1
        REMOVE_BLIP blip
        ADD_BLIP_FOR_COORD @base_heli@ blip
    ENDIF
    GOTO m7_loop
ENDIF
IF stage = 2
    GET_CHAR_COORDINATES scplayer px py pz
    GET_DISTANCE_BETWEEN_COORDS_2D px py @base_heli.x@ @base_heli.y@ d
    IF d < 300.0
        PRINT_NOW AF7_05 5000 1
        stage = 3
    ENDIF
    GOTO m7_loop
ENDIF
IF IS_CHAR_IN_CAR scplayer heli
AND LOCATE_CAR_3D heli @base_heli@ 10.0 10.0 4.0 TRUE
AND IS_CAR_STOPPED heli
    SET_PLAYER_CONTROL player OFF
    PRINT_NOW AF7_07 5000 1
    WAIT 5000
    SET_PLAYER_CONTROL player ON
    mission_progress = 7
    ADD_SCORE player 2500
    PRINT_WITH_NUMBER_BIG M_PASS 2500 5000 1
    REGISTER_MISSION_PASSED AF7_T
    PLAYER_MADE_PROGRESS 1
    PLAY_MISSION_PASSED_TUNE 1
    RETURN
ENDIF
GOTO m7_loop

m7_cleanup:
IF NOT blip = -1
    REMOVE_BLIP blip
ENDIF
k = 0
WHILE k < 3
    IF NOT enemy[k] = -1
        MARK_CHAR_AS_NO_LONGER_NEEDED enemy[k]
    ENDIF
    k += 1
ENDWHILE
IF NOT major = -1
    MARK_CHAR_AS_NO_LONGER_NEEDED major
ENDIF
IF NOT heli = -1
    MARK_CAR_AS_NO_LONGER_NEEDED heli
ENDIF
MARK_MODEL_AS_NO_LONGER_NEEDED 287
MARK_MODEL_AS_NO_LONGER_NEEDED MAVERICK
MARK_MODEL_AS_NO_LONGER_NEEDED 248
flag_player_on_mission = 0
MISSION_HAS_FINISHED
RETURN
}
