package com.example.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.PrimaryTeal
import com.google.android.gms.auth.api.signin.GoogleSignInAccount

/**
 * نافذة احترافية لطلب اسم ورقم المستخدم وربطهما بحساب Google Drive للنسخ الاحتياطي السحابي.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleBackupAccountDialog(
    initialName: String = "",
    initialPhone: String = "",
    currentGoogleAccount: GoogleSignInAccount? = null,
    onDismiss: () -> Unit,
    onProceed: (name: String, phone: String, shouldLaunchSignIn: Boolean) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = if (isDark) Color(0xFF101B20) else Color.White,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = if (isDark) Color(0xFF334B56) else Color(0xFFCBD5E1))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // أيقونة سحابة Google Drive المتألقة
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF16A34A).copy(alpha = 0.2f),
                                PrimaryTeal.copy(alpha = 0.25f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = null,
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "النسخ الاحتياطي السحابي (Google Drive)",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A),
                    fontSize = 19.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "احفظ بياناتك وسجلاتك الزراعية في سحابة Google Drive الخاصة بك لتأمينها من الضياع واستعادتها في أي وقت.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            // كبسولة تنبيه الأمان والخصوصية
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF132822) else Color(0xFFF0FDF4)
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFF16A34A).copy(alpha = 0.35f), Color(0xFF16A34A).copy(alpha = 0.15f))
                    )
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "يتم ربط الاسم والرقم بحسابك السحابي لتسهيل استرجاع بياناتك وتوثيق ملكية النسخ الاحتياطية.",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isDark) Color(0xFF86EFAC) else Color(0xFF166534),
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // حقل الاسم الكامل
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    errorMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("اسم الموزع / صاحب الحساب") },
                placeholder = { Text("مثال: م. عمر الحومي") },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryTeal)
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                    unfocusedTextColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                    focusedLabelColor = PrimaryTeal,
                    unfocusedLabelColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    focusedPlaceholderColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                    unfocusedPlaceholderColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                    cursorColor = PrimaryTeal,
                    focusedBorderColor = PrimaryTeal,
                    unfocusedBorderColor = if (isDark) Color(0xFF334B56) else Color(0xFFCBD5E1),
                    focusedContainerColor = if (isDark) Color(0xFF162329) else Color(0xFFF8FAFC),
                    unfocusedContainerColor = if (isDark) Color(0xFF142026) else Color(0xFFF8FAFC)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // حقل رقم الهاتف
            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = it
                    errorMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("رقم الهاتف (للتواصل واستعادة الحساب)") },
                placeholder = { Text("مثال: 773712030") },
                leadingIcon = {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = PrimaryTeal)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                    unfocusedTextColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                    focusedLabelColor = PrimaryTeal,
                    unfocusedLabelColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    focusedPlaceholderColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                    unfocusedPlaceholderColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                    cursorColor = PrimaryTeal,
                    focusedBorderColor = PrimaryTeal,
                    unfocusedBorderColor = if (isDark) Color(0xFF334B56) else Color(0xFFCBD5E1),
                    focusedContainerColor = if (isDark) Color(0xFF162329) else Color(0xFFF8FAFC),
                    unfocusedContainerColor = if (isDark) Color(0xFF142026) else Color(0xFFF8FAFC)
                )
            )

            // إظهار بطاقة الحساب الحالي إن وُجد
            if (currentGoogleAccount != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFF38BDF8).copy(alpha = 0.4f), Color(0xFF38BDF8).copy(alpha = 0.1f))
                        )
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "الحساب الحالي:",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
                                )
                                Text(
                                    text = currentGoogleAccount.email ?: "حساب Google",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1)
                                    ),
                                    maxLines = 1
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AccentEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // زر المتابعة الرئيسي
            Button(
                onClick = {
                    val cleanN = name.trim()
                    val cleanP = phone.trim()
                    if (cleanN.isBlank()) {
                        errorMessage = "يرجى إدخال اسم الموزع / صاحب الحساب أولاً"
                        return@Button
                    }
                    val cleanDigits = cleanP.replace(Regex("[^0-9]"), "")
                    if (cleanDigits.length < 8) {
                        errorMessage = "يرجى إدخال رقم هاتف صحيح لا يقل عن 8 أرقام"
                        return@Button
                    }

                    // إذا كان الحساب غير متصل، يتم إطلاق تسجيل الدخول
                    // إذا كان متصلاً، يستمر مع الحساب الحالي
                    val shouldSignIn = currentGoogleAccount == null
                    onProceed(cleanN, cleanP, shouldSignIn)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
            ) {
                Icon(
                    imageVector = if (currentGoogleAccount == null) Icons.Default.CloudDone else Icons.Default.Sync,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (currentGoogleAccount == null) "متابعة واختيار حساب Google 🚀" else "حفظ البيانات ومتابعة النسخ الاحتياطي ✨",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }

            // زر لتغيير الحساب إذا كان متصلاً بحساب بالفعل
            if (currentGoogleAccount != null) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        val cleanN = name.trim()
                        val cleanP = phone.trim()
                        if (cleanN.isBlank()) {
                            errorMessage = "يرجى إدخال الاسم أولاً"
                            return@TextButton
                        }
                        onProceed(cleanN, cleanP, true) // Force new account selection
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "اختيار حساب Google آخر 🔄",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = PrimaryTeal,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "إلغاء",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                )
            }
        }
    }
}
