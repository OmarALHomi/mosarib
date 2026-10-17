  <div id="tab-policies" style="display: none;">
    <div class="grid-2">
      <div class="card">
        <div class="card-title">السياسات العامة للنسخ المجانية والتطبيق عن بُعد</div>
        
        <div class="form-group">
          <label for="policyFreeLimit">الحد الأقصى للعمليات المجانية لجميع النسخ غير المشتركة:</label>
          <input type="number" id="policyFreeLimit" value="200" min="5" max="10000">
          <small style="color: var(--text-muted); font-size: 11px;">العدد الذي إذا وصل إليه المستخدم غير المشترك، يُقفل عليه التطبيق ويطالبه بالاشتراك (افتراضياً 200).</small>
        </div>

        <div class="policy-item">
          <div class="policy-info">
            <h4>السماح بالنسخ الاحتياطي للنسخ المجانية</h4>
            <p>عند إيقاف هذا الخيار، يتم تعطيل زر النسخ الاحتياطي والاستعادة لجميع غير المشتركين لمنع تلاعب مسح واستعادة البيانات.</p>
          </div>
          <label class="switch">
            <input type="checkbox" id="policyBackupAllowed" checked>
            <span class="slider"></span>
          </label>
        </div>

        <div class="policy-item">
          <div class="policy-info">
            <h4>السماح بالنسخ السحابي عبر Google Drive</h4>
            <p>إتاحة أو تعطيل النسخ الاحتياطي السحابي عبر حساب Google لغير المشتركين.</p>
          </div>
          <label class="switch">
            <input type="checkbox" id="policyDriveBackupAllowed" checked>
            <span class="slider"></span>
          </label>
        </div>

        <div class="policy-item">
          <div class="policy-info">
            <h4>السماح بتصدير ملفات Excel للنسخ المجانية</h4>
            <p>إتاحة أو تقييد ميزة استخراج كشوفات وتقارير Excel للنسخ التجريبية.</p>
          </div>
          <label class="switch">
            <input type="checkbox" id="policyExcelExportAllowed" checked>
            <span class="slider"></span>
          </label>
        </div>

        <div class="form-group" style="margin-top: 14px;">
          <label for="policyGlobalNote">تنبيه أو إشعار عام يظهر لجميع المستخدمين التجريبيين (اختياري):</label>
          <input type="text" id="policyGlobalNote" placeholder="مثال: خصم 30% على الاشتراك السنوي بمناسبة موسم السقي!">
        </div>

        <button class="btn" onclick="saveAppPolicies()">حفظ وتطبيق السياسات فوراً على السحابة</button>
      </div>

      <div class="card">
        <div class="card-title">مميزات نظام السياسات والتحكم عن بُعد</div>
        <ul style="padding-right: 20px; line-height: 2.2; font-size: 13.5px; color: #B2DFDB;">
          <li><b>تغيير الحد دون الحاجة لتحديث APK:</b> يمكنك خفض الحد إلى 50 أو رفعه إلى 500 في أي ثانية، وسيطبقه هاتف العميل تلقائياً عند اتصاله بالإنترنت!</li>
          <li><b>ردع تحايل مسح البيانات:</b> إذا لاحظت محاولات تفريغ الحسابات واستعادتها لتخطي الحد، عطّل ميزة النسخ الاحتياطي للنسخ المجانية فجأة بنقرة زر واحدة.</li>
          <li><b>تحكم استثنائي لكل جهاز:</b> من خلال جدول "المستخدمون"، يمكنك تخصيص حد أعلى لعميل معين أو قفل النسخ الاحتياطي لجهاز محدد دون التأثير على بقية المستخدمين.</li>
        </ul>

        <div style="margin-top: 18px; padding: 14px; background: var(--input-bg); border-radius: 12px; border: 1px dashed var(--card-border);">
          <div style="font-size: 12.5px; font-weight: 700; color: var(--text-muted); margin-bottom: 6px;">الحالة المسجلة حالياً على السحابة:</div>
          <div id="policyCloudStatus" style="font-size: 13px; color: var(--success); font-weight: bold;">جاري القراءة...</div>
        </div>
      </div>
    </div>
  </div>

  <!-- Tab: Broadcast & Force Update -->
