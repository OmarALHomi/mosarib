package com.example.features.help

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.guide.GuidePreferences
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryAqua

data class HelpItem(
    val title: String,
    val question: String,
    val answer: String,
    val icon: ImageVector,
    val iconTint: Color,
    val tourTarget: String? = null,
    val actionText: String = "أرني في التطبيق"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onBack: () -> Unit,
    onNavigateToTour: (screenId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var expandedIndex by remember { mutableIntStateOf(-1) }

    val helpItems = remember {
        listOf(
            HelpItem(
                title = "تسجيل سقي بالصوت",
                question = "كيف أسجل دورة سقي بالصوت بسرعة؟",
                answer = "اضغط زر الميكروفون بالصفحة الرئيسية وقل عمليتك بطبيعتك، مثل: «سقي مع أحمد ساعتين من 10 آلاف دفع 5 آلاف». سيتعرف التطبيق آلياً على الاسم والمدة والسعر والمبلغ المدفوع.",
                icon = Icons.Default.Mic,
                iconTint = PrimaryTeal,
                tourTarget = GuidePreferences.TOUR_HOME
            ),
            HelpItem(
                title = "الفرق بين القبض والصرف",
                question = "ما الفرق بين سند القبض وسند الصرف؟",
                answer = "سند «القبض» يُسجل عند استلام كاش من المزارع ليخصم من دينه. أما سند «الصرف» فيُسجل للمصروفات وشراء الديزل وصيانة المضخات، أو تسليم مبالغ نقدية لمالك البئر.",
                icon = Icons.Default.Payments,
                iconTint = AccentEmerald,
                tourTarget = GuidePreferences.TOUR_VOUCHER_FORM
            ),
            HelpItem(
                title = "شراء الساعات وحساب المالك",
                question = "كيف يتم احتساب مشتريات البئر والمقاصة؟",
                answer = "سجّل ساعات التشغيل وسعر الشراء من زر شراء الساعات. وإذا سقى صاحب البئر لنفسه، تُخصم قيمة السقي من مستحقات الشراء آلياً عبر نظام المقاصة دون أي تعقيد.",
                icon = Icons.Default.WaterDrop,
                iconTint = SecondaryAqua,
                tourTarget = GuidePreferences.TOUR_PURCHASE_FORM
            ),
            HelpItem(
                title = "الكشف عبر واتساب",
                question = "كيف أرسل كشف الحساب للعميل عبر واتساب؟",
                answer = "افتح صفحة العميل واضغط على زر مشاركة PDF أو الملخص النصي أو الكشف الصوتي، ثم اختر واتساب لإرسال الحساب كاملاً للعميل بضغطة واحدة.",
                icon = Icons.Default.Share,
                iconTint = AccentGold,
                tourTarget = "customers"
            ),
            HelpItem(
                title = "النسخ الاحتياطي واستعادته",
                question = "كيف أحمي حساباتي من الضياع وأستعيدها؟",
                answer = "يمكنك تفعيل النسخ التلقائي السحابي مع Google Drive من شاشة الإعدادات، أو أخذ نسخة احتياطية محلية ومشاركتها في بريدك، مع إمكانية الاستعادة في أي وقت بنقرة واحدة.",
                icon = Icons.Default.Backup,
                iconTint = PrimaryTeal,
                tourTarget = "backup"
            ),
            HelpItem(
                title = "تعديل وحذف العمليات",
                question = "هل يمكنني تعديل عملية أو استرجاع المحذوفات؟",
                answer = "نعم، من شاشة «العمليات» اضغط على أي سقي أو سند لتعديل بياناته، أو اسحبه واضغط مطولاً لحذفه مع إمكانية استرجاعه في أي وقت من سلة المحذوفات.",
                icon = Icons.Default.DeleteSweep,
                iconTint = Color(0xFFE53935),
                tourTarget = GuidePreferences.TOUR_NAV
            ),
            HelpItem(
                title = "التقارير والكشوفات الشاملة",
                question = "كيف أستعرض الأرباح والديون المتبقية؟",
                answer = "من تبويب «التقارير» ستجد ملخصاً شاملاً لإجمالي الإيرادات والمصروفات وصافي الأرباح وديون المزارعين ومستحقات أصحاب الآبار، مع إمكانية طباعة تقرير PDF شامل بخط كايرو.",
                icon = Icons.Default.Assessment,
                iconTint = AccentGold,
                tourTarget = "reports"
            ),
            HelpItem(
                title = "الدعم الفني والمساعدة",
                question = "كيف أتواصل مع الدعم الفني للاستفسار؟",
                answer = "فريق التطوير في خدمتك دائماً عبر واتساب أو الاتصال المباشر لتقديم المساعدة والإجابة على أي تساؤل أو مقترح لتطوير التطبيق.",
                icon = Icons.Default.Headphones,
                iconTint = PrimaryTeal,
                tourTarget = "support",
                actionText = "تواصل مع الدعم الفني"
            )
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "مركز المساعدة والتعليمات",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PrimaryTeal.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = null,
                                tint = PrimaryTeal,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "دليلك السريع لإتقان المسرب",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "إجابات واضحة لأهم الأسئلة الشائعة مع إمكانية خوض الجولة التوضيحية داخل التطبيق.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            itemsIndexed(helpItems, key = { _, item -> item.title }) { index, item ->
                val isExpanded = expandedIndex == index

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    // One animation only (AnimatedVisibility below); a second size animation and an
                    // outer shadow made the card jump and clip while opening and closing.
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedIndex = if (isExpanded) -1 else index
                            }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(item.iconTint.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = item.iconTint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = item.question,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "طي" else "توسيع",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                            ) {
                                Text(
                                    text = item.answer,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        lineHeight = 22.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)
                                )

                                if (item.tourTarget != null) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = {
                                            if (item.tourTarget == "support") {
                                                try {
                                                    val url = "https://wa.me/${com.example.core.license.LicenseManager.DEVELOPER_PHONE}?text=${Uri.encode("مرحباً، أحتاج مساعدة في تطبيق المسرب")}"
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                    context.startActivity(intent)
                                                } catch (_: Exception) {
                                                    Toast.makeText(context, "تطبيق واتساب غير مثبت", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                onNavigateToTour(item.tourTarget)
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = PrimaryTeal,
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Text(
                                            text = item.actionText,
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
