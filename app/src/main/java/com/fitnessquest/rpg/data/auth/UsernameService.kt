package com.fitnessquest.rpg.data.auth

import android.util.Log
import com.fitnessquest.rpg.data.UserPrefs
import com.fitnessquest.rpg.data.db.AppDatabase
import com.fitnessquest.rpg.data.db.CharacterEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

/**
 * Global unique usernames for leaderboard / allies identity.
 *
 * - `usernames/{normalized}` → { uid, displayName }
 * - `usernamePointers/{normalized}` → { uid, displayName } (public “still held” marker)
 * - `users/{uid}.usernameKey` → reverse pointer for this account
 *
 * Claims without a public pointer are treated as abandoned and can be reclaimed.
 *
 * Primary write path is Firestore REST (HTTPS). The Android Firestore gRPC SDK has been
 * hanging / returning UNAVAILABLE (Channel shutdownNow) on some devices; REST is reliable
 * when Auth can mint an ID token.
 */
class UsernameService(
    private val db: AppDatabase,
    private val auth: AuthService,
    private val prefs: UserPrefs
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val http = OkHttpClient.Builder()
        .callTimeout(20, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    data class Validation(
        val ok: Boolean,
        val message: String? = null
    )

    fun validate(raw: String): Validation {
        val trimmed = raw.trim()
        if (trimmed.length < MIN_LEN) {
            return Validation(ok = false, message = "At least $MIN_LEN characters")
        }
        if (trimmed.length > MAX_LEN) {
            return Validation(ok = false, message = "At most $MAX_LEN characters")
        }
        if (!DISPLAY_PATTERN.matches(trimmed)) {
            return Validation(
                ok = false,
                message = "Letters, numbers, and underscores only — must start with a letter"
            )
        }
        val key = normalize(trimmed)
        if (key in RESERVED) {
            return Validation(ok = false, message = "That name is reserved")
        }
        return Validation(ok = true)
    }

    fun normalize(display: String): String = display.trim().lowercase()

    /**
     * Claims [display] for the current Firebase uid, updates the local hero name,
     * and releases any previous claim this account held.
     */
    suspend fun claim(displayRaw: String): Result<String> {
        val validation = validate(displayRaw)
        if (!validation.ok) {
            return Result.failure(IllegalArgumentException(validation.message ?: "Invalid username"))
        }
        val display = displayRaw.trim()
        val key = normalize(display)

        val uid = resolveUidForClaim()
            ?: return Result.failure(
                IllegalStateException("Sign in required to claim a username. Check your connection.")
            )
        val token = try {
            withTimeout(AUTH_TIMEOUT_MS.milliseconds) {
                auth.idToken(forceRefresh = false)
                    ?: auth.idToken(forceRefresh = true)
            }
        } catch (_: TimeoutCancellationException) {
            null
        } ?: return Result.failure(
            IllegalStateException("Sign in required to claim a username. Check your connection.")
        )

        Log.i(TAG, "claim start key=$key uid=$uid")

        return try {
            withTimeout(CLAIM_TIMEOUT_MS.milliseconds) {
                claimViaRest(uid = uid, key = key, display = display, token = token)
                releasePreviousKeysBestEffort(uid = uid, keepKey = key, token = token)

                val character = db.characterDao().get() ?: CharacterEntity()
                db.characterDao().upsert(character.copy(name = display))
                prefs.setUsernameClaim(key)
                Log.i(TAG, "claim success key=$key")
                Result.success(display)
            }
        } catch (_: TimeoutCancellationException) {
            Log.w(TAG, "claim timed out key=$key")
            Result.failure(
                IllegalStateException("Username claim timed out. Check your connection and try again.")
            )
        } catch (e: UsernameTakenException) {
            Result.failure(e)
        } catch (e: Exception) {
            Log.w(TAG, "Username claim failed for $key", e)
            Result.failure(mapClaimError(e))
        }
    }

    /**
     * Prefer an existing local Firebase user. Only hit the network for anonymous
     * sign-in when needed — never force a token refresh on the claim path first.
     */
    private suspend fun resolveUidForClaim(): String? {
        auth.currentUid()?.let { return it }
        return try {
            withTimeout(AUTH_TIMEOUT_MS.milliseconds) {
                if (!auth.ensureSignedIn()) return@withTimeout null
                auth.currentUid()
            }
        } catch (_: TimeoutCancellationException) {
            Log.w(TAG, "Auth timed out before username claim")
            auth.currentUid()
        } catch (e: Exception) {
            Log.w(TAG, "Auth before username claim failed", e)
            auth.currentUid()
        }
    }

    private suspend fun claimViaRest(
        uid: String,
        key: String,
        display: String,
        token: String
    ) = withContext(Dispatchers.IO) {
        val projectId = FirebaseApp.getInstance().options.projectId
            ?: throw IllegalStateException("Firebase project id missing")
        val base =
            "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents"
        val payload = firestoreDocumentBody(uid, display)

        val pointerOwner = restGetUid("$base/$POINTERS/$key", token)
        if ((pointerOwner != null) && (pointerOwner != uid)) {
            throw UsernameTakenException()
        }

        val claimOwner = restGetUid("$base/$COLLECTION/$key", token)
        if ((claimOwner != null) && (claimOwner != uid) && (pointerOwner != null)) {
            throw UsernameTakenException()
        }

        if (pointerOwner == null) {
            // Create-only so two devices cannot both invent the same hold.
            val created = restPatch(
                url = "$base/$POINTERS/$key?currentDocument.exists=false",
                token = token,
                jsonBody = payload,
                allowFailedPrecondition = true
            )
            if (!created) {
                val raced = restGetUid("$base/$POINTERS/$key", token)
                if (raced != null && raced != uid) throw UsernameTakenException()
                if (raced == null) {
                    throw IllegalStateException("Could not claim that username. Try again.")
                }
            }
        } else {
            restPatch(
                url = "$base/$POINTERS/$key",
                token = token,
                jsonBody = payload,
                allowFailedPrecondition = false
            )
        }

        restPatch(
            url = "$base/$COLLECTION/$key",
            token = token,
            jsonBody = payload,
            allowFailedPrecondition = false
        )

        val verify = restGetUid("$base/$POINTERS/$key", token)
        if (verify != uid) throw UsernameTakenException()

        // Reverse pointer on users/{uid} (merge)
        restPatch(
            url = "$base/$USERS/$uid?updateMask.fieldPaths=$USER_USERNAME_KEY",
            token = token,
            jsonBody = JSONObject()
                .put(
                    "fields",
                    JSONObject().put(USER_USERNAME_KEY, JSONObject().put("stringValue", key))
                )
                .toString(),
            allowFailedPrecondition = false
        )
    }

    private fun firestoreDocumentBody(uid: String, display: String): String =
        JSONObject()
            .put(
                "fields",
                JSONObject()
                    .put("uid", JSONObject().put("stringValue", uid))
                    .put("displayName", JSONObject().put("stringValue", display))
                    .put(
                        "updatedAt",
                        JSONObject().put("integerValue", System.currentTimeMillis().toString())
                    )
            )
            .toString()

    private fun restGetUid(url: String, token: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        http.newCall(request).execute().use { response ->
            if (response.code == 404) return null
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                Log.w(TAG, "REST GET failed code=${response.code} body=${body.take(200)}")
                throw IllegalStateException(restErrorMessage(response.code, body))
            }
            return JSONObject(body)
                .optJSONObject("fields")
                ?.optJSONObject("uid")
                ?.optString("stringValue")
                ?.takeIf { it.isNotBlank() }
        }
    }

    /**
     * @return true if write succeeded; false when [allowFailedPrecondition] and doc already exists
     */
    private fun restPatch(
        url: String,
        token: String,
        jsonBody: String,
        allowFailedPrecondition: Boolean
    ): Boolean {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .method("PATCH", jsonBody.toRequestBody(JSON_MEDIA))
            .build()
        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (response.isSuccessful) return true
            if (allowFailedPrecondition &&
                (response.code == 409 || response.code == 400) &&
                (body.contains("ALREADY_EXISTS", ignoreCase = true) ||
                    body.contains("FAILED_PRECONDITION", ignoreCase = true))
            ) {
                return false
            }
            Log.w(TAG, "REST PATCH failed code=${response.code} body=${body.take(240)}")
            throw IllegalStateException(restErrorMessage(response.code, body))
        }
    }

    private fun restErrorMessage(code: Int, body: String): String {
        if (body.contains("PERMISSION_DENIED", ignoreCase = true) || code == 403) {
            return "Could not claim that username (sign in and try again)."
        }
        if (code == 404) return "Could not reach Firestore. Check your connection and try again."
        if (code in 500..599 || code == 0) {
            return "Could not reach Firestore. Check your connection and try again."
        }
        return "Could not claim that username. Try again."
    }

    private suspend fun releasePreviousKeysBestEffort(
        uid: String,
        keepKey: String,
        token: String
    ) {
        val projectId = FirebaseApp.getInstance().options.projectId ?: return
        val base =
            "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents"
        val previousKeys = candidateKeys(extraDisplay = null).filter { it != keepKey }
        for (oldKey in previousKeys) {
            runCatching {
                val owner = restGetUid("$base/$COLLECTION/$oldKey", token)
                if (owner == uid) restDelete("$base/$COLLECTION/$oldKey", token)
                val pointerOwner = restGetUid("$base/$POINTERS/$oldKey", token)
                if (pointerOwner == uid) restDelete("$base/$POINTERS/$oldKey", token)
            }.onFailure { Log.w(TAG, "Failed releasing previous key $oldKey", it) }
        }
    }

    private fun restDelete(url: String, token: String) {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .delete()
            .build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful && response.code != 404) {
                Log.w(TAG, "REST DELETE failed code=${response.code}")
            }
        }
    }

    private fun mapClaimError(error: Throwable): Exception {
        if (error is UsernameTakenException || exceptionMentionsTaken(error)) {
            return UsernameTakenException()
        }
        if (exceptionMentionsPermission(error)) {
            return IllegalStateException("Could not claim that username (sign in and try again).")
        }
        if (exceptionMentionsUnavailable(error)) {
            return IllegalStateException(
                "Could not reach Firestore. Check your connection and try again."
            )
        }
        return when (error) {
            is IllegalArgumentException, is IllegalStateException -> error
            else -> IllegalStateException(
                error.message?.takeIf { it.isNotBlank() && it.length < 120 && !it.contains("Channel") }
                    ?: "Could not claim that username. Try again."
            )
        }
    }

    /**
     * Releases this account's username claim(s) so the name can be reused after a
     * fresh start. Pass [knownDisplay] from the hero name *before* wiping local data.
     */
    suspend fun releaseClaim(knownDisplay: String? = null) {
        auth.ensureSignedIn()
        val uid = auth.currentUid()
        val token = auth.idToken(forceRefresh = false)
        val keys = candidateKeys(extraDisplay = knownDisplay).toMutableSet()
        if (uid != null && token != null) {
            runCatching {
                val projectId = FirebaseApp.getInstance().options.projectId ?: return@runCatching
                val base =
                    "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents"
                val pointerKey = restGetStringField(
                    "$base/$USERS/$uid",
                    token,
                    USER_USERNAME_KEY
                )
                if (!pointerKey.isNullOrBlank()) keys += normalize(pointerKey)
                for (key in keys) {
                    val owner = restGetUid("$base/$COLLECTION/$key", token)
                    if (owner == uid) restDelete("$base/$COLLECTION/$key", token)
                    val pointerOwner = restGetUid("$base/$POINTERS/$key", token)
                    if (pointerOwner == uid) restDelete("$base/$POINTERS/$key", token)
                }
                // Clear reverse pointer via SDK merge when possible; REST delete-field is awkward.
                clearUserUsernamePointer(uid)
            }
        } else if (uid != null) {
            // SDK fallback when token unavailable
            for (key in keys) {
                runCatching {
                    val ref = firestore.collection(COLLECTION).document(key)
                    val snap = ref.get().await()
                    if (snap.exists() && snap.getString("uid") == uid) ref.delete().await()
                }
                runCatching {
                    val pref = firestore.collection(POINTERS).document(key)
                    val snap = pref.get().await()
                    if (snap.exists() && snap.getString("uid") == uid) pref.delete().await()
                }
            }
            clearUserUsernamePointer(uid)
        }
        prefs.clearUsernameClaim()
    }

    private fun restGetStringField(url: String, token: String, field: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        http.newCall(request).execute().use { response ->
            if (response.code == 404) return null
            if (!response.isSuccessful) return null
            val body = response.body?.string().orEmpty()
            return JSONObject(body)
                .optJSONObject("fields")
                ?.optJSONObject(field)
                ?.optString("stringValue")
                ?.takeIf { it.isNotBlank() }
        }
    }

    /**
     * If the hero already has a non-default name but no claim flag (upgrades),
     * try to claim it. Returns false when the player must pick a new username.
     */
    suspend fun ensureClaimedForExistingName(): Boolean {
        if (prefs.usernameSet.value) {
            syncPublicPointer()
            return true
        }
        val name = db.characterDao().get()?.name?.trim().orEmpty()
        if (name.isBlank() || name.equals("Hero", ignoreCase = true)) return false
        return claim(name).isSuccess
    }

    /** Writes/repairs the public pointer for the locally claimed username. */
    suspend fun syncPublicPointer() {
        val key = prefs.usernameKey.value ?: return
        val uid = auth.currentUid() ?: return
        val token = auth.idToken(forceRefresh = false) ?: return
        val display = db.characterDao().get()?.name?.trim().orEmpty().ifBlank { key }
        runCatching {
            withContext(Dispatchers.IO) {
                val projectId = FirebaseApp.getInstance().options.projectId ?: return@withContext
                val base =
                    "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents"
                val payload = firestoreDocumentBody(uid, display)
                restPatch("$base/$POINTERS/$key", token, payload, allowFailedPrecondition = false)
                restPatch("$base/$COLLECTION/$key", token, payload, allowFailedPrecondition = false)
                restPatch(
                    url = "$base/$USERS/$uid?updateMask.fieldPaths=$USER_USERNAME_KEY",
                    token = token,
                    jsonBody = JSONObject()
                        .put(
                            "fields",
                            JSONObject().put(USER_USERNAME_KEY, JSONObject().put("stringValue", key))
                        )
                        .toString(),
                    allowFailedPrecondition = false
                )
            }
        }
    }

    private suspend fun candidateKeys(extraDisplay: String?): Set<String> = buildSet {
        prefs.usernameKey.value?.let { add(normalize(it)) }
        extraDisplay?.trim()?.takeIf { it.isNotBlank() }?.let { add(normalize(it)) }
        val heroName = db.characterDao().get()?.name?.trim().orEmpty()
        if (heroName.isNotBlank() && !heroName.equals("Hero", ignoreCase = true)) {
            add(normalize(heroName))
        }
    }

    private suspend fun clearUserUsernamePointer(uid: String) {
        runCatching {
            firestore.collection(USERS).document(uid)
                .set(mapOf(USER_USERNAME_KEY to FieldValue.delete()), SetOptions.merge())
                .await()
        }
    }

    private fun exceptionMentionsTaken(error: Throwable): Boolean {
        var current: Throwable? = error
        while (current != null) {
            if (current is UsernameTakenException) return true
            if (current.message?.contains("taken", ignoreCase = true) == true) return true
            current = current.cause
        }
        return false
    }

    private fun exceptionMentionsPermission(error: Throwable): Boolean {
        var current: Throwable? = error
        while (current != null) {
            if (current is FirebaseFirestoreException &&
                current.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
            ) {
                return true
            }
            val msg = current.message.orEmpty()
            if (msg.contains("permission", ignoreCase = true) ||
                msg.contains("PERMISSION_DENIED")
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }

    private fun exceptionMentionsUnavailable(error: Throwable): Boolean {
        var current: Throwable? = error
        while (current != null) {
            if (current is FirebaseFirestoreException &&
                (current.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
                    current.code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ||
                    current.code == FirebaseFirestoreException.Code.CANCELLED)
            ) {
                return true
            }
            val msg = current.message.orEmpty()
            if (msg.contains("UNAVAILABLE", ignoreCase = true) ||
                msg.contains("Channel shutdown", ignoreCase = true) ||
                msg.contains("DEADLINE_EXCEEDED", ignoreCase = true) ||
                msg.contains("Unable to resolve host", ignoreCase = true)
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }

    companion object {
        private const val TAG = "UsernameService"
        private const val COLLECTION = "usernames"
        private const val POINTERS = "usernamePointers"
        private const val USERS = "users"
        private const val USER_USERNAME_KEY = "usernameKey"
        private const val AUTH_TIMEOUT_MS = 8_000L
        private const val CLAIM_TIMEOUT_MS = 30_000L
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
        const val MIN_LEN = 3
        const val MAX_LEN = 20
        private val DISPLAY_PATTERN = Regex("^[A-Za-z][A-Za-z0-9_]{2,19}$")
        private val RESERVED = setOf(
            "hero", "admin", "fitquest", "fitnessrpg", "system", "support", "mod", "moderator"
        )
    }
}

/** Thrown when another account already holds the username. */
class UsernameTakenException : IllegalStateException("That username is taken")
