package com.wefit.app.ui.sections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionMembersScreen(
    sectionId: Int,
    isOwner: Boolean,
    viewModel: SectionMembersViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(sectionId) {
        viewModel.load(sectionId)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(state.section?.name ?: "Section") }) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null -> {
                    Text("Error: ${state.error}", modifier = Modifier.align(Alignment.Center))
                }
                state.section != null -> {
                    val members = state.section?.members.orEmpty()
                    if (members.isEmpty()) {
                        Text("No members yet", modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Text("Section Code: ${state.section?.section_code}", style = MaterialTheme.typography.titleLarge)
                                Spacer(Modifier.height(12.dp))
                            }
                            items(members) { member ->
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        Modifier.padding(16.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(member.user?.name ?: "Unknown")
                                            Text(member.user?.email ?: "", style = MaterialTheme.typography.labelLarge)
                                        }
                                        if (isOwner) {
                                            TextButton(onClick = {
                                                viewModel.removeMember(sectionId, member.user_id)
                                            }) {
                                                Text("Remove")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}