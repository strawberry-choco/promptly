package com.example.promptly.domain.repository

interface CooldownRepository {
    suspend fun clearLastTrigger()
}
