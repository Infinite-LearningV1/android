package com.example.infinite_track.domain.model.booking

import java.time.LocalDate

data class WfaRequestDraft(
    val scheduleDate: LocalDate?,
    val reasonId: Long?,
    val otherReasonText: String,
    val notes: String,
    val location: WfaCandidateLocation?
) {
    companion object {
        val Empty = WfaRequestDraft(null, null, "", "", null)
    }
}

enum class WfaRequestFieldError {
    REQUIRED,
    REASON_UNAVAILABLE,
    OTHER_REASON_REQUIRED,
    TOO_LONG,
    INVALID_LOCATION
}

data class WfaRequestFieldErrors(
    val scheduleDate: WfaRequestFieldError? = null,
    val reason: WfaRequestFieldError? = null,
    val otherReason: WfaRequestFieldError? = null,
    val notes: WfaRequestFieldError? = null,
    val location: WfaRequestFieldError? = null
) {
    val isEmpty: Boolean
        get() = scheduleDate == null && reason == null && otherReason == null &&
            notes == null && location == null
}
