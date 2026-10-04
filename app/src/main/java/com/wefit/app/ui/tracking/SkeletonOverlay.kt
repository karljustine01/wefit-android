package com.wefit.app.ui.tracking

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.wefit.app.tracking.PosePoint
import com.wefit.app.tracking.PoseSkeleton

/**
 * Draws the real-time skeleton (bones + joints) using actually-detected
 * landmark positions from PoseTrackingStrategy. Nothing here is
 * simulated — a bone only appears when both its endpoint joints were
 * detected with sufficient confidence that frame.
 */
@Composable
fun SkeletonOverlay(
    points: Map<Int, PosePoint>,
    modifier: Modifier = Modifier,
    mirror: Boolean = true
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        fun toOffset(p: PosePoint): Offset {
            val x = if (mirror) (1f - p.x) * w else p.x * w
            val y = p.y * h
            return Offset(x, y)
        }

        PoseSkeleton.CONNECTIONS.forEach { (a, b) ->
            val pa = points[a]
            val pb = points[b]
            if (pa != null && pb != null) {
                drawLine(
                    color = Color(0xFF00E5A0),
                    start = toOffset(pa),
                    end = toOffset(pb),
                    strokeWidth = 6f
                )
            }
        }

        points.values.forEach { p ->
            drawCircle(
                color = Color(0xFFFFC107),
                radius = 8f,
                center = toOffset(p)
            )
        }
    }
}