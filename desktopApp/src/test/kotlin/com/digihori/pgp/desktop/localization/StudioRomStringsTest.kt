package com.digihori.pgp.desktop.localization

import com.digihori.pgp.core.api.CreateSessionError
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportError
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.core.rom.RomComponentId
import com.digihori.pgp.core.rom.RomPackageValidationError
import com.digihori.pgp.desktop.rom.*
import java.nio.file.Files
import kotlin.test.*

class StudioRomStringsTest {
    @Test fun typedErrorsKeepIdsPathsHashesAndNumbers() {
        val path = "日本語/folder with spaces/internal.bin"
        val validations = listOf(
            RomPackageValidationError.UnsupportedFormat("pgp-rom-set"),
            RomPackageValidationError.UnsupportedVersion(99),
            RomPackageValidationError.InvalidMachineId("pc-unknown"),
            RomPackageValidationError.EmptyComponents,
            RomPackageValidationError.InvalidComponentId("bad-id"),
            RomPackageValidationError.InvalidRole("internal", "bad-role"),
            RomPackageValidationError.InvalidPath("internal", path),
            RomPackageValidationError.DuplicateComponentId("internal"),
            RomPackageValidationError.DuplicateComponentPath(path),
            RomPackageValidationError.MissingComponentFile(path),
            RomPackageValidationError.InvalidDeclaredSize("internal", -1),
            RomPackageValidationError.SizeMismatch("internal", 8192, 10),
            RomPackageValidationError.InvalidSha256("internal", "bad-hash"),
            RomPackageValidationError.Sha256Mismatch("internal", "expected-hash", "actual-hash"),
        )
        for (language in StudioLanguage.entries) {
            val strings = StudioStrings(language)
            val result = strings.romPackageError(DesktopRomPackageError.Validation(validations))
            for (value in listOf(path, "pc-unknown", "8192", "10", "expected-hash", "actual-hash")) {
                assertTrue(result.contains(value), result)
            }
            val sizeError = strings.romImportError(RomImportError.InvalidImageSize(listOf(32768, 65536), 1))
            assertTrue(sizeError.contains(if (language == StudioLanguage.ENGLISH) "32768 or 65536" else "32768 または 65536"), sizeError)
            assertTrue(strings.romImportError(RomImportError.InvalidComponentSize(RomComponentId("internal"), 8192, 1)).contains("internal"))
            assertTrue(strings.romSessionError(CreateSessionError.MachineIdMismatch(MachineId("pc-1261"), MachineId("pc-1401"))).contains("pc-1261"))
            for (error in listOf(
                DesktopRomPackageError.PackageTooLarge(20, 30), DesktopRomPackageError.TooManyEntries(2),
                DesktopRomPackageError.EntryTooLarge(path, 8192), DesktopRomPackageError.UnsafePath(path),
                DesktopRomPackageError.DuplicateEntry(path), DesktopRomPackageError.MissingManifest,
                DesktopRomPackageError.InvalidZip("original ZIP diagnostic"),
                DesktopRomPackageError.InvalidManifest("original JSON diagnostic"),
            )) assertTrue(strings.romLoadError(DesktopRomLoadError.InvalidPackage(error)).isNotBlank())
            assertTrue(strings.romPackageError(DesktopRomPackageError.InvalidZip("original ZIP diagnostic")).endsWith("original ZIP diagnostic"))
        }
    }

    @Test fun familyLabelsDoNotChangeRegistrationOrStorageIdentity() {
        val ids = DesktopRomRegistrationCatalog.definitions.map { it.id }
        val root = Files.createTempDirectory("pgp-localization-family").toFile()
        try {
            val library = DesktopRomLibrary(root)
            for (language in StudioLanguage.entries) {
                val strings = StudioStrings(language)
                val label = DesktopRomRegistrationCatalog.label(MachineId("pc-1260"), strings)
                assertTrue(label.contains("PC-1261"))
                assertEquals(library.packageFile(MachineId("pc-1260")), library.packageFile(MachineId("pc-1261")))
                assertEquals(library.packageFile(MachineId("pc-1401")), library.packageFile(MachineId("pc-1402")))
                assertEquals(ids, DesktopRomRegistrationCatalog.definitions.map { it.id })
            }
        } finally { root.deleteRecursively() }
    }

