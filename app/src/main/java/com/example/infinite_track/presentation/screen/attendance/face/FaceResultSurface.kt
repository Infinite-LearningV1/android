package com.example.infinite_track.presentation.screen.attendance.face

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.components.button.ButtonStateType
import com.example.infinite_track.presentation.components.button.ButtonStyle
import com.example.infinite_track.presentation.components.button.StatefulButton
import com.example.infinite_track.presentation.design.tokens.InfiniteColors

/**
 * Terminal result surface for face verification.
 *
 * Renders the captured photo, an honest success/failure status (icon + text + colour, never
 * colour alone), the differentiated reason copy, and the available actions
 * (Retry / Continue / Cancel). Raw similarity score/threshold are never shown here; they stay
 * debug-only inside the verification use case.
 */
@Composable
fun FaceResultSurface(
    livenessState: LivenessState,
    failureReason: FaceVerificationFailureReason?,
    capturedFacePreview: Bitmap?,
    onRetry: () -> Unit,
    onContinue: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val copy = when {
        livenessState == LivenessState.SUCCESS -> FaceResultCopy.success()
        failureReason != null -> FaceResultCopy.forFailure(failureReason)
        else -> FaceResultCopy.timeout()
    }

    val accent = when (copy.role) {
        FaceResultRole.SUCCESS -> InfiniteColors.Success
        FaceResultRole.ERROR -> InfiniteColors.Error
        FaceResultRole.WARNING -> InfiniteColors.Warning
    }
    val icon = if (copy.role == FaceResultRole.SUCCESS) {
        Icons.Filled.CheckCircle
    } else {
        Icons.Filled.Warning
    }

    val title = stringResource(copy.titleRes)
    val message = stringResource(copy.messageRes)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = title },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            capturedFacePreview?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(36.dp)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = accent,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatefulButton(
                    text = "Tutup",
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    style = ButtonStyle.Outlined,
                    stateType = ButtonStateType.Error,
                    enabled = true
                )
                if (copy.role == FaceResultRole.SUCCESS) {
                    StatefulButton(
                        text = "Lanjutkan",
                        onClick = onContinue,
                        modifier = Modifier.weight(1f),
                        style = ButtonStyle.Elevated,
                        stateType = ButtonStateType.Info,
                        enabled = true
                    )
                } else if (copy.retryable) {
                    StatefulButton(
                        text = "Coba Lagi",
                        onClick = onRetry,
                        modifier = Modifier.weight(1f),
                        style = ButtonStyle.Elevated,
                        stateType = ButtonStateType.Info,
                        enabled = true
                    )
                }
            }
        }
    }
}
