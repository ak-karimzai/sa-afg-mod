// Mission 5 - Aid Convoy: drive the aid truck from Camp Sangar up the valley with an army jeep behind you,
// survive the ambush, deliver to the elder of Dara-e-Sabz.
MISSION_START
{
LVAR_INT truck jeep ally[2] blip cp stage ambushed enemy[6] eblip[6] k block elder bail
LVAR_FLOAT px py pz d
GOSUB m5_main
IF HAS_DEATHARREST_BEEN_EXECUTED
    PRINT_BIG M_FAIL 5000 1
ENDIF
GOSUB m5_cleanup
MISSION_END

m5_main:
SCRIPT_NAME AFM5
truck = -1
jeep = -1
blip = -1
cp = -1
block = -1
elder = -1
bail = 0
k = 0
WHILE k < 2
    ally[k] = -1
    k += 1
ENDWHILE
k = 0
WHILE k < 6
    enemy[k] = -1
    eblip[k] = -1
    k += 1
ENDWHILE
flag_player_on_mission = 1
REGISTER_MISSION_GIVEN
WAIT 0
REQUEST_MODEL 287
REQUEST_MODEL 132
REQUEST_MODEL 247
REQUEST_MODEL 248
REQUEST_MODEL 200
REQUEST_MODEL 355
REQUEST_MODEL 356
REQUEST_MODEL 359
REQUEST_MODEL YANKEE
REQUEST_MODEL PATRIOT
REQUEST_MODEL SADLER
LOAD_ALL_MODELS_NOW
GOSUB m5_contact
CREATE_CAR YANKEE @aid_truck@ truck
SET_CAR_HEADING truck @aid_truck.h@
SET_CAR_HEALTH truck 2500
CREATE_CAR PATRIOT @m5_jeep@ jeep
SET_CAR_HEADING jeep @m5_jeep.h@
CREATE_CHAR_INSIDE_CAR jeep PEDTYPE_MISSION2 287 ally[0]
CREATE_CHAR_AS_PASSENGER jeep PEDTYPE_MISSION2 287 0 ally[1]
k = 0
WHILE k < 2
    GIVE_WEAPON_TO_CHAR ally[k] WEAPONTYPE_M4 1000
    SET_CHAR_HEALTH ally[k] 250
    SET_CHAR_ACCURACY ally[k] 60
    SET_CHAR_DROPS_WEAPONS_WHEN_DEAD ally[k] FALSE
    k += 1
ENDWHILE

// ---------------------------------------------------------------- briefing at HQ
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
PRINT_BIG AF5_T 3000 2
TASK_PLAY_ANIM contact_ped[2] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AF5_01 4500 1
WAIT 4500
SET_FIXED_CAMERA_POSITION @cam_base_hq_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_ct@ JUMP_CUT
PRINT_NOW AF5_02 5500 1
WAIT 5500
SET_FIXED_CAMERA_POSITION @cam_base_hq_p@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_pt@ JUMP_CUT
CLEAR_CHAR_TASKS contact_ped[2]
PRINT_NOW AF5_08 3000 1
WAIT 3000
SET_FIXED_CAMERA_POSITION @cam_base_hq_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_base_hq_ct@ JUMP_CUT
TASK_PLAY_ANIM contact_ped[2] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AF5_09 6000 1
WAIT 6000
CLEAR_CHAR_TASKS contact_ped[2]
GOSUB m5_cut_end

ADD_BLIP_FOR_CAR truck blip
PRINT_NOW AF5_03 5000 1
stage = 0
ambushed = 0

// ---------------------------------------------------------------- the drive
m5_loop:
WAIT 100
IF IS_CAR_DEAD truck
    PRINT_NOW AF5_06 4000 1
    PRINT_BIG M_FAIL 5000 1
    RETURN
ENDIF
IF stage = 0
    IF IS_CHAR_IN_CAR scplayer truck
        REMOVE_BLIP blip
        ADD_BLIP_FOR_COORD @dara_e_sabz@ blip
        CREATE_CHECKPOINT CHECKPOINT_TUBE @dara_e_sabz@ @dara_e_sabz@ 8.0 cp
        PRINT_NOW AF5_04 6000 1
        mission_bar = 100
        DISPLAY_ONSCREEN_COUNTER_WITH_STRING mission_bar COUNTER_DISPLAY_BAR AF5_HB
        // the jeep falls in behind the truck
        IF NOT IS_CAR_DEAD jeep
            SET_CAR_DRIVING_STYLE jeep DRIVINGMODE_PLOUGHTHROUGH
            SET_CAR_CRUISE_SPEED jeep 30.0
            SET_CAR_ESCORT_CAR_REAR jeep truck
        ENDIF
        stage = 1
    ENDIF
ELSE
    GET_CAR_HEALTH truck mission_bar
    mission_bar /= 25
    IF NOT IS_CHAR_IN_CAR scplayer truck
        IF stage = 1
            REMOVE_BLIP blip
            ADD_BLIP_FOR_CAR truck blip
            PRINT_NOW AF5_03 5000 1
            stage = 2
        ENDIF
    ELSE
        IF stage = 2
            REMOVE_BLIP blip
            ADD_BLIP_FOR_COORD @dara_e_sabz@ blip
            PRINT_NOW AF5_04 5000 1
            stage = 1
        ENDIF
        IF LOCATE_CHAR_IN_CAR_3D scplayer @dara_e_sabz@ 8.0 8.0 5.0 FALSE
            GOSUB m5_arrive
            RETURN
        ENDIF
    ENDIF
ENDIF
GOSUB m5_ambush
GOTO m5_loop

// ---------------------------------------------------------------- the ambush
m5_ambush:
IF ambushed = 0
    GET_CHAR_COORDINATES scplayer px py pz
    GET_DISTANCE_BETWEEN_COORDS_2D px py @ambush.x@ @ambush.y@ d
    IF d < 140.0
        ambushed = 1
        PRINT_NOW AF5_05 5000 1
        ADD_EXPLOSION @m5_boom@ EXPLOSION_ROCKET
        CREATE_CAR SADLER @m5_block@ block
        SET_CAR_HEADING block @m5_block.h@
        CREATE_CHAR PEDTYPE_MISSION1 247 @amb0@ enemy[0]
        CREATE_CHAR PEDTYPE_MISSION1 248 @amb1@ enemy[1]
        CREATE_CHAR PEDTYPE_MISSION1 200 @amb2@ enemy[2]
        CREATE_CHAR PEDTYPE_MISSION1 247 @amb3@ enemy[3]
        CREATE_CHAR PEDTYPE_MISSION1 248 @amb4@ enemy[4]
        CREATE_CHAR PEDTYPE_MISSION1 200 @amb5@ enemy[5]
        k = 0
        WHILE k < 6
            GIVE_WEAPON_TO_CHAR enemy[k] WEAPONTYPE_AK47 500
            SET_CHAR_ACCURACY enemy[k] 30
            SET_CHAR_DROPS_WEAPONS_WHEN_DEAD enemy[k] FALSE
            ADD_BLIP_FOR_CHAR enemy[k] eblip[k]
            TASK_KILL_CHAR_ON_FOOT enemy[k] scplayer
            k += 1
        ENDWHILE
        GIVE_WEAPON_TO_CHAR enemy[2] WEAPONTYPE_ROCKETLAUNCHER 6
        SET_CHAR_ACCURACY enemy[2] 15
        // Karim and Jawed dismount and engage
        k = 0
        WHILE k < 2
            IF NOT IS_CHAR_DEAD ally[k]
                TASK_LEAVE_ANY_CAR ally[k]
            ENDIF
            k += 1
        ENDWHILE
        bail = 25
    ENDIF
    RETURN
ENDIF
IF bail > 0
    bail -= 1
    IF bail = 5
        PRINT_NOW AF5_10 4500 1
    ENDIF
    IF bail = 0
        IF NOT IS_CHAR_DEAD ally[0]
            TASK_KILL_CHAR_ON_FOOT ally[0] enemy[0]
        ENDIF
        IF NOT IS_CHAR_DEAD ally[1]
            TASK_KILL_CHAR_ON_FOOT ally[1] enemy[3]
        ENDIF
    ENDIF
ENDIF
k = 0
WHILE k < 6
    IF IS_CHAR_DEAD enemy[k]
        IF NOT eblip[k] = -1
            REMOVE_BLIP eblip[k]
            eblip[k] = -1
        ENDIF
    ENDIF
    k += 1
ENDWHILE
RETURN

// ---------------------------------------------------------------- Dara-e-Sabz
m5_arrive:
SET_PLAYER_CONTROL player OFF
SWITCH_WIDESCREEN ON
CLEAR_ONSCREEN_COUNTER mission_bar
REMOVE_BLIP blip
blip = -1
DELETE_CHECKPOINT cp
cp = -1
SET_CAR_FORWARD_SPEED truck 0.0
CREATE_CHAR PEDTYPE_CIVMALE 132 @m5_elder@ elder
SET_CHAR_HEADING elder @m5_elder.h@
SET_CHAR_PROOFS elder TRUE TRUE TRUE TRUE TRUE
SET_FIXED_CAMERA_POSITION @cam_m5_elder@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_m5_elder_t@ JUMP_CUT
TASK_GO_STRAIGHT_TO_COORD elder @m5_elder_to@ PEDMOVE_WALK 6000
PRINT_NOW AF5_07 5000 1
WAIT 5000
TASK_PLAY_ANIM elder IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AF5_11 5500 1
WAIT 5500
CLEAR_CHAR_TASKS elder
GOSUB m5_cut_end
mission_progress = 5
ADD_SCORE player 2000
PRINT_WITH_NUMBER_BIG M_PASS 2000 5000 1
REGISTER_MISSION_PASSED AF5_T
PLAYER_MADE_PROGRESS 1
PLAY_MISSION_PASSED_TUNE 1
WAIT 5000
PRINT_HELP AF5_12
RETURN

m5_cut_end:
RESTORE_CAMERA_JUMPCUT
SET_CAMERA_BEHIND_PLAYER
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
RETURN

m5_contact:
IF NOT DOES_CHAR_EXIST contact_ped[2]
    CREATE_CHAR PEDTYPE_CIVMALE 287 @base_major@ contact_ped[2]
    SET_CHAR_HEADING contact_ped[2] @base_major.h@
    SET_CHAR_PROOFS contact_ped[2] TRUE TRUE TRUE TRUE TRUE
    SET_CHAR_NEVER_TARGETTED contact_ped[2] TRUE
    SET_CHAR_STAY_IN_SAME_PLACE contact_ped[2] TRUE
ENDIF
RETURN

m5_cleanup:
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
    IF NOT ally[k] = -1
        MARK_CHAR_AS_NO_LONGER_NEEDED ally[k]
        ally[k] = -1
    ENDIF
    k += 1
ENDWHILE
IF NOT elder = -1
    MARK_CHAR_AS_NO_LONGER_NEEDED elder
    elder = -1
ENDIF
IF NOT block = -1
    MARK_CAR_AS_NO_LONGER_NEEDED block
    block = -1
ENDIF
IF NOT jeep = -1
    MARK_CAR_AS_NO_LONGER_NEEDED jeep
    jeep = -1
ENDIF
IF NOT truck = -1
    MARK_CAR_AS_NO_LONGER_NEEDED truck
    truck = -1
ENDIF
IF DOES_CHAR_EXIST contact_ped[2]
    CLEAR_CHAR_TASKS contact_ped[2]
ENDIF
MARK_MODEL_AS_NO_LONGER_NEEDED 287
MARK_MODEL_AS_NO_LONGER_NEEDED 132
MARK_MODEL_AS_NO_LONGER_NEEDED 247
MARK_MODEL_AS_NO_LONGER_NEEDED 248
MARK_MODEL_AS_NO_LONGER_NEEDED 200
MARK_MODEL_AS_NO_LONGER_NEEDED YANKEE
MARK_MODEL_AS_NO_LONGER_NEEDED PATRIOT
MARK_MODEL_AS_NO_LONGER_NEEDED SADLER
RESTORE_CAMERA_JUMPCUT
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
flag_player_on_mission = 0
MISSION_HAS_FINISHED
RETURN
}
