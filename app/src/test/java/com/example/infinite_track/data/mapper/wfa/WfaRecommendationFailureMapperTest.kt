package com.example.infinite_track.data.mapper.wfa

import com.example.infinite_track.domain.model.wfa.WfaRecommendationFailure
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class WfaRecommendationFailureMapperTest {

    @Test
    fun `date and duplicate codes map to typed failures`() {
        assertEquals(
            WfaRecommendationFailure.InvalidScheduleDate,
            WfaRecommendationFailureMapper.mapHttp(400, "SAME_DAY_NOT_ALLOWED")
        )
        assertEquals(
            WfaRecommendationFailure.DuplicateBooking,
            WfaRecommendationFailureMapper.mapHttp(409, "DUPLICATE_BOOKING")
        )
    }

    @Test
    fun `provider and server failures remain distinct`() {
        assertEquals(
            WfaRecommendationFailure.ProviderUnavailable,
            WfaRecommendationFailureMapper.mapHttp(503, "PLACES_PROVIDER_UNAVAILABLE")
        )
        assertEquals(
            WfaRecommendationFailure.ServerUnavailable,
            WfaRecommendationFailureMapper.mapHttp(500, null)
        )
        assertEquals(
            WfaRecommendationFailure.Unknown,
            WfaRecommendationFailureMapper.mapHttp(418, "UNRECOGNIZED")
        )
    }

    @Test
    fun `io exception maps to network unavailable`() {
        assertEquals(
            WfaRecommendationFailure.NetworkUnavailable,
            WfaRecommendationFailureMapper.mapThrowable(IOException("offline"))
        )
        assertEquals(
            WfaRecommendationFailure.Unknown,
            WfaRecommendationFailureMapper.mapThrowable(IllegalStateException("broken"))
        )
    }
}
