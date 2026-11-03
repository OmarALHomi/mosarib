# 032 review, round 1 (Claude, 2026-11-04)

Build and 205 tests pass. Claude also ran 14 NEW phrases that are not in the table (holdout
check): 12 were exactly right, which is very good work. Two bugs:

1. **"بالليل" after midnight.** "مع احمد علي من امس الساعه 10 بالليل لا الساعه 2 بالليل من 10 الف"
   gives end = today 14:00, 960 min. Expected: 22:00 → 02:00, 240 min.
   - Rule: بالليل / الليل with hour 1–4 means 01:00–04:00 (after midnight), not PM.
   - "نص الليل" stays 00:00. Hours 6–12 with بالليل stay PM.
2. **Paid amount lost when notes start with "و…".** "مع يوسف قاسم ساعتين من 10 الف وسلم 5 الف وقال
   الباقي بكره" gives paid = null and notes = "و سلم وقال الباقي بكره". Expected: paid 5000, notes
   "وقال الباقي بكره" (or "قال الباقي بكره").
   - "وسلم عشرين الف باقي الاسبوع" works, so the fault is in digits + "الف" followed by a word
     starting with "و".
   - Check that a "و"-word after an amount never swallows or cancels the amount.

Add both phrases to `VoicePhraseTableTest` with these expectations, plus:
- "مع احمد علي من الساعه 11 بالليل لا الساعه 3 بالليل" → 23:00 → 03:00, 240 min;
- "مع احمد علي ساعتين من 10 الف ودفع 5 الف وباقي عليه" → paid 5000, notes "وباقي عليه" or
  "باقي عليه".

Change only `VoiceCommandParser.kt` / `SpokenDurationParser.kt` / `SpokenNumberParser.kt` /
`VoicePhraseRules.kt` and the test. Then set the task status back to done.
