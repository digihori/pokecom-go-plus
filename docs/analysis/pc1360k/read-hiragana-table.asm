; PC-1360K: read the six candidate table bytes for Hiragana A.
; Entry C000; outputs C200..C205; 3600 is left at 1.
; Saves A/B/P; no F output or normal-ROM bank change.
ORG 0xC000
    PUSH
    EXAB
    PUSH
    LDP
    PUSH
    LIA 0
    LIDP 0x3600
    STD
    LIDP 0x0156
    LDPC
    LIB 0x6D
    LIDP 0xC200
    STD
    LIDP 0x0157
    LDPC
    LIB 0x6D
    LIDP 0xC201
    STD
    LIDP 0x0158
    LDPC
    LIB 0x6D
    LIDP 0xC202
    STD
    LIDP 0x0159
    LDPC
    LIB 0x6D
    LIDP 0xC203
    STD
    LIDP 0x015A
    LDPC
    LIB 0x6D
    LIDP 0xC204
    STD
    LIDP 0x015B
    LDPC
    LIB 0x6D
    LIDP 0xC205
    STD
    LIA 1
    LIDP 0x3600
    STD
    POP
    STP
    POP
    EXAB
    POP
done:
    RTN
