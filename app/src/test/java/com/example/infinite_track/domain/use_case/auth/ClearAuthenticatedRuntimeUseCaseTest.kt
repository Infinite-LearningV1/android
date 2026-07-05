package com.example.infinite_track.domain.use_case.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.example.infinite_track.data.soucre.local.preferences.AttendancePreference
import com.example.infinite_track.data.soucre.local.preferences.CachedTodayStatusPayload
import com.example.infinite_track.data.soucre.local.preferences.TodayStatusPreference
import com.example.infinite_track.data.soucre.local.preferences.UserPreference
import com.example.infinite_track.data.soucre.local.room.UserDao
import com.example.infinite_track.data.soucre.local.room.UserEntity
import com.example.infinite_track.domain.model.attendance.CheckinWindow
import com.example.infinite_track.domain.model.attendance.TodayStatus
import com.google.gson.Gson
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClearAuthenticatedRuntimeUseCaseTest {

    @Test
    fun `clear runtime removes auth profile attendance status cache and geofences`() = runTest {
        val userPreference = UserPreference(createDataStore("clear_runtime_user"))
        val attendancePreference = AttendancePreference(createDataStore("clear_runtime_attendance"))
        val todayStatusPreference = TodayStatusPreference(createDataStore("clear_runtime_today_status"), Gson())
        val userDao = FakeUserDao()
        var removeAllGeofencesCalls = 0
        val useCase = ClearAuthenticatedRuntimeUseCase(
            userPreference = userPreference,
            userDao = userDao,
            attendancePreference = attendancePreference,
            todayStatusPreference = todayStatusPreference,
            removeAllGeofences = { removeAllGeofencesCalls += 1 }
        )

        userPreference.saveSession(
            token = "access-token",
            userId = "42",
            refreshToken = "refresh-token",
            lastRefreshAt = 123L
        )
        userPreference.saveLastProfileSyncAt(456L)
        userDao.insertOrUpdateUserProfile(sampleUserEntity())
        attendancePreference.saveActiveAttendanceId(99)
        attendancePreference.setUserInsideGeofence(true)
        attendancePreference.saveLastGeofenceParams("attendance-geofence", -0.9, 119.8, 100f)
        todayStatusPreference.saveTodayStatusCache(sampleCachedTodayStatus())

        useCase()

        assertEquals("", userPreference.getAuthToken().first())
        assertEquals("", userPreference.getUserId().first())
        assertEquals("", userPreference.getRefreshToken().first())
        assertEquals(0L, userPreference.getLastRefreshAt().first())
        assertEquals(0L, userPreference.getLastProfileSyncAt().first())
        assertNull(userDao.getUserProfile())
        assertNull(attendancePreference.getActiveAttendanceId().first())
        assertEquals(false, attendancePreference.isUserInsideGeofence().first())
        assertNull(attendancePreference.getLastGeofenceParams().first())
        assertNull(todayStatusPreference.getTodayStatusCache().first())
        assertEquals(1, removeAllGeofencesCalls)
    }

    private fun createDataStore(name: String): DataStore<Preferences> {
        val testFile = File.createTempFile(name, ".preferences_pb").also { it.delete() }
        return PreferenceDataStoreFactory.create(produceFile = { testFile })
    }

    private fun sampleCachedTodayStatus(): CachedTodayStatusPayload {
        return CachedTodayStatusPayload(
            userId = "42",
            todayDate = "2026-07-05",
            attendanceSessionStateId = 1,
            attendanceSessionStateKey = "checked_in",
            activeAttendanceId = 99,
            fetchedAtMillis = 1_000L,
            ttlSeconds = 300,
            status = TodayStatus(
                canCheckIn = false,
                canCheckOut = true,
                checkedInAt = "08:00:00",
                checkedOutAt = null,
                activeMode = "WFO",
                activeLocation = null,
                todayDate = "2026-07-05",
                isHoliday = false,
                holidayCheckinEnabled = false,
                currentTime = "09:00:00",
                checkinWindow = CheckinWindow(startTime = "08:00:00", endTime = "10:00:00"),
                checkoutAutoTime = "17:00:00"
            )
        )
    }

    private fun sampleUserEntity(): UserEntity {
        return UserEntity(
            id = 42,
            fullName = "Test User",
            email = "user@example.test",
            roleName = "staff",
            positionName = "Engineer",
            programName = "Program",
            divisionName = "Division",
            nipNim = "EMP-42",
            phone = "08123456789",
            photo = null,
            photoUpdatedAt = null,
            latitude = null,
            longitude = null,
            radius = null,
            locationDescription = null,
            locationCategoryName = null,
            faceEmbedding = byteArrayOf(1, 2, 3)
        )
    }

    private class FakeUserDao : UserDao {
        private var profile: UserEntity? = null

        override suspend fun insertOrUpdateUserProfile(userEntity: UserEntity) {
            profile = userEntity
        }

        override fun getUserProfileFlow(): Flow<UserEntity?> = flowOf(profile)

        override suspend fun getUserProfile(): UserEntity? = profile

        override suspend fun clearUserProfile() {
            profile = null
        }
    }
}
