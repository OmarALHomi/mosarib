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

After this round, Claude repeats the five phrases with the owner. Any phrase that fails then is
added to the tests.
