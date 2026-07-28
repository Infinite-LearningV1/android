package com.example.infinite_track.presentation.components.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.example.infinite_track.R
import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.presentation.design.components.state.InfiniteErrorState
import com.example.infinite_track.presentation.design.tokens.InfiniteRadius
import com.example.infinite_track.presentation.design.tokens.InfiniteSpacing
import com.example.infinite_track.presentation.map.adapter.AttendanceMap
import com.example.infinite_track.presentation.map.model.AttendanceMapEvent
import com.example.infinite_track.presentation.map.model.MapCameraEffect
import com.example.infinite_track.presentation.map.model.MapCircleUiModel
import com.example.infinite_track.presentation.map.model.MapInteractionMode
import com.example.infinite_track.presentation.map.model.MapMarkerCategory
import com.example.infinite_track.presentation.map.model.MapMarkerRole
import com.example.infinite_track.presentation.map.model.MapMarkerUiModel
import com.example.infinite_track.presentation.map.model.MapPermissionRequirement
import com.example.infinite_track.presentation.map.model.MapUiState
import kotlinx.coroutines.delay

private const val MAP_READY_TIMEOUT_MILLIS = 8_000L

@Composable
fun ReadOnlyLocationMap(
    coordinate: GeoCoordinate?,
    radiusMeters: Int,
    title: String,
    address: String,
    modifier: Modifier = Modifier,
    contentDescription: String
) {
    val mapModifier = modifier
        .height(InfiniteSpacing.Default.xxl * 6)
        .clip(RoundedCornerShape(InfiniteRadius.Large))
    val resolvedCoordinate = coordinate ?: run {
        LocationMapFallback(mapModifier)
        return
    }
    val state = remember(resolvedCoordinate, radiusMeters, title, address, contentDescription) {
        buildReadOnlyLocationMapState(
            coordinate = resolvedCoordinate,
            radiusMeters = radiusMeters,
            title = title,
            address = address,
            contentDescription = contentDescription
        )
    }
    val cameraEffect = remember(resolvedCoordinate) {
        MapCameraEffect.Focus(id = 1L, coordinate = resolvedCoordinate)
    }
    var isMapReady by remember(resolvedCoordinate) { mutableStateOf(false) }
    var isMapTimedOut by remember(resolvedCoordinate) { mutableStateOf(false) }

    LaunchedEffect(resolvedCoordinate, isMapReady) {
        if (!isMapReady) {
            delay(MAP_READY_TIMEOUT_MILLIS)
            isMapTimedOut = true
        }
    }

    if (isMapTimedOut) {
        LocationMapFallback(mapModifier)
    } else {
        Box(mapModifier) {
            AttendanceMap(
                state = state,
                cameraEffect = cameraEffect,
                onEvent = { event ->
                    if (event == AttendanceMapEvent.Ready) isMapReady = true
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun LocationMapFallback(modifier: Modifier) {
    InfiniteErrorState(
        title = stringResource(R.string.location_map_unavailable_title),
        message = stringResource(R.string.location_map_unavailable_message),
        modifier = modifier
    )
}

internal fun buildReadOnlyLocationMapState(
    coordinate: GeoCoordinate,
    radiusMeters: Int,
    title: String,
    address: String,
    contentDescription: String
): MapUiState = MapUiState(
    markers = listOf(
        MapMarkerUiModel(
            id = "read-only-location-target",
            role = MapMarkerRole.AUTHORITATIVE_TARGET,
            category = MapMarkerCategory.WFA,
            coordinate = coordinate,
            title = title,
            snippet = address
        )
    ),
    circles = listOf(
        MapCircleUiModel(
            id = "read-only-location-radius",
            center = coordinate,
            radius = DistanceMeters(radiusMeters.toDouble())
        )
    ),
    contentDescription = contentDescription,
    permissionRequirement = MapPermissionRequirement.None,
    interactionMode = MapInteractionMode.ReadOnly
)
