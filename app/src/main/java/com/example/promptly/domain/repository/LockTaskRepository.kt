package com.example.promptly.domain.repository

interface LockTaskRepository {
    suspend fun start(): Boolean
    suspend fun stop()
}
