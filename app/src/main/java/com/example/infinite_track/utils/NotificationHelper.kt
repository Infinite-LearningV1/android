package com.example.infinite_track.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.infinite_track.R
import com.example.infinite_track.presentation.main.MainActivity

object NotificationHelper {

    private const val TAG = "NotificationHelper"
    private const val REMINDER_CHANNEL_ID = "attendance_reminder_channel"
    private const val SESSION_ALERT_CHANNEL_ID = "attendance_session_alert_channel"
    private const val EVIDENCE_SYNC_CHANNEL_ID = "attendance_evidence_sync_channel"

    fun createNotificationChannel(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannels(
            listOf(
                NotificationChannel(
                    REMINDER_CHANNEL_ID,
                    "Attendance Reminder",
                    NotificationManager.IMPORTANCE_HIGH
                ),
                NotificationChannel(
                    SESSION_ALERT_CHANNEL_ID,
                    "Attendance Session Alert",
                    NotificationManager.IMPORTANCE_HIGH
                ),
                NotificationChannel(
                    EVIDENCE_SYNC_CHANNEL_ID,
                    "Attendance Evidence Sync",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        )
    }

    fun showGeofenceNotification(context: Context, eventType: String, locationName: String) {
        if (!canPostNotifications(context)) {
            Log.w(TAG, "Session alert notification skipped because POST_NOTIFICATIONS is not granted")
            return
        }
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Generate message based on event type and location name
        val message = when (eventType) {
            "ENTER" -> "Anda telah memasuki area: $locationName"
            "EXIT" -> "Anda telah meninggalkan area: $locationName"
            else -> "Terdeteksi event lokasi."
        }

        // Intent untuk membuka MainActivity dan menavigasi ke AttendanceScreen
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Add extra to indicate we should navigate to attendance screen
            putExtra("navigate_to_attendance", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, SESSION_ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.notifications_24px)
            .setContentTitle("Pemberitahuan Area Presensi")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    fun showCheckInReminderNotification(context: Context, locationName: String) {
        if (!canPostNotifications(context)) {
            Log.w(TAG, "Reminder notification skipped because POST_NOTIFICATIONS is not granted")
            return
        }
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to_attendance", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.notifications_24px)
            .setContentTitle("Pengingat Check-in")
            .setContentText("Anda berada di area: $locationName. Jangan lupa check-in.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun canPostNotifications(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }
}
