package com.example.infinite_track.data.mapper.booking

import com.example.infinite_track.data.soucre.network.response.booking.WfaRequestConfigDataDto
import com.example.infinite_track.data.soucre.network.response.booking.WfaRequestConfigResponseDto
import com.example.infinite_track.data.soucre.network.response.booking.WfaRequestReasonDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WfaRequestConfigMapperTest {

    @Test
    fun `maps server radius and active reasons`() {
        val dto = WfaRequestConfigResponseDto(
            success = true,
            message = null,
            data = WfaRequestConfigDataDto(
                radiusMeters = 100,
                reasons = listOf(
                    WfaRequestReasonDto(1L, "Pertemuan dengan klien", false),
                    WfaRequestReasonDto(2L, "Lainnya", true)
                )
            )
        )

        val config = requireNotNull(dto.toDomainOrNull())
        assertEquals(100, config.radiusMeters)
        assertEquals(2, config.reasons.size)
        assertTrue(config.reasons.last().isOther)
    }

    @Test
    fun `non-positive radius produces unusable config`() {
        val dto = WfaRequestConfigResponseDto(
            success = true,
            message = null,
            data = WfaRequestConfigDataDto(0, emptyList())
        )

        assertNull(dto.toDomainOrNull())
    }

    @Test
    fun `duplicate identifiers or multiple Other reasons produce unusable config`() {
        val duplicateIds = WfaRequestConfigResponseDto(
            success = true,
            message = null,
            data = WfaRequestConfigDataDto(
                radiusMeters = 100,
                reasons = listOf(
                    WfaRequestReasonDto(1L, "Satu", false),
                    WfaRequestReasonDto(1L, "Duplikat", false)
                )
            )
        )
        val multipleOther = WfaRequestConfigResponseDto(
            success = true,
            message = null,
            data = WfaRequestConfigDataDto(
                radiusMeters = 100,
                reasons = listOf(
                    WfaRequestReasonDto(1L, "Lainnya A", true),
                    WfaRequestReasonDto(2L, "Lainnya B", true)
                )
            )
        )

        assertNull(duplicateIds.toDomainOrNull())
        assertNull(multipleOther.toDomainOrNull())
    }
}
