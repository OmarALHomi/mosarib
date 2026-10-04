package com.baynana.data.local.handover

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HandoverDao {

    /**
     * إدراج سجلّ حزمة. `IGNORE` مقصود: الفهرس الفريد على (bundleId, direction) هو مانع الاستيراد
     * المزدوج، والقيمة المرجعة `-1` هي إعلان «هذه الحزمة سبق أن دخلت» — ولا يُطبَّق منها شيء.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLogIfNew(row: HandoverLogRow): Long

    @Query("SELECT COUNT(*) FROM handover_log WHERE bundleId = :bundleId AND direction = :direction")
    suspend fun countBundle(bundleId: String, direction: String): Int

    @Query("SELECT * FROM handover_log ORDER BY createdAt DESC LIMIT :limit")
    fun observeLog(limit: Int = 40): Flow<List<HandoverLogRow>>

    @Query("SELECT * FROM handover_log ORDER BY createdAt DESC LIMIT :limit")
    suspend fun recentLog(limit: Int = 40): List<HandoverLogRow>

    /** دعوة واردة: تُدرج بقيد التكرار نفسه، فلا تتضاعف دعوة واحدة بتكرار الحزمة. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInviteIfNew(row: PendingInviteRow): Long

    @Query("SELECT * FROM pending_invites ORDER BY receivedAt DESC")
    fun observeInvites(): Flow<List<PendingInviteRow>>

    @Query("SELECT * FROM pending_invites WHERE status = :status ORDER BY receivedAt DESC LIMIT :limit")
    suspend fun invitesByStatus(status: String, limit: Int = 20): List<PendingInviteRow>

    @Query("SELECT * FROM pending_invites WHERE id = :id")
    suspend fun invite(id: String): PendingInviteRow?

    @Query("UPDATE pending_invites SET status = :status WHERE id = :id")
    suspend fun setInviteStatus(id: String, status: String)

    @Query("SELECT COUNT(*) FROM pending_invites WHERE status = :status")
    suspend fun countInvites(status: String): Int

    // ------------------------------------------ عناصر بانتظار قبول دعوة الغرفة

    /** `IGNORE` هنا أيضًا: نفس العنصر لا يُخزَّن مرتين ولو تكرّرت الحزمة أو وصلت في حزمتين. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPendingItemIfNew(row: PendingItemRow): Long

    @Query("SELECT * FROM pending_items WHERE roomId = :roomId ORDER BY createdAt ASC")
    suspend fun pendingItemsForRoom(roomId: String): List<PendingItemRow>

    @Query("SELECT COUNT(*) FROM pending_items")
    fun observePendingItemCount(): Flow<Int>

    @Query("DELETE FROM pending_items WHERE operationId = :operationId")
    suspend fun deletePendingItem(operationId: String)

    @Query("DELETE FROM pending_items WHERE roomId = :roomId")
    suspend fun deletePendingItemsForRoom(roomId: String)
}
