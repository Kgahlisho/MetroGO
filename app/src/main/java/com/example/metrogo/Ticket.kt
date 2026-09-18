package com.example.metrogo

import org.json.JSONObject
import java.io.Serializable

/**
 * A purchased ticket. This is what gets encoded into the QR code shown on the Dashboard,
 * and persisted so it survives app restarts until it's used/expired.
 */
data class Ticket(
    val ticketId: String,
    val passengerName: String,
    val transportName: String,
    val origin: String,
    val destination: String,
    val departureTime: String,
    val arrivalTime: String,
    val registration: String,
    val price: Int,
    val purchaseTimestamp: Long
) : Serializable {

    /** The exact string that gets encoded into the QR code. Keep this compact. */
    fun toQrPayload(): String {
        val json = JSONObject()
        json.put("ticketId", ticketId)
        json.put("passenger", passengerName)
        json.put("transport", transportName)
        json.put("from", origin)
        json.put("to", destination)
        json.put("departs", departureTime)
        json.put("arrives", arrivalTime)
        json.put("registration", registration)
        json.put("price", price)
        json.put("purchasedAt", purchaseTimestamp)
        return json.toString()
    }

    fun toJson(): String = toQrPayload()

    companion object {
        fun fromJson(jsonString: String): Ticket {
            val json = JSONObject(jsonString)
            return Ticket(
                ticketId = json.getString("ticketId"),
                passengerName = json.getString("passenger"),
                transportName = json.getString("transport"),
                origin = json.getString("from"),
                destination = json.getString("to"),
                departureTime = json.getString("departs"),
                arrivalTime = json.getString("arrives"),
                registration = json.getString("registration"),
                price = json.getInt("price"),
                purchaseTimestamp = json.getLong("purchasedAt")
            )
        }
    }
}