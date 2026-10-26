package com.shopmate.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {

    @Test
    fun `the right PIN matches and a wrong one does not`() {
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash("4821", salt)
        assertTrue(PinHasher.matches("4821", salt, hash))
        assertFalse(PinHasher.matches("4812", salt, hash))
    }

    @Test
    fun `the same PIN hashes differently under different salts`() {
        assertNotEquals(PinHasher.hash("0000", PinHasher.newSalt()), PinHasher.hash("0000", PinHasher.newSalt()))
    }

    @Test
    fun `hashing is repeatable for one salt`() {
        val salt = PinHasher.newSalt()
        assertEquals(PinHasher.hash("1234", salt), PinHasher.hash("1234", salt))
    }

    @Test
    fun `only four digits make a PIN`() {
        assertTrue(PinHasher.isValidPin("0912"))
        assertFalse(PinHasher.isValidPin("912"))
        assertFalse(PinHasher.isValidPin("09123"))
        assertFalse(PinHasher.isValidPin("09a2"))
    }
}
