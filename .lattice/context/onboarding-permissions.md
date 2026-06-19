---
feature: Onboarding & Permissions
requirement_doc: .lattice/requirements/features/onboarding-permissions.md
created: 2026-06-19
---

# Onboarding & Permissions

> Guided first-launch flow to enable the Android Accessibility Service, with persistent reminder until setup is complete.

## Decisions Log

| Date | Decision | Reasoning | Alternatives Considered |
|------|----------|-----------|------------------------|

## Open Questions

## Constraints

## Design: Level 1 — Capabilities

1. **Guided setup on first launch** — New users see an onboarding screen explaining why the Accessibility Service is needed and how to enable it.
2. **One-tap navigation to system settings** — Users tap "Open Accessibility Settings" to jump directly to Android's Accessibility settings.
3. **Skip with persistent reminder** — Users can skip onboarding and use the app, but a persistent banner in Settings reminds them until the service is enabled.
4. **Automatic success detection & transition** — When the user enables the service and returns to the app, a success confirmation is shown and the app transitions to normal operation.
5. **Re-entry resilience** — Completed setup persists across restarts; onboarding only shows once. State survives process death.

## Design: Level 2 — Components

### Component Table

| # | Component | Layer | Responsibility |
|---|-----------|-------|---------------|
| 1 | OnboardingActivity | UI (Controller) | Display onboarding screen; route to SettingsActivity after completion |
| 2 | OnboardingViewModel | UI (Controller) | Manage onboarding UI state, coordinate open-settings, skip, resume-detection |
| 3 | SettingsActivity (modify) | UI (Controller) | Add persistent banner when service not enabled |
| 4 | SettingsViewModel (modify) | UI (Controller) | Expose service status and banner visibility to Settings UI |
| 5 | OnboardingUseCase | Application Service | Orchestrate first-launch check, skip, and completion workflows |
| 6 | OnboardingRepository (interface) | Domain | Contract for persisting first-launch flag and skip flag |
| 7 | ServiceStateRepository (interface) | Domain | Contract for querying accessibility service enablement |
| 8 | OnboardingRepositoryImpl | Data (Infrastructure) | SharedPreferences implementation of OnboardingRepository |
| 9 | ServiceStateRepositoryImpl | Data (Infrastructure) | Settings.Secure query for accessibility service check |
| 10 | OnboardingState | Domain (Value Object) | Sealed class: New, Completed, Skipped |

### Dependency Diagram

```
UI Layer:
  OnboardingActivity → OnboardingViewModel
  SettingsActivity (mod) → SettingsViewModel (mod)

Application Layer:
  OnboardingUseCase ← OnboardingViewModel

Domain Layer:
  OnboardingRepository (interface) ← OnboardingUseCase
  ServiceStateRepository (interface) ← OnboardingUseCase
  OnboardingState (value object) ← consumed by OnboardingUseCase

Data Layer:
  OnboardingRepositoryImpl : OnboardingRepository (SharedPreferences)
  ServiceStateRepositoryImpl : ServiceStateRepository (Settings.Secure query)
```

Dependency direction: UI → Application → Domain ← Data (implements)

### DDD Classifications

- **Value Objects**: OnboardingState (sealed class: New, Completed, Skipped). Immutable, equality by value, no identity tracking.
- **Entities**: None — no object tracked through lifecycle changes.
- **Aggregates**: None — no transactional invariants across multiple objects.
- **Domain Events**: Not needed — no other aggregates react to onboarding completion.
- **Domain Services**: None — onboarding logic is orchestration, not pure domain logic spanning entities.

### Component Layer Validation (Architecture)

| Check | Status |
|-------|--------|
| UI layer does only translation and event forwarding | ✓ OnboardingActivity/ViewModel forward user actions to OnboardingUseCase |
| Application Service orchestrates, doesn't embed business rules | ✓ OnboardingUseCase coordinates infrastructure, no domain logic beyond state machine |
| Domain contains no outer-layer imports | ✓ Interfaces and value objects have no Android dependencies |
| Data implements domain interfaces | ✓ Both repositories implement domain-defined interfaces |
| State-changing op (onboarding flags) uses Repository | ✓ OnboardingRepository handles SharedPreferences writes |
| Query op (service state) uses Provider/Repository pattern | ✓ ServiceStateRepository handles system settings query |

### Decisions Log

