package com.example.infinite_track.domain.use_case.attendance

import com.example.infinite_track.domain.model.attendance.Location
import com.example.infinite_track.domain.model.attendance.WorkMode
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
            approvedWfaLocation = null
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
            approvedWfaLocation = null
        )

        assertEquals(WorkMode.WFH, target.mode)
        assertFalse(target.isAvailable)
        assertEquals(
            "Lokasi WFH belum tersedia. Pilih WFO atau perbarui lokasi WFH terlebih dahulu.",
            target.unavailableReason
        )
    }

    @Test
    fun invoke_usesApprovedWfaLocationAsAttendanceAuthority() {
        val approvedLocation = location(
            description = "Cafe Produktif",
            category = "WFA"
        )

        val target = useCase(
            mode = WorkMode.WFA,
            wfoLocation = null,
            wfhLocation = null,
            approvedWfaLocation = approvedLocation
        )

        assertEquals(WorkMode.WFA, target.mode)
        assertEquals("Cafe Produktif", target.displayName)
        assertEquals("WFA", target.description)
        assertEquals(approvedLocation, target.location)
        assertTrue(target.isAvailable)
    }

    @Test
    fun invoke_blocksWfaWhenApprovedBookingLocationIsMissing() {
        val target = useCase(
            mode = WorkMode.WFA,
            wfoLocation = null,
            wfhLocation = null,
            approvedWfaLocation = null
        )

        assertFalse(target.isAvailable)
        assertEquals(
            "Booking WFA yang disetujui untuk hari ini belum tersedia.",
            target.unavailableReason
        )
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
