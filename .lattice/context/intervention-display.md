---
feature: Intervention Display
requirement_doc: .lattice/requirements/features/intervention-display.md
created: 2026-06-19
---

# Intervention Display

> Full-screen activity triggered on phone unlock that blocks phone use via Lock Task Mode until the user taps Dismiss.

## Decisions Log

| Date | Decision | Reasoning | Alternatives Considered |
|------|----------|-----------|------------------------|
| 2026-06-19 | Level 1 approved with 5 capabilities matching the requirement spec scope | All scenarios map directly to capabilities; no scope reduction or extension needed | N/A |
| 2026-06-19 | InterventionConfig sealed class (Locked/Fallback) instead of raw Boolean | Compiler-enforced exhaustive handling; mode determines UI behavior at compile time | Raw Boolean `isLockTaskEnabled` (simpler but no type safety) |
| 2026-06-19 | LockTaskRepository as domain interface with Data impl wrapping Activity.startLockTask | Follows existing clean architecture pattern (like ServiceStateRepositoryImpl wrapping Context); testable via interface mock | Direct Activity method calls without abstraction (untestable, couples domain to framework) |
| 2026-06-19 | InterventionUseCase owns prepare + dismiss orchestration, even though thin | Consistent with architecture principle: all operations route through UseCase. Leaves room for business rules (cooldown handoff, schedule checks) | No UseCase (ViewModel → Activity directly) — simpler but breaks layer convention |
| 2026-06-19 | InterventionViewModel mode modeled as sealed class (Loading/Showing/Dismissing) | Makes state machine explicit; compiler prevents illegal transitions | Raw Boolean flags (allows invalid state combos like isLoading && isDismissing) |
| 2026-06-19 | Implementation: onPause/onResume lifecycle hooks for call interruption, not onSaveInstanceState | ViewModel tracks an `isCallInterrupted` boolean; onResume after interruption calls onResurface() to re-establish lock task | Tracking interruption in saved instance state (over-engineered for ephemeral intervention) |
| 2026-06-19 | Implementation: ViewModel.onPause() synchronously checks mode rather than launching coroutine | No I/O needed — pure in-memory state update on main thread | Launching a coroutine for a non-suspend state read (unnecessary coroutine overhead) |
| 2026-06-19 | Implementation: InterventionActivity uses `android:onClick="onDismissButtonClicked"` in XML | Simple declarative binding consistent with existing Activity click handlers | Programmatic setOnClickListener (equivalent, XML is more concise) |
| 2026-06-19 | Implementation: Activity declared with `android:excludeFromRecents="true"` and `android:noHistory="true"` | Intervention is transient — should not appear in recent tasks or back stack; noHistory ensures finish on navigation away | singleTask launch mode (unnecessary complexity; lock task handles pinning) |

## Open Questions

## Constraints

- InterventionActivity must be launched from AccessibilityService (ACTION_USER_PRESENT broadcast)
- Lock Task Mode (`startLockTask`/`stopLockTask`) is an Activity API — wrapper must receive Activity reference
- Intervention is purely transient — no persisted state across process death
- Activity must resume Lock Task Mode after incoming call interruption
- Fallback mode (no lock task) still shows the intervention but user can bypass via Home/Recents
- No animations, transitions, or sound effects (per scope)

## Key Files

| Path | Role | Status |
|------|------|--------|
| `domain/model/InterventionConfig.kt` | Sealed class — Locked, Fallback. Value object for intervention mode. | **Created** |
| `domain/repository/LockTaskRepository.kt` | Interface: `start(): Boolean`, `stop()`. | **Created** |
| `domain/usecase/InterventionUseCase.kt` | Orchestrator — prepare, dismiss, onResurface. | **Created** |
| `data/repository/LockTaskRepositoryImpl.kt` | Activity-based implementation wrapping startLockTask/stopLockTask. | **Created** |
| `ui/intervention/InterventionUiState.kt` | UI state model + InterventionMode sealed class. | **Created** |
| `ui/intervention/InterventionViewModel.kt` | Holds `StateFlow<InterventionUiState>`, exposes lifecycle-tied actions. | **Created** |
| `ui/intervention/InterventionActivity.kt` | AppCompatActivity with view binding, lock task lifecycle, Dismiss button. | **Created** |
| `res/layout/activity_intervention.xml` | Full-screen layout with Dismiss button. | **Created** |
| `domain/usecase/InterventionUseCaseTest.kt` | Unit tests for prepare/dismiss/onResurface flows. | **Created** |
| `data/repository/LockTaskRepositoryImplTest.kt` | Unit tests for start/stop with success and failure paths. | **Created** |
| `ui/intervention/InterventionViewModelTest.kt` | Unit tests for state machine transitions. | **Created** |

