; Fixed 4096-byte copy and independent reread comparison.
; Params C200 selector, C201/C202 source-minus-one little endian.
; Output D000..DFFF. Results C210..C215. No F/3400 writes.
ORG 0xC000
    PUSH
    LDP
    PUSH
    LDQ
    PUSH
    LIP 0
    LDM
    PUSH
    LIP 1
    LDM
    PUSH
    LIP 3
    LDM
    PUSH
    LIP 4
    LDM
    PUSH
    LIP 5
    LDM
    PUSH
    LIP 6
    LDM
    PUSH
    LIP 7
    LDM
    PUSH
    LIP 8
    LDM
    PUSH
    LIP 9
    LDM
    PUSH
    LIP 10
    LDM
    PUSH
    LIP 11
    LDM
    PUSH
    LIP 12
    LDM
    PUSH
    LIA 0
    LIP 8
    EXAM
    LIA 0
    LIP 9
    EXAM
    LIA 0
    LIP 10
    EXAM
    LIA 0
    LIP 11
    EXAM
    LIA 0
    LIP 12
    EXAM
    CALL setup
copy_loop:
    IX
copy_read:
    LDPC
    LIB 0x6D
    LIP 10
    ADM
    JPNC copy_store
    LIP 11
    ADIM 1
    JPNC copy_store
    LIP 12
    ADIM 1
copy_store:
    IYS
    DECI
    JPNZ copy_loop
    DECJ
    JPNZ copy_loop
    LIA 1
    LIDP 0x3600
    STD
    CALL setup
verify_loop:
    IX
verify_read:
    LDPC
    LIB 0x6D
    EXAB
    IY
    LDD
    LIP 3
    CPMA
    JPZ verify_next
    INCK
    JPNZ verify_next
    INCL
verify_next:
    DECI
    JPNZ verify_loop
    DECJ
    JPNZ verify_loop
    LIP 8
    LDM
    LIDP 0xC210
    STD
    LIP 9
    LDM
    LIDP 0xC211
    STD
    LIP 10
    LDM
    LIDP 0xC212
    STD
    LIP 11
    LDM
    LIDP 0xC213
    STD
    LIP 12
    LDM
    LIDP 0xC214
    STD
    LIA 1
    LIDP 0x3600
    STD
    LIA 165
    LIDP 0xC215
    STD
    POP
    LIP 12
    EXAM
    POP
    LIP 11
    EXAM
    POP
    LIP 10
    EXAM
    POP
    LIP 9
    EXAM
    POP
    LIP 8
    EXAM
    POP
    LIP 7
    EXAM
    POP
    LIP 6
    EXAM
    POP
    LIP 5
    EXAM
    POP
    LIP 4
    EXAM
    POP
    LIP 3
    EXAM
    POP
    LIP 1
    EXAM
    POP
    LIP 0
    EXAM
    POP
    STQ
    POP
    STP
    POP
done:
    RTN
setup:
    LIDP 0xC200
    LDD
    LIDP 0x3600
    STD
    LIDP 0xC201
    LDD
    LIP 4
    EXAM
    LIDP 0xC202
    LDD
    LIP 5
    EXAM
    LIA 0xFF
    LIP 6
    EXAM
    LIA 0xCF
    LIP 7
    EXAM
    LII 0
    LIJ 16
    RTN
