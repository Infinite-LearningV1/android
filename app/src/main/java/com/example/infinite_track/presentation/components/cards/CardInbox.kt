package com.example.infinite_track.presentation.components.cards

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.infinite_track.R
import com.example.infinite_track.presentation.core.body2
import com.example.infinite_track.presentation.core.body2_5
import com.example.infinite_track.presentation.core.body3
import com.example.infinite_track.presentation.core.body4
import com.example.infinite_track.presentation.theme.Blue_500
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme
import com.example.infinite_track.presentation.theme.Purple_300
import kotlin.math.roundToInt

@Composable
fun CardInbox(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    DateTime : String,
    Name : String,
    TypeLeave : String,
    CardImage : Int,
    CardStatus : String,
    availableDays : String,
) {
    var isOnPressed by remember { mutableStateOf(false) }
    var offsetX by remember { mutableStateOf(0f) }

    // Tentukan warna card berdasarkan status
    val StatusColor = when (CardStatus.lowercase()) {
        "pending" -> Color(0xFFE1C900)
        "decline" -> Color(0xFFFF7589)
        "approve" -> Color(0xFF89FE71)
        else -> Color.Gray
    }

    val pressedColor = when (CardStatus.lowercase()) {
        "pending" -> Color(0xFFFFD700)
        "decline" -> Color(0xFFFC1C1C)
        "approve" -> Color(0xFF008500)
        else -> Color.DarkGray
    }

    // Tentukan warna card apakah ditekan atau tidak
    val cardColor = if (isOnPressed) {
        pressedColor // Warna saat card ditekan
    } else {
        StatusColor // Warna default sesuai status
    }

    // Logika swipe hanya untuk "decline"
    val isSwipeEnabled = CardStatus.lowercase() == "decline"
    val minSwipeDistance = -150f // Swipe ke kiri maksimal

    Box(
        modifier = Modifier
            .height(90.dp)
            .width(430.dp)
    ) {
        // Tombol delete di belakang card (muncul saat di-swipe ke kiri)
        if (offsetX < 0) {
            Box(
                modifier = Modifier
                    .height(90.dp)
                    .width(430.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Red),
                contentAlignment = Alignment.CenterEnd
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_pencil),
                    contentDescription = "Delete",
                    modifier = Modifier
                        .padding(end = 16.dp)
                )
            }
        }

        // Card utama dengan pointerInput untuk tap dan swipe gesture
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .height(94.dp)
                .width(430.dp)
//                .padding(bottom = 1.5.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            if (offsetX == 0f) {  // Cek apakah card tidak sedang diswipe
                                isOnPressed = true
                                tryAwaitRelease()
                                isOnPressed = false
                            }
                        },
                        onTap = {
                            if (offsetX == 0f) {  // Cek apakah card tidak sedang diswipe
                                onClick()
                            } else {
                                onDelete()
                            }
                        }
                    )
                }
                .pointerInput(isSwipeEnabled) {
                    if (isSwipeEnabled) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                // Jika swipe lebih dari setengah ke kiri, buka sepenuhnya
                                if (offsetX < minSwipeDistance / 2) {
                                    offsetX = minSwipeDistance
                                } else {
                                    // Jika tidak, kembalikan ke posisi semula
                                    offsetX = 0f
                                }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                // Izinkan hanya swipe ke kiri (nilai negatif)
                                val newOffset = offsetX + dragAmount
                                if (newOffset < 0) { // Batasi agar hanya bisa swipe ke kiri
                                    offsetX = newOffset.coerceIn(minSwipeDistance, 0f)
                                }
                            }
                        )
                    }
                }
                .offset {
                    IntOffset(
                        if (isSwipeEnabled) offsetX.roundToInt() else 0,
                        0
                    )
                }
                .then(
                    if (isOnPressed) {
                        Modifier
                            .shadow(
                                elevation = (7.dp),
                                shape = RoundedCornerShape(14.dp),
                                ambientColor = Blue_500,
                                spotColor = Blue_500,
                            )
                    } else {
                        Modifier
                    }
                )
        ) {
            Card(
                modifier = Modifier
                    .height(90.dp)
                    .width(430.dp)
            ) {
                Column {
                    Text(
                        text = DateTime,
                        style = body3,
                        color = Purple_300,
                        modifier = Modifier
                            .padding(top = 11.dp, start = 11.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 10.dp, bottom = 7.dp)
                    ) {
                        Image(
                            painter = painterResource(id = CardImage),
                            contentDescription = "",
                            modifier = Modifier
                                .height(60.dp)
                                .width(60.dp)
                                .padding(top = 3.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(start = 4.dp)
                            ) {
                                Text(
                                    text = TypeLeave,
                                    style = body2,
                                )
                                Text(
                                    text = Name,
                                    style = body3,
                                    color = Purple_300
                                )
                                Text(
                                    text = "Leave Available: $availableDays",
                                    style = body2_5,
                                    color = Blue_500
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = cardColor),
                                modifier = Modifier
                                    .height(14.dp)
                                    .width(48.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = CardStatus,
                                        style = body4,
                                        color = Color(0xFFFFFFFF)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewCardInbox() {
    Infinite_TrackTheme {
        Column {
            Infinite_TrackTheme {
                Column {
                    CardInbox(
                        DateTime = "11 Ags 2023, 19:39",
                        Name = "Raja Muhammad Farhan Zahputra",
                        TypeLeave = "Cuti Melahirkan",
                        CardImage = R.drawable.ic_profile,
                        CardStatus = "Decline",
                        availableDays = "5 Days",
                        onClick = {println("Card 1 clicked!")},
                        onDelete = {println("Card 2 deleted!")},
                    )
                    CardInbox(
                        DateTime = "15 Feb 2024",
                        Name = "Jane Smith",
                        TypeLeave = "Vacation",
                        CardImage = android.R.drawable.ic_menu_report_image,
                        CardStatus = "Approve",
                        availableDays = "3 Days",
                        onClick = {println("Card 1 clicked!")},
                        onDelete = {println("Card 2 deleted!")}
                    )
                    CardInbox(
                        DateTime = "20 Feb 2024",
                        Name = "Alice Brown",
                        TypeLeave = "Sick Leave",
                        CardImage = android.R.drawable.ic_menu_report_image,
                        CardStatus = "Pending",
                        availableDays = "7 Days",
                        onClick = {println("Card 1 clicked!")},
                        onDelete = {println("Card 2 deleted!")}
                    )
                }
            }
        }
    }
}
