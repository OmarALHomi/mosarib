package com.baynana.features.sessions

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterSessionDao {
    @Query("SELECT COUNT(*) FROM water_sessions")
    fun getSessionsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM water_sessions")
    suspend fun getSessionsCountDirect(): Int

    @Query("SELECT * FROM water_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<WaterSession>>

    @Query("SELECT * FROM water_sessions WHERE customerId = :customerId OR billedToCustomerId = :customerId ORDER BY startTime DESC")
    fun getSessionsForCustomer(customerId: Long): Flow<List<WaterSession>>

    @Query("SELECT * FROM water_sessions WHERE startTime >= :fromTime AND startTime <= :toTime ORDER BY startTime DESC")
    fun getSessionsBetween(fromTime: Long, toTime: Long): Flow<List<WaterSession>>

    @Query("SELECT * FROM water_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): WaterSession?

    /**
     * كل السقيات بلا حد وبلا ترشيح، للنسخ الاحتياطي وللترحيل من الإرث (ح٧).
     * لا تُستخدم في الشاشات: الشاشات تقرأ بتدفق وبترشيح.
     */
    @Query("SELECT * FROM water_sessions ORDER BY id ASC")
    suspend fun getAllSessionsForBackup(): List<WaterSession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WaterSession): Long

    @Update
    suspend fun updateSession(session: WaterSession)

    @Delete
    suspend fun deleteSession(session: WaterSession)

    @Query("DELETE FROM water_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)
}
