# RescueGuide handoff — 0.36.0-dev

## Product scope
- Persian Android app for basic life support (BLS) in adults, children and infants.
- Advanced resuscitation is deferred to a later version.
- Current release excludes choking management.
- Iran only; emergency number 115 opens through ACTION_DIAL; no direct-call permission.
- Newborn resuscitation at birth is out of scope.

## Implemented
- Compose home screen styled to match the user-approved RescueGuide reference image.
- Navigation: home → age/rescuer/breathing selection → review → active pathway.
- Separate adult, child and infant BLS pathways.
- Voice/manual age selection, lay/trained rescuer selection, trained rescuer count, and rescue-breath choice.
- Step repeat/back controls, optional voice commands, ROSC report and CPR restart route.
- Adjustable CPR metronome and Iran 115 dialer.
- 34 user-provided Persian MP3 files bundled and matched to exact text SHA-256 identifiers.
- No choking audio or choking pathway is included in this release.

## Verification limits
- Audio filename-prefix mapping to full SHA-256 IDs is unique for all 34 supplied files.
- MP3 decode/metadata verification is performed with ffprobe and `tools/verify_audio_assets.py`.
- Full Android Gradle build, APK assembly, instrumentation tests, and physical-device verification must still be run in Android Studio or GitHub Actions.
- Medical content and instructional illustrations require final specialist review before publication.


## 0.35.0-dev work in progress
The 0.35.0-dev source draft includes the approved dark navy / cyan / blue-violet theme direction, home shortcuts across non-home pages, expanded BLS study guide, pediatric rescuer-count routing, and an automatic CPR counter. Compression counter targets 30 for adults and single-rescuer pediatric CPR, and 15 for two-trained-rescuer pediatric CPR. The audible ventilation signals are prompts only; they do not represent delivered breaths or verify chest rise. Existing MP3 voice files were not modified. Run Android CI and device tests before distributing an APK.


## 0.36.0-dev implementation notes
- Approved navy / cyan / blue-violet theme is retained across the home, rescuer selection, active CPR, study guide, and post-ROSC screens.
- CPR cadence counter runs at approximately 110/min (within 100–120/min), with 30 compressions for adults and single-rescuer pediatric CPR, and 15 for two-trained-rescuer pediatric CPR.
- When rescue breaths are enabled, two soft airflow-style sound cues mark the two breath windows. These are timing prompts only; they do not deliver ventilation or confirm chest rise.
- Existing 34 Persian MP3 files remain unchanged.
- AED explanatory text now explicitly introduces the attenuator-pad instruction with the requested phrase.
- Android build and physical-device verification are still required.
