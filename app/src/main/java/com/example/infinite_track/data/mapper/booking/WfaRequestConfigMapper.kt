package com.example.infinite_track.data.mapper.booking

import com.example.infinite_track.data.soucre.network.response.booking.WfaRequestConfigResponseDto
import com.example.infinite_track.domain.model.booking.WfaRequestConfig
import com.example.infinite_track.domain.model.booking.WfaRequestReason

fun WfaRequestConfigResponseDto.toDomainOrNull(): WfaRequestConfig? {
    val body = data ?: return null
    val radius = body.radiusMeters ?: return null
    if (!success || radius <= 0) return null

    val rawReasons = body.reasons.orEmpty()
    val mappedReasons = rawReasons.mapNotNull { reason ->
        val id = reason.id ?: return@mapNotNull null
        val label = reason.label?.trim().orEmpty()
        if (label.isBlank()) null else WfaRequestReason(id, label, reason.isOther)
    }
    if (mappedReasons.size != rawReasons.size) return null
    if (mappedReasons.map { it.id }.distinct().size != mappedReasons.size) return null
    if (mappedReasons.isEmpty() || mappedReasons.count { it.isOther } > 1) return null

    return WfaRequestConfig(radius, mappedReasons)
}
