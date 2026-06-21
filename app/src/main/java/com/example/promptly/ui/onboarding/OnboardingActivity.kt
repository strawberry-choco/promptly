package com.example.promptly.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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
import com.example.promptly.databinding.ActivityOnboardingBinding
import com.example.promptly.domain.usecase.OnboardingUseCase
import com.example.promptly.ui.settings.SettingsActivity
import kotlinx.coroutines.launch

class OnboardingActivity : AppCompatActivity() {

    private val viewModel: OnboardingViewModel by viewModels {
        OnboardingViewModelFactory()
    }

    private lateinit var binding: ActivityOnboardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
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
        viewModel.onResume()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.loadingGroup.visibility =
                        if (state.isLoading) android.view.View.VISIBLE else android.view.View.GONE
                    binding.contentGroup.visibility =
                        if (!state.isLoading && !state.showSuccess) android.view.View.VISIBLE else android.view.View.GONE
                    binding.successGroup.visibility =
                        if (state.showSuccess) android.view.View.VISIBLE else android.view.View.GONE
                }
            }
        }
    }

    private fun setupListeners() {
        binding.openSettingsButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        binding.skipButton.setOnClickListener {
            viewModel.onSkip()
            navigateToSettings()
        }

        binding.continueButton.setOnClickListener {
            viewModel.onContinue()
            navigateToSettings()
        }
    }

    private fun navigateToSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
        finish()
    }

    inner class OnboardingViewModelFactory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val onboardingRepo = OnboardingRepositoryImpl(prefs)
            val serviceStateRepo = ServiceStateRepositoryImpl(this@OnboardingActivity)
            val useCase = OnboardingUseCase(onboardingRepo, serviceStateRepo)
            return OnboardingViewModel(useCase) as T
        }
    }

    companion object {
        private const val PREFS_NAME = "promptly_prefs"
    }
}
