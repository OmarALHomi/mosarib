status: accepted
<!-- Owner request 2026-11-23: with only a duration, «من» stays at the recording time and «إلى» moves. -->
# Task 062: Duration-only rule — من = recording time, إلى = من + duration

Files: `features/sessions/SessionDialogs.kt`, `features/wellowners/WellOwnerPurchaseBottomSheet.kt`,
`core/ui/FormValidation.kt` / the pure helper from task 061 (`sessionStartFromDuration` or similar) and
their tests.

Task 061 made «من» = now − duration and «إلى» = now. The owner wants the opposite:
- Duration typed, من/إلى not picked by the user → «من» = the moment the duration was first typed
  (recording time, fixed; it does not move while the user keeps changing the duration) and
  «إلى» = «من» + duration (date rolls over midnight correctly). Both still shown as «تلقائي».
- Changing the duration again only moves «إلى».
- Clearing the duration still resets both to «اختر الوقت» (061 behaviour), and the next typed duration
  takes a new «من» = now.
- User-picked من/إلى, edit mode and voice: unchanged.
Update the pure helper and its unit tests (including past-midnight).

## Done when
Build + tests pass; typing 3 h at 10:00 shows من 10:00, إلى 13:00; changing to 5 h shows إلى 15:00
and من still 10:00.
