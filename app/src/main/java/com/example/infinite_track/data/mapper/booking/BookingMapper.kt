package com.example.infinite_track.data.mapper.booking

import com.example.infinite_track.data.soucre.network.response.booking.BookingData
import com.example.infinite_track.data.soucre.network.response.booking.BookingHistoryResponse
import com.example.infinite_track.data.soucre.network.response.booking.BookingItem
import com.example.infinite_track.data.soucre.network.response.booking.BookingSummaryData
import com.example.infinite_track.data.soucre.network.response.booking.PaginationData
import com.example.infinite_track.domain.model.booking.BookingHistoryItem
import com.example.infinite_track.domain.model.booking.BookingHistoryPage
import com.example.infinite_track.domain.model.booking.BookingHistoryPagination
import com.example.infinite_track.domain.model.booking.BookingHistorySummary
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

fun BookingHistoryResponse.toDomain(): BookingHistoryPage {
    val responseData = data
    return BookingHistoryPage(
        bookings = responseData?.bookings.orEmpty().map { it.toDomain() },
        summary = responseData?.summary.toDomain(),
        pagination = responseData?.pagination.toDomain()
    )
}

fun BookingData.toDomain(): BookingHistoryPage {
    return BookingHistoryPage(
        bookings = bookings.map { it.toDomain() },
        summary = summary.toDomain(),
        pagination = pagination.toDomain()
    )
}

fun BookingItem.toDomain(): BookingHistoryItem {
    val rawStatus = statusKey?.takeIf { it.isNotBlank() }
        ?: status?.takeIf { it.isNotBlank() }
        ?: "unknown"
    val displayStatus = statusLabel?.takeIf { it.isNotBlank() } ?: formatStatusTitle(rawStatus)
    val rawScheduleDate = scheduleDate.orEmpty()
    val rawCreatedAt = createdAt.orEmpty()
    val displayProcessedAt = formatProcessedAt(processedAt, rawStatus)

    return BookingHistoryItem(
        id = bookingId.toString(),
        locationDescription = location?.description?.takeIf { it.isNotBlank() } ?: "Location not available",
        scheduleDate = formatScheduleDateId(rawScheduleDate),
        status = displayStatus,
        notes = notes?.takeIf { it.isNotBlank() } ?: "No notes provided",
        suitabilityLabel = suitabilityLabel?.takeIf { it.isNotBlank() } ?: "Not available",
        bookingId = bookingId,
        scheduleDateRaw = rawScheduleDate,
        statusRaw = status.orEmpty(),
        statusKey = rawStatus.lowercase(Locale.getDefault()),
        statusLabel = displayStatus,
        suitabilityScore = suitabilityScore,
        createdAtRaw = rawCreatedAt,
        createdAt = formatDateTimeId(rawCreatedAt),
        processedAtRaw = processedAt,
        processedAt = displayProcessedAt,
        locationId = location?.locationId,
        latitude = location?.latitude,
        longitude = location?.longitude,
        radiusMeters = location?.radius
    )
}

private fun BookingSummaryData?.toDomain(): BookingHistorySummary {
    return BookingHistorySummary(
        total = this?.total ?: 0,
        pending = this?.pending ?: 0,
        approved = this?.approved ?: 0,
        rejected = this?.rejected ?: 0
    )
}

private fun PaginationData?.toDomain(): BookingHistoryPagination {
    return BookingHistoryPagination(
        currentPage = this?.currentPage ?: 1,
        totalPages = this?.totalPages ?: 1,
        totalItems = this?.totalItems ?: 0,
        itemsPerPage = this?.itemsPerPage ?: 10,
        hasNextPage = this?.hasNextPage ?: false,
        hasPreviousPage = this?.hasPreviousPage ?: false
    )
}

private fun formatScheduleDateId(dateString: String): String {
    if (dateString.isBlank()) return "-"
    return try {
        val date = LocalDate.parse(dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH))
    } catch (e: Exception) {
        dateString
    }
}

private fun formatDateTimeId(dateTimeString: String): String {
    if (dateTimeString.isBlank()) return "-"
    return try {
        val dateTime = LocalDateTime.parse(dateTimeString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        dateTime.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH))
    } catch (e: Exception) {
        dateTimeString
    }
}

private fun formatProcessedAt(processedAt: String?, statusKey: String): String {
    if (!processedAt.isNullOrBlank()) return formatDateTimeId(processedAt)
    return if (statusKey.equals("pending", ignoreCase = true)) {
        "Waiting approval"
    } else {
        "Not processed yet"
    }
}

private fun formatStatusTitle(status: String): String {
    return status.lowercase(Locale.getDefault()).replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
}
