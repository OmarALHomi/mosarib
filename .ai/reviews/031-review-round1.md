# Review 031, round 1 (Claude, 2026-11-03 13:35, combined build 029+030+031)

Build and tests pass.
- **Force update:** `shouldForceUpdate(current, min)` = `current < min`. The flag alone never
  blocks, and the minimum is cached for offline use. Correct. ✓
- **About page:** "الإصدار 1.3.0 (بناء 4)" from `BuildConfig`, the license chip, the device code,
  the developer card, links including mosarib.tubbasoft.com, and a dynamic year. ✓

## Required (small)
1. **The force-update cache must follow the dashboard.** Today the cache is written only when
   `minRequiredVersionCode > 1`. If the owner cancels a force update (the broadcast goes back to
   min 1, or is removed), the old minimum stays cached, and offline users of an old build stay
   blocked. Write the value that was read **every time** a broadcast is fetched (including 1).
   Clear the cache when `/app_broadcast` is empty. Add a test.
2. **About page:**
   - add the row "الإدخال الصوتي للعمليات" (task 030 is now in the build);
   - the row "النسخ الاحتياطي المحلي وGoogle Drive…" renders the و after "Drive". Write "النسخ
     الاحتياطي على الهاتف وعلى Google Drive، ويعمل بدون إنترنت", and isolate the Latin words
     with `\u2068…\u2069`.
3. The owner will check the mandatory screen exit, the force-update screen and the message date
   lines during the final full test.
