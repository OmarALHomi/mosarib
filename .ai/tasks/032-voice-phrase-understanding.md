status: ready
<!-- Owner request 2026-11-03 after testing 1.4.0. Worktree D:\Kotlin\mosarib-t032, branch task/032.
Rules only, no AI, no network. Never save by voice (forms are only prefilled, as in 030). -->
# Task 032: Understand short and free-form spoken phrases (word order does not matter)

## Why
The owner said **"50 الف ريال مع يوسف قاسم سعيد"** and got "لم يتم التعرف على العملية". The log
shows Google heard it perfectly:
```
parsed: VoiceCommand(type=null, customerQuery=50000 ريال مع يوسف قاسم سعيد, amount=null ...)
branch: UNRECOGNIZED_COMMAND
```
Cause: in `VoiceCommandParser.parse`, the `null ->` branch only looks for an amount at the END of the
phrase (`words.subList(k, words.size)`). When the amount comes first, nothing is found and the whole
phrase becomes the name. The parser in general depends on word order. The owner wants every simple
or short phrase understood, and will send more phrases from daily use later, so the words must be
easy to add.

## 1. A slot-based parser, independent of word order
Rewrite the core of `VoiceCommandParser.parse` as: **find the pieces anywhere in the phrase, remove
them, and what is left is the name**.

Steps on the normalized text (existing `SpokenNumberParser.normalizeToken`,
`cleanNumberSeparators`):
1. **Verb / type word** (anywhere, not only at the start): see the table in section 2.
2. **Duration** (`SpokenDurationParser.findDurationSpan`): "ثلاث ساعات ونص", "ساعتين وربع",
   "3 ساعات", "ساعه الا ربع", "نص ساعه", "90 دقيقه", and also a bare "ثلاث وربع" when it is
   followed by a rate marker (owner example "ثلاث وربع مع احمد سعيد من 10 الف").
3. **Amounts**: every spoken number in the phrase, with its unit words removed: "الف / آلاف / الاف /
   ألفين / مليون / ريال / ريالات". Keep each amount with its position and the marker word before it:
   - rate markers: من، بسعر، سعر الساعه، الساعه بـ (only when a duration exists);
   - paid markers: سلم، مسلم، وسلم، دفع، دفعه، ودفع، مدفوع، واصل، وصل، وادي، ادى، أدى، عطى، اعطى،
     نقد، كاش;
   - no marker.
4. **Direction words before the name**: من، مع، لـ / ل (joined), لحساب، حساب، على حساب، حق، عند،
   الى، إلى، للأخ، للاخ؛ and honorifics الأخ، الاخ، العم، الحاج، الشيخ، أبو (keep "أبو" as part of
   the name, it is part of names like "ابو محمد").
5. **Trailing words** to drop: لي، عندي، اليوم، الحين، الآن، تمام، يا، بس.
6. **The name** = the remaining words (1–5 words), cleaned of joiners. Matched with the existing
   `CustomerMatcher` and the aliases.

## 2. Deciding the operation (owner's rules, in this order)
| What was said | Operation | Filled fields |
|---|---|---|
| a type word (table below) | that type | as found |
| duration + name + one amount | **سقي دين** (SESSION) | rate = the amount, paid = 0 |
| duration + name + two amounts | **سقي مع تسليم جزئي** (SESSION) | first = rate, second = paid; a paid marker decides which one is paid when present |
| duration + well-owner name, no type word | the existing chooser (سقي على حسابه / شراء ساعات منه) | |
| amount + "من {name}" (no duration) | **قبض** (RECEIPT) | amount |
| "وصل / واصل / استلمت / سجل / قيد" + amount + "ل/لحساب {name}" | **قبض** (RECEIPT) | amount |
| amount + "مع {name}" (no duration) | **صرف** (EXPENSE) to that customer (owner, 2026-11-03: "50 الف مع يونس قاسم" = سند صرف) | amount |
| amount + "ل/لـ/الى {name}" with صرف word or "اعطيت / سلمت ل" | **صرف** (EXPENSE) | amount |
| amount + an expense category (ديزل، صيانة…) | **صرف** with that category | amount, category |
| amount + name, none of the above | the existing type chooser (قبض / صرف / خصم) with the amount and name filled | |
| nothing usable | the existing "لم يتم التعرف" dialog | |

