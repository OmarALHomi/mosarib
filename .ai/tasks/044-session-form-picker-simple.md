status: accepted
<!-- Owner request 2026-11-22. Depends on 042 (LocalSimpleMode) — merged before this starts. -->
# Task 044: Session form — pick-only customer list, owner switch, billed-to in extras, simple mode

## Changes
1. New shared composable `core/ui/CustomerPickerField.kt`:
   - A **read-only** field (no typing in the box) showing the selected customer name, or
     "اختر العميل..." when none. Tap opens a `MosaribMenu` (same pattern as the expense beneficiary
     picker in `VouchersScreen.kt` ~2765–2800) listing customers, with a small search field **inside**
     the menu (long lists), and a first item «+ عميل جديد».
   - Params roughly: `customers`, `selected: Customer?`, `onSelect(Customer)`, `onAddNew()`,
     `modifier`, `label`. Show the well-owner icon (Park) vs farmer (WaterDrop) like the voucher field.
2. `AddEditSessionBottomSheet` (`features/sessions/SessionDialogs.kt`):
   - Replace `MosaribAutoCompleteTextField` (~946) with `CustomerPickerField`. «+ عميل جديد» opens the
     existing quick customer dialog (`showQuickCustomerDialog`, ~2342).
   - Voice: if the voice name matches confidently, preselect as now. If not, leave nothing selected and
     show a small line under the field: "لم أتعرّف على: <name> — اختر من القائمة أو أضفه"; the quick
     customer dialog opens pre-filled with that name when the user taps «+ عميل جديد».
   - Quick customer dialog: replace "نوع التسجيل:" with the two chips (~2426–2470) by one
     Switch row «هل هذا مورد (صاحب بئر)؟» default off → `quickIsWellOwner`.
   - Move the "احتساب السقي على صاحب بئر" card (~1036–1150, switch + owner menu) and the
     "حساب صاحب بئر" note into the existing «خيارات إضافية» card (~1618), in both modes. Rename the
     card title to «خيارات إضافية» and keep it auto-open when any extra value is set
     (waste, discount, billed-to, notes).
   - Notes field: inside «خيارات إضافية» too.
   - Simple mode (`LocalSimpleMode.current`, from task 042): hide the time-mode tabs and show only the
     «اختيار» content (start + end date/time); full mode keeps both tabs. Start and end time are shown
     in both modes.
3. `features/customers/AddEditCustomerDialog.kt` (~312–335): replace the two `PersonaTypeCard`s with
   the same Switch row «هل هذا مورد (صاحب بئر)؟» bound to `isWellOwner`.
4. Remove now-unused code/imports. Leave `SettleSessionDialog` as it is unless no caller is left in
   your tree, then delete it.

## Allowed files
core/ui/CustomerPickerField.kt (new), features/sessions/SessionDialogs.kt,
features/customers/AddEditCustomerDialog.kt, core/ui/guide/GuideRegistry.kt (only to fix moved targets)

## Done when
Build + tests pass; the customer box cannot be typed in; new customer works from the list;
billed-to-owner lives in خيارات إضافية; simple mode shows customer, start/end, price, amount received,
then the collapsed extras.
