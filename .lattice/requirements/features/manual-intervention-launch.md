---
feature: Manual Intervention Launch
epic: Settings & Intervention Control
status: draft
priority: P1
depends_on: []
personas: ["End user who wants to check the intervention without breaking their schedule", "Developer/tester validating behavior"]
source_docs: []
---

# Manual Intervention Launch

## Problem Statement

The intervention can only be triggered by launching the target app during an active schedule window with an expired cooldown. There is no way to see or test the intervention on demand — users who want to check how it looks, or developers testing the behavior, must manipulate schedule/cooldown settings or wait.

## User / Personas

- **End user** who wants to preview or trigger the intervention to check how it works
- **Developer/tester** validating intervention behavior

## Scope

**In scope:**
- A "Launch intervention now" button/row in the settings screen
- Tapping it launches the intervention activity directly, bypassing cooldown eligibility and schedule checks
- No cooldown trigger is recorded
- The intervention works exactly as it does today — lock task, 1.5s timer, redirect to target app or dismiss
- After dismiss, the user is back in settings with normal app interaction

**Out of scope:**
- Changes to intervention behavior, timer duration, lock task logic, or redirect logic
- New permissions or service changes
- Adding or removing the accessibility service requirement

## Boundary Conditions

- If the accessibility service is not enabled, the intervention still launches (lock task may fail, falling back to Fallback mode)
- If no target app is selected, the intervention shows and allows dismiss without redirect (same as normal flow with NoTarget)
- Button is always visible regardless of enabled/disabled state or schedule window

## Assumptions

- The intervention lifecycle is fully self-contained and does not require cooldown pre-checks
- InterventionActivity is launchable from any context (it already accepts a generic intent from the accessibility service)

## Scenarios

### Scenario 1: User launches intervention with target app configured

The user has a target app selected, taps the manual launch button, sees the intervention, and is redirected after the timer.

**Acceptance Criteria:**
- Given settings has a target app selected, when the user taps "Launch intervention now", then the intervention screen opens in lock task mode
- Given the intervention is showing in lock task mode, when the 1.5s timer expires, then the user is redirected to the target app's launch intent
- Given the user is redirected to the target app, when they press back or switch apps, then no lock task is active and they can navigate freely

### Scenario 2: User launches intervention with no target app configured

No target app is selected, so the intervention shows with dismiss only.

**Acceptance Criteria:**
- Given no target app is selected, when the user taps "Launch intervention now", then the intervention screen appears with the dismiss button visible
- Given the intervention is showing without a redirect target, when the user taps dismiss, then the activity finishes and the user returns to settings

### Scenario 3: User launches intervention with an uninstalled target app

The selected target app has been uninstalled.

**Acceptance Criteria:**
- Given the selected target app has been uninstalled, when the user taps "Launch intervention now", then the intervention screen appears with the error message "Target app not found"
- Given the intervention is showing an error, when the user taps dismiss, then the activity finishes and the user returns to settings

## Implementation Notes

1. Add "Launch intervention now" button/row to the settings screen layout
2. Wire the tap to launch InterventionActivity via a direct Intent

## Open Questions

None.

## Links

- Design: *(updated when design-blueprint creates a context anchor doc for this feature)*
- Epic index: [index.md](../index.md)
