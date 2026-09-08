package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.data.db.GpsPointEntity

@Composable
fun RouteCanvasPreview(
    points: List<GpsPointEntity>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
    ) {
        if (points.size < 2) {
            // Placeholder subtle wave if no GPS points recorded yet
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val path = Path().apply {
                    moveTo(w * 0.1f, h * 0.5f)
                    cubicTo(w * 0.35f, h * 0.2f, w * 0.65f, h * 0.8f, w * 0.9f, h * 0.5f)
                }
                drawPath(
                    path = path,
                    color = lineColor.copy(alpha = 0.3f),
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )
            }
            return@Box
        }

        // Calculate bounding box for normalization
        val minLat = remember(points) { points.minOf { it.latitude } }
        val maxLat = remember(points) { points.maxOf { it.latitude } }
        val minLng = remember(points) { points.minOf { it.longitude } }
        val maxLng = remember(points) { points.maxOf { it.longitude } }

        val latSpan = (maxLat - minLat).coerceAtLeast(0.0001)
        val lngSpan = (maxLng - minLng).coerceAtLeast(0.0001)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val padding = 20f
            val drawWidth = size.width - (padding * 2)
            val drawHeight = size.height - (padding * 2)

            val path = Path()
            var startOffset = Offset.Zero
            var endOffset = Offset.Zero

            points.forEachIndexed { index, pt ->
                // Note: latitude increases upwards (Y decreases)
                val x = padding + ((pt.longitude - minLng) / lngSpan * drawWidth).toFloat()
                val y = padding + ((maxLat - pt.latitude) / latSpan * drawHeight).toFloat()

                if (index == 0) {
                    path.moveTo(x, y)
                    startOffset = Offset(x, y)
                } else {
                    path.lineTo(x, y)
                }
                if (index == points.lastIndex) {
                    endOffset = Offset(x, y)
                }
            }

            // Draw route line
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(
                    width = 6f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Start Dot (Green)
            drawCircle(
                color = Color(0xFF10B981),
                radius = 7f,
                center = startOffset
            )

            // End Dot (Cyan or Red)
            drawCircle(
                color = Color(0xFFFF4D6D),
                radius = 7f,
                center = endOffset
            )
        }
    }
}
