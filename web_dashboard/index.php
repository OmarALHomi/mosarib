<?php
/**
 * Mosarib Official Public Portal
 * Clean Minimalist Architecture & Live Database Sync
 * Developed for Eng. Omar AL-Homi (TubbaSoft 2026)
 */

header('Cache-Control: no-cache, no-store, must-revalidate, max-age=0');
header('Pragma: no-cache');
header('Expires: 0');
header('Content-Type: text/html; charset=UTF-8');

require_once __DIR__ . '/api/config.php';

// 1. Load Live Portal Settings
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
    'hero_badge' => 'المنظومة الرقمية الميدانية لإدارة مياه الآبار',
    'hero_title' => 'مُسَرِّب • حساب دقيق لمياه وساعات الري وحصص المزارعين',
    'hero_subtitle' => 'تطبيق ذكي خفيف يعمل 100% بدون إنترنت (Offline-First)، يدير جلسات الري بالثانية والدقيقة، يرسل سندات القبض عبر واتساب، ويحفظ بياناتك بأمان تام.',
    'contact_whatsapp' => '967773712030',
    'contact_phone' => '773712030',
    'developer_name' => 'المهندس عمر الحومي',
    'developer_company' => 'تُبع سوفت للحلول البرمجية الذكية (TubbaSoft)',
    'show_top_ticker' => true,
    'show_rates_section' => true,
    'show_plans_section' => true,
    'show_features_section' => true,
    'show_download_cta' => true
];

if (file_exists($settingsFile)) {
    $savedSettings = json_decode(@file_get_contents($settingsFile), true);
    if (is_array($savedSettings)) {
        $ps = array_merge($ps, $savedSettings);
    }
}

// 2. Load Real Latest Release Info
$releasesFile = __DIR__ . '/uploads/releases_index.json';
$releases = file_exists($releasesFile) ? (json_decode(@file_get_contents($releasesFile), true) ?: []) : [];
$latestRelease = !empty($releases[0]) ? $releases[0] : null;

$apkVersion = !empty($latestRelease['version_name']) ? $latestRelease['version_name'] : '1.4.0';
$apkSizeMb = !empty($latestRelease['file_size']) ? round($latestRelease['file_size'] / (1024 * 1024), 1) : 2.8;

// Primary download gateway
$downloadUrl = 'api/download.php';

// 3. Load Subscription Pricing
$pricingFile = __DIR__ . '/data/subscription_pricing.json';
$pricing = file_exists($pricingFile) ? (json_decode(@file_get_contents($pricingFile), true) ?: []) : [];
$monthlyPrice = $pricing['monthly']['price'] ?? '10';
$monthlyCurrency = $pricing['monthly']['currency'] ?? '$';
$yearlyPrice = $pricing['yearly']['price'] ?? '80';
$yearlyOrig = $pricing['yearly']['original_price'] ?? '120';
$yearlyDisc = $pricing['yearly']['discount_text'] ?? 'وفر 33% (خصم 40$)';
$yearlyCurrency = $pricing['yearly']['currency'] ?? '$';

// 4. Load Payment Methods
$paymentMethodsFile = __DIR__ . '/data/payment_methods.json';
$paymentMethods = file_exists($paymentMethodsFile) ? (json_decode(@file_get_contents($paymentMethodsFile), true) ?: []) : [];

