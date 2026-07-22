package com.example.infinite_track.domain.use_case.attendance.permission

import com.example.infinite_track.domain.model.attendance.permission.AttendancePermissionReadiness
import com.example.infinite_track.domain.repository.AttendancePermissionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAttendancePermissionReadinessUseCase @Inject constructor(
    private val repository: AttendancePermissionRepository
) {
    operator fun invoke(): Flow<AttendancePermissionReadiness> = repository.observeReadiness()
}
