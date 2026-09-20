package com.example.metrogo

import org.json.JSONObject


data class UserAccount(
    val userId: String,
    val firstName: String,
    val surname: String,
    val email: String,
    val passwordHash: String,
    val mobile: String,
    val idNumber: String = "",
    val dob: String = "",
    val preferredLanguage: String = "English",
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