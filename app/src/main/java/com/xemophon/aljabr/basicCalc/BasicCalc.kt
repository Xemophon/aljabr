package com.xemophon.aljabr.basicCalc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xemophon.aljabr.ui.components.CalcBox
import com.xemophon.aljabr.ui.components.CalcBoxViewModel
import com.xemophon.aljabr.ui.components.CalcButtons
import com.xemophon.aljabr.ui.components.CalculatorScaffold
import com.xemophon.aljabr.ui.theme.AlJabrTheme

@Composable
fun BasicCalc(
    viewModel: CalcBoxViewModel = viewModel(),
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
                    .fillMaxSize()
                    .safeDrawingPadding(),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CalcBox(
                    expression = viewModel.displayText,
                    result = viewModel.resultText,
                    cursorIndex = viewModel.cursorIndex,
                    onCursorIndexChange = { viewModel.updateCursorIndex(it) },
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.height(8.dp))
                CalcButtons(
                    isExpanded = isExpanded,
                    isInverse = isInverse,
                    onToggleExpand = { isExpanded = !isExpanded },
                    onToggleInverse = { isInverse = !isInverse },
                    modifier = Modifier.fillMaxWidth(),
                    onAction = { action ->
                        viewModel.handleAction(action)
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun BasicCalcPreview() {
    AlJabrTheme {
        BasicCalc(onOpenDrawer = {})
    }
}
