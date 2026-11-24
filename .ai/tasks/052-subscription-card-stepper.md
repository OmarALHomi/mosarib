status: accepted
<!-- Owner request 2026-11-22: the forced subscription sheet (even after 050) is long and ugly. Replaces 050's layout. -->
# Task 052: Subscription = small card, then a one-step-at-a-time stepper in the same dialog

File: `core/license/LicenseDialog.kt`, the **non-registered** branch only (the `if (isRegistered)`
renewal dialog stays as it is). Keep every call, validation and activation path from 050 unchanged —
this is presentation only.

## A. The card (first thing the user sees)
A centred `Dialog` (not a full-height sheet), width ~90%, rounded 24dp, theme colours, soft shadow:
- Icon (lock or key) in a tinted circle.
- Title: trial ended → «انتهت الفترة التجريبية»; otherwise «اشترك في المسرّب».
- One or two lines: ended → «بياناتك محفوظة ويمكنك تصفح حساباتك. لإضافة عمليات جديدة اشترك.»;
  otherwise «بقي لك N عملية مجانية».
- Buttons: primary «طلب اشتراك», secondary «إغلاق» (calls `onDismiss`), and a small text button
  «خروج من التطبيق» (existing exit action) under them. Small link «لديّ رمز تفعيل» jumps to step 4.

## B. Stepper (same dialog, after «طلب اشتراك»)
Top: a compact progress row ١ — ٢ — ٣ — ٤ (current step filled with primary) and the step title.
Only one step visible; content scrolls inside the dialog if needed (dialog max height ~80% screen).
Bottom: «السابق» / «التالي» (last step: «تفعيل»).
1. **الباقة** — two selectable cards شهري / سنوي with prices (as 050 shows them; «—» + retry if missing).
2. **الدفع** — transfer method menu, the account/wallet details for that method, optional transfer number.
3. **بياناتك** — name, phone (validation as today), big «إرسال الطلب عبر واتساب», small «إرسال الطلب
   من التطبيق» (the existing online request). «التالي» goes to step 4.
4. **رمز التفعيل** — the code field and «تفعيل» (existing activation), with a line «ستصلك الرسالة
   بالرمز بعد تأكيد الدفع».
Region field, device line, Google restore card and features list: move into step 3 under a collapsed
«تفاصيل إضافية» (restore card) — nothing is lost.

## C. Back / close
- System back: in steps 2–4 → previous step; in step 1 → back to the card; on the card → `onDismiss()`.
  (Use `BackHandler` inside the dialog; `DialogProperties(dismissOnBackPress = false)` so back is
  handled by us, never closes the app.)
- «إغلاق» and tapping outside the card → `onDismiss()` always, also when `isMandatory` (task 051 makes
  MainScreen honour it).

## Allowed files
core/license/LicenseDialog.kt only.

## Done when
Build + tests pass; first view is the small card; the stepper shows one step at a time; back never
leaves the app.
