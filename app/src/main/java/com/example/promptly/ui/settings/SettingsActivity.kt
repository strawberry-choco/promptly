package com.example.promptly.ui.settings

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.NumberPicker
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.promptly.data.repository.CooldownRepositoryImpl
import com.example.promptly.data.repository.OnboardingRepositoryImpl
import com.example.promptly.data.repository.ServiceStateRepositoryImpl
import com.example.promptly.data.repository.SettingsRepositoryImpl
import com.example.promptly.domain.model.CooldownConfig
import com.example.promptly.domain.model.TargetApp
import com.example.promptly.domain.usecase.OnboardingUseCase
import com.example.promptly.domain.usecase.SettingsUseCase
import com.example.promptly.databinding.ActivitySettingsBinding
import com.example.promptly.R
import com.example.promptly.ui.intervention.InterventionActivity
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.launch
import java.time.LocalTime

class SettingsActivity : AppCompatActivity() {

    private val viewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory()
    }

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeState()
        observeEvents()
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
                    binding.bannerCard.visibility =
                        if (state.showBanner) android.view.View.VISIBLE else android.view.View.GONE

                    binding.enabledSwitch.isChecked = state.enabled

                    val targetApp = TargetApp.CURATED_LIST.find {
                        it.packageName == state.targetAppPackage
                    }
                    binding.targetAppSummary.text = targetApp?.displayName
                        ?: getString(R.string.settings_no_target_app)

                    when (val config = state.cooldownConfig) {
                        is CooldownConfig.DailyReset -> {
                            binding.cooldownToggleGroup.check(R.id.cooldown_daily_reset)
                            binding.dailyResetSection.visibility = android.view.View.VISIBLE
                            binding.nHourSection.visibility = android.view.View.GONE
                            binding.resetTimeValue.text = formatTime(config.resetTime)
                        }
                        is CooldownConfig.NHourInterval -> {
                            binding.cooldownToggleGroup.check(R.id.cooldown_n_hour)
                            binding.dailyResetSection.visibility = android.view.View.GONE
                            binding.nHourSection.visibility = android.view.View.VISIBLE
                            binding.intervalHoursValue.text = getString(
                                R.string.settings_interval_hours_format, config.intervalHours
                            )
                        }
                    }

                    binding.scheduleStartValue.text = formatTime(state.scheduleStart)
                    binding.scheduleEndValue.text = formatTime(state.scheduleEnd)
                }
            }
        }
    }

    private fun observeEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is SettingsEvent.LaunchIntervention -> {
                            val intent = Intent(
                                this@SettingsActivity,
                                InterventionActivity::class.java
                            )
                            startActivity(intent)
                        }
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.bannerCard.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        binding.enabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onEnabledChanged(isChecked)
        }

        binding.targetAppRow.setOnClickListener {
            showTargetAppPicker()
        }

        binding.cooldownToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.cooldown_daily_reset -> viewModel.onCooldownTypeChanged(
                    CooldownConfig.DailyReset(LocalTime.MIDNIGHT)
                )
                R.id.cooldown_n_hour -> viewModel.onCooldownTypeChanged(
                    CooldownConfig.NHourInterval(4)
                )
            }
        }

        binding.resetTimeValue.setOnClickListener {
            val current = when (val config = viewModel.uiState.value.cooldownConfig) {
                is CooldownConfig.DailyReset -> config.resetTime
                else -> LocalTime.MIDNIGHT
            }
            showTimePicker(current) { time ->
                viewModel.onCooldownTypeChanged(CooldownConfig.DailyReset(time))
            }
        }

        binding.intervalHoursValue.setOnClickListener {
            showIntervalPicker()
        }

        binding.scheduleStartRow.setOnClickListener {
            showTimePicker(viewModel.uiState.value.scheduleStart) { time ->
                viewModel.onScheduleStartChanged(time)
            }
        }

        binding.scheduleEndRow.setOnClickListener {
            showTimePicker(viewModel.uiState.value.scheduleEnd) { time ->
                viewModel.onScheduleEndChanged(time)
            }
        }

        binding.launchInterventionRow.setOnClickListener {
            viewModel.onLaunchIntervention()
        }
    }

    private fun showTargetAppPicker() {
        val apps = TargetApp.CURATED_LIST
        val names = apps.map { it.displayName }.toTypedArray()
        val selectedIndex = apps.indexOfFirst {
            it.packageName == viewModel.uiState.value.targetAppPackage
        }.takeIf { it >= 0 } ?: 0

        AlertDialog.Builder(this)
            .setTitle(R.string.settings_select_target_app)
            .setSingleChoiceItems(names, selectedIndex) { dialog, which ->
                viewModel.onTargetAppChanged(apps[which].packageName)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showTimePicker(current: LocalTime, onSelected: (LocalTime) -> Unit) {
        val picker = MaterialTimePicker.Builder()
            .setHour(current.hour)
            .setMinute(current.minute)
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .build()
        picker.addOnPositiveButtonClickListener {
            onSelected(LocalTime.of(picker.hour, picker.minute))
        }
        picker.show(supportFragmentManager, "time_picker")
    }

    private fun showIntervalPicker() {
        val current = when (val config = viewModel.uiState.value.cooldownConfig) {
            is CooldownConfig.NHourInterval -> config.intervalHours
            else -> 4
        }
        val numberPicker = NumberPicker(this).apply {
            minValue = 1
            maxValue = 24
            value = current
            wrapSelectorWheel = false
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.settings_interval_hours)
            .setView(numberPicker)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewModel.onCooldownTypeChanged(
                    CooldownConfig.NHourInterval(numberPicker.value)
                )
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun formatTime(time: LocalTime): String =
        String.format("%02d:%02d", time.hour, time.minute)

    inner class SettingsViewModelFactory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val settingsRepo = SettingsRepositoryImpl(prefs)
            val cooldownRepo = CooldownRepositoryImpl(prefs)
            val onboardingRepo = OnboardingRepositoryImpl(prefs)
            val serviceStateRepo = ServiceStateRepositoryImpl(this@SettingsActivity)
            val settingsUseCase = SettingsUseCase(settingsRepo, cooldownRepo)
            val onboardingUseCase = OnboardingUseCase(onboardingRepo, serviceStateRepo)
            return SettingsViewModel(settingsUseCase, onboardingUseCase) as T
        }
    }

    companion object {
        private const val PREFS_NAME = "promptly_prefs"
    }
}
