package com.example.metrogo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Replaces the ticket/XP portion of the old TicketManager. Same XP rules as before,
 *  just keyed by the real userId FK instead of an email string. */
object TicketStore {

    private const val PREFS_NAME = "metrogo_prefs"
    private const val KEY_ACTIVE_TICKET_PREFIX = "active_ticket:"   // active_ticket:<userId>
    private const val KEY_TICKET_HISTORY_PREFIX = "ticket_history:" // ticket_history:<userId>

    const val XP_MIN_TICKET_PRICE = 25
    const val XP_PER_TICKET = 5
    const val XP_PER_LEVEL = 100
    const val XP_HIGH_TICKET_PRICE = 30
    const val XP_HIGH_TICKET = 15
    const val XP_40_TICKET_PRICE = 40
    const val XP_40_TICKET = 25

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun newTicketId(): String = "MG-" + UUID.randomUUID().toString().take(8).uppercase()

    fun xpForPrice(price: Int): Int = when {
        price >= XP_40_TICKET_PRICE -> XP_40_TICKET
        price >= XP_HIGH_TICKET_PRICE -> XP_HIGH_TICKET
        price >= XP_MIN_TICKET_PRICE -> XP_PER_TICKET
        else -> 0
    }

    // ---------------- Active ticket ----------------

    fun saveActiveTicket(context: Context, ticket: Ticket) {
        prefs(context).edit().putString(KEY_ACTIVE_TICKET_PREFIX + ticket.userId, ticket.toJson()).apply()
    }

    fun getActiveTicket(context: Context): Ticket? {
        val userId = UserManager.getCurrentUserId(context) ?: return null
        val json = prefs(context).getString(KEY_ACTIVE_TICKET_PREFIX + userId, null) ?: return null
        return try {
            Ticket.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    fun clearActiveTicket(context: Context) {
        val userId = UserManager.getCurrentUserId(context) ?: return
        prefs(context).edit().remove(KEY_ACTIVE_TICKET_PREFIX + userId).apply()
    }

    // ---------------- Ticket history / XP ----------------

    /** Call once per successful purchase. Returns the XP that ticket earned. Also writes
     *  a matching TravelHistory row -- see TravelHistoryStore. */
    fun addToHistory(context: Context, ticket: Ticket): Int {
        val xp = xpForPrice(ticket.price)
        val array = readHistoryArray(context, ticket.userId)
        val entry = JSONObject()
        entry.put("ticket", JSONObject(ticket.toJson()))
        entry.put("xp", xp)
        array.put(entry)
        prefs(context).edit().putString(KEY_TICKET_HISTORY_PREFIX + ticket.userId, array.toString()).apply()
        return xp
    }

    /** Real purchased tickets, newest first. */
    fun getTripHistory(context: Context): List<Trip> {
        val userId = UserManager.getCurrentUserId(context) ?: return emptyList()
        val array = readHistoryArray(context, userId)
        val trips = mutableListOf<Trip>()
        for (i in 0 until array.length()) {
            try {
                val entry = array.getJSONObject(i)
                val ticket = Ticket.fromJson(entry.getJSONObject("ticket").toString())
                trips.add(Trip(ticket, entry.optInt("xp", xpForPrice(ticket.price))))
            } catch (e: Exception) {
                // skip a corrupt entry rather than crash the history screen
            }
        }
        return trips.sortedByDescending { it.ticket.purchaseDate }
    }

    data class XpProgress(val level: Int, val xpInLevel: Int, val totalXp: Int)

    fun getXpProgress(context: Context): XpProgress {
        val total = getTripHistory(context).sumOf { it.xpEarned }
        return XpProgress(
            level = total / XP_PER_LEVEL + 1,
            xpInLevel = total % XP_PER_LEVEL,
            totalXp = total
        )
    }

    private fun readHistoryArray(context: Context, userId: String): JSONArray {
        val json = prefs(context).getString(KEY_TICKET_HISTORY_PREFIX + userId, null) ?: return JSONArray()
        return try { JSONArray(json) } catch (e: Exception) { JSONArray() }
    }

    /** Wipes this specific user's active ticket + history. Called on account deletion. */
    fun clearAllDataForUser(context: Context, userId: String) {
        prefs(context).edit()
            .remove(KEY_ACTIVE_TICKET_PREFIX + userId)
            .remove(KEY_TICKET_HISTORY_PREFIX + userId)
            .apply()
    }

    // ---------------- QR generation ----------------

    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}