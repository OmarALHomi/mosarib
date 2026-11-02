# Review 025, round 1 (Claude, 2026-11-02, code review; the phone was not connected)

Build and tests pass (`EXIT=0`). What is in place:
- `MosaribSpeaker`: the four states, `synthesizeToFile`, and AAC M4A at 32 kbps.
- The voice statement keeps the newest 10 operations.
- Subscriber check uses `LicenseManager.isActivated` only.
- Nothing is written to Firebase.
- The missing-voice dialog.

The fixes below are small but required.

## Required
1. **The lock stays after activation:** `isSubscribed = remember(context) { … }` in `SpeakButton`
   and `CustomerCardItem` is never read again. After the user activates from `LicenseDialog`, the
   lock badge stays until the screen is reopened. In `onActivated`, read
   `LicenseManager.isActivated(context)` again and update the value (`mutableStateOf`).
2. **The lock badge is an emoji ("🔒"):** use a small icon (`Icons.Default.Lock`, 14dp,
   `textMuted`) in `trailing` instead of the emoji. Do the same in every menu item that uses it.
3. **`isSpeaking` turns true while the voice statement file is generated:** `onStart` fires for
   `synthesizeToFile` as well, so every `SpeakButton` turns into "إيقاف" while the statement is
   being prepared. Change `_isSpeaking` only for speech utterances, not for those that start
   with `synth_`.
4. **Re-initialising after a failure:** if init fails, `tts` keeps the old instance. Call
   `shutdown()` and set it to null before creating a new one.
5. **The report is missing the file size** of a 10-operation statement. It is required in "Done
   when". Measure it on the device when the phone is connected (`adb shell ls -l
   /data/data/<pkg>/cache/voice`, or log the size), and write it in the report.

## Claude will check on the device after the fix
- "قراءة العملية" reads a session correctly.
- The voice statement opens WhatsApp with an M4A file.
- The missing-voice dialog.
- The lock for a non-subscriber.

---
## Device check (Claude, 2026-11-02 23:21, Samsung R5CR812MCHE, the task/025 build)
- "قراءة العملية" appears in the card menu (subscribed phone, no lock). ✓
- The phone's default engine is **Samsung TTS** (`com.samsung.SMT`). Logcat:
  `ara-null-null is LANG_NOT_SUPPORTED`. The app showed "الصوت العربي غير محمّل". ✓
- "تحميل الآن" opened a chooser between Samsung TTS and Google "Speech Recognition and
  Synthesis". ✓
- **But Google TTS (`com.google.android.tts`) is already installed on the phone**, and it supports
  Arabic.

### New required item 6: choose an engine that supports Arabic
Many Samsung phones have Google TTS installed, but it is not the default engine. In
`MosaribSpeaker.prepare()`:
1. Try the default engine. If Arabic is `Ready`, use it.
2. Otherwise, go through `tts.engines` (`TextToSpeech.EngineInfo`). Prefer
   `com.google.android.tts` first, then the others. Create
   `TextToSpeech(appContext, listener, enginePackage)` and test `isLanguageAvailable(Locale("ar"))`.
   Use the first engine that returns Ready. Shut down the instances you do not use.
3. Save the chosen engine in the preferences, so the search does not run every time.
4. Only if no engine supports Arabic: show the dialog as today. "تحميل الآن" should go straight to
   Google's engine (`setPackage("com.google.android.tts")` on `ACTION_INSTALL_TTS_DATA`) if it is
   installed, without a chooser.
5. The status line in Settings shows the engine in use ("المحرك: Google").

The real speech test and the voice statement test (WhatsApp, file size) wait for this fix.
