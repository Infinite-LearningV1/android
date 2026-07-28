package com.example.infinite_track.data.mapper.booking

import com.example.infinite_track.domain.model.booking.WfaRequestFailure
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class WfaRequestFailureMapperTest {

    @Test
    fun `duplicate code maps to DuplicateRequest`() {
        assertEquals(
            WfaRequestFailure.DuplicateRequest,
            WfaRequestFailureMapper.mapHttp(409, "DUPLICATE_BOOKING", null, emptyMap())
        )
    }

    @Test
    fun `inactive reason maps to ReasonUnavailable`() {
        assertEquals(
            WfaRequestFailure.ReasonUnavailable,
            WfaRequestFailureMapper.mapHttp(400, "REQUEST_REASON_INACTIVE", null, emptyMap())
        )
    }

    @Test
    fun `io failure maps to NetworkUnavailable`() {
        assertEquals(
            WfaRequestFailure.NetworkUnavailable,
            WfaRequestFailureMapper.mapThrowable(IOException("offline"))
        )
    }

    @Test
    fun `server error maps to ServerUnavailable`() {
        assertEquals(
            WfaRequestFailure.ServerUnavailable,
            WfaRequestFailureMapper.mapHttp(503, null, null, emptyMap())
        )
    }
}
