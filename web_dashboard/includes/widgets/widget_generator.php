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
        <button class="btn" style="width: auto; padding: 10px 24px; font-weight: 700;" onclick="generateAndSaveKey()">⚡ توليد وحفظ</button>

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
        <div class="card-title">حماية التفعيل</div>
        <ul style="padding-right: 20px; line-height: 2; font-size: 13.5px; color: var(--text-muted);">
          <li><b>قفل عتادي:</b> يرتبط رمز التفعيل ببصمة الجهاز ولا يعمل على جهاز آخر.</li>
          <li><b>سجل الأجهزة:</b> يتم تسجيل كافة الأجهزة المسجلة تلقائياً في شاشة المستخدمين.</li>
          <li><b>إيقاف فوري:</b> يمكن حظر أو إلغاء تفعيل أي جهاز بضغطة زر واحدة.</li>
        </ul>
      </div>
    </div>
  </div>

  <!-- Tab 3: Feedbacks -->
