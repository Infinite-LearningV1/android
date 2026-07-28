package com.example.infinite_track.data.worker

import android.content.ContextWrapper
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.work.Data
import androidx.work.ForegroundUpdater
import androidx.work.ProgressUpdater
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.impl.utils.taskexecutor.SerialExecutor
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import com.example.infinite_track.data.soucre.local.preferences.UserPreference
import com.example.infinite_track.data.soucre.network.request.LocationEventRequest
import com.example.infinite_track.domain.manager.SessionManager
import com.example.infinite_track.domain.model.attendance.ActiveAttendanceSession
import com.example.infinite_track.domain.model.attendance.AttendanceRequestModel
import com.example.infinite_track.domain.model.attendance.AttendanceSessionState
import com.example.infinite_track.domain.model.attendance.CheckinWindow
import com.example.infinite_track.domain.model.attendance.Location
import com.example.infinite_track.domain.model.attendance.TodayStatus
import com.example.infinite_track.domain.model.auth.LoginCredentials
import com.example.infinite_track.domain.model.auth.ReauthReason
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.model.booking.BookingHistoryPage
import com.example.infinite_track.domain.model.geofence.GeofenceDisabledReason
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeMode
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeReadiness
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeResult
import com.example.infinite_track.domain.model.geofence.NotificationReadiness
import com.example.infinite_track.domain.model.geofence.RegistrationReadiness
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.repository.AttendanceRepository
import com.example.infinite_track.domain.repository.AuthRefreshResult
import com.example.infinite_track.domain.repository.AuthRepository
import com.example.infinite_track.domain.repository.BookingRepository
import com.example.infinite_track.domain.repository.GeofenceRuntimeRepository
import com.example.infinite_track.domain.repository.ProfileSyncResult
import com.example.infinite_track.domain.use_case.attendance.ResolveAuthoritativeTargetLocationUseCase
import com.example.infinite_track.domain.use_case.auth.ForegroundSessionValidationResult
import com.example.infinite_track.domain.use_case.auth.GetLoggedInUserUseCase
import com.example.infinite_track.domain.use_case.auth.RefreshAttendanceProfileUseCase
import com.example.infinite_track.domain.use_case.auth.ValidateForegroundSessionUseCase
import com.example.infinite_track.domain.use_case.booking.ResolveTodayWfaBookingStateUseCase
import com.example.infinite_track.domain.use_case.geofence.BuildReminderGeofenceCandidatesUseCase
import com.example.infinite_track.domain.use_case.geofence.RefreshAndReconcileGeofenceRuntimeUseCase
import com.example.infinite_track.domain.use_case.geofence.ResolveGeofenceRuntimeModeUseCase
import java.util.UUID
import java.util.concurrent.Executor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class GeofenceReconciliationWorkerTest {

    @Test
    fun `reconciled runtime completes successfully`() = runTest {
        val result = worker { ForegroundSessionValidationResult.Valid }.doWork()

        assertEquals(androidx.work.ListenableWorker.Result.success(), result)
    }

    @Test
    fun `unavailable auth completes successfully`() = runTest {
        val result = worker {
            ForegroundSessionValidationResult.ReauthRequired(ReauthReason.REFRESH_REVOKED)
        }.doWork()

        assertEquals(androidx.work.ListenableWorker.Result.success(), result)
    }

    @Test
    fun `unavailable backend retries`() = runTest {
        val result = worker {
            ForegroundSessionValidationResult.TemporaryFailure("offline", null)
        }.doWork()

        assertEquals(androidx.work.ListenableWorker.Result.retry(), result)
    }

    @Test
    fun `cancellation is rethrown`() = runTest {
        try {
            worker { throw CancellationException("cancelled") }.doWork()
            fail("Expected cancellation to be rethrown")
        } catch (_: CancellationException) {
        }
    }

    private fun worker(
        validation: suspend () -> ForegroundSessionValidationResult
    ) = GeofenceReconciliationWorker(
        context = ContextWrapper(null),
        params = workerParameters(),
        refreshAndReconcile = coordinator(validation)
    )

    private fun coordinator(
        validation: suspend () -> ForegroundSessionValidationResult
    ): RefreshAndReconcileGeofenceRuntimeUseCase {
        val auth = FakeAuthRepository()
        val targetResolver = ResolveAuthoritativeTargetLocationUseCase()
        return RefreshAndReconcileGeofenceRuntimeUseCase(
            attendanceRepository = FakeAttendanceRepository(),
            refreshProfile = RefreshAttendanceProfileUseCase(auth),
            getLoggedInUser = GetLoggedInUserUseCase(auth),
            resolveBooking = ResolveTodayWfaBookingStateUseCase(FakeBookingRepository()),
            validateSession = FakeValidateForegroundSessionUseCase(validation),
            resolveMode = ResolveGeofenceRuntimeModeUseCase(
                buildCandidates = BuildReminderGeofenceCandidatesUseCase(targetResolver),
                resolveTarget = targetResolver
            ),
            runtimeRepository = FakeGeofenceRuntimeRepository()
        )
    }

    private fun workerParameters(): WorkerParameters = WorkerParameters(
        UUID.randomUUID(),
        Data.EMPTY,
        emptySet(),
        WorkerParameters.RuntimeExtras(),
        0,
        0,
        DIRECT_EXECUTOR,
        DIRECT_TASK_EXECUTOR,
        WorkerFactory.getDefaultWorkerFactory(),
        ProgressUpdater { _, _, _ -> error("Progress is not used") },
        ForegroundUpdater { _, _, _ -> error("Foreground work is not used") }
    )

    private class FakeAttendanceRepository : AttendanceRepository {
        override suspend fun getTodayStatus(forceRefresh: Boolean): Result<TodayStatus> =
            Result.success(
                TodayStatus(
                    canCheckIn = false,
                    canCheckOut = false,
                    checkedInAt = null,
                    checkedOutAt = null,
                    activeMode = "",
                    activeLocation = Location(
                        locationId = 7,
                        description = "Office",
                        coordinate = GeoCoordinate(-0.89, 119.87),
                        radius = 100,
                        category = "Office"
                    ),
                    todayDate = "2026-07-25",
                    isHoliday = false,
                    holidayCheckinEnabled = false,
                    currentTime = "09:00:00",
                    checkinWindow = CheckinWindow(startTime = "08:00:00", endTime = "10:00:00"),
                    checkoutAutoTime = "17:00:00",
                    attendanceSessionState = AttendanceSessionState(3, "completed", "Completed"),
                    activeAttendanceId = null
                )
            )

        override suspend fun clearTodayStatusCache() = error("Not used")
        override suspend fun checkIn(
            request: AttendanceRequestModel
        ): com.example.infinite_track.domain.model.attendance.AttendanceSubmitResult =
            error("Not used")

        override suspend fun checkOut(
            attendanceId: Int,
            latitude: Double,
            longitude: Double
        ): com.example.infinite_track.domain.model.attendance.AttendanceSubmitResult = error("Not used")

        override suspend fun getActiveAttendanceId(): Int? = error("Not used")
        override suspend fun sendLocationEvent(request: LocationEventRequest): Result<Unit> =
            error("Not used")
    }

    private class FakeAuthRepository : AuthRepository {
        override suspend fun refreshSession(): Result<AuthRefreshResult> = error("Not used")
        override suspend fun login(credentials: LoginCredentials): Result<UserModel> = error("Not used")

        override suspend fun syncUserProfile(): ProfileSyncResult = ProfileSyncResult.Success(
            UserModel(
                id = 21,
                fullName = "Test User",
                email = "test.user@example.test",
                roleName = "Employee",
                positionName = null,
                programName = null,
                divisionName = null,
                nipNim = "test-21",
                phone = null,
                photoUrl = null,
                photoUpdatedAt = null,
                latitude = -0.90,
                longitude = 119.86,
                radius = 100,
                locationDescription = "Home",
                locationCategoryName = "WFH"
            )
        )

        @Suppress("DEPRECATION")
        override suspend fun logout(): Result<Unit> = error("Not used")
        override fun getLoggedInUser(): Flow<UserModel?> = emptyFlow()
        override suspend fun saveFaceEmbedding(userId: Int, embedding: ByteArray): Result<Unit> =
            error("Not used")
    }

    private class FakeBookingRepository : BookingRepository {
        override suspend fun getBookingHistory(
            status: String?,
            page: Int,
            limit: Int,
            sortBy: String,
            sortOrder: String
        ): Result<BookingHistoryPage> = Result.success(BookingHistoryPage(emptyList()))

        override suspend fun getWfaRequestConfig():
            com.example.infinite_track.domain.model.booking.WfaRequestConfigResult = error("Not used")

        override suspend fun submitWfaRequest(
            command: com.example.infinite_track.domain.model.booking.SubmitWfaRequestCommand
        ): com.example.infinite_track.domain.model.booking.WfaRequestResult = error("Not used")

    }

    private class FakeGeofenceRuntimeRepository : GeofenceRuntimeRepository {
        override suspend fun reconcile(mode: GeofenceRuntimeMode): GeofenceRuntimeResult =
            GeofenceRuntimeResult.Applied(mode, generation = 1, logicalIds = emptySet())

        override suspend fun clearForLogout(): GeofenceRuntimeResult =
            GeofenceRuntimeResult.Applied(
                GeofenceRuntimeMode.Disabled(GeofenceDisabledReason.LOGGED_OUT),
                generation = 1,
                logicalIds = emptySet()
            )

        override fun observeReadiness(): Flow<GeofenceRuntimeReadiness> = flowOf(
            GeofenceRuntimeReadiness(
                registration = RegistrationReadiness.Ready,
                notification = NotificationReadiness.READY
            )
        )
    }

    private class FakeValidateForegroundSessionUseCase(
        private val validation: suspend () -> ForegroundSessionValidationResult
    ) : ValidateForegroundSessionUseCase(
        authRepository = FakeAuthRepository(),
        userPreference = UserPreference(UnusedDataStore),
        sessionManager = SessionManager()
    ) {
        override suspend fun invoke(): ForegroundSessionValidationResult = validation()
    }

    private object UnusedDataStore : DataStore<Preferences> {
        override val data: Flow<Preferences> = emptyFlow()

        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences
        ): Preferences = error("Not used")
    }

    private companion object {
        val DIRECT_EXECUTOR = Executor { command -> command.run() }
        val DIRECT_SERIAL_EXECUTOR = object : SerialExecutor {
            override fun execute(command: Runnable) = command.run()
            override fun hasPendingTasks() = false
        }
        val DIRECT_TASK_EXECUTOR = object : TaskExecutor {
            override fun getMainThreadExecutor(): Executor = DIRECT_EXECUTOR
            override fun getSerialTaskExecutor(): SerialExecutor = DIRECT_SERIAL_EXECUTOR
        }
    }
}
