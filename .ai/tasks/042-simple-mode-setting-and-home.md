status: accepted
<!-- follow-up: GuideTexts.kt home tour text still says «سند جديد»; target now on «قبض». -->
<!-- Owner request 2026-11-22: users find the app complicated. Simple / full mode. -->
# Task 042: "Simple mode" setting + home buttons سقي / قبض / صرف

## Goal
A setting "طريقة الاستخدام": **مبسّطة** or **كاملة**. Other tasks (044, 045) read it to hide
rarely-used fields. This task adds the setting, a CompositionLocal to read it, and simplifies Home.

## Changes
1. Setting (follow the `biometric_enabled` pattern in `features/settings/SettingsRepository.kt`):
   - key `ui_simple_mode`, field `AppConfig.simpleMode: Boolean`, missing key = `false`
     (existing users keep the full app), plus `updateSimpleMode(Boolean)` in the repository and
     `SettingsViewModel`.
   - New users: when `OnboardingScreen` finishes (`MainScreen.kt` ~326, `onFinish`), save
     `ui_simple_mode = true`. Do not change the onboarding screen itself.
2. `core/ui/SimpleMode.kt` (new): `val LocalSimpleMode = staticCompositionLocalOf { false }`.
   Provide it in `MainScreen.kt` inside `MosaribTheme` from `appConfig.simpleMode`
   (collect the same flow the screen already uses for the theme, or `SettingsViewModel.appConfig`).
3. Settings screen: one row near the top, title "طريقة الاستخدام", subtitle
   "المبسّطة تُظهر الحقول الأساسية فقط، والباقي تحت «خيارات إضافية»", with two choices
   مبسّطة / كاملة (same look as the existing theme choice row).
4. Home quick actions (`features/home/HomeScreen.kt` ~531–630):
   - Replace the single «سند جديد» (قبض أو صرف) button with two buttons:
     «قبض» (subtitle "استلمت مبلغاً", opens the voucher sheet with `VoucherType.RECEIPT`) and
     «صرف» (subtitle "دفعت مبلغاً", opens it with `VoucherType.EXPENSE`). Both modes.
   - Simple mode (`LocalSimpleMode.current`): show only سقي جديد، قبض، صرف، إدخال صوتي — bigger
     buttons are fine. Hide حسابات العملاء، شراء ساعات، عميل جديد، النسخ الاحتياطي (the bottom bar
     still reaches customers/settings).
   - Full mode: all current buttons, with «سند جديد» replaced as above.
   - Keep the guide targets working: if a tour step points at the removed «سند جديد» target, point it
     at «قبض» instead (`core/ui/guide/GuideRegistry.kt`).

## Allowed files
- features/settings/SettingsRepository.kt, AppSetting*.kt (only if needed), SettingsViewModel.kt, SettingsScreen.kt
- core/ui/SimpleMode.kt (new)
- MainScreen.kt
- features/home/HomeScreen.kt
- core/ui/guide/GuideRegistry.kt
- a unit test for the setting parse if the repository parse is testable without Android

## Done when
Build + tests pass; with simple mode on Home shows 4 actions; full mode shows all actions with
قبض and صرف as separate buttons; switching in Settings changes Home immediately.
