# Review 026, round 1 (Claude, 2026-11-02, code review; the phone was not connected)

Build and tests pass (`EXIT=0`). The redesign of the three detail viewers follows the spec:
- header with the type colour and a ⋮ menu that holds حذف;
- amount block with the exact words;
- key–value rows;
- footer with سداد as the primary button for unpaid sessions.

The "+ مجموعة" chip is there. The fixes below are required.

## Required
1. **The irrigation snapshot misses fields** (`SessionDialogs.kt`, `SessionFormSnapshot`). Today
   a change to any of these closes the form without asking, so the user loses the edit. Add them
   to the snapshot:
   - `pricePerHourStr`, through `parseAmountInput`;
   - the **start time** (`startCal.timeInMillis`, rounded to the minute). Moving the start and
     the end together keeps the same `grossMinutes`, but it is still a change;
   - `quickName`, `quickPhone` and `quickIsWellOwner` (quick add of a new customer).
   - Add unit tests for these.
2. **Preselected customer:** when a customer is preselected, the snapshot must not depend on
   `customerInputText`. That text is keyed on `selectedCustomer?.name` and can fill in after the
   first frame. Rule:
   - when `selectedCustomerId > 0`, compare the id only;
   - compare the typed text only when no customer is selected.
3. **Back button:** `shouldDismissOnBackPress = false` is set. The `BackHandler` that is already
   there is in the **parent** composition, outside the sheet's window, so it may never get the
   back press. Then back would do nothing at all. Move the `BackHandler` **inside** the sheet
   content (inside `ModalBottomSheet { … }`), in both `MosaribBottomSheet` and
   `MosaribBottomSheetLazy`. Test on the device:
   - back with no changes closes the sheet;
   - back with changes shows the dialog at once.
4. **The device check is missing.** When the phone is connected, record frames during a close
   with changes (`screencap` is too slow on this phone, several seconds per frame; use
   `adb exec-out "screenrecord --output-format=frames --size 180x400 -"`: each frame is a 20-byte
   header + raw RGB 180×400×3) for the irrigation and
   voucher forms. Write the result in the report.

## Claude will check on the device after the fix
- Every form closes untouched with no question.
- The three viewers in light and dark themes.
- The "+ مجموعة" chip.

---
## Device check (Claude, 2026-11-02 23:15, Samsung R5CR812MCHE, the task/026 build)
I recorded raw frames at ~30 fps (`screenrecord --output-format=frames`) during the close of a
new irrigation form with text typed into the customer field.

- **Untouched form + X:** closes at once with no question. ✓
- **Untouched form + the phone's back button:** **nothing happens**; the form stays open. ✗ (this
  confirms item 3; the fix is required)
- **Changed form + X:** ✗ **the flicker is still there.** Frame 1.23s: the sheet **disappears in
  one frame** (no hide animation), then the dialog appears, then from 1.47s to 1.66s the sheet
  **slides up again from the bottom** behind the dialog.
- **"تجاهل":** ✗ the dialog closes, the sheet disappears again (3.30s), slides all the way up again
  (3.39 to 3.61s), and only then closes. This is exactly what the owner reported.
- Reading of the frames: the sheet is **re-created** (leaves the composition and comes back with
  its opening animation) every time the discard dialog appears or closes. Guards on
  `hide()` do not help, because no `hide()` is causing it.

### New required item 5: find the real cause of the re-creation
- Add a temporary log in `MosaribBottomSheet`
  (`DisposableEffect(Unit) { Log.d("SheetDbg","enter"); onDispose { Log.d("SheetDbg","leave") } }`)
  and a log in the `LaunchedEffect` that shows the sheet. Open a form, change it, press X, and
  read logcat. If "leave/enter" appears, look at what removes it:
  - the call site (`if (showX) …`): does it change when the dialog opens? Is there a `key(...)` or
    a state that changes because of the dialog?
  - `confirmValueChange` returning `false` after a snap: check `sheetState.currentValue` when X is
    pressed.
- Experiment: show `MosaribDialog` (the discard confirmation) from **inside** the sheet content
  (inside `ModalBottomSheet { … }`), not as a sibling before it.
- Remove the temporary logs after the fix. In the report, write the cause you found and the
  result of the device check.

### Detail viewers (session #283 viewer, light)
- The layout is good and matches the spec: header, amount block with the exact words, rows, the
  البيان box, and the footer إرسال / PDF / تعديل. ✓
- ✗ **item 6:** the subtitle is cut: "الجمعة، 30 أكتوبر 202…". The time is missing because the
  status chip shares the line. Put the chip on its own line under the subtitle, as the spec says,
  so the full date and time show.
- ✗ **item 7:** the **linked payments** do not show in the viewer, although the ledger card for
  the same session shows "السداد (1)". Check that `linkedVouchers` reaches the viewer from every
  call site (operations screen and customer page). If the customer-page call site is in
  `CustomerDetailScreen.kt`, which is outside this task, report that and do not edit it.
- "+ مجموعة" chip: shows at the end of the row and opens "مجموعة جديدة". ✓

---
## Round 2: device check (Claude, 2026-11-03 00:05): accepted
- Back on an untouched form closes it. ✓
- Raw-frame recording of a changed irrigation form:
  - X: the dialog fades in and the sheet does not move;
  - "متابعة التعديل": the sheet does not move;
  - X again: same;
  - "تجاهل": one slide down, and the sheet never comes back. ✓
- Untouched forms close with no question: new irrigation, receipt, expense, purchase,
  irrigation from the customer page (customer preselected), and editing an existing voucher. ✓
- Session #369 viewer: the full date and time show; the linked payments show. ✓
- Small leftover (moved to task 027): the 750 **discount** voucher is listed as "سداد · 750" in
  the linked payments (the label must follow the voucher type: خصم / قبض). The ledger card says
  "السداد (3)" while the viewer lists 2. Make the counts match.
- Not checked: dark theme (the code uses tokens only).
