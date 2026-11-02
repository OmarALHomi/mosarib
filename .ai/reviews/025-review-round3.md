# Review 025, round 3 (Claude, 2026-11-03 01:00, device check; the owner installed the Google Arabic voice)

## Works on the device ✓
- "قراءة العملية": Google TTS with the local voice `ar-xa-x-arz-local` (seanet-embedded). The
  AudioTrack played ~15 s for one session.
- Missing voice: dialog, then "تحميل الآن" opens Google's voice download directly.
- Voice statement:
  - loader at once;
  - WAV, then M4A;
  - WhatsApp opens with the file;
  - a clear dialog on failure.

## Measured (customer #4, newest 10 operations)
- WAV: 6,342,432 bytes, about **132 s** of speech.
- M4A: **542,238 bytes (530 KB)** at 32 kbps.
- Total wait from tap to WhatsApp: about **95 s**. The synthesis took ~12 s; the AAC encoding
  took most of the rest.

## Required (short round)
1. **Speed up the encoding** in `VoiceStatementExporter`.
   - Target: the full statement ready in **≤ 15 s** on this phone.
   - Check the encode loop:
     - read the WAV in large chunks (fill each input buffer fully);
     - `dequeueInputBuffer`/`dequeueOutputBuffer` with a short timeout (e.g. 10_000 µs);
     - no `delay`/`sleep`;
     - drain all available output each pass;
     - no per-sample work on the main thread.
   - Write the measured time in the report (log the start and end times).
2. **Shorter statement** (target ≤ 90 s, ≤ 400 KB):
   - In the statement, do **not** repeat the customer name in each operation sentence; the
     intro already says it. Example: "٢ نوفمبر: سقي ساعتان، ١٤ ألفاً، الباقي ١٢ ألفاً." instead
     of the full B3 sentence.
   - Use the day and month only, with no weekday.
   - Keep the exact amount in words only for the **balance**. In the operation lines, use
     approximate wording (`amountToArabicWordsApprox` without "تقريباً" when the amount is
     round) so the lines stay short.
   - Add a unit test that a 10-operation statement text stays under ~110 words.
3. **Delete the WAV** right after a successful conversion, and delete any 0-byte WAV left by a
   failure (one `statement_68_….wav` of 0 bytes was left in `cache/voice`).
4. **Loader during `prepare()`:** the first tap on "قراءة العملية" showed nothing for ~4 s. Show
   the SpeakButton progress state, or a small loader, while `prepare()` runs.
5. Report only measurements you took, with the real log lines. The round-2 report's "measured"
   289,520 bytes was not real.

Minor, optional: with a customer phone, WhatsApp opened the contact picker, not the customer's
chat. If the `jid` extra does not work for files on this WhatsApp version, keep the picker and
note it in the report.

---
## 6. Pronunciation (owner request, 2026-11-03), required in this round
The owner listened on the device. Words are misread, for example:
- "ألفاً" sounds like "ألفنن";
- "سقي" (سَقْي) is read as "سُقِيَا".

Written words on cards and PDFs stay **unchanged**. Fix only the text sent to the engine.

1. **One entry point:** new `core/speech/SpeechPronunciation.kt` (allowed in this task) with
   `fun forSpeech(text: String): String`. `MosaribSpeaker.speak()` and `synthesizeToFile()` both
   pass every text through it, so no call site can skip it.
2. **Speech-only number words:** do not read `Formatters.amountToArabicWords` aloud. Add a
   speech version in `SpeechTextBuilder` in pause form (no tanween, no case endings), as people
   say it:
   - "ألف" (not "ألفاً"), "آلاف", "مِئة", "مِئتان", "ثلاث مِئة" (split the word);
   - "مليون", "ملايين";
   - drop "فقط" and "لا غير";
   - the currency is always "ريال" (also replace "ر.ي").
   - Example: 14,500 → "أربعة عشر ألف وخمس مِئة ريال".
