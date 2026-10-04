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
        "curlup_modified", "lunges", "jumping_rope" -> listOf(
            "Hold the phone securely, or keep it in a pocket/armband — this exercise is tracked by motion sensors.",
            "Keep movements consistent for accurate counting."
        )
        "plank" -> listOf(
            "This exercise is timed only — press Start when you begin holding the position."
        )
        "stork_balance" -> listOf(
            "Hold the phone securely (or in a pocket/armband) while balancing.",
            "The timer and wobble count start when you begin the pose."
        )
        "quadrant_agility" -> listOf(
            "Keep the phone with you (pocket or armband) during the test.",
            "This measures time and movement changes — not exact quadrant accuracy."
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