package com.example.features.customers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.DialogTone
import com.example.core.ui.MosaribBottomSheet
import com.example.core.ui.MosaribDialog
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
import com.example.ui.theme.mosarib
import java.io.File

/**
 * نوافذ الاستيراد والتصدير المرتبطة بحالة الـ ViewModel
 */
@Composable
internal fun CustomersImportExportDialogs(viewModel: CustomersViewModel) {
    val pdfReady by viewModel.pdfReadyFile.collectAsStateWithLifecycle()
    val importDrafts by viewModel.importDrafts.collectAsStateWithLifecycle()
    val importSource by viewModel.detectedImportSource.collectAsStateWithLifecycle()
    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()
    val isSavingImport by viewModel.isSavingImport.collectAsStateWithLifecycle()
    val importProgress by viewModel.importProgress.collectAsStateWithLifecycle()
    val config by viewModel.appConfig.collectAsStateWithLifecycle()

    // شاشة مراجعة استيراد إكسل الذكية
    importDrafts?.let { drafts ->
        CustomerImportReviewDialog(
            drafts = drafts,
            detectedSource = importSource,
            currencySymbol = config.currencySymbol,
            isSavingImport = isSavingImport,
            onDraftUpdated = { viewModel.updateImportDraft(it) },
            onDraftDeleted = { viewModel.removeImportDraft(it) },
            onToggleSelectAll = { viewModel.toggleSelectAllImportDrafts(it) },
            onConfirmImport = { updateExisting, debitMode -> viewModel.confirmImport(updateExisting, debitMode) },
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

    // مؤشر جاري قيد وحفظ الحسابات والعمليات ومنع النقر المتكرر
    if (isSavingImport) {
        val prog = importProgress
        val sub = if (prog != null && prog.second > 0) {
            "تم إدخال ${prog.first} من ${prog.second} حساب وقيد العمليات في السجلات..."
        } else {
            "يتم تسجيل الحسابات وقيد العمليات في قاعدة البيانات..."
        }
        MosaribLoadingDialog(
            title = "جاري استيراد وحفظ البيانات",
            subTitle = sub
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
/**
 * نافذة الإضافة السريعة (عميل جديد / سقي / معاملة)
 */
@Composable
internal fun QuickAddChoiceSheet(
    onDismiss: () -> Unit,
    onAddCustomer: () -> Unit,
    onAddSession: () -> Unit,
    onAddVoucher: () -> Unit
) {
    MosaribBottomSheet(
        onDismissRequest = onDismiss,
        title = "إضافة سريعة",
        subtitle = "اختر الإجراء الذي تود تسجيله مباشرة"
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. عميل جديد
            QuickAddOptionItem(
                icon = Icons.Default.PersonAdd,
                iconTint = MaterialTheme.colorScheme.primary,
                iconBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                title = "عميل أو صاحب بئر جديد",
                subtitle = "تسجيل حساب مزارع جديد أو صاحب بئر",
                onClick = onAddCustomer
            )

            // 2. سقي
            QuickAddOptionItem(
                icon = Icons.Default.WaterDrop,
                iconTint = MaterialTheme.mosarib.info,
                iconBg = MaterialTheme.mosarib.info.copy(alpha = 0.12f),
                title = "تسجيل دورة سقي",
                subtitle = "حساب ساعات ري مزارع وتحديث الحساب",
                onClick = onAddSession
            )

            // 3. معاملة
            QuickAddOptionItem(
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                iconTint = MaterialTheme.mosarib.warning,
                iconBg = MaterialTheme.mosarib.warning.copy(alpha = 0.14f),
                title = "معاملة مالية (سند)",
                subtitle = "تسجيل سند قبض نقد/بنك أو سند صرف",
                onClick = onAddVoucher
            )
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
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
    MosaribDialog(
        onDismissRequest = onDismiss,
        title = "أرشفة الحساب",
        message = "سيتم أرشفة حساب (${customer.name}) وإخفاؤه مع الاحتفاظ بجميع عملياته.",
        tone = DialogTone.Danger,
        confirmText = "حذف نهائي",
        onConfirm = onConfirm,
        dismissText = "إلغاء",
        onDismiss = onDismiss
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
