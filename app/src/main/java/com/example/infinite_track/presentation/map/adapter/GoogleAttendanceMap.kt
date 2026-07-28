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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.infinite_track.R
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.presentation.map.model.AttendanceMapEvent
import com.example.infinite_track.presentation.map.model.AttendanceMapCameraMoveOrigin
import com.example.infinite_track.presentation.map.model.MapCameraEffect
import com.example.infinite_track.presentation.map.model.MapInteractionMode
import com.example.infinite_track.presentation.map.components.CompactMapCallout
import com.example.infinite_track.presentation.map.components.color
import com.example.infinite_track.presentation.map.model.MapMarkerCategory
import com.example.infinite_track.presentation.map.model.MapMarkerUiModel
import com.example.infinite_track.presentation.map.model.MapPermissionRequirement
import com.example.infinite_track.presentation.map.model.MapUiState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerInfoWindow
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun GoogleAttendanceMap(
    state: MapUiState,
    cameraEffect: MapCameraEffect?,
    onEvent: (AttendanceMapEvent) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues()
) {
    val currentOnEvent by rememberUpdatedState(onEvent)

    if (
        state.permissionRequirement == MapPermissionRequirement.PreciseLocation &&
        !state.hasPreciseLocationPermission
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .testTag("attendanceMapFallback"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.attendance_map_precise_location_unavailable),
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
    val uiSettings = remember(state.interactionMode) {
        state.interactionMode.toMapUiSettings()
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
            currentOnEvent(AttendanceMapEvent.CameraEffectConsumed(effect.id))
        } catch (error: CancellationException) {
            throw error
        } catch (_: RuntimeException) {
            // A subsequent effect or map lifecycle transition may invalidate an animation.
        }
    }

    LaunchedEffect(cameraPositionState, mapLoaded) {
        if (!mapLoaded) return@LaunchedEffect
        val movementOriginTracker = AttendanceMapCameraMovementOriginTracker()
        snapshotFlow {
            CameraMovementSnapshot(
                isMoving = cameraPositionState.isMoving,
                origin = cameraPositionState.cameraMoveStartedReason
                    .toAttendanceMapCameraMoveOrigin()
            )
        }
            .distinctUntilChanged()
            .collect { movement ->
                val terminalOrigin = movementOriginTracker.onMovementChanged(
                    isMoving = movement.isMoving,
                    startedOrigin = movement.origin
                ) ?: return@collect
                currentOnEvent(
                    AttendanceMapEvent.CameraIdle(
                        center = cameraPositionState.position.target.toGeoCoordinate(),
                        origin = terminalOrigin
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
            if (!mapLoaded) {
                mapLoaded = true
                currentOnEvent(AttendanceMapEvent.Ready)
            }
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
            key(marker.renderIdentity) {
                val markerState = rememberMarkerState(
                    key = marker.renderIdentity,
                    position = marker.coordinate.toLatLng()
                )
                LaunchedEffect(marker.renderIdentity, marker.isSelected, markerState) {
                    if (marker.isSelected) {
                        markerState.showInfoWindow()
                    } else {
                        markerState.hideInfoWindow()
                    }
                }
                MarkerInfoWindow(
                    state = markerState,
                    title = marker.title,
                    snippet = marker.snippet,
                    icon = rememberMarkerDescriptor(marker.category),
                    zIndex = if (marker.isSelected) 2f else 1f,
                    onClick = {
                        if (state.interactionMode.shouldDispatchMarkerClick()) {
                            currentOnEvent(AttendanceMapEvent.MarkerClicked(marker))
                        }
                        state.interactionMode.shouldConsumeMarkerClick()
                    }
                ) {
                    CompactMapCallout(
                        title = marker.title,
                        category = marker.category,
                        recommendationInfo = marker.recommendationInfo
                    )
                }
            }
        }
    }
}

internal fun MapInteractionMode.toMapUiSettings() = MapUiSettings(
    compassEnabled = false,
    indoorLevelPickerEnabled = false,
    mapToolbarEnabled = false,
    myLocationButtonEnabled = false,
    rotationGesturesEnabled = false,
    scrollGesturesEnabled = this == MapInteractionMode.Interactive,
    tiltGesturesEnabled = false,
    zoomControlsEnabled = false,
    zoomGesturesEnabled = this == MapInteractionMode.Interactive
)

internal fun MapInteractionMode.shouldDispatchMarkerClick(): Boolean =
    this == MapInteractionMode.Interactive

internal fun MapInteractionMode.shouldConsumeMarkerClick(): Boolean = true

internal val MapMarkerUiModel.renderIdentity: String
    get() = "${role.name}:$id"

@Composable
private fun rememberMarkerDescriptor(category: MapMarkerCategory): BitmapDescriptor {
    val color = category.color()
    return remember(category, color) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        BitmapDescriptorFactory.defaultMarker(hsv[0])
    }
}

private fun GeoCoordinate.toLatLng(): LatLng = LatLng(latitude, longitude)

private fun LatLng.toGeoCoordinate(): GeoCoordinate = GeoCoordinate(latitude, longitude)

internal fun CameraMoveStartedReason.toAttendanceMapCameraMoveOrigin():
    AttendanceMapCameraMoveOrigin = when (this) {
    CameraMoveStartedReason.GESTURE -> AttendanceMapCameraMoveOrigin.USER_GESTURE
    CameraMoveStartedReason.API_ANIMATION,
    CameraMoveStartedReason.DEVELOPER_ANIMATION ->
        AttendanceMapCameraMoveOrigin.PROGRAMMATIC
    CameraMoveStartedReason.NO_MOVEMENT_YET,
    CameraMoveStartedReason.UNKNOWN -> AttendanceMapCameraMoveOrigin.UNKNOWN
}

private data class CameraMovementSnapshot(
    val isMoving: Boolean,
    val origin: AttendanceMapCameraMoveOrigin
)

internal class AttendanceMapCameraMovementOriginTracker {
    private var activeOrigin = AttendanceMapCameraMoveOrigin.UNKNOWN

    fun onMovementChanged(
        isMoving: Boolean,
        startedOrigin: AttendanceMapCameraMoveOrigin
    ): AttendanceMapCameraMoveOrigin? {
        if (isMoving) {
            activeOrigin = startedOrigin
            return null
        }
        return activeOrigin.also {
            activeOrigin = AttendanceMapCameraMoveOrigin.UNKNOWN
        }
    }
}
