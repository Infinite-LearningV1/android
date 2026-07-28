package com.example.infinite_track.domain.model.booking

data class WfaRequestConfig(
    val radiusMeters: Int,
    val reasons: List<WfaRequestReason>
)

data class WfaRequestReason(
    val id: Long,
    val label: String,
    val isOther: Boolean
)