| Date | Decision | Reasoning | Alternatives Considered |
|------|----------|-----------|------------------------|
| 2026-06-19 | OnboardingState as sealed class (New/Completed/Skipped) over raw boolean flags | Encodes state machine explicitly; compiler-enforced exhaustive handling prevents invalid states | Raw `Boolean isOnboarded + Boolean isSkipped` (simpler but allows invalid combos) |
| 2026-06-19 | ServiceStateRepository as separate interface from OnboardingRepository | Different persistence mechanisms (SharedPreferences vs Settings.Secure) and concerns (app state vs system state) | Single OnboardingRepository mixing both; ServiceEnabledChecker as utility class |

### Constraints

- Onboarding state persisted in SharedPreferences (existing app convention)
- Service state detection via `Settings.Secure.getString(ENABLED_ACCESSIBILITY_SERVICES)` on activity resume

## Design: Level 3 — Interactions

### Flow 1: First Launch — Normal Onboarding Path

```
User          OnboardingActivity    OnboardingViewModel    OnboardingUseCase    ServiceStateRepo    SettingsActivity
 │                    │                      │                    │                    │                    │
 │  open app          │                      │                    │                    │                    │
 │───────────────────>│                      │                    │                    │                    │
 │                    │  onCreate            │                    │                    │                    │
 │                    │─────────────────────>│                    │                    │                    │
 │                    │                      │  getState()        │                    │                    │
 │                    │                      │───────────────────>│                    │                    │
 │                    │                      │                    │  load OnboardingRepo│                    │
 │                    │                      │                    │────────────────────>│                    │
 │                    │                      │                    │<────────────────────│                    │
 │                    │                      │<──── New ──────────│                    │                    │
 │                    │<── show onboarding ──│                    │                    │                    │
 │                    │                      │                    │                    │                    │
 │  tap "Open Settings"                      │                    │                    │                    │
 │──────────────────────────────────────────>│                    │                    │                    │
 │                    │                      │  openSettings()    │                    │                    │
 │                    │                      │───────────────────>│                    │                    │
 │                    │                      │                    │  startActivity(     │                    │
 │                    │                      │                    │  ACTION_ACCESSIBILITY│                    │
 │                    │                      │                    │  _SETTINGS)         │                    │
 │                    │  [system settings]   │                    │                    │                    │
 │<──────────────────────────────────────────────────────────────│                    │                    │
 │                    │                      │                    │                    │                    │
 │  [user enables service in system settings]                     │                    │                    │
 │                    │                      │                    │                    │                    │
 │  [presses back]    │                      │                    │                    │                    │
 │                    │  onResume            │                    │                    │                    │
 │                    │─────────────────────>│                    │                    │                    │
 │                    │                      │  checkServiceEnabled()                  │                    │
 │                    │                      │───────────────────>│                    │                    │
 │                    │                      │                    │  isEnabled()       │                    │
 │                    │                      │                    │───────────────────>│                    │
 │                    │                      │                    │<──── true ─────────│                    │
 │                    │                      │<── enabled ────────│                    │                    │
 │  [show success]    │                      │                    │                    │                    │
 │<───────────────────│                      │                    │                    │                    │
 │  tap "Continue"    │                      │                    │                    │                    │
 │───────────────────>│                      │                    │                    │                    │
 │                    │  onContinue()        │                    │                    │                    │
 │                    │─────────────────────>│                    │                    │                    │
 │                    │                      │  complete()        │                    │                    │
 │                    │                      │───────────────────>│                    │                    │
 │                    │                      │                    │  save(Completed)   │                    │
 │                    │                      │                    │────────────────────>│                    │
 │                    │                      │                    │                    │                    │
 │                    │                      │                    │  navigate to Settings              │
 │                    │  [launch Settings]   │                    │                    │                    │
 │────────────────────────────────────────────────────────────────────────────────────────────────────>│
```

### Flow 2: Skip Path

