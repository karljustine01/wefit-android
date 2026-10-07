package com.wefit.app.tracking

import android.content.Context

/**
 * Full Sensor Feasibility Matrix
 *
 * | Exercise                          | Strategy                              | Honest limitation |
 * |------------------------------------|-----------------------------------------|--------------------|
 * | walking                            | Step Counter + Timer                    | none — reliable |
 * | running, 50m_dash, mile_walk       | GPS + Timer                             | GPS accuracy varies indoors |
 * | pushup                             | PushUpTrackingStrategy (dedicated)      | requires clear side-view framing |
 * | squats, situps, pushup_modified    | RepStateMachineTrackingStrategy         | requires clear framing of relevant joints |
 * | lunges                             | RepStateMachineTrackingStrategy         | requires clear framing of lower body joints |
 * | jumping_jacks                      | PoseTrackingStrategy (generic)          | lighting/framing affects accuracy |
 * | plank                              | PoseHoldTrackingStrategy                | relies on stable pose estimation |
 * | stork_balance                      | PoseHoldTrackingStrategy                | relies on leg balance detection |
 * | curlup_modified, jumping_rope      | Accelerometer (peak detect)             | cannot verify form |
 * | quadrant_agility                   | Accelerometer + Timer                   | cannot verify movement pattern |
 * | sit_and_reach, vertical_jump       | Manual Entry                            | requires physical ruler/apparatus reading |
 * | project (Eating Healthy)           | No tracking — not physical               | by design, per spec |
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
            "curlup_modified", "jumping_rope" -> AccelerometerRepTrackingStrategy(context)
            "quadrant_agility" -> AgilityTrackingStrategy(context)
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
}