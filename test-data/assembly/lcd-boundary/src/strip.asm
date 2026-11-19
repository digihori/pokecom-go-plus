; Four-column strip crossing one LCD address boundary in the first band.
; The build substitutes the target's RAM entry and physical VRAM addresses.
; Preserve X/Y and port C. A/P/Q/DP/flags are scratch.
ORG ${entry}
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
    LIP 0x5F
    LDM
    PUSH
    ORIM 1
    OUTC

    LIP 4
    LIA LOW(WINDOW-1)
    EXAM
    INCP
    LIA HIGH(WINDOW-1)
    EXAM
    LIA 2
    PUSH
FRAME:
    CALL DRAW_STRIP
FRAME_DRAWN:
    LIA 63
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
    CALL DRAW_STRIP
STRIP_CLEARED:
    LIP 0x5F
    POP
    EXAM
    OUTC
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

; X = address before a four-column window. Preserve X for the frame loop.
DRAW_STRIP:
    LIP 4
    LDM
    PUSH
    INCP
    LDM
    PUSH

    LIP 6
    LIA LOW(${left})
    EXAM
    INCP
    LIA HIGH(${left})
    EXAM
    CALL COPY_LEFT
    LIP 6
    LIA LOW(${right})
    EXAM
    INCP
    LIA HIGH(${right})
    EXAM
    CALL COPY_RIGHT

    LIP 5
    POP
    EXAM
    DECP
    POP
    EXAM
    RTN

COPY_LEFT:
    LIA 1
    PUSH
LEFT_COLUMN:
    IXL
WRITE_LEFT:
    IYS
    LOOP LEFT_COLUMN
    RTN

COPY_RIGHT:
    LIA 1
    PUSH
RIGHT_COLUMN:
    IXL
WRITE_RIGHT:
    ${rightStore}
    LOOP RIGHT_COLUMN
    RTN

WINDOW:
    DB 0,0,0x0E,0x04,0,0
BLANK:
    DB 0,0,0,0
