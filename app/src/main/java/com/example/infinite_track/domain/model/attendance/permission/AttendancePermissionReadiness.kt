package com.example.infinite_track.domain.model.attendance.permission

enum class AttendanceAccessRequirement { REQUIRED, OPTIONAL }

enum class AttendanceAccess(val requirement: AttendanceAccessRequirement) {
    PRECISE_LOCATION(AttendanceAccessRequirement.REQUIRED),
    CAMERA(AttendanceAccessRequirement.REQUIRED),
    DEVICE_LOCATION(AttendanceAccessRequirement.REQUIRED),
    NOTIFICATION(AttendanceAccessRequirement.OPTIONAL),
    BACKGROUND_LOCATION(AttendanceAccessRequirement.OPTIONAL)
}

enum class AttendanceAccessStatus {
    READY,
    ACTION_REQUIRED,
    DENIED,
    PERMANENTLY_DENIED,
    DEGRADED,
    NOT_REQUIRED_ON_DEVICE,
    DEVICE_LOCATION_DISABLED
}

enum class AttendanceAccessReason { NONE, APPROXIMATE_LOCATION_ONLY }

enum class AttendanceAccessRecovery {
    NONE,
    REQUEST_PERMISSION,
    OPEN_APPLICATION_SETTINGS,
    OPEN_DEVICE_LOCATION_SETTINGS
}

enum class AttendancePermissionRequestOutcome {
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED
}

enum class AttendancePermissionFailure {
    PLATFORM_STATE_UNAVAILABLE,
    DEVICE_LOCATION_STATUS_UNAVAILABLE,
    SETTINGS_INTENT_UNAVAILABLE,
    UNKNOWN
}

enum class AttendanceOptionalCapabilitySummary { READY, DEGRADED, NOT_REQUIRED }

data class AttendancePermissionInspectionIssue(
    val failure: AttendancePermissionFailure,
    val affectedAccesses: Set<AttendanceAccess>
) {
    val blocksManualAttendance: Boolean
        get() = affectedAccesses.any {
            it.requirement == AttendanceAccessRequirement.REQUIRED
        }
}

data class AttendanceAccessReadiness(
    val access: AttendanceAccess,
    val status: AttendanceAccessStatus,
    val reason: AttendanceAccessReason = AttendanceAccessReason.NONE,
    val recovery: AttendanceAccessRecovery = AttendanceAccessRecovery.NONE
)

data class AttendancePermissionReadiness(
    val entries: List<AttendanceAccessReadiness>,
    val inspectionIssues: List<AttendancePermissionInspectionIssue> = emptyList()
) {
    private val requiredAccesses: List<AttendanceAccess>
        get() = AttendanceAccess.entries.filter {
            it.requirement == AttendanceAccessRequirement.REQUIRED
        }

    val requiredReadyCount: Int
        get() = requiredAccesses.count { statusOf(it) == AttendanceAccessStatus.READY }

    val requiredTotalCount: Int
        get() = requiredAccesses.size

    val canEnterAttendance: Boolean
        get() = inspectionIssues.none { it.blocksManualAttendance } &&
            requiredAccesses.all { access ->
                entryOf(access)?.status == AttendanceAccessStatus.READY
            }

    val optionalCapabilitySummary: AttendanceOptionalCapabilitySummary
        get() {
            val optional = entries.filter {
                it.access.requirement == AttendanceAccessRequirement.OPTIONAL
            }
            return when {
                inspectionIssues.any { issue ->
                    issue.affectedAccesses.any {
                        it.requirement == AttendanceAccessRequirement.OPTIONAL
                    }
                } -> AttendanceOptionalCapabilitySummary.DEGRADED
                optional.isNotEmpty() && optional.all {
                    it.status == AttendanceAccessStatus.NOT_REQUIRED_ON_DEVICE
                } -> AttendanceOptionalCapabilitySummary.NOT_REQUIRED
                optional.any {
                    it.status != AttendanceAccessStatus.READY &&
                        it.status != AttendanceAccessStatus.NOT_REQUIRED_ON_DEVICE
                } -> AttendanceOptionalCapabilitySummary.DEGRADED
                else -> AttendanceOptionalCapabilitySummary.READY
            }
        }

    fun entryOf(access: AttendanceAccess): AttendanceAccessReadiness? =
        entries.firstOrNull { it.access == access }

    fun statusOf(access: AttendanceAccess): AttendanceAccessStatus =
        entryOf(access)?.status ?: AttendanceAccessStatus.ACTION_REQUIRED

    fun applyingRequestOutcomes(
        outcomes: Map<AttendanceAccess, AttendancePermissionRequestOutcome>
    ): AttendancePermissionReadiness = copy(
        entries = entries.map { entry ->
            if (
                entry.status == AttendanceAccessStatus.READY ||
                entry.status == AttendanceAccessStatus.NOT_REQUIRED_ON_DEVICE
            ) {
                entry
            } else {
                when (outcomes[entry.access]) {
                    AttendancePermissionRequestOutcome.DENIED ->
                        entry.copy(status = AttendanceAccessStatus.DENIED)
                    AttendancePermissionRequestOutcome.PERMANENTLY_DENIED -> entry.copy(
                        status = AttendanceAccessStatus.PERMANENTLY_DENIED,
                        recovery = AttendanceAccessRecovery.OPEN_APPLICATION_SETTINGS
                    )
                    AttendancePermissionRequestOutcome.GRANTED,
                    null -> entry
                }
            }
        }
    )

    fun applyingInspectionIssues(
        issues: List<AttendancePermissionInspectionIssue>
    ): AttendancePermissionReadiness = copy(
        entries = entries.map { entry ->
            if (issues.any { entry.access in it.affectedAccesses }) {
                entry.copy(
                    status = if (
                        entry.access.requirement == AttendanceAccessRequirement.REQUIRED
                    ) {
                        AttendanceAccessStatus.ACTION_REQUIRED
                    } else {
                        AttendanceAccessStatus.DEGRADED
                    },
                    recovery = AttendanceAccessRecovery.NONE
                )
            } else {
                entry
            }
        },
        inspectionIssues = issues
    )

    companion object {
        fun unavailable(
            failure: AttendancePermissionFailure,
            affectedAccesses: Set<AttendanceAccess> = AttendanceAccess.entries
                .filter { it.requirement == AttendanceAccessRequirement.REQUIRED }
                .toSet()
        ) = AttendancePermissionReadiness(
            entries = AttendanceAccess.entries.map { access ->
                AttendanceAccessReadiness(
                    access = access,
                    status = if (
                        access.requirement == AttendanceAccessRequirement.REQUIRED
                    ) {
                        AttendanceAccessStatus.ACTION_REQUIRED
                    } else {
                        AttendanceAccessStatus.DEGRADED
                    },
                    recovery = AttendanceAccessRecovery.NONE
                )
            },
            inspectionIssues = listOf(
                AttendancePermissionInspectionIssue(
                    failure = failure,
                    affectedAccesses = affectedAccesses
                )
            )
        )
    }
}
