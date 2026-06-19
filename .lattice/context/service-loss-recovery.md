---
feature: service-loss-recovery
requirement_doc: .lattice/requirements/features/service-loss-recovery.md
created: 2026-06-20
---

# Service Loss Recovery

Re-prompt users who have completed onboarding when the accessibility service is disabled.

## Design: Level 1 — Capabilities

### Approved Capabilities

As a user who has previously completed onboarding:

1. **Detect service loss** — When I open the app after the accessibility service has been disabled (by me or the system), the app detects the service is missing and shows a recovery notice instead of the normal Settings screen.
2. **See re-prompt notice** — I see a minimal, single-screen notice explaining the service needs re-enabling, without the full first-launch onboarding.
3. **Navigate to accessibility settings** — When I tap "Re-enable," the app opens the system accessibility settings page where I can turn the service back on.
4. **Detect re-enablement on return** — When I return from system settings after re-enabling, the app confirms the service is active and proceeds to the normal Settings screen.
5. **Persist re-prompt across opens** — If I close the app without re-enabling, the re-prompt appears again on the next open. The app does not skip the re-prompt until the service is confirmed active.

### Decisions

- **Detection mechanism**: `Settings.Secure.getString(ENABLED_ACCESSIBILITY_SERVICES)` on activity resume — already supported by existing `ServiceStateRepository`. Reused rather than duplicating.
- **Re-prompt scope**: Minimal screen only (no full onboarding re-display). User already knows what the service is.
- **Navigation target**: System accessibility settings via `Settings.ACTION_ACCESSIBILITY_SETTINGS` intent — standard Android pattern.
- **No push notifications**: Re-prompt only shown when app is actively opened. System-level re-prompt out of scope.

### Constraints

- Must not re-trigger first-launch onboarding when the service is disabled but onboarding was already completed.
- Must use existing `OnboardingState` flag (`Completed` / `Skipped`) as gate — re-prompt only when onboarding is not `New`.
- Re-prompt must not be skippable — service must be enabled for app function.

### Open Questions

- None resolved at this level.

---

## Design: Level 2 — Components

### Approved Components

| # | Component | Layer | Responsibility |
|---|-----------|-------|----------------|
| 1 | **ServiceLossRecoveryUseCase** | Domain (UseCase) | Orchestrate service-loss detection logic: check onboarding state + service state, determine if re-prompt is needed |
| 2 | **ServiceStateRepository** | Domain (Repository Interface) | Already exists. Query whether Promptly accessibility service is currently enabled via `Settings.Secure` |
| 3 | **OnboardingRepository** | Domain (Repository Interface) | Already exists. Load onboarding state to determine if user has completed first-launch flow |
| 4 | **RecoveryViewModel** | UI (ViewModel) | Hold re-prompt UI state, handle "Re-enable" action (navigate to settings), re-check service state on return |
| 5 | **RecoveryActivity** | UI (Activity) | Display re-prompt screen. Launch system accessibility settings on button tap. Single-purpose activity |

```
┌─────────────────────────────────────────────────────────┐
│                      UI Layer                           │
│  ┌──────────────┐    ┌──────────────────────────────┐   │
│  │ MainActivity │───▶│     RecoveryActivity         │   │
│  │  (router)    │    │  ┌────────────────────────┐  │   │
│  └──────────────┘    │  │  RecoveryViewModel     │  │   │
│                      │  │  - uiState: StateFlow  │  │   │
│                      │  │  - onReEnable()        │  │   │
│                      │  │  - onResume()          │  │   │
│                      │  └───────────┬────────────┘  │   │
│                      └──────────────┼────────────────┘   │
└─────────────────────────────────────┼────────────────────┘
                                      │ depends on
┌─────────────────────────────────────┼────────────────────┐
│                   Domain Layer      │                    │
│  ┌──────────────────────────────────┴───────────────┐   │
│  │        ServiceLossRecoveryUseCase                │   │
│  │  - invoke(): RecoveryRoutingDecision             │   │
│  └──────────────────────┬───────────────────────────┘   │
│                         │                               │
│         ┌───────────────┼───────────────┐               │
│         ▼               ▼               │               │
│  ┌────────────┐  ┌─────────────────┐    │               │
│  │Onboarding  │  │ServiceState     │    │               │
│  │Repository  │  │Repository       │    │               │
│  │(interface) │  │(interface)      │    │               │
│  └────────────┘  └─────────────────┘    │               │
│         ▲               ▲               │               │
└─────────┼───────────────┼───────────────┘               │
          │               │                               │
┌─────────┼───────────────┼──────────────────────────────┐│
│  Data Layer              │                              ││
│  ┌───────────────────────┴────────────┐                ││
│  │  ServiceStateRepositoryImpl       │                ││
│  │  (Context + Settings.Secure)       │                ││
│  └────────────────────────────────────┘                ││
│  ┌────────────────────────────────────┐                ││
│  │  OnboardingRepositoryImpl          │                ││
│  │  (SharedPreferences)               │                ││
│  └────────────────────────────────────┘                ││
└─────────────────────────────────────────────────────────┘
```

