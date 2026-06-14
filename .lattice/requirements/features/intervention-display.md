---
feature: Intervention Display
epic: Intervention Engine
status: draft
priority: P0
depends_on:
  - Onboarding & Permissions (must complete before intervention activates)
personas:
  - Distracted Professional — wants a friction-based nudge away from mindless scrolling
  - Focus Seeker — blocks phone use during deep-work windows
  - Parent or Guardian — sets boundaries for their own or a child's phone use
source_docs:
  - CONTEXT.md (domain glossary + technical constraints)
  - User requirements list
---

# Intervention Display

## Problem Statement

A user picks up their phone intending to do something productive but gets pulled into mindless scrolling, social media, or games. The phone offers no resistance at the moment of unlock — the user's willpower is at its weakest right when apps are one tap away. The intervention creates a deliberate pause at the unlock moment, requiring an intentional action (Dismiss or Redirect) before the phone can be used normally.

## User / Personas

- **Distracted Professional**: Knows they waste time on phone; wants a friction-based nudge toward their actual intention.
- **Focus Seeker**: Doing deliberate deep work or study; blocks phone access during certain windows.
- **Parent or Guardian**: Setting boundaries for their own or a child's phone use.

## Scope

**In scope:**
- Full-screen activity triggered immediately on phone unlock (ACTION_USER_PRESENT)
- Single Dismiss button as the only exit from the intervention
- Lock Task Mode to prevent Back, Home, and Recents from closing the intervention
- Service degradation when accessibility service is disabled or Lock Task Mode fails
- Incoming call interruption handling (intervention resumes after call)

**Out of scope:**
- Multiple intervention screen types or variations
- Visual customization of the intervention (colors, branding, layout)
- Animations, transitions, or sound effects
- Per-app intervention rules (different behavior per app category)
- Notification-based intervention (only unlock-based)

## Boundary Conditions

- Screen-off timeout re-lock within the same session counts as a new unlock opportunity
- Incoming call during intervention: system call UI takes over, intervention resumes after call ends
- Device reboot: accessibility service restarts automatically (Android default), intervention triggers on next unlock
- Accessibility Service disabled: no intervention, feature degrades silently
- Lock Task Mode unavailable: intervention still shows but user can bypass via Home/Recents (no kiosk pinning)

## Assumptions

- Accessibility Service is enabled by the user during onboarding (Feature: Onboarding & Permissions)
- Lock Task Mode (`startLockTask`/`stopLockTask`) is available for kiosk-style activity pinning
- Android's `ACTION_USER_PRESENT` broadcast is reliably delivered to the registered receiver
- The user has granted the Accessibility Service permission for the app (required for unlock detection)
- Screen-off timeout followed by unlock is considered an "unlock" even if no deep lock occurred

## Scenarios

### Scenario 1: User unlocks phone, intervention appears
Promptly is enabled and the current time is within the active schedule. The user unlocks their phone.

**Acceptance Criteria:**
- Given Promptly is enabled and the current time is within the active schedule,
- when the user unlocks the phone (ACTION_USER_PRESENT),
- then a full-screen activity opens on top of the launcher or home screen.
- Given the intervention activity is displayed,
- when the user attempts to navigate away via the Back button,
- then the system ignores the Back press and the activity remains visible.
- Given the intervention activity is displayed,
- when the user presses the Home or Recents button,
- then Lock Task Mode prevents the system from responding and the activity remains visible.

### Scenario 2: Accessibility service is not running at unlock
The service was disabled or has not started since the last reboot.

**Acceptance Criteria:**
- Given the Promptly accessibility service is not running,
- when the user unlocks the phone,
- then no intervention appears and normal phone use proceeds.
- Given the intervention would have triggered but the service is not running,
- when the user opens the Promptly app,
- then the Onboarding screen shows with guidance to re-enable the service.
- Given the service is not running or Lock Task Mode is unavailable,
- when an unlock event occurs,
- then the app does not crash or log errors — it degrades silently.
- Given the service becomes available again,
- when the next unlock event occurs,
- then the intervention resumes normal operation.

### Scenario 3: User taps Dismiss, intervention closes
The intervention is displayed, and the user proactively dismisses it.

**Acceptance Criteria:**
- Given the intervention activity is displayed,
- when the user taps the Dismiss button,
- then `stopLockTask()` is called and the activity finishes.
- Given the intervention has been dismissed,
- when the user views the home screen or recent apps,
- then no trace of the intervention activity remains.
- Given the intervention was dismissed and the cooldown is active,
- when the user locks and re-unlocks the phone within the same cooldown period,
- then the intervention does not reappear. *(Cooldown behavior owned by Cooldown Schedule feature.)*

### Scenario 4: Incoming call interrupts intervention
A phone call arrives while the intervention is displayed.

**Acceptance Criteria:**
- Given the intervention activity is displayed,
- when an incoming call is received,
- then the system call UI takes over (standard Android behavior).
- Given the call was answered,
- when the call ends,
- then the intervention activity resumes.
- Given the intervention resumes after a call,
- when it regains focus,
- then Lock Task Mode is re-established.
- Given the call was rejected or missed,
- when the call UI dismisses,
- then the intervention activity remains displayed.

## Implementation Notes

1. **Unlock detection & intervention launch** — Register ACTION_USER_PRESENT broadcast receiver via accessibility service; launch full-screen activity; apply Lock Task Mode.
2. **Dismiss flow** — Dismiss button triggers stopLockTask() followed by finish() on the intervention activity.
3. **Service health & fallback** — Graceful no-op when service is disabled; Lock Task Mode failure fallback; accessibility service status check before triggering.
4. **System interruption resilience** — Handle incoming call and system dialog interruptions; activity lifecycle management to resume Lock Task Mode after interruption.

## Open Questions

- [x] What fallback behavior should apply when Lock Task Mode fails? **Resolved**: Show intervention anyway (user can bypass via Home/Recents).
- [x] Should screen-off-and-on within a short threshold (e.g., 30s) count as a new unlock? **Deferred**: Owned by Cooldown Schedule feature.
- [x] Should the intervention show on first boot before onboarding is complete? **Blocked by Onboarding & Permissions**: Intervention only activates after onboarding completes.

## Links

- Epic index: [index.md](../index.md)
