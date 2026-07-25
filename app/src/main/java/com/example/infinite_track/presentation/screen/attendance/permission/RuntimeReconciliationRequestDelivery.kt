package com.example.infinite_track.presentation.screen.attendance.permission

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull

data class RuntimeReconciliationRequest(
    val token: Long
)

internal suspend fun deliverRuntimeReconciliationRequests(
    requests: Flow<RuntimeReconciliationRequest?>,
    reconcile: suspend () -> Unit,
    acknowledge: (Long) -> Unit
) {
    requests.filterNotNull().collect { request ->
        reconcile()
        acknowledge(request.token)
    }
}
