package com.example.infinite_track.domain.use_case.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.example.infinite_track.data.repository.auth.AuthRuntimeCleanerImpl
import com.example.infinite_track.data.soucre.local.preferences.AttendancePreference
import com.example.infinite_track.data.soucre.local.preferences.CachedTodayStatusPayload
import com.example.infinite_track.data.soucre.local.preferences.TodayStatusPreference
import com.example.infinite_track.data.soucre.local.preferences.UserPreference
import com.example.infinite_track.data.soucre.local.room.UserDao
import com.example.infinite_track.data.soucre.local.room.UserEntity
import com.example.infinite_track.domain.model.attendance.CheckinWindow
import com.example.infinite_track.domain.model.attendance.TodayStatus
import com.example.infinite_track.domain.model.geofence.GeofenceDisabledReason
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeFailure
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeMode
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeReadiness
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeResult
import com.example.infinite_track.domain.repository.GeofenceRuntimeRepository
import com.google.gson.Gson
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ClearAuthenticatedRuntimeUseCaseTest {

    @Test
    fun `clear runtime clears geofences before auth profile today and attendance caches`() = runTest {
        val calls = mutableListOf<String>()
        val userPreference = UserPreference(recordingDataStore("clear_runtime_user", "auth.clear", calls))
        val attendancePreference = AttendancePreference(
            recordingDataStore("clear_runtime_attendance", "attendance.clear", calls)
        )
        val todayStatusPreference = TodayStatusPreference(
            recordingDataStore("clear_runtime_today_status", "today.clear", calls),
            Gson()
        )
        val userDao = FakeUserDao(calls)
        val geofenceRuntimeRepository = FakeGeofenceRuntimeRepository(calls)
        val useCase = ClearAuthenticatedRuntimeUseCase(
            AuthRuntimeCleanerImpl(
                userPreference = userPreference,
                userDao = userDao,
                attendancePreference = attendancePreference,
                todayStatusPreference = todayStatusPreference,
                geofenceRuntimeRepository = geofenceRuntimeRepository
            )
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
        attendancePreference.saveAttendanceSessionStateKey("active")
        todayStatusPreference.saveTodayStatusCache(sampleCachedTodayStatus())
        calls.clear()

        useCase()

        assertEquals(
            listOf("geofence.clearForLogout", "auth.clear", "profile.clear", "today.clear", "attendance.clear"),
            calls
        )
        assertEquals("", userPreference.getAuthToken().first())
        assertEquals("", userPreference.getUserId().first())
        assertEquals("", userPreference.getRefreshToken().first())
        assertEquals(0L, userPreference.getLastRefreshAt().first())
        assertEquals(0L, userPreference.getLastProfileSyncAt().first())
        assertNull(userDao.getUserProfile())
        assertNull(attendancePreference.getActiveAttendanceId().first())
        assertNull(attendancePreference.getAttendanceSessionStateKey().first())
        assertNull(todayStatusPreference.getTodayStatusCache().first())
    }

    @Test
    fun `clear runtime awaits geofence runtime cleanup before returning`() = runTest {
        val geofenceCleanupCompleted = CompletableDeferred<Boolean>()
        val calls = mutableListOf<String>()
        val useCase = ClearAuthenticatedRuntimeUseCase(
            AuthRuntimeCleanerImpl(
                userPreference = UserPreference(recordingDataStore("await_user", "auth.clear", calls)),
                userDao = FakeUserDao(calls),
                attendancePreference = AttendancePreference(
                    recordingDataStore("await_attendance", "attendance.clear", calls)
                ),
                todayStatusPreference = TodayStatusPreference(
                    recordingDataStore("await_today", "today.clear", calls),
                    Gson()
                ),
                geofenceRuntimeRepository = FakeGeofenceRuntimeRepository(
                    calls = calls,
                    onClearForLogout = {
                        delay(10)
                        geofenceCleanupCompleted.complete(true)
                    }
                )
            )
        )

        useCase()

        assertTrue(geofenceCleanupCompleted.isCompleted)
        assertEquals(true, geofenceCleanupCompleted.await())
    }

    @Test
    fun `clear runtime continues after degraded geofence and later cleanup failure`() = runTest {
        val calls = mutableListOf<String>()
        val userPreference = UserPreference(
            recordingDataStore("clear_runtime_user_failure", "auth.clear", calls)
        )
        val attendancePreference = AttendancePreference(
            recordingDataStore("clear_runtime_attendance_failure", "attendance.clear", calls)
        )
        val todayStatusPreference = TodayStatusPreference(
            recordingDataStore("clear_runtime_today_status_failure", "today.clear", calls),
            Gson()
        )
        val userDao = ThrowingUserDao(calls)
        val useCase = ClearAuthenticatedRuntimeUseCase(
            AuthRuntimeCleanerImpl(
                userPreference = userPreference,
                userDao = userDao,
                attendancePreference = attendancePreference,
                todayStatusPreference = todayStatusPreference,
                geofenceRuntimeRepository = FakeGeofenceRuntimeRepository(
                    calls = calls,
                    clearForLogoutResult = GeofenceRuntimeResult.Degraded(
                        mode = GeofenceRuntimeMode.Disabled(GeofenceDisabledReason.LOGGED_OUT),
                        failure = GeofenceRuntimeFailure.RemovalFailed("test")
                    )
                )
            )
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
        attendancePreference.saveAttendanceSessionStateKey("active")
        todayStatusPreference.saveTodayStatusCache(sampleCachedTodayStatus())
        calls.clear()

        try {
            useCase()
            fail("Expected cleanup failure")
        } catch (e: IllegalStateException) {
            assertEquals("Failed to clear authenticated runtime; completed with 2 cleanup error(s)", e.message)
            assertEquals(
                "geofenceRuntimeRepository.clearForLogout failed",
                e.cause?.message
            )
            assertEquals(
                "geofenceRuntimeRepository.clearForLogout degraded",
                e.cause?.cause?.message
            )
            assertEquals(1, e.suppressed.size)
            assertEquals("userDao.clearUserProfile failed", e.suppressed.single().message)
        }

        assertEquals(
            listOf("geofence.clearForLogout", "auth.clear", "profile.clear", "today.clear", "attendance.clear"),
            calls
        )
        assertEquals("", userPreference.getAuthToken().first())
        assertEquals("", userPreference.getUserId().first())
        assertEquals("", userPreference.getRefreshToken().first())
        assertEquals(0L, userPreference.getLastRefreshAt().first())
        assertEquals(0L, userPreference.getLastProfileSyncAt().first())
        assertNull(userDao.getUserProfile())
        assertNull(attendancePreference.getActiveAttendanceId().first())
        assertNull(attendancePreference.getAttendanceSessionStateKey().first())
        assertNull(todayStatusPreference.getTodayStatusCache().first())
    }

    private fun recordingDataStore(
        name: String,
        cleanupCall: String,
        calls: MutableList<String>
    ): DataStore<Preferences> = RecordingDataStore(createDataStore(name), cleanupCall, calls)

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

    private class RecordingDataStore(
        private val delegate: DataStore<Preferences>,
        private val cleanupCall: String,
        private val calls: MutableList<String>
    ) : DataStore<Preferences> by delegate {
        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences
        ): Preferences {
            calls += cleanupCall
            return delegate.updateData(transform)
        }
    }

    private class FakeGeofenceRuntimeRepository(
        private val calls: MutableList<String>,
        private val clearForLogoutResult: GeofenceRuntimeResult = GeofenceRuntimeResult.Applied(
            mode = GeofenceRuntimeMode.Disabled(GeofenceDisabledReason.LOGGED_OUT),
            generation = 0,
            logicalIds = emptySet()
        ),
        private val onClearForLogout: suspend () -> Unit = {}
    ) : GeofenceRuntimeRepository {
        override suspend fun reconcile(mode: GeofenceRuntimeMode): GeofenceRuntimeResult = error("Not used")

        override suspend fun clearForLogout(): GeofenceRuntimeResult {
            calls += "geofence.clearForLogout"
            onClearForLogout()
            return clearForLogoutResult
        }

        override fun observeReadiness(): Flow<GeofenceRuntimeReadiness> = emptyFlow()
    }

    private class FakeUserDao(
        private val calls: MutableList<String>
    ) : UserDao {
        private var profile: UserEntity? = null

        override suspend fun insertOrUpdateUserProfile(userEntity: UserEntity) {
            profile = userEntity
        }

        override fun getUserProfileFlow(): Flow<UserEntity?> = flowOf(profile)

        override suspend fun getUserProfile(): UserEntity? = profile

        override suspend fun clearUserProfile() {
            calls += "profile.clear"
            profile = null
        }
    }

    private class ThrowingUserDao(
        private val calls: MutableList<String>
    ) : UserDao {
        private var profile: UserEntity? = null

        override suspend fun insertOrUpdateUserProfile(userEntity: UserEntity) {
            profile = userEntity
        }

        override fun getUserProfileFlow(): Flow<UserEntity?> = flowOf(profile)

        override suspend fun getUserProfile(): UserEntity? = profile

        override suspend fun clearUserProfile() {
            calls += "profile.clear"
            profile = null
            throw IllegalStateException("user profile cleanup failed")
        }
    }
}
