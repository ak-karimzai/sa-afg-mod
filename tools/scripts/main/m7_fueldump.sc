// Mission 7 - Fuel and Fire: the Black Stone fuel dump in the occupied village of Sar-e-Kotal. Sneak up at dusk,
// plant charges on three fuel stacks (or shoot the barrels), survive the reinforcements, get clear before it blows.
MISSION_START
{
LVAR_INT blip cp stage enemy[12] eblip[12] k alive ptimer ptoggle barrel[12] planted[3] left alarm car cartimer pc
LVAR_FLOAT px py pz d bx by bz
GOSUB fd_main
IF HAS_DEATHARREST_BEEN_EXECUTED
    PRINT_BIG M_FAIL 5000 1
ENDIF
GOSUB fd_cleanup
MISSION_END

fd_main:
SCRIPT_NAME AFM7
blip = -1
cp = -1
car = -1
cartimer = 0
alarm = 0
left = 3
pc = -1
ptimer = 0
ptoggle = 0
k = 0
WHILE k < 12
    enemy[k] = -1
    eblip[k] = -1
    barrel[k] = -1
    k += 1
ENDWHILE
k = 0
WHILE k < 3
    planted[k] = 0
    k += 1
ENDWHILE
flag_player_on_mission = 1
REGISTER_MISSION_GIVEN
WAIT 0
REQUEST_MODEL 287
REQUEST_MODEL 247
REQUEST_MODEL 248
REQUEST_MODEL 162
REQUEST_MODEL 200
REQUEST_MODEL 355
REQUEST_MODEL 349
REQUEST_MODEL 1225
REQUEST_MODEL BOBCAT
LOAD_ALL_MODELS_NOW
GOSUB fd_contact

// ---------------------------------------------------------------- briefing at HQ
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
PRINT_BIG AFF_T 3000 2
TASK_PLAY_ANIM contact_ped[2] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AFF_01 6500 1
WAIT 6500
SET_FIXED_CAMERA_POSITION @cam_base_hq_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_ct@ JUMP_CUT
PRINT_NOW AFF_02 5500 1
WAIT 5500
SET_FIXED_CAMERA_POSITION @cam_base_hq_p@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_pt@ JUMP_CUT
CLEAR_CHAR_TASKS contact_ped[2]
PRINT_NOW AFF_06 3000 1
WAIT 3000
SET_FIXED_CAMERA_POSITION @cam_base_hq_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_ct@ JUMP_CUT
TASK_PLAY_ANIM contact_ped[2] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AFF_07 6500 1
WAIT 6500
CLEAR_CHAR_TASKS contact_ped[2]
DO_FADE 800 FADE_OUT
WAIT 900
SET_TIME_OF_DAY 19 30
RESTORE_CAMERA_JUMPCUT
SET_CAMERA_BEHIND_PLAYER
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
DO_FADE 800 FADE_IN
ADD_BLIP_FOR_COORD @sar_e_kotal@ blip
PRINT_NOW AFF_03 6000 1
stage = 0

// ---------------------------------------------------------------- the village
fd_loop:
WAIT 250
GET_CHAR_COORDINATES scplayer px py pz
GET_DISTANCE_BETWEEN_COORDS_2D px py @site_sk.x@ @site_sk.y@ d
IF stage = 0
    IF d < 280.0
        stage = 1
        REMOVE_BLIP blip
        blip = -1
        CREATE_CHAR PEDTYPE_MISSION1 247 @fdg0@ enemy[0]
        CREATE_CHAR PEDTYPE_MISSION1 248 @fdg1@ enemy[1]
        CREATE_CHAR PEDTYPE_MISSION1 162 @fdg2@ enemy[2]
        CREATE_CHAR PEDTYPE_MISSION1 200 @fdg3@ enemy[3]
        CREATE_CHAR PEDTYPE_MISSION1 247 @fdg4@ enemy[4]
        CREATE_CHAR PEDTYPE_MISSION1 248 @fdg5@ enemy[5]
        CREATE_CHAR PEDTYPE_MISSION1 162 @fdg6@ enemy[6]
        CREATE_CHAR PEDTYPE_MISSION1 200 @fdg7@ enemy[7]
        k = 0
        WHILE k < 8
            GIVE_WEAPON_TO_CHAR enemy[k] WEAPONTYPE_AK47 500
            SET_CHAR_ACCURACY enemy[k] 40
            SET_CHAR_DROPS_WEAPONS_WHEN_DEAD enemy[k] FALSE
            SET_CHAR_STAY_IN_SAME_PLACE enemy[k] TRUE
            ADD_BLIP_FOR_CHAR enemy[k] eblip[k]
            k += 1
        ENDWHILE
        GIVE_WEAPON_TO_CHAR enemy[3] WEAPONTYPE_SHOTGUN 100
        GOSUB fd_barrels
        PRINT_NOW AFF_08 7000 1
        mission_counter = 3
        DISPLAY_ONSCREEN_COUNTER_WITH_STRING mission_counter COUNTER_DISPLAY_NUMBER AFF_CT
        GOSUB fd_patrol
    ENDIF
    GOTO fd_loop
ENDIF
IF alarm = 0
    ptimer += 1
    IF ptimer > 28
        ptimer = 0
        GOSUB fd_patrol
    ENDIF
    IF d < 45.0
        GOSUB fd_alarm
    ENDIF
    IF d < 300.0
    AND IS_CHAR_SHOOTING scplayer
        GOSUB fd_alarm
    ENDIF
    k = 0
    WHILE k < 8
        IF HAS_CHAR_BEEN_DAMAGED_BY_CHAR enemy[k] scplayer
            GOSUB fd_alarm
            k = 8
        ENDIF
        k += 1
    ENDWHILE
ENDIF
IF cartimer > 0
    cartimer -= 1
    IF cartimer = 8
        IF NOT IS_CHAR_DEAD enemy[8]
            TASK_LEAVE_ANY_CAR enemy[8]
        ENDIF
        IF NOT IS_CHAR_DEAD enemy[9]
            TASK_LEAVE_ANY_CAR enemy[9]
        ENDIF
    ENDIF
    IF cartimer = 0
        IF NOT IS_CHAR_DEAD enemy[8]
            TASK_KILL_CHAR_ON_FOOT enemy[8] scplayer
        ENDIF
        IF NOT IS_CHAR_DEAD enemy[9]
            TASK_KILL_CHAR_ON_FOOT enemy[9] scplayer
        ENDIF
    ENDIF
ENDIF
// blips of the dead, count the living
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
IF stage = 1
    GOSUB fd_plant
    IF left = 0
        stage = 2
        CLEAR_ONSCREEN_COUNTER mission_counter
        CLEAR_ONSCREEN_COUNTER mission_bar
        PRINT_NOW AFF_10 6000 1
        mission_timer = 30000
        DISPLAY_ONSCREEN_TIMER_WITH_STRING mission_timer TIMER_DOWN AFF_TM
        IF alarm = 0
            GOSUB fd_alarm
        ENDIF
    ENDIF
    GOTO fd_loop
ENDIF
IF stage = 2
    IF mission_timer < 1
        CLEAR_ONSCREEN_TIMER mission_timer
        GOSUB fd_detonate
        PRINT_NOW AFF_13 6000 1
        stage = 3
    ENDIF
    GOTO fd_loop
ENDIF
// stage 3: get clear or finish them
IF alive = 0
OR d > 300.0
    PRINT_NOW AFF_11 5500 1
    WAIT 5500
    PRINT_NOW AFF_12 5000 1
    WAIT 2000
    mission_progress = 7
    ADD_SCORE player 3000
    PRINT_WITH_NUMBER_BIG M_PASS 3000 5000 1
    REGISTER_MISSION_PASSED AFF_T
    PLAYER_MADE_PROGRESS 1
    PLAY_MISSION_PASSED_TUNE 1
    RETURN
ENDIF
GOTO fd_loop

// ---------------------------------------------------------------- planting: stand in a marker for 3 seconds
fd_plant:
k = -1
IF planted[0] = 0
    IF LOCATE_CHAR_ON_FOOT_3D scplayer @fd0@ 2.5 2.5 2.5 TRUE
        k = 0
    ENDIF
ENDIF
IF planted[1] = 0
    IF LOCATE_CHAR_ON_FOOT_3D scplayer @fd1@ 2.5 2.5 2.5 TRUE
        k = 1
    ENDIF
ENDIF
IF planted[2] = 0
    IF LOCATE_CHAR_ON_FOOT_3D scplayer @fd2@ 2.5 2.5 2.5 TRUE
        k = 2
    ENDIF
ENDIF
IF k = -1
    IF NOT pc = -1
        CLEAR_ONSCREEN_COUNTER mission_bar
        pc = -1
    ENDIF
    RETURN
ENDIF
IF NOT pc = k
    pc = k
    mission_bar = 0
    DISPLAY_ONSCREEN_COUNTER_WITH_STRING mission_bar COUNTER_DISPLAY_BAR AFF_PL
ENDIF
mission_bar += 9
IF mission_bar >= 100
    planted[k] = 1
    left -= 1
    mission_counter = left
    CLEAR_ONSCREEN_COUNTER mission_bar
    pc = -1
ENDIF
RETURN

// ---------------------------------------------------------------- the alarm: everybody attacks, a pickup comes up the road
fd_alarm:
IF alarm = 1
    RETURN
ENDIF
alarm = 1
PRINT_NOW AFF_09 5000 1
k = 0
WHILE k < 8
    IF NOT IS_CHAR_DEAD enemy[k]
        SET_CHAR_STAY_IN_SAME_PLACE enemy[k] FALSE
        TASK_KILL_CHAR_ON_FOOT enemy[k] scplayer
    ENDIF
    k += 1
ENDWHILE
CREATE_CAR BOBCAT @fd_car@ car
SET_CAR_HEADING car @fd_car.h@
CREATE_CHAR_INSIDE_CAR car PEDTYPE_MISSION1 247 enemy[8]
CREATE_CHAR_AS_PASSENGER car PEDTYPE_MISSION1 248 0 enemy[9]
CREATE_CHAR PEDTYPE_MISSION1 162 @fd_r0@ enemy[10]
CREATE_CHAR PEDTYPE_MISSION1 200 @fd_r1@ enemy[11]
k = 8
WHILE k < 12
    GIVE_WEAPON_TO_CHAR enemy[k] WEAPONTYPE_AK47 500
    SET_CHAR_ACCURACY enemy[k] 40
    SET_CHAR_DROPS_WEAPONS_WHEN_DEAD enemy[k] FALSE
    ADD_BLIP_FOR_CHAR enemy[k] eblip[k]
    k += 1
ENDWHILE
TASK_KILL_CHAR_ON_FOOT enemy[10] scplayer
TASK_KILL_CHAR_ON_FOOT enemy[11] scplayer
SET_CAR_FORWARD_SPEED car 16.0
cartimer = 14
RETURN

// ---------------------------------------------------------------- the fuel stacks: 4 explosive barrels at each point
fd_barrels:
k = 0
WHILE k < 12
    IF k < 4
        bx = @fd0.x@
        by = @fd0.y@
        bz = @fd0.z@
    ELSE
        IF k < 8
            bx = @fd1.x@
            by = @fd1.y@
            bz = @fd1.z@
        ELSE
            bx = @fd2.x@
            by = @fd2.y@
            bz = @fd2.z@
        ENDIF
    ENDIF
    bz -= 0.5
    IF k = 0
    OR k = 4
    OR k = 8
        bx += 0.9
        by += 0.9
    ENDIF
    IF k = 1
    OR k = 5
    OR k = 9
        bx -= 0.9
        by += 0.9
    ENDIF
    IF k = 2
    OR k = 6
    OR k = 10
        bx += 0.9
        by -= 0.9
    ENDIF
    IF k = 3
    OR k = 7
    OR k = 11
        bx -= 0.9
        by -= 0.9
    ENDIF
    CREATE_OBJECT 1225 bx by bz barrel[k]
    k += 1
ENDWHILE
RETURN

fd_detonate:
ADD_EXPLOSION @fd0@ EXPLOSION_ROCKET
WAIT 400
ADD_EXPLOSION @fd1@ EXPLOSION_ROCKET
WAIT 400
ADD_EXPLOSION @fd2@ EXPLOSION_ROCKET
WAIT 600
ADD_EXPLOSION @fd0@ EXPLOSION_CAR
WAIT 300
ADD_EXPLOSION @fd2@ EXPLOSION_CAR
WAIT 300
ADD_EXPLOSION @fd1@ EXPLOSION_CAR
RETURN

// ---------------------------------------------------------------- guard rounds
fd_patrol:
IF ptoggle = 0
    ptoggle = 1
    IF NOT IS_CHAR_DEAD enemy[0]
        TASK_GO_STRAIGHT_TO_COORD enemy[0] @fdg4@ PEDMOVE_WALK 12000
    ENDIF
    IF NOT IS_CHAR_DEAD enemy[1]
        TASK_GO_STRAIGHT_TO_COORD enemy[1] @fdg5@ PEDMOVE_WALK 12000
    ENDIF
    IF NOT IS_CHAR_DEAD enemy[2]
        TASK_GO_STRAIGHT_TO_COORD enemy[2] @fdg6@ PEDMOVE_WALK 12000
    ENDIF
    IF NOT IS_CHAR_DEAD enemy[3]
        TASK_GO_STRAIGHT_TO_COORD enemy[3] @fdg7@ PEDMOVE_WALK 12000
    ENDIF
    RETURN
ENDIF
ptoggle = 0
IF NOT IS_CHAR_DEAD enemy[0]
    TASK_GO_STRAIGHT_TO_COORD enemy[0] @fdg0@ PEDMOVE_WALK 12000
ENDIF
IF NOT IS_CHAR_DEAD enemy[1]
    TASK_GO_STRAIGHT_TO_COORD enemy[1] @fdg1@ PEDMOVE_WALK 12000
ENDIF
IF NOT IS_CHAR_DEAD enemy[2]
    TASK_GO_STRAIGHT_TO_COORD enemy[2] @fdg2@ PEDMOVE_WALK 12000
ENDIF
IF NOT IS_CHAR_DEAD enemy[3]
    TASK_GO_STRAIGHT_TO_COORD enemy[3] @fdg3@ PEDMOVE_WALK 12000
ENDIF
RETURN

fd_contact:
IF NOT DOES_CHAR_EXIST contact_ped[2]
    CREATE_CHAR PEDTYPE_CIVMALE 287 @base_major@ contact_ped[2]
    SET_CHAR_HEADING contact_ped[2] @base_major.h@
    SET_CHAR_PROOFS contact_ped[2] TRUE TRUE TRUE TRUE TRUE
    SET_CHAR_NEVER_TARGETTED contact_ped[2] TRUE
    SET_CHAR_STAY_IN_SAME_PLACE contact_ped[2] TRUE
ENDIF
RETURN

fd_cleanup:
CLEAR_ONSCREEN_COUNTER mission_counter
CLEAR_ONSCREEN_COUNTER mission_bar
CLEAR_ONSCREEN_TIMER mission_timer
IF NOT blip = -1
    REMOVE_BLIP blip
    blip = -1
ENDIF
IF NOT cp = -1
    DELETE_CHECKPOINT cp
    cp = -1
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
    IF NOT barrel[k] = -1
        IF DOES_OBJECT_EXIST barrel[k]
            DELETE_OBJECT barrel[k]
        ENDIF
        barrel[k] = -1
    ENDIF
    k += 1
ENDWHILE
IF NOT car = -1
    MARK_CAR_AS_NO_LONGER_NEEDED car
    car = -1
ENDIF
IF DOES_CHAR_EXIST contact_ped[2]
    CLEAR_CHAR_TASKS contact_ped[2]
ENDIF
MARK_MODEL_AS_NO_LONGER_NEEDED 287
MARK_MODEL_AS_NO_LONGER_NEEDED 247
MARK_MODEL_AS_NO_LONGER_NEEDED 248
MARK_MODEL_AS_NO_LONGER_NEEDED 162
MARK_MODEL_AS_NO_LONGER_NEEDED 200
MARK_MODEL_AS_NO_LONGER_NEEDED 1225
MARK_MODEL_AS_NO_LONGER_NEEDED BOBCAT
RESTORE_CAMERA_JUMPCUT
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
flag_player_on_mission = 0
MISSION_HAS_FINISHED
RETURN
}
