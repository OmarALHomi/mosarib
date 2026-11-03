# Review 027, round 1: owner feedback (2026-11-03, seen on the device)

## Required: no "screen inside a pop-up"
The owner does not like the backup and voice-reading settings inside a bottom sheet. With tabs,
lists and sliders, it looks like a screen inside a pop-up.
- **النسخ الاحتياطي** and **القراءة الصوتية** open as **full pages** (a sub-screen in the
  settings flow), not as sheets:
  - a top app bar with the title and a back arrow (RTL: arrow on the right);
  - the system back button returns to the settings list;
  - inside, use the same visual language as the settings list: section headers + grouped rows in
    one rounded card per section. The backup page keeps the local and Drive content, with the
    same behaviour and handlers, laid out as sections, not as a sheet with tabs.
  - Use the app's existing navigation pattern for sub-screens (as `onNavigateToAbout` does), or a
    simple saved `settingsPage` state with `AnimatedContent` inside `SettingsScreen`. Do not add a
    navigation library.
- **Small single-value editors** stay as bottom sheets: سعر ساعة الماء، بيانات الموزع والعملة،
  المظهر، and the voice chooser inside the voice page. They are short forms, not screens.
- Keep the scroll position of the settings list when returning from a sub-page.

Claude will add the code review and device notes when the task is reported done.
