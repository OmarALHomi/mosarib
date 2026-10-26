<?php
/**
 * Mosarib Official Public Portal
 * Clean Minimalist Architecture & Live Database Sync
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
    'hero_badge' => 'المنظومة الميدانية لإدارة مياه الآبار',
    'hero_title' => 'مُسَرِّب • حساب دقيق لمياه وساعات الري وحصص المزارعين',
    'hero_subtitle' => 'تطبيق ذكي خفيف يعمل بدون إنترنت (Offline-First)، يدير جلسات الري بالثانية والدقيقة، يرسل سندات القبض عبر واتساب، ويحفظ بياناتك بأمان على Google Drive.',
    'contact_whatsapp' => '967773712030',
    'contact_phone' => '773712030',
    'developer_name' => 'المهندس عمر الحومي',
    'developer_company' => 'تُبع سوفت للحلول البرمجية الذكية (TubbaSoft)',
    'show_top_ticker' => true,
    'show_rates_section' => true,
    'show_features_section' => true,
    'show_plans_section' => true
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
?>
<!DOCTYPE html>
<html lang="ar" dir="rtl" data-theme="light">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title><?= htmlspecialchars($ps['hero_title']) ?> • البوابة الرسمية</title>
  <meta name="description" content="<?= htmlspecialchars($ps['hero_subtitle']) ?>">
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Cairo:wght@400;600;700;800;900&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="assets/css/portal.css?v=<?= filemtime(__DIR__ . '/assets/css/portal.css') ?>">
  <script>
    (function() {
      try {
        const t = localStorage.getItem('mosarib_portal_theme') || 'light';
        document.documentElement.setAttribute('data-theme', t);
        document.addEventListener('DOMContentLoaded', function() {
          const ic = document.getElementById('themeToggleIcon');
          if (ic) ic.textContent = t === 'dark' ? '☀️' : '🌙';
        });
      } catch(e) {}
    })();
  </script>
</head>
<body>

  <!-- Top Ticker / Headline Alert -->
  <?php if (!empty($ps['headline_alert']) && ($ps['show_top_ticker'] ?? true)): ?>
  <div class="top-ticker" id="topTickerBar">
    <div class="container" style="display: flex; align-items: center; justify-content: center; gap: 8px;">
      <span>📢</span>
      <span id="topTickerText"><?= htmlspecialchars($ps['headline_alert']) ?></span>
    </div>
  </div>
  <?php endif; ?>

  <!-- Clean Header -->
  <header class="portal-nav">
    <div class="container nav-container">
      <a href="#" class="brand-wrapper">
        <div class="brand-badge">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"></path></svg>
        </div>
        <div>
          <div class="brand-title">مُسَرِّب • MOSARIB</div>
          <div class="brand-sub">إدارة مياه وساعات الري وحصص الآبار</div>
        </div>
      </a>

      <nav class="nav-menu" id="desktopNavMenu">
        <?php if (!empty($ps['show_features_section'])): ?>
        <a href="#features" class="nav-link">المميزات</a>
        <?php endif; ?>
        <?php if (!empty($ps['show_rates_section'])): ?>
        <a href="#rates" class="nav-link">متوسط الأسعار</a>
        <?php endif; ?>
        <?php if (!empty($ps['show_plans_section'])): ?>
        <a href="#plans" class="nav-link">الباقات والترخيص</a>
        <?php endif; ?>
        <?php if (!empty($portalNews)): ?>
        <a href="#news" class="nav-link">مستجدات المياه</a>
        <?php endif; ?>
      </nav>

      <div class="nav-actions">
        <button id="themeToggleBtn" onclick="togglePortalTheme()" class="btn-theme" title="تبديل المظهر">
          <span id="themeToggleIcon">🌙</span>
        </button>
        <a href="<?= htmlspecialchars($directDownloadUrl) ?>" class="btn-primary nav-download-btn" download onclick="trackDownload()">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>
          <span>تحميل التطبيق APK</span>
        </a>
        <button id="hamburgerBtn" class="btn-hamburger" onclick="toggleMobileNav()" aria-label="القائمة الرئيسية" aria-expanded="false">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.3" stroke-linecap="round" stroke-linejoin="round">
            <line x1="3" y1="12" x2="21" y2="12"></line>
            <line x1="3" y1="6" x2="21" y2="6"></line>
            <line x1="3" y1="18" x2="21" y2="18"></line>
          </svg>
        </button>
      </div>
    </div>
  </header>

  <!-- Mobile Navigation Drawer -->
  <div class="mobile-nav-backdrop" id="mobileNavBackdrop" onclick="closeMobileNav()"></div>
  <aside class="mobile-nav-drawer" id="mobileNavDrawer" aria-hidden="true" dir="rtl">
    <div class="mobile-nav-header">
      <div class="brand-wrapper">
        <div class="brand-badge">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"></path></svg>
        </div>
        <div>
          <div class="brand-title">مُسَرِّب • MOSARIB</div>
          <div class="brand-sub">إدارة مياه وساعات الري وحصص الآبار</div>
        </div>
      </div>
      <button class="mobile-nav-close" onclick="closeMobileNav()" aria-label="إغلاق القائمة">&times;</button>
    </div>
    <div class="mobile-nav-body">
      <?php if (!empty($ps['show_features_section'])): ?>
      <a href="#features" class="mobile-nav-link" onclick="closeMobileNav()">
        <span class="nav-icon">✨</span>
        <span>المميزات الميدانية</span>
      </a>
      <?php endif; ?>
      <?php if (!empty($ps['show_rates_section'])): ?>
      <a href="#rates" class="mobile-nav-link" onclick="closeMobileNav()">
        <span class="nav-icon">📊</span>
        <span>مؤشر ومتوسط الأسعار</span>
      </a>
      <?php endif; ?>
      <?php if (!empty($ps['show_plans_section'])): ?>
      <a href="#plans" class="mobile-nav-link" onclick="closeMobileNav()">
        <span class="nav-icon">⚡</span>
        <span>الباقات والترخيص</span>
      </a>
      <?php endif; ?>
      <?php if (!empty($portalNews)): ?>
      <a href="#news" class="mobile-nav-link" onclick="closeMobileNav()">
        <span class="nav-icon">📰</span>
        <span>مستجدات وبورصة المياه</span>
      </a>
      <?php endif; ?>

      <div class="mobile-nav-divider"></div>

      <a href="<?= htmlspecialchars($directDownloadUrl) ?>" class="btn-primary mobile-nav-cta" download onclick="trackDownload(); closeMobileNav();">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>
        <span>تحميل التطبيق مجاناً (APK)</span>
      </a>

      <button onclick="openModal('YEARLY'); closeMobileNav();" class="btn-secondary mobile-nav-cta">
        <span>طلب تفعيل الترخيص ⚡</span>
      </button>

      <?php if (!empty($ps['contact_whatsapp'])): ?>
      <a href="https://wa.me/<?= preg_replace('/[^0-9]/', '', $ps['contact_whatsapp']) ?>" target="_blank" class="btn-outline mobile-nav-cta" onclick="closeMobileNav()">
        <span>محادثة واتساب 💬</span>
      </a>
      <?php endif; ?>
    </div>
  </aside>

  <!-- Hero Section -->
  <section class="hero container">
    <div class="hero-pill" id="heroBadgeEl">
      <span>💧</span>
      <span id="heroBadgeText"><?= htmlspecialchars($ps['hero_badge']) ?></span>
    </div>
    <h1 id="heroTitleText"><?= htmlspecialchars($ps['hero_title']) ?></h1>
    <p id="heroSubtitleText"><?= htmlspecialchars($ps['hero_subtitle']) ?></p>

    <div class="hero-cta">
      <a href="<?= htmlspecialchars($directDownloadUrl) ?>" class="btn-primary" download onclick="trackDownload()">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>
        <span>تحميل التطبيق مجاناً (APK)</span>
      </a>
      <button onclick="openModal('YEARLY')" class="btn-secondary">
        <span>طلب تفعيل الترخيص ⚡</span>
      </button>
      <?php if (!empty($ps['contact_whatsapp'])): ?>
      <a href="https://wa.me/<?= preg_replace('/[^0-9]/', '', $ps['contact_whatsapp']) ?>" target="_blank" class="btn-outline">
        <span>محادثة واتساب 💬</span>
      </a>
      <?php endif; ?>
    </div>

    <div class="trust-badges">
      <div class="trust-item"><span class="trust-icon">⚡</span> <span>يعمل 100% بدون إنترنت</span></div>
      <div class="trust-item"><span class="trust-icon">🧾</span> <span>سندات قبض فورية عبر واتساب</span></div>
      <div class="trust-item"><span class="trust-icon">☁️</span> <span>نسخ احتياطي آمن على Google Drive</span></div>
    </div>
  </section>

  <!-- Hourly Rates Strip -->
  <?php if (!empty($ps['show_rates_section'])): ?>
  <div class="container" id="rates" style="margin-bottom: 35px;">
    <div class="rates-strip">
      <div class="rates-col rates-location">
        <div class="rates-location-icon">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
            <circle cx="12" cy="10" r="3"></circle>
          </svg>
        </div>
        <div>
          <div class="rates-sub-lbl">المنطقة الجغرافية المعتمدة:</div>
          <strong class="rates-reg-name" id="regionNameEl"><?= htmlspecialchars($ps['active_region_name']) ?></strong>
        </div>
      </div>
      <div class="rates-col rates-prices">
        <div class="price-item-lbl">متوسط سعر الساعة</div>
        <div class="rates-main-price">
          <span class="price-item-val" id="avgPriceEl"><?= number_format($ps['avg_hourly_price']) ?></span>
          <span class="price-item-cur"><?= htmlspecialchars($ps['currency_symbol']) ?></span>
        </div>
        <div class="rates-range-tag" id="minPriceEl">
          <span>أعلى: <?= number_format($ps['max_hourly_price']) ?> ريال</span>
          <span class="rates-sep">•</span>
          <span>أقل: <?= number_format($ps['min_hourly_price']) ?> ريال</span>
        </div>
      </div>
      <div class="rates-col rates-status-box">
        <div class="rates-sub-lbl">حالة منسوب المياه والضخ:</div>
        <div class="rates-status-pill" id="waterStatusEl">💧 <?= htmlspecialchars($ps['water_status_text'] ?? 'مستقر ومتاح في معظم المناطق الزراعية') ?></div>
      </div>
    </div>
    <div class="rates-note-msg" id="ratesNoteEl">
      ℹ️ <?= htmlspecialchars($ps['rates_note'] ?? 'الأسعار تختلف بحسب عمق البئر ونوع الطاقة المستخدمة') ?>
    </div>
  </div>
  <?php endif; ?>

  <!-- Features Grid -->
  <?php if (!empty($ps['show_features_section'])): ?>
  <section class="section container" id="features">
    <div class="section-head">
      <h2>مزايا ميدانية لحفظ حقوقك</h2>
      <p>مصمم خصيصاً لملاك الآبار وموزعي مياه الري للعمل بأعلى دقة وسهولة</p>
    </div>
    <div class="features-grid">
      <div class="feature-box">
        <div class="feature-icon">⏱️</div>
        <h3>حساب بالثواني والدقائق</h3>
        <p>احتساب دقيق لكسور ساعات الضخ وحصص المزارعين مع ضبط الفاقد الزمني تلقائياً دون أي حسابات يدوية معقدة.</p>
      </div>
      <div class="feature-box">
        <div class="feature-icon">🧾</div>
        <h3>سندات قبض وتقارير فورية</h3>
        <p>إصدار سندات مالية وطباعتها أو إرسالها موثقة للمزارعين مباشرة عبر تطبيق واتساب فور انتهاء دورة السقي.</p>
      </div>
      <div class="feature-box">
        <div class="feature-icon">🛡️</div>
        <h3>أمان تام بدون إنترنت</h3>
        <p>يعمل في المزارع والمناطق النائية بدون شبكة، مع ميزة المزامنة والنسخ الاحتياطي السحابي على Google Drive.</p>
      </div>
    </div>
  </section>
  <?php endif; ?>

  <!-- Pricing Plans Section -->
  <?php if (!empty($ps['show_plans_section'])): ?>
  <section class="section container" id="plans">
    <div class="section-head">
      <h2>باقات الاشتراك والترخيص</h2>
      <p>اختر الباقة المناسبة لمزرعتك وبئرك مع إمكانية التفعيل الفوري</p>
    </div>

    <div class="pricing-grid">
      <!-- Annual Plan (Recommended) -->
      <div class="plan-card popular">
        <div class="popular-tag"><?= htmlspecialchars($yearlyDisc) ?></div>
        <div class="plan-card-body">
          <div class="plan-head">
            <h3>الاشتراك السنوي</h3>
            <div class="plan-desc">الترخيص الكامل لموسم زراعي متكامل (365 يوماً)</div>
          </div>
          <div class="plan-price-box">
            <div class="plan-price-main">
              <span class="plan-price-num" id="yearlyPriceEl"><?= htmlspecialchars($yearlyPrice) ?><?= htmlspecialchars($yearlyCurrency) ?></span>
              <span class="plan-price-period">/ سنوياً</span>
            </div>
            <?php if (!empty($yearlyOrig)): ?>
            <span class="plan-discount" id="yearlyOrigEl">بدلاً من <?= htmlspecialchars($yearlyOrig) ?><?= htmlspecialchars($yearlyCurrency) ?></span>
            <?php endif; ?>
          </div>
          <ul class="plan-features">
            <li><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg> <span>عمليات ري وسندات وحسابات غير محدودة</span></li>
            <li><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg> <span>نسخ احتياطي سحابي يومي على Google Drive</span></li>
            <li><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg> <span>تصدير كشوفات الحسابات PDF وإكسل Excel</span></li>
            <li><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg> <span>دعم فني وإمكانية نقل الرخصة لهاتف بديل</span></li>
          </ul>
        </div>
        <button onclick="openModal('YEARLY')" class="btn-primary plan-btn">طلب تفعيل الباقة السنوية ⚡</button>
      </div>

      <!-- Monthly Plan -->
      <div class="plan-card">
        <div class="plan-card-body">
          <div class="plan-head">
            <h3>الاشتراك الشهري</h3>
            <div class="plan-desc">مرونة كاملة لتشغيل المواسم الزراعية القصيرة</div>
          </div>
          <div class="plan-price-box">
            <div class="plan-price-main">
              <span class="plan-price-num" id="monthlyPriceEl"><?= htmlspecialchars($monthlyPrice) ?><?= htmlspecialchars($monthlyCurrency) ?></span>
              <span class="plan-price-period">/ شهرياً</span>
            </div>
          </div>
          <ul class="plan-features">
            <li><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg> <span>صلاحية كاملة لكافة المزايا لمدة 30 يوماً</span></li>
            <li><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg> <span>إصدار ومشاركة سندات القبض عبر واتساب</span></li>
            <li><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg> <span>حفظ وتأمين البيانات محلياً على الجهاز</span></li>
          </ul>
        </div>
        <button onclick="openModal('MONTHLY')" class="btn-outline plan-btn">طلب تفعيل الباقة الشهرية</button>
      </div>
    </div>
  </section>
  <?php endif; ?>

  <!-- Agricultural News & Water Reports -->
  <?php if (!empty($portalNews)): ?>
  <section class="section container" id="news" style="margin-bottom: 40px;">
    <div class="section-head">
      <h2>مستجدات وبورصة مياه الآبار</h2>
      <p>تقارير ميدانية دورية حول حركة أسعار الري ومعدلات الضخ المائي</p>
    </div>
    <div class="news-grid">
      <?php foreach (array_slice($portalNews, 0, 3) as $nw): ?>
      <div class="news-card">
        <div class="news-body-content">
          <span class="news-tag"><?= htmlspecialchars($nw['category'] ?? 'تقارير الآبار') ?></span>
          <h3 class="news-title"><?= htmlspecialchars($nw['title']) ?></h3>
          <p class="news-desc"><?= htmlspecialchars($nw['summary']) ?></p>
        </div>
      </div>
      <?php endforeach; ?>
    </div>
  </section>
  <?php endif; ?>

  <!-- Footer -->
  <footer class="portal-footer">
    <div class="container footer-content-wrap">
      <div class="footer-logo-row">
        <span class="footer-brand-title">مُسَرِّب • MOSARIB</span>
        <span class="footer-tagline">منظومة إدارة مياه وساعات الري وحصص الآبار</span>
      </div>
      <div class="footer-copy-text">
        جميع الحقوق محفوظة &copy; <?= date('Y') ?> • تطوير <?= htmlspecialchars($ps['developer_name']) ?> (<?= htmlspecialchars($ps['developer_company']) ?>)
      </div>
    </div>
  </footer>

  <!-- Modal -->
  <div class="modal-overlay" id="orderModal">
    <div class="modal-content">
      <div class="modal-header">
        <h3 class="modal-title">طلب تفعيل اشتراك مُسَرِّب</h3>
        <button class="close-btn" onclick="closeModal()">&times;</button>
      </div>

      <div id="modalFeedback" style="display: none; padding: 10px; border-radius: 8px; font-size: 13px; margin-bottom: 14px;"></div>

      <?php if (!empty($paymentMethods)): ?>
      <div style="margin-bottom: 16px; background: var(--surface-alt); border: 1px solid var(--border); border-radius: var(--radius-sm); padding: 12px;">
        <div style="font-size: 12px; font-weight: 700; color: var(--text-muted); margin-bottom: 8px;">حسابات التحويل المعتمدة (اضغط للنسخ):</div>
        <?php foreach ($paymentMethods as $pm): ?>
        <div class="pay-pill" style="margin-bottom: 6px; padding: 6px 10px; cursor: pointer;" onclick="navigator.clipboard.writeText('<?= htmlspecialchars($pm['account_number']) ?>'); alert('تم نسخ رقم الحساب: <?= htmlspecialchars($pm['account_number']) ?>');" title="اضغط لنسخ رقم الحساب">
          <span class="pay-pill-bank"><?= htmlspecialchars($pm['bank_name']) ?> (<?= htmlspecialchars($pm['account_holder'] ?? 'عمر الحومي') ?>)</span>
          <span class="pay-pill-num">📋 <?= htmlspecialchars($pm['account_number']) ?></span>
        </div>
        <?php endforeach; ?>
      </div>
      <?php endif; ?>

      <form id="orderForm" onsubmit="submitOrder(event)">
        <input type="hidden" id="selectedPlan" value="YEARLY">
        
        <div class="form-group">
          <label>الاسم الكامل:</label>
          <input type="text" id="subName" required placeholder="اسم مالك البئر أو الموزع">
        </div>

        <div class="form-group">
          <label>رقم الهاتف (واتساب):</label>
          <input type="tel" id="subPhone" required placeholder="مثال: 773712030">
        </div>

        <div class="form-group">
          <label>المحافظة / المنطقة:</label>
          <input type="text" id="subRegion" placeholder="مثال: صنعاء، تعز، مأرب...">
        </div>

        <div class="form-group">
          <label>طريقة التحويل / المحفظة:</label>
          <select id="subMethod">
            <?php if (!empty($paymentMethods)): ?>
              <?php foreach ($paymentMethods as $pm): ?>
                <option value="<?= htmlspecialchars($pm['bank_name'] . ' - ' . $pm['account_number']) ?>">
                  <?= htmlspecialchars($pm['bank_name']) ?> (<?= htmlspecialchars($pm['account_number']) ?>)
                </option>
              <?php endforeach; ?>
            <?php else: ?>
              <option value="حوالة عبر الكريمي / النجم">حوالة عبر الكريمي / النجم</option>
              <option value="محفظة كاش / ون كاش">محفظة كاش / ون كاش / جيب</option>
            <?php endif; ?>
          </select>
        </div>

        <div class="form-group">
          <label>رقم سند الإيداع أو الحوالة (إن وُجد):</label>
          <input type="text" id="subReceipt" placeholder="رقم المرجع أو إشعار التحويل">
        </div>

        <div class="form-group">
          <label>كود جهازك من التطبيق (اختياري):</label>
          <input type="text" id="subDevice" placeholder="MSRB-XXXX-XXXX">
        </div>

        <button type="submit" id="btnSubmitOrder" class="btn-primary" style="width: 100%; justify-content: center; margin-top: 10px;">إرسال طلب التفعيل المباشر ⚡</button>
      </form>

      <div style="text-align: center; margin-top: 14px;">
        <button onclick="sendOrderWhatsApp()" class="btn-outline" style="width: 100%; justify-content: center; font-size: 13px;">أو إرسال الطلب عبر واتساب 💬</button>
      </div>
    </div>
  </div>

  <!-- Dynamic Live Sync Script -->
  <script>
    // 1. Mobile Drawer Navigation
    function toggleMobileNav() {
      const drawer = document.getElementById('mobileNavDrawer');
      const backdrop = document.getElementById('mobileNavBackdrop');
      const btn = document.getElementById('hamburgerBtn');
      if (!drawer || !backdrop) return;
      const isOpen = drawer.classList.contains('open');
      if (isOpen) {
        closeMobileNav();
      } else {
        drawer.classList.add('open');
        backdrop.classList.add('open');
        if (btn) btn.setAttribute('aria-expanded', 'true');
        document.body.style.overflow = 'hidden';
      }
    }

    function closeMobileNav() {
      const drawer = document.getElementById('mobileNavDrawer');
      const backdrop = document.getElementById('mobileNavBackdrop');
      const btn = document.getElementById('hamburgerBtn');
      if (drawer) drawer.classList.remove('open');
      if (backdrop) backdrop.classList.remove('open');
      if (btn) btn.setAttribute('aria-expanded', 'false');
      document.body.style.overflow = '';
    }

    window.addEventListener('keydown', function(e) {
      if (e.key === 'Escape') closeMobileNav();
    });

    window.addEventListener('resize', function() {
      if (window.innerWidth >= 768) closeMobileNav();
    });

    // 2. Theme Management
    function togglePortalTheme() {
      const cur = document.documentElement.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
      document.documentElement.setAttribute('data-theme', cur);
      document.getElementById('themeToggleIcon').textContent = cur === 'dark' ? '☀️' : '🌙';
      try { localStorage.setItem('mosarib_portal_theme', cur); } catch(e) {}
    }

    // 2. Modal Management
    function openModal(plan) {
      document.getElementById('selectedPlan').value = plan;
      document.getElementById('orderModal').style.display = 'flex';
    }
    function closeModal() {
      document.getElementById('orderModal').style.display = 'none';
      document.getElementById('modalFeedback').style.display = 'none';
    }
    window.onclick = function(e) {
      if (e.target === document.getElementById('orderModal')) closeModal();
    };

    // 3. Live Sync from Database / API upon page load
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
            document.getElementById('minPriceEl').innerHTML = '<span>أعلى: ' + Number(s.max_hourly_price).toLocaleString() + ' ريال</span><span class="rates-sep">•</span><span>أقل: ' + Number(s.min_hourly_price).toLocaleString() + ' ريال</span>';
          }
          if (s.hero_title && document.getElementById('heroTitleText')) document.getElementById('heroTitleText').textContent = s.hero_title;
          if (s.hero_subtitle && document.getElementById('heroSubtitleText')) document.getElementById('heroSubtitleText').textContent = s.hero_subtitle;
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

          if (json.data.app_release && json.data.app_release.download_url) {
            document.querySelectorAll('a.nav-download-btn, a.mobile-nav-cta, .hero-cta a.btn-primary').forEach(function(el) {
              if (el.tagName === 'A' && el.classList.contains('btn-primary')) {
                el.setAttribute('href', json.data.app_release.download_url);
              }
            });
          }
        }
      } catch(_) {}
    }
    document.addEventListener('DOMContentLoaded', syncLiveData);

    // 4. Submit Subscription Order
    async function submitOrder(e) {
      e.preventDefault();
      const btn = document.getElementById('btnSubmitOrder');
      const fb = document.getElementById('modalFeedback');
      btn.disabled = true;
      btn.textContent = 'جاري الإرسال... ⏳';

      try {
        const res = await fetch('api/public_data.php', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            action: 'submit_subscription_request',
            subscriber_name: document.getElementById('subName').value.trim(),
            phone: document.getElementById('subPhone').value.trim(),
            region: document.getElementById('subRegion').value.trim(),
            plan: document.getElementById('selectedPlan').value,
            payment_method: document.getElementById('subMethod').value,
            receipt_number: document.getElementById('subReceipt').value.trim(),
            device_code: document.getElementById('subDevice').value.trim()
          })
        });
        const data = await res.json();
        fb.style.display = 'block';
        if (data.success) {
          fb.style.background = 'var(--success-light)';
          fb.style.color = 'var(--success)';
          fb.innerHTML = `✅ ${data.message}`;
          document.getElementById('orderForm').reset();
        } else {
          fb.style.background = 'rgba(239, 68, 68, 0.1)';
          fb.style.color = '#EF4444';
          fb.textContent = '❌ ' + (data.message || 'تعذر إرسال الطلب');
        }
      } catch (err) {
        fb.style.display = 'block';
        fb.style.background = 'rgba(239, 68, 68, 0.1)';
        fb.style.color = '#EF4444';
        fb.textContent = '❌ تعذر الاتصال بالسيرفر. يمكنك استخدام زر واتساب بالأسفل.';
      } finally {
        btn.disabled = false;
        btn.textContent = 'إرسال طلب التفعيل المباشر ⚡';
      }
    }

    function sendOrderWhatsApp() {
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
    }

    function trackDownload() {
      try {
        const url = 'api/public_data.php?action=track_download';
        if (navigator.sendBeacon) {
          navigator.sendBeacon(url);
        } else {
          fetch(url, { method: 'POST', keepalive: true }).catch(() => {});
        }
      } catch(_) {}
    }
  </script>
</body>
</html>
