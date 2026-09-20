package com.example.metrogo

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** notificationId PK, userId FK now present (was previously implicit in a SharedPreferences
 *  key), and isRead added -- the app had no read/unread concept at all before this. */
data class AppNotification(
    val id: String,
    val userId: String, // FK -> UserAccount
    val title: String,
    val message: String,
    val timestamp: Long,
    val type: String = TYPE_GENERAL,
    val isRead: Boolean = false
) {
    companion object {
        const val TYPE_TICKET = "ticket"
        const val TYPE_GENERAL = "general"
        const val TYPE_WALLET = "wallet"
    }
}

object NotificationStore {

    private const val PREFS_NAME = "metrogo_prefs"
    private const val KEY_NOTIFICATIONS_PREFIX = "notifications:" // notifications:<userId>
    private const val MAX_STORED = 100

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun add(
        context: Context,
        title: String,
        message: String,
        type: String = AppNotification.TYPE_GENERAL
    ): AppNotification? {
        val userId = UserManager.getCurrentUserId(context) ?: return null
        val notification = AppNotification(
            id = UUID.randomUUID().toString(),
            userId = userId,
            title = title,
            message = message,
            timestamp = System.currentTimeMillis(),
            type = type,
            isRead = false
        )
        val updated = (listOf(notification) + getAll(context)).take(MAX_STORED)
        saveAll(context, userId, updated)
        return notification
    }

    fun getAll(context: Context): List<AppNotification> {
        val userId = UserManager.getCurrentUserId(context) ?: return emptyList()
        val json = prefs(context).getString(KEY_NOTIFICATIONS_PREFIX + userId, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<AppNotification>()
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                list.add(
                    AppNotification(
                        id = o.getString("id"),
                        userId = o.getString("userId"),
                        title = o.getString("title"),
                        message = o.getString("message"),
                        timestamp = o.getLong("timestamp"),
                        type = o.optString("type", AppNotification.TYPE_GENERAL),
                        isRead = o.optBoolean("isRead", false)
                    )
                )
            }
            list.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun unreadCount(context: Context): Int = getAll(context).count { !it.isRead }

    fun markAsRead(context: Context, notificationId: String) {
        val userId = UserManager.getCurrentUserId(context) ?: return
        val updated = getAll(context).map {
            if (it.id == notificationId) it.copy(isRead = true) else it
        }
        saveAll(context, userId, updated)
    }

    fun markAllAsRead(context: Context) {
        val userId = UserManager.getCurrentUserId(context) ?: return
        val updated = getAll(context).map { it.copy(isRead = true) }
        saveAll(context, userId, updated)
    }

    private fun saveAll(context: Context, userId: String, notifications: List<AppNotification>) {
        val array = JSONArray()
        for (n in notifications) {
            array.put(
                JSONObject()
                    .put("id", n.id)
                    .put("userId", n.userId)
                    .put("title", n.title)
                    .put("message", n.message)
                    .put("timestamp", n.timestamp)
                    .put("type", n.type)
                    .put("isRead", n.isRead)
            )
        }
        prefs(context).edit().putString(KEY_NOTIFICATIONS_PREFIX + userId, array.toString()).apply()
    }

    /** Wipes this specific user's notifications. Called when their account is deleted. */
    fun clearAllDataForUser(context: Context, userId: String) {
        prefs(context).edit().remove(KEY_NOTIFICATIONS_PREFIX + userId).apply()
    }
}