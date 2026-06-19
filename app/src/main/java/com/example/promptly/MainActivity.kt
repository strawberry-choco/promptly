package com.example.promptly

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.promptly.data.repository.OnboardingRepositoryImpl
import com.example.promptly.data.repository.ServiceStateRepositoryImpl
import com.example.promptly.domain.model.RecoveryRoutingDecision
import com.example.promptly.domain.usecase.ServiceLossRecoveryUseCase
import com.example.promptly.ui.onboarding.OnboardingActivity
import com.example.promptly.ui.recovery.RecoveryActivity
import com.example.promptly.ui.settings.SettingsActivity
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val onboardingRepo = OnboardingRepositoryImpl(prefs)
            val serviceStateRepo = ServiceStateRepositoryImpl(this@MainActivity)
            val useCase = ServiceLossRecoveryUseCase(onboardingRepo, serviceStateRepo)

            val target = when (val decision = useCase()) {
                RecoveryRoutingDecision.ProceedToOnboarding -> OnboardingActivity::class.java
                RecoveryRoutingDecision.ShowRecovery -> RecoveryActivity::class.java
                RecoveryRoutingDecision.ProceedToSettings -> SettingsActivity::class.java
            }
            startActivity(Intent(this@MainActivity, target))
            finish()
        }
    }

    companion object {
        private const val PREFS_NAME = "promptly_prefs"
    }
}
