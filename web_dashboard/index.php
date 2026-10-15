<?php
/**
 * Mosarib Public Portal - Real Visitor Analytics
 */
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
?>
<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>مُسَرِّب • المنظومة الرائدة لإدارة مياه الآبار ودورات الري الزراعي</title>
  <meta name="description" content="البوابة الرسمية لمنظومة مُسَرِّب: نبض سوق المياه، أسعار ساعات الري اللحظية، ومؤشرات وفرة المياه الجوفية والتحميل المباشر لتطبيق المزارعين وملاك الآبار.">
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Cairo:wght@400;500;600;700;800;900&display=swap" rel="stylesheet">
  <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js"></script>
  
  <style>
    :root {
      --primary: #00838F;
      --primary-dark: #004D40;
      --primary-light: #26A69A;
      --accent: #00B4D8;
      --gold: #F59E0B;
      --gold-soft: rgba(245, 158, 11, 0.15);
      --bg: #071015;
      --surface: #0E1D24;
      --surface-elevated: #132731;
      --surface-card: rgba(14, 29, 36, 0.7);
      --border: rgba(0, 180, 216, 0.16);
      --border-subtle: rgba(255, 255, 255, 0.08);
      --text: #E0F2F1;
      --text-muted: #80CBC4;
      --text-dim: #546E7A;
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
        radial-gradient(circle at 10% 10%, rgba(0, 131, 143, 0.18) 0%, transparent 40%),
        radial-gradient(circle at 90% 20%, rgba(0, 180, 216, 0.12) 0%, transparent 45%),
        radial-gradient(circle at 50% 90%, rgba(38, 166, 154, 0.08) 0%, transparent 50%);
      background-attachment: fixed;
      color: var(--text);
      line-height: 1.7;
      overflow-x: hidden;
      -webkit-font-smoothing: antialiased;
    }

    /* Top Live Alert Ticker */
    .top-ticker {
      background: linear-gradient(90deg, #00332C, #004D40, #00332C);
      color: #E0F7FA;
      font-size: 12.5px;
      padding: 7px 16px;
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
      50% { transform: scale(1.25); opacity: 1; }
      100% { transform: scale(0.9); opacity: 0.8; }
    }

    /* Header */
    header.main-header {
      position: sticky;
      top: 0;
      z-index: 1000;
      background: rgba(7, 16, 21, 0.88);
      backdrop-filter: blur(16px);
      -webkit-backdrop-filter: blur(16px);
      border-bottom: 1px solid var(--border-subtle);
      padding: 12px 24px;
    }
    .header-container {
      max-width: 1200px;
      margin: 0 auto;
      display: flex;
      align-items: center;
      justify-content: space-between;
    }
    .brand-logo {
      display: flex;
      align-items: center;
      gap: 12px;
      text-decoration: none;
      color: #FFF;
    }
    .logo-badge {
      width: 42px;
      height: 42px;
      background: linear-gradient(135deg, #00838F, #004D40);
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      border: 1px solid rgba(255, 255, 255, 0.15);
      color: #80DEEA;
      flex-shrink: 0;
    }
    .brand-title {
      font-size: 19px;
      font-weight: 900;
      letter-spacing: -0.3px;
      color: #FFFFFF;
    }
    .brand-sub {
      font-size: 11px;
      color: var(--text-muted);
      font-weight: 500;
    }

    .nav-links {
      display: flex;
      align-items: center;
      gap: 24px;
    }
    .nav-link {
      color: #B2DFDB;
      text-decoration: none;
      font-size: 13.5px;
      font-weight: 600;
      transition: color 0.2s ease;
    }
    .nav-link:hover {
      color: #FFFFFF;
    }

    .btn-nav-download {
      background: linear-gradient(135deg, #00897B, #004D40);
      border: 1px solid #4DB6AC;
      color: #FFFFFF;
      padding: 8px 18px;
      border-radius: var(--radius-full);
      font-size: 13px;
      font-weight: 700;
      text-decoration: none;
      display: inline-flex;
      align-items: center;
      gap: 8px;
      transition: all 0.2s ease;
    }
    .btn-nav-download:hover {
      background: linear-gradient(135deg, #009688, #00695C);
      transform: translateY(-1px);
    }

    /* Container */
    .container {
      max-width: 1200px;
      margin: 0 auto;
      padding: 0 20px;
    }

    /* Hero Section */
    .hero {
      padding: 60px 0 40px 0;
      display: grid;
      grid-template-columns: 1.15fr 0.85fr;
      gap: 40px;
      align-items: center;
    }
    .hero-badge {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      background: rgba(0, 180, 216, 0.1);
      border: 1px solid rgba(0, 180, 216, 0.25);
      color: #80DEEA;
      padding: 5px 14px;
      border-radius: var(--radius-full);
      font-size: 12px;
      font-weight: 700;
      margin-bottom: 18px;
    }
    .hero-title {
      font-size: 38px;
      font-weight: 900;
      line-height: 1.3;
      color: #FFFFFF;
      margin-bottom: 18px;
      letter-spacing: -0.5px;
    }
    .hero-title span {
      color: #4DD0E1;
    }
    .hero-description {
      font-size: 15.5px;
      color: #B2DFDB;
      line-height: 1.85;
      margin-bottom: 30px;
      max-width: 560px;
    }
    .hero-actions {
      display: flex;
      flex-wrap: wrap;
      gap: 14px;
      align-items: center;
      margin-bottom: 24px;
    }
    .btn-primary-action {
      background: linear-gradient(135deg, #F59E0B, #D97706);
      color: #05141A;
      font-size: 15px;
      font-weight: 800;
      padding: 13px 26px;
      border-radius: var(--radius-md);
      text-decoration: none;
      display: inline-flex;
      align-items: center;
      gap: 10px;
      transition: all 0.25s ease;
      box-shadow: 0 4px 16px rgba(245, 158, 11, 0.25);
    }
    .btn-primary-action:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 20px rgba(245, 158, 11, 0.4);
      background: linear-gradient(135deg, #FBBF24, #D97706);
    }
    .btn-secondary-action {
      background: rgba(255, 255, 255, 0.04);
      border: 1px solid var(--border);
      color: #E0F2F1;
      font-size: 14.5px;
      font-weight: 600;
      padding: 12px 22px;
      border-radius: var(--radius-md);
      text-decoration: none;
      display: inline-flex;
      align-items: center;
      gap: 8px;
      transition: all 0.2s ease;
    }
    .btn-secondary-action:hover {
      background: rgba(0, 180, 216, 0.1);
      border-color: rgba(0, 180, 216, 0.4);
      color: #FFFFFF;
    }
    .hero-trust {
      display: flex;
      align-items: center;
      gap: 20px;
      font-size: 12px;
      color: var(--text-muted);
    }
    .trust-item {
      display: flex;
      align-items: center;
      gap: 6px;
    }

    /* Live Water & Metrics Visual Card */
    .hero-visual-card {
      background: linear-gradient(160deg, rgba(19, 39, 49, 0.75), rgba(9, 18, 23, 0.9));
      border: 1px solid var(--border);
      border-radius: var(--radius-lg);
      padding: 26px;
      backdrop-filter: blur(12px);
      box-shadow: 0 16px 40px rgba(0, 0, 0, 0.4);
    }
    .card-header-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;
      padding-bottom: 12px;
      border-bottom: 1px solid var(--border-subtle);
    }
    .card-title-group {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 14.5px;
      font-weight: 700;
      color: #FFFFFF;
    }
    .status-badge {
      font-size: 11.5px;
      font-weight: 700;
      padding: 3px 10px;
      border-radius: var(--radius-full);
      background: rgba(0, 200, 83, 0.12);
      border: 1px solid rgba(0, 200, 83, 0.3);
      color: #69F0AE;
    }

    /* Circular Water Gauge */
    .gauge-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      margin-bottom: 22px;
    }
    .gauge-wrapper {
      position: relative;
      width: 180px;
      height: 95px;
      overflow: hidden;
    }
    .gauge-semicircle {
      width: 180px;
      height: 180px;
      border-radius: 50%;
      border: 14px solid rgba(255, 255, 255, 0.06);
      border-top-color: #00B4D8;
      border-right-color: #26A69A;
      border-left-color: #F59E0B;
      transform: rotate(45deg);
      box-sizing: border-box;
    }
    .gauge-center-text {
      position: absolute;
      bottom: 0;
      left: 50%;
      transform: translateX(-50%);
      text-align: center;
    }
    .gauge-percent {
      font-size: 30px;
      font-weight: 900;
      color: #FFFFFF;
      line-height: 1;
    }
    .gauge-sub {
      font-size: 11px;
      color: var(--text-muted);
      font-weight: 600;
      margin-top: 2px;
    }
    .gauge-status-desc {
      text-align: center;
      font-size: 13px;
      color: #B2DFDB;
      margin-top: 10px;
    }

    /* Key Live Indicators Grid */
    .live-indicators {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 12px;
      margin-top: 16px;
    }
    .indicator-box {
      background: rgba(255, 255, 255, 0.03);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-md);
      padding: 12px 14px;
    }
    .indicator-label {
      font-size: 11.5px;
      color: var(--text-muted);
      margin-bottom: 4px;
      display: flex;
      align-items: center;
      gap: 6px;
    }
    .indicator-val {
      font-size: 17px;
      font-weight: 800;
      color: #FFFFFF;
    }
    .indicator-val small {
      font-size: 11.5px;
      color: var(--gold);
      font-weight: 600;
      margin-right: 4px;
    }

    /* Section Headings */
    .section-title-wrap {
      text-align: center;
      margin: 70px 0 35px 0;
    }
    .section-badge {
      display: inline-block;
      background: rgba(0, 180, 216, 0.1);
      border: 1px solid rgba(0, 180, 216, 0.25);
      color: #80DEEA;
      padding: 4px 14px;
      border-radius: var(--radius-full);
      font-size: 11.5px;
      font-weight: 700;
      margin-bottom: 10px;
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

    /* Market Pulse Cards */
    .market-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
      gap: 18px;
      margin-bottom: 35px;
    }
    .market-card {
      background: var(--surface-card);
      backdrop-filter: blur(12px);
      border: 1px solid var(--border);
      border-radius: var(--radius-md);
      padding: 22px;
      transition: all 0.2s ease;
    }
    .market-card:hover {
      border-color: rgba(0, 180, 216, 0.4);
      transform: translateY(-2px);
    }
    .market-card-top {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;
    }
    .market-card-icon {
      width: 36px;
      height: 36px;
      border-radius: 10px;
      background: rgba(0, 180, 216, 0.12);
      border: 1px solid rgba(0, 180, 216, 0.25);
      display: flex;
      align-items: center;
      justify-content: center;
      color: #80DEEA;
    }
    .market-tag {
      font-size: 11.5px;
      color: var(--text-muted);
      font-weight: 600;
    }
    .market-label {
      font-size: 12.5px;
      color: var(--text-muted);
      margin-bottom: 4px;
    }
    .market-price {
      font-size: 24px;
      font-weight: 900;
      color: #FFFFFF;
      margin-bottom: 8px;
    }
    .market-price span {
      font-size: 13px;
      color: var(--gold);
      font-weight: 700;
    }
    .market-note {
      font-size: 12px;
      color: #B2DFDB;
      line-height: 1.6;
    }

    /* Chart Section */
    .chart-box {
      background: var(--surface-card);
      border: 1px solid var(--border);
      border-radius: var(--radius-lg);
      padding: 24px;
      margin-bottom: 60px;
    }
    .chart-box-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 18px;
      flex-wrap: wrap;
      gap: 10px;
    }
    .chart-box-title {
      font-size: 16px;
      font-weight: 800;
      color: #FFFFFF;
    }
    .chart-box-sub {
      font-size: 12px;
      color: var(--text-muted);
    }

    /* News Grid */
    .news-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
      gap: 20px;
      margin-bottom: 60px;
    }
    .news-card {
      background: linear-gradient(160deg, rgba(19, 39, 49, 0.6), rgba(9, 18, 23, 0.8));
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-md);
      padding: 22px;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      transition: all 0.2s ease;
    }
    .news-card:hover {
      border-color: rgba(0, 180, 216, 0.35);
      transform: translateY(-2px);
    }
    .news-meta {
      display: flex;
      align-items: center;
      justify-content: space-between;
      font-size: 11.5px;
      color: var(--text-muted);
      margin-bottom: 12px;
    }
    .news-category-badge {
      background: rgba(0, 137, 123, 0.2);
      color: #4DB6AC;
      border: 1px solid rgba(0, 137, 123, 0.3);
      padding: 2px 10px;
      border-radius: var(--radius-full);
      font-weight: 700;
      font-size: 11px;
    }
    .news-title {
      font-size: 16px;
      font-weight: 800;
      color: #FFFFFF;
      line-height: 1.5;
      margin-bottom: 8px;
    }
    .news-summary {
      font-size: 13px;
      color: #B2DFDB;
      line-height: 1.7;
      margin-bottom: 16px;
    }
    .news-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      border-top: 1px solid var(--border-subtle);
      padding-top: 10px;
      font-size: 11.5px;
      color: var(--text-muted);
    }

    /* Developer Section */
    .developer-section {
      background: linear-gradient(150deg, rgba(14, 29, 36, 0.85), rgba(7, 16, 21, 0.95));
      border: 1px solid var(--border);
      border-radius: var(--radius-lg);
      padding: 30px;
      margin-bottom: 50px;
    }
    .dev-grid {
      display: grid;
      grid-template-columns: auto 1fr auto;
      gap: 24px;
      align-items: center;
    }
    .dev-avatar {
      width: 68px;
      height: 68px;
      border-radius: 16px;
      background: linear-gradient(135deg, #00838F, #004D40);
      border: 1px solid rgba(0, 180, 216, 0.4);
      display: flex;
      align-items: center;
      justify-content: center;
      color: #80DEEA;
      flex-shrink: 0;
    }
    .dev-name-row {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 4px;
      flex-wrap: wrap;
    }
    .dev-name {
      font-size: 20px;
      font-weight: 900;
      color: #FFFFFF;
    }
    .dev-role-badge {
      background: rgba(245, 158, 11, 0.12);
      border: 1px solid rgba(245, 158, 11, 0.3);
      color: var(--gold);
      padding: 2px 10px;
      border-radius: var(--radius-full);
      font-size: 11px;
      font-weight: 700;
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
      line-height: 1.7;
      max-width: 600px;
    }
    .dev-actions {
      display: flex;
      flex-direction: column;
      gap: 10px;
      min-width: 200px;
    }
    .dev-link {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      padding: 9px 14px;
      border-radius: var(--radius-md);
      font-size: 12.5px;
      font-weight: 700;
      text-decoration: none;
      transition: all 0.2s ease;
    }
    .dev-link-primary {
      background: rgba(0, 180, 216, 0.1);
      border: 1px solid rgba(0, 180, 216, 0.3);
      color: #80DEEA;
    }
    .dev-link-primary:hover {
      background: rgba(0, 180, 216, 0.2);
    }
    .dev-link-secondary {
      background: rgba(255, 255, 255, 0.04);
      border: 1px solid var(--border-subtle);
      color: #FFFFFF;
    }
    .dev-link-secondary:hover {
      background: rgba(255, 255, 255, 0.08);
    }

    /* Download Call to Action Box */
    .cta-download-box {
      background: linear-gradient(135deg, rgba(0, 77, 64, 0.8), rgba(0, 105, 115, 0.8));
      border: 1px solid rgba(0, 180, 216, 0.35);
      border-radius: var(--radius-lg);
      padding: 38px 24px;
      text-align: center;
      margin: 40px 0 70px 0;
    }
    .cta-title {
      font-size: 24px;
      font-weight: 900;
      color: #FFFFFF;
      margin-bottom: 10px;
    }
    .cta-desc {
      font-size: 14.5px;
      color: #E0F2F1;
      max-width: 620px;
      margin: 0 auto 22px auto;
    }
    .cta-specs {
      display: flex;
      justify-content: center;
      align-items: center;
      gap: 20px;
      font-size: 12.5px;
      color: #B2DFDB;
      flex-wrap: wrap;
    }

    /* Footer */
    footer.public-footer {
      border-top: 1px solid var(--border-subtle);
      background: #040A0D;
      padding: 30px 0 20px 0;
      font-size: 12.5px;
      color: var(--text-muted);
    }
    .footer-content {
      display: flex;
      flex-wrap: wrap;
      justify-content: space-between;
      align-items: center;
      gap: 18px;
      margin-bottom: 18px;
    }

    /* Responsive */
    @media (max-width: 880px) {
      .hero {
        grid-template-columns: 1fr;
        padding: 35px 0;
      }
      .hero-title {
        font-size: 30px;
      }
      .nav-links {
        display: none;
      }
      .dev-grid {
        grid-template-columns: 1fr;
        text-align: center;
      }
      .dev-avatar {
        margin: 0 auto;
      }
      .dev-name-row {
        justify-content: center;
      }
      .dev-desc {
        margin: 0 auto;
      }
      .dev-actions {
        margin-top: 10px;
      }
    }
  </style>
</head>
<body>

  <!-- Top Alert Ticker -->
  <div class="top-ticker">
    <div class="pulse-dot"></div>
    <span id="topAlertMessage">مؤشر ري مباشر: متابعة فورية لمنسوب مياه الآبار الجوفية ومعدلات الضخ الزراعي.</span>
  </div>

  <!-- Main Header (Strictly public, NO admin link) -->
  <header class="main-header">
    <div class="header-container">
      <a href="index.php" class="brand-logo">
        <div class="logo-badge">
          <!-- Sleek Water Droplet SVG -->
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"></path>
          </svg>
        </div>
        <div>
          <div class="brand-title">مُسَرِّب • MOSARIB</div>
          <div class="brand-sub">المنظومة الذكية لإدارة مياه الآبار وحسابات الري</div>
        </div>
      </a>

      <nav class="nav-links">
        <a href="#water-meter" class="nav-link">مؤشر وفرة المياه</a>
        <a href="#market-pulse" class="nav-link">أسعار ساعة الري</a>
        <a href="#portal-news" class="nav-link">تقارير وإرشادات</a>
        <a href="#developer-section" class="nav-link">عن المنظومة</a>
      </nav>

      <div>
        <a href="#download-section" class="btn-nav-download">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
            <polyline points="7 10 12 15 17 10"></polyline>
            <line x1="12" y1="15" x2="12" y2="3"></line>
          </svg>
          <span>تحميل التطبيق</span>
        </a>
      </div>
    </div>
  </header>

  <main class="container">

    <!-- Hero Section -->
    <section class="hero">
      <div>
        <div class="hero-badge">
          <span>المنظومة الرقمية الموثوقة لملاك الآبار والمزارعين</span>
        </div>
        <h1 class="hero-title">
          نبض مياه الآبار <span>وساعات الري الزراعي</span> بدقة تقنية ومالية تامة
        </h1>
        <p class="hero-description">
          تطبيق "مُسَرِّب" يربط شبكة الآبار ومسربي المياه وملاك الأراضي الزراعية في منظومة متكاملة: حساب كسور ساعات الري بدقة، تتبع الفاقد المائي، إصدار سندات قبض وحسابات آلية، ومتابعة فورية لمؤشرات سوق المياه.
        </p>

        <div class="hero-actions">
          <a href="#" id="heroDirectDownloadBtn" class="btn-primary-action">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
              <polyline points="7 10 12 15 17 10"></polyline>
              <line x1="12" y1="15" x2="12" y2="3"></line>
            </svg>
            <span>تحميل التطبيق المباشر (APK)</span>
          </a>
          <a href="#water-meter" class="btn-secondary-action">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="18" y1="20" x2="18" y2="10"></line>
              <line x1="12" y1="20" x2="12" y2="4"></line>
              <line x1="6" y1="20" x2="6" y2="14"></line>
            </svg>
            <span>مؤشر المياه والأسعار</span>
          </a>
        </div>

        <div class="hero-trust">
          <div class="trust-item">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#26A69A" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="20 6 9 17 4 12"></polyline>
            </svg>
            <span>تحديث مباشر</span>
          </div>
          <div class="trust-item">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#26A69A" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
            </svg>
            <span>بيانات حقيقية موثوقة</span>
          </div>
          <div class="trust-item">
            <span id="heroReleaseVer">الإصدار: v1.4.0</span>
          </div>
        </div>
      </div>

      <!-- Live Water & Market Visual Card -->
      <div class="hero-visual-card" id="water-meter">
        <div class="card-header-bar">
          <div class="card-title-group">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#00B4D8" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"></path>
            </svg>
            <span>مؤشر وفرة المياه الميداني</span>
          </div>
          <span class="status-badge" id="badgeWaterStatus">مستقر ومتاح</span>
        </div>

        <div class="gauge-container">
          <div class="gauge-wrapper">
            <div class="gauge-semicircle" id="gaugeCircle"></div>
            <div class="gauge-center-text">
              <div class="gauge-percent" id="waterIndexPercent">85%</div>
              <div class="gauge-sub">معدل الوفرة والتدفق</div>
            </div>
          </div>
          <div class="gauge-status-desc" id="waterStatusTextDesc">
            تدفق المياه الجوفية بحالة مستقرة في معظم المناطق الزراعية
          </div>
        </div>

        <!-- Real-time key metrics -->
        <div class="live-indicators">
          <div class="indicator-box">
            <div class="indicator-label">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                <circle cx="9" cy="7" r="4"></circle>
                <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
              </svg>
              <span>المسربون النشطون</span>
            </div>
            <div class="indicator-val" id="activeOperatorsNow">-- <small>مسرب متصل</small></div>
          </div>

          <div class="indicator-box">
            <div class="indicator-label">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="2" y="4" width="20" height="16" rx="2"></rect>
                <line x1="12" y1="8" x2="12" y2="16"></line>
                <line x1="8" y1="12" x2="16" y2="12"></line>
              </svg>
              <span>إجمالي المشتركين</span>
            </div>
            <div class="indicator-val" id="totalRegisteredOperators">-- <small>مشترك مسجل</small></div>
          </div>

          <div class="indicator-box">
            <div class="indicator-label">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="12" y1="1" x2="12" y2="23"></line>
                <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"></path>
              </svg>
              <span>متوسط سعر الساعة</span>
            </div>
            <div class="indicator-val" id="avgHourlyPrice">-- <small id="currencyUnit">ريال</small></div>
          </div>

          <div class="indicator-box">
            <div class="indicator-label">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"></circle>
                <polyline points="12 6 12 12 16 14"></polyline>
              </svg>
              <span>حالة التحديث</span>
            </div>
            <div class="indicator-val" id="syncStatusText" style="font-size: 14.5px; color: #4DD0E1;">لحظي مباشر</div>
          </div>
        </div>
      </div>
    </section>

    <!-- Market Pulse Section -->
    <div class="section-title-wrap" id="market-pulse">
      <div class="section-badge">مؤشرات السوق الزراعي</div>
      <h2 class="section-title">بورصة وأسعار ساعات الري</h2>
      <p class="section-desc">إحصائيات حية مستمدة من واقع دورات وساعات الري المسجلة في تطبيق مُسَرِّب من قبل المسربين المعتمدين.</p>
    </div>

    <div class="market-grid">
      <div class="market-card">
        <div class="market-card-top">
          <div class="market-card-icon">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="18" y1="20" x2="18" y2="10"></line>
              <line x1="12" y1="20" x2="12" y2="4"></line>
              <line x1="6" y1="20" x2="6" y2="14"></line>
            </svg>
          </div>
          <span class="market-tag">السعر التوازني</span>
        </div>
        <div class="market-label">متوسط سعر الساعة العام</div>
        <div class="market-price" id="boxAvgPrice">-- <span>ريال/ساعة</span></div>
        <p class="market-note" id="boxAvgDesc">يمثل متوسط أسعار ساعات الري المسجلة فعلياً لدى المسربين النشطين في المنظومة.</p>
      </div>

      <div class="market-card">
        <div class="market-card-top">
          <div class="market-card-icon">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="23 18 13.5 8.5 8.5 13.5 1 6"></polyline>
              <polyline points="17 18 23 18 23 12"></polyline>
            </svg>
          </div>
          <span class="market-tag">الأدنى تسجيلاً</span>
        </div>
        <div class="market-label">أقل سعر ساعة معتمد</div>
        <div class="market-price" id="boxMinPrice">-- <span>ريال/ساعة</span></div>
        <p class="market-note">يسجل غالباً في الآبار ذات التدفق العالي والمنظومات التي تعمل بالطاقة الشمسية منخفضة الكلفة.</p>
      </div>

      <div class="market-card">
        <div class="market-card-top">
          <div class="market-card-icon">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"></polyline>
              <polyline points="17 6 23 6 23 12"></polyline>
            </svg>
          </div>
          <span class="market-tag">الأعلى طلباً</span>
        </div>
        <div class="market-label">أعلى سعر ساعة مسجل</div>
        <div class="market-price" id="boxMaxPrice">-- <span>ريال/ساعة</span></div>
        <p class="market-note">في فترات ذروة ري المحاصيل الحساسة أو الآبار العميقة التي تعتمد على محروقات الديزل في فترات الجفاف.</p>
      </div>
    </div>

    <!-- Chart: Price Stability -->
    <div class="chart-box">
      <div class="chart-box-header">
        <div>
          <h3 class="chart-box-title">حركة واستقرار مؤشر أسعار ساعات الري</h3>
          <p class="chart-box-sub">رصد منحنى الأسعار وفق تعاقب مواسم الأمطار وفترات الجفاف الزراعي</p>
        </div>
        <span class="hero-badge" style="margin: 0;">بيانات تحليلية دورية</span>
      </div>
      <div style="height: 250px;">
        <canvas id="marketTrendChart"></canvas>
      </div>
    </div>

    <!-- News & Guidance Section -->
    <div class="section-title-wrap" id="portal-news">
      <div class="section-badge">الأخبار والإرشادات</div>
      <h2 class="section-title">تقارير مياه الآبار والري الزراعي</h2>
      <p class="section-desc">معلومات وتوجيهات فنية للمسربين وملاك الآبار لتحقيق أعلى كفاءة وضبط الفاقد المائي.</p>
    </div>

    <div class="news-grid" id="newsCardsContainer">
      <div style="text-align: center; color: var(--text-muted); padding: 30px; grid-column: 1 / -1;">
        جاري تحميل التقارير والأخبار...
      </div>
    </div>

    <!-- Developer & Founder Official Card -->
    <section class="developer-section" id="developer-section">
      <div class="dev-grid">
        <div class="dev-avatar">
          <!-- Code/Brackets SVG -->
          <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="16 18 22 12 16 6"></polyline>
            <polyline points="8 6 2 12 8 18"></polyline>
          </svg>
        </div>

        <div>
          <div class="dev-name-row">
            <h3 class="dev-name" id="devName">المهندس عمر الحومي</h3>
            <span class="dev-role-badge">صاحب المشروع والمطور</span>
          </div>
          <div class="dev-company" id="devCompany">
            تُبع سوفت للحلول البرمجية الذكية (TubbaSoft)
          </div>
          <p class="dev-desc" id="devSupportMsg">
            تم ابتكار وتطوير منظومة وتطبيق مُسَرِّب بهدف حوكمة إدارة مياه الآبار الزراعية وحفظ حقوق المسربين والمزارعين بأعلى دقة تقنية ومالية.
          </p>
        </div>

        <div class="dev-actions">
          <a href="mailto:omar@tubbasoft.com" id="devEmailLink" class="dev-link dev-link-primary">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path>
              <polyline points="22,6 12,13 2,6"></polyline>
            </svg>
            <span id="devEmailText">omar@tubbasoft.com</span>
          </a>
          <a href="https://tubbasoft.com" target="_blank" rel="noopener noreferrer" class="dev-link dev-link-secondary">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"></circle>
              <line x1="2" y1="12" x2="22" y2="12"></line>
              <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"></path>
            </svg>
            <span>زيارة موقع تُبع سوفت</span>
          </a>
        </div>
      </div>
    </section>

    <!-- Download Call to Action Box -->
    <section class="cta-download-box" id="download-section">
      <h2 class="cta-title">حمّل تطبيق مُسَرِّب وابدأ إدارة آبارك بدقة واحترافية</h2>
      <p class="cta-desc">
        احصل على النسخة الرسمية الأحدث مباشرة بصيغة APK دون وسيط، مع تجربة مجانية تشمل 200 عملية ري وسندات مالية وتقارير PDF كاملة.
      </p>

      <div style="display: flex; justify-content: center; gap: 14px; flex-wrap: wrap; margin-bottom: 20px;">
        <a href="#" id="mainDownloadApkBtn" class="btn-primary-action" style="font-size: 16px; padding: 14px 34px;">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
            <polyline points="7 10 12 15 17 10"></polyline>
            <line x1="12" y1="15" x2="12" y2="3"></line>
          </svg>
          <span>تحميل تطبيق مُسَرِّب (APK مباشر)</span>
        </a>
      </div>

      <div class="cta-specs">
        <span id="footerVersionText">الإصدار: v1.4.0</span>
        <span id="footerSizeText">الحجم: 2.78 MB</span>
        <span style="display: inline-flex; align-items: center; gap: 5px;">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#26A69A" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
          </svg>
          فحص أمني معتمد وخالٍ من الفيروسات
        </span>
      </div>
    </section>

  </main>

  <!-- Public Footer (Strictly public, NO admin link) -->
  <footer class="public-footer">
    <div class="container">
      <div class="footer-content">
        <div style="display: flex; align-items: center; gap: 10px;">
          <div class="logo-badge" style="width: 34px; height: 34px;">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"></path>
            </svg>
          </div>
          <div>
            <b style="color: #FFF; font-size: 14px;">مُسَرِّب (MOSARIB)</b>
            <div style="font-size: 11px; color: var(--text-muted);">هندسة وبرمجة: المهندس عمر الحومي &copy; 2026</div>
          </div>
        </div>

        <div style="display: flex; gap: 20px;">
          <a href="#water-meter" class="nav-link" style="font-size: 12.5px;">مؤشر المياه</a>
          <a href="#market-pulse" class="nav-link" style="font-size: 12.5px;">أسعار الري</a>
          <a href="#portal-news" class="nav-link" style="font-size: 12.5px;">التقارير</a>
          <a href="#download-section" class="nav-link" style="font-size: 12.5px;">تحميل APK</a>
        </div>
      </div>

      <div style="text-align: center; font-size: 11.5px; color: rgba(255,255,255,0.35); border-top: 1px solid var(--border-subtle); padding-top: 16px;">
        جميع الحقوق محفوظة &copy; 2026 م. عمر الحومي • شركة تُبع سوفت للحلول الذكية (TubbaSoft)
      </div>
    </div>
  </footer>

  <script>
    let globalDownloadUrl = "uploads/Mosarib_Latest.apk";

    async function loadPublicData() {
      try {
        const res = await fetch('api/public_data.php');
        const json = await res.json();
        if (json.success && json.data) {
          const d = json.data;
          renderMetrics(d.water_metrics);
          renderNews(d.news);
          renderRelease(d.app_release);
          renderDeveloper(d.developer_info);
        }
      } catch (err) {
        console.error("Failed to load public data:", err);
      }
    }

    function renderDeveloper(dev) {
      if (!dev) return;
      const nameEl = document.getElementById("devName");
      const compEl = document.getElementById("devCompany");
      const emailText = document.getElementById("devEmailText");
      const emailLink = document.getElementById("devEmailLink");
      const msgEl = document.getElementById("devSupportMsg");

      if (nameEl) nameEl.innerText = dev.name || "المهندس عمر الحومي";
      if (compEl) compEl.innerText = dev.company || "تُبع سوفت للحلول البرمجية الذكية (TubbaSoft)";
      if (emailText) emailText.innerText = dev.email || "omar@tubbasoft.com";
      if (emailLink) emailLink.href = `mailto:${dev.email || 'omar@tubbasoft.com'}`;
      if (msgEl && dev.support_message) msgEl.innerText = dev.support_message;
    }

    function renderMetrics(m) {
      if (!m) return;
      if (m.headline_alert) {
        document.getElementById("topAlertMessage").innerText = m.headline_alert;
      }
      
      const p = m.index_percent || 85;
      document.getElementById("waterIndexPercent").innerText = p + "%";
      if (m.status_text) {
        document.getElementById("waterStatusTextDesc").innerText = m.status_text;
      }
      document.getElementById("badgeWaterStatus").innerText = (m.status === 'ABUNDANT' ? 'وفيرة ومتاحة' : (m.status === 'GOOD' ? 'مستقر ومتاح' : 'متوسط التدفق'));

      // Real subscriber & operator numbers
      document.getElementById("activeOperatorsNow").innerHTML = `${m.active_operators_now || 0} <small>مسرب متصل</small>`;
      document.getElementById("totalRegisteredOperators").innerHTML = `${m.total_operators_registered || 0} <small>مشترك مسجل</small>`;

      // Real Price Metrics
      const currency = m.currency_symbol || 'ريال';
      document.getElementById("currencyUnit").innerText = currency;

      if (m.is_live_synced && m.avg_hourly_price > 0) {
        const avgFmt = Number(m.avg_hourly_price).toLocaleString('ar-EG');
        const minFmt = Number(m.min_hourly_price).toLocaleString('ar-EG');
        const maxFmt = Number(m.max_hourly_price).toLocaleString('ar-EG');

        document.getElementById("avgHourlyPrice").innerHTML = `${avgFmt} <small>${currency}</small>`;
        document.getElementById("boxAvgPrice").innerHTML = `${avgFmt} <span>${currency}/ساعة</span>`;
        document.getElementById("boxMinPrice").innerHTML = `${minFmt} <span>${currency}/ساعة</span>`;
        document.getElementById("boxMaxPrice").innerHTML = `${maxFmt} <span>${currency}/ساعة</span>`;
        document.getElementById("boxAvgDesc").innerText = `محسوب لحظياً ومباشرة من واقع تسعيرات ${m.samples_count} مسرب فعّال مشترك بالنسخة الكاملة.`;
        document.getElementById("syncStatusText").innerText = `مباشر (${m.samples_count} مسرب)`;
      } else {
        // No live prices published yet: display authentic placeholder without inventing fake numbers
        document.getElementById("avgHourlyPrice").innerHTML = `-- <small>${currency}</small>`;
        document.getElementById("boxAvgPrice").innerHTML = `-- <span>${currency}/ساعة</span>`;
        document.getElementById("boxMinPrice").innerHTML = `-- <span>${currency}/ساعة</span>`;
        document.getElementById("boxMaxPrice").innerHTML = `-- <span>${currency}/ساعة</span>`;
        document.getElementById("boxAvgDesc").innerText = "يتم احتساب المتوسط التلقائي بمجرد تسجيل المشتركين النشطين لأسعارهم في التطبيق.";
        document.getElementById("syncStatusText").innerText = "قيد الجمع والتحليل";
      }
    }

    function renderNews(news) {
      const container = document.getElementById("newsCardsContainer");
      if (!container) return;
      if (!news || news.length === 0) {
        container.innerHTML = `<div style="text-align: center; color: var(--text-muted); padding: 30px; grid-column: 1 / -1;">لا توجد تقارير منشورة حالياً.</div>`;
        return;
      }

      container.innerHTML = news.map(item => {
        const dateStr = item.published_at ? new Date(item.published_at).toLocaleDateString('ar-EG', { year: 'numeric', month: 'long', day: 'numeric' }) : '';
        return `
          <article class="news-card">
            <div>
              <div class="news-meta">
                <span class="news-category-badge">${item.category || 'تقارير المياه'}</span>
                <span>${dateStr}</span>
              </div>
              <h3 class="news-title">${item.title}</h3>
              <p class="news-summary">${item.summary}</p>
            </div>
            <div class="news-footer">
              <span>${item.reads_count || 0} قراءة</span>
              <span style="color: #4DB6AC; font-weight: 700;">منظومة مُسَرِّب</span>
            </div>
          </article>
        `;
      }).join('');
    }

    function trackDownloadMetric() {
      try {
        fetch('api/public_data.php?action=track_download', { method: 'POST' }).catch(() => {});
      } catch (_) {}
    }

    let globalDownloadUrl = "api/download.php";

    function renderRelease(rel) {
      if (!rel) return;
      globalDownloadUrl = rel.download_url || "api/download.php";

      const btn1 = document.getElementById("heroDirectDownloadBtn");
      const btn2 = document.getElementById("mainDownloadApkBtn");
      if (btn1) {
        btn1.href = globalDownloadUrl;
        btn1.onclick = trackDownloadMetric;
      }
      if (btn2) {
        btn2.href = globalDownloadUrl;
        btn2.onclick = trackDownloadMetric;
      }

      const vText = `الإصدار: v${rel.version_name || '1.4.0'}`;
      const el1 = document.getElementById("heroReleaseVer");
      const el2 = document.getElementById("footerVersionText");
      const el3 = document.getElementById("footerSizeText");
      if (el1) el1.innerText = vText;
      if (el2) el2.innerText = `الإصدار: v${rel.version_name || '1.4.0'}`;
      if (el3) el3.innerText = `الحجم: ${rel.file_size_mb || 2.78} MB`;
    }

    function initMarketChart() {
      const ctx = document.getElementById('marketTrendChart');
      if (!ctx) return;
      new Chart(ctx, {
        type: 'line',
        data: {
          labels: ['يناير', 'فبراير', 'مارس', 'أبريل', 'مايو', 'يونيو', 'يوليو', 'أغسطس', 'سبتمبر', 'أكتوبر'],
          datasets: [{
            label: 'متوسط مؤشر تسعير ساعة الري عبر الفصول',
            data: [7200, 7100, 7400, 7800, 8500, 8800, 8200, 7600, 7400, 7500],
            borderColor: '#00B4D8',
            backgroundColor: 'rgba(0, 180, 216, 0.08)',
            fill: true,
            tension: 0.35,
            pointBackgroundColor: '#F59E0B',
            pointRadius: 3,
            pointHoverRadius: 5
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: {
              labels: { color: '#B2DFDB', font: { family: 'Cairo', size: 12 } }
            }
          },
          scales: {
            x: {
              ticks: { color: '#80CBC4', font: { family: 'Cairo', size: 11 } },
              grid: { display: false }
            },
            y: {
              ticks: { color: '#80CBC4', font: { family: 'Cairo', size: 11 } },
              grid: { color: 'rgba(255,255,255,0.05)' }
            }
          }
        }
      });
    }

    // Initialize on load
    document.addEventListener("DOMContentLoaded", () => {
      loadPublicData();
      initMarketChart();
    });
  </script>
</body>
</html>
