package com.shopmate.domain.model

import java.math.BigDecimal
import java.time.Instant

/**
 * One line of the Sync history: an upload and, for a sale, what it was. Rows are named by what
 * the person controls ("Sale · $3.40"), never by the mechanism ("Sale sync").
 */
data class SyncEntry(
    val event: SyncEvent,
    val sale: SaleSummary?,
) {
    /** Why the store refused it, when it did. Null for anything not refused. */
    val failureReason: SyncFailureReason?
        get() = if (event.failed) SyncFailureReason.from(event.errorMessage, event.eventType) else null

    data class SaleSummary(
        val receiptNumber: String,
        val total: BigDecimal,
        /** The paper receipt's number, when the sale was renumbered on upload. */
        val originalReceiptNumber: String?,
        val sentAt: Instant?,
    )
}

/**
 * Why the store refused an upload, in terms the cashier can act on. The sync worker records the
 * server's answer as "Supabase {status}: {message}"; this reads it back. The raw text is never
 * shown in the main UI — only under Details, for the owner or support.
 *
 * Not here on purpose: a 401 (the session was refused — the worker pauses sending and nothing is
 * marked failed) and 5xx / timeouts (not a failure — the worker keeps retrying).
 */
sealed interface SyncFailureReason {
    /** 409 on the store's receipt-number constraint: another till already used the number. */
    data object ReceiptNumberTaken : SyncFailureReason

    /** 403 / row-level security: this account may not make this kind of change. */
    data class NotAllowed(val kind: Kind) : SyncFailureReason

    /** Any other refusal. */
    data object Other : SyncFailureReason

    enum class Kind { SALE, STOCK, PRICE }

    companion object {
        fun from(errorMessage: String?, eventType: EventType): SyncFailureReason {
            val message = errorMessage.orEmpty()
            val status = Regex("""Supabase (\d{3})""").find(message)?.groupValues?.get(1)?.toIntOrNull()
            return when {
                status == 409 && "receipt_number" in message -> ReceiptNumberTaken
                status == 403 || "row-level security" in message -> NotAllowed(
                    // A product list carries stock and prices; stock is what cashiers change.
                    if (eventType == EventType.SALE) Kind.SALE else Kind.STOCK,
                )
                else -> Other
            }
        }
    }
}
