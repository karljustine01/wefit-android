package com.wefit.app.ui.assignments

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.wefit.app.data.repository.SessionVideoRepository
import com.wefit.app.ui.tracking.DemoMedia
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
                    else -> DemoMedia(type = "video", url = Uri.fromFile(file).toString())
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
