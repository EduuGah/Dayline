package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DaylineNowGlow
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun NowIndicator(
    currentTime: LocalTime,
    modifier: Modifier = Modifier
) {
    val formattedTime = currentTime.format(DateTimeFormatter.ofPattern("HH:mm"))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("now_indicator")
    ) {
        // Time badge
        Box(
            modifier = Modifier
                .width(64.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(DaylineNowGlow.copy(alpha = 0.15f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = formattedTime,
                color = DaylineNowGlow,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Glowing node
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(DaylineNowGlow),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Live line across the timeline
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .background(DaylineNowGlow)
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = "AGORA",
            color = DaylineNowGlow,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
    }
}
