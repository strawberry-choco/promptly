package com.example.promptly.data.repository

import android.content.Context
import android.provider.Settings
import com.example.promptly.domain.repository.ServiceStateRepository

class ServiceStateRepositoryImpl(
    private val context: Context
) : ServiceStateRepository {

    override suspend fun isEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(SERVICE_NAME)
    }

    companion object {
        private const val SERVICE_NAME = "com.example.promptly/com.example.promptly.PromptlyAccessibilityService"
    }
}
