package com.xemophon.aljabr.modules.calculus.limits

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xemophon.aljabr.ui.components.AdvancedButtonsGrid
import com.xemophon.aljabr.ui.components.AdvancedGridMode
import com.xemophon.aljabr.ui.components.CalcBoxViewModel
import com.xemophon.aljabr.ui.components.CalcButtonAction
import com.xemophon.aljabr.ui.components.CalculatorFocus
import com.xemophon.aljabr.ui.components.CalculatorMode
import com.xemophon.aljabr.ui.components.CalculatorScaffold
import com.xemophon.aljabr.ui.components.LimitType
import com.xemophon.aljabr.ui.theme.AlJabrTheme

@Composable
fun Limits(onOpenDrawer: () -> Unit) {
    val viewModel: CalcBoxViewModel = viewModel()

    LaunchedEffect(Unit) {
        viewModel.calculatorMode = CalculatorMode.LIMITS
    }

    LimitsContent(
        displayText = viewModel.displayText,
        targetText = viewModel.targetText,
        resultText = viewModel.resultText,
        currentFocus = viewModel.currentFocus,
        limitType = viewModel.limitType,
        cursorIndex = viewModel.cursorIndex,
        onFocusChange = { viewModel.setFocus(it) },
        onCursorIndexChange = { viewModel.updateCursorIndex(it) },
        onAction = { viewModel.handleAction(it) },
        onOpenDrawer = onOpenDrawer
    )
}

@Composable
fun LimitsContent(
    displayText: String,
    targetText: String,
    resultText: String,
    currentFocus: CalculatorFocus,
    limitType: LimitType,
    cursorIndex: Int,
    onFocusChange: (CalculatorFocus) -> Unit,
    onCursorIndexChange: (Int) -> Unit,
    onAction: (CalcButtonAction) -> Unit,
    onOpenDrawer: () -> Unit
) {
    var isInverse by remember { mutableStateOf(false) }


    CalculatorScaffold(
        title = { Text("Limits") },
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
                    LimitDisplay(
                        expression = displayText,
                        target = targetText,
                        result = resultText,
                        focus = currentFocus,
                        cursorIndex = cursorIndex,
                        onFocusChange = onFocusChange,
                        onCursorIndexChange = onCursorIndexChange
                    )
                }
                AdvancedButtonsGrid(
                    isInverse = isInverse,
                    gridMode = AdvancedGridMode.Limits(limitType),
                    onToggleInverse = { isInverse = !isInverse },
                    onAction = onAction
                )
            }
        }
    }
}

@Composable
fun LimitDisplay(
    expression: String,
    target: String,
    result: String,
    focus: CalculatorFocus,
    cursorIndex: Int,
    onFocusChange: (CalculatorFocus) -> Unit,
    onCursorIndexChange: (Int) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        if (result.isEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Limit Notation (lim x->a)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onFocusChange(CalculatorFocus.TARGET) }
                ) {
                    Text(
                        text = "lim",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (focus == CalculatorFocus.TARGET) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "x ➔ ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (focus == CalculatorFocus.TARGET) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp
                        )
                        Text(
                            text = target.ifEmpty { "a" },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (focus == CalculatorFocus.TARGET) FontWeight.Bold else FontWeight.Normal,
                            color = if (focus == CalculatorFocus.TARGET) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Expression f(x)
                Box(
                    modifier = Modifier
                        .clickable {
                            onFocusChange(CalculatorFocus.EXPRESSION)
                            onCursorIndexChange(expression.length)
                        }
                        .padding(8.dp)
                ) {
                    val displayText = if (focus == CalculatorFocus.EXPRESSION && cursorIndex != -1) {
                        if (cursorIndex < expression.length) {
                            StringBuilder(expression).insert(cursorIndex, "|").toString()
                        } else {
                            "$expression|"
                        }
                    } else {
                        expression.ifEmpty { "f(x)" }
                    }

                    val textStyle = MaterialTheme.typography.displayMedium.copy(
                        fontSize = if (expression.length > 10) 32.sp else 48.sp
                    )

                    Text(
                        text = displayText,
                        style = textStyle,
                        color = if (focus == CalculatorFocus.EXPRESSION) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (focus == CalculatorFocus.EXPRESSION) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        } else {
            // Result only display
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = result,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = if (result.length > 8) 48.sp else 64.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LimitPreview() {
    AlJabrTheme {
        var displayText by remember { mutableStateOf("sin(x)/x") }
        var targetText by remember { mutableStateOf("0") }
        var resultText by remember { mutableStateOf("1") }
        var currentFocus by remember { mutableStateOf(CalculatorFocus.EXPRESSION) }
        var limitType by remember { mutableStateOf(LimitType.FINITE) }
        var cursorIndex by remember { mutableIntStateOf(8) }

        LimitsContent(
            displayText = displayText,
            targetText = targetText,
            resultText = resultText,
            currentFocus = currentFocus,
            limitType = limitType,
            cursorIndex = cursorIndex,
            onFocusChange = { currentFocus = it },
            onCursorIndexChange = { cursorIndex = it },
            onAction = { action ->
                when (action) {
                    is CalcButtonAction.Symbol -> {
                        if (currentFocus == CalculatorFocus.EXPRESSION) {
                            displayText += action.text
                        } else {
                            targetText += action.text
                        }
                    }
                    is CalcButtonAction.Limits -> {
                        limitType = action.type
                        if (action.type == LimitType.INFINITE) targetText = "∞"
                    }
                    is CalcButtonAction.Clear -> {
                        displayText = ""
                        targetText = ""
                        resultText = ""
                    }
                    else -> {}
                }
            },
            onOpenDrawer = {}
        )
    }
}
