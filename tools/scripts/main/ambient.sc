// Ambient life: villagers in towns, soldiers at Camp Sangar, police at the checkpoint,
// and a dry-climate weather cycle. Runs for the whole game.
MISSION_START
SCRIPT_NAME AMBIENT
SET_DEATHARREST_STATE OFF
amb_timer = 0
weather_timer = 0
amb_k = 0
WHILE amb_k < 10
    amb_ped[amb_k] = -1
    amb_k += 1
ENDWHILE

amb_loop:
WAIT 1000
IF IS_PLAYER_PLAYING player
    GET_CHAR_COORDINATES scplayer player_x player_y player_z
    IF NOT amb_active = -1
        GET_DISTANCE_BETWEEN_COORDS_2D player_x player_y amb_site_x[amb_active] amb_site_y[amb_active] amb_d
        IF amb_d > 330.0
            GOSUB amb_release
        ENDIF
    ENDIF
    IF amb_active = -1
        amb_i = 0
        WHILE amb_i < amb_site_count
            GET_DISTANCE_BETWEEN_COORDS_2D player_x player_y amb_site_x[amb_i] amb_site_y[amb_i] amb_d
            IF amb_d < 230.0
                amb_active = amb_i
                GOSUB amb_spawn
                amb_i = 99
            ENDIF
            amb_i += 1
        ENDWHILE
    ELSE
        amb_timer += 1
        IF amb_timer > 7
            amb_timer = 0
            GOSUB amb_wander
        ENDIF
    ENDIF
    GOSUB weather_cycle
ENDIF
GOTO amb_loop

// ------------------------------------------------------------------ spawn
amb_spawn:
amb_k = 0
WHILE amb_k < 10
    amb_j = amb_active
    amb_j *= 12
    amb_j += amb_k
    GOSUB amb_pick_model
    REQUEST_MODEL amb_model
    LOAD_ALL_MODELS_NOW
    IF amb_site_kind[amb_active] = 0
        CREATE_CHAR PEDTYPE_CIVMALE amb_model amb_spot_x[amb_j] amb_spot_y[amb_j] amb_spot_z[amb_j] amb_ped[amb_k]
    ELSE
        CREATE_CHAR PEDTYPE_MISSION2 amb_model amb_spot_x[amb_j] amb_spot_y[amb_j] amb_spot_z[amb_j] amb_ped[amb_k]
        IF amb_site_kind[amb_active] = 1
            REQUEST_MODEL 356
            LOAD_ALL_MODELS_NOW
            GIVE_WEAPON_TO_CHAR amb_ped[amb_k] WEAPONTYPE_M4 500
        ELSE
            REQUEST_MODEL 355
            LOAD_ALL_MODELS_NOW
            GIVE_WEAPON_TO_CHAR amb_ped[amb_k] WEAPONTYPE_AK47 500
        ENDIF
        SET_CHAR_DROPS_WEAPONS_WHEN_DEAD amb_ped[amb_k] FALSE
    ENDIF
    GENERATE_RANDOM_FLOAT_IN_RANGE 0.0 359.0 amb_d
    SET_CHAR_HEADING amb_ped[amb_k] amb_d
    MARK_MODEL_AS_NO_LONGER_NEEDED amb_model
    amb_k += 1
ENDWHILE
amb_timer = 6
RETURN

amb_pick_model:
IF amb_site_kind[amb_active] = 1
    amb_model = 287          // ARMY
    RETURN
ENDIF
IF amb_site_kind[amb_active] = 2
    GENERATE_RANDOM_INT_IN_RANGE 0 2 amb_i
    amb_model = 283          // CSHER
    IF amb_i = 1
        amb_model = 288      // DSHER
    ENDIF
    RETURN
ENDIF
// rural / desert civilians (men and women, young and old)
GENERATE_RANDOM_INT_IN_RANGE 0 14 amb_i
IF amb_i = 0
    amb_model = 128
ENDIF
IF amb_i = 1
    amb_model = 132
ENDIF
IF amb_i = 2
    amb_model = 133
ENDIF
IF amb_i = 3
    amb_model = 158
ENDIF
IF amb_i = 4
    amb_model = 161
ENDIF
IF amb_i = 5
    amb_model = 202
ENDIF
IF amb_i = 6
    amb_model = 32
ENDIF
IF amb_i = 7
    amb_model = 34
ENDIF
IF amb_i = 8
    amb_model = 129
ENDIF
IF amb_i = 9
    amb_model = 130
ENDIF
IF amb_i = 10
    amb_model = 196
ENDIF
IF amb_i = 11
    amb_model = 201
ENDIF
IF amb_i = 12
    amb_model = 31
ENDIF
IF amb_i = 13
    amb_model = 160
ENDIF
RETURN

// ------------------------------------------------------------------ wander between open spots
amb_wander:
amb_k = 0
WHILE amb_k < 10
    IF NOT amb_ped[amb_k] = -1
    AND DOES_CHAR_EXIST amb_ped[amb_k]
        IF NOT IS_CHAR_DEAD amb_ped[amb_k]
            GENERATE_RANDOM_INT_IN_RANGE 0 3 amb_i
            IF amb_i > 0
                GENERATE_RANDOM_INT_IN_RANGE 0 12 amb_j
                amb_i = amb_active
                amb_i *= 12
                amb_j += amb_i
                amb_tx = amb_spot_x[amb_j]
                amb_ty = amb_spot_y[amb_j]
                amb_tz = amb_spot_z[amb_j]
                TASK_GO_STRAIGHT_TO_COORD amb_ped[amb_k] amb_tx amb_ty amb_tz PEDMOVE_WALK 20000
            ENDIF
        ENDIF
    ENDIF
    amb_k += 1
ENDWHILE
RETURN

amb_release:
amb_k = 0
WHILE amb_k < 10
    IF NOT amb_ped[amb_k] = -1
        IF DOES_CHAR_EXIST amb_ped[amb_k]
            MARK_CHAR_AS_NO_LONGER_NEEDED amb_ped[amb_k]
        ENDIF
        amb_ped[amb_k] = -1
    ENDIF
    amb_k += 1
ENDWHILE
amb_active = -1
RETURN

// ------------------------------------------------------------------ weather: mostly clear and dry
weather_cycle:
weather_timer += 1
IF weather_timer < 300
    RETURN
ENDIF
weather_timer = 0
GENERATE_RANDOM_INT_IN_RANGE 0 20 weather_pick
IF weather_pick < 10
    FORCE_WEATHER WEATHER_EXTRASUNNY_DESERT
    RETURN
ENDIF
IF weather_pick < 15
    FORCE_WEATHER WEATHER_SUNNY_DESERT
    RETURN
ENDIF
IF weather_pick < 17
    FORCE_WEATHER WEATHER_CLOUDY_COUNTRYSIDE
    RETURN
ENDIF
IF weather_pick < 19
    FORCE_WEATHER WEATHER_SANDSTORM_DESERT
    RETURN
ENDIF
FORCE_WEATHER WEATHER_RAINY_COUNTRYSIDE
RETURN
MISSION_END
