---
feature: remove-intervention-pinning
requirement_doc: .lattice/requirements/features/remove-intervention-pinning.md
created: 2026-06-20
---

# Remove Intervention Pinning

> Remove lock task pinning from the intervention lifecycle to fix the redirect flow and allow users to exit via system navigation (Back, Home, Recents).

## Decisions Log

| Date | Decision | Reasoning | Alternatives Considered |
|------|----------|-----------|------------------------|
| 2026-06-20 | Start design at Level 1 (Capabilities) — multi-component feature | Changes span UI, Domain, and Data layers. Layer-spanning changes need full scope alignment before designing components. | Starting at Level 2 (Components) was considered but rejected because the scope affects 3 layers and an entry-level alignment prevents drift. |
| 2026-06-20 | Remove `InterventionUseCase` entirely | After removing lock task operations (prepare, dismiss, onResurface), the use case becomes an anemic no-op. Architecture doc §3.2 explicitly lists "anemic use cases" as a violation. No domain logic remains to wrap. | Keep as thin wrapper for future hooks (rejected: no concrete future requirement, YAGNI) |
| 2026-06-20 | Remove `InterventionConfig` sealed class | `Locked`/`Fallback` variants model lock-task success state. Without lock task, the sealed class has no semantic meaning and is only stored in UI state without being consumed by rendering. | Collapse to simple Boolean signal (rejected: no consumer needs it) |
| 2026-06-20 | Remove `LockTaskRepository` + `LockTaskRepositoryImpl` | No remaining consumers after `InterventionUseCase` removal. Interface and impl can be deleted entirely. | Keep for future pinning re-add (rejected: YAGNI, requirement doc accepts re-add as future work) |
| 2026-06-20 | Design approved at Level 4. Blueprint complete, ready for implementation. | All 4 design levels completed and persisted. Context doc captures capabilities, components, interactions, and contracts. | N/A |
| 2026-06-21 | Implementation complete — 6 files deleted, 3 files modified, tests updated | All components implemented per approved Level 4 contracts. Build passes, all intervention tests pass. No deviations from blueprint. | N/A |

## Open Questions

<!-- All resolved during design — see Decisions Log. -->

## Constraints

<!-- Non-negotiable once recorded. Add only when confirmed. -->
- UI layout and appearance remain unchanged (no visual changes to intervention)
- Accessibility service continues to launch via `FLAG_ACTIVITY_NEW_TASK` unchanged
- Cooldown, schedule, and other domain logic unaffected
- `excludeFromRecents` and `noHistory` manifest attributes remain
- Dismiss button remains as the primary close affordance

## Key Files

### Deleted
- `domain/model/InterventionConfig.kt` — `Locked`/`Fallback` sealed class, no longer meaningful
- `domain/repository/LockTaskRepository.kt` — interface, no consumers
- `domain/usecase/InterventionUseCase.kt` — became anemic no-op after lock task removal
- `data/repository/LockTaskRepositoryImpl.kt` — `Activity.startLockTask`/`stopLockTask` wrapper, no consumers
- `domain/usecase/InterventionUseCaseTest.kt` — tests for deleted use case
- `data/repository/LockTaskRepositoryImplTest.kt` — tests for deleted impl

### Modified
- `ui/intervention/InterventionUiState.kt` — removed `config` and `isCallInterrupted` fields
- `ui/intervention/InterventionViewModel.kt` — removed `InterventionUseCase` dependency, `onPause`, `onResumeAfterCall`, `isCallInterrupted` tracking
- `ui/intervention/InterventionActivity.kt` — removed `onPause`/`onResume` overrides, simplified factory (no `LockTaskRepositoryImpl`/`InterventionUseCase`)
- `ui/intervention/InterventionViewModelTest.kt` — removed intervention use case and call interruption tests, aligned with simplified VM

---

## Design: Level 1 — Capabilities

1. **Intervention displays without pinning** — Device is never in lock task mode. Full-screen UI (dismiss button, redirect timer) renders normally as a regular activity.
2. **Dismiss button closes the intervention** — Tapping Dismiss finishes the activity immediately without a `stopLockTask` call.
3. **System navigation exits the intervention** — Back gesture/button finishes the activity. Home/Recents sends it to background (standard behavior for a non-pinned activity).
4. **Redirect launches target app reliably** — After pre-redirect delay, target app launches via `startActivity`. No `stopLockTask` needed before redirect.
5. **Phone call interruption behaves naturally** — Activity pauses/resumes via standard lifecycle. No `isCallInterrupted` tracking or `onResurface` calls.

