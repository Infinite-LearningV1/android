package com.example.infinite_track.data.platform.geofence.receiver

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BootCompletedReceiverContractTest {

    @Test
    fun `boot receiver replaces connected reconciliation work without restoring stored geometry`() {
        val receiver = source(
            "src/main/java/com/example/infinite_track/data/platform/geofence/receiver/BootCompletedReceiver.kt"
        )

        assertTrue(receiver.contains("geofence_runtime_reconcile"))
        assertTrue(receiver.contains("NetworkType.CONNECTED"))
        assertTrue(receiver.contains("ExistingWorkPolicy.REPLACE"))
        assertTrue(receiver.contains("BackoffPolicy.EXPONENTIAL"))
        assertTrue(receiver.contains("30, TimeUnit.SECONDS"))
        assertTrue(receiver.contains("addTag"))
        assertTrue(receiver.contains("GeofenceReconciliationWorker"))
        assertFalse(receiver.contains("AttendancePreference"))
        assertFalse(receiver.contains("GeofenceManager"))
        assertFalse(receiver.contains("getLastGeofenceParams"))
        assertFalse(receiver.contains("getReminderGeofences"))
    }

    @Test
    fun `manifest routes boot completed to reconciliation receiver and retains boot permission`() {
        val manifest = source("src/main/AndroidManifest.xml")

        assertTrue(
            manifest.contains(
                "android:name=\".data.platform.geofence.receiver.BootCompletedReceiver\""
            )
        )
        assertTrue(manifest.contains("android.permission.RECEIVE_BOOT_COMPLETED"))
    }

    private fun source(relativePath: String): String = File(
        requireNotNull(System.getProperty("user.dir")),
        relativePath
    ).readText()
}