## Design: Level 1 — Capabilities

1. **Full-screen intervention on unlock** — When the user unlocks their phone during an active schedule, a full-screen activity appears over the launcher, blocking access to apps until dismissed.
2. **Lock Task Mode enforcement** — Back, Home, and Recents buttons are disabled while the intervention is active, preventing the user from bypassing it.
3. **One-tap dismiss** — A single Dismiss button stops Lock Task Mode and closes the intervention, returning the user to normal phone use.
4. **Graceful degradation when service unavailable** — If the accessibility service is disabled or Lock Task Mode fails, the feature silently no-ops without crashing or logging errors.
5. **Incoming call resilience** — If a phone call arrives during the intervention, the call UI takes over normally; when the call ends, the intervention resumes with Lock Task Mode re-established.

## Design: Level 2 — Components

### Component Table

| # | Component | Layer | Responsibility |
|---|-----------|-------|---------------|
| 1 | InterventionActivity | UI (Controller) | Full-screen activity; manages lock task lifecycle; handles Dismiss and call interruption resume |
| 2 | InterventionViewModel | UI (Controller) | UI state machine (Loading → Showing → Dismissing); exposes onCreated, onDismiss, onResumeAfterCall |
| 3 | InterventionUseCase | Domain | Orchestrates: prepare() returns InterventionConfig, dismiss() cleans up. Coordinates with LockTaskRepository |
| 4 | LockTaskRepository (interface) | Domain | Contract for system lock task APIs: start(): Boolean, stop() |
| 5 | InterventionConfig | Domain (Value Object) | Sealed class: Locked (with lock task) or Fallback (user can bypass) |
| 6 | LockTaskRepositoryImpl | Data (Infrastructure) | Wraps Activity.startLockTask/stopLockTask via try-catch; returns false on failure |

### Dependency Diagram

```
UI Layer:
  InterventionActivity → InterventionViewModel

Domain Layer:
  InterventionUseCase ← InterventionViewModel
  LockTaskRepository (interface) ← InterventionUseCase
  InterventionConfig (value object) ← consumed by InterventionUseCase, InterventionViewModel

Data Layer:
  LockTaskRepositoryImpl : LockTaskRepository (wraps Activity APIs)
```

Dependency direction: UI → Domain ← Data (implements)

### DDD Classifications

- **Value Objects**: InterventionConfig (sealed class: Locked, Fallback). Immutable, equality by value, no identity tracking.
- **Entities**: None — no object tracked through lifecycle changes.
- **Aggregates**: None — no transactional invariants across multiple objects.
- **Domain Events**: None — no other aggregates react to intervention dismissal.
- **Domain Services**: None.

### Component Layer Validation (Architecture)

| Check | Status |
|-------|--------|
| UI layer does only translation and event forwarding | ✓ InterventionActivity/ViewModel forward user actions to InterventionUseCase |
| UseCase orchestrates, doesn't embed business rules | ✓ InterventionUseCase coordinates LockTaskRepository; no domain logic beyond mode selection |
| Domain contains no outer-layer imports | ✓ Interfaces and value objects have no Android dependencies |
| Data implements domain interfaces | ✓ LockTaskRepositoryImpl implements LockTaskRepository |
| State-changing op (lock task) uses Repository | ✓ LockTaskRepository handles start/stop |
| Fallback flow handled at UseCase level | ✓ InterventionUseCase.prepare() returns InterventionConfig.Fallback when start() fails |

## Design: Level 3 — Interactions

### Flow 1: Unlock — Show Intervention

