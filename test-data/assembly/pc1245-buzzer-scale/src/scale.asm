; PC-1245, 192 kHz. Call once from BASIC at &C500.
; Save I, X, internal RAM[8] and port C. A/P/Q/DP/flags are scratch.
ORG 0xC500
START:
    LIP 0
    LDM
    PUSH
    LIP 4
    LDM
    PUSH
    INCP
    LDM
    PUSH
    LIP 8
    LDM
    PUSH
    LIP 0x5F
    LDM
    PUSH
    ANIM 0x8F
    OUTC

    LIP 4
    LIA LOW(NOTES-1)
    EXAM
    INCP
    LIA HIGH(NOTES-1)
    EXAM
    LIA NOTE_COUNT-1
    PUSH
NEXT_NOTE:
    IXL
    CALL PLAY_NOTE
    LOOP NEXT_NOTE

    LIP 0x5F
    POP
    EXAM
    OUTC
    LIP 8
    POP
    EXAM
    LIP 5
    POP
    EXAM
    DECP
    POP
    EXAM
    LIP 0
    POP
    EXAM
DONE:
    RTN

; A = half-period loop count. Emit 96 periods using a shared pulse loop.
PLAY_NOTE:
    LIP 8
    EXAM
    LIA 95
    PUSH
PULSE:
    LIP 0x5F
    ORIM 0x10
    OUTC
HIGH_EDGE:
    LIP 8
    LDM
    LIP 0
    EXAM
HIGH_WAIT:
    DECI
    JRNZM HIGH_WAIT
    LIP 0x5F
    ANIM 0xEF
    OUTC
LOW_EDGE:
    LIP 8
    LDM
    LIP 0
    EXAM
LOW_WAIT:
    DECI
    JRNZM LOW_WAIT
    LOOP PULSE
    RTN

; Period during the note is 50 + 22*N cycles.
NOTES:
    DB 31, 27, 24, 23, 20
NOTES_END:
NOTE_COUNT EQU NOTES_END-NOTES
