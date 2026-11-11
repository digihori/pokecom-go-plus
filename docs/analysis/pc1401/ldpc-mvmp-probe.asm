; PC-1401 LDPC/MVMP source probe. No I/O or selector writes.
ORG 0x4400
    PUSH
    EXAB
    PUSH
    LDP
    PUSH
    LIP 0x20
    LDM
    PUSH
    LIA 0xA5
    LIDP 0x4490
    STD
    LIA 0x5A
    LIDP 0x4491
    STD
    LIDP 0x4490
    CALL ldpc_a
    LIDP 0x4492
    STD
    LIDP 0x4491
    CALL ldpc_a
    LIDP 0x4493
    STD
    LIDP 0x4490
    CALL ldpc_b
    LIDP 0x4494
    STD
    LIDP 0x4491
    CALL ldpc_b
    LIDP 0x4495
    STD
    LIDP 0x4490
    CALL mvmp_a
    LIDP 0x4496
    STD
    LIDP 0x4491
    CALL mvmp_a
    LIDP 0x4497
    STD
    LIDP 0x4490
    CALL mvmp_b
    LIDP 0x4498
    STD
    LIDP 0x4491
    CALL mvmp_b
    LIDP 0x4499
    STD
    POP
    LIP 0x20
    EXAM
    POP
    STP
    POP
    EXAB
    POP
    RTN
ldpc_a:
    LDPC
    LIB 0
    RTN
ldpc_b:
    LDPC
    LIP 0x20
    RTN
mvmp_a:
    LIP 0x20
    MVMP
    LDM
    RTN
mvmp_b:
    LIP 0x20
    MVMP
    LIP 0x20
    LDM
    RTN
