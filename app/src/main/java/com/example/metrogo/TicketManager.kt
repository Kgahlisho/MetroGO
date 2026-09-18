package com.example.metrogo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Simple SharedPreferences-backed store for:
 *  - the passenger's wallet balance (deducted when a ticket is purchased)
 *  - the currently active ticket (shown as a QR code on the Dashboard)
 *
 * This is intentionally lightweight (no database) since MetroGO only ever needs
 * to track a single active ticket at a time. Swap for Room if you later want
 * a full purchase history stored locally instead of relying on the dummy
 * TravelHistory data.
 */
object TicketManager {

    private const val PREFS_NAME = "metrogo_prefs"
    private const val KEY_ACTIVE_TICKET = "active_ticket_json"
    private const val KEY_WALLET_BALANCE = "wallet_balance"
    private const val KEY_TICKET_HISTORY = "ticket_history_json"


    private const val DEFAULT_BALANCE = 100

    const val XP_MIN_TICKET_PRICE = 25
    const val XP_PER_TICKET = 5
    const val XP_PER_LEVEL = 100

    const val XP_HIGH_TICKET_PRICE = 30
    const val XP_HIGH_TICKET = 15

    const val XP_40_TICKET_PRICE = 40

    const val XP_40_TICKET=25



    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ---------------- Wallet balance ----------------

    fun getBalance(context: Context): Int =
        prefs(context).getInt(KEY_WALLET_BALANCE, DEFAULT_BALANCE)

    /** Returns true if the balance was sufficient and the deduction succeeded. */
    fun deduct(context: Context, amount: Int): Boolean {
        val current = getBalance(context)
        if (current < amount) return false
        prefs(context).edit().putInt(KEY_WALLET_BALANCE, current - amount).apply()
        return true
    }

    fun topUp(context: Context, amount: Int) {
        val current = getBalance(context)
        prefs(context).edit().putInt(KEY_WALLET_BALANCE, current + amount).apply()
    }

    // ---------------- Active ticket ----------------

    fun saveActiveTicket(context: Context, ticket: Ticket) {
        prefs(context).edit().putString(KEY_ACTIVE_TICKET, ticket.toJson()).apply()
    }

    fun getActiveTicket(context: Context): Ticket? {
        val json = prefs(context).getString(KEY_ACTIVE_TICKET, null) ?: return null
        return try {
            Ticket.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    fun clearActiveTicket(context: Context) {
        prefs(context).edit().remove(KEY_ACTIVE_TICKET).apply()
    }



    //fun xpForPrice(price: Int): Int = if (price > XP_MIN_TICKET_PRICE) XP_PER_TICKET else 0
    fun xpForPrice(price:Int ): Int = when {
        price >= XP_40_TICKET_PRICE -> XP_40_TICKET
        price >= XP_HIGH_TICKET_PRICE -> XP_HIGH_TICKET
        price >= XP_MIN_TICKET_PRICE -> XP_PER_TICKET
        else -> 0
    }
    /** Call once per successful purchase. Returns the XP that ticket earned. */
    fun addToHistory(context: Context, ticket: Ticket): Int {
        val xp = xpForPrice(ticket.price)
        val array = readHistoryArray(context)
        val entry = JSONObject()
        entry.put("ticket", JSONObject(ticket.toJson()))
        entry.put("xp", xp)
        array.put(entry)
        prefs(context).edit().putString(KEY_TICKET_HISTORY, array.toString()).apply()
        return xp
    }

    /** Real purchased tickets, newest first. */
    fun getTripHistory(context: Context): List<Trip> {
        val array = readHistoryArray(context)
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
        return trips.sortedByDescending { it.ticket.purchaseTimestamp }
    }

    data class XpProgress(val level: Int, val xpInLevel: Int, val totalXp: Int)

    /**
     * Level starts at 1. Every XP_PER_LEVEL XP promotes the user one level and the bar
     * starts again from 0 (e.g. 100 total XP = Level 2, 0/100).
     */
    fun getXpProgress(context: Context): XpProgress {
        val total = getTripHistory(context).sumOf { it.xpEarned }
        return XpProgress(
            level = total / XP_PER_LEVEL + 1,
            xpInLevel = total % XP_PER_LEVEL,
            totalXp = total
        )
    }

    private fun readHistoryArray(context: Context): JSONArray {
        val json = prefs(context).getString(KEY_TICKET_HISTORY, null) ?: return JSONArray()
        return try { JSONArray(json) } catch (e: Exception) { JSONArray() }
    }

    fun newTicketId(): String = "MG-" + UUID.randomUUID().toString().take(8).uppercase()


    // ---------------- QR generation ----------------

    /** Renders [content] as a black-on-white QR code bitmap, [sizePx] square. */
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