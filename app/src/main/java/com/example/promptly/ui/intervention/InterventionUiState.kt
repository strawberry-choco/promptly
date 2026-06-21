package com.example.promptly.ui.intervention

sealed class InterventionMode {
    data object Loading : InterventionMode()
    data object Showing : InterventionMode()
    data object Dismissing : InterventionMode()
    data class Redirecting(val packageName: String) : InterventionMode()
}

data class InterventionUiState(
    val mode: InterventionMode = InterventionMode.Loading,
    val errorMessage: String? = null
)
