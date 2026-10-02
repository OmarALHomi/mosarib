package com.example.features.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryAqua

data class RoleItem(
    val code: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val accentColor: Color
)

val SYSTEM_ROLES = listOf(
    RoleItem("MUSRIB", "مسرب ماء", "أوزع الماء، أدير جلسات السقي، وأحصل أتعاب الري", "💧", PrimaryTeal),
    RoleItem("FARMER", "مزارع / صاحب أرض", "أمتلك مزارع، أتابع كشوفاتي، وأسوق محصولي", "🌾", AccentEmerald),
    RoleItem("DALLAL", "دلال / وسيط زراعي", "أتوسط في بيع المحاصيل، أنشر العروض، وأبرم الصلح", "🤝", AccentGold),
    RoleItem("BUYER", "مشتري / مجبري", "أشتري المحاصيل والثمار وأتابع الأقساط والدفعات", "📦", AccentAmber)
)

@Composable
fun RoleSelectionScreen(
    initialName: String = "",
    initialPhone: String = "",
    initialVillage: String = "",
    onComplete: (name: String, phone: String, village: String, primaryRole: String, activeRoles: String) -> Unit
) {
    var primaryRole by remember { mutableStateOf("MUSRIB") }
    var activeRoles by remember { mutableStateOf(setOf("MUSRIB", "FARMER", "DALLAL", "BUYER")) }

    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone) }
    var village by remember { mutableStateOf(initialVillage) }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            PrimaryTeal.copy(alpha = 0.05f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Emblem
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .shadow(12.dp, CircleShape, spotColor = PrimaryTeal)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(PrimaryTeal, AccentEmerald)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🌾", fontSize = 36.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "مرحباً بك في «جِربة»",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Text(
                    text = "المنظومة الزراعية الشاملة وإدارة الري والوساطة",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "اختر دورك الأساسي في العمل الزراعي:",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Role Cards Grid
                SYSTEM_ROLES.forEach { role ->
                    val isSelected = primaryRole == role.code
                    Card(
                        onClick = {
                            primaryRole = role.code
                            activeRoles = activeRoles + role.code
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) role.accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(18.dp)
                            ),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) role.accentColor.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(role.accentColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(role.icon, fontSize = 22.sp)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = role.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) role.accentColor else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = role.subtitle,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(role.accentColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✓", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Profile Fields
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "بيانات حسابك (للتوثيق والتواصل المباشر):",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("الاسم الكامل أو الكنية") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("رقم الهاتف (واتساب)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )

                        OutlinedTextField(
                            value = village,
                            onValueChange = { village = it },
                            label = { Text("القرية / العزلة / المديرية") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Submit Button
                Button(
                    onClick = {
                        val finalRoles = activeRoles.joinToString(",")
                        onComplete(name.trim(), phone.trim(), village.trim(), primaryRole, finalRoles)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) {
                    Text(
                        text = "بدء استخدام منظومة جِربة 🚀",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
