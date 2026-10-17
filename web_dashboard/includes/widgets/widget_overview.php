  <!-- Stats Grid Widget -->
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
