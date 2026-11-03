status: ready
<!-- Owner decisions 2026-11-03: free for everyone; no AI (offline rules only); the user says the
hours and the hourly rate (and an optional payment), never the total; all operation types. Runs in
worktree D:\Kotlin\mosarib-t020 (branch task/030) IN PARALLEL with 027. Do NOT touch
CustomerDetailScreen.kt, features/settings/*, SessionDetailBottomSheet.kt (027 changes them). -->
# Task 030: Voice input: dictation, voice search, and spoken commands that fill a form

## Why
Many users cannot type well. They can say "سقي أحمد العولقي ساعتين ونص من عشرة آلاف، دفع خمسة
آلاف", and the app opens the irrigation form already filled. The app, not the user, computes the
total and the remaining debt.

## Hard rules
- **Never save by voice.** Voice only fills a form. The user reviews and taps save.
- **No AI, no network service of ours, no new library.** Use Android speech recognition through
  `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` (system UI, so no `RECORD_AUDIO` permission). Use
  language `ar-YE` with `ar` as fallback, `EXTRA_PREFER_OFFLINE = true`, and
  `EXTRA_MAX_RESULTS = 5`. Try every result in order and keep the first one that parses.
- **The user never says the total.** If a total-like word appears ("المجموع", "الإجمالي"), ignore
  that number. The total comes from hours × rate − waste/discount, as the form already
  computes it.
- **Free for everyone.** No license check.
- No Room schema change. Store learned aliases as JSON in `app_settings`
  (key `voice_aliases`).
- Nothing is written to Firebase.

## A. Pure parsers (new `core/voice/`, fully unit tested)
1. **`SpokenNumberParser`:** Arabic spoken amounts to `Double`.
   - Digits: the recognizer often returns "5000" or "٥٠٠٠", in both digit sets.
   - MSA: واحد … عشرة، أحد عشر …، عشرون/عشرين …، مئة/مية/ميه، مئتين/ميتين، ثلاثمئة/تلتمية،
     ألف/الف، ألفين/الفين، آلاف/الاف/تلاف، مليون/ملايين.
   - **Yemeni dialect:** حداعش، اطناعش، تلطاعش، اربعطعش، خمسطعش، ستطعش، سبعطعش، ثمنطعش،
     تسعطعش، "عشرة آلاف"، "خمسة الاف"، "الفين وخمسمية"، "ونص" after a thousand (+500, as in
     "خمسة آلاف ونص" = 5500), "ربع مليون".
   - Mixed: "15 ألف" = 15000, "2 مليون".
2. **`SpokenDurationParser`:** to minutes.
   - ساعة، ساعتين/ساعتان، ثلاث ساعات / 3 ساعات؛
   - "ونص" (+30), "وربع" (+15), "الا ربع" (−15), "وثلث" (+20);
   - "نص ساعة" (30), "ساعة وعشر دقائق", "45 دقيقة", "ساعتين و20 دقيقة".
3. **`VoiceCommandParser`:** returns `VoiceCommand(type, customerQuery, durationMinutes,
   ratePerHour, amount, paid, category, notes, unparsed)`.

   **Type words** (first match wins; also at the very start):

   | Type | Words |
   |---|---|
   | SESSION | سقي، سقية، سقاية، سقيت |
   | RECEIPT | قبض، قبضت، استلمت، سدد، سداد |
   | EXPENSE | صرف، صرفت، دفعت لـ (when there is no session context) |
   | PURCHASE | شراء، اشتريت، شريت |
   | DISCOUNT | خصم، سامحت |

   **Fields:**
   - **Rate:** the number after "من" that follows a duration ("ساعتين ونص **من عشرة آلاف**"),
     or after "سعر الساعة" / "الساعة بـ".
   - **Paid** (session/purchase): the number after دفع / دفعت / مدفوع / سلم / وسلّم.
   - **Amount** (vouchers): the main number.
   - **Expense category:** match words against the existing expense categories in the app
     (ديزل، صيانة، …). If none matches, put the text in notes.
   - **Notes:** the text after "ملاحظة" / "البيان".
   - **Customer:** the words left between the type word and the first number or duration word,
     after removing joiners (لـ، من، عند، حق).
   - **Missing values:**
     - rate missing → the form's normal default (customer price, else the global price);
     - duration missing → leave the field empty and mark it;
     - anything not understood → `unparsed`, shown to the user.
4. **`CustomerMatcher`:**
   - Normalise Arabic: أ/إ/آ → ا, ة → ه, ى → ي; drop tatweel, diacritics, "ال" and "بن/ابن".
   - Score each customer: token overlap, plus an edit distance for each token, plus the
     learned aliases.
   - Return a ranked list with a confidence. Accept automatically **only** when the best match
     is clearly ahead (define a margin and test it). Otherwise the user chooses.
   - Learned aliases: when the user corrects the customer in the review step, save
     `spokenText → customerId`. Next time it matches at once.

**Tests:** at least 60 phrases covering all five types, the dialect numbers and durations,
missing fields, two similar names ("أحمد بن حسين" / "أحمد بن قاسم"), a total word that must be
ignored, and recognizer digit output.

## B. Spoken command flow
1. **Entry points:**
   - home quick access: a new tile **"إدخال صوتي"** (mic icon);
   - a mic `IconButton` in the header of the add forms (session, voucher, purchase), through
     `MosaribBottomSheet(headerActions = …)`, which already exists.
2. **Hint sheet** before listening (first time, then a small "أمثلة" link): one example per type.
   - سقي أحمد العولقي ساعتين ونص من عشرة آلاف، دفع خمسة آلاف
   - قبض من أحمد العولقي خمسطعش ألف
   - صرف ديزل خمسين ألف
   - شراء من صالح ثلاث ساعات من ثمانية آلاف
   - خصم لأحمد ألفين
3. **After recognition:**
   - parse the result;
   - if the customer is ambiguous, show a small chooser with the top 3 and "عميل آخر…";
   - open the right existing form, **prefilled** (new optional prefill parameters on
     `AddEditSessionBottomSheet`, `AddVoucherBottomSheet` and `WellOwnerPurchaseBottomSheet`; the
     defaults keep today's behaviour).
   - The session form sets the duration in its manual-hours mode, then the rate and the paid
     amount. The form computes the total and the remainder as it does today.
4. **Summary banner** at the top of the prefilled form: "فهمت: سقي · أحمد بن حسين العولقي ·
   ساعتان ونصف · من 10,000 · دفع 5,000". Fields that were not understood are marked. The banner
   has a speaker button that reads it aloud with `MosaribSpeaker` (pronunciation rules from 025).
   Under it is a "إعادة" button to speak again.
5. **The snapshot from 026 must treat the prefilled values as the initial state.** Closing a
   voice-filled form without edits asks "تجاهل التغييرات؟", because voice data would be lost.
   Test this.
6. **Errors:**
   - no recognizer on the phone: a `MosaribDialog` that explains how to enable Google voice
     typing (button to the Play Store page of `com.google.android.googlequicksearchbox`);
   - nothing understood: "لم أفهم، حاول مرة أخرى" with the examples.

## C. Dictation in fields and voice search
- A small mic trailing icon in:
  - the amount fields of the session, voucher and purchase forms (the spoken number is parsed
    by `SpokenNumberParser`);
  - the notes fields (raw text);
  - the customer field of the session/voucher forms (through `CustomerMatcher`).
- Voice search:
  - a mic icon in the search bar of the customers screen; the search uses the
    `CustomerMatcher` ranking for the spoken text;
  - in the operations screen, the spoken text goes into the existing search.

## Allowed files
- new `core/voice/*`
- `features/sessions/SessionDialogs.kt`
- `features/vouchers/VouchersScreen.kt` (add/edit voucher form and the search bar only)
- `features/wellowners/WellOwnerPurchaseBottomSheet.kt`
- `features/customers/CustomersScreen.kt`, `features/customers/CustomersScreenComponents.kt`
  (search bar only)
- `features/home/HomeScreen.kt` (quick access tile only)
- the call sites that open these forms (to pass the prefill), except `CustomerDetailScreen.kt`;
  report if one is needed there
- `AndroidManifest.xml` (`<queries>` for `RecognizerIntent` only)
- tests

## Done when
- Build and tests pass (including the 60+ parser phrases).
- On the device:
  - each of the five example phrases opens the right form, correctly filled;
  - the session total and remainder are computed by the form;
  - an ambiguous name shows the chooser, and the correction is remembered next time;
  - field dictation and the customer search work.
- The report lists every example phrase with what the recognizer returned and what was
  filled.

---
## Review notes (written by Claude)
_(empty)_
