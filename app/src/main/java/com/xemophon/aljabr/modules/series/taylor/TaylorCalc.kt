package com.xemophon.aljabr.modules.series.taylor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import com.xemophon.aljabr.ui.components.screens.CalcBoxViewModel
import com.xemophon.aljabr.ui.components.screens.CalculatorFocus
import com.xemophon.aljabr.ui.components.screens.CalculatorMode
import com.xemophon.aljabr.ui.components.screens.CalculatorScaffold
import com.xemophon.aljabr.ui.components.screens.ConverterField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun TaylorCalc(
    viewModel: CalcBoxViewModel = viewModel(),
    onOpenDrawer: () -> Unit
) {
    var isInverse by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.calculatorMode = CalculatorMode.TAYLOR
        if (viewModel.displayText == "0" || viewModel.displayText.isEmpty()) {
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
                    ConverterField(
                        label = "Center (a)",
                        value = viewModel.targetText,
                        isFocused = viewModel.currentFocus == CalculatorFocus.TARGET,
                        cursorIndex = -1,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.setFocus(CalculatorFocus.TARGET) }
                    )
                    ConverterField(
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
                    isInverse = isInverse,
                    onToggleInverse = { isInverse = !isInverse },
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
            // Result View with LaTeX
            val needsLatex = remember(result) {
                result.any { it.isLetter() || it == '/' || it == '^' }
            }

            // Build the formal summation prefix in LaTeX
            val aLatex = remember(center) {
                if (center.isEmpty()) "a" else center.replace("pi", "\\pi").replace("e", "e")
            }
            val nLatex = remember(order) { if (order.isEmpty()) "n" else order }
            val prefix = "\\sum_{k=0}^{$nLatex} \\frac{f^{(k)}($aLatex)}{k!} (x - $aLatex)^k = "

            val latexState = produceState<String?>(initialValue = null, result) {
                val expansion = if (needsLatex) {
                    withContext(Dispatchers.Default) {
                        SymjaUtils.toLaTeX(result)
                    }
                } else {
                    result
                }
                value = prefix + expansion
            }

            val latexContent = latexState.value

            Box(
                modifier = Modifier
                    .clickable { onFocusChange(CalculatorFocus.EXPRESSION) }
                    .padding(16.dp)
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                contentAlignment = Alignment.CenterStart
            ) {
                if (latexContent != null) {
                    Latex(
                        latex = latexContent,
                        config = LatexConfig(
                            fontSize = if (result.length > 20) 18.sp else 24.sp,
                            theme = LatexTheme.light(color = MaterialTheme.colorScheme.primary),
                        )
                    )
                }
            }
        }
    }
}
