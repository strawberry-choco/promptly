package com.example.promptly

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.promptly.data.repository.CooldownRepositoryImpl
import com.example.promptly.data.repository.SettingsRepositoryImpl
import com.example.promptly.data.repository.TargetAppRepositoryImpl
import com.example.promptly.domain.service.CooldownEligibilityService
import com.example.promptly.domain.usecase.AppRedirectUseCase
import com.example.promptly.domain.usecase.CheckCooldownUseCase
import com.example.promptly.domain.usecase.RecordCooldownTriggerUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PromptlyAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentForegroundPackage: String? = null

    private lateinit var appRedirectUseCase: AppRedirectUseCase
    private lateinit var checkCooldownUseCase: CheckCooldownUseCase
    private lateinit var recordCooldownTriggerUseCase: RecordCooldownTriggerUseCase

    override fun onServiceConnected() {
        val prefs = getSharedPreferences("promptly_prefs", MODE_PRIVATE)
        val settingsRepository = SettingsRepositoryImpl(prefs)
        val targetAppRepository = TargetAppRepositoryImpl(packageManager)
        val cooldownRepository = CooldownRepositoryImpl(prefs)
        val eligibilityService = CooldownEligibilityService()

        appRedirectUseCase = AppRedirectUseCase(settingsRepository, targetAppRepository)
        checkCooldownUseCase = CheckCooldownUseCase(settingsRepository, cooldownRepository, eligibilityService)
        recordCooldownTriggerUseCase = RecordCooldownTriggerUseCase(cooldownRepository)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName == currentForegroundPackage) return
        currentForegroundPackage = packageName

        scope.launch {
            val decision = appRedirectUseCase.evaluate()
            val targetPackage = when (decision) {
                is com.example.promptly.domain.model.RedirectDecision.Ready -> decision.packageName
                else -> return@launch
            }
            if (packageName != targetPackage) return@launch

            if (checkCooldownUseCase() != com.example.promptly.domain.model.EligibilityResult.Eligible) return@launch

            recordCooldownTriggerUseCase()
            val intent = Intent(this@PromptlyAccessibilityService, com.example.promptly.ui.intervention.InterventionActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
            }
            startActivity(intent)
        }
    }

    override fun onInterrupt() {}
}
