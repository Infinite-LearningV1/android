package com.example.infinite_track.data.platform.geofence.event

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

interface GeofenceNotificationGateway {
    fun canPostNotifications(): Boolean
    fun showReminder(label: String)
    fun showActive(transition: GeofenceTransition, label: String)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class GeofenceEventGatewayModule {

    @Binds
    @Singleton
    abstract fun bindGeofenceNotificationGateway(
        gateway: AndroidGeofenceNotificationGateway
    ): GeofenceNotificationGateway

    @Binds
    @Singleton
    abstract fun bindLocationEvidenceScheduler(
        scheduler: WorkManagerLocationEvidenceScheduler
    ): LocationEvidenceScheduler
}
