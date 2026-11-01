status: accepted

# Task 003: Design-system foundation (semantic colours, shapes, spacing, type floor)

## Background (verified by Claude in the code)
Screens bypass the theme: ~900 hard-coded `Color(0x…)` values, 27 different literal font sizes
(8sp–34sp, including 8.5/9/9.5), 15 different corner radii, and 44 manual `isAppDark()` checks
that pick a light and a dark colour by hand. The theme has no semantic colours for
success / warning / info / "له" / "عليه", so every screen invents its own (e.g. six different reds).
This task only builds the foundation. Screens are migrated to it in later tasks.

## Required behaviour
1. **Extended semantic colours** in a new file `ui/theme/MosaribColors.kt`:
   ```kotlin
   @Immutable
   data class MosaribExtendedColors(
       val success: Color, val onSuccess: Color, val successContainer: Color, val onSuccessContainer: Color,
       val warning: Color, val onWarning: Color, val warningContainer: Color, val onWarningContainer: Color,
       val info: Color, val onInfo: Color, val infoContainer: Color, val onInfoContainer: Color,
       val debt: Color,      // money the customer owes us ("عليه")
       val credit: Color,    // money we owe / customer credit ("له")
       val settled: Color,   // balance zero
       val textMuted: Color  // secondary hints (replaces 0xFF94A3B8 / 0xFF64748B)
   )
   ```
   Provide a light and a dark instance. Use these values (they are the ones the app already
   uses most, so screens will not change colour when migrated):
   | token | light | dark |
   |---|---|---|
   | success / container | `0xFF2E7D32` / `0xFFE8F5E9` | `0xFF4ADE80` / `0xFF132A1C` |
   | warning / container | `0xFFD97706` / `0xFFFFF3E0` | `0xFFFBBF24` / `0xFF2E2210` |
   | info / container | `0xFF0284C7` / `0xFFE0F2FE` | `0xFF38BDF8` / `0xFF0C2433` |
   | debt | `0xFFE53935` | `0xFFF87171` |
   | credit | `0xFF2E7D32` | `0xFF4ADE80` |
   | settled | `0xFF64748B` | `0xFF94A3B8` |
   | textMuted | `0xFF64748B` | `0xFF94A3B8` |
   `on*` colours: white on the strong colour in light mode, `0xFF0A1014` on the strong colour in
   dark mode; `onXContainer` = the strong colour of the same mode.
   Expose it with a `staticCompositionLocalOf` and an accessor
   `val MaterialTheme.mosarib: MosaribExtendedColors @Composable @ReadOnlyComposable get()`.
   Provide it inside `MosaribTheme` (choose the instance with the same `darkTheme` flag).
2. **Shapes**: add `val MosaribShapes = Shapes(extraSmall = 6.dp, small = 8.dp, medium = 12.dp,
   large = 16.dp, extraLarge = 24.dp)` (RoundedCornerShape) in `ui/theme/Shape.kt` and pass it
   to `MaterialTheme(shapes = …)`.
3. **Spacing**: `object Spacing { val xxs = 2.dp; val xs = 4.dp; val s = 8.dp; val m = 12.dp;
   val l = 16.dp; val xl = 24.dp; val xxl = 32.dp }` in `ui/theme/Spacing.kt`.
4. **Type floor**: in `Type.kt` change `labelSmall` to `fontSize = 11.sp, lineHeight = 16.sp`
   (it is 10sp today). Leave every other style as it is.
5. Add a short KDoc at the top of `MosaribColors.kt` (in Arabic or English) that tells future
   code: use `MaterialTheme.colorScheme` / `MaterialTheme.mosarib` instead of `Color(0x…)` and
   `isAppDark()`, and use typography styles instead of literal `fontSize`.

## Allowed files
- `app/src/main/java/com/example/ui/theme/Theme.kt`
- `app/src/main/java/com/example/ui/theme/Type.kt`
- new: `ui/theme/MosaribColors.kt`, `ui/theme/Shape.kt`, `ui/theme/Spacing.kt`

## Out of scope
- Do not touch any screen or component. Do not delete `isAppDark()` or any existing colour
  constant in `Color.kt` (screens still use them).

## Done when
- Build and tests pass.
- The report lists the final token table (light and dark) exactly as implemented.

---
## Review notes (written by Claude)
**2026-11-01 — accepted.** Matches the spec exactly. Build and tests pass (re-run by Claude, while
task 004 was in progress in the same tree).
- Claude fixed one word in the KDoc: the app name was written "مصارف" instead of "مُسَرِّب".
- Note: passing `MosaribShapes` also changes Material defaults app-wide. `extraSmall` goes 4→6dp
  (text fields, menus) and `extraLarge` goes 28→24dp (dialogs, bottom sheets). This is intended.
  Check it on the device after wave 1.
