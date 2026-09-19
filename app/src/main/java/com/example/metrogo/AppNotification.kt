package com.example.metrogo

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class AppNotification(

    val id : String,
    val title : String,
    val message : String,
    val timestamp: Long ,
    val type: String = TYPE_GENERAL ){

    companion object{
        const val TYPE_TICKET = "ticket"
        const val TYPE_GENERAL = "general"

        const val TYPE_WALLET = "wallet"
    }
}

object NotificationStore {

    private const val PREFS_NAME = "metrogo_prefs"
    private const val KEY_NOTIFICATIONS = "notifications_json"
    private const val MAX_STORED = 100

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun add(
        context: Context,
        title: String,
        message: String,
        type: String = AppNotification.TYPE_GENERAL
    ): AppNotification {
        val notification = AppNotification(
            id = UUID.randomUUID().toString(),
            title = title,
            message = message,
            timestamp = System.currentTimeMillis(),
            type = type
        )
        val updated = (listOf(notification) + getAll(context)).take(MAX_STORED)

        val array = JSONArray()
        for (n in updated) {
            array.put(
                JSONObject()
                    .put("id", n.id)
                    .put("title", n.title)
                    .put("message", n.message)
                    .put("timestamp", n.timestamp)
                    .put("type", n.type)
            )
        }
        prefs(context).edit().putString(KEY_NOTIFICATIONS, array.toString()).apply()
        return notification
    }

    fun getAll(context: Context): List<AppNotification> {
        val json = prefs(context).getString(KEY_NOTIFICATIONS, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<AppNotification>()
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                list.add(
                    AppNotification(
                        id = o.getString("id"),
                        title = o.getString("title"),
                        message = o.getString("message"),
                        timestamp = o.getLong("timestamp"),
                        type = o.optString("type", AppNotification.TYPE_GENERAL)
                    )
                )
            }
            list.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

