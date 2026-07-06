package com.example.infinite_track.presentation.components.status

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.core.body2
import com.example.infinite_track.presentation.core.headline4

@Composable
fun InfiniteTrackInlineAlert(
    status: StatusStateSpec,
    message: String,
    modifier: Modifier = Modifier,
    title: String? = status.value,
    onDismiss: (() -> Unit)? = null
) {
    val tokens = rememberStatusStateTokens(status)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = tokens.containerColor),
        border = BorderStroke(1.dp, tokens.borderColor.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = tokens.icon,
                contentDescription = status.value,
                tint = tokens.iconTint,
                modifier = Modifier.size(22.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                if (!title.isNullOrBlank()) {
                    Text(
                        text = title,
                        style = headline4,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.contentColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = message,
                    style = body2,
                    color = tokens.contentColor.copy(alpha = 0.82f)
                )
            }

            if (onDismiss != null) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Dismiss alert",
                        tint = tokens.contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
