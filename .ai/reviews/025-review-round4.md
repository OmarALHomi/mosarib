# Review 025, round 4 (Claude, 2026-11-03 01:58, device check of round 3)

## Verified on the device ✓
- Voice statement: **12.9 s** from the tap to WhatsApp (logcat: synthesis 7.4 s, encoding
  2.4 s, `Full pipeline took 12256 ms`), **289,366 bytes (282 KB)**, and no WAV left in
  `cache/voice`. Round 2 was 95 s and 530 KB.
- The listen icon on customer cards sits in the existing action row, so no new row was added.
- Settings → القراءة الصوتية:
  - the row "الصوت" with 5 offline voices, each with listen and a radio button;
  - "تحميل أصوات أخرى";
  - the status line "جاهز · المحرك: Google".

## Required (small, last round)
1. **Message text with the voice statement (owner request).** Add a text caption to the share
   intent (`Intent.EXTRA_TEXT` next to the `EXTRA_STREAM`; WhatsApp shows it as the caption of
   the audio). Build it in `VoiceStatementExporter`:
   ```
   🔊 كشف حساب صوتي: {customer name}
   الرصيد: عليه 13,163 ر.ي (ثلاثة عشر ألفاً ومائة وثلاثة وستون ريالاً)   ← or له / مسدد
   آخر {N} عمليات · بتاريخ {dd/MM/yyyy}
   من: {distributor name} {distributor phone if set}
   ---
   تطبيق المسرّب | م. عمر الحومي
   mosarib.tubbasoft.com
   ```
   - The amount here is written, so use the exact written words
     (`Formatters.amountToArabicWords`), not the speech form.
   - **Shared footer:** in `core/util/FileSharingHelper.kt` (allowed for this item only), add the
     line `mosarib.tubbasoft.com` to `MESSAGE_FOOTER`, under "تطبيق المسرّب | م. عمر الحومي". Keep
     the existing `wa.me/967773712030` line. Reuse `MESSAGE_FOOTER` in the caption so every
     message has the same footer.
   - Add a unit test for the caption: عليه / له / مسدد, and a missing distributor phone.
2. **Pronunciation, owner (2026-11-03):** "عليك" is read as **"عليكِ"** (feminine); it must be
   **"عَلَيْكَ"**. Add to the dictionary, with the prefixes:
   - عليك → عَلَيْكَ
   - لك → لَكَ
   - عليكم → عَلَيْكُم

   Also fix two points in the round-3 sample:
   - inside compound numbers, write "مِئة" with the kasra everywhere ("مِئة وخمسة وعشرون ألف"
     still has a plain "مئة");
   - 2,000,000 must be "مليونين ريال", not "مليونان ريال".

   Add tests.
3. Not required: the voice "صوت ١ · ar-language" is a generic alias of the default voice. Hide it
   if it duplicates another listed voice.

Everything else in 025 is accepted. After this round, Claude does a short device check and merges.
