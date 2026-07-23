package com.example.infinite_track.presentation.map.adapter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.presentation.map.model.AttendanceMapEvent
import com.example.infinite_track.presentation.map.model.MapCameraEffect
import com.example.infinite_track.presentation.map.model.MapMarkerRole
import com.example.infinite_track.presentation.map.model.MapUiState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
internal fun GoogleAttendanceMap(
    state: MapUiState,
    cameraEffect: MapCameraEffect?,
    onEvent: (AttendanceMapEvent) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues()
) {
    val currentOnEvent by rememberUpdatedState(onEvent)

    if (!state.hasPreciseLocationPermission) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .testTag("attendanceMapFallback"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Lokasi presisi belum siap",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(24.dp)
            )
        }
        return
    }

    val cameraPositionState = rememberCameraPositionState()
    val properties = remember {
        MapProperties(
            isBuildingEnabled = false,
            isIndoorEnabled = false,
            isMyLocationEnabled = false,
            isTrafficEnabled = false
        )
    }
    val uiSettings = remember {
        MapUiSettings(
            compassEnabled = false,
            indoorLevelPickerEnabled = false,
            mapToolbarEnabled = false,
            myLocationButtonEnabled = false,
            rotationGesturesEnabled = false,
            scrollGesturesEnabled = true,
            tiltGesturesEnabled = false,
            zoomControlsEnabled = false,
            zoomGesturesEnabled = true
        )
    }
    var mapLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(cameraEffect?.id, mapLoaded) {
        val effect = cameraEffect ?: return@LaunchedEffect
        if (!mapLoaded) return@LaunchedEffect

        try {
            when (effect) {
                is MapCameraEffect.Focus -> cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(effect.coordinate.toLatLng(), effect.zoom),
                    1_200
                )

                is MapCameraEffect.Fit -> {
                    when (effect.coordinates.size) {
                        0 -> Unit
                        1 -> cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(effect.coordinates.first().toLatLng(), 14f),
                            1_200
                        )

                        else -> {
                            val bounds = LatLngBounds.builder().apply {
                                effect.coordinates.forEach { include(it.toLatLng()) }
                            }.build()
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngBounds(bounds, 96),
                                1_500
                            )
                        }
                    }
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: RuntimeException) {
            // A subsequent effect or map lifecycle transition may invalidate an animation.
        }
    }

    LaunchedEffect(cameraPositionState, mapLoaded) {
        if (!mapLoaded) return@LaunchedEffect
        snapshotFlow { cameraPositionState.isMoving }
            .distinctUntilChanged()
            .filter { isMoving -> !isMoving }
            .collect {
                currentOnEvent(
                    AttendanceMapEvent.CameraIdle(
                        cameraPositionState.position.target.toGeoCoordinate()
                    )
                )
            }
    }

    val radiusFill = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val radiusStroke = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)

    GoogleMap(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = state.contentDescription }
            .testTag("attendanceMapContent"),
        cameraPositionState = cameraPositionState,
        properties = properties,
        uiSettings = uiSettings,
        contentPadding = contentPadding,
        onMapLoaded = {
            mapLoaded = true
            currentOnEvent(AttendanceMapEvent.Ready)
        }
    ) {
        state.circles.forEach { circle ->
            Circle(
                center = circle.center.toLatLng(),
                radius = circle.radius.value,
                fillColor = radiusFill,
                strokeColor = radiusStroke,
                strokeWidth = 2f
            )
        }

        state.markers.forEach { marker ->
            val markerState = rememberMarkerState(
                key = marker.id,
                position = marker.coordinate.toLatLng()
            )
            val icon = remember(marker.role, marker.isSelected) {
                BitmapDescriptorFactory.defaultMarker(
                    when {
                        marker.isSelected -> BitmapDescriptorFactory.HUE_AZURE
                        marker.role == MapMarkerRole.CURRENT_USER -> BitmapDescriptorFactory.HUE_CYAN
                        marker.role == MapMarkerRole.WFA_RECOMMENDATION -> BitmapDescriptorFactory.HUE_VIOLET
                        marker.role == MapMarkerRole.WFH -> BitmapDescriptorFactory.HUE_GREEN
                        else -> BitmapDescriptorFactory.HUE_RED
                    }
                )
            }
            Marker(
                state = markerState,
                title = marker.title,
                snippet = marker.snippet,
                icon = icon,
                onClick = {
                    currentOnEvent(AttendanceMapEvent.MarkerClicked(marker))
                    true
                }
            )
        }
    }
}

private fun GeoCoordinate.toLatLng(): LatLng = LatLng(latitude, longitude)

private fun LatLng.toGeoCoordinate(): GeoCoordinate = GeoCoordinate(latitude, longitude)
