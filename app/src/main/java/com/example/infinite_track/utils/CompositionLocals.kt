package com.example.infinite_track.utils

import androidx.compose.runtime.compositionLocalOf

/**
 * CompositionLocal untuk LocationPermissionHelper
 * Memungkinkan akses ke permission helper dari mana saja dalam composition tree
 */
val LocalLocationPermissionHelper = compositionLocalOf<LocationPermissionHelper?> { null }