package com.example.infinite_track.data.soucre.local.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.example.infinite_track.domain.model.attendance.CheckinWindow
import com.example.infinite_track.domain.model.attendance.TodayStatus
import com.example.infinite_track.domain.model.auth.AuthRuntimePolicy
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

class TodayStatusPreferenceTest {
    private lateinit var tempFile: File
    private lateinit var scope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>

    @Before
    fun setUp() {
        tempFile = File.createTempFile("today-status", ".preferences_pb").also { it.delete() }
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        dataStore = PreferenceDataStoreFactory.create(scope = scope) { tempFile }
    }

    @After
    fun tearDown() {
        scope.cancel()
        tempFile.delete()
    }

    @Test
    fun `round trips cached today status payload and clears it`() = runTest {
        val preference = createTodayStatusPreference()
        val payload = CachedTodayStatusPayload(
            userId = "42",
            todayDate = "2026-07-05",
            attendanceSessionStateId = 2,
            attendanceSessionStateKey = "active",
            activeAttendanceId = 123,
            fetchedAtMillis = 1_720_000_000_000,
            ttlSeconds = AuthRuntimePolicy.SHARED_TTL_SECONDS,
            status = TodayStatus(
                canCheckIn = false,
                canCheckOut = true,
                checkedInAt = "08:00:00",
                checkedOutAt = null,
                activeMode = "Work From Office",
                activeLocation = null,
                todayDate = "2026-07-05",
                isHoliday = false,
                holidayCheckinEnabled = false,
                currentTime = "09:00:00",
                checkinWindow = CheckinWindow("07:00:00", "09:00:00"),
                checkoutAutoTime = "17:00:00"
            )
        )

        preference.saveTodayStatusCache(payload)
        assertEquals(payload, preference.getTodayStatusCache().first())

        preference.clearTodayStatusCache()
        assertEquals(null, preference.getTodayStatusCache().first())
    }

    private fun createTodayStatusPreference(): TodayStatusPreference {
        return TodayStatusPreference(
            dataStore = dataStore,
            gson = Gson()
        )
    }
}
