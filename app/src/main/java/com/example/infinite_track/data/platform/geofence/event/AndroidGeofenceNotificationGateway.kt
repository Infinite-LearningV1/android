package com.example.infinite_track.data.platform.geofence.event

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.infinite_track.utils.NotificationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidGeofenceNotificationGateway @Inject constructor(
    @ApplicationContext private val context: Context
) : GeofenceNotificationGateway {

    override fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    override fun showReminder(label: String) {
        NotificationHelper.showCheckInReminderNotification(context, label)
    }

    override fun showActive(transition: GeofenceTransition, label: String) {
        NotificationHelper.showGeofenceNotification(context, transition.name, label)
    }
}
