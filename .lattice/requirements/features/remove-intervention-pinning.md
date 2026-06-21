---
feature: Remove Intervention Pinning
epic: Settings & Intervention Control
status: draft
priority: P1
depends_on: []
personas: []
source_docs: []
---

# Remove Intervention Pinning

## Problem Statement

The intervention activity uses Lock Task Mode (`startLockTask`) to pin itself, blocking the system Back button, Home gesture, and Recents overview. This pinning interferes with the redirect flow — `stopLockTask` must be called before `startActivity` for the target app, creating a fragile two-step sequence where the redirect can fail. The pinning constraint is no longer a product requirement, and its removal fixes the redirect behavior while allowing users to exit the intervention via normal system navigation.

## User / Personas

The person who installed Promptly, configured a target app, and expects the redirect to work reliably. Also the person who needs to exit the intervention using system navigation (Back, Home, Recents) when the target app is not configured or they choose not to redirect.

## Scope

**In scope:**
- Remove all `startLockTask` / `stopLockTask` calls from the intervention lifecycle
- Simplify `InterventionUseCase` — no lock task operations
- Remove `LockTaskRepository` interface and `LockTaskRepositoryImpl`
- Simplify or remove `InterventionConfig` sealed class (`Locked` / `Fallback` no longer meaningful)
- Remove `isCallInterrupted` tracking and `onPause` / `onResumeAfterCall` logic from `InterventionViewModel` and `InterventionActivity`
- Redirect launches target app via `startActivity` without a `stopLockTask` preamble
- Dismiss button finishes the activity without a `stopLockTask` call

**Out of scope:**
- UI changes to the intervention layout or appearance
- Changes to how the accessibility service launches the intervention (still uses `FLAG_ACTIVITY_NEW_TASK`)
- Changes to cooldown, schedule, or other domain logic
- Removing the dismiss button
- Changes to `excludeFromRecents` or `noHistory` manifest attributes

## Boundary Conditions

- If another part of the codebase references `LockTaskRepository` or `InterventionConfig` externally, the removal must be coordinated — verified that all references are internal to the intervention domain
- The intervention will no longer be "non-closable" — users can exit via Back/Home/Recents. This is an accepted product change
- If a future feature requires pinning again, the removed components must be re-added

## Assumptions

- No code outside `InterventionUseCase`, `InterventionViewModel`, `InterventionActivity`, and `LockTaskRepositoryImpl` references `LockTaskRepository` or relies on lock task state — verified in the current codebase
- The back button and home button exiting the intervention is an acceptable product change — the Dismiss button remains as the primary close affordance
- The existing redirect error handling (`onAppLaunchFailed`) covers launch failures without needing additional changes

## Scenarios

### Scenario 1: Intervention displays without lock task

The intervention opens as a regular activity with no pinning. The full-screen UI (dismiss button, redirect timer) renders normally.

**Acceptance Criteria:**
- Given the intervention is triggered from the accessibility service,
  when the activity is created and `onCreated` runs,
  then `startLockTask()` is never called.
- Given the intervention is displayed,
  when the user inspects the system status,
  then the device is not in lock task mode.
- Given the intervention is displayed,
  when the pre-redirect delay elapses,
  then the redirect evaluation proceeds normally.

### Scenario 2: Dismiss closes the intervention

User taps the Dismiss button and the activity finishes cleanly.

**Acceptance Criteria:**
- Given the intervention is displayed,
  when the user taps Dismiss,
  then the activity finishes immediately.
- Given the user taps Dismiss,
  then `stopLockTask()` is never called.
- Given the intervention was dismissed,
  when the user returns to the device,
  then they see the app that was previously in the foreground.

### Scenario 3: Back gesture exits the intervention

User presses the system Back button or uses the Back gesture to leave the intervention.

**Acceptance Criteria:**
- Given the intervention is displayed,
  when the user presses the system Back button (or gesture),
  then the activity finishes.
- Given the intervention was exited via Back,
  when the user returns to the device,
  then they see the app that was previously in the foreground.
- Given the intervention is displayed,
  when the user presses Home or opens Recents,
  then the activity is sent to the background (standard behavior for a non-pinned activity).

### Scenario 4: Redirect launches target app without stopLockTask

After the pre-redirect delay, the intervention successfully navigates to the configured target app.

**Acceptance Criteria:**
- Given a target app is configured,
  when the pre-redirect delay elapses and the redirect decision is `Ready`,
  then the target app is launched via `startActivity`.
- Given the redirect launches the target app,
  then `stopLockTask()` is never called.
- Given the redirect succeeds,
  then the intervention activity finishes after launching the target app.
- Given the target app package cannot be resolved or launch throws,
  then `onAppLaunchFailed` is called and the intervention shows an error (existing behavior, unchanged).

### Scenario 5: Phone call interruption

A phone call interrupts the intervention, then the user returns — the activity resumes normally without re-establishing pinning.

**Acceptance Criteria:**
- Given the intervention is displayed,
  when an incoming call causes the activity to pause,
  then the activity goes to the background normally.
- Given the activity was paused due to a call,
  when the call ends and the activity resumes,
  then the intervention is still visible in its previous state.
- Given the activity resumes after interruption,
  then no `startLockTask()` or `onResurface()` call is made.
- Given the user returns from a call interruption,
  then the `isCallInterrupted` field is never set (removed from the state model).

## Implementation Notes

1. **Remove lock task from domain + data layers** — Simplify `InterventionUseCase` (remove `prepare`/`dismiss`/`onResurface` lock task calls; make `prepare` return `Unit` or remove if nothing depends on it). Remove `LockTaskRepository` interface and `LockTaskRepositoryImpl` class. Remove `InterventionConfig` sealed class since `Locked`/`Fallback` no longer carries meaning.
2. **Remove pinning from UI layer** — Clean `InterventionViewModel`: remove `interventionUseCase.dismiss()` call in `onCreated` before redirect, remove `isCallInterrupted` tracking, remove `onPause`/`onResumeAfterCall` logic. Clean `InterventionActivity`: remove `LockTaskRepositoryImpl` from ViewModel factory, remove `onPause`/`onResume` lifecycle hooks for call interruption. Simplify `InterventionUiState`: remove `isCallInterrupted` field, remove `config` field (or keep as nullable if needed elsewhere).
3. **Verify redirect end-to-end** — Confirm redirect launches target app cleanly without `stopLockTask` preamble. Verify both success and failure paths (`onAppLaunchFailed`). Verify Dismiss still works. Verify Back/Home exit works.

## Open Questions

- [ ] Should `InterventionUseCase` be kept as a thin wrapper or removed entirely if it becomes a no-op? If kept, its `prepare()` method still provides a hook for future pre-intervention logic.
- [ ] Should `InterventionConfig` model be removed entirely or collapsed into a simpler signal (e.g., a `Boolean` indicating whether the intervention is ready)? Currently only stored in `InterventionUiState.config` and not consumed by the UI rendering.

## Links

- Design: *(updated when design-blueprint creates a context anchor doc for this feature)*
- Epic index: [index.md](../index.md)
