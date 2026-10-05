package com.baynana.features.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baynana.ui.components.InfoPill

/**
 * «المزيد»: كل ما ليس في التدفّق اليومي — ومرتّب كما يفكر المستخدم لا كما ترتّب الشيفرة.
 *
 * الشاشة تستدعي الشاشات القائمة (الصلح، مزرعتي، التقارير، النسخ الاحتياطي، الإعدادات، حول) بلا
 * إعادة بناء: ما بُني سابقًا يبقى، والمفردات تُنقّى تدريجيًا (حزمة د٦).
 */
@Composable
fun MoreScreen(
    onOpenMigration: () -> Unit,
    onOpenDeals: () -> Unit = {},
    onOpenFarmAccounting: () -> Unit = {},
    onOpenReports: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    /** «حالة المزامنة»: حقيقة كل حركة وما العمل فيها. */
    onOpenSyncStatus: () -> Unit = {},
    /** «التسليم بلا إنترنت»: ملفّ/رمز يمرّ في واتساب بلا حساب ولا خادم (ح١٩). */
    onOpenHandover: () -> Unit = {},
    /** «التحديث»: فحص ملفّ الإصدار الموقّع، ثم تنزيل بزر (ح٢٠). */
    onOpenUpdate: () -> Unit = {},
    onOpenCatalog: (() -> Unit)? = null
) {
    data class Item(
        val icon: ImageVector,
        val title: String,
        val subtitle: String,
        val onClick: () -> Unit,
        val comingSoon: Boolean = false
    )

    val menuItems = listOf(
        Item(Icons.Default.Gavel, "الصلح والأقساط", "بيان الصلح، الأقساط، وسعاية الدلال", onOpenDeals),
        Item(Icons.Default.Agriculture, "مزرعتي", "مصروفات المزرعة في دفترك الخاص", onOpenFarmAccounting),
        Item(Icons.Default.Analytics, "التقارير", "خلاصات وطباعة الكشوف", onOpenReports),
        Item(
            Icons.Default.Backup, "النسخ الاحتياطي",
            "النسخ يعمل من الإعدادات؛ ومعاينة الاستعادة تُبنى في حزمة قادمة",
            {},
            comingSoon = true
        ),
        Item(Icons.AutoMirrored.Filled.MenuBook, "الترحيل من الدفتر القديم", "الجرد والقرار قبل أي نقل", onOpenMigration),
        Item(
            Icons.Default.Sync,
            "حالة المزامنة",
            "لكل حركة: محفوظة في جهازك، أُرسلت، أُقرّت، أو فشلت ومعها الحل",
            onOpenSyncStatus
        ),
        Item(
            Icons.Default.ImportExport,
            "التسليم بلا إنترنت",
            "جهّز حزمة من حركاتك وأرسلها في واتساب، أو استورد حزمة وصلتك — بلا حساب ولا شبكة",
            onOpenHandover
        ),
        Item(
            Icons.Default.SystemUpdate,
            "التحديث",
            "افحص ملفّ الإصدار الموقّع، ونزّل آخر نسخة زرًّا — بلا تنزيل صامت وبلا مفاجآت",
            onOpenUpdate
        ),
        Item(Icons.Default.Settings, "الإعدادات", "الاسم، الحماية، الوضع الليلي", onOpenSettings),
        Item(Icons.Default.Info, "حول «بيننا»", "ما التطبيق وما لا يفعله", onOpenAbout)
    ).let { base ->
        // شاشة عيّنات المكوّنات: للمراجعة أثناء التطوير، ولا تظهر للمستخدم النهائي إطلاقًا.
        if (onOpenCatalog == null) base else base + Item(
            Icons.Default.Analytics,
            "عيّنات المكوّنات (للمراجعة)",
            "شاشة تطوير: كل حالات القيد والألوان والمكوّنات في صفحة واحدة",
            onOpenCatalog
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(top = 14.dp)) {
                Text("المزيد", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text(
                    "«بيننا» دفتر حسابات محلي أولًا. لا نطلب مالًا، ولا نحجب بياناتك عند أي إيقاف، " +
                        "ولا نعرض ديونك لأحد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(menuItems.size) { index ->
            val item = menuItems[index]
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !item.comingSoon, onClick = item.onClick),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            item.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (item.comingSoon) {
                        InfoPill(
                            text = "قريبًا",
                            container = MaterialTheme.colorScheme.surfaceVariant,
                            onContainer = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}
