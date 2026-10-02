package com.example.features.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class AppConfig(
    val distributorName: String = "مستخدم جِربة",
    val distributorPhone: String = "777000000",
    val defaultPricePerHour: Double = 5000.0,
    val currencySymbol: String = "ر.ي",
    val themeMode: String = "SYSTEM", // LIGHT, DARK, SYSTEM
    val biometricEnabled: Boolean = false,
    val primaryRole: String = "MUSRIB", // MUSRIB, FARMER, DALLAL, BUYER
    val activeRoles: String = "MUSRIB,FARMER,DALLAL,BUYER",
    val userVillage: String = "",
    val isOnboardingCompleted: Boolean = false
) {
    fun hasRole(role: String): Boolean = activeRoles.split(",").contains(role)
}

class SettingsRepository(
    private val settingDao: AppSettingDao
) {
    val appConfig: Flow<AppConfig> = settingDao.getAllSettings().map { list ->
        val map = list.associate { it.key to it.value }
        AppConfig(
            distributorName = map["distributor_name"] ?: "مستخدم جِربة",
            distributorPhone = map["distributor_phone"] ?: "777000000",
            defaultPricePerHour = map["default_price_per_hour"]?.toDoubleOrNull() ?: 5000.0,
            currencySymbol = map["currency_symbol"] ?: "ر.ي",
            themeMode = map["theme_mode"] ?: "SYSTEM",
            biometricEnabled = map["biometric_enabled"]?.toBooleanStrictOrNull() ?: false,
            primaryRole = map["primary_role"] ?: "MUSRIB",
            activeRoles = map["active_roles"] ?: "MUSRIB,FARMER,DALLAL,BUYER",
            userVillage = map["user_village"] ?: "",
            isOnboardingCompleted = map["onboarding_completed"]?.toBooleanStrictOrNull() ?: false
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

    suspend fun updateBiometricEnabled(enabled: Boolean) {
        settingDao.saveSetting(AppSetting("biometric_enabled", enabled.toString()))
    }

    suspend fun updateRoles(primaryRole: String, activeRoles: String) {
        settingDao.saveSetting(AppSetting("primary_role", primaryRole))
        settingDao.saveSetting(AppSetting("active_roles", activeRoles))
    }

    suspend fun completeOnboarding(
        name: String,
        phone: String,
        village: String,
        primaryRole: String,
        activeRoles: String
    ) {
        settingDao.saveSetting(AppSetting("distributor_name", name))
        settingDao.saveSetting(AppSetting("distributor_phone", phone))
        settingDao.saveSetting(AppSetting("user_village", village))
        settingDao.saveSetting(AppSetting("primary_role", primaryRole))
        settingDao.saveSetting(AppSetting("active_roles", activeRoles))
        settingDao.saveSetting(AppSetting("onboarding_completed", "true"))
    }
}
