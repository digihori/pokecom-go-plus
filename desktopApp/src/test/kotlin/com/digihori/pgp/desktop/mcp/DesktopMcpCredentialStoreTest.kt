package com.digihori.pgp.desktop.mcp

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.AclEntryType
import java.nio.file.attribute.AclFileAttributeView
import java.nio.file.attribute.PosixFileAttributeView
import java.nio.file.attribute.PosixFilePermissions
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopMcpCredentialStoreTest {
    @Test
    fun publishesOwnerOnlyHeaderJsonAndRemovesOnlyItsOwnToken() {
        val home = Files.createTempDirectory("pgp-mcp-home")
        val properties = mapOf("user.home" to home.toString(), "os.name" to "Mac OS X")
        val store = PlatformDesktopMcpCredentialStore(emptyMap(), properties)

        val location = store.publish("first-token")
        val authorization = Json.parseToJsonElement(Files.readString(location.path)).jsonObject
            .getValue("Authorization").jsonPrimitive.content
        assertEquals("Bearer first-token", authorization)
        val posix = Files.getFileAttributeView(location.path, PosixFileAttributeView::class.java)
        if (posix != null) {
            assertEquals(
                PosixFilePermissions.fromString("rw-------"),
                Files.getPosixFilePermissions(location.path),
            )
        } else {
            val acl = requireNotNull(Files.getFileAttributeView(location.path, AclFileAttributeView::class.java))
            assertTrue(acl.acl.isNotEmpty())
            assertTrue(acl.acl.all { it.principal() == acl.owner && it.type() == AclEntryType.ALLOW })
        }
        assertTrue(location.headerHelperCommand.contains(location.path.toString()))

        store.removeIfOwned("different-token")
        assertTrue(Files.exists(location.path))
        store.removeIfOwned("first-token")
        assertFalse(Files.exists(location.path))
    }

    @Test
    fun resolvesPlatformSpecificLocations() {
        val home = "/Users/test"
        assertEquals(
            Path.of(home, "Library", "Application Support", "PokecomGOStudio", "mcp-headers.json"),
            PlatformDesktopMcpCredentialStore.resolveCredentialPath(
                emptyMap(),
                mapOf("user.home" to home, "os.name" to "Mac OS X"),
            ),
        )
        assertEquals(
            Path.of("C:\\Users\\test\\AppData\\Local", "PokecomGOStudio", "mcp-headers.json"),
            PlatformDesktopMcpCredentialStore.resolveCredentialPath(
                mapOf("LOCALAPPDATA" to "C:\\Users\\test\\AppData\\Local"),
                mapOf("user.home" to "C:\\Users\\test", "os.name" to "Windows 11"),
            ),
        )
        assertEquals(
            Path.of("/run/user/1000", "pokecom-go-studio", "mcp-headers.json"),
            PlatformDesktopMcpCredentialStore.resolveCredentialPath(
                mapOf("XDG_RUNTIME_DIR" to "/run/user/1000"),
                mapOf("user.home" to "/home/test", "os.name" to "Linux"),
            ),
        )
    }
}