    @Test fun creatorValidationIsTypedAndDoesNotOverwriteInputs() {
        val root = Files.createTempDirectory("pgp-localization-rom-set").toFile()
        try {
            val internal = java.io.File(root, "internal.bin").apply { writeBytes(ByteArray(8192)) }
            val external = java.io.File(root, "external.bin").apply { writeBytes(ByteArray(16384)) }
            val bad = java.io.File(root, "bad.bin").apply { writeBytes(byteArrayOf(0, 0, 0)) }
            val issue = assertIs<RomSetInputIssue.SizeMismatch>(DesktopRomSetCreator.inputIssue(bad, 8192))
            assertEquals(8192L, issue.expectedSize)
            assertEquals("Expected 8 KiB; selected 3 bytes.", DesktopRomSetCreator.inputError(bad, 8192))
            val failure = assertFailsWith<RomSetInputException> {
                DesktopRomSetCreator.create(MachineId("pc-1245"), bad, external, java.io.File(root, "out.pgrom"))
            }
            for (language in StudioLanguage.entries) {
                val strings = StudioStrings(language)
                assertTrue(strings.romSetCreationError(failure).contains("3"))
                val sameInput = assertFailsWith<IllegalArgumentException> {
                    DesktopRomSetCreator.create(MachineId("pc-1245"), internal, external, internal, true)
                }
                assertTrue(strings.romSetCreationError(sameInput).isNotBlank())
            }
            assertEquals(8192L, internal.length())
            assertFalse(java.io.File(root, "out.pgrom").exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun libraryRetainsTypedValidationAndOriginalIoDetails() {
        val root = Files.createTempDirectory("pgp-localization-library").toFile()
        try {
            val result = assertIs<DesktopRomLibraryResult.Failure>(DesktopRomLibrary(root).install(byteArrayOf(1, 2, 3), "bad.pgrom"))
            val error = assertIs<DesktopRomLoadError.InvalidPackage>(result.error)
            assertTrue(StudioStrings(StudioLanguage.JAPANESE).romLoadError(error).contains("パッケージ"))
            for (language in StudioLanguage.entries) {
                assertTrue(StudioStrings(language).romSetCreationError(java.io.IOException("original OS diagnostic")).endsWith("original OS diagnostic"))
            }
            assertTrue(root.listFiles().orEmpty().isEmpty())
        } finally { root.deleteRecursively() }
    }
    @Test fun packageContentsStayIdenticalWhileUiLanguageChanges() {
        val root = Files.createTempDirectory("pgp-localization-package").toFile()
        try {
            val internal = java.io.File(root, "internal.bin").apply { writeBytes(ByteArray(8192)) }
            val external = java.io.File(root, "external.bin").apply { writeBytes(ByteArray(16384)) }
            fun entries(file: java.io.File): Map<String, ByteArray> {
                val result = linkedMapOf<String, ByteArray>()
                java.util.zip.ZipInputStream(file.inputStream()).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        result[entry.name] = zip.readBytes()
                    }
                }
                return result
            }
            val packages = StudioLanguage.entries.map { language ->
                val strings = StudioStrings(language)
                strings.text("romSet.title")
                val file = DesktopRomSetCreator.create(MachineId("pc-1245"), internal, external,
                    java.io.File(root, "${language.name}.pgrom"))
                strings.text("romSet.created", file.absolutePath)
                entries(file)
            }
            assertEquals(packages[0].keys, packages[1].keys)
            for (key in packages[0].keys) assertContentEquals(packages[0][key], packages[1][key], key)
            // ZIP timestamps are deliberately excluded; manifest, hashes and ROM components are compared byte for byte.
            assertTrue(packages[0].getValue("manifest.json").decodeToString().contains("pc-1245"))
        } finally { root.deleteRecursively() }
    }
}