### Layer Assignments (Architecture Validation)

- **ServiceLossRecoveryUseCase** → Domain UseCase. Pure Kotlin. Orchestrates two repo calls, no Android imports. ✓
- **ServiceStateRepository** → Domain Repository interface (already exists). ✓
- **OnboardingRepository** → Domain Repository interface (already exists). ✓
- **RecoveryViewModel** → UI ViewModel. Depends on UseCase. Holds `StateFlow<RecoveryUiState>`. ✓
- **RecoveryActivity** → UI Activity. One-shot layout + intent dispatch. ✓

### DDD Classification

- **`RecoveryRoutingDecision`** → Value Object (sealed class). Three states: `ProceedToSettings` (service is active), `ShowRecovery` (service is off, user onboarded), `ProceedToOnboarding` (user is new — not handled here, routed by MainActivity upstream).
- **`RecoveryUiState`** → UI state model (data class). Pure presentation state — not a domain object.
- No aggregates or entities needed — this feature is a routing gate, not a domain model.

### Decisions

- **New UseCase vs inline in ViewModel**: Created dedicated `ServiceLossRecoveryUseCase` to keep domain logic testable without Android instrumentation. ViewModel only maps result to UI state.
- **RecoveryActivity as separate Activity**: Follows existing pattern (`OnboardingActivity`, `SettingsActivity` are all separate activities). Maintains consistency.
- **Reuse existing repositories**: `ServiceStateRepository` already queries `Settings.Secure`; `OnboardingRepository` already tracks state. No new data sources needed.

### Constraints

- `ServiceLossRecoveryUseCase` must be pure Kotlin — no `Context`, no Android imports.
- `RecoveryViewModel` must not access `Settings.Secure` directly — goes through UseCase → Repository.
- `RecoveryActivity` must register in `AndroidManifest.xml` (noHistory not needed — user can return to it via recents).

---

## Design: Level 3 — Interactions

### Approved Interaction Flows

#### Flow 1: App opens, service is disabled

```
User                    MainActivity        RecoveryUseCase    ServiceStateRepo  OnboardingRepo    RecoveryActivity
 │                          │                     │                  │                 │                  │
 │  opens app               │                     │                  │                 │                  │
 │─────────────────────────▶│                     │                  │                 │                  │
 │                          │  invoke()           │                  │                 │                  │
 │                          │────────────────────▶│                  │                 │                  │
 │                          │                     │ load()           │                 │                  │
 │                          │                     │─────────────────────────────────▶│                  │
 │                          │                     │◀─────────────────────────────────│                  │
 │                          │                     │                  │                 │                  │
 │                          │                     │ isEnabled()      │                 │                  │
 │                          │                     │─────────────────▶│                 │                  │
 │                          │                     │◀─────────────────│                 │                  │
 │                          │                     │                  │                 │                  │
 │                          │  ShowRecovery       │                  │                 │                  │
 │                          │◀────────────────────│                  │                 │                  │
 │                          │  startActivity(RecoveryActivity)       │                 │                  │
 │                          │─────────────────────────────────────────────────────────▶│                  │
 │                          │  finish()           │                  │                 │                  │
 │  sees re-prompt          │                     │                  │                 │                  │
 │◀────────────────────────────────────────────────────────────────────────────────────│                  │
```

**Data flow:**
- `OnboardingRepository.load()` → `OnboardingState` (sealed class: `New | Completed | Skipped`)
- `ServiceStateRepository.isEnabled()` → `Boolean`
- `ServiceLossRecoveryUseCase.invoke()` → `RecoveryRoutingDecision` (`ShowRecovery | ProceedToSettings | ProceedToOnboarding`)
- MainActivity receives decision, routes to appropriate Activity.

#### Flow 2: User taps "Re-enable"

