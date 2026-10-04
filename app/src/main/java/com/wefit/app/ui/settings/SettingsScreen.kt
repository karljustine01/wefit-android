package com.wefit.app.ui.settings

import com.wefit.app.ui.components.WeFitCard
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wefit.app.data.local.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val currentMode by viewModel.themeMode.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            Text("Theme", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            WeFitCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.selectableGroup()) {
                    ThemeOption("System Default", ThemeMode.SYSTEM, currentMode) { viewModel.setThemeMode(it) }
                    ThemeOption("Light", ThemeMode.LIGHT, currentMode) { viewModel.setThemeMode(it) }
                    ThemeOption("Dark", ThemeMode.DARK, currentMode) { viewModel.setThemeMode(it) }
                }
            }
        }
    }
}

@Composable
private fun ThemeOption(
    label: String,
    mode: ThemeMode,
    selectedMode: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = (mode == selectedMode),
                onClick = { onSelect(mode) },
                role = Role.RadioButton
            )
            .padding(vertical = 12.dp)
    ) {
        RadioButton(selected = (mode == selectedMode), onClick = null)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}