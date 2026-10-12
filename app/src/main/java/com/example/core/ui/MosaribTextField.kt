package com.example.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.PrimaryTealLight

/**
 * حقل إدخال موحد وفائق التباين لكافة شاشات ونماذج تطبيق مُسَرِب.
 * - يدعم الوضعين المظلم والفاتح بدقة تامة اعتماداً على ثيم التطبيق النشط.
 * - خلفية بيضاء نقية في الوضع الفاتح، وخلفية بترولية داكنة فخمة في الوضع المظلم.
 * - حدود واضحة ومتباينة وألوان خطوط مقروءة وواضحة جداً.
 */
@Composable
fun MosaribTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isDense: Boolean = false,
    shape: Shape = if (isDense) RoundedCornerShape(10.dp) else RoundedCornerShape(14.dp),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val isDark = isAppDark()
    val isError = !errorText.isNullOrBlank()

    val labelColor = when {
        isError -> Color(0xFFE53935)
        isDark -> Color(0xFFE2E8F0)
        else -> Color(0xFF1E293B)
    }

    val containerColor = if (isDark) Color(0xFF141F25) else Color.White
    val textColor = if (isDark) Color(0xFFF0F6F8) else Color(0xFF0F172A)
    val placeholderColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
    val borderColor = if (isDark) Color(0xFF2A3F4D) else Color(0xFFCBD5E1)
    val activeTeal = if (isDark) PrimaryTealLight else PrimaryTeal

    Column(modifier = modifier) {
        if (!label.isNullOrBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isDense) 12.sp else 13.sp,
                    color = labelColor
                ),
                modifier = Modifier.padding(bottom = if (isDense) 4.dp else 6.dp, start = 2.dp)
            )
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            maxLines = maxLines,
            shape = shape,
            textStyle = TextStyle(
                fontSize = if (isDense) 13.5.sp else 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            ),
            placeholder = if (!placeholder.isNullOrBlank()) {
                {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = placeholderColor,
                            fontSize = if (isDense) 12.5.sp else 14.sp
                        )
                    )
                }
            } else null,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            isError = isError,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = containerColor,
                unfocusedContainerColor = containerColor,
                disabledContainerColor = containerColor,
                errorContainerColor = if (isDark) Color(0xFF2C1517) else Color(0xFFFFF5F5),

                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                disabledTextColor = textColor,

                focusedBorderColor = activeTeal,
                unfocusedBorderColor = borderColor,
                disabledBorderColor = borderColor,
                errorBorderColor = Color(0xFFE53935),

                focusedLeadingIconColor = activeTeal,
                unfocusedLeadingIconColor = activeTeal,
                disabledLeadingIconColor = activeTeal.copy(alpha = 0.6f),

                focusedTrailingIconColor = activeTeal,
                unfocusedTrailingIconColor = if (isDark) Color(0xFFAABCC6) else Color(0xFF64748B),
                disabledTrailingIconColor = if (isDark) Color(0xFFAABCC6) else Color(0xFF64748B),

                cursorColor = activeTeal
            )
        )

        AnimatedVisibility(
            visible = isError,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = errorText ?: "",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFFE53935),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp
                ),
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }

        if (!helperText.isNullOrBlank() && !isError) {
            Text(
                text = helperText,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    fontSize = 11.sp
                ),
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}

/**
 * حقل إدخال واقتراح تلقائي موحد (Autocomplete) للقوائم والعملاء.
 * - يمنع اختفاء لوحة المفاتيح أثناء الكتابة بفضل تعيين focusable = false في القائمة المنبثقة.
 * - نص التلميح الافتراضي الموحد: "اكتب او اختر الاسم".
 * - يدعم الوضعين المظلم والفاتح وخيار تسجيل عنصر جديد فوراً عند عدم وجود نتائج.
 */
@Composable
fun <T> MosaribAutoCompleteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    items: List<T>,
    itemText: (T) -> String,
    modifier: Modifier = Modifier,
    placeholder: String = "اكتب او اختر الاسم",
    label: String? = null,
    isDense: Boolean = false,
    itemDetail: ((T) -> String)? = null,
    isItemSecondary: ((T) -> Boolean)? = null,
    isItemSelected: ((T) -> Boolean)? = null,
    onItemSelected: (T) -> Unit,
    onAddNewClicked: (() -> Unit)? = null,
    addNewPrompt: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val isDark = isAppDark()
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        MosaribTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            placeholder = placeholder,
            label = label,
            isDense = isDense,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon ?: {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = null
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            properties = PopupProperties(
                focusable = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            ),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 300.dp)
        ) {
            val filtered = if (value.isBlank()) items
            else items.filter { item ->
                itemText(item).contains(value, ignoreCase = true) ||
                (itemDetail != null && itemDetail(item).contains(value, ignoreCase = true))
            }

            if (filtered.isEmpty()) {
                if (onAddNewClicked != null && value.isNotBlank()) {
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = addNewPrompt ?: "غير مسجل: \"$value\"",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryTeal
                                    )
                                )
                                Text(
                                    text = "انقر هنا لتسجيله كعميل جديد فوراً",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                                )
                            }
                        },
                        leadingIcon = {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = PrimaryTeal)
                        },
                        onClick = {
                            expanded = false
                            onAddNewClicked()
                        }
                    )
                } else {
                    DropdownMenuItem(
                        text = {
                            Text("لا توجد نتائج مطابقة", color = Color.Gray, fontSize = 13.sp)
                        },
                        onClick = { expanded = false }
                    )
                }
            } else {
                filtered.forEach { item ->
                    val text = itemText(item)
                    val detail = itemDetail?.invoke(item) ?: ""
                    val isSelected = isItemSelected?.invoke(item) ?: false
                    val isSecondary = isItemSecondary?.invoke(item) ?: false

                    DropdownMenuItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSelected) PrimaryTeal.copy(alpha = 0.08f) else Color.Transparent),
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(34.dp),
                                    shape = CircleShape,
                                    color = if (isSecondary) (if (isDark) Color(0xFF0369A1) else Color(0xFFBAE6FD))
                                            else (if (isDark) Color(0xFF1E3A3A) else Color(0xFFCCFBF1))
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = text.firstOrNull()?.toString() ?: "ع",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSecondary) (if (isDark) Color(0xFFE0F2FE) else Color(0xFF0369A1))
                                                        else (if (isDark) Color(0xFF5EEAD4) else PrimaryTeal)
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = text,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                        )
                                    )
                                    if (detail.isNotBlank()) {
                                        Text(
                                            text = detail,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        },
                        onClick = {
                            onItemSelected(item)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

