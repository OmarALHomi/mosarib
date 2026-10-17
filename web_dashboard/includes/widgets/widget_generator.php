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
        <button class="btn" onclick="generateAndSaveKey()">توليد وحفظ في سحابة فايربيس</button>

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
        <div class="card-title">حماية متقدمة ومكافحة التحايل</div>
        <ul style="padding-right: 20px; line-height: 2.2; font-size: 14px; color: #B2DFDB;">
          <li><b>تسجيل كافة المستخدمين:</b> يظهر في لوحتك كل من قام بفتح التطبيق بحساب Google ومعرف جهازه وعدد عملياته، حتى وإن لم يشترك بعد!</li>
          <li><b>إيقاف فوري بنقرة واحدة:</b> إذا لاحظت أن شخصاً يتلاعب أو يكرر مسح البيانات، يمكنك إيقاف التطبيق عليه وحظره فوراً برقم جهازه أو إيميله.</li>
          <li><b>قفل عتادي:</b> الكود يرتبط ببصمة الهاتف ولا يمكن تشغيله على هاتفين إطلاقاً.</li>
        </ul>
      </div>
    </div>
  </div>

  <!-- Tab 3: Feedbacks -->