**Type words** (anywhere in the phrase, normalized; keep them in one table so new words are one line
each):
- SESSION: سقي، سقيه، سقايه، سقيت، سقينا، ري، جلسه، دوره
- RECEIPT: قبض، قبضت، قبضنا، استلمت، استلمنا، سدد، سداد، وصل، واصل، وصلني، سجل، قيد، اضف لحساب،
  ايداع، دفع لي، جاب، جابه
- EXPENSE: صرف، صرفت، صرفنا، اعطيت، عطيت، سلفه، سلفت، دفعت ل، سلمت ل
- PURCHASE: شراء، اشتريت، شريت، اشترينا، من البئر، من صاحب البئر
- DISCOUNT: خصم، سامحت، سامحته، تنازل، اسقط

## 3. Data-driven rules
Move all word lists (type words, markers, joiners, trailing words, expense categories) to one new
file `core/voice/VoicePhraseRules.kt` (plain `object` with lists/maps and a short comment per list).
The parser reads only from there. Adding a phrase later must be a one-line change plus a test line.

## 4. Tests: a phrase table
Add `VoicePhraseTableTest.kt` with a table (phrase → type, name, minutes, rate, amount, paid). It
must include at least these, each passing:

| Phrase | Type | Name | Min | Rate | Amount | Paid |
|---|---|---|---|---|---|---|
| 50,000 ريال مع يوسف قاسم سعيد | EXPENSE | يوسف قاسم سعيد | | | 50000 | |
| 50 الف مع يونس قاسم | EXPENSE | يونس قاسم | | | 50000 | |
| 20 الف ريال من حارث سعيد | RECEIPT | حارث سعيد | | | 20000 | |
| 20 الف ريال من احمد عبده لي | RECEIPT | احمد عبده | | | 20000 | |
| وصل 20 الف لحارث سعيد | RECEIPT | حارث سعيد | | | 20000 | |
| سجل 20 الف لحساب حارث سعيد | RECEIPT | حارث سعيد | | | 20000 | |
| حارث سعيد واصل منه 20 الف | RECEIPT | حارث سعيد | | | 20000 | |
| ثلاث ساعات ونص مع احمد علي من 10 الف | SESSION | احمد علي | 210 | 10000 | | 0/null |
| ثلاث ساعات ونص مع احمد علي من 10,000 ومسلم 20,000 | SESSION | احمد علي | 210 | 10000 | | 20000 |
| ثلاث وربع مع احمد سعيد من 10 الف وادي 20 الف | SESSION | احمد سعيد | 195 | 10000 | | 20000 |
| احمد علي ساعتين من 8 الف | SESSION | احمد علي | 120 | 8000 | | |
| ساعتين ونص احمد علي 10 الف 5 الف | SESSION | احمد علي | 150 | 10000 | | 5000 |
| سقي محمد صالح ساعه ونص بسعر 12 الف | SESSION | محمد صالح | 90 | 12000 | | |
| قبض من محمد صالح خمسين الف | RECEIPT | محمد صالح | | | 50000 | |
| خمسين الف من محمد صالح | RECEIPT | محمد صالح | | | 50000 | |
| صرف 30 الف ديزل | EXPENSE | | | | 30000 (category ديزل) | |
| 15 الف صيانه | EXPENSE | | | | 15000 (category صيانة) | |
| اعطيت يوسف 10 الف | EXPENSE | يوسف | | | 10000 | |
| خصم 5 الف لاحمد علي | DISCOUNT | احمد علي | | | 5000 | |
| اشتريت ثلاث ساعات من صاحب البئر سالم من 7 الف | PURCHASE | سالم | 180 | 7000 | | |
| مية الف من ابو محمد | RECEIPT | ابو محمد | | | 100000 | |
| ألفين ريال مع علي | EXPENSE | علي | | | 2000 | |

Keep every existing test in `VoiceParsersTest.kt` passing; if one conflicts with the owner rules
above, change the expectation and list it in the report.

## 5. The "لم يتم التعرف" dialog
When some pieces were found (an amount, a duration or a name) never show "لم يتم التعرف": open the
type chooser or the closest form with what was found. Show the dialog only when nothing was found.

