package com.example.ui.components

import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.db.GpsPointEntity
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun PulseTrackMap(
    modifier: Modifier = Modifier,
    points: List<GpsPointEntity> = emptyList(),
    currentLat: Double? = null,
    currentLng: Double? = null,
    isLiveTracking: Boolean = false,
    fitBounds: Boolean = false,
    polylineColor: Color = Color(0xFF00E5FF)
) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(16.5)
            // Default center if no GPS yet
            controller.setCenter(GeoPoint(37.7749, -122.4194))
        }
    }

    var followUser by remember { mutableStateOf(isLiveTracking) }
    var tileSourceIndex by remember { mutableIntStateOf(0) }

    val tileSources = remember {
        listOf(
            TileSourceFactory.MAPNIK,
            TileSourceFactory.OpenTopo
        )
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
        }
    }

    // Update map overlays whenever points or current location updates
    LaunchedEffect(points.size, currentLat, currentLng) {
        mapView.overlays.clear()

        val geoPoints = points.map { GeoPoint(it.latitude, it.longitude) }

        // Draw Route Polyline
        if (geoPoints.size >= 2) {
            val polyline = Polyline(mapView).apply {
                outlinePaint.color = polylineColor.toArgb()
                outlinePaint.strokeWidth = 12f
                outlinePaint.strokeCap = Paint.Cap.ROUND
                outlinePaint.strokeJoin = Paint.Join.ROUND
                outlinePaint.isAntiAlias = true
                setPoints(geoPoints)
            }
            mapView.overlays.add(polyline)
        }

        // Start Marker (Green Pin)
        if (geoPoints.isNotEmpty()) {
            val startPoint = geoPoints.first()
            val startMarker = Marker(mapView).apply {
                position = startPoint
                title = "Start"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            mapView.overlays.add(startMarker)
        }

        // Finish / End Marker (if not live tracking or finished)
        if (!isLiveTracking && geoPoints.size > 1) {
            val finishPoint = geoPoints.last()
            val finishMarker = Marker(mapView).apply {
                position = finishPoint
                title = "Finish"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            mapView.overlays.add(finishMarker)
        }

        // Current GPS location puck (if live tracking or current location known)
        if (currentLat != null && currentLng != null) {
            val currentGeo = GeoPoint(currentLat, currentLng)
            val currentMarker = Marker(mapView).apply {
                position = currentGeo
                title = "Current Position"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            }
            mapView.overlays.add(currentMarker)

            if (followUser) {
                mapView.controller.animateTo(currentGeo)
            }
        }

        // Auto zoom-fit route if requested
        if (fitBounds && geoPoints.size >= 2) {
            try {
                val boundingBox = BoundingBox.fromGeoPoints(geoPoints)
                mapView.zoomToBoundingBox(boundingBox, true, 80)
            } catch (_: Exception) {}
        } else if (currentLat != null && currentLng != null && points.isEmpty()) {
            mapView.controller.setCenter(GeoPoint(currentLat, currentLng))
        }

        mapView.invalidate()
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        // Floating Map Controls Overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Layer Toggle
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                tonalElevation = 4.dp,
                shadowElevation = 4.dp
            ) {
                IconButton(
                    onClick = {
                        tileSourceIndex = (tileSourceIndex + 1) % tileSources.size
                        mapView.setTileSource(tileSources[tileSourceIndex])
                    },
                    modifier = Modifier.testTag("map_layer_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Change map layer",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Follow User / Re-center Button
            Surface(
                shape = CircleShape,
                color = if (followUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                tonalElevation = 4.dp,
                shadowElevation = 4.dp
            ) {
                IconButton(
                    onClick = {
                        followUser = !followUser
                        if (currentLat != null && currentLng != null) {
                            mapView.controller.animateTo(GeoPoint(currentLat, currentLng))
                        }
                    },
                    modifier = Modifier.testTag("map_recenter_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Recenter on my location",
                        tint = if (followUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Zoom In Button
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                tonalElevation = 4.dp,
                shadowElevation = 4.dp
            ) {
                IconButton(
                    onClick = { mapView.controller.zoomIn() },
                    modifier = Modifier.testTag("map_zoom_in_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Zoom Out Button
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                tonalElevation = 4.dp,
                shadowElevation = 4.dp
            ) {
                IconButton(
                    onClick = { mapView.controller.zoomOut() },
                    modifier = Modifier.testTag("map_zoom_out_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
