package com.example.infinite_track.presentation.screen.profile.details.about

import androidx.lifecycle.ViewModel
import com.example.infinite_track.domain.use_case.about.GetAboutContentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class AboutViewModel @Inject constructor(
    getAboutContentUseCase: GetAboutContentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AboutUiState(content = getAboutContentUseCase())
    )
    val uiState: StateFlow<AboutUiState> = _uiState.asStateFlow()
}