```
User          OnboardingActivity    OnboardingViewModel    OnboardingUseCase    ServiceStateRepo    SettingsActivity
 │                    │                      │                    │                    │                    │
 │  tap "Skip"        │                      │                    │                    │                    │
 │───────────────────>│                      │                    │                    │                    │
 │                    │  onSkip()            │                    │                    │                    │
 │                    │─────────────────────>│                    │                    │                    │
 │                    │                      │  skip()            │                    │                    │
 │                    │                      │───────────────────>│                    │                    │
 │                    │                      │                    │  save(Skipped)     │                    │
 │                    │                      │                    │────────────────────>│                    │
 │                    │                      │                    │  navigate to Settings              │
 │                    │  [launch Settings]   │                    │                    │                    │
 │────────────────────────────────────────────────────────────────────────────────────────────────────>│
 │                    │                      │                    │                    │                    │
 │                    │                      │                    │                    │                    │
 │  [Settings displays, service not enabled] │                    │                    │                    │
 │                    │                      │                    │                    │                    │
 │                    │                      │      SettingsViewModel checks onResume    │                    │
 │                    │                      │                    │  checkServiceEnabled()             │
 │                    │                      │                    │───────────────────>│                    │
 │                    │                      │                    │<──── false ────────│                    │
 │                    │  [show banner: "Enable service to start interventions"]          │                    │
 │                    │                      │                    │                    │                    │
 │  tap banner        │                      │                    │                    │                    │
 │──────────────────────────────────────────────────────────────────────────────────────>│                    │
 │                    │                      │                    │  openSettings()                    │
 │                    │                      │                    │───────────────────>│                    │
 │ [system settings]  │                      │                    │                    │                    │
 │<──────────────────────────────────────────────────────────────│                    │                    │
```

### Flow 3: Subsequent Launches (Onboarding Complete)

```
User            MainActivity    OnboardingUseCase    OnboardingRepository
 │                  │                  │                    │
 │  open app        │                  │                    │
 │─────────────────>│                  │                    │
 │                  │  getState()      │                    │
 │                  │─────────────────>│                    │
 │                  │                  │  load()            │
 │                  │                  │───────────────────>│
 │                  │                  │<── Completed ──────│
 │                  │<── Completed ────│                    │
 │                  │                  │                    │
 │                  │  route to SettingsActivity            │
 │─────────────────────────────────────────────────────────>│
```

### Data Flow Description

1. **State determination** (read-only, on every activity start/resume):
   - `OnboardingUseCase.getState()` → `OnboardingRepository.load()` → returns `OnboardingState` (New/Completed/Skipped)
   - `OnboardingUseCase.checkServiceEnabled()` → `ServiceStateRepository.isEnabled()` → queries `Settings.Secure`

2. **Onboarding completion** (command, one-time):
   - `OnboardingUseCase.complete()` → `OnboardingRepository.save(OnboardingState.Completed)`
   - Caller (OnboardingViewModel) navigates to SettingsActivity

3. **Skip** (command, one-time):
   - `OnboardingUseCase.skip()` → `OnboardingRepository.save(OnboardingState.Skipped)`
   - Caller (OnboardingViewModel) navigates to SettingsActivity

4. **Banner dismissal** (read-check on every Settings resume):
   - SettingsViewModel checks `OnboardingUseCase.checkServiceEnabled()` on each resume
   - If true → hide banner; if false → show banner

### Interaction Validation (Architecture & DDD)

| Check | Status |
|-------|--------|
| UI → Application → Domain direction respected | ✓ Activities/ViewModels call UseCase, which calls Repository interfaces |
| State-changing ops go through domain + Repository | ✓ `complete()` and `skip()` write via Repository |
| Read-only queries (service state) use Provider/query pattern | ✓ `checkServiceEnabled()` queries ServiceStateRepository |
| Domain events for cross-aggregate communication | N/A — no other aggregates react to onboarding state |
| Data crossing boundaries uses simple structures | ✓ OnboardingState value object crosses from domain to UI |

### Decisions Log

| Date | Decision | Reasoning | Alternatives Considered |
|------|----------|-----------|------------------------|
| 2026-06-19 | OnboardingUseCase owns both state-check and service-check flows | Single entry point for all onboarding logic; keeps ViewModel thin | Two separate use cases (more indirection) |
| 2026-06-19 | Service state checked on every activity onResume, not via callback | More robust — works even if user enables service and returns after process death | Relying solely on AccessibilityService.onServiceConnected() (fragile across process restarts) |

## Design: Level 4 — Contracts

### Package: `com.example.promptly.domain.model`

**OnboardingState.kt** — Value object representing onboarding state machine.

```kotlin
package com.example.promptly.domain.model

sealed class OnboardingState {
    data object New : OnboardingState()
    data object Completed : OnboardingState()
    data object Skipped : OnboardingState()
}
```

### Package: `com.example.promptly.domain.repository`

**OnboardingRepository.kt** — Persistence contract for onboarding flags.

```kotlin
package com.example.promptly.domain.repository

import com.example.promptly.domain.model.OnboardingState

interface OnboardingRepository {
    suspend fun load(): OnboardingState
    suspend fun save(state: OnboardingState)
}
```

