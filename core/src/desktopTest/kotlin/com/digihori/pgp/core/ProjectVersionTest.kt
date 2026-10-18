package com.digihori.pgp.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ProjectVersionTest {
    @Test
    fun runtimeVersionMatchesGradleReleaseVersion() {
        val releaseVersion = assertNotNull(System.getProperty("pgp.projectVersion"))
        assertEquals(releaseVersion, ProjectInfo.VERSION,
            "About and MCP must report the version used to build the distribution")
    }
}
