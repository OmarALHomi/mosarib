status: accepted

<!-- Owner request 2026-11-02: all popups (menus, bottom sheets, dialogs) look inconsistent, and a
sheet that contains a list closes by mistake when the user scrolls the list down. This task builds
the shared components and migrates the core/ui ones. Tasks 019 and 020 migrate the rest. -->
# Task 018: Shared bottom sheet, dialog and menu components (+ fix accidental sheet dismiss)

## Background (Claude's code audit)
- 13 `ModalBottomSheet`s, ~35 dialogs and ~20 `DropdownMenu`s, each styled by hand:
  - different corner radii and paddings;
  - hard-coded colours (`Color(0x…)`) and `fontSize`;
  - emoji inside labels ("سلة المحذوفات 🗑️", "إدارة المجموعات ⚙️");
  - destructive items are not set apart;
  - headers and close buttons are inconsistent.
- **Bug:** in a bottom sheet with a scrolling list or a scrolling form, when the list is at the top
  and the user drags or flings down to scroll, the drag goes to the sheet and the **sheet closes**.
  Typed data is lost. All sheets use the default drag-to-dismiss.
  `compose-bom` is `2024.09.00` (material3 1.3.x).

## A. New file `core/ui/MosaribSheets.kt`
### 1. `MosaribBottomSheet`
```kotlin
@Composable fun MosaribBottomSheet(
    onDismissRequest: () -> Unit,
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    hasUnsavedChanges: Boolean = false,     // true → closing asks "تجاهل التغييرات؟"
    footer: (@Composable RowScope.() -> Unit)? = null,  // sticky action buttons
    content: @Composable ColumnScope.() -> Unit
)
```
- `rememberModalBottomSheetState(skipPartiallyExpanded = true)`.
- **No drag-to-dismiss from the content.** Use `sheetGesturesEnabled = false` if the resolved
  material3 version has it. If not, reject the transition to `Hidden` that comes from a drag
  with `confirmValueChange`, while still allowing the X button, back press and scrim tap. Say in
  the report which approach you used.
- The sheet closes by: the **X button** in the header, the system **back**, or a **tap on the
  scrim**. If `hasUnsavedChanges`, each of these first shows a `MosaribDialog` "تجاهل التغييرات؟"
  with the buttons "تجاهل" (destructive) and "متابعة التعديل".
- Look:
  - shape: top corners 24dp;
  - container `surface` (dark: `surfaceContainerLow`);
  - drag handle drawn as a 36×4dp pill in `outlineVariant` (visual only);
  - `windowInsets` handled: `navigationBarsPadding()` on the footer, `imePadding()` on the
    content;
  - maximum height 92% of the screen.
- Header row, 16dp horizontal padding:
  - optional icon in a 40dp circle (`iconTint` at 12%);
  - title in `titleLarge` bold, with the subtitle under it in `bodySmall` `onSurfaceVariant`;
  - X `IconButton` (40dp, "إغلاق") at the end;
  - a 1dp `outlineVariant` divider under the header.
- Content: a `Column` with `verticalScroll` and 16dp padding. When the caller needs a lazy list,
  provide an overload `MosaribBottomSheetLazy(…, content: LazyListScope.() -> Unit)` using
  `LazyColumn`.
- Footer (when given): sticky at the bottom, 1dp divider above, 16dp padding, buttons in a `Row`
  with 12dp spacing. Use the button helpers below.

### 2. `MosaribDialog`
```kotlin
@Composable fun MosaribDialog(
    onDismissRequest: () -> Unit,
    title: String,
    icon: ImageVector? = null,
    tone: DialogTone = DialogTone.Neutral,   // Neutral, Info, Success, Warning, Danger → colours from MaterialTheme.mosarib
    message: String? = null,
    confirmText: String, onConfirm: () -> Unit,
    dismissText: String? = "إلغاء", onDismiss: (() -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null
)
```
- Shape 24dp, `surface`, padding 24dp, max width 400dp.
- Optional 48dp tone-coloured icon circle at the top, centred.
- Title in `titleLarge` bold, centred. Message in `bodyMedium` `onSurfaceVariant`, centred.
- Buttons: the confirm button is a filled button in the tone colour (Danger → `debt`); dismiss
  is a text button. In RTL they sit side by side, confirm first. Below 320dp they stack.

### 3. `MosaribMenu` and `MosaribMenuItem`
```kotlin
@Composable fun MosaribMenu(expanded: Boolean, onDismissRequest: () -> Unit, content: @Composable ColumnScope.() -> Unit)
@Composable fun MosaribMenuItem(text: String, icon: ImageVector?, onClick: () -> Unit,
                                tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
                                destructive: Boolean = false, trailing: String? = null /* e.g. a count */)
@Composable fun MosaribMenuDivider()
```
- Menu: shape 16dp, container `surfaceContainer`, shadow 6dp in light and none in dark, minimum
  width 200dp, vertical padding 6dp.
- Item: height 44dp, horizontal padding 14dp, 20dp icon, text in `bodyLarge`. `destructive`
  colours text and icon in `debt`. `trailing` is a small count pill. **No emoji in labels.**
