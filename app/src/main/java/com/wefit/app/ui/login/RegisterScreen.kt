package com.wefit.app.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: (String) -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("STUDENT") }
    var schoolId by remember { mutableStateOf("") }
    val roles = listOf("STUDENT", "VARSITY", "TEACHER", "COACH")
    var expanded by remember { mutableStateOf(false) }

    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.loggedInRole) {
        state.loggedInRole?.let { onRegisterSuccess(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text("Create Account", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = { Text("Confirm Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = role,
                onValueChange = {},
                readOnly = true,
                label = { Text("Role") },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                roles.forEach {
                    DropdownMenuItem(text = { Text(it) }, onClick = { role = it; expanded = false })
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = schoolId,
            onValueChange = { schoolId = it },
            label = { Text(schoolIdLabel(role)) },
            supportingText = { Text("Must match the school's official list for this role.") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        if (state.error != null) {
            Text(state.error ?: "", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
        }

        Button(
            onClick = { viewModel.register(name.trim(), email.trim(), password, confirmPassword, role, schoolId.trim()) },
            enabled = !state.isLoading && schoolId.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp)) else Text("Register")
        }

        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onNavigateToLogin, modifier = Modifier.fillMaxWidth()) {
            Text("Already have an account? Login")
        }
    }
}

fun schoolIdLabel(role: String): String = when (role) {
    "STUDENT" -> "Student Number"
    "VARSITY" -> "Varsity / Student Number"
    "TEACHER" -> "Teacher / Employee ID"
    "COACH" -> "Coach / Employee ID"
    else -> "School ID"
}