package com.example.infinite_track.domain.use_case.booking

import com.example.infinite_track.domain.model.booking.SubmitWfaRequestCommand
import com.example.infinite_track.domain.model.booking.WfaRequestConfig
import com.example.infinite_track.domain.model.booking.WfaRequestDraft
import com.example.infinite_track.domain.model.booking.WfaRequestFieldError
import com.example.infinite_track.domain.model.booking.WfaRequestFieldErrors
import com.example.infinite_track.domain.model.booking.WfaRequestValidationResult
import javax.inject.Inject

class ValidateWfaRequestDraftUseCase @Inject constructor() {

    operator fun invoke(
        draft: WfaRequestDraft,
        config: WfaRequestConfig
    ): WfaRequestValidationResult {
        val reason = draft.reasonId?.let { id ->
            config.reasons.firstOrNull { it.id == id }
        }
        val errors = WfaRequestFieldErrors(
            scheduleDate = if (draft.scheduleDate == null) {
                WfaRequestFieldError.REQUIRED
            } else {
                null
            },
            reason = when {
                draft.reasonId == null -> WfaRequestFieldError.REQUIRED
                reason == null -> WfaRequestFieldError.REASON_UNAVAILABLE
                else -> null
            },
            otherReason = if (reason?.isOther == true && draft.otherReasonText.isBlank()) {
                WfaRequestFieldError.OTHER_REASON_REQUIRED
            } else {
                null
            },
            location = when {
                draft.location == null -> WfaRequestFieldError.REQUIRED
                !draft.location.hasValidCoordinates -> WfaRequestFieldError.INVALID_LOCATION
                else -> null
            }
        )

        if (!errors.isEmpty) {
            return WfaRequestValidationResult.Invalid(errors)
        }

        return WfaRequestValidationResult.Valid(
            SubmitWfaRequestCommand(
                scheduleDate = requireNotNull(draft.scheduleDate),
                reasonId = requireNotNull(draft.reasonId),
                otherReasonText = draft.otherReasonText.trim().ifBlank { null },
                notes = draft.notes.trim().ifBlank { null },
                location = requireNotNull(draft.location)
            )
        )
    }
}
