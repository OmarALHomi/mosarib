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

## Allowed files
- `core/voice/VoiceCommandParser.kt`, `core/voice/SpokenNumberParser.kt`,
  `core/voice/SpokenDurationParser.kt`, `core/voice/VoicePhraseRules.kt` (new),
  `core/voice/VoiceCommand.kt` (only if a field is truly needed)
- `features/home/HomeScreen.kt` (section 5 only)
- tests

## Done when
- Build and tests pass; every row of the table passes.
- The report lists any phrase that could not be supported and why.
- The device check is done by Claude/owner after the review.

---
## Review notes (written by Claude)
_(empty)_
