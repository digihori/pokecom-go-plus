# Studio macro manual checks

Use a supported machine with your own ROM: PC-1260/1261, PC-1401/1402,
PC-1450, PC-1460 or PC-1470U. No ROM is included here.

1. Select `basic-switch.pgmacro` from the Macro menu. BASIC 1 should load and run.
2. At pause, confirm the LCD shows BASIC 1 has finished. Resume from the menu or console.
3. BASIC 2 should replace BASIC 1. Type RUN manually to check its contents.
4. Repeat and stop while paused. BASIC 2 must not load.
5. In a temporary copy, use `cmd "1/0"` followed by `load "basic2.bas"`.
   A ROM error must interrupt the macro and preserve the error display.
6. Try a missing .bas path, malformed syntax and an unsupported character.
   Confirm the console identifies the file and line; subsequent commands must not run.
7. Hide the console during a long wait. Use the Macro menu to stop it.
   Errors should reveal the console again. Hiding and clearing logs must not stop execution.

Every successful instruction waits one second before proceeding, including the last instruction.
For cmd this starts after the complete input sequence, including ENTER; for mode, load and key,
it starts after the operation completes. A wait adds this second to its requested duration;
pause waits one second after Resume.
It does not prove program completion. Stop Macro does not stop running machine code;
use the emulator Pause or Reset for that. Avoid running the samples over unsaved programs.

The macro file and its referenced BASIC files must stay together for relative paths to work.
When importing into a Project, add all three files. Import renaming does not rewrite paths.
