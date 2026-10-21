// Mission 13 - Ashes (act 2 opener): Haji Gul's Northerners raid the bazaar at night with two pickups and two
// motorbikes. Hold the bazaar with Rahim, then take his pickup and run down the raid leader before he reaches
// Deh-e-Bagh.
MISSION_START
{
LVAR_INT blip cp stage enemy[10] eblip[10] k alive car[2] mbike[2] cartimer pcar leader lcar lgun lblip wp k2 retask alerted
LVAR_FLOAT px py pz d cx cy cz
GOSUB as_main
IF HAS_DEATHARREST_BEEN_EXECUTED
    PRINT_BIG M_FAIL 5000 1
ENDIF
GOSUB as_cleanup
MISSION_END

as_main:
SCRIPT_NAME AFM13
blip = -1
cp = -1
pcar = -1
leader = -1
lcar = -1
lgun = -1
lblip = -1
cartimer = 0
retask = 0
alerted = 0
wp = 0
k = 0
WHILE k < 10
    enemy[k] = -1
    eblip[k] = -1
    k += 1
ENDWHILE
car[0] = -1
car[1] = -1
mbike[0] = -1
mbike[1] = -1
flag_player_on_mission = 1
REGISTER_MISSION_GIVEN
WAIT 0
REQUEST_MODEL 158
REQUEST_MODEL 247
REQUEST_MODEL 248
REQUEST_MODEL 162
REQUEST_MODEL 200
REQUEST_MODEL 355
REQUEST_MODEL 352
REQUEST_MODEL BOBCAT
REQUEST_MODEL SANCHEZ
LOAD_ALL_MODELS_NOW
GOSUB as_contact

// ---------------------------------------------------------------- Rahim at the bazaar: the burned stalls
DO_FADE 400 FADE_OUT
WAIT 450
SET_PLAYER_CONTROL player OFF
SWITCH_WIDESCREEN ON
SET_CHAR_COORDINATES scplayer @talk_bazaar@
SET_CHAR_HEADING scplayer @talk_bazaar.h@
SET_CHAR_COORDINATES contact_ped[0] @bazaar_uncle@
SET_CHAR_HEADING contact_ped[0] @bazaar_uncle.h@
WAIT 600
SET_FIXED_CAMERA_POSITION @cam_bazaar_w@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_bazaar_wt@ JUMP_CUT
DO_FADE 400 FADE_IN
PRINT_BIG AFG_ACT 4000 2
WAIT 4000
PRINT_BIG AFA_T 3000 2
TASK_PLAY_ANIM contact_ped[0] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AFA_01 7000 1
WAIT 7000
SET_FIXED_CAMERA_POSITION @cam_bazaar_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_bazaar_ct@ JUMP_CUT
PRINT_NOW AFA_02 7500 1
WAIT 7500
SET_FIXED_CAMERA_POSITION @cam_bazaar_p@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_bazaar_pt@ JUMP_CUT
CLEAR_CHAR_TASKS contact_ped[0]
PRINT_NOW AFA_06 2500 1
WAIT 2500
SET_FIXED_CAMERA_POSITION @cam_bazaar_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_bazaar_ct@ JUMP_CUT
TASK_PLAY_ANIM contact_ped[0] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AFA_07 5500 1
WAIT 5500
CLEAR_CHAR_TASKS contact_ped[0]
DO_FADE 800 FADE_OUT
WAIT 900
// nightfall: the raid
SET_TIME_OF_DAY 22 30
GIVE_WEAPON_TO_CHAR scplayer WEAPONTYPE_AK47 240
SET_CURRENT_CHAR_WEAPON scplayer WEAPONTYPE_AK47
CREATE_CAR BOBCAT @as_pcar@ pcar
SET_CAR_HEADING pcar @as_pcar.h@
GOSUB as_spawn_raid
RESTORE_CAMERA_JUMPCUT
SET_CAMERA_BEHIND_PLAYER
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
DO_FADE 800 FADE_IN
PRINT_NOW AFA_08 6000 1
mission_counter = 10
DISPLAY_ONSCREEN_COUNTER_WITH_STRING mission_counter COUNTER_DISPLAY_NUMBER AFA_CT
stage = 1

// ---------------------------------------------------------------- the raid
as_loop:
WAIT 250
GET_CHAR_COORDINATES scplayer px py pz
IF stage = 1
    // the bikers ride in, then dismount and fight on foot
    IF cartimer > 0
        cartimer -= 1
        IF cartimer = 0
            k = 4
            WHILE k < 6
                IF NOT IS_CHAR_DEAD enemy[k]
                    TASK_LEAVE_ANY_CAR enemy[k]
                ENDIF
                k += 1
            ENDWHILE
        ENDIF
    ENDIF
    retask += 1
    IF retask = 30
        retask = 0
        k = 4
        WHILE k < 6
            IF NOT IS_CHAR_DEAD enemy[k]
                IF NOT IS_CHAR_IN_ANY_CAR enemy[k]
                    TASK_KILL_CHAR_ON_FOOT enemy[k] scplayer
                ENDIF
            ENDIF
            k += 1
        ENDWHILE
        // the pickup crews bail out when the truck is wrecked or they are far from the fight
        k = 0
        WHILE k < 4
            IF NOT IS_CHAR_DEAD enemy[k]
                IF IS_CHAR_IN_ANY_CAR enemy[k]
                    GET_DISTANCE_BETWEEN_COORDS_2D px py @bazaar.x@ @bazaar.y@ d
                    IF alerted > 3
                        TASK_LEAVE_ANY_CAR enemy[k]
                    ENDIF
                ELSE
                    TASK_KILL_CHAR_ON_FOOT enemy[k] scplayer
                ENDIF
            ENDIF
            k += 1
        ENDWHILE
        alerted += 1
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
        GOSUB as_leader_flees
        stage = 2
    ENDIF
    GOTO as_loop
ENDIF
// stage 2: the chase down the Deh-e-Bagh road
IF IS_CHAR_IN_ANY_CAR scplayer
    IF NOT blip = -1
        REMOVE_BLIP blip
        blip = -1
    ENDIF
ENDIF
IF IS_CAR_DEAD lcar
OR IS_CHAR_DEAD leader
    GOSUB as_pass
    RETURN
ENDIF
retask += 1
GET_CAR_COORDINATES lcar cx cy cz
k2 = wp
GET_DISTANCE_BETWEEN_COORDS_2D cx cy qb_x[k2] qb_y[k2] d
IF d < 24.0
    wp += 1
    k2 = wp
    IF k2 < qb_count
        GOSUB as_drive
    ENDIF
ELSE
    IF retask > 15
        retask = 0
        GOSUB as_drive
    ENDIF
ENDIF
k2 = qb_count
k2 -= 2
IF wp >= k2
    PRINT_NOW AFA_10 5000 1
    PRINT_BIG M_FAIL 5000 1
    RETURN
ENDIF
GOTO as_loop

// ---------------------------------------------------------------- helpers
as_drive:
TASK_CAR_DRIVE_TO_COORD leader lcar qb_x[k2] qb_y[k2] qb_z[k2] 15.0 MODE_STRAIGHTLINE -1 3
RETURN

as_spawn_raid:
// two pickups from the valley road, each with a driver and a gunner
CREATE_CAR BOBCAT @as_car0@ car[0]
SET_CAR_HEADING car[0] @as_car0.h@
CREATE_CAR BOBCAT @as_car1@ car[1]
SET_CAR_HEADING car[1] @as_car1.h@
CREATE_CHAR_INSIDE_CAR car[0] PEDTYPE_MISSION1 247 enemy[0]
CREATE_CHAR_AS_PASSENGER car[0] PEDTYPE_MISSION1 248 0 enemy[1]
CREATE_CHAR_INSIDE_CAR car[1] PEDTYPE_MISSION1 162 enemy[2]
CREATE_CHAR_AS_PASSENGER car[1] PEDTYPE_MISSION1 200 0 enemy[3]
// two motorbikes from the Deh-e-Bagh road
CREATE_CAR SANCHEZ @as_bike0@ mbike[0]
SET_CAR_HEADING mbike[0] @as_bike0.h@
CREATE_CAR SANCHEZ @as_bike1@ mbike[1]
SET_CAR_HEADING mbike[1] @as_bike1.h@
CREATE_CHAR_INSIDE_CAR mbike[0] PEDTYPE_MISSION1 247 enemy[4]
CREATE_CHAR_INSIDE_CAR mbike[1] PEDTYPE_MISSION1 248 enemy[5]
// four on foot already in the lanes
CREATE_CHAR PEDTYPE_MISSION1 162 @as_foot0@ enemy[6]
CREATE_CHAR PEDTYPE_MISSION1 200 @as_foot1@ enemy[7]
CREATE_CHAR PEDTYPE_MISSION1 247 @as_foot2@ enemy[8]
CREATE_CHAR PEDTYPE_MISSION1 248 @as_foot3@ enemy[9]
k = 0
WHILE k < 10
    GIVE_WEAPON_TO_CHAR enemy[k] WEAPONTYPE_AK47 500
    SET_CHAR_ACCURACY enemy[k] 35
    SET_CHAR_DROPS_WEAPONS_WHEN_DEAD enemy[k] FALSE
    ADD_BLIP_FOR_CHAR enemy[k] eblip[k]
    k += 1
ENDWHILE
GIVE_WEAPON_TO_CHAR enemy[1] WEAPONTYPE_MICRO_UZI 900
GIVE_WEAPON_TO_CHAR enemy[3] WEAPONTYPE_MICRO_UZI 900
SET_CURRENT_CHAR_WEAPON enemy[1] WEAPONTYPE_MICRO_UZI
SET_CURRENT_CHAR_WEAPON enemy[3] WEAPONTYPE_MICRO_UZI
// 3 = MISSION_RAMPLAYER_CLOSE: steers straight at the player, needs no road nodes
TASK_CAR_MISSION enemy[0] car[0] -1 3 28.0 3
TASK_CAR_MISSION enemy[2] car[1] -1 3 28.0 3
TASK_DRIVE_BY enemy[1] scplayer -1 0.0 0.0 0.0 60.0 DRIVEBY_AI_ALL_DIRN TRUE 100
TASK_DRIVE_BY enemy[3] scplayer -1 0.0 0.0 0.0 60.0 DRIVEBY_AI_ALL_DIRN TRUE 100
TASK_CAR_MISSION enemy[4] mbike[0] -1 3 30.0 3
TASK_CAR_MISSION enemy[5] mbike[1] -1 3 30.0 3
k = 6
WHILE k < 10
    TASK_KILL_CHAR_ON_FOOT enemy[k] scplayer
    k += 1
ENDWHILE
cartimer = 24
RETURN

as_leader_flees:
CREATE_CAR BOBCAT @as_flee@ lcar
SET_CAR_HEADING lcar @as_flee.h@
SET_CAR_HEALTH lcar 1500
CREATE_CHAR_INSIDE_CAR lcar PEDTYPE_MISSION1 200 leader
CREATE_CHAR_AS_PASSENGER lcar PEDTYPE_MISSION1 247 0 lgun
GIVE_WEAPON_TO_CHAR leader WEAPONTYPE_PISTOL 100
GIVE_WEAPON_TO_CHAR lgun WEAPONTYPE_AK47 900
SET_CHAR_ACCURACY lgun 35
SET_CHAR_DROPS_WEAPONS_WHEN_DEAD leader FALSE
SET_CHAR_DROPS_WEAPONS_WHEN_DEAD lgun FALSE
TASK_DRIVE_BY lgun scplayer -1 0.0 0.0 0.0 70.0 DRIVEBY_AI_ALL_DIRN TRUE 100
ADD_BLIP_FOR_CAR lcar lblip
CHANGE_BLIP_SCALE lblip 3
wp = 0
k2 = 0
retask = 0
GOSUB as_drive
IF NOT IS_CAR_DEAD pcar
    ADD_BLIP_FOR_CAR pcar blip
ENDIF
PRINT_NOW AFA_09 7000 1
RETURN

as_pass:
IF NOT lblip = -1
    REMOVE_BLIP lblip
    lblip = -1
ENDIF
WAIT 2500
PRINT_NOW AFA_11 5000 1
WAIT 5000
PRINT_NOW AFA_12 6500 1
WAIT 3000
mission_progress = 13
ADD_SCORE player 3000
PRINT_WITH_NUMBER_BIG M_PASS 3000 5000 1
REGISTER_MISSION_PASSED AFA_T
PLAYER_MADE_PROGRESS 1
PLAY_MISSION_PASSED_TUNE 1
RETURN

as_contact:
IF NOT DOES_CHAR_EXIST contact_ped[0]
    CREATE_CHAR PEDTYPE_CIVMALE 158 @bazaar_uncle@ contact_ped[0]
    SET_CHAR_HEADING contact_ped[0] @bazaar_uncle.h@
    SET_CHAR_PROOFS contact_ped[0] TRUE TRUE TRUE TRUE TRUE
    SET_CHAR_NEVER_TARGETTED contact_ped[0] TRUE
    SET_CHAR_STAY_IN_SAME_PLACE contact_ped[0] TRUE
ENDIF
RETURN

as_cleanup:
CLEAR_ONSCREEN_COUNTER mission_counter
IF NOT blip = -1
    REMOVE_BLIP blip
    blip = -1
ENDIF
IF NOT lblip = -1
    REMOVE_BLIP lblip
    lblip = -1
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
    IF NOT car[k] = -1
        MARK_CAR_AS_NO_LONGER_NEEDED car[k]
        car[k] = -1
    ENDIF
    IF NOT mbike[k] = -1
        MARK_CAR_AS_NO_LONGER_NEEDED mbike[k]
        mbike[k] = -1
    ENDIF
    k += 1
ENDWHILE
IF NOT leader = -1
    MARK_CHAR_AS_NO_LONGER_NEEDED leader
    leader = -1
ENDIF
IF NOT lgun = -1
    MARK_CHAR_AS_NO_LONGER_NEEDED lgun
    lgun = -1
ENDIF
IF NOT lcar = -1
    MARK_CAR_AS_NO_LONGER_NEEDED lcar
    lcar = -1
ENDIF
IF NOT pcar = -1
    MARK_CAR_AS_NO_LONGER_NEEDED pcar
    pcar = -1
ENDIF
IF DOES_CHAR_EXIST contact_ped[0]
    CLEAR_CHAR_TASKS contact_ped[0]
ENDIF
MARK_MODEL_AS_NO_LONGER_NEEDED 158
MARK_MODEL_AS_NO_LONGER_NEEDED 247
MARK_MODEL_AS_NO_LONGER_NEEDED 248
MARK_MODEL_AS_NO_LONGER_NEEDED 162
MARK_MODEL_AS_NO_LONGER_NEEDED 200
MARK_MODEL_AS_NO_LONGER_NEEDED BOBCAT
MARK_MODEL_AS_NO_LONGER_NEEDED SANCHEZ
RESTORE_CAMERA_JUMPCUT
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
flag_player_on_mission = 0
MISSION_HAS_FINISHED
RETURN
}