```
User                    RecoveryActivity     RecoveryViewModel   Intent
 │                          │                     │                │
 │  taps "Re-enable"        │                     │                │
 │─────────────────────────▶│  onReEnable()       │                │
 │                          │────────────────────▶│                │
 │                          │                     │ startActivity  │
 │                          │                     │ (Settings.     │
 │                          │                     │  ACTION_       │
 │                          │                     │  ACCESSIBILITY_│
 │                          │                     │  SETTINGS)     │
 │                          │                     │───────────────▶│
 │                          │                     │                │
 │  system settings open    │                     │                │
 │◀─────────────────────────────────────────────────────────────────│
 │                          │                     │                │
 │  user enables service    │                     │                │
 │  and presses back        │                     │                │
 │─────────────────────────▶│  onResume()         │                │
 │                          │────────────────────▶│                │
```

**Data flow:**
- No data passed between activities — system accessibility settings are an Android system activity.
- On return, `RecoveryActivity.onResume()` triggers re-check.

#### Flow 3: Re-check after return from system settings

```
RecoveryActivity       RecoveryViewModel     ServiceLossRecoveryUseCase   ServiceStateRepo    MainActivity
 │                          │                        │                        │                    │
 │  onResume()              │                        │                        │                    │
 │─────────────────────────▶│  checkService()        │                        │                    │
 │                          │───────────────────────▶│  isEnabled()           │                    │
 │                          │                        │───────────────────────▶│                    │
 │                          │                        │◀───────────────────────│                    │
 │                          │◀───────────────────────│                        │                    │
 │                          │                        │                        │                    │
 │  if enabled →            │                        │                        │                    │
 │  start SettingsActivity  │                        │                        │                    │
 │───────────────────────────────────────────────────────────────────────────────────────────────▶│
 │  finish()                │                        │                        │                    │
```

**Data flow:**
- `ServiceStateRepository.isEnabled()` → `Boolean`.
- If `true`: `RecoveryViewModel` emits state → `RecoveryActivity` starts `SettingsActivity` and finishes itself.
- If `false`: UI remains on re-prompt screen. User can tap "Re-enable" again.

#### Flow 4: App opens, service is already enabled (normal flow)

```
User                    MainActivity        RecoveryUseCase    ServiceStateRepo  OnboardingRepo
 │                          │                     │                  │                 │
 │  opens app               │                     │                  │                 │
 │─────────────────────────▶│  invoke()           │                  │                 │
 │                          │────────────────────▶│                  │                 │
 │                          │                     │ load()           │                 │
 │                          │                     │─────────────────────────────────▶│
 │                          │                     │◀─────────────────────────────────│
 │                          │                     │ isEnabled()      │                 │
 │                          │                     │─────────────────▶│                 │
 │                          │                     │◀─────────────────│                 │
 │                          │  ProceedToSettings  │                  │                 │
 │                          │◀────────────────────│                  │                 │
 │                          │ startActivity(SettingsActivity)        │                 │
 │                          │ finish()            │                  │                 │
```

#### Flow 5: App opens, user is new (delegated to Onboarding)

```
User                    MainActivity        RecoveryUseCase    ServiceStateRepo  OnboardingRepo
 │                          │                     │                  │                 │
 │  opens app               │                     │                  │                 │
 │─────────────────────────▶│  invoke()           │                  │                 │
 │                          │────────────────────▶│  load()           │                 │
 │                          │                     │─────────────────────────────────▶│
 │                          │                     │◀── OnboardingState.New ─────────│
 │                          │  ProceedToOnboarding│                  │                 │
 │                          │◀────────────────────│                  │                 │
 │                          │ startActivity(OnboardingActivity)      │                 │
 │                          │ finish()            │                  │                 │
```

### Architecture Validation

- **Data flow direction**: UI → Domain UseCase → Repository Interface → Data (Repository Impl). Complies with layered architecture. ✓
- **Boundary crossing**: Domain UseCase returns domain model (`RecoveryRoutingDecision`). UI maps to `RecoveryUiState` internally. ✓
- **No shortcuts**: All queries go through full stack. ✓

### DDD Validation

- **Cross-aggregate communication**: No aggregates involved — this is a routing gate. No domain events needed. ✓
- **ServiceLossRecoveryUseCase** is a pure orchestration use case — stateless, no side effects. ✓

### Decisions

- **Re-check on `onResume`**: Uses standard Android lifecycle callback. No need for ActivityResultLauncher since system settings are a system activity we cannot observe directly.
- **MainActivity routes to RecoveryActivity**: Not inline. Keeps MainActivity as pure router — it decides destination and finishes. RecoveryActivity has its own ViewModel lifecycle.
- **RecoveryActivity re-checks independently**: On return from system settings, RecoveryActivity runs its own check (via ViewModel → UseCase) rather than relying on MainActivity. This avoids coupling between the two activities.

---

## Design: Level 4 — Contracts

### Approved Contracts and Type Definitions

