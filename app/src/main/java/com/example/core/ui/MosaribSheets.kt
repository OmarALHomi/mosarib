package com.example.core.ui

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.mosarib
import kotlinx.coroutines.launch

/**
 * درجات نغمة النوافذ المنبثقة والأزرار (Neutral, Info, Success, Warning, Danger).
 */
enum class DialogTone {
    Neutral, Info, Success, Warning, Danger
}

@Composable
private fun toneToColor(tone: DialogTone): Pair<Color, Color> {
    return when (tone) {
        DialogTone.Neutral -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        DialogTone.Info -> MaterialTheme.mosarib.info to MaterialTheme.mosarib.onInfo
        DialogTone.Success -> MaterialTheme.mosarib.success to MaterialTheme.mosarib.onSuccess
        DialogTone.Warning -> MaterialTheme.mosarib.warning to MaterialTheme.mosarib.onWarning
        DialogTone.Danger -> MaterialTheme.mosarib.debt to Color.White
    }
}

/**
 * زر رئيسي مخصص لتذييل الـ Bottom Sheets والنوافذ المنبثقة.
 */
@Composable
fun MosaribPrimaryButton(
    text: String,
    onClick: () -> Unit,
    tone: DialogTone = DialogTone.Neutral,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val (bgColor, fgColor) = toneToColor(tone)
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = bgColor,
            contentColor = fgColor
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * زر ثانوي مخصص لتذييل الـ Bottom Sheets والنوافذ المنبثقة.
 */
@Composable
fun MosaribSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * نافذة حوار موحدة ومتجاوبة لمختلف الإجراءات والتأكيدات.
 */
@Composable
fun MosaribDialog(
    onDismissRequest: () -> Unit,
    title: String,
    icon: ImageVector? = null,
    tone: DialogTone = DialogTone.Neutral,
    message: String? = null,
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String? = "إلغاء",
    onDismiss: (() -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    val (toneColor, onToneColor) = toneToColor(tone)

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 400.dp)
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (icon != null) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(toneColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = toneColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    if (!message.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (content != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        content()
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val stackButtons = maxWidth < 320.dp
                        if (stackButtons) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onConfirm,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = toneColor,
                                        contentColor = onToneColor
                                    )
                                ) {
                                    Text(
                                        text = confirmText,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (dismissText != null) {
                                    TextButton(
                                        onClick = { onDismiss?.invoke() ?: onDismissRequest() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = dismissText,
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = onConfirm,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = toneColor,
                                        contentColor = onToneColor
                                    )
                                ) {
                                    Text(
                                        text = confirmText,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (dismissText != null) {
                                    TextButton(
                                        onClick = { onDismiss?.invoke() ?: onDismissRequest() },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = dismissText,
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * قائمة خيارات منسدلة موحدة التصميم والظلال.
 */
@Composable
fun MosaribMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset.Zero,
    // false for suggestion lists under a text field: a focusable popup takes the focus and closes the keyboard
    focusable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isAppDark()
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.widthIn(min = 200.dp),
        offset = offset,
        properties = androidx.compose.ui.window.PopupProperties(focusable = focusable),
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
        shadowElevation = if (isDark) 0.dp else 6.dp,
        border = if (isDark) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) else null
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                content()
            }
        }
    }
}

/**
 * عنصر في القائمة المنسدلة بارتفاع 44dp وتصميم موحد.
 * ملاحظة: العناصر الحذوفة (destructive) توضع في آخر القائمة بعد [MosaribMenuDivider].
 */
@Composable
fun MosaribMenuItem(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    destructive: Boolean = false,
    trailing: String? = null
) {
    val itemColor = if (destructive) MaterialTheme.mosarib.debt else tint

    DropdownMenuItem(
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = itemColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (trailing != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = (if (destructive) itemColor else MaterialTheme.colorScheme.primary).copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = trailing,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (destructive) itemColor else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        },
        leadingIcon = if (icon != null) {
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = itemColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else null,
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        contentPadding = PaddingValues(horizontal = 14.dp)
    )
}

/**
 * فاصل موحد داخل القوائم المنسدلة.
 */
@Composable
fun MosaribMenuDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(vertical = 4.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Composable
private fun rememberSheetDragTracker(): Triple<NestedScrollConnection, Modifier, () -> Long> {
    var lastDragAtMillis by remember { mutableLongStateOf(0L) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y != 0f) {
                    lastDragAtMillis = SystemClock.uptimeMillis()
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (available.y != 0f) {
                    lastDragAtMillis = SystemClock.uptimeMillis()
                }
                return Velocity.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y != 0f || consumed.y != 0f) {
                    lastDragAtMillis = SystemClock.uptimeMillis()
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (available.y != 0f || consumed.y != 0f) {
                    lastDragAtMillis = SystemClock.uptimeMillis()
                }
                return Velocity.Zero
            }
        }
    }

    val dragModifier = Modifier.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            awaitTouchSlopOrCancellation(down.id) { _, over ->
                if (over.y != 0f) {
                    lastDragAtMillis = SystemClock.uptimeMillis()
                }
            }
            do {
                val event = awaitPointerEvent()
                val hasMotion = event.changes.any { change ->
                    kotlin.math.abs(change.position.y - change.previousPosition.y) > 0.5f
                }
                if (hasMotion) {
                    lastDragAtMillis = SystemClock.uptimeMillis()
                }
            } while (event.changes.any { it.pressed })
        }
    }

    return Triple(nestedScrollConnection, dragModifier) { lastDragAtMillis }
}

/**
 * صفيحة سفلية عائمة موحدة ومحصنة ضد الإغلاق العرضي بالتمرير أو السحب.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MosaribBottomSheet(
    onDismissRequest: () -> Unit,
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    headerBadge: (@Composable () -> Unit)? = null,
    headerActions: (@Composable () -> Unit)? = null,
    hasUnsavedChanges: Boolean = false,
    footer: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var showDiscardDialog by remember { mutableStateOf(false) }
    var isDismissingAllowed by remember { mutableStateOf(false) }
    var isDismissingInProgress by remember { mutableStateOf(false) }
    var hasDismissBeenCalled by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val (nestedScrollConnection, dragModifier, getLastDragTime) = rememberSheetDragTracker()

    var requestDismiss: () -> Unit = {}

    val notifyDismiss = {
        if (!hasDismissBeenCalled) {
            hasDismissBeenCalled = true
            onDismissRequest()
        }
    }

    // The sheet state is remembered with confirmValueChange as a key, so this lambda must be the
    // same instance on every recomposition; otherwise a changed title, subtitle or unsaved flag
    // rebuilds the state and the whole sheet hides and shows again (and the keyboard closes).
    val requestDismissRef = remember { java.util.concurrent.atomic.AtomicReference<() -> Unit>({}) }
    val latestLastDragTime = rememberUpdatedState(getLastDragTime)
    val confirmSheetChange: (SheetValue) -> Boolean = remember {
        { targetValue ->
            if (targetValue == SheetValue.Hidden) {
                if (isDismissingAllowed) {
                    true
                } else {
                    val now = SystemClock.uptimeMillis()
                    val timeSinceLastDrag = now - latestLastDragTime.value()
                    if (timeSinceLastDrag > 400L) {
                        coroutineScope.launch {
                            requestDismissRef.get().invoke()
                        }
                    }
                    false
                }
            } else {
                true
            }
        }
    }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = confirmSheetChange
    )

    val executeDismiss: () -> Unit = {
        if (!isDismissingInProgress && !hasDismissBeenCalled) {
            isDismissingInProgress = true
            isDismissingAllowed = true
            coroutineScope.launch {
                try {
                    sheetState.hide()
                } catch (_: Throwable) {
                } finally {
                    notifyDismiss()
                }
            }
        }
    }

    requestDismiss = {
        if (hasUnsavedChanges) {
            showDiscardDialog = true
        } else {
            executeDismiss()
        }
    }
    requestDismissRef.set(requestDismiss)

    val isDark = isAppDark()
    val containerColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        MaterialTheme.colorScheme.surface
    }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val maxHeight = screenHeight * 0.92f

    ModalBottomSheet(
        onDismissRequest = {
            if (!isDismissingInProgress && !hasDismissBeenCalled) {
                requestDismiss()
            } else {
                notifyDismiss()
            }
        },
        sheetState = sheetState,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false),
        containerColor = containerColor,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant)
                    .then(dragModifier)
            )
        }
    ) {
        BackHandler(enabled = true) {
            requestDismiss()
        }

        if (showDiscardDialog) {
            MosaribDialog(
                onDismissRequest = { showDiscardDialog = false },
                title = "تجاهل التغييرات؟",
                message = "لديك بيانات أو تغييرات غير محفوظة، هل أنت متأكد من التجاهل والإغلاق؟",
                tone = DialogTone.Danger,
                confirmText = "تجاهل",
                onConfirm = {
                    showDiscardDialog = false
                    executeDismiss()
                },
                dismissText = "متابعة التعديل",
                onDismiss = { showDiscardDialog = false }
            )
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight)
            ) {
                // Header Row
                if (title.isNotEmpty() || headerBadge != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (icon != null) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(iconTint.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = iconTint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                SheetTitleText(title)
                                if (!subtitle.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (headerBadge != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    headerBadge()
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (headerActions != null) {
                                headerActions()
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            IconButton(
                                onClick = requestDismiss,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إغلاق",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (headerActions != null) {
                            headerActions()
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        IconButton(
                            onClick = requestDismiss,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .imePadding()
                        .nestedScroll(nestedScrollConnection)
                        .then(dragModifier)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    content = content
                )

                // Sticky Footer
                if (footer != null) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        content = footer
                    )
                }
            }
        }
    }
}

/**
 * صفيحة سفلية عائمة تدعم القوائم الكسولة (LazyColumn).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MosaribBottomSheetLazy(
    onDismissRequest: () -> Unit,
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    headerBadge: (@Composable () -> Unit)? = null,
    headerActions: (@Composable () -> Unit)? = null,
    hasUnsavedChanges: Boolean = false,
    footer: (@Composable RowScope.() -> Unit)? = null,
    content: LazyListScope.() -> Unit
) {
    var showDiscardDialog by remember { mutableStateOf(false) }
    var isDismissingAllowed by remember { mutableStateOf(false) }
    var isDismissingInProgress by remember { mutableStateOf(false) }
    var hasDismissBeenCalled by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val (nestedScrollConnection, dragModifier, getLastDragTime) = rememberSheetDragTracker()

    var requestDismiss: () -> Unit = {}

    val notifyDismiss = {
        if (!hasDismissBeenCalled) {
            hasDismissBeenCalled = true
            onDismissRequest()
        }
    }

    // The sheet state is remembered with confirmValueChange as a key, so this lambda must be the
    // same instance on every recomposition; otherwise a changed title, subtitle or unsaved flag
    // rebuilds the state and the whole sheet hides and shows again (and the keyboard closes).
    val requestDismissRef = remember { java.util.concurrent.atomic.AtomicReference<() -> Unit>({}) }
    val latestLastDragTime = rememberUpdatedState(getLastDragTime)
    val confirmSheetChange: (SheetValue) -> Boolean = remember {
        { targetValue ->
            if (targetValue == SheetValue.Hidden) {
                if (isDismissingAllowed) {
                    true
                } else {
                    val now = SystemClock.uptimeMillis()
                    val timeSinceLastDrag = now - latestLastDragTime.value()
                    if (timeSinceLastDrag > 400L) {
                        coroutineScope.launch {
                            requestDismissRef.get().invoke()
                        }
                    }
                    false
                }
            } else {
                true
            }
        }
    }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = confirmSheetChange
    )

    val executeDismiss: () -> Unit = {
        if (!isDismissingInProgress && !hasDismissBeenCalled) {
            isDismissingInProgress = true
            isDismissingAllowed = true
            coroutineScope.launch {
                try {
                    sheetState.hide()
                } catch (_: Throwable) {
                } finally {
                    notifyDismiss()
                }
            }
        }
    }

    requestDismiss = {
        if (hasUnsavedChanges) {
            showDiscardDialog = true
        } else {
            executeDismiss()
        }
    }
    requestDismissRef.set(requestDismiss)

    val isDark = isAppDark()
    val containerColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        MaterialTheme.colorScheme.surface
    }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val maxHeight = screenHeight * 0.92f

    ModalBottomSheet(
        onDismissRequest = {
            if (!isDismissingInProgress && !hasDismissBeenCalled) {
                requestDismiss()
            } else {
                notifyDismiss()
            }
        },
        sheetState = sheetState,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false),
        containerColor = containerColor,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant)
                    .then(dragModifier)
            )
        }
    ) {
        BackHandler(enabled = true) {
            requestDismiss()
        }

        if (showDiscardDialog) {
            MosaribDialog(
                onDismissRequest = { showDiscardDialog = false },
                title = "تجاهل التغييرات؟",
                message = "لديك بيانات أو تغييرات غير محفوظة، هل أنت متأكد من التجاهل والإغلاق؟",
                tone = DialogTone.Danger,
                confirmText = "تجاهل",
                onConfirm = {
                    showDiscardDialog = false
                    executeDismiss()
                },
                dismissText = "متابعة التعديل",
                onDismiss = { showDiscardDialog = false }
            )
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight)
            ) {
                // Header Row
                if (title.isNotEmpty() || headerBadge != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (icon != null) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(iconTint.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = iconTint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                SheetTitleText(title)
                                if (!subtitle.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (headerBadge != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    headerBadge()
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (headerActions != null) {
                                headerActions()
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            IconButton(
                                onClick = requestDismiss,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إغلاق",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (headerActions != null) {
                            headerActions()
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        IconButton(
                            onClick = requestDismiss,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Lazy Content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .imePadding()
                        .nestedScroll(nestedScrollConnection)
                        .then(dragModifier)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    content = content
                )

                // Sticky Footer
                if (footer != null) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        content = footer
                    )
                }
            }
        }
    }
}

/** Sheet title on one line: shrinks a little (down to 80%) before it would wrap or cut. */
@Composable
private fun SheetTitleText(title: String) {
    val baseStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
    var scale by remember(title) { mutableFloatStateOf(1f) }
    Text(
        text = title,
        style = baseStyle.copy(fontSize = baseStyle.fontSize * scale),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { result ->
            if (result.hasVisualOverflow && scale > 0.8f) scale -= 0.05f
        }
    )
}

