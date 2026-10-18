package com.wefit.app.tracking

import android.content.Context

/**
 * Full Sensor Feasibility Matrix
 *
 * | Exercise                          | Strategy                              | Honest limitation |
 * |-----------------------------------|---------------------------------------|-------------------|
 * | walking                           | Step Counter + Timer                  | none — reliable |
 * | running, 50m_dash, mile_walk      | GPS + Timer                           | GPS accuracy varies indoors |
 * | pushup                            | PushUpTrackingStrategy (dedicated)    | requires clear side-view framing |
 * | squats, situps, pushup_modified   | RepStateMachineTrackingStrategy       | requires clear framing of relevant joints |
 * | lunges                            | RepStateMachineTrackingStrategy       | requires clear framing of lower body joints |
 * | jumping_jacks                     | PoseTrackingStrategy (generic)        | lighting/framing affects accuracy |
 * | plank                             | PoseHoldTrackingStrategy              | relies on stable pose estimation |
 * | stork_balance                     | PoseHoldTrackingStrategy              | relies on leg balance detection |
 * | curlup_modified, jumping_rope     | Accelerometer (peak detect)           | cannot verify form |
 * | quadrant_agility                  | Accelerometer + Timer                 | cannot verify movement pattern |
 * | sit_and_reach, vertical_jump      | Manual Entry                          | requires physical ruler/apparatus reading |
 * | project (Eating Healthy)          | No tracking — not physical             | by design, per spec |
 */
object TrackingStrategyFactory {

    fun create(context: Context, exerciseType: String, lifecycleOwner: androidx.lifecycle.LifecycleOwner? = null): TrackingStrategy {
        return when (exerciseType) {
            "walking" -> StepCounterTrackingStrategy(context)
            "running", "50m_dash", "mile_walk" -> GpsTrackingStrategy(context)
            "pushup" -> lifecycleOwner?.let { PushUpTrackingStrategy(context, it) }
                ?: AccelerometerRepTrackingStrategy(context)
            "squats" -> lifecycleOwner?.let { RepStateMachineTrackingStrategy(context, it, RepExerciseConfigs.SQUAT) }
                ?: AccelerometerRepTrackingStrategy(context)
            "situps" -> lifecycleOwner?.let { RepStateMachineTrackingStrategy(context, it, RepExerciseConfigs.SIT_UP) }
                ?: AccelerometerRepTrackingStrategy(context)
            "pushup_modified" -> lifecycleOwner?.let { RepStateMachineTrackingStrategy(context, it, RepExerciseConfigs.PUSHUP_MODIFIED) }
                ?: AccelerometerRepTrackingStrategy(context)
            "jumping_jacks" -> lifecycleOwner?.let { PoseTrackingStrategy(context, it, PoseExerciseConfigs.JUMPING_JACK) }
                ?: AccelerometerRepTrackingStrategy(context)
            "lunges" -> lifecycleOwner?.let { RepStateMachineTrackingStrategy(context, it, RepExerciseConfigs.LUNGE) }
                ?: AccelerometerRepTrackingStrategy(context)
            "plank" -> lifecycleOwner?.let { PoseHoldTrackingStrategy(context, it, PoseHoldConfigs.PLANK) }
                ?: TimerTrackingStrategy()
            "stork_balance" -> lifecycleOwner?.let { PoseHoldTrackingStrategy(context, it, PoseHoldConfigs.STORK_BALANCE) }
                ?: BalanceTrackingStrategy(context)
            "curlup_modified" -> AccelerometerRepTrackingStrategy(context)
            "jumping_rope" -> MotionPeakTrackingStrategy(context, peakThreshold = 3.5f, releaseThreshold = 1.8f, minIntervalMs = 230L)
            "quadrant_agility" -> MotionPeakTrackingStrategy(context, peakThreshold = 4.0f, releaseThreshold = 2.0f, minIntervalMs = 400L)
            "sit_and_reach", "vertical_jump" -> ManualEntryStrategy()
            "project" -> ManualEntryStrategy()
            "fitness_assessment" -> TimerTrackingStrategy()
            else -> TimerTrackingStrategy()
        }
    }

    fun trackingMethodLabel(exerciseType: String): String {
        return when (exerciseType) {
            "walking" -> "step_counter"
            "running", "50m_dash", "mile_walk" -> "gps"
            "pushup", "squats", "situps", "pushup_modified", "jumping_jacks", "plank", "stork_balance", "lunges" -> "pose_estimation"
            "curlup_modified", "jumping_rope" -> "accelerometer"
            "quadrant_agility" -> "accelerometer"
            "sit_and_reach", "vertical_jump", "project" -> "manual"
            else -> "timer"
        }
    }

    fun isManualEntry(exerciseType: String): Boolean =
        exerciseType in listOf("sit_and_reach", "vertical_jump", "project")

    fun isPoseBased(exerciseType: String): Boolean =
        exerciseType in listOf("pushup", "squats", "situps", "pushup_modified", "jumping_jacks", "plank", "stork_balance", "lunges")

    /** Exercises using the rigorous state-machine strategy (top->bottom->top rep validation). */
    fun hasStateMachineFeedback(exerciseType: String): Boolean =
        exerciseType in listOf("pushup", "squats", "situps", "pushup_modified", "lunges")

    /** Calculates progress percentage based on exercise-specific metrics. */
    fun progressPercent(exerciseType: String, d: TrackingData, target: Double?): Double {
        val goal = when (exerciseType) {
            "50m_dash" -> 50.0
            "mile_walk" -> 1609.34
            else -> target ?: return 0.0
        }
        val current = when (exerciseType) {
            "walking" -> d.steps.toDouble()
            "running", "50m_dash", "mile_walk" -> d.distanceMeters
            "plank", "stork_balance", "quadrant_agility" -> d.durationSeconds.toDouble()
            else -> d.repetitions.toDouble()
        }
        return if (goal > 0) (current / goal * 100.0).coerceIn(0.0, 100.0) else 0.0
    }

    /** Determines if the target metric or fixed-distance requirement has been fulfilled. */
    fun targetReached(exerciseType: String, d: TrackingData, target: Double?): Boolean {
        if (exerciseType in listOf("sit_and_reach", "vertical_jump", "project")) return false
        val fixed = exerciseType == "50m_dash" || exerciseType == "mile_walk"
        if (!fixed && (target == null || target <= 0.0)) return false
        return progressPercent(exerciseType, d, target) >= 100.0
    }
}