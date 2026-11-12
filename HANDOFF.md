# RescueGuide handoff — 0.29.0-dev

## Scope and safeguards
- Persian Android app shell; current intended scope is Basic Life Support and choking only. Advanced life support remains deferred.
- Target references for future clinical authoring: AHA/AAP 2025. The actual clinical pathways have not yet been implemented or approved.
- Iran only, emergency number 115, opened with ACTION_DIAL. No CALL_PHONE permission.
- Adult, child and infant group selection; newborn resuscitation at birth is out of scope.
- Clinical content must remain disabled until the user explicitly reviews and approves it as the resuscitation specialist.
- Do not add medical illustrations until shown to and approved by the user.

## Current code
- Compose application shell, selection flow and review-only gate.
- Pure Kotlin domain policies, age routing and session reducer.
- Persian voice command allowlist/parser; commands are display-only.
- Android one-shot speech recognition adapter and Persian TextToSpeech adapter used only for opt-in hardware tests.
- Emergency dialer shortcut via ACTION_DIAL.
- Unit tests for domain/routing/session/parser.
- `.github/workflows/android.yml` uses Java 17, Gradle 8.13 and Android SDK 36; it runs unit tests, Android lint and debug APK assembly, then uploads reports and APK artifact.
- AGP 8.11.1 paired with Kotlin 2.2.20 and Gradle 8.13 to respect published Kotlin/AGP compatibility limits.

## Verification truth
- Source structure and Android manifest have been inspected. The pure Kotlin domain/parser sources compiled with `kotlinc`; 17 smoke assertions passed. Workflow YAML parsed successfully.
- Android SDK/Gradle are not available in the authoring container; Android/Compose build, Android lint, APK generation, emulator and device tests have not run here. GitHub Actions must provide the first real Android build result.
- Persian speech recognition and TTS need a real handset check.

## Phone-only upload workflow
Download ZIP, extract it on Android, then upload the **contents** of the inner `RescueGuide` folder to the repository root. Keep `.github/workflows/android.yml` at the exact path. GitHub does not unpack an uploaded ZIP automatically. See `docs/UPLOAD_TO_GITHUB_FA.md`.

## Not implemented yet
- Adult/child/infant BLS algorithms and adult/child/infant choking pathways.
- Medical approval record, clinical voice prompts, and algorithm execution.
- Approved illustration assets, recorded Persian audio, final metronome, and clinical voice-control orchestration.

Do not describe this source bundle as a finished clinical app or claim a verified APK until the workflow passes and the specialist approval/device-validation gates are complete.
