# RescueGuide handoff — 0.30.0-dev (local worktree)

## Product scope
- Persian Android app for BLS and choking in adults, children and infants.
- Advanced resuscitation is explicitly deferred.
- Iran only; emergency number 115 opens through `ACTION_DIAL`; no direct-call permission.
- Newborn resuscitation at birth is out of scope.
- The user has explicitly approved the agreed Persian clinical content for review in the app. Medical illustrations were not added because the user asked to inspect/approve them separately.

## Implemented in the local source
- Compose navigation: home → module/age/rescuer selection → review → active pathway.
- Data-driven pathway content for every module × age × rescuer combination.
- Optional spoken age selection for explicit category words and numeric ages; ages 1–17 are not automatically classified from age alone because puberty signs determine adult vs pediatric routing; explicit category wording or manual selection is required.
- Step-by-step controls, previous/next, repeat audio, manual/voice ROSC report, restart CPR route, choking-unconscious transition to BLS, and emergency dialer shortcut.
- 48 bundled Persian MP3 files: 47 unique pathway instruction phrases plus opening greeting; no device Persian TTS or network required for playback.
- Optional one-shot voice commands: confirm/yes, repeat, back, and user-reported return of normal breathing (ROSC). Touch navigation remains available and no time-critical action is blocked by voice confirmation.
- Adjustable 100–120/min CPR metronome (default 110/min), independent from speech audio.
- Branded medical color palette and custom heart/ECG adaptive launcher icon.
- Tests added for pathway coverage, rescue-breath ratios, ROSC routing and selected 2025 safety rules.

## Clinical source checks
- AHA 2025 adult BLS and FBAO guidance.
- AHA/AAP 2025 pediatric BLS and infant/child FBAO guidance.
- Infant CPR compression method: two-thumb encircling hands or heel of one hand; no two-finger infant CPR method.
- Infant severe choking: five back blows alternating with five chest thrusts using heel of one hand; no abdominal thrusts.
- Adult/child severe choking: five back blows alternating with five abdominal thrusts; late pregnancy or inability to encircle abdomen uses chest thrusts.

## Local verification
- `kotlinc` compilation of domain and clinical catalog succeeds.
- A local smoke test exercises navigation, all 12 module/age/rescuer combinations, infant compression and infant choking safety rules.
- Android XML resources parse successfully.
- MP3 inventory is checked against SHA-256 names derived from every clinical instruction.
- Full Android/Compose Gradle build, JUnit execution, lint, APK assembly, and physical-device testing have not yet been run in this authoring environment (Android SDK/Gradle are unavailable here).
- eSpeak Persian voice is synthetic/robotic; audio intelligibility and pronunciation must be reviewed on an actual handset before release.

## Build workflow
The existing `.github/workflows/android.yml` remains unchanged. Once local source is finished, use the existing GitHub Actions workflow for the first full Android build and tests. Do not claim a verified APK until that run succeeds and the APK is installed/tested on a real phone.
