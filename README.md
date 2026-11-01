# Luma LED Controller

Luma is an Android app for configuring addressable LED strips: several controller boards, the
strips wired to them, a library of reusable colour modes, and schedules that switch a strip to a
mode at a set time.

This build is the **app shell phase**: a complete, locally persisted configuration UI with no
hardware behind it yet.

> **Status: source only.** This tree has not been compiled or run. The environment it was adapted
> in had no JDK, Android SDK, Gradle cache or network access, so no Gradle task, unit test or
> instrumentation test was executed. Nothing here is claimed to work on a device. See
> [BUILD.md](BUILD.md) for how to build it and where to look first.

## What the app does

**Home** — every configured strip with the mode it is showing, a full-strength colour preview of
that mode, a brightness slider and the next scheduled change that will affect it.

**Modes** — a library of named, reusable modes. Each mode owns a colour ramp (one solid colour, or a
gradient of up to eight stops) and a set of animation parameters. Editing a mode never edits a
strip: strips and schedules *reference* modes by id.

**Schedules** — a strip, a mode, a time, the days it repeats and a transition length. Enabling and
disabling is a switch on the card.

**Settings** — boards and strips (a board drives one or more strips; each strip belongs to exactly
one board and has its own GPIO pin and active LED count), plus the Wi-Fi credentials the controller
will eventually be provisioned with.

## Product rules enforced in code

| Rule | Where |
| --- | --- |
| 1,000 pixel UI limit for boards and strips | `core/model/…/LumaLimits.kt` |
| Brightness ceiling of 80%, exponential slider at the low end | `core/model/…/Brightness.kt`, `core/designsystem/…/BrightnessSlider.kt` |
| Brightness never dims a mode or ramp preview | `core/designsystem/…/ColorRamp.kt`, `ModePreview.kt` |
| Ramps: 1–8 stops, ordered by 0–100 position, movable endpoints, nearest colour extended to the edges, OKLab interpolation | `core/model/…/ColorRamp.kt`, `ColorMath.kt` |
| Mode edits are drafts until Save; leaving a dirty editor asks Discard / Save / Save as new mode | `feature/modes/impl`, `core/ui/…/LeaveGuard.kt` |
| Wi-Fi password encrypted at rest, never shown again | `core/data/…/AndroidKeystoreCredentialCipher.kt` |
| A strip references a mode; deleting a mode clears the reference and deletes its schedules | `core/data/…/DefaultModeRepository.kt` |

## Not implemented (deliberately)

There is no Bluetooth, no board communication, no schedule execution and no firmware. Schedules and
modes are stored, not run. The UI says so in the banners on Home and Schedules and in Settings, so
nothing can be mistaken for a working controller link. The transfer contract the firmware will need
is implemented and tested as data (`core/data/transfer`), but nothing sends it anywhere.

## Modules

| Module | Responsibility |
| --- | --- |
| `app` | Single activity, four tabs, the one top app bar, the navigation graph |
| `feature/*/api` | Navigation keys and screen titles |
| `feature/*/impl` | Screens, ViewModels and navigation entries for Home, Modes, Schedules, Settings |
| `core/model` | Pure Kotlin models and maths: ramps, brightness, schedules, validation, limits |
| `core/domain` | Use cases that apply the product rules and return `SaveResult` |
| `core/data` | Repositories over Room and DataStore, the credential cipher, the transfer payload |
| `core/database` | Room entities, DAOs and the database |
| `core/datastore` | Preferences DataStore (Wi-Fi credentials only) |
| `core/designsystem` | Fixed dark purple theme, icons, every component the screens use |
| `core/ui` | Leave guard, shared snackbar host, screen enter animation, date formatting |
| `core/navigation` | One back stack per tab (inherited from the base app) |
| `core/common`, `core/testing` | Dispatchers and `Result`; test doubles and assertions |

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the data flow and the list of changes made to
the base app.

## Building

See **[BUILD.md](BUILD.md)**. Short version: open the project in Android Studio with JDK 17, let it
sync, run the `app` configuration, or `./gradlew :app:assembleDebug`.

Unit tests: `./gradlew testDebugUnitTest`.

## Base app and attribution

This repository started as the [Now in Android](https://github.com/android/nowinandroid) sample and
was adapted in place for Luma. The Gradle convention plugins, module layout, Hilt DI, Navigation 3
state holder, Room/DataStore data layer, design-system structure and testing conventions are
inherited; the news, sync, analytics, notification, network and screenshot-testing modules were
removed because Luma has no remote content.

Original code is licensed under Apache 2.0 — see [LICENSE](LICENSE).
