package com.example.features.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.license.LicenseManager
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal

data class TourStep(
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val bgColor: Color,
    val tip: String
)

@Composable
fun HomeShowcaseTourDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val steps = remember {
        listOf(
            TourStep(
                title = "زر «سقي جديد»",
                subtitle = "تسجيل دورة ري لمزارع",
                description = "استخدم هذا الزر لبدء أو توثيق جلسة سقي. يمكنك اختيار المزارع وبدء المؤقت المباشر، أو إدخال الساعات يدوياً وسيقوم التطبيق بحساب التكلفة تلقائياً بحسب سعر الساعة المعتمد.",
                icon = Icons.Default.WaterDrop,
                color = PrimaryTeal,
                bgColor = Color(0xFFE0F2FE),
                tip = "نصيحة: يمكنك إيقاف السقي مؤقتاً عند انقطاع الماء أو التبديل ثم الاستئناف بسهولة!"
            ),
            TourStep(
                title = "زر «قبض»",
                subtitle = "سند تحصيل كاش",
                description = "عند استلامك لأي مبلغ نقدي من المزارع، اضغط هنا لإنشاء سند قبض. يتم خصم المبلغ فورياً من مديونية المزارع لتبقى الحسابات واضحة وموثقة.",
                icon = Icons.Default.Payments,
                color = AccentEmerald,
                bgColor = Color(0xFFE8F5E9),
                tip = "نصيحة: يمكنك مشاركة إشعار القبض مع المزارع مباشرة فور الحفظ!"
            ),
            TourStep(
                title = "زر «صرف»",
                subtitle = "سند مصروفات ونفقات",
                description = "سجّل هنا كافة المصروفات التشغيلية لمشروع الماء مثل شراء الديزل، فواتير الكهرباء، الزيوت، صيانة الغطاس والمواسير، وأجور العمال.",
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                color = Color(0xFFE53935),
                bgColor = Color(0xFFFFEBEE),
                tip = "نصيحة: قيد المصروفات أولاً بأول يمنحك تقارير صافي أرباح دقيقة بنسبة 100%!"
            ),
            TourStep(
                title = "زر «شراء»",
                subtitle = "شراء ساعات ماء من بئر",
                description = "إذا كنت وسيطاً أو اشتريت ساعات ري من بئر مجاور لسقي مزارعيك، وثّق الشراء من هنا لتسجيل عدد الساعات وتكلفتها وحساب رصيد صاحب البئر بدقة.",
                icon = Icons.Default.LocalGasStation,
                color = Color(0xFF7E22CE),
                bgColor = Color(0xFFF3E8FF),
                tip = "نصيحة: يمكنك ربط ساعات الشراء باسم صاحب البئر ومعرفة حسابه المتبقي لديك."
            ),
            TourStep(
                title = "زر «عميل جديد»",
                subtitle = "إضافة مزارع أو صاحب بئر",
                description = "أضف بيانات عملائك الجدد؛ اختر نوع العميل (مزارع أو صاحب بئر)، وأدخل اسمه ورقم هاتفه والمزرعة، مع إمكانية تخصيص سعر ساعة مختلف لكل مزارع.",
                icon = Icons.Default.PersonAdd,
                color = Color(0xFF0284C7),
                bgColor = Color(0xFFE0F2FE),
                tip = "نصيحة: يمكنك الاتصال بالعميل أو مراسلته عبر واتساب بنقرة واحدة من صفحته!"
            ),
            TourStep(
                title = "زر «النسخ الاحتياطي»",
                subtitle = "حفظ واسترجاع البيانات سحابياً ومحلياً",
                description = "يتيح لك حفظ نسخة احتياطية من كافة سجلات السقي والعملاء والمصروفات على حسابك في Google Drive أو ذاكرة الهاتف، واسترجاعها بأي وقت بنقرة واحدة لحماية أعمالك من الضياع.",
                icon = Icons.Default.CloudSync,
                color = Color(0xFF16A34A),
                bgColor = Color(0xFFDCFCE7),
                tip = "نصيحة: احرص على تفعيل النسخ التلقائي في Google Drive ليتم حفظ أعمالك بأمان دورياً!"
            )
        )
    }

    var currentStepIdx by remember { mutableIntStateOf(0) }
    val currentStep = steps[currentStepIdx]
    val isLast = currentStepIdx == steps.size - 1

    fun finishTour() {
        LicenseManager.setHomeTourCompleted(context, true)
        onDismiss()
    }

    // Modal dialog overlay to guarantee rendering above the entire screen
    Dialog(
        onDismissRequest = { finishTour() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* block outside clicks */ },
            contentAlignment = Alignment.Center
        ) {
            Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .shadow(16.dp, RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header with step number & close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(currentStep.color.copy(alpha = 0.14f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "خطوة ${currentStepIdx + 1} من ${steps.size}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = currentStep.color,
                                fontSize = 11.5.sp
                            )
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = { finishTour() },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(
                                text = "تخطي الدليل",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        IconButton(
                            onClick = { finishTour() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Content Animated Transition
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        fadeIn(tween(220)) togetherWith fadeOut(tween(180))
                    },
                    label = "tour_step_content"
                ) { step ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Icon circle
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(step.bgColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = step.icon,
                                contentDescription = null,
                                tint = step.color,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Title
                        Text(
                            text = step.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        )

                        Text(
                            text = step.subtitle,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = step.color,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Description
                        Text(
                            text = step.description,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 21.sp,
                                fontSize = 13.5.sp
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Tip pill
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(step.color.copy(alpha = 0.08f))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "💡 " + step.tip,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.sp
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Progress dots
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(steps.size) { idx ->
                        val isSelected = currentStepIdx == idx
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .height(6.dp)
                                .width(if (isSelected) 20.dp else 6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (isSelected) currentStep.color else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Buttons (Next & Back)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (currentStepIdx > 0) {
                        OutlinedButton(
                            onClick = { currentStepIdx -= 1 },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("السابق", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            if (isLast) {
                                finishTour()
                            } else {
                                currentStepIdx += 1
                            }
                        },
                        modifier = Modifier.weight(if (currentStepIdx > 0) 1.5f else 1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLast) AccentEmerald else currentStep.color
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isLast) "تم، ابدأ العمل الآن! 🎉" else "التالي",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (isLast) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}
}
