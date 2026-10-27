; PC-1401: HELLO WORLD scrolls one dot to the left per frame.
; Entry 0x4400. BASIC: CALL 17408. One pass, then RTN (~8 seconds).
; No ROM routines. Preserve X/Y and I/J; A/P/Q/DP/flags are scratch.
; All program bytes are below 0x4500. LCD symbol bytes are untouched.
ORG 0x4400
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
    ORIM 1
    OUTC
; Right-aligned text: copy logical columns 0..39 first.
    LIP 4
    LIA LOW(TABLE-1)
    EXAM
    INCP
    LIA HIGH(TABLE-1)
    EXAM
    INCP
    LIA 0xFF
    EXAM
    INCP
    LIA 0x5F
    EXAM
    LIA 39
    PUSH
DRAW:
    IXL
    IYS
    LOOP DRAW
; Copy remaining logical columns 40..79 (6067 down to 6040).
    LIP 6
    LIA 0x68
    EXAM
    INCP
    LIA 0x60
    EXAM
    LIA 39
    PUSH
DRAW_TAIL:
    IXL
    DYS
    LOOP DRAW_TAIL
    LIA 79
    PUSH
FRAME:
; Roughly 0.1 second per frame at the normal PC-1401 clock.
    LIA 63
    PUSH
DELAY:
    WAIT 255
    LOOP DELAY
; Shift logical columns 0..38 from left to right to avoid overwrites.
    LIP 4
    LIA 0x00
    EXAM
    INCP
    LIA 0x60
    EXAM
    INCP
    LIA 0xFF
    EXAM
    INCP
    LIA 0x5F
    EXAM
    LIA 38
    PUSH
SHIFT_LEFT_BANK:
    IXL
    IYS
    LOOP SHIFT_LEFT_BANK
; Bridge logical column 40 -> 39 without touching symbol addresses.
    LIDP 0x6067
    LDD
    LIDP 0x6027
    STD
; Shift logical columns 40..78 in the reversed-address bank.
    LIP 4
    LIA 0x67
    EXAM
    INCP
    LIA 0x60
    EXAM
    INCP
    LIA 0x68
    EXAM
    INCP
    LIA 0x60
    EXAM
    LIA 38
    PUSH
SHIFT_RIGHT_BANK:
    DXL
    DYS
    LOOP SHIFT_RIGHT_BANK
; Insert a blank column at the right edge.
    LIDP 0x6040
    LIA 0
    STD
FRAME_END:
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
    LIA 0
DONE:
    RTN
ORG 0x44B0
TABLE:
    DB 0,0,0,0,0
    DB 0,0,0,0,0
    DB 0,0,0,0,0
    DB 0,0,0,0,0
    DB 0,0,0,0,0
    DB 0x7F,0x08,0x08,0x08,0x7F ; H
    DB 0x7F,0x49,0x49,0x49,0x41 ; E
    DB 0x7F,0x40,0x40,0x40,0x40 ; L
    DB 0x7F,0x40,0x40,0x40,0x40 ; L
    DB 0x3E,0x41,0x41,0x41,0x3E ; O
    DB 0,0,0,0,0                ; space
    DB 0x7F,0x20,0x18,0x20,0x7F ; W
    DB 0x3E,0x41,0x41,0x41,0x3E ; O
    DB 0x7F,0x09,0x19,0x29,0x46 ; R
    DB 0x7F,0x40,0x40,0x40,0x40 ; L
    DB 0x7F,0x41,0x41,0x41,0x3E ; D
