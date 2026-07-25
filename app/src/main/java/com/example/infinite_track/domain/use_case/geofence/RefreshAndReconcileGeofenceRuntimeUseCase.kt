package com.example.infinite_track.domain.use_case.geofence

import com.example.infinite_track.domain.model.attendance.TodayStatus
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.model.geofence.BackendTruthSource
import com.example.infinite_track.domain.model.geofence.GeofenceReconcileReason
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeFailure
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeInputs
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeReadiness
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeModeResolution
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeResult
import com.example.infinite_track.domain.model.wfa.WfaBookingForDate
import com.example.infinite_track.domain.repository.AttendanceRepository
import com.example.infinite_track.domain.repository.GeofenceRuntimeRepository
import com.example.infinite_track.domain.repository.ProfileSyncResult
import com.example.infinite_track.domain.use_case.auth.ForegroundSessionValidationResult
import com.example.infinite_track.domain.use_case.auth.GetLoggedInUserUseCase
import com.example.infinite_track.domain.use_case.auth.RefreshAttendanceProfileUseCase
import com.example.infinite_track.domain.use_case.auth.ValidateForegroundSessionUseCase
import com.example.infinite_track.domain.use_case.booking.ResolveTodayWfaBookingStateUseCase
import kotlinx.coroutines.flow.first
import javax.inject.Inject

sealed interface RefreshAndReconcileGeofenceRuntimeResult {
    data class Reconciled(
        val todayStatus: TodayStatus,
        val profile: UserModel?,
        val wfaBooking: WfaBookingForDate,
        val readiness: GeofenceRuntimeReadiness,
        val resolution: GeofenceRuntimeModeResolution,
        val runtime: GeofenceRuntimeResult
    ) : RefreshAndReconcileGeofenceRuntimeResult

    data class BackendUnavailable(
        val failure: GeofenceRuntimeFailure.BackendTruthUnavailable
    ) : RefreshAndReconcileGeofenceRuntimeResult

    data class AuthUnavailable(
        val runtime: GeofenceRuntimeResult
    ) : RefreshAndReconcileGeofenceRuntimeResult
}

class RefreshAndReconcileGeofenceRuntimeUseCase @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val refreshProfile: RefreshAttendanceProfileUseCase,
    private val getLoggedInUser: GetLoggedInUserUseCase,
    private val resolveBooking: ResolveTodayWfaBookingStateUseCase,
    private val validateSession: ValidateForegroundSessionUseCase,
    private val resolveMode: ResolveGeofenceRuntimeModeUseCase,
    private val runtimeRepository: GeofenceRuntimeRepository
) {
    suspend operator fun invoke(
        reason: GeofenceReconcileReason
    ): RefreshAndReconcileGeofenceRuntimeResult {
        if (reason == GeofenceReconcileReason.BOOT_RECOVERY) {
            when (validateSession()) {
                ForegroundSessionValidationResult.Valid -> Unit
                ForegroundSessionValidationResult.Skipped,
                is ForegroundSessionValidationResult.ReauthRequired -> {
                    return RefreshAndReconcileGeofenceRuntimeResult.AuthUnavailable(
                        runtimeRepository.clearForLogout()
                    )
                }

                is ForegroundSessionValidationResult.TemporaryFailure -> {
                    return backendUnavailable(BackendTruthSource.SESSION_VALIDATION)
                }
            }
        }

        val todayStatus = attendanceRepository.getTodayStatus(forceRefresh = true)
            .getOrElse { return backendUnavailable(BackendTruthSource.STATUS_TODAY) }

        val profileResult = refreshProfile()
        val profileWarnings = mutableListOf<GeofenceRuntimeFailure>()
        val profile = when (profileResult) {
            is ProfileSyncResult.Success -> profileResult.user
            is ProfileSyncResult.Unauthorized -> {
                return RefreshAndReconcileGeofenceRuntimeResult.AuthUnavailable(
                    runtimeRepository.clearForLogout()
                )
            }

            is ProfileSyncResult.TemporaryFailure -> {
                profileWarnings += GeofenceRuntimeFailure.BackendTruthUnavailable(
                    BackendTruthSource.PROFILE
                )
                getLoggedInUser().first()
            }
        }

        val wfaBooking = resolveBooking(todayStatus.todayDate)
        val readiness = runtimeRepository.observeReadiness().first()
        val resolved = resolveMode(
            GeofenceRuntimeInputs(
                todayStatus = todayStatus,
                profile = profile,
                approvedWfaBooking = wfaBooking,
                readiness = readiness,
                reason = reason
            )
        )
        val resolution = resolved.copy(warnings = resolved.warnings + profileWarnings)
        val runtime = runtimeRepository.reconcile(resolution.mode)

        return RefreshAndReconcileGeofenceRuntimeResult.Reconciled(
            todayStatus = todayStatus,
            profile = profile,
            wfaBooking = wfaBooking,
            readiness = readiness,
            resolution = resolution,
            runtime = runtime
        )
    }

    private fun backendUnavailable(
        source: BackendTruthSource
    ) = RefreshAndReconcileGeofenceRuntimeResult.BackendUnavailable(
        GeofenceRuntimeFailure.BackendTruthUnavailable(source)
    )
}
