package com.example.infinite_track.domain.model.attendance

data class WorkModeEligibility(
    val mode: WorkMode,
    val canContinueToFaceVerification: Boolean,
    val blockingReason: String? = null,
    val recoveryAction: WorkModeRecoveryAction? = null,
    val approvedWfaBookingId: Int? = null
)

enum class WorkModeRecoveryAction {
    CHOOSE_WFO,
    UPDATE_WFH_LOCATION,
    SELECT_WFA_LOCATION,
    REQUEST_WFA_BOOKING,
    VIEW_WFA_REQUESTS
}
