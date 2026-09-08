package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActivityType
import com.example.ui.MainViewModel
import com.example.ui.components.DataSeriesChart
import com.example.ui.components.MetricCard
import com.example.ui.components.PulseTrackMap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailScreen(
    activityId: Long,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activityWithPointsState = remember(activityId) { viewModel.getActivityWithPoints(activityId) }
    val activityWithPoints by activityWithPointsState.collectAsState()
    val settings by viewModel.userSettings.collectAsState()

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var chartTabIndex by remember { mutableIntStateOf(0) }

    val act = activityWithPoints?.activity
    val points = activityWithPoints?.points ?: emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = act?.title ?: "Workout Details",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (act != null) {
                                val distFormatted = viewModel.formatDistance(act.distanceMeters, settings.unitSystem)
                                val paceFormatted = viewModel.formatPace(act.avgPaceSecPerKm, settings.unitSystem)
                                val timeFormatted = viewModel.formatDuration(act.durationSeconds)

                                val shareText = """
                                    ⚡ PulseTrack Workout
                                    Activity: ${act.title}
                                    Distance: ${distFormatted.first} ${distFormatted.second}
                                    Time: $timeFormatted
                                    Avg Pace: ${paceFormatted.first} ${paceFormatted.second}
                                    Elevation Gain: +${act.elevationGainMeters.toInt()}m
                                    Calories: ${act.calories} kcal
                                    Tracked with PulseTrack GPS
                                """.trimIndent()

                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Workout"))
                            }
                        },
                        modifier = Modifier.testTag("detail_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.testTag("detail_edit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Title",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("detail_delete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
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
        if (act == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading workout...")
            }
            return@Scaffold
        }

        val activityType = ActivityType.fromString(act.type)
        val (distFormatted, distUnit) = viewModel.formatDistance(act.distanceMeters, settings.unitSystem)
        val (avgPaceFormatted, paceUnit) = viewModel.formatPace(act.avgPaceSecPerKm, settings.unitSystem)
        val (fastestPaceFormatted, _) = viewModel.formatPace(act.fastestPaceSecPerKm, settings.unitSystem)
        val (speedFormatted, speedUnit) = viewModel.formatSpeed(act.avgSpeedKmh, settings.unitSystem)
        val (maxSpeedFormatted, _) = viewModel.formatSpeed(act.maxSpeedKmh, settings.unitSystem)
        val (elevationGainFormatted, elevationUnit) = viewModel.formatElevation(act.elevationGainMeters, settings.unitSystem)
        val (elevationLossFormatted, _) = viewModel.formatElevation(act.elevationLossMeters, settings.unitSystem)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Interactive Map of Route
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    PulseTrackMap(
                        modifier = Modifier.fillMaxSize(),
                        points = points,
                        fitBounds = true,
                        isLiveTracking = false,
                        polylineColor = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Header Info & Optional Notes
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = activityType.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = act.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = viewModel.formatDate(act.startTimestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (act.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "\"${act.notes}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            // Primary Metrics Grid
            item {
                Text(
                    text = "PERFORMANCE METRICS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Distance",
                        value = distFormatted,
                        unit = distUnit,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Duration",
                        value = viewModel.formatDuration(act.durationSeconds),
                        unit = "",
                        accentColor = MaterialTheme.colorScheme.secondary,
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
                        title = "Avg Pace",
                        value = avgPaceFormatted,
                        unit = paceUnit,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Avg Speed",
                        value = speedFormatted,
                        unit = speedUnit,
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
                        value = "+$elevationGainFormatted",
                        unit = elevationUnit,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Calories",
                        value = "${act.calories}",
                        unit = "kcal",
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Charts Section: Elevation, Speed, Pace
            item {
                Text(
                    text = "DATA PROFILES",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                TabRow(
                    selectedTabIndex = chartTabIndex,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[chartTabIndex]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    Tab(
                        selected = chartTabIndex == 0,
                        onClick = { chartTabIndex = 0 },
                        text = { Text("Elevation") }
                    )
                    Tab(
                        selected = chartTabIndex == 1,
                        onClick = { chartTabIndex = 1 },
                        text = { Text("Speed") }
                    )
                    Tab(
                        selected = chartTabIndex == 2,
                        onClick = { chartTabIndex = 2 },
                        text = { Text("Pace") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (chartTabIndex) {
                    0 -> {
                        DataSeriesChart(
                            title = "Elevation Profile",
                            unit = elevationUnit,
                            points = points,
                            dataSelector = { it.altitude },
                            lineColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    1 -> {
                        DataSeriesChart(
                            title = "Speed Profile",
                            unit = speedUnit,
                            points = points,
                            dataSelector = { (it.speed * 3.6).toDouble() },
                            lineColor = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    2 -> {
                        DataSeriesChart(
                            title = "Pace Profile",
                            unit = paceUnit,
                            points = points,
                            dataSelector = {
                                if (it.speed > 0.5f) (1000f / it.speed / 60f).toDouble() else 0.0
                            },
                            lineColor = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Complete Diagnostics & Time breakdown
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "EXTENDED DETAILS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        DetailRow("Moving Time", viewModel.formatDuration(act.movingTimeSeconds))
                        DetailRow("Stopped Time", viewModel.formatDuration(act.stoppedTimeSeconds))
                        DetailRow("Fastest Pace", "$fastestPaceFormatted $paceUnit")
                        DetailRow("Max Speed", "$maxSpeedFormatted $speedUnit")
                        DetailRow("Elevation Gain", "+$elevationGainFormatted $elevationUnit")
                        DetailRow("Elevation Loss", "-$elevationLossFormatted $elevationUnit")
                        DetailRow("Start Time", viewModel.formatDate(act.startTimestamp))
                        DetailRow("End Time", viewModel.formatDate(act.endTimestamp))
                        DetailRow("GPS Points Logged", "${points.size}")
                    }
                }
            }
        }
    }

    // Delete Confirmation
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Activity?") },
            text = { Text("Are you sure you want to permanently delete this workout? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteActivity(activityId)
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Title/Notes Dialog
    if (showEditDialog && act != null) {
        var editTitle by remember { mutableStateOf(act.title) }
        var editNotes by remember { mutableStateOf(act.notes) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Activity") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEditDialog = false
                        viewModel.updateActivity(act.copy(title = editTitle, notes = editNotes))
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
