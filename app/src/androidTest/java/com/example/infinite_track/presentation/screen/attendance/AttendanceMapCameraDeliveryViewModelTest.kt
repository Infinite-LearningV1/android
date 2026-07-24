package com.example.infinite_track.presentation.screen.attendance

import android.os.SystemClock
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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AttendanceMapCameraDeliveryViewModelTest {
    @Test
    fun late_wfa_target_resolution_does_not_replace_explicit_search_preview_focus() {
        val resolver =
            AttendanceScreenFaceResultRescueTest.ControllableFakeWfaBookingResolver()
        val viewModel = AttendanceScreenFaceResultRescueTest()
            .createAttendanceViewModelForCameraTest(resolver)
        waitUntil {
            viewModel.uiState.value.preparation.targetResolution is
                TargetLocationResolution.Resolved
        }

        onMainThread { viewModel.onWorkModeSelected(WorkMode.WFA) }
        runBlocking { resolver.awaitRequestStarted() }

        val preview = LocationResult(
            placeName = "Pilihan saat resolving",
            address = "Palu",
            latitude = -0.90,
            longitude = 119.88
        )
        onMainThread { viewModel.onLocationSelected(preview) }
        val previewFocus = viewModel.mapCameraEffect.value.requireFocus()

        resolver.completeApprovedWfa()
        runBlocking { resolver.awaitRequestCompleted() }
        waitUntil {
            viewModel.uiState.value.preparation.targetResolution is
                TargetLocationResolution.Resolved
        }

        assertEquals(previewFocus, viewModel.mapCameraEffect.value)
        assertEquals(GeoCoordinate(-0.90, 119.88), previewFocus.coordinate)
        assertEquals(17f, previewFocus.zoom)
    }

    @Test
    fun map_recreation_refocuses_persisted_explicit_selection_before_authoritative_target() {
        onMainThread {
            val viewModel = createViewModelInWfaMode()
            val preview = LocationResult(
                placeName = "Pilihan persisten",
                address = "Palu",
                latitude = -0.91,
                longitude = 119.89
            )

            viewModel.onLocationSelected(preview)
            val initial = viewModel.mapCameraEffect.value.requireFocus()
            viewModel.onMapCameraEffectConsumed(initial.id)
            assertNull(viewModel.mapCameraEffect.value)

            viewModel.onMapReady()
            val recreated = viewModel.mapCameraEffect.value.requireFocus()

            assertTrue(recreated.id > initial.id)
            assertEquals(GeoCoordinate(-0.91, 119.89), recreated.coordinate)
            assertEquals(17f, recreated.zoom)

            viewModel.onMapReady()
            assertEquals(recreated, viewModel.mapCameraEffect.value)
        }
    }

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

    private fun waitUntil(
        timeoutMillis: Long = 5_000,
        predicate: () -> Boolean
    ) {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        while (!predicate()) {
            check(SystemClock.uptimeMillis() < deadline) {
                "Timed out waiting for ViewModel state."
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            SystemClock.sleep(10)
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
