package com.example.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.mosarib
import com.google.android.gms.auth.api.signin.GoogleSignInAccount

/**
 * صفيحة سفلية عائمة لطلب اسم ورقم المستخدم وربطهما بحساب Google Drive للنسخ الاحتياطي السحابي.
 */
@Composable
fun GoogleBackupAccountDialog(
    initialName: String = "",
    initialPhone: String = "",
    currentGoogleAccount: GoogleSignInAccount? = null,
    onDismiss: () -> Unit,
    onProceed: (name: String, phone: String, shouldLaunchSignIn: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val hasUnsavedChanges = (name.trim() != initialName.trim()) || (phone.trim() != initialPhone.trim())

    MosaribBottomSheet(
        onDismissRequest = onDismiss,
        title = "النسخ الاحتياطي السحابي (Google Drive)",
        subtitle = "احفظ بياناتك وسجلاتك الزراعية في سحابة Google Drive",
        icon = Icons.Default.CloudDone,
        iconTint = MaterialTheme.mosarib.success,
        hasUnsavedChanges = hasUnsavedChanges
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "احفظ بياناتك وسجلاتك الزراعية في سحابة Google Drive الخاصة بك لتأمينها من الضياع واستعادتها في أي وقت.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // كبسولة تنبيه الأمان والخصوصية
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.mosarib.successContainer.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.mosarib.success.copy(alpha = 0.35f))
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
                        tint = MaterialTheme.mosarib.success,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "يتم ربط الاسم والرقم بحسابك السحابي لتسهيل استرجاع بياناتك وتوثيق ملكية النسخ الاحتياطية.",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.mosarib.onSuccessContainer,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                    focusedBorderColor = PrimaryTeal,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    cursorColor = PrimaryTeal,
                    focusedLabelColor = PrimaryTeal,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                    focusedBorderColor = PrimaryTeal,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    cursorColor = PrimaryTeal,
                    focusedLabelColor = PrimaryTeal,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            // إظهار بطاقة الحساب الحالي إن وُجد
            if (currentGoogleAccount != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.mosarib.info.copy(alpha = 0.35f))
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
                                tint = MaterialTheme.mosarib.info,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "الحساب الحالي:",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Text(
                                    text = currentGoogleAccount.email ?: "حساب Google",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.mosarib.info
                                    ),
                                    maxLines = 1
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.mosarib.success,
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
                        color = MaterialTheme.mosarib.debt,
                        fontWeight = FontWeight.Bold
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

                    val shouldSignIn = currentGoogleAccount == null
                    onProceed(cleanN, cleanP, shouldSignIn)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.mosarib.success,
                    contentColor = MaterialTheme.mosarib.onSuccess
                )
            ) {
                Icon(
                    imageVector = if (currentGoogleAccount == null) Icons.Default.CloudDone else Icons.Default.Sync,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (currentGoogleAccount == null) "متابعة واختيار حساب Google" else "حفظ البيانات ومتابعة النسخ الاحتياطي",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
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
                        onProceed(cleanN, cleanP, true)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "اختيار حساب Google آخر",
                        style = MaterialTheme.typography.bodyMedium.copy(
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}
