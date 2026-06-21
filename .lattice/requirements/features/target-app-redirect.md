---
feature: Target App Redirect
epic: Intervention Engine
status: draft
priority: P1
depends_on:
  - Intervention Display
personas:
  - Distracted Professional — redirects to notes, calendar, or task manager
  - Focus Seeker — redirects to reading, journaling, or study app
  - Parent or Guardian — redirects to educational content or family app
source_docs:
  - CONTEXT.md (domain glossary + technical constraints)
  - User requirements list
---

# Target App Redirect

## Problem Statement

The intervention creates a pause at unlock, but without a productive next action the user will dismiss it and return to whatever they were doing. By automatically redirecting to a pre-configured app, the intervention turns from a passive obstacle into an active nudge toward the user's intended activity.

## User / Personas

Same personas as Intervention Display. The redirect targets differ by persona:
- Distracted Professional: notes, calendar, or task management app
- Focus Seeker: reading, journaling, or study app
- Parent or Guardian: educational content or family communication app

## Scope

**In scope:**
- Auto-redirect to the configured target app when intervention appears
- Fallback to Dismiss-only intervention when no target app is configured
- Detection and handling of uninstalled target app (clear setting, show message)
- Target app package name sourced from the free-form text entry in Settings UI
- stopLockTask before launching target app

**Out of scope:**
- Multiple target apps shown on intervention for user to pick from
- Deep links to specific content within the target app
- App suggestion or rotation logic
- Web URLs as targets
- Per-schedule target app switching

## Boundary Conditions

- Target app is uninstalled after configuration: detect at unlock time, clear setting, show message
- Target app is updated or disabled: falls under "not installed" check
- User revokes app's install: handled by PackageManager query returning empty
- Target app is the Promptly app itself: excluded from the picker (infinite redirect loop prevention) ✓ confirmed

## Assumptions

- The target app package name is entered by the user in the Settings UI (handled by Settings UI feature)
- The package name is validated for format at entry time, but installation is only verified at redirect time
- `startActivity()` with the target package's launch intent works for any installed app with a launcher activity
- The app uninstall broadcast (ACTION_PACKAGE_REMOVED) may not arrive in time — checking at unlock time via PackageManager is the reliable detection point

## Scenarios

### Scenario 1: Target app configured — auto-redirect on unlock
A valid target app is configured and installed. The user unlocks their phone.

**Acceptance Criteria:**
- Given a target app is configured in Settings and the app is installed,
- when the user unlocks the phone,
- then the intervention is displayed for 1-2 seconds.
- Given the brief display period ends,
- when the auto-redirect triggers,
- then the target app opens.
- Given the auto-redirect triggered,
- when the target app opens,
- then lock task mode is ended before the target app is fully displayed.
- Given the intervention is no longer needed,
- when the target app is displayed,
- then the intervention activity has finished and does not appear in the back stack.

### Scenario 2: No target app configured — manual dismiss only
No target app is set in Settings. The intervention displays with Dismiss as the only action.

**Acceptance Criteria:**
- Given no target app is configured in Settings,
- when the user unlocks the phone,
- then the intervention appears with a Dismiss button and no redirect mechanism.
- Given the intervention shows in Dismiss-only mode,
- when the user taps Dismiss,
- then `stopLockTask()` is called and the activity finishes.

### Scenario 3: Target app was uninstalled
The configured target app was removed from the device after configuration.

**Acceptance Criteria:**
- Given a target app is configured in Settings,
- when that app is no longer installed on the device,
- then the auto-redirect does not trigger.
- Given the configured app is uninstalled,
- when the intervention appears,
- then it shows a Dismiss button and a message: "Target app not found."
- Given the uninstalled target condition is detected,
- when the intervention appears,
- then the target app setting is cleared automatically.
- Given the setting was cleared,
- when the user unlocks again,
- then the intervention behaves as in Scenario 2 (Dismiss-only).

## Implementation Notes

1. **Auto-redirect flow** — On unlock, check if target app is configured and installed (PackageManager); if yes, call stopLockTask(), startActivity(), finish() on intervention.
2. **No-target fallback** — When no target is configured, show Dismiss-only intervention (existing Intervention Display behavior).
3. **Uninstalled target detection** — Before auto-redirect, verify app is installed via PackageManager.getLaunchIntentForPackage(); on failure, show message, clear SharedPreferences setting.

## Resolved Questions

- Auto-redirect has a brief delay (1-2 seconds) — intervention is visible so the user registers the nudge before being redirected. ✓
- Promptly is excluded from the target app picker to prevent redirect loops. ✓

## Links

- Epic index: [index.md](../index.md)
