package com.example.metrogo

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.launch

object GoogleSignInHelper {

    private const val TAG = "GoogleSignIn"

    fun signIn(activity: AppCompatActivity, onResult: (UserManager.AuthResult) -> Unit) {
        val googleOption = GetSignInWithGoogleOption.Builder(
            activity.getString(R.string.default_web_client_id)
        ).build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleOption)
            .build()

        val credentialManager = CredentialManager.create(activity)

        activity.lifecycleScope.launch {
            try {
                val response = credentialManager.getCredential(activity, request)
                val credential = response.credential

                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val google = GoogleIdTokenCredential.createFrom(credential.data)
                    UserManager.signInWithGoogle(
                        context = activity,
                        idToken = google.idToken,
                        givenName = google.givenName,
                        familyName = google.familyName,
                        displayName = google.displayName,
                        onResult = onResult
                    )
                } else {
                    Log.e(TAG, "Unexpected credential type: ${credential.type}")
                    onResult(UserManager.AuthResult.Failure("Unexpected sign-in response. Please try again."))
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Sign-in cancelled", e)
                onResult(UserManager.AuthResult.Failure(""))
            } catch (e: NoCredentialException) {
                Log.e(TAG, "NoCredentialException", e)
                onResult(
                    UserManager.AuthResult.Failure(
                        "Google sign-in isn't available right now. Make sure a Google account is " +
                                "added on this device and that you're online, then try again."
                    )
                )
            } catch (e: GetCredentialException) {
                Log.e(TAG, "GetCredentialException type=${e.type}", e)
                onResult(UserManager.AuthResult.Failure(e.message ?: "Google sign-in failed. Please try again."))
            } catch (e: GoogleIdTokenParsingException) {
                Log.e(TAG, "Token parsing failed", e)
                onResult(UserManager.AuthResult.Failure("Google sign-in failed. Please try again."))
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected sign-in error", e)
                onResult(UserManager.AuthResult.Failure("Google sign-in failed. Please try again."))
            }
        }
    }
}