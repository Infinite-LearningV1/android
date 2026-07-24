package com.example.infinite_track.data.platform.geofence.receiver

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class GeofenceReceiverManifestContractTest {

    @Test
    fun `manifest declares both geofence receivers while boot remains legacy`() {
        val manifest = source("src/main/AndroidManifest.xml")
        val bootReceiver = source(
            "src/main/java/com/example/infinite_track/presentation/geofencing/BootCompletedReceiver.kt"
        )

        assertTrue(
            "Legacy receiver must remain declared while BootCompletedReceiver uses GeofenceManager",
            manifest.contains("android:name=\".presentation.geofencing.GeofenceBroadcastReceiver\"")
        )
        assertTrue(
            "V2 receiver must remain declared for canonical gf2 registrations",
            manifest.contains("android:name=\".data.platform.geofence.receiver.GeofenceBroadcastReceiver\"")
        )
        assertTrue(bootReceiver.contains("geofenceManager.addGeofence("))
        assertTrue(bootReceiver.contains("geofenceManager.addReminderGeofence("))
    }

    private fun source(relativePath: String): String = File(
        requireNotNull(System.getProperty("user.dir")),
        relativePath
    ).readText()
}
