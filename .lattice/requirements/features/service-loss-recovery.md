---
feature: Service Loss Recovery
epic: App Configuration
status: draft
priority: P1
depends_on:
  - Settings UI
  - Onboarding & Permissions
personas:
  - Distracted Professional — accidentally disables and needs quick recovery
  - Focus Seeker — may not notice service stopped; needs clear signal
  - Parent or Guardian — may disable out of privacy concern, then forget
source_docs:
  - CONTEXT.md (domain glossary + technical constraints)
  - User requirements list
---

# Service Loss Recovery

## Problem Statement

Once a user has completed onboarding and enabled the accessibility service, the service can be disabled at any time — by the user through system settings, by the system due to battery optimization, or by a force-stop. When this happens, the app's core functionality silently stops. The user may not realize the service is off and will wonder why interventions have stopped. The app must detect service loss on open and guide the user to re-enable it without re-running the full first-launch onboarding.

## User / Personas

Any user who has previously completed onboarding. The re-prompt must be minimal and action-oriented — the user already knows what the service is and why it's needed.

## Scope

**In scope:**
- Detection of service disablement on app open via `Settings.Secure` query
- Re-prompt screen: simple notice that the service needs re-enabling + "Re-enable" button
- Navigation to system accessibility settings from re-prompt
- Detection of re-enablement on return from settings
- Return to normal Settings screen after successful re-enablement
- Re-prompt persists across app opens until service is re-enabled

**Out of scope:**
- Full first-launch onboarding re-display
- Push notification or system-level re-prompt (app must be open)
- In-place service restart without user action
- Banner inside Settings screen (belongs to skip flow in Onboarding & Permissions)

## Boundary Conditions

- System disables service due to battery optimization or force-stop → equivalent to user disabling; re-prompt on next open
- User reinstalls the app → SharedPreferences cleared; full onboarding re-triggered by Onboarding & Permissions feature
- User re-enables service directly in system settings without opening the app → next app open detects service is active; no re-prompt
- User kills the app while on the re-prompt screen → service unbound; next open triggers another re-prompt

## Assumptions

- Service disablement detected via `Settings.Secure.getString(ENABLED_ACCESSIBILITY_SERVICES)` on activity resume
- Re-enablement follows same detection path as first-launch flow: user enables in system settings, returns to app, `Settings.Secure` query confirms
- First-launch flag remains set even when service is disabled (user does not need to re-onboard)

## Scenarios

### Scenario 1: User disables service and opens the app
User completed onboarding, later disabled Promptly in accessibility settings, then opened the app.

**Acceptance Criteria:**
- Given the user previously enabled the Promptly accessibility service,
- when the user opens the Promptly app,
- then a re-prompt screen is displayed (not the full onboarding — just a notice that the service needs to be re-enabled).
- Given the re-prompt screen is displayed,
- when the user taps "Re-enable,"
- then the system accessibility settings open via `startActivity(Settings.ACTION_ACCESSIBILITY_SETTINGS)`.
- Given the user re-enables Promptly in system settings and returns to the app,
- when the app detects the service is active via `Settings.Secure`,
- then the Settings screen opens normally.

### Scenario 2: User ignores the re-prompt
User sees the re-prompt but does not take action, then opens the app again later.

**Acceptance Criteria:**
- Given the re-prompt screen is displayed,
- when the user closes the app without tapping "Re-enable,"
- then no state change occurs.
- Given the user opens the app again,
- when the app launches,
- then the re-prompt screen is displayed again.
- Given the user later re-enables the service (directly in system settings, bypassing the app),
- when they open the app,
- then the Settings screen opens directly (no re-prompt).

## Implementation Notes

1. **Service loss detection** — On activity resume (MainActivity.onResume), query `Settings.Secure.getString(ENABLED_ACCESSIBILITY_SERVICES)` and check for Promptly's service ID. If missing and first-launch flag is set, route to re-prompt.
2. **Re-prompt UI** — Minimal screen: short explanation (one sentence) + "Re-enable" button. No skip option — service must be enabled for the app to function. Dismissal returns to launcher.
3. **Re-enablement detection** — On return from accessibility settings, re-run `Settings.Secure` query. If service ID is present, proceed to Settings screen.

## Links

- Epic index: [index.md](../index.md)
