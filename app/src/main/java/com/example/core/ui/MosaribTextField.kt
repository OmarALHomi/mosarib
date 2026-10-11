package com.example.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
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

/**
 * حقل إدخال موحد وعصري لكافة شاشات ونماذج تطبيق مُسَرِب.
 * - زوايا ناعمة 14dp
 * - دعم كامل ودقيق للوضع المظلم (Dark Mode) والفاتح (Light Mode)
 * - حدود تركوازية جذابة عند التركيز
 * - دعم الأيقونات، رسائل الأخطاء، والنصوص التوضيحية
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
    shape: Shape = RoundedCornerShape(14.dp),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val isDark = isSystemInDarkTheme()
    val isError = !errorText.isNullOrBlank()

    Column(modifier = modifier) {
        if (!label.isNullOrBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isError) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
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
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            ),
            placeholder = if (!placeholder.isNullOrBlank()) {
                {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF9E9E9E),
                            fontSize = 14.sp
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
                focusedContainerColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                unfocusedContainerColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                disabledContainerColor = if (isDark) Color(0xFF111A20).copy(alpha = 0.3f) else Color(0xFFF5F5F5),
                errorContainerColor = if (isDark) Color(0xFF2C1517).copy(alpha = 0.5f) else Color(0xFFFFEBEE).copy(alpha = 0.4f),

                focusedBorderColor = PrimaryTeal,
                unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCFD8DC),
                disabledBorderColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE0E0E0),
                errorBorderColor = Color(0xFFE53935),

                focusedLeadingIconColor = PrimaryTeal,
                unfocusedLeadingIconColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF78909C),
                focusedTrailingIconColor = PrimaryTeal,
                unfocusedTrailingIconColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF78909C),

                cursorColor = PrimaryTeal
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
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.5.sp
                ),
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }

        if (!isError && !helperText.isNullOrBlank()) {
            Text(
                text = helperText,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF78909C),
                    fontSize = 11.5.sp
                ),
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}
