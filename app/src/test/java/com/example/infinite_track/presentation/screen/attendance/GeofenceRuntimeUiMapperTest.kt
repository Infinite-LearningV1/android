package com.example.infinite_track.presentation.screen.attendance

import com.example.infinite_track.domain.model.geofence.GeofenceDisabledReason
import com.example.infinite_track.domain.model.geofence.GeofencePermissionRequirement
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeFailure
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeMode
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeReadiness
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeResult
import com.example.infinite_track.domain.model.geofence.NotificationReadiness
import com.example.infinite_track.domain.model.geofence.RegistrationReadiness
import org.junit.Assert.assertEquals
import org.junit.Test

class GeofenceRuntimeUiMapperTest {

    private val mapper = GeofenceRuntimeUiMapper()

    @Test
    fun `ready runtime maps both monitoring and notification as available`() {
        assertEquals(
            GeofenceRuntimeUiState(
                monitoringAvailable = true,
                notificationAvailable = true
            ),
            mapper.map(readyReadiness(), appliedRuntime())
        )
    }

    @Test
    fun `notification permission required keeps monitoring available`() {
        assertEquals(
            GeofenceRuntimeUiState(
                monitoringAvailable = true,
                notificationAvailable = false,
                reason = GeofenceRuntimeUiReason.NOTIFICATION_PERMISSION_REQUIRED
            ),
            mapper.map(
                readyReadiness(notification = NotificationReadiness.PERMISSION_REQUIRED),
                appliedRuntime()
            )
        )
    }

    @Test
    fun `precise location permission required disables monitoring`() {
        assertEquals(
            GeofenceRuntimeUiState(
                notificationAvailable = true,
                reason = GeofenceRuntimeUiReason.PRECISE_LOCATION_REQUIRED
            ),
            mapper.map(
                readinessWith(
                    RegistrationReadiness.PermissionRequired(
                        setOf(GeofencePermissionRequirement.PRECISE_FOREGROUND)
                    )
                ),
                degradedRuntime(
                    GeofenceRuntimeFailure.PermissionNotGranted(
                        setOf(GeofencePermissionRequirement.PRECISE_FOREGROUND)
                    )
                )
            )
        )
    }

    @Test
    fun `background location permission required disables monitoring`() {
        assertEquals(
            GeofenceRuntimeUiState(
                notificationAvailable = true,
                reason = GeofenceRuntimeUiReason.BACKGROUND_LOCATION_REQUIRED
            ),
            mapper.map(
                readinessWith(
                    RegistrationReadiness.PermissionRequired(
                        setOf(GeofencePermissionRequirement.BACKGROUND_LOCATION)
                    )
                ),
                degradedRuntime(
                    GeofenceRuntimeFailure.PermissionNotGranted(
                        setOf(GeofencePermissionRequirement.BACKGROUND_LOCATION)
                    )
                )
            )
        )
    }

    @Test
    fun `disabled device location disables monitoring`() {
        assertEquals(
            GeofenceRuntimeUiState(
                notificationAvailable = true,
                reason = GeofenceRuntimeUiReason.DEVICE_LOCATION_DISABLED
            ),
            mapper.map(
                readinessWith(RegistrationReadiness.DeviceLocationDisabled),
                degradedRuntime(GeofenceRuntimeFailure.DeviceLocationDisabled)
            )
        )
    }

    @Test
    fun `unavailable play services disables monitoring`() {
        assertEquals(
            GeofenceRuntimeUiState(
                notificationAvailable = true,
                reason = GeofenceRuntimeUiReason.PLAY_SERVICES_UNAVAILABLE
            ),
            mapper.map(
                readinessWith(RegistrationReadiness.PlayServicesUnavailable),
                degradedRuntime(GeofenceRuntimeFailure.PlayServicesUnavailable)
            )
        )
    }

    @Test
    fun `registration degradation does not report monitoring as available`() {
        assertEquals(
            GeofenceRuntimeUiState(
                notificationAvailable = true,
                reason = GeofenceRuntimeUiReason.REGISTRATION_DEGRADED
            ),
            mapper.map(
                readyReadiness(),
                degradedRuntime(GeofenceRuntimeFailure.RegistrationFailed("internal"))
            )
        )
    }

    private fun readyReadiness(
        notification: NotificationReadiness = NotificationReadiness.READY
    ) = GeofenceRuntimeReadiness(
        registration = RegistrationReadiness.Ready,
        notification = notification
    )

    private fun readinessWith(
        registration: RegistrationReadiness
    ) = GeofenceRuntimeReadiness(
        registration = registration,
        notification = NotificationReadiness.READY
    )

    private fun appliedRuntime() = GeofenceRuntimeResult.Applied(
        mode = runtimeMode(),
        generation = 1,
        logicalIds = emptySet()
    )

    private fun degradedRuntime(
        failure: GeofenceRuntimeFailure
    ) = GeofenceRuntimeResult.Degraded(
        mode = runtimeMode(),
        failure = failure
    )

    private fun runtimeMode() = GeofenceRuntimeMode.Disabled(
        GeofenceDisabledReason.NO_ELIGIBLE_SESSION
    )
}
