<?php
/**
 * Mosarib Official Public Portal
 * Ultra Professional Teal & Pearl Architecture & Live Database Sync
 * Developed for Eng. Omar AL-Homi (TubbaSoft 2026)
 */

// Prevent caching so admin modifications reflect instantly
header('Cache-Control: no-cache, no-store, must-revalidate, max-age=0');
header('Pragma: no-cache');
header('Expires: 0');
header('Content-Type: text/html; charset=UTF-8');

require_once __DIR__ . '/api/config.php';

// Real Visitor Tracking (Daily unique visitor deduplication via cookie)
$today = date('Y-m-d');
$analyticsFile = __DIR__ . '/data/portal_analytics.json';
$cookieName = 'msrb_v_visit_' . str_replace('-', '', $today);
$isNewVisitorToday = empty($_COOKIE[$cookieName]);

if ($isNewVisitorToday) {
    @setcookie($cookieName, '1', time() + 86400, '/');
}

$analytics = [
    'total_visitors' => 0,
    'today_visitors' => 0,
    'unique_visitors' => 0,
    'last_visit_date' => $today,
    'last_visit_at' => 0,
    'total_downloads' => 0,
    'today_downloads' => 0,
    'last_download_date' => $today,
    'last_download_at' => 0,
    'daily_stats' => []
];

if (file_exists($analyticsFile)) {
    $existingAnalytics = json_decode(@file_get_contents($analyticsFile), true);
    if (is_array($existingAnalytics)) {
        $analytics = array_merge($analytics, $existingAnalytics);
    }
}

if (($analytics['last_visit_date'] ?? '') !== $today) {
    $analytics['last_visit_date'] = $today;
    $analytics['today_visitors'] = 0;
}
$analytics['total_visitors'] = ($analytics['total_visitors'] ?? 0) + 1;
$analytics['today_visitors'] = ($analytics['today_visitors'] ?? 0) + 1;
if ($isNewVisitorToday) {
    $analytics['unique_visitors'] = ($analytics['unique_visitors'] ?? 0) + 1;
}
$analytics['last_visit_at'] = time();

if (!isset($analytics['daily_stats'][$today])) {
    $analytics['daily_stats'][$today] = ['visitors' => 0, 'downloads' => 0];
}
$analytics['daily_stats'][$today]['visitors']++;

@file_put_contents($analyticsFile, json_encode($analytics, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT));

// 1. Load Live Portal Settings (Sync with Firebase or fallback to local JSON)
$settingsFile = __DIR__ . '/data/portal_settings.json';
$ps = [
    'active_region_name' => 'الحيمة العليا - تعز',
    'avg_hourly_price' => 18000,
    'min_hourly_price' => 1500,
    'max_hourly_price' => 25000,
    'currency_symbol' => 'ريال',
    'water_status' => 'GOOD',
    'water_status_text' => 'مستقر ومتاح في معظم المناطق الزراعية',
    'rates_note' => 'الأسعار تختلف بحسب عمق البئر ونوع الطاقة المستخدمة',
    'hero_badge' => 'المنظومة الرقمية الأولى لإدارة مياه الآبار وحصص السقي',
    'hero_title' => 'كل قطرة محسوبة وكل حقّ محفوظ',
    'hero_subtitle' => 'مسرب يدير ساعات الري بالدقيقة والثانية، يحسب حصة كل مزارع، ويصدر سندات القبض الفورية — كل ذلك يعمل بدون إنترنت في أعمق الحقول الزراعية.',
    'contact_whatsapp' => '967773712030',
    'contact_phone' => '773712030',
    'developer_name' => 'المهندس عمر الحومي',
    'developer_company' => 'تُبع سوفت للحلول البرمجية الذكية (TubbaSoft)',
    'show_top_ticker' => true,
    'show_rates_section' => true,
    'show_features_section' => true,
    'show_how_section' => true,
    'show_testimonials_section' => true,
    'show_plans_section' => true,
    'show_news_section' => true,
    'show_faq_section' => true,
    'show_developer_section' => true,
    'show_download_cta' => true
];

if (file_exists($settingsFile)) {
    $savedSettings = json_decode(file_get_contents($settingsFile), true);
    if (is_array($savedSettings)) {
        $ps = array_merge($ps, $savedSettings);
    }
}

// 2. Load Subscription Pricing
$pricingFile = __DIR__ . '/data/subscription_pricing.json';
$pricing = file_exists($pricingFile) ? (json_decode(file_get_contents($pricingFile), true) ?: []) : [];
$monthlyPrice = $pricing['monthly']['price'] ?? '8';
$monthlyCurrency = $pricing['monthly']['currency'] ?? '$';
$yearlyPrice = $pricing['yearly']['price'] ?? '75';
$yearlyOrig = $pricing['yearly']['original_price'] ?? '115';
$yearlyDisc = $pricing['yearly']['discount_text'] ?? 'وفر 35% (خصم 40$)';
$yearlyCurrency = $pricing['yearly']['currency'] ?? '$';

// 3. Load Payment Methods
$paymentMethodsFile = __DIR__ . '/data/payment_methods.json';
$paymentMethods = file_exists($paymentMethodsFile) ? (json_decode(file_get_contents($paymentMethodsFile), true) ?: []) : [];

// 4. Load Public News / Bulletins
$newsFile = __DIR__ . '/data/portal_news.json';
$portalNews = file_exists($newsFile) ? (json_decode(file_get_contents($newsFile), true) ?: []) : [];

// 5. Load Real Latest Release Info for direct APK download
$releasesFile = __DIR__ . '/uploads/releases_index.json';
$releases = file_exists($releasesFile) ? (json_decode(@file_get_contents($releasesFile), true) ?: []) : [];
$latestRelease = !empty($releases[0]) ? $releases[0] : null;

$directDownloadUrl = '';
if (!empty($latestRelease['download_url'])) {
    $directDownloadUrl = $latestRelease['download_url'];
} elseif (!empty($latestRelease['file_name']) && file_exists(__DIR__ . '/uploads/' . $latestRelease['file_name'])) {
    $directDownloadUrl = 'uploads/' . rawurlencode($latestRelease['file_name']);
} elseif (file_exists(__DIR__ . '/uploads/Mosarib_Latest.apk')) {
    $directDownloadUrl = 'uploads/Mosarib_Latest.apk';
} else {
    $apks = glob(__DIR__ . '/uploads/*.apk');
    if (!empty($apks)) {
        usort($apks, function($a, $b) { return filemtime($b) - filemtime($a); });
        $directDownloadUrl = 'uploads/' . rawurlencode(basename($apks[0]));
    }
}
if (empty($directDownloadUrl)) {
    $directDownloadUrl = 'uploads/Mosarib_Latest.apk';
}

