# Luma architecture

Luma is an app-shell phase product built on the Now in Android foundation. This document covers the
data flow, the parts inherited from the base app, every significant deviation from it, and the
boundary between what this build does and what the eventual controller will do.

## 1. What is inherited

| Area | Inherited from the base app | Notes |
| --- | --- | --- |
| Build | Gradle convention plugins in `build-logic/convention`, version catalog, `spotless` + ktlint | Flavour and Firebase plugins removed (see §3) |
| Modules | `core:*` / `feature:*:api` + `feature:*:impl` split, `VersionCatalog`-driven deps | Feature modules trimmed to the four tabs |
| DI | Hilt, `SingletonComponent` modules per core module, `@HiltViewModel` | Same shape, Luma bindings |
| Navigation | `core:navigation` `NavigationState` + `Navigator` + Navigation 3 `NavDisplay`, one back stack per tab | Unchanged except for the leave guard (§4.4) |
| Data | Repository interfaces in `core:data` implemented over Room + DataStore, model ↔ entity mappers | No remote source, so no `offline-first` sync wrapper |
| UI | `core:designsystem` component layer over Material 3, `core:ui` shared composables | Components renamed `Luma*`, new colour/brightness/preview components added |
| Testing | JUnit4 + Truth + Turbine + `core:testing` fakes and assertions, `MainDispatcherRule` | Domain/model tests only |

The news, sync, network, analytics, notification, search, bookmarks, interests, topic and benchmark
modules were deleted together with their dependencies (`core:network`, `core:analytics`,
`core:notifications`, `core:sync`, `sync:work`, `app-nia-catalog`, `benchmarks`, `screenshot-testing`).
Luma has no remote content, no accounts and no background work, so keeping them would have meant
carrying dead code and dead permissions.

## 2. Data flow

```
Compose screen  ──events──▶  ViewModel (UiState StateFlow)  ──calls──▶  Use case (validation, rules)
      ▲                                                                          │
      └──────────────────── Flow<model> ◀── Repository (core:data) ◀─────────────┘
                                                    │
                                       Room (config)  +  DataStore (Wi-Fi secret)
```

* **Model** (`core:model`, pure JVM): `LumaBoard`, `LumaStrip`, `LumaMode`, `ColorRamp`, `ColorStop`,
  `ModeAnimation`, `LumaSchedule`, `Weekday`, `WifiCredentialsSummary`, `LumaLimits`,
  `LumaValidation`, `BrightnessCurve`, `ColorMath`, schedule occurrence maths.
* **Domain** (`core:domain`): one use case per user action (`SaveModeUseCase`, `SaveStripUseCase`,
  `SaveBoardUseCase`, `SaveScheduleUseCase`, `SaveWifiCredentialsUseCase`,
  `SetStripBrightnessUseCase`, `AssignModeToStripUseCase`, delete use cases, getters,
  `GetNextScheduledChangeUseCase`, `CreateSampleSetupUseCase`). Saves return
  `SaveResult<T>` = `Saved` | `Invalid(Set<LumaError>)`, so validation problems are data, not
  exceptions, and the UI can render them inline.
* **Data** (`core:data`): `BoardRepository`, `StripRepository`, `ModeRepository`,
  `ScheduleRepository`, `WifiRepository`, plus `CredentialCipher`, `IdGenerator` and
  `LumaConfigTransfer`.
* **Persistence**: Room database `luma-database` (version 1, `exportSchema = true`, schemas in
  `core/database/schemas`); Preferences DataStore `luma_settings` for the Wi-Fi secret only.

### Database

| Table | Purpose |
| --- | --- |
| `boards` | One row per controller board: name, pixel limit, buffer size, pixel layout |
| `strips` | One row per strip, FK to `boards` with cascade delete; GPIO, active LED count, brightness, **mode id reference** |
| `modes` | One row per reusable mode: name + animation parameters + timestamps |
| `mode_stops` | Ramp stops, PK `(modeId, stopId)`, FK to `modes` with cascade delete |
| `schedules` | Strip reference, mode reference, time, repeat-day bit mask, transition length, enabled flag |

Modes own their ramp rows. A strip and a schedule hold a `modeId` string, never a copy, which is what
makes a mode reusable and editable in one place. Deleting a mode clears the reference on every strip
and deletes the schedules that pointed at it, in a transaction; deleting a strip deletes its
schedules; deleting a board deletes its strips and their schedules.

### DataStore

`luma_settings` holds `wifi_ssid` and `wifi_password_cipher`. The cipher text is produced by
`AndroidKeystoreCredentialCipher`: AES/GCM/NoPadding, 256-bit key, key material generated on first
use inside the Android Keystore and non-exportable, stored as `base64(iv):base64(ciphertext)`. The
data source never receives a plaintext password, the ViewModel does not keep one after saving, and
the UI only ever reads back `ssid` + `hasStoredPassword`.