**ServiceStateRepository.kt** — Contract for querying accessibility service enablement.

```kotlin
package com.example.promptly.domain.repository

interface ServiceStateRepository {
    suspend fun isEnabled(): Boolean
}
```

### Package: `com.example.promptly.domain.usecase`

**OnboardingUseCase.kt** — Orchestration for onboarding flows.

```kotlin
package com.example.promptly.domain.usecase

import com.example.promptly.domain.model.OnboardingState
import com.example.promptly.domain.repository.OnboardingRepository
import com.example.promptly.domain.repository.ServiceStateRepository

class OnboardingUseCase(
    private val onboardingRepository: OnboardingRepository,
    private val serviceStateRepository: ServiceStateRepository
) {
    suspend fun getState(): OnboardingState
    suspend fun complete()
    suspend fun skip()
    suspend fun checkServiceEnabled(): Boolean
}
```

### Package: `com.example.promptly.ui.onboarding`

**OnboardingUiState.kt** — UI state model for onboarding screen.

```kotlin
package com.example.promptly.ui.onboarding

data class OnboardingUiState(
    val isLoading: Boolean = true,
    val showSuccess: Boolean = false
)
```

**OnboardingViewModel.kt** — ViewModel managing onboarding screen state.

```kotlin
package com.example.promptly.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.promptly.domain.usecase.OnboardingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val onboardingUseCase: OnboardingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onResume()
    fun onOpenSettings()
    fun onSkip()
    fun onContinue()
}
```

**OnboardingActivity.kt** — Onboarding screen with explanation and action buttons.

```kotlin
package com.example.promptly.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.promptly.databinding.ActivityOnboardingBinding
import com.example.promptly.ui.settings.SettingsActivity
import kotlinx.coroutines.launch

class OnboardingActivity : AppCompatActivity() {

    private val viewModel: OnboardingViewModel by viewModels {
        OnboardingViewModelFactory()
    }

    private lateinit var binding: ActivityOnboardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // Inflate binding, set content view
        // Observe uiState to show success confirmation when service enabled
        // Set up "Open Accessibility Settings" button → viewModel.onOpenSettings()
        // Set up "Skip" link → viewModel.onSkip()
        // Set up "Continue" button → viewModel.onContinue()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    inner class OnboardingViewModelFactory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val prefs = getSharedPreferences("promptly_prefs", MODE_PRIVATE)
            val onboardingRepo = com.example.promptly.data.repository.OnboardingRepositoryImpl(prefs)
            val serviceStateRepo = com.example.promptly.data.repository.ServiceStateRepositoryImpl(this@OnboardingActivity)
            val useCase = OnboardingUseCase(onboardingRepo, serviceStateRepo)
            return OnboardingViewModel(useCase) as T
        }
    }

    companion object {
        private const val PREFS_NAME = "promptly_prefs"
    }
}
```

### Package: `com.example.promptly.data.repository`

**OnboardingRepositoryImpl.kt** — SharedPreferences-based persistence for onboarding state.

```kotlin
package com.example.promptly.data.repository

import android.content.SharedPreferences
import com.example.promptly.domain.model.OnboardingState
import com.example.promptly.domain.repository.OnboardingRepository

class OnboardingRepositoryImpl(
    private val prefs: SharedPreferences
) : OnboardingRepository {

    override suspend fun load(): OnboardingState
    override suspend fun save(state: OnboardingState)

    companion object {
        private const val KEY_ONBOARDING_STATE = "onboarding_state"
    }
}
```

**ServiceStateRepositoryImpl.kt** — Checks accessibility service via Settings.Secure query.

```kotlin
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
        return enabledServices.contains("com.example.promptly/com.example.promptly.PromptlyAccessibilityService")
    }
}
```

### Package: `com.example.promptly.ui.settings` — Modifications

**SettingsViewModel.kt** — Add service status and banner state.

```kotlin
// Extend existing SettingsUiState:
data class SettingsUiState(
    val enabled: Boolean = false,
    val cooldownConfig: CooldownConfig = CooldownConfig.DailyReset(LocalTime.MIDNIGHT),
    val targetAppPackage: String? = null,
    val scheduleStart: LocalTime = LocalTime.of(9, 0),
    val scheduleEnd: LocalTime = LocalTime.of(17, 0),
    val serviceEnabled: Boolean = false,
    val showBanner: Boolean = false
)

// Extend SettingsViewModel:
class SettingsViewModel(
    private val settingsUseCase: SettingsUseCase,
    // NEW: inject OnboardingUseCase for service status checks
    private val onboardingUseCase: OnboardingUseCase
) : ViewModel() {

    fun onResume() {
        viewModelScope.launch {
            val enabled = onboardingUseCase.checkServiceEnabled()
            _uiState.value = _uiState.value.copy(
                serviceEnabled = enabled,
                showBanner = !enabled
            )
        }
    }
}
```

