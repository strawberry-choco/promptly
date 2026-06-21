package com.example.promptly.ui.intervention

import android.os.Bundle
import android.view.View
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
import com.example.promptly.data.repository.SettingsRepositoryImpl
import com.example.promptly.data.repository.TargetAppRepositoryImpl
import com.example.promptly.databinding.ActivityInterventionBinding
import com.example.promptly.domain.usecase.AppRedirectUseCase
import kotlinx.coroutines.launch

class InterventionActivity : AppCompatActivity() {

    private val viewModel: InterventionViewModel by viewModels {
        InterventionViewModelFactory()
    }

    private lateinit var binding: ActivityInterventionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityInterventionBinding.inflate(layoutInflater)
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
        viewModel.onCreated()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state.mode) {
                        InterventionMode.Loading -> {
                            binding.dismissButton.visibility = View.GONE
                            binding.errorMessage.visibility = View.GONE
                        }
                        InterventionMode.Showing -> {
                            binding.dismissButton.visibility = View.VISIBLE
                            if (state.errorMessage != null) {
                                binding.errorMessage.text = state.errorMessage
                                binding.errorMessage.visibility = View.VISIBLE
                            } else {
                                binding.errorMessage.visibility = View.GONE
                            }
                        }
                        InterventionMode.Dismissing -> {
                            finish()
                        }
                        is InterventionMode.Redirecting -> {
                            val packageName = state.mode.packageName
                            val intent = packageManager.getLaunchIntentForPackage(
                                packageName
                            )
                            if (intent != null) {
                                try {
                                    startActivity(intent)
                                    finish()
                                    return@collect
                                } catch (e: Exception) {
                                    viewModel.onAppLaunchFailed(packageName)
                                }
                            } else {
                                viewModel.onAppLaunchFailed(packageName)
                            }
                        }
                    }
                }
            }
        }
    }

    fun onDismissButtonClicked() {
        viewModel.onDismiss()
    }

    inner class InterventionViewModelFactory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val settingsRepo = SettingsRepositoryImpl(prefs)
            val targetAppRepo = TargetAppRepositoryImpl(packageManager)
            val appRedirectUseCase = AppRedirectUseCase(settingsRepo, targetAppRepo)
            return InterventionViewModel(appRedirectUseCase) as T
        }
    }

    companion object {
        private const val PREFS_NAME = "promptly_prefs"
    }
}
