<?php
/**
 * Mosarib Public Portal - Official Landing Page
 * High-performance, responsive portal with dynamic admin customization
 */

// 1. Visitor Analytics Tracking
$analyticsFile = __DIR__ . '/data/portal_analytics.json';
$today = date('Y-m-d');

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
    $existing = json_decode(file_get_contents($analyticsFile), true);
    if (is_array($existing)) {
        $analytics = array_merge($analytics, $existing);
    }
}

if (($analytics['last_visit_date'] ?? '') !== $today) {
    $analytics['last_visit_date'] = $today;
    $analytics['today_visitors'] = 0;
}
if (($analytics['last_download_date'] ?? '') !== $today) {
    $analytics['last_download_date'] = $today;
    $analytics['today_downloads'] = 0;
}

$analytics['total_visitors']++;
$analytics['today_visitors']++;
$analytics['last_visit_at'] = time();

$visitedCookie = 'mosarib_visited_' . $today;
if (empty($_COOKIE[$visitedCookie])) {
    setcookie($visitedCookie, '1', time() + 86400, '/');
    $analytics['unique_visitors']++;
}

if (!isset($analytics['daily_stats'][$today])) {
    $analytics['daily_stats'][$today] = ['visitors' => 0, 'downloads' => 0];
}
$analytics['daily_stats'][$today]['visitors']++;

if (count($analytics['daily_stats']) > 30) {
    $analytics['daily_stats'] = array_slice($analytics['daily_stats'], -30, null, true);
}

@file_put_contents($analyticsFile, json_encode($analytics, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT));

// 2. Load Portal Settings with default fallbacks
$settingsFile = __DIR__ . '/data/portal_settings.json';
$ps = [
    'water_status' => 'GOOD',
    'active_region_name' => 'الجمهورية اليمنية (صنعاء، مأرب، الجوف، عمران)',
    'avg_hourly_price' => 7500,
    'min_hourly_price' => 5500,
    'max_hourly_price' => 9500,
    'currency_symbol' => 'ريال',
    'headline_alert' => 'منظومة مُسَرِّب: إدارة دقيقة لساعات الري وسندات القبض وحسابات المزارعين بالثواني والدقائق.',
    'rates_note' => 'الأسعار تختلف بحسب نوع تشغيل البئر (منظومات طاقة شمسية أو محروقات ديزل) وطبيعة المنطقة الجغرافية.',
    'hero_badge' => 'المنظومة الرقمية الميدانية لإدارة مياه الآبار',
    'hero_title' => 'مُسَرِّب • حساب دقيق لمياه وساعات الري وحصص المزارعين',
    'hero_subtitle' => 'تطبيق ذكي خفيف يعمل بدون إنترنت (Offline-First)، يدير جلسات الري بالثانية والدقيقة، يرسل سندات القبض عبر واتساب، ويحفظ بياناتك بأمان على Google Drive.',
    'contact_whatsapp' => '967773712030',
    'contact_phone' => '773712030',
    'developer_name' => 'المهندس عمر الحومي',
    'developer_company' => 'تُبع سوفت للحلول البرمجية الذكية (TubbaSoft)',
    'developer_email' => 'omar@tubbasoft.com',
    'developer_message' => 'تطبيق مُسَرِّب صُمم خصيصاً لتلبية احتياجات ملاك الآبار والمشتركين بدقة متناهية وسهولة مطلقة.',
    'show_top_ticker' => true,
    'show_rates_section' => true,
    'show_features_section' => true,
    'show_plans_section' => true,
    'show_developer_section' => true,
    'show_download_cta' => true
];

if (file_exists($settingsFile)) {
    $savedSettings = json_decode(file_get_contents($settingsFile), true);
    if (is_array($savedSettings)) {
        $ps = array_merge($ps, $savedSettings);
    }
}

