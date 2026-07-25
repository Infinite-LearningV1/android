package com.example.infinite_track.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.infinite_track.domain.model.geofence.GeofenceReconcileReason
import com.example.infinite_track.domain.use_case.geofence.RefreshAndReconcileGeofenceRuntimeResult
import com.example.infinite_track.domain.use_case.geofence.RefreshAndReconcileGeofenceRuntimeUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class GeofenceReconciliationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val refreshAndReconcile: RefreshAndReconcileGeofenceRuntimeUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = when (
        refreshAndReconcile(GeofenceReconcileReason.BOOT_RECOVERY)
    ) {
        is RefreshAndReconcileGeofenceRuntimeResult.Reconciled,
        is RefreshAndReconcileGeofenceRuntimeResult.AuthUnavailable -> Result.success()
        is RefreshAndReconcileGeofenceRuntimeResult.BackendUnavailable -> Result.retry()
    }
}
