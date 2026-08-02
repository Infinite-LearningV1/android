package com.example.infinite_track.presentation.screen.attendance.wfa_request

import com.example.infinite_track.R
import com.example.infinite_track.domain.model.booking.WfaRequestFailure
import com.example.infinite_track.domain.model.wfa.WfaRecommendationFailure
import org.junit.Assert.assertEquals
import org.junit.Test

class WfaRequestUiMapperTest {

    @Test
    fun `recommendation failures map to typed safe resources`() {
        assertEquals(
            R.string.wfa_recommendation_failure_location,
            WfaRequestUiMapper.map(WfaRecommendationFailure.CurrentLocationUnavailable).messageRes
        )
        assertEquals(
            R.string.wfa_recommendation_failure_date,
            WfaRequestUiMapper.map(WfaRecommendationFailure.InvalidScheduleDate).messageRes
        )
        assertEquals(
            R.string.wfa_recommendation_failure_duplicate,
            WfaRequestUiMapper.map(WfaRecommendationFailure.DuplicateBooking).messageRes
        )
        assertEquals(
            R.string.wfa_recommendation_failure_network,
            WfaRequestUiMapper.map(WfaRecommendationFailure.NetworkUnavailable).messageRes
        )
        listOf(
            WfaRecommendationFailure.ProviderUnavailable,
            WfaRecommendationFailure.ServerUnavailable,
            WfaRecommendationFailure.Unknown
        ).forEach {
            assertEquals(
                R.string.wfa_recommendation_failure_unavailable,
                WfaRequestUiMapper.map(it).messageRes
            )
        }
    }

    @Test
    fun `duplicate request has correction copy`() {
        val copy = WfaRequestUiMapper.map(WfaRequestFailure.DuplicateRequest)

        assertEquals("Permintaan sudah ada", copy.title)
        assertEquals(
            "Anda sudah memiliki permintaan WFA aktif pada tanggal tersebut.",
            copy.message
        )
        assertEquals(WfaRequestFailureAction.EDIT, copy.primaryAction)
    }

    @Test
    fun `network failure is retryable`() {
        val copy = WfaRequestUiMapper.map(WfaRequestFailure.NetworkUnavailable)

        assertEquals(WfaRequestFailureAction.RETRY, copy.primaryAction)
        assertEquals("Koneksi bermasalah", copy.title)
    }

    @Test
    fun `backend rejection exposes only explicitly safe message`() {
        val copy = WfaRequestUiMapper.map(
            WfaRequestFailure.BackendRejected("Permintaan tidak dapat diproses.")
        )

        assertEquals("Permintaan ditolak", copy.title)
        assertEquals("Permintaan tidak dapat diproses.", copy.message)
        assertEquals(WfaRequestFailureAction.EDIT, copy.primaryAction)
    }
}
