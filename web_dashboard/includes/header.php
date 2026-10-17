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
      <div class="status-badge" id="firebaseStatus" onclick="promptConfigureFirebaseUrl()" style="cursor: pointer;" title="انقر لعرض وفحص وتعديل رابط وقواعد قاعدة بيانات فايربيس ومفتاح الأمان">
        <div class="status-dot"></div> سحابة فايربيس
      </div>
    </div>
  </header>
