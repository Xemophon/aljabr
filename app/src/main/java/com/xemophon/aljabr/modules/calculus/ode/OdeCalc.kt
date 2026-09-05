package com.xemophon.aljabr.modules.calculus.ode

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
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
fun OdeCalc(
    viewModel: CalcBoxViewModel = viewModel(),
    onOpenDrawer: () -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.calculatorMode = CalculatorMode.ODE
    }

    BackHandler(enabled = viewModel.odeResult != null || viewModel.isCalculating) {
        viewModel.handleAction(CalcButtonAction.Clear)
    }

    OdeContent(
        displayText = viewModel.displayText,
        resultText = viewModel.resultText,
        cursorIndex = viewModel.cursorIndex,
        odeResult = viewModel.odeResult,
        isCalculating = viewModel.isCalculating,
        onUpdateCursorIndex = { viewModel.updateCursorIndex(it) },
        onAction = { viewModel.handleAction(it) },
        onOpenDrawer = onOpenDrawer
    )
}

@Composable
fun OdeContent(
    displayText: String,
    resultText: String,
    cursorIndex: Int,
    odeResult: OdeResult? = null,
    isCalculating: Boolean = false,
    onUpdateCursorIndex: (Int) -> Unit,
    onAction: (CalcButtonAction) -> Unit,
    onOpenDrawer: () -> Unit
) {
    CalculatorScaffold(
        title = { Text("Differential Equation Solver") },
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
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (isCalculating) {
                        OdeLoadingReport()
                    } else if (odeResult == null) {
                        CalcBox(
                            expression = displayText,
                            result = resultText,
                            cursorIndex = cursorIndex,
                            onCursorIndexChange = onUpdateCursorIndex,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        OdeReport(
                            result = odeResult,
                            onClear = { onAction(CalcButtonAction.Clear) }
                        )
                    }
                }

                if (odeResult == null && !isCalculating) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ShortCalcButtons(
                        modifier = Modifier.weight(1.3f),
                        gridMode = ShortGridMode.BDE,
                        onAction = onAction
                    )
                }
            }
        }
    }
}

@Composable
fun OdeLoadingReport() {
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
            text = "Solving Differential Equation...",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
