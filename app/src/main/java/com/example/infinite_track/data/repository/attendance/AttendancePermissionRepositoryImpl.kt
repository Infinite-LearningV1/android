package com.example.infinite_track.data.repository.attendance

import com.example.infinite_track.data.mapper.attendance.toAttendancePermissionReadiness
import com.example.infinite_track.data.soucre.local.permission.AttendancePermissionDataSource
import com.example.infinite_track.data.soucre.local.permission.AttendancePermissionSnapshotResult
import com.example.infinite_track.domain.model.attendance.permission.AttendancePermissionInspectionIssue
import com.example.infinite_track.domain.model.attendance.permission.AttendancePermissionReadiness
import com.example.infinite_track.domain.repository.AttendancePermissionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttendancePermissionRepositoryImpl @Inject constructor(
    private val dataSource: AttendancePermissionDataSource
) : AttendancePermissionRepository {
    private val state = MutableStateFlow<AttendancePermissionReadiness?>(null)
    private val refreshMutex = Mutex()

    override fun observeReadiness(): Flow<AttendancePermissionReadiness> = state.filterNotNull()

    override suspend fun refreshReadiness() = refreshMutex.withLock {
        when (val result = dataSource.readSnapshot()) {
            is AttendancePermissionSnapshotResult.Success -> state.value = result.snapshot.toAttendancePermissionReadiness()
            is AttendancePermissionSnapshotResult.Failure -> {
                val issue = AttendancePermissionInspectionIssue(result.failure, result.affectedAccesses)
                state.value = state.value?.applyingInspectionIssues(listOf(issue))
                    ?: AttendancePermissionReadiness.unavailable(result.failure, result.affectedAccesses)
            }
        }
    }
}
