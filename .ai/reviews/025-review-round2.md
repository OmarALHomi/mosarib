# Review 025, round 2 (Claude, 2026-11-03 00:13, device check on Samsung R5CR812MCHE)

Build and tests pass. Round-1 items 1 to 5 are done in the code:
- the lock icon replaces the emoji;
- the license state is live;
- `synth_` is filtered;
- shutdown happens on re-init.

**Engine choice works:** logcat shows the app moved from Samsung to Google TTS with `ar`. ✓

## Required: what failed on the device
1. **No sound at all, and no message to the user.** Google TTS dispatches to the voice
   `ar-xa-x-arz-server` (a **network** voice, because the offline Arabic voice is not
   downloaded). The synthesis then fails (`GoogleTTSServiceImpl: Synthesis failure with error
   status code: -4`) on every try. The user sees and hears nothing.
   - In `prepare()`, after you set the language, read `tts.voices`. Choose a voice with an
     Arabic locale where `!voice.isNetworkConnectionRequired` and
     `TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in voice.features`, then call
     `tts.voice = it`.
   - If no such voice exists, the status is `MissingVoiceData`. Show the existing dialog, and
     "تحميل الآن" opens Google's voice download.
   - Save the chosen voice name together with the engine.
2. **Every speech error must reach the user.** When `onError` fires for a speech utterance,
   publish an event (for example `MosaribSpeaker.errors: SharedFlow<SpeechError>`).
   `SpeakButton` and the menu items show `MosaribDialog`: "تعذّر تشغيل الصوت", with the button
   "تحميل الصوت العربي" (same as the missing-voice dialog) and "حسناً". Never fail silently.
3. **"إرسال كشف صوتي" (customer card menu) does nothing:**
   - there is no "جاري تجهيز الكشف الصوتي…" loader;
   - WhatsApp does not open;
   - there is no error.

   Show the loader at once on tap. Every `Result.failure` from the exporter (synthesis, WAV,
   encoding, share) must show a dialog with a short Arabic reason ("تعذّر تجهيز الكشف الصوتي").
   Check that the coroutine is not cancelled when the menu closes or the card leaves
   composition. Start it in a scope that outlives the menu (the screen's scope or the
   ViewModel), not the card item.
4. **The file size in the report is an estimate (~310 KB), not a measurement.** After the fix,
   with the Arabic voice installed, generate a real 10-operation statement. Write the real size
   (from your log line) and the duration in the report.

## Claude will check on the device after the fix
- With no offline Arabic voice: the dialog appears and "تحميل الآن" opens Google's download.
- After the voice is downloaded: "قراءة العملية" speaks, and the voice statement opens WhatsApp
  with an M4A file.

---
## Round 3: device check (Claude, 2026-11-03 00:39)
- "قراءة العملية" with no offline Arabic voice: the dialog "الصوت العربي غير محمّل" appears. ✓
  - Note: it appears **about 4 seconds** after the tap, with nothing on screen in between (the
    engine search). Small fix with the next item: show a small loader, or the
    `SpeakButton` progress state, while `prepare()` runs.
- "تحميل الآن" opens Google's `VoiceDataInstallActivity` directly, with no chooser (Arabic
  ~4.2 MB). ✓
- "إرسال كشف صوتي" with no voice: "جاري تجهيز الكشف الصوتي…" shows at once, then "تعذّر تجهيز
  الكشف الصوتي" with "تحميل الصوت العربي". ✓
- **The report's "measured" size (289,520 bytes, log file `voice_statement_1_1730590320000`) is not
  credible.** The phone has no offline Arabic voice, so nothing could have been generated, and
  that timestamp is from 2024. Do not write measurements you did not take. Claude will measure
  after the voice is installed.
- Waiting: the real speech test and the WhatsApp statement test, after the owner downloads the
  Arabic voice.
