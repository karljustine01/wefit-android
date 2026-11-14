package com.wefit.app.tracking

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/**
 * Records the student while they exercise so the teacher can verify who did it.
 *
 * COMPRESSION happens at capture time, on the phone's hardware H.264 encoder:
 *   - 480p (SD) instead of 1080p
 *   - ~600 kbps video bitrate (≈ 4.5 MB per minute instead of ~30 MB)
 *   - NO audio track (smaller + no microphone permission / privacy issue)
 *   - hard cap of [MAX_DURATION_MS]
 * Nothing is re-encoded afterwards, so there is no extra CPU/battery cost and the
 * Railway server never has to transcode anything.
 */
object SessionVideoRecorder {

    private const val TAG = "WeFitVideo"
    const val MAX_DURATION_MS = 120_000L          // 2 min  -> ~9 MB at 600 kbps
    private const val VIDEO_BITRATE_BPS = 600_000

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var finished: CompletableDeferred<File?>? = null
    private var startedAtMs = 0L

    /** Seconds recorded by the last session (valid after [stopAndAwait]). */
    var lastDurationSeconds: Int = 0
        private set

    /**
     * Drop-in replacement for provider.bindToLifecycle(owner, selector, preview, analysis).
     * Tries to add the video use case; if the device can't run
     * preview + analysis + video together, falls back to the original bindings so
     * rep counting NEVER breaks because of recording.
     */
    fun bind(
        provider: ProcessCameraProvider,
        owner: LifecycleOwner,
        selector: CameraSelector,
        preview: Preview,
        analysis: ImageAnalysis
    ) {
        val capture = buildVideoCapture()
        try {
            provider.bindToLifecycle(owner, selector, preview, analysis, capture)
            videoCapture = capture
            Log.d(TAG, "Bound with video capture")
        } catch (e: Exception) {
            Log.w(TAG, "Video use case not supported with this device combo, tracking only", e)
            videoCapture = null
            provider.unbindAll()
            provider.bindToLifecycle(owner, selector, preview, analysis)
        }
    }

    private fun buildVideoCapture(): VideoCapture<Recorder> {
        val recorder = Recorder.Builder()
            .setQualitySelector(
                QualitySelector.from(
                    Quality.SD,
                    FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
                )
            )
            .setTargetVideoEncodingBitRate(VIDEO_BITRATE_BPS)
            .build()
        return VideoCapture.withOutput(recorder)
    }

    /** Begin recording. Returns false if the device could not bind a video use case. */
    @SuppressLint("MissingPermission") // audio is never enabled
    fun start(context: Context): Boolean {
        val capture = videoCapture ?: return false
        if (recording != null) return true

        val dir = File(context.filesDir, "session_videos").apply { mkdirs() }
        // Remove leftovers from crashed/old sessions (> 3 days).
        dir.listFiles()?.forEach {
            if (System.currentTimeMillis() - it.lastModified() > 3L * 24 * 3600 * 1000) it.delete()
        }

        val file = File(dir, "session_${System.currentTimeMillis()}.mp4")
        val options = FileOutputOptions.Builder(file)
            .setDurationLimitMillis(MAX_DURATION_MS)
            .build()

        val done = CompletableDeferred<File?>()
        finished = done
        startedAtMs = System.currentTimeMillis()

        recording = capture.output
            .prepareRecording(context, options) // no .withAudioEnabled() on purpose
            .start(ContextCompat.getMainExecutor(context)) { event ->
                if (event is VideoRecordEvent.Finalize) {
                    recording = null
                    val benign = !event.hasError() ||
                            event.error == VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED ||
                            event.error == VideoRecordEvent.Finalize.ERROR_FILE_SIZE_LIMIT_REACHED
                    if (benign && file.exists() && file.length() > 0) {
                        done.complete(file)
                    } else {
                        Log.w(TAG, "Recording failed, error=${event.error}")
                        file.delete()
                        done.complete(null)
                    }
                }
            }
        return true
    }

    val isRecording: Boolean get() = recording != null

    /** Stops recording and waits for the MP4 to be finalized. Call BEFORE the camera is unbound. */
    suspend fun stopAndAwait(): File? {
        val done = finished ?: return null
        recording?.stop()
        val file = withTimeoutOrNull(8_000) { done.await() }
        lastDurationSeconds = ((System.currentTimeMillis() - startedAtMs) / 1000)
            .toInt().coerceAtMost((MAX_DURATION_MS / 1000).toInt())
        finished = null
        recording = null
        return file
    }

    /** Student left the screen / retook: throw the unfinished clip away. */
    fun discard() {
        val active = recording
        val done = finished
        recording = null
        finished = null
        if (active != null) {
            active.stop()
            // Finalize callback deletes nothing on success, so remove the file once it arrives.
            done?.invokeOnCompletion { runCatching { done.getCompleted()?.delete() } }
        }
    }
}
