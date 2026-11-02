status: draft

<!-- Owner approved on 2026-11-02: both phases, subscribers only, and the user must be told
clearly when the Arabic voice data is missing. Becomes ready after 013 and 024 are merged, because
it touches files they change (menus in VouchersScreen, CustomerDetailScreen, CustomerCardItem,
AmountWithWords). -->
# Task 025: Read amounts and operations aloud (Arabic TTS), and voice statements via WhatsApp

## Why
Many customers cannot read digits. The app already builds the exact Arabic amount in words
(`Formatters.amountToArabicWords`). Android's built-in `TextToSpeech` can read it aloud, offline
once the Arabic voice is installed. It needs no new library and no permission.

## A. Speech engine wrapper: new `core/speech/MosaribSpeaker.kt`
- An app-scoped singleton around `android.speech.tts.TextToSpeech`. Initialise it lazily on first
  use; shut it down with the process.
- `suspend fun prepare(context): SpeechStatus` returns one of:
  - `Ready`;
  - `MissingVoiceData` (`isLanguageAvailable(Locale("ar"))` returns `LANG_MISSING_DATA`);
  - `LanguageNotSupported` (`LANG_NOT_SUPPORTED`);
  - `NoEngine` (init failed or no engine installed).
- `fun speak(text: String, utteranceId: String)`: queue mode `QUEUE_FLUSH`, Arabic locale. The
  speech rate comes from settings (B4). It also provides `stop()` and an `isSpeaking` StateFlow
  for the button state.
- `suspend fun synthesizeToFile(text: String, file: File): Result<File>` using
  `TextToSpeech.synthesizeToFile`. Wait for `UtteranceProgressListener.onDone` / `onError`.
- Stop speaking when the screen leaves composition, using a `DisposableEffect` in the button
  composable.

## B. Phase 1: "استمع" buttons inside the app
1. **Shared button** `SpeakButton(text: () -> String, modifier)`: a 40dp `IconButton`,
   `Icons.AutoMirrored.Filled.VolumeUp`. While it speaks, the icon becomes `Stop` and a tap stops
   it. Content description "استمع" / "إيقاف القراءة".
2. **Where:**
   - the `AmountWithWords` tooltip (task 024): a "استمع" button under the words reads the exact
     words;
   - every operation card menu (`OperationCard` kebab in `VouchersScreen.kt` and in the customer
     ledger): a new item **"قراءة العملية"** with the volume icon, before the destructive items;
   - customer detail balance card: a small `SpeakButton` next to the balance.
3. **Sentences** (pure builder `core/speech/SpeechTextBuilder.kt`, unit tested):
   - session: "سقي لـ{name}، {duration words}، المبلغ {exact words}، المدفوع {words}، الباقي
     {words}." Omit "المدفوع/الباقي" when fully paid, and say "مسدد بالكامل" instead;
   - receipt: "سند قبض من {name}، {amount words}، بتاريخ {day month}.";
   - expense: "سند صرف لـ{name or category}، {amount words}، بتاريخ {…}.";
   - discount: "خصم لـ{name}، {amount words}.";
   - purchase: "شراء ساعات من {owner}، {duration words}، {amount words}.";
   - balance: "رصيد {name}: عليه {words}" / "له {words}" / "الحساب مسدد".

   Durations are said as words ("ساعتان وثلاثون دقيقة"), dates as "الثاني من نوفمبر".
4. **Settings → section "القراءة الصوتية":**
   - speech rate slider (0.7–1.3, default 0.9);
   - a "تجربة الصوت" button that reads a sample;
   - a status line from `prepare()`.

## C. Phase 2: "إرسال كشف صوتي" via WhatsApp
1. New item **"إرسال كشف صوتي"** next to "إرسال كشف" and "كشف حساب PDF", in the customer card
   menu and in the customer detail header actions.
