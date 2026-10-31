package com.example.features.customers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.MosaribFileActionDialog
import com.example.core.ui.MosaribLoadingDialog
import com.example.core.ui.SendMessageChoiceDialog
import com.example.core.util.FileSharingHelper
import com.example.features.sessions.AddEditSessionBottomSheet
import com.example.features.sessions.SessionsViewModel
import com.example.features.sessions.WaterSession
import com.example.features.vouchers.AddVoucherBottomSheet
import com.example.features.vouchers.VoucherType
import com.example.features.vouchers.VouchersViewModel
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal
import java.io.File

/**
 * نوافذ الاستيراد والتصدير المرتبطة بحالة الـ ViewModel
 */
@Composable
internal fun CustomersImportExportDialogs(viewModel: CustomersViewModel) {
    val pdfReady by viewModel.pdfReadyFile.collectAsStateWithLifecycle()
    val importDrafts by viewModel.importDrafts.collectAsStateWithLifecycle()
    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()

    // شاشة مراجعة استيراد إكسل الذكية
    importDrafts?.let { drafts ->
        CustomerImportReviewDialog(
            drafts = drafts,
            onDraftUpdated = { viewModel.updateImportDraft(it) },
            onDraftDeleted = { viewModel.removeImportDraft(it) },
            onToggleSelectAll = { viewModel.toggleSelectAllImportDrafts(it) },
            onConfirmImport = { updateExisting -> viewModel.confirmImport(updateExisting) },
            onDismiss = { viewModel.cancelImport() }
        )
    }

    // مؤشر جاري قراءة ملف الإكسل بذكاء بهوية مسرب المائية
    if (isImporting) {
        MosaribLoadingDialog(
            title = "جاري قراءة ملف البيانات",
            subTitle = "يتم فحص الأعمدة وتدفق البيانات بذكاء..."
        )
    }

    // File Open / Share Dialog (PDF / Excel / CSV)
    pdfReady?.let { (file, title) ->
        CustomersFileReadyDialog(
            file = file,
            title = title,
            onFinished = { viewModel.clearPdfReady() }
        )
    }
}

/**
 * نافذة فتح / مشاركة الملف الجاهز (PDF / Excel / CSV)
 */
