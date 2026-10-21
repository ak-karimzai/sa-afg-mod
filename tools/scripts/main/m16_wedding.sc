// Mission 16 - The Wedding: the elder of Dara-e-Sabz marries off his son. From the ridge above the village,
// with the long rifle, stop three groups of Haji Gul's men coming down the slopes on foot, then the pickup with
// the RPG that comes up the valley road. The elder must live.
MISSION_START
{
LVAR_INT blip cp stage enemy[14] eblip[14] k alive guest[8] elder wave timer car cartimer taunted
LVAR_FLOAT px py pz d
GOSUB wd_main
IF HAS_DEATHARREST_BEEN_EXECUTED
    PRINT_BIG M_FAIL 5000 1
ENDIF
GOSUB wd_cleanup
MISSION_END

wd_main:
SCRIPT_NAME AFM16
blip = -1
cp = -1
elder = -1
car = -1
wave = 0
timer = 0
cartimer = 0
taunted = 0
k = 0
WHILE k < 14
    enemy[k] = -1
    eblip[k] = -1
    k += 1
ENDWHILE
k = 0
WHILE k < 8
    guest[k] = -1
    k += 1
ENDWHILE
flag_player_on_mission = 1
REGISTER_MISSION_GIVEN
WAIT 0
REQUEST_MODEL 158
REQUEST_MODEL 160
REQUEST_MODEL 128
REQUEST_MODEL 161
REQUEST_MODEL 202
REQUEST_MODEL 132
REQUEST_MODEL 247
REQUEST_MODEL 248
REQUEST_MODEL 162
REQUEST_MODEL 200
REQUEST_MODEL 355
REQUEST_MODEL 358
REQUEST_MODEL 359
REQUEST_MODEL BOBCAT
LOAD_ALL_MODELS_NOW
GOSUB wd_contact

// ---------------------------------------------------------------- Rahim at the bazaar
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
PRINT_BIG AFW_T 3000 2
TASK_PLAY_ANIM contact_ped[0] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AFW_01 5500 1
WAIT 5500
SET_FIXED_CAMERA_POSITION @cam_bazaar_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_bazaar_ct@ JUMP_CUT
PRINT_NOW AFW_02 7000 1
WAIT 7000
SET_FIXED_CAMERA_POSITION @cam_bazaar_p@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_bazaar_pt@ JUMP_CUT
CLEAR_CHAR_TASKS contact_ped[0]
PRINT_NOW AFW_06 3000 1
WAIT 3000
SET_FIXED_CAMERA_POSITION @cam_bazaar_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_bazaar_ct@ JUMP_CUT
TASK_PLAY_ANIM contact_ped[0] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AFW_07 6500 1
WAIT 6500
CLEAR_CHAR_TASKS contact_ped[0]
GIVE_WEAPON_TO_CHAR scplayer WEAPONTYPE_SNIPERRIFLE 60
GIVE_WEAPON_TO_CHAR scplayer WEAPONTYPE_AK47 200
SET_TIME_OF_DAY 15 0
RESTORE_CAMERA_JUMPCUT
SET_CAMERA_BEHIND_PLAYER
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
ADD_BLIP_FOR_COORD @dara_e_sabz@ blip
CREATE_CHECKPOINT CHECKPOINT_TUBE @dara_e_sabz@ @dara_e_sabz@ 8.0 cp
PRINT_NOW AFW_03 6000 1
stage = 0

// ---------------------------------------------------------------- Dara-e-Sabz
wd_loop:
WAIT 250
GET_CHAR_COORDINATES scplayer px py pz
IF stage = 0
    GET_DISTANCE_BETWEEN_COORDS_2D px py @dara_e_sabz.x@ @dara_e_sabz.y@ d
    IF d < 150.0
        stage = 1
        GOSUB wd_party
    ENDIF
    GOTO wd_loop
ENDIF
IF stage = 1
    IF LOCATE_CHAR_ON_FOOT_3D scplayer @dara_e_sabz@ 5.0 5.0 3.0 TRUE
        GOSUB wd_greeting
        stage = 2
    ENDIF
    GOTO wd_loop
ENDIF
IF stage = 2
    IF LOCATE_CHAR_ANY_MEANS_3D scplayer @wd_post@ 9.0 9.0 8.0 TRUE
        stage = 3
        REMOVE_BLIP blip
        blip = -1
        DELETE_CHECKPOINT cp
        cp = -1
        PRINT_NOW AFW_09 6000 1
        timer = 24
        mission_counter = 14
        DISPLAY_ONSCREEN_COUNTER_WITH_STRING mission_counter COUNTER_DISPLAY_NUMBER AFW_CT
    ENDIF
    GOTO wd_loop
ENDIF
// stage 3: the attack
IF IS_CHAR_DEAD elder
    CLEAR_ONSCREEN_COUNTER mission_counter
    PRINT_NOW AFW_12 5000 1
    PRINT_BIG M_FAIL 5000 1
    RETURN
ENDIF
alive = 0
k = 0
WHILE k < 14
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
// waves: the next group comes when the timer runs out or the current one is nearly finished
IF wave < 3
    timer -= 1
    IF timer < 1
    OR alive < 2
        GOSUB wd_wave
    ENDIF
ENDIF
IF wave = 3
AND cartimer = 0
AND alive = 0
    GOSUB wd_technical
ENDIF
IF cartimer > 0
    cartimer -= 1
    IF cartimer = 1
        IF NOT IS_CHAR_DEAD enemy[12]
            TASK_LEAVE_ANY_CAR enemy[12]
        ENDIF
        IF NOT IS_CHAR_DEAD enemy[13]
            TASK_LEAVE_ANY_CAR enemy[13]
        ENDIF
    ENDIF
    IF NOT IS_CAR_DEAD car
        GET_CAR_COORDINATES car px py pz
        GET_DISTANCE_BETWEEN_COORDS_2D px py @dara_e_sabz.x@ @dara_e_sabz.y@ d
        IF d < 45.0
            cartimer = 2
        ENDIF
    ENDIF
ENDIF
IF wave = 4
AND taunted = 0
    IF NOT IS_CHAR_DEAD enemy[12]
    AND NOT IS_CHAR_IN_ANY_CAR enemy[12]
        taunted = 1
        TASK_KILL_CHAR_ON_FOOT enemy[12] elder
        IF NOT IS_CHAR_DEAD enemy[13]
            TASK_KILL_CHAR_ON_FOOT enemy[13] scplayer
        ENDIF
    ENDIF
ENDIF
mission_counter = alive
IF wave = 4
AND alive = 0
    GOSUB wd_pass
    RETURN
ENDIF
GOTO wd_loop

// ---------------------------------------------------------------- the party
wd_party:
CREATE_CHAR PEDTYPE_CIVMALE 160 @wd_elder@ elder
SET_CHAR_HEADING elder @wd_elder.h@
SET_CHAR_MAX_HEALTH elder 300
SET_CHAR_HEALTH elder 300
SET_CHAR_STAY_IN_SAME_PLACE elder TRUE
CREATE_CHAR PEDTYPE_CIVMALE 128 @wd_guest0@ guest[0]
CREATE_CHAR PEDTYPE_CIVMALE 161 @wd_guest1@ guest[1]
CREATE_CHAR PEDTYPE_CIVMALE 202 @wd_guest2@ guest[2]
CREATE_CHAR PEDTYPE_CIVMALE 158 @wd_guest3@ guest[3]
CREATE_CHAR PEDTYPE_CIVMALE 132 @wd_guest4@ guest[4]
CREATE_CHAR PEDTYPE_CIVMALE 128 @wd_guest5@ guest[5]
CREATE_CHAR PEDTYPE_CIVMALE 161 @wd_guest6@ guest[6]
CREATE_CHAR PEDTYPE_CIVMALE 202 @wd_guest7@ guest[7]
k = 0
WHILE k < 8
    TASK_PLAY_ANIM guest[k] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
    k += 1
ENDWHILE
RETURN

wd_greeting:
SET_PLAYER_CONTROL player OFF
SWITCH_WIDESCREEN ON
SET_FIXED_CAMERA_POSITION @cam_wd@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_wd_t@ JUMP_CUT
TASK_PLAY_ANIM elder IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AFW_08 5500 1
WAIT 5500
CLEAR_CHAR_TASKS elder
RESTORE_CAMERA_JUMPCUT
SET_CAMERA_BEHIND_PLAYER
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
REMOVE_BLIP blip
blip = -1
DELETE_CHECKPOINT cp
cp = -1
ADD_BLIP_FOR_COORD @wd_post@ blip
CREATE_CHECKPOINT CHECKPOINT_TUBE @wd_post@ @wd_post@ 6.0 cp
PRINT_NOW AFW_03 5000 1
RETURN

// ---------------------------------------------------------------- the three groups on the slopes
wd_wave:
wave += 1
timer = 200
IF wave = 1
    CREATE_CHAR PEDTYPE_MISSION1 247 @wd_a0_0@ enemy[0]
    CREATE_CHAR PEDTYPE_MISSION1 248 @wd_a0_1@ enemy[1]
    CREATE_CHAR PEDTYPE_MISSION1 162 @wd_a0_2@ enemy[2]
    CREATE_CHAR PEDTYPE_MISSION1 200 @wd_a0_3@ enemy[3]
    // the guests run, the elder stays with his son
    k = 0
    WHILE k < 8
        IF NOT IS_CHAR_DEAD guest[k]
            CLEAR_CHAR_TASKS guest[k]
            TASK_SMART_FLEE_POINT guest[k] @wd_elder@ 120.0 60000
        ENDIF
        k += 1
    ENDWHILE
    TASK_DUCK elder 120000
ENDIF
IF wave = 2
    PRINT_NOW AFW_10 4000 1
    CREATE_CHAR PEDTYPE_MISSION1 247 @wd_a1_0@ enemy[4]
    CREATE_CHAR PEDTYPE_MISSION1 248 @wd_a1_1@ enemy[5]
    CREATE_CHAR PEDTYPE_MISSION1 162 @wd_a1_2@ enemy[6]
    CREATE_CHAR PEDTYPE_MISSION1 200 @wd_a1_3@ enemy[7]
ENDIF
IF wave = 3
    PRINT_NOW AFW_10 4000 1
    CREATE_CHAR PEDTYPE_MISSION1 247 @wd_a2_0@ enemy[8]
    CREATE_CHAR PEDTYPE_MISSION1 248 @wd_a2_1@ enemy[9]
    CREATE_CHAR PEDTYPE_MISSION1 162 @wd_a2_2@ enemy[10]
    CREATE_CHAR PEDTYPE_MISSION1 200 @wd_a2_3@ enemy[11]
ENDIF
k = wave
k -= 1
k *= 4
alive = k
alive += 4
WHILE k < alive
    GIVE_WEAPON_TO_CHAR enemy[k] WEAPONTYPE_AK47 500
    SET_CHAR_ACCURACY enemy[k] 30
    SET_CHAR_DROPS_WEAPONS_WHEN_DEAD enemy[k] FALSE
    ADD_BLIP_FOR_CHAR enemy[k] eblip[k]
    TASK_KILL_CHAR_ON_FOOT enemy[k] elder
    k += 1
ENDWHILE
RETURN

// ---------------------------------------------------------------- the technical with the RPG
wd_technical:
wave = 4
PRINT_NOW AFW_11 5000 1
CREATE_CAR BOBCAT @wd_car@ car
SET_CAR_HEADING car @wd_car.h@
CREATE_CHAR_INSIDE_CAR car PEDTYPE_MISSION1 247 enemy[12]
CREATE_CHAR_AS_PASSENGER car PEDTYPE_MISSION1 248 0 enemy[13]
GIVE_WEAPON_TO_CHAR enemy[12] WEAPONTYPE_AK47 500
GIVE_WEAPON_TO_CHAR enemy[13] WEAPONTYPE_ROCKETLAUNCHER 6
SET_CHAR_ACCURACY enemy[12] 35
SET_CHAR_ACCURACY enemy[13] 12
SET_CHAR_DROPS_WEAPONS_WHEN_DEAD enemy[12] FALSE
SET_CHAR_DROPS_WEAPONS_WHEN_DEAD enemy[13] FALSE
ADD_BLIP_FOR_CHAR enemy[12] eblip[12]
ADD_BLIP_FOR_CHAR enemy[13] eblip[13]
TASK_CAR_DRIVE_TO_COORD enemy[12] car @dara_e_sabz@ 16.0 MODE_STRAIGHTLINE -1 3
cartimer = 160
RETURN

// ---------------------------------------------------------------- the elder's thanks
wd_pass:
CLEAR_ONSCREEN_COUNTER mission_counter
IF NOT blip = -1
    REMOVE_BLIP blip
    blip = -1
ENDIF
SET_CHAR_PROOFS elder TRUE TRUE TRUE TRUE TRUE
CLEAR_CHAR_TASKS elder
WAIT 2000
PRINT_NOW AFW_13 6000 1
WAIT 6000
PRINT_NOW AFW_14 5500 1
WAIT 3000
mission_progress = 16
ADD_SCORE player 3500
PRINT_WITH_NUMBER_BIG M_PASS 3500 5000 1
REGISTER_MISSION_PASSED AFW_T
PLAYER_MADE_PROGRESS 1
PLAY_MISSION_PASSED_TUNE 1
RETURN

wd_contact:
IF NOT DOES_CHAR_EXIST contact_ped[0]
    CREATE_CHAR PEDTYPE_CIVMALE 158 @bazaar_uncle@ contact_ped[0]
    SET_CHAR_HEADING contact_ped[0] @bazaar_uncle.h@
    SET_CHAR_PROOFS contact_ped[0] TRUE TRUE TRUE TRUE TRUE
    SET_CHAR_NEVER_TARGETTED contact_ped[0] TRUE
    SET_CHAR_STAY_IN_SAME_PLACE contact_ped[0] TRUE
ENDIF
RETURN

wd_cleanup:
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
k = 0
WHILE k < 8
    IF NOT guest[k] = -1
        MARK_CHAR_AS_NO_LONGER_NEEDED guest[k]
        guest[k] = -1
    ENDIF
    k += 1
ENDWHILE
IF NOT elder = -1
    MARK_CHAR_AS_NO_LONGER_NEEDED elder
    elder = -1
ENDIF
IF NOT car = -1
    MARK_CAR_AS_NO_LONGER_NEEDED car
    car = -1
ENDIF
IF DOES_CHAR_EXIST contact_ped[0]
    CLEAR_CHAR_TASKS contact_ped[0]
ENDIF
MARK_MODEL_AS_NO_LONGER_NEEDED 158
MARK_MODEL_AS_NO_LONGER_NEEDED 160
MARK_MODEL_AS_NO_LONGER_NEEDED 128
MARK_MODEL_AS_NO_LONGER_NEEDED 161
MARK_MODEL_AS_NO_LONGER_NEEDED 202
MARK_MODEL_AS_NO_LONGER_NEEDED 132
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
