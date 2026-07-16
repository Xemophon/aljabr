package com.xemophon.aljabr.differentiate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.activity.compose.BackHandler
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xemophon.aljabr.ui.components.AdvancedButtonsGrid
import com.xemophon.aljabr.ui.components.CalcBoxViewModel
import com.xemophon.aljabr.ui.components.CalculatorFocus
import com.xemophon.aljabr.ui.components.CalculatorMode
import com.xemophon.aljabr.ui.components.CalculatorScaffold
import com.xemophon.aljabr.ui.theme.AlJabrTheme

@Composable
fun DiffCalc(onOpenDrawer: () -> Unit) {
    val viewModel: CalcBoxViewModel = viewModel()
    var isInverse by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.calculatorMode = CalculatorMode.DIFFERENTIATE
    }

    BackHandler(enabled = viewModel.analysisResult != null) {
        viewModel.handleAction(com.xemophon.aljabr.ui.components.CalcButtonAction.Clear)
    }

    CalculatorScaffold(
        title = { Text("Differentiate") },
        onOpenDrawer = onOpenDrawer
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (viewModel.analysisResult == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            DiffDisplay(
                                expression = viewModel.displayText,
                                diffGridMode = viewModel.diffGridMode,
                                cursorIndex = viewModel.cursorIndex,
                                onFocusChange = { viewModel.setFocus(it) },
                                onCursorIndexChange = { viewModel.updateCursorIndex(it) }
                            )
                        }
                    } else {
                        AnalysisReport(
                            result = viewModel.analysisResult!!,
                            onClear = { viewModel.handleAction(com.xemophon.aljabr.ui.components.CalcButtonAction.Clear) }
                        )
                    }
                }
                
                if (viewModel.analysisResult == null) {
                    AdvancedButtonsGrid(
                        isInverse = isInverse,
                        mode = "Differentiate",
                        diffGridMode = viewModel.diffGridMode,
                        onToggleInverse = { isInverse = !isInverse },
                        onAction = { viewModel.handleAction(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun AnalysisReport(result: AnalysisResult, onClear: () -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Analysis Result",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        if (result.error != null) {
            item {
                Text(text = "Error: ${result.error}", color = MaterialTheme.colorScheme.error)
            }
        } else {
            // Derivatives
            item { AnalysisSectionHeader("Derivatives") }
            items(result.derivatives) { deriv ->
                AnalysisItemCard(deriv.name, deriv.expression)
            }

            // Stationary Points
            if (result.stationaryPoints.isNotEmpty()) {
                item { AnalysisSectionHeader("Stationary Points") }
                items(result.stationaryPoints) { point ->
                    AnalysisItemCard("Point", point)
                }
            }

            // Inflection Points
            if (result.inflectionPoints.isNotEmpty()) {
                item { AnalysisSectionHeader("Inflection Points") }
                items(result.inflectionPoints) { point ->
                    AnalysisItemCard("Inflection", point)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            ElevatedCard(
                onClick = onClear,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(text = "Clear and Return", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun AnalysisSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun AnalysisItemCard(label: String, value: String) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun DiffDisplay(
    expression: String,
    diffGridMode: String,
    cursorIndex: Int,
    onFocusChange: (CalculatorFocus) -> Unit,
    onCursorIndexChange: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Derivative Notation
            if (diffGridMode == "Single") {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "d",
                        style = MaterialTheme.typography.displaySmall.copy(fontSize = 32.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(2.dp)
                            .background(MaterialTheme.colorScheme.onSurface)
                    )
                    Text(
                        text = "dx",
                        style = MaterialTheme.typography.displaySmall.copy(fontSize = 32.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                Text(
                    text = "∇f",
                    style = MaterialTheme.typography.displayMedium.copy(fontSize = 48.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Expression in brackets [ f(x) ]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable {
                    onFocusChange(CalculatorFocus.EXPRESSION)
                    onCursorIndexChange(expression.length)
                }
            ) {
                Text(
                    text = "[",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 80.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                ) {
                    val base = if (expression == "0") "" else expression
                    val textWithCursor = if (cursorIndex != -1) {
                        if (cursorIndex < base.length) {
                            StringBuilder(base).insert(cursorIndex, "|").toString()
                        } else {
                            "$base|"
                        }
                    } else {
                        base.ifEmpty { "f(x)" }
                    }

                    Text(
                        text = textWithCursor,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = if (expression.length > 10) 32.sp else 48.sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "]",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 80.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DiffPreview() {
    AlJabrTheme {
        DiffCalc(onOpenDrawer = {})
    }
}
