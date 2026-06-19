package com.example.promptly.domain.repository

interface ServiceStateRepository {
    suspend fun isEnabled(): Boolean
}
