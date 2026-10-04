package com.wefit.app.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

class GoogleAuthRepository(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)

    // Replace with YOUR Web application Client ID from Cloud Console Step 1
    private val webClientId = "1067037192724-jib3aq64smslg3ms3icdl1qnuod0um93.apps.googleusercontent.com"

    suspend fun getGoogleIdToken(): Result<String> {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response: GetCredentialResponse = credentialManager.getCredential(context, request)
            val credential = GoogleIdTokenCredential.createFrom(response.credential.data)
            Result.success(credential.idToken)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}