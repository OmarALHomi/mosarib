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
