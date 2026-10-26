package com.shopmate.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class LicenceTest {

    private val expires: Instant = Instant.parse("2026-11-22T00:00:00Z")

    private fun licence(storesUsed: Int = 1, staffUsed: Int = 2) = Licence(
        plan = LicencePlan.TRIAL,
        expiresAt = expires,
        graceEndsAt = expires.plus(Duration.ofDays(7)),
        maxStores = 1,
        maxStaff = 3,
        storesUsed = storesUsed,
        staffUsed = staffUsed,
    )

    @Test
    fun `active until a week before expiry`() {
        assertEquals(LicenceState.Active, licence().state(expires.minus(Duration.ofDays(8))))
    }

    @Test
    fun `ending counts whole days, rounding up`() {
        assertEquals(LicenceState.Ending(3), licence().state(expires.minus(Duration.ofHours(50))))
        assertEquals(LicenceState.Ending(1), licence().state(expires.minus(Duration.ofHours(2))))
    }

    @Test
    fun `grace runs for the week after expiry and does not block`() {
        val state = licence().state(expires.plus(Duration.ofDays(2)))
        assertEquals(LicenceState.Grace(5), state)
        assertFalse(state.blocksNewSales)
    }

    @Test
    fun `expired after grace blocks new sales`() {
        val state = licence().state(expires.plus(Duration.ofDays(7)).plusSeconds(1))
        assertEquals(LicenceState.Expired, state)
        assertTrue(state.blocksNewSales)
    }

    @Test
    fun `limits read from usage`() {
        assertFalse(licence(storesUsed = 1).canAddStore)
        assertTrue(licence(staffUsed = 2).canAddStaff)
        assertFalse(licence(staffUsed = 3).canAddStaff)
    }
}
