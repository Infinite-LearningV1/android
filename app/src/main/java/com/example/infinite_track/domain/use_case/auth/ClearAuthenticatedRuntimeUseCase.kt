package com.example.infinite_track.domain.use_case.auth

import com.example.infinite_track.data.soucre.local.preferences.AttendancePreference
import com.example.infinite_track.data.soucre.local.preferences.TodayStatusPreference
import com.example.infinite_track.data.soucre.local.preferences.UserPreference
import com.example.infinite_track.data.soucre.local.room.UserDao
import com.example.infinite_track.presentation.geofencing.GeofenceManager

class ClearAuthenticatedRuntimeUseCase internal constructor(
    private val userPreference: UserPreference,
    private val userDao: UserDao,
    private val attendancePreference: AttendancePreference,
    private val todayStatusPreference: TodayStatusPreference,
    private val removeAllGeofences: () -> Unit
) {
    constructor(
        userPreference: UserPreference,
        userDao: UserDao,
        attendancePreference: AttendancePreference,
        todayStatusPreference: TodayStatusPreference,
        geofenceManager: GeofenceManager
    ) : this(
        userPreference = userPreference,
        userDao = userDao,
        attendancePreference = attendancePreference,
        todayStatusPreference = todayStatusPreference,
        removeAllGeofences = geofenceManager::removeAllGeofences
    )

    suspend operator fun invoke() {
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
        runBestEffort("removeAllGeofences", failures) {
            removeAllGeofences()
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

    private inline fun runBestEffort(
        stepName: String,
        failures: MutableList<Throwable>,
        block: () -> Unit
    ) {
        try {
            block()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            failures += IllegalStateException("$stepName failed", e)
        }
    }
}
