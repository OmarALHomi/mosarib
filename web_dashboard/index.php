<?php
/**
 * Mosarib Official Public Portal
 * Light-Mode by Default with Dark Mode Switcher
 * Direct Subscription Request & Payment Accounts Integration
 */
if (!headers_sent()) {
    header('Content-Type: text/html; charset=UTF-8');
}
require_once __DIR__ . '/api/config.php';

$analyticsFile = __DIR__ . '/data/portal_analytics.json';
$today = date('Y-m-d');
$ip = $_SERVER['REMOTE_ADDR'] ?? '127.0.0.1';
$ua = $_SERVER['HTTP_USER_AGENT'] ?? '';
$visitorHash = substr(md5($ip . $ua . $today), 0, 16);

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

$sessionKey = 'visited_' . $today;
session_start();
if (empty($_SESSION[$sessionKey])) {
    $_SESSION[$sessionKey] = true;
    $analytics['total_visitors']++;
    $analytics['today_visitors']++;
    $analytics['unique_visitors']++;
}
$analytics['last_visit_at'] = time();

if (!isset($analytics['daily_stats'][$today])) {
    $analytics['daily_stats'][$today] = ['visitors' => 0, 'downloads' => 0];
}
$analytics['daily_stats'][$today]['visitors']++;

@file_put_contents($analyticsFile, json_encode($analytics, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT));

