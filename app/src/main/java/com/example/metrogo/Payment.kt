package com.example.metrogo

import org.json.JSONObject

/**
 * Replaces WalletTransaction. Adds the userId and ticketId foreign keys that were
 * missing before -- previously "which user does this belong to" was only implicit in a
 * SharedPreferences key, and "which ticket did this pay for" wasn't tracked at all.
 *
 * ticketId is nullable: a wallet top-up isn't a payment FOR anything, so it has no
 * ticket to reference. balanceAfter is a deliberate audit/ledger convenience (same
 * reasoning as a bank statement's running balance column) -- technically derivable by
 * summing prior transactions, but recomputing that on every read is wasteful and this
 * is standard practice in transaction-log tables.
 */
data class Payment(
    val paymentId: String,
    val userId: String,       // FK -> UserAccount
    val ticketId: String?,    // FK -> Ticket, null for top-ups
    val type: String,         // TYPE_TOPUP or TYPE_PURCHASE
    val description: String,
    val amount: Int,
    val balanceAfter: Int,
    val paymentMethod: String = "wallet",
    val paymentDate: Long = System.currentTimeMillis(),
    val paymentStatus: String = STATUS_COMPLETED
) {
    companion object {
        const val TYPE_TOPUP = "topup"
        const val TYPE_PURCHASE = "purchase"

        const val STATUS_COMPLETED = "completed"
        const val STATUS_FAILED = "failed"
    }
}