@Composable
private fun CustomersFileReadyDialog(
    file: File,
    title: String,
    onFinished: () -> Unit
) {
    val context = LocalContext.current

    MosaribFileActionDialog(
        file = file,
        title = title,
        fileType = when {
            file.name.endsWith(".xlsx", true) -> "Excel"
            file.name.endsWith(".csv", true) -> "CSV"
            else -> "PDF"
        },
        onOpen = {
            if (file.name.endsWith(".xlsx", true)) {
                FileSharingHelper.openFile(context, file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            } else if (file.name.endsWith(".csv", true)) {
                FileSharingHelper.openFile(context, file, "text/csv")
            } else {
                FileSharingHelper.openPdf(context, file)
            }
            onFinished()
        },
        onShare = {
            if (file.name.endsWith(".xlsx", true)) {
                FileSharingHelper.shareFile(context, file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", title)
            } else if (file.name.endsWith(".csv", true)) {
                FileSharingHelper.shareFile(context, file, "text/csv", title)
            } else {
                FileSharingHelper.sharePdf(context, file, title)
            }
            onFinished()
        },
        onDismiss = onFinished
    )
}

/**
 * نافذة اختيار إرسال كشف الحساب (واتساب / رسالة نصية)
 */
@Composable
internal fun CustomerStatementMessageDialog(
    item: CustomerWithBalance,
    viewModel: CustomersViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val waMsg = viewModel.buildCustomerStatementMessage(item.customer, item, isSms = false)
    val smsMsg = viewModel.buildCustomerStatementMessage(item.customer, item, isSms = true)

    SendMessageChoiceDialog(
        recipientName = item.customer.name,
        recipientPhone = item.customer.phone,
        messageText = waMsg,
        smsMessageText = smsMsg,
        onDismiss = onDismiss,
        onSendWhatsApp = {
            onDismiss()
            FileSharingHelper.sendWhatsAppMessage(context, item.customer.phone, waMsg)
        },
        onSendSms = {
            onDismiss()
            FileSharingHelper.sendSms(context, item.customer.phone, smsMsg)
        }
    )
}

/**
 * نافذة الإضافة السريعة (عميل جديد / سقي / معاملة)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuickAddChoiceSheet(
    onDismiss: () -> Unit,
    onAddCustomer: () -> Unit,
    onAddSession: () -> Unit,
    onAddVoucher: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "إضافة سريعة",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 17.sp
                )
            )
            Text(
                text = "اختر الإجراء الذي تود تسجيله مباشرة",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                ),
                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
            )

            // 1. عميل جديد
            QuickAddOptionItem(
                icon = Icons.Default.PersonAdd,
                iconTint = PrimaryTeal,
                iconBg = PrimaryTeal.copy(alpha = 0.12f),
                title = "عميل أو صاحب بئر جديد",
                subtitle = "تسجيل حساب مزارع جديد أو صاحب بئر",
                onClick = onAddCustomer
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. سقي
            QuickAddOptionItem(
                icon = Icons.Default.WaterDrop,
                iconTint = Color(0xFF0288D1),
                iconBg = Color(0xFF0288D1).copy(alpha = 0.12f),
                title = "تسجيل دورة سقي",
                subtitle = "حساب ساعات ري مزارع وتحديث الحساب",
                onClick = onAddSession
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. معاملة
            QuickAddOptionItem(
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                iconTint = AccentGold,
                iconBg = AccentGold.copy(alpha = 0.14f),
                title = "معاملة مالية (سند)",
                subtitle = "تسجيل سند قبض نقد/بنك أو سند صرف",
                onClick = onAddVoucher
            )

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
private fun QuickAddOptionItem(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                )
            }
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * تسجيل دورة سقي يدوية مباشرة من شاشة الحسابات
 */
@Composable
internal fun QuickAddSessionSheet(
    customers: List<Customer>,
    defaultPricePerHour: Double,
    currencySymbol: String,
    sessionsViewModel: SessionsViewModel?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    AddEditSessionBottomSheet(
        initialSession = null,
        customers = customers,
        defaultPricePerHour = defaultPricePerHour,
        currencySymbol = currencySymbol,
        onCreateCustomer = { customer ->
            sessionsViewModel?.createCustomer(customer) ?: 0L
        },
        onDismiss = onDismiss,
        onSave = { id, custId, pumpName, startTime, endTime, hrs, mins, rate, paid, notes, billedTo, wastedMins, wastedReason, discount, costRate, pumpId ->
            sessionsViewModel?.saveManualSession(
                id = id,
                customerId = custId,
                pumpName = pumpName,
                startTime = startTime,
                endTime = endTime,
                hours = hrs,
                minutes = mins,
                pricePerHour = rate,
                amountPaid = paid,
                notes = notes,
                billedToCustomerId = billedTo,
                wastedMinutes = wastedMins,
                wastedReason = wastedReason,
                discountAmount = discount,
                costPricePerHour = costRate,
                pumpSourceId = pumpId
            )
            onSaved()
        }
    )
}

/**
 * تسجيل معاملة مالية (سند) مباشرة من شاشة الحسابات
 */
@Composable
internal fun QuickAddVoucherSheet(
    customers: List<Customer>,
    allSessions: List<WaterSession>,
    currencySymbol: String,
    vouchersViewModel: VouchersViewModel?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    AddVoucherBottomSheet(
        initialType = VoucherType.RECEIPT,
        customers = customers,
        allSessions = allSessions,
        currencySymbol = currencySymbol,
        onDismiss = onDismiss,
        onSave = { type, customerId, amount, category, method, notes, sessionId, selectedSessionIds, discountAmount ->
            vouchersViewModel?.addVoucher(
                type = type,
                customerId = customerId,
                amount = amount,
                category = category,
                paymentMethod = method,
                notes = notes,
                sessionId = sessionId,
                selectedSessionIds = selectedSessionIds,
                discountAmount = discountAmount
            )
            onSaved()
        }
    )
}

/**
 * تأكيد أرشفة الحساب
 */
@Composable
internal fun ArchiveCustomerDialog(
    customer: Customer,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("أرشفة الحساب", fontWeight = FontWeight.Bold) },
        text = { Text("سيتم أرشفة حساب (${customer.name}) وإخفاؤه مع الاحتفاظ بجميع عملياته.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
            ) {
                Text("حذف نهائي")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

/**
 * إضافة / تعديل مجموعة مخصصة مع أعضائها
 */
@Composable
internal fun CustomerGroupEditorDialog(
    groupToEdit: String?,
    initialMemberIdsForEdit: Set<Long>,
    rawCustomers: List<CustomerWithBalance>,
    onDismiss: () -> Unit,
    onSave: (name: String, selectedIds: Set<Long>) -> Unit
) {
    val initialIds = remember(groupToEdit, rawCustomers) {
        if (groupToEdit != null) {
            rawCustomers.filter { it.customer.customGroup.trim().equals(groupToEdit.trim(), ignoreCase = true) }
                .map { it.customer.id }.toSet()
        } else {
            initialMemberIdsForEdit
        }
    }
    AddEditGroupDialog(
        initialGroupName = groupToEdit,
        initialMemberIds = initialIds,
        allCustomers = rawCustomers,
        onDismiss = onDismiss,
        onSave = onSave
    )
}
