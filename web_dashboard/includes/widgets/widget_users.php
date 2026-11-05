  <div id="tab-users">
    <div class="card" id="expiringCard" style="margin-bottom: 20px;">
      <div class="toolbar">
        <div class="card-title" style="margin-bottom: 0;">اشتراكات تنتهي قريباً <span class="badge badge-blocked" id="expiringCountBadge" style="margin-right:6px;">0</span></div>
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
        <button class="pill-filter-btn" id="filterDeleted" onclick="setUserFilter('DELETED')">المحذوفون (<span id="countFilterDeleted">0</span>)</button>
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

      <div class="table-container" id="usersTableContainer">
        <table>
          <thead>
            <tr>
              <th style="width:28px;"><input type="checkbox" id="usersSelectAll" style="width:auto;" onclick="event.stopPropagation(); toggleSelectAllUsers(this.checked)"></th>
              <th>حساب Google / الاسم</th>
              <th>كود الجهاز والموديل</th>
              <th>سعر الساعة للمسرب</th>
              <th>الحالة</th>
              <th>الباقة والاشتراك</th>
              <th>رصيد العمليات والحد</th>
              <th>النسخ الاحتياطي</th>
              <th>آخر ظهور</th>
              <th>إجراءات التحكم</th>
            </tr>
          </thead>
          <tbody id="usersTableBody">
            <tr><td colspan="10" style="text-align: center; color: var(--text-muted);">جاري تحميل قائمة المستخدمين من السحابة...</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>

  <!-- Tab: Releases & APK Upload Manager -->
