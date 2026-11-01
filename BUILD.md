# Building and running Luma

**Read this first:** this source tree was written without a JDK, Android SDK, Gradle cache or
network access, so **it has never been compiled**. Expect the first build to surface ordinary
compile errors. Nothing below claims the app was tested.

## Requirements

| Tool | Version |
| --- | --- |
| Android Studio | Latest stable (the project pins AGP 9.3.2, Kotlin 2.3.0 and KSP 2.3.4, which need a recent Studio) |
| JDK | **17** (the settings script fails fast on anything older) |
| Android SDK | Platform **36** (`compileSdk`/`targetSdk` are 36), build-tools matching AGP |
| Gradle | Use the wrapper: `./gradlew` |
| Device / emulator | Android 7.0 (API 24) or newer |

If Studio offers to upgrade AGP or Gradle, decline unless you know the pinned versions are
incompatible with your SDK: the catalog and `build-logic` are matched to the versions above.

## Build from Android Studio

1. Clone the repository and check out the branch you were given.
2. **File → Open** the project root (the folder containing `settings.gradle.kts`).
3. Point Studio at a JDK 17 (**Settings → Build, Execution, Deployment → Build Tools → Gradle →
   Gradle JDK → 17**) and let the first sync run. It downloads AGP, Kotlin, Compose, Hilt, Room and
   Navigation 3 artifacts; the first sync is slow.
4. Accept the SDK 36 platform install if prompted.
5. Choose the `app` run configuration and press **Run**.

## Build from the command line

```bash
# Debug APK -> app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:assembleDebug

# Unit tests, including the pure-JVM model/common modules and custom lint rules
./gradlew testDebugUnitTest :core:model:test :core:common:test :lint:test

# The remaining CI checks
./gradlew :build-logic:convention:check spotlessCheck :app:lintDebug :lint:lint
```

Install it on a connected device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Release builds are signed with the debug key so anyone can install them:

```bash
./gradlew :app:assembleRelease
```

## GitHub Actions CI

The **Android CI** workflow (`.github/workflows/Build.yaml`) runs automatically on every push and
pull request. You can also open **Actions → Android CI → Run workflow** to start it manually. It uses
JDK 17 and SDK 36, then performs these steps in order:

1. Compile `:app:assembleDebug`.
2. Run the Android debug unit tests, the pure-JVM model/common tests and the custom lint-rule tests.
3. Check the convention plugins, formatting and Android lint.

Open a workflow run to read any compiler or test errors. Its **Artifacts** section contains:

- **`luma-debug-apk`** — the installable debug APK once compilation succeeds (download and unzip the
  artifact first). It is still uploaded if a later test or lint step fails.
- **`unit-test-results`** and **`lint-reports`** — uploaded when available, including on failures.

There are currently no instrumented `androidTest` sources, so CI deliberately does not spend time
starting an empty emulator. The five first-run flows and cosmetic checks below still need a manual
on-device pass; emulator jobs can be added when real UI tests exist.

## First-run experience

The app starts empty on purpose: it does not invent hardware for you. To see the whole UI:

1. **Settings → Add board** (name, pixel limit, buffer size, pixel layout), save.
2. In the same board card, **Add strip** (GPIO pin, active LED count), save.
3. **Modes → New mode**: drag the ramp handles, pick stop colours, choose an animation, save.
4. **Schedules → New schedule**: pick the strip and the mode, choose a time and repeat days.
5. **Home** now shows the strip with its mode preview, brightness and next scheduled change.

## Where to look first if the build fails

The riskiest spots, in order, because they use comparatively new APIs:

1. **Navigation 3** — `NavDisplay(entries = …, sceneStrategies = …, onBack = …)` and
   `rememberListDetailSceneStrategy` in `app/src/main/kotlin/…/ui/LumaApp.kt`, plus
   `rememberDecoratedNavEntries` in `core/navigation`. If the Navigation 3 API differs in `1.2.0`,
   that is the only place that needs changing.
2. **Material 3 time picker** — `TimePicker` / `rememberTimePickerState` inside `AlertDialog` in
   `feature/schedules/impl/…/ScheduleEditorScreen.kt` (the `ExperimentalMaterial3Api` opt-in is
   already declared).
3. **Room codegen** — `core/database` (entities, `PopulatedModeEntity`, DAOs) with KSP. Room writes
   the schema directory on first build.
4. **Compose / adaptive versions** — the Compose BOM and the Navigation Suite / adaptive artifacts
   come from `gradle/libs.versions.toml`; they were carried over from the base app, not re-verified
   here.
5. **Gradle convention plugins** — `build-logic/convention` is inherited and trimmed (the flavour and
   Firebase plugins were removed). A stale plugin alias fails with the alias name in the message;
   delete that alias from the module's `plugins {}` block.

## What has been verified

- Static review only: imports, resource names, string format arguments, icon references and model
  API usage were cross-checked by script.
- Unit tests exist for `core/model` (ramp maths, brightness curve, schedule occurrence, validation)
  and `core/domain` (save-mode, save-strip, set-brightness, save-Wi-Fi, next-scheduled-change), but
  they have **not** been run.
- No instrumented test was written or run.

## Repository conventions inherited from Now in Android

- Gradle convention plugins in `build-logic/convention` own the Android/Kotlin/Compose/Room/Hilt
  configuration; module build files stay short.
- `./gradlew spotlessApply` formats with ktlint using `tools/luma-codestyle.xml`.
- `./gradlew graphUpdate` regenerates the per-module dependency graphs in each `README.md` (they are
  deliberately not committed after the module graph was re-cut).
