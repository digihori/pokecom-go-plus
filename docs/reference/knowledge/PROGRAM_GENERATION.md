# Program generation and pre-build review

Apply this guidance to a plain-language program request. Use the target machine, requested behavior, and any constraints the user supplied. For routine choices such as loop structure, frame rate when unspecified, and source organization, choose a reasonable default and state assumptions that affect behavior.

## Gather the facts needed for the program

- Identify the BASIC dialect, available RAM, and relevant LCD or sound hardware. Use `pgp_search_knowledge` with `includeContent=true`, and reuse results for the same machine and catalog. Batch several known knowledge IDs with the `ids` argument in one request. Fetch source examples with `pgp_get_example`.
- For PC-1350 animation, `pc1350-lcd-strip` demonstrates a small data-driven sprite, a shared copy routine, and a VRAM block crossing. Use the selected machine's display mapping when adapting the technique. For tones or melodies, use the audio topic and the selected machine's clock.
- Check instruction support and flag or register effects against the available evidence. Search the instruction topic by mnemonic with `includeContent=true` for Core behavior and timing of selected drawing, delay, and sound instructions. Where firmware behavior, an instruction effect, or timing is unknown, report the gap and identify what must be checked.

## Structure the source

- Give routines and data meaningful names. Use loops for repeated operations, tables for sprites and notes, and shared routines for drawing, erasing, waiting, or playing a tone. Keep frame positions, durations, and other tunable values easy to find. State each machine-code routine's inputs, affected state, and return behavior.
- Update the position or table index in a loop. For LCD animation, erase the previous footprint or redraw the affected region. Check screen edges and each VRAM block boundary using the machine's actual address map.
- For BASIC calling machine code, use the target dialect's supported hexadecimal literal where available. Check that the CALL address agrees with the assembler entry point. Studio formats bare decimal CALL operands in supported BASIC source proposals before review.

## Check before proposing a Build

1. **Syntax and control flow.** Check BASIC syntax, assembler operands and branches, loop counts, termination, and the return path to BASIC.
2. **Placement and bounds.** Check the occupied address ranges for BASIC, code and data, and workspace. Check every path that writes to RAM or VRAM, including sprite edges and the last iteration.
3. **Timing and sound.** Compute frame intervals and note frequency and duration from the machine clock and the cycles for the whole loop, including branch overhead and its final iteration. Include the display or audio setup and final hold where they affect the result.
4. **State restoration.** Check the stack balance and preserve the registers and I/O state the caller needs. Check that sound stops or that the previous sound state is restored, as appropriate.
5. **Proposal and evidence.** Prepare related BASIC and Assembly together, then propose creation or a consolidated source change using current revisions. Keep source review, Build diagnostics, and observed execution results distinct. After a failed Build, use the completed operation’s buildDiagnostics to identify the causes. Fix related findings in one patch per source before proposing another Build. If the diagnostics are missing or truncated, consult the latest development diagnostics or Studio while the result still refers to that Build.

Use Studio's approval flow for changes, Build, Load, and execution. When reporting results, state which checks ran and which still need an execution test.

## PC-1350 full-width LCD checks

- Turn the request into acceptance checks before proposing source. For the rocket exercise, draw both the title and the rocket in machine code, use two vertical bands for the rocket, and use the full horizontal range for its trajectory. BASIC supplies the CALL and return path.
- Verify each destination from the machine's five 30-column blocks per band. For x in 0..149, use the band's block-zero address + 0x200 × floor(x/30) + (x modulo 30). For a pre-increment IYS, initialize Y to the first write address minus one and reset it when crossing into a new block. Check the first and last byte of each transfer. For example x=65 maps to 0x7445 in band 1 and 0x7423 in band 2.
- Check the first and last sprite positions against all 150 columns. For a visible W-column sprite moving right across the full display without clipping, its left edge goes from 0 to 150-W. Check the corresponding range in reverse for leftward movement. Verify the sprite's footprint and erase path at every block boundary; a 60-column drawing area cannot traverse the full screen.
- A bounded execution stop is an observation point. Inspect completed input, the stopped PC and LCD before proposing changes. BASIC may consume the rest of the budget after RTN. If the LCD may have been repainted, capture it at a breakpoint before RTN. Rebuild only when there is a diagnosed source defect to fix. A minimal single frame can isolate address mapping from animation timing.
