package com.example.infinite_track.presentation.screen.attendance.booking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.infinite_track.domain.use_case.auth.GetLoggedInUserUseCase
import com.example.infinite_track.domain.use_case.booking.SubmitWfaBookingUseCase
import com.example.infinite_track.domain.use_case.location.ReverseGeocodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class WfaBookingViewModel @Inject constructor(
    private val submitWfaBookingUseCase: SubmitWfaBookingUseCase,
    private val getLoggedInUserUseCase: GetLoggedInUserUseCase,
    private val reverseGeocodeUseCase: ReverseGeocodeUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(WfaBookingState())
    val uiState: StateFlow<WfaBookingState> = _uiState.asStateFlow()

    // Get navigation arguments (latitude and longitude only) using FloatType
    private val latitude = savedStateHandle.get<Float>("latitude")?.toDouble() ?: 0.0
    private val longitude = savedStateHandle.get<Float>("longitude")?.toDouble() ?: 0.0

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                // Set initial coordinates
                _uiState.value = _uiState.value.copy(
                    latitude = latitude,
                    longitude = longitude
                )

                // Load user data and address concurrently
                loadUserData()
                loadAddressFromCoordinates()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load initial data: ${e.message}"
                )
            }
        }
    }

    private suspend fun loadUserData() {
        try {
            getLoggedInUserUseCase().collect { user ->
                user?.let {
                    _uiState.value = _uiState.value.copy(
                        fullName = it.fullName,
                        division = it.divisionName?: "",
                    )
                }
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = "Failed to load user data: ${e.message}"
            )
        }
    }

    private suspend fun loadAddressFromCoordinates() {
        try {
            reverseGeocodeUseCase(
                latitude = latitude,
                longitude = longitude
            ).onSuccess { locationResult ->
                _uiState.value = _uiState.value.copy(
                    address = locationResult.address
                )
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    address = "Lat: $latitude, Lng: $longitude",
                    error = "Gagal mendapatkan alamat: ${exception.message}"
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                address = "Lat: $latitude, Lng: $longitude",
                error = "Error loading address: ${e.message}"
            )
        }
    }

    fun onRadiusChanged(radius: Int) {
        _uiState.value = _uiState.value.copy(radius = radius)
    }

    fun onDescriptionChanged(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun onScheduleDateChanged(scheduleDate: String) {
        _uiState.value = _uiState.value.copy(scheduleDate = scheduleDate)
    }

    fun onNotesChanged(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    fun onSubmitBooking() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val currentState = _uiState.value

                // Format schedule date to DD-MM-YYYY
                val formattedDate = formatDate(currentState.scheduleDate)

                val result = submitWfaBookingUseCase(
                    scheduleDate = formattedDate,
                    latitude = currentState.latitude,
                    longitude = currentState.longitude,
                    radius = currentState.radius,
                    description = currentState.description,
                    notes = currentState.notes
                )

                result.onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isBookingSuccessful = true
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Terjadi kesalahan saat mengirim booking."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Terjadi kesalahan: ${e.message}"
                )
            }
        }
    }

    private fun formatDate(dateString: String): String {
        return try {
            // Assuming the input date is in format like "2024-12-25"
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            // If parsing fails, return the current date formatted
            val outputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            outputFormat.format(Date())
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class WfaBookingState(
    val fullName: String = "",
    val division: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radius: Int = 100,
    val description: String = "",
    val scheduleDate: String = "",
    val notes: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isBookingSuccessful: Boolean = false
)
