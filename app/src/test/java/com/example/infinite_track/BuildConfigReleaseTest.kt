package com.example.infinite_track

import org.junit.Test
import org.junit.Assert.*

/**
 * Release Build Configuration Verification Test
 * 
 * Purpose: Ensures that release builds are properly configured with:
 * - BuildConfig.DEBUG == false
 * - Proper version information
 * 
 * This test will FAIL if run against a debug build, ensuring release configuration is correct.
 * 
 * Run with: ./gradlew test
 */
class BuildConfigReleaseTest {

    @Test
    fun `verify BuildConfig DEBUG is false for release builds`() {
        // This assertion ensures that release builds have DEBUG = false
        // If this fails, check build.gradle.kts release configuration
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
    fun `verify Mapbox token is configured`() {
        assertTrue(
            "Mapbox token must be configured in local.properties",
            BuildConfig.MAPBOX_PUBLIC_TOKEN.isNotEmpty()
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
        
        // This test always passes but outputs useful info
        assertTrue(true)
    }
}

