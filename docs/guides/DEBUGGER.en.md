# Studio Debugger

Updated: 2026-10-30. See the [Japanese reference](DEBUGGER.md) for detailed rules and history,
and the [acceptance record](../development/DEBUGGER_RELEASE_ACCEPTANCE.md) for what has been verified.

Open **Debug → Open Debugger**. Run, Pause, Step, Step Over and Step Out stay at the top.
Wide windows show execution and memory side by side. Narrow windows use Execution and Memory tabs.
Registrations and History are available in both layouts. Scroll each pane to reach additional tools.

Memory View appears first in the memory pane, followed by the RAM editor and search.
Selecting a search result returns to Memory View and releases text input focus.
While an input field has focus, typing is reserved for that field. Switching away from a pane releases its
input ownership. Native Tab/Enter/IME behavior in distributed apps remains part of manual acceptance.

## Breakpoints and monitoring

Use **Registrations** to add, edit, enable, disable or remove breakpoints and watches.
Addresses are hexadecimal. An optional ROM bank is decimal; blank matches any bank.
Bank scope is supported only within the selected machine's banked ROM window.
Use normalized bank numbers, not raw selector register values.

Two registrations at the same address may specify different banks. Duplicate bank scopes, including an
unscoped breakpoint overlapping a scoped one, are rejected even if a registration is disabled.
Each bank has its own hit counter. A different bank does not increment it or evaluate its value condition.

Value conditions use one comparison, such as `A == 0x41` or `MEM[0xC000] != 0`.
Hit conditions support `== 3`, `>= 3` and `% 3`. Values are unsigned; executable expressions are not supported.
Reset and successful Load reset counters while retaining definitions. Enable toggles preserve counters.

Change watches detect changed values after an instruction. Access watches detect CPU reads and writes.
RAM edits and debugger reads are host operations and are not reported as CPU writes or reads.

## Memory search and editing

Pause before searching. Enter inclusive hexadecimal start/end addresses and select a format:

| Format | Input |
| --- | --- |
| Hex bytes | `00 FF 41`, 1–64 bytes separated by whitespace |
| Unsigned 8-bit | 0–255, decimal or `0x` hexadecimal |
| Unsigned 16-bit | 0–65535, decimal or `0x` hexadecimal; choose little or big endian |

Search examines RAM and ROM in the current mapping and bank. It skips LCD, control and unmapped areas,
shows up to 256 matches, and reports when more matches exist. Overlapping matches are included.
Click an address to show it in Memory View. After execution, Reset, Load or editing changes the session,
search again before navigating from old results. Search does not switch banks or execute instructions.

The RAM byte editor accepts a hexadecimal address and exactly two hexadecimal digits, `00`–`FF`.
Choose **Preview edit**, check the logical/physical address and old/new values, then **Apply byte**.
RAM mirrors share a physical byte. ROM, LCD, control and unmapped regions are excluded.
Invalid, stale or rejected edits leave the previous state intact. Editing updates Memory View and watch
baselines for that physical byte, retains breakpoint counters, and invalidates source mapping, observed
call frames and loaded-program indicators. CPU execution stays paused. Undo and bulk editing are not included.

## Logpoints

Logpoints in **Registrations** record one register or `MEM[0xC000]` immediately before an instruction.
An optional condition uses the same single-comparison syntax as breakpoints. Memory operands must refer
to RAM or ROM. A logpoint never stops execution by itself; breakpoints, watches and faults retain priority.

There are at most 32 registrations and 256 retained records. Old records are discarded with a visible count.
Records contain sequence, registration ID, instruction address, value and available physical ROM location.
Reading failures are recorded. Reset and successful Load clear records, but keep definitions.
History clearing does not delete registrations. Logpoint definitions are not included in settings files.
Enabled logpoints require instruction-by-instruction observation and can reduce execution speed.

## Step Over and Step Out

Step Over follows the observed CALL/CAL or CASE1 frame until its matching RTN. Ordinary instructions,
including CASE2 dispatch, execute once. Step Out requires an observed CALL/CAL/CASE1 frame.
Nested calls and CASE paths use distinct frame IDs, even when return addresses are identical.

The debugger checks stack bytes, R, return PC and available physical ROM location.
Stack damage or a different return bank stops stepping as untrackable. Breakpoints, watches, faults,
manual Pause and execution limits can interrupt the operation. Unobserved stack frames are not inferred.
Fast batch execution loses observation continuity. Asynchronous interrupts not modeled by Core are outside this scope.

## Settings and export

Save/restore debugger settings explicitly from **Registrations**. The target machine, ROM, RAM configuration
and project location must match. Settings do not restore execution state.
Files without bank-scoped breakpoints use schema v1; files containing them use v2. Current Studio reads both.
Old Studio versions reject v2 rather than silently dropping its bank scopes.

Use **History → Save history / trace** while paused or faulted. The independent `.pgtrace.json` export contains
retained trace, access/bank history and logpoints, along with session metadata and stop reason.
Choose the newest-entry limit for each section: 0–4096 for trace/access/bank entries, 0–256 for logpoints.
Zero excludes entries in that section. Sections retain counts of entries available and omitted by the limit.
The file limit is 8 MiB. Captured instruction bytes may include ROM-derived fragments; a full ROM/RAM dump is not captured.

Cancellation writes nothing. A session change during destination selection rejects the save.
Saving uses atomic replacement and preserves an existing file on failure. Export does not clear observations,
restore old state, or establish an exact correlation between bank events and instruction trace.
Checkpoint remains a separate format. CSV and state restoration are not included.

For MCP bank arguments, approval and compatibility rules, see [AI integration](../reference/AI_INTEGRATION.md#19-bank指定breakpoint2026-10-30).
