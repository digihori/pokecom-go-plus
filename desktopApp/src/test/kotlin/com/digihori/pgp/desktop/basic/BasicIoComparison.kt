package com.digihori.pgp.desktop.basic

import com.digihori.pgp.core.rom.MachineId
import java.io.File

/** Compare saved BASIC without requiring identical source formatting. Read-only. */
fun main(args: Array<String>) {
    require(args.size == 3) { "Use <machine-id> <expected.bas> <saved.bas>" }
    val id = MachineId(args[0])
    fun compile(path: String): ByteArray {
        val result = DesktopBasicLoader.compileProgram(File(path).readBytes(), id)
        check(result is DesktopBasicProgramCompileResult.Success) { "$path: $result" }
        return result.bytes
    }
    val expected = compile(args[1])
    val actual = compile(args[2])
    check(expected.contentEquals(actual)) { "FAIL: BASIC program bytes differ (${expected.size} expected, ${actual.size} actual)" }
    println("PASS: ${id.value}, ${actual.size} BASIC program bytes match")
}
