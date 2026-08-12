package com.fitnessquest.rpg.data.auth

import android.app.Activity
import android.app.Application
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

data class AccountState(
    val signedIn: Boolean = false,
    val isAnonymous: Boolean = true,
    val uid: String? = null,
    val displayName: String? = null,
    val email: String? = null,
) {
    /** True when the player has a real (non-guest) account. */
    val hasAccount: Boolean get() = signedIn && !isAnonymous
}

/**
 * Firebase Authentication wrapper. The app is "anonymous-first": players get a
 * silent guest account on launch, and signing in with Google or email *links*
 * that guest account so no progress is ever lost behind a login wall.
 */
class AuthService(private val app: Application) {

    private val auth = FirebaseAuth.getInstance()

    private val _state = MutableStateFlow(currentState())
    val state: StateFlow<AccountState> = _state

    /**
     * Invoked right before an anonymous guest uid is abandoned because the Google/email
     * credential already belongs to another account. Use this to free username claims.
     */
    var beforeAbandonAnonymous: (suspend () -> Unit)? = null

    init {
        // AuthStateListener only fires on sign-in/sign-out. Linking a guest account
        // keeps the same user, so also listen for token changes, which do fire on link.
        auth.addAuthStateListener { _state.value = currentState() }
        auth.addIdTokenListener(FirebaseAuth.IdTokenListener { _state.value = currentState() })
    }

    /** Live Firebase uid (prefer this over [state] right after sign-in). */
    fun currentUid(): String? = auth.currentUser?.uid

    private fun currentState(): AccountState {
        val user = auth.currentUser
        // Trust the linked providers over the cached isAnonymous flag, which can be
        // stale right after linking a Google/email credential to a guest account.
        val hasRealProvider = user?.providerData?.any { it.providerId != "firebase" } ?: false
        return AccountState(
            signedIn = user != null,
            isAnonymous = (user?.isAnonymous ?: true) && !hasRealProvider,
            uid = user?.uid,
            displayName = user?.displayName
                ?: user?.providerData?.firstNotNullOfOrNull { it.displayName?.takeIf(String::isNotBlank) },
            email = user?.email
                ?: user?.providerData?.firstNotNullOfOrNull { it.email?.takeIf(String::isNotBlank) }
        )
    }

    /** Creates a guest account when needed. Returns false if Auth could not sign in. */
    suspend fun ensureSignedIn(): Boolean {
        if (auth.currentUser != null) return true
        return try {
            auth.signInAnonymously().await()
            auth.currentUser != null
        } catch (e: Exception) {
            android.util.Log.w("AuthService", "Anonymous sign-in failed", e)
            false
        }
    }

    /**
     * Ensures an ID token is available for Firestore. Prefer cached token ([forceRefresh]=false)
     * so claims don't hang when Auth's network refresh is slow.
     */
    suspend fun ensureIdToken(forceRefresh: Boolean = false): Boolean =
        idToken(forceRefresh) != null

    /** Returns a Firebase ID token for REST calls, or null if unavailable. */
    suspend fun idToken(forceRefresh: Boolean = false): String? {
        val user = auth.currentUser ?: return null
        return runCatching {
            user.getIdToken(forceRefresh).await().token
        }.getOrNull()
    }

