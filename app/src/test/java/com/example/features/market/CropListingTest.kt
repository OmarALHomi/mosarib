package com.example.features.market

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CropListingTest {

    @Test
    fun getDisplayPrice_whenZero_returnsNegotiableText() {
        val listing = CropListing(
            id = "test-1",
            title = "قات شامي",
            cropType = "قات",
            priceEstimate = 0.0,
            priceUnit = "شروة كاملة"
        )

        assertEquals("على السوم والمفاوضة", listing.getDisplayPrice())
    }

    @Test
    fun getDisplayPrice_whenPositive_formatsWithCurrencyAndUnit() {
        val listing = CropListing(
            id = "test-2",
            title = "رمان صعدي درجة أولى",
            cropType = "رمان",
            priceEstimate = 1500000.0,
            priceUnit = "شروة كاملة"
        )

        assertEquals("1,500,000 ريال (شروة كاملة)", listing.getDisplayPrice())
    }

    @Test
    fun brokerPrivacy_defaultHidesFarmerPhone() {
        val listing = CropListing(
            id = "test-3",
            title = "عنب عاصمي",
            cropType = "عنب",
            farmerName = "أبو صالح",
            farmerPhone = "771234567",
            hideFarmerPhone = true
        )

        assertTrue("Farmer phone should be protected by default", listing.hideFarmerPhone)
    }

    @Test
    fun listingStatus_resolvesCorrectly() {
        val available = CropListing(id = "1", title = "T", cropType = "C", status = "AVAILABLE")
        assertTrue(available.isAvailable)
        assertFalse(available.isSold)
        assertFalse(available.isInNegotiation)

        val sold = CropListing(id = "2", title = "T", cropType = "C", status = "SOLD")
        assertTrue(sold.isSold)
        assertFalse(sold.isAvailable)

        val inNegotiation = CropListing(id = "3", title = "T", cropType = "C", status = "IN_NEGOTIATION")
        assertTrue(inNegotiation.isInNegotiation)
        assertFalse(inNegotiation.isSold)
    }
}
