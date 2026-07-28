package com.example.infinite_track.presentation.screen.attendance.wfa_request

import com.example.infinite_track.domain.model.booking.WfaRequestFailure
import org.junit.Assert.assertEquals
import org.junit.Test

class WfaRequestUiMapperTest {

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
