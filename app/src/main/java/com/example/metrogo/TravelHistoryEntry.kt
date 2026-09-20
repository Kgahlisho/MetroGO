package com.example.metrogo

import org.json.JSONObject


data class TravelHistoryEntry(
    val travelId: String,
    val userId: String,   // FK -> UserAccount
    val ticketId: String, // FK -> Ticket
    val travelDate: Long,
    val fare: Int
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("travelId", travelId)
        json.put("userId", userId)
        json.put("ticketId", ticketId)
        json.put("travelDate", travelDate)
        json.put("fare", fare)
        return json.toString()
    }

    companion object {
        fun fromJson(jsonString: String): TravelHistoryEntry {
            val json = JSONObject(jsonString)
            return TravelHistoryEntry(
                travelId = json.getString("travelId"),
                userId = json.getString("userId"),
                ticketId = json.getString("ticketId"),
                travelDate = json.getLong("travelDate"),
                fare = json.getInt("fare")
            )
        }
    }
}