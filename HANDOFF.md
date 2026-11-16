# RescueGuide handoff — 0.37.0-dev

## Product scope
- Persian Android app for basic life support (BLS) in adults, children and infants.
- Advanced resuscitation is deferred to a later version; choking pathway remains disabled.
- Iran only; emergency number 115 opens through ACTION_DIAL.

## 0.37.0-dev visual implementation
- Reworked the home screen around the user-approved three-phone reference: navy medical-tech backdrop, dotted texture, ECG accent, centered heart/ECG identity, voice waveform panel, three separate age cards, red 115 emergency action and settings/guide controls.
- Reworked rescuer selection into large cyan/blue/violet gradient cards with a persistent home shortcut.
- Reworked active BLS, study guide and post-ROSC pages to use the same navy/cyan medical visual system, outlined cards, visible home navigation, and high-contrast action controls.
- Replaced launcher vector with the two-tone red/cyan heart and ECG mark from the approved visual direction.

## Preserved functionality and assets
- Separate adult, child and infant BLS pathways.
- Trained-rescuer count selection for pediatric pathways; one rescuer uses 30:2 and two trained rescuers use 15:2. Adult path remains 30:2.
- Step repeat/back controls, CPR cadence counter (~110/min), two ventilation timing cues when breaths are enabled, and post-ROSC six-step page.
- 34 bundled Persian MP3 assets and their SHA-256 text mapping are preserved unchanged; verify with `python tools/verify_audio_assets.py`.
- Offline instructional images are retained. Emergency 115 opens the phone dialer; it does not place a call automatically.
- Theme selection remains stored in app preferences.

## Verification and release gates
- This source revision has not yet been built by Android Gradle/CI or tested on a physical phone. Do not treat it as a verified APK until CI and device installation pass.
- MP3 verification is separate from listening tests; each voice prompt must be auditioned on-device.
- Medical content and instructional illustrations require final specialist review before publication.
