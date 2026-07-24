package com.example.infinite_track.data.location.discovery

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GooglePlacesInitializationContractTest {
    private val repoRoot = File(requireNotNull(System.getProperty("user.dir"))).parentFile!!

    @Test
    fun `places uses fixed new sdk and new initialization`() {
        val versions = repoRoot.resolve("gradle/libs.versions.toml").readText()
        val provider = repoRoot.resolve(
            "app/src/main/java/com/example/infinite_track/data/location/discovery/" +
                "GooglePlacesClientProvider.kt"
        ).readText()

        assertTrue(versions.contains("places = \"3.5.0\""))
        assertTrue(provider.contains("Places.initializeWithNewPlacesApiEnabled("))
        assertFalse(provider.contains("Places.initialize(context"))
    }

    @Test
    fun `debug does not redundantly disable resource shrinking`() {
        val buildFile = repoRoot.resolve("app/build.gradle.kts").readText()
        val debugBlock = buildFile.substringAfter("getByName(\"debug\")")
            .substringBefore("}")

        assertFalse(debugBlock.contains("isShrinkResources = false"))
        assertTrue(debugBlock.contains("isMinifyEnabled = false"))
        assertTrue(debugBlock.contains("isDebuggable = true"))
    }
}
