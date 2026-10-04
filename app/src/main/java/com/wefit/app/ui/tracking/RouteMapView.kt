package com.wefit.app.ui.tracking

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.Marker

/**
 * Free, no-API-key live route map using OpenStreetMap tiles via osmdroid.
 * Draws the actual GPS points recorded during the session as a polyline,
 * with a marker at the most recent position.
 */
@Composable
fun RouteMapView(
    points: List<Pair<Double, Double>>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = context.packageName
    }

    val mapView = remember { MapView(context) }

    DisposableEffect(Unit) {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(17.0)
        onDispose { mapView.onDetach() }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            view.overlays.clear()

            if (points.isNotEmpty()) {
                val geoPoints = points.map { GeoPoint(it.first, it.second) }

                val polyline = Polyline().apply {
                    setPoints(geoPoints)
                    outlinePaint.color = android.graphics.Color.parseColor("#00C853")
                    outlinePaint.strokeWidth = 8f
                }
                view.overlays.add(polyline)

                val latest = geoPoints.last()
                val marker = Marker(view).apply {
                    position = latest
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "Current position"
                }
                view.overlays.add(marker)

                view.controller.setCenter(latest)
            }

            view.invalidate()
        }
    )
}

@Composable
private fun remember(calculation: () -> MapView): MapView {
    return androidx.compose.runtime.remember { calculation() }
}