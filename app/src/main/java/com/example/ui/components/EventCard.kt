package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EventOccurrence
import com.example.model.EventStatus
import com.example.ui.theme.DaylineError
import com.example.ui.theme.DaylineNowGlow
import com.example.ui.theme.DaylineSky
import com.example.ui.theme.DaylineSuccess
import java.time.format.DateTimeFormatter

/**
 * Timeline row item displaying time, node, and a card wrapped with [SwipeableTimelineItem].
 */
@Composable
fun EventCard(
    occurrence: EventOccurrence,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onPostpone: ((Long) -> Unit)? = null,
    onComplete: (() -> Unit)? = null,
    onReopen: (() -> Unit)? = null,
    isLocked: Boolean = false,
    modifier: Modifier = Modifier
) {
    val event = occurrence.event
    val isCompleted = occurrence.status == EventStatus.COMPLETED || event.isCompleted
    val isNow = occurrence.status == EventStatus.NOW
    val isOverdue = occurrence.status == EventStatus.OVERDUE
    val isPast = occurrence.status == EventStatus.PAST && !isCompleted
    val categoryColor = occurrence.category?.colorHex?.let { Color(it) } ?: DaylineSky

    // Hierarchy styling:
    // Completed/Past: gracefully dimmed to preserve chronological history without cluttering
    // Now: Hero spotlight with high contrast
    // Upcoming: Crisp, vibrant
    val cardAlpha = when {
        isCompleted -> 0.65f
        isPast -> 0.70f
        else -> 1.0f
    }

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val startTimeStr = occurrence.startTime?.format(timeFormatter) ?: "--:--"
    val endTimeStr = occurrence.endTime?.format(timeFormatter) ?: "--:--"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = if (isNow) 8.dp else 4.dp)
            .alpha(cardAlpha)
            .testTag("event_row_${event.id}")
    ) {
        // Left Column: Times
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .width(56.dp)
                .padding(top = if (isNow) 12.dp else 8.dp, end = 8.dp)
        ) {
            Text(
                text = startTimeStr,
                fontSize = if (isNow) 14.sp else 13.sp,
                fontWeight = if (isNow) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isNow) DaylineNowGlow else if (isCompleted || isPast) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = endTimeStr,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }

        // Timeline Node
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = if (isNow) 14.dp else 10.dp, end = 10.dp)
        ) {
            val nodeColor by animateColorAsState(
                targetValue = when {
                    isCompleted -> DaylineSuccess
                    isNow -> DaylineNowGlow
                    isPast -> categoryColor.copy(alpha = 0.5f)
                    else -> categoryColor
                },
                label = "nodeColor"
            )

            Box(
                modifier = Modifier
                    .size(if (isNow) 14.dp else 10.dp)
                    .clip(CircleShape)
                    .background(nodeColor)
                    .then(if (isNow) Modifier.border(2.dp, Color.White, CircleShape) else Modifier)
            )
        }

        // Reusable Swipe Wrapper:
        // Holds back layer (Concluir / Adiar 30m) fixed underneath, and translates the card with finger
        SwipeableTimelineItem(
            modifier = Modifier.weight(1f),
            isCompleted = isCompleted,
            isLocked = isLocked,
            enabledRight = true,
            enabledLeft = onPostpone != null || occurrence.startTime != null,
            onSwipeRight = {
                if (isCompleted) {
                    onReopen?.invoke() ?: onToggleComplete()
                } else {
                    onComplete?.invoke() ?: onToggleComplete()
                }
            },
            onSwipeLeft = {
                if (onPostpone != null && occurrence.startTime != null) {
                    onPostpone(30L)
                } else {
                    onEdit()
                }
            }
        ) {
            TimelineEventCard(
                occurrence = occurrence,
                isNow = isNow,
                isPast = isPast,
                isOverdue = isOverdue,
                isCompleted = isCompleted,
                onClick = onClick,
                onEdit = onEdit,
                onToggleComplete = onToggleComplete,
                onPostpone = onPostpone,
                onDuplicate = onDuplicate,
                onDelete = onDelete
            )
        }
    }
}

