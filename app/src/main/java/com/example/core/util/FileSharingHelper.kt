package com.example.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object FileSharingHelper {

    fun shareFile(context: Context, file: File, mimeType: String, title: String = "مشاركة الملف") {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Toast.makeText(context, "فشل في مشاركة الملف: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun openFile(context: Context, file: File, mimeType: String, title: String = "فتح الملف") {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(viewIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح الملف مباشرة، يرجى مشاركته: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun sharePdf(context: Context, file: File, title: String = "مشاركة المستند") {
        shareFile(context, file, "application/pdf", title)
    }

    fun openPdf(context: Context, file: File) {
        openFile(context, file, "application/pdf", "فتح ملف PDF")
    }

    fun shareText(context: Context, text: String, title: String = "مشاركة") {
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, attachMessageFooter(text))
                type = "text/plain"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val shareIntent = Intent.createChooser(sendIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر مشاركة النص: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    const val MESSAGE_FOOTER = "\n\n---\nتطبيق المسرّب | م. عمر الحومي\nwa.me/967773712030"

    /**
     * يضيف فوتر مختصر للرسالة يحتوي على اسم التطبيق والمطور ورابط الواتساب المباشر
     */
    fun attachMessageFooter(message: String): String {
        val trimmed = message.trimEnd()
        return if (trimmed.contains("wa.me/967773712030")) {
            trimmed
        } else {
            "$trimmed$MESSAGE_FOOTER"
        }
    }

    fun sendWhatsAppMessage(context: Context, phone: String, message: String) {
        try {
            val finalMsg = attachMessageFooter(message)
            val cleanPhone = normalizePhone(phone)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(finalMsg)}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح تطبيق واتساب: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendSms(context: Context, phone: String, message: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phone")
                putExtra("sms_body", message.trimEnd())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح تطبيق الرسائل: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun makePhoneCall(context: Context, phone: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phone")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح لوحة الاتصال: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun convertArabicDigitsToAscii(str: String): String {
        val builder = StringBuilder(str.length)
        for (ch in str) {
            when (ch) {
                in '٠'..'٩' -> builder.append('0' + (ch - '٠'))
                in '۰'..'۹' -> builder.append('0' + (ch - '۰'))
                else -> builder.append(ch)
            }
        }
        return builder.toString()
    }

    /**
     * يضيف +967 (اليمن) تلقائياً إذا لم يكن الرقم يبدأ بمفتاح دولي.
     * يُرجع الرقم بدون + (صالح لـ WhatsApp API).
     */
    fun normalizePhone(phone: String): String {
        val ascii = convertArabicDigitsToAscii(phone)
        val digits = ascii.replace(Regex("[^0-9+]"), "")
        return when {
            digits.startsWith("+")   -> digits.removePrefix("+")
            digits.startsWith("00")  -> digits.removePrefix("00")
            digits.startsWith("967") -> digits
            digits.startsWith("0")   -> "967${digits.removePrefix("0")}"
            digits.isNotEmpty()      -> "967$digits"
            else                     -> digits
        }
    }
}
