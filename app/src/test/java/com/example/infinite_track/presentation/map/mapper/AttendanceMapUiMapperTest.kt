package com.example.infinite_track.presentation.map.mapper

import com.example.infinite_track.domain.model.attendance.Location
import com.example.infinite_track.domain.model.attendance.SelectedTargetLocation
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.model.wfa.WfaRecommendation
import com.example.infinite_track.presentation.map.model.MapMarkerRole
import com.example.infinite_track.presentation.screen.attendance.AttendanceScreenState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceMapUiMapperTest {

    @Test
    fun `maps fixed locations current user and selected target radius`() {
        val office = location(1, "Office", -0.89, 119.87)
        val home = location(2, "Home", -0.90, 119.88)
        val state = AttendanceScreenState(
            wfoLocation = office,
            wfhLocation = home,
            selectedTargetLocation = SelectedTargetLocation(
                mode = WorkMode.WFO,
                location = office,
                displayName = office.description,
                description = office.category,
                isAvailable = true
            ),
            currentUserLatitude = -0.91,
            currentUserLongitude = 119.89,
            currentUserAddress = "Palu"
        )

        val mapped = AttendanceMapUiMapper.map(state, hasPreciseLocationPermission = true)

        assertEquals(3, mapped.markers.size)
        assertTrue(mapped.markers.single { it.role == MapMarkerRole.WFO }.isSelected)
        assertEquals(GeoCoordinate(-0.91, 119.89), mapped.markers.single { it.role == MapMarkerRole.CURRENT_USER }.coordinate)
        assertEquals(100.0, mapped.circles.single().radius.value, 0.0)
    }

    @Test
    fun `wfa mode hides fixed markers and preserves stable recommendation ids`() {
        val recommendation = WfaRecommendation(
            name = "Cafe",
            address = "Palu",
            coordinate = GeoCoordinate(-0.90, 119.88),
            score = 0.9,
            label = "Good",
            category = "Cafe",
            distance = 1.0
        )
        val state = AttendanceScreenState(
            isWfaModeActive = true,
            wfoLocation = location(1, "Office", -0.89, 119.87),
            wfaRecommendations = listOf(recommendation),
            selectedWfaLocation = recommendation
        )

        val first = AttendanceMapUiMapper.map(state, true)
        val second = AttendanceMapUiMapper.map(state.copy(), true)

        assertEquals(listOf(MapMarkerRole.WFA_RECOMMENDATION), first.markers.map { it.role })
        assertEquals(first.markers.single().id, second.markers.single().id)
        assertTrue(first.markers.single().isSelected)
    }

    private fun location(
        id: Int,
        name: String,
        latitude: Double,
        longitude: Double
    ) = Location(
        locationId = id,
        description = name,
        coordinate = GeoCoordinate(latitude, longitude),
        radius = 100,
        category = "Work"
    )
}
