// Mission 24 - Haji Gul (finale): Haji Gul throws everything he has left at Camp Sangar at dawn. Hold the gate
// with the soldiers through three waves, then take the Hunter and shoot down his helicopter before it crosses
// the northern mountains. Epilogue: tea with Rahim.
MISSION_START
{
LVAR_INT blip stage ally[6] enemy[14] eblip[14] car[3] k alive dead wave timer cartimer hcar heli pilot hblip hwp retask rahim away
LVAR_FLOAT px py pz d cx cy cz
GOSUB hg_main
IF HAS_DEATHARREST_BEEN_EXECUTED
    PRINT_BIG M_FAIL 5000 1
ENDIF
GOSUB hg_cleanup
MISSION_END

hg_main:
SCRIPT_NAME AFM24
blip = -1
hcar = -1
heli = -1
pilot = -1
hblip = -1
rahim = -1
wave = 0
timer = 0
cartimer = 0
hwp = 0
retask = 0
away = 0
k = 0
WHILE k < 14
    enemy[k] = -1
    eblip[k] = -1
    k += 1
ENDWHILE
k = 0
WHILE k < 6
    ally[k] = -1
    k += 1
ENDWHILE
car[0] = -1
car[1] = -1
car[2] = -1
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
REQUEST_MODEL BOBCAT
REQUEST_MODEL HUNTER
REQUEST_MODEL MAVERICK
LOAD_ALL_MODELS_NOW
GOSUB hg_contact

// ---------------------------------------------------------------- Hamid at HQ, before dawn
DO_FADE 400 FADE_OUT
WAIT 450
SET_PLAYER_CONTROL player OFF
SWITCH_WIDESCREEN ON
SET_CHAR_COORDINATES scplayer @talk_base_hq@
SET_CHAR_HEADING scplayer @talk_base_hq.h@
SET_CHAR_COORDINATES contact_ped[2] @base_major@
SET_CHAR_HEADING contact_ped[2] @base_major.h@
SET_TIME_OF_DAY 5 0
WAIT 600
SET_FIXED_CAMERA_POSITION @cam_base_hq_w@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_wt@ JUMP_CUT
DO_FADE 400 FADE_IN
PRINT_BIG AFH_T 3000 2
TASK_PLAY_ANIM contact_ped[2] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AFH_01 7000 1
WAIT 7000
SET_FIXED_CAMERA_POSITION @cam_base_hq_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_ct@ JUMP_CUT
PRINT_NOW AFH_02 7000 1
WAIT 7000
SET_FIXED_CAMERA_POSITION @cam_base_hq_p@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_pt@ JUMP_CUT
CLEAR_CHAR_TASKS contact_ped[2]
PRINT_NOW AFH_06 2500 1
WAIT 2500
SET_FIXED_CAMERA_POSITION @cam_base_hq_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_ct@ JUMP_CUT
TASK_PLAY_ANIM contact_ped[2] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AFH_07 6500 1
WAIT 6500
CLEAR_CHAR_TASKS contact_ped[2]
DO_FADE 800 FADE_OUT
WAIT 900
// the gate
SET_TIME_OF_DAY 5 40
SET_CHAR_COORDINATES scplayer @hg_gate@
SET_CHAR_HEADING scplayer @hg_gate.h@
GIVE_WEAPON_TO_CHAR scplayer WEAPONTYPE_M4 500
GIVE_WEAPON_TO_CHAR scplayer WEAPONTYPE_ROCKETLAUNCHER 6
SET_CURRENT_CHAR_WEAPON scplayer WEAPONTYPE_M4
ADD_ARMOUR_TO_CHAR scplayer 100
CREATE_CHAR PEDTYPE_MISSION2 287 @hg_ally0@ ally[0]
CREATE_CHAR PEDTYPE_MISSION2 287 @hg_ally1@ ally[1]
CREATE_CHAR PEDTYPE_MISSION2 287 @hg_ally2@ ally[2]
CREATE_CHAR PEDTYPE_MISSION2 287 @hg_ally3@ ally[3]
CREATE_CHAR PEDTYPE_MISSION2 287 @hg_ally4@ ally[4]
CREATE_CHAR PEDTYPE_MISSION2 287 @hg_ally5@ ally[5]
k = 0
WHILE k < 6
    GIVE_WEAPON_TO_CHAR ally[k] WEAPONTYPE_M4 1000
    SET_CHAR_HEALTH ally[k] 250
    SET_CHAR_ACCURACY ally[k] 55
    SET_CHAR_DROPS_WEAPONS_WHEN_DEAD ally[k] FALSE
    k += 1
ENDWHILE
CREATE_CAR HUNTER @hunter@ hcar
SET_CAR_HEADING hcar @hunter.h@
SET_FIXED_CAMERA_POSITION @cam_hg@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_hg_t@ JUMP_CUT
DO_FADE 800 FADE_IN
WAIT 2500
RESTORE_CAMERA_JUMPCUT
SET_CAMERA_BEHIND_PLAYER
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
timer = 20
stage = 1

// ---------------------------------------------------------------- the siege
hg_loop:
WAIT 250
GET_CHAR_COORDINATES scplayer px py pz
IF stage = 1
OR stage = 2
    GET_DISTANCE_BETWEEN_COORDS_2D px py @hg_gate.x@ @hg_gate.y@ d
    IF d > 230.0
        CLEAR_ONSCREEN_COUNTER mission_counter
        PRINT_NOW AFH_09 4000 1
        PRINT_BIG M_FAIL 5000 1
        RETURN
    ENDIF
    dead = 0
    k = 0
    WHILE k < 6
        IF IS_CHAR_DEAD ally[k]
            dead += 1
        ENDIF
        k += 1
    ENDWHILE
    IF dead = 6
        CLEAR_ONSCREEN_COUNTER mission_counter
        PRINT_NOW AFH_10 5000 1
        PRINT_BIG M_FAIL 5000 1
        RETURN
    ENDIF
ENDIF
IF stage = 1
    timer -= 1
    IF timer < 1
        GOSUB hg_wave
        stage = 2
    ENDIF
    GOTO hg_loop
ENDIF
IF stage = 2
    IF cartimer > 0
        cartimer -= 1
        IF cartimer = 8
            GOSUB hg_bail_out
        ENDIF
        IF cartimer = 0
            GOSUB hg_attack_all
        ENDIF
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
    mission_counter = alive
    IF alive = 0
        GOSUB hg_clear_wave
        IF wave = 3
            CLEAR_ONSCREEN_COUNTER mission_counter
            GOSUB hg_heli_start
            stage = 3
        ELSE
            stage = 1
            timer = 24
        ENDIF
    ENDIF
    GOTO hg_loop
ENDIF
// ---------------------------------------------------------------- stage 3 / 4: the Hunter
IF IS_CAR_DEAD hcar
    PRINT_NOW AFH_15 4000 1
    PRINT_BIG M_FAIL 5000 1
    RETURN
ENDIF
IF stage = 3
    IF IS_CHAR_IN_CAR scplayer hcar
        stage = 4
        REMOVE_BLIP blip
        blip = -1
        GOSUB hg_heli_spawn
        PRINT_NOW AFH_13 7000 1
    ENDIF
    GOTO hg_loop
ENDIF
IF NOT IS_CHAR_IN_CAR scplayer hcar
    IF away = 0
        away = 1
        ADD_BLIP_FOR_CAR hcar blip
        PRINT_NOW AFH_12 4000 1
    ENDIF
ELSE
    IF away = 1
        away = 0
        REMOVE_BLIP blip
        blip = -1
    ENDIF
ENDIF
IF IS_CAR_DEAD heli
    GOSUB hg_finale
    RETURN
ENDIF
GOSUB hg_heli_fly
IF hwp >= 4
    PRINT_NOW AFH_14 5000 1
    PRINT_BIG M_FAIL 5000 1
    RETURN
ENDIF
GOTO hg_loop

// ---------------------------------------------------------------- waves
hg_wave:
wave += 1
PRINT_WITH_NUMBER_NOW AFH_03 wave 4000 1
IF wave = 1
    CREATE_CHAR PEDTYPE_MISSION1 247 @hg_att0@ enemy[0]
    CREATE_CHAR PEDTYPE_MISSION1 248 @hg_att1@ enemy[1]
    CREATE_CHAR PEDTYPE_MISSION1 162 @hg_att2@ enemy[2]
    CREATE_CHAR PEDTYPE_MISSION1 200 @hg_att3@ enemy[3]
    CREATE_CHAR PEDTYPE_MISSION1 247 @hg_att4@ enemy[4]
    CREATE_CHAR PEDTYPE_MISSION1 248 @hg_att5@ enemy[5]
    CREATE_CHAR PEDTYPE_MISSION1 162 @hg_att6@ enemy[6]
    CREATE_CHAR PEDTYPE_MISSION1 200 @hg_att7@ enemy[7]
    k = 0
    WHILE k < 8
        GIVE_WEAPON_TO_CHAR enemy[k] WEAPONTYPE_AK47 500
        k += 1
    ENDWHILE
    GOSUB hg_attack_all
ENDIF
IF wave = 2
    PRINT AFH_08 3000 1
    CREATE_CAR BOBCAT @hg_car0@ car[0]
    SET_CAR_HEADING car[0] @hg_car0.h@
    CREATE_CAR BOBCAT @hg_car1@ car[1]
    SET_CAR_HEADING car[1] @hg_car1.h@
    CREATE_CHAR_INSIDE_CAR car[0] PEDTYPE_MISSION1 247 enemy[0]
    CREATE_CHAR_AS_PASSENGER car[0] PEDTYPE_MISSION1 248 0 enemy[1]
    CREATE_CHAR_INSIDE_CAR car[1] PEDTYPE_MISSION1 162 enemy[2]
    CREATE_CHAR_AS_PASSENGER car[1] PEDTYPE_MISSION1 200 0 enemy[3]
    CREATE_CHAR PEDTYPE_MISSION1 247 @hg_att0@ enemy[4]
    CREATE_CHAR PEDTYPE_MISSION1 248 @hg_att1@ enemy[5]
    CREATE_CHAR PEDTYPE_MISSION1 162 @hg_att2@ enemy[6]
    CREATE_CHAR PEDTYPE_MISSION1 200 @hg_att3@ enemy[7]
    k = 0
    WHILE k < 8
        GIVE_WEAPON_TO_CHAR enemy[k] WEAPONTYPE_AK47 500
        k += 1
    ENDWHILE
    GIVE_WEAPON_TO_CHAR enemy[7] WEAPONTYPE_ROCKETLAUNCHER 5
    SET_CURRENT_CHAR_WEAPON enemy[7] WEAPONTYPE_ROCKETLAUNCHER
    SET_CAR_FORWARD_SPEED car[0] 18.0
    SET_CAR_FORWARD_SPEED car[1] 18.0
    TASK_CAR_MISSION enemy[0] car[0] -1 3 30.0 3
    TASK_CAR_MISSION enemy[2] car[1] -1 3 30.0 3
    TASK_DRIVE_BY enemy[1] scplayer -1 0.0 0.0 0.0 60.0 DRIVEBY_AI_ALL_DIRN TRUE 100
    TASK_DRIVE_BY enemy[3] scplayer -1 0.0 0.0 0.0 60.0 DRIVEBY_AI_ALL_DIRN TRUE 100
    k = 4
    WHILE k < 8
        TASK_KILL_CHAR_ON_FOOT enemy[k] scplayer
        k += 1
    ENDWHILE
    cartimer = 24
ENDIF
IF wave = 3
    PRINT AFH_08 3000 1
    CREATE_CAR BOBCAT @hg_car0@ car[0]
    SET_CAR_HEADING car[0] @hg_car0.h@
    CREATE_CAR BOBCAT @hg_car1@ car[1]
    SET_CAR_HEADING car[1] @hg_car1.h@
    CREATE_CAR BOBCAT @hg_car2@ car[2]
    SET_CAR_HEADING car[2] @hg_car2.h@
    CREATE_CHAR_INSIDE_CAR car[0] PEDTYPE_MISSION1 247 enemy[0]
    CREATE_CHAR_AS_PASSENGER car[0] PEDTYPE_MISSION1 248 0 enemy[1]
    CREATE_CHAR_INSIDE_CAR car[1] PEDTYPE_MISSION1 162 enemy[2]
    CREATE_CHAR_AS_PASSENGER car[1] PEDTYPE_MISSION1 200 0 enemy[3]
    CREATE_CHAR_INSIDE_CAR car[2] PEDTYPE_MISSION1 247 enemy[4]
    CREATE_CHAR_AS_PASSENGER car[2] PEDTYPE_MISSION1 248 0 enemy[5]
    CREATE_CHAR PEDTYPE_MISSION1 247 @hg_att0@ enemy[6]
    CREATE_CHAR PEDTYPE_MISSION1 248 @hg_att1@ enemy[7]
    CREATE_CHAR PEDTYPE_MISSION1 162 @hg_att2@ enemy[8]
    CREATE_CHAR PEDTYPE_MISSION1 200 @hg_att3@ enemy[9]
    CREATE_CHAR PEDTYPE_MISSION1 247 @hg_att4@ enemy[10]
    CREATE_CHAR PEDTYPE_MISSION1 248 @hg_att5@ enemy[11]
    CREATE_CHAR PEDTYPE_MISSION1 162 @hg_att6@ enemy[12]
    CREATE_CHAR PEDTYPE_MISSION1 200 @hg_att7@ enemy[13]
    k = 0
    WHILE k < 14
        GIVE_WEAPON_TO_CHAR enemy[k] WEAPONTYPE_AK47 500
        k += 1
    ENDWHILE
    GIVE_WEAPON_TO_CHAR enemy[9] WEAPONTYPE_ROCKETLAUNCHER 5
    GIVE_WEAPON_TO_CHAR enemy[13] WEAPONTYPE_ROCKETLAUNCHER 5
    SET_CURRENT_CHAR_WEAPON enemy[9] WEAPONTYPE_ROCKETLAUNCHER
    SET_CURRENT_CHAR_WEAPON enemy[13] WEAPONTYPE_ROCKETLAUNCHER
    SET_CAR_FORWARD_SPEED car[0] 18.0
    SET_CAR_FORWARD_SPEED car[1] 18.0
    SET_CAR_FORWARD_SPEED car[2] 18.0
    TASK_CAR_MISSION enemy[0] car[0] -1 3 30.0 3
    TASK_CAR_MISSION enemy[2] car[1] -1 3 30.0 3
    TASK_CAR_MISSION enemy[4] car[2] -1 3 30.0 3
    TASK_DRIVE_BY enemy[1] scplayer -1 0.0 0.0 0.0 60.0 DRIVEBY_AI_ALL_DIRN TRUE 100
    TASK_DRIVE_BY enemy[3] scplayer -1 0.0 0.0 0.0 60.0 DRIVEBY_AI_ALL_DIRN TRUE 100
    TASK_DRIVE_BY enemy[5] scplayer -1 0.0 0.0 0.0 60.0 DRIVEBY_AI_ALL_DIRN TRUE 100
    k = 6
    WHILE k < 14
        TASK_KILL_CHAR_ON_FOOT enemy[k] scplayer
        k += 1
    ENDWHILE
    cartimer = 28
ENDIF
k = 0
WHILE k < 14
    IF NOT enemy[k] = -1
        SET_CHAR_ACCURACY enemy[k] 35
        SET_CHAR_DROPS_WEAPONS_WHEN_DEAD enemy[k] FALSE
        ADD_BLIP_FOR_CHAR enemy[k] eblip[k]
    ENDIF
    k += 1
ENDWHILE
IF wave = 2
    SET_CHAR_ACCURACY enemy[7] 12
ENDIF
IF wave = 3
    SET_CHAR_ACCURACY enemy[9] 12
    SET_CHAR_ACCURACY enemy[13] 12
ENDIF
// the soldiers pick their targets
k = 0
WHILE k < 6
    IF NOT IS_CHAR_DEAD ally[k]
        IF NOT enemy[k] = -1
            TASK_KILL_CHAR_ON_FOOT ally[k] enemy[k]
        ENDIF
    ENDIF
    k += 1
ENDWHILE
mission_counter = 14
DISPLAY_ONSCREEN_COUNTER_WITH_STRING mission_counter COUNTER_DISPLAY_NUMBER AFH_CT
RETURN

hg_bail_out:
k = 0
WHILE k < 6
    IF NOT enemy[k] = -1
        IF NOT IS_CHAR_DEAD enemy[k]
            IF IS_CHAR_IN_ANY_CAR enemy[k]
                TASK_LEAVE_ANY_CAR enemy[k]
            ENDIF
        ENDIF
    ENDIF
    k += 1
ENDWHILE
RETURN

hg_attack_all:
k = 0
WHILE k < 14
    IF NOT enemy[k] = -1
        IF NOT IS_CHAR_DEAD enemy[k]
            TASK_KILL_CHAR_ON_FOOT enemy[k] scplayer
        ENDIF
    ENDIF
    k += 1
ENDWHILE
RETURN

hg_clear_wave:
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
WHILE k < 3
    IF NOT car[k] = -1
        MARK_CAR_AS_NO_LONGER_NEEDED car[k]
        car[k] = -1
    ENDIF
    k += 1
ENDWHILE
RETURN

// ---------------------------------------------------------------- Haji Gul runs
hg_heli_start:
WAIT 2000
PRINT_NOW AFH_11 7000 1
ADD_BLIP_FOR_CAR hcar blip
RETURN

hg_heli_spawn:
CREATE_CAR MAVERICK @hg_heli@ heli
SET_CAR_HEADING heli @hg_heli.h@
SET_CAR_HEALTH heli 2200
CREATE_CHAR_INSIDE_CAR heli PEDTYPE_MISSION1 200 pilot
SET_CHAR_CANT_BE_DRAGGED_OUT pilot TRUE
ADD_BLIP_FOR_CAR heli hblip
CHANGE_BLIP_SCALE hblip 3
hwp = 0
retask = 0
GOSUB hg_heli_goto
RETURN

hg_heli_goto:
SET_CAR_CRUISE_SPEED heli 26.0
IF hwp = 0
    HELI_GOTO_COORDS heli @hg_wp0@ 60.0 160.0
ENDIF
IF hwp = 1
    HELI_GOTO_COORDS heli @hg_wp1@ 60.0 160.0
ENDIF
IF hwp = 2
    HELI_GOTO_COORDS heli @hg_wp2@ 60.0 160.0
ENDIF
IF hwp = 3
    HELI_GOTO_COORDS heli @hg_wp3@ 60.0 160.0
ENDIF
RETURN

hg_heli_fly:
IF IS_CHAR_DEAD pilot
    RETURN
ENDIF
GET_CAR_COORDINATES heli cx cy cz
IF hwp = 0
    GET_DISTANCE_BETWEEN_COORDS_2D cx cy @hg_wp0.x@ @hg_wp0.y@ d
ENDIF
IF hwp = 1
    GET_DISTANCE_BETWEEN_COORDS_2D cx cy @hg_wp1.x@ @hg_wp1.y@ d
ENDIF
IF hwp = 2
    GET_DISTANCE_BETWEEN_COORDS_2D cx cy @hg_wp2.x@ @hg_wp2.y@ d
ENDIF
IF hwp = 3
    GET_DISTANCE_BETWEEN_COORDS_2D cx cy @hg_wp3.x@ @hg_wp3.y@ d
ENDIF
IF d < 140.0
    hwp += 1
    IF hwp < 4
        GOSUB hg_heli_goto
    ENDIF
    RETURN
ENDIF
retask += 1
IF retask > 20
    retask = 0
    GOSUB hg_heli_goto
ENDIF
RETURN

// ---------------------------------------------------------------- the end
hg_finale:
IF NOT hblip = -1
    REMOVE_BLIP hblip
    hblip = -1
ENDIF
WAIT 3000
PRINT_NOW AFH_16 7500 1
WAIT 7500
DO_FADE 1000 FADE_OUT
WAIT 1100
SET_PLAYER_CONTROL player OFF
SWITCH_WIDESCREEN ON
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
PRINT_NOW AFH_17 5000 1
WAIT 5000
CLEAR_CHAR_TASKS rahim
PRINT_NOW AFH_18 2500 1
WAIT 2500
TASK_PLAY_ANIM rahim IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AFH_19 6500 1
WAIT 6500
CLEAR_CHAR_TASKS rahim
RESTORE_CAMERA_JUMPCUT
SET_CAMERA_BEHIND_PLAYER
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
mission_progress = 24
ADD_SCORE player 20000
PRINT_WITH_NUMBER_BIG M_PASS 20000 5000 1
REGISTER_MISSION_PASSED AFH_T
PLAYER_MADE_PROGRESS 1
PLAY_MISSION_PASSED_TUNE 1
WAIT 5500
PRINT_BIG AFG_TTL 5000 2
RETURN

hg_contact:
IF NOT DOES_CHAR_EXIST contact_ped[2]
    CREATE_CHAR PEDTYPE_CIVMALE 287 @base_major@ contact_ped[2]
    SET_CHAR_HEADING contact_ped[2] @base_major.h@
    SET_CHAR_PROOFS contact_ped[2] TRUE TRUE TRUE TRUE TRUE
    SET_CHAR_NEVER_TARGETTED contact_ped[2] TRUE
    SET_CHAR_STAY_IN_SAME_PLACE contact_ped[2] TRUE
ENDIF
RETURN

hg_cleanup:
CLEAR_ONSCREEN_COUNTER mission_counter
IF NOT blip = -1
    REMOVE_BLIP blip
    blip = -1
ENDIF
IF NOT hblip = -1
    REMOVE_BLIP hblip
    hblip = -1
ENDIF
GOSUB hg_clear_wave
k = 0
WHILE k < 6
    IF NOT ally[k] = -1
        MARK_CHAR_AS_NO_LONGER_NEEDED ally[k]
        ally[k] = -1
    ENDIF
    k += 1
ENDWHILE
IF NOT pilot = -1
    MARK_CHAR_AS_NO_LONGER_NEEDED pilot
    pilot = -1
ENDIF
IF NOT heli = -1
    MARK_CAR_AS_NO_LONGER_NEEDED heli
    heli = -1
ENDIF
IF NOT hcar = -1
    MARK_CAR_AS_NO_LONGER_NEEDED hcar
    hcar = -1
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
MARK_MODEL_AS_NO_LONGER_NEEDED BOBCAT
MARK_MODEL_AS_NO_LONGER_NEEDED HUNTER
MARK_MODEL_AS_NO_LONGER_NEEDED MAVERICK
RESTORE_CAMERA_JUMPCUT
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
flag_player_on_mission = 0
MISSION_HAS_FINISHED
RETURN
}
