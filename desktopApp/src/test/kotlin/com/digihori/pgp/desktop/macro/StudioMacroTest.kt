package com.digihori.pgp.desktop.macro

import com.digihori.pgp.core.api.*
import kotlinx.coroutines.*
import java.io.File
import kotlin.test.*

class StudioMacroTest {
    @Test fun parsesAllCommandsCommentsEscapesAndOriginalLineNumbers() {
        val lines = StudioMacroParser.parse("""
            # comment
            MoDe pro
            cmd "CALL &C000"
            load "program files/日本語.dmp"
            wait 2S
            key break
            pause "確認してください"
        """.trimIndent())
        assertEquals(6, lines.size)
        assertEquals(2, lines.first().number)
        assertEquals(MacroCommand.Mode(OperatingMode.PROGRAM), lines.first().command)
        assertEquals(MacroCommand.Wait(2000), lines[3].command)
        assertEquals(MacroCommand.Key(PocketKey.BREAK), lines[4].command)
        assertEquals(MacroCommand.Load("program files/日本語.dmp"), lines[2].command)
        assertEquals(MacroCommand.Cmd("PRINT \"A\""), StudioMacroParser.parse("cmd \"PRINT \\\"A\\\"\"").single().command)
    }

    @Test fun rejectsMalformedProgramsBeforeReturningAnyCommands() {
        for (bad in listOf("unknown x", "cmd ”RUN”", "cmd \"RUN\" junk", "cmd \"\"", "wait -1s",
            "wait 9223372036854775807s", "key NONEXISTENT", "mode INVALID", "load \"\"", "pause \"bad\\n\"")) {
            val error = assertFailsWith<MacroSyntaxException> { StudioMacroParser.parse("key ENTER\n$bad") }
            assertEquals(2, error.line)
        }
    }

    @Test fun commandWaitsOneSecondThenPauseRequiresExplicitResume() = runBlocking {
        withTimeout(4000) {
            val host = FakeHost()
            val executor = StudioMacroExecutor()
            val paused = CompletableDeferred<Unit>()
            val started = System.nanoTime()
            val job = launch {
                executor.execute(StudioMacroParser.parse("cmd \"RUN\"\npause \"confirm\"\nload \"two.bas\""),
                    File("macro-root"), host, {}, { if (it != null) paused.complete(Unit) })
            }
            paused.await()
            assertTrue((System.nanoTime() - started) / 1_000_000 >= 950)
            assertEquals(listOf("cmd:RUN"), host.events)
            executor.resume()
            job.join()
            assertEquals(listOf("cmd:RUN", "load:macro-root/two.bas", "cleanup"), host.events)
        }
    }

    @Test fun romErrorDuringWaitOrPauseAbortsBeforeNextLoad() = runBlocking {
        for (waiting in listOf("wait 10s", "pause \"confirm\"")) {
            val host = FakeHost()
            val entered = CompletableDeferred<Unit>()
            val signal = launch { entered.await(); delay(30); host.error = true }
            assertFailsWith<IllegalStateException> {
                withTimeout(1000) {
                    StudioMacroExecutor().execute(StudioMacroParser.parse("$waiting\nload \"next.bas\""),
                        File("root"), host, { entered.complete(Unit) }, {})
                }
            }
            signal.join()
            assertEquals(listOf("cleanup"), host.events)
        }
    }

    @Test fun cancelDuringCommandWaitOrPauseReleasesInputWithoutRunningNextCommand() = runBlocking {
        for (command in listOf("cmd \"RUN\"", "wait 10s", "pause \"confirm\"")) {
            val host = FakeHost()
            val entered = CompletableDeferred<Unit>()
            val job = launch {
                StudioMacroExecutor().execute(StudioMacroParser.parse("$command\nload \"next.bas\""),
                    File("root"), host, { entered.complete(Unit) }, {})
            }
            entered.await()
            delay(20)
            job.cancelAndJoin()
            assertFalse(host.events.any { it.startsWith("load:") })
            assertEquals("cleanup", host.events.last())
        }
    }

    @Test fun studioFailureStopsFollowingCommandsAndCleansUp() = runBlocking {
        val host = FakeHost().apply { failLoad = true }
        assertFailsWith<IllegalArgumentException> {
            StudioMacroExecutor().execute(StudioMacroParser.parse("load \"bad.bas\"\ncmd \"RUN\""), File("root"), host, {}, {})
        }
        assertEquals(listOf("load:root/bad.bas", "cleanup"), host.events)
    }

    private class FakeHost : MacroHost {
        val events = mutableListOf<String>()
        var error = false
        var failLoad = false
        override fun checkError() { check(!error) { "ROM error" } }
        override suspend fun load(file: File) { events += "load:${file.invariantSeparatorsPath}"; require(!failLoad) }
        override suspend fun mode(value: OperatingMode) { events += "mode:$value" }
        override suspend fun command(value: String) { events += "cmd:$value" }
        override suspend fun key(value: PocketKey) { events += "key:$value" }
        override fun cancelInput() { events += "cleanup" }
    }
}
