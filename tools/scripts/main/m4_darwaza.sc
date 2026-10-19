// Mission 4 - Darwaza: hold the valley checkpoint against three waves of bandits at night.
MISSION_START
{
LVAR_INT nasir ally[3] enemy[4] eblip[4] blip stage wave k alive
LVAR_FLOAT px py pz d
GOSUB m4_main
IF HAS_DEATHARREST_BEEN_EXECUTED
    PRINT_BIG M_FAIL 5000 1
ENDIF
GOSUB m4_cleanup
MISSION_END

m4_main:
SCRIPT_NAME AFM4
nasir = -1
blip = -1
k = 0
WHILE k < 3
    ally[k] = -1
    k += 1
ENDWHILE
k = 0
WHILE k < 4
    enemy[k] = -1
    k += 1
ENDWHILE
k = 0
WHILE k < 4
    eblip[k] = -1
    k += 1
ENDWHILE
flag_player_on_mission = 1
REGISTER_MISSION_GIVEN
WAIT 0
PRINT_BIG AF4_T 3000 2
REQUEST_MODEL 288
REQUEST_MODEL 283
REQUEST_MODEL 247
REQUEST_MODEL 248
REQUEST_MODEL 162
REQUEST_MODEL 355
REQUEST_MODEL 352
LOAD_ALL_MODELS_NOW
CREATE_CHAR PEDTYPE_MISSION2 288 @police_nasir@ nasir
SET_CHAR_HEADING nasir @police_nasir.h@
SET_CHAR_PROOFS nasir TRUE TRUE TRUE TRUE TRUE
SET_PLAYER_CONTROL player OFF
SWITCH_WIDESCREEN ON
PRINT_NOW AF4_01 5000 1
WAIT 5000
PRINT_NOW AF4_02 5500 1
WAIT 5500
DO_FADE 800 FADE_OUT
WAIT 900
SET_TIME_OF_DAY 21 0
DO_FADE 800 FADE_IN
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
SET_CAMERA_BEHIND_PLAYER
GIVE_WEAPON_TO_CHAR scplayer WEAPONTYPE_AK47 150
SET_CURRENT_CHAR_WEAPON scplayer WEAPONTYPE_AK47
ADD_BLIP_FOR_COORD @checkpoint@ blip
PRINT_NOW AF4_03 6000 1
stage = 0
wave = 0

m4_loop:
WAIT 250
GET_CHAR_COORDINATES scplayer px py pz
GET_DISTANCE_BETWEEN_COORDS_2D px py @checkpoint.x@ @checkpoint.y@ d
IF stage = 0
    IF d < 45.0
        IF NOT blip = -1
            REMOVE_BLIP blip
            blip = -1
        ENDIF
        CREATE_CHAR PEDTYPE_MISSION2 283 @cp_ally0@ ally[0]
        CREATE_CHAR PEDTYPE_MISSION2 288 @cp_ally1@ ally[1]
        CREATE_CHAR PEDTYPE_MISSION2 283 @cp_ally2@ ally[2]
        k = 0
        WHILE k < 3
            GIVE_WEAPON_TO_CHAR ally[k] WEAPONTYPE_AK47 500
            SET_CHAR_HEALTH ally[k] 200
            k += 1
        ENDWHILE
        stage = 1
        wave = 0
        GOSUB m4_spawn_wave
    ENDIF
    GOTO m4_loop
ENDIF
IF d > 160.0
    PRINT_NOW AF4_06 4000 1
    PRINT_BIG M_FAIL 5000 1
    RETURN
ENDIF
alive = 0
k = 0
WHILE k < 4
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
IF alive = 0
    k = 0
    WHILE k < 4
        IF NOT enemy[k] = -1
            MARK_CHAR_AS_NO_LONGER_NEEDED enemy[k]
            enemy[k] = -1
        ENDIF
        k += 1
    ENDWHILE
    IF wave = 3
        PRINT_NOW AF4_05 6000 1
        GIVE_WEAPON_TO_CHAR scplayer WEAPONTYPE_AK47 250
        WAIT 3000
        mission_progress = 4
        ADD_SCORE player 1500
        PRINT_WITH_NUMBER_BIG M_PASS 1500 5000 1
        REGISTER_MISSION_PASSED AF4_T
        PLAYER_MADE_PROGRESS 1
        PLAY_MISSION_PASSED_TUNE 1
        RETURN
    ENDIF
    WAIT 4000
    GOSUB m4_spawn_wave
ENDIF
GOTO m4_loop

m4_spawn_wave:
wave += 1
PRINT_WITH_NUMBER_NOW AF4_04 wave 4000 1
CREATE_CHAR PEDTYPE_MISSION1 247 @cp_att0@ enemy[0]
CREATE_CHAR PEDTYPE_MISSION1 248 @cp_att1@ enemy[1]
CREATE_CHAR PEDTYPE_MISSION1 162 @cp_att2@ enemy[2]
CREATE_CHAR PEDTYPE_MISSION1 247 @cp_att3@ enemy[3]
GIVE_WEAPON_TO_CHAR enemy[0] WEAPONTYPE_AK47 500
GIVE_WEAPON_TO_CHAR enemy[1] WEAPONTYPE_MICRO_UZI 500
GIVE_WEAPON_TO_CHAR enemy[2] WEAPONTYPE_AK47 500
GIVE_WEAPON_TO_CHAR enemy[3] WEAPONTYPE_MICRO_UZI 500
k = 0
WHILE k < 4
    SET_CHAR_ACCURACY enemy[k] 35
    ADD_BLIP_FOR_CHAR enemy[k] eblip[k]
    TASK_KILL_CHAR_ON_FOOT enemy[k] scplayer
    k += 1
ENDWHILE
RETURN

m4_cleanup:
IF NOT blip = -1
    REMOVE_BLIP blip
    blip = -1
ENDIF
k = 0
WHILE k < 4
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
WHILE k < 3
    IF NOT ally[k] = -1
        MARK_CHAR_AS_NO_LONGER_NEEDED ally[k]
        ally[k] = -1
    ENDIF
    k += 1
ENDWHILE
IF NOT nasir = -1
    MARK_CHAR_AS_NO_LONGER_NEEDED nasir
    nasir = -1
ENDIF
MARK_MODEL_AS_NO_LONGER_NEEDED 288
MARK_MODEL_AS_NO_LONGER_NEEDED 283
MARK_MODEL_AS_NO_LONGER_NEEDED 247
MARK_MODEL_AS_NO_LONGER_NEEDED 248
MARK_MODEL_AS_NO_LONGER_NEEDED 162
flag_player_on_mission = 0
MISSION_HAS_FINISHED
RETURN
}
