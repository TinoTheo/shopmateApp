package com.shopmate.scan

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A keyboard-wedge (HID) scanner: the device types the code and sends a terminator, exactly as
 * a USB or Bluetooth keyboard would. This is the common hardware at the till, so it is the
 * source that ships first.
 *
 * The screen feeds key events in through [onCharacter]; a terminator (CR or LF) emits the
 * buffered code. There is no permission to ask for and no overlay to show — the cashier just
 * scans and the code arrives.
 */
@Singleton
class HidScanSource @Inject constructor() : ScanSource {

    private val buffer = StringBuilder()

    private val _codes = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override val codes: Flow<String> = _codes.asSharedFlow()

    /** HID is always available — if no scanner is attached, no codes simply arrive. */
    override suspend fun isAvailable(): Boolean = true

    private var lastCharAt = 0L

    /**
     * Feeds one character from a hardware key event. Returns true when the character was
     * consumed as part of a scan, so the caller can keep it out of the search field.
     *
     * A wedge scanner emits its whole code in a few milliseconds, far faster than anyone types.
     * That gap is the only thing separating a scan from a person using a hardware keyboard, so
     * characters arriving more than [INTER_CHAR_GAP_MS] apart start a fresh buffer and are
     * **not** consumed — typing into the search field with a real keyboard still works.
     */
    fun onCharacter(char: Char, atMillis: Long = System.currentTimeMillis()): Boolean {
        val gap = atMillis - lastCharAt
        lastCharAt = atMillis

        if (char == '\n' || char == '\r') {
            val code = buffer.toString().trim()
            buffer.setLength(0)
            // Only a terminator that closes a fast-typed run is a scan.
            if (code.length >= MIN_CODE_LENGTH && gap <= INTER_CHAR_GAP_MS) {
                _codes.tryEmit(code)
                return true
            }
            return false
        }

        if (!char.isLetterOrDigit()) {
            buffer.setLength(0)
            return false
        }

        if (gap > INTER_CHAR_GAP_MS) buffer.setLength(0)
        buffer.append(char)
        // Never consumed mid-code: a human typing gets their characters, and a scanner's are
        // replaced by the cart line the terminator produces.
        return false
    }

    private companion object {
        /** A wedge scanner is well under this between characters; a person never is. */
        const val INTER_CHAR_GAP_MS = 60L

        /** Shorter than this and it is far more likely to be typing than a barcode. */
        const val MIN_CODE_LENGTH = 4
    }

    /** Drops a half-typed code — call when the scanning surface goes away. */
    fun reset() {
        buffer.setLength(0)
    }
}
