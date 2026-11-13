package com.wefit.app.ui.admin

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wefit.app.data.remote.dto.RosterImportResult
import com.wefit.app.data.remote.dto.RosterMemberDto
import com.wefit.app.data.repository.AdminRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

val ROSTER_ROLES = listOf("STUDENT", "VARSITY", "TEACHER", "COACH")

data class RosterUiState(
    val isLoading: Boolean = false,
    val members: List<RosterMemberDto> = emptyList(),
    val error: String? = null,
    val search: String = "",
    val roleFilter: String? = null,
    /** One-shot text for the snackbar. */
    val message: String? = null
)

data class AddFormState(
    val isSaving: Boolean = false,
    val error: String? = null,
    /** Confirmation of the last saved person, shown in the sheet so "save & add another" feels responsive. */
    val lastSaved: String? = null
)

data class ImportUiState(
    val fileName: String? = null,
    val rowCount: Int = 0,
    /** Problem found in the picked file before uploading (wrong columns, too big, ...). */
    val fileProblem: String? = null,
    val isUploading: Boolean = false,
    val result: RosterImportResult? = null,
    val error: String? = null
) {
    val canUpload: Boolean get() = fileName != null && fileProblem == null && !isUploading && result == null
}

class AdminRosterViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AdminRepository(application)

    private val _uiState = MutableStateFlow(RosterUiState())
    val uiState: StateFlow<RosterUiState> = _uiState.asStateFlow()

    private val _form = MutableStateFlow(AddFormState())
    val form: StateFlow<AddFormState> = _form.asStateFlow()

    private val _import = MutableStateFlow(ImportUiState())
    val importState: StateFlow<ImportUiState> = _import.asStateFlow()

    private var searchJob: Job? = null
    private var pickedBytes: ByteArray? = null

    init { load() }

    // ------------------------------------------------------------------ list

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val s = _uiState.value
            repository.listRoster(s.roleFilter, s.search.ifBlank { null }).fold(
                onSuccess = { _uiState.value = _uiState.value.copy(isLoading = false, members = it) },
                onFailure = { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message ?: "Could not load the roster") }
            )
        }
    }

    fun onSearchChange(text: String) {
        _uiState.value = _uiState.value.copy(search = text)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // wait for the admin to stop typing
            load()
        }
    }

    fun onRoleFilter(role: String?) {
        _uiState.value = _uiState.value.copy(roleFilter = role)
        load()
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun delete(member: RosterMemberDto) {
        viewModelScope.launch {
            repository.deleteRosterMember(member.id).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        members = _uiState.value.members.filterNot { it.id == member.id },
                        message = "${member.name} removed from the roster"
                    )
                },
                onFailure = { _uiState.value = _uiState.value.copy(message = it.message ?: "Could not remove entry") }
            )
        }
    }

    // ------------------------------------------------------------- add one

    fun resetForm() {
        _form.value = AddFormState()
    }

    /** @param onSaved called after a successful save (the screen decides whether to close the sheet). */
    fun add(schoolId: String, name: String, email: String, role: String, onSaved: () -> Unit) {
        val id = schoolId.trim()
        val fullName = name.trim()
        val mail = email.trim()

        val problem = when {
            id.isEmpty() -> "Enter the ID number."
            fullName.isEmpty() -> "Enter the person's full name."
            mail.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(mail).matches() -> "That email address doesn't look right."
            else -> null
        }
        if (problem != null) {
            _form.value = _form.value.copy(error = problem)
            return
        }

        viewModelScope.launch {
            _form.value = _form.value.copy(isSaving = true, error = null, lastSaved = null)
            repository.addRosterMember(id, fullName, mail.ifEmpty { null }, role).fold(
                onSuccess = {
                    _form.value = AddFormState(lastSaved = "${it.name} (${it.school_id}) saved")
                    _uiState.value = _uiState.value.copy(message = "${it.name} added as ${it.role.lowercase()}")
                    load()
                    onSaved()
                },
                onFailure = { _form.value = _form.value.copy(isSaving = false, error = it.message ?: "Could not save") }
            )
        }
    }

    // ----------------------------------------------------------- CSV import

    fun resetImport() {
        pickedBytes = null
        _import.value = ImportUiState()
    }

    fun onFilePicked(uri: Uri) {
        val resolver = getApplication<Application>().contentResolver
        val displayName: String? = try {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        } catch (_: Exception) {
            null
        }
        val name = displayName ?: "roster.csv"

        val bytes = try {
            resolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (_: Exception) { null }

        if (bytes == null) {
            _import.value = ImportUiState(fileName = name, fileProblem = "Could not read that file.")
            return
        }
        pickedBytes = bytes

        val text = bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF")
        val lines = text.lines().filter { it.isNotBlank() }
        val header = lines.firstOrNull()?.lowercase()?.split(',')?.map { it.trim().trim('"') }.orEmpty()
        val missing = listOf("school_id", "name", "role").filterNot { it in header }

        val problem = when {
            bytes.size > 5 * 1024 * 1024 -> "File is larger than 5 MB."
            lines.isEmpty() -> "The file is empty."
            missing.isNotEmpty() && (lines.first().contains(';') || lines.first().contains('\t')) ->
                "Columns must be separated by commas, not semicolons or tabs. Re-save as \"CSV (Comma delimited)\"."
            missing.isNotEmpty() -> "Missing column: ${missing.joinToString(", ")}. First row must be school_id,name,email,role."
            lines.size < 2 -> "The file has a header but no people in it."
            else -> null
        }

        _import.value = ImportUiState(
            fileName = name,
            rowCount = (lines.size - 1).coerceAtLeast(0),
            fileProblem = problem
        )
    }

    fun upload() {
        val bytes = pickedBytes ?: return
        val state = _import.value
        val name = state.fileName ?: "roster.csv"
        if (!state.canUpload) return

        viewModelScope.launch {
            _import.value = state.copy(isUploading = true, error = null)
            repository.importRoster(name, bytes).fold(
                onSuccess = {
                    _import.value = _import.value.copy(isUploading = false, result = it)
                    load()
                },
                onFailure = {
                    _import.value = _import.value.copy(isUploading = false, error = it.message ?: "Upload failed")
                }
            )
        }
    }
}