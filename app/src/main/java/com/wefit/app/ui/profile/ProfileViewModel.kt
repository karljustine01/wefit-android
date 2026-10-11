package com.wefit.app.ui.profile

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wefit.app.data.remote.dto.UserDto
import com.wefit.app.data.repository.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

data class ProfileUiState(
    val isLoading: Boolean = false,
    val user: UserDto? = null,
    val error: String? = null,
    val saveSuccess: Boolean = false,
    val imageBase64: String? = null,
    val isUploadingImage: Boolean = false
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProfileRepository(application)

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState(isLoading = true)
            repository.getProfile().fold(
                onSuccess = { user ->
                    _uiState.value = ProfileUiState(user = user)
                    repository.getProfileImage().onSuccess { img ->
                        _uiState.value = _uiState.value.copy(imageBase64 = img)
                    }
                },
                onFailure = { e -> _uiState.value = ProfileUiState(error = e.message) }
            )
        }
    }

    fun updateName(name: String) {
        viewModelScope.launch {
            val keepImage = _uiState.value.imageBase64
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, saveSuccess = false)
            repository.updateProfile(name).fold(
                onSuccess = { user ->
                    _uiState.value = ProfileUiState(user = user, saveSuccess = true, imageBase64 = keepImage)
                },
                onFailure = { e -> _uiState.value = _uiState.value.copy(isLoading = false, error = e.message) }
            )
        }
    }

    fun uploadImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploadingImage = true, error = null)
            val bytes = withContext(Dispatchers.IO) { compress(uri) }
            if (bytes == null) {
                _uiState.value = _uiState.value.copy(isUploadingImage = false, error = "Could not read that image")
                return@launch
            }
            repository.uploadProfileImage(bytes).fold(
                onSuccess = { user ->
                    _uiState.value = _uiState.value.copy(
                        user = user,
                        isUploadingImage = false,
                        imageBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    )
                },
                onFailure = { e -> _uiState.value = _uiState.value.copy(isUploadingImage = false, error = e.message) }
            )
        }
    }

    /**
     * Shrinks the picked image to at most 512px and re-encodes as JPEG so the
     * upload is small. LIMITATION: camera photos with EXIF rotation may show
     * rotated, because EXIF orientation is not applied here.
     */
    private fun compress(uri: Uri): ByteArray? = try {
        val resolver = getApplication<Application>().contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        var sample = 1
        while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) sample *= 2

        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }

        if (bmp == null) null else {
            val scale = minOf(1f, 512f / maxOf(bmp.width, bmp.height))
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true)
            } else bmp
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
            out.toByteArray()
        }
    } catch (e: Exception) {
        null
    }
}