## 6. The owner's most common patterns (owner, 2026-11-03, second message): these win over section 2
"ريال" is optional after every amount. `[...]` = may be missing.

**A. Irrigation by hours**
`مع {name} {duration} من {rate} [ريال] [ومسلم {amount | كامل}] [{notes}]`
- No paid part → the session is a debt (paid empty).
- "كامل" (also: كامل المبلغ، كله، خالص) → paid = the full total; add `paidFull: Boolean` to
  `VoiceCommand` and let the form fill paid with the computed total.
- Any words after the last recognized piece = **البيان** (notes).

**B. Irrigation by a time range**
`مع {name} من الساعه {t1} {الى | لا | لين | حتى} الساعه {t2} [من {rate}] [ومسلم {amount | كامل}] [{notes}]`
- "لا" means "إلى" **only between two times** ("من الساعه 12 لا الساعه 2"). Elsewhere "لا" is
  ignored or treated as normal text. The word "الساعه" before t2 may be missing.
- Times: "12"، "2 ونص"، "3 وربع"، "4 الا ربع"، "11 وعشر دقايق"، digits "2:30".
- Day-part words: **بالصبح / الصبح / صباحاً = AM**, **بالليل / الليل / مساءً = PM**. Also: الفجر
  (AM), الظهر (12–2 PM), العصر / بعد العصر (PM), المغرب (PM), العشاء (PM).
- No day-part word: pick the most recent past time for t1 relative to the time of speaking
  (people record after the irrigation), then t2 is the first time after t1 within 12 hours.
  Crossing midnight is allowed ("من 9 بالليل لا 1" = 21:00 → 01:00, 240 min). The parser takes
  `now` as a parameter so the tests are fixed.
- Add `startMillis: Long?` and `endMillis: Long?` (full date and time) to `VoiceCommand`; the
  session form opens in its time-range mode with its existing `startDateMillis` / `endDateMillis`
  and times filled from them. `durationMinutes` is also set.
- No rate → leave the rate to the form's normal default (the customer's or the pump's price).

**B2. Long irrigation with a day or date (owner, 2026-11-03, third message)**
`مع {name} [{duration}] من {day} الساعه {t1} {الى | لا | لااا} [{day2}] الساعه {t2} [من {rate}] [ومسلم …] [{notes}]`
- **Day words** (the start, and optionally the end):
  - اليوم; امس / البارحه / البارح; قبل امس / اول امس / اول من امس;
  - weekday names (السبت … الجمعه, with or without "يوم"): the most recent past one (today
    counts only if the time is already past);
  - "يوم 25" / "25 الشهر": the most recent past day 25; "25/10" or "25-10": that date this year
    (last year if it would be in the future).
- "لا" stretched in speech ("لاا"، "لااا") is the same as "لا". Normalize repeated letters.
- **End**: with an end day, use it. Without one, the end is the first time after the start within
  24 hours (day-part words respected), so "من امس الساعه 8 بالصبح لا الساعه 10 بالليل" is 14 hours
  on the same day and "من امس الساعه 8 بالليل لا 6 بالصبح" ends today at 06:00.
- **Hours said and a range said**: the range wins for start, end and duration. If the spoken hours
  differ from the range by more than 15 minutes, keep the range and add a line to the voice banner:
  "قلت {N} ساعات، والمدة بين الوقتين {M}، راجعها".
- A day word counts as a date only right after "من" / "الى" / "لا" and before a time; elsewhere it
  is normal text, so "باقي امس" in the notes stays notes.

**C. Payment voucher (صرف) to a person**
`مع {name} {amount} [{notes}]` or `{amount} مع {name} [{notes}]` with no duration → **EXPENSE** to
that customer, notes = the words after.

**D. Receipt voucher (قبض)**
`{amount} من {name} [بن {name}] [{notes}]` → **RECEIPT**. "بن / ابن / بنت" join name parts.

