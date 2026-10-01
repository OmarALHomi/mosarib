package com.example.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.example.core.database.AppDatabase
import com.example.features.customers.Customer
import com.example.features.pumps.PumpSource
import com.example.features.sessions.WaterSession
import com.example.features.settings.AppSetting
import com.example.features.vouchers.Voucher
import com.example.features.vouchers.VoucherType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {

    suspend fun createBackupJson(context: Context, database: AppDatabase): File = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("app", "Mosarib")
        root.put("version", 1)
        root.put("createdAt", System.currentTimeMillis())
        root.put("formattedDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))

        // Customers
        val customers = database.customerDao().getAllCustomers().first()
        val customersArray = JSONArray()
        customers.forEach { c ->
            val obj = JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("phone", c.phone)
                put("farmName", c.farmName)
                put("location", c.location)
                put("notes", c.notes)
                c.customPricePerHour?.let { put("customPricePerHour", it) }
                put("createdAt", c.createdAt)
                put("isArchived", c.isArchived)
            }
            customersArray.put(obj)
        }
        root.put("customers", customersArray)

        // Water Sessions
        val sessions = database.waterSessionDao().getAllSessions().first()
        val sessionsArray = JSONArray()
        sessions.forEach { s ->
            val obj = JSONObject().apply {
                put("id", s.id)
                put("customerId", s.customerId)
                put("pumpName", s.pumpName)
                put("startTime", s.startTime)
                put("endTime", s.endTime)
                put("durationMinutes", s.durationMinutes)
                put("pricePerHour", s.pricePerHour)
                put("totalAmount", s.totalAmount)
                put("amountPaid", s.amountPaid)
                put("remainingDebt", s.remainingDebt)
                put("notes", s.notes)
                put("isLive", s.isLive)
                put("createdAt", s.createdAt)
            }
            sessionsArray.put(obj)
        }
        root.put("sessions", sessionsArray)

        // Vouchers
        val vouchers = database.voucherDao().getAllVouchers().first()
        val vouchersArray = JSONArray()
        vouchers.forEach { v ->
            val obj = JSONObject().apply {
                put("id", v.id)
                put("voucherNumber", v.voucherNumber)
                put("type", v.type.name)
                v.customerId?.let { put("customerId", it) }
                put("amount", v.amount)
                put("category", v.category)
                put("paymentMethod", v.paymentMethod)
                put("date", v.date)
                put("notes", v.notes)
                put("createdAt", v.createdAt)
            }
            vouchersArray.put(obj)
        }
        root.put("vouchers", vouchersArray)

        // Pumps
        val pumps = database.pumpSourceDao().getAllPumps().first()
        val pumpsArray = JSONArray()
        pumps.forEach { p ->
            val obj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("locationOrWellNumber", p.locationOrWellNumber)
                put("defaultPricePerHour", p.defaultPricePerHour)
                put("powerType", p.powerType)
                put("notes", p.notes)
                put("isPrimary", p.isPrimary)
                put("isActive", p.isActive)
            }
            pumpsArray.put(obj)
        }
        root.put("pumps", pumpsArray)

        // Settings
        val settings = database.appSettingDao().getAllSettings().first()
        val settingsArray = JSONArray()
        settings.forEach { st ->
            val obj = JSONObject().apply {
                put("key", st.key)
                put("value", st.value)
            }
            settingsArray.put(obj)
        }
        root.put("settings", settingsArray)

        val backupDir = File(context.filesDir, "backups")
        if (!backupDir.exists()) backupDir.mkdirs()

        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val backupFile = File(backupDir, "mosarib_backup_$dateStr.back")
        FileOutputStream(backupFile).use { fos ->
            fos.write(root.toString(2).toByteArray(Charsets.UTF_8))
        }

        backupFile
    }

    data class BackupFileInfo(
        val file: File,
        val name: String,
        val formattedDate: String,
        val sizeText: String
    )

    fun getAvailableBackups(context: Context): List<BackupFileInfo> {
        val backupDir = File(context.filesDir, "backups")
        if (!backupDir.exists()) return emptyList()
        val sdf = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.forLanguageTag("ar"))
        return backupDir.listFiles { _, name -> name.endsWith(".back") || name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }
            ?.map { file ->
                val sizeKb = (file.length() / 1024.0)
                val sizeStr = if (sizeKb < 1.0) "${file.length()} بايت" else String.format(Locale.US, "%.1f ك.ب", sizeKb)
                BackupFileInfo(
                    file = file,
                    name = file.name,
                    formattedDate = sdf.format(Date(file.lastModified())),
                    sizeText = sizeStr
                )
            } ?: emptyList()
    }

    fun deleteBackup(file: File): Boolean {
        return runCatching { if (file.exists()) file.delete() else false }.getOrDefault(false)
    }

    suspend fun restoreFromFile(context: Context, database: AppDatabase, file: File): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val jsonString = file.readText(Charsets.UTF_8)
            val root = JSONObject(jsonString)
            restoreFromJsonObject(database, root)
        }
    }

    suspend fun restoreFromJson(context: Context, database: AppDatabase, uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val stringBuilder = java.lang.StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line)
                        line = reader.readLine()
                    }
                }
            }

            val root = JSONObject(stringBuilder.toString())
            restoreFromJsonObject(database, root)
        }
    }

    private suspend fun restoreFromJsonObject(database: AppDatabase, root: JSONObject): Int = database.withTransaction {
        var count = 0

        // Restore Customers
        if (root.has("customers")) {
            val array = root.getJSONArray("customers")
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val customer = Customer(
                    id = obj.optLong("id", 0),
                    name = obj.getString("name"),
                    phone = obj.optString("phone", ""),
                    farmName = obj.optString("farmName", ""),
                    location = obj.optString("location", ""),
                    notes = obj.optString("notes", ""),
                    customPricePerHour = if (obj.has("customPricePerHour")) obj.getDouble("customPricePerHour") else null,
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    isArchived = obj.optBoolean("isArchived", false)
                )
                database.customerDao().insertCustomer(customer)
                count++
            }
        }

        // Restore Pumps
        if (root.has("pumps")) {
            val array = root.getJSONArray("pumps")
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val pump = PumpSource(
                    id = obj.optLong("id", 0),
                    name = obj.getString("name"),
                    locationOrWellNumber = obj.optString("locationOrWellNumber", ""),
                    defaultPricePerHour = obj.optDouble("defaultPricePerHour", 5000.0),
                    powerType = obj.optString("powerType", "ديزل"),
                    notes = obj.optString("notes", ""),
                    isPrimary = obj.optBoolean("isPrimary", false),
                    isActive = obj.optBoolean("isActive", true)
                )
                database.pumpSourceDao().insertPump(pump)
                count++
            }
        }

        // Restore Water Sessions
        if (root.has("sessions")) {
            val array = root.getJSONArray("sessions")
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val session = WaterSession(
                    id = obj.optLong("id", 0),
                    customerId = obj.getLong("customerId"),
                    pumpName = obj.optString("pumpName", "البئر"),
                    startTime = obj.optLong("startTime", System.currentTimeMillis()),
                    endTime = obj.optLong("endTime", System.currentTimeMillis()),
                    durationMinutes = obj.optInt("durationMinutes", 0),
                    pricePerHour = obj.optDouble("pricePerHour", 5000.0),
                    totalAmount = obj.optDouble("totalAmount", 0.0),
                    amountPaid = obj.optDouble("amountPaid", 0.0),
                    remainingDebt = obj.optDouble("remainingDebt", 0.0),
                    notes = obj.optString("notes", ""),
                    isLive = obj.optBoolean("isLive", false),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
                database.waterSessionDao().insertSession(session)
                count++
            }
        }

        // Restore Vouchers
        if (root.has("vouchers")) {
            val array = root.getJSONArray("vouchers")
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val typeStr = obj.optString("type", "RECEIPT")
                val vType = runCatching { VoucherType.valueOf(typeStr) }.getOrDefault(VoucherType.RECEIPT)
                val voucher = Voucher(
                    id = obj.optLong("id", 0),
                    voucherNumber = obj.optString("voucherNumber", ""),
                    type = vType,
                    customerId = if (obj.has("customerId") && !obj.isNull("customerId")) obj.getLong("customerId") else null,
                    amount = obj.optDouble("amount", 0.0),
                    category = obj.optString("category", "عام"),
                    paymentMethod = obj.optString("paymentMethod", "نقداً"),
                    date = obj.optLong("date", System.currentTimeMillis()),
                    notes = obj.optString("notes", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
                database.voucherDao().insertVoucher(voucher)
                count++
            }
        }

        // Restore Settings
        if (root.has("settings")) {
            val array = root.getJSONArray("settings")
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val key = obj.getString("key")
                val value = obj.getString("value")
                database.appSettingDao().saveSetting(AppSetting(key, value))
            }
        }

        count
    }

    fun shareBackupToDriveOrApps(context: Context, backupFile: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            backupFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "نسخة احتياطية - تطبيق المُسَرِّب")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "حفظ في Google Drive أو مشاركة النسخة الاحتياطية").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    /**
     * Attempts to send the backup file directly to Google Drive app.
     * Falls back to general chooser if Google Drive is not installed.
     */
    fun shareBackupDirectToDrive(context: Context, backupFile: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            backupFile
        )

        val driveIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "نسخة احتياطية - تطبيق المُسَرِّب")
            `package` = "com.google.android.apps.docs"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val pm = context.packageManager
        if (driveIntent.resolveActivity(pm) != null) {
            context.startActivity(driveIntent)
        } else {
            // Google Drive not installed or cannot resolve — fallback to system chooser
            shareBackupToDriveOrApps(context, backupFile)
        }
    }

    suspend fun saveBackupToPhoneDownloads(context: Context, database: AppDatabase): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val backupFile = createBackupJson(context, database)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, backupFile.name)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS + "/MosaribBackups")
                }
                val uri = context.contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        backupFile.inputStream().use { it.copyTo(os) }
                    }
                }
            } else {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                val subDir = File(downloadsDir, "MosaribBackups")
                if (!subDir.exists()) subDir.mkdirs()
                val targetFile = File(subDir, backupFile.name)
                backupFile.copyTo(targetFile, overwrite = true)
            }
            backupFile
        }
    }
}
