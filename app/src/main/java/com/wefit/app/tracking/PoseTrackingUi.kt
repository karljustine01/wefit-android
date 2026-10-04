package com.wefit.app.tracking

import androidx.camera.view.PreviewView
import kotlinx.coroutines.flow.StateFlow

/**
 * Shared UI-facing contract for camera-based pose tracking strategies.
 * Implemented by both the generic PoseTrackingStrategy (squats, sit-ups,
 * jumping jacks — unchanged) and the new PushUpTrackingStrategy, so the
 * ViewModel and Compose UI can drive either one through a single type
 * without depending on which concrete class is active.
 */
interface PoseTrackingUi {
    var previewView: PreviewView?
    val isFrontCamera: Boolean
    val skeletonPoints: StateFlow<Map<Int, PosePoint>>
    val currentAngle: StateFlow<Double?>
    val repState: StateFlow<String>
    val poseDetected: StateFlow<Boolean>
    fun attachPreviewSurface(view: PreviewView)
    fun beginCounting()
}