package me.rpgz.treetools.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt
import kotlin.ranges.coerceIn

data class LineSegmentPoints(
    val startPoint: Offset = Offset.Zero,
    val endPoint: Offset = Offset.Zero
)

@Composable
fun BitmapLineSegmentPicker(
    bitmap: Bitmap,
    onPointsChanged: (LineSegmentPoints) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val initialStartPoint = Offset(100f, 100f)
    val initialEndPoint = Offset(200f, 200f)
    var startPoint by remember {
        mutableStateOf(initialStartPoint)
    }
    var endPoint by remember {
        mutableStateOf(initialEndPoint)
    }
    var draggedPoint by remember { mutableStateOf<String?>(null) }

    val handleRadius = with(density) { 12.dp.toPx() }
    val lineColor = MaterialTheme.colorScheme.primary
    val handleColor = MaterialTheme.colorScheme.secondary

    // Calculate scaling factors for coordinate transformation
    val scaleX = if (canvasSize.width > 0) canvasSize.width.toFloat() / bitmap.width.toFloat() else 1f
    val scaleY = if (canvasSize.height > 0) canvasSize.height.toFloat() / bitmap.height.toFloat() else 1f
    val scale = minOf(scaleX, scaleY)

    val scaledBitmapWidth = bitmap.width * scale
    val scaledBitmapHeight = bitmap.height * scale
    val offsetX = (canvasSize.width - scaledBitmapWidth) / 2f
    val offsetY = (canvasSize.height - scaledBitmapHeight) / 2f

    // Transform points from bitmap coordinates to display coordinates
    fun bitmapToDisplay(point: Offset): Offset {
        return Offset(
            point.x * scale + offsetX,
            point.y * scale + offsetY
        )
    }

    // Transform points from display coordinates to bitmap coordinates
    fun displayToBitmap(point: Offset): Offset {
        return Offset(
            ((point.x - offsetX) / scale).coerceIn(0f, bitmap.width.toFloat() - 1f),
            ((point.y - offsetY) / scale).coerceIn(0f, bitmap.height.toFloat() - 1f)
        )
    }

    // Notify parent of point changes (in bitmap coordinates)
    LaunchedEffect(startPoint, endPoint) {
        onPointsChanged(LineSegmentPoints(startPoint, endPoint))
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = {
                startPoint = initialStartPoint
                endPoint = initialEndPoint
            },
            modifier = Modifier.padding(8.dp)
        ) {
            Text("重置点位置")
        }

        Box(modifier = Modifier.fillMaxSize()) {
        // Background bitmap
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Measurement image",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { canvasSize = it }
        )

        // Overlay for line and draggable points
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            // Convert display coordinates to bitmap coordinates for comparison
                            val displayStart = bitmapToDisplay(startPoint)
                            val displayEnd = bitmapToDisplay(endPoint)

                            val distanceToStart = sqrt(
                                (offset.x - displayStart.x) * (offset.x - displayStart.x) +
                                        (offset.y - displayStart.y) * (offset.y - displayStart.y)
                            )
                            val distanceToEnd = sqrt(
                                (offset.x - displayEnd.x) * (offset.x - displayEnd.x) +
                                        (offset.y - displayEnd.y) * (offset.y - displayEnd.y)
                            )

                            draggedPoint = when {
                                distanceToStart <= handleRadius -> "start"
                                distanceToEnd <= handleRadius -> "end"
                                else -> null
                            }
                        },
                        onDrag = { _, dragAmount ->
                            when (draggedPoint) {
                                "start" -> {
                                    val displayPoint = bitmapToDisplay(startPoint)
                                    val newDisplayPoint = Offset(
                                        displayPoint.x + dragAmount.x,
                                        displayPoint.y + dragAmount.y
                                    )
                                    startPoint = displayToBitmap(newDisplayPoint)
                                }
                                "end" -> {
                                    val displayPoint = bitmapToDisplay(endPoint)
                                    val newDisplayPoint = Offset(
                                        displayPoint.x + dragAmount.x,
                                        displayPoint.y + dragAmount.y
                                    )
                                    endPoint = displayToBitmap(newDisplayPoint)
                                }
                            }
                        },
                        onDragEnd = {
                            draggedPoint = null
                        }
                    )
                }
        ) {
            if (canvasSize != IntSize.Zero) {
                drawLineSegmentOverlay(
                    startPoint = bitmapToDisplay(startPoint),
                    endPoint = bitmapToDisplay(endPoint),
                    handleRadius = handleRadius,
                    lineColor = lineColor,
                    handleColor = handleColor
                )
            }
        }
        }
    }
}

private fun DrawScope.drawLineSegmentOverlay(
    startPoint: Offset,
    endPoint: Offset,
    handleRadius: Float,
    lineColor: Color,
    handleColor: Color
) {
    // Draw the line segment
    drawLine(
        color = lineColor,
        start = startPoint,
        end = endPoint,
        strokeWidth = 3.dp.toPx()
    )

    // Draw start point handle
    drawCircle(
        color = handleColor,
        radius = handleRadius,
        center = startPoint
    )

    // Draw start point handle border
    drawCircle(
        color = Color.White,
        radius = handleRadius,
        center = startPoint,
        style = Stroke(width = 2.dp.toPx())
    )

    // Draw end point handle
    drawCircle(
        color = handleColor,
        radius = handleRadius,
        center = endPoint
    )

    // Draw end point handle border
    drawCircle(
        color = Color.White,
        radius = handleRadius,
        center = endPoint,
        style = Stroke(width = 2.dp.toPx())
    )
}