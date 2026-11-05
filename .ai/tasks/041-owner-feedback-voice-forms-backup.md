status: done
<!-- Owner feedback on the phone, 2026-11-05. Done by Claude at the owner's request; phone test pending. -->
# Task 041: Voice times, form flicker, guides, backup, home buttons, customer-screen forms

Done:
- Forms flickered and the keyboard closed on every letter: `MosaribBottomSheet` passed a new
  `confirmValueChange` lambda each recomposition, so `rememberModalBottomSheetState` was rebuilt whenever
  the title, subtitle or unsaved flag changed (typing a name, قبض/صرف switch, well-owner switch).
  The lambda is now remembered.
- Customer suggestions popup is non-focusable (it took focus and closed the keyboard); the list arrow
  shows next to the field mic.
- Voice: day parts now apply to "10:00"-style times (the speech engine always writes them so);
  more day-part words (بالظهر، ظهرا، عصرا، فجرا، ليلا ...); "10 الظهر" = 10 AM; "امس من ..."; "للساعه";
  "سجل مع ..." no longer joins the name; irrigation notes only from words after the last number.
  Tests: `VoiceTimeRangeTask041Test`.
- Repeated start beep: automatic listening restarts briefly mute the beep streams.
- Guides: field mic vs whole-operation mic, price text, paid, full payment, waste/discount, notes;
  voucher and purchase tours extended.
- Home backup shortcut opens the backup page; confirm before Google sign-out.
- Drive backup: no second automatic copy on the same day, same-day uploads use current data,
  one Drive copy per day name.
- Home: «سند جديد» (قبض أو صرف) and a new «حسابات العملاء» button.
- From a customer's screen: no customer picker in the voucher or irrigation form; unpaid sessions
  are a collapsed card with the total and «تحديد الكل».
- Version 1.4.4 (9).
