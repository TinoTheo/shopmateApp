package com.shopmate.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A short code that tells this till's receipts apart from every other till's — `K7Q` in
 * `SM-20261022-K7Q-0001`. The server holds receipt numbers unique per store, and each till counts
 * its own receipts from 0001 every day, so without it two tills (or one till reinstalled) issue
 * the same number and the server refuses the second sale.
 *
 * Generated once per install and kept in DataStore. A reinstall gets a new code, which is what
 * keeps its fresh 0001 from colliding with the old install's. Three characters from an alphabet
 * with no 0/O or 1/I, so it reads back unambiguously off a printed slip: 32,768 codes, ample for
 * the tills of one store.
 */
@Singleton
class TillCode @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    suspend fun get(): String {
        dataStore.data.first()[TILL_CODE_KEY]?.let { return it }
        val code = buildString { repeat(LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) } }
        var stored = code
        dataStore.edit { prefs ->
            // Another caller may have won the race; the first code written is the till's code.
            stored = prefs[TILL_CODE_KEY] ?: code.also { prefs[TILL_CODE_KEY] = it }
        }
        return stored
    }

    companion object {
        private val TILL_CODE_KEY = stringPreferencesKey("till_code")
        private const val ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        private const val LENGTH = 3
        private val random = SecureRandom()
    }
}
