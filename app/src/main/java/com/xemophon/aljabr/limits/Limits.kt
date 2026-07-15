package com.xemophon.aljabr.limits

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
import com.xemophon.aljabr.ui.components.AdvancedButtonsGrid
import com.xemophon.aljabr.ui.components.CalcBoxViewModel
import com.xemophon.aljabr.ui.components.CalculatorFocus
import com.xemophon.aljabr.ui.components.CalculatorMode
import com.xemophon.aljabr.ui.components.CalculatorScaffold
import com.xemophon.aljabr.ui.theme.AlJabrTheme

@Composable
fun Limits(onOpenDrawer: () -> Unit) {
    val viewModel: CalcBoxViewModel = viewModel()
    var isInverse by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.calculatorMode = CalculatorMode.LIMITS
    }

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
                        expression = viewModel.displayText,
                        target = viewModel.targetText,
                        result = viewModel.resultText,
                        focus = viewModel.currentFocus,
                        cursorIndex = viewModel.cursorIndex,
                        onFocusChange = { viewModel.setFocus(it) },
                        onCursorIndexChange = { viewModel.updateCursorIndex(it) }
                    )
                }
                AdvancedButtonsGrid(
                    isInverse = isInverse,
                    mode = "Limits",
                    limitType = viewModel.limitType,
                    onToggleInverse = { isInverse = !isInverse },
                    onAction = { viewModel.handleAction(it) }
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

            if (result.isNotEmpty()) {
                val resultStyle = MaterialTheme.typography.displayMedium.copy(
                    fontSize = if (expression.length > 10) 32.sp else 48.sp
                )
                Text(
                    text = " = $result",
                    style = resultStyle,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LimitPreview() {
    AlJabrTheme {
        Limits(onOpenDrawer = {})
    }
}
