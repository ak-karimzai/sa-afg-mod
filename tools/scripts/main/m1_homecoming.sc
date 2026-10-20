// Mission 1 - Homecoming: meet Kaka Rahim at the bazaar, drive flour to Deh-e-Bagh past a Black Stone roadblock.
MISSION_START
{
LVAR_INT truck blip cp stage baker block thug[2] tblip[2] k warned
LVAR_FLOAT px py pz d
GOSUB m1_main
IF HAS_DEATHARREST_BEEN_EXECUTED
    PRINT_BIG M_FAIL 5000 1
ENDIF
GOSUB m1_cleanup
MISSION_END

m1_main:
SCRIPT_NAME AFM1
truck = -1
blip = -1
cp = -1
baker = -1
block = -1
k = 0
WHILE k < 2
    thug[k] = -1
    tblip[k] = -1
    k += 1
ENDWHILE
warned = 0
flag_player_on_mission = 1
REGISTER_MISSION_GIVEN
WAIT 0
REQUEST_MODEL 158
REQUEST_MODEL 161
REQUEST_MODEL 247
REQUEST_MODEL 248
REQUEST_MODEL 346
REQUEST_MODEL BOBCAT
REQUEST_MODEL SADLER
LOAD_ALL_MODELS_NOW
GOSUB m1_contact
CREATE_CAR BOBCAT @bazaar_car@ truck
SET_CAR_HEADING truck @bazaar_car.h@
SET_CAR_HEALTH truck 1000

// ---------------------------------------------------------------- intro cutscene at Rahim's stall
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
PRINT_BIG AF1_T 3000 2
TASK_PLAY_ANIM contact_ped[0] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AF1_01 4500 1
WAIT 4500
SET_FIXED_CAMERA_POSITION @cam_bazaar_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_bazaar_ct@ JUMP_CUT
PRINT_NOW AF1_02 5000 1
WAIT 5000
SET_FIXED_CAMERA_POSITION @cam_bazaar_p@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_bazaar_pt@ JUMP_CUT
CLEAR_CHAR_TASKS contact_ped[0]
PRINT_NOW AF1_08 4000 1
WAIT 4000
SET_FIXED_CAMERA_POSITION @cam_bazaar_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_bazaar_ct@ JUMP_CUT
TASK_PLAY_ANIM contact_ped[0] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AF1_09 4500 1
WAIT 4500
CLEAR_CHAR_TASKS contact_ped[0]
GOSUB m1_cut_end

ADD_BLIP_FOR_CAR truck blip
PRINT_NOW AF1_03 5000 1
stage = 0

// ---------------------------------------------------------------- drive
m1_loop:
WAIT 100
IF IS_CAR_DEAD truck
    PRINT_NOW AF1_06 4000 1
    PRINT_BIG M_FAIL 5000 1
    RETURN
ENDIF
IF stage = 0
    IF IS_CHAR_IN_CAR scplayer truck
        REMOVE_BLIP blip
        ADD_BLIP_FOR_COORD @deh_e_bagh@ blip
        CREATE_CHECKPOINT CHECKPOINT_TUBE @deh_e_bagh@ @deh_e_bagh@ 6.0 cp
        PRINT_NOW AF1_04 5000 1
        PRINT AF1_05 4000 1
        mission_bar = 100
        DISPLAY_ONSCREEN_COUNTER_WITH_STRING mission_bar COUNTER_DISPLAY_BAR AF1_HB
        stage = 1
    ENDIF
ELSE
    GET_CAR_HEALTH truck mission_bar
    mission_bar /= 10
    IF NOT IS_CHAR_IN_CAR scplayer truck
        REMOVE_BLIP blip
        DELETE_CHECKPOINT cp
        cp = -1
        CLEAR_ONSCREEN_COUNTER mission_bar
        ADD_BLIP_FOR_CAR truck blip
        PRINT_NOW AF1_03 5000 1
        stage = 0
    ELSE
        IF LOCATE_CHAR_IN_CAR_3D scplayer @deh_e_bagh@ 6.0 6.0 4.0 FALSE
            GOSUB m1_arrive
            RETURN
        ENDIF
    ENDIF
ENDIF
GOSUB m1_roadblock
GOTO m1_loop

// ---------------------------------------------------------------- Black Stone "road tax" post half way
m1_roadblock:
IF warned > 2
    k = 0
    WHILE k < 2
        IF NOT tblip[k] = -1
            IF IS_CHAR_DEAD thug[k]
                REMOVE_BLIP tblip[k]
                tblip[k] = -1
            ENDIF
        ENDIF
        k += 1
    ENDWHILE
    RETURN
ENDIF
GET_CHAR_COORDINATES scplayer px py pz
GET_DISTANCE_BETWEEN_COORDS_2D px py @m1_block.x@ @m1_block.y@ d
IF warned = 0
    IF d < 120.0
        warned = 1
        CREATE_CAR SADLER @m1_block@ block
        SET_CAR_HEADING block @m1_block.h@
        CREATE_CHAR PEDTYPE_CIVMALE 247 @m1_thug0@ thug[0]
        SET_CHAR_HEADING thug[0] @m1_thug0.h@
        CREATE_CHAR PEDTYPE_CIVMALE 248 @m1_thug1@ thug[1]
        SET_CHAR_HEADING thug[1] @m1_thug1.h@
        k = 0
        WHILE k < 2
            GIVE_WEAPON_TO_CHAR thug[k] WEAPONTYPE_PISTOL 300
            SET_CHAR_ACCURACY thug[k] 20
            SET_CHAR_STAY_IN_SAME_PLACE thug[k] TRUE
            ADD_BLIP_FOR_CHAR thug[k] tblip[k]
            k += 1
        ENDWHILE
        PRINT_NOW AF1_10 4000 1
    ENDIF
    RETURN
ENDIF
IF warned = 1
    IF d < 55.0
        warned = 2
        PRINT_NOW AF1_11 4000 1
        TASK_AIM_GUN_AT_CHAR thug[0] scplayer 6000
    ENDIF
    RETURN
ENDIF
// warned = 2: they open fire when you get close or when you shoot first
IF d < 28.0
OR IS_CHAR_SHOOTING scplayer
    warned = 3
    PRINT_NOW AF1_12 4000 1
    k = 0
    WHILE k < 2
        SET_CHAR_RELATIONSHIP thug[k] ACQUAINTANCE_TYPE_PED_HATE PEDTYPE_PLAYER1
        SET_CHAR_STAY_IN_SAME_PLACE thug[k] FALSE
        TASK_KILL_CHAR_ON_FOOT thug[k] scplayer
        k += 1
    ENDWHILE
ENDIF
RETURN

// ---------------------------------------------------------------- arrival at the baker
m1_arrive:
SET_PLAYER_CONTROL player OFF
SWITCH_WIDESCREEN ON
CLEAR_ONSCREEN_COUNTER mission_bar
REMOVE_BLIP blip
blip = -1
DELETE_CHECKPOINT cp
cp = -1
SET_CAR_FORWARD_SPEED truck 0.0
CREATE_CHAR PEDTYPE_CIVMALE 161 @m1_baker@ baker
SET_CHAR_HEADING baker @m1_baker.h@
SET_CHAR_PROOFS baker TRUE TRUE TRUE TRUE TRUE
SET_FIXED_CAMERA_POSITION @cam_m1_baker@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_m1_baker_t@ JUMP_CUT
TASK_GO_STRAIGHT_TO_COORD baker @m1_baker_to@ PEDMOVE_WALK 6000
PRINT_NOW AF1_13 5000 1
WAIT 5000
TASK_PLAY_ANIM baker IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AF1_07 5500 1
WAIT 5500
CLEAR_CHAR_TASKS baker
GOSUB m1_cut_end
mission_progress = 1
ADD_SCORE player 300
PRINT_WITH_NUMBER_BIG M_PASS 300 5000 1
REGISTER_MISSION_PASSED AF1_T
PLAYER_MADE_PROGRESS 1
PLAY_MISSION_PASSED_TUNE 1
RETURN

m1_cut_end:
RESTORE_CAMERA_JUMPCUT
SET_CAMERA_BEHIND_PLAYER
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
RETURN

m1_contact:
IF NOT DOES_CHAR_EXIST contact_ped[0]
    CREATE_CHAR PEDTYPE_CIVMALE 158 @bazaar_uncle@ contact_ped[0]
    SET_CHAR_HEADING contact_ped[0] @bazaar_uncle.h@
    SET_CHAR_PROOFS contact_ped[0] TRUE TRUE TRUE TRUE TRUE
    SET_CHAR_NEVER_TARGETTED contact_ped[0] TRUE
    SET_CHAR_STAY_IN_SAME_PLACE contact_ped[0] TRUE
ENDIF
RETURN

m1_cleanup:
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
WHILE k < 2
    IF NOT tblip[k] = -1
        REMOVE_BLIP tblip[k]
        tblip[k] = -1
    ENDIF
    IF NOT thug[k] = -1
        MARK_CHAR_AS_NO_LONGER_NEEDED thug[k]
        thug[k] = -1
    ENDIF
    k += 1
ENDWHILE
IF NOT baker = -1
    MARK_CHAR_AS_NO_LONGER_NEEDED baker
    baker = -1
ENDIF
IF NOT block = -1
    MARK_CAR_AS_NO_LONGER_NEEDED block
    block = -1
ENDIF
IF NOT truck = -1
    MARK_CAR_AS_NO_LONGER_NEEDED truck
    truck = -1
ENDIF
IF DOES_CHAR_EXIST contact_ped[0]
    CLEAR_CHAR_TASKS contact_ped[0]
ENDIF
MARK_MODEL_AS_NO_LONGER_NEEDED 158
MARK_MODEL_AS_NO_LONGER_NEEDED 161
MARK_MODEL_AS_NO_LONGER_NEEDED 247
MARK_MODEL_AS_NO_LONGER_NEEDED 248
MARK_MODEL_AS_NO_LONGER_NEEDED BOBCAT
MARK_MODEL_AS_NO_LONGER_NEEDED SADLER
RESTORE_CAMERA_JUMPCUT
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
flag_player_on_mission = 0
MISSION_HAS_FINISHED
RETURN
}