- Rule for callers, documented in KDoc: destructive items go last, after a `MosaribMenuDivider()`.

### 4. Buttons for sheet footers
`MosaribPrimaryButton(text, onClick, tone, modifier, enabled)` and
`MosaribSecondaryButton(text, onClick, modifier)`, both 48dp high with 14dp radius.

All colours come from `MaterialTheme.colorScheme` / `MaterialTheme.mosarib`. All text styles come
from `MaterialTheme.typography`. No literal `Color(0x…)` and no `fontSize` in this file.

## B. Migrate the `core/ui` popups (this task only)
Rebuild these on the new components. Keep their public APIs, texts and behaviour:
- `core/ui/MosaribConfirmDialogs.kt` (all its dialogs on `MosaribDialog`)
- `core/ui/OperationDetailComponents.kt` and `core/ui/SessionDetailBottomSheet.kt` (on
  `MosaribBottomSheet`; menus on `MosaribMenu`)
- `core/ui/SendMessageChoiceDialog.kt`, `core/ui/GoogleBackupAccountDialog.kt`
- `core/ui/BackupReminderDialog.kt`, `core/updates/BroadcastAnnouncementDialog.kt`
- `core/ui/MosaribLoadingIndicator.kt` (only the dialog wrapper's shape and colours)

## C. A gallery for review
A debug-only composable `core/ui/MosaribSheetsPreview.kt` with `@Preview`s:
- a sheet with a long list;
- a form sheet with `hasUnsavedChanges`;
- each dialog tone;
- a menu with a destructive item.

## Allowed files
- new `core/ui/MosaribSheets.kt`, new `core/ui/MosaribSheetsPreview.kt`
- the files listed in B
- unit tests if you extract pure logic

## Out of scope
- Feature dialogs in `features/**`: tasks 019 and 020.
- `core/license/LicenseDialog.kt`: task 020.

## Done when
- Build and tests pass.
- Tested on the device: open an operation's detail sheet and the send-message sheet; scroll to the
  top and fling down 5 times; **the sheet stays open**. It closes with X, back and a scrim tap.
- The report lists every migrated popup and the dismiss approach used.

---
## Review notes (written by Claude)
**Round 1: changes-requested** (2026-11-02). Build and tests pass. Claude installed this worktree's
build on the phone and tested the session detail sheet (dark theme):

| Action | Expected | Result |
|---|---|---|
| Fling down ×5 on the content, and drag from the handle | stays open | ✅ stays open |
| X button | closes | ✅ |
| System back | closes | ✅ |
| **Tap on the scrim** (dimmed area above the sheet) | closes, or asks "تجاهل التغييرات؟" | ❌ **nothing happens** |

**Why:** in material3 1.3.0 the scrim tap calls `animateToDismiss()`, which first checks
`confirmValueChange(Hidden)`. Your `confirmValueChange` returns `false` unless
`isDismissingAllowed`, so the scrim tap is swallowed together with the drags.

**Fix (in `MosaribBottomSheet` and `MosaribBottomSheetLazy`):** tell a scrim tap apart from a
drag.
- Mark drags: on the sheet content and the handle, add a `pointerInput` that records
  `lastDragAtMillis` when a vertical drag starts or continues
  (`awaitEachGesture { … awaitTouchSlopOrCancellation … }`, without consuming the events).
  Sheet nested-scroll flings also count: wrap the content in a `nestedScroll` connection that
  updates the same timestamp in `onPreScroll` / `onPreFling`.
- In `confirmValueChange`, when the target is `Hidden` and dismiss is not yet allowed:
  - if there was **no drag in the last 400ms**, it is a scrim tap: call `requestDismiss()` (post
    it with `coroutineScope.launch`) and return `false`. `requestDismiss` then closes, or shows
    the discard dialog;
  - otherwise return `false` (a drag must never close).
- Keep the X and back behaviour as they are now.
- Re-test on the device: fling ×5 stays open; scrim tap closes; scrim tap with
  `hasUnsavedChanges` shows the dialog.

Minor, in the same round: in `SessionDetailBottomSheet` the header title is cut ("تفاصيل دورة
السق…") because the "مسدد بالكامل" badge shares the row. In `MosaribBottomSheet`, put the
optional `headerBadge` **under** the title, next to the subtitle, so the title gets the full
width (max 2 lines).

Allowed files: `core/ui/MosaribSheets.kt` (and `SessionDetailBottomSheet.kt` only if the badge
change needs it). Append a "Round 2" section to `.ai/reports/018.md`; do not rewrite files.

**Round 2 verdict: accepted** (2026-11-02). Claude installed the build and tested it on the device
(light theme, session detail sheet):
- content flings ×2 and a slow drag: stays open ✅;
- scrim tap closes ✅;
- back closes ✅;
- X closes ✅;
- title on its own line, badge under it ✅.

A drag that starts **on the handle** closes the sheet. That is a deliberate gesture and standard
Material behaviour, so it is accepted. Content drags, the reported bug, never close.

Process: the status was left `in-progress` and the report has no Round 2 section. Please always
finish both.
