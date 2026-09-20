package com.example.metrogo

import android.content.Context
import java.util.UUID


object TransportCardManager {

    private const val PREFS_NAME = "metrogo_prefs"
    private const val KEY_CARD_PREFIX = "transport_card:"
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

      fun clearAllDataForUser(context: Context, userId: String) {
        prefs(context).edit().remove(KEY_CARD_PREFIX + userId).apply()
    }
}