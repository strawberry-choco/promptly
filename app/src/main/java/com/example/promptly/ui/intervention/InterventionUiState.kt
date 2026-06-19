package com.example.promptly.ui.intervention

import com.example.promptly.domain.model.InterventionConfig

sealed class InterventionMode {
    data object Loading : InterventionMode()
    data object Showing : InterventionMode()
    data object Dismissing : InterventionMode()
    data class Redirecting(val packageName: String) : InterventionMode()
}

data class InterventionUiState(
    val mode: InterventionMode = InterventionMode.Loading,
    val config: InterventionConfig? = null,
    val isCallInterrupted: Boolean = false,
    val errorMessage: String? = null
)
