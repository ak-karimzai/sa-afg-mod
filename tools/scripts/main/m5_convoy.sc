// Mission 5 - Aid Convoy: drive the aid truck from Camp Sangar up the valley, survive the ambush.
MISSION_START
{
LVAR_INT major truck blip stage ambushed enemy[6] eblip[6] k health
LVAR_FLOAT px py pz d
GOSUB m5_main
IF HAS_DEATHARREST_BEEN_EXECUTED
    PRINT_BIG M_FAIL 5000 1
ENDIF
GOSUB m5_cleanup
MISSION_END

m5_main:
SCRIPT_NAME AFM5
major = -1
truck = -1
blip = -1
k = 0
WHILE k < 6
    enemy[k] = -1
    k += 1
ENDWHILE
k = 0
WHILE k < 6
    eblip[k] = -1
    k += 1
ENDWHILE
flag_player_on_mission = 1
REGISTER_MISSION_GIVEN
WAIT 0
PRINT_BIG AF5_T 3000 2
REQUEST_MODEL 287
REQUEST_MODEL YANKEE
REQUEST_MODEL 247
REQUEST_MODEL 248
REQUEST_MODEL 200
REQUEST_MODEL 355
REQUEST_MODEL 359
LOAD_ALL_MODELS_NOW
CREATE_CHAR PEDTYPE_MISSION2 287 @base_major@ major
SET_CHAR_HEADING major @base_major.h@
SET_CHAR_PROOFS major TRUE TRUE TRUE TRUE TRUE
CREATE_CAR YANKEE @aid_truck@ truck
SET_CAR_HEADING truck @aid_truck.h@
SET_CAR_HEALTH truck 2500
SET_PLAYER_CONTROL player OFF
SWITCH_WIDESCREEN ON
PRINT_NOW AF5_01 5000 1
WAIT 5000
PRINT_NOW AF5_02 5000 1
WAIT 5000
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
SET_CAMERA_BEHIND_PLAYER
ADD_BLIP_FOR_CAR truck blip
PRINT_NOW AF5_03 5000 1
stage = 0
ambushed = 0

m5_loop:
WAIT 100
IF IS_CAR_DEAD truck
    PRINT_NOW AF5_06 4000 1
    PRINT_BIG M_FAIL 5000 1
    RETURN
ENDIF
IF stage = 0
    IF IS_CHAR_IN_CAR scplayer truck
        IF NOT blip = -1
            REMOVE_BLIP blip
            blip = -1
        ENDIF
        ADD_BLIP_FOR_COORD @dara_e_sabz@ blip
        PRINT_NOW AF5_04 6000 1
        stage = 1
    ENDIF
ELSE
    IF NOT IS_CHAR_IN_CAR scplayer truck
        IF NOT blip = -1
            REMOVE_BLIP blip
            blip = -1
        ENDIF
        ADD_BLIP_FOR_CAR truck blip
        PRINT_NOW AF5_03 5000 1
        stage = 0
    ELSE
        IF LOCATE_CHAR_IN_CAR_3D scplayer @dara_e_sabz@ 8.0 8.0 5.0 TRUE
            SET_PLAYER_CONTROL player OFF
            PRINT_NOW AF5_07 6000 1
            WAIT 6000
            SET_PLAYER_CONTROL player ON
            mission_progress = 5
            ADD_SCORE player 2000
            PRINT_WITH_NUMBER_BIG M_PASS 2000 5000 1
            REGISTER_MISSION_PASSED AF5_T
            PLAYER_MADE_PROGRESS 1
            PLAY_MISSION_PASSED_TUNE 1
            RETURN
        ENDIF
    ENDIF
ENDIF
IF ambushed = 0
    GET_CHAR_COORDINATES scplayer px py pz
    GET_DISTANCE_BETWEEN_COORDS_2D px py @ambush.x@ @ambush.y@ d
    IF d < 140.0
        ambushed = 1
        PRINT_NOW AF5_05 5000 1
        CREATE_CHAR PEDTYPE_MISSION1 247 @amb0@ enemy[0]
        CREATE_CHAR PEDTYPE_MISSION1 248 @amb1@ enemy[1]
        CREATE_CHAR PEDTYPE_MISSION1 200 @amb2@ enemy[2]
        CREATE_CHAR PEDTYPE_MISSION1 247 @amb3@ enemy[3]
        CREATE_CHAR PEDTYPE_MISSION1 248 @amb4@ enemy[4]
        CREATE_CHAR PEDTYPE_MISSION1 200 @amb5@ enemy[5]
        k = 0
        WHILE k < 6
            GIVE_WEAPON_TO_CHAR enemy[k] WEAPONTYPE_AK47 500
            SET_CHAR_ACCURACY enemy[k] 30
            ADD_BLIP_FOR_CHAR enemy[k] eblip[k]
            TASK_KILL_CHAR_ON_FOOT enemy[k] scplayer
            k += 1
        ENDWHILE
        GIVE_WEAPON_TO_CHAR enemy[2] WEAPONTYPE_ROCKETLAUNCHER 6
        SET_CHAR_ACCURACY enemy[2] 15
    ENDIF
ELSE
    k = 0
    WHILE k < 6
        IF IS_CHAR_DEAD enemy[k]
            IF NOT eblip[k] = -1
                REMOVE_BLIP eblip[k]
                eblip[k] = -1
            ENDIF
        ENDIF
        k += 1
    ENDWHILE
ENDIF
GOTO m5_loop

m5_cleanup:
IF NOT blip = -1
    REMOVE_BLIP blip
    blip = -1
ENDIF
k = 0
WHILE k < 6
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
IF NOT major = -1
    MARK_CHAR_AS_NO_LONGER_NEEDED major
    major = -1
ENDIF
IF NOT truck = -1
    MARK_CAR_AS_NO_LONGER_NEEDED truck
    truck = -1
ENDIF
MARK_MODEL_AS_NO_LONGER_NEEDED 287
MARK_MODEL_AS_NO_LONGER_NEEDED YANKEE
MARK_MODEL_AS_NO_LONGER_NEEDED 247
MARK_MODEL_AS_NO_LONGER_NEEDED 248
MARK_MODEL_AS_NO_LONGER_NEEDED 200
flag_player_on_mission = 0
MISSION_HAS_FINISHED
RETURN
}
