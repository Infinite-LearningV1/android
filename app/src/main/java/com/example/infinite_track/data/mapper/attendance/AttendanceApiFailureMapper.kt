package com.example.infinite_track.data.mapper.attendance

import com.example.infinite_track.domain.model.attendance.AttendanceSubmitFailure
import java.io.IOException
import java.util.Locale

/**
 * Classifies transport/backend failures into typed domain failures.
 * Message-pattern matching is a compatibility bridge until backend exposes
 * machine-readable error codes; unrecognized safe messages fall back to
 * BackendRejected(message) so backend copy is preserved for the user.
 */
object AttendanceApiFailureMapper {

    fun mapHttp(code: Int, backendMessage: String?): AttendanceSubmitFailure {
        if (code >= 500) return AttendanceSubmitFailure.ServerUnavailable

        val message = backendMessage?.trim().orEmpty()
        if (message.isEmpty()) return AttendanceSubmitFailure.Unknown

        val normalized = message.lowercase(Locale.ROOT)
        return when {
            "check-out" in normalized && "sudah" in normalized ->
                AttendanceSubmitFailure.AlreadyCheckedOut

            "sudah" in normalized && ("check-in" in normalized || "absen" in normalized) ->
                AttendanceSubmitFailure.DuplicateAttendance

            "luar radius" in normalized ->
                AttendanceSubmitFailure.OutsideAllowedRadius

            "booking" in normalized && (
                "belum disetujui" in normalized ||
                    "ditolak" in normalized ||
                    "tidak valid" in normalized
                ) ->
                AttendanceSubmitFailure.WfaBookingRejected

            "booking" in normalized && ("wajib" in normalized || "diperlukan" in normalized) ->
                AttendanceSubmitFailure.WfaBookingRequired

            else -> AttendanceSubmitFailure.BackendRejected(safeReason = message)
        }
    }

    fun mapThrowable(throwable: Throwable): AttendanceSubmitFailure = when (throwable) {
        is IOException -> AttendanceSubmitFailure.NetworkUnavailable
        else -> AttendanceSubmitFailure.Unknown
    }
}
