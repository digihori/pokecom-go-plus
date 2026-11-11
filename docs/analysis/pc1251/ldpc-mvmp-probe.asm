; PC-1251 LDPC/MVMP source probe. No I/O or selector writes.
ORG 0xC500
    PUSH
    EXAB
    PUSH
    LDP
    PUSH
    LIP 0x20
    LDM
    PUSH
    LIA 0xA5
    LIDP 0xC590
    STD
    LIA 0x5A
    LIDP 0xC591
    STD
    LIDP 0xC590
    CALL ldpc_a
    LIDP 0xC592
    STD
    LIDP 0xC591
    CALL ldpc_a
    LIDP 0xC593
    STD
    LIDP 0xC590
    CALL ldpc_b
    LIDP 0xC594
    STD
    LIDP 0xC591
    CALL ldpc_b
    LIDP 0xC595
    STD
    LIDP 0xC590
    CALL mvmp_a
    LIDP 0xC596
    STD
    LIDP 0xC591
    CALL mvmp_a
    LIDP 0xC597
    STD
    LIDP 0xC590
    CALL mvmp_b
    LIDP 0xC598
    STD
    LIDP 0xC591
    CALL mvmp_b
    LIDP 0xC599
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
