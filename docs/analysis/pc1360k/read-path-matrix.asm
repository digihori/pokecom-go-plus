; PC-1360K READ matrix: same READ site, two DP values, two next opcodes.
; Entry C000. Code below C100; test data C100/C101; results C200..C207.
; Saves A/B/P. Leaves 3600=1. Does not change F output or normal ROM bank.
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
    CALL read_lib
    LIDP 0xC200
    STD
    LIDP 0xC100
    CALL read_lip
    LIDP 0xC201
    STD
    LIDP 0xC101
    CALL read_lib
    LIDP 0xC202
    STD
    LIDP 0xC101
    CALL read_lip
    LIDP 0xC203
    STD
    LIA 1
    LIDP 0x3600
    STD
    LIDP 0xC100
    CALL read_lib
    LIDP 0xC204
    STD
    LIDP 0xC100
    CALL read_lip
    LIDP 0xC205
    STD
    LIDP 0xC101
    CALL read_lib
    LIDP 0xC206
    STD
    LIDP 0xC101
    CALL read_lip
    LIDP 0xC207
    STD
    POP
    STP
    POP
    EXAB
    POP
done:
    RTN
read_lib:
    LDPC
    LIB 0x6D
    RTN
read_lip:
    LDPC
    LIP 0x20
    RTN
