package com.digihori.pgp.desktop.mcp

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.AclEntry
import java.nio.file.attribute.AclEntryPermission
import java.nio.file.attribute.AclEntryType
import java.nio.file.attribute.AclFileAttributeView
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

internal interface DesktopMcpCredentialStore {
    fun publish(token: String): DesktopMcpCredentialLocation
    fun removeIfOwned(token: String)
}

internal data class DesktopMcpCredentialLocation(
    val path: Path,
    val headerHelperCommand: String,
)

/** Publishes only the short-lived HTTP header, outside the project tree. */
internal class PlatformDesktopMcpCredentialStore(
    private val environment: Map<String, String> = System.getenv(),
    private val properties: Map<String, String> = System.getProperties().stringPropertyNames()
        .associateWith(System::getProperty),
) : DesktopMcpCredentialStore {
    private val credentialPath: Path = resolveCredentialPath(environment, properties)

    override fun publish(token: String): DesktopMcpCredentialLocation {
        val directory = credentialPath.parent
        Files.createDirectories(directory)
        restrict(directory, directory = true)

        val temporary = if (
            Files.getFileAttributeView(directory, java.nio.file.attribute.PosixFileAttributeView::class.java) != null
        ) {
            Files.createTempFile(
                directory,
                ".mcp-headers-",
                ".tmp",
                PosixFilePermissions.asFileAttribute(FILE_PERMISSIONS),
            )
        } else {
            Files.createTempFile(directory, ".mcp-headers-", ".tmp")
        }
        try {
            Files.writeString(temporary, headerJson(token), StandardCharsets.UTF_8)
            restrict(temporary, directory = false)
            try {
                Files.move(
                    temporary,
                    credentialPath,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                Files.move(temporary, credentialPath, StandardCopyOption.REPLACE_EXISTING)
            }
            restrict(credentialPath, directory = false)
            verifyRestricted(credentialPath)
        } finally {
            Files.deleteIfExists(temporary)
        }
        return DesktopMcpCredentialLocation(credentialPath, helperCommand(credentialPath, properties))
    }

    override fun removeIfOwned(token: String) {
        val storedToken = runCatching {
            Json.parseToJsonElement(Files.readString(credentialPath)).jsonObject["Authorization"]
                ?.jsonPrimitive?.content?.removePrefix("Bearer ")
        }.getOrNull()
        if (storedToken == token) Files.deleteIfExists(credentialPath)
    }

    private fun restrict(path: Path, directory: Boolean) {
        val posix = Files.getFileAttributeView(path, java.nio.file.attribute.PosixFileAttributeView::class.java)
        if (posix != null) {
            Files.setPosixFilePermissions(path, if (directory) DIRECTORY_PERMISSIONS else FILE_PERMISSIONS)
            return
        }
        val acl = Files.getFileAttributeView(path, AclFileAttributeView::class.java)
            ?: error("The filesystem does not support secure POSIX permissions or Windows ACLs")
        val owner = acl.owner
        val permissions = if (directory) DIRECTORY_ACL_PERMISSIONS else FILE_ACL_PERMISSIONS
        acl.acl = listOf(
            AclEntry.newBuilder()
                .setType(AclEntryType.ALLOW)
                .setPrincipal(owner)
                .setPermissions(permissions)
                .build(),
        )
    }

    private fun verifyRestricted(path: Path) {
        val posix = Files.getFileAttributeView(path, java.nio.file.attribute.PosixFileAttributeView::class.java)
        if (posix != null) {
            check(Files.getPosixFilePermissions(path) == FILE_PERMISSIONS) {
                "MCP credential file permissions are not owner-only"
            }
            return
        }
        val acl = Files.getFileAttributeView(path, AclFileAttributeView::class.java)
            ?: error("Could not verify MCP credential file access")
        check(acl.acl.isNotEmpty() && acl.acl.all { it.principal() == acl.owner && it.type() == AclEntryType.ALLOW }) {
            "MCP credential file ACL is not owner-only"
        }
    }

    companion object {
        private val DIRECTORY_PERMISSIONS = PosixFilePermissions.fromString("rwx------")
        private val FILE_PERMISSIONS = PosixFilePermissions.fromString("rw-------")
        private val FILE_ACL_PERMISSIONS = setOf(
            AclEntryPermission.READ_DATA,
            AclEntryPermission.WRITE_DATA,
            AclEntryPermission.APPEND_DATA,
            AclEntryPermission.READ_ATTRIBUTES,
            AclEntryPermission.WRITE_ATTRIBUTES,
            AclEntryPermission.READ_ACL,
            AclEntryPermission.WRITE_ACL,
            AclEntryPermission.READ_NAMED_ATTRS,
            AclEntryPermission.WRITE_NAMED_ATTRS,
            AclEntryPermission.DELETE,
            AclEntryPermission.SYNCHRONIZE,
        )
        private val DIRECTORY_ACL_PERMISSIONS = FILE_ACL_PERMISSIONS + setOf(
            AclEntryPermission.DELETE_CHILD,
            AclEntryPermission.EXECUTE,
        )

        internal fun resolveCredentialPath(
            environment: Map<String, String>,
            properties: Map<String, String>,
        ): Path {
            val home = properties["user.home"]?.takeIf(String::isNotBlank)
                ?: error("Cannot determine the user home directory")
            val os = properties["os.name"].orEmpty().lowercase()
            val directory = when {
                os.contains("mac") -> Path.of(home, "Library", "Application Support", "PokecomGOStudio")
                os.contains("win") -> Path.of(
                    environment["LOCALAPPDATA"]?.takeIf(String::isNotBlank)
                        ?: error("LOCALAPPDATA is not available"),
                    "PokecomGOStudio",
                )
                else -> Path.of(
                    environment["XDG_RUNTIME_DIR"]?.takeIf(String::isNotBlank)
                        ?: Path.of(home, ".local", "state").toString(),
                    "pokecom-go-studio",
                )
            }
            return directory.resolve("mcp-headers.json")
        }

        internal fun helperCommand(path: Path, properties: Map<String, String>): String {
            val os = properties["os.name"].orEmpty().lowercase()
            val value = path.toAbsolutePath().toString()
            return if (os.contains("win")) {
                "cmd.exe /d /s /c type \"${value.replace("\"", "\"\"")}\""
            } else {
                "/bin/cat '${value.replace("'", "'\\''")}'"
            }
        }

        private fun headerJson(token: String): String = Json.encodeToString(buildJsonObject {
            put("Authorization", "Bearer $token")
        })
    }
}
