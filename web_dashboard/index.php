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
      --primary-light: #4FB3BF;
      --teal-accent: #00B4D8;
      --gold: #FFB300;
      --gold-glow: rgba(255, 179, 0, 0.35);
      --emerald: #00C853;
      --bg: #071015;
      --surface: #0E1D24;
      --surface-light: #152B35;
      --surface-card: rgba(14, 29, 36, 0.75);
      --border: rgba(0, 180, 216, 0.2);
      --border-light: rgba(255, 255, 255, 0.08);
      --text: #E0F2F1;
      --text-muted: #80CBC4;
      --text-heading: #FFFFFF;
      --radius-sm: 8px;
      --radius-md: 14px;
      --radius-lg: 22px;
      --radius-full: 9999px;
    }

    * {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
      font-family: 'Cairo', system-ui, sans-serif;
    }

    body {
      background-color: var(--bg);
      background-image: 
        radial-gradient(circle at 15% 15%, rgba(0, 131, 143, 0.25) 0%, transparent 45%),
        radial-gradient(circle at 85% 30%, rgba(0, 180, 216, 0.15) 0%, transparent 50%),
        radial-gradient(circle at 50% 85%, rgba(255, 179, 0, 0.1) 0%, transparent 55%);
      background-attachment: fixed;
      color: var(--text);
      line-height: 1.6;
      overflow-x: hidden;
    }

    /* Top Live Alert Ticker */
    .top-ticker {
      background: linear-gradient(90deg, #004D40, #00838F, #004D40);
      color: #E0F7FA;
      font-size: 13px;
      padding: 8px 16px;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 12px;
      border-bottom: 1px solid rgba(0, 229, 255, 0.3);
      font-weight: 600;
    }
    .pulse-dot {
      width: 9px;
      height: 9px;
      background: var(--gold);
      border-radius: 50%;
      box-shadow: 0 0 10px var(--gold);
      animation: pulse 1.5s infinite;
    }
    @keyframes pulse {
      0% { transform: scale(0.9); opacity: 0.8; }
      50% { transform: scale(1.3); opacity: 1; filter: drop-shadow(0 0 6px var(--gold)); }
      100% { transform: scale(0.9); opacity: 0.8; }
    }

    /* Navigation Bar */
    header.main-header {
      position: sticky;
      top: 0;
      z-index: 1000;
      background: rgba(7, 16, 21, 0.85);
      backdrop-filter: blur(16px);
      -webkit-backdrop-filter: blur(16px);
      border-bottom: 1px solid var(--border);
      padding: 14px 24px;
    }
    .header-container {
      max-width: 1240px;
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
      width: 44px;
      height: 44px;
      background: linear-gradient(135deg, #00BCD4, #004D40);
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
      box-shadow: 0 4px 16px rgba(0, 188, 212, 0.4);
      border: 1px solid rgba(255, 255, 255, 0.2);
    }
    .brand-title {
      font-size: 20px;
      font-weight: 900;
      letter-spacing: -0.5px;
      background: linear-gradient(135deg, #FFFFFF, #80DEEA);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }
    .brand-sub {
      font-size: 11px;
      color: var(--text-muted);
      font-weight: 500;
    }

    .nav-links {
      display: flex;
      align-items: center;
      gap: 22px;
    }
    .nav-link {
      color: #B2DFDB;
      text-decoration: none;
      font-size: 14px;
      font-weight: 600;
      transition: all 0.2s;
    }
    .nav-link:hover {
      color: #FFF;
      transform: translateY(-1px);
    }

    .btn-nav-action {
      background: linear-gradient(135deg, #00897B, #004D40);
      border: 1px solid #4DB6AC;
      color: #FFF;
      padding: 8px 18px;
      border-radius: var(--radius-full);
      font-size: 13px;
      font-weight: 700;
      text-decoration: none;
      display: inline-flex;
      align-items: center;
      gap: 8px;
      box-shadow: 0 4px 14px rgba(0, 137, 123, 0.3);
      transition: all 0.2s ease;
    }
    .btn-nav-action:hover {
      background: linear-gradient(135deg, #009688, #00695C);
      box-shadow: 0 6px 20px rgba(0, 150, 136, 0.5);
      transform: translateY(-2px);
    }
    .btn-admin-access {
      background: rgba(255, 255, 255, 0.06);
      border: 1px solid var(--border-light);
      color: var(--gold);
      padding: 7px 14px;
      border-radius: var(--radius-full);
      font-size: 12px;
      font-weight: 700;
      text-decoration: none;
      transition: all 0.2s;
    }
    .btn-admin-access:hover {
      background: rgba(255, 179, 0, 0.15);
      border-color: var(--gold);
    }

    /* Main Container */
    .container {
      max-width: 1240px;
      margin: 0 auto;
      padding: 0 20px;
    }

    /* Hero Section */
    .hero {
      padding: 50px 0 35px 0;
      display: grid;
      grid-template-columns: 1.15fr 0.85fr;
      gap: 36px;
      align-items: center;
    }
    .hero-badge {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      background: rgba(0, 188, 212, 0.12);
      border: 1px solid rgba(0, 188, 212, 0.35);
      color: #80DEEA;
      padding: 6px 14px;
      border-radius: var(--radius-full);
      font-size: 12.5px;
      font-weight: 700;
      margin-bottom: 16px;
    }
    .hero-title {
      font-size: 40px;
      font-weight: 900;
      line-height: 1.25;
      color: #FFF;
      margin-bottom: 16px;
    }
    .hero-title span {
      background: linear-gradient(135deg, #00E5FF, #FFB300);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }
    .hero-description {
      font-size: 16px;
      color: #B2DFDB;
      line-height: 1.8;
      margin-bottom: 28px;
      max-width: 580px;
    }
    .hero-actions {
      display: flex;
      flex-wrap: wrap;
      gap: 14px;
      align-items: center;
      margin-bottom: 26px;
    }
    .btn-primary-download {
      background: linear-gradient(135deg, #FFB300, #FF8F00);
      color: #05141A;
      font-size: 16px;
      font-weight: 900;
      padding: 14px 28px;
      border-radius: var(--radius-md);
      text-decoration: none;
      display: inline-flex;
      align-items: center;
      gap: 12px;
      box-shadow: 0 8px 24px rgba(255, 179, 0, 0.4);
      transition: all 0.25s ease;
      border: 1px solid #FFE082;
    }
    .btn-primary-download:hover {
      transform: translateY(-3px);
      box-shadow: 0 12px 32px rgba(255, 179, 0, 0.6);
      background: linear-gradient(135deg, #FFC107, #FFA000);
    }
    .btn-secondary-link {
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid var(--border);
      color: #E0F2F1;
      font-size: 14.5px;
      font-weight: 700;
      padding: 13px 22px;
      border-radius: var(--radius-md);
      text-decoration: none;
      display: inline-flex;
      align-items: center;
      gap: 8px;
      transition: all 0.2s;
    }
    .btn-secondary-link:hover {
      background: rgba(0, 188, 212, 0.15);
      border-color: #00BCD4;
      color: #FFF;
    }

    /* Live Water Card (Hero Visual) */
    .hero-visual-card {
      background: linear-gradient(145deg, rgba(18, 35, 43, 0.85), rgba(9, 18, 23, 0.95));
      border: 1px solid rgba(0, 180, 216, 0.35);
      border-radius: var(--radius-lg);
      padding: 26px;
      box-shadow: 0 20px 50px rgba(0,0,0,0.5), 0 0 30px rgba(0, 180, 216, 0.15);
      position: relative;
      overflow: hidden;
    }
    .hero-visual-card::before {
      content: '';
      position: absolute;
      top: -50px;
      left: -50px;
      width: 150px;
      height: 150px;
      background: radial-gradient(circle, rgba(0, 188, 212, 0.2), transparent 70%);
      pointer-events: none;
    }

    /* Live Water Meter Gauge Component */
    .water-gauge-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      margin-bottom: 20px;
      position: relative;
    }
    .gauge-wrapper {
      position: relative;
      width: 190px;
      height: 105px;
      overflow: hidden;
    }
    .gauge-semicircle {
      width: 190px;
      height: 190px;
      border-radius: 50%;
      border: 18px solid rgba(255, 255, 255, 0.08);
      border-top-color: #00BCD4;
      border-right-color: #00E676;
      border-left-color: #FFB300;
      transform: rotate(45deg);
      box-sizing: border-box;
      filter: drop-shadow(0 0 10px rgba(0, 188, 212, 0.5));
    }
    .gauge-center-text {
      position: absolute;
      bottom: 0;
      left: 50%;
      transform: translateX(-50%);
      text-align: center;
    }
    .gauge-percent {
      font-size: 34px;
      font-weight: 900;
      color: #FFF;
      line-height: 1;
    }
    .gauge-sub {
      font-size: 11px;
      color: var(--text-muted);
      font-weight: 600;
    }

    /* Key Live Indicators Grid */
    .live-indicators {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 12px;
      margin-top: 18px;
    }
    .indicator-box {
      background: rgba(255, 255, 255, 0.03);
      border: 1px solid var(--border-light);
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
      font-size: 18px;
      font-weight: 800;
      color: #FFF;
    }
    .indicator-val small {
      font-size: 12px;
      color: var(--gold);
      margin-right: 4px;
    }

    /* Section Headings */
    .section-title-wrap {
      text-align: center;
      margin: 60px 0 35px 0;
    }
    .section-badge {
      display: inline-block;
      background: rgba(255, 179, 0, 0.12);
      border: 1px solid rgba(255, 179, 0, 0.35);
      color: var(--gold);
      padding: 4px 14px;
      border-radius: var(--radius-full);
      font-size: 12px;
      font-weight: 700;
      margin-bottom: 10px;
    }
    .section-title {
      font-size: 28px;
      font-weight: 900;
      color: #FFF;
      margin-bottom: 8px;
    }
    .section-desc {
      font-size: 14.5px;
      color: var(--text-muted);
      max-width: 600px;
      margin: 0 auto;
    }

    /* Market Pulse Cards */
    .market-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
      gap: 20px;
      margin-bottom: 50px;
    }
    .market-card {
      background: var(--surface-card);
      backdrop-filter: blur(12px);
      border: 1px solid var(--border);
      border-radius: var(--radius-lg);
      padding: 24px;
      transition: all 0.25s ease;
      position: relative;
    }
    .market-card:hover {
      transform: translateY(-4px);
      border-color: rgba(0, 188, 212, 0.6);
      box-shadow: 0 12px 30px rgba(0, 0, 0, 0.4);
    }
    .market-card-top {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 14px;
    }
    .market-card-icon {
      width: 42px;
      height: 42px;
      border-radius: 12px;
      background: rgba(0, 188, 212, 0.15);
      border: 1px solid rgba(0, 188, 212, 0.3);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;
    }

    /* News & Articles Grid */
    .news-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
      gap: 22px;
      margin-bottom: 60px;
    }
    .news-card {
      background: linear-gradient(160deg, rgba(18, 35, 43, 0.7), rgba(9, 18, 23, 0.85));
      border: 1px solid var(--border-light);
      border-radius: var(--radius-lg);
      padding: 24px;
      transition: all 0.25s ease;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
    }
    .news-card:hover {
      border-color: rgba(0, 188, 212, 0.4);
      transform: translateY(-3px);
      box-shadow: 0 10px 28px rgba(0,0,0,0.3);
    }
    .news-meta {
      display: flex;
      align-items: center;
      justify-content: space-between;
      font-size: 11.5px;
      color: var(--text-muted);
      margin-bottom: 12px;
    }
    .news-category {
      background: rgba(0, 137, 123, 0.25);
      color: #4DB6AC;
      border: 1px solid rgba(0, 137, 123, 0.4);
      padding: 3px 10px;
      border-radius: var(--radius-full);
      font-weight: 700;
    }
    .news-title {
      font-size: 17px;
      font-weight: 800;
      color: #FFF;
      line-height: 1.5;
      margin-bottom: 10px;
    }
    .news-summary {
      font-size: 13.5px;
      color: #B2DFDB;
      line-height: 1.7;
      margin-bottom: 16px;
    }

    /* Download Banner Call to Action */
    .cta-download-box {
      background: linear-gradient(135deg, rgba(0, 77, 64, 0.85), rgba(0, 131, 143, 0.85));
      border: 1px solid rgba(0, 229, 255, 0.4);
      border-radius: var(--radius-lg);
      padding: 40px;
      text-align: center;
      margin: 40px 0 70px 0;
      box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5), 0 0 40px rgba(0, 188, 212, 0.2);
      position: relative;
    }
    .cta-download-box h2 {
      font-size: 28px;
      font-weight: 900;
      color: #FFF;
      margin-bottom: 12px;
    }
    .cta-download-box p {
      font-size: 15px;
      color: #E0F2F1;
      max-width: 650px;
      margin: 0 auto 24px auto;
    }

    /* Footer */
    footer.public-footer {
      border-top: 1px solid var(--border-light);
      background: rgba(4, 10, 13, 0.95);
      padding: 40px 0 25px 0;
      font-size: 13px;
      color: var(--text-muted);
    }
    .footer-content {
      display: flex;
      flex-wrap: wrap;
      justify-content: space-between;
      align-items: center;
      gap: 20px;
      margin-bottom: 25px;
    }

    @media (max-width: 868px) {
      .hero {
        grid-template-columns: 1fr;
        padding: 30px 0;
      }
      .hero-title {
        font-size: 30px;
      }
      .nav-links {
        display: none;
      }
    }
  </style>
</head>
<body>

  <!-- Top Alert Ticker -->
  <div class="top-ticker">
    <div class="pulse-dot"></div>
    <span id="topAlertMessage">مؤشر ري مباشر: وفرة ملحوظة في مياه الآبار الجوفية واستقرار معدلات الضخ الزراعي.</span>
  </div>

  <!-- Header -->
  <header class="main-header">
    <div class="header-container">
      <a href="index.php" class="brand-logo">
        <div class="logo-badge">💧</div>
        <div>
          <div class="brand-title">مُسَرِّب • MOSARIB</div>
          <div class="brand-sub">المنظومة الوطنية الذكية لإدارة مياه الآبار والري</div>
        </div>
      </a>

      <nav class="nav-links">
        <a href="#water-meter" class="nav-link">مؤشر توفر المياه</a>
        <a href="#market-pulse" class="nav-link">أسعار ساعة الري</a>
        <a href="#portal-news" class="nav-link">تقارير وأخبار</a>
        <a href="#download-section" class="nav-link">تحميل التطبيق</a>
      </nav>

      <div style="display: flex; align-items: center; gap: 10px;">
        <a href="#download-section" class="btn-nav-action">
          <span>⚡ تحميل APK</span>
        </a>
        <a href="admin.php" class="btn-admin-access" title="دخول إدارة المنظومة المشفر">
          🔒 لوحة الإدارة
        </a>
      </div>
    </div>
  </header>

  <main class="container">

    <!-- Hero Section -->
    <section class="hero">
      <div>
        <div class="hero-badge">
          <span>🌱 الشبكة الذكية الأولى لخدمة المزارعين وملاك الآبار</span>
        </div>
        <h1 class="hero-title">
          نبض مياه الآبار <span>وساعات الري الزراعي</span> بين يديك بدقة متناهية
        </h1>
        <p class="hero-description">
          تطبيق "مُسَرِّب" يربط شبكة الآبار ومسربي المياه وملاك الأراضي الزراعية في منظومة رقمية موحدة: حساب دقيق لكسور ساعات الري، تتبع الفاقد المائي، إصدار سندات وكشوفات الحساب، ومتابعة فورية لمؤشرات سوق المياه.
        </p>

        <div class="hero-actions">
          <a href="#" id="heroDirectDownloadBtn" class="btn-primary-download">
            <span>📥 تحميل التطبيق المباشر (APK)</span>
          </a>
          <a href="#water-meter" class="btn-secondary-link">
            <span>📊 عرض تقرير وفرة الماء والأسعار</span>
          </a>
        </div>

        <div style="font-size: 12px; color: var(--text-muted); display: flex; align-items: center; gap: 16px;">
          <span>✔️ آمن وموثوق 100%</span>
          <span>✔️ تحديث فوري مباشر</span>
          <span id="heroReleaseVer">الإصدار: v1.1.0 (أحدث نسخة)</span>
        </div>
      </div>

      <!-- Live Water Meter Card -->
      <div class="hero-visual-card" id="water-meter">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px;">
          <b style="font-size: 15px; color: #FFF; display: flex; align-items: center; gap: 6px;">
            <span>💧</span> مؤشر وفرة المياه اللحظي
          </b>
          <span class="hero-badge" style="margin: 0; background: rgba(0, 200, 83, 0.15); border-color: #00C853; color: #69F0AE;" id="badgeWaterStatus">
            متوفر ومستقر ✅
          </span>
        </div>

        <div class="water-gauge-container">
          <div class="gauge-wrapper">
            <div class="gauge-semicircle" id="gaugeCircle"></div>
            <div class="gauge-center-text">
              <div class="gauge-percent" id="waterIndexPercent">84%</div>
              <div class="gauge-sub">معدل الوفرة والتدفق</div>
            </div>
          </div>
        </div>

        <div style="text-align: center; font-size: 13px; color: #B2DFDB; margin-bottom: 14px;" id="waterStatusTextDesc">
          تدفق المياه الجوفية بحالة ممتازة في أغلب المناطق الزراعية
        </div>

        <!-- Real-time key metrics -->
        <div class="live-indicators">
          <div class="indicator-box">
            <div class="indicator-label"><span>👥</span> المسربون الفعّالون الآن</div>
            <div class="indicator-val" id="activeOperatorsNow">-- <small>مسرب نشط</small></div>
          </div>
          <div class="indicator-box">
            <div class="indicator-label"><span>💰</span> متوسط سعر ساعة الري</div>
            <div class="indicator-val" id="avgHourlyPrice">-- <small id="currencyUnit">ريال</small></div>
          </div>
          <div class="indicator-box">
            <div class="indicator-label"><span>🚜</span> إجمالي الآبار والمسربين</div>
            <div class="indicator-val" id="totalRegisteredOperators">-- <small>مشترك مسجل</small></div>
          </div>
          <div class="indicator-box">
            <div class="indicator-label"><span>📍</span> النطاقات والمناطق المغطاة</div>
            <div class="indicator-val" id="activeRegionsCount">18 <small>منطقة زراعية</small></div>
          </div>
        </div>
      </div>
    </section>

    <!-- Market Pulse Section -->
    <div class="section-title-wrap" id="market-pulse">
      <div class="section-badge">📈 شفافية السوق الزراعي</div>
      <h2 class="section-title">بورصة ومؤشرات أسعار ساعات الري</h2>
      <p class="section-desc">إحصائيات حية مستمدة من واقع مئات دورات الري المنفذة والمسجلة في تطبيق مُسَرِّب لضمان عدالة التسعير لملاك الآبار والمزارعين.</p>
    </div>

    <div class="market-grid">
      <div class="market-card">
        <div class="market-card-top">
          <div class="market-card-icon" style="color: #4DB6AC;">📊</div>
          <span style="font-size: 12px; color: #80CBC4;">تحديث يومي</span>
        </div>
        <div style="font-size: 13px; color: var(--text-muted); margin-bottom: 6px;">متوسط سعر الساعة العام</div>
        <div style="font-size: 26px; font-weight: 900; color: #FFF; margin-bottom: 8px;" id="boxAvgPrice">7,500 <span style="font-size: 14px; color: var(--gold);">ريال/ساعة</span></div>
        <p style="font-size: 12.5px; color: #B2DFDB; line-height: 1.6;">يمثل السعر العادل التوازني لمعظم الآبار التي تعمل بالطاقة الشمسية أو الديزل في الموسم الحالي.</p>
      </div>

      <div class="market-card">
        <div class="market-card-top">
          <div class="market-card-icon" style="color: #69F0AE;">📉</div>
          <span style="font-size: 12px; color: #69F0AE;">الأدنى تسجيلاً</span>
        </div>
        <div style="font-size: 13px; color: var(--text-muted); margin-bottom: 6px;">أقل سعر ساعة معتمد</div>
        <div style="font-size: 26px; font-weight: 900; color: #FFF; margin-bottom: 8px;" id="boxMinPrice">6,000 <span style="font-size: 14px; color: var(--gold);">ريال/ساعة</span></div>
        <p style="font-size: 12.5px; color: #B2DFDB; line-height: 1.6;">يسجل غالباً في الآبار ذات التدفق الغزير أو المنظومات الشمسية المتكاملة ذات التكلفة المنخفضة.</p>
      </div>

      <div class="market-card">
        <div class="market-card-top">
          <div class="market-card-icon" style="color: #FFB74D;">📈</div>
          <span style="font-size: 12px; color: #FFB74D;">الأعلى طلباً</span>
        </div>
        <div style="font-size: 13px; color: var(--text-muted); margin-bottom: 6px;">أعلى سعر ساعة مسجل</div>
        <div style="font-size: 26px; font-weight: 900; color: #FFF; margin-bottom: 8px;" id="boxMaxPrice">9,500 <span style="font-size: 14px; color: var(--gold);">ريال/ساعة</span></div>
        <p style="font-size: 12.5px; color: #B2DFDB; line-height: 1.6;">في فترات ذروة ري المحاصيل الحساسة كشجرة القات والفاكهة أو الآبار العميقة العاملة بالمولدات.</p>
      </div>
    </div>

    <!-- Live Chart: Price & Water Fluctuation -->
    <div style="background: var(--surface-card); border: 1px solid var(--border); border-radius: var(--radius-lg); padding: 26px; margin-bottom: 60px;">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
        <div>
          <h3 style="font-size: 18px; font-weight: 800; color: #FFF;">📈 المنحنى البياني لاستقرار أسعار ساعات الري عبر الأشهر</h3>
          <p style="font-size: 12px; color: var(--text-muted);">رصد حركة مؤشر الأسعار بحسب تعاقب فصول الأمطار ومواسم الجفاف</p>
        </div>
        <span class="hero-badge" style="margin: 0;">بيانات تحليلية موثوقة</span>
      </div>
      <div style="height: 260px;">
        <canvas id="marketTrendChart"></canvas>
      </div>
    </div>

    <!-- News & Technical Guidance Section -->
    <div class="section-title-wrap" id="portal-news">
      <div class="section-badge">📰 مركز الأخبار والإرشادات</div>
      <h2 class="section-title">أحدث تقارير مياه الآبار والري الزراعي</h2>
      <p class="section-desc">معلومات حصرية وتوجيهات فنية وتقارير دورية تضاف مباشرة من إدارة المنظومة لإرشاد المزارعين والمسربين.</p>
    </div>

    <div class="news-grid" id="newsCardsContainer">
      <div style="text-align: center; color: var(--text-muted); padding: 30px; grid-column: 1 / -1;">
        جاري تحميل أحدث التقارير والأخبار...
      </div>
    </div>

    <!-- Direct Download Call to Action -->
    <section class="cta-download-box" id="download-section">
      <h2>🚀 حمّل تطبيق مُسَرِّب الآن مجاناً وابدأ إدارة آبارك</h2>
      <p>
        احصل على النسخة الأحدث والأكثر استقراراً مباشرة بصيغة APK دون وسيط، مع تجربة مجانية لـ 200 عملية ري وسندات مالية كاملة.
      </p>

      <div style="display: flex; justify-content: center; gap: 16px; flex-wrap: wrap; margin-bottom: 20px;">
        <a href="#" id="mainDownloadApkBtn" class="btn-primary-download" style="font-size: 18px; padding: 16px 36px;">
          <span>⚡ تحميل تطبيق مُسَرِّب (APK مباشر)</span>
        </a>
      </div>

      <div style="display: flex; justify-content: center; align-items: center; gap: 24px; font-size: 13px; color: #E0F2F1;">
        <span id="footerVersionText">📦 الإصدار: v1.1.0</span>
        <span id="footerSizeText">💾 الحجم: 24.5 MB</span>
        <span>🛡️ فحص أمني وخلو من الفيروسات 100%</span>
      </div>
    </section>

  </main>

  <!-- Public Footer -->
  <footer class="public-footer">
    <div class="container">
      <div class="footer-content">
        <div style="display: flex; align-items: center; gap: 10px;">
          <div class="logo-badge" style="width: 36px; height: 36px; font-size: 18px;">💧</div>
          <div>
            <b style="color: #FFF; font-size: 15px;">مُسَرِّب (MOSARIB)</b>
            <div style="font-size: 11px;">تطوير وهندسة: المهندس عمر الحومي &copy; 2026</div>
          </div>
        </div>

        <div style="display: flex; gap: 18px;">
          <a href="#water-meter" class="nav-link" style="font-size: 13px;">مؤشر المياه</a>
          <a href="#market-pulse" class="nav-link" style="font-size: 13px;">أسعار الري</a>
          <a href="#portal-news" class="nav-link" style="font-size: 13px;">التقارير</a>
          <a href="admin.php" class="nav-link" style="font-size: 13px; color: var(--gold);">🔒 بوابة الإدارة</a>
        </div>
      </div>

      <div style="text-align: center; font-size: 12px; color: rgba(255,255,255,0.4); border-top: 1px solid var(--border-light); padding-top: 20px;">
        جميع الحقوق محفوظة لمنظومة مُسَرِّب لإدارة مياه الآبار ودورات الري الزراعي.
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
        }
      } catch (err) {
        console.error("Failed to load public data:", err);
      }
    }

    function renderMetrics(m) {
      if (!m) return;
      document.getElementById("topAlertMessage").innerText = m.headline_alert || "مؤشر ري مباشر: استقرار تام في توفر المياه الجوفية وأسعار ساعات الري.";
      
      const p = m.index_percent || 84;
      document.getElementById("waterIndexPercent").innerText = p + "%";
      document.getElementById("waterStatusTextDesc").innerText = m.status_text || "تدفق المياه الجوفية بحالة ممتازة";
      document.getElementById("badgeWaterStatus").innerText = (m.status === 'ABUNDANT' ? 'وفيرة جداً 🌊' : (m.status === 'GOOD' ? 'متوفر ومستقر ✅' : 'متوسط ⚠️'));

      document.getElementById("activeOperatorsNow").innerHTML = `${m.active_operators_now} <small>مسرب نشط</small>`;
      document.getElementById("totalRegisteredOperators").innerHTML = `${m.total_operators_registered} <small>مشترك مسجل</small>`;
      document.getElementById("avgHourlyPrice").innerHTML = `${m.avg_hourly_price.toLocaleString('ar-EG')} <small>${m.currency_symbol}</small>`;
      document.getElementById("currencyUnit").innerText = m.currency_symbol;
      document.getElementById("activeRegionsCount").innerHTML = `${m.active_regions} <small>منطقة زراعية</small>`;

      document.getElementById("boxAvgPrice").innerHTML = `${m.avg_hourly_price.toLocaleString('ar-EG')} <span style="font-size: 14px; color: var(--gold);">${m.currency_symbol}/ساعة</span>`;
      document.getElementById("boxMinPrice").innerHTML = `${m.min_hourly_price.toLocaleString('ar-EG')} <span style="font-size: 14px; color: var(--gold);">${m.currency_symbol}/ساعة</span>`;
      document.getElementById("boxMaxPrice").innerHTML = `${m.max_hourly_price.toLocaleString('ar-EG')} <span style="font-size: 14px; color: var(--gold);">${m.currency_symbol}/ساعة</span>`;
    }

    function renderNews(news) {
      const container = document.getElementById("newsCardsContainer");
      if (!container) return;
      if (!news || news.length === 0) {
        container.innerHTML = `<div style="text-align: center; color: var(--text-muted); padding: 30px; grid-column: 1 / -1;">لا توجد مقالات منشورة حالياً.</div>`;
        return;
      }

      container.innerHTML = news.map(item => {
        const dateStr = item.published_at ? new Date(item.published_at).toLocaleDateString('ar-EG', { year: 'numeric', month: 'long', day: 'numeric' }) : '';
        return `
          <article class="news-card">
            <div>
              <div class="news-meta">
                <span class="news-category">${item.icon || '💧'} ${item.category || 'عام'}</span>
                <span>${dateStr}</span>
              </div>
              <h3 class="news-title">${item.title}</h3>
              <p class="news-summary">${item.summary}</p>
            </div>
            <div style="display: flex; justify-content: space-between; align-items: center; border-top: 1px solid var(--border-light); padding-top: 12px; font-size: 12px; color: var(--text-muted);">
              <span>👁️ ${item.reads_count || 120} قراءة</span>
              <span style="color: #4DB6AC; font-weight: 700;">منظومة مُسَرِّب 💧</span>
            </div>
          </article>
        `;
      }).join('');
    }

    function renderRelease(rel) {
      if (!rel) return;
      globalDownloadUrl = rel.download_url || "uploads/Mosarib_Latest.apk";

      const btn1 = document.getElementById("heroDirectDownloadBtn");
      const btn2 = document.getElementById("mainDownloadApkBtn");
      if (btn1) btn1.href = globalDownloadUrl;
      if (btn2) btn2.href = globalDownloadUrl;

      const vText = `الإصدار: v${rel.version_name || '1.1.0'} (أحدث نسخة)`;
      const el1 = document.getElementById("heroReleaseVer");
      const el2 = document.getElementById("footerVersionText");
      const el3 = document.getElementById("footerSizeText");
      if (el1) el1.innerText = vText;
      if (el2) el2.innerText = `📦 الإصدار: v${rel.version_name || '1.1.0'}`;
      if (el3) el3.innerText = `💾 الحجم: ${rel.file_size_mb || 24.5} MB`;
    }

    function initMarketChart() {
      const ctx = document.getElementById('marketTrendChart');
      if (!ctx) return;
      new Chart(ctx, {
        type: 'line',
        data: {
          labels: ['يناير', 'فبراير', 'مارس', 'أبريل', 'مايو', 'يونيو', 'يوليو', 'أغسطس', 'سبتمبر', 'أكتوبر'],
          datasets: [{
            label: 'متوسط سعر ساعة الري (ريال)',
            data: [7200, 7100, 7400, 7800, 8500, 8900, 8100, 7600, 7400, 7500],
            borderColor: '#00B4D8',
            backgroundColor: 'rgba(0, 180, 216, 0.12)',
            fill: true,
            tension: 0.35,
            pointBackgroundColor: '#FFB300',
            pointRadius: 4,
            pointHoverRadius: 6
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: {
              labels: { color: '#B2DFDB', font: { family: 'Cairo' } }
            }
          },
          scales: {
            x: {
              ticks: { color: '#80CBC4', font: { family: 'Cairo' } },
              grid: { display: false }
            },
            y: {
              ticks: { color: '#80CBC4', font: { family: 'Cairo' } },
              grid: { color: 'rgba(255,255,255,0.06)' }
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
