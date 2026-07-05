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
        userPreference.clearAuthData()
        userDao.clearUserProfile()
        todayStatusPreference.clearTodayStatusCache()
        attendancePreference.clearAttendanceRuntimeState()
        removeAllGeofences()
    }
}
