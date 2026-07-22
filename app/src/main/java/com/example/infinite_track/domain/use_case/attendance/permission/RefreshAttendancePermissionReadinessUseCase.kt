package com.example.infinite_track.domain.use_case.attendance.permission

import com.example.infinite_track.domain.repository.AttendancePermissionRepository
import javax.inject.Inject

class RefreshAttendancePermissionReadinessUseCase @Inject constructor(
    private val repository: AttendancePermissionRepository
) {
    suspend operator fun invoke() {
        repository.refreshReadiness()
    }
}
