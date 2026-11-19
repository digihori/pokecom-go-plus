; PC-1350 top band: x=27..29 at 0x701B..0x701D,
; x=30..32 at 0x7200..0x7202. Work in this six-column strip only.
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
    LIA LOW(WINDOW-1)
    EXAM
    INCP
    LIA HIGH(WINDOW-1)
    EXAM
    LIA 3
    PUSH
FRAME:
    CALL DRAW_STRIP
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
    CALL DRAW_STRIP

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

; X points one byte before the six source columns. Preserve X so the
; caller can slide the window by one column with IX after each frame.
DRAW_STRIP:
    LIP 4
    LDM
    PUSH
    INCP
    LDM
    PUSH

    LIP 7
    LIA 0x70
    EXAM
    LIA 0x1A
    CALL COPY_THREE
    LIP 7
    LIA 0x71
    EXAM
    LIA 0xFF
    CALL COPY_THREE

    LIP 5
    POP
    EXAM
    DECP
    POP
    EXAM
    RTN

; A supplies the low byte of Y before the first write.
; IYS crosses from 0x71FF to 0x7200 in the second block.
COPY_THREE:
    LIP 6
    EXAM
    LIA 2
    PUSH
COPY_COLUMN:
    IXL
WRITE_COLUMN:
    IYS
    LOOP COPY_COLUMN
    RTN

WINDOW:
    DB 0, 0, 0, 0x04, 0x0E, 0x04, 0, 0, 0
BLANK:
    DB 0, 0, 0, 0, 0, 0