// 3. Load App Release Info
$releaseFile = __DIR__ . '/data/release_info.json';
$release = [
    'version_name' => '1.4.0',
    'file_size_mb' => 2.80,
    'download_url' => 'api/download.php'
];
if (file_exists($releaseFile)) {
    $savedRel = json_decode(file_get_contents($releaseFile), true);
    if (is_array($savedRel)) {
        $release = array_merge($release, $savedRel);
    }
}
?>
<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title><?= htmlspecialchars($ps['hero_title']) ?> • البوابة الرسمية</title>
  <meta name="description" content="<?= htmlspecialchars($ps['hero_subtitle']) ?>">
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Cairo:wght@400;500;600;700;800;900&display=swap" rel="stylesheet">

  <style>
    :root {
      --primary: #00838F;
      --primary-dark: #004D40;
      --primary-light: #26A69A;
      --accent: #00B4D8;
      --accent-glow: rgba(0, 180, 216, 0.25);
      --gold: #FFB300;
      --gold-soft: rgba(255, 179, 0, 0.15);
      --bg: #071015;
      --surface: #0E1D24;
      --surface-card: rgba(14, 29, 36, 0.75);
      --surface-border: rgba(0, 180, 216, 0.18);
      --border-subtle: rgba(255, 255, 255, 0.08);
      --text: #E0F2F1;
      --text-muted: #80CBC4;
      --text-dim: #607D8B;
      --text-heading: #FFFFFF;
      --radius-sm: 8px;
      --radius-md: 14px;
      --radius-lg: 20px;
      --radius-full: 9999px;
    }

    * {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
      font-family: 'Cairo', system-ui, -apple-system, sans-serif;
    }

    body {
      background-color: var(--bg);
      background-image: 
        radial-gradient(circle at 15% 15%, rgba(0, 131, 143, 0.15) 0%, transparent 40%),
        radial-gradient(circle at 85% 25%, rgba(0, 180, 216, 0.10) 0%, transparent 45%),
        radial-gradient(circle at 50% 85%, rgba(0, 77, 64, 0.12) 0%, transparent 50%);
      background-attachment: fixed;
      color: var(--text);
      line-height: 1.6;
      overflow-x: hidden;
      -webkit-font-smoothing: antialiased;
    }

    .container {
      max-width: 1140px;
      margin: 0 auto;
      padding: 0 20px;
    }

    /* Top Live Alert Ticker */
    .top-ticker {
      background: linear-gradient(90deg, #002B24, #004D40, #002B24);
      color: #E0F7FA;
      font-size: 13px;
      padding: 8px 16px;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      border-bottom: 1px solid rgba(0, 180, 216, 0.2);
      font-weight: 600;
    }
    .pulse-dot {
      width: 8px;
      height: 8px;
      background: var(--gold);
      border-radius: 50%;
      box-shadow: 0 0 8px var(--gold);
      animation: pulse 1.8s infinite;
      flex-shrink: 0;
    }
    @keyframes pulse {
      0% { transform: scale(0.9); opacity: 0.8; }
      50% { transform: scale(1.3); opacity: 1; }
      100% { transform: scale(0.9); opacity: 0.8; }
    }

    /* Header */
    header.main-header {
      position: sticky;
      top: 0;
      z-index: 1000;
      background: rgba(7, 16, 21, 0.92);
      backdrop-filter: blur(16px);
      -webkit-backdrop-filter: blur(16px);
      border-bottom: 1px solid var(--border-subtle);
      padding: 12px 0;
    }
    .header-content {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 20px;
    }
    .brand-logo {
      display: flex;
      align-items: center;
      gap: 12px;
      text-decoration: none;
    }
    .brand-badge {
      width: 44px;
      height: 44px;
      border-radius: 12px;
      background: linear-gradient(135deg, #00838F, #004D40);
      border: 1px solid rgba(0, 180, 216, 0.4);
      display: flex;
      align-items: center;
      justify-content: center;
      color: #80DEEA;
      box-shadow: 0 4px 14px rgba(0, 131, 143, 0.3);
    }
    .brand-text b {
      color: #FFFFFF;
      font-size: 19px;
      font-weight: 900;
      letter-spacing: 0.5px;
      display: block;
      line-height: 1.2;
    }
    .brand-text span {
      font-size: 11.5px;
      color: var(--text-muted);
      font-weight: 600;
    }
    .nav-links {
      display: flex;
      align-items: center;
      gap: 24px;
    }
    .nav-link {
      color: #B2DFDB;
      font-size: 14px;
      font-weight: 600;
      text-decoration: none;
      transition: color 0.2s ease;
    }
    .nav-link:hover {
      color: #FFFFFF;
    }
    .btn-header-cta {
      background: linear-gradient(135deg, #00897B, #004D40);
      color: #FFFFFF;
      border: 1px solid rgba(0, 180, 216, 0.4);
      padding: 8px 18px;
      border-radius: var(--radius-full);
      font-size: 13px;
      font-weight: 700;
      text-decoration: none;
      display: inline-flex;
      align-items: center;
      gap: 6px;
      transition: all 0.2s ease;
    }
    .btn-header-cta:hover {
      background: linear-gradient(135deg, #00BFA5, #00695C);
      box-shadow: 0 4px 16px rgba(0, 191, 165, 0.3);
      transform: translateY(-1px);
    }

    /* Hero Section */
    .hero-section {
      padding: 48px 0 36px 0;
      text-align: center;
    }
    .hero-badge-pill {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      background: rgba(0, 180, 216, 0.1);
      border: 1px solid rgba(0, 180, 216, 0.25);
      color: #80DEEA;
      padding: 6px 16px;
      border-radius: var(--radius-full);
      font-size: 12.5px;
      font-weight: 700;
      margin-bottom: 20px;
    }
    .hero-title {
      font-size: 34px;
      font-weight: 900;
      color: #FFFFFF;
      line-height: 1.35;
      margin-bottom: 16px;
      max-width: 860px;
      margin-left: auto;
      margin-right: auto;
    }
    .hero-subtitle {
      font-size: 16px;
      color: #B2DFDB;
      max-width: 760px;
      margin: 0 auto 28px auto;
      line-height: 1.7;
    }
    .hero-actions {
      display: flex;
      justify-content: center;
      gap: 14px;
      flex-wrap: wrap;
      margin-bottom: 26px;
    }
    .btn-primary-action {
      background: linear-gradient(135deg, #00897B, #004D40);
      color: #FFFFFF;
      border: 1px solid rgba(0, 230, 118, 0.4);
      padding: 13px 28px;
      border-radius: var(--radius-md);
      font-size: 15px;
      font-weight: 800;
      text-decoration: none;
      display: inline-flex;
      align-items: center;
      gap: 10px;
      box-shadow: 0 8px 24px rgba(0, 137, 123, 0.35);
      transition: all 0.2s ease;
    }
    .btn-primary-action:hover {
      background: linear-gradient(135deg, #00BFA5, #00796B);
      transform: translateY(-2px);
      box-shadow: 0 10px 28px rgba(0, 191, 165, 0.45);
    }
    .btn-whatsapp-action {
      background: rgba(37, 211, 102, 0.12);
      border: 1px solid rgba(37, 211, 102, 0.35);
      color: #69F0AE;
      padding: 13px 24px;
      border-radius: var(--radius-md);
      font-size: 15px;
      font-weight: 800;
      text-decoration: none;
      display: inline-flex;
      align-items: center;
      gap: 10px;
      transition: all 0.2s ease;
    }
    .btn-whatsapp-action:hover {
      background: rgba(37, 211, 102, 0.22);
      transform: translateY(-2px);
    }
    .hero-trust-bar {
      display: flex;
      justify-content: center;
      align-items: center;
      gap: 20px;
      flex-wrap: wrap;
      font-size: 12.5px;
      color: var(--text-muted);
      font-weight: 600;
    }
    .hero-trust-item {
      display: inline-flex;
      align-items: center;
      gap: 6px;
    }

    /* Section Styling */
    section.portal-section {
      padding: 38px 0;
    }
    .section-header {
      text-align: center;
      margin-bottom: 28px;
    }
    .section-tag {
      font-size: 12px;
      font-weight: 800;
      color: var(--accent);
      text-transform: uppercase;
      letter-spacing: 1px;
      margin-bottom: 6px;
      display: block;
    }
    .section-title {
      font-size: 26px;
      font-weight: 900;
      color: #FFFFFF;
      margin-bottom: 8px;
    }
    .section-desc {
      font-size: 14px;
      color: var(--text-muted);
      max-width: 620px;
      margin: 0 auto;
    }

    /* Region & Rates Box */
    .rates-box {
      background: var(--surface-card);
      border: 1px solid var(--surface-border);
      border-radius: var(--radius-lg);
      padding: 26px;
      box-shadow: 0 10px 30px rgba(0, 0, 0, 0.25);
    }
    .region-banner {
      background: linear-gradient(135deg, rgba(0, 131, 143, 0.2), rgba(0, 77, 64, 0.3));
      border: 1px solid rgba(0, 180, 216, 0.3);
      border-radius: var(--radius-md);
      padding: 16px 20px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      margin-bottom: 22px;
      flex-wrap: wrap;
    }
    .region-info {
      display: flex;
      align-items: center;
      gap: 14px;
    }
    .region-icon {
      width: 44px;
      height: 44px;
      border-radius: 12px;
      background: rgba(0, 180, 216, 0.15);
      border: 1px solid rgba(0, 180, 216, 0.3);
      display: flex;
      align-items: center;
      justify-content: center;
      color: #4DD0E1;
      flex-shrink: 0;
    }
    .region-label {
      font-size: 12px;
      color: var(--text-muted);
      font-weight: 600;
    }
    .region-name {
      font-size: 17px;
      font-weight: 900;
      color: #FFFFFF;
    }
    .region-badge {
      background: rgba(0, 230, 118, 0.15);
      border: 1px solid rgba(0, 230, 118, 0.4);
      color: #69F0AE;
      padding: 4px 12px;
      border-radius: var(--radius-full);
      font-size: 12px;
      font-weight: 700;
    }

    /* Rates 3-Card Grid */
    .rates-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
      gap: 18px;
      margin-bottom: 18px;
    }
    .rate-card {
      background: rgba(7, 16, 21, 0.65);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-md);
      padding: 20px;
      text-align: center;
      transition: all 0.2s ease;
    }
    .rate-card:hover {
      border-color: rgba(0, 180, 216, 0.35);
      transform: translateY(-2px);
    }
    .rate-card-highlight {
      background: linear-gradient(160deg, rgba(0, 131, 143, 0.18), rgba(7, 16, 21, 0.85));
      border: 1px solid rgba(0, 180, 216, 0.4);
      box-shadow: 0 4px 18px rgba(0, 131, 143, 0.15);
    }
    .rate-card-title {
      font-size: 13px;
      color: var(--text-muted);
      font-weight: 700;
      margin-bottom: 6px;
    }
    .rate-card-value {
      font-size: 30px;
      font-weight: 900;
      color: #FFFFFF;
      margin-bottom: 4px;
      line-height: 1.2;
    }
    .rate-card-value span {
      font-size: 14px;
      color: var(--gold);
      font-weight: 700;
      margin-right: 4px;
    }
    .rate-card-desc {
      font-size: 11.5px;
      color: var(--text-dim);
    }
    .rates-note-banner {
      background: rgba(255, 179, 0, 0.08);
      border: 1px solid rgba(255, 179, 0, 0.22);
      border-radius: var(--radius-sm);
      padding: 10px 14px;
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 12.5px;
      color: #FFE082;
    }

    /* Features 4-Card Grid */
    .features-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
      gap: 20px;
    }
    .feature-card {
      background: var(--surface-card);
      border: 1px solid var(--surface-border);
      border-radius: var(--radius-md);
      padding: 24px;
      transition: all 0.2s ease;
    }
    .feature-card:hover {
      border-color: rgba(0, 180, 216, 0.4);
      transform: translateY(-2px);
    }
    .feature-icon-badge {
      width: 46px;
      height: 46px;
      border-radius: 12px;
      background: rgba(0, 180, 216, 0.12);
      border: 1px solid rgba(0, 180, 216, 0.25);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 22px;
      margin-bottom: 16px;
    }
    .feature-title {
      font-size: 16.5px;
      font-weight: 800;
      color: #FFFFFF;
      margin-bottom: 8px;
    }
    .feature-desc {
      font-size: 13.5px;
      color: #B2DFDB;
      line-height: 1.65;
    }

    /* Subscription Plans Section */
    .plans-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(290px, 1fr));
      gap: 22px;
    }
    .plan-card {
      background: var(--surface-card);
      border: 1px solid var(--surface-border);
      border-radius: var(--radius-lg);
      padding: 28px 24px;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      position: relative;
      transition: all 0.2s ease;
    }
    .plan-card:hover {
      border-color: rgba(0, 180, 216, 0.4);
      transform: translateY(-3px);
    }
    .plan-card-popular {
      background: linear-gradient(160deg, rgba(0, 131, 143, 0.25), rgba(14, 29, 36, 0.85));
      border: 1px solid rgba(0, 230, 118, 0.45);
      box-shadow: 0 8px 30px rgba(0, 131, 143, 0.25);
    }
    .plan-badge {
      position: absolute;
      top: -12px;
      left: 20px;
      background: linear-gradient(135deg, #00C853, #00796B);
      color: #FFFFFF;
      font-size: 11px;
      font-weight: 800;
      padding: 3px 12px;
      border-radius: var(--radius-full);
      box-shadow: 0 4px 10px rgba(0, 200, 83, 0.3);
    }
    .plan-name {
      font-size: 19px;
      font-weight: 900;
      color: #FFFFFF;
      margin-bottom: 6px;
    }
    .plan-subtitle {
      font-size: 13px;
      color: var(--text-muted);
      margin-bottom: 16px;
    }
    .plan-price {
      font-size: 26px;
      font-weight: 900;
      color: #FFFFFF;
      margin-bottom: 18px;
      padding-bottom: 16px;
      border-bottom: 1px solid var(--border-subtle);
    }
    .plan-price small {
      font-size: 13px;
      color: var(--text-dim);
      font-weight: 600;
    }
    .plan-features-list {
      list-style: none;
      margin-bottom: 24px;
    }
    .plan-features-list li {
      font-size: 13.5px;
      color: #E0F2F1;
      margin-bottom: 10px;
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .plan-features-list li svg {
      color: #00E676;
      flex-shrink: 0;
    }
    .btn-plan {
      background: rgba(0, 180, 216, 0.12);
      border: 1px solid rgba(0, 180, 216, 0.3);
      color: #80DEEA;
      padding: 10px;
      border-radius: var(--radius-md);
      font-size: 14px;
      font-weight: 700;
      text-align: center;
      text-decoration: none;
      display: block;
      transition: all 0.2s ease;
    }
    .btn-plan:hover {
      background: rgba(0, 180, 216, 0.22);
    }
    .btn-plan-popular {
      background: linear-gradient(135deg, #00897B, #004D40);
      color: #FFFFFF;
      border: 1px solid rgba(0, 230, 118, 0.4);
    }
    .btn-plan-popular:hover {
      background: linear-gradient(135deg, #00BFA5, #00695C);
    }

    /* Developer & Contact Card */
    .developer-card {
      background: linear-gradient(150deg, rgba(14, 29, 36, 0.9), rgba(7, 16, 21, 0.95));
      border: 1px solid var(--surface-border);
      border-radius: var(--radius-lg);
      padding: 28px;
    }
    .dev-grid {
      display: grid;
      grid-template-columns: auto 1fr auto;
      gap: 22px;
      align-items: center;
    }
    .dev-avatar {
      width: 64px;
      height: 64px;
      border-radius: 16px;
      background: linear-gradient(135deg, #00838F, #004D40);
      border: 1px solid rgba(0, 180, 216, 0.4);
      display: flex;
      align-items: center;
      justify-content: center;
      color: #80DEEA;
      flex-shrink: 0;
    }
    .dev-name {
      font-size: 19px;
      font-weight: 900;
      color: #FFFFFF;
      margin-bottom: 2px;
    }
    .dev-company {
      font-size: 13.5px;
      color: #4DD0E1;
      font-weight: 700;
      margin-bottom: 6px;
    }
    .dev-desc {
      font-size: 13px;
      color: #B2DFDB;
      line-height: 1.6;
      max-width: 580px;
    }
    .dev-buttons {
      display: flex;
      flex-direction: column;
      gap: 10px;
      min-width: 200px;
    }
    .dev-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      padding: 9px 14px;
      border-radius: var(--radius-md);
      font-size: 13px;
      font-weight: 700;
      text-decoration: none;
      transition: all 0.2s ease;
    }
    .dev-btn-whatsapp {
      background: rgba(37, 211, 102, 0.15);
      border: 1px solid rgba(37, 211, 102, 0.35);
      color: #69F0AE;
    }
    .dev-btn-whatsapp:hover {
      background: rgba(37, 211, 102, 0.25);
    }
    .dev-btn-phone {
      background: rgba(0, 180, 216, 0.12);
      border: 1px solid rgba(0, 180, 216, 0.3);
      color: #80DEEA;
    }
    .dev-btn-phone:hover {
      background: rgba(0, 180, 216, 0.22);
    }

    /* Download Call to Action Box */
    .download-cta-box {
      background: linear-gradient(135deg, rgba(0, 77, 64, 0.75), rgba(0, 105, 115, 0.75));
      border: 1px solid rgba(0, 180, 216, 0.35);
      border-radius: var(--radius-lg);
      padding: 34px 24px;
      text-align: center;
      margin: 10px 0 30px 0;
    }
    .download-cta-title {
      font-size: 24px;
      font-weight: 900;
      color: #FFFFFF;
      margin-bottom: 8px;
    }
    .download-cta-desc {
      font-size: 14px;
      color: #E0F2F1;
      max-width: 600px;
      margin: 0 auto 20px auto;
    }
    .download-cta-specs {
      display: flex;
      justify-content: center;
      align-items: center;
      gap: 18px;
      font-size: 12px;
      color: #B2DFDB;
      flex-wrap: wrap;
      margin-top: 16px;
    }

    /* Footer */
    footer.main-footer {
      border-top: 1px solid var(--border-subtle);
      padding: 24px 0;
      text-align: center;
      font-size: 12px;
      color: var(--text-dim);
    }
    .footer-content {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 14px;
      margin-bottom: 12px;
    }

    @media (max-width: 768px) {
      .hero-title { font-size: 26px; }
      .dev-grid { grid-template-columns: 1fr; text-align: center; }
      .dev-avatar { margin: 0 auto; }
      .dev-buttons { min-width: 100%; }
      .nav-links { display: none; }
      .rates-grid { grid-template-columns: 1fr; }
    }
  </style>
</head>
<body>

  <!-- Top Alert Ticker (Controlled by Admin) -->
  <?php if (!empty($ps['show_top_ticker'])): ?>
  <div class="top-ticker" id="topTickerBar">
    <span class="pulse-dot"></span>
    <span id="topAlertText"><?= htmlspecialchars($ps['headline_alert']) ?></span>
  </div>
  <?php endif; ?>

  <!-- Main Header -->
  <header class="main-header">
    <div class="container header-content">
      <a href="index.php" class="brand-logo">
        <div class="brand-badge">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"></path>
          </svg>
        </div>
        <div class="brand-text">
          <b>مُسَرِّب • MOSARIB</b>
          <span>إدارة آبار المياه ودورات الري</span>
        </div>
      </a>

      <nav class="nav-links">
        <?php if (!empty($ps['show_rates_section'])): ?>
        <a href="#rates-section" class="nav-link">بورصة الأسعار</a>
        <?php endif; ?>
        <?php if (!empty($ps['show_features_section'])): ?>
        <a href="#features-section" class="nav-link">المميزات الميدانية</a>
        <?php endif; ?>
        <?php if (!empty($ps['show_plans_section'])): ?>
        <a href="#plans-section" class="nav-link">باقات الاشتراك</a>
        <?php endif; ?>
        <?php if (!empty($ps['show_developer_section'])): ?>
        <a href="#developer-section" class="nav-link">الدعم والمطور</a>
        <?php endif; ?>
      </nav>

      <a href="<?= htmlspecialchars($release['download_url']) ?>" class="btn-header-cta" onclick="trackDownloadMetric()">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
          <polyline points="7 10 12 15 17 10"></polyline>
          <line x1="12" y1="15" x2="12" y2="3"></line>
        </svg>
        <span>تحميل التطبيق APK</span>
      </a>
    </div>
  </header>

  <!-- Hero Section -->
  <main class="container">
    <section class="hero-section">
      <div class="hero-badge-pill">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10"></circle>
          <polygon points="12 8 8 12 12 16 12 8"></polygon>
        </svg>
        <span id="heroBadgeText"><?= htmlspecialchars($ps['hero_badge']) ?></span>
      </div>

      <h1 class="hero-title" id="heroTitleText"><?= htmlspecialchars($ps['hero_title']) ?></h1>
      <p class="hero-subtitle" id="heroSubtitleText"><?= htmlspecialchars($ps['hero_subtitle']) ?></p>

      <div class="hero-actions">
        <a href="<?= htmlspecialchars($release['download_url']) ?>" class="btn-primary-action" onclick="trackDownloadMetric()">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
            <polyline points="7 10 12 15 17 10"></polyline>
            <line x1="12" y1="15" x2="12" y2="3"></line>
          </svg>
          <span>تحميل تطبيق مُسَرِّب (APK مباشر)</span>
        </a>

        <?php if (!empty($ps['contact_whatsapp'])): ?>
        <a href="https://wa.me/<?= preg_replace('/[^0-9]/', '', $ps['contact_whatsapp']) ?>?text=<?= urlencode('السلام عليكم، أود الاستفسار عن ترخيص وتفعيل تطبيق مُسَرِّب لإدارة مياه الآبار') ?>" target="_blank" class="btn-whatsapp-action">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12.031 6.172c-3.181 0-5.767 2.586-5.768 5.766-.001 1.298.38 2.27 1.019 3.287l-.582 2.128 2.182-.573c.978.58 1.911.928 3.145.929 3.178 0 5.767-2.587 5.768-5.766.001-3.187-2.575-5.77-5.764-5.771zm3.392 8.244c-.144.405-.837.774-1.17.824-.312.045-.694.073-2.125-.522-1.83-1.074-3.003-2.883-3.095-3.005-.091-.122-.736-.98-.736-1.87 0-.89.464-1.328.629-1.507.165-.18.36-.225.48-.225.12 0 .24.001.345.006.111.005.26-.042.406.31.15.361.51 1.246.555 1.337.045.09.075.195.015.315-.06.12-.09.195-.18.3-.09.105-.19.234-.27.315-.09.09-.184.187-.079.367.105.18.468.772 1.004 1.249.691.614 1.274.805 1.454.895.18.09.285.075.39-.045.105-.12.45-.525.57-.705.12-.18.24-.15.405-.09.165.06 1.05.495 1.23.585.18.09.3.135.345.21.045.075.045.435-.099.84z"/>
          </svg>
          <span>تواصل عبر واتساب</span>
        </a>
        <?php endif; ?>
      </div>

      <div class="hero-trust-bar">
        <span class="hero-trust-item">⚡ يعمل 100% بدون إنترنت (Offline)</span>
        <span class="hero-trust-item">•</span>
        <span class="hero-trust-item">🛡️ فحص أمني معتمد وخالٍ من الفيروسات</span>
        <span class="hero-trust-item">•</span>
        <span class="hero-trust-item">☁️ نسخ احتياطي يومي على Google Drive</span>
        <span class="hero-trust-item">•</span>
        <span class="hero-trust-item">📦 الحجم: <?= htmlspecialchars($release['file_size_mb']) ?> MB</span>
      </div>
    </section>

    <!-- Geographic Scope & Hourly Rates Section -->
    <?php if (!empty($ps['show_rates_section'])): ?>
    <section class="portal-section" id="rates-section">
      <div class="section-header">
        <span class="section-tag">النطاق والأسعار</span>
        <h2 class="section-title">بورصة أسعار ساعات الري والنطاق الميداني</h2>
        <p class="section-desc">مؤشرات الأسعار المعتمدة ونطاق تغطية المنظومة الميدانية الحالية</p>
      </div>

      <div class="rates-box">
        <!-- Active Geographic Coverage Card -->
        <div class="region-banner">
          <div class="region-info">
            <div class="region-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
                <circle cx="12" cy="10" r="3"></circle>
              </svg>
            </div>
            <div>
              <div class="region-label">نطاق العمل والتغطية الميدانية للتطبيق حالياً:</div>
              <div class="region-name" id="activeRegionDisplay"><?= htmlspecialchars($ps['active_region_name']) ?></div>
            </div>
          </div>
          <div class="region-badge">🟢 نطاق تغطية معتمد ومباشر</div>
        </div>

        <!-- 3 Hourly Rates Metric Cards -->
        <div class="rates-grid">
          <div class="rate-card rate-card-highlight">
            <div class="rate-card-title">متوسط سعر الساعة</div>
            <div class="rate-card-value" id="avgPriceDisplay">
              <?= number_format($ps['avg_hourly_price']) ?>
              <span><?= htmlspecialchars($ps['currency_symbol']) ?>/ساعة</span>
            </div>
            <div class="rate-card-desc">متوسط أسعار ساعات الري في المنطقة النشطة</div>
          </div>

          <div class="rate-card">
            <div class="rate-card-title">أقل سعر مسجل</div>
            <div class="rate-card-value" id="minPriceDisplay">
              <?= number_format($ps['min_hourly_price']) ?>
              <span><?= htmlspecialchars($ps['currency_symbol']) ?>/ساعة</span>
            </div>
            <div class="rate-card-desc">أدنى تسعيرة مسجلة لساعات السقي</div>
          </div>

          <div class="rate-card">
            <div class="rate-card-title">أعلى سعر مسجل</div>
            <div class="rate-card-value" id="maxPriceDisplay">
              <?= number_format($ps['max_hourly_price']) ?>
              <span><?= htmlspecialchars($ps['currency_symbol']) ?>/ساعة</span>
            </div>
            <div class="rate-card-desc">أعلى تسعيرة مسجلة بحسب طبيعة التشغيل</div>
          </div>
        </div>

        <?php if (!empty($ps['rates_note'])): ?>
        <div class="rates-note-banner" id="ratesNoteDisplay">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="12" y1="8" x2="12" y2="12"></line>
            <line x1="12" y1="16" x2="12.01" y2="16"></line>
          </svg>
          <span><?= htmlspecialchars($ps['rates_note']) ?></span>
        </div>
        <?php endif; ?>
      </div>
    </section>
    <?php endif; ?>

    <!-- Core Features Section -->
    <?php if (!empty($ps['show_features_section'])): ?>
    <section class="portal-section" id="features-section">
      <div class="section-header">
        <span class="section-tag">الكفاءة والأمان</span>
        <h2 class="section-title">لماذا يثق ملاك الآبار والمزارعون بتطبيق مُسَرِّب؟</h2>
        <p class="section-desc">أدوات عملية تم تصميمها خصيصاً لمطابقة واقع إدارة الآبار والري في الميدان</p>
      </div>

      <div class="features-grid">
        <div class="feature-card">
          <div class="feature-icon-badge">⏱️</div>
          <h3 class="feature-title">توقيت دقيق بالثانية والدقيقة</h3>
          <p class="feature-desc">
            عداد ري ذكي يحسب وقت السقي وتكلفته التلقائية بناءً على تسعيرة الساعة بدقة متناهية ودون أي أخطاء أو خلافات.
          </p>
        </div>

        <div class="feature-card">
          <div class="feature-icon-badge">📲</div>
          <h3 class="feature-title">سندات وفواتير فورية عبر واتساب</h3>
          <p class="feature-desc">
            إرسال إشعار فوري للعميل بالسند المالي أو كشف الحساب وتفاصيل السقية بنقرة زر واحدة عبر تطبيق واتساب.
          </p>
        </div>

        <div class="feature-card">
          <div class="feature-icon-badge">⚡</div>
          <h3 class="feature-title">يعمل 100% بدون إنترنت</h3>
          <p class="feature-desc">
            سجل عملياتك وتابع كشوفات ديون المشتركين في عمق المناطق الزراعية النائية بدون الحاجة لأي تغطية إنترنت.
          </p>
        </div>

        <div class="feature-card">
          <div class="feature-icon-badge">☁️</div>
          <h3 class="feature-title">نسخ احتياطي على Google Drive</h3>
          <p class="feature-desc">
            أمان كامل لسجلاتك المالية وعملياتك السابقة مع إمكانية استرجاع بياناتك بنقرة زر واحدة عند استبدال هاتفك.
          </p>
        </div>
      </div>
    </section>
    <?php endif; ?>

    <!-- Subscription Plans Section -->
    <?php if (!empty($ps['show_plans_section'])): ?>
    <section class="portal-section" id="plans-section">
      <div class="section-header">
        <span class="section-tag">التراخيص والباقات</span>
        <h2 class="section-title">باقات واشتراكات منظومة مُسَرِّب</h2>
        <p class="section-desc">خيارات مرنة ومناسبة لجميع ملاك الآبار والمشغلين مع دعم فني مستمر</p>
      </div>

      <div class="plans-grid">
        <!-- Free Trial -->
        <div class="plan-card">
          <div>
            <h3 class="plan-name">النسخة التجريبية</h3>
            <div class="plan-subtitle">لتجربة التطبيق والتأكد من ملاءمته لمزرعتك</div>
            <div class="plan-price">مجاناً <small>/ بدون بطاقة</small></div>
            <ul class="plan-features-list">
              <li>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
                <span>200 عملية ري تجريبية كاملة</span>
              </li>
              <li>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
                <span>إصدار سندات القبض وتقارير السقي</span>
              </li>
              <li>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
                <span>النسخ الاحتياطي المحلي للهاتف</span>
              </li>
            </ul>
          </div>
          <a href="<?= htmlspecialchars($release['download_url']) ?>" class="btn-plan" onclick="trackDownloadMetric()">تحميل وبدء التجربة المجانية</a>
        </div>

        <!-- Annual Plan (Most Popular) -->
        <div class="plan-card plan-card-popular">
          <span class="plan-badge">الأكثر طلباً واعتماداً ⭐</span>
          <div>
            <h3 class="plan-name">الاشتراك السنوي الشامل</h3>
            <div class="plan-subtitle">الترخيص المكتمل لمالك البئر لموسم زراعي كامل</div>
            <div class="plan-price">رخصة سنوية <small>/ عام كامل</small></div>
            <ul class="plan-features-list">
              <li>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
                <span>عمليات ري غير محدودة إطلاقاً</span>
              </li>
              <li>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
                <span>النسخ الاحتياطي السحابي اليومي المباشر</span>
              </li>
              <li>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
                <span>تصدير كشوفات الحسابات PDF وإكسل</span>
              </li>
              <li>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
                <span>دعم فني وتحديثات مستمرة طوال العام</span>
              </li>
            </ul>
          </div>
          <a href="https://wa.me/<?= preg_replace('/[^0-9]/', '', $ps['contact_whatsapp']) ?>?text=<?= urlencode('السلام عليكم، أريد طلب تفعيل رخصة الاشتراك السنوي لتطبيق مُسَرِّب') ?>" target="_blank" class="btn-plan btn-plan-popular">طلب التفعيل عبر واتساب ⚡</a>
        </div>

        <!-- Monthly Plan -->
        <div class="plan-card">
          <div>
            <h3 class="plan-name">الاشتراك الشهري</h3>
            <div class="plan-subtitle">مرونة في الاشتراك خلال مواسم الزراعة المحددة</div>
            <div class="plan-price">تجديد شهري <small>/ شهر بشهر</small></div>
            <ul class="plan-features-list">
              <li>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
                <span>عمليات ري غير محدودة خلال الشهر</span>
              </li>
              <li>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
                <span>إرسال السندات المالية عبر واتساب</span>
              </li>
              <li>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
                <span>تقارير الديون والإيرادات الإجمالية</span>
              </li>
            </ul>
          </div>
          <a href="https://wa.me/<?= preg_replace('/[^0-9]/', '', $ps['contact_whatsapp']) ?>?text=<?= urlencode('السلام عليكم، أريد طلب تفعيل الاشتراك الشهري لتطبيق مُسَرِّب') ?>" target="_blank" class="btn-plan">طلب تفعيل شهري</a>
        </div>
      </div>
    </section>
    <?php endif; ?>

    <!-- Developer & Support Section -->
    <?php if (!empty($ps['show_developer_section'])): ?>
    <section class="portal-section" id="developer-section">
      <div class="developer-card">
        <div class="dev-grid">
          <div class="dev-avatar">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
              <circle cx="12" cy="7" r="4"></circle>
            </svg>
          </div>
          <div>
            <div class="dev-name" id="devNameDisplay"><?= htmlspecialchars($ps['developer_name']) ?></div>
            <div class="dev-company" id="devCompanyDisplay"><?= htmlspecialchars($ps['developer_company']) ?></div>
            <p class="dev-desc" id="devDescDisplay"><?= htmlspecialchars($ps['developer_message']) ?></p>
          </div>
          <div class="dev-buttons">
            <?php if (!empty($ps['contact_whatsapp'])): ?>
            <a href="https://wa.me/<?= preg_replace('/[^0-9]/', '', $ps['contact_whatsapp']) ?>?text=<?= urlencode('السلام عليكم، أود التواصل مع المهندس مطور تطبيق مُسَرِّب') ?>" target="_blank" class="dev-btn dev-btn-whatsapp">
              💬 مراسلة واتساب مباشرة
            </a>
            <?php endif; ?>
            <?php if (!empty($ps['contact_phone'])): ?>
            <a href="tel:<?= htmlspecialchars($ps['contact_phone']) ?>" class="dev-btn dev-btn-phone">
              📞 اتصال: <?= htmlspecialchars($ps['contact_phone']) ?>
            </a>
            <?php endif; ?>
          </div>
        </div>
      </div>
    </section>
    <?php endif; ?>

    <!-- Direct Download CTA Box -->
    <?php if (!empty($ps['show_download_cta'])): ?>
    <section class="download-cta-box" id="download-section">
      <h2 class="download-cta-title">حمّل تطبيق مُسَرِّب وابدأ تنظيم بئرك اليوم</h2>
      <p class="download-cta-desc">
        احصل على النسخة الرسمية الأحدث بصيغة APK مباشرة، مع تجربة مجانية تشمل 200 عملية ري وسندات وكشوفات حساب متكاملة.
      </p>
      <div style="display: flex; justify-content: center; gap: 14px; flex-wrap: wrap;">
        <a href="<?= htmlspecialchars($release['download_url']) ?>" class="btn-primary-action" onclick="trackDownloadMetric()">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
            <polyline points="7 10 12 15 17 10"></polyline>
            <line x1="12" y1="15" x2="12" y2="3"></line>
          </svg>
          <span>تحميل تطبيق مُسَرِّب (v<?= htmlspecialchars($release['version_name']) ?>)</span>
        </a>
      </div>
      <div class="download-cta-specs">
        <span>الإصدار: v<?= htmlspecialchars($release['version_name']) ?></span>
        <span>•</span>
        <span>الحجم: <?= htmlspecialchars($release['file_size_mb']) ?> MB</span>
        <span>•</span>
        <span>فحص أمني معتمد وخالٍ من الفيروسات</span>
      </div>
    </section>
    <?php endif; ?>
  </main>

  <!-- Footer -->
  <footer class="main-footer">
    <div class="container">
      <div class="footer-content">
        <div style="font-weight: 700; color: #FFF;">
          منظومة مُسَرِّب (MOSARIB) &copy; <?= date('Y') ?>
        </div>
        <div style="color: var(--text-muted);">
          هندسة وبرمجة: <?= htmlspecialchars($ps['developer_name']) ?> • <?= htmlspecialchars($ps['developer_company']) ?>
        </div>
      </div>
      <div style="font-size: 11px; color: var(--text-dim);">
        جميع الحقوق محفوظة للمطور • نظام تشغيل وإدارة آبار المياه ودورات الري الزراعي
      </div>
    </div>
  </footer>

  <script>
    function trackDownloadMetric() {
      try {
        fetch('api/public_data.php?action=track_download', { method: 'POST' }).catch(() => {});
      } catch (_) {}
    }
  </script>
</body>
</html>
