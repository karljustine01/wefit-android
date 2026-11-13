package com.wefit.app.ui.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wefit.app.data.remote.dto.RosterMemberDto
import com.wefit.app.ui.components.*
import com.wefit.app.ui.login.schoolIdLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRosterScreen(viewModel: AdminRosterViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val form by viewModel.form.collectAsState()
    val import by viewModel.importState.collectAsState()

    val snackbar = remember { SnackbarHostState() }
    var showAdd by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<RosterMemberDto?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("School Roster") }) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            // --- Primary actions
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WeFitButton("Add person", onClick = { viewModel.resetForm(); showAdd = true }, modifier = Modifier.weight(1f))
                WeFitOutlinedButton("Import CSV", onClick = { viewModel.resetImport(); showImport = true }, modifier = Modifier.weight(1f))
            }

            // --- Search + role filter
            WeFitTextField(
                value = state.search,
                onValueChange = viewModel::onSearchChange,
                label = "Search by name or ID",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                item {
                    FilterChip(selected = state.roleFilter == null, onClick = { viewModel.onRoleFilter(null) }, label = { Text("All") })
                }
                items(ROSTER_ROLES) { role ->
                    FilterChip(
                        selected = state.roleFilter == role,
                        onClick = { viewModel.onRoleFilter(if (state.roleFilter == role) null else role) },
                        label = { Text(role.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            if (state.members.isNotEmpty()) {
                val registered = state.members.count { it.isRegistered }
                Text(
                    "${state.members.size} on roster  •  $registered registered",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            // --- List
            Box(Modifier.fillMaxSize()) {
                when {
                    state.isLoading && state.members.isEmpty() -> WeFitLoading()
                    state.error != null -> WeFitError(state.error ?: "", onRetry = { viewModel.load() })
                    state.members.isEmpty() -> WeFitEmpty(
                        if (state.search.isNotBlank() || state.roleFilter != null) "No one matches that search."
                        else "The roster is empty.\nAdd people one by one or import a CSV so they can register."
                    )
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.members, key = { it.id }) { member ->
                            RosterRow(member, onDelete = { pendingDelete = member })
                        }
                    }
                }
            }
        }
    }

    // --- Add one person
    if (showAdd) {
        ModalBottomSheet(
            onDismissRequest = { showAdd = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            AddPersonSheet(
                form = form,
                onSave = { id, name, email, role, addAnother ->
                    viewModel.add(id, name, email, role) { if (!addAnother) showAdd = false }
                },
                addAnotherKey = form.lastSaved
            )
        }
    }

    // --- Import CSV
    if (showImport) {
        ModalBottomSheet(
            onDismissRequest = { showImport = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            ImportSheet(
                state = import,
                onPick = viewModel::onFilePicked,
                onUpload = viewModel::upload,
                onDone = { showImport = false }
            )
        }
    }

    // --- Delete confirmation
    pendingDelete?.let { member ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove from roster?") },
            text = {
                Text(
                    buildString {
                        append("${member.name} (${member.school_id}) will no longer be able to register as ${member.role.lowercase()}.")
                        if (member.isRegistered) append("\n\nThey already have an account. It will not be deleted.")
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(member); pendingDelete = null }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun RosterRow(member: RosterMemberDto, onDelete: () -> Unit) {
    WeFitCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(member.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    member.school_id,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!member.email.isNullOrBlank()) {
                    Text(
                        member.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill(member.role.lowercase().replaceFirstChar { it.uppercase() })
                    if (member.isRegistered) StatusPill("Registered", PillTone.Success)
                    else StatusPill("Not registered yet", PillTone.Warning)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Remove ${member.name}", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// ------------------------------------------------------------------ Add sheet

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddPersonSheet(
    form: AddFormState,
    onSave: (id: String, name: String, email: String, role: String, addAnother: Boolean) -> Unit,
    addAnotherKey: String?
) {
    var role by remember { mutableStateOf("STUDENT") }
    var schoolId by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    val idFocus = remember { FocusRequester() }

    // After "Save & add another" the ViewModel publishes a confirmation; clear the fields and refocus.
    LaunchedEffect(addAnotherKey) {
        if (addAnotherKey != null) {
            schoolId = ""; name = ""; email = ""
            runCatching { idFocus.requestFocus() }
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding()
            .padding(horizontal = 20.dp).padding(bottom = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text("Add person to roster", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "They'll be able to register once with this ID and role.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ROSTER_ROLES.forEach { r ->
                FilterChip(
                    selected = role == r,
                    onClick = { role = r },
                    label = { Text(r.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        WeFitTextField(
            value = schoolId,
            onValueChange = { schoolId = it },
            label = schoolIdLabel(role),
            modifier = Modifier.fillMaxWidth().focusRequester(idFocus)
        )
        Spacer(Modifier.height(12.dp))
        WeFitTextField(
            value = name,
            onValueChange = { name = it },
            label = "Full name",
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        WeFitTextField(
            value = email,
            onValueChange = { email = it },
            label = "School email (optional)",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "If filled in, the person must register with exactly this email.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
        )

        if (form.lastSaved != null) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(form.lastSaved, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (form.error != null) {
            Spacer(Modifier.height(12.dp))
            Text(form.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(20.dp))
        WeFitButton(
            "Save & add another",
            onClick = { onSave(schoolId, name, email, role, true) },
            isLoading = form.isSaving,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        WeFitOutlinedButton(
            "Save and close",
            onClick = { onSave(schoolId, name, email, role, false) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// --------------------------------------------------------------- Import sheet

@Composable
private fun ImportSheet(
    state: ImportUiState,
    onPick: (android.net.Uri) -> Unit,
    onUpload: () -> Unit,
    onDone: () -> Unit
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onPick(uri)
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(bottom = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.UploadFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text("Import roster from CSV", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(12.dp))

        val result = state.result
        if (result != null) {
            // ---- Finished
            WeFitCard(modifier = Modifier.fillMaxWidth()) {
                Text("${result.imported} imported", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                Text(
                    "People already on the roster were updated, not duplicated.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (result.skipped.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("${result.skipped.size} rows skipped", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                Text(
                    "Each needs an ID, a name, a valid role (student, varsity, teacher or coach) and, if given, a valid email.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                result.skipped.take(20).forEach {
                    Text(it.ifBlank { "(empty row)" }, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                }
                if (result.skipped.size > 20) {
                    Text("…and ${result.skipped.size - 20} more", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(20.dp))
            WeFitButton("Done", onClick = onDone, modifier = Modifier.fillMaxWidth())
            return@Column
        }

        // ---- Instructions
        WeFitCard(modifier = Modifier.fillMaxWidth()) {
            Text("File format", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "First row must be the column names. Email may be left blank.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "school_id,name,email,role\n2021-00001,Juan Dela Cruz,,STUDENT\nT-1001,Ana Lopez,ana@school.edu,TEACHER",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(Modifier.height(16.dp))

        WeFitOutlinedButton(
            if (state.fileName == null) "Choose CSV file" else "Choose a different file",
            onClick = { picker.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel", "application/octet-stream")) },
            modifier = Modifier.fillMaxWidth()
        )

        if (state.fileName != null) {
            Spacer(Modifier.height(12.dp))
            Text(state.fileName, style = MaterialTheme.typography.titleMedium)
            if (state.fileProblem != null) {
                Text(state.fileProblem, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(
                    "${state.rowCount} ${if (state.rowCount == 1) "person" else "people"} found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (state.error != null) {
            Spacer(Modifier.height(12.dp))
            Text(state.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(20.dp))
        WeFitButton(
            if (state.rowCount > 0 && state.fileProblem == null) "Import ${state.rowCount}" else "Import",
            onClick = onUpload,
            enabled = state.canUpload,
            isLoading = state.isUploading,
            modifier = Modifier.fillMaxWidth()
        )
    }
}