**E. General expense (no person)**
`{amount} {notes}` where the words right after the amount start a description and are not "من/مع
{person}" → **EXPENSE with no customer**. Example: "20 الف ريال حق الديزل من محطة صالح بن علي" is a
general expense with notes "حق الديزل من محطة صالح بن علي" and category ديزل: the "من" there is part
of the description, because a description word came first. Description starters: حق، ثمن، قيمه،
اجره، اجرة، مقابل، عشان، بدل، فاتوره، and every expense category.

**Where the name ends and the notes begin**: `parse` gets an optional list of the known customer
names (`parse(raw, knownNames = emptyList(), now = ...)`; HomeScreen passes the customers and
aliases). Take the longest run of 1–5 words after the joiner that matches a known name (through
the normalization of `CustomerMatcher`); the rest is notes. With no match, the name ends at the
first description starter or amount; if neither exists, all the words are the name.

**More tests** (add to the table; pass `knownNames` = [احمد علي، يوسف قاسم، يوسف قاسم سعيد، حارث سعيد
بن علي، حارث سعيد] and a fixed `now` = 15:00):

| Phrase | Type | Name | Min | Start→End | Rate | Paid | Notes |
|---|---|---|---|---|---|---|---|
| مع احمد علي ثلاث ساعات من 10 الف ريال ومسلم 20 الف حق الشهر الماضي | SESSION | احمد علي | 180 | | 10000 | 20000 | حق الشهر الماضي |
| مع احمد علي ساعتين ونص من 8000 | SESSION | احمد علي | 150 | | 8000 | (debt) | |
| مع احمد علي ساعتين من 10 الف ومسلم كامل | SESSION | احمد علي | 120 | | 10000 | paidFull | |
| مع احمد علي من الساعه 12 لا الساعه 2 من 10 الف | SESSION | احمد علي | 120 | 12:00→14:00 | 10000 | | |
| مع احمد علي من الساعه 8 بالصبح الى الساعه 11 ونص مسلم كامل | SESSION | احمد علي | 210 | 08:00→11:30 | | paidFull | |
| مع احمد علي من الساعه 9 بالليل لا الساعه 1 | SESSION | احمد علي | 240 | 21:00→01:00 | | | |
| مع احمد علي من 1 لا 2 ونص من 10 الف ومسلم 5 الف باقي امس | SESSION | احمد علي | 90 | 13:00→14:30 | 10000 | 5000 | باقي امس |
| مع يوسف قاسم 50 الف سلفه للعيد | EXPENSE | يوسف قاسم | | | | | سلفه للعيد (amount 50000) |
| 50 الف ريال مع يوسف قاسم سعيد حق العمال | EXPENSE | يوسف قاسم سعيد | | | | | حق العمال (amount 50000) |
| 30 الف من حارث سعيد بن علي باقي حساب الشهر | RECEIPT | حارث سعيد بن علي | | | | | باقي حساب الشهر (amount 30000) |
| 20 الف ريال حق الديزل من محطة صالح بن علي | EXPENSE | (none) | | | | | حق الديزل من محطة صالح بن علي (amount 20000, category ديزل) |

**Day and date tests** (fixed `now` = Tuesday 2026-11-03 15:00):

| Phrase | Min | Start → End | Rate | Paid |
|---|---|---|---|---|
| مع احمد علي من امس الساعه 8 بالليل لا الساعه 6 بالصبح من 10 الف | 600 | 2026-11-02 20:00 → 11-03 06:00 | 10000 | |
| مع احمد علي 14 ساعه من قبل امس الساعه 6 بالصبح الى الساعه 8 بالليل ومسلم 50 الف | 840 | 11-01 06:00 → 11-01 20:00 | | 50000 |
| مع احمد علي من يوم الخميس الساعه 10 بالليل الى يوم الجمعه الساعه 4 العصر من 10 الف | 1080 | 10-29 22:00 → 10-30 16:00 | 10000 | |
| مع احمد علي من يوم 25 الساعه 7 بالصبح لااا يوم 26 الساعه 7 بالصبح | 1440 | 10-25 07:00 → 10-26 07:00 | | |
| مع احمد علي من 25/10 الساعه 7 الصبح لا الساعه 11 الصبح | 240 | 10-25 07:00 → 10-25 11:00 | | |
| مع احمد علي 10 ساعات من امس الساعه 8 بالصبح لا الساعه 10 بالليل | 840 (+ banner line: 10 vs 14) | 11-02 08:00 → 11-02 22:00 | | |

