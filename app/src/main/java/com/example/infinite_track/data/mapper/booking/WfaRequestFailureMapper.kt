package com.example.infinite_track.data.mapper.booking

import com.example.infinite_track.domain.model.booking.WfaRequestFailure
import java.io.IOException

object WfaRequestFailureMapper {
    fun mapHttp(
        statusCode: Int,
        code: String?,
        safeMessage: String?,
        fieldErrors: Map<String, String>
    ): WfaRequestFailure = when {
        code == "DUPLICATE_BOOKING" -> WfaRequestFailure.DuplicateRequest
        code in setOf(
            "INVALID_DATE_FORMAT",
            "INVALID_DATE_VALUE",
            "PAST_DATE_NOT_ALLOWED",
            "SAME_DAY_NOT_ALLOWED"
        ) -> WfaRequestFailure.InvalidDate
        code in setOf("REQUEST_REASON_NOT_FOUND", "REQUEST_REASON_INACTIVE") ->
            WfaRequestFailure.ReasonUnavailable
        fieldErrors.isNotEmpty() -> WfaRequestFailure.ValidationRejected(fieldErrors)
        statusCode >= 500 -> WfaRequestFailure.ServerUnavailable
        else -> WfaRequestFailure.Unknown
    }

    fun mapThrowable(throwable: Throwable): WfaRequestFailure =
        if (throwable is IOException) WfaRequestFailure.NetworkUnavailable
        else WfaRequestFailure.Unknown
}
