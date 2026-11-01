# Contributing

This repository is a personal project adapted from the Apache-2.0 licensed
[Now in Android](https://github.com/android/nowinandroid) sample, so the licence and the original
copyright headers stay in place. Contributions are not solicited, but if you change the code:

1. Keep to the conventions in [AGENTS.md](AGENTS.md): module layout (`core:*`, `feature:*:api` /
   `feature:*:impl`), one use case per user action, `SaveResult` for user errors, product limits in
   `core:model/LumaLimits.kt`, and user-facing text in the owning module's `strings.xml`.
2. Do not add a hardware transport, real BLE code or a fake "connected" state. This build is a UI
   shell; see `docs/ARCHITECTURE.md` §5.
3. Run `./gradlew spotlessApply testDebugUnitTest :app:assembleDebug` before pushing.
4. If you change persisted configuration, add the Room migration and update
   `docs/ARCHITECTURE.md`.
