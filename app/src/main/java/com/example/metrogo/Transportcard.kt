package com.example.metrogo

import org.json.JSONObject


data class TransportCard(
    val cardId: String,
    val userId: String, // FK -> UserAccount
    val cardNumber: String,
    val balance: Int,
    val status: String = "active",
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("cardId", cardId)
        json.put("userId", userId)
        json.put("cardNumber", cardNumber)
        json.put("balance", balance)
        json.put("status", status)
        json.put("lastUpdated", lastUpdated)
        return json.toString()
    }

    companion object {
        fun fromJson(jsonString: String): TransportCard {
            val json = JSONObject(jsonString)
            return TransportCard(
                cardId = json.getString("cardId"),
                userId = json.getString("userId"),
                cardNumber = json.getString("cardNumber"),
                balance = json.getInt("balance"),
                status = json.optString("status", "active"),
                lastUpdated = json.optLong("lastUpdated", System.currentTimeMillis())
            )
        }
    }
}