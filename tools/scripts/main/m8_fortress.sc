// Mission 8 - Qala-e-Kohna: lead an army squad up the mountain before dawn, breach the gate, storm the
// fortress and kill Siahdast. Epilogue in the safehouse courtyard.
MISSION_START
{
LVAR_INT boss bblip stage enemy[11] eblip[11] ally[4] k alive taunted blip cp rahim
LVAR_FLOAT px py pz d
GOSUB m8_main
IF HAS_DEATHARREST_BEEN_EXECUTED
    PRINT_BIG M_FAIL 5000 1
ENDIF
GOSUB m8_cleanup
MISSION_END

m8_main:
SCRIPT_NAME AFM8
boss = -1
bblip = -1
blip = -1
cp = -1
rahim = -1
k = 0
WHILE k < 11
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
REQUEST_MODEL 158
REQUEST_MODEL 247
REQUEST_MODEL 248
REQUEST_MODEL 162
REQUEST_MODEL 200
REQUEST_MODEL 355
REQUEST_MODEL 356
REQUEST_MODEL 359
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
k = 0
WHILE k < 11
    GIVE_WEAPON_TO_CHAR enemy[k] WEAPONTYPE_AK47 500
    SET_CHAR_ACCURACY enemy[k] 40
    SET_CHAR_DROPS_WEAPONS_WHEN_DEAD enemy[k] FALSE
    SET_CHAR_STAY_IN_SAME_PLACE enemy[k] TRUE
    ADD_BLIP_FOR_CHAR enemy[k] eblip[k]
    k += 1
ENDWHILE
GIVE_WEAPON_TO_CHAR enemy[4] WEAPONTYPE_ROCKETLAUNCHER 6
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
mission_counter = 12
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
        stage = 1
        REMOVE_BLIP blip
        blip = -1
        DELETE_CHECKPOINT cp
        cp = -1
        ADD_EXPLOSION @fortress_gate@ EXPLOSION_ROCKET
        PRINT_NOW AF8_09 4000 1
        PRINT AF8_02 5000 1
        k = 0
        WHILE k < 11
            IF NOT IS_CHAR_DEAD enemy[k]
                SET_CHAR_STAY_IN_SAME_PLACE enemy[k] FALSE
                TASK_KILL_CHAR_ON_FOOT enemy[k] scplayer
            ENDIF
            k += 1
        ENDWHILE
    ENDIF
ENDIF
GET_DISTANCE_BETWEEN_COORDS_2D px py @fort_boss.x@ @fort_boss.y@ d
IF stage = 0
AND d < 75.0
    // they saw us coming
    stage = 1
    REMOVE_BLIP blip
    blip = -1
    DELETE_CHECKPOINT cp
    cp = -1
    PRINT_NOW AF8_02 5000 1
    k = 0
    WHILE k < 11
        IF NOT IS_CHAR_DEAD enemy[k]
            SET_CHAR_STAY_IN_SAME_PLACE enemy[k] FALSE
            TASK_KILL_CHAR_ON_FOOT enemy[k] scplayer
        ENDIF
        k += 1
    ENDWHILE
ENDIF
IF taunted = 0
AND d < 30.0
    taunted = 1
    PRINT_NOW AF8_03 5000 1
    IF NOT IS_CHAR_DEAD boss
        SET_CHAR_STAY_IN_SAME_PLACE boss FALSE
        TASK_KILL_CHAR_ON_FOOT boss scplayer
    ENDIF
ENDIF
alive = 0
k = 0
WHILE k < 11
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
IF IS_CHAR_DEAD boss
    IF NOT bblip = -1
        REMOVE_BLIP bblip
        bblip = -1
    ENDIF
ELSE
    alive += 1
ENDIF
mission_counter = alive
IF alive = 0
    CLEAR_ONSCREEN_COUNTER mission_counter
    WAIT 1500
    PRINT_NOW AF8_05 6000 1
    WAIT 6000
    GOSUB m8_epilogue
    RETURN
ENDIF
GOTO m8_loop

// ---------------------------------------------------------------- epilogue: tea with Rahim
m8_epilogue:
DO_FADE 1000 FADE_OUT
WAIT 1100
SET_PLAYER_CONTROL player OFF
SWITCH_WIDESCREEN ON
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
SET_TIME_OF_DAY 7 30
REQUEST_COLLISION @safehouse.x@ @safehouse.y@
LOAD_SCENE @m8_farid@
SET_CHAR_COORDINATES scplayer @m8_farid@
SET_CHAR_HEADING scplayer @m8_farid.h@
CREATE_CHAR PEDTYPE_CIVMALE 158 @m8_rahim@ rahim
SET_CHAR_HEADING rahim @m8_rahim.h@
SET_CHAR_PROOFS rahim TRUE TRUE TRUE TRUE TRUE
WAIT 600
SET_FIXED_CAMERA_POSITION @cam_end@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_end_t@ JUMP_CUT
DO_FADE 1000 FADE_IN
TASK_PLAY_ANIM rahim IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AF8_10 4500 1
WAIT 4500
CLEAR_CHAR_TASKS rahim
PRINT_NOW AF8_11 2500 1
WAIT 2500
TASK_PLAY_ANIM rahim IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AF8_12 6000 1
WAIT 6000
CLEAR_CHAR_TASKS rahim
RESTORE_CAMERA_JUMPCUT
SET_CAMERA_BEHIND_PLAYER
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
mission_progress = 8
ADD_SCORE player 10000
PRINT_WITH_NUMBER_BIG M_PASS 10000 5000 1
REGISTER_MISSION_PASSED AF8_T
PLAYER_MADE_PROGRESS 1
PLAY_MISSION_PASSED_TUNE 1
WAIT 5500
PRINT_BIG AFG_TTL 5000 2
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
WHILE k < 11
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
IF NOT rahim = -1
    MARK_CHAR_AS_NO_LONGER_NEEDED rahim
    rahim = -1
ENDIF
IF DOES_CHAR_EXIST contact_ped[2]
    CLEAR_CHAR_TASKS contact_ped[2]
ENDIF
MARK_MODEL_AS_NO_LONGER_NEEDED 287
MARK_MODEL_AS_NO_LONGER_NEEDED 158
MARK_MODEL_AS_NO_LONGER_NEEDED 247
MARK_MODEL_AS_NO_LONGER_NEEDED 248
MARK_MODEL_AS_NO_LONGER_NEEDED 162
MARK_MODEL_AS_NO_LONGER_NEEDED 200
RESTORE_CAMERA_JUMPCUT
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
flag_player_on_mission = 0
MISSION_HAS_FINISHED
RETURN
}
