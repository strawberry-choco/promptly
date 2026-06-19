package com.example.promptly.ui.recovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.promptly.domain.usecase.ServiceLossRecoveryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecoveryViewModel(
    private val serviceLossRecoveryUseCase: ServiceLossRecoveryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecoveryUiState())
    val uiState: StateFlow<RecoveryUiState> = _uiState.asStateFlow()

    fun onReEnable() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isChecking = false, serviceEnabled = false)
        }
    }

    fun checkService() {
        viewModelScope.launch {
            val enabled = serviceLossRecoveryUseCase.invoke()
            _uiState.value = RecoveryUiState(
                isChecking = false,
                serviceEnabled = enabled is com.example.promptly.domain.model.RecoveryRoutingDecision.ProceedToSettings
            )
        }
    }
}
