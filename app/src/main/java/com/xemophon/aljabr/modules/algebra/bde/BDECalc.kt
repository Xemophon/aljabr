package com.xemophon.aljabr.modules.algebra.bde

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xemophon.aljabr.ui.components.buttons.CalcButtonAction
import com.xemophon.aljabr.ui.components.buttons.ShortCalcButtons
import com.xemophon.aljabr.ui.components.buttons.ShortGridMode
import com.xemophon.aljabr.ui.components.screens.*

@Composable
fun BDECalc(
    viewModel: CalcBoxViewModel = viewModel(),
    onOpenDrawer: () -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.calculatorMode = CalculatorMode.ODE
    }

    BackHandler(enabled = viewModel.BDEResult != null || viewModel.isCalculating) {
        viewModel.handleAction(CalcButtonAction.Clear)
    }

    BDEContent(
        displayText = viewModel.displayText,
        resultText = viewModel.resultText,
        cursorIndex = viewModel.cursorIndex,
        BDEResult = viewModel.BDEResult,
        isCalculating = viewModel.isCalculating,
        odeConditions = viewModel.odeConditions,
        odeConditionFocusIndex = viewModel.odeConditionFocusIndex,
        onConditionFocus = { viewModel.setOdeConditionFocus(it) },
        onAddCondition = { viewModel.addOdeCondition() },
        onRemoveCondition = { viewModel.removeOdeCondition(it) },
        onMainFocus = { viewModel.setOdeConditionFocus(-1) },
        onUpdateCursorIndex = { viewModel.updateCursorIndex(it) },
        onAction = { viewModel.handleAction(it) },
        onOpenDrawer = onOpenDrawer
    )
}

@Composable
fun BDEContent(
    displayText: String,
    resultText: String,
    cursorIndex: Int,
    BDEResult: BDEResult? = null,
    isCalculating: Boolean = false,
    odeConditions: List<String>,
    odeConditionFocusIndex: Int,
    onConditionFocus: (Int) -> Unit,
    onAddCondition: () -> Unit,
    onRemoveCondition: (Int) -> Unit,
    onMainFocus: () -> Unit,
    onUpdateCursorIndex: (Int) -> Unit,
    onAction: (CalcButtonAction) -> Unit,
    onOpenDrawer: () -> Unit
) {
    CalculatorScaffold(
        title = { Text("Differential Equation Solver") },
        onOpenDrawer = onOpenDrawer,
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
                    .safeDrawingPadding()
            ) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (isCalculating) {
                        LoadingIndicator(
                            modifier = Modifier.fillMaxSize(),
                            message = "Solving Differential Equation...",
                        )
                    } else if (BDEResult == null) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Text(
                                    text = "Main Equation",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onMainFocus() }
                                ) {
                                    CalcBox(
                                        expression = displayText,
                                        result = resultText,
                                        cursorIndex = if (odeConditionFocusIndex == -1) cursorIndex else -1,
                                        onCursorIndexChange = onUpdateCursorIndex,
                                        modifier = Modifier.fillMaxWidth().height(140.dp)
                                    )
                                }
                            }

                            if (odeConditions.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Boundary Conditions",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                                itemsIndexed(odeConditions) { index, condition ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        ConverterField(
                                            label = "Condition ${index + 1}",
                                            value = condition,
                                            isFocused = odeConditionFocusIndex == index,
                                            cursorIndex = if (odeConditionFocusIndex == index) condition.length else -1,
                                            modifier = Modifier.weight(1f),
                                            onClick = { onConditionFocus(index) }
                                        )
                                        IconButton(
                                            onClick = { onRemoveCondition(index) },
                                            modifier = Modifier.padding(top = 24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove Condition",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                OutlinedButton(
                                    onClick = onAddCondition,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add Condition...")
                                }
                            }
                        }
                    } else {
                        OdeReport(
                            result = BDEResult,
                            onClear = { onAction(CalcButtonAction.Clear) }
                        )
                    }
                }

                if (BDEResult == null && !isCalculating) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ShortCalcButtons(
                        modifier = Modifier.weight(1.5f),
                        gridMode = ShortGridMode.BDE,
                        onAction = onAction
                    )
                }
            }
        }
    }
}
