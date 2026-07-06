package com.example.infinite_track.domain.use_case.auth

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.infinite_track.data.soucre.local.preferences.AttendancePreference
import com.example.infinite_track.data.soucre.local.preferences.TodayStatusPreference
import com.example.infinite_track.data.soucre.local.preferences.UserPreference
import com.example.infinite_track.data.soucre.local.room.UserDao
import com.example.infinite_track.data.soucre.local.room.UserEntity
import com.example.infinite_track.domain.manager.SessionManager
import com.google.gson.Gson
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class ForceReauthUseCaseTest {

    @Test
    fun `force reauth clears local runtime and never calls remote logout`() = runTest {
        var clearRuntimeCalls = 0
        val clearRuntime = createClearRuntimeUseCase { clearRuntimeCalls += 1 }
        val sessionManager = SessionManager()
        val useCase = ForceReauthUseCase(sessionManager, clearRuntime)

        useCase(SessionManager.ReauthReason.REFRESH_INVALID)

        assertEquals(1, clearRuntimeCalls)
        assertEquals(true, sessionManager.sessionExpired.value)
        assertEquals(SessionManager.ReauthReason.REFRESH_INVALID, sessionManager.reauthReason.value)
    }

    @Test
    fun `force reauth is single flight until session expiry is reset`() = runTest {
        var clearRuntimeCalls = 0
        val clearRuntime = createClearRuntimeUseCase { clearRuntimeCalls += 1 }
        val sessionManager = SessionManager()
        val useCase = ForceReauthUseCase(sessionManager, clearRuntime)

        useCase(SessionManager.ReauthReason.REFRESH_INVALID)
        useCase(SessionManager.ReauthReason.REFRESH_REVOKED)

        assertEquals(1, clearRuntimeCalls)
        assertEquals(SessionManager.ReauthReason.REFRESH_INVALID, sessionManager.reauthReason.value)
    }

    @Test
    fun `force reauth still publishes terminal state when cleanup fails`() = runTest {
        var clearRuntimeCalls = 0
        val sessionManager = SessionManager()
        val useCase = ForceReauthUseCase(sessionManager) {
            clearRuntimeCalls += 1
            error("cleanup failed")
        }

        useCase(SessionManager.ReauthReason.REFRESH_REVOKED)

        assertEquals(1, clearRuntimeCalls)
        assertEquals(true, sessionManager.sessionExpired.value)
        assertEquals(SessionManager.ReauthReason.REFRESH_REVOKED, sessionManager.reauthReason.value)
    }

    @Test
    fun `force reauth still publishes terminal state before rethrowing cancellation`() = runTest {
        val sessionManager = SessionManager()
        val cancellation = CancellationException("cancelled")
        val useCase = ForceReauthUseCase(sessionManager) {
            throw cancellation
        }

        try {
            useCase(SessionManager.ReauthReason.REFRESH_INVALID)
            fail("Expected cancellation")
        } catch (e: CancellationException) {
            assertEquals(cancellation, e)
        }

        assertEquals(true, sessionManager.sessionExpired.value)
        assertEquals(SessionManager.ReauthReason.REFRESH_INVALID, sessionManager.reauthReason.value)
    }

    private fun createClearRuntimeUseCase(onRemoveAllGeofences: () -> Unit): ClearAuthenticatedRuntimeUseCase {
        return ClearAuthenticatedRuntimeUseCase(
            userPreference = UserPreference(createDataStore("force_reauth_user")),
            userDao = FakeUserDao(),
            attendancePreference = AttendancePreference(createDataStore("force_reauth_attendance")),
            todayStatusPreference = TodayStatusPreference(createDataStore("force_reauth_today_status"), Gson()),
            removeAllGeofences = onRemoveAllGeofences
        )
    }

    private fun createDataStore(name: String) = PreferenceDataStoreFactory.create(
        produceFile = { File.createTempFile(name, ".preferences_pb").also { it.delete() } }
    )

    private class FakeUserDao : UserDao {
        override suspend fun insertOrUpdateUserProfile(userEntity: UserEntity) = Unit
        override fun getUserProfileFlow(): Flow<UserEntity?> = flowOf(null)
        override suspend fun getUserProfile(): UserEntity? = null
        override suspend fun clearUserProfile() = Unit
    }
}
