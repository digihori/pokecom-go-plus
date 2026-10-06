package com.digihori.pgp.core.project

import com.digihori.pgp.core.rom.MachineId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ProjectManifestReaderTest {
    @Test
    fun allowsProjectWithNoSourcesWhileFilesAreBeingReorganized() {
        val project = ProjectDefinition(
            name = "Empty project",
            machineId = MachineId("pc-1245"),
            sources = emptyList(),
        )

        val decoded = assertIs<ProjectManifestReadResult.Success>(
            ProjectManifestReader.read(ProjectManifestWriter.write(project)),
        )

        assertTrue(decoded.project.sources.isEmpty())
    }

    @Test
    fun writesCanonicalManifestThatCanBeReadAgain() {
        val project = ProjectDefinition(
            name = "Mixed project",
            machineId = MachineId("pc-1245"),
            sources = listOf(
                ProjectSource("main", ProjectSourceType.BASIC, "src/main.bas"),
                ProjectSource("data", ProjectSourceType.RAW_BINARY, "src/data.bin", 0xc000),
            ),
        )

        val encoded = ProjectManifestWriter.write(project)
        val decoded = assertIs<ProjectManifestReadResult.Success>(ProjectManifestReader.read(encoded))

        assertEquals(project, decoded.project)
        assertTrue("\"loadAddress\": \"0xC000\"" in encoded)
        assertTrue(encoded.endsWith("\n"))
    }

    @Test
    fun readsMultipleBasicAndMachineSources() {
        val result = assertIs<ProjectManifestReadResult.Success>(
            ProjectManifestReader.read(
                """
                {
                  "format": "pgp-project",
                  "formatVersion": 1,
                  "name": "Mogura Game",
                  "machineId": "pc-1245",
                  "sources": [
                    { "id": "main", "type": "basic", "path": "src/main.bas" },
                    { "id": "sound", "type": "memory-dump", "path": "src/sound.dmp" },
                    {
                      "id": "characters",
                      "type": "raw-binary",
                      "path": "src/characters.bin",
                      "loadAddress": "0xC000"
                    },
                    { "id": "routine", "type": "assembly", "path": "src/routine.asm" }
                  ]
                }
                """.trimIndent(),
            ),
        )

        assertEquals("Mogura Game", result.project.name)
        assertEquals("pc-1245", result.project.machineId.value)
        assertEquals(4, result.project.sources.size)
        assertEquals(0xc000, result.project.sources[2].loadAddress)
    }

    @Test
    fun rejectsUnsafePathsDuplicateIdsAndMissingBinaryAddress() {
        val failure = assertIs<ProjectManifestReadResult.Failure>(
            ProjectManifestReader.read(
                """
                {
                  "format": "pgp-project",
                  "formatVersion": 1,
                  "name": "Broken",
                  "machineId": "pc-1245",
                  "sources": [
                    { "id": "main", "type": "basic", "path": "../outside.bas" },
                    { "id": "main", "type": "raw-binary", "path": "src/main.bin" }
                  ]
                }
                """.trimIndent(),
            ),
        )

        assertTrue(failure.errors.any { it is ProjectManifestError.InvalidSourcePath })
        assertTrue(failure.errors.any { it is ProjectManifestError.DuplicateSourceId })
        assertTrue(failure.errors.any { it is ProjectManifestError.MissingLoadAddress })
    }

    @Test
    fun rejectsUnsupportedMachineAndUnknownJsonFields() {
        val unsupportedMachine = assertIs<ProjectManifestReadResult.Failure>(
            ProjectManifestReader.read(
                """
                {
                  "format": "pgp-project",
                  "formatVersion": 1,
                  "name": "Future machine",
                  "machineId": "pc-9999",
                  "sources": [{ "id": "main", "type": "basic", "path": "main.bas" }]
                }
                """.trimIndent(),
            ),
        )
        assertTrue(unsupportedMachine.errors.any { it is ProjectManifestError.UnsupportedMachine })

        val unknownField = assertIs<ProjectManifestReadResult.Failure>(
            ProjectManifestReader.read(
                """
                {
                  "format": "pgp-project",
                  "formatVersion": 1,
                  "name": "Typo",
                  "machineId": "pc-1245",
                  "source": []
                }
                """.trimIndent(),
            ),
        )
        assertTrue(unknownField.errors.any { it is ProjectManifestError.InvalidJson })
    }
}
