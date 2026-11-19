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

## Keep fixes maintainable

- Before proposing either new source or a patch, check the innermost repeated work. Use a shared transfer loop for columns and reuse it across blocks and bands. Represent destinations with a base address and stride or a small table. Avoid expanding the same instruction sequence for every column or frame. If an explicit expansion is needed for a measured timing constraint, explain the constraint and keep the expansion local.
- If two bands appear out of alignment, inspect the source offset and destination column for each band on the same frame. Use one frame position for both bands, initialize each source pointer from that position, and advance the position once after both transfers. Keep the shared transfer loop when fixing the alignment.
- Give each stored bitmap or padded strip its own end label. Check the first and last byte read at both ends of the trajectory against that label, including any pre-increment and both bytes of an interleaved column. Reserve blank padding in the same data region. Check a frame that crosses a block boundary as well as the first and last frames.

## Check before proposing a Build

1. **Syntax and control flow.** Check BASIC syntax, assembler operands and branches, loop counts, termination, and the return path to BASIC.
2. **Placement and bounds.** Check the occupied address ranges for BASIC, code and data, and workspace. Check every path that writes to RAM or VRAM, including sprite edges and the last iteration.
3. **Timing and sound.** Compute frame intervals and note frequency and duration from the machine clock and the cycles for the whole loop, including branch overhead and its final iteration. Include the display or audio setup and final hold where they affect the result.
4. **State restoration.** Check the stack balance and preserve the registers and I/O state the caller needs. Check that sound stops or that the previous sound state is restored, as appropriate.
5. **Proposal and evidence.** Prepare related BASIC and Assembly together, then propose creation or a consolidated source change using current revisions. Keep source review, Build diagnostics, and observed execution results distinct. After a failed Build, use the completed operation’s buildDiagnostics to identify the causes. Fix related findings in one patch per source before proposing another Build. If the diagnostics are missing or truncated, consult the latest development diagnostics or Studio while the result still refers to that Build.

Use Studio's approval flow for changes, Build, Load, and execution. When reporting results, state which checks ran and which still need an execution test.

## PC-1245 single-line LCD and audio checks

- For graphics, batch `pgp.program-generation`, `pc1245.display`, `pc1245.memory`, and `pc1245.example.hello-scroll`. Use the 80×7 pixel area and check columns 59 and 60: the first range is ascending and the last 20 columns are descending. Use shared routines and preserve LCD symbol addresses. For an animation, verify the frame at the boundary and the final return.
- For a scale, batch `pgp.program-generation`, `sc61860.buzzer`, `pc1245.memory`, and `pc1245.example.buzzer-scale`. Use the 192 kHz clock, a table of notes, and one shared tone routine. Recalculate a whole period if the pulse loop changes, including the branch and loop overhead.
- The sample projects use `CALL &C500` and keep their code and data below 0xC600. Verify placement against the actual BASIC and RAM configuration; the sample range is not a general reservation.

## PC-1261 two-line LCD checks

- Batch `pgp.program-generation`, `pc1261.display`, `pc1261.memory`, and `pc1261.example.lcd-strip`. Use the 120×14 display and the machine's two 60-column blocks per band. At x=60 reset the destination pointer to the second block. Bit 0 is the top of each seven-pixel band.
- Use one frame position and an identical drawing width for both bands. Check the visible sprite and the erase path at x=59/60. The sample uses `CALL &6000` with a short BASIC source. Check RAM placement against the active configuration and calculate timing at 256 kHz.

## PC-1350 full-width LCD checks

- Turn the request into acceptance checks before proposing source. For the rocket exercise, draw both the title and the rocket in machine code, use two vertical bands for the rocket, and use the full horizontal range for its trajectory. BASIC supplies the CALL and return path.
- Verify each destination from the machine's five 30-column blocks per band. For x in 0..149, use the band's block-zero address + 0x200 × floor(x/30) + (x modulo 30). For a pre-increment IYS, initialize Y to the first write address minus one and reset it when crossing into a new block. Check the first and last byte of each transfer. For example x=65 maps to 0x7445 in band 1 and 0x7423 in band 2.
- Check the first and last sprite positions against all 150 columns. For a visible W-column sprite moving right across the full display without clipping, its left edge goes from 0 to 150-W. Check the corresponding range in reverse for leftward movement. Verify the sprite's footprint and erase path at every block boundary; a 60-column drawing area cannot traverse the full screen.
- For a two-band sprite, a practical layout is one source strip for each band and one 30-column copy routine. Select the same 150-column window offset in both strips. For each band, call the routine for its five blocks, using that band's destination base. Advance the window once after both bands are drawn. For an offset measured in bytes, check `maxOffset + 150 <= end - start` for each strip. If the source interleaves two bytes per column, convert the column offset and window width to bytes before checking.
- A bounded execution stop is an observation point. Inspect completed input, the stopped PC and LCD before proposing changes. BASIC may consume the rest of the budget after RTN. If the LCD may have been repainted, capture it at a breakpoint before RTN. Rebuild only when there is a diagnosed source defect to fix. A minimal single frame can isolate address mapping from animation timing.

## PC-1360 and PC-1360K full-width LCD checks

- Batch `pgp.program-generation`, the selected model's display and memory entries, and its full-width LCD example entry (`pc1360.example.lcd-traverse` or `pc1360k.example.lcd-traverse`). Use the model-specific manifest returned by `pgp_get_example`.
- Both models use 150×32 dots and five 30-column blocks per band. The first block addresses are 0x2800, 0x2840, 0x281E, and 0x285E from top to bottom. Each subsequent block adds 0x200. Check the writes against 0x303C, the symbol address.
- Use one full-width band transfer routine and the same frame position for both sprite bands. The example assembles at 0xC000 and uses `CALL &C000`. Verify placement with the current RAM configuration and the selected ROM. Calculate timing at 256 kHz.
