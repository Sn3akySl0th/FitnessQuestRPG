package com.fitnessquest.rpg.data.feedback

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import com.fitnessquest.rpg.BuildConfig
import com.fitnessquest.rpg.data.db.CharacterEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.security.SecureRandom
import java.util.Locale

enum class FeedbackCategory(val label: String, val emoji: String) {
    BUG("Bug / Glitch", "🐞"),
    SUGGESTION("Feature Idea", "💡"),
    EXERCISE_REQUEST("Exercise Request", "🏋️"),
    EQUIPMENT_REQUEST("Gear / Equipment", "⚔️"),
    USABILITY("UI & Controls", "🎨"),
    PRAISE("Praise / Fun", "⭐")
}

data class FollowUpNote(
    val authorHero: String = "Hero",
    val authorUid: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isDeveloper: Boolean = false
) {
    fun toMap(): Map<String, Any> = mapOf(
        "authorHero" to authorHero,
        "authorUid" to authorUid,
        "text" to text.trim(),
        "timestamp" to timestamp,
        "isDeveloper" to isDeveloper
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): FollowUpNote = FollowUpNote(
            authorHero = map["authorHero"] as? String ?: "Hero",
            authorUid = map["authorUid"] as? String ?: "",
            text = map["text"] as? String ?: "",
            timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            isDeveloper = map["isDeveloper"] as? Boolean ?: false
        )
    }
}

