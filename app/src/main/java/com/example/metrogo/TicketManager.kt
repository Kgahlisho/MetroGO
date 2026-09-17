package com.example.metrogo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.util.UUID

object TicketManager {

    private const val PREFS_NAME = "metrogo_prefs"
    private const val KEY_ACTIVE_TICKET = "active_ticket_json"
    private const val KEY_WALLET_BALANCE = "wallet_balance"
    private const val DEFAULT_BALANCE = 1678

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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

    fun newTicketId(): String = "MG-" + UUID.randomUUID().toString().take(8).uppercase()

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


