package com.example.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
