package com.wefit.app.ui.analytics

import com.wefit.app.ui.components.WeFitCard
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.entry.entryModelOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(viewModel: AnalyticsViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Analytics") }) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Error: ${state.error}", color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.load() }) { Text("Retry") }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        state.teacherData?.let { data ->
                            StatGrid(
                                listOf(
                                    StatItem("Sections", data.total_sections.toString(), Icons.Filled.Group),
                                    StatItem("Students", data.total_students.toString(), Icons.Filled.Group),
                                    StatItem("Assignments", data.total_assignments.toString(), Icons.Filled.Assignment),
                                    StatItem("Completed", data.completed_sessions.toString(), Icons.Filled.CheckCircle),
                                    StatItem("Pending Grades", data.pending_grades.toString(), Icons.Filled.Star)
                                )
                            )
                        }
                        state.studentData?.let { data ->
                            StatGrid(
                                listOf(
                                    StatItem("Assignments", data.total_assignments.toString(), Icons.Filled.Assignment),
                                    StatItem("Completed", data.completed_sessions.toString(), Icons.Filled.CheckCircle),
                                    StatItem("Avg Grade", data.average_grade?.let { "%.1f".format(it) } ?: "—", Icons.Filled.Star),
                                    StatItem("Total Grades", data.total_grades.toString(), Icons.Filled.Star)
                                )
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        if (state.trend.isNotEmpty()) {
                            Text("Completed Sessions — Last 6 Weeks", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(12.dp))
                            WeFitCard(modifier = Modifier.fillMaxWidth()) {
                                Box(modifier = Modifier.height(220.dp)) {
                                    val entries = state.trend.mapIndexed { index, point ->
                                        com.patrykandpatrick.vico.core.entry.entryOf(index.toFloat(), point.count.toFloat())
                                    }
                                    val model = entryModelOf(entries)
                                    Chart(
                                        chart = columnChart(),
                                        model = model,
                                        startAxis = rememberStartAxis(),
                                        bottomAxis = rememberBottomAxis()
                                    )
                                }
                            }
                        } else {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "No completed sessions in the last 6 weeks yet — the trend chart will populate as sessions are completed.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class StatItem(val label: String, val value: String, val icon: ImageVector)

@Composable
private fun StatGrid(items: List<StatItem>) {
    Column {
        items.chunked(2).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowItems.forEach { item ->
                    StatCard(item, modifier = Modifier.weight(1f).padding(4.dp))
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatCard(item: StatItem, modifier: Modifier = Modifier) {
    WeFitCard(modifier = modifier) {
        Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text(item.value, style = MaterialTheme.typography.headlineSmall)
        Text(item.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}