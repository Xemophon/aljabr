package com.xemophon.aljabr.modules.basicCalc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xemophon.aljabr.ui.components.screens.CalcBox
import com.xemophon.aljabr.ui.components.screens.CalcBoxViewModel
import com.xemophon.aljabr.ui.components.buttons.CalcButtonAction
import com.xemophon.aljabr.ui.components.buttons.CalcButtons
import com.xemophon.aljabr.ui.components.screens.CalculatorScaffold
import com.xemophon.aljabr.ui.theme.AlJabrTheme

@Composable
fun BasicCalc(
    viewModel: CalcBoxViewModel = viewModel(),
    onOpenDrawer: () -> Unit
) {
    BasicCalcContent(
        displayText = viewModel.displayText,
        resultText = viewModel.resultText,
        cursorIndex = viewModel.cursorIndex,
        useRadians = viewModel.useRadians,
        onUpdateCursorIndex = { viewModel.updateCursorIndex(it) },
        onToggleAngleUnit = { viewModel.toggleAngleUnit() },
        onAction = { viewModel.handleAction(it) },
        onOpenDrawer = onOpenDrawer
    )
}

@Composable
fun BasicCalcContent(
    displayText: String,
    resultText: String,
    cursorIndex: Int,
    useRadians: Boolean,
    onUpdateCursorIndex: (Int) -> Unit,
    onToggleAngleUnit: () -> Unit,
    onAction: (CalcButtonAction) -> Unit,
    onOpenDrawer: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isInverse by remember { mutableStateOf(false) }

    CalculatorScaffold(
        title = { Text("Basic Calculator") },
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
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CalcBox(
                    expression = displayText,
                    result = resultText,
                    cursorIndex = cursorIndex,
                    onCursorIndexChange = onUpdateCursorIndex,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.height(8.dp))
                CalcButtons(
                    isExpanded = isExpanded,
                    isInverse = isInverse,
                    useRadians = useRadians,
                    onToggleExpand = { isExpanded = !isExpanded },
                    onToggleInverse = { isInverse = !isInverse },
                    onToggleAngleUnit = onToggleAngleUnit,
                    modifier = Modifier.fillMaxWidth(),
                    onAction = onAction
                )
            }
        }  
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun BasicCalcPreview() {
    AlJabrTheme {
        var displayText by remember { mutableStateOf("2+2") }
        var resultText by remember { mutableStateOf("4") }
        var cursorIndex by remember { mutableIntStateOf(3) }
        var useRadians by remember { mutableStateOf(false) }

        BasicCalcContent(
            displayText = displayText,
            resultText = resultText,
            cursorIndex = cursorIndex,
            useRadians = useRadians,
            onUpdateCursorIndex = { cursorIndex = it },
            onToggleAngleUnit = { useRadians = !useRadians },
            onAction = { action ->
                when (action) {
                    is CalcButtonAction.Symbol -> {
                        displayText += action.text
                        cursorIndex = displayText.length
                    }
                    is CalcButtonAction.Clear -> {
                        displayText = "0"
                        resultText = ""
                        cursorIndex = 1
                    }
                    else -> {}
                }
            },
            onOpenDrawer = {}
        )
    }
}
