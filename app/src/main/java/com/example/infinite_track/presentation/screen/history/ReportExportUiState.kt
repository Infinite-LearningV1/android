package com.example.infinite_track.presentation.screen.history

import android.net.Uri

sealed interface ReportExportUiState {
    data object Idle : ReportExportUiState
    data object Downloading : ReportExportUiState
    data class Success(val localUri: Uri, val fileName: String) : ReportExportUiState
    data class Error(val message: String) : ReportExportUiState
}
