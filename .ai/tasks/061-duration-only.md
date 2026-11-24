status: accepted
<!-- Owner request 2026-11-23 after phone test of 060. -->
# Task 061: Duration opens empty; duration alone is enough to save

Files: `features/sessions/SessionDialogs.kt` (AddEditSessionBottomSheet), `features/wellowners/WellOwnerPurchaseBottomSheet.kt`,
`core/ui/FormValidation.kt` (+ its test).

1. **New** forms: the «المدة» hours and minutes fields open **empty** (today they show «1» and «0»
   while من/إلى are empty). Edit mode and voice prefill unchanged.
2. Required time rule: either (من and إلى) **or** a duration > 0.
   - Duration given, من/إلى not chosen → valid. On save use the **recording time**: إلى = now,
     من = now − duration (the irrigation was just finished). Show this in the form as soon as the
     duration is typed (من/إلى boxes fill with those times in a lighter style, labelled «تلقائي»), so
     the confirmation statement and the saved record use the same times.
   - Only من chosen + duration → إلى = من + duration (as today).
   - Nothing given → the validation error goes to the duration row («أدخل المدة أو اختر وقت البدء
     والانتهاء»), not to the «من» box.
   Update `firstMissingField` and its unit tests for these cases.
3. Keep every calculation, the confirmation sheet and the post-save dialog unchanged.

## Done when
Build + tests pass; a new form shows empty duration; typing only «2» hours and saving works with
من = now − 2h, إلى = now.

## 4. Zero must not stick to typed numbers (owner, 2026-11-23)
Typing «4» in a field showing «0» gives «40». In `CompactDurationField` and every number/amount field
of these forms (المدة ساعات/دقائق، الواصل، سعر الساعة، الهدر): an empty value shows «0» only as a grey
**placeholder**, never as real text; and when a field that holds the value 0 gets focus, its text is
cleared (or fully selected) so the first digit replaces it. If the shared text field
(`core/ui/MosaribTextField.kt`) is the right place, fix it there once (allowed for this item).
