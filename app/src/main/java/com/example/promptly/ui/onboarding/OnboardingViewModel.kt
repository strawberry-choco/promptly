package com.example.promptly.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.promptly.domain.usecase.OnboardingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val onboardingUseCase: OnboardingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private var hasNavigated = false

    fun onResume() {
        if (hasNavigated) return
        viewModelScope.launch {
            val enabled = onboardingUseCase.checkServiceEnabled()
            _uiState.value = OnboardingUiState(
                isLoading = false,
                showSuccess = enabled
            )
        }
    }

    fun onSkip() {
        hasNavigated = true
        viewModelScope.launch {
            onboardingUseCase.skip()
        }
    }

    fun onContinue() {
        if (hasNavigated) return
        hasNavigated = true
        viewModelScope.launch {
            onboardingUseCase.complete()
        }
    }
}
