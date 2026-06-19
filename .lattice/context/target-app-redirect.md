---
feature: target-app-redirect
requirement_doc: .lattice/requirements/features/target-app-redirect.md
created: 2026-06-20
---

# Target App Redirect

Automatically redirect to a pre-configured target app when the intervention appears, turning the passive obstacle into an active nudge.

## Decisions Log

| Date | Decision | Reasoning | Alternatives Considered |
|---|---|---|---|
| 2026-06-20 | AppRedirectUseCase returns sealed RedirectDecision enum | Pure Kotlin, no framework types leak from domain. ViewModel maps decision to UI state. | Passing Intent from domain (breaks purity); callback-based (less testable) |
| 2026-06-20 | auto-redirect delay lives in ViewModel as coroutine delay | Simple, testable via coroutines TestDispatcher. Existing pattern in codebase uses ViewModel for orchestration. | Handler in Activity (ties to lifecycle); AlarmManager (overkill) |
| 2026-06-20 | TargetAppRepository uses PackageManager.getLaunchIntentForPackage() for install check | Matches requirement doc. Null check on result is reliable detection of installed launcher app. | queryIntentActivities (more complex, same result); ACTION_PACKAGE_REMOVED broadcast (unreliable timing) |
| 2026-06-20 | clearTargetAppSetting via SettingsRepository.save() with null package | Reuses existing repository contract — no new persistence method needed. Single source of truth for settings. | New clearTargetApp on SettingsRepository (unnecessary method proliferation) |
| 2026-06-20 | stopLockTask via existing InterventionUseCase.dismiss() before redirect | Reuses established lock-task stop logic. Single code path for ending lock task — dismiss and redirect share the same mechanism. | Duplicate stopLockTask call (code duplication, two code paths to maintain) |

## Open Questions

## Constraints

- Domain layer must not import Android framework types (Intent, PackageManager, Activity)
- Auto-redirect must have brief delay (1-2s) so user registers nudge
- Promptly app must be excluded from redirect (loop prevention) — enforced at Settings picker level
- Uninstalled app must clear setting and show message
- stopLockTask must happen before startActivity

## Implementation Notes

- Auto-redirect delay set to 1500ms. Matching requirement "1-2 seconds".
- `RedirectDecision.Uninstalled` changed from data class to data object — message moved to ViewModel (UI layer) to avoid hardcoding display strings in domain. ViewModel uses literal string; `R.string.intervention_target_not_found` available for future localization.
- All three requirement scenarios implemented:
  - Scenario 1: Target app configured → auto-redirect after 1.5s delay
  - Scenario 2: No target → Dismiss-only (existing behavior, no changes)
  - Scenario 3: Uninstalled → error message + setting cleared + manual dismiss

## Key Files

- `.lattice/requirements/features/target-app-redirect.md` — feature requirements
- `app/src/main/java/com/example/promptly/domain/model/RedirectDecision.kt` — redirect decision sealed class
- `app/src/main/java/com/example/promptly/domain/repository/TargetAppRepository.kt` — target app install check interface
- `app/src/main/java/com/example/promptly/domain/usecase/AppRedirectUseCase.kt` — redirect orchestration
- `app/src/main/java/com/example/promptly/data/repository/TargetAppRepositoryImpl.kt` — PackageManager-based install check
- `app/src/main/java/com/example/promptly/ui/intervention/InterventionUiState.kt` — UI state with redirect modes
- `app/src/main/java/com/example/promptly/ui/intervention/InterventionViewModel.kt` — redirect orchestration in ViewModel
- `app/src/main/java/com/example/promptly/ui/intervention/InterventionActivity.kt` — redirect execution (startActivity, finish)

## Design: Level 1 -- Capabilities

1. User configures a target app in Settings (existing UI, no changes needed)
2. After unlock, the intervention shows for 1-2 seconds then auto-redirects to the configured target app
3. When no target app is configured, the intervention shows with Dismiss-only (existing behavior)
4. When the configured target app was uninstalled, the intervention shows an error message and clears the setting
5. stopLockTask completes before the target app launches, returning the user to normal lock-task-free mode

