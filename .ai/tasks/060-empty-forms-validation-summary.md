status: accepted
<!-- Owner request 2026-11-23. -->
# Task 060: Forms open empty, save validates like a web form; simpler summary words; small fixes

## 1. New irrigation and purchase forms open empty
`AddEditSessionBottomSheet` (SessionDialogs.kt) and `WellOwnerPurchaseBottomSheet`, **new** records only
(edit and voice prefill unchanged):
- «سعر الساعة» empty (placeholder «مثلاً 5,000» from the default price). When a customer with a saved
  custom price is picked, fill that price (as today).
- «من» / «إلى» show «اختر الوقت» (no preset now+1h), duration fields empty, summary card shows «—».

## 2. Save always enabled; validation like a web page
Pressing «حفظ» checks required fields in screen order: customer/owner, من, إلى (or duration), سعر
الساعة; voucher form: customer (for قبض), المبلغ. On the **first** missing field:
- scroll the form to it (`BringIntoViewRequester`), give it a red border and a small red line
  «هذا الحقل مطلوب», and focus it (text field gets focus + keyboard; customer picker opens its list;
  time box opens the time picker);
- do not save and do not open the confirmation sheet. The error clears as soon as the field is filled.
Existing range checks (1..MAX minutes, waste ≤ duration) use the same mechanism.

## 3. Operations summary words (`VouchersScreen.kt`)
The four tiles become, in this order: «مقبوض» (receipts — today's دخل), «صرف» (today's خرج),
«شراء» (sum of `WellOwnerPurchaseMath.payableAmount` of well-owner purchases in the period),
«متبقي» (receivables still owed to the owner — today's «لك», from `ledgerTotalsFor`, caption
«عند الناس الآن»). Under the grid one small line «عليك للآخرين: Y» (today's «عليك») and the
«ساعات السقي» line. Drop «الباقي في الصندوق».

## 4. Bug: «ساعات السقي» shows «ساعة وخمس دقائق» for all time
The total (≈1570 h) is lost by the words formatter. Show it as numbers: «1,570 ساعة و5 دقائق».

## 5. Purchase form: «+ عميل جديد» in the owner picker
Add an `onCreateCustomer` callback to `WellOwnerPurchaseBottomSheet` (like the voucher sheet) and wire
it from every caller (VouchersScreen, HomeScreen, CustomerDetailScreen). The new customer is created
as a well owner (`isWellOwner = true`) and becomes the selected owner.

## Allowed files
features/sessions/SessionDialogs.kt, features/wellowners/WellOwnerPurchaseBottomSheet.kt,
features/vouchers/VouchersScreen.kt, features/home/HomeScreen.kt, features/customers/CustomerDetailScreen.kt,
core/ui/MosaribTextField.kt / core/ui/CustomerPickerField.kt (only to add an `isError` / error-text
param), a small shared helper for "scroll to and focus first error" in core/ui if useful.

## Done when
Build + tests pass; new forms open empty; pressing save with a missing field jumps to it in red.
