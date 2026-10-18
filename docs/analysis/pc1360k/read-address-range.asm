; PC-1360K upper-address comparison at one fixed READ instruction.
; Entry C000; 16 results C200..C20F; leaves 3600=1.
; Saves A/B/P and stack; no F output or normal-ROM bank change.
ORG 0xC000
    PUSH
    EXAB
    PUSH
    LDP
    PUSH
    LIA 0
    LIDP 0x3600
    STD
    LIDP 0x0157
    CALL read_byte
    LIDP 0xC200
    STD
    LIDP 0x4157
    CALL read_byte
    LIDP 0xC201
    STD
    LIDP 0x8157
    CALL read_byte
    LIDP 0xC202
    STD
    LIDP 0xC157
    CALL read_byte
    LIDP 0xC203
    STD
    LIDP 0x237D
    CALL read_byte
    LIDP 0xC204
    STD
    LIDP 0x637D
    CALL read_byte
    LIDP 0xC205
    STD
    LIDP 0xA37D
    CALL read_byte
    LIDP 0xC206
    STD
    LIDP 0xE37D
    CALL read_byte
    LIDP 0xC207
    STD
    LIA 1
    LIDP 0x3600
    STD
    LIDP 0x0157
    CALL read_byte
    LIDP 0xC208
    STD
    LIDP 0x4157
    CALL read_byte
    LIDP 0xC209
    STD
    LIDP 0x8157
    CALL read_byte
    LIDP 0xC20A
    STD
    LIDP 0xC157
    CALL read_byte
    LIDP 0xC20B
    STD
    LIDP 0x237D
    CALL read_byte
    LIDP 0xC20C
    STD
    LIDP 0x637D
    CALL read_byte
    LIDP 0xC20D
    STD
    LIDP 0xA37D
    CALL read_byte
    LIDP 0xC20E
    STD
    LIDP 0xE37D
    CALL read_byte
    LIDP 0xC20F
    STD
    POP
    STP
    POP
    EXAB
    POP
done:
    RTN
read_byte:
    LDPC
    LIB 0x6D
    RTN