2. Content (builder, unit tested), kept short (target under 90 seconds):
   - "كشف حساب {name} من {distributor name} بتاريخ {today}."
   - the current balance (عليه / له / مسدد) in words;
   - the **last 10 operations**, newest first, one sentence each (B3) with the day;
   - "انتهى الكشف."
3. Generate it with `synthesizeToFile` into `cacheDir/voice/`, which gives WAV. **Convert it to
   AAC in an M4A container** (`MediaCodec` AAC-LC, mono, 32 kbps + `MediaMuxer` MPEG_4; minSdk 24
   supports both) to keep the file small. Delete the WAV after conversion. While it is
   generated, show `MosaribLoadingIndicator` with "جاري تجهيز الكشف الصوتي…".
4. Share it through the existing `FileProvider` with an `ACTION_SEND` intent, `audio/mp4`, package
   `com.whatsapp` (or `com.whatsapp.w4b`), falling back to the normal chooser. If the customer has
   a phone number, use the same WhatsApp target logic as the existing "إرسال كشف".
5. Clean up `cacheDir/voice` files older than 1 day on app start.

## D. Missing voice data: the user must know (owner requirement)
On any speak or voice-statement action, when `prepare()` is not `Ready`:
- **MissingVoiceData / LanguageNotSupported:** show `MosaribDialog` (tone Info):
  - title "الصوت العربي غير محمّل";
  - message "لتفعيل القراءة الصوتية، حمّل حزمة اللغة العربية من إعدادات تحويل النص إلى كلام في
    جوالك (مرة واحدة، تعمل بعدها بدون إنترنت).";
  - buttons "تحميل الآن", which starts `TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA` and falls
    back to `Settings` → `com.android.settings.TTS_SETTINGS`, and "لاحقاً".
- **NoEngine:** the same dialog with the message "لا يوجد محرك نطق على جوالك. ثبّت «خدمات
  التعرف على الكلام من Google» من المتجر ثم أعد المحاولة.", and a button that opens the Play
  Store page of `com.google.android.tts`.
- The Settings status line (B4) shows the same state, with the same button.
- Re-check the state in `onResume`, so the buttons work right after the user returns from
  installing.

## E. Subscribers only (owner requirement)
- Every speak action and the voice statement need `LicenseManager.isActivated(context)`. Do not
  add any new license logic; only call this existing function.
- For a free user, the buttons and menu items are **visible**, with a small lock badge. A tap
  opens the existing `LicenseDialog` (not mandatory), with the reason line "القراءة الصوتية
  والكشف الصوتي متاحان للمشتركين."
- Do not write anything to Firebase. Do not change `LicenseManager`.

## Allowed files
- new `core/speech/MosaribSpeaker.kt`, `core/speech/SpeechTextBuilder.kt`,
  `core/speech/VoiceStatementExporter.kt` (WAV → M4A + share), `core/speech/SpeakButton.kt`
- `core/ui/AmountWithWords.kt` (add the button)
- `features/vouchers/VouchersScreen.kt` (card menu item only)
- `features/customers/CustomerDetailScreen.kt` (ledger card menu item, balance button, header
  action), `features/customers/CustomerCardItem.kt` (menu item)
- `features/settings/SettingsGeneralSections.kt` / `SettingsScreen.kt` (new section)
- `AndroidManifest.xml` only for a `<queries>` entry for the TTS intent and WhatsApp (if needed)
- tests under `app/src/test/java/com/example/`

## Tests
- `SpeechTextBuilder`: each operation type; fully paid vs partial; balance for عليه / له / مسدد;
  durations and dates in words; a statement with more than 10 operations keeps the newest 10.

## Done when
- Build and tests pass.
- On the device (with Arabic voice installed): "قراءة العملية" reads a session correctly; the
  voice statement opens WhatsApp with an M4A attachment. The report states the file size for a
  10-operation statement.
- With the voice missing (or simulated): the dialog appears and "تحميل الآن" opens the system
  screen.
- As a free user: the lock badge shows and a tap opens the license dialog.

---
## Review notes (written by Claude)
_(empty)_
