package com.example.infinite_track.presentation.screen.attendance

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.infinite_track.domain.model.attendance.AttendancePreparationEligibility
import com.example.infinite_track.domain.model.attendance.AuthoritativeTargetLocation
import com.example.infinite_track.domain.model.attendance.TargetLocationId
import com.example.infinite_track.domain.model.attendance.TargetLocationResolution
import com.example.infinite_track.domain.model.attendance.TargetLocationSource
import com.example.infinite_track.domain.model.attendance.TargetRangeStatus
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.model.location.LocationResult
import com.example.infinite_track.presentation.map.model.MapCameraEffect
import com.example.infinite_track.presentation.screen.attendance.preparation.WfaDiscoveryState
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AttendanceMapCameraDeliveryViewModelTest {
    @Test
    fun search_preview_without_collector_survives_map_ready_and_is_consumed_once() {
        onMainThread {
            val viewModel = createViewModelInWfaMode()
            val preview = LocationResult(
                placeName = "Kopi Palu",
                address = "Jalan Merpati",
                latitude = -0.90,
                longitude = 119.88
            )

            viewModel.onLocationSelected(preview)
            val pending = viewModel.mapCameraEffect.value.requireFocus()

            assertEquals(GeoCoordinate(-0.90, 119.88), pending.coordinate)
            assertEquals(17f, pending.zoom)
            assertTrue(
                viewModel.uiState.value.preparation.targetResolution is
                    TargetLocationResolution.Resolved
            )

            viewModel.onMapReady()

            assertEquals(pending, viewModel.mapCameraEffect.value)

            viewModel.onMapCameraEffectConsumed(pending.id)

            assertNull(viewModel.mapCameraEffect.value)
            viewModel.onMapCameraEffectConsumed(pending.id)
            assertNull(viewModel.mapCameraEffect.value)
        }
    }

    @Test
    fun newer_wfa_selection_supersedes_older_focus_and_stale_acknowledgement() {
        onMainThread {
            val viewModel = createViewModelInWfaMode()

            viewModel.onLocationSelected(
                LocationResult("Pertama", "Palu", -0.90, 119.88)
            )
            val first = viewModel.mapCameraEffect.value.requireFocus()

            viewModel.onLocationSelected(
                LocationResult("Terbaru", "Palu", -0.91, 119.89)
            )
            val latest = viewModel.mapCameraEffect.value.requireFocus()

            assertTrue(latest.id > first.id)
            assertEquals(GeoCoordinate(-0.91, 119.89), latest.coordinate)
            assertEquals(17f, latest.zoom)

            viewModel.onMapCameraEffectConsumed(first.id)
            assertEquals(latest, viewModel.mapCameraEffect.value)

            viewModel.onMapCameraEffectConsumed(latest.id)
            assertNull(viewModel.mapCameraEffect.value)
        }
    }

    private fun createViewModelInWfaMode(): AttendanceViewModel {
        val viewModel = AttendanceScreenFaceResultRescueTest()
            .createDefaultAttendanceViewModelForCameraTest()
        viewModel.onWfaDiscoveryRetryRequested()
        viewModel.setResolvedWfaTarget()
        return viewModel
    }

    private fun AttendanceViewModel.setResolvedWfaTarget() {
        val state = mutableUiState()
        val range = TargetRangeStatus.Inside(DistanceMeters(20.0))
        state.value = state.value.copy(
            preparation = state.value.preparation.copy(
                selectedMode = WorkMode.WFA,
                targetResolution = TargetLocationResolution.Resolved(approvedTarget),
                rangeStatus = range,
                wfaDiscovery = WfaDiscoveryState.Content(recommendations = emptyList()),
                eligibility = AttendancePreparationEligibility.Ready(approvedTarget, range)
            )
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun AttendanceViewModel.mutableUiState(): MutableStateFlow<AttendanceScreenState> =
        AttendanceViewModel::class.java
            .getDeclaredField("_uiState")
            .apply { isAccessible = true }
            .get(this) as MutableStateFlow<AttendanceScreenState>

    private fun MapCameraEffect?.requireFocus(): MapCameraEffect.Focus =
        requireNotNull(this) as MapCameraEffect.Focus

    private fun onMainThread(block: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
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
