// Mission 17 - Dust Storm: the army's Dodo is down on the plain in a sandstorm. Drive out to the wreck, kill
// Haji Gul's men who got there first, find the wounded pilot and bring him back to Camp Sangar before he bleeds
// out, with two pickups hunting you in the dust.
MISSION_START
{
LVAR_INT blip cp stage enemy[6] eblip[6] k alive wreck ecar pilot jeep chaser[2] cdrv[2] cgun[2] cstate[2] retask
LVAR_FLOAT px py pz d
GOSUB ds_main
IF HAS_DEATHARREST_BEEN_EXECUTED
    PRINT_BIG M_FAIL 5000 1
ENDIF
GOSUB ds_cleanup
MISSION_END

ds_main:
SCRIPT_NAME AFM17
blip = -1
cp = -1
wreck = -1
ecar = -1
pilot = -1
jeep = -1
retask = 0
k = 0
WHILE k < 6
    enemy[k] = -1
    eblip[k] = -1
    k += 1
ENDWHILE
k = 0
WHILE k < 2
    chaser[k] = -1
    cdrv[k] = -1
    cgun[k] = -1
    cstate[k] = 0
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
REQUEST_MODEL DODO
REQUEST_MODEL PATRIOT
REQUEST_MODEL BOBCAT
LOAD_ALL_MODELS_NOW
GOSUB ds_contact

// ---------------------------------------------------------------- Hamid at HQ, the storm already blowing
DO_FADE 400 FADE_OUT
WAIT 450
SET_PLAYER_CONTROL player OFF
SWITCH_WIDESCREEN ON
SET_CHAR_COORDINATES scplayer @talk_base_hq@
SET_CHAR_HEADING scplayer @talk_base_hq.h@
SET_CHAR_COORDINATES contact_ped[2] @base_major@
SET_CHAR_HEADING contact_ped[2] @base_major.h@
SET_TIME_OF_DAY 14 0
FORCE_WEATHER_NOW WEATHER_SANDSTORM_DESERT
WAIT 600
SET_FIXED_CAMERA_POSITION @cam_base_hq_w@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_wt@ JUMP_CUT
DO_FADE 400 FADE_IN
PRINT_BIG AFD_T 3000 2
TASK_PLAY_ANIM contact_ped[2] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AFD_01 6000 1
WAIT 6000
SET_FIXED_CAMERA_POSITION @cam_base_hq_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_ct@ JUMP_CUT
PRINT_NOW AFD_02 7000 1
WAIT 7000
SET_FIXED_CAMERA_POSITION @cam_base_hq_p@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_pt@ JUMP_CUT
CLEAR_CHAR_TASKS contact_ped[2]
PRINT_NOW AFD_06 2500 1
WAIT 2500
SET_FIXED_CAMERA_POSITION @cam_base_hq_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_ct@ JUMP_CUT
TASK_PLAY_ANIM contact_ped[2] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AFD_07 6000 1
WAIT 6000
CLEAR_CHAR_TASKS contact_ped[2]
CREATE_CAR PATRIOT @m5_jeep@ jeep
SET_CAR_HEADING jeep @m5_jeep.h@
RESTORE_CAMERA_JUMPCUT
SET_CAMERA_BEHIND_PLAYER
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
ADD_BLIP_FOR_COORD @ds_crash@ blip
CREATE_CHECKPOINT CHECKPOINT_TUBE @ds_crash@ @ds_crash@ 8.0 cp
PRINT_NOW AFD_03 6500 1
stage = 0

// ---------------------------------------------------------------- the wreck
ds_loop:
WAIT 250
GET_CHAR_COORDINATES scplayer px py pz
GET_DISTANCE_BETWEEN_COORDS_2D px py @ds_crash.x@ @ds_crash.y@ d
IF stage = 0
    IF d < 220.0
        stage = 1
        REMOVE_BLIP blip
        blip = -1
        DELETE_CHECKPOINT cp
        cp = -1
        // the wreck: a Dodo that nobody can set alight
        CREATE_CAR DODO @ds_crash@ wreck
        SET_CAR_HEADING wreck @ds_crash.h@
        SET_CAR_PROOFS wreck TRUE TRUE TRUE TRUE TRUE
        SET_CAR_ENGINE_ON wreck FALSE
        LOCK_CAR_DOORS wreck CARLOCK_LOCKED
        ADD_EXPLOSION @ds_crash@ EXPLOSION_SMALL
        CREATE_CHAR PEDTYPE_CIVMALE 287 @ds_pilot@ pilot
        SET_CHAR_HEADING pilot @ds_pilot.h@
        SET_CHAR_PROOFS pilot TRUE TRUE TRUE TRUE TRUE
        SET_CHAR_NEVER_TARGETTED pilot TRUE
        SET_CHAR_STAY_IN_SAME_PLACE pilot TRUE
        TASK_DUCK pilot 600000
        CREATE_CAR BOBCAT @ds_ecar@ ecar
        SET_CAR_HEADING ecar @ds_ecar.h@
        CREATE_CHAR PEDTYPE_MISSION1 247 @ds_e0@ enemy[0]
        CREATE_CHAR PEDTYPE_MISSION1 248 @ds_e1@ enemy[1]
        CREATE_CHAR PEDTYPE_MISSION1 162 @ds_e2@ enemy[2]
        CREATE_CHAR PEDTYPE_MISSION1 200 @ds_e3@ enemy[3]
        CREATE_CHAR PEDTYPE_MISSION1 247 @ds_e4@ enemy[4]
        CREATE_CHAR PEDTYPE_MISSION1 248 @ds_e5@ enemy[5]
        k = 0
        WHILE k < 6
            GIVE_WEAPON_TO_CHAR enemy[k] WEAPONTYPE_AK47 500
            SET_CHAR_ACCURACY enemy[k] 35
            SET_CHAR_DROPS_WEAPONS_WHEN_DEAD enemy[k] FALSE
            ADD_BLIP_FOR_CHAR enemy[k] eblip[k]
            TASK_KILL_CHAR_ON_FOOT enemy[k] scplayer
            k += 1
        ENDWHILE
        PRINT_NOW AFD_08 6000 1
        mission_counter = 6
        DISPLAY_ONSCREEN_COUNTER_WITH_STRING mission_counter COUNTER_DISPLAY_NUMBER AFD_CT
    ENDIF
    GOTO ds_loop
ENDIF
IF stage = 1
    alive = 0
    k = 0
    WHILE k < 6
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
        stage = 2
        ADD_BLIP_FOR_CHAR pilot blip
        PRINT_NOW AFD_09 5000 1
    ENDIF
    GOTO ds_loop
ENDIF
IF stage = 2
    IF LOCATE_CHAR_ON_FOOT_3D scplayer @ds_pilot@ 2.5 2.5 2.5 TRUE
        stage = 3
        REMOVE_BLIP blip
        blip = -1
        CLEAR_CHAR_TASKS pilot
        SET_CHAR_STAY_IN_SAME_PLACE pilot FALSE
        SET_CHAR_PROOFS pilot FALSE FALSE FALSE FALSE FALSE
        SET_CHAR_NEVER_TARGETTED pilot FALSE
        SET_CHAR_HEALTH pilot 200
        SET_GROUP_MEMBER player_group pilot
        SET_CHAR_NEVER_LEAVES_GROUP pilot TRUE
        PRINT_NOW AFD_10 5500 1
        WAIT 5500
        PRINT_NOW AFD_11 6000 1
        ADD_BLIP_FOR_COORD @base_gate@ blip
        CREATE_CHECKPOINT CHECKPOINT_TUBE @base_gate@ @base_gate@ 8.0 cp
        mission_timer = 300000
        DISPLAY_ONSCREEN_TIMER_WITH_STRING mission_timer TIMER_DOWN AFD_TM
    ENDIF
    GOTO ds_loop
ENDIF
// stage 3: the drive back
IF IS_CHAR_DEAD pilot
    CLEAR_ONSCREEN_TIMER mission_timer
    PRINT_NOW AFD_13 4000 1
    PRINT_BIG M_FAIL 5000 1
    RETURN
ENDIF
IF mission_timer < 1
    CLEAR_ONSCREEN_TIMER mission_timer
    PRINT_NOW AFD_14 5000 1
    PRINT_BIG M_FAIL 5000 1
    RETURN
ENDIF
GOSUB ds_chasers
IF IS_CHAR_IN_ANY_CAR scplayer
AND LOCATE_CHAR_IN_CAR_3D scplayer @base_gate@ 9.0 9.0 6.0 FALSE
AND LOCATE_CHAR_ANY_MEANS_3D pilot @base_gate@ 18.0 18.0 8.0 FALSE
    GOSUB ds_arrive
    RETURN
ENDIF
GOTO ds_loop

// ---------------------------------------------------------------- pickups waiting in the dust
ds_chasers:
k = 0
WHILE k < 2
    IF cstate[k] = 0
        IF k = 0
            GET_DISTANCE_BETWEEN_COORDS_2D px py @ds_chase0.x@ @ds_chase0.y@ d
        ELSE
            GET_DISTANCE_BETWEEN_COORDS_2D px py @ds_chase1.x@ @ds_chase1.y@ d
        ENDIF
        IF d < 130.0
            cstate[k] = 1
            IF k = 0
                CREATE_CAR BOBCAT @ds_chase0@ chaser[0]
                SET_CAR_HEADING chaser[0] @ds_chase0.h@
            ELSE
                CREATE_CAR BOBCAT @ds_chase1@ chaser[1]
                SET_CAR_HEADING chaser[1] @ds_chase1.h@
            ENDIF
            CREATE_CHAR_INSIDE_CAR chaser[k] PEDTYPE_MISSION1 247 cdrv[k]
            CREATE_CHAR_AS_PASSENGER chaser[k] PEDTYPE_MISSION1 248 0 cgun[k]
            GIVE_WEAPON_TO_CHAR cgun[k] WEAPONTYPE_AK47 900
            GIVE_WEAPON_TO_CHAR cdrv[k] WEAPONTYPE_PISTOL 200
            SET_CHAR_ACCURACY cgun[k] 30
            SET_CHAR_DROPS_WEAPONS_WHEN_DEAD cgun[k] FALSE
            SET_CHAR_DROPS_WEAPONS_WHEN_DEAD cdrv[k] FALSE
            TASK_CAR_MISSION cdrv[k] chaser[k] -1 3 32.0 3
            TASK_DRIVE_BY cgun[k] scplayer -1 0.0 0.0 0.0 60.0 DRIVEBY_AI_ALL_DIRN TRUE 100
            PRINT_NOW AFD_12 4000 1
        ENDIF
    ENDIF
    k += 1
ENDWHILE
retask += 1
IF retask > 40
    retask = 0
    k = 0
    WHILE k < 2
        IF cstate[k] = 1
            IF NOT IS_CAR_DEAD chaser[k]
            AND NOT IS_CHAR_DEAD cdrv[k]
                TASK_CAR_MISSION cdrv[k] chaser[k] -1 3 32.0 3
            ENDIF
        ENDIF
        k += 1
    ENDWHILE
ENDIF
RETURN

// ---------------------------------------------------------------- the base
ds_arrive:
CLEAR_ONSCREEN_TIMER mission_timer
SET_PLAYER_CONTROL player OFF
SWITCH_WIDESCREEN ON
REMOVE_BLIP blip
blip = -1
DELETE_CHECKPOINT cp
cp = -1
REMOVE_CHAR_FROM_GROUP pilot
SET_CHAR_PROOFS pilot TRUE TRUE TRUE TRUE TRUE
k = 0
WHILE k < 2
    IF NOT chaser[k] = -1
        IF NOT IS_CAR_DEAD chaser[k]
            EXPLODE_CAR chaser[k]
        ENDIF
    ENDIF
    k += 1
ENDWHILE
WAIT 1500
TASK_LEAVE_ANY_CAR pilot
WAIT 2500
PRINT_NOW AFD_15 7000 1
WAIT 7000
RELEASE_WEATHER
FORCE_WEATHER WEATHER_SUNNY_DESERT
RESTORE_CAMERA_JUMPCUT
SET_CAMERA_BEHIND_PLAYER
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
mission_progress = 17
ADD_SCORE player 3500
PRINT_WITH_NUMBER_BIG M_PASS 3500 5000 1
REGISTER_MISSION_PASSED AFD_T
PLAYER_MADE_PROGRESS 1
PLAY_MISSION_PASSED_TUNE 1
RETURN

ds_contact:
IF NOT DOES_CHAR_EXIST contact_ped[2]
    CREATE_CHAR PEDTYPE_CIVMALE 287 @base_major@ contact_ped[2]
    SET_CHAR_HEADING contact_ped[2] @base_major.h@
    SET_CHAR_PROOFS contact_ped[2] TRUE TRUE TRUE TRUE TRUE
    SET_CHAR_NEVER_TARGETTED contact_ped[2] TRUE
    SET_CHAR_STAY_IN_SAME_PLACE contact_ped[2] TRUE
ENDIF
RETURN

ds_cleanup:
CLEAR_ONSCREEN_COUNTER mission_counter
CLEAR_ONSCREEN_TIMER mission_timer
RELEASE_WEATHER
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
WHILE k < 2
    IF NOT cdrv[k] = -1
        MARK_CHAR_AS_NO_LONGER_NEEDED cdrv[k]
        cdrv[k] = -1
    ENDIF
    IF NOT cgun[k] = -1
        MARK_CHAR_AS_NO_LONGER_NEEDED cgun[k]
        cgun[k] = -1
    ENDIF
    IF NOT chaser[k] = -1
        MARK_CAR_AS_NO_LONGER_NEEDED chaser[k]
        chaser[k] = -1
    ENDIF
    k += 1
ENDWHILE
IF NOT pilot = -1
    IF NOT IS_CHAR_DEAD pilot
        REMOVE_CHAR_FROM_GROUP pilot
    ENDIF
    MARK_CHAR_AS_NO_LONGER_NEEDED pilot
    pilot = -1
ENDIF
IF NOT wreck = -1
    MARK_CAR_AS_NO_LONGER_NEEDED wreck
    wreck = -1
ENDIF
IF NOT ecar = -1
    MARK_CAR_AS_NO_LONGER_NEEDED ecar
    ecar = -1
ENDIF
IF NOT jeep = -1
    MARK_CAR_AS_NO_LONGER_NEEDED jeep
    jeep = -1
ENDIF
IF DOES_CHAR_EXIST contact_ped[2]
    CLEAR_CHAR_TASKS contact_ped[2]
ENDIF
MARK_MODEL_AS_NO_LONGER_NEEDED 287
MARK_MODEL_AS_NO_LONGER_NEEDED 247
MARK_MODEL_AS_NO_LONGER_NEEDED 248
MARK_MODEL_AS_NO_LONGER_NEEDED 162
MARK_MODEL_AS_NO_LONGER_NEEDED 200
MARK_MODEL_AS_NO_LONGER_NEEDED DODO
MARK_MODEL_AS_NO_LONGER_NEEDED PATRIOT
MARK_MODEL_AS_NO_LONGER_NEEDED BOBCAT
RESTORE_CAMERA_JUMPCUT
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
flag_player_on_mission = 0
MISSION_HAS_FINISHED
RETURN
}
