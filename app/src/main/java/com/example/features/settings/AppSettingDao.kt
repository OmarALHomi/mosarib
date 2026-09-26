package com.example.features.settings

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingDao {
    @Query("SELECT * FROM app_settings WHERE `key` = :key LIMIT 1")
    fun getSetting(key: String): Flow<AppSetting?>

    @Query("SELECT value FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSettingValue(key: String): String?

    @Query("SELECT * FROM app_settings")
    fun getAllSettings(): Flow<List<AppSetting>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSetting(setting: AppSetting)
}
