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
