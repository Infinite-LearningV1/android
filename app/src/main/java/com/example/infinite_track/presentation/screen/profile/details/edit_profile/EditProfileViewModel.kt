package com.example.infinite_track.presentation.screen.profile.details.edit_profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.infinite_track.data.soucre.network.request.ProfileUpdateRequest
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.use_case.auth.GetLoggedInUserUseCase
import com.example.infinite_track.domain.use_case.profile.UpdateProfileUseCase
import com.example.infinite_track.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the EditProfile screen
 * Acts as the single source of truth for UI state
 */
@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val getLoggedInUserUseCase: GetLoggedInUserUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase
) : ViewModel() {

    // User profile state
    private val _userProfileState = MutableStateFlow<UserModel?>(null)
    val userProfileState: StateFlow<UserModel?> = _userProfileState.asStateFlow()

    // Form field states - only the editable fields
    private val _fullName = MutableStateFlow("")
    val fullName: StateFlow<String> = _fullName.asStateFlow()

    private val _nipNim = MutableStateFlow("")
    val nipNim: StateFlow<String> = _nipNim.asStateFlow()

    private val _phone = MutableStateFlow("")
    val phone: StateFlow<String> = _phone.asStateFlow()

    // Editing state
    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing.asStateFlow()

    // Update profile state
    private val _updateProfileState = MutableStateFlow<UiState<UserModel>>(UiState.Idle)
    val updateProfileState: StateFlow<UiState<UserModel>> = _updateProfileState.asStateFlow()

    val hasChanges: StateFlow<Boolean> = combine(
        _userProfileState,
        _fullName,
        _nipNim,
        _phone
    ) { currentUser, fullName, nipNim, phone ->
        currentUser?.let { isFormChanged(it, fullName, nipNim, phone) } ?: false
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val canSave: StateFlow<Boolean> = combine(
        _userProfileState,
        _fullName,
        _nipNim,
        hasChanges,
        _updateProfileState
    ) { currentUser, fullName, nipNim, hasChanges, updateState ->
        currentUser != null &&
                fullName.isNotBlank() &&
                nipNim.isNotBlank() &&
                hasChanges &&
                updateState !is UiState.Loading
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        fetchUserProfile()
    }

    /**
     * Fetch the current user profile from local storage
     */
    private fun fetchUserProfile() {
        viewModelScope.launch {
            getLoggedInUserUseCase()
                .catch { e ->
                    // Handle error
                }
                .collect { user ->
                    _userProfileState.value = user

                    // Initialize form fields with current user data
                    user?.let { userData ->
                        _fullName.value = userData.fullName
                        _nipNim.value = userData.nipNim
                        _phone.value = userData.phone ?: ""
                    }
                }
        }
    }

    /**
     * Toggle editing mode on/off
     */
    fun onToggleEditMode() {
        _isEditing.value = !_isEditing.value
    }

    /**
     * Update full name field
     */
    fun onFullNameChange(newValue: String) {
        _fullName.value = newValue
    }

    /**
     * Update NIP/NIM field
     */
    fun onNipNimChange(newValue: String) {
        _nipNim.value = newValue
    }

    /**
     * Update phone field
     */
    fun onPhoneChange(newValue: String) {
        _phone.value = newValue
    }

    /**
     * Save profile changes
     */
    fun onSaveChangesClick() {
        val currentUser = _userProfileState.value ?: return

        viewModelScope.launch {
            _updateProfileState.value = UiState.Loading

            val request = ProfileUpdateRequest(
                fullName = _fullName.value.takeIf { it != currentUser.fullName },
                nipNim = _nipNim.value.takeIf { it != currentUser.nipNim },
                phone = _phone.value.takeIf { it != currentUser.phone.orEmpty() }
            )

            // Only update if there are changes
            if (isFormChanged(currentUser, _fullName.value, _nipNim.value, _phone.value)) {
                updateProfileUseCase(currentUser.id, request)
                    .onSuccess { updatedUser ->
                        _userProfileState.value = updatedUser
                        _fullName.value = updatedUser.fullName
                        _nipNim.value = updatedUser.nipNim
                        _phone.value = updatedUser.phone ?: ""
                        _updateProfileState.value = UiState.Success(updatedUser)
                        _isEditing.value = false
                    }
                    .onFailure { error ->
                        _updateProfileState.value = UiState.Error(error.message ?: "Unknown error occurred")
                    }
            } else {
                // No changes to save
                _updateProfileState.value = UiState.Idle
                _isEditing.value = false
            }
        }
    }

    /**
     * Cancel editing and reset form fields
     */
    fun onCancelClick() {
        userProfileState.value?.let { userData ->
            _fullName.value = userData.fullName
            _nipNim.value = userData.nipNim
            _phone.value = userData.phone ?: ""
        }

        _isEditing.value = false
        _updateProfileState.value = UiState.Idle
    }

    /**
     * Check if the form has changed compared to the current user data
     */
    private fun isFormChanged(
        currentUser: UserModel,
        fullName: String,
        nipNim: String,
        phone: String
    ): Boolean {
        return fullName != currentUser.fullName ||
                nipNim != currentUser.nipNim ||
                phone != currentUser.phone.orEmpty()
    }

    /**
     * Reset the update profile state
     */
    fun resetUpdateState() {
        _updateProfileState.value = UiState.Idle
    }
}
