package com.example.infinite_track.presentation.screen.attendance.preparation

import com.example.infinite_track.presentation.screen.attendance.AttendanceActionIntent
import com.example.infinite_track.presentation.screen.attendance.AttendanceActionState
import com.example.infinite_track.presentation.screen.attendance.AttendanceBlockReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendancePrimaryActionUiCombinerTest {

    @Test
    fun `ready checkout always wins over unresolved or blocked preparation`() {
        listOf(
            preparation(
                action = AttendancePreparationPrimaryAction.WAIT,
                label = "Menyiapkan lokasi...",
                enabled = false
            ),
            preparation(
                action = AttendancePreparationPrimaryAction.REFRESH_PROFILE,
                label = "Muat ulang",
                enabled = true
            )
        ).forEach { preparation ->
            val result = AttendancePrimaryActionUiCombiner.combine(
                preparation = preparation,
                actionState = AttendanceActionState.Ready(
                    intent = AttendanceActionIntent.CHECK_OUT,
                    label = "Check-out di sini"
                )
            )

            assertEquals(
                AttendancePreparationPrimaryAction.SUBMIT_ATTENDANCE,
                result.primaryAction
            )
            assertEquals("Check-out di sini", result.primaryActionLabel)
            assertTrue(result.isPrimaryActionEnabled)
        }
    }

    @Test
    fun `ready checkin uses attendance CTA when preparation is ready`() {
        val result = AttendancePrimaryActionUiCombiner.combine(
            preparation = preparation(
                action = AttendancePreparationPrimaryAction.CONTINUE_TO_FACE_VERIFICATION,
                label = "Lanjut ke Verifikasi Wajah",
                enabled = true
            ),
            actionState = AttendanceActionState.Ready(
                intent = AttendanceActionIntent.CHECK_IN,
                label = "Check-in di sini"
            )
        )

        assertEquals(AttendancePreparationPrimaryAction.SUBMIT_ATTENDANCE, result.primaryAction)
        assertEquals("Check-in di sini", result.primaryActionLabel)
        assertTrue(result.isPrimaryActionEnabled)
    }

    @Test
    fun `blocked attendance preserves the typed preparation recovery CTA`() {
        val recoveryActions = listOf(
            AttendancePreparationPrimaryAction.OPEN_WFA_BOOKING,
            AttendancePreparationPrimaryAction.OPEN_WFA_REQUESTS,
            AttendancePreparationPrimaryAction.REFRESH_STATUS,
            AttendancePreparationPrimaryAction.REFRESH_PROFILE
        )

        recoveryActions.forEach { recoveryAction ->
            val result = AttendancePrimaryActionUiCombiner.combine(
                preparation = preparation(
                    action = recoveryAction,
                    label = recoveryAction.name,
                    enabled = true
                ),
                actionState = AttendanceActionState.Blocked(
                    reason = AttendanceBlockReason.TARGET_LOCATION_UNAVAILABLE,
                    title = "Lokasi belum siap",
                    message = "Pulihkan data target terlebih dahulu"
                )
            )

            assertEquals(recoveryAction, result.primaryAction)
            assertTrue(result.isPrimaryActionEnabled)
        }
    }

    @Test
    fun `in flight and completed states cannot expose an enabled stale checkin CTA`() {
        val staleReadyPreparation = preparation(
            action = AttendancePreparationPrimaryAction.CONTINUE_TO_FACE_VERIFICATION,
            label = "Lanjut ke Verifikasi Wajah",
            enabled = true
        )
        val terminalOrInFlight = listOf(
            AttendanceActionState.VerifyingFace(AttendanceActionIntent.CHECK_IN),
            AttendanceActionState.Submitting(
                intent = AttendanceActionIntent.CHECK_IN,
                message = "Mengirim check-in..."
            ),
            AttendanceActionState.Completed
        )

        terminalOrInFlight.forEach { actionState ->
            val result = AttendancePrimaryActionUiCombiner.combine(
                preparation = staleReadyPreparation,
                actionState = actionState
            )

            assertEquals(AttendancePreparationPrimaryAction.WAIT, result.primaryAction)
            assertFalse(result.isPrimaryActionEnabled)
        }
    }

    private fun preparation(
        action: AttendancePreparationPrimaryAction,
        label: String,
        enabled: Boolean
    ) = AttendancePreparationUiModel(
        modeOptions = emptyList(),
        targetSummary = null,
        statusMessage = "status",
        wfaDiscovery = WfaDiscoveryUiModel.Hidden,
        primaryAction = action,
        primaryActionLabel = label,
        isPrimaryActionEnabled = enabled,
        secondaryAction = null,
        secondaryActionLabel = null
    )
}
