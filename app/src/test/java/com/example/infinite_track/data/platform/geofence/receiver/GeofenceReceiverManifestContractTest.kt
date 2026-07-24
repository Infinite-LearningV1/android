package com.example.infinite_track.data.platform.geofence.receiver

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class GeofenceReceiverManifestContractTest {

    @Test
    fun `manifest retains both geofence receivers while boot uses reconciliation receiver`() {
        val manifest = source("src/main/AndroidManifest.xml")

        assertTrue(
            "Legacy receiver must remain declared until Task 11 cleanup",
            manifest.contains("android:name=\".presentation.geofencing.GeofenceBroadcastReceiver\"")
        )
        assertTrue(
            "V2 receiver must remain declared for canonical gf2 registrations",
            manifest.contains("android:name=\".data.platform.geofence.receiver.GeofenceBroadcastReceiver\"")
        )
        assertTrue(
            manifest.contains(
                "android:name=\".data.platform.geofence.receiver.BootCompletedReceiver\""
            )
        )
    }

    private fun source(relativePath: String): String = File(
        requireNotNull(System.getProperty("user.dir")),
        relativePath
    ).readText()
}
