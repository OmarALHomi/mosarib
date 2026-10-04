package com.baynana.data.local.license

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * واجهة سجلّ الترخيص.
 *
 * القاعدة الحاكمة هنا: **الإدراج لا يُعيد التمديد**. `insertLicenseIfNew` تُرجع `-1` عند وجود
 * المفتاح، وهذه الإشارة هي التي توقف الاسترداد الثاني عند حدّه. أي مسار كتابة مستقبلي يجب أن
 * يمرّ من هذه الدالة أو يكرّر خطأ LIC-01 الذي أُصلح هنا.
 */
@Dao
interface LicenseDao {

    /** تُرجع رقم الصف، أو `-1` إن كان هذا المفتاح قد استُردّ سابقًا (مانع الـreplay). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLicenseIfNew(row: LicenseRow): Long

    @Insert
    suspend fun insertEvent(row: LicenseEventRow)

    @Query("SELECT * FROM licenses WHERE licenseId = :licenseId")
    suspend fun getLicense(licenseId: String): LicenseRow?

    /** أحدث استحقاق سارٍ لمهنة، أو `null`. */
    @Query("SELECT * FROM licenses WHERE role = :role AND expiresAt > :now ORDER BY expiresAt DESC LIMIT 1")
    suspend fun activeLicense(role: String, now: Long): LicenseRow?

    @Query("SELECT * FROM licenses ORDER BY grantedAt DESC")
    fun observeAll(): Flow<List<LicenseRow>>

    /** كل مفاتيح ما استُردّ: تُقرأ داخل معاملة الاسترداد لبناء قرار الفاحص. */
    @Query("SELECT licenseId FROM licenses UNION SELECT licenseId FROM license_events WHERE outcome = 'GRANTED'")
    suspend fun redeemedKeys(): List<String>

    @Query("SELECT * FROM license_events ORDER BY occurredAt DESC LIMIT :limit")
    fun observeEvents(limit: Int): Flow<List<LicenseEventRow>>

    @Query("SELECT COUNT(*) FROM licenses")
    suspend fun countLicenses(): Int
}
