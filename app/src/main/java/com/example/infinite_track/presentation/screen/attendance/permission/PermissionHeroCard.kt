package com.example.infinite_track.presentation.screen.attendance.permission

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.design.tokens.InfiniteColors

@Composable
internal fun PermissionHeroCard(
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    PermissionGlassCard(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(92.dp),
                shape = CircleShape,
                color = InfiniteColors.Primary.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, InfiniteColors.Primary.copy(alpha = 0.24f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(46.dp),
                        tint = InfiniteColors.Primary
                    )
                }
            }
            Text(
                text = if (isLoading) "Memeriksa kesiapan akses" else "Siapkan akses absensi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                color = InfiniteColors.Text.copy(alpha = 0.88f),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Kami cek lokasi, kamera, dan status lokasi perangkat sebelum Anda memilih mode kerja.",
                style = MaterialTheme.typography.bodyMedium,
                color = InfiniteColors.Text.copy(alpha = 0.72f),
                textAlign = TextAlign.Center
            )
        }
    }
}
