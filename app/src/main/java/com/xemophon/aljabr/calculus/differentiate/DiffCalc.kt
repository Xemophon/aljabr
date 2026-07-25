package com.xemophon.aljabr.calculus.differentiate

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
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
import com.hrm.latex.renderer.Latex
import com.hrm.latex.renderer.font.MathFont
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.model.LatexTheme
import com.xemophon.aljabr.ui.components.AdvancedButtonsGrid
import com.xemophon.aljabr.ui.components.AdvancedGridMode
import com.xemophon.aljabr.ui.components.CalcBoxViewModel
import com.xemophon.aljabr.ui.components.CalcButtonAction
import com.xemophon.aljabr.ui.components.CalculatorFocus
import com.xemophon.aljabr.ui.components.CalculatorMode
import com.xemophon.aljabr.ui.components.CalculatorScaffold
import com.xemophon.aljabr.ui.theme.AlJabrTheme
import com.xemophon.aljabr.utils.SymjaUtils

import androidx.compose.runtime.mutableIntStateOf
import com.xemophon.aljabr.calculus.differentiate.AnalysisResult

@Composable
fun DiffCalc(onOpenDrawer: () -> Unit) {
    val viewModel: CalcBoxViewModel = viewModel()
    
    LaunchedEffect(Unit) {
        viewModel.calculatorMode = CalculatorMode.DIFFERENTIATE
    }

    BackHandler(enabled = viewModel.analysisResult != null) {
        viewModel.handleAction(CalcButtonAction.Clear)
    }

    DiffCalcContent(
        displayText = viewModel.displayText,
        cursorIndex = viewModel.cursorIndex,
        diffGridMode = viewModel.diffGridMode,
        analysisResult = viewModel.analysisResult,
        onFocusChange = { viewModel.setFocus(it) },
        onCursorIndexChange = { viewModel.updateCursorIndex(it) },
        onAction = { viewModel.handleAction(it) },
        onOpenDrawer = onOpenDrawer
    )
}

@Composable
fun DiffCalcContent(
    displayText: String,
    cursorIndex: Int,
    diffGridMode: String,
    analysisResult: AnalysisResult?,
    onFocusChange: (CalculatorFocus) -> Unit,
    onCursorIndexChange: (Int) -> Unit,
    onAction: (CalcButtonAction) -> Unit,
    onOpenDrawer: () -> Unit
) {
    var isInverse by remember { mutableStateOf(false) }

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
                    if (analysisResult == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            DiffDisplay(
                                expression = displayText,
                                diffGridMode = diffGridMode,
                                cursorIndex = cursorIndex,
                                onFocusChange = onFocusChange,
                                onCursorIndexChange = onCursorIndexChange
                            )
                        }
                    } else {
                        AnalysisReport(
                            result = analysisResult,
                            onClear = { onAction(CalcButtonAction.Clear) }
                        )
                    }
                }
                
                if (analysisResult == null) {
                    AdvancedButtonsGrid(
                        isInverse = isInverse,
                        gridMode = AdvancedGridMode.Differentiation(diffGridMode),
                        onToggleInverse = { isInverse = !isInverse },
                        onAction = onAction
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
                AnalysisItemCard(deriv.name, deriv.expression, deriv.rawExpression)
            }

            // Maxima
            if (result.localMaxima.isNotEmpty()) {
                item { AnalysisSectionHeader("Local Maxima") }
                items(result.localMaxima) { point ->
                    AnalysisItemCard("Maximum", point)
                }
            }

            // Minima
            if (result.localMinima.isNotEmpty()) {
                item { AnalysisSectionHeader("Local Minima") }
                items(result.localMinima) { point ->
                    AnalysisItemCard("Minimum", point)
                }
            }

            // Inflection Points
            if (result.inflectionPoints.isNotEmpty()) {
                item { AnalysisSectionHeader("Inflection Points") }
                items(result.inflectionPoints) { point ->
                    AnalysisItemCard("Inflection", point)
                }
            }

            // Saddle Points
            if (result.saddlePoints.isNotEmpty()) {
                item { AnalysisSectionHeader("Saddle Points") }
                items(result.saddlePoints) { point ->
                    AnalysisItemCard("Saddle", point)
                }
            }

            // Other Stationary Points
            if (result.stationaryPoints.isNotEmpty()) {
                item { AnalysisSectionHeader("Stationary Points") }
                items(result.stationaryPoints) { point ->
                    AnalysisItemCard("Stationary", point)
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
fun AnalysisItemCard(label: String, value: String, rawValue: String? = null) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            
            val latexValue = remember(value, rawValue) {
                val toConvert = rawValue ?: value
                // Try converting to LaTeX if it looks like a math expression or we have a raw value
                if (rawValue != null || toConvert.any { it.isLetter() || it == '^' || it == '/' || it == '*' || it == '(' }) {
                    SymjaUtils.toLaTeX(toConvert)
                } else {
                    toConvert
                }
            }

            if (latexValue != value || value.contains("^") || value.contains("/")) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    Latex(
                        latex = latexValue,
                        config = LatexConfig(
                            fontSize = 20.sp,
                            theme = LatexTheme.light(color = MaterialTheme.colorScheme.onSurface),
                            mathFont = MathFont.KaTeXTTF
                        )
                    )
                }
            } else {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
                    fontWeight = FontWeight.Medium
                )
            }
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
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 48.sp),
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
        var displayText by remember { mutableStateOf("x^2") }
        var cursorIndex by remember { mutableIntStateOf(3) }
        var diffGridMode by remember { mutableStateOf("Single") }

        DiffCalcContent(
            displayText = displayText,
            cursorIndex = cursorIndex,
            diffGridMode = diffGridMode,
            analysisResult = null,
            onFocusChange = {},
            onCursorIndexChange = { cursorIndex = it },
            onAction = { action ->
                when (action) {
                    is CalcButtonAction.Symbol -> {
                        displayText += action.text
                        cursorIndex = displayText.length
                    }
                    is CalcButtonAction.Differentiate -> diffGridMode = "Multiple"
                    is CalcButtonAction.DifferentiateSingle -> diffGridMode = "Single"
                    is CalcButtonAction.Clear -> {
                        displayText = ""
                        cursorIndex = 0
                    }
                    else -> {}
                }
            },
            onOpenDrawer = {}
        )
    }
}
