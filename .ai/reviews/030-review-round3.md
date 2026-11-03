# Review 030, round 3 (Claude + owner, 2026-11-03 13:35, combined build 029+030+031)

Build and tests pass (50 voice tests). The recognition support logic and the debug log work.

## 1. A recognised phrase ends with nothing on screen (blocker)
Owner said: "ثلاث ساعات ونص مع احمد علي من 10 الف ومسلم 20 الف". Logcat:
```
VoiceInput: candidates: [ثلاث ساعات ونص مع احمد علي من 10,000 ومسلم 20,000, … مسلم 20,000, 3 ساعات ونص مع احمد علي من 10,000 ومسلم 20,000]
```
The recognition was perfect, but no form, chooser or message appeared. In `HomeScreen.kt`,
`openFormForVoiceCommand` has `null -> {}`, and the "ask" cases (a duration with a well owner, an
amount + name with no direction, a name not found) have no visible outcome.
- **Every path must end in something the user sees:**
  - a prefilled form;
  - the customer chooser;
  - the type chooser ("سقي على حسابه / شراء ساعات منه", "قبض منه / صرف له / خصم له");
  - or a message with the "سمعت: …" text and the examples.
- **A name that is not found** ("احمد علي" when there is no such customer):
  - open the inferred form;
  - put the spoken name in the customer field;
  - show the chooser with the closest customers + **"إضافة عميل جديد باسم «احمد علي»"** (the
    existing quick-add).
- Add a debug log of the parsed `VoiceCommand` and the chosen branch.
- Add a test that runs this exact candidate list through `parseFirstValidCommand` and asserts
  SESSION, 210 min, rate 10000, paid 20000, customerQuery "احمد علي".
- Check every entry point (home tile, form header mics) for the same silent paths.

## 2. Push-to-talk (owner request): pauses must not end the recording
Google's recognition screen closes at the first short pause. Replace the `RecognizerIntent`
activity with our own in-app recorder based on `SpeechRecognizer`
(`createSpeechRecognizer`, `startListening`, `stopListening`), using the same locale and
offline/online logic as today.
- **New `VoiceRecordSheet`** (shared, used by every voice entry point):
  - a large round mic button (88dp, `primary`);
  - **hold to talk:** recording starts on press and stops on release (`stopListening`). The
    result is processed on release.
  - **tap mode** for people who cannot hold: a single tap starts, a second tap stops. Show the
    hint "اضغط مطولاً وتكلّم، ثم أفلت" with a small "أو اضغط للبدء ثم اضغط للإنهاء".
  - **live text:** show partial results (`EXTRA_PARTIAL_RESULTS = true`, `onPartialResults`) as
    the user speaks;
  - a sound level ring from `onRmsChanged`;
  - "إلغاء" closes without processing.
- **Long pauses:** set `EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS` and
  `EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS` to about 5000.
  - Engines may ignore these. If the engine still ends the session while the button is held
    (`onResults` / `onEndOfSpeech` / `onError` with `ERROR_SPEECH_TIMEOUT` /
    `ERROR_NO_MATCH`), **restart listening at once** and append the new text to the text
    already heard, until the user releases.
  - The final text is all the parts joined.
- **Permission:** add `RECORD_AUDIO` to the manifest and ask for it on first use, with a short
  Arabic reason before the system prompt: "يحتاج الإدخال الصوتي إلى الميكرفون لسماع كلامك. لا
  يُحفظ أي تسجيل." If it is denied, explain and offer to open the app settings.
- No audio is stored. Nothing leaves the phone except through Google's recognizer.
- The field dictation mics and the customers search mic use the same sheet, in a compact
  version.

After this round, the owner repeats the free-form phrases with the new button.
