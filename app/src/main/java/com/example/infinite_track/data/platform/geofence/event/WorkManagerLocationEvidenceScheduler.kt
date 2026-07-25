package com.example.infinite_track.data.platform.geofence.event

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.infinite_track.data.worker.LocationEventWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerLocationEvidenceScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationEvidenceScheduler {

    override fun enqueue(
        attendanceId: Int,
        logicalId: String,
        transition: GeofenceTransition,
        occurredAt: Instant
    ) {
        val request = OneTimeWorkRequestBuilder<LocationEventWorker>()
            .setInputData(
                Data.Builder()
                    .putString(LocationEventWorker.KEY_EVENT_TYPE, transition.name)
                    .putString(LocationEventWorker.KEY_LOCATION_ID, logicalId)
                    .putString(LocationEventWorker.KEY_EVENT_TIMESTAMP, occurredAt.toString())
                    .putInt(LocationEventWorker.KEY_ACTIVE_ATTENDANCE_ID, attendanceId)
                    .build()
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag("location_event_$logicalId")
            .build()
        val uniqueName = "location_event_${attendanceId}_${logicalId}_${transition.name}"
        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueName,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
