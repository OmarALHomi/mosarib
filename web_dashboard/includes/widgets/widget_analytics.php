  <!-- Tab: Royal Analytics & Interactive Visual Charts -->
  <div id="tab-analytics" style="display: none;">
    <!-- Top KPI Row (Theme Harmonious: Light & Dark) -->
    <div class="kpi-grid">
      <div class="kpi-card cyan">
        <div class="stat-info">
          <h3>إجمالي العمليات المسجلة سحابياً</h3>
          <div class="num" id="kpiTotalOperations">0</div>
          <small style="color: var(--text-muted); font-size: 11px;">مجموع دورات الري + السندات + الآبار</small>
        </div>
        <div class="stat-icon">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"></polygon>
          </svg>
        </div>
      </div>
      <div class="kpi-card green">
        <div class="stat-info">
          <h3>نسبة تحويل المشتركين</h3>
          <div class="num" id="kpiConversionRate">0%</div>
          <small style="color: var(--text-muted); font-size: 11px;">نسبة من دفعوا واشتركوا مقارنة بالإجمالي</small>
        </div>
        <div class="stat-icon">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"></polyline>
            <polyline points="17 6 23 6 23 12"></polyline>
          </svg>
        </div>
      </div>
      <div class="kpi-card amber">
        <div class="stat-info">
          <h3>المستخدمون النشطون (أونلاين)</h3>
          <div class="num" id="kpiOnlineUsers">0</div>
          <small style="color: var(--text-muted); font-size: 11px;">متصلون الآن خلال آخر 24 ساعة</small>
        </div>
        <div class="stat-icon">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M22 12h-4l-3 9L9 3l-3 9H2"></path>
          </svg>
        </div>
      </div>
      <div class="kpi-card purple">
        <div class="stat-info">
          <h3>التحديثات والإصدار الأحدث</h3>
          <div class="num" id="kpiLatestVersion" style="font-size: 20px;">v1.0.0</div>
          <small style="color: var(--text-muted); font-size: 11px;">إصدار التطبيق المنتشر حالياً</small>
        </div>
        <div class="stat-icon">
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
      <div class="card" style="border: 1px solid var(--card-border); box-shadow: var(--card-shadow);">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
          <div class="card-title" style="margin-bottom: 0;">توزيع حالة التراخيص والاشتراكات</div>
          <span style="font-size: 11px; background: rgba(0,188,212,0.15); color: var(--primary); padding: 4px 8px; border-radius: 6px; font-weight: 700;">مباشر من فايربيس</span>
        </div>
        <div style="height: 280px; position: relative;">
          <canvas id="chartSubscriptionDistribution"></canvas>
        </div>
      </div>

      <div class="card" style="border: 1px solid var(--card-border); box-shadow: var(--card-shadow);">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
          <div class="card-title" style="margin-bottom: 0;">استهلاك العمليات في النسخ التجريبية</div>
          <span style="font-size: 11px; background: rgba(255,179,0,0.15); color: var(--accent); padding: 4px 8px; border-radius: 6px; font-weight: 700;">قرب نفاد التجربة</span>
        </div>
        <div style="height: 280px; position: relative;">
          <canvas id="chartOperationsBucket"></canvas>
        </div>
      </div>
    </div>

    <!-- Charts Grid 2: Timeline Activity & Devices Breakdown -->
    <div class="grid-2">
      <div class="card" style="border: 1px solid var(--card-border); box-shadow: var(--card-shadow);">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
          <div class="card-title" style="margin-bottom: 0;">نشاط وتسجيل المستخدمين الجدد (Timeline)</div>
          <span style="font-size: 11px; background: rgba(76,175,80,0.15); color: var(--success); padding: 4px 8px; border-radius: 6px; font-weight: 700;">حركة التسجيل اليومية</span>
        </div>
        <div style="height: 280px; position: relative;">
          <canvas id="chartUsersTimeline"></canvas>
        </div>
      </div>

      <div class="card" style="border: 1px solid var(--card-border); box-shadow: var(--card-shadow);">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
          <div class="card-title" style="margin-bottom: 0;">انتشار إصدارات هواتف الأندرويد والتطبيق</div>
          <span style="font-size: 11px; background: rgba(171,71,188,0.15); color: #AB47BC; padding: 4px 8px; border-radius: 6px; font-weight: 700;">Android & App Versions</span>
        </div>
        <div style="height: 280px; position: relative;">
          <canvas id="chartAppVersions"></canvas>
        </div>
      </div>
    </div>
  </div>

  <!-- Tab: Subscription Requests (طلبات الاشتراك الواردة من التطبيق) -->
