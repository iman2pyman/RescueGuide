# Source snapshot verification — 0.29.0-dev

This document distinguishes checks performed in the authoring environment from checks that still require GitHub Actions or a real Android phone.

## Passed locally

- Kotlin domain and voice-parser production files compiled with `kotlinc`.
- 17 source-level smoke assertions passed, covering emergency number validation, age routing, specialist approval gate, exact voice command parsing, and navigation state transitions.
- Android manifest parsed as XML.
- Manifest assertions confirmed microphone permission is present and `CALL_PHONE` / `INTERNET` permissions are absent.
- GitHub Actions workflow parsed as YAML and its required trigger/SDK setup paths were inspected.
- The source archive was inspected after creation to confirm it includes the Android source, tests, Gradle configuration, docs, and hidden `.github/workflows/android.yml` path.

## Not run / not claimed

- Android Gradle Plugin / Compose compilation.
- Gradle JUnit test task, Android lint, APK assembly, emulator tests, or physical-device tests.
- Runtime microphone, speech recognition, Persian TTS or installation behavior on the user's handset.
- Clinical validation or specialist approval of BLS / choking algorithms.

The first real Android build result must come from the included GitHub Actions workflow. A successful debug APK build would still not mean the medical content is ready: clinical guidance remains unimplemented and disabled in this prototype.
