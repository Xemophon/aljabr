package com.xemophon.aljabr.modules.calculus.integrate

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
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
import com.xemophon.aljabr.ui.components.CalculusStep
import com.xemophon.aljabr.data.SymjaUtils
import com.xemophon.aljabr.ui.components.AdvancedButtonsGrid
import com.xemophon.aljabr.ui.components.AdvancedGridMode
import com.xemophon.aljabr.ui.components.CalcBoxViewModel
import com.xemophon.aljabr.ui.components.CalcButtonAction
import com.xemophon.aljabr.ui.components.CalculatorFocus
import com.xemophon.aljabr.ui.components.CalculatorMode
import com.xemophon.aljabr.ui.components.CalculatorScaffold
import com.xemophon.aljabr.ui.components.IntegralType
import com.xemophon.aljabr.ui.components.StepsBottomSheet
import com.xemophon.aljabr.ui.theme.AlJabrTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegCalc(onOpenDrawer: () -> Unit) {
    val viewModel: CalcBoxViewModel = viewModel()

    LaunchedEffect(Unit) {
        viewModel.calculatorMode = CalculatorMode.INTEGRATE
    }

    if (viewModel.showStepsSheet) {
        StepsBottomSheet(
            steps = viewModel.stepsList,
            isCalculating = viewModel.isCalculatingSteps,
            sheetState = rememberModalBottomSheetState(),
            onDismissRequest = { viewModel.showStepsSheet = false }
        )
    }

    IntegCalcContent(
        displayText = viewModel.displayText,
        lowerLimitText = viewModel.lowerLimitText,
        upperLimitText = viewModel.upperLimitText,
        resultText = viewModel.resultText,
        currentFocus = viewModel.currentFocus,
        integType = viewModel.integType,
        integrationAxis = viewModel.integrationAxis,
        cursorIndex = viewModel.cursorIndex,
        steps = viewModel.stepsList,
        isCalculatingSteps = viewModel.isCalculatingSteps,
        onShowStepsClick = { viewModel.showStepsSheet = true },
        onFocusChange = { viewModel.setFocus(it) },
        onCursorIndexChange = { viewModel.updateCursorIndex(it) },
        onAction = { viewModel.handleAction(it) },
        onOpenDrawer = onOpenDrawer
    )
}

@Composable
fun IntegCalcContent(
    displayText: String,
    lowerLimitText: String,
    upperLimitText: String,
    resultText: String,
    currentFocus: CalculatorFocus,
    integType: IntegralType,
    integrationAxis: String = "X",
    cursorIndex: Int,
    steps: List<CalculusStep> = emptyList(),
    isCalculatingSteps: Boolean = false,
    onShowStepsClick: () -> Unit = {},
    onFocusChange: (CalculatorFocus) -> Unit,
    onCursorIndexChange: (Int) -> Unit,
    onAction: (CalcButtonAction) -> Unit,
    onOpenDrawer: () -> Unit
) {
    var isInverse by remember { mutableStateOf(false) }

    CalculatorScaffold(
        title = { Text("Integrate") },
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
                    contentAlignment = Alignment.Center
                ) {
                    IntegDisplay(
                        expression = displayText,
                        lower = lowerLimitText,
                        upper = upperLimitText,
                        result = resultText,
                        focus = currentFocus,
                        integType = integType,
                        cursorIndex = cursorIndex,
                        onFocusChange = onFocusChange,
                        onCursorIndexChange = onCursorIndexChange,
                        showStepsButton = steps.isNotEmpty() || isCalculatingSteps,
                        onShowStepsClick = onShowStepsClick
                    )
                }
                AdvancedButtonsGrid(
                    isInverse = isInverse,
                    gridMode = AdvancedGridMode.Integration(integType, integrationAxis),
                    onToggleInverse = { isInverse = !isInverse },
                    onAction = onAction
                )
            }
        }
    }
}

