package com.xemophon.aljabr.differentiate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
fun DiffCalc(onOpenDrawer: () -> Unit) {
    val viewModel: CalcBoxViewModel = viewModel()
    var isInverse by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.calculatorMode = CalculatorMode.DIFFERENTIATE
    }

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
                    contentAlignment = Alignment.Center
                ) {
                    DiffDisplay(
                        expression = viewModel.displayText,
                        result = viewModel.resultText,
                        cursorIndex = viewModel.cursorIndex,
                        onFocusChange = { viewModel.setFocus(it) },
                        onCursorIndexChange = { viewModel.updateCursorIndex(it) }
                    )
                }
                AdvancedButtonsGrid(
                    isInverse = isInverse,
                    mode = "Differentiation",
                    onToggleInverse = { isInverse = !isInverse },
                    onAction = { viewModel.handleAction(it) }
                )
            }
        }
    }
}

@Composable
fun DiffDisplay(
    expression: String,
    result: String,
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Derivative Notation d/dx
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
            // Display only the result when it exists
            Text(
                text = result,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = if (result.length > 15) 28.sp else 40.sp
                ),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.clickable {
                    // Clear result to edit again
                    onFocusChange(CalculatorFocus.EXPRESSION)
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DiffPreview() {
    AlJabrTheme {
        DiffCalc(onOpenDrawer = {})
    }
}
