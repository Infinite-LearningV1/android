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
import com.example.infinite_track.domain.repository.AuthRuntimeCleaner
import com.google.gson.Gson
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
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
            AuthRuntimeCleaner {
                userPreference.clearAuthData()
                userDao.clearUserProfile()
                todayStatusPreference.clearTodayStatusCache()
                attendancePreference.clearAttendanceRuntimeState()
                removeAllGeofencesCalls += 1
            }
        )

        userPreference.saveSession(
            token = "SENTINEL_TOKEN_ALPHA",
            userId = "SENTINEL_USER_ALPHA",
            refreshToken = "SENTINEL_REFRESH_ALPHA",
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

    @Test
    fun `clear runtime awaits geofence cleanup before returning`() = runTest {
        val geofenceCleanupCompleted = kotlinx.coroutines.CompletableDeferred<Boolean>()
        val useCase = ClearAuthenticatedRuntimeUseCase(
            AuthRuntimeCleaner {
                delay(10)
                geofenceCleanupCompleted.complete(true)
            }
        )

        useCase()

        assertEquals(true, geofenceCleanupCompleted.await())
    }

    @Test
    fun `clear runtime continues best effort after one step fails and reports failure`() = runTest {
        val userPreference = UserPreference(createDataStore("clear_runtime_user_failure"))
        val attendancePreference = AttendancePreference(createDataStore("clear_runtime_attendance_failure"))
        val todayStatusPreference = TodayStatusPreference(createDataStore("clear_runtime_today_status_failure"), Gson())
        val userDao = ThrowingUserDao()
        var removeAllGeofencesCalls = 0
        val useCase = ClearAuthenticatedRuntimeUseCase(
            AuthRuntimeCleaner {
                userPreference.clearAuthData()
                userDao.clearUserProfile()
                todayStatusPreference.clearTodayStatusCache()
                attendancePreference.clearAttendanceRuntimeState()
                removeAllGeofencesCalls += 1
            }
        )

        userPreference.saveSession(
            token = "SENTINEL_TOKEN_BETA",
            userId = "SENTINEL_USER_BETA",
            refreshToken = "SENTINEL_REFRESH_BETA",
            lastRefreshAt = 321L
        )
        userPreference.saveLastProfileSyncAt(654L)
        userDao.insertOrUpdateUserProfile(sampleUserEntity())
        attendancePreference.saveActiveAttendanceId(100)
        attendancePreference.setUserInsideGeofence(true)
        attendancePreference.saveLastGeofenceParams("attendance-geofence", -1.0, 120.0, 150f)
        todayStatusPreference.saveTodayStatusCache(sampleCachedTodayStatus())

        try {
            useCase()
            fail("Expected cleanup failure")
        } catch (e: IllegalStateException) {
            assertEquals("Failed to clear authenticated runtime; completed with 1 cleanup error(s)", e.message)
        }

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
            userId = "SENTINEL_USER_ALPHA",
            todayDate = "2099-01-01",
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
            id = 900_001,
            fullName = "SENTINEL_NAME_ALPHA",
            email = "SENTINEL_EMAIL_ALPHA",
            roleName = "staff",
            positionName = "Engineer",
            programName = "Program",
            divisionName = "Division",
            nipNim = "SENTINEL_ID_ALPHA",
            phone = "SENTINEL_PHONE_ALPHA",
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

    private class ThrowingUserDao : UserDao {
        private var profile: UserEntity? = null

        override suspend fun insertOrUpdateUserProfile(userEntity: UserEntity) {
            profile = userEntity
        }

        override fun getUserProfileFlow(): Flow<UserEntity?> = flowOf(profile)

        override suspend fun getUserProfile(): UserEntity? = profile

        override suspend fun clearUserProfile() {
            profile = null
            throw IllegalStateException("user profile cleanup failed")
        }
    }
}
