package com.fitnessquest.rpg.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitnessquest.rpg.ui.theme.Gold
import com.fitnessquest.rpg.ui.theme.NightBg
import com.fitnessquest.rpg.ui.theme.NightSurface
import com.fitnessquest.rpg.ui.theme.Parchment

data class DockTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val hasBadge: Boolean = false,
)

/** Fitscape-style floating pill dock for root tabs. */
@Composable
fun FloatingGameDock(
    tabs: List<DockTab>,
    currentRoute: String?,
    onTabClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Box(
            modifier = modifier
                .fillMaxHeight()
                .navigationBarsPadding()
                .padding(start = 8.dp, top = 8.dp, bottom = 8.dp, end = 2.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxHeight(),
                shape = RoundedCornerShape(24.dp),
                color = NightSurface.copy(alpha = 0.96f),
                contentColor = Parchment,
                tonalElevation = 6.dp,
                shadowElevation = 10.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = 0.22f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 12.dp, horizontal = 6.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    tabs.forEach { tab ->
                        DockItemVertical(
                            tab = tab,
                            selected = currentRoute == tab.route,
                            onClick = { onTabClick(tab.route) }
                        )
                    }
                }
            }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = NightSurface.copy(alpha = 0.96f),
                contentColor = Parchment,
                tonalElevation = 6.dp,
                shadowElevation = 10.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = 0.22f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { tab ->
                        DockItem(
                            tab = tab,
                            selected = currentRoute == tab.route,
                            onClick = { onTabClick(tab.route) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DockItemVertical(
    tab: DockTab,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = tween(180),
        label = "dockScale"
    )
    Column(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (selected) Gold.copy(alpha = 0.18f) else Color.Transparent)
                .then(
                    if (selected) Modifier.border(1.5.dp, Gold, CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                tab.icon,
                contentDescription = tab.label,
                tint = if (selected) Gold else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            if (tab.hasBadge) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Gold)
                        .align(Alignment.TopEnd)
                )
            }
        }
        Text(
            tab.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (selected) Gold else Parchment.copy(alpha = 0.7f),
            maxLines = 1
        )
    }
}

@Composable
private fun RowScope.DockItem(
    tab: DockTab,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = tween(180),
        label = "dockScale"
    )
    Column(
        modifier = Modifier
            .weight(1f)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (selected) Gold.copy(alpha = 0.18f) else Color.Transparent)
                .then(
                    if (selected) Modifier.border(1.5.dp, Gold, CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                tab.icon,
                contentDescription = tab.label,
                tint = if (selected) Gold else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            if (tab.hasBadge) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Gold)
                        .align(Alignment.TopEnd)
                )
            }
        }
        Text(
            tab.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) Gold else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

enum class SceneKind {
    HERO, TRAIN, BATTLE, ALLIES, SHOP
}

/**
 * Full-bleed scene header: gradient art strip + display title + tagline.
 * Optional [actions] row sits on the trailing edge (settings, history, etc.).
 */
@Composable
fun SceneBanner(
    kind: SceneKind,
    title: String,
    tagline: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val brush = remember(kind) { sceneBrush(kind) }
    var shown by remember { mutableStateOf(value = false) }
    LaunchedEffect(kind) { shown = true }
    val alpha by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(420),
        label = "bannerAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(132.dp)
            .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
            .background(brush)
    ) {
        // Soft vignette for readable type
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, NightBg.copy(alpha = 0.55f))
                    )
                )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = Parchment.copy(alpha = alpha),
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp,
                    letterSpacing = 1.2.sp,
                    lineHeight = 30.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    tagline,
                    color = Gold.copy(alpha = 0.9f * alpha),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }
    }
}

/** Compact hub card used on Allies home (Party / Guild / Rivals). */
@Composable
fun AlliesHubCard(
    title: String,
    subtitle: String,
    emoji: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: String? = null
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = NightSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(emoji, fontSize = 28.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (trailing != null) {
                Text(
                    trailing,
                    color = Gold,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(">", color = Gold, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun sceneBrush(kind: SceneKind): Brush = when (kind) {
    SceneKind.HERO -> Brush.linearGradient(
        listOf(Color(0xFF2A1F3D), Color(0xFF1A2438), Color(0xFF12131F))
    )
    SceneKind.TRAIN -> Brush.linearGradient(
        listOf(Color(0xFF1E3A2F), Color(0xFF1A2A38), Color(0xFF12131F))
    )
    SceneKind.BATTLE -> Brush.linearGradient(
        listOf(Color(0xFF3A1A1A), Color(0xFF2A1830), Color(0xFF12131F))
    )
    SceneKind.ALLIES -> Brush.linearGradient(
        listOf(Color(0xFF1A2F4A), Color(0xFF24305A), Color(0xFF12131F))
    )
    SceneKind.SHOP -> Brush.linearGradient(
        listOf(Color(0xFF3A2E14), Color(0xFF2A2418), Color(0xFF12131F))
    )
}
