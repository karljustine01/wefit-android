package com.wefit.app.ui.tracking

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage

@Composable
fun DemoMedia(type: String, url: String, modifier: Modifier = Modifier) {
    if (type == "image") {
        AsyncImage(
            model = url,
            contentDescription = "Exercise demonstration",
            contentScale = ContentScale.Fit,
            modifier = modifier
                .fillMaxWidth()
                .heightIn(max = 260.dp)
                .clip(RoundedCornerShape(12.dp))
        )
    } else {
        val context = LocalContext.current
        val videoView = remember { VideoView(context) }
        DisposableEffect(Unit) { onDispose { videoView.stopPlayback() } }
        AndroidView(
            factory = {
                videoView.apply {
                    val controller = MediaController(context)
                    controller.setAnchorView(this)
                    setMediaController(controller)
                    setVideoURI(Uri.parse(url))
                    setOnPreparedListener { it.seekTo(100) } // shows the first frame instead of black
                }
            },
            modifier = modifier.fillMaxWidth().height(240.dp)
        )
    }
}