$apkVersionName = $latestRelease['version_name'] ?? '1.2.0';
$apkSizeMb = $latestRelease['file_size_mb'] ?? '2.88';
?>
<!DOCTYPE html>
<html lang="ar" dir="rtl" data-theme="light">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title><?= htmlspecialchars($ps['hero_title']) ?> • البوابة الرسمية</title>
  <meta name="description" content="<?= htmlspecialchars($ps['hero_subtitle']) ?>">
  <meta name="theme-color" content="#FAFBFB">
  <meta property="og:title" content="مُسَرِّب | Mosarib — حوكمة مياه وساعات ري الآبار">
  <meta property="og:description" content="كل قطرة محسوبة، وكل حق محفوظ.">

  <link rel="icon" href="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%230D9488' stroke-width='2.5' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M12 2.7 17.7 8.4a8 8 0 1 1-11.4 0Z'/%3E%3C/svg%3E">

  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=IBM+Plex+Sans+Arabic:wght@400;500;600;700&family=Noto+Kufi+Arabic:wght@500;700;900&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="assets/css/portal.css?v=<?= filemtime(__DIR__ . '/assets/css/portal.css') ?>">

  <script>
    (function () {
      try {
        var s = localStorage.getItem('musarib-theme');
        var t = s || (matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
        document.documentElement.setAttribute('data-theme', t);
      } catch (e) {}
    })();
  </script>
</head>
<body>

<!-- SVG Icon Symbols -->
<svg width="0" height="0" style="position:absolute" aria-hidden="true">
  <symbol id="i-drop" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2.7 17.7 8.4a8 8 0 1 1-11.4 0Z"/></symbol>
  <symbol id="i-check" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></symbol>
  <symbol id="i-check-circle" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="m8.5 12 2.5 2.5 4.5-5"/></symbol>
  <symbol id="i-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 12H5M11 6l-6 6 6 6"/></symbol>
  <symbol id="i-download" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></symbol>
  <symbol id="i-x" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 6 6 18M6 6l12 12"/></symbol>
  <symbol id="i-sun" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.9 4.9l1.5 1.5M17.6 17.6l1.5 1.5M2 12h2M20 12h2M4.9 19.1l1.5-1.5M17.6 6.4l1.5-1.5"/></symbol>
  <symbol id="i-moon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8Z"/></symbol>
  <symbol id="i-menu" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 6h16M4 12h16M4 18h16"/></symbol>
  <symbol id="i-chart" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 3v18h18"/><path d="M7 16v-5M12 16V8M17 16v-9"/></symbol>
  <symbol id="i-cloud" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.5 19a4.5 4.5 0 1 0-.5-9A6 6 0 0 0 6.3 9.5 4 4 0 0 0 7 19Z"/></symbol>
  <symbol id="i-receipt" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 2h12a1 1 0 0 1 1 1v18l-3-2-3 2-3-2-3 2V3a1 1 0 0 1 1-1Z"/><path d="M9 7h6M9 11h6"/></symbol>
  <symbol id="i-wifi" viewBox="0 0 24 24"><path d="M12 20a2 2 0 1 0 0-4 2 2 0 0 0 0 4ZM5 12.5a10 10 0 0 1 14 0M8.5 16a5 5 0 0 1 7 0M1.5 9a15 15 0 0 1 21 0" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/></symbol>
  <symbol id="i-battery" viewBox="0 0 24 24"><rect x="2" y="7" width="18" height="10" rx="2" stroke="currentColor" stroke-width="2" fill="none"/><rect x="4" y="9" width="12" height="6" rx="1" fill="currentColor"/><path d="M22 11v2" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></symbol>
  <symbol id="i-star" viewBox="0 0 24 24"><path d="M12 2l3 7 7 .8-5.2 4.8L18 22l-6-3.6L6 22l1.2-7.4L2 9.8 9 9z"/></symbol>
  <symbol id="i-users" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.9M16 3.1a4 4 0 0 1 0 7.8"/></symbol>
  <symbol id="i-user-plus" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M19 8v6M22 11h-6"/></symbol>
  <symbol id="i-settings" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1Z"/></symbol>
  <symbol id="i-plus" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 5v14M5 12h14"/></symbol>
  <symbol id="i-help" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M9.1 9a3 3 0 0 1 5.8 1c0 2-3 3-3 3"/><path d="M12 17h.01"/></symbol>
  <symbol id="i-wallet" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 8H5a2 2 0 0 1 0-4h13v4"/><path d="M20 8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6"/><path d="M17 13h.01"/></symbol>
  <symbol id="i-home" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m3 11 9-8 9 8v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><path d="M9 22V12h6v10"/></symbol>
  <symbol id="i-whatsapp" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/></symbol>
</svg>

<!-- Headline Ticker -->
<?php if (!empty($ps['headline_alert']) && ($ps['show_top_ticker'] ?? true)): ?>
<div class="top-ticker" id="topTickerBar">
  <div class="container" style="display:flex; align-items:center; justify-content:center; gap:8px;">
    <span>📢</span>
    <span id="topTickerText"><?= htmlspecialchars($ps['headline_alert']) ?></span>
  </div>
</div>
<?php endif; ?>

<!-- Header -->
<header class="site-header" id="siteHeader">
  <div class="container nav">
    <a href="#" class="brand" aria-label="مُسَرِّب — الصفحة الرئيسية">
      <span class="brand__logo"><svg><use href="#i-drop"/></svg></span>
      <span>مُسَرِّب<span class="brand__dot"></span></span>
    </a>
    
    <nav class="nav__links" aria-label="التنقل الرئيسي">
      <?php if (!empty($ps['show_features_section'])): ?>
      <a href="#features">المميزات</a>
      <?php endif; ?>
      <?php if (!empty($ps['show_rates_section'])): ?>
      <a href="#rates">متوسط الأسعار</a>
      <?php endif; ?>
      <?php if (!empty($ps['show_plans_section'])): ?>
      <a href="#pricing">الباقات والترخيص</a>
      <?php endif; ?>
      <?php if (!empty($ps['show_how_section'])): ?>
      <a href="#how">كيف يعمل</a>
      <?php endif; ?>
      <?php if (!empty($ps['show_faq_section'])): ?>
      <a href="#faq">الأسئلة</a>
      <?php endif; ?>
    </nav>

    <div class="nav__actions">
      <button class="icon-btn" id="themeToggle" aria-label="تبديل المظهر" title="تبديل المظهر الداكن/الفاتح">
        <svg class="i-sun"><use href="#i-sun"/></svg>
        <svg class="i-moon"><use href="#i-moon"/></svg>
      </button>

      <a href="<?= htmlspecialchars($directDownloadUrl) ?>" class="btn btn-primary" download onclick="trackDownload()" title="تحميل ملف APK المباشر">
        <svg class="btn__icon"><use href="#i-download"/></svg>
        <span>تحميل التطبيق</span>
      </a>

      <button class="icon-btn menu-btn" id="menuBtn" aria-label="القائمة" aria-expanded="false" aria-controls="mobileMenu">
        <svg><use href="#i-menu"/></svg>
      </button>
    </div>
  </div>
</header>

<!-- Mobile Menu Drawer -->
<nav class="mobile-menu" id="mobileMenu" aria-label="التنقل في الجوال">
  <?php if (!empty($ps['show_features_section'])): ?>
  <a href="#features" data-menu-close>المميزات الميدانية</a>
  <?php endif; ?>
  <?php if (!empty($ps['show_rates_section'])): ?>
  <a href="#rates" data-menu-close>مؤشر وأسعار الساعات</a>
  <?php endif; ?>
  <?php if (!empty($ps['show_plans_section'])): ?>
  <a href="#pricing" data-menu-close>الباقات والترخيص</a>
  <?php endif; ?>
  <?php if (!empty($ps['show_how_section'])): ?>
  <a href="#how" data-menu-close>كيف يعمل</a>
  <?php endif; ?>
  <?php if (!empty($ps['show_faq_section'])): ?>
  <a href="#faq" data-menu-close>الأسئلة الشائعة</a>
  <?php endif; ?>
  <a href="<?= htmlspecialchars($directDownloadUrl) ?>" class="btn btn-primary btn-block btn-lg" download onclick="trackDownload()" data-menu-close>
    تحميل التطبيق مجاناً (APK v<?= htmlspecialchars($apkVersionName) ?>)
  </a>
  <button class="btn btn-secondary btn-block btn-lg" onclick="openModal('YEARLY')" data-menu-close>
    طلب تفعيل الترخيص ⚡
  </button>
</nav>

<main>
  <!-- Hero Section -->
  <section class="hero">
    <div class="container hero__grid">
      <div class="hero__copy reveal">
        <p class="eyebrow" id="heroBadgeEl"><span class="eyebrow__dot"></span><span id="heroBadgeText"><?= htmlspecialchars($ps['hero_badge']) ?></span></p>
        <h1 class="display" id="heroTitleText">
          كل قطرة محسوبة<br>
          وكل حقّ <em>محفوظ</em>
        </h1>
        <p class="lead" id="heroSubtitleText">
          <?= htmlspecialchars($ps['hero_subtitle']) ?>
        </p>

        <div class="hero__actions">
          <a href="<?= htmlspecialchars($directDownloadUrl) ?>" class="btn btn-primary btn-lg" download onclick="trackDownload()">
            <svg class="btn__icon"><use href="#i-download"/></svg>
            <span>تحميل التطبيق (APK)</span>
          </a>
          <button class="btn btn-secondary btn-lg" onclick="openModal('YEARLY')">
            طلب تفعيل الترخيص ⚡
          </button>
          <?php if (!empty($ps['contact_whatsapp'])): ?>
          <a class="btn btn-ghost btn-lg" href="https://wa.me/<?= preg_replace('/[^0-9]/', '', $ps['contact_whatsapp']) ?>" target="_blank">
            <svg class="btn__icon" style="color:var(--success);"><use href="#i-whatsapp"/></svg>
            <span>واتساب</span>
          </a>
          <?php endif; ?>
        </div>
      </div>

      <!-- Interactive 3D Phone Mockup -->
      <div class="hero__visual reveal" id="heroVisual">
        <!-- Floating Glassmorphism Card 1 -->
        <div class="float-card float-card--receipt">
          <div class="float-card__label">
            <svg><use href="#i-receipt"/></svg>
            سند قبض جديد
          </div>
          <div class="float-card__value">١٠٬٠٠٠ ر.ي</div>
          <div class="float-card__sub">✓ تم الإصدار والمشاركة عبر واتساب</div>
        </div>

        <!-- Floating Glassmorphism Card 2 -->
        <div class="float-card float-card--level">
          <div class="float-card__label">
            <svg><use href="#i-chart"/></svg>
            مستوى الضخ والتشغيل
          </div>
          <div class="float-card__value">100% Offline</div>
          <div class="float-card__sub" style="color:var(--success);">⚡ يعمل بكفاءة تامة بدون إنترنت</div>
        </div>

        <!-- Device -->
        <div class="device" id="device" aria-hidden="true">
          <div class="device__island"></div>
          <div class="device__screen">
            <!-- Status bar -->
            <div class="app-status">
              <span>٩:٤١</span>
              <span class="app-status__icons">
                <svg><use href="#i-wifi"/></svg>
                <svg><use href="#i-battery"/></svg>
              </span>
            </div>

            <!-- App Bar -->
            <div class="app-bar">
              <div class="app-bar__btn"><svg><use href="#i-moon"/></svg></div>
              <div class="app-bar__brand">
                <span class="app-bar__logo"><svg><use href="#i-drop"/></svg></span>
                مُسَرِّب <small>• Mosarib</small>
              </div>
              <div class="app-bar__btn"><svg><use href="#i-help"/></svg></div>
            </div>

            <!-- In-App Content -->
            <div class="app-content">
              <!-- Quick access -->
              <div class="app-label">
                <span>الوصول السريع</span>
              </div>
              <div class="quick-grid">
                <div class="q-card">
                  <div class="q-icon bg-water"><svg><use href="#i-drop"/></svg></div>
                  <div class="q-text"><span class="q-title">سقي جديد</span><span class="q-sub">تسجيل دورة ري</span></div>
                </div>
                <div class="q-card">
                  <div class="q-icon bg-green"><svg><use href="#i-receipt"/></svg></div>
                  <div class="q-text"><span class="q-title">قبض</span><span class="q-sub">سند تحصيل كاش</span></div>
                </div>
                <div class="q-card">
                  <div class="q-icon bg-red"><svg><use href="#i-wallet"/></svg></div>
                  <div class="q-text"><span class="q-title">صرف</span><span class="q-sub">صيانة ونفقات</span></div>
                </div>
                <div class="q-card">
                  <div class="q-icon bg-purple"><svg><use href="#i-drop"/></svg></div>
                  <div class="q-text"><span class="q-title">شراء</span><span class="q-sub">شراء ماء من بئر</span></div>
                </div>
                <div class="q-card">
                  <div class="q-icon bg-cyan"><svg><use href="#i-user-plus"/></svg></div>
                  <div class="q-text"><span class="q-title">عميل جديد</span><span class="q-sub">إضافة مزارع</span></div>
                </div>
                <div class="q-card">
                  <div class="q-icon bg-teal"><svg><use href="#i-cloud"/></svg></div>
                  <div class="q-text"><span class="q-title">نسخ احتياطي</span><span class="q-sub">سحابي آمن</span></div>
                </div>
              </div>

              <!-- Records -->
              <div class="app-label">
                <span>آخر التسجيلات الميدانية</span>
                <span class="app-label__link">عرض الكل ←</span>
              </div>
              <div class="records">
                <div class="record">
                  <div class="record__top">
                    <div class="record__name">
                      <span class="record__avatar">س</span>
                      سعيد بن حسين الحميلي
                    </div>
                    <span class="record__amount">10,000 ر.ي</span>
                  </div>
                  <div class="record__bottom">
                    <span class="record__time">٤:٤٩ – ٦:١٩ ص</span>
                    <span class="record__status status-paid"><svg><use href="#i-check-circle"/></svg> مسدد</span>
                  </div>
                </div>
                <div class="record">
                  <div class="record__top">
                    <div class="record__name">
                      <span class="record__avatar">م</span>
                      محمد بن ناصر الحميلي
                    </div>
                    <span class="record__amount">8,750 ر.ي</span>
                  </div>
                  <div class="record__bottom">
                    <span class="record__time">١٢:٤٩ – ٣:١٩ ص</span>
                    <span class="record__status status-unpaid">متبقي أجل</span>
                  </div>
                </div>
                <div class="record">
                  <div class="record__top">
                    <div class="record__name">
                      <span class="record__avatar">ص</span>
                      صالح بن علي اليافعي
                    </div>
                    <span class="record__amount">13,500 ر.ي</span>
                  </div>
                  <div class="record__bottom">
                    <span class="record__time">١:٤٩ – ٤:٤٩ ص</span>
                    <span class="record__status status-paid"><svg><use href="#i-check-circle"/></svg> مسدد</span>
                  </div>
                </div>
              </div>

              <!-- Chart -->
              <div class="app-label">
                <span>سقي الأسبوع</span>
                <span class="app-label__link">التقرير ←</span>
              </div>
              <div class="chart">
                <div class="chart__head">
                  <span class="chart__title">إجمالي الضخ: <strong>48 ساعة</strong></span>
                  <span class="chart__badge">7 أيام</span>
                </div>
                <div class="chart__bars">
                  <span style="--h: 35%; --d: 0s"></span>
                  <span style="--h: 60%; --d: .1s"></span>
                  <span class="is-active" style="--h: 85%; --d: .2s"></span>
                  <span style="--h: 45%; --d: .3s"></span>
                  <span style="--h: 70%; --d: .4s"></span>
                  <span style="--h: 92%; --d: .5s"></span>
                  <span style="--h: 30%; --d: .6s"></span>
                </div>
              </div>
            </div>

            <!-- Bottom Nav -->
            <div class="app-nav">
              <div class="app-nav__item">
                <svg><use href="#i-chart"/></svg>
                <span>التقارير</span>
              </div>
              <div class="app-nav__item">
                <svg><use href="#i-users"/></svg>
                <span>العملاء</span>
              </div>
              <div class="app-nav__fab">
                <svg><use href="#i-plus"/></svg>
              </div>
              <div class="app-nav__item is-active">
                <svg><use href="#i-home"/></svg>
                <span>الرئيسية</span>
              </div>
              <div class="app-nav__item">
                <svg><use href="#i-settings"/></svg>
                <span>الإعدادات</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Stats Counters -->
    <div class="container">
      <dl class="hero__stats reveal">
        <div>
          <dt><span class="stat-num" data-target="1240">1,240</span><span class="stat__suffix">+</span></dt>
          <dd>بئر يُدار عبر منظومة مُسَرِّب</dd>
        </div>
        <div>
          <dt><span class="stat-num" data-target="38000">38,000</span><span class="stat__suffix">+</span></dt>
          <dd>سند قبض مالي تم إصداره</dd>
        </div>
        <div>
          <dt><span class="stat-num" data-target="99.9" data-decimal="1">99.9</span><span class="stat__suffix">%</span></dt>
          <dd>دقة حساب كسور الساعات بالثواني</dd>
        </div>
      </dl>
    </div>
  </section>

  <!-- Live Water Rates & Hourly Prices Strip -->
  <?php if (!empty($ps['show_rates_section'])): ?>
  <section class="section" id="rates" style="padding-top: 20px; padding-bottom: 40px;">
    <div class="container">
      <div class="rates-strip-card reveal">
        <div class="rates-strip-loc">
          <div class="rates-icon-badge">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path><circle cx="12" cy="10" r="3"></circle></svg>
          </div>
          <div>
            <div style="font-size: var(--step--2); color: var(--muted); font-weight: 600;">المنطقة الجغرافية المعتمدة:</div>
            <strong style="font-size: var(--step-0); color: var(--ink);" id="regionNameEl"><?= htmlspecialchars($ps['active_region_name']) ?></strong>
          </div>
        </div>

        <div class="rates-strip-price">
          <div style="font-size: var(--step--2); color: var(--muted); font-weight: 600;">متوسط سعر ساعة الري</div>
          <div class="rates-main-val">
            <span id="avgPriceEl"><?= number_format($ps['avg_hourly_price']) ?></span>
            <span style="font-size: var(--step--1); font-weight: 600; color: var(--muted);"><?= htmlspecialchars($ps['currency_symbol']) ?></span>
          </div>
          <div style="font-size: var(--step--2); color: var(--muted); margin-top: 4px;" id="minPriceEl">
            أعلى: <?= number_format($ps['max_hourly_price']) ?> • أقل: <?= number_format($ps['min_hourly_price']) ?> ريال
          </div>
        </div>

        <div>
          <div style="font-size: var(--step--2); color: var(--muted); font-weight: 600; margin-bottom: 6px;">حالة منسوب المياه والضخ:</div>
          <div class="rates-status-pill" id="waterStatusEl">
            💧 <?= htmlspecialchars($ps['water_status_text'] ?? 'مستقر ومتاح في معظم المناطق الزراعية') ?>
          </div>
        </div>
      </div>
      <?php if (!empty($ps['rates_note'])): ?>
      <div style="margin-top: 12px; font-size: var(--step--1); color: var(--muted); text-align: center;" id="ratesNoteEl">
        ℹ️ <?= htmlspecialchars($ps['rates_note']) ?>
      </div>
      <?php endif; ?>
    </div>
  </section>
  <?php endif; ?>

  <!-- Features Section -->
  <?php if (!empty($ps['show_features_section'])): ?>
  <section class="section features" id="features">
    <div class="container">
      <div class="section-head reveal">
        <h2 class="h2">صُمم خصيصاً ليلائم واقع الحقول الزراعية</h2>
        <p class="lead">كل ميزة في مُسَرِّب أُضيفت لحل تحديات حقيقية تواجه ملاك الآبار والموزعين والمزارعين.</p>
      </div>
      <div class="features__grid">
        <article class="feature reveal" style="--i:0">
          <div class="feature__icon"><svg><use href="#i-chart"/></svg></div>
          <h3 class="h3">حساب بالثواني والدقائق</h3>
          <p>تسجيل دورات الري بدقة متناهية. التطبيق يحسب حصة كل مزارع ويوزع التكلفة تلقائياً دون أي أخطاء حسابية يدوية.</p>
        </article>
        <article class="feature reveal" style="--i:1">
          <div class="feature__icon"><svg><use href="#i-cloud"/></svg></div>
          <h3 class="h3">يعمل 100% بدون إنترنت</h3>
          <p>جميع العمليات والحسابات تُحفظ وتُدار محلياً على جهازك في أعمق المزارع والوديان، مع إمكانية المزامنة والنسخ السحابي على Google Drive.</p>
        </article>
        <article class="feature reveal" style="--i:2">
          <div class="feature__icon"><svg><use href="#i-receipt"/></svg></div>
          <h3 class="h3">سندات قبض فورية عبر واتساب</h3>
          <p>أصدر سند قبض معتمد لكل عملية تحصيل أو صرف، وشاركه مع المزارع عبر واتساب بضغطة واحدة مع رقم مرجعي وتاريخ لا يقبل التلاعب.</p>
        </article>
      </div>
    </div>
  </section>
  <?php endif; ?>

  <!-- How It Works -->
  <?php if (!empty($ps['show_how_section'])): ?>
  <section class="section" id="how">
    <div class="container">
      <div class="section-head reveal">
        <h2 class="h2">ثلاث خطوات فقط للانطلاق</h2>
        <p class="lead">من تسجيل دورة الري إلى سند القبض المعتمد والموثق — بأبسط ما يمكن.</p>
      </div>
      <div class="steps">
        <div class="step reveal" style="--i:0">
          <div class="step__num">١</div>
          <h3 class="h3">أضف مزارعيك وآبارك</h3>
          <p>سجّل أسماء العملاء وبيانات الآبار وسعر الساعة المتفق عليه في أقل من دقيقة.</p>
        </div>
        <div class="step reveal" style="--i:1">
          <div class="step__num">٢</div>
          <h3 class="h3">سجّل الري والدفعات</h3>
          <p>كل دورة سقي تُحسب بالوقت الفعلي. وسند القبض يصدر فوراً عند استلام أي دفعة نقدية.</p>
        </div>
        <div class="step reveal" style="--i:2">
          <div class="step__num">٣</div>
          <h3 class="h3">شارك الكشوفات والسندات</h3>
          <p>أرسل تقريراً مفصلاً لكل مزارع عبر واتساب بصيغة PDF أو Excel توضح الساعات والمتبقي.</p>
        </div>
      </div>
    </div>
  </section>
  <?php endif; ?>

  <!-- Testimonials -->
  <?php if (!empty($ps['show_testimonials_section'])): ?>
  <section class="section testimonials" id="testimonials">
    <div class="container">
      <div class="section-head reveal">
        <h2 class="h2">يثق به المزارعون وملاك الآبار</h2>
        <p class="lead">من مزارع الحيمة وتعز إلى وادي حضرموت ولحج — مُسَرِّب رفيق العمل اليومي.</p>
      </div>
      <div class="testimonials__grid">
        <article class="testimonial reveal" style="--i:0">
          <div class="testimonial__stars">
            <svg><use href="#i-star"/></svg><svg><use href="#i-star"/></svg><svg><use href="#i-star"/></svg><svg><use href="#i-star"/></svg><svg><use href="#i-star"/></svg>
          </div>
          <p class="testimonial__quote">«كنت أضيّع ساعات طويلة في حساب كسور الساعات والدفاتر الورقية. الآن بضغطة زر يخرج السند والحساب الصافي.»</p>
          <div class="testimonial__author">
            <div class="testimonial__avatar">س</div>
            <div>
              <div class="testimonial__name">سعيد بن حسين الحميلي</div>
              <div class="testimonial__role">مالك بئر وموزع ري — تعز</div>
            </div>
          </div>
        </article>
        <article class="testimonial reveal" style="--i:1">
          <div class="testimonial__stars">
            <svg><use href="#i-star"/></svg><svg><use href="#i-star"/></svg><svg><use href="#i-star"/></svg><svg><use href="#i-star"/></svg><svg><use href="#i-star"/></svg>
          </div>
          <p class="testimonial__quote">«ميزة العمل بدون إنترنت هي الأساس عندنا في الوادي. البيانات محفوظة وسندات الواتساب وثقت حقوق الجميع دون أي خلاف.»</p>
          <div class="testimonial__author">
            <div class="testimonial__avatar">ص</div>
            <div>
              <div class="testimonial__name">صالح بن علي اليافعي</div>
              <div class="testimonial__role">مشغل مضخة زراعية — لحج</div>
            </div>
          </div>
        </article>
        <article class="testimonial reveal" style="--i:2">
          <div class="testimonial__stars">
            <svg><use href="#i-star"/></svg><svg><use href="#i-star"/></svg><svg><use href="#i-star"/></svg><svg><use href="#i-star"/></svg><svg><use href="#i-star"/></svg>
          </div>
          <p class="testimonial__quote">«النسخ الاحتياطي على Google Drive أنقذ حساباتي تماماً عندما تعطل هاتفي واسترجعت كل بيانات المواسم خلال دقيقة.»</p>
          <div class="testimonial__author">
            <div class="testimonial__avatar">م</div>
            <div>
              <div class="testimonial__name">محمد بن ناصر الحميلي</div>
              <div class="testimonial__role">مالك بئر ارتوازية — الحيمة</div>
            </div>
          </div>
        </article>
      </div>
    </div>
  </section>
  <?php endif; ?>

  <!-- Pricing Plans Section -->
  <?php if (!empty($ps['show_plans_section'])): ?>
  <section class="section" id="pricing">
    <div class="container">
      <div class="section-head reveal">
        <h2 class="h2">باقات التفعيل والترخيص</h2>
        <p class="lead">استثمر في حوكمة بئرك وحفظ حقوقك بأعلى دقة واحترافية.</p>
      </div>
      <div class="pricing__grid">
        <!-- Monthly Plan -->
        <article class="plan reveal" style="--i:0">
          <div class="plan__name">الاشتراك الشهري</div>
          <div class="plan__price">
            <span id="monthlyPriceEl"><?= htmlspecialchars($monthlyPrice) ?><?= htmlspecialchars($monthlyCurrency) ?></span>
            <span>/ شهرياً</span>
          </div>
          <ul class="plan__features">
            <li><svg><use href="#i-check"/></svg>صلاحية كاملة لكافة المزايا لمدة 30 يوماً</li>
            <li><svg><use href="#i-check"/></svg>إصدار ومشاركة سندات القبض عبر واتساب</li>
            <li><svg><use href="#i-check"/></svg>حفظ وتأمين البيانات محلياً بدون إنترنت</li>
            <li><svg><use href="#i-check"/></svg>دعم فني مباشر عبر واتساب</li>
          </ul>
          <button class="btn btn-ghost btn-block btn-lg" onclick="openModal('MONTHLY')">طلب تفعيل الباقة الشهرية</button>
        </article>

        <!-- Annual Plan (Featured) -->
        <article class="plan plan--featured reveal" style="--i:1">
          <div class="plan__badge"><?= htmlspecialchars($yearlyDisc) ?></div>
          <div class="plan__name">الاشتراك السنوي الشامل ⭐</div>
          <div class="plan__price">
            <span id="yearlyPriceEl"><?= htmlspecialchars($yearlyPrice) ?><?= htmlspecialchars($yearlyCurrency) ?></span>
            <span>/ سنوياً</span>
            <?php if (!empty($yearlyOrig)): ?>
            <span class="plan__orig" id="yearlyOrigEl">بدلاً من <?= htmlspecialchars($yearlyOrig) ?><?= htmlspecialchars($yearlyCurrency) ?></span>
            <?php endif; ?>
          </div>
          <ul class="plan__features">
            <li><svg><use href="#i-check"/></svg>عمليات ري وسندات وحسابات غير محدودة طوال الموسم</li>
            <li><svg><use href="#i-check"/></svg>نسخ احتياطي سحابي تلقائي على Google Drive</li>
            <li><svg><use href="#i-check"/></svg>تصدير كشوفات الحسابات الشاملة PDF و Excel</li>
            <li><svg><use href="#i-check"/></svg>إمكانية نقل الرخصة لهاتف بديل مجاناً</li>
            <li><svg><use href="#i-check"/></svg>أولوية الدعم الفني والتحديثات المباشرة</li>
          </ul>
          <button class="btn btn-primary btn-block btn-lg" onclick="openModal('YEARLY')">طلب تفعيل الباقة السنوية ⚡</button>
        </article>
      </div>
    </div>
  </section>
  <?php endif; ?>

  <!-- Agricultural News & Market Reports -->
  <?php if (!empty($ps['show_news_section']) && !empty($portalNews)): ?>
  <section class="section" id="news" style="background:var(--surface); border-block:1px solid var(--line);">
    <div class="container">
      <div class="section-head reveal">
        <h2 class="h2">مستجدات وبورصة مياه الآبار</h2>
        <p class="lead">تقارير دورية وإرشادات حصرية لملاك الآبار والمزارعين.</p>
      </div>
      <div class="features__grid">
        <?php foreach (array_slice($portalNews, 0, 3) as $idx => $nw): ?>
        <article class="feature reveal" style="--i:<?= $idx ?>">
          <div style="font-size: var(--step--1); font-weight: 700; color: var(--teal-600); margin-bottom: 6px;">
            <?= htmlspecialchars($nw['category'] ?? 'تقارير الآبار') ?>
          </div>
          <h3 class="h3" style="font-size: var(--step-0); margin-bottom: 8px;"><?= htmlspecialchars($nw['title']) ?></h3>
          <p style="font-size: var(--step--1); line-height: 1.7;"><?= htmlspecialchars($nw['summary']) ?></p>
        </article>
        <?php endforeach; ?>
      </div>
    </div>
  </section>
  <?php endif; ?>

  <!-- FAQ Section -->
  <?php if (!empty($ps['show_faq_section'])): ?>
  <section class="section faq" id="faq">
    <div class="container">
      <div class="section-head reveal">
        <h2 class="h2">الأسئلة الشائعة</h2>
        <p class="lead">كل ما تحتاج معرفته عن تطبيق ومنظومة مُسَرِّب.</p>
      </div>
      <div class="faq__list">
        <details class="faq__item reveal" style="--i:0">
          <summary>هل يعمل التطبيق فعلاً 100% بدون إنترنت؟</summary>
          <p>نعم، بكل تأكيد. تم تصميم التطبيق ليعمل بنظام Offline-First؛ فجميع العمليات من تسجيل دورات السقي وإصدار السندات وحساب ديون المزارعين تعمل محلياً على الهاتف. وعند توفر الإنترنت، يمكنك مزامنة البيانات سحابياً بضغطة زر واحدة.</p>
        </details>
        <details class="faq__item reveal" style="--i:1">
          <summary>ماذا يحدث إذا فُقد هاتفي أو تعرض للتلف؟</summary>
          <p>بياناتك مؤمنة تماماً إذا قمت بتفعيل النسخ الاحتياطي على Google Drive. يمكنك تسجيل الدخول من أي هاتف أندرويد بديل واستعادة كافة حساباتك ومواسمك في ثوانٍ معدودة.</p>
        </details>
        <details class="faq__item reveal" style="--i:2">
          <summary>هل يمكنني نقل رخصة التفعيل لجهاز آخر؟</summary>
          <p>نعم، في الباقة السنوية يمكنك نقل الرخصة لأي هاتف جديد مجاناً بالتواصل مع الدعم الفني، وسيتم نقل صلاحية حسابك فوراً دون أي رسوم إضافية.</p>
        </details>
        <details class="faq__item reveal" style="--i:3">
          <summary>كيف يتم دفع قيمة الاشتراك وتفعيل الحساب؟</summary>
          <p>يمكنك التحويل عبر بنك الكريمي أو محفظة جيب أو ون كاش، ثم تسجيل رقم السند في نموذج الاشتراك بالموقع أو عبر واتساب، وسيتم تفعيل حسابك فوراً.</p>
        </details>
        <details class="faq__item reveal" style="--i:4">
          <summary>هل سندات القبض معتمدة وقانونية بين المزارعين؟</summary>
          <p>نعم، يحمل كل سند رقماً مرجعياً فريداً وتاريخاً وتوقيتاً بالدقيقة وبيانات تفصيلية لساعات الري وحساب الفاقد، مما يجعله وثيقة رسمية متفقاً عليها وموثوقة لفض أي نزاع.</p>
        </details>
      </div>
    </div>
  </section>
  <?php endif; ?>

  <!-- CTA Section -->
  <?php if (!empty($ps['show_download_cta'])): ?>
  <section class="cta">
    <div class="container">
      <div class="cta__card reveal">
        <div class="cta__content">
          <h2 class="cta__title">ابدأ اليوم — واحمِ حقوقك المائية والمالية</h2>
          <p class="cta__desc">انضم إلى مئات ملاك الآبار والموزعين الذين يعتمدون على مُسَرِّب كل يوم لإدارة دورات السقي وسندات القبض.</p>
          <div style="display:flex; justify-content:center; gap:14px; flex-wrap:wrap;">
            <a href="<?= htmlspecialchars($directDownloadUrl) ?>" class="btn btn-primary btn-lg" download onclick="trackDownload()">
              <svg class="btn__icon"><use href="#i-download"/></svg>
              <span>تحميل النسخة الأحدث (APK)</span>
            </a>
            <button class="btn btn-secondary btn-lg" onclick="openModal('YEARLY')">
              طلب تفعيل رخصة التطبيق ⚡
            </button>
          </div>
        </div>
      </div>
    </div>
  </section>
  <?php endif; ?>
</main>

<!-- Footer -->
<footer class="site-footer">
  <div class="container">
    <div class="footer__top">
      <div class="footer__brand">
        <a href="#" class="brand">
          <span class="brand__logo"><svg><use href="#i-drop"/></svg></span>
          <span>مُسَرِّب<span class="brand__dot"></span></span>
        </a>
        <p>المنظومة الرقمية المعتمدة لحوكمة مياه الآبار وساعات الري وحفظ حقوق الموزعين والمزارعين.</p>
      </div>
      <div class="footer__col">
        <h4>المنظومة</h4>
        <?php if (!empty($ps['show_features_section'])): ?>
        <a href="#features">المميزات</a>
        <?php endif; ?>
        <?php if (!empty($ps['show_rates_section'])): ?>
        <a href="#rates">بورصة الأسعار</a>
        <?php endif; ?>
        <?php if (!empty($ps['show_plans_section'])): ?>
        <a href="#pricing">الباقات والترخيص</a>
        <?php endif; ?>
        <?php if (!empty($ps['show_how_section'])): ?>
        <a href="#how">كيف يعمل</a>
        <?php endif; ?>
      </div>
      <div class="footer__col">
        <h4>الدعم المباشر</h4>
        <?php if (!empty($ps['show_faq_section'])): ?>
        <a href="#faq">الأسئلة الشائعة</a>
        <?php endif; ?>
        <?php if (!empty($ps['contact_whatsapp'])): ?>
        <a href="https://wa.me/<?= preg_replace('/[^0-9]/', '', $ps['contact_whatsapp']) ?>" target="_blank">محادثة واتساب</a>
        <?php endif; ?>
        <?php if (!empty($ps['contact_phone'])): ?>
        <a href="tel:<?= htmlspecialchars($ps['contact_phone']) ?>">اتصال مباشر: <?= htmlspecialchars($ps['contact_phone']) ?></a>
        <?php endif; ?>
      </div>
      <div class="footer__col">
        <h4>المطور</h4>
        <div style="color:var(--ink); font-weight:700; font-size:var(--step--1); margin-bottom:4px;"><?= htmlspecialchars($ps['developer_name']) ?></div>
        <div style="color:var(--muted); font-size:var(--step--2);"><?= htmlspecialchars($ps['developer_company']) ?></div>
      </div>
    </div>
    <div class="footer__bottom">
      <span>© <?= date('Y') ?> مُسَرِّب • جميع الحقوق محفوظة.</span>
      <span>تطوير وإشراف: <strong><?= htmlspecialchars($ps['developer_name']) ?></strong> (<?= htmlspecialchars($ps['developer_company']) ?>)</span>
    </div>
  </div>
</footer>

<!-- Interactive Subscription Order Modal -->
<div class="modal" id="payModal" role="dialog" aria-modal="true" aria-labelledby="modalTitle" hidden>
  <div class="modal__backdrop" data-modal-close></div>
  <div class="modal__card">
    <button class="modal__close" aria-label="إغلاق" data-modal-close>
      <svg><use href="#i-x"/></svg>
    </button>
    <div id="modalBodyContent">
      <h2 class="modal__title" id="modalTitle">طلب تفعيل رخصة مُسَرِّب</h2>
      <p class="modal__desc">اختر حساب التحويل المعتمد، قم بالإيداع، ثم أرسل بياناتك للتفعيل الفوري.</p>

      <div id="modalFeedback" style="display:none;" class="modal-feedback"></div>

      <!-- Payment Method Tabs -->
      <?php if (!empty($paymentMethods)): ?>
      <div class="method-toggle" id="payMethodToggle">
        <?php foreach ($paymentMethods as $idx => $pm): ?>
        <label class="<?= $idx === 0 ? 'active' : '' ?>" data-idx="<?= $idx ?>" data-num="<?= htmlspecialchars($pm['account_number']) ?>" data-holder="<?= htmlspecialchars($pm['account_holder'] ?? 'عمر الحومي') ?>">
          <input type="radio" name="pay-method" value="<?= htmlspecialchars($pm['bank_name']) ?>" <?= $idx === 0 ? 'checked' : '' ?>>
          <?= htmlspecialchars($pm['bank_name']) ?>
        </label>
        <?php endforeach; ?>
      </div>
      
      <div class="account-info">
        <div class="account-row">
          <span class="account-row__label">اسم المستفيد:</span>
          <span style="font-weight:700;" id="accHolderVal"><?= htmlspecialchars($paymentMethods[0]['account_holder'] ?? 'عمر عبدالله علي أحمد ثابت') ?></span>
        </div>
        <div class="account-row">
          <span class="account-row__label">رقم الحساب / المحفظة:</span>
          <div style="display:flex; align-items:center; gap:10px;">
            <span class="account-row__value" id="accVal"><?= htmlspecialchars($paymentMethods[0]['account_number'] ?? '3079300437') ?></span>
            <button type="button" class="copy-btn" id="copyBtn">نسخ</button>
          </div>
        </div>
      </div>
      <?php else: ?>
      <div class="method-toggle">
        <label class="active" data-num="3079300437" data-holder="عمر عبدالله علي أحمد ثابت">
          <input type="radio" name="pay-method" value="kuraimi" checked>بنك الكريمي
        </label>
        <label data-num="773712030" data-holder="عمر عبدالله علي أحمد ثابت">
          <input type="radio" name="pay-method" value="jawwal">محفظة جيب
        </label>
      </div>
      <div class="account-info">
        <div class="account-row">
          <span class="account-row__label">اسم المستفيد:</span>
          <span style="font-weight:700;" id="accHolderVal">عمر عبدالله علي أحمد ثابت</span>
        </div>
        <div class="account-row">
          <span class="account-row__label">رقم الحساب:</span>
          <div style="display:flex; align-items:center; gap:10px;">
            <span class="account-row__value" id="accVal">3079300437</span>
            <button type="button" class="copy-btn" id="copyBtn">نسخ</button>
          </div>
        </div>
      </div>
      <?php endif; ?>

      <form id="orderForm" onsubmit="submitOrder(event)">
        <input type="hidden" id="selectedPlan" value="YEARLY">
        
        <div class="form-group">
          <label for="subName">الاسم الكامل لمالك البئر أو الموزع *</label>
          <input type="text" id="subName" required placeholder="مثال: سعيد بن حسين الحميلي">
        </div>
        <div class="form-group">
          <label for="subPhone">رقم الهاتف (واتساب) للتواصل والتفعيل *</label>
          <input type="tel" id="subPhone" required placeholder="مثال: 773712030">
        </div>
        <div class="form-group">
          <label for="subRegion">المحافظة / المنطقة الزراعية</label>
          <input type="text" id="subRegion" placeholder="مثال: الحيمة، صنعاء، تعز، مأرب...">
        </div>
        <div class="form-group">
          <label for="subReceipt">رقم سند الإيداع أو الحوالة (إن وُجد)</label>
          <input type="text" id="subReceipt" placeholder="رقم المرجع أو إشعار التحويل">
        </div>
        <div class="form-group">
          <label for="subDevice">كود جهازك من التطبيق (اختياري للتفعيل الفوري)</label>
          <input type="text" id="subDevice" placeholder="MSRB-XXXX-XXXX">
        </div>

        <button type="submit" id="btnSubmitOrder" class="btn btn-primary btn-block btn-lg" style="margin-top:10px;">
          إرسال طلب التفعيل المباشر ⚡
        </button>
      </form>

      <div style="text-align:center; margin-top:14px;">
        <button type="button" onclick="sendOrderWhatsApp()" class="btn btn-ghost btn-block" style="font-size:var(--step--1);">
          أو إرسال الطلب والتواصل مباشرة عبر واتساب 💬
        </button>
      </div>
    </div>
  </div>
</div>

<script>
(function () {
  'use strict';

  /* Theme Switcher */
  const root = document.documentElement;
  const themeToggle = document.getElementById('themeToggle');
  if (themeToggle) {
    themeToggle.addEventListener('click', () => {
      const next = root.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
      root.setAttribute('data-theme', next);
      try { localStorage.setItem('musarib-theme', next); } catch (e) {}
    });
  }

  /* Sticky Header */
  const header = document.getElementById('siteHeader');
  let ticking = false;
  window.addEventListener('scroll', () => {
    if (!ticking) {
      requestAnimationFrame(() => {
        if (header) header.classList.toggle('scrolled', window.scrollY > 8);
        ticking = false;
      });
      ticking = true;
    }
  }, { passive: true });

  /* Mobile Drawer Menu */
  const menuBtn = document.getElementById('menuBtn');
  const mobileMenu = document.getElementById('mobileMenu');
  if (menuBtn && mobileMenu) {
    menuBtn.addEventListener('click', () => {
      const open = mobileMenu.classList.toggle('is-open');
      menuBtn.setAttribute('aria-expanded', String(open));
    });
    mobileMenu.querySelectorAll('[data-menu-close]').forEach(el => {
      el.addEventListener('click', () => {
        mobileMenu.classList.remove('is-open');
        menuBtn.setAttribute('aria-expanded', 'false');
      });
    });
  }

  /* 3D Phone Tilt Parallax with Mouse Follow */
  const heroVisual = document.getElementById('heroVisual');
  const device = document.getElementById('device');
  const supportsHover = matchMedia('(hover: hover) and (pointer: fine)').matches;
  const reduceMotion = matchMedia('(prefers-reduced-motion: reduce)').matches;

  if (supportsHover && !reduceMotion && heroVisual && device) {
    let raf = null;
    heroVisual.addEventListener('mousemove', (e) => {
      if (raf) return;
      raf = requestAnimationFrame(() => {
        const rect = heroVisual.getBoundingClientRect();
        const x = ((e.clientX - rect.left) / rect.width - 0.5) * 2;
        const y = ((e.clientY - rect.top) / rect.height - 0.5) * 2;
        device.style.setProperty('--mx', x.toFixed(3));
        device.style.setProperty('--my', y.toFixed(3));
        raf = null;
      });
    });
    heroVisual.addEventListener('mouseleave', () => {
      device.style.setProperty('--mx', '0');
      device.style.setProperty('--my', '0');
    });
  }

  /* Scroll Reveal */
  const io = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('is-in');
        io.unobserve(entry.target);
      }
    });
  }, { threshold: 0.12, rootMargin: '0px 0px -50px 0px' });
  document.querySelectorAll('.reveal').forEach(el => io.observe(el));

  /* Number Counters Animation */
  const counters = document.querySelectorAll('.stat-num');
  const counterIO = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        animateCounter(entry.target);
        counterIO.unobserve(entry.target);
      }
    });
  }, { threshold: 0.5 });
  counters.forEach(el => counterIO.observe(el));

  function animateCounter(el) {
    const target = parseFloat(el.dataset.target);
    const decimals = parseInt(el.dataset.decimal || '0', 10);
    const duration = 1600;
    const start = performance.now();
    function step(now) {
      const progress = Math.min((now - start) / duration, 1);
      const eased = 1 - Math.pow(1 - progress, 3);
      const val = target * eased;
      el.textContent = decimals > 0
        ? val.toFixed(decimals)
        : Math.floor(val).toLocaleString('en-US');
      if (progress < 1) requestAnimationFrame(step);
    }
    requestAnimationFrame(step);
  }

  /* Chart Animation in Phone */
  const chart = document.querySelector('.chart');
  if (chart) {
    const chartObs = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('is-in');
          chartObs.unobserve(entry.target);
        }
      });
    }, { threshold: 0.3 });
    chartObs.observe(chart);
  }

  /* Modal Management */
  const modal = document.getElementById('payModal');
  window.openModal = function (plan = 'YEARLY') {
    const selPlan = document.getElementById('selectedPlan');
    if (selPlan) selPlan.value = plan;
    if (modal) {
      modal.hidden = false;
      requestAnimationFrame(() => modal.classList.add('open'));
      document.body.style.overflow = 'hidden';
    }
  };

  window.closeModal = function () {
    if (modal) {
      modal.classList.remove('open');
      document.body.style.overflow = '';
      setTimeout(() => { modal.hidden = true; }, 350);
    }
  };

  if (modal) {
    modal.addEventListener('click', (e) => {
      if (e.target.closest('[data-modal-close]')) closeModal();
    });
  }

  document.addEventListener('keydown', (e) => {
    if (modal && modal.classList.contains('open') && e.key === 'Escape') {
      closeModal();
    }
  });

  /* Payment Method Toggle */
  document.querySelectorAll('.method-toggle label').forEach(label => {
    label.addEventListener('click', () => {
      document.querySelectorAll('.method-toggle label').forEach(l => l.classList.remove('active'));
      label.classList.add('active');
      const num = label.dataset.num;
      const holder = label.dataset.holder;
      const valEl = document.getElementById('accVal');
      const holderEl = document.getElementById('accHolderVal');
      if (valEl && num) valEl.textContent = num;
      if (holderEl && holder) holderEl.textContent = holder;
    });
  });

  /* 1-Click Clipboard Copy */
  const copyBtn = document.getElementById('copyBtn');
  if (copyBtn) {
    copyBtn.addEventListener('click', async () => {
      const valEl = document.getElementById('accVal');
      const val = valEl ? valEl.textContent.trim() : '';
      if (!val) return;
      try {
        await navigator.clipboard.writeText(val);
        copyBtn.textContent = '✓ تم النسخ';
        copyBtn.classList.add('is-copied');
        setTimeout(() => {
          copyBtn.textContent = 'نسخ';
          copyBtn.classList.remove('is-copied');
        }, 1800);
      } catch (_) {
        const ta = document.createElement('textarea');
        ta.value = val;
        document.body.appendChild(ta);
        ta.select();
        document.execCommand('copy');
        document.body.removeChild(ta);
        copyBtn.textContent = '✓ تم النسخ';
        setTimeout(() => { copyBtn.textContent = 'نسخ'; }, 1800);
      }
    });
  }

  /* Live Data Sync with Database upon page load */
  async function syncLiveData() {
    try {
      const res = await fetch('api/public_data.php?t=' + Date.now());
      const json = await res.json();
      if (json && json.data) {
        const s = json.data.portal_settings || {};
        const p = json.data.subscription_pricing || {};

        if (s.active_region_name && document.getElementById('regionNameEl')) document.getElementById('regionNameEl').textContent = s.active_region_name;
        if (s.avg_hourly_price && document.getElementById('avgPriceEl')) document.getElementById('avgPriceEl').textContent = Number(s.avg_hourly_price).toLocaleString();
        if (s.min_hourly_price && s.max_hourly_price && document.getElementById('minPriceEl')) {
          document.getElementById('minPriceEl').innerHTML = 'أعلى: ' + Number(s.max_hourly_price).toLocaleString() + ' • أقل: ' + Number(s.min_hourly_price).toLocaleString() + ' ريال';
        }
        if (s.hero_badge && document.getElementById('heroBadgeText')) document.getElementById('heroBadgeText').textContent = s.hero_badge;
        if (s.headline_alert && document.getElementById('topTickerText')) document.getElementById('topTickerText').textContent = s.headline_alert;
        if (s.water_status_text && document.getElementById('waterStatusEl')) document.getElementById('waterStatusEl').textContent = '💧 ' + s.water_status_text;
        if (s.rates_note && document.getElementById('ratesNoteEl')) document.getElementById('ratesNoteEl').textContent = 'ℹ️ ' + s.rates_note;

        if (p.yearly && p.yearly.price && document.getElementById('yearlyPriceEl')) {
          document.getElementById('yearlyPriceEl').textContent = p.yearly.price + (p.yearly.currency || '$');
        }
        if (p.monthly && p.monthly.price && document.getElementById('monthlyPriceEl')) {
          document.getElementById('monthlyPriceEl').textContent = p.monthly.price + (p.monthly.currency || '$');
        }
      }
    } catch (_) {}
  }
  document.addEventListener('DOMContentLoaded', syncLiveData);

  /* Submit Order via API */
  window.submitOrder = async function (e) {
    e.preventDefault();
    const btn = document.getElementById('btnSubmitOrder');
    const fb = document.getElementById('modalFeedback');
    btn.disabled = true;
    btn.textContent = 'جاري إرسال الطلب... ⏳';

    try {
      const activeMethod = document.querySelector('.method-toggle label.active input')?.value || 'حوالة بنكية';
      const res = await fetch('api/public_data.php', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          action: 'submit_subscription_request',
          subscriber_name: document.getElementById('subName').value.trim(),
          phone: document.getElementById('subPhone').value.trim(),
          region: document.getElementById('subRegion').value.trim(),
          plan: document.getElementById('selectedPlan').value,
          payment_method: activeMethod,
          receipt_number: document.getElementById('subReceipt').value.trim(),
          device_code: document.getElementById('subDevice').value.trim()
        })
      });
      const data = await res.json();
      fb.style.display = 'block';
      if (data.success) {
        fb.style.background = 'rgba(5, 150, 105, 0.12)';
        fb.style.color = 'var(--success)';
        fb.innerHTML = `✅ ${data.message}`;
        document.getElementById('orderForm').reset();
      } else {
        fb.style.background = 'rgba(220, 38, 38, 0.12)';
        fb.style.color = 'var(--danger)';
        fb.textContent = '❌ ' + (data.message || 'تعذر إرسال الطلب');
      }
    } catch (err) {
      fb.style.display = 'block';
      fb.style.background = 'rgba(220, 38, 38, 0.12)';
      fb.style.color = 'var(--danger)';
      fb.textContent = '❌ تعذر الاتصال بالسيرفر. يمكنك استخدام زر الإرسال عبر واتساب بالأسفل.';
    } finally {
      btn.disabled = false;
      btn.textContent = 'إرسال طلب التفعيل المباشر ⚡';
    }
  };

  /* Send WhatsApp Order with Dynamic Prices */
  window.sendOrderWhatsApp = function () {
    const name = document.getElementById('subName').value.trim() || 'مشترك جديد';
    const phone = document.getElementById('subPhone').value.trim() || '';
    const isYearly = document.getElementById('selectedPlan').value === 'YEARLY';
    const yearlyPriceTxt = document.getElementById('yearlyPriceEl') ? document.getElementById('yearlyPriceEl').textContent.trim() : '75$';
    const monthlyPriceTxt = document.getElementById('monthlyPriceEl') ? document.getElementById('monthlyPriceEl').textContent.trim() : '8$';
    const plan = isYearly ? `سنوي (${yearlyPriceTxt})` : `شهري (${monthlyPriceTxt})`;
    const region = document.getElementById('subRegion').value.trim() || '';
    const receipt = document.getElementById('subReceipt').value.trim() || 'قيد التحويل';
    const device = document.getElementById('subDevice').value.trim() || '';

    const msg = `السلام عليكم يا مهندس عمر، أود تفعيل رخصة تطبيق مُسَرِّب:
• نوع الباقة: ${plan}
• الاسم: ${name}
• الهاتف: ${phone}
• المنطقة: ${region}
• سند التحويل: ${receipt}
${device ? `• كود جهازي: ${device}` : ''}`;

    window.open(`https://wa.me/<?= preg_replace('/[^0-9]/', '', $ps['contact_whatsapp']) ?>?text=${encodeURIComponent(msg)}`, '_blank');
  };

  /* Track Download via SendBeacon */
  window.trackDownload = function () {
    try {
      const url = 'api/public_data.php?action=track_download';
      if (navigator.sendBeacon) {
        navigator.sendBeacon(url);
      } else {
        fetch(url, { method: 'POST', keepalive: true }).catch(() => {});
      }
    } catch (_) {}
  };
})();
</script>
</body>
</html>
