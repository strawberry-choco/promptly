package com.example.promptly.ui.intervention

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.promptly.domain.model.RedirectDecision
import com.example.promptly.domain.usecase.AppRedirectUseCase
import com.example.promptly.domain.usecase.InterventionUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InterventionViewModel(
    private val interventionUseCase: InterventionUseCase,
    private val appRedirectUseCase: AppRedirectUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(InterventionUiState())
    val uiState: StateFlow<InterventionUiState> = _uiState.asStateFlow()

    fun onCreated() {
        viewModelScope.launch {
            val config = interventionUseCase.prepare()
            _uiState.value = _uiState.value.copy(
                mode = InterventionMode.Showing,
                config = config
            )
            delay(1500L)
            val decision = appRedirectUseCase.evaluate()
            when (decision) {
                is RedirectDecision.Ready -> {
                    interventionUseCase.dismiss()
                    _uiState.value = _uiState.value.copy(
                        mode = InterventionMode.Redirecting(decision.packageName)
                    )
                }
                is RedirectDecision.Uninstalled -> {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Target app not found."
                    )
                }
                is RedirectDecision.NoTarget -> {
                    // stay in Showing mode with dismiss only
                }
            }
        }
    }

    fun onDismiss() {
        viewModelScope.launch {
            interventionUseCase.dismiss()
            _uiState.value = _uiState.value.copy(mode = InterventionMode.Dismissing)
        }
    }

    fun onPause() {
        val current = _uiState.value
        if (current.mode == InterventionMode.Showing) {
            _uiState.value = current.copy(isCallInterrupted = true)
        }
    }

    fun onResumeAfterCall() {
        if (!_uiState.value.isCallInterrupted) return
        viewModelScope.launch {
            interventionUseCase.onResurface()
            _uiState.value = _uiState.value.copy(isCallInterrupted = false)
        }
    }
}