---

## Design: Level 2 — Components

### Removed Components

| Component | Layer | Type | Reason |
|-----------|-------|------|--------|
| `LockTaskRepository` | Domain | Interface | No longer consumed — lock task removed |
| `LockTaskRepositoryImpl` | Data | Implementation | No longer needed |
| `InterventionConfig` (sealed) | Domain | Value Object | `Locked`/`Fallback` meaningless without lock task |
| `InterventionUseCase` | Domain | Use Case | After removing lock task ops, becomes anemic no-op. Per architecture §3.2: "Anemic use cases that just pass through to repository" is a listed anti-pattern. |

### Remaining / Impacted Components

| # | Component | Layer | Responsibility |
|---|-----------|-------|--------------|
| 1 | `InterventionActivity` | UI | Lifecycle owner, state collector, launches redirect Intent. Remove `onPause`/`onResume` call interception, remove `LockTaskRepositoryImpl` from factory. |
| 2 | `InterventionViewModel` | UI | Owns `StateFlow<InterventionUiState>`. Remove `isCallInterrupted`, `onPause`, `onResumeAfterCall`, `interventionUseCase.dismiss()` before redirect. |
| 3 | `InterventionUiState` | UI | State model. Remove `config` field, remove `isCallInterrupted` field. |
| 4 | `AppRedirectUseCase` | Domain | Unchanged — evaluates redirect decision. Injected into ViewModel directly after `InterventionUseCase` removal. |

### Architecture Layer Diagram

```
   UI Layer                          Domain Layer
┌──────────────────┐            ┌────────────────────┐
│ InterventionAct. │──owns──►   │ InterventionVM     │──calls──► AppRedirectUC
│ (Activity)       │            │ (StateFlow)        │
└──────────────────┘            └────────────────────┘
```

### DDD Assessment

No new domain concepts introduced. `InterventionConfig` (removed) was a value object with two variants (`Locked`, `Fallback`) that modeled lock-task success/failure. Without lock task, the sealed class carries no semantic meaning. Clean removal — no aggregates, entities, or domain events affected.

### Architectural Validation

- **Layer placement**: All components respect defined layer boundaries. No Android framework types in domain. `AppRedirectUseCase` remains in domain (pure Kotlin). ViewModel stays in UI layer.
- **Dependency direction**: UI → Domain (via `AppRedirectUseCase`). Domain has no dependencies on UI or Data.

---

## Design: Level 3 — Interactions

### Flow 1: Intervention Display & Successful Redirect

```
InterventionActivity        InterventionViewModel        AppRedirectUseCase
      │                            │                          │
      │  onCreate()                │                          │
      │ ─────────────────► onCreated()                       │
      │                            │                          │
      │                            │ uiState = Showing        │
      │                            │ (no prepare step)        │
      │                            │                          │
      │                            │ delay(PRE_REDIRECT_MS)   │
      │                            │ evaluate()              │
      │                            │ ──────────────────────►  │
      │                            │                          │
      │                            │ ◄── RedirectDecision    │
      │                            │     .Ready(packageName)  │
      │                            │                          │
      │                            │ uiState = Redirecting    │
      │ collect: Redirecting       │ (no dismiss step)        │
      │ ◄──────────────────────────│                          │
      │ resolve launch intent      │                          │
      │ startActivity(intent)      │                          │
      │ finish()                   │                          │
```

**Data passed**: `RedirectDecision.Ready(packageName: String)` flows from Domain → UI. Nothing else crosses layers.

### Flow 2: Dismiss

```
User                    InterventionActivity          InterventionViewModel
  │                              │                          │
  │ tap Dismiss                  │                          │
  │ ──────────────────────────►  onDismissButtonClicked()   │
  │                              │                          │
  │                              │  onDismiss()             │
  │                              │ ─────────────────────►   │
  │                              │                          │
  │                              │  uiState = Dismissing    │
  │                              │  (no stopLockTask call)  │
  │                              │                          │
  │                              │ collect: Dismissing      │
  │                              │ ◄────────────────────────│
  │                              │ finish()                 │
  │ ◄────────────────────────────│                          │
```

