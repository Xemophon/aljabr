package com.xemophon.aljabr.modules.series.taylor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hrm.latex.renderer.Latex
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.model.LatexTheme
import com.xemophon.aljabr.data.SymjaUtils
import com.xemophon.aljabr.ui.components.buttons.AdvancedButtonsGrid
import com.xemophon.aljabr.ui.components.buttons.AdvancedGridMode
import com.xemophon.aljabr.ui.components.buttons.CalcButtonAction
import com.xemophon.aljabr.ui.components.engine.CalcBoxViewModel
import com.xemophon.aljabr.ui.components.engine.CalculatorFocus
import com.xemophon.aljabr.ui.components.engine.CalculatorMode
import com.xemophon.aljabr.ui.components.screens.CalculatorScaffold
import com.xemophon.aljabr.ui.components.screens.ScrollableLatexView
import com.xemophon.aljabr.ui.components.screens.InputFieldSmall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun TaylorCalc(
    viewModel: CalcBoxViewModel = viewModel(),
    onOpenDrawer: () -> Unit,
) {
    LaunchedEffect(Unit) {
        viewModel.calculatorMode = CalculatorMode.TAYLOR
        if ((viewModel.displayText == "0") || viewModel.displayText.isEmpty()) {
            viewModel.handleAction(CalcButtonAction.Symbol("sin(x)"))
        }
    }

    CalculatorScaffold(
        title = { Text("Taylor Series") },
        onOpenDrawer = onOpenDrawer
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TaylorDisplay(
                        expression = viewModel.displayText,
                        center = viewModel.targetText,
                        order = viewModel.orderText,
                        result = viewModel.resultText,
                        focus = viewModel.currentFocus,
                        cursorIndex = viewModel.cursorIndex,
                        onFocusChange = { viewModel.setFocus(it) },
                        onCursorIndexChange = { viewModel.updateCursorIndex(it) }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InputFieldSmall(
                        label = "Center (a)",
                        value = viewModel.targetText,
                        isFocused = viewModel.currentFocus == CalculatorFocus.TARGET,
                        cursorIndex = -1,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.setFocus(CalculatorFocus.TARGET) }
                    )
                    InputFieldSmall(
                        label = "Order (n)",
                        value = viewModel.orderText,
                        isFocused = viewModel.currentFocus == CalculatorFocus.ORDER,
                        cursorIndex = -1,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.setFocus(CalculatorFocus.ORDER) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                AdvancedButtonsGrid(
                    gridMode = AdvancedGridMode.Taylor,
                    onAction = { viewModel.handleAction(it) }
                )
            }
        }
    }
}

@Composable
fun TaylorDisplay(
    expression: String,
    center: String,
    order: String,
    result: String,
    focus: CalculatorFocus,
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
        if (result.isEmpty()) {
            // Function Definition only
            Box(
                modifier = Modifier
                    .clickable {
                        onFocusChange(CalculatorFocus.EXPRESSION)
                        onCursorIndexChange(expression.length)
                    }
                    .padding(8.dp)
            ) {
                val base = if (expression == "0") "" else expression
                val textWithCursor = if (focus == CalculatorFocus.EXPRESSION && cursorIndex != -1) {
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
                        fontSize = if (textWithCursor.length > 10) 24.sp else 32.sp
                    ),
                    color = if (focus == CalculatorFocus.EXPRESSION) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            // Build the formal summation prefix in LaTeX
            val aLatex = remember(center) {
                if (center.isBlank() || center == "0") "0" else SymjaUtils.toLaTeX(center)
            }
            val nLatex = remember(order) { order.ifEmpty { "n" } }
            val resultLatex = remember(result) {
                if (result == "Error") "Error" else SymjaUtils.toLaTeX(result)
            }
            val centerTerm = if (center.isBlank() || center == "0") "x" else "(x - $aLatex)"
            val prefix = "\\sum_{k=0}^{$nLatex} \\frac{f^{(k)}($aLatex)}{k!} $centerTerm^k = "

            ScrollableLatexView(
                expression = prefix + resultLatex,
                isAlreadyLatex = true,
                fontSize = if (result.length > 20) 18.sp else 24.sp,
                color = MaterialTheme.colorScheme.primary,
                onClick = { onFocusChange(CalculatorFocus.EXPRESSION) },
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
