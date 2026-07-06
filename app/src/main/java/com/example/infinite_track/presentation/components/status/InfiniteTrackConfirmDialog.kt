package com.example.infinite_track.presentation.components.status

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.infinite_track.presentation.core.body2
import com.example.infinite_track.presentation.core.headline3
import com.example.infinite_track.presentation.theme.Blue_500
import com.example.infinite_track.presentation.theme.Red_Error
import com.example.infinite_track.presentation.theme.White

@Composable
fun InfiniteTrackConfirmDialog(
    status: StatusStateSpec,
    title: String,
    message: String,
    showDialog: Boolean,
    modifier: Modifier = Modifier,
    confirmText: String = "Confirm",
    cancelText: String = "Cancel",
    isDestructive: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (!showDialog) return

    val tokens = rememberStatusStateTokens(status)
    val confirmColor = if (isDestructive) Red_Error else Blue_500

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
                Icon(
                    imageVector = tokens.icon,
                    contentDescription = status.value,
                    tint = tokens.iconTint,
                    modifier = Modifier.size(58.dp)
                )

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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = cancelText)
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = confirmColor,
                            contentColor = White
                        )
                    ) {
                        Text(text = confirmText)
                    }
                }
            }
        }
    }
}
