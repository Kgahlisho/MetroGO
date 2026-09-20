package com.example.metrogo

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class WalletTransaction(


    val id: String,
    val type: String,
    val description: String,
    val amount: Int,
    val balanceAfter: Int,
    val timestamp: Long

) {companion object {
    const val TYPE_TOPUP = "topup"
    const val TYPE_PURCHASE = "purchase"
}
}

object WalletTransactionStore {

    private const val PREFS_NAME = "metrogo_prefs"
    private const val KEY_TRANSACTIONS = "wallet_transactions_json"
    private const val MAX_STORED = 100

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun userKey(context: Context): String =
        UserManager.getCurrentUser(context)?.email ?: "guest"

    fun add(context: Context, type: String, description: String, amount: Int, balanceAfter: Int) {
        val transaction = WalletTransaction(
            id = UUID.randomUUID().toString(),
            type = type,
            description = description,
            amount = amount,
            balanceAfter = balanceAfter,
            timestamp = System.currentTimeMillis()
        )
        val updated = (listOf(transaction) + getAll(context)).take(MAX_STORED)

        val array = JSONArray()
        for (t in updated) {
            array.put(
                JSONObject()
                    .put("id", t.id)
                    .put("type", t.type)
                    .put("description", t.description)
                    .put("amount", t.amount)
                    .put("balanceAfter", t.balanceAfter)
                    .put("timestamp", t.timestamp)
            )
        }
        prefs(context).edit().putString("$KEY_TRANSACTIONS:${userKey(context)}", array.toString()).apply()
    }

    fun getAll(context: Context): List<WalletTransaction> {
        val json = prefs(context).getString("$KEY_TRANSACTIONS:${userKey(context)}", null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<WalletTransaction>()
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                list.add(
                    WalletTransaction(
                        id = o.getString("id"),
                        type = o.getString("type"),
                        description = o.getString("description"),
                        amount = o.getInt("amount"),
                        balanceAfter = o.getInt("balanceAfter"),
                        timestamp = o.getLong("timestamp")
                    )
                )
            }
            list.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Wipes this specific user's transaction history. Called when their account is deleted. */
    fun clearAllDataForUser(context: Context, email: String) {
        prefs(context).edit().remove("$KEY_TRANSACTIONS:$email").apply()
    }
}