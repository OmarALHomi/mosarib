package com.example.features.farmer

import com.example.features.settings.AppConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FarmExpenseTest {

    @Test
    fun farmExpense_formatsAmountCorrectly() {
        val expense = FarmExpense(
            id = "exp-1",
            farmName = "مزرعة ضبر",
            expenseCategory = "سماد وتغذية",
            amount = 45000.0,
            notes = "سماد يوريا بلدي"
        )

        assertEquals("45,000 ريال", expense.formatAmount())
    }

    @Test
    fun appConfig_hasRole_resolvesRolesCorrectly() {
        val config = AppConfig(
            primaryRole = "FARMER",
            activeRoles = "FARMER,BUYER"
        )

        assertTrue(config.hasRole("FARMER"))
        assertTrue(config.hasRole("BUYER"))
        assertFalse(config.hasRole("DALLAL"))
        assertFalse(config.hasRole("MUSRIB"))
    }
}
