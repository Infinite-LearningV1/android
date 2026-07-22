package com.example.infinite_track.data.repository.attendance

import com.example.infinite_track.data.soucre.local.permission.AttendancePermissionDataSource
import com.example.infinite_track.data.soucre.local.permission.AttendancePermissionPlatformSnapshot
import com.example.infinite_track.data.soucre.local.permission.AttendancePermissionSnapshotResult
import com.example.infinite_track.data.soucre.local.permission.DeviceLocationPlatformStatus
import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccess
import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccessStatus
import com.example.infinite_track.domain.model.attendance.permission.AttendancePermissionFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class AttendancePermissionRepositoryImplTest {
    @Test fun refreshPublishesMappedSnapshot() = runTest {
        val repository = AttendancePermissionRepositoryImpl(FakeDataSource(success()))
        repository.refreshReadiness()
        assertTrue(repository.observeReadiness().first().canEnterAttendance)
    }

    @Test fun concurrentRefreshesAreSerialized() = runTest {
        val source = FakeDataSource(success(), delayMillis = 50)
        val repository = AttendancePermissionRepositoryImpl(source)
        listOf(async(Dispatchers.Default) { repository.refreshReadiness() }, async(Dispatchers.Default) { repository.refreshReadiness() }).awaitAll()
        assertEquals(1, source.maximumConcurrentReads.get())
    }

    @Test fun requiredFailureWithoutPriorContentPublishesUnavailableReadiness() = runTest {
        val repository = AttendancePermissionRepositoryImpl(FakeDataSource(failure(setOf(AttendanceAccess.CAMERA))))
        repository.refreshReadiness()
        val readiness = repository.observeReadiness().first()
        assertFalse(readiness.canEnterAttendance)
        assertEquals(AttendanceAccessStatus.ACTION_REQUIRED, readiness.statusOf(AttendanceAccess.CAMERA))
    }

    @Test fun requiredFailureRetainsPriorEntriesAndBlocksEntry() = runTest {
        val source = FakeDataSource(success())
        val repository = AttendancePermissionRepositoryImpl(source)
        repository.refreshReadiness()
        source.result = failure(setOf(AttendanceAccess.CAMERA))
        repository.refreshReadiness()
        val readiness = repository.observeReadiness().first()
        assertEquals(AttendanceAccessStatus.ACTION_REQUIRED, readiness.statusOf(AttendanceAccess.CAMERA))
        assertEquals(AttendanceAccessStatus.READY, readiness.statusOf(AttendanceAccess.PRECISE_LOCATION))
        assertFalse(readiness.canEnterAttendance)
    }

    @Test fun optionalOnlyFailureRetainsPriorEntriesWithoutBlockingEntry() = runTest {
        val source = FakeDataSource(success())
        val repository = AttendancePermissionRepositoryImpl(source)
        repository.refreshReadiness()
        source.result = failure(setOf(AttendanceAccess.NOTIFICATION))
        repository.refreshReadiness()
        val readiness = repository.observeReadiness().first()
        assertEquals(AttendanceAccessStatus.DEGRADED, readiness.statusOf(AttendanceAccess.NOTIFICATION))
        assertTrue(readiness.canEnterAttendance)
    }

    @Test fun laterSuccessClearsPriorInspectionIssues() = runTest {
        val source = FakeDataSource(failure(setOf(AttendanceAccess.CAMERA)))
        val repository = AttendancePermissionRepositoryImpl(source)
        repository.refreshReadiness()
        source.result = success()
        repository.refreshReadiness()
        assertTrue(repository.observeReadiness().first().inspectionIssues.isEmpty())
    }

    private fun success() = AttendancePermissionSnapshotResult.Success(
        AttendancePermissionPlatformSnapshot(34, true, true, true, true, true, DeviceLocationPlatformStatus.ENABLED)
    )

    private fun failure(affectedAccesses: Set<AttendanceAccess>) = AttendancePermissionSnapshotResult.Failure(
        AttendancePermissionFailure.PLATFORM_STATE_UNAVAILABLE, affectedAccesses
    )

    private class FakeDataSource(
        initial: AttendancePermissionSnapshotResult,
        private val delayMillis: Long = 0
    ) : AttendancePermissionDataSource {
        var result = initial
        private val activeReads = AtomicInteger()
        val maximumConcurrentReads = AtomicInteger()
        override fun readSnapshot(): AttendancePermissionSnapshotResult {
            val active = activeReads.incrementAndGet()
            maximumConcurrentReads.updateAndGet { maxOf(it, active) }
            try { if (delayMillis > 0) Thread.sleep(delayMillis); return result } finally { activeReads.decrementAndGet() }
        }
    }
}
