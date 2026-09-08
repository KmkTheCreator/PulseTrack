package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.ActivityEntity
import com.example.ui.MainViewModel
import com.example.ui.components.MetricCard
import com.example.ui.components.WeeklyBarChart
import java.util.Calendar

enum class TimePeriod(val displayName: String) {
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year"),
    ALL_TIME("All Time")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val activities by viewModel.allActivities.collectAsState()
    val settings by viewModel.userSettings.collectAsState()

    var selectedPeriodIndex by remember { mutableIntStateOf(0) }
    val periods = TimePeriod.entries

    val currentPeriod = periods[selectedPeriodIndex]

    val filteredActivities = remember(activities, currentPeriod) {
        val calendar = Calendar.getInstance()
        val cutoffTimestamp = when (currentPeriod) {
            TimePeriod.WEEK -> {
                calendar.apply {
                    firstDayOfWeek = Calendar.MONDAY
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
            }
            TimePeriod.MONTH -> {
                calendar.apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
            }
            TimePeriod.YEAR -> {
                calendar.apply {
                    set(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
            }
            TimePeriod.ALL_TIME -> 0L
        }

        activities.filter { it.startTimestamp >= cutoffTimestamp }
    }

    val totalDistanceMeters = remember(filteredActivities) { filteredActivities.sumOf { it.distanceMeters } }
    val totalDurationSeconds = remember(filteredActivities) { filteredActivities.sumOf { it.durationSeconds } }
    val totalMovingSeconds = remember(filteredActivities) { filteredActivities.sumOf { it.movingTimeSeconds } }
    val totalElevationGain = remember(filteredActivities) { filteredActivities.sumOf { it.elevationGainMeters } }
    val totalCalories = remember(filteredActivities) { filteredActivities.sumOf { it.calories } }

    val avgPaceSec = remember(totalDistanceMeters, totalMovingSeconds) {
        val km = totalDistanceMeters / 1000.0
        if (km > 0.05 && totalMovingSeconds > 0) totalMovingSeconds / km else 0.0
    }

    // Personal Records across ALL activities
    val longestRun = remember(activities) { activities.maxByOrNull { it.distanceMeters } }
    val longestDuration = remember(activities) { activities.maxByOrNull { it.durationSeconds } }
    val fastestActivity = remember(activities) {
        activities.filter { it.distanceMeters >= 1000 && it.avgPaceSecPerKm > 60 }
            .minByOrNull { it.avgPaceSecPerKm }
    }
    val highestElevation = remember(activities) { activities.maxByOrNull { it.elevationGainMeters } }

    val (distFormatted, distUnit) = viewModel.formatDistance(totalDistanceMeters, settings.unitSystem)
    val (paceFormatted, paceUnit) = viewModel.formatPace(avgPaceSec, settings.unitSystem)
    val (elevFormatted, elevUnit) = viewModel.formatElevation(totalElevationGain, settings.unitSystem)

    val weeklyBars = remember(activities) { viewModel.getWeeklyDistances(activities) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Performance Statistics",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("stats_back_button")) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Period Selector Tabs
            item {
                TabRow(
                    selectedTabIndex = selectedPeriodIndex,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedPeriodIndex]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    periods.forEachIndexed { index, period ->
                        Tab(
                            selected = selectedPeriodIndex == index,
                            onClick = { selectedPeriodIndex = index },
                            text = {
                                Text(
                                    text = period.displayName,
                                    fontWeight = if (selectedPeriodIndex == index) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.testTag("tab_period_${period.name.lowercase()}")
                        )
                    }
                }
            }

            // Period Summary Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${currentPeriod.displayName.uppercase()} TOTAL DISTANCE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$distFormatted $distUnit",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${filteredActivities.size} Workouts Recorded",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Metrics Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Moving Time",
                        value = viewModel.formatDuration(totalMovingSeconds),
                        unit = "",
                        icon = Icons.Default.Timer,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Avg Pace",
                        value = paceFormatted,
                        unit = paceUnit,
                        icon = Icons.Default.Speed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Elev Gain",
                        value = "+$elevFormatted",
                        unit = elevUnit,
                        icon = Icons.Default.Height,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Calories",
                        value = "$totalCalories",
                        unit = "kcal",
                        icon = Icons.Default.LocalFireDepartment,
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Activity Breakdown Chart
            item {
                WeeklyBarChart(
                    weeklyDistancesKm = weeklyBars,
                    barColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Personal Records / Trophies Section
            item {
                Text(
                    text = "PERSONAL RECORDS (ALL-TIME)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                PersonalRecordCard(
                    title = "Longest Distance",
                    value = longestRun?.let {
                        val d = viewModel.formatDistance(it.distanceMeters, settings.unitSystem)
                        "${d.first} ${d.second} • ${it.title}"
                    } ?: "No records yet",
                    icon = Icons.Default.Straighten,
                    badgeColor = Color(0xFFFFB703)
                )
            }

            item {
                PersonalRecordCard(
                    title = "Fastest Pace",
                    value = fastestActivity?.let {
                        val p = viewModel.formatPace(it.avgPaceSecPerKm, settings.unitSystem)
                        "${p.first} ${p.second} • ${it.title}"
                    } ?: "No records yet",
                    icon = Icons.Default.Speed,
                    badgeColor = Color(0xFF00E5FF)
                )
            }

            item {
                PersonalRecordCard(
                    title = "Longest Duration",
                    value = longestDuration?.let {
                        "${viewModel.formatDuration(it.durationSeconds)} • ${it.title}"
                    } ?: "No records yet",
                    icon = Icons.Default.Timer,
                    badgeColor = Color(0xFF10B981)
                )
            }

            item {
                PersonalRecordCard(
                    title = "Highest Elevation Gain",
                    value = highestElevation?.let {
                        val e = viewModel.formatElevation(it.elevationGainMeters, settings.unitSystem)
                        "+${e.first} ${e.second} • ${it.title}"
                    } ?: "No records yet",
                    icon = Icons.Default.Height,
                    badgeColor = Color(0xFFFF4D6D)
                )
            }
        }
    }
}

@Composable
fun PersonalRecordCard(
    title: String,
    value: String,
    icon: ImageVector,
    badgeColor: Color
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
