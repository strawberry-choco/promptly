package com.example.promptly

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.promptly.data.repository.OnboardingRepositoryImpl
import com.example.promptly.domain.model.OnboardingState
import com.example.promptly.ui.onboarding.OnboardingActivity
import com.example.promptly.ui.settings.SettingsActivity
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val onboardingRepo = OnboardingRepositoryImpl(prefs)
            val state = onboardingRepo.load()

            val target = when (state) {
                OnboardingState.New -> OnboardingActivity::class.java
                else -> SettingsActivity::class.java
            }
            startActivity(Intent(this@MainActivity, target))
            finish()
        }
    }

    companion object {
        private const val PREFS_NAME = "promptly_prefs"
    }
}
