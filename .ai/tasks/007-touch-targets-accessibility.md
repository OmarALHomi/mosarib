status: accepted

# Task 007: Touch targets and TalkBack labels on the customer list

## Background (verified by Claude in the code)
- `features/customers/CustomerCardItem.kt`: the four card actions (اتصال، إرسال كشف، كشف حساب PDF،
  خيارات) are 30dp boxes with 15dp icons (~line 253). The minimum touch target is 48dp, so users
  hit the neighbouring action.
- Across the app 340 icons use `contentDescription = null`; some of them are the only content of
  a clickable element, so TalkBack announces just "button".

## Required behaviour
1. In `CustomerCardItem.kt` keep the 30dp visual circle and 15dp icon, but give every action a
   48dp touch target: apply `Modifier.minimumInteractiveComponentSize()` (or wrap in `IconButton`)
   **before** the visual `size(30.dp)`, and make sure the row spacing does not overlap targets.
   Ensure each action has `role = Role.Button` and a content description
   ("اتصال", "إرسال كشف", "كشف حساب PDF", "خيارات" — the existing strings).
2. In `CustomerCardItem.kt`, `CustomersScreenComponents.kt` and `CustomersFilterRow.kt`: for every
   clickable element whose only child is an `Icon` with `contentDescription = null`, add a short
   Arabic description that says what the click does. Decorative icons next to a text label keep
   `null`. List every description you added in the report.
3. In `CustomerCardItem.kt` replace the literal font sizes 10sp / 11sp with
   `MaterialTheme.typography.labelSmall` (11sp after task 003); keep the larger amount text as is.

## Depends on
- Task 003 (labelSmall = 11sp).

## Allowed files
- `app/src/main/java/com/example/features/customers/CustomerCardItem.kt`
- `app/src/main/java/com/example/features/customers/CustomersScreenComponents.kt`
- `app/src/main/java/com/example/features/customers/CustomersFilterRow.kt`

## Out of scope
- No colour, layout or behaviour changes beyond the above.

## Done when
- Build and tests pass.

---
## Review notes (written by Claude)
**2026-11-01 — accepted with a fix by Claude.**
- Four 48dp targets made the action row 60dp wider, leaving about 80dp for the customer name on a
  360dp phone. Claude changed the outer box to `size(40.dp)` + `clip(RoundedCornerShape(10.dp))`.
  Compose already extends a pointer hit area to 48dp where there is room. The clip also bounds the
  ripple, which was a square.
- Check on the device: name truncation and taps between neighbouring actions.
