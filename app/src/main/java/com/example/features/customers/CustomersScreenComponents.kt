package com.example.features.customers

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import com.example.core.voice.VoiceFieldMic
import androidx.compose.material3.Surface
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.EmptyStateView
import com.example.core.ui.LuxuryToastNotification
import com.example.core.ui.ToastMessage
import com.example.core.util.Formatters
import com.example.core.ui.AnimatedAmount

import com.example.ui.theme.mosarib

/**
 * محتوى الشاشة: قائمة الحسابات مع رأس البحث والفلاتر + الشريط المالي السفلي / الزر العائم + الإشعار
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CustomersListContent(
    listState: LazyListState,
    isCompactHeight: Boolean,
    isStickyPinned: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onVoiceSearchClick: (() -> Unit)? = null,
    filterRow: @Composable () -> Unit,
    customers: List<CustomerWithBalance>,
    balanceFilter: CustomerBalanceFilter,
    accountFilter: AccountFilter,
    currencySymbol: String,
    owedByUs: Double,
    owedToUs: Double,
    toast: ToastMessage?,
    onCustomerClick: (CustomerWithBalance) -> Unit,
    onCallClick: (CustomerWithBalance) -> Unit,
    onWhatsAppClick: (CustomerWithBalance) -> Unit,
    onPdfStatementClick: (CustomerWithBalance) -> Unit,
    onEditClick: (CustomerWithBalance) -> Unit,
    onDeleteClick: (CustomerWithBalance) -> Unit,
    onQuickAddClick: () -> Unit,
    onDismissToast: () -> Unit,
    modifier: Modifier = Modifier,
    filterHint: String? = null
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = if (isCompactHeight) 64.dp else 96.dp)
        ) {
            // شريط الفلاتر المصغرة وحقل البحث المدمج وفلاتر الترتيب المتجاوبة
            if (isCompactHeight) {
                // في الوضع الأفقي: حقل البحث وشريط الفلاتر ثلاثي الأقسام
                item {
                    CustomersSearchHeaderWithVoice(
                        isCompact = true,
                        isPinned = false,
                        searchQuery = searchQuery,
                        onSearchQueryChange = onSearchQueryChange,
                        onVoiceSearchClick = onVoiceSearchClick,
                        filterRow = filterRow
                    )
                }
            } else {
                // في الوضع الرأسي: StickyHeader أنيق يضم حقل البحث أولاً ثم شريط الفلاتر ثلاثي الأقسام
                stickyHeader {
                    CustomersSearchHeaderWithVoice(
                        isCompact = false,
                        isPinned = isStickyPinned,
                        searchQuery = searchQuery,
                        onSearchQueryChange = onSearchQueryChange,
                        onVoiceSearchClick = onVoiceSearchClick,
                        filterRow = filterRow
                    )
                }
            }

            // 2. قائمة الحسابات المضغوطة (دفتر الحسابات)
            if (customers.isEmpty()) {
                item {
                    CustomersEmptyState(balanceFilter = balanceFilter, accountFilter = accountFilter)
                }
            } else {
                items(customers, key = { it.customer.id }) { item ->
                    CustomerCardItem(
                        modifier = Modifier.animateItem(),
                        item = item,
                        currencySymbol = currencySymbol,
                        onClick = { onCustomerClick(item) },
                        onCallClick = { onCallClick(item) },
                        onWhatsAppClick = { onWhatsAppClick(item) },
                        onPdfStatementClick = { onPdfStatementClick(item) },
                        onEditClick = { onEditClick(item) },
                        onDeleteClick = { onDeleteClick(item) }
                    )
                }
            }
        }

        if (!isCompactHeight) {
            // 3. الشريط المالي السفلي الثابت (دفتر الحسابات) التفاعلي: عليك | (+) | لك
            CustomersBalanceBar(
                accountFilter = accountFilter,
                owedByUs = owedByUs,
                owedToUs = owedToUs,
                currencySymbol = currencySymbol,
                onQuickAddClick = onQuickAddClick,
                filterHint = filterHint,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        } else {
            CustomersCompactAddButton(
                onClick = onQuickAddClick,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }

        LuxuryToastNotification(
            toast = toast,
            onDismiss = onDismissToast,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 76.dp)
        )
    }
}

@Composable
internal fun CustomersEmptyState(
    balanceFilter: CustomerBalanceFilter,
    accountFilter: AccountFilter
) {
    EmptyStateView(
        icon = Icons.Default.Person,
        title = when {
            balanceFilter == CustomerBalanceFilter.DEBTOR -> "لا يوجد عملاء عليهم مبالغ"
            balanceFilter == CustomerBalanceFilter.CREDITOR -> "لا يوجد عملاء لهم أرصدة"
            balanceFilter == CustomerBalanceFilter.SETTLED -> "لا يوجد عملاء مسددون بالكامل"
            accountFilter == AccountFilter.WELL_OWNERS -> "لا يوجد أصحاب آبار مسجلين"
            else -> "لا يوجد عملاء مضافين"
        },
        description = if (balanceFilter != CustomerBalanceFilter.ALL) "جرّب تغيير خيارات الفلترة لعرض باقي الحسابات" else "اضغط على زر (+) لإضافة حساب جديد"
    )
}

/**
 * الشريط المالي السفلي الثابت (دفتر الحسابات) التفاعلي: عليك | (+) | لك
 */
