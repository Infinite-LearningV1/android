package com.example.infinite_track.presentation.screen.attendance.preparation

import com.example.infinite_track.domain.model.attendance.AttendancePreparationBlockReason
import com.example.infinite_track.domain.model.attendance.AttendancePreparationEligibility
import com.example.infinite_track.domain.model.attendance.AttendancePreparationRecovery
import com.example.infinite_track.domain.model.attendance.AuthoritativeTargetLocation
import com.example.infinite_track.domain.model.attendance.TargetLocationId
import com.example.infinite_track.domain.model.attendance.TargetLocationResolution
import com.example.infinite_track.domain.model.attendance.TargetLocationSource
import com.example.infinite_track.domain.model.attendance.TargetRangeStatus
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class AttendancePreparationUiMapperTest {

    @Test
    fun `resolving state exposes disabled wait action`() {
        val mapped = AttendancePreparationUiMapper.map(
            AttendancePreparationState(selectedMode = WorkMode.WFA),
            strings
        )

        assertEquals(AttendancePreparationPrimaryAction.WAIT, mapped.primaryAction)
        assertFalse(mapped.isPrimaryActionEnabled)
        assertEquals(3, mapped.modeOptions.size)
        assertEquals(1, mapped.modeOptions.count { it.isSelected })
    }

    @Test
    fun `not requested state waits while navigation policy owns auto open`() {
        val mapped = AttendancePreparationUiMapper.map(
            blocked(
                AttendancePreparationBlockReason.WFA_NOT_REQUESTED,
                AttendancePreparationRecovery.OPEN_WFA_BOOKING
            ),
            strings
        )

        assertEquals(AttendancePreparationPrimaryAction.WAIT, mapped.primaryAction)
        assertFalse(mapped.isPrimaryActionEnabled)
    }

    @Test
    fun `pending rejected and missing-date states preserve request-status recovery`() {
        listOf(
            AttendancePreparationBlockReason.WFA_PENDING,
            AttendancePreparationBlockReason.WFA_REJECTED,
            AttendancePreparationBlockReason.WFA_APPROVAL_MISSING_FOR_DATE
        ).forEach { reason ->
            val mapped = AttendancePreparationUiMapper.map(
                blocked(reason, AttendancePreparationRecovery.OPEN_WFA_REQUESTS),
                strings
            )

            assertEquals(AttendancePreparationPrimaryAction.OPEN_WFA_REQUESTS, mapped.primaryAction)
            assertTrue(mapped.isPrimaryActionEnabled)
        }
    }

    @Test
    fun `approved WFA target exposes authoritative booking summary`() {
        val range = TargetRangeStatus.Inside(DistanceMeters(25.0))
        val mapped = AttendancePreparationUiMapper.map(
            AttendancePreparationState(
                selectedMode = WorkMode.WFA,
                targetResolution = TargetLocationResolution.Resolved(approvedTarget),
                rangeStatus = range,
                eligibility = AttendancePreparationEligibility.Ready(approvedTarget, range)
            ),
            strings
        )

        assertEquals(
            AttendancePreparationPrimaryAction.CONTINUE_TO_FACE_VERIFICATION,
            mapped.primaryAction
        )
        assertTrue(mapped.isPrimaryActionEnabled)
        assertNotNull(mapped.targetSummary)
        assertEquals("WFA disetujui", mapped.targetSummary?.displayName)
    }

    private fun blocked(
        reason: AttendancePreparationBlockReason,
        recovery: AttendancePreparationRecovery
    ) = AttendancePreparationState(
        selectedMode = WorkMode.WFA,
        targetResolution = TargetLocationResolution.Resolving(WorkMode.WFA),
        eligibility = AttendancePreparationEligibility.Blocked(reason, recovery)
    )

    private val strings = object : AttendancePreparationTextResolver {
        override val locale: Locale = Locale.US

        override fun text(resourceId: Int, vararg formatArgs: Any): String =
            buildString {
                append(resourceId)
                formatArgs.forEach { append(':').append(it) }
            }
    }

    private val approvedTarget = AuthoritativeTargetLocation(
        targetId = TargetLocationId("wfa:booking:42"),
        mode = WorkMode.WFA,
        source = TargetLocationSource.APPROVED_WFA_BOOKING,
        coordinate = GeoCoordinate(-0.88, 119.86),
        radius = DistanceMeters(100.0),
        displayName = "WFA disetujui"
    )
}