### Flow 3: Redirect Failure (No Target / Uninstalled)

```
InterventionActivity        InterventionViewModel        AppRedirectUseCase
      │                            │                          │
      │  onCreate()                │                          │
      │ ─────────────────► onCreated()                       │
      │                            │                          │
      │                            │ uiState = Showing        │
      │                            │ delay                    │
      │                            │ evaluate()              │
      │                            │ ─────────────────────►  │
      │                            │                          │
      │                            │ ◄── RedirectDecision    │
      │                            │     .Uninstalled / NoTarget
      │                            │                          │
      │                            │ uiState.errorMessage     │
      │                            │ (stay in Showing mode)   │
      │ collect: Showing + error   │                          │
      │ ◄──────────────────────────│                          │
```

### Removed Flow: Phone Call Interruption

Previously:
- `onPause()` → set `isCallInterrupted = true`
- `onResume()` → if interrupted, call `interventionUseCase.onResurface()`
- Activity lifecycle hooks in both `InterventionActivity` and `InterventionViewModel`

After removal: Standard Android lifecycle. No special handling. `onPause`/`onResume` hooks removed from both `InterventionActivity` and `InterventionViewModel`.

### Architecture Validation

- All data flows respect clean architecture: UI → Domain (query), Domain → UI (result). No data layer accessed.
- `AppRedirectUseCase` is a pure query use case — no state mutation.
- No new boundary crossings introduced. Both removed flows (lock task operations, call interruption) reduce coupling.
- Command flow: None remains (writes were lock-task ops, all removed).
- Query flow: `evaluate()` → UI state update (read-only domain query).

### DDD Validation

- No aggregate interactions, no domain events.
- `RedirectDecision` is a pure sealed class (value object) — returned as-is from domain to UI.
- Cross-aggregate communication does not apply — only one domain concept.

---

## Design: Level 4 — Contracts

### InterventionUiState (simplified)

```kotlin
// Domain imports: none (pure UI state)

sealed class InterventionMode {
    data object Loading : InterventionMode()
    data object Showing : InterventionMode()
    data object Dismissing : InterventionMode()
    data class Redirecting(val packageName: String) : InterventionMode()
}

data class InterventionUiState(
    val mode: InterventionMode = InterventionMode.Loading,
    val errorMessage: String? = null
)
```

**Changes from current**: Removed `config: InterventionConfig?` field, removed `isCallInterrupted: Boolean` field.

### InterventionViewModel (simplified)

```kotlin
class InterventionViewModel(
    private val appRedirectUseCase: AppRedirectUseCase
) : ViewModel() {

    val uiState: StateFlow<InterventionUiState>

    fun onCreated(preRedirectDelayMs: Long = PRE_REDIRECT_DELAY_MS)
    fun onAppLaunchFailed(packageName: String)
    fun onDismiss()

    companion object {
        const val PRE_REDIRECT_DELAY_MS = 1500L
    }
}
```

**Changes from current**: Removed `InterventionUseCase` constructor parameter. Removed `onPause()`. Removed `onResumeAfterCall()`. Internal logic: no `interventionUseCase.prepare()` call in `onCreated`, no `interventionUseCase.dismiss()` call before redirect, no `isCallInterrupted` tracking.

### InterventionActivity (simplified)

```kotlin
class InterventionActivity : AppCompatActivity() {

    private val viewModel: InterventionViewModel by viewModels {
        InterventionViewModelFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?)
    fun onDismissButtonClicked()

    inner class InterventionViewModelFactory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T
    }

    companion object {
        private const val PREFS_NAME = "promptly_prefs"
    }
}
```

**Changes from current**: Removed `onPause()` → `viewModel.onPause()`. Removed `onResume()` → `viewModel.onResumeAfterCall()`. Factory no longer creates `LockTaskRepositoryImpl` or `InterventionUseCase`.

### Removed Contracts (delete entirely)

