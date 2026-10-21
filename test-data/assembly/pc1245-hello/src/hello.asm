; PC-1245 / Studio built-in SC61860 assembler.
; Independent 5x7 glyphs; bit 0 is the top pixel. BASIC CALL 50432.
; Keep code/data below C600: BASIC uses the upper RAM as workspace.
; A, P, Q, DP and flags are scratch. Preserve X/Y and leave I/J intact.
ORG 0xC500
HELLO:
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
; X = TABLE-1, Y = F7FF. IXL/IYS increment before accessing memory.
    LIP 4
    LIA 0x7F
    EXAM
    INCP
    LIA 0xC5
    EXAM
    INCP
    LIA 0xFF
    EXAM
    INCP
    LIA 0xF7
    EXAM
    LIA 59
    PUSH
DRAW: IXL
    IYS
    LOOP DRAW
; Logical columns 60..79 occupy F87B..F868 in descending order.
    LIP 6
    LIA 0x7C
    EXAM
    INCP
    LIA 0xF8
    EXAM
    LIA 19
    PUSH
    LIA 0
CLEAR_TAIL: DYS
    LOOP CLEAR_TAIL
; About 3.3 seconds at 192000 cycles/s. LOOP consumes its stack counter.
HOLD_START: LIA 8
    PUSH
OUTER: LIA 255
    PUSH
HOLD: WAIT 255
    LOOP HOLD
    LOOP OUTER
; Restore X/Y in reverse push order before returning to BASIC.
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
DONE: RTN
ORG 0xC580
TABLE:
    DB 0x7F, 0x08, 0x08, 0x08, 0x7F
    DB 0x7F, 0x49, 0x49, 0x49, 0x41
    DB 0x7F, 0x40, 0x40, 0x40, 0x40
    DB 0x7F, 0x40, 0x40, 0x40, 0x40
    DB 0x3E, 0x41, 0x41, 0x41, 0x3E
    DB 0x00, 0x00, 0x00, 0x00, 0x00
    DB 0x7F, 0x20, 0x18, 0x20, 0x7F
    DB 0x3E, 0x41, 0x41, 0x41, 0x3E
    DB 0x7F, 0x09, 0x19, 0x29, 0x46
    DB 0x7F, 0x40, 0x40, 0x40, 0x40
    DB 0x7F, 0x41, 0x41, 0x41, 0x3E
    DB 0x00, 0x00, 0x00, 0x00, 0x00
