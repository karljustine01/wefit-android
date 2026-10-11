package com.wefit.app.tracking

object ExerciseInstructions {

    fun tipsFor(exerciseType: String): List<String> = when (exerciseType) {
        "pushup" -> listOf(
            "Side View Required.",
            "Place the camera beside you, roughly 1.5–2 meters away.",
            "Make sure your whole body is visible — head to feet.",
            "Keep your shoulder, elbow, wrist, hip, knee, and ankle visible on one side.",
            "Keep the camera stable (prop it against a wall or object).",
            "Start in the push-up position before you tap Start.",
            "This does not guarantee perfect pose recognition — adjust position if tracking looks unstable."
        )
        "pushup_modified" -> listOf(
            "Side View Required (same setup as a regular push-up).",
            "Place the camera beside you, roughly 1.5–2 meters away.",
            "Keep your shoulder, elbow, wrist, hip, knee, and ankle visible on one side.",
            "Start with your knees on the ground — this is what distinguishes this variant.",
            "Keep the camera stable and start in position before tapping Start."
        )
        "squats" -> listOf(
            "Front View Required.",
            "Prop the phone about 2 meters away, facing you directly.",
            "Make sure your hips, knees, and ankles are all visible.",
            "Stand centered in the frame with feet shoulder-width apart."
        )
        "situps" -> listOf(
            "Side View Required.",
            "Prop the phone at floor level, side-on to your body, ~1.5 meters away.",
            "Make sure your shoulder, hip, and knee are all visible in frame.",
            "Keep your feet anchored throughout."
        )
        "jumping_jacks" -> listOf(
            "Prop the phone 2–3 meters away, facing you directly.",
            "Make sure there's room in frame for your arms and legs to fully extend.",
            "Stand centered before starting."
        )
        "walking" -> listOf(
            "Carry your phone normally (pocket or armband) — the step counter works from device motion.",
            "No camera needed for this exercise."
        )
        "running", "50m_dash", "mile_walk" -> listOf(
            "Carry your phone with you and go outdoors for the most accurate GPS signal.",
            "GPS accuracy is reduced indoors or in dense urban areas.",
            "Allow a few seconds after starting for GPS to lock on."
        )
        "curlup_modified", "lunges" -> listOf(
            "Hold the phone securely, or keep it in a pocket/armband — this exercise is tracked by motion sensors.",
            "Keep movements consistent for accurate counting."
        )
        "jumping_rope" -> listOf(
            "Keep the phone in a snug pocket or armband, not in your hand.",
            "Each jump is counted as a motion peak; keep jumps consistent.",
            "Counting is motion-based and cannot check your form."
        )
        "plank" -> listOf(
            "Side view required — camera should see your whole body straight-on from the side.",
            "Prop the phone 1.5–2 meters away, at floor level if possible.",
            "Get into plank position before the timer will start counting.",
            "The timer only advances while your body stays straight; it pauses automatically if your form breaks."
        )
        "stork_balance" -> listOf(
            "Side or 3/4 view required — camera should see your whole body.",
            "Prop the phone 1.5–2 meters away.",
            "Stand on one leg, lifting the other foot off the ground.",
            "The timer starts automatically once your lifted leg is detected, and pauses if you lower it or lose balance.",
            "Hold as long as you can — the timer resumes instantly if you regain the pose."
        )
        "quadrant_agility" -> listOf(
            "Keep the phone in a snug pocket or armband.",
            "The timer is the main result; direction changes are counted as extra data.",
            "Quadrant order is not verified by the phone."
        )
        "sit_and_reach", "vertical_jump" -> listOf(
            "This result must be measured manually (e.g. with a ruler or marked wall) and entered after."
        )
        "project" -> listOf(
            "This is a non-physical assignment — mark it complete when finished."
        )
        else -> emptyList()
    }
}