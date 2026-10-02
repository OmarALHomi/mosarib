package com.baynana.features.farmer

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Represents a Musrib irrigation account linked by a Farmer using a 5-character link code.
 * Cached locally in Room for offline access.
 */
@Entity(tableName = "linked_musribs")
data class LinkedMusrib(
    @PrimaryKey val linkCode: String,
    val musribName: String = "",
    val musribPhone: String = "",
    val farmName: String = "",
    val currentBalance: Double = 0.0,
    val totalDebit: Double = 0.0,
    val totalPaid: Double = 0.0,
    val lastSyncTimestamp: Long = 0L,
    val addedAt: Long = System.currentTimeMillis()
)

@Dao
interface LinkedMusribDao {
    @Query("SELECT * FROM linked_musribs ORDER BY addedAt DESC")
    fun getAllLinkedMusribs(): Flow<List<LinkedMusrib>>

    @Query("SELECT * FROM linked_musribs WHERE linkCode = :linkCode")
    suspend fun getByLinkCode(linkCode: String): LinkedMusrib?

    /** كل الروابط، للنسخ الاحتياطي. */
    @Query("SELECT * FROM linked_musribs ORDER BY addedAt ASC")
    suspend fun getAllLinkedMusribsForBackup(): List<LinkedMusrib>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(linkedMusrib: LinkedMusrib)

    @Update
    suspend fun update(linkedMusrib: LinkedMusrib)

    @Delete
    suspend fun delete(linkedMusrib: LinkedMusrib)
}
