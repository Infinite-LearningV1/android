package com.example.infinite_track.data.mapper.booking

import com.example.infinite_track.data.soucre.network.request.WfaRequestDto
import com.example.infinite_track.data.soucre.network.response.booking.WfaRequestResponseDto
import com.example.infinite_track.domain.model.booking.SubmitWfaRequestCommand
import com.example.infinite_track.domain.model.booking.SubmittedWfaRequest
import com.example.infinite_track.domain.model.booking.WfaCandidateLocation
import com.example.infinite_track.domain.model.booking.WfaRequestStatus
import java.time.Instant
import java.time.LocalDate

fun SubmitWfaRequestCommand.toDto() = WfaRequestDto(
    scheduleDate = scheduleDate.toString(),
    reasonId = reasonId,
    otherReasonText = otherReasonText,
    notes = notes,
    latitude = location.latitude,
    longitude = location.longitude
)

fun WfaRequestResponseDto.toDomainOrNull(
    fallbackLocation: WfaCandidateLocation? = null
): SubmittedWfaRequest? {
    if (!success) return null
    val body = data ?: return null
    val bookingId = body.bookingId ?: return null
    val scheduleDate = body.scheduleDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?: return null
    val status = body.status?.uppercase()?.let {
        runCatching { WfaRequestStatus.valueOf(it) }.getOrDefault(WfaRequestStatus.UNKNOWN)
    } ?: return null
    val location = body.location?.let { dto ->
        val latitude = dto.latitude ?: return@let null
        val longitude = dto.longitude ?: return@let null
        WfaCandidateLocation(
            latitude = latitude,
            longitude = longitude,
            displayName = dto.displayName?.trim().orEmpty().ifBlank { "Lokasi WFA" },
            formattedAddress = dto.formattedAddress?.trim().orEmpty()
        ).takeIf { it.hasValidCoordinates }
    } ?: fallbackLocation
    val reasonLabel = body.reason?.label?.trim().orEmpty().ifBlank {
        body.reasonLabel?.trim().orEmpty()
    }
    val radiusMeters = body.radiusMeters ?: return null

    if (location == null || reasonLabel.isBlank() || radiusMeters <= 0) return null

    return SubmittedWfaRequest(
        bookingId = bookingId,
        scheduleDate = scheduleDate,
        status = status,
        location = location,
        reasonLabel = reasonLabel,
        radiusMeters = radiusMeters,
        submittedAt = body.submittedAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
    )
}