/**
 * Pure presentation card for the timeline event.
 * Has no gesture translation logic — purely responsible for layout, typography, colors and actions.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelineEventCard(
    occurrence: EventOccurrence,
    isNow: Boolean,
    isPast: Boolean,
    isOverdue: Boolean,
    isCompleted: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onToggleComplete: () -> Unit,
    onPostpone: ((Long) -> Unit)?,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val event = occurrence.event
    var showMenu by remember { mutableStateOf(false) }

    val cardBorderModifier = when {
        isNow -> Modifier.border(2.dp, DaylineNowGlow, RoundedCornerShape(16.dp))
        isOverdue -> Modifier.border(1.dp, DaylineError.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
        isCompleted || isPast -> Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
        else -> Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
    }

    val cardBg = when {
        isNow -> DaylineNowGlow.copy(alpha = 0.12f)
        isCompleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        isPast -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        else -> MaterialTheme.colorScheme.surface
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        tonalElevation = if (isNow) 6.dp else 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .then(cardBorderModifier)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = { showMenu = true }
            )
    ) {
        Column(
            modifier = Modifier.padding(if (isNow) 16.dp else 12.dp)
        ) {
            // Top Row: Category tag, Status badge, Repeat, Complete & Menu
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    // Category dot & name
                    occurrence.category?.let { cat ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(cat.colorHex))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = cat.name.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = Color(cat.colorHex)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    if (occurrence.isRecurring) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = "Recorrente",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    if (isNow) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(DaylineNowGlow)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "AGORA",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    } else if (isOverdue) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(DaylineError.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ATRASADO",
                                color = DaylineError,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Complete Toggle Button & More Menu
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Circular check button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCompleted) DaylineSuccess else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                            .clickable { onToggleComplete() }
                            .testTag("complete_toggle_${event.id}")
                    ) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Concluído",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // More dropdown menu
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Mais opções",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isCompleted) "Desmarcar conclusão" else "Concluir") },
                                leadingIcon = {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = DaylineSuccess)
                                },
                                onClick = {
                                    showMenu = false
                                    onToggleComplete()
                                }
                            )

                            if (onPostpone != null && occurrence.startTime != null) {
                                DropdownMenuItem(
                                    text = { Text("Adiar 15 min") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Schedule, contentDescription = null)
                                    },
                                    onClick = {
                                        showMenu = false
                                        onPostpone(15L)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Adiar 30 min") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Schedule, contentDescription = null)
                                    },
                                    onClick = {
                                        showMenu = false
                                        onPostpone(30L)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Adiar 1 hora") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Schedule, contentDescription = null)
                                    },
                                    onClick = {
                                        showMenu = false
                                        onPostpone(60L)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Mover para outro horário") },
                                    leadingIcon = {
                                        Icon(Icons.Default.DateRange, contentDescription = null)
                                    },
                                    onClick = {
                                        showMenu = false
                                        onEdit()
                                    }
                                )
                            }

                            HorizontalDivider()

                            DropdownMenuItem(
                                text = { Text("Editar") },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Duplicar") },
                                leadingIcon = {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                                },
                                onClick = {
                                    showMenu = false
                                    onDuplicate()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Excluir", color = DaylineError) },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = DaylineError)
                                },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Title - Hero typography if Now
            Text(
                text = event.title,
                fontSize = if (isNow) 17.sp else 15.sp,
                fontWeight = if (isNow) FontWeight.Bold else FontWeight.SemiBold,
                textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
            )

            // Optional Description
            if (!event.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = event.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 2
                )
            }

            // Conflict warning
            if (occurrence.hasConflict) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFEF3C7))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Conflito",
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Conflito com outro horário",
                        fontSize = 10.sp,
                        color = Color(0xFF92400E),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Live Progress bar if happening right now
            if (isNow) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { occurrence.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = DaylineNowGlow,
                    trackColor = DaylineNowGlow.copy(alpha = 0.2f),
                    strokeCap = StrokeCap.Round
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val remaining = occurrence.remainingMinutes
                    Text(
                        text = if (remaining != null && remaining > 0) "Restam $remaining min" else "Finalizando...",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = DaylineNowGlow
                    )
                    Text(
                        text = "${(occurrence.progressFraction * 100).toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DaylineNowGlow
                    )
                }
            }
        }
    }
}
