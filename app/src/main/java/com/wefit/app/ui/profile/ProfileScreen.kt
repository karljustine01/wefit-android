package com.wefit.app.ui.profile

import com.wefit.app.ui.components.WeFitCard
import com.wefit.app.ui.components.WeFitButton
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: ProfileViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    var name by remember { mutableStateOf("") }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(state.user) {
        if (state.user != null && !initialized) {
            name = state.user?.name ?: ""
            initialized = true
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Profile") }) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading && state.user == null -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null && state.user == null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Error: ${state.error}", color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.load() }) { Text("Retry") }
                    }
                }
                state.user != null -> {
                    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                        WeFitCard(modifier = Modifier.fillMaxWidth()) {
                            Text("Email: ${state.user?.email}", style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(4.dp))
                            Text("Role: ${state.user?.role}", style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(4.dp))
                            Text("Status: ${state.user?.status}", style = MaterialTheme.typography.bodyLarge)
                        }
                        Spacer(Modifier.height(24.dp))
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (state.error != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(state.error ?: "", color = MaterialTheme.colorScheme.error)
                        }
                        if (state.saveSuccess) {
                            Spacer(Modifier.height(8.dp))
                            Text("Saved!", color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(16.dp))
                        WeFitButton(
                            text = "Save",
                            onClick = { viewModel.updateName(name) },
                            enabled = name.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(24.dp))
                        Text(
                            "NOT YET VERIFIED: profile photo upload backend endpoint exists (/api/users/profile-image) but no UI to pick/upload an image is built yet.",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}