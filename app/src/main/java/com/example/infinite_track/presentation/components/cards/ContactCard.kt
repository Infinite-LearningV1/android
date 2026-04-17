package com.example.infinite_track.presentation.components.cards

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.infinite_track.R
import com.example.infinite_track.presentation.core.body2
import com.example.infinite_track.presentation.core.body3
import com.example.infinite_track.presentation.theme.Blue_500
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme
import com.example.infinite_track.presentation.theme.Purple_300
import com.example.infinite_track.utils.makePhoneCall
import com.example.infinite_track.utils.openWhatsApp
import com.example.infinite_track.utils.sendEmail

@Composable
fun ContactCard(
    modifier: Modifier = Modifier,
    name : String,
    position : String,
    cardImage : String?,
    phone: String?="",
    message: String?="",
    whatsapp: String?="",
    onClickCard: () -> Unit,
    messageWA: String,
    context: Context
) {
    var isOnPressed by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .height(75.dp)
            .fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .height(94.dp)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isOnPressed = true
                            tryAwaitRelease()
                            isOnPressed = false
                        },
                        onTap = {
                            onClickCard()
                        }
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
                        isOnPressed -> Color(0xFFFFFFFF)
                        else -> Color(0x33FFFFFF)
                    }
                ),
                border = BorderStroke(1.dp, Color(0xFFFFFFFF)),
                modifier = Modifier
                    .height(70.dp)
                    .fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically

                ) {
                    Row (
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        Row (
                            modifier = Modifier
                                .fillMaxHeight(),
                            verticalAlignment = Alignment.CenterVertically
                        ){
                            AsyncImage(
                                model = cardImage,
                                contentDescription = "",
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .height(50.dp)
                                    .width(50.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier,
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(start = 4.dp)
                                        .width(140.dp)
                                ) {
                                    Text(
                                        text = name,
                                        style = body2,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = position,
                                        style = body3,
                                        color = Purple_300,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }

                        Row (
                            modifier = Modifier.fillMaxHeight(),
                            verticalAlignment = Alignment.CenterVertically
                        ){
                            Image(
                                painter = painterResource(id = R.drawable.ic_phone),
                                contentDescription = "",
                                modifier = Modifier
                                    .padding(start = 10.dp)
                                    .height(20.dp)
                                    .width(20.dp)
                                    .clickable (
                                        interactionSource = interactionSource,
                                        indication = null  // Hilangkan efek klik (ripple)
                                    ) {
                                        if (phone != null) {
                                            makePhoneCall(context, phone)
                                        }
                                    }
                            )
                            Image(
                                painter = painterResource(id = R.drawable.ic_whatsapp),
                                contentDescription = "",
                                modifier = Modifier
                                    .padding(start = 10.dp, end = 10.dp)
                                    .height(17.dp)
                                    .width(21.dp)
                                    .clickable (
                                        interactionSource = interactionSource,
                                        indication = null  // Hilangkan efek klik (ripple)
                                    ) {
                                        if (message != null) {
                                            sendEmail(context, message)
                                        }
                                    }
                            )
                            Image(
                                painter = painterResource(id = R.drawable.ic_whatsapp),
                                contentDescription = "",
                                modifier = Modifier
                                    .height(21.dp)
                                    .width(21.dp)
                                    .clickable (
                                        interactionSource = interactionSource,
                                        indication = null  // Hilangkan efek klik (ripple)
                                    ) {
                                        if (whatsapp != null) {
                                            openWhatsApp(context, whatsapp, messageWA)
                                        }
                                    }
                            )
                        }
                    }

                }
            }
        }
    }
}