```kotlin
// ─── Domain: Model ────────────────────────────────────────

// Routing decision produced by ServiceLossRecoveryUseCase
sealed class RecoveryRoutingDecision {
    data object ProceedToSettings : RecoveryRoutingDecision()
    data object ShowRecovery : RecoveryRoutingDecision()
    data object ProceedToOnboarding : RecoveryRoutingDecision()
}
```

```kotlin
// ─── Domain: UseCase ──────────────────────────────────────

class ServiceLossRecoveryUseCase(
    private val onboardingRepository: OnboardingRepository,
    private val serviceStateRepository: ServiceStateRepository
) {
    /** Determine what to show when the app opens. Pure Kotlin — no Android imports. */
    suspend operator fun invoke(): RecoveryRoutingDecision
}
```

```kotlin
// ─── Domain: Repository Interfaces (existing, no changes) ─

interface ServiceStateRepository {
    /** @return true if the Promptly accessibility service is listed in enabled services. */
    suspend fun isEnabled(): Boolean
}

interface OnboardingRepository {
    suspend fun load(): OnboardingState
    suspend fun save(state: OnboardingState)
}
```

```kotlin
// ─── UI: State ────────────────────────────────────────────

data class RecoveryUiState(
    val isChecking: Boolean,
    val serviceEnabled: Boolean
)
```

```kotlin
// ─── UI: ViewModel ────────────────────────────────────────

class RecoveryViewModel(
    private val serviceLossRecoveryUseCase: ServiceLossRecoveryUseCase
) : ViewModel() {

    val uiState: StateFlow<RecoveryUiState>

    /** Called when the user taps "Re-enable". Dispatches to system accessibility settings. */
    fun onReEnable()

    /** Called on activity resume to re-check service state after returning from settings. */
    fun checkService()
}
```

```kotlin
// ─── UI: Activity ─────────────────────────────────────────

class RecoveryActivity : ComponentActivity() {
    // Layout: activity_recovery.xml with:
    // - TextView: "Promptly's accessibility service needs to be re-enabled."
    // - Button: "Re-enable" → calls viewModel.onReEnable()
    //
    // onResume(): calls viewModel.checkService()
    // When serviceEnabled == true: startActivity(SettingsActivity) + finish()
}
```

### Interface Mapping (Level-3 to Level-4)

| Level 3 Flow | Contract Entry Point |
|---|---|
| MainActivity routes decision | `ServiceLossRecoveryUseCase.invoke()` → `RecoveryRoutingDecision` |
| RecoveryActivity shows re-prompt | `RecoveryViewModel.uiState` → `RecoveryUiState` |
| User taps "Re-enable" | `RecoveryViewModel.onReEnable()` → starts `Settings.ACTION_ACCESSIBILITY_SETTINGS` |
| Return from settings re-check | `RecoveryViewModel.checkService()` → `ServiceLossRecoveryUseCase.invoke()` → `isEnabled` |
| Service re-enabled | UI observes `serviceEnabled == true` → starts `SettingsActivity` |

### Architecture Validation

- **ServiceLossRecoveryUseCase** is pure Kotlin — no Android imports. ✓
- **ServiceStateRepository** interface defined in domain (existing). ✓
- **RecoveryViewModel** depends on UseCase, not repository directly. ✓
- **RecoveryActivity** depends on ViewModel, not on domain directly. ✓
- **All domain types** are pure Kotlin. ✓

### DDD Validation

- **RecoveryRoutingDecision** → Sealed class (value object). Correct — no identity, immutable, self-describing. ✓
- **RecoveryUiState** → Data class in UI layer. Not a domain object. ✓
- No entities or aggregates — correct for this feature. ✓

### Decisions

- **RecoveryRoutingDecision sealed class**: Three variants match the three routing paths. Simple and exhaustive in `when` expressions.
- **ServiceLossRecoveryUseCase reuses existing repository interfaces**: No new abstractions.
- **RecoveryUiState as simple data class**: Only two fields needed. Not exposed to domain layer.

### Constraints

- `ServiceLossRecoveryUseCase` must not import `android.*` or `kotlinx.coroutines.*` (beyond `suspend` which is Kotlin stdlib).
- `RecoveryViewModel` must not reference `Context`, `Intent`, or Android framework types.
- `RecoveryActivity` must register in `AndroidManifest.xml` with `android:exported="true"` (launched from MainActivity).

---

## Design Summary

### Components and Layer Assignments

| Component | Layer |
|---|---|
| `ServiceLossRecoveryUseCase` | Domain: UseCase |
| `ServiceStateRepository` (interface) | Domain: Repository — existing, reused |
| `OnboardingRepository` (interface) | Domain: Repository — existing, reused |
| `RecoveryViewModel` | UI: ViewModel |
| `RecoveryActivity` | UI: Activity |

