package com.example.metrogo

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


data class Trip(
    val ticket: Ticket,
    val xpEarned: Int,
    val isPaid: Boolean = true
) {
    private val details = TransportRouteRepository.scheduleDetails(ticket.scheduleId)

    val id: String get() = ticket.ticketId
    val route: String get() =
        if (details != null) "${details.originStop.stopName} to ${details.destinationStop.stopName}" else "Unknown route"
    val boardingTime: String get() = details?.schedule?.departureTime ?: "--"
    val date: String get() = formatDate(ticket.purchaseDate)
    val cost: String get() = "R${ticket.price}"
    val paymentMethod: String get() = "Wallet"

    companion object {
        fun formatDate(timestamp: Long): String =
            SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(timestamp))

        fun formatDateTime(timestamp: Long): String =
            SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}