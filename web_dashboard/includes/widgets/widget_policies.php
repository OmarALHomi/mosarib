  <div id="tab-policies" style="display: none;">
    <div class="grid-2">
      <div class="card">
        <div class="card-title">السياسات العامة للنسخ المجانية والتطبيق عن بُعد</div>
        
        <div class="form-group">
          <label for="policyFreeLimit">الحد الأقصى للعمليات المجانية لجميع النسخ غير المشتركة:</label>
          <input type="number" id="policyFreeLimit" value="50" min="5" max="10000">
          <small style="color: var(--text-muted); font-size: 11px;">العدد الذي إذا وصل إليه المستخدم غير المشترك، يُقفل عليه التطبيق ويطالبه بالاشتراك (افتراضياً 50).</small>
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

        <button class="btn" style="width: auto; padding: 10px 24px; font-weight: 700;" onclick="saveAppPolicies()">💾 حفظ</button>
      </div>

      <div class="card">
        <div class="card-title">معلومات السياسات السحابية</div>
        <ul style="padding-right: 20px; line-height: 2; font-size: 13.5px; color: var(--text-muted);">
          <li><b>تحديث فوري:</b> يتم تطبيق حد العمليات والقيود تلقائياً على أجهزة المستخدمين عند اتصالهم بالإنترنت.</li>
          <li><b>النسخ الاحتياطي:</b> يمكنك تفعيل أو تعطيل النسخ السحابي والمحلي لضبط الاستخدام التجريبي.</li>
          <li><b>تخصيص فردي:</b> يمكن تعديل صلاحيات أو حد أي مستخدم بشكل منفصل من جدول المستخدمين.</li>
        </ul>

        <div style="margin-top: 18px; padding: 14px; background: var(--input-bg); border-radius: 12px; border: 1px dashed var(--card-border);">
          <div style="font-size: 12.5px; font-weight: 700; color: var(--text-muted); margin-bottom: 6px;">الحالة المسجلة حالياً على السحابة:</div>
          <div id="policyCloudStatus" style="font-size: 13px; color: var(--success); font-weight: bold;">جاري القراءة...</div>
        </div>
      </div>
    </div>

    <!-- Per-Version Feature Flags & Operations Limit Manager -->
    <div class="card" id="versionPoliciesCard" style="margin-top: 20px; border: 1px solid var(--card-border);">
      <div class="card-title" style="justify-content: space-between; flex-wrap: wrap;">
        <span>📱 سياسات وميزات حسب رقم الإصدار (VersionCode)</span>
        <small style="font-size: 11.5px; font-weight: 600; color: var(--text-muted);">تُحفظ في /app_config/versionPolicies/{versionCode}</small>
      </div>
      <p style="font-size: 12.5px; color: var(--text-muted); line-height: 1.7; margin-bottom: 14px;">
        خصّص حد العمليات المجانية أو فعّل/عطّل ميزات محددة لإصدار بعينه دون التأثير على بقية الإصدارات. القيمة «موروث من العام» تعني اتباع السياسة العامة أعلاه.
      </p>

      <div style="margin-bottom: 14px;">
        <div style="font-size: 12px; font-weight: 700; color: var(--text-muted); margin-bottom: 6px;">النسخ النشطة لدى المستخدمين (اضغط للاختيار السريع):</div>
        <div class="pill-filter" id="vpActiveVersionChips" style="margin-bottom: 0;"></div>
      </div>

      <div class="grid-2" style="margin-bottom: 0;">
        <div>
          <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; margin-bottom: 12px;">
            <div class="form-group" style="margin-bottom: 0;">
              <label for="vpVersionCode">رقم البناء (VersionCode):</label>
              <input type="number" id="vpVersionCode" min="1" placeholder="مثال: 8" oninput="onVersionPolicyCodeInput()">
              <small id="vpTargetUsersHint" style="color: var(--primary-light); font-size: 11px; display: block; margin-top: 4px;"></small>
            </div>
            <div class="form-group" style="margin-bottom: 0;">
              <label for="vpFreeLimit">حد العمليات المجانية لهذه النسخة:</label>
              <input type="number" id="vpFreeLimit" min="0" max="10000" placeholder="فارغ أو 0 = الحد العام">
              <small style="color: var(--text-muted); font-size: 11px; display: block; margin-top: 4px;">اتركه فارغاً أو 0 لاستخدام الحد العام.</small>
            </div>
          </div>

          <div class="policy-item">
            <div class="policy-info">
              <h4>النسخ الاحتياطي المحلي</h4>
              <p><code>isBackupAllowedForTrial</code></p>
            </div>
            <select id="vpFlag_isBackupAllowedForTrial" style="width: auto; min-width: 155px; padding: 7px 10px; font-size: 12.5px;">
              <option value="">موروث من العام</option>
              <option value="true">مفتوح ✅</option>
              <option value="false">مغلق 🔒</option>
            </select>
          </div>

          <div class="policy-item">
            <div class="policy-info">
              <h4>النسخ السحابي Google Drive</h4>
              <p><code>isCloudBackupAllowed</code></p>
            </div>
            <select id="vpFlag_isCloudBackupAllowed" style="width: auto; min-width: 155px; padding: 7px 10px; font-size: 12.5px;">
              <option value="">موروث من العام</option>
              <option value="true">مفتوح ✅</option>
              <option value="false">مغلق 🔒</option>
            </select>
          </div>

          <div class="policy-item">
            <div class="policy-info">
              <h4>تصدير Excel</h4>
              <p><code>isExcelExportAllowedForTrial</code></p>
            </div>
            <select id="vpFlag_isExcelExportAllowedForTrial" style="width: auto; min-width: 155px; padding: 7px 10px; font-size: 12.5px;">
              <option value="">موروث من العام</option>
              <option value="true">مفتوح ✅</option>
              <option value="false">مغلق 🔒</option>
            </select>
          </div>

          <div class="policy-item">
            <div class="policy-info">
              <h4>الإدخال الصوتي الذكي</h4>
              <p><code>allowVoiceInput</code></p>
            </div>
            <select id="vpFlag_allowVoiceInput" style="width: auto; min-width: 155px; padding: 7px 10px; font-size: 12.5px;">
              <option value="">موروث من العام</option>
              <option value="true">مفتوح ✅</option>
              <option value="false">مغلق 🔒</option>
            </select>
          </div>

          <div class="policy-item">
            <div class="policy-info">
              <h4>القراءة الصوتية للسندات والعمليات</h4>
              <p><code>allowVoiceReadout</code></p>
            </div>
            <select id="vpFlag_allowVoiceReadout" style="width: auto; min-width: 155px; padding: 7px 10px; font-size: 12.5px;">
              <option value="">موروث من العام</option>
              <option value="true">مفتوح ✅</option>
              <option value="false">مغلق 🔒</option>
            </select>
          </div>

          <div class="policy-item">
            <div class="policy-info">
              <h4>تصدير ومشاركة PDF</h4>
              <p><code>allowPdfExport</code></p>
            </div>
            <select id="vpFlag_allowPdfExport" style="width: auto; min-width: 155px; padding: 7px 10px; font-size: 12.5px;">
              <option value="">موروث من العام</option>
              <option value="true">مفتوح ✅</option>
              <option value="false">مغلق 🔒</option>
            </select>
          </div>

          <div class="policy-item">
            <div class="policy-info">
              <h4>ويجت الشاشة الرئيسية</h4>
              <p><code>allowHomeWidget</code></p>
            </div>
            <select id="vpFlag_allowHomeWidget" style="width: auto; min-width: 155px; padding: 7px 10px; font-size: 12.5px;">
              <option value="">موروث من العام</option>
              <option value="true">مفتوح ✅</option>
              <option value="false">مغلق 🔒</option>
            </select>
          </div>

          <div style="display: flex; gap: 10px; flex-wrap: wrap; margin-top: 12px;">
            <button type="button" class="btn" style="width: auto; padding: 10px 22px; font-weight: 700;" onclick="saveVersionPolicy()">💾 حفظ سياسة الإصدار</button>
            <button type="button" class="btn btn-outline" style="width: auto; padding: 10px 16px;" onclick="resetVersionPolicyForm()">🔄 تفريغ</button>
          </div>
          <div id="vpSaveStatus" style="margin-top: 10px; font-size: 12.5px; font-weight: 700; display: none;"></div>
        </div>

        <div>
          <div style="font-size: 14px; font-weight: 800; color: var(--text-title); margin-bottom: 10px;">السياسات النشطة حسب الإصدار</div>
          <div id="versionPoliciesList" style="display: flex; flex-direction: column; gap: 10px;">
            <div style="padding: 16px; border-radius: 12px; background: var(--input-bg); border: 1px dashed var(--card-border); color: var(--text-muted); font-size: 13px; text-align: center;">
              جاري قراءة سياسات الإصدارات...
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- Tab: Broadcast & Force Update -->
