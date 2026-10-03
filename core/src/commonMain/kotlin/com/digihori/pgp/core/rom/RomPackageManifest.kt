package com.digihori.pgp.core.rom

import kotlinx.serialization.Serializable

@Serializable
public data class RomPackageManifest(
    public val format: String,
    public val formatVersion: Int,
    public val machineId: String,
    public val title: String? = null,
    public val components: List<RomPackageComponentManifest>,
)

@Serializable
public data class RomPackageComponentManifest(
    public val id: String,
    public val role: String,
    public val file: String,
    public val size: Int,
    public val sha256: String,
)

public data class RomPackageEntryInfo(
    public val path: String,
    public val size: Int,
    public val sha256: String,
)

public sealed interface RomPackageValidationResult {
    public data class Success(
        public val romSet: RomSet,
        public val warnings: List<RomPackageWarning>,
    ) : RomPackageValidationResult

    public data class Failure(public val errors: List<RomPackageValidationError>) :
        RomPackageValidationResult
}

public sealed interface RomPackageValidationError {
    public data class UnsupportedFormat(public val actual: String) : RomPackageValidationError
    public data class UnsupportedVersion(public val actual: Int) : RomPackageValidationError
    public data class InvalidMachineId(public val actual: String) : RomPackageValidationError
    public data object EmptyComponents : RomPackageValidationError
    public data class InvalidComponentId(public val actual: String) : RomPackageValidationError
    public data class InvalidRole(public val componentId: String, public val actual: String) :
        RomPackageValidationError
    public data class InvalidPath(public val componentId: String, public val actual: String) :
        RomPackageValidationError
    public data class DuplicateComponentId(public val id: String) : RomPackageValidationError
    public data class DuplicateComponentPath(public val path: String) : RomPackageValidationError
    public data class MissingComponentFile(public val path: String) : RomPackageValidationError
    public data class InvalidDeclaredSize(public val componentId: String, public val actual: Int) :
        RomPackageValidationError
    public data class SizeMismatch(
        public val componentId: String,
        public val expected: Int,
        public val actual: Int,
    ) : RomPackageValidationError
    public data class InvalidSha256(public val componentId: String, public val actual: String) :
        RomPackageValidationError
    public data class Sha256Mismatch(
        public val componentId: String,
        public val expected: String,
        public val actual: String,
    ) : RomPackageValidationError
}

public sealed interface RomPackageWarning {
    public data class UnlistedEntry(public val path: String) : RomPackageWarning
}

public object RomPackageValidator {
    public const val FORMAT: String = "pgp-rom-package"
    public const val VERSION: Int = 1

    public fun validate(
        manifest: RomPackageManifest,
        entries: Map<String, Pair<RomPackageEntryInfo, ByteArray>>,
    ): RomPackageValidationResult {
        val errors = mutableListOf<RomPackageValidationError>()
        if (manifest.format != FORMAT) {
            errors += RomPackageValidationError.UnsupportedFormat(manifest.format)
        }
        if (manifest.formatVersion != VERSION) {
            errors += RomPackageValidationError.UnsupportedVersion(manifest.formatVersion)
        }
        val machineId = runCatching { MachineId(manifest.machineId) }.getOrElse {
            errors += RomPackageValidationError.InvalidMachineId(manifest.machineId)
            null
        }
        if (manifest.components.isEmpty()) errors += RomPackageValidationError.EmptyComponents

        manifest.components.groupingBy { it.id }.eachCount()
            .filterValues { it > 1 }.keys
            .forEach { errors += RomPackageValidationError.DuplicateComponentId(it) }
        manifest.components.groupingBy { it.file }.eachCount()
            .filterValues { it > 1 }.keys
            .forEach { errors += RomPackageValidationError.DuplicateComponentPath(it) }

        val components = mutableListOf<RomComponent>()
        for (component in manifest.components) {
            val id = runCatching { RomComponentId(component.id) }.getOrElse {
                errors += RomPackageValidationError.InvalidComponentId(component.id)
                null
            }
            val role = runCatching { RomRole(component.role) }.getOrElse {
                errors += RomPackageValidationError.InvalidRole(component.id, component.role)
                null
            }
            if (!component.file.isSafeComponentPath()) {
                errors += RomPackageValidationError.InvalidPath(component.id, component.file)
            }
            if (component.size < 0) {
                errors += RomPackageValidationError.InvalidDeclaredSize(component.id, component.size)
            }
            if (!component.sha256.matches(SHA256_PATTERN)) {
                errors += RomPackageValidationError.InvalidSha256(component.id, component.sha256)
            }
            val entry = entries[component.file]
            if (entry == null) {
                errors += RomPackageValidationError.MissingComponentFile(component.file)
                continue
            }
            if (entry.first.size != component.size) {
                errors += RomPackageValidationError.SizeMismatch(
                    component.id, component.size, entry.first.size,
                )
            }
            if (entry.first.sha256 != component.sha256) {
                errors += RomPackageValidationError.Sha256Mismatch(
                    component.id, component.sha256, entry.first.sha256,
                )
            }
            if (id != null && role != null) components += RomComponent(id, role, entry.second)
        }

        if (errors.isNotEmpty() || machineId == null) {
            return RomPackageValidationResult.Failure(errors)
        }
        val listed = manifest.components.mapTo(mutableSetOf()) { it.file }
        val warnings = entries.keys.filterNot { it in listed }
            .sorted().map { RomPackageWarning.UnlistedEntry(it) }
        return RomPackageValidationResult.Success(RomSet(machineId, components), warnings)
    }

    private fun String.isSafeComponentPath(): Boolean =
        startsWith("rom/") &&
            length > "rom/".length &&
            !startsWith('/') &&
            '\\' !in this &&
            split('/').none { it.isEmpty() || it == "." || it == ".." }

    private val SHA256_PATTERN: Regex = Regex("[0-9a-f]{64}")
}
