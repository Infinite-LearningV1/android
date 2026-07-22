package com.example.infinite_track.presentation.screen.attendance.permission

import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccess
import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccessReadiness
import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccessReason
import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccessRequirement
import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccessStatus
import com.example.infinite_track.domain.model.attendance.permission.AttendancePermissionNextAction
import com.example.infinite_track.domain.model.attendance.permission.AttendancePermissionReadiness
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import javax.inject.Inject

class AttendancePermissionReadinessUiMapper @Inject constructor() {
    fun map(
        readiness: AttendancePermissionReadiness,
        nextAction: AttendancePermissionNextAction,
        isRefreshing: Boolean = false
    ): AttendancePermissionReadinessUiState {
        val items = AttendanceAccess.entries.map { access ->
            itemFor(readiness.entryOf(access) ?: AttendanceAccessReadiness(
                access = access,
                status = AttendanceAccessStatus.ACTION_REQUIRED
            ))
        }
        val hasRequiredIssue = readiness.inspectionIssues.any { it.blocksManualAttendance }
        val hasOptionalIssue = readiness.inspectionIssues.any { issue ->
            issue.affectedAccesses.isNotEmpty() && issue.affectedAccesses.all {
                it.requirement == AttendanceAccessRequirement.OPTIONAL
            }
        }
        return AttendancePermissionReadinessUiState(
            isLoading = false,
            isRefreshing = isRefreshing,
            requiredItems = items.filter { it.access.requirement == AttendanceAccessRequirement.REQUIRED },
            optionalItems = items.filter { it.access.requirement == AttendanceAccessRequirement.OPTIONAL },
            requiredReadyCount = readiness.requiredReadyCount,
            requiredTotalCount = readiness.requiredTotalCount,
            canContinue = readiness.canEnterAttendance,
            primaryActionLabel = if (readiness.canEnterAttendance) "Lanjut ke Mode Kerja" else "Lanjutkan Setup",
            primaryActionEnabled = !hasRequiredIssue || nextAction is AttendancePermissionNextAction.RetryRefresh,
            contextualGuidance = if (hasOptionalIssue && !hasRequiredIssue) optionalGuidance() else null,
            recoverableFailure = if (hasRequiredIssue) requiredFailureGuidance() else null
        )
    }

    private fun itemFor(entry: AttendanceAccessReadiness): PermissionItemUiModel {
        val copy = copyFor(entry)
        return PermissionItemUiModel(
            access = entry.access,
            title = titleFor(entry.access),
            supportingText = copy.supportingText,
            requirementLabel = if (entry.access.requirement == AttendanceAccessRequirement.REQUIRED) "Wajib" else "Opsional",
            statusLabel = copy.statusLabel,
            actionLabel = copy.actionLabel,
            iconKey = iconFor(entry.access),
            semantic = copy.semantic,
            stateDescription = "${titleFor(entry.access)}, ${if (entry.access.requirement == AttendanceAccessRequirement.REQUIRED) "wajib" else "opsional"}, ${copy.statusLabel}${copy.actionLabel?.let { ", aksi $it" }.orEmpty()}",
            isReady = entry.status == AttendanceAccessStatus.READY || entry.status == AttendanceAccessStatus.NOT_REQUIRED_ON_DEVICE
        )
    }

