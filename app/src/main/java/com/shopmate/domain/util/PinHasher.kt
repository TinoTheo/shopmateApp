package com.shopmate.domain.util

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Till PINs are stored only as PBKDF2-SHA256 hashes with a per-PIN salt (CLAUDE.md § Staff). The
 * hash is made on the till that sets the PIN and checked on any till of the store, offline.
 *
 * A 4-digit PIN has 10,000 values, so no hash makes it a password. The iteration count makes each
 * guess cost something; the real guard is that only members of the store can read the hashes.
 */
object PinHasher {
    const val PIN_LENGTH = 4
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16

    fun isValidPin(pin: String): Boolean = pin.length == PIN_LENGTH && pin.all { it.isDigit() }

    fun newSalt(): String =
        Base64.getEncoder().encodeToString(ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) })

    fun hash(pin: String, salt: String): String {
        val spec = PBEKeySpec(pin.toCharArray(), Base64.getDecoder().decode(salt), ITERATIONS, KEY_BITS)
        val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        spec.clearPassword()
        return Base64.getEncoder().encodeToString(key)
    }

    /** Constant-time comparison, so timing doesn't hint at how close a guess was. */
    fun matches(pin: String, salt: String, expectedHash: String): Boolean =
        MessageDigest.isEqual(hash(pin, salt).toByteArray(), expectedHash.toByteArray())
}
