package com.example.infinite_track.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.infinite_track.R
import com.example.infinite_track.presentation.main.MainActivity

object NotificationHelper {

    private const val CHANNEL_ID = "geofence_channel_01"
    private const val CHANNEL_NAME = "Geofence Notifications"

    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun buildLaunchAppIntent(context: Context): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
    }

    private fun buildLaunchAppPendingIntent(context: Context): PendingIntent {
        return PendingIntent.getActivity(
            context,
            0,
            buildLaunchAppIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun showExitAreaWarningNotification(context: Context, locationName: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.notifications_24px)
            .setContentTitle("Peringatan Keluar Area")
            .setContentText("Anda telah meninggalkan area: $locationName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(buildLaunchAppPendingIntent(context))
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    fun showCheckInReminderNotification(context: Context, locationName: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.notifications_24px)
            .setContentTitle("Pengingat Check-in")
            .setContentText("Anda berada di area: $locationName. Jangan lupa check-in.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(buildLaunchAppPendingIntent(context))
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
