package com.wefit.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.wefit.app.data.remote.api.AssignmentDetailsApi
import com.wefit.app.data.remote.api.RetrofitClient
import com.wefit.app.data.remote.dto.AssignmentDetailsDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.ByteArrayOutputStream

class AssignmentDetailsRepository(private val context: Context) {

    private val api = RetrofitClient.getInstance(context).create(AssignmentDetailsApi::class.java)

    suspend fun get(assignmentId: Int): Result<AssignmentDetailsDto> = try {
        val r = api.get(assignmentId)
        if (r.success && r.data != null) Result.success(r.data) else Result.failure(Exception(r.message))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun save(assignmentId: Int, instructions: String?, mediaUri: Uri?): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val text = instructions?.takeIf { it.isNotBlank() }?.toRequestBody("text/plain".toMediaType())
                val media = mediaUri?.let { buildMediaPart(it) }
                if (text == null && media == null) return@withContext Result.success(Unit)
                val r = api.save(assignmentId, text, media)
                if (r.success) Result.success(Unit) else Result.failure(Exception(r.message))
            } catch (e: HttpException) {
                Result.failure(Exception("Server rejected the upload (HTTP ${e.code()})"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun buildMediaPart(uri: Uri): MultipartBody.Part {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "application/octet-stream"

        return if (mime.startsWith("video/")) {
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalArgumentException("Could not read the video")
            if (bytes.size > 10 * 1024 * 1024) {
                throw IllegalArgumentException("Video is larger than 10 MB. Trim or compress it first.")
            }
            val ext = if (mime.contains("webm")) "webm" else "mp4"
            MultipartBody.Part.createFormData("media", "demo.$ext", bytes.toRequestBody(mime.toMediaType()))
        } else {
            val bytes = compressImage(uri) ?: throw IllegalArgumentException("Could not read the image")
            MultipartBody.Part.createFormData("media", "demo.jpg", bytes.toRequestBody("image/jpeg".toMediaType()))
        }
    }

    private fun compressImage(uri: Uri): ByteArray? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > 2048 || bounds.outHeight / sample > 2048) sample *= 2
        val bmp = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        val scale = minOf(1f, 1280f / maxOf(bmp.width, bmp.height))
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true)
        } else bmp
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 82, out)
        return out.toByteArray()
    }
}