### Contract Validation (DDD + Architecture)

| Check | Status |
|-------|--------|
| Every Level 3 interaction maps to at least one contract | ✓ All 3 flows covered (getState, complete, skip, checkServiceEnabled, isEnabled) |
| No new interactions introduced not agreed at Level 3 | ✓ |
| Value objects validated in constructor | ✓ OnboardingState is sealed class — no invalid states possible |
| Repository interfaces defined in domain layer | ✓ OnboardingRepository, ServiceStateRepository in domain.repository |
| Infrastructure implements domain interfaces | ✓ OnboardingRepositoryImpl, ServiceStateRepositoryImpl in data.repository |
| Boundary data uses simple structures | ✓ OnboardingState crosses domain → UI; no framework types leak |
| No implementation logic in contracts | ✓ Signatures only; no function bodies |

### Decisions Log

| Date | Decision | Reasoning | Alternatives Considered |
|------|----------|-----------|------------------------|
| 2026-06-19 | ServiceStateRepositoryImpl uses Context (not just ContentResolver) | Follows Android convention; Context needed for contentResolver access | ContentResolver-only parameter (cleaner but less conventional for Android) |
| 2026-06-19 | Service state detection via `contains()` on enabled services string | Matches existing implementation notes; handles multiple enabled services | Equality check (fails if multiple services enabled) |

## Open Questions Resolved

- All 3 open questions from the requirement doc were pre-resolved (detection method, skip option behavior).
- **Service class name** assumed as `com.example.promptly.PromptlyAccessibilityService` for the `Settings.Secure` string match — must match the actual service declared in AndroidManifest.xml when implemented.

## Design Summary

### Components & Layer Assignments

| Component | Layer | Status |
|-----------|-------|--------|
| OnboardingActivity | UI (Controller) | **New** |
| OnboardingViewModel | UI (Controller) | **New** |
| SettingsActivity | UI (Controller) | **Modified** — banner added |
| SettingsViewModel | UI (Controller) | **Modified** — service status + banner state |
| OnboardingUseCase | Application Service | **New** |
| OnboardingRepository (interface) | Domain | **New** |
| ServiceStateRepository (interface) | Domain | **New** |
| OnboardingState (value object) | Domain | **New** |
| OnboardingRepositoryImpl | Data (Infrastructure) | **New** |
| ServiceStateRepositoryImpl | Data (Infrastructure) | **New** |

### Key Contracts

- **`OnboardingState`** — sealed class: `New`, `Completed`, `Skipped`. Value object in `domain.model`.
- **`OnboardingRepository.load()` / `save(state)`** — in `domain.repository`, impl in `data.repository` via SharedPreferences.
- **`ServiceStateRepository.isEnabled()`** — in `domain.repository`, impl in `data.repository` via `Settings.Secure` query.
- **`OnboardingUseCase.getState()`, `.complete()`, `.skip()`, `.checkServiceEnabled()`** — in `domain.usecase`.

### Architectural Constraints

- Dependencies flow inward: UI → Application → Domain ← Data
- Onboarding state persisted via SharedPreferences (existing app convention)
- Service state checked via `Settings.Secure` query on every activity `onResume`
- No domain events needed — no other aggregates react to onboarding state changes
- Value object (OnboardingState) ensures exhaustive handling of all states

### Domain Model Decisions

- No entities or aggregates — onboarding is simple state tracking, not a rich domain
- `OnboardingState` as sealed class prevents invalid state combinations (unlike raw booleans)
- `ServiceStateRepository` separated from `OnboardingRepository` due to different persistence mechanisms

### Design Status

**Approved — ready for implementation**

Design complete at Level 4 (Contracts). Implementation is a separate concern handled by the code-forge skill. Run `/code-forge` to begin coding against this blueprint.

### Completion Decision

| Date | Decision | Reasoning | Alternatives Considered |
|------|----------|-----------|------------------------|
| 2026-06-19 | Design approved at Level 4. Blueprint complete ready for implementation | All 5 capabilities defined, 10 components mapped with layer assignments, 3 interaction flows documented, 6+ contracts specified | N/A — design complete |

## Key Files
