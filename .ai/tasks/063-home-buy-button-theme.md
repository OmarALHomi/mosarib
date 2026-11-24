status: accepted
<!-- Owner request 2026-11-23. -->
# Task 063: «شراء» button on Home; quick buttons fit light and dark themes

File: `features/home/HomeScreen.kt` (simple-mode compact row and the full-mode quick actions).
1. Simple mode: the compact row becomes five buttons: سقي | قبض | صرف | شراء | صوت. «شراء» (icon
   like the purchase sheet's, info/blue tint) opens the well-owner purchase sheet exactly like the
   full-mode «شراء ساعات» button, through `LocalOperationGate`. Five equal buttons must fit 360dp
   (icon + one word, label may shrink to 12sp); no horizontal scroll.
2. Colours for both themes: every quick button's background and icon/text colour come from theme
   tokens (`MaterialTheme.mosarib.*Container` / `colorScheme`), not fixed light pastels or
   `.copy(alpha=…)` on light colours. In dark theme backgrounds are dark tinted containers with light
   icons/text and enough contrast (WCAG AA for the label). Check both themes in a Compose @Preview
   (light + dark) for the row.
Keep the actions, order of the existing buttons, guide targets and full-mode layout otherwise.

## Done when
Build + tests pass; simple Home shows five buttons; they look right in light and dark.
