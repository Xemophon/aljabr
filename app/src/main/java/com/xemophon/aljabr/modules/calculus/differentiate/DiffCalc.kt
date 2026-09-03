package com.xemophon.aljabr.modules.calculus.differentiate

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.model.LatexTheme
import com.xemophon.aljabr.ui.components.screens.CalculusStep
import com.xemophon.aljabr.data.SymjaUtils
import com.xemophon.aljabr.ui.components.buttons.AdvancedButtonsGrid
import com.xemophon.aljabr.ui.components.buttons.AdvancedGridMode
import com.xemophon.aljabr.ui.components.screens.AnalysisReport
import com.xemophon.aljabr.ui.components.screens.AnalysisResult
import com.xemophon.aljabr.ui.components.screens.CalcBoxViewModel
import com.xemophon.aljabr.ui.components.buttons.CalcButtonAction
import com.xemophon.aljabr.ui.components.screens.CalculatorFocus
import com.xemophon.aljabr.ui.components.screens.CalculatorMode
import com.xemophon.aljabr.ui.components.screens.CalculatorScaffold
import com.xemophon.aljabr.ui.components.screens.StepsBottomSheet
import com.xemophon.aljabr.ui.theme.AlJabrTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiffCalc(onOpenDrawer: () -> Unit) {
    val viewModel: CalcBoxViewModel = viewModel()
    
    LaunchedEffect(Unit) {
        viewModel.calculatorMode = CalculatorMode.DIFFERENTIATE
    }

    if (viewModel.showStepsSheet) {
        StepsBottomSheet(
            steps = viewModel.stepsList,
            isCalculating = viewModel.isCalculatingSteps,
            sheetState = rememberModalBottomSheetState(),
            onDismissRequest = { viewModel.showStepsSheet = false }
        )
    }

    BackHandler(enabled = viewModel.analysisResult != null) {
        viewModel.handleAction(CalcButtonAction.Clear)
    }

    DiffCalcContent(
        displayText = viewModel.displayText,
        cursorIndex = viewModel.cursorIndex,
        resultText = viewModel.resultText,
        diffGridMode = viewModel.diffGridMode,
        analysisResult = viewModel.analysisResult,
        steps = viewModel.stepsList,
        isCalculatingSteps = viewModel.isCalculatingSteps,
        onShowStepsClick = { viewModel.showStepsSheet = true },
        onFocusChange = { viewModel.setFocus(it) },
        onCursorIndexChange = { viewModel.updateCursorIndex(it) },
        onRunAnalysis = { viewModel.runFullAnalysis() },
        onAction = { viewModel.handleAction(it) },
        onOpenDrawer = onOpenDrawer
    )
}

@Composable
fun DiffCalcContent(
    displayText: String,
    cursorIndex: Int,
    resultText: String = "",
    diffGridMode: String,
    analysisResult: AnalysisResult?,
    steps: List<CalculusStep> = emptyList(),
    isCalculatingSteps: Boolean = false,
    onShowStepsClick: () -> Unit = {},
    onFocusChange: (CalculatorFocus) -> Unit,
    onCursorIndexChange: (Int) -> Unit,
    onRunAnalysis: () -> Unit = {},
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
                modifier = Modifier
                    .fillMaxSize()
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
                                result = resultText,
                                diffGridMode = diffGridMode,
                                cursorIndex = cursorIndex,
                                onFocusChange = onFocusChange,
                                onCursorIndexChange = onCursorIndexChange,
                                showStepsButton = steps.isNotEmpty() || isCalculatingSteps,
                                onShowStepsClick = onShowStepsClick,
                                onRunAnalysis = onRunAnalysis
                            )
                        }
                    } else {
                        AnalysisReport(
                            result = analysisResult,
                            steps = steps,
                            isCalculatingSteps = isCalculatingSteps,
                            onShowStepsClick = onShowStepsClick,
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
fun DiffDisplay(
    expression: String,
    result: String = "",
    diffGridMode: String,
    cursorIndex: Int,
    onFocusChange: (CalculatorFocus) -> Unit,
    onCursorIndexChange: (Int) -> Unit,
    showStepsButton: Boolean = false,
    onShowStepsClick: () -> Unit = {},
    onRunAnalysis: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (showStepsButton) {
            IconButton(
                onClick = onShowStepsClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = "Show Steps",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (result.isEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Derivative Notation
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "∂",
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
                            text = when(diffGridMode){
                                "Single" -> "∂x"
                                "Complex" -> "∂z"
                                else -> "∂f"
                            },
                            style = MaterialTheme.typography.displaySmall.copy(fontSize = 32.sp),
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
            } else {
                // Show Derivative Result
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val needsLatex = remember(result) {
                        result.any { it.isLetter() || it == '^' || it == '/' }
                    }
                    val latexState = produceState<String?>(initialValue = if (!needsLatex) result else null, result) {
                        if (needsLatex) {
                            value = withContext(Dispatchers.Default) {
                                SymjaUtils.toLaTeX(result)
                            }
                        } else {
                            value = result
                        }
                    }
                    val latexValue = latexState.value

                    Box(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center
                    ) {
                        if (latexValue != null) {
                            if (needsLatex || (latexValue != result || result.contains("^") || result.contains("/"))) {
                                Box(modifier = Modifier.widthIn(max = 2000.dp)) {
                                    Latex(
                                        latex = latexValue,
                                        config = LatexConfig(
                                            fontSize = if (result.length > 15) 24.sp else 32.sp,
                                            theme = LatexTheme.light(color = MaterialTheme.colorScheme.primary),
                                        )
                                    )
                                }
                            } else {
                                Text(
                                    text = result,
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontSize = if (result.length > 10) 32.sp else 48.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    ElevatedCard(
                        onClick = onRunAnalysis,
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Text(
                            text = "View Detailed Analysis",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
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
            resultText = "",
            diffGridMode = diffGridMode,
            analysisResult = null,
            onFocusChange = {},
            onCursorIndexChange = { cursorIndex = it },
            onRunAnalysis = {},
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
