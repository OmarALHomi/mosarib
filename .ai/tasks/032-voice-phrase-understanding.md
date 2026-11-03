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
- Add `startMinuteOfDay: Int?` and `endMinuteOfDay: Int?` to `VoiceCommand`; the session form opens
  in its time-range mode with the start and end filled (the date stays today, or yesterday when the
  range is in the past across midnight). `durationMinutes` is also set.
- No rate → leave the rate to the form's normal default (the customer's or the pump's price).

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

## Allowed files
- `core/voice/VoiceCommandParser.kt`, `core/voice/SpokenNumberParser.kt`,
  `core/voice/SpokenDurationParser.kt`, `core/voice/VoicePhraseRules.kt` (new),
  `core/voice/VoiceCommand.kt` (`paidFull`, `startMinuteOfDay`, `endMinuteOfDay`),
  `core/voice/VoiceRecognitionHelper.kt` (pass `knownNames` / `now` through)
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
