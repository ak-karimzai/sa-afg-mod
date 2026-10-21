// Mission 21 - Flying Carpet: Haji Gul's ledger sits in the courier's sedan in the fortress courtyard, too
// narrow to land in and ringed with RPGs. Take the Leviathan, hover over the sedan until the magnet takes it,
// and carry the whole car back to the motor pool at Camp Sangar.
MISSION_START
{
LVAR_INT heli blip cp stage sedan sblip enemy[7] eblip[7] k held away defenders settle
LVAR_FLOAT px py pz d hx hy hz cx cy cz
GOSUB fc_main
IF HAS_DEATHARREST_BEEN_EXECUTED
    PRINT_BIG M_FAIL 5000 1
ENDIF
GOSUB fc_cleanup
MISSION_END

fc_main:
SCRIPT_NAME AFM21
heli = -1
blip = -1
cp = -1
sedan = -1
sblip = -1
held = 0
away = 0
defenders = 0
settle = 0
k = 0
WHILE k < 7
    enemy[k] = -1
    eblip[k] = -1
    k += 1
ENDWHILE
flag_player_on_mission = 1
REGISTER_MISSION_GIVEN
WAIT 0
REQUEST_MODEL 287
REQUEST_MODEL 247
REQUEST_MODEL 248
REQUEST_MODEL 355
REQUEST_MODEL 359
REQUEST_MODEL LEVIATHN
REQUEST_MODEL SENTINEL
LOAD_ALL_MODELS_NOW
GOSUB fc_contact
CREATE_CAR LEVIATHN @base_heli@ heli
SET_CAR_HEADING heli @base_heli.h@
CREATE_CAR SENTINEL @fc_car@ sedan
SET_CAR_HEADING sedan @fc_car.h@
SET_CAR_HEALTH sedan 1800
LOCK_CAR_DOORS sedan CARLOCK_LOCKED

// ---------------------------------------------------------------- Hamid at HQ
DO_FADE 400 FADE_OUT
WAIT 450
SET_PLAYER_CONTROL player OFF
SWITCH_WIDESCREEN ON
SET_CHAR_COORDINATES scplayer @talk_base_hq@
SET_CHAR_HEADING scplayer @talk_base_hq.h@
SET_CHAR_COORDINATES contact_ped[2] @base_major@
SET_CHAR_HEADING contact_ped[2] @base_major.h@
SET_TIME_OF_DAY 12 0
WAIT 600
SET_FIXED_CAMERA_POSITION @cam_base_hq_w@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_wt@ JUMP_CUT
DO_FADE 400 FADE_IN
PRINT_BIG AFM_T 3000 2
TASK_PLAY_ANIM contact_ped[2] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AFM_01 7500 1
WAIT 7500
SET_FIXED_CAMERA_POSITION @cam_base_hq_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_ct@ JUMP_CUT
PRINT_NOW AFM_02 7500 1
WAIT 7500
SET_FIXED_CAMERA_POSITION @cam_base_hq_p@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_pt@ JUMP_CUT
CLEAR_CHAR_TASKS contact_ped[2]
PRINT_NOW AFM_06 2500 1
WAIT 2500
SET_FIXED_CAMERA_POSITION @cam_base_hq_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_ct@ JUMP_CUT
TASK_PLAY_ANIM contact_ped[2] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AFM_07 7000 1
WAIT 7000
CLEAR_CHAR_TASKS contact_ped[2]
RESTORE_CAMERA_JUMPCUT
SET_CAMERA_BEHIND_PLAYER
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
ADD_BLIP_FOR_CAR heli blip
PRINT_NOW AFM_03 5000 1
stage = 0

// ---------------------------------------------------------------- the lift
fc_loop:
WAIT 250
IF IS_CAR_DEAD heli
    PRINT_NOW AFM_13 4000 1
    PRINT_BIG M_FAIL 5000 1
    RETURN
ENDIF
IF IS_CAR_DEAD sedan
    PRINT_NOW AFM_12 5000 1
    PRINT_BIG M_FAIL 5000 1
    RETURN
ENDIF
GET_CHAR_COORDINATES scplayer px py pz
IF stage = 0
    IF IS_CHAR_IN_CAR scplayer heli
        stage = 1
        REMOVE_BLIP blip
        blip = -1
        ADD_BLIP_FOR_CAR sedan sblip
        CHANGE_BLIP_SCALE sblip 3
        PRINT_NOW AFM_08 7000 1
    ENDIF
    GOTO fc_loop
ENDIF
// out of the helicopter?
IF NOT IS_CHAR_IN_CAR scplayer heli
    IF away = 0
        away = 1
        ADD_BLIP_FOR_CAR heli blip
        PRINT_NOW AFM_14 4000 1
    ENDIF
ELSE
    IF away = 1
        away = 0
        REMOVE_BLIP blip
        blip = -1
    ENDIF
ENDIF
// the fortress wakes up when the helicopter comes close
IF defenders = 0
    GET_DISTANCE_BETWEEN_COORDS_2D px py @fortress_keep.x@ @fortress_keep.y@ d
    IF d < 260.0
        GOSUB fc_defenders
    ENDIF
ENDIF
// is the sedan hanging under the helicopter?
GET_CAR_COORDINATES heli hx hy hz
GET_CAR_COORDINATES sedan cx cy cz
GET_DISTANCE_BETWEEN_COORDS_2D hx hy cx cy d
hz -= cz
// hanging under the helicopter: close below it, and (while still at the fortress) clear of the courtyard floor
GET_DISTANCE_BETWEEN_COORDS_2D cx cy @fc_car.x@ @fc_car.y@ px
IF px < 90.0
    cz -= @fc_car.z@
    IF cz < 2.5
        hz = 0.0
    ENDIF
ENDIF
IF d < 8.0
AND hz > 2.5
AND hz < 24.0
    IF held = 0
        held = 1
        settle = 0
        IF NOT sblip = -1
            REMOVE_BLIP sblip
            sblip = -1
        ENDIF
        IF NOT cp = -1
            DELETE_CHECKPOINT cp
            cp = -1
        ENDIF
        ADD_BLIP_FOR_COORD @fc_drop@ cp
        PRINT_NOW AFM_10 6000 1
    ENDIF
ELSE
    IF held = 1
        held = 0
        IF NOT cp = -1
            REMOVE_BLIP cp
            cp = -1
        ENDIF
        // set down at the motor pool, or dropped somewhere else?
        IF LOCATE_CAR_3D sedan @fc_drop@ 28.0 28.0 15.0 FALSE
            stage = 2
        ELSE
            ADD_BLIP_FOR_CAR sedan sblip
            CHANGE_BLIP_SCALE sblip 3
            PRINT_NOW AFM_11 5000 1
        ENDIF
    ENDIF
ENDIF
IF stage = 2
    // let it come to rest, then count it as delivered
    settle += 1
    IF settle > 12
        IF LOCATE_CAR_3D sedan @fc_drop@ 28.0 28.0 15.0 FALSE
            GOSUB fc_pass
            RETURN
        ENDIF
        stage = 1
    ENDIF
ENDIF
GOTO fc_loop

// ---------------------------------------------------------------- the walls
fc_defenders:
defenders = 1
PRINT_NOW AFM_09 5000 1
CREATE_CHAR PEDTYPE_MISSION1 248 @fort_rpg0@ enemy[0]
CREATE_CHAR PEDTYPE_MISSION1 248 @fort_rpg1@ enemy[1]
CREATE_CHAR PEDTYPE_MISSION1 248 @fort_rpg2@ enemy[2]
CREATE_CHAR PEDTYPE_MISSION1 247 @fort0@ enemy[3]
CREATE_CHAR PEDTYPE_MISSION1 247 @fort1@ enemy[4]
CREATE_CHAR PEDTYPE_MISSION1 247 @fort6@ enemy[5]
CREATE_CHAR PEDTYPE_MISSION1 247 @fort7@ enemy[6]
k = 0
WHILE k < 7
    IF k < 3
        GIVE_WEAPON_TO_CHAR enemy[k] WEAPONTYPE_ROCKETLAUNCHER 12
        SET_CHAR_ACCURACY enemy[k] 10
    ELSE
        GIVE_WEAPON_TO_CHAR enemy[k] WEAPONTYPE_AK47 800
        SET_CHAR_ACCURACY enemy[k] 40
    ENDIF
    SET_CHAR_DROPS_WEAPONS_WHEN_DEAD enemy[k] FALSE
    ADD_BLIP_FOR_CHAR enemy[k] eblip[k]
    TASK_KILL_CHAR_ON_FOOT enemy[k] scplayer
    k += 1
ENDWHILE
RETURN

// ---------------------------------------------------------------- the ledger
fc_pass:
SET_PLAYER_CONTROL player OFF
SWITCH_WIDESCREEN ON
WAIT 1500
PRINT_NOW AFM_15 7500 1
WAIT 7500
RESTORE_CAMERA_JUMPCUT
SET_CAMERA_BEHIND_PLAYER
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
mission_progress = 21
ADD_SCORE player 6000
PRINT_WITH_NUMBER_BIG M_PASS 6000 5000 1
REGISTER_MISSION_PASSED AFM_T
PLAYER_MADE_PROGRESS 1
PLAY_MISSION_PASSED_TUNE 1
RETURN

fc_contact:
IF NOT DOES_CHAR_EXIST contact_ped[2]
    CREATE_CHAR PEDTYPE_CIVMALE 287 @base_major@ contact_ped[2]
    SET_CHAR_HEADING contact_ped[2] @base_major.h@
    SET_CHAR_PROOFS contact_ped[2] TRUE TRUE TRUE TRUE TRUE
    SET_CHAR_NEVER_TARGETTED contact_ped[2] TRUE
    SET_CHAR_STAY_IN_SAME_PLACE contact_ped[2] TRUE
ENDIF
RETURN

fc_cleanup:
IF NOT blip = -1
    REMOVE_BLIP blip
    blip = -1
ENDIF
IF NOT sblip = -1
    REMOVE_BLIP sblip
    sblip = -1
ENDIF
IF NOT cp = -1
    REMOVE_BLIP cp
    cp = -1
ENDIF
k = 0
WHILE k < 7
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
IF NOT sedan = -1
    MARK_CAR_AS_NO_LONGER_NEEDED sedan
    sedan = -1
ENDIF
IF NOT heli = -1
    MARK_CAR_AS_NO_LONGER_NEEDED heli
    heli = -1
ENDIF
IF DOES_CHAR_EXIST contact_ped[2]
    CLEAR_CHAR_TASKS contact_ped[2]
ENDIF
MARK_MODEL_AS_NO_LONGER_NEEDED 287
MARK_MODEL_AS_NO_LONGER_NEEDED 247
MARK_MODEL_AS_NO_LONGER_NEEDED 248
MARK_MODEL_AS_NO_LONGER_NEEDED LEVIATHN
MARK_MODEL_AS_NO_LONGER_NEEDED SENTINEL
RESTORE_CAMERA_JUMPCUT
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
flag_player_on_mission = 0
MISSION_HAS_FINISHED
RETURN
}
