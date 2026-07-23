package com.example.infinite_track.data.soucre.local.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File

class AttendancePreferenceRuntimeStateTest {
    private lateinit var tempFile: File
    private lateinit var scope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var attendancePreference: AttendancePreference

    @Before
    fun setUp() {
        tempFile = File.createTempFile("attendance-runtime", ".preferences_pb").also { it.delete() }
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        dataStore = PreferenceDataStoreFactory.create(scope = scope) { tempFile }
        attendancePreference = AttendancePreference(dataStore)
    }

    @After
    fun tearDown() {
        scope.cancel()
        tempFile.delete()
    }

    @Test
    fun `clearAttendanceRuntimeState removes active attendance geofence and reminder state`() = runBlocking {
        attendancePreference.saveActiveAttendanceId(123)
        attendancePreference.setUserInsideGeofence(true)
        attendancePreference.saveLastGeofenceParams(
            StoredGeofence(
                id = "geofence-request-123",
                coordinate = GeoCoordinate(-0.898, 119.87),
                radius = DistanceMeters(100.0)
            )
        )
        attendancePreference.addReminderGeofences(
            listOf(
                ReminderGeofence(
                    id = "reminder-1",
                    coordinate = GeoCoordinate(-0.898, 119.87),
                    radius = DistanceMeters(100.0)
                )
            )
        )

        attendancePreference.clearAttendanceRuntimeState()

        assertNull(attendancePreference.getActiveAttendanceId().first())
        assertEquals(false, attendancePreference.isUserInsideGeofence().first())
        assertNull(attendancePreference.getLastGeofenceRequestId().first())
        assertNull(attendancePreference.getLastGeofenceParams().first())
        assertEquals(emptyList<ReminderGeofence>(), attendancePreference.getReminderGeofences().first())
    }
}
