  <!-- Tab: Royal Analytics & Interactive Visual Charts -->
  <div id="tab-analytics" style="display: none;">
    <!-- Top KPI Row -->
    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px; margin-bottom: 24px;">
      <div class="stat-card" style="background: linear-gradient(135deg, rgba(0,131,143,0.2), rgba(18,35,43,0.9)); border: 1px solid rgba(0,188,212,0.4);">
        <div class="stat-info">
          <h3 style="color: #4DD0E1;">إجمالي العمليات المسجلة سحابياً</h3>
          <div class="num" id="kpiTotalOperations" style="color: #E0F7FA;">0</div>
          <small style="color: var(--text-muted); font-size: 11px;">مجموع دورات الري + السندات + الآبار</small>
        </div>
        <div class="stat-icon" style="color: #00BCD4;">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"></polygon>
          </svg>
        </div>
      </div>
      <div class="stat-card" style="background: linear-gradient(135deg, rgba(76,175,80,0.15), rgba(18,35,43,0.9)); border: 1px solid rgba(76,175,80,0.4);">
        <div class="stat-info">
          <h3 style="color: #81C784;">نسبة تحويل المشتركين</h3>
          <div class="num" id="kpiConversionRate" style="color: #C8E6C9;">0%</div>
          <small style="color: var(--text-muted); font-size: 11px;">نسبة من دفعوا واشتركوا مقارنة بالإجمالي</small>
        </div>
        <div class="stat-icon" style="color: #4CAF50;">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"></polyline>
            <polyline points="17 6 23 6 23 12"></polyline>
          </svg>
        </div>
      </div>
      <div class="stat-card" style="background: linear-gradient(135deg, rgba(255,179,0,0.15), rgba(18,35,43,0.9)); border: 1px solid rgba(255,179,0,0.4);">
        <div class="stat-info">
          <h3 style="color: #FFD54F;">المستخدمون النشطون (أونلاين)</h3>
          <div class="num" id="kpiOnlineUsers" style="color: #FFF9C4;">0</div>
          <small style="color: var(--text-muted); font-size: 11px;">متصلون الآن خلال آخر 24 ساعة</small>
        </div>
        <div class="stat-icon" style="color: #FFB300;">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M22 12h-4l-3 9L9 3l-3 9H2"></path>
          </svg>
        </div>
      </div>
      <div class="stat-card" style="background: linear-gradient(135deg, rgba(156,39,176,0.15), rgba(18,35,43,0.9)); border: 1px solid rgba(156,39,176,0.4);">
        <div class="stat-info">
          <h3 style="color: #BA68C8;">التحديثات والإصدار الأحدث</h3>
          <div class="num" id="kpiLatestVersion" style="font-size: 20px; color: #F3E5F5;">v1.0.0</div>
          <small style="color: var(--text-muted); font-size: 11px;">إصدار التطبيق المنتشر حالياً</small>
        </div>
        <div class="stat-icon" style="color: #AB47BC;">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path>
            <polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline>
            <line x1="12" y1="22.08" x2="12" y2="12"></line>
          </svg>
        </div>
      </div>
    </div>

    <!-- Charts Grid 1: Subscriptions & Operations Distribution -->
    <div class="grid-2" style="margin-bottom: 24px;">
      <div class="card" style="border: 1px solid rgba(0, 150, 166, 0.35); box-shadow: 0 10px 30px rgba(0,0,0,0.4);">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
          <div class="card-title" style="margin-bottom: 0;">توزيع حالة التراخيص والاشتراكات</div>
          <span style="font-size: 11px; background: rgba(0,188,212,0.15); color: #00BCD4; padding: 4px 8px; border-radius: 6px;">مباشر من فايربيس</span>
        </div>
        <div style="height: 280px; position: relative;">
          <canvas id="chartSubscriptionDistribution"></canvas>
        </div>
      </div>

      <div class="card" style="border: 1px solid rgba(255, 179, 0, 0.35); box-shadow: 0 10px 30px rgba(0,0,0,0.4);">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
          <div class="card-title" style="margin-bottom: 0; color: #FFB300;">استهلاك العمليات في النسخ التجريبية</div>
          <span style="font-size: 11px; background: rgba(255,179,0,0.15); color: #FFB300; padding: 4px 8px; border-radius: 6px;">قرب نفاد التجربة</span>
        </div>
        <div style="height: 280px; position: relative;">
          <canvas id="chartOperationsBucket"></canvas>
        </div>
      </div>
    </div>

    <!-- Charts Grid 2: Timeline Activity & Devices Breakdown -->
    <div class="grid-2">
      <div class="card" style="border: 1px solid rgba(76, 175, 80, 0.35); box-shadow: 0 10px 30px rgba(0,0,0,0.4);">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
          <div class="card-title" style="margin-bottom: 0; color: #81C784;">نشاط وتسجيل المستخدمين الجدد (Timeline)</div>
          <span style="font-size: 11px; background: rgba(76,175,80,0.15); color: #81C784; padding: 4px 8px; border-radius: 6px;">حركة التسجيل اليومية</span>
        </div>
        <div style="height: 280px; position: relative;">
          <canvas id="chartUsersTimeline"></canvas>
        </div>
      </div>

      <div class="card" style="border: 1px solid rgba(171, 71, 188, 0.35); box-shadow: 0 10px 30px rgba(0,0,0,0.4);">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
          <div class="card-title" style="margin-bottom: 0; color: #CE93D8;">انتشار إصدارات هواتف الأندرويد والتطبيق</div>
          <span style="font-size: 11px; background: rgba(171,71,188,0.15); color: #CE93D8; padding: 4px 8px; border-radius: 6px;">Android & App Versions</span>
        </div>
        <div style="height: 280px; position: relative;">
          <canvas id="chartAppVersions"></canvas>
        </div>
      </div>
    </div>
  </div>

  <!-- Tab: Subscription Requests (طلبات الاشتراك الواردة من التطبيق) -->
