package com.wefit.app.ui.assignments

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.wefit.app.data.repository.SessionVideoRepository
import kotlinx.coroutines.delay
import java.io.File

/** Teacher side: downloads the student's compressed clip and plays it so identity can be verified. */
@Composable
fun VideoReviewDialog(sessionId: Int, studentName: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var file by remember { mutableStateOf<File?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(sessionId) {
        SessionVideoRepository(context).download(sessionId)
            .onSuccess { file = it }
            .onFailure { error = it.message ?: "Could not load video." }
    }
    DisposableEffect(sessionId) { onDispose { file?.delete() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$studentName's video") },
        text = {
            Box(Modifier.fillMaxWidth().heightIn(min = 120.dp), contentAlignment = Alignment.Center) {
                when {
                    error != null -> Text(error ?: "", color = MaterialTheme.colorScheme.error)
                    file == null -> CircularProgressIndicator()
                    else -> ReviewVideoPlayer(Uri.fromFile(file))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

/**
 * Player with its own Compose controls.
 *
 * The old version used android.widget.MediaController, which opens in a separate window
 * BEHIND the AlertDialog window -> no play button was ever visible and nothing called
 * start(), so the teacher only saw the first frame (looked like a picture).
 */
@Composable
private fun ReviewVideoPlayer(uri: Uri) {
    val context = LocalContext.current
    val videoView = remember { VideoView(context) }
    var isPlaying by remember { mutableStateOf(false) }
    var ended by remember { mutableStateOf(false) }
    var prepared by remember { mutableStateOf(false) }
    var position by remember { mutableStateOf(0f) }
    var duration by remember { mutableStateOf(1) }
    var playbackError by remember { mutableStateOf(false) }

    DisposableEffect(Unit) { onDispose { videoView.stopPlayback() } }

    // Keep the slider in sync while playing.
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            position = videoView.currentPosition.toFloat()
            delay(200)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AndroidView(
            factory = {
                videoView.apply {
                    setOnPreparedListener { mp ->
                        duration = mp.duration.coerceAtLeast(1)
                        prepared = true
                        start()               // autoplay so it is obviously a video
                        isPlaying = true
                    }
                    setOnCompletionListener { isPlaying = false; ended = true; position = duration.toFloat() }
                    setOnErrorListener { _, _, _ -> playbackError = true; true }
                    setVideoURI(uri)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(12.dp))
        )

        if (playbackError) {
            Text("This video can't be played on this device.", color = MaterialTheme.colorScheme.error)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    enabled = prepared,
                    onClick = {
                        when {
                            ended -> { videoView.seekTo(0); videoView.start(); ended = false; isPlaying = true }
                            videoView.isPlaying -> { videoView.pause(); isPlaying = false }
                            else -> { videoView.start(); isPlaying = true }
                        }
                    }
                ) {
                    Icon(
                        when {
                            ended -> Icons.Filled.Replay
                            isPlaying -> Icons.Filled.Pause
                            else -> Icons.Filled.PlayArrow
                        },
                        contentDescription = "Play / pause"
                    )
                }
                Slider(
                    value = position.coerceIn(0f, duration.toFloat()),
                    onValueChange = { position = it; videoView.seekTo(it.toInt()) },
                    valueRange = 0f..duration.toFloat(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
