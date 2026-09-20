package com.example.metrogo

import org.json.JSONObject

/**
 * 3NF: userId is now a real surrogate primary key (a generated UUID), not the email
 * string. Email is still unique and used to look the account up at login time, but it's
 * no longer what other tables key off of -- this also means the migration path to
 * Firebase Auth is a straight swap later (Auth's UID takes over this same role).
 *
 * fullName has been split into firstName/surname -- a concatenated name is not an
 * atomic value and violates 1NF, which 3NF assumes as a baseline.
 */
data class UserAccount(
    val userId: String,
    val firstName: String,
    val surname: String,
    val email: String,
    val passwordHash: String,
    val mobile: String,
    val idNumber: String = "",
    val dob: String = "",
    val preferredLanguage: String = "English", // now per-user, not a device-wide setting
    val registeredAt: Long = System.currentTimeMillis()
) {
    val fullName: String get() = "$firstName $surname"

    fun toJson(): String {
        val json = JSONObject()
        json.put("userId", userId)
        json.put("firstName", firstName)
        json.put("surname", surname)
        json.put("email", email)
        json.put("passwordHash", passwordHash)
        json.put("mobile", mobile)
        json.put("idNumber", idNumber)
        json.put("dob", dob)
        json.put("preferredLanguage", preferredLanguage)
        json.put("registeredAt", registeredAt)
        return json.toString()
    }

    companion object {
        fun fromJson(jsonString: String): UserAccount {
            val json = JSONObject(jsonString)
            return UserAccount(
                userId = json.getString("userId"),
                firstName = json.getString("firstName"),
                surname = json.getString("surname"),
                email = json.getString("email"),
                passwordHash = json.getString("passwordHash"),
                mobile = json.getString("mobile"),
                idNumber = json.optString("idNumber", ""),
                dob = json.optString("dob", ""),
                preferredLanguage = json.optString("preferredLanguage", "English"),
                registeredAt = json.optLong("registeredAt", System.currentTimeMillis())
            )
        }
    }
}