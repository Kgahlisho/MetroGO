package com.example.metrogo

import org.json.JSONObject

data class UserAccount(
    val userId: String,          // Firebase Auth UID
    val firstName: String,
    val surname: String,
    val email: String,
    val mobile: String,
    val idNumber: String = "",
    val dob: String = "",
    val preferredLanguage: String = "English",
    val registeredAt: Long = System.currentTimeMillis()
) {
    val fullName: String get() = "$firstName $surname".trim()

    /** Local cache format (SharedPreferences). */
    fun toJson(): String {
        val json = JSONObject()
        json.put("userId", userId)
        json.put("firstName", firstName)
        json.put("surname", surname)
        json.put("email", email)
        json.put("mobile", mobile)
        json.put("idNumber", idNumber)
        json.put("dob", dob)
        json.put("preferredLanguage", preferredLanguage)
        json.put("registeredAt", registeredAt)
        return json.toString()
    }

    /** Firestore document format (users/{uid}). The UID is the document ID, so it isn't stored as a field. */
    fun toMap(): Map<String, Any> = mapOf(
        "firstName" to firstName,
        "surname" to surname,
        "email" to email,
        "mobile" to mobile,
        "idNumber" to idNumber,
        "dob" to dob,
        "preferredLanguage" to preferredLanguage,
        "registeredAt" to registeredAt
    )

    companion object {
        fun fromJson(jsonString: String): UserAccount {
            val json = JSONObject(jsonString)
            return UserAccount(
                userId = json.getString("userId"),
                firstName = json.getString("firstName"),
                surname = json.getString("surname"),
                email = json.getString("email"),
                mobile = json.getString("mobile"),
                idNumber = json.optString("idNumber", ""),
                dob = json.optString("dob", ""),
                preferredLanguage = json.optString("preferredLanguage", "English"),
                registeredAt = json.optLong("registeredAt", System.currentTimeMillis())
            )
        }

        fun fromMap(userId: String, data: Map<String, Any?>): UserAccount = UserAccount(
            userId = userId,
            firstName = data["firstName"] as? String ?: "",
            surname = data["surname"] as? String ?: "",
            email = data["email"] as? String ?: "",
            mobile = data["mobile"] as? String ?: "",
            idNumber = data["idNumber"] as? String ?: "",
            dob = data["dob"] as? String ?: "",
            preferredLanguage = data["preferredLanguage"] as? String ?: "English",
            registeredAt = (data["registeredAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }
}