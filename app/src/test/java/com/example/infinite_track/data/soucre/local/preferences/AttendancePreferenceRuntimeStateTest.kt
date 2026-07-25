package com.example.infinite_track.data.soucre.local.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
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
    fun `clearAttendanceSessionState removes only attendance session values`() = runBlocking {
        attendancePreference.saveActiveAttendanceId(123)
        attendancePreference.saveAttendanceSessionStateKey("active")

        attendancePreference.clearAttendanceSessionState()

        assertNull(attendancePreference.getActiveAttendanceId().first())
        assertNull(attendancePreference.getAttendanceSessionStateKey().first())
    }
}
