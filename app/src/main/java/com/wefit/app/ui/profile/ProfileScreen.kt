package com.wefit.app.ui.profile

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wefit.app.ui.components.WeFitButton
import com.wefit.app.ui.components.WeFitCard
import com.wefit.app.ui.components.WeFitOutlinedButton

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

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.uploadImage(uri)
    }

    val avatar = remember(state.imageBase64) {
        state.imageBase64?.let {
            try {
                val bytes = Base64.decode(it, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Profile") }) }) { padding ->
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
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(104.dp).clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (avatar != null) {
                                Image(
                                    bitmap = avatar,
                                    contentDescription = "Profile photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(24.dp)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        if (state.isUploadingImage) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            TextButton(onClick = {
                                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }) { Text(if (avatar == null) "Add photo" else "Change photo") }
                        }

                        Spacer(Modifier.height(12.dp))
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
                    }
                }
            }
        }
    }
}