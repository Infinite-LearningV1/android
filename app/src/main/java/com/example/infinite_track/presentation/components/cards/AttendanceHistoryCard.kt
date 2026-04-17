package com.example.infinite_track.presentation.components.cards

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.infinite_track.domain.model.attendance.AttendanceRecord
import com.example.infinite_track.presentation.core.body2
import com.example.infinite_track.presentation.core.bodyThin
import com.example.infinite_track.presentation.core.headline2
import com.example.infinite_track.presentation.core.headline4
import com.example.infinite_track.presentation.theme.Blue_100
import com.example.infinite_track.presentation.theme.Blue_500
import com.example.infinite_track.presentation.theme.White

@Composable
fun AttendanceHistoryC(
	record: AttendanceRecord
) {
	// Interaction source untuk mendeteksi klik/tombol ditekan
	val mutableInteractionSource = remember { MutableInteractionSource() }
	val pressed = mutableInteractionSource.collectIsPressedAsState()

	// Animasi untuk stroke warna
	val borderColor = animateColorAsState(
		targetValue = if (pressed.value) Blue_100 else Color.White,
		label = "BorderColor"
	)

	// Animasi untuk warna latar belakang
	val backgroundColor = animateColorAsState(
		targetValue = if (pressed.value) White else Color.Transparent,
		label = "BackgroundColor"
	)

	// Animasi untuk shadow elevation
	val elevation = animateDpAsState(
		targetValue = if (pressed.value) 8.dp else 0.dp,
		label = "Elevation"
	)

	Box(
		modifier = Modifier
			.shadow(
				elevation = elevation.value,
				shape = RoundedCornerShape(8.dp),
				clip = false,
				ambientColor = Blue_500.copy(alpha = 0.5f), // Warna bayangan
				spotColor = Blue_500 // Warna bayangan saat terkena cahaya
			)
			.background(
				color = backgroundColor.value,
				shape = RoundedCornerShape(8.dp)
			)
			.border(
				width = 1.dp, // Ketebalan border
				color = borderColor.value,
				shape = RoundedCornerShape(8.dp) // Radius
			)
			.clickable(
				interactionSource = mutableInteractionSource,
				indication = null // Menghilangkan efek ripple bawaan
			) {  }
			.fillMaxWidth()
			.height(53.dp)
	) {
		Row(
			modifier = Modifier
				.padding(6.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.SpaceBetween,
		) {
			Row(
				modifier = Modifier
					.background(
						color = Color(0xFF8A3DFF),
						shape = RoundedCornerShape(6.dp)
					)
					.height(40.dp)
					.width(100.dp)
					.padding(start = 8.dp, bottom = 5.dp)
			) {
				Text(
					text = record.date,
					modifier = Modifier.align(Alignment.Bottom),
					style = headline2,
					color = Color.White
				)
				Spacer(modifier = Modifier.width(4.dp))
				Text(
					text = record.monthYear,
					modifier = Modifier
						.align(Alignment.Bottom)
						.padding(bottom = 2.dp),
					color = Color.White,
					style = body2
				)
			}
			Spacer(modifier = Modifier.width(16.dp))
			Divider(
				color = Color.Gray,
				modifier = Modifier
					.width(1.dp)
					.height(22.dp)
			)
			Spacer(modifier = Modifier.width(16.dp))
			Column {
				Text(
					text = record.timeIn,
					style = headline4
				)
				Spacer(modifier = Modifier.height(3.dp))
				Text(
					text = "Check In",
					style = bodyThin
				)
			}
			Spacer(modifier = Modifier.width(16.dp))
			Divider(
				color = Color.Gray,
				modifier = Modifier
					.width(1.dp)
					.height(22.dp)
			)
			Spacer(modifier = Modifier.width(16.dp))
			Column {
				Text(
					text = record.timeOut?: "--:--",
					style = headline4
				)
				Spacer(modifier = Modifier.height(3.dp))
				Text(
					text = "Check Out",
					style = bodyThin
				)
			}
			Spacer(modifier = Modifier.width(16.dp))
			Divider(
				color = Color.Gray,
				modifier = Modifier
					.width(1.dp)
					.height(22.dp)
			)
			Spacer(modifier = Modifier.width(16.dp))
			Column {
				Text(
					text = record.workHour ?: "--:--",
					style = headline4
				)
				Spacer(modifier = Modifier.height(3.dp))
				Text(
					text = "Total Course",
					style = bodyThin
				)
			}
			Spacer(modifier = Modifier.width(16.dp))
		}
	}
}




@Preview
@Composable
private fun AttendanceHistoryPreview() {
	AttendanceHistoryC(
		record = AttendanceRecord(
			id = 1,
			date = "00",
			monthYear = "Dec 2024",
			timeIn = "07:00",
			timeOut = "17:00",
			workHour = "08:00",
		)
	)
}