```
AccessibilityService              InterventionActivity    InterventionViewModel    InterventionUseCase    LockTaskRepo
       │                                  │                       │                       │                    │
       │  startActivity(                                                  │
       │  InterventionActivity)           │                       │                       │                    │
       │─────────────────────────────────>│                       │                       │                    │
       │                                  │  onCreate             │                       │                    │
       │                                  │──────────────────────>│                       │                    │
       │                                  │                       │  prepare()            │                    │
       │                                  │                       │──────────────────────>│                    │
       │                                  │                       │                       │  start()           │
       │                                  │                       │                       │───────────────────>│
       │                                  │                       │  <──── true/false ────│                    │
       │                                  │                       │<── InterventionConfig.│                    │
       │                                  │<── config ───────────│  Locked or Fallback   │                    │
       │                                  │                       │                       │                    │
       │  [apply lock task if            │                       │                       │                    │
       │   config is Locked]              │                       │                       │                    │
       │  [render full-screen UI]         │                       │                       │                    │
```

### Flow 2: Dismiss

```
User               InterventionActivity    InterventionViewModel    InterventionUseCase    LockTaskRepo
 │                         │                       │                       │                    │
 │  tap Dismiss            │                       │                       │                    │
 │────────────────────────>│                       │                       │                    │
 │                         │  onDismiss()          │                       │                    │
 │                         │──────────────────────>│                       │                    │
 │                         │                       │  dismiss()           │                    │
 │                         │                       │──────────────────────>│                    │
 │                         │                       │                       │  stop()            │
 │                         │                       │                       │───────────────────>│
 │                         │                       │                       │<───── Unit ────────│
 │                         │                       │<───── Unit ──────────│                    │
 │                         │<── mode=Dismissing ───│                       │                    │
 │  [stopLockTask]         │                       │                       │                    │
 │  [finish()]             │                       │                       │                    │
 │<────────────────────────│                       │                       │                    │
```

### Flow 3: Call Interruption — Resume

```
User         System Call UI    InterventionActivity    InterventionViewModel    InterventionUseCase    LockTaskRepo
 │                │                    │                       │                       │                    │
 │  incoming call  │                   │                       │                       │                    │
 │────────────────>│                   │                       │                       │                    │
 │                │                    │  onPause              │                       │                    │
 │                │                    │──────────────────────>│                       │                    │
 │                │                    │                       │  markInterrupted()    │                    │
 │                │                    │                       │──────────────────────>│                    │
 │                │                    │                       │<──── Unit ───────────│                    │
 │                │                    │<── store interruption─│                       │                    │
 │                │                    │                       │                       │                    │
 │  [call ends]   │                    │                       │                       │                    │
 │────────────────>│                    │                       │                       │                    │
 │                │                    │  onResume             │                       │                    │
 │                │                    │──────────────────────>│                       │                    │
 │                │                    │                       │  onResurface()       │                    │
 │                │                    │                       │──────────────────────>│                    │
 │                │                    │                       │                       │  start()           │
 │                │                    │                       │                       │───────────────────>│
 │                │                    │                       │                       │<───── true ────────│
 │                │                    │                       │<── proceed ──────────│                    │
 │                │                    │<── reapply lock task ─│                       │                    │
 │  [intervention  │                   │                       │                       │                    │
 │   back in       │                   │                       │                       │                    │
 │   foreground]   │                   │                       │                       │                    │
```

### Flow 4: Degradation (No Lock Task)

```
AccessibilityService              InterventionActivity    InterventionViewModel    InterventionUseCase    LockTaskRepo
       │                                  │                       │                       │                    │
       │  startActivity()                 │                       │                       │                    │
       │─────────────────────────────────>│                       │                       │                    │
       │                                  │  onCreate             │                       │                    │
       │                                  │──────────────────────>│                       │                    │
       │                                  │                       │  prepare()            │                    │
       │                                  │                       │──────────────────────>│                    │
       │                                  │                       │                       │  start()           │
       │                                  │                       │                       │───────────────────>│
       │                                  │                       │  <──── false ─────────│  [FAILS - returns  │
       │                                  │                       │<── Fallback ──────────│   false]           │
       │                                  │<── Fallback config ──│                       │                    │
       │                                  │                       │                       │                    │
       │  [show intervention without      │                       │                       │                    │
       │   lock task — user can bypass]   │                       │                       │                    │
       │  [Normal dismiss still works]    │                       │                       │                    │
```

### Data Flow Description

1. **Intervention preparation** (read-only, on activity create):
   - `InterventionUseCase.prepare()` → `LockTaskRepository.start()` → returns `InterventionConfig` (Locked or Fallback)
   - If `start()` returns false → `Fallback` mode; intervention still shows but without lock task