## 7. Breadth and robustness (owner: "اجعل الموضوع فيه توسع وشمولية")
The patterns above are the core. This section makes them hold with real speech, dialect and the way
Google writes Arabic. Put every list in `VoicePhraseRules.kt`.

**7.1 Normalization (before any rule)**
- Arabic letters: أ إ آ ٱ → ا; ى → ي; ة → ه; ؤ → و; ئ → ي.
- Remove marks and tatweel; Eastern digits ٠-٩ and ۰-۹ → 0-9.
- "٫" and "," inside numbers are removed; "،" and "." between words become spaces.
- Repeated letters collapse ("لااا" → "لا", "ايوووه" → "ايوه"). Never inside digits.
- "و" glued to a word is split when the rest is a known word ("ومسلم" → "و مسلم", "وربع",
  "ونص").

**7.2 Spoken numbers (dialect forms)**
- Units: الف، ألف، الاف، آلاف، تلاف (as in "عشر تلاف")، الفين، الفان، مليون، ملايين.
- Hundreds: مية، ميه، مئه، ميتين، ثلاثميه / تلتميه … تسعميه.
- Tens and units in dialect: تلاته، تلاتين، تمنيه، تمانين، اتنين، اتنعش، حداعش، طنعش، خمستاشر…
- Fractions with thousands: "خمسه آلاف ونص" = 5500, "عشره ونص" (with الف) = 10500, "الف ونص" =
  1500, "ربع مليون" = 250000, "نص مليون" = 500000.
- "عشرين الف وخمسميه" = 20500; "10 الاف و500" = 10500.
- The same amount said twice in a row (a speech repeat) counts once.

**7.3 Paid / debt / cash words**
- Paid: سلم، مسلم، سلّم، سلمه، سلمها، سلمنا، اعطى، عطى، اعطانا، دفع، دفعه، دفع منها، واصل، وصل
  منه، ادى، وادي، جاب، نقد منها، عربون، مقدم.
- Full paid: كامل، كامل المبلغ، كله، خالص، مخلص، نقد، نقدا، كاش، دفع الكل، ما عليه شي.
- Explicit debt (paid empty, no question): دين، دينه، عليه، اجل، آجل، على الحساب، قيده عليه، ما
  سلم، ما دفع، بدون تسليم، ولا ريال.

**7.4 Durations in dialect**
- ساعه، ساعتين، ساعات، ساعتين الا ربع، ساعه وثلث، ساعتين وتلت، نص ساعه، ربع ساعه، ثلث ساعه،
  دقيقه، دقايق، دقائق، "ساعه وعشر دقايق", "45 دقيقه", "ساعه الا عشر".
- "ثلاث ونص" / "ثلاث وربع" without the word ساعات count as hours only when followed by a rate
  marker ("من …") or a paid marker, or preceded by "مع {name}" in the SESSION pattern.

**7.5 Times and day parts**
- "الساعه 2 ونص"، "2 وربع"، "2 الا ربع"، "2 وتلت"، "2 وعشر"، "2 و 20"، "14:30"، "2:30".
- Day parts: الفجر، الصبح، بالصبح، صباحا، الضحى، الظهر، الظهريه، العصر، بعد العصر، المغرب،
  العشاء، بالليل، الليل، نص الليل (00:00)، اخر الليل (AM).
- "لا"، "الى"، "إلى"، "لين"، "لما"، "حتى"، "لعند" between two times all mean "to".

