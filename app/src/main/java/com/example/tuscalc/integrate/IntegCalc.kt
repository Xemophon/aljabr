package com.example.tuscalc.integrate

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tuscalc.ui.components.*
import com.example.tuscalc.ui.theme.TUsCalcTheme

@Composable
fun IntegCalc(onOpenDrawer: () -> Unit) {
    val viewModel: CalcBoxViewModel = viewModel()
    var isInverse by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.calculatorMode = CalculatorMode.INTEGRATE
    }

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
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    IntegDisplay(
                        expression = viewModel.displayText,
                        lower = viewModel.lowerLimitText,
                        upper = viewModel.upperLimitText,
                        result = viewModel.resultText,
                        focus = viewModel.currentFocus,
                        integType = viewModel.integType,
                        cursorIndex = viewModel.cursorIndex,
                        onFocusChange = { viewModel.setFocus(it) },
                        onCursorIndexChange = { viewModel.updateCursorIndex(it) }
                    )
                }
                CalcButtonsIntegrate(
                    isInverse = isInverse,
                    onToggleInverse = { isInverse = !isInverse },
                    onAction = { viewModel.handleAction(it) }
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
    onCursorIndexChange: (Int) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
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

                    if (integType == IntegralType.DEFINITE) {
                        // Upper limit b
                        Text(
                            text = upper.ifEmpty { "b" },
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = 22.dp, y = (-10).dp)
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
                                .offset(x = 10.dp, y = 10.dp)
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
                    val textWithCursor = if (focus == CalculatorFocus.EXPRESSION && cursorIndex != -1) {
                        if (cursorIndex < base.length) {
                            StringBuilder(base).insert(cursorIndex, "|").toString()
                        } else {
                            "$base|"
                        }
                    } else {
                        base.ifEmpty { "f(x)" }
                    }

                    val displayText = "$textWithCursor dx"

                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = if (expression.length > 10) 32.sp else 48.sp
                        ),
                        color = if (focus == CalculatorFocus.EXPRESSION) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (focus == CalculatorFocus.EXPRESSION) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        } else {
            // Display only the result when it exists
            Text(
                text = result,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = if (result.length > 10) 32.sp else 48.sp
                ),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.clickable { 
                    // Clicking the result can reset focus to edit the expression again
                    onFocusChange(CalculatorFocus.EXPRESSION)
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun IntegPreview(){
    TUsCalcTheme() {
        IntegCalc(onOpenDrawer = {})
    }
}