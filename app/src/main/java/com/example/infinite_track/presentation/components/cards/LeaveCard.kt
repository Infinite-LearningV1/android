package com.example.infinite_track.presentation.components.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.infinite_track.R
import com.example.infinite_track.domain.model.MyLeaveModel
import com.example.infinite_track.presentation.core.body1
import com.example.infinite_track.presentation.core.body2
import com.example.infinite_track.presentation.core.body3
import com.example.infinite_track.presentation.theme.Blue_500
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme
import com.example.infinite_track.presentation.theme.Purple_300
import kotlin.math.roundToInt

@Composable
fun LeaveCard(
    modifier: Modifier = Modifier,
    myLeave : MyLeaveModel,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var isOnPressed by remember { mutableStateOf(false) }
    var offsetX by remember { mutableStateOf(0f) } // Untuk mengatur posisi swipe

    // Tentukan warna card berdasarkan status
    val cardColor = when (myLeave.cardStatus.lowercase()) {
        "pending" -> Color(0xFFFDBA13)
        "decline" -> Color(0xFFD42323)
        "approve" -> Color(0xFF4BA613)
        else -> Color.Gray
    }

    // Logika swipe hanya untuk "decline"
    val isSwipeEnabled = myLeave.cardStatus.lowercase() == "decline"
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
                .height(90.dp)
                .width(430.dp)
//                .padding(bottom = 10.dp)
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
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    when {
                        isOnPressed -> Color(0xFFFFFFFF) // Warna ketika ditekan
                        offsetX != 0f -> Color(0xFFFFFFFF) // Warna berdasarkan offsetX
                        else -> Color(0x33FFFFFF) // Warna default
                    }
                ),
                border = BorderStroke(1.dp, Color(0xFFFFFFFF)),
                modifier = Modifier
                    .height(90.dp)
                    .width(430.dp)
            ) {
                Column (
                    Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier
                        .fillMaxWidth(),
                        contentAlignment = Alignment.BottomEnd){
                        Text(
                            text = myLeave.dateTime,
                            style = body3,
                            color = Purple_300,
                            modifier = Modifier
                                .padding(top = 4.dp, end = 20.dp)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 10.dp, bottom = 7.dp)
                    ) {
                        Image(
                            painter = painterResource(id = myLeave.cardImage),
                            contentDescription = "",
                            modifier = Modifier
                                .height(60.dp)
                                .width(60.dp)
                                .padding(top = 3.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(235.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(start = 5.dp)
                            ) {
                                Text(
                                    text = myLeave.name,
                                    style = body1,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(5.dp))
                                Card(
                                    shape = RoundedCornerShape(30.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                    border = BorderStroke(0.5.dp, cardColor),
                                    modifier = Modifier
                                        .height(17.dp)
                                        .width(50.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            text = myLeave.cardStatus,
                                            style = body3,
                                            color = cardColor
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(5.dp))
                                Text(
                                    text = myLeave.typeLeave,
                                    style = body2,
                                    color = Purple_300,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun PreviewLeaveCard() {
    Infinite_TrackTheme {
        Column {
            Infinite_TrackTheme {
                Column {
                    LeaveCard(
                        myLeave = MyLeaveModel(
                            id = 1,
                            dateTime = "11 Ags 2023, 19:39",
                            name = "Raja Muhammad Farhan Zahputra",
                            typeLeave = "Cuti Melahirkan",
                            cardImage = R.drawable.ic_profile,
                            cardStatus = "Decline",
                        ),
                        onClick = {println("Card 1 clicked!")},
                        onDelete = {println("Card 2 deleted!")}
                    )
                }
            }
        }
    }
}
