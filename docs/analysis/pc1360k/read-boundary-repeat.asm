; PC-1360K: eight consecutive bytes across 3FFF/4000, selectors 0/1.
; Two complete passes; same READ site throughout. Entry C000.
; Results C200..C21F; saves A/B/P; leaves 3600=1; no F/bank writes.
ORG 0xC000
    PUSH
    EXAB
    PUSH
    LDP
    PUSH
    LIA 0
    LIDP 0x3600
    STD
    LIDP 0x3FFC
    CALL read_byte
    LIDP 0xC200
    STD
    LIDP 0x3FFD
    CALL read_byte
    LIDP 0xC201
    STD
    LIDP 0x3FFE
    CALL read_byte
    LIDP 0xC202
    STD
    LIDP 0x3FFF
    CALL read_byte
    LIDP 0xC203
    STD
    LIDP 0x4000
    CALL read_byte
    LIDP 0xC204
    STD
    LIDP 0x4001
    CALL read_byte
    LIDP 0xC205
    STD
    LIDP 0x4002
    CALL read_byte
    LIDP 0xC206
    STD
    LIDP 0x4003
    CALL read_byte
    LIDP 0xC207
    STD
    LIA 1
    LIDP 0x3600
    STD
    LIDP 0x3FFC
    CALL read_byte
    LIDP 0xC208
    STD
    LIDP 0x3FFD
    CALL read_byte
    LIDP 0xC209
    STD
    LIDP 0x3FFE
    CALL read_byte
    LIDP 0xC20A
    STD
    LIDP 0x3FFF
    CALL read_byte
    LIDP 0xC20B
    STD
    LIDP 0x4000
    CALL read_byte
    LIDP 0xC20C
    STD
    LIDP 0x4001
    CALL read_byte
    LIDP 0xC20D
    STD
    LIDP 0x4002
    CALL read_byte
    LIDP 0xC20E
    STD
    LIDP 0x4003
    CALL read_byte
    LIDP 0xC20F
    STD
    LIA 0
    LIDP 0x3600
    STD
    LIDP 0x3FFC
    CALL read_byte
    LIDP 0xC210
    STD
    LIDP 0x3FFD
    CALL read_byte
    LIDP 0xC211
    STD
    LIDP 0x3FFE
    CALL read_byte
    LIDP 0xC212
    STD
    LIDP 0x3FFF
    CALL read_byte
    LIDP 0xC213
    STD
    LIDP 0x4000
    CALL read_byte
    LIDP 0xC214
    STD
    LIDP 0x4001
    CALL read_byte
    LIDP 0xC215
    STD
    LIDP 0x4002
    CALL read_byte
    LIDP 0xC216
    STD
    LIDP 0x4003
    CALL read_byte
    LIDP 0xC217
    STD
    LIA 1
    LIDP 0x3600
    STD
    LIDP 0x3FFC
    CALL read_byte
    LIDP 0xC218
    STD
    LIDP 0x3FFD
    CALL read_byte
    LIDP 0xC219
    STD
    LIDP 0x3FFE
    CALL read_byte
    LIDP 0xC21A
    STD
    LIDP 0x3FFF
    CALL read_byte
    LIDP 0xC21B
    STD
    LIDP 0x4000
    CALL read_byte
    LIDP 0xC21C
    STD
    LIDP 0x4001
    CALL read_byte
    LIDP 0xC21D
    STD
    LIDP 0x4002
    CALL read_byte
    LIDP 0xC21E
    STD
    LIDP 0x4003
    CALL read_byte
    LIDP 0xC21F
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
