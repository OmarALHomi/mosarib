package com.example.core.ui.guide

import androidx.compose.ui.Alignment

import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.ui.theme.PrimaryTeal

/**
 * Standard small «؟» button used in top bars and form headers to restart a tour.
 */
@Composable
fun GuideHelpButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "دليل الاستخدام والتعليمات"
) {
    // A plain clickable box: IconButton reserves 48dp and pushed sheet titles onto two lines.
    Box(
        modifier = modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
            contentDescription = contentDescription,
            tint = PrimaryTeal,
            modifier = Modifier.size(17.dp)
        )
    }
}