    suspend fun signInWithGoogle(activity: Activity): Result<Unit> {
        val webClientId = webClientId()
            ?: return Result.failure(
                IllegalStateException(
                    "Google Sign-In isn't configured yet. Enable the Google provider " +
                        "in the Firebase console and refresh google-services.json."
                )
            )
        return try {
            android.util.Log.d(TAG, "Starting Google Sign-In with webClientId: $webClientId")
            val option = com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption.Builder(webClientId)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build()
            android.util.Log.d(TAG, "Requesting credential from CredentialManager...")
            val credential = CredentialManager.create(activity)
                .getCredential(activity, request)
                .credential
            android.util.Log.d(TAG, "Credential received, type: ${credential.type}")
            if ((credential is CustomCredential) &&
                (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
            ) {
                val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                android.util.Log.d(TAG, "Google ID Token received, signing into Firebase...")
                signInOrLink(GoogleAuthProvider.getCredential(idToken, null))
                android.util.Log.d(TAG, "Firebase sign-in complete!")
                Result.success(Unit)
            } else {
                android.util.Log.e(TAG, "Unexpected credential type: ${credential.type}")
                Result.failure(IllegalStateException("Unexpected credential type."))
            }
        } catch (e: GetCredentialCancellationException) {
            android.util.Log.w(TAG, "Google Sign-In canceled by user")
            Result.failure(IllegalStateException("Sign-in canceled."))
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Google Sign-In failed with exception", e)
            Result.failure(e)
        }
    }

    /** Creates an email/password account (linking the guest account if present). */
    suspend fun signUpWithEmail(email: String, password: String): Result<Unit> = try {
        val user = auth.currentUser
        if (user != null && user.isAnonymous) {
            signInOrLink(EmailAuthProvider.getCredential(email, password))
        } else {
            auth.createUserWithEmailAndPassword(email, password).await()
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun signInWithEmail(email: String, password: String): Result<Unit> = try {
        auth.signInWithEmailAndPassword(email, password).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun signOut() {
        auth.signOut()
        // Return to a fresh guest session so the game keeps working.
        ensureSignedIn()
    }

    /** True when the signed-in account uses email/password (may also have Google linked). */
    fun hasEmailPasswordProvider(): Boolean =
        auth.currentUser?.providerData?.any { it.providerId == EmailAuthProvider.PROVIDER_ID } == true

    /** True when the signed-in account uses Google. */
    fun hasGoogleProvider(): Boolean =
        auth.currentUser?.providerData?.any { it.providerId == GoogleAuthProvider.PROVIDER_ID } == true

    /**
     * Permanently deletes the Firebase Auth user. Callers must wipe Firestore / local
     * data first while still authenticated. Reauthenticates when Firebase requires it.
     *
     * @param activity needed to reauthenticate Google accounts
     * @param emailPassword required to reauthenticate email/password accounts
     */
    suspend fun deleteAccount(activity: Activity?, emailPassword: String?): Result<Unit> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("Not signed in."))
        if (user.isAnonymous && !state.value.hasAccount) {
            return Result.failure(IllegalStateException("No account to delete."))
        }
        return try {
            try {
                user.delete().await()
            } catch (_: FirebaseAuthRecentLoginRequiredException) {
                reauthenticateForDeletion(activity, emailPassword).getOrElse { return Result.failure(it) }
                auth.currentUser?.delete()?.await()
                    ?: return Result.failure(IllegalStateException("Could not delete account after reauthentication."))
            }
            // Start a clean guest session so the app keeps working.
            ensureSignedIn()
            _state.value = currentState()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun reauthenticateForDeletion(
        activity: Activity?,
        emailPassword: String?
    ): Result<Unit> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("Not signed in."))
        return try {
            when {
                hasGoogleProvider() && activity != null -> {
                    val webClientId = webClientId()
                        ?: return Result.failure(
                            IllegalStateException("Google Sign-In isn't configured.")
                        )
                    val option = com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption.Builder(webClientId)
                        .build()
                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(option)
                        .build()
                    val credential = CredentialManager.create(activity)
                        .getCredential(activity, request)
                        .credential
                    if ((credential is CustomCredential) &&
                        (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
                    ) {
                        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                        user.reauthenticate(GoogleAuthProvider.getCredential(idToken, null)).await()
                        Result.success(Unit)
                    } else {
                        Result.failure(IllegalStateException("Unexpected credential type."))
                    }
                }
                hasEmailPasswordProvider() -> {
                    val email = user.email
                        ?: return Result.failure(IllegalStateException("Account has no email."))
                    val password = emailPassword?.takeIf { it.isNotBlank() }
                        ?: return Result.failure(
                            IllegalStateException("Enter your password to delete this account.")
                        )
                    user.reauthenticate(EmailAuthProvider.getCredential(email, password)).await()
                    Result.success(Unit)
                }
                else -> Result.failure(
                    IllegalStateException("Sign in again, then retry account deletion.")
                )
            }
        } catch (e: GetCredentialCancellationException) {
            Result.failure(IllegalStateException("Deletion canceled."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Links the credential to the guest account, or signs in if it already belongs to someone. */
    private suspend fun signInOrLink(credential: com.google.firebase.auth.AuthCredential) {
        val user = auth.currentUser
        if (user != null && user.isAnonymous) {
            try {
                user.linkWithCredential(credential).await()
            } catch (e: FirebaseAuthUserCollisionException) {
                // This abandons the guest uid — free its username before switching.
                runCatching { beforeAbandonAnonymous?.invoke() }
                auth.signInWithCredential(credential).await()
            }
        } else {
            auth.signInWithCredential(credential).await()
        }
        // Refresh the local user so profile fields (email, name, providers) are current.
        runCatching { auth.currentUser?.reload()?.await() }
        _state.value = currentState()
    }

    /**
     * The OAuth web client ID generated by the google-services plugin. Looked up at
     * runtime so the app still compiles before the Google provider is enabled.
     */
    private fun webClientId(): String? {
        val resId = app.resources.getIdentifier("default_web_client_id", "string", app.packageName)
        if (resId != 0) {
            val id = app.getString(resId)
            if (id.isNotBlank()) return id
        }
        return "652275814212-d2rtrkopn90mb8msh31rlopn5hbu41jb.apps.googleusercontent.com"
    }

    companion object {
        private const val TAG = "AuthService"
    }
}
