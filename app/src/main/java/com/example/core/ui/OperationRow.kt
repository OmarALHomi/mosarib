package com.example.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.mosarib

/**
 * صف عمليات موحد ومضغوط لقوائم التطبيق (سندات، دورات سقي، شراء ساعات).
 */
@Composable
fun OperationRow(
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    title: String,
    subtitle: String?,
    meta: String,
    amountText: String,
    amountColor: Color,
    trailingNote: String? = null,
    trailingNoteColor: Color = MaterialTheme.mosarib.textMuted,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    menu: (@Composable () -> Unit)? = null
) {
    val shape = MaterialTheme.shapes.medium

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clip(shape)
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outline, shape = shape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Leading 36dp circle icon
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // 2. Middle Column (weight 1f)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = meta,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.mosarib.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 3. Trailing column aligned to end
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = amountText,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = amountColor,
                maxLines = 1,
                softWrap = false
            )

            if (!trailingNote.isNullOrBlank()) {
                Text(
                    text = trailingNote,
                    style = MaterialTheme.typography.labelSmall,
                    color = trailingNoteColor,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // 4. Optional 40dp kebab menu
        if (menu != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                menu()
            }
        }
    }
}

@Composable
fun OperationRow(
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    title: String,
    subtitle: String?,
    meta: String,
    amountText: String,
    amountColor: Color,
    trailingNote: String?,
    onClick: () -> Unit,
    menu: (@Composable () -> Unit)? = null
) = OperationRow(
    icon = icon,
    iconTint = iconTint,
    iconBackground = iconBackground,
    title = title,
    subtitle = subtitle,
    meta = meta,
    amountText = amountText,
    amountColor = amountColor,
    trailingNote = trailingNote,
    trailingNoteColor = MaterialTheme.mosarib.textMuted,
    modifier = Modifier,
    onClick = onClick,
    menu = menu
)

