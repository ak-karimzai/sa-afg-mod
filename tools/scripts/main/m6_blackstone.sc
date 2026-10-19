// Mission 6 - Black Stone: clear the bandit camp at Sang-e-Siah.
MISSION_START
{
LVAR_INT nasir blip stage enemy[10] eblip[10] k alive
LVAR_FLOAT px py pz d
GOSUB m6_main
IF HAS_DEATHARREST_BEEN_EXECUTED
    PRINT_BIG M_FAIL 5000 1
ENDIF
GOSUB m6_cleanup
MISSION_END

m6_main:
SCRIPT_NAME AFM6
nasir = -1
blip = -1
k = 0
WHILE k < 10
    enemy[k] = -1
    eblip[k] = -1
    k += 1
ENDWHILE
flag_player_on_mission = 1
REGISTER_MISSION_GIVEN
WAIT 0
PRINT_BIG AF6_T 3000 2
REQUEST_MODEL 288
REQUEST_MODEL 247
REQUEST_MODEL 248
REQUEST_MODEL 162
REQUEST_MODEL 200
REQUEST_MODEL 355
REQUEST_MODEL 349
REQUEST_MODEL 359
LOAD_ALL_MODELS_NOW
CREATE_CHAR PEDTYPE_MISSION2 288 @police_nasir@ nasir
SET_CHAR_HEADING nasir @police_nasir.h@
SET_CHAR_PROOFS nasir TRUE TRUE TRUE TRUE TRUE
SET_PLAYER_CONTROL player OFF
SWITCH_WIDESCREEN ON
PRINT_NOW AF6_01 5000 1
WAIT 5000
PRINT_NOW AF6_02 5000 1
WAIT 5000
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
SET_CAMERA_BEHIND_PLAYER
ADD_BLIP_FOR_COORD @bandit_camp@ blip
PRINT_NOW AF6_03 6000 1
stage = 0

m6_loop:
WAIT 250
GET_CHAR_COORDINATES scplayer px py pz
GET_DISTANCE_BETWEEN_COORDS_2D px py @bandit_camp.x@ @bandit_camp.y@ d
IF stage = 0
    IF d < 260.0
        stage = 1
        CREATE_CHAR PEDTYPE_MISSION1 247 @camp0@ enemy[0]
        CREATE_CHAR PEDTYPE_MISSION1 248 @camp1@ enemy[1]
        CREATE_CHAR PEDTYPE_MISSION1 162 @camp2@ enemy[2]
        CREATE_CHAR PEDTYPE_MISSION1 200 @camp3@ enemy[3]
        CREATE_CHAR PEDTYPE_MISSION1 247 @camp4@ enemy[4]
        CREATE_CHAR PEDTYPE_MISSION1 248 @camp5@ enemy[5]
        CREATE_CHAR PEDTYPE_MISSION1 162 @camp6@ enemy[6]
        CREATE_CHAR PEDTYPE_MISSION1 200 @camp7@ enemy[7]
        CREATE_CHAR PEDTYPE_MISSION1 247 @camp8@ enemy[8]
        CREATE_CHAR PEDTYPE_MISSION1 248 @camp9@ enemy[9]
        k = 0
        WHILE k < 10
            GIVE_WEAPON_TO_CHAR enemy[k] WEAPONTYPE_AK47 500
            SET_CHAR_ACCURACY enemy[k] 35
            SET_CHAR_STAY_IN_SAME_PLACE enemy[k] TRUE
            k += 1
        ENDWHILE
        GIVE_WEAPON_TO_CHAR enemy[3] WEAPONTYPE_SHOTGUN 100
        GIVE_WEAPON_TO_CHAR enemy[7] WEAPONTYPE_ROCKETLAUNCHER 5
        SET_CHAR_ACCURACY enemy[7] 15
        REMOVE_BLIP blip
        blip = -1
        k = 0
        WHILE k < 10
            ADD_BLIP_FOR_CHAR enemy[k] eblip[k]
            k += 1
        ENDWHILE
        mission_counter = 10
        DISPLAY_ONSCREEN_COUNTER_WITH_STRING mission_counter COUNTER_DISPLAY_NUMBER AF6_04
    ENDIF
    GOTO m6_loop
ENDIF
IF stage = 1
    IF d < 90.0
        stage = 2
        k = 0
        WHILE k < 10
            IF NOT IS_CHAR_DEAD enemy[k]
                SET_CHAR_STAY_IN_SAME_PLACE enemy[k] FALSE
                TASK_KILL_CHAR_ON_FOOT enemy[k] scplayer
            ENDIF
            k += 1
        ENDWHILE
    ENDIF
ENDIF
alive = 0
k = 0
WHILE k < 10
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
    PRINT_NOW AF6_05 7000 1
    WAIT 3000
    mission_progress = 6
    ADD_SCORE player 3000
    PRINT_WITH_NUMBER_BIG M_PASS 3000 5000 1
    REGISTER_MISSION_PASSED AF6_T
    PLAYER_MADE_PROGRESS 1
    PLAY_MISSION_PASSED_TUNE 1
    RETURN
ENDIF
GOTO m6_loop

m6_cleanup:
CLEAR_ONSCREEN_COUNTER mission_counter
IF NOT blip = -1
    REMOVE_BLIP blip
ENDIF
k = 0
WHILE k < 10
    IF NOT eblip[k] = -1
        REMOVE_BLIP eblip[k]
    ENDIF
    IF NOT enemy[k] = -1
        MARK_CHAR_AS_NO_LONGER_NEEDED enemy[k]
    ENDIF
    k += 1
ENDWHILE
IF NOT nasir = -1
    MARK_CHAR_AS_NO_LONGER_NEEDED nasir
ENDIF
MARK_MODEL_AS_NO_LONGER_NEEDED 288
MARK_MODEL_AS_NO_LONGER_NEEDED 247
MARK_MODEL_AS_NO_LONGER_NEEDED 248
MARK_MODEL_AS_NO_LONGER_NEEDED 162
MARK_MODEL_AS_NO_LONGER_NEEDED 200
flag_player_on_mission = 0
MISSION_HAS_FINISHED
RETURN
}
