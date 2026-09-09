package com.xemophon.aljabr.modules.algebra.polynomials

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xemophon.aljabr.ui.components.buttons.CalcButtonAction
import com.xemophon.aljabr.ui.components.buttons.ShortCalcButtons
import com.xemophon.aljabr.ui.components.buttons.ShortGridMode
import com.xemophon.aljabr.ui.components.engine.CalcBox
import com.xemophon.aljabr.ui.components.engine.CalcBoxViewModel
import com.xemophon.aljabr.ui.components.engine.CalculatorMode
import com.xemophon.aljabr.ui.components.screens.*

@Composable
fun PolyCalc(
    viewModel: CalcBoxViewModel = viewModel(),
    onOpenDrawer: () -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.calculatorMode = CalculatorMode.POLYNOMIALS
    }

    BackHandler(enabled = viewModel.polynomialResult != null) {
        viewModel.handleAction(CalcButtonAction.Clear)
    }

    PolyContent(
        displayText = viewModel.displayText,
        resultText = viewModel.resultText,
        cursorIndex = viewModel.cursorIndex,
        polynomialResult = viewModel.polynomialResult,
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
    onUpdateCursorIndex: (Int) -> Unit,
    onAction: (CalcButtonAction) -> Unit,
    onOpenDrawer: () -> Unit
) {
    CalculatorScaffold(
        title = { Text("Polynomial Solver") },
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
                    if (polynomialResult == null) {
                        CalcBox(
                            expression = displayText,
                            result = resultText,
                            cursorIndex = cursorIndex,
                            onCursorIndexChange = onUpdateCursorIndex,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        PolynomialReport(
                            result = polynomialResult,
                            onClear = { onAction(CalcButtonAction.Clear) }
                        )
                    }
                }

                if (polynomialResult == null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ShortCalcButtons(
                        modifier = Modifier.weight(1.3f),
                        gridMode = ShortGridMode.Polynomials,
                        onAction = onAction
                    )
                }
            }
        }
    }
}
