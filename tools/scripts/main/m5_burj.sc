// Mission 5 - Burj-e-Khak: the doctor from Shahrak has been kidnapped. Night raid on the ruins (guards on patrol,
// marksmen on the perimeter), free him, get him into a vehicle and drive him home while Black Stone pickups
// try to run you off the road.
MISSION_START
{
LVAR_INT blip cp stage enemy[10] eblip[10] k alive ptimer ptoggle hostage hblip car[2] cdrv[2] cgun[2] cstate[2]
LVAR_FLOAT px py pz d
GOSUB bk_main
IF HAS_DEATHARREST_BEEN_EXECUTED
    PRINT_BIG M_FAIL 5000 1
ENDIF
GOSUB bk_cleanup
MISSION_END

bk_main:
SCRIPT_NAME AFM5
blip = -1
cp = -1
hostage = -1
hblip = -1
ptimer = 0
ptoggle = 0
k = 0
WHILE k < 10
    enemy[k] = -1
    eblip[k] = -1
    k += 1
ENDWHILE
k = 0
WHILE k < 2
    car[k] = -1
    cdrv[k] = -1
    cgun[k] = -1
    cstate[k] = 0
    k += 1
ENDWHILE
flag_player_on_mission = 1
REGISTER_MISSION_GIVEN
WAIT 0
REQUEST_MODEL 288
REQUEST_MODEL 160
REQUEST_MODEL 247
REQUEST_MODEL 248
REQUEST_MODEL 162
REQUEST_MODEL 200
REQUEST_MODEL 355
REQUEST_MODEL 358
REQUEST_MODEL 352
REQUEST_MODEL BOBCAT
LOAD_ALL_MODELS_NOW
GOSUB bk_contact

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
PRINT_BIG AFB_T 3000 2
TASK_PLAY_ANIM contact_ped[1] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AFB_01 5000 1
WAIT 5000
SET_FIXED_CAMERA_POSITION @cam_police_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_police_ct@ JUMP_CUT
PRINT_NOW AFB_02 6000 1
WAIT 6000
SET_FIXED_CAMERA_POSITION @cam_police_p@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_police_pt@ JUMP_CUT
CLEAR_CHAR_TASKS contact_ped[1]
PRINT_NOW AFB_06 2500 1
WAIT 2500
SET_FIXED_CAMERA_POSITION @cam_police_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_police_ct@ JUMP_CUT
TASK_PLAY_ANIM contact_ped[1] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AFB_07 6000 1
WAIT 6000
CLEAR_CHAR_TASKS contact_ped[1]
DO_FADE 800 FADE_OUT
WAIT 900
SET_TIME_OF_DAY 23 0
RESTORE_CAMERA_JUMPCUT
SET_CAMERA_BEHIND_PLAYER
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
DO_FADE 800 FADE_IN
ADD_BLIP_FOR_COORD @ruins@ blip
CREATE_CHECKPOINT CHECKPOINT_TUBE @ruins@ @ruins@ 8.0 cp
PRINT_NOW AFB_03 6000 1
stage = 0

// ---------------------------------------------------------------- the ruins
bk_loop:
WAIT 250
GET_CHAR_COORDINATES scplayer px py pz
GET_DISTANCE_BETWEEN_COORDS_2D px py @site_bk.x@ @site_bk.y@ d
IF stage = 0
    IF d < 260.0
        stage = 1
        REMOVE_BLIP blip
        blip = -1
        DELETE_CHECKPOINT cp
        cp = -1
        CREATE_CHAR PEDTYPE_MISSION1 247 @bk0@ enemy[0]
        CREATE_CHAR PEDTYPE_MISSION1 248 @bk1@ enemy[1]
        CREATE_CHAR PEDTYPE_MISSION1 162 @bk2@ enemy[2]
        CREATE_CHAR PEDTYPE_MISSION1 200 @bk3@ enemy[3]
        CREATE_CHAR PEDTYPE_MISSION1 247 @bk4@ enemy[4]
        CREATE_CHAR PEDTYPE_MISSION1 248 @bk5@ enemy[5]
        CREATE_CHAR PEDTYPE_MISSION1 162 @bk6@ enemy[6]
        CREATE_CHAR PEDTYPE_MISSION1 200 @bk7@ enemy[7]
        CREATE_CHAR PEDTYPE_MISSION1 248 @bk_sn0@ enemy[8]
        CREATE_CHAR PEDTYPE_MISSION1 248 @bk_sn1@ enemy[9]
        k = 0
        WHILE k < 8
            GIVE_WEAPON_TO_CHAR enemy[k] WEAPONTYPE_AK47 500
            SET_CHAR_ACCURACY enemy[k] 40
            k += 1
        ENDWHILE
        GIVE_WEAPON_TO_CHAR enemy[8] WEAPONTYPE_SNIPERRIFLE 60
        GIVE_WEAPON_TO_CHAR enemy[9] WEAPONTYPE_SNIPERRIFLE 60
        SET_CHAR_ACCURACY enemy[8] 70
        SET_CHAR_ACCURACY enemy[9] 70
        k = 0
        WHILE k < 10
            SET_CHAR_DROPS_WEAPONS_WHEN_DEAD enemy[k] FALSE
            SET_CHAR_STAY_IN_SAME_PLACE enemy[k] TRUE
            ADD_BLIP_FOR_CHAR enemy[k] eblip[k]
            k += 1
        ENDWHILE
        CREATE_CHAR PEDTYPE_CIVMALE 160 @bk_hostage@ hostage
        SET_CHAR_HEADING hostage @bk_hostage.h@
        SET_CHAR_PROOFS hostage TRUE TRUE TRUE TRUE TRUE
        SET_CHAR_NEVER_TARGETTED hostage TRUE
        TASK_HANDS_UP hostage 600000
        PRINT_NOW AFB_08 6000 1
        mission_counter = 10
        DISPLAY_ONSCREEN_COUNTER_WITH_STRING mission_counter COUNTER_DISPLAY_NUMBER AFB_CT
        GOSUB bk_patrol
    ENDIF
    GOTO bk_loop
ENDIF
IF stage = 1
    ptimer += 1
    IF ptimer > 24
        ptimer = 0
        GOSUB bk_patrol
    ENDIF
    IF d < 70.0
        stage = 2
    ENDIF
    IF d < 300.0
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
        k = 0
        WHILE k < 8
            IF NOT IS_CHAR_DEAD enemy[k]
                SET_CHAR_STAY_IN_SAME_PLACE enemy[k] FALSE
                TASK_KILL_CHAR_ON_FOOT enemy[k] scplayer
            ENDIF
            k += 1
        ENDWHILE
        // the marksmen stay where they are and keep shooting
        IF NOT IS_CHAR_DEAD enemy[8]
            TASK_KILL_CHAR_ON_FOOT enemy[8] scplayer
        ENDIF
        IF NOT IS_CHAR_DEAD enemy[9]
            TASK_KILL_CHAR_ON_FOOT enemy[9] scplayer
        ENDIF
    ENDIF
ENDIF
IF stage < 3
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
        stage = 3
        ADD_BLIP_FOR_CHAR hostage hblip
        PRINT_NOW AFB_09 6000 1
    ENDIF
    GOTO bk_loop
ENDIF
IF stage = 3
    IF LOCATE_CHAR_ON_FOOT_3D scplayer @bk_hostage@ 2.0 2.0 2.0 TRUE
        stage = 4
        REMOVE_BLIP hblip
        hblip = -1
        CLEAR_CHAR_TASKS hostage
        SET_CHAR_PROOFS hostage FALSE FALSE FALSE FALSE FALSE
        SET_CHAR_HEALTH hostage 150
        SET_GROUP_MEMBER player_group hostage
        SET_CHAR_NEVER_LEAVES_GROUP hostage TRUE
        PRINT_NOW AFB_10 5000 1
        WAIT 5000
        PRINT_NOW AFB_11 6000 1
        ADD_BLIP_FOR_COORD @shahrak@ blip
        CREATE_CHECKPOINT CHECKPOINT_TUBE @shahrak@ @shahrak@ 7.0 cp
    ENDIF
    GOTO bk_loop
ENDIF
// stage 4: the drive home
IF IS_CHAR_DEAD hostage
    PRINT_NOW AFB_13 4000 1
    PRINT_BIG M_FAIL 5000 1
    RETURN
ENDIF
GOSUB bk_chasers
IF IS_CHAR_IN_ANY_CAR scplayer
AND LOCATE_CHAR_IN_CAR_3D scplayer @shahrak@ 7.0 7.0 5.0 FALSE
AND LOCATE_CHAR_ANY_MEANS_3D hostage @shahrak@ 16.0 16.0 8.0 FALSE
    GOSUB bk_arrive
    RETURN
ENDIF
GOTO bk_loop

// ---------------------------------------------------------------- Black Stone pickups waiting on the road home
bk_chasers:
k = 0
WHILE k < 2
    IF cstate[k] = 0
        IF k = 0
            GET_DISTANCE_BETWEEN_COORDS_2D px py @bk_car0.x@ @bk_car0.y@ d
        ELSE
            GET_DISTANCE_BETWEEN_COORDS_2D px py @bk_car1.x@ @bk_car1.y@ d
        ENDIF
        IF d < 170.0
            cstate[k] = 1
            IF k = 0
                CREATE_CAR BOBCAT @bk_car0@ car[0]
                SET_CAR_HEADING car[0] @bk_car0.h@
            ELSE
                CREATE_CAR BOBCAT @bk_car1@ car[1]
                SET_CAR_HEADING car[1] @bk_car1.h@
            ENDIF
            CREATE_CHAR_INSIDE_CAR car[k] PEDTYPE_MISSION1 247 cdrv[k]
            CREATE_CHAR_AS_PASSENGER car[k] PEDTYPE_MISSION1 248 0 cgun[k]
            GIVE_WEAPON_TO_CHAR cgun[k] WEAPONTYPE_MICRO_UZI 900
            GIVE_WEAPON_TO_CHAR cdrv[k] WEAPONTYPE_PISTOL 200
            SET_CHAR_ACCURACY cgun[k] 35
            SET_CHAR_DROPS_WEAPONS_WHEN_DEAD cgun[k] FALSE
            SET_CHAR_DROPS_WEAPONS_WHEN_DEAD cdrv[k] FALSE
            PRINT_NOW AFB_12 4000 1
        ENDIF
    ENDIF
    IF cstate[k] = 1
        IF k = 0
            GET_DISTANCE_BETWEEN_COORDS_2D px py @bk_car0.x@ @bk_car0.y@ d
        ELSE
            GET_DISTANCE_BETWEEN_COORDS_2D px py @bk_car1.x@ @bk_car1.y@ d
        ENDIF
        IF d < 60.0
            cstate[k] = 2
            IF NOT IS_CHAR_DEAD cdrv[k]
                // 3 = MISSION_RAMPLAYER_CLOSE: steers straight at the player, needs no road nodes
                TASK_CAR_MISSION cdrv[k] car[k] -1 3 32.0 3
            ENDIF
            IF NOT IS_CHAR_DEAD cgun[k]
                TASK_DRIVE_BY cgun[k] scplayer -1 0.0 0.0 0.0 60.0 DRIVEBY_AI_ALL_DIRN TRUE 100
            ENDIF
        ENDIF
    ENDIF
    k += 1
ENDWHILE
RETURN

// ---------------------------------------------------------------- the clinic
bk_arrive:
SET_PLAYER_CONTROL player OFF
SWITCH_WIDESCREEN ON
REMOVE_BLIP blip
blip = -1
DELETE_CHECKPOINT cp
cp = -1
REMOVE_CHAR_FROM_GROUP hostage
SET_CHAR_PROOFS hostage TRUE TRUE TRUE TRUE TRUE
SET_FIXED_CAMERA_POSITION @cam_m3_doctor@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_m3_doctor_t@ JUMP_CUT
TASK_LEAVE_ANY_CAR hostage
WAIT 2500
TASK_GO_STRAIGHT_TO_COORD hostage @m3_doctor@ PEDMOVE_WALK 8000
PRINT_NOW AFB_14 6500 1
WAIT 6500
PRINT_NOW AFB_15 5000 1
WAIT 5000
RESTORE_CAMERA_JUMPCUT
SET_CAMERA_BEHIND_PLAYER
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
mission_progress = 5
ADD_SCORE player 2500
PRINT_WITH_NUMBER_BIG M_PASS 2500 5000 1
REGISTER_MISSION_PASSED AFB_T
PLAYER_MADE_PROGRESS 1
PLAY_MISSION_PASSED_TUNE 1
RETURN

// ---------------------------------------------------------------- guard rounds
bk_patrol:
IF ptoggle = 0
    ptoggle = 1
    IF NOT IS_CHAR_DEAD enemy[0]
        TASK_GO_STRAIGHT_TO_COORD enemy[0] @bk4@ PEDMOVE_WALK 12000
    ENDIF
    IF NOT IS_CHAR_DEAD enemy[1]
        TASK_GO_STRAIGHT_TO_COORD enemy[1] @bk5@ PEDMOVE_WALK 12000
    ENDIF
    IF NOT IS_CHAR_DEAD enemy[2]
        TASK_GO_STRAIGHT_TO_COORD enemy[2] @bk6@ PEDMOVE_WALK 12000
    ENDIF
    IF NOT IS_CHAR_DEAD enemy[3]
        TASK_GO_STRAIGHT_TO_COORD enemy[3] @bk7@ PEDMOVE_WALK 12000
    ENDIF
    RETURN
ENDIF
ptoggle = 0
IF NOT IS_CHAR_DEAD enemy[0]
    TASK_GO_STRAIGHT_TO_COORD enemy[0] @bk0@ PEDMOVE_WALK 12000
ENDIF
IF NOT IS_CHAR_DEAD enemy[1]
    TASK_GO_STRAIGHT_TO_COORD enemy[1] @bk1@ PEDMOVE_WALK 12000
ENDIF
IF NOT IS_CHAR_DEAD enemy[2]
    TASK_GO_STRAIGHT_TO_COORD enemy[2] @bk2@ PEDMOVE_WALK 12000
ENDIF
IF NOT IS_CHAR_DEAD enemy[3]
    TASK_GO_STRAIGHT_TO_COORD enemy[3] @bk3@ PEDMOVE_WALK 12000
ENDIF
RETURN

bk_contact:
IF NOT DOES_CHAR_EXIST contact_ped[1]
    CREATE_CHAR PEDTYPE_CIVMALE 288 @police_nasir@ contact_ped[1]
    SET_CHAR_HEADING contact_ped[1] @police_nasir.h@
    SET_CHAR_PROOFS contact_ped[1] TRUE TRUE TRUE TRUE TRUE
    SET_CHAR_NEVER_TARGETTED contact_ped[1] TRUE
    SET_CHAR_STAY_IN_SAME_PLACE contact_ped[1] TRUE
ENDIF
RETURN

bk_cleanup:
CLEAR_ONSCREEN_COUNTER mission_counter
IF NOT blip = -1
    REMOVE_BLIP blip
    blip = -1
ENDIF
IF NOT hblip = -1
    REMOVE_BLIP hblip
    hblip = -1
ENDIF
IF NOT cp = -1
    DELETE_CHECKPOINT cp
    cp = -1
ENDIF
k = 0
WHILE k < 10
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
WHILE k < 2
    IF NOT cdrv[k] = -1
        MARK_CHAR_AS_NO_LONGER_NEEDED cdrv[k]
        cdrv[k] = -1
    ENDIF
    IF NOT cgun[k] = -1
        MARK_CHAR_AS_NO_LONGER_NEEDED cgun[k]
        cgun[k] = -1
    ENDIF
    IF NOT car[k] = -1
        MARK_CAR_AS_NO_LONGER_NEEDED car[k]
        car[k] = -1
    ENDIF
    k += 1
ENDWHILE
IF NOT hostage = -1
    IF NOT IS_CHAR_DEAD hostage
        REMOVE_CHAR_FROM_GROUP hostage
    ENDIF
    MARK_CHAR_AS_NO_LONGER_NEEDED hostage
    hostage = -1
ENDIF
IF DOES_CHAR_EXIST contact_ped[1]
    CLEAR_CHAR_TASKS contact_ped[1]
ENDIF
MARK_MODEL_AS_NO_LONGER_NEEDED 288
MARK_MODEL_AS_NO_LONGER_NEEDED 160
MARK_MODEL_AS_NO_LONGER_NEEDED 247
MARK_MODEL_AS_NO_LONGER_NEEDED 248
MARK_MODEL_AS_NO_LONGER_NEEDED 162
MARK_MODEL_AS_NO_LONGER_NEEDED 200
MARK_MODEL_AS_NO_LONGER_NEEDED BOBCAT
RESTORE_CAMERA_JUMPCUT
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
flag_player_on_mission = 0
MISSION_HAS_FINISHED
RETURN
}
