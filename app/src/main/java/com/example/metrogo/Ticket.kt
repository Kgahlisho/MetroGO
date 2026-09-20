package com.example.metrogo

import org.json.JSONObject


data class Ticket(
    val ticketId: String,
    val userId: String,      // FK -> UserAccount
    val scheduleId: String,  // FK -> Schedule
    val ticketType: String = "single",
    val purchaseDate: Long = System.currentTimeMillis(),
    val expiryDate: Long = System.currentTimeMillis() + 24 * 60 * 60 * 1000L, // valid 24h by default
    val price: Int,
    val status: String = STATUS_ACTIVE
) {
    companion object {
        const val STATUS_ACTIVE = "active"
        const val STATUS_USED = "used"
        const val STATUS_EXPIRED = "expired"
        const val STATUS_REFUNDED = "refunded"

        fun fromJson(jsonString: String): Ticket {
            val json = JSONObject(jsonString)
            return Ticket(
                ticketId = json.getString("ticketId"),
                userId = json.getString("userId"),
                scheduleId = json.getString("scheduleId"),
                ticketType = json.optString("ticketType", "single"),
                purchaseDate = json.optLong("purchaseDate", System.currentTimeMillis()),
                expiryDate = json.optLong("expiryDate", System.currentTimeMillis()),
                price = json.getInt("price"),
                status = json.optString("status", STATUS_ACTIVE)
            )
        }
    }

    fun toJson(): String {
        val json = JSONObject()
        json.put("ticketId", ticketId)
        json.put("userId", userId)
        json.put("scheduleId", scheduleId)
        json.put("ticketType", ticketType)
        json.put("purchaseDate", purchaseDate)
        json.put("expiryDate", expiryDate)
        json.put("price", price)
        json.put("status", status)
        return json.toString()
    }

       fun toQrPayload(passengerName: String, details: TransportRouteRepository.ScheduleDetails): String {
        val json = JSONObject()
        json.put("ticketId", ticketId)
        json.put("passenger", passengerName)
        json.put("transport", details.route.routeName)
        json.put("from", details.originStop.stopName)
        json.put("to", details.destinationStop.stopName)
        json.put("departs", details.schedule.departureTime)
        json.put("arrives", details.schedule.arrivalTime)
        json.put("registration", details.schedule.busRegistration)
        json.put("price", price)
        json.put("purchasedAt", purchaseDate)
        return json.toString()
    }
}