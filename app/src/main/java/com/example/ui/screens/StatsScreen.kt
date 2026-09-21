package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.DaylineNowGlow
import com.example.ui.theme.DaylineSky
import com.example.ui.theme.DaylineSuccess
import com.example.ui.viewmodel.DaylineViewModel

@Composable
fun StatsScreen(
    viewModel: DaylineViewModel,
    modifier: Modifier = Modifier
) {
    val daySummary by viewModel.daySummary.collectAsStateWithLifecycle()
    val weekSummary by viewModel.weekSummary.collectAsStateWithLifecycle()
    val categoryStats by viewModel.categoryStats.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Hoje, 1 = Semana

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .testTag("stats_screen")
        ) {
            item {
                Text(
                    text = "Estatísticas Operacionais",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Acompanhamento do planejado vs. realizado em tempo real",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Tab Switcher (Hoje / Esta Semana)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(4.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedTab == 0) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Today,
                                contentDescription = null,
                                tint = if (selectedTab == 0) DaylineSky else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Hoje",
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedTab == 1) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CalendarViewWeek,
                                contentDescription = null,
                                tint = if (selectedTab == 1) DaylineSky else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Esta Semana",
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            if (selectedTab == 0) {
                // HOJE STATS
                item {
                    val busyHours = daySummary.totalBusyMinutes / 60
                    val busyMins = daySummary.totalBusyMinutes % 60
                    val busyText = "${busyHours}h ${busyMins}m"

                    val realHours = daySummary.realizedMinutes / 60
                    val realMins = daySummary.realizedMinutes % 60
                    val realText = "${realHours}h ${realMins}m"

                    val freeHours = daySummary.totalFreeMinutes / 60
                    val freeMins = daySummary.totalFreeMinutes % 60
                    val freeText = "${freeHours}h ${freeMins}m"

                    val punctuality = if (daySummary.totalItems > 0) {
                        ((daySummary.completedItems.toFloat() / daySummary.totalItems.toFloat()) * 100).toInt()
                    } else 100

                    // 2x2 Grid of operational metrics
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Planejado
                            MetricCard(
                                title = "Planejado",
                                value = busyText,
                                subtitle = "${daySummary.totalItems} compromissos",
                                icon = Icons.Default.Schedule,
                                iconTint = DaylineSky,
                                modifier = Modifier.weight(1f)
                            )

                            // Realizado
                            MetricCard(
                                title = "Realizado",
                                value = realText,
                                subtitle = "${daySummary.completedItems} finalizados",
                                icon = Icons.Default.TaskAlt,
                                iconTint = DaylineSuccess,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Livre restante
                            MetricCard(
                                title = "Livre restante",
                                value = freeText,
                                subtitle = "tempo não alocado",
                                icon = Icons.Default.HourglassEmpty,
                                iconTint = DaylineNowGlow,
                                modifier = Modifier.weight(1f)
                            )

                            // Pontualidade
                            MetricCard(
                                title = "Conclusão",
                                value = "$punctuality%",
                                subtitle = "taxa de sucesso",
                                icon = Icons.Default.TrendingUp,
                                iconTint = DaylineSky,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            } else {
                // SEMANA STATS
                item {
                    val avgFreeH = weekSummary.avgFreeMinutesPerDay / 60
                    val avgFreeM = weekSummary.avgFreeMinutesPerDay % 60
                    val avgFreeStr = "${avgFreeH}h ${avgFreeM}m"

                    val completionRate = if (weekSummary.totalItems > 0) {
                        ((weekSummary.completedItems.toFloat() / weekSummary.totalItems.toFloat()) * 100).toInt()
                    } else 0

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            MetricCard(
                                title = "Dia mais carregado",
                                value = weekSummary.busiestDayName,
                                subtitle = "${weekSummary.busiestDayMinutes / 60}h de compromissos",
                                icon = Icons.Default.CalendarViewWeek,
                                iconTint = DaylineSky,
                                modifier = Modifier.weight(1f)
                            )

                            MetricCard(
                                title = "Média livre / dia",
                                value = avgFreeStr,
                                subtitle = "tempo disponível",
                                icon = Icons.Default.HourglassEmpty,
                                iconTint = DaylineNowGlow,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            MetricCard(
                                title = "Compromissos",
                                value = "${weekSummary.completedItems} / ${weekSummary.totalItems}",
                                subtitle = "$completionRate% taxa de conclusão",
                                icon = Icons.Default.CheckCircle,
                                iconTint = DaylineSuccess,
                                modifier = Modifier.weight(1f)
                            )

                            MetricCard(
                                title = "Total Planejado",
                                value = "${weekSummary.totalBusyMinutes / 60}h",
                                subtitle = "na semana inteira",
                                icon = Icons.Default.Schedule,
                                iconTint = DaylineSky,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // Category Breakdown Section
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PieChart,
                        contentDescription = null,
                        tint = DaylineSky,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tempo por Categoria (${if (selectedTab == 0) "Hoje" else "Semana"})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (categoryStats.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = "Sem compromissos com duração registrados para este período.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(categoryStats) { stat ->
                    val catColor = Color(stat.category.colorHex)
                    val hours = stat.totalMinutes / 60
                    val mins = stat.totalMinutes % 60
                    val timeStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(catColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stat.category.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Text(
                                    text = "$timeStr (${(stat.percentage * 100).toInt()}%)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = { stat.percentage },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = catColor,
                                trackColor = catColor.copy(alpha = 0.2f),
                                strokeCap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier.border(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            RoundedCornerShape(14.dp)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