2. **Dismiss** (command):
   - `InterventionUseCase.dismiss()` → `LockTaskRepository.stop()`
   - Caller (InterventionActivity) calls `finish()` after dismiss completes
   - No persistence — intervention state is ephemeral

3. **Call interruption → resurface** (read-check on activity resume):
   - ViewModel detects `onResume` after `onPause` triggered by system call
   - `InterventionUseCase.onResurface()` → `LockTaskRepository.start()` to re-establish lock task
   - If start fails → degrade silently (fallback mode already active)

4. **Degradation**: No crash path — lock task failure handled via return value, not exception

### Interaction Validation (Architecture & DDD)

| Check | Status |
|-------|--------|
| UI → Domain direction respected | ✓ Activity/ViewModel call UseCase, which calls LockTaskRepository interface |
| State-changing ops go through domain + Repository | ✓ `dismiss()` calls `LockTaskRepository.stop()` |
| Read-only queries use Repository/Provider pattern | ✓ `prepare()` calls `LockTaskRepository.start()` as a capability check |
| Domain events for cross-aggregate communication | N/A — no other aggregates react to intervention states |
| Data crossing boundaries uses simple structures | ✓ InterventionConfig sealed class crosses domain → UI |
| Fallback/no-crash path documented and validated | ✓ Lock task failure degrades to Fallback mode silently |

## Design: Level 4 — Contracts

### Package: `com.example.promptly.domain.model`

**InterventionConfig.kt** — Value object representing intervention mode.

```kotlin
package com.example.promptly.domain.model

sealed class InterventionConfig {
    data object Locked : InterventionConfig()
    data object Fallback : InterventionConfig()
}
```

### Package: `com.example.promptly.domain.repository`

**LockTaskRepository.kt** — Contract for system Lock Task Mode operations.

```kotlin
package com.example.promptly.domain.repository

interface LockTaskRepository {
    suspend fun start(): Boolean
    suspend fun stop()
}
```

### Package: `com.example.promptly.domain.usecase`

**InterventionUseCase.kt** — Orchestration for intervention lifecycle.

```kotlin
package com.example.promptly.domain.usecase

import com.example.promptly.domain.model.InterventionConfig
import com.example.promptly.domain.repository.LockTaskRepository

class InterventionUseCase(
    private val lockTaskRepository: LockTaskRepository
) {
    suspend fun prepare(): InterventionConfig
    suspend fun dismiss()
    suspend fun onResurface(): InterventionConfig
}
```

### Package: `com.example.promptly.ui.intervention`

**InterventionUiState.kt** — UI state model for intervention screen.

```kotlin
package com.example.promptly.ui.intervention

sealed class InterventionMode {
    data object Loading : InterventionMode()
    data object Showing : InterventionMode()
    data object Dismissing : InterventionMode()
}

data class InterventionUiState(
    val mode: InterventionMode = InterventionMode.Loading,
    val config: com.example.promptly.domain.model.InterventionConfig? = null,
    val isCallInterrupted: Boolean = false
)
```

**InterventionViewModel.kt** — ViewModel managing intervention screen state.

```kotlin
package com.example.promptly.ui.intervention

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.promptly.domain.usecase.InterventionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InterventionViewModel(
    private val interventionUseCase: InterventionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(InterventionUiState())
    val uiState: StateFlow<InterventionUiState> = _uiState.asStateFlow()

    fun onCreated()
    fun onDismiss()
    fun onResumeAfterCall()
}
```

**InterventionActivity.kt** — Full-screen activity with lock task lifecycle.

```kotlin
package com.example.promptly.ui.intervention

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.promptly.databinding.ActivityInterventionBinding
import com.example.promptly.domain.model.InterventionConfig
import com.example.promptly.domain.usecase.InterventionUseCase
import com.example.promptly.data.repository.LockTaskRepositoryImpl
import kotlinx.coroutines.launch

class InterventionActivity : AppCompatActivity() {

    private val viewModel: InterventionViewModel by viewModels {
        InterventionViewModelFactory()
    }

    private lateinit var binding: ActivityInterventionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Inflate binding, set content view
        // Observe uiState to react to mode changes (Loading → Showing → Dismissing)
        // Call viewModel.onCreated() to prepare intervention
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResumeAfterCall()
    }

    fun onDismissButtonClicked() {
        viewModel.onDismiss()
    }

    inner class InterventionViewModelFactory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val lockTaskRepo = LockTaskRepositoryImpl(this@InterventionActivity)
            val useCase = InterventionUseCase(lockTaskRepo)
            return InterventionViewModel(useCase) as T
        }
    }
}
```

