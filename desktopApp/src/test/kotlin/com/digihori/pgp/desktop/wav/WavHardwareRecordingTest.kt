package com.digihori.pgp.desktop.wav

import java.io.File
import java.nio.file.Files
import org.junit.Assume.assumeTrue
import kotlin.test.Test

/** Optional local recordings; private audio is never copied into fixtures or committed. */
class WavHardwareRecordingTest {
    @Test fun recordedTransfersMatchAllExpectedBodyBytes() {
        val directory = System.getenv("PGP_WAV_RECORDING_DIR")?.let(::File)
        assumeTrue("Set PGP_WAV_RECORDING_DIR for local hardware audio", directory?.isDirectory == true)
        val root = Files.createTempDirectory("pgp-recorded-wav-").toFile()
        try {
            main(arrayOf("generate", root.absolutePath))
            val checked = mutableSetOf<String>()
            for ((family, prefix) in listOf("old" to "old-basic", "s1" to "s1-basic", "s2" to "s2-basic")) {
                for (rate in listOf("16k", "44k", "48k")) {
                    val recording = listOf(File(directory, "$prefix-$rate.wav"), File(directory, "${prefix.replace("basic", "basec")}-$rate.wav"))
                        .firstOrNull { it.isFile } ?: continue
                    main(arrayOf("check", File(root, "$family-basic").absolutePath, recording.absolutePath))
                    checked += "$family-$rate"
                }
            }
            check(checked.containsAll(listOf("s1-44k", "s1-48k", "s2-44k", "s2-48k"))) { "Expected at least S1/S2 44/48 kHz recordings" }
        } finally { root.deleteRecursively() }
    }
    @Test fun recordedModernBinaryMatchesExpectedDataAndAddress() {
        val directory = System.getenv("PGP_WAV_RECORDING_DIR")?.let(::File)
        val recording = File(directory, "s2-binary-48k.wav")
        assumeTrue("Set PGP_WAV_RECORDING_DIR with S2 Binary audio", directory?.isDirectory == true && recording.isFile)
        val root = Files.createTempDirectory("pgp-recorded-binary-").toFile()
        try {
            main(arrayOf("generate", root.absolutePath))
            for (family in listOf("s1", "s2")) for (rate in listOf("44k", "48k")) {
                val binaryRecording = File(directory, "$family-binary-$rate.wav")
                if (binaryRecording.isFile) main(arrayOf("check", File(root, "$family-binary").absolutePath, binaryRecording.absolutePath))
            }
        } finally { root.deleteRecursively() }
    }

}
