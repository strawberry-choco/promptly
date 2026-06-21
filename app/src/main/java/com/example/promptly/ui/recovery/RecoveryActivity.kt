package com.example.promptly.ui.recovery

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.promptly.data.repository.OnboardingRepositoryImpl
import com.example.promptly.data.repository.ServiceStateRepositoryImpl
import com.example.promptly.databinding.ActivityRecoveryBinding
import com.example.promptly.domain.usecase.ServiceLossRecoveryUseCase
import com.example.promptly.ui.settings.SettingsActivity
import kotlinx.coroutines.launch

class RecoveryActivity : ComponentActivity() {

    private val viewModel: RecoveryViewModel by viewModels {
        RecoveryViewModelFactory()
    }

    private lateinit var binding: ActivityRecoveryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRecoveryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                left = maxOf(insets.left, view.paddingLeft),
                top = maxOf(insets.top, view.paddingTop),
                right = maxOf(insets.right, view.paddingRight),
                bottom = maxOf(insets.bottom, view.paddingBottom)
            )
            WindowInsetsCompat.CONSUMED
        }

        observeState()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkService()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressIndicator.visibility =
                        if (state.isChecking) android.view.View.VISIBLE else android.view.View.GONE
                    binding.recoveryContent.visibility =
                        if (!state.isChecking && !state.serviceEnabled) android.view.View.VISIBLE else android.view.View.GONE
                    if (state.serviceEnabled) {
                        startActivity(Intent(this@RecoveryActivity, SettingsActivity::class.java))
                        finish()
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.reEnableButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            viewModel.onReEnable()
        }
    }

    inner class RecoveryViewModelFactory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val onboardingRepo = OnboardingRepositoryImpl(prefs)
            val serviceStateRepo = ServiceStateRepositoryImpl(this@RecoveryActivity)
            val useCase = ServiceLossRecoveryUseCase(onboardingRepo, serviceStateRepo)
            return RecoveryViewModel(useCase) as T
        }
    }

    companion object {
        private const val PREFS_NAME = "promptly_prefs"
    }
}
