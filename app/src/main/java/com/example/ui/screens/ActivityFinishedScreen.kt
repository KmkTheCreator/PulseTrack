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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.ui.components.MetricCard
import com.example.ui.components.PulseTrackMap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityFinishedScreen(
    activityId: Long,
    viewModel: MainViewModel,
    onSavedOrDone: () -> Unit
) {
    val context = LocalContext.current
    val activityWithPointsState = remember(activityId) { viewModel.getActivityWithPoints(activityId) }
    val activityWithPoints by activityWithPointsState.collectAsState()
    val settings by viewModel.userSettings.collectAsState()

    var titleText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var isInitialized by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val act = activityWithPoints?.activity
    val points = activityWithPoints?.points ?: emptyList()

    if (act != null && !isInitialized) {
        titleText = act.title
        notesText = act.notes
        isInitialized = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Activity Completed",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("delete_finished_activity_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Activity",
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
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Share Summary Button
                    OutlinedButton(
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
                                    Elevation: +${act.elevationGainMeters.toInt()}m
                                    Calories: ${act.calories} kcal
                                    Tracked with PulseTrack GPS
                                """.trimIndent()

                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Workout Summary"))
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("share_summary_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }

                    // Save / Done Button
                    Button(
                        onClick = {
                            if (act != null) {
                                viewModel.updateActivity(act.copy(title = titleText, notes = notesText))
                            }
                            onSavedOrDone()
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(56.dp)
                            .testTag("save_finished_activity_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Save Activity",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
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
                Text("Loading workout data...")
            }
            return@Scaffold
        }

        val activityType = ActivityType.fromString(act.type)
        val (distFormatted, distUnit) = viewModel.formatDistance(act.distanceMeters, settings.unitSystem)
        val (avgPaceFormatted, paceUnit) = viewModel.formatPace(act.avgPaceSecPerKm, settings.unitSystem)
        val (fastestPaceFormatted, _) = viewModel.formatPace(act.fastestPaceSecPerKm, settings.unitSystem)
        val (speedFormatted, speedUnit) = viewModel.formatSpeed(act.avgSpeedKmh, settings.unitSystem)
        val (maxSpeedFormatted, _) = viewModel.formatSpeed(act.maxSpeedKmh, settings.unitSystem)
        val (elevationFormatted, elevationUnit) = viewModel.formatElevation(act.elevationGainMeters, settings.unitSystem)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Map Snapshot
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    PulseTrackMap(
                        modifier = Modifier.fillMaxSize(),
                        points = points,
                        fitBounds = true,
                        isLiveTracking = false
                    )
                }
            }

            // Title & Notes Input Form
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = activityType.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Workout Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = titleText,
                            onValueChange = { titleText = it },
                            label = { Text("Activity Title") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("finished_title_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = notesText,
                            onValueChange = { notesText = it },
                            label = { Text("How did it feel? (Optional Notes)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("finished_notes_input"),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 2
                        )
                    }
                }
            }

            // Key Summary Statistics Grid
            item {
                Text(
                    text = "WORKOUT SUMMARY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
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
                        title = "Total Time",
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
                        title = "Elevation Gain",
                        value = "+$elevationFormatted",
                        unit = elevationUnit,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Est. Calories",
                        value = "${act.calories}",
                        unit = "kcal",
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Detailed Timing Breakdown
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
                            text = "TIME & PACE BREAKDOWN",
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
                        DetailRow("Started At", viewModel.formatDate(act.startTimestamp))
                        DetailRow("Finished At", viewModel.formatDate(act.endTimestamp))
                        DetailRow("GPS Trackpoints", "${points.size} points logged")
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Activity?") },
            text = { Text("This will permanently remove this recorded workout and its GPS route.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteActivity(activityId)
                        onSavedOrDone()
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
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
