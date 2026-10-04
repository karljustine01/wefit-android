package com.wefit.app.tracking

import com.google.mlkit.vision.pose.PoseLandmark

data class PosePoint(val x: Float, val y: Float)

/**
 * Defines which landmark pairs get drawn as bones for the stick-figure
 * overlay. Uses ML Kit's 33 real detected body landmarks — nothing here
 * is invented; a connection only draws when both its endpoints were
 * actually detected with sufficient confidence that frame.
 */
object PoseSkeleton {
    val CONNECTIONS: List<Pair<Int, Int>> = listOf(
        PoseLandmark.LEFT_SHOULDER to PoseLandmark.RIGHT_SHOULDER,
        PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_ELBOW,
        PoseLandmark.LEFT_ELBOW to PoseLandmark.LEFT_WRIST,
        PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_ELBOW,
        PoseLandmark.RIGHT_ELBOW to PoseLandmark.RIGHT_WRIST,
        PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_HIP,
        PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_HIP,
        PoseLandmark.LEFT_HIP to PoseLandmark.RIGHT_HIP,
        PoseLandmark.LEFT_HIP to PoseLandmark.LEFT_KNEE,
        PoseLandmark.LEFT_KNEE to PoseLandmark.LEFT_ANKLE,
        PoseLandmark.RIGHT_HIP to PoseLandmark.RIGHT_KNEE,
        PoseLandmark.RIGHT_KNEE to PoseLandmark.RIGHT_ANKLE,
        PoseLandmark.NOSE to PoseLandmark.LEFT_SHOULDER,
        PoseLandmark.NOSE to PoseLandmark.RIGHT_SHOULDER
    )
}