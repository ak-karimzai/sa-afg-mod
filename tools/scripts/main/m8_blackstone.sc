// Mission 8 - Black Stone: raid the bandit camp at Sang-e-Siah at first light. Guards patrol until they
// notice you; afterwards recover the stolen aid.
MISSION_START
{
LVAR_INT blip stage enemy[12] eblip[12] k alive ptimer ptoggle loot lblip car cartimer
LVAR_FLOAT px py pz d
GOSUB m6_main
IF HAS_DEATHARREST_BEEN_EXECUTED
    PRINT_BIG M_FAIL 5000 1
ENDIF
GOSUB m6_cleanup
MISSION_END

m6_main:
SCRIPT_NAME AFM8
blip = -1
car = -1
cartimer = 0
loot = -1
lblip = -1
ptimer = 0
ptoggle = 0
k = 0
WHILE k < 12
    enemy[k] = -1
    eblip[k] = -1
    k += 1
ENDWHILE
flag_player_on_mission = 1
REGISTER_MISSION_GIVEN
WAIT 0
REQUEST_MODEL 288
REQUEST_MODEL 247
REQUEST_MODEL 248
REQUEST_MODEL 162
REQUEST_MODEL 200
REQUEST_MODEL 355
REQUEST_MODEL 349
REQUEST_MODEL 359
REQUEST_MODEL 1210
REQUEST_MODEL BOBCAT
LOAD_ALL_MODELS_NOW
GOSUB m6_contact

// ---------------------------------------------------------------- briefing
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
PRINT_BIG AF6_T 3000 2
TASK_PLAY_ANIM contact_ped[1] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AF6_01 5000 1
WAIT 5000
SET_FIXED_CAMERA_POSITION @cam_police_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_police_ct@ JUMP_CUT
PRINT_NOW AF6_02 5500 1
WAIT 5500
SET_FIXED_CAMERA_POSITION @cam_police_p@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_police_pt@ JUMP_CUT
CLEAR_CHAR_TASKS contact_ped[1]
PRINT_NOW AF6_06 2500 1
WAIT 2500
SET_FIXED_CAMERA_POSITION @cam_police_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_police_ct@ JUMP_CUT
TASK_PLAY_ANIM contact_ped[1] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AF6_07 5500 1
WAIT 5500
CLEAR_CHAR_TASKS contact_ped[1]
ADD_ARMOUR_TO_CHAR scplayer 50
// first light
DO_FADE 800 FADE_OUT
WAIT 900
SET_TIME_OF_DAY 5 30
RESTORE_CAMERA_JUMPCUT
SET_CAMERA_BEHIND_PLAYER
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
DO_FADE 800 FADE_IN
ADD_BLIP_FOR_COORD @bandit_camp@ blip
PRINT_NOW AF6_03 6000 1
stage = 0

// ---------------------------------------------------------------- the camp
m6_loop:
WAIT 250
GET_CHAR_COORDINATES scplayer px py pz
GET_DISTANCE_BETWEEN_COORDS_2D px py @bandit_camp.x@ @bandit_camp.y@ d
IF stage = 0
    IF d < 300.0
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
            SET_CHAR_DROPS_WEAPONS_WHEN_DEAD enemy[k] FALSE
            ADD_BLIP_FOR_CHAR enemy[k] eblip[k]
            k += 1
        ENDWHILE
        GIVE_WEAPON_TO_CHAR enemy[3] WEAPONTYPE_SHOTGUN 100
        GIVE_WEAPON_TO_CHAR enemy[7] WEAPONTYPE_ROCKETLAUNCHER 5
        SET_CHAR_ACCURACY enemy[7] 15
        REMOVE_BLIP blip
        blip = -1
        PRINT_NOW AF6_09 5000 1
        mission_counter = 10
        DISPLAY_ONSCREEN_COUNTER_WITH_STRING mission_counter COUNTER_DISPLAY_NUMBER AF6_04
        GOSUB m6_patrol
    ENDIF
    GOTO m6_loop
ENDIF
IF stage = 1
    // guards walk their rounds until somebody notices you
    ptimer += 1
    IF ptimer > 28
        ptimer = 0
        GOSUB m6_patrol
    ENDIF
    IF d < 80.0
        stage = 2
    ENDIF
    IF d < 260.0
    AND IS_CHAR_SHOOTING scplayer
        stage = 2
    ENDIF
    k = 0
    WHILE k < 10
        IF HAS_CHAR_BEEN_DAMAGED_BY_CHAR enemy[k] scplayer
            stage = 2
        ENDIF
        k += 1
    ENDWHILE
    IF stage = 2
        PRINT_NOW AF6_10 4000 1
        k = 0
        WHILE k < 10
            IF NOT IS_CHAR_DEAD enemy[k]
                TASK_KILL_CHAR_ON_FOOT enemy[k] scplayer
            ENDIF
            k += 1
        ENDWHILE
        // a pickup with two more men comes up from the valley
        PRINT AF6_11 4000 1
        CREATE_CAR BOBCAT @camp_car@ car
        SET_CAR_HEADING car @camp_car.h@
        CREATE_CHAR_INSIDE_CAR car PEDTYPE_MISSION1 247 enemy[10]
        CREATE_CHAR_AS_PASSENGER car PEDTYPE_MISSION1 248 0 enemy[11]
        GIVE_WEAPON_TO_CHAR enemy[10] WEAPONTYPE_AK47 500
        GIVE_WEAPON_TO_CHAR enemy[11] WEAPONTYPE_AK47 500
        SET_CHAR_ACCURACY enemy[10] 40
        SET_CHAR_ACCURACY enemy[11] 40
        SET_CHAR_DROPS_WEAPONS_WHEN_DEAD enemy[10] FALSE
        SET_CHAR_DROPS_WEAPONS_WHEN_DEAD enemy[11] FALSE
        ADD_BLIP_FOR_CHAR enemy[10] eblip[10]
        ADD_BLIP_FOR_CHAR enemy[11] eblip[11]
        SET_CAR_FORWARD_SPEED car 16.0
        cartimer = 14
    ENDIF
ENDIF
IF cartimer > 0
    cartimer -= 1
    IF cartimer = 8
        IF NOT IS_CHAR_DEAD enemy[10]
            TASK_LEAVE_ANY_CAR enemy[10]
        ENDIF
        IF NOT IS_CHAR_DEAD enemy[11]
            TASK_LEAVE_ANY_CAR enemy[11]
        ENDIF
    ENDIF
    IF cartimer = 0
        IF NOT IS_CHAR_DEAD enemy[10]
            TASK_KILL_CHAR_ON_FOOT enemy[10] scplayer
        ENDIF
        IF NOT IS_CHAR_DEAD enemy[11]
            TASK_KILL_CHAR_ON_FOOT enemy[11] scplayer
        ENDIF
    ENDIF
ENDIF
IF stage < 3
    alive = 0
    k = 0
    WHILE k < 12
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
    mission_counter = alive
    IF alive = 0
        CLEAR_ONSCREEN_COUNTER mission_counter
        stage = 3
        CREATE_PICKUP 1210 PICKUP_ONCE @m6_loot@ loot
        ADD_BLIP_FOR_PICKUP loot lblip
        PRINT_NOW AF6_08 6000 1
    ENDIF
    GOTO m6_loop
ENDIF
// stage 3: collect the stolen aid
IF HAS_PICKUP_BEEN_COLLECTED loot
    REMOVE_BLIP lblip
    lblip = -1
    REMOVE_PICKUP loot
    loot = -1
    WAIT 1500
    PRINT_NOW AF6_05 7000 1
    WAIT 3000
    mission_progress = 8
    ADD_SCORE player 3000
    PRINT_WITH_NUMBER_BIG M_PASS 3000 5000 1
    REGISTER_MISSION_PASSED AF6_T
    PLAYER_MADE_PROGRESS 1
    PLAY_MISSION_PASSED_TUNE 1
    RETURN
ENDIF
GOTO m6_loop

// ---------------------------------------------------------------- guard rounds: each guard walks to another guard's post
m6_patrol:
IF ptoggle = 0
    ptoggle = 1
    IF NOT IS_CHAR_DEAD enemy[0]
        TASK_GO_STRAIGHT_TO_COORD enemy[0] @camp5@ PEDMOVE_WALK 12000
    ENDIF
    IF NOT IS_CHAR_DEAD enemy[1]
        TASK_GO_STRAIGHT_TO_COORD enemy[1] @camp6@ PEDMOVE_WALK 12000
    ENDIF
    IF NOT IS_CHAR_DEAD enemy[2]
        TASK_GO_STRAIGHT_TO_COORD enemy[2] @camp7@ PEDMOVE_WALK 12000
    ENDIF
    IF NOT IS_CHAR_DEAD enemy[4]
        TASK_GO_STRAIGHT_TO_COORD enemy[4] @camp9@ PEDMOVE_WALK 12000
    ENDIF
    IF NOT IS_CHAR_DEAD enemy[8]
        TASK_GO_STRAIGHT_TO_COORD enemy[8] @camp3@ PEDMOVE_WALK 12000
    ENDIF
    RETURN
ENDIF
ptoggle = 0
IF NOT IS_CHAR_DEAD enemy[0]
    TASK_GO_STRAIGHT_TO_COORD enemy[0] @camp0@ PEDMOVE_WALK 12000
ENDIF
IF NOT IS_CHAR_DEAD enemy[1]
    TASK_GO_STRAIGHT_TO_COORD enemy[1] @camp1@ PEDMOVE_WALK 12000
ENDIF
IF NOT IS_CHAR_DEAD enemy[2]
    TASK_GO_STRAIGHT_TO_COORD enemy[2] @camp2@ PEDMOVE_WALK 12000
ENDIF
IF NOT IS_CHAR_DEAD enemy[4]
    TASK_GO_STRAIGHT_TO_COORD enemy[4] @camp4@ PEDMOVE_WALK 12000
ENDIF
IF NOT IS_CHAR_DEAD enemy[8]
    TASK_GO_STRAIGHT_TO_COORD enemy[8] @camp8@ PEDMOVE_WALK 12000
ENDIF
RETURN

m6_contact:
IF NOT DOES_CHAR_EXIST contact_ped[1]
    CREATE_CHAR PEDTYPE_CIVMALE 288 @police_nasir@ contact_ped[1]
    SET_CHAR_HEADING contact_ped[1] @police_nasir.h@
    SET_CHAR_PROOFS contact_ped[1] TRUE TRUE TRUE TRUE TRUE
    SET_CHAR_NEVER_TARGETTED contact_ped[1] TRUE
    SET_CHAR_STAY_IN_SAME_PLACE contact_ped[1] TRUE
ENDIF
RETURN

m6_cleanup:
CLEAR_ONSCREEN_COUNTER mission_counter
IF NOT blip = -1
    REMOVE_BLIP blip
    blip = -1
ENDIF
IF NOT lblip = -1
    REMOVE_BLIP lblip
    lblip = -1
ENDIF
IF NOT loot = -1
    REMOVE_PICKUP loot
    loot = -1
ENDIF
k = 0
WHILE k < 12
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
IF NOT car = -1
    MARK_CAR_AS_NO_LONGER_NEEDED car
    car = -1
ENDIF
IF DOES_CHAR_EXIST contact_ped[1]
    CLEAR_CHAR_TASKS contact_ped[1]
ENDIF
MARK_MODEL_AS_NO_LONGER_NEEDED 288
MARK_MODEL_AS_NO_LONGER_NEEDED 247
MARK_MODEL_AS_NO_LONGER_NEEDED 248
MARK_MODEL_AS_NO_LONGER_NEEDED 162
MARK_MODEL_AS_NO_LONGER_NEEDED 200
MARK_MODEL_AS_NO_LONGER_NEEDED 1210
MARK_MODEL_AS_NO_LONGER_NEEDED BOBCAT
RESTORE_CAMERA_JUMPCUT
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
flag_player_on_mission = 0
MISSION_HAS_FINISHED
RETURN
}
