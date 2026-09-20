package com.example.metrogo

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat

object NotificationHelper {

    private const val CHANNEL_ID = "ticket_purchases"

    fun notifyTicketPurchased(context: Context, ticket: Ticket, xpEarned: Int, newLevel: Int?) {
        val appContext = context.applicationContext

        // Route/time details no longer live on the ticket -- resolve them via the join.
        val details = TransportRouteRepository.scheduleDetails(ticket.scheduleId)
        val routeText = if (details != null) {
            "${details.originStop.stopName} \u2192 ${details.destinationStop.stopName}"
        } else {
            "your trip"
        }

        val title = "Ticket purchased"
        val message = buildString {
            append("Your ticket ${ticket.ticketId} for $routeText is ready. ")
            if (details != null) append("Departs ${details.schedule.departureTime} \u00b7 ")
            append("R${ticket.price} paid from your wallet.")
            if (xpEarned > 0) append(" +$xpEarned XP.")
            if (newLevel != null) append(" Level up! You are now Level $newLevel.")
        }

        NotificationStore.add(appContext, title, message, AppNotification.TYPE_TICKET)
        showSystemNotification(appContext, title, message, (ticket.purchaseDate and 0x7FFFFFFF).toInt())
    }

    fun notifyWalletTopUp(context: Context, amount: Int, newBalance: Int) {
        val appContext = context.applicationContext
        val fmt = java.text.DecimalFormat("#,##0.00")

        val title = "Wallet top-up successful"
        val message = "R${fmt.format(amount)} was added to your MetroGO wallet. " +
                "New balance: R${fmt.format(newBalance)}."

        NotificationStore.add(appContext, title, message, AppNotification.TYPE_WALLET)
        showSystemNotification(appContext, title, message, (System.currentTimeMillis() and 0x7FFFFFFF).toInt())
    }

    private fun showSystemNotification(context: Context, title: String, message: String, id: Int) {
        createChannel(context)

        val hasPermission = Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return // the in-app notification was already saved

        // Tapping opens the Notifications page, with the Dashboard behind it for the back button.
        val pendingIntent = TaskStackBuilder.create(context)
            .addNextIntent(Intent(context, Dashboard::class.java))
            .addNextIntent(Intent(context, NotificationPage::class.java))
            .getPendingIntent(id, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_bell)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            // permission was revoked between the check and the call; nothing else to do
        }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "MetroGO notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Ticket purchases and wallet top-ups"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}