package com.example.promptly.ui.intervention

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.promptly.domain.model.RedirectDecision
import com.example.promptly.domain.gate.Gate
import com.example.promptly.domain.gate.PendingClaim
import com.example.promptly.domain.usecase.AppRedirectUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InterventionViewModel(
    private val appRedirectUseCase: AppRedirectUseCase,
    private val gate: Gate
) : ViewModel() {

    private val _uiState = MutableStateFlow(InterventionUiState())
    val uiState: StateFlow<InterventionUiState> = _uiState.asStateFlow()

    fun onCreated(preRedirectDelayMs: Long = PRE_REDIRECT_DELAY_MS) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(mode = InterventionMode.Showing)
            delay(preRedirectDelayMs)
            val decision = appRedirectUseCase.evaluate()
            when (decision) {
                is RedirectDecision.Ready -> {
                    _uiState.value = _uiState.value.copy(
                        mode = InterventionMode.Redirecting(decision.packageName)
                    )
                }
                is RedirectDecision.Uninstalled -> {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Target app '${decision.packageName}' not found."
                    )
                }
                is RedirectDecision.NoTarget -> {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "No target app configured."
                    )
                }
            }
        }
    }

    fun onShown(claim: PendingClaim) {
        viewModelScope.launch {
            gate.confirm(claim)
        }
    }

    fun onAppLaunchFailed(packageName: String) {
        _uiState.value = _uiState.value.copy(
            mode = InterventionMode.Showing,
            errorMessage = "Failed to launch '$packageName'."
        )
    }

    fun onDismiss() {
        _uiState.value = _uiState.value.copy(mode = InterventionMode.Dismissing)
    }

    companion object {
        internal const val PRE_REDIRECT_DELAY_MS = 1500L
    }
}
