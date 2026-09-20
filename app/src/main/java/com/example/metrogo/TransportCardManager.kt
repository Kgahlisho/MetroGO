package com.example.metrogo

import android.content.Context
import java.util.UUID

/** Replaces the wallet-balance portion of the old TicketManager. Every function takes
 *  the current user's real userId (a proper FK) instead of namespacing SharedPreferences
 *  keys with an email string by hand. */
object TransportCardManager {

    private const val PREFS_NAME = "metrogo_prefs"
    private const val KEY_CARD_PREFIX = "transport_card:" // transport_card:<userId> -> TransportCard json
    private const val DEFAULT_BALANCE = 0

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun getOrCreateCard(context: Context, userId: String): TransportCard {
        val existing = prefs(context).getString(KEY_CARD_PREFIX + userId, null)
        if (existing != null) {
            return try {
                TransportCard.fromJson(existing)
            } catch (e: Exception) {
                createCard(context, userId)
            }
        }
        return createCard(context, userId)
    }

    private fun createCard(context: Context, userId: String): TransportCard {
        val card = TransportCard(
            cardId = UUID.randomUUID().toString(),
            userId = userId,
            cardNumber = "MG-" + userId.take(8).uppercase(),
            balance = DEFAULT_BALANCE
        )
        saveCard(context, card)
        return card
    }

    private fun saveCard(context: Context, card: TransportCard) {
        prefs(context).edit().putString(KEY_CARD_PREFIX + card.userId, card.toJson()).apply()
    }

    fun getBalance(context: Context): Int {
        val userId = UserManager.getCurrentUserId(context) ?: return DEFAULT_BALANCE
        return getOrCreateCard(context, userId).balance
    }

    /** Returns true if the balance was sufficient and the deduction succeeded. */
    fun deduct(context: Context, amount: Int, description: String = "Ticket purchase", ticketId: String? = null): Boolean {
        val userId = UserManager.getCurrentUserId(context) ?: return false
        val card = getOrCreateCard(context, userId)
        if (card.balance < amount) return false

        val newBalance = card.balance - amount
        saveCard(context, card.copy(balance = newBalance, lastUpdated = System.currentTimeMillis()))
        PaymentStore.add(context, userId, PaymentStore.TYPE_PURCHASE, description, amount, ticketId)
        return true
    }

    fun topUp(context: Context, amount: Int) {
        val userId = UserManager.getCurrentUserId(context) ?: return
        val card = getOrCreateCard(context, userId)
        val newBalance = card.balance + amount
        saveCard(context, card.copy(balance = newBalance, lastUpdated = System.currentTimeMillis()))
        PaymentStore.add(context, userId, PaymentStore.TYPE_TOPUP, "Wallet top-up", amount, ticketId = null)
    }

    /** Wipes this specific user's card. Called when their account is deleted. */
    fun clearAllDataForUser(context: Context, userId: String) {
        prefs(context).edit().remove(KEY_CARD_PREFIX + userId).apply()
    }
}