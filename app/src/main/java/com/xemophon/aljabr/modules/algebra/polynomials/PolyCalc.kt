package com.xemophon.aljabr.modules.algebra.polynomials

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xemophon.aljabr.ui.components.buttons.CalcButtonAction
import com.xemophon.aljabr.ui.components.buttons.ShortCalcButtons
import com.xemophon.aljabr.ui.components.buttons.ShortGridMode
import com.xemophon.aljabr.ui.components.screens.*

@Composable
fun PolyCalc(
    viewModel: CalcBoxViewModel = viewModel(),
    onOpenDrawer: () -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.calculatorMode = CalculatorMode.POLYNOMIALS
    }

    BackHandler(enabled = viewModel.polynomialResult != null || viewModel.odeResult != null || viewModel.isCalculating) {
        viewModel.handleAction(CalcButtonAction.Clear)
    }

    PolyContent(
        displayText = viewModel.displayText,
        resultText = viewModel.resultText,
        cursorIndex = viewModel.cursorIndex,
        polynomialResult = viewModel.polynomialResult,
        odeResult = viewModel.odeResult,
        polyMode = viewModel.polyMode,
        isCalculating = viewModel.isCalculating,
        onModeChange = { mode ->
            viewModel.polyMode = mode
            viewModel.handleAction(CalcButtonAction.Clear)
        },
        onUpdateCursorIndex = { viewModel.updateCursorIndex(it) },
        onAction = { viewModel.handleAction(it) },
        onOpenDrawer = onOpenDrawer
    )
}

@Composable
fun PolyContent(
    displayText: String,
    resultText: String,
    cursorIndex: Int,
    polynomialResult: PolynomialResult? = null,
    odeResult: OdeResult? = null,
    polyMode: String,
    isCalculating: Boolean = false,
    onModeChange: (String) -> Unit,
    onUpdateCursorIndex: (Int) -> Unit,
    onAction: (CalcButtonAction) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val modes = listOf("Polynomials", "BDE")
    val gridMode = if (polyMode == "BDE") ShortGridMode.BDE else ShortGridMode.Polynomials

    CalculatorScaffold(
        title = { Text(if (polyMode == "Polynomials") "Polynomial Solver" else "Differential Equation Solver") },
        onOpenDrawer = onOpenDrawer,
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
            ) {
                // Mode Selector Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    modes.forEach { mode ->
                        FilterChip(
                            selected = polyMode == mode,
                            onClick = { onModeChange(mode) },
                            label = { Text(if (mode == "BDE") "Differential Equation" else mode) }
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (isCalculating) {
                        PolyLoadingReport(polyMode)
                    } else if (polynomialResult == null && odeResult == null) {
                        CalcBox(
                            expression = displayText,
                            result = resultText,
                            cursorIndex = cursorIndex,
                            onCursorIndexChange = onUpdateCursorIndex,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        if (polynomialResult != null) {
                            PolynomialReport(
                                result = polynomialResult,
                                onClear = { onAction(CalcButtonAction.Clear) }
                            )
                        } else if (odeResult != null) {
                            OdeReport(
                                result = odeResult,
                                onClear = { onAction(CalcButtonAction.Clear) }
                            )
                        }
                    }
                }

                if (polynomialResult == null && odeResult == null && !isCalculating) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ShortCalcButtons(
                        modifier = Modifier.weight(1.3f),
                        gridMode = gridMode,
                        onAction = onAction
                    )
                }
            }
        }
    }
}

@Composable
fun PolyLoadingReport(polyMode: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            strokeWidth = 4.dp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (polyMode == "Polynomials") "Analyzing Polynomial..." else "Solving Differential Equation...",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
