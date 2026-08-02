package com.example.infinite_track.data.mapper.wfa

import com.example.infinite_track.data.soucre.network.response.FacilityEvidenceDto
import com.example.infinite_track.data.soucre.network.response.RecommendationItem
import com.example.infinite_track.domain.model.wfa.WfaFacilityAvailability
import com.example.infinite_track.domain.model.wfa.WfaRecommendationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WfaMapperTest {

    @Test
    fun `ranked candidate maps score and tri-state facilities`() {
        val result = rankedDto().toDomainOrNull()

        requireNotNull(result)
        assertEquals(WfaRecommendationStatus.Ranked, result.status)
        assertEquals(82.4, result.finalScore!!, 0.0)
        assertEquals("Sangat Tinggi", result.finalLabel)
        assertEquals(WfaFacilityAvailability.AVAILABLE, result.facilities.internetAccess)
        assertEquals(WfaFacilityAvailability.UNAVAILABLE, result.facilities.toilets)
        assertEquals(
            WfaFacilityAvailability.UNKNOWN,
            result.facilities.wheelchairAccessibility
        )
    }

    @Test
    fun `insufficient candidate preserves null final score`() {
        val result = rankedDto().copy(
            status = "insufficient_facility_data",
            finalRank = null,
            finalScore = null,
            finalLabel = null
        ).toDomainOrNull()

        requireNotNull(result)
        assertEquals(WfaRecommendationStatus.InsufficientFacilityData, result.status)
        assertNull(result.finalRank)
        assertNull(result.finalScore)
        assertNull(result.finalLabel)
    }

    @Test
    fun `facility enrichment failure remains unscored`() {
        val result = rankedDto().copy(status = "facility_enrichment_failed").toDomainOrNull()

        requireNotNull(result)
        assertEquals(WfaRecommendationStatus.FacilityEnrichmentFailed, result.status)
        assertNull(result.finalRank)
        assertNull(result.finalScore)
        assertNull(result.finalLabel)
    }

    @Test
    fun `stable key prefers trimmed place id`() {
        val result = rankedDto(placeId = " place-123 ").toDomainOrNull()

        assertEquals("place:place-123", requireNotNull(result).stableKey)
        assertEquals("place-123", result.placeId)
    }

    @Test
    fun `unknown status is explicit and remains unscored`() {
        val result = rankedDto().copy(status = "future_status").toDomainOrNull()

        requireNotNull(result)
        assertEquals(WfaRecommendationStatus.Unsupported("future_status"), result.status)
        assertNull(result.finalScore)
        assertNull(result.finalLabel)
    }

    @Test
    fun `ranked candidate missing final score or label becomes unsupported`() {
        listOf(
            rankedDto().copy(finalScore = null),
            rankedDto().copy(finalLabel = "  ")
        ).forEach { dto ->
            val result = requireNotNull(dto.toDomainOrNull())
            assertEquals(
                WfaRecommendationStatus.Unsupported("ranked_missing_final_score"),
                result.status
            )
            assertNull(result.finalRank)
            assertNull(result.finalScore)
            assertNull(result.finalLabel)
        }
    }

    @Test
    fun `facility confidence outside zero to one hundred rejects candidate`() {
        assertNull(rankedDto().copy(facilityConfidence = -1).toDomainOrNull())
        assertNull(rankedDto().copy(facilityConfidence = 101).toDomainOrNull())
    }

    @Test
    fun `missing place id falls back to normalized name and coordinates`() {
        val result = rankedDto(placeId = " ").toDomainOrNull()

        assertEquals("cafe@-0.900000,119.880000", requireNotNull(result).stableKey)
        assertTrue(result.distanceMeters.value == 1250.0)
    }

    private fun rankedDto(placeId: String? = "place-1") = RecommendationItem(
        placeId = placeId,
        name = " Cafe ",
        address = "Palu",
        latitude = -0.9,
        longitude = 119.88,
        distanceMeters = 1250.0,
        placeType = "cafe",
        status = "ranked",
        finalRank = 1,
        finalScore = 82.4,
        finalLabel = "Sangat Tinggi",
        facilityScore = 75.0,
        facilityConfidence = 80,
        facilities = FacilityEvidenceDto(
            internetAccess = true,
            openingHours = true,
            toilets = false,
            airConditioning = null,
            wheelchairAccessibility = null
        )
    )
}
