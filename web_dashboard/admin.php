<?php
/**
 * Mosarib Admin Dashboard - Unified Modular Architecture
 * Developed by Eng. Omar AL-Homi (TubbaSoft 2026)
 * 
 * High-performance, modular dashboard structured into standalone widgets:
 * - includes/head.php: Core typography, Chart.js, theme loader & admin.css
 * - includes/header.php: Brand identity, real-time Firebase status & session user badge
 * - includes/nav_tabs.php: Section navigation (sidebar / bottom bar)
 * - includes/widgets/: Specialized standalone business widgets (Overview, Users, Licenses, Public Portal, Push, Releases, Policies, etc.)
 * - assets/js/: Direct Firebase REST engine, Argon2id/Bcrypt authentication, and dynamic analytics
 */
?>
<?php include_once __DIR__ . '/includes/head.php'; ?>
<body>

  <!-- Authentication & Security Screen Widget -->
  <?php include_once __DIR__ . '/includes/widgets/widget_auth_modal.php'; ?>

  <!-- Main Dashboard Container -->
  <div class="container" id="mainDashboardContainer" style="filter: blur(8px); pointer-events: none; transition: filter 0.3s ease;">
    
    <!-- Top Header & Branding Component -->
    <?php include_once __DIR__ . '/includes/header.php'; ?>

    <div class="app-shell">
    <!-- Navigation: 5 sections (sidebar on desktop, bottom bar on phones) -->
    <?php include_once __DIR__ . '/includes/nav_tabs.php'; ?>

    <main class="app-main" id="appMain">

    <!-- Home: KPI cards + overview statistics (shown only in the Home section) -->
    <?php include_once __DIR__ . '/includes/widgets/widget_overview.php'; ?>

    <!-- Settings sub-tabs (shown only in the Settings section) -->
    <div class="subtabs" id="settingsSubtabs" style="display:none;" role="tablist">
      <button type="button" class="subtab-btn" data-tab="policies" onclick="switchTab('policies')">السياسات</button>
      <button type="button" class="subtab-btn" data-tab="publicPortal" onclick="switchTab('publicPortal')">الأسعار وطرق الدفع</button>
      <button type="button" class="subtab-btn" data-tab="releases" onclick="switchTab('releases')">الإصدارات</button>
      <button type="button" class="subtab-btn" data-tab="push" onclick="switchTab('push')">الإشعارات والبث</button>
      <button type="button" class="subtab-btn" data-tab="feedbacks" onclick="switchTab('feedbacks')">الملاحظات</button>
      <button type="button" class="subtab-btn" data-tab="admins" onclick="switchTab('admins')">المشرفون</button>
      <button type="button" class="subtab-btn" data-tab="generator" onclick="switchTab('generator')">توليد التراخيص</button>
    </div>

    <!-- Tab 1: Royal Analytics & Charts Widget -->
    <?php include_once __DIR__ . '/includes/widgets/widget_analytics.php'; ?>

    <!-- Tab 2: Subscription Requests Widget -->
    <?php include_once __DIR__ . '/includes/widgets/widget_requests.php'; ?>

    <!-- Tab 3: Users & Licenses Management Widget -->
    <?php include_once __DIR__ . '/includes/widgets/widget_users.php'; ?>

    <!-- Tab 4: Releases & APK Downloads Widget -->
    <?php include_once __DIR__ . '/includes/widgets/widget_releases.php'; ?>

    <!-- Tab 5: App Policies & Remote Configuration Widget -->
    <?php include_once __DIR__ . '/includes/widgets/widget_policies.php'; ?>

    <!-- Tab 6: Push Notifications (FCM) Widget -->
    <?php include_once __DIR__ . '/includes/widgets/widget_push.php'; ?>

    <!-- Tab 7: In-App Broadcast Announcements Widget -->
    <?php include_once __DIR__ . '/includes/widgets/widget_broadcast.php'; ?>

    <!-- Tab 8: License Keys Generator Widget -->
    <?php include_once __DIR__ . '/includes/widgets/widget_generator.php'; ?>

    <!-- Tab 9: User Feedbacks & Star Ratings Widget -->
    <?php include_once __DIR__ . '/includes/widgets/widget_feedbacks.php'; ?>

    <!-- Tab 10: System Admins & Security Settings Widget -->
    <?php include_once __DIR__ . '/includes/widgets/widget_admins.php'; ?>

    <!-- Tab 11: Public Portal, Dynamic Pricing & Payment Methods Widget -->
    <?php include_once __DIR__ . '/includes/widgets/widget_public_portal.php'; ?>

    <!-- Section: Subscriptions list (read-only, one row per device) -->
    <?php include_once __DIR__ . '/includes/widgets/widget_subscriptions.php'; ?>
    </main>
    </div>

    <!-- User Profile & Deep Actions Modal -->
    <?php include_once __DIR__ . '/includes/widgets/widget_user_detail_modal.php'; ?>

    <!-- Dashboard Footer -->
    <footer>
      لوحة الإدارة والتحكم — المهندس عمر الحومي &copy; 2026 | جميع الحقوق محفوظة
    </footer>
  </div>

  <!-- Unified Modular Scripts -->
  <script src="assets/js/firebase-api.js?v=<?= @filemtime(__DIR__ . '/assets/js/firebase-api.js') ?: time() ?>"></script>
  <script src="assets/js/admin-auth.js?v=<?= @filemtime(__DIR__ . '/assets/js/admin-auth.js') ?: time() ?>"></script>
  <script src="assets/js/admin-core.js?v=<?= @filemtime(__DIR__ . '/assets/js/admin-core.js') ?: time() ?>"></script>

</body>
</html>
