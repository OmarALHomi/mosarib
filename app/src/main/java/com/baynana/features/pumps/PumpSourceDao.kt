package com.baynana.features.pumps

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PumpSourceDao {
    @Query("SELECT * FROM pump_sources WHERE isActive = 1 ORDER BY isPrimary DESC, name ASC")
    fun getAllPumps(): Flow<List<PumpSource>>

    /**
     * كل المصادر بما فيها المعطّلة، للنسخ الاحتياطي فقط.
     * [getAllPumps] يُسقط غير النشط، والنسخة الاحتياطية يجب ألا تُسقط شيئًا.
     */
    @Query("SELECT * FROM pump_sources ORDER BY id ASC")
    suspend fun getAllPumpsForBackup(): List<PumpSource>

    @Query("SELECT * FROM pump_sources WHERE id = :id")
    suspend fun getPumpById(id: Long): PumpSource?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPump(pump: PumpSource): Long

    @Update
    suspend fun updatePump(pump: PumpSource)

    @Delete
    suspend fun deletePump(pump: PumpSource)
}
