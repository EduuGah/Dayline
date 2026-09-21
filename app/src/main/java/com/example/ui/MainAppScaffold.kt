package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InboxScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.viewmodel.DaylineNavTab
import com.example.ui.viewmodel.DaylineViewModel

@Composable
fun MainAppScaffold(
    viewModel: DaylineViewModel,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val unscheduledTasks by viewModel.unscheduledTasks.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                // Tab 1: Timeline / Hoje
                NavigationBarItem(
                    selected = currentTab == DaylineNavTab.TIMELINE,
                    onClick = { viewModel.setTab(DaylineNavTab.TIMELINE) },
                    icon = {
                        Icon(imageVector = Icons.Default.Schedule, contentDescription = "Timeline")
                    },
                    label = { Text("Hoje", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_timeline")
                )

                // Tab 2: Calendário
                NavigationBarItem(
                    selected = currentTab == DaylineNavTab.CALENDAR,
                    onClick = { viewModel.setTab(DaylineNavTab.CALENDAR) },
                    icon = {
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = "Calendário")
                    },
                    label = { Text("Calendário", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_calendar")
                )

                // Tab 3: Inbox
                NavigationBarItem(
                    selected = currentTab == DaylineNavTab.INBOX,
                    onClick = { viewModel.setTab(DaylineNavTab.INBOX) },
                    icon = {
                        if (unscheduledTasks.isNotEmpty()) {
                            BadgedBox(
                                badge = {
                                    Badge {
                                        Text(unscheduledTasks.size.toString())
                                    }
                                }
                            ) {
                                Icon(imageVector = Icons.Default.Inbox, contentDescription = "Inbox")
                            }
                        } else {
                            Icon(imageVector = Icons.Default.Inbox, contentDescription = "Inbox")
                        }
                    },
                    label = { Text("Inbox", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_inbox")
                )

                // Tab 4: Estatísticas
                NavigationBarItem(
                    selected = currentTab == DaylineNavTab.STATS,
                    onClick = { viewModel.setTab(DaylineNavTab.STATS) },
                    icon = {
                        Icon(imageVector = Icons.Default.BarChart, contentDescription = "Estatísticas")
                    },
                    label = { Text("Estatísticas", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_stats")
                )

                // Tab 5: Ajustes
                NavigationBarItem(
                    selected = currentTab == DaylineNavTab.SETTINGS,
                    onClick = { viewModel.setTab(DaylineNavTab.SETTINGS) },
                    icon = {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Ajustes")
                    },
                    label = { Text("Ajustes", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                DaylineNavTab.TIMELINE -> HomeScreen(viewModel = viewModel)
                DaylineNavTab.CALENDAR -> CalendarScreen(viewModel = viewModel)
                DaylineNavTab.INBOX -> InboxScreen(viewModel = viewModel)
                DaylineNavTab.STATS -> StatsScreen(viewModel = viewModel)
                DaylineNavTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
