package com.fitnessquest.rpg.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.fitnessquest.rpg.BuildConfig
import com.fitnessquest.rpg.data.db.CharacterEntity
import com.fitnessquest.rpg.data.feedback.BetaFeedbackNotifier
import com.fitnessquest.rpg.data.feedback.BetaFeedbackRepository
import com.fitnessquest.rpg.data.feedback.BetaTicket
import com.fitnessquest.rpg.data.feedback.FeedbackCategory
import com.fitnessquest.rpg.data.feedback.FollowUpNote
import com.fitnessquest.rpg.ui.theme.Gold
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BetaFeedbackDialog(
    character: CharacterEntity?,
    onDismiss: () -> Unit,
    onFeedbackSubmitted: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authUid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }

    val tickets by remember { BetaFeedbackRepository.observeTickets() }.collectAsState(initial = emptyList())

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Community Tickets, 1 = New Ticket
    var selectedTicketForDetail by remember { mutableStateOf<BetaTicket?>(null) }
    var submittedTicketId by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "🛡️ Beta Feedback & Tickets",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Gold
                        )
                        Text(
                            "Community Tracker & Direct Dev Hub",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                if (selectedTicketForDetail != null) {
                    // Detail & Thread View for a selected ticket
                    val liveTicket = tickets.find { it.id == selectedTicketForDetail?.id } ?: selectedTicketForDetail!!
                    TicketDetailView(
                        ticket = liveTicket,
                        character = character,
                        currentUid = authUid,
                        onBack = { selectedTicketForDetail = null }
                    )
                } else {
                    // Tabs: Community Tickets vs Submit New Report
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = Gold,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = Gold
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Filled.Forum, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Tickets (${tickets.size})", fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Filled.PostAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("New Report", fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                    }

                    if (selectedTab == 0) {
                        CommunityTicketsScreen(
                            tickets = tickets,
                            currentUid = authUid,
                            onSelectTicket = { selectedTicketForDetail = it },
                            onSwitchToNew = { selectedTab = 1 }
                        )
                    } else {
                        NewReportScreen(
                            character = character,
                            submittedTicketId = submittedTicketId,
                            onTicketSubmitted = { newTicketId ->
                                submittedTicketId = newTicketId
                                onFeedbackSubmitted?.invoke()
                            },
                            onViewTickets = {
                                submittedTicketId = null
                                selectedTab = 0
                            },
                            onDismiss = onDismiss
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CommunityTicketsScreen(
    tickets: List<BetaTicket>,
    currentUid: String,
    onSelectTicket: (BetaTicket) -> Unit,
    onSwitchToNew: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var filterCategory by remember { mutableStateOf<FeedbackCategory?>(null) }
    var filterMyOnly by remember { mutableStateOf(false) }
    var filterOpenOnly by remember { mutableStateOf(false) }

    val filteredTickets = remember(tickets, searchQuery, filterCategory, filterMyOnly, filterOpenOnly, currentUid) {
        tickets.filter { ticket ->
            val matchesSearch = searchQuery.isBlank() ||
                ticket.ticketId.contains(searchQuery, ignoreCase = true) ||
                ticket.title.contains(searchQuery, ignoreCase = true) ||
                ticket.comment.contains(searchQuery, ignoreCase = true) ||
                ticket.authorHero.contains(searchQuery, ignoreCase = true)

            val matchesCategory = filterCategory == null || ticket.category == filterCategory
            val matchesMy = !filterMyOnly || (currentUid.isNotBlank() && ticket.authorUid == currentUid)
            val matchesOpen = !filterOpenOnly || ticket.status != "RESOLVED" && ticket.status != "CLOSED"

            matchesSearch && matchesCategory && matchesMy && matchesOpen
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search tickets by keyword, ID (e.g. FQ-123456)...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = Gold) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Gold,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Filter Chips Row
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = filterMyOnly,
                onClick = { filterMyOnly = !filterMyOnly },
                label = { Text("👤 My Tickets") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Gold.copy(alpha = 0.25f))
            )
            FilterChip(
                selected = filterOpenOnly,
                onClick = { filterOpenOnly = !filterOpenOnly },
                label = { Text("🟡 Open Only") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Gold.copy(alpha = 0.25f))
            )
            FeedbackCategory.entries.forEach { cat ->
                FilterChip(
                    selected = filterCategory == cat,
                    onClick = { filterCategory = if (filterCategory == cat) null else cat },
                    label = { Text("${cat.emoji} ${cat.label}") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Gold.copy(alpha = 0.25f))
                )
            }
        }

        if (filteredTickets.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("🔍", fontSize = 48.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (searchQuery.isNotBlank() || filterCategory != null || filterMyOnly) "No matching tickets found" else "No tickets reported yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "If you found a bug or have a suggestion, open a new ticket!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = onSwitchToNew) {
                    Icon(Icons.Filled.PostAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Submit New Ticket")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredTickets, key = { it.id }) { ticket ->
                    TicketCard(
                        ticket = ticket,
                        currentUid = currentUid,
                        onClick = { onSelectTicket(ticket) },
                        onToggleMeToo = {
                            if (currentUid.isNotBlank()) {
                                scope.launch {
                                    BetaFeedbackRepository.toggleMeToo(ticket, currentUid)
                                }
                            } else {
                                Toast.makeText(context, "Sign-in required to vote", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TicketCard(
    ticket: BetaTicket,
    currentUid: String,
    onClick: () -> Unit,
    onToggleMeToo: () -> Unit
) {
    val hasVoted = currentUid.isNotBlank() && ticket.meTooUids.contains(currentUid)
    val hasDevReply = !ticket.devNotes.isNullOrBlank()

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(
            1.dp,
            if (hasDevReply) Gold.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top row: Ticket ID & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Gold.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Gold.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = ticket.ticketId,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = Gold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "${ticket.category.emoji} ${ticket.category.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusBadge(status = ticket.status)
            }

            // Title & Content Preview
            if (ticket.title.isNotBlank()) {
                Text(
                    text = ticket.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = ticket.comment,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Developer Note Highlight (if present)
            if (hasDevReply) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Gold.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, Gold.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🛡️", fontSize = 14.sp)
                        Text(
                            text = "Dev Reply: ${ticket.devNotes}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Gold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Bottom row: Author, Date, and "+1 Me Too" Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "By ${ticket.authorHero} • ${formatRelativeDate(ticket.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (ticket.followUps.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Icon(Icons.Filled.Forum, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${ticket.followUps.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Me Too Button
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (hasVoted) Gold.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(1.dp, if (hasVoted) Gold else Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier.clickable(onClick = onToggleMeToo)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (hasVoted) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                contentDescription = "Me Too",
                                tint = if (hasVoted) Gold else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (ticket.meTooCount > 0) "+${ticket.meTooCount} Me Too" else "Me Too",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (hasVoted) FontWeight.Bold else FontWeight.Normal,
                                color = if (hasVoted) Gold else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (bgColor, textColor, label) = when (status.uppercase(Locale.ROOT)) {
        "OPEN" -> Triple(Gold.copy(alpha = 0.2f), Gold, "🟡 Open")
        "IN_REVIEW" -> Triple(Color(0xFF3B82F6).copy(alpha = 0.2f), Color(0xFF60A5FA), "🔵 In Review")
        "IN_PROGRESS" -> Triple(Color(0xFFA855F7).copy(alpha = 0.2f), Color(0xFFC084FC), "🟣 In Progress")
        "RESOLVED" -> Triple(Color(0xFF22C55E).copy(alpha = 0.2f), Color(0xFF4ADE80), "🟢 Resolved")
        "CLOSED" -> Triple(Color.Gray.copy(alpha = 0.2f), Color.LightGray, "⚪ Closed")
        else -> Triple(Gold.copy(alpha = 0.2f), Gold, status)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.4f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun TicketDetailView(
    ticket: BetaTicket,
    character: CharacterEntity?,
    currentUid: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var followUpText by remember { mutableStateOf("") }
    var submittingFollowUp by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Back / Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("← Back to List", fontSize = 13.sp)
            }

            IconButton(onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Ticket ID", ticket.ticketId))
                Toast.makeText(context, "Copied ${ticket.ticketId} to clipboard", Toast.LENGTH_SHORT).show()
            }) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Ticket ID", tint = Gold)
            }
        }

        Spacer(Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Main Ticket Info Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ticket.ticketId,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = Gold
                        )
                        StatusBadge(status = ticket.status)
                    }

                    Text(
                        text = "${ticket.category.emoji} ${ticket.category.label} • Rating: ${"⭐".repeat(ticket.rating.coerceIn(1, 5))}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (ticket.title.isNotBlank()) {
                        Text(
                            text = ticket.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = ticket.comment,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    Text(
                        text = "Reported by ${ticket.authorHero} • ${formatFullDate(ticket.createdAt)}\nDevice: ${ticket.deviceModel} (FitQuest v${ticket.appVersion})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Attached Screenshot Preview (if present)
            if (!ticket.screenshotUrl.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("📸", fontSize = 16.sp)
                            Text("Attached Screenshot", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Gold)
                        }
                        AsyncImage(
                            model = ticket.screenshotUrl,
                            contentDescription = "Attached Screenshot",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                        )
                    }
                }
            }

            // Developer Response Section
            if (!ticket.devNotes.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Gold.copy(alpha = 0.12f),
                    border = BorderStroke(1.5.dp, Gold),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("🛡️", fontSize = 18.sp)
                            Text("Developer Response", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Gold)
                        }
                        Text(
                            text = ticket.devNotes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Follow-up Notes Thread
            if (ticket.followUps.isNotEmpty()) {
                Text(
                    text = "Discussion & Additional Info (${ticket.followUps.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Gold
                )

                ticket.followUps.forEach { note ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (note.isDeveloper) "🛡️ Dev Team" else note.authorHero,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (note.isDeveloper) Gold else MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = formatRelativeDate(note.timestamp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(text = note.text, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // Add Follow-Up Input
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Add Additional Information / Repro Notes",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Gold
                    )
                    OutlinedTextField(
                        value = followUpText,
                        onValueChange = { followUpText = it },
                        placeholder = { Text("Add more details, steps to reproduce, or notes from your device...") },
                        minLines = 2,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                if (followUpText.isNotBlank()) {
                                    submittingFollowUp = true
                                    scope.launch {
                                        val res = BetaFeedbackRepository.addFollowUp(
                                            ticket = ticket,
                                            noteText = followUpText,
                                            character = character,
                                            context = context
                                        )
                                        submittingFollowUp = false
                                        if (res.isSuccess) {
                                            followUpText = ""
                                            Toast.makeText(context, "Added note to ${ticket.ticketId}!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Failed to submit note.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            enabled = !submittingFollowUp && followUpText.isNotBlank(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (submittingFollowUp) {
                                CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.AddComment, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Post Note")
                            }
                        }
                    }
                }
            }

            // Direct Email Fallback
            OutlinedButton(
                onClick = {
                    val (subject, fields) = BetaFeedbackNotifier.buildNewTicketFields(ticket)
                    BetaFeedbackNotifier.openDeveloperEmail(
                        context = context,
                        subject = subject,
                        body = fields.entries.joinToString("\n") { "${it.key}: ${it.value}" }
                    )
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Email Developer Directly About Ticket")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NewReportScreen(
    character: CharacterEntity?,
    submittedTicketId: String?,
    onTicketSubmitted: (String) -> Unit,
    onViewTickets: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(FeedbackCategory.BUG) }
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var submitting by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    if (submittedTicketId != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Gold, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text(
                "Ticket Dispatched!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = Gold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Your ticket number is:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Gold.copy(alpha = 0.15f),
                border = BorderStroke(2.dp, Gold)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = submittedTicketId,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = Gold
                    )
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Ticket ID", submittedTicketId))
                        Toast.makeText(context, "Copied $submittedTicketId", Toast.LENGTH_SHORT).show()
                    }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = Gold, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "You can track status updates and dev replies right inside the Tickets tab.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Close")
                }
                Button(onClick = onViewTickets, modifier = Modifier.weight(1f)) {
                    Text("View Tickets")
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Category Selection
            Text(
                "What would you like to report?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Gold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeedbackCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text("${cat.emoji} ${cat.label}") }
                    )
                }
            }

            // Star Rating
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Overall Experience Rating",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Gold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..5).forEach { star ->
                        Icon(
                            imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = "$star stars",
                            tint = if (star <= rating) Gold else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { rating = star }
                        )
                    }
                }
            }

            // Summary Title
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Summary / Title (Optional)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Gold
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("e.g. Watch heart rate dropped during combat...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Feedback Details
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Details & Observations",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Gold
                )
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    placeholder = {
                        Text(
                            when (category) {
                                FeedbackCategory.BUG -> "What went wrong? Steps to reproduce, what you expected vs what happened..."
                                FeedbackCategory.SUGGESTION -> "What would make this feature or workout mechanic more enjoyable?"
                                FeedbackCategory.EXERCISE_REQUEST -> "What exercise should be added? (Target muscle, movement type, reps/time)..."
                                FeedbackCategory.EQUIPMENT_REQUEST -> "What weapon, armor, gym tool, or accessory should be added?"
                                FeedbackCategory.USABILITY -> "What screen or button felt awkward, cramped, or hard to use?"
                                FeedbackCategory.PRAISE -> "What's your favorite part of the game so far?"
                            }
                        )
                    },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Screenshot Attachment
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Attach Screenshot (Optional)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Gold
                )
                if (selectedImageUri != null) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    ) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Selected Screenshot",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { selectedImageUri = null },
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove Screenshot", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Pick screenshot from gallery")
                    }
                }
            }

            // Diagnostic Telemetry Preview
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("📋 Auto-attached diagnostic telemetry:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Gold)
                    Text("• Build: FitQuest v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", style = MaterialTheme.typography.labelSmall)
                    Text("• Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})", style = MaterialTheme.typography.labelSmall)
                    if (character != null) {
                        Text("• Hero: ${character.name} (Lv ${character.level} ${character.characterClass?.label ?: "Hero"})", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Submit Button
            Button(
                onClick = {
                    submitting = true
                    scope.launch {
                        try {
                            val result = BetaFeedbackRepository.submitTicket(
                                title = title,
                                category = category,
                                rating = rating,
                                comment = comment,
                                imageUri = selectedImageUri,
                                character = character,
                                context = context
                            )
                            if (result.isSuccess) {
                                val ticket = result.getOrNull()
                                onTicketSubmitted(ticket?.ticketId ?: "FQ-TICKET")
                            } else {
                                // Fallback to email dispatch
                                Toast.makeText(context, "Network issue: opening email fallback", Toast.LENGTH_SHORT).show()
                                val fallbackTicket = BetaTicket(
                                    ticketId = BetaFeedbackRepository.generateTicketId(),
                                    title = title,
                                    category = category,
                                    rating = rating,
                                    comment = comment,
                                    appVersion = BuildConfig.VERSION_NAME,
                                    deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                                    androidVersion = Build.VERSION.RELEASE
                                )
                                val (subj, fields) = BetaFeedbackNotifier.buildNewTicketFields(fallbackTicket)
                                BetaFeedbackNotifier.openDeveloperEmail(
                                    context = context,
                                    subject = subj,
                                    body = fields.entries.joinToString("\n") { "${it.key}: ${it.value}" },
                                    attachmentUri = selectedImageUri
                                )
                                onTicketSubmitted(fallbackTicket.ticketId)
                            }
                        } finally {
                            submitting = false
                        }
                    }
                },
                enabled = !submitting && comment.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (submitting) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Generating Ticket & Submitting...")
                } else {
                    Text("Submit Feedback Ticket")
                }
            }
        }
    }
}

private fun formatRelativeDate(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 0 -> "${days}d ago"
        hours > 0 -> "${hours}h ago"
        minutes > 0 -> "${minutes}m ago"
        else -> "just now"
    }
}

private fun formatFullDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
