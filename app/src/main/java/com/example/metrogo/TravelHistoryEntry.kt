package com.example.metrogo

import org.json.JSONObject

/**
 * A record of a trip actually taken, separate from the Ticket that paid for it -- these
 * happen to be created at the same moment today (purchase == the trip being logged), but
 * modeling them as distinct tables leaves room for e.g. a tap-in/tap-out flow later where
 * a ticket could exist without a completed trip yet.
 *
 * Note: your original diagram put origin/destination/routeId directly on this table too.
 * That's dropped here since it's a 3NF violation -- all of it is derivable via
 * ticketId -> Ticket.scheduleId -> Schedule.routeId -> TransportRoute/BusStop. If you
 * want it back purely as a read-optimization (skip the join when just listing history),
 * that's a valid reason to denormalize deliberately later -- just know that's what it'd be.
 */
data class TravelHistoryEntry(
    val travelId: String,
    val userId: String,   // FK -> UserAccount
    val ticketId: String, // FK -> Ticket
    val travelDate: Long,
    val fare: Int          // snapshot of what was paid, same reasoning as Ticket.price
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