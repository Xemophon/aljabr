package com.xemophon.aljabr.algebra.polynomials

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xemophon.aljabr.ui.components.CalcBox
import com.xemophon.aljabr.ui.components.CalcBoxViewModel
import com.xemophon.aljabr.ui.components.CalcButtonAction
import com.xemophon.aljabr.ui.components.CalculatorMode
import com.xemophon.aljabr.ui.components.CalculatorScaffold
import com.xemophon.aljabr.ui.components.PolynomialReport
import com.xemophon.aljabr.ui.components.PolynomialResult
import com.xemophon.aljabr.ui.components.ShortCalcButtons
import com.xemophon.aljabr.ui.components.ShortGridMode
import com.xemophon.aljabr.ui.theme.AlJabrTheme

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

@Preview(showBackground = true)
@Composable
fun PolyCalcPreview() {
    AlJabrTheme {
        PolyContent(
            displayText = "x^2+2*x+1",
            resultText = "(x+1)^2",
            cursorIndex = 5,
            polynomialResult = null,
            onUpdateCursorIndex = {},
            onAction = {},
            onOpenDrawer = {}
        )
    }
}
