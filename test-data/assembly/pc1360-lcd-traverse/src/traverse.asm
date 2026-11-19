; PC-1360 / PC-1360K: title and synchronized two-band sprite.
; Call from short BASIC at &C000. X/Y are saved; A/P/Q/DP/flags are scratch.
WIDTH EQU 150
BLOCK_WIDTH EQU 30
BLOCKS EQU 5
SPRITE_WIDTH EQU 3
TRAVEL EQU WIDTH-SPRITE_WIDTH
ORG 0xC000
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
    LIA LOW(TITLE-1)
    EXAM
    INCP
    LIA HIGH(TITLE-1)
    EXAM
    LIA 0
    CALL DRAW_BAND

    LIP 4
    LIA LOW(TOP-1)
    EXAM
    INCP
    LIA HIGH(TOP-1)
    EXAM
    LIA TRAVEL
    PUSH
FRAME:
    CALL DRAW_FRAME
FRAME_DRAWN:
    LIA 15
    PUSH
WAIT_FRAME:
    WAIT 255
    LOOP WAIT_FRAME
DELAY_DONE:
    IX
    LOOP FRAME

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
DONE:
    RTN

; Use the same offset for both source strips and keep it for the caller.
DRAW_FRAME:
    LIP 4
    LDM
    PUSH
    INCP
    LDM
    PUSH
    LIA 0x40
    CALL DRAW_BAND
    ; X advanced WIDTH bytes. Skip the rest of the upper strip.
    LIP 4
    ADIM BOTTOM-TOP-WIDTH
    JPNC LOWER_READY
    INCP
    ADIM 1
LOWER_READY:
    LIA 0x1E
    CALL DRAW_BAND
    LIP 5
    POP
    EXAM
    DECP
    POP
    EXAM
    RTN

; A = band offset. X = one byte before the source window.
; Write 5 blocks of 30 columns, resetting Y for each VRAM block.
DRAW_BAND:
    LIP 6
    EXAM
    INCP
    LIA 0x28
    EXAM
    DY
    LIA BLOCKS-1
    PUSH
NEXT_BLOCK:
    LIA BLOCK_WIDTH-1
    PUSH
COPY_COLUMN:
    IXL
WRITE_COLUMN:
    IYS
    LOOP COPY_COLUMN
    ; Y is at the last write. Subtract 30 and add the 0x200 block stride.
    LIP 6
    SBIM BLOCK_WIDTH
    JPC BLOCK_BORROW
    INCP
    ADIM 2
    JP BLOCK_READY
BLOCK_BORROW:
    INCP
    ADIM 1
BLOCK_READY:
    LOOP NEXT_BLOCK
    RTN

TITLE:
    DS 45,0
    DB 0x7F,0x09,0x09,0x09,0x06,0
    DB 0x3E,0x41,0x41,0x41,0x3E,0
    DB 0x7F,0x08,0x14,0x22,0x41,0
    DB 0x7F,0x49,0x49,0x49,0x41,0
    DB 0x3E,0x41,0x41,0x41,0x22,0
    DB 0x3E,0x41,0x41,0x41,0x3E,0
    DB 0x7F,0x02,0x0C,0x02,0x7F,0
    DS 6,0
    DB 0x3E,0x41,0x49,0x49,0x7A,0
    DB 0x3E,0x41,0x41,0x41,0x3E,0
    DS 45,0

; Each 297-column strip contains every complete 150-column window.
TOP:
    DS TRAVEL,0
    DB 0x80,0xE0,0x80
    DS TRAVEL,0
BOTTOM:
    DS TRAVEL,0
    DB 0x01,0x07,0x01
    DS TRAVEL,0
BOTTOM_END:
