---
feature: manual-intervention-launch
requirement_doc: .lattice/requirements/features/manual-intervention-launch.md
created: 2026-06-20
---

# Manual Intervention Launch

> Add a "Launch intervention now" button in settings to trigger the intervention on demand, bypassing cooldown/schedule checks.

## Decisions Log

| Date | Decision | Reasoning | Alternatives Considered |
|------|----------|-----------|------------------------|
| 2026-06-20 | No new UseCase needed — pure UI wiring | Feature involves zero business logic: no cooldown check, no eligibility check, no state mutation. Just launch InterventionActivity via Intent. Adding a UseCase would be an empty passthrough. | Adding `LaunchInterventionUseCase` as domain orchestrator — rejected because it would be a no-op |
| 2026-06-20 | SharedFlow for one-shot navigation event | Existing ViewModel uses StateFlow for persistent UI state; this is a one-shot action that should not be replayed on config change (no need to re-launch intervention after rotation). SharedFlow with default replay=0 matches the semantics. | StateFlow with consumed-flag pattern — rejected, more ceremony for same effect |
| 2026-06-20 | Button always visible regardless of enabled/disabled state | Requirement specifies: "Button is always visible regardless of enabled/disabled state or schedule window." No conditional visibility logic. | Hiding when disabled — rejected per requirement |
| 2026-06-20 | No cooldown trigger recorded | Requirement explicitly states: "No cooldown trigger is recorded." Manual launch is a preview mechanism, not an actual intervention event. | Recording cooldown trigger — rejected, would conflate preview with real intervention |
| 2026-06-20 | SharedFlow with extraBufferCapacity=1 | Default `MutableSharedFlow()` uses `extraBufferCapacity=0` which causes `emit` to suspend when no collector is attached. Given this is a UI-triggered event (button tap) with at-most-once semantics, using buffer capacity 1 allows safe emission from `viewModelScope.launch` without suspension risk when the Activity is not yet collecting. | Default buffer (0, SUSPEND) — rejected, causes `emit` suspension and potential test flakiness |

## Open Questions

<!-- All resolved during design. None remaining. -->

## Constraints

- No cooldown eligibility or schedule check bypass — this is by design, not a bypass (no cooldown recorded either)
- Button always visible regardless of enabled/disabled state or schedule window
- Intervention behavior itself must remain unchanged (timer duration, lock task logic, redirect logic)
- No new permissions or service changes

## Design: Level 1 — Capabilities

1. **Launch intervention on demand** — User taps "Launch intervention now" in settings; intervention launches immediately, bypassing cooldown/schedule/eligibility checks
2. **Auto-redirect to target app** — If a target app is configured, the 1.5s countdown runs and redirects to the target (existing behavior, unchanged)
3. **Dismiss with no target** — If no target app is selected, the intervention shows with dismiss only (existing behavior, unchanged)
4. **Handle uninstalled target** — If the selected target app was uninstalled, show error "Target app not found" and allow dismiss (existing behavior, unchanged)
5. **Return to settings** — After dismiss, user returns to SettingsActivity via natural back-stack navigation

## Design: Level 2 — Components

| Component | Layer | Responsibility | New/Existing |
|---|---|---|---|
| `SettingsActivity` | UI | Add button click handler, observe ViewModel navigation event, launch `InterventionActivity` Intent | Existing — modified |
| `SettingsViewModel` | UI | Expose `onLaunchIntervention()` action, emit `SettingsEvent.LaunchIntervention` via SharedFlow | Existing — modified |
| `activity_settings.xml` | UI | Add "Launch intervention now" row to the settings screen layout | Existing — modified |
| `SettingsEvent` (sealed interface) | UI | One-shot navigation event type, added to `SettingsUiState.kt` | Existing — modified |
| `InterventionActivity` | UI | **Unchanged** — handles lock task, 1.5s timer, redirect evaluation, dismiss | Existing — no changes |

No new components. No domain layer changes. No data layer changes.

**Layer validation** (architecture checklist):
- ✅ Business logic in domain layer — no business logic exists for this feature
- ✅ ViewModel depends on no data sources — only emits an event
- ✅ No Android framework types in domain — N/A, no domain changes
- ✅ Activity handles framework operation (Intent launch) — correct layering
- ✅ No circular dependencies
- ✅ Activity has no data-access logic

## Design: Level 3 — Interactions

```
┌──────────────────┐     tap "Launch"     ┌──────────────────┐
│  SettingsActivity │ ──────────────────▶  │ SettingsViewModel │
│                   │                      │                  │
│                   │  ◀────────────────── │  emit LaunchIntervention
│                   │   SharedFlow<Event>  │                  │
│                   │                      └──────────────────┘
│                   │
│                   │  create Intent() ──▶  InterventionActivity
│                   │                      (existing flow)
│                   │                           │
│                   │                      startLockTask()
│                   │                      delay(1500ms)
│                   │                      evaluate RedirectDecision
│                   │                      redirect or show dismiss
│                   │                      finish()
│                   │                           │
│                   │  ◀── back stack returns ──┘
│  (visible again)  │
└──────────────────┘
```

**Flow details:**
1. User taps "Launch intervention now" button
2. `SettingsActivity` calls `viewModel.onLaunchIntervention()`
3. `SettingsViewModel` emits `SettingsEvent.LaunchIntervention` on `SharedFlow`
4. `SettingsActivity` collects event → creates `Intent(this@SettingsActivity, InterventionActivity::class.java)` → calls `startActivity(intent)`
5. `InterventionActivity` launches → its ViewModel runs existing lifecycle:
   - `interventionUseCase.prepare()` → starts lock task mode (may fail if accessibility service disabled → returns `InterventionConfig.Fallback`)
   - Sets state to `Showing`
   - Waits 1.5s (`delay(1500L)`)
   - Calls `appRedirectUseCase.evaluate()`:
     - `Ready(packageName)` → dismiss lock task → `Redirecting` → launch target app → finish()
     - `Uninstalled` → set error "Target app not found"
     - `NoTarget` → stay in `Showing` mode
6. User taps Dismiss → `interventionUseCase.dismiss()` → lock task stopped → `Dismissing` → activity finishes
7. Back stack returns to `SettingsActivity`

**What does NOT happen** (deliberate):
- ❌ No `CheckCooldownUseCase` call
- ❌ No `RecordCooldownTriggerUseCase` call
- ❌ No schedule window check
- ❌ No eligibility evaluation
- ❌ No state change to settings or cooldown data

## Design: Level 4 — Contracts

### SettingsEvent (add to `SettingsUiState.kt`)

```kotlin
sealed interface SettingsEvent {
    data object LaunchIntervention : SettingsEvent
}
```

### SettingsViewModel additions

```kotlin
// New members
private val _events = MutableSharedFlow<SettingsEvent>()
val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

fun onLaunchIntervention() {
    viewModelScope.launch { _events.emit(SettingsEvent.LaunchIntervention) }
}
```

### SettingsActivity additions

```kotlin
// In onCreate, alongside existing observeState():
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.LaunchIntervention -> {
                    val intent = Intent(this@SettingsActivity, InterventionActivity::class.java)
                    startActivity(intent)
                }
            }
        }
    }
}
```

### Layout addition (in `activity_settings.xml`)

Add a new `MaterialCardView` row below the existing cards (e.g., after the schedule section) with:
- Label: "Launch intervention now"
- Description: "Preview the intervention without waiting for cooldown or schedule"
- Clickable, with `android:onClick` or binding listener

### Files modified

| File | Change |
|---|---|
| `app/src/main/java/.../ui/settings/SettingsUiState.kt` | Add `SettingsEvent` sealed interface |
| `app/src/main/java/.../ui/settings/SettingsViewModel.kt` | Add `_events`/`events` SharedFlow, `onLaunchIntervention()` |
| `app/src/main/java/.../ui/settings/SettingsActivity.kt` | Add event observer in `onCreate`, button click wiring |
| `app/src/main/res/layout/activity_settings.xml` | Add "Launch intervention now" card |
| `app/src/main/res/values/strings.xml` | Add `settings_launch_intervention` and `settings_launch_intervention_description` |
| `app/src/test/java/.../ui/settings/SettingsViewModelTest.kt` | Add test for `onLaunchIntervention` emits event |

### Error handling

- If accessibility service not enabled → `interventionUseCase.prepare()` returns `Fallback` mode → intervention still shows (no lock task) — same as existing behavior
- If target app uninstalled → `appRedirectUseCase.evaluate()` returns `Uninstalled` → error message shown — same as existing behavior
- No target selected → `appRedirectUseCase.evaluate()` returns `NoTarget` → dismiss-only — same as existing behavior

## Design Summary

- **Components and layer assignments**: SettingsActivity + SettingsViewModel + layout (UI layer, modified). InterventionActivity (UI layer, unchanged). No domain/data layer changes.
- **Key contracts and interfaces**: `SettingsEvent.LaunchIntervention` — one-shot SharedFlow event for navigation
- **Architectural constraints**: Manual launch bypasses all cooldown/schedule/eligibility checks. No cooldown recorded. Intervention behavior unchanged. Button always visible.
- **Domain model decisions**: None — no domain changes needed
- **Open questions resolved**: All resolved during design
- **Design status**: **Implemented**
