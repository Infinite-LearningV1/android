package com.example.infinite_track.domain.repository

import com.example.infinite_track.domain.model.attendance.permission.AttendancePermissionReadiness
import kotlinx.coroutines.flow.Flow

interface AttendancePermissionRepository {
    fun observeReadiness(): Flow<AttendancePermissionReadiness>

    suspend fun refreshReadiness()
}
