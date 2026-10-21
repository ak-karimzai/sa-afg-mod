// Mission 20 - Echoes in the Gorge: Haji Gul's armoured column is camped on the mountain road under a rotten
// gorge wall. Kill the six sentries silently (knife, silenced pistol): every loud shot or revving engine fills the
// noise meter, and if it fills the wall comes down on you. Plant three charges above the road, get clear, and
// bring the whole face down on the column.
MISSION_START
{
LVAR_INT blip cp stage enemy[10] eblip[10] k alive truck[3] noise weapon planted[3] left pc cpk[3] cpb[3] awake
LVAR_FLOAT px py pz d spd
GOSUB ec_main
IF HAS_DEATHARREST_BEEN_EXECUTED
    PRINT_BIG M_FAIL 5000 1
ENDIF
GOSUB ec_cleanup
MISSION_END

ec_main:
SCRIPT_NAME AFM20
blip = -1
cp = -1
noise = 0
pc = -1
left = 3
awake = 0
k = 0
WHILE k < 10
    enemy[k] = -1
    eblip[k] = -1
    k += 1
ENDWHILE
k = 0
WHILE k < 3
    truck[k] = -1
    planted[k] = 0
    cpk[k] = -1
    cpb[k] = -1
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
REQUEST_MODEL 347
REQUEST_MODEL 335
REQUEST_MODEL YANKEE
LOAD_ALL_MODELS_NOW
GOSUB ec_contact

// ---------------------------------------------------------------- Hamid at HQ
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
PRINT_BIG AFE_T 3000 2
TASK_PLAY_ANIM contact_ped[2] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AFE_01 6500 1
WAIT 6500
SET_FIXED_CAMERA_POSITION @cam_base_hq_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_ct@ JUMP_CUT
PRINT_NOW AFE_02 7500 1
WAIT 7500
SET_FIXED_CAMERA_POSITION @cam_base_hq_p@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_pt@ JUMP_CUT
CLEAR_CHAR_TASKS contact_ped[2]
PRINT_NOW AFE_06 2500 1
WAIT 2500
SET_FIXED_CAMERA_POSITION @cam_base_hq_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_ct@ JUMP_CUT
TASK_PLAY_ANIM contact_ped[2] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AFE_07 7000 1
WAIT 7000
CLEAR_CHAR_TASKS contact_ped[2]
DO_FADE 800 FADE_OUT
WAIT 900
// Karim drops you above the camp, one o'clock in the morning
SET_TIME_OF_DAY 1 0
REQUEST_COLLISION @ec_start.x@ @ec_start.y@
LOAD_SCENE @ec_start@
SET_CHAR_COORDINATES scplayer @ec_start@
SET_CHAR_HEADING scplayer @ec_start.h@
GIVE_WEAPON_TO_CHAR scplayer WEAPONTYPE_PISTOL_SILENCED 60
GIVE_WEAPON_TO_CHAR scplayer WEAPONTYPE_KNIFE 1
SET_CURRENT_CHAR_WEAPON scplayer WEAPONTYPE_PISTOL_SILENCED
CREATE_CAR YANKEE @ec_t0@ truck[0]
SET_CAR_HEADING truck[0] @ec_t0.h@
CREATE_CAR YANKEE @ec_t1@ truck[1]
SET_CAR_HEADING truck[1] @ec_t1.h@
CREATE_CAR YANKEE @ec_t2@ truck[2]
SET_CAR_HEADING truck[2] @ec_t2.h@
k = 0
WHILE k < 3
    SET_CAR_ENGINE_ON truck[k] FALSE
    LOCK_CAR_DOORS truck[k] CARLOCK_LOCKED
    k += 1
ENDWHILE
CREATE_CHAR PEDTYPE_MISSION1 247 @ec_s0@ enemy[0]
CREATE_CHAR PEDTYPE_MISSION1 248 @ec_s1@ enemy[1]
CREATE_CHAR PEDTYPE_MISSION1 162 @ec_s2@ enemy[2]
CREATE_CHAR PEDTYPE_MISSION1 200 @ec_s3@ enemy[3]
CREATE_CHAR PEDTYPE_MISSION1 247 @ec_s4@ enemy[4]
CREATE_CHAR PEDTYPE_MISSION1 248 @ec_s5@ enemy[5]
SET_CHAR_HEADING enemy[0] @ec_s0.h@
SET_CHAR_HEADING enemy[1] @ec_s1.h@
SET_CHAR_HEADING enemy[2] @ec_s2.h@
SET_CHAR_HEADING enemy[3] @ec_s3.h@
SET_CHAR_HEADING enemy[4] @ec_s4.h@
SET_CHAR_HEADING enemy[5] @ec_s5.h@
k = 0
WHILE k < 6
    GIVE_WEAPON_TO_CHAR enemy[k] WEAPONTYPE_AK47 500
    SET_CHAR_ACCURACY enemy[k] 40
    SET_CHAR_DROPS_WEAPONS_WHEN_DEAD enemy[k] FALSE
    SET_CHAR_STAY_IN_SAME_PLACE enemy[k] TRUE
    ADD_BLIP_FOR_CHAR enemy[k] eblip[k]
    k += 1
ENDWHILE
RESTORE_CAMERA_JUMPCUT
SET_CAMERA_BEHIND_PLAYER
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
DO_FADE 800 FADE_IN
PRINT_NOW AFE_03 8000 1
mission_counter = 6
DISPLAY_ONSCREEN_COUNTER_WITH_STRING mission_counter COUNTER_DISPLAY_NUMBER AFE_CT
mission_bar = 0
DISPLAY_ONSCREEN_COUNTER_WITH_STRING mission_bar COUNTER_DISPLAY_BAR AFE_NS
stage = 0

// ---------------------------------------------------------------- the camp
ec_loop:
WAIT 250
GET_CHAR_COORDINATES scplayer px py pz
IF stage < 3
    GOSUB ec_noise
    IF noise >= 100
        GOSUB ec_rockfall_on_you
        RETURN
    ENDIF
ENDIF
// sentries notice you up close or when hit without dying
k = 0
WHILE k < 6
    IF NOT IS_CHAR_DEAD enemy[k]
        IF HAS_CHAR_BEEN_DAMAGED_BY_CHAR enemy[k] scplayer
            GOSUB ec_wake
        ENDIF
        IF LOCATE_CHAR_ANY_MEANS_CHAR_3D scplayer enemy[k] 6.0 6.0 3.0 FALSE
            GOSUB ec_wake
        ENDIF
    ENDIF
    k += 1
ENDWHILE
IF stage = 0
    alive = 0
    k = 0
    WHILE k < 10
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
        stage = 1
        PRINT_NOW AFE_08 6000 1
        ADD_BLIP_FOR_COORD @ec_c0@ cpb[0]
        ADD_BLIP_FOR_COORD @ec_c1@ cpb[1]
        ADD_BLIP_FOR_COORD @ec_c2@ cpb[2]
        CREATE_CHECKPOINT CHECKPOINT_TUBE @ec_c0@ @ec_c0@ 2.5 cpk[0]
        CREATE_CHECKPOINT CHECKPOINT_TUBE @ec_c1@ @ec_c1@ 2.5 cpk[1]
        CREATE_CHECKPOINT CHECKPOINT_TUBE @ec_c2@ @ec_c2@ 2.5 cpk[2]
        mission_counter = 3
        DISPLAY_ONSCREEN_COUNTER_WITH_STRING mission_counter COUNTER_DISPLAY_NUMBER AFE_C2
    ENDIF
    GOTO ec_loop
ENDIF
IF stage = 1
    GOSUB ec_plant
    IF left = 0
        stage = 2
        CLEAR_ONSCREEN_COUNTER mission_counter
        PRINT_NOW AFE_09 6000 1
        ADD_BLIP_FOR_COORD @ec_safe@ blip
        CREATE_CHECKPOINT CHECKPOINT_TUBE @ec_safe@ @ec_safe@ 6.0 cp
    ENDIF
    GOTO ec_loop
ENDIF
IF stage = 2
    IF LOCATE_CHAR_ANY_MEANS_3D scplayer @ec_safe@ 8.0 8.0 6.0 TRUE
        stage = 3
        GOSUB ec_rockfall
        RETURN
    ENDIF
    GOTO ec_loop
ENDIF
GOTO ec_loop

// ---------------------------------------------------------------- the noise meter
ec_noise:
IF IS_CHAR_SHOOTING scplayer
    GET_CURRENT_CHAR_WEAPON scplayer weapon
    IF NOT weapon = WEAPONTYPE_PISTOL_SILENCED
        noise += 30
    ENDIF
ENDIF
IF IS_CHAR_IN_ANY_CAR scplayer
    noise += 4
ENDIF
noise -= 2
IF noise < 0
    noise = 0
ENDIF
IF noise > 100
    noise = 100
ENDIF
mission_bar = noise
IF noise >= 55
AND awake = 0
    GOSUB ec_wake
ENDIF
RETURN

ec_wake:
IF awake = 1
    RETURN
ENDIF
awake = 1
PRINT_NOW AFE_11 4000 1
k = 0
WHILE k < 6
    IF NOT IS_CHAR_DEAD enemy[k]
        SET_CHAR_STAY_IN_SAME_PLACE enemy[k] FALSE
        TASK_KILL_CHAR_ON_FOOT enemy[k] scplayer
    ENDIF
    k += 1
ENDWHILE
// four more men out of the trucks
CREATE_CHAR PEDTYPE_MISSION1 162 @ec_x0@ enemy[6]
CREATE_CHAR PEDTYPE_MISSION1 200 @ec_x1@ enemy[7]
CREATE_CHAR PEDTYPE_MISSION1 247 @ec_x3@ enemy[8]
CREATE_CHAR PEDTYPE_MISSION1 248 @ec_x4@ enemy[9]
k = 6
WHILE k < 10
    GIVE_WEAPON_TO_CHAR enemy[k] WEAPONTYPE_AK47 500
    SET_CHAR_ACCURACY enemy[k] 35
    SET_CHAR_DROPS_WEAPONS_WHEN_DEAD enemy[k] FALSE
    ADD_BLIP_FOR_CHAR enemy[k] eblip[k]
    TASK_KILL_CHAR_ON_FOOT enemy[k] scplayer
    k += 1
ENDWHILE
RETURN

// ---------------------------------------------------------------- planting: stand in a marker for 3 seconds
ec_plant:
k = -1
IF planted[0] = 0
    IF LOCATE_CHAR_ON_FOOT_3D scplayer @ec_c0@ 2.5 2.5 2.5 FALSE
        k = 0
    ENDIF
ENDIF
IF planted[1] = 0
    IF LOCATE_CHAR_ON_FOOT_3D scplayer @ec_c1@ 2.5 2.5 2.5 FALSE
        k = 1
    ENDIF
ENDIF
IF planted[2] = 0
    IF LOCATE_CHAR_ON_FOOT_3D scplayer @ec_c2@ 2.5 2.5 2.5 FALSE
        k = 2
    ENDIF
ENDIF
IF k = -1
    IF NOT pc = -1
        CLEAR_ONSCREEN_COUNTER mission_bar
        pc = -1
        mission_bar = noise
        DISPLAY_ONSCREEN_COUNTER_WITH_STRING mission_bar COUNTER_DISPLAY_BAR AFE_NS
    ENDIF
    RETURN
ENDIF
IF NOT pc = k
    pc = k
    CLEAR_ONSCREEN_COUNTER mission_bar
    mission_bar = 0
    DISPLAY_ONSCREEN_COUNTER_WITH_STRING mission_bar COUNTER_DISPLAY_BAR AFE_PL
ENDIF
mission_bar += 9
IF mission_bar >= 100
    planted[k] = 1
    left -= 1
    mission_counter = left
    REMOVE_BLIP cpb[k]
    cpb[k] = -1
    DELETE_CHECKPOINT cpk[k]
    cpk[k] = -1
    CLEAR_ONSCREEN_COUNTER mission_bar
    pc = -1
    mission_bar = noise
    DISPLAY_ONSCREEN_COUNTER_WITH_STRING mission_bar COUNTER_DISPLAY_BAR AFE_NS
ENDIF
RETURN

// ---------------------------------------------------------------- the gorge wall comes down
ec_rockfall_on_you:
CLEAR_ONSCREEN_COUNTER mission_counter
CLEAR_ONSCREEN_COUNTER mission_bar
PRINT_NOW AFE_10 5000 1
SHAKE_CAM 800
WAIT 1200
d = px
d += 3.0
ADD_EXPLOSION d py pz EXPLOSION_ROCKET
WAIT 300
d = py
d -= 3.0
ADD_EXPLOSION px d pz EXPLOSION_ROCKET
WAIT 300
ADD_EXPLOSION px py pz EXPLOSION_CAR
WAIT 1500
IF IS_PLAYER_PLAYING player
    SET_CHAR_HEALTH scplayer 0
ENDIF
WAIT 2500
PRINT_BIG M_FAIL 5000 1
RETURN

ec_rockfall:
CLEAR_ONSCREEN_COUNTER mission_bar
REMOVE_BLIP blip
blip = -1
DELETE_CHECKPOINT cp
cp = -1
SET_PLAYER_CONTROL player OFF
SWITCH_WIDESCREEN ON
WAIT 1000
SET_FIXED_CAMERA_POSITION @ec_safe@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @ec_t1@ JUMP_CUT
WAIT 1500
SHAKE_CAM 1500
ADD_EXPLOSION @ec_c0@ EXPLOSION_ROCKET
WAIT 400
ADD_EXPLOSION @ec_c1@ EXPLOSION_ROCKET
WAIT 400
ADD_EXPLOSION @ec_c2@ EXPLOSION_ROCKET
WAIT 600
ADD_EXPLOSION @ec_x0@ EXPLOSION_CAR
WAIT 250
ADD_EXPLOSION @ec_x1@ EXPLOSION_CAR
IF NOT IS_CAR_DEAD truck[0]
    EXPLODE_CAR truck[0]
ENDIF
WAIT 250
ADD_EXPLOSION @ec_x2@ EXPLOSION_CAR
IF NOT IS_CAR_DEAD truck[1]
    EXPLODE_CAR truck[1]
ENDIF
WAIT 250
ADD_EXPLOSION @ec_x3@ EXPLOSION_CAR
WAIT 250
ADD_EXPLOSION @ec_x4@ EXPLOSION_CAR
IF NOT IS_CAR_DEAD truck[2]
    EXPLODE_CAR truck[2]
ENDIF
WAIT 3000
PRINT_NOW AFE_12 6000 1
WAIT 6000
RESTORE_CAMERA_JUMPCUT
SET_CAMERA_BEHIND_PLAYER
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
mission_progress = 20
ADD_SCORE player 6000
PRINT_WITH_NUMBER_BIG M_PASS 6000 5000 1
REGISTER_MISSION_PASSED AFE_T
PLAYER_MADE_PROGRESS 1
PLAY_MISSION_PASSED_TUNE 1
RETURN

ec_contact:
IF NOT DOES_CHAR_EXIST contact_ped[2]
    CREATE_CHAR PEDTYPE_CIVMALE 287 @base_major@ contact_ped[2]
    SET_CHAR_HEADING contact_ped[2] @base_major.h@
    SET_CHAR_PROOFS contact_ped[2] TRUE TRUE TRUE TRUE TRUE
    SET_CHAR_NEVER_TARGETTED contact_ped[2] TRUE
    SET_CHAR_STAY_IN_SAME_PLACE contact_ped[2] TRUE
ENDIF
RETURN

ec_cleanup:
CLEAR_ONSCREEN_COUNTER mission_counter
CLEAR_ONSCREEN_COUNTER mission_bar
IF NOT blip = -1
    REMOVE_BLIP blip
    blip = -1
ENDIF
IF NOT cp = -1
    DELETE_CHECKPOINT cp
    cp = -1
ENDIF
k = 0
WHILE k < 3
    IF NOT cpb[k] = -1
        REMOVE_BLIP cpb[k]
        cpb[k] = -1
    ENDIF
    IF NOT cpk[k] = -1
        DELETE_CHECKPOINT cpk[k]
        cpk[k] = -1
    ENDIF
    IF NOT truck[k] = -1
        MARK_CAR_AS_NO_LONGER_NEEDED truck[k]
        truck[k] = -1
    ENDIF
    k += 1
ENDWHILE
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
IF DOES_CHAR_EXIST contact_ped[2]
    CLEAR_CHAR_TASKS contact_ped[2]
ENDIF
MARK_MODEL_AS_NO_LONGER_NEEDED 287
MARK_MODEL_AS_NO_LONGER_NEEDED 247
MARK_MODEL_AS_NO_LONGER_NEEDED 248
MARK_MODEL_AS_NO_LONGER_NEEDED 162
MARK_MODEL_AS_NO_LONGER_NEEDED 200
MARK_MODEL_AS_NO_LONGER_NEEDED 347
MARK_MODEL_AS_NO_LONGER_NEEDED 335
MARK_MODEL_AS_NO_LONGER_NEEDED YANKEE
RESTORE_CAMERA_JUMPCUT
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
flag_player_on_mission = 0
MISSION_HAS_FINISHED
RETURN
}
