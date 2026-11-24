status: accepted
<!-- Owner OK 2026-11-24. Starts after 065 is merged (same JS file). Web dashboard only; owner uploads by hand. -->
# Task 067: Dashboard redesign — 5 sections, KPI cards, clear users table, subscriptions list

Files: `web_dashboard/admin.php`, `web_dashboard/includes/*.php`, `web_dashboard/includes/widgets/*.php`,
`web_dashboard/assets/css/admin.css`, `web_dashboard/assets/js/admin-core.js`.
Keep all existing functions, Firebase reads/writes, auth, CSRF and the proxy exactly; this is layout,
navigation and presentation. Keep RTL and the dark/light theme switch.

1. **Navigation: 5 sections** instead of 11 equal tabs: الرئيسية (overview + analytics),
   الطلبات, المستخدمون, الاشتراكات (new), الإعدادات (sub-tabs: السياسات، الأسعار وطرق الدفع،
   الإصدارات، الإشعارات والبث، الملاحظات، المشرفون، توليد التراخيص). Desktop: right sidebar;
   phone (< 768px): bottom bar or a top menu button. Remember the last section in localStorage.
2. **Overview KPI cards** (4, clickable → filtered list): طلبات تنتظر التفعيل، مشتركون فعّالون،
   تنتهي خلال 7 أيام، نشطون اليوم. Computed from the data `loadFirebaseData()` already loads.
3. **Users table**: one status chip per row (تجريبي / مشترك حتى <date> / منتهٍ / محظور), remaining
   free operations, last seen (relative), and one ⋮ actions menu holding the existing row actions and
   065's تفعيل / تمديد / إلغاء التفعيل. Search + filter chips by status. Cards layout on phones.
4. **الاشتراكات section (new, read-only list)**: one row per licence (newest per device as today's
   `rememberSubscription` merges): subscriber, device code, plan, expires (with «ينتهي خلال N يوم»
   colour), status; filter «ينتهي قريباً / منتهٍ / ملغى»; row action «تمديد» reusing 065.
5. Visual polish: consistent spacing scale, 12–16px radii, clear typographic hierarchy, sticky
   table headers, empty states, loading skeletons; WCAG AA contrast in both themes.
Checks: `node --check` on JS, `php -l` on PHP. Report the exact changed-file list for upload.
