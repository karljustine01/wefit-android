package com.wefit.app.ui.assignments

import com.wefit.app.ui.components.WeFitCard
import com.wefit.app.ui.components.WeFitEmpty
import com.wefit.app.ui.components.WeFitError
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.entry.entryOf

private enum class Metric(val label: String) { REPS("Reps"), DISTANCE("Distance (m)"), DURATION("Duration (s)") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionCurveScreen(sessionId: Int, viewModel: SessionCurveViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    var metric by remember { mutableStateOf(Metric.REPS) }

    LaunchedEffect(sessionId) { viewModel.load(sessionId) }

    Scaffold(topBar = { TopAppBar(title = { Text("Session Progress Curve") }) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(androidx.compose.ui.Alignment.Center))
                state.error != null -> WeFitError(state.error ?: "", onRetry = { viewModel.load(sessionId) }, modifier = Modifier.fillMaxSize())
                state.logs.size < 2 -> WeFitEmpty(
                    "Not enough progress snapshots to draw a curve. Snapshots are saved every 5 seconds during a tracked session on the latest app version.",
                    modifier = Modifier.fillMaxSize()
                )
                else -> Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Metric.values().forEach { m ->
                            FilterChip(selected = metric == m, onClick = { metric = m }, label = { Text(m.label) })
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    val values = state.logs.map {
                        when (metric) {
                            Metric.REPS -> (it.repetitions ?: 0).toFloat()
                            Metric.DISTANCE -> (it.distance ?: 0.0).toFloat()
                            Metric.DURATION -> (it.duration ?: 0).toFloat()
                        }
                    }

                    WeFitCard(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.height(240.dp)) {
                            val model = entryModelOf(values.mapIndexed { i, v -> entryOf(i.toFloat(), v) })
                            Chart(chart = lineChart(), model = model, startAxis = rememberStartAxis(), bottomAxis = rememberBottomAxis())
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "X axis: snapshot number (about 5 seconds apart). ${values.size} snapshots, final ${metric.label.lowercase()}: ${values.last()}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    val maxV = values.maxOrNull() ?: 0f
                    val avgV = values.average()
                    WeFitCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Peak: $maxV", style = MaterialTheme.typography.titleMedium)
                        Text("Average over snapshots: ${"%.1f".format(avgV)}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}