package com.digihori.pgp.desktop.basic

import com.digihori.pgp.core.rom.MachineId
import java.io.File
import kotlin.test.*

class BasicIoFixturesTest {
    private val root = sequenceOf(File("test-data/basic/io"), File("../test-data/basic/io"))
        .first { it.isDirectory }

    private fun compile(bytes: ByteArray, id: MachineId): ByteArray {
        val result = DesktopBasicLoader.compileProgram(bytes, id)
        return assertIs<DesktopBasicProgramCompileResult.Success>(result, "${id.value}: $result").bytes
    }

    @Test fun fixturesSurviveSaveReloadAndTextFileVariantsOnEveryModel() {
        val groups = mapOf(
            "1245" to listOf("1245", "1250", "1251", "1255"),
            "1401" to listOf("1260", "1261", "1350", "1401", "1402", "1450", "1460"),
            "1360k" to listOf("1360", "1360k", "1470u"),
        )
        for ((fixture, models) in groups) for (model in models) {
            val id = MachineId("pc-$model")
            val images = listOf("full", "edited", "short").map { stage ->
                val source = File(root, "pc$fixture/$stage.bas").readText()
                val expected = compile(source.encodeToByteArray(), id)
                for (newline in listOf("\n", "\r\n", "\r")) {
                    val variant = source.replace("\n", newline)
                    for (bom in listOf("", "\uFEFF")) {
                        assertContentEquals(expected, compile((bom + variant).encodeToByteArray(), id), "$model/$stage")
                    }
                }
                val saved = assertIs<DesktopBasicProgramDecodeResult.Success>(
                    DesktopBasicLoader.detokenizeProgram(expected, id),
                ).utf8Bytes
                assertContentEquals(expected, compile(saved, id), "$model/$stage save/reload")
                expected
            }
            assertFalse(images[0].contentEquals(images[1]), "$model edit must change program")
            assertTrue(images[2].size < images[0].size, "$model short replacement")
        }
    }
}
