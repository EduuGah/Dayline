package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FreeInterval
import com.example.ui.theme.DaylineNowGlow
import java.time.format.DateTimeFormatter

@Composable
fun FreeIntervalCard(
    interval: FreeInterval,
    onClickInterval: (interval: FreeInterval) -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val startStr = interval.startTime.format(timeFormatter)
    val endStr = interval.endTime.format(timeFormatter)

    val durationText = if (interval.durationMinutes >= 60) {
        val hours = interval.durationMinutes / 60
        val mins = interval.durationMinutes % 60
        if (mins > 0) "${hours}h${mins}m livre" else "${hours}h livre"
    } else {
        "${interval.durationMinutes}m livre"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("free_interval_${startStr}")
    ) {
        // Left Column Placeholder to align with timeline times
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .width(56.dp)
                .padding(end = 8.dp)
        ) {
            Text(
                text = "$startStr",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
            )
        }

        // Timeline connector
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(end = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            )
        }

        // Free space pill - clean, subtle, interactive
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    RoundedCornerShape(10.dp)
                )
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                .clickable { onClickInterval(interval) }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowDownward,
                contentDescription = null,
                tint = DaylineNowGlow.copy(alpha = 0.8f),
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$durationText ($startStr — $endStr)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = DaylineNowGlow.copy(alpha = 0.9f)
            )
        }
    }
}
