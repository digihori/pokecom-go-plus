#!/usr/bin/env python3
"""Generate the pure-Core S2 CP932 mapping using Python's standard-library codec.
No external repository, firmware, network, or build-time dependency is needed.
Run with --verify to check the committed mapping without writing files.
"""
from pathlib import Path
import argparse

PREFIX = r'''package com.digihori.pgp.core.emulator.machine.pc1360

/** CP932 two-byte mapping generated from Python's standard-library cp932 codec.
 * Runtime uses only immutable data; no host charset, filesystem, or platform APIs.
 * Lead order 81..9F,E0..FC; trail order 40..7E,80..FC. Zero denotes an unassigned pair.
 */
internal object S2JapaneseText {
    fun encode(character: Char): Int? = when (character.code) {
        0x00a2 -> 0x8191; 0x00a3 -> 0x8192; 0x00ac -> 0x81ca
        0x2016 -> 0x8161; 0x2212 -> 0x817c; 0x301c -> 0x8160
        else -> encoded[character]
    }
    fun decode(lead: Int, trail: Int): Char? {
        val row = when (lead) { in 0x81..0x9f -> lead - 0x81; in 0xe0..0xfc -> lead - 0xe0 + 31; else -> return null }
        val column = when (trail) { in 0x40..0x7e -> trail - 0x40; in 0x80..0xfc -> trail - 0x80 + 63; else -> return null }
        return characters[row * 188 + column].takeUnless { it == '\u0000' }
    }
    private val encoded: Map<Char, Int> by lazy {
        buildMap {
            characters.forEachIndexed { index, character ->
                if (character != '\u0000' && !containsKey(character)) {
                    val row = index / 188
                    val column = index % 188
                    val lead = if (row < 31) row + 0x81 else row - 31 + 0xe0
                    val trail = if (column < 63) column + 0x40 else column - 63 + 0x80
                    put(character, lead * 256 + trail)
                }
            }
        }
    }
    private const val characters: String =
'''

def generate():
    chars = []
    for lead in list(range(0x81, 0xa0)) + list(range(0xe0, 0xfd)):
        for trail in list(range(0x40, 0x7f)) + list(range(0x80, 0xfd)):
            try:
                char = bytes([lead, trail]).decode('cp932')
            except UnicodeDecodeError:
                char = '\0'
            assert len(char) == 1 and ord(char) <= 65535
            chars.append(char)
    result = PREFIX
    for offset in range(0, len(chars), 94):
        escaped = ''.join('\\u%04x' % ord(char) for char in chars[offset:offset + 94])
        result += '        "' + escaped + '"' + (' +\n' if offset + 94 < len(chars) else '\n')
    return result + '}\n'

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--verify', action='store_true')
    options = parser.parse_args()
    target = Path(__file__).resolve().parent.parent / 'core/src/commonMain/kotlin/com/digihori/pgp/core/emulator/machine/pc1360/S2JapaneseText.kt'
    expected = generate()
    if options.verify:
        if target.read_text() != expected:
            raise SystemExit('S2 CP932 mapping differs; regenerate and review the change')
        print('Verified 11280 S2 CP932 pair entries')
    else:
        target.write_text(expected)
        print('Generated', target.name)
