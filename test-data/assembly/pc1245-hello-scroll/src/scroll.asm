; PC-1245: HELLO WORLD scrolls one dot to the left per frame.
; Entry 0xC500. BASIC: CALL 50432. One pass, then RTN (~8 seconds).
; No ROM routines. Preserve X/Y and I/J; A/P/Q/DP/flags are scratch.
; All program bytes are below 0xC600. LCD symbol bytes are untouched.
ORG 0xC500
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
; Right-aligned text: copy logical columns 0..59 first.
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
    LIA 0xF7
    EXAM
    LIA 59
    PUSH
DRAW:
    IXL
    IYS
    LOOP DRAW
; Copy remaining logical columns 60..79 (F87B down to F868).
    LIP 6
    LIA 0x7C
    EXAM
    INCP
    LIA 0xF8
    EXAM
    LIA 19
    PUSH
DRAW_TAIL:
    IXL
    DYS
    LOOP DRAW_TAIL
    LIA 79
    PUSH
FRAME:
; Roughly 0.1 second per frame at the normal PC-1245 clock.
    LIA 63
    PUSH
DELAY:
    WAIT 255
    LOOP DELAY
; Shift logical columns 0..58 from left to right to avoid overwrites.
    LIP 4
    LIA 0x00
    EXAM
    INCP
    LIA 0xF8
    EXAM
    INCP
    LIA 0xFF
    EXAM
    INCP
    LIA 0xF7
    EXAM
    LIA 58
    PUSH
SHIFT_LEFT_BANK:
    IXL
    IYS
    LOOP SHIFT_LEFT_BANK
; Bridge logical column 60 -> 59 without touching symbol addresses.
    LIDP 0xF87B
    LDD
    LIDP 0xF83B
    STD
; Shift logical columns 60..78 in the reversed-address bank.
    LIP 4
    LIA 0x7B
    EXAM
    INCP
    LIA 0xF8
    EXAM
    INCP
    LIA 0x7C
    EXAM
    INCP
    LIA 0xF8
    EXAM
    LIA 18
    PUSH
SHIFT_RIGHT_BANK:
    DXL
    DYS
    LOOP SHIFT_RIGHT_BANK
; Insert a blank column at the right edge.
    LIDP 0xF868
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
ORG 0xC5B0
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
