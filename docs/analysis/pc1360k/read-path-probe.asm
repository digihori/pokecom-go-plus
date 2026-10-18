; PC-1360K READ-path probe. Entry: CALL &C000.
; Requires free writable RAM C000..C203. Writes test bytes C100/C101,
; results C200..C203 and selector 3600 (left at 1, as in the glyph routine).
; Does not change F output or normal-ROM bank. Saves A, B and P.
; Current Studio baseline: results 03 12 03 12.
ORG 0xC000
    PUSH
    EXAB
    PUSH
    LDP
    PUSH
    LIA 0xA5
    LIDP 0xC100
    STD
    LIA 0x5A
    LIDP 0xC101
    STD
    LIA 0
    LIDP 0x3600
    STD
    LIDP 0xC100
    LDPC
    LIB 0x6D
    LIDP 0xC200
    STD
    LIDP 0xC101
    LDPC
    LIP 0x20
    LIDP 0xC201
    STD
    LIA 1
    LIDP 0x3600
    STD
    LIDP 0xC100
    LDPC
    LIB 0x6D
    LIDP 0xC202
    STD
    LIDP 0xC101
    LDPC
    LIP 0x20
    LIDP 0xC203
    STD
    POP
    STP
    POP
    EXAB
    POP
done:
    RTN
