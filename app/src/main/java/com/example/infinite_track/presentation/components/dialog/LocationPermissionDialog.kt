package com.example.infinite_track.presentation.components.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.infinite_track.utils.LocationPermissionHelper

/**
 * Dialog komprehensif untuk menangani permission request lokasi
 * dengan penjelasan yang user-friendly untuk Android 10+ background location
 */
@Composable
fun LocationPermissionDialog(
    permissionResult: LocationPermissionHelper.PermissionResult,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dialogData = getDialogData(permissionResult)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon
                Icon(
                    imageVector = dialogData.icon,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = dialogData.iconColor
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = dialogData.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description
                Text(
                    text = dialogData.description,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                )

                // Additional info for background permission
                if (permissionResult == LocationPermissionHelper.PermissionResult.BackgroundPermissionDenied) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "💡 Panduan:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "1. Pilih \"Lokasi\" di pengaturan\n" +
                                      "2. Pilih \"Izinkan sepanjang waktu\"\n" +
                                      "3. Kembali ke aplikasi",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (dialogData.showSettingsButton) {
                        Arrangement.spacedBy(12.dp)
                    } else {
                        Arrangement.Center
                    }
                ) {
                    // Cancel/Later button
                    if (dialogData.showSettingsButton) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text("Nanti")
                        }
                    }

                    // Main action button
                    Button(
                        onClick = if (dialogData.showSettingsButton) onOpenSettings else onRequestPermission,
                        modifier = if (dialogData.showSettingsButton) Modifier.weight(1f) else Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = dialogData.buttonColor
                        )
                    ) {
                        if (dialogData.showSettingsButton) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(dialogData.buttonText)
                    }
                }
            }
        }
    }
}

/**
 * Data class untuk menyimpan konfigurasi dialog berdasarkan permission result
 */
private data class DialogData(
    val icon: ImageVector,
    val iconColor: Color,
    val title: String,
    val description: String,
    val buttonText: String,
    val buttonColor: Color,
    val showSettingsButton: Boolean
)

/**
 * Helper function untuk mendapatkan data dialog berdasarkan permission result
 */
@Composable
private fun getDialogData(permissionResult: LocationPermissionHelper.PermissionResult): DialogData {
    return when (permissionResult) {
        LocationPermissionHelper.PermissionResult.ForegroundPermissionDenied -> DialogData(
            icon = Icons.Default.LocationOn,
            iconColor = MaterialTheme.colorScheme.primary,
            title = "Izin Lokasi Diperlukan",
            description = "Aplikasi memerlukan akses lokasi untuk fitur absensi dan pemantauan area kerja. " +
                         "Izin ini digunakan untuk memverifikasi kehadiran Anda di lokasi kerja yang telah ditentukan.",
            buttonText = "Berikan Izin",
            buttonColor = MaterialTheme.colorScheme.primary,
            showSettingsButton = false
        )

        LocationPermissionHelper.PermissionResult.BackgroundPermissionDenied -> DialogData(
            icon = Icons.Default.LocationOn,
            iconColor = MaterialTheme.colorScheme.secondary,
            title = "Izin Lokasi Latar Belakang",
            description = "Untuk pemantauan area kerja otomatis, aplikasi memerlukan akses lokasi " +
                         "\"sepanjang waktu\". Fitur ini akan memberitahu Anda ketika memasuki atau " +
                         "meninggalkan area kerja, bahkan saat aplikasi tidak aktif.",
            buttonText = "Buka Pengaturan",
            buttonColor = MaterialTheme.colorScheme.secondary,
            showSettingsButton = true
        )

        LocationPermissionHelper.PermissionResult.PermanentlyDenied -> DialogData(
            icon = Icons.Default.Warning,
            iconColor = MaterialTheme.colorScheme.error,
            title = "Akses Pengaturan Diperlukan",
            description = "Izin lokasi telah ditolak secara permanen. Untuk mengaktifkan fitur geofencing, " +
                         "silakan buka pengaturan aplikasi dan berikan izin lokasi secara manual.",
            buttonText = "Buka Pengaturan",
            buttonColor = MaterialTheme.colorScheme.error,
            showSettingsButton = true
        )

        else -> DialogData(
            icon = Icons.Default.LocationOn,
            iconColor = MaterialTheme.colorScheme.primary,
            title = "Izin Lokasi",
            description = "Aplikasi memerlukan izin lokasi untuk fitur absensi.",
            buttonText = "Berikan Izin",
            buttonColor = MaterialTheme.colorScheme.primary,
            showSettingsButton = false
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LocationPermissionDialogPreview() {
    MaterialTheme {
        LocationPermissionDialog(
            permissionResult = LocationPermissionHelper.PermissionResult.BackgroundPermissionDenied,
            onRequestPermission = { },
            onOpenSettings = { },
            onDismiss = { }
        )
    }
}