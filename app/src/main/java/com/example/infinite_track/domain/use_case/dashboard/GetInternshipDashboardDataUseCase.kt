package com.example.infinite_track.domain.use_case.dashboard

import com.example.infinite_track.domain.model.attendance.AttendancePeriod
import com.example.infinite_track.domain.model.dashboard.InternshipSummary
import com.example.infinite_track.domain.repository.AttendanceHistoryRepository
import com.example.infinite_track.domain.repository.AttendanceRepository
import javax.inject.Inject

/**
 * UseCase that orchestrates and combines data from multiple repositories
 * to create the Internship Dashboard summary data.
 */
class GetInternshipDashboardDataUseCase @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val attendanceHistoryRepository: AttendanceHistoryRepository
) {
    /**
     * Invoke function that combines data from today's status and monthly attendance history
     * to create a single InternshipSummary object.
     *
     * @return Result containing InternshipSummary if successful
     */
    suspend operator fun invoke(): Result<InternshipSummary> {
        return try {
            // Get today's attendance status
            val todayStatusResult = attendanceRepository.getTodayStatus()

            // Get monthly attendance history
            val historyResult = attendanceHistoryRepository.getAttendanceHistory(
                period = AttendancePeriod.MONTHLY,
                page = 1,
                limit = 30 // Assuming we need about a month of data
            )

            // If either API call fails, return the error
            if (todayStatusResult.isFailure) {
                return Result.failure(todayStatusResult.exceptionOrNull() ?: Exception("Failed to fetch today's status"))
            }

            if (historyResult.isFailure) {
                return Result.failure(historyResult.exceptionOrNull() ?: Exception("Failed to fetch attendance history"))
            }

            // Extract data from successful results
            val todayStatus = todayStatusResult.getOrThrow()
            val historyPage = historyResult.getOrThrow()

            // Create InternshipSummary from combined data
            val internshipSummary = InternshipSummary(
                checkedInTime = todayStatus.checkedInAt,
                checkedOutTime = todayStatus.checkedOutAt, // Need to determine if there's a checked out time in today's status
                totalAbsence = historyPage.summary.totalAlpha,
                totalAttended = historyPage.records.size // Count of attendance records
            )

            Result.success(internshipSummary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
