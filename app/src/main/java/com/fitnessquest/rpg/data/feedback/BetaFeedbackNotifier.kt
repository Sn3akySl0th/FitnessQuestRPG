package com.fitnessquest.rpg.data.feedback

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

/**
 * Dispatches email notifications to the developer for Closed Beta tickets and follow-ups.
 * Automated notifications are processed server-side via native Firebase Cloud Functions.
 */
object BetaFeedbackNotifier {
    private const val TAG = "BetaFeedbackNotifier"
    private const val DEV_EMAIL = "fitnessquestrpg@gmail.com"

    /**
     * Builds structured field map for a new ticket.
     */
    fun buildNewTicketFields(ticket: BetaTicket): Pair<String, Map<String, String>> {
        val subject = "[FitQuest Beta] New Ticket ${ticket.ticketId} (OPEN): ${ticket.category.emoji} ${ticket.title.ifBlank { ticket.category.label }}"
        val fields = linkedMapOf(
            "Ticket ID" to ticket.ticketId,
            "Category" to "${ticket.category.emoji} ${ticket.category.label}",
            "Status" to "${ticket.status} (OPEN)",
            "Rating" to "${"⭐".repeat(ticket.rating.coerceIn(1, 5))} (${ticket.rating}/5)",
            "Author Hero" to ticket.authorHero,
            "User Account" to (ticket.authorEmail ?: "Guest (${ticket.authorUid.take(8)})"),
            "Title" to ticket.title.ifBlank { "N/A" },
            "Comment / Details" to ticket.comment,
            "App Version" to "v${ticket.appVersion}",
            "Device Model" to ticket.deviceModel,
            "Android OS" to ticket.androidVersion,
            "Screenshot" to (ticket.screenshotUrl ?: if (ticket.hasScreenshot) "Uploaded" else "None")
        )
        return subject to fields
    }

    /**
     * Builds structured field map for a follow-up note.
     */
    fun buildFollowUpFields(ticket: BetaTicket, note: FollowUpNote): Pair<String, Map<String, String>> {
        val subject = "[FitQuest Beta] Update on ${ticket.ticketId}: ${note.authorHero} added notes"
        val fields = linkedMapOf(
            "Ticket ID" to ticket.ticketId,
            "Original Title" to ticket.title.ifBlank { ticket.category.label },
            "Status" to ticket.status,
            "Note Author" to note.authorHero,
            "Additional Info / Comment" to note.text
        )
        return subject to fields
    }

    /**
     * Direct email intent fallback if user taps "Email Developer Directly".
     */
    fun openDeveloperEmail(
        context: Context,
        subject: String,
        body: String,
        attachmentUri: Uri? = null
    ) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = if (attachmentUri != null) "image/*" else "text/plain"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(DEV_EMAIL))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                if (attachmentUri != null) {
                    putExtra(Intent.EXTRA_STREAM, attachmentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Dispatch Feedback Email").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch email intent", e)
        }
    }
}
