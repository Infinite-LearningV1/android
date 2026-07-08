package com.example.infinite_track.domain.use_case.attendance

import com.example.infinite_track.domain.model.attendance.Location
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.wfa.WfaRecommendation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResolveSelectedTargetLocationUseCaseTest {

    private val useCase = ResolveSelectedTargetLocationUseCase()

    @Test
    fun invoke_resolvesWfoFromStatusTodayLocation() {
        val wfoLocation = location(description = "Kantor Infinite Track", category = "Office")

        val target = useCase(
            mode = WorkMode.WFO,
            wfoLocation = wfoLocation,
            wfhLocation = null,
            selectedWfaLocation = null
        )

        assertEquals(WorkMode.WFO, target.mode)
        assertEquals(wfoLocation, target.location)
        assertEquals("Kantor Infinite Track", target.displayName)
        assertTrue(target.isAvailable)
    }

    @Test
    fun invoke_blocksWfhWhenHomeLocationMissing() {
        val target = useCase(
            mode = WorkMode.WFH,
            wfoLocation = location(),
            wfhLocation = null,
            selectedWfaLocation = null
        )

        assertEquals(WorkMode.WFH, target.mode)
        assertFalse(target.isAvailable)
        assertEquals(
            "Lokasi WFH belum tersedia. Pilih WFO atau perbarui lokasi WFH terlebih dahulu.",
            target.unavailableReason
        )
    }

    @Test
    fun invoke_convertsSelectedWfaRecommendationIntoAttendanceLocation() {
        val recommendation = WfaRecommendation(
            name = "Cafe Produktif",
            address = "Jl. Merdeka",
            latitude = -0.9,
            longitude = 119.8,
            score = 0.8,
            label = "Good",
            category = "Cafe",
            distance = 1.2
        )

        val target = useCase(
            mode = WorkMode.WFA,
            wfoLocation = null,
            wfhLocation = null,
            selectedWfaLocation = recommendation
        )

        assertEquals(WorkMode.WFA, target.mode)
        assertEquals("Cafe Produktif", target.displayName)
        assertEquals("Jl. Merdeka", target.description)
        assertEquals(-0.9, target.location?.latitude ?: 0.0, 0.0)
        assertEquals(119.8, target.location?.longitude ?: 0.0, 0.0)
        assertTrue(target.isAvailable)
    }

    private fun location(
        description: String = "Target",
        category: String = "Category"
    ) = Location(
        locationId = 1,
        description = description,
        latitude = -0.8,
        longitude = 119.9,
        radius = 100,
        category = category
    )
}
