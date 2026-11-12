# RescueGuide

Persian Android prototype for voice-assisted basic life support (BLS) and choking guidance.

## Source snapshot

`0.29.0-dev` — complete snapshot of the source currently implemented in this repository. It is **not** a finished or clinically approved app, and it is not an APK.

## Included implementation

- Persian Android/Jetpack Compose home screen and navigation shell.
- BLS and choking module selection, adult/child/infant manual routing, and rescuer-type selection.
- Review gate; clinical guidance remains disabled.
- Strict Persian voice-command phrase parser for confirm, decline, repeat and back; parsed commands are not connected to clinical actions.
- Opt-in Android speech recognition and Persian Text-to-Speech hardware tests. Microphone access is requested only after the user starts the test.
- Iran emergency dialer shortcut using `ACTION_DIAL` for 115.
- Unit tests for domain policies, patient routing, navigation and voice parsing.
- GitHub Actions workflow to install Java 17, Gradle 8.13 and Android SDK 36, then run unit tests, lint and debug APK assembly.

## Build through GitHub Actions

The GitHub workflow is located at `.github/workflows/android.yml`. The extracted project files must be placed in the **root** of the repository with their directory structure preserved. Uploading the ZIP file alone does not extract it on GitHub. Then open **Actions** and inspect the `RescueGuide Android CI` run. A debug APK is published as an artifact only after all build/test/lint steps succeed. See `docs/UPLOAD_TO_GITHUB_FA.md` and `docs/PHONE_ONLY_GITHUB_BUILD_FA.md`.

## Important limits

- Clinical BLS and choking pathways are not implemented or active. No algorithm or medical illustration is enabled.
- Clinical content must be checked against AHA/AAP 2025 and approved explicitly by the user's specialist before activation or publication.
- Advanced life support is outside this version's scope.
- Persian recognition and TTS depend on services and voice data installed on the target handset; physical-device testing is required.
- No Android build, APK, emulator test or device test has yet been verified from this workspace. GitHub Actions is the next verification step.
- The app is not a medical device and does not replace emergency dispatch or hands-on training.

## Import workflow maintenance

The ZIP import workflow rebases its extracted-source commit onto the latest `main` branch before pushing, and leaves workflow definitions under separate maintenance.
