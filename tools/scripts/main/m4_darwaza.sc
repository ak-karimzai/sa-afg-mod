// Mission 4 - Darwaza: hold the valley checkpoint with three policemen against three waves of bandits at night.
// Wave 1 comes on foot, waves 2 and 3 arrive by pickup with men on the slopes.
MISSION_START
{
LVAR_INT ally[3] enemy[6] eblip[6] car[2] blip cp stage wave k alive dead timer cartimer
LVAR_FLOAT px py pz d
GOSUB m4_main
IF HAS_DEATHARREST_BEEN_EXECUTED
    PRINT_BIG M_FAIL 5000 1
ENDIF
GOSUB m4_cleanup
MISSION_END

m4_main:
SCRIPT_NAME AFM4
blip = -1
cp = -1
k = 0
WHILE k < 3
    ally[k] = -1
    k += 1
ENDWHILE
k = 0
WHILE k < 6
    enemy[k] = -1
    eblip[k] = -1
    k += 1
ENDWHILE
car[0] = -1
car[1] = -1
cartimer = 0
flag_player_on_mission = 1
REGISTER_MISSION_GIVEN
WAIT 0
REQUEST_MODEL 288
REQUEST_MODEL 283
REQUEST_MODEL 247
REQUEST_MODEL 248
REQUEST_MODEL 162
REQUEST_MODEL 355
REQUEST_MODEL 352
REQUEST_MODEL 359
REQUEST_MODEL BOBCAT
LOAD_ALL_MODELS_NOW
GOSUB m4_contact

// ---------------------------------------------------------------- briefing at the police station
DO_FADE 400 FADE_OUT
WAIT 450
SET_PLAYER_CONTROL player OFF
SWITCH_WIDESCREEN ON
SET_CHAR_COORDINATES scplayer @talk_police@
SET_CHAR_HEADING scplayer @talk_police.h@
SET_CHAR_COORDINATES contact_ped[1] @police_nasir@
SET_CHAR_HEADING contact_ped[1] @police_nasir.h@
WAIT 600
SET_FIXED_CAMERA_POSITION @cam_police_w@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_police_wt@ JUMP_CUT
DO_FADE 400 FADE_IN
PRINT_BIG AF4_T 3000 2
TASK_PLAY_ANIM contact_ped[1] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AF4_01 4500 1
WAIT 4500
SET_FIXED_CAMERA_POSITION @cam_police_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_police_ct@ JUMP_CUT
PRINT_NOW AF4_02 5500 1
WAIT 5500
SET_FIXED_CAMERA_POSITION @cam_police_p@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_police_pt@ JUMP_CUT
CLEAR_CHAR_TASKS contact_ped[1]
PRINT_NOW AF4_07 3500 1
WAIT 3500
SET_FIXED_CAMERA_POSITION @cam_police_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_police_ct@ JUMP_CUT
TASK_PLAY_ANIM contact_ped[1] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AF4_08 4500 1
WAIT 4500
CLEAR_CHAR_TASKS contact_ped[1]
GIVE_WEAPON_TO_CHAR scplayer WEAPONTYPE_AK47 150
SET_CURRENT_CHAR_WEAPON scplayer WEAPONTYPE_AK47
// evening falls
DO_FADE 800 FADE_OUT
WAIT 900
SET_TIME_OF_DAY 21 0
RESTORE_CAMERA_JUMPCUT
SET_CAMERA_BEHIND_PLAYER
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
DO_FADE 800 FADE_IN
ADD_BLIP_FOR_COORD @checkpoint@ blip
CREATE_CHECKPOINT CHECKPOINT_TUBE @checkpoint@ @checkpoint@ 8.0 cp
PRINT_NOW AF4_03 6000 1
stage = 0
wave = 0

// ---------------------------------------------------------------- the checkpoint
m4_loop:
WAIT 250
GET_CHAR_COORDINATES scplayer px py pz
GET_DISTANCE_BETWEEN_COORDS_2D px py @checkpoint.x@ @checkpoint.y@ d
IF stage = 0
    IF d < 45.0
        REMOVE_BLIP blip
        blip = -1
        DELETE_CHECKPOINT cp
        cp = -1
        CREATE_CHAR PEDTYPE_MISSION2 283 @cp_ally0@ ally[0]
        CREATE_CHAR PEDTYPE_MISSION2 288 @cp_ally1@ ally[1]
        CREATE_CHAR PEDTYPE_MISSION2 283 @cp_ally2@ ally[2]
        k = 0
        WHILE k < 3
            GIVE_WEAPON_TO_CHAR ally[k] WEAPONTYPE_AK47 800
            SET_CHAR_HEALTH ally[k] 200
            SET_CHAR_ACCURACY ally[k] 45
            SET_CHAR_HEADING ally[k] 300.0
            k += 1
        ENDWHILE
        PRINT_NOW AF4_09 5000 1
        stage = 1
        timer = 28
    ENDIF
    GOTO m4_loop
ENDIF
IF d > 160.0
    PRINT_NOW AF4_06 4000 1
    PRINT_BIG M_FAIL 5000 1
    RETURN
ENDIF
dead = 0
k = 0
WHILE k < 3
    IF IS_CHAR_DEAD ally[k]
        dead += 1
    ENDIF
    k += 1
ENDWHILE
IF dead = 3
    PRINT_NOW AF4_10 5000 1
    PRINT_BIG M_FAIL 5000 1
    RETURN
ENDIF
IF stage = 1
    // breather between waves
    timer -= 1
    IF timer < 1
        GOSUB m4_spawn_wave
        stage = 2
    ENDIF
    GOTO m4_loop
ENDIF
// stage 2: a wave is on
IF cartimer > 0
    cartimer -= 1
    IF cartimer = 8
        GOSUB m4_bail_out
    ENDIF
    IF cartimer = 0
        GOSUB m4_attack_all
    ENDIF
ENDIF
alive = 0
k = 0
WHILE k < 6
    IF NOT enemy[k] = -1
        IF IS_CHAR_DEAD enemy[k]
            IF NOT eblip[k] = -1
                REMOVE_BLIP eblip[k]
                eblip[k] = -1
            ENDIF
        ELSE
            alive += 1
        ENDIF
    ENDIF
    k += 1
ENDWHILE
IF alive = 0
    k = 0
    WHILE k < 6
        IF NOT enemy[k] = -1
            MARK_CHAR_AS_NO_LONGER_NEEDED enemy[k]
            enemy[k] = -1
        ENDIF
        k += 1
    ENDWHILE
    IF wave = 3
        GOSUB m4_pass
        RETURN
    ENDIF
    stage = 1
    timer = 20
ENDIF
GOTO m4_loop

// ---------------------------------------------------------------- waves
m4_spawn_wave:
wave += 1
PRINT_WITH_NUMBER_NOW AF4_04 wave 4000 1
IF wave = 1
    CREATE_CHAR PEDTYPE_MISSION1 247 @cp_att0@ enemy[0]
    CREATE_CHAR PEDTYPE_MISSION1 248 @cp_att1@ enemy[1]
    CREATE_CHAR PEDTYPE_MISSION1 162 @cp_att2@ enemy[2]
    CREATE_CHAR PEDTYPE_MISSION1 247 @cp_att3@ enemy[3]
    GIVE_WEAPON_TO_CHAR enemy[0] WEAPONTYPE_AK47 500
    GIVE_WEAPON_TO_CHAR enemy[1] WEAPONTYPE_MICRO_UZI 500
    GIVE_WEAPON_TO_CHAR enemy[2] WEAPONTYPE_AK47 500
    GIVE_WEAPON_TO_CHAR enemy[3] WEAPONTYPE_MICRO_UZI 500
    GOSUB m4_attack_all
ENDIF
IF wave = 2
    PRINT AF4_12 3000 1
    CREATE_CAR BOBCAT @cp_car@ car[0]
    SET_CAR_HEADING car[0] @cp_car.h@
    CREATE_CHAR_INSIDE_CAR car[0] PEDTYPE_MISSION1 247 enemy[0]
    CREATE_CHAR_AS_PASSENGER car[0] PEDTYPE_MISSION1 248 0 enemy[1]
    CREATE_CHAR PEDTYPE_MISSION1 162 @cp_flank0@ enemy[2]
    CREATE_CHAR PEDTYPE_MISSION1 248 @cp_flank1@ enemy[3]
    GIVE_WEAPON_TO_CHAR enemy[0] WEAPONTYPE_AK47 500
    GIVE_WEAPON_TO_CHAR enemy[1] WEAPONTYPE_AK47 500
    GIVE_WEAPON_TO_CHAR enemy[2] WEAPONTYPE_MICRO_UZI 500
    GIVE_WEAPON_TO_CHAR enemy[3] WEAPONTYPE_AK47 500
    SET_CAR_FORWARD_SPEED car[0] 16.0
    TASK_KILL_CHAR_ON_FOOT enemy[2] scplayer
    TASK_KILL_CHAR_ON_FOOT enemy[3] scplayer
    cartimer = 14
ENDIF
IF wave = 3
    PRINT AF4_12 3000 1
    CREATE_CAR BOBCAT @cp_car2@ car[1]
    SET_CAR_HEADING car[1] @cp_car2.h@
    CREATE_CHAR_INSIDE_CAR car[1] PEDTYPE_MISSION1 248 enemy[0]
    CREATE_CHAR_AS_PASSENGER car[1] PEDTYPE_MISSION1 247 0 enemy[1]
    CREATE_CHAR PEDTYPE_MISSION1 162 @cp_att0@ enemy[2]
    CREATE_CHAR PEDTYPE_MISSION1 247 @cp_att1@ enemy[3]
    CREATE_CHAR PEDTYPE_MISSION1 248 @cp_flank0@ enemy[4]
    CREATE_CHAR PEDTYPE_MISSION1 162 @cp_flank1@ enemy[5]
    GIVE_WEAPON_TO_CHAR enemy[0] WEAPONTYPE_AK47 500
    GIVE_WEAPON_TO_CHAR enemy[1] WEAPONTYPE_AK47 500
    GIVE_WEAPON_TO_CHAR enemy[2] WEAPONTYPE_AK47 500
    GIVE_WEAPON_TO_CHAR enemy[3] WEAPONTYPE_MICRO_UZI 500
    GIVE_WEAPON_TO_CHAR enemy[4] WEAPONTYPE_AK47 500
    GIVE_WEAPON_TO_CHAR enemy[5] WEAPONTYPE_ROCKETLAUNCHER 4
    SET_CAR_FORWARD_SPEED car[1] 16.0
    TASK_KILL_CHAR_ON_FOOT enemy[2] scplayer
    TASK_KILL_CHAR_ON_FOOT enemy[3] scplayer
    TASK_KILL_CHAR_ON_FOOT enemy[4] scplayer
    TASK_KILL_CHAR_ON_FOOT enemy[5] scplayer
    cartimer = 14
ENDIF
k = 0
WHILE k < 6
    IF NOT enemy[k] = -1
        SET_CHAR_ACCURACY enemy[k] 35
        SET_CHAR_DROPS_WEAPONS_WHEN_DEAD enemy[k] FALSE
        ADD_BLIP_FOR_CHAR enemy[k] eblip[k]
    ENDIF
    k += 1
ENDWHILE
IF wave = 3
    SET_CHAR_ACCURACY enemy[5] 12
ENDIF
// the policemen pick their targets
k = 0
WHILE k < 3
    IF NOT IS_CHAR_DEAD ally[k]
        TASK_KILL_CHAR_ON_FOOT ally[k] enemy[k]
    ENDIF
    k += 1
ENDWHILE
RETURN

m4_bail_out:
k = 0
WHILE k < 2
    IF NOT enemy[k] = -1
        IF NOT IS_CHAR_DEAD enemy[k]
            TASK_LEAVE_ANY_CAR enemy[k]
        ENDIF
    ENDIF
    k += 1
ENDWHILE
RETURN

m4_attack_all:
k = 0
WHILE k < 6
    IF NOT enemy[k] = -1
        IF NOT IS_CHAR_DEAD enemy[k]
            TASK_KILL_CHAR_ON_FOOT enemy[k] scplayer
        ENDIF
    ENDIF
    k += 1
ENDWHILE
RETURN

// ---------------------------------------------------------------- held
m4_pass:
PRINT_NOW AF4_11 5000 1
WAIT 5000
PRINT_NOW AF4_05 6000 1
GIVE_WEAPON_TO_CHAR scplayer WEAPONTYPE_AK47 250
WAIT 3000
mission_progress = 4
ADD_SCORE player 1500
PRINT_WITH_NUMBER_BIG M_PASS 1500 5000 1
REGISTER_MISSION_PASSED AF4_T
PLAYER_MADE_PROGRESS 1
PLAY_MISSION_PASSED_TUNE 1
WAIT 5000
PRINT_HELP AF4_13
RETURN

m4_contact:
IF NOT DOES_CHAR_EXIST contact_ped[1]
    CREATE_CHAR PEDTYPE_CIVMALE 288 @police_nasir@ contact_ped[1]
    SET_CHAR_HEADING contact_ped[1] @police_nasir.h@
    SET_CHAR_PROOFS contact_ped[1] TRUE TRUE TRUE TRUE TRUE
    SET_CHAR_NEVER_TARGETTED contact_ped[1] TRUE
    SET_CHAR_STAY_IN_SAME_PLACE contact_ped[1] TRUE
ENDIF
RETURN

m4_cleanup:
IF NOT blip = -1
    REMOVE_BLIP blip
    blip = -1
ENDIF
IF NOT cp = -1
    DELETE_CHECKPOINT cp
    cp = -1
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
k = 0
WHILE k < 3
    IF NOT ally[k] = -1
        MARK_CHAR_AS_NO_LONGER_NEEDED ally[k]
        ally[k] = -1
    ENDIF
    k += 1
ENDWHILE
k = 0
WHILE k < 2
    IF NOT car[k] = -1
        MARK_CAR_AS_NO_LONGER_NEEDED car[k]
        car[k] = -1
    ENDIF
    k += 1
ENDWHILE
IF DOES_CHAR_EXIST contact_ped[1]
    CLEAR_CHAR_TASKS contact_ped[1]
ENDIF
MARK_MODEL_AS_NO_LONGER_NEEDED 288
MARK_MODEL_AS_NO_LONGER_NEEDED 283
MARK_MODEL_AS_NO_LONGER_NEEDED 247
MARK_MODEL_AS_NO_LONGER_NEEDED 248
MARK_MODEL_AS_NO_LONGER_NEEDED 162
MARK_MODEL_AS_NO_LONGER_NEEDED BOBCAT
RESTORE_CAMERA_JUMPCUT
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
flag_player_on_mission = 0
MISSION_HAS_FINISHED
RETURN
}
