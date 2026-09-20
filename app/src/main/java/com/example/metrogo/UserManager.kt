package com.example.metrogo

import android.content.Context
import java.security.MessageDigest
import java.util.UUID

/**
 * MetroGO's local "user table". userId (a generated UUID) is the real primary key now;
 * email is a unique lookup index into it, not the key other tables reference. A small
 * "email -> userId" index is kept so login-by-email still works in O(1).
 */
object UserManager {

    private const val PREFS_NAME = "metrogo_prefs"
    private const val KEY_SESSION_USER_ID = "session_user_id"
    private const val USER_KEY_PREFIX = "user:"          // user:<userId> -> UserAccount json
    private const val EMAIL_INDEX_PREFIX = "email_index:" // email_index:<email> -> userId

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun normalize(email: String) = email.trim().lowercase()

    private fun hash(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(password.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    sealed class AuthResult {
        data class Success(val user: UserAccount) : AuthResult()
        data class Failure(val message: String) : AuthResult()
    }

    // ---------------- Registration ----------------

    fun register(
        context: Context,
        email: String,
        password: String,
        firstName: String,
        surname: String,
        mobile: String
    ): AuthResult {
        val normalizedEmail = normalize(email)

        if (normalizedEmail.isBlank() || !normalizedEmail.contains("@")) {
            return AuthResult.Failure("Please enter a valid email address.")
        }
        if (password.length < 6) {
            return AuthResult.Failure("Password must be at least 6 characters.")
        }
        if (firstName.isBlank() || surname.isBlank()) {
            return AuthResult.Failure("Please enter your first name and surname.")
        }
        if (findUserIdByEmail(context, normalizedEmail) != null) {
            return AuthResult.Failure("An account with this email already exists.")
        }

        val user = UserAccount(
            userId = UUID.randomUUID().toString(),
            firstName = firstName.trim(),
            surname = surname.trim(),
            email = normalizedEmail,
            passwordHash = hash(password),
            mobile = mobile.trim()
        )
        saveUser(context, user)
        prefs(context).edit().putString(EMAIL_INDEX_PREFIX + normalizedEmail, user.userId).apply()
        setSession(context, user.userId)
        return AuthResult.Success(user)
    }

    // ---------------- Login ----------------

    fun login(context: Context, email: String, password: String): AuthResult {
        val normalizedEmail = normalize(email)
        val userId = findUserIdByEmail(context, normalizedEmail)
            ?: return AuthResult.Failure("No account found with this email.")
        val user = getUserById(context, userId)
            ?: return AuthResult.Failure("No account found with this email.")

        if (user.passwordHash != hash(password)) {
            return AuthResult.Failure("Incorrect password.")
        }
        setSession(context, userId)
        return AuthResult.Success(user)
    }

    fun logout(context: Context) {
        prefs(context).edit().remove(KEY_SESSION_USER_ID).apply()
    }

    // ---------------- Session ----------------

    private fun setSession(context: Context, userId: String) {
        prefs(context).edit().putString(KEY_SESSION_USER_ID, userId).apply()
    }

    fun isLoggedIn(context: Context): Boolean = getCurrentUser(context) != null

    fun getCurrentUserId(context: Context): String? =
        prefs(context).getString(KEY_SESSION_USER_ID, null)

    fun getCurrentUser(context: Context): UserAccount? {
        val userId = getCurrentUserId(context) ?: return null
        return getUserById(context, userId)
    }

    // ---------------- Profile ----------------

    fun updateProfile(
        context: Context,
        firstName: String,
        surname: String,
        mobile: String,
        idNumber: String,
        dob: String,
        preferredLanguage: String? = null
    ): Boolean {
        val current = getCurrentUser(context) ?: return false
        val updated = current.copy(
            firstName = firstName.trim(),
            surname = surname.trim(),
            mobile = mobile.trim(),
            idNumber = idNumber.trim(),
            dob = dob.trim(),
            preferredLanguage = preferredLanguage ?: current.preferredLanguage
        )
        saveUser(context, updated)
        return true
    }

    fun deleteAccount(context: Context) {
        val user = getCurrentUser(context) ?: return
        prefs(context).edit()
            .remove(USER_KEY_PREFIX + user.userId)
            .remove(EMAIL_INDEX_PREFIX + user.email)
            .apply()
        TransportCardManager.clearAllDataForUser(context, user.userId)
        TicketStore.clearAllDataForUser(context, user.userId)
        PaymentStore.clearAllDataForUser(context, user.userId)
        NotificationStore.clearAllDataForUser(context, user.userId)
        TravelHistoryStore.clearAllDataForUser(context, user.userId)
        logout(context)
    }

    // ---------------- Storage ----------------

    private fun findUserIdByEmail(context: Context, email: String): String? =
        prefs(context).getString(EMAIL_INDEX_PREFIX + normalize(email), null)

    private fun getUserById(context: Context, userId: String): UserAccount? {
        val json = prefs(context).getString(USER_KEY_PREFIX + userId, null) ?: return null
        return try {
            UserAccount.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    private fun saveUser(context: Context, user: UserAccount) {
        prefs(context).edit().putString(USER_KEY_PREFIX + user.userId, user.toJson()).apply()
    }
}