package com.wefit.app.data.local

import android.content.Context
import com.wefit.app.data.local.database.WeFitDatabase
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * On-device data recycling. Keeps the phone lean:
 *  - Room rows that are already synced (or cancelled) are dropped after [SESSION_DAYS].
 *  - Recorded proof videos that were never uploaded are dropped after [VIDEO_DAYS]
 *    (the server only keeps videos for a short time anyway).
 */
object LocalRecycler {
    private const val SESSION_DAYS = 30L
    private const val VIDEO_DAYS = 7L

    suspend fun run(context: Context) {
        try {
            val now = System.currentTimeMillis()
            WeFitDatabase.getInstance(context).exerciseSessionDao()
                .deleteOlderThan(now - TimeUnit.DAYS.toMillis(SESSION_DAYS))

            val videoCutoff = now - TimeUnit.DAYS.toMillis(VIDEO_DAYS)
            File(context.filesDir, "session_videos").listFiles()?.forEach { f ->
                if (f.isFile && f.lastModified() < videoCutoff) f.delete()
            }
        } catch (_: Exception) {
            // Cleanup is best-effort and must never break syncing.
        }
    }
}
