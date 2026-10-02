package com.example.features.farmer

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.text.NumberFormat
import java.util.Locale

/**
 * Represents an agricultural expense for a farmer or buyer (spraying, fertilizer, labor, diesel).
 * Used to calculate net farm profit and cost per harvest.
 */
@Entity(tableName = "farm_expenses")
data class FarmExpense(
    @PrimaryKey
    val id: String,
    val farmName: String,
    val expenseCategory: String, // "رش ومبيدات", "سماد وتغذية", "أجور عمال", "ديزل وطاقة", "صيانة وشبكات", "أخرى"
    val amount: Double,
    val notes: String = "",
    val date: Long = System.currentTimeMillis()
) {
    fun formatAmount(): String {
        return NumberFormat.getNumberInstance(Locale.US).format(amount) + " ريال"
    }
}

@Dao
interface FarmExpenseDao {
    @Query("SELECT * FROM farm_expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<FarmExpense>>

    @Query("SELECT SUM(amount) FROM farm_expenses")
    fun getTotalExpenses(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: FarmExpense)

    @Query("DELETE FROM farm_expenses WHERE id = :id")
    suspend fun deleteExpense(id: String)
}