### Package: `com.example.promptly.data.repository`

**LockTaskRepositoryImpl.kt** — Activity-based Lock Task Mode implementation.

```kotlin
package com.example.promptly.data.repository

import android.app.Activity
import com.example.promptly.domain.repository.LockTaskRepository

class LockTaskRepositoryImpl(
    private val activity: Activity
) : LockTaskRepository {

    override suspend fun start(): Boolean {
        return try {
            activity.startLockTask()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun stop() {
        activity.stopLockTask()
    }
}
```

### Contract Validation (DDD + Architecture)

| Check | Status |
|-------|--------|
| Every Level 3 interaction maps to at least one contract | ✓ All 4 flows covered (prepare, dismiss, onResurface, start, stop) |
| No new interactions introduced not agreed at Level 3 | ✓ |
| Value objects validated in constructor | ✓ InterventionConfig is sealed class — no invalid states possible |
| Repository interfaces defined in domain layer | ✓ LockTaskRepository in domain.repository |
| Infrastructure implements domain interfaces | ✓ LockTaskRepositoryImpl in data.repository |
| Boundary data uses simple structures | ✓ InterventionConfig crosses domain → UI; no framework types leak |
| No implementation logic in contracts | ✓ Signatures only; no function bodies |

## Design Summary

### Components & Layer Assignments

| Component | Layer | Status |
|-----------|-------|--------|
| InterventionActivity | UI (Controller) | **New** |
| InterventionViewModel | UI (Controller) | **New** |
| InterventionUseCase | Domain | **New** |
| LockTaskRepository (interface) | Domain | **New** |
| InterventionConfig (value object) | Domain | **New** |
| LockTaskRepositoryImpl | Data (Infrastructure) | **New** |

### Key Contracts

- **`InterventionConfig`** — sealed class: `Locked`, `Fallback`. Value object in `domain.model`.
- **`LockTaskRepository.start(): Boolean` / `stop()`** — in `domain.repository`, impl in `data.repository` via Activity API.
- **`InterventionUseCase.prepare()`, `.dismiss()`, `.onResurface()`** — in `domain.usecase`.
- **`InterventionUiState`** + **`InterventionMode`** sealed class — UI state model in `ui.intervention`.

### Architectural Constraints

- Dependencies flow inward: UI → Domain ← Data
- Lock Task Mode operations require Activity reference (Activity API)
- Intervention config determined at runtime: try startLockTask → success = Locked, failure = Fallback
- No persisted state — intervention is ephemeral, no SharedPreferences needed
- Call interruption handled via Activity lifecycle (onPause/onResume)
- Fallback mode still renders intervention UI but allows bypass via Home/Recents

### Domain Model Decisions

- No entities or aggregates — intervention is a transient UI concern
- `InterventionConfig` as sealed class prevents invalid states (unlike raw Boolean)
- `LockTaskRepository` abstracted behind interface for testability, though Activity-coupled at data layer
- `InterventionMode` sealed class for ViewModel state machine prevents illegal transitions

### Open Questions Resolved During Design

- Lock Task Mode abstraction → `LockTaskRepository` interface (domain) + `LockTaskRepositoryImpl` (data with Activity reference)
- Fallback behavior → `InterventionConfig.Fallback` returned when `LockTaskRepository.start()` fails
- Call interruption → ViewModel tracks interruption state; `InterventionUseCase.onResurface()` re-establishes lock task
- No cooldown ownership → cooldown is owned by Cooldown Schedule feature; Intervention Display only triggers dismiss

### Design Status

**Approved — ready for implementation**

Design complete at Level 4 (Contracts). Implementation is a separate concern handled by the code-forge skill. Run `/code-forge` to begin coding against this blueprint.

### Completion Decision

| Date | Decision | Reasoning | Alternatives Considered |
|------|----------|-----------|------------------------|
| 2026-06-19 | Design approved at Level 4. Blueprint complete ready for implementation | All 5 capabilities defined, 6 components mapped with layer assignments, 4 interaction flows documented, 6+ contracts specified | N/A — design complete |
