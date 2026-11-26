  <div id="tab-push" style="display: none;">
    <!-- Clear 3-Channel Overview Switcher -->
    <div class="card" style="margin-bottom: 20px; border: 1px solid var(--card-border);">
      <div class="card-title" style="margin-bottom: 12px;">مركز الإشعارات والتحديثات — اختر القناة المناسبة</div>
      <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 12px;">
        <div style="padding: 12px 14px; border-radius: 12px; background: var(--input-bg); border: 1px solid var(--card-border); display: flex; flex-direction: column; justify-content: space-between; gap: 10px;">
          <div>
            <div style="font-weight: 800; font-size: 14px; color: var(--text-title); margin-bottom: 4px;">📢 إعلان توجيهي / تنبيه داخل التطبيق</div>
            <div style="font-size: 12px; color: var(--text-muted); line-height: 1.6;">رسالة أو تنبيه يظهر داخل التطبيق عند فتحه دون قفل العمل (INFO / WARNING).</div>
          </div>
          <button type="button" class="btn btn-outline btn-compact" onclick="focusNoticeChannel('INFO')">فتح نموذج الإعلان</button>
        </div>
        <div style="padding: 12px 14px; border-radius: 12px; background: var(--input-bg); border: 1px solid var(--card-border); display: flex; flex-direction: column; justify-content: space-between; gap: 10px;">
          <div>
            <div style="font-weight: 800; font-size: 14px; color: var(--danger); margin-bottom: 4px;">🔒 تحديث إجباري لقفل النسخ القديمة</div>
            <div style="font-size: 12px; color: var(--text-muted); line-height: 1.6;">يقفل التطبيق على الإصدارات الأقدم أو المحددة ويجبر المستخدم على التحديث (UPDATE).</div>
          </div>
          <button type="button" class="btn btn-outline btn-compact" style="border-color: var(--danger); color: var(--danger);" onclick="focusNoticeChannel('UPDATE')">فتح التحديث الإجباري</button>
        </div>
        <div style="padding: 12px 14px; border-radius: 12px; background: var(--input-bg); border: 1px solid var(--card-border); display: flex; flex-direction: column; justify-content: space-between; gap: 10px;">
          <div>
            <div style="font-weight: 800; font-size: 14px; color: var(--primary-light); margin-bottom: 4px;">🔔 إشعار فوري للخلفية (Push / FCM)</div>
            <div style="font-size: 12px; color: var(--text-muted); line-height: 1.6;">يصل لستارة الهاتف بالصوت والاهتزاز حتى لو كان التطبيق مغلقاً تماماً.</div>
          </div>
          <button type="button" class="btn btn-outline btn-compact" onclick="focusNoticeChannel('PUSH')">فتح إشعار الستارة</button>
        </div>
      </div>
    </div>

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

    <div class="grid-2" id="fcmPushFormCard">
      <div class="card">
        <div class="card-title">🔔 إشعار فوري للخلفية (Push / FCM — ستارة الهاتف)</div>
        <div class="form-group">
          <label for="pushTarget">الجهة المستهدفة:</label>
          <select id="pushTarget">
            <option value="ALL">جميع الهواتف والمستخدمين المسجلين</option>
            <option value="TRIAL">مستخدمو النسخة التجريبية فقط</option>
            <option value="SUBSCRIBERS">المشتركون المفعّلون فقط</option>
            <option value="VERSION">مستخدمو إصدار محدد من التطبيق</option>
          </select>
        </div>
        <div class="form-group" id="pushVersionBlock" style="display:none; padding:12px; border:1px dashed var(--border); border-radius:10px;">
          <label for="pushVersionMode">الإصدار المستهدف (رقم البناء VersionCode):</label>
          <select id="pushVersionMode" onchange="refreshPushAudience()">
            <option value="EQ">إصدار محدد</option>
            <option value="LT">أقدم من</option>
            <option value="RANGE">من … إلى</option>
          </select>
          <div style="display:flex; gap:8px; margin-top:8px;">
            <input type="number" id="pushVersionFrom" min="1" placeholder="رقم البناء، مثل 6" oninput="refreshPushAudience()">
            <input type="number" id="pushVersionTo" min="1" placeholder="إلى (للمدى فقط)" oninput="refreshPushAudience()">
          </div>
          <div id="pushVersionUsage" style="font-size:12px; color:var(--text-muted); margin-top:8px; line-height:1.8;"></div>
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
          <input type="text" id="pushUrl" placeholder="https://... يفتح في المتصفح، أو mosarib://settings/backup يفتح شاشة في التطبيق" oninput="renderPushPreview()">
          <label style="display:flex; align-items:center; gap:8px; margin-top:8px; font-weight:normal; cursor:pointer;">
            <input type="checkbox" id="pushOpenInApp" style="width:auto;"> فتح التطبيق عند الضغط بدلاً من الرابط (يبقى زر «فتح الرابط» في الإشعار)
          </label>
          <small style="color: var(--text-muted); font-size: 11px;">شاشات التطبيق: mosarib://home، operations، customers، reports، settings، settings/backup</small>
        </div>
        <div class="form-group">
          <label for="pushImageUrl">صورة مع الإشعار (اختياري):</label>
          <div style="display:flex; gap:8px; flex-wrap:wrap; align-items:center;">
            <input type="url" id="pushImageUrl" placeholder="https://... أو ارفع صورة" style="flex:1; min-width:200px;" oninput="previewNoticeImage('push')">
            <input type="file" id="pushImageFile" accept="image/jpeg,image/png,image/webp" style="display:none;" onchange="uploadNoticeImage('push')">
            <button type="button" class="btn btn-outline" style="width:auto; padding:8px 14px;" onclick="document.getElementById('pushImageFile').click()">🖼️ رفع صورة</button>
          </div>
          <small style="color: var(--text-muted); font-size: 11px;">JPG أو PNG أو WEBP، حتى 1 ميجابايت.</small>
          <img id="pushImagePreview" alt="" style="display:none; margin-top:8px; max-width:100%; max-height:160px; border-radius:12px; object-fit:cover;">
        </div>
        <div id="pushPreview" style="margin-bottom:12px;"></div>
        <div id="pushAudience" style="font-size:13px; font-weight:700; color:var(--accent); margin-bottom:10px;"></div>
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
