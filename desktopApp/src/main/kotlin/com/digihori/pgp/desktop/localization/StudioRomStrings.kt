package com.digihori.pgp.desktop.localization

import com.digihori.pgp.core.api.CreateSessionError
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportError
import com.digihori.pgp.core.rom.RomPackageValidationError
import com.digihori.pgp.desktop.rom.DesktopRomLoadError
import com.digihori.pgp.desktop.rom.DesktopRomPackageError

internal fun StudioStrings.romImportError(error: RomImportError): String = when (error) {
    is RomImportError.UnsupportedMachine -> text("rom.err.unsupportedMachine", error.machineId.value)
    is RomImportError.InvalidImageSize -> text("rom.err.imageSize", error.actual.toString(), error.expected.joinToString(text("rom.err.or")))
    is RomImportError.InvalidComponentSize -> text("rom.err.componentSize", error.componentId.value, error.actual.toString(), error.expected.toString())
}

internal fun StudioStrings.romSessionError(error: CreateSessionError): String = when (error) {
    is CreateSessionError.UnsupportedMachine -> text("rom.err.sessionMachine", error.machineId.value)
    is CreateSessionError.MachineIdMismatch -> text("rom.err.machineMismatch", error.expected.value, error.actual.value)
    is CreateSessionError.InvalidRomSet -> text("rom.err.invalidSet", error.reason)
}

internal fun StudioStrings.romValidationError(error: RomPackageValidationError): String = when (error) {
    is RomPackageValidationError.UnsupportedFormat -> text("rom.err.format", error.actual)
    is RomPackageValidationError.UnsupportedVersion -> text("rom.err.version", error.actual.toString())
    is RomPackageValidationError.InvalidMachineId -> text("rom.err.machineId", error.actual)
    RomPackageValidationError.EmptyComponents -> text("rom.err.emptyComponents")
    is RomPackageValidationError.InvalidComponentId -> text("rom.err.componentId", error.actual)
    is RomPackageValidationError.InvalidRole -> text("rom.err.role", error.componentId, error.actual)
    is RomPackageValidationError.InvalidPath -> text("rom.err.path", error.componentId, error.actual)
    is RomPackageValidationError.DuplicateComponentId -> text("rom.err.duplicateId", error.id)
    is RomPackageValidationError.DuplicateComponentPath -> text("rom.err.duplicatePath", error.path)
    is RomPackageValidationError.MissingComponentFile -> text("rom.err.missingFile", error.path)
    is RomPackageValidationError.InvalidDeclaredSize -> text("rom.err.declaredSize", error.componentId, error.actual.toString())
    is RomPackageValidationError.SizeMismatch -> text("rom.err.sizeMismatch", error.componentId, error.expected.toString(), error.actual.toString())
    is RomPackageValidationError.InvalidSha256 -> text("rom.err.invalidHash", error.componentId, error.actual)
    is RomPackageValidationError.Sha256Mismatch -> text("rom.err.hashMismatch", error.componentId, error.expected, error.actual)
}

internal fun StudioStrings.romPackageError(error: DesktopRomPackageError): String = when (error) {
    is DesktopRomPackageError.PackageTooLarge -> text("rom.err.packageSize", error.actual.toString(), error.maximum.toString())
    is DesktopRomPackageError.TooManyEntries -> text("rom.err.entryCount", error.maximum.toString())
    is DesktopRomPackageError.EntryTooLarge -> text("rom.err.entrySize", error.path, error.maximum.toString())
    is DesktopRomPackageError.UnsafePath -> text("rom.err.unsafePath", error.path)
    is DesktopRomPackageError.DuplicateEntry -> text("rom.err.duplicateEntry", error.path)
    is DesktopRomPackageError.InvalidZip -> text("rom.err.zip", error.reason)
    DesktopRomPackageError.MissingManifest -> text("rom.err.missingManifest")
    is DesktopRomPackageError.InvalidManifest -> text("rom.err.manifest", error.reason)
    is DesktopRomPackageError.Validation -> error.errors.joinToString("\n") { romValidationError(it) }
}

internal fun StudioStrings.romLoadError(error: DesktopRomLoadError): String = when (error) {
    is DesktopRomLoadError.InvalidRom -> text("rom.err.invalidRom", romImportError(error.error))
    is DesktopRomLoadError.InvalidPackage -> text("rom.err.invalidPackage", romPackageError(error.error))
    is DesktopRomLoadError.SessionCreation -> text("rom.err.session", romSessionError(error.error))
}


internal fun StudioStrings.romSetInputIssue(issue: com.digihori.pgp.desktop.rom.RomSetInputIssue): String = when (issue) {
    com.digihori.pgp.desktop.rom.RomSetInputIssue.MissingFile -> text("romSet.err.missing")
    com.digihori.pgp.desktop.rom.RomSetInputIssue.UnreadableFile -> text("romSet.err.unreadable")
    is com.digihori.pgp.desktop.rom.RomSetInputIssue.SizeMismatch ->
        text("romSet.err.size", (issue.expectedSize / 1024).toString(), issue.actualSize.toString())
}

internal fun StudioStrings.romSetCreationError(failure: Throwable): String = when {
    failure is com.digihori.pgp.desktop.rom.RomSetValidationException -> romImportError(failure.error)
    failure.message == "The output must not replace an input ROM." -> text("romSet.err.inputDestination")
    failure.message == "The destination already exists." -> text("romSet.err.destinationExists")
    failure is com.digihori.pgp.desktop.rom.RomSetInputException -> romSetInputIssue(failure.issue)
    else -> text("romSet.err.creation", failure.message ?: failure::class.simpleName.orEmpty())
}
