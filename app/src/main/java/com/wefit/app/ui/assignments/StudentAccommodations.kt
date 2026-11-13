package com.wefit.app.ui.assignments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.wefit.app.data.remote.dto.ExerciseDto
import com.wefit.app.data.remote.dto.SectionMemberDto
import com.wefit.app.data.remote.dto.StudentOverrideDto
import com.wefit.app.tracking.ExerciseUnits
import com.wefit.app.ui.components.WeFitCard

private enum class AccommodationMode { SAME, TARGET, ALTERNATE, EXCLUDE }

private fun formatNumber(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()

/**
 * Lets a teacher give individual students (e.g. those with a medical condition) a different
 * target, a different exercise, or leave them out of the assignment.
 */
@Composable
fun StudentAccommodationsCard(
    members: List<SectionMemberDto>,
    loading: Boolean,
    exercises: List<ExerciseDto>,
    mainExerciseId: Int?,
    mainExerciseType: String,
    overrides: Map<Int, StudentOverrideDto>,
    onSet: (userId: Int, override: StudentOverrideDto?) -> Unit,
    modifier: Modifier = Modifier
) {
    var editing by remember { mutableStateOf<SectionMemberDto?>(null) }

    WeFitCard(modifier = modifier.fillMaxWidth()) {
        Text("Special-needs adjustments", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "Give a student their own target, a different exercise, or leave them out " +
                    "(for example, students with a medical condition). Everyone else gets the class settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        when {
            loading -> CircularProgressIndicator()
            members.isEmpty() -> Text(
                "This section has no students yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            else -> {
                Text(
                    "${overrides.size} of ${members.size} students adjusted",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                members.forEach { member ->
                    val userId = member.user_id ?: return@forEach
                    val current = overrides[userId]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editing = member }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(member.user?.name ?: "Student #$userId", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                summarize(current, exercises, mainExerciseType),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (current == null) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.primary
                            )
                        }
                        TextButton(onClick = { editing = member }) { Text(if (current == null) "Adjust" else "Edit") }
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    editing?.let { member ->
        val userId = member.user_id
        if (userId != null) {
            AccommodationDialog(
                studentName = member.user?.name ?: "Student #$userId",
                userId = userId,
                current = overrides[userId],
                exercises = exercises.filter { it.id != mainExerciseId },
                mainExerciseType = mainExerciseType,
                onSave = { onSet(userId, it); editing = null },
                onDismiss = { editing = null }
            )
        } else {
            editing = null
        }
    }
}

private fun summarize(o: StudentOverrideDto?, exercises: List<ExerciseDto>, mainType: String): String = when {
    o == null -> "Same as class"
    o.excluded -> "Excluded from this assignment"
    o.alternate_exercise_id != null -> {
        val alt = exercises.find { it.id == o.alternate_exercise_id }
        val unit = alt?.let { ExerciseUnits.targetUnit(it.exercise_type) }
        buildString {
            append("Different exercise: ${alt?.name ?: "#${o.alternate_exercise_id}"}")
            o.target_value?.let { append(" · ${formatNumber(it)} ${unit ?: ""}".trimEnd()) }
        }
    }
    o.target_value != null -> "Own target: ${formatNumber(o.target_value)} ${ExerciseUnits.targetUnit(mainType) ?: ""}".trimEnd()
    else -> "Same as class"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccommodationDialog(
    studentName: String,
    userId: Int,
    current: StudentOverrideDto?,
    exercises: List<ExerciseDto>,
    mainExerciseType: String,
    onSave: (StudentOverrideDto?) -> Unit,
    onDismiss: () -> Unit
) {
    val mainUnit = if (mainExerciseType.isBlank()) null else ExerciseUnits.targetUnit(mainExerciseType)

    var mode by remember {
        mutableStateOf(
            when {
                current == null -> AccommodationMode.SAME
                current.excluded -> AccommodationMode.EXCLUDE
                current.alternate_exercise_id != null -> AccommodationMode.ALTERNATE
                current.target_value != null -> AccommodationMode.TARGET
                else -> AccommodationMode.SAME
            }
        )
    }
    var targetText by remember {
        mutableStateOf(if (current?.alternate_exercise_id == null) current?.target_value?.let(::formatNumber).orEmpty() else "")
    }
    var altId by remember { mutableStateOf(current?.alternate_exercise_id) }
    var altTargetText by remember {
        mutableStateOf(if (current?.alternate_exercise_id != null) current.target_value?.let(::formatNumber).orEmpty() else "")
    }
    var note by remember { mutableStateOf(current?.note.orEmpty()) }
    var altExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val altExercise = exercises.find { it.id == altId }
    val altUnit = altExercise?.let { ExerciseUnits.targetUnit(it.exercise_type) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(studentName) },
        text = {
            Column(modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                ModeRow("Same as class", mode == AccommodationMode.SAME) { mode = AccommodationMode.SAME; error = null }

                ModeRow(
                    if (mainUnit != null) "Own target (reps / duration)" else "Own target (not available for this exercise)",
                    mode == AccommodationMode.TARGET,
                    enabled = mainUnit != null
                ) { mode = AccommodationMode.TARGET; error = null }
                if (mode == AccommodationMode.TARGET && mainUnit != null) {
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it; error = null },
                        label = { Text("Target ($mainUnit)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                ModeRow("Different exercise", mode == AccommodationMode.ALTERNATE) { mode = AccommodationMode.ALTERNATE; error = null }
                if (mode == AccommodationMode.ALTERNATE) {
                    ExposedDropdownMenuBox(expanded = altExpanded, onExpandedChange = { altExpanded = it }) {
                        OutlinedTextField(
                            value = altExercise?.name.orEmpty(), onValueChange = {}, readOnly = true,
                            label = { Text("Exercise for this student") },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = altExpanded,
                            onDismissRequest = { altExpanded = false },
                            modifier = Modifier.heightIn(max = 260.dp)
                        ) {
                            exercises.forEach { ex ->
                                DropdownMenuItem(
                                    text = { Text(ex.name) },
                                    onClick = {
                                        altId = ex.id
                                        altTargetText = ""
                                        altExpanded = false
                                        error = null
                                    }
                                )
                            }
                        }
                    }
                    if (altUnit != null) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = altTargetText,
                            onValueChange = { altTargetText = it; error = null },
                            label = { Text("Target ($altUnit) – optional") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                ModeRow("Exclude from this assignment", mode == AccommodationMode.EXCLUDE) { mode = AccommodationMode.EXCLUDE; error = null }

                if (mode != AccommodationMode.SAME) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it.take(500) },
                        label = { Text("Private note (only you can see this)") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                fun parse(text: String) = text.trim().replace(',', '.').toDoubleOrNull()
                val noteOrNull = note.trim().ifBlank { null }
                when (mode) {
                    AccommodationMode.SAME -> onSave(null)
                    AccommodationMode.EXCLUDE -> onSave(StudentOverrideDto(userId, excluded = true, note = noteOrNull))
                    AccommodationMode.TARGET -> {
                        val t = parse(targetText)
                        if (t == null || t <= 0.0) error = "Enter a target greater than 0."
                        else onSave(StudentOverrideDto(userId, target_value = t, note = noteOrNull))
                    }
                    AccommodationMode.ALTERNATE -> {
                        val t = if (altTargetText.isBlank()) null else parse(altTargetText)
                        when {
                            altId == null -> error = "Pick the exercise for this student."
                            altTargetText.isNotBlank() && (t == null || t <= 0.0) -> error = "Target must be greater than 0."
                            else -> onSave(StudentOverrideDto(userId, alternate_exercise_id = altId, target_value = t, note = noteOrNull))
                        }
                    }
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ModeRow(label: String, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick, enabled = enabled)
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}