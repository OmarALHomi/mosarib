  <div id="tab-publicPortal" style="display: none;">
    <!-- Real Site Visitors & APK Downloads KPI Row -->
    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 16px; margin-bottom: 22px;">
      <div class="stat-card" style="background: var(--card-bg); border: 1px solid var(--card-border); padding: 18px;">
        <div class="stat-info">
          <h3 style="color: var(--primary); font-size: 13.5px; font-weight: 700;">إجمالي زوار صفحة الزوار (بيانات حقيقية)</h3>
          <div class="num" id="portalKpiTotalVisitors" style="color: var(--text-title); font-size: 28px; margin: 4px 0;">0</div>
          <small id="portalKpiTodayVisitors" style="color: var(--text-muted); font-size: 12px; font-weight: 600;">زوار اليوم: 0 زائر فريد</small>
        </div>
        <div class="stat-icon" style="color: #00BCD4; width: 44px; height: 44px;">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
            <circle cx="12" cy="12" r="3"></circle>
          </svg>
        </div>
      </div>

      <div class="stat-card" style="background: var(--card-bg); border: 1px solid var(--card-border); padding: 18px;">
        <div class="stat-info">
          <h3 style="color: var(--success); font-size: 13.5px; font-weight: 700;">عدد تنزيلات التطبيق من الموقع (APK)</h3>
          <div class="num" id="portalKpiTotalDownloads" style="color: var(--text-title); font-size: 28px; margin: 4px 0;">0</div>
          <small id="portalKpiTodayDownloads" style="color: var(--text-muted); font-size: 12px; font-weight: 600;">تنزيلات اليوم: 0 تحميل مباشر</small>
        </div>
        <div class="stat-icon" style="color: var(--success); width: 44px; height: 44px;">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
            <polyline points="7 10 12 15 17 10"></polyline>
            <line x1="12" y1="15" x2="12" y2="3"></line>
          </svg>
        </div>
      </div>
    </div>

    <!-- Full Website Content & Customization Control -->
    <form id="portalSettingsForm" onsubmit="handleSavePortalSettings(event)">
      <div class="grid-2">
        <!-- 1. Water Rates & Active Geographic Region -->
        <div class="card" style="border: 1px solid rgba(0, 188, 212, 0.45);">
          <div class="card-title">💧 بورصة أسعار ساعات الري والنطاق الجغرافي</div>
          
          <div class="form-group">
            <label for="pActiveRegionName">اسم المنطقة/المحافظات التي يعمل فيها التطبيق حالياً:</label>
            <input type="text" id="pActiveRegionName" placeholder="مثال: الجمهورية اليمنية (صنعاء، مأرب، الجوف، عمران)" required>
          </div>

          <div style="display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 10px;">
            <div class="form-group">
              <label for="pAvgPrice">متوسط سعر الساعة:</label>
              <input type="number" id="pAvgPrice" placeholder="7500" required>
            </div>
            <div class="form-group">
              <label for="pMinPrice">أقل سعر مسجل:</label>
              <input type="number" id="pMinPrice" placeholder="5500" required>
            </div>
            <div class="form-group">
              <label for="pMaxPrice">أعلى سعر مسجل:</label>
              <input type="number" id="pMaxPrice" placeholder="9500" required>
            </div>
          </div>

          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
            <div class="form-group">
              <label for="pCurrency">العملة المعتمدة:</label>
              <input type="text" id="pCurrency" value="ريال" required>
            </div>
            <div class="form-group">
              <label for="pWaterStatus">حالة منسوب المياه:</label>
              <select id="pWaterStatus">
                <option value="ABUNDANT">وفيرة ومرتفعة المنسوب</option>
                <option value="GOOD" selected>متوفرة ومستقرة</option>
                <option value="MODERATE">متوسطة وتستدعي الترشيد</option>
                <option value="SCARCE">شحيحة وطلب متزايد</option>
              </select>
            </div>
          </div>

          <div class="form-group">
            <label for="pRatesNote">ملاحظة وتوضيح الأسعار في الموقع:</label>
            <textarea id="pRatesNote" rows="2" placeholder="الأسعار تختلف بحسب عمق البئر، حجم المضخة، ونوع الطاقة المستخدمة (شمسية / ديزل)."></textarea>
          </div>

          <div class="form-group">
            <label for="pHeadlineAlert">الشريط الإخباري المتحرك أعلى الموقع للزوار:</label>
            <input type="text" id="pHeadlineAlert" placeholder="تنبيه أو إشعار يظهر في الشريط الإخباري العلوي">
          </div>
        </div>

        <!-- 2. Hero Content & Direct Contacts -->
        <div class="card" style="border: 1px solid rgba(0, 150, 166, 0.45);">
          <div class="card-title">📝 نصوص وهوية الواجهة الرئيسية والتواصل</div>

          <div class="form-group">
            <label for="pHeroBadge">شارة الواجهة العلوية:</label>
            <input type="text" id="pHeroBadge" placeholder="المنظومة الرقمية المعتمدة لملاك الآبار والموزعين">
          </div>

          <div class="form-group">
            <label for="pHeroTitle">العنوان الرئيسي للموقع:</label>
            <input type="text" id="pHeroTitle" placeholder="إدارة جلسات السقي وحسابات مياه الآبار بدقة واحترافية" required>
          </div>

          <div class="form-group">
            <label for="pHeroSubtitle">الوصف التعريفي بالمنظومة والتطبيق:</label>
            <textarea id="pHeroSubtitle" rows="3" placeholder="نظام ميداني متكامل يعمل بدون إنترنت لموزعي المياه وملاك الآبار الارتوازية..." required></textarea>
          </div>

          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
            <div class="form-group">
              <label for="pContactWhatsapp">رقم الواتساب للتفعيل والتواصل:</label>
              <input type="text" id="pContactWhatsapp" placeholder="967773712030" required>
            </div>
            <div class="form-group">
              <label for="pContactPhone">رقم الاتصال المباشر:</label>
              <input type="text" id="pContactPhone" placeholder="773712030" required>
            </div>
          </div>

          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
            <div class="form-group">
              <label for="pDeveloperName">اسم المطور / المالك:</label>
              <input type="text" id="pDeveloperName" value="المهندس عمر الحومي">
            </div>
            <div class="form-group">
              <label for="pDeveloperCompany">اسم المؤسسة / الشركة:</label>
              <input type="text" id="pDeveloperCompany" value="تُبع سوفت للحلول البرمجية الذكية (TubbaSoft)">
            </div>
          </div>

          <div class="form-group">
            <label for="pDeveloperEmail">البريد الإلكتروني الرسمي:</label>
            <input type="email" id="pDeveloperEmail" value="omar@tubbasoft.com">
          </div>
        </div>
      </div>

      <!-- 3. Section Visibility Switches -->
      <div class="card" style="margin-bottom: 24px; border: 1px solid rgba(0, 200, 83, 0.4);">
        <div class="card-title">👁️ التحكم بإظهار وإخفاء أقسام الموقع العام (تشغيل / إيقاف)</div>
        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; padding: 10px 0;">
          <label style="display: flex; align-items: center; gap: 10px; cursor: pointer; color: #FFF; font-weight: 700;">
            <input type="checkbox" id="pShowTopTicker" style="width: 20px; height: 20px; accent-color: var(--primary);" checked>
            <span>الشريط الإخباري العلوي</span>
          </label>
          <label style="display: flex; align-items: center; gap: 10px; cursor: pointer; color: #FFF; font-weight: 700;">
            <input type="checkbox" id="pShowRatesSection" style="width: 20px; height: 20px; accent-color: var(--primary);" checked>
            <span>قسم أسعار ساعات الري</span>
          </label>
          <label style="display: flex; align-items: center; gap: 10px; cursor: pointer; color: #FFF; font-weight: 700;">
            <input type="checkbox" id="pShowFeaturesSection" style="width: 20px; height: 20px; accent-color: var(--primary);" checked>
            <span>قسم مميزات التطبيق</span>
          </label>
          <label style="display: flex; align-items: center; gap: 10px; cursor: pointer; color: #FFF; font-weight: 700;">
            <input type="checkbox" id="pShowPlansSection" style="width: 20px; height: 20px; accent-color: var(--primary);" checked>
            <span>قسم خطط وتراخيص التطبيق</span>
          </label>
          <label style="display: flex; align-items: center; gap: 10px; cursor: pointer; color: #FFF; font-weight: 700;">
            <input type="checkbox" id="pShowDeveloperSection" style="width: 20px; height: 20px; accent-color: var(--primary);" checked>
            <span>قسم المطور والتواصل</span>
          </label>
          <label style="display: flex; align-items: center; gap: 10px; cursor: pointer; color: #FFF; font-weight: 700;">
            <input type="checkbox" id="pShowDownloadCta" style="width: 20px; height: 20px; accent-color: var(--primary);" checked>
            <span>صندوق التحميل المباشر السفلي</span>
          </label>
        </div>

        <div style="margin-top: 18px; display: flex; justify-content: flex-end;">
          <button type="submit" class="btn" id="savePortalSettingsBtn" style="width: auto; padding: 14px 38px; font-size: 15px; font-weight: 800; background: linear-gradient(135deg, #00BFA5, #00838F); box-shadow: 0 4px 18px rgba(0, 191, 165, 0.4);">
            💾 حفظ وتطبيق كافة تعديلات الموقع العام فوراً
          </button>
        </div>
      </div>
    </form>

      <!-- Add News & Articles to Public Portal -->
      <div class="card" style="border: 1px solid rgba(255, 179, 0, 0.4);">
        <div class="card-title">نشر خبر أو تقرير مائي جديد</div>
        <form id="portalNewsForm" onsubmit="handleCreatePortalNews(event)">
          <div class="form-group">
            <label for="newsTitleInput">عنوان الخبر أو التقرير الزراعي:</label>
            <input type="text" id="newsTitleInput" placeholder="مثال: تقرير استقرار أسعار ساعات الري في محافظة صنعاء" required>
          </div>
          <div class="form-group">
            <label for="newsCategoryInput">تصنيف التقرير:</label>
            <select id="newsCategoryInput">
              <option value="تقارير مياه الآبار">تقارير مياه الآبار</option>
              <option value="بورصة أسعار الري">بورصة أسعار الري</option>
              <option value="إرشادات زراعية وفنية">إرشادات زراعية وفنية</option>
              <option value="تحديثات المنظومة">تحديثات المنظومة</option>
            </select>
          </div>
          <div class="form-group">
            <label for="newsSummaryInput">ملخص وتفاصيل التقرير المنشور:</label>
            <textarea id="newsSummaryInput" rows="4" placeholder="اكتب ملخص الخبر الذي سيقرأه الزوار والمزارعون في الصفحة الرئيسية..." required></textarea>
          </div>
          <button type="submit" class="btn" style="background: linear-gradient(135deg, #FFB300, #FF8F00); color: #000; font-weight: 800;">
            نشر التقرير للعامة الآن
          </button>
        </form>

        <hr style="border: 0; border-top: 1px solid var(--border); margin: 20px 0;">

        <div style="font-weight: 800; font-size: 14px; margin-bottom: 12px; color: #FFF;">قائمة الأخبار المنشورة حالياً:</div>
        <div id="adminPortalNewsList" style="max-height: 250px; overflow-y: auto;">
          <div style="text-align: center; color: var(--text-muted); padding: 14px;">جاري تحميل الأخبار...</div>
        </div>
      </div>

      <!-- Manage Subscription Pricing & Discounts -->
      <div class="card" style="border: 1px solid rgba(255, 179, 0, 0.45); margin-top: 24px; background: linear-gradient(135deg, rgba(255, 179, 0, 0.06), rgba(18, 35, 43, 0.95));">
        <div class="card-title">🏷️ إدارة تسعير وباقات الاشتراك والخصومات (سنوي وشهرية)</div>
        <p style="font-size: 13px; color: var(--text-muted); margin-bottom: 16px;">
          الأسعار والخصومات المحددة هنا تُحفظ فوراً في قاعدة بيانات Firebase السحابية، وتنعكس مباشرة في كل من <b>الموقع الإلكتروني</b> و<b>تطبيق الأندرويد</b> و<b>رسائل طلبات الواتساب</b>.
        </p>

        <form id="subscriptionPricingForm" onsubmit="handleSaveSubscriptionPricing(event)">
          <div class="grid-2">
            <!-- Yearly Plan Pricing -->
            <div style="background: rgba(0, 0, 0, 0.25); border: 1px solid rgba(255, 179, 0, 0.3); border-radius: 10px; padding: 16px;">
              <h4 style="color: var(--accent); margin-bottom: 12px; font-size: 14px; font-weight: 800;">⭐ باقة الاشتراك السنوي الشامل:</h4>
              <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px;">
                <div class="form-group">
                  <label for="pricingYearlyPrice">السعر الفعلي المطلوب (بعد الخصم):</label>
                  <input type="text" id="pricingYearlyPrice" placeholder="مثال: 80" required>
                </div>
                <div class="form-group">
                  <label for="pricingYearlyOriginal">السعر السابق قبل الخصم (اختياري مشطوب):</label>
                  <input type="text" id="pricingYearlyOriginal" placeholder="مثال: 120">
                </div>
              </div>
              <div style="display: grid; grid-template-columns: 2fr 1fr; gap: 10px;">
                <div class="form-group">
                  <label for="pricingYearlyDiscount">نص شارة الخصم والتوفير:</label>
                  <input type="text" id="pricingYearlyDiscount" placeholder="مثال: وفر 33% (خصم 40$)">
                </div>
                <div class="form-group">
                  <label for="pricingYearlyCurrency">رمز العملة:</label>
                  <input type="text" id="pricingYearlyCurrency" value="$">
                </div>
              </div>
            </div>

            <!-- Monthly Plan Pricing -->
            <div style="background: rgba(0, 0, 0, 0.25); border: 1px solid rgba(0, 188, 212, 0.3); border-radius: 10px; padding: 16px;">
              <h4 style="color: #4DD0E1; margin-bottom: 12px; font-size: 14px; font-weight: 800;">📅 باقة الاشتراك الشهري:</h4>
              <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px;">
                <div class="form-group">
                  <label for="pricingMonthlyPrice">السعر الفعلي المطلوب (شهرياً):</label>
                  <input type="text" id="pricingMonthlyPrice" placeholder="مثال: 10" required>
                </div>
                <div class="form-group">
                  <label for="pricingMonthlyOriginal">السعر السابق قبل الخصم (اختياري مشطوب):</label>
                  <input type="text" id="pricingMonthlyOriginal" placeholder="مثال: 15">
                </div>
              </div>
              <div style="display: grid; grid-template-columns: 2fr 1fr; gap: 10px;">
                <div class="form-group">
                  <label for="pricingMonthlyDiscount">نص شارة الخصم (إن وجد):</label>
                  <input type="text" id="pricingMonthlyDiscount" placeholder="مثال: عرض خاص">
                </div>
                <div class="form-group">
                  <label for="pricingMonthlyCurrency">رمز العملة:</label>
                  <input type="text" id="pricingMonthlyCurrency" value="$">
                </div>
              </div>
            </div>
          </div>

          <div style="margin-top: 16px; display: flex; justify-content: flex-end;">
            <button type="submit" class="btn" id="btnSavePricing" style="width: auto; padding: 12px 30px; font-weight: 800; background: linear-gradient(135deg, #FFB300, #FF8F00); color: #000;">
              💾 حفظ ونشر الأسعار والخصومات في فايربيس والتطبيق والموقع فوراً
            </button>
          </div>
          <div id="pricingSaveStatus" style="margin-top: 10px; text-align: center; font-size: 13px; font-weight: bold; display: none;"></div>
        </form>
      </div>

      <!-- Manage Payment Accounts & Wallets -->
      <div class="card" style="border: 1px solid rgba(0, 191, 165, 0.4); margin-top: 24px;">
        <div class="card-title">💳 إدارة حسابات وطرق الدفع والتحويل البنكي</div>
        <p style="font-size: 13px; color: var(--text-muted); margin-bottom: 16px;">
          الحسابات والمحافظ المسجلة هنا يتم رفعها ومزامنتها تلقائياً مع سحابة فايربيس، وتظهر للعملاء في نافذة طلب الاشتراك بالموقع الرسمي وفي شاشة التفعيل بالتطبيق مباشرة مع إمكانية النسخ الفوري.
        </p>

        <!-- Existing Accounts List -->
        <div style="font-weight: 800; font-size: 14px; margin-bottom: 10px; color: #FFF;">قائمة الحسابات والمحافظ المتاحة حالياً:</div>
        <div id="adminPaymentMethodsList" style="display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 12px; margin-bottom: 20px;">
          <div style="text-align: center; color: var(--text-muted); padding: 14px; grid-column: 1 / -1;">جاري تحميل الحسابات...</div>
        </div>

        <hr style="border: 0; border-top: 1px solid var(--border); margin: 20px 0;">

        <!-- Form to Add New Account -->
        <div style="font-weight: 800; font-size: 14px; margin-bottom: 12px; color: var(--accent);">إضافة حساب أو محفظة بنكية جديدة:</div>
        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 12px; margin-bottom: 12px;">
          <div class="form-group" style="margin-bottom:0;">
            <label>اسم البنك أو المحفظة *</label>
            <input type="text" id="newPayBankName" placeholder="مثال: بنك الكريمي أو محفظة جوالي">
          </div>
          <div class="form-group" style="margin-bottom:0;">
            <label>نوع الحساب / العملة *</label>
            <select id="newPayCurrency">
              <option value="YER">ريال يمني (YER)</option>
              <option value="USD">دولار أمريكي (USD)</option>
              <option value="SAR">ريال سعودي (SAR)</option>
            </select>
          </div>
          <div class="form-group" style="margin-bottom:0;">
            <label>رقم الحساب / الآيبان / الهاتف *</label>
            <input type="text" id="newPayAccountNumber" placeholder="مثال: 123456789 أو 773712030">
          </div>
          <div class="form-group" style="margin-bottom:0;">
            <label>اسم صاحب الحساب *</label>
            <input type="text" id="newPayAccountHolder" value="عمر الحومي" placeholder="اسم المستفيد">
          </div>
          <div class="form-group" style="margin-bottom:0;">
            <label>رابط الشعار / الصورة (اختياري)</label>
            <input type="url" id="newPayLogoUrl" placeholder="https://... (رابط صورة الشعار)">
          </div>
          <div class="form-group" style="margin-bottom:0;">
            <label>ملاحظات التحويل (اختياري)</label>
            <input type="text" id="newPayNotes" placeholder="مثال: تحويل فوري عبر التطبيق">
          </div>
        </div>

        <div style="display: flex; gap: 12px; justify-content: flex-end; margin-top: 14px; flex-wrap: wrap;">
          <button type="button" class="btn" onclick="addNewPaymentMethod()" style="width: auto; padding: 10px 24px; font-weight: 800; background: linear-gradient(135deg, #00BFA5, #00838F);">
            ➕ إضافة الحساب إلى القائمة
          </button>
          <button type="button" class="btn" onclick="saveAllPaymentMethods()" style="width: auto; padding: 10px 28px; font-weight: 800; background: linear-gradient(135deg, #FFB300, #FF8F00); color: #000;">
            💾 حفظ ونشر الحسابات في فايربيس
          </button>
        </div>
      </div>
    </div>
  </div>