## Design: Level 2 -- Components

| Component | Layer | Responsibility |
|---|---|---|
| `TargetAppRepository` | Domain (interface) | Check if a package is installed on device |
| `TargetAppRepositoryImpl` | Data | PackageManager-based install check; clears setting via SharedPreferences |
| `AppRedirectUseCase` | Domain | Evaluate settings + install state → sealed RedirectDecision |
| `RedirectDecision` | Domain (model) | Sealed interface: NoTarget, Ready(packageName), Uninstalled(message) |
| `InterventionViewModel` | UI | After show delay, call use case, map result to UI state |
| `InterventionActivity` | UI | Observe state; on Redirecting → stopLockTask + startActivity + finish |

### Dependencies

```
InterventionViewModel → AppRedirectUseCase → SettingsRepository + TargetAppRepository
                                           ↘ RedirectDecision (model)
TargetAppRepositoryImpl implements TargetAppRepository
```

Architecture: Clean Architecture with overlay from `.lattice/standards/architecture.md`. No DDD tactical patterns apply here — this feature is infrastructure-orchestration with no complex domain invariants.

## Design: Level 3 -- Interactions

### Scenario 1: Target app configured and installed

```
InterventionViewModel        AppRedirectUseCase        SettingsRepository      TargetAppRepository      Activity
      │                             │                         │                       │                   │
      │──onCreated()──>             │                         │                       │                   │
      │                             │                         │                       │                   │
      │  [startLockTask via InterventionUseCase]                                         │                   │
      │                             │                         │                       │                   │
      │  [delay 1500ms]             │                         │                       │                   │
      │                             │                         │                       │                   │
      │──evaluate()────────────>    │                         │                       │                   │
      │                             │──load()─────────────>   │                       │                   │
      │                             │<──Settings────────────  │                       │                   │
      │                             │                         │                       │                   │
      │                             │──isInstalled(pkg)───────────────────────────>   │                   │
      │                             │<──true───────────────────────────────────────   │                   │
      │                             │                         │                       │                   │
      │<──Ready(packageName)──────  │                         │                       │                   │
      │                             │                         │                       │                   │
      │ [sets state.mode=Redirecting]                                                 │                   │
      │                                                                               │──observes────>    │
      │                                                                               │                   │
      │                                                              [stopLockTask via InterventionUseCase.dismiss()]
      │                                                                               │  [startActivity]  │
      │                                                                               │  [finish()]       │
```

### Scenario 2: No target configured

```
InterventionViewModel        AppRedirectUseCase        SettingsRepository
      │──evaluate()────>    │                         │
      │                     │──load()─────────────>   │
      │                     │<──Settings────────────  │
      │                     │                         │
      │<──NoTarget────────  │                         │
      │                                             
      │ [stays in Showing mode, Dismiss-only]       
```

### Scenario 3: Target app uninstalled

```
InterventionViewModel        AppRedirectUseCase        SettingsRepository      TargetAppRepository
      │──evaluate()────>    │                         │                       │
      │                     │──load()─────────────>   │                       │
      │                     │<──Settings────────────  │                       │
      │                     │                         │                       │
      │                     │──isInstalled(pkg)───────────────────────────>   │
      │                     │<──false───────────────────────────────────────   │
      │                     │                         │                       │
      │                     │──save(targetApp=null)─> │                       │
      │                     │                         │                       │
      │<──Uninstalled─────  │                         │                       │
      │                                             
      │ [sets errorMessage, stays in Showing mode]   
```

### Data Flow Summary

| From | To | Data | Condition |
|---|---|---|---|
| ViewModel → AppRedirectUseCase | evaluate() | No params | On unlock, after 1.5s delay |
| AppRedirectUseCase → SettingsRepository | load() | Settings { targetAppPackage } | Always |
| AppRedirectUseCase → TargetAppRepository | isInstalled(packageName) | true/false | Only when targetAppPackage != null |
| AppRedirectUseCase → SettingsRepository | save(settings.copy(targetApp=null)) | — | Only when uninstalled detected |
| AppRedirectUseCase → ViewModel | RedirectDecision | NoTarget / Ready / Uninstalled | Always |