### Key Contracts

- `ServiceLossRecoveryUseCase.invoke(): RecoveryRoutingDecision` — top-level routing entry point
- `RecoveryRoutingDecision` sealed class with `ProceedToSettings`, `ShowRecovery`, `ProceedToOnboarding`
- `RecoveryViewModel.uiState: StateFlow<RecoveryUiState>` — drives re-prompt UI
- `RecoveryViewModel.onReEnable()` — navigates to system accessibility settings
- `RecoveryViewModel.checkService()` — re-checks after return from system settings

### Architectural Constraints

- UseCase is pure Kotlin — no Android framework types
- ViewModel depends on UseCase, not repositories directly
- No new data layer code — both repositories already exist
- RecoveryActivity is a new Activity registered in AndroidManifest

### Domain Model Decisions

- `RecoveryRoutingDecision` = value object (sealed class)
- No new aggregates or entities needed — feature is a routing gate, not a domain model
- No domain events needed — no cross-aggregate communication

### Open Questions Resolved During Design

- **Q**: Should MainActivity do the service check inline or delegate? → **A**: Delegate to UseCase for testability.
- **Q**: Where does the re-check on return happen? → **A**: RecoveryActivity.onResume() triggers its own check via ViewModel.
- **Q**: Should the use case distinguish "onboarding not done" from "service is enabled"? → **A**: Yes — three-way routing decision to let MainActivity handle each case correctly.

### Design Status

**Approved — ready for implementation**

---

## Decisions Log

| Date | Decision | Reasoning | Alternatives Considered |
|---|---|---|---|
| 2026-06-20 | Use existing `ServiceStateRepository` and `OnboardingRepository` — no new data layer code | Repositories already exist with correct implementations. Duplication would violate DRY and create maintenance burden. | Creating a new purpose-built repository |
| 2026-06-20 | `ServiceLossRecoveryUseCase` produces three-way `RecoveryRoutingDecision` sealed class | MainActivity needs to distinguish "new user" (→ onboarding), "service on" (→ settings), and "service off + onboarded" (→ recovery). Boolean insufficient. | Returning `Boolean`, branching in MainActivity |
| 2026-06-20 | Separate `RecoveryActivity` (not inline in MainActivity) | Follows existing pattern (OnboardingActivity, SettingsActivity). Keeps MainActivity as pure router. Each screen has its own lifecycle and ViewModel. | Inline re-prompt in MainActivity |
| 2026-06-20 | Re-check on `onResume` using UseCase (not ActivityResultLauncher) | System accessibility settings cannot be observed via `ActivityResultLauncher` (they're a system activity, not app-owned). `onResume` is the standard Android pattern for this. | `ActivityResultLauncher` with `startActivityForResult` |
| 2026-06-20 | RecoveryActivity starts SettingsActivity directly on re-enable detection | Clean separation: RecoveryActivity handles the re-prompt lifecycle end-to-end. No coupling to MainActivity routing logic. | Calling back to MainActivity via result |
| 2026-06-20 | `checkService()` in ViewModel maps `useCase()` result to boolean via type check | Simpler than creating a dedicated re-check method on the use case. Reuses existing `ServiceLossRecoveryUseCase.invoke()`. Safe because onboarding state is invariant between MainActivity routing and RecoveryActivity re-check. | Adding `isEnabled()`-only method to use case |
| 2026-06-20 | Layout uses ProgressBar + visibility groups matching OnboardingActivity pattern | Consistent with existing UI patterns. No skip button or dismiss option — re-prompt is not skippable per requirements. | Dedicated loading screen |

---

## Key Files

| File | Purpose |
|---|---|---|
| `domain/model/RecoveryRoutingDecision.kt` | Sealed class for routing decision |
| `domain/usecase/ServiceLossRecoveryUseCase.kt` | Orchestrates detection logic |
| `ui/recovery/RecoveryUiState.kt` | UI state data class |
| `ui/recovery/RecoveryViewModel.kt` | ViewModel for re-prompt |
| `ui/recovery/RecoveryActivity.kt` | Re-prompt screen |
| `res/layout/activity_recovery.xml` | Re-prompt layout |
| `domain/model/RecoveryRoutingDecisionTest.kt` | Tests for routing decision value object |
| `domain/usecase/ServiceLossRecoveryUseCaseTest.kt` | Tests for service loss use case (5 scenarios) |
| `ui/recovery/RecoveryViewModelTest.kt` | Tests for recovery ViewModel (4 scenarios) |
