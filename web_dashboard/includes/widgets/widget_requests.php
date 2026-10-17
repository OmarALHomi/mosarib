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
