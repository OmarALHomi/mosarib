  <!-- Section: Subscriptions (read-only list, newest licence per device; the only action is تمديد) -->
  <div id="tab-subs" style="display: none;">
    <div class="card">
      <div class="toolbar">
        <div class="card-title" style="margin-bottom: 0;">الاشتراكات</div>
        <div class="toolbar-actions">
          <input type="search" class="search-input" id="subsSearch" placeholder="بحث بالاسم أو كود الجهاز..." oninput="renderSubscriptionsList()">
          <button class="btn btn-outline btn-compact" onclick="loadFirebaseData()">تحديث</button>
        </div>
      </div>
      <div class="pill-filter" id="subsFilterChips">
        <button class="pill-filter-btn active" data-subs="ALL" onclick="setSubsFilter('ALL')">الكل (<span id="countSubsAll">0</span>)</button>
        <button class="pill-filter-btn" data-subs="soon" onclick="setSubsFilter('soon')">ينتهي قريباً (<span id="countSubsSoon">0</span>)</button>
        <button class="pill-filter-btn" data-subs="expired" onclick="setSubsFilter('expired')">منتهٍ (<span id="countSubsExpired">0</span>)</button>
        <button class="pill-filter-btn" data-subs="revoked" onclick="setSubsFilter('revoked')">ملغى (<span id="countSubsRevoked">0</span>)</button>
      </div>
      <div class="table-container sticky-head">
        <table class="rtable">
          <thead>
            <tr><th>المشترك</th><th>كود الجهاز</th><th>الباقة</th><th>ينتهي في</th><th>الحالة</th><th class="col-actions">إجراء</th></tr>
          </thead>
          <tbody id="subsTableBody">
            <tr class="skeleton-row"><td colspan="6"><div class="skeleton"></div></td></tr>
            <tr class="skeleton-row"><td colspan="6"><div class="skeleton"></div></td></tr>
            <tr class="skeleton-row"><td colspan="6"><div class="skeleton"></div></td></tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Renewal reminders (moved here from the users tab, unchanged) -->
    <div class="card" id="expiringCard" style="margin-top: 20px;">
      <div class="toolbar">
        <div class="card-title" style="margin-bottom: 0;">تذكيرات التجديد <span class="badge badge-blocked" id="expiringCountBadge" style="margin-right:6px;">0</span></div>
        <div class="pill-filter" style="margin: 0;">
          <button class="pill-filter-btn exp-pill active" data-days="7" onclick="setExpiringFilter(7)">7 أيام</button>
          <button class="pill-filter-btn exp-pill" data-days="15" onclick="setExpiringFilter(15)">15 يوماً</button>
          <button class="pill-filter-btn exp-pill" data-days="30" onclick="setExpiringFilter(30)">30 يوماً</button>
          <button class="pill-filter-btn exp-pill" data-days="-1" onclick="setExpiringFilter(-1)">منتهية</button>
        </div>
      </div>
      <div class="table-container">
        <table>
          <thead>
            <tr><th>المشترك</th><th>الهاتف</th><th>الباقة</th><th>نهاية الاشتراك</th><th>المتبقي</th><th>تواصل</th></tr>
          </thead>
          <tbody id="expiringTableBody"><tr><td colspan="6" style="text-align:center; color:var(--text-muted);">جاري التحميل...</td></tr></tbody>
        </table>
      </div>
    </div>
  </div>
