package com.example.infinite_track.presentation.map.mapper

import com.example.infinite_track.domain.model.attendance.AuthoritativeTargetLocation
import com.example.infinite_track.domain.model.attendance.TargetLocationId
import com.example.infinite_track.domain.model.attendance.TargetLocationResolution
import com.example.infinite_track.domain.model.attendance.TargetLocationSource
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.location.CurrentLocation
import com.example.infinite_track.domain.model.location.CurrentLocationResult
import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.presentation.map.model.MapMarkerCategory
import com.example.infinite_track.presentation.map.model.MapMarkerRole
import com.example.infinite_track.presentation.screen.attendance.preparation.AttendancePreparationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceMapUiMapperTest {

    @Test
    fun `current location and authoritative target are the only projected markers`() {
        val currentCoordinate = GeoCoordinate(-0.91, 119.89)
        val preparation = AttendancePreparationState(
            selectedMode = WorkMode.WFA,
            targetResolution = TargetLocationResolution.Resolved(approvedTarget),
            currentLocation = CurrentLocationResult.Success(
                CurrentLocation(
                    coordinate = currentCoordinate,
                    accuracy = DistanceMeters(5.0),
                    capturedAtEpochMillis = 1_000L,
                    provider = "provider",
                    isMock = false
                )
            )
        )

        val mapped = AttendanceMapUiMapper.map(preparation, hasPreciseLocationPermission = true)

        assertEquals(2, mapped.markers.size)
        assertEquals(
            currentCoordinate,
            mapped.markers.single { it.role == MapMarkerRole.CURRENT_LOCATION }.coordinate
        )
        val targetMarker = mapped.markers.single {
            it.role == MapMarkerRole.AUTHORITATIVE_TARGET
        }
        assertEquals(approvedTarget.coordinate, targetMarker.coordinate)
        assertEquals(MapMarkerCategory.WFA, targetMarker.category)
        assertEquals(approvedTarget.radius, mapped.circles.single().radius)
        assertTrue(mapped.hasPreciseLocationPermission)
    }

    @Test
    fun `unresolved target produces no target marker or circle`() {
        val mapped = AttendanceMapUiMapper.map(
            preparation = AttendancePreparationState(
                selectedMode = WorkMode.WFA,
                targetResolution = TargetLocationResolution.Resolving(WorkMode.WFA)
            ),
            hasPreciseLocationPermission = false
        )

        assertFalse(mapped.markers.any { it.role == MapMarkerRole.AUTHORITATIVE_TARGET })
        assertTrue(mapped.circles.isEmpty())
        assertFalse(mapped.hasPreciseLocationPermission)
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
