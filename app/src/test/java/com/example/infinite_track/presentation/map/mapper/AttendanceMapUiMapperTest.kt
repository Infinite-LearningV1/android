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
import com.example.infinite_track.domain.model.location.LocationResult
import com.example.infinite_track.domain.model.wfa.WfaRecommendation
import com.example.infinite_track.presentation.map.model.MapMarkerCategory
import com.example.infinite_track.presentation.map.model.MapMarkerRole
import com.example.infinite_track.presentation.screen.attendance.preparation.AttendancePreparationState
import com.example.infinite_track.presentation.screen.attendance.preparation.WfaDiscoveryState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceMapUiMapperTest {

    @Test
    fun `marker categories follow work mode and discovery roles`() {
        val recommendation = recommendation("cafe", -0.90, 119.88)
        val mapped = AttendanceMapUiMapper.map(
            preparation = AttendancePreparationState(
                selectedMode = WorkMode.WFA,
                targetResolution = TargetLocationResolution.Resolved(approvedTarget),
                wfaDiscovery = WfaDiscoveryState.Content(
                    recommendations = listOf(recommendation),
                    selectedKey = recommendation.stableKey
                )
            ),
            hasPreciseLocationPermission = true
        )

        assertEquals(
            MapMarkerCategory.WFA,
            mapped.markers.single { it.role == MapMarkerRole.AUTHORITATIVE_TARGET }.category
        )
        assertEquals(
            MapMarkerCategory.WFA,
            mapped.markers.single { it.role == MapMarkerRole.WFA_RECOMMENDATION }.category
        )
    }

    @Test
    fun `WFA recommendation preview never replaces authoritative marker`() {
        val recommendation = recommendation("cafe@-0.900000,119.880000", -0.90, 119.88)
        val preview = LocationResult("Ruang kerja", "Palu", -0.91, 119.89)
        val preparation = AttendancePreparationState(
            selectedMode = WorkMode.WFA,
            targetResolution = TargetLocationResolution.Resolved(approvedTarget),
            wfaDiscovery = WfaDiscoveryState.Content(
                recommendations = listOf(recommendation),
                selectedKey = recommendation.stableKey,
                searchPreview = preview
            )
        )

        val mapped = AttendanceMapUiMapper.map(preparation, true)

        assertEquals(1, mapped.markers.count { it.role == MapMarkerRole.AUTHORITATIVE_TARGET })
        assertEquals(1, mapped.markers.count { it.role == MapMarkerRole.WFA_RECOMMENDATION })
        assertEquals(1, mapped.markers.count { it.role == MapMarkerRole.SEARCH_PREVIEW })
        assertEquals(
            approvedTarget.coordinate,
            mapped.markers.single { it.role == MapMarkerRole.AUTHORITATIVE_TARGET }.coordinate
        )
        assertEquals(approvedTarget.coordinate, mapped.circles.single().center)
    }

    @Test
    fun `current location and resolved target are projected from preparation state`() {
        val currentCoordinate = GeoCoordinate(-0.91, 119.89)
        val preparation = AttendancePreparationState(
            targetResolution = TargetLocationResolution.Resolved(officeTarget),
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

        assertEquals(
            currentCoordinate,
            mapped.markers.single { it.role == MapMarkerRole.CURRENT_LOCATION }.coordinate
        )
        assertEquals(
            officeTarget.coordinate,
            mapped.markers.single { it.role == MapMarkerRole.AUTHORITATIVE_TARGET }.coordinate
        )
        assertEquals(officeTarget.radius, mapped.circles.single().radius)
        assertTrue(mapped.hasPreciseLocationPermission)
    }

    @Test
    fun `unresolved target never produces authoritative marker or circle`() {
        val preparation = AttendancePreparationState(
            selectedMode = WorkMode.WFA,
            targetResolution = TargetLocationResolution.Resolving(WorkMode.WFA),
            wfaDiscovery = WfaDiscoveryState.Content(
                recommendations = listOf(recommendation("library@-0.920000,119.900000", -0.92, 119.90))
            )
        )

        val mapped = AttendanceMapUiMapper.map(preparation, false)

        assertFalse(mapped.markers.any { it.role == MapMarkerRole.AUTHORITATIVE_TARGET })
        assertTrue(mapped.circles.isEmpty())
        assertEquals(1, mapped.markers.count { it.role == MapMarkerRole.WFA_RECOMMENDATION })
    }

    @Test
    fun `recommendation marker id and selection use stable key`() {
        val recommendation = recommendation("cafe@-0.900000,119.880000", -0.90, 119.88)
        val preparation = AttendancePreparationState(
            selectedMode = WorkMode.WFA,
            targetResolution = TargetLocationResolution.Resolving(WorkMode.WFA),
            wfaDiscovery = WfaDiscoveryState.Content(
                recommendations = listOf(recommendation),
                selectedKey = recommendation.stableKey
            )
        )

        val marker = AttendanceMapUiMapper.map(preparation, true).markers.single()

        assertEquals("wfa:${recommendation.stableKey}", marker.id)
        assertTrue(marker.isSelected)
    }

    private fun recommendation(key: String, latitude: Double, longitude: Double) =
        WfaRecommendation(
            stableKey = key,
            name = "Cafe",
            address = "Palu",
            coordinate = GeoCoordinate(latitude, longitude),
            category = "Cafe",
            suitabilityScore = 0.91,
            suitabilityLabel = "Sangat sesuai",
            distanceMeters = DistanceMeters(1_250.0)
        )

    private val officeTarget = AuthoritativeTargetLocation(
        targetId = TargetLocationId("office:1"),
        mode = WorkMode.WFO,
        source = TargetLocationSource.STATUS_TODAY,
        coordinate = GeoCoordinate(-0.89, 119.87),
        radius = DistanceMeters(100.0),
        displayName = "Kantor Palu"
    )

    private val approvedTarget = AuthoritativeTargetLocation(
        targetId = TargetLocationId("wfa:booking:42"),
        mode = WorkMode.WFA,
        source = TargetLocationSource.APPROVED_WFA_BOOKING,
        coordinate = GeoCoordinate(-0.88, 119.86),
        radius = DistanceMeters(100.0),
        displayName = "WFA disetujui"
    )
}
