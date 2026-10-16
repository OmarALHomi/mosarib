<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>لوحة تحكم وإدارة تراخيص ومستخدمي مُسَرِّب | م. عمر الحومي</title>
  <script>
    (function() {
      try {
        const t = localStorage.getItem('mosarib_admin_theme');
        if (t === 'light') document.documentElement.setAttribute('data-theme', 'light');
      } catch(e) {}
    })();
  </script>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Cairo:wght@400;600;700;800;900&display=swap" rel="stylesheet">
  <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js"></script>
  <style>
    :root {
      --primary: #00838F;
      --primary-dark: #004D40;
      --primary-light: #4FB3BF;
      --accent: #FFB300;
      --bg: #0B171D;
      --body-gradient: radial-gradient(circle at top, #132A32, var(--bg));
      --card-bg: #12232B;
      --card-border: rgba(0, 150, 166, 0.25);
      --card-shadow: 0 8px 24px rgba(0, 0, 0, 0.35);
      --text: #E0F2F1;
      --text-title: #FFFFFF;
      --text-muted: #80CBC4;
      --danger: #FF5252;
      --success: #00E676;
      --input-bg: #081318;
      --input-border: rgba(0, 150, 166, 0.4);
      --table-th-bg: rgba(0, 131, 143, 0.2);
      --table-hover: rgba(0, 150, 166, 0.08);
      --table-border: rgba(0, 150, 166, 0.15);
      --stat-icon-bg: rgba(0, 150, 166, 0.15);
      --theme-btn-bg: rgba(255, 255, 255, 0.08);
      --theme-btn-border: rgba(255, 255, 255, 0.18);
      --theme-btn-color: #E0F2F1;
    }

    [data-theme="light"] {
      --primary: #00838F;
      --primary-dark: #004D40;
      --primary-light: #0097A7;
      --accent: #D97706;
      --bg: #F4F7F8;
      --body-gradient: radial-gradient(circle at top, #E2F1F3, #F4F7F8);
      --card-bg: #FFFFFF;
      --card-border: rgba(0, 131, 143, 0.18);
      --card-shadow: 0 4px 20px rgba(0, 77, 64, 0.08);
      --text: #1E293B;
      --text-title: #0F172A;
      --text-muted: #475569;
      --danger: #DC2626;
      --success: #16A34A;
      --input-bg: #F8FAFC;
      --input-border: #CBD5E1;
      --table-th-bg: #E6F4F5;
      --table-hover: #F1F8F8;
      --table-border: #E2E8F0;
      --stat-icon-bg: #E0F2F1;
      --theme-btn-bg: #FFFFFF;
      --theme-btn-border: rgba(0, 131, 143, 0.25);
      --theme-btn-color: #004D40;
    }

    * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Cairo', 'Segoe UI', Tahoma, sans-serif; }
    body {
      background: var(--body-gradient);
      color: var(--text);
      min-height: 100vh;
      padding: 24px;
      transition: background 0.3s ease, color 0.3s ease;
    }
    .container { max-width: 1280px; margin: 0 auto; }
    
    /* Header */
    header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding-bottom: 20px;
      border-bottom: 1px solid var(--card-border);
      margin-bottom: 22px;
      flex-wrap: wrap;
      gap: 14px;
    }
    .brand { display: flex; align-items: center; gap: 14px; }
    .brand-icon {
      width: 48px;
      height: 48px;
      background: linear-gradient(135deg, var(--primary), #00BFA5);
      border-radius: 14px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
      box-shadow: 0 8px 22px rgba(0, 191, 165, 0.35);
      flex-shrink: 0;
    }
    h1 { font-size: 22px; font-weight: 800; color: var(--text-title); transition: color 0.3s ease; line-height: 1.2; }
    .subtitle { font-size: 12.5px; color: var(--text-muted); margin-top: 2px; }

    /* Header Actions */
    .header-actions {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-wrap: wrap;
    }

    /* Theme Toggle Button */
    .theme-toggle-btn {
      display: inline-flex;
      align-items: center;
      gap: 7px;
      background: var(--theme-btn-bg);
      border: 1px solid var(--theme-btn-border);
      color: var(--theme-btn-color);
      padding: 7px 14px;
      border-radius: 20px;
      font-size: 12.5px;
      font-weight: 700;
      cursor: pointer;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      transition: all 0.25s ease;
      user-select: none;
    }
    .theme-toggle-btn:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 14px rgba(0, 131, 143, 0.25);
    }
    .theme-toggle-btn .theme-icon {
      font-size: 15px;
      display: inline-block;
      transition: transform 0.3s ease;
    }
    .theme-toggle-btn:hover .theme-icon {
      transform: rotate(20deg) scale(1.15);
    }

    .status-badge {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      background: rgba(0, 230, 118, 0.14);
      color: var(--success);
      padding: 6px 12px;
      border-radius: 20px;
      font-size: 12.5px;
      font-weight: 700;
      border: 1px solid rgba(0, 230, 118, 0.3);
    }
    .status-dot {
      width: 8px;
      height: 8px;
      background: var(--success);
      border-radius: 50%;
      box-shadow: 0 0 8px var(--success);
      animation: pulseDot 2s infinite ease-in-out;
    }
    @keyframes pulseDot {
      0%, 100% { opacity: 1; transform: scale(1); }
      50% { opacity: 0.5; transform: scale(0.85); }
    }

    /* Stats Grid */
    .stats-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
      gap: 14px;
      margin-bottom: 24px;
    }
    .stat-card {
      background: var(--card-bg);
      border: 1px solid var(--card-border);
      border-radius: 16px;
      padding: 16px 18px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      box-shadow: var(--card-shadow);
      transition: all 0.25s ease;
      position: relative;
      overflow: hidden;
    }
    .stat-card::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      height: 3px;
      background: linear-gradient(90deg, transparent, var(--primary), transparent);
      opacity: 0.5;
    }
    .stat-card:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 24px rgba(0, 150, 166, 0.2);
    }
    .stat-info h3 { font-size: 12px; font-weight: 700; color: var(--text-muted); margin-bottom: 4px; }
    .stat-info .num { font-size: 26px; font-weight: 900; color: var(--text-title); transition: color 0.3s ease; }
    .stat-icon {
      width: 44px;
      height: 44px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;
      background: var(--stat-icon-bg);
      color: var(--primary-light);
      flex-shrink: 0;
    }

    /* Responsive Pill Tabs */
    .tabs {
      display: flex;
      gap: 8px;
      margin-bottom: 20px;
      padding-bottom: 6px;
      overflow-x: auto;
      -webkit-overflow-scrolling: touch;
      scrollbar-width: none;
      white-space: nowrap;
    }
    .tabs::-webkit-scrollbar { display: none; }
    .tab-btn {
      background: var(--card-bg);
      border: 1px solid var(--card-border);
      color: var(--text-muted);
      font-size: 13.5px;
      font-weight: 700;
      padding: 9px 18px;
      cursor: pointer;
      border-radius: 12px;
      transition: all 0.2s ease;
      flex-shrink: 0;
      display: inline-flex;
      align-items: center;
      gap: 6px;
      user-select: none;
    }
    .tab-btn:hover {
      color: var(--text);
      border-color: var(--primary);
    }
    .tab-btn.active {
      background: linear-gradient(135deg, var(--primary), #00BFA5);
      border-color: transparent;
      color: #FFF;
      box-shadow: 0 4px 14px rgba(0, 131, 143, 0.45);
    }

    /* Cards */
    .grid-2 {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 20px;
      margin-bottom: 24px;
    }
    @media (max-width: 900px) { .grid-2 { grid-template-columns: 1fr; } }

    .card {
      background: var(--card-bg);
      border: 1px solid var(--card-border);
      border-radius: 18px;
      padding: 22px;
      box-shadow: var(--card-shadow);
      transition: background 0.3s ease, border-color 0.3s ease, box-shadow 0.3s ease;
    }
    .card-title {
      font-size: 16px;
      font-weight: 800;
      color: var(--text-title);
      margin-bottom: 16px;
      display: flex;
      align-items: center;
      gap: 8px;
      transition: color 0.3s ease;
    }
    .form-group { margin-bottom: 14px; }
    label { display: block; font-size: 12.5px; font-weight: 700; color: var(--text-muted); margin-bottom: 6px; }
    input, select, textarea {
      width: 100%;
      padding: 11px 13px;
      border-radius: 10px;
      border: 1px solid var(--input-border);
      background: var(--input-bg);
      color: var(--text);
      font-size: 14px;
      outline: none;
      transition: border-color 0.2s, background 0.3s ease, color 0.3s ease;
    }
    input:focus, select:focus, textarea:focus { border-color: var(--primary-light); }
    .btn {
      width: 100%;
      padding: 12px;
      border: none;
      border-radius: 10px;
      background: linear-gradient(135deg, var(--primary), #00BFA5);
      color: white;
      font-size: 14px;
      font-weight: 800;
      cursor: pointer;
      transition: all 0.2s;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
    }
    .btn:hover { opacity: 0.92; transform: translateY(-1px); }
    .btn:active { transform: translateY(0); }
    .btn-outline {
      background: transparent;
      border: 1px solid var(--card-border);
      color: var(--text);
    }
    .btn-outline:hover {
      background: rgba(0, 150, 166, 0.1);
      border-color: var(--primary);
    }

    .key-box {
      margin-top: 16px;
      background: var(--input-bg);
      border: 1px dashed var(--accent);
      border-radius: 12px;
      padding: 14px;
      text-align: center;
      display: none;
      transition: background 0.3s ease;
    }
    .key-text {
      font-size: 20px;
      font-weight: 900;
      letter-spacing: 2px;
      color: var(--success);
      margin: 6px 0 10px;
      user-select: all;
    }

    /* Tables */
    .table-container {
      overflow-x: auto;
      border-radius: 14px;
      border: 1px solid var(--card-border);
      -webkit-overflow-scrolling: touch;
    }
    table {
      width: 100%;
      border-collapse: collapse;
      text-align: right;
      background: var(--card-bg);
      transition: background 0.3s ease;
      min-width: 650px;
    }
    th, td {
      padding: 12px 14px;
      border-bottom: 1px solid var(--table-border);
      font-size: 13.5px;
    }
    th {
      background: var(--table-th-bg);
      color: var(--text-muted);
      font-weight: 700;
      white-space: nowrap;
      transition: background 0.3s ease, color 0.3s ease;
    }
    tr:hover { background: var(--table-hover); }
    .badge {
      padding: 4px 8px;
      border-radius: 6px;
      font-size: 11.5px;
      font-weight: 700;
      display: inline-block;
      white-space: nowrap;
    }
    .badge-active { background: rgba(0, 230, 118, 0.15); color: var(--success); }
    .badge-trial { background: rgba(255, 179, 0, 0.15); color: var(--accent); }
    .badge-blocked { background: rgba(255, 82, 82, 0.15); color: var(--danger); }
    .badge-monthly { background: rgba(0, 184, 212, 0.15); color: #00E5FF; }
    .badge-yearly { background: rgba(255, 179, 0, 0.15); color: var(--accent); }
    .btn-sm {
      padding: 5px 10px;
      font-size: 11.5px;
      border-radius: 6px;
      cursor: pointer;
      border: none;
      font-weight: 700;
      transition: 0.2s;
      white-space: nowrap;
    }
    .btn-block { background: rgba(255, 82, 82, 0.15); color: var(--danger); border: 1px solid var(--danger); }
    .btn-block:hover { background: var(--danger); color: white; }
    .btn-unblock { background: rgba(0, 230, 118, 0.15); color: var(--success); border: 1px solid var(--success); }
    .btn-unblock:hover { background: var(--success); color: black; }

    .rating-stars { color: var(--accent); letter-spacing: 2px; }
    .toolbar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 14px;
      gap: 10px;
      flex-wrap: wrap;
    }
    .search-input {
      width: 260px;
      padding: 9px 12px;
      font-size: 13px;
    }
    .ops-bar-container {
      width: 130px;
      background: rgba(255, 255, 255, 0.08);
      border-radius: 6px;
      height: 8px;
      overflow: hidden;
      margin-top: 5px;
      border: 1px solid rgba(255, 255, 255, 0.1);
    }
    .ops-bar-fill {
      height: 100%;
      border-radius: 6px;
      transition: width 0.3s ease;
    }
    .pill-filter {
      display: flex;
      gap: 7px;
      flex-wrap: wrap;
      margin-bottom: 14px;
    }
    .pill-filter-btn {
      padding: 6px 14px;
      border-radius: 20px;
      border: 1px solid var(--card-border);
      background: var(--input-bg);
      color: var(--text-muted);
      font-size: 12px;
      font-weight: 700;
      cursor: pointer;
      transition: all 0.2s ease;
    }
    .pill-filter-btn:hover {
      border-color: var(--primary);
      color: var(--text);
    }
    .pill-filter-btn.active {
      background: linear-gradient(135deg, var(--primary), #00BFA5);
      color: white;
      border-color: transparent;
      box-shadow: 0 2px 8px rgba(0, 131, 143, 0.35);
    }
    .policy-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 13px 15px;
      background: var(--input-bg);
      border: 1px solid var(--card-border);
      border-radius: 12px;
      margin-bottom: 12px;
      gap: 12px;
    }
    .policy-info h4 {
      font-size: 13.5px;
      font-weight: 700;
      color: var(--text-title);
      margin-bottom: 3px;
    }
    .policy-info p {
      font-size: 11.5px;
      color: var(--text-muted);
      line-height: 1.5;
    }
    .switch {
      position: relative;
      display: inline-block;
      width: 48px;
      height: 26px;
      flex-shrink: 0;
    }
    .switch input { opacity: 0; width: 0; height: 0; }
    .slider {
      position: absolute; cursor: pointer;
      top: 0; left: 0; right: 0; bottom: 0;
      background-color: #475569;
      transition: .3s;
      border-radius: 26px;
    }
    .slider:before {
      position: absolute; content: "";
      height: 20px; width: 20px;
      left: 3px; bottom: 3px;
      background-color: white;
      transition: .3s;
      border-radius: 50%;
    }
    input:checked + .slider { background-color: var(--success); }
    input:checked + .slider:before { transform: translateX(-22px); }

    .btn-action-group {
      display: flex;
      gap: 5px;
      flex-wrap: wrap;
    }

    footer {
      text-align: center;
      margin-top: 36px;
      font-size: 12.5px;
      color: var(--text-muted);
    }

    /* Mobile First Media Queries */
    @media (max-width: 768px) {
      body { padding: 12px 10px; }
      header {
        flex-direction: column;
        align-items: stretch;
        gap: 12px;
        margin-bottom: 16px;
        padding-bottom: 14px;
      }
      .brand { justify-content: flex-start; }
      h1 { font-size: 19px; }
      .header-actions {
        justify-content: space-between;
        width: 100%;
      }
      .stats-grid {
        grid-template-columns: repeat(2, 1fr);
        gap: 10px;
        margin-bottom: 16px;
      }
      .stat-card {
        padding: 12px 14px;
      }
      .stat-info .num { font-size: 22px; }
      .stat-icon { width: 38px; height: 38px; font-size: 18px; }
      .card { padding: 16px; border-radius: 14px; }
      .toolbar {
        flex-direction: column;
        align-items: stretch;
      }
      .search-input { width: 100%; }
      .toolbar-buttons {
        display: flex;
        gap: 8px;
        width: 100%;
      }
      .toolbar-buttons .btn {
        flex: 1;
        font-size: 12.5px;
        padding: 8px 10px;
      }
    }
  </style>
</head>
<body>

<!-- Royal Security Authentication Modal / Screen -->
<div id="authScreenOverlay" style="position: fixed; inset: 0; z-index: 999999; background: radial-gradient(circle at center, #132A32 0%, #070E12 100%); display: flex; align-items: center; justify-content: center; padding: 20px;">
  <div style="background: rgba(18, 35, 43, 0.95); backdrop-filter: blur(16px); -webkit-backdrop-filter: blur(16px); border: 1px solid rgba(0, 188, 212, 0.35); border-radius: 20px; box-shadow: 0 20px 60px rgba(0,0,0,0.8), 0 0 40px rgba(0, 188, 212, 0.15); width: 100%; max-width: 460px; padding: 36px 30px; text-align: center; color: #E0F2F1;">
    <div style="display: flex; justify-content: center; margin-bottom: 16px;">
      <div style="width: 52px; height: 52px; border-radius: 14px; background: linear-gradient(135deg, #00838F, #004D40); display: flex; align-items: center; justify-content: center; color: #80DEEA; border: 1px solid rgba(0, 188, 212, 0.3);">
        <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"></path>
        </svg>
      </div>
    </div>
    <h2 style="font-size: 22px; font-weight: 800; color: #FFF; margin: 0 0 6px 0;">بوابة الدخول المحمية • مُسَرِّب</h2>
    <p style="font-size: 13px; color: #80CBC4; margin: 0 0 24px 0; line-height: 1.6;">لوحة الإدارة مشفرة بحماية أمنية (Bcrypt + Rate-Limit). لا يُسمح بالدخول إلا لمسؤولي النظام المعتمدين.</p>
    
    <div id="authErrorBanner" style="display: none; background: rgba(229, 57, 53, 0.15); border: 1px solid #E53935; color: #FFCDD2; font-size: 12.5px; border-radius: 10px; padding: 10px; margin-bottom: 18px; text-align: right; line-height: 1.5;"></div>

    <form id="secureLoginForm" onsubmit="handleSecureLogin(event)" style="text-align: right;">
      <input type="hidden" id="loginCsrfToken" value="">
      
      <div class="form-group" style="margin-bottom: 16px;">
        <label for="adminEmailInput" style="font-size: 13px; color: #B2DFDB; font-weight: 700; margin-bottom: 6px; display: block;">البريد الإلكتروني المعتمد:</label>
        <div style="position: relative;">
          <input type="email" id="adminEmailInput" placeholder="admin@mosarib.com" required style="width: 100%; padding: 12px 14px; background: rgba(9, 18, 23, 0.85); border: 1px solid rgba(0, 150, 166, 0.4); border-radius: 10px; color: #FFF; font-size: 14px; font-family: 'Cairo';">
        </div>
      </div>

      <div class="form-group" style="margin-bottom: 22px;">
        <label for="adminPasswordInput" style="font-size: 13px; color: #B2DFDB; font-weight: 700; margin-bottom: 6px; display: block;">كلمة المرور المشفرة:</label>
        <div style="position: relative;">
          <input type="password" id="adminPasswordInput" placeholder="••••••••••••" required style="width: 100%; padding: 12px 14px; background: rgba(9, 18, 23, 0.85); border: 1px solid rgba(0, 150, 166, 0.4); border-radius: 10px; color: #FFF; font-size: 14px; font-family: 'Cairo';">
        </div>
      </div>

      <button type="submit" id="loginSubmitBtn" class="btn" style="width: 100%; padding: 14px; font-size: 14.5px; font-weight: 800; border-radius: 12px; background: linear-gradient(135deg, #00897B, #004D40); box-shadow: 0 6px 20px rgba(0, 137, 123, 0.4); border: 1px solid #4DB6AC; display: flex; align-items: center; justify-content: center; gap: 8px;">
        <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
          <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
        </svg>
        <span>التحقق والدخول الآمن</span>
      </button>
    </form>

    <div style="margin-top: 24px; padding-top: 18px; border-top: 1px solid rgba(255,255,255,0.08); font-size: 11.5px; color: #80CBC4; display: flex; justify-content: space-between; align-items: center;">
      <span>تشفير Argon2id / Bcrypt</span>
      <span>حظر التخمين 15 دقيقة</span>
      <span>م. عمر الحومي</span>
    </div>
  </div>
</div>

<div class="container" id="mainDashboardContainer" style="filter: blur(8px); pointer-events: none; transition: filter 0.3s ease;">
  <!-- Header -->
  <header>
    <div class="brand">
      <div class="brand-icon">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"></path>
        </svg>
      </div>
      <div>
        <h1>مُسَرِّب • لوحة الإدارة</h1>
        <p class="subtitle">إدارة التراخيص والمستخدمين والإشعارات السحابية</p>
      </div>
    </div>
    <div class="header-actions">
      <div id="loggedInUserBadge" style="display: flex; align-items: center; gap: 8px; background: rgba(0, 137, 123, 0.2); border: 1px solid rgba(0, 188, 212, 0.4); border-radius: 20px; padding: 5px 14px; font-size: 12.5px;">
        <span style="color: #4DB6AC;">المسؤول:</span>
        <strong id="currentAdminName" style="color: #FFF;">-</strong>
        <span id="currentAdminRole" class="badge badge-yearly" style="font-size: 10px; padding: 2px 6px;">SUPER_ADMIN</span>
      </div>
      <button class="btn btn-outline" style="width: auto; padding: 6px 14px; font-size: 12px; border-color: rgba(229,57,53,0.4); color: #EF5350;" onclick="handleSecureLogout()">تسجيل الخروج</button>
      <button id="themeToggleBtn" class="theme-toggle-btn" onclick="toggleTheme()" title="تبديل المظهر">
        <span class="theme-icon" id="themeIcon">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="5"></circle>
            <line x1="12" y1="1" x2="12" y2="3"></line>
            <line x1="12" y1="21" x2="12" y2="23"></line>
            <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"></line>
            <line x1="18.36" y1="18.36" x2="19.78" y2="19.78"></line>
            <line x1="1" y1="12" x2="3" y2="12"></line>
            <line x1="21" y1="12" x2="23" y2="12"></line>
            <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"></line>
            <line x1="18.36" y1="5.64" x2="19.78" y2="4.22"></line>
          </svg>
        </span>
        <span id="themeLabel">الوضع الفاتح</span>
      </button>
      <div class="status-badge" id="firebaseStatus" onclick="promptConfigureFirebaseUrl()" style="cursor: pointer;" title="انقر لعرض وفحص وتعديل رابط قاعدة بيانات فايربيس">
        <div class="status-dot"></div> سحابة فايربيس
      </div>
    </div>
  </header>

  <!-- Stats Grid -->
  <div class="stats-grid">
    <div class="stat-card">
      <div class="stat-info">
        <h3>المستخدمون</h3>
        <div class="num" id="statTotalUsers">0</div>
      </div>
      <div class="stat-icon">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
          <circle cx="9" cy="7" r="4"></circle>
          <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
          <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
        </svg>
      </div>
    </div>
    <div class="stat-card">
      <div class="stat-info">
        <h3>المشتركون النشطون</h3>
        <div class="num" id="statActiveLicenses">0</div>
      </div>
      <div class="stat-icon" style="color: var(--success)">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
          <polyline points="22 4 12 14.01 9 11.01"></polyline>
        </svg>
      </div>
    </div>
    <div class="stat-card">
      <div class="stat-info">
        <h3>النسخ التجريبية</h3>
        <div class="num" id="statTrialUsers">0</div>
      </div>
      <div class="stat-icon" style="color: var(--accent)">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10"></circle>
          <polyline points="12 6 12 12 16 14"></polyline>
        </svg>
      </div>
    </div>
    <div class="stat-card" style="border: 1px solid rgba(255, 179, 0, 0.4); cursor: pointer;" onclick="switchTab('requests')">
      <div class="stat-info">
        <h3>طلبات معلقة</h3>
        <div class="num" id="statPendingRequests" style="color: var(--accent);">0</div>
      </div>
      <div class="stat-icon" style="color: var(--accent)">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
          <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
        </svg>
      </div>
    </div>
    <div class="stat-card">
      <div class="stat-info">
        <h3>المحظورون</h3>
        <div class="num" id="statBlockedUsers">0</div>
      </div>
      <div class="stat-icon" style="color: var(--danger)">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10"></circle>
          <line x1="4.93" y1="4.93" x2="19.07" y2="19.07"></line>
        </svg>
      </div>
    </div>
    <div class="stat-card" style="border: 1px solid rgba(0, 188, 212, 0.4); cursor: pointer;" onclick="switchTab('publicPortal')">
      <div class="stat-info">
        <h3>زوار الموقع العام</h3>
        <div class="num" id="statTotalVisitors" style="color: #4DD0E1;">0</div>
        <small id="statTodayVisitorsBadge" style="font-size: 11px; color: var(--text-muted); display: block; margin-top: 2px;">اليوم: 0</small>
      </div>
      <div class="stat-icon" style="color: #00BCD4;">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
          <circle cx="12" cy="12" r="3"></circle>
        </svg>
      </div>
    </div>
    <div class="stat-card" style="border: 1px solid rgba(0, 230, 118, 0.4); cursor: pointer;" onclick="switchTab('publicPortal')">
      <div class="stat-info">
        <h3>تنزيلات التطبيق</h3>
        <div class="num" id="statTotalDownloads" style="color: #69F0AE;">0</div>
        <small id="statTodayDownloadsBadge" style="font-size: 11px; color: var(--text-muted); display: block; margin-top: 2px;">اليوم: 0</small>
      </div>
      <div class="stat-icon" style="color: var(--success);">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
          <polyline points="7 10 12 15 17 10"></polyline>
          <line x1="12" y1="15" x2="12" y2="3"></line>
        </svg>
      </div>
    </div>
  </div>

  <!-- Tabs Navigation -->
  <div class="tabs">
    <button class="tab-btn" id="tabBtnAnalytics" onclick="switchTab('analytics')">المخططات والتحليلات</button>
    <button class="tab-btn" id="tabBtnRequests" onclick="switchTab('requests')">طلبات التفعيل <span id="badgeReqCount" class="badge badge-yearly" style="display:none; margin-right:4px;">0</span></button>
    <button class="tab-btn active" id="tabBtnUsers" onclick="switchTab('users')">المستخدمون</button>
    <button class="tab-btn" id="tabBtnReleases" onclick="switchTab('releases')">الإصدارات والتحميل</button>
    <button class="tab-btn" id="tabBtnPolicies" onclick="switchTab('policies')">سياسات التطبيق</button>
    <button class="tab-btn" id="tabBtnPush" onclick="switchTab('push')">إشعارات Push</button>
    <button class="tab-btn" id="tabBtnBroadcast" onclick="switchTab('broadcast')">إعلانات البث والتحديث</button>
    <button class="tab-btn" id="tabBtnGenerator" onclick="switchTab('generator')">توليد التراخيص</button>
    <button class="tab-btn" id="tabBtnFeedbacks" onclick="switchTab('feedbacks')">الملاحظات والتقييمات</button>
    <button class="tab-btn" id="tabBtnAdmins" onclick="switchTab('admins')">المسؤولون والأمان</button>
    <button class="tab-btn" id="tabBtnPublicPortal" onclick="switchTab('publicPortal')">بوابة الزوار والأسعار</button>
  </div>

  <!-- Tab: Royal Analytics & Interactive Visual Charts -->
  <div id="tab-analytics" style="display: none;">
    <!-- Top KPI Row -->
    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px; margin-bottom: 24px;">
      <div class="stat-card" style="background: linear-gradient(135deg, rgba(0,131,143,0.2), rgba(18,35,43,0.9)); border: 1px solid rgba(0,188,212,0.4);">
        <div class="stat-info">
          <h3 style="color: #4DD0E1;">إجمالي العمليات المسجلة سحابياً</h3>
          <div class="num" id="kpiTotalOperations" style="color: #E0F7FA;">0</div>
          <small style="color: var(--text-muted); font-size: 11px;">مجموع دورات الري + السندات + الآبار</small>
        </div>
        <div class="stat-icon" style="color: #00BCD4;">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"></polygon>
          </svg>
        </div>
      </div>
      <div class="stat-card" style="background: linear-gradient(135deg, rgba(76,175,80,0.15), rgba(18,35,43,0.9)); border: 1px solid rgba(76,175,80,0.4);">
        <div class="stat-info">
          <h3 style="color: #81C784;">نسبة تحويل المشتركين</h3>
          <div class="num" id="kpiConversionRate" style="color: #C8E6C9;">0%</div>
          <small style="color: var(--text-muted); font-size: 11px;">نسبة من دفعوا واشتركوا مقارنة بالإجمالي</small>
        </div>
        <div class="stat-icon" style="color: #4CAF50;">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"></polyline>
            <polyline points="17 6 23 6 23 12"></polyline>
          </svg>
        </div>
      </div>
      <div class="stat-card" style="background: linear-gradient(135deg, rgba(255,179,0,0.15), rgba(18,35,43,0.9)); border: 1px solid rgba(255,179,0,0.4);">
        <div class="stat-info">
          <h3 style="color: #FFD54F;">المستخدمون النشطون (أونلاين)</h3>
          <div class="num" id="kpiOnlineUsers" style="color: #FFF9C4;">0</div>
          <small style="color: var(--text-muted); font-size: 11px;">متصلون الآن خلال آخر 24 ساعة</small>
        </div>
        <div class="stat-icon" style="color: #FFB300;">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M22 12h-4l-3 9L9 3l-3 9H2"></path>
          </svg>
        </div>
      </div>
      <div class="stat-card" style="background: linear-gradient(135deg, rgba(156,39,176,0.15), rgba(18,35,43,0.9)); border: 1px solid rgba(156,39,176,0.4);">
        <div class="stat-info">
          <h3 style="color: #BA68C8;">التحديثات والإصدار الأحدث</h3>
          <div class="num" id="kpiLatestVersion" style="font-size: 20px; color: #F3E5F5;">v1.0.0</div>
          <small style="color: var(--text-muted); font-size: 11px;">إصدار التطبيق المنتشر حالياً</small>
        </div>
        <div class="stat-icon" style="color: #AB47BC;">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path>
            <polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline>
            <line x1="12" y1="22.08" x2="12" y2="12"></line>
          </svg>
        </div>
      </div>
    </div>

    <!-- Charts Grid 1: Subscriptions & Operations Distribution -->
    <div class="grid-2" style="margin-bottom: 24px;">
      <div class="card" style="border: 1px solid rgba(0, 150, 166, 0.35); box-shadow: 0 10px 30px rgba(0,0,0,0.4);">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
          <div class="card-title" style="margin-bottom: 0;">توزيع حالة التراخيص والاشتراكات</div>
          <span style="font-size: 11px; background: rgba(0,188,212,0.15); color: #00BCD4; padding: 4px 8px; border-radius: 6px;">مباشر من فايربيس</span>
        </div>
        <div style="height: 280px; position: relative;">
          <canvas id="chartSubscriptionDistribution"></canvas>
        </div>
      </div>

      <div class="card" style="border: 1px solid rgba(255, 179, 0, 0.35); box-shadow: 0 10px 30px rgba(0,0,0,0.4);">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
          <div class="card-title" style="margin-bottom: 0; color: #FFB300;">استهلاك العمليات في النسخ التجريبية</div>
          <span style="font-size: 11px; background: rgba(255,179,0,0.15); color: #FFB300; padding: 4px 8px; border-radius: 6px;">قرب نفاد التجربة</span>
        </div>
        <div style="height: 280px; position: relative;">
          <canvas id="chartOperationsBucket"></canvas>
        </div>
      </div>
    </div>

    <!-- Charts Grid 2: Timeline Activity & Devices Breakdown -->
    <div class="grid-2">
      <div class="card" style="border: 1px solid rgba(76, 175, 80, 0.35); box-shadow: 0 10px 30px rgba(0,0,0,0.4);">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
          <div class="card-title" style="margin-bottom: 0; color: #81C784;">نشاط وتسجيل المستخدمين الجدد (Timeline)</div>
          <span style="font-size: 11px; background: rgba(76,175,80,0.15); color: #81C784; padding: 4px 8px; border-radius: 6px;">حركة التسجيل اليومية</span>
        </div>
        <div style="height: 280px; position: relative;">
          <canvas id="chartUsersTimeline"></canvas>
        </div>
      </div>

      <div class="card" style="border: 1px solid rgba(171, 71, 188, 0.35); box-shadow: 0 10px 30px rgba(0,0,0,0.4);">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
          <div class="card-title" style="margin-bottom: 0; color: #CE93D8;">انتشار إصدارات هواتف الأندرويد والتطبيق</div>
          <span style="font-size: 11px; background: rgba(171,71,188,0.15); color: #CE93D8; padding: 4px 8px; border-radius: 6px;">Android & App Versions</span>
        </div>
        <div style="height: 280px; position: relative;">
          <canvas id="chartAppVersions"></canvas>
        </div>
      </div>
    </div>
  </div>

  <!-- Tab: Subscription Requests (طلبات الاشتراك الواردة من التطبيق) -->
  <div id="tab-requests" style="display: none;">
    <div class="card">
      <div class="toolbar">
        <div class="card-title" style="margin-bottom: 0;">طلبات الاشتراك الواردة من هواتف العملاء</div>
        <div style="display: flex; gap: 10px;">
          <input type="text" class="search-input" id="requestSearch" placeholder="بحث باسم المشترك، الهاتف، الكود..." oninput="filterRequests()">
          <button class="btn btn-outline" style="width: auto; padding: 8px 16px;" onclick="loadFirebaseData()">تحديث</button>
          <button class="btn btn-outline" style="width: auto; padding: 8px 16px;" onclick="exportRequestsCSV()">تصدير CSV</button>
        </div>
      </div>

      <div class="table-container">
        <table>
          <thead>
            <tr>
              <th>تاريخ الطلب</th>
              <th>اسم المشترك</th>
              <th>رقم الهاتف</th>
              <th>الباقة المطلوبة</th>
              <th>كود الجهاز</th>
              <th>الموديل</th>
              <th>الحالة</th>
              <th>إجراء فوري</th>
            </tr>
          </thead>
          <tbody id="requestsTableBody">
            <tr><td colspan="8" style="text-align: center; color: var(--text-muted);">جاري تحميل طلبات الاشتراك...</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>

  <!-- Tab 1: All Users (Both subscribed & trial, with Google Accounts & Instant Block) -->
  <div id="tab-users">
    <div class="card">
      <div class="toolbar">
        <div class="card-title" style="margin-bottom: 0;">قائمة مستخدمي التطبيق ومراقبة الأجهزة</div>
        <div style="display: flex; gap: 10px;">
          <input type="text" class="search-input" id="userSearch" placeholder="بحث بحساب جوجل، الكود، الموديل..." oninput="filterUsers()">
          <button class="btn btn-outline" style="width: auto; padding: 8px 16px;" onclick="loadFirebaseData()">تحديث</button>
          <button class="btn btn-outline" style="width: auto; padding: 8px 16px;" onclick="exportUsersCSV()">تصدير CSV</button>
        </div>
      </div>

      <!-- Quick Filter Pills -->
      <div class="pill-filter">
        <button class="pill-filter-btn active" id="filterAll" onclick="setUserFilter('ALL')">الكل (<span id="countFilterAll">0</span>)</button>
        <button class="pill-filter-btn" id="filterActive" onclick="setUserFilter('ACTIVE')">المشتركون النشطون (<span id="countFilterActive">0</span>)</button>
        <button class="pill-filter-btn" id="filterTrial" onclick="setUserFilter('TRIAL')">النسخ التجريبية (<span id="countFilterTrial">0</span>)</button>
        <button class="pill-filter-btn" id="filterLocked" onclick="setUserFilter('LOCKED')">تجاوزوا الحد (<span id="countFilterLocked">0</span>)</button>
        <button class="pill-filter-btn" id="filterBackupLocked" onclick="setUserFilter('BACKUP_LOCKED')">معطل نسخهم الاحتياطي (<span id="countFilterBackupLocked">0</span>)</button>
        <button class="pill-filter-btn" id="filterBlocked" onclick="setUserFilter('BLOCKED')">المحظورون (<span id="countFilterBlocked">0</span>)</button>
      </div>

      <div class="table-container">
        <table>
          <thead>
            <tr>
              <th>حساب Google / الاسم</th>
              <th>كود الجهاز والموديل</th>
              <th>سعر الساعة للمسرب</th>
              <th>الحالة</th>
              <th>الباقة</th>
              <th>رصيد العمليات والحد</th>
              <th>النسخ الاحتياطي</th>
              <th>آخر ظهور</th>
              <th>إجراءات التحكم</th>
            </tr>
          </thead>
          <tbody id="usersTableBody">
            <tr><td colspan="9" style="text-align: center; color: var(--text-muted);">جاري تحميل قائمة المستخدمين من السحابة...</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>

  <!-- Tab: Releases & APK Upload Manager -->
  <div id="tab-releases" style="display: none;">
    <div class="grid-2">
      <div class="card">
        <div class="card-title">رفع نسخة APK جديدة وإنشاء رابط تحميل مباشر</div>
        <form id="apkUploadForm" onsubmit="handleApkUpload(event)">
          <div class="form-group">
            <label for="apkFile">اختيار ملف التطبيق (APK):</label>
            <input type="file" id="apkFile" accept=".apk" required style="padding: 10px;">
          </div>
          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
            <div class="form-group">
              <label for="apkVersionName">اسم الإصدار (VersionName):</label>
              <input type="text" id="apkVersionName" placeholder="مثال: 1.0.1" required>
            </div>
            <div class="form-group">
              <label for="apkVersionCode">رقم الإصدار (VersionCode):</label>
              <input type="number" id="apkVersionCode" placeholder="مثال: 2" min="1" required>
            </div>
          </div>
          <div class="form-group">
            <label for="apkReleaseNotes">ملاحظات الإصدار الجديد (الميزات والإصلاحات):</label>
            <textarea id="apkReleaseNotes" rows="3" placeholder="اكتب ما تم إضافته أو إصلاحه في هذا التحديث..."></textarea>
          </div>
          <div class="form-group" style="display: flex; align-items: center; gap: 10px;">
            <input type="checkbox" id="apkForceUpdate" style="width: auto;">
            <label for="apkForceUpdate" style="margin: 0; cursor: pointer; color: var(--danger); font-weight: bold;">
              تفعيل كتحديث إجباري وقفل كافة النسخ الأقدم فوراً في فايربيس
            </label>
          </div>
          <button type="submit" class="btn" id="uploadApkBtn">رفع التطبيق وإنشاء رابط التحميل المباشر</button>
          <div id="uploadProgress" style="display: none; margin-top: 14px; text-align: center;">
            <div style="font-size: 13px; color: var(--accent);">جاري رفع ملف الـ APK وتثبيته على السيرفر...</div>
          </div>
        </form>

        <hr style="border: 0; border-top: 1px solid var(--border); margin: 24px 0;">

        <div class="card-title" style="font-size: 15px;">أو ربط رابط تحميل خارجي مباشر (Google Drive / MediaFire)</div>
        <div class="form-group">
          <label for="externalApkUrl">رابط التحميل المباشر:</label>
          <input type="url" id="externalApkUrl" placeholder="https://example.com/mosarib.apk">
        </div>
        <button class="btn btn-outline" onclick="setExternalCloudLink()">نشر الرابط الخارجي للمستخدمين</button>
      </div>

      <div class="card">
        <div class="card-title">سجل الإصدارات وروابط التحميل المباشرة</div>
        <div id="releasesListContainer" style="max-height: 480px; overflow-y: auto;">
          <div style="text-align: center; color: var(--text-muted); padding: 24px;">جاري تحميل سجل الإصدارات...</div>
        </div>
      </div>
    </div>
  </div>

  <!-- Tab: Cloud App Policies (إعدادات وسياسات التطبيق السحابية) -->
  <div id="tab-policies" style="display: none;">
    <div class="grid-2">
      <div class="card">
        <div class="card-title">السياسات العامة للنسخ المجانية والتطبيق عن بُعد</div>
        
        <div class="form-group">
          <label for="policyFreeLimit">الحد الأقصى للعمليات المجانية لجميع النسخ غير المشتركة:</label>
          <input type="number" id="policyFreeLimit" value="200" min="5" max="10000">
          <small style="color: var(--text-muted); font-size: 11px;">العدد الذي إذا وصل إليه المستخدم غير المشترك، يُقفل عليه التطبيق ويطالبه بالاشتراك (افتراضياً 200).</small>
        </div>

        <div class="policy-item">
          <div class="policy-info">
            <h4>السماح بالنسخ الاحتياطي للنسخ المجانية</h4>
            <p>عند إيقاف هذا الخيار، يتم تعطيل زر النسخ الاحتياطي والاستعادة لجميع غير المشتركين لمنع تلاعب مسح واستعادة البيانات.</p>
          </div>
          <label class="switch">
            <input type="checkbox" id="policyBackupAllowed" checked>
            <span class="slider"></span>
          </label>
        </div>

        <div class="policy-item">
          <div class="policy-info">
            <h4>السماح بالنسخ السحابي عبر Google Drive</h4>
            <p>إتاحة أو تعطيل النسخ الاحتياطي السحابي عبر حساب Google لغير المشتركين.</p>
          </div>
          <label class="switch">
            <input type="checkbox" id="policyDriveBackupAllowed" checked>
            <span class="slider"></span>
          </label>
        </div>

        <div class="policy-item">
          <div class="policy-info">
            <h4>السماح بتصدير ملفات Excel للنسخ المجانية</h4>
            <p>إتاحة أو تقييد ميزة استخراج كشوفات وتقارير Excel للنسخ التجريبية.</p>
          </div>
          <label class="switch">
            <input type="checkbox" id="policyExcelExportAllowed" checked>
            <span class="slider"></span>
          </label>
        </div>

        <div class="form-group" style="margin-top: 14px;">
          <label for="policyGlobalNote">تنبيه أو إشعار عام يظهر لجميع المستخدمين التجريبيين (اختياري):</label>
          <input type="text" id="policyGlobalNote" placeholder="مثال: خصم 30% على الاشتراك السنوي بمناسبة موسم السقي!">
        </div>

        <button class="btn" onclick="saveAppPolicies()">حفظ وتطبيق السياسات فوراً على السحابة</button>
      </div>

      <div class="card">
        <div class="card-title">مميزات نظام السياسات والتحكم عن بُعد</div>
        <ul style="padding-right: 20px; line-height: 2.2; font-size: 13.5px; color: #B2DFDB;">
          <li><b>تغيير الحد دون الحاجة لتحديث APK:</b> يمكنك خفض الحد إلى 50 أو رفعه إلى 500 في أي ثانية، وسيطبقه هاتف العميل تلقائياً عند اتصاله بالإنترنت!</li>
          <li><b>ردع تحايل مسح البيانات:</b> إذا لاحظت محاولات تفريغ الحسابات واستعادتها لتخطي الحد، عطّل ميزة النسخ الاحتياطي للنسخ المجانية فجأة بنقرة زر واحدة.</li>
          <li><b>تحكم استثنائي لكل جهاز:</b> من خلال جدول "المستخدمون"، يمكنك تخصيص حد أعلى لعميل معين أو قفل النسخ الاحتياطي لجهاز محدد دون التأثير على بقية المستخدمين.</li>
        </ul>

        <div style="margin-top: 18px; padding: 14px; background: var(--input-bg); border-radius: 12px; border: 1px dashed var(--card-border);">
          <div style="font-size: 12.5px; font-weight: 700; color: var(--text-muted); margin-bottom: 6px;">الحالة المسجلة حالياً على السحابة:</div>
          <div id="policyCloudStatus" style="font-size: 13px; color: var(--success); font-weight: bold;">جاري القراءة...</div>
        </div>
      </div>
    </div>
  </div>

  <!-- Tab: Broadcast & Force Update -->
  <div id="tab-broadcast" style="display: none;">
    <!-- Active Broadcast Preview Card -->
    <div class="card" id="currentBroadcastCard" style="margin-bottom: 20px; border: 1px solid var(--accent); display: none;">
      <div class="card-title" style="color: var(--accent); margin-bottom: 8px;">الإعلان أو التحديث النشط حالياً على جميع الهواتف</div>
      <div id="currentBroadcastContent" style="font-size: 13.5px; line-height: 1.8; color: var(--text);"></div>
      <div style="display: flex; gap: 10px; margin-top: 12px;">
        <button class="btn btn-outline" style="width: auto; padding: 6px 14px; font-size: 12.5px; color: var(--danger); border-color: var(--danger);" onclick="cancelBroadcast()">إلغاء وتصفير هذا الإعلان فوراً</button>
      </div>
    </div>

    <div class="grid-2">
      <div class="card">
        <div class="card-title">إرسال إعلان عام أو تفعيل التحديث الإجباري</div>
        <div class="form-group">
          <label for="bcTitle">عنوان الإعلان / التنبيه:</label>
          <input type="text" id="bcTitle" placeholder="مثال: تحديث إجباري هام للإصدار v1.1.0">
        </div>
        <div class="form-group">
          <label for="bcType">نوع الإشعار:</label>
          <select id="bcType" onchange="toggleUpdateInputs()">
            <option value="INFO">رسالة توجيهية / إعلان عام</option>
            <option value="WARNING">تنبيه فني هام</option>
            <option value="UPDATE">تحديث إجباري وقفل النسخ القديمة</option>
          </select>
        </div>
        <div class="form-group" id="versionGroup" style="display: none;">
          <label for="bcMinVersion">الحد الأدنى لرقم الإصدار المسموح به (VersionCode):</label>
          <input type="number" id="bcMinVersion" value="2" min="1">
          <small style="color: var(--text-muted); font-size: 11px;">أي هاتف لديه VersionCode أقل من هذا الرقم سيُقفل عليه التطبيق فوراً ويطالبه بالتحديث.</small>
        </div>
        <div class="form-group">
          <label for="bcMessage">نص الإشعار / رسالة التحديث:</label>
          <textarea id="bcMessage" rows="3" placeholder="اكتب التفاصيل والميزات الجديدة أو التوجيهات..."></textarea>
        </div>
        <div class="form-group">
          <label for="bcUrl">رابط التحديث أو التحميل (اختياري):</label>
          <input type="url" id="bcUrl" placeholder="مثال: https://mosarib.com/download أو رابط واتساب">
        </div>
        <div class="form-group">
          <label for="bcBtnText">نص الزر:</label>
          <input type="text" id="bcBtnText" value="تحديث التطبيق الآن">
        </div>
        <button class="btn" onclick="publishBroadcast()">نشر الإشعار / قفل النسخ القديمة لجميع الهواتف</button>
        <button class="btn btn-outline" style="margin-top: 10px;" onclick="cancelBroadcast()">إلغاء وتصفير الإعلان الحالي</button>
      </div>

      <div class="card">
        <div class="card-title">كيف يعمل التحديث الإجباري والإعلانات؟</div>
        <ul style="padding-right: 20px; line-height: 2.2; font-size: 14px; color: #B2DFDB;">
          <li><b>الإعلان العام (INFO/WARNING):</b> يظهر كنافذة أنيقة للمستخدمين لمرة واحدة فقط عند فتح التطبيق، ولا يمنعهم من العمل.</li>
          <li><b>التحديث الإجباري (Force Update):</b> إذا اخترت "تحديث إجباري" وحددت VersionCode أعلى من النسخ القديمة، فإن أي شخص يفتح النسخة القديمة <b>ستُقفل الشاشة في وجهه تماماً</b> ولا يمكنه الدخول أو فتح الحسابات حتى يحمّل التحديث الجديد عبر الرابط المحدد!</li>
          <li><b>تحكم فوري:</b> يتم تفعيل القفل فوراً لحظة اتصال هاتف العميل بالإنترنت.</li>
        </ul>
      </div>
    </div>
  </div>

  <!-- Tab: Push Notifications -->
  <div id="tab-push" style="display: none;">
    <div class="grid-2">
      <div class="card">
        <div class="card-title">إرسال إشعار فوري لستارة الهاتف (FCM Push)</div>
        <div class="form-group">
          <label for="fcmServerKeyInput">مفتاح خادم FCM (Firebase Cloud Messaging Server Key):</label>
          <input type="password" id="fcmServerKeyInput" placeholder="AAAA... انسخه من Project Settings > Cloud Messaging (اختياري للإرسال المباشر)" onchange="saveFcmServerKey()">
          <small style="color:var(--text-muted); font-size:11px;">إذا تركته فارغاً سيتم بث الإشعار عبر قاعدة البيانات، وإذا وضعته سيرن هاتف المزارع فوراً والتطبيق مغلق!</small>
        </div>
        <div class="form-group">
          <label for="pushTarget">الجهة المستهدفة:</label>
          <select id="pushTarget">
            <option value="ALL">جميع الهواتف والمستخدمين المسجلين</option>
            <option value="TRIAL">مستخدمو النسخة التجريبية فقط (لتشجيع الاشتراك)</option>
            <option value="SUBSCRIBERS">المشتركون المفعّلون فقط</option>
          </select>
        </div>
        <div class="form-group">
          <label for="pushTitle">عنوان الإشعار:</label>
          <input type="text" id="pushTitle" placeholder="مثال: تنبيه هام من إدارة مُسَرِّب">
        </div>
        <div class="form-group">
          <label for="pushBody">نص الإشعار:</label>
          <textarea id="pushBody" rows="3" placeholder="اكتب نص الإشعار الذي سيرن ويظهر في ستارة الهاتف..."></textarea>
        </div>
        <div class="form-group">
          <label for="pushUrl">رابط يفتح عند الضغط على الإشعار (اختياري):</label>
          <input type="url" id="pushUrl" placeholder="مثال: رابط تحديث، أو رابط واتساب للدعم">
        </div>
        <button class="btn" onclick="sendRealFcmPushNotification()">إرسال الإشعار الآن لهواتف المستخدمين</button>
        <div id="pushStatus" style="margin-top: 12px; font-size: 13px; text-align: center; display: none;"></div>
      </div>

      <div class="card">
        <div class="card-title">مميزات إشعارات ستارة الهاتف (FCM)</div>
        <ul style="padding-right: 20px; line-height: 2.2; font-size: 14px; color: #B2DFDB;">
          <li><b>تصل والهاتف مقفل:</b> يرن الهاتف ويهتز ويظهر الإشعار في الستارة العلوية بأيقونة التطبيق حتى لو كان التطبيق مغلقاً تماماً.</li>
          <li><b>إرسال سحابي هجين:</b> يرسل إشعار Google FCM المباشر ويرسله أيضاً لقاعدة بيانات التطبيق الداخلية لضمان وصوله للجميع 100%.</li>
          <li><b>تخصيص كامل:</b> تحكّم بالعنوان، النص، ورابط التحويل المباشر عند النقر.</li>
        </ul>
      </div>
    </div>
  </div>

  <!-- Tab 2: Generator -->
  <div id="tab-generator" style="display: none;">
    <div class="grid-2">
      <div class="card">
        <div class="card-title">توليد رمز تفعيل وقفل لجهاز واحد</div>
        <div class="form-group">
          <label for="deviceCode">كود جهاز المزارع / العميل (Device Code):</label>
          <input type="text" id="deviceCode" placeholder="MSRB-XXXX-XXXX" autocomplete="off">
        </div>
        <div class="form-group">
          <label for="subscriberName">اسم المشترك / المسرّب:</label>
          <input type="text" id="subscriberName" placeholder="مثال: المهندس / الأخ سالم">
        </div>
        <div class="form-group">
          <label for="planSelect">نوع الاشتراك:</label>
          <select id="planSelect">
            <option value="MONTHLY">اشتراك شهري (30 يوماً)</option>
            <option value="YEARLY">اشتراك سنوي (365 يوماً)</option>
          </select>
        </div>
        <button class="btn" onclick="generateAndSaveKey()">توليد وحفظ في سحابة فايربيس</button>

        <div class="key-box" id="keyBox">
          <div style="font-size: 13px; color: var(--accent); font-weight: bold;">رمز التفعيل المقفل لهذا الهاتف:</div>
          <div class="key-text" id="keyText">ACTV-M-XXXX-XXXX</div>
          <div style="display: flex; gap: 8px;">
            <button class="btn btn-outline" style="flex: 1;" onclick="copyKey()">نسخ الرمز</button>
            <button class="btn" style="flex: 1; background: #25D366;" onclick="sendToWhatsApp()">إرسال عبر واتساب</button>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-title">حماية متقدمة ومكافحة التحايل</div>
        <ul style="padding-right: 20px; line-height: 2.2; font-size: 14px; color: #B2DFDB;">
          <li><b>تسجيل كافة المستخدمين:</b> يظهر في لوحتك كل من قام بفتح التطبيق بحساب Google ومعرف جهازه وعدد عملياته، حتى وإن لم يشترك بعد!</li>
          <li><b>إيقاف فوري بنقرة واحدة:</b> إذا لاحظت أن شخصاً يتلاعب أو يكرر مسح البيانات، يمكنك إيقاف التطبيق عليه وحظره فوراً برقم جهازه أو إيميله.</li>
          <li><b>قفل عتادي:</b> الكود يرتبط ببصمة الهاتف ولا يمكن تشغيله على هاتفين إطلاقاً.</li>
        </ul>
      </div>
    </div>
  </div>

  <!-- Tab 3: Feedbacks -->
  <div id="tab-feedbacks" style="display: none;">
    <div class="card">
      <div class="toolbar">
        <div class="card-title" style="margin-bottom: 0;">آراء وملاحظات المستخدمين الواردة من التطبيق</div>
        <div style="display: flex; gap: 10px;">
          <input type="text" class="search-input" id="feedbackSearch" placeholder="بحث بالرسائل أو الأسماء..." oninput="filterFeedbacks()">
          <button class="btn btn-outline" style="width: auto; padding: 8px 16px;" onclick="loadFirebaseData()">تحديث</button>
          <button class="btn btn-outline" style="width: auto; padding: 8px 16px;" onclick="exportFeedbacksCSV()">تصدير CSV</button>
        </div>
      </div>

      <div class="table-container">
        <table>
          <thead>
            <tr>
              <th>تاريخ الإرسال</th>
              <th>المرسل</th>
              <th>رقم الهاتف</th>
              <th>التقييم</th>
              <th>نوع المشاركة</th>
              <th>نص الرسالة / الملاحظة</th>
              <th>الجهاز</th>
              <th>إجراء</th>
            </tr>
          </thead>
          <tbody id="feedbacksTableBody">
            <tr><td colspan="8" style="text-align: center; color: var(--text-muted);">جاري تحميل الملاحظات...</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>

  <!-- Tab: System Admins & Security Management -->
  <div id="tab-admins" style="display: none;">
    <div class="grid-2">
      <div class="card" style="border: 1px solid rgba(0, 188, 212, 0.4);">
        <div class="card-title">إضافة مسؤول نظام جديد</div>
        <p style="font-size: 12.5px; color: var(--text-muted); margin-bottom: 16px;">
          لن يتمكن أي شخص من فتح الموقع أو التحكم بالتطبيق إلا إذا كان مسجلاً بالاسم والبريد وكلمة المرور المشفرة.
        </p>
        <form id="newAdminForm" onsubmit="handleCreateAdmin(event)">
          <div class="form-group">
            <label for="newAdminName">اسم المسؤول بالكامل:</label>
            <input type="text" id="newAdminName" placeholder="مثال: المهندس أحمد" required>
          </div>
          <div class="form-group">
            <label for="newAdminEmail">البريد الإلكتروني المعتمد:</label>
            <input type="email" id="newAdminEmail" placeholder="ahmed@example.com" required>
          </div>
          <div class="form-group">
            <label for="newAdminPassword">كلمة المرور (8 خانات على الأقل):</label>
            <input type="password" id="newAdminPassword" placeholder="••••••••••••" minlength="8" required>
          </div>
          <div class="form-group">
            <label for="newAdminRole">الصلاحية والرتبة الإدارية:</label>
            <select id="newAdminRole">
              <option value="ADMIN">مسؤول إدارة وتراخيص (Admin)</option>
              <option value="SUPER_ADMIN">مسؤول رئيسي بصلاحيات مطلقة (Super Admin)</option>
              <option value="MODERATOR">مشرف متابعة وقراءة فقط (Moderator)</option>
            </select>
          </div>
          <button type="submit" class="btn" id="createAdminBtn" style="background: linear-gradient(135deg, #00897B, #004D40);">
            إنشاء وتأمين حساب المسؤول
          </button>
        </form>
      </div>

      <div class="card" style="border: 1px solid rgba(0, 150, 166, 0.3);">
        <div class="card-title">قائمة المسؤولين المعتمدين</div>
        <div id="adminsListContainer" style="max-height: 480px; overflow-y: auto;">
          <div style="text-align: center; color: var(--text-muted); padding: 24px;">جاري تحميل مسؤولي النظام...</div>
        </div>
      </div>
    </div>
  </div>

  <!-- Tab: Public Portal Management (التحكم بأسعار المياه وأخبار وزوار الموقع العام) -->
  <div id="tab-publicPortal" style="display: none;">
    <!-- Real Site Visitors & APK Downloads KPI Row -->
    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 16px; margin-bottom: 22px;">
      <div class="stat-card" style="background: linear-gradient(135deg, rgba(0, 188, 212, 0.12), rgba(18, 35, 43, 0.95)); border: 1px solid rgba(0, 188, 212, 0.45); padding: 18px;">
        <div class="stat-info">
          <h3 style="color: #4DD0E1; font-size: 13.5px; font-weight: 700;">إجمالي زوار صفحة الزوار (بيانات حقيقية)</h3>
          <div class="num" id="portalKpiTotalVisitors" style="color: #E0F7FA; font-size: 28px; margin: 4px 0;">0</div>
          <small id="portalKpiTodayVisitors" style="color: #80CBC4; font-size: 12px; font-weight: 600;">زوار اليوم: 0 زائر فريد</small>
        </div>
        <div class="stat-icon" style="color: #00BCD4; width: 44px; height: 44px;">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
            <circle cx="12" cy="12" r="3"></circle>
          </svg>
        </div>
      </div>

      <div class="stat-card" style="background: linear-gradient(135deg, rgba(0, 230, 118, 0.12), rgba(18, 35, 43, 0.95)); border: 1px solid rgba(0, 230, 118, 0.45); padding: 18px;">
        <div class="stat-info">
          <h3 style="color: #69F0AE; font-size: 13.5px; font-weight: 700;">عدد تنزيلات التطبيق من الموقع (APK)</h3>
          <div class="num" id="portalKpiTotalDownloads" style="color: #E8F5E9; font-size: 28px; margin: 4px 0;">0</div>
          <small id="portalKpiTodayDownloads" style="color: #A5D6A7; font-size: 12px; font-weight: 600;">تنزيلات اليوم: 0 تحميل مباشر</small>
        </div>
        <div class="stat-icon" style="color: var(--success); width: 44px; height: 44px;">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
            <polyline points="7 10 12 15 17 10"></polyline>
            <line x1="12" y1="15" x2="12" y2="3"></line>
          </svg>
        </div>
      </div>
    </div>

    <!-- Full Website Content & Customization Control -->
    <form id="portalSettingsForm" onsubmit="handleSavePortalSettings(event)">
      <div class="grid-2">
        <!-- 1. Water Rates & Active Geographic Region -->
        <div class="card" style="border: 1px solid rgba(0, 188, 212, 0.45);">
          <div class="card-title">💧 بورصة أسعار ساعات الري والنطاق الجغرافي</div>
          
          <div class="form-group">
            <label for="pActiveRegionName">اسم المنطقة/المحافظات التي يعمل فيها التطبيق حالياً:</label>
            <input type="text" id="pActiveRegionName" placeholder="مثال: الجمهورية اليمنية (صنعاء، مأرب، الجوف، عمران)" required>
          </div>

          <div style="display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 10px;">
            <div class="form-group">
              <label for="pAvgPrice">متوسط سعر الساعة:</label>
              <input type="number" id="pAvgPrice" placeholder="7500" required>
            </div>
            <div class="form-group">
              <label for="pMinPrice">أقل سعر مسجل:</label>
              <input type="number" id="pMinPrice" placeholder="5500" required>
            </div>
            <div class="form-group">
              <label for="pMaxPrice">أعلى سعر مسجل:</label>
              <input type="number" id="pMaxPrice" placeholder="9500" required>
            </div>
          </div>

          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
            <div class="form-group">
              <label for="pCurrency">العملة المعتمدة:</label>
              <input type="text" id="pCurrency" value="ريال" required>
            </div>
            <div class="form-group">
              <label for="pWaterStatus">حالة منسوب المياه:</label>
              <select id="pWaterStatus">
                <option value="ABUNDANT">وفيرة ومرتفعة المنسوب</option>
                <option value="GOOD" selected>متوفرة ومستقرة</option>
                <option value="MODERATE">متوسطة وتستدعي الترشيد</option>
                <option value="SCARCE">شحيحة وطلب متزايد</option>
              </select>
            </div>
          </div>

          <div class="form-group">
            <label for="pRatesNote">ملاحظة وتوضيح الأسعار في الموقع:</label>
            <textarea id="pRatesNote" rows="2" placeholder="الأسعار تختلف بحسب عمق البئر، حجم المضخة، ونوع الطاقة المستخدمة (شمسية / ديزل)."></textarea>
          </div>

          <div class="form-group">
            <label for="pHeadlineAlert">الشريط الإخباري المتحرك أعلى الموقع للزوار:</label>
            <input type="text" id="pHeadlineAlert" placeholder="تنبيه أو إشعار يظهر في الشريط الإخباري العلوي">
          </div>
        </div>

        <!-- 2. Hero Content & Direct Contacts -->
        <div class="card" style="border: 1px solid rgba(0, 150, 166, 0.45);">
          <div class="card-title">📝 نصوص وهوية الواجهة الرئيسية والتواصل</div>

          <div class="form-group">
            <label for="pHeroBadge">شارة الواجهة العلوية:</label>
            <input type="text" id="pHeroBadge" placeholder="المنظومة الرقمية المعتمدة لملاك الآبار والموزعين">
          </div>

          <div class="form-group">
            <label for="pHeroTitle">العنوان الرئيسي للموقع:</label>
            <input type="text" id="pHeroTitle" placeholder="إدارة جلسات السقي وحسابات مياه الآبار بدقة واحترافية" required>
          </div>

          <div class="form-group">
            <label for="pHeroSubtitle">الوصف التعريفي بالمنظومة والتطبيق:</label>
            <textarea id="pHeroSubtitle" rows="3" placeholder="نظام ميداني متكامل يعمل بدون إنترنت لموزعي المياه وملاك الآبار الارتوازية..." required></textarea>
          </div>

          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
            <div class="form-group">
              <label for="pContactWhatsapp">رقم الواتساب للتفعيل والتواصل:</label>
              <input type="text" id="pContactWhatsapp" placeholder="967773712030" required>
            </div>
            <div class="form-group">
              <label for="pContactPhone">رقم الاتصال المباشر:</label>
              <input type="text" id="pContactPhone" placeholder="773712030" required>
            </div>
          </div>

          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
            <div class="form-group">
              <label for="pDeveloperName">اسم المطور / المالك:</label>
              <input type="text" id="pDeveloperName" value="المهندس عمر الحومي">
            </div>
            <div class="form-group">
              <label for="pDeveloperCompany">اسم المؤسسة / الشركة:</label>
              <input type="text" id="pDeveloperCompany" value="تُبع سوفت للحلول البرمجية الذكية (TubbaSoft)">
            </div>
          </div>

          <div class="form-group">
            <label for="pDeveloperEmail">البريد الإلكتروني الرسمي:</label>
            <input type="email" id="pDeveloperEmail" value="omar@tubbasoft.com">
          </div>
        </div>
      </div>

      <!-- 3. Section Visibility Switches -->
      <div class="card" style="margin-bottom: 24px; border: 1px solid rgba(0, 200, 83, 0.4);">
        <div class="card-title">👁️ التحكم بإظهار وإخفاء أقسام الموقع العام (تشغيل / إيقاف)</div>
        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; padding: 10px 0;">
          <label style="display: flex; align-items: center; gap: 10px; cursor: pointer; color: #FFF; font-weight: 700;">
            <input type="checkbox" id="pShowTopTicker" style="width: 20px; height: 20px; accent-color: var(--primary);" checked>
            <span>الشريط الإخباري العلوي</span>
          </label>
          <label style="display: flex; align-items: center; gap: 10px; cursor: pointer; color: #FFF; font-weight: 700;">
            <input type="checkbox" id="pShowRatesSection" style="width: 20px; height: 20px; accent-color: var(--primary);" checked>
            <span>قسم أسعار ساعات الري</span>
          </label>
          <label style="display: flex; align-items: center; gap: 10px; cursor: pointer; color: #FFF; font-weight: 700;">
            <input type="checkbox" id="pShowFeaturesSection" style="width: 20px; height: 20px; accent-color: var(--primary);" checked>
            <span>قسم مميزات التطبيق</span>
          </label>
          <label style="display: flex; align-items: center; gap: 10px; cursor: pointer; color: #FFF; font-weight: 700;">
            <input type="checkbox" id="pShowPlansSection" style="width: 20px; height: 20px; accent-color: var(--primary);" checked>
            <span>قسم خطط وتراخيص التطبيق</span>
          </label>
          <label style="display: flex; align-items: center; gap: 10px; cursor: pointer; color: #FFF; font-weight: 700;">
            <input type="checkbox" id="pShowDeveloperSection" style="width: 20px; height: 20px; accent-color: var(--primary);" checked>
            <span>قسم المطور والتواصل</span>
          </label>
          <label style="display: flex; align-items: center; gap: 10px; cursor: pointer; color: #FFF; font-weight: 700;">
            <input type="checkbox" id="pShowDownloadCta" style="width: 20px; height: 20px; accent-color: var(--primary);" checked>
            <span>صندوق التحميل المباشر السفلي</span>
          </label>
        </div>

        <div style="margin-top: 18px; display: flex; justify-content: flex-end;">
          <button type="submit" class="btn" id="savePortalSettingsBtn" style="width: auto; padding: 14px 38px; font-size: 15px; font-weight: 800; background: linear-gradient(135deg, #00BFA5, #00838F); box-shadow: 0 4px 18px rgba(0, 191, 165, 0.4);">
            💾 حفظ وتطبيق كافة تعديلات الموقع العام فوراً
          </button>
        </div>
      </div>
    </form>

      <!-- Add News & Articles to Public Portal -->
      <div class="card" style="border: 1px solid rgba(255, 179, 0, 0.4);">
        <div class="card-title">نشر خبر أو تقرير مائي جديد</div>
        <form id="portalNewsForm" onsubmit="handleCreatePortalNews(event)">
          <div class="form-group">
            <label for="newsTitleInput">عنوان الخبر أو التقرير الزراعي:</label>
            <input type="text" id="newsTitleInput" placeholder="مثال: تقرير استقرار أسعار ساعات الري في محافظة صنعاء" required>
          </div>
          <div class="form-group">
            <label for="newsCategoryInput">تصنيف التقرير:</label>
            <select id="newsCategoryInput">
              <option value="تقارير مياه الآبار">تقارير مياه الآبار</option>
              <option value="بورصة أسعار الري">بورصة أسعار الري</option>
              <option value="إرشادات زراعية وفنية">إرشادات زراعية وفنية</option>
              <option value="تحديثات المنظومة">تحديثات المنظومة</option>
            </select>
          </div>
          <div class="form-group">
            <label for="newsSummaryInput">ملخص وتفاصيل التقرير المنشور:</label>
            <textarea id="newsSummaryInput" rows="4" placeholder="اكتب ملخص الخبر الذي سيقرأه الزوار والمزارعون في الصفحة الرئيسية..." required></textarea>
          </div>
          <button type="submit" class="btn" style="background: linear-gradient(135deg, #FFB300, #FF8F00); color: #000; font-weight: 800;">
            نشر التقرير للعامة الآن
          </button>
        </form>

        <hr style="border: 0; border-top: 1px solid var(--border); margin: 20px 0;">

        <div style="font-weight: 800; font-size: 14px; margin-bottom: 12px; color: #FFF;">قائمة الأخبار المنشورة حالياً:</div>
        <div id="adminPortalNewsList" style="max-height: 250px; overflow-y: auto;">
          <div style="text-align: center; color: var(--text-muted); padding: 14px;">جاري تحميل الأخبار...</div>
        </div>
      </div>
    </div>
  </div>

  <footer>
    لوحة الإدارة والتحكم — المهندس عمر الحومي &copy; 2026 | جميع الحقوق محفوظة
  </footer>
</div>

<script>
  let currentAdminSession = null;
  let currentCsrfToken = "";

  async function checkServerSession() {
    try {
      const res = await fetch('api/auth.php?action=check_session');
      const data = await res.json();
      if (data.success && data.data && data.data.admin) {
        currentAdminSession = data.data.admin;
        currentCsrfToken = data.data.csrf_token || "";
        unlockDashboardView(currentAdminSession);
        return true;
      } else {
        currentCsrfToken = (data.data && data.data.csrf_token) ? data.data.csrf_token : "";
        lockDashboardView();
        return false;
      }
    } catch (_) {
      // If deployed in static-only mode without PHP (e.g. preview)
      fetchCsrfToken();
      return false;
    }
  }

  async function fetchCsrfToken() {
    try {
      const res = await fetch('api/auth.php?action=get_csrf');
      const data = await res.json();
      if (data.data && data.data.csrf_token) {
        currentCsrfToken = data.data.csrf_token;
        const el = document.getElementById("loginCsrfToken");
        if (el) el.value = currentCsrfToken;
      }
    } catch (_) {}
  }

  function lockDashboardView() {
    const overlay = document.getElementById("authScreenOverlay");
    const mainCont = document.getElementById("mainDashboardContainer");
    if (overlay) overlay.style.display = "flex";
    if (mainCont) {
      mainCont.style.filter = "blur(10px)";
      mainCont.style.pointerEvents = "none";
    }
    const tokenEl = document.getElementById("loginCsrfToken");
    if (tokenEl && currentCsrfToken) tokenEl.value = currentCsrfToken;
  }

  function unlockDashboardView(admin) {
    const overlay = document.getElementById("authScreenOverlay");
    const mainCont = document.getElementById("mainDashboardContainer");
    if (overlay) overlay.style.display = "none";
    if (mainCont) {
      mainCont.style.filter = "none";
      mainCont.style.pointerEvents = "auto";
    }

    const nameEl = document.getElementById("currentAdminName");
    const roleEl = document.getElementById("currentAdminRole");
    if (nameEl) nameEl.innerText = admin.name || admin.email;
    if (roleEl) roleEl.innerText = admin.role || "ADMIN";

    // Auto-load data upon successful secure login
    loadFirebaseData();
  }

  async function handleSecureLogin(e) {
    e.preventDefault();
    const email = document.getElementById("adminEmailInput").value.trim();
    const password = document.getElementById("adminPasswordInput").value;
    const errorBanner = document.getElementById("authErrorBanner");
    const btn = document.getElementById("loginSubmitBtn");

    errorBanner.style.display = "none";
    btn.disabled = true;
    btn.innerText = "جاري التحقق والتشفير ⏳...";

    try {
      const res = await fetch('api/auth.php?action=login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          email: email,
          password: password,
          csrf_token: currentCsrfToken
        })
      });

      const data = await res.json();
      if (data.success) {
        currentAdminSession = data.data.admin;
        currentCsrfToken = data.data.csrf_token;
        unlockDashboardView(currentAdminSession);
      } else {
        errorBanner.innerText = `⛔ خطأ: ${data.message}`;
        errorBanner.style.display = "block";
      }
    } catch (err) {
      // Local/Demo fallback if opened as pure HTML file without web server
      if (email === "admin@mosarib.com" && password === "Admin@2026!") {
        currentAdminSession = { name: "المهندس عمر الحومي", email: email, role: "SUPER_ADMIN" };
        unlockDashboardView(currentAdminSession);
      } else {
        errorBanner.innerText = "تعذر الاتصال بخادم الحماية. تأكد من إدخال البريد وكلمة المرور الصحيحة.";
        errorBanner.style.display = "block";
      }
    } finally {
      btn.disabled = false;
      btn.innerText = "🔐 التحقق والدخول الآمن للوحة";
    }
  }

  async function handleSecureLogout() {
    if (!confirm("هل أنت متأكد من تسجيل الخروج وقفل اللوحة؟")) return;
    try {
      await fetch('api/auth.php?action=logout');
    } catch (_) {}
    currentAdminSession = null;
    lockDashboardView();
    fetchCsrfToken();
  }

  async function handleCreateAdmin(e) {
    e.preventDefault();
    const name = document.getElementById("newAdminName").value.trim();
    const email = document.getElementById("newAdminEmail").value.trim();
    const password = document.getElementById("newAdminPassword").value;
    const role = document.getElementById("newAdminRole").value;

    const btn = document.getElementById("createAdminBtn");
    btn.disabled = true;

    try {
      const res = await fetch('api/auth.php?action=create_admin', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: name,
          email: email,
          password: password,
          role: role,
          csrf_token: currentCsrfToken
        })
      });

      const data = await res.json();
      if (data.success) {
        alert("✅ " + data.message);
        document.getElementById("newAdminForm").reset();
        loadAdminsList();
      } else {
        alert("⛔ خطأ: " + data.message);
      }
    } catch (err) {
      alert("حدث خطأ: " + err.message);
    } finally {
      btn.disabled = false;
    }
  }

  async function loadAdminsList() {
    const container = document.getElementById("adminsListContainer");
    if (!container) return;
    try {
      const res = await fetch('api/auth.php?action=list_admins');
      const data = await res.json();
      const list = data.data || [];

      if (list.length === 0) {
        container.innerHTML = `<div style="text-align: center; color: var(--text-muted); padding: 20px;">لا يوجد مسؤولون آخرون.</div>`;
        return;
      }

      container.innerHTML = list.map(a => {
        const lastLoginStr = a.last_login ? new Date(a.last_login).toLocaleString('ar-EG') : 'لم يدخل بعد';
        const roleBadge = a.role === 'SUPER_ADMIN' ? '<span class="badge badge-yearly">مدير رئيسي (Super Admin)</span>' : '<span class="badge badge-active">مسؤول (Admin)</span>';
        return `
          <div style="background: var(--input-bg); border: 1px solid var(--border); border-radius: 12px; padding: 14px; margin-bottom: 12px;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
              <strong style="font-size: 14px; color: #FFF;">${a.name}</strong>
              ${roleBadge}
            </div>
            <div style="font-size: 12px; color: #80CBC4; margin-bottom: 6px;">📧 ${a.email}</div>
            <div style="font-size: 11px; color: var(--text-muted);">آخر دخول: ${lastLoginStr} • أضيف بواسطة: ${a.created_by}</div>
          </div>
        `;
      }).join('');
    } catch (e) {
      container.innerHTML = `<div style="text-align: center; color: #EF5350; padding: 20px;">تعذر تحميل قائمة المسؤولين</div>`;
    }
  }

  const FIRESTORE_BASE_URL = "https://firestore.googleapis.com/v1/projects/mosarib/databases/(default)/documents";
  const DEFAULT_RTDB_URL = "https://mosarib-default-rtdb.asia-southeast1.firebasedatabase.app";
  let storedUrl = localStorage.getItem("mosarib_rtdb_url");
  if (!storedUrl || storedUrl.includes("firebaseio.com")) {
    storedUrl = DEFAULT_RTDB_URL;
    localStorage.setItem("mosarib_rtdb_url", storedUrl);
  }
  let RTDB_BASE_URL = storedUrl;
  let FIREBASE_BASE_URL = RTDB_BASE_URL;
  let adminSecretKey = localStorage.getItem("mosarib_admin_secret_key") || "";
  const SECRET_SALT = "mosarib_secure_license_secret_key_alhomi_2026_water_app";

  function getFirebaseUrl(path) {
    const cleanPath = path.replace(/^\/+/, '');
    const sep = cleanPath.includes("?") ? "&" : "?";
    return adminSecretKey ? `${FIREBASE_BASE_URL}/${cleanPath}${sep}auth=${encodeURIComponent(adminSecretKey)}` : `${FIREBASE_BASE_URL}/${cleanPath}`;
  }

  function updateFirebaseStatusIndicator(state, detail = '') {
    const el = document.getElementById("firebaseStatus");
    if (!el) return;
    if (state === 'CONNECTED') {
      el.className = "status-badge status-active";
      el.style.borderColor = "rgba(0, 230, 118, 0.4)";
      el.style.background = "rgba(0, 230, 118, 0.12)";
      el.innerHTML = `<div class="status-dot" style="background:#00E676; box-shadow:0 0 8px #00E676;"></div> متصل بفايربيس`;
      el.title = detail || "تم التحقق والاتصال بنجاح بسحابة فايربيس";
    } else if (state === 'AUTH_REQUIRED') {
      el.className = "status-badge";
      el.style.borderColor = "rgba(255, 179, 0, 0.5)";
      el.style.background = "rgba(255, 179, 0, 0.15)";
      el.innerHTML = `<div class="status-dot" style="background:#FFB300; box-shadow:0 0 8px #FFB300;"></div> مفتاح الصلاحية مطلوب ⚠️`;
      el.title = "قاعدة البيانات تتطلب مفتاح Database Secret أو تعديل القواعد في Firebase Console. انقر هنا للضبط.";
    } else {
      el.className = "status-badge";
      el.style.borderColor = "rgba(239, 83, 80, 0.4)";
      el.style.background = "rgba(239, 83, 80, 0.12)";
      el.innerHTML = `<div class="status-dot" style="background:#EF5350; box-shadow:0 0 8px #EF5350;"></div> تعذر الاتصال بفايربيس ❌`;
      el.title = detail || "تعذر الوصول لقاعدة البيانات. انقر هنا لفحص الرابط.";
    }
  }

  async function firebaseFetch(path, options = {}) {
    const directUrl = getFirebaseUrl(path);
    try {
      const res = await fetch(directUrl, options);
      if (res.status === 401) {
        updateFirebaseStatusIndicator('AUTH_REQUIRED');
      } else if (res.ok) {
        updateFirebaseStatusIndicator('CONNECTED');
      }
      return res;
    } catch (err) {
      // Browser direct fetch failed (e.g. CORS or network error), fallback to server-side PHP proxy
      try {
        const proxyUrl = `api/firebase_proxy.php?path=${encodeURIComponent(path)}${adminSecretKey ? `&auth=${encodeURIComponent(adminSecretKey)}` : ''}`;
        const proxyRes = await fetch(proxyUrl, options);
        if (proxyRes.ok) {
          const proxyJson = await proxyRes.clone().json().catch(() => null);
          if (proxyJson && proxyJson.success) {
            updateFirebaseStatusIndicator('CONNECTED');
          }
        }
        return proxyRes;
      } catch (_) {}
      updateFirebaseStatusIndicator('ERROR');
      throw err;
    }
  }

  function promptConfigureFirebaseUrl() {
    const choice = prompt(
      "⚙️ إعدادات ربط قاعدة بيانات فايربيس (Firebase Settings):\n\n" +
      "1. تعديل رابط Realtime Database\n" +
      "2. إدخال/تعديل مفتاح الأمان (Database Secret)\n" +
      "3. فحص الاتصال وتحديث البيانات الآن\n" +
      "4. عرض إرشادات ضبط قواعد الحماية (Rules)\n\n" +
      "اكتب رقم الخيار (1 أو 2 أو 3 أو 4):",
      "1"
    );

    if (choice === "1") {
      const newUrl = prompt("رابط قاعدة بيانات فايربيس الحالية:\nيمكنك نسخه من Firebase Console:", FIREBASE_BASE_URL);
      if (newUrl !== null && newUrl.trim()) {
        let clean = newUrl.trim().replace(/\/+$/, '');
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) clean = "https://" + clean;
        RTDB_BASE_URL = clean;
        FIREBASE_BASE_URL = clean;
        localStorage.setItem("mosarib_rtdb_url", clean);
        alert("✅ تم حفظ الرابط بنجاح.");
        loadFirebaseData();
      }
    } else if (choice === "2") {
      const newSecret = prompt("أدخل مفتاح الأمان لقاعدة البيانات (Database Secret):\nتنسخه من Firebase Console > Project Settings > Service accounts > Database secrets:", adminSecretKey);
      if (newSecret !== null) {
        adminSecretKey = newSecret.trim();
        localStorage.setItem("mosarib_admin_secret_key", adminSecretKey);
        alert(adminSecretKey ? "✅ تم حفظ مفتاح الصلاحية بنجاح." : "تم مسح مفتاح الصلاحية.");
        loadFirebaseData();
      }
    } else if (choice === "3") {
      loadFirebaseData();
    } else if (choice === "4") {
      alert(
        "💡 لجعل قاعدة بيانات فايربيس تقبل القراءة والكتابة بدون الحاجة لمفتاح سري:\n\n" +
        "1. افتح Firebase Console\n" +
        "2. ادخل إلى Realtime Database > تبويب Rules (القواعد)\n" +
        "3. ضع القواعد التالية:\n\n" +
        '{\n  "rules": {\n    ".read": true,\n    ".write": true\n  }\n}\n\n' +
        "4. اضغط Publish (نشر)."
      );
    }
  }

  let allUsers = [];
  let allFeedbacks = [];
  let allRequests = [];
  let pendingRequestId = null;

  function parseFirestoreDoc(doc) {
    if (!doc || !doc.fields) return doc;
    const out = {};
    for (const [key, val] of Object.entries(doc.fields)) {
      if (val.stringValue !== undefined) out[key] = val.stringValue;
      else if (val.integerValue !== undefined) out[key] = parseInt(val.integerValue, 10);
      else if (val.booleanValue !== undefined) out[key] = val.booleanValue;
      else if (val.doubleValue !== undefined) out[key] = parseFloat(val.doubleValue);
      else if (val.timestampValue !== undefined) out[key] = val.timestampValue;
      else out[key] = val;
    }
    return out;
  }

  async function fetchCollection(name) {
    // 1. First Attempt: Realtime Database with auth and proxy fallback
    try {
      const res = await firebaseFetch(`${name}.json`);
      if (res.ok) {
        const data = await res.json();
        const items = data.data ? data.data : data;
        if (items && typeof items === 'object') return Object.values(items);
      }
    } catch (_) {}

    // 2. Second Attempt: Cloud Firestore REST API
    try {
      const res = await fetch(`${FIRESTORE_BASE_URL}/${name}?pageSize=300`);
      if (res.ok) {
        const data = await res.json();
        if (data && data.documents && Array.isArray(data.documents)) {
          return data.documents.map(parseFirestoreDoc);
        }
      }
    } catch (_) {}

    return [];
  }

  async function sha256(message) {
    const msgBuffer = new TextEncoder().encode(message);
    const hashBuffer = await crypto.subtle.digest('SHA-256', msgBuffer);
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    return hashArray.map(b => b.toString(16).padStart(2, '0')).join('').toUpperCase();
  }

  function toggleUpdateInputs() {
    const isUpdate = document.getElementById("bcType").value === "UPDATE";
    document.getElementById("versionGroup").style.display = isUpdate ? "block" : "none";
    if (isUpdate) {
      document.getElementById("bcBtnText").value = "تحميل التحديث الجديد 📥";
    }
  }

  let allOverrides = {};
  let appConfig = {
    freeOperationsLimit: 200,
    isBackupAllowedForTrial: true,
    isCloudBackupAllowed: true,
    isExcelExportAllowedForTrial: true,
    globalAnnouncementText: ""
  };
  let currentBroadcast = null;
  let currentUserFilter = 'ALL';

  function switchTab(tabId) {
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    ['analytics', 'requests', 'users', 'releases', 'policies', 'push', 'broadcast', 'generator', 'feedbacks', 'admins', 'publicPortal'].forEach(id => {
      const el = document.getElementById(`tab-${id}`);
      if (el) el.style.display = 'none';
    });

    const targetEl = document.getElementById(`tab-${tabId}`);
    if (targetEl) targetEl.style.display = 'block';
    const activeBtn = document.getElementById(`tabBtn${tabId.charAt(0).toUpperCase() + tabId.slice(1)}`);
    if (activeBtn) activeBtn.classList.add('active');

    if (tabId === 'releases') {
      loadReleasesList();
    } else if (tabId === 'analytics') {
      renderAllRoyalCharts();
    } else if (tabId === 'admins') {
      loadAdminsList();
    } else if (tabId === 'publicPortal') {
      loadPortalSettingsForAdmin();
    }
  }

  async function loadPortalSettingsForAdmin() {
    try {
      const res = await fetch('api/public_data.php');
      const json = await res.json();
      if (json.success && json.data) {
        const ps = json.data.portal_settings || {};
        if (document.getElementById("pActiveRegionName")) {
          document.getElementById("pActiveRegionName").value = ps.active_region_name || 'الجمهورية اليمنية (صنعاء، مأرب، الجوف، عمران)';
          document.getElementById("pAvgPrice").value = ps.avg_hourly_price || 7500;
          document.getElementById("pMinPrice").value = ps.min_hourly_price || 5500;
          document.getElementById("pMaxPrice").value = ps.max_hourly_price || 9500;
          document.getElementById("pCurrency").value = ps.currency_symbol || 'ريال';
          document.getElementById("pWaterStatus").value = ps.water_status || 'GOOD';
          document.getElementById("pRatesNote").value = ps.rates_note || '';
          document.getElementById("pHeadlineAlert").value = ps.headline_alert || '';

          document.getElementById("pHeroBadge").value = ps.hero_badge || '';
          document.getElementById("pHeroTitle").value = ps.hero_title || '';
          document.getElementById("pHeroSubtitle").value = ps.hero_subtitle || '';
          document.getElementById("pContactWhatsapp").value = ps.contact_whatsapp || '967773712030';
          document.getElementById("pContactPhone").value = ps.contact_phone || '773712030';
          document.getElementById("pDeveloperName").value = ps.developer_name || 'المهندس عمر الحومي';
          document.getElementById("pDeveloperCompany").value = ps.developer_company || 'تُبع سوفت للحلول البرمجية الذكية (TubbaSoft)';
          document.getElementById("pDeveloperEmail").value = ps.developer_email || 'omar@tubbasoft.com';

          document.getElementById("pShowTopTicker").checked = ps.show_top_ticker !== false;
          document.getElementById("pShowRatesSection").checked = ps.show_rates_section !== false;
          document.getElementById("pShowFeaturesSection").checked = ps.show_features_section !== false;
          document.getElementById("pShowPlansSection").checked = ps.show_plans_section !== false;
          document.getElementById("pShowDeveloperSection").checked = ps.show_developer_section !== false;
          document.getElementById("pShowDownloadCta").checked = ps.show_download_cta !== false;
        }

        const an = json.data.analytics;
        if (an) {
          const totVis = an.total_visitors || 0;
          const todVis = an.today_visitors || 0;
          const totDl = an.total_downloads || 0;
          const todDl = an.today_downloads || 0;

          const elTotVis = document.getElementById("statTotalVisitors");
          const elTodVis = document.getElementById("statTodayVisitorsBadge");
          const elTotDl = document.getElementById("statTotalDownloads");
          const elTodDl = document.getElementById("statTodayDownloadsBadge");
          if (elTotVis) elTotVis.innerText = totVis.toLocaleString('ar-EG');
          if (elTodVis) elTodVis.innerText = `اليوم: ${todVis.toLocaleString('ar-EG')}`;
          if (elTotDl) elTotDl.innerText = totDl.toLocaleString('ar-EG');
          if (elTodDl) elTodDl.innerText = `اليوم: ${todDl.toLocaleString('ar-EG')}`;

          const pTotVis = document.getElementById("portalKpiTotalVisitors");
          const pTodVis = document.getElementById("portalKpiTodayVisitors");
          const pTotDl = document.getElementById("portalKpiTotalDownloads");
          const pTodDl = document.getElementById("portalKpiTodayDownloads");
          if (pTotVis) pTotVis.innerText = totVis.toLocaleString('ar-EG');
          if (pTodVis) pTodVis.innerText = `زوار اليوم: ${todVis.toLocaleString('ar-EG')} زائر فريد`;
          if (pTotDl) pTotDl.innerText = totDl.toLocaleString('ar-EG');
          if (pTodDl) pTodDl.innerText = `تنزيلات اليوم: ${todDl.toLocaleString('ar-EG')} تحميل مباشر`;
        }

        renderAdminPortalNews(json.data.news || []);
      }
    } catch (e) {
      console.error(e);
    }
  }

  function renderAdminPortalNews(news) {
    const cont = document.getElementById("adminPortalNewsList");
    if (!cont) return;
    if (news.length === 0) {
      cont.innerHTML = `<div style="text-align: center; color: var(--text-muted); padding: 14px;">لا توجد أخبار منشورة.</div>`;
      return;
    }

    cont.innerHTML = news.map(item => `
      <div style="background: var(--input-bg); border: 1px solid var(--border); border-radius: 10px; padding: 10px 14px; margin-bottom: 8px; display: flex; justify-content: space-between; align-items: center;">
        <div>
          <b style="font-size: 13.5px; color: #FFF;">${item.title}</b>
          <div style="font-size: 11px; color: var(--text-muted);">${item.category} • ${new Date(item.published_at).toLocaleDateString('ar-EG')}</div>
        </div>
        <button class="btn-sm" style="background: rgba(229,57,53,0.2); color: #EF5350; border: 1px solid #EF5350;" onclick="deletePortalNews('${item.id}')">حذف 🗑️</button>
      </div>
    `).join('');
  }

  async function handleSavePortalSettings(e) {
    e.preventDefault();
    const btn = document.getElementById("savePortalSettingsBtn");
    btn.disabled = true;

    try {
      const payload = {
        action: 'update_settings',
        active_region_name: document.getElementById("pActiveRegionName").value.trim(),
        avg_hourly_price: parseFloat(document.getElementById("pAvgPrice").value) || 0,
        min_hourly_price: parseFloat(document.getElementById("pMinPrice").value) || 0,
        max_hourly_price: parseFloat(document.getElementById("pMaxPrice").value) || 0,
        currency_symbol: document.getElementById("pCurrency").value.trim(),
        water_status: document.getElementById("pWaterStatus").value,
        rates_note: document.getElementById("pRatesNote").value.trim(),
        headline_alert: document.getElementById("pHeadlineAlert").value.trim(),

        hero_badge: document.getElementById("pHeroBadge").value.trim(),
        hero_title: document.getElementById("pHeroTitle").value.trim(),
        hero_subtitle: document.getElementById("pHeroSubtitle").value.trim(),
        contact_whatsapp: document.getElementById("pContactWhatsapp").value.trim(),
        contact_phone: document.getElementById("pContactPhone").value.trim(),
        developer_name: document.getElementById("pDeveloperName").value.trim(),
        developer_company: document.getElementById("pDeveloperCompany").value.trim(),
        developer_email: document.getElementById("pDeveloperEmail").value.trim(),

        show_top_ticker: document.getElementById("pShowTopTicker").checked,
        show_rates_section: document.getElementById("pShowRatesSection").checked,
        show_features_section: document.getElementById("pShowFeaturesSection").checked,
        show_plans_section: document.getElementById("pShowPlansSection").checked,
        show_developer_section: document.getElementById("pShowDeveloperSection").checked,
        show_download_cta: document.getElementById("pShowDownloadCta").checked,
        csrf_token: currentCsrfToken
      };

      const res = await fetch('api/public_data.php', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      const data = await res.json();
      if (data.success) {
        alert("✅ " + data.message);
      } else {
        alert("خطأ: " + data.message);
      }
    } catch (err) {
      alert("تعذر الحفظ: " + err.message);
    } finally {
      btn.disabled = false;
    }
  }

  async function handleCreatePortalNews(e) {
    e.preventDefault();
    const title = document.getElementById("newsTitleInput").value.trim();
    const cat = document.getElementById("newsCategoryInput").value;
    const summary = document.getElementById("newsSummaryInput").value.trim();

    try {
      const res = await fetch('api/public_data.php', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          action: 'add_news',
          title: title,
          category: cat,
          summary: summary,
          csrf_token: currentCsrfToken
        })
      });
      const data = await res.json();
      if (data.success) {
        alert("✅ " + data.message);
        document.getElementById("portalNewsForm").reset();
        loadPortalSettingsForAdmin();
      } else {
        alert("خطأ: " + data.message);
      }
    } catch (err) {
      alert("فشل النشر: " + err.message);
    }
  }

  async function deletePortalNews(id) {
    if (!confirm("هل أنت متأكد من حذف هذا الخبر من واجهة الموقع العام؟")) return;
    try {
      const res = await fetch('api/public_data.php', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          action: 'delete_news',
          id: id,
          csrf_token: currentCsrfToken
        })
      });
      const data = await res.json();
      if (data.success) {
        loadPortalSettingsForAdmin();
      }
    } catch (err) {
      alert("خطأ: " + err.message);
    }
  }

  function saveFcmServerKey() {
    const key = document.getElementById("fcmServerKeyInput").value.trim();
    localStorage.setItem("mosarib_fcm_server_key", key);
  }

  async function sendRealFcmPushNotification() {
    const title = document.getElementById("pushTitle").value.trim();
    const body = document.getElementById("pushBody").value.trim();
    const url = document.getElementById("pushUrl").value.trim();
    const target = document.getElementById("pushTarget").value;
    const serverKey = document.getElementById("fcmServerKeyInput").value.trim() || localStorage.getItem("mosarib_fcm_server_key") || "";
    const statusDiv = document.getElementById("pushStatus");

    if (!title || !body) {
      alert("يرجى كتابة عنوان الإشعار ونصه أولاً");
      return;
    }

    statusDiv.style.display = "block";
    statusDiv.style.color = "var(--accent)";
    statusDiv.innerText = "جاري إرسال الإشعار وبثه عبر Google FCM وسحابة فايربيس...";

    try {
      const res = await fetch('api/fcm_sender.php', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          title: title,
          body: body,
          action_url: url,
          target: target,
          fcm_server_key: serverKey
        })
      });

      const resData = await res.json();
      if (resData.success) {
        statusDiv.style.color = "var(--success)";
        const directMsg = resData.data.fcm_direct_used ? ` (تم الإرسال المباشر لـ ${resData.data.fcm_sent_success} هاتف)` : ' (تم بث الإشعار في السحابة وسيرن للأجهزة عند أول اتصال)';
        statusDiv.innerHTML = `✅ ${resData.message}! إجمالي التوكنات المسجلة: <b>${resData.data.registered_tokens_count}</b>${directMsg}`;
      } else {
        throw new Error(resData.message || 'حدث خطأ غير معروف');
      }
    } catch (e) {
      // Fallback directly to client-side RTDB broadcast
      sendCustomPushNotification();
    }
  }

  async function handleApkUpload(e) {
    e.preventDefault();
    const fileInput = document.getElementById("apkFile");
    if (!fileInput.files || fileInput.files.length === 0) {
      alert("يرجى اختيار ملف APK أولاً");
      return;
    }

    const formData = new FormData();
    formData.append('action', 'upload');
    formData.append('apk_file', fileInput.files[0]);
    formData.append('version_name', document.getElementById("apkVersionName").value.trim());
    formData.append('version_code', document.getElementById("apkVersionCode").value);
    formData.append('release_notes', document.getElementById("apkReleaseNotes").value.trim());
    formData.append('is_force_update', document.getElementById("apkForceUpdate").checked ? '1' : '0');

    const progressDiv = document.getElementById("uploadProgress");
    const btn = document.getElementById("uploadApkBtn");
    btn.disabled = true;
    progressDiv.style.display = "block";

    try {
      const res = await fetch('api/release_manager.php', {
        method: 'POST',
        body: formData
      });
      const data = await res.json();
      if (data.success) {
        alert("✅ " + data.message + "\n\nرابط التحميل المباشر:\n" + data.data.download_url);
        document.getElementById("apkUploadForm").reset();
        loadReleasesList();
        loadFirebaseData();
      } else {
        alert("فشل الرفع: " + data.message);
      }
    } catch (err) {
      alert("خطأ أثناء الرفع: " + err.message);
    } finally {
      btn.disabled = false;
      progressDiv.style.display = "none";
    }
  }

  async function setExternalCloudLink() {
    const url = document.getElementById("externalApkUrl").value.trim();
    if (!url) return alert("يرجى إدخال رابط التحميل المباشر");

    const vCode = prompt("رقم الإصدار (VersionCode) الذي يتضمنه هذا الرابط (مثال: 2):", "2");
    if (!vCode) return;
    const isForce = confirm("هل تريد تفعيل هذا الرابط كتحديث إجباري وقفل النسخ القديمة فوراً؟");
    const notes = prompt("ملاحظات ورسالة التحديث للمستخدمين:", "تحديث جديد متاح لتطبيق مُسَرِّب");

    const formData = new FormData();
    formData.append('action', 'set_cloud_link');
    formData.append('custom_url', url);
    formData.append('version_code', vCode);
    formData.append('is_force_update', isForce ? '1' : '0');
    formData.append('release_notes', notes || '');

    try {
      const res = await fetch('api/release_manager.php', { method: 'POST', body: formData });
      const data = await res.json();
      if (data.success) {
        alert("✅ تم ربط وبث الرابط لجميع المستخدمين في فايربيس بنجاح!");
        loadFirebaseData();
      } else {
        alert("فشل: " + data.message);
      }
    } catch (err) {
      alert("خطأ: " + err.message);
    }
  }

  async function loadReleasesList() {
    const container = document.getElementById("releasesListContainer");
    if (!container) return;
    try {
      const res = await fetch('api/release_manager.php');
      const data = await res.json();
      const releases = (data.data && data.data.releases) ? data.data.releases : [];

      if (releases.length === 0) {
        container.innerHTML = `<div style="text-align: center; color: var(--text-muted); padding: 24px;">لا توجد نسخ APK مرفوعة على السيرفر بعد. يمكنك رفع أول نسخة عبر النموذج على اليمين.</div>`;
        return;
      }

      container.innerHTML = releases.map(r => {
        const sizeMb = (r.file_size / (1024 * 1024)).toFixed(1);
        const dateStr = new Date(r.uploaded_at).toLocaleString('ar-EG');
        return `
          <div style="background: var(--input-bg); border: 1px solid var(--border); border-radius: 12px; padding: 14px; margin-bottom: 12px;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
              <b style="font-size: 15px; color: #FFF;">نسخة: v${r.version_name} (Code: ${r.version_code})</b>
              <span class="badge ${r.is_force_update ? 'badge-blocked' : 'badge-active'}">${r.is_force_update ? 'تحديث إجباري' : 'إصدار اختياري'}</span>
            </div>
            <div style="font-size: 12px; color: var(--text-muted); margin-bottom: 8px;">
              الحجم: ${sizeMb} MB • تاريخ الرفع: ${dateStr}
            </div>
            <div style="font-size: 13px; line-height: 1.6; margin-bottom: 10px; color: var(--text);">
              ${r.release_notes || 'بدون ملاحظات'}
            </div>
            <div style="display: flex; gap: 8px;">
              <input type="text" value="${r.download_url}" readonly style="font-size: 12px; padding: 6px 10px; flex: 1;">
              <button class="btn-sm btn-action" onclick="navigator.clipboard.writeText('${r.download_url}').then(()=>alert('تم نسخ رابط التحميل المباشر: ${r.download_url}'))">📋 نسخ الرابط</button>
              <button class="btn-sm btn-action" onclick="window.open('${r.download_url}', '_blank')">📥 تنزيل</button>
            </div>
          </div>
        `;
      }).join('');
    } catch (_) {
      container.innerHTML = `<div style="text-align: center; color: var(--text-muted); padding: 24px;">سجل الإصدارات سيكون متاحاً بمجرد رفع الموقع إلى السيرفر (PHP Backend).</div>`;
    }
  }

  async function sendCustomPushNotification() {
    const title = document.getElementById("pushTitle").value.trim();
    const body = document.getElementById("pushBody").value.trim();
    const url = document.getElementById("pushUrl").value.trim();
    const target = document.getElementById("pushTarget").value;
    const statusDiv = document.getElementById("pushStatus");

    if (!title || !body) {
      alert("يرجى كتابة عنوان الإشعار ونصه أولاً");
      return;
    }

    statusDiv.style.display = "block";
    statusDiv.style.color = "var(--accent)";
    statusDiv.innerText = "جاري جلب أجهزة المستخدمين وإرسال الإشعار...";

    try {
      let tokens = [];
      try {
        const tokenRes = await firebaseFetch('fcm_tokens.json');
        if (tokenRes.ok) {
          const tokenData = await tokenRes.json();
          const tokenObj = tokenData.data ? tokenData.data : tokenData;
          if (tokenObj && !tokenObj.error) {
            tokens = Object.values(tokenObj);
          }
        }
      } catch (_) {}

      const bcRes = await firebaseFetch('app_broadcast.json', {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          id: "PUSH_" + Date.now(),
          title: title,
          message: body,
          actionUrl: url,
          actionText: "فتح الإشعار",
          type: "INFO",
          publishedAt: Date.now()
        })
      });

      if (!bcRes.ok) {
        throw new Error(`خطأ من Firebase (${bcRes.status}): تأكد من إضافة قواعد app_broadcast`);
      }

      statusDiv.style.color = "var(--success)";
      statusDiv.innerHTML = `✅ تم نشر الإشعار والبث بنجاح إلى قاعدة البيانات ومتاح لجميع الأجهزة فور فتح التطبيق! (الأجهزة المسجلة بالتوكن: <b>${tokens.length}</b>)`;
      loadFirebaseData();
    } catch (e) {
      statusDiv.style.color = "var(--danger)";
      statusDiv.innerText = "فشل الإرسال: " + e.message;
    }
  }

  async function publishBroadcast() {
    const title = document.getElementById("bcTitle").value.trim();
    const type = document.getElementById("bcType").value;
    const msg = document.getElementById("bcMessage").value.trim();
    const url = document.getElementById("bcUrl").value.trim();
    const btnText = document.getElementById("bcBtnText").value.trim();
    const minVer = parseInt(document.getElementById("bcMinVersion").value) || 1;

    if (!title) {
      alert("يرجى كتابة عنوان الإعلان أو التحديث أولاً");
      return;
    }

    const payload = {
      id: "BC_" + Date.now(),
      title: title,
      type: type,
      message: msg,
      actionUrl: url,
      actionText: btnText || "عرض التفاصيل",
      minRequiredVersionCode: minVer,
      isForceUpdate: type === "UPDATE",
      publishedAt: Date.now()
    };

    try {
      const res = await firebaseFetch('app_broadcast.json', {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      });
      if (!res.ok) {
        throw new Error(`رفض السيرفر النشر (${res.status}): يرجى التأكد من قواعد app_broadcast`);
      }
      alert(type === "UPDATE" ? "تم تفعيل التحديث الإجباري وقفل النسخ القديمة بنجاح!" : "تم نشر الإشعار لجميع المستخدمين بنجاح!");
      loadFirebaseData();
    } catch (e) {
      alert("فشل النشر: " + e.message);
    }
  }

  async function cancelBroadcast() {
    if (!confirm("هل تريد إلغاء الإعلان والتحديث الإجباري الحالي؟")) return;
    try {
      await firebaseFetch('app_broadcast.json', { method: "DELETE" });
      alert("تم إلغاء الإعلان بنجاح.");
      loadFirebaseData();
    } catch (e) {
      alert("خطأ: " + e.message);
    }
  }

  async function saveAppPolicies() {
    const freeLimit = parseInt(document.getElementById("policyFreeLimit").value) || 200;
    const backupAllowed = document.getElementById("policyBackupAllowed").checked;
    const driveBackupAllowed = document.getElementById("policyDriveBackupAllowed").checked;
    const excelExportAllowed = document.getElementById("policyExcelExportAllowed").checked;
    const note = document.getElementById("policyGlobalNote").value.trim();

    const payload = {
      freeOperationsLimit: freeLimit,
      isBackupAllowedForTrial: backupAllowed,
      isCloudBackupAllowed: driveBackupAllowed,
      isExcelExportAllowedForTrial: excelExportAllowed,
      globalAnnouncementText: note,
      updatedAt: Date.now()
    };

    try {
      const res = await firebaseFetch('app_config.json', {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      });
      if (!res.ok) throw new Error("رفض السيرفر الحفظ (" + res.status + ")");
      appConfig = payload;
      alert(`✅ تم حفظ السياسات بنجاح في السحابة!\n• الحد المجاني: ${freeLimit} عملية\n• النسخ للمجانيين: ${backupAllowed ? 'مسموح' : 'معطل'}\n• نسخ Drive: ${driveBackupAllowed ? 'مسموح' : 'معطل'}\nستطبق الهواتف هذه السياسات تلقائياً عند فتح التطبيق.`);
      loadFirebaseData();
    } catch (e) {
      alert("فشل حفظ السياسات: " + e.message);
    }
  }

  function renderAppPolicies() {
    if (document.getElementById("policyFreeLimit")) {
      document.getElementById("policyFreeLimit").value = appConfig.freeOperationsLimit || 200;
      document.getElementById("policyBackupAllowed").checked = appConfig.isBackupAllowedForTrial !== false;
      document.getElementById("policyDriveBackupAllowed").checked = appConfig.isCloudBackupAllowed !== false;
      document.getElementById("policyExcelExportAllowed").checked = appConfig.isExcelExportAllowedForTrial !== false;
      document.getElementById("policyGlobalNote").value = appConfig.globalAnnouncementText || "";

      const updatedStr = appConfig.updatedAt ? new Date(appConfig.updatedAt).toLocaleString('ar-EG') : 'القيم الافتراضية';
      document.getElementById("policyCloudStatus").innerHTML = `
        حد العمليات: <b>${appConfig.freeOperationsLimit || 200}</b> • النسخ المجاني: <b>${appConfig.isBackupAllowedForTrial !== false ? 'متاح ✅' : 'معطل ❌'}</b><br>
        <small style="color:var(--text-muted);">آخر تحديث للسياسات: ${updatedStr}</small>
      `;
    }
  }

  function renderActiveBroadcast() {
    const card = document.getElementById("currentBroadcastCard");
    const content = document.getElementById("currentBroadcastContent");
    if (!card || !content) return;

    if (!currentBroadcast || !currentBroadcast.title) {
      card.style.display = "none";
      return;
    }

    const isUpdate = currentBroadcast.type === 'UPDATE' || currentBroadcast.isForceUpdate;
    const pubDate = currentBroadcast.publishedAt ? new Date(currentBroadcast.publishedAt).toLocaleString('ar-EG') : '';
    content.innerHTML = `
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:6px;">
        <span style="font-weight:bold; font-size:15px; color:${isUpdate ? 'var(--danger)' : 'var(--accent)'};">${currentBroadcast.title}</span>
        <span class="badge ${isUpdate ? 'badge-blocked' : 'badge-active'}">${isUpdate ? 'تحديث إجباري وقفل' : 'إعلان توجيهي'}</span>
      </div>
      <div>${currentBroadcast.message || 'بدون تفاصيل'}</div>
      <div style="font-size:12px; color:var(--text-muted); margin-top:6px;">
        ${currentBroadcast.minRequiredVersionCode ? `الحد الأدنى للإصدار المسموح: <b>vCode ${currentBroadcast.minRequiredVersionCode}</b> • ` : ''}
        تاريخ النشر: ${pubDate}
      </div>
    `;
    card.style.display = "block";
  }

  async function loadFirebaseData() {
    try {
      const [reqs, users, fbs] = await Promise.all([
        fetchCollection('subscription_requests'),
        fetchCollection('all_users'),
        fetchCollection('feedbacks')
      ]);

      // Cross-reference with licenses to ensure isActivated is 100% accurate even if user node lagged
      let activeLicenseMap = {};
      try {
        const licRes = await firebaseFetch('licenses.json');
        if (licRes.ok) {
          const licData = await licRes.json();
          const licObj = licData.data ? licData.data : licData;
          if (licObj && typeof licObj === 'object') {
            Object.values(licObj).forEach(lic => {
              if (lic && lic.deviceCode && !lic.isRevoked && (lic.expiresAt > Date.now())) {
                const cleanCode = lic.deviceCode.replace(/-/g, "").trim();
                activeLicenseMap[cleanCode] = lic;
              }
            });
          }
        }
      } catch (_) {}

      // Cross-reference with devices node as well
      try {
        const devRes = await firebaseFetch('devices.json');
        if (devRes.ok) {
          const devData = await devRes.json();
          const devObj = devData.data ? devData.data : devData;
          if (devObj && typeof devObj === 'object') {
            Object.entries(devObj).forEach(([dCode, dVal]) => {
              if (dVal && !dVal.isRevoked && (dVal.expiresAt > Date.now())) {
                const cleanCode = dCode.replace(/-/g, "").trim();
                activeLicenseMap[cleanCode] = dVal;
              }
            });
          }
        }
      } catch (_) {}

      allRequests = reqs;
      allUsers = users.map(u => {
        const devKey = u.cleanDeviceKey || (u.deviceCode ? u.deviceCode.replace(/-/g, "").trim() : "");
        const verifiedLic = activeLicenseMap[devKey];
        if (verifiedLic) {
          return {
            ...u,
            isActivated: true,
            plan: verifiedLic.plan || u.plan || 'MONTHLY',
            subscriberName: u.subscriberName || verifiedLic.subscriberName
          };
        }
        return u;
      });
      allFeedbacks = fbs;

      // Fetch app_config, device_overrides, app_broadcast
      try {
        const cfgRes = await firebaseFetch('app_config.json');
        if (cfgRes.ok) {
          const cfgData = await cfgRes.json();
          const cfgObj = cfgData.data ? cfgData.data : cfgData;
          if (cfgObj && typeof cfgObj === 'object') appConfig = { ...appConfig, ...cfgObj };
        }
      } catch (_) {}

      try {
        const overRes = await firebaseFetch('device_overrides.json');
        if (overRes.ok) {
          const overData = await overRes.json();
          const overObj = overData.data ? overData.data : overData;
          if (overObj && typeof overObj === 'object') allOverrides = overObj;
        }
      } catch (_) {}

      try {
        const bcRes = await firebaseFetch('app_broadcast.json');
        if (bcRes.ok) {
          const bcData = await bcRes.json();
          currentBroadcast = bcData.data ? bcData.data : bcData;
        } else {
          currentBroadcast = null;
        }
      } catch (_) {
        currentBroadcast = null;
      }

      updateStats();
      loadPortalSettingsForAdmin();
      renderAppPolicies();
      renderActiveBroadcast();
      renderRequestsTable(allRequests);
      renderUsersTable(allUsers);
      renderFeedbacksTable(allFeedbacks);
    } catch (e) {
      console.error("Error loading data from Firebase:", e);
    }
  }

  // Chart Instances Registry
  let chartInstances = {};

  function destroyChart(id) {
    if (chartInstances[id]) {
      chartInstances[id].destroy();
      delete chartInstances[id];
    }
  }

  function renderAllRoyalCharts() {
    if (typeof Chart === 'undefined') return;

    // 1. Calculate Real KPIs
    let totalOps = 0;
    let onlineCount = 0;
    const now = Date.now();
    const oneDayAgo = now - 24 * 60 * 60 * 1000;

    let versionsMap = {};
    let activeSubs = 0;
    let trialCount = 0;
    let blockedCount = 0;

    // Operations buckets for trial users
    let bucket0to50 = 0;
    let bucket51to100 = 0;
    let bucket101to150 = 0;
    let bucket151to200 = 0;
    let bucketOverLimit = 0;

    allUsers.forEach(u => {
      const ops = u.operationsCount || 0;
      totalOps += ops;

      if (u.lastSeen && u.lastSeen >= oneDayAgo) {
        onlineCount++;
      }

      if (u.isBlocked) {
        blockedCount++;
      } else if (u.isActivated) {
        activeSubs++;
      } else {
        trialCount++;
        if (ops <= 50) bucket0to50++;
        else if (ops <= 100) bucket51to100++;
        else if (ops <= 150) bucket101to150++;
        else if (ops <= 200) bucket151to200++;
        else bucketOverLimit++;
      }

      const vName = u.appVersion || 'v1.0.0';
      versionsMap[vName] = (versionsMap[vName] || 0) + 1;
    });

    const kpiOpsEl = document.getElementById("kpiTotalOperations");
    if (kpiOpsEl) kpiOpsEl.innerText = totalOps.toLocaleString('ar-EG');

    const conversionRate = allUsers.length > 0 ? ((activeSubs / allUsers.length) * 100).toFixed(1) : 0;
    const kpiConvEl = document.getElementById("kpiConversionRate");
    if (kpiConvEl) kpiConvEl.innerText = `${conversionRate}%`;

    const kpiOnlineEl = document.getElementById("kpiOnlineUsers");
    if (kpiOnlineEl) kpiOnlineEl.innerText = onlineCount;

    const latestVer = Object.keys(versionsMap).sort().pop() || 'v1.0.0';
    const kpiVerEl = document.getElementById("kpiLatestVersion");
    if (kpiVerEl) kpiVerEl.innerText = latestVer;

    // Chart Font & Theme Colors
    const isDark = document.documentElement.getAttribute('data-theme') !== 'light';
    const textColor = isDark ? '#B2DFDB' : '#37474F';
    const gridColor = isDark ? 'rgba(255, 255, 255, 0.08)' : 'rgba(0, 0, 0, 0.06)';

    // 1. Subscription Distribution Donut Chart
    destroyChart('chartSubscriptionDistribution');
    const ctxSub = document.getElementById('chartSubscriptionDistribution');
    if (ctxSub) {
      chartInstances['chartSubscriptionDistribution'] = new Chart(ctxSub, {
        type: 'doughnut',
        data: {
          labels: ['مشتركون مفعّلون ⭐', 'نسخ تجريبية ⏳', 'محظورون ⛔'],
          datasets: [{
            data: [activeSubs, trialCount, blockedCount],
            backgroundColor: ['#00897B', '#FFB300', '#E53935'],
            borderColor: isDark ? '#12232B' : '#FFFFFF',
            borderWidth: 3,
            hoverOffset: 8
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: {
              position: 'bottom',
              labels: { color: textColor, font: { family: 'Cairo', size: 12 } }
            }
          },
          cutout: '68%'
        }
      });
    }

    // 2. Operations Bucket Bar Chart (Trial Consumption)
    destroyChart('chartOperationsBucket');
    const ctxBucket = document.getElementById('chartOperationsBucket');
    if (ctxBucket) {
      chartInstances['chartOperationsBucket'] = new Chart(ctxBucket, {
        type: 'bar',
        data: {
          labels: ['0-50 عملية', '51-100 عملية', '101-150 عملية', '151-200 عملية', 'تجاوز الحد (+200)'],
          datasets: [{
            label: 'عدد المستخدمين التجريبيين',
            data: [bucket0to50, bucket51to100, bucket101to150, bucket151to200, bucketOverLimit],
            backgroundColor: ['#4DB6AC', '#26A69A', '#FFA726', '#FF7043', '#EF5350'],
            borderRadius: 8
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: { display: false }
          },
          scales: {
            x: {
              ticks: { color: textColor, font: { family: 'Cairo', size: 11 } },
              grid: { display: false }
            },
            y: {
              ticks: { color: textColor, font: { family: 'Cairo' }, stepSize: 1 },
              grid: { color: gridColor }
            }
          }
        }
      });
    }

    // 3. User Registration Timeline (Last 7 Days)
    destroyChart('chartUsersTimeline');
    const ctxTimeline = document.getElementById('chartUsersTimeline');
    if (ctxTimeline) {
      const days = [];
      const dayCounts = [];
      for (let i = 6; i >= 0; i--) {
        const d = new Date(now - i * 24 * 60 * 60 * 1000);
        const dayStr = d.toLocaleDateString('ar-EG', { weekday: 'short', day: 'numeric', month: 'numeric' });
        days.push(dayStr);

        const startOfDay = new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
        const endOfDay = startOfDay + 24 * 60 * 60 * 1000;
        const count = allUsers.filter(u => u.firstSeen && u.firstSeen >= startOfDay && u.firstSeen < endOfDay).length;
        dayCounts.push(count);
      }

      chartInstances['chartUsersTimeline'] = new Chart(ctxTimeline, {
        type: 'line',
        data: {
          labels: days,
          datasets: [{
            label: 'المستخدمون الجدد',
            data: dayCounts,
            fill: true,
            borderColor: '#4CAF50',
            backgroundColor: 'rgba(76, 175, 80, 0.15)',
            tension: 0.4,
            pointBackgroundColor: '#81C784',
            pointRadius: 5,
            pointHoverRadius: 7
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: { display: false }
          },
          scales: {
            x: {
              ticks: { color: textColor, font: { family: 'Cairo', size: 11 } },
              grid: { display: false }
            },
            y: {
              ticks: { color: textColor, font: { family: 'Cairo' }, stepSize: 1 },
              grid: { color: gridColor }
            }
          }
        }
      });
    }

    // 4. App Versions Polar / Bar
    destroyChart('chartAppVersions');
    const ctxVer = document.getElementById('chartAppVersions');
    if (ctxVer) {
      const vLabels = Object.keys(versionsMap);
      const vValues = Object.values(versionsMap);

      chartInstances['chartAppVersions'] = new Chart(ctxVer, {
        type: 'bar',
        data: {
          labels: vLabels.length > 0 ? vLabels : ['v1.0.0'],
          datasets: [{
            label: 'عدد الهواتف',
            data: vValues.length > 0 ? vValues : [allUsers.length],
            backgroundColor: ['#AB47BC', '#7E57C2', '#5C6BC0', '#42A5F5', '#26C6DA'],
            borderRadius: 8
          }]
        },
        options: {
          indexAxis: 'y',
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: { display: false }
          },
          scales: {
            x: {
              ticks: { color: textColor, font: { family: 'Cairo' }, stepSize: 1 },
              grid: { color: gridColor }
            },
            y: {
              ticks: { color: textColor, font: { family: 'Cairo', size: 12 } },
              grid: { display: false }
            }
          }
        }
      });
    }
  }

  function updateStats() {
    document.getElementById("statTotalUsers").innerText = allUsers.length;
    const activeLic = allUsers.filter(u => u.isActivated && !u.isBlocked).length;
    const trial = allUsers.filter(u => !u.isActivated && !u.isBlocked).length;
    const blocked = allUsers.filter(u => u.isBlocked).length;
    const pendingReqs = allRequests.filter(r => r.status !== 'COMPLETED').length;

    document.getElementById("statActiveLicenses").innerText = activeLic;
    document.getElementById("statTrialUsers").innerText = trial;
    document.getElementById("statBlockedUsers").innerText = blocked;
    document.getElementById("statPendingRequests").innerText = pendingReqs;

    const badge = document.getElementById("badgeReqCount");
    if (badge) {
      if (pendingReqs > 0) {
        badge.innerText = pendingReqs;
        badge.style.display = "inline-block";
      } else {
        badge.style.display = "none";
      }
    }

    // Update charts if active tab
    const analyticsTab = document.getElementById("tab-analytics");
    if (analyticsTab && analyticsTab.style.display !== 'none') {
      renderAllRoyalCharts();
    }
  }

  function renderRequestsTable(requests) {
    const tbody = document.getElementById("requestsTableBody");
    if (!requests || requests.length === 0) {
      tbody.innerHTML = `<tr><td colspan="8" style="text-align: center; color: var(--text-muted); padding: 24px;">لا توجد طلبات اشتراك واردة حالياً (أي طلب يُرسل من التطبيق سيظهر هنا فوراً)</td></tr>`;
      return;
    }

    requests.sort((a,b) => (b.timestamp || 0) - (a.timestamp || 0));

    tbody.innerHTML = requests.map(r => {
      const dateStr = r.timestamp ? new Date(r.timestamp).toLocaleString('ar-EG') : 'الآن';
      const cleanPhone = (r.phone || '').replace(/[^0-9]/g, '');
      const phoneLink = cleanPhone ? `<a href="https://wa.me/${cleanPhone}" target="_blank" style="color: var(--success); font-weight: bold;">${r.phone} 💬</a>` : '-';
      const planBadge = r.plan === 'YEARLY' ? `<span class="badge badge-yearly">سنوي ⭐</span>` : `<span class="badge badge-monthly">شهري</span>`;
      const statusBadge = r.status === 'COMPLETED' ? `<span class="badge badge-active">تم التفعيل ✅</span>` : `<span class="badge badge-trial">جديد ⏳</span>`;
      const devCode = r.deviceCode || '';

      return `
        <tr>
          <td><small>${dateStr}</small></td>
          <td><b>${r.subscriberName || 'مشترك جديد'}</b></td>
          <td>${phoneLink}</td>
          <td>${planBadge}</td>
          <td><code style="color: var(--accent); font-weight: bold; font-size: 13px;">${devCode}</code></td>
          <td><small>${r.deviceModel || '-'}</small></td>
          <td>${statusBadge}</td>
          <td>
            <button class="btn-sm btn-unblock" onclick="quickActivateRequest('${devCode}', '${(r.subscriberName || '').replace(/'/g, "\\'")}', '${r.plan || 'MONTHLY'}', '${r.id || ''}')">⚡ تفعيل وتوليد الرمز</button>
          </td>
        </tr>
      `;
    }).join('');
  }

  function quickActivateRequest(code, name, plan, reqId) {
    switchTab('generator');
    document.getElementById('deviceCode').value = code;
    document.getElementById('subscriberName').value = name;
    document.getElementById('planSelect').value = plan || 'MONTHLY';
    pendingRequestId = reqId || null;
    generateAndSaveKey();
  }

  function filterRequests() {
    const q = document.getElementById("requestSearch").value.trim().toLowerCase();
    const filtered = allRequests.filter(r => 
      (r.subscriberName && r.subscriberName.toLowerCase().includes(q)) ||
      (r.phone && r.phone.includes(q)) ||
      (r.deviceCode && r.deviceCode.toLowerCase().includes(q))
    );
    renderRequestsTable(filtered);
  }

  function exportRequestsCSV() {
    if (allRequests.length === 0) return alert("لا توجد طلبات لتصديرها");
    let csv = "\uFEFFتاريخ الطلب,اسم المشترك,الهاتف,الباقة,كود الجهاز,الموديل,الحالة\n";
    allRequests.forEach(r => {
      const dateStr = new Date(r.timestamp || Date.now()).toLocaleString('ar-EG');
      csv += `"${dateStr}","${r.subscriberName || ''}","${r.phone || ''}","${r.plan || 'MONTHLY'}","${r.deviceCode || ''}","${r.deviceModel || ''}","${r.status || 'PENDING'}"\n`;
    });
    downloadCSV(csv, "mosarib_subscription_requests.csv");
  }

  function setUserFilter(filter) {
    currentUserFilter = filter;
    document.querySelectorAll('.pill-filter-btn').forEach(b => b.classList.remove('active'));
    const btn = document.getElementById(`filter${filter.charAt(0) + filter.slice(1).toLowerCase().replace(/_([a-z])/g, (_, l) => l.toUpperCase())}`);
    if (btn) btn.classList.add('active');
    filterUsers();
  }

  function renderUsersTable(users) {
    const tbody = document.getElementById("usersTableBody");
    if (!users || users.length === 0) {
      tbody.innerHTML = `<tr><td colspan="8" style="text-align: center; color: var(--text-muted);">لا يوجد مستخدمون مسجلون بعد (سيظهر كل من يفتح التطبيق هنا تلقائياً)</td></tr>`;
      return;
    }

    const globalLimit = appConfig.freeOperationsLimit || 200;

    // Counters for pill filters
    let countActive = 0, countTrial = 0, countLocked = 0, countBackupLocked = 0, countBlocked = 0;
    users.forEach(u => {
      const isBlocked = u.isBlocked === true;
      const isAct = u.isActivated === true && !isBlocked;
      const devKey = u.cleanDeviceKey || (u.deviceCode ? u.deviceCode.replace(/-/g, "") : "");
      const override = allOverrides[devKey] || {};
      const limit = override.customOperationsLimit || globalLimit;
      const ops = u.operationsCount || 0;
      const isLocked = !isAct && (ops >= limit || u.isLocked);
      const isBkLocked = override.isBackupLocked === true || (!isAct && appConfig.isBackupAllowedForTrial === false);

      if (isBlocked) countBlocked++;
      else if (isAct) countActive++;
      else countTrial++;

      if (isLocked) countLocked++;
      if (isBkLocked) countBackupLocked++;
    });

    if (document.getElementById("countFilterAll")) document.getElementById("countFilterAll").innerText = users.length;
    if (document.getElementById("countFilterActive")) document.getElementById("countFilterActive").innerText = countActive;
    if (document.getElementById("countFilterTrial")) document.getElementById("countFilterTrial").innerText = countTrial;
    if (document.getElementById("countFilterLocked")) document.getElementById("countFilterLocked").innerText = countLocked;
    if (document.getElementById("countFilterBackupLocked")) document.getElementById("countFilterBackupLocked").innerText = countBackupLocked;
    if (document.getElementById("countFilterBlocked")) document.getElementById("countFilterBlocked").innerText = countBlocked;

    // Apply filter
    let filteredList = users.filter(u => {
      const isBlocked = u.isBlocked === true;
      const isAct = u.isActivated === true && !isBlocked;
      const devKey = u.cleanDeviceKey || (u.deviceCode ? u.deviceCode.replace(/-/g, "") : "");
      const override = allOverrides[devKey] || {};
      const limit = override.customOperationsLimit || globalLimit;
      const ops = u.operationsCount || 0;
      const isLocked = !isAct && (ops >= limit || u.isLocked);
      const isBkLocked = override.isBackupLocked === true || (!isAct && appConfig.isBackupAllowedForTrial === false);

      if (currentUserFilter === 'ACTIVE') return isAct;
      if (currentUserFilter === 'TRIAL') return !isAct && !isBlocked;
      if (currentUserFilter === 'LOCKED') return isLocked;
      if (currentUserFilter === 'BACKUP_LOCKED') return isBkLocked;
      if (currentUserFilter === 'BLOCKED') return isBlocked;
      return true;
    });

    // Search query
    const q = (document.getElementById("userSearch")?.value || "").trim().toLowerCase();
    if (q) {
      filteredList = filteredList.filter(u => 
        (u.googleEmail && u.googleEmail.toLowerCase().includes(q)) ||
        (u.googleDisplayName && u.googleDisplayName.toLowerCase().includes(q)) ||
        (u.subscriberName && u.subscriberName.toLowerCase().includes(q)) ||
        (u.phone && u.phone.includes(q)) ||
        (u.deviceCode && u.deviceCode.toLowerCase().includes(q)) ||
        (u.deviceModel && u.deviceModel.toLowerCase().includes(q))
      );
    }

    filteredList.sort((a,b) => (b.lastActiveAt || 0) - (a.lastActiveAt || 0));

    if (filteredList.length === 0) {
      tbody.innerHTML = `<tr><td colspan="8" style="text-align: center; color: var(--text-muted); padding: 20px;">لا توجد نتائج مطابقة للفلتر المختار</td></tr>`;
      return;
    }

    tbody.innerHTML = filteredList.map(u => {
      const devKey = u.cleanDeviceKey || (u.deviceCode ? u.deviceCode.replace(/-/g, "") : "");
      const override = allOverrides[devKey] || {};
      const limit = override.customOperationsLimit || globalLimit;
      const ops = u.operationsCount || 0;
      const isBlocked = u.isBlocked === true;
      const isAct = u.isActivated === true && !isBlocked;
      const isOverLimit = !isAct && ops >= limit;
      const isDeviceBkLocked = override.isBackupLocked === true;

      // Status Badge
      let statusBadge;
      if (isBlocked) statusBadge = `<span class="badge badge-blocked">⛔ محظور</span>`;
      else if (isAct) statusBadge = `<span class="badge badge-active">✅ مشترك مفعّل</span>`;
      else if (isOverLimit) statusBadge = `<span class="badge badge-blocked">🔒 تجاوز الحد (مقفل)</span>`;
      else statusBadge = `<span class="badge badge-trial">⏳ تجريبي مجاني</span>`;

      // Plan Badge
      const planBadge = u.plan === "YEARLY" 
        ? `<span class="badge badge-yearly">سنوي ⭐</span>` 
        : (u.plan === "MONTHLY" ? `<span class="badge badge-monthly">شهري</span>` : `<span class="badge" style="background:#334155;color:#94a3b8;">تجربة</span>`);

      // Operations Progress Bar
      let pct = Math.min(100, Math.round((ops / limit) * 100));
      let barColor = isAct ? "var(--success)" : (ops >= limit ? "var(--danger)" : (pct >= 75 ? "var(--accent)" : "var(--primary-light)"));
      const customLimitNote = override.customOperationsLimit ? `<br><small style="color:var(--accent); font-weight:bold;">(حد خاص: ${override.customOperationsLimit})</small>` : '';
      const opsHtml = isAct 
        ? `<div><b>${ops}</b> عملية<br><small style="color:var(--success);">غير محدود (مشترك)</small></div>`
        : `
          <div>
            <b>${ops}</b> / ${limit} عملية (${pct}%)${customLimitNote}
            <div class="ops-bar-container">
              <div class="ops-bar-fill" style="width: ${pct}%; background: ${barColor};"></div>
            </div>
          </div>
        `;

      // Backup Status Badge
      let bkBadge;
      if (isDeviceBkLocked) bkBadge = `<span class="badge badge-blocked" title="تم قفل النسخ لهذا الجهاز بالاسم">🚫 مقفول للجهاز</span>`;
      else if (!isAct && appConfig.isBackupAllowedForTrial === false) bkBadge = `<span class="badge badge-trial" title="معطل عالمياً للمجانيين">⚠️ معطل للكل</span>`;
      else bkBadge = `<span class="badge badge-active">✅ متاح</span>`;

      const lastSeen = u.lastActiveAt ? new Date(u.lastActiveAt).toLocaleString('ar-EG') : 'غير محدد';
      const userName = u.subscriberName || u.googleDisplayName || 'مستخدم مسرّب';
      const userPhone = u.phone ? `<br><small style="color:#4ADE80; font-weight:600;">📞 ${u.phone}</small>` : '';
      const googleInfo = u.googleEmail 
        ? `<div><b>${userName}</b>${userPhone}<br><small style="color:#38BDF8;">${u.googleEmail}</small></div>`
        : `<div><b>${userName}</b>${userPhone}<br><span style="color:#64748B; font-size:11px;">بدون بريد جوجل</span></div>`;

      // Action Buttons
      const blockBtn = isBlocked
        ? `<button class="btn-sm btn-unblock" onclick="toggleBlockUser('${devKey}', '${u.googleEmail || ''}', false)">فك الحظر</button>`
        : `<button class="btn-sm btn-block" onclick="toggleBlockUser('${devKey}', '${u.googleEmail || ''}', true)">حظر ⛔</button>`;

      const bkLockBtn = isDeviceBkLocked
        ? `<button class="btn-sm btn-unblock" onclick="toggleDeviceBackupLock('${devKey}', false)">فتح النسخ 🔓</button>`
        : `<button class="btn-sm btn-outline" style="padding:4px 8px; font-size:11px;" onclick="toggleDeviceBackupLock('${devKey}', true)">قفل النسخ 🔒</button>`;

      const customLimitBtn = `<button class="btn-sm btn-outline" style="padding:4px 8px; font-size:11px;" onclick="promptSetCustomLimit('${devKey}', ${limit})">تعديل الحد ⚙️</button>`;
      const warnBtn = `<button class="btn-sm btn-outline" style="padding:4px 8px; font-size:11px;" onclick="promptSendDeviceWarning('${devKey}')">رسالة ⚠️</button>`;

      const warningBadge = override.specialWarningMessage ? `<div style="margin-top:4px;"><small style="color:var(--accent); font-size:10px;">رسالة موجهة: ${override.specialWarningMessage}</small></div>` : '';

      const priceTag = (u.pricePerHour || u.hourlyRate) 
        ? `<b style="color:var(--gold); font-size:13px;">${Number(u.pricePerHour || u.hourlyRate).toLocaleString('ar-EG')}</b> <small style="color:var(--text-muted);">ريال/س</small>`
        : `<span style="color:var(--text-muted); font-size:11.5px;">افتراضي (5000)</span>`;

      return `
        <tr>
          <td>${googleInfo}</td>
          <td><code>${u.deviceCode || devKey}</code><br><small style="color:var(--text-muted);">${u.deviceModel || '-'}</small></td>
          <td>${priceTag}</td>
          <td>${statusBadge}${warningBadge}</td>
          <td>${planBadge}</td>
          <td>${opsHtml}</td>
          <td>${bkBadge}</td>
          <td><small>${lastSeen}</small></td>
          <td>
            <div class="btn-action-group">
              ${blockBtn}
              ${bkLockBtn}
              ${customLimitBtn}
              ${warnBtn}
            </div>
          </td>
        </tr>
      `;
    }).join('');
  }

  async function toggleDeviceBackupLock(cleanDeviceKey, lock) {
    const text = lock ? "قفل ميزة النسخ الاحتياطي لهذا الجهاز تحديداً؟" : "إعادة السماح بالنسخ الاحتياطي لهذا الجهاز؟";
    if (!confirm(text)) return;
    try {
      await firebaseFetch(`device_overrides/${cleanDeviceKey}/isBackupLocked.json`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(lock)
      });
      alert(lock ? "تم قفل النسخ الاحتياطي للجهاز بنجاح." : "تم فك قفل النسخ الاحتياطي بنجاح.");
      loadFirebaseData();
    } catch (e) {
      alert("خطأ: " + e.message);
    }
  }

  async function promptSetCustomLimit(cleanDeviceKey, currentLimit) {
    const res = prompt(`أدخل حد العمليات المخصص لهذا الجهاز (الافتراضي الحالي: ${currentLimit}):\nاكتب 0 لإلغاء الحد المخصص والعودة للحد العام.`, currentLimit);
    if (res === null) return;
    const num = parseInt(res);
    if (isNaN(num) || num < 0) return alert("يرجى إدخال رقم صحيح");

    try {
      if (num === 0) {
        await firebaseFetch(`device_overrides/${cleanDeviceKey}/customOperationsLimit.json`, { method: "DELETE" });
        alert("تم إلغاء الحد المخصص والعودة للحد العام.");
      } else {
        await firebaseFetch(`device_overrides/${cleanDeviceKey}/customOperationsLimit.json`, {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(num)
        });
        alert(`تم ضبط حد العمليات لهذا الجهاز إلى ${num} عملية بنجاح!`);
      }
      loadFirebaseData();
    } catch (e) {
      alert("خطأ: " + e.message);
    }
  }

  async function promptSendDeviceWarning(cleanDeviceKey) {
    const currentMsg = allOverrides[cleanDeviceKey]?.specialWarningMessage || "";
    const msg = prompt("اكتب رسالة التنبيه التي ستظهر للمستخدم على شاشته فور فتح التطبيق:\n(اترك الحقل فارغاً لحذف الرسالة الحالية)", currentMsg);
    if (msg === null) return;

    try {
      if (msg.trim().length === 0) {
        await firebaseFetch(`device_overrides/${cleanDeviceKey}/specialWarningMessage.json`, { method: "DELETE" });
        alert("تم حذف رسالة التنبيه الموجهة.");
      } else {
        await firebaseFetch(`device_overrides/${cleanDeviceKey}/specialWarningMessage.json`, {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(msg.trim())
        });
        alert("تم حفظ رسالة التنبيه وستظهر للمستخدم في هاتفه!");
      }
      loadFirebaseData();
    } catch (e) {
      alert("خطأ: " + e.message);
    }
  }

  async function toggleBlockUser(cleanDeviceKey, email, block) {
    const actionText = block ? "إيقاف وحظر التطبيق تماماً على هذا الجهاز أو المستخدم؟" : "إلغاء الحظر والسماح للمستخدم بالعمل؟";
    let reason = "تم إيقاف التطبيق على هذا الجهاز لمخالفة شروط الاستخدام أو محاولة التحايل.";
    if (block) {
      const inputReason = prompt("اكتب سبب الإيقاف الذي سيظهر للمستخدم على شاشته:", reason);
      if (inputReason === null) return;
      if (inputReason.trim()) reason = inputReason.trim();
    } else {
      if (!confirm(`هل أنت متأكد من ${actionText}`)) return;
    }

    try {
      await firebaseFetch(`all_users/${cleanDeviceKey}/isBlocked.json`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(block)
      });
      await firebaseFetch(`all_users/${cleanDeviceKey}/blockReason.json`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(reason)
      });

      if (email && email.includes("@")) {
        const safeEmailKey = email.replace(/\./g, "_").replace(/@/g, "_at_");
        if (block) {
          await firebaseFetch(`blocked_emails/${safeEmailKey}.json`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ email: email, blockedAt: Date.now(), reason: reason })
          });
        } else {
          await firebaseFetch(`blocked_emails/${safeEmailKey}.json`, { method: "DELETE" });
        }
      }

      alert(block ? "تم إيقاف وحظر التطبيق على المستخدم بنجاح." : "تم فك الحظر بنجاح.");
      loadFirebaseData();
    } catch (e) {
      alert("حدث خطأ أثناء تعديل الحالة: " + e.message);
    }
  }

  function filterUsers() {
    renderUsersTable(allUsers);
  }

  function exportUsersCSV() {
    if (allUsers.length === 0) return alert("لا توجد بيانات لتصديرها");
    let csv = "\uFEFFالاسم بجوجل,إيميل جوجل,كود الجهاز,الموديل,النظام,الحالة,الباقة,العمليات,آخر ظهور\n";
    allUsers.forEach(u => {
      const lastSeen = u.lastActiveAt ? new Date(u.lastActiveAt).toLocaleString('ar-EG') : '';
      const status = u.isBlocked ? "موقوف" : (u.isActivated ? "مشترك" : "تجريبي");
      csv += `"${u.googleDisplayName || ''}","${u.googleEmail || ''}","${u.deviceCode || ''}","${u.deviceModel || ''}","${u.androidVersion || ''}","${status}","${u.plan || 'TRIAL'}","${u.operationsCount || 0}","${lastSeen}"\n`;
    });
    downloadCSV(csv, "mosarib_all_users.csv");
  }

  async function generateAndSaveKey() {
    const devCode = document.getElementById("deviceCode").value.trim().toUpperCase();
    const subName = document.getElementById("subscriberName").value.trim();
    const plan = document.getElementById("planSelect").value;
    const prefix = plan === "MONTHLY" ? "M" : "Y";
    const durationDays = plan === "MONTHLY" ? 30 : 365;

    if (!devCode) {
      alert("يرجى كتابة كود جهاز العميل أولاً");
      return;
    }

    const cleanCode = devCode.replace(/-/g, "").trim();
    const hash = await sha256(cleanCode + SECRET_SALT + prefix);
    const part1 = hash.substring(0, 4);
    const part2 = hash.substring(4, 8);
    const key = `ACTV-${prefix}-${part1}-${part2}`;
    const cleanKey = key.replace(/-/g, "");

    const now = Date.now();
    const expiresAt = now + (durationDays * 24 * 3600 * 1000);

    const payload = {
      key: cleanKey,
      displayKey: key,
      deviceCode: devCode,
      subscriberName: subName || "مشترك",
      plan: plan,
      activatedAt: now,
      expiresAt: expiresAt,
      isRevoked: false,
      lastCheckedAt: now
    };

    let pushSuccess = false;
    let pushError = null;

    try {
      // 1. Save License to RTDB
      const res = await firebaseFetch(`licenses/${cleanKey}.json`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      });
      if (res.ok) {
        pushSuccess = true;
      } else {
        pushError = `HTTP ${res.status} (${res.statusText || 'رفض الخادم'})`;
      }

      // 2. Also save to devices for fast device lookup
      await firebaseFetch(`devices/${devCode}.json`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      }).catch(() => {});

      // 3. Mark subscription request as completed if applicable
      if (pendingRequestId) {
        await firebaseFetch(`subscription_requests/${pendingRequestId}/status.json`, {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify("COMPLETED")
        }).catch(() => {});

        await firebaseFetch(`subscription_requests/${pendingRequestId}/activationKey.json`, {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(key)
        }).catch(() => {});

        pendingRequestId = null;
      }
    } catch (e) {
      pushError = e.message;
    }

    document.getElementById("keyText").innerText = key;
    document.getElementById("keyBox").style.display = "block";

    if (pushSuccess) {
      alert(`✅ تم توليد وتفعيل الرمز بنجاح: ${key}\nتم رفعه وحفظه في سحابة فايربيس بنجاح وربطه بالجهاز (${devCode}).`);
    } else {
      alert(`⚠️ تم توليد الرمز محلياً: ${key}\nولكن حدث خطأ أثناء رفعه إلى فايربيس: (${pushError || 'تعذر الاتصال'}).\nالرابط الحالي: ${FIREBASE_BASE_URL}\nيمكنك النقر على زر سحابة فايربيس بأعلى الصفحة للتأكد من الرابط وقواعد الأمان.`);
    }

    loadFirebaseData();
  }

  function copyKey() {
    const key = document.getElementById("keyText").innerText;
    navigator.clipboard.writeText(key).then(() => alert("تم نسخ الكود بنجاح: " + key));
  }

  function sendToWhatsApp() {
    const key = document.getElementById("keyText").innerText;
    const name = document.getElementById("subscriberName").value.trim() || "عزيزنا المشترك";
    const plan = document.getElementById("planSelect").value === "MONTHLY" ? "اشتراك شهري (30 يوماً)" : "اشتراك سنوي (365 يوماً)";
    const text = `السلام عليكم ورحمة الله أخي ${name}،\nتم تفعيل اشتراكك في تطبيق المُسَرِّب (${plan}).\n\nرمز التفعيل لجهازك هو:\n${key}\n\nيرجى نسخه ولصقه في نافذة التفعيل بالتطبيق أثناء الاتصال بالإنترنت.`;
    window.open(`https://wa.me/?text=${encodeURIComponent(text)}`, '_blank');
  }

  function renderFeedbacksTable(feedbacks) {
    const tbody = document.getElementById("feedbacksTableBody");
    if (!feedbacks || feedbacks.length === 0) {
      tbody.innerHTML = `<tr><td colspan="8" style="text-align: center; color: var(--text-muted);">لا توجد رسائل ملاحظات واردة بعد</td></tr>`;
      return;
    }

    feedbacks.sort((a,b) => (b.timestamp || 0) - (a.timestamp || 0));

    tbody.innerHTML = feedbacks.map(f => {
      const dateStr = new Date(f.timestamp || Date.now()).toLocaleString('ar-EG');
      const stars = "★".repeat(f.rating || 5) + "☆".repeat(5 - (f.rating || 5));
      const phoneLink = f.phone ? `<a href="https://wa.me/${f.phone.replace(/[^0-9]/g, '')}" target="_blank" style="color:var(--success); font-weight:bold;">${f.phone}</a>` : '-';

      return `
        <tr>
          <td><small>${dateStr}</small></td>
          <td><b>${f.subscriberName || 'مستخدم'}</b></td>
          <td>${phoneLink}</td>
          <td><span class="rating-stars">${stars}</span></td>
          <td><span class="badge badge-monthly">${f.category || 'اقتراح'}</span></td>
          <td style="max-width: 320px; line-height: 1.6;">${f.message}</td>
          <td><small>${f.deviceModel || f.deviceCode || '-'}</small></td>
          <td>
            ${f.phone ? `<button class="btn-sm btn-unblock" onclick="window.open('https://wa.me/${f.phone.replace(/[^0-9]/g, '')}', '_blank')">واتساب 💬</button>` : '-'}
          </td>
        </tr>
      `;
    }).join('');
  }

  function filterFeedbacks() {
    const q = document.getElementById("feedbackSearch").value.trim().toLowerCase();
    const filtered = allFeedbacks.filter(f => 
      (f.subscriberName && f.subscriberName.toLowerCase().includes(q)) ||
      (f.message && f.message.toLowerCase().includes(q)) ||
      (f.phone && f.phone.includes(q))
    );
    renderFeedbacksTable(filtered);
  }

  function exportFeedbacksCSV() {
    if (allFeedbacks.length === 0) return alert("لا توجد رسائل لتصديرها");
    let csv = "\uFEFFالتاريخ,الاسم,الهاتف,التقييم,النوع,الرسالة,الجهاز\n";
    allFeedbacks.forEach(f => {
      const dateStr = new Date(f.timestamp || Date.now()).toLocaleString('ar-EG');
      csv += `"${dateStr}","${f.subscriberName || ''}","${f.phone || ''}","${f.rating || 5}","${f.category || ''}","${(f.message || '').replace(/"/g, '""')}","${f.deviceModel || ''}"\n`;
    });
    downloadCSV(csv, "mosarib_feedbacks.csv");
  }

  function downloadCSV(csvContent, fileName) {
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement("a");
    link.href = URL.createObjectURL(blob);
    link.setAttribute("download", fileName);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  // --- Theme Toggle Logic ---
  function initTheme() {
    try {
      const savedTheme = localStorage.getItem('mosarib_admin_theme') || 'dark';
      applyTheme(savedTheme);
    } catch(e) {
      applyTheme('dark');
    }
  }

  function applyTheme(theme) {
    const icon = document.getElementById('themeIcon');
    const label = document.getElementById('themeLabel');
    if (theme === 'light') {
      document.documentElement.setAttribute('data-theme', 'light');
      if (icon) icon.textContent = '🌙';
      if (label) label.textContent = 'الوضع الليلي';
    } else {
      document.documentElement.removeAttribute('data-theme');
      if (icon) icon.textContent = '☀️';
      if (label) label.textContent = 'الوضع الفاتح';
    }
    try {
      localStorage.setItem('mosarib_admin_theme', theme);
    } catch(e) {}
  }

  function toggleTheme() {
    const isLight = document.documentElement.getAttribute('data-theme') === 'light';
    applyTheme(isLight ? 'dark' : 'light');
  }

  // Initial load
  initTheme();
  checkServerSession();
</script>

</body>
</html>