```kotlin
// DELETE: domain/repository/LockTaskRepository.kt
interface LockTaskRepository {
    suspend fun start(): Boolean
    suspend fun stop()
}

// DELETE: data/repository/LockTaskRepositoryImpl.kt
class LockTaskRepositoryImpl(private val activity: Activity) : LockTaskRepository {
    override suspend fun start(): Boolean
    override suspend fun stop()
}

// DELETE: domain/model/InterventionConfig.kt
sealed class InterventionConfig {
    data object Locked : InterventionConfig()
    data object Fallback : InterventionConfig()
}

// DELETE: domain/usecase/InterventionUseCase.kt
class InterventionUseCase(private val lockTaskRepository: LockTaskRepository) {
    suspend fun prepare(): InterventionConfig
    suspend fun dismiss()
    suspend fun onResurface(): InterventionConfig
}
```

### Unchanged Contracts

```kotlin
// domain/usecase/AppRedirectUseCase.kt — NO CHANGES
class AppRedirectUseCase(
    private val settingsRepo: SettingsRepository,
    private val targetAppRepo: TargetAppRepository
) {
    suspend operator fun invoke(): RedirectDecision
}

// domain/model/RedirectDecision.kt — NO CHANGES
sealed class RedirectDecision {
    data class Ready(val packageName: String) : RedirectDecision()
    data object Uninstalled : RedirectDecision()
    data object NoTarget : RedirectDecision()
}
```

### Architecture Validation

- **Boundary rules**: All removed interfaces were in the correct layers (domain interfaces, data impl). Their removal simplifies without violating boundaries.
- **Interface ownership**: `AppRedirectUseCase` remains in domain. ViewModel depends on it via constructor injection. No new interfaces introduced.
- **Data crossing**: `RedirectDecision` is a domain type flowing to UI — already established pattern.
- **Framework-free domain**: Remaining domain types (`RedirectDecision`, `AppRedirectUseCase`) have zero Android imports. ✓

### DDD Validation

- No new value objects, entities, aggregates, or domain events introduced.
- `RedirectDecision` remains a valid value object (immutable, self-validating sealed class).
- `AppRedirectUseCase` is a query-only domain service — no mutations, no side effects.

---

## Design Summary

### Components and Layer Assignments

| Component | Layer | Action |
|-----------|-------|--------|
| `InterventionActivity` | UI | Modify — remove lifecycle hooks and factory wiring |
| `InterventionViewModel` | UI | Modify — remove call-interruption tracking, lock-task calls |
| `InterventionUiState` | UI | Modify — remove `config`, `isCallInterrupted` fields |
| `AppRedirectUseCase` | Domain | Unchanged |
| `RedirectDecision` | Domain | Unchanged |
| `InterventionUseCase` | Domain | **Delete** |
| `InterventionConfig` | Domain | **Delete** |
| `LockTaskRepository` | Domain | **Delete** |
| `LockTaskRepositoryImpl` | Data | **Delete** |

### Key Contracts and Interfaces

- **Removed**: `LockTaskRepository` interface, `LockTaskRepositoryImpl` class, `InterventionConfig` sealed class, `InterventionUseCase` class
- **Simplified**: `InterventionUiState` (no `config`, no `isCallInterrupted`); `InterventionViewModel` (no `InterventionUseCase` dependency, no `onPause`/`onResumeAfterCall`)
- **Unchanged**: `AppRedirectUseCase`, `RedirectDecision`, `activity_intervention.xml`

### Architectural Constraints

- Clean architecture with UI → Domain dependency direction
- No Android framework types in domain layer (verified: all remaining domain types are pure Kotlin)
- ViewModel delegates to `AppRedirectUseCase` for redirect evaluation (no direct data source access)
- No DI framework — manual constructor injection in Activity factory

### Domain Model Decisions

- `InterventionConfig` removed entirely — its `Locked`/`Fallback` variants modeled lock-task state, which no longer exists
- No new domain concepts introduced
- `RedirectDecision` (value object) remains as sole cross-boundary type

### Open Questions Resolved During Design

| Question | Resolution |
|----------|-----------|
| Should `InterventionUseCase` be kept as thin wrapper? | **Remove** — becomes anemic no-op after lock task ops removed (YAGNI) |
| Should `InterventionConfig` be collapsed to Boolean? | **Remove** entirely — no consumer needs it |

### Design Status

**Approved — ready for implementation**

<!-- End of document -->
