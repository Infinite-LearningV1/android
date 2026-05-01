package com.example.infinite_track

import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

/**
 * Release Build Configuration Verification Test
 * 
 * Purpose: Verifies release build configuration values such as:
 * - BuildConfig.DEBUG == false
 * - application ID
 * - version information
 * - required tokens
 *
 * This suite is gated to release unit tests and skipped when the build type is not `release`.
 *
 * Run with: ./gradlew app:testReleaseUnitTest
 */
class BuildConfigReleaseTest {

    @Before
    fun requireReleaseBuild() {
        assumeTrue("Release-only verification must run only for release unit tests", BuildConfig.BUILD_TYPE == "release")
    }

    @Test
    fun `verify BuildConfig DEBUG is false for release builds`() {
        // Verifies that the release test variant exposes BuildConfig.DEBUG == false.
        // If this fails, check build.gradle.kts release configuration.
        assertFalse(
            "BuildConfig.DEBUG must be false in release builds! " +
            "Current value: ${BuildConfig.DEBUG}. " +
            "Check app/build.gradle.kts -> buildTypes.release configuration.",
            BuildConfig.DEBUG
        )
    }

    @Test
    fun `verify application ID is correct`() {
        assertEquals(
            "Application ID mismatch",
            "com.example.infinite_track",
            BuildConfig.APPLICATION_ID
        )
    }

    @Test
    fun `verify version code is set`() {
        assertTrue(
            "Version code must be greater than 0",
            BuildConfig.VERSION_CODE > 0
        )
    }

    @Test
    fun `verify version name is not empty`() {
        assertTrue(
            "Version name must not be empty",
            BuildConfig.VERSION_NAME.isNotEmpty()
        )
    }

    @Test
    fun `verify Mapbox token is configured when provided`() {
        assumeTrue(
            "Mapbox token verification requires MAPBOX_PUBLIC_TOKEN in this test environment",
            BuildConfig.MAPBOX_PUBLIC_TOKEN.isNotEmpty()
        )

        assertTrue(
            "Mapbox token must not be blank when provided to the release test environment",
            BuildConfig.MAPBOX_PUBLIC_TOKEN.isNotBlank()
        )
    }

    @Test
    fun `display build information`() {
        println("========================================")
        println("Build Configuration Information:")
        println("========================================")
        println("Application ID: ${BuildConfig.APPLICATION_ID}")
        println("Version Code: ${BuildConfig.VERSION_CODE}")
        println("Version Name: ${BuildConfig.VERSION_NAME}")
        println("Build Type: ${BuildConfig.BUILD_TYPE}")
        println("DEBUG Mode: ${BuildConfig.DEBUG}")
        println("========================================")
        
        // Diagnostic output only; no release-config assertion is performed here.
        assertTrue(true)
    }
}

