status: accepted
<!-- Owner request 2026-11-22 after phone check. -->
# Task 053: Dashboard device limit honoured, smaller field hints, one تفقيط tooltip, Home in the middle

## 1. BUG: device limit from the dashboard is ignored after the trial was once used up
Phone prefs: `device_override_custom_limit` = 1783793664 (set by the owner in the dashboard),
`is_trial_consumed_permanently` = true, `cumulative_ops_count` = 4987, not activated.
`LicenseManager.canPerformOperation` returns false at `if (isTrialConsumed(context)) return false`
**before** looking at the custom device limit, so every new operation is blocked although the admin
raised the limit. `isTrialLimitExceeded` already checks the custom limit first — make
`canPerformOperation` (and `getRemainingOperations` if it has the same order) consistent:
- after the DEBUG and `isActivated` checks: if `RemoteConfigManager.getCustomDeviceLimit(context) > 0`
  → return `maxOf(storedOps, currentOperationsCount) < customLimit` (do not read or set the
  consumed flag in this branch);
- otherwise keep today's logic exactly.
Unit test with a fake/robolectric-free approach if the code allows; otherwise extract the decision
into a pure function `decideCanOperate(activated, customLimit, consumed, count, freeLimit)` and test it.
Do not change how licences/activation are validated or how the limit is synced.

## 2. Subscription stepper (`core/license/LicenseDialog.kt`)
Remove the «طريقة التحويل / المحفظة البنكية» dropdown from step «الدفع»: the list «حسابات التحويل
ومحافظ الدفع المعتمدة» already shows every account. Show that list **expanded** in the step, then the
optional transfer number. Where the request message used the selected method, write the account
list's first active method or omit the line — never block sending because no method is chosen.

## 3. Smaller hint text in fields (`core/ui/MosaribTextField.kt` and the autocomplete variant)
Placeholder and floating label use `labelMedium`/`bodySmall` (≈12–13sp) instead of the body size, so
long hints («رقم سند التحويل أو الإيداع (إن وجد)») no longer make fields tall. Value text unchanged.

## 4. One تفقيط tooltip everywhere
Use the existing `core/ui/AmountWithWords.kt` (RichTooltip with words + `SpeakButton`) for the Home
totals box (`features/home/HomeCustomerList.kt`) and the operations summary (`VouchersScreen.kt`),
replacing `AmountTooltip`. Delete `core/ui/AmountTooltip.kt` when unused.

## 5. Bottom bar: Home in the middle
Simple mode order (RTL, right → left): العمليات | الرئيسية | الإعدادات. Full mode already has Home in
the middle — keep it. App still opens on Home. (`MainScreen.kt` / `core/ui/MosaribNavBar.kt`.)

## 6. Voucher form wording (`VouchersScreen.kt`, `AddVoucherBottomSheet`)
Title «تسجيل سند (عليه)/(له)» → «تسجيل صرف» / «تسجيل قبض»; «المستفيد (عليه):» → «المستفيد»;
save button «حفظ وترحيل ال…» (cut) → «حفظ».

## 7. Voice button must be gated (`features/home/HomeScreen.kt`)
The Home «صوت» button (and the voice sheet's save/«فهمت» direct save) opens even when operations are
blocked. Wrap it with the same `LocalOperationGate` as سقي/قبض/صرف so it opens the subscription card.

## 8. صرف by voice with a new name → new customer not selected (`VouchersScreen.kt`)
Owner said «صرف مع احمد علي ساتر ونص 10000», the name is unknown, the form shows «المستفيد: مصروف عام».
- EXPENSE with a voice name that does not match: leave the beneficiary unselected (not «مصروف عام»)
  and show «لم أتعرّف على: <name> — اختر من القائمة أو أضفه» like the receipt form does.
- «+ عميل جديد» from the beneficiary menu opens the add-customer sheet pre-filled with that name, and
  after saving, the new customer is **selected as the beneficiary** (same for the receipt picker:
  check it selects the new customer too).

## Allowed files
core/license/LicenseManager.kt (item 1 only), a new test, core/license/LicenseDialog.kt,
core/ui/MosaribTextField.kt, core/ui/AmountWithWords.kt (only if a size param is needed),
core/ui/AmountTooltip.kt (delete), features/home/HomeCustomerList.kt, features/vouchers/VouchersScreen.kt,
MainScreen.kt, core/ui/MosaribNavBar.kt, features/home/HomeScreen.kt (item 7).

## Done when
Build + tests pass; with the phone prefs above, `canPerformOperation` returns true.
