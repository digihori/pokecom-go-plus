package com.digihori.pgp.desktop.project

import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.*
import kotlin.test.*

class DesktopExternalAssemblerTest {
    @Test fun preservesSourcePathsAndArgumentBoundaries() {
        val source = File("/tmp/日本語 space/test.asm")
        assertEquals(listOf("/tmp/my assembler", "--input", source.absolutePath, "two words"),
            DesktopAssemblerSettings(true, "/tmp/my assembler", "--input {source} 'two words'").command(source))
        assertEquals(listOf("sc61", source.absolutePath), DesktopAssemblerSettings(true, "sc61", "").command(source))
        assertFailsWith<IllegalArgumentException> { DesktopAssemblerSettings(true, "sc61", "'unfinished").command(source) }
    }

    private val fixtureClasspath = listOf(AssemblerProcessFixture::class.java, Unit::class.java)
        .joinToString(File.pathSeparator) { File(it.protectionDomain.codeSource.location.toURI()).absolutePath }

    private fun settings(mode: String, seconds: Int = 5) = DesktopAssemblerSettings(
        true, File(System.getProperty("java.home"), "bin/java").absolutePath,
        "-Dstdout.encoding=windows-1252 -Dstderr.encoding=windows-1252 " +
            "-cp \"$fixtureClasspath\" ${AssemblerProcessFixture::class.java.name} $mode {source}", seconds,
    )

    @Test fun capturesBothStreamsAndPreservesNonzeroExitCode() = runBlocking {
        val directory = Files.createTempDirectory("pgp assembler 日本語").toFile()
        try {
            val source = File(directory, "test.asm").apply { writeText("ORG $4000") }
            val configured = if (System.getProperty("os.name").startsWith("Windows", true)) {
                // The Windows Java launcher can convert Unicode argv using the system ANSI code page.
                // PowerShell receives native Unicode arguments, matching ProcessBuilder's Windows contract.
                val script = File(directory, "fixture.ps1").apply { writeText("""
                    [Console]::OutputEncoding = New-Object System.Text.UTF8Encoding
                    [Console]::Out.WriteLine("stdout " + ${'$'}args[0])
                    [Console]::Error.WriteLine("stderr")
                    [IO.File]::WriteAllText([IO.Path]::Combine([Environment]::CurrentDirectory, "cwd marker.txt"), "ok")
                    exit 2
                """.trimIndent()) }
                DesktopAssemblerSettings(true, "powershell.exe", "-NoProfile -NonInteractive -File \"${script.absolutePath}\" {source}", 15)
            } else settings("exit")
            val result = assertIs<DesktopExternalAssemblerRunResult.Exited>(DesktopExternalAssemblerRunner.run(configured, source), "Fixture process did not exit")
            assertEquals(2, result.code, "Fixture exit code; log=${result.log}")
            assertTrue(result.log.contains("stdout"), "Missing stdout; log=${result.log}")
            assertTrue(result.log.contains("stderr"), "Missing stderr; log=${result.log}")
            assertTrue(result.log.contains(source.absolutePath), "Source argument was changed; expected=${source.absolutePath}; log=${result.log}")
            assertEquals("ok", File(directory, "cwd marker.txt").readText(), "Working directory was changed")
        } finally { directory.deleteRecursively() }
    }

    @Test fun reportsLaunchFailure() = runBlocking {
        val source = Files.createTempFile("pgp-asm-", ".asm").toFile()
        try {
            assertIs<DesktopExternalAssemblerRunResult.Failed>(DesktopExternalAssemblerRunner.run(
                DesktopAssemblerSettings(true, "/nonexistent/pgp-assembler"), source))
            Unit
        } finally { source.delete() }
    }

    @Test fun timesOutSleepingProcess() = runBlocking {
        val source = Files.createTempFile("pgp-asm-", ".asm").toFile()
        try {
            assertIs<DesktopExternalAssemblerRunResult.TimedOut>(DesktopExternalAssemblerRunner.run(settings("sleep", 1), source))
            Unit
        } finally { source.delete(); File(source.path + ".started").delete() }
    }

    @Test fun cancellationStopsProcessBeforeLaterWrite() = runBlocking {
        val source = Files.createTempFile("pgp-asm-", ".asm").toFile()
        val started = File(source.path + ".started")
        val completed = File(source.path + ".completed")
        try {
            val job = launch { DesktopExternalAssemblerRunner.run(settings("sleep"), source) }
            withTimeout(5000) { while (!started.exists()) delay(20) }
            val pid = withTimeout(5000) {
                var value = started.readText().toLongOrNull()
                while (value == null) { delay(20); value = started.readText().toLongOrNull() }
                value
            }
            job.cancelAndJoin()
            assertTrue(job.isCancelled)
            assertFalse(ProcessHandle.of(pid).map { it.isAlive }.orElse(false))
            assertFalse(completed.exists())
        } finally { source.delete(); started.delete(); completed.delete() }
    }
}

object AssemblerProcessFixture {
    @JvmStatic fun main(args: Array<String>) {
        // The runner's test protocol is UTF-8, independent of the host console code page.
        System.out.write("stdout ${args[1]} ${File(".").canonicalPath}\n".toByteArray(Charsets.UTF_8))
        System.out.flush()
        System.err.write("stderr\n".toByteArray(Charsets.UTF_8))
        System.err.flush()
        if (args[0] == "exit") File("cwd marker.txt").writeText("ok")
        if (args[0] == "sleep") {
            File(args[1] + ".started").writeText(ProcessHandle.current().pid().toString())
            Thread.sleep(20_000)
            File(args[1] + ".completed").writeText("completed")
        }
        kotlin.system.exitProcess(2)
    }
}