**7.6 Corrections inside one phrase**
"لا لا"، "لا قصدي"، "قصدي"، "اقصد"، "عفوا"، "بل"، "اقول": the part after the correction replaces the
same kind of piece said before it ("من 10 الف لا قصدي 12 الف" → rate 12000; "مع احمد علي لا قصدي
احمد سعيد" → name احمد سعيد). A single "لا" between two times is still "to".

**7.7 Two operations in one phrase**
If a second operation is clearly started ("… وقبض من …"، "… وصرف …"), fill the form with the first
one and add a banner line: "قلت عمليتين، سُجّلت الأولى، أعد قول الثانية". Never save anything.

**7.8 Fuzzy keyword match**
Keywords of 4 letters or more match with one letter different (Google spelling slips: "مسلّم" /
"مسلمه" / "مسلم"، "سقيه" / "سقيا"). Never fuzzy-match short words (من، مع، لا، ل) or names.

**7.9 The phrases the app did not understand (to grow the rules)**
- Store the last 50 phrases that ended in the type chooser, the "لم يتم التعرف" dialog or a field
  the user changed right after a voice fill. Store them **on the phone only** (`app_settings`, key
  `voice_unparsed_log`, JSON list of {text, date, result}). Never Firebase, never network.
- In `SettingsVoiceScreen` (the voice settings page) add a row "جمل لم تُفهم ({n})" that opens a
  list with copy and share (text) buttons and "مسح القائمة". The owner sends this list to Claude so
  the rules grow from real use.

**7.10 Generated tests**
Besides the table, add a generated test: for each core pattern (A, B, B2, C, D, E) build variants
automatically: amount in digits / words / with and without "ريال"; "ومسلم" / "وسلم" / "ودفع";
"الى" / "لا" / "لين"; with and without "الساعه"; name before or after the duration; with and without
notes. At least 200 generated phrases, all expected to parse to the same fields as the base. List the
counts per pattern in the report.

**7.11 More table rows** (same `knownNames` and `now` as above):

| Phrase | Expect |
|---|---|
| مع احمد علي تلات ساعات من عشر تلاف وسلم خمستاشر الف | SESSION 180 min, rate 10000, paid 15000 |
| مع احمد علي ساعتين الا ربع من 10 الف دين | SESSION 105 min, rate 10000, paid empty |
| مع احمد علي ساعه وتلت من 9 الاف نقدا | SESSION 80 min, rate 9000, paidFull |
| مع احمد علي من 10 الف لا قصدي 12 الف ساعتين | SESSION 120 min, rate 12000 |
| مع احمد علي لا قصدي يوسف قاسم ساعتين من 10 الف | SESSION, name يوسف قاسم |
| مع احمد علي من الساعه 2 الا ربع لين الساعه 5 العصر | SESSION 13:45 → 17:00, 195 min |
| مع احمد علي من نص الليل لا الفجر الساعه 4 | SESSION 00:00 → 04:00, 240 min (today; earlier than now) |
| خمسه آلاف ونص من حارث سعيد | RECEIPT 5500 |
| ربع مليون من حارث سعيد بن علي | RECEIPT 250000, name حارث سعيد بن علي |
| ٢٠٠٠٠ من احمد علي | RECEIPT 20000 |
| مع يوسف قاسم عشرين الف وخمسميه حق قطع غيار | EXPENSE 20500, notes "حق قطع غيار" |
| 10 الاف ديزل و5 الاف زيت | EXPENSE 10000 ديزل + banner "قلت عمليتين…" |
| سقي احمد علي ساعتين من 10 الف وقبض من حارث سعيد 5 الف | SESSION (first) + banner "قلت عمليتين…" |

## Allowed files
- `core/voice/VoiceCommandParser.kt`, `core/voice/SpokenNumberParser.kt`,
  `core/voice/SpokenDurationParser.kt`, `core/voice/VoicePhraseRules.kt` (new),
  `core/voice/VoiceCommand.kt` (`paidFull`, `startMillis`, `endMillis`, a banner warning line),
  `core/voice/VoiceRecognitionHelper.kt` (pass `knownNames` / `now` through),
  `core/voice/VoiceUiComponents.kt` (banner warning lines of B2 and 7.7, the 7.9 list UI)
- `features/settings/SettingsVoiceScreen.kt` (only the 7.9 row and list)
- `core/voice/VoiceUnparsedLog.kt` (new, 7.9 storage)
- `features/home/HomeScreen.kt` (section 5, and passing the customer names)
- `features/sessions/SessionDialogs.kt` (prefill only: time-range mode with start/end, paidFull)
- `features/vouchers/VouchersScreen.kt` (prefill only, if notes or "no customer" are not already
  filled)
- tests

## Done when
- Build and tests pass; every row of the table passes.
- The report lists any phrase that could not be supported and why.
- The device check is done by Claude/owner after the review.

---
## Review notes (written by Claude)
_(empty)_