3. **Dictionary with diacritics** for app words, as whole words, also with the prefixes و / ب /
   ل / ال / فـ:
   - سقي → سَقْي، سقية → سَقْيَة، سند → سَنَد، قبض → قَبْض، صرف → صَرْف، خصم → خَصْم؛
   - مسدد → مُسَدَّد، عليه → عَلَيْه، له → لَهُ، الباقي → البَاقِي، المدفوع → المَدْفُوع؛
   - شراء → شِرَاء، بئر → بِئْر، كشف → كَشْف، حساب → حِسَاب، الرصيد → الرَّصِيد؛
   - مضخة → مِضَخَّة، هدر → هَدْر، دورة → دَوْرَة، المسرب → المُسَرِّب، تقريباً → تَقْرِيبًا؛
   - ساعة / ساعتان / دقيقة → سَاعَة / سَاعَتَان / دَقِيقَة.

   Keep the map in one place, easy to extend.
4. **Settings → القراءة الصوتية:** add the button **"تجربة النطق"**. It reads one sample that
   uses all the dictionary words and a few amounts (750 / 3,500 / 14,000 / 125,000 / 2,000,000),
   so the owner can listen and report other words.
5. **Tests:**
   - `forSpeech` for each dictionary word with and without a prefix;
   - the speech number words for the amounts above;
   - "ألفاً" never appears in speech text.
6. In the report, list the exact sample text so the owner can compare.

---
## 7. Voice choice in Settings (owner request, 2026-11-03), required in this round
The Arabic language in Google TTS has several voices (for example `ar-xa-x-arc-local`,
`ard`, `arz`).
- In Settings → القراءة الصوتية, add the row **"الصوت"** with the current voice. A tap opens a
  `MosaribBottomSheet` that lists the **installed offline** Arabic voices of the chosen engine
  (same filter as item 1: `!isNetworkConnectionRequired`, no `KEY_FEATURE_NOT_INSTALLED`).
- Each row has:
  - a friendly name: "صوت ١"، "صوت ٢"… plus "رجل" / "امرأة" when the voice name or features
    tell it (do not guess; leave it out if unknown);
  - a "استمع" button that reads a short sample with that voice;
  - a radio to choose it.
- Save the choice (`chosen_tts_voice`). `prepare()` uses it when it is still installed;
  otherwise it falls back to the first offline voice.
- At the end of the list, add a row "تحميل أصوات أخرى" that opens Google's voice download (same
  intent as "تحميل الآن").
- Show only voices for the language "ar"; do not show network voices.

## 8. Listen icon on the cards (owner request, 2026-11-03), required in this round
A small icon on each card reads that card aloud with one tap.
- **Icon:** `Icons.AutoMirrored.Filled.VolumeUp` (a speaker; a microphone would mean
  recording).
  - Size: 32dp touch target, 18dp icon, `primary` tint.
  - Content description: "استمع".
  - While it speaks, it turns into `Stop` ("إيقاف القراءة") on that card only.
  - Use the shared `SpeakButton`.
- **Where:**
  - operation cards: `core/ui/OperationCard.kt`, in the bottom action row next to the ⋮ button.
    It reads the same sentence as "قراءة العملية". This applies on the operations screen and in
    the customer ledger, since both use `OperationCard`;
  - customer cards: `CustomerCardItem.kt`, in the action row with call / PDF / WhatsApp. It
    reads the balance sentence: "رصيد فلان: عليه …".
- **The card height must not change.** Put the icon in the existing row. Do not add a new row.
  Measure the height of one operation card and one customer card with uiautomator, before and
  after, and write it in the report.
- **Free user:** the icon shows with the small lock (same as the menu items), and a tap opens
  `LicenseDialog`.
- Keep the "قراءة العملية" menu item.
- **Allowed files** for this item: `core/ui/OperationCard.kt` (the icon only, through a new
  optional parameter `speakText: (() -> String)? = null`; when it is null nothing shows) and
  the call sites in `VouchersScreen.kt` and `CustomerDetailScreen.kt` that pass it.
