; One DP-dependent READ per CALL; BASIC patches selector and source operands.
; A/B/P saved; 3600 left at 1; output C200; no F/bank writes.
ORG 0xC000
    PUSH
    EXAB
    PUSH
    LDP
    PUSH
select:
    LIA 0
    LIDP 0x3600
    STD
source:
    LIDP 0x0100
    LDPC
    LIB 0x6D
    LIDP 0xC200
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
