package com.example.infinite_track.data.mapper.booking

import com.example.infinite_track.data.soucre.network.response.booking.BookingItem
import com.example.infinite_track.domain.model.booking.BookingHistoryItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun BookingItem.toDomain(): BookingHistoryItem {
	return BookingHistoryItem(
		id = this.bookingId.toString(),
		locationDescription = this.location.description,
		scheduleDate = formatScheduleDateId(this.scheduleDate),
		status = formatStatusTitle(this.status),
		notes = this.notes ?: "",
		suitabilityLabel = this.suitabilityLabel ?: mapSuitabilityLabelId(this.suitabilityScore)
	)
}

private fun formatScheduleDateId(dateString: String): String {
	return try {
		val date = LocalDate.parse(dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
		date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale("id", "ID")))
	} catch (e: Exception) {
		dateString // Return original if parsing fails
	}
}

private fun formatStatusTitle(status: String): String {
	return status.lowercase().replaceFirstChar {
		if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
	}
}

private fun mapSuitabilityLabelId(score: Float?): String {
	val s = score ?: return ""
	return when {
		s >= 85f -> "Sangat Sesuai"
		s >= 70f -> "Sesuai"
		s >= 50f -> "Cukup Sesuai"
		else -> "Kurang Sesuai"
	}
}