$cleanWhatsapp = preg_replace('/[^0-9]/', '', $ps['contact_whatsapp'] ?: '967773712030');
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

  <!-- Top Announcement Bar (Optional) -->
  <?php if (!empty($ps['headline_alert']) && ($ps['show_top_ticker'] ?? true)): ?>
  <div style="background: var(--surface-alt); border-bottom: 1px solid var(--border); padding: 8px 16px; font-size: 13px; text-align: center; color: var(--text-main); font-weight: 700;">
    <div class="container" style="display: flex; align-items: center; justify-content: center; gap: 8px;">
      <span>📢</span>
      <span id="topTickerText"><?= htmlspecialchars($ps['headline_alert']) ?></span>
    </div>
  </div>
  <?php endif; ?>

  <!-- Navbar -->
  <header class="portal-nav">
    <div class="container nav-container">
      <a href="#" class="brand-wrapper">
        <div class="brand-badge">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"></path></svg>
        </div>
        <div>
          <div class="brand-title">
            <span>مُسَرِّب</span>
            <span class="badge-tag">APK مباشر</span>
          </div>
          <div class="brand-sub">إدارة مياه وساعات الري وحصص الآبار</div>
        </div>
      </a>

      <nav class="nav-menu">
        <a href="#features" class="nav-link">المميزات</a>
        <a href="#how" class="nav-link">طريقة الاستخدام</a>
        <?php if (!empty($ps['show_rates_section'])): ?>
        <a href="#rates" class="nav-link">مؤشر الأسعار</a>
        <?php endif; ?>
        <?php if (!empty($ps['show_plans_section'])): ?>
        <a href="#plans" class="nav-link">الترخيص والباقات</a>
        <?php endif; ?>
      </nav>

      <div class="nav-actions">
        <button id="themeToggleBtn" onclick="togglePortalTheme()" class="btn-theme" title="تبديل المظهر">
          <span id="themeToggleIcon">🌙</span>
        </button>
        <a href="<?= $downloadUrl ?>" class="btn-primary nav-download-btn" onclick="trackDownload()">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>
          <span>تحميل APK</span>
        </a>
        <button id="hamburgerBtn" class="btn-hamburger" onclick="toggleMobileNav()" aria-label="القائمة">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><line x1="3" y1="12" x2="21" y2="12"></line><line x1="3" y1="6" x2="21" y2="6"></line><line x1="3" y1="18" x2="21" y2="18"></line></svg>
        </button>
      </div>
    </div>
  </header>

  <!-- Mobile Drawer -->
  <div class="mobile-nav-backdrop" id="mobileNavBackdrop" onclick="closeMobileNav()"></div>
  <aside class="mobile-nav-drawer" id="mobileNavDrawer" aria-hidden="true" dir="rtl">
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
      <span style="font-weight: 800; font-size: 16px;">القائمة الرئيسية</span>
      <button onclick="closeMobileNav()" style="background: none; border: none; font-size: 24px; cursor: pointer;">&times;</button>
    </div>
    <a href="#features" class="mobile-nav-link" onclick="closeMobileNav()">المميزات الميدانية</a>
    <a href="#how" class="mobile-nav-link" onclick="closeMobileNav()">طريقة الاستخدام</a>
    <?php if (!empty($ps['show_rates_section'])): ?>
    <a href="#rates" class="mobile-nav-link" onclick="closeMobileNav()">مؤشر الأسعار</a>
    <?php endif; ?>
    <?php if (!empty($ps['show_plans_section'])): ?>
    <a href="#plans" class="mobile-nav-link" onclick="closeMobileNav()">الباقات والترخيص</a>
    <?php endif; ?>
    <div style="margin-top: auto; display: flex; flex-direction: column; gap: 10px; padding-top: 20px;">
      <a href="<?= $downloadUrl ?>" class="btn-primary" style="justify-content: center;" onclick="trackDownload(); closeMobileNav();">تحميل التطبيق APK</a>
      <a href="https://wa.me/<?= $cleanWhatsapp ?>" target="_blank" class="btn-whatsapp" style="justify-content: center;">محادثة واتساب 💬</a>
    </div>
  </aside>

  <!-- Hero Section -->
  <section class="hero-section container">
    <div class="hero-grid">
      <div class="hero-content">
        <div class="hero-pill" id="heroBadgeEl">
          <span>💧</span>
          <span id="heroBadgeText"><?= htmlspecialchars($ps['hero_badge']) ?></span>
        </div>
        <h1 class="hero-title" id="heroTitleText">
          <?= htmlspecialchars($ps['hero_title']) ?>
        </h1>
        <p class="hero-subtitle" id="heroSubtitleText">
          <?= htmlspecialchars($ps['hero_subtitle']) ?>
        </p>

        <div class="hero-cta-box">
          <div class="hero-buttons-row">
            <a href="<?= $downloadUrl ?>" class="btn-primary hero-btn-main" id="mainDownloadBtn" onclick="trackDownload()">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>
              <span>تحميل التطبيق مجاناً (APK)</span>
            </a>
            <a href="https://wa.me/<?= $cleanWhatsapp ?>?text=<?= rawurlencode('السلام عليكم يا مهندس عمر، أود الاستفسار عن تطبيق مسرب لإدارة مياه الآبار') ?>" target="_blank" class="btn-whatsapp hero-btn-sub">
              <span>واتساب المباشر 💬</span>
            </a>
          </div>
          <div class="hero-version-pill">
            <span class="dot"></span>
            <span id="heroReleaseTag">الإصدار الأحدث v<?= htmlspecialchars($apkVersion) ?> • حجم خفيف ~<?= htmlspecialchars($apkSizeMb) ?> ميجابايت • أندرويد</span>
          </div>
        </div>
      </div>

      <!-- Real App Showcase Phone Frame -->
      <div class="hero-visual">
        <div class="float-tag float-tag-1">
          <span>⚡</span>
          <span>100% يعمل بدون إنترنت</span>
        </div>

        <div class="phone-mockup-frame">
          <div class="phone-notch"></div>
          <div class="phone-screen">
            <img src="assets/images/app_screen_real.png" alt="واجهة تطبيق مسرب الحقيقية" loading="eager">
          </div>
        </div>

        <div class="float-tag float-tag-2">
          <span>🧾</span>
          <span>سندات قبض فورية عبر واتساب</span>
        </div>
      </div>
    </div>
  </section>

  <!-- Quick Trust Value Strip -->
  <div class="container trust-strip">
    <div class="trust-grid">
      <div class="trust-card">
        <div class="trust-icon-box">📶</div>
        <div>
          <div class="trust-title">بدون إنترنت إطلاقاً</div>
          <div class="trust-desc">يعمل في المزارع والوديان النائية دون الحاجة لأي اتصال.</div>
        </div>
      </div>
      <div class="trust-card">
        <div class="trust-icon-box">⏱️</div>
        <div>
          <div class="trust-title">دقة بالثانية والدقيقة</div>
          <div class="trust-desc">احتساب كسور الساعات ومنع أي هدر أو فاقد مالي بدقة.</div>
        </div>
      </div>
      <div class="trust-card">
        <div class="trust-icon-box">🧾</div>
        <div>
          <div class="trust-title">سندات وتقارير فورية</div>
          <div class="trust-desc">إصدار سندات القبض ومشاركتها مع المزارعين عبر واتساب.</div>
        </div>
      </div>
      <div class="trust-card">
        <div class="trust-icon-box">☁️</div>
        <div>
          <div class="trust-title">نسخ احتياطي آمن</div>
          <div class="trust-desc">حفظ البيانات محلياً مع المزامنة على Google Drive.</div>
        </div>
      </div>
    </div>
  </div>

  <!-- Compact Rates Banner (If Enabled) -->
  <?php if (!empty($ps['show_rates_section'])): ?>
  <div class="container" id="rates">
    <div class="rates-compact-banner">
      <div class="rates-info-main">
        <div class="rates-badge-icon">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.3"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path><circle cx="12" cy="10" r="3"></circle></svg>
        </div>
        <div>
          <div class="rates-info-title">المنطقة الجغرافية المعتمدة:</div>
          <div class="rates-info-val" id="regionNameEl"><?= htmlspecialchars($ps['active_region_name']) ?></div>
        </div>
      </div>
      <div class="rates-price-block">
        <div class="rates-info-title">متوسط سعر الساعة</div>
        <div class="rates-price-num"><span id="avgPriceEl"><?= number_format($ps['avg_hourly_price']) ?></span> <span class="rates-price-unit"><?= htmlspecialchars($ps['currency_symbol']) ?></span></div>
      </div>
      <div class="rates-status-pill" id="waterStatusEl">
        <span>💧</span>
        <span><?= htmlspecialchars($ps['water_status_text'] ?? 'مستقر ومتاح في معظم المناطق الزراعية') ?></span>
      </div>
    </div>
  </div>
  <?php endif; ?>

  <!-- Features Section -->
  <section class="section container" id="features">
    <div class="section-head">
      <span class="section-tag">حلول ميدانية متكاملة</span>
      <h2>أبرز ما يقدمه تطبيق مُسَرِّب</h2>
      <p>صُمم خصيصاً لملاك الآبار والموزعين لضبط الحسابات وتفادي الخلافات في الميدان</p>
    </div>
    <div class="features-grid">
      <div class="feature-card">
        <div class="feature-card-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.3"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
        </div>
        <h3>حساب دقيق لكسور الساعات</h3>
        <p>توقيت دقيق لبدء وانتهاء الضخ، واحتساب مستحقات الساعات بالريال آلياً دون الحاجة لأي عمليات حسابية معقدة.</p>
      </div>

      <div class="feature-card">
        <div class="feature-card-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.3"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line></svg>
        </div>
        <h3>سندات قبض فورية عبر واتساب</h3>
        <p>توليد سند إلكتروني موثق وإرساله للمزارع مباشرة عبر تطبيق واتساب فور انتهاء دورة السقي لضمان الشفافية.</p>
      </div>

      <div class="feature-card">
        <div class="feature-card-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.3"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>
        </div>
        <h3>سجل المزارعين والديون</h3>
        <p>متابعة حسابات كل عميل، المبالغ المسددة، والمتبقي كآجل، مع كشف حساب تفصيلي يمكن تصديره في أي لحظة.</p>
      </div>

      <div class="feature-card">
        <div class="feature-card-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.3"><rect x="2" y="2" width="20" height="8" rx="2" ry="2"></rect><rect x="2" y="14" width="20" height="8" rx="2" ry="2"></rect><line x1="6" y1="6" x2="6.01" y2="6"></line><line x1="6" y1="18" x2="6.01" y2="18"></line></svg>
        </div>
        <h3>إدارة الوقود والطاقة الشمسية</h3>
        <p>متوافق مع آبار الطاقة الشمسية ومضخات الديزل، مع تسجيل نفقات الصيانة وشراء المياه لحساب الأرباح الصافية.</p>
      </div>
    </div>
  </section>

  <!-- How It Works (3 Steps) -->
  <section class="section container" id="how">
    <div class="section-head">
      <span class="section-tag">سهولة مطلقة</span>
      <h2>كيف تبدأ في 3 خطوات بسيطة؟</h2>
      <p>لا يتطلب إعدادات معقدة، التطبيق جاهز للعمل فور التثبيت</p>
    </div>
    <div class="steps-grid">
      <div class="step-card">
        <div class="step-num">1</div>
        <h3>حمّل وثبّت التطبيق (APK)</h3>
        <p>اضغط على زر التحميل، نزل الملف الصغير وثبته مباشرة على هاتفك الأندرويد.</p>
      </div>
      <div class="step-card">
        <div class="step-num">2</div>
        <h3>سجّل بيانات البئر والمزارعين</h3>
        <p>أدخل اسم البئر وسعر الساعة، وأضف أسماء المشتركين في ثوانٍ معدودة.</p>
      </div>
      <div class="step-card">
        <div class="step-num">3</div>
        <h3>ابدأ السقي وأصدر السندات</h3>
        <p>اضغط "سقي جديد" لبدء العداد، وبمجرد الانتهاء أرسل السند للمزارع عبر واتساب.</p>
      </div>
    </div>
  </section>

  <!-- Pricing Plans Section (If Enabled) -->
  <?php if (!empty($ps['show_plans_section'])): ?>
  <section class="section container" id="plans">
    <div class="section-head">
      <span class="section-tag">الترخيص والتفعيل</span>
      <h2>باقات الاشتراك المعتمدة</h2>
      <p>اختر الباقة المناسبة لمزرعتك مع إمكانية التفعيل الفوري</p>
    </div>

    <div class="pricing-grid">
      <!-- Annual Plan -->
      <div class="plan-card popular">
        <div class="popular-tag"><?= htmlspecialchars($yearlyDisc) ?></div>
        <div class="plan-title">الترخيص السنوي الكامل</div>
        <div class="plan-desc">الخيار الأمثل لموسم زراعي متكامل (365 يوماً)</div>
        <div class="plan-price-wrap">
          <span class="plan-price-num" id="yearlyPriceEl"><?= htmlspecialchars($yearlyPrice) ?><?= htmlspecialchars($yearlyCurrency) ?></span>
          <span class="plan-price-period">/ سنوياً</span>
        </div>
        <?php if (!empty($yearlyOrig)): ?>
        <div class="plan-old-price" id="yearlyOrigEl">بدلاً من <?= htmlspecialchars($yearlyOrig) ?><?= htmlspecialchars($yearlyCurrency) ?></div>
        <?php endif; ?>
        <ul class="plan-features-list">
          <li><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg> <span>عمليات ري وحسابات وسندات غير محدودة</span></li>
          <li><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg> <span>نسخ احتياطي سحابي دوري على Google Drive</span></li>
          <li><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg> <span>تصدير كشوفات الحسابات PDF وإكسل Excel</span></li>
          <li><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg> <span>دعم فني مباشر وإمكانية نقل الرخصة لهاتف بديل</span></li>
        </ul>
        <button onclick="openModal('YEARLY')" class="btn-primary" style="justify-content: center; width: 100%;">طلب تفعيل الباقة السنوية ⚡</button>
      </div>

      <!-- Monthly Plan -->
      <div class="plan-card">
        <div class="plan-title">الترخيص الشهري</div>
        <div class="plan-desc">مرونة كاملة لتشغيل المواسم الزراعية القصيرة</div>
        <div class="plan-price-wrap">
          <span class="plan-price-num" id="monthlyPriceEl"><?= htmlspecialchars($monthlyPrice) ?><?= htmlspecialchars($monthlyCurrency) ?></span>
          <span class="plan-price-period">/ شهرياً</span>
        </div>
        <div style="height: 20px;"></div>
        <ul class="plan-features-list">
          <li><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg> <span>صلاحية كاملة لكافة المزايا لمدة 30 يوماً</span></li>
          <li><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg> <span>إصدار ومشاركة سندات القبض عبر واتساب</span></li>
          <li><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg> <span>حفظ وتأمين البيانات محلياً على الهاتف</span></li>
        </ul>
        <button onclick="openModal('MONTHLY')" class="btn-outline" style="justify-content: center; width: 100%;">طلب تفعيل الباقة الشهرية</button>
      </div>
    </div>
  </section>
  <?php endif; ?>

  <!-- Bottom Direct Download Banner -->
  <?php if (!empty($ps['show_download_cta'])): ?>
  <div class="container">
    <div class="bottom-cta-banner">
      <h2>جاهز لضبط حسابات بئرك بلمسة زر؟</h2>
      <p>حمّل أحدث نسخة رسمية من تطبيق مُسَرِّب الميداني مجاناً الآن وابدأ بتسجيل جلسات السقي بدقة واحترافية.</p>
      <div class="bottom-cta-btns">
        <a href="<?= $downloadUrl ?>" class="btn-white" onclick="trackDownload()">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>
          <span>تحميل التطبيق APK (مباشر)</span>
        </a>
        <a href="https://wa.me/<?= $cleanWhatsapp ?>?text=<?= rawurlencode('السلام عليكم يا مهندس عمر، أود تفعيل رخصة تطبيق مسرب') ?>" target="_blank" class="btn-outline" style="background: rgba(255,255,255,0.15); color: #FFFFFF; border-color: rgba(255,255,255,0.3);">
          <span>تواصل عبر واتساب 💬</span>
        </a>
      </div>
    </div>
  </div>
  <?php endif; ?>

  <!-- Minimal Footer -->
  <footer class="portal-footer">
    <div class="container footer-content">
      <div class="footer-brand">
        مُسَرِّب • MOSARIB
      </div>
      <div class="footer-copy">
        جميع الحقوق محفوظة &copy; <?= date('Y') ?> • تطوير <?= htmlspecialchars($ps['developer_name']) ?> (<?= htmlspecialchars($ps['developer_company']) ?>)
      </div>
      <div class="footer-links">
        <a href="https://wa.me/<?= $cleanWhatsapp ?>" target="_blank" style="color: var(--primary);">واتساب: <?= htmlspecialchars($ps['contact_phone']) ?></a>
      </div>
    </div>
  </footer>

  <!-- Clean Activation Modal -->
  <div class="modal-overlay" id="orderModal">
    <div class="modal-content">
      <div class="modal-header">
        <h3 class="modal-title">طلب تفعيل اشتراك مُسَرِّب</h3>
        <button class="close-btn" onclick="closeModal()">&times;</button>
      </div>

      <div id="modalFeedback" style="display: none; padding: 10px; border-radius: 8px; font-size: 13px; margin-bottom: 14px;"></div>

      <?php if (!empty($paymentMethods)): ?>
      <div style="margin-bottom: 16px; background: var(--bg-subtle); border: 1px solid var(--border); border-radius: var(--radius-sm); padding: 12px;">
        <div style="font-size: 12px; font-weight: 800; color: var(--text-muted); margin-bottom: 8px;">حسابات التحويل المعتمدة (اضغط لنسخ الرقم):</div>
        <?php foreach ($paymentMethods as $pm): ?>
        <div class="bank-pill" onclick="navigator.clipboard.writeText('<?= htmlspecialchars($pm['account_number']) ?>'); alert('تم نسخ رقم الحساب: <?= htmlspecialchars($pm['account_number']) ?>');" title="اضغط لنسخ رقم الحساب">
          <span><?= htmlspecialchars($pm['bank_name']) ?> (<?= htmlspecialchars($pm['account_holder'] ?? 'عمر الحومي') ?>)</span>
          <span style="color: var(--primary);">📋 <?= htmlspecialchars($pm['account_number']) ?></span>
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

      <div style="text-align: center; margin-top: 12px;">
        <button onclick="sendOrderWhatsApp()" class="btn-outline" style="width: 100%; justify-content: center; font-size: 13px;">أو إرسال الطلب عبر واتساب 💬</button>
      </div>
    </div>
  </div>

  <!-- Dynamic Live Sync & Mobile Logic -->
  <script>
    function toggleMobileNav() {
      const drawer = document.getElementById('mobileNavDrawer');
      const backdrop = document.getElementById('mobileNavBackdrop');
      if (!drawer || !backdrop) return;
      const isOpen = drawer.classList.contains('open');
      if (isOpen) {
        closeMobileNav();
      } else {
        drawer.classList.add('open');
        backdrop.classList.add('open');
        document.body.style.overflow = 'hidden';
      }
    }

    function closeMobileNav() {
      const drawer = document.getElementById('mobileNavDrawer');
      const backdrop = document.getElementById('mobileNavBackdrop');
      if (drawer) drawer.classList.remove('open');
      if (backdrop) backdrop.classList.remove('open');
      document.body.style.overflow = '';
    }

    function togglePortalTheme() {
      const cur = document.documentElement.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
      document.documentElement.setAttribute('data-theme', cur);
      document.getElementById('themeToggleIcon').textContent = cur === 'dark' ? '☀️' : '🌙';
      try { localStorage.setItem('mosarib_portal_theme', cur); } catch(e) {}
    }

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

    // Live Sync with Database & Public API
    async function syncLiveData() {
      try {
        const res = await fetch('api/public_data.php?t=' + Date.now());
        const json = await res.json();
        if (json && json.data) {
          const s = json.data.portal_settings || {};
          const p = json.data.subscription_pricing || {};
          const r = json.data.app_release || {};
          
          if (s.active_region_name && document.getElementById('regionNameEl')) document.getElementById('regionNameEl').textContent = s.active_region_name;
          if (s.avg_hourly_price && document.getElementById('avgPriceEl')) document.getElementById('avgPriceEl').textContent = Number(s.avg_hourly_price).toLocaleString();
          if (s.hero_title && document.getElementById('heroTitleText')) document.getElementById('heroTitleText').textContent = s.hero_title;
          if (s.hero_subtitle && document.getElementById('heroSubtitleText')) document.getElementById('heroSubtitleText').textContent = s.hero_subtitle;
          if (s.hero_badge && document.getElementById('heroBadgeText')) document.getElementById('heroBadgeText').textContent = s.hero_badge;
          if (s.headline_alert && document.getElementById('topTickerText')) document.getElementById('topTickerText').textContent = s.headline_alert;
          if (s.water_status_text && document.getElementById('waterStatusEl')) document.getElementById('waterStatusEl').innerHTML = '<span>💧</span><span>' + s.water_status_text + '</span>';

          if (p.yearly && p.yearly.price && document.getElementById('yearlyPriceEl')) {
            document.getElementById('yearlyPriceEl').textContent = p.yearly.price + (p.yearly.currency || '$');
          }
          if (p.monthly && p.monthly.price && document.getElementById('monthlyPriceEl')) {
            document.getElementById('monthlyPriceEl').textContent = p.monthly.price + (p.monthly.currency || '$');
          }

          if (r.version_name && document.getElementById('heroReleaseTag')) {
            document.getElementById('heroReleaseTag').textContent = 'الإصدار الأحدث v' + r.version_name + ' • حجم خفيف ~' + (r.file_size_mb || '2.8') + ' ميجابايت • أندرويد';
          }
        }
      } catch(_) {}
    }
    document.addEventListener('DOMContentLoaded', syncLiveData);

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
      const plan = document.getElementById('selectedPlan').value === 'YEARLY' ? 'سنوي' : 'شهري';
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

      window.open(`https://wa.me/<?= $cleanWhatsapp ?>?text=${encodeURIComponent(msg)}`, '_blank');
    }

    function trackDownload() {
      try { fetch('api/public_data.php?action=track_download', { method: 'POST' }).catch(() => {}); } catch(_) {}
    }
  </script>
</body>
</html>
