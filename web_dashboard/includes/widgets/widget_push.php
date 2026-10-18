  <div id="tab-push" style="display: none;">
    <!-- FCM v1 Engine Status Banner (Dual Theme Compatible) -->
    <div class="card" style="margin-bottom: 24px; border: 1px solid var(--card-border); background: var(--card-bg); box-shadow: var(--card-shadow); border-radius: var(--border-radius);">
      <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 16px;">
        <div style="flex: 1; min-width: 280px;">
          <div style="font-weight: 800; font-size: 16px; display: flex; align-items: center; gap: 8px; color: var(--text-title);">
            <span id="fcmEngineStatusBadge">⏳ جاري فحص محرك Google FCM HTTP v1...</span>
          </div>
          <div id="fcmEngineDetails" style="font-size: 13px; color: var(--text-body); margin-top: 6px; line-height: 1.6;">
            يفحص حالة ربط مفتاح الخدمة وإجمالي التوكنات المسجلة لهواتف العملاء...
          </div>
        </div>
        <div style="display: flex; gap: 10px; flex-wrap: wrap; align-items: center;">
          <button class="btn btn-secondary" style="font-size: 12.5px; padding: 8px 16px; font-weight: 700; width: auto;" onclick="toggleServiceAccountModal()">
            ⚙️ إعداد / رفع مفتاح الخدمة
          </button>
          <button class="btn" style="font-size: 12.5px; padding: 8px 18px; font-weight: 700; width: auto;" onclick="testFcmConnection()">
            ⚡ اختبار الاتصال
          </button>
        </div>
      </div>

      <!-- Service Account Upload Panel (Collapsible) -->
      <div id="serviceAccountPanel" style="display: none; margin-top: 20px; padding-top: 20px; border-top: 1px solid var(--card-border);">
        <h4 style="color: var(--primary); margin-bottom: 8px; font-size: 14.5px; font-weight: 800;">🔑 ربط مفتاح خدمة Google Service Account (FCM HTTP v1):</h4>
        <p style="font-size: 12.5px; color: var(--text-body); line-height: 1.8; margin-bottom: 14px;">
          لإرسال إشعارات ستارة حقيقية ترن وتهتز في هواتف المزارعين حتى <b>لو كان التطبيق مغلقاً تماماً</b>، يتطلب Google FCM الحديث مفتاح خدمة (Private Key):
          <br>1. افتح <a href="https://console.firebase.google.com/project/mosarib/settings/serviceaccounts/adminsdk" target="_blank" style="color:var(--primary); text-decoration:underline; font-weight:700;">Firebase Console > Service Accounts</a>.
          <br>2. اضغط على الزر <b>"Generate new private key"</b> وحمّل ملف JSON.
          <br>3. ارفع الملف أدناه أو الصق محتواه واضغط "حفظ وتفعيل المفتاح".
        </p>

        <div style="display: flex; flex-direction: column; gap: 12px;">
          <div>
            <label style="font-size: 12.5px; font-weight: 700; margin-bottom: 6px; display: block; color: var(--text-title);">اختيار ملف JSON من جهازك:</label>
            <input type="file" id="saFileInput" accept=".json" style="font-size: 12px; padding: 8px 12px; width: 100%; border: 1px dashed var(--primary); border-radius: 8px; background: var(--surface-alt); color: var(--text-main);">
          </div>
          <div>
            <label style="font-size: 12.5px; font-weight: 700; margin-bottom: 6px; display: block; color: var(--text-title);">أو لصق كود المفتاح JSON مباشرة:</label>
            <textarea id="saJsonInput" rows="4" placeholder='{ "type": "service_account", "project_id": "mosarib", "private_key": "-----BEGIN PRIVATE KEY-----...", "client_email": "..." }' style="width: 100%; font-family: monospace; font-size: 11.5px; background: var(--surface-alt); color: var(--text-main); border: 1px solid var(--border); border-radius: 8px; padding: 10px;"></textarea>
          </div>
          <div style="display: flex; gap: 10px;">
            <button class="btn" style="width: auto; padding: 9px 22px; font-weight: 700;" onclick="saveServiceAccountKey()">💾 حفظ المفتاح</button>
            <button class="btn btn-secondary" style="width: auto; padding: 9px 18px; font-weight: 700;" onclick="toggleServiceAccountModal()">إغلاق</button>
          </div>
          <div id="saSaveStatus" style="font-size: 12.5px; display: none; font-weight: 700; margin-top: 6px;"></div>
        </div>
      </div>
    </div>

    <div class="grid-2">
      <div class="card">
        <div class="card-title">إرسال إشعار فوري لستارة الهاتف (FCM Push)</div>
        <div class="form-group">
          <label for="pushTarget">الجهة المستهدفة:</label>
          <select id="pushTarget">
            <option value="ALL">جميع الهواتف والمستخدمين المسجلين</option>
            <option value="TRIAL">مستخدمو النسخة التجريبية فقط</option>
            <option value="SUBSCRIBERS">المشتركون المفعّلون فقط</option>
          </select>
        </div>
        <div class="form-group">
          <label for="pushTitle">عنوان الإشعار:</label>
          <input type="text" id="pushTitle" placeholder="مثال: تنبيه هام من إدارة مُسَرِّب">
        </div>
        <div class="form-group">
          <label for="pushBody">نص الإشعار:</label>
          <textarea id="pushBody" rows="3" placeholder="اكتب نص الإشعار..."></textarea>
        </div>
        <div class="form-group">
          <label for="pushUrl">رابط يفتح عند الضغط على الإشعار (اختياري):</label>
          <input type="url" id="pushUrl" placeholder="مثال: رابط تحديث أو واتساب">
        </div>
        <button class="btn" id="btnSendPush" style="width: auto; padding: 10px 24px; font-weight: 700;" onclick="sendRealFcmPushNotification()">🚀 إرسال الإشعار</button>
        <div id="pushStatus" style="margin-top: 12px; font-size: 13px; text-align: center; display: none;"></div>
      </div>

      <div class="card">
        <div class="card-title">معلومات إشعارات FCM</div>
        <ul style="padding-right: 20px; line-height: 2; font-size: 13.5px; color: var(--text-muted);">
          <li><b>تصل والتطبيق مغلق:</b> يستقبل هاتف العميل الإشعار في الستارة العلوية عبر Google FCM مباشرة.</li>
          <li><b>تخزين سحابي:</b> يحفظ الإشعار في قاعدة البيانات ليظهر أيضاً داخل صندوق الوارد بالتطبيق.</li>
          <li><b>استهداف مرن:</b> إمكانية الإرسال لكافة المستخدمين أو لمجموعة محددة.</li>
        </ul>
      </div>
    </div>
  </div>

  <!-- Tab 2: Generator -->
