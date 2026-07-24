package com.example.infinite_track

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Test

class LegacyGeofenceConfigurationContractTest {
    @Test
    fun `legacy geofence and FCM symbols are absent`() {
        val appRoot = File(".").canonicalFile
        val repositoryRoot = requireNotNull(appRoot.parentFile)
        val text = sequenceOf(
            File(appRoot, "src/main"),
            File(appRoot, "build.gradle.kts"),
            File(repositoryRoot, "build.gradle.kts"),
            File(repositoryRoot, "gradle/libs.versions.toml"),
            File(repositoryRoot, ".github/workflows")
        ).flatMap { root ->
            if (root.isFile) sequenceOf(root) else root.walkTopDown().filter(File::isFile)
        }.joinToString("\n") { it.readText() }

        listOf(
            "GeofenceManager",
            "LAST_GEOFENCE_",
            "REMINDER_GEOFENCES_KEY",
            "StoredGeofence",
            "FirebaseMessagingService",
            "firebase.messaging",
            "google.gms.google.services"
        ).forEach { forbidden ->
            assertFalse("Forbidden symbol remains: $forbidden", text.contains(forbidden))
        }

        assertFalse(
            "Forbidden legacy ReminderGeofence symbol remains",
            Regex("\\bReminderGeofence\\b").containsMatchIn(text)
        )

        val attendancePreferenceSource = File(
            appRoot,
            "src/main/java/com/example/infinite_track/data/soucre/local/preferences/AttendancePreference.kt"
        ).readText()
        assertFalse(
            "AttendancePreference must not own legacy notification cooldowns",
            attendancePreferenceSource.contains("NOTIFICATION_COOLDOWNS_KEY")
        )
    }
}
