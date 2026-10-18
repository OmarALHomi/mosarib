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

      <!-- Quick Filter Pills for Requests -->
      <div class="pill-filter" style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px; margin-top: 12px; margin-bottom: 12px;">
        <div style="display: flex; gap: 8px; flex-wrap: wrap;">
          <button class="pill-filter-btn active" id="reqFilterPending" onclick="setRequestFilter('PENDING')">الطلبات الجديدة المعلقة (<span id="countReqPending">0</span>)</button>
          <button class="pill-filter-btn" id="reqFilterCompleted" onclick="setRequestFilter('COMPLETED')">الطلبات المنفذة (<span id="countReqCompleted">0</span>)</button>
          <button class="pill-filter-btn" id="reqFilterAll" onclick="setRequestFilter('ALL')">كافة الطلبات (<span id="countReqAll">0</span>)</button>
        </div>
        <button class="btn btn-outline" style="width: auto; padding: 6px 14px; font-size: 12px; color: var(--danger); border-color: var(--danger);" onclick="deleteCompletedRequests()" title="حذف جميع الطلبات التي تم تفعيلها سابقاً">🗑️ حذف المنفذة</button>
      </div>

      <div class="table-container">
        <table>
          <thead>
            <tr>
              <th>تاريخ الطلب</th>
              <th>اسم المشترك</th>
              <th>رقم الهاتف</th>
              <th>الباقة المطلوبة</th>
              <th>المنطقة / رقم السند</th>
              <th>كود الجهاز</th>
              <th>المصدر</th>
              <th>الحالة</th>
              <th>إجراء فوري</th>
            </tr>
          </thead>
          <tbody id="requestsTableBody">
            <tr><td colspan="9" style="text-align: center; color: var(--text-muted);">جاري تحميل طلبات الاشتراك...</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>

  <!-- Tab 1: All Users (Both subscribed & trial, with Google Accounts & Instant Block) -->
