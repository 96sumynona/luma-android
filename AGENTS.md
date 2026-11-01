# Luma LED Controller — working notes

Luma is an Android app for configuring addressable LED strips, built on the Now in Android base. This
file is the short version of the conventions; `docs/ARCHITECTURE.md` explains the design and the
deviations from the base app.

## Scope discipline

**This build is a UI shell.** There is no Bluetooth, no board control, no schedule execution and no
firmware. Never claim, label or imply otherwise in code, strings, comments or documentation, and do
not "helpfully" add a fake transport. Modes and schedules are stored data; the previews animate in
the app only.

The configuration stays local. The Wi-Fi password is encrypted at rest (Android Keystore, AES/GCM)
and is never read back into the UI.

## Build and test

The project targets JDK 17 and Android SDK 36; use the wrapper.

- Build: `./gradlew :app:assembleDebug`
- Format: `./gradlew spotlessApply` (ktlint, config in `tools/luma-codestyle.xml`)
- Unit tests: `./gradlew testDebugUnitTest`
- Single test: `./gradlew testDebugUnitTest --tests "com.luma.ledcontroller.core.model.ColorRampTest"`
- Lint: `./gradlew :app:lintDebug :lint:lint`

There are **no product flavours**: no `DemoDebug`, no `testDemoDebug`. Variants are `debug` and
`release` only, and all sources live in `src/main`.

## Architecture rules

- One module per concern, mirroring the base app: `core:model` (pure JVM), `core:domain` (use cases),
  `core:data` (repositories over Room/DataStore), `core:database`, `core:datastore`,
  `core:designsystem`, `core:ui`, `core:navigation`, `core:common`, `core:testing`, and
  `feature:{home,modes,schedules,settings}:{api,impl}`.
- `feature/*/api` holds only nav keys and screen titles; screens, ViewModels and entry providers live
  in `feature/*/impl`.
- Screens never touch a repository. They call a use case or read `ViewModel.uiState`.
- Saves go through a use case returning `SaveResult<T>` (`Saved` | `Invalid(Set<LumaError>)`). Do not
  throw for user error; render `LumaError` as inline text.
- Product limits live in `core:model/LumaLimits.kt` and validation in `LumaValidation.kt`. Never
  duplicate a limit in a screen: the 1,000-pixel cap, the 80% brightness ceiling and the 8 ramp stops
  are single-sourced there.
- A strip references a mode by id. Never copy ramp or animation data onto a strip, a schedule or a
  board.

## UI rules

- Use the `core:designsystem` components (`LumaCard`, `LumaSectionCard`, `LumaSettingRow`,
  `LumaDropdown`, `LumaTextField`, `LumaNumberField`, `LumaButton`, `LumaTextButton`,
  `LumaConfirmDialog`, `LumaEmptyState`, `LumaInfoBanner`, …) rather than raw Material 3 widgets; a
  lint check flags some raw usages.
- Colours come from `LumaTheme`. The app is one fixed dark purple scheme; do not add light/dark or
  dynamic-colour branches.
- Every user-facing string goes in the owning module's `res/values/strings.xml`, with the module name
  as the prefix (`feature_modes_impl_…`, `core_ui_…`). Resource prefixes are enforced by the
  convention plugin.
- Editors with unsaved work register through `LeaveGuardEffect`: system back, toolbar back and tab
  switching all route through `LeaveGuard.requestLeave`. Any new exit path must do the same.
- A new screen gets its title in `app/.../ui/NestedDestinationTitle.kt` if it is not a tab.

## Navigation

Nav keys are `@Serializable` objects/data classes in the feature's `api` module; register the
destination in the feature's entry provider (`homeEntry`, `modesEntry`, `schedulesEntry`,
`settingsEntry`) and call it from `LumaApp`. Each tab keeps its own back stack (`core:navigation`).

## Persistence

Room changes need a migration and a schema export under `core/database/schemas`. If a change alters
the transfer contract, bump `LumaConfigPayload.CURRENT_SCHEMA_VERSION` and update
`docs/ARCHITECTURE.md` §5.

## Honesty in docs

`README.md`, `BUILD.md` and `docs/ARCHITECTURE.md` state that nothing has been compiled or run in the
environment the code was written in. Keep those statements accurate: only claim something is tested
if a build or test run actually happened.
