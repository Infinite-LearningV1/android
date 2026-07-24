package com.example.infinite_track.presentation.map.adapter

import com.example.infinite_track.domain.model.attendance.TargetLocationResolution
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.model.location.LocationResult
import com.example.infinite_track.presentation.map.mapper.AttendanceMapUiMapper
import com.example.infinite_track.presentation.map.model.MapMarkerRole
import com.example.infinite_track.presentation.screen.attendance.preparation.AttendancePreparationState
import com.example.infinite_track.presentation.screen.attendance.preparation.WfaDiscoveryState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleAttendanceMapRenderStateTest {

    @Test
    fun `second search preview replaces marker renderer state and reopens its callout`() {
        val first = previewMarker(
            LocationResult(
                placeName = "Kopi Pertama",
                address = "Jalan Pertama",
                latitude = -0.900,
                longitude = 119.880,
                placeId = "places/first"
            )
        )
        val second = previewMarker(
            LocationResult(
                placeName = "Kopi Kedua",
                address = "Jalan Kedua",
                latitude = -0.910,
                longitude = 119.890,
                placeId = "places/second"
            )
        )

        assertNotEquals(first.renderIdentity, second.renderIdentity)
        assertEquals(GeoCoordinate(-0.910, 119.890), second.coordinate)
        assertEquals("Kopi Kedua", second.title)
        assertEquals("Jalan Kedua", second.snippet)
        assertTrue(second.isSelected)
    }

    private fun previewMarker(preview: LocationResult) =
        AttendanceMapUiMapper.map(
            preparation = AttendancePreparationState(
                selectedMode = WorkMode.WFA,
                targetResolution = TargetLocationResolution.Resolving(WorkMode.WFA),
                wfaDiscovery = WfaDiscoveryState.Content(
                    recommendations = emptyList(),
                    searchPreview = preview
                )
            ),
            hasPreciseLocationPermission = true
        ).markers.single { it.role == MapMarkerRole.SEARCH_PREVIEW }
}
