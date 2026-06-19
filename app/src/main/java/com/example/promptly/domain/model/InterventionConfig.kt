package com.example.promptly.domain.model

sealed class InterventionConfig {
    data object Locked : InterventionConfig()
    data object Fallback : InterventionConfig()
}
