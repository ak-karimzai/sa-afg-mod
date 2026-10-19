// Afghan radio (CLEO 4/5): plays the mod's own music in vehicles instead of the SA stations.
// N = next track, M = radio on/off. Replace the mp3 files in CLEO/afghan_radio to use your own music.
SCRIPT_START
{
LVAR_INT scplayer stream loaded track incar radio_on state shown since
SCRIPT_NAME AFRADIO
radio_on = 1
track = 0
incar = 0
loaded = 0
shown = 0

radio_loop:
WAIT 100
IF NOT IS_PLAYER_PLAYING 0
    GOTO radio_loop
ENDIF
GET_PLAYER_CHAR 0 scplayer
IF IS_KEY_PRESSED 77
    WHILE IS_KEY_PRESSED 77
        WAIT 0
    ENDWHILE
    IF radio_on = 1
        radio_on = 0
        GOSUB radio_stop
    ELSE
        radio_on = 1
        incar = 0
    ENDIF
ENDIF
IF IS_CHAR_IN_ANY_CAR scplayer
AND radio_on = 1
    IF incar = 0
        incar = 1
        since = 0
        IF shown = 0
            PRINT_HELP AFG_RAD
            shown = 1
        ENDIF
        GOSUB radio_play
    ENDIF
    // the game switches its own station on when you get in: keep it off for a few seconds
    IF since < 40
        SET_RADIO_CHANNEL RS_OFF
    ENDIF
    since += 1
    IF IS_KEY_PRESSED 78
        WHILE IS_KEY_PRESSED 78
            WAIT 0
        ENDWHILE
        GOSUB radio_next
    ENDIF
    IF loaded = 1
    AND since > 15
        GET_AUDIO_STREAM_STATE stream state
        IF NOT state = 1
        AND NOT state = 2
            GOSUB radio_next
        ENDIF
    ENDIF
ELSE
    IF incar = 1
        incar = 0
        GOSUB radio_stop
    ENDIF
ENDIF
GOTO radio_loop

radio_next:
track += 1
IF track > 5
    track = 0
ENDIF
GOSUB radio_stop
GOSUB radio_play
since = 16
RETURN

radio_play:
IF track = 0
    LOAD_AUDIO_STREAM "CLEO/afghan_radio/01_shomali_morning.mp3" stream
ENDIF
IF track = 1
    LOAD_AUDIO_STREAM "CLEO/afghan_radio/02_mughulchi.mp3" stream
ENDIF
IF track = 2
    LOAD_AUDIO_STREAM "CLEO/afghan_radio/03_valley_night.mp3" stream
ENDIF
IF track = 3
    LOAD_AUDIO_STREAM "CLEO/afghan_radio/04_bazaar_run.mp3" stream
ENDIF
IF track = 4
    LOAD_AUDIO_STREAM "CLEO/afghan_radio/05_mountain_road.mp3" stream
ENDIF
IF track = 5
    LOAD_AUDIO_STREAM "CLEO/afghan_radio/06_attan.mp3" stream
ENDIF
loaded = 1
SET_AUDIO_STREAM_VOLUME stream 0.85
SET_AUDIO_STREAM_STATE stream 1
RETURN

radio_stop:
IF loaded = 1
    SET_AUDIO_STREAM_STATE stream 0
    REMOVE_AUDIO_STREAM stream
    loaded = 0
ENDIF
RETURN
}
SCRIPT_END