@Composable
internal fun CustomersBalanceBar(
    accountFilter: AccountFilter,
    owedByUs: Double,
    owedToUs: Double,
    currencySymbol: String,
    onQuickAddClick: () -> Unit,
    modifier: Modifier = Modifier,
    filterHint: String? = null
) {
    val filterIcon = accountFilterIcon(accountFilter)

    Card(
        modifier = modifier
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // مستحقات عليك (مختلفة تفاعلياً حسب الفلتر المختار)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BalanceBarDot(color = MaterialTheme.mosarib.debt)
                        Spacer(modifier = Modifier.width(4.dp))
                        if (filterIcon != null) {
                            BalanceBarIcon(icon = filterIcon, tint = MaterialTheme.mosarib.debt)
                            Spacer(modifier = Modifier.width(3.dp))
                        }
                        Text(
                            text = "عليك",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.mosarib.debt,
                                fontWeight = FontWeight.Bold
                            ),
                            softWrap = false,
                            maxLines = 1
                        )
                    }
                    if (!filterHint.isNullOrBlank()) {
                        Text(
                            text = filterHint,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.mosarib.textMuted
                            ),
                            softWrap = false,
                            maxLines = 1
                        )
                    }
                    BalanceBarAmount(amount = owedByUs, currencySymbol = currencySymbol, color = MaterialTheme.mosarib.debt)
                }

                // مساحة مخصصة للزر العائم لضمان عدم التداخل والتوسيط التام
                Spacer(modifier = Modifier.width(56.dp))

                // مستحقات لك (مختلفة تفاعلياً حسب الفلتر المختار)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "لك",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.mosarib.credit,
                                fontWeight = FontWeight.Bold
                            ),
                            softWrap = false,
                            maxLines = 1
                        )
                        if (filterIcon != null) {
                            Spacer(modifier = Modifier.width(3.dp))
                            BalanceBarIcon(icon = filterIcon, tint = MaterialTheme.mosarib.credit)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        BalanceBarDot(color = MaterialTheme.mosarib.credit)
                    }
                    if (!filterHint.isNullOrBlank()) {
                        Text(
                            text = filterHint,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.mosarib.textMuted
                            ),
                            softWrap = false,
                            maxLines = 1
                        )
                    }
                    BalanceBarAmount(amount = owedToUs, currencySymbol = currencySymbol, color = MaterialTheme.mosarib.credit)
                }
            }

            // زر الإضافة السريع (+) الدائري في المنتصف الهندسي الدقيق للكرت دائماً
            FloatingActionButton(
                onClick = onQuickAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
                    .testTag("fab_add_customer")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "إضافة سريعة",
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

/**
 * في الوضع الأفقي / الشاشات المضغوطة: زر عائم مدمج أنيق في الزاوية دون حجب أي كرت
 */
@Composable
internal fun CustomersCompactAddButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = CircleShape,
        modifier = modifier
            .padding(start = 16.dp, bottom = 12.dp)
            .size(46.dp)
            .testTag("fab_add_customer")
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "إضافة حساب جديد",
            modifier = Modifier.size(24.dp)
        )
    }
}

/** أيقونة نوع الحساب المعروضة بجانب عناوين الشريط المالي (لا أيقونة عند عرض الكل) */
private fun accountFilterIcon(accountFilter: AccountFilter): ImageVector? = when (accountFilter) {
    AccountFilter.ALL -> null
    AccountFilter.FARMERS -> Icons.Default.Park
    AccountFilter.WELL_OWNERS -> Icons.Default.WaterDrop
}

@Composable
private fun BalanceBarDot(color: Color) {
    Box(
        modifier = Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun BalanceBarIcon(icon: ImageVector, tint: Color) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(15.dp)
    )
}

@Composable
private fun BalanceBarAmount(amount: Double, currencySymbol: String, color: Color) {
    AnimatedAmount(
        value = amount,
        currencySymbol = currencySymbol,
        style = MaterialTheme.typography.titleMedium.copy(
            color = color,
            fontWeight = FontWeight.ExtraBold
        )
    )
}

@Composable
internal fun CustomersSearchHeaderWithVoice(
    isCompact: Boolean,
    isPinned: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onVoiceSearchClick: (() -> Unit)? = null,
    filterRow: @Composable () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        shadowElevation = if (!isCompact && isPinned) 3.dp else 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = if (isCompact) 4.dp else 6.dp)) {
            val searchTextStyle = if (isCompact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isCompact) 34.dp else 38.dp)
                    .clip(RoundedCornerShape(if (isCompact) 10.dp else 12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isCompact) 0.45f else 0.5f))
                    .padding(horizontal = if (isCompact) 8.dp else 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(if (isCompact) 15.dp else 16.dp)
                    )
                    Spacer(modifier = Modifier.width(if (isCompact) 4.dp else 6.dp))
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "بحث بالاسم، المزرعة، أو الهاتف...",
                                style = searchTextStyle.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                ),
                                maxLines = 1
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { onSearchQueryChange(it) },
                            singleLine = true,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            textStyle = searchTextStyle.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("customers_search_input")
                        )
                    }
                    if (onVoiceSearchClick != null) {
                        VoiceFieldMic(onClick = onVoiceSearchClick)
                    }
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.size(if (isCompact) 18.dp else 20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "مسح",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(if (isCompact) 13.dp else 14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 6.dp))
            filterRow()
        }
    }
}
