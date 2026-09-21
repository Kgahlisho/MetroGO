package com.example.metrogo

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/**
 * Accounts are handled by Firebase Auth; the profile lives in Firestore at users/{uid}
 * and is mirrored in a small local cache so getCurrentUser() can stay synchronous.
 *
 * register/login/deleteAccount are asynchronous and report back through a callback
 * (always invoked on the main thread).
 */
object UserManager {

    private const val TAG = "UserManager"
    private const val PREFS_NAME = "metrogo_prefs"
    private const val PROFILE_CACHE_PREFIX = "profile_cache:" // profile_cache:<uid>
    private const val USERS_COLLECTION = "users"

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val users get() = FirebaseFirestore.getInstance().collection(USERS_COLLECTION)

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun normalize(email: String) = email.trim().lowercase()

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
        mobile: String,
        onResult: (AuthResult) -> Unit
    ) {
        val normalizedEmail = normalize(email)

        if (normalizedEmail.isBlank() || !normalizedEmail.contains("@")) {
            onResult(AuthResult.Failure("Please enter a valid email address."))
            return
        }
        if (password.length < 6) {
            onResult(AuthResult.Failure("Password must be at least 6 characters."))
            return
        }
        if (firstName.isBlank() || surname.isBlank()) {
            onResult(AuthResult.Failure("Please enter your first name and surname."))
            return
        }

        val app = context.applicationContext
        auth.createUserWithEmailAndPassword(normalizedEmail, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid == null) {
                    onResult(AuthResult.Failure("Sign-up failed. Please try again."))
                    return@addOnSuccessListener
                }
                val user = UserAccount(
                    userId = uid,
                    firstName = firstName.trim(),
                    surname = surname.trim(),
                    email = normalizedEmail,
                    mobile = mobile.trim()
                )
                cacheUser(app, user)
                users.document(uid).set(user.toMap()).addOnCompleteListener { task ->
                    // The account exists either way. If the profile write failed,
                    // syncProfile() will push the cached copy up next time.
                    if (!task.isSuccessful) Log.w(TAG, "Profile write failed", task.exception)
                    onResult(AuthResult.Success(user))
                }
            }
            .addOnFailureListener { e -> onResult(AuthResult.Failure(describe(e))) }
    }

    // ---------------- Login ----------------

    fun login(context: Context, email: String, password: String, onResult: (AuthResult) -> Unit) {
        val app = context.applicationContext
        auth.signInWithEmailAndPassword(normalize(email), password)
            .addOnSuccessListener { result ->
                val fbUser = result.user
                if (fbUser == null) {
                    onResult(AuthResult.Failure("Login failed. Please try again."))
                    return@addOnSuccessListener
                }
                val uid = fbUser.uid
                users.document(uid).get().addOnCompleteListener { task ->
                    val snapshot = if (task.isSuccessful) task.result else null
                    val user = when {
                        snapshot != null && snapshot.exists() ->
                            UserAccount.fromMap(uid, snapshot.data ?: emptyMap())
                        else -> {
                            val fallback = fallbackUser(uid, fbUser.email)
                            // Only create the profile if we positively know it's missing,
                            // never because the read failed (that would overwrite real data).
                            if (snapshot != null && !snapshot.exists()) pushProfile(fallback)
                            fallback
                        }
                    }
                    cacheUser(app, user)
                    onResult(AuthResult.Success(user))
                }
            }
            .addOnFailureListener { e -> onResult(AuthResult.Failure(describe(e))) }
    }

    fun logout(context: Context) {
        val uid = auth.currentUser?.uid
        auth.signOut()
        if (uid != null) prefs(context).edit().remove(PROFILE_CACHE_PREFIX + uid).apply()
    }

    // ---------------- Session ----------------

    fun isLoggedIn(context: Context): Boolean = auth.currentUser != null

    fun getCurrentUserId(context: Context): String? = auth.currentUser?.uid

    fun getCurrentUser(context: Context): UserAccount? {
        val fbUser = auth.currentUser ?: return null
        val json = prefs(context).getString(PROFILE_CACHE_PREFIX + fbUser.uid, null)
        if (json != null) {
            try {
                return UserAccount.fromJson(json)
            } catch (e: Exception) {
                // fall through to the fallback
            }
        }
        return fallbackUser(fbUser.uid, fbUser.email)
    }

    /**
     * Refreshes the local cache from Firestore (call when the app starts with an existing session).
     * If the profile doesn't exist remotely yet, the cached copy is pushed up instead.
     */
    fun syncProfile(context: Context, onDone: (() -> Unit)? = null) {
        val fbUser = auth.currentUser
        if (fbUser == null) {
            onDone?.invoke()
            return
        }
        val app = context.applicationContext
        users.document(fbUser.uid).get().addOnCompleteListener { task ->
            val snapshot = if (task.isSuccessful) task.result else null
            if (snapshot != null && snapshot.exists()) {
                cacheUser(app, UserAccount.fromMap(fbUser.uid, snapshot.data ?: emptyMap()))
            } else if (snapshot != null) {
                getCurrentUser(app)?.let { pushProfile(it) }
            }
            onDone?.invoke()
        }
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
        cacheUser(context, updated)   // UI sees the change immediately
        pushProfile(updated)          // Firestore queues this offline and syncs when connected
        return true
    }

    /**
     * Deletes the Firestore profile, then the Firebase Auth account, then local data.
     * Firebase requires a recent login to delete an account; if it refuses, the profile
     * is restored and the callback explains what to do.
     */
    fun deleteAccount(context: Context, onResult: (success: Boolean, message: String?) -> Unit) {
        val fbUser = auth.currentUser
        if (fbUser == null) {
            onResult(false, "You're not logged in.")
            return
        }
        val app = context.applicationContext
        val uid = fbUser.uid
        val backup = getCurrentUser(app)

        // Delete the profile first: once the Auth user is gone, Firestore rules would reject the request.
        users.document(uid).delete().addOnCompleteListener { deleteTask ->
            if (!deleteTask.isSuccessful) {
                onResult(false, describe(deleteTask.exception))
                return@addOnCompleteListener
            }
            fbUser.delete().addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    TransportCardManager.clearAllDataForUser(app, uid)
                    TicketStore.clearAllDataForUser(app, uid)
                    PaymentStore.clearAllDataForUser(app, uid)
                    NotificationStore.clearAllDataForUser(app, uid)
                    TravelHistoryStore.clearAllDataForUser(app, uid)
                    prefs(app).edit().remove(PROFILE_CACHE_PREFIX + uid).apply()
                    auth.signOut()
                    onResult(true, null)
                } else {
                    backup?.let { pushProfile(it) }   // undo the profile delete
                    val message = if (authTask.exception is FirebaseAuthRecentLoginRequiredException) {
                        "For security, please log out, log back in, then delete your account again."
                    } else {
                        describe(authTask.exception)
                    }
                    onResult(false, message)
                }
            }
        }
    }

    // ---------------- Internals ----------------

    private fun cacheUser(context: Context, user: UserAccount) {
        prefs(context).edit().putString(PROFILE_CACHE_PREFIX + user.userId, user.toJson()).apply()
    }

    private fun pushProfile(user: UserAccount) {
        users.document(user.userId).set(user.toMap(), SetOptions.merge())
            .addOnFailureListener { Log.w(TAG, "Profile write failed", it) }
    }

    private fun fallbackUser(uid: String, email: String?): UserAccount {
        val safeEmail = email ?: ""
        return UserAccount(
            userId = uid,
            firstName = safeEmail.substringBefore("@"),
            surname = "",
            email = safeEmail,
            mobile = ""
        )
    }

    private fun describe(e: Exception?): String = when (e) {
        is FirebaseAuthUserCollisionException -> "An account with this email already exists."
        is FirebaseAuthWeakPasswordException -> "Password is too weak. Use at least 6 characters."
        is FirebaseAuthInvalidUserException,
        is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
        is FirebaseNetworkException -> "No internet connection. Please try again."
        else -> e?.localizedMessage ?: "Something went wrong. Please try again."
    }
}