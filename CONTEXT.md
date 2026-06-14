# Promptly — Domain Glossary

## Configuration model

| Setting | Values |
|---|---|
| Enabled | On / Off |
| Cooldown type | Daily reset \| N-hour interval |
| Reset time (if daily) | HH:mm (default 00:00) |
| Interval hours (if interval) | 1–24 |
| Target app | Picked from installed apps, or None |

## Core concepts

**Intervention**
: A full-screen, non-dismissable activity that appears after the user unlocks the phone. It can only be exited via a dedicated Dismiss button, or by being Redirected to the target app. Intended to interrupt phone usage and steer the user toward a productive activity.

**Dismissing**
: The act of pressing the close button on the Intervention to remove it and return to normal phone usage.

**Redirecting**
: Launching the configured target app from the Intervention. When the user is Redirected, the Intervention closes after handoff.

**Settings**
: The configuration UI of the Promptly app, where the user sets up the configuration and toggles enablement.

**Cooldown**
: The behaviour where, once an Intervention has been triggered (shown on unlock), it will not appear again on subsequent unlocks until a reset event occurs.

## Technical constraints

- **Unlock detection**: Accessibility Service registers a `BroadcastReceiver` for `ACTION_USER_PRESENT`.
- **Non‑closable Intervention**: Lock Task Mode (`startLockTask` / `stopLockTask`).
- **Target app redirect**: `startActivity()` for the target package, preceded by `stopLockTask()` and followed by `finish()`.
- **Target app picker**: Custom list built from `PackageManager` query for `CATEGORY_LAUNCHER` apps.
- **Uninstalled target app**: Intervention shows with Dismiss button and a message; target setting is cleared.
- **Configuration storage**: `SharedPreferences`.
- **Onboarding**: Dedicated screen guiding the user to enable the Accessibility Service, then Settings.
