# Debugger manual acceptance fixture; no firmware included.
# Use a disposable PC-1245 session. Build & Load does not start execution.
# RUN mode: CALL &C000 for the loop, CALL &C040 for CASE.
# Both paths loop until Pause/Reset; they do not return to BASIC.
ORG 0xC000
START: LIDP 0xC100
       LIA 0x41
       STD
       CALL SUB
AFTER_CALL: LIA 9
       JP START

ORG 0xC020
SUB:   LIA 0x42
       STD
       RTN

ORG 0xC040
CASE_START: LIA 0x10
CASE_ENTRY: CASE1 0x02C080
       CASE2
       DB 0x10
       DW FIRST
       DB 0x20
       DW SECOND
       DW DEFAULT

ORG 0xC060
FIRST: LIA 1
       RTN
ORG 0xC070
SECOND: LIA 2
       RTN
ORG 0xC078
DEFAULT: LIA 3
       RTN
ORG 0xC080
CASE_AFTER: LIA 9
       JP CASE_AFTER

ORG 0xC100
DATA: DB 0xDE, 0xAD, 0xBE, 0xEF, 0x34, 0x12, 0x12, 0x34