## 3. Deviations from the base app

These are deliberate; each has a reason tied to Luma's scope.

1. **No product flavours.** The base app's `demo`/`prod` flavours existed to switch between a mock
   and a real network backend and to gate Firebase. Luma has no backend, so the flavour plugins,
   `LumaFlavor`, the flavour-aware badging inputs and flavour-specific source sets were removed and
   the app builds as plain `debug`/`release`. Consequence: CI and scripts call `testDebugUnitTest`,
   `:app:assemble` and `connectedDebugAndroidTest` instead of flavour-qualified task names, and all
   sources live in `src/main`.
2. **One fixed dark theme.** `LumaTheme` always applies the same `darkColorScheme` built from the
   almost-black purple palette (`LumaInk`, `LumaSurface`, `LumaPurple`, `LumaPink`). There is no
   light scheme, no dynamic colour and therefore no `disableDynamicTheming` switch; the design-system
   previews no longer parameterise light/dark. The platform window theme (`Theme.Luma`) is a dark,
   chrome-less theme so the splash and first frame match Compose.
3. **Preferences DataStore instead of a protobuf DataStore.** The base app used a generated
   `UserPreferences` proto. Luma stores two strings, so protobuf codegen (plugin + `protoc`
   download) would have been overhead; `androidx.datastore:datastore-preferences` is used directly.
   Everything structured lives in Room, which is what keeps the configuration serialisable.
4. **Keystore-backed AES/GCM instead of Jetpack Security.** `androidx.security:security-crypto` is
   deprecated and `EncryptedSharedPreferences` is not suitable for a value that must later be handed
   to the controller. The small `CredentialCipher` interface (Keystore implementation + a
   deterministic fake in `core:testing`) keeps it testable and swappable.
5. **No offline-first sync wrapper.** With no remote source, `Repository` methods read the local
   source directly instead of going through `NetworkBoundResource`-style reconciliation with expiry.
   The repository interfaces are still the only thing the domain layer sees, so a sync layer can be
   added behind them later without touching the UI.
6. **Modes are normalised, not a JSON blob.** The base app stored related resources in separate
   tables with `@Relation`; Luma does the same for ramp stops (`PopulatedModeEntity`). This keeps the
   ramp editable stop-by-stop and query-friendly, and the JSON transfer shape is built separately in
   `core/data/transfer` so the on-disk schema and the wire schema can evolve independently.
7. **A leave guard was added to `core:ui`.** The base app had no dirty-editor concept. `LeaveGuard`
   (a `CompositionLocal` provided by the app shell) lets a dirty editor intercept *every* exit: the
   shell routes system back (`NavDisplay.onBack`), the toolbar back arrow and tab switches through
   `requestLeave`, and the editor answers with Discard / Save / Save as new mode / Keep editing.
