# Review 030, round 1 (Claude, 2026-11-03 05:20, code review + device check with the owner)

Build and tests pass (20 tests, 65 phrases). The parsers are well structured. The voice flow did not
work on the device.

## Required
1. **The home tile "إدخال صوتي" does nothing** (owner). In `HomeScreen.kt`, the tile sets
   `showVoiceHintSheet` / `showVoiceMissingDialog`, but no composable reads these flags. The hint
   sheet and the missing dialog are never drawn, and `homeVoiceLauncher` is never launched.
   - Render the hint sheet (its "ابدأ" button launches the recognizer) and the missing-recognizer
     dialog.
   - Check every other entry point the same way: form header mics, field mics, the customers
     search mic.
2. **Google's red error screen: no offline Arabic pack.** Logcat (the session form mic):
   `TranscriptionState{language=Arabic (Yemen)…}` →
   `RecognitionClient: #onError agsa_transcription_LANGUAGE_PACK_ERROR code 12`. The intent asks
   for `ar-YE` with `EXTRA_PREFER_OFFLINE = true`. The phone has no offline Arabic recognition
   pack, so Google fails instead of using the network. (The TTS voice pack the owner installed is
   a different pack.)
   - **API 33+** (the phone is API 33): use `SpeechRecognizer.checkRecognitionSupport(intent, …)`.
     Choose the best Arabic locale that is installed offline, in this order: `ar-YE`, `ar-SA`,
     `ar-EG`, `ar`. If one exists, use it with `EXTRA_PREFER_OFFLINE = true`.
   - **If none is installed (owner decision 2026-11-03): always tell the user first.** Show our
     `MosaribDialog` (tone Info). Never open Google's red error screen.
     - Title "حزمة التعرف على العربية غير محمّلة".
     - Message: "لاستخدام الإدخال الصوتي حمّل حزمة اللغة العربية مرة واحدة (تعمل بعدها بدون
       إنترنت)."
     - Primary button **"تحميل الآن"** starts the download **at once**:
       `SpeechRecognizer.triggerModelDownload(intent)` for the chosen Arabic locale on API 33+
       (this phone is API 33). Show "جاري تحميل حزمة العربية…" and, when the download is done
       (use the download listener on API 34+, or re-check `checkRecognitionSupport` on resume or
       after a short wait), say "تم التحميل، اضغط الميكرفون مرة أخرى". Below API 33, or if the
       call fails, open Google's speech-recognition language settings directly. If no direct
       screen exists, open the Speech Services app details.
     - Secondary text button, shown only when the internet is available: "متابعة عبر الإنترنت
       هذه المرة". It starts recognition without `EXTRA_PREFER_OFFLINE`, so the user is not
       blocked.
     - "لاحقاً".
     - Offline with no pack: same dialog. "تحميل الآن" says "يحتاج التحميل إلى إنترنت" when
       there is no connection.
   - **API < 33:** try offline. If the result is cancelled with no results, or the network is
     available and `RESULT_*` reports an error, retry once online. Map the result codes
     (`RESULT_NO_MATCH`, `RESULT_NETWORK_ERROR`, `RESULT_AUDIO_ERROR`, …) to short Arabic
     messages.
   - Put this in `VoiceRecognitionHelper`, so every entry point uses the same logic.

### 2b. Arabic may not be available offline at all (owner checked, 2026-11-03)
The owner looked at the offline languages list of Google's speech services on this phone, and
Arabic is not there. Decide by what the phone reports, not by guessing. With
`checkRecognitionSupport` (API 33+), read `installedOnDeviceLanguages`,
`pendingOnDeviceLanguages`, `supportedOnDeviceLanguages` and `onlineLanguages`:

| State | Behaviour |
|---|---|
| An Arabic locale is installed on the device | offline (`EXTRA_PREFER_OFFLINE = true`) |
| Arabic is in `supportedOnDeviceLanguages` but not installed | the dialog with "تحميل الآن" (as above) |
| Arabic is pending | "جاري تحميل حزمة العربية…" |
| Arabic is **not supported on the device** (this phone) | start **online recognition directly**, with no download dialog. One time only, a small note: "الإدخال الصوتي يعمل عبر الإنترنت على جوالك." |
| No internet, and Arabic is not installed | `MosaribDialog`: "الإدخال الصوتي يحتاج إنترنت على هذا الجوال. يمكنك الإدخال يدوياً." |

Below API 33, assume online, with the same offline message when there is no network.
Report what `checkRecognitionSupport` returned on this phone (the exact lists).

3. **Recognizer-style test input:** the recognizer often returns digits ("15 ألف", "15000",
   "٢ ساعة ونص", "10000"). Add tests for each of the five example phrases written as the
   recognizer would return them (digits in both digit sets, and no punctuation).
4. **The report must contain only real observations.** It calls the phone a "Galaxy S20+" (it is
   an A12, `R5CR812MCHE`), and its "recognizer candidate" column repeats the spoken text. After
   the fix, Claude and the owner will test by voice. Write only what you saw.

## Claude will check on the device after the fix (the owner speaks)
- The five example phrases from the home tile and from each form's mic.
- An ambiguous name.
- Dictation in the amount and notes fields.
- The customers search mic.
