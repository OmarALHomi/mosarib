package com.example.core.util

import android.accounts.Account
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Google Drive AppData Backup Helper.
 * Uses the private, isolated scope: https://www.googleapis.com/auth/drive.appdata
 * Files stored in 'appDataFolder' are hidden from regular Google Drive UI,
 * preventing accidental modification or deletion by non-technical users.
 */
object GoogleDriveBackupHelper {

    const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    private const val DRIVE_API_URL = "https://www.googleapis.com/drive/v3/files"
    private const val DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"

    data class DriveBackupFile(
        val id: String,
        val name: String,
        val sizeBytes: Long,
        val createdTime: String,
        val formattedDate: String,
        val sizeText: String
    )

    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DRIVE_APPDATA_SCOPE))
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun getSignedInAccount(context: Context): GoogleSignInAccount? {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return if (account != null && GoogleSignIn.hasPermissions(account, Scope(DRIVE_APPDATA_SCOPE))) {
            account
        } else {
            null
        }
    }

    /**
     * Obtains an OAuth2 bearer token for the drive.appdata scope
     */
    suspend fun getAccessToken(context: Context, account: Account): String = withContext(Dispatchers.IO) {
        val scopeStr = "oauth2:$DRIVE_APPDATA_SCOPE"
        GoogleAuthUtil.getToken(context, account, scopeStr)
    }

    /**
     * Upload a local backup file (.back) directly into Google Drive's hidden appDataFolder
     */
    suspend fun uploadBackupToAppData(
        context: Context,
        account: GoogleSignInAccount,
        backupFile: File
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val androidAccount = account.account ?: throw IllegalStateException("حساب Google غير متوفر")
            val token = getAccessToken(context, androidAccount)

            val boundary = "=====MosaribDriveAppData${System.currentTimeMillis()}====="
            val lineEnd = "\r\n"
            val twoHyphens = "--"

            val url = URL(DRIVE_UPLOAD_URL)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                useCaches = false
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
                connectTimeout = 30000
                readTimeout = 30000
            }

            // Part 1: Metadata JSON
            val metadataJson = JSONObject().apply {
                put("name", backupFile.name)
                val parentsArray = org.json.JSONArray().apply { put("appDataFolder") }
                put("parents", parentsArray)
            }.toString()

            val fileBytes = backupFile.readBytes()

            DataOutputStream(conn.outputStream).use { dos ->
                // Part 1
                dos.writeBytes("$twoHyphens$boundary$lineEnd")
                dos.writeBytes("Content-Type: application/json; charset=UTF-8$lineEnd$lineEnd")
                dos.write(metadataJson.toByteArray(Charsets.UTF_8))
                dos.writeBytes(lineEnd)

                // Part 2: Media
                dos.writeBytes("$twoHyphens$boundary$lineEnd")
                dos.writeBytes("Content-Type: application/octet-stream$lineEnd$lineEnd")
                dos.write(fileBytes)
                dos.writeBytes(lineEnd)

                // End of multipart
                dos.writeBytes("$twoHyphens$boundary$twoHyphens$lineEnd")
                dos.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                val respJson = JSONObject(responseStr)
                respJson.getString("id")
            } else {
                val errorStr = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                throw Exception("فشل رفع النسخة إلى Drive ($responseCode): $errorStr")
            }
        }
    }

    /**
     * List backups stored in Google Drive's hidden appDataFolder
     */
    suspend fun listAppDataBackups(
        context: Context,
        account: GoogleSignInAccount
    ): Result<List<DriveBackupFile>> = withContext(Dispatchers.IO) {
        runCatching {
            val androidAccount = account.account ?: throw IllegalStateException("حساب Google غير متوفر")
            val token = getAccessToken(context, androidAccount)

            val queryUrl = "$DRIVE_API_URL?spaces=appDataFolder&fields=files(id,name,size,createdTime)&orderBy=createdTime%20desc"
            val conn = (URL(queryUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $token")
                connectTimeout = 20000
                readTimeout = 20000
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(responseStr)
                val filesArr = root.optJSONArray("files") ?: org.json.JSONArray()

                val resultList = mutableListOf<DriveBackupFile>()
                val parserSdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val displaySdf = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.forLanguageTag("ar"))

                for (i in 0 until filesArr.length()) {
                    val f = filesArr.getJSONObject(i)
                    val id = f.optString("id")
                    val name = f.optString("name")
                    val sizeBytes = f.optLong("size", 0L)
                    val createdTime = f.optString("createdTime", "")

                    var formattedDate = createdTime
                    try {
                        val cleanedTime = if (createdTime.length >= 19) createdTime.substring(0, 19) else createdTime
                        val parsed = parserSdf.parse(cleanedTime)
                        if (parsed != null) {
                            formattedDate = displaySdf.format(parsed)
                        }
                    } catch (_: Exception) {}

                    val sizeKb = sizeBytes / 1024.0
                    val sizeStr = if (sizeKb < 1.0) "$sizeBytes بايت" else String.format(Locale.US, "%.1f ك.ب", sizeKb)

                    resultList.add(
                        DriveBackupFile(
                            id = id,
                            name = name,
                            sizeBytes = sizeBytes,
                            createdTime = createdTime,
                            formattedDate = formattedDate,
                            sizeText = sizeStr
                        )
                    )
                }
                resultList
            } else {
                val errorStr = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                throw Exception("فشل جلب النسخ من Google Drive ($responseCode): $errorStr")
            }
        }
    }

    /**
     * Download a backup file from Google Drive appDataFolder to a local destination file
     */
    suspend fun downloadAppDataBackup(
        context: Context,
        account: GoogleSignInAccount,
        fileId: String,
        destFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val androidAccount = account.account ?: throw IllegalStateException("حساب Google غير متوفر")
            val token = getAccessToken(context, androidAccount)

            val downloadUrl = "$DRIVE_API_URL/$fileId?alt=media"
            val conn = (URL(downloadUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $token")
                connectTimeout = 30000
                readTimeout = 30000
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                if (destFile.parentFile?.exists() == false) destFile.parentFile?.mkdirs()
                conn.inputStream.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                destFile
            } else {
                val errorStr = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                throw Exception("فشل تنزيل النسخة من Google Drive ($responseCode): $errorStr")
            }
        }
    }

    /**
     * Delete a backup file from Google Drive appDataFolder
     */
    suspend fun deleteAppDataBackup(
        context: Context,
        account: GoogleSignInAccount,
        fileId: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val androidAccount = account.account ?: throw IllegalStateException("حساب Google غير متوفر")
            val token = getAccessToken(context, androidAccount)

            val deleteUrl = "$DRIVE_API_URL/$fileId"
            val conn = (URL(deleteUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                setRequestProperty("Authorization", "Bearer $token")
                connectTimeout = 20000
                readTimeout = 20000
            }

            val responseCode = conn.responseCode
            responseCode in 200..299 || responseCode == 204
        }
    }
}
