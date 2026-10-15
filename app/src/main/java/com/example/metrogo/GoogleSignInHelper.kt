package com.example.metrogo


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

object GoogleSignInHelper {fun signIn(activity: AppCompatActivity, onResult: (UserManager.AuthResult) -> Unit) {
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
                onResult(UserManager.AuthResult.Failure("Unexpected sign-in response. Please try again."))
            }
        } catch (e: GetCredentialCancellationException) {
            onResult(UserManager.AuthResult.Failure(""))   // user backed out, no error toast
        } catch (e: NoCredentialException) {
            onResult(
                UserManager.AuthResult.Failure(
                    "No Google account found on this device. Add one in Settings and try again."
                )
            )
        } catch (e: GetCredentialException) {
            onResult(UserManager.AuthResult.Failure(e.message ?: "Google sign-in failed. Please try again."))
        } catch (e: GoogleIdTokenParsingException) {
            onResult(UserManager.AuthResult.Failure("Google sign-in failed. Please try again."))
        }
    }
}
}