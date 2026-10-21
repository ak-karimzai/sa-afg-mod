// Mission 14 - Buzkashi: motorised buzkashi on the plain. Grab the sack, carry it into the scoring circle on your
// pbike while Haji Gul's five riders ram you off it; if you fall the sack drops where you fell and a rider can
// take it. First to three carries. When you win, Haji Gul's men shoot up the field rather than pay.
MISSION_START
{
LVAR_INT blip cp stage pbike rider[5] rbike[5] rblip[5] k k2 bag holder myscore theirscore offbike retask car[2] cdrv[2] cgun[2] alive dead
LVAR_FLOAT px py pz d bagx bagy bagz cx cy cz
GOSUB bz_main
IF HAS_DEATHARREST_BEEN_EXECUTED
    PRINT_BIG M_FAIL 5000 1
ENDIF
GOSUB bz_cleanup
MISSION_END

bz_main:
SCRIPT_NAME AFM14
blip = -1
cp = -1
pbike = -1
bag = -1
holder = -1
myscore = 0
theirscore = 0
offbike = 0
retask = 0
k = 0
WHILE k < 5
    rider[k] = -1
    rbike[k] = -1
    rblip[k] = -1
    k += 1
ENDWHILE
k = 0
WHILE k < 2
    car[k] = -1
    cdrv[k] = -1
    cgun[k] = -1
    k += 1
ENDWHILE
flag_player_on_mission = 1
REGISTER_MISSION_GIVEN
WAIT 0
REQUEST_MODEL 158
REQUEST_MODEL 247
REQUEST_MODEL 248
REQUEST_MODEL 162
REQUEST_MODEL 200
REQUEST_MODEL 355
REQUEST_MODEL 1210
REQUEST_MODEL SANCHEZ
REQUEST_MODEL BOBCAT
LOAD_ALL_MODELS_NOW
GOSUB bz_contact

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
PRINT_BIG AFK_T 3000 2
TASK_PLAY_ANIM contact_ped[0] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AFK_01 6500 1
WAIT 6500
SET_FIXED_CAMERA_POSITION @cam_bazaar_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_bazaar_ct@ JUMP_CUT
PRINT_NOW AFK_02 6500 1
WAIT 6500
SET_FIXED_CAMERA_POSITION @cam_bazaar_p@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_bazaar_pt@ JUMP_CUT
CLEAR_CHAR_TASKS contact_ped[0]
PRINT_NOW AFK_06 2500 1
WAIT 2500
SET_FIXED_CAMERA_POSITION @cam_bazaar_c@ 0.0 0.0 0.0
POINT_CAMERA_AT_POINT @cam_bazaar_ct@ JUMP_CUT
TASK_PLAY_ANIM contact_ped[0] IDLE_chat PED 4.0 TRUE FALSE FALSE FALSE -1
PRINT_NOW AFK_07 6500 1
WAIT 6500
CLEAR_CHAR_TASKS contact_ped[0]
SET_TIME_OF_DAY 10 0
CREATE_CAR SANCHEZ @bz_pbike@ pbike
SET_CAR_HEADING pbike @bz_pbike.h@
RESTORE_CAMERA_JUMPCUT
SET_CAMERA_BEHIND_PLAYER
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
ADD_BLIP_FOR_CAR pbike blip
PRINT_NOW AFK_03 5000 1
PRINT AFK_08 5000 1
stage = 0

// ---------------------------------------------------------------- the field
bz_loop:
WAIT 250
GET_CHAR_COORDINATES scplayer px py pz
IF stage = 0
    GET_DISTANCE_BETWEEN_COORDS_2D px py @bz_center.x@ @bz_center.y@ d
    IF d < 70.0
    AND IS_CHAR_ON_ANY_BIKE scplayer
        stage = 1
        REMOVE_BLIP blip
        blip = -1
        GOSUB bz_start
    ENDIF
    GOTO bz_loop
ENDIF
IF stage = 1
    GOSUB bz_riders
    GOSUB bz_bag
    IF myscore >= 3
        stage = 2
        GOSUB bz_cheat
    ENDIF
    IF theirscore >= 3
        CLEAR_ONSCREEN_COUNTER mission_counter
        CLEAR_ONSCREEN_COUNTER mission_bar
        PRINT_NOW AFK_15 5000 1
        PRINT_BIG M_FAIL 5000 1
        RETURN
    ENDIF
    GOTO bz_loop
ENDIF
// stage 2: Haji Gul's men
dead = 0
k = 0
WHILE k < 2
    IF IS_CHAR_DEAD cdrv[k]
        dead += 1
    ENDIF
    IF IS_CHAR_DEAD cgun[k]
        dead += 1
    ENDIF
    k += 1
ENDWHILE
retask += 1
IF retask > 40
    retask = 0
    k = 0
    WHILE k < 2
        IF NOT IS_CAR_DEAD car[k]
        AND NOT IS_CHAR_DEAD cdrv[k]
            IF IS_CHAR_IN_CAR cdrv[k] car[k]
                TASK_CAR_MISSION cdrv[k] car[k] -1 3 30.0 3
            ELSE
                TASK_KILL_CHAR_ON_FOOT cdrv[k] scplayer
            ENDIF
        ENDIF
        IF NOT IS_CHAR_DEAD cgun[k]
            IF NOT IS_CHAR_IN_CAR cgun[k] car[k]
                TASK_KILL_CHAR_ON_FOOT cgun[k] scplayer
            ENDIF
        ENDIF
        k += 1
    ENDWHILE
ENDIF
IF dead = 4
    GOSUB bz_pass
    RETURN
ENDIF
GOTO bz_loop

// ---------------------------------------------------------------- start of the match
bz_start:
PRINT_NOW AFK_09 7000 1
CREATE_CAR SANCHEZ @bz_r0@ rbike[0]
SET_CAR_HEADING rbike[0] @bz_r0.h@
CREATE_CAR SANCHEZ @bz_r1@ rbike[1]
SET_CAR_HEADING rbike[1] @bz_r1.h@
CREATE_CAR SANCHEZ @bz_r2@ rbike[2]
SET_CAR_HEADING rbike[2] @bz_r2.h@
CREATE_CAR SANCHEZ @bz_r3@ rbike[3]
SET_CAR_HEADING rbike[3] @bz_r3.h@
CREATE_CAR SANCHEZ @bz_r4@ rbike[4]
SET_CAR_HEADING rbike[4] @bz_r4.h@
k = 0
WHILE k < 5
    CREATE_CHAR_INSIDE_CAR rbike[k] PEDTYPE_MISSION1 247 rider[k]
    SET_CHAR_HEALTH rider[k] 150
    SET_CHAR_DROPS_WEAPONS_WHEN_DEAD rider[k] FALSE
    ADD_BLIP_FOR_CAR rbike[k] rblip[k]
    k += 1
ENDWHILE
bagx = @bz_center.x@
bagy = @bz_center.y@
bagz = @bz_center.z@
GOSUB bz_place_bag
mission_counter = 0
mission_bar = 0
DISPLAY_ONSCREEN_COUNTER_WITH_STRING mission_counter COUNTER_DISPLAY_NUMBER AFK_SC
DISPLAY_ONSCREEN_COUNTER_WITH_STRING mission_bar COUNTER_DISPLAY_NUMBER AFK_S2
RETURN

bz_place_bag:
CREATE_PICKUP 1210 PICKUP_ONCE bagx bagy bagz bag
IF NOT blip = -1
    REMOVE_BLIP blip
    blip = -1
ENDIF
IF NOT cp = -1
    DELETE_CHECKPOINT cp
    cp = -1
ENDIF
ADD_BLIP_FOR_PICKUP bag blip
holder = -1
RETURN

// ---------------------------------------------------------------- the riders
bz_riders:
retask += 1
k = 0
WHILE k < 5
    IF NOT IS_CHAR_DEAD rider[k]
    AND NOT IS_CAR_DEAD rbike[k]
        IF IS_CHAR_IN_CAR rider[k] rbike[k]
            IF retask > 12
                IF holder = k
                    TASK_CAR_DRIVE_TO_COORD rider[k] rbike[k] @bz_pit@ 22.0 MODE_STRAIGHTLINE -1 3
                ENDIF
                IF holder = -2
                    TASK_CAR_MISSION rider[k] rbike[k] -1 3 32.0 3
                ENDIF
                IF holder = -1
                    TASK_CAR_DRIVE_TO_COORD rider[k] rbike[k] bagx bagy bagz 22.0 MODE_STRAIGHTLINE -1 3
                ENDIF
            ENDIF
        ELSE
            IF retask > 12
                TASK_ENTER_CAR_AS_DRIVER rider[k] rbike[k] 8000
            ENDIF
        ENDIF
    ENDIF
    k += 1
ENDWHILE
IF retask > 12
    retask = 0
ENDIF
RETURN

// ---------------------------------------------------------------- the sack
bz_bag:
IF holder = -1
    IF HAS_PICKUP_BEEN_COLLECTED bag
        holder = -2
        offbike = 0
        REMOVE_BLIP blip
        blip = -1
        ADD_BLIP_FOR_COORD @bz_pit@ blip
        CREATE_CHECKPOINT CHECKPOINT_TUBE @bz_pit@ @bz_pit@ 9.0 cp
        PRINT_NOW AFK_10 4000 1
        RETURN
    ENDIF
    k = 0
    WHILE k < 5
        IF holder = -1
            IF NOT IS_CHAR_DEAD rider[k]
                IF IS_CHAR_IN_CAR rider[k] rbike[k]
                    GET_CAR_COORDINATES rbike[k] cx cy cz
                    GET_DISTANCE_BETWEEN_COORDS_2D cx cy bagx bagy d
                    IF d < 5.0
                        holder = k
                        REMOVE_PICKUP bag
                        REMOVE_BLIP blip
                        blip = -1
                        ADD_BLIP_FOR_CHAR rider[k] blip
                        PRINT_NOW AFK_12 4000 1
                    ENDIF
                ENDIF
            ENDIF
        ENDIF
        k += 1
    ENDWHILE
    RETURN
ENDIF
IF holder = -2
    IF IS_CHAR_ON_ANY_BIKE scplayer
        offbike = 0
        IF LOCATE_CHAR_IN_CAR_3D scplayer @bz_pit@ 9.0 9.0 6.0 FALSE
            myscore += 1
            mission_counter = myscore
            PRINT_NOW AFK_14 2500 1
            bagx = @bz_center.x@
            bagy = @bz_center.y@
            bagz = @bz_center.z@
            GOSUB bz_place_bag
        ENDIF
    ELSE
        offbike += 1
        IF offbike > 6
            // knocked off: the sack lies where you fell
            PRINT_NOW AFK_11 3000 1
            bagx = px
            bagy = py
            bagz = pz
            GOSUB bz_place_bag
        ENDIF
    ENDIF
    RETURN
ENDIF
// a rider has it
k = holder
IF IS_CHAR_DEAD rider[k]
OR NOT IS_CHAR_IN_CAR rider[k] rbike[k]
    IF IS_CHAR_DEAD rider[k]
        GET_CAR_COORDINATES rbike[k] bagx bagy bagz
    ELSE
        GET_CHAR_COORDINATES rider[k] bagx bagy bagz
    ENDIF
    GOSUB bz_place_bag
    RETURN
ENDIF
GET_CAR_COORDINATES rbike[k] cx cy cz
GET_DISTANCE_BETWEEN_COORDS_2D cx cy @bz_pit.x@ @bz_pit.y@ d
IF d < 9.0
    theirscore += 1
    mission_bar = theirscore
    PRINT_NOW AFK_13 3000 1
    bagx = @bz_center.x@
    bagy = @bz_center.y@
    bagz = @bz_center.z@
    GOSUB bz_place_bag
ENDIF
RETURN

// ---------------------------------------------------------------- Haji Gul does not pay
bz_cheat:
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
IF NOT bag = -1
    REMOVE_PICKUP bag
    bag = -1
ENDIF
// the riders scatter
k = 0
WHILE k < 5
    IF NOT rblip[k] = -1
        REMOVE_BLIP rblip[k]
        rblip[k] = -1
    ENDIF
    IF NOT IS_CHAR_DEAD rider[k]
        IF IS_CHAR_IN_CAR rider[k] rbike[k]
            TASK_CAR_DRIVE_TO_COORD rider[k] rbike[k] @bazaar_car@ 25.0 MODE_STRAIGHTLINE -1 3
        ENDIF
    ENDIF
    k += 1
ENDWHILE
PRINT_NOW AFK_16 6000 1
CREATE_CAR BOBCAT @bz_car0@ car[0]
SET_CAR_HEADING car[0] @bz_car0.h@
CREATE_CAR BOBCAT @bz_car1@ car[1]
SET_CAR_HEADING car[1] @bz_car1.h@
k = 0
WHILE k < 2
    CREATE_CHAR_INSIDE_CAR car[k] PEDTYPE_MISSION1 162 cdrv[k]
    CREATE_CHAR_AS_PASSENGER car[k] PEDTYPE_MISSION1 200 0 cgun[k]
    GIVE_WEAPON_TO_CHAR cdrv[k] WEAPONTYPE_AK47 500
    GIVE_WEAPON_TO_CHAR cgun[k] WEAPONTYPE_AK47 900
    SET_CHAR_ACCURACY cdrv[k] 35
    SET_CHAR_ACCURACY cgun[k] 35
    SET_CHAR_DROPS_WEAPONS_WHEN_DEAD cdrv[k] FALSE
    SET_CHAR_DROPS_WEAPONS_WHEN_DEAD cgun[k] FALSE
    TASK_CAR_MISSION cdrv[k] car[k] -1 3 30.0 3
    TASK_DRIVE_BY cgun[k] scplayer -1 0.0 0.0 0.0 60.0 DRIVEBY_AI_ALL_DIRN TRUE 100
    k += 1
ENDWHILE
GIVE_WEAPON_TO_CHAR scplayer WEAPONTYPE_AK47 200
retask = 0
RETURN

bz_pass:
WAIT 2500
PRINT_NOW AFK_17 7000 1
WAIT 4000
mission_progress = 14
ADD_SCORE player 4000
PRINT_WITH_NUMBER_BIG M_PASS 4000 5000 1
REGISTER_MISSION_PASSED AFK_T
PLAYER_MADE_PROGRESS 1
PLAY_MISSION_PASSED_TUNE 1
RETURN

bz_contact:
IF NOT DOES_CHAR_EXIST contact_ped[0]
    CREATE_CHAR PEDTYPE_CIVMALE 158 @bazaar_uncle@ contact_ped[0]
    SET_CHAR_HEADING contact_ped[0] @bazaar_uncle.h@
    SET_CHAR_PROOFS contact_ped[0] TRUE TRUE TRUE TRUE TRUE
    SET_CHAR_NEVER_TARGETTED contact_ped[0] TRUE
    SET_CHAR_STAY_IN_SAME_PLACE contact_ped[0] TRUE
ENDIF
RETURN

bz_cleanup:
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
IF NOT bag = -1
    REMOVE_PICKUP bag
    bag = -1
ENDIF
k = 0
WHILE k < 5
    IF NOT rblip[k] = -1
        REMOVE_BLIP rblip[k]
        rblip[k] = -1
    ENDIF
    IF NOT rider[k] = -1
        MARK_CHAR_AS_NO_LONGER_NEEDED rider[k]
        rider[k] = -1
    ENDIF
    IF NOT rbike[k] = -1
        MARK_CAR_AS_NO_LONGER_NEEDED rbike[k]
        rbike[k] = -1
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
    IF NOT car[k] = -1
        MARK_CAR_AS_NO_LONGER_NEEDED car[k]
        car[k] = -1
    ENDIF
    k += 1
ENDWHILE
IF NOT pbike = -1
    MARK_CAR_AS_NO_LONGER_NEEDED pbike
    pbike = -1
ENDIF
IF DOES_CHAR_EXIST contact_ped[0]
    CLEAR_CHAR_TASKS contact_ped[0]
ENDIF
MARK_MODEL_AS_NO_LONGER_NEEDED 158
MARK_MODEL_AS_NO_LONGER_NEEDED 247
MARK_MODEL_AS_NO_LONGER_NEEDED 248
MARK_MODEL_AS_NO_LONGER_NEEDED 162
MARK_MODEL_AS_NO_LONGER_NEEDED 200
MARK_MODEL_AS_NO_LONGER_NEEDED 1210
MARK_MODEL_AS_NO_LONGER_NEEDED SANCHEZ
MARK_MODEL_AS_NO_LONGER_NEEDED BOBCAT
RESTORE_CAMERA_JUMPCUT
SWITCH_WIDESCREEN OFF
SET_PLAYER_CONTROL player ON
flag_player_on_mission = 0
MISSION_HAS_FINISHED
RETURN
}
