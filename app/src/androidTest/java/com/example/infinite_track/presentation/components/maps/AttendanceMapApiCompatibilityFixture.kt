package com.example.infinite_track.presentation.components.maps

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Compile-only fixtures for the public call shapes that predate explicit permission input. */
@Composable
internal fun LegacyAttendanceMapDefaultCallShapeFixture() {
    AttendanceMap()
}

@Composable
internal fun LegacyAttendanceMapPositionalModifierCallShapeFixture(modifier: Modifier) {
    AttendanceMap(modifier)
}