@Composable
fun IntegDisplay(
    expression: String,
    lower: String,
    upper: String,
    result: String,
    focus: CalculatorFocus,
    integType: IntegralType,
    cursorIndex: Int,
    onFocusChange: (CalculatorFocus) -> Unit,
    onCursorIndexChange: (Int) -> Unit,
    showStepsButton: Boolean = false,
    onShowStepsClick: () -> Unit = {}
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
                    // Integral Symbol with Limits
                    Box(
                        modifier = Modifier.height(100.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "∫",
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 80.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (integType != IntegralType.INDEFINITE) {
                            // Upper limit b
                            Text(
                                text = upper.ifEmpty { "b" },
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset(x = 25.dp, y = (-8).dp)
                                    .clickable { onFocusChange(CalculatorFocus.INTEG_UPPER) },
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 18.sp),
                                fontWeight = if (focus == CalculatorFocus.INTEG_UPPER) FontWeight.Bold else FontWeight.Normal,
                                color = if (focus == CalculatorFocus.INTEG_UPPER) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            // Lower limit a
                            Text(
                                text = lower.ifEmpty { "a" },
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .offset(x = (-10).dp, y = 18.dp)
                                    .clickable { onFocusChange(CalculatorFocus.INTEG_LOWER) },
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 18.sp),
                                fontWeight = if (focus == CalculatorFocus.INTEG_LOWER) FontWeight.Bold else FontWeight.Normal,
                                color = if (focus == CalculatorFocus.INTEG_LOWER) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Expression f(x)dx
                    Box(
                        modifier = Modifier
                            .clickable {
                                onFocusChange(CalculatorFocus.EXPRESSION)
                                onCursorIndexChange(expression.length)
                            }
                            .padding(8.dp)
                    ) {
                        val base = if (expression == "0") "" else expression
                        val textWithCursor =
                            if (focus == CalculatorFocus.EXPRESSION && cursorIndex != -1) {
                                if (cursorIndex < base.length) {
                                    StringBuilder(base).insert(cursorIndex, "|").toString()
                                } else {
                                    "$base|"
                                }
                            } else {
                                base.ifEmpty { "f(x)" }
                            }

                        val displayText = when (integType) {
                            IntegralType.DEFINITE, IntegralType.INDEFINITE -> "$textWithCursor dx"
                            IntegralType.ARC -> "√[1 + ($textWithCursor)']² dx"
                            IntegralType.XVOL -> "π[$textWithCursor]² dx"
                            IntegralType.YVOL -> "2πx|$textWithCursor| dx"
                            IntegralType.XSURF -> "2π|$textWithCursor|√[1 + ($textWithCursor)']² dx"
                            IntegralType.YSURF -> "2π|x|√[1 + ($textWithCursor)']² dx"
                        }

                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontSize = if (displayText.length > 15) 24.sp else if (displayText.length > 10) 32.sp else 48.sp
                            ),
                            color = if (focus == CalculatorFocus.EXPRESSION) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (focus == CalculatorFocus.EXPRESSION) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            } else {
                // Display only the result when it exists using LaTeX if possible
                val needsLatex = remember(result, integType) {
                    if (integType == IntegralType.INDEFINITE && result.endsWith(" + C")) {
                        val expr = result.removeSuffix(" + C")
                        expr.any { it.isLetter() }
                    } else {
                        result.any { it.isLetter() || it == '/' || it == '^' }
                    }
                }

                val latexState = produceState<String?>(initialValue = if (!needsLatex) result else null, result) {
                    if (needsLatex) {
                        val converted = if (integType == IntegralType.INDEFINITE && result.endsWith(" + C")) {
                            val expr = result.removeSuffix(" + C")
                            val res = withContext(Dispatchers.Default) {
                                SymjaUtils.toLaTeX(expr)
                            }
                            "$res + C"
                        } else {
                            withContext(Dispatchers.Default) {
                                SymjaUtils.toLaTeX(result)
                            }
                        }
                        value = converted
                    } else {
                        value = result
                    }
                }

                val latexContent = latexState.value

                Box(
                    modifier = Modifier
                        .clickable {
                            // Clear result to edit again
                            onFocusChange(CalculatorFocus.EXPRESSION)
                        }
                        .padding(16.dp)
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    if (latexContent != null) {
                        if (needsLatex || (latexContent != result || result.any { it == '^' || it == '/' })) {
                            Box(modifier = Modifier.widthIn(max = 2000.dp)) {
                                Latex(
                                    latex = latexContent,
                                    config = LatexConfig(
                                        fontSize = if (result.length > 15) 24.sp else 32.sp,
                                        theme = LatexTheme.light(color = MaterialTheme.colorScheme.secondary),
                                    )
                                )
                            }
                        } else {
                            Text(
                                text = result,
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontSize = if (result.length > 15) 28.sp else 40.sp
                                ),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun IntegPreview() {
    AlJabrTheme {
        var displayText by remember { mutableStateOf("x^2") }
        var lowerLimitText by remember { mutableStateOf("0") }
        var upperLimitText by remember { mutableStateOf("1") }
        var resultText by remember { mutableStateOf("") }
        var currentFocus by remember { mutableStateOf(CalculatorFocus.EXPRESSION) }
        var integType by remember { mutableStateOf(IntegralType.DEFINITE) }
        var cursorIndex by remember { mutableIntStateOf(3) }

        IntegCalcContent(
            displayText = displayText,
            lowerLimitText = lowerLimitText,
            upperLimitText = upperLimitText,
            resultText = resultText,
            currentFocus = currentFocus,
            integType = integType,
            integrationAxis = "X",
            cursorIndex = cursorIndex,
            onFocusChange = { currentFocus = it },
            onCursorIndexChange = { cursorIndex = it },
            onAction = { action ->
                when (action) {
                    is CalcButtonAction.Symbol -> {
                        when (currentFocus) {
                            CalculatorFocus.EXPRESSION -> displayText += action.text
                            CalculatorFocus.INTEG_LOWER -> lowerLimitText += action.text
                            CalculatorFocus.INTEG_UPPER -> upperLimitText += action.text
                            else -> {}
                        }
                    }
                    is CalcButtonAction.Integrals -> integType = action.type
                    is CalcButtonAction.Clear -> {
                        displayText = ""
                        lowerLimitText = ""
                        upperLimitText = ""
                        resultText = ""
                    }
                    else -> {}
                }
            },
            onOpenDrawer = {}
        )
    }
}
