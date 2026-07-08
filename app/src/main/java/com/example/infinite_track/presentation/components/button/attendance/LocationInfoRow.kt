package com.example.infinite_track.presentation.components.button.attendance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.infinite_track.domain.model.attendance.Location
import com.example.infinite_track.domain.model.attendance.SelectedTargetLocation
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.presentation.core.body1
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme

/**
 * Komponen untuk menampilkan informasi lokasi dengan subtitle dan detail lengkap.
 */
@Composable
fun LocationInfoSection(
    targetLocationInfo: SelectedTargetLocation?,
    currentLocationAddress: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Target Location Section
        targetLocationInfo?.let { targetInfo ->
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Subtitle: Lokasi Target
                Text(
                    text = "Lokasi Target",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black.copy(alpha = 0.8f)
                )

                // Location Info Row dengan data dari API
                LocationInfoRow(
                    icon = Icons.Default.LocationOn,
                    primaryText = targetInfo.displayName,
                    secondaryText = targetInfo.description ?: targetInfo.unavailableReason
                )
            }
        }

        // Current Location Section
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Subtitle: Lokasi Anda Saat Ini
            Text(
                text = "Lokasi Anda Saat Ini",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black.copy(alpha = 0.8f)
            )

            // Current Location Info Row
            LocationInfoRow(
                icon = Icons.Default.MyLocation,
                primaryText = currentLocationAddress,
                secondaryText = null // Tidak ada secondary text untuk lokasi saat ini
            )
        }
    }
}

/**
 * Komponen individu untuk menampilkan informasi lokasi dengan ikon dan teks.
 *
 * @param icon Ikon yang akan ditampilkan di sebelah kiri
 * @param primaryText Teks utama (description/address)
 * @param secondaryText Teks sekunder opsional (nama lokasi)
 */
@Composable
fun LocationInfoRow(
    icon: ImageVector,
    primaryText: String,
    secondaryText: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Location Icon",
            modifier = Modifier.size(24.dp),
            tint = Color.Black.copy(alpha = 0.6f)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Primary text (description/address)
            Text(
                text = primaryText,
                style = body1,
                color = Color.Black.copy(alpha = 0.8f),
                maxLines = 1, // Ubah dari 1 ke 2 untuk text yang lebih panjang
                overflow = TextOverflow.Ellipsis
            )

            // Secondary text (location name) jika ada
            secondaryText?.let { secondary ->
                Text(
                    text = secondary,
                    fontSize = 12.sp,
                    color = Color.Black.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LocationInfoSectionPreview() {
    Infinite_TrackTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Preview dengan target location info
            LocationInfoSection(
                targetLocationInfo = SelectedTargetLocation(
                    mode = WorkMode.WFO,
                    location = Location(
                        locationId = 1,
                        description = "Kantor Pusat PT. Technology Indonesia",
                        latitude = 0.0,
                        longitude = 0.0,
                        radius = 100,
                        category = "Gedung Cyber 2, Kuningan"
                    ),
                    displayName = "Kantor Pusat PT. Technology Indonesia",
                    description = "Gedung Cyber 2, Kuningan",
                    isAvailable = true
                ),
                currentLocationAddress = "Jl. Kemang Raya No. 123, Jakarta Selatan, DKI Jakarta"
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LocationInfoRowPreview() {
    Infinite_TrackTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LocationInfoRow(
                icon = Icons.Default.LocationOn,
                primaryText = "Kantor Pusat PT. Technology Indonesia",
                secondaryText = "Gedung Cyber 2, Kuningan"
            )

            LocationInfoRow(
                icon = Icons.Default.MyLocation,
                primaryText = "Jl. Kemang Raya No. 123, Jakarta Selatan, DKI Jakarta"
            )
        }
    }
}