8. **No WorkManager, analytics, notifications or baseline profiles.** Nothing in this phase runs in
   the background, so the corresponding modules, permissions, plugin aliases and CI steps were
   removed. `ProfileInstaller` (for the APK's baseline profile installation on device) is kept, and
   `JankStats` frame logging from the base app is retained in `MainActivity`.
9. **Screens own their own state shape.** Features expose `UiState` sealed interfaces with
   `Loading`/`Content` (and `NotFound` where a saved id could disappear: strip detail, mode editor).
   `StateFlow` + `collectAsStateWithLifecycle` is used exactly as in the base app.

## 4. UI behaviour worth knowing

### 4.1 Colour ramps

`ColorRamp` is an ordered set of 1–8 `ColorStop(position 0..100, color ARGB Long)`.

* Positions are clamped to `0..100`; a stop cannot be dragged within `LumaLimits.MIN_STOP_GAP`
  (0.5) of a neighbour, so the ramp never collapses and never produces a division by zero.
* Both endpoints are movable. `colorAt(x)` extends the first stop's colour to `x = 0` and the last
  stop's colour to `x = 100`, so no section of the ramp is ever undefined, and the strip is never
  asked for a colour it does not have.
* Interpolation is OKLab: sRGB → linear → LMS → OKLab, mix, then back. Perceptual interpolation is
  what stops the muddy mid-tones a naive RGB lerp produces on purple/pink ramps.
* `colors(n)` returns `n` samples for a preview; the editor drags handles with a single gesture
  detector (tap selects the nearest handle, drag moves it) so a tap is never swallowed by a drag.
* Brightness is deliberately *not* applied to any preview: `LumaColorRampBar`, `LumaRampEditor` and
  `LumaModePreview` always draw full-strength colour. The strip's brightness is shown as a value and
  a slider, so a dimmed strip does not distort the colours the user is choosing.

### 4.2 Brightness

Stored per strip as `0f..0.8f` (`LumaLimits.MAX_BRIGHTNESS`). `BrightnessCurve` maps the slider
position `0..1` through `(e^(k·t) − 1) / (e^k − 1)` with `k = 6`, which gives fine control at the
low end where a linear slider would jump; `sliderForBrightness` is the inverse (used to seed the
slider from a stored value). `SetStripBrightnessUseCase` re-clamps on write, so no code path can
persist more than 80%. The 8.6 fixed-point form the controller expects is
`BrightnessCurve.toFixedPoint8_6`.

### 4.3 Drafts and the leave guard

The mode editor keeps a single `ModeEditorDraft` that is serialised into the `SavedStateHandle` on
every edit. `isDirty` is computed against the loaded mode, so a no-op edit does not block leaving.
Recomposition, rotation and process death restore the draft; nothing else in the app writes to the
mode until Save. Saving as a new mode generates a fresh id (`IdGenerator`), leaves the original mode
untouched, and adopts the new id in the draft so further edits update the copy.

Board, strip and schedule editors use the same pattern with their own drafts; their dialogs pass
`showSaveAsNew = false` because "save as new" only makes sense for reusable modes.

### 4.4 Navigation

`core:navigation` keeps one back stack per top-level tab (`Home`, `Modes`, `Schedules`, `Settings`).
Nested destinations are `@Serializable` nav keys in each feature's `api` module, registered by the
feature's entry provider (`homeEntry`, `modesEntry`, `schedulesEntry`, `settingsEntry`). The shell
renders a single top app bar whose title comes from the current key, so a nested screen still gets a
back arrow and a correct title without owning its own scaffold. `NavDisplay` is given the list-detail
scene strategy from `material3-adaptive-navigation3`, so a wide window shows list and detail side by
side, and the same code path is what the phone layout uses.

## 5. The hardware boundary

This build **stores** configuration; it does not control hardware.

* No Bluetooth permissions, no scanning, no GATT, no Wi-Fi provisioning flow, no schedule execution,
  no firmware.
* Modes' animation parameters (`AnimationKind`, `speed`, `spread`, `reverse`) are stored parameters.
  The editor preview animates them in-app as an approximation of what the controller would do; it is
  a UI preview, not the strip.
* The next scheduled change on Home is computed locally from the stored schedules; nothing fires.
* `LumaConfigTransfer` (export → `LumaConfigPayload` → JSON → decode → `validate()`) is implemented
  and unit-tested because it is the contract the firmware must accept. `schemaVersion` is 1 and any
  breaking change must bump it.

Constraints the model is shaped around, and where they show up:

| Constraint | Where it is visible in this code |
| --- | --- |
| 1,000-pixel UI limit | `LumaLimits.MAX_LEDS`, board `pixelLimit`, strip `activeLeds` validation |
| 80% brightness ceiling, exponential low end | `LumaLimits.MAX_BRIGHTNESS`, `BrightnessCurve` |
| 8-bit + 6 fractional bits, FastLED dithering off | `LumaLimits.OUTPUT_FRACTIONAL_BITS`, `toFixedPoint8_6` |
| Same `.ino` on both boards | Board/strip split with per-board pixel limit and per-strip GPIO |
| Portable struct/JSON persistence on the board | `LumaConfigPayload` + `validate()` |
| Slow, drifting, non-simultaneous LED updates | Modes express motion as ramp + speed/spread rather than frame timing; nothing promises frame-accurate sync |

## 6. Testing

* `core:model` — ramp maths (endpoint extension, ordering, gap clamping, add/remove, OKLab
  round-trip), brightness curve monotonicity and 80% ceiling, schedule occurrence (day roll-over,
  bit masks, disabled schedules), validation rules.
* `core:domain` — save-mode, save-strip, set-brightness, save-Wi-Fi credentials,
  next-scheduled-change, using the fakes in `core:testing`.
* `core:navigation` — `Navigator` tab/back-stack behaviour.
* No instrumentation test and no screenshot test exists in this phase.

**None of these tests has been executed.** See `BUILD.md` for what that means and what to check
first.

## 7. Where the hardware layer will attach

1. `WifiRepository.encryptedPassword()` and `LumaConfigTransfer.exportConfig()` are the inputs a BLE
   provisioning step needs; nothing else in the app has to change to send them.
2. `ScheduleRepository` exposes stored schedules; time sync and on-board scheduling would be a new
   `core:sync`-style module writing the same payload.
3. Board/strip entities already carry GPIO, active LED count, pixel limit, buffer size and layout, so
   adding a field is a Room migration plus a mapper change, not a model redesign.
4. `SaveResult`/`LumaError` are the place to surface controller-side rejections once a transport
   exists; the UI already renders per-field errors from that type.
