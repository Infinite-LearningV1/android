package com.example.infinite_track.domain.use_case.attendance

import com.example.infinite_track.domain.model.attendance.AttendanceRequestModel
import com.example.infinite_track.domain.model.attendance.AttendanceSubmitCommand
import com.example.infinite_track.domain.model.attendance.AttendanceSubmitFailure
import com.example.infinite_track.domain.model.attendance.AttendanceSubmitResult
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.location.CurrentLocationResult
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.repository.AttendanceRepository
import com.example.infinite_track.domain.use_case.auth.GetLoggedInUserUseCase
import com.example.infinite_track.domain.use_case.location.GetCurrentLocationUseCase
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * Single Layer 5 orchestrator: validates the submit command, captures fresh GPS
 * once, resolves the checkout attendance id per fallback policy, and performs
 * exactly one repository mutation per attempt. Backend remains the final
 * attendance authority.
 */
class SubmitAttendanceUseCase @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val getCurrentLocationUseCase: GetCurrentLocationUseCase,
    private val getLoggedInUserUseCase: GetLoggedInUserUseCase
) {
    suspend operator fun invoke(command: AttendanceSubmitCommand): AttendanceSubmitResult {
        getLoggedInUserUseCase().firstOrNull()
            ?: return AttendanceSubmitResult.Failure(
                command.intent,
                AttendanceSubmitFailure.SessionUnavailable
            )

        return when (command) {
            is AttendanceSubmitCommand.CheckIn -> submitCheckIn(command)
            is AttendanceSubmitCommand.CheckOut -> submitCheckOut(command)
        }
    }

    private suspend fun submitCheckIn(
        command: AttendanceSubmitCommand.CheckIn
    ): AttendanceSubmitResult {
        if (command.authoritativeTarget.mode != command.workMode) {
            return AttendanceSubmitResult.Failure(
                command.intent,
                AttendanceSubmitFailure.TargetModeMismatch
            )
        }
        val bookingId = command.authoritativeTarget.approvedWfaContext?.bookingId
        if (command.workMode == WorkMode.WFA && bookingId == null) {
            return AttendanceSubmitResult.Failure(
                command.intent,
                AttendanceSubmitFailure.WfaBookingRequired
            )
        }

        val coordinate = freshCoordinate()
            ?: return AttendanceSubmitResult.Failure(
                command.intent,
                AttendanceSubmitFailure.CurrentLocationUnavailable
            )

        val request = AttendanceRequestModel(
            categoryId = command.workMode.categoryId,
            latitude = coordinate.latitude,
            longitude = coordinate.longitude,
            notes = CHECK_IN_NOTES,
            bookingId = bookingId.takeIf { command.workMode == WorkMode.WFA },
            type = REQUEST_TYPE_CHECK_IN
        )
        return attendanceRepository.checkIn(request)
    }

    private suspend fun submitCheckOut(
        command: AttendanceSubmitCommand.CheckOut
    ): AttendanceSubmitResult {
        val attendanceId = command.activeAttendanceId?.takeIf { it > 0 }
            ?: attendanceRepository.getTodayStatus(forceRefresh = true)
                .getOrNull()?.activeAttendanceId?.takeIf { it > 0 }
            ?: attendanceRepository.getActiveAttendanceId()?.takeIf { it > 0 }
            ?: return AttendanceSubmitResult.Failure(
                command.intent,
                AttendanceSubmitFailure.ActiveAttendanceUnavailable
            )

        val coordinate = freshCoordinate()
            ?: return AttendanceSubmitResult.Failure(
                command.intent,
                AttendanceSubmitFailure.CurrentLocationUnavailable
            )

        return attendanceRepository.checkOut(
            attendanceId = attendanceId,
            latitude = coordinate.latitude,
            longitude = coordinate.longitude
        )
    }

    private suspend fun freshCoordinate(): GeoCoordinate? =
        (getCurrentLocationUseCase() as? CurrentLocationResult.Success)
            ?.location?.coordinate

    private companion object {
        const val CHECK_IN_NOTES = "Check-in via mobile app"
        const val REQUEST_TYPE_CHECK_IN = "checkin"
    }
}
