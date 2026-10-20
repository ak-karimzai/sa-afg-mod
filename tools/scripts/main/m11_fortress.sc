// Mission 11 - Qala-e-Kohna: lead an army squad up the mountain before dawn, breach the gate, storm the
// fortress. When his guard is nearly gone Siahdast runs for a pickup and flees down the mountain road: chase him
// in the squad's jeep and finish him before he reaches the valley.
MISSION_START
{
LVAR_INT boss bblip stage enemy[14] eblip[14] ally[4] k k2 alive taunted blip cp fled bcar gunner jeep jblip wp retask
LVAR_FLOAT px py pz d cx cy cz
GOSUB m8_main
IF HAS_DEATHARREST_BEEN_EXECUTED
    PRINT_BIG M_FAIL 5000 1
ENDIF
GOSUB m8_cleanup
MISSION_END

m8_main:
SCRIPT_NAME AFM11
boss = -1
bblip = -1
blip = -1
cp = -1
fled = 0
bcar = -1
gunner = -1
jeep = -1
jblip = -1
wp = 0
retask = 0
k = 0
WHILE k < 14
    enemy[k] = -1
    eblip[k] = -1
    k += 1
ENDWHILE
k = 0
WHILE k < 4
    ally[k] = -1
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
REQUEST_MODEL 356
REQUEST_MODEL 359
REQUEST_MODEL BOBCAT
REQUEST_MODEL PATRIOT
LOAD_ALL_MODELS_NOW
GOSUB m8_contact

// ---------------------------------------------------------------- briefing
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
PRINT_BIG AF8_T 3000 2
TASK_PLAY_ANIM contact_ped[2] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AF8_01 5000 1
WAIT 5000
SET_FIXED_CAMERA_POSITION @cam_base_hq_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_ct@ JUMP_CUT
PRINT_NOW AF8_06 6000 1
WAIT 6000
SET_FIXED_CAMERA_POSITION @cam_base_hq_p@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_pt@ JUMP_CUT
CLEAR_CHAR_TASKS contact_ped[2]
PRINT_NOW AF8_07 3000 1
WAIT 3000
DO_FADE 1000 FADE_OUT
WAIT 1100
SET_TIME_OF_DAY 4 30
// the army trucks drop the squad below the fortress gate
LOAD_SCENE @fort_start@
SET_CHAR_COORDINATES scplayer @fort_start@
SET_CHAR_HEADING scplayer 0.0
GIVE_WEAPON_TO_CHAR scplayer WEAPONTYPE_M4 400
SET_CURRENT_CHAR_WEAPON scplayer WEAPONTYPE_M4
ADD_ARMOUR_TO_CHAR scplayer 100
CREATE_CHAR PEDTYPE_MISSION2 287 @fort_ally0@ ally[0]
CREATE_CHAR PEDTYPE_MISSION2 287 @fort_ally1@ ally[1]
CREATE_CHAR PEDTYPE_MISSION2 287 @fort_ally2@ ally[2]
CREATE_CHAR PEDTYPE_MISSION2 287 @fort_ally3@ ally[3]
k = 0
WHILE k < 4
    GIVE_WEAPON_TO_CHAR ally[k] WEAPONTYPE_M4 1000
    SET_CHAR_HEALTH ally[k] 250
    SET_CHAR_ACCURACY ally[k] 60
    SET_CHAR_DROPS_WEAPONS_WHEN_DEAD ally[k] FALSE
    SET_GROUP_MEMBER player_group ally[k]
    SET_CHAR_NEVER_LEAVES_GROUP ally[k] TRUE
    k += 1
ENDWHILE
SET_GROUP_SEPARATION_RANGE player_group 60.0
CREATE_CHAR PEDTYPE_MISSION1 247 @fort0@ enemy[0]
CREATE_CHAR PEDTYPE_MISSION1 248 @fort1@ enemy[1]
CREATE_CHAR PEDTYPE_MISSION1 162 @fort2@ enemy[2]
CREATE_CHAR PEDTYPE_MISSION1 200 @fort3@ enemy[3]
CREATE_CHAR PEDTYPE_MISSION1 247 @fort4@ enemy[4]
CREATE_CHAR PEDTYPE_MISSION1 248 @fort5@ enemy[5]
CREATE_CHAR PEDTYPE_MISSION1 162 @fort6@ enemy[6]
CREATE_CHAR PEDTYPE_MISSION1 200 @fort7@ enemy[7]
CREATE_CHAR PEDTYPE_MISSION1 247 @fort8@ enemy[8]
CREATE_CHAR PEDTYPE_MISSION1 248 @fort9@ enemy[9]
CREATE_CHAR PEDTYPE_MISSION1 162 @fort10@ enemy[10]
CREATE_CHAR PEDTYPE_MISSION1 248 @fort_rpg0@ enemy[11]
CREATE_CHAR PEDTYPE_MISSION1 247 @fort_rpg1@ enemy[12]
CREATE_CHAR PEDTYPE_MISSION1 200 @fort_rpg2@ enemy[13]
k = 0
WHILE k < 14
    GIVE_WEAPON_TO_CHAR enemy[k] WEAPONTYPE_AK47 500
    SET_CHAR_ACCURACY enemy[k] 40
    SET_CHAR_DROPS_WEAPONS_WHEN_DEAD enemy[k] FALSE
    SET_CHAR_STAY_IN_SAME_PLACE enemy[k] TRUE
    ADD_BLIP_FOR_CHAR enemy[k] eblip[k]
    k += 1
ENDWHILE
GIVE_WEAPON_TO_CHAR enemy[4] WEAPONTYPE_ROCKETLAUNCHER 6
GIVE_WEAPON_TO_CHAR enemy[11] WEAPONTYPE_ROCKETLAUNCHER 6
SET_CHAR_ACCURACY enemy[11] 15
CREATE_CAR PATRIOT @fort_jeep@ jeep
SET_CAR_HEADING jeep @fort_jeep.h@
CREATE_CHAR PEDTYPE_MISSION1 248 @fort_boss@ boss
SET_CHAR_HEADING boss @fort_boss.h@
SET_CHAR_MAX_HEALTH boss 600
SET_CHAR_HEALTH boss 600
ADD_ARMOUR_TO_CHAR boss 100
GIVE_WEAPON_TO_CHAR boss WEAPONTYPE_AK47 1000
SET_CHAR_ACCURACY boss 55
SET_CHAR_DROPS_WEAPONS_WHEN_DEAD boss FALSE
SET_CHAR_STAY_IN_SAME_PLACE boss TRUE
ADD_BLIP_FOR_CHAR boss bblip
CHANGE_BLIP_SCALE bblip 3
RESTORE_CAMERA_JUMPCUT
SWITCH_WIDESCREEN OFF
SET_CAMERA_BEHIND_PLAYER
WAIT 500
DO_FADE 1500 FADE_IN
SET_PLAYER_CONTROL player ON
PRINT_NOW AF8_08 6000 1
ADD_BLIP_FOR_COORD @fortress_gate@ blip
CREATE_CHECKPOINT CHECKPOINT_TUBE @fortress_gate@ @fortress_gate@ 5.0 cp
mission_counter = 15
DISPLAY_ONSCREEN_COUNTER_WITH_STRING mission_counter COUNTER_DISPLAY_NUMBER AF8_04
stage = 0
taunted = 0

// ---------------------------------------------------------------- the assault
m8_loop:
WAIT 250
GET_CHAR_COORDINATES scplayer px py pz
IF stage = 0
    GET_DISTANCE_BETWEEN_COORDS_2D px py @fortress_gate.x@ @fortress_gate.y@ d
    IF d < 30.0
        GOSUB m8_breach
    ENDIF
ENDIF
GET_DISTANCE_BETWEEN_COORDS_2D px py @fort_boss.x@ @fort_boss.y@ d
IF stage = 0
AND d < 75.0
    // they saw us coming
    GOSUB m8_breach
ENDIF
IF taunted = 0
AND d < 30.0
    taunted = 1
    PRINT_NOW AF8_03 5000 1
    IF NOT IS_CHAR_DEAD boss
    AND fled = 0
        SET_CHAR_STAY_IN_SAME_PLACE boss FALSE
        TASK_KILL_CHAR_ON_FOOT boss scplayer
    ENDIF
ENDIF
alive = 0
k = 0
WHILE k < 14
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
IF NOT IS_CHAR_DEAD boss
    mission_counter += 1
ENDIF
// Siahdast runs for his pickup when his guard is nearly gone
IF fled = 0
AND stage = 1
AND alive < 5
AND NOT IS_CHAR_DEAD boss
    GOSUB m8_flee
ENDIF
IF fled = 1
    GOSUB m8_chase
    IF wp < 2
        PRINT_NOW AF8_14 5000 1
        PRINT_BIG M_FAIL 5000 1
        RETURN
    ENDIF
ENDIF
IF IS_CHAR_DEAD boss
    IF NOT bblip = -1
        REMOVE_BLIP bblip
        bblip = -1
    ENDIF
    CLEAR_ONSCREEN_COUNTER mission_counter
    WAIT 2000
    PRINT_NOW AF8_15 6000 1
    WAIT 6000
    mission_progress = 11
    ADD_SCORE player 10000
    PRINT_WITH_NUMBER_BIG M_PASS 10000 5000 1
    REGISTER_MISSION_PASSED AF8_T
    PLAYER_MADE_PROGRESS 1
    PLAY_MISSION_PASSED_TUNE 1
    RETURN
ENDIF
GOTO m8_loop

// ---------------------------------------------------------------- the gate goes, the defenders come out
m8_breach:
stage = 1
IF NOT blip = -1
    REMOVE_BLIP blip
    blip = -1
ENDIF
IF NOT cp = -1
    DELETE_CHECKPOINT cp
    cp = -1
ENDIF
ADD_EXPLOSION @fortress_gate@ EXPLOSION_ROCKET
PRINT_NOW AF8_09 4000 1
PRINT AF8_16 4000 1
PRINT AF8_02 5000 1
GIVE_WEAPON_TO_CHAR scplayer WEAPONTYPE_ROCKETLAUNCHER 4
k = 0
WHILE k < 14
    IF NOT IS_CHAR_DEAD enemy[k]
        SET_CHAR_STAY_IN_SAME_PLACE enemy[k] FALSE
        TASK_KILL_CHAR_ON_FOOT enemy[k] scplayer
    ENDIF
    k += 1
ENDWHILE
RETURN

// ---------------------------------------------------------------- Siahdast's getaway
m8_flee:
fled = 1
CREATE_CAR BOBCAT @fort_gate_car@ bcar
SET_CAR_HEADING bcar @fort_gate_car.h@
SET_CAR_HEALTH bcar 1600
CLEAR_CHAR_TASKS boss
SET_CHAR_STAY_IN_SAME_PLACE boss FALSE
WARP_CHAR_INTO_CAR boss bcar
CREATE_CHAR_AS_PASSENGER bcar PEDTYPE_MISSION1 247 0 gunner
GIVE_WEAPON_TO_CHAR gunner WEAPONTYPE_AK47 900
SET_CHAR_ACCURACY gunner 35
SET_CHAR_DROPS_WEAPONS_WHEN_DEAD gunner FALSE
TASK_DRIVE_BY gunner scplayer -1 0.0 0.0 0.0 60.0 DRIVEBY_AI_ALL_DIRN TRUE 100
wp = fr_count
wp -= 2
k2 = wp
GOSUB m8_drive
retask = 0
PRINT_NOW AF8_13 6000 1
ADD_BLIP_FOR_CAR jeep jblip
RETURN

m8_chase:
IF IS_CHAR_IN_CAR scplayer jeep
    IF NOT jblip = -1
        REMOVE_BLIP jblip
        jblip = -1
    ENDIF
ENDIF
IF IS_CAR_DEAD bcar
    RETURN
ENDIF
IF NOT IS_CHAR_IN_CAR boss bcar
    RETURN
ENDIF
retask += 1
GET_CAR_COORDINATES bcar cx cy cz
k2 = wp
GET_DISTANCE_BETWEEN_COORDS_2D cx cy fr_x[k2] fr_y[k2] d
IF d < 24.0
    wp -= 1
    k2 = wp
    IF k2 >= 2
        GOSUB m8_drive
    ENDIF
ELSE
    IF retask > 15
        retask = 0
        GOSUB m8_drive
    ENDIF
ENDIF
RETURN

m8_drive:
TASK_CAR_DRIVE_TO_COORD boss bcar fr_x[k2] fr_y[k2] fr_z[k2] 18.0 MODE_STRAIGHTLINE -1 3
RETURN

m8_contact:
IF NOT DOES_CHAR_EXIST contact_ped[2]
    CREATE_CHAR PEDTYPE_CIVMALE 287 @base_major@ contact_ped[2]
    SET_CHAR_HEADING contact_ped[2] @base_major.h@
    SET_CHAR_PROOFS contact_ped[2] TRUE TRUE TRUE TRUE TRUE
    SET_CHAR_NEVER_TARGETTED contact_ped[2] TRUE
    SET_CHAR_STAY_IN_SAME_PLACE contact_ped[2] TRUE
ENDIF
RETURN

m8_cleanup:
CLEAR_ONSCREEN_COUNTER mission_counter
IF NOT blip = -1
    REMOVE_BLIP blip
    blip = -1
ENDIF
IF NOT cp = -1
    DELETE_CHECKPOINT cp
    cp = -1
ENDIF
k = 0
WHILE k < 14
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
IF NOT jblip = -1
    REMOVE_BLIP jblip
    jblip = -1
ENDIF
IF NOT gunner = -1
    MARK_CHAR_AS_NO_LONGER_NEEDED gunner
    gunner = -1
ENDIF
IF NOT bcar = -1
    MARK_CAR_AS_NO_LONGER_NEEDED bcar
    bcar = -1
ENDIF
IF NOT jeep = -1
    MARK_CAR_AS_NO_LONGER_NEEDED jeep
    jeep = -1
ENDIF
k = 0
WHILE k < 4
    IF NOT ally[k] = -1
        IF NOT IS_CHAR_DEAD ally[k]
            REMOVE_CHAR_FROM_GROUP ally[k]
        ENDIF
        MARK_CHAR_AS_NO_LONGER_NEEDED ally[k]
        ally[k] = -1
    ENDIF
    k += 1
ENDWHILE
IF NOT bblip = -1
    REMOVE_BLIP bblip
    bblip = -1
ENDIF
IF NOT boss = -1
    MARK_CHAR_AS_NO_LONGER_NEEDED boss
    boss = -1
ENDIF
IF DOES_CHAR_EXIST contact_ped[2]
    CLEAR_CHAR_TASKS contact_ped[2]
ENDIF
MARK_MODEL_AS_NO_LONGER_NEEDED 287
MARK_MODEL_AS_NO_LONGER_NEEDED 247
MARK_MODEL_AS_NO_LONGER_NEEDED 248
MARK_MODEL_AS_NO_LONGER_NEEDED 162
MARK_MODEL_AS_NO_LONGER_NEEDED 200
MARK_MODEL_AS_NO_LONGER_NEEDED BOBCAT
MARK_MODEL_AS_NO_LONGER_NEEDED PATRIOT
RESTORE_CAMERA_JUMPCUT
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
flag_player_on_mission = 0
MISSION_HAS_FINISHED
RETURN
}
