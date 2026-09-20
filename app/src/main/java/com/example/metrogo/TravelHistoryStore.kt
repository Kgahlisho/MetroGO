package com.example.metrogo

import android.content.Context
import org.json.JSONArray
import java.util.UUID

object TravelHistoryStore {

    private const val PREFS_NAME = "metrogo_prefs"
    private const val KEY_TRAVEL_HISTORY_PREFIX = "travel_history:" // travel_history:<userId>

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Call once per successful purchase, alongside TicketStore.addToHistory(). */
    fun add(context: Context, ticket: Ticket) {
        val entry = TravelHistoryEntry(
            travelId = UUID.randomUUID().toString(),
            userId = ticket.userId,
            ticketId = ticket.ticketId,
            travelDate = ticket.purchaseDate,
            fare = ticket.price
        )
        val array = readArray(context, ticket.userId)
        array.put(org.json.JSONObject(entry.toJson()))
        prefs(context).edit().putString(KEY_TRAVEL_HISTORY_PREFIX + ticket.userId, array.toString()).apply()
    }

    fun getAll(context: Context): List<TravelHistoryEntry> {
        val userId = UserManager.getCurrentUserId(context) ?: return emptyList()
        val array = readArray(context, userId)
        val list = mutableListOf<TravelHistoryEntry>()
        for (i in 0 until array.length()) {
            try {
                list.add(TravelHistoryEntry.fromJson(array.getJSONObject(i).toString()))
            } catch (e: Exception) {
                // skip a corrupt entry
            }
        }
        return list.sortedByDescending { it.travelDate }
    }

    private fun readArray(context: Context, userId: String): JSONArray {
        val json = prefs(context).getString(KEY_TRAVEL_HISTORY_PREFIX + userId, null) ?: return JSONArray()
        return try { JSONArray(json) } catch (e: Exception) { JSONArray() }
    }

    /** Wipes this specific user's travel history. Called when their account is deleted. */
    fun clearAllDataForUser(context: Context, userId: String) {
        prefs(context).edit().remove(KEY_TRAVEL_HISTORY_PREFIX + userId).apply()
    }
}