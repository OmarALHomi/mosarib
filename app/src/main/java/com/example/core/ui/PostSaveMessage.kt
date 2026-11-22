package com.example.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.speech.SpeakButton
import com.example.core.util.FileSharingHelper
import com.example.core.util.MessageFormatHelper
import com.example.features.customers.Customer
import com.example.features.sessions.WaterSession
import com.example.features.settings.AppConfig
import com.example.features.vouchers.Voucher
import com.example.features.vouchers.VoucherType
import com.example.features.wellowners.WellOwnerPurchase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What to offer (copy / WhatsApp / SMS) right after a new operation was saved. */
data class PostSaveMessage(
    val recipientName: String,
    val recipientPhone: String,
    val whatsAppText: String,
    val smsText: String
)

/** App-level holder: view models call [post] after the insert; MainScreen shows [PostSaveMessageHost]. */
object PostSaveMessages {
    private val _pending = MutableStateFlow<PostSaveMessage?>(null)
    val pending: StateFlow<PostSaveMessage?> = _pending.asStateFlow()

    fun post(recipient: Customer, whatsAppText: String, smsText: String) {
        _pending.value = PostSaveMessage(recipient.name, recipient.phone, whatsAppText, smsText)
    }

    fun postSession(session: WaterSession, customer: Customer, sponsor: Customer?, config: AppConfig) = post(
        customer,
        MessageFormatHelper.buildSessionBill(session, customer, config, isSms = false, sponsorCustomer = sponsor, beneficiaryCustomer = customer),
        MessageFormatHelper.buildSessionBill(session, customer, config, isSms = true, sponsorCustomer = sponsor, beneficiaryCustomer = customer)
    )

    fun postVoucher(voucher: Voucher, customer: Customer, config: AppConfig) {
        if (voucher.type == VoucherType.RECEIPT) post(
            customer,
            MessageFormatHelper.buildReceiptVoucher(voucher, customer, voucher.sessionId, config, isSms = false),
            MessageFormatHelper.buildReceiptVoucher(voucher, customer, voucher.sessionId, config, isSms = true)
        ) else post(
            customer,
            MessageFormatHelper.buildDisbursementVoucher(voucher, customer, config, isSms = false),
            MessageFormatHelper.buildDisbursementVoucher(voucher, customer, config, isSms = true)
        )
    }

    fun postPurchase(purchase: WellOwnerPurchase, owner: Customer, config: AppConfig) = post(
        owner,
        MessageFormatHelper.buildPurchaseBill(purchase, owner, config, isSms = false),
        MessageFormatHelper.buildPurchaseBill(purchase, owner, config, isSms = true)
    )

    fun clear() {
        _pending.value = null
    }
}

@Composable
fun PostSaveMessageHost() {
    val context = LocalContext.current
    val msg by PostSaveMessages.pending.collectAsStateWithLifecycle()
    msg?.let { m ->
        SendMessageChoiceDialog(
            recipientName = m.recipientName,
            recipientPhone = m.recipientPhone,
            messageText = m.whatsAppText,
            smsMessageText = m.smsText,
            onDismiss = { PostSaveMessages.clear() },
            onSendWhatsApp = {
                PostSaveMessages.clear()
                FileSharingHelper.sendWhatsAppMessage(context, m.recipientPhone, m.whatsAppText)
            },
            onSendSms = {
                PostSaveMessages.clear()
                FileSharingHelper.sendSms(context, m.recipientPhone, m.smsText)
            }
        )
    }
}

/** «تأكيد العملية»: the statement, a read-aloud button, «تأكيد الحفظ» and «تعديل» (form stays as typed). */
@Composable
fun OperationConfirmSheet(
    statement: String,
    spoken: String,
    onConfirm: () -> Unit,
    onEdit: () -> Unit
) {
    MosaribBottomSheet(
        onDismissRequest = onEdit,
        title = "تأكيد العملية",
        icon = Icons.Default.Check,
        footer = {
            MosaribPrimaryButton(text = "تأكيد الحفظ", onClick = onConfirm, modifier = Modifier.weight(1f))
            MosaribSecondaryButton(text = "تعديل", onClick = onEdit, modifier = Modifier.weight(1f))
        }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = statement,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, lineHeight = 24.sp),
                    modifier = Modifier.weight(1f)
                )
                SpeakButton(text = { spoken })
            }
        }
    }
}
