package com.example.features.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class AppConfig(
    val distributorName: String = "موزع الماء / المسرب",
    val distributorPhone: String = "777000000",
    val defaultPricePerHour: Double = 5000.0,
    val currencySymbol: String = "ر.ي",
    val themeMode: String = "SYSTEM" // LIGHT, DARK, SYSTEM
)

class SettingsRepository(
    private val settingDao: AppSettingDao
) {
    val appConfig: Flow<AppConfig> = settingDao.getAllSettings().map { list ->
        val map = list.associate { it.key to it.value }
        AppConfig(
            distributorName = map["distributor_name"] ?: "موزع الماء / المسرب",
            distributorPhone = map["distributor_phone"] ?: "777000000",
            defaultPricePerHour = map["default_price_per_hour"]?.toDoubleOrNull() ?: 5000.0,
            currencySymbol = map["currency_symbol"] ?: "ر.ي",
            themeMode = map["theme_mode"] ?: "SYSTEM"
        )
    }

    suspend fun updateDistributorName(name: String) {
        settingDao.saveSetting(AppSetting("distributor_name", name))
    }

    suspend fun updateDistributorPhone(phone: String) {
        settingDao.saveSetting(AppSetting("distributor_phone", phone))
    }

    suspend fun updateDefaultPricePerHour(price: Double) {
        settingDao.saveSetting(AppSetting("default_price_per_hour", price.toString()))
    }

    suspend fun updateCurrencySymbol(symbol: String) {
        settingDao.saveSetting(AppSetting("currency_symbol", symbol))
    }

    suspend fun updateThemeMode(mode: String) {
        settingDao.saveSetting(AppSetting("theme_mode", mode))
    }
}