    private fun copyFor(entry: AttendanceAccessReadiness): ItemCopy = when (entry.status) {
        AttendanceAccessStatus.READY -> ItemCopy("Siap", readyText(entry.access), null, InfiniteSemantic.Success)
        AttendanceAccessStatus.NOT_REQUIRED_ON_DEVICE -> ItemCopy("Tidak diperlukan di perangkat ini", "Fitur ini tidak memerlukan izin pada perangkat Anda.", null, InfiniteSemantic.Info)
        AttendanceAccessStatus.DEVICE_LOCATION_DISABLED -> ItemCopy("GPS belum aktif", "Nyalakan Location atau GPS perangkat untuk melanjutkan absensi.", "Buka pengaturan", InfiniteSemantic.Warning)
        AttendanceAccessStatus.PERMANENTLY_DENIED -> ItemCopy("Izin diblokir", "Izin perlu diaktifkan dari pengaturan aplikasi.", "Buka pengaturan", InfiniteSemantic.Error)
        AttendanceAccessStatus.DENIED -> ItemCopy("Izin ditolak", "Izin diperlukan agar fitur ini dapat digunakan.", "Minta izin", InfiniteSemantic.Warning)
        AttendanceAccessStatus.DEGRADED -> ItemCopy("Terbatas", "Fitur opsional belum aktif. Absensi manual tetap dapat dilanjutkan.", "Aktifkan", InfiniteSemantic.Warning)
        AttendanceAccessStatus.ACTION_REQUIRED -> if (entry.reason == AttendanceAccessReason.APPROXIMATE_LOCATION_ONLY) {
            ItemCopy("Lokasi presisi diperlukan", "Lokasi perkiraan aktif. Izinkan lokasi presisi untuk absensi.", "Minta izin", InfiniteSemantic.Warning)
        } else {
            ItemCopy("Perlu diatur", requiredText(entry.access), "Minta izin", InfiniteSemantic.Primary)
        }
    }

    private fun titleFor(access: AttendanceAccess) = when (access) {
        AttendanceAccess.PRECISE_LOCATION -> "Lokasi presisi"
        AttendanceAccess.CAMERA -> "Kamera"
        AttendanceAccess.DEVICE_LOCATION -> "Lokasi perangkat aktif"
        AttendanceAccess.NOTIFICATION -> "Notifikasi pengingat"
        AttendanceAccess.BACKGROUND_LOCATION -> "Lokasi latar belakang"
    }

    private fun iconFor(access: AttendanceAccess) = when (access) {
        AttendanceAccess.PRECISE_LOCATION -> PermissionIconKey.LOCATION
        AttendanceAccess.CAMERA -> PermissionIconKey.CAMERA
        AttendanceAccess.DEVICE_LOCATION -> PermissionIconKey.DEVICE_LOCATION
        AttendanceAccess.NOTIFICATION -> PermissionIconKey.NOTIFICATION
        AttendanceAccess.BACKGROUND_LOCATION -> PermissionIconKey.BACKGROUND_LOCATION
    }

    private fun readyText(access: AttendanceAccess) = when (access) {
        AttendanceAccess.PRECISE_LOCATION -> "Lokasi presisi siap digunakan untuk absensi."
        AttendanceAccess.CAMERA -> "Kamera siap untuk verifikasi wajah."
        AttendanceAccess.DEVICE_LOCATION -> "Location atau GPS perangkat sudah aktif."
        AttendanceAccess.NOTIFICATION -> "Pengingat absensi sudah aktif."
        AttendanceAccess.BACKGROUND_LOCATION -> "Pemantauan geofence latar belakang sudah aktif."
    }

    private fun requiredText(access: AttendanceAccess) = when (access) {
        AttendanceAccess.PRECISE_LOCATION -> "Izinkan lokasi presisi untuk absensi."
        AttendanceAccess.CAMERA -> "Izinkan kamera untuk verifikasi wajah."
        AttendanceAccess.DEVICE_LOCATION -> "Aktifkan Location atau GPS perangkat."
        AttendanceAccess.NOTIFICATION -> "Aktifkan pengingat absensi bila diperlukan."
        AttendanceAccess.BACKGROUND_LOCATION -> "Aktifkan pemantauan geofence bila diperlukan."
    }

    private fun requiredFailureGuidance() = PermissionGuidanceUiModel(
        title = "Status akses belum dapat diperiksa",
        message = "Periksa kembali status akses sebelum melanjutkan absensi.",
        semantic = InfiniteSemantic.Error,
        actionLabel = "Coba lagi",
        action = PermissionGuidanceAction.RETRY_REFRESH
    )

    private fun optionalGuidance() = PermissionGuidanceUiModel(
        title = "Pengingat opsional belum dapat diperiksa",
        message = "Absensi manual tetap dapat dilanjutkan. Coba periksa lagi nanti untuk mengaktifkan pengingat.",
        semantic = InfiniteSemantic.Warning
    )

    private data class ItemCopy(
        val statusLabel: String,
        val supportingText: String,
        val actionLabel: String?,
        val semantic: InfiniteSemantic
    )
}
