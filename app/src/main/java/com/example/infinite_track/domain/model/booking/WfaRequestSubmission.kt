package com.example.infinite_track.domain.model.booking

import java.time.Instant
import java.time.LocalDate

data class SubmitWfaRequestCommand(
    val scheduleDate: LocalDate,
    val reasonId: Long,
    val otherReasonText: String?,
    val notes: String?,
    val location: WfaCandidateLocation
)

enum class WfaRequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    UNKNOWN
}

data class SubmittedWfaRequest(
    val bookingId: Long,
    val scheduleDate: LocalDate,
    val status: WfaRequestStatus,
    val location: WfaCandidateLocation,
    val reasonLabel: String,
    val radiusMeters: Int,
    val submittedAt: Instant?
)

sealed interface WfaRequestFailure {
    data object BootstrapUnavailable : WfaRequestFailure
    data object ConfigUnavailable : WfaRequestFailure
    data object InvalidDate : WfaRequestFailure
    data object ReasonUnavailable : WfaRequestFailure
    data object DuplicateRequest : WfaRequestFailure
    data object NetworkUnavailable : WfaRequestFailure
    data object ServerUnavailable : WfaRequestFailure
    data class ValidationRejected(val fieldErrors: Map<String, String>) : WfaRequestFailure
    data class BackendRejected(val safeMessage: String) : WfaRequestFailure
    data object Unknown : WfaRequestFailure
}

sealed interface WfaRequestConfigResult {
    data class Success(val config: WfaRequestConfig) : WfaRequestConfigResult
    data class Failure(val failure: WfaRequestFailure) : WfaRequestConfigResult
}

sealed interface WfaRequestResult {
    data class Success(val request: SubmittedWfaRequest) : WfaRequestResult
    data class Failure(val failure: WfaRequestFailure) : WfaRequestResult
}

sealed interface WfaRequestValidationResult {
    data class Valid(val command: SubmitWfaRequestCommand) : WfaRequestValidationResult
    data class Invalid(val errors: WfaRequestFieldErrors) : WfaRequestValidationResult
}
