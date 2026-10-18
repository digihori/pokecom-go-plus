; PC-1360K: 30-byte Hiragana A glyph candidate, selector 0, DP 2378..2395.
; Entry C000. Code below C200; results C200..C21D; leaves 3600=1.
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
    LIDP 0x2378
    LDPC
    LIB 0x6D
    LIDP 0xC200
    STD
    LIDP 0x2379
    LDPC
    LIB 0x6D
    LIDP 0xC201
    STD
    LIDP 0x237A
    LDPC
    LIB 0x6D
    LIDP 0xC202
    STD
    LIDP 0x237B
    LDPC
    LIB 0x6D
    LIDP 0xC203
    STD
    LIDP 0x237C
    LDPC
    LIB 0x6D
    LIDP 0xC204
    STD
    LIDP 0x237D
    LDPC
    LIB 0x6D
    LIDP 0xC205
    STD
    LIDP 0x237E
    LDPC
    LIB 0x6D
    LIDP 0xC206
    STD
    LIDP 0x237F
    LDPC
    LIB 0x6D
    LIDP 0xC207
    STD
    LIDP 0x2380
    LDPC
    LIB 0x6D
    LIDP 0xC208
    STD
    LIDP 0x2381
    LDPC
    LIB 0x6D
    LIDP 0xC209
    STD
    LIDP 0x2382
    LDPC
    LIB 0x6D
    LIDP 0xC20A
    STD
    LIDP 0x2383
    LDPC
    LIB 0x6D
    LIDP 0xC20B
    STD
    LIDP 0x2384
    LDPC
    LIB 0x6D
    LIDP 0xC20C
    STD
    LIDP 0x2385
    LDPC
    LIB 0x6D
    LIDP 0xC20D
    STD
    LIDP 0x2386
    LDPC
    LIB 0x6D
    LIDP 0xC20E
    STD
    LIDP 0x2387
    LDPC
    LIB 0x6D
    LIDP 0xC20F
    STD
    LIDP 0x2388
    LDPC
    LIB 0x6D
    LIDP 0xC210
    STD
    LIDP 0x2389
    LDPC
    LIB 0x6D
    LIDP 0xC211
    STD
    LIDP 0x238A
    LDPC
    LIB 0x6D
    LIDP 0xC212
    STD
    LIDP 0x238B
    LDPC
    LIB 0x6D
    LIDP 0xC213
    STD
    LIDP 0x238C
    LDPC
    LIB 0x6D
    LIDP 0xC214
    STD
    LIDP 0x238D
    LDPC
    LIB 0x6D
    LIDP 0xC215
    STD
    LIDP 0x238E
    LDPC
    LIB 0x6D
    LIDP 0xC216
    STD
    LIDP 0x238F
    LDPC
    LIB 0x6D
    LIDP 0xC217
    STD
    LIDP 0x2390
    LDPC
    LIB 0x6D
    LIDP 0xC218
    STD
    LIDP 0x2391
    LDPC
    LIB 0x6D
    LIDP 0xC219
    STD
    LIDP 0x2392
    LDPC
    LIB 0x6D
    LIDP 0xC21A
    STD
    LIDP 0x2393
    LDPC
    LIB 0x6D
    LIDP 0xC21B
    STD
    LIDP 0x2394
    LDPC
    LIB 0x6D
    LIDP 0xC21C
    STD
    LIDP 0x2395
    LDPC
    LIB 0x6D
    LIDP 0xC21D
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
