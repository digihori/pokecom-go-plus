; PC-1261, 120x14 dots. Animate a six-column strip at x=57..62.
; The two bands share a window offset and a three-column copy routine.
; Save X/Y. A/P/Q/DP/flags are scratch; port C is untouched.
ORG 0x6000
START:
    LIP 4
    LDM
    PUSH
    INCP
    LDM
    PUSH
    INCP
    LDM
    PUSH
    INCP
    LDM
    PUSH

    LIP 4
    LIA LOW(TOP-1)
    EXAM
    INCP
    LIA HIGH(TOP-1)
    EXAM
    LIA 3
    PUSH
FRAME:
    CALL DRAW_FRAME
FRAME_DRAWN:
    LIA 127
    PUSH
DELAY:
    WAIT 255
    LOOP DELAY
DELAY_DONE:
    IX
    LOOP FRAME

    LIP 4
    LIA LOW(BLANK-1)
    EXAM
    INCP
    LIA HIGH(BLANK-1)
    EXAM
    CALL DRAW_FRAME

    LIP 7
    POP
    EXAM
    DECP
    POP
    EXAM
    DECP
    POP
    EXAM
    DECP
    POP
    EXAM
    RTN

; X = one byte before the upper window. Preserve it for the frame loop.
DRAW_FRAME:
    LIP 4
    LDM
    PUSH
    INCP
    LDM
    PUSH

    LIA 0
    CALL DRAW_BAND
    ; Six bytes were consumed. Advance X by the rest of the strip spacing.
    LIP 4
    ADIM BOTTOM-TOP-6
    JPNC BOTTOM_READY
    INCP
    ADIM 1
BOTTOM_READY:
    LIA 0x40
    CALL DRAW_BAND

    LIP 5
    POP
    EXAM
    DECP
    POP
    EXAM
    RTN

; A = band offset (0 or 0x40). Compute both VRAM blocks for this band.
DRAW_BAND:
    PUSH
    ADIA 0x39
    LIP 6
    EXAM
    INCP
    LIA 0x20
    EXAM
    CALL COPY_THREE
    POP
    LIP 6
    EXAM
    INCP
    LIA 0x28
    EXAM
    CALL COPY_THREE
    RTN

; Y = first destination. IYS increments before each store.
COPY_THREE:
    DY
    LIA 2
    PUSH
COPY_COLUMN:
    IXL
WRITE_COLUMN:
    IYS
    LOOP COPY_COLUMN
    RTN

TOP:
    DB 0,0,0,0x40,0x70,0x40,0,0,0
BOTTOM:
    DB 0,0,0,0x01,0x07,0x01,0,0,0
BLANK:
    DS BOTTOM-TOP+6,0
