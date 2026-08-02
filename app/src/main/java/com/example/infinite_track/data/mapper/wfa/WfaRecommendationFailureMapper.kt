package com.example.infinite_track.data.mapper.wfa

import com.example.infinite_track.domain.model.wfa.WfaRecommendationFailure
import java.io.IOException
import java.util.Locale

object WfaRecommendationFailureMapper {
    fun mapHttp(statusCode: Int, code: String?): WfaRecommendationFailure {
        val normalizedCode = code?.trim()?.uppercase(Locale.ROOT)
        return when {
            normalizedCode in DATE_CODES -> WfaRecommendationFailure.InvalidScheduleDate
            normalizedCode == "DUPLICATE_BOOKING" ->
                WfaRecommendationFailure.DuplicateBooking
            normalizedCode in PROVIDER_CODES -> WfaRecommendationFailure.ProviderUnavailable
            statusCode >= 500 -> WfaRecommendationFailure.ServerUnavailable
            else -> WfaRecommendationFailure.Unknown
        }
    }

    fun mapThrowable(throwable: Throwable): WfaRecommendationFailure =
        if (throwable.causeSequence().any { it is IOException }) {
            WfaRecommendationFailure.NetworkUnavailable
        } else {
            WfaRecommendationFailure.Unknown
        }

    private fun Throwable.causeSequence(): Sequence<Throwable> = generateSequence(this) { cause ->
        cause.cause?.takeUnless { it === cause }
    }

    private val DATE_CODES = setOf(
        "INVALID_SCHEDULE_DATE",
        "SCHEDULE_DATE_REQUIRED",
        "PAST_DATE_NOT_ALLOWED",
        "SAME_DAY_NOT_ALLOWED",
        "FUTURE_DATE_REQUIRED"
    )

    private val PROVIDER_CODES = setOf(
        "PROVIDER_UNAVAILABLE",
        "PROVIDER_CONFIG_ERROR",
        "PLACES_PROVIDER_UNAVAILABLE",
        "PLACES_CONFIG_ERROR",
        "MAPS_CONFIGURATION_ERROR"
    )
}
