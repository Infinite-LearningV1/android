package com.example.infinite_track.presentation.screen.profile

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualLogoutIntegrationContractTest {
    private val appModuleRoot = File(requireNotNull(System.getProperty("user.dir")))

    @Test
    fun `manual logout navigation clears every authenticated root destination`() {
        val source = source(
            "presentation/screen/profile/ProfileScreen.kt"
        )
        val navigationEffect = source.blockBetween(
            start = "ProfileEffect.NavigateToLogin -> {",
            end = "profileViewModel.consumeEffect(effect)"
        )

        assertTrue(navigationEffect.contains("navigate(\"auth_graph\")"))
        assertTrue(
            "Manual logout must clear the full root stack even when Splash is absent",
            navigationEffect.contains("popUpTo(0) { inclusive = true }")
        )
        assertFalse(
            "Manual logout must not depend on a possibly absent start destination",
            navigationEffect.contains("startDestinationId")
        )
    }

    @Test
    fun `session expired dialog visibility follows true to false session transitions`() {
        val source = source("presentation/main/InfiniteTrackApp.kt")
        val sessionSynchronization = source.blockBetween(
            start = "LaunchedEffect(sessionExpired) {",
            end = "val reauthUiCopy"
        )

        assertTrue(
            "Session expiration visibility must synchronize both true and false values",
            sessionSynchronization.contains("showSessionExpiredDialog = sessionExpired")
        )
        assertFalse(
            "A one-way true assignment leaves a stale forced-reauth dialog after manual logout",
            sessionSynchronization.contains("if (sessionExpired)")
        )
    }

    @Test
    fun `logout teardown clears v2 runtime and attendance session state`() {
        val cleaner = source("data/repository/auth/AuthRuntimeCleanerImpl.kt")
        val repositoryModule = source("di/RepositoryModule.kt")

        assertTrue(cleaner.contains("private val geofenceRuntimeRepository: GeofenceRuntimeRepository"))
        assertTrue(cleaner.contains("geofenceRuntimeRepository.clearForLogout()"))
        assertTrue(cleaner.contains("attendancePreference.clearAttendanceSessionState()"))
        assertTrue(repositoryModule.contains("geofenceRuntimeRepository: GeofenceRuntimeRepository"))
    }

    private fun source(relativePath: String): String = File(
        appModuleRoot,
        "src/main/java/com/example/infinite_track/$relativePath"
    ).readText()

    private fun String.blockBetween(start: String, end: String): String {
        assertTrue("Missing start marker: $start", contains(start))
        val afterStart = substringAfter(start, missingDelimiterValue = "")
        assertTrue("Missing end marker after: $start", afterStart.contains(end))
        return afterStart.substringBefore(end, missingDelimiterValue = "")
    }
}
