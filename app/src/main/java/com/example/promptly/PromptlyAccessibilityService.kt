package com.example.promptly

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.promptly.data.repository.CooldownRepositoryImpl
import com.example.promptly.data.repository.SettingsRepositoryImpl
import com.example.promptly.domain.gate.Gate
import com.example.promptly.domain.gate.GateDecision
import com.example.promptly.ui.intervention.InterventionActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PromptlyAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var gate: Gate

    override fun onServiceConnected() {
        val prefs = getSharedPreferences("promptly_prefs", MODE_PRIVATE)
        val settingsRepository = SettingsRepositoryImpl(prefs)
        val cooldownRepository = CooldownRepositoryImpl(prefs)

        gate = Gate(settingsRepository, cooldownRepository)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return

        scope.launch {
            val decision = gate.decide(foregroundPackage = packageName)
            if (decision !is GateDecision.Show) return@launch

            val intent = Intent(this@PromptlyAccessibilityService, InterventionActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
                putExtra(
                    InterventionActivity.EXTRA_PENDING_TRIGGER_EPOCH_MILLIS,
                    decision.pendingClaim.triggerEpochMillis
                )
            }
            startActivity(intent)
        }
    }

    override fun onInterrupt() {}
}