## Design: Level 4 -- Contracts

### RedirectDecision (domain/model)

```kotlin
sealed interface RedirectDecision {
    data object NoTarget : RedirectDecision
    data class Ready(val packageName: String) : RedirectDecision
    data class Uninstalled(val message: String) : RedirectDecision
}
```

### TargetAppRepository (domain/repository)

```kotlin
interface TargetAppRepository {
    suspend fun isInstalled(packageName: String): Boolean
}
```

### AppRedirectUseCase (domain/usecase)

```kotlin
class AppRedirectUseCase(
    private val settingsRepository: SettingsRepository,
    private val targetAppRepository: TargetAppRepository
) {
    suspend fun evaluate(): RedirectDecision
}
```

### InterventionUseCase changes

No interface changes. Existing `dismiss()` reused for stopLockTask before redirect.

### InterventionUiState changes

```kotlin
data class InterventionUiState(
    val mode: InterventionMode = InterventionMode.Loading,
    val config: InterventionConfig? = null,
    val isCallInterrupted: Boolean = false,
    val errorMessage: String? = null
)

sealed class InterventionMode {
    data object Loading : InterventionMode()
    data object Showing : InterventionMode()
    data object Dismissing : InterventionMode()
    data object Redirecting : InterventionMode()
}
```

## Design Summary

### Components and Layer Assignments

| Component | Layer | Role |
|---|---|---|
| `RedirectDecision` | Domain/Model | Sealed result type for redirect evaluation |
| `TargetAppRepository` | Domain/Repository | Interface for install check |
| `TargetAppRepositoryImpl` | Data/Repository | PackageManager-based implementation |
| `AppRedirectUseCase` | Domain/UseCase | Orchestrates settings load + install check |
| `InterventionViewModel` | UI | Manages redirect timing and state |
| `InterventionActivity` | UI | Executes redirect (startActivity, finish) |

### Key Contracts

- `AppRedirectUseCase.evaluate() → RedirectDecision` — single entry point for redirect logic
- `TargetAppRepository.isInstalled(packageName: String): Boolean` — install check
- Existing `InterventionUseCase.dismiss()` reused for stopLockTask

### Architectural Constraints

- Domain layer: pure Kotlin, no Android imports. `RedirectDecision` and `TargetAppRepository` contain no framework types.
- Dependency inversion: `TargetAppRepositoryImpl` (data) implements `TargetAppRepository` (domain).
- ViewModel maps domain types to UI state types — no domain-to-UI type leakage.

### Domain Model Decisions

- `RedirectDecision` as sealed interface (union type) follows existing patterns (`InterventionConfig`, `CooldownConfig`). No DDD aggregates or value objects needed — this feature is infrastructure-orchestration, not domain modeling.

### Open Questions Resolved During Design

- Where to clear uninstalled app setting → in AppRedirectUseCase, reusing SettingsRepository.save()
- Where stopLockTask call goes → reuses existing InterventionUseCase.dismiss() from ViewModel before Activity redirect
- How Activity receives redirect command → via InterventionMode.Redirecting state in observed StateFlow

### Design Status

**Approved — implementation complete**

| 2026-06-20 | Design approved at Level 4. Blueprint complete ready for implementation. | All 4 design levels validated and captured. Architecture and DDD atoms applied. | — |
| 2026-06-20 | Implementation complete — all layers built and verified | Domain (RedirectDecision, TargetAppRepository, AppRedirectUseCase), Data (TargetAppRepositoryImpl), UI (InterventionViewModel, InterventionActivity, layout, strings). Build successful. | — |

## Decisions Log

| 2026-06-20 | Design approved at Level 4. Blueprint complete ready for implementation. | All 4 design levels validated and captured. Architecture and DDD atoms applied. | — |
| 2026-06-20 | Implementation complete — all layers implemented and verified | Domain (RedirectDecision, TargetAppRepository, AppRedirectUseCase), Data (TargetAppRepositoryImpl), UI (InterventionViewModel, InterventionActivity, layout, strings). Build successful. | — |
