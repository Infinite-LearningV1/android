package com.example.infinite_track.presentation.components.status

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.infinite_track.presentation.core.body2
import com.example.infinite_track.presentation.core.headline3
import com.example.infinite_track.presentation.theme.Blue_500
import com.example.infinite_track.presentation.theme.White

@Composable
fun InfiniteTrackStatusDialog(
    status: StatusStateSpec,
    title: String,
    message: String,
    showDialog: Boolean,
    modifier: Modifier = Modifier,
    confirmText: String = "OK",
    @DrawableRes imageRes: Int? = null,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit = onDismiss
) {
    if (!showDialog) return

    val tokens = rememberStatusStateTokens(status)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (imageRes != null) {
                    Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = title,
                        modifier = Modifier.size(112.dp)
                    )
                } else {
                    Icon(
                        imageVector = tokens.icon,
                        contentDescription = status.value,
                        tint = tokens.iconTint,
                        modifier = Modifier.size(64.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = title,
                    style = headline3,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.contentColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = message,
                    style = body2,
                    color = tokens.contentColor.copy(alpha = 0.78f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Blue_500,
                        contentColor = White
                    )
                ) {
                    Text(text = confirmText)
                }
            }
        }
    }
}
