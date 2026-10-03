package com.digihori.pgp.core.integration

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFailsWith

class GoldenScenarioRunnerTest {
    @Test
    fun replaysCheckedInSyntheticScenarioThroughPublicSessionApi() {
        val goldenFile = findFromWorkingDirectoryAncestors(GOLDEN_PATH)
        val goldenCase = GoldenScenarioRunner.decode(goldenFile.readText())

        GoldenScenarioRunner.run(goldenCase, ByteArray(0x10000) { 0x33 })
    }

    @Test
    fun rejectsARomWhoseHashDoesNotMatchTheScenario() {
        val goldenFile = findFromWorkingDirectoryAncestors(GOLDEN_PATH)
        val goldenCase = GoldenScenarioRunner.decode(goldenFile.readText())

        assertFailsWith<AssertionError> {
            GoldenScenarioRunner.run(goldenCase, ByteArray(0x10000))
        }
    }

    private fun findFromWorkingDirectoryAncestors(path: String): File =
        requireNotNull(
            generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
                .map { File(it, path) }
                .firstOrNull(File::isFile),
        ) { "Cannot find $path" }

    private companion object {
        const val GOLDEN_PATH: String = "test-data/golden/pc1245-synthetic-reset.json"
    }
}