data class BetaTicket(
    val id: String = "",
    val ticketId: String = "",
    val title: String = "",
    val category: FeedbackCategory = FeedbackCategory.BUG,
    val rating: Int = 5,
    val comment: String = "",
    val status: String = "OPEN", // "OPEN", "IN_REVIEW", "IN_PROGRESS", "RESOLVED", "CLOSED"
    val devNotes: String? = null,
    val authorHero: String = "Hero",
    val authorUid: String = "",
    val authorEmail: String? = null,
    val meTooCount: Int = 0,
    val meTooUids: List<String> = emptyList(),
    val followUps: List<FollowUpNote> = emptyList(),
    val appVersion: String = "",
    val deviceModel: String = "",
    val androidVersion: String = "",
    val hasScreenshot: Boolean = false,
    val screenshotUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

object BetaFeedbackRepository {
    private const val TAG = "BetaFeedbackRepo"
    private const val COLLECTION = "beta_feedback"
    private val random = SecureRandom()

    /**
     * Ensures an authenticated Firebase user exists (signing in anonymously if needed)
     * so Firestore and Storage rules permitting signed-in testers will succeed.
     */
    suspend fun ensureAuth(): String {
        val auth = FirebaseAuth.getInstance()
        val existing = auth.currentUser
        if (existing != null) return existing.uid
        return try {
            withTimeoutOrNull(6000L) {
                auth.signInAnonymously().await()
            }
            auth.currentUser?.uid ?: "anonymous"
        } catch (e: Exception) {
            Log.w(TAG, "Anonymous sign-in before feedback failed", e)
            "anonymous"
        }
    }

    /**
     * Generates a clean, human-friendly 6-digit ticket code (e.g. FQ-748291).
     */
    fun generateTicketId(): String {
        val number = 100000 + random.nextInt(900000)
        return "FQ-$number"
    }

    /**
     * Observes real-time community tickets stream from Firestore.
     */
    fun observeTickets(): Flow<List<BetaTicket>> = callbackFlow {
        ensureAuth()
        val db = FirebaseFirestore.getInstance()
        val listener = db.collection(COLLECTION)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Failed to listen to beta tickets", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val tickets = snapshot.documents.mapNotNull { doc ->
                        try {
                            val data = doc.data ?: return@mapNotNull null
                            val categoryName = data["category"] as? String ?: "BUG"
                            val category = FeedbackCategory.entries.firstOrNull { it.name == categoryName } ?: FeedbackCategory.BUG

                            @Suppress("UNCHECKED_CAST")
                            val rawFollowUps = data["followUps"] as? List<Map<String, Any?>> ?: emptyList()
                            val followUps = rawFollowUps.map { FollowUpNote.fromMap(it) }

                            @Suppress("UNCHECKED_CAST")
                            val rawMeToo = data["meTooUids"] as? List<String> ?: emptyList()

                            BetaTicket(
                                id = doc.id,
                                ticketId = data["ticketId"] as? String ?: "FQ-${doc.id.take(6).uppercase(Locale.ROOT)}",
                                title = data["title"] as? String ?: "",
                                category = category,
                                rating = (data["rating"] as? Number)?.toInt() ?: 5,
                                comment = data["comment"] as? String ?: "",
                                status = data["status"] as? String ?: "OPEN",
                                devNotes = data["devNotes"] as? String,
                                authorHero = data["authorHero"] as? String ?: "Vanguard",
                                authorUid = data["uid"] as? String ?: "",
                                authorEmail = data["email"] as? String,
                                meTooCount = (data["meTooCount"] as? Number)?.toInt() ?: rawMeToo.size,
                                meTooUids = rawMeToo,
                                followUps = followUps,
                                appVersion = data["appVersionName"] as? String ?: BuildConfig.VERSION_NAME,
                                deviceModel = data["deviceModel"] as? String ?: "${Build.MANUFACTURER} ${Build.MODEL}",
                                androidVersion = data["androidRelease"] as? String ?: Build.VERSION.RELEASE,
                                hasScreenshot = data["hasScreenshot"] as? Boolean ?: false,
                                screenshotUrl = data["screenshotUrl"] as? String,
                                createdAt = (data["createdAtMillis"] as? Number)?.toLong() ?: System.currentTimeMillis()
                            )
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to parse ticket ${doc.id}", e)
                            null
                        }
                    }
                    trySend(tickets)
                }
            }
        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    /**
     * Submits a new ticket to Firestore with timeouts and triggers developer notifications.
     */
    suspend fun submitTicket(
        title: String,
        category: FeedbackCategory,
        rating: Int,
        comment: String,
        imageUri: Uri?,
        character: CharacterEntity?,
        context: Context
    ): Result<BetaTicket> = withContext(Dispatchers.IO) {
        try {
            val uid = ensureAuth()
            val auth = FirebaseAuth.getInstance()
            val email = auth.currentUser?.email

            val heroName = if (character != null) {
                "${character.name} (Lv ${character.level} ${character.characterClass?.label ?: "Hero"})"
            } else {
                "Pioneer Hero"
            }

            val ticketId = generateTicketId()
            val now = System.currentTimeMillis()

            var screenshotUrl: String? = null
            if (imageUri != null) {
                try {
                    withTimeoutOrNull(10000L) {
                        val storageRef = FirebaseStorage.getInstance()
                            .reference
                            .child("beta_feedback_screenshots/${ticketId}.jpg")
                        val metadata = StorageMetadata.Builder()
                            .setContentType("image/jpeg")
                            .build()
                        storageRef.putFile(imageUri, metadata).await()
                        screenshotUrl = storageRef.downloadUrl.await().toString()
                        Log.d(TAG, "Screenshot uploaded to Firebase Storage: $screenshotUrl")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to upload screenshot to Firebase Storage", e)
                }
            }

            val payload = hashMapOf<String, Any?>(
                "ticketId" to ticketId,
                "title" to title.trim(),
                "category" to category.name,
                "rating" to rating,
                "comment" to comment.trim(),
                "status" to "OPEN",
                "devNotes" to null,
                "authorHero" to heroName,
                "authorEmail" to email,
                "authorUid" to uid,
                "uid" to uid,
                "email" to email,
                "meTooCount" to 0,
                "meTooUids" to emptyList<String>(),
                "followUps" to emptyList<Map<String, Any>>(),
                "deviceManufacturer" to Build.MANUFACTURER,
                "deviceModel" to "${Build.MANUFACTURER} ${Build.MODEL}",
                "androidRelease" to Build.VERSION.RELEASE,
                "androidVersion" to Build.VERSION.RELEASE,
                "androidSdk" to Build.VERSION.SDK_INT,
                "appVersion" to BuildConfig.VERSION_NAME,
                "appVersionName" to BuildConfig.VERSION_NAME,
                "appVersionCode" to BuildConfig.VERSION_CODE,
                "hasScreenshot" to (imageUri != null),
                "screenshotUrl" to screenshotUrl,
                "createdAtMillis" to now,
                "createdAt" to FieldValue.serverTimestamp()
            )

            val docRef = withTimeout(15000L) {
                FirebaseFirestore.getInstance()
                    .collection(COLLECTION)
                    .add(payload)
                    .await()
            }

            val ticket = BetaTicket(
                id = docRef.id,
                ticketId = ticketId,
                title = title.trim(),
                category = category,
                rating = rating,
                comment = comment.trim(),
                status = "OPEN",
                authorHero = heroName,
                authorUid = uid,
                authorEmail = email,
                appVersion = BuildConfig.VERSION_NAME,
                deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                androidVersion = Build.VERSION.RELEASE,
                hasScreenshot = imageUri != null,
                screenshotUrl = screenshotUrl,
                createdAt = now
            )

            Result.success(ticket)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to submit ticket", e)
            Result.failure(e)
        }
    }

    /**
     * Appends a follow-up note to an existing ticket.
     */
    suspend fun addFollowUp(
        ticket: BetaTicket,
        noteText: String,
        character: CharacterEntity?,
        context: Context
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val uid = ensureAuth()
            val heroName = if (character != null) {
                "${character.name} (Lv ${character.level} ${character.characterClass?.label ?: "Hero"})"
            } else {
                "Pioneer"
            }

            val note = FollowUpNote(
                authorHero = heroName,
                authorUid = uid,
                text = noteText.trim(),
                timestamp = System.currentTimeMillis(),
                isDeveloper = false
            )

            withTimeout(12000L) {
                FirebaseFirestore.getInstance()
                    .collection(COLLECTION)
                    .document(ticket.id)
                    .update(
                        "followUps", FieldValue.arrayUnion(note.toMap()),
                        "updatedAt", FieldValue.serverTimestamp()
                    )
                    .await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add follow-up", e)
            Result.failure(e)
        }
    }

    /**
     * Toggles "+1 / Me Too" for the current user.
     */
    suspend fun toggleMeToo(ticket: BetaTicket, uid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            ensureAuth()
            withTimeout(10000L) {
                val docRef = FirebaseFirestore.getInstance().collection(COLLECTION).document(ticket.id)
                val alreadyVoted = ticket.meTooUids.contains(uid)

                if (alreadyVoted) {
                    docRef.update(
                        "meTooUids", FieldValue.arrayRemove(uid),
                        "meTooCount", FieldValue.increment(-1)
                    ).await()
                } else {
                    docRef.update(
                        "meTooUids", FieldValue.arrayUnion(uid),
                        "meTooCount", FieldValue.increment(1)
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle me-too", e)
            Result.failure(e)
        }
    }
}
