package com.example.core.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

const val REQUIRED_FIELD_TEXT = "هذا الحقل مطلوب"

/** The first field (in screen order) whose check is false, or null when every field is filled. */
fun <T> firstMissingField(vararg fields: Pair<T, Boolean>): T? = fields.firstOrNull { !it.second }?.first

/** Small red line shown under a required field that is empty after pressing save. */
@Composable
fun RequiredFieldText(visible: Boolean, modifier: Modifier = Modifier) {
    if (!visible) return
    Text(
        text = REQUIRED_FIELD_TEXT,
        style = MaterialTheme.typography.labelSmall.copy(
            color = Color(0xFFE53935),
            fontWeight = FontWeight.Bold,
            fontSize = 11.5.sp
        ),
        modifier = modifier.padding(top = 4.dp, start = 4.dp)
    )
}
