status: accepted
<!-- Owner request 2026-11-22. Depends on 042, 043, 044 (CustomerPickerField) being merged first. -->
# Task 045: Voucher form — قبض / صرف wording, pick-only customer, add new, simple mode

## Changes (`features/vouchers/VouchersScreen.kt`, `AddEditVoucherBottomSheet`)
1. Type toggle (~2448–2480): labels «له» / «عليه» become «قبض» / «صرف». Same colours/behaviour.
2. RECEIPT customer: replace `MosaribAutoCompleteTextField` (~2488) with `CustomerPickerField`
   (from 044). «+ عميل جديد» opens the existing `AddEditCustomerBottomSheet` / quick add used by the
   session form; add an `onCreateCustomer: suspend (Customer) -> Long` param to the sheet if needed
   and wire it from its callers (`VouchersScreen`, `HomeScreen`) the same way `AddEditSessionBottomSheet`
   gets it. `LockedVoucherCustomer` stays when the customer is locked.
3. EXPENSE beneficiary picker (~2765): keep, but add «+ عميل جديد» as its first item the same way.
4. Simple mode (`LocalSimpleMode.current`): show only type, customer/beneficiary, amount and the save
   button; date, البيان/notes, frequent-description chips, payment method and discount go into a
   collapsed «خيارات إضافية» card (auto-open if any of them has a value). Full mode unchanged apart
   from 1–3.

## Allowed files
features/vouchers/VouchersScreen.kt, features/home/HomeScreen.kt (only the wiring for
onCreateCustomer), core/ui/CustomerPickerField.kt (only if a small param is missing),
core/ui/guide/GuideRegistry.kt (only to fix moved targets)

## Done when
Build + tests pass; the receipt customer box cannot be typed in; a new customer can be added from the
voucher form; simple mode shows type + customer + amount + save.
