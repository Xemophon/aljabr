package com.xemophon.aljabr.modules.calculus.laplace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaplaceCalc(onOpenDrawer: () -> Unit) {
    val viewModel: CalcBoxViewModel = viewModel()

    LaunchedEffect(Unit) {
        viewModel.calculatorMode = CalculatorMode.LAPLACE
//        if (viewModel.displayText == "0" || viewModel.displayText.isEmpty()) {
//            viewModel.handleAction(CalcButtonAction.Symbol("t"))
//        }
    }

    LaplaceCalcContent(
        displayText = viewModel.displayText,
        resultText = viewModel.resultText,
        laplaceMode = viewModel.laplaceMode,
        cursorIndex = viewModel.cursorIndex,
        onFocusChange = { viewModel.setFocus(it) },
        onCursorIndexChange = { viewModel.updateCursorIndex(it) },
        onAction = { viewModel.handleAction(it) },
        onOpenDrawer = onOpenDrawer
    )
}

@Composable
fun LaplaceCalcContent(
    displayText: String,
    resultText: String,
    laplaceMode: String,
    cursorIndex: Int,
    onFocusChange: (CalculatorFocus) -> Unit,
    onCursorIndexChange: (Int) -> Unit,
    onAction: (CalcButtonAction) -> Unit,
    onOpenDrawer: () -> Unit
) {
    var isInverse by remember { mutableStateOf(false) }

    CalculatorScaffold(
        title = { Text(if (laplaceMode == "Reverse") "Reverse Laplace Transform" else "Laplace Transform") },
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
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    LaplaceDisplay(
                        expression = displayText,
                        result = resultText,
                        laplaceMode = laplaceMode,
                        cursorIndex = cursorIndex,
                        onFocusChange = onFocusChange,
                        onCursorIndexChange = onCursorIndexChange
                    )
                }

                AdvancedButtonsGrid(
                    gridMode = AdvancedGridMode.Laplace(laplaceMode),
                    isInverse = isInverse,
                    onToggleInverse = { isInverse = !isInverse },
                    onAction = onAction
                )
            }
        }
    }
}

@Composable
fun LaplaceDisplay(
    expression: String,
    result: String,
    laplaceMode: String,
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
            Box(
                modifier = Modifier
                    .clickable {
                        onFocusChange(CalculatorFocus.EXPRESSION)
                        onCursorIndexChange(expression.length)
                    }
                    .padding(8.dp)
            ) {
                val base = if (expression == "0") "" else expression
                val textWithCursor = if (cursorIndex != -1) {
                    if (cursorIndex <= base.length && cursorIndex >= 0) {
                        StringBuilder(base).insert(cursorIndex, "|").toString()
                    } else {
                        "$base|"
                    }
                } else {
                    base.ifEmpty { if (laplaceMode == "Reverse") "F(s)" else "f(t)" }
                }

                Text(
                    text = textWithCursor,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = if (textWithCursor.length > 10) 24.sp else 32.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            val needsLatex = remember(result, expression) {
                (result + expression).any { it.isLetter() || it == '/' || it == '^' }
            }

            val prefix = if (laplaceMode == "Reverse") {
                val exprLatex = remember(expression) { SymjaUtils.toLaTeX(expression) }
                """\mathcal{L}^{-1}\left\{$exprLatex\right\} = """
            } else {
                val exprLatex = remember(expression) { SymjaUtils.toLaTeX(expression) }
                """\mathcal{L}\left\{$exprLatex\right\} = """
            }

            val latexState = produceState<String?>(initialValue = null, result, expression) {
                val resLatex = if (needsLatex) {
                    withContext(Dispatchers.Default) {
                        SymjaUtils.toLaTeX(result)
                    }
                } else {
                    result
                }
                value = prefix + resLatex
            }

            val latexContent = latexState.value

            Box(
                modifier = Modifier
                    .clickable { onFocusChange(CalculatorFocus.EXPRESSION) }
                    .padding(16.dp)
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center
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
