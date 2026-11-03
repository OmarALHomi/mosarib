# Review 030, round 2 (Claude + owner speaking, 2026-11-03 06:05, device R5CR812MCHE, online recognition)

Build and tests pass. The home tile opens the hint sheet. With the internet on, recognition runs
online (Google, `Arabic (Yemen)`).

## Owner voice test
| # | Spoken | Result |
|---|---|---|
| 1 | سقي أحمد العولقي ساعتين ونص من عشرة آلاف، دفع خمسة آلاف | ⚠️ session form, customer "أحمد بن حسين العولقي" ✓, 2 h 30 ✓; **rate and paid empty**. Banner: "كلمات لم يتم التعرف عليها: 10,000 دفعه 5000" |
| 2 | قبض من أحمد العولقي خمسطعش ألف | ✓ receipt, the right customer, 15,000, auto-distribution |
| 3 | صرف ديزل خمسين ألف | ✓ expense 50,000, notes "ديزل" (no "فهمت" banner on this form) |
| 4 | شراء من أحمد الريمي ثلاث ساعات من ثمانية آلاف | ✓ owner, 3 h, rate 8,000, the form computed 24,000, banner shown |
| 5 | خصم لأحمد ألفين | ✓ the chooser with the three "أحمد" customers + "عميل آخر…" |

## Required
1. **Google's real output, from phrase 1:** "… من 10,000 دفعه 5000".
   - **Paid words:** also accept دفعه، دفعة، ودفع، و دفع، دافع، مدفوع، سلّم، سلمه، وسلم، and a
     missing space after و. Normalise ة→ه (as everywhere) before matching.
   - **Numbers with separators:** "10,000", "10٬000", "10 000", "١٠٬٠٠٠", "10.000" (a dot used
     as a thousands separator when followed by exactly 3 digits).
   - Split the rate and the paid part **before** parsing the rate: the text after "من" ends at
     the paid word.
   - Add the exact phrase "سقي أحمد العولقي ساعتين ونص من 10,000 دفعه 5000" as a test, giving
     150 min, rate 10000, paid 5000.
2. **"ونص" is sometimes not understood** (owner). We do not know what Google wrote in those
   cases. Accept every variant: "ونص"، "و نص"، "ونصف"، "و نصف"، "ونُص"، "و30"، "و 30 دقيقه"، "و نص
   ساعه"، "ساعتين ونص ساعه"، "٢ ساعه ونص"، "2:30"، "ساعتين و نصف"، "الا ربع"، "إلا ربع". Add tests.
3. **Show what Google heard.** Under the "فهمت: …" line of the banner, add a muted line **"سمعت:
   {the recognizer text that was used}"**. It helps the user see why something was missed, and
   helps us fix the dictionary. Also write `Log.d("VoiceInput", candidates)` in debug builds
   only.
4. **The banner on every voice-filled form:** the voucher forms (receipt, expense, discount)
   showed no "فهمت" banner (phrase 3). Show the same banner, with the speaker and "إعادة"
   buttons, on all of them.
5. **Recognition support (2b):** distinguish "Arabic can be downloaded"
   (`supportedOnDeviceLanguages`) from "Arabic is not offered on the device". Offer the download
   only in the first case. In the report, write the exact lists `checkRecognitionSupport`
   returned on this phone (they are still missing).


## 6. Free-form phrases with no type word (owner, 2026-11-03), required in this round
People do not always start with "سقي" / "قبض". The owner's real examples:
- "ثلاث ساعات ونص مع احمد علي من 10 الف ومسلم 20 الف" → session, 210 min, rate 10000, paid 20000
- "ثلاث وربع مع احمد سعيد من 10 الف وادي 20 الف" → session, 195 min, rate 10000, paid 20000
- "50 الف مع يونس قاسم" → **expense** voucher 50000 to that person (he has the money)
- "20 الف ريال من احمد عبده لي" → **receipt** 20000 from that person

When no explicit type word is found, infer the type from the structure, in this order:

| Signal in the phrase | Type |
|---|---|
| An explicit type word anywhere (the current list + قبضت من، استلمت من، وصلني من، جاني من، اعطيت، عطيت، سلمت لـ، دفعت لـ، صرفت لـ، شريت من، اشتريت من، خصمت، سامحت، مسامحه، تخفيض) | that type |
| A **duration** (see below) | SESSION. But if the matched person **is a well owner**, ask with a small choice: "سقي على حسابه" / "شراء ساعات منه" |
| An amount + **"من {person}"** (optionally followed by لي، الي، عندي، وصلت، استلمتها) | RECEIPT |
| An amount + **"مع {person}"**، "لـ{person}"، "حق {person}"، "عند {person}" | EXPENSE (to that person) |
| An amount + an expense category word and no person | EXPENSE |
| An amount + a person, with no direction word | ask: "قبض منه" / "صرف له" / "خصم له" |

Field rules for these phrases:
- **Joiners before the person:** مع، من، لـ، ل، حق، عند، الاخ، الحاج.
- **"من" + a number = the rate** (after a duration). **"من" + a name = the source**
  (receipt). Decide by whether the next tokens parse as a number.
- **Paid words** (sessions and purchases), with or without و: دفع، دفعه، ادى، أدّى، وادي، اداني،
  سلم، سلّم، مسلم، مسلّم، ومسلم، سدد، عطاني، اعطاني، حاسب، نقد، كاش.
- **Rate forms:**
  - "من 10 الف"؛
  - "الساعه بـ10 الف" / "الساعه بعشره"؛
  - "كل ساعه 10 الف"؛
  - "سعر الساعه 10 الف"؛
  - "بـ10 الف للساعه".
- **Durations without the unit word:**
  - at the start, "ثلاث وربع" / "ثلاث ونص" / "اربع الا ربع" (a number followed by
    ونص/وربع/وثلث/الا ربع/الا ثلث means hours);
  - "ساعتين الا ربع"، "نص ساعه"، "ساعه الا ثلث"، "ربع ساعه"، "2:30"، "3 و45 دقيقه".
- **Ignore:** the words ريال، ر.ي، فقط، تقريبا، "والباقي عليه"، "والباقي". The remainder is
  computed.
- **Number words also include:** عشره الف، عشر تلاف، 10 الف، ١٠ الف، "عشرين الف ونص" (20500)، مية
  الف، ميتين الف، الف وخمسميه.

**When the type was inferred (not said):**
- the banner says it clearly, "فهمت: سند صرف · يونس قاسم · 50,000";
- next to it, quick chips **"غيّر إلى: قبض · صرف · خصم · سقي"** reopen the right form with the
  same person and amount, with no new speaking. Never save.

**Tests:**
- the owner's four phrases exactly as above;
- at least 40 more free-form phrases covering every row of the table, the paid words, the
  rate forms, the durations without a unit, "من" with a number vs a name, a well owner with a
  duration (gives "ask"), and an amount + name with no direction (gives "ask");
- each written once with digits and once with words, as the recognizer may return either.

After this round, Claude repeats the five phrases with the owner. Any phrase that fails then is
added to the tests.
