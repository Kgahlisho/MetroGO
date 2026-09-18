package com.example.metrogo

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Trip(

    val ticket: Ticket,
    val xpEarned: Int,
    val isPaid: Boolean = true
) {
    val id: String get() = ticket.ticketId
    val route: String get() = "${ticket.origin} to ${ticket.destination}"
    val boardingTime: String get() = ticket.departureTime
    val date: String get() = formatDate(ticket.purchaseTimestamp)
    val cost: String get() = "R${ticket.price}"
    val paymentMethod: String get() = "Wallet"


    companion object {
        fun formatDate(timestamp: Long): String =
            SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(timestamp))

        fun formatDateTime(timestamp: Long): String =
            SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}

