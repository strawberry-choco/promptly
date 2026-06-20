package com.example.promptly.domain.model

sealed interface RedirectDecision {
    data object NoTarget : RedirectDecision
    data class Ready(val packageName: String) : RedirectDecision
    data class Uninstalled(val packageName: String) : RedirectDecision
}
