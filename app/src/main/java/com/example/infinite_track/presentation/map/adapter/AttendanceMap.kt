package com.example.infinite_track.presentation.map.adapter

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.infinite_track.presentation.map.model.AttendanceMapEvent
import com.example.infinite_track.presentation.map.model.MapCameraEffect
import com.example.infinite_track.presentation.map.model.MapUiState

@Composable
fun AttendanceMap(
    state: MapUiState,
    cameraEffect: MapCameraEffect?,
    onEvent: (AttendanceMapEvent) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues()
) {
    GoogleAttendanceMap(
        state = state,
        cameraEffect = cameraEffect,
        onEvent = onEvent,
        modifier = modifier,
        contentPadding = contentPadding
    )
}
