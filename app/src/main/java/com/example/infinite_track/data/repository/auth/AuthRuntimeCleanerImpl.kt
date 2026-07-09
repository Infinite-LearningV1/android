package com.example.infinite_track.data.repository.auth

import com.example.infinite_track.data.soucre.local.preferences.AttendancePreference
import com.example.infinite_track.data.soucre.local.preferences.TodayStatusPreference
import com.example.infinite_track.data.soucre.local.preferences.UserPreference
import com.example.infinite_track.data.soucre.local.room.UserDao
import com.example.infinite_track.domain.repository.AuthRuntimeCleaner
import com.example.infinite_track.presentation.geofencing.GeofenceManager
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRuntimeCleanerImpl @Inject constructor(
    private val userPreference: UserPreference,
    private val userDao: UserDao,
    private val attendancePreference: AttendancePreference,
    private val todayStatusPreference: TodayStatusPreference,
    private val geofenceManager: GeofenceManager
) : AuthRuntimeCleaner {
    override suspend fun clearAuthenticatedRuntime() {
        val failures = mutableListOf<Throwable>()

        runBestEffort("userPreference.clearAuthData", failures) {
            userPreference.clearAuthData()
        }
        runBestEffort("userDao.clearUserProfile", failures) {
            userDao.clearUserProfile()
        }
        runBestEffort("todayStatusPreference.clearTodayStatusCache", failures) {
            todayStatusPreference.clearTodayStatusCache()
        }
        runBestEffort("attendancePreference.clearAttendanceRuntimeState", failures) {
            attendancePreference.clearAttendanceRuntimeState()
        }
        runBestEffort("geofenceManager.removeAllGeofencesForLogoutOnlyAwait", failures) {
            geofenceManager.removeAllGeofencesForLogoutOnlyAwait()
        }

        if (failures.isNotEmpty()) {
            throw IllegalStateException(
                "Failed to clear authenticated runtime; completed with ${failures.size} cleanup error(s)",
                failures.first()
            ).apply {
                failures.drop(1).forEach(::addSuppressed)
            }
        }
    }

    private suspend inline fun runBestEffort(
        stepName: String,
        failures: MutableList<Throwable>,
        block: suspend () -> Unit
    ) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            failures += IllegalStateException("$stepName failed", e)
        }
    }
}
