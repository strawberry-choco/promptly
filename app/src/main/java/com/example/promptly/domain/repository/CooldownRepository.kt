package com.example.promptly.domain.repository

interface CooldownRepository {
    suspend fun loadLastTriggerEpochMillis(): Long?
    suspend fun saveLastTriggerEpochMillis(timestamp: Long)
    suspend fun clearLastTrigger()
}
