status: accepted
<!-- Owner request 2026-11-24. -->
# Task 068: Purchase price empty; customer form = basics + «المزيد»; big «استيراد من جهات الاتصال»

1. **Purchase form price** (`features/wellowners/WellOwnerPurchaseBottomSheet.kt`): a **new** purchase
   opens with «سعر الساعة» showing 4,500 (filled from somewhere: last purchase / owner / default).
   It must open empty with the placeholder, like the irrigation form (task 060). Fill a price only
   when the user picks an owner who has a saved purchase price, if such a field exists; otherwise
   leave it empty. Edit mode and voice unchanged.
2. **Add/edit customer sheet** (`features/customers/AddEditCustomerDialog.kt`,
   `AddEditCustomerBottomSheet`), both modes, add and edit:
   - At the very top a large full-width tonal button «استيراد من جهات الاتصال» (contacts icon) that
     runs the existing pick-contact action (fills name + phone). Remove the small old contact icon.
   - Visible basics: الاسم، رقم الهاتف، the switch «هل هذا مورد (صاحب بئر)؟».
   - Everything else (farm/location, custom hour price, group, notes, any other field) inside a
     collapsed «المزيد» section; it opens automatically when editing a customer who has any of those
     values filled.
   - Save button and validation unchanged.
3. **From the purchase form** «+ عميل جديد» must open this same compact sheet (not a big screen),
   with the «مورد» switch already ON and the spoken/typed name prefilled (`initialName`), and select
   the new owner after saving (task 060 wiring). Add an `initialIsWellOwner: Boolean = false`
   parameter if needed and pass it from `WellOwnerPurchaseBottomSheet`'s callers.
4. Every other place that adds a customer (Home, customers screen, session/voucher pickers) uses the
   same compact sheet automatically through item 2.

## Allowed files
features/customers/AddEditCustomerDialog.kt, features/wellowners/WellOwnerPurchaseBottomSheet.kt,
the callers that open the add-customer sheet from the purchase form (VouchersScreen.kt, HomeScreen.kt,
CustomerDetailScreen.kt) only for the new parameter.

## Done when
Build + tests pass; new purchase shows an empty price; add-customer shows the big import button, the
three basics and a collapsed «المزيد»; from the purchase form the مورد switch is on.