$settingsFile = __DIR__ . '/data/portal_settings.json';
$ps = [
    'active_region_name' => 'الجمهورية اليمنية (صنعاء، مأرب، الجوف، عمران)',
    'avg_hourly_price' => 7500,
    'min_hourly_price' => 5500,
    'max_hourly_price' => 9500,
    'currency_symbol' => 'ريال',
    'rates_note' => 'الأسعار تختلف بحسب نوع تشغيل البئر (منظومات طاقة شمسية أو محروقات ديزل) ونطاق المنطقة الجغرافية.',
    'water_status' => 'GOOD',
    'water_status_text' => 'مستقر ومتاح في معظم المناطق الزراعية',
    'water_index_percent' => 85,
    'headline_alert' => 'منظومة مُسَرِّب: إدارة دقيقة لساعات الري وسندات القبض وحسابات المزارعين بالثواني والدقائق.',
    'hero_badge' => 'المنظومة الرقمية الميدانية لإدارة مياه الآبار',
    'hero_title' => 'مُسَرِّب • حساب دقيق لمياه وساعات الري وحصص المزارعين',
    'hero_subtitle' => 'تطبيق ذكي خفيف يعمل بدون إنترنت (Offline-First)، يدير جلسات الري بالثانية والدقيقة، يرسل سندات القبض عبر واتساب، ويحفظ بياناتك بأمان على Google Drive.',
    'contact_whatsapp' => '967773712030',
    'contact_phone' => '773712030',
    'developer_name' => 'المهندس عمر الحومي',
    'developer_company' => 'تُبع سوفت للحلول البرمجية الذكية (TubbaSoft)',
    'developer_email' => 'omar@tubbasoft.com',
    'developer_message' => 'تطبيق مُسَرِّب صُمم خصيصاً لتلبية احتياجات ملاك الآبار والمزارعين بدقة متناهية وسهولة مطلقة في الميدان.',
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

// Payment Methods
$paymentMethodsFile = __DIR__ . '/data/payment_methods.json';
$paymentMethods = file_exists($paymentMethodsFile) ? (json_decode(file_get_contents($paymentMethodsFile), true) ?: []) : [];

// Subscription Pricing & Discounts
$pricingFile = __DIR__ . '/data/subscription_pricing.json';
$pricing = file_exists($pricingFile) ? (json_decode(file_get_contents($pricingFile), true) ?: []) : [];
$monthlyPrice = !empty($pricing['monthly']['price']) ? $pricing['monthly']['price'] : '10';
$monthlyOrig = !empty($pricing['monthly']['original_price']) ? $pricing['monthly']['original_price'] : '';
$monthlyDisc = !empty($pricing['monthly']['discount_text']) ? $pricing['monthly']['discount_text'] : '';
$monthlyCurrency = !empty($pricing['monthly']['currency']) ? $pricing['monthly']['currency'] : '$';

$yearlyPrice = !empty($pricing['yearly']['price']) ? $pricing['yearly']['price'] : '80';
$yearlyOrig = !empty($pricing['yearly']['original_price']) ? $pricing['yearly']['original_price'] : '120';
$yearlyDisc = !empty($pricing['yearly']['discount_text']) ? $pricing['yearly']['discount_text'] : 'وفر 33% (خصم 40$)';
$yearlyCurrency = !empty($pricing['yearly']['currency']) ? $pricing['yearly']['currency'] : '$';

$releaseFile = __DIR__ . '/data/release_info.json';
$release = [
    'version_name' => '1.4.0',
    'version_code' => 4,
    'file_size_mb' => 2.8,
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
<html lang="ar" dir="rtl" data-theme="light">
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
      /* Light Mode Palette (Default) */
      --primary: #00838F;
      --primary-dark: #004D40;
      --primary-light: #26A69A;
      --accent: #0284C7;
      --accent-glow: rgba(2, 132, 199, 0.15);
      --gold: #D97706;
      --gold-soft: rgba(217, 119, 6, 0.12);
      --bg: #F8FAFC;
      --surface: #FFFFFF;
      --surface-card: #FFFFFF;
      --surface-border: rgba(0, 131, 143, 0.16);
      --border-subtle: rgba(0, 0, 0, 0.08);
      --text: #1E293B;
      --text-muted: #0F766E;
      --text-dim: #64748B;
      --text-heading: #0F172A;
      --header-bg: rgba(255, 255, 255, 0.94);
      --ticker-bg: linear-gradient(90deg, #E0F2F1, #E0F7FA, #E0F2F1);
      --ticker-text: #004D40;
      --card-shadow: 0 10px 25px -5px rgba(0, 77, 64, 0.08), 0 8px 10px -6px rgba(0, 0, 0, 0.04);
      --input-bg: #F8FAFC;
      --input-border: #CBD5E1;
      --modal-overlay-bg: rgba(15, 23, 42, 0.65);
      --radius-sm: 8px;
      --radius-md: 14px;
      --radius-lg: 20px;
      --radius-full: 9999px;
    }

    [data-theme="dark"] {
      /* Dark Mode Palette */
      --primary: #00838F;
      --primary-dark: #004D40;
      --primary-light: #26A69A;
      --accent: #00B4D8;
      --accent-glow: rgba(0, 180, 216, 0.25);
      --gold: #FFB300;
      --gold-soft: rgba(255, 179, 0, 0.15);
      --bg: #071015;
      --surface: #0E1D24;
      --surface-card: rgba(14, 29, 36, 0.85);
      --surface-border: rgba(0, 180, 216, 0.18);
      --border-subtle: rgba(255, 255, 255, 0.08);
      --text: #E0F2F1;
      --text-muted: #80CBC4;
      --text-dim: #607D8B;
      --text-heading: #FFFFFF;
      --header-bg: rgba(7, 16, 21, 0.92);
      --ticker-bg: linear-gradient(90deg, #002B24, #004D40, #002B24);
      --ticker-text: #E0F7FA;
      --card-shadow: 0 12px 35px rgba(0, 0, 0, 0.4);
      --input-bg: rgba(7, 16, 21, 0.8);
      --input-border: rgba(0, 180, 216, 0.25);
      --modal-overlay-bg: rgba(0, 0, 0, 0.8);
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
        radial-gradient(circle at 15% 15%, rgba(0, 131, 143, 0.08) 0%, transparent 40%),
        radial-gradient(circle at 85% 25%, rgba(2, 132, 199, 0.07) 0%, transparent 45%),
        radial-gradient(circle at 50% 85%, rgba(0, 77, 64, 0.06) 0%, transparent 50%);
      background-attachment: fixed;
      color: var(--text);
      line-height: 1.6;
      overflow-x: hidden;
      -webkit-font-smoothing: antialiased;
      transition: background-color 0.3s ease, color 0.3s ease;
    }

    .container {
      max-width: 1140px;
      margin: 0 auto;
      padding: 0 20px;
    }

    /* Top Alert Ticker */
    .top-ticker {
      background: var(--ticker-bg);
      color: var(--ticker-text);
      font-size: 13px;
      padding: 8px 16px;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      border-bottom: 1px solid var(--surface-border);
      font-weight: 700;
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
      background: var(--header-bg);
      backdrop-filter: blur(16px);
      -webkit-backdrop-filter: blur(16px);
      border-bottom: 1px solid var(--border-subtle);
      padding: 12px 0;
      transition: background 0.3s ease, border-color 0.3s ease;
    }
    .header-content {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
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
      color: #FFFFFF;
      box-shadow: 0 4px 14px rgba(0, 131, 143, 0.3);
    }
    .brand-text b {
      color: var(--text-heading);
      font-size: 19px;
      font-weight: 900;
      letter-spacing: 0.5px;
      display: block;
      line-height: 1.2;
    }
    .brand-text span {
      color: var(--text-muted);
      font-size: 11.5px;
      font-weight: 600;
    }

    .nav-links {
      display: flex;
      align-items: center;
      gap: 20px;
    }
    .nav-link {
      color: var(--text);
      text-decoration: none;
      font-size: 13.5px;
      font-weight: 700;
      transition: color 0.2s ease;
    }
    .nav-link:hover {
      color: var(--accent);
    }

    .header-actions {
      display: flex;
      align-items: center;
      gap: 10px;
    }

    /* Theme Toggle Button */
    .btn-theme-toggle {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 7px 14px;
      border-radius: var(--radius-full);
      background: var(--surface);
      border: 1px solid var(--surface-border);
      color: var(--text);
      font-size: 12.5px;
      font-weight: 800;
      cursor: pointer;
      transition: all 0.2s ease;
      box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
    }
    .btn-theme-toggle:hover {
      border-color: var(--primary);
      transform: translateY(-1px);
    }

    .btn-header-sub {
      background: linear-gradient(135deg, #D97706, #B45309);
      color: #FFFFFF;
      border: none;
      padding: 8px 16px;
      border-radius: var(--radius-full);
      font-size: 13px;
      font-weight: 800;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      gap: 6px;
      box-shadow: 0 4px 12px rgba(217, 119, 6, 0.25);
      transition: all 0.2s ease;
    }
    .btn-header-sub:hover {
      transform: translateY(-1px);
      box-shadow: 0 6px 16px rgba(217, 119, 6, 0.35);
    }

    .btn-header-cta {
      background: linear-gradient(135deg, #00838F, #004D40);
      color: #FFFFFF;
      text-decoration: none;
      padding: 8px 16px;
      border-radius: var(--radius-full);
      font-size: 13px;
      font-weight: 800;
      display: inline-flex;
      align-items: center;
      gap: 6px;
      box-shadow: 0 4px 12px rgba(0, 131, 143, 0.25);
      transition: all 0.2s ease;
    }
    .btn-header-cta:hover {
      transform: translateY(-1px);
      box-shadow: 0 6px 18px rgba(0, 131, 143, 0.35);
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
      background: var(--surface);
      border: 1px solid var(--surface-border);
      padding: 6px 16px;
      border-radius: var(--radius-full);
      font-size: 12.5px;
      font-weight: 700;
      color: var(--accent);
      margin-bottom: 18px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
    }
    .hero-title {
      font-size: 34px;
      font-weight: 900;
      color: var(--text-heading);
      line-height: 1.35;
      max-width: 820px;
      margin: 0 auto 16px auto;
      letter-spacing: -0.5px;
    }
    .hero-subtitle {
      font-size: 15.5px;
      color: var(--text-dim);
      max-width: 720px;
      margin: 0 auto 28px auto;
      line-height: 1.7;
    }
    .hero-actions {
      display: flex;
      justify-content: center;
      gap: 14px;
      flex-wrap: wrap;
      margin-bottom: 24px;
    }
    .btn-primary-action {
      background: linear-gradient(135deg, #00838F, #004D40);
      color: #FFFFFF;
      padding: 13px 28px;
      border-radius: var(--radius-full);
      font-size: 15px;
      font-weight: 800;
      text-decoration: none;
      display: inline-flex;
      align-items: center;
      gap: 10px;
      box-shadow: 0 6px 20px rgba(0, 131, 143, 0.3);
      transition: all 0.2s ease;
      cursor: pointer;
      border: none;
    }
    .btn-primary-action:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 24px rgba(0, 131, 143, 0.4);
    }
    .btn-sub-action {
      background: linear-gradient(135deg, #D97706, #B45309);
      color: #FFFFFF;
      padding: 13px 28px;
      border-radius: var(--radius-full);
      font-size: 15px;
      font-weight: 800;
      text-decoration: none;
      display: inline-flex;
      align-items: center;
      gap: 10px;
      box-shadow: 0 6px 20px rgba(217, 119, 6, 0.28);
      transition: all 0.2s ease;
      cursor: pointer;
      border: none;
    }
    .btn-sub-action:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 24px rgba(217, 119, 6, 0.38);
    }
    .btn-whatsapp-action {
      background: rgba(37, 211, 102, 0.12);
      border: 1px solid rgba(37, 211, 102, 0.3);
      color: #128C7E;
      padding: 13px 24px;
      border-radius: var(--radius-full);
      font-size: 15px;
      font-weight: 800;
      text-decoration: none;
      display: inline-flex;
      align-items: center;
      gap: 10px;
      transition: all 0.2s ease;
    }
    [data-theme="dark"] .btn-whatsapp-action {
      color: #25D366;
    }
    .btn-whatsapp-action:hover {
      background: rgba(37, 211, 102, 0.2);
      transform: translateY(-2px);
    }
    .hero-trust-bar {
      display: flex;
      justify-content: center;
      align-items: center;
      gap: 16px;
      flex-wrap: wrap;
      font-size: 12.5px;
      color: var(--text-dim);
      font-weight: 700;
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
      color: var(--primary);
      text-transform: uppercase;
      letter-spacing: 1px;
      margin-bottom: 6px;
      display: block;
    }
    .section-title {
      font-size: 26px;
      font-weight: 900;
      color: var(--text-heading);
      margin-bottom: 8px;
    }
    .section-desc {
      font-size: 14.5px;
      color: var(--text-dim);
      max-width: 620px;
      margin: 0 auto;
    }

    /* Rates Box */
    .rates-box {
      background: var(--surface-card);
      border: 1px solid var(--surface-border);
      border-radius: var(--radius-lg);
      padding: 26px;
      box-shadow: var(--card-shadow);
    }
    .region-banner {
      background: linear-gradient(135deg, rgba(0, 131, 143, 0.08), rgba(0, 77, 64, 0.12));
      border: 1px solid var(--surface-border);
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
      color: var(--accent);
      flex-shrink: 0;
    }
    .region-label {
      font-size: 12px;
      color: var(--text-dim);
      font-weight: 600;
    }
    .region-name {
      font-size: 16px;
      font-weight: 800;
      color: var(--text-heading);
    }
    .water-status-pill {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      padding: 6px 14px;
      border-radius: var(--radius-full);
      background: rgba(0, 230, 118, 0.12);
      border: 1px solid rgba(0, 230, 118, 0.3);
      color: #008744;
      font-size: 12.5px;
      font-weight: 800;
    }
    [data-theme="dark"] .water-status-pill {
      color: #00E676;
    }
    .rates-cards-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 16px;
      margin-bottom: 20px;
    }
    .rate-card {
      background: var(--surface);
      border: 1px solid var(--surface-border);
      border-radius: var(--radius-md);
      padding: 20px;
      text-align: center;
      position: relative;
      overflow: hidden;
      box-shadow: 0 4px 12px rgba(0,0,0,0.02);
    }
    .rate-card.highlight {
      border-color: var(--primary);
      background: linear-gradient(180deg, var(--surface) 0%, rgba(0, 131, 143, 0.05) 100%);
    }
    .rate-type {
      font-size: 12px;
      color: var(--text-dim);
      font-weight: 700;
      margin-bottom: 6px;
    }
    .rate-amount {
      font-size: 28px;
      font-weight: 900;
      color: var(--text-heading);
      line-height: 1.2;
      margin-bottom: 4px;
    }
    .rate-amount.primary { color: var(--primary); }
    .rate-currency {
      font-size: 12px;
      color: var(--text-dim);
      font-weight: 600;
    }
    .rates-note-bar {
      background: var(--surface);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-sm);
      padding: 12px 16px;
      font-size: 12.5px;
      color: var(--text-dim);
      display: flex;
      align-items: center;
      gap: 10px;
    }

    /* Features Grid */
    .features-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 20px;
    }
    .feature-card {
      background: var(--surface-card);
      border: 1px solid var(--surface-border);
      border-radius: var(--radius-md);
      padding: 24px;
      transition: all 0.25s ease;
      box-shadow: var(--card-shadow);
    }
    .feature-card:hover {
      transform: translateY(-4px);
      border-color: var(--primary);
    }
    .feature-icon-box {
      width: 48px;
      height: 48px;
      border-radius: 12px;
      background: rgba(0, 131, 143, 0.12);
      border: 1px solid rgba(0, 131, 143, 0.25);
      display: flex;
      align-items: center;
      justify-content: center;
      color: var(--primary);
      margin-bottom: 16px;
    }
    .feature-title {
      font-size: 17px;
      font-weight: 800;
      color: var(--text-heading);
      margin-bottom: 8px;
    }
    .feature-desc {
      font-size: 13.5px;
      color: var(--text-dim);
      line-height: 1.6;
    }

    /* Plans Grid */
    .plans-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 22px;
      align-items: stretch;
    }
    .plan-card {
      background: var(--surface-card);
      border: 1px solid var(--surface-border);
      border-radius: var(--radius-lg);
      padding: 30px 24px;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      position: relative;
      box-shadow: var(--card-shadow);
      transition: all 0.25s ease;
    }
    .plan-card:hover {
      transform: translateY(-4px);
    }
    .plan-card-popular {
      border: 2px solid var(--gold);
      box-shadow: 0 14px 40px rgba(217, 119, 6, 0.16);
    }
    .plan-badge {
      position: absolute;
      top: -13px;
      left: 50%;
      transform: translateX(-50%);
      background: linear-gradient(135deg, #D97706, #B45309);
      color: #FFFFFF;
      font-size: 11.5px;
      font-weight: 900;
      padding: 4px 16px;
      border-radius: var(--radius-full);
      box-shadow: 0 4px 12px rgba(217, 119, 6, 0.3);
      white-space: nowrap;
    }
    .plan-name {
      font-size: 20px;
      font-weight: 900;
      color: var(--text-heading);
      margin-bottom: 6px;
    }
    .plan-subtitle {
      font-size: 13px;
      color: var(--text-dim);
      margin-bottom: 16px;
      min-height: 38px;
    }
    .plan-price-wrap {
      display: flex;
      align-items: baseline;
      gap: 8px;
      margin-bottom: 18px;
      flex-wrap: wrap;
    }
    .plan-old-price {
      font-size: 16px;
      color: var(--text-dim);
      text-decoration: line-through;
      font-weight: 700;
    }
    .plan-price {
      font-size: 32px;
      font-weight: 900;
      color: var(--text-heading);
    }
    .plan-price small {
      font-size: 13px;
      color: var(--text-dim);
      font-weight: 600;
    }
    .plan-discount-badge {
      display: inline-block;
      background: rgba(217, 119, 6, 0.12);
      border: 1px solid rgba(217, 119, 6, 0.3);
      color: var(--gold);
      font-size: 11px;
      font-weight: 800;
      padding: 2px 8px;
      border-radius: var(--radius-full);
    }
    .plan-features-list {
      list-style: none;
      margin-bottom: 24px;
    }
    .plan-features-list li {
      display: flex;
      align-items: flex-start;
      gap: 10px;
      font-size: 13px;
      color: var(--text);
      margin-bottom: 10px;
      line-height: 1.5;
    }
    .plan-features-list li svg {
      color: var(--primary);
      flex-shrink: 0;
      margin-top: 3px;
    }
    .plan-features-list li.feature-highlight {
      background: rgba(0, 131, 143, 0.06);
      padding: 6px 10px;
      border-radius: var(--radius-sm);
      border: 1px dashed var(--surface-border);
      font-weight: 700;
    }
    .btn-plan {
      background: var(--surface);
      border: 1px solid var(--surface-border);
      color: var(--text-heading);
      padding: 11px 18px;
      border-radius: var(--radius-full);
      font-size: 13.5px;
      font-weight: 800;
      text-decoration: none;
      display: block;
      text-align: center;
      transition: all 0.2s ease;
      cursor: pointer;
      width: 100%;
    }
    .btn-plan:hover {
      border-color: var(--primary);
      background: rgba(0, 131, 143, 0.05);
      transform: translateY(-1px);
    }
    .btn-plan-popular {
      background: linear-gradient(135deg, #00838F, #004D40);
      border: none;
      color: #FFFFFF;
      box-shadow: 0 4px 14px rgba(0, 131, 143, 0.3);
    }
    .btn-plan-popular:hover {
      background: linear-gradient(135deg, #0097A7, #00695C);
      color: #FFFFFF;
      box-shadow: 0 6px 18px rgba(0, 131, 143, 0.4);
    }
    .btn-plan-sub {
      color: var(--text-dim);
      font-size: 12px;
      font-weight: 700;
      text-align: center;
      text-decoration: none;
      display: block;
      transition: color 0.2s ease;
    }
    .btn-plan-sub:hover {
      color: #25D366;
    }

    /* Developer Section */
    .developer-card {
      background: var(--surface-card);
      border: 1px solid var(--surface-border);
      border-radius: var(--radius-lg);
      padding: 28px;
      box-shadow: var(--card-shadow);
    }
    .dev-grid {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 20px;
      flex-wrap: wrap;
    }
    .dev-avatar {
      width: 60px;
      height: 60px;
      border-radius: 16px;
      background: linear-gradient(135deg, #00838F, #004D40);
      display: flex;
      align-items: center;
      justify-content: center;
      color: #FFFFFF;
      box-shadow: 0 4px 14px rgba(0, 131, 143, 0.3);
      flex-shrink: 0;
    }
    .dev-name {
      font-size: 18px;
      font-weight: 900;
      color: var(--text-heading);
    }
    .dev-company {
      font-size: 13px;
      color: var(--primary);
      font-weight: 700;
      margin-bottom: 4px;
    }
    .dev-desc {
      font-size: 13px;
      color: var(--text-dim);
      max-width: 500px;
    }
    .dev-buttons {
      display: flex;
      gap: 10px;
      flex-wrap: wrap;
    }
    .dev-btn {
      padding: 9px 18px;
      border-radius: var(--radius-full);
      font-size: 13px;
      font-weight: 700;
      text-decoration: none;
      display: inline-flex;
      align-items: center;
      gap: 6px;
    }
    .dev-btn-whatsapp {
      background: rgba(37, 211, 102, 0.12);
      border: 1px solid rgba(37, 211, 102, 0.3);
      color: #128C7E;
    }
    [data-theme="dark"] .dev-btn-whatsapp { color: #25D366; }
    .dev-btn-phone {
      background: var(--surface);
      border: 1px solid var(--border-subtle);
      color: var(--text);
    }

    /* Download CTA Box */
    .download-cta-box {
      margin: 40px 0;
      background: linear-gradient(135deg, rgba(0, 131, 143, 0.12), rgba(0, 77, 64, 0.18));
      border: 1px solid var(--surface-border);
      border-radius: var(--radius-lg);
      padding: 40px 24px;
      text-align: center;
      box-shadow: var(--card-shadow);
    }
    .download-cta-title {
      font-size: 26px;
      font-weight: 900;
      color: var(--text-heading);
      margin-bottom: 10px;
    }
    .download-cta-desc {
      font-size: 14.5px;
      color: var(--text-dim);
      max-width: 600px;
      margin: 0 auto 24px auto;
    }
    .download-cta-specs {
      display: flex;
      justify-content: center;
      gap: 12px;
      font-size: 12.5px;
      color: var(--text-dim);
      margin-top: 18px;
    }

    /* Footer */
    footer.main-footer {
      border-top: 1px solid var(--border-subtle);
      padding: 24px 0;
      text-align: center;
      font-size: 12px;
      background: var(--surface);
    }
    .footer-content {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 12px;
      margin-bottom: 6px;
    }

    /* Direct Subscription Modal */
    .modal-overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: var(--modal-overlay-bg);
      backdrop-filter: blur(8px);
      -webkit-backdrop-filter: blur(8px);
      z-index: 2000;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 16px;
      animation: fadeIn 0.2s ease-out;
    }
    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }
    .modal-card {
      background: var(--surface);
      border: 1px solid var(--surface-border);
      border-radius: var(--radius-lg);
      width: 100%;
      max-width: 680px;
      max-height: 90vh;
      overflow-y: auto;
      box-shadow: 0 20px 60px rgba(0, 0, 0, 0.4);
      animation: slideUp 0.25s cubic-bezier(0.16, 1, 0.3, 1);
      position: relative;
    }
    @keyframes slideUp {
      from { transform: translateY(24px) scale(0.97); }
      to { transform: translateY(0) scale(1); }
    }
    .modal-header {
      padding: 20px 24px 16px 24px;
      border-bottom: 1px solid var(--border-subtle);
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 12px;
    }
    .modal-title {
      font-size: 19px;
      font-weight: 900;
      color: var(--text-heading);
      line-height: 1.3;
    }
    .modal-subtitle {
      font-size: 13px;
      color: var(--text-dim);
      margin-top: 4px;
    }
    .modal-close-btn {
      background: transparent;
      border: none;
      font-size: 26px;
      color: var(--text-dim);
      cursor: pointer;
      line-height: 1;
      padding: 4px;
      transition: color 0.15s ease;
    }
    .modal-close-btn:hover {
      color: #EF5350;
    }
    .modal-body {
      padding: 20px 24px 24px 24px;
    }
    .modal-label {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 13.5px;
      font-weight: 800;
      color: var(--text-heading);
      margin-bottom: 8px;
    }
    .plan-radio-group {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 12px;
      margin-bottom: 16px;
    }
    .plan-radio-card {
      border: 2px solid var(--surface-border);
      background: var(--input-bg);
      border-radius: var(--radius-md);
      padding: 12px;
      cursor: pointer;
      transition: all 0.2s ease;
      display: flex;
      align-items: flex-start;
      gap: 10px;
    }
    .plan-radio-card.active {
      border-color: var(--gold);
      background: rgba(217, 119, 6, 0.08);
    }
    .plan-radio-card input {
      margin-top: 4px;
      accent-color: var(--gold);
    }
    .plan-radio-title {
      font-size: 14px;
      font-weight: 800;
      color: var(--text-heading);
    }
    .plan-radio-price {
      font-size: 14.5px;
      color: var(--primary);
      font-weight: 800;
      margin: 2px 0;
    }
    .plan-radio-price .old-price {
      text-decoration: line-through;
      color: var(--text-dim);
      font-size: 12px;
      font-weight: normal;
    }
    .plan-radio-features {
      font-size: 11px;
      color: var(--text-dim);
      line-height: 1.4;
    }

    /* Payment Accounts inside Modal */
    .payment-methods-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 10px;
      margin-bottom: 16px;
    }
    .payment-method-card {
      background: var(--input-bg);
      border: 1px solid var(--surface-border);
      border-radius: var(--radius-md);
      padding: 12px;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
    }
    .pm-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 6px;
      gap: 6px;
    }
    .pm-bank {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 12.5px;
      font-weight: 800;
      color: var(--text-heading);
    }
    .pm-logo {
      width: 20px;
      height: 20px;
      border-radius: 4px;
      object-fit: contain;
    }
    .pm-currency-badge {
      background: rgba(0, 131, 143, 0.12);
      color: var(--primary);
      font-size: 10.5px;
      font-weight: 800;
      padding: 1px 6px;
      border-radius: var(--radius-sm);
    }
    .pm-number-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      background: var(--surface);
      border: 1px dashed var(--surface-border);
      border-radius: var(--radius-sm);
      padding: 6px 8px;
      margin: 4px 0;
    }
    .pm-number {
      font-family: monospace;
      font-size: 13.5px;
      font-weight: 900;
      color: var(--text-heading);
      letter-spacing: 0.5px;
    }
    .btn-copy-acc {
      background: var(--primary);
      color: #FFFFFF;
      border: none;
      padding: 3px 8px;
      border-radius: 4px;
      font-size: 11px;
      font-weight: 700;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      gap: 4px;
      transition: background 0.15s ease;
    }
    .btn-copy-acc:hover {
      background: var(--primary-dark);
    }
    .pm-holder {
      font-size: 11px;
      color: var(--text-dim);
    }
    .pm-notes {
      font-size: 10.5px;
      color: var(--text-dim);
      margin-top: 2px;
    }

    /* Form Fields */
    .form-row-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 12px;
    }
    .form-field {
      display: flex;
      flex-direction: column;
      gap: 5px;
    }
    .field-label {
      font-size: 12.5px;
      font-weight: 700;
      color: var(--text-heading);
    }
    .form-input {
      background: var(--input-bg);
      border: 1px solid var(--input-border);
      border-radius: var(--radius-sm);
      padding: 9px 12px;
      font-size: 13.5px;
      color: var(--text);
      outline: none;
      transition: border-color 0.2s ease;
    }
    .form-input:focus {
      border-color: var(--primary);
      box-shadow: 0 0 0 3px rgba(0, 131, 143, 0.12);
    }

    .modal-actions-row {
      display: flex;
      gap: 10px;
    }
    .btn-modal-submit {
      flex: 1.4;
      background: linear-gradient(135deg, #00838F, #004D40);
      color: #FFFFFF;
      border: none;
      padding: 12px;
      border-radius: var(--radius-md);
      font-size: 14.5px;
      font-weight: 800;
      cursor: pointer;
      box-shadow: 0 4px 14px rgba(0, 131, 143, 0.3);
      transition: all 0.2s ease;
    }
    .btn-modal-submit:hover {
      transform: translateY(-1px);
      box-shadow: 0 6px 18px rgba(0, 131, 143, 0.4);
    }
    .btn-modal-submit:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
    .btn-modal-whatsapp {
      flex: 1;
      background: rgba(37, 211, 102, 0.15);
      border: 1px solid rgba(37, 211, 102, 0.35);
      color: #128C7E;
      padding: 12px;
      border-radius: var(--radius-md);
      font-size: 13.5px;
      font-weight: 800;
      cursor: pointer;
      transition: all 0.2s ease;
    }
    [data-theme="dark"] .btn-modal-whatsapp { color: #25D366; }
    .btn-modal-whatsapp:hover {
      background: rgba(37, 211, 102, 0.25);
    }

    /* Responsive Design */
    @media (max-width: 900px) {
      .plans-grid, .features-grid, .rates-cards-grid {
        grid-template-columns: 1fr;
      }
      .payment-methods-grid {
        grid-template-columns: 1fr;
      }
      .plan-radio-group {
        grid-template-columns: 1fr;
      }
      .form-row-grid {
        grid-template-columns: 1fr;
      }
      .nav-links {
        display: none;
      }
      .hero-title {
        font-size: 26px;
      }
      .header-content {
        gap: 8px;
      }
      .btn-header-cta span {
        display: none;
      }
      .btn-header-cta {
        padding: 8px 10px;
      }
      .modal-actions-row {
        flex-direction: column;
      }
    }
    @media (max-width: 480px) {
      .hero-title {
        font-size: 22px;
      }
      .hero-subtitle {
        font-size: 14px;
      }
      .dev-grid {
        flex-direction: column;
        text-align: center;
      }
      .dev-buttons {
        justify-content: center;
      }
    }
  </style>
</head>
<body>

  <!-- Top Alert Ticker -->
  <?php if (!empty($ps['show_top_ticker']) && !empty($ps['headline_alert'])): ?>
  <div class="top-ticker" id="topTickerBar">
    <div class="pulse-dot"></div>
    <span id="headlineAlertText"><?= htmlspecialchars($ps['headline_alert']) ?></span>
  </div>
  <?php endif; ?>

  <!-- Main Header -->
  <header class="main-header">
    <div class="container header-content">
      <a href="#" class="brand-logo">
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

      <div class="header-actions">
        <!-- Theme Switcher -->
        <button id="themeToggleBtn" onclick="togglePortalTheme()" class="btn-theme-toggle" title="تبديل المظهر">
          <span id="themeToggleIcon">🌙</span>
          <span id="themeToggleLabel" class="theme-toggle-text">الوضع الليلي</span>
        </button>

        <!-- Subscription Button -->
        <button onclick="openSubscriptionModal('YEARLY')" class="btn-header-sub">
          <span>طلب اشتراك ⚡</span>
        </button>

        <a href="<?= htmlspecialchars($release['download_url']) ?>" class="btn-header-cta" onclick="trackDownloadMetric()">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
            <polyline points="7 10 12 15 17 10"></polyline>
            <line x1="12" y1="15" x2="12" y2="3"></line>
          </svg>
          <span>تحميل APK</span>
        </a>
      </div>
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
        <button onclick="openSubscriptionModal('YEARLY')" class="btn-sub-action">
          <span>طلب اشتراك وشراء مباشر ⚡</span>
        </button>

        <a href="<?= htmlspecialchars($release['download_url']) ?>" class="btn-primary-action" onclick="trackDownloadMetric()">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
            <polyline points="7 10 12 15 17 10"></polyline>
            <line x1="12" y1="15" x2="12" y2="3"></line>
          </svg>
          <span>تحميل التطبيق مجاناً</span>
        </a>

        <?php if (!empty($ps['contact_whatsapp'])): ?>
        <a href="https://wa.me/<?= preg_replace('/[^0-9]/', '', $ps['contact_whatsapp']) ?>?text=<?= urlencode('السلام عليكم، أود الاستفسار عن ترخيص وتفعيل تطبيق مُسَرِّب لإدارة مياه الآبار') ?>" target="_blank" class="btn-whatsapp-action">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12.031 6.172c-3.181 0-5.767 2.586-5.768 5.766-.001 1.298.38 2.27 1.019 3.287l-.582 2.128 2.182-.573c.978.58 1.911.928 3.145.929 3.178 0 5.767-2.587 5.768-5.766.001-3.187-2.575-5.77-5.764-5.771zm3.392 8.244c-.144.405-.837.774-1.17.824-.312.045-.694.073-2.125-.522-1.83-1.074-3.003-2.883-3.095-3.005-.091-.122-.736-.98-.736-1.87 0-.89.464-1.328.629-1.507.165-.18.36-.225.48-.225.12 0 .24.001.345.006.111.005.26-.042.406.31.15.361.51 1.246.555 1.337.045.09.075.195.015.315-.06.12-.09.195-.18.3-.09.105-.19.234-.27.315-.09.09-.184.187-.079.367.105.18.468.772 1.004 1.249.691.614 1.274.805 1.454.895.18.09.285.075.39-.045.105-.12.45-.525.57-.705.12-.18.24-.15.405-.09.165.06 1.05.495 1.23.585.18.09.3.135.345.21.045.075.045.435-.099.84z"/>
          </svg>
          <span>تواصل واتساب</span>
        </a>
        <?php endif; ?>
      </div>

      <div class="hero-trust-bar">
        <span>⚡ يعمل 100% بدون إنترنت (Offline)</span>
        <span>•</span>
        <span>🛡️ فحص أمني معتمد</span>
        <span>•</span>
        <span>☁️ نسخ احتياطي يومي Google Drive</span>
        <span>•</span>
        <span>📦 الحجم: <?= htmlspecialchars($release['file_size_mb']) ?> MB</span>
      </div>
    </section>

    <!-- Regional Hourly Rates Section -->
    <?php if (!empty($ps['show_rates_section'])): ?>
    <section class="portal-section" id="rates-section">
      <div class="section-header">
        <span class="section-tag">مؤشر سوق الري وضخ الآبار</span>
        <h2 class="section-title">متوسط تسعيرة ساعة الري</h2>
        <p class="section-desc">مؤشرات حية لأسعار ساعات ضخ مياه الآبار لمساعدة الملاك والمزارعين على ضبط تكاليف السقي بدقة.</p>
      </div>

      <div class="rates-box">
        <div class="region-banner">
          <div class="region-info">
            <div class="region-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
                <circle cx="12" cy="10" r="3"></circle>
              </svg>
            </div>
            <div>
              <div class="region-label">نطاق البورصة والمنطقة الجغرافية:</div>
              <div class="region-name" id="activeRegionNameDisplay"><?= htmlspecialchars($ps['active_region_name']) ?></div>
            </div>
          </div>
          <div class="water-status-pill">
            <span class="pulse-dot" style="background:#00E676; box-shadow:0 0 6px #00E676;"></span>
            <span>حالة توفر المياه: <?= htmlspecialchars($ps['water_status_text']) ?></span>
          </div>
        </div>

        <div class="rates-cards-grid">
          <div class="rate-card">
            <div class="rate-type">أدنى تسعيرة مسجلة</div>
            <div class="rate-amount" id="minPriceDisplay"><?= number_format($ps['min_hourly_price']) ?></div>
            <div class="rate-currency"><?= htmlspecialchars($ps['currency_symbol']) ?> / ساعة</div>
          </div>

          <div class="rate-card highlight">
            <div class="rate-type">متوسط سعر الساعة العام ⭐</div>
            <div class="rate-amount primary" id="avgPriceDisplay"><?= number_format($ps['avg_hourly_price']) ?></div>
            <div class="rate-currency"><?= htmlspecialchars($ps['currency_symbol']) ?> / ساعة ضخ</div>
          </div>

          <div class="rate-card">
            <div class="rate-type">أعلى تسعيرة مسجلة</div>
            <div class="rate-amount" id="maxPriceDisplay"><?= number_format($ps['max_hourly_price']) ?></div>
            <div class="rate-currency"><?= htmlspecialchars($ps['currency_symbol']) ?> / ساعة</div>
          </div>
        </div>

        <div class="rates-note-bar">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="12" y1="16" x2="12" y2="12"></line>
            <line x1="12" y1="8" x2="12.01" y2="8"></line>
          </svg>
          <span id="ratesNoteText"><?= htmlspecialchars($ps['rates_note']) ?></span>
        </div>
      </div>
    </section>
    <?php endif; ?>

    <!-- Features Section -->
    <?php if (!empty($ps['show_features_section'])): ?>
    <section class="portal-section" id="features-section">
      <div class="section-header">
        <span class="section-tag">لماذا يختار ملاك الآبار تطبيق مُسَرِّب؟</span>
        <h2 class="section-title">حلول ميدانية متكاملة لضبط المياه والأموال</h2>
        <p class="section-desc">صُمم التطبيق خصيصاً ليناسب بيئة الميدان الزراعي مع التركيز على السرعة، الدقة، والعمل بدون إنترنت.</p>
      </div>

      <div class="features-grid">
        <div class="feature-card">
          <div class="feature-icon-box">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"></circle>
              <polyline points="12 6 12 12 16 14"></polyline>
            </svg>
          </div>
          <h3 class="feature-title">حساب دقيق لكسور الساعات</h3>
          <p class="feature-desc">تحديد دقيق لأوقات السقي بالثواني والدقائق وحساب الفاقد المائي لضمان حفظ حقوق مالك البئر والمزارع دون أي لبس.</p>
        </div>

        <div class="feature-card">
          <div class="feature-icon-box">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
              <polyline points="14 2 14 8 20 8"></polyline>
              <line x1="16" y1="13" x2="8" y2="13"></line>
              <line x1="16" y1="17" x2="8" y2="17"></line>
            </svg>
          </div>
          <h3 class="feature-title">سندات قبض فورية ومشاركتها</h3>
          <p class="feature-desc">إصدار سندات مالية وطباعتها أو مشاركتها كرسائل رسمية وموثقة عبر واتساب للمزارعين فور انتهاء دورة السقي.</p>
        </div>

        <div class="feature-card">
          <div class="feature-icon-box">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M18 10h-1.26A8 8 0 1 0 9 20h9a5 5 0 0 0 0-10z"></path>
            </svg>
          </div>
          <h3 class="feature-title">أمان تام ونسخ احتياطي يومي</h3>
          <p class="feature-desc">حفظ تلقائي لكافة البيانات على جهازك مع دعم النسخ الاحتياطي اليومي على Google Drive الخاص بك لضمان عدم ضياع أي سجل.</p>
        </div>
      </div>
    </section>
    <?php endif; ?>

    <!-- Subscription Plans Section -->
    <?php if (!empty($ps['show_plans_section'])): ?>
    <section class="portal-section" id="plans-section">
      <div class="section-header">
        <span class="section-tag">باقات الاشتراك الرسمية</span>
        <h2 class="section-title">اختر الباقة المناسبة لبئرك ومزرعتك</h2>
        <p class="section-desc">ترخيص كامل لكافة مزايا تطبيق مُسَرِّب مع دعم فني مستمر، تحديثات فورية، وإمكانية نقل الاشتراك لأي هاتف بديل في أي وقت.</p>
      </div>

      <div class="plans-grid">
        <!-- Free Trial -->
        <div class="plan-card">
          <div>
            <h3 class="plan-name">النسخة التجريبية المجانية</h3>
            <div class="plan-subtitle">لتجربة واختبار النظام الميداني في البئر</div>
            <div class="plan-price-wrap">
              <span class="plan-price">مجاناً <small>/ 200 عملية ري</small></span>
            </div>
            <ul class="plan-features-list">
              <li>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
                <span>حساب أوقات السقي بالدقائق والثواني</span>
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
          <span class="plan-badge">الأكثر طلباً وتوفيراً ⭐</span>
          <div>
            <h3 class="plan-name">الاشتراك السنوي الشامل</h3>
            <div class="plan-subtitle">الترخيص المكتمل لمالك البئر لموسم زراعي كامل</div>
            <div class="plan-price-wrap">
              <?php if (!empty($yearlyOrig)): ?>
              <span class="plan-old-price"><?= htmlspecialchars($yearlyOrig . $yearlyCurrency) ?></span>
              <?php endif; ?>
              <span class="plan-price"><?= htmlspecialchars($yearlyPrice . $yearlyCurrency) ?> <small>/ سنة كاملة</small></span>
              <?php if (!empty($yearlyDisc)): ?>
              <span class="plan-discount-badge"><?= htmlspecialchars($yearlyDisc) ?></span>
              <?php endif; ?>
            </div>
            <ul class="plan-features-list">
              <li class="feature-highlight">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
                <span><b>دعم فني مباشر عند الحاجة 🛠️</b></span>
              </li>
              <li class="feature-highlight">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
                <span><b>تحديثات متواصلة وحل مشاكل بشكل فوري ⚡</b></span>
              </li>
              <li class="feature-highlight">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
                <span><b>إمكانية تغيير الهاتف ونقل الاشتراك بأي وقت 📱</b></span>
              </li>
              <li>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
                <span>عمليات ري وسندات غير محدودة إطلاقاً</span>
              </li>
              <li>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
                <span>النسخ الاحتياطي السحابي اليومي على Google Drive</span>
              </li>
              <li>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
                <span>تصدير كشوفات الحسابات PDF وإكسل Excel</span>
              </li>
            </ul>
          </div>
          <div style="display:flex; flex-direction:column; gap:8px;">
            <button onclick="openSubscriptionModal('YEARLY')" class="btn-plan btn-plan-popular">طلب اشتراك وشراء مباشر ⚡</button>
            <a href="https://wa.me/<?= preg_replace('/[^0-9]/', '', $ps['contact_whatsapp']) ?>?text=<?= urlencode('السلام عليكم، أريد طلب تفعيل رخصة الاشتراك السنوي (' . $yearlyPrice . '$) لتطبيق مُسَرِّب') ?>" target="_blank" class="btn-plan-sub">أو تفعيل عبر واتساب 💬</a>
          </div>
        </div>

        <!-- Monthly Plan -->
        <div class="plan-card">
          <div>
            <h3 class="plan-name">الاشتراك الشهري</h3>
            <div class="plan-subtitle">مرونة في الاشتراك بحسب فترات ومواسم السقي</div>
            <div class="plan-price-wrap">
              <?php if (!empty($monthlyOrig)): ?>
              <span class="plan-old-price"><?= htmlspecialchars($monthlyOrig . $monthlyCurrency) ?></span>
              <?php endif; ?>
              <span class="plan-price"><?= htmlspecialchars($monthlyPrice . $monthlyCurrency) ?> <small>/ شهرياً</small></span>
              <?php if (!empty($monthlyDisc)): ?>
              <span class="plan-discount-badge"><?= htmlspecialchars($monthlyDisc) ?></span>
              <?php endif; ?>
            </div>
            <ul class="plan-features-list">
              <li class="feature-highlight">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
                <span><b>دعم فني مباشر عند الحاجة 🛠️</b></span>
              </li>
              <li class="feature-highlight">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
                <span><b>تحديثات متواصلة وحل مشاكل بشكل فوري ⚡</b></span>
              </li>
              <li class="feature-highlight">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
                <span><b>إمكانية تغيير الهاتف ونقل الاشتراك بأي وقت 📱</b></span>
              </li>
              <li>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
                <span>عمليات ري وسندات غير محدودة خلال الشهر</span>
              </li>
              <li>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
                <span>إرسال سندات القبض والتقارير عبر واتساب</span>
              </li>
            </ul>
          </div>
          <div style="display:flex; flex-direction:column; gap:8px;">
            <button onclick="openSubscriptionModal('MONTHLY')" class="btn-plan">طلب اشتراك وشراء مباشر ⚡</button>
            <a href="https://wa.me/<?= preg_replace('/[^0-9]/', '', $ps['contact_whatsapp']) ?>?text=<?= urlencode('السلام عليكم، أريد طلب تفعيل رخصة الاشتراك الشهري (' . $monthlyPrice . '$) لتطبيق مُسَرِّب') ?>" target="_blank" class="btn-plan-sub">أو تفعيل عبر واتساب 💬</a>
          </div>
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
        <div style="font-weight: 800; color: var(--text-heading);">
          منظومة مُسَرِّب (MOSARIB) &copy; <?= date('Y') ?>
        </div>
        <div style="color: var(--text-dim); font-weight: 700;">
          هندسة وبرمجة: <?= htmlspecialchars($ps['developer_name']) ?> • <?= htmlspecialchars($ps['developer_company']) ?>
        </div>
      </div>
      <div style="font-size: 11px; color: var(--text-dim);">
        جميع الحقوق محفوظة للمطور • نظام تشغيل وإدارة آبار المياه ودورات الري الزراعي
      </div>
    </div>
  </footer>

  <!-- Direct Subscription & Purchase Modal -->
  <div id="subscriptionModal" class="modal-overlay" style="display:none;" onclick="handleModalBackdropClick(event)">
    <div class="modal-card">
      <div class="modal-header">
        <div>
          <h3 class="modal-title">طلب تفعيل اشتراك جديد في تطبيق مُسَرِّب 💧</h3>
          <p class="modal-subtitle">اختر الباقة، ثم حوّل المبلغ إلى أحد حساباتنا المعتمدة أدناه وضع رقم السند لتفعيل جهازك فوراً.</p>
        </div>
        <button type="button" class="modal-close-btn" onclick="closeSubscriptionModal()">&times;</button>
      </div>

      <div class="modal-body">
        <!-- 1. Plan Selector -->
        <label class="modal-label">1. باقة الاشتراك المطلوبة:</label>
        <div class="plan-radio-group">
          <label class="plan-radio-card active" id="planCardYearly">
            <input type="radio" name="modalPlanChoice" value="YEARLY" checked onchange="updateModalPlanSelection()">
            <div class="plan-radio-content">
              <div class="plan-radio-title">الاشتراك السنوي الشامل ⭐</div>
              <div class="plan-radio-price">
                <b><?= htmlspecialchars($yearlyPrice) ?> دولار</b>
                <?php if (!empty($yearlyOrig)): ?>
                  <span class="old-price"><?= htmlspecialchars($yearlyOrig . $yearlyCurrency) ?></span>
                <?php endif; ?>
                <?php if (!empty($yearlyDisc)): ?>
                  (<?= htmlspecialchars($yearlyDisc) ?>)
                <?php endif; ?>
              </div>
              <div class="plan-radio-features">دعم فني • تحديثات فورية • نقل الاشتراك بأي وقت • عام كامل</div>
            </div>
          </label>
          <label class="plan-radio-card" id="planCardMonthly">
            <input type="radio" name="modalPlanChoice" value="MONTHLY" onchange="updateModalPlanSelection()">
            <div class="plan-radio-content">
              <div class="plan-radio-title">الاشتراك الشهري</div>
              <div class="plan-radio-price">
                <b><?= htmlspecialchars($monthlyPrice) ?> دولار</b> / شهرياً
                <?php if (!empty($monthlyOrig)): ?>
                  <span class="old-price"><?= htmlspecialchars($monthlyOrig . $monthlyCurrency) ?></span>
                <?php endif; ?>
                <?php if (!empty($monthlyDisc)): ?>
                  (<?= htmlspecialchars($monthlyDisc) ?>)
                <?php endif; ?>
              </div>
              <div class="plan-radio-features">دعم فني • تحديثات متواصلة • تجديد شهري مرن</div>
            </div>
          </label>
        </div>

        <!-- 2. Payment Accounts Display -->
        <label class="modal-label" style="margin-top: 14px;">
          <span>2. حسابات ومحافظ التحويل المعتمدة:</span>
          <small style="color:var(--text-dim); font-weight:normal;">(انقر على الزر لنسخ رقم الحساب فوراً)</small>
        </label>
        <div class="payment-methods-grid" id="modalPaymentMethodsList">
          <?php foreach ($paymentMethods as $pm): ?>
            <?php if (!empty($pm['is_active'])): ?>
            <div class="payment-method-card">
              <div class="pm-header">
                <div class="pm-bank">
                  <?php if (!empty($pm['logo_url'])): ?>
                    <img src="<?= htmlspecialchars($pm['logo_url']) ?>" alt="logo" class="pm-logo" onerror="this.style.display='none'">
                  <?php endif; ?>
                  <b><?= htmlspecialchars($pm['bank_name']) ?></b>
                </div>
                <span class="pm-currency-badge"><?= htmlspecialchars($pm['account_currency']) ?></span>
              </div>
              <div class="pm-body">
                <div class="pm-number-row">
                  <span class="pm-number"><?= htmlspecialchars($pm['account_number']) ?></span>
                  <button type="button" class="btn-copy-acc" onclick="copyAccountNumber('<?= htmlspecialchars($pm['account_number']) ?>', this)">
                    <span>نسخ</span> 📋
                  </button>
                </div>
                <?php if (!empty($pm['account_holder'])): ?>
                <div class="pm-holder">باسم: <b><?= htmlspecialchars($pm['account_holder']) ?></b></div>
                <?php endif; ?>
                <?php if (!empty($pm['notes'])): ?>
                <div class="pm-notes"><?= htmlspecialchars($pm['notes']) ?></div>
                <?php endif; ?>
              </div>
            </div>
            <?php endif; ?>
          <?php endforeach; ?>
        </div>

        <!-- 3. Customer Info Form -->
        <form id="subscriptionOrderForm" onsubmit="handleDirectSubscriptionSubmit(event)" style="margin-top: 16px;">
          <label class="modal-label">3. بيانات المشترك وسند التحويل:</label>
          <div class="form-row-grid">
            <div class="form-field">
              <label class="field-label">اسم المشترك الرباعي *</label>
              <input type="text" id="subFullName" class="form-input" placeholder="مثال: يحيى صالح مسعد" required>
            </div>
            <div class="form-field">
              <label class="field-label">رقم الهاتف (واتساب) *</label>
              <input type="tel" id="subPhone" class="form-input" placeholder="مثال: 773712030" required>
            </div>
          </div>

          <div class="form-row-grid" style="margin-top: 10px;">
            <div class="form-field">
              <label class="field-label">المحافظة والمنطقة الزراعية *</label>
              <input type="text" id="subRegion" class="form-input" placeholder="مثال: مأرب - وادي عبيدة / صنعاء" required>
            </div>
            <div class="form-field">
              <label class="field-label">طريقة التحويل / المحفظة البنكية *</label>
              <select id="subPaymentMethod" class="form-input" style="cursor:pointer;" required>
                <?php foreach ($paymentMethods as $pm): ?>
                  <?php if (!empty($pm['is_active'])): ?>
                    <option value="<?= htmlspecialchars($pm['bank_name'] . ' (' . ($pm['account_currency'] ?? 'YER') . ')') ?>">
                      <?= htmlspecialchars($pm['bank_name']) ?> (<?= htmlspecialchars($pm['account_currency'] ?? 'YER') ?>)
                    </option>
                  <?php endif; ?>
                <?php endforeach; ?>
                <option value="حوالة صرافة / شبكة أخرى">حوالة عبر صراف آخر / شبكة صرافة</option>
              </select>
            </div>
          </div>

          <div class="form-row-grid" style="margin-top: 10px;">
            <div class="form-field">
              <label class="field-label">رقم سند التحويل / الحوالة / الإيداع *</label>
              <input type="text" id="subReceipt" class="form-input" placeholder="مثال: رقم إشعار كريمي أو جوالي أو رقم الإيداع" required>
            </div>
            <div class="form-field">
              <label class="field-label">كود جهازك الفريد (Device Token) <small style="color:var(--text-dim);">(اختياري)</small></label>
              <input type="text" id="subDeviceCode" class="form-input" placeholder="مثال: A1B2-C3D4-E5F6 (من شاشة التفعيل)">
            </div>
          </div>

          <div id="subFormFeedback" style="display:none; margin-top:12px; padding:12px; border-radius:10px; font-weight:bold; font-size:13.5px; text-align:center;"></div>

          <div class="modal-actions-row" style="margin-top: 16px;">
            <button type="submit" id="btnSubmitSubOrder" class="btn-modal-submit">
              <span>إرسال طلب التفعيل المباشر 🚀</span>
            </button>
            <button type="button" onclick="sendSubscriptionViaWhatsApp()" class="btn-modal-whatsapp">
              <span>طلب التفعيل عبر واتساب 💬</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>

  <script>
    // --- Theme Switcher Logic ---
    function initPortalTheme() {
      const saved = localStorage.getItem('mosarib_portal_theme') || 'light';
      applyPortalTheme(saved);
    }

    function applyPortalTheme(theme) {
      const icon = document.getElementById('themeToggleIcon');
      const label = document.getElementById('themeToggleLabel');
      if (theme === 'dark') {
        document.documentElement.setAttribute('data-theme', 'dark');
        if (icon) icon.textContent = '☀️';
        if (label) label.textContent = 'الوضع النهاري';
      } else {
        document.documentElement.setAttribute('data-theme', 'light');
        if (icon) icon.textContent = '🌙';
        if (label) label.textContent = 'الوضع الليلي';
      }
      try {
        localStorage.setItem('mosarib_portal_theme', theme);
      } catch (_) {}
    }

    function togglePortalTheme() {
      const current = document.documentElement.getAttribute('data-theme') || 'light';
      applyPortalTheme(current === 'dark' ? 'light' : 'dark');
    }

    initPortalTheme();

    // --- Subscription Modal Logic ---
    function openSubscriptionModal(plan = 'YEARLY') {
      const modal = document.getElementById('subscriptionModal');
      if (!modal) return;
      const radios = document.getElementsByName('modalPlanChoice');
      radios.forEach(r => {
        r.checked = (r.value === plan);
      });
      updateModalPlanSelection();
      modal.style.display = 'flex';
      document.body.style.overflow = 'hidden';
    }

    function closeSubscriptionModal() {
      const modal = document.getElementById('subscriptionModal');
      if (!modal) return;
      modal.style.display = 'none';
      document.body.style.overflow = 'auto';
    }

    function handleModalBackdropClick(e) {
      if (e.target && e.target.id === 'subscriptionModal') {
        closeSubscriptionModal();
      }
    }

    function updateModalPlanSelection() {
      const radios = document.getElementsByName('modalPlanChoice');
      let chosen = 'YEARLY';
      radios.forEach(r => { if (r.checked) chosen = r.value; });
      const cardY = document.getElementById('planCardYearly');
      const cardM = document.getElementById('planCardMonthly');
      if (cardY) cardY.classList.toggle('active', chosen === 'YEARLY');
      if (cardM) cardM.classList.toggle('active', chosen === 'MONTHLY');
    }

    function copyAccountNumber(text, btn) {
      if (!text) return;
      navigator.clipboard.writeText(text).then(() => {
        const originalText = btn.innerHTML;
        btn.innerHTML = '<span>تم النسخ!</span> ✅';
        btn.style.background = '#059669';
        setTimeout(() => {
          btn.innerHTML = originalText;
          btn.style.background = '';
        }, 2000);
      }).catch(() => {
        prompt('انسخ رقم الحساب يدوياً:', text);
      });
    }

    async function handleDirectSubscriptionSubmit(e) {
      e.preventDefault();
      const name = document.getElementById('subFullName').value.trim();
      const phone = document.getElementById('subPhone').value.trim();
      const region = document.getElementById('subRegion').value.trim();
      const paymentMethod = document.getElementById('subPaymentMethod') ? document.getElementById('subPaymentMethod').value.trim() : '';
      const receipt = document.getElementById('subReceipt').value.trim();
      const deviceCode = document.getElementById('subDeviceCode').value.trim();
      const radios = document.getElementsByName('modalPlanChoice');
      let plan = 'YEARLY';
      radios.forEach(r => { if (r.checked) plan = r.value; });

      const feedback = document.getElementById('subFormFeedback');
      const btn = document.getElementById('btnSubmitSubOrder');

      feedback.style.display = 'block';
      feedback.style.background = 'rgba(0, 131, 143, 0.12)';
      feedback.style.color = 'var(--primary)';
      feedback.style.border = '1px solid var(--surface-border)';
      feedback.innerText = 'جاري إرسال طلب التفعيل إلى الإدارة والتحقق... ⏳';
      btn.disabled = true;

      try {
        const res = await fetch('api/public_data.php', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            action: 'submit_subscription_request',
            subscriber_name: name,
            phone: phone,
            region: region,
            plan: plan,
            receipt_number: receipt,
            device_code: deviceCode,
            payment_method: paymentMethod
          })
        });

        const text = await res.text();
        let data = {};
        try { data = JSON.parse(text); } catch (_) {}

        if (data.success) {
          feedback.style.background = 'rgba(16, 185, 129, 0.15)';
          feedback.style.color = '#059669';
          feedback.style.border = '1px solid #10B981';
          feedback.innerHTML = `✅ ${data.message}<br><small style="font-weight:normal;">رقم المرجع: <b>${data.data ? data.data.request_id : ''}</b></small>`;
          document.getElementById('subscriptionOrderForm').reset();
        } else {
          feedback.style.background = 'rgba(239, 68, 68, 0.15)';
          feedback.style.color = '#DC2626';
          feedback.style.border = '1px solid #EF4444';
          feedback.innerText = 'خطأ: ' + (data.message || 'تعذر إرسال الطلب، يمكنك الضغط على زر واتساب بالأسفل.');
        }
      } catch (err) {
        feedback.style.background = 'rgba(239, 68, 68, 0.15)';
        feedback.style.color = '#DC2626';
        feedback.style.border = '1px solid #EF4444';
        feedback.innerText = 'تعذر الاتصال بالسيرفر. يرجى استخدام زر طلب التفعيل عبر واتساب أدناه.';
      } finally {
        btn.disabled = false;
      }
    }

    function sendSubscriptionViaWhatsApp() {
      const name = document.getElementById('subFullName').value.trim() || 'مشترك جديد';
      const phone = document.getElementById('subPhone').value.trim() || '';
      const region = document.getElementById('subRegion').value.trim() || '';
      const paymentMethod = document.getElementById('subPaymentMethod') ? document.getElementById('subPaymentMethod').value.trim() : '';
      const receipt = document.getElementById('subReceipt').value.trim() || '';
      const deviceCode = document.getElementById('subDeviceCode').value.trim() || 'لم يتم تحديده بعد';
      const radios = document.getElementsByName('modalPlanChoice');
      let plan = 'سنوي (<?= $yearlyPrice ?>$)';
      radios.forEach(r => { if (r.checked) plan = (r.value === 'YEARLY' ? 'سنوي (<?= $yearlyPrice ?>$)' : 'شهري (<?= $monthlyPrice ?>$)'); });

      const adminPhone = '<?= preg_replace('/[^0-9]/', '', $ps['contact_whatsapp']) ?>';
      const paymentLine = paymentMethod ? `• طريقة التحويل / المحفظة: ${paymentMethod}\n` : '';
      const msg = 
`السلام عليكم يا باشمهندس عمر،
أود تفعيل رخصة تطبيق مُسَرِّب:
• نوع الاشتراك المطلوب: ${plan}
• اسم المشترك: ${name}
• رقم الهاتف: ${phone}
• المحافظة والمنطقة: ${region}
${paymentLine}• رقم سند التحويل / الإيداع: ${receipt}
• كود جهازي: ${deviceCode}

يرجى مراجعة الإيداع وتوليد رمز التفعيل، شكراً لك.`;

      window.open(`https://wa.me/${adminPhone}?text=${encodeURIComponent(msg)}`, '_blank');
    }

    function trackDownloadMetric() {
      try {
        fetch('api/public_data.php?action=track_download', { method: 'POST' }).catch(() => {});
      } catch (_) {}
    }
  </script>
</body>
</html>
