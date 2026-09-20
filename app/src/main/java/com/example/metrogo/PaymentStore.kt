package com.example.metrogo

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object PaymentStore {

    private const val PREFS_NAME = "metrogo_prefs"
    private const val KEY_PAYMENTS_PREFIX = "payments:" // payments:<userId>
    private const val MAX_STORED = 100
    const val TYPE_TOPUP = Payment.TYPE_TOPUP
    const val TYPE_PURCHASE = Payment.TYPE_PURCHASE

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun add(
        context: Context,
        userId: String,
        type: String,
        description: String,
        amount: Int,
        ticketId: String?
    ) {
        val balanceAfter = TransportCardManager.getBalance(context)
        val payment = Payment(
            paymentId = UUID.randomUUID().toString(),
            userId = userId,
            ticketId = ticketId,
            type = type,
            description = description,
            amount = amount,
            balanceAfter = balanceAfter
        )
        val updated = (listOf(payment) + getAll(context, userId)).take(MAX_STORED)

        val array = JSONArray()
        for (p in updated) {
            array.put(
                JSONObject()
                    .put("paymentId", p.paymentId)
                    .put("userId", p.userId)
                    .put("ticketId", p.ticketId ?: JSONObject.NULL)
                    .put("type", p.type)
                    .put("description", p.description)
                    .put("amount", p.amount)
                    .put("balanceAfter", p.balanceAfter)
                    .put("paymentMethod", p.paymentMethod)
                    .put("paymentDate", p.paymentDate)
                    .put("paymentStatus", p.paymentStatus)
            )
        }
        prefs(context).edit().putString(KEY_PAYMENTS_PREFIX + userId, array.toString()).apply()
    }

    fun getAll(context: Context, userId: String): List<Payment> {
        val json = prefs(context).getString(KEY_PAYMENTS_PREFIX + userId, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<Payment>()
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                list.add(
                    Payment(
                        paymentId = o.getString("paymentId"),
                        userId = o.getString("userId"),
                        ticketId = if (o.isNull("ticketId")) null else o.getString("ticketId"),
                        type = o.getString("type"),
                        description = o.getString("description"),
                        amount = o.getInt("amount"),
                        balanceAfter = o.getInt("balanceAfter"),
                        paymentMethod = o.optString("paymentMethod", "wallet"),
                        paymentDate = o.getLong("paymentDate"),
                        paymentStatus = o.optString("paymentStatus", Payment.STATUS_COMPLETED)
                    )
                )
            }
            list.sortedByDescending { it.paymentDate }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getAll(context: Context): List<Payment> {
        val userId = UserManager.getCurrentUserId(context) ?: return emptyList()
        return getAll(context, userId)
    }

     fun clearAllDataForUser(context: Context, userId: String) {
        prefs(context).edit().remove(KEY_PAYMENTS_PREFIX + userId).apply()
    }
}