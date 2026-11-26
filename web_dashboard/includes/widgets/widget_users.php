  <div id="tab-users" style="display: none;">
    <div class="card">
      <div class="toolbar">
        <div class="card-title" style="margin-bottom: 0;">المستخدمون</div>
        <div class="toolbar-actions">
          <input type="search" class="search-input" id="userSearch" placeholder="بحث بالاسم، البريد، الهاتف، الكود، الموديل..." oninput="filterUsers()">
          <button class="btn btn-outline btn-compact" onclick="loadFirebaseData()">تحديث</button>
          <button class="btn btn-outline btn-compact" onclick="exportUsersCSV()">تصدير CSV</button>
        </div>
      </div>

      <!-- Quick Filter Pills -->
      <div class="pill-filter" id="userFilterChips">
        <button class="pill-filter-btn active" id="filterAll" onclick="setUserFilter('ALL')">الكل (<span id="countFilterAll">0</span>)</button>
        <button class="pill-filter-btn" id="filterActive" onclick="setUserFilter('ACTIVE')">مشتركون (<span id="countFilterActive">0</span>)</button>
        <button class="pill-filter-btn" id="filterTrial" onclick="setUserFilter('TRIAL')">تجريبي (<span id="countFilterTrial">0</span>)</button>
        <button class="pill-filter-btn" id="filterExpired" onclick="setUserFilter('EXPIRED')">منتهٍ (<span id="countFilterExpired">0</span>)</button>
        <button class="pill-filter-btn" id="filterBlocked" onclick="setUserFilter('BLOCKED')">محظور (<span id="countFilterBlocked">0</span>)</button>
        <button class="pill-filter-btn" id="filterToday" onclick="setUserFilter('TODAY')">نشطون اليوم (<span id="countFilterToday">0</span>)</button>
        <button class="pill-filter-btn" id="filterLocked" onclick="setUserFilter('LOCKED')">تجاوزوا الحد (<span id="countFilterLocked">0</span>)</button>
        <button class="pill-filter-btn" id="filterBackupLocked" onclick="setUserFilter('BACKUP_LOCKED')">النسخ معطّل (<span id="countFilterBackupLocked">0</span>)</button>
        <button class="pill-filter-btn" id="filterDeleted" onclick="setUserFilter('DELETED')">المحذوفون (<span id="countFilterDeleted">0</span>)</button>
      </div>

      <!-- Installed App Versions Distribution & Quick Actions -->
      <div id="userVersionDistributionBar" style="margin-bottom: 14px; padding: 12px 14px; border-radius: 12px; background: var(--input-bg); border: 1px solid var(--card-border);">
        <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px; margin-bottom: 8px;">
          <span style="font-size: 12.5px; font-weight: 800; color: var(--text-title);">📱 توزيع النسخ المثبّتة:</span>
          <span id="userVersionSummaryHint" style="font-size: 11.5px; color: var(--text-muted);">اضغط على أي نسخة لفلترة الجدول أو استهدافها</span>
        </div>
        <div class="pill-filter" id="userVersionChips" style="margin-bottom: 0;"></div>
        <div id="userVersionQuickActions" style="display: none; align-items: center; gap: 8px; flex-wrap: wrap; margin-top: 10px; padding-top: 10px; border-top: 1px dashed var(--card-border);">
          <span id="userVersionSelectedLabel" style="font-size: 12.5px; font-weight: 700; color: var(--primary-light);"></span>
          <button type="button" class="btn-sm btn-unblock" onclick="jumpBroadcastForSelectedVersion()">📢 إرسال إعلان لهذه النسخة</button>
          <button type="button" class="btn-sm" style="background: rgba(0, 150, 166, 0.16); color: var(--primary-light); border: 1px solid var(--primary);" onclick="jumpPolicyForSelectedVersion()">⚙️ ضبط ميزات هذه النسخة</button>
          <button type="button" class="btn-sm btn-outline" onclick="setUserVersionFilter('ALL')">✕ إظهار كل النسخ</button>
        </div>
      </div>

      <div id="usersBulkBar" style="display:none; align-items:center; gap:10px; flex-wrap:wrap; padding:10px 12px; margin-bottom:12px; border:1px solid var(--accent); border-radius:10px;">
        <b>المحدد: <span id="usersSelectedCount">0</span></b>
        <button class="btn-sm btn-block" onclick="bulkDisableUsers()">تعطيل المحدد ⛔</button>
        <button class="btn-sm btn-outline" style="color:var(--danger); border-color:var(--danger);" onclick="bulkSoftDeleteUsers()">حذف من القائمة 🗑️</button>
        <button class="btn-sm btn-outline" onclick="clearUserSelection()">إلغاء التحديد</button>
      </div>

      <div id="deletedUsersPanel" style="display:none;">
        <p style="font-size:12.5px; color:var(--text-muted); line-height:1.8; margin-bottom:10px;">
          المحذوفون من القائمة يُسترجعون خلال 30 يوماً، ثم يُحذفون تلقائياً. الحذف النهائي للمدير الأعلى فقط. سجلات الترخيص لا تُحذف أبداً: إذا فتح المستخدم التطبيق مرة أخرى يُسجَّل كمستخدم جديد، ويبقى ترخيصه صالحاً لجهازه.
        </p>
        <div class="table-container">
          <table>
            <thead><tr><th>المستخدم</th><th>كود الجهاز</th><th>حُذف في</th><th>بواسطة / السبب</th><th>يُحذف نهائياً في</th><th>إجراءات</th></tr></thead>
            <tbody id="deletedUsersBody"><tr><td colspan="6" style="text-align:center; color:var(--text-muted);">جاري التحميل...</td></tr></tbody>
          </table>
        </div>
      </div>

      <div class="table-container sticky-head" id="usersTableContainer">
        <table class="rtable">
          <thead>
            <tr>
              <th style="width:28px;"><input type="checkbox" id="usersSelectAll" style="width:auto;" onclick="event.stopPropagation(); toggleSelectAllUsers(this.checked)"></th>
              <th>المستخدم</th>
              <th>الجهاز</th>
              <th>الحالة</th>
              <th>العمليات المتبقية</th>
              <th>آخر ظهور</th>
              <th class="col-actions">إجراءات</th>
            </tr>
          </thead>
          <tbody id="usersTableBody">
            <tr class="skeleton-row"><td colspan="7"><div class="skeleton"></div></td></tr>
            <tr class="skeleton-row"><td colspan="7"><div class="skeleton"></div></td></tr>
            <tr class="skeleton-row"><td colspan="7"><div class="skeleton"></div></td></tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>

  <!-- Tab: Releases & APK Upload Manager -->
