package com.example.infinite_track.domain.use_case.attendance

import com.example.infinite_track.domain.model.attendance.TodayStatus
import com.example.infinite_track.domain.repository.AttendanceRepository
import javax.inject.Inject

/**
 * Use case for getting today's attendance status
 */
class GetTodayStatusUseCase @Inject constructor(
    private val attendanceRepository: AttendanceRepository
) {
    suspend operator fun invoke(forceRefresh: Boolean = false): Result<TodayStatus> {
        return attendanceRepository.getTodayStatus(forceRefresh)
    }
}
