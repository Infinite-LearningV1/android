package com.example.infinite_track.presentation.components.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.infinite_track.domain.model.booking.BookingHistoryItem
import com.example.infinite_track.presentation.theme.Blue_500
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme
import com.example.infinite_track.presentation.theme.Purple_300
import com.example.infinite_track.presentation.theme.Purple_400
import com.example.infinite_track.presentation.theme.Purple_500

@Composable
fun BookingHistoryCard(
	booking: BookingHistoryItem,
	modifier: Modifier = Modifier
) {
	Box(
		modifier = modifier
			.fillMaxWidth()
	) {
		// Replace Card with Box for better opacity effect
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.height(IntrinsicSize.Min)
				.clip(RoundedCornerShape(16.dp))
				.background(Color.White.copy(alpha = 0.5f))
				.border(BorderStroke(1.dp, Color.White), RoundedCornerShape(16.dp))
		) {
			Row(
				modifier = Modifier
					.padding(
						top = 8.dp,
						bottom = 8.dp,
						start = 84.dp,
						end = 4.dp
					)
			) {
				// Info Section
				Column(
					modifier = Modifier
						.weight(1f)
						.background(color = Color.Transparent),
				) {
					// Location title - 1 line sampai start Badge status
					Text(
						text = booking.locationDescription,
						style = MaterialTheme.typography.titleMedium,
						fontWeight = FontWeight.Bold,
						color = Purple_500,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis,
						modifier = Modifier.padding(end = 80.dp)
					)

					// Work from anywhere - 1 line sampai start Badge status
					Text(
						text = booking.notes,
						style = MaterialTheme.typography.bodySmall,
						color = Purple_300,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis,
						modifier = Modifier.padding(end = 80.dp)
					)

					Spacer(modifier = Modifier.height(4.dp))

					// Progress bar dengan ujung melengkung - sejajar dengan badge text
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.SpaceBetween,
						verticalAlignment = Alignment.CenterVertically
					) {
						// Custom rounded progress bar
						Box(
							modifier = Modifier
								.weight(1f)
								.height(6.dp)
								.padding(end = 6.dp)
								.clip(RoundedCornerShape(100.dp))
								.background(Purple_400)
						) {
							val progress = when (booking.suitabilityLabel) {
								"Sangat Sesuai" -> 1.0f
								"Sesuai" -> 0.8f
								"Cukup Sesuai" -> 0.5f
								"Kurang Sesuai" -> 0.3f
								else -> 0.3f
							}

							Box(
								modifier = Modifier
									.fillMaxWidth(progress)
									.height(6.dp)
									.clip(RoundedCornerShape(100.dp))
									.background(Blue_500)
								)
						}
					}

					Spacer(modifier = Modifier.height(6.dp))

					// Suitability label di bawah progress bar
					Box(
						modifier = Modifier
							.clip(RoundedCornerShape(12.dp))
							.background(Blue_500)
							.padding(horizontal = 8.dp, vertical = 2.dp)
					) {
						Text(
							text = booking.suitabilityLabel,
							color = Color.White,
							style = MaterialTheme.typography.labelSmall
						)
					}
				}
			}
		}

		// Date section - bebas seperti badge dengan padding 8.dp dari card
		Box(
			modifier = Modifier
				.align(Alignment.TopStart)
				.padding(8.dp)
				.width(70.dp)
				.background(Blue_500, shape = RoundedCornerShape(12.dp))
				.padding(vertical = 12.dp),
		) {
			Column(
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.Center,
				modifier = Modifier.fillMaxWidth()
			) {
				Text(
					text = booking.scheduleDate.split(" ")[0],
					style = MaterialTheme.typography.headlineMedium,
					color = Color.White,
					fontWeight = FontWeight.Bold
				)
				Text(
					text = "${booking.scheduleDate.split(" ")[1]} ${booking.scheduleDate.split(" ")[2]}",
					style = MaterialTheme.typography.bodySmall,
					color = Color.White
				)
			}
		}

		// Status Badge - nempel di atas kanan tanpa terpengaruh padding card
		Box(
			modifier = Modifier
				.align(Alignment.TopEnd)
				.clip(RoundedCornerShape(topEnd = 16.dp, bottomStart = 8.dp))
				.background(
					when (booking.status) {
						"Pending" -> Color(0xFFFFC107)
						"Approved" -> Color(0xFF4CAF50)
						"Rejected" -> Color(0xFFF44336)
						else -> Color(0xFFFFC107)
					}
				)
				.padding(horizontal = 12.dp, vertical = 4.dp)
		) {
			Text(
				text = booking.status,
				color = Color.White,
				style = MaterialTheme.typography.labelMedium,
				fontWeight = FontWeight.SemiBold
			)
		}
	}
}

@Preview(showBackground = true, backgroundColor = 0xFF7B2CF7)
@Composable
fun BookingHistoryCardPreview() {
	Infinite_TrackTheme {
		Column(
			modifier = Modifier.padding(16.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp)
		) {
			// Preview dengan status Pending
			BookingHistoryCard(
				booking = BookingHistoryItem(
					id = "1",
					locationDescription = "Starbucks Mall Kelapa Gading",
					scheduleDate = "15 Jan 2025",
					status = "Pending",
					notes = "Work from anywhere",
					suitabilityLabel = "Sangat Sesuai"
				)
			)

			// Preview dengan status Approved
			BookingHistoryCard(
				booking = BookingHistoryItem(
					id = "2",
					locationDescription = "Coffee Shop Central Park",
					scheduleDate = "20 Feb 2025",
					status = "Approved",
					notes = "Work from anywhere",
					suitabilityLabel = "Sesuai"
				)
			)

			// Preview dengan status Rejected
			BookingHistoryCard(
				booking = BookingHistoryItem(
					id = "3",
					locationDescription = "WeWork Sudirman",
					scheduleDate = "25 Mar 2025",
					status = "Rejected",
					notes = "Work from anywhere",
					suitabilityLabel = "Kurang Sesuai"
				)
			)
		}
	}
}
